package com.android.camera.ui;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.Device;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.preferences.PreferenceInflater;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;

public class SkinBeautyButton extends AnimationImageView implements MessageDispacher, OnOtherPopupShowedListener, OnClickListener {
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    SkinBeautyButton.this.dismissPopup();
                    return;
                default:
                    return;
            }
        }
    };
    private V6AbstractSettingPopup mLastSubPopup;
    private V6AbstractSettingPopup mPopup;
    private IconListPreference mPreference;
    private MessageDispacher mSubDispacher = new MessageDispacher() {
        public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
            if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
                if (what == 10) {
                    SkinBeautyButton.this.sendHideMessage();
                    return true;
                }
                SkinBeautyButton.this.notifyClickToDispatcher();
                SkinBeautyButton.this.sendHideMessage();
            }
            return true;
        }
    };
    private V6AbstractSettingPopup[] mSubPopups;
    private boolean mVisible = true;

    public SkinBeautyButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPreference = (IconListPreference) new PreferenceInflater(this.mContext).inflate((int) R.xml.camera_skin_beautify_preferences);
        this.mSubPopups = new V6AbstractSettingPopup[this.mPreference.getEntryValues().length];
    }

    public void onCameraOpen() {
        int i = 0;
        super.onCameraOpen();
        if (CameraSettingPreferences.instance().isFrontCamera() && Device.isSupportedSkinBeautify() && !V6ModulePicker.isVideoModule()) {
            this.mVisible = true;
            setImageResource(this.mPreference.getIconIds()[findCurrentIndex()]);
            setVisibility(0);
            PopupManager.getInstance(this.mContext).setOnOtherPopupShowedListener(this);
            if (this.mPopup != null) {
                this.mPopup.updateBackground();
            }
            if (this.mSubPopups != null) {
                V6AbstractSettingPopup[] v6AbstractSettingPopupArr = this.mSubPopups;
                int length = v6AbstractSettingPopupArr.length;
                while (i < length) {
                    V6AbstractSettingPopup popup = v6AbstractSettingPopupArr[i];
                    if (popup != null) {
                        popup.updateBackground();
                    }
                    i++;
                }
            }
            return;
        }
        this.mVisible = false;
        setVisibility(8);
    }

    public void onPause() {
        this.mHandler.removeMessages(1);
    }

    public void setOrientation(int degree, boolean animation) {
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    protected void showSubPopup() {
        initializeSubPopup();
        V6AbstractSettingPopup subPopup = findCurrentSubPopup();
        if (subPopup != null) {
            subPopup.setOrientation(0, false);
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().showPopup(subPopup);
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().simplifyPopup(false, false);
            PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
        }
        if (!(this.mLastSubPopup == null || this.mLastSubPopup == subPopup)) {
            dismissSubPopup();
        }
        this.mLastSubPopup = subPopup;
    }

    protected boolean dismissSubPopup() {
        boolean result = false;
        if (this.mLastSubPopup != null && this.mLastSubPopup.getVisibility() == 0) {
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().dismissPopup(this.mLastSubPopup);
            result = true;
            if (findCurrentSubPopup() == null) {
                PopupManager.getInstance(getContext()).notifyDismissPopup();
            }
        }
        return result;
    }

    private V6AbstractSettingPopup findCurrentSubPopup() {
        return this.mSubPopups[findCurrentIndex()];
    }

    protected void initializeSubPopup() {
        V6AbstractSettingPopup subPopup = findCurrentSubPopup();
        if (subPopup != null) {
            subPopup.reloadPreference();
            return;
        }
        ViewGroup root = ((ActivityBase) this.mContext).getUIController().getPopupParent();
        subPopup = SettingPopupFactory.createSettingPopup(this.mPreference.getValue(), root, getContext());
        PreferenceGroup group = ((ActivityBase) this.mContext).getUIController().getPreferenceGroup();
        subPopup.initialize(group, (IconListPreference) group.findPreference(this.mPreference.getValue()), this.mSubDispacher);
        root.addView(subPopup);
        this.mSubPopups[findCurrentIndex()] = subPopup;
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(0, false);
            ((ActivityBase) this.mContext).getUIController().getTopPopupParent().showPopup(this.mPopup, true);
            setActivated(true);
        }
    }

    protected void initializePopup() {
        if (this.mPreference != null && !this.mPreference.hasPopup()) {
            Log.d("SkinBeautyButton", "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
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
        this.mHandler.removeMessages(1);
        dismissSubPopup();
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            return false;
        }
        ((ActivityBase) this.mContext).getUIController().getTopPopupParent().dismissPopup(this.mPopup, true);
        setActivated(false);
        PopupManager.getInstance(getContext()).notifyDismissPopup();
        return true;
    }

    public boolean onOtherPopupShowed(int level) {
        if (level == 1) {
            return dismissPopup();
        }
        return false;
    }

    public void recoverIfNeeded() {
    }

    private int findCurrentIndex() {
        return this.mPreference.findIndexOfValue(this.mPreference.getValue());
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        switch (what) {
            case 3:
                if (extra1 instanceof Boolean) {
                    boolean visible = ((Boolean) extra1).booleanValue();
                    if (this.mMessageDispacher != null) {
                        this.mMessageDispacher.dispacherMessage(4, R.id.skin_beatify_button, 3, Boolean.valueOf(visible), null);
                    }
                    if (visible) {
                        if (this.mPreference.getValue().equals("pref_camera_face_beauty_key") || this.mPreference.getValue().equals("pref_camera_face_beauty_advanced_key")) {
                            showSubPopup();
                            break;
                        }
                    }
                    dismissSubPopup();
                    break;
                }
                break;
            case 6:
                if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
                    notifyClickToDispatcher();
                    sendHideMessage();
                    setImageResource(this.mPreference.getIconIds()[findCurrentIndex()]);
                    if (!this.mPreference.getValue().equals("pref_camera_face_beauty_key") && !this.mPreference.getValue().equals("pref_camera_face_beauty_advanced_key")) {
                        dismissSubPopup();
                        break;
                    }
                    showSubPopup();
                    break;
                }
                break;
        }
        return true;
    }

    private void notifyClickToDispatcher() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.skin_beatify_button, 2, null, null);
        }
    }

    public void onClick(View v) {
        if (!isPopupShown()) {
            CameraDataAnalytics.instance().trackEvent("pref_camera_face_beauty_mode_key");
        }
        if (isPopupShown()) {
            dismissPopup();
        } else {
            showPopup();
            sendHideMessage();
        }
        AutoLockManager.getInstance(this.mContext).onUserInteraction();
    }

    private void sendHideMessage() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    private boolean isPopupShown() {
        return this.mPopup != null && this.mPopup.getVisibility() == 0;
    }

    private String getKey() {
        return this.mPreference == null ? "" : this.mPreference.getKey();
    }

    public boolean couldBeVisible() {
        if (CameraSettingPreferences.instance().isFrontCamera() && Device.isSupportedSkinBeautify()) {
            return V6ModulePicker.isCameraModule();
        }
        return false;
    }
}
