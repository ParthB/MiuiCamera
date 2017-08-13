package com.android.camera.hardware;

import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.text.TextUtils.SimpleStringSplitter;
import android.text.TextUtils.StringSplitter;
import android.util.Log;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

public class CameraHardwareProxy {
    private static CameraHardwareProxy sProxy;
    protected Rect mHalCoordinate = new Rect(-1000, -1000, 1000, 1000);

    public interface CameraMetaDataCallback {
        void onCameraMetaData(byte[] bArr, Camera camera);
    }

    public interface ContinuousShotCallback {
        void onContinuousShotDone(int i);
    }

    public static class CameraHardwareFace {
        public float ageFemale;
        public float ageMale;
        public float beautyscore;
        public int blinkDetected = 0;
        public int faceRecognised = 0;
        public int faceType = 0;
        public float gender;
        public int id = -1;
        public Point leftEye = null;
        public Point mouth = null;
        public float prob;
        public Rect rect;
        public Point rightEye = null;
        public int score;
        public int smileDegree = 0;
        public int smileScore = 0;
        public int t2tStop = 0;

        public static CameraHardwareFace[] convertCameraHardwareFace(Face[] faces) {
            CameraHardwareFace[] qcomFaces = new CameraHardwareFace[faces.length];
            for (int i = 0; i < faces.length; i++) {
                qcomFaces[i] = new CameraHardwareFace();
                copyFace(qcomFaces[i], faces[i]);
            }
            return qcomFaces;
        }

        private static void copyFace(CameraHardwareFace cameraface, Face face) {
            for (Field f : face.getClass().getFields()) {
                try {
                    cameraface.getClass().getField(f.getName()).set(cameraface, f.get(face));
                } catch (IllegalArgumentException e) {
                } catch (IllegalAccessException e2) {
                } catch (NoSuchFieldException e3) {
                }
            }
        }
    }

    private static class CameraMetaDataCallbackProxy implements InvocationHandler {
        private CameraMetaDataCallback mMetaDataCallback;

        public CameraMetaDataCallbackProxy(CameraMetaDataCallback callback) {
            this.mMetaDataCallback = callback;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (this.mMetaDataCallback != null && method.getName().equals("onCameraMetaData")) {
                this.mMetaDataCallback.onCameraMetaData((byte[]) args[0], (Camera) args[1]);
            }
            return null;
        }
    }

    public static synchronized CameraHardwareProxy getDeviceProxy() {
        CameraHardwareProxy cameraHardwareProxy;
        synchronized (CameraHardwareProxy.class) {
            if (sProxy == null) {
                if (Device.isQcomPlatform()) {
                    sProxy = new QcomCameraProxy();
                } else if (Device.isLCPlatform()) {
                    sProxy = new LCCameraProxy();
                } else if (Device.isNvPlatform()) {
                    sProxy = new NvidiaCameraProxy();
                } else if (Device.isMTKPlatform()) {
                    sProxy = new MTKCameraProxy();
                } else {
                    sProxy = new CameraHardwareProxy();
                }
            }
            cameraHardwareProxy = sProxy;
        }
        return cameraHardwareProxy;
    }

    public Camera openCamera(int cameraId) {
        return Camera.open(cameraId);
    }

    public void setParameters(Camera camera, Parameters params) {
        camera.setParameters(params);
        if (Util.sIsDumpLog) {
            params.dump();
        }
    }

    public void setLongshotMode(Camera camera, boolean enable) {
    }

    public boolean isFocusSuccessful(Camera camera) {
        return true;
    }

