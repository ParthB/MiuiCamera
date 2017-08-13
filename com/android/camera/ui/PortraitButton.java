package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;
import miui.view.animation.CubicEaseOutInterpolator;

public class PortraitButton extends ImageView implements V6FunctionUI, OnClickListener, OnOtherPopupShowedListener, MutexView {
    private boolean mAnimatorInitialized;
    private ScaleDrawable mBackgroundDrawable;
    private ObjectAnimator mHintHideAnimator;
    private AnimatorListenerAdapter mHintHideAnimatorListener = new AnimatorListenerAdapter() {
        public void onAnimationEnd(Animator animation) {
            PortraitButton.this.mHintTextView.setVisibility(8);
        }
    };
    private ObjectAnimator mHintShowAnimator;
    private TextView mHintTextView;
    private MessageDispacher mMessageDispacher;
    private ScaleDrawable mPortraitDrawable;
    private IconListPreference mPreference;
    private AnimatorSet mSwitchOnAnimator;

    public PortraitButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
    }

    private void startTransition() {
        this.mSwitchOnAnimator.start();
    }

    private void reverseTransition() {
        this.mSwitchOnAnimator.reverse();
    }

    private void resetTransition() {
        this.mPortraitDrawable.setLevel(10000);
        this.mBackgroundDrawable.setAlpha(0);
        this.mBackgroundDrawable.setLevel(8000);
    }

    public void onClick(View v) {
        if (isActivated()) {
            setActivated(false);
            reverseTransition();
        } else {
            setActivated(true);
            startTransition();
            CameraDataAnalytics.instance().trackEvent("pref_camera_portrait_mode_key");
        }
        updatePreference();
        requestPortraitModeChange();
    }

    public boolean isVisible() {
        if (!CameraSettings.isSupportedPortrait() || !V6ModulePicker.isCameraModule() || CameraSettingPreferences.instance().isFrontCamera() || (((ActivityBase) this.mContext).getCurrentModule().isCaptureIntent() && !Util.isPortraitIntent((ActivityBase) this.mContext))) {
            return false;
        }
        return CameraSettings.isNoCameraModeSelected(this.mContext);
    }

    private boolean isSettingsStatusBarShown() {
        if (((ActivityBase) this.mContext).getUIController().getSettingsStatusBar().isSubViewShown()) {
            return ((ActivityBase) this.mContext).getUIController().getSettingsStatusBar().isShown();
        }
        return false;
    }

    public void updateVisible() {
        if (!isVisible() || isSettingsStatusBarShown()) {
            setVisibility(8);
            this.mHintTextView.setVisibility(8);
            return;
        }
        setVisibility(0);
    }

    private void requestPortraitModeChange() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.portrait_switch_image, 2, "pref_camera_portrait_mode_key", null);
        }
    }

    private void initialize() {
        if (this.mPreference == null) {
            this.mPreference = (IconListPreference) ((ActivityBase) this.mContext).getUIController().getPreferenceGroup().findPreference("pref_camera_portrait_mode_key");
        }
        if (!this.mAnimatorInitialized) {
            initializeAnimator();
            this.mAnimatorInitialized = true;
        }
    }

    private void initializeAnimator() {
        this.mHintTextView = ((ActivityBase) this.mContext).getUIController().getPortraitHintTextView();
        LayerDrawable drawable = (LayerDrawable) getDrawable();
        this.mPortraitDrawable = (ScaleDrawable) drawable.findDrawableByLayerId(R.id.ic_portrait_button_foreground);
        this.mBackgroundDrawable = (ScaleDrawable) drawable.findDrawableByLayerId(R.id.ic_portrait_button_background);
        resetTransition();
        ObjectAnimator foregroundZoomOutAnimator = ObjectAnimator.ofInt(this.mPortraitDrawable, "level", new int[]{10000, 7800});
        ObjectAnimator backgroundFadeInAnimator = ObjectAnimator.ofInt(this.mBackgroundDrawable, "alpha", new int[]{0, 255});
        ObjectAnimator backgroundZoomInAnimator = ObjectAnimator.ofInt(this.mBackgroundDrawable, "level", new int[]{8000, 10000});
        this.mSwitchOnAnimator = new AnimatorSet();
        this.mSwitchOnAnimator.playTogether(new Animator[]{foregroundZoomOutAnimator, backgroundFadeInAnimator, backgroundZoomInAnimator});
        this.mSwitchOnAnimator.setInterpolator(new CubicEaseOutInterpolator());
        this.mSwitchOnAnimator.setDuration(350);
        this.mHintShowAnimator = ObjectAnimator.ofFloat(this.mHintTextView, "alpha", new float[]{0.0f, 1.0f});
        this.mHintShowAnimator.setInterpolator(new CubicEaseOutInterpolator());
        this.mHintShowAnimator.setDuration(100);
        this.mHintHideAnimator = ObjectAnimator.ofFloat(this.mHintTextView, "alpha", new float[]{1.0f, 0.0f});
        this.mHintHideAnimator.setInterpolator(new CubicEaseOutInterpolator());
        this.mHintHideAnimator.addListener(this.mHintHideAnimatorListener);
        this.mHintHideAnimator.setDuration(100);
    }

    public void switchOff(boolean animation) {
        if (isActivated()) {
            setActivated(false);
            if (animation) {
                reverseTransition();
            } else {
                resetTransition();
            }
            updatePreference();
            requestPortraitModeChange();
        }
    }

    public void show() {
        if (isVisible() && !isSettingsStatusBarShown()) {
            setVisibility(0);
        }
    }

    public void hide() {
        setVisibility(8);
        this.mHintTextView.setVisibility(8);
    }

    public void switchOff() {
        switchOff(true);
    }

    public void showHintText() {
        this.mHintTextView.setVisibility(0);
        this.mHintShowAnimator.start();
    }

    public void hideHintText() {
        this.mHintHideAnimator.start();
    }

    public boolean isHintTextShown() {
        return this.mHintTextView.isShown();
    }

    public void updatePreference() {
        if (this.mPreference != null) {
            if (isActivated()) {
                this.mPreference.setValue(this.mContext.getString(R.string.pref_camera_setting_switch_entryvalue_on));
            } else {
                this.mPreference.setValue(this.mContext.getString(R.string.pref_camera_setting_switch_entryvalue_off));
            }
            setContentDescription(this.mPreference.getEntry());
        }
    }

    public void reloadPreference() {
        if (this.mPreference != null) {
            if (this.mPreference.getValue().equals(this.mContext.getString(R.string.pref_camera_setting_switch_entryvalue_on))) {
                if (!isActivated()) {
                    setActivated(true);
                    startTransition();
                }
            } else if (isActivated()) {
                setActivated(false);
                reverseTransition();
            }
        }
        updatePreference();
    }

    public void onCreate() {
        initialize();
        updateVisible();
    }

    public void onCameraOpen() {
        initialize();
        reloadPreference();
        updateVisible();
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public boolean onOtherPopupShowed(int level) {
        return false;
    }

    public void recoverIfNeeded() {
    }
}
