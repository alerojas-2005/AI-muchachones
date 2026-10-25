package com.example.chat;

import com.example.chat.dto.ChatRequest;
import com.example.chat.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerTest {

    @Test
    void returnsMetricsAlongsideExistingResponseProperties() {
        ChatController controller = new ChatController(
                (question, sessionId) -> new ModelResult(
                        "respuesta",
                        12,
                        6,
                        18,
                        "llama3",
                        "stop",
                        3.0));
        ChatRequest request = new ChatRequest();
        request.setQuestion("pregunta");
        request.setSessionId("session");

        ResponseEntity<ChatResponse> response = controller.chat(
                request,
                new BeanPropertyBindingResult(request, "chatRequest"));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        ChatResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getRequestId()).isNotBlank();
        assertThat(body.getAnswer()).isEqualTo("respuesta");
        assertThat(body.getElapsedMs()).isGreaterThanOrEqualTo(0);
        assertThat(body.getStatus()).isEqualTo("ok");
        assertThat(body.getPromptTokens()).isEqualTo(12);
        assertThat(body.getCompletionTokens()).isEqualTo(6);
        assertThat(body.getTotalTokens()).isEqualTo(18);
        assertThat(body.getModel()).isEqualTo("llama3");
        assertThat(body.getFinishReason()).isEqualTo("stop");
        assertThat(body.getTokensPerSecond()).isEqualTo(3.0);
    }
}
