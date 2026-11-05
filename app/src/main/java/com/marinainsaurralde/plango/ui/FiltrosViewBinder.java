package com.marinainsaurralde.plango.ui;

import android.view.View;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.marinainsaurralde.plango.R;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.model.FiltroEventos;

public final class FiltrosViewBinder {

    private final View root;
    private final EventosStore store;
    private final Runnable onFiltersChanged;

    public FiltrosViewBinder(View root, EventosStore store, Runnable onFiltersChanged) {
        this.root = root;
        this.store = store;
        this.onFiltersChanged = onFiltersChanged;
    }

    public void bind() {
        MaterialButtonToggleGroup dateGroup = root.findViewById(R.id.toggleDateFilter);
        dateGroup.check(buttonIdFor(store.getCriterios().getPeriodo()));
        dateGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) {
                return;
            }
            FiltroEventos.Criterios current = store.getCriterios();
            store.setCriterios(new FiltroEventos.Criterios(
                    periodFor(checkedId),
                    current.isSoloGratuitos(),
                    current.getDistrito(),
                    current.getCategoria()
            ));
            onFiltersChanged.run();
        });

        SwitchMaterial freeSwitch = root.findViewById(R.id.switchFreeOnly);
        freeSwitch.setChecked(store.getCriterios().isSoloGratuitos());
        freeSwitch.setOnCheckedChangeListener((button, checked) -> {
            FiltroEventos.Criterios current = store.getCriterios();
            store.setCriterios(new FiltroEventos.Criterios(
                    current.getPeriodo(),
                    checked,
                    current.getDistrito(),
                    current.getCategoria()
            ));
            onFiltersChanged.run();
        });
    }

    public void syncSelection() {
        MaterialButtonToggleGroup dateGroup = root.findViewById(R.id.toggleDateFilter);
        dateGroup.check(buttonIdFor(store.getCriterios().getPeriodo()));
        ((SwitchMaterial) root.findViewById(R.id.switchFreeOnly))
                .setChecked(store.getCriterios().isSoloGratuitos());
    }

    private static int buttonIdFor(FiltroEventos.Periodo period) {
        switch (period) {
            case TODOS: return R.id.buttonDateAll;
            case HOY: return R.id.buttonDateToday;
            case FIN_DE_SEMANA: return R.id.buttonDateWeekend;
            case PROXIMOS_7_DIAS:
            default: return R.id.buttonDateNext7;
        }
    }

    private static FiltroEventos.Periodo periodFor(int buttonId) {
        if (buttonId == R.id.buttonDateAll) return FiltroEventos.Periodo.TODOS;
        if (buttonId == R.id.buttonDateToday) return FiltroEventos.Periodo.HOY;
        if (buttonId == R.id.buttonDateWeekend) return FiltroEventos.Periodo.FIN_DE_SEMANA;
        return FiltroEventos.Periodo.PROXIMOS_7_DIAS;
    }
}
