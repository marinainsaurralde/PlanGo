package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class FiltroEventosTest {

    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 11, 4);

    @Test
    public void nextSevenDaysIncludesTodayAndSixFollowingDays() {
        List<Evento> result = filter(
                Arrays.asList(
                        event("today", "2026-11-04", "2026-11-04", true, "CENTRO", null, null),
                        event("day-six", "2026-11-10", "2026-11-10", true, "CENTRO", null, null),
                        event("day-seven", "2026-11-11", "2026-11-11", true, "CENTRO", null, null)
                ),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.PROXIMOS_7_DIAS, false, null)
        );

        assertEquals(Arrays.asList("today", "day-six"), ids(result));
    }

    @Test
    public void ongoingEventMatchesEvenWhenItStartedBeforeRange() {
        Evento ongoing = event("ongoing", "2026-10-01", "2026-11-06",
                false, "RETIRO", "V", null);

        List<Evento> result = filter(
                Arrays.asList(ongoing),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.PROXIMOS_7_DIAS, false, null)
        );

        assertEquals(1, result.size());
    }

    @Test
    public void weekendOnSundayIncludesCurrentWeekend() {
        LocalDate sunday = LocalDate.of(2026, 11, 8);
        List<Evento> result = FiltroEventos.aplicar(
                Arrays.asList(
                        event("saturday", "2026-11-07", "2026-11-07", true, "CENTRO", null, null),
                        event("sunday", "2026-11-08", "2026-11-08", true, "CENTRO", null, null),
                        event("next-saturday", "2026-11-14", "2026-11-14", true, "CENTRO", null, null)
                ),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.FIN_DE_SEMANA, false, null),
                sunday
        );

        assertEquals(Arrays.asList("saturday", "sunday"), ids(result));
    }

    @Test
    public void recurrenceAndExcludedDatesAreApplied() {
        List<Evento> result = filter(
                Arrays.asList(
                        event("excluded", "2026-11-01", "2026-11-30", true,
                                "CENTRO", "X", "4/11/2026;"),
                        event("valid", "2026-11-01", "2026-11-30", true,
                                "CENTRO", "J", "texto-corrupto;12/11/2026;")
                ),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY, false, null)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void combinesDateFreeAndDistrictFilters() {
        List<Evento> result = filter(
                Arrays.asList(
                        event("match", "2026-11-04", "2026-11-04", true, "CENTRO", null, null),
                        event("paid", "2026-11-04", "2026-11-04", false, "CENTRO", null, null),
                        event("other-district", "2026-11-04", "2026-11-04", true, "RETIRO", null, null),
                        event("other-date", "2026-12-04", "2026-12-04", true, "CENTRO", null, null)
                ),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY, true, "CENTRO")
        );

        assertEquals(Arrays.asList("match"), ids(result));
    }

    @Test
    public void districtsComeOnlyFromAvailableDataAndAreSorted() {
        List<String> districts = FiltroEventos.distritosDisponibles(Arrays.asList(
                event("1", "2026-11-04", "2026-11-04", true, "RETIRO", null, null),
                event("2", "2026-11-04", "2026-11-04", true, "CENTRO", null, null),
                event("3", "2026-11-04", "2026-11-04", true, " ", null, null),
                event("4", "2026-11-04", "2026-11-04", true, "CENTRO", null, null)
        ));

        assertEquals(Arrays.asList("CENTRO", "RETIRO"), districts);
    }

    @Test
    public void eventEndedBeforeSelectedPeriodIsExcluded() {
        Evento ended = event("ended", "2026-10-01", "2026-11-03",
                true, "CENTRO", null, null);

        List<Evento> result = filter(
                Arrays.asList(ended),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY, false, null)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void completelyUnknownWeekdayValueDoesNotMeanEveryDay() {
        Evento unknown = event("unknown", "2026-11-01", "2026-11-30",
                true, "CENTRO", "DESCONOCIDO", null);

        List<Evento> result = filter(
                Arrays.asList(unknown),
                new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY, false, null)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    public void formatsSingleDayRangeAndOngoingStateWithoutInventingDates() {
        Evento oneDay = event("one", "2026-11-04", "2026-11-04",
                true, "CENTRO", null, null);
        Evento ongoing = event("ongoing", "2026-07-21", "2027-01-31",
                true, "CENTRO", null, null);

        assertEquals("4 nov 2026", oneDay.getPeriodoFormateado());
        assertEquals("21 jul 2026 – 31 ene 2027", ongoing.getPeriodoFormateado());
        assertTrue(ongoing.isEnCurso(WEDNESDAY));
    }

    private List<Evento> filter(List<Evento> events, FiltroEventos.Criterios criteria) {
        return FiltroEventos.aplicar(events, criteria, WEDNESDAY);
    }

    private List<String> ids(List<Evento> events) {
        java.util.ArrayList<String> ids = new java.util.ArrayList<>();
        for (Evento event : events) {
            ids.add(event.getIdentificador());
        }
        return ids;
    }

    private Evento event(
            String id,
            String start,
            String end,
            boolean free,
            String district,
            String weekdays,
            String excluded
    ) {
        return new Evento(
                id,
                id,
                start + " 00:00:00.0",
                end + " 23:59:00.0",
                district,
                "tipo",
                free,
                null,
                null,
                null,
                weekdays,
                excluded
        );
    }
}
