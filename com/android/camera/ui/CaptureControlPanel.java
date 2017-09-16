package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import com.android.camera.ActivityBase;

public class CaptureControlPanel extends RelativeLayout implements Rotatable, OnClickListener, V6FunctionUI {
    private V6BottomAnimationImageView mCancle;
    private V6BottomAnimationImageView mDone;
    private MessageDispacher mMessageDispacher;
    private boolean mVisible = true;

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public CaptureControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mDone = (V6BottomAnimationImageView) findViewById(R.id.v6_btn_done);
        this.mCancle = (V6BottomAnimationImageView) findViewById(R.id.v6_btn_cancel);
        this.mDone.setOnClickListener(this);
        this.mCancle.setOnClickListener(this);
    }

    public void onClick(View v) {
        if (this.mDone == v) {
            this.mMessageDispacher.dispacherMessage(0, R.id.capture_control_panel, 2, null, null);
        } else {
            this.mMessageDispacher.dispacherMessage(1, R.id.capture_control_panel, 2, null, null);
        }
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void onResume() {
        ActivityBase activity = (ActivityBase) getContext();
        this.mVisible = !activity.isImageCaptureIntent() ? activity.isVideoCaptureIntent() : true;
        if (this.mVisible) {
            setVisibility(0);
            this.mCancle.setVisibility(0);
        } else {
            setVisibility(8);
            this.mCancle.setVisibility(8);
        }
        this.mDone.setVisibility(8);
    }

    public void onPause() {
    }

    public void enableControls(boolean enable) {
    }

    public void setOrientation(int orientation, boolean animation) {
        if (this.mVisible) {
            this.mDone.setOrientation(orientation, animation);
            this.mCancle.setOrientation(orientation, animation);
        }
    }

    public V6BottomAnimationImageView getReviewDoneView() {
        return this.mDone;
    }

    public V6BottomAnimationImageView getReviewCanceledView() {
        return this.mCancle;
    }
}
