#!/usr/bin/env bash
set -euo pipefail

# Simple integration test: POST to backend and expect non-empty answer
URL="http://localhost:8080/api/v1/chat"
PAYLOAD='{"question":"Integration test","sessionId":"it"}'

RESPONSE=$(curl -s -H "Content-Type: application/json" -d "$PAYLOAD" "$URL" || true)

if [ -z "$RESPONSE" ]; then
  echo "No response"
  exit 2
fi

echo "Response: $RESPONSE"
echo "$RESPONSE" | grep -q 'answer' || { echo "Missing answer field"; exit 3; }
for field in promptTokens completionTokens totalTokens model finishReason tokensPerSecond; do
  echo "$RESPONSE" | grep -Eq "\"$field\"[[:space:]]*:" || {
    echo "Missing $field field"
    exit 4
  }
done

echo "Integration smoke test passed"
