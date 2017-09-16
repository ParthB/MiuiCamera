package com.android.camera.ui;

import android.content.Context;
import android.hardware.Camera.Parameters;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.CameraManager;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.preferences.CameraSettingPreferences;
import java.util.Locale;

public class V6SettingsStatusBar extends V6RelativeLayout implements MutexView {
    private TextView mApertureTextView;
    private TextView mEvTextView;
    private int mMarginTop;
    private int mMarginTopLandscape;
    private MessageDispacher mMessageDispacher;
    private int mOrientation = -1;
    private boolean mVisible;
    private TextView mZoomTextView;

    public V6SettingsStatusBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mEvTextView = (TextView) findChildrenById(R.id.ev_text);
        this.mZoomTextView = (TextView) findChildrenById(R.id.zoom_text);
        this.mApertureTextView = (TextView) findChildrenById(R.id.aperture_text);
        this.mMarginTop = getResources().getDimensionPixelOffset(R.dimen.v6_setting_status_bar_margin_top);
        this.mMarginTopLandscape = getResources().getDimensionPixelOffset(R.dimen.v6_setting_status_bar_margin_top_landscape);
    }

    public void show() {
        setVisibility(0);
        this.mVisible = true;
    }

    public void hide() {
        this.mVisible = false;
        setVisibility(8);
    }

    public void setMessageDispacher(MessageDispacher p) {
        super.setMessageDispacher(p);
        this.mMessageDispacher = p;
    }

    public void animateIn(Runnable callback) {
        if (getVisibility() != 0 || !this.mVisible) {
            if (getVisibility() != 0) {
                setVisibility(0);
            }
            animate().withLayer().alpha(1.0f).setDuration(200).setInterpolator(new DecelerateInterpolator()).withEndAction(callback).start();
            this.mVisible = true;
        }
    }

    public void animateOut(final Runnable callback) {
        this.mVisible = false;
        if (getVisibility() == 0) {
            animate().withLayer().alpha(0.0f).setDuration(200).setInterpolator(new DecelerateInterpolator()).withEndAction(new Runnable() {
                public void run() {
                    if (!V6SettingsStatusBar.this.mVisible) {
                        V6SettingsStatusBar.this.setVisibility(8);
                    }
                    if (callback != null) {
                        callback.run();
                    }
                    V6SettingsStatusBar.this.setAlpha(1.0f);
                }
            }).start();
        }
    }

    public void updateStatus() {
        updateEV();
        updateZoom();
        updateAperture();
    }

    public void updateStatus(String key) {
        if ("pref_camera_zoom_key".equals(key)) {
            updateZoom();
        } else if ("pref_camera_exposure_key".equals(key)) {
            updateEV();
        } else if ("pref_camera_stereo_key".equals(key)) {
            updateAperture();
        }
    }

    public void updateEV() {
        float ev = ((float) CameraSettings.readExposure(CameraSettingPreferences.instance())) * CameraManager.instance().getStashParameters().getExposureCompensationStep();
        if (Math.abs(ev) <= 0.05f || CameraSettings.isSupportedPortrait()) {
            setSubViewVisible(this.mEvTextView, 8, true);
            return;
        }
        String symbol = ev < 0.0f ? "-" : "+";
        String text = String.format(Locale.ENGLISH, "%s %.1f", new Object[]{symbol, Float.valueOf(Math.abs(ev))});
        boolean same = this.mEvTextView.getText().equals(text);
        this.mEvTextView.setText(text);
        setSubViewVisible(this.mEvTextView, 0, same);
    }

    public void updateZoom() {
        boolean zoomButtonInVisible;
        Parameters parameters = CameraManager.instance().getStashParameters();
        if (((ActivityBase) this.mContext).getUIController().getZoomButton().getVisibility() == 8) {
            zoomButtonInVisible = true;
        } else {
            zoomButtonInVisible = false;
        }
        if ((zoomButtonInVisible || CameraSettings.isSwitchCameraZoomMode()) && parameters != null) {
            int value = ((Integer) parameters.getZoomRatios().get(CameraSettings.readZoom(CameraSettingPreferences.instance()))).intValue();
            if (value > 100) {
                value /= 10;
                int zoomFraction = value % 10;
                String text = "x " + (value / 10) + "." + zoomFraction;
                boolean same = this.mZoomTextView.getText().equals(text);
                this.mZoomTextView.setText(text);
                setSubViewVisible(this.mZoomTextView, 0, same);
                return;
            }
        }
        setSubViewVisible(this.mZoomTextView, 8, true);
    }

    public void updateAperture() {
        if (Device.isSupportedStereo() && CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            String text = ((ActivityBase) this.mContext).getUIController().getPreferenceGroup().findPreference("pref_camera_stereo_key").getEntry();
            boolean same = this.mApertureTextView.getText().equals(text);
            this.mApertureTextView.setText(text);
            setSubViewVisible(this.mApertureTextView, 0, same);
            return;
        }
        setSubViewVisible(this.mApertureTextView, 8, true);
    }

    public void onCameraOpen() {
        boolean z = false;
        updateStatus();
        if (getVisibility() == 0) {
            z = true;
        }
        this.mVisible = z;
    }

    public void onResume() {
    }

    public void setOrientation(int degree, boolean animation) {
    }

    private void setSubViewVisible(View v, int visible, boolean sameContent) {
        if (visible != v.getVisibility() || !sameContent) {
            if (visible != 8) {
                show();
            }
            v.setVisibility(visible);
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(4, R.id.v6_setting_status_bar, 3, Boolean.valueOf(isSubViewShown()), null);
            }
        }
    }

    public boolean isSubViewShown() {
        for (View child : this.mChildren) {
            if (child.getVisibility() == 0) {
                return true;
            }
        }
        return false;
    }
}
