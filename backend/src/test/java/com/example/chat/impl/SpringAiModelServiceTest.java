package com.example.chat.impl;

import com.example.chat.ModelResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringAiModelServiceTest {

    @Test
    void mapsUsageModelFinishReasonAndEvaluationThroughput() {
        ChatResponse response = new ChatResponse(
                List.of(new Generation(
                        new AssistantMessage("respuesta"),
                        ChatGenerationMetadata.builder().finishReason("stop").build())),
                ChatResponseMetadata.builder()
                        .usage(new DefaultUsage(12, 6))
                        .model("llama3")
                        .keyValue("eval-duration", Duration.ofSeconds(2))
                        .build());

        ModelResult result = SpringAiModelService.toModelResult(response);

        assertThat(result.answer()).isEqualTo("respuesta");
        assertThat(result.promptTokens()).isEqualTo(12);
        assertThat(result.completionTokens()).isEqualTo(6);
        assertThat(result.totalTokens()).isEqualTo(18);
        assertThat(result.model()).isEqualTo("llama3");
        assertThat(result.finishReason()).isEqualTo("stop");
        assertThat(result.tokensPerSecond()).isEqualTo(3.0);
    }

    @Test
    void leavesThroughputUnavailableWhenEvaluationDurationIsMissingOrNotPositive() {
        ChatResponse withoutDuration = response(null);
        ChatResponse withZeroDuration = response(Duration.ZERO);
        ChatResponse withNegativeDuration = response(Duration.ofSeconds(-1));

        assertThat(SpringAiModelService.toModelResult(withoutDuration).tokensPerSecond()).isNull();
        assertThat(SpringAiModelService.toModelResult(withZeroDuration).tokensPerSecond()).isNull();
        assertThat(SpringAiModelService.toModelResult(withNegativeDuration).tokensPerSecond()).isNull();
    }

    @Test
    void leavesModelUnavailableWhenResponseMetadataHasNoModelName() {
        ChatResponse response = new ChatResponse(
                List.of(new Generation(new AssistantMessage("respuesta"))));

        ModelResult result = SpringAiModelService.toModelResult(response);

        assertThat(result.model()).isNull();
        assertThat(result.promptTokens()).isZero();
        assertThat(result.completionTokens()).isZero();
        assertThat(result.totalTokens()).isZero();
        assertThat(result.tokensPerSecond()).isNull();
    }

    private ChatResponse response(Duration evalDuration) {
        ChatResponseMetadata.Builder metadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage(12, 6))
                .model("llama3");
        if (evalDuration != null) {
            metadata.keyValue("eval-duration", evalDuration);
        }

        return new ChatResponse(
                List.of(new Generation(
                        new AssistantMessage("respuesta"),
                        ChatGenerationMetadata.builder().finishReason("stop").build())),
                metadata.build());
    }
}
