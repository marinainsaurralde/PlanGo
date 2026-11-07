package com.marinainsaurralde.plango;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.ui.EventoAdapter;
import com.marinainsaurralde.plango.ui.ExplorarPlanesBottomSheet;

import java.util.List;
import com.google.android.material.snackbar.Snackbar;

public class EventosActivity extends AppCompatActivity {

    private final EventosStore eventosStore = EventosStore.getInstance();
    private EventoAdapter eventoAdapter;
    private final FavoritosRepository favoritosRepository = FavoritosRepository.getInstance();
    private final FavoritosRepository.Listener favoritosListener = new FavoritosRepository.Listener() {
        @Override public void onFavoritosChanged() { updateFavoriteState(); }
        @Override public void onFavoritosError(Exception error) { showFavoriteError(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_eventos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.eventsRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBackToMap).setOnClickListener(view -> finish());
        findViewById(R.id.buttonRetryEvents).setOnClickListener(view -> finish());
        findViewById(R.id.buttonExplorePlans).setOnClickListener(view ->
                new ExplorarPlanesBottomSheet(this, eventosStore, this::showEvents).show());

        RecyclerView eventsList = findViewById(R.id.recyclerEvents);
        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventoAdapter = new EventoAdapter(this::openEventDetail, this::toggleFavorite);
        eventsList.setAdapter(eventoAdapter);

        showEvents();
    }

    private void toggleFavorite(Plan evento) {
        favoritosRepository.toggle(evento, error -> showFavoriteError());
    }

    private void updateFavoriteState() {
        if (eventoAdapter != null) {
            eventoAdapter.setFavoriteState(
                    favoritosRepository.getFavoriteIds(),
                    favoritosRepository.getPendingIds());
        }
    }

    private void showFavoriteError() {
        Snackbar.make(findViewById(R.id.eventsRoot),
                R.string.favorite_operation_error, Snackbar.LENGTH_LONG).show();
    }

    @Override protected void onStart() {
        super.onStart();
        favoritosRepository.addListener(favoritosListener);
    }

    @Override protected void onResume() {
        super.onResume();
        if (eventoAdapter != null) showEvents();
    }

    @Override protected void onStop() {
        favoritosRepository.removeListener(favoritosListener);
        super.onStop();
    }

    private void showEvents() {
        if (!eventosStore.isLoaded()) {
            showEventError();
            return;
        }
        List<Plan> eventos = FiltroEventos.aplicar(
                eventosStore.getEventos(),
                eventosStore.getCriterios()
        );
        eventoAdapter.setEventos(eventos);
        ((TextView) findViewById(R.id.textListResultCount)).setText(
                getResources().getQuantityString(
                        R.plurals.events_found,
                        eventos.size(),
                        eventos.size()
                )
        );
        showEventResults(eventos.isEmpty());
    }

    private void showEventLoading() {
        findViewById(R.id.progressEvents).setVisibility(View.VISIBLE);
        findViewById(R.id.recyclerEvents).setVisibility(View.GONE);
        findViewById(R.id.layoutEventState).setVisibility(View.GONE);
    }

    private void showEventResults(boolean empty) {
        findViewById(R.id.progressEvents).setVisibility(View.GONE);
        findViewById(R.id.recyclerEvents).setVisibility(empty ? View.GONE : View.VISIBLE);
        findViewById(R.id.layoutEventState).setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            ((TextView) findViewById(R.id.textEventState)).setText(
                    eventosStore.getCriterios().getPlanSelection().getSelectedTypes().isEmpty()
                            ? R.string.select_plan_type_empty
                            : R.string.no_events);
            findViewById(R.id.buttonRetryEvents).setVisibility(View.GONE);
        }
    }

    private void showEventError() {
        findViewById(R.id.progressEvents).setVisibility(View.GONE);
        findViewById(R.id.recyclerEvents).setVisibility(View.GONE);
        findViewById(R.id.layoutEventState).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.textEventState)).setText(R.string.events_connection_error);
        findViewById(R.id.buttonRetryEvents).setVisibility(View.VISIBLE);
    }

    private void openEventDetail(Plan evento) {
        Intent intent = new Intent(this, DetalleEventoActivity.class);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, evento);
        startActivity(intent);
    }

}
