package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.widget.TextView;
import com.android.camera.AutoLockManager;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.ui.V6SeekBar.OnValueChangedListener;

public class V6SkinBeautifySeekbarPopup extends V6AbstractSettingPopup implements OnValueChangedListener {
    private V6SeekBar mBar;
    private TextView mMaxText;
    private TextView mMinText;
    private int mValue;

    public V6SkinBeautifySeekbarPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mBar = (V6SeekBar) findViewById(R.id.bar);
        this.mMinText = (TextView) findViewById(R.id.min);
        this.mMaxText = (TextView) findViewById(R.id.max);
        this.mBar.setOnValueChangedListener(this);
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        this.mValue = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        this.mMinText.setText(preference.getEntries()[0]);
        this.mMaxText.setText(preference.getEntries()[preference.getEntries().length - 1]);
        this.mBar.initialize(preference);
    }

    public void reloadPreference() {
        this.mValue = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        this.mBar.setValue(this.mValue);
    }

    public void onValueChanged(int value, boolean touchUp) {
        if (value != this.mValue) {
            this.mValue = value;
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
}
