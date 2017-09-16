package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ViewGroup;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;

public class EffectButton extends AnimationImageView implements MessageDispacher, OnOtherPopupShowedListener {
    private static String TAG = "EffectButton";
    private boolean mDispatching = false;
    private String mOverrideValue;
    private EffectPopup mPopup;
    private IconListPreference mPreference;
    private String mSavedValue;
    private boolean mVisible = true;

    public EffectButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
    }

    public void initializeXml() {
        if (this.mPreference == null && V6ModulePicker.isCameraModule() && Device.isSupportedShaderEffect()) {
            this.mPreference = (IconListPreference) ((ActivityBase) this.mContext).getUIController().getPreferenceGroup().findPreference("pref_camera_shader_coloreffect_key");
            this.mPreference.setEntries(EffectController.getInstance().getEntries());
            this.mPreference.setEntryValues(EffectController.getInstance().getEntryValues());
            this.mPreference.setIconIds(EffectController.getInstance().getImageIds());
        }
    }

    public void onPause() {
        dismissPopup();
    }

    public void onCreate() {
        super.onCreate();
        if (V6ModulePicker.isCameraModule() && Device.isSupportedShaderEffect()) {
            this.mVisible = true;
            initializeXml();
            setVisibility(0);
            return;
        }
        this.mVisible = false;
        setVisibility(8);
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        if (this.mPopup != null) {
            this.mPopup.updateBackground();
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        int action = ev.getAction();
        if (action == 0 && !isOverridden()) {
            setPressed(true);
            return true;
        } else if (action == 3) {
            dismissPopup();
            return true;
        } else if (!isPressed() || action != 1) {
            return true;
        } else {
            if (Util.pointInView(ev.getRawX(), ev.getRawY(), this)) {
                if (!isPopupShown()) {
                    CameraDataAnalytics.instance().trackEvent(getKey());
                }
                doTapButton();
                playSoundEffect(0);
                AutoLockManager.getInstance(this.mContext).onUserInteraction();
            }
            if (!isPopupShown()) {
                setPressed(false);
            }
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
            return true;
        }
    }

    private void triggerPopup() {
        if (!isOverridden() && this.mPreference.hasPopup() && this.mPreference.getEntryValues().length >= 3) {
            if (isPopupShown() || this.mOverrideValue != null) {
                dismissPopup();
                return;
            }
            setPressed(true);
            showPopup();
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().simplifyPopup(false, false);
            PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
        }
    }

    private void doTapButton() {
        if (!isOverridden()) {
            if (this.mPreference == null || !this.mPreference.hasPopup() || this.mPreference.getEntryValues().length < 3) {
                toggle();
            } else {
                triggerPopup();
            }
        }
    }

    private void refreshIcon() {
        if (this.mPreference != null) {
            int i;
            if (findCurrentIndex() == 0) {
                i = R.drawable.ic_effect_button_normal;
            } else {
                i = R.drawable.ic_effect_button_highlight;
            }
            setImageResource(i);
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

    private String getValue() {
        return this.mOverrideValue != null ? this.mOverrideValue : this.mPreference.getValue();
    }

    public void reloadPreference() {
        if (this.mPreference != null) {
            refreshValue();
        }
    }

    public void refreshValue() {
        if (this.mPreference != null) {
            if (isPopupShown()) {
                this.mPopup.reloadPreference();
            }
            refreshIcon();
        }
    }

    private int findCurrentIndex() {
        return this.mPreference.findIndexOfValue(getValue());
    }

    public void resetSettings() {
        this.mSavedValue = getValue();
        this.mPreference.setValueIndex(0);
        dismissPopup();
        refreshValue();
    }

    public void restoreSettings() {
        if (this.mSavedValue != null) {
            this.mPreference.setValue(this.mSavedValue);
            dismissPopup();
            refreshValue();
        }
    }

    public boolean isOverridden() {
        return this.mOverrideValue != null;
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
        if (!enabled && isPressed()) {
            setPressed(false);
        }
        refreshIcon();
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
            this.mPopup.startEffectRender();
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().showPopup(this.mPopup);
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
            ViewGroup root = ((ActivityBase) this.mContext).getUIController().getPopupParent();
            this.mPopup = (EffectPopup) SettingPopupFactory.createSettingPopup(getKey(), root, getContext());
            this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
            root.addView(this.mPopup);
        }
    }

    private String getKey() {
        return this.mPreference == null ? "" : this.mPreference.getKey();
    }

    public boolean dismissPopup() {
        setPressed(false);
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            return false;
        }
        ((ActivityBase) this.mContext).getUIController().getPreviewPage().dismissPopup(this.mPopup);
        this.mPopup.stopEffectRender();
        PopupManager.getInstance(getContext()).notifyDismissPopup();
        return true;
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
            notifyClickToDispatcher();
        }
        return true;
    }

    private void notifyClickToDispatcher() {
        if (this.mMessageDispacher != null && this.mPreference != null) {
            this.mDispatching = true;
            this.mSavedValue = null;
            this.mMessageDispacher.dispacherMessage(6, R.id.v6_setting_page, 2, getKey(), this);
            this.mDispatching = false;
            refreshIcon();
        }
    }

    public void requestEffectRender() {
        if (isPopupShown()) {
            this.mPopup.requestEffectRender();
        }
    }

    public boolean onOtherPopupShowed(int level) {
        dismissPopup();
        return false;
    }

    public void recoverIfNeeded() {
    }
}
