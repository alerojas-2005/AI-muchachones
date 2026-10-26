package com.example.chat.impl;

import com.example.chat.ModelResult;
import com.example.chat.PromptExecutionResult;
import com.example.chat.PromptTemplateId;
import com.example.chat.PromptTemplateModelService;
import com.example.chat.PromptTemplateRegistry;
import com.example.chat.StructuredOutputMode;
import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ExtractedItem;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;
import java.util.HashMap;

@Service
public class SpringAiPromptTemplateModelService implements PromptTemplateModelService {

    private final ChatClient chatClient;
    private final OllamaChatOptions configuredOptions;
    private final PromptTemplateRegistry registry;

public SpringAiPromptTemplateModelService(
        ChatClient.Builder chatClientBuilder,
        OllamaChatModel chatModel,
        PromptTemplateRegistry registry,
        VectorStore vectorStore,
        @Value("${rag.top-k:4}") int topK,
        @Value("${rag.similarity-threshold:0.5}") double similarityThreshold) {

    this.registry = registry;
    this.configuredOptions = chatModel.getOptions();

    this.chatClient = chatClientBuilder
            .defaultSystem(
                    registry.resolve(
                            PromptTemplateId.CONCISE.value(),
                            Map.of()
                    ).resource()
            )
            .defaultTemplateRenderer(
                    StTemplateRenderer.builder()
                            .startDelimiterToken('<')
                            .endDelimiterToken('>')
                            .build()
            )
            .defaultAdvisors(
                    QuestionAnswerAdvisor.builder(vectorStore)
                            .promptTemplate(
                                    PromptTemplate.builder()
                                            .renderer(
                                                    StTemplateRenderer.builder()
                                                            .startDelimiterToken('<')
                                                            .endDelimiterToken('>')
                                                            .build()
                                            )
                                            .template("""
                                                    <query>

                                                    Información recuperada del documento:
                                                    ---------------------
                                                    <question_answer_context>
                                                    ---------------------

                                                    Reglas:
                                                    Reglas:
                                                    1. Responde siempre en español, de forma clara y directa.
                                                    2. Usa el contexto de arriba para responder, citando el artículo cuando aparezca.
                                                    3. Solo si el contexto realmente no contiene información relacionada con la pregunta,
                                                    responde: No encuentro esta información en el documento.
                                                    4. No uses conocimiento externo ni inventes datos.
                                                    """)
                                            .build()
                            )
                            .searchRequest(
                                    SearchRequest.builder()
                                            .topK(topK)
                                            .similarityThreshold(similarityThreshold)
                                            .build()
                            )
                            .build(),
                    new SimpleLoggerAdvisor(
                            Ordered.LOWEST_PRECEDENCE - 1
                    )
            )
            .build();
             org.slf4j.LoggerFactory.getLogger(getClass())
            .info("RAG config: topK={} threshold={}", topK, similarityThreshold);
}

    @Override
    public String validate(ChatRequest request) {
        boolean hasTemplateVariables = request.getTemplateVariables() != null && !request.getTemplateVariables().isEmpty();
        String templateId = request.getPromptTemplateId();
        StructuredOutputMode mode;
        try {
            mode = StructuredOutputMode.from(request.getStructuredOutputMode());
            if (templateId == null && mode != null) {
                return "promptTemplateId is required for structuredOutputMode";
            }
            if (templateId == null && hasTemplateVariables) {
                return "promptTemplateId is required when templateVariables are supplied";
            }
            if (templateId != null) {
                registry.resolve(templateId, request.getTemplateVariables());
            }
            if (mode != null && !PromptTemplateId.BOOK_EXTRACTOR.value().equals(templateId)) {
                return "structured output requires promptTemplateId 'book-extractor'";
            }
            return null;
        }
        catch (IllegalArgumentException ex) {
            return ex.getMessage();
        }
    }

    @Override
    public PromptExecutionResult generate(String question, String sessionId, ChatRequest request) {
        String templateId = request.getPromptTemplateId() == null
                ? PromptTemplateId.CONCISE.value()
                : request.getPromptTemplateId();
        PromptTemplateRegistry.Definition template = request.getPromptTemplateId() == null
                ? registry.resolve(templateId, Map.of())
                : registry.resolve(templateId, request.getTemplateVariables());
        StructuredOutputMode mode = StructuredOutputMode.from(request.getStructuredOutputMode());
        Map<String, Object> templateParameters = new HashMap<>(template.variables());

        var call = chatClient.prompt()
                .system(system -> system.text(template.resource()).params(templateParameters))
                .user(question)
                .options(effectiveOptions(request));

        if (mode == null) {
            ChatResponse response = call.call().chatResponse();
            ModelResult result = SpringAiModelService.toModelResult(response);
            return new PromptExecutionResult(result, null, templateId, null, samplingParameters(request));
        }

        ResponseEntity<ChatResponse, ExtractedItem> response = call.call().responseEntity(ExtractedItem.class, spec -> {
            if (mode == StructuredOutputMode.FRAMEWORK_VALIDATION || mode == StructuredOutputMode.BOTH) {
                spec.validateSchema();
            }
            if (mode == StructuredOutputMode.PROVIDER_SCHEMA || mode == StructuredOutputMode.BOTH) {
                spec.useProviderStructuredOutput();
            }
        });
        return new PromptExecutionResult(
                SpringAiModelService.toModelResult(response.response()),
                response.entity(),
                templateId,
                mode.name(),
                samplingParameters(request));
    }

    private OllamaChatOptions.Builder effectiveOptions(ChatRequest request) {
        OllamaChatOptions.Builder builder = OllamaChatOptions.builder().model(configuredOptions.getModel());
        Double temperature = request.getTemperature() != null ? request.getTemperature() : configuredOptions.getTemperature();
        Double topP = request.getTopP() != null ? request.getTopP() : configuredOptions.getTopP();
        Integer topK = request.getTopK() != null ? request.getTopK() : configuredOptions.getTopK();
        Integer numPredict = request.getNumPredict() != null ? request.getNumPredict() : configuredOptions.getNumPredict();
        Integer seed = request.getSeed() != null ? request.getSeed() : configuredOptions.getSeed();
        if (temperature != null) builder.temperature(temperature);
        if (topP != null) builder.topP(topP);
        if (topK != null) builder.topK(topK);
        if (numPredict != null) builder.numPredict(numPredict);
        if (seed != null) builder.seed(seed);
        return builder;
    }

    private com.example.chat.dto.ChatResponse.SamplingParameters samplingParameters(ChatRequest request) {
        OllamaChatOptions options = effectiveOptions(request).build();
        return new com.example.chat.dto.ChatResponse.SamplingParameters(options.getTemperature(), options.getTopP(),
                options.getTopK(), options.getNumPredict(), options.getSeed());
    }
}
