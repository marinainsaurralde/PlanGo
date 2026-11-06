package com.marinainsaurralde.plango;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.ui.FiltrosViewBinder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RunWith(AndroidJUnit4.class)
public class FilterControlsInstrumentedTest {

    @Test
    public void dateButtonsUpdateStoreResultsAndRestoreSelection() {
        EventosStore store = EventosStore.getInstance();
        store.setCriterios(FiltroEventos.Criterios.porDefecto());
        store.setEventos(Arrays.asList(
                event("today", "2026-11-04", "2026-11-04"),
                event("later", "2026-11-08", "2026-11-08")
        ));

        Context context = new ContextThemeWrapper(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                R.style.Theme_PlanGo
        );
        AtomicReference<List<Plan>> visibleEvents = new AtomicReference<>();
        AtomicReference<FilterView> firstView = new AtomicReference<>();

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            FilterView view = createFilterView(context);
            firstView.set(view);
            FiltrosViewBinder binder = new FiltrosViewBinder(view.root, store, () -> {
                List<Plan> filtered = FiltroEventos.aplicar(
                        store.getEventos(),
                        store.getCriterios(),
                        java.time.LocalDate.of(2026, 11, 4)
                );
                visibleEvents.set(filtered);
                view.counter.setText(String.valueOf(filtered.size()));
            });
            binder.bind();

            view.today.performClick();
            assertSame(FiltroEventos.Periodo.HOY, store.getCriterios().getPeriodo());
            assertEquals("1", view.counter.getText().toString());
            assertEquals("today", visibleEvents.get().get(0).getIdentificador());

            view.nextSeven.performClick();
            assertSame(FiltroEventos.Periodo.PROXIMOS_7_DIAS,
                    store.getCriterios().getPeriodo());
            assertEquals("2", view.counter.getText().toString());

            List<Plan> listResults = FiltroEventos.aplicar(
                    store.getEventos(),
                    store.getCriterios(),
                    java.time.LocalDate.of(2026, 11, 4)
            );
            assertEquals(visibleEvents.get(), listResults);

            FilterView recreatedMap = createFilterView(context);
            FiltrosViewBinder recreatedBinder = new FiltrosViewBinder(
                    recreatedMap.root,
                    store,
                    () -> { }
            );
            recreatedBinder.bind();
            assertEquals(R.id.buttonDateNext7, recreatedMap.group.getCheckedButtonId());
        });

        store.setCriterios(FiltroEventos.Criterios.porDefecto());
    }

    private FilterView createFilterView(Context context) {
        LinearLayout root = new LinearLayout(context);
        MaterialButtonToggleGroup group = new MaterialButtonToggleGroup(context);
        group.setId(R.id.toggleDateFilter);
        group.setSingleSelection(true);
        group.setSelectionRequired(true);

        addButton(context, group, R.id.buttonDateAll);
        MaterialButton today = addButton(context, group, R.id.buttonDateToday);
        addButton(context, group, R.id.buttonDateWeekend);
        MaterialButton nextSeven = addButton(context, group, R.id.buttonDateNext7);
        root.addView(group);

        SwitchMaterial freeOnly = new SwitchMaterial(context);
        freeOnly.setId(R.id.switchFreeOnly);
        root.addView(freeOnly);

        TextView counter = new TextView(context);
        counter.setId(R.id.textResultCount);
        root.addView(counter);
        return new FilterView(root, group, today, nextSeven, counter);
    }

    private MaterialButton addButton(
            Context context,
            MaterialButtonToggleGroup group,
            int id
    ) {
        MaterialButton button = new MaterialButton(context);
        button.setId(id);
        group.addView(button);
        return button;
    }

    private Evento event(String id, String start, String end) {
        return new Evento(
                id, id,
                start + " 00:00:00.0",
                end + " 23:59:00.0",
                "CENTRO", "tipo", true,
                40.4168, -3.7038, "Instalación",
                null, null
        );
    }

    private static final class FilterView {
        private final LinearLayout root;
        private final MaterialButtonToggleGroup group;
        private final MaterialButton today;
        private final MaterialButton nextSeven;
        private final TextView counter;

        private FilterView(
                LinearLayout root,
                MaterialButtonToggleGroup group,
                MaterialButton today,
                MaterialButton nextSeven,
                TextView counter
        ) {
            this.root = root;
            this.group = group;
            this.today = today;
            this.nextSeven = nextSeven;
            this.counter = counter;
        }
    }
}
