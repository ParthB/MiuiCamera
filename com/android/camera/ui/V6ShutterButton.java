package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;

public class V6ShutterButton extends V6BottomAnimationViewGroup {
    private V6ShutterButtonAudioSound mAudioSound;
    private V6ShutterButtonInternal mShutterButton;

    public V6ShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mShutterButton = (V6ShutterButtonInternal) findChildrenById(R.id.v6_shutter_button_internal);
        this.mAudioSound = (V6ShutterButtonAudioSound) findChildrenById(R.id.v6_shutter_button_audio_sound);
        if (this.mAudioSound != null) {
            this.mAudioSound.setRadius(this.mShutterButton.getDrawable().getIntrinsicWidth() / 2, getResources().getDimensionPixelSize(R.dimen.bottom_control_lower_panel_height) / 2);
        }
    }

    public void onResume() {
        super.onResume();
        setVisibility(0);
    }

    public void setAudioProgress(float progress) {
        if (this.mAudioSound != null) {
            this.mAudioSound.setAudioProgress(progress);
        }
    }

    public void setEnabled(boolean enabled) {
        this.mShutterButton.setEnabled(enabled);
    }

    public Drawable getDrawable() {
        return this.mShutterButton.getDrawable();
    }

    public void setImageResource(int resId) {
        this.mShutterButton.setImageResource(resId);
    }

    public void setImageDrawable(Drawable drawable) {
        this.mShutterButton.setImageDrawable(drawable);
    }

    public void changeImageWithAnimation(int resId, long duration) {
        this.mShutterButton.changeImageWithAnimation(resId, duration);
    }

    public boolean isCanceled() {
        return this.mShutterButton.isCanceled();
    }

    public boolean isEnabled() {
        return this.mShutterButton.isEnabled();
    }

    public boolean isPressed() {
        return this.mShutterButton.isPressed();
    }

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }
}
