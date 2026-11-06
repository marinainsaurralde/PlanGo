package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventosMergerTest {

    @Test
    public void sharedEventIsMergedWithoutOverwritingValidGeneralData() {
        Evento general = event("1", "Título general", PlanType.CULTURA,
                "cultura:musica", "CENTRO", null);
        Evento sports = event("1", "Título deportivo", PlanType.DEPORTE,
                "deporte:ciclismo", null, "Descripción deportiva");

        List<Evento> result = EventosMerger.merge(
                Collections.singletonList(general), Collections.singletonList(sports));
        Evento merged = result.get(0);

        assertEquals(1, result.size());
        assertEquals("Título general", merged.getTitulo());
        assertEquals("CENTRO", merged.getDistrito());
        assertEquals("Descripción deportiva", merged.getDescription());
        assertTrue(merged.getPlanTypes().containsAll(
                Arrays.asList(PlanType.CULTURA, PlanType.DEPORTE)));
        assertTrue(merged.getCategoryKeys().contains("cultura:musica"));
        assertTrue(merged.getCategoryKeys().contains("deporte:ciclismo"));
        assertEquals("1", merged.getFavoriteDocumentId());
    }

    @Test
    public void sportsOnlyEventIsAddedOnceWithCompatibleIdentity() {
        Evento sports = event("2", "Evento deportivo", PlanType.DEPORTE,
                "deporte:golf", "MONCLOA-ARAVACA", null);

        List<Evento> result = EventosMerger.merge(Collections.emptyList(),
                Collections.singletonList(sports));

        assertEquals(1, result.size());
        assertEquals("evento-madrid:2", result.get(0).getCompositeId());
        assertEquals("2", result.get(0).getFavoriteDocumentId());
    }

    private Evento event(String id, String title, PlanType type, String category,
                         String district, String description) {
        Plan.Builder plan = Plan.builder(PlanIdentity.EVENTO_MADRID, id, title)
                .planType(type).categoryKey(category).description(description)
                .priceStatus(PriceStatus.DESCONOCIDO).distrito(district);
        return new Evento(plan, "2026-11-05 00:00:00.0", "2026-11-05 23:59:00.0",
                category, false, false, null, null, null, null);
    }
}
