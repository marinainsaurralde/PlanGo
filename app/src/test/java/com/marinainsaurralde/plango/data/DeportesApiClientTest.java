package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.CategoriaDeportiva;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;

import org.junit.Test;

import java.util.List;

public class DeportesApiClientTest {

    @Test
    public void parsesSportsEventAndRealCategory() throws Exception {
        String json = "{\"@graph\":[{"
                + "\"id\":\"50386232\",\"title\":\"Half Madrid Triatlón\","
                + "\"@type\":\"https://datos.madrid.es/actividades/ActividadesDeportivas/CarrerasMaratones\","
                + "\"description\":\"Prueba deportiva\",\"free\":0,\"price\":\"10 €\","
                + "\"dtstart\":\"2026-09-27 08:15:00.0\",\"dtend\":\"2026-09-27 23:59:00.0\","
                + "\"time\":\"08:15\",\"excluded-days\":\"\","
                + "\"recurrence\":{\"days\":\"SU\"},"
                + "\"link\":\"https://www.madrid.es/evento\",\"event-location\":\"Casa de Campo\","
                + "\"address\":{\"district\":{\"@id\":\"https://datos.madrid.es/Distrito/Moncloa-Aravaca\"},"
                + "\"street-address\":\"PASEO PUERTA DEL ANGEL 1\"},"
                + "\"location\":{\"latitude\":40.4147,\"longitude\":-3.7301},"
                + "\"organization\":{\"organization-name\":\"Parque Casa de Campo\"}}]}";

        List<Evento> events = DeportesApiClient.parseEvents(json);
        Evento event = events.get(0);

        assertEquals(1, events.size());
        assertEquals("50386232", event.getSourceId());
        assertTrue(event.getPlanTypes().contains(PlanType.DEPORTE));
        assertTrue(event.getCategoryKeys().contains(CategoriaDeportiva.CARRERAS_MARATONES.getKey()));
        assertEquals("Carreras y maratones", event.getTipo());
        assertEquals(PriceStatus.DE_PAGO, event.getPriceStatus());
        assertEquals("D", event.getDiasSemana());
        assertEquals("MONCLOA-ARAVACA", event.getDistrito());
        assertTrue(event.hasValidCoordinates());
    }

    @Test
    public void unknownAndMissingTypesRemainUnclassified() {
        assertEquals(CategoriaDeportiva.SIN_CLASIFICAR,
                CategoriaDeportiva.fromApiType(null));
        assertEquals(CategoriaDeportiva.SIN_CLASIFICAR,
                CategoriaDeportiva.fromApiType("https://example.test/NuevaCategoria"));
    }

    @Test
    public void classifiesEveryCategoryObservedInSportsFeed() {
        String base = "https://datos.madrid.es/egob/kos/actividades/";
        assertEquals(CategoriaDeportiva.ACTIVIDADES_DEPORTIVAS,
                CategoriaDeportiva.fromApiType(base + "ActividadesDeportivas"));
        assertEquals(CategoriaDeportiva.CICLISMO,
                CategoriaDeportiva.fromApiType(base + "ActividadesDeportivas/Ciclismo"));
        assertEquals(CategoriaDeportiva.CARRERAS_MARATONES,
                CategoriaDeportiva.fromApiType(base + "ActividadesDeportivas/CarrerasMaratones"));
        assertEquals(CategoriaDeportiva.SKATEBOARDING,
                CategoriaDeportiva.fromApiType(base + "ActividadesDeportivas/Skateboarding"));
        assertEquals(CategoriaDeportiva.GOLF,
                CategoriaDeportiva.fromApiType(base + "ActividadesDeportivas/Golf"));
        assertEquals(CategoriaDeportiva.CURSOS_TALLERES,
                CategoriaDeportiva.fromApiType(base + "CursosTalleres"));
    }
}
