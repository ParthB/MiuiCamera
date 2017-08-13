package com.android.camera.hardware;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import java.util.ArrayList;
import java.util.List;
import miui.reflect.Method;
import miui.util.FeatureParser;

public class QcomCameraProxy extends CameraHardwareProxy {
    public Camera openCamera(int cameraId) {
        Camera camera = null;
        try {
            Class<?>[] ownerClazz = new Class[]{Class.forName("android.hardware.Camera")};
            Method method = Util.getMethod(ownerClazz, "openLegacy", "(II)Landroid/hardware/Camera;");
            if (method != null) {
                camera = (Camera) method.invokeObject(ownerClazz[0], null, new Object[]{Integer.valueOf(cameraId), Integer.valueOf(256)});
            }
        } catch (Exception e) {
            Log.v("QcomCameraProxy", "openLegacy failed due to " + e.getMessage() + ", using open instead");
        }
        if (camera == null) {
            return super.openCamera(cameraId);
        }
        return camera;
    }

    public void setLongshotMode(Camera camera, boolean enable) {
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            Method method = Util.getMethod(ownerClazz, "setLongshot", "(Z)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{Boolean.valueOf(enable)});
            }
        } catch (IllegalArgumentException e) {
            Log.e("QcomCameraProxy", "setLongshotMode IllegalArgumentException");
        }
    }

    public boolean isFocusSuccessful(Camera camera) {
        boolean z = true;
        if (Device.IS_B3 || Device.IS_B3_PRO) {
            return "true".equals(camera.getParameters().get("focus-done"));
        }
        int successFlag = FeatureParser.getInteger("camera_focus_success_flag", 0);
        if (successFlag != 0) {
            try {
                Class<?>[] ownerClazz = new Class[]{camera.getClass()};
                Method method = Util.getMethod(ownerClazz, "getFocusState", "()I");
                if (method != null) {
                    if (successFlag != method.invokeInt(ownerClazz[0], camera, new Object[0])) {
                        z = false;
                    }
                    return z;
                }
            } catch (IllegalArgumentException e) {
                Log.e("QcomCameraProxy", "isFocusSuccessful IllegalArgumentException");
            }
        }
        return true;
    }

    public void startObjectTrack(Camera camera, int left, int top, int width, int height) {
        Log.v("QcomCameraProxy", "startObjectTrack left=" + left + " top=" + top + " width=" + width + " height=" + height);
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            Method method = Util.getMethod(ownerClazz, "startTrack", "(IIII)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{Integer.valueOf(left), Integer.valueOf(top), Integer.valueOf(width), Integer.valueOf(height)});
            }
        } catch (IllegalArgumentException e) {
            Log.e("QcomCameraProxy", "startObjectTrack IllegalArgumentException");
        }
    }

    public void stopObjectTrack(Camera camera) {
        Log.v("QcomCameraProxy", "stopObjectTrack");
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            Method method = Util.getMethod(ownerClazz, "stopTrack", "()V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[0]);
            }
        } catch (IllegalArgumentException e) {
            Log.e("QcomCameraProxy", "stopObjectTrack IllegalArgumentException");
        }
    }

    public int getWBCurrentCCT(Camera camera) {
        String cctStr = getWBCurrentCCT(camera.getParameters());
        if (cctStr != null) {
            return Integer.parseInt(cctStr);
        }
        return 0;
    }

    public void setAoHDR(Parameters params, String aoHdrValue) {
        params.set("sensor-hdr", aoHdrValue);
    }

    public void setVideoHDR(Parameters params, String aoHdrValue) {
        params.set("video-hdr", aoHdrValue);
    }

    public void setMorphoHDR(Parameters params, boolean enable) {
        params.set("morpho-hdr", Boolean.toString(enable));
    }

    public void setMultiFaceBeautify(Parameters params, String v) {
        params.set("xiaomi-multi-face-beautify", v);
    }

    public void setHandNight(Parameters params, boolean enable) {
        params.set("morpho-hht", Boolean.toString(enable));
    }

    public void setFocusMode(Parameters params, String value) {
        if ("manual".equals(value)) {
            setFocusPosition(params, CameraSettings.getFocusPosition());
        } else if ("lock".equals(value)) {
            value = "auto";
        }
        params.setFocusMode(value);
    }

    public void setWhiteBalance(Parameters params, String value) {
        if ("manual".equals(value)) {
            setWBManualCCT(params, CameraSettings.getKValue());
        } else if ("measure".equals(value)) {
            value = "auto";
        }
        super.setWhiteBalance(params, value);
    }

    public List<String> getSupportedWhiteBalance(Parameters params) {
        List<String> list = params.getSupportedWhiteBalance();
        if (list != null && (CameraSettings.isFrontCamera() || !Device.isSupportedManualFunction())) {
            list.remove("manual");
            list.remove("manual-cct");
        }
        return list;
    }

    public void setWBManualCCT(Parameters params, int cct) {
        params.set("manual-wb-type", 0);
        params.set("manual-wb-value", cct);
    }

    private String getWBCurrentCCT(Parameters params) {
        return params.get("wb-manual-cct");
    }

    public void setFocusPosition(Parameters params, int postion) {
        setFocusPosition(params, 2, (1000 - postion) / 10);
    }

    private void setFocusPosition(Parameters params, int type, int pos) {
        params.set("manual-focus-pos-type", Integer.toString(type));
        params.set("manual-focus-position", Integer.toString(pos));
    }

    public int getMinExposureTimeValue(Parameters params) {
        String minValue = params.get("min-exposure-time");
        if (minValue == null || minValue.length() == 0) {
            return 0;
        }
        if (Device.isFloatExposureTime()) {
            return (int) (Double.parseDouble(minValue) * 1000.0d);
        }
        return Integer.parseInt(minValue);
    }

    public int getMaxExposureTimeValue(Parameters params) {
        String maxValue = params.get("max-exposure-time");
        if (maxValue == null || maxValue.length() == 0) {
            return 0;
        }
        if (Device.isFloatExposureTime()) {
            return (int) (Double.parseDouble(maxValue) * 1000.0d);
        }
        return Integer.parseInt(maxValue);
    }

    public void setExposureTime(Parameters params, int value) {
        if (Device.isFloatExposureTime()) {
            params.set("exposure-time", Double.toString(((double) value) / 1000.0d));
        } else {
            params.set("exposure-time", Integer.toString(value));
        }
    }

    public void clearExposureTime(Parameters params) {
        setExposureTime(params, 0);
    }

    public String getExposureTime(Parameters params) {
        return params.get("exposure-time");
    }

    public void setAutoExposure(Parameters params, String value) {
        params.set("auto-exposure", value);
    }

    public void setPictureFlip(Parameters params, String value) {
        params.set("snapshot-picture-flip", value);
    }

    public String getPictureFlip(Parameters params) {
        return params.get("snapshot-picture-flip");
    }

    public List<String> getSupportedDenoiseModes(Parameters params) {
        return CameraHardwareProxy.split(params.get("denoise-values"));
    }

    public void setDenoise(Parameters params, String value) {
        params.set("denoise", value);
    }

    public List<String> getSupportedIsoValues(Parameters params) {
        return CameraHardwareProxy.split(params.get("iso-values"));
    }

    public void setISOValue(Parameters params, String iso) {
        params.set("iso", iso);
    }

    public int getMaxSaturation(Parameters params) {
        return params.getInt("max-saturation");
    }

    public void setSaturation(Parameters params, int saturation) {
        if (saturation >= 0 && saturation <= params.getInt("max-saturation")) {
            params.set("saturation", String.valueOf(saturation));
        }
    }

    public int getMaxContrast(Parameters params) {
        return params.getInt("max-contrast");
    }

    public void setContrast(Parameters params, int contrast) {
        if (contrast >= 0 && contrast <= params.getInt("max-contrast")) {
            params.set("contrast", String.valueOf(contrast));
        }
    }

    public int getMaxSharpness(Parameters params) {
        return params.getInt("max-sharpness");
    }

    public void setSharpness(Parameters params, int sharpness) {
        if (sharpness >= 0 && sharpness <= params.getInt("max-sharpness")) {
            params.set("sharpness", String.valueOf(sharpness));
        }
    }

    public List<String> getSupportedTouchAfAec(Parameters params) {
        return CameraHardwareProxy.split(params.get("touch-af-aec-values"));
    }

    public void setTouchAfAec(Parameters params, String value) {
        params.set("touch-af-aec", value);
    }

    public void setFaceDetectionMode(Parameters params, String value) {
        params.set("face-detection", value);
    }

    public void setZSLMode(Parameters params, String zsl) {
        params.set("zsl", zsl);
    }

    public void setUbiFocus(Parameters params, String ubiFocusMode) {
        params.set("af-bracket", ubiFocusMode);
    }

    public String getUbiFocus(Parameters params) {
        return params.get("af-bracket");
    }

    public void setChromaFlash(Parameters params, String chromaFlash) {
        params.set("chroma-flash", chromaFlash);
    }

    public String getChromaFlash(Parameters params) {
        return params.get("chroma-flash");
    }

    public List<String> getSupportedVideoHighFrameRateModes(Parameters params) {
        return CameraHardwareProxy.split(params.get("video-hfr-values"));
    }

    public String getVideoHighFrameRate(Parameters params) {
        return params.get("video-hfr");
    }

    public void setVideoHighFrameRate(Parameters params, String hfr) {
        params.set("video-hfr", hfr);
    }

    public boolean isFrontMirror(Parameters params) {
        String pictureFlip = params.get("snapshot-picture-flip");
        if ("flip-h".equals(pictureFlip)) {
            return true;
        }
        return "flip-v".equals(pictureFlip);
    }

    public void setCameraMode(Parameters params, int cameraMode) {
        params.set("camera-mode", cameraMode);
    }

    public boolean getInternalPreviewSupported(Parameters params) {
        return "true".equals(params.get("internal-restart"));
    }

    public boolean isZSLHDRSupported(Parameters params) {
        String val = params.get("zsl-hdr-supported");
        if (val == null || !"true".equals(val)) {
            return false;
        }
        return true;
    }

    public void setOIS(Parameters params, boolean v) {
        String value = v ? "enable" : "disable";
        ArrayList<String> oisValues = CameraHardwareProxy.split(params.get("ois-values"));
        if (oisValues != null && oisValues.contains(value)) {
            params.set("ois", value);
        }
    }

    public boolean isZSLMode(Parameters params) {
        return "on".equals(params.get("zsl"));
    }

    public List<String> getSupportedAutoexposure(Parameters params) {
        return CameraHardwareProxy.split(params.get("auto-exposure-values"));
    }

    public void setNightAntiMotion(Parameters params, String value) {
        super.setNightAntiMotion(params, value);
        if ("true".equals(value)) {
            setHandNight(params, true);
            if ((Device.IS_XIAOMI || Device.IS_HM3LTE || Device.IS_H2XLTE) && !Device.isNewHdrParamKeyUsed()) {
                params.set("ae-bracket-hdr", "AE-Bracket");
                params.set("capture-burst-exposures", "0");
            }
        }
    }

    public void setNightShot(Parameters params, String value) {
        super.setNightShot(params, value);
        if ("true".equals(value)) {
            setHandNight(params, true);
            if ((Device.IS_XIAOMI || Device.IS_HM3LTE || Device.IS_H2XLTE) && !Device.isNewHdrParamKeyUsed()) {
                params.set("ae-bracket-hdr", "AE-Bracket");
                params.set("capture-burst-exposures", "0,0,0");
            }
        }
    }

    public void setHDR(Parameters params, String value) {
        super.setHDR(params, value);
        if ("true".equals(value)) {
            setMorphoHDR(params, true);
            if (!Device.isNewHdrParamKeyUsed()) {
                params.set("ae-bracket-hdr", "AE-Bracket");
                params.set("capture-burst-exposures", "-6,8,0");
            }
        }
    }

    public boolean isNeedFlashOn(Camera camera) {
        if (!Device.IS_XIAOMI || Device.IS_B3 || Device.IS_B3_PRO) {
            return "true".equals(camera.getParameters().get("flash-on"));
        }
        return super.isNeedFlashOn(camera);
    }

    public void setPortraitMode(Parameters params, String value) {
        params.set("xiaomi-portrait-mode", value);
    }

    public List<Size> getSupportedPortraitPictureSizes(CameraProxy camera, Parameters parameters) {
        return splitSize(camera, parameters.get("bokeh-picture-size"));
    }
}
