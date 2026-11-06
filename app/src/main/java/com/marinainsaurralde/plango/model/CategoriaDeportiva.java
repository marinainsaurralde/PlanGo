package com.marinainsaurralde.plango.model;

public enum CategoriaDeportiva {
    ACTIVIDADES_DEPORTIVAS("deporte:actividades-deportivas", "Actividades deportivas"),
    CICLISMO("deporte:ciclismo", "Ciclismo"),
    CARRERAS_MARATONES("deporte:carreras-maratones", "Carreras y maratones"),
    SKATEBOARDING("deporte:skateboarding", "Skateboarding"),
    GOLF("deporte:golf", "Golf"),
    CURSOS_TALLERES("deporte:cursos-talleres", "Cursos y talleres"),
    SIN_CLASIFICAR("deporte:sin-clasificar", "Sin clasificar");

    private final String key;
    private final String label;

    CategoriaDeportiva(String key, String label) {
        this.key = key;
        this.label = label;
    }

    public String getKey() { return key; }
    public String getLabel() { return label; }

    public static CategoriaDeportiva fromApiType(String apiType) {
        if (apiType == null || apiType.trim().isEmpty()) return SIN_CLASIFICAR;
        String value = apiType.substring(apiType.lastIndexOf('/') + 1);
        switch (value) {
            case "ActividadesDeportivas": return ACTIVIDADES_DEPORTIVAS;
            case "Ciclismo": return CICLISMO;
            case "CarrerasMaratones": return CARRERAS_MARATONES;
            case "Skateboarding": return SKATEBOARDING;
            case "Golf": return GOLF;
            case "CursosTalleres": return CURSOS_TALLERES;
            default: return SIN_CLASIFICAR;
        }
    }
}
