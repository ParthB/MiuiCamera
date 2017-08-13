package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.android.camera.ActivityBase;
import com.android.camera.Log;
import com.android.camera.Util;

public class V6ModeExitView extends LinearLayout implements V6FunctionUI {
    private String mCurrentKey;
    private ExitButton mExitButton;
    private boolean mVisible = true;

    public V6ModeExitView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mExitButton = (ExitButton) findViewById(R.id.camera_mode_exit_button);
        this.mExitButton.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                Util.expandViewTouchDelegate(V6ModeExitView.this.mExitButton);
            }
        });
    }

    public void setExitContent(int resId) {
        if (resId != -1) {
            this.mExitButton.setText(this.mContext.getResources().getString(resId));
        }
    }

    public void updateExitButton(int resId, boolean visible) {
        Log.v("Camera6", "V6ModeExitView updateExitButton = " + visible);
        if (resId != -1) {
            this.mExitButton.setText(this.mContext.getResources().getString(resId));
        }
        int i = (!visible || TextUtils.isEmpty(this.mExitButton.getText())) ? 8 : 0;
        setExitButtonVisible(i);
    }

    public boolean isExitButtonShown() {
        return this.mExitButton.getVisibility() == 0;
    }

    public void setExitButtonClickListener(OnClickListener l, String key) {
        Log.v("Camera6", "V6ModeExitView setOnClickListener = " + l + " this=" + this);
        this.mCurrentKey = key;
        this.mExitButton.setOnClickListener(l);
    }

    public boolean isCurrentExitView(String key) {
        Log.v("Camera5", "V6ModeExitView isCurrent key=" + key + " mCurrentKey=" + this.mCurrentKey);
        return key != null ? key.equals(this.mCurrentKey) : false;
    }

    public void clearExitButtonClickListener(boolean executeClick) {
        if (executeClick && this.mExitButton.hasOnClickListeners()) {
            this.mExitButton.callOnClick();
        }
        setExitButtonClickListener(null, null);
        updateExitButton(-1, false);
    }

    public void onCreate() {
        if (!isCurrentExitView("pref_camera_stereo_mode_key")) {
            setExitButtonVisible(8);
            setExitButtonClickListener(null, null);
        }
    }

    public void onCameraOpen() {
        if (this.mVisible) {
            updateBackground();
            setVisibility(0);
        }
    }

    public void setExitButtonVisible(int visibility) {
        if (this.mExitButton != null) {
            this.mExitButton.setAlpha(1.0f);
            this.mExitButton.setVisibility(visibility);
            if (8 == visibility) {
                Util.expandViewTouchDelegate(this.mExitButton);
            }
        }
    }

    public void onResume() {
    }

    public void onPause() {
        this.mExitButton.setExpandedAnimation(false);
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
        this.mExitButton.setEnabled(enabled);
    }

    public void setMessageDispacher(MessageDispacher p) {
    }

    public void hide() {
        if (this.mVisible) {
            this.mVisible = false;
            setVisibility(8);
        }
    }

    public void show() {
        if (!this.mVisible) {
            this.mVisible = true;
            setVisibility(0);
        }
    }

    public void updateBackground() {
        if (((ActivityBase) this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
            this.mExitButton.setBackgroundResource(R.drawable.btn_camera_mode_exit_full_screen);
        } else {
            this.mExitButton.setBackgroundResource(R.drawable.btn_camera_mode_exit);
        }
    }

    public void setLayoutParameters(int aboveId, int margin) {
        LayoutParams lp = (LayoutParams) getLayoutParams();
        if (aboveId != 0) {
            lp.addRule(2, aboveId);
        } else {
            lp.removeRule(2);
        }
        lp.bottomMargin = margin;
        if (margin != 0) {
            lp.addRule(12, -1);
        } else {
            lp.removeRule(12);
        }
    }

    public ExitButton getExitButton() {
        return this.mExitButton;
    }
}
