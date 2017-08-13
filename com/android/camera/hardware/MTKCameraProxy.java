package com.android.camera.hardware;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy.ContinuousShotCallback;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import miui.reflect.NoSuchMethodException;

public class MTKCameraProxy extends CameraHardwareProxy {

    public interface StereoDataCallback {
    }

    private class ContinuousShotCallbackProxy implements InvocationHandler {
        private ContinuousShotCallback mContinuousCallback;

        public ContinuousShotCallbackProxy(ContinuousShotCallback callback) {
            this.mContinuousCallback = callback;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (this.mContinuousCallback != null && method.getName().equals("onConinuousShotDone")) {
                this.mContinuousCallback.onContinuousShotDone(((Integer) args[0]).intValue());
            }
            return null;
        }
    }

    private static class SameNameCallbackProxy implements InvocationHandler {
        private Class<?> mClazz;
        private Object mRealCallbackImpl;

        public SameNameCallbackProxy(Object callback, Class<?> cls) {
            this.mRealCallbackImpl = callback;
            this.mClazz = callback.getClass();
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.v("MTKCameraProxy", "invokeinvokeinvokeinvoke " + this.mClazz);
            if (!(this.mRealCallbackImpl == null || method == null)) {
                miui.reflect.Method realMethod = miui.reflect.Method.of(this.mClazz, method.getName(), method.getReturnType(), method.getParameterTypes());
                if (realMethod != null) {
                    realMethod.invoke(this.mClazz, this.mRealCallbackImpl, args);
                }
            }
            return null;
        }
    }

    public interface StereoWarningCallback {
    }

    public void setContinuousShotCallback(Camera camera, ContinuousShotCallback callback) {
        String className = Device.getContinuousShotCallbackClass();
        String setter = Device.getContinuousShotCallbackSetter();
        if (className == null || setter == null) {
            Log.w("MTKCameraProxy", "Insufficient continuous shot callback info[class:" + className + " setter:" + setter + "]");
            className = "ContinuousShotCallback";
            setter = "setContinuousShotCallback";
        }
        Object callbackProxy = null;
        if (callback != null) {
            try {
                callbackProxy = Proxy.newProxyInstance(Class.forName("android.hardware.Camera$" + className).getClassLoader(), new Class[]{callbackClazz}, new ContinuousShotCallbackProxy(callback));
            } catch (IllegalArgumentException e) {
                Log.e("MTKCameraProxy", "IllegalArgumentException", e);
                return;
            } catch (ClassNotFoundException e2) {
                Log.e("MTKCameraProxy", "ClassNotFoundException", e2);
                return;
            }
        }
        Class<?>[] ownerClazz = new Class[]{camera.getClass()};
        miui.reflect.Method method = Util.getMethod(ownerClazz, setter, "(Landroid/hardware/Camera$" + className + ";)V");
        if (method != null) {
            method.invoke(ownerClazz[0], camera, new Object[]{callbackProxy});
        }
    }

    public void setBurstShotSpeed(Camera camera, int speed) {
        try {
            Log.d("MTKCameraProxy", "setBurstShotSpeed to " + speed + " fps");
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            miui.reflect.Method method = Util.getMethod(ownerClazz, "setContinuousShotSpeed", "(I)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{Integer.valueOf(speed)});
            }
        } catch (IllegalArgumentException e) {
            Log.e("MTKCameraProxy", "setBurstShotSpeed IllegalArgumentException");
        }
    }

