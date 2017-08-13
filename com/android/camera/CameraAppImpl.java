package com.android.camera;

import android.app.Activity;
import android.content.Context;
import miui.external.Application;

public class CameraAppImpl extends Application {
    private static CameraApplicationDelegate sApplicationDelegate;

    public CameraApplicationDelegate onCreateApplicationDelegate() {
        if (sApplicationDelegate == null) {
            sApplicationDelegate = new CameraApplicationDelegate(this);
        }
        CrashHandler.getInstance().init(this);
        return sApplicationDelegate;
    }

    public static Context getAndroidContext() {
        return CameraApplicationDelegate.getAndroidContext();
    }

    public void resetRestoreFlag() {
        sApplicationDelegate.resetRestoreFlag();
    }

    public boolean isNeedRestore() {
        return sApplicationDelegate.getSettingsFlag();
    }

    public void addActivity(Activity activity) {
        sApplicationDelegate.addActivity(activity);
    }

    public void removeActivity(Activity activity) {
        sApplicationDelegate.removeActivity(activity);
    }

    public void closeAllActivitiesBut(Activity current) {
        sApplicationDelegate.closeAllActivitiesBut(current);
    }
}
