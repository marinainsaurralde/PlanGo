package com.marinainsaurralde.plango.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CategoriaTuristica {
    private static final String PREFIX = "turismo:";
    private static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        label("7172", "Instalaciones culturales");
        label("7173", "Edificios y monumentos");
        label("7174", "Parques y jardines");
        label("7171", "Parques y centros de ocio");
        label("7169", "Empresas de guías turísticos");
        label("7561", "Otros");
        label("7483", "Espacios para eventos");
        label("7175", "Servicios");
        label("7166", "Instalaciones deportivas");
        label("7168", "Escuelas de cocina y catas de vinos y aceites");
        label("7482", "Cotrabajo");
        label("7519", "Consignas");
    }

    private CategoriaTuristica() { }
    private static void label(String id, String value) { LABELS.put(PREFIX + id, value); }
    public static String key(String id) { return id == null ? null : PREFIX + id.trim(); }
    public static String labelFor(String key) { return LABELS.get(key); }
    public static List<String> labelsFor(Iterable<String> keys) {
        List<String> labels = new ArrayList<>();
        if (keys != null) for (String key : keys) {
            if (!key.startsWith(PREFIX)) continue;
            String label = LABELS.get(key);
            labels.add(label == null ? key.substring(PREFIX.length()) : label);
        }
        return labels;
    }
}
