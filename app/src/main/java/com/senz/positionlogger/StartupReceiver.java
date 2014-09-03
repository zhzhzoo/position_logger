package com.senz.positionlogger;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;

public class StartupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        LoggerService.initConfiguration(context);
        if (LoggerService.autoStart) {
            L.d("autostarting service");
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            LoggerService.wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Starting logger service");
            LoggerService.wakeLock.acquire();
            context.startService(new Intent(context, LoggerService.class));
        }
    }
}