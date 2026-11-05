package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
        put(values, "source", evento.getSource());
        put(values, "sourceId", evento.getSourceId());
        values.put("planTypes", enumNames(evento.getPlanTypes()));
        put(values, "temporalKind", evento.getTemporalKind().name());
        values.put("categoryKeys", new ArrayList<>(evento.getCategoryKeys()));
        put(values, "description", evento.getDescription());
        put(values, "address", evento.getAddress());
        put(values, "schedule", evento.getSchedule());
        put(values, "priceStatus", evento.getPriceStatus().name());
        put(values, "lastSourceUpdate", evento.getLastSourceUpdate());
        values.put("gratuidadInformada", evento.isGratuidadInformada());
        return values;
    }

    public static Evento fromMap(String documentId, Map<String, Object> values) {
        String id = string(values.get("idEvento"));
        String sourceId = first(string(values.get("sourceId")), id, documentId);
        String source = first(string(values.get("source")), PlanIdentity.EVENTO_MADRID);
        PriceStatus priceStatus = priceStatus(values);
        Plan.Builder plan = Plan.builder(source, sourceId, string(values.get("titulo")))
                .planTypes(planTypes(values.get("planTypes")))
                .temporalKind(enumValue(TemporalKind.class, values.get("temporalKind"), TemporalKind.EVENTO))
                .categoryKeys(strings(values.get("categoryKeys")))
                .description(string(values.get("description")))
                .address(string(values.get("address")))
                .schedule(string(values.get("schedule")))
                .priceStatus(priceStatus)
                .lastSourceUpdate(string(values.get("lastSourceUpdate")))
                .distrito(string(values.get("distrito")))
                .coordinates(number(values.get("latitud")), number(values.get("longitud")))
                .nombreInstalacion(string(values.get("instalacion")))
                .urlOficial(string(values.get("urlOficial")));
        return new Evento(
                plan,
                string(values.get("fechaInicio")),
                string(values.get("fechaFin")),
                string(values.get("tipo")),
                priceStatus == PriceStatus.GRATIS,
                priceStatus != PriceStatus.DESCONOCIDO,
                string(values.get("diasSemana")),
                string(values.get("diasExcluidos")),
                string(values.get("hora")),
                string(values.get("precio"))
        );
    }

    private static PriceStatus priceStatus(Map<String, Object> values) {
        PriceStatus explicit = enumValue(PriceStatus.class, values.get("priceStatus"), null);
        if (explicit != null) return explicit;
        if (bool(values.get("gratuito"))) return PriceStatus.GRATIS;
        if (Boolean.TRUE.equals(values.get("gratuidadInformada"))
                || string(values.get("precio")) != null) return PriceStatus.DE_PAGO;
        return PriceStatus.DESCONOCIDO;
    }

    private static Set<PlanType> planTypes(Object value) {
        Set<PlanType> result = new LinkedHashSet<>();
        for (String item : strings(value)) {
            PlanType type = enumValue(PlanType.class, item, null);
            if (type != null) result.add(type);
        }
        if (result.isEmpty()) result.add(PlanType.CULTURA);
        return result;
    }

    private static List<String> enumNames(Iterable<? extends Enum<?>> values) {
        List<String> names = new ArrayList<>();
        for (Enum<?> value : values) names.add(value.name());
        return names;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof Iterable<?>)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object item : (Iterable<?>) value) {
            String text = string(item);
            if (text != null) result.add(text);
        }
        return result;
    }

    private static <T extends Enum<T>> T enumValue(
            Class<T> type, Object value, T fallback) {
        String text = string(value);
        if (text == null) return fallback;
        try {
            return Enum.valueOf(type, text);
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }

    private static String first(String... values) {
        for (String value : values) if (value != null && !value.trim().isEmpty()) return value;
        return null;
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
