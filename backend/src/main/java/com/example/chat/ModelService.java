package com.example.chat;

public interface ModelService {
    /**
     * Generate a response from the chosen model.
     * Implementations should handle communication with Spring AI / model client.
     */
    ModelResult generateResponse(String question, String sessionId);
}
