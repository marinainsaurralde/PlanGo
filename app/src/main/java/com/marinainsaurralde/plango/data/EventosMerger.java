package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EventosMerger {
    private EventosMerger() {
    }

    public static List<Evento> merge(List<Evento> primary, List<Evento> sports) {
        Map<String, Evento> merged = new LinkedHashMap<>();
        addAll(merged, primary, false);
        addAll(merged, sports, true);
        return new ArrayList<>(merged.values());
    }

    private static void addAll(Map<String, Evento> result, List<Evento> events, boolean sports) {
        if (events == null) return;
        for (Evento event : events) {
            if (event == null || event.getSourceId() == null) continue;
            String key = PlanIdentity.compose(PlanIdentity.EVENTO_MADRID, event.getSourceId());
            Evento previous = result.get(key);
            if (previous == null) result.put(key, event);
            else result.put(key, sports ? mergePair(previous, event) : mergePair(event, previous));
        }
    }

    static Evento mergePair(Evento general, Evento sports) {
        Set<PlanType> planTypes = new LinkedHashSet<>(general.getPlanTypes());
        planTypes.addAll(sports.getPlanTypes());
        Set<String> categories = new LinkedHashSet<>(general.getCategoryKeys());
        categories.addAll(sports.getCategoryKeys());
        boolean informed = general.isGratuidadInformada() || sports.isGratuidadInformada();
        boolean free = general.isGratuidadInformada() ? general.isGratuito() : sports.isGratuito();
        PriceStatus priceStatus = general.getPriceStatus() != PriceStatus.DESCONOCIDO
                ? general.getPriceStatus() : sports.getPriceStatus();
        Double latitude = general.hasValidCoordinates() ? general.getLatitud() : sports.getLatitud();
        Double longitude = general.hasValidCoordinates() ? general.getLongitud() : sports.getLongitud();
        Plan.Builder plan = Plan.builder(PlanIdentity.EVENTO_MADRID, general.getSourceId(),
                        first(general.getTitulo(), sports.getTitulo()))
                .planTypes(planTypes)
                .temporalKind(TemporalKind.EVENTO)
                .categoryKeys(categories)
                .description(first(general.getDescription(), sports.getDescription()))
                .address(first(general.getAddress(), sports.getAddress()))
                .schedule(first(general.getSchedule(), sports.getSchedule()))
                .priceInfo(first(general.getPriceInfo(), sports.getPriceInfo()))
                .priceStatus(priceStatus)
                .lastSourceUpdate(first(general.getLastSourceUpdate(), sports.getLastSourceUpdate()))
                .distrito(first(general.getDistrito(), sports.getDistrito()))
                .coordinates(latitude, longitude)
                .nombreInstalacion(first(general.getNombreInstalacion(), sports.getNombreInstalacion()))
                .urlOficial(first(general.getUrlOficial(), sports.getUrlOficial()))
                .sourceRefs(general.getSourceRefs()).sourceRefs(sports.getSourceRefs());
        return new Evento(
                plan,
                first(general.getFechaInicio(), sports.getFechaInicio()),
                first(general.getFechaFin(), sports.getFechaFin()),
                first(general.getTipo(), sports.getTipo()),
                free,
                informed,
                first(general.getDiasSemana(), sports.getDiasSemana()),
                first(general.getDiasExcluidos(), sports.getDiasExcluidos()),
                first(general.getHora(), sports.getHora()),
                first(general.getPrecio(), sports.getPrecio())
        );
    }

    private static String first(String preferred, String fallback) {
        return preferred == null || preferred.trim().isEmpty() ? fallback : preferred;
    }
}
