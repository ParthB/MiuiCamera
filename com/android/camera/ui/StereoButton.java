package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import com.android.camera.ActivityBase;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;

public class StereoButton extends ImageView implements V6FunctionUI, OnClickListener, MessageDispacher, OnOtherPopupShowedListener, AnimationListener {
    private ObjectAnimator mAnimator;
    private CustomAnimatorListener mAnimatorListener;
    private ExitButton mExitButton;
    private V6ModeExitView mExitView;
    private boolean mIsShowing;
    private MessageDispacher mMessageDispacher;
    private int mModeExitButtonCenterX;
    private int mModeExitButtonHalfWidth;
    private int mModeExitButtonPadding;
    private StereoPopup mPopup;
    private IconListPreference mPreference;
    private Animation mRotateImageAnim;
    private Animation mSlideDownAnim;
    private Animation mSlideUpAnim;
    private TransitionDrawable mTransitionDrawable = ((TransitionDrawable) getDrawable());

    private enum AnimationType {
        COLLAPSE,
        EXPAND
    }

    private class CustomAnimatorListener extends AnimatorListenerAdapter implements AnimatorUpdateListener {
        private AnimationType mAnimationType;

        public CustomAnimatorListener(StereoButton this$0) {
            this(AnimationType.COLLAPSE);
        }

        public CustomAnimatorListener(AnimationType type) {
            this.mAnimationType = type;
            updateParameters();
        }

        public void setAnimationType(AnimationType type) {
            this.mAnimationType = type;
        }

        public void onAnimationEnd(Animator animation) {
            StereoButton.this.mExitButton.setExpandedAnimation(false);
            if (this.mAnimationType == AnimationType.COLLAPSE) {
                StereoButton.this.mExitView.setExitButtonVisible(8);
            } else {
                StereoButton.this.mIsShowing = false;
            }
        }

        public void onAnimationUpdate(ValueAnimator animation) {
        }

        public void updateParameters() {
            int textWidth = (int) StereoButton.this.mExitButton.getPaint().measureText(StereoButton.this.mExitButton.getText(), 0, StereoButton.this.mExitButton.getText().length());
            StereoButton.this.mModeExitButtonCenterX = Util.sWindowWidth / 2;
            StereoButton.this.mModeExitButtonHalfWidth = textWidth / 2;
            StereoButton.this.mModeExitButtonPadding = StereoButton.this.mExitButton.getPaddingLeft();
        }
    }

