package com.android.camera;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Window;
import android.widget.FrameLayout;
import com.android.camera.camera_adapter.CameraLC;
import com.android.camera.camera_adapter.CameraMTK;
import com.android.camera.camera_adapter.CameraNv;
import com.android.camera.camera_adapter.CameraPadOne;
import com.android.camera.camera_adapter.CameraQcom;
import com.android.camera.camera_adapter.VideoLC;
import com.android.camera.camera_adapter.VideoMTK;
import com.android.camera.camera_adapter.VideoNv;
import com.android.camera.camera_adapter.VideoPadOne;
import com.android.camera.camera_adapter.VideoQcom;
import com.android.camera.effect.EffectController;
import com.android.camera.module.CameraModule;
import com.android.camera.module.Module;
import com.android.camera.module.MorphoPanoramaModule;
import com.android.camera.module.VideoModule;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.ImageSaver;
import com.android.camera.storage.Storage;
import com.android.camera.ui.UIController;
import com.android.camera.ui.V6GestureRecognizer;
import com.android.camera.ui.V6ModulePicker;
import com.android.zxing.QRCodeManager;

public class Camera extends ActivityBase implements OnRequestPermissionsResultCallback {
    private boolean mCameraErrorShown;
    private FrameLayout mContentFrame;
    private boolean mContentInflated;
    private int mCurrentModuleIndex = 0;
    private LogThread mDebugThread;
    private ImageSaver mImageSaver;
    private boolean mIntentChanged;
    private boolean mIsFromLauncher;
    private int mLastIgnoreKey = -1;
    private long mLastKeyEventTime = 0;
    private MyOrientationEventListener mOrientationListener;
    private SensorStateManager mSensorStateManager;
    private int mTick;
    private Thread mWatchDog;
    private final Runnable tickerRunnable = new Runnable() {
        public void run() {
            Camera.this.mTick = (Camera.this.mTick + 1) % 10;
        }
    };

    class LogThread extends Thread {
        private boolean mRunFlag = true;

        LogThread() {
        }

        public void setRunFlag(boolean run) {
            this.mRunFlag = run;
        }

