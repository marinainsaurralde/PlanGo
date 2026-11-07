package com.marinainsaurralde.plango.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.marinainsaurralde.plango.model.Plan;

import org.junit.Test;

import java.util.Collections;

public final class EventosStoreTest {
    @Test
    public void partialRefreshDoesNotReplacePreviouslyLoadedPlans() {
        EventosStore store = new EventosStore();
        Plan completePlan = Plan.builder("test", "complete", "Completo").build();
        Plan partialPlan = Plan.builder("test", "partial", "Parcial").build();
        store.setEventos(Collections.singletonList(completePlan));

        assertFalse(store.setEventosFromLoad(Collections.singletonList(partialPlan), false));
        assertEquals(1, store.getEventos().size());
        assertSame(completePlan, store.getEventos().get(0));
    }

    @Test
    public void completeRefreshReplacesPreviouslyLoadedPlans() {
        EventosStore store = new EventosStore();
        Plan previous = Plan.builder("test", "previous", "Anterior").build();
        Plan refreshed = Plan.builder("test", "refreshed", "Actualizado").build();
        store.setEventos(Collections.singletonList(previous));

        assertTrue(store.setEventosFromLoad(Collections.singletonList(refreshed), true));
        assertSame(refreshed, store.getEventos().get(0));
    }

    @Test
    public void firstPartialLoadIsAccepted() {
        EventosStore store = new EventosStore();
        Plan partialPlan = Plan.builder("test", "partial", "Parcial").build();

        assertTrue(store.setEventosFromLoad(Collections.singletonList(partialPlan), false));
        assertTrue(store.isLoaded());
        assertSame(partialPlan, store.getEventos().get(0));
    }
}
