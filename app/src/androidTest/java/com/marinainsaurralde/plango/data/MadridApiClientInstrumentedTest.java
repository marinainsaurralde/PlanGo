package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.marinainsaurralde.plango.model.Evento;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class MadridApiClientInstrumentedTest {

    @Test
    public void parseEventsHandlesApiFieldsAndNullValues() throws Exception {
        String json = "{\"success\":true,\"result\":{\"records\":["
                + "{\"ID-EVENTO\":123,\"TITULO\":\"Concierto\","
                + "\"FECHA\":\"2026-11-04 18:00:00.0\",\"FECHA-FIN\":null,"
                + "\"DISTRITO-INSTALACION\":\"CENTRO\","
                + "\"TIPO\":\"/contenido/actividades/Musica\",\"GRATUITO\":1,"
                + "\"LATITUD\":\"40.4168\",\"LONGITUD\":\"-3,7038\","
                + "\"NOMBRE-INSTALACION\":\"Centro Cultural\","
                + "\"DIAS-SEMANA\":\"L,M,X\",\"DIAS-EXCLUIDOS\":\"5/11/2026;\"},"
                + "{\"ID-EVENTO\":\"456\",\"TITULO\":\"\",\"GRATUITO\":0}"
                + "]}}";

        List<Evento> eventos = MadridApiClient.parseEvents(json);

        assertEquals(2, eventos.size());
        assertEquals("123", eventos.get(0).getIdentificador());
        assertEquals("Concierto", eventos.get(0).getTitulo());
        assertEquals("CENTRO", eventos.get(0).getDistrito());
        assertTrue(eventos.get(0).isGratuito());
        assertTrue(eventos.get(0).hasValidCoordinates());
        assertEquals("Centro Cultural", eventos.get(0).getNombreInstalacion());
        assertEquals("L,M,X", eventos.get(0).getDiasSemana());
        assertNull(eventos.get(0).getFechaFin());
        assertNull(eventos.get(1).getTitulo());
        assertFalse(eventos.get(1).isGratuito());
    }

    @Test
    public void obtainsFiveEventsFromMadridOpenData() throws Exception {
        MadridApiClient client = new MadridApiClient();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<List<Evento>> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();

        client.obtenerEventos(5, 0, new MadridApiClient.Callback() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                result.set(eventos);
                latch.countDown();
            }

            @Override
            public void onError(Exception error) {
                failure.set(error);
                latch.countDown();
            }
        });

        assertTrue("La consulta no terminó dentro del tiempo esperado",
                latch.await(30, TimeUnit.SECONDS));
        client.cerrar();

        assertNull(failure.get());
        assertNotNull(result.get());
        assertEquals(5, result.get().size());
        assertNotNull(result.get().get(0).getIdentificador());
        assertTrue("La respuesta real no contiene coordenadas válidas",
                result.get().stream().anyMatch(Evento::hasValidCoordinates));
    }
}
