package com.marinainsaurralde.plango.model;

import java.text.Normalizer;
import java.util.Locale;

public enum CategoriaCultural {
    TEATRO_ESPECTACULOS,
    MUSICA,
    CINE,
    EXPOSICIONES,
    TALLERES_ACTIVIDADES,
    CONFERENCIAS,
    OTRAS;

    public static CategoriaCultural clasificar(Evento evento) {
        String raw = evento == null ? null : evento.getTipo();
        if (raw == null || raw.trim().isEmpty()) return OTRAS;
        String value = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        if (containsAny(value, "teatro", "performance", "danza", "baile", "circo", "magia",
                "titeres", "marionetas")) return TEATRO_ESPECTACULOS;
        if (containsAny(value, "musica", "concierto")) return MUSICA;
        if (containsAny(value, "cine", "audiovisual")) return CINE;
        if (containsAny(value, "exposicion", "museo")) return EXPOSICIONES;
        if (containsAny(value, "cursostalleres", "curso", "taller", "clubeslectura",
                "actividadesescolares", "campamento")) return TALLERES_ACTIVIDADES;
        if (containsAny(value, "conferenciascoloquios", "conferencia", "coloquio",
                "congreso", "jornada")) return CONFERENCIAS;
        return OTRAS;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) if (value.contains(needle)) return true;
        return false;
    }
}
