# Tasks: Token Usage Metrics

**Input**: Design documents in `specs/003-token-usage-metrics/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/chat-api.md`

## Phase 1: Setup

- [x] T001 [P] Document verified Spring AI 2.0.0 metadata behavior in `specs/003-token-usage-metrics/research.md`
- [x] T002 [P] Define additive chat response contract in `specs/003-token-usage-metrics/contracts/chat-api.md`

## Phase 2: Foundational

- [x] T003 Add a typed result carrying generated answer and optional model metadata in `backend/src/main/java/com/example/chat/ModelResult.java`
- [x] T004 Update the model service contract to return the typed result in `backend/src/main/java/com/example/chat/ModelService.java`

## Phase 3: User Story 1 - Inspect chat generation metrics (P1)

**Goal**: Return the model metadata additively with each successful chat response.

**Independent Test**: Run backend unit tests and verify the live API smoke check sees all six metadata properties.

- [x] T005 [US1] Read full Spring AI response with `.chatResponse()` and map common usage, model, finish reason, and throughput in `backend/src/main/java/com/example/chat/impl/SpringAiModelService.java`
- [x] T006 [US1] Add metadata fields to the existing response envelope in `backend/src/main/java/com/example/chat/dto/ChatResponse.java`
- [x] T007 [US1] Pass metadata through the existing controller without changing route or error behavior in `backend/src/main/java/com/example/chat/ChatController.java`
- [x] T008 [P] [US1] Test metadata mapping and unavailable-duration handling in `backend/src/test/java/com/example/chat/impl/SpringAiModelServiceTest.java`
- [x] T009 [P] [US1] Test additive response fields and preserved existing fields in `backend/src/test/java/com/example/chat/ChatControllerTest.java`
- [x] T010 [US1] Assert all metadata properties in the integration and CI smoke checks in `tests/integration/test_chat_flow.sh` and `scripts/smoke_test.sh`

## Phase 4: User Story 2 - Preserve existing chat clients (P2)

**Goal**: Keep the existing route, request, response properties, and error behavior intact.

**Independent Test**: Controller unit tests verify the existing response envelope, and the integration smoke test continues posting the original request.

- [x] T011 [US2] Document API compatibility and null metadata behavior in `README.md` and `specs/003-token-usage-metrics/contracts/chat-api.md`
- [x] T012 [US2] Record specification readiness and scope boundaries in `specs/003-token-usage-metrics/checklists/requirements.md`

## Phase 5: Polish

- [x] T013 Update agent context to reference the active implementation plan in `.github/copilot-instructions.md`
- [x] T014 Run backend unit tests with Java 21 release targeting and shell syntax checks for the modified smoke tests

## Dependencies & Execution Order

- T001 and T002 are independent documentation tasks.
- T003 precedes T004 and the model metadata implementation.
- T005-T007 are sequential because the result must be mapped before the API can expose it.
- T008 and T009 can be authored independently after the typed contract exists; T010 is independent of Java implementation but validates the externally visible fields.
- T011/T012/T013 are documentation/context tasks and do not alter the API.
- T014 depends on all implementation and test changes.

## Parallel Opportunities

- T001/T002 can be prepared independently.
- T008 and T009 touch separate test files.
- T010 touches only shell test files and can be implemented independently of Java files.

## Implementation Strategy

Deliver User Story 1 as the MVP: retain the API and add metadata from the existing Spring AI result. User Story 2 is verified through the same controller and smoke tests. Run tests after all changes; no live Ollama service is required for unit tests.
