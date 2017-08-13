package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Log;
import com.android.camera.Util;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.ArrayList;
import java.util.List;

public class SubScreenIndicatorButton extends V6AbstractIndicator implements MessageDispacher {
    private String mOverrideValue;
    private SubScreenPopup mParentPopup;

    public SubScreenIndicatorButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
        setClickable(true);
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (this.mMessageDispacher == null) {
            return false;
        }
        if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
            this.mMessageDispacher.dispacherMessage(what, sender, receiver, extra1, this);
        }
        reloadPreference();
        return true;
    }

    public void reloadPreference() {
        updateContent();
        if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        }
        PopupManager.getInstance(getContext()).setOnOtherPopupShowedListener(this);
    }

    protected int getShowedColor() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            return TEXT_COLOR_DEFAULT;
        }
        return super.getShowedColor();
    }

    public String getKey() {
        return this.mPreference.getKey();
    }

    public boolean isOverridden() {
        return this.mOverrideValue != null;
    }

    public void initialize(IconListPreference preference, MessageDispacher p, ViewGroup popupRoot, int width, int height, PreferenceGroup preferenceGroup, V6AbstractSettingPopup parentPopup) {
        super.initialize(preference, p, popupRoot, width, height, preferenceGroup);
        this.mParentPopup = (SubScreenPopup) parentPopup;
        rebuildPreference();
        filterPreference();
    }

    private void rebuildPreference() {
        if ("pref_qc_camera_exposuretime_key".equals(this.mPreference.getKey()) && Device.IS_XIAOMI && Device.isQcomPlatform()) {
            List<String> entries = new ArrayList();
            List<String> entryValues = new ArrayList();
            filterPreference(entries, entryValues);
            this.mPreference.setEntries((CharSequence[]) entries.toArray(new String[entries.size()]));
            this.mPreference.setEntryValues((CharSequence[]) entryValues.toArray(new String[entryValues.size()]));
        }
    }

    private void filterPreference() {
        if (this.mPreference != null && this.mPreference.getEntryValues() != null && this.mPreference.getEntryValues().length != 0) {
            boolean reload = false;
            if ("pref_qc_camera_exposuretime_key".equals(this.mPreference.getKey())) {
                CharSequence[] exposureTimes = this.mPreference.getEntryValues();
                try {
                    int currentTime = Integer.valueOf(this.mPreference.getValue()).intValue();
                    int i = exposureTimes.length - 1;
                    while (i >= 0) {
                        int exTime = Integer.valueOf(exposureTimes[i].toString()).intValue();
                        if (exTime == currentTime) {
                            break;
                        } else if (currentTime > exTime) {
                            this.mPreference.setValueIndex(i);
                            reload = true;
                            break;
                        } else {
                            i--;
                        }
                    }
                } catch (NumberFormatException e) {
                    this.mPreference.print();
                }
            } else if (this.mPreference.findIndexOfValue(this.mPreference.getValue()) < 0) {
                this.mPreference.setValueIndex(0);
                reload = true;
            }
            if (reload) {
                reloadPreference();
            }
        }
    }

    private void filterPreference(List<String> entries, List<String> entryValues) {
        String[] preferenceEntries = this.mContext.getResources().getStringArray(R.array.pref_camera_exposuretime_entries);
        String[] preferenceEntryValues = this.mContext.getResources().getStringArray(R.array.pref_camera_exposuretime_entryvalues);
        int maxExposure = Device.IS_MI4 ? 2000000 : CameraSettings.getMaxExposureTimes(this.mContext);
        int minExposure = CameraSettings.getMinExposureTimes(this.mContext);
        for (int i = 0; i < preferenceEntryValues.length; i++) {
            int entryValue = Integer.parseInt(preferenceEntryValues[i]);
            if (minExposure > entryValue || entryValue > maxExposure) {
                if (i == 0 && this.mContext.getResources().getString(R.string.pref_camera_exposuretime_default).equals(preferenceEntryValues[0])) {
                }
            }
            entries.add(preferenceEntries[i]);
            entryValues.add(preferenceEntryValues[i]);
        }
    }

    private void initializePopup() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            Log.i("SubScreenIndicatorButton", "no need to initialize popup, key=" + this.mPreference.getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
        } else if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        } else {
            this.mPopup = SettingPopupFactory.createSettingPopup(getKey(), this.mPopupRoot, getContext());
            this.mPopup.initialize(this.mPreferenceGroup, this.mPreference, this);
            this.mPopupRoot.addView(this.mPopup);
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
                if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
                    CameraDataAnalytics.instance().trackEvent(getKey());
                }
                if (!isOverridden() && this.mPreference.hasPopup()) {
                    if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
                        showPopup();
                        PopupManager.getInstance(getContext()).notifyShowPopup(this, 2);
                    } else {
                        dismissPopup();
                    }
                }
            }
            if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
                setPressed(false);
                if (!(this.mPreference == null || this.mPreference.hasPopup())) {
                    toggle();
                }
            }
            notifyMessageToDispatcher(10);
            return true;
        }
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(this.mOrientation, false);
            this.mParentPopup.showChildPopup(this.mPopup);
        }
    }

    public boolean dismissPopup() {
        setPressed(false);
        if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
            return false;
        }
        this.mParentPopup.dismissChildPopup(this.mPopup);
        return true;
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
        notifyMessageToDispatcher(6);
    }

    private int getPreferenceSize() {
        CharSequence[] entries = this.mPreference.getEntryValues();
        return entries != null ? entries.length : 0;
    }

    private void notifyMessageToDispatcher(int msg) {
        Log.v("Camera5", "mMessageDispatcher=" + this.mMessageDispacher);
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(msg, 0, 3, getKey(), this);
        }
    }

    protected void updateContent() {
        if (this.mContent != null) {
            if ("pref_skin_beautify_skin_color_key".equals(this.mPreference.getKey()) || "pref_skin_beautify_slim_face_key".equals(this.mPreference.getKey()) || "pref_skin_beautify_skin_smooth_key".equals(this.mPreference.getKey()) || "pref_skin_beautify_enlarge_eye_key".equals(this.mPreference.getKey())) {
                this.mContent.setText(CameraSettings.getSkinBeautifyHumanReadableValue(this.mContext, this.mPreference));
            } else if (this.mPreference.getEntries() != null && this.mPreference.getEntries().length != 0) {
                Util.setNumberText(this.mContent, this.mPreference.getEntry());
            } else if ("pref_focus_position_key".equals(this.mPreference.getKey())) {
                this.mContent.setText(CameraSettings.getManualFocusName(this.mContext, CameraSettings.getFocusPosition()));
            } else {
                this.mContent.setText(this.mPreference.getValue());
            }
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        if (this.mPopup != null) {
            this.mPopup.setOrientation(orientation, animation);
        }
    }

    public boolean onOtherPopupShowed(int level) {
        if (level == 2) {
            dismissPopup();
        }
        return false;
    }

    public void onDismiss() {
        super.onDismiss();
        PopupManager.getInstance(getContext()).removeOnOtherPopupShowedListener(this);
    }

    public void onDestroy() {
        super.onDestroy();
        this.mMessageDispacher = null;
    }
}
