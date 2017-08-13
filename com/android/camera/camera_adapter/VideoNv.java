package com.android.camera.camera_adapter;

import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.NvidiaCameraProxy;
import com.android.camera.module.VideoModule;

public class VideoNv extends VideoModule {
    private static NvidiaCameraProxy sProxy = ((NvidiaCameraProxy) CameraHardwareProxy.getDeviceProxy());

    protected void updateVideoParametersPreference() {
        super.updateVideoParametersPreference();
        sProxy.setAohdrEnable(this.mParameters, false);
    }
}
