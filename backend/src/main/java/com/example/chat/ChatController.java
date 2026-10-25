package com.example.chat;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/api/v1")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ModelService modelService;
    private final PromptTemplateModelService promptTemplateModelService;

    @Autowired
    public ChatController(ModelService modelService, PromptTemplateModelService promptTemplateModelService) {
        this.modelService = modelService;
        this.promptTemplateModelService = promptTemplateModelService;
    }

    /** Keeps the simple constructor useful for unit-level controller clients that don't test prompt selection. */
    public ChatController(ModelService modelService) {
        this(modelService, null);
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest req, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String err = bindingResult.getAllErrors().toString();
            log.warn("Validation failed for chat request: {}", err);
            ChatResponse bad = new ChatResponse(UUID.randomUUID().toString(), "", 0, "error");
            return ResponseEntity.badRequest().body(bad);
        }

        String samplingError = validateSamplingParameters(req);
        if (samplingError != null) {
            ChatResponse bad = new ChatResponse(UUID.randomUUID().toString(), "", 0, "error");
            bad.setErrorMessage(samplingError);
            return ResponseEntity.badRequest().body(bad);
        }

        String templateError = promptTemplateModelService == null ? null : promptTemplateModelService.validate(req);
        if (templateError != null) {
            ChatResponse bad = new ChatResponse(UUID.randomUUID().toString(), "", 0, "error");
            bad.setErrorMessage(templateError);
            return ResponseEntity.badRequest().body(bad);
        }

        log.info("Received chat request sessionId={} question={}", req.getSessionId(), req.getQuestion());
        long start = Instant.now().toEpochMilli();
        PromptExecutionResult promptResult = promptTemplateModelService == null
                ? null
                : promptTemplateModelService.generate(req.getQuestion(), req.getSessionId(), req);
        SamplingModelService.SamplingResult samplingResult = promptResult == null
                ? modelService instanceof SamplingModelService samplingService
                    ? samplingService.generateResponse(req.getQuestion(), req.getSessionId(), req)
                    : new SamplingModelService.SamplingResult(
                            modelService.generateResponse(req.getQuestion(), req.getSessionId()), null)
                : null;
        ModelResult result = promptResult != null ? promptResult.modelResult() : samplingResult.modelResult();
        if (result == null) {
            log.error("Model service returned no result. sessionId={}", req.getSessionId());
            result = new ModelResult("", null, null, null, null, null, null);
        }
        long elapsed = Instant.now().toEpochMilli() - start;

        String answer = result.answer();
        String status = (answer == null || answer.isBlank()) ? "error" : "ok";
        ChatResponse resp = new ChatResponse(
                UUID.randomUUID().toString(),
                answer == null ? "" : answer,
                elapsed,
                status,
                result.promptTokens(),
                result.completionTokens(),
                result.totalTokens(),
                result.model(),
                result.finishReason(),
                result.tokensPerSecond());
        resp.setSamplingParameters(promptResult != null ? promptResult.samplingParameters() : samplingResult.samplingParameters());
        if (promptResult != null) {
            resp.setStructuredAnswer(promptResult.structuredAnswer());
            resp.setPromptTemplateId(promptResult.promptTemplateId());
            resp.setStructuredOutputMode(promptResult.structuredOutputMode());
        }

        log.info("Chat response for sessionId={} elapsedMs={} status={} response={}", req.getSessionId(), elapsed, status, resp);
        return status.equals("ok") ? ResponseEntity.ok(resp) : ResponseEntity.status(502).body(resp);
    }

    private String validateSamplingParameters(ChatRequest request) {
        if (request.getTemperature() != null && (!Double.isFinite(request.getTemperature())
                || request.getTemperature() < 0.0 || request.getTemperature() > 2.0)) {
            return "temperature must be in range 0.0-2.0";
        }
        if (request.getTopP() != null && (!Double.isFinite(request.getTopP())
                || request.getTopP() < 0.0 || request.getTopP() > 1.0)) {
            return "topP must be in range 0.0-1.0";
        }
        if (request.getTopK() != null && (request.getTopK() < 0 || request.getTopK() > 200)) {
            return "topK must be in range 0-200";
        }
        if (request.getNumPredict() != null && (request.getNumPredict() < 1 || request.getNumPredict() > 2048)) {
            return "numPredict must be in range 1-2048";
        }
        return null;
    }
}