    public void cancelContinuousMode(Camera camera) {
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            Util.getMethod(ownerClazz, "cancelContinuousShot", "()V").invoke(ownerClazz[0], camera, new Object[0]);
        } catch (NoSuchMethodException e) {
        } catch (IllegalArgumentException e2) {
            Log.e("MTKCameraProxy", "cancelContinuousMode IllegalArgumentException");
        }
    }

    public List<String> getSupportedFocusModes(Parameters params) {
        List<String> list = params.getSupportedFocusModes();
        list.remove("manual");
        return list;
    }

    public List<String> getSupportedIsoValues(Parameters params) {
        return CameraHardwareProxy.split(params.get("iso-speed-values"));
    }

    public void setISOValue(Parameters params, String iso) {
        params.set("iso-speed", iso);
    }

    public void setZSLMode(Parameters params, String zsl) {
        params.set("zsd-mode", zsl);
    }

    public void setSaturation(Parameters params, String saturation) {
        params.set("saturation", saturation);
    }

    public void setContrast(Parameters params, String contrast) {
        params.set("contrast", contrast);
    }

    public void setSharpness(Parameters params, String sharpness) {
        params.set("edge", sharpness);
    }

    public void setCameraMode(Parameters params, int value) {
        params.set("mtk-cam-mode", value);
    }

    public void setBurstShotNum(Parameters params, int value) {
        params.set("burst-num", value);
    }

    public void setCaptureMode(Parameters params, String value) {
        params.set("cap-mode", value);
    }

    public void setPictureFlip(Parameters params, String value) {
        params.set("snapshot-picture-flip", value);
    }

    public String getPictureFlip(Parameters params) {
        return params.get("snapshot-picture-flip");
    }

    public List<String> getSupportedCaptureMode(Parameters params) {
        return CameraHardwareProxy.split(params.get("cap-mode-values"));
    }

    public boolean isFrontMirror(Parameters params) {
        return "1".equals(getPictureFlip(params));
    }

    public List<String> getNormalFlashModes(Parameters params) {
        String str = params.get("flash-mode-values");
        if (str == null || str.length() == 0) {
            return null;
        }
        return CameraHardwareProxy.split("off,on,auto,red-eye,torch");
    }

    public void setAutoExposure(Parameters params, String value) {
        params.set("exposure-meter", value);
    }

    public void setSmoothLevel(Parameters params, String value) {
        params.set("fb-smooth-level", value);
    }

    public void setFacePosition(Parameters params, String value) {
        params.set("fb-face-pos", value);
    }

    public void setFaceBeauty(Parameters params, String value) {
        params.set("face-beauty", value);
    }

    public void set3dnrMode(Parameters params, String value) {
        params.set("3dnr-mode", value);
    }

    public void setEnlargeEye(Parameters params, String value) {
        if ("off".equals(value)) {
            params.remove("fb-enlarge-eye");
        } else {
            params.set("fb-enlarge-eye", value);
        }
    }

    public void setSlimFace(Parameters params, String value) {
        if ("off".equals(value)) {
            params.remove("fb-slim-face");
        } else {
            params.set("fb-slim-face", value);
        }
    }

    public void setSkinColor(Parameters params, String value) {
        if ("off".equals(value)) {
            params.remove("fb-skin-color");
        } else {
            params.set("fb-skin-color", value);
        }
    }

    public void setExtremeBeauty(Parameters params, String value) {
        params.set("fb-extreme-beauty", value);
    }

    public void setSlowMotion(Parameters params, String value) {
        params.set("slow-motion", value);
    }

    public void setVideoHighFrameRate(Parameters params, String frameRate) {
        params.set("video-hfr", frameRate);
    }

    public String getVideoHighFrameRate(Parameters params) {
        return params.get("video-hfr");
    }

    public boolean isZSLMode(Parameters params) {
        return "on".equals(params.get("zsd-mode"));
    }

    public List<String> getSupportedAutoexposure(Parameters params) {
        return CameraHardwareProxy.split(params.get("exposure-meter-values"));
    }

    public void setVsDofLevel(Parameters params, String level) {
        params.set("stereo-dof-level", level);
    }

    public void setVsDofMode(Parameters parameters, boolean isVsDof) {
        parameters.set("stereo-vsdof-mode", isVsDof ? "on" : "off");
        parameters.set("stereo-image-refocus", isVsDof ? "on" : "off");
        parameters.set("stereo-denoise-mode", "off");
    }

    public void setStereoDataCallback(Camera camera, Object cb) {
        Log.v("MTKCameraProxy", "setStereoDataCallback");
        if (Device.isSupportedStereo()) {
            try {
                Log.v("MTKCameraProxy", "setStereoDataCallback 366");
                Object callbackProxy = null;
                if (cb != null) {
                    callbackProxy = Proxy.newProxyInstance(Class.forName("android.hardware.Camera$StereoCameraDataCallback").getClassLoader(), new Class[]{callbackClazz}, new SameNameCallbackProxy(cb, StereoDataCallback.class));
                }
                Class<?>[] ownerClazz = new Class[]{camera.getClass()};
                Log.v("MTKCameraProxy", "setStereoDataCallback 375");
                miui.reflect.Method method = Util.getMethod(ownerClazz, "setStereoCameraDataCallback", "(Landroid/hardware/Camera$StereoCameraDataCallback;)V");
                Log.v("MTKCameraProxy", "setStereoDataCallback 378");
                if (method != null) {
                    Log.v("MTKCameraProxy", "setStereoDataCallback 380");
                    method.invoke(ownerClazz[0], camera, new Object[]{callbackProxy});
                }
            } catch (IllegalArgumentException e) {
                Log.e("MTKCameraProxy", "IllegalArgumentException", e);
            } catch (ClassNotFoundException e2) {
                Log.e("MTKCameraProxy", "ClassNotFoundException", e2);
            }
        }
    }

    public void setStereoWarningCallback(Camera camera, Object cb) {
        Log.v("MTKCameraProxy", "setStereoWarningCallback");
        if (Device.isSupportedStereo()) {
            Object callbackProxy = null;
            if (cb != null) {
                try {
                    callbackProxy = Proxy.newProxyInstance(Class.forName("android.hardware.Camera$StereoCameraWarningCallback").getClassLoader(), new Class[]{callbackClazz}, new SameNameCallbackProxy(cb, StereoWarningCallback.class));
                } catch (IllegalArgumentException e) {
                    Log.e("MTKCameraProxy", "IllegalArgumentException", e);
                    return;
                } catch (ClassNotFoundException e2) {
                    Log.e("MTKCameraProxy", "ClassNotFoundException", e2);
                    return;
                }
            }
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            miui.reflect.Method method = Util.getMethod(ownerClazz, "setStereoCameraWarningCallback", "(Landroid/hardware/Camera$StereoCameraWarningCallback;)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{callbackProxy});
            }
        }
    }

    public void enableRaw(Camera camera, Object obj) {
        try {
            Class<?>[] ownerClazz = new Class[]{camera.getClass()};
            miui.reflect.Method method = Util.getMethod(ownerClazz, "enableRaw16", "(Z)V");
            if (method != null) {
                method.invoke(ownerClazz[0], camera, new Object[]{obj});
                return;
            }
            Log.e("MTKCameraProxy", "disableRawCallback NoSuchMethodException ownerClazz=" + ownerClazz[0]);
        } catch (IllegalArgumentException e) {
            Log.e("MTKCameraProxy", "disableRawCallback IllegalArgumentException");
        }
    }

    public void enableStereoMode() {
        miui.reflect.Method method = miui.reflect.Method.of(Camera.class, "setProperty", "(Ljava/lang/String;Ljava/lang/String;)V");
        if (method != null) {
            method.invoke(Camera.class, null, new Object[]{"client.appmode", "MtkStereo"});
        }
    }

    public List<Size> getSupportedStereoPictureSizes(CameraProxy camera, Parameters parameters) {
        return splitSize(camera, parameters.get("refocus-picture-size-values"));
    }
}
