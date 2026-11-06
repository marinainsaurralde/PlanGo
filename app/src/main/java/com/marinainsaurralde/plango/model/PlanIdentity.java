package com.marinainsaurralde.plango.model;

public final class PlanIdentity {

    public static final String EVENTO_MADRID = "evento-madrid";
    public static final String PARQUE_MADRID = "parque-madrid";

    private PlanIdentity() {
    }

    public static String compose(String source, String sourceId) {
        String normalizedSource = normalize(source);
        String normalizedId = normalize(sourceId);
        if (normalizedSource == null || normalizedId == null) {
            return null;
        }
        return normalizedSource + ":" + normalizedId;
    }

    public static String favoriteDocumentId(String source, String sourceId) {
        String normalizedId = normalize(sourceId);
        if (normalizedId == null) {
            return null;
        }
        if (EVENTO_MADRID.equals(normalize(source))) {
            return normalizedId;
        }
        String composite = compose(source, sourceId);
        return composite == null ? null : composite.replace("%", "%25").replace("/", "%2F");
    }

    private static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
