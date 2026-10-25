# Implementation Plan: Token Usage Metrics

**Branch**: `003-token-usage-metrics` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification in `specs/003-token-usage-metrics/spec.md`

## Summary

Expose Ollama's token usage, model name, generation finish reason, and calculated completion throughput in the existing `POST /api/v1/chat` response. Preserve the current API version and fields. Replace the text-only service result with a typed result carrying both the answer and response metadata, obtain the full Spring AI response using `.chatResponse()`, and use Spring AI common usage metadata rather than native usage.

## Technical Context

**Language/Version**: Java 21 (Gradle toolchain)

**Primary Dependencies**: Spring Boot 4.1.0; Spring AI 2.0.0; Spring AI Ollama starter; JUnit/Spring Boot Test

**Storage**: None; response metadata is transient

**Testing**: Gradle `test`, focused unit tests, and existing shell integration/smoke checks

**Target Platform**: Spring Boot backend on local Docker Compose/Ollama

**Project Type**: Web application (Angular frontend and Spring Boot backend)

**Performance Goals**: No additional model calls; calculate metrics from the existing model response

**Constraints**: Keep `/api/v1` and request shape stable; unavailable provider metadata must not be fabricated; do not edit `specs/001`

**Scale/Scope**: One existing chat endpoint and its backend response envelope

## Constitution Check

| Principle | Pre-implementation finding | Design disposition |
|---|---|---|
| I. Demo-Focused Simplicity | Pass: no additional services or infrastructure are required. | Preserve the existing single request/model call. |
| II. Reproducible Local Execution | Pass for this feature: no new runtime setup is introduced. | Use the current Docker/Ollama quickstart. |
| III. Clear Interfaces & Contracts | Pass: the endpoint remains explicit, versioned HTTP/JSON. | Extend the existing response only; document the contract and tests. |
| IV. Data Minimization & Secrets Handling | **Existing violation**: `ChatController` logs the raw question; `LogRedactionFilter` does not apply its declared redaction pattern. | Do not add prompt/secret logging. Existing logging issue is out of scope and remains open. |
| V. Testing & Observability | **Existing violation**: no backend Java unit tests existed; the smoke check only searched for `answer` and does not validate the response contract or full frontend path. | Add backend unit tests and assert metadata properties in the existing smoke checks. The broader end-to-end coverage gap remains open. |

**Gate result**: Principles IV and V have confirmed pre-existing violations, explicitly reported above; they are not new violations introduced by this feature. The feature-specific design preserves the other principles and improves test coverage. No scope expansion to remediate existing prompt logging or full UI end-to-end testing.

## Verified Spring AI 2.0.0 API

Before implementation, the `v2.0.0` source was checked (details in [research.md](research.md)):

- `OllamaChatModel.from(...)` supplies `ChatResponseMetadata.usage` and `.model`.
- Usage comes from Ollama prompt-evaluation and evaluation counts via `DefaultUsage`; the implementation will call `getUsage()` and will not call `getNativeUsage()`.
- The provider stores `eval-duration` as a `Duration` metadata entry.
- The first `Generation` carries a nullable finish reason in `getMetadata().getFinishReason()`.
- `ChatClient.CallResponseSpec.chatResponse()` is nullable, so the empty-response path is explicitly guarded.

## Design

1. Add a typed backend model result to transport `answer` and metadata together through `ModelService`.
2. Call `.call().chatResponse()` and map common usage/model metadata and first-generation finish reason.
3. Calculate `tokensPerSecond` from completion tokens and positive `eval-duration`; return null for unavailable or non-positive values.
4. Extend the existing API response DTO and controller without changing its route, request, existing properties, or error statuses.
5. Add service/controller unit tests and assert all new properties in the integration and CI smoke checks.

## Project Structure

```text
backend/src/main/java/com/example/chat/
├── ChatController.java
├── ModelService.java
├── ModelResult.java
├── dto/ChatResponse.java
└── impl/SpringAiModelService.java

backend/src/test/java/com/example/chat/
├── ChatControllerTest.java
└── impl/SpringAiModelServiceTest.java

tests/integration/test_chat_flow.sh
scripts/smoke_test.sh
specs/003-token-usage-metrics/
├── contracts/chat-api.md
├── data-model.md
├── plan.md
├── quickstart.md
├── research.md
├── spec.md
└── tasks.md
```

**Structure Decision**: Keep application code in the existing Spring Boot backend, verify its API at the existing integration scripts, and keep design artifacts in the new feature directory. Do not modify `specs/001`.

## Complexity Tracking

No new services, dependencies, persistence, or API versions are introduced. The two existing principle violations are pre-existing and recorded in the Constitution Check; this implementation does not claim to remediate them.
