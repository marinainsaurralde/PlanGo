package com.marinainsaurralde.plango.data;

import com.marinainsaurralde.plango.model.Plan;
import java.util.List;

public interface PlanesDataSource {
    interface Callback {
        void onSuccess(List<Plan> planes);
        void onError(Exception error);
    }

    void obtenerTodosLosPlanes(Callback callback);
    void cerrar();
}
