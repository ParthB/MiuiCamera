package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.text.SpannableStringBuilder;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.CameraManager;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;
import miui.view.animation.BackEaseOutInterpolator;
import miui.view.animation.CubicEaseInOutInterpolator;
import miui.view.animation.CubicEaseOutInterpolator;
import miui.view.animation.QuadraticEaseInOutInterpolator;
import miui.view.animation.QuadraticEaseOutInterpolator;
import miui.view.animation.SineEaseOutInterpolator;

public class ZoomButton extends TextView implements V6FunctionUI, MessageDispacher, OnOtherPopupShowedListener, OnClickListener, OnLongClickListener, MutexView {
    private Interpolator mBackEaseOutInterpolator = new BackEaseOutInterpolator();
    private int mBottomMargin;
    private AnimatorSet mButtonSlideDownAnimator;
    private ObjectAnimator mButtonSlideUpAnimator;
    private Interpolator mCubicEaseInOutInterpolator = new CubicEaseInOutInterpolator();
    private Interpolator mCubicEaseOutInterpolator = new CubicEaseOutInterpolator();
    private TextAppearanceSpan mDigitsTextStyle;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ZoomButton.this.dismissPopup();
                    return;
                default:
                    return;
            }
        }
    };
    private int mLayoutLocationStatus;
    private MessageDispacher mMessageDispacher;
    private ZoomPopup mPopup;
    private int mPopupHeight;
    private ObjectAnimator mPopupSlideDownAnimator;
    private ObjectAnimator mPopupSlideUpAnimator;
    private IconListPreference mPreference;
    private Interpolator mQuadraticEaseInOutInterpolator = new QuadraticEaseInOutInterpolator();
    private Interpolator mQuadraticEaseOutInterpolator = new QuadraticEaseOutInterpolator();
    private Animation mShowAnimation;
    private Interpolator mSineEaseOutInterpolator = new SineEaseOutInterpolator();
    private AnimatorListener mSlideDownAnimatorListener = new AnimatorListenerAdapter() {
        private float mButtonOriginalTranslationY;
        private boolean mButtonSlideDownAnimatorRunning;
        private float mPopupOriginalTranslationY;
        private boolean mPopupSlideDownAnimatorRunning;

        public void onAnimationStart(Animator animation) {
            if (animation == ZoomButton.this.mPopupSlideDownAnimator) {
                this.mPopupSlideDownAnimatorRunning = true;
                this.mPopupOriginalTranslationY = ZoomButton.this.mPopup.getTranslationY();
            } else if (animation == ZoomButton.this.mButtonSlideDownAnimator) {
                this.mButtonSlideDownAnimatorRunning = true;
                this.mButtonOriginalTranslationY = ZoomButton.this.getTranslationY();
            }
        }

        public void onAnimationEnd(Animator animation) {
            if (animation == ZoomButton.this.mPopupSlideDownAnimator) {
                this.mPopupSlideDownAnimatorRunning = false;
            } else if (animation == ZoomButton.this.mButtonSlideDownAnimator) {
                this.mButtonSlideDownAnimatorRunning = false;
            }
            if (!this.mPopupSlideDownAnimatorRunning && !this.mButtonSlideDownAnimatorRunning) {
                ZoomButton.this.mPopup.setVisibility(8);
                ZoomButton.this.mPopup.setTranslationY(this.mPopupOriginalTranslationY);
                ZoomButton.this.setTranslationY(this.mButtonOriginalTranslationY);
            }
        }
    };
    private float mTouchDownEventOriginX;
    private boolean mTouchDownEventPassed;
    private TextAppearanceSpan mXTextStyle;
    private AnimatorSet mZoomInAnimator;
    private AnimatorSet mZoomInOutAnimator;
    private AnimatorSet mZoomOutAnimator;
    private boolean mZoomPopupAdjusting;
    private OnTouchListener mZoomPopupTouchListener = new OnTouchListener() {
        private boolean mAnimated = false;

        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == 2) {
                if (!this.mAnimated) {
                    ZoomButton.this.mZoomInAnimator.start();
                    this.mAnimated = true;
                }
            } else if ((event.getAction() == 1 || event.getAction() == 3) && this.mAnimated) {
                ZoomButton.this.mZoomOutAnimator.start();
                this.mAnimated = false;
            }
            ZoomButton.this.sendHideMessage();
            return false;
        }
    };
    private int mZoomRatio;
    private int mZoomRatioTele;
    private int mZoomRatioWide;
    private ObjectAnimator mZoomRequestAnimator;

    public ZoomButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        PopupManager.getInstance(context).setOnOtherPopupShowedListener(this);
        setOnClickListener(this);
        setOnLongClickListener(this);
        GradientDrawable backgroundDrawable = (GradientDrawable) ((LayerDrawable) ((InsetDrawable) getBackground()).getDrawable()).findDrawableByLayerId(R.id.ic_zoom_button_background);
        this.mDigitsTextStyle = new TextAppearanceSpan(context, R.style.ZoomButtonDigitsTextStyle);
        this.mXTextStyle = new TextAppearanceSpan(context, R.style.ZoomButtonXTextStyle);
        this.mZoomRatioWide = Integer.valueOf(context.getResources().getString(R.string.pref_camera_zoom_ratio_wide)).intValue();
        this.mZoomRatioTele = Integer.valueOf(context.getResources().getString(R.string.pref_camera_zoom_ratio_tele)).intValue();
        this.mPopupHeight = context.getResources().getDimensionPixelSize(R.dimen.zoom_popup_layout_height);
        this.mBottomMargin = context.getResources().getDimensionPixelSize(R.dimen.zoom_button_margin_bottom);
        this.mShowAnimation = AnimationUtils.loadAnimation(context, R.anim.show);
        this.mShowAnimation.setDuration(100);
        this.mZoomRequestAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.zoom_request);
        this.mZoomRequestAnimator.setTarget(this);
        this.mZoomInOutAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.anim.zoom_button_zoom_in_out);
        this.mZoomInOutAnimator.setTarget(this);
        this.mZoomInOutAnimator.setInterpolator(this.mQuadraticEaseOutInterpolator);
        this.mZoomInAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.anim.zoom_button_zoom_in);
        this.mZoomInAnimator.setTarget(this);
        this.mZoomInAnimator.setInterpolator(this.mQuadraticEaseInOutInterpolator);
        this.mZoomOutAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.anim.zoom_button_zoom_out);
        this.mZoomOutAnimator.setTarget(this);
        this.mZoomOutAnimator.setInterpolator(this.mQuadraticEaseInOutInterpolator);
        this.mButtonSlideUpAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.zoom_button_slide_up);
        this.mButtonSlideUpAnimator.setTarget(this);
        this.mButtonSlideUpAnimator.setFloatValues(new float[]{(float) this.mPopupHeight, 0.0f});
        this.mButtonSlideUpAnimator.setInterpolator(this.mBackEaseOutInterpolator);
        this.mPopupSlideUpAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.zoom_popup_slide_up);
        this.mPopupSlideUpAnimator.setFloatValues(new float[]{(float) this.mPopupHeight, 0.0f});
        this.mPopupSlideUpAnimator.setInterpolator(this.mCubicEaseOutInterpolator);
        this.mButtonSlideDownAnimator = (AnimatorSet) AnimatorInflater.loadAnimator(context, R.anim.zoom_button_slide_down);
        this.mButtonSlideDownAnimator.setTarget(this);
        ObjectAnimator buttonSlideDownPass1 = (ObjectAnimator) this.mButtonSlideDownAnimator.getChildAnimations().get(0);
        buttonSlideDownPass1.setFloatValues(new float[]{0.0f, ((float) this.mPopupHeight) + (((float) this.mPopupHeight) * 0.2f)});
        buttonSlideDownPass1.setInterpolator(this.mCubicEaseInOutInterpolator);
        ObjectAnimator buttonSlideDownPass2 = (ObjectAnimator) this.mButtonSlideDownAnimator.getChildAnimations().get(1);
        buttonSlideDownPass2.setFloatValues(new float[]{((float) this.mPopupHeight) + (((float) this.mPopupHeight) * 0.2f), (float) this.mPopupHeight});
        buttonSlideDownPass2.setInterpolator(this.mSineEaseOutInterpolator);
        this.mButtonSlideDownAnimator.addListener(this.mSlideDownAnimatorListener);
        this.mPopupSlideDownAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.zoom_popup_slide_down);
        this.mPopupSlideDownAnimator.setFloatValues(new float[]{0.0f, (float) this.mPopupHeight});
        this.mPopupSlideDownAnimator.setInterpolator(this.mCubicEaseInOutInterpolator);
        this.mPopupSlideDownAnimator.addListener(this.mSlideDownAnimatorListener);
    }

    public void reloadPreference() {
        if (this.mPreference != null && isVisible()) {
            if (!CameraSettings.isSwitchCameraZoomMode()) {
                int zoomIndex = CameraSettings.readZoom(CameraSettingPreferences.instance());
                Parameters parameters = CameraManager.instance().getStashParameters();
                if (parameters == null) {
                    this.mZoomRatio = this.mZoomRatioWide;
                } else {
                    this.mZoomRatio = ((Integer) parameters.getZoomRatios().get(zoomIndex)).intValue();
                }
            } else if (CameraSettings.getString(R.string.pref_camera_zoom_mode_entryvalue_wide).equals(this.mPreference.getValue())) {
                this.mZoomRatio = this.mZoomRatioWide;
            } else {
                this.mZoomRatio = this.mZoomRatioTele;
            }
            if (this.mZoomRequestAnimator.isRunning() && this.mZoomRatio != this.mZoomRatioWide) {
                if (this.mZoomRatio == this.mZoomRatioTele) {
                }
            }
            SpannableStringBuilder builder = new SpannableStringBuilder();
            int zoom = this.mZoomRatio / 10;
            int zoomSig = zoom / 10;
            int zoomFraction = zoom % 10;
            if (zoomFraction == 0) {
                builder.append(String.valueOf(zoomSig), this.mDigitsTextStyle, 33);
            } else {
                builder.append(zoomSig + "." + zoomFraction, this.mDigitsTextStyle, 33);
            }
            builder.append("X", this.mXTextStyle, 33);
            setText(builder);
        }
    }

    public void initialize() {
        if (this.mPreference == null) {
            this.mPreference = (IconListPreference) ((ActivityBase) this.mContext).getUIController().getPreferenceGroup().findPreference("pref_camera_zoom_mode_key");
        }
    }

    private void initializePopup() {
        if (this.mPreference == null) {
            Log.i("ZoomButton", "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
        } else if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        } else {
            ViewGroup root = ((ActivityBase) this.mContext).getUIController().getPopupParent();
            this.mPopup = (ZoomPopup) SettingPopupFactory.createSettingPopup(getKey(), root, getContext());
            this.mPopupSlideUpAnimator.setTarget(this.mPopup);
            this.mPopupSlideDownAnimator.setTarget(this.mPopup);
            this.mPopup.setOnTouchListener(this.mZoomPopupTouchListener);
            this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
            root.addView(this.mPopup);
        }
    }

    private void triggerPopup() {
        if (this.mPreference == null) {
            return;
        }
        if (isPopupShown()) {
            dismissPopup();
            return;
        }
        setPressed(true);
        showPopup();
        ((ActivityBase) this.mContext).getUIController().getPreviewPage().simplifyPopup(false, false);
        PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
        sendHideMessage();
    }

    public boolean onLongClick(View view) {
        if (isPopupShown() || CameraSettings.isSwitchCameraZoomMode()) {
            return false;
        }
        triggerPopup();
        this.mZoomPopupAdjusting = true;
        this.mTouchDownEventPassed = false;
        return true;
    }

    public void onClick(View view) {
        if (CameraSettings.isSwitchCameraZoomMode()) {
            toggle();
        } else if (this.mZoomRatio == this.mZoomRatioWide) {
            this.mZoomRequestAnimator.setIntValues(new int[]{this.mZoomRatio, this.mZoomRatioTele});
            this.mZoomRequestAnimator.start();
            notifyZoomAnimationStart();
        } else if (this.mZoomRatio <= this.mZoomRatioTele) {
            this.mZoomRequestAnimator.setIntValues(new int[]{this.mZoomRatio, this.mZoomRatioWide});
            this.mZoomRequestAnimator.start();
        } else {
            requestZoomRatio(this.mZoomRatioTele, true);
            requestZoomRatio(this.mZoomRatioWide, false);
        }
        this.mZoomInOutAnimator.start();
        dismissPopup();
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        if (ev.getAction() == 0) {
            this.mTouchDownEventOriginX = ev.getX();
            this.mZoomPopupAdjusting = false;
        } else if (this.mZoomPopupAdjusting) {
            float originX = ev.getX();
            ev.setLocation(((((float) Util.sWindowWidth) / 2.0f) + originX) - this.mTouchDownEventOriginX, ev.getY());
            if (ev.getAction() == 3 || ev.getAction() == 1) {
                this.mZoomPopupAdjusting = false;
                if (this.mPopup != null) {
                    this.mPopup.passTouchEvent(ev);
                }
            } else if (ev.getAction() == 2) {
                if (!this.mTouchDownEventPassed) {
                    this.mTouchDownEventOriginX = originX;
                    ev.setLocation(((float) Util.sWindowWidth) / 2.0f, ev.getY());
                    ev.setAction(0);
                    this.mPopup.passTouchEvent(ev);
                    this.mTouchDownEventPassed = true;
                    ev.setAction(2);
                }
                if (this.mPopup != null) {
                    this.mPopup.passTouchEvent(ev);
                }
            }
            ev.setLocation(originX, ev.getY());
        }
        return super.onTouchEvent(ev);
    }

    private void toggle() {
        if (this.mPreference != null) {
            int index = this.mPreference.findIndexOfValue(this.mPreference.getValue()) + 1;
            if (index >= getPreferenceSize()) {
                index = 0;
            }
            this.mPreference.setValueIndex(index);
            reloadPreference();
        }
        requestSwitchCamera();
    }

    private int getPreferenceSize() {
        CharSequence[] entries = this.mPreference.getEntryValues();
        return entries != null ? entries.length : 0;
    }

    private String getKey() {
        if (this.mPreference != null) {
            return this.mPreference.getKey();
        }
        return null;
    }

    private boolean isPopupShown() {
        return this.mPopup != null && this.mPopup.getVisibility() == 0;
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(0, false);
            this.mPopup.setVisibility(0);
            if (this.mLayoutLocationStatus == 0) {
                this.mPopupSlideUpAnimator.start();
                this.mButtonSlideUpAnimator.start();
            }
        }
    }

    public boolean dismissPopup() {
        return dismissPopup(true);
    }

    public boolean dismissPopup(boolean animation) {
        setPressed(false);
        if (this.mPopup == null || this.mPopup.getVisibility() != 0 || this.mPopupSlideDownAnimator.isRunning() || this.mButtonSlideDownAnimator.isRunning()) {
            return false;
        }
        if (animation && this.mLayoutLocationStatus == 0) {
            this.mPopupSlideDownAnimator.start();
            this.mButtonSlideDownAnimator.start();
        } else {
            this.mPopup.setVisibility(8);
        }
        PopupManager.getInstance(getContext()).notifyDismissPopup();
        return true;
    }

    public void onCreate() {
        initialize();
        updateVisible();
        if (!CameraSettings.isNoCameraModeSelected(this.mContext)) {
            setVisibility(8);
        }
    }

    public void onCameraOpen() {
        initialize();
        reloadPreference();
        updateVisible();
        if (this.mPopup != null) {
            this.mPopup.updateBackground();
        }
    }

    public void onResume() {
        reloadPreference();
    }

    public void onPause() {
        dismissPopup();
        this.mZoomRequestAnimator.cancel();
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mPopup != null) {
            this.mPopup.setEnabled(enabled);
        }
    }

    public void requestSwitchCamera() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(7, R.id.zoom_button, 2, getKey(), Boolean.valueOf(true));
        }
    }

    public void setZoomRatio(int ratio) {
        requestZoomRatio(ratio, false);
    }

    private void requestZoomRatio(int ratio) {
        requestZoomRatio(ratio, false);
    }

    private void requestZoomRatio(int ratio, boolean sync) {
        if (ratio != this.mZoomRatio && this.mMessageDispacher != null && isVisible()) {
            this.mMessageDispacher.dispacherMessage(7, R.id.zoom_popup, 2, Boolean.valueOf(sync), Integer.valueOf(ratio));
        }
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (this.mMessageDispacher == null) {
            return false;
        }
        requestZoomRatio(((Integer) extra2).intValue());
        return true;
    }

    private void sendHideMessage() {
        this.mHandler.removeMessages(1);
        this.mHandler.sendEmptyMessageDelayed(1, 5000);
    }

    public boolean onOtherPopupShowed(int level) {
        return dismissPopup(false);
    }

    public void recoverIfNeeded() {
    }

    private boolean isVisible() {
        if (!CameraSettings.isSupportedOpticalZoom() || V6ModulePicker.isVideoModule() || !((ActivityBase) this.mContext).getUIController().getReviewDoneView().isInVisibleForUser() || CameraSettingPreferences.instance().isFrontCamera() || CameraSettings.isSwitchOn("pref_camera_manual_mode_key")) {
            return false;
        }
        return !CameraSettings.isSwitchOn("pref_camera_portrait_mode_key");
    }

    public void updateVisible() {
        if (isVisible()) {
            setVisibility(0);
            return;
        }
        setVisibility(8);
        this.mZoomRequestAnimator.cancel();
        dismissPopup();
    }

    public void show() {
        if (isVisible()) {
            setVisibility(0);
        }
    }

    public void hide() {
        setVisibility(8);
    }

    public void updateLayoutLocation() {
        int layoutLocationStatus;
        View exitButton = ((ActivityBase) this.mContext).getUIController().getModeExitView().getExitButton();
        View indicator = ((ActivityBase) this.mContext).getUIController().getPopupIndicatorLayout();
        if (exitButton.getVisibility() != 0) {
            layoutLocationStatus = 0;
        } else if (((ActivityBase) this.mContext).getUIController().getPopupParent().getVisibility() == 0) {
            layoutLocationStatus = 1;
        } else if (indicator.getVisibility() == 0) {
            layoutLocationStatus = 2;
        } else {
            layoutLocationStatus = 0;
        }
        if (layoutLocationStatus != this.mLayoutLocationStatus) {
            LayoutParams params = (LayoutParams) getLayoutParams();
            params.removeRule(2);
            params.removeRule(14);
            params.removeRule(11);
            params.removeRule(8);
            if (layoutLocationStatus == 0) {
                params.addRule(14);
                params.addRule(2, R.id.qrcode_viewfinder_layout);
                params.bottomMargin = this.mBottomMargin;
            } else if (layoutLocationStatus == 1) {
                params.addRule(11);
                params.addRule(8, R.id.camera_mode_exit_view);
                params.bottomMargin = ((LinearLayout.LayoutParams) exitButton.getLayoutParams()).bottomMargin + ((exitButton.getHeight() - getHeight()) / 2);
            } else if (layoutLocationStatus == 2) {
                params.addRule(11);
                params.addRule(8, R.id.popup_indicator_layout);
                params.bottomMargin = (indicator.getHeight() - getHeight()) / 2;
            }
            if (isVisible() && (layoutLocationStatus == 0 || this.mLayoutLocationStatus == 0)) {
                startAnimation(this.mShowAnimation);
            }
            this.mLayoutLocationStatus = layoutLocationStatus;
            requestLayout();
        }
    }

    private void notifyZoomAnimationStart() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(7, R.id.zoom_button, 2, getKey(), Boolean.valueOf(false));
        }
    }
}
