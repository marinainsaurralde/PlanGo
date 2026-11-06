package com.marinainsaurralde.plango.model;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class Plan implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String source;
    private final String sourceId;
    private final String titulo;
    private final Set<PlanType> planTypes;
    private final TemporalKind temporalKind;
    private final Set<String> categoryKeys;
    private final String description;
    private final String address;
    private final String schedule;
    private final String priceInfo;
    private final PriceStatus priceStatus;
    private final String lastSourceUpdate;
    private final String distrito;
    private final Double latitud;
    private final Double longitud;
    private final String nombreInstalacion;
    private final String urlOficial;
    private final Map<String, String> sourceRefs;

    protected Plan(Builder builder) {
        source = normalize(builder.source);
        sourceId = normalize(builder.sourceId);
        titulo = normalize(builder.titulo);
        planTypes = immutableCopy(builder.planTypes);
        temporalKind = builder.temporalKind == null ? TemporalKind.EVENTO : builder.temporalKind;
        categoryKeys = immutableStrings(builder.categoryKeys);
        description = normalize(builder.description);
        address = normalize(builder.address);
        schedule = normalize(builder.schedule);
        priceInfo = normalize(builder.priceInfo);
        priceStatus = builder.priceStatus == null ? PriceStatus.DESCONOCIDO : builder.priceStatus;
        lastSourceUpdate = normalize(builder.lastSourceUpdate);
        distrito = normalize(builder.distrito);
        latitud = builder.latitud;
        longitud = builder.longitud;
        nombreInstalacion = normalize(builder.nombreInstalacion);
        urlOficial = normalize(builder.urlOficial);
        LinkedHashMap<String, String> refs = new LinkedHashMap<>(builder.sourceRefs);
        if (source != null && sourceId != null) refs.put(source, sourceId);
        sourceRefs = Collections.unmodifiableMap(refs);
    }

    public String getIdentificador() { return sourceId; }
    public String getSource() { return source; }
    public String getSourceId() { return sourceId; }
    public String getCompositeId() { return PlanIdentity.compose(source, sourceId); }
    public String getFavoriteDocumentId() { return PlanIdentity.favoriteDocumentId(source, sourceId); }
    public String getTitulo() { return titulo; }
    public Set<PlanType> getPlanTypes() { return planTypes; }
    public TemporalKind getTemporalKind() { return temporalKind; }
    public Set<String> getCategoryKeys() { return categoryKeys; }
    public String getDescription() { return description; }
    public String getAddress() { return address; }
    public String getSchedule() { return schedule; }
    public String getPriceInfo() { return priceInfo; }
    public PriceStatus getPriceStatus() { return priceStatus; }
    public String getLastSourceUpdate() { return lastSourceUpdate; }
    public String getDistrito() { return distrito; }
    public Double getLatitud() { return latitud; }
    public Double getLongitud() { return longitud; }
    public String getNombreInstalacion() { return nombreInstalacion; }
    public String getUrlOficial() { return urlOficial; }
    public Map<String, String> getSourceRefs() { return sourceRefs; }

    public boolean hasValidCoordinates() {
        return latitud != null && longitud != null
                && latitud >= -90 && latitud <= 90
                && longitud >= -180 && longitud <= 180;
    }

    public static Builder builder(String source, String sourceId, String titulo) {
        return new Builder(source, sourceId, titulo);
    }

    public static final class Builder {
        private final String source;
        private final String sourceId;
        private final String titulo;
        private final Set<PlanType> planTypes = new LinkedHashSet<>();
        private TemporalKind temporalKind = TemporalKind.EVENTO;
        private final Set<String> categoryKeys = new LinkedHashSet<>();
        private String description;
        private String address;
        private String schedule;
        private String priceInfo;
        private PriceStatus priceStatus = PriceStatus.DESCONOCIDO;
        private String lastSourceUpdate;
        private String distrito;
        private Double latitud;
        private Double longitud;
        private String nombreInstalacion;
        private String urlOficial;
        private final Map<String, String> sourceRefs = new LinkedHashMap<>();

        private Builder(String source, String sourceId, String titulo) {
            this.source = source;
            this.sourceId = sourceId;
            this.titulo = titulo;
        }

        public Builder planType(PlanType value) { if (value != null) planTypes.add(value); return this; }
        public Builder planTypes(Iterable<PlanType> values) { if (values != null) for (PlanType value : values) planType(value); return this; }
        public Builder temporalKind(TemporalKind value) { temporalKind = value; return this; }
        public Builder categoryKey(String value) { String normalized = normalize(value); if (normalized != null) categoryKeys.add(normalized); return this; }
        public Builder categoryKeys(Iterable<String> values) { if (values != null) for (String value : values) categoryKey(value); return this; }
        public Builder description(String value) { description = value; return this; }
        public Builder address(String value) { address = value; return this; }
        public Builder schedule(String value) { schedule = value; return this; }
        public Builder priceInfo(String value) { priceInfo = value; return this; }
        public Builder priceStatus(PriceStatus value) { priceStatus = value; return this; }
        public Builder lastSourceUpdate(String value) { lastSourceUpdate = value; return this; }
        public Builder distrito(String value) { distrito = value; return this; }
        public Builder coordinates(Double latitude, Double longitude) { latitud = latitude; longitud = longitude; return this; }
        public Builder nombreInstalacion(String value) { nombreInstalacion = value; return this; }
        public Builder urlOficial(String value) { urlOficial = value; return this; }
        public Builder sourceRef(String source, String sourceId) { String s=normalize(source),i=normalize(sourceId);if(s!=null&&i!=null)sourceRefs.put(s,i);return this; }
        public Builder sourceRefs(Map<String, String> values) { if(values!=null)for(Map.Entry<String,String> entry:values.entrySet())sourceRef(entry.getKey(),entry.getValue());return this; }
        public Plan build() { return new Plan(this); }
    }

    private static <T> Set<T> immutableCopy(Set<T> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }

    private static Set<String> immutableStrings(Set<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String value : values) {
            String item = normalize(value);
            if (item != null) normalized.add(item);
        }
        return Collections.unmodifiableSet(normalized);
    }

    protected static String normalize(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
