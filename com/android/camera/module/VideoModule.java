package com.android.camera.module;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.Size;
import android.location.Location;
import android.media.AudioManager;
import android.media.AudioSystem;
import android.media.CamcorderProfile;
import android.media.CameraProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.provider.MediaStore.Video.Media;
import android.support.v7.recyclerview.R;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.View;
import android.widget.Toast;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraHolder;
import com.android.camera.CameraPreferenceActivity;
import com.android.camera.CameraSettings;
import com.android.camera.ChangeManager;
import com.android.camera.Device;
import com.android.camera.Exif;
import com.android.camera.FocusManagerSimple;
import com.android.camera.LocationManager;
import com.android.camera.OnClickAttr;
import com.android.camera.SensorStateManager.SensorStateListener;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;
import com.android.camera.permission.PermissionManager;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;
import com.android.camera.ui.GridSettingTextPopup;
import com.android.camera.ui.ObjectView.ObjectViewListener;
import com.android.camera.ui.PopupManager;
import com.android.camera.ui.RotateTextToast;
import com.android.camera.ui.V6AbstractSettingPopup;
import com.android.camera.ui.V6GestureRecognizer;
import com.android.camera.ui.V6ModulePicker;
import com.android.camera.ui.V6ShutterButton;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import miui.reflect.Method;
import miui.reflect.NoSuchMethodException;

