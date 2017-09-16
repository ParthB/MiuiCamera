package com.android.camera.ui;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera.Parameters;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraManager;
import com.android.camera.Device;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceInflater;
import java.util.List;

public class FlashButton extends AnimationImageView implements MessageDispacher, OnClickListener {
    private static String TAG = "FlashButton";
    private boolean mCameraOpened;
    private boolean mDispatching = false;
    private boolean mIsVideo;
    private String mOverrideValue;
    private V6AbstractSettingPopup mPopup;
    private IconListPreference mPreference;
    private boolean mVisible = true;

    public FlashButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void initializeXml(boolean isVideo) {
        int resId;
        this.mIsVideo = isVideo;
        if (isVideo) {
            resId = R.xml.v6_video_flashmode_preferences;
        } else {
            resId = R.xml.v6_camera_flashmode_preferences;
        }
        this.mPreference = (IconListPreference) new PreferenceInflater(this.mContext).inflate(resId);
        if (!CameraSettingPreferences.instance().isFrontCamera() || Device.isSupportFrontFlash()) {
            this.mVisible = true;
            setVisibility(0);
            if (!avoidTorchOpen()) {
                refreshValue();
            }
            return;
        }
        this.mVisible = false;
        setVisibility(8);
    }

    public void onCreate() {
        this.mOverrideValue = null;
        initializeXml(V6ModulePicker.isVideoModule());
    }

