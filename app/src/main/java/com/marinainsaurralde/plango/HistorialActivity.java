package com.marinainsaurralde.plango;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.EdgeToEdge;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.marinainsaurralde.plango.data.ConsultasRepository;
import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.ui.ConsultaAdapter;

import java.util.List;

public class HistorialActivity extends AppCompatActivity {
    private ConsultaAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_historial);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.historyRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                });
        findViewById(R.id.buttonBackHistory).setOnClickListener(view ->
                getOnBackPressedDispatcher().onBackPressed());
        RecyclerView list = findViewById(R.id.recyclerHistory);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ConsultaAdapter(this::open);
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        show(true, false, null);
        ConsultasRepository.getInstance().cargar(new ConsultasRepository.LoadCallback() {
            @Override
            public void onSuccess(List<ConsultaHistorial> consultas) {
                if (isFinishing()) return;
                adapter.setItems(consultas);
                show(false, consultas.isEmpty(),
                        consultas.isEmpty() ? getString(R.string.history_empty) : null);
            }

            @Override
            public void onError(Exception error) {
                if (!isFinishing()) show(false, true, getString(R.string.history_error));
            }
        });
    }

    private void show(boolean loading, boolean state, String text) {
        findViewById(R.id.progressHistory).setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.recyclerHistory).setVisibility(
                !loading && !state ? View.VISIBLE : View.GONE);
        TextView stateView = findViewById(R.id.textHistoryState);
        stateView.setVisibility(state ? View.VISIBLE : View.GONE);
        if (text != null) stateView.setText(text);
    }

    private void open(ConsultaHistorial consulta) {
        Intent intent = new Intent(this, DetalleConsultaActivity.class);
        intent.putExtra(DetalleConsultaActivity.EXTRA, consulta);
        startActivity(intent);
    }
}
