package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import com.android.camera.AutoLockManager;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.ui.V6SeekBar.OnValueChangedListener;
import java.util.ArrayList;
import java.util.List;

public class V6SeekbarPopup extends V6AbstractSettingPopup implements OnValueChangedListener {
    private V6SeekBar mBar;
    private V6SeekbarPopupTexts mTexts;
    private int mValue;

    public V6SeekbarPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBar = (V6SeekBar) findViewById(R.id.bar);
        this.mTexts = (V6SeekbarPopupTexts) findViewById(R.id.texts);
        this.mBar.setOnValueChangedListener(this);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mTexts.setOrientation(orientation, animation);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        if ("pref_camera_face_beauty_mode_key".equals(preference.getKey())) {
            preference = (IconListPreference) preferenceGroup.findPreference("pref_camera_face_beauty_key");
        } else if ("pref_delay_capture_mode".equals(preference.getKey())) {
            preference = (IconListPreference) preferenceGroup.findPreference("pref_delay_capture_key");
            filterPreference(preference);
        }
        super.initialize(preferenceGroup, preference, p);
        this.mValue = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        this.mTexts.initialize(preference);
        this.mBar.initialize(preference);
    }

    public void reloadPreference() {
        this.mValue = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        this.mTexts.setValue(this.mValue);
        this.mBar.setValue(this.mValue);
    }

    public void onValueChanged(int value, boolean touchUp) {
        if (value != this.mValue) {
            this.mValue = value;
            this.mTexts.setValue(value);
            this.mPreference.setValueIndex(value);
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(7, 0, 0, new String(this.mPreference.getKey()), null);
            }
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mBar != null) {
            this.mBar.setEnabled(enabled);
        }
    }

    private void filterPreference(IconListPreference preference) {
        if (preference != null && "pref_delay_capture_key".equals(preference.getKey())) {
            List<String> supported = new ArrayList(3);
            for (CharSequence value : preference.getEntryValues()) {
                if (!value.equals("0")) {
                    supported.add(value.toString());
                }
            }
            preference.filterUnsupported(supported);
        }
    }
}
