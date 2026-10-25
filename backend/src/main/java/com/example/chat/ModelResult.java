package com.example.chat;

public record ModelResult(
        String answer,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        String model,
        String finishReason,
        Double tokensPerSecond) {
}
