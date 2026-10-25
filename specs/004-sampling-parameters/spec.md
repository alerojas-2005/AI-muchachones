# Feature Specification: Per-request Sampling Parameters

**Feature Branch**: `004-sampling-parameters`  
**Created**: 2026-10-15  
**Status**: Ready for implementation

## User Scenarios & Testing

### User Story 1 — Vary sampling per chat request (P1)

As an API caller, I can set temperature, topP, topK, numPredict, and seed on an individual chat request, so that I can compare generations without rebuilding or restarting the backend.

**Acceptance scenarios**

1. A request with any subset of the five parameters applies those overrides while retaining configured defaults for omitted values.
2. A request that omits all five parameters behaves as before, using `application.properties` and provider defaults.
3. `temperature: 17` receives HTTP 400 with `temperature` and `0.0-2.0` in the error; the model service is not called. Values are rejected, never clamped or passed through.
4. Out-of-range `topP`, `topK`, or `numPredict` similarly receives a field-specific HTTP 400 before model invocation.
5. A successful response additively reports the effective sampling values while preserving all existing response fields.

## Requirements

- **FR-001**: Add optional `Double temperature`, `Double topP`, `Integer topK`, `Integer numPredict`, and `Integer seed` request properties to `POST /api/v1/chat`. Boxed types preserve omitted JSON fields as `null`.
- **FR-002**: Null request values inherit the corresponding configured Ollama option; if configuration is absent, leave that option unset for Ollama's own default.
- **FR-003**: Accept temperature in inclusive range 0.0–2.0 and topP in 0.0–1.0; reject non-finite values as invalid.
- **FR-004**: Accept topK in inclusive range 0–200.
- **FR-005**: Accept numPredict in inclusive range 1–2048. The upper bound is a resource budget for this demo: twice the currently configured 1024-token default, not a value inferred from num-ctx (which is not configured in this project).
- **FR-006**: Reject invalid values with HTTP 400 identifying the field and range. Validation MUST happen before calling `ModelService` or Ollama.
- **FR-007**: seed is optional and uses `Integer`; this feature imposes no additional sign/range constraint because Ollama's seed value is an input identifier, not a generation budget.
- **FR-008**: Keep `/api/v1` and every current response property. Add `samplingParameters` containing effective values and `errorMessage` for validation failures.
- **FR-009**: Never silently clamp or normalize supplied values.

## Security and resource decision

An unbounded `numPredict` lets a caller monopolize the model, consume CPU/memory for a long generation, delay other callers, and grow response/transfer size. The endpoint is a shared resource even when run as a demo. A fixed cap of 2048 limits each generation and is twice the configured default, allowing useful experimentation while bounding the request. It is an application policy limit, not an assertion about model context capacity. A future deployment with known context and concurrency budgets should revisit this value.

## Scope Boundaries

No changes to `specs/001` or `specs/003`. Templates (005), conversation history (006), and guardrails (007) are out of scope. No new endpoint, API version, persistence, or frontend controls.
