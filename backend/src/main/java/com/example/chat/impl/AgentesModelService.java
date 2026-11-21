package com.example.chat.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import com.example.chat.ModelResult;
import com.example.chat.ModelService;
import com.example.chat.planeador.PlaneadorOrquestador;

/**
 * Implementación de ModelService que usa el sistema multiagente.
 *
 * @Primary hace que el ChatController reciba ESTA implementación en lugar de
 * SpringAiModelService. El ChatController y el frontend no cambian: mismo contrato /api/v1/chat.
 */
@Service
@Primary
public class AgentesModelService implements ModelService {

    private static final Logger log = LoggerFactory.getLogger(AgentesModelService.class);

    private final PlaneadorOrquestador orquestador;
    private final String modelo;

    public AgentesModelService(PlaneadorOrquestador orquestador,
                               @Value("${spring.ai.ollama.chat.model:}") String modelo) {
        this.orquestador = orquestador;
        this.modelo = modelo;
    }

    @Override
    public ModelResult generateResponse(String question, String sessionId) {
        log.info("Planeador multiagente. sessionId={}", sessionId);
        String respuesta = orquestador.planear(question);
        return new ModelResult(respuesta == null ? "" : respuesta,
                null, null, null, modelo.isBlank() ? null : modelo, null, null);
    }
}