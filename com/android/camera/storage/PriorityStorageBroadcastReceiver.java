package com.android.camera.storage;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v7.recyclerview.R;
import com.android.camera.CameraAppImpl;

public class PriorityStorageBroadcastReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
    }

    public static boolean isPriorityStorage() {
        boolean z = true;
        Context context = CameraAppImpl.getAndroidContext();
        int state = context.getPackageManager().getComponentEnabledSetting(new ComponentName(context, PriorityStorageBroadcastReceiver.class));
        if (state == 0) {
            return context.getResources().getBoolean(R.bool.priority_storage);
        }
        if (state != 1) {
            z = false;
        }
        return z;
    }

    public static void setPriorityStorage(boolean enabled) {
        int i;
        Context context = CameraAppImpl.getAndroidContext();
        PackageManager pm = context.getPackageManager();
        ComponentName name = new ComponentName(context, PriorityStorageBroadcastReceiver.class);
        if (enabled) {
            i = 1;
        } else {
            i = 2;
        }
        pm.setComponentEnabledSetting(name, i, 1);
    }
}
