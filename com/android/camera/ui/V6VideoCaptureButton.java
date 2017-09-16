package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;

public class V6VideoCaptureButton extends V6BottomAnimationImageView implements OnClickListener, V6FunctionUI {
    public V6VideoCaptureButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        setOnClickListener(this);
    }

    public void setVisibility(int visibility) {
        if (!(V6ModulePicker.isVideoModule() && !((ActivityBase) this.mContext).isVideoCaptureIntent() && CameraSettings.isVideoCaptureVisible())) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    public void onCameraOpen() {
        setVisibility(8);
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
    }

    public void onClick(View v) {
        if (this.mMessageDispacher != null && V6ModulePicker.isVideoModule()) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_video_capture_button, 2, null, null);
        }
    }
}
