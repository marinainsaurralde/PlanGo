package com.marinainsaurralde.plango.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.marinainsaurralde.plango.model.EventReminder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ReminderRepository {
    private static final String REGISTRY_FILE = "plango_reminder_registry";
    private static final String REGISTRY_UIDS = "uids";
    private static final String FILE_PREFIX = "plango_reminders_";

    private final Context context;
    private final String uid;
    private final SharedPreferences preferences;

    public ReminderRepository(Context context, String uid) {
        this.context = context.getApplicationContext();
        this.uid = uid;
        preferences = this.context.getSharedPreferences(fileName(uid), Context.MODE_PRIVATE);
    }

    public synchronized void save(EventReminder reminder) {
        if (reminder == null || !uid.equals(reminder.getUid())) {
            throw new IllegalArgumentException("Recordatorio de otro usuario");
        }
        preferences.edit().putString(reminder.getEventId(), reminder.toJson()).apply();
        SharedPreferences registry = context.getSharedPreferences(REGISTRY_FILE, Context.MODE_PRIVATE);
        Set<String> uids = new HashSet<>(registry.getStringSet(REGISTRY_UIDS,
                Collections.emptySet()));
        uids.add(uid);
        registry.edit().putStringSet(REGISTRY_UIDS, uids).apply();
    }

    public synchronized EventReminder get(String eventId) {
        return eventId == null ? null : EventReminder.fromJson(preferences.getString(eventId, null));
    }

    public synchronized List<EventReminder> getAll() {
        List<EventReminder> result = new ArrayList<>();
        for (Object value : preferences.getAll().values()) {
            if (!(value instanceof String)) continue;
            EventReminder reminder = EventReminder.fromJson((String) value);
            if (reminder != null && uid.equals(reminder.getUid())) result.add(reminder);
        }
        return result;
    }

    public synchronized void remove(String eventId) {
        if (eventId == null) return;
        preferences.edit().remove(eventId).apply();
        if (preferences.getAll().size() <= 1) cleanupRegistryIfEmpty();
    }

    private void cleanupRegistryIfEmpty() {
        if (!preferences.getAll().isEmpty()) return;
        SharedPreferences registry = context.getSharedPreferences(REGISTRY_FILE, Context.MODE_PRIVATE);
        Set<String> uids = new HashSet<>(registry.getStringSet(REGISTRY_UIDS,
                Collections.emptySet()));
        uids.remove(uid);
        registry.edit().putStringSet(REGISTRY_UIDS, uids).apply();
    }

    public static List<EventReminder> getAllStored(Context context) {
        SharedPreferences registry = context.getSharedPreferences(REGISTRY_FILE,
                Context.MODE_PRIVATE);
        Set<String> uids = new HashSet<>(registry.getStringSet(REGISTRY_UIDS,
                Collections.emptySet()));
        List<EventReminder> result = new ArrayList<>();
        for (String uid : uids) result.addAll(new ReminderRepository(context, uid).getAll());
        return result;
    }

    static String fileName(String uid) {
        if (uid == null || uid.trim().isEmpty()) throw new IllegalArgumentException("UID requerido");
        return FILE_PREFIX + uid.trim();
    }
}
