package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import com.android.camera.ActivityBase;

public class TopControlPanel extends V6RelativeLayout implements MessageDispacher {
    public SkinBeautyButton mBeautyButton;
    private boolean mControlVisible;
    public FlashButton mFlashButton;
    public HdrButton mHdrButton;
    private MessageDispacher mMessageDispacher;
    public PeakButton mPeakButton;

    public TopControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        this.mFlashButton = (FlashButton) findChildrenById(R.id.v6_flash_mode_button);
        this.mHdrButton = (HdrButton) findChildrenById(R.id.v6_hdr);
        this.mBeautyButton = (SkinBeautyButton) findChildrenById(R.id.skin_beatify_button);
        this.mPeakButton = (PeakButton) findChildrenById(R.id.v6_peak);
    }

    public void setMessageDispacher(MessageDispacher p) {
        super.setMessageDispacher(this);
        this.mMessageDispacher = p;
    }

    public FlashButton getFlashButton() {
        return this.mFlashButton;
    }

    public HdrButton getHdrButton() {
        return this.mHdrButton;
    }

    public SkinBeautyButton getSkinBeautyButton() {
        return this.mBeautyButton;
    }

    public PeakButton getPeakButton() {
        return this.mPeakButton;
    }

    public void onCameraOpen() {
        boolean z;
        super.onCameraOpen();
        if (((ActivityBase) this.mContext).isScanQRCodeIntent()) {
            setVisibility(4);
        } else if (((ActivityBase) this.mContext).getUIController().getReviewDoneView().getVisibility() == 0 || V6ModulePicker.isPanoramaModule()) {
            setVisibility(8);
        } else {
            setVisibility(0);
        }
        if (getVisibility() == 0) {
            z = true;
        } else {
            z = false;
        }
        this.mControlVisible = z;
    }

    public boolean dispacherMessage(int what, int sender, int receiver, Object extra1, Object extra2) {
        if (what == 4) {
            if (!((Boolean) extra1).booleanValue()) {
                switch (sender) {
                    case R.id.v6_flash_mode_button:
                        if (!this.mHdrButton.couldBeVisible()) {
                            if (!this.mBeautyButton.couldBeVisible()) {
                                if (this.mPeakButton.couldBeVisible()) {
                                    this.mPeakButton.show(true);
                                    break;
                                }
                            }
                            this.mBeautyButton.show(true);
                            break;
                        }
                        this.mHdrButton.overrideSettings(null);
                        this.mHdrButton.show(true);
                        break;
                        break;
                    case R.id.v6_hdr:
                    case R.id.skin_beatify_button:
                        if (this.mFlashButton.couldBeVisible()) {
                            this.mFlashButton.show(true);
                            break;
                        }
                        break;
                    default:
                        break;
                }
            }
            switch (sender) {
                case R.id.v6_flash_mode_button:
                    hideSubViewExcept(this.mFlashButton, true);
                    break;
                case R.id.v6_hdr:
                    hideSubViewExcept(this.mHdrButton, true);
                    break;
                case R.id.skin_beatify_button:
                    hideSubViewExcept(this.mBeautyButton, true);
                    break;
            }
        }
        if (this.mMessageDispacher != null) {
            return this.mMessageDispacher.dispacherMessage(what, sender, receiver, extra1, extra2);
        }
        return false;
    }

    public void animateIn(Runnable callback) {
        if (getVisibility() != 0 || !this.mControlVisible) {
            if (getVisibility() != 0) {
                setVisibility(0);
            }
            animate().withLayer().alpha(1.0f).setDuration(150).setInterpolator(new DecelerateInterpolator()).withEndAction(callback).start();
            this.mControlVisible = true;
        }
    }

    public void animateOut(final Runnable callback) {
        this.mControlVisible = false;
        if (getVisibility() == 0) {
            animate().withLayer().alpha(0.0f).setDuration(150).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
                public void run() {
                    if (callback != null) {
                        callback.run();
                    }
                    if (!TopControlPanel.this.mControlVisible) {
                        TopControlPanel.this.setVisibility(8);
                    }
                    TopControlPanel.this.setAlpha(1.0f);
                }
            }).start();
        }
    }

    public void onShowModeSettings() {
        this.mFlashButton.dismissPopup();
        this.mHdrButton.dismissPopup();
    }

    private void hideSubViewExcept(View view, boolean animate) {
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            if (!(v == view || v.getVisibility() == 8)) {
                if (v instanceof AnimateView) {
                    ((AnimateView) v).hide(animate);
                } else {
                    v.setVisibility(8);
                }
            }
        }
    }
}
