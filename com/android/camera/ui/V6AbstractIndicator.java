package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.CameraAppImpl;
import com.android.camera.Log;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.ui.PopupManager.OnOtherPopupShowedListener;

public abstract class V6AbstractIndicator extends RelativeLayout implements Rotatable, OnOtherPopupShowedListener {
    public static final int TEXT_COLOR_DEFAULT = CameraAppImpl.getAndroidContext().getResources().getColor(R.color.text_color_default);
    public static final int TEXT_COLOR_SELECTED = CameraAppImpl.getAndroidContext().getResources().getColor(R.color.text_color_selected);
    protected TextView mContent;
    protected V6ModeExitView mExitView;
    protected TwoStateImageView mImage;
    protected MessageDispacher mMessageDispacher;
    protected int mOrientation;
    protected V6AbstractSettingPopup mPopup;
    protected ViewGroup mPopupRoot;
    protected IconListPreference mPreference;
    protected PreferenceGroup mPreferenceGroup;
    protected TextView mTitle;

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public V6AbstractIndicator(Context context) {
        super(context);
    }

    public V6AbstractIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(IconListPreference preference, MessageDispacher p, ViewGroup popupRoot, int width, int height, PreferenceGroup preferenceGroup) {
        this.mPreferenceGroup = preferenceGroup;
        this.mPreference = preference;
        this.mMessageDispacher = p;
        this.mPopupRoot = popupRoot;
        boolean reLayout = false;
        LayoutParams selfParams = getLayoutParams();
        if (-10 != width) {
            selfParams.width = width;
            reLayout = true;
        }
        if (-10 != height) {
            selfParams.height = height;
            reLayout = true;
        }
        this.mExitView = ((ActivityBase) this.mContext).getUIController().getModeExitView();
        if (this.mImage != null) {
            this.mImage.setImageResource(this.mPreference.getSingleIcon());
        }
        setContentDescription(this.mPreference.getTitle());
        updateImage();
        updateTitle();
        updateContent();
        if (reLayout) {
            setLayoutParams(selfParams);
            requestLayout();
        }
    }

    protected boolean isIndicatorSelected() {
        return false;
    }

    public V6AbstractSettingPopup getPopup() {
        return this.mPopup;
    }

    protected void updateImage() {
        Log.v("Camera5", "updateImage= " + this.mPreference.getSingleIcon() + " default=" + this.mPreference.isDefaultValue() + " value=" + this.mPreference.getValue() + " isIndicatorSelected=" + isIndicatorSelected() + " this=" + this.mPreference.getKey());
        if (this.mImage != null) {
            if (this.mPreference.getSingleIcon() != 0) {
                this.mImage.setVisibility(0);
                if (this.mPreference.getEnable()) {
                    boolean isIndicatorSelected;
                    TwoStateImageView twoStateImageView = this.mImage;
                    if (this.mPreference.isDefaultValue()) {
                        isIndicatorSelected = isIndicatorSelected();
                    } else {
                        isIndicatorSelected = true;
                    }
                    twoStateImageView.setSelected(isIndicatorSelected);
                    this.mTitle.setEnabled(true);
                } else {
                    this.mImage.setEnabled(false);
                    this.mTitle.setEnabled(false);
                }
                this.mImage.setContentDescription(this.mPreference.getTitle());
            } else {
                this.mImage.setVisibility(8);
            }
        }
    }

    protected int getShowedColor() {
        return isPressed() ? TEXT_COLOR_SELECTED : TEXT_COLOR_DEFAULT;
    }

    protected void updateTitle() {
        if (this.mTitle != null) {
            String title = this.mPreference.getTitle();
            this.mTitle.setText(title);
            if (!(this instanceof V6IndicatorButton)) {
                if (title != null) {
                    this.mTitle.setVisibility(0);
                    this.mTitle.setTextColor(getShowedColor());
                } else {
                    this.mTitle.setVisibility(8);
                }
            }
        }
    }

    protected void updateContent() {
    }

    public void setPressed(boolean pressed) {
        super.setPressed(pressed);
        updateTitle();
    }

    public void setActivated(boolean activated) {
        super.setActivated(activated);
        if (this.mImage != null) {
            this.mImage.setActivated(activated);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mImage = (TwoStateImageView) findViewById(R.id.image);
        this.mTitle = (TextView) findViewById(R.id.text);
        this.mContent = (TextView) findViewById(R.id.indicator_content);
        if (this.mImage != null) {
            this.mImage.enableFilter(true);
        }
    }

    public boolean onOtherPopupShowed(int level) {
        dismissPopup();
        return false;
    }

    public void recoverIfNeeded() {
        showPopup();
    }

    public void showPopup() {
    }

    public boolean dismissPopup() {
        return false;
    }

    public String getKey() {
        return this.mPreference == null ? "" : this.mPreference.getKey();
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        orientation = -orientation;
        int rotation = (int) getRotation();
        int deltaR = (orientation >= 0 ? orientation % 360 : (orientation % 360) + 360) - (rotation >= 0 ? rotation % 360 : (rotation % 360) + 360);
        if (deltaR == 0) {
            animate().cancel();
            return;
        }
        if (Math.abs(deltaR) > 180) {
            if (deltaR >= 0) {
                deltaR -= 360;
            } else {
                deltaR += 360;
            }
        }
        if (animation) {
            animate().withLayer().rotation((float) (rotation + deltaR)).setDuration((long) ((Math.abs(deltaR) * 1000) / 270));
        } else {
            animate().withLayer().rotation((float) (rotation + deltaR)).setDuration(0);
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mPopup != null) {
            this.mPopup.setEnabled(enabled);
        }
    }

    public void reloadPreference() {
    }

    public void onDismiss() {
    }

    public void onDestroy() {
    }
}
