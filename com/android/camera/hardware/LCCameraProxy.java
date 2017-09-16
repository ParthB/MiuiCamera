package com.android.camera.hardware;

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import java.util.List;

public class LCCameraProxy extends CameraHardwareProxy {
    public boolean isNeedFlashOn(Camera camera) {
        return false;
    }

    public boolean isPreviewEnabled(Camera camera) {
        return false;
    }

    public List<String> getSupportedIsoValues(Parameters param) {
        return CameraHardwareProxy.split(param.get("iso-mode-values"));
    }

    public void setISOValue(Parameters param, String iso) {
        param.set("iso", iso);
    }

    public void setZSLMode(Parameters param, String zsl) {
        param.set("zsl", zsl);
    }

    public boolean getZslSupported(Parameters param) {
        return "true".equals(param.get("zsl-supported"));
    }

    public void setSaturation(Parameters param, String saturation) {
        param.set("saturation", saturation);
    }

    public void setContrast(Parameters param, String contrast) {
        param.set("contrast", contrast);
    }

    public void setSharpness(Parameters param, String sharpness) {
        param.set("sharpness", sharpness);
    }

    public void setBurstShotNum(Parameters param, int value) {
        param.set("zsl-num", value);
    }

    public void setPictureFlip(Parameters param, String value) {
        param.set("snapshot-picture-flip", value);
    }

    public String getPictureFlip(Parameters param) {
        return param.get("snapshot-picture-flip");
    }

    public void setAutoExposure(Parameters param, String value) {
        param.set("metering", value);
    }

    public boolean isFrontMirror(Parameters param) {
        return "1".equals(getPictureFlip(param));
    }

    public boolean isZSLMode(Parameters params) {
        return "true".equals(params.get("zsl"));
    }

    public List<String> getSupportedAutoexposure(Parameters params) {
        return CameraHardwareProxy.split(params.get("metering-values"));
    }
}
