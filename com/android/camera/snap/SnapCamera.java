package com.android.camera.snap;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.net.Uri;
import android.provider.MediaStore.Video.Media;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.support.v7.recyclerview.R;
import android.view.OrientationEventListener;
import com.android.camera.CameraHardwareException;
import com.android.camera.CameraHolder;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Exif;
import com.android.camera.LocationManager;
import com.android.camera.Log;
import com.android.camera.PictureSize;
import com.android.camera.PictureSizeManager;
import com.android.camera.Util;
import com.android.camera.module.VideoModule;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.Storage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;

public class SnapCamera implements OnErrorListener, OnInfoListener {
    private static final String TAG = SnapCamera.class.getSimpleName();
    private ContentValues contentValues = null;
    private CameraProxy mCamera;
    private int mCameraId;
    private Context mContext;
    private int mHeight;
    private boolean mIsCamcorder = false;
    private int mLastAngle = 0;
    private MediaRecorder mMediaRecorder;
    private OrientationEventListener mOrientationListener;
    private PictureCallback mPicture = new PictureCallback() {
        public void onPictureTaken(byte[] jpegData, Camera camera) {
            try {
                Location loc = LocationManager.instance().getCurrentLocation();
                String title = Util.createJpegName(System.currentTimeMillis()) + "_SNAP";
                int orientation = Exif.getOrientation(jpegData);
                Uri uri = Storage.addImage(SnapCamera.this.mContext, title, System.currentTimeMillis(), loc, orientation, jpegData, SnapCamera.this.mWidth, SnapCamera.this.mHeight, false, false, false);
                if (uri != null) {
                }
                if (SnapCamera.this.mStatusListener != null) {
                    SnapCamera.this.mStatusListener.onDone(uri);
                }
                if (SnapCamera.this.mCamera != null) {
                    SnapCamera.this.mCamera.startPreview();
                }
            } catch (Exception e) {
                Log.e(SnapCamera.TAG, "save picture failed " + e.getMessage());
            }
        }
    };
    private CamcorderProfile mProfile;
    private boolean mRecording = false;
    private SnapStatusListener mStatusListener;
    private SurfaceTexture mSurface;
    private int mWidth;

    public interface SnapStatusListener {
        void onDone(Uri uri);
    }

    public void onError(MediaRecorder mr, int what, int extra) {
        stopCamcorder();
    }

    public void onInfo(MediaRecorder mr, int what, int extra) {
        if (what == 800 || what == 801) {
            Log.d(TAG, "duration or file size reach MAX");
            stopCamcorder();
        }
    }

    public SnapCamera(Context context, SnapStatusListener listener) {
        try {
            LocationManager.instance().recordLocation(CameraSettings.isRecordLocation(CameraSettingPreferences.instance()));
            this.mStatusListener = listener;
            this.mContext = context;
            initSnapType();
            initOrientationListener();
            initCamera();
        } catch (Exception e) {
            Log.e(TAG, "init failed" + e.getMessage());
        }
    }

    public static boolean isSnapEnabled(Context context) {
        String snapValue = CameraSettingPreferences.instance().getString("pref_camera_snap_key", null);
        if (snapValue != null) {
            Secure.putString(context.getContentResolver(), "key_long_press_volume_down", CameraSettings.getMiuiSettingsKeyForStreetSnap(snapValue));
            CameraSettingPreferences.instance().edit().remove("pref_camera_snap_key").apply();
        }
        String snapType = Secure.getString(context.getContentResolver(), "key_long_press_volume_down");
        if ("public_transportation_shortcuts".equals(snapType) || "none".equals(snapType)) {
            return false;
        }
        return true;
    }

    private void initSnapType() {
        String snapType = Secure.getString(this.mContext.getContentResolver(), "key_long_press_volume_down");
        if (snapType.equals("Street-snap-picture")) {
            this.mIsCamcorder = false;
        } else if (snapType.equals("Street-snap-movie")) {
            this.mIsCamcorder = true;
        } else {
            this.mIsCamcorder = false;
        }
    }

