package com.android.camera.camera_adapter;

import android.hardware.Camera.Parameters;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.Camera;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.module.CameraModule;
import java.util.ArrayList;
import java.util.List;

public class CameraPadOne extends CameraModule {
    private final String KEY_AUTO_ROTATE = "jpeg-auto-rotate";
    private final String KEY_FLIP = "jpeg-flip";
    private final String TAG = "Camera";

    private void updateCameraParametersPreference(Parameters parameters) {
        parameters.set("jpeg-auto-rotate", "true");
        if ((EffectController.getInstance().hasEffect() ? Device.isEffectWatermarkFilted() : false) || !CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            sProxy.setTimeWatermark(parameters, "off");
        } else {
            sProxy.setTimeWatermark(parameters, "on");
        }
        Log.i("Camera", "SetTimeWatermark =" + sProxy.getTimeWatermark(parameters));
        String faceBeauty = this.mPreferences.getString("pref_camera_face_beauty_key", getString(R.string.pref_face_beauty_default));
        sProxy.setStillBeautify(parameters, faceBeauty);
        Log.i("Camera", "SetStillBeautify =" + faceBeauty);
        String showGenderAndAge = this.mPreferences.getString("pref_camera_show_gender_age_key", getString(R.string.pref_camera_show_gender_age_default));
        getUIController().getFaceView().setShowGenderAndAge(showGenderAndAge);
        Log.i("Camera", "SetShowGenderAndAge =" + showGenderAndAge);
        sProxy.setMultiFaceBeautify(parameters, "on");
        Log.i("Camera", "SetMultiFaceBeautify =on");
        if (isFrontMirror()) {
            parameters.set("jpeg-flip", "true");
        } else {
            parameters.set("jpeg-flip", "false");
        }
        Log.i("Camera", "Set JPEG horizontal flip = " + parameters.get("jpeg-flip"));
    }

    protected void updateCameraParametersPreference() {
        super.updateCameraParametersPreference();
        updateCameraParametersPreference(this.mParameters);
    }

    protected boolean isZeroShotMode() {
        return true;
    }

    public static List<String> getLayoutModeKeys(Camera activity, boolean isBackCamera, boolean isImageCaptureIntent) {
        List<String> keys = new ArrayList();
        if (isBackCamera) {
            keys.add("pref_camera_face_beauty_key");
        } else {
            keys.add("pref_camera_face_beauty_key");
        }
        return keys;
    }
}
