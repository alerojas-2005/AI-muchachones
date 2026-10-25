# Requirements Checklist: Prompt Templates and Structured Output

- [x] Client selects a registered prompt by ID, not by sending system text.
- [x] Templates are classpath resources, rendered at request time.
- [x] Extractor JSON example uses angle-bracket delimiters for variables.
- [x] Typed output uses required-schema fields and the correct Jackson annotation package.
- [x] Framework validation/retry and provider schema are separate selectable modes.
- [x] API changes are additive on `/api/v1`.
- [x] Existing feature 003/004 response fields and sampling behavior are preserved.
- [x] Template input-token cost method is documented.
- [ ] Record actual llama3.2:3b prompt-token delta for every template.
- [ ] Verify Ollama itself enforces provider `format` using a live model request.
