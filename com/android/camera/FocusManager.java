package com.android.camera;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.FocusIndicator;
import com.android.camera.ui.FocusView;
import com.android.camera.ui.FrameView;
import java.util.ArrayList;
import java.util.List;

public class FocusManager extends FocusManagerAbstract {
    private boolean mAeAwbLock;
    private long mCafStartTime;
    private Context mContext;
    private String[] mDefaultFocusModes;
    private boolean mFocusAreaSupported;
    private String mFocusMode;
    private FocusView mFocusView;
    private FrameView mFrameView;
    private Handler mHandler;
    private boolean mKeepFocusUIState;
    private int mLastFocusFrom = -1;
    private int mLastState = 0;
    private RectF mLatestFocusFace;
    private long mLatestFocusTime;
    private Listener mListener;
    private boolean mLockAeAwbNeeded;
    private boolean mMeteringAreaSupported;
    private String mOverrideFocusMode;
    private Parameters mParameters;
    private boolean mPendingMultiCapture;

    public interface Listener {
        void autoFocus();

        void cancelAutoFocus();

        boolean capture();

        boolean multiCapture();

        void playSound(int i);

        void setFocusParameters();

        void startFaceDetection();

        void stopObjectTracking(boolean z);
    }

    private class MainHandler extends Handler {
        public MainHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                case 1:
                    FocusManager.this.cancelAutoFocus();
                    FocusManager.this.mListener.startFaceDetection();
                    return;
                default:
                    return;
            }
        }
    }

    public FocusManager(Context context, CameraSettingPreferences preferences, String[] defaultFocusModes, FocusView focusView, Parameters parameters, Listener listener, boolean mirror, Looper looper) {
        this.mHandler = new MainHandler(looper);
        this.mContext = context;
        this.mDefaultFocusModes = defaultFocusModes;
        this.mFocusView = focusView;
        setParameters(parameters);
        this.mListener = listener;
        setMirror(mirror);
    }

    public void setParameters(Parameters parameters) {
        boolean isSupported;
        boolean z = true;
        boolean z2 = false;
        this.mParameters = parameters;
        if (this.mParameters.getMaxNumFocusAreas() > 0) {
            isSupported = isSupported("auto", CameraHardwareProxy.getDeviceProxy().getSupportedFocusModes(this.mParameters));
        } else {
            isSupported = false;
        }
        this.mFocusAreaSupported = isSupported;
        if (this.mParameters.getMaxNumMeteringAreas() > 0) {
            z2 = true;
        }
        this.mMeteringAreaSupported = z2;
        if (!this.mParameters.isAutoExposureLockSupported()) {
            z = this.mParameters.isAutoWhiteBalanceLockSupported();
        }
        this.mLockAeAwbNeeded = z;
    }

    public void setPreviewSize(int previewWidth, int previewHeight) {
        if (this.mPreviewWidth != previewWidth || this.mPreviewHeight != previewHeight) {
            this.mPreviewWidth = previewWidth;
            this.mPreviewHeight = previewHeight;
            setMatrix();
        }
    }

    public void setFrameView(FrameView frameView) {
        this.mFrameView = frameView;
    }

    private void setFocusState(int state) {
        this.mState = state;
    }

    private void setLastFocusState(int state) {
        this.mLastState = state;
    }

    public void prepareCapture(boolean autoFocus, int fromWhat) {
        if (this.mInitialized) {
            boolean tryAutoFocus = true;
            boolean autoFocusCalled = false;
            String focusMode = getFocusMode();
            if (fromWhat == 2 && (("auto".equals(focusMode) || "macro".equals(focusMode)) && this.mLastState == 3)) {
                tryAutoFocus = false;
            }
            boolean isCaf = "continuous-picture".equals(focusMode);
            if (isFocusEnabled() && !isCaf && tryAutoFocus) {
                if (this.mState == 3 || this.mState == 4) {
                    if (!(!autoFocus || this.mFocusArea == null || Device.isResetToCCAFAfterCapture())) {
                        this.mKeepFocusUIState = true;
                        autoFocus(this.mLastFocusFrom);
                        this.mKeepFocusUIState = false;
                        autoFocusCalled = true;
                    }
                } else if (this.mFrameView == null || !this.mFrameView.faceExists()) {
                    resetFocusAreaToCenter();
                    autoFocus(0);
                    autoFocusCalled = true;
                } else {
                    focusFaceArea();
                    autoFocusCalled = true;
                }
            }
            if (!autoFocusCalled && autoFocus && isCaf) {
                if (!Device.isHalDoesCafWhenFlashOn()) {
                    requestAutoFocus();
                } else if (this.mState == 1) {
                    cancelAutoFocus();
                }
            }
        }
    }

    public void onShutterDown() {
    }

    public void onShutterUp() {
    }

    public void doSnap() {
        if (this.mInitialized) {
            if (this.mState == 3 || this.mState == 4 || !needAutoFocusCall()) {
                capture();
            } else if (this.mState == 1) {
                setFocusState(2);
            } else if (this.mState == 0) {
                capture();
            }
        }
    }

    public void doMultiSnap(boolean checkFocusState) {
        if (this.mInitialized) {
            if (!checkFocusState) {
                multiCapture();
            }
            if (this.mState == 3 || this.mState == 4 || !needAutoFocusCall()) {
                multiCapture();
            } else if (this.mState == 1) {
                setFocusState(2);
                this.mPendingMultiCapture = true;
            } else if (this.mState == 0) {
                multiCapture();
            }
        }
    }

    public void onAutoFocus(boolean focused) {
        if (this.mState == 2) {
            if (focused) {
                setFocusState(3);
                setLastFocusState(3);
            } else {
                setFocusState(4);
                setLastFocusState(4);
            }
            updateFocusUI();
            if (this.mPendingMultiCapture) {
                multiCapture();
            } else {
                capture();
            }
        } else if (this.mState == 1) {
            if (focused) {
                setFocusState(3);
                setLastFocusState(3);
                if (!("continuous-picture".equals(this.mFocusMode) || this.mLastFocusFrom == 1)) {
                    this.mListener.playSound(1);
                }
            } else {
                int i;
                if (this.mMirror) {
                    i = 1;
                } else {
                    i = 4;
                }
                setFocusState(i);
                setLastFocusState(4);
            }
            updateFocusUI();
            this.mHandler.removeMessages(1);
            this.mCancelAutoFocusIfMove = true;
        } else if (this.mState != 0) {
        }
    }

    public void onAutoFocusMoving(boolean moving, boolean isSuccessful) {
        if (this.mInitialized) {
            boolean showFocusIndicator = true;
            if (this.mFrameView != null && this.mFrameView.faceExists()) {
                this.mFocusView.clear();
                showFocusIndicator = false;
            }
            if (this.mFocusArea == null && "continuous-picture".equals(getFocusMode())) {
                if (moving) {
                    if (this.mState != 2) {
                        setFocusState(1);
                    }
                    Log.v("FocusManager", "Camera KPI: CAF start");
                    this.mCafStartTime = System.currentTimeMillis();
                    if (showFocusIndicator) {
                        this.mFocusView.showStart();
                    }
                } else {
                    int state = this.mState;
                    Log.v("FocusManager", "Camera KPI: CAF stop: Focus time: " + (System.currentTimeMillis() - this.mCafStartTime));
                    if (isSuccessful) {
                        setFocusState(3);
                        setLastFocusState(3);
                    } else {
                        setFocusState(4);
                        setLastFocusState(4);
                    }
                    if (showFocusIndicator) {
                        if (isSuccessful) {
                            this.mFocusView.showSuccess();
                        } else {
                            this.mFocusView.showFail();
                        }
                    }
                    if (state == 2) {
                        setFocusState(3);
                        this.mFocusView.showSuccess();
                        if (this.mPendingMultiCapture) {
                            multiCapture();
                        } else {
                            capture();
                        }
                    }
                }
            }
        }
    }

    private boolean resetFocusAreaToFaceArea() {
        if (this.mFrameView != null && this.mFrameView.faceExists()) {
            RectF rect = this.mFrameView.getFocusRect();
            if (rect != null) {
                this.mLatestFocusFace = rect;
                initializeFocusAreas(this.FOCUS_AREA_WIDTH, this.FOCUS_AREA_HEIGHT, (int) ((rect.left + rect.right) / 2.0f), (int) ((rect.top + rect.bottom) / 2.0f), this.mPreviewWidth, this.mPreviewHeight);
                return true;
            }
        }
        return false;
    }

    private void resetFocusAreaToCenter() {
        initializeFocusAreas(this.FOCUS_AREA_WIDTH, this.FOCUS_AREA_HEIGHT, this.mPreviewWidth / 2, this.mPreviewHeight / 2, this.mPreviewWidth, this.mPreviewHeight);
        initializeFocusIndicator(this.mPreviewWidth / 2, this.mPreviewHeight / 2);
    }

    private void initializeFocusAreas(int focusWidth, int focusHeight, int x, int y, int previewWidth, int previewHeight) {
        if (this.mFocusArea == null) {
            this.mFocusArea = new ArrayList();
            this.mFocusArea.add(new Area(new Rect(), 1));
        }
        calculateTapArea(focusWidth, focusHeight, 1.0f, x, y, previewWidth, previewHeight, ((Area) this.mFocusArea.get(0)).rect);
    }

    private void initializeMeteringAreas(int focusWidth, int focusHeight, int x, int y, int previewWidth, int previewHeight, int from) {
        if (this.mMeteringArea == null) {
            this.mMeteringArea = new ArrayList();
            this.mMeteringArea.add(new Area(new Rect(), 1));
        }
        if (from != 1 || this.mFrameView.isNeedExposure()) {
            calculateTapArea(focusWidth, focusHeight, 1.8f, x, y, previewWidth, previewHeight, ((Area) this.mMeteringArea.get(0)).rect);
            return;
        }
        this.mMeteringArea = null;
    }

    private void initializeFocusIndicator(int x, int y) {
        this.mFocusView.setPosition(x, y);
    }

    public void resetFocusIndicator() {
        this.mFocusView.clear();
    }

    private void initializeParameters(int x, int y, int from, boolean onlyAe) {
        int previewX = x;
        int previewY = y;
        if (EffectController.getInstance().isFishEye()) {
            float[] pts = new float[]{(float) x, (float) y};
            this.mPreviewChangeMatrix.mapPoints(pts);
            previewX = (int) pts[0];
            previewY = (int) pts[1];
        }
        if (this.mFocusAreaSupported && !onlyAe) {
            initializeFocusAreas(this.FOCUS_AREA_WIDTH, this.FOCUS_AREA_HEIGHT, previewX, previewY, this.mPreviewWidth, this.mPreviewHeight);
        }
        if (this.mMeteringAreaSupported) {
            initializeMeteringAreas(this.FOCUS_AREA_WIDTH, this.FOCUS_AREA_HEIGHT, previewX, previewY, this.mPreviewWidth, this.mPreviewHeight, from);
        }
        initializeFocusIndicator(x, y);
    }

    public void onSingleTapUp(int x, int y) {
        boolean z = true;
        if (1 != getTapAction()) {
            z = false;
        }
        focusPoint(x, y, 3, z);
    }

    private void focusPoint(int x, int y, int from, boolean onlyAe) {
        if (this.mInitialized && this.mState != 2 && (this.mOverrideFocusMode == null || isAutoFocusMode(this.mOverrideFocusMode))) {
            if (isNeedCancelAutoFocus()) {
                cancelAutoFocus();
            }
            initializeParameters(x, y, from, onlyAe);
            this.mListener.setFocusParameters();
            if (!this.mFocusAreaSupported || onlyAe) {
                if (this.mMeteringAreaSupported) {
                    if (3 == from && isFocusValid(from)) {
                        this.mCancelAutoFocusIfMove = true;
                    }
                    this.mLastFocusFrom = from;
                    setFocusState(1);
                    updateFocusUI();
                    this.mHandler.removeMessages(0);
                }
            } else if (isFocusValid(from)) {
                autoFocus(from);
            }
        }
    }

    public void requestAutoFocus() {
        if (needAutoFocusCall() && this.mInitialized && this.mState != 2) {
            int from = 4;
            if (isNeedCancelAutoFocus()) {
                this.mListener.cancelAutoFocus();
                this.mFocusView.clear();
                setFocusState(0);
                this.mCancelAutoFocusIfMove = false;
                this.mHandler.removeMessages(0);
                this.mHandler.removeMessages(1);
            }
            if (resetFocusAreaToFaceArea()) {
                this.mFocusView.clear();
                from = 1;
            } else {
                resetFocusAreaToCenter();
            }
            this.mAeAwbLock = false;
            this.mListener.setFocusParameters();
            autoFocus(from);
        }
    }

    public boolean focusFaceArea() {
        if (this.mFrameView == null || !isAutoFocusMode(getFocusMode())) {
            return false;
        }
        RectF rect = this.mFrameView.getFocusRect();
        if (rect == null) {
            return false;
        }
        if (this.mLatestFocusFace != null && this.mLastFocusFrom == 1 && Math.abs(rect.left - this.mLatestFocusFace.left) < 80.0f && Math.abs((rect.right - rect.left) - (this.mLatestFocusFace.right - this.mLatestFocusFace.left)) < 80.0f) {
            return false;
        }
        this.mLatestFocusFace = rect;
        focusPoint((int) ((rect.left + rect.right) / 2.0f), (int) ((rect.top + rect.bottom) / 2.0f), 1, false);
        return true;
    }

    public void onShutter() {
        updateFocusUI();
        this.mAeAwbLock = false;
    }

    public void onPreviewStarted() {
        setFocusState(0);
    }

    public void onPreviewStopped() {
        setFocusState(0);
        resetTouchFocus();
        updateFocusUI();
    }

    public void onCameraReleased() {
        onPreviewStopped();
    }

    private boolean isFocusValid(int from) {
        long now = System.currentTimeMillis();
        int i = (this.mLastFocusFrom == 3 || this.mLastFocusFrom == 4) ? 5000 : 4000;
        long timeout = (long) i;
        if (from >= 3 || from >= this.mLastFocusFrom || Util.isTimeout(now, this.mLatestFocusTime, timeout)) {
            this.mLatestFocusTime = System.currentTimeMillis();
            return true;
        }
        if (this.mLastFocusFrom == 1) {
            resetTouchFocus();
        }
        return false;
    }

    private void autoFocus(int from) {
        Log.v("FocusManager", "start autoFocus from " + from);
        this.mLastFocusFrom = from;
        if (from != 1 || (this.mFrameView instanceof FaceView)) {
            this.mListener.stopObjectTracking(false);
        }
        this.mListener.autoFocus();
        if (!(this.mFrameView == null || from == 1)) {
            this.mFrameView.pause();
        }
        setFocusState(1);
        updateFocusUI();
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    private void cancelAutoFocus() {
        resetTouchFocus();
        if (needAutoFocusCall()) {
            this.mListener.cancelAutoFocus();
        } else {
            this.mListener.setFocusParameters();
        }
        setFocusState(0);
        updateFocusUI();
        this.mCancelAutoFocusIfMove = false;
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        Log.v("FocusManager", "cancelAutoFocus");
    }

    private void capture() {
        if (this.mListener.capture()) {
            if (Device.isResetToCCAFAfterCapture()) {
                setFocusState(0);
                this.mCancelAutoFocusIfMove = false;
            }
            this.mPendingMultiCapture = false;
            this.mHandler.removeMessages(0);
        }
    }

    private void multiCapture() {
        if (this.mListener.multiCapture()) {
            setFocusState(0);
            this.mPendingMultiCapture = false;
            this.mHandler.removeMessages(0);
        }
    }

    public void resetFocusStateIfNeeded() {
        this.mFocusArea = null;
        this.mMeteringArea = null;
        setFocusState(0);
        setLastFocusState(0);
        this.mCancelAutoFocusIfMove = false;
        if (!this.mHandler.hasMessages(0)) {
            this.mHandler.sendEmptyMessage(0);
        }
    }

    public String getFocusMode() {
        if (this.mOverrideFocusMode != null) {
            return this.mOverrideFocusMode;
        }
        List<String> supportedFocusModes = CameraHardwareProxy.getDeviceProxy().getSupportedFocusModes(this.mParameters);
        this.mFocusMode = CameraSettings.getFocusMode();
        if (this.mFocusAreaSupported && this.mFocusArea != null) {
            if ("manual".equals(this.mFocusMode)) {
                this.mFocusMode = "manual";
            } else if ("continuous-picture".equals(this.mFocusMode) || "continuous-video".equals(this.mFocusMode) || "macro".equals(this.mFocusMode)) {
                this.mFocusMode = "auto";
            }
        }
        if (!isSupported(this.mFocusMode, supportedFocusModes)) {
            boolean find = false;
            for (String mode : this.mDefaultFocusModes) {
                if (Util.isSupported(mode, supportedFocusModes)) {
                    this.mFocusMode = mode;
                    find = true;
                    break;
                }
            }
            if (!find) {
                if (isSupported("auto", supportedFocusModes)) {
                    this.mFocusMode = "auto";
                } else {
                    this.mFocusMode = this.mParameters.getFocusMode();
                }
            }
            if (this.mFocusMode != null) {
                Editor editor = CameraSettingPreferences.instance().edit();
                editor.putString("pref_camera_focus_mode_key", this.mFocusMode);
                editor.apply();
            }
        }
        if ("continuous-picture".equals(this.mFocusMode)) {
            this.mLastFocusFrom = -1;
        }
        Log.v("FocusManager", "FocusMode = " + this.mFocusMode);
        return this.mFocusMode;
    }

    public List<Area> getFocusAreas() {
        return this.mFocusArea;
    }

    public List<Area> getMeteringAreas() {
        return this.mMeteringArea;
    }

    public void updateFocusUI() {
        if (this.mInitialized && !this.mKeepFocusUIState) {
            FocusIndicator focusIndicator;
            if (this.mLastFocusFrom == 1) {
                focusIndicator = this.mFrameView;
            } else {
                focusIndicator = this.mFocusView;
            }
            if (this.mState == 0) {
                focusIndicator.clear();
            } else if (this.mState == 1 || this.mState == 2) {
                focusIndicator.showStart();
            } else if ("continuous-picture".equals(this.mFocusMode)) {
                focusIndicator.showSuccess();
            } else if (this.mState == 3) {
                focusIndicator.showSuccess();
            } else if (this.mState == 4) {
                focusIndicator.showFail();
            }
        }
    }

    public void resetTouchFocus() {
        if (this.mInitialized) {
            this.mFocusArea = null;
            this.mMeteringArea = null;
            this.mCancelAutoFocusIfMove = false;
            resetFocusIndicator();
        }
        if (this.mFrameView != null) {
            this.mFrameView.resume();
        }
    }

    public void resetAfterCapture(boolean forceFocusCapture) {
        if (Device.isResetToCCAFAfterCapture()) {
            resetTouchFocus();
        } else if (!forceFocusCapture) {
        } else {
            if (this.mLastFocusFrom == 4) {
                this.mListener.cancelAutoFocus();
                resetTouchFocus();
                removeMessages();
                return;
            }
            setLastFocusState(0);
        }
    }

    public boolean isFocusCompleted() {
        return this.mState == 3 || this.mState == 4;
    }

    public boolean isFocusingSnapOnFinish() {
        return this.mState == 2;
    }

    public boolean cancelMultiSnapPending() {
        if (this.mState != 2 || !this.mPendingMultiCapture) {
            return false;
        }
        this.mPendingMultiCapture = false;
        return true;
    }

    public void removeMessages() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
    }

    public void overrideFocusMode(String focusMode) {
        this.mOverrideFocusMode = focusMode;
    }

    public void setAeAwbLock(boolean lock) {
        this.mAeAwbLock = lock;
    }

    public boolean getAeAwbLock() {
        return this.mAeAwbLock;
    }

    private static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    private boolean needAutoFocusCall() {
        return 2 == getTapAction() ? this.mFocusAreaSupported : false;
    }

    private int getTapAction() {
        String focusMode = getFocusMode();
        if (focusMode.equals("infinity") || focusMode.equals("edof") || focusMode.equals("fixed") || focusMode.equals("lock") || focusMode.equals("manual")) {
            return 1;
        }
        return 2;
    }

    private boolean isAutoFocusMode(String focusMode) {
        if ("auto".equals(focusMode)) {
            return true;
        }
        return "macro".equals(focusMode);
    }

    private boolean isNeedCancelAutoFocus() {
        if (this.mHandler.hasMessages(0) || this.mHandler.hasMessages(1)) {
            return true;
        }
        return this.mCancelAutoFocusIfMove;
    }

    public void onDeviceKeepMoving(double a) {
        if (Util.isTimeout(System.currentTimeMillis(), this.mLatestFocusTime, 3000)) {
            setLastFocusState(0);
            if (this.mCancelAutoFocusIfMove) {
                this.mHandler.sendEmptyMessage(0);
            }
        }
    }

    private boolean isFocusEnabled() {
        if (!this.mInitialized || this.mState == 2 || this.mState == 1) {
            return false;
        }
        return needAutoFocusCall();
    }
}
