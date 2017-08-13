package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraSettings;
import com.android.camera.Log;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class V6IndicatorButton extends V6AbstractIndicator implements MessageDispacher, OnClickListener {
    private View mModeRemind;
    private String mOverrideValue;
    private boolean mSelected;

    public V6IndicatorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
    }

    public V6IndicatorButton(Context context, IconListPreference pref) {
        super(context);
        this.mPreference = pref;
        reloadPreference();
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mModeRemind = findViewById(R.id.mode_remind);
    }

    public void initialize(IconListPreference preference, MessageDispacher p, ViewGroup popupRoot, int width, int height, PreferenceGroup preferenceGroup) {
        this.mSelected = !preference.isDefaultValue();
        super.initialize(preference, p, popupRoot, width, height, preferenceGroup);
        this.mImage.setOnClickListener(this);
        setClickable(false);
        updateExitButton();
        updatePopup();
        updateRemind();
    }

    public boolean isItemSelected() {
        return this.mSelected;
    }

    public void overrideSettings(String... keyValues) {
        boolean valid = false;
        this.mOverrideValue = null;
        for (int i = 0; i < keyValues.length; i += 2) {
            String key = keyValues[i];
            String value = keyValues[i + 1];
            if (key.equals(getKey())) {
                valid = true;
                this.mOverrideValue = value;
                setEnabled(value == null);
                if (valid) {
                    reloadPreference();
                }
            }
        }
        if (valid) {
            reloadPreference();
        }
    }

    protected boolean isIndicatorSelected() {
        return isPopupVisible();
    }

    public void onClick(View v) {
        boolean z = false;
        if (this.mImage != v || isEnabled()) {
            this.mSelected = !this.mSelected;
            if (v != null) {
                z = v instanceof TwoStateImageView;
            }
            notifyClickAction(z);
            onIndicatorValueChange();
            if (this.mImage != null) {
                AutoLockManager.getInstance(this.mContext).onUserInteraction();
            }
        }
    }

    private void onIndicatorValueChange() {
        resetOtherSetting();
        refreshValue();
        updatePopup();
        notifyToModule();
    }

    private void refreshValue() {
        if (this.mSelected) {
            CameraDataAnalytics.instance().trackEvent(getKey());
            this.mPreference.setValue(this.mContext.getString(R.string.pref_camera_setting_switch_entryvalue_on));
        } else {
            this.mPreference.setValue(this.mPreference.findSupportedDefaultValue());
        }
        if ("pref_camera_manual_mode_key".equals(getKey())) {
            CameraSettings.updateFocusMode();
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        super.setOrientation(orientation, animation);
        if (this.mPopup != null) {
            this.mPopup.setOrientation(orientation, animation);
        }
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (this.mMessageDispacher == null || what == 10) {
            return false;
        }
        return this.mMessageDispacher.dispacherMessage(what, sender, receiver, extra1, this);
    }

    private void updatePopup() {
        Log.v("Camera5", "updatePopup this=" + this.mPreference.getKey() + " value=" + this.mPreference.getValue() + " default=" + this.mPreference.findSupportedDefaultValue());
        if (!this.mPreference.hasPopup()) {
            return;
        }
        if (this.mSelected) {
            showPopup();
            PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
            return;
        }
        PopupManager.getInstance(getContext()).clearRecoveredPopupListenerIfNeeded(this);
        dismissPopup();
    }

    private void updateExitButton() {
        int txtId = CameraSettings.getExitText(this.mPreference.getKey());
        if (txtId == -1) {
            return;
        }
        if (this.mSelected) {
            this.mExitView.updateExitButton(txtId, true);
            this.mExitView.setExitButtonClickListener(this, this.mPreference.getKey());
        } else if (this.mExitView.isCurrentExitView(this.mPreference.getKey())) {
            this.mExitView.updateExitButton(txtId, false);
            this.mExitView.setExitButtonClickListener(null, null);
        }
    }

    private void notifyClickAction(boolean click) {
        if (click) {
            CameraSettings.cancelRemind(this.mPreference.getKey());
            if (!V6ModulePicker.isPanoramaModule() && !"pref_camera_panoramamode_key".equals(getKey())) {
                this.mMessageDispacher.dispacherMessage(9, 0, 3, getKey(), this);
            }
        }
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(this.mOrientation, false);
            this.mPopup.show(false);
        }
    }

    private void resetOtherSetting() {
        if (!V6ModulePicker.isPanoramaModule() && this.mSelected) {
            this.mMessageDispacher.dispacherMessage(8, 0, 3, getKey(), this);
        }
    }

    private void notifyToModule() {
        Log.v("Camera5", "mMessageDispacher=" + this.mMessageDispacher);
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(6, 0, 3, getKey(), this);
        }
    }

    public void resetSettings() {
        this.mSelected = false;
        onIndicatorValueChange();
        if (this.mSelected) {
            this.mPreference.setValue(this.mPreference.findSupportedDefaultValue());
        }
        dismissPopup();
    }

    public boolean dismissPopup() {
        if (!isPopupVisible()) {
            return false;
        }
        this.mPopup.dismiss(false);
        return true;
    }

    public boolean isPopupVisible() {
        boolean z;
        String str = "Camera5";
        StringBuilder append = new StringBuilder().append("visible=");
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            z = false;
        } else {
            z = true;
        }
        Log.v(str, append.append(z).append(" this=").append(this.mPreference.getKey()).toString());
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            return false;
        }
        return true;
    }

    protected void initializePopup() {
        if (this.mPopup == null && this.mPreference.hasPopup()) {
            this.mPopup = SettingPopupFactory.createSettingPopup(getKey(), this.mPopupRoot, getContext());
            this.mPopup.initialize(this.mPreferenceGroup, this.mPreference, this);
            this.mPopupRoot.addView(this.mPopup);
        }
    }

    public void reloadPreference() {
        Log.v("Camera5", "indicatorbutton reloadPreference");
        updateImage();
        updateExitButton();
        updateRemind();
        if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        }
    }

    public boolean onOtherPopupShowed(int level) {
        if (level == 1) {
            return dismissPopup();
        }
        return false;
    }

    private void updateRemind() {
        if (CameraSettings.isNeedRemind(this.mPreference.getKey())) {
            this.mModeRemind.setVisibility(0);
        } else {
            this.mModeRemind.setVisibility(8);
        }
    }

    public void removePopup() {
        if (this.mPopup != null) {
            this.mPopupRoot.removeView(this.mPopup);
            this.mPopup.onDestroy();
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.mImage.setOnClickListener(null);
    }
}
