package com.android.camera;

import android.content.Context;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.ref.WeakReference;

public class CrashHandler implements UncaughtExceptionHandler {
    private static CrashHandler sInstance = new CrashHandler();
    private WeakReference<Context> mContextRef;
    private UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler() {
    }

    public static CrashHandler getInstance() {
        return sInstance;
    }

    public void init(Context ctx) {
        this.mContextRef = new WeakReference(ctx);
        if (this.mDefaultHandler == null) {
            this.mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
        }
    }

    public void uncaughtException(Thread thread, Throwable ex) {
        Log.e("CameraFCHandler", "Camera FC, msg=" + ex.getMessage());
        if (this.mContextRef != null) {
            CameraSettings.setEdgeMode((Context) this.mContextRef.get(), false);
            this.mContextRef = null;
        }
        if (this.mDefaultHandler != null) {
            Log.e("CameraFCHandler", "mDefaultHandler=" + this.mDefaultHandler);
            this.mDefaultHandler.uncaughtException(thread, ex);
            this.mDefaultHandler = null;
        }
    }
}
