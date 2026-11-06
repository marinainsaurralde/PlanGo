package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Evento;

import java.util.List;

public interface EventosDataSource {
    interface Callback {
        void onSuccess(List<Evento> eventos);
        void onError(Exception error);
    }

    void obtenerTodosLosEventos(Callback callback);
    void cerrar();
}
