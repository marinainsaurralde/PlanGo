package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventosMultifuenteLoader {

    public interface Callback { void onComplete(Result result); }

    public static final class Result {
        private final List<Plan> planes;
        private final Exception culturalError;
        private final Exception sportsError;
        private final Exception parksError;

        private Result(List<Plan> planes, Exception culturalError, Exception sportsError,
                       Exception parksError) {
            this.planes = Collections.unmodifiableList(planes);
            this.culturalError = culturalError;
            this.sportsError = sportsError;
            this.parksError = parksError;
        }

        public List<Plan> getPlanes() { return planes; }
        public boolean isComplete() { return culturalError == null && sportsError == null && parksError == null; }
        public boolean hasAnySuccess() { return culturalError == null || sportsError == null || parksError == null; }
        public Exception getCulturalError() { return culturalError; }
        public Exception getSportsError() { return sportsError; }
        public Exception getParksError() { return parksError; }
    }

    private final EventosDataSource culturalSource;
    private final EventosDataSource sportsSource;
    private final PlanesDataSource parksSource;

    public EventosMultifuenteLoader(EventosDataSource culturalSource, EventosDataSource sportsSource,
                                    PlanesDataSource parksSource) {
        this.culturalSource = culturalSource;
        this.sportsSource = sportsSource;
        this.parksSource = parksSource;
    }

    public void cargar(Callback callback) {
        LoadState state = new LoadState(callback);
        culturalSource.obtenerTodosLosEventos(new EventosDataSource.Callback() {
            @Override public void onSuccess(List<Evento> eventos) { state.cultural(eventos, null); }
            @Override public void onError(Exception error) { state.cultural(null, error); }
        });
        sportsSource.obtenerTodosLosEventos(new EventosDataSource.Callback() {
            @Override public void onSuccess(List<Evento> eventos) { state.sports(eventos, null); }
            @Override public void onError(Exception error) { state.sports(null, error); }
        });
        parksSource.obtenerTodosLosPlanes(new PlanesDataSource.Callback() {
            @Override public void onSuccess(List<Plan> planes) { state.parks(planes, null); }
            @Override public void onError(Exception error) { state.parks(null, error); }
        });
    }

    public void cerrar() {
        culturalSource.cerrar();
        sportsSource.cerrar();
        parksSource.cerrar();
    }

    private static final class LoadState {
        private final Callback callback;
        private boolean culturalFinished;
        private boolean sportsFinished;
        private boolean parksFinished;
        private List<Evento> cultural = Collections.emptyList();
        private List<Evento> sports = Collections.emptyList();
        private List<Plan> parks = Collections.emptyList();
        private Exception culturalError;
        private Exception sportsError;
        private Exception parksError;

        private LoadState(Callback callback) { this.callback = callback; }

        synchronized void cultural(List<Evento> values, Exception error) {
            if (culturalFinished) return;
            culturalFinished = true;
            cultural = values == null ? Collections.emptyList() : values;
            culturalError = error;
            completeIfReady();
        }

        synchronized void sports(List<Evento> values, Exception error) {
            if (sportsFinished) return;
            sportsFinished = true;
            sports = values == null ? Collections.emptyList() : values;
            sportsError = error;
            completeIfReady();
        }

        synchronized void parks(List<Plan> values, Exception error) {
            if (parksFinished) return;
            parksFinished = true;
            parks = values == null ? Collections.emptyList() : values;
            parksError = error;
            completeIfReady();
        }

        private void completeIfReady() {
            if (!culturalFinished || !sportsFinished || !parksFinished) return;
            Map<String, Plan> unique = new LinkedHashMap<>();
            for (Plan plan : EventosMerger.merge(cultural, sports)) {
                if (plan.getCompositeId() != null) unique.put(plan.getCompositeId(), plan);
            }
            for (Plan plan : parks) {
                if (plan != null && plan.getCompositeId() != null) unique.putIfAbsent(plan.getCompositeId(), plan);
            }
            callback.onComplete(new Result(new ArrayList<>(unique.values()), culturalError, sportsError, parksError));
        }
    }
}
