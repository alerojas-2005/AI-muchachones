package com.example.chat.impl;

import com.example.chat.ModelResult;
import com.example.chat.SamplingModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.Ordered;

import java.time.Duration;

/**
 * Implementacion de ModelService sobre Spring AI.
 *
 * El starter spring-ai-starter-model-ollama autoconfigura el bean ChatClient.Builder
 * a partir de las propiedades spring.ai.ollama.*, por lo que aqui no hace falta
 * construir clientes HTTP, ni resolver endpoints, ni usar reflexion.
 */
@Service
public class SpringAiModelService implements SamplingModelService {

    private static final Logger log = LoggerFactory.getLogger(SpringAiModelService.class);

    private static final String SYSTEM_PROMPT = """
        Eres un asistente que responde preguntas sobre los documentos cargados por el usuario.
        Responde siempre en el mismo idioma en el que te escriba el usuario.
        Usa solo la informacion del contexto. Si no esta ahi, dilo en lugar de inventar.
        """;

    private final ChatClient chatClient;
    private final OllamaChatOptions configuredOptions;

    public SpringAiModelService(ChatClient.Builder chatClientBuilder,
                            OllamaChatModel chatModel,
                            VectorStore vectorStore) {
    this.chatClient = chatClientBuilder
            .defaultSystem(SYSTEM_PROMPT)
            .defaultAdvisors(
                    QuestionAnswerAdvisor.builder(vectorStore)
                            .searchRequest(SearchRequest.builder()
                                    .topK(4)                   // cuantos fragmentos recuperar
                                    .similarityThreshold(0.5)  // similitud minima (0 a 1)
                                    .build())
                            .build(),
                    // Muestra en el log el prompt real (con fragmentos). Va DESPUES del RAG.
                    new SimpleLoggerAdvisor(Ordered.LOWEST_PRECEDENCE - 1))
            .build();
    this.configuredOptions = chatModel.getOptions();
}

    @Override
    public ModelResult generateResponse(String question, String sessionId) {
        return generateResponse(question, sessionId, new com.example.chat.dto.ChatRequest()).modelResult();
    }


@Override
public SamplingResult generateResponse(
        String question,
        String sessionId,
        com.example.chat.dto.ChatRequest request) {

    log.debug(
            "Enviando prompt al modelo. sessionId={} longitud={}",
            sessionId,
            question.length()
    );

    OllamaChatOptions.Builder optionsBuilder = effectiveOptions(request);
    OllamaChatOptions options = optionsBuilder.build();

    ChatResponse response = chatClient.prompt()
            .user(question)
            .options(optionsBuilder)
            .call()
            .chatResponse();

    if (response == null) {
        log.warn(
                "El modelo devolvio una respuesta vacia. sessionId={}",
                sessionId
        );
        return emptyResult(options);
    }

    ModelResult result = toModelResult(response);
    SamplingResult samplingResult =
            new SamplingResult(result, samplingParameters(options));

    if (result.answer() == null || result.answer().isBlank()) {
        log.warn(
                "El modelo devolvio una respuesta vacia. sessionId={}",
                sessionId
        );
        return samplingResult;
    }

    log.debug(
            "Respuesta recibida. sessionId={} longitud={}",
            sessionId,
            result.answer().length()
    );

    return samplingResult;
}


private OllamaChatOptions.Builder effectiveOptions(
        com.example.chat.dto.ChatRequest request) {

    OllamaChatOptions.Builder builder =
            OllamaChatOptions.builder()
                    .model(configuredOptions.getModel());

    Double temperature = request.getTemperature() != null
            ? request.getTemperature()
            : configuredOptions.getTemperature();

    Double topP = request.getTopP() != null
            ? request.getTopP()
            : configuredOptions.getTopP();

    Integer topK = request.getTopK() != null
            ? request.getTopK()
            : configuredOptions.getTopK();

    Integer numPredict = request.getNumPredict() != null
            ? request.getNumPredict()
            : configuredOptions.getNumPredict();

    Integer seed = request.getSeed() != null
            ? request.getSeed()
            : configuredOptions.getSeed();

    if (temperature != null) builder.temperature(temperature);
    if (topP != null) builder.topP(topP);
    if (topK != null) builder.topK(topK);
    if (numPredict != null) builder.numPredict(numPredict);
    if (seed != null) builder.seed(seed);

    return builder;
}

    static ModelResult toModelResult(ChatResponse response) {
        Generation generation = response.getResult();
        String answer = generation == null || generation.getOutput() == null
                ? ""
                : generation.getOutput().getText();

        Usage usage = response.getMetadata().getUsage();
        Integer promptTokens = usage == null ? null : usage.getPromptTokens();
        Integer completionTokens = usage == null ? null : usage.getCompletionTokens();
        Integer totalTokens = usage == null ? null : usage.getTotalTokens();

        String finishReason = generation == null || generation.getMetadata() == null
                ? null
                : generation.getMetadata().getFinishReason();
        Object evalDurationValue = response.getMetadata().get("eval-duration");
        Double tokensPerSecond = calculateTokensPerSecond(completionTokens, evalDurationValue);
        String model = response.getMetadata().getModel();
        if (model == null || model.isBlank()) {
            model = null;
        }

        return new ModelResult(
                answer,
                promptTokens,
                completionTokens,
                totalTokens,
                model,
                finishReason,
                tokensPerSecond);
    }

    private static Double calculateTokensPerSecond(Integer completionTokens, Object evalDurationValue) {
        if (completionTokens == null
                || !(evalDurationValue instanceof Duration evalDuration)
                || evalDuration.isZero()
                || evalDuration.isNegative()) {
            return null;
        }

        double seconds = evalDuration.getSeconds() + evalDuration.getNano() / 1_000_000_000.0;
        return completionTokens / seconds;
    }

    private static SamplingResult emptyResult(OllamaChatOptions options) {
        return new SamplingResult(new ModelResult("", null, null, null, null, null, null), samplingParameters(options));
    }

    private static com.example.chat.dto.ChatResponse.SamplingParameters samplingParameters(OllamaChatOptions options) {
        return new com.example.chat.dto.ChatResponse.SamplingParameters(options.getTemperature(), options.getTopP(),
                options.getTopK(), options.getNumPredict(), options.getSeed());
    }
}
