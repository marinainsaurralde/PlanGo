package com.marinainsaurralde.plango.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;

public class PlanTest {

    @Test
    public void existingCulturalEventKeepsLegacyIdentityAndDefaults() {
        Evento event = new Evento(
                "123", "Concierto", "2026-11-05 18:00:00.0", "2026-11-05 23:59:00.0",
                "CENTRO", "/contenido/actividades/Musica", true,
                40.4, -3.7, "Auditorio", null, null
        );

        assertEquals("123", event.getIdentificador());
        assertEquals(PlanIdentity.EVENTO_MADRID, event.getSource());
        assertEquals("evento-madrid:123", event.getCompositeId());
        assertEquals("123", event.getFavoriteDocumentId());
        assertEquals(TemporalKind.EVENTO, event.getTemporalKind());
        assertEquals(PriceStatus.GRATIS, event.getPriceStatus());
        assertEquals(Arrays.asList(PlanType.CULTURA), Arrays.asList(event.getPlanTypes().toArray()));
    }

    @Test
    public void permanentPlanSupportsUnknownPriceAndNoInventedDates() {
        Plan park = Plan.builder("parque-madrid", "42", "Parque del Oeste")
                .planTypes(Arrays.asList(PlanType.NATURALEZA, PlanType.TURISMO))
                .temporalKind(TemporalKind.LUGAR_PERMANENTE)
                .categoryKeys(Arrays.asList("parques", "jardines"))
                .priceStatus(PriceStatus.DESCONOCIDO)
                .distrito("MONCLOA-ARAVACA")
                .coordinates(40.43, -3.72)
                .build();

        assertEquals(TemporalKind.LUGAR_PERMANENTE, park.getTemporalKind());
        assertEquals(PriceStatus.DESCONOCIDO, park.getPriceStatus());
        assertTrue(park.getPlanTypes().contains(PlanType.NATURALEZA));
        assertTrue(park.getPlanTypes().contains(PlanType.TURISMO));
        assertEquals(2, park.getCategoryKeys().size());
        assertNull(park.getLastSourceUpdate());
    }

    @Test
    public void compositeIdentityAvoidsSourcesCollidingAndSharesMadridEventIdentity() {
        Plan park = Plan.builder("parque-madrid", "123", "Parque").build();
        Plan tourism = Plan.builder("turismo-madrid", "123", "Lugar").build();
        Plan cultural = Plan.builder(PlanIdentity.EVENTO_MADRID, "999", "Evento").build();
        Plan sports = Plan.builder(PlanIdentity.EVENTO_MADRID, "999", "Evento deportivo").build();

        assertFalse(park.getCompositeId().equals(tourism.getCompositeId()));
        assertFalse(park.getFavoriteDocumentId().equals(tourism.getFavoriteDocumentId()));
        assertEquals(cultural.getCompositeId(), sports.getCompositeId());
        assertEquals("999", cultural.getFavoriteDocumentId());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void categoriesAreImmutable() {
        Plan plan = Plan.builder("test", "1", "Plan").categoryKey("uno").build();
        plan.getCategoryKeys().add("dos");
    }
}