    public StereoButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
        this.mRotateImageAnim = AnimationUtils.loadAnimation(context, R.anim.rotate_image);
        this.mRotateImageAnim.setAnimationListener(this);
        this.mSlideUpAnim = AnimationUtils.loadAnimation(context, R.anim.slide_up);
        this.mSlideUpAnim.setAnimationListener(this);
        this.mSlideDownAnim = AnimationUtils.loadAnimation(context, R.anim.slide_down);
        this.mSlideDownAnim.setAnimationListener(this);
    }

    public void onClick(View v) {
        if (v == this && CameraSettings.isSwitchOn(this.mPreference.getKey())) {
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().simplifyPopup(false, false);
            animateShow();
            return;
        }
        setStereoValue(!CameraSettings.isSwitchOn(this.mPreference.getKey()), true, true);
    }

    public void switchOffStereo(boolean notify) {
        setStereoValue(false, notify, false);
    }

    public void setStereoValue(boolean stereoValue, boolean notify, boolean animate) {
        if (CameraSettings.isSwitchOn(this.mPreference.getKey()) != stereoValue) {
            if (stereoValue) {
                CameraDataAnalytics.instance().trackEvent(this.mPreference.getKey());
                this.mPreference.setValue(this.mContext.getString(R.string.pref_camera_setting_switch_entryvalue_on));
            } else {
                this.mPreference.setValue(this.mPreference.findSupportedDefaultValue());
            }
            if (!stereoValue) {
                this.mRotateImageAnim.cancel();
                PopupManager.getInstance(getContext()).clearRecoveredPopupListenerIfNeeded(this);
                dismissPopup(animate);
            } else if (animate) {
                animateShow();
            } else {
                showPopup(animate);
            }
            updateExitButton(animate);
            if (notify && this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(0, R.id.stereo_switch_image, 2, null, null);
            }
        }
    }

    private void animateShow() {
        this.mIsShowing = true;
        if (isActivated()) {
            showPopup(true);
        } else {
            startAnimation(this.mRotateImageAnim);
        }
        PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
    }

    private void doWithPopup(boolean animate, boolean shown) {
        if (!shown) {
            startAnimation(this.mSlideDownAnim);
        } else if (animate) {
            startAnimation(this.mSlideUpAnim);
        } else {
            setVisibility(8);
            showScale(false);
        }
    }

    private void initializePopup() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            Log.i("StereoButton", "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
        } else if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        } else {
            ViewGroup root = ((ActivityBase) this.mContext).getUIController().getPopupParent();
            this.mPopup = (StereoPopup) SettingPopupFactory.createSettingPopup(this.mPreference.getKey(), root, getContext());
            this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
            root.addView(this.mPopup);
        }
    }

    private String getKey() {
        if (this.mPreference != null) {
            return this.mPreference.getKey();
        }
        return null;
    }

    public void showPopup(boolean animate) {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.show(animate);
            doWithPopup(animate, true);
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().onPopupChange();
        }
    }

    public boolean dismissPopup(boolean animate) {
        this.mIsShowing = false;
        if (!isPopupVisible()) {
            return false;
        }
        this.mPopup.dismiss(animate);
        dismissScale(animate);
        if (!animate) {
            updateVisible();
        }
        return true;
    }

    public boolean isPopupVisible() {
        return this.mPopup != null && this.mPopup.getVisibility() == 0;
    }

    public View getPopup() {
        return this.mPopup;
    }

    public void onCameraOpen() {
        if (!Device.isSupportedStereo() || CameraSettingPreferences.instance().isFrontCamera()) {
            setVisibility(8);
            return;
        }
        if (this.mPreference == null) {
            this.mPreference = (IconListPreference) ((ActivityBase) this.mContext).getUIController().getPreferenceGroup().findPreference("pref_camera_stereo_mode_key");
        }
        if (this.mPreference == null) {
            setVisibility(8);
            return;
        }
        PopupManager.getInstance(this.mContext).setOnOtherPopupShowedListener(this);
        if (!this.mIsShowing) {
            if (!CameraSettings.isSwitchOn(this.mPreference.getKey())) {
                updateVisible();
                dismissPopup(false);
            } else if (!isPopupVisible()) {
                setVisibility(8);
                showPopup(false);
            }
            updateExitButton(false);
        }
        if (this.mPopup != null) {
            this.mPopup.updateBackground();
        }
    }

    private void updateActivated() {
        if (!CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || isPopupVisible()) {
            setActivated(false);
            return;
        }
        setActivated(true);
        setImageDrawable(this.mTransitionDrawable);
    }

    public void updateVisible() {
        if (Device.isSupportedStereo() && CameraSettingPreferences.instance().isBackCamera() && CameraSettings.isNoCameraModeSelected(this.mContext) && !((ActivityBase) this.mContext).getCurrentModule().isCaptureIntent() && !CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            setActivated(false);
            setVisibility(0);
        } else if (!CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") || isPopupVisible()) {
            setActivated(false);
            setVisibility(8);
        } else {
            setVisibility(0);
        }
    }

    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (!activated) {
            this.mTransitionDrawable.resetTransition();
        }
    }

    private void updateExitButton(boolean animate) {
        int txtId = CameraSettings.getExitText(this.mPreference.getKey());
        if (txtId == -1) {
            return;
        }
        if (CameraSettings.isSwitchOn(this.mPreference.getKey())) {
            if (animate) {
                this.mExitView.setExitContent(txtId);
            } else {
                this.mExitView.updateExitButton(txtId, true);
            }
            this.mExitView.setExitButtonClickListener(this, this.mPreference.getKey());
        } else if (this.mExitView.isCurrentExitView(this.mPreference.getKey())) {
            if (!animate) {
                this.mExitView.updateExitButton(txtId, false);
            }
            this.mExitView.setExitButtonClickListener(null, null);
        }
    }

    public void onCreate() {
        this.mExitView = ((ActivityBase) this.mContext).getUIController().getModeExitView();
        if (this.mExitView != null) {
            this.mExitButton = this.mExitView.getExitButton();
        }
    }

    public void onResume() {
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onPause() {
        this.mIsShowing = false;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mPopup != null) {
            this.mPopup.setEnabled(enabled);
        }
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        switch (what) {
            case 11:
                updateActivated();
                break;
            case 12:
                if (!CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                    updateActivated();
                }
                post(new Runnable() {
                    public void run() {
                        StereoButton.this.updateVisible();
                        StereoButton.this.updateActivated();
                        if (StereoButton.this.isActivated()) {
                            StereoButton.this.mTransitionDrawable.startTransition(200);
                        }
                        StereoButton.this.animate().rotationBy(60.0f).start();
                    }
                });
                break;
            default:
                if (this.mMessageDispacher != null) {
                    this.mMessageDispacher.dispacherMessage(what, R.id.stereo_switch_image, 2, extra1, extra2);
                }
                reloadPreference();
                break;
        }
        return false;
    }

    public boolean onOtherPopupShowed(int level) {
        boolean result = dismissPopup(false);
        updateActivated();
        return result;
    }

    public void recoverIfNeeded() {
    }

    private void reloadPreference() {
        ((ActivityBase) this.mContext).getUIController().getSettingsStatusBar().updateAperture();
        if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        }
    }

    public void onAnimationStart(Animation animation) {
    }

    public void onAnimationEnd(Animation animation) {
        if (this.mRotateImageAnim == animation && this.mIsShowing) {
            showPopup(true);
        } else if (this.mSlideUpAnim == animation && this.mIsShowing) {
            setVisibility(8);
            showScale(true);
        }
    }

    public void onAnimationRepeat(Animation animation) {
    }

    public void showScale(boolean animate) {
        if (!this.mExitView.isExitButtonShown()) {
            if (animate) {
                createExpandAnimator(true);
            }
            this.mExitView.setExitButtonVisible(0);
        }
    }

    public void dismissScale(boolean animate) {
        if (this.mExitView.isExitButtonShown()) {
            if (animate) {
                this.mExitView.animate().setDuration(200).translationYBy((float) getContext().getResources().getDimensionPixelSize(R.dimen.manual_popup_layout_height)).withEndAction(new Runnable() {
                    public void run() {
                        StereoButton.this.mExitView.setTranslationY(0.0f);
                    }
                }).start();
                createExpandAnimator(false);
            } else {
                this.mExitView.setExitButtonVisible(8);
            }
        }
    }

    private void createExpandAnimator(boolean show) {
        if (this.mAnimatorListener == null) {
            this.mAnimatorListener = new CustomAnimatorListener(this);
        } else {
            this.mAnimatorListener.updateParameters();
        }
        if (this.mAnimator == null) {
            this.mAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(this.mContext, R.anim.exit_button_expand);
            this.mAnimator.setTarget(this);
            this.mAnimator.addListener(this.mAnimatorListener);
        }
        this.mAnimator.setIntValues(new int[]{0, this.mModeExitButtonHalfWidth});
        if (show) {
            this.mAnimatorListener.setAnimationType(AnimationType.EXPAND);
            this.mAnimator.start();
        } else {
            this.mAnimatorListener.setAnimationType(AnimationType.COLLAPSE);
            this.mAnimator.reverse();
        }
        this.mExitButton.setExpandedAnimation(true);
    }

    public void setDeltaX(int deltaX) {
        this.mExitButton.setExpandingSize((this.mModeExitButtonCenterX - this.mModeExitButtonPadding) - deltaX, (this.mModeExitButtonCenterX + this.mModeExitButtonPadding) + deltaX);
        this.mExitButton.postInvalidateOnAnimation();
    }
}
