package com.marinainsaurralde.plango.data;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.marinainsaurralde.plango.ReminderReceiver;
import com.marinainsaurralde.plango.model.EventReminder;

import java.util.List;

public final class ReminderScheduler {
    public static final String EXTRA_UID = "reminder_uid";
    public static final String EXTRA_EVENT_ID = "reminder_event_id";
    public static final String EXTRA_TRIGGER = "reminder_trigger";
    public static final String EXTRA_RETRY_COUNT = "reminder_retry_count";
    private static final long AUTH_RECOVERY_RETRY_DELAY_MILLIS = 60_000L;

    private final Context context;
    private final AlarmManager alarmManager;

    public ReminderScheduler(Context context) {
        this.context = context.getApplicationContext();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public void schedule(EventReminder reminder) {
        if (reminder == null || reminder.getTriggerAtMillis() <= System.currentTimeMillis()) return;
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                reminder.getTriggerAtMillis(),
                pendingIntent(reminder, PendingIntent.FLAG_UPDATE_CURRENT, 0));
    }

    /** Único reintento corto cuando Firebase todavía no recuperó la sesión local. */
    public void scheduleAuthRecoveryRetry(EventReminder reminder, int retryCount) {
        if (reminder == null || retryCount != 1) return;
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + AUTH_RECOVERY_RETRY_DELAY_MILLIS,
                pendingIntent(reminder, PendingIntent.FLAG_UPDATE_CURRENT, retryCount));
    }

    public void cancel(EventReminder reminder) {
        if (reminder == null) return;
        PendingIntent intent = pendingIntent(reminder, PendingIntent.FLAG_NO_CREATE, 0);
        if (intent != null) {
            alarmManager.cancel(intent);
            intent.cancel();
        }
    }

    public void restoreForUser(String uid) {
        if (uid == null) return;
        restore(new ReminderRepository(context, uid).getAll());
    }

    public void cancelForUser(String uid) {
        if (uid == null) return;
        for (EventReminder reminder : new ReminderRepository(context, uid).getAll()) cancel(reminder);
    }

    public void restoreAllStored() {
        restore(ReminderRepository.getAllStored(context));
    }

    private void restore(List<EventReminder> reminders) {
        long now = System.currentTimeMillis();
        for (EventReminder reminder : reminders) {
            if (reminder.getTriggerAtMillis() > now) schedule(reminder);
            else new ReminderRepository(context, reminder.getUid()).remove(reminder.getEventId());
        }
    }

    private PendingIntent pendingIntent(EventReminder reminder, int lookupFlag, int retryCount) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setData(Uri.parse("plango://reminder/"
                        + Uri.encode(reminder.getUid()) + "/" + Uri.encode(reminder.getEventId())))
                .putExtra(EXTRA_UID, reminder.getUid())
                .putExtra(EXTRA_EVENT_ID, reminder.getEventId())
                .putExtra(EXTRA_TRIGGER, reminder.getTriggerAtMillis())
                .putExtra(EXTRA_RETRY_COUNT, retryCount);
        return PendingIntent.getBroadcast(context, reminder.stableId(), intent,
                lookupFlag | PendingIntent.FLAG_IMMUTABLE);
    }
}
