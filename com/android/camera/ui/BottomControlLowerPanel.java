package com.android.camera.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import com.android.camera.Device;
import com.android.camera.Thumbnail;
import com.android.camera.Util;

public class BottomControlLowerPanel extends V6RelativeLayout implements AnimationListener {
    private static final int DURATION = (Device.IS_CM_TEST ? 200 : 300);
    private boolean mControlVisible;
    private Runnable mModuleAnimationCallback;
    public V6ModulePicker mModulePicker;
    private AnimationSet mModulePickerSwitchIn;
    public V6PauseRecordingButton mPauseRecordingButton;
    public View mProgressBar;
    public V6ShutterButton mShutterButton;
    private AnimationSet mShutterButtonSwitchIn;
    public V6ThumbnailButton mThumbnailButton;
    private boolean mToVideo;
    public V6VideoCaptureButton mVideoCaptureButton;

    public BottomControlLowerPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onCameraOpen() {
        boolean z = false;
        super.onCameraOpen();
        if (getVisibility() == 0) {
            z = true;
        }
        this.mControlVisible = z;
    }

    public void onResume() {
        super.onResume();
        setVisibility(0);
        this.mThumbnailButton.setVisibility(0);
        this.mShutterButton.setVisibility(0);
        this.mModulePicker.setVisibility(0);
        this.mPauseRecordingButton.setVisibility(8);
        this.mVideoCaptureButton.setVisibility(8);
        if (this.mToVideo != V6ModulePicker.isVideoModule()) {
            setButton(V6ModulePicker.isVideoModule());
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mThumbnailButton = (V6ThumbnailButton) findChildrenById(R.id.v6_thumbnail_button);
        this.mShutterButton = (V6ShutterButton) findChildrenById(R.id.v6_shutter_button);
        this.mModulePicker = (V6ModulePicker) findChildrenById(R.id.v6_module_picker);
        this.mProgressBar = findViewById(R.id.v6_progress_capture);
        this.mPauseRecordingButton = (V6PauseRecordingButton) findChildrenById(R.id.v6_video_pause_button);
        this.mVideoCaptureButton = (V6VideoCaptureButton) findChildrenById(R.id.v6_video_capture_button);
        int thumbnailButtonWidth = this.mThumbnailButton.mImage.getDrawable().getIntrinsicWidth();
        int shutterButtonWidth = this.mShutterButton.getDrawable().getIntrinsicWidth();
        int modulePickerWidth = this.mModulePicker.getDrawable().getIntrinsicWidth();
        int padding = this.mContext.getResources().getDimensionPixelSize(R.dimen.bottom_control_lower_panel_padding_width) + this.mContext.getResources().getDimensionPixelSize(R.dimen.normal_view_expanded_space);
        LayoutParams layoutParams = this.mProgressBar.getLayoutParams();
        int dimensionPixelSize = thumbnailButtonWidth - this.mContext.getResources().getDimensionPixelSize(R.dimen.zoom_button_background_stroke_width);
        layoutParams.width = dimensionPixelSize;
        layoutParams.height = dimensionPixelSize;
        LayoutParams coverParams = findChildrenById(R.id.v6_thumbnail_image_cover).getLayoutParams();
        coverParams.width = thumbnailButtonWidth;
        coverParams.height = thumbnailButtonWidth;
        this.mProgressBar.setLayoutParams(layoutParams);
        initShutterButtonSwitchAnimation((float) thumbnailButtonWidth, (float) shutterButtonWidth, (float) modulePickerWidth, (float) padding);
        initModulePickerSwitchAnimation((float) thumbnailButtonWidth, (float) shutterButtonWidth, (float) modulePickerWidth, (float) padding);
    }

    private void initShutterButtonSwitchAnimation(float thumbnailButtonWidth, float shutterButtonWidth, float modulePickerWidth, float padding) {
        this.mShutterButtonSwitchIn = new AnimationSet(true);
        this.mShutterButtonSwitchIn.setDuration((long) DURATION);
        this.mShutterButtonSwitchIn.setInterpolator(new DecelerateInterpolator());
        this.mShutterButtonSwitchIn.setAnimationListener(this);
        this.mShutterButtonSwitchIn.addAnimation(new ScaleAnimation(modulePickerWidth / shutterButtonWidth, 1.0f, modulePickerWidth / shutterButtonWidth, 1.0f, 1, 0.5f, 1, 0.5f));
        this.mShutterButtonSwitchIn.addAnimation(new TranslateAnimation(0, (((float) (Util.sWindowWidth / 2)) - padding) - (thumbnailButtonWidth / 2.0f), 1, 0.0f, 1, 0.0f, 1, 0.0f));
    }

    private void initModulePickerSwitchAnimation(float thumbnailButtonWidth, float shutterButtonWidth, float modulePickerWidth, float padding) {
        this.mModulePickerSwitchIn = new AnimationSet(true);
        this.mModulePickerSwitchIn.setDuration((long) DURATION);
        this.mModulePickerSwitchIn.setInterpolator(new DecelerateInterpolator());
        this.mModulePickerSwitchIn.addAnimation(new ScaleAnimation(shutterButtonWidth / modulePickerWidth, 1.0f, shutterButtonWidth / modulePickerWidth, 1.0f, 1, 0.5f, 1, 0.5f));
        this.mModulePickerSwitchIn.addAnimation(new TranslateAnimation(0, -((((float) (Util.sWindowWidth / 2)) - padding) - (thumbnailButtonWidth / 2.0f)), 1, 0.0f, 1, 0.0f, 1, 0.0f));
    }

    public void animationSwitchToVideo(Runnable callback) {
        animateSwitch(callback, true);
    }

    public void animationSwitchToCamera(Runnable callback) {
        animateSwitch(callback, false);
    }

    private TransitionDrawable initModulePickTransView(boolean toVideo) {
        Drawable[] mImages = new Drawable[2];
        if (toVideo) {
            mImages[0] = this.mContext.getResources().getDrawable(R.drawable.ic_camera_shutter_button_small);
            mImages[1] = this.mContext.getResources().getDrawable(R.drawable.video_module_picker_bg);
            this.mModulePicker.setContentDescription(getResources().getString(R.string.accessibility_camera_module_picker));
        } else {
            mImages[0] = this.mContext.getResources().getDrawable(R.drawable.ic_video_shutter_button_small);
            mImages[1] = this.mContext.getResources().getDrawable(R.drawable.camera_module_picker_bg);
            this.mModulePicker.setContentDescription(getResources().getString(R.string.accessibility_video_module_picker));
        }
        TransitionDrawable modulePickTransition = new TransitionDrawable(mImages);
        modulePickTransition.setCrossFadeEnabled(true);
        return modulePickTransition;
    }

    private TransitionDrawable initShutterTransView(boolean toVideo) {
        Drawable[] mImages = new Drawable[2];
        if (toVideo) {
            mImages[0] = this.mContext.getResources().getDrawable(R.drawable.camera_module_picker_bg);
            mImages[1] = this.mContext.getResources().getDrawable(R.drawable.video_shutter_button_start_bg);
        } else {
            mImages[0] = this.mContext.getResources().getDrawable(R.drawable.video_module_picker_bg);
            mImages[1] = this.mContext.getResources().getDrawable(R.drawable.camera_shutter_button_bg);
        }
        TransitionDrawable shutterTransition = new TransitionDrawable(mImages);
        shutterTransition.setCrossFadeEnabled(true);
        return shutterTransition;
    }

    public void animateSwitch(Runnable callback, boolean toVideo) {
        this.mToVideo = toVideo;
        setVisibility(0);
        clearLastAnimation();
        TransitionDrawable moduleTransition = initModulePickTransView(toVideo);
        TransitionDrawable shutterTransition = initShutterTransView(toVideo);
        this.mModuleAnimationCallback = callback;
        this.mModulePicker.setImageDrawable(moduleTransition);
        moduleTransition.startTransition(DURATION - 50);
        this.mShutterButton.setImageDrawable(shutterTransition);
        shutterTransition.startTransition(DURATION - 50);
        this.mShutterButton.startAnimation(this.mShutterButtonSwitchIn);
        this.mModulePicker.startAnimation(this.mModulePickerSwitchIn);
    }

    private void setButton(boolean toVideo) {
        this.mToVideo = toVideo;
        setVisibility(0);
        clearLastAnimation();
        if (toVideo) {
            this.mModulePicker.setImageResource(R.drawable.video_module_picker_bg);
            this.mModulePicker.setContentDescription(getResources().getString(R.string.accessibility_camera_module_picker));
            this.mShutterButton.setImageResource(R.drawable.video_shutter_button_start_bg);
            return;
        }
        this.mModulePicker.setImageResource(R.drawable.camera_module_picker_bg);
        this.mModulePicker.setContentDescription(getResources().getString(R.string.accessibility_video_module_picker));
        this.mShutterButton.setImageResource(R.drawable.camera_shutter_button_bg);
    }

    private void clearLastAnimation() {
        if (!this.mShutterButtonSwitchIn.hasEnded()) {
            this.mShutterButton.clearAnimation();
            this.mModulePicker.clearAnimation();
            if (this.mModuleAnimationCallback != null) {
                this.mModuleAnimationCallback.run();
                this.mModuleAnimationCallback = null;
            }
        }
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
        if (this.mShutterButtonSwitchIn == animation && this.mModuleAnimationCallback != null) {
            post(this.mModuleAnimationCallback);
            this.mModuleAnimationCallback = null;
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public V6ShutterButton getShutterButton() {
        return this.mShutterButton;
    }

    public V6ThumbnailButton getThumbnailButton() {
        return this.mThumbnailButton;
    }

    public V6ModulePicker getModulePicker() {
        return this.mModulePicker;
    }

    public V6VideoCaptureButton getVideoCaptureButton() {
        return this.mVideoCaptureButton;
    }

    public V6PauseRecordingButton getVideoPauseButton() {
        return this.mPauseRecordingButton;
    }

    public View getProgressBar() {
        return this.mProgressBar;
    }

    public void updateThumbnailView(Thumbnail t, boolean needAnimation) {
        this.mThumbnailButton.updateThumbnail(t, needAnimation, new Runnable() {
            public void run() {
                BottomControlLowerPanel.this.mProgressBar.setVisibility(8);
            }
        });
    }
}
