# Quickstart: Token Usage Metrics

## Prerequisites

- JDK 21 (the backend's configured toolchain) and Gradle available to run backend tests.
- For the live API smoke check: Docker Compose, Ollama, and the configured `llama3` model.

## Verify the backend mapping

From `backend/`, run:

```bash
gradle test
```

The unit tests verify usage/model/finish-reason mapping, throughput calculation, and unavailable-duration behavior without requiring a running Ollama service.

## Verify the live API contract

Start the project using the existing Quickstart commands in the repository README. Then run:

```bash
./scripts/smoke_test.sh
```

The smoke test posts to the existing `/api/v1/chat` endpoint and verifies the existing answer plus all six metadata property names. A successful response retains the existing response envelope and includes the fields documented in [the API contract](contracts/chat-api.md).

## Expected behavior

- Successful responses contain `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond`.
- Missing model/finish metadata is serialized as `null`; Spring AI-reported token counts (including its normalized zero values) are preserved. Zero or missing evaluation duration does not produce a throughput value.
- The endpoint remains `/api/v1/chat`.
