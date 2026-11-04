package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Evento;

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
        Evento restored = FavoritoMapper.fromMap("fallback", document);

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
        assertFalse(document.containsKey("guardadoEn"));
    }

    @Test
    public void missingValuesRemainMissingAndDocumentIdIsFallback() {
        Map<String, Object> document = new HashMap<>();
        document.put("gratuito", false);

        Evento restored = FavoritoMapper.fromMap("document-id", document);

        assertEquals("document-id", restored.getIdentificador());
        assertNull(restored.getTitulo());
        assertNull(restored.getFechaInicio());
        assertFalse(restored.isGratuito());
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
}
