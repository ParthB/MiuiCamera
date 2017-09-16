package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceInflater;

public class PeakButton extends V6TopTextView {
    public PeakButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPreference = (IconListPreference) new PreferenceInflater(this.mContext).inflate((int) R.xml.v6_peak_focus_preferences);
        if (!Device.isSupportedPeakingMF() || CameraSettingPreferences.instance().isFrontCamera()) {
            setVisibility(8);
        } else {
            updateTitle();
        }
    }

    protected void notifyClickToDispatcher() {
        boolean z = false;
        if (this.mPreference != null) {
            EffectController instance = EffectController.getInstance();
            if (V6ModulePicker.isCameraModule() && getVisibility() == 0) {
                z = CameraSettings.isSwitchOn("pref_camera_peak_key");
            }
            instance.setDrawPeaking(z);
            reloadPreference();
        }
    }

    public void onCameraOpen() {
        if (!Device.isSupportedPeakingMF() || !V6ModulePicker.isCameraModule() || CameraSettingPreferences.instance().isFrontCamera() || 1000 == CameraSettings.getFocusPosition()) {
            setVisibility(8);
            notifyClickToDispatcher();
            return;
        }
        if (this.mPreference.findIndexOfValue(this.mPreference.getValue()) < 0) {
            this.mPreference.setValueIndex(0);
        }
        updateTitle();
        updateVisible();
    }

    public void onPause() {
        EffectController.getInstance().setDrawPeaking(false);
    }

    public boolean couldBeVisible() {
        if (!Device.isSupportedPeakingMF() || CameraSettingPreferences.instance().isFrontCamera() || !CameraSettings.isSwitchOn("pref_camera_manual_mode_key") || 1000 == CameraSettings.getFocusPosition()) {
            return false;
        }
        return true;
    }

    public void updateVisible() {
        boolean z;
        int i = 0;
        boolean visible = couldBeVisible();
        if (getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        if (visible != z) {
            if (!visible) {
                i = 8;
            }
            setVisibility(i);
        }
        notifyClickToDispatcher();
    }
}
