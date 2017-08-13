package com.android.camera.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.preferences.CameraSettingPreferences;

public class V6PreviewFrame extends RelativeLayout implements OnLayoutChangeListener, V6FunctionUI {
    private float mAspectRatio = 1.7777778f;
    private MessageDispacher mMessageDispacher;
    public SplitLineDrawer mReferenceGrid;

    public V6PreviewFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
        updateRefenceLineAccordSquare();
        updatePreviewGrid();
    }

    public void onResume() {
        if (!(getWidth() == 0 || getHeight() == 0)) {
            this.mAspectRatio = CameraSettings.getPreviewAspectRatio(getHeight(), getWidth());
        }
        if (!V6ModulePicker.isCameraModule()) {
            this.mReferenceGrid.setVisibility(8);
        }
    }

    public void onPause() {
    }

    public void enableControls(boolean enable) {
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mReferenceGrid = (SplitLineDrawer) findViewById(R.id.v6_reference_grid);
        this.mReferenceGrid.initialize(3, 3);
        this.mReferenceGrid.setBorderVisible(false, false);
        this.mReferenceGrid.setLineColor(-2130706433);
        addOnLayoutChangeListener(this);
    }

    public void setAspectRatio(float ratio) {
        if (((double) ratio) <= 0.0d) {
            throw new IllegalArgumentException();
        } else if (((double) Math.abs(this.mAspectRatio - ratio)) > 0.01d) {
            this.mAspectRatio = ratio;
        }
    }

    public void updateRefenceLineAccordSquare() {
        LayoutParams lp = (LayoutParams) this.mReferenceGrid.getLayoutParams();
        if (CameraSettings.isSwitchOn("pref_camera_square_mode_key")) {
            int margin = Util.sWindowWidth / 6;
            lp.topMargin = margin;
            lp.bottomMargin = margin;
        } else {
            lp.topMargin = 0;
            lp.bottomMargin = 0;
        }
        if (this.mReferenceGrid.getVisibility() == 0) {
            this.mReferenceGrid.requestLayout();
        }
    }

    public boolean isFullScreen() {
        if (Math.abs(this.mAspectRatio - CameraSettings.getPreviewAspectRatio(Util.sWindowHeight, Util.sWindowWidth)) < 0.1f || Math.abs(((double) this.mAspectRatio) - 1.5d) < 0.10000000149011612d || Math.abs(((double) this.mAspectRatio) - 1.7777777777777777d) < 0.10000000149011612d) {
            return true;
        }
        return false;
    }

    public void updatePreviewGrid() {
        if (isReferenceLineEnabled() && !((ActivityBase) getContext()).isScanQRCodeIntent() && V6ModulePicker.isCameraModule()) {
            this.mReferenceGrid.setVisibility(0);
        } else {
            this.mReferenceGrid.setVisibility(8);
        }
    }

    private boolean isReferenceLineEnabled() {
        return CameraSettingPreferences.instance().getBoolean("pref_camera_referenceline_key", false);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (right - left <= bottom - top) {
            this.mAspectRatio = CameraSettings.getPreviewAspectRatio(bottom - top, right - left);
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(1, R.id.v6_frame_layout, 2, v, new Rect(left, top, right, bottom));
                this.mMessageDispacher.dispacherMessage(1, R.id.v6_frame_layout, 3, v, new Rect(left, top, right, bottom));
            }
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_frame_layout, 2, new Point(w, h), new Point(oldw, oldh));
        }
    }
}
