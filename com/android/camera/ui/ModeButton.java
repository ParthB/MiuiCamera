package com.android.camera.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;
import java.util.List;

public class ModeButton extends V6TopTextView {
    public ModeButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onCameraOpen() {
        updateVisible();
        updateRemind();
    }

    public void updateRemind() {
        List<String> keys = ((ActivityBase) this.mContext).getCurrentModule().getSupportedSettingKeys();
        boolean remind = false;
        for (String remindKey : CameraSettings.sRemindMode) {
            if (CameraSettings.isNeedRemind(remindKey) && ("pref_camera_mode_settings_key".equals(remindKey) || keys.contains(remindKey))) {
                remind = true;
                break;
            }
        }
        int paddingWidth = getResources().getDimensionPixelSize(R.dimen.panel_imageview_button_padding_width);
        if (remind) {
            int remindMargin = getResources().getDimensionPixelSize(R.dimen.mode_remind_margin);
            Drawable drawable = getResources().getDrawable(R.drawable.ic_new_remind);
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, drawable, null);
            setCompoundDrawablePadding(remindMargin);
            setPaddingRelative((drawable.getIntrinsicWidth() + remindMargin) + paddingWidth, 0, paddingWidth, 0);
        } else if (super.getCompoundDrawablesRelative()[2] != null) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null);
            setPaddingRelative(paddingWidth, 0, paddingWidth, 0);
        }
    }

    protected void notifyClickToDispatcher() {
        if (this.mMessageDispacher != null) {
            CameraSettings.cancelRemind("pref_camera_mode_settings_key");
            this.mMessageDispacher.dispacherMessage(0, R.id.mode_button, 3, null, null);
        }
    }

    public void updateVisible() {
        int i;
        List<String> keys = ((ActivityBase) this.mContext).getCurrentModule().getSupportedSettingKeys();
        if (keys == null || keys.size() == 0) {
            i = 8;
        } else {
            i = 0;
        }
        setVisibility(i);
    }
}
