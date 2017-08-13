package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.Log;
import com.android.camera.Util;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;

public class V6TopTextView extends TextView implements MessageDispacher, OnOtherPopupShowedListener, V6FunctionUI, AnimateView {
    protected MessageDispacher mMessageDispacher;
    protected String mOverrideValue;
    protected V6AbstractSettingPopup mPopup;
    protected IconListPreference mPreference;

    public V6TopTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!isEnabled()) {
            return false;
        }
        int action = ev.getAction();
        if (action == 0 && !isOverridden()) {
            setPressed(true);
            return true;
        } else if (action == 3) {
            dismissPopup();
            return true;
        } else if (action != 1) {
            return true;
        } else {
            if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
                CameraDataAnalytics.instance().trackEvent(getKey());
            }
            if (Util.pointInView(ev.getRawX(), ev.getRawY(), this)) {
                doTapButton();
                if (this.mPopup == null) {
                    setPressed(false);
                }
                playSoundEffect(0);
                AutoLockManager.getInstance(this.mContext).onUserInteraction();
            }
            return true;
        }
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        dismissPopup();
        if (!((extra2 instanceof Boolean) && ((Boolean) extra2).booleanValue())) {
            notifyClickToDispatcher();
        }
        return true;
    }

    private void doTapButton() {
        if (!isOverridden()) {
            if (this.mPreference == null || !this.mPreference.hasPopup() || this.mPreference.getEntryValues().length < 3) {
                toggle();
            } else if (this.mPopup == null || this.mPopup.getVisibility() != 0) {
                setPressed(true);
                showPopup();
                ((ActivityBase) this.mContext).getUIController().getPreviewPage().simplifyPopup(false, false);
                PopupManager.getInstance(getContext()).notifyShowPopup(this, 1);
            } else {
                dismissPopup();
            }
        }
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
        notifyClickToDispatcher();
    }

    public void reloadPreference() {
        updateTitle();
        if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        }
    }

    private int getPreferenceSize() {
        CharSequence[] entries = this.mPreference.getEntryValues();
        return entries != null ? entries.length : 0;
    }

    public void enableControls(boolean enabled) {
        setEnabled(enabled);
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public boolean onOtherPopupShowed(int level) {
        dismissPopup();
        return false;
    }

    public void recoverIfNeeded() {
        showPopup();
    }

    public void showPopup() {
        initializePopup();
        if (this.mPopup != null) {
            this.mPopup.setOrientation(0, false);
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().showPopup(this.mPopup);
            notifyPopupVisibleChange(true);
        }
    }

    protected void initializePopup() {
        if (this.mPreference == null || !this.mPreference.hasPopup()) {
            Log.i("V6TopTextView", "no need to initialize popup, key=" + getKey() + " mPreference=" + this.mPreference + " mPopup=" + this.mPopup);
        } else if (this.mPopup != null) {
            this.mPopup.reloadPreference();
        } else {
            ViewGroup root = ((ActivityBase) this.mContext).getUIController().getTopPopupParent();
            this.mPopup = SettingPopupFactory.createSettingPopup(getKey(), root, getContext());
            this.mPopup.initialize(((ActivityBase) this.mContext).getUIController().getPreferenceGroup(), this.mPreference, this);
            root.addView(this.mPopup);
        }
    }

    public boolean dismissPopup() {
        boolean result = false;
        if (this.mPopup != null && this.mPopup.getVisibility() == 0) {
            ((ActivityBase) this.mContext).getUIController().getPreviewPage().dismissPopup(this.mPopup);
            notifyPopupVisibleChange(false);
            result = true;
            PopupManager.getInstance(getContext()).notifyDismissPopup();
        }
        setPressed(false);
        return result;
    }

    private String getKey() {
        return this.mPreference == null ? "" : this.mPreference.getKey();
    }

    protected void updateTitle() {
        StringBuilder sb = new StringBuilder(this.mPreference.getTitle());
        sb.append("  ");
        sb.append(this.mPreference.getEntry());
        setText(sb.toString());
    }

    public boolean isOverridden() {
        return this.mOverrideValue != null;
    }

    protected void notifyClickToDispatcher() {
    }

    protected void notifyPopupVisibleChange(boolean visible) {
    }

    public void hide(boolean animation) {
        if (animation) {
            clearAnimation();
            startAnimation(initAnimation(false));
            return;
        }
        setVisibility(8);
    }

    public void show(boolean animation) {
        setVisibility(0);
        if (animation) {
            clearAnimation();
            startAnimation(initAnimation(true));
        }
    }

    private Animation initAnimation(boolean show) {
        if (show) {
            return AnimationUtils.loadAnimation(this.mContext, R.anim.show);
        }
        Animation animation = AnimationUtils.loadAnimation(this.mContext, R.anim.dismiss);
        animation.setAnimationListener(new SimpleAnimationListener(this, false));
        return animation;
    }
}
