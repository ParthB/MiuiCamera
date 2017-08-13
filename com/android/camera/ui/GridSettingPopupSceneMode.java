package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class GridSettingPopupSceneMode extends GridSettingPopup {
    public GridSettingPopupSceneMode(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        preference = (IconListPreference) preferenceGroup.findPreference("pref_camera_scenemode_key");
        this.mGridViewHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.scene_settings_popup_height);
        super.initialize(preferenceGroup, preference, p);
    }
}
