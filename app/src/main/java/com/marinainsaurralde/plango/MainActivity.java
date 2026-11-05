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
import com.marinainsaurralde.plango.data.ConsultasRepository;
import com.marinainsaurralde.plango.data.ConsultaEstadisticas;
import com.marinainsaurralde.plango.data.AgrupadorEventos;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.model.GrupoUbicacion;
import com.marinainsaurralde.plango.model.CategoriaCultural;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.ui.DistritoAdapter;
import com.marinainsaurralde.plango.ui.FiltrosViewBinder;
import com.marinainsaurralde.plango.ui.MarkerIconFactory;
import com.marinainsaurralde.plango.ui.EventoAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
    private List<CategoriaCultural> availableCategories = Collections.emptyList();
    private boolean downloadInProgress;
    private final MarkerIconFactory markerIconFactory = new MarkerIconFactory();

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
        findViewById(R.id.buttonHistory).setOnClickListener(view -> startActivity(new Intent(this, HistorialActivity.class)));
        findViewById(R.id.buttonRefreshEvents).setOnClickListener(view -> loadMapEvents(true));
        findViewById(R.id.buttonRetryMapEvents).setOnClickListener(view -> loadMapEvents(false));
        setupFilters();
        findViewById(R.id.buttonDistrict).setOnClickListener(view -> showDistrictBottomSheet());
        findViewById(R.id.buttonCategory).setOnClickListener(view -> showCategoryBottomSheet());

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
            updateLastRefreshText();
        } else if (!eventsRequested) {
            loadMapEvents(false);
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
            if (tag instanceof GrupoUbicacion && ((GrupoUbicacion) tag).size() == 1) {
                openEventDetail(((GrupoUbicacion) tag).getFirst());
            }
        });
        googleMap.setOnMarkerClickListener(marker -> {
            Object tag=marker.getTag();
            if(tag instanceof GrupoUbicacion && ((GrupoUbicacion)tag).size()>1){showLocationBottomSheet((GrupoUbicacion)tag);return true;}
            return false;
        });
        renderEventMarkers();
    }

    private void loadMapEvents(boolean manual) {
        if (downloadInProgress) return;
        FirebaseUser startUser=FirebaseAuth.getInstance().getCurrentUser();
        if(startUser==null)return;
        String startUid=startUser.getUid(), downloadId=UUID.randomUUID().toString();
        downloadInProgress=true; findViewById(R.id.buttonRefreshEvents).setEnabled(false);
        eventsRequested = true;
        showMapLoading();
        madridApiClient.obtenerTodosLosEventos(new MadridApiClient.Callback() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                eventosStore.setEventos(eventos);
                ConsultaHistorial stats=ConsultaEstadisticas.calcular(eventos,manual?"manual":"automatica");
                ConsultasRepository.getInstance().guardar(startUid,downloadId,stats,new ConsultasRepository.SaveCallback(){
                    @Override public void onSuccess(){if(manual&&!isFinishing()&&!isDestroyed())Snackbar.make(findViewById(R.id.main),R.string.refresh_success,Snackbar.LENGTH_LONG).show();}
                    @Override public void onError(Exception error){if(!isFinishing()&&!isDestroyed())Snackbar.make(findViewById(R.id.main),R.string.history_save_error,Snackbar.LENGTH_LONG).show();}
                });
                if (isFinishing() || isDestroyed()) return;
                populateDistricts(); applyFilters(); finishDownload(); updateLastRefreshText();
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                finishDownload();
                if(eventosStore.isLoaded()){findViewById(R.id.progressMapEvents).setVisibility(View.GONE);Snackbar.make(findViewById(R.id.main),R.string.refresh_error,Snackbar.LENGTH_LONG).show();}else showMapError();
            }
        });
    }

    private void finishDownload(){downloadInProgress=false;findViewById(R.id.buttonRefreshEvents).setEnabled(true);}
    private void updateLastRefreshText(){long value=eventosStore.getLastUpdatedMillis();TextView view=findViewById(R.id.textLastRefresh);if(value==0){view.setText(R.string.never_updated);return;}DateTimeFormatter f=DateTimeFormatter.ofPattern("d MMM, HH:mm",new Locale("es","ES"));view.setText(getString(R.string.last_updated,Instant.ofEpochMilli(value).atZone(ZoneId.of("Europe/Madrid")).format(f)));}

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
        updateCategoryButton();
    }

    private void populateDistricts() {
        availableDistricts = FiltroEventos.distritosDisponibles(eventosStore.getEventos());
        availableCategories = FiltroEventos.categoriasDisponibles(eventosStore.getEventos());
        updateDistrictButton();
        updateCategoryButton();
    }

    private void updateDistrict(String district) {
        FiltroEventos.Criterios current = eventosStore.getCriterios();
        FiltroEventos.Criterios updated = new FiltroEventos.Criterios(
                current.getPeriodo(),
                current.isSoloGratuitos(),
                district,
                current.getCategoria()
        );
        eventosStore.setCriterios(updated);
        updateDistrictButton();
        if (eventosStore.isLoaded()) {
            applyFilters();
        }
    }

    private void updateCategory(CategoriaCultural category) {
        FiltroEventos.Criterios current = eventosStore.getCriterios();
        eventosStore.setCriterios(new FiltroEventos.Criterios(
                current.getPeriodo(), current.isSoloGratuitos(), current.getDistrito(), category));
        updateCategoryButton();
        if (eventosStore.isLoaded()) applyFilters();
    }

    private void updateCategoryButton() {
        CategoriaCultural selected = eventosStore.getCriterios().getCategoria();
        ((MaterialButton) findViewById(R.id.buttonCategory)).setText(
                selected == null ? getString(R.string.all_categories) : categoryLabel(selected));
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

    private void showCategoryBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_category, null);
        dialog.setContentView(content);
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.all_categories));
        for (CategoriaCultural category : availableCategories) labels.add(categoryLabel(category));
        CategoriaCultural selected = eventosStore.getCriterios().getCategoria();
        DistritoAdapter adapter = new DistritoAdapter(
                selected == null ? getString(R.string.all_categories) : categoryLabel(selected),
                label -> {
                    CategoriaCultural chosen = null;
                    for (CategoriaCultural category : availableCategories) {
                        if (categoryLabel(category).equals(label)) { chosen = category; break; }
                    }
                    updateCategory(chosen);
                    dialog.dismiss();
                });
        adapter.submitList(labels);
        RecyclerView list = content.findViewById(R.id.listCategories);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        dialog.show();
    }

    private String categoryLabel(CategoriaCultural category) {
        switch (category) {
            case TEATRO_ESPECTACULOS: return getString(R.string.category_theater);
            case MUSICA: return getString(R.string.category_music);
            case CINE: return getString(R.string.category_cinema);
            case EXPOSICIONES: return getString(R.string.category_exhibitions);
            case TALLERES_ACTIVIDADES: return getString(R.string.category_workshops);
            case CONFERENCIAS: return getString(R.string.category_conferences);
            default: return getString(R.string.category_other);
        }
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
        for (GrupoUbicacion grupo : AgrupadorEventos.agrupar(mapEvents)) {
            Evento evento=grupo.getFirst();
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(grupo.getLatitud(), grupo.getLongitud()));
            if(grupo.size()>1){options.title(getString(R.string.events_marker_count,grupo.size())).icon(markerIconFactory.group(grupo.size()));}
            else {
                options.title(evento.getTitulo()==null?getString(R.string.event_without_title):evento.getTitulo())
                        .icon(markerIconFactory.single(CategoriaCultural.clasificar(evento)));
                List<String> details=new ArrayList<>();if(evento.getNombreInstalacion()!=null)details.add(evento.getNombreInstalacion());if(evento.getPeriodoFormateado()!=null)details.add(evento.getPeriodoFormateado());if(evento.isEnCurso(FiltroEventos.MADRID_ZONE))details.add(getString(R.string.event_ongoing));if(!details.isEmpty())options.snippet(String.join("\n",details));
            }
            Marker marker = googleMap.addMarker(options);
            if (marker != null) marker.setTag(grupo);
        }
    }

    private void showLocationBottomSheet(GrupoUbicacion grupo) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View content = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_location, null);
        dialog.setContentView(content);
        TextView title = content.findViewById(R.id.textLocationTitle);
        title.setText(grupo.getInstalacion() == null
                ? getString(R.string.shared_location)
                : grupo.getInstalacion());
        ((TextView) content.findViewById(R.id.textLocationCount)).setText(
                getResources().getQuantityString(
                        R.plurals.events_at_location, grupo.size(), grupo.size()));

        FavoritosRepository repository = FavoritosRepository.getInstance();
        EventoAdapter adapter = new EventoAdapter(
                evento -> {
                    dialog.dismiss();
                    openEventDetail(evento);
                },
                evento -> repository.toggle(evento, error -> Snackbar.make(
                        content,
                        R.string.favorite_operation_error,
                        Snackbar.LENGTH_LONG
                ).show())
        );
        adapter.setEventos(grupo.getEventos());

        FavoritosRepository.Listener favoritesListener = new FavoritosRepository.Listener() {
            @Override
            public void onFavoritosChanged() {
                adapter.setFavoriteState(
                        repository.getFavoriteIds(),
                        repository.getPendingIds());
            }

            @Override
            public void onFavoritosError(Exception error) {
                Snackbar.make(content, R.string.favorite_operation_error,
                        Snackbar.LENGTH_LONG).show();
            }
        };
        repository.addListener(favoritesListener);
        dialog.setOnDismissListener(ignored -> repository.removeListener(favoritesListener));

        RecyclerView list = content.findViewById(R.id.recyclerLocationEvents);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        dialog.show();
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
            updateCategoryButton();
        }
    }

    @Override
    protected void onDestroy() {
        madridApiClient.cerrar();
        super.onDestroy();
    }
}
