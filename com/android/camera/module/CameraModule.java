package com.android.camera.module;

import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.android.camera.AudioCaptureManager;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraDisabledException;
import com.android.camera.CameraHardwareException;
import com.android.camera.CameraHolder;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraPreferenceActivity;
import com.android.camera.CameraSettings;
import com.android.camera.ChangeManager;
import com.android.camera.Device;
import com.android.camera.Exif;
import com.android.camera.ExifHelper;
import com.android.camera.FocusManager;
import com.android.camera.FocusManager.Listener;
import com.android.camera.JpegEncodingQualityMappings;
import com.android.camera.LocationManager;
import com.android.camera.OnClickAttr;
import com.android.camera.PictureSize;
import com.android.camera.PictureSizeManager;
import com.android.camera.SensorStateManager.SensorStateListener;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.EffectController.EffectRectAttribute;
import com.android.camera.effect.draw_mode.DrawJPEGAttribute;
import com.android.camera.effect.renders.SnapshotEffectRender;
import com.android.camera.groupshot.GroupShot;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;
import com.android.camera.hardware.CameraHardwareProxy.CameraMetaDataCallback;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;
import com.android.camera.ui.FaceView;
import com.android.camera.ui.FocusView;
import com.android.camera.ui.ObjectView.ObjectViewListener;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.V6GestureRecognizer;
import com.android.camera.ui.V6ModulePicker;
import com.android.zxing.QRCodeManager;
import com.android.zxing.QRCodeManager.QRCodeManagerListener;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CameraModule extends BaseModule implements Listener, FaceDetectionListener, QRCodeManagerListener, ObjectViewListener, CameraMetaDataCallback {
    protected static final int BURST_SHOOTING_COUNT = Device.getBurstShootCount();
    protected boolean m3ALocked;
    private int mAFEndLogTimes;
    private boolean mAeLockSupported;
    protected AudioCaptureManager mAudioCaptureManager;
    private final AutoFocusCallback mAutoFocusCallback = new AutoFocusCallback();
    private final AutoFocusMoveCallback mAutoFocusMoveCallback = new AutoFocusMoveCallback();
    public long mAutoFocusTime;
    protected boolean mAwbLockSupported;
    private String mBurstShotTitle;
    private BurstSpeedController mBurstSpeedController = new BurstSpeedController();
    protected CameraCategory mCameraCategory = new CameraCategory();
    private CameraStartUpThread mCameraStartUpThread;
    protected volatile int mCameraState = 0;
    private byte[] mCameraUUIDWatermarkImageData;
    public long mCaptureStartTime;
    private boolean mContinuousFocusSupported;
    private String mCropValue;
    private boolean mDidRegister = false;
    private int mDoCaptureRetry = 0;
    private Runnable mDoSnapRunnable = new Runnable() {
        public void run() {
            CameraModule.this.onShutterButtonClick();
        }
    };
    private SnapshotEffectRender mEffectProcessor;
    protected boolean mFaceDetectionEnabled = true;
    private boolean mFaceDetectionStarted;
    private boolean mFirstTimeInitialized;
    private boolean mFocusAreaSupported;
    protected FocusManager mFocusManager;
    private long mFocusStartTime;
    protected boolean mFoundFace;
    private int mGroupFaceNum = 10;
    private GroupShot mGroupShot;
    private int mGroupShotTimes;
    protected final Handler mHandler = new MainHandler();
    private Parameters mInitialParams;
    private boolean mIsCaptureAfterLaunch;
    private boolean mIsCountDown;
    protected boolean mIsImageCaptureIntent;
    boolean mIsRecreateCameraScreenNail;
    private boolean mIsSaveCaptureImage;
    protected boolean mIsZSLMode;
    public long mJpegCallbackFinishTime;
    private byte[] mJpegImageData;
    private long mJpegPictureCallbackTime;
    protected int mJpegRotation;
    private boolean mKeepBitmapTexture;
    private long mLastFreezeHDRTime;
    private boolean mLastIsEffect;
    private long mLastShutterButtonClickTime;
    private boolean mLongPressedAutoFocus;
    private boolean mManualModeSwitched;
    private ContentProviderClient mMediaProviderClient;
    protected MetaDataManager mMetaDataManager = new MetaDataManager();
    private boolean mMeteringAreaSupported;
    protected Size mMultiSnapPictureSize;
    protected boolean mMultiSnapStatus = false;
    protected boolean mMultiSnapStopRequest = false;
    private boolean mNeedAutoFocus;
    private boolean mNeedSealCameraUUID;
    private long mOnResumeTime;
    private boolean mPendingCapture;
    private boolean mPendingMultiCapture;
    public long mPictureDisplayedToJpegCallbackTime;
    protected final PostViewPictureCallback mPostViewPictureCallback = new PostViewPictureCallback();
    private long mPostViewPictureCallbackTime;
    private int mPreviewHeight;
    private PreviewTextureCopiedCallback mPreviewTextureCopiedActionByPass = new PreviewTextureCopiedCallback() {
        public void onPreviewTextureCopied() {
            CameraModule.this.mActivity.getCameraScreenNail().animateSwitchCameraBefore();
            CameraModule.this.mHandler.sendEmptyMessage(19);
        }
    };
    private PreviewTextureCopiedCallback mPreviewTextureCopiedActionSwitchCamera = new PreviewTextureCopiedCallback() {
        public void onPreviewTextureCopied() {
            CameraModule.this.animateSwitchCamera();
            CameraModule.this.mHandler.sendEmptyMessage(6);
        }
    };
    private PreviewTextureCopiedCallback mPreviewTextureCopiedActionSwitchCameraLater = new PreviewTextureCopiedCallback() {
        public void onPreviewTextureCopied() {
            CameraModule.this.animateSwitchCamera();
            synchronized (CameraModule.this.mSwitchCameraLater) {
                if (!CameraModule.this.mSwitchCameraLater.booleanValue()) {
                    CameraModule.this.mHandler.sendEmptyMessage(6);
                }
                CameraModule.this.mSwitchCameraLater = Boolean.valueOf(false);
            }
        }
    };
    private PreviewTextureCopiedCallback mPreviewTextureCopiedCallback;
    private int mPreviewWidth;
    protected boolean mQuickCapture;
    protected final RawPictureCallback mRawPictureCallback = new RawPictureCallback();
    private long mRawPictureCallbackTime;
    protected int mReceivedJpegCallbackNum = 0;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("Camera", "Received intent action=" + action);
            if (action.equals("android.intent.action.MEDIA_MOUNTED") || action.equals("android.intent.action.MEDIA_UNMOUNTED") || action.equals("android.intent.action.MEDIA_CHECKING")) {
                CameraModule.this.mActivity.getScreenHint().updateHint();
            } else if (action.equals("android.intent.action.MEDIA_SCANNER_FINISHED")) {
                CameraModule.this.mActivity.getScreenHint().updateHint();
                if (!CameraModule.this.mIsImageCaptureIntent) {
                    CameraModule.this.mActivity.getThumbnailUpdater().getLastThumbnail();
                }
            } else if (action.equals("android.intent.action.MEDIA_EJECT") && Storage.isCurrentStorageIsSecondary()) {
                Storage.switchToPhoneStorage();
            }
        }
    };
    protected boolean mRestartPreview = false;
    private Uri mSaveUri;
    protected String mSceneMode;
    private SensorStateListener mSensorStateListener = new SensorStateListener() {
        public void onDeviceBecomeStable() {
        }

        public boolean isWorking() {
            return CameraModule.this.mCameraState != 0;
        }

        public void onDeviceKeepMoving(double a) {
            if (!CameraModule.this.mPaused && CameraModule.this.mFocusManager != null && !CameraModule.this.mMultiSnapStatus && !CameraModule.this.getUIController().getShutterButton().isPressed() && !CameraModule.this.m3ALocked && !CameraModule.this.getUIController().getFocusView().isEvAdjustedTime()) {
                CameraModule.this.mFocusManager.onDeviceKeepMoving(a);
            }
        }

        public void onDeviceBeginMoving() {
            if (!CameraModule.this.mPaused && CameraSettings.isEdgePhotoEnable()) {
                CameraModule.this.getUIController().getEdgeShutterView().onDeviceMoving();
            }
        }

        public void onDeviceOrientationChanged(float orientation, boolean isLying) {
            if (CameraSettings.isSwitchOn("pref_camera_gradienter_key")) {
                CameraModule.this.mDeviceRotation = orientation;
                if (CameraModule.this.mCameraState != 3) {
                    EffectController.getInstance().setDeviceRotation(CameraModule.this.mActivity.getSensorStateManager().isDeviceLying(), Util.getShootRotation(CameraModule.this.mActivity, CameraModule.this.mDeviceRotation));
                }
                CameraModule.this.mHandler.removeMessages(33);
                if (!CameraModule.this.mPaused) {
                    CameraModule.this.mHandler.obtainMessage(33, Math.round(orientation), 0).sendToTarget();
                }
            }
            if (!CameraModule.this.mPaused && CameraModule.this.mActivity.getSensorStateManager().canDetectOrientation()) {
                CameraModule.this.mHandler.removeMessages(39);
                CameraModule.this.mHandler.obtainMessage(39, Boolean.valueOf(isLying)).sendToTarget();
            }
        }

        public void notifyDevicePostureChanged() {
            CameraModule.this.getUIController().getEdgeShutterView().onDevicePostureChanged();
        }
    };
    private int mSetCameraParameter = 0;
    private boolean mSetMetaCallback;
    private int mShootOrientation;
    private float mShootRotation;
    protected final ShutterCallback mShutterCallback = new ShutterCallback();
    private long mShutterCallbackTime;
    public long mShutterLag;
    private boolean mSnapshotOnIdle = false;
    private ConditionVariable mStartPreviewPrerequisiteReady = new ConditionVariable();
    private boolean mSwitchCameraAnimationRunning;
    private Boolean mSwitchCameraLater;
    protected int mTotalJpegCallbackNum = 1;
    private boolean mUpdateImageTitle = false;
    private int mUpdateSet;
    protected boolean mVolumeLongPress = false;

    protected class CameraCategory {
        protected CameraCategory() {
        }

        public void takePicture(Location loc) {
            PictureCallback jpegPictureCallback;
            CameraProxy cameraProxy = CameraModule.this.mCameraDevice;
            android.hardware.Camera.ShutterCallback shutterCallback = CameraModule.this.mShutterCallback;
            PictureCallback pictureCallback = CameraModule.this.mRawPictureCallback;
            PictureCallback pictureCallback2 = CameraModule.this.mPostViewPictureCallback;
            if (CameraModule.this.mTotalJpegCallbackNum <= 2 || CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
                jpegPictureCallback = new JpegPictureCallback(loc);
            } else {
                jpegPictureCallback = new JpegQuickPictureCallback(loc);
            }
            cameraProxy.takePicture(shutterCallback, pictureCallback, pictureCallback2, jpegPictureCallback);
        }
    }

    protected class JpegPictureCallback implements PictureCallback {
        protected Location mLocation;
        private final boolean mPortraitMode = CameraSettings.isSwitchOn("pref_camera_portrait_mode_key");
        private final boolean mZSLEnabled;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
            this.mZSLEnabled = CameraModule.this.mIsZSLMode;
        }

        public void setLocation(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] jpegData, Camera camera) {
            if (!CameraModule.this.mPaused) {
                CameraModule cameraModule = CameraModule.this;
                cameraModule.mReceivedJpegCallbackNum++;
                CameraModule.this.mJpegPictureCallbackTime = System.currentTimeMillis();
                if (CameraModule.this.mPostViewPictureCallbackTime != 0) {
                    CameraModule.this.mPictureDisplayedToJpegCallbackTime = CameraModule.this.mJpegPictureCallbackTime - CameraModule.this.mPostViewPictureCallbackTime;
                } else if (CameraModule.this.mRawPictureCallbackTime != 0) {
                    CameraModule.this.mPictureDisplayedToJpegCallbackTime = CameraModule.this.mJpegPictureCallbackTime - CameraModule.this.mRawPictureCallbackTime;
                } else {
                    CameraModule.this.mPictureDisplayedToJpegCallbackTime = CameraModule.this.mJpegPictureCallbackTime - CameraModule.this.mShutterCallbackTime;
                }
                Log.v("Camera", "mPictureDisplayedToJpegCallbackTime = " + CameraModule.this.mPictureDisplayedToJpegCallbackTime + "ms");
                CameraModule.this.mFocusManager.onShutter();
                if ((CameraModule.this.mReceivedJpegCallbackNum >= CameraModule.this.mTotalJpegCallbackNum || CameraModule.this.isGroupShotCapture()) && jpegData != null) {
                    int width;
                    int height;
                    Object obj = (Device.isHDRFreeze() && CameraModule.this.mMutexModePicker.isMorphoHdr() && !CameraModule.this.mIsImageCaptureIntent) ? 1 : null;
                    if (obj == null && CameraModule.this.mReceivedJpegCallbackNum == CameraModule.this.mTotalJpegCallbackNum) {
                        CameraModule.this.updateMutexModeUI(true);
                        if (!CameraModule.this.playAnimationBeforeCapture()) {
                            CameraModule.this.playSound(0);
                            CameraModule.this.animateSlide();
                        }
                    }
                    Size s = CameraModule.this.mParameters.getPictureSize();
                    int orientation = Exif.getOrientation(jpegData);
                    if ((CameraModule.this.mJpegRotation + orientation) % 180 == 0) {
                        width = s.width;
                        height = s.height;
                    } else {
                        width = s.height;
                        height = s.width;
                    }
                    CameraModule.this.mBurstShotTitle = Util.createJpegName(System.currentTimeMillis()) + CameraModule.this.getSuffix();
                    String title = CameraModule.this.mBurstShotTitle;
                    DrawJPEGAttribute drawJPEGAttribute = null;
                    if (EffectController.getInstance().hasEffect()) {
                        int max;
                        int max2;
                        if (width > height) {
                            max = Math.max(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight);
                        } else {
                            max = Math.min(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight);
                        }
                        if (height > width) {
                            max2 = Math.max(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight);
                        } else {
                            max2 = Math.min(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight);
                        }
                        int effect = EffectController.getInstance().getEffect(false);
                        EffectRectAttribute copyEffectRectAttribute = EffectController.getInstance().copyEffectRectAttribute();
                        Location location = this.mLocation == null ? null : new Location(this.mLocation);
                        long currentTimeMillis = System.currentTimeMillis();
                        int shootOrientation = Util.getShootOrientation(CameraModule.this.mActivity, CameraModule.this.mShootOrientation);
                        float shootRotation = (EffectController.sGradienterIndex == EffectController.getInstance().getEffect(false) && CameraModule.this.mShootRotation == -1.0f) ? 0.0f : Util.getShootRotation(CameraModule.this.mActivity, CameraModule.this.mShootRotation);
                        boolean z = CameraModule.this.isFrontCamera() && !CameraModule.sProxy.isFrontMirror(CameraModule.this.mParameters);
                        drawJPEGAttribute = new DrawJPEGAttribute(jpegData, max, max2, width, height, effect, copyEffectRectAttribute, location, title, currentTimeMillis, shootOrientation, orientation, shootRotation, z, this.mPortraitMode);
                    }
                    CameraModule.this.trackPictureTaken(1, false, width, height, this.mLocation != null);
                    if (CameraModule.this.mIsImageCaptureIntent) {
                        if (drawJPEGAttribute != null) {
                            CameraModule.this.mEffectProcessor.processorJpegSync(drawJPEGAttribute);
                            CameraModule.this.mJpegImageData = drawJPEGAttribute.mData;
                        } else {
                            CameraModule.this.mJpegImageData = jpegData;
                        }
                        if (CameraModule.this.needReturnInvisibleWatermark()) {
                            String cameraUUID = CameraModule.this.getCameraUUID();
                            if (TextUtils.isEmpty(cameraUUID)) {
                                cameraUUID = "no-fusion-id!";
                            }
                            CameraModule.this.mCameraUUIDWatermarkImageData = Util.sealInvisibleWatermark(CameraModule.this.mJpegImageData, 518400, CameraModule.buildWaterMarkForCameraUUID(cameraUUID));
                        }
                        if (CameraModule.this.mQuickCapture) {
                            CameraModule.this.doAttach();
                        } else {
                            Bitmap cover = Thumbnail.createBitmap(CameraModule.this.mJpegImageData, ((360 - CameraModule.this.mShootOrientation) + orientation) + CameraModule.this.mDisplayRotation, false, Integer.highestOneBit((int) Math.floor(((double) width) / ((double) CameraModule.this.mPreviewHeight))));
                            if (cover != null) {
                                CameraModule.this.mActivity.getCameraScreenNail().renderBitmapToCanvas(cover);
                                CameraModule.this.showPostCaptureAlert();
                                CameraModule.this.mKeepBitmapTexture = true;
                            }
                        }
                    } else {
                        if (drawJPEGAttribute != null) {
                            if (CameraModule.this.mEffectProcessor.processorJpegAsync(drawJPEGAttribute)) {
                                CameraModule.this.mLastIsEffect = true;
                            } else {
                                CameraModule.this.mBurstShotTitle = null;
                            }
                        } else if (CameraModule.this.isGroupShotCapture()) {
                            int result = CameraModule.this.mGroupShot.attach(jpegData);
                            com.android.camera.Log.v("Camera", String.format("mGroupShot attach() = 0x%08x index=%d", new Object[]{Integer.valueOf(result), Integer.valueOf(CameraModule.this.mReceivedJpegCallbackNum)}));
                            if (CameraModule.this.mReceivedJpegCallbackNum < CameraModule.this.mTotalJpegCallbackNum) {
                                if (CameraModule.this.mReceivedJpegCallbackNum == 1 && CameraModule.this.mPreferences.getBoolean("pref_groupshot_with_primitive_picture_key", true)) {
                                    CameraModule.this.mActivity.getImageSaver().addImage(jpegData, title, System.currentTimeMillis(), null, this.mLocation, width, height, null, orientation, false, false, true);
                                }
                                if (CameraModule.this.needSetupPreview(this.mZSLEnabled)) {
                                    CameraModule.this.mCameraDevice.startPreview();
                                }
                                int delay = (!CameraSettings.isFrontCamera() || CameraModule.this.getString(R.string.pref_face_beauty_close).equals(CameraSettings.getFaceBeautifyValue())) ? 100 : 0;
                                CameraModule.this.mHandler.sendEmptyMessageDelayed(30, (long) delay);
                                return;
                            }
                            new SaveOutputImageTask(System.currentTimeMillis(), this.mLocation, width, height, orientation, title, CameraModule.this.mGroupShot).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new Void[0]);
                            cameraModule = CameraModule.this;
                            cameraModule.mGroupShotTimes = cameraModule.mGroupShotTimes + 1;
                        } else {
                            CameraModule.this.mActivity.getImageSaver().addImage(jpegData, title, null, System.currentTimeMillis(), null, this.mLocation, width, height, null, orientation, false, false, true, this.mPortraitMode);
                        }
                        if (!(Device.isSupportedStereo() && CameraSettings.isSwitchOn("pref_camera_stereo_mode_key"))) {
                            CameraModule.this.setupPreview(this.mZSLEnabled);
                        }
                    }
                    long now = System.currentTimeMillis();
                    CameraModule.this.mJpegCallbackFinishTime = now - CameraModule.this.mJpegPictureCallbackTime;
                    Log.v("Camera", "mJpegCallbackFinishTime = " + CameraModule.this.mJpegCallbackFinishTime + "ms");
                    CameraModule.this.mCaptureStartTime = now - CameraModule.this.mCaptureStartTime;
                    Log.d("Camera", "mCaptureStartTime(from onShutterButtonClick start to jpegCallback finished) = " + CameraModule.this.mCaptureStartTime + "ms");
                    if (!CameraModule.this.mHandler.hasMessages(24)) {
                        CameraModule.this.mHandler.sendEmptyMessage(27);
                    }
                }
            }
        }
    }

    private interface PreviewTextureCopiedCallback {
        void onPreviewTextureCopied();
    }

    private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback {
        private AutoFocusCallback() {
        }

        public void onAutoFocus(boolean focused, Camera camera) {
            if (!CameraModule.this.mPaused && !CameraModule.this.mActivity.getCameraScreenNail().isModuleSwitching()) {
                CameraModule.this.mAutoFocusTime = System.currentTimeMillis() - CameraModule.this.mFocusStartTime;
                Log.v("Camera", "mAutoFocusTime = " + CameraModule.this.mAutoFocusTime + "ms" + " focused=" + focused);
                if (!(CameraModule.this.mFocusManager.isFocusingSnapOnFinish() || CameraModule.this.mCameraState == 3)) {
                    CameraModule.this.setCameraState(1);
                }
                CameraModule.this.mFocusManager.onAutoFocus(focused);
                CameraModule.this.mActivity.getSensorStateManager().reset();
            }
        }
    }

    private final class AutoFocusMoveCallback implements android.hardware.Camera.AutoFocusMoveCallback {
        private AutoFocusMoveCallback() {
        }

        public void onAutoFocusMoving(boolean moving, Camera camera) {
            if (!CameraModule.this.mPaused) {
                Log.v("Camera", "onAutoFocusMoving moving=" + moving + " " + CameraModule.this.mCameraState);
                CameraModule.this.getUIController().getFocusView().setFocusType(false);
                boolean isFocusSuccessful = moving ? false : CameraModule.this.mCameraDevice.isFocusSuccessful();
                boolean requestScan = false;
                String logContent = null;
                if (moving) {
                    logContent = "onAutoFocusMoving start";
                    CameraModule.this.mAFEndLogTimes = 0;
                } else if (CameraModule.this.mAFEndLogTimes == 0) {
                    logContent = "onAutoFocusMoving end. result=" + isFocusSuccessful;
                    requestScan = true;
                    CameraModule cameraModule = CameraModule.this;
                    cameraModule.mAFEndLogTimes = cameraModule.mAFEndLogTimes + 1;
                }
                if (Util.sIsDumpLog && logContent != null) {
                    Log.v("Camera", logContent);
                }
                if ((CameraModule.this.mCameraState != 3 || CameraModule.this.mHandler.hasMessages(36)) && !CameraModule.this.mActivity.getCameraScreenNail().isModuleSwitching()) {
                    CameraModule.this.mFocusManager.onAutoFocusMoving(moving, isFocusSuccessful);
                }
                if (requestScan) {
                    QRCodeManager.instance(CameraModule.this.mActivity).requestDecode();
                }
            }
        }
    }

    private class BurstSpeedController {
        private long mBurstStartTime;

        private BurstSpeedController() {
        }

        private void onShutter() {
            if (this.mBurstStartTime == 0 && CameraModule.this.isLongShotMode()) {
                this.mBurstStartTime = System.currentTimeMillis();
            }
            if (Device.isQcomPlatform() && !Device.IS_MI2 && !Device.IS_MI2A && CameraModule.this.isLongShotMode()) {
                if (!CameraModule.this.mActivity.getImageSaver().isNeedStopCapture() || CameraModule.this.mReceivedJpegCallbackNum < 1) {
                    int delay = CameraModule.this.mActivity.getImageSaver().getBurstDelay();
                    if (delay == 0) {
                        CameraModule.this.sendBurstCommand();
                        return;
                    } else {
                        CameraModule.this.mHandler.sendEmptyMessageDelayed(30, (long) delay);
                        return;
                    }
                }
                CameraModule.this.onShutterButtonFocus(false, 2);
            }
        }

        public void capture() {
            this.mBurstStartTime = 0;
        }

        private void onPictureTaken() {
            if (!Device.isMTKPlatform()) {
                return;
            }
            if (CameraModule.this.mActivity.getImageSaver().isNeedStopCapture() && CameraModule.this.mReceivedJpegCallbackNum >= 1) {
                CameraModule.this.onShutterButtonFocus(false, 2);
            } else if (CameraModule.this.mActivity.getImageSaver().isNeedSlowDown()) {
                int suitableSpeed = (int) (((float) getBurstSpeed()) * CameraModule.this.mActivity.getImageSaver().getSuitableBurstShotSpeed());
                if (suitableSpeed == 0) {
                    suitableSpeed = 1;
                    Log.d("Camera", "current performance is very poor, will set the speed = 1 to native ");
                }
                if (Util.sIsDumpLog) {
                    Log.v("Camera", "set BurstShotSpeed to " + suitableSpeed + " fps");
                }
                CameraModule.this.mCameraDevice.setBurstShotSpeed(suitableSpeed);
            }
        }

        private int getBurstSpeed() {
            long timeduration = System.currentTimeMillis() - this.mBurstStartTime;
            int speed = 0;
            if (CameraModule.this.mReceivedJpegCallbackNum > 0 && timeduration != 0) {
                speed = (int) (((long) (CameraModule.this.mReceivedJpegCallbackNum * 1000)) / timeduration);
            }
            if (Util.sIsDumpLog) {
                Log.v("Camera", "current burst Speed is " + speed + " fps");
            }
            return speed;
        }
    }

    private class CameraStartUpThread extends Thread {
        private volatile boolean mCancelled;

        private CameraStartUpThread() {
        }

        public void cancel() {
            this.mCancelled = true;
        }

        public void run() {
            try {
                if (!this.mCancelled) {
                    CameraDataAnalytics.instance().trackEventTime("open_camera_times_key");
                    CameraModule.this.prepareOpenCamera();
                    CameraModule.this.mCameraDevice = Util.openCamera(CameraModule.this.mActivity, CameraModule.this.mCameraId);
                    CameraModule.this.mCameraDevice.setHardwareListener(CameraModule.this);
                    if (!this.mCancelled) {
                        Log.v("Camera", "CameraStartUpThread mCameraDevice=" + CameraModule.this.mCameraDevice + " " + CameraModule.this);
                        CameraModule.this.mParameters = CameraModule.this.mCameraDevice.getParameters();
                        CameraModule.this.initializeCapabilities();
                        if (CameraModule.this.mInitialParams == null || CameraModule.this.mParameters == null) {
                            throw new CameraHardwareException(new Exception("Failed to get parameters"));
                        }
                        CameraModule.this.mStartPreviewPrerequisiteReady.block();
                        if (CameraModule.this.mFocusManager == null) {
                            CameraModule.this.initializeFocusManager();
                        }
                        if (!this.mCancelled) {
                            CameraModule.this.setDisplayOrientation();
                            CameraModule.this.setCameraParameters(-1);
                            CameraModule.this.mHandler.sendEmptyMessage(8);
                            if (!this.mCancelled) {
                                if (CameraModule.this.mIsImageCaptureIntent && CameraModule.this.getUIController().getReviewDoneView().getVisibility() == 0) {
                                    CameraModule.this.mHandler.sendEmptyMessageDelayed(25, 30);
                                } else {
                                    CameraModule.this.startPreview();
                                }
                                CameraModule.this.onCameraStartPreview();
                                CameraModule.this.mHandler.sendEmptyMessage(9);
                                CameraModule.this.mOnResumeTime = SystemClock.uptimeMillis();
                                CameraModule.this.mHandler.sendEmptyMessage(4);
                                CameraModule.this.mHandler.sendEmptyMessage(31);
                                Log.v("Camera", "CameraStartUpThread done");
                            }
                        }
                    }
                }
            } catch (CameraHardwareException e) {
                CameraModule.this.mCameraStartUpThread = null;
                CameraModule.this.mOpenCameraFail = true;
                CameraModule.this.mHandler.sendEmptyMessage(10);
            } catch (CameraDisabledException e2) {
                CameraModule.this.mCameraStartUpThread = null;
                CameraModule.this.mCameraDisabled = true;
                CameraModule.this.mHandler.sendEmptyMessage(10);
            }
        }
    }

    private final class JpegQuickPictureCallback implements PictureCallback {
        Location mLocation;
        String mPressDownTitle;
        private final boolean mZSLEnabled;

        public JpegQuickPictureCallback(Location loc) {
            this.mLocation = loc;
            this.mZSLEnabled = CameraModule.this.mIsZSLMode;
        }

        private String getBurstShotTitle() {
            String str;
            if (CameraModule.this.mUpdateImageTitle && CameraModule.this.mBurstShotTitle != null && CameraModule.this.mReceivedJpegCallbackNum == 1) {
                this.mPressDownTitle = CameraModule.this.mBurstShotTitle;
                CameraModule.this.mBurstShotTitle = null;
            }
            if (CameraModule.this.mBurstShotTitle == null) {
                long currentTime = System.currentTimeMillis();
                CameraModule.this.mBurstShotTitle = Util.createJpegName(currentTime);
                if (CameraModule.this.mBurstShotTitle.length() != 19) {
                    CameraModule.this.mBurstShotTitle = Util.createJpegName(1000 + currentTime);
                }
            }
            StringBuilder append = new StringBuilder().append(CameraModule.this.mBurstShotTitle);
            if (CameraModule.this.mMutexModePicker.isUbiFocus()) {
                str = "_UBIFOCUS_" + (CameraModule.this.mReceivedJpegCallbackNum - 1);
            } else {
                str = "_BURST" + CameraModule.this.mReceivedJpegCallbackNum;
            }
            return append.append(str).toString();
        }

        public void onPictureTaken(byte[] jpegData, Camera camera) {
            if (!CameraModule.this.mPaused && jpegData != null && CameraModule.this.mReceivedJpegCallbackNum < CameraModule.this.mTotalJpegCallbackNum && (CameraModule.this.mMutexModePicker.isUbiFocus() || CameraModule.this.isLongShotMode())) {
                if (CameraModule.this.mReceivedJpegCallbackNum == 1 && !CameraModule.this.mMultiSnapStopRequest) {
                    CameraModule.this.mFocusManager.onShutter();
                    if (!CameraModule.this.mMutexModePicker.isUbiFocus() && CameraModule.this.mUpdateImageTitle) {
                        if (CameraModule.this.mLastIsEffect) {
                            CameraModule.this.mEffectProcessor.changeJpegTitle(getBurstShotTitle(), this.mPressDownTitle);
                        } else {
                            CameraModule.this.mActivity.getImageSaver().updateImage(getBurstShotTitle(), this.mPressDownTitle);
                        }
                    }
                }
                CameraModule cameraModule;
                if (Storage.isLowStorageAtLastPoint()) {
                    if (!CameraModule.this.mMutexModePicker.isUbiFocus() && CameraModule.this.mMultiSnapStatus) {
                        cameraModule = CameraModule.this;
                        int i = CameraModule.this.mReceivedJpegCallbackNum;
                        int i2 = CameraModule.this.mMultiSnapPictureSize.width;
                        int i3 = CameraModule.this.mMultiSnapPictureSize.height;
                        boolean z = this.mLocation != null ? this.mLocation.getLatitude() == 0.0d ? this.mLocation.getLongitude() != 0.0d : true : false;
                        cameraModule.trackPictureTaken(i, true, i2, i3, z);
                        CameraModule.this.stopMultiSnap();
                    }
                    return;
                }
                int width;
                int height;
                if (!CameraModule.this.mMutexModePicker.isUbiFocus()) {
                    CameraModule.this.playSound(4);
                }
                cameraModule = CameraModule.this;
                cameraModule.mReceivedJpegCallbackNum++;
                CameraModule.this.getUIController().getMultiSnapNum().setText("" + CameraModule.this.mReceivedJpegCallbackNum);
                boolean isHide = CameraModule.this.mMutexModePicker.isUbiFocus() && CameraModule.this.mReceivedJpegCallbackNum <= CameraModule.this.mTotalJpegCallbackNum;
                int orientation = isHide ? 0 : Exif.getOrientation(jpegData);
                if ((CameraModule.this.mJpegRotation + orientation) % 180 == 0) {
                    width = CameraModule.this.mMultiSnapPictureSize.width;
                    height = CameraModule.this.mMultiSnapPictureSize.height;
                } else {
                    width = CameraModule.this.mMultiSnapPictureSize.height;
                    height = CameraModule.this.mMultiSnapPictureSize.width;
                }
                String title = getBurstShotTitle();
                boolean isMap = CameraModule.this.mMutexModePicker.isUbiFocus() && CameraModule.this.mReceivedJpegCallbackNum == CameraModule.this.mTotalJpegCallbackNum - 1;
                Object obj = (CameraModule.this.mMutexModePicker.isUbiFocus() && CameraModule.this.mReceivedJpegCallbackNum == CameraModule.this.mTotalJpegCallbackNum) ? 1 : null;
                if (obj == null) {
                    CameraModule.this.mActivity.getImageSaver().addImage(jpegData, title, System.currentTimeMillis(), (Uri) null, this.mLocation, width, height, null, orientation, isHide, isMap, true);
                }
                if (CameraModule.this.mReceivedJpegCallbackNum >= CameraModule.this.mTotalJpegCallbackNum || CameraModule.this.mMultiSnapStopRequest) {
                    boolean z2;
                    CameraModule.this.mCaptureStartTime = System.currentTimeMillis() - CameraModule.this.mCaptureStartTime;
                    if (CameraModule.this.mMutexModePicker.isUbiFocus()) {
                        CameraModule.this.updateMutexModeUI(true);
                        CameraModule.this.setupPreview(this.mZSLEnabled);
                    } else {
                        CameraModule.this.stopMultiSnap();
                    }
                    CameraModule cameraModule2 = CameraModule.this;
                    int i4 = !CameraModule.this.mMutexModePicker.isUbiFocus() ? CameraModule.this.mReceivedJpegCallbackNum : 1;
                    boolean z3 = !CameraModule.this.mMutexModePicker.isUbiFocus();
                    if (this.mLocation != null) {
                        z2 = true;
                    } else {
                        z2 = false;
                    }
                    cameraModule2.trackPictureTaken(i4, z3, width, height, z2);
                    Log.d("Camera", "Burst shooting finished. Total:" + CameraModule.this.mReceivedJpegCallbackNum + "pictures, " + "cost consuming:" + CameraModule.this.mCaptureStartTime + "ms");
                } else if (CameraModule.this.mMutexModePicker.isUbiFocus() && isMap && !Util.isProduceFocusInfoSuccess(jpegData)) {
                    CameraModule.this.updateWarningMessage(R.string.ubi_focus_capture_fail, false);
                } else {
                    CameraModule.this.mBurstSpeedController.onPictureTaken();
                }
            }
        }
    }

    private final class JpegQuickShutterCallback implements android.hardware.Camera.ShutterCallback {
        private JpegQuickShutterCallback() {
        }

        public void onShutter() {
            CameraModule.this.mShutterCallbackTime = System.currentTimeMillis();
            CameraModule.this.mShutterLag = CameraModule.this.mShutterCallbackTime - CameraModule.this.mCaptureStartTime;
            Log.v("Camera", "mShutterLag = " + CameraModule.this.mShutterLag + "ms");
            CameraModule.this.mBurstSpeedController.onShutter();
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            switch (msg.what) {
                case 1:
                    CameraModule.this.initializeFirstTime();
                    return;
                case 2:
                    CameraModule.this.getWindow().clearFlags(128);
                    return;
                case 3:
                    CameraModule.this.setCameraParametersWhenIdle(0);
                    return;
                case 4:
                    CameraModule.this.checkActivityOrientation();
                    if (SystemClock.uptimeMillis() - CameraModule.this.mOnResumeTime < 5000) {
                        CameraModule.this.mHandler.sendEmptyMessageDelayed(4, 100);
                        return;
                    }
                    return;
                case 5:
                    CameraModule.this.mIgnoreFocusChanged = true;
                    CameraModule.this.mActivity.getScreenHint().showFirstUseHint();
                    return;
                case 6:
                    CameraModule.this.switchCamera();
                    return;
                case 7:
                    CameraModule.this.mActivity.getCameraScreenNail().animateSwitchCameraBefore();
                    return;
                case 8:
                    CameraModule.this.initializeAfterCameraOpen();
                    return;
                case 9:
                    CameraModule.this.mCameraStartUpThread = null;
                    CameraModule.this.mActivity.getCameraScreenNail().animateModuleChangeAfter();
                    CameraModule.this.getUIController().onCameraOpen();
                    CameraModule.this.mHandler.sendEmptyMessageDelayed(22, 100);
                    CameraModule.this.getUIController().getFocusView().initialize(CameraModule.this);
                    CameraModule.this.getUIController().getObjectView().setObjectViewListener(CameraModule.this);
                    CameraModule.this.updateModePreference();
                    if ((CameraModule.this.mCameraState == 0 || 1 == CameraModule.this.mCameraState) && !(CameraModule.this.mIsImageCaptureIntent && CameraModule.this.getUIController().getReviewDoneView().getVisibility() == 0)) {
                        CameraModule.this.setCameraState(1);
                    }
                    CameraModule.this.onSettingsBack();
                    CameraModule.this.startFaceDetection();
                    CameraModule.this.takeAPhotoIfNeeded();
                    return;
                case 10:
                    CameraModule.this.onCameraException();
                    return;
                case 12:
                    if (CameraModule.this.mCameraState == 3 || CameraModule.this.mFocusManager.isFocusingSnapOnFinish()) {
                        CameraModule.this.mPendingMultiCapture = true;
                        return;
                    } else if (CameraModule.this.mCameraState == 3) {
                        return;
                    } else {
                        if (!Device.isHDRFreeze() || Util.isTimeout(System.currentTimeMillis(), CameraModule.this.mLastFreezeHDRTime, 800)) {
                            if (!CameraModule.this.mMutexModePicker.isNormal()) {
                                CameraModule.this.mMutexModePicker.resetMutexMode();
                            }
                            CameraModule.this.mFocusManager.doMultiSnap(true);
                            return;
                        }
                        return;
                    }
                case 15:
                    if (CameraModule.this.isShutterButtonClickable()) {
                        CameraModule.this.onShutterButtonFocus(true, 3);
                        CameraModule.this.onShutterButtonClick();
                        CameraModule.this.onShutterButtonFocus(false, 0);
                        return;
                    } else if (CameraModule.this.mDoCaptureRetry < 20) {
                        CameraModule cameraModule = CameraModule.this;
                        cameraModule.mDoCaptureRetry = cameraModule.mDoCaptureRetry + 1;
                        Log.d("Camera", "retry do-capture: " + CameraModule.this.mDoCaptureRetry);
                        CameraModule.this.mHandler.sendEmptyMessageDelayed(15, 200);
                        return;
                    } else {
                        return;
                    }
                case 17:
                    CameraModule.this.mHandler.removeMessages(17);
                    CameraModule.this.mHandler.removeMessages(2);
                    CameraModule.this.getWindow().addFlags(128);
                    CameraModule.this.mHandler.sendEmptyMessageDelayed(2, (long) CameraModule.this.getScreenDelay());
                    return;
                case 18:
                    CameraSettings.changeUIByPreviewSize(CameraModule.this.mActivity, CameraModule.this.mUIStyle);
                    CameraModule.this.changePreviewSurfaceSize();
                    return;
                case 19:
                    if (CameraModule.this.mHasPendingSwitching) {
                        CameraModule.this.updateCameraScreenNailSize(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight, CameraModule.this.mFocusManager);
                        CameraSettings.changeUIByPreviewSize(CameraModule.this.mActivity, CameraModule.this.mUIStyle);
                        CameraModule.this.changePreviewSurfaceSize();
                        CameraModule.this.mHasPendingSwitching = false;
                    } else if (CameraModule.this.isSquareModeChange()) {
                        CameraModule.this.updateCameraScreenNailSize(CameraModule.this.mPreviewWidth, CameraModule.this.mPreviewHeight, CameraModule.this.mFocusManager);
                    }
                    CameraModule.this.mActivity.getCameraScreenNail().switchCameraDone();
                    CameraModule.this.mSwitchingCamera = false;
                    CameraModule.this.mSwitchCameraAnimationRunning = false;
                    return;
                case 20:
                    if (msg.arg1 > 0) {
                        Message message = obtainMessage(20);
                        int i = msg.arg1 - 1;
                        msg.arg1 = i;
                        message.arg1 = i;
                        message.arg2 = msg.arg2;
                        CameraModule.this.mAudioCaptureManager.setDelayStep(message.arg1);
                        sendMessageDelayed(message, (long) message.arg2);
                        if (message.arg1 < 3) {
                            CameraModule.this.playSound(5);
                            return;
                        }
                        return;
                    }
                    CameraModule.this.mAudioCaptureManager.hideDelayNumber();
                    CameraModule.this.sendDoCaptureMessage(1);
                    CameraModule.this.traceDelayCaptureEvents();
                    return;
                case 21:
                    CameraModule.this.updateWarningMessage(0, true);
                    return;
                case 22:
                    if (Device.isMDPRender()) {
                        CameraModule.this.getUIController().getSurfaceViewFrame().setSurfaceViewVisible(false);
                    }
                    if (CameraModule.this.getUIController().getSettingPage().getVisibility() != 0) {
                        CameraModule.this.mActivity.setBlurFlag(false);
                        return;
                    }
                    return;
                case 23:
                    CameraModule.this.mActivity.getScreenHint().showObjectTrackHint(CameraModule.this.mPreferences);
                    return;
                case 24:
                    if (Device.isHDRFreeze() && CameraModule.this.mMutexModePicker.isMorphoHdr()) {
                        CameraModule.this.updateMutexModeUI(true);
                        if (!CameraModule.this.playAnimationBeforeCapture()) {
                            CameraModule.this.playSound(0);
                            CameraModule.this.animateSlide();
                        }
                        CameraModule.this.setCameraState(msg.arg1);
                        CameraModule.this.startFaceDetection();
                        CameraModule.this.mLastFreezeHDRTime = System.currentTimeMillis();
                        return;
                    }
                    return;
                case 25:
                    if (CameraModule.this.getUIController().getGLView().isBusy()) {
                        CameraModule.this.mHandler.sendEmptyMessageDelayed(25, 30);
                        return;
                    } else {
                        CameraModule.this.getUIController().getGLView().requestRender();
                        return;
                    }
                case 27:
                    if (CameraModule.this.mPendingMultiCapture) {
                        CameraModule.this.mPendingMultiCapture = false;
                        if (!CameraModule.this.mMutexModePicker.isNormal()) {
                            CameraModule.this.mMutexModePicker.resetMutexMode();
                        }
                        CameraModule.this.mFocusManager.doMultiSnap(true);
                        return;
                    }
                    return;
                case 28:
                    CameraModule.this.enableCameraControls(true);
                    return;
                case 29:
                    if (!CameraModule.this.mPaused) {
                        CameraModule.this.playSound(7);
                        CameraModule.this.mAudioCaptureManager.onClick();
                        return;
                    }
                    return;
                case 30:
                    if (CameraModule.this.isGroupShotCapture()) {
                        CameraModule.this.mCameraDevice.takePicture(null, null, null, new JpegPictureCallback(LocationManager.instance().getCurrentLocation()));
                        return;
                    } else {
                        CameraModule.this.sendBurstCommand();
                        return;
                    }
                case 31:
                    CameraModule.this.setOrientationParameter();
                    return;
                case 32:
                    CameraModule.this.applyPreferenceChange();
                    return;
                case 33:
                    CameraModule.this.setOrientation(msg.arg1);
                    return;
                case 34:
                    boolean enterFBMode;
                    if (msg.arg1 > 0) {
                        enterFBMode = true;
                    } else {
                        enterFBMode = false;
                    }
                    if (enterFBMode) {
                        if (msg.obj != null && (msg.obj instanceof FaceDetectionListener)) {
                            CameraModule.this.mCameraDevice.setFaceDetectionListener(msg.obj);
                        }
                        CameraModule.this.updateFaceView(true, true);
                        return;
                    } else if (CameraModule.this.mFaceDetectionEnabled) {
                        CameraModule.this.startFaceDetection();
                        return;
                    } else {
                        CameraModule.this.updateFaceView(false, true);
                        return;
                    }
                case 35:
                    CameraModule cameraModule2 = CameraModule.this;
                    boolean z2 = msg.arg1 > 0;
                    if (msg.arg2 <= 0) {
                        z = false;
                    }
                    cameraModule2.handleUpdateFaceView(z2, z);
                    return;
                case 36:
                    CameraModule.this.setCameraState(1);
                    CameraModule.this.startFaceDetection();
                    return;
                case 37:
                    Log.e("Camera", "No continuous shot callback!", new RuntimeException());
                    CameraModule.this.handleMultiSnapDone();
                    return;
                case 39:
                    if (CameraModule.this.getUIController().getReviewDoneView().getVisibility() != 0) {
                        CameraModule.this.getUIController().getOrientationIndicator().updateVisible(((Boolean) msg.obj).booleanValue());
                        return;
                    }
                    return;
                case 40:
                    if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                        CameraModule.this.updateWarningMessage(R.string.dual_camera_use_hint, false);
                        return;
                    }
                    return;
                case 41:
                    if (CameraSettings.isSupportedOpticalZoom()) {
                        CameraModule.this.onCameraPickerClicked(CameraHolder.instance().getBackCameraId());
                        return;
                    }
                    return;
                case 42:
                    CameraModule.this.getUIController().updateThumbnailView(msg.obj);
                    return;
                case 43:
                    CameraModule.this.enableCameraControls(true);
                    return;
                case 44:
                    int countDownTime = CameraSettings.getCountDownTimes();
                    CameraModule.this.sendDelayedCaptureMessage(1000, countDownTime);
                    if (countDownTime > 3) {
                        CameraModule.this.playSound(7);
                    }
                    CameraModule.this.mIsCountDown = true;
                    return;
                default:
                    return;
            }
        }
    }

    protected class MetaDataManager {
        private int mCurrentScene = -1;
        private int mLastScene = -1;
        private int mLastestState = -1;
        private int mLastestTimes = 0;
        private int mMetaType = 0;
        private int mSceneShieldMask = 255;

        public void setAsdDetectMask(java.lang.String r1) {
            /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.android.camera.module.CameraModule.MetaDataManager.setAsdDetectMask(java.lang.String):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:256)
	at jadx.core.ProcessClass.process(ProcessClass.java:34)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 7 more
*/
            /*
            // Can't load method instructions.
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.CameraModule.MetaDataManager.setAsdDetectMask(java.lang.String):void");
        }

        public void setType(int type) {
            if (this.mMetaType != type) {
                reset();
            }
            this.mMetaType = type;
        }

        public void resetFilter() {
            setAsdDetectMask("pref_camera_flashmode_key");
            setAsdDetectMask("pref_camera_hdr_key");
            setAsdDetectMask("pref_camera_asd_night_key");
            setAsdDetectMask("pref_camera_asd_motion_key");
        }

        private void resetSceneMode() {
            if (CameraModule.this.currentIsMainThread()) {
                restoreScene(this.mCurrentScene);
            }
            this.mLastScene = -1;
            this.mCurrentScene = -1;
            this.mLastestState = -1;
            this.mLastestTimes = 0;
        }

        public void reset() {
            resetFilter();
            resetSceneMode();
        }

        private void printMetaData(byte[] data) {
            String format;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                sb.append(String.format(Locale.ENGLISH, "%02x", new Object[]{Byte.valueOf(data[i])}));
            }
            sb.append("  data[8]=");
            if (data.length > 8) {
                format = String.format(Locale.ENGLISH, "%02x", new Object[]{Byte.valueOf(data[8])});
            } else {
                format = "not exist";
            }
            sb.append(format);
            sb.append("  mSceneShieldMask=").append(String.format(Locale.ENGLISH, "%02x", new Object[]{Integer.valueOf(this.mSceneShieldMask)}));
            sb.append("  result=").append(String.format(Locale.ENGLISH, "%02x", new Object[]{Integer.valueOf(data[8] & this.mSceneShieldMask)}));
            Log.v("CameraMetaDataManager", "onCameraMetaData buffer=" + sb.toString());
        }

        public void setData(byte[] data) {
            if (Util.sIsDumpLog) {
                printMetaData(data);
            }
            if (data.length >= 9) {
                if (this.mMetaType == 5 && data[0] == (byte) 5) {
                    filterScene(detectRTBScene(data[8]));
                } else if (this.mMetaType == 3) {
                    filterScene(detectASDScene(data[8]));
                }
            }
        }

        private void filterScene(int currentDetect) {
            if (setScene(currentDetect)) {
                restoreScene(this.mLastScene);
                this.mLastScene = -1;
                applyScene(this.mCurrentScene);
            }
        }

        private int detectASDScene(int senceResult) {
            senceResult &= this.mSceneShieldMask;
            if (Device.isSupportedAsdFlash() && "auto".equals(CameraModule.this.mParameters.getFlashMode()) && (senceResult & 1) != 0) {
                return 0;
            }
            if (!CameraModule.this.getUIController().getSettingPage().isItemSelected()) {
                if (Device.isSupportedAsdHdr() && CameraModule.this.mParameters.getZoom() == 0 && !"torch".equals(CameraModule.this.mParameters.getFlashMode()) && (senceResult & 16) != 0) {
                    return 1;
                }
                if (Device.isSupportedAsdNight() && ((CameraModule.this.mMutexModePicker.isNormal() || CameraModule.this.mMutexModePicker.isHandNight()) && "off".equals(CameraModule.this.mParameters.getFlashMode()) && (senceResult & 64) != 0)) {
                    return 2;
                }
                if (Device.isSupportedAsdMotion() && ((CameraModule.this.mMutexModePicker.isNormal() || CameraModule.this.mMutexModePicker.isHandNight()) && "off".equals(CameraModule.this.mParameters.getFlashMode()) && (senceResult & 32) != 0)) {
                    return 3;
                }
            }
            return -1;
        }

        private int detectRTBScene(int senceResult) {
            int i = -1;
            if (CameraModule.this.isPortraitModeUseHintShowing() || !CameraSettings.isSupportedPortrait() || !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                return -1;
            }
            boolean isHintTextShown = CameraModule.this.getUIController().getPortraitButton().isHintTextShown();
            if (senceResult == 2) {
                if (!isHintTextShown) {
                    i = 4;
                }
                return i;
            } else if (senceResult == 3) {
                if (!isHintTextShown) {
                    i = 5;
                }
                return i;
            } else if (senceResult == 4) {
                if (!isHintTextShown) {
                    i = 6;
                }
                return i;
            } else if (senceResult == 6) {
                return this.mCurrentScene;
            } else {
                return 7;
            }
        }

        private boolean setScene(int scene) {
            if (Util.sIsDumpLog) {
                Log.v("CameraMetaDataManager", "setScene " + scene + " mLastestState=" + this.mLastestState + " mLastestTimes=" + this.mLastestTimes + " mCurrentScene=" + this.mCurrentScene);
            }
            if (this.mLastestState != scene) {
                this.mLastestState = scene;
                this.mLastestTimes = 1;
            } else if (this.mLastestTimes < 3) {
                this.mLastestTimes++;
                if (3 == this.mLastestTimes && this.mCurrentScene != this.mLastestState) {
                    this.mLastScene = this.mCurrentScene;
                    this.mCurrentScene = this.mLastestState;
                    return true;
                }
            }
            return false;
        }

        private void restoreScene(int scene) {
            Log.v("CameraMetaDataManager", "restoreScene " + scene);
            switch (scene) {
                case 0:
                    CameraModule.this.getUIController().getAsdIndicator().setVisibility(8);
                    if (CameraSettings.isAsdPopupEnable()) {
                        CameraModule.this.getUIController().getFlashButton().updatePopup(false);
                        return;
                    }
                    return;
                case 1:
                    if (CameraModule.this.mMutexModePicker.isMorphoHdr() || CameraModule.this.mMutexModePicker.isSceneHdr()) {
                        CameraModule.this.mMutexModePicker.resetMutexMode();
                    }
                    CameraModule.this.getUIController().getAsdIndicator().setVisibility(8);
                    return;
                case 2:
                    if (CameraModule.this.mMutexModePicker.isHandNight()) {
                        CameraModule.this.mMutexModePicker.resetMutexMode();
                        return;
                    }
                    return;
                case 3:
                    if (CameraModule.this.mMutexModePicker.isHandNight()) {
                        CameraModule.this.mMutexModePicker.resetMutexMode();
                        return;
                    }
                    return;
                case 4:
                case 5:
                case 6:
                    CameraModule.this.updateWarningMessage(0, true);
                    return;
                case 7:
                    CameraModule.this.getUIController().getPortraitButton().hideHintText();
                    return;
                default:
                    return;
            }
        }

        private void applyScene(int scene) {
            Log.v("CameraMetaDataManager", "applyScene " + scene);
            switch (scene) {
                case 0:
                    CameraModule.this.getUIController().getAsdIndicator().setImageResource(R.drawable.v6_ic_indicator_asd_flash);
                    CameraModule.this.getUIController().getAsdIndicator().setVisibility(0);
                    if (CameraSettings.isAsdPopupEnable()) {
                        CameraModule.this.getUIController().getFlashButton().updatePopup(true);
                        return;
                    }
                    return;
                case 1:
                    CameraModule.this.mMutexModePicker.setMutexMode(1);
                    CameraModule.this.getUIController().getAsdIndicator().setImageResource(R.drawable.v6_ic_indicator_asd_hdr);
                    CameraModule.this.getUIController().getAsdIndicator().setVisibility(0);
                    return;
                case 2:
                    CameraModule.this.mMutexModePicker.setMutexMode(3);
                    return;
                case 3:
                    CameraModule.this.mMutexModePicker.setMutexMode(3);
                    return;
                case 4:
                    CameraModule.this.updateWarningMessage(R.string.portrait_mode_too_close_hint, false, false);
                    return;
                case 5:
                    CameraModule.this.updateWarningMessage(R.string.portrait_mode_too_far_hint, false, false);
                    return;
                case 6:
                    CameraModule.this.updateWarningMessage(R.string.portrait_mode_lowlight_hint, false, false);
                    return;
                case 7:
                    CameraModule.this.getUIController().getPortraitButton().showHintText();
                    return;
                default:
                    return;
            }
        }
    }

    private final class PostViewPictureCallback implements PictureCallback {
        private PostViewPictureCallback() {
        }

        public void onPictureTaken(byte[] data, Camera camera) {
            CameraModule.this.mPostViewPictureCallbackTime = System.currentTimeMillis();
            Log.v("Camera", "mShutterToPostViewCallbackTime = " + (CameraModule.this.mPostViewPictureCallbackTime - CameraModule.this.mShutterCallbackTime) + "ms");
        }
    }

    private final class RawPictureCallback implements PictureCallback {
        private RawPictureCallback() {
        }

        public void onPictureTaken(byte[] rawData, Camera camera) {
            CameraModule.this.mRawPictureCallbackTime = System.currentTimeMillis();
            Log.v("Camera", "mShutterToRawCallbackTime = " + (CameraModule.this.mRawPictureCallbackTime - CameraModule.this.mShutterCallbackTime) + "ms");
            if (rawData != null) {
                Log.v("Camera", "rawData size = " + rawData.length);
                CameraModule.this.writeImage(rawData, parseDataSizeDNG(rawData));
            }
        }

        protected int parseDataSizeDNG(byte[] data) {
            if (8 > data.length) {
                return 0;
            }
            int size = ((((data[4] & 255) | ((data[5] & 255) << 8)) | ((data[6] & 255) << 16)) | ((data[7] & 255) << 24)) + 318;
            if (size > data.length) {
                return 0;
            }
            Log.e("Camera", "DNG size:" + size);
            return size;
        }
    }

    private class SaveOutputImageTask extends AsyncTask<Void, Integer, Integer> {
        private GroupShot mGroupShotInternal;
        private int mHeight;
        private Location mLocation;
        private int mOrientation;
        private long mTimeTaken;
        private String mTitle;
        private int mWidth;
        private long start_time;

        SaveOutputImageTask(long time, Location location, int width, int height, int orientation, String title, GroupShot groupShot) {
            this.mTimeTaken = time;
            this.mLocation = location;
            this.mWidth = width;
            this.mHeight = height;
            this.mOrientation = orientation;
            this.mTitle = title;
            this.mGroupShotInternal = groupShot;
        }

        protected Integer doInBackground(Void... params) {
            Log.v("Camera", "doInBackground start");
            try {
                int result = this.mGroupShotInternal.attach_end();
                com.android.camera.Log.v("Camera", String.format("attach_end() = 0x%08x", new Object[]{Integer.valueOf(result)}));
                if (isCancelled()) {
                    return null;
                }
                result = this.mGroupShotInternal.setBaseImage(0);
                com.android.camera.Log.v("Camera", String.format("setBaseImage() = 0x%08x", new Object[]{Integer.valueOf(result)}));
                result = this.mGroupShotInternal.setBestFace();
                com.android.camera.Log.v("Camera", "groupshot attach end & setbestface cost " + (System.currentTimeMillis() - this.start_time));
                String path = Storage.generateFilepath(this.mTitle);
                if (Util.sIsDumpLog) {
                    String originPath = path.substring(0, path.lastIndexOf(".jpg"));
                    new File(originPath).mkdirs();
                    CameraModule.this.mGroupShot.saveInputImages(originPath + File.separator);
                }
                if (isCancelled()) {
                    return null;
                }
                this.mGroupShotInternal.getImageAndSaveJpeg(path);
                Log.v("Camera", "groupshot finish group cost " + (System.currentTimeMillis() - this.start_time));
                if (isCancelled()) {
                    return null;
                }
                ExifHelper.writeExif(path, this.mOrientation, LocationManager.instance().getCurrentLocation(), this.mTimeTaken);
                Uri uri = Storage.addImage(CameraModule.this.mActivity, path, this.mOrientation, this.mTimeTaken, this.mLocation, this.mWidth, this.mHeight);
                Log.v("Camera", "groupshot insert db cost " + (System.currentTimeMillis() - this.start_time));
                CameraDataAnalytics.instance().trackEvent("capture_times_group_shot");
                CameraModule.this.mActivity.getScreenHint().updateHint();
                if (uri != null) {
                    CameraModule.this.mActivity.addSecureUri(uri);
                    Thumbnail t = Thumbnail.createThumbnailFromUri(CameraModule.this.mActivity.getContentResolver(), uri, false);
                    Util.broadcastNewPicture(CameraModule.this.mActivity, uri);
                    CameraModule.this.mActivity.getThumbnailUpdater().setThumbnail(t, false);
                }
                Log.v("Camera", "groupshot asynctask cost " + (System.currentTimeMillis() - this.start_time));
                return null;
            } catch (Exception e) {
                Log.e("Camera", "SaveOutputImageTask exception occurs, " + e.getMessage());
                if (null != null) {
                    new File(null).delete();
                }
                return null;
            }
        }

        protected void onPreExecute() {
            this.start_time = System.currentTimeMillis();
        }

        protected void onCancelled() {
            Log.v("Camera", "SaveOutputImageTask onCancelled");
            finishGroupshot();
        }

        protected void onPostExecute(Integer result) {
            Log.v("Camera", "SaveOutputImageTask onPostExecute");
            if (!CameraModule.this.mPaused) {
                CameraModule.this.mActivity.getThumbnailUpdater().updateThumbnailView();
            }
            Log.v("Camera", "groupshot image process cost " + (System.currentTimeMillis() - this.start_time));
            finishGroupshot();
        }

        private void finishGroupshot() {
            this.mGroupShotInternal.clearImages();
            this.mGroupShotInternal.finish();
            if (CameraModule.this.mPaused) {
                this.mGroupShotInternal = null;
            }
            this.mGroupShotInternal = null;
            CameraModule cameraModule = CameraModule.this;
            cameraModule.mGroupShotTimes = cameraModule.mGroupShotTimes - 1;
        }
    }

    private final class ShutterCallback implements android.hardware.Camera.ShutterCallback {
        private ShutterCallback() {
        }

        public void onShutter() {
            CameraModule.this.mShutterCallbackTime = System.currentTimeMillis();
            CameraModule.this.mShutterLag = CameraModule.this.mShutterCallbackTime - CameraModule.this.mCaptureStartTime;
            Log.v("Camera", "mShutterLag = " + CameraModule.this.mShutterLag + "ms");
            if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                CameraModule.this.mActivity.getCameraScreenNail().requestReadPixels();
            } else {
                CameraModule.this.animateShutter();
            }
            CameraModule.this.updateMutexModeUI(false);
        }
    }

    protected void initializeAfterCameraOpen() {
        setPreviewFrameLayoutAspectRatio();
        initializeZoom();
        initializeExposureCompensation();
        showTapToFocusToastIfNeeded();
        QRCodeManager.instance(this.mActivity).setCameraDevice(this.mCameraDevice);
    }

    private void keepMediaProviderInstance() {
        if (this.mMediaProviderClient == null) {
            this.mMediaProviderClient = this.mContentResolver.acquireContentProviderClient("media");
        }
    }

    private void initializeFirstTime() {
        if (!this.mFirstTimeInitialized) {
            keepMediaProviderInstance();
            installIntentFilter();
            updateLyingSensorState(true);
            this.mFirstTimeInitialized = true;
        }
    }

    private void showTapToFocusToastIfNeeded() {
        if (this.mPreferences.getBoolean("pref_camera_first_use_hint_shown_key", true) || this.mPreferences.getBoolean("pref_camera_first_portrait_use_hint_shown_key", true)) {
            this.mHandler.sendEmptyMessageDelayed(5, 1000);
        }
    }

    private void initializeSecondTime() {
        installIntentFilter();
        keepMediaProviderInstance();
        updateLyingSensorState(true);
        if (getUIController().getReviewDoneView().getVisibility() != 0) {
            hidePostCaptureAlert();
        }
    }

    private void handleUpdateFaceView(boolean visible, boolean clearFaces) {
        boolean z = false;
        FaceView view = getUIController().getFaceView();
        if (!visible) {
            if (clearFaces) {
                view.clear();
            }
            view.setVisibility(8);
        } else if ((this.mFaceDetectionStarted || isFaceBeautyMode()) && !"auto".equals(this.mParameters.getFocusMode())) {
            view.clear();
            view.setVisibility(0);
            view.setDisplayOrientation(this.mCameraDisplayOrientation);
            if (this.mCameraId == CameraHolder.instance().getFrontCameraId()) {
                z = true;
            }
            view.setMirror(z);
            view.resume();
            this.mFocusManager.setFrameView(view);
        }
    }

    protected void updateFaceView(boolean visible, boolean clearFaces) {
        int i;
        int i2 = 1;
        if (this.mHandler.hasMessages(35)) {
            this.mHandler.removeMessages(35);
        }
        Handler handler = this.mHandler;
        if (visible) {
            i = 1;
        } else {
            i = 0;
        }
        if (!clearFaces) {
            i2 = 0;
        }
        handler.obtainMessage(35, i, i2).sendToTarget();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startFaceDetection() {
        /*
        r2 = this;
        r1 = 1;
        r0 = r2.mFaceDetectionEnabled;
        if (r0 == 0) goto L_0x000d;
    L_0x0005:
        r0 = r2.mFaceDetectionStarted;
        if (r0 != 0) goto L_0x000d;
    L_0x0009:
        r0 = r2.mCameraState;
        if (r0 == r1) goto L_0x000e;
    L_0x000d:
        return;
    L_0x000e:
        r0 = r2.mObjectTrackingStarted;
        if (r0 != 0) goto L_0x000d;
    L_0x0012:
        r0 = r2.isFaceBeautyMode();
        if (r0 != 0) goto L_0x000d;
    L_0x0018:
        r0 = r2.getUIController();
        r0 = r0.getObjectView();
        if (r0 == 0) goto L_0x0030;
    L_0x0022:
        r0 = r2.getUIController();
        r0 = r0.getObjectView();
        r0 = r0.isAdjusting();
        if (r0 != 0) goto L_0x000d;
    L_0x0030:
        r0 = r2.mParameters;
        r0 = r0.getMaxNumDetectedFaces();
        if (r0 <= 0) goto L_0x0047;
    L_0x0038:
        r2.mFaceDetectionStarted = r1;
        r0 = r2.mCameraDevice;
        r0.setFaceDetectionListener(r2);
        r0 = r2.mCameraDevice;
        r0.startFaceDetection();
        r2.updateFaceView(r1, r1);
    L_0x0047:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.CameraModule.startFaceDetection():void");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void stopFaceDetection(boolean r4) {
        /*
        r3 = this;
        r2 = 0;
        r0 = r3.mFaceDetectionEnabled;
        if (r0 == 0) goto L_0x0031;
    L_0x0005:
        r0 = r3.mFaceDetectionStarted;
        if (r0 == 0) goto L_0x0031;
    L_0x0009:
        r0 = r3.mParameters;
        r0 = r0.getMaxNumDetectedFaces();
        if (r0 <= 0) goto L_0x0030;
    L_0x0011:
        r0 = com.android.camera.Device.isMTKPlatform();
        if (r0 == 0) goto L_0x0020;
    L_0x0017:
        r0 = r3.mCameraState;
        r1 = 3;
        if (r0 == r1) goto L_0x0025;
    L_0x001c:
        r0 = r3.mCameraState;
        if (r0 == 0) goto L_0x0025;
    L_0x0020:
        r0 = r3.mCameraDevice;
        r0.stopFaceDetection();
    L_0x0025:
        r3.mFaceDetectionStarted = r2;
        r0 = r3.mCameraDevice;
        r1 = 0;
        r0.setFaceDetectionListener(r1);
        r3.updateFaceView(r2, r4);
    L_0x0030:
        return;
    L_0x0031:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.CameraModule.stopFaceDetection(boolean):void");
    }

    public void onFaceDetection(Face[] faces, Camera camera) {
        if (this.mSwitchingCamera || this.mActivity.getCameraScreenNail().isModuleSwitching()) {
            getUIController().getFaceView().clear();
            return;
        }
        CameraHardwareFace[] cameraFaces = CameraHardwareFace.convertCameraHardwareFace(faces);
        if (Device.isSupportedObjectTrack() && cameraFaces.length > 0 && cameraFaces[0].faceType == 64206) {
            if (this.mObjectTrackingStarted) {
                getUIController().getObjectView().setObject(cameraFaces[0]);
            }
        } else if (getUIController().getFaceView().setFaces(cameraFaces) && this.mCameraState != 2 && getUIController().getFaceView().faceExists() && "continuous-picture".equals(this.mParameters.getFocusMode())) {
            this.mFocusManager.resetFocusIndicator();
        }
    }

    private void animateShutter() {
        if (playAnimationBeforeCapture()) {
            animateCapture();
            playSound(0);
            return;
        }
        animateHold();
    }

    private void writeImage(byte[] data, int len) {
        Exception e;
        Object obj;
        Throwable th;
        Closeable closeable = null;
        try {
            String title = Util.createJpegName(System.currentTimeMillis());
            String path = Storage.generateFilepath(title, ".dng");
            FileOutputStream outstream = new FileOutputStream(path);
            try {
                Log.e("Camera", "write image to: " + path + " with length: " + len);
                outstream.write(data, 0, len);
                outstream.close();
                closeable = null;
                Storage.addDNGToDataBase(this.mActivity, title);
                Util.closeSilently(null);
            } catch (Exception e2) {
                e = e2;
                obj = outstream;
                try {
                    Log.d("Camera", "exception: " + e.getMessage());
                    Util.closeSilently(closeable);
                } catch (Throwable th2) {
                    th = th2;
                    Util.closeSilently(closeable);
                    throw th;
                }
            } catch (Throwable th3) {
                th = th3;
                obj = outstream;
                Util.closeSilently(closeable);
                throw th;
            }
        } catch (Exception e3) {
            e = e3;
            Log.d("Camera", "exception: " + e.getMessage());
            Util.closeSilently(closeable);
        }
    }

    protected void setupPreview() {
        setupPreview(this.mIsZSLMode);
    }

    protected void setupPreview(boolean zslMode) {
        boolean burstEnd = isLongShotMode();
        if (Device.isResetToCCAFAfterCapture()) {
            this.mCameraDevice.cancelAutoFocus();
        }
        if (needSetupPreview(zslMode)) {
            startPreview();
        } else if (this.mSnapshotOnIdle) {
            this.mHandler.post(this.mDoSnapRunnable);
        } else {
            boolean z;
            FocusManager focusManager = this.mFocusManager;
            if (this.mNeedAutoFocus) {
                z = true;
            } else {
                z = this.mLongPressedAutoFocus;
            }
            focusManager.resetAfterCapture(z);
            this.mLongPressedAutoFocus = false;
            this.mFocusManager.setAeAwbLock(false);
            setCameraParameters(-1);
        }
        if (this.mNeedAutoFocus) {
            this.mHandler.sendEmptyMessageDelayed(26, 600);
            this.mNeedAutoFocus = false;
        }
        if (Device.isHDRFreeze() && this.mMutexModePicker.isMorphoHdr()) {
            this.mHandler.sendMessageDelayed(this.mHandler.obtainMessage(24, 1, 0), 500);
        } else if (burstEnd && Device.isQcomPlatform() && getBurstDelayTime() > 0) {
            this.mHandler.sendEmptyMessageDelayed(36, (long) getBurstDelayTime());
        } else {
            setCameraState(1);
            startFaceDetection();
        }
    }

    protected boolean needSetupPreview(boolean zslMode) {
        return true;
    }

    protected int getMaxPictureSize() {
        if (CameraSettings.getShaderEffect() != 0) {
            if (Device.isSupportFullSizeEffect()) {
                return 0;
            }
            return Device.isLowerEffectSize() ? 3145728 : 9000000;
        } else if (CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
            return 7680000;
        } else {
            return 0;
        }
    }

    private static String buildWaterMarkForCameraUUID(String cameraUUID) {
        return Build.DEVICE + "_" + cameraUUID + "_" + String.valueOf(System.currentTimeMillis());
    }

    private String getCameraUUID() {
        this.mParameters.set("camera-get-fusion-id", "true");
        this.mCameraDevice.setParameters(this.mParameters);
        this.mParameters = this.mCameraDevice.getParameters();
        return this.mParameters.get("camera-fusion-id");
    }

    private void sendBurstCommand() {
        if (Device.isQcomPlatform() && isLongShotMode()) {
            synchronized (this.mCameraDevice) {
                this.mCameraDevice.takePicture(new JpegQuickShutterCallback(), this.mRawPictureCallback, null, new JpegQuickPictureCallback(LocationManager.instance().getCurrentLocation()));
            }
        }
    }

    protected void setCameraState(int state) {
        this.mCameraState = state;
        if (currentIsMainThread()) {
            switch (state) {
                case 0:
                case 3:
                case 4:
                    enableCameraControls(false);
                    break;
                case 1:
                    enableCameraControls(true);
                    break;
            }
        }
    }

    protected boolean playAnimationBeforeCapture() {
        if (isZeroShotMode() || this.mMutexModePicker.isNeedComposed()) {
            return (Device.isHDRFreeze() && this.mMutexModePicker.isHdr()) ? false : true;
        } else {
            return false;
        }
    }

    public boolean capture() {
        if (this.mCameraDevice == null || this.mCameraState == 3 || this.mSwitchingCamera) {
            return false;
        }
        tryRemoveCountDownMessage();
        this.mCaptureStartTime = System.currentTimeMillis();
        this.mBurstSpeedController.capture();
        this.mPostViewPictureCallbackTime = 0;
        this.mJpegImageData = null;
        Location loc = null;
        if (!(Device.IS_MI2 || Device.IS_C1 || Device.IS_C8)) {
            this.mParameters = this.mCameraDevice.getParameters();
        }
        this.mJpegRotation = Util.getJpegRotation(this.mCameraId, this.mOrientation);
        setPictureOrientation();
        this.mParameters.setRotation(this.mJpegRotation);
        if (256 == this.mParameters.getPictureFormat()) {
            loc = LocationManager.instance().getCurrentLocation();
        }
        Util.setGpsParameters(this.mParameters, loc);
        prepareCapture();
        this.mCameraDevice.setParameters(this.mParameters);
        this.mTotalJpegCallbackNum = getBurstCount();
        if (!this.mIsZSLMode) {
            stopObjectTracking(false);
        }
        this.mLastIsEffect = false;
        this.mCameraCategory.takePicture(loc);
        if (Device.isCaptureStopFaceDetection()) {
            this.mFaceDetectionStarted = false;
        }
        setCameraState(3);
        this.mBurstShotTitle = null;
        this.mMultiSnapPictureSize = this.mParameters.getPictureSize();
        this.mReceivedJpegCallbackNum = 0;
        prepareGroupShot();
        if (Integer.parseInt(getManualValue("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default))) > 250000 || !getString(R.string.pref_face_beauty_close).equals(CameraSettings.getFaceBeautifyValue())) {
            hideLoadUI(false);
        }
        return true;
    }

    private String getSuffix() {
        if (this.mMutexModePicker.isNormal()) {
            return "";
        }
        return this.mMutexModePicker.getSuffix();
    }

    protected String getRequestFlashMode() {
        if (isSupportSceneMode()) {
            getUIController().getFlashButton().overrideValue(CameraSettings.getFlashModeByScene(this.mSceneMode));
        }
        if (this.mMutexModePicker.isSupportedFlashOn() || this.mMutexModePicker.isSupportedTorch()) {
            return getUIController().getFlashButton().getValue();
        }
        return "off";
    }

    protected void setAutoExposure(Parameters parameters, String value) {
        if (this.mFocusManager.getMeteringAreas() == null) {
            CameraSettings.setAutoExposure(sProxy, this.mParameters, value);
        }
    }

    protected boolean isZeroShotMode() {
        return false;
    }

    protected boolean isLongShotMode() {
        return false;
    }

    protected boolean isFaceBeautyMode() {
        return false;
    }

    protected void resetFaceBeautyMode() {
    }

    public boolean multiCapture() {
        if (this.mIsImageCaptureIntent || this.mCameraState == 3 || this.mCameraDevice == null || this.mSwitchingCamera || isFrontCamera()) {
            return false;
        }
        if (this.mAudioCaptureManager.isRunning()) {
            Toast.makeText(this.mActivity, R.string.toast_burst_snap_forbidden_when_audio_capture_open, 0).show();
            return false;
        }
        this.mActivity.getScreenHint().updateHint();
        if (Storage.isLowStorageAtLastPoint()) {
            Log.i("Camera", "Not enough space or storage not ready. remaining=" + Storage.getLeftSpace());
            return false;
        } else if (this.mActivity.getImageSaver().shouldStopShot()) {
            Log.i("Camera", "ImageSaver is full, wait for a moment!");
            RotateTextToast.getInstance(this.mActivity).show(R.string.toast_saving, 0);
            return false;
        } else {
            Location loc = null;
            if (256 == this.mParameters.getPictureFormat()) {
                loc = LocationManager.instance().getCurrentLocation();
            }
            this.mCaptureStartTime = System.currentTimeMillis();
            this.mBurstSpeedController.capture();
            if (!Device.IS_MI2) {
                this.mParameters = this.mCameraDevice.getParameters();
            }
            Util.setGpsParameters(this.mParameters, loc);
            this.mJpegRotation = Util.getJpegRotation(this.mCameraId, this.mOrientation);
            this.mParameters.setRotation(this.mJpegRotation);
            prepareMultiCapture();
            this.mCameraDevice.setParameters(this.mParameters);
            saveStatusBeforeBurst();
            this.mTotalJpegCallbackNum = getBurstCount();
            if (!this.mUpdateImageTitle || this.mBurstShotTitle == null) {
                this.mReceivedJpegCallbackNum = 0;
                this.mBurstShotTitle = null;
            } else {
                this.mReceivedJpegCallbackNum = 1;
            }
            this.mMultiSnapStopRequest = false;
            this.mMultiSnapPictureSize = this.mParameters.getPictureSize();
            this.mCameraDevice.takePicture(new JpegQuickShutterCallback(), this.mRawPictureCallback, null, new JpegQuickPictureCallback(LocationManager.instance().getCurrentLocation()));
            CameraDataAnalytics.instance().trackEvent("burst_times");
            getUIController().getMultiSnapNum().setText("");
            getUIController().getMultiSnapNum().setVisibility(0);
            return true;
        }
    }

    protected void prepareMultiCapture() {
        applyMultiShutParameters(true);
    }

    protected void applyMultiShutParameters(boolean startshut) {
    }

    protected int getBurstCount() {
        if (this.mMultiSnapStatus) {
            return BURST_SHOOTING_COUNT;
        }
        if (this.mMutexModePicker.isUbiFocus()) {
            return 7;
        }
        if (this.mMutexModePicker.isSceneHdr() && Device.IS_HM2A) {
            return this.mParameters.getInt("num-snaps-per-shutter");
        }
        if (CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key") && this.mGroupShotTimes <= 5 && (Util.isMemoryRich(this.mActivity) || this.mGroupShotTimes == 0)) {
            return getGroupshotNum();
        }
        return 1;
    }

    protected void prepareCapture() {
    }

    protected int getCameraRotation() {
        return ((this.mOrientationCompensation - this.mDisplayRotation) + 360) % 360;
    }

    public void setFocusParameters() {
        if (this.mCameraState != 3) {
            setCameraParameters(2);
        }
    }

    public void playSound(int soundId) {
        if (!this.mAudioCaptureManager.isRunning() || soundId != 1) {
            playCameraSound(soundId);
        }
    }

    public void onCreate(com.android.camera.Camera activity) {
        super.onCreate(activity);
        this.mPreferences = CameraSettingPreferences.instance();
        CameraSettings.upgradeGlobalPreferences(this.mPreferences);
        if (isRestoring()) {
            this.mActivity.getCameraAppImpl().resetRestoreFlag();
        } else {
            resetCameraSettingsIfNeed();
        }
        this.mCameraId = getPreferredCameraId();
        changeConflictPreference();
        this.mActivity.changeRequestOrientation();
        if (PermissionManager.checkCameraLaunchPermissions()) {
            this.mCameraStartUpThread = new CameraStartUpThread();
            this.mCameraStartUpThread.start();
        }
        this.mIsImageCaptureIntent = this.mActivity.isImageCaptureIntent();
        CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
        this.mActivity.createContentView();
        this.mActivity.createCameraScreenNail(!this.mIsImageCaptureIntent, false);
        V6ModulePicker.setCurrentModule(0);
        getUIController().onCreate();
        getUIController().useProperView();
        prepareUIByPreviewSize();
        this.mActivity.getSensorStateManager().setSensorStateListener(this.mSensorStateListener);
        QRCodeManager.instance(this.mActivity).onCreate(this.mActivity, this.mHandler.getLooper(), this);
        this.mStartPreviewPrerequisiteReady.open();
        if (this.mIsImageCaptureIntent) {
            setupCaptureParams();
        }
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra("android.intent.extra.quickCapture", false);
        initializeMutexMode();
        this.mAudioCaptureManager = new AudioCaptureManager(this, this.mActivity);
        enableCameraControls(false);
    }

    public void onNewIntent() {
        this.mCameraId = getPreferredCameraId();
        changeConflictPreference();
        this.mIsImageCaptureIntent = this.mActivity.isImageCaptureIntent();
        CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
        if (Util.isPortraitIntent(this.mActivity) && !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            this.mPreferences.edit().putString("pref_camera_portrait_mode_key", "on").apply();
        }
    }

    private void updateMutexModeUI(boolean done) {
        if (!this.mMutexModePicker.isNormal()) {
            if (!this.mMutexModePicker.isAoHdr()) {
                hideLoadUI(done);
            }
            if (this.mMutexModePicker.isUbiFocus() && !(done && getUIController().getWarningMessageView().getText().equals(getString(R.string.ubi_focus_capture_fail)))) {
                updateWarningMessage(R.string.cannot_move_warning_message, done);
            }
        }
        if (done || Integer.parseInt(getManualValue("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default))) > 250000 || !getString(R.string.pref_face_beauty_close).equals(CameraSettings.getFaceBeautifyValue())) {
            hideLoadUI(done);
        }
    }

    protected void updateExitButton(boolean show) {
        boolean isDetectedHHT = !this.mMutexModePicker.isHdr() ? this.mMutexModePicker.isHandNight() ? isDetectedHHT() : false : true;
        if (!isDetectedHHT) {
            getUIController().getModeExitView().updateExitButton(-1, show);
        }
    }

    private void hideLoadUI(boolean hide) {
        getUIController().getCaptureProgressBar().setVisibility(hide ? 8 : 0);
    }

    private void prepareUIByPreviewSize() {
        if (Device.isPad()) {
            this.mUIStyle = 0;
        } else if (CameraSettings.sCroppedIfNeeded) {
            this.mUIStyle = 1;
        } else {
            PictureSize pictureSize = new PictureSize(this.mPreferences.getString("pref_camera_picturesize_key", PictureSizeManager.getDefaultValue()));
            if (!pictureSize.isEmpty()) {
                int style = CameraSettings.getUIStyleByPreview(pictureSize.width, pictureSize.height);
                if (style != this.mUIStyle) {
                    this.mUIStyle = style;
                    CameraSettings.changeUIByPreviewSize(this.mActivity, this.mUIStyle);
                    changePreviewSurfaceSize();
                }
                getUIController().getPreviewFrame().setAspectRatio(CameraSettings.getPreviewAspectRatio(pictureSize.width, pictureSize.height));
            }
        }
    }

    private void overrideCameraSettings(String flashMode, String whiteBalance, String exposureMode, String focusMode, String isoValue, String effectValue) {
        if (Device.isQcomPlatform()) {
            getUIController().getSettingPage().overrideSettings("pref_camera_whitebalance_key", whiteBalance, "pref_camera_exposure_key", exposureMode, "pref_camera_focus_mode_key", focusMode, "pref_qc_camera_iso_key", isoValue, "pref_camera_coloreffect_key", effectValue);
            return;
        }
        getUIController().getSettingPage().overrideSettings("pref_camera_whitebalance_key", whiteBalance, "pref_camera_exposure_key", exposureMode, "pref_camera_focus_mode_key", focusMode, "pref_qc_camera_iso_key", isoValue);
    }

    private void updateSceneModeUI() {
        if ("auto".equals(this.mSceneMode)) {
            overrideCameraSettings(null, null, null, null, null, null);
        } else {
            overrideCameraSettings(this.mParameters.getFlashMode(), getString(R.string.pref_camera_whitebalance_default), getString(R.string.pref_exposure_default), getString(R.string.pref_camera_focusmode_value_default), getString(R.string.pref_camera_iso_default), getString(R.string.pref_camera_coloreffect_default));
        }
        getUIController().getFlashButton().overrideSettings(CameraSettings.getFlashModeByScene(this.mSceneMode));
    }

    protected boolean exitWhiteBalanceLockMode() {
        return false;
    }

    public List<String> getSupportedSettingKeys() {
        return getLayoutModeKeys(this.mActivity, isBackCamera(), this.mIsImageCaptureIntent);
    }

    public static List<String> getLayoutModeKeys(com.android.camera.Camera activity, boolean isBackCamera, boolean isImageCaptureIntent) {
        List<String> keys = new ArrayList();
        if (isBackCamera) {
            if (!isImageCaptureIntent) {
                keys.add("pref_camera_panoramamode_key");
            }
            keys.add("pref_delay_capture_mode");
            keys.add("pref_audio_capture");
            if (!isImageCaptureIntent && Device.isSupportedUbiFocus()) {
                keys.add("pref_camera_ubifocus_key");
            }
            keys.add("pref_camera_manual_mode_key");
            if (Device.isSupportGradienter()) {
                keys.add("pref_camera_gradienter_key");
            }
            if (Device.isSupportedSkinBeautify() && !Device.IS_H2X_LC) {
                keys.add("pref_camera_face_beauty_mode_key");
            }
            if (!isImageCaptureIntent && Device.isSupportGroupShot()) {
                keys.add("pref_camera_groupshot_mode_key");
            } else if (Device.isUsedMorphoLib()) {
                keys.add("pref_camera_hand_night_key");
            }
            if (Device.IS_HONGMI) {
                keys.add("pref_camera_scenemode_setting_key");
            }
            if (Device.isSupportedTiltShift()) {
                keys.add("pref_camera_tilt_shift_mode");
            }
            if (Device.isSupportSquare()) {
                keys.add("pref_camera_square_mode_key");
            }
            if (!isImageCaptureIntent && Device.isSupportGroupShot() && Device.isUsedMorphoLib()) {
                keys.add("pref_camera_hand_night_key");
            }
        } else {
            keys.add("pref_delay_capture_mode");
            keys.add("pref_audio_capture");
            if (Device.isSupportedMagicMirror()) {
                keys.add("pref_camera_magic_mirror_key");
            }
            if (Device.isSupportGroupShot()) {
                keys.add("pref_camera_groupshot_mode_key");
            }
        }
        return keys;
    }

    public void requestRender() {
    }

    public boolean handleMessage(int what, int sender, final Object extra1, Object extra2) {
        if (super.handleMessage(what, sender, extra1, extra2)) {
            return true;
        }
        switch (sender) {
            case R.id.hide_mode_animation_done:
                if (this.mSetCameraParameter != 0) {
                    this.mHandler.removeMessages(32);
                    this.mHandler.sendEmptyMessageDelayed(32, 200);
                }
                if (this.mManualModeSwitched) {
                    this.mManualModeSwitched = false;
                    this.mHandler.removeMessages(41);
                    this.mHandler.sendEmptyMessageDelayed(41, 200);
                }
                return true;
            case R.id.v6_thumbnail_button:
                onThumbnailClicked(null);
                return true;
            case R.id.v6_shutter_button:
                if (what == 0) {
                    onShutterButtonClick();
                    if (!isCountDownMode()) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_shutter");
                    }
                    if (this.mFocusManager.getMeteringAreas() != null) {
                        CameraDataAnalytics.instance().trackEvent("touch_focus_shutter_capture_times_key");
                    }
                } else if (what == 1) {
                    onShutterButtonLongClick();
                } else if (what == 2) {
                    if (isBackCamera()) {
                        Point start = (Point) extra1;
                        Point center = (Point) extra2;
                        getUIController().getSmartShutterButton().flyin(start.x, start.y, center.x, center.y);
                    }
                } else if (what == 3) {
                    onShutterButtonFocus(((Boolean) extra1).booleanValue(), 2);
                    if (this.mMutexModePicker.isBurstShoot() && ((Boolean) extra1).booleanValue()) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_shutter");
                    }
                }
                return true;
            case R.id.v6_module_picker:
                Runnable r = new Runnable() {
                    public void run() {
                        CameraModule.this.mActivity.getCameraScreenNail().animateModuleChangeBefore();
                        CameraModule.this.switchToOtherMode(((Integer) extra1).intValue());
                    }
                };
                enableCameraControls(false);
                getUIController().getShutterButton().onPause();
                getUIController().getFocusView().clear();
                getUIController().getBottomControlLowerPanel().animationSwitchToVideo(r);
                this.mActivity.getCameraScreenNail().switchModule();
                return true;
            case R.id.mode_button:
                tryRemoveCountDownMessage();
                resetMetaDataManager();
                QRCodeManager.instance(this.mActivity).hideViewFinderFrame();
                return true;
            case R.id.v6_camera_picker:
                return onCameraPickerClicked(((Integer) extra1).intValue());
            case R.id.capture_control_panel:
                if (what == 0) {
                    onReviewDoneClicked(null);
                } else {
                    onReviewCancelClicked(null);
                }
                return true;
            case R.id.v6_flash_mode_button:
                if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                    this.mSettingsOverrider.removeSavedSetting("pref_camera_flashmode_key");
                    if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                        getUIController().getStereoButton().switchOffStereo(true);
                    } else {
                        this.mSettingsOverrider.removeSavedSetting("pref_camera_hdr_key");
                        getUIController().getPortraitButton().switchOff();
                    }
                    getUIController().getHdrButton().updateHdrAccordingFlash(getUIController().getFlashButton().getValue());
                    return true;
                }
                if (!(!CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key") || "off".equals(getUIController().getFlashButton().getValue()) || "torch".equals(getUIController().getFlashButton().getValue()))) {
                    getUIController().getModeExitView().clearExitButtonClickListener(true);
                }
                if (!(this.mMutexModePicker.isNormal() || this.mMutexModePicker.isHdr() || this.mMutexModePicker.isSupportedFlashOn() || this.mMutexModePicker.isSupportedTorch())) {
                    if (getUIController().getModeExitView().isExitButtonShown()) {
                        getUIController().getModeExitView().clearExitButtonClickListener(true);
                    } else if (this.mMutexModePicker.isHandNight() && isDetectedHHT()) {
                        this.mMutexModePicker.resetMutexMode();
                    }
                }
                tryRemoveCountDownMessage();
                getUIController().getHdrButton().updateHdrAccordingFlash(getUIController().getFlashButton().getValue());
                stopObjectTracking(true);
                updateASD("pref_camera_flashmode_key");
                onSharedPreferenceChanged();
                return true;
            case R.id.v6_hdr:
                if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                    this.mSettingsOverrider.removeSavedSetting("pref_camera_hdr_key");
                    getUIController().getStereoButton().switchOffStereo(true);
                    return true;
                } else if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                    this.mSettingsOverrider.removeSavedSetting("pref_camera_flashmode_key");
                    this.mSettingsOverrider.removeSavedSetting("pref_camera_hdr_key");
                    getUIController().getPortraitButton().switchOff();
                    return true;
                } else {
                    stopObjectTracking(true);
                    getUIController().getFlashButton().updateFlashModeAccordingHdr(getUIController().getHdrButton().getValue());
                    updateHDRPreference();
                    return true;
                }
            case R.id.skin_beatify_button:
                onSharedPreferenceChanged();
                return true;
            case R.id.portrait_switch_image:
                onSettingValueChanged((String) extra1);
                return true;
            case R.id.stereo_switch_image:
                if (what == 7) {
                    onSettingValueChanged((String) extra1);
                } else {
                    onStereoModeChanged();
                }
                return true;
            case R.id.v6_frame_layout:
                if (what == 0) {
                    if (this.mFocusManager != null) {
                        Point p = (Point) extra1;
                        this.mFocusManager.setPreviewSize(p.x, p.y);
                    }
                } else if (what == 1) {
                    onFrameLayoutChange((View) extra1, (Rect) extra2);
                }
                return true;
            case R.id.v6_focus_view:
                if (what == 2) {
                    onShutterButtonClick();
                }
                return true;
            case R.id.v6_setting_page:
                if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                    if ("pref_camera_shader_coloreffect_key".equals(extra1)) {
                        this.mSettingsOverrider.removeSavedSetting("pref_camera_shader_coloreffect_key");
                    }
                    if ("pref_camera_panoramamode_key".equals(extra1) && CameraSettings.isSwitchOn("pref_camera_panoramamode_key")) {
                        getUIController().getStereoButton().switchOffStereo(false);
                        closeCamera();
                        switchToOtherMode(2);
                    } else {
                        getUIController().getStereoButton().switchOffStereo(true);
                    }
                    return true;
                }
                if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key") && !"pref_camera_shader_coloreffect_key".equals(extra1)) {
                    getUIController().getPortraitButton().switchOff(false);
                }
                if (getUIController().getHdrButton().getVisibility() == 0) {
                    getUIController().getPeakButton().updateVisible();
                    getUIController().getHdrButton().updateVisible();
                }
                if (getUIController().getSettingPage().isItemSelected()) {
                    stopObjectTracking(true);
                }
                if (what == 7) {
                    onSettingValueChanged((String) extra1);
                } else if (what == 6) {
                    onModeSelected(extra1);
                }
                if (getUIController().getHdrButton().getVisibility() == 8) {
                    getUIController().getHdrButton().updateVisible();
                    getUIController().getPeakButton().updateVisible();
                }
                getUIController().getStereoButton().updateVisible();
                getUIController().getPortraitButton().updateVisible();
                return true;
            case R.id.setting_page_watermark_option:
                onSettingValueChanged((String) extra1);
                return true;
            case R.id.setting_button:
                openSettingActivity();
                return true;
            case R.id.edge_shutter_view:
                onShutterButtonClick();
                return true;
            default:
                return false;
        }
    }

    private void onStereoModeChanged() {
        QRCodeManager.instance(this.mActivity).onPause();
        setCameraState(0);
        resetMetaDataManager();
        this.mActivity.getSensorStateManager().setFocusSensorEnabled(false);
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
            this.mFocusManager.resetTouchFocus();
        }
        this.mCameraStartUpThread = new CameraStartUpThread();
        this.mCameraStartUpThread.start();
        CameraSettings.resetZoom(this.mPreferences);
        CameraSettings.resetExposure();
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            this.mMutexModePicker.resetMutexModeDummy();
            updateStereoSettings(true);
            return;
        }
        this.mSettingsOverrider.restoreSettings();
        if (TextUtils.equals(getUIController().getWarningMessageView().getText(), getString(R.string.dual_camera_use_hint))) {
            updateWarningMessage(0, true);
        }
    }

    private void onModeSelected(Object extra1) {
        handleDelayShutter();
        if ("pref_camera_panoramamode_key".equals(extra1)) {
            boolean switchAnimationRunning = this.mSwitchCameraAnimationRunning;
            if (switchAnimationRunning) {
                this.mActivity.getCameraScreenNail().animateSwitchCameraBefore();
            }
            switchToOtherMode(2);
            if (switchAnimationRunning) {
                this.mActivity.getCameraScreenNail().switchCameraDone();
            }
            return;
        }
        if ("pref_camera_ubifocus_key".equals(extra1)) {
            if (CameraSettings.isSwitchOn("pref_camera_ubifocus_key")) {
                this.mMutexModePicker.setMutexMode(6);
            } else if (this.mMutexModePicker.isUbiFocus()) {
                this.mMutexModePicker.resetMutexMode();
            }
        } else if ("pref_camera_hand_night_key".equals(extra1)) {
            if (CameraSettings.isSwitchOn("pref_camera_hand_night_key")) {
                this.mMutexModePicker.setMutexMode(3);
            } else if (this.mMutexModePicker.isHandNight()) {
                this.mMutexModePicker.resetMutexMode();
            }
        } else if ("pref_camera_square_mode_key".equals(extra1)) {
            getUIController().getPreviewFrame().updateRefenceLineAccordSquare();
            if ("auto".equals(this.mParameters.getFocusMode())) {
                this.mFocusManager.resetTouchFocus();
                cancelAutoFocus();
            }
            if (CameraSettings.isSupportedOpticalZoom()) {
                CameraSettings.resetZoom(this.mPreferences);
                getUIController().getZoomButton().reloadPreference();
            }
        } else if ("pref_camera_shader_coloreffect_key".equals(extra1)) {
            if (this.mMutexModePicker.isUbiFocus() || this.mMutexModePicker.isBurstShoot()) {
                this.mSettingsOverrider.removeSavedSetting("pref_camera_shader_coloreffect_key");
                getUIController().getModeExitView().clearExitButtonClickListener(true);
            } else if (CameraSettings.isSwitchOn("pref_camera_gradienter_key") || CameraSettings.isSwitchOn("pref_camera_tilt_shift_mode") || CameraSettings.isSwitchOn("pref_camera_magic_mirror_key") || CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
                getUIController().getModeExitView().clearExitButtonClickListener(true);
            }
        } else if ("pref_camera_manual_mode_key".equals(extra1)) {
            if (CameraSettings.isSupportedOpticalZoom()) {
                CameraSettings.resetZoom(this.mPreferences);
                CameraSettings.resetCameraZoomMode();
                getUIController().getZoomButton().reloadPreference();
                this.mSwitchCameraLater = Boolean.valueOf(true);
                prepareSwitchCameraAnimation(this.mPreviewTextureCopiedActionSwitchCameraLater);
                if (getUIController().getPreviewPage().isPreviewPageVisible()) {
                    this.mHandler.sendEmptyMessage(41);
                } else {
                    this.mManualModeSwitched = true;
                }
            }
            if (CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
                getUIController().getFlashButton().keepSetValue("off");
                CameraSettings.updateFocusMode();
            } else {
                getUIController().getFlashButton().restoreKeptValue();
                getUIController().getHdrButton().overrideSettings(null);
            }
        } else if ("pref_camera_zoom_mode_key".equals(extra1)) {
            getUIController().getZoomButton().requestSwitchCamera();
        }
        if ("pref_camera_groupshot_mode_key".equals(extra1)) {
            if (CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
                initGroupShot(getGroupshotNum());
                if (!"torch".equals(getUIController().getFlashButton().getValue())) {
                    getUIController().getFlashButton().keepSetValue("off");
                }
                updateWarningMessage(R.string.groupshot_mode_use_hint, false);
            } else {
                updateWarningMessage(0, true);
            }
            if (CameraSettings.isSupportedOpticalZoom()) {
                CameraSettings.resetZoom(this.mPreferences);
                getUIController().getZoomButton().reloadPreference();
            }
        }
        this.mActivity.getSensorStateManager().setGradienterEnabled(CameraSettings.isSwitchOn("pref_camera_gradienter_key"));
        if (CameraSettings.isSwitchOn("pref_camera_gradienter_key") || CameraSettings.isSwitchOn("pref_camera_tilt_shift_mode") || CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
            getUIController().getEffectButton().resetSettings();
        } else {
            getUIController().getEffectButton().restoreSettings();
        }
        onSharedPreferenceChanged();
        getUIController().getEffectCropView().updateVisible();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    protected void resetMetaDataManager() {
        if (CameraSettings.isSupportedMetadata()) {
            this.mMetaDataManager.reset();
        }
    }

    protected void updateASD(String key) {
        if (Device.isSupportedASD()) {
            this.mMetaDataManager.setAsdDetectMask(key);
        }
    }

    private void applyPreferenceSettingsLater() {
        this.mSetCameraParameter = -1;
        if (getUIController().getPreviewPage().isPreviewPageVisible()) {
            this.mHandler.removeMessages(32);
            this.mHandler.sendEmptyMessage(32);
        }
    }

    private void handleDelayShutter() {
        tryRemoveCountDownMessage();
        this.mHandler.removeMessages(29);
        if (CameraSettings.isAudioCaptureOpen()) {
            if (!this.mAudioCaptureManager.isRunning()) {
                this.mHandler.sendEmptyMessageDelayed(29, 350);
            }
        } else if (this.mAudioCaptureManager.isRunning()) {
            this.mAudioCaptureManager.close();
        }
    }

    public void onSettingValueChanged(String key) {
        super.onSettingValueChanged(key);
        if (this.mCameraDevice != null) {
            if ("pref_delay_capture_key".equals(key)) {
                handleDelayShutter();
            } else {
                if ("pref_camera_focus_mode_key".equals(key)) {
                    getUIController().getPeakButton().updateVisible();
                }
                if ("pref_camera_portrait_mode_key".equals(key)) {
                    CameraSettings.resetZoom(this.mPreferences);
                    CameraSettings.resetExposure();
                    if (!(CameraSettings.isSwitchOn("pref_camera_manual_mode_key") || CameraSettings.isSwitchOn("pref_camera_panoramamode_key"))) {
                        prepareSwitchCameraAnimation(this.mPreviewTextureCopiedActionByPass);
                        stopPreview();
                        if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                            this.mSettingsOverrider.overrideSettings("pref_camera_flashmode_key", "off", "pref_camera_hdr_key", "off");
                            if (CameraSettings.isDualCameraHintShown(this.mPreferences)) {
                                this.mHandler.sendEmptyMessage(40);
                            }
                            this.mMutexModePicker.resetMutexModeDummy();
                        } else {
                            this.mSettingsOverrider.restoreSettings();
                            this.mHandler.removeMessages(40);
                            if (TextUtils.equals(getUIController().getWarningMessageView().getText(), getString(R.string.dual_camera_use_hint))) {
                                updateWarningMessage(0, true);
                            }
                            updateHDRPreference();
                        }
                        getUIController().getFlashButton().reloadPreference();
                        getUIController().getHdrButton().reloadPreference();
                        getUIController().getZoomButton().reloadPreference();
                        getUIController().getZoomButton().updateVisible();
                        startPreview();
                        enableCameraControls(false);
                        this.mHandler.sendEmptyMessageDelayed(43, 500);
                    }
                } else {
                    onSharedPreferenceChanged();
                }
            }
        }
    }

    public void onOrientationChanged(int orientation) {
        this.mDeviceRotation = (float) orientation;
        if (!CameraSettings.isSwitchOn("pref_camera_gradienter_key")) {
            setOrientation(orientation);
        }
    }

    private void setOrientation(int orientation) {
        if (orientation != -1) {
            this.mOrientation = Util.roundOrientation(orientation, this.mOrientation);
            EffectController.getInstance().setOrientation(Util.getShootOrientation(this.mActivity, this.mOrientation));
            checkActivityOrientation();
            int orientationCompensation = (this.mOrientation + this.mDisplayRotation) % 360;
            if (this.mOrientationCompensation != orientationCompensation) {
                this.mOrientationCompensation = orientationCompensation;
                setOrientationIndicator(this.mOrientationCompensation, true);
                setOrientationParameter();
            }
        }
    }

    private void setOrientationParameter() {
        if (this.mParameters != null && this.mCameraDevice != null && this.mCameraState != 3 && this.mCameraStartUpThread == null) {
            boolean changeFlag = false;
            if (Device.isFaceDetectNeedRotation()) {
                int newRotation = Util.getJpegRotation(this.mCameraId, this.mOrientation);
                if (sProxy.getRotation(this.mParameters) != newRotation) {
                    changeFlag = true;
                    this.mParameters.setRotation(newRotation);
                }
            }
            if (!((!Device.isSupportedIntelligentBeautify() && !Device.isSupportedObjectTrack()) || this.mOrientation == -1 || String.valueOf(this.mOrientation).equals(this.mParameters.get("xiaomi-preview-rotation")))) {
                changeFlag = true;
                this.mParameters.set("xiaomi-preview-rotation", this.mOrientation);
            }
            if (!changeFlag) {
                return;
            }
            if (Device.isLCPlatform() || Device.isMTKPlatform() || Device.isSupportedIntelligentBeautify()) {
                this.mCameraDevice.setParametersAsync(this.mParameters);
            }
        }
    }

    public void onStop() {
        super.onStop();
        if (this.mActivity.isNeedResetGotoGallery() && Device.isReleaseLaterForGallery()) {
            releaseResources();
        }
        if (this.mMediaProviderClient != null) {
            this.mMediaProviderClient.release();
            this.mMediaProviderClient = null;
        }
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        tryRemoveCountDownMessage();
        if (this.mActivity.getThumbnailUpdater().getThumbnail() != null) {
            this.mActivity.gotoGallery();
        }
    }

    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        this.mKeepBitmapTexture = false;
        if (isSelectingCapturedImage()) {
            previewBecomeVisible();
            setCameraState(1);
            hidePostCaptureAlert();
            return;
        }
        this.mActivity.setResult(0, new Intent());
        this.mActivity.finish();
    }

    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        doAttach();
    }

    private void saveJpegData(byte[] data) {
        int width;
        int height;
        Size s = this.mParameters.getPictureSize();
        Location loc = LocationManager.instance().getCurrentLocation();
        int orientation = Exif.getOrientation(data);
        if ((this.mJpegRotation + orientation) % 180 == 0) {
            width = s.width;
            height = s.height;
        } else {
            width = s.height;
            height = s.width;
        }
        byte[] bArr = data;
        this.mActivity.getImageSaver().addImage(bArr, Util.createJpegName(System.currentTimeMillis()), System.currentTimeMillis(), null, loc, width, height, null, orientation, false, false, true);
    }

    private boolean needReturnInvisibleWatermark() {
        return this.mNeedSealCameraUUID;
    }

    private void doAttach() {
        if (!this.mPaused) {
            byte[] data = this.mJpegImageData;
            if (this.mIsSaveCaptureImage) {
                saveJpegData(data);
            }
            if (this.mCropValue != null) {
                Uri uri = null;
                Closeable closeable = null;
                try {
                    File path = this.mActivity.getFileStreamPath("crop-temp");
                    path.delete();
                    FileOutputStream tempStream = this.mActivity.openFileOutput("crop-temp", 0);
                    tempStream.write(data);
                    tempStream.close();
                    closeable = null;
                    uri = Uri.fromFile(path);
                    Bundle newExtras = new Bundle();
                    if ("circle".equals(this.mCropValue)) {
                        newExtras.putString("circleCrop", "true");
                    }
                    if (this.mSaveUri != null) {
                        newExtras.putParcelable("output", this.mSaveUri);
                    } else {
                        newExtras.putBoolean("return-data", true);
                    }
                    Intent cropIntent = new Intent("com.android.camera.action.CROP");
                    cropIntent.setData(uri);
                    cropIntent.putExtras(newExtras);
                    this.mActivity.startActivityForResult(cropIntent, 1000);
                } catch (FileNotFoundException e) {
                    this.mActivity.setResult(0);
                    this.mActivity.finish();
                } catch (IOException e2) {
                    this.mActivity.setResult(0);
                    this.mActivity.finish();
                } finally {
                    Util.closeSilently(closeable);
                }
            } else if (this.mSaveUri != null) {
                if (needReturnInvisibleWatermark()) {
                    if (this.mCameraUUIDWatermarkImageData == null) {
                        this.mActivity.setResult(0);
                        this.mActivity.finish();
                        return;
                    }
                    data = this.mCameraUUIDWatermarkImageData;
                }
                Closeable closeable2 = null;
                try {
                    closeable2 = this.mContentResolver.openOutputStream(this.mSaveUri);
                    closeable2.write(data);
                    closeable2.close();
                    this.mActivity.setResult(-1);
                } catch (IOException ex) {
                    Log.e("Camera", "IOException when doAttach");
                    ex.printStackTrace();
                } finally {
                    this.mActivity.finish();
                    Util.closeSilently(closeable2);
                }
            } else {
                this.mActivity.setResult(-1, new Intent("inline-data").putExtra("data", Util.rotate(Util.makeBitmap(data, 51200), Exif.getOrientation(this.mJpegImageData))));
                this.mActivity.finish();
            }
        }
    }

    public void onLongPress(int x, int y) {
        if (isInTapableRect(x, y)) {
            onSingleTapUp(x, y);
            this.mHandler.post(this.mDoSnapRunnable);
            CameraDataAnalytics.instance().trackEvent("capture_times_long_press");
            getUIController().getPreviewFrame().performHapticFeedback(0);
        }
    }

    public void onShutterButtonFocus(boolean pressed, int fromWhat) {
        if (pressed) {
            if (this.mMutexModePicker.isBurstShoot()) {
                this.mFocusManager.doMultiSnap(false);
            }
        } else if (this.mPendingMultiCapture) {
            this.mPendingMultiCapture = false;
            return;
        } else if (this.mHandler.hasMessages(12)) {
            this.mHandler.removeMessages(12);
            return;
        } else if (!this.mFocusManager.cancelMultiSnapPending()) {
            if (this.mMultiSnapStatus) {
                this.mMultiSnapStopRequest = true;
                return;
            } else if (this.mPendingCapture) {
                this.mPendingCapture = false;
                if (getUIController().getShutterButton().isCanceled() || CameraSettings.isPressDownCapture()) {
                    this.mFocusManager.resetFocusStateIfNeeded();
                    cancelAutoFocus();
                } else {
                    onShutterButtonClick();
                }
                return;
            }
        } else {
            return;
        }
        if (!this.mPaused && this.mCameraState != 3 && this.mCameraState != 0 && !this.mSwitchingCamera && !isFrontCamera()) {
            if (!pressed || canTakePicture()) {
                if (pressed) {
                    if (needSwitchZeroShotMode()) {
                        setCameraParameters(2);
                    }
                    this.mFocusManager.onShutterDown();
                } else {
                    this.mFocusManager.onShutterUp();
                }
            }
        }
    }

    private boolean isCountDownMode() {
        if (!CameraSettings.isSwitchOn("pref_delay_capture_mode") || CameraSettings.getCountDownTimes() <= 0) {
            return false;
        }
        return true;
    }

    public void onShutterButtonClick() {
        Log.v("Camera", "onShutterButtonClick " + this.mCameraState);
        this.m3ALocked = false;
        if (!(!isShutterButtonClickable() || this.mMultiSnapStatus || this.mMutexModePicker.isBurstShoot())) {
            AutoLockManager.getInstance(this.mActivity).onUserInteraction();
            QRCodeManager.instance(this.mActivity).hideViewFinderFrame();
            if (this.mIsCountDown || !isCountDownMode()) {
                tryRemoveCountDownMessage();
                exitWhiteBalanceLockMode();
                this.mActivity.getScreenHint().updateHint();
                if (Storage.isLowStorageAtLastPoint()) {
                    Log.i("Camera", "Not enough space or storage not ready. remaining=" + Storage.getLeftSpace());
                } else if (this.mActivity.getImageSaver().shouldStopShot()) {
                    Log.i("Camera", "ImageSaver is full, wait for a moment!");
                    RotateTextToast.getInstance(this.mActivity).show(R.string.toast_saving, 0);
                } else {
                    if (getUIController().getObjectView().isTrackFailed()) {
                        stopObjectTracking(false);
                    }
                    if (this.mCameraState != 3) {
                        this.mNeedAutoFocus = needAutoFocusBeforeCapture();
                    }
                    if (this.mCameraState == 3) {
                        if (!(this.mIsImageCaptureIntent || this.mNeedAutoFocus || !this.mMutexModePicker.isNormal())) {
                            this.mHandler.removeCallbacks(this.mDoSnapRunnable);
                            this.mSnapshotOnIdle = true;
                        }
                        return;
                    }
                    this.mLastShutterButtonClickTime = System.currentTimeMillis();
                    this.mSnapshotOnIdle = false;
                    this.mUpdateImageTitle = false;
                    this.mFocusManager.prepareCapture(this.mNeedAutoFocus, 2);
                    this.mFocusManager.doSnap();
                    if (this.mFocusManager.isFocusingSnapOnFinish()) {
                        enableCameraControls(false);
                    }
                    if (this.mKeepAdjustedEv) {
                        CameraDataAnalytics.instance().trackEvent("ev_adjust_keep_time_key");
                    }
                }
            } else {
                int countDownTime = CameraSettings.getCountDownTimes();
                sendDelayedCaptureMessage(1000, countDownTime);
                if (countDownTime > 3) {
                    playSound(7);
                }
                this.mIsCountDown = true;
            }
        }
    }

    private boolean isShutterButtonClickable() {
        return (this.mPaused || this.mSwitchingCamera || this.mCameraState == 0) ? false : true;
    }

    protected boolean needSwitchZeroShotMode() {
        return false;
    }

    protected boolean needAutoFocusBeforeCapture() {
        return false;
    }

    public boolean onShutterButtonLongClick() {
        if (this.mMutexModePicker.isBurstShoot() || this.mIsImageCaptureIntent) {
            return true;
        }
        if (CameraSettings.isBurstShootingEnable(this.mPreferences) && !getUIController().getSettingPage().isItemSelected() && !this.mIsImageCaptureIntent && !CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") && !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key") && isBackCamera() && !this.mMultiSnapStatus && !this.mHandler.hasMessages(12) && !this.mHandler.hasMessages(24) && !this.mPendingMultiCapture) {
            this.mHandler.sendEmptyMessageDelayed(12, 0);
            if (Device.isSupportedFastCapture()) {
                this.mUpdateImageTitle = true;
            }
        } else if (this.mCameraState == 3) {
            return false;
        } else {
            this.mPendingCapture = true;
            this.mLongPressedAutoFocus = true;
            getUIController().getFocusView().setFocusType(false);
            this.mFocusManager.requestAutoFocus();
            this.mActivity.getScreenHint().updateHint();
            AutoLockManager.getInstance(this.mActivity).onUserInteraction();
            QRCodeManager.instance(this.mActivity).hideViewFinderFrame();
            exitWhiteBalanceLockMode();
        }
        return true;
    }

    private void saveStatusBeforeBurst() {
        this.mMultiSnapStatus = true;
        if (!this.mMutexModePicker.isBurstShoot()) {
            List<String> override = new ArrayList();
            override.addAll(Arrays.asList(new String[]{"pref_qc_camera_iso_key", null, "pref_qc_camera_exposuretime_key", null, "pref_camera_face_beauty_key", null, "pref_camera_shader_coloreffect_key", null}));
            String flash = getUIController().getFlashButton().getValue();
            if (!("off".equals(flash) || "torch".equals(flash))) {
                override.add("pref_camera_flashmode_key");
                override.add("off");
            }
            this.mSettingsOverrider.overrideSettings((String[]) override.toArray(new String[override.size()]));
        }
        stopObjectTracking(false);
        setCameraState(3);
        setCameraParameters(-1);
        getUIController().getEffectCropView().updateVisible();
        getUIController().getFlashButton().refreshValue();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    private void restoreStatusAfterBurst() {
        this.mMultiSnapStatus = false;
        this.mSnapshotOnIdle = false;
        setupPreview();
        if (!this.mMutexModePicker.isBurstShoot() && this.mSettingsOverrider.restoreSettings()) {
            setCameraParameters(2);
            getUIController().getFlashButton().refreshValue();
            getUIController().getSettingsStatusBar().updateStatus();
            getUIController().getEffectCropView().updateVisible();
        }
    }

    private void stopMultiSnap() {
        animateCapture();
        cancelContinuousShot();
        this.mHandler.removeMessages(30);
        this.mMultiSnapStopRequest = false;
        if (Device.isMTKPlatform()) {
            this.mHandler.sendEmptyMessageDelayed(37, 5000);
        } else {
            handleMultiSnapDone();
        }
    }

    protected void handleMultiSnapDone() {
        if (!this.mPaused) {
            restoreStatusAfterBurst();
            final int burstNum = this.mReceivedJpegCallbackNum;
            this.mHandler.post(new Runnable() {
                public void run() {
                    if (burstNum > 1) {
                        String toastStart = CameraModule.this.getResources().getString(R.string.toast_burst_snap_finished_start);
                        Toast.makeText(CameraModule.this.mActivity, toastStart + " " + burstNum + " " + CameraModule.this.getResources().getString(R.string.toast_burst_snap_finished_end), 0).show();
                    }
                    if (!CameraModule.this.mMultiSnapStatus) {
                        CameraModule.this.getUIController().getMultiSnapNum().setVisibility(8);
                    }
                    CameraModule.this.mBurstShotTitle = null;
                    CameraModule.this.mUpdateImageTitle = false;
                }
            });
            updateHDRPreference();
        }
    }

    protected void cancelContinuousShot() {
        this.mCameraDevice.cancelPicture();
    }

    private void installIntentFilter() {
        IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
        intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
        intentFilter.addAction("android.intent.action.MEDIA_CHECKING");
        intentFilter.addDataScheme("file");
        this.mActivity.registerReceiver(this.mReceiver, intentFilter);
        this.mDidRegister = true;
    }

    public void onResumeBeforeSuper() {
        super.onResumeBeforeSuper();
    }

    public void onResumeAfterSuper() {
        super.onResumeAfterSuper();
        if (!this.mOpenCameraFail && !this.mCameraDisabled && PermissionManager.checkCameraLaunchPermissions()) {
            if (!(this.mIsImageCaptureIntent && getUIController().getReviewDoneView().getVisibility() == 0)) {
                this.mKeepBitmapTexture = false;
                this.mActivity.getCameraScreenNail().releaseBitmapIfNeeded();
                getUIController().onResume();
            }
            this.mJpegPictureCallbackTime = 0;
            updateStereoSettings(true);
            if (this.mCameraStartUpThread == null && (this.mCameraState == 0 || this.mCameraDevice == null)) {
                this.mCameraStartUpThread = new CameraStartUpThread();
                this.mCameraStartUpThread.start();
                if (Util.checkDeviceHasNavigationBar(this.mActivity)) {
                    CameraSettings.changeUIByPreviewSize(this.mActivity, this.mUIStyle);
                }
            } else if (this.mWaitForRelease) {
                resumePreview();
            }
            this.mWaitForRelease = false;
            if (!this.mIsImageCaptureIntent) {
                this.mActivity.getThumbnailUpdater().getLastThumbnail();
            }
            if (this.mFirstTimeInitialized) {
                initializeSecondTime();
            } else {
                this.mHandler.sendEmptyMessage(1);
            }
            keepScreenOnAwhile();
            this.mActivity.loadCameraSound(1);
            this.mActivity.loadCameraSound(0);
            this.mActivity.loadCameraSound(4);
            this.mActivity.loadCameraSound(5);
            this.mActivity.loadCameraSound(7);
            this.mAudioCaptureManager.onResume();
        }
    }

    private void resumePreview() {
        startPreview();
        this.mHandler.sendEmptyMessage(9);
    }

    public void onPauseBeforeSuper() {
        resetMetaDataManager();
        super.onPauseBeforeSuper();
        if (!this.mMutexModePicker.isNormal()) {
            updateExitButton(true);
        }
        hideLoadUI(true);
        if (this.mMultiSnapStatus) {
            this.mMultiSnapStatus = false;
            setCameraState(1);
            if (Device.isQcomPlatform() || Device.isLCPlatform() || Device.isMTKPlatform()) {
                cancelContinuousShot();
            }
            getUIController().getMultiSnapNum().setVisibility(8);
        }
        exitWhiteBalanceLockMode();
        this.mAudioCaptureManager.onPause();
        resetGradienter();
        resetFaceBeautyMode();
        updateLyingSensorState(false);
        updateStereoSettings(false);
        waitCameraStartUpThread();
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            this.mCameraDevice.cancelAutoFocus();
        }
        stopFaceDetection(true);
        if (this.mActivity.isGotoGallery() ? Device.isReleaseLaterForGallery() : false) {
            this.mWaitForRelease = true;
            enableCameraControls(false);
        } else {
            releaseResources();
        }
        getUIController().onPause();
        if (!(this.mActivity.isActivityPaused() || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key"))) {
            PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
        }
        if (this.mEffectProcessor != null) {
            this.mEffectProcessor.setImageSaver(null);
            this.mEffectProcessor.release();
            this.mEffectProcessor = null;
        }
        if (this.mDidRegister) {
            this.mActivity.unregisterReceiver(this.mReceiver);
            this.mDidRegister = false;
        }
        if (this.mFocusManager != null) {
            this.mFocusManager.removeMessages();
        }
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(6);
        this.mHandler.removeMessages(7);
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(9);
        this.mHandler.removeMessages(22);
        this.mHandler.removeMessages(10);
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(15);
        this.mHandler.removeMessages(24);
        this.mHandler.removeMessages(25);
        this.mHandler.removeMessages(29);
        this.mHandler.removeMessages(30);
        this.mHandler.removeMessages(31);
        this.mHandler.removeMessages(32);
        this.mHandler.removeMessages(34);
        this.mHandler.removeMessages(37);
        this.mHandler.removeMessages(36);
        this.mHandler.removeMessages(40);
        this.mHandler.removeMessages(43);
        this.mSetCameraParameter = 0;
        this.mIsRecreateCameraScreenNail = false;
        tryRemoveCountDownMessage();
        this.mActivity.getSensorStateManager().reset();
        resetScreenOn();
        updateWarningMessage(0, true);
        QRCodeManager.instance(this.mActivity).onPause();
        this.mPendingSwitchCameraId = -1;
        this.mSwitchingCamera = false;
        this.mSwitchCameraAnimationRunning = false;
        if (this.mHasPendingSwitching) {
            int w = getUIController().getPreviewFrame().getHeight();
            int h = getUIController().getPreviewFrame().getWidth();
            if (!(w == 0 || h == 0)) {
                this.mUIStyle = CameraSettings.getUIStyleByPreview(w, h);
            }
            this.mHasPendingSwitching = false;
        }
        if (this.mMutexModePicker.isUbiFocus() && this.mTotalJpegCallbackNum == 7 && this.mReceivedJpegCallbackNum > 0 && this.mReceivedJpegCallbackNum < this.mTotalJpegCallbackNum) {
            Storage.deleteImage(this.mBurstShotTitle);
        }
    }

    protected void exitMutexMode() {
        boolean isDetectedHHT = (this.mMutexModePicker.getLastMutexMode() == 0 || this.mMutexModePicker.getLastMutexMode() == 4 || this.mMutexModePicker.getLastMutexMode() == 2 || this.mMutexModePicker.getLastMutexMode() == 5 || this.mMutexModePicker.getLastMutexMode() == 1) ? true : this.mMutexModePicker.getLastMutexMode() == 3 ? isDetectedHHT() : false;
        if (!isDetectedHHT) {
            getUIController().getFlashButton().restoreKeptValue();
        }
        this.mSettingsOverrider.restoreSettings();
        updateWarningMessage(0, true);
        if (!(this.m3ALocked || CameraSettings.getFocusMode().equals(getString(R.string.pref_camera_focusmode_value_default)))) {
            CameraSettings.setFocusModeSwitching(true);
        }
        if (this.mCameraState == 3) {
            startPreview();
        } else {
            setCameraParameters(2);
        }
        checkRestartPreview();
        getUIController().getEffectCropView().updateVisible();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    protected void enterMutexMode() {
        if (!(this.mMutexModePicker.isHdr() || isDetectedHHT() || this.mMutexModePicker.isSupportedFlashOn())) {
            getUIController().getFlashButton().keepSetValue("off");
        }
        setOrientationIndicator(this.mOrientationCompensation, false);
        if (this.mMutexModePicker.isUbiFocus()) {
            setZoomValue(0);
        }
        if (this.mMutexModePicker.isBurstShoot()) {
            Util.clearMemoryLimit();
        }
        if (!CameraSettings.getFocusMode().equals(getString(R.string.pref_camera_focusmode_value_default))) {
            CameraSettings.setFocusModeSwitching(true);
        }
        List<String> override = new ArrayList();
        override.addAll(Arrays.asList(new String[]{"pref_qc_camera_iso_key", null, "pref_qc_camera_exposuretime_key", null, "pref_camera_face_beauty_key", null, "pref_camera_focus_mode_key", null, "pref_camera_whitebalance_key", null, "pref_camera_coloreffect_key", null}));
        if (this.mMutexModePicker.isUbiFocus()) {
            getUIController().getFocusView().clear();
            updateWarningMessage(R.string.ubifocus_capture_warning_message, false);
        }
        if (this.mMutexModePicker.isUbiFocus() || this.mMutexModePicker.isBurstShoot()) {
            override.add("pref_camera_shader_coloreffect_key");
            override.add(null);
        }
        if (!(this.mMutexModePicker.isHdr() || this.mMutexModePicker.isHandNight())) {
            override.add("pref_camera_exposure_key");
            override.add(null);
        }
        this.mSettingsOverrider.overrideSettings((String[]) override.toArray(new String[override.size()]));
        setCameraParameters(2);
        checkRestartPreview();
        getUIController().getEffectCropView().updateVisible();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    private void waitCameraStartUpThread() {
        try {
            if (this.mCameraStartUpThread != null) {
                this.mCameraStartUpThread.cancel();
                this.mCameraStartUpThread.join();
                this.mCameraStartUpThread = null;
                setCameraState(1);
            }
        } catch (InterruptedException e) {
        }
    }

    private void initializeFocusManager() {
        boolean z = false;
        String[] defaultFocusModes = getResources().getStringArray(R.array.pref_camera_focusmode_default_array);
        Context context = this.mActivity;
        CameraSettingPreferences cameraSettingPreferences = this.mPreferences;
        FocusView focusView = getUIController().getFocusView();
        Parameters parameters = this.mInitialParams;
        if (this.mCameraId == CameraHolder.instance().getFrontCameraId()) {
            z = true;
        }
        this.mFocusManager = new FocusManager(context, cameraSettingPreferences, defaultFocusModes, focusView, parameters, this, z, this.mActivity.getMainLooper());
        Rect rect = null;
        if (this.mActivity.getCameraScreenNail() != null) {
            rect = this.mActivity.getCameraScreenNail().getRenderRect();
        }
        if (rect == null || rect.width() <= 0) {
            this.mFocusManager.setRenderSize(Util.sWindowWidth, Util.sWindowHeight);
            this.mFocusManager.setPreviewSize(Util.sWindowWidth, Util.sWindowHeight);
            return;
        }
        this.mFocusManager.setRenderSize(this.mActivity.getCameraScreenNail().getRenderWidth(), this.mActivity.getCameraScreenNail().getRenderHeight());
        this.mFocusManager.setPreviewSize(rect.width(), rect.height());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1000:
                Intent intent = new Intent();
                if (data != null) {
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        intent.putExtras(extras);
                    }
                }
                this.mActivity.setResult(resultCode, intent);
                this.mActivity.finish();
                this.mActivity.getFileStreamPath("crop-temp").delete();
                return;
            default:
                return;
        }
    }

    private boolean canTakePicture() {
        return isCameraIdle() && !Storage.isLowStorageAtLastPoint();
    }

    public void autoFocus() {
        this.mFocusStartTime = System.currentTimeMillis();
        if (this.mFirstTimeInitialized && this.mFocusAreaSupported && !this.mSwitchingCamera) {
            this.mCameraDevice.autoFocus(this.mAutoFocusCallback);
            setCameraState(2);
        }
    }

    public void cancelAutoFocus() {
        this.mCameraDevice.cancelAutoFocus();
        if (this.mParameters.getExposureCompensation() != 0) {
            this.mKeepAdjustedEv = true;
        }
        if (this.mCameraState != 3) {
            setCameraState(1);
            setCameraParameters(-1);
        }
    }

    public void onSingleTapUp(int x, int y) {
        Log.v("Camera", "onSingleTapUp " + this.mPaused + " " + this.mCameraDevice + " " + this.mFirstTimeInitialized + " " + this.mCameraState + " " + this.mMultiSnapStatus + " " + this);
        getUIController().getEffectButton().dismissPopup();
        getUIController().getZoomButton().dismissPopup();
        getUIController().getPreviewPage().simplifyPopup(true, true);
        this.m3ALocked = false;
        this.mFocusManager.setAeAwbLock(false);
        if (!this.mPaused && this.mCameraDevice != null && this.mFirstTimeInitialized && !this.mActivity.getCameraScreenNail().isModuleSwitching() && isInTapableRect(x, y) && this.mCameraState != 3 && this.mCameraState != 4 && this.mCameraState != 0 && !this.mMultiSnapStatus) {
            tryRemoveCountDownMessage();
            if ((this.mFocusAreaSupported || this.mMeteringAreaSupported) && !isSelectingCapturedImage()) {
                QRCodeManager.instance(this.mActivity).hideViewFinderFrame();
                if (!this.mMutexModePicker.isUbiFocus()) {
                    if (this.mObjectTrackingStarted) {
                        stopObjectTracking(true);
                    }
                    getUIController().getFocusView().setFocusType(true);
                    showObjectTrackToastIfNeeded();
                    Point point = new Point(x, y);
                    mapTapCoordinate(point);
                    this.mFocusManager.onSingleTapUp(point.x, point.y);
                    if (!this.mFocusAreaSupported && this.mMeteringAreaSupported) {
                        this.mActivity.getSensorStateManager().reset();
                    }
                }
            }
        }
    }

    public boolean onBackPressed() {
        boolean z = false;
        AutoLockManager.getInstance(this.mActivity).onUserInteraction();
        if (this.mCameraStartUpThread != null) {
            return false;
        }
        if (isSelectingCapturedImage()) {
            onReviewCancelClicked(getUIController().getReviewCanceledView());
            return true;
        } else if ((!this.mMutexModePicker.isNormal() && this.mCameraState == 3) || QRCodeManager.instance(this.mActivity).onBackPressed() || exitWhiteBalanceLockMode()) {
            return true;
        } else {
            tryRemoveCountDownMessage();
            if (getUIController().onBack() || this.mCameraState == 3) {
                return true;
            }
            if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                getUIController().getStereoButton().setStereoValue(false, true, true);
                return true;
            } else if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                getUIController().getPortraitButton().switchOff();
                if (!Util.isPortraitIntent(this.mActivity)) {
                    z = true;
                }
                return z;
            } else {
                if (getUIController().getSettingPage().isItemSelected()) {
                    boolean result = getUIController().getSettingPage().resetSettings();
                    if (this.mAudioCaptureManager.isRunning()) {
                        this.mAudioCaptureManager.close();
                    }
                    if (result) {
                        return true;
                    }
                }
                if (getUIController().getPreviewPage().isPopupShown()) {
                    PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
                    return true;
                } else if (this.mAudioCaptureManager.onBackPressed()) {
                    return true;
                } else {
                    return super.onBackPressed();
                }
            }
        }
    }

    private boolean isSelectingCapturedImage() {
        if (this.mIsImageCaptureIntent) {
            return getUIController().getReviewDoneView().isVisibleWithAnimationDone();
        }
        return false;
    }

    private boolean isPreviewVisible() {
        if (this.mFirstTimeInitialized && getUIController().getPreviewPage().isPreviewPageVisible() && getUIController().getReviewDoneView().getVisibility() != 0) {
            return true;
        }
        return false;
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean z = false;
        switch (keyCode) {
            case 24:
            case 25:
                if (isPreviewVisible()) {
                    if (keyCode == 24) {
                        z = true;
                    }
                    if (handleVolumeKeyEvent(z, true, event.getRepeatCount())) {
                        return true;
                    }
                }
                break;
            case 27:
            case 66:
                if (event.getRepeatCount() == 0 && isPreviewVisible()) {
                    onShutterButtonClick();
                    if (Util.isFingerPrintKeyEvent(event)) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_finger");
                    }
                }
                return true;
            case 80:
                if (event.getRepeatCount() == 0 && isPreviewVisible()) {
                    onShutterButtonFocus(true, 1);
                }
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 24:
            case 25:
                if (isPreviewVisible()) {
                    boolean z;
                    if (keyCode == 24) {
                        z = true;
                    } else {
                        z = false;
                    }
                    if (handleVolumeKeyEvent(z, false, event.getRepeatCount())) {
                        return true;
                    }
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    protected void performVolumeKeyClicked(int repeatCount, boolean pressed) {
        if (isShutterButtonClickable()) {
            if (repeatCount == 0) {
                if (pressed) {
                    onShutterButtonFocus(true, 2);
                    onShutterButtonClick();
                    if (!isCountDownMode()) {
                        CameraDataAnalytics.instance().trackEvent("capture_times_volume");
                    }
                    if (this.mParameters.getMeteringAreas() != null) {
                        CameraDataAnalytics.instance().trackEvent("touch_focus_volume_capture_times_key");
                    }
                } else {
                    onShutterButtonFocus(false, 0);
                    this.mVolumeLongPress = false;
                }
            } else if (pressed && !this.mVolumeLongPress) {
                onShutterButtonLongClick();
                this.mVolumeLongPress = true;
                this.mUpdateImageTitle = true;
            }
        }
    }

    protected void closeCamera() {
        Log.v("Camera", "closeCamera");
        if (this.mCameraDevice != null) {
            if (this.mSetMetaCallback) {
                this.mSetMetaCallback = false;
                this.mCameraDevice.setMetaDataCallback(null);
            }
            this.mCameraDevice.setFaceDetectionListener(null);
            this.mCameraDevice.setErrorCallback(null);
            this.mCameraDevice.setOneShotPreviewCallback(null);
            this.mCameraDevice.setAutoFocusMoveCallback(null);
            this.mCameraDevice.addRawImageCallbackBuffer(null);
            this.mCameraDevice.removeAllAsyncMessage();
            CameraHolder.instance().release();
            this.mFaceDetectionStarted = false;
            this.m3ALocked = false;
            this.mCameraDevice = null;
            setCameraState(0);
            if (this.mFocusManager != null && currentIsMainThread()) {
                this.mFocusManager.onCameraReleased();
            }
        }
    }

    protected void setDisplayOrientation() {
        super.setDisplayOrientation();
        getUIController().getFaceView().setDisplayOrientation(this.mCameraDisplayOrientation);
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mCameraDisplayOrientation);
        }
    }

    protected void startPreview() {
        if (this.mCameraDevice != null && this.mFocusManager != null) {
            if (currentIsMainThread()) {
                this.mFocusManager.resetTouchFocus();
            }
            this.mCameraDevice.setErrorCallback(this.mErrorCallback);
            if (!(this.mCameraState == 0 || (Device.isMTKPlatform() && isZeroShotMode()))) {
                stopPreview();
            }
            setDisplayOrientation();
            this.mCameraDevice.setDisplayOrientation(this.mCameraDisplayOrientation);
            if (!this.mSnapshotOnIdle) {
                if ("continuous-picture".equals(this.mFocusManager.getFocusMode())) {
                    this.mCameraDevice.cancelAutoFocus();
                }
                this.mFocusManager.setAeAwbLock(false);
            }
            this.mFoundFace = false;
            this.mKeepAdjustedEv = false;
            if (currentIsMainThread()) {
                setCameraParameters(-1);
            }
            this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraScreenNail().getSurfaceTexture());
            Log.v("Camera", "startPreview");
            this.mCameraDevice.startPreview();
            this.mFocusManager.onPreviewStarted();
            if (currentIsMainThread()) {
                setCameraState(1);
            }
            if (this.mSnapshotOnIdle) {
                this.mHandler.post(this.mDoSnapRunnable);
            }
        }
    }

    private void stopPreview() {
        if (currentIsMainThread()) {
            stopObjectTracking(false);
        }
        if (!(this.mCameraDevice == null || this.mCameraState == 0)) {
            Log.v("Camera", "stopPreview");
            this.mCameraDevice.stopPreview();
            this.mFaceDetectionStarted = false;
        }
        if (currentIsMainThread()) {
            setCameraState(0);
        } else {
            this.mCameraState = 0;
        }
        if (currentIsMainThread() && this.mFocusManager != null) {
            this.mFocusManager.onPreviewStopped();
        }
    }

    protected void updateCameraParametersInitialize() {
        List<Integer> frameRates = this.mParameters.getSupportedPreviewFrameRates();
        if (frameRates != null) {
            this.mParameters.setPreviewFrameRate(((Integer) Collections.max(frameRates)).intValue());
        }
        this.mParameters.setRecordingHint(false);
        if ("true".equals(this.mParameters.get("video-stabilization-supported"))) {
            this.mParameters.set("video-stabilization", "false");
        }
    }

    protected void updateCameraParametersPreference() {
        if (this.mAeLockSupported) {
            this.mParameters.setAutoExposureLock(this.mFocusManager.getAeAwbLock());
        }
        if (this.mAwbLockSupported) {
            this.mParameters.setAutoWhiteBalanceLock(this.mFocusManager.getAeAwbLock());
        }
        PictureSize pictureSize = getBestPictureSize();
        if (pictureSize != null) {
            Log.d("Camera", "pictureSize = " + pictureSize);
            Size oldPictureSize = this.mParameters.getPictureSize();
            if (!(oldPictureSize.width == pictureSize.width && oldPictureSize.height == pictureSize.height)) {
                stopObjectTracking(true);
            }
            this.mParameters.setPictureSize(pictureSize.width, pictureSize.height);
        } else {
            Log.e("Camera", "get null pictureSize");
            pictureSize = PictureSizeManager.getPictureSize(false);
        }
        Size optimalSize = Util.getOptimalPreviewSize(this.mActivity, sProxy.getSupportedPreviewSizes(this.mParameters), (double) CameraSettings.getPreviewAspectRatio(pictureSize.width, pictureSize.height));
        Size original = this.mParameters.getPreviewSize();
        int style = CameraSettings.getUIStyleByPreview(optimalSize.width, optimalSize.height);
        if (!original.equals(optimalSize)) {
            this.mParameters.setPreviewSize(optimalSize.width, optimalSize.height);
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
        }
        if (this.mUIStyle != style) {
            this.mUIStyle = style;
            if (!this.mSwitchingCamera || Device.isMDPRender()) {
                this.mHandler.sendEmptyMessage(18);
            } else {
                this.mHasPendingSwitching = true;
            }
        }
        this.mPreviewWidth = optimalSize.width;
        this.mPreviewHeight = optimalSize.height;
        if (21 <= VERSION.SDK_INT) {
            optimalSize = Util.getOptimalJpegThumbnailSize(this.mParameters.getSupportedJpegThumbnailSizes(), ((double) pictureSize.width) / ((double) pictureSize.height));
            if (!this.mParameters.getJpegThumbnailSize().equals(optimalSize)) {
                this.mParameters.setJpegThumbnailSize(optimalSize.width, optimalSize.height);
            }
            Log.v("Camera", "Thumbnail size is " + optimalSize.width + "x" + optimalSize.height);
        }
        if (this.mMutexModePicker.isSceneHdr()) {
            this.mSceneMode = "hdr";
            if (!("auto".equals(this.mParameters.getSceneMode()) || "hdr".equals(this.mParameters.getSceneMode()))) {
                this.mParameters.setSceneMode("auto");
                this.mCameraDevice.setParameters(this.mParameters);
                this.mParameters = this.mCameraDevice.getParameters();
            }
        } else if (CameraSettings.isSwitchOn("pref_camera_scenemode_setting_key")) {
            this.mSceneMode = this.mPreferences.getString("pref_camera_scenemode_key", getString(R.string.pref_camera_scenemode_default));
        } else {
            this.mSceneMode = "auto";
        }
        Log.v("Camera", "mSceneMode " + this.mSceneMode + " getMutexMode=" + this.mMutexModePicker.getMutexMode());
        if (!BaseModule.isSupported(this.mSceneMode, this.mParameters.getSupportedSceneModes())) {
            this.mSceneMode = this.mParameters.getSceneMode();
            if (this.mSceneMode == null) {
                this.mSceneMode = "auto";
            }
        } else if (!this.mParameters.getSceneMode().equals(this.mSceneMode)) {
            Log.v("Camera", "mSceneMode " + this.mSceneMode + " pas=" + this.mParameters.getSceneMode());
            this.mParameters.setSceneMode(this.mSceneMode);
            this.mCameraDevice.setParameters(this.mParameters);
            this.mParameters = this.mCameraDevice.getParameters();
        }
        String jpegQuality = CameraSettings.getJpegQuality(this.mPreferences, this.mMultiSnapStatus);
        Log.i("Camera", "jpegQuality : " + jpegQuality);
        this.mParameters.setJpegQuality(JpegEncodingQualityMappings.getQualityNumber(jpegQuality));
        int value = CameraSettings.readExposure(this.mPreferences);
        Log.i("Camera", "EV : " + value);
        int max = this.mParameters.getMaxExposureCompensation();
        if (value < this.mParameters.getMinExposureCompensation() || value > max) {
            Log.w("Camera", "invalid exposure range: " + value);
        } else {
            this.mParameters.setExposureCompensation(value);
        }
        if (Device.isSupportedShaderEffect()) {
            int effect = CameraSettings.getShaderEffect();
            Log.v("Camera", "Shader color effect value =" + effect);
            EffectController.getInstance().setEffect(effect);
            if (EffectController.getInstance().hasEffect() && this.mEffectProcessor == null) {
                this.mEffectProcessor = new SnapshotEffectRender(this.mActivity, this.mIsImageCaptureIntent);
                this.mEffectProcessor.setImageSaver(this.mActivity.getImageSaver());
            }
            if (this.mEffectProcessor != null) {
                this.mEffectProcessor.prepareEffectRender(effect);
                this.mEffectProcessor.setQuality(this.mParameters.getJpegQuality());
            }
        } else {
            String colorEffect = this.mPreferences.getString("pref_camera_coloreffect_key", getString(R.string.pref_camera_coloreffect_default));
            Log.v("Camera", "Color effect value =" + colorEffect);
            if (BaseModule.isSupported(colorEffect, this.mParameters.getSupportedColorEffects())) {
                this.mParameters.setColorEffect(colorEffect);
            }
        }
        String autoExposure = this.mPreferences.getString("pref_camera_autoexposure_key", getString(R.string.pref_camera_autoexposure_default));
        Log.v("Camera", "autoExposure value =" + autoExposure);
        setAutoExposure(this.mParameters, autoExposure);
        String antiBanding = this.mPreferences.getString("pref_camera_antibanding_key", getString(CameraSettings.getDefaultPreferenceId(R.string.pref_camera_antibanding_default)));
        Log.v("Camera", "antiBanding value =" + antiBanding);
        if (BaseModule.isSupported(antiBanding, this.mParameters.getSupportedAntibanding())) {
            this.mParameters.setAntibanding(antiBanding);
        }
        String flashMode;
        if ("auto".equals(this.mSceneMode) || "hdr".equals(this.mSceneMode)) {
            String focusMode;
            if (!this.m3ALocked) {
                this.mFocusManager.overrideFocusMode(null);
            }
            List<String> supportedFlash = this.mParameters.getSupportedFlashModes();
            if (supportedFlash != null && supportedFlash.size() > 0) {
                String flashModeOld = this.mParameters.getFlashMode();
                flashMode = getRequestFlashMode();
                if (BaseModule.isSupported(flashMode, supportedFlash)) {
                    this.mParameters.setFlashMode(flashMode);
                }
                if (this.mMutexModePicker.isHdr() && !"off".equals(flashModeOld) && !"torch".equals(flashModeOld) && BaseModule.isSupported("off", supportedFlash)) {
                    this.mParameters.setFlashMode("off");
                    if (this.mMutexModePicker.isAoHdr()) {
                        this.mCameraDevice.setParameters(this.mParameters);
                        this.mParameters = this.mCameraDevice.getParameters();
                    }
                }
            }
            if (CameraSettings.isFocusModeSwitching()) {
                CameraSettings.setFocusModeSwitching(false);
                if (this.mCameraStartUpThread == null) {
                    this.mFocusManager.resetFocusStateIfNeeded();
                }
            }
            if ((this.mMultiSnapStatus || this.mObjectTrackingStarted) && this.mFocusAreaSupported) {
                focusMode = "auto";
            } else {
                focusMode = this.mFocusManager.getFocusMode();
            }
            if (focusMode != null) {
                if (!Device.isQcomPlatform() || this.mCameraState != 0 || !"manual".equals(focusMode)) {
                    sProxy.setFocusMode(this.mParameters, focusMode);
                }
                if ("macro".equals(focusMode) || "manual".equals(focusMode)) {
                    stopObjectTracking(true);
                }
            }
            Log.i("Camera", "Focus mode value = " + focusMode);
            String whiteBalance = getManualValue("pref_camera_whitebalance_key", getString(R.string.pref_camera_whitebalance_default));
            if (BaseModule.isSupported(whiteBalance, this.mParameters.getSupportedWhiteBalance())) {
                sProxy.setWhiteBalance(this.mParameters, whiteBalance);
            } else if (this.mParameters.getWhiteBalance() == null) {
                whiteBalance = "auto";
            }
        } else {
            flashMode = getRequestFlashMode();
            if (BaseModule.isSupported(flashMode, this.mParameters.getSupportedFlashModes())) {
                this.mParameters.setFlashMode(flashMode);
            }
            if (CameraSettings.isFocusModeSwitching() && isBackCamera()) {
                CameraSettings.setFocusModeSwitching(false);
                if (this.mCameraStartUpThread == null) {
                    this.mFocusManager.resetFocusStateIfNeeded();
                }
            }
            sProxy.setFocusMode(this.mParameters, "continuous-picture");
            this.mFocusManager.overrideFocusMode("continuous-picture");
        }
        if (this.mFocusAreaSupported) {
            sProxy.setFocusAreas(this.mParameters, this.mFocusManager.getFocusAreas());
        }
        if (this.mMeteringAreaSupported) {
            sProxy.setMeteringAreas(this.mParameters, this.mFocusManager.getMeteringAreas());
        }
        if (this.mContinuousFocusSupported) {
            if (this.mParameters.getFocusMode().equals("continuous-picture")) {
                this.mCameraDevice.setAutoFocusMoveCallback(this.mAutoFocusMoveCallback);
            } else {
                this.mCameraDevice.setAutoFocusMoveCallback(null);
            }
        }
        boolean faceDetection = true;
        if (this.mMultiSnapStatus || this.mMutexModePicker.isUbiFocus() || CameraSettings.isSwitchOn("pref_camera_gradienter_key") || CameraSettings.isSwitchOn("pref_camera_tilt_shift_mode")) {
            faceDetection = false;
        } else if (!(CameraSettings.isSwitchOn("pref_camera_magic_mirror_key") || CameraSettings.isSwitchOn("pref_camera_portrait_mode_key") || CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key"))) {
            faceDetection = this.mPreferences.getBoolean("pref_camera_facedetection_key", getResources().getBoolean(CameraSettings.getDefaultPreferenceId(R.bool.pref_camera_facedetection_default)));
        }
        getUIController().getFaceView().setSkipDraw(!faceDetection);
        if (faceDetection) {
            if (!this.mFaceDetectionEnabled) {
                this.mFaceDetectionEnabled = true;
                startFaceDetection();
            }
        } else if (this.mFaceDetectionEnabled) {
            stopFaceDetection(true);
            this.mFaceDetectionEnabled = false;
        }
        if (this.mParameters.isZoomSupported()) {
            this.mParameters.setZoom(getZoomValue());
        }
        this.mActivity.getSensorStateManager().setFocusSensorEnabled(this.mFocusManager.getMeteringAreas() != null);
        QRCodeManager.instance(this.mActivity).needScanQRCode(CameraSettings.isScanQRCode(this.mPreferences));
        QRCodeManager.instance(this.mActivity).setTransposePreviewSize(this.mPreviewWidth, this.mPreviewHeight);
        QRCodeManager.instance(this.mActivity).setPreviewFormat(this.mParameters.getPreviewFormat());
        addMuteToParameters(this.mParameters);
        if ((!CameraSettings.isSupportedOpticalZoom() || this.mParameters.getZoom() <= 0 || CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) && !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            configOisParameters(this.mParameters, true);
        } else {
            configOisParameters(this.mParameters, false);
        }
        addT2TParameters(this.mParameters);
        if (!this.mSwitchingCamera || CameraSettings.sCroppedIfNeeded) {
            updateCameraScreenNailSize(this.mPreviewWidth, this.mPreviewHeight, this.mFocusManager);
        }
        if (Device.isFaceDetectNeedRotation()) {
            this.mParameters.setRotation(Util.getJpegRotation(this.mCameraId, this.mOrientation));
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.mActivity.getScreenHint().hideToast();
        this.mJpegImageData = null;
        this.mNeedSealCameraUUID = false;
        this.mCameraUUIDWatermarkImageData = null;
    }

    private void onFrameLayoutChange(View v, Rect rect) {
        this.mActivity.onLayoutChange(rect.width(), rect.height());
        if (this.mActivity.getCameraScreenNail() != null) {
            this.mActivity.getCameraScreenNail().setRenderArea(rect);
        }
        if (!(this.mFocusManager == null || this.mActivity.getCameraScreenNail() == null)) {
            this.mFocusManager.setRenderSize(this.mActivity.getCameraScreenNail().getRenderWidth(), this.mActivity.getCameraScreenNail().getRenderHeight());
            this.mFocusManager.setPreviewSize(rect.width(), rect.height());
        }
        if (getUIController().getObjectView() != null) {
            getUIController().getObjectView().setDisplaySize(rect.right - rect.left, rect.bottom - rect.top);
        }
        QRCodeManager.instance(this.mActivity).setPreviewLayoutSize(rect.width(), rect.height());
    }

    protected void setCameraParameters(int updateSet) {
        this.mParameters = this.mCameraDevice.getParameters();
        if ((updateSet & 1) != 0) {
            updateCameraParametersInitialize();
        }
        if ((updateSet & 2) != 0) {
            updateCameraParametersPreference();
            this.mSetCameraParameter &= -2;
        }
        this.mCameraDevice.setParameters(this.mParameters);
    }

    protected void setCameraParametersWhenIdle(int additionalUpdateSet) {
        this.mUpdateSet |= additionalUpdateSet;
        if (this.mCameraDevice == null) {
            this.mUpdateSet = 0;
            return;
        }
        if (isCameraIdle()) {
            setCameraParameters(this.mUpdateSet);
            checkRestartPreview();
            this.mRestartPreview = false;
            setPreviewFrameLayoutAspectRatio();
            updateSceneModeUI();
            exitWhiteBalanceLockMode();
            getUIController().getSettingsStatusBar().updateStatus();
            this.mUpdateSet = 0;
        } else if (!this.mHandler.hasMessages(3)) {
            this.mHandler.sendEmptyMessageDelayed(3, 1000);
        }
    }

    protected void openSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(this.mActivity, CameraPreferenceActivity.class);
        intent.putExtra("from_where", 1);
        intent.putExtra("IsCaptureIntent", this.mIsImageCaptureIntent);
        intent.putExtra(":miui:starting_window_label", getResources().getString(R.string.pref_camera_settings_category));
        if (this.mActivity.startFromKeyguard()) {
            intent.putExtra("StartActivityWhenLocked", true);
        }
        this.mActivity.startActivity(intent);
        this.mActivity.setJumpFlag(2);
        CameraDataAnalytics.instance().trackEvent("pref_settings");
    }

    protected int getMutexHdrMode(String hdr) {
        if (getString(R.string.pref_camera_hdr_entryvalue_normal).equals(hdr)) {
            int i;
            if (!Device.isUsedMorphoLib() || (Device.isMTKPlatform() && !Device.isSupportedAsdHdr())) {
                i = 5;
            } else {
                i = 1;
            }
            return i;
        } else if (Device.isSupportedAoHDR() && getString(R.string.pref_camera_hdr_entryvalue_live).equals(hdr)) {
            return 2;
        } else {
            return 0;
        }
    }

    private boolean isCameraIdle() {
        if (this.mCameraState == 1) {
            return true;
        }
        if (this.mFocusManager == null || !this.mFocusManager.isFocusCompleted()) {
            return false;
        }
        return (this.mCameraState == 4 || this.mCameraState == 3) ? false : true;
    }

    private void setupCaptureParams() {
        Bundle myExtras = this.mActivity.getIntent().getExtras();
        if (myExtras != null) {
            this.mSaveUri = (Uri) myExtras.getParcelable("output");
            this.mCropValue = myExtras.getString("crop");
            this.mIsSaveCaptureImage = myExtras.getBoolean("save-image", false);
            this.mNeedSealCameraUUID = Util.getNeedSealCameraUUIDIntentExtras(this.mActivity);
        }
        if (Util.isPortraitIntent(this.mActivity) && !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            this.mPreferences.edit().putString("pref_camera_portrait_mode_key", "on").apply();
        }
    }

    private void showPostCaptureAlert() {
        if (this.mIsImageCaptureIntent) {
            ignoreTouchEvent(true);
            this.mFocusManager.removeMessages();
            previewBecomeInvisible();
            getUIController().getSettingsStatusBar().hide();
            getUIController().getEffectCropView().hide();
            getUIController().getPreviewPage().setPopupVisible(false);
            getUIController().getZoomButton().setVisibility(8);
            getUIController().getShutterButton().animateOut(null, 100, true);
            getUIController().getBottomControlUpperPanel().animateOut(null);
            getUIController().getTopControlPanel().animateOut(null);
            getUIController().getReviewDoneView().animateIn(null, 100, true);
            getUIController().getOrientationIndicator().updateVisible(false);
            resetMetaDataManager();
        }
    }

    private void hidePostCaptureAlert() {
        if (this.mIsImageCaptureIntent) {
            ignoreTouchEvent(false);
            getUIController().getSettingsStatusBar().show();
            getUIController().getEffectCropView().show();
            getUIController().getPreviewPage().setPopupVisible(true);
            getUIController().getShutterButton().animateIn(null, 100, true);
            getUIController().getBottomControlUpperPanel().animateIn(null);
            getUIController().getTopControlPanel().animateIn(null);
            getUIController().getReviewDoneView().animateOut(null, 100, true);
            getUIController().getZoomButton().updateVisible();
        }
    }

    private void previewBecomeInvisible() {
        stopFaceDetection(true);
        stopPreview();
    }

    private void previewBecomeVisible() {
        this.mActivity.getCameraScreenNail().releaseBitmapIfNeeded();
        startPreview();
        startFaceDetection();
    }

    private void switchToOtherMode(int mode) {
        if (!this.mActivity.isFinishing()) {
            this.mHandler.removeMessages(1);
            this.mActivity.switchToOtherModule(mode);
        }
    }

    public void onSharedPreferenceChanged() {
        if (!this.mPaused) {
            LocationManager.instance().recordLocation(CameraSettings.isRecordLocation(this.mPreferences));
            setCameraParametersWhenIdle(2);
        }
    }

    private void onSettingsBack() {
        ChangeManager cm = CameraSettings.sCameraChangeManager;
        if (cm.check(3)) {
            cm.clear(3);
            restorePreferences();
        } else if (cm.check(1)) {
            cm.clear(1);
            onSharedPreferenceChanged();
        }
    }

    private void prepareSwitchCameraAnimation(PreviewTextureCopiedCallback callback) {
        this.mPreviewTextureCopiedCallback = callback;
        this.mActivity.getCameraScreenNail().animateSwitchCopyTexture();
        this.mSwitchCameraAnimationRunning = true;
    }

    public boolean onCameraPickerClicked(int cameraId) {
        if (this.mPaused || this.mPendingSwitchCameraId != -1 || this.mSwitchingCamera) {
            return false;
        }
        Log.v("Camera", "Start to copy texture. cameraId=" + cameraId + " " + CameraSettings.isBackCamera());
        if (Device.isMDPRender()) {
            this.mActivity.setBlurFlag(true);
            this.mHandler.sendEmptyMessage(6);
        } else if (this.mSwitchCameraAnimationRunning) {
            synchronized (this.mSwitchCameraLater) {
                if (this.mSwitchCameraLater.booleanValue()) {
                    this.mSwitchCameraLater = Boolean.valueOf(false);
                } else {
                    this.mHandler.sendEmptyMessage(6);
                }
            }
        } else {
            prepareSwitchCameraAnimation(this.mPreviewTextureCopiedActionSwitchCamera);
        }
        this.mPendingSwitchCameraId = cameraId;
        setCameraState(4);
        this.mSwitchingCamera = true;
        exitWhiteBalanceLockMode();
        QRCodeManager.instance(this.mActivity).hideViewFinderFrame();
        return true;
    }

    private void switchCamera() {
        boolean z = true;
        if (!this.mPaused) {
            updateWarningMessage(0, true);
            updateStereoSettings(false);
            resetMetaDataManager();
            if (!this.mMutexModePicker.isNormal()) {
                this.mMutexModePicker.resetMutexMode();
            }
            this.mAudioCaptureManager.onPause();
            tryRemoveCountDownMessage();
            Log.v("Camera", "Start to switch camera. id=" + this.mPendingSwitchCameraId);
            this.mCameraId = this.mPendingSwitchCameraId;
            this.mPendingSwitchCameraId = -1;
            CameraSettings.writePreferredCameraId(this.mPreferences, this.mCameraId);
            this.mActivity.changeRequestOrientation();
            CameraSettings.resetZoom(this.mPreferences);
            if (!isBackCamera()) {
                CameraSettings.resetCameraZoomMode();
            }
            CameraSettings.resetExposure();
            resetGradienter();
            resetFaceBeautyMode();
            CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
            updateExitButton(false);
            PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
            stopObjectTracking(false);
            closeCamera();
            getUIController().getFaceView().clear();
            getUIController().getEdgeShutterView().cancelAnimation();
            if (this.mFocusManager != null) {
                this.mFocusManager.removeMessages();
            }
            getUIController().updatePreferenceGroup();
            openCamera();
            if (hasCameraException()) {
                onCameraException();
                return;
            }
            initializeCapabilities();
            updateStereoSettings(true);
            FocusManager focusManager = this.mFocusManager;
            if (this.mCameraId != CameraHolder.instance().getFrontCameraId()) {
                z = false;
            }
            focusManager.setMirror(z);
            this.mFocusManager.setParameters(this.mParameters);
            setOrientationIndicator(this.mOrientationCompensation, false);
            getUIController().getFlashButton().avoidTorchOpen();
            startPreview();
            startFaceDetection();
            initializeAfterCameraOpen();
            enableCameraControls(false);
            getUIController().onCameraOpen();
            getUIController().getFocusView().initialize(this);
            getUIController().getObjectView().setObjectViewListener(this);
            onCameraStartPreview();
            updateModePreference();
            this.mAudioCaptureManager.onResume();
            this.mHandler.sendEmptyMessage(19);
        }
    }

    private void updateStereoSettings(boolean open) {
        if (!CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            return;
        }
        if (open) {
            this.mSettingsOverrider.overrideSettings("pref_camera_shader_coloreffect_key", null, "pref_camera_flashmode_key", "off", "pref_camera_hdr_key", "off");
            return;
        }
        this.mSettingsOverrider.restoreSettings();
    }

    private void updateModePreference() {
        if (!isFrontCamera()) {
            int hdr = getMutexHdrMode(getUIController().getHdrButton().getValue());
            if (hdr != 0) {
                getUIController().getFlashButton().updateFlashModeAccordingHdr(getUIController().getHdrButton().getValue());
                this.mMutexModePicker.setMutexMode(hdr);
            } else if (CameraSettings.isSwitchOn("pref_camera_hand_night_key")) {
                this.mMutexModePicker.setMutexMode(3);
            } else if (CameraSettings.isSwitchOn("pref_camera_ubifocus_key")) {
                this.mMutexModePicker.setMutexMode(6);
            } else {
                this.mMutexModePicker.resetMutexMode();
            }
            if (CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
                if (!"torch".equals(getUIController().getFlashButton().getValue())) {
                    getUIController().getFlashButton().keepSetValue("off");
                }
                updateWarningMessage(R.string.groupshot_mode_use_hint, false);
            } else if (CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
                setManualParameters();
                this.mCameraDevice.setParameters(this.mParameters);
            }
            int effect = CameraSettings.getShaderEffect();
            if (EffectController.sGradienterIndex == effect) {
                this.mActivity.getSensorStateManager().setGradienterEnabled(true);
            } else if (effect != EffectController.getInstance().getEffect(false)) {
                applyPreferenceSettingsLater();
            }
        }
    }

    public void onPreviewPixelsRead(byte[] pixels, int width, int height) {
        animateShutter();
        Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels));
        this.mHandler.obtainMessage(42, Thumbnail.createThumbnail(null, bitmap, this.mShootOrientation, false)).sendToTarget();
    }

    public void onPreviewTextureCopied() {
        this.mPreviewTextureCopiedCallback.onPreviewTextureCopied();
    }

    protected void animateSwitchCamera() {
        if (Device.isMDPRender()) {
            this.mHandler.sendEmptyMessageDelayed(22, 100);
            enableCameraControls(true);
            this.mSwitchingCamera = false;
            return;
        }
        this.mHandler.sendEmptyMessage(7);
    }

    private void animateHold() {
        if (!this.mIsImageCaptureIntent && !this.mPaused) {
            this.mActivity.getCameraScreenNail().animateHold(getCameraRotation());
        }
    }

    private void animateSlide() {
        if (!this.mIsImageCaptureIntent && !this.mPaused) {
            this.mActivity.getCameraScreenNail().animateSlide();
        }
    }

    private void animateCapture() {
        if (!this.mIsImageCaptureIntent && !this.mPaused) {
            if (Device.isMDPRender()) {
                getUIController().getPreviewPanel().onCapture();
            } else {
                this.mActivity.getCameraScreenNail().animateCapture(getCameraRotation());
            }
        }
    }

    public void onSwitchAnimationDone() {
        this.mHandler.sendEmptyMessage(28);
    }

    public void onUserInteraction() {
        super.onUserInteraction();
        keepScreenOnAwhile();
    }

    private void resetScreenOn() {
        this.mHandler.removeMessages(17);
        this.mHandler.removeMessages(2);
        getWindow().clearFlags(128);
    }

    private void keepScreenOnAwhile() {
        this.mHandler.sendEmptyMessageDelayed(17, 1000);
    }

    private void restorePreferences() {
        if (this.mParameters.isZoomSupported()) {
            setZoomValue(0);
        }
        getUIController().getFlashButton().reloadPreference();
        getUIController().getSettingPage().reloadPreferences();
        onSharedPreferenceChanged();
    }

    private void initializeCapabilities() {
        boolean z = false;
        this.mInitialParams = this.mCameraDevice.getParameters();
        if (this.mInitialParams != null) {
            boolean isSupported;
            if (this.mInitialParams.getMaxNumFocusAreas() > 0) {
                isSupported = BaseModule.isSupported("auto", sProxy.getSupportedFocusModes(this.mInitialParams));
            } else {
                isSupported = false;
            }
            this.mFocusAreaSupported = isSupported;
            if (this.mInitialParams.getMaxNumMeteringAreas() > 0) {
                z = true;
            }
            this.mMeteringAreaSupported = z;
            this.mAeLockSupported = this.mInitialParams.isAutoExposureLockSupported();
            this.mAwbLockSupported = this.mInitialParams.isAutoWhiteBalanceLockSupported();
            this.mContinuousFocusSupported = BaseModule.isSupported("continuous-picture", sProxy.getSupportedFocusModes(this.mInitialParams));
        }
    }

    private void setPreviewFrameLayoutAspectRatio() {
        Size size = this.mParameters.getPictureSize();
        getUIController().getPreviewFrame().setAspectRatio(CameraSettings.getPreviewAspectRatio(size.width, size.height));
    }

    public boolean scanQRCodeEnabled() {
        if (Device.IS_D2A) {
            return false;
        }
        if ((this.mCameraState != 1 && this.mCameraState != 2) || this.mIsImageCaptureIntent || !isBackCamera() || this.mMultiSnapStatus || getUIController().getSettingPage().isItemSelected() || !getUIController().getPreviewPage().isPreviewPageVisible() || getUIController().getModeExitView().isExitButtonShown() || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            return false;
        }
        return true;
    }

    public void findQRCode() {
    }

    private void takeAPhotoIfNeeded() {
        if (this.mIsCaptureAfterLaunch) {
            AutoLockManager.getInstance(this.mActivity).removeMessage();
        }
        boolean googleAssist = this.mActivity.isVoiceAssistantCaptureIntent();
        this.mIsCaptureAfterLaunch = !isCaptureAfterLaunch() ? googleAssist : true;
        if (this.mIsCaptureAfterLaunch) {
            if (BaseModule.isSupported("off", this.mParameters.getSupportedFlashModes())) {
                this.mParameters.setFlashMode("off");
                getUIController().getFlashButton().setValue("off");
                this.mCameraDevice.setParameters(this.mParameters);
            }
            if (googleAssist) {
                this.mHandler.removeMessages(44);
                this.mHandler.sendEmptyMessageDelayed(44, 1000);
            } else {
                sendDoCaptureMessage(1000);
            }
            AutoLockManager.getInstance(this.mActivity).lockScreenDelayed();
        }
    }

    private void sendDoCaptureMessage(long delay) {
        this.mDoCaptureRetry = 0;
        if (!this.mHandler.hasMessages(15)) {
            this.mHandler.sendEmptyMessageDelayed(15, delay);
        }
    }

    private boolean isCaptureAfterLaunch() {
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(this.mActivity.getIntent().getAction())) {
            Bundle myExtras = this.mActivity.getIntent().getExtras();
            if (myExtras != null && myExtras.containsKey("captureAfterLaunch")) {
                boolean result = myExtras.getBoolean("captureAfterLaunch", false);
                myExtras.putBoolean("captureAfterLaunch", false);
                this.mActivity.getIntent().putExtras(myExtras);
                return result;
            }
        }
        return false;
    }

    protected boolean isSupportSceneMode() {
        return false;
    }

    public void sendDelayedCaptureMessage(int period, int times) {
        if (!this.mHandler.hasMessages(20) && getUIController().getPreviewPage().isPreviewPageVisible()) {
            this.mHandler.obtainMessage(20, times, period).sendToTarget();
        }
    }

    public boolean readyToAudioCapture() {
        boolean ready = false;
        if (this.mHandler.hasMessages(20)) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (((this.mCameraState == 2 && !this.mFocusManager.isFocusingSnapOnFinish()) || this.mCameraState == 1) && Util.isTimeout(now, AutoLockManager.getInstance(this.mActivity).getLastActionTime(), 500)) {
            if (Util.isTimeout(now, this.mActivity.getSoundPlayTime(), 1000)) {
                if (Util.isTimeout(now, this.mLastShutterButtonClickTime, 1000)) {
                    ready = Util.isTimeout(now, this.mJpegPictureCallbackTime, 500);
                }
            }
        }
        return ready;
    }

    public void tryRemoveCountDownMessage() {
        this.mHandler.removeMessages(20);
        this.mAudioCaptureManager.hideDelayNumber();
        this.mIsCountDown = false;
    }

    public boolean isMeteringAreaOnly() {
        String focusMode = this.mParameters.getFocusMode();
        return ((!this.mFocusAreaSupported && this.mMeteringAreaSupported) || "edof".equals(focusMode) || "fixed".equals(focusMode) || "infinity".equals(focusMode) || "manual".equals(focusMode)) ? true : "lock".equals(focusMode);
    }

    public boolean isShowCaptureButton() {
        return !this.mMutexModePicker.isBurstShoot();
    }

    public boolean onGestureTrack(RectF rectF, boolean up) {
        if (couldEnableObjectTrack()) {
            return initializeObjectTrack(rectF, up);
        }
        return false;
    }

    protected boolean isDefaultManualExposure() {
        if (isDefaultPreference("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default))) {
            return isDefaultPreference("pref_qc_camera_exposuretime_key", getString(R.string.pref_camera_exposuretime_default));
        }
        return false;
    }

    protected String getManualValue(String key, String defaultValue) {
        if (CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
            return this.mPreferences.getString(key, defaultValue);
        }
        return defaultValue;
    }

    protected boolean isDefaultPreference(String key, String defaultValue) {
        return defaultValue.equals(getManualValue(key, defaultValue));
    }

    private boolean couldEnableObjectTrack() {
        if (!Device.isSupportedObjectTrack() || !isBackCamera() || getUIController().getSettingPage().isItemSelected() || this.mMultiSnapStatus || this.mCameraState == 3 || this.mIsImageCaptureIntent) {
            return false;
        }
        return true;
    }

    public void onObjectStable() {
        if (this.mCameraState != 3 && !this.mFocusManager.isFocusingSnapOnFinish() && getUIController().getPreviewPage().isPreviewPageVisible()) {
            this.mFocusManager.requestAutoFocus();
            if (this.mPreferences.getBoolean("pref_capture_when_stable_key", false)) {
                Log.v("Camera", "Object is Stable, call onShutterButtonClick to capture");
                onShutterButtonClick();
                CameraDataAnalytics.instance().trackEvent("capture_times_t2t");
            }
        }
    }

    private boolean initializeObjectTrack(RectF rectF, boolean up) {
        mapTapCoordinate(rectF);
        stopObjectTracking(false);
        getUIController().getObjectView().clear();
        getUIController().getFocusView().clear();
        getUIController().getObjectView().setVisibility(0);
        if (!getUIController().getObjectView().initializeTrackView(rectF, up)) {
            return false;
        }
        this.mFocusManager.setFrameView(getUIController().getObjectView());
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startObjectTracking() {
        /*
        r5 = this;
        r4 = 1;
        r1 = com.android.camera.Device.isSupportedObjectTrack();
        if (r1 != 0) goto L_0x0008;
    L_0x0007:
        return;
    L_0x0008:
        r1 = "Camera";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "startObjectTracking mObjectTrackingStarted=";
        r2 = r2.append(r3);
        r3 = r5.mObjectTrackingStarted;
        r2 = r2.append(r3);
        r3 = " mCameraState=";
        r2 = r2.append(r3);
        r3 = r5.mCameraState;
        r2 = r2.append(r3);
        r2 = r2.toString();
        android.util.Log.i(r1, r2);
        r1 = r5.mObjectTrackingStarted;
        if (r1 != 0) goto L_0x003a;
    L_0x0035:
        r1 = r5.mCameraState;
        r2 = 3;
        if (r1 != r2) goto L_0x003b;
    L_0x003a:
        return;
    L_0x003b:
        r1 = r5.mPaused;
        if (r1 != 0) goto L_0x003a;
    L_0x003f:
        r1 = r5.mCameraDevice;
        if (r1 == 0) goto L_0x00c6;
    L_0x0043:
        r1 = com.android.camera.Device.isSupportedObjectTrack();
        if (r1 == 0) goto L_0x00c6;
    L_0x0049:
        r5.stopFaceDetection(r4);
        r5.mObjectTrackingStarted = r4;
        r1 = r5.mFocusManager;
        r2 = r5.getUIController();
        r2 = r2.getObjectView();
        r1.setFrameView(r2);
        r1 = sProxy;
        r2 = r5.mParameters;
        r3 = "auto";
        r1.setFocusMode(r2, r3);
        r1 = r5.getUIController();
        r1 = r1.getFlashButton();
        r0 = r1.getValue();
        r1 = "torch";
        r1 = r1.equals(r0);
        if (r1 != 0) goto L_0x0083;
    L_0x007a:
        r1 = "off";
        r1 = r1.equals(r0);
        if (r1 == 0) goto L_0x00c7;
    L_0x0083:
        r1 = r5.mCameraDevice;
        r2 = r5.mParameters;
        r1.setParameters(r2);
        r1 = r5.mCameraDevice;
        r1.setFaceDetectionListener(r5);
        r1 = "Camera";
        r2 = new java.lang.StringBuilder;
        r2.<init>();
        r3 = "startObjectTracking rect=";
        r2 = r2.append(r3);
        r3 = r5.getUIController();
        r3 = r3.getObjectView();
        r3 = r3.getFocusRectInPreviewFrame();
        r2 = r2.append(r3);
        r2 = r2.toString();
        android.util.Log.i(r1, r2);
        r1 = r5.mCameraDevice;
        r2 = r5.getUIController();
        r2 = r2.getObjectView();
        r2 = r2.getFocusRectInPreviewFrame();
        r1.startObjectTrack(r2);
    L_0x00c6:
        return;
    L_0x00c7:
        r1 = r5.getUIController();
        r1 = r1.getFlashButton();
        r2 = "off";
        r1.keepSetValue(r2);
        r1 = "pref_camera_flashmode_key";
        r5.updateASD(r1);
        goto L_0x0083;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.CameraModule.startObjectTracking():void");
    }

    public void stopObjectTracking(boolean restartFD) {
        if (Device.isSupportedObjectTrack()) {
            Log.i("Camera", "stopObjectTracking mObjectTrackingStarted=" + this.mObjectTrackingStarted + " restartFD=" + restartFD);
            if (this.mObjectTrackingStarted) {
                if (this.mCameraDevice != null) {
                    this.mObjectTrackingStarted = false;
                    this.mCameraDevice.setFaceDetectionListener(null);
                    this.mCameraDevice.stopObjectTrack();
                    if (!(getUIController().getObjectView().isAdjusting() || V6GestureRecognizer.getInstance(this.mActivity).getCurrentGesture() == 10)) {
                        getUIController().getFlashButton().updateFlashModeAccordingHdr(getUIController().getHdrButton().getValue());
                        if (!(this.mPaused || this.mMultiSnapStatus || this.mCameraState == 3 || this.mFocusManager.isFocusingSnapOnFinish())) {
                            CameraSettings.setFocusModeSwitching(true);
                            setCameraParameters(2);
                        }
                    }
                    getUIController().getObjectView().clear();
                    getUIController().getObjectView().setVisibility(8);
                }
                if (restartFD) {
                    startFaceDetection();
                }
                return;
            }
            if (!(getUIController().getObjectView() == null || getUIController().getObjectView().getVisibility() == 8)) {
                getUIController().getObjectView().clear();
                getUIController().getObjectView().setVisibility(8);
            }
        }
    }

    private void showObjectTrackToastIfNeeded() {
        if (this.mPreferences.getBoolean("pref_camera_first_tap_screen_hint_shown_key", true) && couldEnableObjectTrack()) {
            this.mHandler.sendEmptyMessageDelayed(23, 1000);
        }
    }

    private void updateWarningMessage(int id, boolean hide) {
        updateWarningMessage(id, hide, true);
    }

    private void updateWarningMessage(int id, boolean hide, boolean autoHide) {
        int i = 0;
        if (id != 0) {
            getUIController().getWarningMessageView().setText(id);
        }
        this.mHandler.removeMessages(21);
        if (!hide && autoHide) {
            if (R.string.ubifocus_capture_warning_message == id) {
                this.mHandler.sendEmptyMessageDelayed(21, 15000);
            } else {
                this.mHandler.sendEmptyMessageDelayed(21, 5000);
            }
        }
        LinearLayout warningMessageParent = getUIController().getWarningMessageParent();
        if (hide) {
            i = 8;
        }
        warningMessageParent.setVisibility(i);
    }

    protected boolean isFrontMirror() {
        if (!isFrontCamera()) {
            return false;
        }
        String frontMirror = CameraSettings.getFrontMirror(this.mPreferences);
        if (getString(R.string.pref_front_mirror_entryvalue_auto).equals(frontMirror)) {
            return getUIController().getFaceView().faceExists();
        }
        return getString(R.string.pref_front_mirror_entryvalue_on).equals(frontMirror);
    }

    public boolean isKeptBitmapTexture() {
        return this.mKeepBitmapTexture;
    }

    private void updateHDRPreference() {
        int mutexhdr = getMutexHdrMode(getUIController().getHdrButton().getValue());
        updateASD("pref_camera_hdr_key");
        if (mutexhdr != -1) {
            this.mMutexModePicker.setMutexMode(mutexhdr);
        } else if (this.mMutexModePicker.isHdr()) {
            this.mMutexModePicker.resetMutexMode();
        } else {
            onSharedPreferenceChanged();
        }
    }

    public boolean isNeedMute() {
        return !super.isNeedMute() ? CameraSettings.isAudioCaptureOpen() : true;
    }

    protected boolean isDetectedHHT() {
        boolean z = true;
        if (!Device.isSupportedAsdNight() && !Device.isSupportedAsdMotion()) {
            return false;
        }
        if (!(2 == this.mMetaDataManager.mCurrentScene || 2 == this.mMetaDataManager.mLastScene || 3 == this.mMetaDataManager.mCurrentScene || 3 == this.mMetaDataManager.mLastScene)) {
            z = false;
        }
        return z;
    }

    protected boolean isZoomEnabled() {
        if (this.mMutexModePicker.isUbiFocus() || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
            return false;
        }
        return true;
    }

    protected void sendOpenFailMessage() {
        this.mHandler.sendEmptyMessage(10);
    }

    protected void setTimeWatermarkIfNeed() {
        if (CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            sProxy.setTimeWatermarkValue(this.mParameters, Util.getTimeWatermark());
        }
    }

    private void checkRestartPreview() {
        if (this.mRestartPreview && this.mCameraState != 0) {
            Log.v("Camera", "Restarting Preview... Camera Mode Changed");
            stopPreview();
            startPreview();
            startFaceDetection();
            setCameraState(1);
            this.mRestartPreview = false;
        }
    }

    private void resetGradienter() {
        if (CameraSettings.isSwitchOn("pref_camera_gradienter_key")) {
            this.mActivity.getSensorStateManager().setGradienterEnabled(false);
            this.mSettingsOverrider.restoreSettings();
        }
    }

    private void applyPreferenceChange() {
        if ((this.mSetCameraParameter & 1) != 0) {
            setCameraParameters(2);
        }
        if ((this.mSetCameraParameter & 2) != 0) {
            getUIController().getEffectCropView().updateVisible();
            getUIController().getSettingsStatusBar().updateStatus();
        }
        this.mSetCameraParameter = 0;
    }

    protected void trackPictureTaken(int takenNum, boolean burst, int width, int height, boolean location) {
        if (this.mMutexModePicker.isMorphoHdr() || this.mMutexModePicker.isSceneHdr()) {
            CameraDataAnalytics.instance().trackEvent("capture_nums_normal_hdr");
        } else if (this.mMutexModePicker.isAoHdr()) {
            CameraDataAnalytics.instance().trackEvent("capture_nums_live_hdr");
        } else if (this.mMutexModePicker.isHandNight()) {
            CameraDataAnalytics.instance().trackEvent("capture_nums_hht");
        } else if (this.mMutexModePicker.isUbiFocus()) {
            CameraDataAnalytics.instance().trackEvent("capture_nums_ubfocus");
        } else {
            String fb = sProxy.getStillBeautify(this.mParameters);
            String closeFB = getString(R.string.pref_face_beauty_close);
            if (!TextUtils.isEmpty(fb) && !fb.equals(closeFB)) {
                CameraDataAnalytics.instance().trackEvent("capture_nums_beauty");
            } else if (CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
                CameraDataAnalytics.instance().trackEvent("capture_nums_manual");
            } else if (CameraSettings.isSwitchOn("pref_camera_gradienter_key")) {
                CameraDataAnalytics.instance().trackEvent("capture_nums_gradienter");
            } else if (CameraSettings.isSwitchOn("pref_camera_tilt_shift_mode")) {
                int effect = CameraSettings.getShaderEffect();
                if (effect == EffectController.sGaussianIndex) {
                    CameraDataAnalytics.instance().trackEvent("capture_nums_tilt_shift_circle");
                } else if (effect == EffectController.sTiltShiftIndex) {
                    CameraDataAnalytics.instance().trackEvent("capture_nums_tilt_shift_parallel");
                }
            }
        }
        if (EffectController.getInstance().isEffectPageSelected()) {
            CameraDataAnalytics.instance().trackEvent(EffectController.getInstance().getAnalyticsKey());
        }
        if (burst && CameraSettings.isPressDownCapture() && takenNum > 1) {
            takenNum--;
        }
        if (CameraSettings.isTimeWaterMarkOpen(this.mPreferences)) {
            CameraDataAnalytics.instance().trackEvent("time_watermark_taken_key");
        } else if (CameraSettings.isDualCameraWaterMarkOpen(this.mPreferences)) {
            CameraDataAnalytics.instance().trackEvent("dual_watermark_taken_key");
        }
        if (sProxy.isFaceWatermarkOn(this.mParameters) && getUIController().getFaceView().faceExists()) {
            CameraDataAnalytics.instance().trackEvent("faceinfo_watermark_taken_key");
        }
        super.trackPictureTaken(takenNum, burst, width, height, location);
    }

    public boolean isCameraEnabled() {
        return (this.mPaused || this.mSwitchingCamera || this.mCameraState == 0) ? false : true;
    }

    private void setPictureOrientation() {
        this.mShootRotation = this.mActivity.getSensorStateManager().isDeviceLying() ? -1.0f : this.mDeviceRotation;
        this.mShootOrientation = this.mOrientation == -1 ? 0 : this.mOrientation;
    }

    private void traceDelayCaptureEvents() {
        if (this.mAudioCaptureManager.isRunning()) {
            CameraDataAnalytics.instance().trackEvent("capture_times_audio");
            return;
        }
        CameraDataAnalytics.instance().trackEvent("capture_times_count_down");
        int countTimes = CameraSettings.getCountDownTimes();
        if (countTimes == 3) {
            CameraDataAnalytics.instance().trackEvent("capture_times_count_down_3s");
        } else if (countTimes == 5) {
            CameraDataAnalytics.instance().trackEvent("capture_times_count_down_5s");
        } else if (countTimes == 10) {
            CameraDataAnalytics.instance().trackEvent("capture_times_count_down_10s");
        }
    }

    private void releaseResources() {
        stopPreview();
        closeCamera();
        CameraDataAnalytics.instance().uploadToServer();
        this.mWaitForRelease = false;
    }

    protected int getBurstDelayTime() {
        return 0;
    }

    public void onCameraMetaData(byte[] data, Camera camera) {
        if (!this.mPaused && getUIController().getPreviewPage().isPreviewPageVisible() && !this.mActivity.getCameraScreenNail().isModuleSwitching() && this.mCameraState == 1 && !this.mMultiSnapStatus && !this.mHandler.hasMessages(26)) {
            this.mMetaDataManager.setData(data);
        }
    }

    protected void setMetaCallback(boolean asdEnable) {
        setMetaCallback(asdEnable ? 3 : 0);
    }

    protected void setMetaCallback(int metaType) {
        boolean z = false;
        boolean metaEnable = metaType != 0;
        if (this.mSetMetaCallback != metaEnable) {
            if (!this.mSetMetaCallback) {
                z = true;
            }
            this.mSetMetaCallback = z;
            this.mCameraDevice.setMetaDataCallback(this.mSetMetaCallback ? this : null);
        }
        if (this.mSetMetaCallback) {
            this.mMetaDataManager.resetFilter();
            this.mMetaDataManager.setType(metaType);
        }
        if (!metaEnable && -1 != this.mMetaDataManager.mCurrentScene) {
            this.mMetaDataManager.resetSceneMode();
        }
    }

    protected boolean isSceneMotion() {
        return this.mMetaDataManager.mCurrentScene == 3;
    }

    private boolean isPortraitModeUseHintShowing() {
        if (TextUtils.equals(getUIController().getWarningMessageView().getText(), getString(R.string.dual_camera_use_hint)) && getUIController().getWarningMessageParent().getVisibility() == 0) {
            return true;
        }
        return false;
    }

    private void updateLyingSensorState(boolean enabled) {
        if (this.mActivity.getSensorStateManager().canDetectOrientation()) {
            this.mActivity.getSensorStateManager().setRotationIndicatorEnabled(enabled);
        }
    }

    protected PictureSize getBestPictureSize() {
        PictureSizeManager.initialize(getActivity(), this.mParameters.getSupportedPictureSizes(), getMaxPictureSize());
        return PictureSizeManager.getBestPictureSize();
    }

    public boolean isCaptureIntent() {
        return this.mIsImageCaptureIntent;
    }

    protected void setBeautyParams() {
        if (Device.isSupportedSkinBeautify()) {
            String beauty = CameraSettings.getFaceBeautifyValue();
            if (CameraSettings.isSwitchOn("pref_camera_portrait_mode_key") && getUIController().getFaceView().faceExists() && CameraSettings.isCameraPortraitWithFaceBeauty()) {
                beauty = getString(R.string.pref_face_beauty_default);
            }
            sProxy.setStillBeautify(this.mParameters, beauty);
            Log.i("Camera", "SetStillBeautify =" + sProxy.getStillBeautify(this.mParameters));
            if (CameraSettings.isFaceBeautyOn(beauty)) {
                sProxy.setBeautifySkinColor(this.mParameters, CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_color_key"));
                sProxy.setBeautifySlimFace(this.mParameters, CameraSettings.getBeautifyDetailValue("pref_skin_beautify_slim_face_key"));
                sProxy.setBeautifySkinSmooth(this.mParameters, CameraSettings.getBeautifyDetailValue("pref_skin_beautify_skin_smooth_key"));
                sProxy.setBeautifyEnlargeEye(this.mParameters, CameraSettings.getBeautifyDetailValue("pref_skin_beautify_enlarge_eye_key"));
            }
        }
    }

    protected void onCameraStartPreview() {
    }

    public void notifyError() {
        super.notifyError();
        setCameraState(0);
    }

    private int getGroupshotNum() {
        CameraHardwareFace[] faces = getUIController().getFaceView().getFaces();
        return Util.clamp((faces != null ? faces.length : 0) + 1, 2, 4);
    }

    private void initGroupShot(int maxImage) {
        if (this.mParameters != null) {
            if (this.mGroupShot == null || this.mGroupShot.isUsed()) {
                this.mGroupShot = new GroupShot();
            }
            if (this.mOrientation % 180 == 0 && Device.isISPRotated()) {
                this.mGroupShot.initialize(maxImage, this.mGroupFaceNum, this.mParameters.getPictureSize().height, this.mParameters.getPictureSize().width, this.mParameters.getPreviewSize().height, this.mParameters.getPreviewSize().width);
            } else {
                this.mGroupShot.initialize(maxImage, this.mGroupFaceNum, this.mParameters.getPictureSize().width, this.mParameters.getPictureSize().height, this.mParameters.getPreviewSize().width, this.mParameters.getPreviewSize().height);
            }
        }
    }

    private void prepareGroupShot() {
        if (isGroupShotCapture()) {
            initGroupShot(this.mTotalJpegCallbackNum);
            if (this.mGroupShot != null) {
                this.mGroupShot.attach_start(1);
            } else {
                this.mTotalJpegCallbackNum = 1;
            }
            this.mReceivedJpegCallbackNum = 0;
        }
    }

    private boolean isGroupShotCapture() {
        if (CameraSettings.isSwitchOn("pref_camera_groupshot_mode_key")) {
            return this.mTotalJpegCallbackNum > 1;
        } else {
            return false;
        }
    }

    protected void setManualParameters() {
    }
}
