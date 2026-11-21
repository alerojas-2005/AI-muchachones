package com.example.chat.planeador.herramientas;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.example.chat.planeador.datos.DatosPlaneador;

/**
 * Tools del AGENTE DE RESERVAS.
 *
 * Human-in-the-loop: crearReserva ya NO modifica datos. Solo valida y deja la reserva PENDIENTE.
 * La reserva real se hace en confirmar(), que el código llama (no el modelo) cuando el usuario
 * responde "confirmo" en el mensaje siguiente.
 */
@Component
public class ReservasTools {

    /** Reserva validada que espera el "confirmo" del usuario. */
    public record Pendiente(String clave, String dia, int personas, String responsable) {}

    private static final String SESION_POR_DEFECTO = "default";

    private final DatosPlaneador datos;
    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();
    private final ThreadLocal<String> sesionActual = new ThreadLocal<>();

    public ReservasTools(DatosPlaneador datos) {
        this.datos = datos;
    }

    @Tool(description = "Consulta cuántos cupos quedan para un destino el sábado o el domingo.")
    public String consultarDisponibilidad(
            @ToolParam(description = "Nombre del destino") String destino,
            @ToolParam(description = "sabado o domingo") String dia) {

        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s."
                    .formatted(destino, datos.destinosValidos());
        }

        String d = DatosPlaneador.normalizarDia(dia);
        if (d == null) {
            return "Solo se puede reservar para 'sabado' o 'domingo'.";
        }

        return "%s el %s: quedan %d cupos."
                .formatted(datos.destino(clave).nombre(), d, datos.cupos(clave, d));
    }

    @Tool(description = "Prepara una reserva para un grupo y la deja PENDIENTE de confirmación del usuario "
            + "(NO queda reservada todavía). Úsala SOLO si el usuario pidió reservar y dio el nombre "
            + "del responsable.")
    public String crearReserva(
            @ToolParam(description = "Nombre del destino") String destino,
            @ToolParam(description = "sabado o domingo") String dia,
            @ToolParam(description = "Número de personas") int personas,
            @ToolParam(description = "Nombre de la persona responsable de la reserva") String responsable) {

        String clave = datos.buscarDestino(destino);
        if (clave == null) {
            return "No conozco el destino '%s'. Destinos válidos: %s."
                    .formatted(destino, datos.destinosValidos());
        }

        String d = DatosPlaneador.normalizarDia(dia);
        if (d == null) {
            return "Solo se puede reservar para 'sabado' o 'domingo'.";
        }

        if (personas <= 0) {
            return "El número de personas debe ser mayor que cero.";
        }

        if (responsable == null || responsable.isBlank()) {
            return "Falta el nombre del responsable de la reserva. Pídeselo al usuario.";
        }

        int cupos = datos.cupos(clave, d);
        if (personas > cupos) {
            return "No hay cupo suficiente: se piden %d y quedan %d.".formatted(personas, cupos);
        }

        pendientes.put(sesion(), new Pendiente(clave, d, personas, responsable.trim()));

        return "RESERVA PENDIENTE (todavía NO está reservada): %s, %s, %d personas, a nombre de %s. "
                .formatted(datos.destino(clave).nombre(), d, personas, responsable.trim())
                + "El usuario debe responder 'confirmo' en su siguiente mensaje para completarla.";
    }

    // ---- Confirmación humana: las llama el servicio, NO el modelo (no llevan @Tool) ----

    /** Asocia las tools de este hilo a una sesión de chat. Llamar antes de ejecutar el orquestador. */
    public void usarSesion(String sessionId) {
        sesionActual.set(sessionId == null || sessionId.isBlank() ? SESION_POR_DEFECTO : sessionId);
    }

    public void limpiarSesion() {
        sesionActual.remove();
    }

    public boolean hayPendiente(String sessionId) {
        return pendientes.containsKey(clave(sessionId));
    }

    /** Resumen de la reserva pendiente, o null si no hay. */
    public String resumenPendiente(String sessionId) {
        Pendiente p = pendientes.get(clave(sessionId));
        if (p == null) {
            return null;
        }
        return "Resumen de la reserva pendiente: %s, %s, %d personas, a nombre de %s."
                .formatted(datos.destino(p.clave()).nombre(), p.dia(), p.personas(), p.responsable());
    }

    /** Descarta la reserva pendiente (el usuario no confirmó). */
    public void descartar(String sessionId) {
        pendientes.remove(clave(sessionId));
    }

    /** Crea de verdad la reserva pendiente. Se consume: no se puede confirmar dos veces. */
    public String confirmar(String sessionId) {
        Pendiente p = pendientes.remove(clave(sessionId));
        if (p == null) {
            return "No hay ninguna reserva pendiente por confirmar.";
        }

        int cupos = datos.cupos(p.clave(), p.dia());
        if (p.personas() > cupos) {
            return "No hay cupo suficiente: se piden %d y quedan %d. La reserva no se creó."
                    .formatted(p.personas(), cupos);
        }

        DatosPlaneador.Reserva r = datos.reservar(p.clave(), p.dia(), p.personas(), p.responsable());
        if (r == null) {
            return "No se pudo registrar la reserva porque los cupos cambiaron. Consulta la disponibilidad otra vez.";
        }

        return "Reserva confirmada %s: %s, %s, %d personas, a nombre de %s."
                .formatted(r.codigo(), datos.destino(p.clave()).nombre(), p.dia(), p.personas(), r.responsable());
    }

    private String sesion() {
        String s = sesionActual.get();
        return s == null ? SESION_POR_DEFECTO : s;
    }

    private static String clave(String sessionId) {
        return sessionId == null || sessionId.isBlank() ? SESION_POR_DEFECTO : sessionId;
    }
}