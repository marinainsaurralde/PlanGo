package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.Lugar;
import com.marinainsaurralde.plango.model.PlanIdentity;
import com.marinainsaurralde.plango.model.PlanType;

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
                new FakeSource(null, new IllegalStateException("sports unavailable")),
                new FakePlanSource(Collections.emptyList(), null));
        AtomicReference<EventosMultifuenteLoader.Result> captured = new AtomicReference<>();
        AtomicInteger callbacks = new AtomicInteger();

        loader.cargar(result -> { captured.set(result); callbacks.incrementAndGet(); });

        assertEquals(1, callbacks.get());
        assertTrue(captured.get().hasAnySuccess());
        assertFalse(captured.get().isComplete());
        assertEquals(1, captured.get().getPlanes().size());
    }

    @Test
    public void bothFailuresReturnNoSuccessfulSource() {
        EventosMultifuenteLoader loader = new EventosMultifuenteLoader(
                new FakeSource(null, new IllegalStateException("culture unavailable")),
                new FakeSource(null, new IllegalStateException("sports unavailable")),
                new FakePlanSource(null, new IllegalStateException("parks unavailable")));
        AtomicReference<EventosMultifuenteLoader.Result> captured = new AtomicReference<>();

        loader.cargar(captured::set);

        assertFalse(captured.get().hasAnySuccess());
        assertFalse(captured.get().isComplete());
        assertTrue(captured.get().getPlanes().isEmpty());
    }

    @Test public void combinesThreeSourcesAndDeduplicatesParksByCompositeIdentity() {
        Evento event = new Evento("1", "Evento", null, null, null, null,
                false, false, null, null, null, null, null, null, null, null);
        Plan park = new Lugar(Plan.builder(PlanIdentity.PARQUE_MADRID,"p1","Parque")
                .planType(PlanType.NATURALEZA));
        EventosMultifuenteLoader loader = new EventosMultifuenteLoader(
                new FakeSource(Collections.singletonList(event),null),
                new FakeSource(Collections.emptyList(),null),
                new FakePlanSource(java.util.Arrays.asList(park,park),null));
        AtomicReference<EventosMultifuenteLoader.Result> captured=new AtomicReference<>();
        loader.cargar(captured::set);
        assertTrue(captured.get().isComplete());
        assertEquals(2,captured.get().getPlanes().size());
    }

    @Test public void parksFailureKeepsEventsButMarksLoadAsPartial() {
        Evento event = new Evento("1", "Evento", null, null, null, null,
                false, false, null, null, null, null, null, null, null, null);
        EventosMultifuenteLoader loader = new EventosMultifuenteLoader(
                new FakeSource(Collections.singletonList(event),null),
                new FakeSource(Collections.emptyList(),null),
                new FakePlanSource(null,new IllegalStateException("parks unavailable")));
        AtomicReference<EventosMultifuenteLoader.Result> captured=new AtomicReference<>();
        loader.cargar(captured::set);
        assertTrue(captured.get().hasAnySuccess());
        assertFalse(captured.get().isComplete());
        assertEquals(1,captured.get().getPlanes().size());
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

    private static final class FakePlanSource implements PlanesDataSource {
        private final List<Plan> values; private final Exception error;
        private FakePlanSource(List<Plan> values, Exception error) { this.values=values;this.error=error; }
        @Override public void obtenerTodosLosPlanes(Callback callback) {
            if (error == null) callback.onSuccess(values); else callback.onError(error);
        }
        @Override public void cerrar() { }
    }
}