        public void run() {
            while (this.mRunFlag) {
                try {
                    Thread.sleep(10);
                    if (!Camera.this.mPaused) {
                        Camera.this.mHandler.obtainMessage(0, Util.getDebugInfo()).sendToTarget();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private class MyOrientationEventListener extends OrientationEventListener {
        public MyOrientationEventListener(Context context) {
            super(context);
        }

        public void onOrientationChanged(int orientation) {
            if (orientation != -1) {
                Camera.this.mOrientation = Util.roundOrientation(orientation, Camera.this.mOrientation);
                Camera.this.mCurrentModule.onOrientationChanged(orientation);
            }
        }
    }

    private class WatchDogThread extends Thread {
        private WatchDogThread() {
        }

        public void run() {
            setName("ANR-WatchDog");
            while (!isInterrupted()) {
                Log.v("Camera", "watch dog run " + Thread.currentThread().getId());
                int lastTick = Camera.this.mTick;
                Camera.this.mHandler.post(Camera.this.tickerRunnable);
                try {
                    Thread.sleep(5000);
                    if (Camera.this.mTick == lastTick) {
                        CameraSettings.setEdgeMode(Camera.this, false);
                        return;
                    }
                } catch (InterruptedException e) {
                    Log.v("Camera", "watch dog InterruptedException " + Thread.currentThread().getId());
                    return;
                }
            }
        }
    }

    public void onCreate(Bundle state) {
        Handler handler;
        boolean isVideoCaptureIntent;
        super.onCreate(state);
        EffectController.releaseInstance();
        setContentView(R.layout.v6_main);
        getWindow().setBackgroundDrawable(null);
        if (!getKeyguardFlag()) {
            PermissionManager.requestCameraRuntimePermissions(this);
        }
        this.mContentInflated = false;
        this.mContentFrame = (FrameLayout) findViewById(R.id.main_content);
        this.mUIController = new UIController(this);
        if (state != null) {
            int restoreModuleIndex = state.getInt("killed-moduleIndex", -1);
            if (restoreModuleIndex != -1) {
                this.mCurrentModule = getModuleByIndex(restoreModuleIndex);
                this.mCurrentModule.setRestoring(true);
                Log.d("Camera", "restoreModuleIndex=" + restoreModuleIndex);
                this.mIsFromLauncher = true;
                Util.updateCountryIso(this);
                this.mSensorStateManager = new SensorStateManager(this, getMainLooper());
                this.mCurrentModule.onCreate(this);
                this.mOrientationListener = new MyOrientationEventListener(this);
                handler = this.mHandler;
                if (isImageCaptureIntent()) {
                    isVideoCaptureIntent = isVideoCaptureIntent();
                } else {
                    isVideoCaptureIntent = true;
                }
                this.mImageSaver = new ImageSaver(this, handler, isVideoCaptureIntent);
                showDebug();
                setTranslucentNavigation(true);
                trackLaunchEvent();
            }
        }
        if ("android.media.action.VIDEO_CAMERA".equals(getIntent().getAction()) || "android.media.action.VIDEO_CAPTURE".equals(getIntent().getAction())) {
            this.mCurrentModule = getModuleByIndex(1);
        } else {
            this.mCurrentModule = getModuleByIndex(0);
        }
        this.mIsFromLauncher = true;
        Util.updateCountryIso(this);
        this.mSensorStateManager = new SensorStateManager(this, getMainLooper());
        this.mCurrentModule.onCreate(this);
        this.mOrientationListener = new MyOrientationEventListener(this);
        handler = this.mHandler;
        if (isImageCaptureIntent()) {
            isVideoCaptureIntent = true;
        } else {
            isVideoCaptureIntent = isVideoCaptureIntent();
        }
        this.mImageSaver = new ImageSaver(this, handler, isVideoCaptureIntent);
        showDebug();
        setTranslucentNavigation(true);
        trackLaunchEvent();
    }

    private void setTranslucentNavigation(boolean on) {
        if (Util.checkDeviceHasNavigationBar(this)) {
            Window win = getWindow();
            win.getDecorView().setSystemUiVisibility(768);
            win.addFlags(Integer.MIN_VALUE);
        }
    }

    private void trackLaunchEvent() {
        boolean fromKeyguard = false;
        Intent intent = getIntent();
        if (intent == null) {
            CameraDataAnalytics.instance().trackEvent("launch_normal_times_key");
            return;
        }
        String key;
        if (TextUtils.equals(intent.getAction(), "android.media.action.STILL_IMAGE_CAMERA") && getKeyguardFlag()) {
            if ((8388608 & intent.getFlags()) != 0) {
                fromKeyguard = true;
            }
            if (fromKeyguard) {
                key = "launch_keyguard_times_key";
            } else {
                key = "launch_volume_key_times_key";
            }
        } else if (isImageCaptureIntent()) {
            key = "launch_capture_intent_times_key";
        } else if (isVideoCaptureIntent()) {
            key = "launch_video_intent_times_key";
        } else {
            key = "launch_normal_times_key";
        }
        CameraDataAnalytics.instance().trackEvent(key);
    }

    public void createContentView() {
        if (!this.mContentInflated) {
            getLayoutInflater().inflate(R.layout.v6_camera, this.mContentFrame);
            this.mContentInflated = true;
        }
    }

    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.mIntentChanged = true;
        setIntent(intent);
        this.mCurrentModule.onNewIntent();
        trackLaunchEvent();
    }

    public void onResume() {
        boolean z;
        if (getKeyguardFlag() && !PermissionManager.checkCameraLaunchPermissions()) {
            finish();
        }
        if (Util.checkDeviceHasNavigationBar(this)) {
            Util.getWindowAttribute(this);
        }
        Util.checkLockedOrientation(this);
        this.mPaused = false;
        this.mActivityPaused = false;
        switchEdgeFingerMode(true);
        Storage.initStorage(this);
        this.mOrientationListener.enable();
        int switchToModuleIndex = Util.getStartModuleIndex(this);
        if (!this.mIntentChanged || switchToModuleIndex < 0 || switchToModuleIndex == this.mCurrentModuleIndex) {
            this.mCurrentModule.onResumeBeforeSuper();
            super.onResume();
            this.mCurrentModule.onResumeAfterSuper();
        } else {
            super.onResume();
            switchToOtherModule(switchToModuleIndex);
            this.mIntentChanged = false;
        }
        if (this.mCurrentModuleIndex == 0) {
            Util.replaceStartEffectRender(this);
        }
        setBlurFlag(false);
        ImageSaver imageSaver = this.mImageSaver;
        if (isImageCaptureIntent()) {
            z = true;
        } else {
            z = isVideoCaptureIntent();
        }
        imageSaver.onHostResume(z);
        QRCodeManager.instance(this).resetQRScanExit(true);
    }

    public void onStop() {
        super.onStop();
        this.mCurrentModule.onStop();
    }

    public void onPause() {
        this.mPaused = true;
        this.mActivityPaused = true;
        switchEdgeFingerMode(false);
        this.mOrientationListener.disable();
        this.mCurrentModule.onPauseBeforeSuper();
        super.onPause();
        this.mCurrentModule.onPauseAfterSuper();
        this.mImageSaver.onHostPause();
    }

    public void onDestroy() {
        super.onDestroy();
        this.mCurrentModule.onDestroy();
        this.mImageSaver.onHostDestroy();
        this.mSensorStateManager.onDestory();
        QRCodeManager.instance(this).onDestroy();
        V6GestureRecognizer.onDestory(this);
        if (this.mDebugThread != null) {
            this.mDebugThread.setRunFlag(false);
        }
    }

    public void resume() {
        if (!this.mCurrentModule.isVideoRecording()) {
            super.resume();
        }
    }

    public void pause() {
        if (!this.mCurrentModule.isVideoRecording()) {
            super.pause();
        }
    }

    public void switchToOtherModule(int moduleIndex) {
        if (!this.mPaused) {
            this.mIsFromLauncher = false;
            CameraHolder.instance().keep();
            closeModule(this.mCurrentModule);
            openModule(getModuleByIndex(moduleIndex));
        }
    }

    private void openModule(Module module) {
        module.transferOrientationCompensation(this.mCurrentModule);
        this.mCurrentModule = module;
        this.mPaused = false;
        module.onCreate(this);
        module.onResumeBeforeSuper();
        module.onResumeAfterSuper();
    }

    private void closeModule(Module module) {
        this.mPaused = true;
        module.onPauseBeforeSuper();
        module.onPauseAfterSuper();
        module.onStop();
        module.onDestroy();
    }

    private Module getModuleByIndex(int moduleIndex) {
        if (moduleIndex == 2) {
            this.mCurrentModuleIndex = moduleIndex;
            V6ModulePicker.setCurrentModule(this.mCurrentModuleIndex);
            return new MorphoPanoramaModule();
        } else if (moduleIndex == 1) {
            this.mCurrentModuleIndex = moduleIndex;
            V6ModulePicker.setCurrentModule(this.mCurrentModuleIndex);
            return getVideoByDevice();
        } else {
            this.mCurrentModuleIndex = 0;
            V6ModulePicker.setCurrentModule(this.mCurrentModuleIndex);
            return getCameraByDevice();
        }
    }

    private Module getCameraByDevice() {
        if (Device.isPad()) {
            return new CameraPadOne();
        }
        if (Device.isQcomPlatform()) {
            return new CameraQcom();
        }
        if (Device.isLCPlatform()) {
            return new CameraLC();
        }
        if (Device.isNvPlatform()) {
            return new CameraNv();
        }
        if (Device.isMTKPlatform()) {
            return new CameraMTK();
        }
        return new CameraModule();
    }

    private Module getVideoByDevice() {
        if (Device.isQcomPlatform()) {
            return new VideoQcom();
        }
        if (Device.isLCPlatform()) {
            return new VideoLC();
        }
        if (Device.isNvPlatform()) {
            return new VideoNv();
        }
        if (Device.isMTKPlatform()) {
            return new VideoMTK();
        }
        if (Device.isPad()) {
            return new VideoPadOne();
        }
        return new VideoModule();
    }

    public void onBackPressed() {
        if (!this.mCurrentModule.onBackPressed()) {
            super.onBackPressed();
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean z;
        if (event.getRepeatCount() == 0 && (keyCode == 66 || keyCode == 27 || keyCode == 24 || keyCode == 25)) {
            if (Util.isTimeout(event.getEventTime(), this.mLastKeyEventTime, 150)) {
                this.mLastIgnoreKey = -1;
            } else {
                this.mLastIgnoreKey = keyCode;
                return true;
            }
        } else if (event.getRepeatCount() > 0 && keyCode == this.mLastIgnoreKey) {
            this.mLastIgnoreKey = -1;
        }
        if (this.mCurrentModule.onKeyDown(keyCode, event)) {
            z = true;
        } else {
            z = super.onKeyDown(keyCode, event);
        }
        return z;
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean z = true;
        if (keyCode == this.mLastIgnoreKey) {
            this.mLastKeyEventTime = 0;
            this.mLastIgnoreKey = -1;
            return true;
        }
        this.mLastKeyEventTime = event.getEventTime();
        if (!this.mCurrentModule.onKeyUp(keyCode, event)) {
            z = super.onKeyUp(keyCode, event);
        }
        return z;
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        this.mCurrentModule.onWindowFocusChanged(hasFocus);
        if (!(this.mCameraBrightness == null || this.mCurrentModule.canIgnoreFocusChanged())) {
            this.mCameraBrightness.onWindowFocusChanged(hasFocus);
        }
        if (hasFocus) {
            Util.checkLockedOrientation(this);
            this.mCurrentModule.checkActivityOrientation();
            if (this.mSensorStateManager != null) {
                this.mSensorStateManager.register();
            }
        } else if (this.mSensorStateManager != null) {
            this.mSensorStateManager.unregister(15);
        }
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        return !super.dispatchTouchEvent(event) ? this.mCurrentModule.dispatchTouchEvent(event) : true;
    }

    public void onUserInteraction() {
        super.onUserInteraction();
        this.mCurrentModule.onUserInteraction();
    }

    public ImageSaver getImageSaver() {
        return this.mImageSaver;
    }

    public SensorStateManager getSensorStateManager() {
        return this.mSensorStateManager;
    }

    public void setBlurFlag(boolean blurred) {
        if (blurred) {
            getWindow().addFlags(4);
            getUIController().getGLView().setBackgroundColor(getResources().getColor(R.color.realtimeblur_bg));
            return;
        }
        getWindow().clearFlags(4);
        getUIController().getGLView().setBackground(null);
    }

    public int getCapturePosture() {
        return this.mSensorStateManager.getCapturePosture();
    }

    private void switchEdgeFingerMode(boolean enable) {
        if (Device.isSupportedEdgeTouch()) {
            CameraSettings.setEdgeMode(this, enable);
            if (enable) {
                this.mWatchDog = new WatchDogThread();
                this.mWatchDog.start();
            } else if (this.mWatchDog != null) {
                this.mWatchDog.interrupt();
                this.mWatchDog = null;
            }
        }
    }

    private void showDebug() {
        if (Util.isShowDebugInfo()) {
            Log.e("CameraDebug", "ready to start show debug info ");
            this.mUIController.showDebugView();
            this.mDebugThread = new LogThread();
            this.mDebugThread.start();
        }
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PermissionManager.getCameraRuntimePermissionRequestCode()) {
            if (!PermissionManager.isCameraLaunchPermissionsResultReady(permissions, grantResults)) {
                finish();
            }
            if (!this.mPaused && PermissionManager.isCameraLocationPermissionsResultReady(permissions, grantResults)) {
                LocationManager.instance().recordLocation(CameraSettings.isRecordLocation(CameraSettingPreferences.instance()));
            }
        }
    }

    public void changeRequestOrientation() {
        if (Device.IS_A8 || Device.IS_D5) {
            if (CameraSettings.isFrontCamera()) {
                setRequestedOrientation(7);
            } else {
                setRequestedOrientation(1);
            }
        }
    }

    public boolean couldShowErrorDialog() {
        return !this.mCameraErrorShown;
    }

    public void showErrorDialog() {
        this.mCameraErrorShown = true;
    }
}
