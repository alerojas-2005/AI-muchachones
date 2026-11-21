package com.example.chat.planeador;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.chat.planeador.datos.DatosPlaneador;
import com.example.chat.planeador.herramientas.ReservasTools;

/** Human-in-the-loop: crearReserva no reserva; solo confirmar() lo hace. */
class ConfirmacionReservaTest {

    @Test
    void crearReservaDejaPendienteYNoDescuentaCupos() {
        DatosPlaneador datos = new DatosPlaneador();
        ReservasTools tools = new ReservasTools(datos);
        int antes = datos.cupos(datos.buscarDestino("Zipaquirá"), "sabado");

        String r = tools.crearReserva("Zipaquirá", "sabado", 2, "Ana");

        assertTrue(r.contains("PENDIENTE"));
        assertTrue(tools.hayPendiente(null));
        assertEquals(antes, datos.cupos(datos.buscarDestino("Zipaquirá"), "sabado"));
    }

    @Test
    void confirmarCreaLaReservaUnaSolaVez() {
        DatosPlaneador datos = new DatosPlaneador();
        ReservasTools tools = new ReservasTools(datos);
        String clave = datos.buscarDestino("Zipaquirá");
        int antes = datos.cupos(clave, "sabado");

        tools.usarSesion("s1");
        tools.crearReserva("Zipaquirá", "sabado", 2, "Ana");
        tools.limpiarSesion();
        assertTrue(tools.hayPendiente("s1"));

        String ok = tools.confirmar("s1");
        assertTrue(ok.startsWith("Reserva confirmada RES-"), ok);
        assertEquals(antes - 2, datos.cupos(clave, "sabado"));

        assertFalse(tools.hayPendiente("s1"));
        assertTrue(tools.confirmar("s1").startsWith("No hay ninguna reserva pendiente"));
        assertEquals(antes - 2, datos.cupos(clave, "sabado"));
    }

    @Test
    void descartarCancelaLaReserva() {
        DatosPlaneador datos = new DatosPlaneador();
        ReservasTools tools = new ReservasTools(datos);
        tools.usarSesion("s1");
        tools.crearReserva("Zipaquirá", "sabado", 2, "Ana");
        tools.limpiarSesion();

        tools.descartar("s1");

        assertFalse(tools.hayPendiente("s1"));
        assertTrue(tools.confirmar("s1").startsWith("No hay ninguna reserva pendiente"));
    }

    @Test
    void laConfirmacionEsPorSesion() {
        DatosPlaneador datos = new DatosPlaneador();
        ReservasTools tools = new ReservasTools(datos);
        tools.usarSesion("s1");
        tools.crearReserva("Zipaquirá", "sabado", 2, "Ana");
        tools.limpiarSesion();

        assertTrue(tools.confirmar("otra").startsWith("No hay ninguna reserva pendiente"));
        assertTrue(tools.hayPendiente("s1"));
    }
}