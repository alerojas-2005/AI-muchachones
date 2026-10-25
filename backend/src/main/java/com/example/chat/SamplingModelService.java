package com.example.chat;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;

/** Optional per-request sampling capability, separate from the existing model result contract. */
public interface SamplingModelService extends ModelService {

    SamplingResult generateResponse(String question, String sessionId, ChatRequest request);

    record SamplingResult(ModelResult modelResult, ChatResponse.SamplingParameters samplingParameters) {}
}