    private void initCamera() {
        try {
            Constructor constructor = Class.forName("android.graphics.SurfaceTexture").getDeclaredConstructor(new Class[]{Boolean.TYPE});
            constructor.setAccessible(true);
            this.mSurface = (SurfaceTexture) constructor.newInstance(new Object[]{Boolean.valueOf(false)});
            this.mCameraId = 0;
            if (System.getInt(this.mContext.getContentResolver(), "persist.camera.snap.auto_switch", 0) == 1) {
                this.mCameraId = CameraSettings.readPreferredCameraId(CameraSettingPreferences.instance());
            }
            this.mCamera = CameraHolder.instance().open(this.mCameraId, false);
            this.mCamera.setPreviewTexture(this.mSurface);
            Parameters parameters = this.mCamera.getParameters();
            if (isCamcorder()) {
                this.mProfile = CamcorderProfile.get(this.mCameraId, CameraSettings.getPreferVideoQuality());
                parameters.set("video-size", this.mProfile.videoFrameWidth + "x" + this.mProfile.videoFrameHeight);
                parameters.set("camera-service-mute", "true");
                parameters.setFocusMode("continuous-video");
                parameters.setRecordingHint(true);
            } else {
                PictureSizeManager.initialize(null, parameters.getSupportedPictureSizes(), 0);
                PictureSize pictureSize = PictureSizeManager.getBestPictureSize();
                this.mWidth = pictureSize.width;
                this.mHeight = pictureSize.height;
                parameters.setPictureSize(this.mWidth, this.mHeight);
                parameters.setRotation(this.mLastAngle);
                parameters.set("zsl", "on");
                parameters.setFocusMode("continuous-picture");
                parameters.set("street-snap-mode", "on");
                parameters.set("no-display-mode", 1);
            }
            this.mCamera.setParameters(parameters);
            if (!isCamcorder()) {
                this.mCamera.startPreview();
            }
        } catch (CameraHardwareException e) {
            Log.e(TAG, "camera init failed " + e.getMessage());
        } catch (ClassNotFoundException e2) {
            Log.e(TAG, "reflecting constructor of SurfaceTexture failed. " + e2.getMessage());
        } catch (InvocationTargetException e3) {
            Log.e(TAG, "reflecting constructor of SurfaceTexture failed. " + e3.getMessage());
        } catch (NoSuchMethodException e4) {
            Log.e(TAG, "reflecting constructor of SurfaceTexture failed. " + e4.getMessage());
        } catch (InstantiationException e5) {
            Log.e(TAG, "reflecting constructor of SurfaceTexture failed. " + e5.getMessage());
        } catch (IllegalAccessException e6) {
            Log.e(TAG, "reflecting constructor of SurfaceTexture failed. " + e6.getMessage());
        }
    }

    private void initOrientationListener() {
        int i;
        Context context = this.mContext;
        if (Device.IS_D4 || Device.IS_C1 || Device.IS_D5) {
            i = 2;
        } else {
            i = 3;
        }
        this.mOrientationListener = new OrientationEventListener(context, i) {
            public void onOrientationChanged(int orientation) {
                int angle;
                int toAngle;
                if (45 <= orientation && orientation < 135) {
                    angle = 180;
                } else if (135 <= orientation && orientation < 225) {
                    angle = 270;
                } else if (225 > orientation || orientation >= 315) {
                    angle = 90;
                } else {
                    angle = 0;
                }
                CameraInfo info = CameraHolder.instance().getCameraInfo()[SnapCamera.this.mCameraId];
                if (angle == -1) {
                    toAngle = info.orientation;
                } else if (info.facing == 1) {
                    toAngle = (360 - angle) % 360;
                } else {
                    toAngle = angle % 360;
                }
                if (SnapCamera.this.mLastAngle != toAngle) {
                    SnapCamera.this.updateCameraOrientation(toAngle);
                    SnapCamera.this.mLastAngle = toAngle;
                }
            }
        };
        if (this.mOrientationListener.canDetectOrientation()) {
            Log.d(TAG, "Can detect orientation");
            this.mOrientationListener.enable();
            return;
        }
        Log.d(TAG, "Cannot detect orientation");
        this.mOrientationListener.disable();
    }

    public boolean isCamcorder() {
        return this.mIsCamcorder;
    }

    public void updateCameraOrientation(int angle) {
        if (!isCamcorder() && this.mCamera != null) {
            Parameters parameters = this.mCamera.getParameters();
            parameters.setRotation(angle);
            this.mCamera.setParameters(parameters);
        }
    }

    public void takeSnap() {
        try {
            this.mCamera.takePicture(null, null, null, this.mPicture);
        } catch (Exception e) {
            Log.e(TAG, "take picture failed" + e.getMessage());
        }
    }

