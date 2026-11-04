package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.FiltroEventos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventosStore {

    private static final EventosStore INSTANCE = new EventosStore();

    private List<Evento> eventos = Collections.emptyList();
    private FiltroEventos.Criterios criterios = FiltroEventos.Criterios.porDefecto();
    private boolean loaded;
    private long lastUpdatedMillis;

    private EventosStore() {
    }

    public static EventosStore getInstance() {
        return INSTANCE;
    }

    public synchronized void setEventos(List<Evento> nuevosEventos) {
        eventos = Collections.unmodifiableList(new ArrayList<>(nuevosEventos));
        loaded = true;
        lastUpdatedMillis = System.currentTimeMillis();
    }

    public synchronized List<Evento> getEventos() {
        return eventos;
    }

    public synchronized boolean isLoaded() {
        return loaded;
    }

    public synchronized long getLastUpdatedMillis() { return lastUpdatedMillis; }

    public synchronized FiltroEventos.Criterios getCriterios() {
        return criterios;
    }

    public synchronized void setCriterios(FiltroEventos.Criterios nuevosCriterios) {
        criterios = nuevosCriterios;
    }
}
