package com.android.camera.camera_adapter;

import com.android.camera.CameraSettings;
import com.android.camera.module.VideoModule;

public class VideoLC extends VideoModule {
    protected void updateVideoParametersPreference() {
        super.updateVideoParametersPreference();
        int[] fpsRange = CameraSettings.getMaxPreviewFpsRange(this.mParameters);
        if (fpsRange != null) {
            this.mParameters.setPreviewFpsRange(fpsRange[1], fpsRange[1]);
        }
    }
}
