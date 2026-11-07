package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.PlanFilterSelection;
import com.marinainsaurralde.plango.model.PlanType;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

public final class AppPreferencesTest {
    @Test public void decodesMultiplePlanTypesAndIgnoresUnknownValues() {
        assertEquals(new HashSet<>(Arrays.asList(PlanType.CULTURA, PlanType.TURISMO)),
                AppPreferences.decodeTypes(new HashSet<>(
                        Arrays.asList("CULTURA", "TURISMO", "DESCONOCIDO"))));
    }

    @Test public void emptyPlanTypePreferenceRemainsEmpty() {
        assertTrue(AppPreferences.decodeTypes(Collections.emptySet()).isEmpty());
    }

    @Test public void invalidPeriodFallsBackToToday() {
        assertEquals(FiltroEventos.Periodo.HOY,
                AppPreferences.decodePeriod("valor_antiguo"));
    }

    @Test public void decodesExistingDatePeriodIdentifiers() {
        assertEquals(FiltroEventos.Periodo.FIN_DE_SEMANA,
                AppPreferences.decodePeriod("FIN_DE_SEMANA"));
        assertEquals(FiltroEventos.Periodo.PROXIMOS_7_DIAS,
                AppPreferences.decodePeriod("PROXIMOS_7_DIAS"));
        assertEquals(FiltroEventos.Periodo.TODOS,
                AppPreferences.decodePeriod("TODOS"));
    }

    @Test public void preferenceFilesAreIsolatedByUid() {
        assertEquals("plango_settings_user-a", AppPreferences.fileName("user-a"));
        assertEquals("plango_settings_user-b", AppPreferences.fileName("user-b"));
    }

    @Test public void sessionDefaultsDoNotOverwriteTemporaryFiltersForSameUser() {
        EventosStore store = EventosStore.getInstance();
        store.endSession();
        FiltroEventos.Criterios initial = new FiltroEventos.Criterios(
                FiltroEventos.Periodo.HOY, false, null,
                PlanFilterSelection.culturaPorDefecto());
        assertTrue(store.initializeSession("user-a", initial));
        FiltroEventos.Criterios temporary = new FiltroEventos.Criterios(
                FiltroEventos.Periodo.TODOS, true, "CENTRO",
                PlanFilterSelection.todosLosTipos());
        store.setCriterios(temporary);

        assertTrue(!store.initializeSession("user-a", initial));
        assertEquals(temporary, store.getCriterios());
        assertTrue(store.initializeSession("user-b", initial));
        assertEquals(initial, store.getCriterios());
        store.endSession();
    }
}
