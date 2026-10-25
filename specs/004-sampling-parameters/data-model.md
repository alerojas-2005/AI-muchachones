# Data Model: Per-request Sampling Parameters

## ChatRequest additions

| Field | Type | Optional | Accepted range |
|---|---|---:|---|
| temperature | Double | yes | 0.0–2.0 inclusive |
| topP | Double | yes | 0.0–1.0 inclusive |
| topK | Integer | yes | 0–200 inclusive |
| numPredict | Integer | yes | 1–2048 inclusive |
| seed | Integer | yes | any representable integer |

Null means use configured Ollama option. When the configured value is also absent, omit the option so Ollama supplies its own default.

## ChatResponse additions

- `samplingParameters`: effective values after config/request resolution; component values may be null where neither source supplies a value.
- `errorMessage`: field and accepted range on a sampling validation failure.

The existing request and response fields remain unchanged.
