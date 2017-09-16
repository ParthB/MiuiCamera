package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;

public class GridSettingExpandedTextPopup extends GridSettingPopup {
    private int mLeftMargin;
    private int mRightMargin;

    public GridSettingExpandedTextPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mIgnoreSameItemClick = false;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mGridViewHeight = this.mContext.getResources().getDimensionPixelSize(R.dimen.expanded_text_popup_height);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        this.mHasImage = false;
        this.mIgnoreSameItemClick = false;
        if ("pref_camera_flashmode_key".equals(preference.getKey())) {
            this.mLeftMargin = ((ActivityBase) this.mContext).getUIController().getFlashButton().getWidth();
            this.mRightMargin = 0;
        } else if ("pref_camera_hdr_key".equals(preference.getKey())) {
            this.mRightMargin = ((ActivityBase) this.mContext).getUIController().getHdrButton().getWidth();
            this.mLeftMargin = 0;
        } else if ("pref_camera_face_beauty_switch_key".equals(preference.getKey())) {
            this.mRightMargin = ((ActivityBase) this.mContext).getUIController().getSkinBeautyButton().getWidth();
            this.mLeftMargin = 0;
        }
        super.initialize(preferenceGroup, preference, p);
    }

    protected int getItemResId() {
        return R.layout.grid_setting_expanded_text_item;
    }

    public void updateBackground() {
    }

    public Animation getAnimation(boolean show) {
        if (this.mLeftMargin != 0) {
            return AnimationUtils.loadAnimation(this.mContext, show ? R.anim.expand_right : R.anim.shrink_left);
        }
        return AnimationUtils.loadAnimation(this.mContext, show ? R.anim.expand_left : R.anim.shrink_right);
    }

    protected void notifyToDispatcher(boolean sameItem) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(6, 0, 3, this.mPreference.getKey(), Boolean.valueOf(sameItem));
        }
    }

    protected void initGridViewLayoutParam(int itemNum) {
        LayoutParams params = (LayoutParams) this.mGridView.getLayoutParams();
        params.width = itemNum * getResources().getDimensionPixelSize(R.dimen.expanded_text_item_width);
        params.leftMargin = this.mLeftMargin;
        params.rightMargin = this.mRightMargin;
        if ("pref_camera_hdr_key".equals(this.mPreference.getKey()) || "pref_camera_face_beauty_switch_key".equals(this.mPreference.getKey())) {
            params.addRule(11, -1);
        } else {
            params.addRule(9, -1);
        }
        this.mGridView.setLayoutParams(params);
    }

    public void show(boolean animate) {
        setVisibility(0);
        if (animate) {
            clearAnimation();
            startAnimation(initAnimation(true));
        }
        notifyPopupVisibleChange(true);
    }

    public void dismiss(boolean animate) {
        if (animate) {
            clearAnimation();
            startAnimation(initAnimation(false));
        } else {
            setVisibility(8);
        }
        notifyPopupVisibleChange(false);
    }

    private Animation initAnimation(boolean show) {
        Animation animation = getAnimation(show);
        animation.setAnimationListener(new SimpleAnimationListener(this, show));
        return animation;
    }

    protected void updateItemView(int position, View item) {
        TextView tv = (TextView) item.findViewById(R.id.text);
        if (tv == null) {
            return;
        }
        if (this.mCurrentIndex == position) {
            tv.setShadowLayer(0.0f, 0.0f, 0.0f, 0);
        } else {
            tv.setShadowLayer(4.0f, 0.0f, 0.0f, -1073741824);
        }
    }
}
