# Chat API Contract

## `POST /api/v1/chat`

The existing endpoint and request shape are unchanged.

### Request

```json
{
  "question": "How are you?",
  "sessionId": "example-session"
}
```

### Successful response

The response is additive. Existing properties remain present; six metadata properties are added:

```json
{
  "requestId": "123e4567-e89b-12d3-a456-426614174000",
  "answer": "I'm ready to help.",
  "elapsedMs": 1200,
  "status": "ok",
  "promptTokens": 12,
  "completionTokens": 6,
  "totalTokens": 18,
  "model": "llama3",
  "finishReason": "stop",
  "tokensPerSecond": 30.0
}
```

All six metadata properties remain present in the response. Nullable model/finish metadata and uncalculable throughput are `null`; token counts are passed through from Spring AI, including zero values returned by its Ollama adapter when native counts are absent. `tokensPerSecond` is completion tokens divided by the model evaluation duration in seconds; it is null when either input is unavailable or duration is zero/negative.

### Compatibility

- The route remains `POST /api/v1/chat`.
- Request properties remain unchanged.
- `requestId`, `answer`, `elapsedMs`, and `status` keep their existing meanings and behavior.
- Existing consumers may ignore the added response properties.
- Validation and model errors keep their current HTTP status and response envelope.
