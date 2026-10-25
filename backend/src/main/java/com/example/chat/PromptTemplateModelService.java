package com.example.chat;

import com.example.chat.dto.ChatRequest;

public interface PromptTemplateModelService {
    String validate(ChatRequest request);
    PromptExecutionResult generate(String question, String sessionId, ChatRequest request);
}
