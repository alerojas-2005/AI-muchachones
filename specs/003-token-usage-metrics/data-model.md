# Data Model: Token Usage Metrics

## Chat response

An ephemeral response envelope returned for each `POST /api/v1/chat` request.

| Property | Type | Source / behavior |
|---|---|---|
| `requestId` | string | Existing request identifier; unchanged |
| `answer` | string | Existing generated response; unchanged |
| `elapsedMs` | integer | Existing backend elapsed time; unchanged |
| `status` | string | Existing success/error status; unchanged |
| `promptTokens` | integer or null | Common model usage metadata; Spring AI's Ollama adapter reports absent native counts as zero |
| `completionTokens` | integer or null | Common model usage metadata; Spring AI's Ollama adapter reports absent native counts as zero |
| `totalTokens` | integer or null | Common model usage metadata |
| `model` | string or null | Chat response metadata |
| `finishReason` | string or null | First generation's metadata |
| `tokensPerSecond` | number or null | Completion tokens divided by positive evaluation duration in seconds |

No entity is persisted. Nullable metadata and uncalculable throughput remain `null`. Token values are passed through as reported by Spring AI, including zeros used by its Ollama adapter and `EmptyUsage`.

## Relationships

One chat response corresponds to the Spring AI chat response from the single model invocation. Its common usage and model metadata apply to the response; finish reason applies to the generated choice. The throughput calculation uses the provider-reported model evaluation duration and completion-token count.
