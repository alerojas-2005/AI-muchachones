
package com.example.chat.planeador;

import com.example.chat.planeador.datos.DatosPlaneador;
import com.example.chat.planeador.herramientas.PresupuestoTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PresupuestoToolsTest {

    private DatosPlaneador datos;
    private PresupuestoTools herramientas;

    @BeforeEach
    void configurar() {
        datos = new DatosPlaneador();
        herramientas = new PresupuestoTools(datos);
    }

    @Test
    void costoDeZipaquiraParaCuatroPersonas() {
        String resultado = herramientas.estimarCosto("Zipaquirá", 4);

        assertTrue(resultado.contains("$400.000"), resultado);
    }

    @Test
    void costoDeGuatavitaParaUnaPersona() {
        String resultado = herramientas.estimarCosto("Guatavita", 1);

        assertTrue(resultado.contains("$90.000"), resultado);
    }

    @Test
    void saldoNegativoIndicaCuantoFalta() {
        String resultado = herramientas.calcularSaldo(350000, 400000);

        assertTrue(resultado.contains("faltan $50.000"), resultado);
    }

    @Test
    void saldoPositivoIndicaCuantoSobra() {
        String resultado = herramientas.calcularSaldo(250000, 210000);

        assertTrue(resultado.contains("sobran $40.000"), resultado);
    }

    @Test
    void destinoDesconocidoMuestraDestinosValidos() {
        String resultado = herramientas.estimarCosto("Medellín", 2);

        assertTrue(resultado.contains("Destinos válidos"), resultado);
    }
}
