package com.android.camera.ui;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.recyclerview.R;
import android.view.ViewGroup;
import android.widget.TextView;
import com.android.camera.Util;

public class RotateTextToast {
    private static RotateTextToast sRotateTextToast;
    Handler mHandler = new Handler();
    ViewGroup mLayoutRoot;
    private final Runnable mRunnable = new Runnable() {
        public void run() {
            Util.fadeOut(RotateTextToast.this.mToast);
            RotateTextToast.this.mLayoutRoot.removeView(RotateTextToast.this.mToast);
            RotateTextToast.this.mToast = null;
            RotateTextToast.sRotateTextToast = null;
        }
    };
    RotateLayout mToast;

    public static RotateTextToast getInstance(Activity activity) {
        if (sRotateTextToast == null) {
            sRotateTextToast = new RotateTextToast(activity);
        }
        return sRotateTextToast;
    }

    public static RotateTextToast getInstance() {
        return sRotateTextToast;
    }

    public void show(int textResourceId, int orientation) {
        if (textResourceId == 0) {
            this.mHandler.removeCallbacks(this.mRunnable);
            this.mHandler.post(this.mRunnable);
            return;
        }
        ((TextView) this.mToast.findViewById(R.id.message)).setText(textResourceId);
        this.mToast.setOrientation(orientation, false);
        this.mToast.setVisibility(0);
        this.mHandler.removeCallbacks(this.mRunnable);
        this.mHandler.postDelayed(this.mRunnable, 5000);
    }

    private RotateTextToast(Activity activity) {
        this.mLayoutRoot = (ViewGroup) activity.getWindow().getDecorView();
        this.mToast = (RotateLayout) activity.getLayoutInflater().inflate(R.layout.rotate_text_toast, this.mLayoutRoot).findViewById(R.id.rotate_toast);
    }
}
