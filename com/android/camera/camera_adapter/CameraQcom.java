package com.android.camera.camera_adapter;

import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.os.SystemProperties;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.PictureSize;
import com.android.camera.PictureSizeManager;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.QcomCameraProxy;
import com.android.camera.module.BaseModule;
import com.android.camera.module.CameraModule;
import java.util.List;

public class CameraQcom extends CameraModule {
    private static QcomCameraProxy sProxy = ((QcomCameraProxy) CameraHardwareProxy.getDeviceProxy());
    private boolean mIsLongShotMode = false;

    private void qcomUpdateCameraParametersPreference() {
        boolean z;
        if (Device.isSupportedManualFunction()) {
            int exposure = Math.min(Integer.parseInt(getManualValue("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default))), sProxy.getMaxExposureTimeValue(this.mParameters));
            if (exposure >= 0) {
                sProxy.setExposureTime(this.mParameters, exposure);
                Log.v("Camera", "ExposureTime value=" + sProxy.getExposureTime(this.mParameters));
                if (Device.isFloatExposureTime()) {
                    exposure /= 1000;
                }
                if (exposure >= 1000) {
                    configOisParameters(this.mParameters, false);
                }
            }
        }
        if ((EffectController.getInstance().hasEffect() ? Device.isEffectWatermarkFilted() : false) || this.mMutexModePicker.isUbiFocus() || !CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            sProxy.setTimeWatermark(this.mParameters, "off");
        } else {
            sProxy.setTimeWatermark(this.mParameters, "on");
        }
        if ((EffectController.getInstance().hasEffect() ? Device.isEffectWatermarkFilted() : false) || this.mMutexModePicker.isUbiFocus() || !CameraSettings.isDualCameraWaterMarkOpen(this.mPreferences)) {
            sProxy.setDualCameraWatermark(this.mParameters, "off");
        } else {
            sProxy.setDualCameraWatermark(this.mParameters, "on");
        }
        if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            sProxy.setPortraitMode(this.mParameters, "on");
        } else {
            sProxy.setPortraitMode(this.mParameters, "off");
        }
        if (Device.isSupportedFaceInfoWaterMark()) {
            QcomCameraProxy qcomCameraProxy = sProxy;
            Parameters parameters = this.mParameters;
            if (this.mMutexModePicker.isUbiFocus()) {
                z = false;
            } else {
                z = CameraSettings.isFaceWaterMarkOpen(this.mPreferences);
            }
            qcomCameraProxy.setFaceWatermark(parameters, z);
        }
        setBeautyParams();
        if (Device.isSupportedIntelligentBeautify()) {
            String showGenderAndAge = this.mPreferences.getString("pref_camera_show_gender_age_key", getString(R.string.pref_camera_show_gender_age_default));
            getUIController().getFaceView().setShowGenderAndAge(showGenderAndAge);
            Log.i("Camera", "SetShowGenderAndAge =" + showGenderAndAge);
        }
        if (Device.isSupportedObjectTrack() || Device.isSupportedIntelligentBeautify()) {
            this.mParameters.set("xiaomi-preview-rotation", this.mOrientation == -1 ? 0 : this.mOrientation);
        }
        if (sProxy.getSupportedDenoiseModes(this.mParameters) != null) {
            String Denoise;
            if (Device.isSupportBurstDenoise() || !this.mMultiSnapStatus) {
                Denoise = "denoise-on";
            } else {
                Denoise = "denoise-off";
            }
            Log.v("Camera", "Denoise value = " + Denoise);
            sProxy.setDenoise(this.mParameters, Denoise);
        }
        if (this.mCameraState != 0) {
            String iso = getManualValue("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
            if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
                Log.v("Camera", "ISO value = " + iso);
                sProxy.setISOValue(this.mParameters, iso);
            }
        }
        int saturation = Math.min(Integer.parseInt(this.mPreferences.getString("pref_qc_camera_saturation_key", getString(CameraSettings.getDefaultPreferenceId(R.string.pref_camera_saturation_default)))), sProxy.getMaxSaturation(this.mParameters));
        if (saturation >= 0) {
            Log.v("Camera", "Saturation value = " + saturation);
            sProxy.setSaturation(this.mParameters, saturation);
        }
        int contrast = Math.min(Integer.parseInt(this.mPreferences.getString("pref_qc_camera_contrast_key", getString(R.string.pref_camera_contrast_default))), sProxy.getMaxContrast(this.mParameters));
        if (contrast >= 0) {
            Log.v("Camera", "Contrast value = " + contrast);
            sProxy.setContrast(this.mParameters, contrast);
        }
        int sharpness = Math.min(Integer.parseInt(this.mPreferences.getString("pref_qc_camera_sharpness_key", getString(R.string.pref_camera_sharpness_default))), sProxy.getMaxSharpness(this.mParameters));
        if (sharpness >= 0) {
            Log.v("Camera", "Sharpness value = " + sharpness);
            sProxy.setSharpness(this.mParameters, sharpness);
        }
        String touchAfAec = this.mPreferences.getString("pref_camera_touchafaec_key", getString(R.string.pref_camera_touchafaec_default));
        if (BaseModule.isSupported(touchAfAec, sProxy.getSupportedTouchAfAec(this.mParameters))) {
            Log.v("Camera", "TouchAfAec value = " + touchAfAec);
            sProxy.setTouchAfAec(this.mParameters, touchAfAec);
        }
        if (Device.isSupportedMagicMirror()) {
            sProxy.setBeautyRank(this.mParameters, CameraSettings.isSwitchOn("pref_camera_magic_mirror_key"));
        }
        setPictureFlipIfNeed();
        if (this.mFaceDetectionEnabled) {
            sProxy.setFaceDetectionMode(this.mParameters, "on");
        } else {
            sProxy.setFaceDetectionMode(this.mParameters, "off");
        }
        if (Device.isUsedMorphoLib()) {
            this.mParameters.set("ae-bracket-hdr", "Off");
        }
        sProxy.setHandNight(this.mParameters, false);
        sProxy.setMorphoHDR(this.mParameters, false);
        sProxy.setUbiFocus(this.mParameters, "af-bracket-off");
        sProxy.setAoHDR(this.mParameters, "off");
        sProxy.setHDR(this.mParameters, "false");
        sProxy.setNightShot(this.mParameters, "false");
        sProxy.setNightAntiMotion(this.mParameters, "false");
        if (!this.mMutexModePicker.isNormal()) {
            if (this.mMutexModePicker.isHandNight()) {
                if (isSceneMotion()) {
                    sProxy.setNightAntiMotion(this.mParameters, "true");
                } else {
                    sProxy.setNightShot(this.mParameters, "true");
                }
                Log.v("Camera", "Hand Nigh = true");
            } else if (this.mMutexModePicker.isRAW()) {
                Log.v("Camera", "Raw Data = true");
            } else if (this.mMutexModePicker.isAoHdr()) {
                sProxy.setAoHDR(this.mParameters, "on");
                Log.v("Camera", "AoHDR = true");
            } else if (this.mMutexModePicker.isMorphoHdr()) {
                sProxy.setHDR(this.mParameters, "true");
                Log.v("Camera", "Morpho HDR = true");
            } else if (this.mMutexModePicker.isUbiFocus()) {
                sProxy.setUbiFocus(this.mParameters, "af-bracket-on");
                Log.v("Camera", "Ubi Focus = true");
            }
        }
        String zsl = getZSL();
        Log.v("Camera", "ZSL value = " + zsl);
        if (zsl.equals("on")) {
            z = (!Device.shouldRestartPreviewAfterZslSwitch() || this.mIsZSLMode) ? false : this.mCameraState != 0;
            this.mRestartPreview = z;
            this.mIsZSLMode = true;
            sProxy.setZSLMode(this.mParameters, "on");
            sProxy.setCameraMode(this.mParameters, 1);
        } else if (zsl.equals("off")) {
            z = (Device.shouldRestartPreviewAfterZslSwitch() && this.mIsZSLMode) ? this.mCameraState != 0 : false;
            this.mRestartPreview = z;
            this.mIsZSLMode = false;
            sProxy.setZSLMode(this.mParameters, "off");
            sProxy.setCameraMode(this.mParameters, 0);
        }
        if (this.mIsZSLMode && this.mMultiSnapStatus && !this.mIsLongShotMode) {
            this.mIsLongShotMode = true;
            if (Device.IS_MI2 || Device.IS_MI2A) {
                this.mParameters.set("num-snaps-per-shutter", BURST_SHOOTING_COUNT);
            } else {
                this.mCameraDevice.setLongshotMode(true);
            }
            setTimeWatermarkIfNeed();
        } else if (this.mIsLongShotMode) {
            this.mIsLongShotMode = false;
            if (Device.IS_MI2 || Device.IS_MI2A) {
                this.mParameters.set("num-snaps-per-shutter", 1);
            } else {
                this.mCameraDevice.setLongshotMode(false);
            }
        }
        Log.v("Camera", "Long Shot mode value = " + isLongShotMode());
        Parameters parameters2 = this.mParameters;
        if (this.mIsLongShotMode) {
            z = "torch".equals(this.mParameters.getFlashMode());
        } else {
            z = false;
        }
        parameters2.setAutoWhiteBalanceLock(z);
        if (Device.isSupportedChromaFlash()) {
            String chromaFlash;
            if (couldEnableChromaFlash() && this.mPreferences.getBoolean("pref_auto_chroma_flash_key", getResources().getBoolean(CameraSettings.getDefaultPreferenceId(R.bool.pref_camera_auto_chroma_flash_default)))) {
                chromaFlash = "chroma-flash-on";
            } else {
                chromaFlash = "chroma-flash-off";
            }
            sProxy.setChromaFlash(this.mParameters, chromaFlash);
        }
        Log.v("Camera", "Chroma Flash = " + sProxy.getChromaFlash(this.mParameters));
        if (isBackCamera() && CameraSettings.isSupportedMetadata()) {
            int metaType = 0;
            if (CameraSettings.isSupportedPortrait() && CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                metaType = 5;
            }
            if (Device.isSupportedASD()) {
                boolean asdEnable = (getUIController().getSettingPage().isItemSelected() || this.mIsLongShotMode) ? false : metaType == 0;
                Log.v("Camera", "ASD Enable = " + asdEnable);
                this.mParameters.set("scene-detect", asdEnable ? "on" : "off");
                if (asdEnable) {
                    metaType = 3;
                }
            }
            setMetaCallback(metaType);
        }
    }

    protected void cancelContinuousShot() {
        if (!this.mMultiSnapStatus && this.mIsLongShotMode) {
            this.mIsLongShotMode = false;
            this.mCameraDevice.setLongshotMode(false);
            Log.v("Camera", "Long Shot mode value = " + isLongShotMode());
        }
    }

    protected void setManualParameters() {
        sProxy.setFocusMode(this.mParameters, this.mFocusManager.getFocusMode());
        int exposure = Math.min(Integer.parseInt(getManualValue("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default))), sProxy.getMaxExposureTimeValue(this.mParameters));
        if (exposure >= 0) {
            sProxy.setExposureTime(this.mParameters, exposure);
            Log.v("Camera", "ExposureTime value=" + sProxy.getExposureTime(this.mParameters));
        }
        String iso = getManualValue("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
        if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
            Log.v("Camera", "ISO value = " + iso);
            sProxy.setISOValue(this.mParameters, iso);
        }
    }

    private boolean couldEnableChromaFlash() {
        if (this.mMultiSnapStatus || "af-bracket-on".equals(sProxy.getUbiFocus(this.mParameters)) || !isDefaultPreference("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default))) {
            return false;
        }
        return isDefaultPreference("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default));
    }

    protected void updateCameraParametersInitialize() {
        super.updateCameraParametersInitialize();
        int[] fpsRange = CameraSettings.getPhotoPreviewFpsRange(this.mParameters);
        if ((Device.IS_MI4 || Device.IS_X5) && fpsRange != null && fpsRange.length > 0) {
            this.mParameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        }
    }

    public void onSettingValueChanged(String key) {
        if (this.mCameraDevice != null) {
            if ("pref_focus_position_key".equals(key)) {
                sProxy.setFocusPosition(this.mParameters, CameraSettings.getFocusPosition());
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else if ("pref_qc_manual_whitebalance_k_value_key".equals(key)) {
                sProxy.setWBManualCCT(this.mParameters, CameraSettings.getKValue());
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else {
                super.onSettingValueChanged(key);
            }
        }
    }

    protected boolean isSupportSceneMode() {
        return Device.IS_HONGMI;
    }

    protected boolean needSetupPreview(boolean zslMode) {
        if (zslMode) {
            return false;
        }
        if (SystemProperties.getBoolean("persist.camera.feature.restart", false) && sProxy.getInternalPreviewSupported(this.mParameters) && "jpeg".equalsIgnoreCase(this.mParameters.get("picture-format"))) {
            return false;
        }
        return true;
    }

    protected boolean isZeroShotMode() {
        return this.mIsZSLMode;
    }

    protected void setAutoExposure(Parameters parameters, String value) {
        List<String> aeList = sProxy.getSupportedAutoexposure(parameters);
        if (aeList != null && aeList.contains(value)) {
            sProxy.setAutoExposure(parameters, value);
        }
    }

    protected boolean isLongShotMode() {
        return this.mIsLongShotMode;
    }

    protected void updateCameraParametersPreference() {
        super.updateCameraParametersPreference();
        qcomUpdateCameraParametersPreference();
    }

    protected boolean needAutoFocusBeforeCapture() {
        String flashMode = this.mParameters.getFlashMode();
        if ("on".equals(flashMode)) {
            return true;
        }
        if ("auto".equals(flashMode)) {
            return this.mCameraDevice.isNeedFlashOn();
        }
        return false;
    }

    private void setPictureFlipIfNeed() {
        if (!isFrontMirror()) {
            sProxy.setPictureFlip(this.mParameters, "off");
        } else if (this.mOrientation == -1 || this.mOrientation % 180 == 0) {
            sProxy.setPictureFlip(this.mParameters, "flip-v");
        } else {
            sProxy.setPictureFlip(this.mParameters, "flip-h");
        }
        Log.d("Camera", "Picture flip value = " + sProxy.getPictureFlip(this.mParameters));
    }

    protected void prepareCapture() {
        setPictureFlipIfNeed();
        if (Device.IS_H2XLTE && this.mMutexModePicker.isHdr()) {
            this.mParameters.setAutoExposureLock(true);
            this.mParameters.setAutoWhiteBalanceLock(true);
        }
        if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            setBeautyParams();
        }
        setTimeWatermarkIfNeed();
    }

    protected void onCameraStartPreview() {
        if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key") && CameraSettings.isDualCameraHintShown(this.mPreferences)) {
            this.mHandler.sendEmptyMessage(40);
        }
    }

    protected PictureSize getBestPictureSize() {
        List<Size> sizes;
        if (CameraSettings.isSupportedPortrait() && CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            sizes = sProxy.getSupportedPortraitPictureSizes(this.mCameraDevice, this.mParameters);
        } else {
            sizes = this.mParameters.getSupportedPictureSizes();
        }
        PictureSizeManager.initialize(getActivity(), sizes, getMaxPictureSize());
        return PictureSizeManager.getBestPictureSize();
    }

    private String getZSL() {
        if (this.mMultiSnapStatus) {
            return "on";
        }
        if (this.mMutexModePicker.isRAW()) {
            return "off";
        }
        if (Device.IS_XIAOMI && !Device.IS_MI2A && !isDefaultManualExposure()) {
            return "off";
        }
        if ((Device.isUsedMorphoLib() || this.mMutexModePicker.isNormal() || (this.mMutexModePicker.isSceneHdr() && sProxy.isZSLHDRSupported(this.mParameters))) && (!Device.IS_HM3LTE || !isFrontCamera())) {
            return "on";
        }
        return "off";
    }

    protected int getBurstDelayTime() {
        if (Device.IS_HONGMI) {
            return 300;
        }
        return 200;
    }

    public void onResumeAfterSuper() {
        super.onResumeAfterSuper();
        this.mActivity.getSensorStateManager().setEdgeTouchEnabled(CameraSettings.isEdgePhotoEnable());
    }

    protected void setBeautyParams() {
        if (!Device.IS_MI2 || Device.IS_MI2A) {
            super.setBeautyParams();
            return;
        }
        String beauty = CameraSettings.getFaceBeautifyValue();
        if (CameraSettings.isFaceBeautyOn(beauty)) {
            try {
                beauty = String.valueOf((((Integer.parseInt(beauty) | (Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_color_key")) << 28)) | (Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_slim_face_key")) << 24)) | (Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_smooth_key")) << 16)) | (Integer.parseInt(CameraSettings.getBeautifyDetailValue("pref_skin_beautify_enlarge_eye_key")) << 20));
            } catch (NumberFormatException e) {
                Log.e("Camera", "check beautify detail values in strings.xml of aries");
            }
        }
        sProxy.setStillBeautify(this.mParameters, beauty);
        Log.i("Camera", "SetStillBeautify =" + sProxy.getStillBeautify(this.mParameters));
    }
}
