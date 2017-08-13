package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnPreDrawListener;
import android.widget.FrameLayout;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Log;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import miui.os.Build;

public class SubScreenPopup extends V6AbstractSettingPopup {
    private V6AbstractSettingPopup mCurrentPopup;
    private V6ModeExitView mExitView;
    private OnPreDrawListener mOnPreDrawListener = new OnPreDrawListener() {
        public boolean onPreDraw() {
            if (SubScreenPopup.this.mCurrentPopup == null || SubScreenPopup.this.mPendingAnimationType == null) {
                return true;
            }
            int transY = SubScreenPopup.this.computeTransY();
            SubScreenPopup.this.setTransY(SubScreenPopup.this.mCurrentPopup, transY);
            SubScreenPopup.this.startAnimation(SubScreenPopup.this.setupAnimation(transY, SubScreenPopup.this.mPendingAnimationType), SubScreenPopup.this.mPendingAnimationType);
            SubScreenPopup.this.removeOnPreDrawListener();
            return false;
        }
    };
    private AnimationType mPendingAnimationType;
    private HashMap<V6AbstractSettingPopup, Integer> mPopupTranslationMap = new HashMap();
    private ValueAnimator mRunningAnimation;
    private SettingView mSettingView;
    private FrameLayout mSubPopupParent;
    private SparseArray<ValueAnimator> mTranslationAnimationMap = new SparseArray();
    private View mValueBottomLine;

    private enum AnimationType {
        SLIDE_DOWN_POPUP,
        SLIDE_UP_POPUP
    }

    private class CustomAnimatorListener extends AnimatorListenerAdapter implements AnimatorUpdateListener {
        private boolean mIsValueVisible;
        private int mLayerType;
        private AnimationType mType;

        public CustomAnimatorListener(boolean isValueVisible, AnimationType type) {
            this.mIsValueVisible = isValueVisible;
            this.mType = type;
        }

        public CustomAnimatorListener(SubScreenPopup this$0) {
            this(true, AnimationType.SLIDE_DOWN_POPUP);
        }

        public void setAnimationType(AnimationType type) {
            this.mType = type;
        }

        public void onAnimationUpdate(ValueAnimator animation) {
            float transY = ((Float) animation.getAnimatedValue()).floatValue();
            float alpha = 1.0f - animation.getAnimatedFraction();
            SubScreenPopup.this.mExitView.setTranslationY(transY);
            SubScreenPopup.this.mValueBottomLine.setAlpha(alpha);
            SubScreenPopup.this.mValueBottomLine.setTranslationY(transY);
        }

        public void onAnimationCancel(Animator animation) {
            Log.v("V6ManualPopup", "onAnimationCancel: animation=" + animation);
        }

        public void onAnimationEnd(Animator animation) {
            boolean slideDown = AnimationType.SLIDE_DOWN_POPUP == this.mType;
            Log.v("V6ManualPopup", "onAnimationEnd: type=" + this.mType + ",animation=" + animation + ",popup=" + SubScreenPopup.this.mCurrentPopup);
            if (slideDown) {
                SubScreenPopup.this.mValueBottomLine.setVisibility(8);
                if (SubScreenPopup.this.mCurrentPopup != null) {
                    SubScreenPopup.this.mCurrentPopup.setVisibility(8);
                    SubScreenPopup.this.mCurrentPopup = null;
                }
            }
            SubScreenPopup.this.mExitView.setTranslationY(0.0f);
            SubScreenPopup.this.mValueBottomLine.setAlpha(1.0f);
            SubScreenPopup.this.mValueBottomLine.setTranslationY(0.0f);
            SubScreenPopup.this.setLayerType(this.mLayerType, null);
        }

        public void onAnimationStart(Animator animation) {
            this.mLayerType = SubScreenPopup.this.getLayerType();
            if (this.mLayerType != 2) {
                SubScreenPopup.this.setLayerType(2, null);
            }
            Log.v("V6ManualPopup", "onAnimationStart: layerType=" + this.mLayerType + ",type=" + this.mType + ",animation=" + animation + ",popup=" + SubScreenPopup.this.mCurrentPopup);
        }
    }

