# Research: Per-request Sampling Parameters

## Spring AI 2.0.0 source verification

The exact `v2.0.0` source tag was inspected before implementation:

- [`OllamaChatOptions.java`](https://raw.githubusercontent.com/spring-projects/spring-ai/v2.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java): static `builder()`; setters for temperature, topP, topK, numPredict, and seed. `numPredict(Integer)` delegates to `maxTokens(Integer)`.
- [`OllamaChatModel.java`](https://raw.githubusercontent.com/spring-projects/spring-ai/v2.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java): `buildRequestPrompt` attaches configured model options only if the prompt has no options; otherwise it returns the prompt unchanged. Consequently options supplied to a prompt are not automatically merged over `application.properties`.

Implementation implication: read the configured `OllamaChatOptions` from the autoconfigured model, resolve each field independently, retain the configured model name, and supply the resolved complete options object. For an unset property and absent request override, keep the field null so Ollama applies its default.

The requested deployment context names `llama3.2:3b`, while the current checked-in `application.properties` still selects `llama3`. The sampling implementation preserves the configured model; it does not migrate the existing model setting as part of this feature.

## numPredict bound

The codebase configures `num-predict=1024` and does not define `num-ctx`. `numPredict` is the requested output-token budget. An attacker can set a very large value to occupy inference resources, increase latency for all callers, and generate an oversized response. Cap it at 2048 (2x current configured default) as an explicit demo resource limit; do not derive it from an unspecified context window.
