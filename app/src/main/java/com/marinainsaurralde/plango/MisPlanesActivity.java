package com.marinainsaurralde.plango;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.FavoritosRepository;
import com.marinainsaurralde.plango.data.MisPlanesSorter;
import com.marinainsaurralde.plango.data.ReminderRepository;
import com.marinainsaurralde.plango.data.ReminderScheduler;
import com.marinainsaurralde.plango.model.EventReminder;
import com.marinainsaurralde.plango.model.Plan;
import com.marinainsaurralde.plango.ui.EventoAdapter;

import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;

public class MisPlanesActivity extends AppCompatActivity {

    private final FavoritosRepository repository = FavoritosRepository.getInstance();
    private EventoAdapter adapter;
    private String uid;
    private ReminderRepository reminderRepository;
    private ReminderScheduler reminderScheduler;
    private Plan pendingReminderPlan;
    private EventReminder.Option pendingReminderOption;
    private boolean loadError;
    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted && pendingReminderPlan != null && pendingReminderOption != null) {
                    activateReminder(pendingReminderPlan, pendingReminderOption);
                } else if (!granted) {
                    Snackbar.make(findViewById(R.id.myPlansRoot),
                            R.string.reminder_permission_denied, Snackbar.LENGTH_LONG).show();
                }
                pendingReminderPlan = null;
                pendingReminderOption = null;
            });
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
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        uid = user.getUid();
        reminderRepository = new ReminderRepository(this, uid);
        reminderScheduler = new ReminderScheduler(this);
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
        adapter = new EventoAdapter(this::openDetail, this::removeFavorite,
                this::showReminderDialog);
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
        List<Plan> favorites = MisPlanesSorter.ordenar(repository.getFavoritos());
        Set<String> favoriteIds = repository.getFavoriteIds();
        for (EventReminder reminder : new ArrayList<>(reminderRepository.getAll())) {
            if (!favoriteIds.contains(reminder.getEventId())) {
                reminderScheduler.cancel(reminder);
                reminderRepository.remove(reminder.getEventId());
            }
        }
        adapter.setEventos(favorites);
        adapter.setFavoriteState(favoriteIds, repository.getPendingIds());
        Set<String> reminderIds = new HashSet<>();
        for (EventReminder reminder : reminderRepository.getAll()) {
            reminderIds.add(reminder.getEventId());
        }
        adapter.setReminderState(reminderIds);
        boolean empty = favorites.isEmpty();
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        state.setVisibility(empty ? View.VISIBLE : View.GONE);
        stateText.setText(R.string.no_favorites);
    }

    private void removeFavorite(Plan evento) {
        EventReminder reminder = reminderRepository.get(evento.getFavoriteDocumentId());
        repository.toggle(evento, new FavoritosRepository.OperationCallback() {
            @Override public void onSuccess() {
                if (reminder != null) {
                    reminderScheduler.cancel(reminder);
                    reminderRepository.remove(reminder.getEventId());
                    runOnUiThread(() -> render());
                }
            }

            @Override public void onError(Exception error) {
                Snackbar.make(findViewById(R.id.myPlansRoot),
                        R.string.favorite_operation_error,
                        Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void showReminderDialog(Plan plan) {
        String eventId = plan.getFavoriteDocumentId();
        EventReminder active = reminderRepository.get(eventId);
        List<EventReminder.Option> options = EventReminder.availableOptions(plan, Instant.now());
        if (options.isEmpty() && active == null) {
            Snackbar.make(findViewById(R.id.myPlansRoot), R.string.reminder_no_options,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        String[] labels = new String[options.size()];
        for (int index = 0; index < options.size(); index++) {
            labels[index] = optionLabel(options.get(index));
        }
        int activeIndex = active == null ? -1 : options.indexOf(active.getOption());
        int[] selectedIndex = {activeIndex >= 0 ? activeIndex : 0};
        TextView approximateNotice = new TextView(this);
        approximateNotice.setText(R.string.reminder_approximate_notice);
        approximateNotice.setTextColor(ContextCompat.getColor(this, R.color.plango_text_secondary));
        int horizontalPadding = Math.round(24 * getResources().getDisplayMetrics().density);
        int verticalPadding = Math.round(8 * getResources().getDisplayMetrics().density);
        approximateNotice.setPadding(horizontalPadding, verticalPadding,
                horizontalPadding, verticalPadding);
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(active == null ? R.string.reminder_create_title
                        : R.string.reminder_modify_title)
                .setView(approximateNotice)
                .setNegativeButton(R.string.cancel, null);
        if (!options.isEmpty()) {
            dialog.setSingleChoiceItems(labels, selectedIndex[0],
                    (ignored, which) -> selectedIndex[0] = which);
            dialog.setPositiveButton(R.string.save,
                    (ignored, which) -> requestActivation(plan,
                            options.get(selectedIndex[0])));
        }
        if (active != null) {
            dialog.setNeutralButton(R.string.reminder_cancel_action,
                    (ignored, which) -> cancelReminder(active));
        }
        dialog.show();
    }

    private String optionLabel(EventReminder.Option option) {
        return getString(option == EventReminder.Option.DAY_BEFORE_18
                ? R.string.reminder_day_before : R.string.reminder_same_day);
    }

    private void requestActivation(Plan plan, EventReminder.Option option) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            pendingReminderPlan = plan;
            pendingReminderOption = option;
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        activateReminder(plan, option);
    }

    private void activateReminder(Plan plan, EventReminder.Option option) {
        EventReminder reminder = EventReminder.create(uid, plan, option, Instant.now());
        if (reminder == null) {
            Snackbar.make(findViewById(R.id.myPlansRoot), R.string.reminder_no_options,
                    Snackbar.LENGTH_LONG).show();
            return;
        }
        EventReminder previous = reminderRepository.get(reminder.getEventId());
        try {
            if (previous != null) reminderScheduler.cancel(previous);
            reminderRepository.save(reminder);
            reminderScheduler.schedule(reminder);
            render();
            Snackbar.make(findViewById(R.id.myPlansRoot), R.string.reminder_saved,
                    Snackbar.LENGTH_LONG).show();
        } catch (RuntimeException error) {
            reminderRepository.remove(reminder.getEventId());
            Snackbar.make(findViewById(R.id.myPlansRoot), R.string.reminder_error,
                    Snackbar.LENGTH_LONG).show();
        }
    }

    private void cancelReminder(EventReminder reminder) {
        reminderScheduler.cancel(reminder);
        reminderRepository.remove(reminder.getEventId());
        render();
        Snackbar.make(findViewById(R.id.myPlansRoot), R.string.reminder_cancelled,
                Snackbar.LENGTH_LONG).show();
    }

    private void openDetail(Plan evento) {
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
