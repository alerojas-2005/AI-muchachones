# Research: Token Usage Metrics

## Spring AI 2.0.0 source verification

The backend uses Spring AI `2.0.0` as pinned in `backend/build.gradle`. Before implementation, the tagged 2.0.0 source was inspected:

- [`OllamaChatModel.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java) constructs `ChatResponseMetadata` with `.usage(aggregatedUsage)` and `.model(response.model())`.
- Ollama usage is created from `promptEvalCount` and `evalCount` through `DefaultUsage`; absent Ollama counts are converted to zero by this provider implementation.
- Spring AI's `EmptyUsage` also returns zero token counts, so the service preserves those common API values rather than interpreting zero as unavailable.
- [`EmptyUsage.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/metadata/EmptyUsage.java) confirms that the common no-usage implementation returns zero for prompt and completion tokens.
- The provider stores the evaluation time as the response metadata key `eval-duration`, with a `Duration` value. Missing duration values are omitted by the metadata builder.
- For a non-streaming response with both evaluation counts, Ollama sets the generation finish reason from `ollamaResponse.doneReason()`.
- [`ChatResponseMetadata.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/metadata/ChatResponseMetadata.java) exposes common `getUsage()` and `getModel()` accessors.
- [`Usage.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/metadata/Usage.java) exposes prompt, completion, and total tokens; its `getNativeUsage()` is explicitly nullable and is not needed for this feature.
- [`Generation.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-model/src/main/java/org/springframework/ai/chat/model/Generation.java) exposes `getMetadata()`, whose `ChatGenerationMetadata.getFinishReason()` may be null.
- [`ChatClient.java`](https://github.com/spring-projects/spring-ai/blob/v2.0.0/spring-ai-client-chat/src/main/java/org/springframework/ai/chat/client/ChatClient.java) declares `CallResponseSpec.chatResponse()` as nullable.

## Decisions

### Decision: Keep the endpoint and add metadata to its response DTO

**Rationale**: This is an additive change to the existing versioned contract and does not force current clients to migrate.

**Alternatives considered**: A new API version or a separate metrics endpoint would add migration or request complexity without a requirement for either.

### Decision: Use Spring AI common usage metadata and generation metadata

**Rationale**: Spring AI 2.0.0's Ollama adapter populates common usage and model metadata and generation finish reason. This avoids coupling the service to Ollama-native response objects. The adapter and `EmptyUsage` normalize missing counts to zero, and those values are returned unchanged.

**Alternatives considered**: `Usage.getNativeUsage()` is nullable, provider-specific, and unnecessary for the requested token metrics.

### Decision: Calculate throughput from completion tokens and `eval-duration`

**Rationale**: Ollama reports model evaluation duration as a `Duration`; completion tokens divided by positive evaluation seconds yields generation tokens per second. An absent or non-positive duration results in `null`.

**Alternatives considered**: Dividing by backend elapsed time would mix prompt processing and application overhead into a generation-rate metric.

## Existing Constitution IV and V findings (pre-feature baseline)

- **Principle IV — Data Minimization & Secrets Handling: violation found.** `ChatController` logs `req.getQuestion()` verbatim at INFO level, so user-provided text can be persisted in logs. `LogRedactionFilter` is a placeholder: its `SECRET_PATTERN` is not used to redact request data and the filter only logs method and path. This feature does not change the pre-existing logging behavior.
- **Principle V — Testing & Observability: violation found.** Before this feature there were no backend Java unit-test sources. The existing shell smoke checks only for an `answer` substring, and the CI workflow runs that smoke check rather than verifying the chat response contract or a frontend-to-model-to-frontend path. This feature adds unit coverage for metadata mapping and extends the API smoke checks for the six new response properties, but does not claim to close all existing end-to-end coverage gaps.

These findings are recorded explicitly and are not silently treated as passing gates.
