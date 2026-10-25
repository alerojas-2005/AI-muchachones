package com.example.chat;

public record PromptExecutionResult(
        ModelResult modelResult,
        Object structuredAnswer,
        String promptTemplateId,
        String structuredOutputMode,
        com.example.chat.dto.ChatResponse.SamplingParameters samplingParameters) {
}
