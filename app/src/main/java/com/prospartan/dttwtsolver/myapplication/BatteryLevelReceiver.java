package com.prospartan.dttwtsolver.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by dima on 22.02.15.
 */
public class BatteryLevelReceiver extends BroadcastReceiver {

    final String LOG_TAG = "myLogs";

    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.ACTION_BATTERY_LOW")) {
            WalkingIconService.zariad = false;
        }
        else
        {
            WalkingIconService.zariad = true;
        }
    }
}