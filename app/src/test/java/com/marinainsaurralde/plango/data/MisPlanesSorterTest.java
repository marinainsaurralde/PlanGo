package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.TemporalKind;

import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class MisPlanesSorterTest {
    private static final Instant REFERENCE = Instant.parse("2026-11-07T12:00:00Z");

    @Test public void currentEventComesBeforeFutureEvent() {
        assertTitles(Arrays.asList(
                event("future", "Futuro", "2026-11-09", "2026-11-10"),
                event("current", "En curso", "2026-11-01", "2026-11-08")),
                "En curso", "Futuro");
    }

    @Test public void currentEventsEndSoonestFirst() {
        assertTitles(Arrays.asList(
                event("later", "Termina después", "2026-11-01", "2026-11-20"),
                event("soon", "Termina antes", "2026-11-01", "2026-11-08")),
                "Termina antes", "Termina después");
    }

    @Test public void futureEventsAreAscending() {
        assertTitles(Arrays.asList(
                event("later", "Más lejano", "2026-12-01", "2026-12-02"),
                event("soon", "Más próximo", "2026-11-08", "2026-11-09")),
                "Más próximo", "Más lejano");
    }

    @Test public void finishedEventsAreDescending() {
        assertTitles(Arrays.asList(
                event("old", "Antiguo", "2026-09-01", "2026-09-02"),
                event("recent", "Reciente", "2026-11-01", "2026-11-06")),
                "Reciente", "Antiguo");
    }

    @Test public void invalidAndMissingDatesAreAlphabeticalBeforePlaces() {
        Plan park = place("park", "Parque");
        assertTitles(Arrays.asList(
                park,
                event("invalid", "Zeta", "fecha inválida", null),
                event("missing", "Árbol", null, null)),
                "Árbol", "Zeta", "Parque");
    }

    @Test public void permanentPlacesAreLastAndAlphabetical() {
        assertTitles(Arrays.asList(
                place("z", "Zoo"),
                event("unknown", "Sin fecha", null, null),
                place("a", "Alameda")),
                "Sin fecha", "Alameda", "Zoo");
    }

    @Test public void startOnlyIsFutureOrUndeterminedWithoutInventingEnd() {
        assertTitles(Arrays.asList(
                event("past", "Inicio pasado", "2026-11-01", null),
                event("future", "Inicio futuro", "2026-11-08", null)),
                "Inicio futuro", "Inicio pasado");
    }

    @Test public void dateTiesUseStableAlphabeticalTitle() {
        assertTitles(Arrays.asList(
                event("b", "Beta", "2026-11-08", "2026-11-09"),
                event("a", "Álfa", "2026-11-08", "2026-11-09")),
                "Álfa", "Beta");
    }

    @Test public void madridDateIsUsedAcrossDaylightSavingBoundary() {
        Instant madridAlreadyNextDay = Instant.parse("2026-03-29T22:30:00Z");
        List<Plan> ordered = MisPlanesSorter.ordenar(Arrays.asList(
                event("future", "Mañana", "2026-03-31", "2026-03-31"),
                event("today", "Hoy en Madrid", "2026-03-30", "2026-03-30")),
                madridAlreadyNextDay);
        assertEquals("Hoy en Madrid", ordered.get(0).getTitulo());
    }

    @Test public void sortingKeepsEveryFavoriteAndDoesNotMutateInput() {
        List<Plan> input = new ArrayList<>(Arrays.asList(
                place("place", "Lugar"),
                event("future", "Futuro", "2026-11-08", "2026-11-09"),
                event("past", "Pasado", "2026-11-01", "2026-11-02")));
        List<Plan> originalOrder = new ArrayList<>(input);
        List<Plan> ordered = MisPlanesSorter.ordenar(input, REFERENCE);
        assertEquals(3, ordered.size());
        assertEquals(originalOrder, input);
        assertEquals(input.size(), ordered.stream().distinct().count());
    }

    private void assertTitles(List<Plan> plans, String... expected) {
        List<Plan> ordered = MisPlanesSorter.ordenar(plans, REFERENCE);
        List<String> titles = new ArrayList<>();
        for (Plan plan : ordered) titles.add(plan.getTitulo());
        assertEquals(Arrays.asList(expected), titles);
    }

    private Evento event(String id, String title, String start, String end) {
        return new Evento(id, title, apiDate(start), apiDate(end), null, null,
                false, false, null, null, null, null, null, null, null, null);
    }

    private String apiDate(String date) {
        if (date == null || date.contains("inválida")) return date;
        return date + " 00:00:00.0";
    }

    private Plan place(String id, String title) {
        return new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID, id, title)
                .planType(PlanType.NATURALEZA)
                .temporalKind(TemporalKind.LUGAR_PERMANENTE));
    }
}
