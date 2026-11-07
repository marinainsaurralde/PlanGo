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

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.ReminderRepository;
import com.marinainsaurralde.plango.data.ReminderScheduler;
import com.marinainsaurralde.plango.model.EventReminder;

public final class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "event_reminders";

    @Override
    public void onReceive(Context context, Intent intent) {
        String uid = intent.getStringExtra(ReminderScheduler.EXTRA_UID);
        String eventId = intent.getStringExtra(ReminderScheduler.EXTRA_EVENT_ID);
        long expectedTrigger = intent.getLongExtra(ReminderScheduler.EXTRA_TRIGGER, -1L);
        if (uid == null || eventId == null) return;
        ReminderRepository repository = new ReminderRepository(context, uid);
        EventReminder reminder = repository.get(eventId);
        if (reminder == null || reminder.getTriggerAtMillis() != expectedTrigger) return;
        repository.remove(eventId);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || !uid.equals(user.getUid())) return;
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        createChannel(context);
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
        NotificationManagerCompat.from(context).notify(reminder.stableId(), notification.build());
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
