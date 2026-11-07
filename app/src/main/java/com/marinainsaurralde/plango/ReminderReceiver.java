package com.marinainsaurralde.plango;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.ReminderRepository;
import com.marinainsaurralde.plango.data.ReminderScheduler;
import com.marinainsaurralde.plango.model.EventReminder;

import java.util.concurrent.atomic.AtomicBoolean;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "event_reminders";
    private static final long AUTH_RECOVERY_WAIT_MILLIS = 5_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid = intent.getStringExtra(ReminderScheduler.EXTRA_UID);
        String eventId = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_ID);
        long expectedTrigger = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, -1L);
        int retryCount = intent.getIntExtra(ReminderScheduler.EXTRA_RETRY_COUNT, 0);
        if (uid == null || eventId == null) return;
        ReminderRepository repository = new ReminderRepository(context, uid);
        EventReminder reminder = repository.get(eventId);
        if (reminder == null || reminder.getTriggerAtMillis() != expectedTrigger) return;

        PendingResult pendingResult = goAsync();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean(false);
        FirebaseAuth.AuthStateListener[] listenerHolder = new FirebaseAuth.AuthStateListener[1];
        Runnable timeout = () -> {
            if (!completed.compareAndSet(false, true)) return;
            auth.removeAuthStateListener(listenerHolder[0]);
            if (retryCount == 0) {
                new ReminderScheduler(context).scheduleAuthRecoveryRetry(reminder, 1);
            }
            pendingResult.finish();
        };
        listenerHolder[0] = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null || !completed.compareAndSet(false, true)) return;
            handler.removeCallbacks(timeout);
            auth.removeAuthStateListener(listenerHolder[0]);
            if (uid.equals(user.getUid()) && showNotification(context, reminder)) {
                repository.remove(eventId);
            }
            pendingResult.finish();
        };
        handler.postDelayed(timeout, AUTH_RECOVERY_WAIT_MILLIS);
        auth.addAuthStateListener(listenerHolder[0]);
    }

    private boolean showNotification(Context context, EventReminder reminder) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        if (!notificationManager.areNotificationsEnabled()) return false;

        createChannel(context);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            NotificationChannel channel = manager.getNotificationChannel(CHANNEL_ID);
            if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) {
                return false;
            }
        }
        Intent open = new Intent(context, MisPlanesActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, reminder.stableId(), open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_24)
                .setContentTitle(context.getString(R.string.reminder_notification_title))
                .setContentText(context.getString(R.string.reminder_notification_text,
                        reminder.getTitle()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);
        try {
            notificationManager.notify(reminder.stableId(), notification.build());
            return true;
        } catch (SecurityException error) {
            return false;
        }
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }
}
