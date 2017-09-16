package com.android.camera.camera_adapter;

import android.hardware.Camera.Parameters;
import android.media.MediaRecorder;
import android.os.Build.VERSION;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.QcomCameraProxy;
import com.android.camera.module.BaseModule;
import com.android.camera.module.VideoModule;
import java.util.HashMap;

public class VideoQcom extends VideoModule {
    private static final String VIDEO_HIGH_FRAME_RATE = (Device.IS_MI2 ? "90" : "120");
    private static QcomCameraProxy sProxy = ((QcomCameraProxy) CameraHardwareProxy.getDeviceProxy());

    static {
        if (Device.isSupportedHFR()) {
            int highSpeed480 = Util.getIntField("android.media.CamcorderProfile", null, "QUALITY_HIGH_SPEED_480P", "I");
            HashMap hashMap = VIDEO_QUALITY_TO_HIGHSPEED;
            Integer valueOf = Integer.valueOf(4);
            if (highSpeed480 == Integer.MIN_VALUE) {
                highSpeed480 = 4;
            }
            hashMap.put(valueOf, Integer.valueOf(highSpeed480));
            int highSpeed720 = Util.getIntField("android.media.CamcorderProfile", null, "QUALITY_HIGH_SPEED_720P", "I");
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
        int[] fpsRange = CameraSettings.getMaxPreviewFpsRange(this.mParameters);
        if ((Device.IS_MI4 || Device.IS_X5) && fpsRange.length > 0) {
            this.mParameters.setPreviewFpsRange(fpsRange[0], fpsRange[1]);
        } else {
            this.mParameters.setPreviewFrameRate(this.mProfile.videoFrameRate);
        }
        if (Device.isSupportedAoHDR()) {
            sProxy.setVideoHDR(this.mParameters, this.mMutexModePicker.isAoHdr() ? "on" : "off");
        }
        if (Device.isSupportedVideoQuality4kUHD()) {
            String str;
            Parameters parameters = this.mParameters;
            String str2 = "preview-format";
            if (CameraSettings.is4KHigherVideoQuality(this.mQuality)) {
                str = "nv12-venus";
            } else {
                str = "yuv420sp";
            }
            parameters.set(str2, str);
        }
        if (Device.isSupportedHFR()) {
            String highFrameRate = !"slow".equals(this.mHfr) ? "off" : VIDEO_HIGH_FRAME_RATE;
            if (BaseModule.isSupported(highFrameRate, sProxy.getSupportedVideoHighFrameRateModes(this.mParameters))) {
                Log.v("VideoQcom", "HighFrameRate value =" + highFrameRate);
                sProxy.setVideoHighFrameRate(this.mParameters, highFrameRate);
            }
        }
        if (sProxy.getSupportedDenoiseModes(this.mParameters) != null) {
            sProxy.setDenoise(this.mParameters, "denoise-on");
        }
        sProxy.setFaceWatermark(this.mParameters, false);
    }

    protected void configMediaRecorder(MediaRecorder mediaRecorder) {
        if (VERSION.SDK_INT >= 23 && "slow".equals(this.mHfr)) {
            int captureRate = 0;
            String str = null;
            try {
                str = sProxy.getVideoHighFrameRate(this.mParameters);
                captureRate = Integer.parseInt(str);
            } catch (NumberFormatException e) {
                Log.e("VideoQcom", "Invalid hfr(" + str + ")");
            }
            if (captureRate > 0) {
                Log.i("VideoQcom", "Setting capture-rate = " + captureRate);
                mediaRecorder.setCaptureRate((double) captureRate);
            }
            Log.i("VideoQcom", "Setting target fps = " + 30);
            mediaRecorder.setVideoFrameRate(30);
            int bitrate = this.mProfile.videoBitRate;
            if (!(!Device.IS_MI4 ? Device.IS_X5 : true)) {
                bitrate = (this.mProfile.videoBitRate * 30) / this.mProfile.videoFrameRate;
            }
            Log.i("VideoQcom", "Scaled Video bitrate : " + bitrate);
            mediaRecorder.setVideoEncodingBitRate(bitrate);
        }
    }

    protected boolean isShowHFRDuration() {
        return VERSION.SDK_INT < 23;
    }
}
