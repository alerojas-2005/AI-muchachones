package com.example.chat.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public class ChatRequest {
    private String sessionId;

    @NotBlank(message = "question must be provided")
    private String question;

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer numPredict;
    private Integer seed;
    private String promptTemplateId;
    private Map<String, String> templateVariables;
    private String structuredOutputMode;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Double getTopP() { return topP; }
    public void setTopP(Double topP) { this.topP = topP; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
    public Integer getNumPredict() { return numPredict; }
    public void setNumPredict(Integer numPredict) { this.numPredict = numPredict; }
    public Integer getSeed() { return seed; }
    public void setSeed(Integer seed) { this.seed = seed; }
    public String getPromptTemplateId() { return promptTemplateId; }
    public void setPromptTemplateId(String promptTemplateId) { this.promptTemplateId = promptTemplateId; }
    public Map<String, String> getTemplateVariables() { return templateVariables; }
    public void setTemplateVariables(Map<String, String> templateVariables) { this.templateVariables = templateVariables; }
    public String getStructuredOutputMode() { return structuredOutputMode; }
    public void setStructuredOutputMode(String structuredOutputMode) { this.structuredOutputMode = structuredOutputMode; }
}
