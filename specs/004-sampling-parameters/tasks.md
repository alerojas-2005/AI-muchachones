# Tasks: Per-request Sampling Parameters

## 1. Request and validation

- [x] Add nullable wrapper fields for temperature, topP, topK, numPredict, and seed.
- [x] Validate defined ranges explicitly in the controller before calling the model service.
- [ ] Confirm each invalid field returns 400 and never invokes the model service.

## 2. Option resolution and response

- [x] Inspect Spring AI 2.0.0 source for builder names and option merge behavior.
- [x] Resolve each field from request override or configured model options; retain model identity.
- [x] Call the model with `OllamaChatOptions` request options.
- [x] Add effective parameters in an additive response property.
- [ ] Verify configured values survive a partial override and unspecified Ollama values remain unset.

## 3. Quality gates

- [ ] Add controller tests for all boundaries, each rejection, and no service invocation on 400.
- [ ] Add service tests for per-field override and default merge semantics.
- [ ] Verify the existing `/api/v1/chat` response fields, including feature 003 metrics, remain present.
- [ ] Run backend tests and the existing smoke/acceptance checks.

**Coverage note**: Principle V remains at risk until these tests exist; current coverage is minimal.
