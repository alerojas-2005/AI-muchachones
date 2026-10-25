# Quickstart: Sampling Overrides

No rebuild or restart is needed to vary sampling for individual requests. With the backend running, send:

```sh
curl -X POST http://localhost:8080/api/v1/chat \
  -H 'Content-Type: application/json' \
  -d '{"question":"Give me a concise explanation","temperature":0.2,"seed":7}'
```

Unspecified parameters continue using configured values. Valid limits: temperature 0–2, topP 0–1, topK 0–200, numPredict 1–2048. Out-of-range values return 400 before Ollama is called.
