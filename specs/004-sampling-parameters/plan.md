# Implementation Plan: Per-request Sampling Parameters

**Branch**: `004-sampling-parameters` | **Date**: 2026-10-15 | **Spec**: [spec.md](spec.md)

## Summary

Add five nullable sampling overrides to `ChatRequest`, validate them explicitly at the controller boundary, merge them over the configured `OllamaChatOptions`, and call the existing `ChatClient` with those effective options. Preserve the `/api/v1/chat` envelope and add effective values under `samplingParameters`.

## Verified Spring AI 2.0.0 API and option semantics

Checked the `v2.0.0` source for [`OllamaChatOptions`](https://raw.githubusercontent.com/spring-projects/spring-ai/v2.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/api/OllamaChatOptions.java) and [`OllamaChatModel`](https://raw.githubusercontent.com/spring-projects/spring-ai/v2.0.0/models/spring-ai-ollama/src/main/java/org/springframework/ai/ollama/OllamaChatModel.java). `OllamaChatOptions.builder()` exists and exposes `temperature(Double)`, `topP(Double)`, `topK(Integer)`, `numPredict(Integer)`, and `seed(Integer)` (numPredict delegates to maxTokens). `OllamaChatModel.buildRequestPrompt` uses model defaults only when prompt options are null; when request options exist, it passes the prompt through unchanged. Therefore per-request options replace defaults rather than being merged automatically. Build effective options from `OllamaChatModel.getOptions()`, preserve its configured model, and then select request values when non-null; pass that complete effective options object to `.options(options).call().chatResponse()`.

## Decisions

- Validate explicitly in `ChatController`, before the model service call. Jakarta annotations are not used for the sampling ranges: current controller `BindingResult` is tailored to `@Valid`, whereas the generic `RuntimeException` handler returns 500. Direct checks produce the required field/range HTTP 400 and make the no-model-call condition obvious. Existing `question` validation remains as-is.
- `temperature: 17` is rejected with HTTP 400; it is neither clamped to 2.0 nor sent to Ollama.
- `numPredict` maximum is 2048, twice the configured 1024 default. This is a demo resource budget, not a claim tied to `num-ctx`, which is undefined in this project.
- Successful responses add `samplingParameters` with the effective values, including configured defaults. This is additive and allows callers to know what the endpoint resolved/applied. Validation failures include additive `errorMessage`.
- No change to `specs/003` or its existing metrics contract.

## Constitution Check (`.specify/memory/constitution.md`)

| Principle | Status | Assessment |
|---|---|---|
| I. Demo-Focused Simplicity | Pass | Keeps the existing endpoint, model call, and local setup. |
| II. Reproducible Local Execution | Pass | No new services or runtime configuration required. |
| III. Clear Interfaces & Contracts | Pass | Optional JSON fields and additive response fields are specified. |
| IV. Data Minimization & Secrets Handling | Existing issue, not addressed | Existing controller logs raw questions; this feature adds no parameter or prompt logging. |
| V. Testing & Observability | **At risk / not satisfied by current coverage** | Each new parameter adds endpoint paths, and current coverage is minimal (one controller success test and service metadata tests; no acceptance coverage for these ranges/overrides). Tests should cover each boundary, omitted/default behavior, override merge, no invocation on 400, and additive response compatibility before merge. This gap is reported explicitly; the plan does not claim Principle V passes. |

**Gate result**: Principle V remains an explicit coverage gap. The implementation stays bounded and keeps validation observable through field-specific client errors. Existing Principle IV issue remains unchanged.

## Technical Context

- Java 21; Spring Boot 4.1.0; Spring AI 2.0.0; Ollama. The requested runtime model is `llama3.2:3b`; the checked-in `application.properties` currently names `llama3`. This feature leaves that existing model setting untouched and preserves whichever model is configured at runtime.
- No persistence or frontend changes.
- Existing endpoint response metrics from feature 003 must remain intact.

## Implementation Shape

1. Add boxed optional request fields.
2. Reject out-of-range values explicitly before invoking `ModelService`.
3. Resolve effective option values from model configuration plus request overrides.
4. Call the model with `OllamaChatOptions.builder()` and the complete resolved options.
5. Return resolved parameters additively in `samplingParameters`.
6. Update feature 004 contracts and quickstart examples; leave specs 001 and 003 untouched.
