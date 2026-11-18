
package com.example.chat.planeador.datos;

import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Datos simulados del planeador de fin de semana.
 *
 * En un sistema real, estos datos vendrían de servicios externos.
 * Para este taller se almacenan en memoria.
 */
@Component
public class DatosPlaneador {

    /** Costos por persona en pesos colombianos. */
    public record Destino(
            String nombre,
            int transporte,
            int entrada,
            int alimentacion
    ) {
        public int costoPorPersona() {
            return transporte + entrada + alimentacion;
        }
    }

    /** Pronóstico simulado del clima. */
    public record Pronostico(
            String cielo,
            int min,
            int max,
            int lluvia
    ) {}

    /** Información de una reserva creada. */
    public record Reserva(
            String codigo,
            String destino,
            String dia,
            int personas,
            String responsable
    ) {}

    private final Map<String, Destino> destinos = new LinkedHashMap<>();
    private final Map<String, String> alias = new LinkedHashMap<>();
    private final Map<String, Pronostico> pronosticos = new LinkedHashMap<>();
    private final Map<String, Integer> cupos = new ConcurrentHashMap<>();
    private final List<Reserva> reservas = new ArrayList<>();

    public DatosPlaneador() {

        // Destinos y costos por persona.
        destinos.put("guatavita",
                new Destino("Laguna de Guatavita", 30000, 25000, 35000));

        destinos.put("zipaquira",
                new Destino("Catedral de Sal de Zipaquirá", 20000, 50000, 30000));

        destinos.put("villa de leyva",
                new Destino("Villa de Leyva", 70000, 0, 45000));

        destinos.put("suesca",
                new Destino("Rocas de Suesca", 25000, 15000, 30000));

        destinos.put("chicaque",
                new Destino("Parque Natural Chicaque", 20000, 35000, 35000));

        // Alias para reconocer diferentes formas de nombrar los destinos.
        alias.put("laguna", "guatavita");
        alias.put("catedral", "zipaquira");
        alias.put("zipa", "zipaquira");
        alias.put("leiva", "villa de leyva");
        alias.put("leyva", "villa de leyva");
        alias.put("rocas", "suesca");

        // Pronósticos simulados por destino y día.
        pronosticos.put("guatavita|sabado",
                new Pronostico("parcialmente nublado", 8, 18, 30));
        pronosticos.put("guatavita|domingo",
                new Pronostico("lluvioso", 7, 15, 80));

        pronosticos.put("zipaquira|sabado",
                new Pronostico("nublado", 9, 19, 40));
        pronosticos.put("zipaquira|domingo",
                new Pronostico("soleado", 8, 20, 10));

        pronosticos.put("villa de leyva|sabado",
                new Pronostico("soleado", 12, 25, 5));
        pronosticos.put("villa de leyva|domingo",
                new Pronostico("soleado", 11, 24, 10));

        pronosticos.put("suesca|sabado",
                new Pronostico("soleado", 7, 21, 15));
        pronosticos.put("suesca|domingo",
                new Pronostico("tormentas en la tarde", 8, 17, 70));

        pronosticos.put("chicaque|sabado",
                new Pronostico("neblina en la mañana", 10, 19, 50));
        pronosticos.put("chicaque|domingo",
                new Pronostico("parcialmente nublado", 10, 20, 35));

        // Cupos disponibles por destino y día.
        cupos.put("guatavita|sabado", 12);
        cupos.put("guatavita|domingo", 4);

        cupos.put("zipaquira|sabado", 30);
        cupos.put("zipaquira|domingo", 25);

        cupos.put("villa de leyva|sabado", 2);
        cupos.put("villa de leyva|domingo", 10);

        cupos.put("suesca|sabado", 8);
        cupos.put("suesca|domingo", 6);

        cupos.put("chicaque|sabado", 0);
        cupos.put("chicaque|domingo", 15);
    }

    /** Normaliza texto: minúsculas, sin tildes y espacios sobrantes. */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }

        String sinTildes = Normalizer.normalize(
                texto,
                Normalizer.Form.NFD
        ).replaceAll("\\p{M}", "");

        return sinTildes.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("\\s+", " ");
    }

    /** Busca un destino por su nombre o alguno de sus alias. */
    public String buscarDestino(String texto) {
        String normalizado = normalizar(texto);

        if (normalizado.isEmpty()) {
            return null;
        }

        for (String clave : destinos.keySet()) {
            if (normalizado.contains(clave)
                    || (normalizado.length() >= 4 && clave.contains(normalizado))) {
                return clave;
            }
        }

        for (Map.Entry<String, String> entrada : alias.entrySet()) {
            if (normalizado.contains(entrada.getKey())) {
                return entrada.getValue();
            }
        }

        return null;
    }

    /** Normaliza sábado o domingo. */
    public static String normalizarDia(String texto) {
        String dia = normalizar(texto);

        if (dia.startsWith("sab")) {
            return "sabado";
        }

        if (dia.startsWith("dom")) {
            return "domingo";
        }

        return null;
    }

    /** Formatea cantidades en pesos colombianos. */
    public static String pesos(int valor) {
        return "$" + String.format(Locale.ROOT, "%,d", valor)
                .replace(',', '.');
    }

    /** Devuelve los nombres de los destinos disponibles. */
    public String destinosValidos() {
        return destinos.values().stream()
                .map(Destino::nombre)
                .collect(Collectors.joining(", "));
    }

    public Destino destino(String clave) {
        return destinos.get(clave);
    }

    public Pronostico pronostico(String clave, String dia) {
        return pronosticos.get(clave + "|" + dia);
    }

    public int cupos(String clave, String dia) {
        return cupos.getOrDefault(clave + "|" + dia, 0);
    }

    /** Registra una reserva y descuenta los cupos disponibles. */
    public synchronized Reserva reservar(
            String clave,
            String dia,
            int personas,
            String responsable
    ) {
        String llave = clave + "|" + dia;
        int disponibles = cupos.getOrDefault(llave, 0);

        if (personas <= 0 || personas > disponibles) {
            return null;
        }

        cupos.put(llave, disponibles - personas);

        Reserva reserva = new Reserva(
                "RES-%03d".formatted(reservas.size() + 1),
                clave,
                dia,
                personas,
                responsable
        );

        reservas.add(reserva);
        return reserva;
    }
}
