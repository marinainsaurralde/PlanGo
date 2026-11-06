package com.marinainsaurralde.plango;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PriceStatus;
import com.marinainsaurralde.plango.model.TemporalKind;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

public class DetalleEventoActivity extends AppCompatActivity {

    public static final String EXTRA_EVENTO = "com.marinainsaurralde.plango.EVENTO";
    private final FavoritosRepository favoritosRepository = FavoritosRepository.getInstance();
    private Plan evento;
    private final FavoritosRepository.Listener favoritosListener = new FavoritosRepository.Listener() {
        @Override public void onFavoritosChanged() { updateFavoriteButton(); }
        @Override public void onFavoritosError(Exception error) { showFavoriteError(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_evento);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.detailRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonBack).setOnClickListener(view -> finish());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            evento = getIntent().getSerializableExtra(EXTRA_EVENTO, Plan.class);
        } else {
            evento = (Plan) getIntent().getSerializableExtra(EXTRA_EVENTO);
        }

        if (evento == null) {
            finish();
            return;
        }
        findViewById(R.id.buttonDetailFavorite).setOnClickListener(view ->
                favoritosRepository.toggle(evento, error -> showFavoriteError()));
        showEvent(evento);
    }

    private void updateFavoriteButton() {
        if (evento == null) return;
        boolean favorite = favoritosRepository.isFavorito(evento.getFavoriteDocumentId());
        boolean pending = favoritosRepository.isPending(evento.getFavoriteDocumentId());
        MaterialButton button = findViewById(R.id.buttonDetailFavorite);
        button.setIconResource(favorite
                ? R.drawable.ic_favorite_filled_24
                : R.drawable.ic_favorite_border_24);
        button.setContentDescription(getString(
                favorite ? R.string.remove_from_plans : R.string.add_to_plans));
        button.setEnabled(!pending && evento.getFavoriteDocumentId() != null);
    }

    private void showFavoriteError() {
        Snackbar.make(findViewById(R.id.detailRoot),
                R.string.favorite_operation_error, Snackbar.LENGTH_LONG).show();
    }

    @Override protected void onStart() {
        super.onStart();
        favoritosRepository.addListener(favoritosListener);
    }

    @Override protected void onStop() {
        favoritosRepository.removeListener(favoritosListener);
        super.onStop();
    }

    private void showEvent(Plan evento) {
        Evento temporalEvent = evento instanceof Evento ? (Evento) evento : null;
        ((TextView) findViewById(R.id.textDetailTitle)).setText(
                evento.getTitulo() == null
                        ? getString(R.string.event_without_title)
                        : evento.getTitulo()
        );
        setOptionalField(R.id.textDetailId, R.string.event_id, evento.getIdentificador());
        if (evento.getTemporalKind() == TemporalKind.LUGAR_PERMANENTE) {
            TextView temporalKind = findViewById(R.id.textDetailDateRange);
            temporalKind.setVisibility(View.VISIBLE);
            temporalKind.setText(R.string.permanent_place);
        } else {
            setOptionalField(R.id.textDetailDateRange, R.string.event_dates,
                    temporalEvent == null ? null : temporalEvent.getPeriodoFormateado());
        }
        findViewById(R.id.textDetailOngoing).setVisibility(
                temporalEvent != null && temporalEvent.isEnCurso(com.marinainsaurralde.plango.model.FiltroEventos.MADRID_ZONE)
                        ? View.VISIBLE
                        : View.GONE
        );
        setOptionalField(R.id.textDetailDistrict, R.string.event_district, evento.getDistrito());
        setOptionalField(R.id.textDetailAddress, R.string.plan_address, evento.getAddress());
        setOptionalField(R.id.textDetailSchedule, R.string.plan_schedule, evento.getSchedule());
        setOptionalField(R.id.textDetailDescription, R.string.plan_description, evento.getDescription());
        setOptionalField(R.id.textDetailVenue, R.string.event_venue, evento.getNombreInstalacion());
        setOptionalField(R.id.textDetailTime, R.string.event_time, temporalEvent == null ? null : temporalEvent.getHora());
        setOptionalField(R.id.textDetailPrice, R.string.event_price, temporalEvent == null ? null : temporalEvent.getPrecio());
        setOptionalField(R.id.textDetailType, R.string.event_type,
                evento.getTemporalKind() == TemporalKind.LUGAR_PERMANENTE
                        ? getString(R.string.plan_type_nature)
                        : temporalEvent == null ? null : temporalEvent.getTipo());
        setOptionalField(R.id.textDetailUrl, R.string.event_url, evento.getUrlOficial());
        TextView coordinates = findViewById(R.id.textDetailCoordinates);
        coordinates.setVisibility(evento.hasValidCoordinates() ? View.VISIBLE : View.GONE);
        if (evento.hasValidCoordinates()) {
            coordinates.setText(getString(
                    R.string.event_coordinates,
                    evento.getLatitud(),
                    evento.getLongitud()
            ));
        }
        ((TextView) findViewById(R.id.textDetailFree)).setText(
                evento.getPriceStatus() == PriceStatus.DESCONOCIDO ? R.string.event_price_unknown
                        : evento.getPriceStatus() == PriceStatus.GRATIS ? R.string.event_free_yes : R.string.event_free_no
        );
    }

    private void setOptionalField(int viewId, int labelId, String value) {
        TextView view = findViewById(viewId);
        boolean hasValue = value != null && !value.trim().isEmpty();
        view.setVisibility(hasValue ? View.VISIBLE : View.GONE);
        if (hasValue) {
            view.setText(getString(labelId, value));
        }
    }
}
