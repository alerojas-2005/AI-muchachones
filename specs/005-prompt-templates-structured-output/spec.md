# Feature Specification: Prompt Templates and Structured Output

**Feature Branch**: `005-prompt-templates-structured-output`  
**Created**: 2026-10-15  
**Status**: Implemented; model-dependent token measurements pending

## User Scenarios

### Story 1 — Select a registered system prompt template (P1)

As an API client, I can select a server-owned prompt template by identifier and provide its permitted variables, so prompt variants can be changed without sending system prompt text or rebuilding Java.

### Story 2 — Request domain-shaped output by two mechanisms (P1)

As an API client, I can ask for a typed domain result using either Spring AI framework validation with error-feedback retries or Ollama's native JSON schema constraint, and compare their behavior.

## Functional Requirements

- **FR-001**: Keep `POST /api/v1/chat`; add optional request fields for `promptTemplateId`, template variables, and structured-output mode.
- **FR-002**: The client MUST select a registered template identifier; it MUST NOT send arbitrary system-prompt text.
- **FR-003**: System prompt templates MUST live as classpath resources under `src/main/resources/prompts/` and be rendered at request time.
- **FR-004**: Unknown template identifiers and variables not permitted by that template MUST be rejected with HTTP 400 before model invocation.
- **FR-005**: A JSON example in an extractor template MUST remain literal text and MUST NOT be treated as template variables.
- **FR-006**: Structured output MUST map to a typed domain entity whose schema marks required fields.
- **FR-007**: Support framework-validation mode using `EntityParamSpec.validateSchema()` and provider-schema mode using `EntityParamSpec.useProviderStructuredOutput()` independently. A combined mode may activate both explicitly, but comparison requests select exactly one mechanism.
- **FR-008**: Framework validation MUST validate the generated entity schema and retry with validation error feedback when invalid. Provider mode MUST pass the generated schema through Ollama's native `format` option.
- **FR-009**: Preserve all existing response fields and add the typed structured result/template metadata without changing API version.
- **FR-010**: A template's token overhead MUST be reported/measured from actual prompt token usage; the specification must state that local Ollama has no per-token invoice, while each additional input token adds prompt-evaluation work and latency. Measure overhead against a no-template baseline using the same model and question.

## Decisions to Record

- Clients send identifiers instead of prompt strings to keep system instructions server-controlled, avoid arbitrary prompt policy changes, bound template size, and make allowed variable names explicit. A variable value is still untrusted user data and must not become a template identifier or template source.
- Use `<` and `>` as template delimiters so JSON braces in the extractor example remain literal.
- Report measured additional prompt tokens per template and per-turn cost as extra local inference work; do not invent a monetary amount for local Ollama.

## Scope Boundaries

No changes to `specs/001`, `specs/003`, or `specs/004`. Memory/history (006) and guardrails (007) are out of scope. No user-provided prompt text, persistence, or frontend redesign.
