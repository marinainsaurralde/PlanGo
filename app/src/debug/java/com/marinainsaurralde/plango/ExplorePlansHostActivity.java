package com.marinainsaurralde.plango;

import android.os.Bundle;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

/** Actividad mínima disponible solo en debug para probar componentes modales aislados. */
public final class ExplorePlansHostActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(new FrameLayout(this));
    }
}
