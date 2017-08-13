package com.android.camera.camera_adapter;

import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.NvidiaCameraProxy;
import com.android.camera.module.CameraModule;

public class CameraNv extends CameraModule {
    private static int RAW_META_DATA = 1000000;
    private static NvidiaCameraProxy sProxy = ((NvidiaCameraProxy) CameraHardwareProxy.getDeviceProxy());
    private final String TAG = "CameraNv";
    private int mNSLBurstCount;
    private boolean mPreviewPausedDisabled;
    private byte[] mRawBuffer;
    private int mRawBufferSize = 0;
    private boolean mSetAohdrLater;
    private boolean mSkipSetNSLAfterMultiShot;

    private void updateNvCameraParametersPreference() {
        int saturation = Integer.parseInt(this.mPreferences.getString("pref_qc_camera_saturation_key", getString(R.string.pref_camera_saturation_default)));
        if (saturation >= -100 && saturation <= 100) {
            sProxy.setSaturation(this.mParameters, saturation);
        }
        Log.i("CameraNv", "Saturation = " + saturation);
        String contrastStr = this.mPreferences.getString("pref_qc_camera_contrast_key", getString(R.string.pref_camera_contrast_default));
        sProxy.setContrast(this.mParameters, contrastStr);
        Log.i("CameraNv", "Contrast = " + contrastStr);
        int sharpness = Integer.parseInt(this.mPreferences.getString("pref_qc_camera_sharpness_key", getString(R.string.pref_camera_sharpness_default)));
        if (sharpness >= -100 && sharpness <= 100) {
            sProxy.setEdgeEnhancement(this.mParameters, sharpness);
        }
        Log.i("CameraNv", "Sharpness = " + sharpness);
        if (!sProxy.getAutoRotation(this.mParameters)) {
            sProxy.setAutoRotation(this.mParameters, true);
        }
        String iso = getManualValue("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
        sProxy.setISOValue(this.mParameters, iso);
        Log.i("CameraNv", "PictureISO = " + iso);
        String exposureTime = getManualValue("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default));
        sProxy.setExposureTime(this.mParameters, Integer.parseInt(exposureTime));
        Log.i("CameraNv", "ExposureTime = " + exposureTime);
        this.mSkipSetNSLAfterMultiShot = false;
        this.mSetAohdrLater = false;
        if (this.mMutexModePicker.isNormal()) {
            if (this.mRawBuffer != null) {
                this.mRawBuffer = null;
                this.mRawBufferSize = 0;
                this.mCameraDevice.addRawImageCallbackBuffer(null);
            }
            sProxy.setHandNight(this.mParameters, false);
            sProxy.setRawDumpFlag(this.mParameters, 0);
            if (sProxy.getAohdrEnable(this.mParameters)) {
                sProxy.setAohdrEnable(this.mParameters, false);
                this.mCameraDevice.setParameters(this.mParameters);
                this.mParameters = this.mCameraDevice.getParameters();
            }
            sProxy.setMorphoHDR(this.mParameters, false);
        } else if (this.mMutexModePicker.isHandNight()) {
            sProxy.setHandNight(this.mParameters, true);
            Log.i("CameraNv", "Hand Nigh = true");
        } else if (this.mMutexModePicker.isRAW()) {
            sProxy.setRawDumpFlag(this.mParameters, 13);
            Log.i("CameraNv", "Raw Data = true");
            allocRawBufferIfNeeded();
        } else if (this.mMutexModePicker.isAoHdr()) {
            if (!sProxy.getAohdrEnable(this.mParameters)) {
                this.mSetAohdrLater = true;
                Log.i("CameraNv", "AO HDR = true");
            }
        } else if (this.mMutexModePicker.isMorphoHdr()) {
            sProxy.setMorphoHDR(this.mParameters, true);
            Log.i("CameraNv", "Morpho HDR = true");
        }
        if (this.mMultiSnapStopRequest) {
            this.mSkipSetNSLAfterMultiShot = true;
        }
        this.mNSLBurstCount = sProxy.getNSLNumBuffers(this.mParameters);
        int NSLBuffersNeeded = getNSLBuffersNeededCount();
        if (!(this.mSkipSetNSLAfterMultiShot || this.mNSLBurstCount == NSLBuffersNeeded)) {
            sProxy.setNSLNumBuffers(this.mParameters, NSLBuffersNeeded);
            if (NSLBuffersNeeded == 0) {
                sProxy.setNSLBurstCount(this.mParameters, 0);
                sProxy.setBurstCount(this.mParameters, 1);
                sProxy.setNVShotMode(this.mParameters, "normal");
            }
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
            this.mNSLBurstCount = sProxy.getNSLNumBuffers(this.mParameters);
            Log.i("CameraNv", "Allocate NSLNumBuffers = " + this.mNSLBurstCount);
        }
        if (this.mMultiSnapStatus) {
            if (this.mNSLBurstCount <= 0 || NSLBuffersNeeded <= 0) {
                sProxy.setNVShotMode(this.mParameters, "normal");
            } else {
                sProxy.setNVShotMode(this.mParameters, "shot2shot");
            }
            sProxy.setNSLBurstCount(this.mParameters, 0);
            sProxy.setBurstCount(this.mParameters, BURST_SHOOTING_COUNT);
        } else {
            if (this.mSkipSetNSLAfterMultiShot || this.mNSLBurstCount <= 0 || NSLBuffersNeeded <= 0 || !this.mMutexModePicker.isNormal()) {
                sProxy.setNSLBurstCount(this.mParameters, 0);
                sProxy.setBurstCount(this.mParameters, 1);
            } else {
                sProxy.setNSLBurstCount(this.mParameters, 1);
                sProxy.setBurstCount(this.mParameters, 0);
            }
            sProxy.setNVShotMode(this.mParameters, "normal");
        }
        if (this.mSetAohdrLater) {
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
            if (!"off".equals(this.mParameters.getFlashMode())) {
                this.mParameters.setFlashMode("off");
                this.mCameraDevice.setParameters(this.mParameters);
                this.mParameters = this.mCameraDevice.getParameters();
            } else if (sProxy.getNSLNumBuffers(this.mParameters) != 0) {
                sProxy.setNSLNumBuffers(this.mParameters, 0);
                sProxy.setNSLBurstCount(this.mParameters, 0);
                this.mCameraDevice.setParameters(this.mParameters);
                this.mParameters = this.mCameraDevice.getParameters();
            }
            sProxy.setAohdrEnable(this.mParameters, true);
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
        }
        sProxy.setPreviewPauseDisabled(this.mParameters, getPreviewPausedDisabled());
        Log.d("CameraNv", "preview disabled = " + sProxy.getPreviewPauseDisabled(this.mParameters));
        if ((EffectController.getInstance().hasEffect() ? Device.isEffectWatermarkFilted() : false) || !CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            sProxy.setTimeWatermark(this.mParameters, "off");
        } else {
            sProxy.setTimeWatermark(this.mParameters, "on");
        }
        Log.i("CameraNv", "SetTimeWatermark =" + sProxy.getTimeWatermark(this.mParameters));
        setBeautyParams();
        String showGenderAndAge = this.mPreferences.getString("pref_camera_show_gender_age_key", getString(R.string.pref_camera_show_gender_age_default));
        getUIController().getFaceView().setShowGenderAndAge(showGenderAndAge);
        Log.i("CameraNv", "SetShowGenderAndAge =" + showGenderAndAge);
        sProxy.setMultiFaceBeautify(this.mParameters, "on");
        Log.i("CameraNv", "SetMultiFaceBeautify =on");
    }

    protected void prepareCapture() {
        if (isFrontMirror()) {
            sProxy.setFlipStill(this.mParameters, "horizontal");
        } else {
            sProxy.setFlipStill(this.mParameters, "off");
        }
        Log.i("CameraNv", "Set JPEG horizontal flip = " + sProxy.isFrontMirror(this.mParameters));
    }

    protected void updateCameraParametersPreference() {
        super.updateCameraParametersPreference();
        updateNvCameraParametersPreference();
    }

    public void onSettingValueChanged(String key) {
        if (this.mCameraDevice != null) {
            if ("pref_focus_position_key".equals(key)) {
                sProxy.setFocusPosition(this.mParameters, CameraSettings.getFocusPosition());
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else if ("pref_qc_manual_whitebalance_k_value_key".equals(key)) {
                sProxy.setColorTemperature(this.mParameters, CameraSettings.getKValue());
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else {
                super.onSettingValueChanged(key);
            }
        }
    }

    protected boolean isZeroShotMode() {
        return this.mNSLBurstCount != 0;
    }

    protected boolean needAutoFocusBeforeCapture() {
        String flashMode = this.mParameters.getFlashMode();
        if ("auto".equals(flashMode) && this.mCameraDevice.isNeedFlashOn()) {
            return true;
        }
        return "on".equals(flashMode);
    }

    protected boolean needSwitchZeroShotMode() {
        String flashMode = getRequestFlashMode();
        if (this.mSkipSetNSLAfterMultiShot) {
            return true;
        }
        if (this.mNSLBurstCount <= 0) {
            return false;
        }
        if ("auto".equals(flashMode) && this.mCameraDevice.isNeedFlashOn()) {
            return true;
        }
        return "on".equals(flashMode);
    }

    protected boolean needSetupPreview(boolean zslMode) {
        return this.mPreviewPausedDisabled ? this.mMultiSnapStopRequest : true;
    }

    private void allocRawBufferIfNeeded() {
        int sizeWanted = RAW_META_DATA + 26257920;
        if (this.mRawBuffer == null || this.mRawBufferSize < sizeWanted) {
            try {
                this.mRawBuffer = new byte[sizeWanted];
                this.mRawBufferSize = sizeWanted;
            } catch (OutOfMemoryError e) {
                this.mRawBuffer = null;
                this.mRawBufferSize = 0;
                Log.v("CameraNv", "Raw OutOfMemoryError: " + e.getMessage());
            }
        }
    }

    private String getZSL() {
        return "on";
    }

    private boolean getPreviewPausedDisabled() {
        this.mPreviewPausedDisabled = true;
        if (this.mMutexModePicker.isNormal() && sProxy.getNvExposureTime(this.mParameters) == 0 && sProxy.getISOValue(this.mParameters).equals(getString(R.string.pref_camera_iso_value_auto))) {
            if (this.mIsImageCaptureIntent) {
            }
            return this.mPreviewPausedDisabled;
        }
        this.mPreviewPausedDisabled = false;
        Log.v("CameraNv", "getPreviewPausedDisabled " + this.mPreviewPausedDisabled + " " + this.mMutexModePicker.isNormal() + " " + sProxy.getNvExposureTime(this.mParameters) + " " + sProxy.getISOValue(this.mParameters) + " " + this.mIsImageCaptureIntent);
        return this.mPreviewPausedDisabled;
    }

    private int getNSLBuffersNeededCount() {
        String flashMode = this.mParameters.getFlashMode();
        if (this.mMultiSnapStatus) {
            return 4;
        }
        return (!getZSL().equals(getString(R.string.pref_camera_zsl_value_off)) && this.mMutexModePicker.isNormal() && !"on".equals(flashMode) && sProxy.getNvExposureTime(this.mParameters) == 0 && sProxy.getISOValue(this.mParameters).equals(getString(R.string.pref_camera_iso_value_auto)) && !("auto".equals(flashMode) && this.mCameraDevice.isNeedFlashOn())) ? 4 : 0;
    }

    protected boolean isLongShotMode() {
        return this.mMultiSnapStatus;
    }

    public void onPauseBeforeSuper() {
        if (this.mMutexModePicker.isAoHdr()) {
            this.mMutexModePicker.resetMutexMode();
        }
        super.onPauseBeforeSuper();
    }
}
