package com.marinainsaurralde.plango;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.marinainsaurralde.plango.data.ReminderScheduler;

import java.util.concurrent.atomic.AtomicBoolean;

public final class BootReceiver extends BroadcastReceiver {
    private static final long AUTH_RECOVERY_WAIT_MILLIS = 8_000L;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        PendingResult pendingResult = goAsync();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        Handler handler = new Handler(Looper.getMainLooper());
        AtomicBoolean completed = new AtomicBoolean(false);
        FirebaseAuth.AuthStateListener[] listenerHolder = new FirebaseAuth.AuthStateListener[1];
        Runnable timeout = () -> {
            if (!completed.compareAndSet(false, true)) return;
            auth.removeAuthStateListener(listenerHolder[0]);
            pendingResult.finish();
        };
        listenerHolder[0] = firebaseAuth -> {
            FirebaseUser user = firebaseAuth.getCurrentUser();
            if (user == null || !completed.compareAndSet(false, true)) return;
            handler.removeCallbacks(timeout);
            auth.removeAuthStateListener(listenerHolder[0]);
            new ReminderScheduler(context).restoreForUser(user.getUid());
            pendingResult.finish();
        };
        handler.postDelayed(timeout, AUTH_RECOVERY_WAIT_MILLIS);
        auth.addAuthStateListener(listenerHolder[0]);
    }
}
