package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Evento;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class EventosMultifuenteLoaderTest {

    @Test
    public void partialFailureKeepsSuccessfulResultsAndIsNotComplete() {
        Evento event = new Evento("1", "Cultural", null, null,
                null, null, false, false, null, null, null, null, null,
                null, null, null);
        EventosMultifuenteLoader loader = new EventosMultifuenteLoader(
                new FakeSource(Collections.singletonList(event), null),
                new FakeSource(null, new IllegalStateException("sports unavailable")));
        AtomicReference<EventosMultifuenteLoader.Result> captured = new AtomicReference<>();
        AtomicInteger callbacks = new AtomicInteger();

        loader.cargar(result -> { captured.set(result); callbacks.incrementAndGet(); });

        assertEquals(1, callbacks.get());
        assertTrue(captured.get().hasAnySuccess());
        assertFalse(captured.get().isComplete());
        assertEquals(1, captured.get().getEventos().size());
    }

    @Test
    public void bothFailuresReturnNoSuccessfulSource() {
        EventosMultifuenteLoader loader = new EventosMultifuenteLoader(
                new FakeSource(null, new IllegalStateException("culture unavailable")),
                new FakeSource(null, new IllegalStateException("sports unavailable")));
        AtomicReference<EventosMultifuenteLoader.Result> captured = new AtomicReference<>();

        loader.cargar(captured::set);

        assertFalse(captured.get().hasAnySuccess());
        assertFalse(captured.get().isComplete());
        assertTrue(captured.get().getEventos().isEmpty());
    }

    private static final class FakeSource implements EventosDataSource {
        private final List<Evento> values;
        private final Exception error;
        private FakeSource(List<Evento> values, Exception error) { this.values=values;this.error=error; }
        @Override public void obtenerTodosLosEventos(Callback callback) {
            if (error == null) callback.onSuccess(values); else callback.onError(error);
        }
        @Override public void cerrar() { }
    }
}
