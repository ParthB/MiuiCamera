package com.android.camera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;

public class CameraButtonIntentReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        CameraHolder holder = CameraHolder.instance();
        int cameraId = CameraSettings.readPreferredCameraId(CameraSettingPreferences.instance());
        if (PermissionManager.checkCameraLaunchPermissions() && holder.tryOpen(cameraId) != null) {
            holder.keep();
            holder.release();
            Intent i = new Intent("android.intent.action.MAIN");
            i.setClass(context, Camera.class);
            i.addCategory("android.intent.category.LAUNCHER");
            i.setFlags(268435456);
            context.startActivity(i);
        }
    }
}
