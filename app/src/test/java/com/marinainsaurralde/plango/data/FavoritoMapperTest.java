package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;

import org.junit.Test;

import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class FavoritoMapperTest {

    @Test
    public void eventRoundTripPreservesFavoriteData() {
        Evento original = new Evento(
                "123", "Concierto", "2026-11-04 00:00:00.0",
                "2026-11-05 23:59:00.0", "CENTRO", "Música", true,
                40.4168, -3.7038, "Centro Cultural", "X,J", "5/11/2026;",
                "19:00", "Entrada gratuita", "https://example.test/evento"
        );

        Map<String, Object> document = FavoritoMapper.toMap(original);
        Evento restored = (Evento) FavoritoMapper.fromMap("fallback", document);

        assertEquals("123", restored.getIdentificador());
        assertEquals(original.getTitulo(), restored.getTitulo());
        assertEquals(original.getFechaInicio(), restored.getFechaInicio());
        assertEquals(original.getFechaFin(), restored.getFechaFin());
        assertEquals(original.getHora(), restored.getHora());
        assertEquals(original.getNombreInstalacion(), restored.getNombreInstalacion());
        assertEquals(original.getDistrito(), restored.getDistrito());
        assertEquals(original.getPrecio(), restored.getPrecio());
        assertTrue(restored.isGratuito());
        assertEquals(original.getTipo(), restored.getTipo());
        assertEquals(original.getLatitud(), restored.getLatitud());
        assertEquals(original.getLongitud(), restored.getLongitud());
        assertEquals(original.getUrlOficial(), restored.getUrlOficial());
        assertEquals(PlanIdentity.EVENTO_MADRID, restored.getSource());
        assertEquals(PriceStatus.GRATIS, restored.getPriceStatus());
        assertTrue(restored.getPlanTypes().contains(PlanType.CULTURA));
        assertFalse(document.containsKey("guardadoEn"));
    }

    @Test
    public void missingValuesRemainMissingAndDocumentIdIsFallback() {
        Map<String, Object> document = new HashMap<>();
        document.put("gratuito", false);

        Evento restored = (Evento) FavoritoMapper.fromMap("document-id", document);

        assertEquals("document-id", restored.getIdentificador());
        assertNull(restored.getTitulo());
        assertNull(restored.getFechaInicio());
        assertFalse(restored.isGratuito());
        assertFalse(restored.isGratuidadInformada());
        assertEquals(PriceStatus.DESCONOCIDO, restored.getPriceStatus());
        assertEquals("document-id", restored.getFavoriteDocumentId());
        assertEquals(TemporalKind.EVENTO, restored.getTemporalKind());
        assertTrue(restored.getPlanTypes().contains(PlanType.CULTURA));
    }

    @Test
    public void newMetadataSurvivesFavoriteRoundTrip() {
        Plan.Builder metadata = Plan.builder("evento-madrid", "456", "Ruta deportiva")
                .planType(PlanType.DEPORTE)
                .planType(PlanType.NATURALEZA)
                .temporalKind(TemporalKind.EVENTO)
                .categoryKey("ciclismo")
                .categoryKey("aire-libre")
                .description("Ruta guiada")
                .address("Casa de Campo")
                .schedule("Domingos")
                .priceStatus(PriceStatus.DE_PAGO)
                .lastSourceUpdate("2026-11-05")
                .distrito("MONCLOA-ARAVACA");
        Evento original = new Evento(metadata, "2026-11-08 09:00:00.0",
                "2026-11-08 12:00:00.0", "Ciclismo", false, true,
                null, null, "09:00", "10 €");

        Evento restored = (Evento) FavoritoMapper.fromMap("456", FavoritoMapper.toMap(original));

        assertEquals(original.getPlanTypes(), restored.getPlanTypes());
        assertEquals(original.getCategoryKeys(), restored.getCategoryKeys());
        assertEquals(original.getDescription(), restored.getDescription());
        assertEquals(original.getAddress(), restored.getAddress());
        assertEquals(original.getSchedule(), restored.getSchedule());
        assertEquals(PriceStatus.DE_PAGO, restored.getPriceStatus());
        assertEquals("2026-11-05", restored.getLastSourceUpdate());
    }

    @Test
    public void finishedStateUsesMadridDateAndKeepsInvalidDatesUnfinished() {
        Evento finished = new Evento(
                "1", "Pasado", null, "2020-01-01 23:59:00.0",
                null, null, false, null, null, null, null, null
        );
        Evento invalid = new Evento(
                "2", "Sin fecha", null, "fecha inválida",
                null, null, false, null, null, null, null, null
        );

        assertTrue(finished.isFinalizado(ZoneId.of("Europe/Madrid")));
        assertFalse(invalid.isFinalizado(ZoneId.of("Europe/Madrid")));
    }

    @Test
    public void parkRoundTripUsesCompositeIdentityAndPreservesPermanentMetadata() {
        Plan park = new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID, "123", "Parque")
                .planType(PlanType.NATURALEZA)
                .temporalKind(TemporalKind.LUGAR_PERMANENTE)
                .categoryKey("naturaleza:parques-jardines")
                .description("Zona verde")
                .address("CALLE VERDE 1")
                .schedule("De 8 a 22 h")
                .priceStatus(PriceStatus.DESCONOCIDO)
                .distrito("RETIRO")
                .coordinates(40.4, -3.7));

        Plan restored = FavoritoMapper.fromMap(park.getFavoriteDocumentId(),
                FavoritoMapper.toMap(park));

        assertTrue(restored instanceof Lugar);
        assertEquals("parque-madrid:123", restored.getFavoriteDocumentId());
        assertEquals(TemporalKind.LUGAR_PERMANENTE, restored.getTemporalKind());
        assertEquals(PriceStatus.DESCONOCIDO, restored.getPriceStatus());
        assertEquals(park.getDescription(), restored.getDescription());
        assertEquals(park.getAddress(), restored.getAddress());
        assertEquals(park.getSchedule(), restored.getSchedule());
    }

    @Test public void tourismRoundTripPreservesPriceAndSourceReferences(){Plan tourism=new Lugar(Plan.builder(PlanIdentity.TURISMO_MADRID,"99","Museo").planType(PlanType.TURISMO).temporalKind(TemporalKind.LUGAR_PERMANENTE).categoryKey("turismo:7172").priceInfo("Entrada general: 12 €").priceStatus(PriceStatus.DE_PAGO).lastSourceUpdate("2026-11-04").sourceRef(PlanIdentity.PARQUE_MADRID,"123"));Plan restored=FavoritoMapper.fromMap(tourism.getFavoriteDocumentId(),FavoritoMapper.toMap(tourism));assertTrue(restored instanceof Lugar);assertEquals("turismo-madrid:99",restored.getFavoriteDocumentId());assertEquals("Entrada general: 12 €",restored.getPriceInfo());assertEquals("99",restored.getSourceRefs().get(PlanIdentity.TURISMO_MADRID));assertEquals("123",restored.getSourceRefs().get(PlanIdentity.PARQUE_MADRID));assertTrue(restored.getPlanTypes().contains(PlanType.TURISMO));}
}