public class VideoModule extends BaseModule implements OnErrorListener, OnInfoListener, AutoFocusCallback, AutoFocusMoveCallback, ObjectViewListener, FaceDetectionListener {
    private static boolean HOLD_WHEN_SAVING_VIDEO = false;
    public static final long VIDEO_MIN_SINGLE_FILE_SIZE = Math.min(8388608, 52428800);
    protected static final HashMap<Integer, Integer> VIDEO_QUALITY_TO_HIGHSPEED = new HashMap();
    private AudioManager mAudioManager;
    private String mBaseFileName;
    private boolean mCaptureTimeLapse;
    private boolean mContinuousFocusSupported;
    private volatile int mCurrentFileNumber;
    private int mCurrentShowIndicator = -1;
    private String mCurrentVideoFilename;
    private Uri mCurrentVideoUri;
    private ContentValues mCurrentVideoValues;
    private int mDesiredPreviewHeight;
    private int mDesiredPreviewWidth;
    private boolean mFocusAreaSupported;
    private FocusManagerSimple mFocusManager;
    private long mFocusStartTime;
    protected final Handler mHandler = new MainHandler();
    protected String mHfr = "normal";
    private boolean mInStartingFocusRecording = false;
    private boolean mIsFromStop;
    private boolean mIsTouchFocused;
    protected boolean mIsVideoCaptureIntent;
    private long mLastBackPressedTime = 0;
    private AsyncTask<Void, Void, Void> mLoadThumbnailTask;
    protected int mMaxVideoDurationInMs;
    protected MediaRecorder mMediaRecorder;
    private volatile boolean mMediaRecorderRecording = false;
    private boolean mMediaRecorderRecordingPaused = false;
    private boolean mMeteringAreaSupported;
    private long mOnResumeTime;
    private int mOrientationCompensationAtRecordStart;
    private int mOriginalMusicVolume;
    private long mPauseClickTime = 0;
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        public void onCallStateChanged(int state, String incomingNumber) {
            if (state == 2 && VideoModule.this.mMediaRecorderRecording) {
                Log.i("videocamera", "CALL_STATE_OFFHOOK, so we call onstop here to stop recording");
                VideoModule.this.onStop();
            }
            super.onCallStateChanged(state, incomingNumber);
        }
    };
    private boolean mPreviewing;
    protected CamcorderProfile mProfile;
    protected int mQuality = 5;
    private boolean mQuickCapture;
    private BroadcastReceiver mReceiver = null;
    public volatile boolean mRecorderBusy = false;
    private long mRecordingStartTime;
    private String mRecordingTime;
    private boolean mRecordingTimeCountsDown = false;
    private boolean mRecordingUIShown;
    protected boolean mRestartPreview;
    private Runnable mRestoreRunnable = new Runnable() {
        public void run() {
            Log.i("videocamera", "mRestoreRunnable start");
            VideoModule.this.mAudioManager.abandonAudioFocus(null);
            VideoModule.this.restoreMusicSound();
            if (!VideoModule.this.mIsVideoCaptureIntent) {
                VideoModule.this.enableCameraControls(true);
            }
            VideoModule.this.keepScreenOnAwhile();
            if (VideoModule.this.mIsVideoCaptureIntent && !VideoModule.this.mPaused) {
                if (VideoModule.this.mQuickCapture) {
                    VideoModule.this.doReturnToCaller(VideoModule.this.mSavingResult);
                } else if (VideoModule.this.mSavingResult) {
                    VideoModule.this.showAlert();
                }
            }
            VideoModule.this.mActivity.getScreenHint().updateHint();
            VideoModule.this.animateSlide();
            if (VideoModule.this.mRecordingUIShown) {
                VideoModule.this.showRecordingUI(false);
            }
            VideoModule.this.updateLoadUI(false);
            VideoModule.this.onStopRecording();
            VideoModule.this.mRecorderBusy = false;
        }
    };
    private boolean mSavingResult = false;
    private SensorStateListener mSensorStateListener = new SensorStateListener() {
        public void onDeviceBecomeStable() {
        }

        public boolean isWorking() {
            return VideoModule.this.mPreviewing;
        }

        public void onDeviceKeepMoving(double a) {
            if (!VideoModule.this.getUIController().getFocusView().isEvAdjustedTime() && !VideoModule.this.mPaused && Util.isTimeout(System.currentTimeMillis(), VideoModule.this.mTouchFocusStartingTime, 2000)) {
                VideoModule.this.mIsTouchFocused = false;
                if (VideoModule.this.mFocusManager != null) {
                    VideoModule.this.mFocusManager.onDeviceKeepMoving();
                    if (VideoModule.this.mFocusManager.isNeedCancelAutoFocus()) {
                        VideoModule.this.cancelAutoFocus();
                        VideoModule.this.getUIController().getFocusView().clear();
                    }
                }
            }
        }

        public void onDeviceBeginMoving() {
        }

        public void onDeviceOrientationChanged(float orientation, boolean isLying) {
        }

        public void notifyDevicePostureChanged() {
            VideoModule.this.getUIController().getEdgeShutterView().onDevicePostureChanged();
        }
    };
    private boolean mSnapshotInProgress = false;
    private StereoSwitchThread mStereoSwitchThread;
    private boolean mSwitchingCamera;
    private final Object mTaskLock = new Object();
    TelephonyManager mTelephonyManager;
    private int mTimeBetweenTimeLapseFrameCaptureMs = 0;
    private long mTouchFocusStartingTime = 0;
    private ParcelFileDescriptor mVideoFileDescriptor;
    private String mVideoFilename;
    private String mVideoFocusMode;
    protected int mVideoHeight;
    private long mVideoRecordedDuration;
    private SavingTask mVideoSavingTask;
    protected int mVideoWidth;

    private final class JpegPictureCallback implements PictureCallback {
        Location mLocation;

        public JpegPictureCallback(Location loc) {
            this.mLocation = loc;
        }

        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.v("videocamera", "onPictureTaken");
            VideoModule.this.mSnapshotInProgress = false;
            if (!VideoModule.this.mPaused) {
                VideoModule.this.storeImage(jpegData, this.mLocation);
                VideoModule.this.getUIController().getShutterButton().enableControls(true);
            }
        }
    }

    private class LoadThumbnailTask extends AsyncTask<Void, Void, Void> {
        Thumbnail mThumbnail;
        Uri mUri;
        String mVideoPath;

        public LoadThumbnailTask() {
            this.mUri = VideoModule.this.mCurrentVideoUri;
            this.mVideoPath = VideoModule.this.mCurrentVideoFilename;
        }

        protected Void doInBackground(Void... params) {
            if (this.mUri != null) {
                Bitmap videoFrame = Thumbnail.createVideoThumbnailBitmap(this.mVideoPath, 512);
                if (!(isCancelled() || videoFrame == null)) {
                    this.mThumbnail = Thumbnail.createThumbnail(this.mUri, videoFrame, 0, false);
                }
            }
            return null;
        }

        protected void onPostExecute(Void result) {
            if (VideoModule.this.mPaused || !isCancelled()) {
                updateThumbnail();
            }
        }

        protected void onCancelled() {
            Log.e("videocamera", "LoadThumbnailTask onCancelled");
            updateThumbnail();
        }

        private void updateThumbnail() {
            Log.e("videocamera", "LoadThumbnailTask updateThumbnail mThumbnail=" + this.mThumbnail + " mPaused=" + VideoModule.this.mPaused);
            if (this.mThumbnail != null || VideoModule.this.mPaused) {
                VideoModule.this.mActivity.getThumbnailUpdater().setThumbnail(this.mThumbnail, !VideoModule.this.mPaused);
            }
        }
    }

    private class MainHandler extends Handler {
        private MainHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (!(Util.getDisplayRotation(VideoModule.this.mActivity) == VideoModule.this.mDisplayRotation || VideoModule.this.mMediaRecorderRecording || VideoModule.this.mSwitchingCamera)) {
                        VideoModule.this.startPreview();
                    }
                    if (SystemClock.uptimeMillis() - VideoModule.this.mOnResumeTime < 5000) {
                        VideoModule.this.mHandler.sendEmptyMessageDelayed(1, 100);
                        return;
                    }
                    return;
                case 2:
                    VideoModule.this.getWindow().clearFlags(128);
                    return;
                case 3:
                    VideoModule.this.updateRecordingTime();
                    return;
                case 4:
                    VideoModule.this.getUIController().getShutterButton().enableControls(true);
                    return;
                case 5:
                    VideoModule.this.switchCamera();
                    return;
                case 6:
                    VideoModule.this.mActivity.getCameraScreenNail().animateSwitchCameraBefore();
                    return;
                case 10:
                    VideoModule.this.mHandler.removeMessages(10);
                    VideoModule.this.mHandler.removeMessages(2);
                    VideoModule.this.getWindow().addFlags(128);
                    VideoModule.this.mHandler.sendEmptyMessageDelayed(2, (long) VideoModule.this.getScreenDelay());
                    return;
                case 11:
                    CameraSettings.changeUIByPreviewSize(VideoModule.this.mActivity, VideoModule.this.mUIStyle, VideoModule.this.mDesiredPreviewWidth, VideoModule.this.mDesiredPreviewHeight);
                    VideoModule.this.changePreviewSurfaceSize();
                    return;
                case 12:
                    if (VideoModule.this.mHasPendingSwitching) {
                        CameraSettings.changeUIByPreviewSize(VideoModule.this.mActivity, VideoModule.this.mUIStyle, VideoModule.this.mDesiredPreviewWidth, VideoModule.this.mDesiredPreviewHeight);
                        VideoModule.this.changePreviewSurfaceSize();
                        VideoModule.this.mHasPendingSwitching = false;
                    }
                    VideoModule.this.updateCameraScreenNailSize(VideoModule.this.mDesiredPreviewWidth, VideoModule.this.mDesiredPreviewHeight, VideoModule.this.mFocusManager);
                    VideoModule.this.mActivity.getCameraScreenNail().switchCameraDone();
                    VideoModule.this.mSwitchingCamera = false;
                    return;
                case 13:
                    VideoModule.this.getUIController().getCaptureProgressBar().setVisibility(0);
                    return;
                case 14:
                    if (Device.isMDPRender() && VideoModule.this.getUIController().getSurfaceViewFrame().isSurfaceViewVisible()) {
                        VideoModule.this.getUIController().getGLView().setVisibility(8);
                    }
                    if (VideoModule.this.getUIController().getSettingPage().getVisibility() != 0) {
                        VideoModule.this.mActivity.setBlurFlag(false);
                    }
                    if (!VideoModule.this.mIsVideoCaptureIntent || VideoModule.this.getUIController().getReviewDoneView().getVisibility() != 0) {
                        VideoModule.this.ignoreTouchEvent(false);
                        return;
                    }
                    return;
                case 15:
                    VideoModule.this.showStoppingUI();
                    return;
                case 16:
                    VideoModule.this.enableCameraControls(true);
                    return;
                case 17:
                    if (VideoModule.this.mPaused || !VideoModule.this.mMediaRecorderRecording || !hasMessages(3)) {
                        return;
                    }
                    if (V6GestureRecognizer.getInstance(VideoModule.this.mActivity).isGestureDetecting()) {
                        Runnable showAnim = new Runnable() {
                            public void run() {
                                if (VideoModule.this.getUIController().getSettingsStatusBar().getVisibility() == 8) {
                                    if (VideoModule.this.getUIController().getVideoRecordingTimeView().getVisibility() != 0) {
                                        VideoModule.this.getUIController().getVideoRecordingTimeView().animateIn(null, 150, true);
                                    }
                                    if (VideoModule.this.mMediaRecorderRecordingPaused) {
                                        VideoModule.this.mCurrentShowIndicator = 0;
                                    }
                                }
                            }
                        };
                        if (VideoModule.this.getUIController().getSettingsStatusBar().getVisibility() == 0) {
                            VideoModule.this.getUIController().getSettingsStatusBar().animateOut(showAnim);
                            return;
                        } else {
                            showAnim.run();
                            return;
                        }
                    }
                    sendEmptyMessageDelayed(17, 1000);
                    return;
                case 18:
                    VideoModule.this.onCameraException();
                    VideoModule.this.onStopVideoRecording(VideoModule.this.mPaused);
                    if (VideoModule.this.mPaused) {
                        VideoModule.this.closeCamera();
                        return;
                    }
                    return;
                case 19:
                    VideoModule.this.mIgnoreFocusChanged = true;
                    VideoModule.this.mActivity.getScreenHint().showFirstUseHint();
                    return;
                case 20:
                    VideoModule.this.onPreviewStart();
                    VideoModule.this.mStereoSwitchThread = null;
                    return;
                case 21:
                    if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                        VideoModule.this.getUIController().getWarningMessageView().setText(R.string.dual_camera_use_hint);
                        VideoModule.this.getUIController().getWarningMessageParent().setVisibility(0);
                        VideoModule.this.mHandler.sendEmptyMessageDelayed(22, 5000);
                        return;
                    }
                    return;
                case 22:
                    VideoModule.this.getUIController().getWarningMessageParent().setVisibility(8);
                    return;
                case 23:
                    VideoModule.this.restoreMusicSound();
                    return;
                case 24:
                    VideoModule.this.autoFocus(VideoModule.this.getUIController().getPreviewFrame().getWidth() / 2, VideoModule.this.getUIController().getPreviewFrame().getHeight() / 2, VideoModule.this.mFocusManager.getDefaultFocusAreaWidth(), VideoModule.this.mFocusManager.getDefaultFocusAreaHeight(), 0);
                    return;
                default:
                    Log.v("videocamera", "Unhandled message: " + msg.what);
                    return;
            }
        }
    }

    private class MyBroadcastReceiver extends BroadcastReceiver {
        private MyBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (Storage.isRelatedStorage(intent.getData())) {
                String action = intent.getAction();
                Log.v("videocamera", "onReceive: action=" + action);
                if (action.equals("android.intent.action.MEDIA_EJECT")) {
                    if (Storage.isCurrentStorageIsSecondary()) {
                        Storage.switchToPhoneStorage();
                        VideoModule.this.stopVideoRecordingAsync();
                    }
                } else if (action.equals("android.intent.action.MEDIA_MOUNTED")) {
                    VideoModule.this.mActivity.getScreenHint().updateHint();
                    VideoModule.this.mActivity.getThumbnailUpdater().getLastThumbnail();
                } else if (action.equals("android.intent.action.MEDIA_UNMOUNTED")) {
                    VideoModule.this.mActivity.getScreenHint().updateHint();
                } else if (!action.equals("android.intent.action.MEDIA_SCANNER_STARTED") && action.equals("android.intent.action.MEDIA_SCANNER_FINISHED")) {
                    VideoModule.this.mActivity.getScreenHint().updateHint();
                }
            }
        }
    }

    class SavingTask extends Thread {
        private boolean mRestart;

        private SavingTask(boolean restart) {
            this.mRestart = false;
            this.mRestart = restart;
        }

        public void run() {
            Log.v("videocamera", "SavingTask run mMediaRecorderRecording = " + VideoModule.this.mMediaRecorderRecording);
            VideoModule.this.mSavingResult = false;
            if (VideoModule.this.mMediaRecorderRecording) {
                ContentValues contentValues = null;
                if (VideoModule.this.subStopRecording()) {
                    VideoModule videoModule;
                    if (this.mRestart) {
                        boolean z;
                        contentValues = new ContentValues(VideoModule.this.mCurrentVideoValues);
                        videoModule = VideoModule.this;
                        videoModule.mCurrentFileNumber = videoModule.mCurrentFileNumber + 1;
                        Storage.switchStoragePathIfNeeded();
                        if (Storage.isLowStorageSpace(Storage.DIRECTORY)) {
                            z = false;
                        } else {
                            z = VideoModule.this.startRecordVideo();
                        }
                        this.mRestart = z;
                    }
                    if (!this.mRestart) {
                        if (!VideoModule.this.mPaused) {
                            VideoModule.this.playCameraSound(3);
                        }
                        contentValues = VideoModule.this.mCurrentVideoValues;
                        if (VideoModule.this.mCurrentFileNumber > 0) {
                            videoModule = VideoModule.this;
                            videoModule.mCurrentFileNumber = videoModule.mCurrentFileNumber + 1;
                        }
                    }
                    if (!VideoModule.this.addVideoToMediaStore(contentValues)) {
                        VideoModule.this.mSavingResult = true;
                    }
                }
                Object -get19;
                if (this.mRestart) {
                    VideoModule.this.mRecorderBusy = false;
                    -get19 = VideoModule.this.mTaskLock;
                    synchronized (-get19) {
                        VideoModule.this.mTaskLock.notifyAll();
                        VideoModule.this.mVideoSavingTask = null;
                    }
                } else {
                    VideoModule.this.mCurrentVideoValues = null;
                    VideoModule.this.mActivity.sendBroadcast(new Intent("com.android.camera.action.stop_video_recording"));
                    if (!VideoModule.this.mIsVideoCaptureIntent && VideoModule.this.mSavingResult) {
                        if (VideoModule.this.mPaused) {
                            VideoModule.this.mActivity.getThumbnailUpdater().setThumbnail(null, !VideoModule.this.mPaused);
                        } else {
                            if (VideoModule.this.mLoadThumbnailTask != null) {
                                VideoModule.this.mLoadThumbnailTask.cancel(true);
                            }
                            VideoModule.this.mLoadThumbnailTask = new LoadThumbnailTask().execute(new Void[0]);
                        }
                    }
                    VideoModule.this.mTelephonyManager.listen(VideoModule.this.mPhoneStateListener, 0);
                    Log.v("videocamera", "listen none");
                    -get19 = VideoModule.this.mTaskLock;
                    synchronized (-get19) {
                        VideoModule.this.mTaskLock.notifyAll();
                        VideoModule.this.mHandler.removeCallbacks(VideoModule.this.mRestoreRunnable);
                        VideoModule.this.mHandler.postAtFrontOfQueue(VideoModule.this.mRestoreRunnable);
                        VideoModule.this.mMediaRecorderRecording = false;
                        Log.w("videocamera", "stop recording at SavingTask, space = " + Storage.getLeftSpace());
                        VideoModule.this.mVideoSavingTask = null;
                    }
                }
                return;
            }
            VideoModule.this.mVideoSavingTask = null;
        }
    }

    protected class StereoSwitchThread extends Thread {
        private volatile boolean mCancelled;

        protected StereoSwitchThread() {
        }

        public void cancel() {
            this.mCancelled = true;
        }

        public void run() {
            VideoModule.this.closeCamera();
            if (!this.mCancelled) {
                VideoModule.this.openCamera();
                if (VideoModule.this.hasCameraException()) {
                    VideoModule.this.onCameraException();
                } else if (!this.mCancelled) {
                    CameraSettings.resetZoom(VideoModule.this.mPreferences);
                    CameraSettings.resetExposure();
                    VideoModule.this.onCameraOpen();
                    VideoModule.this.readVideoPreferences();
                    VideoModule.this.resizeForPreviewAspectRatio();
                    if (!this.mCancelled) {
                        VideoModule.this.startPreview();
                        VideoModule.this.mHandler.sendEmptyMessage(20);
                    }
                }
            }
        }
    }

    private void showStoppingUI() {
        if (this.mRecordingUIShown) {
            showRecordingUI(false);
        }
        updateLoadUI(true);
    }

    private String createName(long dateTaken) {
        if (this.mCurrentFileNumber > 0) {
            return this.mBaseFileName;
        }
        this.mBaseFileName = new SimpleDateFormat(getString(R.string.video_file_name_format)).format(new Date(dateTaken));
        return this.mBaseFileName;
    }

    public void onCreate(com.android.camera.Camera activity) {
        boolean z;
        super.onCreate(activity);
        this.mActivity.createContentView();
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
        this.mIsVideoCaptureIntent = this.mActivity.isVideoCaptureIntent();
        CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
        EffectController.getInstance().setEffect(0);
        V6ModulePicker.setCurrentModule(1);
        getUIController().onCreate();
        getUIController().useProperView();
        this.mActivity.getSensorStateManager().setSensorStateListener(this.mSensorStateListener);
        CameraDataAnalytics.instance().trackEventTime("open_camera_times_key");
        CameraOpenThread cameraOpenThread = null;
        boolean launch = PermissionManager.checkCameraLaunchPermissions();
        if (launch) {
            cameraOpenThread = new CameraOpenThread();
            cameraOpenThread.start();
        }
        initializeMiscControls();
        com.android.camera.Camera camera = this.mActivity;
        if (this.mIsVideoCaptureIntent) {
            z = false;
        } else {
            z = true;
        }
        camera.createCameraScreenNail(z, false);
        if (cameraOpenThread != null) {
            try {
                cameraOpenThread.join();
            } catch (InterruptedException e) {
            }
        }
        if (hasCameraException()) {
            onCameraException();
            return;
        }
        initializeFocusManager();
        if (launch) {
            onCameraOpen();
            initializeCapabilities();
            readVideoPreferences();
            prepareUIByPreviewSize();
            if (!Device.isMDPRender() || getUIController().getSurfaceViewFrame().isSurfaceViewAvailable()) {
                Thread startPreviewThread = new Thread(new Runnable() {
                    public void run() {
                        VideoModule.this.startPreview();
                    }
                });
                startPreviewThread.start();
                try {
                    startPreviewThread.join();
                } catch (InterruptedException e2) {
                }
                onPreviewStart();
            }
            resizeForPreviewAspectRatio();
        }
        this.mQuickCapture = this.mActivity.getIntent().getBooleanExtra("android.intent.extra.quickCapture", false);
        getUIController().getObjectView().setObjectViewListener(this);
        showFirstUseHintIfNeeded();
        ignoreTouchEvent(true);
        this.mTelephonyManager = (TelephonyManager) this.mActivity.getSystemService("phone");
    }

    public void onNewIntent() {
        this.mCameraId = getPreferredCameraId();
        changeConflictPreference();
        this.mIsVideoCaptureIntent = this.mActivity.isVideoCaptureIntent();
        CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
    }

    public List<String> getSupportedSettingKeys() {
        List<String> keys = new ArrayList();
        if (isBackCamera()) {
            keys.add("pref_video_speed_fast_key");
            if (Device.isSupportedHFR()) {
                keys.add("pref_video_speed_slow_key");
            }
            if (Device.isSupportedAudioFocus()) {
                keys.add("pref_audio_focus_mode_key");
            }
        }
        return keys;
    }

    private void startPlayVideoActivity() {
        Intent intent = new Intent("android.intent.action.VIEW");
        intent.setDataAndType(this.mCurrentVideoUri, convertOutputFormatToMimeType(this.mProfile.fileFormat));
        intent.setFlags(1);
        try {
            this.mActivity.startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Log.e("videocamera", "Couldn't view video " + this.mCurrentVideoUri, ex);
        }
    }

    @OnClickAttr
    public void onThumbnailClicked(View v) {
        if (!this.mMediaRecorderRecording && this.mActivity.getThumbnailUpdater().getThumbnail() != null && !this.mSwitchingCamera) {
            this.mActivity.gotoGallery();
        }
    }

    private boolean isSelectingCapturedVideo() {
        return this.mIsVideoCaptureIntent && getUIController().getReviewDoneView().getVisibility() == 0;
    }

    @OnClickAttr
    public void onReviewDoneClicked(View v) {
        doReturnToCaller(true);
    }

    @OnClickAttr
    public void onReviewCancelClicked(View v) {
        if (isSelectingCapturedVideo()) {
            deleteCurrentVideo();
            hideAlert();
            return;
        }
        stopVideoRecordingAsync();
        doReturnToCaller(false);
    }

    private void onStopVideoRecording(boolean sync) {
        if (sync) {
            stopVideoOnPause();
        } else {
            stopVideoRecordingAsync();
        }
    }

    protected int getCameraRotation() {
        return ((this.mOrientationCompensation - this.mDisplayRotation) + 360) % 360;
    }

    public void onShutterButtonFocus(boolean pressed) {
        Log.v("videocamera", "onShutterButtonFocus " + this.mFocusManager.isInValidFocus());
        if (pressed && !this.mSwitchingCamera && getUIController().getShutterButton().isEnabled() && !isVideoRecording() && isBackCamera() && this.mFocusManager.isInValidFocus()) {
            getUIController().getFocusView().clear();
            getUIController().getFocusView().setFocusType(false);
            autoFocus(getUIController().getPreviewFrame().getWidth() / 2, getUIController().getPreviewFrame().getHeight() / 2, this.mFocusManager.getDefaultFocusAreaWidth(), this.mFocusManager.getDefaultFocusAreaHeight(), 4);
            this.mInStartingFocusRecording = true;
        } else if ("continuous-video".equals(this.mVideoFocusMode) && getUIController().getFocusView().isShown()) {
            getUIController().getFocusView().clear();
        }
    }

    public void onShutterButtonClick() {
        Log.v("videocamera", "onShutterButtonClick mSwitchingCamera=" + this.mSwitchingCamera + " mMediaRecorderRecording=" + this.mMediaRecorderRecording + " mInStartingFocusRecording=" + this.mInStartingFocusRecording);
        this.mInStartingFocusRecording = false;
        if (!this.mSwitchingCamera && getUIController().getShutterButton().isShown() && getUIController().getShutterButton().isEnabled()) {
            boolean stop = this.mMediaRecorderRecording;
            this.mHandler.removeMessages(24);
            if (stop) {
                onStopVideoRecording(false);
                updateParametersAfterRecording();
            } else if (checkCallingState()) {
                this.mActivity.getScreenHint().updateHint();
                if (Storage.isLowStorageAtLastPoint()) {
                    Log.v("videocamera", "Storage issue, ignore the start request");
                    return;
                }
                enableCameraControls(false);
                if (this.mFocusManager.canRecord()) {
                    record();
                } else {
                    Log.v("videocamera", "wait for autofocus");
                    this.mInStartingFocusRecording = true;
                }
            } else {
                return;
            }
            getUIController().getShutterButton().enableControls(false);
        }
    }

    public void record() {
        Log.v("videocamera", "record");
        playCameraSound(2);
        startVideoRecording();
        this.mHandler.sendEmptyMessageDelayed(4, 500);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateParametersAfterRecording() {
        /*
        r2 = this;
        r0 = r2.mParameters;
        if (r0 == 0) goto L_0x0008;
    L_0x0004:
        r0 = r2.mCameraDevice;
        if (r0 != 0) goto L_0x0009;
    L_0x0008:
        return;
    L_0x0009:
        r0 = com.android.camera.Device.isPad();
        if (r0 == 0) goto L_0x003f;
    L_0x000f:
        r0 = r2.mParameters;
        r0 = r0.isVideoStabilizationSupported();
        if (r0 == 0) goto L_0x003f;
    L_0x0017:
        r0 = r2.mPreferences;
        r0 = com.android.camera.CameraSettings.isMovieSolidOn(r0);
        if (r0 == 0) goto L_0x003f;
    L_0x001f:
        r0 = "videocamera";
        r1 = "set video stabilization to false";
        android.util.Log.v(r0, r1);
        r0 = r2.mParameters;
        r1 = 0;
        r0.setVideoStabilization(r1);
        r0 = r2.mCameraDevice;
        r1 = r2.mParameters;
        r0.setParameters(r1);
        r0 = r2.mActivity;
        r0 = r0.getCameraScreenNail();
        r1 = 1;
        r0.setVideoStabilizationCropped(r1);
    L_0x003f:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.VideoModule.updateParametersAfterRecording():void");
    }

    public boolean onShutterButtonLongClick() {
        return false;
    }

    protected void readVideoPreferences() {
        int quality = CameraSettings.getVideoQuality();
        Intent intent = this.mActivity.getIntent();
        if (intent.hasExtra("android.intent.extra.videoQuality")) {
            if (intent.getIntExtra("android.intent.extra.videoQuality", 0) > 0) {
                quality = 1;
            } else {
                quality = 0;
            }
        }
        this.mHfr = CameraSettings.getVideoSpeed(this.mPreferences);
        this.mTimeBetweenTimeLapseFrameCaptureMs = 0;
        this.mCaptureTimeLapse = false;
        if ("fast".equals(this.mHfr)) {
            boolean z;
            this.mTimeBetweenTimeLapseFrameCaptureMs = Integer.parseInt(this.mPreferences.getString("pref_video_time_lapse_frame_interval_key", getString(R.string.pref_video_time_lapse_frame_interval_default)));
            if (this.mTimeBetweenTimeLapseFrameCaptureMs != 0) {
                z = true;
            } else {
                z = false;
            }
            this.mCaptureTimeLapse = z;
            if (this.mCaptureTimeLapse) {
                quality += 1000;
                if (quality < 1000 || quality > 1018) {
                    quality -= 1000;
                    Editor editor = this.mPreferences.edit();
                    editor.putString("pref_video_speed_key", "normal");
                    editor.apply();
                    this.mCaptureTimeLapse = false;
                    RotateTextToast.getInstance(this.mActivity).show(R.string.time_lapse_error, this.mOrientation);
                    getUIController().getSettingPage().reload();
                }
            }
            this.mQuality = quality % 1000;
        } else {
            this.mQuality = quality;
            if ("slow".equals(this.mHfr)) {
                quality = getHFRQuality(this.mCameraId, quality);
            }
        }
        if (!(this.mProfile == null || this.mProfile.quality % 1000 == this.mQuality)) {
            stopObjectTracking(false);
        }
        this.mProfile = fetchProfile(this.mCameraId, quality);
        Log.v("videocamera", "readVideoPreferences: frameRate=" + this.mProfile.videoFrameRate + ", w=" + this.mProfile.videoFrameWidth + ", h=" + this.mProfile.videoFrameHeight + ", codec=" + this.mProfile.videoCodec);
        getDesiredPreviewSize();
        if (intent.hasExtra("android.intent.extra.durationLimit")) {
            this.mMaxVideoDurationInMs = intent.getIntExtra("android.intent.extra.durationLimit", 0) * 1000;
        } else if (!CameraSettings.is4KHigherVideoQuality(this.mQuality) || this.mCaptureTimeLapse) {
            this.mMaxVideoDurationInMs = 0;
        } else {
            this.mMaxVideoDurationInMs = 480000;
        }
        if (this.mMaxVideoDurationInMs != 0 && this.mMaxVideoDurationInMs < 1000) {
            this.mMaxVideoDurationInMs = 1000;
        }
    }

    protected CamcorderProfile fetchProfile(int cameraId, int quality) {
        return CamcorderProfile.get(cameraId, quality);
    }

    private int getHFRQuality(int cameraId, int quality) {
        Integer hfrQuality = (Integer) VIDEO_QUALITY_TO_HIGHSPEED.get(Integer.valueOf(quality));
        if (hfrQuality != null && isProfileExist(cameraId, hfrQuality)) {
            return hfrQuality.intValue();
        }
        Log.w("videocamera", "cannot find hfrquality in VIDEO_QUALITY_TO_HIGHSPEED, quality " + quality + " hfrQuality=" + hfrQuality);
        return quality;
    }

    protected int getNormalVideoFrameRate() {
        if ("slow".equals(this.mHfr) || this.mProfile == null) {
            return 30;
        }
        return this.mProfile.videoFrameRate;
    }

    protected void setHFRSpeed(MediaRecorder recorder, int speed) {
    }

    private void getDesiredPreviewSize() {
        this.mParameters = this.mCameraDevice.getParameters();
        if (Device.isMTKPlatform() && "slow".equals(this.mHfr)) {
            this.mDesiredPreviewWidth = this.mProfile.videoFrameWidth;
            this.mDesiredPreviewHeight = this.mProfile.videoFrameHeight;
        } else if (this.mParameters.getSupportedVideoSizes() == null) {
            this.mDesiredPreviewWidth = this.mProfile.videoFrameWidth;
            this.mDesiredPreviewHeight = this.mProfile.videoFrameHeight;
        } else if ((Device.IS_MI4 || Device.IS_X5) && CameraSettings.is4KHigherVideoQuality(this.mQuality)) {
            this.mDesiredPreviewWidth = this.mProfile.videoFrameWidth;
            this.mDesiredPreviewHeight = this.mProfile.videoFrameHeight;
        } else {
            double d;
            List<Size> sizes = sProxy.getSupportedPreviewSizes(this.mParameters);
            Size preferred = this.mParameters.getPreferredPreviewSizeForVideo();
            int product = preferred.width * preferred.height;
            Iterator<Size> it = sizes.iterator();
            while (it.hasNext()) {
                Size size = (Size) it.next();
                if (size.width * size.height > product) {
                    it.remove();
                }
            }
            Activity activity = this.mActivity;
            if (Device.IS_MI3TD && this.mQuality == 0) {
                d = 1.3333333333333333d;
            } else {
                d = ((double) this.mProfile.videoFrameWidth) / ((double) this.mProfile.videoFrameHeight);
            }
            Size optimalSize = Util.getOptimalPreviewSize(activity, sizes, d);
            this.mDesiredPreviewWidth = optimalSize.width;
            this.mDesiredPreviewHeight = optimalSize.height;
        }
        Log.v("videocamera", "mDesiredPreviewWidth=" + this.mDesiredPreviewWidth + ". mDesiredPreviewHeight=" + this.mDesiredPreviewHeight);
    }

    private void prepareUIByPreviewSize() {
        if (!CameraSettings.sCroppedIfNeeded || Device.isMDPRender()) {
            if (1 != this.mUIStyle) {
                this.mUIStyle = 1;
                CameraSettings.changeUIByPreviewSize(this.mActivity, this.mUIStyle, this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
                changePreviewSurfaceSize();
            }
            getUIController().getPreviewFrame().setAspectRatio(CameraSettings.getPreviewAspectRatio(16, 9));
            return;
        }
        this.mUIStyle = 1;
    }

    private void resizeForPreviewAspectRatio() {
        CameraInfo info = CameraHolder.instance().getCameraInfo()[this.mCameraId];
        if (((info.orientation - Util.getDisplayRotation(this.mActivity)) + 360) % 180 == 0) {
            getUIController().getPreviewFrame().setAspectRatio(((float) this.mProfile.videoFrameHeight) / ((float) this.mProfile.videoFrameWidth));
        } else {
            getUIController().getPreviewFrame().setAspectRatio(((float) this.mProfile.videoFrameWidth) / ((float) this.mProfile.videoFrameHeight));
        }
    }

    public void onResumeBeforeSuper() {
        super.onResumeBeforeSuper();
    }

    public void onResumeAfterSuper() {
        if (!isVideoRecording()) {
            super.onResumeAfterSuper();
            if (!this.mOpenCameraFail && !this.mCameraDisabled && PermissionManager.checkCameraLaunchPermissions()) {
                if (!(this.mIsVideoCaptureIntent && getUIController().getReviewDoneView().getVisibility() == 0)) {
                    getUIController().onResume();
                }
                if (this.mCameraDevice == null && (!this.mPreviewing || this.mWaitForRelease)) {
                    CameraDataAnalytics.instance().trackEventTime("open_camera_times_key");
                    openCamera();
                    if (hasCameraException()) {
                        onCameraException();
                        return;
                    }
                    onCameraOpen();
                    initializeCapabilities();
                    readVideoPreferences();
                    resizeForPreviewAspectRatio();
                    showFirstUseHintIfNeeded();
                }
                updateStereoSettings(true);
                if (!this.mPreviewing || this.mWaitForRelease) {
                    if (!Device.isMDPRender() || getUIController().getSurfaceViewFrame().isSurfaceViewAvailable()) {
                        startPreview();
                        onPreviewStart();
                    }
                    this.mWaitForRelease = false;
                }
                initializeZoom();
                initializeExposureCompensation();
                keepScreenOnAwhile();
                IntentFilter intentFilter = new IntentFilter("android.intent.action.MEDIA_MOUNTED");
                intentFilter.addAction("android.intent.action.MEDIA_EJECT");
                intentFilter.addAction("android.intent.action.MEDIA_UNMOUNTED");
                intentFilter.addAction("android.intent.action.MEDIA_SCANNER_STARTED");
                intentFilter.addAction("android.intent.action.MEDIA_SCANNER_FINISHED");
                intentFilter.addDataScheme("file");
                this.mReceiver = new MyBroadcastReceiver();
                this.mActivity.registerReceiver(this.mReceiver, intentFilter);
                if (!this.mIsVideoCaptureIntent) {
                    this.mActivity.getThumbnailUpdater().getLastThumbnail();
                }
                onSettingsBack();
                if (this.mPreviewing) {
                    this.mOnResumeTime = SystemClock.uptimeMillis();
                    this.mHandler.sendEmptyMessageDelayed(1, 100);
                }
                this.mActivity.loadCameraSound(2);
                this.mActivity.loadCameraSound(3);
                if (getUIController().getReviewDoneView().getVisibility() == 0) {
                    ignoreTouchEvent(true);
                }
            }
        }
    }

    public void onPauseBeforeSuper() {
        super.onPauseBeforeSuper();
        if (!isVideoRecording() || isVideoProcessing()) {
            waitStereoSwitchThread();
            getUIController().onPause();
            if (this.mMediaRecorderRecording) {
                stopObjectTracking(false);
                onStopVideoRecording(true);
                closeCamera();
            } else {
                boolean isReleaseLaterForGallery;
                if (this.mActivity.isGotoGallery()) {
                    isReleaseLaterForGallery = Device.isReleaseLaterForGallery();
                } else {
                    isReleaseLaterForGallery = false;
                }
                if (isReleaseLaterForGallery) {
                    this.mWaitForRelease = true;
                } else {
                    releaseResources();
                }
            }
            updateStereoSettings(false);
            closeVideoFileDescriptor();
            if (this.mReceiver != null) {
                this.mActivity.unregisterReceiver(this.mReceiver);
                this.mReceiver = null;
            }
            this.mActivity.getSensorStateManager().reset();
            resetScreenOn();
            this.mHandler.removeMessages(1);
            this.mHandler.removeMessages(5);
            this.mHandler.removeMessages(6);
            this.mHandler.removeMessages(17);
            this.mHandler.removeMessages(18);
            this.mHandler.removeMessages(19);
            this.mHandler.removeMessages(14);
            if (this.mLoadThumbnailTask != null) {
                this.mLoadThumbnailTask.cancel(true);
            }
            if (this.mHandler.hasMessages(15)) {
                this.mHandler.removeMessages(15);
                showStoppingUI();
            }
            if (this.mHandler.hasCallbacks(this.mRestoreRunnable)) {
                this.mHandler.removeCallbacks(this.mRestoreRunnable);
                this.mRestoreRunnable.run();
            }
            if (!(this.mActivity.isActivityPaused() || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key"))) {
                PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
            }
            this.mPendingSwitchCameraId = -1;
            this.mSwitchingCamera = false;
        }
    }

    public void onPauseAfterSuper() {
        if (!isVideoRecording() || isVideoProcessing()) {
            super.onPauseAfterSuper();
        }
    }

    public void onStop() {
        super.onStop();
        if (this.mMediaRecorderRecording) {
            this.mIsFromStop = true;
            onPauseBeforeSuper();
            this.mActivity.pause();
            onPauseAfterSuper();
            this.mIsFromStop = false;
        }
        if (this.mActivity.isNeedResetGotoGallery() && Device.isReleaseLaterForGallery()) {
            releaseResources();
        }
    }

    protected void setDisplayOrientation() {
        super.setDisplayOrientation();
        if (this.mFocusManager != null) {
            this.mFocusManager.setDisplayOrientation(this.mCameraDisplayOrientation);
        }
    }

    protected void startPreview() {
        Log.v("videocamera", "startPreview " + this.mPreviewing);
        if (this.mCameraDevice != null && this.mFocusManager != null) {
            this.mCameraDevice.setErrorCallback(this.mErrorCallback);
            if (this.mPreviewing) {
                stopPreview();
            }
            setDisplayOrientation();
            this.mCameraDevice.setDisplayOrientation(this.mCameraDisplayOrientation);
            setCameraParameters();
            try {
                if (Device.isMDPRender()) {
                    SurfaceHolder sh = getUIController().getSurfaceViewFrame().getSurfaceHolder();
                    if (sh == null) {
                        Log.w("videocamera", "startPreview: holder for preview are not ready.");
                        return;
                    }
                    this.mCameraDevice.setPreviewDisplay(sh);
                } else {
                    this.mCameraDevice.setPreviewTexture(this.mActivity.getCameraScreenNail().getSurfaceTexture());
                }
                this.mCameraDevice.startPreviewAsync();
                this.mFocusManager.resetFocused();
                this.mPreviewing = true;
                manuallyTriggerAutoFocus();
            } catch (Exception ex) {
                closeCamera();
                throw new RuntimeException("startPreview or setPreviewSurfaceTexture failed", ex);
            }
        }
    }

    private void manuallyTriggerAutoFocus() {
        if (!TextUtils.equals("continuous-video", this.mPreferences.getString("pref_video_focusmode_key", getString(R.string.pref_video_focusmode_entryvalue_default)))) {
            this.mHandler.sendEmptyMessageDelayed(24, 1000);
        }
    }

    protected void stopPreview() {
        Log.v("videocamera", "stopPreview");
        if (currentIsMainThread()) {
            stopObjectTracking(false);
        }
        this.mHandler.removeMessages(24);
        this.mCameraDevice.stopPreview();
        this.mPreviewing = false;
        if (this.mFocusManager != null) {
            this.mFocusManager.resetFocused();
        }
    }

    protected void closeCamera() {
        Log.v("videocamera", "closeCamera");
        if (this.mCameraDevice == null) {
            Log.d("videocamera", "already stopped.");
            return;
        }
        stopPreview();
        this.mCameraDevice.setErrorCallback(null);
        this.mCameraDevice.removeAllAsyncMessage();
        CameraHolder.instance().release();
        this.mCameraDevice = null;
        this.mPreviewing = false;
        this.mSnapshotInProgress = false;
    }

    public void onUserInteraction() {
        super.onUserInteraction();
        if (!this.mMediaRecorderRecording) {
            keepScreenOnAwhile();
        }
    }

    public boolean onBackPressed() {
        if (this.mPaused) {
            return true;
        }
        if (this.mStereoSwitchThread != null) {
            return false;
        }
        if (this.mMediaRecorderRecording) {
            long now = System.currentTimeMillis();
            if (now - this.mLastBackPressedTime > 3000) {
                this.mLastBackPressedTime = now;
                Toast.makeText(this.mActivity, getString(R.string.record_back_pressed_hint), 0).show();
            } else {
                onStopVideoRecording(false);
            }
            return true;
        } else if (getUIController().onBack()) {
            return true;
        } else {
            if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                getUIController().getStereoButton().setStereoValue(false, true, true);
                return true;
            } else if (getUIController().getSettingPage().isItemSelected() && getUIController().getSettingPage().resetSettings()) {
                return true;
            } else {
                if (!getUIController().getPreviewPage().isPopupShown()) {
                    return super.onBackPressed();
                }
                PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
                return true;
            }
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

    public boolean handleMessage(int what, int sender, final Object extra1, Object extra2) {
        if (super.handleMessage(what, sender, extra1, extra2)) {
            return true;
        }
        switch (sender) {
            case R.id.hide_mode_animation_done:
                getUIController().useProperView();
                return true;
            case R.id.v6_thumbnail_button:
                onThumbnailClicked(null);
                return true;
            case R.id.v6_video_pause_button:
                onPauseButtonClick();
                return true;
            case R.id.v6_shutter_button:
                if (what == 0) {
                    onShutterButtonClick();
                } else if (what == 1) {
                    onShutterButtonLongClick();
                } else if (what == 2) {
                    if (isBackCamera()) {
                        Point start = (Point) extra1;
                        Point center = (Point) extra2;
                        getUIController().getSmartShutterButton().flyin(start.x, start.y, center.x, center.y);
                    }
                } else if (what == 3) {
                    onShutterButtonFocus(((Boolean) extra1).booleanValue());
                }
                return true;
            case R.id.v6_module_picker:
                Runnable r = new Runnable() {
                    public void run() {
                        VideoModule.this.mActivity.getCameraScreenNail().animateModuleChangeBefore();
                        VideoModule.this.switchToOtherMode(((Integer) extra1).intValue());
                    }
                };
                getUIController().enableControls(false);
                ignoreTouchEvent(true);
                getUIController().getFocusView().clear();
                getUIController().getBottomControlLowerPanel().animationSwitchToCamera(r);
                this.mActivity.getCameraScreenNail().switchModule();
                return true;
            case R.id.v6_video_capture_button:
                capture();
                CameraDataAnalytics.instance().trackEvent("capture_nums_video_capture");
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
                if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                    this.mSettingsOverrider.removeSavedSetting("pref_camera_flashmode_key");
                    getUIController().getStereoButton().switchOffStereo(true);
                    return true;
                } else if (this.mMutexModePicker.isNormal() || this.mMutexModePicker.isSupportedFlashOn() || this.mMutexModePicker.isSupportedTorch()) {
                    if (this.mFocusManager.isFocusing()) {
                        cancelAutoFocus();
                        getUIController().getFocusView().clear();
                    }
                    onSharedPreferenceChanged();
                    return true;
                } else {
                    if (getUIController().getModeExitView().isExitButtonShown()) {
                        getUIController().getModeExitView().clearExitButtonClickListener(true);
                    } else {
                        this.mMutexModePicker.resetMutexMode();
                    }
                    return true;
                }
            case R.id.v6_hdr:
                switchMutexHDR();
                return true;
            case R.id.stereo_switch_image:
                if (what == 7) {
                    onSettingValueChanged((String) extra1);
                } else {
                    if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                        updateStereoSettings(true);
                    } else {
                        this.mSettingsOverrider.restoreSettings();
                    }
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
            case R.id.v6_video_btn_play:
                startPlayVideoActivity();
                return true;
            case R.id.v6_setting_page:
                if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                    getUIController().getStereoButton().switchOffStereo(true);
                    return true;
                }
                getUIController().getHdrButton().updateVisible();
                if (what == 7) {
                    onSharedPreferenceChanged();
                } else if (what == 6) {
                    if ("pref_video_speed_fast_key".equals(extra1) || "pref_video_speed_slow_key".equals(extra1)) {
                        this.mRestartPreview = true;
                        onSharedPreferenceChanged();
                    } else if ("pref_video_hdr_key".equals(extra1)) {
                        switchMutexHDR();
                    }
                }
                getUIController().getStereoButton().updateVisible();
                return true;
            case R.id.setting_button:
                openSettingActivity();
                return true;
            case R.id.v6_surfaceview:
                if (Device.isMDPRender() && !this.mPreviewing) {
                    startPreview();
                    onPreviewStart();
                }
                return true;
            default:
                return false;
        }
    }

    private void onPreviewStart() {
        if (this.mPreviewing) {
            this.mActivity.getCameraScreenNail().animateModuleChangeAfter();
            getUIController().getFocusView().initialize(this);
            getUIController().onCameraOpen();
            updateMutexModePreference();
            this.mHandler.removeMessages(14);
            this.mHandler.sendEmptyMessageDelayed(14, 100);
            enableCameraControls(true);
        }
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean z = false;
        if (this.mPaused) {
            return true;
        }
        switch (keyCode) {
            case 24:
            case 25:
            case 87:
            case 88:
                if (getUIController().getPreviewPage().isPreviewPageVisible()) {
                    if (keyCode == 24 || keyCode == 88) {
                        z = true;
                    }
                    if (handleVolumeKeyEvent(z, true, event.getRepeatCount())) {
                        return true;
                    }
                }
                break;
            case 27:
            case 66:
                if (event.getRepeatCount() == 0 && getUIController().getPreviewPage().isPreviewPageVisible()) {
                    onShutterButtonClick();
                    if (Util.isFingerPrintKeyEvent(event)) {
                        CameraDataAnalytics.instance().trackEvent("record_times_finger");
                    }
                    return true;
                }
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void performVolumeKeyClicked(int repeatCount, boolean pressed) {
        if (repeatCount == 0 && pressed) {
            onShutterButtonClick();
        }
    }

    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case 27:
            case 66:
                getUIController().getShutterButton().setPressed(false);
                return true;
            default:
                return super.onKeyUp(keyCode, event);
        }
    }

    private void doReturnToCaller(boolean valid) {
        int resultCode;
        Intent resultIntent = new Intent();
        if (valid) {
            resultCode = -1;
            resultIntent.setData(this.mCurrentVideoUri);
            resultIntent.setFlags(1);
        } else {
            resultCode = 0;
        }
        this.mActivity.setResult(resultCode, resultIntent);
        this.mActivity.finish();
    }

    private void cleanupEmptyFile() {
        if (this.mVideoFilename != null) {
            File f = new File(this.mVideoFilename);
            if (f.length() == 0 && f.delete()) {
                Log.v("videocamera", "Empty video file deleted: " + this.mVideoFilename);
                this.mVideoFilename = null;
            }
        }
    }

    private void initializeRecorder() {
        Log.v("videocamera", "initializeRecorder");
        if (this.mCameraDevice != null) {
            int rotation;
            Bundle myExtras = this.mActivity.getIntent().getExtras();
            this.mVideoWidth = this.mProfile.videoFrameWidth;
            this.mVideoHeight = this.mProfile.videoFrameHeight;
            long requestedSizeLimit = 0;
            closeVideoFileDescriptor();
            if (this.mIsVideoCaptureIntent && myExtras != null) {
                Uri saveUri = (Uri) myExtras.getParcelable("output");
                if (saveUri != null) {
                    try {
                        this.mVideoFileDescriptor = this.mContentResolver.openFileDescriptor(saveUri, "rw");
                        this.mCurrentVideoUri = saveUri;
                    } catch (FileNotFoundException ex) {
                        Log.e("videocamera", ex.toString());
                    }
                }
                requestedSizeLimit = myExtras.getLong("android.intent.extra.sizeLimit");
            }
            this.mMediaRecorder = new MediaRecorder();
            this.mCameraDevice.unlock();
            this.mMediaRecorder.setCamera(this.mCameraDevice.getCamera());
            if ("normal".equals(this.mHfr)) {
                this.mMediaRecorder.setAudioSource(5);
            } else {
                this.mProfile.audioCodec = -1;
            }
            this.mMediaRecorder.setVideoSource(1);
            this.mProfile.duration = this.mMaxVideoDurationInMs;
            setProfileToRecorder();
            this.mMediaRecorder.setMaxDuration(this.mMaxVideoDurationInMs);
            if (Device.isSupportedAudioFocus()) {
                String audioFocus;
                if (isBackCamera()) {
                    audioFocus = this.mPreferences.getString("pref_audio_focus_key", getString(R.string.pref_audio_focus_default));
                } else {
                    audioFocus = getString(R.string.pref_audio_focus_entryvalue_front);
                }
                Log.v("videocamera", "set AudioParam camcorder_mode=" + audioFocus);
                AudioSystem.setParameters("camcorder_mode=" + audioFocus);
            }
            int quality = CameraSettings.getVideoQuality();
            if (Device.IS_MI2 && "fast".equals(this.mHfr) && quality == 5) {
                this.mMediaRecorder.setVideoEncodingBitRate(4000000);
            }
            if (this.mCaptureTimeLapse) {
                this.mMediaRecorder.setCaptureRate(1000.0d / ((double) this.mTimeBetweenTimeLapseFrameCaptureMs));
            }
            configMediaRecorder(this.mMediaRecorder);
            Location loc = LocationManager.instance().getCurrentLocation();
            if (loc != null) {
                this.mMediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
            }
            if (this.mVideoFileDescriptor != null) {
                this.mMediaRecorder.setOutputFile(this.mVideoFileDescriptor.getFileDescriptor());
            } else {
                generateVideoFilename(this.mProfile.fileFormat);
                this.mMediaRecorder.setOutputFile(this.mVideoFilename);
            }
            long maxFileSize = this.mActivity.getScreenHint().getStorageSpace() - 52428800;
            if (3670016000L < maxFileSize) {
                Log.v("videocamera", "need reduce , now maxFileSize = " + maxFileSize);
                maxFileSize = 3670016000L;
            }
            if (maxFileSize < VIDEO_MIN_SINGLE_FILE_SIZE) {
                maxFileSize = VIDEO_MIN_SINGLE_FILE_SIZE;
            }
            if (requestedSizeLimit > 0 && requestedSizeLimit < maxFileSize) {
                maxFileSize = requestedSizeLimit;
            }
            try {
                Log.v("videocamera", "maxFileSize = " + maxFileSize);
                this.mMediaRecorder.setMaxFileSize(maxFileSize);
            } catch (RuntimeException e) {
            }
            if ("slow".equals(this.mHfr)) {
                setHFRSpeed(this.mMediaRecorder, this.mProfile.videoFrameRate / 30);
            } else {
                setHFRSpeed(this.mMediaRecorder, 1);
            }
            CameraInfo info = CameraHolder.instance().getCameraInfo()[this.mCameraId];
            if (this.mOrientation == -1) {
                rotation = info.orientation;
            } else if (info.facing == 1) {
                rotation = ((info.orientation - this.mOrientation) + 360) % 360;
            } else {
                rotation = (info.orientation + this.mOrientation) % 360;
            }
            this.mMediaRecorder.setOrientationHint(rotation);
            this.mOrientationCompensationAtRecordStart = this.mOrientationCompensation;
            try {
                this.mMediaRecorder.prepare();
                this.mMediaRecorder.setOnErrorListener(this);
                this.mMediaRecorder.setOnInfoListener(this);
            } catch (IOException e2) {
                Log.e("videocamera", "prepare failed for " + this.mVideoFilename, e2);
                releaseMediaRecorder();
                throw new RuntimeException(e2);
            }
        }
    }

    private void releaseMediaRecorder() {
        Log.v("videocamera", "Releasing media recorder.");
        if (this.mMediaRecorder != null) {
            cleanupEmptyFile();
            this.mMediaRecorder.reset();
            this.mMediaRecorder.release();
            this.mMediaRecorder = null;
        }
        if (Device.isSupportedAudioFocus()) {
            Log.v("videocamera", "restore AudioParam camcorder_mode=" + getString(R.string.pref_audio_focus_default));
            AudioSystem.setParameters("camcorder_mode=" + getString(R.string.pref_audio_focus_default));
        }
        this.mVideoFilename = null;
    }

    private void generateVideoFilename(int outputFileFormat) {
        long dateTaken = System.currentTimeMillis();
        String title = createName(dateTaken);
        String filename = title + convertOutputFormatToFileExt(outputFileFormat);
        String mime = convertOutputFormatToMimeType(outputFileFormat);
        String path = Storage.DIRECTORY + '/' + filename;
        String tmpPath = path + ".tmp" + this.mCurrentFileNumber;
        this.mCurrentVideoValues = new ContentValues(7);
        this.mCurrentVideoValues.put("title", title);
        this.mCurrentVideoValues.put("_display_name", filename);
        this.mCurrentVideoValues.put("datetaken", Long.valueOf(dateTaken));
        this.mCurrentVideoValues.put("mime_type", mime);
        this.mCurrentVideoValues.put("_data", path);
        this.mCurrentVideoValues.put("resolution", Integer.toString(this.mProfile.videoFrameWidth) + "x" + Integer.toString(this.mProfile.videoFrameHeight));
        Location loc = LocationManager.instance().getCurrentLocation();
        if (!(loc == null || (loc.getLatitude() == 0.0d && loc.getLongitude() == 0.0d))) {
            this.mCurrentVideoValues.put("latitude", Double.valueOf(loc.getLatitude()));
            this.mCurrentVideoValues.put("longitude", Double.valueOf(loc.getLongitude()));
        }
        this.mVideoFilename = tmpPath;
        Log.v("videocamera", "New video filename: " + this.mVideoFilename);
    }

    private long getDuration(String filePath) {
        long parseLong;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            parseLong = Long.parseLong(retriever.extractMetadata(9));
            return parseLong;
        } catch (IllegalArgumentException e) {
            parseLong = "IllegalArgumentException when getDuration()";
            Log.e("videocamera", parseLong, e);
            return 0;
        } catch (RuntimeException e2) {
            parseLong = "RuntimeException when getDuration()";
            Log.e("videocamera", parseLong, e2);
            return 0;
        } finally {
            retriever.release();
        }
    }

    private String insertPostfix(String name, String postfix) {
        StringBuffer sBuffer = new StringBuffer(name);
        sBuffer.insert(sBuffer.lastIndexOf("."), postfix);
        return sBuffer.toString();
    }

    private boolean addVideoToMediaStore(ContentValues values) {
        boolean fail = false;
        if (this.mVideoFileDescriptor != null) {
            return false;
        }
        values.put("_size", Long.valueOf(new File(this.mCurrentVideoFilename).length()));
        values.put("duration", Long.valueOf(getDuration(this.mCurrentVideoFilename)));
        try {
            String finalName = values.getAsString("_data");
            if (this.mCurrentFileNumber > 0) {
                String postfix = String.format(Locale.ENGLISH, "_%d", new Object[]{Integer.valueOf(this.mCurrentFileNumber)});
                finalName = insertPostfix(finalName, postfix);
                values.put("_data", finalName);
                values.put("title", this.mBaseFileName + postfix);
                values.put("_display_name", insertPostfix(values.getAsString("_display_name"), postfix));
            }
            if (new File(this.mCurrentVideoFilename).renameTo(new File(finalName))) {
                this.mCurrentVideoFilename = finalName;
            } else {
                values.put("_data", this.mCurrentVideoFilename);
            }
            this.mCurrentVideoUri = this.mContentResolver.insert(Media.EXTERNAL_CONTENT_URI, values);
            this.mActivity.addSecureUri(this.mCurrentVideoUri);
            if (VERSION.SDK_INT < 24) {
                this.mActivity.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", this.mCurrentVideoUri));
            }
            Storage.saveToCloudAlbum(this.mActivity, this.mCurrentVideoFilename);
        } catch (Exception e) {
            Log.e("videocamera", "failed to add video to media store", e);
            this.mCurrentVideoUri = null;
            this.mCurrentVideoFilename = null;
            fail = true;
        } finally {
            Log.v("videocamera", "Current video URI: " + this.mCurrentVideoUri);
        }
        return fail;
    }

    private void deleteCurrentVideo() {
        if (this.mCurrentVideoFilename != null) {
            deleteVideoFile(this.mCurrentVideoFilename);
            this.mCurrentVideoFilename = null;
            if (this.mCurrentVideoUri != null) {
                Util.safeDelete(this.mCurrentVideoUri, null, null);
                this.mCurrentVideoUri = null;
            }
        }
        this.mActivity.getScreenHint().updateHint();
    }

    private void deleteVideoFile(String fileName) {
        Log.v("videocamera", "Deleting video " + fileName);
        if (!new File(fileName).delete()) {
            Log.v("videocamera", "Could not delete " + fileName);
        }
    }

    public void onError(MediaRecorder mr, int what, int extra) {
        Log.e("videocamera", "MediaRecorder error. what=" + what + ". extra=" + extra);
        if (what == 1) {
            stopVideoRecordingAsync();
            this.mActivity.getScreenHint().updateHint();
        }
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == 800) {
            if (this.mMediaRecorderRecording) {
                onStopVideoRecording(false);
            }
        } else if (what == 801 && this.mMediaRecorderRecording) {
            Log.v("videocamera", "reached max size " + this.mCurrentFileNumber);
            if (-1 < this.mCurrentFileNumber) {
                onRestartVideoRecording();
                return;
            }
            onStopVideoRecording(false);
            if (!this.mActivity.getScreenHint().isScreenHintVisible()) {
                Toast.makeText(this.mActivity, R.string.video_reach_size_limit, 1).show();
            }
        }
    }

    private void onRestartVideoRecording() {
        if (this.mMediaRecorderRecording && this.mVideoSavingTask == null) {
            this.mRecorderBusy = true;
            this.mVideoSavingTask = new SavingTask(true);
            this.mVideoSavingTask.start();
        }
    }

    protected boolean startRecordVideo() {
        initializeRecorder();
        if (this.mMediaRecorder == null) {
            Log.e("videocamera", "Fail to initialize media recorder");
            return false;
        }
        try {
            this.mMediaRecorder.start();
            return true;
        } catch (RuntimeException e) {
            Log.e("videocamera", "Could not start media recorder. ", e);
            if (e instanceof IllegalStateException) {
                this.mActivity.getScreenHint().showConfirmMessage(R.string.confirm_recording_fail_title, R.string.confirm_recording_fail_recorder_busy_alert);
            }
            releaseMediaRecorder();
            this.mCameraDevice.lock();
            return false;
        }
    }

    public boolean isVideoRecording() {
        return !this.mIsFromStop ? this.mMediaRecorderRecording : false;
    }

    protected boolean isZoomEnabled() {
        if (this.mVideoSavingTask != null || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            return false;
        }
        return true;
    }

    private void startVideoRecording() {
        Log.v("videocamera", "startVideoRecording");
        this.mCurrentVideoUri = null;
        this.mCurrentFileNumber = this.mIsVideoCaptureIntent ? -1 : 0;
        silenceSounds();
        prepareRecording();
        if (startRecordVideo()) {
            ignoreTouchEvent(false);
            Log.v("videocamera", "startVideoRecording process done");
            this.mParameters = this.mCameraDevice.getParameters();
            this.mActivity.sendBroadcast(new Intent("com.android.camera.action.start_video_recording"));
            this.mMediaRecorderRecording = true;
            this.mSavingResult = false;
            this.mMediaRecorderRecordingPaused = false;
            this.mRecordingStartTime = SystemClock.uptimeMillis();
            this.mPauseClickTime = SystemClock.uptimeMillis();
            this.mCurrentShowIndicator = -1;
            this.mRecordingTime = "";
            showRecordingUI(true);
            this.mTelephonyManager.listen(this.mPhoneStateListener, 32);
            Log.v("videocamera", "listen call state");
            updateRecordingTime();
            keepScreenOn();
            trackRecordingInfo();
            AutoLockManager.getInstance(this.mActivity).hibernateDelayed();
            return;
        }
        enableCameraControls(true);
        this.mAudioManager.abandonAudioFocus(null);
        restoreMusicSound();
    }

    private boolean isAudioFocusPopupVisible(View popup) {
        if (popup == null || !(popup instanceof V6AbstractSettingPopup)) {
            return false;
        }
        return "pref_audio_focus_key".equals(((V6AbstractSettingPopup) popup).getKey());
    }

    private int computePopupTransY() {
        View view = getUIController().getBottomControlUpperPanel();
        if (view == null) {
            return 0;
        }
        return view.getHeight() - this.mActivity.getResources().getDimensionPixelSize(R.dimen.bottom_control_margin_bottom);
    }

    private void showRecordingUI(boolean recording) {
        boolean statusBarVisible = false;
        this.mRecordingUIShown = recording;
        View popup = getUIController().getSettingPage().getCurrentPopup();
        boolean isAudioFocusPopupShown = isAudioFocusPopupVisible(popup);
        boolean isFullScreen = getUIController().getPreviewFrame().isFullScreen();
        View popupParentLayout;
        if (recording) {
            getUIController().getShutterButton().changeImageWithAnimation(R.drawable.video_shutter_button_stop_bg, 200);
            getUIController().getVideoRecordingTimeView().setText("");
            if (isAudioFocusPopupShown) {
                getUIController().getPreviewPage().showPopupWithoutExitView();
                popup.setEnabled(true);
                popupParentLayout = getUIController().getPopupParentLayout();
                if (popupParentLayout != null) {
                    popupParentLayout.setTranslationY((float) computePopupTransY());
                }
                if (isFullScreen) {
                    ((GridSettingTextPopup) popup).shrink(this.mActivity.getResources().getDimensionPixelSize(R.dimen.audio_focus_popup_width));
                }
            } else if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                getUIController().getStereoButton().showPopup(false);
                getUIController().getPreviewPage().showPopupWithoutExitView();
                getUIController().getStereoButton().getPopup().setEnabled(true);
            } else {
                getUIController().getPreviewPage().setPopupVisible(false);
            }
            getUIController().getStereoButton().setVisibility(8);
            getUIController().getBottomControlPanel().setBackgroundVisible(false);
            getUIController().getBottomControlUpperPanel().animateOut(null);
            getUIController().getTopControlPanel().animateOut(null);
            if (getUIController().getSettingsStatusBar().getVisibility() == 0) {
                statusBarVisible = true;
            }
            if (statusBarVisible) {
                getUIController().getSettingsStatusBar().animateOut(new Runnable() {
                    public void run() {
                        VideoModule.this.getUIController().getVideoRecordingTimeView().animateIn(null, 150, true);
                    }
                });
            }
            getUIController().getThumbnailButton().animateOut(new Runnable() {
                public void run() {
                    if (!statusBarVisible) {
                        VideoModule.this.getUIController().getVideoRecordingTimeView().animateIn(null, 150, true);
                    }
                    VideoModule.this.getUIController().getPauseRecordingButton().setImageResource(R.drawable.ic_recording_pause);
                    VideoModule.this.getUIController().getPauseRecordingButton().animateIn(null, 100, true);
                    VideoModule.this.getUIController().getPauseRecordingButton().enableControls(true);
                    VideoModule.this.getUIController().getVideoCaptureButton().animateIn(null, 100, true);
                    VideoModule.this.getUIController().getVideoCaptureButton().enableControls(true);
                }
            }, 100, true);
            if (this.mIsVideoCaptureIntent) {
                getUIController().getReviewCanceledView().animateOut(null, 100, true);
                return;
            } else {
                getUIController().getModulePicker().animateOut(null, 100, true);
                return;
            }
        }
        getUIController().getShutterButton().changeImageWithAnimation(R.drawable.video_shutter_button_start_bg, 200);
        getUIController().getVideoCaptureButton().setVisibility(8);
        getUIController().getBottomControlPanel().setBackgroundVisible(true);
        if (isAudioFocusPopupShown) {
            popupParentLayout = getUIController().getPopupParentLayout();
            if (popupParentLayout != null) {
                popupParentLayout.setTranslationY(0.0f);
            }
            if (isFullScreen) {
                ((GridSettingTextPopup) popup).restoreFromShrink();
            }
        }
        if (isAudioFocusPopupShown || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            getUIController().getModeExitView().show();
        }
        if (!this.mIsVideoCaptureIntent) {
            getUIController().getPreviewPage().setPopupVisible(true);
            getUIController().getBottomControlUpperPanel().animateIn(null);
            getUIController().getTopControlPanel().animateIn(null);
        }
        getUIController().getVideoRecordingTimeView().animateOut(new Runnable() {
            public void run() {
                if (!VideoModule.this.mIsVideoCaptureIntent || VideoModule.this.mPaused) {
                    VideoModule.this.getUIController().getVideoRecordingTimeView().setText("");
                }
                if (VideoModule.this.getUIController().getSettingsStatusBar().isSubViewShown()) {
                    VideoModule.this.getUIController().getSettingsStatusBar().animateIn(null);
                }
            }
        }, 150, true);
        getUIController().getPauseRecordingButton().animateOut(new Runnable() {
            public void run() {
                VideoModule.this.getUIController().getThumbnailButton().animateIn(null, 100, true);
                if (!VideoModule.this.mIsVideoCaptureIntent || VideoModule.this.mPaused) {
                    VideoModule.this.getUIController().getModulePicker().animateIn(null, 100, true);
                } else {
                    VideoModule.this.getUIController().getReviewCanceledView().animateIn(null, 100, true);
                }
                VideoModule.this.getUIController().getStereoButton().updateVisible();
            }
        }, 100, true);
    }

    protected void updateStatusBar(String key) {
        super.updateStatusBar(key);
        this.mHandler.removeMessages(17);
        if (!this.mPaused && this.mMediaRecorderRecording && !this.mRecorderBusy) {
            if ((getUIController().getVideoRecordingTimeView().isShown() || this.mCurrentShowIndicator != -1) && getUIController().getSettingsStatusBar().getVisibility() == 0) {
                this.mCurrentShowIndicator = -1;
                getUIController().getVideoRecordingTimeView().clearAnimation();
                getUIController().getVideoRecordingTimeView().setVisibility(4);
            }
            this.mHandler.sendEmptyMessageDelayed(17, 1000);
        }
    }

    private void showAlert() {
        Bitmap bitmap = null;
        if (this.mVideoFileDescriptor != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(this.mVideoFileDescriptor.getFileDescriptor(), getUIController().getPreviewFrame().getWidth());
        } else if (this.mCurrentVideoFilename != null) {
            bitmap = Thumbnail.createVideoThumbnailBitmap(this.mCurrentVideoFilename, getUIController().getPreviewFrame().getWidth());
        }
        if (bitmap != null) {
            boolean z;
            int i = -this.mOrientationCompensationAtRecordStart;
            if (this.mCameraId == CameraHolder.instance().getFrontCameraId()) {
                z = true;
            } else {
                z = false;
            }
            getUIController().getReviewImageView().setImageBitmap(Util.rotateAndMirror(bitmap, i, z));
            getUIController().getReviewImageView().setVisibility(0);
        }
        Util.fadeIn(getUIController().getReviewPlayView());
        ignoreTouchEvent(true);
        getUIController().getSettingsStatusBar().hide();
        getUIController().getPreviewPage().setPopupVisible(false);
        getUIController().getShutterButton().animateOut(null, 100, true);
        getUIController().getReviewDoneView().animateIn(null, 100, true);
        getUIController().getBottomControlUpperPanel().animateOut(null);
        getUIController().getTopControlPanel().animateOut(null);
    }

    private void hideAlert() {
        Util.fadeOut(getUIController().getReviewImageView());
        Util.fadeOut(getUIController().getReviewPlayView());
        getUIController().getSettingsStatusBar().show();
        getUIController().getPreviewPage().setPopupVisible(true);
        getUIController().getReviewDoneView().animateOut(null, 100, true);
        getUIController().getShutterButton().animateIn(null, 100, true);
        getUIController().getBottomControlUpperPanel().animateIn(null);
        getUIController().getTopControlPanel().animateIn(null);
        getUIController().getBottomControlUpperPanel().setEnabled(true);
        enableCameraControls(true);
    }

    protected boolean subStopRecording() {
        boolean shouldAddToMediaStoreNow;
        synchronized (this) {
            shouldAddToMediaStoreNow = false;
            if (this.mMediaRecorderRecording) {
                try {
                    this.mMediaRecorder.setOnErrorListener(null);
                    this.mMediaRecorder.setOnInfoListener(null);
                    this.mMediaRecorder.stop();
                    shouldAddToMediaStoreNow = true;
                    this.mCurrentVideoFilename = this.mVideoFilename;
                    Log.v("videocamera", "stopVideoRecording: Setting current video filename: " + this.mCurrentVideoFilename);
                } catch (RuntimeException e) {
                    Log.e("videocamera", "stop fail", e);
                    if (this.mVideoFilename != null) {
                        deleteVideoFile(this.mVideoFilename);
                    }
                    synchronized (this.mTaskLock) {
                        this.mMediaRecorderRecording = false;
                        if (!isVideoProcessing()) {
                            this.mHandler.postAtFrontOfQueue(this.mRestoreRunnable);
                        }
                    }
                }
                if (this.mPaused) {
                    closeCamera();
                }
            }
            releaseMediaRecorder();
        }
        return shouldAddToMediaStoreNow;
    }

    protected void waitForRecorder() {
        synchronized (this.mTaskLock) {
            if (this.mVideoSavingTask != null && this.mMediaRecorderRecording) {
                try {
                    Log.i("videocamera", "Wait for releasing camera done in MediaRecorder");
                    this.mTaskLock.wait();
                } catch (InterruptedException e) {
                    Log.w("videocamera", "Got notify from Media recorder()", e);
                }
            }
        }
    }

    private void animateHold() {
        if (HOLD_WHEN_SAVING_VIDEO && !this.mIsVideoCaptureIntent && !this.mPaused) {
            this.mActivity.getCameraScreenNail().animateHold(getCameraRotation());
        }
    }

    private void animateSlide() {
        if (HOLD_WHEN_SAVING_VIDEO && !this.mIsVideoCaptureIntent && !this.mPaused) {
            this.mActivity.getCameraScreenNail().clearAnimation();
        }
    }

    private void stopVideoRecordingAsync() {
        if (!this.mRecorderBusy && this.mMediaRecorderRecording) {
            animateHold();
            this.mRecorderBusy = true;
            this.mHandler.removeMessages(3);
            this.mHandler.sendEmptyMessage(15);
            this.mVideoSavingTask = new SavingTask(false);
            this.mVideoSavingTask.start();
        }
    }

    private boolean isVideoProcessing() {
        return this.mVideoSavingTask != null ? this.mVideoSavingTask.isAlive() : false;
    }

    private void stopVideoOnPause() {
        Log.i("videocamera", "stopVideoOnPause() mMediaRecorderRecording =  " + this.mMediaRecorderRecording + " mRecorderBusy=" + this.mRecorderBusy);
        boolean videoSaving = false;
        if (this.mMediaRecorderRecording) {
            stopVideoRecordingAsync();
            videoSaving = isVideoProcessing();
        } else {
            releaseMediaRecorder();
        }
        if (videoSaving) {
            waitForRecorder();
        } else {
            closeVideoFileDescriptor();
        }
        Log.i("videocamera", "stopVideoOnPause()  videoSaving=" + videoSaving + ", mVideoSavingTask=" + this.mVideoSavingTask + ", mMediaRecorderRecording=" + this.mMediaRecorderRecording);
    }

    private void resetScreenOn() {
        this.mHandler.removeMessages(10);
        this.mHandler.removeMessages(2);
        getWindow().clearFlags(128);
    }

    private void keepScreenOnAwhile() {
        this.mHandler.sendEmptyMessageDelayed(10, 1000);
    }

    private void keepScreenOn() {
        this.mHandler.removeMessages(10);
        this.mHandler.removeMessages(2);
        getWindow().addFlags(128);
    }

    private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (60 * hours);
        long remainderSeconds = seconds - (60 * minutes);
        StringBuilder timeStringBuilder = new StringBuilder();
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append(String.format("%d", new Object[]{Integer.valueOf(0)}));
            }
            timeStringBuilder.append(String.format("%d", new Object[]{Long.valueOf(hours)}));
            timeStringBuilder.append(':');
        }
        if (remainderMinutes < 10) {
            timeStringBuilder.append(String.format("%d", new Object[]{Integer.valueOf(0)}));
        }
        timeStringBuilder.append(String.format("%d", new Object[]{Long.valueOf(remainderMinutes)}));
        timeStringBuilder.append(':');
        if (remainderSeconds < 10) {
            timeStringBuilder.append(String.format("%d", new Object[]{Integer.valueOf(0)}));
        }
        timeStringBuilder.append(String.format("%d", new Object[]{Long.valueOf(remainderSeconds)}));
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - (1000 * seconds)) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }
        return timeStringBuilder.toString();
    }

    private long getSpeedRecordVideoLength(long deltaMs, double timeBetweenFrameMs) {
        if (timeBetweenFrameMs == 0.0d) {
            return 0;
        }
        return (long) (((((double) deltaMs) / timeBetweenFrameMs) / ((double) getNormalVideoFrameRate())) * 1000.0d);
    }

    private void updateRecordingTime() {
        if (this.mMediaRecorderRecording) {
            String text;
            long delta = SystemClock.uptimeMillis() - this.mRecordingStartTime;
            if (this.mMediaRecorderRecordingPaused) {
                delta = this.mVideoRecordedDuration;
            }
            boolean countdownRemainingTime = this.mMaxVideoDurationInMs != 0 ? delta >= ((long) (this.mMaxVideoDurationInMs - 60000)) : false;
            long deltaAdjusted = delta;
            if (countdownRemainingTime) {
                deltaAdjusted = Math.max(0, ((long) this.mMaxVideoDurationInMs) - deltaAdjusted) + 999;
            }
            long targetNextUpdateDelay = 1000;
            if ("normal".equals(this.mHfr)) {
                text = millisecondToTimeString(deltaAdjusted, false);
            } else {
                double timeBetweenFrameMs = 0.0d;
                if ("fast".equals(this.mHfr)) {
                    timeBetweenFrameMs = (double) this.mTimeBetweenTimeLapseFrameCaptureMs;
                    targetNextUpdateDelay = (long) timeBetweenFrameMs;
                } else {
                    if ("slow".equals(this.mHfr) && isShowHFRDuration()) {
                        String hfr = sProxy.getVideoHighFrameRate(this.mParameters);
                        if (!(hfr == null || hfr.equals("off"))) {
                            timeBetweenFrameMs = 1000.0d / Double.parseDouble(hfr);
                            targetNextUpdateDelay = (long) ((getNormalVideoFrameRate() * 1000) / Integer.parseInt(hfr));
                        }
                    }
                }
                if (timeBetweenFrameMs != 0.0d) {
                    text = millisecondToTimeString(getSpeedRecordVideoLength(delta, timeBetweenFrameMs), "fast".equals(this.mHfr));
                    if (text.equals(this.mRecordingTime)) {
                        targetNextUpdateDelay = (long) timeBetweenFrameMs;
                    }
                } else {
                    text = millisecondToTimeString(deltaAdjusted, false);
                }
            }
            getUIController().getVideoRecordingTimeView().setText(text);
            this.mRecordingTime = text;
            if (this.mRecordingTimeCountsDown != countdownRemainingTime) {
                this.mRecordingTimeCountsDown = countdownRemainingTime;
            }
            if (this.mCurrentShowIndicator != -1) {
                this.mCurrentShowIndicator = 1 - this.mCurrentShowIndicator;
                if (this.mMediaRecorderRecordingPaused && 1 == this.mCurrentShowIndicator) {
                    getUIController().getVideoRecordingTimeView().setVisibility(4);
                } else {
                    getUIController().getVideoRecordingTimeView().setVisibility(0);
                    if (!this.mMediaRecorderRecordingPaused) {
                        this.mCurrentShowIndicator = -1;
                    }
                }
            }
            long actualNextUpdateDelay = 500;
            if (!this.mMediaRecorderRecordingPaused) {
                actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
            }
            this.mHandler.sendEmptyMessageDelayed(3, actualNextUpdateDelay);
        }
    }

    protected boolean isShowHFRDuration() {
        return true;
    }

    protected void updateVideoParametersPreference() {
        String antiBanding;
        Log.e("videocamera", "Preview dimension in App->" + this.mDesiredPreviewWidth + "X" + this.mDesiredPreviewHeight);
        this.mParameters.setPreviewSize(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
        this.mVideoWidth = this.mProfile.videoFrameWidth;
        this.mVideoHeight = this.mProfile.videoFrameHeight;
        String recordSize = this.mVideoWidth + "x" + this.mVideoHeight;
        Log.e("videocamera", "Video dimension in App->" + recordSize);
        this.mParameters.set("video-size", recordSize);
        List<String> supportedFlash = this.mParameters.getSupportedFlashModes();
        String flashMode = this.mPreferences.getString("pref_camera_video_flashmode_key", getString(R.string.pref_camera_video_flashmode_default));
        if (BaseModule.isSupported(flashMode, supportedFlash)) {
            this.mParameters.setFlashMode(flashMode);
        }
        if (isBackCamera()) {
            List<String> supportedFocusMode = sProxy.getSupportedFocusModes(this.mParameters);
            String focusMode = this.mPreferences.getString("pref_video_focusmode_key", getString(R.string.pref_video_focusmode_entryvalue_default));
            if (BaseModule.isSupported(focusMode, supportedFocusMode)) {
                if ("continuous-video".equals(focusMode)) {
                    this.mVideoFocusMode = "continuous-video";
                    getUIController().getFocusView().setFocusType(false);
                    this.mFocusManager.resetFocused();
                } else {
                    this.mVideoFocusMode = "auto";
                    getUIController().getFocusView().setFocusType(true);
                }
                sProxy.setFocusMode(this.mParameters, this.mVideoFocusMode);
                updateMotionFocusManager();
                updateAutoFocusMoveCallback();
            }
        }
        String colorEffect = this.mPreferences.getString("pref_camera_coloreffect_key", getString(R.string.pref_camera_coloreffect_default));
        Log.e("videocamera", "Color effect value =" + colorEffect);
        if (BaseModule.isSupported(colorEffect, this.mParameters.getSupportedColorEffects())) {
            this.mParameters.setColorEffect(colorEffect);
        }
        String whiteBalance = "auto";
        if (BaseModule.isSupported(whiteBalance, this.mParameters.getSupportedWhiteBalance())) {
            this.mParameters.setWhiteBalance(whiteBalance);
        } else if (this.mParameters.getWhiteBalance() == null) {
            whiteBalance = "auto";
        }
        if (this.mParameters.isZoomSupported()) {
            this.mParameters.setZoom(getZoomValue());
        }
        this.mParameters.setRecordingHint(true);
        if (!this.mParameters.isVideoStabilizationSupported() || ((Device.IS_X9 && (!"normal".equals(this.mHfr) || this.mQuality >= 6)) || CameraSettings.is4KHigherVideoQuality(this.mQuality) || !isBackCamera() || !CameraSettings.isMovieSolidOn(this.mPreferences))) {
            Log.v("videocamera", "set video stabilization to false");
            this.mParameters.setVideoStabilization(false);
            this.mActivity.getCameraScreenNail().setVideoStabilizationCropped(false);
        } else {
            Log.v("videocamera", "set video stabilization to true");
            this.mParameters.setVideoStabilization(true);
            this.mActivity.getCameraScreenNail().setVideoStabilizationCropped(true);
        }
        int maxWidth = Integer.MAX_VALUE;
        int maxHeight = Integer.MAX_VALUE;
        if (Device.isVideoSnapshotSizeLimited()) {
            maxWidth = this.mProfile.videoFrameWidth;
            maxHeight = this.mProfile.videoFrameHeight;
        }
        Size optimalSize = Util.getOptimalVideoSnapshotPictureSize(this.mParameters.getSupportedPictureSizes(), ((double) this.mDesiredPreviewWidth) / ((double) this.mDesiredPreviewHeight), maxWidth, maxHeight);
        Size original = this.mParameters.getPictureSize();
        if (original == null) {
            Log.v("videocamera", "get null pictureSize");
        } else if (!original.equals(optimalSize)) {
            this.mParameters.setPictureSize(optimalSize.width, optimalSize.height);
        }
        Log.v("videocamera", "Video snapshot size is " + optimalSize.width + "x" + optimalSize.height);
        if (Device.isQcomPlatform()) {
            if (21 <= VERSION.SDK_INT) {
                Size size = this.mParameters.getPictureSize();
                optimalSize = Util.getOptimalJpegThumbnailSize(this.mParameters.getSupportedJpegThumbnailSizes(), ((double) size.width) / ((double) size.height));
                if (!this.mParameters.getJpegThumbnailSize().equals(optimalSize)) {
                    this.mParameters.setJpegThumbnailSize(optimalSize.width, optimalSize.height);
                }
                Log.v("videocamera", "Thumbnail size is " + optimalSize.width + "x" + optimalSize.height);
            } else {
                this.mParameters.setJpegThumbnailSize(0, 0);
            }
        }
        this.mParameters.setJpegQuality(CameraProfile.getJpegEncodingQualityParameter(this.mCameraId, 2));
        addMuteToParameters(this.mParameters);
        configOisParameters(this.mParameters, true);
        addT2TParameters(this.mParameters);
        resetFaceBeautyParams(this.mParameters);
        sProxy.clearExposureTime(this.mParameters);
        if (Device.isSupportedHFR() && "slow".equals(this.mHfr)) {
            antiBanding = "off";
        } else {
            antiBanding = this.mPreferences.getString("pref_camera_antibanding_key", getString(CameraSettings.getDefaultPreferenceId(R.string.pref_camera_antibanding_default)));
        }
        Log.v("videocamera", "antiBanding value =" + antiBanding);
        if (BaseModule.isSupported(antiBanding, this.mParameters.getSupportedAntibanding())) {
            this.mParameters.setAntibanding(antiBanding);
        }
        int style = CameraSettings.getUIStyleByPreview(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
        if (this.mUIStyle != style) {
            this.mUIStyle = style;
            if (this.mSwitchingCamera) {
                this.mHasPendingSwitching = true;
            } else {
                this.mHandler.sendEmptyMessage(11);
            }
        }
        if (this.mParameters.get("xiaomi-time-watermark") != null) {
            this.mParameters.set("xiaomi-time-watermark", "off");
        }
        if (this.mParameters.get("xiaomi-dualcam-watermark") != null) {
            this.mParameters.set("xiaomi-dualcam-watermark", "off");
        }
        if (this.mParameters.get("watermark") != null) {
            this.mParameters.set("watermark", "off");
        }
    }

    private void updateMotionFocusManager() {
        this.mActivity.getSensorStateManager().setFocusSensorEnabled("auto".equals(this.mVideoFocusMode));
    }

    protected void setCameraParameters() {
        updateVideoParametersPreference();
        this.mCameraDevice.setParameters(this.mParameters);
        this.mParameters = this.mCameraDevice.getParameters();
        if (!this.mSwitchingCamera || CameraSettings.sCroppedIfNeeded) {
            updateCameraScreenNailSize(this.mDesiredPreviewWidth, this.mDesiredPreviewHeight, this.mFocusManager);
        }
    }

    private void initializeFocusManager() {
        this.mFocusManager = new FocusManagerSimple(getUIController().getPreviewFrame().getWidth(), getUIController().getPreviewFrame().getHeight(), this.mCameraId == CameraHolder.instance().getFrontCameraId(), Util.getDisplayOrientation(this.mCameraDisplayOrientation, this.mCameraId));
        Display display = this.mActivity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        this.mFocusManager.setPreviewSize(point.x, point.y);
        this.mFocusManager.setRenderSize(point.x, point.y);
    }

    private void initializeCapabilities() {
        boolean isSupported;
        boolean z = false;
        if (this.mParameters.getMaxNumFocusAreas() > 0) {
            isSupported = BaseModule.isSupported("auto", sProxy.getSupportedFocusModes(this.mParameters));
        } else {
            isSupported = false;
        }
        this.mFocusAreaSupported = isSupported;
        if (this.mParameters.getMaxNumMeteringAreas() > 0) {
            z = true;
        }
        this.mMeteringAreaSupported = z;
        this.mContinuousFocusSupported = BaseModule.isSupported("continuous-video", sProxy.getSupportedFocusModes(this.mParameters));
    }

    private boolean switchToOtherMode(int mode) {
        if (this.mActivity.isFinishing()) {
            return false;
        }
        this.mActivity.switchToOtherModule(mode);
        return true;
    }

    private void initializeMiscControls() {
    }

    protected void updateLoadUI(boolean show) {
        boolean z;
        if (show) {
            this.mHandler.sendEmptyMessageDelayed(13, 500);
        } else {
            this.mHandler.removeMessages(13);
            getUIController().getCaptureProgressBar().setVisibility(8);
        }
        V6ShutterButton shutterButton = getUIController().getShutterButton();
        if (show) {
            z = false;
        } else {
            z = true;
        }
        shutterButton.enableControls(z);
    }

    private boolean capture() {
        if (this.mPaused || this.mSnapshotInProgress || !this.mMediaRecorderRecording) {
            return false;
        }
        if (Storage.isLowStorageAtLastPoint()) {
            onStopVideoRecording(false);
            return false;
        } else if (this.mActivity.getImageSaver().shouldStopShot()) {
            Log.i("videocamera", "ImageSaver is full, wait for a moment!");
            RotateTextToast.getInstance(this.mActivity).show(R.string.toast_saving, 0);
            return false;
        } else {
            Util.setRotationParameter(this.mParameters, this.mCameraId, this.mOrientation);
            Location loc = LocationManager.instance().getCurrentLocation();
            Util.setGpsParameters(this.mParameters, loc);
            this.mCameraDevice.setParameters(this.mParameters);
            if (Device.isMDPRender()) {
                getUIController().getPreviewPanel().onCapture();
            } else {
                this.mActivity.getCameraScreenNail().animateCapture(getCameraRotation());
            }
            Log.v("videocamera", "Video snapshot start");
            this.mCameraDevice.takePicture(null, null, null, new JpegPictureCallback(loc));
            getUIController().getShutterButton().enableControls(false);
            this.mSnapshotInProgress = true;
            return true;
        }
    }

    private void restorePreferences() {
        if (this.mParameters.isZoomSupported()) {
            setZoomValue(0);
        }
        getUIController().getFlashButton().reloadPreference();
        getUIController().getSettingPage().reloadPreferences();
        onSharedPreferenceChanged();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onSharedPreferenceChanged() {
        /*
        r4 = this;
        r1 = r4.mPaused;
        if (r1 == 0) goto L_0x0005;
    L_0x0004:
        return;
    L_0x0005:
        r2 = r4.mPreferences;
        monitor-enter(r2);
        r1 = r4.mCameraDevice;	 Catch:{ all -> 0x0041 }
        if (r1 != 0) goto L_0x000e;
    L_0x000c:
        monitor-exit(r2);
        return;
    L_0x000e:
        r4.readVideoPreferences();	 Catch:{ all -> 0x0041 }
        r1 = r4.mParameters;	 Catch:{ all -> 0x0041 }
        r0 = r1.getPreviewSize();	 Catch:{ all -> 0x0041 }
        r1 = r0.width;	 Catch:{ all -> 0x0041 }
        r3 = r4.mDesiredPreviewWidth;	 Catch:{ all -> 0x0041 }
        if (r1 != r3) goto L_0x0023;
    L_0x001d:
        r1 = r0.height;	 Catch:{ all -> 0x0041 }
        r3 = r4.mDesiredPreviewHeight;	 Catch:{ all -> 0x0041 }
        if (r1 == r3) goto L_0x0039;
    L_0x0023:
        r4.stopPreview();	 Catch:{ all -> 0x0041 }
        r4.resizeForPreviewAspectRatio();	 Catch:{ all -> 0x0041 }
        r4.startPreview();	 Catch:{ all -> 0x0041 }
    L_0x002c:
        monitor-exit(r2);
        r1 = r4.getUIController();
        r1 = r1.getSettingsStatusBar();
        r1.updateStatus();
        return;
    L_0x0039:
        r1 = r4.mRestartPreview;	 Catch:{ all -> 0x0041 }
        if (r1 != 0) goto L_0x0023;
    L_0x003d:
        r4.setCameraParameters();	 Catch:{ all -> 0x0041 }
        goto L_0x002c;
    L_0x0041:
        r1 = move-exception;
        monitor-exit(r2);
        throw r1;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.VideoModule.onSharedPreferenceChanged():void");
    }

    public boolean onCameraPickerClicked(int cameraId) {
        if (this.mPaused || this.mPendingSwitchCameraId != -1 || this.mSwitchingCamera) {
            return false;
        }
        Log.d("videocamera", "Start to copy texture.");
        if (Device.isMDPRender()) {
            this.mActivity.setBlurFlag(true);
            this.mHandler.sendEmptyMessage(5);
        } else {
            this.mActivity.getCameraScreenNail().animateSwitchCopyTexture();
        }
        this.mPendingSwitchCameraId = cameraId;
        enableCameraControls(false);
        this.mSwitchingCamera = true;
        return true;
    }

    private void switchCamera() {
        if (!this.mPaused) {
            if (!this.mMutexModePicker.isNormal()) {
                this.mMutexModePicker.resetMutexMode();
            }
            updateStereoSettings(false);
            Log.d("videocamera", "Start to switch camera.");
            this.mCameraId = this.mPendingSwitchCameraId;
            this.mPendingSwitchCameraId = -1;
            CameraSettings.writePreferredCameraId(this.mPreferences, this.mCameraId);
            this.mActivity.changeRequestOrientation();
            CameraSettings.resetZoom(this.mPreferences);
            CameraSettings.resetExposure();
            CameraSettingPreferences.instance().setLocalId(getPreferencesLocalId());
            PopupManager.getInstance(this.mActivity).notifyShowPopup(null, 1);
            getUIController().getModeExitView().updateExitButton(-1, false);
            closeCamera();
            getUIController().updatePreferenceGroup();
            CameraOpenThread cameraOpenThread = new CameraOpenThread();
            cameraOpenThread.start();
            try {
                cameraOpenThread.join();
            } catch (InterruptedException e) {
            }
            if (hasCameraException()) {
                onCameraException();
                return;
            }
            onCameraOpen();
            initializeCapabilities();
            updateStereoSettings(true);
            readVideoPreferences();
            getUIController().getFlashButton().avoidTorchOpen();
            startPreview();
            getUIController().onCameraOpen();
            getUIController().getFocusView().initialize(this);
            initializeZoom();
            initializeExposureCompensation();
            setOrientationIndicator(this.mOrientationCompensation, false);
            updateMutexModePreference();
            showFirstUseHintIfNeeded();
            this.mHandler.sendEmptyMessage(12);
        }
    }

    private void updateMutexModePreference() {
        if ("on".equals(getUIController().getHdrButton().getValue())) {
            this.mMutexModePicker.setMutexMode(2);
        }
    }

    private void showFirstUseHintIfNeeded() {
        if (this.mPreferences.getBoolean("pref_camera_first_use_hint_shown_key", true) || this.mPreferences.getBoolean("pref_camera_first_portrait_use_hint_shown_key", true)) {
            this.mHandler.sendEmptyMessageDelayed(19, 1000);
        }
    }

    public void onPreviewTextureCopied() {
        animateSwitchCamera();
        this.mHandler.sendEmptyMessage(5);
    }

    protected void animateSwitchCamera() {
        if (Device.isMDPRender()) {
            this.mHandler.sendEmptyMessageDelayed(14, 100);
            enableCameraControls(true);
            CameraSettings.changeUIByPreviewSize(this.mActivity, this.mUIStyle, this.mDesiredPreviewWidth, this.mDesiredPreviewHeight);
            this.mSwitchingCamera = false;
            return;
        }
        this.mHandler.sendEmptyMessage(6);
    }

    public void onSwitchAnimationDone() {
        this.mHandler.sendEmptyMessage(16);
    }

    protected void switchMutexHDR() {
        if ("off".equals(getUIController().getHdrButton().getValue())) {
            this.mMutexModePicker.resetMutexMode();
        } else {
            this.mMutexModePicker.setMutexMode(2);
        }
    }

    protected void openSettingActivity() {
        Intent intent = new Intent();
        intent.setClass(this.mActivity, CameraPreferenceActivity.class);
        intent.putExtra("from_where", 2);
        intent.putExtra(":miui:starting_window_label", getResources().getString(R.string.pref_camera_settings_category));
        if (this.mActivity.startFromKeyguard()) {
            intent.putExtra("StartActivityWhenLocked", true);
        }
        this.mActivity.startActivity(intent);
        this.mActivity.setJumpFlag(2);
        CameraDataAnalytics.instance().trackEvent("pref_settings");
    }

    protected void enterMutexMode() {
        setOrientationIndicator(this.mOrientationCompensation, false);
        setZoomValue(0);
        this.mSettingsOverrider.overrideSettings("pref_camera_whitebalance_key", null, "pref_camera_coloreffect_key", null);
        onSharedPreferenceChanged();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    protected void exitMutexMode() {
        this.mSettingsOverrider.restoreSettings();
        onSharedPreferenceChanged();
        getUIController().getSettingsStatusBar().updateStatus();
    }

    public void onSingleTapUp(int x, int y) {
        if (!this.mPaused && !this.mSnapshotInProgress && !isFrontCamera() && isInTapableRect(x, y) && !this.mActivity.getCameraScreenNail().isModuleSwitching()) {
            if (!isVideoRecording()) {
                getUIController().getPreviewPage().simplifyPopup(true, true);
            }
            if (this.mObjectTrackingStarted) {
                stopObjectTracking(false);
            }
            this.mHandler.removeMessages(24);
            getUIController().getFocusView().setFocusType(true);
            this.mIsTouchFocused = true;
            this.mTouchFocusStartingTime = System.currentTimeMillis();
            Point point = new Point(x, y);
            mapTapCoordinate(point);
            autoFocus(point.x, point.y, this.mFocusManager.getDefaultFocusAreaWidth(), this.mFocusManager.getDefaultFocusAreaHeight(), 3);
        }
    }

    private void cancelAutoFocus() {
        String focusMode;
        this.mCameraDevice.cancelAutoFocus();
        this.mFocusManager.cancelAutoFocus();
        this.mParameters = this.mCameraDevice.getParameters();
        List<String> supportedFocusMode = sProxy.getSupportedFocusModes(this.mParameters);
        if (Device.isMTKPlatform()) {
            focusMode = "auto";
        } else {
            focusMode = "macro";
        }
        if (BaseModule.isSupported(focusMode, supportedFocusMode)) {
            sProxy.setFocusMode(this.mParameters, focusMode);
            updateAutoFocusMoveCallback();
        }
        if (this.mFocusAreaSupported) {
            sProxy.setFocusAreas(this.mParameters, null);
        }
        if (this.mMeteringAreaSupported) {
            sProxy.setMeteringAreas(this.mParameters, null);
        }
        this.mCameraDevice.setParameters(this.mParameters);
    }

    private void autoFocus(int x, int y, int focusWidth, int focusHeight, int focusType) {
        Log.v("videocamera", "autoFocus mVideoFocusMode=" + this.mVideoFocusMode);
        if (!"auto".equals(this.mVideoFocusMode) && !this.mObjectTrackingStarted) {
            return;
        }
        if (this.mFocusAreaSupported || this.mMeteringAreaSupported) {
            if (this.mFocusManager.isNeedCancelAutoFocus()) {
                cancelAutoFocus();
            }
            this.mParameters = this.mCameraDevice.getParameters();
            this.mFocusManager.focusPoint();
            if (this.mFocusAreaSupported) {
                sProxy.setFocusAreas(this.mParameters, this.mFocusManager.getFocusArea(x, y, focusWidth, focusHeight));
            }
            if (this.mMeteringAreaSupported && focusType != 4) {
                sProxy.setMeteringAreas(this.mParameters, this.mFocusManager.getMeteringsArea(x, y, focusWidth, focusHeight));
            }
            this.mCameraDevice.setParameters(this.mParameters);
            this.mFocusStartTime = System.currentTimeMillis();
            if (!this.mObjectTrackingStarted) {
                getUIController().getFocusView().setPosition(x, y);
            }
            if (focusType == 3) {
                getUIController().getFocusView().showStart();
            }
            this.mCameraDevice.autoFocus(this);
        }
    }

    public void onAutoFocus(boolean success, Camera camera) {
        if (!this.mPaused && !this.mActivity.getCameraScreenNail().isModuleSwitching()) {
            Log.v("videocamera", "mAutoFocusTime = " + (System.currentTimeMillis() - this.mFocusStartTime) + "ms focused=" + success + " waitforrecording=" + this.mFocusManager.isFocusingSnapOnFinish());
            if (this.mFocusManager.isFocusingSnapOnFinish()) {
                this.mInStartingFocusRecording = false;
                record();
            }
            if (!this.mObjectTrackingStarted) {
                if (success) {
                    getUIController().getFocusView().showSuccess();
                    if (!isNeedMute() && this.mIsTouchFocused) {
                        playCameraSound(1);
                    }
                } else {
                    getUIController().getFocusView().showFail();
                }
            }
            this.mFocusManager.onAutoFocus(success);
            this.mActivity.getSensorStateManager().reset();
        }
    }

    private void storeImage(byte[] data, Location loc) {
        boolean z;
        long dateTaken = System.currentTimeMillis();
        int orientation = Exif.getOrientation(data);
        Size s = this.mParameters.getPictureSize();
        this.mActivity.getImageSaver().addImage(10, data, Util.createJpegName(dateTaken), System.currentTimeMillis(), null, loc, s.width, s.height, null, orientation, false, false, true);
        int i = s.width;
        int i2 = s.height;
        if (loc != null) {
            z = true;
        } else {
            z = false;
        }
        trackPictureTaken(1, false, i, i2, z);
    }

    public static String convertOutputFormatToMimeType(int outputFileFormat) {
        if (outputFileFormat == 2) {
            return "video/mp4";
        }
        return "video/3gpp";
    }

    public static String convertOutputFormatToFileExt(int outputFileFormat) {
        if (outputFileFormat == 2) {
            return ".mp4";
        }
        return ".3gp";
    }

    private void onFrameLayoutChange(View v, Rect rect) {
        this.mActivity.onLayoutChange(rect.width(), rect.height());
        if (!(this.mFocusManager == null || this.mActivity.getCameraScreenNail() == null)) {
            this.mActivity.getCameraScreenNail().setRenderArea(rect);
            this.mFocusManager.setRenderSize(this.mActivity.getCameraScreenNail().getRenderWidth(), this.mActivity.getCameraScreenNail().getRenderHeight());
            this.mFocusManager.setPreviewSize(rect.width(), rect.height());
        }
        if (getUIController().getObjectView() != null) {
            getUIController().getObjectView().setDisplaySize(rect.right - rect.left, rect.bottom - rect.top);
        }
    }

    private void closeVideoFileDescriptor() {
        if (this.mVideoFileDescriptor != null) {
            try {
                this.mVideoFileDescriptor.close();
            } catch (IOException e) {
                Log.e("videocamera", "Fail to close fd", e);
            }
            this.mVideoFileDescriptor = null;
        }
    }

    private void updateAutoFocusMoveCallback() {
        if (!this.mContinuousFocusSupported) {
            return;
        }
        if (this.mParameters.getFocusMode().equals("continuous-video")) {
            this.mCameraDevice.setAutoFocusMoveCallback(this);
        } else {
            this.mCameraDevice.setAutoFocusMoveCallback(null);
        }
    }

    public boolean isNeedMute() {
        if (super.isNeedMute() || this.mObjectTrackingStarted) {
            return true;
        }
        return this.mMediaRecorderRecording && !this.mMediaRecorderRecordingPaused;
    }

    public boolean isMeteringAreaOnly() {
        return !this.mFocusAreaSupported ? this.mMeteringAreaSupported : false;
    }

    public void onAutoFocusMoving(boolean moving, Camera camera) {
        Log.v("videocamera", "onAutoFocusMoving moving= " + moving);
        if (!this.mPaused && !this.mMediaRecorderRecording && !this.mActivity.getCameraScreenNail().isModuleSwitching()) {
            getUIController().getFocusView().setFocusType(false);
            if (moving) {
                getUIController().getFocusView().showStart();
            } else if (this.mCameraDevice.isFocusSuccessful()) {
                getUIController().getFocusView().showSuccess();
            } else {
                getUIController().getFocusView().showFail();
            }
        }
    }

    public boolean onGestureTrack(RectF rectF, boolean up) {
        if (this.mInStartingFocusRecording || isVideoProcessing() || this.mSwitchingCamera || !isBackCamera() || !Device.isSupportedObjectTrack() || CameraSettings.is4KHigherVideoQuality(this.mQuality) || this.mIsVideoCaptureIntent) {
            return false;
        }
        return initializeObjectTrack(rectF, up);
    }

    public void onFaceDetection(Face[] faces, Camera camera) {
        CameraHardwareFace[] cameraFaces = CameraHardwareFace.convertCameraHardwareFace(faces);
        if (Device.isSupportedObjectTrack() && cameraFaces.length > 0 && cameraFaces[0].faceType == 64206 && this.mObjectTrackingStarted) {
            getUIController().getObjectView().setObject(cameraFaces[0]);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void startObjectTracking() {
        /*
        r3 = this;
        r0 = "videocamera";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "startObjectTracking mObjectTrackingStarted=";
        r1 = r1.append(r2);
        r2 = r3.mObjectTrackingStarted;
        r1 = r1.append(r2);
        r1 = r1.toString();
        android.util.Log.i(r0, r1);
        r0 = r3.mObjectTrackingStarted;
        if (r0 != 0) goto L_0x0024;
    L_0x0020:
        r0 = r3.mPaused;
        if (r0 == 0) goto L_0x0025;
    L_0x0024:
        return;
    L_0x0025:
        r0 = r3.mCameraDevice;
        if (r0 == 0) goto L_0x00a5;
    L_0x0029:
        r0 = com.android.camera.Device.isSupportedObjectTrack();
        if (r0 == 0) goto L_0x00a5;
    L_0x002f:
        r0 = 1;
        r3.mObjectTrackingStarted = r0;
        r0 = "continuous-video";
        r1 = r3.mParameters;
        r1 = r1.getFocusMode();
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0066;
    L_0x0041:
        r0 = "auto";
        r1 = sProxy;
        r2 = r3.mParameters;
        r1 = r1.getSupportedFocusModes(r2);
        r0 = com.android.camera.module.BaseModule.isSupported(r0, r1);
        if (r0 == 0) goto L_0x0066;
    L_0x0052:
        r0 = sProxy;
        r1 = r3.mParameters;
        r2 = "auto";
        r0.setFocusMode(r1, r2);
        r3.updateMotionFocusManager();
        r0 = r3.mCameraDevice;
        r1 = r3.mParameters;
        r0.setParameters(r1);
    L_0x0066:
        r3.updateAutoFocusMoveCallback();
        r0 = r3.mCameraDevice;
        r0.setFaceDetectionListener(r3);
        r0 = "videocamera";
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "startObjectTracking rect=";
        r1 = r1.append(r2);
        r2 = r3.getUIController();
        r2 = r2.getObjectView();
        r2 = r2.getFocusRectInPreviewFrame();
        r1 = r1.append(r2);
        r1 = r1.toString();
        android.util.Log.i(r0, r1);
        r0 = r3.mCameraDevice;
        r1 = r3.getUIController();
        r1 = r1.getObjectView();
        r1 = r1.getFocusRectInPreviewFrame();
        r0.startObjectTrack(r1);
    L_0x00a5:
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.module.VideoModule.startObjectTracking():void");
    }

    public void onObjectStable() {
        RectF rect = getUIController().getObjectView().getFocusRect();
        if (rect != null && this.mFocusManager.canAutoFocus()) {
            autoFocus((int) rect.centerX(), (int) rect.centerY(), (int) rect.width(), (int) rect.height(), 2);
        }
    }

    private boolean initializeObjectTrack(RectF rectF, boolean up) {
        mapTapCoordinate(rectF);
        stopObjectTracking(false);
        getUIController().getObjectView().clear();
        getUIController().getFocusView().clear();
        getUIController().getObjectView().setVisibility(0);
        return getUIController().getObjectView().initializeTrackView(rectF, up);
    }

    public void stopObjectTracking(boolean restartFD) {
        Log.i("videocamera", "stopObjectTracking mObjectTrackingStarted=" + this.mObjectTrackingStarted);
        if (this.mObjectTrackingStarted) {
            if (this.mCameraDevice != null) {
                this.mObjectTrackingStarted = false;
                this.mCameraDevice.setFaceDetectionListener(null);
                this.mCameraDevice.stopObjectTrack();
                if (!this.mInStartingFocusRecording && this.mFocusManager.isNeedCancelAutoFocus()) {
                    this.mCameraDevice.cancelAutoFocus();
                    this.mFocusManager.cancelAutoFocus();
                }
                if (!(this.mPaused || getUIController().getObjectView().isAdjusting())) {
                    setCameraParameters();
                }
                getUIController().getObjectView().clear();
                getUIController().getObjectView().setVisibility(8);
            }
            return;
        }
        if (!(!this.mPaused || getUIController().getObjectView() == null || getUIController().getObjectView().getVisibility() == 8)) {
            getUIController().getObjectView().clear();
            getUIController().getObjectView().setVisibility(8);
        }
    }

    public void onPauseButtonClick() {
        Log.i("videocamera", "mVideoPauseResumeListener.onClick() mMediaRecorderRecordingPaused=" + this.mMediaRecorderRecordingPaused + ",mRecorderBusy = " + this.mRecorderBusy + ",mMediaRecorderRecording = " + this.mMediaRecorderRecording);
        long now = System.currentTimeMillis();
        if (!this.mRecorderBusy && this.mMediaRecorderRecording && now - this.mPauseClickTime >= 500) {
            this.mPauseClickTime = now;
            this.mRecorderBusy = true;
            if (this.mMediaRecorderRecordingPaused) {
                getUIController().getPauseRecordingButton().setImageResource(R.drawable.ic_recording_pause);
                try {
                    resumeMediaRecorder(this.mMediaRecorder);
                    this.mRecordingStartTime = SystemClock.uptimeMillis() - this.mVideoRecordedDuration;
                    this.mVideoRecordedDuration = 0;
                    this.mMediaRecorderRecordingPaused = false;
                    this.mHandler.removeMessages(3);
                    this.mRecordingTime = "";
                    updateRecordingTime();
                } catch (IllegalStateException e) {
                    Log.e("videocamera", "Could not start media recorder. ", e);
                    releaseMediaRecorder();
                }
            } else {
                pauseVideoRecording();
                CameraDataAnalytics.instance().trackEvent("video_pause_recording_times_key");
            }
            this.mRecorderBusy = false;
            Log.i("videocamera", "mVideoPauseResumeListener.onClick() end. mRecorderBusy=" + this.mRecorderBusy);
        }
    }

    private void pauseVideoRecording() {
        Log.d("videocamera", "pauseVideoRecording() mRecorderBusy=" + this.mRecorderBusy);
        getUIController().getPauseRecordingButton().setImageResource(R.drawable.ic_recording_resume);
        if (this.mMediaRecorderRecording && !this.mMediaRecorderRecordingPaused) {
            try {
                pauseMediaRecorder(this.mMediaRecorder);
            } catch (IllegalStateException e) {
                Log.e("videocamera", "Could not pause media recorder. ");
            }
            this.mVideoRecordedDuration = SystemClock.uptimeMillis() - this.mRecordingStartTime;
            this.mMediaRecorderRecordingPaused = true;
            this.mHandler.removeMessages(3);
            this.mCurrentShowIndicator = 0;
            updateRecordingTime();
        }
    }

    protected void pauseMediaRecorder(MediaRecorder mediaRecorder) {
        try {
            Method.of(MediaRecorder.class, "pause", "()V").invoke(MediaRecorder.class, this.mMediaRecorder, new Object[0]);
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e2) {
            Log.e("videocamera", "pauseMediaRecorder IllegalArgumentException");
        }
    }

    protected void resumeMediaRecorder(MediaRecorder mediaRecorder) {
        if (VERSION.SDK_INT < 24) {
            this.mMediaRecorder.start();
            return;
        }
        try {
            Method.of(MediaRecorder.class, "resume", "()V").invoke(MediaRecorder.class, this.mMediaRecorder, new Object[0]);
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e2) {
            Log.e("videocamera", "resumeMediaRecorder IllegalArgumentException");
        }
    }

    protected void sendOpenFailMessage() {
        this.mHandler.sendEmptyMessage(18);
    }

    public boolean isCameraEnabled() {
        return this.mPreviewing;
    }

    private boolean checkCallingState() {
        if (2 != this.mTelephonyManager.getCallState()) {
            return true;
        }
        this.mActivity.getScreenHint().showConfirmMessage(R.string.confirm_recording_fail_title, R.string.confirm_recording_fail_calling_alert);
        return false;
    }

    private void releaseResources() {
        closeCamera();
        releaseMediaRecorder();
        CameraDataAnalytics.instance().uploadToServer();
        this.mWaitForRelease = false;
    }

    protected boolean isProfileExist(int cameraId, Integer quality) {
        return CamcorderProfile.hasProfile(cameraId, quality.intValue());
    }

    protected void configMediaRecorder(MediaRecorder mediaRecorder) {
    }

    private void trackRecordingInfo() {
        CameraDataAnalytics.instance().trackEvent("video_recorded_key");
        if ("fast".equals(this.mHfr)) {
            CameraDataAnalytics.instance().trackEvent("video_fast_recording_times_key");
        } else if ("slow".equals(this.mHfr)) {
            CameraDataAnalytics.instance().trackEvent("video_slow_recording_times_key");
        } else if (CameraSettings.isSwitchOn("pref_video_hdr_key")) {
            CameraDataAnalytics.instance().trackEvent("video_hdr_recording_times_key");
        }
        if (this.mQuality == 5) {
            CameraDataAnalytics.instance().trackEvent("video_quality_720_recording_times_key");
        } else if (this.mQuality == 4) {
            CameraDataAnalytics.instance().trackEvent("video_quality_480_recording_times_key");
        } else if (this.mQuality == 6) {
            CameraDataAnalytics.instance().trackEvent("video_quality_1080_recording_times_key");
        } else {
            CameraDataAnalytics.instance().trackEvent("video_quality_4k_recording_times_key");
        }
        if ("torch".equals(this.mParameters.getFlashMode())) {
            CameraDataAnalytics.instance().trackEvent("video_torch_recording_times_key");
        }
        if (isFrontCamera()) {
            CameraDataAnalytics.instance().trackEvent("video_front_camera_recording_times_key");
        }
        if (this.mCurrentVideoValues == null) {
            return;
        }
        if (this.mCurrentVideoValues.get("latitude") != null || this.mCurrentVideoValues.get("longitude") != null) {
            CameraDataAnalytics.instance().trackEvent("video_with_location_key");
        } else if (CameraSettings.isRecordLocation(this.mPreferences)) {
            CameraDataAnalytics.instance().trackEvent("video_without_location_key");
        }
    }

    public void notifyError() {
        super.notifyError();
        if (currentIsMainThread()) {
            onStopVideoRecording(this.mPaused);
            if (this.mPaused) {
                closeCamera();
            }
        }
    }

    private void onStereoModeChanged() {
        enableCameraControls(false);
        this.mActivity.getSensorStateManager().setFocusSensorEnabled(false);
        if (this.mFocusManager != null && this.mFocusManager.isNeedCancelAutoFocus()) {
            this.mFocusManager.cancelAutoFocus();
        }
        this.mStereoSwitchThread = new StereoSwitchThread();
        this.mStereoSwitchThread.start();
    }

    protected void onCameraOpen() {
    }

    protected void prepareRecording() {
    }

    public void onStopRecording() {
        AutoLockManager.getInstance(this.mActivity).hibernateDelayed();
    }

    private void setProfileToRecorder() {
        this.mMediaRecorder.setOutputFormat(this.mProfile.fileFormat);
        this.mMediaRecorder.setVideoFrameRate(this.mProfile.videoFrameRate);
        this.mMediaRecorder.setVideoSize(this.mProfile.videoFrameWidth, this.mProfile.videoFrameHeight);
        this.mMediaRecorder.setVideoEncodingBitRate(this.mProfile.videoBitRate);
        this.mMediaRecorder.setVideoEncoder(this.mProfile.videoCodec);
        if (this.mProfile.audioCodec >= 0) {
            this.mMediaRecorder.setAudioEncodingBitRate(this.mProfile.audioBitRate);
            this.mMediaRecorder.setAudioChannels(this.mProfile.audioChannels);
            this.mMediaRecorder.setAudioSamplingRate(this.mProfile.audioSampleRate);
            this.mMediaRecorder.setAudioEncoder(this.mProfile.audioCodec);
        }
    }

    public boolean isCaptureIntent() {
        return this.mIsVideoCaptureIntent;
    }

    private void waitStereoSwitchThread() {
        try {
            if (this.mStereoSwitchThread != null) {
                this.mStereoSwitchThread.cancel();
                this.mStereoSwitchThread.join();
                this.mStereoSwitchThread = null;
            }
        } catch (InterruptedException e) {
        }
    }

    private void updateStereoSettings(boolean open) {
        if (!CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            return;
        }
        if (open) {
            this.mSettingsOverrider.overrideSettings("pref_camera_video_flashmode_key", "off");
            return;
        }
        this.mSettingsOverrider.restoreSettings();
    }

    private void silenceSounds() {
        if (this.mAudioManager == null) {
            this.mAudioManager = (AudioManager) this.mActivity.getSystemService("audio");
        }
        this.mAudioManager.requestAudioFocus(null, 3, 2);
        this.mOriginalMusicVolume = this.mAudioManager.getStreamVolume(3);
        if (this.mOriginalMusicVolume != 0) {
            this.mAudioManager.setStreamMute(3, true);
            this.mHandler.sendEmptyMessageDelayed(23, 3000);
        }
    }

    private void restoreMusicSound() {
        if (this.mOriginalMusicVolume != 0 && this.mAudioManager.getStreamVolume(3) == 0) {
            this.mAudioManager.setStreamMute(3, false);
        }
        this.mOriginalMusicVolume = 0;
        this.mHandler.removeMessages(23);
    }
}
