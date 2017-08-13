package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class GridSettingTextPopup extends GridSettingPopup {
    private int mSavedGridViewWidth;
    private int mSavedPopupWidth;
    private SplitLineDrawer mSplitLineDrawer;

    public GridSettingTextPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mSplitLineDrawer = (SplitLineDrawer) findViewById(R.id.text_popup_split_line_view);
        this.mGridViewHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.text_only_settings_screen_popup_height);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        this.mHasImage = false;
        if ("pref_audio_focus_mode_key".equals(preference.getKey())) {
            preference = (IconListPreference) preferenceGroup.findPreference("pref_audio_focus_key");
        } else if ("pref_camera_tilt_shift_mode".equals(preference.getKey())) {
            preference = (IconListPreference) preferenceGroup.findPreference("pref_camera_tilt_shift_key");
        } else {
            this.mIgnoreSameItemClick = false;
        }
        super.initialize(preferenceGroup, preference, p);
        initializeSplitLine();
    }

    public void shrink(int width) {
        setBackgroundResource(R.drawable.bg_shrunk_audio_focus_full_screen);
        LayoutParams selfParams = getLayoutParams();
        this.mSavedPopupWidth = selfParams.width;
        selfParams.width = width;
        setLayoutParams(selfParams);
        this.mSavedGridViewWidth = setGridViewParameters(width);
        setGridViewSoundEffects(false);
        setSplitLineParameters(width, false, false);
    }

    public void restoreFromShrink() {
        updateBackground();
        if (this.mSavedPopupWidth != 0) {
            LayoutParams selfParams = getLayoutParams();
            selfParams.width = this.mSavedPopupWidth;
            this.mSavedPopupWidth = 0;
            setLayoutParams(selfParams);
        }
        if (this.mSavedGridViewWidth != 0) {
            setGridViewParameters(this.mSavedGridViewWidth);
            this.mSavedGridViewWidth = 0;
        }
        setGridViewSoundEffects(true);
        initializeSplitLine();
    }

    private void initializeSplitLine() {
        int viewWidth;
        int w = getResources().getDisplayMetrics().widthPixels;
        int h = getResources().getDisplayMetrics().heightPixels;
        if (w < h) {
            viewWidth = w;
        } else {
            viewWidth = h;
        }
        setSplitLineParameters(viewWidth, true, true);
    }

    private void setSplitLineParameters(int width, boolean topBorderVisible, boolean bottomBorderVisible) {
        RelativeLayout.LayoutParams layout = (RelativeLayout.LayoutParams) this.mSplitLineDrawer.getLayoutParams();
        layout.height = -1;
        layout.width = width;
        this.mSplitLineDrawer.setLayoutParams(layout);
        this.mSplitLineDrawer.initialize(1, this.mDisplayColumnNum);
        this.mSplitLineDrawer.setBorderVisible(topBorderVisible, bottomBorderVisible);
        this.mSplitLineDrawer.setVisibility(0);
    }

    private int setGridViewParameters(int width) {
        LayoutParams params = this.mGridView.getLayoutParams();
        int oldWidth = params.width;
        params.width = width;
        this.mGridView.setLayoutParams(params);
        return oldWidth;
    }

    private void setGridViewSoundEffects(boolean enabled) {
        if (this.mGridView != null) {
            int childCount = this.mGridView.getChildCount();
            for (int index = 0; index < childCount; index++) {
                this.mGridView.getChildAt(index).setSoundEffectsEnabled(enabled);
            }
        }
    }

    protected void updateItemView(int position, View item) {
        super.updateItemView(position, item);
        if (item != null) {
            ((TextView) item.findViewById(R.id.text)).setTextAppearance(this.mContext, R.style.GridTextOnlySettingItem);
            if (this.mDisableKeys != null && this.mPreference.getEntryValues() != null && position < this.mPreference.getEntryValues().length) {
                if (this.mDisableKeys.contains(this.mPreference.getEntryValues()[position])) {
                    item.setEnabled(false);
                } else {
                    item.setEnabled(true);
                }
            }
        }
    }

    protected void notifyToDispatcher(boolean sameItem) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(6, 0, 3, this.mPreference.getKey(), Boolean.valueOf(sameItem));
        }
    }

    protected int getItemResId() {
        return R.layout.grid_setting_text_item;
    }
}
