package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.data.EventosStore;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlanFilterSelectionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test public void initialSelectionIsCultureAndToday() {
        FiltroEventos.Criterios criteria = FiltroEventos.Criterios.porDefecto();
        assertEquals(FiltroEventos.Periodo.HOY, criteria.getPeriodo());
        assertEquals(Collections.singleton(PlanType.CULTURA),
                criteria.getPlanSelection().getSelectedTypes());
        assertFalse(criteria.getPlanSelection().hasCategoryRestriction(PlanType.CULTURA));
    }

    @Test public void selectedTypesUseOrAndDoNotDuplicateMultiTypePlans() {
        Evento culture = event("culture", EnumSet.of(PlanType.CULTURA), "/Musica", null,
                "CENTRO", true);
        Evento sport = event("sport", EnumSet.of(PlanType.DEPORTE), null,
                CategoriaDeportiva.CICLISMO.getKey(), "CENTRO", true);
        Evento shared = event("shared", EnumSet.of(PlanType.CULTURA, PlanType.DEPORTE),
                "/Cine", CategoriaDeportiva.CARRERAS_MARATONES.getKey(), "CENTRO", true);
        PlanFilterSelection selection = PlanFilterSelection.of(
                EnumSet.of(PlanType.CULTURA, PlanType.DEPORTE), Collections.emptyMap());

        List<Plan> result = filter(Arrays.asList(culture, sport, shared), selection,
                false, null);

        assertEquals(3, result.size());
        assertEquals(1, result.stream().filter(plan -> "shared".equals(plan.getSourceId())).count());
    }

    @Test public void categoriesUseOrWithinTypeAndCombineWithTopFiltersUsingAnd() {
        Evento musicFreeCentre = event("music", EnumSet.of(PlanType.CULTURA), "/Musica",
                null, "CENTRO", true);
        Evento cinemaFreeCentre = event("cinema", EnumSet.of(PlanType.CULTURA), "/Cine",
                null, "CENTRO", true);
        Evento theaterPaidCentre = event("theater", EnumSet.of(PlanType.CULTURA), "/Teatro",
                null, "CENTRO", false);
        Evento musicFreeRetiro = event("retiro", EnumSet.of(PlanType.CULTURA), "/Musica",
                null, "RETIRO", true);
        Map<PlanType, Set<String>> restrictions = new EnumMap<>(PlanType.class);
        restrictions.put(PlanType.CULTURA, new LinkedHashSet<>(Arrays.asList(
                FiltroEventos.culturalCategoryKey(CategoriaCultural.MUSICA),
                FiltroEventos.culturalCategoryKey(CategoriaCultural.CINE))));
        PlanFilterSelection selection = PlanFilterSelection.of(
                EnumSet.of(PlanType.CULTURA), restrictions);

        List<Plan> result = filter(Arrays.asList(musicFreeCentre, cinemaFreeCentre,
                theaterPaidCentre, musicFreeRetiro), selection, true, "CENTRO");

        assertEquals(Arrays.asList(musicFreeCentre, cinemaFreeCentre), result);
    }

    @Test public void multiTypePlanPassesWhenEitherSelectedTypeMatchesItsCategory() {
        Evento shared = event("shared", EnumSet.of(PlanType.CULTURA, PlanType.DEPORTE),
                "/Cine", CategoriaDeportiva.CICLISMO.getKey(), "CENTRO", true);
        Map<PlanType, Set<String>> restrictions = new EnumMap<>(PlanType.class);
        restrictions.put(PlanType.CULTURA, Collections.singleton(
                FiltroEventos.culturalCategoryKey(CategoriaCultural.MUSICA)));
        restrictions.put(PlanType.DEPORTE, Collections.singleton(
                CategoriaDeportiva.CICLISMO.getKey()));
        PlanFilterSelection selection = PlanFilterSelection.of(
                EnumSet.of(PlanType.CULTURA, PlanType.DEPORTE), restrictions);

        assertEquals(1, filter(Collections.singletonList(shared), selection,
                false, null).size());
    }

    @Test public void permanentPlacesOnlyAppearWhenTheirTypeIsSelected() {
        Plan park = new Lugar(Plan.builder("parks", "p1", "Parque")
                .planType(PlanType.NATURALEZA).temporalKind(TemporalKind.LUGAR_PERMANENTE));
        assertTrue(filter(Collections.singletonList(park),
                PlanFilterSelection.culturaPorDefecto(), false, null).isEmpty());
        assertEquals(1, filter(Collections.singletonList(park), PlanFilterSelection.of(
                EnumSet.of(PlanType.NATURALEZA), Collections.emptyMap()), false, null).size());
    }

    @Test public void draftApplyAndClearPreserveSharedStoreSelectionWithoutChangingData() {
        EventosStore store = EventosStore.getInstance();
        Plan original = event("one", EnumSet.of(PlanType.CULTURA), "/Musica", null,
                "CENTRO", true);
        store.setEventos(Collections.singletonList(original));
        PlanFilterDraft draft = new PlanFilterDraft(PlanFilterSelection.culturaPorDefecto());
        draft.setTypeSelected(PlanType.TURISMO, true);
        FiltroEventos.Criterios applied = new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY,
                false, null, draft.toSelection());
        store.setCriterios(applied);
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.CULTURA));
        assertTrue(store.getCriterios().getPlanSelection().isTypeSelected(PlanType.TURISMO));
        assertSame(original, store.getEventos().get(0));

        draft.clearAll();
        store.setCriterios(new FiltroEventos.Criterios(FiltroEventos.Periodo.HOY,
                false, null, draft.toSelection()));
        assertTrue(store.getCriterios().getPlanSelection().getSelectedTypes().isEmpty());
        assertSame(original, store.getEventos().get(0));
    }

    @Test public void eachTypeTogglesIndependentlyAndRemovingItDropsItsCategories() {
        PlanFilterDraft draft = new PlanFilterDraft(PlanFilterSelection.culturaPorDefecto());
        draft.setTypeSelected(PlanType.DEPORTE, true);
        assertTrue(draft.isTypeSelected(PlanType.CULTURA));
        assertTrue(draft.isTypeSelected(PlanType.DEPORTE));
        draft.setSpecificCategorySelected(PlanType.DEPORTE, "deporte:golf", true);
        assertTrue(draft.toSelection().hasCategoryRestriction(PlanType.DEPORTE));

        draft.setTypeSelected(PlanType.DEPORTE, false);
        assertTrue(draft.isTypeSelected(PlanType.CULTURA));
        assertFalse(draft.isTypeSelected(PlanType.DEPORTE));
        assertFalse(draft.toSelection().hasCategoryRestriction(PlanType.DEPORTE));
        draft.setTypeSelected(PlanType.DEPORTE, true);
        assertFalse(draft.toSelection().hasCategoryRestriction(PlanType.DEPORTE));
    }

    @Test public void emptySelectionProducesNoResultsAndDoesNotMeanAll() {
        Plan culture = event("culture", EnumSet.of(PlanType.CULTURA), "/Musica", null,
                "CENTRO", true);
        Plan tourism = new Lugar(Plan.builder("tourism", "tourism", "Turismo")
                .planType(PlanType.TURISMO).temporalKind(TemporalKind.LUGAR_PERMANENTE));
        assertTrue(filter(Arrays.asList(culture, tourism), PlanFilterSelection.vacia(),
                false, null).isEmpty());
    }

    @Test public void categoryDraftStartsWithAllAndSupportsSpecificSelection() {
        PlanFilterDraft draft = new PlanFilterDraft(PlanFilterSelection.of(
                EnumSet.of(PlanType.DEPORTE), Collections.emptyMap()));
        assertTrue(draft.includesAllCategories(PlanType.DEPORTE));
        assertFalse(draft.isSpecificCategorySelected(PlanType.DEPORTE, "deporte:ciclismo"));
        assertFalse(draft.isSpecificCategorySelected(PlanType.DEPORTE, "deporte:golf"));
        draft.setSpecificCategorySelected(PlanType.DEPORTE, "deporte:ciclismo", true);
        assertTrue(draft.toSelection().hasCategoryRestriction(PlanType.DEPORTE));
        assertEquals(Collections.singleton("deporte:ciclismo"),
                draft.toSelection().getSelectedCategoryKeys(PlanType.DEPORTE));
    }

    @Test public void removingLastSpecificCategoryRestoresAllCategories() {
        PlanFilterDraft draft = new PlanFilterDraft(PlanFilterSelection.culturaPorDefecto());
        String music = FiltroEventos.culturalCategoryKey(CategoriaCultural.MUSICA);
        draft.setSpecificCategorySelected(PlanType.CULTURA, music, true);
        draft.setSpecificCategorySelected(PlanType.CULTURA, music, false);

        assertTrue(draft.isTypeSelected(PlanType.CULTURA));
        assertTrue(draft.includesAllCategories(PlanType.CULTURA));
        assertFalse(draft.toSelection().hasCategoryRestriction(PlanType.CULTURA));
    }

    private List<Plan> filter(List<Plan> plans, PlanFilterSelection selection,
                              boolean freeOnly, String district) {
        return FiltroEventos.aplicar(plans, new FiltroEventos.Criterios(
                FiltroEventos.Periodo.HOY, freeOnly, district, selection), TODAY);
    }

    private Evento event(String id, Set<PlanType> types, String culturalType,
                         String categoryKey, String district, boolean free) {
        Plan.Builder builder = Plan.builder(PlanIdentity.EVENTO_MADRID, id, id)
                .planTypes(types).temporalKind(TemporalKind.EVENTO)
                .distrito(district).priceStatus(free ? PriceStatus.GRATIS : PriceStatus.DE_PAGO);
        if (categoryKey != null) builder.categoryKey(categoryKey);
        return new Evento(builder, "2026-11-06 00:00:00.0", "2026-11-06 23:59:00.0",
                culturalType, free, true, null, null, null, null);
    }
}
