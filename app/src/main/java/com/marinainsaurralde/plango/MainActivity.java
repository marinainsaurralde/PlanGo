package com.marinainsaurralde.plango;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.MadridApiClient;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.ui.DistritoAdapter;
import com.marinainsaurralde.plango.ui.FiltrosViewBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final LatLng MADRID_CENTER = new LatLng(40.4168, -3.7038);
    private static final float MADRID_ZOOM = 11.5f;

    private final MadridApiClient madridApiClient = new MadridApiClient();
    private final EventosStore eventosStore = EventosStore.getInstance();
    private List<Evento> mapEvents = Collections.emptyList();
    private GoogleMap googleMap;
    private boolean eventsRequested;
    private FiltrosViewBinder filtrosViewBinder;
    private List<String> availableDistricts = Collections.emptyList();

    private final ActivityResultLauncher<Intent> signInLauncher =
            registerForActivityResult(
                    new FirebaseAuthUIActivityResultContract(),
                    this::onSignInResult
            );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonSignOut).setOnClickListener(view -> signOut());
        findViewById(R.id.buttonViewList).setOnClickListener(
                view -> startActivity(new Intent(this, EventosActivity.class))
        );
        findViewById(R.id.buttonMyPlans).setOnClickListener(
                view -> startActivity(new Intent(this, MisPlanesActivity.class))
        );
        findViewById(R.id.buttonRetryMapEvents).setOnClickListener(view -> loadMapEvents());
        setupFilters();
        findViewById(R.id.buttonDistrict).setOnClickListener(view -> showDistrictBottomSheet());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this::onMapReady);
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            showSignedOutUi();
            if (savedInstanceState == null) {
                startSignIn();
            }
        } else {
            showSignedInUi(currentUser);
        }
    }

    private void startSignIn() {
        Intent signInIntent = AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()
                ))
                .setTheme(R.style.Theme_PlanGo_FirebaseUI)
                .build();

        signInLauncher.launch(signInIntent);
    }

    private void onSignInResult(FirebaseAuthUIAuthenticationResult result) {
        IdpResponse response = result.getIdpResponse();

        if (result.getResultCode() == RESULT_OK) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                showSignedInUi(currentUser);
                return;
            }
            showAuthenticationMessage(R.string.auth_error, true);
            return;
        }

        showSignedOutUi();
        if (response == null) {
            showAuthenticationMessage(R.string.auth_cancelled, true);
        } else if (response.getError() != null
                && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
            showAuthenticationMessage(R.string.auth_no_network, true);
        } else {
            showAuthenticationMessage(R.string.auth_error, true);
        }
    }

    private void signOut() {
        MaterialButton signOutButton = findViewById(R.id.buttonSignOut);
        signOutButton.setEnabled(false);

        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(task -> {
                    signOutButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        showSignedOutUi();
                        startSignIn();
                    } else {
                        showAuthenticationMessage(R.string.sign_out_error, false);
                    }
                });
    }

    private void showSignedInUi(FirebaseUser user) {
        TextView status = findViewById(R.id.textAuthenticationStatus);
        String email = user.getEmail();
        status.setText(email == null
                ? getString(R.string.authenticated_user)
                : getString(R.string.authenticated_as, email));
        findViewById(R.id.buttonSignOut).setVisibility(View.VISIBLE);
        findViewById(R.id.authenticatedContent).setVisibility(View.VISIBLE);
        if (eventosStore.isLoaded()) {
            eventsRequested = true;
            populateDistricts();
            applyFilters();
        } else if (!eventsRequested) {
            loadMapEvents();
        }
    }

    private void showSignedOutUi() {
        ((TextView) findViewById(R.id.textAuthenticationStatus))
                .setText(R.string.authentication_required);
        findViewById(R.id.buttonSignOut).setVisibility(View.GONE);
        findViewById(R.id.authenticatedContent).setVisibility(View.GONE);
    }

    private void showAuthenticationMessage(int message, boolean allowRetry) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_INDEFINITE);
        if (allowRetry) {
            snackbar.setAction(R.string.retry, view -> startSignIn());
        } else {
            snackbar.setDuration(Snackbar.LENGTH_LONG);
        }
        snackbar.show();
    }

    private void onMapReady(GoogleMap map) {
        googleMap = map;
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MADRID_CENTER, MADRID_ZOOM));
        googleMap.setOnInfoWindowClickListener(marker -> {
            Object tag = marker.getTag();
            if (tag instanceof Evento) {
                openEventDetail((Evento) tag);
            }
        });
        renderEventMarkers();
    }

    private void loadMapEvents() {
        eventsRequested = true;
        showMapLoading();
        madridApiClient.obtenerTodosLosEventos(new MadridApiClient.Callback() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                eventosStore.setEventos(eventos);
                populateDistricts();
                applyFilters();
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showMapError();
            }
        });
    }

    private void setupFilters() {
        filtrosViewBinder = new FiltrosViewBinder(
                findViewById(R.id.authenticatedContent),
                eventosStore,
                () -> {
                    if (eventosStore.isLoaded()) {
                        applyFilters();
                    }
                }
        );
        filtrosViewBinder.bind();
        updateDistrictButton();
    }

    private void populateDistricts() {
        availableDistricts = FiltroEventos.distritosDisponibles(eventosStore.getEventos());
        updateDistrictButton();
    }

    private void updateDistrict(String district) {
        FiltroEventos.Criterios current = eventosStore.getCriterios();
        FiltroEventos.Criterios updated = new FiltroEventos.Criterios(
                current.getPeriodo(),
                current.isSoloGratuitos(),
                district
        );
        eventosStore.setCriterios(updated);
        updateDistrictButton();
        if (eventosStore.isLoaded()) {
            applyFilters();
        }
    }

    private void updateDistrictButton() {
        String selected = eventosStore.getCriterios().getDistrito();
        ((MaterialButton) findViewById(R.id.buttonDistrict)).setText(
                selected == null ? getString(R.string.all_districts) : selected
        );
    }

    private void showDistrictBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_district, null);
        dialog.setContentView(content);

        String allDistricts = getString(R.string.all_districts);
        List<String> allOptions = new ArrayList<>();
        allOptions.add(allDistricts);
        allOptions.addAll(availableDistricts);
        String selected = eventosStore.getCriterios().getDistrito();
        DistritoAdapter adapter = new DistritoAdapter(
                selected == null ? allDistricts : selected,
                district -> {
                    updateDistrict(allDistricts.equals(district) ? null : district);
                    dialog.dismiss();
                }
        );
        adapter.submitList(allOptions);

        RecyclerView list = content.findViewById(R.id.listDistricts);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        EditText search = content.findViewById(R.id.inputDistrictSearch);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toLowerCase(Locale.ROOT);
                if (query.isEmpty()) {
                    adapter.submitList(allOptions);
                    return;
                }
                List<String> filtered = new ArrayList<>();
                for (String option : allOptions) {
                    if (option.toLowerCase(Locale.ROOT).contains(query)) {
                        filtered.add(option);
                    }
                }
                adapter.submitList(filtered);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        dialog.show();
    }

    private void applyFilters() {
        mapEvents = FiltroEventos.aplicar(
                eventosStore.getEventos(),
                eventosStore.getCriterios()
        );
        ((TextView) findViewById(R.id.textResultCount)).setText(
                getResources().getQuantityString(
                        R.plurals.events_found,
                        mapEvents.size(),
                        mapEvents.size()
                )
        );
        renderEventMarkers();
        showMapLoaded(mapEvents.isEmpty());
    }

    private void renderEventMarkers() {
        if (googleMap == null) {
            return;
        }
        googleMap.clear();
        for (Evento evento : mapEvents) {
            if (!evento.hasValidCoordinates()) {
                continue;
            }
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(evento.getLatitud(), evento.getLongitud()))
                    .title(evento.getTitulo() == null
                            ? getString(R.string.event_without_title)
                            : evento.getTitulo());
            if (evento.getNombreInstalacion() != null) {
                options.snippet(evento.getNombreInstalacion());
            }
            Marker marker = googleMap.addMarker(options);
            if (marker != null) {
                marker.setTag(evento);
            }
        }
    }

    private void showMapLoading() {
        findViewById(R.id.progressMapEvents).setVisibility(View.VISIBLE);
        findViewById(R.id.layoutMapState).setVisibility(View.GONE);
    }

    private void showMapLoaded(boolean empty) {
        findViewById(R.id.progressMapEvents).setVisibility(View.GONE);
        findViewById(R.id.layoutMapState).setVisibility(empty ? View.VISIBLE : View.GONE);
        if (empty) {
            ((TextView) findViewById(R.id.textMapState)).setText(R.string.no_filtered_events);
            findViewById(R.id.buttonRetryMapEvents).setVisibility(View.GONE);
        }
    }

    private void showMapError() {
        findViewById(R.id.progressMapEvents).setVisibility(View.GONE);
        findViewById(R.id.layoutMapState).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.textMapState)).setText(R.string.map_events_error);
        findViewById(R.id.buttonRetryMapEvents).setVisibility(View.VISIBLE);
    }

    private void openEventDetail(Evento evento) {
        Intent intent = new Intent(this, DetalleEventoActivity.class);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, evento);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (filtrosViewBinder != null) {
            filtrosViewBinder.syncSelection();
            updateDistrictButton();
        }
    }

    @Override
    protected void onDestroy() {
        madridApiClient.cerrar();
        super.onDestroy();
    }
}
