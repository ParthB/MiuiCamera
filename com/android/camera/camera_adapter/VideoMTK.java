package com.android.camera.camera_adapter;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.MTKCameraProxy;
import com.android.camera.module.VideoModule;
import com.android.camera.stereo.WarningCallback;
import java.util.HashMap;
import miui.reflect.Method;
import miui.reflect.NoSuchMethodException;

public class VideoMTK extends VideoModule {
    private static MTKCameraProxy sProxy = ((MTKCameraProxy) CameraHardwareProxy.getDeviceProxy());
    private final WarningCallback mStereoCameraWarningCallback = new WarningCallback();

    static {
        if (Device.isSupportedHFR()) {
            int highSpeed480 = Util.getIntField("com.mediatek.camcorder.CamcorderProfileEx", null, "SLOW_MOTION_480P_120FPS", "I");
            HashMap hashMap = VIDEO_QUALITY_TO_HIGHSPEED;
            Integer valueOf = Integer.valueOf(4);
            if (highSpeed480 == Integer.MIN_VALUE) {
                highSpeed480 = 4;
            }
            hashMap.put(valueOf, Integer.valueOf(highSpeed480));
            int highSpeed720 = Util.getIntField("com.mediatek.camcorder.CamcorderProfileEx", null, "SLOW_MOTION_HD_120FPS", "I");
            hashMap = VIDEO_QUALITY_TO_HIGHSPEED;
            Integer valueOf2 = Integer.valueOf(5);
            if (highSpeed720 == Integer.MIN_VALUE) {
                highSpeed720 = 5;
            }
            hashMap.put(valueOf2, Integer.valueOf(highSpeed720));
        }
    }

    protected void updateVideoParametersPreference() {
        super.updateVideoParametersPreference();
        sProxy.setCameraMode(this.mParameters, 2);
        if (Device.isSupportedHFR()) {
            if ("slow".equals(this.mHfr)) {
                sProxy.setSlowMotion(this.mParameters, "on");
                sProxy.set3dnrMode(this.mParameters, "off");
                sProxy.setVideoHighFrameRate(this.mParameters, String.valueOf(this.mProfile.videoFrameRate));
            } else {
                sProxy.setSlowMotion(this.mParameters, "off");
                sProxy.set3dnrMode(this.mParameters, "on");
            }
            this.mParameters.setPreviewFrameRate(this.mProfile.videoFrameRate);
        }
        if (Device.isSupportedStereo()) {
            if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                this.mParameters.setPreviewFpsRange(5000, 24000);
                sProxy.setVsDofMode(this.mParameters, true);
                String vfLevel = this.mPreferences.getString("pref_camera_stereo_key", getString(R.string.pref_camera_stereo_default));
                Log.v("VideoMTK", "vfLevel value = " + vfLevel);
                sProxy.setVsDofLevel(this.mParameters, vfLevel);
            } else {
                sProxy.setVsDofMode(this.mParameters, false);
                this.mParameters.setPreviewFpsRange(5000, 30000);
            }
        }
        this.mActivity.getCameraScreenNail().setVideoStabilizationCropped(false);
    }

    protected CamcorderProfile fetchProfile(int cameraId, int quality) {
        try {
            Class<?> cls = Class.forName("com.mediatek.camcorder.CamcorderProfileEx");
            return (CamcorderProfile) Method.of(cls, "getProfile", "(II)Landroid/media/CamcorderProfile;").invokeObject(cls, null, new Object[]{Integer.valueOf(cameraId), Integer.valueOf(quality)});
        } catch (ClassNotFoundException e) {
            Log.e("VideoMTK", "com.mediatek.camcorder.CamcorderProfileEx", e);
            return null;
        } catch (NoSuchMethodException e2) {
            Log.e("VideoMTK", "no method getProfile", e2);
            return null;
        } catch (IllegalArgumentException e3) {
            Log.e("VideoMTK", "Illegal argument", e3);
            return null;
        }
    }

    protected void pauseMediaRecorder(MediaRecorder mediaRecorder) {
        try {
            Class<?> cls = Class.forName("com.mediatek.media.MediaRecorderEx");
            Method.of(cls, "pause", "(Landroid/media/MediaRecorder;)V").invoke(cls, null, new Object[]{mediaRecorder});
        } catch (ClassNotFoundException e) {
            Log.e("VideoMTK", "com.mediatek.media.MediaRecorderEx", e);
        } catch (NoSuchMethodException e2) {
            Log.e("VideoMTK", "no method pause", e2);
        } catch (IllegalArgumentException e3) {
            Log.e("VideoMTK", "Illegal argument", e3);
        }
    }

    protected boolean startRecordVideo() {
        boolean result = super.startRecordVideo();
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            setParameterExtra(this.mMediaRecorder, "media-param-audio-stop-first=1");
        }
        return result;
    }

    protected int getNormalVideoFrameRate() {
        return 30;
    }

    protected void setHFRSpeed(MediaRecorder mediaRecorder, int speed) {
        if (Device.isSupportedHFR()) {
            setParameterExtra(mediaRecorder, "media-param-slowmotion=" + speed);
        }
    }

    protected boolean isProfileExist(int cameraId, Integer quality) {
        return true;
    }

    private void setParameterExtra(MediaRecorder mediaRecorder, String value) {
        try {
            Class<?> cls = Class.forName("android.media.MediaRecorder");
            Method.of(cls, "setParametersExtra", "(Ljava/lang/String;)V").invoke(cls, mediaRecorder, new Object[]{value});
        } catch (ClassNotFoundException e) {
            Log.e("VideoMTK", "android.media.MediaRecorder", e);
        } catch (NoSuchMethodException e2) {
            Log.e("VideoMTK", "no method setParametersExtra", e2);
        } catch (IllegalArgumentException e3) {
            Log.e("VideoMTK", "Illegal argument", e3);
        }
    }

    protected void prepareOpenCamera() {
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") && !this.mIsVideoCaptureIntent) {
            sProxy.enableStereoMode();
        }
    }

    public void onSettingValueChanged(String key) {
        if (this.mCameraDevice != null) {
            if ("pref_camera_stereo_key".equals(key)) {
                String vfLevel = this.mPreferences.getString("pref_camera_stereo_key", getString(R.string.pref_camera_stereo_default));
                Log.v("VideoMTK", "vfLevel value = " + vfLevel);
                sProxy.setVsDofLevel(this.mParameters, vfLevel);
                this.mCameraDevice.setParametersAsync(this.mParameters);
                updateStatusBar("pref_camera_stereo_key");
            } else {
                super.onSettingValueChanged(key);
            }
        }
    }

    protected void onCameraOpen() {
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            this.mStereoCameraWarningCallback.setActivity(this.mActivity);
            this.mCameraDevice.setStereoWarningCallback(this.mStereoCameraWarningCallback);
            if (CameraSettings.isDualCameraHintShown(this.mPreferences)) {
                this.mHandler.sendEmptyMessage(21);
            }
        }
    }

    protected void closeCamera() {
        if (Device.isSupportedStereo() && this.mCameraDevice != null) {
            this.mCameraDevice.setStereoWarningCallback(null);
        }
        super.closeCamera();
    }
}
