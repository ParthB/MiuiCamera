package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Log;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceInflater;
import java.util.ArrayList;
import java.util.List;

public class HdrButton extends AnimationImageView implements MessageDispacher, OnClickListener {
    private boolean mIsVideo;
    private String mOverrideValue;
    private V6AbstractSettingPopup mPopup;
    private IconListPreference mPreference;

    public HdrButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void initializeXml(boolean isVideo) {
        int resId;
        this.mIsVideo = isVideo;
        if (isVideo) {
            resId = R.xml.v6_video_hdr_preferences;
        } else {
            resId = R.xml.v6_camera_hdr_preferences;
        }
        this.mPreference = (IconListPreference) new PreferenceInflater(this.mContext).inflate(resId);
        filterPreference();
        if (CameraSettingPreferences.instance().isFrontCamera() || this.mPreference.getEntries().length <= 1) {
            setVisibility(8);
            return;
        }
        setVisibility(CameraSettings.isNoCameraModeSelected(this.mContext) ? 0 : 8);
        refreshValue();
    }

    public void onCreate() {
        initializeXml(V6ModulePicker.isVideoModule());
    }

    public void overrideSettings(String value) {
        this.mOverrideValue = value;
        reloadPreference();
    }

    public void reloadPreference() {
        refreshValue();
        if (this.mPreference != null && isPopupShown()) {
            this.mPopup.reloadPreference();
        }
    }

    private boolean isPopupShown() {
        return this.mPopup != null && this.mPopup.getVisibility() == 0;
    }

    public String getValue() {
        return this.mOverrideValue != null ? this.mOverrideValue : this.mPreference.getValue();
    }

    public void setValue(String value) {
        this.mPreference.setValue(value);
        reloadPreference();
    }

    public void refreshValue() {
        if (this.mPreference != null) {
            setImageResource(this.mPreference.getIconIds()[findCurrentIndex()]);
            setContentDescription(getResources().getString(R.string.accessibility_hdr) + this.mPreference.getEntry());
            if (isPopupShown()) {
                this.mPopup.reloadPreference();
            }
        }
    }

    private int findCurrentIndex() {
        return this.mPreference.findIndexOfValue(getValue());
    }

    public void setOrientation(int degree, boolean animation) {
    }

    public void onCameraOpen() {
        int i = 8;
        clearAnimation();
        if (CameraSettingPreferences.instance().isFrontCamera() || this.mPreference.getEntries().length <= 1) {
            setVisibility(8);
            return;
        }
        if (V6ModulePicker.isVideoModule() == this.mIsVideo) {
            boolean visible = CameraSettings.isNoCameraModeSelected(this.mContext);
            if (visible) {
                i = 0;
            }
            setVisibility(i);
            overrideSettings(visible ? null : "off");
        }
        if (this.mPreference.findIndexOfValue(this.mPreference.getValue()) < 0) {
            this.mPreference.setValueIndex(0);
        }
        refreshValue();
        if (this.mPopup != null) {
            this.mPopup.updateBackground();
        }
    }

    private void notifyClickToDispatcher() {
        if (this.mMessageDispacher != null && this.mPreference != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_hdr, 2, null, null);
            reloadPreference();
        }
    }

    private void notifyPopupVisibleChange(boolean visible) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(4, R.id.v6_hdr, 3, Boolean.valueOf(visible), null);
        }
    }

    public boolean couldBeVisible() {
        if (CameraSettingPreferences.instance().isFrontCamera() || ((ActivityBase) this.mContext).getUIController().getSettingPage().isItemSelected() || this.mPreference == null) {
            return false;
        }
        return this.mPreference.getEntries().length > 1;
    }

    public void updateVisible() {
        boolean z;
        boolean visible = couldBeVisible();
        if (getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        if (visible != z) {
            int i;
            overrideSettings(visible ? null : "off");
            if (visible && ((ActivityBase) this.mContext).getUIController().getFlashButton().isFlashPressed()) {
                updateHdrAccordingFlash(((ActivityBase) this.mContext).getUIController().getFlashButton().getValue());
            }
            notifyClickToDispatcher();
            if (visible) {
                i = 0;
            } else {
                i = 8;
            }
            setVisibility(i);
        }
    }

    private void filterPreference() {
        List<String> supported = new ArrayList(4);
        for (CharSequence value : this.mPreference.getEntryValues()) {
            supported.add(value.toString());
        }
        if (!this.mIsVideo) {
            if (Device.IS_MI2 || !Device.isSupportedAoHDR()) {
                supported.remove("live");
                CharSequence[] entryValues = this.mPreference.getEntryValues();
                for (int i = 0; i < entryValues.length; i++) {
                    if ("normal".equals(entryValues[i])) {
                        this.mPreference.getEntries()[i] = getResources().getString(R.string.pref_simple_hdr_entry_on);
                    }
                }
            }
            if (Device.IS_MI2A) {
                supported.remove("normal");
            }
            if (!Device.isSupportedAsdHdr()) {
                supported.remove("auto");
            }
        } else if (Device.IS_MI3TD || !Device.isSupportedAoHDR()) {
            supported.remove("on");
        }
        this.mPreference.filterUnsupported(supported);
        if (this.mPreference.findIndexOfValue(this.mPreference.getValue()) < 0) {
            this.mPreference.setValueIndex(0);
        }
    }

    public void updateHdrAccordingFlash(String flashMode) {
        String hdrMode = getValue();
        if ("auto".equals(flashMode)) {
            if ("normal".equals(hdrMode) || "live".equals(hdrMode)) {
                setValue(Device.isSupportedAsdHdr() ? "auto" : "off");
                notifyClickToDispatcher();
            }
        } else if ("on".equals(flashMode)) {
            if (!"off".equals(hdrMode)) {
                setValue("off");
                notifyClickToDispatcher();
            }
        } else if ("torch".equals(flashMode) && !"live".equals(hdrMode) && !"off".equals(hdrMode)) {
            setValue("off");
            notifyClickToDispatcher();
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

    private void doTapButton() {
        if (!isOverridden()) {
            if (this.mPreference == null || !this.mPreference.hasPopup() || this.mPreference.getEntryValues().length < 3) {
                toggle();
            } else if (isPopupShown()) {
                dismissPopup();
            } else {
                showPopup();
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
        }
        notifyClickToDispatcher();
    }

    private int getPreferenceSize() {
        CharSequence[] entries = this.mPreference.getEntryValues();
        return entries != null ? entries.length : 0;
    }

    public boolean isOverridden() {
        return this.mOverrideValue != null;
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

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(0, false);
            ((ActivityBase) this.mContext).getUIController().getTopPopupParent().showPopup(this.mPopup, true);
        }
    }

    private void initializePopup() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            Log.i("HdrButton", "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
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

    public void enableControls(boolean enabled) {
        super.enableControls(enabled);
        if (this.mPopup != null) {
            this.mPopup.setEnabled(enabled);
        }
    }
}
