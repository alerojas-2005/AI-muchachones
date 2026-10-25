#!/usr/bin/env bash
set -euo pipefail

URL="http://localhost:8080/api/v1/chat"
PAYLOAD='{"question":"Hello, demo","sessionId":"demo"}'

echo "Running smoke test against $URL"
RESPONSE=$(curl -s -H "Content-Type: application/json" -d "$PAYLOAD" "$URL" || true)

if [ -z "$RESPONSE" ]; then
  echo "No response from backend"
  exit 2
fi

# Basic check for non-empty answer field
echo "$RESPONSE" | grep -q "answer" || { echo "Response missing 'answer' field"; exit 3; }
for field in promptTokens completionTokens totalTokens model finishReason tokensPerSecond; do
  echo "$RESPONSE" | grep -Eq "\"$field\"[[:space:]]*:" || {
    echo "Response missing '$field' field"
    exit 4
  }
done

echo "Smoke test passed"
