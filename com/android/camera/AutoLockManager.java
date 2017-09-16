package com.android.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import java.util.WeakHashMap;

public class AutoLockManager {
    private static WeakHashMap<Context, AutoLockManager> sMap = new WeakHashMap();
    private Context mContext;
    private volatile int mCount;
    private Handler mHandler;
    private long mHibernateTimeOut = 480000;
    private long mLastActionTime = 0;
    private boolean mPaused;
    private long mScreenOffTimeOut = 15000;

    private AutoLockManager(Context context) {
        this.mContext = context;
        this.mCount = 0;
        try {
            this.mScreenOffTimeOut = (long) System.getInt(context.getContentResolver(), "screen_off_timeout");
        } catch (SettingNotFoundException e) {
        }
    }

    public static AutoLockManager getInstance(Context context) {
        AutoLockManager instance = (AutoLockManager) sMap.get(context);
        if (instance != null) {
            return instance;
        }
        instance = new AutoLockManager(context);
        sMap.put(context, instance);
        return instance;
    }

    public static void removeInstance(Context context) {
        AutoLockManager instance = (AutoLockManager) sMap.remove(context);
        if (instance != null && instance.mHandler != null) {
            instance.mHandler.getLooper().quit();
        }
    }

    public void onPause() {
        this.mPaused = true;
        if (this.mHandler != null) {
            this.mHandler.removeMessages(1);
        }
    }

    public void onResume() {
        this.mPaused = false;
        hibernateDelayed();
    }

    public void onUserInteraction() {
        synchronized (this) {
            this.mCount++;
        }
        this.mLastActionTime = System.currentTimeMillis();
    }

    public long getLastActionTime() {
        return this.mLastActionTime;
    }

    private void initHandler() {
        if (this.mHandler == null) {
            HandlerThread handlerThread = new HandlerThread("my_handler_thread");
            handlerThread.start();
            this.mHandler = new Handler(handlerThread.getLooper()) {
                public void dispatchMessage(Message msg) {
                    if (msg.what == 0) {
                        AutoLockManager.this.lockSreen();
                    } else if (1 == msg.what) {
                        AutoLockManager.this.hibernate();
                    }
                }
            };
        }
    }

    private void hibernate() {
        ((ActivityBase) this.mContext).onHibernate();
    }

    private void lockSreen() {
        if (this.mCount <= 0 && !this.mPaused) {
            this.mContext.sendBroadcast(new Intent("com.miui.app.ExtraStatusBarManager.TRIGGER_TOGGLE_LOCK"));
        }
    }

    public void removeMessage() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(0);
        }
    }

    public void lockScreenDelayed() {
        initHandler();
        if (this.mHandler.hasMessages(0)) {
            this.mHandler.removeMessages(0);
        }
        this.mHandler.sendEmptyMessageDelayed(0, this.mScreenOffTimeOut);
    }

    public void hibernateDelayed() {
        initHandler();
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
        }
        if (!((ActivityBase) this.mContext).getCurrentModule().isVideoRecording()) {
            this.mHandler.sendEmptyMessageDelayed(1, this.mHibernateTimeOut);
        }
    }
}
