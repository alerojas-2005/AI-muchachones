package com.example.chat.impl;

import com.example.chat.ModelResult;
import com.example.chat.PromptExecutionResult;
import com.example.chat.PromptTemplateModelService;
import com.example.chat.dto.ChatRequest;
import com.example.chat.planeador.PlaneadorOrquestador;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * Conecta el sistema multiagente con el chat SIN tocar ChatController.
 *
 * En este repo el ChatController atiende primero con PromptTemplateModelService (el chat con RAG)
 * y solo usa ModelService si ese servicio no existe. Por eso AgentesModelService, por sí solo,
 * nunca se ejecuta. Esta clase es @Primary: el ChatController recibe ESTA implementación y las
 * preguntas del front llegan al orquestador. El chat con RAG se conserva intacto.
 *
 * Para volver al chat con RAG: planeador.enabled=false (o la variable PLANEADOR_ENABLED=false).
 */
@Service
@Primary
@ConditionalOnProperty(name = "planeador.enabled", havingValue = "true", matchIfMissing = true)
public class AgentesPromptTemplateModelService implements PromptTemplateModelService {

    private static final Logger log = LoggerFactory.getLogger(AgentesPromptTemplateModelService.class);

    private final PlaneadorOrquestador orquestador;
    private final String modelo;

    public AgentesPromptTemplateModelService(PlaneadorOrquestador orquestador,
                                             @Value("${spring.ai.ollama.chat.model:}") String modelo) {
        this.orquestador = orquestador;
        this.modelo = modelo;
    }

    /** El planeador no usa plantillas de prompt: no hay nada que validar. */
    @Override
    public String validate(ChatRequest request) {
        return null;
    }

    @Override
    public PromptExecutionResult generate(String question, String sessionId, ChatRequest request) {
        log.info("Planeador multiagente. sessionId={}", sessionId);
        String respuesta = orquestador.planear(question);
        ModelResult resultado = new ModelResult(
                respuesta == null ? "" : respuesta,
                null, null, null, modelo.isBlank() ? null : modelo, null, null);
        return new PromptExecutionResult(resultado, null, null, null, null);
    }
}