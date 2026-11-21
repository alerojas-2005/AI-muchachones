package com.example.chat.planeador.agentes;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.chat.planeador.herramientas.ClimaTools;
import com.example.chat.planeador.herramientas.PresupuestoTools;
import com.example.chat.planeador.herramientas.ReservasTools;

/**
 * Definición de los agentes.
 *
 * Cada agente = ChatClient + instrucciones (defaultSystem) + herramientas (defaultTools).
 * El ToolCallingAdvisor que Spring AI registra automáticamente ejecuta el ciclo:
 * el modelo pide una tool -> Spring la ejecuta -> el resultado vuelve al modelo...
 * hasta que el modelo responde sin pedir más tools.
 *
 * ChatClient.Builder es un bean "prototype": cada método @Bean recibe un builder NUEVO,
 * por eso podemos crear varios ChatClient independientes a partir del mismo modelo.
 */
@Configuration
public class AgentesConfig {

    static final String PROMPT_CLIMA = """
            Eres el AGENTE DE CLIMA de un planeador de salidas de fin de semana cerca de Bogotá.
            Tu único trabajo es informar el pronóstico del tiempo.
            - Usa SIEMPRE la herramienta consultarPronostico. Nunca inventes datos del clima.
            - Si el usuario no dice el día, consulta sábado y domingo.
            - Ignora las partes de la pregunta que no son de clima (dinero, reservas).
            - Responde en español, en máximo 3 frases, con una recomendación práctica (ropa, impermeable...).
            """;

    static final String PROMPT_PRESUPUESTO = """
            Eres el AGENTE DE PRESUPUESTO de un planeador de salidas de fin de semana.
            Tu único trabajo es calcular cuánto cuesta el plan y si el dinero alcanza.
            - Usa estimarCosto para obtener el costo total. Nunca hagas las cuentas de memoria.
            - Si el usuario dio un presupuesto, usa calcularSaldo con ese presupuesto y el costo total.
            - Escribe los valores en pesos como números enteros (300000, no 300.000) al llamar las herramientas.
            - Ignora las partes de la pregunta que no son de dinero.
            - Responde en español, en máximo 3 frases.
            """;

    static final String PROMPT_RESERVAS = """
            Eres el AGENTE DE RESERVAS de un planeador de salidas de fin de semana.
            Tu único trabajo es consultar cupos y crear reservas.
            - Usa consultarDisponibilidad antes de reservar.
            - Usa crearReserva SOLO si el usuario pidió reservar explícitamente y dio el nombre del responsable.
            - Si el usuario condicionó la reserva (por ejemplo "si no llueve" o "si nos alcanza") y los
              resultados de otros agentes muestran que la condición NO se cumple, NO reserves y explica por qué.
            - Responde en español, en máximo 3 frases. Si reservaste, incluye el código de la reserva.
            """;

    @Bean
    ChatClient agenteClima(ChatClient.Builder builder, ClimaTools tools) {
        return builder.defaultSystem(PROMPT_CLIMA).defaultTools(tools).build();
    }

    @Bean
    ChatClient agentePresupuesto(ChatClient.Builder builder, PresupuestoTools tools) {
        return builder.defaultSystem(PROMPT_PRESUPUESTO).defaultTools(tools).build();
    }

    @Bean
    ChatClient agenteReservas(ChatClient.Builder builder, ReservasTools tools) {
        return builder.defaultSystem(PROMPT_RESERVAS).defaultTools(tools).build();
    }
}