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

public class DetalleEventoActivity extends AppCompatActivity {

    public static final String EXTRA_EVENTO = "com.marinainsaurralde.plango.EVENTO";

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

        Evento evento;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            evento = getIntent().getSerializableExtra(EXTRA_EVENTO, Evento.class);
        } else {
            evento = (Evento) getIntent().getSerializableExtra(EXTRA_EVENTO);
        }

        if (evento == null) {
            finish();
            return;
        }
        showEvent(evento);
    }

    private void showEvent(Evento evento) {
        ((TextView) findViewById(R.id.textDetailTitle)).setText(
                evento.getTitulo() == null
                        ? getString(R.string.event_without_title)
                        : evento.getTitulo()
        );
        setOptionalField(R.id.textDetailId, R.string.event_id, evento.getIdentificador());
        setOptionalField(R.id.textDetailDateRange, R.string.event_dates, evento.getPeriodoFormateado());
        findViewById(R.id.textDetailOngoing).setVisibility(
                evento.isEnCurso(com.marinainsaurralde.plango.model.FiltroEventos.MADRID_ZONE)
                        ? View.VISIBLE
                        : View.GONE
        );
        setOptionalField(R.id.textDetailDistrict, R.string.event_district, evento.getDistrito());
        setOptionalField(R.id.textDetailVenue, R.string.event_venue, evento.getNombreInstalacion());
        setOptionalField(R.id.textDetailType, R.string.event_type, evento.getTipo());
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
                evento.isGratuito() ? R.string.event_free_yes : R.string.event_free_no
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
