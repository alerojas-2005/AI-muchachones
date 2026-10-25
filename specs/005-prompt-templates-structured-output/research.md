# Research: Prompt Templates and Structured Output

## Resolved dependency classpath verification

Local Gradle cache inspection confirmed the exact Spring AI 2.0.0 artifacts and class entries:

| Artifact | Class entry | Package |
|---|---|---|
| `spring-ai-template-st-2.0.0.jar` | `org/springframework/ai/template/st/StTemplateRenderer.class` | `org.springframework.ai.template.st.StTemplateRenderer` |
| `spring-ai-model-2.0.0.jar` | `org/springframework/ai/converter/BeanOutputConverter.class` | `org.springframework.ai.converter.BeanOutputConverter` |
| `spring-ai-client-chat-2.0.0.jar` | `org/springframework/ai/chat/client/ChatClient$EntityParamSpec.class` | `org.springframework.ai.chat.client.ChatClient.EntityParamSpec` |

`javap` confirmed `EntityParamSpec.validateSchema()` and `.useProviderStructuredOutput()`, both returning `EntityParamSpec`; `CallResponseSpec.entity(Class<T>, Consumer<EntityParamSpec>)`; `PromptSystemSpec.text(Resource)` and `.params(Map)`; `ChatClient.Builder.defaultSystem(Resource)` and `.defaultAdvisors(...)`; and renderer builder delimiter methods.

Jackson note: the schema-related annotations remain `com.fasterxml.jackson.annotation.JsonProperty` / `JsonPropertyDescription`. Jackson 3's databind types use `tools.jackson`; this does not change the annotations package.

## Loopback HTTP behavior checks

Ollama was not running at `localhost:11434`, so the requested local simulation used an Ollama-compatible HTTP server on loopback. These are actual `ChatClient`/`OllamaChatModel` calls through the resolved Spring AI 2.0.0 classes, but the simulation does not prove how the llama3.2:3b model itself obeys `format`.

1. **Renderer collision**: default delimiters threw exactly `java.lang.IllegalArgumentException: The template string is not valid.` With `<`/`>` delimiters the JSON example remained literal and `<topic>` rendered as `books`.
2. **Feature 004 sampling through `entity()`**: the fake API received options `temperature=0.35`, `top_p=0.82`, `top_k=31`, `num_predict=123`, and `seed=77` on the typed response request. This verifies Spring AI's request path with the sampling builder; it also exposed that the checked-in feature 004 source currently calls `.options()` with a built options object, which fails compilation because Spring AI 2.0.0 expects an `ChatOptions.Builder`. That source was not edited.
3. **Framework vs provider**: with the same required-field `ExtractedItem` and first response missing `summary`, framework mode issued a second request. Its second user-message content included `Output JSON validation failed because of: no se encontró la propiedad requerida 'summary'` and the schema instructions, then returned the valid second result. Provider mode sent an object-valued top-level `format` JSON schema; when the fake server returned the same invalid first result, conversion threw `Missing required creator property 'summary'` after one request, with no framework retry. The fake server does not enforce format; provider enforcement by Ollama remains a live-model check.

The verification harness completed 4 tests successfully, including the system-resource override check. This is framework-level behavior against the loopback server, not a real llama3.2:3b inference.

## Template token overhead and cost

Measure each resource by comparing `promptTokens` for an identical question/model with no template and with that template, keeping sampling options equal. Record model version, template ID/revision, baseline count, template count, and delta. No numeric delta is reported yet: the loopback server used a fixed synthetic count and cannot tokenize prompts. Token count varies with model tokenizer and variable values, so each template's count must come from the llama3.2:3b run. Local Ollama has no per-token invoice; the per-turn cost is extra prompt-evaluation compute and latency. For a hosted price `p` per million input tokens, the monetary formula is `deltaTokens * p / 1,000,000`; no hosted price is assumed here.
