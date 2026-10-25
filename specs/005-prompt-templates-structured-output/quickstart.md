# Quickstart: Template/Output Comparison

After implementation and once Ollama is running with `llama3.2:3b`, send the same request twice, changing only `structuredOutputMode` between `FRAMEWORK_VALIDATION` and `PROVIDER_SCHEMA`. Keep template ID, variables, question, and sampling options fixed. Compare `structuredAnswer`, retries, usage, and latency.

To measure template overhead, repeat the same question once with no template and once per registered template. Use response `promptTokens`; report each delta and the local inference cost (additional prompt evaluation/latency). Do not interpret local usage as a token-billed charge.
