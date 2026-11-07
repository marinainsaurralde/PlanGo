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
import com.marinainsaurralde.plango.data.DeportesApiClient;
import com.marinainsaurralde.plango.data.ParquesApiClient;
import com.marinainsaurralde.plango.data.TurismoApiClient;
import com.marinainsaurralde.plango.data.EventosMultifuenteLoader;
import com.marinainsaurralde.plango.data.EventosStore;
import com.marinainsaurralde.plango.data.ConsultasRepository;
import com.marinainsaurralde.plango.data.ConsultaEstadisticas;
import com.marinainsaurralde.plango.data.AgrupadorEventos;
import com.marinainsaurralde.plango.data.AppPreferences;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.model.PlanType;
import com.marinainsaurralde.plango.model.TemporalKind;
import com.marinainsaurralde.plango.model.ConsultaHistorial;
import com.marinainsaurralde.plango.model.GrupoUbicacion;
import com.marinainsaurralde.plango.model.CategoriaCultural;
import com.marinainsaurralde.plango.model.FiltroEventos;
import com.marinainsaurralde.plango.ui.DistritoAdapter;
import com.marinainsaurralde.plango.ui.FiltrosViewBinder;
import com.marinainsaurralde.plango.ui.MarkerIconFactory;
import com.marinainsaurralde.plango.ui.EventoAdapter;
import com.marinainsaurralde.plango.ui.ExplorarPlanesBottomSheet;

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

    private final EventosMultifuenteLoader eventosLoader = new EventosMultifuenteLoader(
            new MadridApiClient(), new DeportesApiClient(), new ParquesApiClient(),
            new TurismoApiClient());
    private final EventosStore eventosStore = EventosStore.getInstance();
    private List<Plan> mapEvents = Collections.emptyList();
    private GoogleMap googleMap;
    private boolean eventsRequested;
    private FiltrosViewBinder filtrosViewBinder;
    private List<String> availableDistricts = Collections.emptyList();
    private boolean downloadInProgress;
    private boolean pendingInitialList;
    private boolean initialViewHandled;
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

        findViewById(R.id.buttonViewList).setOnClickListener(
                view -> startActivity(new Intent(this, EventosActivity.class))
        );
        findViewById(R.id.buttonMyPlans).setOnClickListener(
                view -> startActivity(new Intent(this, MisPlanesActivity.class))
        );
        findViewById(R.id.buttonHistory).setOnClickListener(view -> startActivity(new Intent(this, HistorialActivity.class)));
        findViewById(R.id.buttonSettings).setOnClickListener(
                view -> startActivity(new Intent(this, SettingsActivity.class)));
        findViewById(R.id.buttonRefreshEvents).setOnClickListener(view -> loadMapEvents(true));
        findViewById(R.id.buttonRetryMapEvents).setOnClickListener(view -> loadMapEvents(false));
        setupFilters();
        findViewById(R.id.buttonDistrict).setOnClickListener(view -> showDistrictBottomSheet());
        findViewById(R.id.buttonExplorePlans).setOnClickListener(view ->
                new ExplorarPlanesBottomSheet(this, eventosStore, this::applyFilters).show());

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

    private void showSignedInUi(FirebaseUser user) {
        AppPreferences preferences = new AppPreferences(this, user.getUid());
        boolean newSession = eventosStore.initializeSession(
                user.getUid(), preferences.initialCriteria());
        if (newSession) {
            pendingInitialList = preferences.startsInList();
            initialViewHandled = false;
            if (filtrosViewBinder != null) filtrosViewBinder.syncSelection();
            updateDistrictButton();
        }
        updateGreeting(preferences.getDisplayName());
        TextView status = findViewById(R.id.textAuthenticationStatus);
        String email = user.getEmail();
        status.setText(email == null
                ? getString(R.string.authenticated_user)
                : getString(R.string.authenticated_as, email));
        findViewById(R.id.buttonSettings).setVisibility(View.VISIBLE);
        findViewById(R.id.authenticatedContent).setVisibility(View.VISIBLE);
        if (eventosStore.isLoaded()) {
            eventsRequested = true;
            populateDistricts();
            applyFilters();
            updateLastRefreshText();
            openInitialListIfNeeded();
        } else if (!eventsRequested) {
            loadMapEvents(false);
        }
    }

    private void showSignedOutUi() {
        ((TextView) findViewById(R.id.textAuthenticationStatus))
                .setText(R.string.authentication_required);
        findViewById(R.id.buttonSettings).setVisibility(View.GONE);
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
        eventosLoader.cargar(result -> {
            if (!result.hasAnySuccess()) {
                if (isFinishing() || isDestroyed()) return;
                finishDownload();
                if(eventosStore.isLoaded()){findViewById(R.id.progressMapEvents).setVisibility(View.GONE);Snackbar.make(findViewById(R.id.main),R.string.refresh_error,Snackbar.LENGTH_LONG).show();}else showMapError();
                return;
            }
            List<Plan> eventos = result.getPlanes();
            eventosStore.setEventos(eventos);
            if (result.isComplete()) {
                ConsultaHistorial stats=ConsultaEstadisticas.calcular(eventos,manual?"manual":"automatica");
                ConsultasRepository.getInstance().guardar(startUid,downloadId,stats,new ConsultasRepository.SaveCallback(){
                    @Override public void onSuccess(){if(manual&&!isFinishing()&&!isDestroyed())Snackbar.make(findViewById(R.id.main),R.string.refresh_success,Snackbar.LENGTH_LONG).show();}
                    @Override public void onError(Exception error){if(!isFinishing()&&!isDestroyed())Snackbar.make(findViewById(R.id.main),R.string.history_save_error,Snackbar.LENGTH_LONG).show();}
                });
            }
            if (isFinishing() || isDestroyed()) return;
            populateDistricts(); applyFilters(); finishDownload(); updateLastRefreshText();
            openInitialListIfNeeded();
            if (!result.isComplete()) {
                Snackbar.make(findViewById(R.id.main), R.string.events_partial_results,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void finishDownload(){downloadInProgress=false;findViewById(R.id.buttonRefreshEvents).setEnabled(true);}

    private void updateGreeting(String displayName) {
        TextView greeting = findViewById(R.id.textGreeting);
        greeting.setText(displayName == null || displayName.trim().isEmpty()
                ? getString(R.string.greeting_generic)
                : getString(R.string.greeting_named, displayName.trim()));
    }

    private void openInitialListIfNeeded() {
        if (initialViewHandled || !pendingInitialList || !eventosStore.isLoaded()) return;
        initialViewHandled = true;
        startActivity(new Intent(this, EventosActivity.class));
    }
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
                district,
                current.getPlanSelection()
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
        for (GrupoUbicacion grupo : AgrupadorEventos.agrupar(mapEvents)) {
            Plan evento=grupo.getFirst();
            MarkerOptions options = new MarkerOptions()
                    .position(new LatLng(grupo.getLatitud(), grupo.getLongitud()));
            if(grupo.size()>1){options.title(getString(R.string.events_marker_count,grupo.size())).icon(markerIconFactory.group(grupo.size()));}
            else {
                options.title(evento.getTitulo()==null?getString(R.string.event_without_title):evento.getTitulo())
                        .icon(evento.getPlanTypes().contains(PlanType.NATURALEZA)
                                ? markerIconFactory.nature()
                                : evento.getPlanTypes().contains(PlanType.TURISMO)
                                ? markerIconFactory.tourism()
                                : markerIconFactory.single(CategoriaCultural.clasificar((Evento) evento)));
                List<String> details=new ArrayList<>();
                if(evento.getNombreInstalacion()!=null)details.add(evento.getNombreInstalacion());
                if(evento.getTemporalKind()==TemporalKind.LUGAR_PERMANENTE)details.add(getString(R.string.permanent_place));
                if(evento instanceof Evento){Evento event=(Evento)evento;if(event.getPeriodoFormateado()!=null)details.add(event.getPeriodoFormateado());if(event.isEnCurso(FiltroEventos.MADRID_ZONE))details.add(getString(R.string.event_ongoing));}
                if(!details.isEmpty())options.snippet(String.join("\n",details));
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
        adapter.setEventos(grupo.getPlanes());

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
            ((TextView) findViewById(R.id.textMapState)).setText(
                    eventosStore.getCriterios().getPlanSelection().getSelectedTypes().isEmpty()
                            ? R.string.select_plan_type_empty
                            : R.string.no_filtered_events);
            findViewById(R.id.buttonRetryMapEvents).setVisibility(View.GONE);
        }
    }

    private void showMapError() {
        findViewById(R.id.progressMapEvents).setVisibility(View.GONE);
        findViewById(R.id.layoutMapState).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.textMapState)).setText(R.string.map_events_error);
        findViewById(R.id.buttonRetryMapEvents).setVisibility(View.VISIBLE);
    }

    private void openEventDetail(Plan evento) {
        Intent intent = new Intent(this, DetalleEventoActivity.class);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, evento);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) updateGreeting(new AppPreferences(this, user.getUid()).getDisplayName());
        if (filtrosViewBinder != null) {
            filtrosViewBinder.syncSelection();
            updateDistrictButton();
            if (eventosStore.isLoaded()) applyFilters();
        }
    }

    @Override
    protected void onDestroy() {
        eventosLoader.cerrar();
        super.onDestroy();
    }
}
