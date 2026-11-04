package com.marinainsaurralde.plango;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
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
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract;
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.MadridApiClient;
import com.marinainsaurralde.plango.model.Evento;
import com.marinainsaurralde.plango.ui.EventoAdapter;

import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int INITIAL_EVENT_LIMIT = 20;
    private final MadridApiClient madridApiClient = new MadridApiClient();
    private EventoAdapter eventoAdapter;
    private boolean eventsRequested;

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
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.buttonSignOut).setOnClickListener(view -> signOut());
        findViewById(R.id.buttonRetryEvents).setOnClickListener(view -> loadEvents());

        RecyclerView eventsList = findViewById(R.id.recyclerEvents);
        eventsList.setLayoutManager(new LinearLayoutManager(this));
        eventoAdapter = new EventoAdapter(this::openEventDetail);
        eventsList.setAdapter(eventoAdapter);

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
        if (!eventsRequested) {
            loadEvents();
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

    private void loadEvents() {
        eventsRequested = true;
        showEventLoading();
        madridApiClient.obtenerEventos(INITIAL_EVENT_LIMIT, 0, new MadridApiClient.Callback() {
            @Override
            public void onSuccess(List<Evento> eventos) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                eventoAdapter.setEventos(eventos);
                showEventResults(eventos.isEmpty());
            }

            @Override
            public void onError(Exception error) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showEventError();
            }
        });
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
            ((TextView) findViewById(R.id.textEventState)).setText(R.string.no_events);
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

    private void openEventDetail(Evento evento) {
        Intent intent = new Intent(this, DetalleEventoActivity.class);
        intent.putExtra(DetalleEventoActivity.EXTRA_EVENTO, evento);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        madridApiClient.cerrar();
        super.onDestroy();
    }
}
