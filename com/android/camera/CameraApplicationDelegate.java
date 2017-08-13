package com.android.camera;

import android.app.Activity;
import android.content.Context;
import android.os.SystemProperties;
import com.android.camera.snap.SnapTrigger;
import com.xiaomi.mistatistic.sdk.CustomSettings;
import com.xiaomi.mistatistic.sdk.MiStatInterface;
import java.util.Stack;
import miui.external.ApplicationDelegate;
import miui.os.Build;

public class CameraApplicationDelegate extends ApplicationDelegate {
    private static String appChannel = SystemProperties.get("ro.product.mod_device", Build.DEVICE);
    private static String appID = "2882303761517373386";
    private static String appKey = "5641737344386";
    private static CameraAppImpl sContext;
    private Stack<Activity> mActivities;
    private boolean mRestoreSetting = false;

    public CameraApplicationDelegate(CameraAppImpl cameraAppImpl) {
        sContext = cameraAppImpl;
    }

    public void onCreate() {
        super.onCreate();
        Util.initialize(this);
        MiStatInterface.initialize(getAndroidContext(), appID, appKey, appChannel);
        CustomSettings.setUseSystemUploadingService(true);
        MiStatInterface.setUploadPolicy(4, 180000);
        this.mActivities = new Stack();
        this.mRestoreSetting = true;
    }

    public synchronized Activity getActivity(int index) {
        if (index >= 0) {
            if (index < getActivityCount()) {
                return (Activity) this.mActivities.get(index);
            }
        }
        return null;
    }

    public static Context getAndroidContext() {
        return sContext;
    }

    public void resetRestoreFlag() {
        this.mRestoreSetting = false;
    }

    public boolean getSettingsFlag() {
        return this.mRestoreSetting;
    }

    public synchronized int getActivityCount() {
        return this.mActivities.size();
    }

    public synchronized void addActivity(Activity activity) {
        if (activity != null) {
            this.mActivities.push(activity);
        }
    }

    public synchronized void removeActivity(Activity activity) {
        if (activity != null) {
            this.mActivities.remove(activity);
        }
    }

    public synchronized void closeAllActivitiesBut(Activity current) {
        int i = 0;
        for (int j = 0; j < getActivityCount(); j++) {
            Activity activity = getActivity(i);
            if (activity == current || "android.intent.action.MAIN".equals(activity.getIntent().getAction())) {
                i++;
            } else {
                activity.finish();
                this.mActivities.remove(activity);
            }
        }
        if (SnapTrigger.getInstance().isRunning()) {
            SnapTrigger.getInstance().trigerStop();
        }
    }
}
