package com.android.camera;

import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.view.GraphicBuffer;

public class EmptyService extends Service {
    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            EmptyService.this.stopForeground(true);
            EmptyService.this.stopSelf();
        }
    };

    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onCreate() {
        getApplicationContext().registerReceiver(this.mReceiver, new IntentFilter("android.intent.action.SCREEN_ON"));
        super.onCreate();
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(0, new Notification(this, -1, "camera service", System.currentTimeMillis(), "", "", intent));
        allocGraphicBuffers();
        return 2;
    }

    public void onDestroy() {
        stopForeground(true);
        getApplicationContext().unregisterReceiver(this.mReceiver);
        super.onDestroy();
    }

    private void allocGraphicBuffers() {
        GraphicBuffer.create(12800, 2560, 1, 51).destroy();
    }
}
