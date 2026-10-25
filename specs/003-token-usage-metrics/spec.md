# Feature Specification: Token Usage Metrics

**Feature Branch**: `003-token-usage-metrics`

**Created**: 2026-10-15

**Status**: Ready for implementation

**Input**: Expose token usage and model metadata that the backend currently discards when producing chat responses.

## User Scenarios & Testing

### User Story 1 - Inspect chat generation metrics (Priority: P1)

As an API consumer, I can inspect the token usage and generation metadata for each chat response so that I can understand the model output and its cost and throughput.

**Why this priority**: The backend currently returns only generated text and request timing, losing data already provided by the model.

**Independent Test**: POST a valid question to `/api/v1/chat` and verify that the response retains its existing fields and additively includes token counts, model, finish reason, and throughput.

**Acceptance Scenarios**:

1. **Given** a successful model response containing usage and generation metadata, **When** a client posts a chat request, **Then** the existing response fields remain unchanged and `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond` are present.
2. **Given** the model reports an evaluation duration greater than zero, **When** the response is assembled, **Then** `tokensPerSecond` represents generated completion tokens per second.
3. **Given** model metadata is unavailable, **When** a response is assembled, **Then** nullable metadata and uncalculable throughput are represented as `null`; token counts are preserved exactly as Spring AI reports them, and generation still follows the existing success/error behavior.

### User Story 2 - Preserve existing chat clients (Priority: P2)

As an existing API consumer, I can continue using the current chat endpoint and response fields without changing API version or request shape.

**Why this priority**: Metrics add observability without requiring a breaking migration for current clients.

**Independent Test**: Verify the existing route, request body, `requestId`, `answer`, `elapsedMs`, and `status` remain available after metrics are added.

**Acceptance Scenarios**:

1. **Given** an existing client posting to `/api/v1/chat`, **When** it sends the same request as before, **Then** the route and pre-existing response fields remain compatible.

## Edge Cases

- The chat client may return no response or a response without a generation; the backend treats this as the existing empty-answer error case.
- A response may omit usage or the model may omit `eval-duration`; fields remain present. Spring AI's Ollama adapter reports missing usage counts as zero, while unavailable nullable metadata and throughput remain `null`.
- A missing or zero evaluation duration does not produce an infinite, undefined, or misleading throughput value.
- Validation failures and model failures retain their existing HTTP status and response fields; new metadata fields remain present and are null when no model result is available.

## Requirements

### Functional Requirements

- **FR-001**: The chat endpoint MUST return `promptTokens`, `completionTokens`, `totalTokens`, `model`, `finishReason`, and `tokensPerSecond` in addition to the response fields it already returns.
- **FR-002**: The endpoint MUST remain `POST /api/v1/chat`; this feature MUST NOT change the API version or request shape.
- **FR-003**: Token counts and model name MUST reflect metadata reported for the corresponding generated response.
- **FR-004**: `finishReason` MUST reflect the generated choice's reported completion reason.
- **FR-005**: `tokensPerSecond` MUST be calculated as completion tokens divided by positive model evaluation duration; it MUST be `null` if either input is unavailable or duration is not positive.
- **FR-006**: All six properties MUST remain present; nullable metadata and uncalculable throughput MUST be `null`, while token counts MUST reflect Spring AI's common usage values without inventing replacements.
- **FR-007**: Existing response properties and existing success/error behavior MUST be preserved.
- **FR-008**: The implementation MUST NOT depend on provider-native usage metadata to populate the common token counts.

### Key Entities

- **Chat response**: The existing response envelope for a chat request, extended with usage counts, model identity, finish reason, and generation throughput.
- **Generation metadata**: Per-response information reported by the model, including usage counts, model identifier, finish reason, and evaluation duration.

## Success Criteria

### Measurable Outcomes

- **SC-001**: A successful chat response includes all six new JSON properties while retaining every existing response property.
- **SC-002**: Automated tests verify the field mapping and throughput calculation for a response with model metadata.
- **SC-003**: Automated tests verify missing or non-positive timing does not produce a fabricated throughput value.
- **SC-004**: The existing API path and request shape remain unchanged.

## Assumptions

- Nullable metadata and uncalculable throughput are represented as JSON `null`. Token counts are passed through from Spring AI; the Ollama adapter may normalize absent native counts to zero.
- Completion throughput uses the model evaluation duration, not total request elapsed time, because the latter includes prompt processing and backend overhead.
- Token usage provided by Spring AI's common usage metadata is the supported source for token counts.
- The frontend does not display these metrics in this feature; existing clients may ignore the added properties.

## Scope Boundaries

- No changes are made to `specs/001`.
- No new endpoint, API version, persistence, model configuration, or frontend metrics display is introduced.
