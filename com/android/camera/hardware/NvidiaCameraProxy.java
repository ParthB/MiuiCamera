package com.android.camera.hardware;

import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.text.TextUtils;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy.CameraMetaDataCallback;
import java.util.List;

public class NvidiaCameraProxy extends CameraHardwareProxy {
    public Camera openCamera(int cameraId) {
        return null;
    }

    public void setParameters(Camera camera, Parameters params) {
    }

    public void cancelContinuousMode(Camera camera) {
    }

    public void setMetadataCb(Camera camera, CameraMetaDataCallback cb) {
    }

    public int getWBCurrentCCT(Camera camera) {
        return 0;
    }

    public void setAutoRotation(Parameters parameter, boolean value) {
        parameter.set("nv-auto-rotation", Boolean.toString(value));
    }

    public boolean getAutoRotation(Parameters parameter) {
        return Boolean.valueOf(parameter.get("nv-auto-rotation")).booleanValue();
    }

    public void setFlipStill(Parameters parameter, String flip) {
        parameter.set("nv-flip-still", flip);
    }

    public void setHandNight(Parameters parameter, boolean enable) {
        parameter.set("hand-night", Boolean.toString(enable));
    }

    public void setMorphoHDR(Parameters parameter, boolean enable) {
        parameter.set("nv-still-hdr-morpho", Boolean.toString(enable));
    }

    public void setFocusPosition(Parameters parameter, int position) {
        parameter.set("nv-focus-position", Integer.toString(position));
    }

    public void setWhiteBalance(Parameters parameter, String value) {
        if ("manual".equals(value)) {
            setColorTemperature(parameter, CameraSettings.getKValue());
        }
        parameter.setWhiteBalance(value);
    }

    public void setColorTemperature(Parameters parameter, int value) {
        parameter.set("nv-awb-cct-range", value + "," + value);
    }

    public void setISOValue(Parameters parameter, String str) {
        parameter.set("nv-picture-iso", str);
    }

    public String getISOValue(Parameters parameter) {
        return parameter.get("nv-picture-iso");
    }

    public void setRawDumpFlag(Parameters parameter, int flag) {
        parameter.set("nv-raw-dump-flag", Integer.toString(flag));
    }

    public boolean getAohdrEnable(Parameters parameter) {
        return Boolean.valueOf(parameter.get("nv-aohdr-enable")).booleanValue();
    }

    public void setAohdrEnable(Parameters parameter, boolean enable) {
        parameter.set("nv-aohdr-enable", Boolean.toString(enable));
    }

    public int getNSLNumBuffers(Parameters parameter) {
        return parameter.getInt("nv-nsl-num-buffers");
    }

    public void setNSLNumBuffers(Parameters parameter, int num) {
        parameter.set("nv-nsl-num-buffers", Integer.toString(num));
    }

    public void setNSLBurstCount(Parameters parameter, int count) {
        parameter.set("nv-nsl-burst-picture-count", Integer.toString(count));
    }

    public void setBurstCount(Parameters parameter, int count) {
        parameter.set("nv-burst-picture-count", Integer.toString(count));
    }

    public void setPreviewPauseDisabled(Parameters parameter, boolean disable) {
        parameter.set("nv-disable-preview-pause", Boolean.toString(disable));
    }

    public boolean getPreviewPauseDisabled(Parameters parameter) {
        return Boolean.valueOf(parameter.get("nv-disable-preview-pause")).booleanValue();
    }

    public boolean setNVShotMode(Parameters parameter, String mode) {
        if (mode == null) {
            return false;
        }
        if (mode.equals("shot2shot")) {
            parameter.set("nv-capture-mode", "shot2shot");
            return true;
        } else if (!mode.equals("normal")) {
            return false;
        } else {
            parameter.set("nv-capture-mode", "normal");
            return true;
        }
    }

    public void setSaturation(Parameters parameter, int saturation) {
        parameter.set("nv-saturation", Integer.toString(saturation));
    }

    public void setContrast(Parameters parameter, String str) {
        parameter.set("nv-contrast", str);
    }

    public void setEdgeEnhancement(Parameters parameter, int value) {
        parameter.set("nv-edge-enhancement", Integer.toString(value));
    }

    public void setExposureTime(Parameters parameter, int value) {
        parameter.set("nv-exposure-time", Integer.toString(value));
    }

    public void clearExposureTime(Parameters params) {
        setExposureTime(params, 0);
    }

    public int getNvExposureTime(Parameters parameter) {
        String exposure = parameter.get("nv-exposure-time");
        return TextUtils.isEmpty(exposure) ? 0 : Integer.parseInt(exposure);
    }

    public void setFocusMode(Parameters parameter, String value) {
        if ("manual".equals(value)) {
            value = "auto";
            setFocusPosition(parameter, CameraSettings.getFocusPosition());
        } else if ("lock".equals(value)) {
            value = "auto";
        }
        parameter.setFocusMode(value);
    }

    public List<String> getSupportedFocusModes(Parameters parameter) {
        List<String> list = parameter.getSupportedFocusModes();
        if (!(list == null || CameraSettings.isFrontCamera())) {
            if (!Util.isSupported("manual", list)) {
                list.add("manual");
            }
            if (!Util.isSupported("lock", list)) {
                list.add("lock");
            }
        }
        return list;
    }

    public boolean isFrontMirror(Parameters parameter) {
        return "horizontal".equals(parameter.get("nv-flip-still"));
    }

    public List<String> getSupportedIsoValues(Parameters parameter) {
        return CameraHardwareProxy.split(parameter.get("nv-picture-iso-values"));
    }

    public List<String> getNormalFlashModes(Parameters params) {
        String str = params.get("flash-mode-values");
        if (getAohdrEnable(params) && TextUtils.isEmpty(str)) {
            return CameraHardwareProxy.split("off,on,auto,red-eye,torch");
        }
        return params.getSupportedFlashModes();
    }

    public void setFocusAreas(Parameters parameter, List<Area> focusAreas) {
        if (focusAreas != null && focusAreas.size() > 0) {
            for (Area i : focusAreas) {
                if (!this.mHalCoordinate.contains(i.rect)) {
                    Log.e("Camera", "setFocusAreas fail :" + i.rect);
                    return;
                }
            }
        }
        String str = areaListToString(focusAreas);
        if (str != null) {
            parameter.set("focus-areas", str);
        }
    }

    public void setMeteringAreas(Parameters parameter, List<Area> meteringAreas) {
        if (meteringAreas != null && meteringAreas.size() > 0) {
            for (Area i : meteringAreas) {
                if (!this.mHalCoordinate.contains(i.rect)) {
                    Log.e("Camera", "setMeteringAreas fail :" + i.rect);
                    return;
                }
            }
        }
        String str = areaListToString(meteringAreas);
        if (str != null) {
            parameter.set("metering-areas", str);
        }
    }

    private static String areaListToString(List<Area> areaList) {
        if (areaList == null || areaList.size() == 0) {
            return null;
        }
        int size = areaList.size();
        StringBuilder windowsString = new StringBuilder(256);
        for (int i = 0; i < size; i++) {
            Area area = (Area) areaList.get(i);
            windowsString.append("(");
            windowsString.append(area.rect.left);
            windowsString.append(",");
            windowsString.append(area.rect.top);
            windowsString.append(",");
            windowsString.append(area.rect.right);
            windowsString.append(",");
            windowsString.append(area.rect.bottom);
            windowsString.append(",");
            windowsString.append(area.weight);
            windowsString.append(")");
            if (i != size - 1) {
                windowsString.append(",");
            }
        }
        return windowsString.toString();
    }
}
