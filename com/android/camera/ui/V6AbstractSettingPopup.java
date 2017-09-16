package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import com.android.camera.ActivityBase;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.List;

public abstract class V6AbstractSettingPopup extends RelativeLayout implements Rotatable {
    protected List<String> mDisableKeys;
    protected MessageDispacher mMessageDispacher;
    protected IconListPreference mPreference;
    protected PreferenceGroup mPreferenceGroup;

    public abstract void reloadPreference();

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        this.mPreferenceGroup = preferenceGroup;
        this.mPreference = preference;
        this.mMessageDispacher = p;
        updateBackground();
    }

    public V6AbstractSettingPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void show(boolean animate) {
        setVisibility(0);
    }

    public void dismiss(boolean animate) {
        setVisibility(8);
    }

    public void updateBackground() {
        if (((ActivityBase) this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
            setBackgroundResource(R.color.fullscreen_background);
        } else {
            setBackgroundResource(R.color.halfscreen_background);
        }
    }

    public String getKey() {
        if (this.mPreference != null) {
            return this.mPreference.getKey();
        }
        return null;
    }

    public Animation getAnimation(boolean show) {
        return null;
    }

    protected void notifyPopupVisibleChange(boolean visible) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(3, 0, 3, Boolean.valueOf(visible), null);
        }
    }

    public void onDestroy() {
    }
}
