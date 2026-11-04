package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;

import java.util.HashMap;
import java.util.Map;

public final class FavoritoMapper {

    private FavoritoMapper() {
    }

    public static Map<String, Object> toMap(Evento evento) {
        Map<String, Object> values = new HashMap<>();
        put(values, "idEvento", evento.getIdentificador());
        put(values, "titulo", evento.getTitulo());
        put(values, "fechaInicio", evento.getFechaInicio());
        put(values, "fechaFin", evento.getFechaFin());
        put(values, "hora", evento.getHora());
        put(values, "instalacion", evento.getNombreInstalacion());
        put(values, "distrito", evento.getDistrito());
        put(values, "precio", evento.getPrecio());
        values.put("gratuito", evento.isGratuito());
        put(values, "tipo", evento.getTipo());
        put(values, "latitud", evento.getLatitud());
        put(values, "longitud", evento.getLongitud());
        put(values, "urlOficial", evento.getUrlOficial());
        put(values, "diasSemana", evento.getDiasSemana());
        put(values, "diasExcluidos", evento.getDiasExcluidos());
        return values;
    }

    public static Evento fromMap(String documentId, Map<String, Object> values) {
        String id = string(values.get("idEvento"));
        return new Evento(
                id == null ? documentId : id,
                string(values.get("titulo")),
                string(values.get("fechaInicio")),
                string(values.get("fechaFin")),
                string(values.get("distrito")),
                string(values.get("tipo")),
                bool(values.get("gratuito")),
                number(values.get("latitud")),
                number(values.get("longitud")),
                string(values.get("instalacion")),
                string(values.get("diasSemana")),
                string(values.get("diasExcluidos")),
                string(values.get("hora")),
                string(values.get("precio")),
                string(values.get("urlOficial"))
        );
    }

    private static void put(Map<String, Object> values, String key, Object value) {
        if (value != null && (!(value instanceof String) || !((String) value).trim().isEmpty())) {
            values.put(key, value);
        }
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Double number(Object value) {
        return value instanceof Number ? ((Number) value).doubleValue() : null;
    }

    private static boolean bool(Object value) {
        return value instanceof Boolean && (Boolean) value;
    }
}
