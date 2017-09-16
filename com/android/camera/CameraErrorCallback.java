package com.android.camera;

import android.app.Activity;
import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.util.Log;

public class CameraErrorCallback implements ErrorCallback {
    private Activity mActivity;

    public void onError(int error, Camera camera) {
        if (this.mActivity != null) {
            Log.e("CameraErrorCallback", "Got camera error callback. error=" + error + " paused=" + ((ActivityBase) this.mActivity).isPaused());
            if (error == 100) {
                Log.d("CameraErrorCallback", "media server died");
            } else if (error == 1) {
                Log.d("CameraErrorCallback", "unspecified camera error");
            } else {
                Log.d("CameraErrorCallback", " other unknown error");
            }
            ((ActivityBase) this.mActivity).getCurrentModule().notifyError();
            CameraDataAnalytics.instance().trackEvent("open_camera_fail_key");
            this.mActivity = null;
        }
    }

    public CameraErrorCallback(Activity activity) {
        this.mActivity = activity;
    }
}
