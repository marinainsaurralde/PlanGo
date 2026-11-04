package com.marinainsaurralde.plango;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.ui.EventoAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class EventUiInstrumentedTest {

    @Test
    public void eventCardDisplaysDataAndInvokesClick() {
        Context targetContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Context themedContext = new ContextThemeWrapper(targetContext, R.style.Theme_PlanGo);
        FrameLayout parent = new FrameLayout(themedContext);
        Evento evento = sampleEvent();
        AtomicReference<Evento> clickedEvent = new AtomicReference<>();
        EventoAdapter adapter = new EventoAdapter(clickedEvent::set);
        adapter.setEventos(Collections.singletonList(evento));

        EventoAdapter.EventoViewHolder holder = adapter.onCreateViewHolder(parent, 0);
        adapter.onBindViewHolder(holder, 0);

        assertEquals("Festival de otoño",
                ((TextView) holder.itemView.findViewById(R.id.textEventTitle)).getText().toString());
        assertEquals("CENTRO",
                ((TextView) holder.itemView.findViewById(R.id.textEventDistrict)).getText().toString());
        assertEquals(View.VISIBLE, holder.itemView.findViewById(R.id.textFreeBadge).getVisibility());

        holder.itemView.performClick();
        assertSame(evento, clickedEvent.get());
    }

    @Test
    public void detailActivityDisplaysAvailableEventData() {
        Intent intent = new Intent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                DetalleEventoActivity.class
        );
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, sampleEvent());

        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertTrue(activity instanceof DetalleEventoActivity);
        assertEquals("Festival de otoño",
                ((TextView) activity.findViewById(R.id.textDetailTitle)).getText().toString());
        assertEquals("Distrito: CENTRO",
                ((TextView) activity.findViewById(R.id.textDetailDistrict)).getText().toString());
        assertEquals("Gratuito: Sí",
                ((TextView) activity.findViewById(R.id.textDetailFree)).getText().toString());

        activity.finish();
    }

    private Evento sampleEvent() {
        return new Evento(
                "123",
                "Festival de otoño",
                "2026-11-04 18:00:00.0",
                null,
                "CENTRO",
                "/contenido/actividades/Musica",
                true
        );
    }
}
