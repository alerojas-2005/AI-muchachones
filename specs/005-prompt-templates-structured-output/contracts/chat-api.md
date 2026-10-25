# Chat API Contract: Templates and Structured Output

Existing `POST /api/v1/chat` remains the only route. Existing request fields and response envelope remain compatible.

Additive request fields:

```json
{
  "question": "Extract the book details",
  "promptTemplateId": "book-extractor",
  "templateVariables": { "language": "English" },
  "structuredOutputMode": "FRAMEWORK_VALIDATION"
}
```

`promptTemplateId` references a server registry entry. The client never sends system-prompt text. Supported modes are `FRAMEWORK_VALIDATION`, `PROVIDER_SCHEMA`, and `BOTH`; ordinary free-text chat remains the default if mode is omitted.

The response keeps `answer` and existing metrics. Structured modes add `structuredAnswer` as the typed domain object and `structuredOutputMode` to identify the mechanism used. Invalid template ID/variable input returns HTTP 400 before model invocation.

Successful structured response additions look like:

```json
{
  "promptTemplateId": "book-extractor",
  "structuredOutputMode": "FRAMEWORK_VALIDATION",
  "structuredAnswer": {
    "title": "Dune",
    "summary": "A desert planet"
  }
}
```
