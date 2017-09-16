package com.android.camera.module;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.RectF;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import com.android.camera.AutoLockManager;
import com.android.camera.Camera;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraDisabledException;
import com.android.camera.CameraErrorCallback;
import com.android.camera.CameraHardwareException;
import com.android.camera.CameraHolder;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraManager.HardwareErrorListener;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.FocusManagerAbstract;
import com.android.camera.MutexModeManager;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.SettingsOverrider;
import com.android.camera.ui.FocusView.ExposureViewListener;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.UIController;
import com.android.camera.ui.V6GestureRecognizer;
import com.android.camera.ui.V6ModulePicker;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public abstract class BaseModule implements Module, ExposureViewListener, HardwareErrorListener {
    protected static CameraHardwareProxy sProxy = CameraHardwareProxy.getDeviceProxy();
    protected Camera mActivity;
    protected CameraProxy mCameraDevice;
    protected boolean mCameraDisabled;
    protected int mCameraDisplayOrientation;
    protected boolean mCameraHardwareError;
    protected int mCameraId;
    protected ContentResolver mContentResolver;
    protected float mDeviceRotation = -1.0f;
    protected int mDisplayRotation;
    protected CameraErrorCallback mErrorCallback;
    protected float mExposureCompensationStep;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    if (BaseModule.this.mDisplayRotation != 180) {
                        BaseModule.this.mActivity.getScreenHint().showFrontCameraFirstUseHintPopup();
                        CameraSettings.cancelFrontCameraFirstUseHint(BaseModule.this.mPreferences);
                        return;
                    }
                    return;
                case 1:
                    BaseModule.this.mActivity.getScreenHint().dismissFrontCameraFirstUseHintPopup();
                    return;
                default:
                    return;
            }
        }
    };
    protected boolean mHasPendingSwitching;
    protected boolean mIgnoreFocusChanged;
    private boolean mIgnoreTouchEvent;
    protected boolean mKeepAdjustedEv;
    protected long mMainThreadId;
    protected int mMaxExposureCompensation;
    protected int mMinExposureCompensation;
    protected MutexModeManager mMutexModePicker;
    protected int mNumberOfCameras;
    protected boolean mObjectTrackingStarted;
    protected boolean mOpenCameraFail;
    protected int mOrientation = -1;
    protected int mOrientationCompensation = 0;
    protected Parameters mParameters;
    protected boolean mPaused;
    protected int mPendingSwitchCameraId = -1;
    protected CameraSettingPreferences mPreferences;
    private boolean mRestoring;
    protected SettingsOverrider mSettingsOverrider;
    protected boolean mSwitchingCamera;
    protected int mUIStyle = -1;
    protected boolean mWaitForRelease;
    protected int mZoomMax;
    protected int mZoomMaxRatio;
    private float mZoomScaled;

    public enum CameraMode {
        Normal(0),
        ImageCapture(2),
        VideoCapture(4),
        ScanQRCode(6);
        
        public int value;

        private CameraMode(int value) {
            this.value = value;
        }
    }

    protected class CameraOpenThread extends Thread {
        public void run() {
            BaseModule.this.openCamera();
        }
    }

    protected void openCamera() {
        try {
            prepareOpenCamera();
            this.mCameraDevice = Util.openCamera(this.mActivity, this.mCameraId);
            this.mCameraDevice.setHardwareListener(this);
            if (this.mCameraDevice != null) {
                this.mParameters = this.mCameraDevice.getParameters();
            }
        } catch (CameraHardwareException e) {
            this.mOpenCameraFail = true;
        } catch (CameraDisabledException e2) {
            this.mCameraDisabled = true;
        }
    }

    public Camera getActivity() {
        return this.mActivity;
    }

    public void onCreate(Camera activity) {
        this.mActivity = activity;
        this.mMainThreadId = Thread.currentThread().getId();
        this.mContentResolver = activity.getContentResolver();
        this.mNumberOfCameras = CameraHolder.instance().getNumberOfCameras();
        this.mErrorCallback = new CameraErrorCallback(this.mActivity);
        this.mPreferences = CameraSettingPreferences.instance();
        this.mSettingsOverrider = new SettingsOverrider();
        initializeMutexMode();
    }

    public void onNewIntent() {
    }

    public UIController getUIController() {
        return this.mActivity.getUIController();
    }

    public void onDestroy() {
        getUIController().getEffectCropView().onDestory();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return false;
    }

    protected void openSettingActivity() {
    }

    public boolean handleMessage(int what, int sender, Object extra1, Object extra2) {
        switch (sender) {
            case R.id.zoom_button:
                if (what == 7 && !this.mPaused && isCameraEnabled() && CameraSettings.isSupportedOpticalZoom() && CameraSettings.isSwitchCameraZoomMode()) {
                    onCameraPickerClicked(CameraHolder.instance().getBackCameraId());
                    return true;
                }
            case R.id.v6_focus_view:
                if (what == 1 && !this.mPaused && isCameraEnabled()) {
                    int value = ((Integer) extra1).intValue();
                    int state = ((Integer) extra2).intValue();
                    if (state == 2) {
                        this.mParameters.setAutoWhiteBalanceLock(false);
                    } else if (state == 1) {
                        this.mParameters.setExposureCompensation(value);
                        this.mParameters.setAutoWhiteBalanceLock(true);
                    }
                    this.mCameraDevice.setParametersAsync(this.mParameters);
                    if (state == 1) {
                        CameraSettings.writeExposure(this.mPreferences, value);
                        updateStatusBar("pref_camera_exposure_key");
                    }
                    if (this.mKeepAdjustedEv) {
                        CameraDataAnalytics.instance().trackEvent("ev_adjust_recom_times_key");
                        this.mKeepAdjustedEv = false;
                    }
                    Log.d("Camera", "EV = : " + value);
                    return true;
                }
            case R.id.zoom_popup:
                if (what == 7 && !this.mPaused && isCameraEnabled()) {
                    onZoomValueChanged(Util.binarySearchRightMost(this.mParameters.getZoomRatios(), Integer.valueOf(((Integer) extra2).intValue())), ((Boolean) extra1).booleanValue());
                    return true;
                }
        }
        return false;
    }

    public void onResumeBeforeSuper() {
        this.mPaused = false;
    }

    public void onResumeAfterSuper() {
        if (this.mActivity.getScreenHint() != null) {
            this.mActivity.getScreenHint().updateHint();
        }
        AutoLockManager.getInstance(this.mActivity).onResume();
    }

    public void onPauseBeforeSuper() {
        this.mPaused = true;
    }

    public void onPauseAfterSuper() {
        AutoLockManager.getInstance(this.mActivity).onPause();
        if (this.mActivity.getScreenHint() != null) {
            this.mActivity.getScreenHint().cancelHint();
        }
        CameraSettings.resetZoom(this.mPreferences);
        CameraSettings.resetCameraZoomMode();
        setZoomValue(0);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus) {
            this.mIgnoreFocusChanged = false;
        }
    }

    public boolean onBackPressed() {
        return false;
    }

    public boolean isVideoRecording() {
        return false;
    }

    protected boolean currentIsMainThread() {
        return this.mMainThreadId == Thread.currentThread().getId();
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == 82 && !this.mActivity.startFromSecureKeyguard() && getUIController().getSettingButton().isEnabled()) {
            openSettingActivity();
        }
        return false;
    }

    public void ignoreTouchEvent(boolean ignore) {
        this.mIgnoreTouchEvent = ignore;
    }

    public boolean IsIgnoreTouchEvent() {
        return this.mIgnoreTouchEvent;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return V6GestureRecognizer.getInstance(this.mActivity).onTouchEvent(event);
    }

    public List<String> getSupportedSettingKeys() {
        return null;
    }

    public void onSwitchAnimationDone() {
    }

    public boolean onScaleBegin(float focusX, float focusY) {
        this.mZoomScaled = Float.MIN_VALUE;
        return true;
    }

    public boolean onScale(float focusX, float focusY, float scale) {
        if (isZoomEnabled()) {
            this.mZoomScaled += scale - 1.0f;
            if (scaleZoomValue(this.mZoomScaled)) {
                this.mZoomScaled = Float.MIN_VALUE;
            }
            getUIController().getZoomButton().dismissPopup();
        }
        return true;
    }

    public void onScaleEnd() {
    }

    protected boolean isZoomEnabled() {
        return true;
    }

    public boolean scaleZoomValue(float contrast) {
        int value = getZoomValue() + ((int) (((float) this.mZoomMax) * contrast));
        if (getZoomValue() == value) {
            return false;
        }
        if (value < 0) {
            value = 0;
        } else if (value > this.mZoomMax) {
            value = this.mZoomMax;
        }
        onZoomValueChanged(value);
        return true;
    }

    protected void onCameraException() {
        if (currentIsMainThread()) {
            if (this.mOpenCameraFail || this.mCameraHardwareError) {
                if (this.mOpenCameraFail) {
                    CameraDataAnalytics.instance().trackEvent("open_camera_fail_key");
                }
                if ((!this.mActivity.isPaused() || this.mOpenCameraFail) && this.mActivity.couldShowErrorDialog()) {
                    int i;
                    Activity activity = this.mActivity;
                    if (Util.isInVideoCall(this.mActivity)) {
                        i = R.string.cannot_connect_camera_volte_call;
                    } else if (CameraSettings.updateOpenCameraFailTimes() > 1) {
                        i = R.string.cannot_connect_camera_twice;
                    } else {
                        i = R.string.cannot_connect_camera_once;
                    }
                    Util.showErrorAndFinish(activity, i);
                    this.mActivity.showErrorDialog();
                }
            }
            if (this.mCameraDisabled && this.mActivity.couldShowErrorDialog()) {
                Util.showErrorAndFinish(this.mActivity, R.string.camera_disabled);
                this.mActivity.showErrorDialog();
                return;
            }
            return;
        }
        sendOpenFailMessage();
    }

    protected void sendOpenFailMessage() {
    }

    protected boolean hasCameraException() {
        return (this.mCameraDisabled || this.mOpenCameraFail) ? true : this.mCameraHardwareError;
    }

    public void requestRender() {
    }

    public void onLongPress(int x, int y) {
    }

    public void onPreviewTextureCopied() {
    }

    public void onPreviewPixelsRead(byte[] pixels, int width, int height) {
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
    }

    public void onOrientationChanged(int orientation) {
        if (orientation != -1) {
            this.mOrientation = Util.roundOrientation(orientation, this.mOrientation);
            EffectController.getInstance().setOrientation(Util.getShootOrientation(this.mActivity, this.mOrientation));
            checkActivityOrientation();
            int orientationCompensation = (this.mOrientation + this.mDisplayRotation) % 360;
            if (this.mOrientationCompensation != orientationCompensation) {
                this.mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(this.mOrientationCompensation, true);
            }
            if (this.mDisplayRotation == 180 && isFrontCamera() && this.mActivity.getScreenHint().isShowingFrontCameraFirstUseHintPopup()) {
                this.mHandler.sendEmptyMessageDelayed(1, 1000);
            }
        }
    }

    public void checkActivityOrientation() {
        if (this.mDisplayRotation != Util.getDisplayRotation(this.mActivity)) {
            setDisplayOrientation();
        }
    }

    protected void setDisplayOrientation() {
        this.mDisplayRotation = Util.getDisplayRotation(this.mActivity);
        this.mCameraDisplayOrientation = Util.getDisplayOrientation(this.mDisplayRotation, this.mCameraId);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setDisplayOrientation(this.mCameraDisplayOrientation);
        }
    }

    protected void setOrientationIndicator(int orientation, boolean animation) {
        int i = 0;
        Rotatable[] indicators = new Rotatable[]{getUIController(), this.mActivity.getCameraScreenNail()};
        int length = indicators.length;
        while (i < length) {
            Rotatable indicator = indicators[i];
            if (indicator != null) {
                indicator.setOrientation(orientation, animation);
            }
            i++;
        }
    }

    public void onUserInteraction() {
    }

    public void onStop() {
    }

    public void onSettingValueChanged(String key) {
    }

    public boolean onCameraPickerClicked(int cameraId) {
        return false;
    }

    public void transferOrientationCompensation(Module lastModule) {
        this.mOrientation = ((BaseModule) lastModule).mOrientation;
        this.mOrientationCompensation = ((BaseModule) lastModule).mOrientationCompensation;
    }

    public String getString(int resId) {
        return this.mActivity.getString(resId);
    }

    public Resources getResources() {
        return this.mActivity.getResources();
    }

    public Window getWindow() {
        return this.mActivity.getWindow();
    }

    protected boolean handleVolumeKeyEvent(boolean up, boolean pressed, int repeatCount) {
        String value = this.mPreferences.getString("pref_camera_volumekey_function_key", this.mActivity.getString(R.string.pref_camera_volumekey_function_default));
        if (this.mCameraDevice == null || this.mParameters == null || !isCameraEnabled()) {
            return true;
        }
        if (value.equals(this.mActivity.getString(R.string.pref_camera_volumekey_function_entryvalue_shutter))) {
            performVolumeKeyClicked(repeatCount, pressed);
            return true;
        } else if (V6ModulePicker.isPanoramaModule() || !value.equals(this.mActivity.getString(R.string.pref_camera_volumekey_function_entryvalue_zoom)) || !this.mParameters.isZoomSupported() || !isZoomEnabled() || !pressed) {
            return false;
        } else {
            if (repeatCount == 0) {
                CameraDataAnalytics.instance().trackEvent("zoom_volume_times");
            }
            if (up) {
                addZoom(1);
            } else {
                addZoom(-1);
            }
            return true;
        }
    }

    protected void initializeZoom() {
        if (this.mParameters.isZoomSupported()) {
            this.mZoomMax = this.mParameters.getMaxZoom();
            this.mZoomMaxRatio = ((Integer) this.mParameters.getZoomRatios().get(this.mZoomMax)).intValue();
            this.mActivity.getCameraScreenNail().setOrientation(this.mOrientationCompensation, false);
            setZoomValue(this.mParameters.getZoom());
        }
    }

    protected void initializeExposureCompensation() {
        this.mMaxExposureCompensation = this.mParameters.getMaxExposureCompensation();
        this.mMinExposureCompensation = this.mParameters.getMinExposureCompensation();
        this.mExposureCompensationStep = this.mParameters.getExposureCompensationStep();
    }

    protected void addZoom(int add) {
        int value = getZoomValue() + add;
        if (value < 0) {
            value = 0;
        } else if (value > this.mZoomMax) {
            value = this.mZoomMax;
        }
        onZoomValueChanged(value);
    }

    protected void resetCameraSettingsIfNeed() {
        if (this.mActivity.getCameraAppImpl().isNeedRestore()) {
            this.mActivity.getCameraAppImpl().resetRestoreFlag();
            Iterator<CameraMode> iterator = getCameraModeList().iterator();
            while (iterator.hasNext()) {
                CameraMode cameraMode = (CameraMode) iterator.next();
                this.mCameraId = 0;
                this.mPreferences.setLocalId(getPreferencesLocalId(this.mCameraId, cameraMode));
                CameraSettings.resetSettingsNoNeedToSave(this.mPreferences, this.mCameraId);
                this.mCameraId = 1;
                this.mPreferences.setLocalId(getPreferencesLocalId(this.mCameraId, cameraMode));
                CameraSettings.resetSettingsNoNeedToSave(this.mPreferences, this.mCameraId);
            }
            return;
        }
        CameraSettings.resetPreference("pref_camera_panoramamode_key");
        CameraSettings.resetPreference("pref_camera_portrait_mode_key");
    }

    public void enableCameraControls(boolean enable) {
        getUIController().enableControls(enable);
        ignoreTouchEvent(!enable);
    }

    protected void exitMutexMode() {
    }

    protected void enterMutexMode() {
    }

    protected void initializeMutexMode() {
        if (this.mMutexModePicker == null) {
            HashMap<String, HashMap<String, Runnable>> map = new HashMap();
            Runnable enterHDR = new Runnable() {
                public void run() {
                    BaseModule.this.enterMutexMode();
                }
            };
            Runnable exitHDR = new Runnable() {
                public void run() {
                    BaseModule.this.exitMutexMode();
                }
            };
            HashMap<String, Runnable> HDRRunnable = new HashMap();
            HDRRunnable.put("enter", enterHDR);
            HDRRunnable.put("exit", exitHDR);
            map.put(MutexModeManager.getMutexModeName(1), HDRRunnable);
            map.put(MutexModeManager.getMutexModeName(2), HDRRunnable);
            map.put(MutexModeManager.getMutexModeName(5), HDRRunnable);
            map.put(MutexModeManager.getMutexModeName(3), HDRRunnable);
            map.put(MutexModeManager.getMutexModeName(7), HDRRunnable);
            Runnable enterRAW = new Runnable() {
                public void run() {
                    BaseModule.this.enterMutexMode();
                }
            };
            Runnable exitRAW = new Runnable() {
                public void run() {
                    BaseModule.this.exitMutexMode();
                }
            };
            HashMap<String, Runnable> RAWRunnable = new HashMap();
            RAWRunnable.put("enter", enterRAW);
            RAWRunnable.put("exit", exitRAW);
            map.put(MutexModeManager.getMutexModeName(4), RAWRunnable);
            this.mMutexModePicker = new MutexModeManager(map);
        }
    }

    protected void addMuteToParameters(Parameters parameters) {
        parameters.set("camera-service-mute", "true");
    }

    protected void addT2TParameters(Parameters parameters) {
        if (Device.isSupportedObjectTrack()) {
            parameters.set("t2t", "on");
        }
    }

    protected void configOisParameters(Parameters parameters, boolean v) {
        sProxy.setOIS(parameters, v);
    }

    protected void resetFaceBeautyParams(Parameters parameters) {
        sProxy.setStillBeautify(parameters, getString(R.string.pref_face_beauty_close));
    }

    protected void playCameraSound(int soundId) {
        if (CameraSettings.isCameraSoundOpen(this.mPreferences)) {
            this.mActivity.playCameraSound(soundId);
        }
    }

    protected int getPreferencesLocalId() {
        int preferencesLocalId = this.mCameraId;
        if (this.mActivity.isImageCaptureIntent()) {
            return getPreferencesLocalId(this.mCameraId, CameraMode.ImageCapture);
        }
        if (this.mActivity.isVideoCaptureIntent()) {
            return getPreferencesLocalId(this.mCameraId, CameraMode.VideoCapture);
        }
        if (this.mActivity.isScanQRCodeIntent()) {
            return getPreferencesLocalId(this.mCameraId, CameraMode.ScanQRCode);
        }
        return getPreferencesLocalId(this.mCameraId, CameraMode.Normal);
    }

    public static int getPreferencesLocalId(int cameraId, CameraMode mode) {
        return mode.value + cameraId;
    }

    protected int getPreferredCameraId() {
        int intentCameraId = Util.getCameraFacingIntentExtras(this.mActivity);
        if (intentCameraId == -1) {
            intentCameraId = Util.getStartCameraId(this.mActivity);
        }
        if (intentCameraId == -1) {
            intentCameraId = CameraSettings.readPreferredCameraId(this.mPreferences);
        }
        CameraSettings.writePreferredCameraId(this.mPreferences, intentCameraId);
        return intentCameraId;
    }

    protected static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    protected boolean isFrontCamera() {
        return this.mCameraId == CameraHolder.instance().getFrontCameraId();
    }

    protected boolean isBackCamera() {
        return this.mCameraId == CameraHolder.instance().getBackCameraId();
    }

    public boolean isInTapableRect(int x, int y) {
        if (getUIController().getPreviewFrame() == null) {
            return false;
        }
        Point point = new Point(x, y);
        mapTapCoordinate(point);
        return this.mActivity.getCameraScreenNail().getRenderRect().contains(point.x, point.y);
    }

    protected void mapTapCoordinate(Object object) {
        int[] relativeLocation = Util.getRelativeLocation(getUIController().getGLView(), getUIController().getPreviewFrame());
        if (object instanceof Point) {
            Point point = (Point) object;
            point.x -= relativeLocation[0];
            Point point2 = (Point) object;
            point2.y -= relativeLocation[1];
        } else if (object instanceof RectF) {
            RectF rectF = (RectF) object;
            rectF.left -= (float) relativeLocation[0];
            rectF = (RectF) object;
            rectF.right -= (float) relativeLocation[0];
            rectF = (RectF) object;
            rectF.top -= (float) relativeLocation[1];
            RectF rectF2 = (RectF) object;
            rectF2.bottom -= (float) relativeLocation[1];
        }
    }

    public void onZoomValueChanged(int value) {
        onZoomValueChanged(value, false);
    }

    public void onZoomValueChanged(int value, boolean sync) {
        if (!this.mPaused && this.mParameters != null && this.mCameraDevice != null && isCameraEnabled()) {
            setZoomValue(value);
            this.mParameters.setZoom(value);
            if (CameraSettings.isSupportedOpticalZoom() && V6ModulePicker.isCameraModule() && !CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
                if (value > 0) {
                    configOisParameters(this.mParameters, false);
                } else {
                    configOisParameters(this.mParameters, true);
                }
            }
            if (sync) {
                this.mCameraDevice.setParameters(this.mParameters);
            } else {
                this.mCameraDevice.setParametersAsync(this.mParameters);
            }
            updateStatusBar("pref_camera_zoom_key");
            this.mActivity.getUIController().getZoomButton().reloadPreference();
            Log.d("Camera", "Zoom : " + value);
        }
    }

    protected void updateStatusBar(String key) {
        this.mActivity.getUIController().getSettingsStatusBar().updateStatus(key);
    }

    protected void setZoomValue(int value) {
        CameraSettings.writeZoom(this.mPreferences, value);
    }

    protected int getZoomValue() {
        return CameraSettings.readZoom(this.mPreferences);
    }

    public int getZoomMax() {
        return this.mZoomMax;
    }

    public int getZoomMaxRatio() {
        return this.mZoomMaxRatio;
    }

    public boolean isNeedMute() {
        return !CameraSettings.isCameraSoundOpen(this.mPreferences);
    }

    public void notifyError() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setCameraError();
        }
        this.mCameraHardwareError = true;
        if (this.mActivity.isPaused()) {
            this.mActivity.finish();
        } else {
            onCameraException();
        }
    }

    protected void trackPictureTaken(int takenNum, boolean burst, int width, int height, boolean location) {
        CameraDataAnalytics.instance().trackEvent("camera_picture_taken_key", (long) takenNum);
        if (burst) {
            CameraDataAnalytics.instance().trackEvent("capture_nums_burst", (long) takenNum);
        }
        if (CameraSettings.isAspectRatio16_9(width, height)) {
            CameraDataAnalytics.instance().trackEvent("capture_times_size_16_9", (long) takenNum);
        } else {
            CameraDataAnalytics.instance().trackEvent("capture_times_size_4_3", (long) takenNum);
        }
        if (location) {
            CameraDataAnalytics.instance().trackEvent("picture_with_location_key", (long) takenNum);
        } else if (CameraSettings.isRecordLocation(this.mPreferences)) {
            CameraDataAnalytics.instance().trackEvent("picture_without_location_key", (long) takenNum);
        }
        CameraDataAnalytics.instance().trackEvent(isFrontCamera() ? "front_camera_picture_taken_key" : "back_camera_picture_taken_key", (long) takenNum);
    }

    public boolean isShowCaptureButton() {
        return false;
    }

    public boolean isMeteringAreaOnly() {
        return false;
    }

    protected void performVolumeKeyClicked(int repeatCount, boolean pressed) {
    }

    public void onSingleTapUp(int x, int y) {
    }

    public boolean onGestureTrack(RectF rectF, boolean up) {
        return true;
    }

    public boolean canIgnoreFocusChanged() {
        return this.mIgnoreFocusChanged;
    }

    public boolean isKeptBitmapTexture() {
        return false;
    }

    public boolean isCameraEnabled() {
        return true;
    }

    private ArrayList<CameraMode> getCameraModeList() {
        ArrayList<CameraMode> modes = new ArrayList();
        modes.add(CameraMode.Normal);
        modes.add(CameraMode.ImageCapture);
        modes.add(CameraMode.VideoCapture);
        modes.add(CameraMode.ScanQRCode);
        return modes;
    }

    protected int getScreenDelay() {
        return (this.mActivity == null || this.mActivity.startFromKeyguard()) ? 30000 : 60000;
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt("killed-moduleIndex", V6ModulePicker.getCurrentModule());
    }

    public void setRestoring(boolean restoring) {
        this.mRestoring = restoring;
    }

    protected boolean isRestoring() {
        return this.mRestoring;
    }

    protected void changePreviewSurfaceSize() {
        int width = 0;
        int height = 0;
        switch (this.mUIStyle) {
            case 0:
                width = Util.sWindowWidth;
                height = (Util.sWindowWidth * 4) / 3;
                break;
            case 1:
                width = Util.sWindowWidth;
                height = Util.sWindowHeight;
                break;
        }
        this.mActivity.onLayoutChange(width, height);
    }

    protected void updateCameraScreenNailSize(int width, int height, FocusManagerAbstract focusManager) {
        if (this.mCameraDisplayOrientation % 180 != 0) {
            int tmp = width;
            width = height;
            height = tmp;
        }
        if (this.mActivity.getCameraScreenNail().getWidth() == width && this.mActivity.getCameraScreenNail().getHeight() == height && !this.mSwitchingCamera) {
            if (isSquareModeChange()) {
            }
            if (getUIController().getObjectView() != null) {
                getUIController().getObjectView().setPreviewSize(width, height);
            }
        }
        this.mActivity.getCameraScreenNail().setSize(width, height);
        focusManager.setRenderSize(this.mActivity.getCameraScreenNail().getRenderWidth(), this.mActivity.getCameraScreenNail().getRenderHeight());
        if (getUIController().getObjectView() != null) {
            getUIController().getObjectView().setPreviewSize(width, height);
        }
    }

    protected void prepareOpenCamera() {
        if (this.mDisplayRotation != 180 && isFrontCamera() && CameraSettings.isNeedFrontCameraFirstUseHint(this.mPreferences)) {
            this.mHandler.sendEmptyMessageDelayed(0, 200);
        }
    }

    public boolean isCaptureIntent() {
        return false;
    }

    protected void changeConflictPreference() {
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            List<String> keys = getSupportedSettingKeys();
            if (keys != null) {
                Editor editor = CameraSettingPreferences.instance().edit();
                for (String key : keys) {
                    if (CameraSettings.isSwitchOn(key)) {
                        editor.remove(key);
                    }
                }
                editor.apply();
            }
        }
    }

    protected boolean isSquareModeChange() {
        boolean z;
        boolean isSwitchOn = CameraSettings.isSwitchOn("pref_camera_square_mode_key");
        if (this.mActivity.getCameraScreenNail().getRenderTargeRatio() == 2) {
            z = true;
        } else {
            z = false;
        }
        if (isSwitchOn != z) {
            return true;
        }
        return false;
    }
}