    public SubScreenPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mExitView = ((ActivityBase) this.mContext).getUIController().getModeExitView();
        this.mValueBottomLine = findViewById(R.id.manual_popup_parent_upper_line);
        this.mSubPopupParent = (FrameLayout) findViewById(R.id.setting_view_popup_parent);
        this.mSettingView = (SettingView) findViewById(R.id.setting_view);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mSettingView.setOrientation(orientation, animation);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        List<String> itemKeys = getItemKeys();
        this.mSettingView.initializeSettingScreen(this.mPreferenceGroup, itemKeys, itemKeys.size(), this.mMessageDispacher, this.mSubPopupParent, this);
    }

    public void updateBackground() {
        if (((ActivityBase) this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
            this.mSettingView.setBackgroundResource(R.color.fullscreen_background);
        } else {
            this.mSettingView.setBackgroundResource(R.color.halfscreen_background);
        }
    }

    private List<String> getItemKeys() {
        List<String> keys = new ArrayList();
        if (this.mPreference == null) {
            if (Device.IS_MI3TD || Device.IS_HM3Y || Device.IS_HM3Z || Device.IS_C8) {
                keys.add("pref_skin_beautify_enlarge_eye_key");
            }
            keys.add("pref_skin_beautify_slim_face_key");
            if (!Build.IS_INTERNATIONAL_BUILD) {
                keys.add("pref_skin_beautify_skin_color_key");
            }
            keys.add("pref_skin_beautify_skin_smooth_key");
        } else {
            keys.add("pref_camera_whitebalance_key");
            if (Device.isSupportedManualFunction()) {
                keys.add("pref_focus_position_key");
                keys.add("pref_qc_camera_exposuretime_key");
            }
            keys.add("pref_qc_camera_iso_key");
            if (CameraSettings.isSupportedOpticalZoom()) {
                keys.add("pref_camera_zoom_mode_key");
            }
        }
        return keys;
    }

    public void reloadPreference() {
        if (this.mSettingView != null) {
            this.mSettingView.reloadPreferences();
            if (this.mCurrentPopup != null && this.mCurrentPopup.getVisibility() == 0) {
                this.mSettingView.setPressed(this.mCurrentPopup.getKey(), true);
            }
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mSettingView != null) {
            this.mSettingView.setEnabled(enabled);
        }
    }

    public void showChildPopup(V6AbstractSettingPopup popup) {
        if (popup != null && popup.getVisibility() != 0) {
            this.mCurrentPopup = popup;
            this.mValueBottomLine.setVisibility(0);
            popup.show(false);
            if (shouldAnimatePopup(popup)) {
                int transY = getTransY(popup);
                Log.v("V6ManualPopup", "showChildPopup: transY=" + transY + ",popup=" + popup);
                if (transY == 0) {
                    this.mPendingAnimationType = AnimationType.SLIDE_UP_POPUP;
                    addOnPreDrawListener();
                    return;
                }
                startAnimation(setupAnimation(transY, AnimationType.SLIDE_UP_POPUP), AnimationType.SLIDE_UP_POPUP);
            }
        }
    }

    public void dismiss(boolean animate) {
        super.dismiss(animate);
        this.mSettingView.onDismiss();
    }

    public void onDestroy() {
        super.onDestroy();
        removeOnPreDrawListener();
        this.mSettingView.onDestroy();
    }

    public boolean dismissChildPopup(V6AbstractSettingPopup popup) {
        if (popup == null || popup.getVisibility() != 0) {
            return false;
        }
        if (shouldAnimatePopup(popup)) {
            int transY = getTransY(popup);
            Log.v("V6ManualPopup", "dismissChildPopup: transY=" + transY + ",popup=" + popup);
            if (transY == 0) {
                this.mPendingAnimationType = AnimationType.SLIDE_DOWN_POPUP;
                if (!addOnPreDrawListener()) {
                    if (this.mCurrentPopup == popup) {
                        this.mValueBottomLine.setVisibility(8);
                    }
                    popup.dismiss(false);
                }
            } else {
                startAnimation(setupAnimation(transY, AnimationType.SLIDE_DOWN_POPUP), AnimationType.SLIDE_DOWN_POPUP);
            }
        } else {
            if (this.mCurrentPopup == popup) {
                this.mValueBottomLine.setVisibility(8);
            }
            popup.dismiss(false);
        }
        return true;
    }

    private boolean shouldAnimatePopup(V6AbstractSettingPopup popup) {
        if (this.mSubPopupParent != null) {
            int childCount = this.mSubPopupParent.getChildCount();
            if (childCount == 0) {
                return true;
            }
            for (int index = 0; index < childCount; index++) {
                View child = this.mSubPopupParent.getChildAt(index);
                if (popup != child && child.getVisibility() == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean addOnPreDrawListener() {
        ViewTreeObserver observer = getViewTreeObserver();
        if (!observer.isAlive()) {
            return false;
        }
        observer.addOnPreDrawListener(this.mOnPreDrawListener);
        return true;
    }

    private boolean removeOnPreDrawListener() {
        ViewTreeObserver observer = getViewTreeObserver();
        if (!observer.isAlive()) {
            return false;
        }
        observer.removeOnPreDrawListener(this.mOnPreDrawListener);
        return true;
    }

    private int getTransY(V6AbstractSettingPopup popup) {
        Integer transY = (Integer) this.mPopupTranslationMap.get(popup);
        if (transY == null) {
            return 0;
        }
        return transY.intValue();
    }

    private void setTransY(V6AbstractSettingPopup popup, int transY) {
        if (popup != null) {
            this.mPopupTranslationMap.put(popup, Integer.valueOf(transY));
        }
    }

    private int computeTransY() {
        return this.mSubPopupParent.getHeight();
    }

    private ValueAnimator setupAnimation(int transY, AnimationType type) {
        ValueAnimator animSlideDown = (ValueAnimator) this.mTranslationAnimationMap.get(transY);
        if (animSlideDown != null) {
            for (AnimatorListener listener : animSlideDown.getListeners()) {
                if (listener instanceof CustomAnimatorListener) {
                    ((CustomAnimatorListener) listener).setAnimationType(type);
                }
            }
            Log.v("V6ManualPopup", "setupAnimation: reuse transY=" + transY + " -> anim=" + animSlideDown);
            return animSlideDown;
        }
        CustomAnimatorListener animatorListener = new CustomAnimatorListener(this);
        animatorListener.setAnimationType(type);
        PropertyValuesHolder popupTransYOut = PropertyValuesHolder.ofFloat("translationY", new float[]{0.0f, (float) transY});
        animSlideDown = ObjectAnimator.ofPropertyValuesHolder(this.mSubPopupParent, new PropertyValuesHolder[]{popupTransYOut});
        animSlideDown.addListener(animatorListener);
        animSlideDown.addUpdateListener(animatorListener);
        this.mTranslationAnimationMap.put(transY, animSlideDown);
        Log.v("V6ManualPopup", "setupAnimation: new transY=" + transY + " -> anim=" + animSlideDown);
        return animSlideDown;
    }

    private void startAnimation(ValueAnimator animator, AnimationType type) {
        this.mPendingAnimationType = null;
        if (!(this.mRunningAnimation == null || this.mRunningAnimation == animator || !this.mRunningAnimation.isRunning())) {
            this.mRunningAnimation.cancel();
        }
        this.mRunningAnimation = animator;
        if (type == AnimationType.SLIDE_DOWN_POPUP) {
            animator.start();
        } else {
            animator.reverse();
        }
    }
}
