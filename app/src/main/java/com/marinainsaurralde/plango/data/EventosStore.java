package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.FiltroEventos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class EventosStore {

    private static final EventosStore INSTANCE = new EventosStore();

    private List<Plan> eventos = Collections.emptyList();
    private FiltroEventos.Criterios criterios = FiltroEventos.Criterios.porDefecto();
    private boolean loaded;
    private long lastUpdatedMillis;
    private String sessionUid;

    EventosStore() {
    }

    public static EventosStore getInstance() {
        return INSTANCE;
    }

    public synchronized void setEventos(List<Plan> nuevosEventos) {
        eventos = Collections.unmodifiableList(new ArrayList<>(nuevosEventos));
        loaded = true;
        lastUpdatedMillis = System.currentTimeMillis();
    }

    /** Conserva una carga completa previa cuando una actualización posterior es parcial. */
    public synchronized boolean setEventosFromLoad(List<Plan> nuevosEventos, boolean complete) {
        if (loaded && !complete) return false;
        setEventos(nuevosEventos);
        return true;
    }

    public synchronized List<Plan> getEventos() {
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

    /** Aplica preferencias solo al comenzar una sesión, nunca al recrear pantallas. */
    public synchronized boolean initializeSession(String uid,
                                                  FiltroEventos.Criterios initialCriteria) {
        if (uid == null || uid.equals(sessionUid)) return false;
        sessionUid = uid;
        criterios = initialCriteria == null ? FiltroEventos.Criterios.porDefecto() : initialCriteria;
        return true;
    }

    public synchronized void endSession() {
        sessionUid = null;
        criterios = FiltroEventos.Criterios.porDefecto();
    }
}
