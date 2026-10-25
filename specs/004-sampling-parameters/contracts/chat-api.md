# Chat API Contract — Sampling Parameters

Existing route remains `POST /api/v1/chat`.

Request fields are additive and optional:

```json
{
  "question": "Explain temperature settings",
  "temperature": 0.2,
  "topP": 0.9,
  "topK": 40,
  "numPredict": 512,
  "seed": 7
}
```

Omitted/null values inherit `spring.ai.ollama.chat.*` settings. A successful response retains the current response and metrics properties and adds:

```json
"samplingParameters": {
  "temperature": 0.2,
  "topP": 0.9,
  "topK": 40,
  "numPredict": 512,
  "seed": 7
}
```

Invalid values return HTTP 400 before model invocation. Example for `temperature: 17`:

```json
{
  "requestId": "…",
  "answer": "",
  "elapsedMs": 0,
  "status": "error",
  "errorMessage": "temperature must be in range 0.0-2.0"
}
```

The validation error keeps the established response envelope and adds `errorMessage`.
