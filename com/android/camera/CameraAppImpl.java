package com.android.camera;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import com.android.camera.aosp_porting.FeatureParser;

public class CameraAppImpl extends Application {
    private static CameraApplicationDelegate sApplicationDelegate;

    public void onCreate() {
        super.onCreate();
        FeatureParser.read(this);
        sApplicationDelegate = createApplicationDelegate();
        sApplicationDelegate.onCreate();
    }

    public CameraApplicationDelegate createApplicationDelegate() {
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
