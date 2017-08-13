package com.android.camera.ui;

import android.content.Context;
import android.hardware.Camera.Parameters;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import com.android.camera.ActivityBase;
import com.android.camera.CameraManager;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Log;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.preferences.CameraPreference;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.ListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.List;
import miui.widget.SlidingButton;

public class V6SettingPage extends RelativeLayout implements MessageDispacher, V6FunctionUI, Rotatable, AnimationListener, OnCheckedChangeListener {
    private int mDefaultColumnCount;
    private SettingDismissButton mDismissButton;
    private boolean mEnabled;
    private Animation mFadeIn;
    private Animation mFadeOut;
    private int mIndicatorWidth;
    private MessageDispacher mMessageDispacher;
    private ModeView mModeView;
    public int mOrientation;
    private PreferenceGroup mPreferenceGroup;
    public V6SettingButton mSettingButton;
    private View mTitleView;
    private View mWaterMarkLayout;
    private SlidingButton mWaterMarkOptionView;

    public V6SettingPage(Context context) {
        super(context);
    }

    public V6SettingPage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDefaultColumnCount = Device.isPad() ? 6 : 3;
        this.mModeView = (ModeView) findViewById(R.id.setting_mode_view);
        this.mSettingButton = (V6SettingButton) findViewById(R.id.setting_button);
        this.mDismissButton = (SettingDismissButton) findViewById(R.id.dismiss_setting);
        this.mIndicatorWidth = this.mContext.getResources().getDimensionPixelSize(R.dimen.v6_setting_item_width);
        this.mTitleView = findViewById(R.id.setting_page_title_view);
        this.mWaterMarkLayout = findViewById(R.id.setting_page_watermark_option_layout);
        this.mWaterMarkOptionView = (SlidingButton) findViewById(R.id.setting_page_watermark_option);
        this.mWaterMarkOptionView.setOnPerformCheckedChangeListener(this);
        initAnimation();
    }

    public void onCreate() {
        this.mModeView.resetSelectedFlag();
    }

    public void onCameraOpen() {
        reload();
    }

    private void initAnimation() {
        this.mFadeIn = AnimationUtils.loadAnimation(this.mContext, R.anim.screen_setting_fade_in);
        this.mFadeOut = AnimationUtils.loadAnimation(this.mContext, R.anim.screen_setting_fade_out);
        this.mFadeIn.setAnimationListener(this);
        this.mFadeOut.setAnimationListener(this);
    }

    public void onPause() {
    }

    public void onResume() {
        setVisibility(8);
    }

    public void reload() {
        Log.v("Camera5", "reload getid=" + getId());
        removePopup();
        setVisibility(8);
        initPreference();
        initIndicators();
    }

    public boolean isItemSelected() {
        return this.mModeView.isItemSelected();
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        CameraSettings.setDualCameraWaterMarkOpen(CameraSettingPreferences.instance(), isChecked);
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.setting_page_watermark_option, 2, "pref_dualcamera_watermark", null);
        }
    }

    public void show() {
        clearAnimation();
        if (CameraSettings.isSupportedOpticalZoom() && CameraSettings.isBackCamera() && V6ModulePicker.isCameraModule()) {
            this.mTitleView.setVisibility(8);
            this.mWaterMarkOptionView.setChecked(CameraSettings.isDualCameraWaterMarkOpen(CameraSettingPreferences.instance()));
            this.mWaterMarkLayout.setVisibility(0);
        } else {
            this.mTitleView.setVisibility(0);
            this.mWaterMarkLayout.setVisibility(8);
        }
        setVisibility(0);
        reloadPreferences();
        enableControls(false);
        startAnimation(this.mFadeIn);
    }

    public void dismiss() {
        clearAnimation();
        startAnimation(this.mFadeOut);
        setVisibility(8);
    }

    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        Log.v("Camera", "V6SettingPage setVisibility =" + visibility);
    }

    public void removePopup() {
        this.mModeView.removePopup();
    }

    public View getCurrentPopup() {
        return this.mModeView.getCurrentPopup();
    }

    public boolean resetSettings() {
        return this.mModeView.resetSettings();
    }

    public void enableControls(boolean enable) {
        if (enable) {
            this.mSettingButton.setEnabled(true);
        } else {
            enableSelfControls(false);
        }
    }

    private void enableSelfControls(boolean enable) {
        setEnabled(enable);
        this.mEnabled = enable;
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
        this.mSettingButton.setMessageDispatcher(p);
        this.mDismissButton.setMessageDispatcher(p);
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (what == 9) {
            this.mMessageDispacher.dispacherMessage(0, R.id.dismiss_setting, 3, null, null);
        } else {
            this.mMessageDispacher.dispacherMessage(what, R.id.v6_setting_page, 2, extra1, extra2);
        }
        return true;
    }

    protected void initIndicators() {
        List<String> keys = ((ActivityBase) this.mContext).getCurrentModule().getSupportedSettingKeys();
        if (keys != null && keys.size() != 0) {
            this.mModeView.initializeSettingScreen(this.mPreferenceGroup, keys, this, 3);
        }
    }

    public void setEnabled(boolean enabled) {
        this.mSettingButton.setEnabled(enabled);
        this.mModeView.setEnabled(enabled);
        this.mDismissButton.setEnabled(enabled);
        super.setEnabled(enabled);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        this.mModeView.setOrientation(orientation, animation);
        this.mSettingButton.setOrientation(orientation, animation);
    }

    public void reloadPreferences() {
        this.mModeView.reloadPreferences();
    }

    public void overrideSettings(String... keyvalues) {
        this.mModeView.overrideSettings(keyvalues);
    }

    private void initPreference() {
        Parameters parameters = CameraManager.instance().getStashParameters();
        this.mPreferenceGroup = ((ActivityBase) this.mContext).getUIController().getPreferenceGroup();
        ListPreference timeLapseInterval = this.mPreferenceGroup.findPreference("pref_video_time_lapse_frame_interval_key");
        ListPreference whiteBalance = this.mPreferenceGroup.findPreference("pref_camera_whitebalance_key");
        ListPreference sceneMode = this.mPreferenceGroup.findPreference("pref_camera_scenemode_key");
        ListPreference colorEffect = this.mPreferenceGroup.findPreference("pref_camera_coloreffect_key");
        ListPreference cameraFocusMode = this.mPreferenceGroup.findPreference("pref_camera_focus_mode_key");
        if (whiteBalance != null) {
            filterUnsupportedOptions(this.mPreferenceGroup, whiteBalance, CameraHardwareProxy.getDeviceProxy().getSupportedWhiteBalance(parameters));
        }
        if (sceneMode != null) {
            filterUnsupportedOptions(this.mPreferenceGroup, sceneMode, parameters.getSupportedSceneModes());
        }
        if (colorEffect != null) {
            filterUnsupportedOptions(this.mPreferenceGroup, colorEffect, parameters.getSupportedColorEffects());
        }
        if (cameraFocusMode != null) {
            filterUnsupportedOptions(this.mPreferenceGroup, cameraFocusMode, CameraHardwareProxy.getDeviceProxy().getSupportedFocusModes(parameters));
        }
        if (timeLapseInterval != null) {
            resetIfInvalid(timeLapseInterval);
        }
    }

    private void filterUnsupportedOptions(PreferenceGroup group, ListPreference pref, List<String> supported) {
        if (supported == null || supported.size() <= 1) {
            removePreference(group, pref.getKey());
            return;
        }
        pref.filterUnsupported(supported);
        if (pref.getEntries().length <= 1) {
            removePreference(group, pref.getKey());
        } else {
            resetIfInvalid(pref);
        }
    }

    private void resetIfInvalid(ListPreference pref) {
        if (pref.findIndexOfValue(pref.getValue()) == -1) {
            pref.setValueIndex(0);
        }
    }

    private boolean removePreference(PreferenceGroup group, String key) {
        int n = group.size();
        for (int i = 0; i < n; i++) {
            CameraPreference child = group.get(i);
            if ((child instanceof PreferenceGroup) && removePreference((PreferenceGroup) child, key)) {
                return true;
            }
            if ((child instanceof ListPreference) && ((ListPreference) child).getKey().equals(key)) {
                group.removePreference(i);
                return true;
            }
        }
        return false;
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
        int id = 0;
        if (animation == this.mFadeOut) {
            id = R.id.hide_mode_animation_done;
        } else if (animation == this.mFadeIn) {
            id = R.id.show_mode_animation_done;
            enableSelfControls(true);
        }
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, id, 3, null, null);
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }
}
