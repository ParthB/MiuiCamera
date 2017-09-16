package com.android.camera;

import android.app.Activity;
import android.content.Context;
import java.util.Stack;

public class CameraApplicationDelegate {
    private static CameraAppImpl sContext;
    private Stack<Activity> mActivities;
    private boolean mRestoreSetting = false;

    public CameraApplicationDelegate(CameraAppImpl cameraAppImpl) {
        sContext = cameraAppImpl;
    }

    public void onCreate() {
        Util.initialize(sContext);
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
            if (activity != current) {
                activity.finish();
                this.mActivities.remove(activity);
            } else {
                i++;
            }
        }
    }
}
