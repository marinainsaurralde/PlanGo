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

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.ui.EventoAdapter;

import java.util.List;

public class MisPlanesActivity extends AppCompatActivity {

    private final FavoritosRepository repository = FavoritosRepository.getInstance();
    private EventoAdapter adapter;
    private boolean loadError;
    private final FavoritosRepository.Listener listener = new FavoritosRepository.Listener() {
        @Override public void onFavoritosChanged() {
            if (repository.isLoaded()) loadError = false;
            render();
        }
        @Override public void onFavoritosError(Exception error) {
            loadError = true;
            render();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_planes);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myPlansRoot), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        findViewById(R.id.buttonBackFromPlans).setOnClickListener(view -> finish());

        RecyclerView list = findViewById(R.id.recyclerMyPlans);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventoAdapter(this::openDetail, this::removeFavorite);
        adapter.setShowFinished(true);
        list.setAdapter(adapter);
        render();
    }

    private void render() {
        if (adapter == null) return;
        View progress = findViewById(R.id.progressMyPlans);
        View list = findViewById(R.id.recyclerMyPlans);
        View state = findViewById(R.id.layoutMyPlansState);
        TextView stateText = findViewById(R.id.textMyPlansState);

        if (!repository.isLoaded() && !loadError) {
            progress.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
            state.setVisibility(View.GONE);
            return;
        }
        progress.setVisibility(View.GONE);
        if (loadError) {
            list.setVisibility(View.GONE);
            state.setVisibility(View.VISIBLE);
            stateText.setText(R.string.favorites_load_error);
            return;
        }
        List<Evento> favorites = repository.getFavoritos();
        adapter.setEventos(favorites);
        adapter.setFavoriteState(repository.getFavoriteIds(), repository.getPendingIds());
        boolean empty = favorites.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        state.setVisibility(empty ? View.VISIBLE : View.GONE);
        stateText.setText(R.string.no_favorites);
    }

    private void removeFavorite(Evento evento) {
        repository.toggle(evento, error -> Snackbar.make(
                findViewById(R.id.myPlansRoot),
                R.string.favorite_operation_error,
                Snackbar.LENGTH_LONG).show());
    }

    private void openDetail(Evento evento) {
        Intent intent = new Intent(this, DetalleEventoActivity.class);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, evento);
        startActivity(intent);
    }

    @Override protected void onStart() {
        super.onStart();
        repository.addListener(listener);
    }

    @Override protected void onStop() {
        repository.removeListener(listener);
        super.onStop();
    }
}
