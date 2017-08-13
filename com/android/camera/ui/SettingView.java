package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import com.android.camera.preferences.PreferenceGroup;
import java.util.ArrayList;
import java.util.List;

public abstract class SettingView extends RelativeLayout implements Rotatable {
    protected ArrayList<V6AbstractIndicator> mIndicators = new ArrayList();
    protected boolean mIsAnimating = false;
    protected MessageDispacher mMessageDispacher;
    protected int mOrientation = 0;
    protected PreferenceGroup mPreferenceGroup;
    protected ArrayList<Rotatable> mRotatables = new ArrayList();

    public abstract void initializeSettingScreen(PreferenceGroup preferenceGroup, List<String> list, int i, MessageDispacher messageDispacher, ViewGroup viewGroup, V6AbstractSettingPopup v6AbstractSettingPopup);

    public SettingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SettingView(Context context) {
        super(context);
    }

    public SettingView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void reloadPreferences() {
        for (V6AbstractIndicator b : this.mIndicators) {
            b.reloadPreference();
        }
    }

    public void setPressed(String key, boolean pressed) {
        for (V6AbstractIndicator b : this.mIndicators) {
            if (key != null && key.equals(b.getKey())) {
                b.setPressed(pressed);
                return;
            }
        }
    }

    public void onDismiss() {
        for (V6AbstractIndicator ind : this.mIndicators) {
            ind.onDismiss();
        }
    }

    public void onDestroy() {
        this.mIndicators.clear();
        this.mMessageDispacher = null;
    }

    public void setOrientation(int orientation, boolean animation) {
    }
}
