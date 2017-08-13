package com.android.camera.camera_adapter;

import android.hardware.Camera.Parameters;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.LCCameraProxy;
import com.android.camera.module.BaseModule;
import com.android.camera.module.CameraModule;
import java.util.List;

public class CameraLC extends CameraModule {
    private static LCCameraProxy sProxy = ((LCCameraProxy) CameraHardwareProxy.getDeviceProxy());
    private boolean mIsLongShotMode = false;

    private void lcUpdateCameraParametersPreference() {
        boolean isEffectWatermarkFilted;
        setBeautyParams();
        if (Device.isSupportedIntelligentBeautify()) {
            String showGenderAndAge = this.mPreferences.getString("pref_camera_show_gender_age_key", getString(R.string.pref_camera_show_gender_age_default));
            getUIController().getFaceView().setShowGenderAndAge(showGenderAndAge);
            Log.i("Camera", "SetShowGenderAndAge =" + showGenderAndAge);
            if ("on".equals(showGenderAndAge)) {
                this.mParameters.set("xiaomi-preview-rotation", this.mOrientation == -1 ? 0 : this.mOrientation);
            }
        }
        if (EffectController.getInstance().hasEffect()) {
            isEffectWatermarkFilted = Device.isEffectWatermarkFilted();
        } else {
            isEffectWatermarkFilted = false;
        }
        if (isEffectWatermarkFilted || this.mMutexModePicker.isUbiFocus() || !CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            sProxy.setTimeWatermark(this.mParameters, "off");
        } else {
            sProxy.setTimeWatermark(this.mParameters, "on");
        }
        String iso = getManualValue("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
        if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
            Log.v("Camera", "ISO value = " + iso);
            sProxy.setISOValue(this.mParameters, iso);
        }
        String saturationStr = this.mPreferences.getString("pref_qc_camera_saturation_key", getString(R.string.pref_camera_saturation_default));
        Log.v("Camera", "Saturation value = " + saturationStr);
        sProxy.setSaturation(this.mParameters, saturationStr);
        String contrastStr = this.mPreferences.getString("pref_qc_camera_contrast_key", getString(R.string.pref_camera_contrast_default));
        Log.v("Camera", "Contrast value = " + contrastStr);
        sProxy.setContrast(this.mParameters, contrastStr);
        String sharpnessStr = this.mPreferences.getString("pref_qc_camera_sharpness_key", getString(R.string.pref_camera_sharpness_default));
        Log.v("Camera", "Sharpness value = " + sharpnessStr);
        sProxy.setSharpness(this.mParameters, sharpnessStr);
        setPictureFlipIfNeed(this.mParameters);
        this.mIsZSLMode = getZSL();
        sProxy.setZSLMode(this.mParameters, this.mIsZSLMode ? "true" : "false");
        if (this.mIsZSLMode && this.mMultiSnapStatus && !this.mIsLongShotMode) {
            this.mIsLongShotMode = true;
            sProxy.setBurstShotNum(this.mParameters, BURST_SHOOTING_COUNT);
        } else if (this.mIsLongShotMode) {
            this.mIsLongShotMode = false;
            sProxy.setBurstShotNum(this.mParameters, 1);
        } else {
            sProxy.setBurstShotNum(this.mParameters, 1);
        }
        Log.v("Camera", "Long Shot mode value = " + isLongShotMode());
    }

    protected void applyMultiShutParameters(boolean startshut) {
        sProxy.setBurstShotNum(this.mParameters, startshut ? BURST_SHOOTING_COUNT : 0);
    }

    protected boolean isZeroShotMode() {
        return this.mIsZSLMode;
    }

    protected void prepareCapture() {
        setPictureFlipIfNeed(this.mParameters);
        setTimeWatermarkIfNeed();
    }

    protected boolean needSetupPreview(boolean zslMode) {
        return !this.mCameraDevice.isPreviewEnable();
    }

    protected boolean isSupportSceneMode() {
        return true;
    }

    protected void setAutoExposure(Parameters parameters, String value) {
        List<String> aeList = sProxy.getSupportedAutoexposure(parameters);
        if (aeList != null && aeList.contains(value)) {
            sProxy.setAutoExposure(parameters, value);
        }
    }

    public void onSettingValueChanged(String key) {
        if (this.mCameraDevice != null) {
            if ("pref_qc_camera_iso_key".equals(key)) {
                String iso = this.mPreferences.getString("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
                if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
                    Log.v("Camera", "ISO value = " + iso);
                    sProxy.setISOValue(this.mParameters, iso);
                }
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else {
                super.onSettingValueChanged(key);
            }
        }
    }

    protected boolean isLongShotMode() {
        return this.mIsLongShotMode;
    }

    private boolean getZSL() {
        if (getString(R.string.pref_face_beauty_close).equals(CameraSettings.getFaceBeautifyValue()) && (this.mMultiSnapStatus || this.mMutexModePicker.isNormal() || this.mMutexModePicker.isHdr())) {
            return sProxy.getZslSupported(this.mParameters);
        }
        return false;
    }

    protected boolean needAutoFocusBeforeCapture() {
        String flashMode = this.mParameters.getFlashMode();
        if ("auto".equals(flashMode) && this.mCameraDevice.isNeedFlashOn()) {
            return true;
        }
        return "on".equals(flashMode);
    }

    protected void updateCameraParametersPreference() {
        super.updateCameraParametersPreference();
        lcUpdateCameraParametersPreference();
    }

    private void setPictureFlipIfNeed(Parameters parameters) {
        if (isFrontMirror()) {
            sProxy.setPictureFlip(parameters, "1");
        } else {
            sProxy.setPictureFlip(parameters, "0");
        }
        Log.d("Camera", "Picture flip value = " + sProxy.getPictureFlip(parameters));
    }

    protected void cancelContinuousShot() {
        if (this.mIsLongShotMode) {
            this.mIsLongShotMode = false;
            applyMultiShutParameters(false);
            this.mCameraDevice.setParameters(this.mParameters);
        }
    }
}
