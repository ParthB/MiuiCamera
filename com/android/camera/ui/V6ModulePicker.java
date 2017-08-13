package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;

public class V6ModulePicker extends V6BottomAnimationImageView implements OnClickListener {
    private static final String TAG = V6ModulePicker.class.getSimpleName();
    private static int sCurrentModule = 0;
    private boolean mEnabled;
    private boolean mVisible;

    public V6ModulePicker(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(this);
    }

    public void onCreate() {
        boolean z = false;
        super.onCreate();
        ActivityBase activity = this.mContext;
        if (!(activity.isImageCaptureIntent() || activity.isVideoCaptureIntent())) {
            z = true;
        }
        this.mVisible = z;
        if (this.mVisible) {
            initModulePickView();
        }
    }

    public void onResume() {
        boolean z = false;
        super.onResume();
        ActivityBase activity = this.mContext;
        if (!(activity.isImageCaptureIntent() || activity.isVideoCaptureIntent())) {
            z = true;
        }
        this.mVisible = z;
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        setVisibility(0);
    }

    public void enableControls(boolean enable) {
        this.mEnabled = enable;
        setEnabled(enable);
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    public static boolean isVideoModule() {
        return sCurrentModule == 1;
    }

    public static boolean isCameraModule() {
        return sCurrentModule == 0;
    }

    public static boolean isPanoramaModule() {
        return sCurrentModule == 2;
    }

    public void onClick(View v) {
        Log.v(TAG, "ModulePicker onclick");
        int module = sCurrentModule == 1 ? 0 : 1;
        if (this.mMessageDispacher != null && this.mEnabled) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_module_picker, 2, Integer.valueOf(module), null);
        }
        AutoLockManager.getInstance(getContext()).onUserInteraction();
    }

    public static void setCurrentModule(int module) {
        sCurrentModule = module;
    }

    public static int getCurrentModule() {
        return sCurrentModule;
    }

    private void initModulePickView() {
        if (isVideoModule()) {
            setImageResource(R.drawable.video_module_picker_bg);
            setContentDescription(getResources().getString(R.string.accessibility_camera_module_picker));
            return;
        }
        setImageResource(R.drawable.camera_module_picker_bg);
        setContentDescription(getResources().getString(R.string.accessibility_video_module_picker));
    }
}
