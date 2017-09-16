package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.Device;
import com.android.camera.Log;

public class V6PreviewPage extends V6RelativeLayout implements OnClickListener {
    private ActivityBase mActivity;
    private ValueAnimator mAnimPopup;
    private CustomAnimatorListener mAnimatorListener;
    public ImageView mAsdIndicatorView;
    private TimeInterpolator mCollapseInterpolator = new OvershootInterpolator(1.0f);
    private TimeInterpolator mExpandInterpolator = new AccelerateDecelerateInterpolator();
    private OnLayoutChangeListener mLayoutChangeListener = new OnLayoutChangeListener() {
        public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
            if (v == V6PreviewPage.this.mModeExitView) {
                V6PreviewPage.this.mZoomButton.updateLayoutLocation();
            }
            if (left != oldLeft || right != oldRight || top != oldTop || bottom != oldBottom) {
                if (v == V6PreviewPage.this.mModeExitView || v == V6PreviewPage.this.mModeExitButton || v == V6PreviewPage.this.mPopupIndicatorLayout || v == V6PreviewPage.this.mPopupIndicator) {
                    V6PreviewPage.this.createOrUpdateAnimatorListener();
                } else if (v == V6PreviewPage.this.mPopupParent) {
                    V6PreviewPage.this.createAnimation();
                }
            }
        }
    };
    public OrientationIndicator mLyingOriFlag;
    private View mModeExitButton;
    public V6ModeExitView mModeExitView;
    private RotateLayout mOrientationArea;
    private RelativeLayout mOrientationParent;
    public RelativeLayout mPanoramaViewRoot;
    private boolean mPopupGroupVisible = true;
    private View mPopupIndicator;
    public View mPopupIndicatorLayout;
    public ViewGroup mPopupParent;
    public ViewGroup mPopupParentLayout;
    private boolean mPopupVisible = true;
    public PortraitButton mPortraitButton;
    public TextView mPortraitHintTextView;
    public StereoButton mStereoButton;
    public TopPopupParent mTopPopupParent;
    private boolean mVisible = true;
    public LinearLayout mWarningMessageLayout;
    public TextView mWarningView;
    public ZoomButton mZoomButton;

    private enum AnimationType {
        COLLAPSE,
        EXPAND
    }

    private class CustomAnimatorListener extends AnimatorListenerAdapter implements AnimatorUpdateListener {
        private AnimationType mAnimationType;
        private int mIndicatorAndExitDeltaCenter;
        private int mIndicatorLayoutMaxY;
        private int mIndicatorLayoutTransY;
        private int mLayerType;
        private float mModeExitButtonHalfWidth;
        private int mModeExitButtonLeft;
        private int mModeExitButtonRight;

        public CustomAnimatorListener(V6PreviewPage this$0) {
            this(AnimationType.COLLAPSE);
        }

        public CustomAnimatorListener(AnimationType type) {
            this.mAnimationType = type;
            updateParameters();
        }

        public void updateParameters() {
            this.mIndicatorAndExitDeltaCenter = (V6PreviewPage.this.mPopupIndicator.getTop() - V6PreviewPage.this.mModeExitButton.getTop()) + ((V6PreviewPage.this.mPopupIndicator.getHeight() - V6PreviewPage.this.mModeExitButton.getHeight()) / 2);
            this.mIndicatorLayoutMaxY = V6PreviewPage.this.mPopupIndicatorLayout.getTop() + ((V6PreviewPage.this.mPopupIndicatorLayout.getHeight() - V6PreviewPage.this.mPopupIndicator.getBottom()) + V6PreviewPage.this.mPopupIndicator.getPaddingBottom());
            this.mModeExitButtonLeft = V6PreviewPage.this.mModeExitButton.getLeft();
            this.mModeExitButtonRight = V6PreviewPage.this.mModeExitButton.getRight();
            this.mModeExitButtonHalfWidth = ((float) (this.mModeExitButtonRight - this.mModeExitButtonLeft)) * 0.5f;
            int exitViewY = V6PreviewPage.this.getChildY(V6PreviewPage.this.mModeExitView);
            int indicatorLayoutY = V6PreviewPage.this.getChildY(V6PreviewPage.this.mPopupIndicatorLayout);
            this.mIndicatorLayoutTransY = (indicatorLayoutY - exitViewY) + this.mIndicatorAndExitDeltaCenter;
            Log.v("V6PreviewPage", "updateParameters: exitView=" + V6PreviewPage.this.mModeExitView + ",exitButton=" + V6PreviewPage.this.mModeExitButton + ",exitViewY=" + exitViewY);
            Log.v("V6PreviewPage", "updateParameters: indicatorLayout=" + V6PreviewPage.this.mPopupIndicatorLayout + ",indicator=" + V6PreviewPage.this.mPopupIndicator + ",indicatorLayoutY=" + indicatorLayoutY);
        }

        public void setAnimationType(AnimationType type) {
            this.mAnimationType = type;
        }

        public AnimationType getAnimationType() {
            return this.mAnimationType;
        }

        public void onAnimationStart(Animator animation) {
            this.mLayerType = V6PreviewPage.this.getLayerType();
            if (this.mLayerType != 2) {
                V6PreviewPage.this.setLayerType(2, null);
            }
            Log.v("V6PreviewPage", "onAnimationStart: type=" + this.mAnimationType + ",layerType=" + this.mLayerType);
            if (AnimationType.EXPAND == this.mAnimationType) {
                V6PreviewPage.this.mModeExitView.show();
                V6PreviewPage.this.mPopupParent.setVisibility(0);
            } else {
                V6PreviewPage.this.mPopupIndicatorLayout.setVisibility(0);
            }
            V6PreviewPage.this.mZoomButton.updateLayoutLocation();
        }

        public void onAnimationEnd(Animator animation) {
            Log.v("V6PreviewPage", "onAnimationEnd: type=" + this.mAnimationType);
            V6PreviewPage.this.setLayerType(this.mLayerType, null);
            V6PreviewPage.this.mModeExitView.setTranslationY(0.0f);
            V6PreviewPage.this.mZoomButton.setTranslationY(0.0f);
            V6PreviewPage.this.mPopupParent.setTranslationY(0.0f);
            V6PreviewPage.this.mPopupIndicatorLayout.setTranslationY(0.0f);
            V6PreviewPage.this.mModeExitButton.setLeft(this.mModeExitButtonLeft);
            V6PreviewPage.this.mModeExitButton.setRight(this.mModeExitButtonRight);
            if (AnimationType.EXPAND == this.mAnimationType) {
                V6PreviewPage.this.mPopupIndicatorLayout.setVisibility(4);
                V6PreviewPage.this.mPopupIndicator.setAlpha(1.0f);
            } else {
                V6PreviewPage.this.mModeExitView.hide();
                V6PreviewPage.this.mModeExitButton.setAlpha(1.0f);
                V6PreviewPage.this.mPopupParent.setVisibility(4);
            }
            V6PreviewPage.this.mZoomButton.updateLayoutLocation();
        }

        public void onAnimationCancel(Animator animation) {
            Log.v("V6PreviewPage", "onAnimationCancel: type=" + this.mAnimationType);
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            float transY = ((float) this.mIndicatorLayoutTransY) * fraction;
            V6PreviewPage.this.mModeExitView.setTranslationY(transY);
            V6PreviewPage.this.mZoomButton.setTranslationY(transY);
            int deltaX = (int) (((double) (this.mModeExitButtonHalfWidth * fraction)) + 0.5d);
            int newRight = this.mModeExitButtonRight - deltaX;
            V6PreviewPage.this.mModeExitButton.setLeft(this.mModeExitButtonLeft + deltaX);
            V6PreviewPage.this.mModeExitButton.setRight(newRight);
            V6PreviewPage.this.mModeExitButton.setAlpha(Math.max(1.0f - fraction, 0.0f));
            V6PreviewPage.this.mPopupIndicatorLayout.setY(Math.min(V6PreviewPage.this.mModeExitView.getY() - ((float) this.mIndicatorAndExitDeltaCenter), (float) this.mIndicatorLayoutMaxY));
            V6PreviewPage.this.mPopupIndicator.setAlpha(fraction);
        }
    }

    public V6PreviewPage(Context context) {
        super(context);
        this.mActivity = (ActivityBase) context;
    }

    public V6PreviewPage(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mActivity = (ActivityBase) context;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mPopupParentLayout = (ViewGroup) findChildrenById(R.id.v6_setting_popup_parent_layout);
        this.mPopupParent = (ViewGroup) findChildrenById(R.id.v6_setting_popup_parent);
        this.mTopPopupParent = (TopPopupParent) findChildrenById(R.id.setting_expanded_popup_parent);
        this.mModeExitView = (V6ModeExitView) findChildrenById(R.id.camera_mode_exit_view);
        this.mModeExitButton = findViewById(R.id.camera_mode_exit_button);
        this.mWarningView = (TextView) findViewById(R.id.warning_message);
        this.mWarningMessageLayout = (LinearLayout) findViewById(R.id.warning_message_layout);
        this.mPopupIndicatorLayout = findViewById(R.id.popup_indicator_layout);
        this.mPopupIndicator = findChildrenById(R.id.popup_indicator);
        this.mAsdIndicatorView = (ImageView) findViewById(R.id.asd_indicator_image);
        this.mOrientationParent = (RelativeLayout) findViewById(R.id.orientation_indicator_area_parent);
        this.mOrientationArea = (RotateLayout) findChildrenById(R.id.orientation_indicator_area);
        this.mLyingOriFlag = (OrientationIndicator) findChildrenById(R.id.orientation_indicator);
        this.mStereoButton = (StereoButton) findChildrenById(R.id.stereo_switch_image);
        this.mZoomButton = (ZoomButton) findChildrenById(R.id.zoom_button);
        this.mPortraitButton = (PortraitButton) findChildrenById(R.id.portrait_switch_image);
        this.mPortraitHintTextView = (TextView) findChildrenById(R.id.portrait_hint_text);
        this.mPopupIndicator.setOnClickListener(this);
        setupOnLayoutChangeListener();
    }

    private void doViewAnimation(View view, boolean visible) {
        boolean z;
        if (view.getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        if (z == visible) {
            return;
        }
        if (visible) {
            if (view == this.mModeExitView) {
                this.mModeExitView.show();
            } else {
                view.setVisibility(0);
            }
        } else if (view == this.mModeExitView) {
            this.mModeExitView.hide();
        } else {
            view.setVisibility(4);
        }
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        this.mWarningView.setVisibility(0);
        this.mAsdIndicatorView.setVisibility(8);
        this.mVisible = true;
        updatePopupIndicatorImageResource();
    }

    public void switchWithAnimation(boolean switchToPreviewPage) {
        Log.v("Camera10", "switchWithAnimation: toPreviewPage=" + switchToPreviewPage + ",popupVisible=" + this.mPopupVisible + ",groupVisible=" + this.mPopupGroupVisible);
        if (switchToPreviewPage) {
            doViewAnimation(this.mWarningView, true);
            doViewAnimation(this.mOrientationArea, true);
            if (this.mPopupGroupVisible) {
                if (this.mPopupVisible) {
                    doViewAnimation(this.mModeExitView, true);
                    doViewAnimation(this.mPopupParent, true);
                } else if (hasCollapsedPopup()) {
                    doViewAnimation(this.mPopupIndicatorLayout, true);
                }
            }
            this.mStereoButton.updateVisible();
            this.mZoomButton.updateVisible();
            this.mPortraitButton.updateVisible();
        } else {
            doViewAnimation(this.mModeExitView, false);
            doViewAnimation(this.mWarningView, false);
            doViewAnimation(this.mAsdIndicatorView, false);
            doViewAnimation(this.mPopupParent, false);
            doViewAnimation(this.mPopupIndicatorLayout, false);
            doViewAnimation(this.mOrientationArea, false);
            doViewAnimation(this.mStereoButton, false);
            doViewAnimation(this.mZoomButton, false);
            doViewAnimation(this.mPortraitButton, false);
            doViewAnimation(this.mPortraitHintTextView, false);
        }
        this.mTopPopupParent.onPreviewPageShown(switchToPreviewPage);
        this.mVisible = switchToPreviewPage;
    }

    public void updatePopupIndicator() {
        boolean hasSettingPopup = hasCollapsedPopup();
        Log.v("V6PreviewPage", "updatePopupIndicator: groupVisible=" + this.mPopupGroupVisible + ",popupVisible=" + this.mPopupVisible + ",hasSettingPopup=" + hasSettingPopup);
        if (this.mPopupGroupVisible) {
            boolean z = this.mPopupVisible;
            boolean z2 = this.mPopupVisible;
            if (this.mPopupVisible) {
                hasSettingPopup = false;
            }
            updatePopupVisibility(z, z2, hasSettingPopup);
            return;
        }
        updatePopupVisibility(false, false, false);
    }

    public void setPopupVisible(boolean visible) {
        if (this.mPopupGroupVisible != visible) {
            this.mPopupGroupVisible = visible;
            updatePopupIndicator();
        }
    }

    public void simplifyPopup(boolean simplify, boolean animation) {
        Log.v("V6PreviewPage", "simplifyPopup: simplify=" + simplify + ",animation=" + animation);
        if (this.mPopupVisible || !simplify) {
            if (simplify && hasCollapsedPopup()) {
                this.mPopupVisible = false;
                if (animation) {
                    hidePopupView();
                } else {
                    updatePopupVisibility(false, false, true);
                }
            } else if (!simplify) {
                this.mPopupVisible = true;
                if (!animation) {
                    updatePopupVisibility(true, true, false);
                } else if (!(this.mAnimPopup.isStarted() && this.mAnimatorListener.getAnimationType() == AnimationType.EXPAND)) {
                    this.mAnimatorListener.setAnimationType(AnimationType.EXPAND);
                    this.mAnimPopup.setInterpolator(this.mExpandInterpolator);
                    this.mAnimPopup.reverse();
                }
            }
        }
    }

    public void showPopupWithoutExitView() {
        if (hasCollapsedPopup()) {
            this.mPopupVisible = true;
            updatePopupVisibility(false, true, false);
        }
    }

    private boolean shouldAnimatePopup(V6AbstractSettingPopup popup) {
        if (((ActivityBase) this.mContext).isPaused()) {
            return false;
        }
        View visiblePopup = null;
        for (int i = 0; i < this.mPopupParent.getChildCount(); i++) {
            if (this.mPopupParent.getChildAt(i).getVisibility() == 0) {
                visiblePopup = this.mPopupParent.getChildAt(i);
                if (visiblePopup != popup) {
                    return false;
                }
            }
        }
        if (visiblePopup == null) {
            return true;
        }
        if (popup == null || popup != visiblePopup) {
            return false;
        }
        return PopupManager.getInstance(this.mContext).getLastOnOtherPopupShowedListener() == null;
    }

    private Animation initAnimation(V6AbstractSettingPopup popup, boolean slideUp) {
        Animation animation = popup.getAnimation(slideUp);
        if (animation == null) {
            if (slideUp) {
                animation = AnimationUtils.loadAnimation(this.mContext, R.anim.slide_up);
            } else {
                animation = AnimationUtils.loadAnimation(this.mContext, R.anim.slide_down);
            }
        }
        if (!slideUp) {
            animation.setAnimationListener(new SimpleAnimationListener(popup, false));
        }
        return animation;
    }

    public void showPopup(V6AbstractSettingPopup popup) {
        if (shouldAnimatePopup(popup)) {
            popup.show(false);
            popup.clearAnimation();
            popup.startAnimation(initAnimation(popup, true));
            return;
        }
        popup.show(false);
    }

    public void dismissPopup(V6AbstractSettingPopup popup) {
        if (shouldAnimatePopup(popup)) {
            popup.clearAnimation();
            popup.startAnimation(initAnimation(popup, false));
            return;
        }
        popup.dismiss(false);
    }

    private View getCurrentPopupShownView() {
        if (this.mPopupParent.isShown()) {
            for (int i = 0; i < this.mPopupParent.getChildCount(); i++) {
                if (this.mPopupParent.getChildAt(i).isShown()) {
                    return this.mPopupParent.getChildAt(i);
                }
            }
        }
        return null;
    }

    public boolean isPopupShown() {
        return getCurrentPopupShownView() != null;
    }

    public void onPopupChange() {
        Log.v("V6PreviewPage", "onPopupChange");
        this.mPopupVisible = true;
        this.mPopupIndicatorLayout.setVisibility(4);
        this.mZoomButton.updateLayoutLocation();
    }

    public void inflatePanoramaView() {
        if (this.mPanoramaViewRoot == null) {
            ((LayoutInflater) this.mContext.getSystemService("layout_inflater")).inflate(R.layout.pano_view, this);
            this.mPanoramaViewRoot = (RelativeLayout) findChildrenById(R.id.pano_capture_view_layout);
        }
    }

    public boolean isPreviewPageVisible() {
        return this.mVisible;
    }

    public void onClick(View v) {
        simplifyPopup(false, true);
    }

    private void setupOnLayoutChangeListener() {
        this.mPopupParent.addOnLayoutChangeListener(this.mLayoutChangeListener);
        this.mPopupIndicatorLayout.addOnLayoutChangeListener(this.mLayoutChangeListener);
        this.mPopupIndicator.addOnLayoutChangeListener(this.mLayoutChangeListener);
        this.mModeExitView.addOnLayoutChangeListener(this.mLayoutChangeListener);
        this.mModeExitButton.addOnLayoutChangeListener(this.mLayoutChangeListener);
    }

    private void createOrUpdateAnimatorListener() {
        if (this.mAnimatorListener == null) {
            this.mAnimatorListener = new CustomAnimatorListener(this);
        } else {
            this.mAnimatorListener.updateParameters();
        }
    }

    private void createAnimation() {
        Log.v("V6PreviewPage", "createAnimation: popupHeight=" + this.mPopupParent.getHeight());
        if (this.mAnimatorListener == null) {
            createOrUpdateAnimatorListener();
        }
        PropertyValuesHolder popupTransYOut = PropertyValuesHolder.ofFloat("translationY", new float[]{0.0f, (float) this.mPopupParent.getHeight()});
        this.mAnimPopup = ObjectAnimator.ofPropertyValuesHolder(this.mPopupParent, new PropertyValuesHolder[]{popupTransYOut});
        this.mAnimPopup.addListener(this.mAnimatorListener);
        this.mAnimPopup.addUpdateListener(this.mAnimatorListener);
    }

    private void updatePopupIndicatorImageResource() {
        if (!(this.mPopupIndicator instanceof ImageView)) {
            return;
        }
        if (((ActivityBase) this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
            ((ImageView) this.mPopupIndicator).setImageResource(R.drawable.ic_popup_indicator_full_screen);
        } else {
            ((ImageView) this.mPopupIndicator).setImageResource(R.drawable.ic_popup_indicator);
        }
    }

    private void updatePopupVisibility(boolean isExitViewVisible, boolean isPopupVisible, boolean isIndicatorVisible) {
        int i;
        int i2 = 0;
        if (isExitViewVisible) {
            this.mModeExitView.show();
        } else {
            this.mModeExitView.hide();
        }
        ViewGroup viewGroup = this.mPopupParent;
        if (isPopupVisible) {
            i = 0;
        } else {
            i = 4;
        }
        viewGroup.setVisibility(i);
        View view = this.mPopupIndicatorLayout;
        if (!isIndicatorVisible) {
            i2 = 4;
        }
        view.setVisibility(i2);
    }

    private boolean hasCollapsedPopup() {
        if (((ActivityBase) this.mContext).getUIController().getSettingPage().getCurrentPopup() == null) {
            return ((ActivityBase) this.mContext).getUIController().getStereoButton().isPopupVisible();
        }
        return true;
    }

    private int getChildY(View view) {
        int y = view.getTop();
        View parent = view.getParent();
        while ((parent instanceof View) && parent != this) {
            View viewParent = parent;
            y += viewParent.getTop();
            parent = viewParent.getParent();
        }
        return y;
    }

    public void setOrientation(int orientation, boolean animation) {
        super.setOrientation(orientation, animation);
        updateRotateLayout(orientation, this.mOrientationArea);
    }

    private void updateRotateLayout(int degree, View view) {
        LayoutParams params = (LayoutParams) view.getLayoutParams();
        params.removeRule(9);
        params.removeRule(10);
        params.removeRule(11);
        params.removeRule(12);
        switch (degree) {
            case 0:
                params.addRule(10, -1);
                break;
            case 90:
                params.addRule(9, -1);
                break;
            case 180:
                params.addRule(12, -1);
                break;
            case 270:
                params.addRule(11, -1);
                break;
        }
        view.setLayoutParams(params);
    }

    public void updateOrientationLayout(boolean topMargin) {
        if (Device.isOrientationIndicatorEnabled()) {
            boolean z;
            LayoutParams params = (LayoutParams) this.mOrientationParent.getLayoutParams();
            if (params.topMargin != 0) {
                z = true;
            } else {
                z = false;
            }
            if (topMargin != z) {
                int dimensionPixelSize;
                if (topMargin) {
                    dimensionPixelSize = getResources().getDimensionPixelSize(R.dimen.top_control_panel_height);
                } else {
                    dimensionPixelSize = 0;
                }
                params.setMargins(0, dimensionPixelSize, 0, 0);
                this.mOrientationParent.setLayoutParams(params);
            }
        }
    }

    private void hidePopupView() {
        if (getCurrentPopupShownView() instanceof StereoPopup) {
            ((ActivityBase) this.mContext).getUIController().getStereoButton().dismissPopup(true);
            return;
        }
        this.mAnimatorListener.setAnimationType(AnimationType.COLLAPSE);
        this.mAnimPopup.setInterpolator(this.mCollapseInterpolator);
        this.mAnimPopup.start();
    }
}
