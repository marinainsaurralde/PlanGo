package com.marinainsaurralde.plango;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.marinainsaurralde.plango.data.ReminderScheduler;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // No se consulta Firebase durante el arranque: se restauran los registros locales.
            new ReminderScheduler(context).restoreAllStored();
        }
    }
}