    public void release() {
        try {
            this.mLastAngle = 0;
            LocationManager.instance().recordLocation(false);
            if (this.mOrientationListener != null) {
                this.mOrientationListener.disable();
                this.mOrientationListener = null;
            }
        } catch (Exception e) {
        }
        try {
            stopCamcorder();
        } catch (Exception e2) {
        }
        try {
            if (this.mSurface != null) {
                this.mSurface.release();
                this.mSurface = null;
            }
        } catch (Exception e3) {
        }
        try {
            if (this.mCamera != null) {
                this.mCamera.setZoomChangeListener(null);
                this.mCamera.setFaceDetectionListener(null);
                this.mCamera.setErrorCallback(null);
                this.mCamera.setOneShotPreviewCallback(null);
                this.mCamera.setAutoFocusMoveCallback(null);
                this.mCamera.addRawImageCallbackBuffer(null);
                this.mCamera.removeAllAsyncMessage();
                CameraHolder.instance().release();
                this.mCamera = null;
            }
        } catch (Exception e4) {
        }
    }

    public void startCamcorder() {
        try {
            this.mMediaRecorder = new MediaRecorder();
            this.mCamera.unlock();
            this.mMediaRecorder.setCamera(this.mCamera.getCamera());
            this.mMediaRecorder.setAudioSource(5);
            this.mMediaRecorder.setVideoSource(1);
            this.mProfile.duration = 300000;
            this.mMediaRecorder.setProfile(this.mProfile);
            this.mMediaRecorder.setMaxDuration(this.mProfile.duration);
            Location loc = LocationManager.instance().getCurrentLocation();
            if (loc != null) {
                this.mMediaRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
            }
            long dateTaken = System.currentTimeMillis();
            String title = new SimpleDateFormat(this.mContext.getString(R.string.video_file_name_format)).format(Long.valueOf(dateTaken));
            String filename = title + "_SNAP" + VideoModule.convertOutputFormatToFileExt(this.mProfile.fileFormat);
            String mime = VideoModule.convertOutputFormatToMimeType(this.mProfile.fileFormat);
            String path = Storage.DIRECTORY + '/' + filename;
            this.contentValues = new ContentValues(7);
            this.contentValues.put("title", title);
            this.contentValues.put("_display_name", filename);
            this.contentValues.put("datetaken", Long.valueOf(dateTaken));
            this.contentValues.put("mime_type", mime);
            this.contentValues.put("_data", path);
            this.contentValues.put("resolution", Integer.toString(this.mProfile.videoFrameWidth) + "x" + Integer.toString(this.mProfile.videoFrameHeight));
            if (loc != null) {
                this.contentValues.put("latitude", Double.valueOf(loc.getLatitude()));
                this.contentValues.put("longitude", Double.valueOf(loc.getLongitude()));
            }
            Log.d(TAG, "save to " + path);
            this.mMediaRecorder.setOutputFile(path);
            long maxFileSize = Storage.getAvailableSpace() - 52428800;
            if (3670016000L < maxFileSize) {
                Log.d(TAG, "need reduce , now maxFileSize = " + maxFileSize);
                maxFileSize = 3670016000L;
            }
            if (maxFileSize < VideoModule.VIDEO_MIN_SINGLE_FILE_SIZE) {
                maxFileSize = VideoModule.VIDEO_MIN_SINGLE_FILE_SIZE;
            }
            try {
                this.mMediaRecorder.setMaxFileSize(maxFileSize);
            } catch (RuntimeException e) {
            }
            Log.d(TAG, "set orientation to " + this.mLastAngle);
            this.mMediaRecorder.setOrientationHint(this.mLastAngle);
            this.mMediaRecorder.prepare();
            this.mMediaRecorder.setOnErrorListener(this);
            this.mMediaRecorder.setOnInfoListener(this);
            this.mMediaRecorder.start();
            this.mRecording = true;
        } catch (Exception e2) {
            Log.e(TAG, "prepare or start failed " + e2.getMessage());
            stopCamcorder();
            this.mCamera.lock();
        }
    }

    private void stopCamcorder() {
        if (this.mMediaRecorder != null) {
            if (this.mRecording) {
                try {
                    this.mMediaRecorder.stop();
                } catch (IllegalStateException e) {
                    this.mRecording = false;
                    e.printStackTrace();
                }
            }
            this.mMediaRecorder.reset();
            this.mMediaRecorder.release();
            this.mMediaRecorder = null;
        }
        if (this.mRecording) {
            Uri uri = null;
            try {
                uri = this.mContext.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, this.contentValues);
            } catch (Exception th) {
                th.printStackTrace();
                Log.e(TAG, "Failed to write MediaStore" + th);
            }
            if (this.mStatusListener != null) {
                this.mStatusListener.onDone(uri);
            }
        }
        this.mRecording = false;
    }
}
