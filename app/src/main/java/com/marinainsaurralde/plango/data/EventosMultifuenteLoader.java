package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;

import java.util.Collections;
import java.util.List;

public final class EventosMultifuenteLoader {

    public interface Callback { void onComplete(Result result); }

    public static final class Result {
        private final List<Evento> eventos;
        private final Exception culturalError;
        private final Exception sportsError;

        private Result(List<Evento> eventos, Exception culturalError, Exception sportsError) {
            this.eventos = Collections.unmodifiableList(eventos);
            this.culturalError = culturalError;
            this.sportsError = sportsError;
        }

        public List<Evento> getEventos() { return eventos; }
        public boolean isComplete() { return culturalError == null && sportsError == null; }
        public boolean hasAnySuccess() { return culturalError == null || sportsError == null; }
        public Exception getCulturalError() { return culturalError; }
        public Exception getSportsError() { return sportsError; }
    }

    private final EventosDataSource culturalSource;
    private final EventosDataSource sportsSource;

    public EventosMultifuenteLoader(EventosDataSource culturalSource, EventosDataSource sportsSource) {
        this.culturalSource = culturalSource;
        this.sportsSource = sportsSource;
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
    }

    public void cerrar() {
        culturalSource.cerrar();
        sportsSource.cerrar();
    }

    private static final class LoadState {
        private final Callback callback;
        private boolean culturalFinished;
        private boolean sportsFinished;
        private List<Evento> cultural = Collections.emptyList();
        private List<Evento> sports = Collections.emptyList();
        private Exception culturalError;
        private Exception sportsError;

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

        private void completeIfReady() {
            if (!culturalFinished || !sportsFinished) return;
            callback.onComplete(new Result(
                    EventosMerger.merge(cultural, sports), culturalError, sportsError));
        }
    }
}
