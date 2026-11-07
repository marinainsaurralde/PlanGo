package com.marinainsaurralde.plango;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.model.MuestraEvento;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class DetalleConsultaActivity extends AppCompatActivity {
    public static final String EXTRA = "consulta";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_consulta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.queryDetailRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
        findViewById(R.id.buttonBackQuery).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());

        ConsultaHistorial consulta =
                (ConsultaHistorial) getIntent().getSerializableExtra(EXTRA);
        if (consulta == null) {
            finish();
            return;
        }
        SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy, HH:mm",
                new Locale("es", "ES"));
        formatter.setTimeZone(TimeZone.getTimeZone("Europe/Madrid"));
        set(R.id.textQueryDetailDate,
                consulta.fechaConsulta == null ? "—" : formatter.format(consulta.fechaConsulta));
        set(R.id.textQueryDetailTotals, getString(R.string.history_detail_totals,
                consulta.totalEventos, consulta.totalGratuitos, consulta.totalPago,
                consulta.totalSinInformacion));
        fill(findViewById(R.id.layoutDistrictStats), consulta.eventosPorDistrito);
        fill(findViewById(R.id.layoutTypeStats), consulta.eventosPorTipo);
        LinearLayout sample = findViewById(R.id.layoutSample);
        for (MuestraEvento event : consulta.muestraEventos) {
            add(sample, (event.titulo == null
                    ? getString(R.string.event_without_title) : event.titulo)
                    + (event.distrito == null ? "" : " · " + event.distrito));
        }
    }

    private void fill(LinearLayout layout, Map<String, Integer> values) {
        if (values.isEmpty()) {
            add(layout, getString(R.string.no_data));
        } else {
            values.forEach((key, value) -> add(layout, key + ": " + value));
        }
    }

    private void add(LinearLayout layout, String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(getColor(R.color.plango_text));
        view.setPadding(0, 8, 0, 8);
        layout.addView(view);
    }

    private void set(int id, String text) {
        ((TextView) findViewById(id)).setText(text);
    }
}