    public boolean isNeedFlashOn(Camera camera) {
        boolean z = true;
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            miui.reflect.Method method = Util.getMethod(ownerClazz, "getFlashOn", "()I");
            if (method != null) {
                if (method.invokeInt(ownerClazz[0], camera, new Object[0]) != 1) {
                    z = false;
                }
                return z;
            }
        } catch (IllegalArgumentException e) {
            Log.e("CameraHardwareProxy", "isNeedFlashOn IllegalArgumentException");
        }
        return false;
    }

    public void setMetadataCb(Camera camera, CameraMetaDataCallback cb) {
        if (CameraSettings.isSupportedMetadata()) {
            Object callbackProxy = null;
            if (cb != null) {
                try {
                    callbackProxy = Proxy.newProxyInstance(Class.forName("android.hardware.Camera$CameraMetaDataCallback").getClassLoader(), new Class[]{callbackClazz}, new CameraMetaDataCallbackProxy(cb));
                } catch (IllegalArgumentException e) {
                    Log.e("CameraHardwareProxy", "IllegalArgumentException", e);
                    return;
                } catch (ClassNotFoundException e2) {
                    Log.e("CameraHardwareProxy", "ClassNotFoundException", e2);
                    return;
                }
            }
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            miui.reflect.Method method = Util.getMethod(ownerClazz, "setMetadataCb", "(Landroid/hardware/Camera$CameraMetaDataCallback;)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{callbackProxy});
            }
        }
    }

    public void setBurstShotSpeed(Camera camera, int speed) {
    }

    public void setContinuousShotCallback(Camera camera, ContinuousShotCallback callback) {
    }

    public boolean isPreviewEnabled(Camera camera) {
        return false;
    }

    public void startObjectTrack(Camera camera, int left, int top, int width, int height) {
    }

    public void stopObjectTrack(Camera camera) {
    }

    protected static ArrayList<String> split(String str) {
        if (str == null) {
            return null;
        }
        StringSplitter<String> splitter = new SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<String> substrings = new ArrayList();
        for (String s : splitter) {
            substrings.add(s);
        }
        return substrings;
    }

    public List<String> getSupportedFocusModes(Parameters params) {
        return params.getSupportedFocusModes();
    }

    public List<Size> getSupportedPreviewSizes(Parameters parameter) {
        List<Size> listOld = parameter.getSupportedPreviewSizes();
        List<Size> listNew = new ArrayList();
        if (listOld != null) {
            for (Size size : listOld) {
                if (size.width <= Util.sWindowHeight && size.height <= Util.sWindowWidth) {
                    listNew.add(size);
                }
            }
        }
        return listNew;
    }

    public void setStillBeautify(Parameters parameter, String v) {
        parameter.set("xiaomi-still-beautify-values", v);
    }

    public void setBeautifySkinColor(Parameters parameter, String v) {
        parameter.set("xiaomi-beauty-skin-color", v);
    }

    public void setBeautifySlimFace(Parameters parameter, String v) {
        parameter.set("xiaomi-beauty-slim-face", v);
    }

    public void setBeautifySkinSmooth(Parameters parameter, String v) {
        parameter.set("xiaomi-beauty-skin-smooth", v);
    }

    public void setBeautifyEnlargeEye(Parameters parameter, String v) {
        parameter.set("xiaomi-beauty-enlarge-eye", v);
    }

    public String getStillBeautify(Parameters parameter) {
        return parameter.get("xiaomi-still-beautify-values");
    }

    public void setTimeWatermark(Parameters parameter, String v) {
        parameter.set("xiaomi-time-watermark", v);
        parameter.set("watermark", v);
    }

    public String getTimeWatermark(Parameters parameter) {
        return parameter.get("xiaomi-time-watermark");
    }

    public void setTimeWatermarkValue(Parameters parameter, String v) {
        parameter.set("xiaomi-time-watermark-value", v);
        parameter.set("watermark_value", v);
    }

    public void setDualCameraWatermark(Parameters parameter, String v) {
        parameter.set("xiaomi-dualcam-watermark", v);
    }

    public void setFocusAreas(Parameters parameter, List<Area> focusAreas) {
        if (focusAreas != null && focusAreas.size() > 0) {
            for (Area i : focusAreas) {
                if (!this.mHalCoordinate.contains(i.rect)) {
                    Log.e("Camera", "setFocusAreas fail :" + i.rect);
                    parameter.setFocusAreas(null);
                    return;
                }
            }
        }
        parameter.setFocusAreas(focusAreas);
    }

    public void setMeteringAreas(Parameters parameter, List<Area> meteringAreas) {
        if (meteringAreas != null && meteringAreas.size() > 0) {
            for (Area i : meteringAreas) {
                if (!this.mHalCoordinate.contains(i.rect)) {
                    Log.e("Camera", "setMeteringAreas fail :" + i.rect);
                    parameter.setMeteringAreas(null);
                    return;
                }
            }
        }
        parameter.setMeteringAreas(meteringAreas);
    }

    public void setMultiFaceBeautify(Parameters parameter, String v) {
        parameter.set("xiaomi-multi-face-beautify", v);
    }

    public String getVideoHighFrameRate(Parameters parameter) {
        return "off";
    }

    public boolean isFrontMirror(Parameters params) {
        return false;
    }

    public List<String> getSupportedIsoValues(Parameters prameter) {
        return new ArrayList();
    }

    public int getRotation(Parameters prameter) {
        String rotation = prameter.get("rotation");
        if (rotation == null) {
            return -1;
        }
        return Integer.parseInt(rotation);
    }

    public void setHDR(Parameters prameter, String hdr) {
        prameter.set("mi-hdr", hdr);
    }

    public void setNightShot(Parameters prameter, String value) {
        prameter.set("night-shot", value);
    }

    public void setNightAntiMotion(Parameters prameter, String value) {
        prameter.set("night-anti-motion", value);
    }

    public int getWBCurrentCCT(Camera camera) {
        return 0;
    }

    public void cancelContinuousMode(Camera camera) {
    }

    public List<String> getNormalFlashModes(Parameters parameter) {
        return parameter.getSupportedFlashModes();
    }

    public void setFocusMode(Parameters params, String value) {
        params.setFocusMode(value);
    }

    public void setOIS(Parameters params, boolean v) {
    }

    public void setZSLMode(Parameters params, String zsl) {
    }

    public boolean isZSLMode(Parameters params) {
        return false;
    }

    public void setBeautyRank(Parameters params, boolean v) {
        params.set("xiaomi-face-beauty-rank", v ? "on" : "off");
    }

    public void setFaceWatermark(Parameters params, boolean v) {
        params.set("xiaomi-face-watermark", v ? "on" : "off");
        params.set("watermark_age", v ? "on" : "off");
    }

    public void clearExposureTime(Parameters params) {
    }

    public boolean isFaceWatermarkOn(Parameters params) {
        if ("on".equals(params.get("xiaomi-face-watermark"))) {
            return true;
        }
        return "on".equals(params.get("watermark_age"));
    }

    public List<String> getSupportedWhiteBalance(Parameters parameters) {
        return parameters.getSupportedWhiteBalance();
    }

    public void setWhiteBalance(Parameters params, String value) {
        params.setWhiteBalance(value);
    }

    public void setStereoDataCallback(Camera mCamera, Object obj) {
    }

    public void setStereoWarningCallback(Camera mCamera, Object obj) {
    }

    public void enableRaw(Camera mCamera, Object obj) {
    }

    protected ArrayList<Size> splitSize(CameraProxy camera, String str) {
        if (str == null) {
            return null;
        }
        StringSplitter<String> splitter = new SimpleStringSplitter(',');
        splitter.setString(str);
        ArrayList<Size> sizeList = new ArrayList();
        for (String s : splitter) {
            Size size = strToSize(camera, s);
            if (size != null) {
                sizeList.add(size);
            }
        }
        if (sizeList.size() == 0) {
            return null;
        }
        return sizeList;
    }

    private Size strToSize(CameraProxy camera, String str) {
        if (str == null) {
            return null;
        }
        int pos = str.indexOf(120);
        if (pos != -1) {
            String width = str.substring(0, pos);
            String height = str.substring(pos + 1);
            Camera camera2 = camera.getCamera();
            camera2.getClass();
            return new Size(camera2, Integer.parseInt(width), Integer.parseInt(height));
        }
        Log.e("CameraHardwareProxy", "Invalid size parameter string=" + str);
        return null;
    }
}
