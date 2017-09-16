package com.android.camera.stereo;

import android.app.Activity;
import android.support.v7.recyclerview.R;
import android.util.Log;
import android.widget.Toast;
import com.android.camera.hardware.MTKCameraProxy.StereoWarningCallback;

public class WarningCallback implements StereoWarningCallback {
    private static final String TAG = WarningCallback.class.getSimpleName();
    private Activity mActivity;
    private boolean mIsDualCameraReady = true;

    public void setActivity(Activity activity) {
        this.mActivity = activity;
        this.mIsDualCameraReady = true;
    }

    public void onWarning(int type) {
        Log.i(TAG, "onWarning type = " + type);
        switch (type) {
            case 0:
                Toast.makeText(this.mActivity, R.string.dual_camera_lens_covered_toast, 0).show();
                this.mIsDualCameraReady = false;
                return;
            case 1:
                Toast.makeText(this.mActivity, R.string.dual_camera_lowlight_toast, 0).show();
                this.mIsDualCameraReady = false;
                return;
            case 2:
                Toast.makeText(this.mActivity, R.string.dual_camera_too_close_toast, 0).show();
                this.mIsDualCameraReady = false;
                return;
            case 3:
                this.mIsDualCameraReady = true;
                return;
            default:
                Log.w(TAG, "Warning message don't need to show");
                return;
        }
    }

    public boolean isDualCameraReady() {
        return this.mIsDualCameraReady;
    }
}
