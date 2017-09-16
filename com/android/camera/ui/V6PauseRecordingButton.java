package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.camera.Device;

public class V6PauseRecordingButton extends V6BottomAnimationImageView implements OnClickListener, V6FunctionUI {
    private boolean mVisible;

    public V6PauseRecordingButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void onResume() {
        super.onResume();
        setVisibility(0);
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    public void onCreate() {
        super.onCreate();
        this.mVisible = Device.isSupportedVideoPause() ? V6ModulePicker.isVideoModule() : false;
    }

    public void onClick(View v) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_video_pause_button, 2, null, null);
        }
    }
}
