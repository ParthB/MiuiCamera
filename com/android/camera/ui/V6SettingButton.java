package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.camera.AutoLockManager;

public class V6SettingButton extends RotateImageView implements OnClickListener {
    private MessageDispacher mMessageDispacher;
    private boolean mVisible = true;

    public V6SettingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void setMessageDispatcher(MessageDispacher messageDispacher) {
        this.mMessageDispacher = messageDispacher;
    }

    public void onClick(View v) {
        if (this.mVisible) {
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
            if (!this.mMessageDispacher.dispacherMessage(0, R.id.setting_button, 2, null, null)) {
            }
        }
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }
}
