# Tasks: Prompt Templates and Structured Output

## 1. Specify and verify framework behavior

- [x] Inspect resolved 2.0.0 JARs for `StTemplateRenderer`, `BeanOutputConverter`, and `ChatClient.EntityParamSpec` packages.
- [x] Confirm entity consumer and delimiter builder method signatures with `javap`.
- [x] Run and capture default-delimiter JSON-template failure verbatim (`IllegalArgumentException: The template string is not valid.`).
- [x] Run the same template with `<`/`>` and verify literal JSON rendering.
- [x] Invoke `entity()` with sampling overrides and verify they survive in the outgoing Ollama request options.
- [x] Compare framework validation/retry with provider schema using identical input against a loopback Ollama-compatible HTTP simulation.
- [ ] Run the same comparison against an actual Ollama daemon/model; confirm provider enforcement.
- [ ] Measure per-template prompt-token delta against same-question llama3.2:3b baseline.

## 2. Implementation (after live checks)

- [x] Add classpath prompt resources and an ID/variable allowlist registry.
- [x] Configure shared angle-bracket `StTemplateRenderer` and request-selected system resource.
- [x] Add required-field structured domain type using `com.fasterxml.jackson.annotation` imports.
- [x] Add framework, provider, and optional combined structured-output modes.
- [x] Preserve sampling options in typed `.entity()` requests using the Spring AI options builder.
- [x] Extend response additively and keep unstructured output as default.
- [x] Add runtime verification tests; preserve existing feature 003/004 fields and behavior.

**Gate**: Spring AI behavior checks passed against a loopback Ollama-compatible simulation. Actual llama3.2:3b enforcement and per-template token deltas remain pending because no Ollama daemon was available. The pre-existing feature 004 source compile error was temporarily corrected solely to run the compile/harness, then restored; it prevents an unmodified full build.