    public void onResume() {
        super.onResume();
        if (Device.isPad()) {
            setVisibility(8);
        }
        avoidTorchOpen();
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean avoidTorchOpen() {
        /*
        r4 = this;
        r0 = "torch";
        r1 = r4.mPreference;
        r1 = r1.getValue();
        r0 = r0.equals(r1);
        if (r0 == 0) goto L_0x0069;
    L_0x000f:
        r0 = r4.mIsVideo;
        if (r0 != 0) goto L_0x0038;
    L_0x0013:
        r0 = r4.mContext;
        r0 = com.android.camera.CameraSettings.isNoCameraModeSelected(r0);
        if (r0 == 0) goto L_0x0060;
    L_0x001b:
        r1 = "live";
        r0 = r4.mCameraOpened;
        if (r0 == 0) goto L_0x0045;
    L_0x0022:
        r0 = r4.mContext;
        r0 = (com.android.camera.ActivityBase) r0;
        r0 = r0.getUIController();
        r0 = r0.getHdrButton();
        r0 = r0.getValue();
    L_0x0032:
        r0 = r1.equals(r0);
        if (r0 == 0) goto L_0x0060;
    L_0x0038:
        r0 = r4.mPreference;
        r1 = "off";
        r0.setValue(r1);
    L_0x0040:
        r4.refreshValue();
        r0 = 1;
        return r0;
    L_0x0045:
        r0 = r4.mPreference;
        r2 = r0.getSharedPreferences();
        r3 = "pref_camera_hdr_key";
        r0 = com.android.camera.Device.isSupportedAsdHdr();
        if (r0 == 0) goto L_0x005c;
    L_0x0054:
        r0 = "auto";
    L_0x0057:
        r0 = r2.getString(r3, r0);
        goto L_0x0032;
    L_0x005c:
        r0 = "off";
        goto L_0x0057;
    L_0x0060:
        r0 = r4.mPreference;
        r1 = "auto";
        r0.setValue(r1);
        goto L_0x0040;
    L_0x0069:
        r0 = 0;
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ui.FlashButton.avoidTorchOpen():boolean");
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        this.mCameraOpened = true;
        boolean isFront = CameraSettingPreferences.instance().isFrontCamera();
        if (!isFront || Device.isSupportFrontFlash()) {
            List list;
            Parameters parameter = CameraManager.instance().getStashParameters();
            if (parameter == null) {
                list = null;
            } else {
                list = CameraHardwareProxy.getDeviceProxy().getNormalFlashModes(parameter);
            }
            if (!(this.mIsVideo || list == null)) {
                if (Device.isSupportFrontFlash()) {
                    this.mPreference.setEntries(this.mContext.getResources().getTextArray(R.array.pref_camera_flashmode_entries));
                    this.mPreference.setEntryValues((int) R.array.pref_camera_flashmode_entryvalues);
                    this.mPreference.setIconRes(R.array.camera_flashmode_icons);
                    if (isFront) {
                        list.remove("on");
                    } else if (!Device.isSupportedTorchCapture()) {
                        list.remove("torch");
                    }
                } else if (!Device.isSupportedTorchCapture()) {
                    list.remove("torch");
                }
            }
            if (list == null || list.size() <= 1) {
                this.mVisible = false;
                setVisibility(8);
                return;
            }
            this.mPreference.filterUnsupported(list);
            if (this.mPreference.getEntries().length <= 1) {
                this.mVisible = false;
                setVisibility(8);
                return;
            }
            this.mVisible = true;
            setVisibility(0);
            if (this.mPreference.findIndexOfValue(this.mPreference.getValue()) < 0) {
                this.mPreference.setValueIndex(0);
            }
            refreshValue();
            if (this.mPopup != null) {
                this.mPopup.updateBackground();
                if (Device.isSupportFrontFlash()) {
                    this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
                }
                if (this.mPopup.getVisibility() == 0) {
                    this.mPopup.dismiss(false);
                }
            }
            return;
        }
        this.mVisible = false;
        setVisibility(8);
    }

    public void updatePopup(boolean visible) {
        if (visible != isPopupShown()) {
            if (visible) {
                setVisibility(0);
            }
            triggerPopup();
        }
    }

    private void triggerPopup() {
        if (!isOverridden() && this.mPreference.hasPopup() && this.mPreference.getEntryValues().length >= 3) {
            if (isPopupShown()) {
                dismissPopup();
            } else {
                showPopup();
            }
        }
    }

    private void doTapButton() {
        if (!isOverridden()) {
            if (!this.mPreference.hasPopup() || this.mPreference.getEntryValues().length < 3) {
                toggle();
            } else {
                triggerPopup();
            }
        }
    }

    private void toggle() {
        if (this.mPreference != null) {
            int index = this.mPreference.findIndexOfValue(this.mPreference.getValue()) + 1;
            if (index >= getPreferenceSize()) {
                index = 0;
            }
            this.mPreference.setValueIndex(index);
            reloadPreference();
            notifyClickToDispatcher();
        }
    }

    private int getPreferenceSize() {
        CharSequence[] entries = this.mPreference.getEntryValues();
        return entries != null ? entries.length : 0;
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    public String getValue() {
        return this.mOverrideValue != null ? this.mOverrideValue : this.mPreference.getValue();
    }

    public void reloadPreference() {
        if (this.mPreference != null) {
            refreshValue();
        }
    }

    public void refreshValue() {
        if (this.mPreference != null) {
            setImageResource(this.mPreference.getIconIds()[findCurrentIndex()]);
            setContentDescription(getResources().getString(R.string.accessibility_flash_mode_button) + this.mPreference.getEntry());
            if (isPopupShown()) {
                this.mPopup.reloadPreference();
            }
        }
    }

    private int findCurrentIndex() {
        return this.mPreference.findIndexOfValue(getValue());
    }

    public void overrideSettings(String value) {
        this.mOverrideValue = value;
        dismissPopup();
        refreshValue();
        setEnabled(value == null);
    }

    public void overrideValue(String value) {
        this.mOverrideValue = value;
    }

    public void setValue(String value) {
        this.mPreference.setValue(value);
        refreshValue();
    }

    public void keepSetValue(String value) {
        if (!getValue().equals(value)) {
            String restoredValue = getRestoredFlashMode();
            if (restoredValue == null) {
                setRestoredFlashMode(this.mPreference.getValue());
                setValue(value);
            } else if (restoredValue.equals(value)) {
                restoreKeptValue();
            } else {
                setValue(value);
            }
        }
    }

    public void restoreKeptValue() {
        if (isFlashPressed()) {
            setRestoredFlashMode(null);
            return;
        }
        String restoredValue = getRestoredFlashMode();
        if (restoredValue != null) {
            setValue(restoredValue);
            setRestoredFlashMode(null);
        }
    }

    public boolean isOverridden() {
        return this.mOverrideValue != null;
    }

    public void enableControls(boolean enabled) {
        super.enableControls(enabled);
        setEnabled(enabled);
    }

    public void setOrientation(int degree, boolean animation) {
    }

    public void setEnabled(boolean enabled) {
        if (isOverridden()) {
            enabled = false;
        }
        if ((isEnabled() ^ enabled) != 0) {
            super.setEnabled(enabled);
        }
        if (this.mPopup != null) {
            this.mPopup.setEnabled(enabled);
        }
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(0, false);
            ((ActivityBase) this.mContext).getUIController().getTopPopupParent().showPopup(this.mPopup, true);
        }
    }

    private boolean isPopupShown() {
        return this.mPopup != null && this.mPopup.getVisibility() == 0;
    }

    protected void initializePopup() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            Log.i(TAG, "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
        } else if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        } else {
            ViewGroup root = ((ActivityBase) this.mContext).getUIController().getTopPopupParent();
            this.mPopup = SettingPopupFactory.createSettingPopup(getKey(), root, getContext());
            this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
            root.addView(this.mPopup);
        }
    }

    public boolean dismissPopup() {
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            return false;
        }
        ((ActivityBase) this.mContext).getUIController().getTopPopupParent().dismissPopup(this.mPopup, true);
        return true;
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        switch (what) {
            case 3:
                if (extra1 instanceof Boolean) {
                    notifyPopupVisibleChange(((Boolean) extra1).booleanValue());
                    break;
                }
                break;
            case 6:
                dismissPopup();
                if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
                    notifyClickToDispatcher();
                    break;
                }
        }
        return true;
    }

    private void notifyPopupVisibleChange(boolean visible) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(4, R.id.v6_flash_mode_button, 3, Boolean.valueOf(visible), null);
        }
    }

    private void notifyClickToDispatcher() {
        if (this.mMessageDispacher != null && this.mPreference != null) {
            this.mDispatching = true;
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_flash_mode_button, 2, null, null);
            this.mDispatching = false;
            reloadPreference();
        }
    }

    public boolean isFlashPressed() {
        return this.mDispatching;
    }

    private void setRestoredFlashMode(String value) {
        Editor editor = CameraSettingPreferences.instance().edit();
        if (value == null) {
            editor.remove("pref_camera_restored_flashmode_key");
        } else {
            String str = "pref_camera_restored_flashmode_key";
            if ("torch".equals(value)) {
                value = this.mIsVideo ? "off" : "auto";
            }
            editor.putString(str, value);
        }
        editor.apply();
    }

    public static String getRestoredFlashMode() {
        return CameraSettingPreferences.instance().getString("pref_camera_restored_flashmode_key", null);
    }

    public void updateFlashModeAccordingHdr(String hdrMode) {
        if (isFlashPressed()) {
            setRestoredFlashMode(null);
            return;
        }
        String storeFlashMode = getRestoredFlashMode();
        String flashMode = storeFlashMode != null ? storeFlashMode : getValue();
        if ("auto".equals(hdrMode)) {
            if (!"off".equals(flashMode)) {
                keepSetValue("auto");
            }
        } else if ("normal".equals(hdrMode)) {
            if (!"off".equals(flashMode)) {
                keepSetValue("off");
            }
        } else if (!"live".equals(hdrMode)) {
            restoreKeptValue();
        } else if (!"off".equals(flashMode) && !"torch".equals(flashMode)) {
            keepSetValue("off");
        }
    }

    public void onClick(View v) {
        if (!isPopupShown()) {
            CameraDataAnalytics.instance().trackEvent(getKey());
        }
        doTapButton();
        AutoLockManager.getInstance(this.mContext).onUserInteraction();
    }

    private String getKey() {
        return this.mPreference == null ? "" : this.mPreference.getKey();
    }
}
