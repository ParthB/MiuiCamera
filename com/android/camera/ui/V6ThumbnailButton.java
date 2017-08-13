package com.android.camera.ui;

import android.content.Context;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import com.android.camera.ActivityBase;
import com.android.camera.Thumbnail;

public class V6ThumbnailButton extends V6BottomAnimationViewGroup implements OnClickListener {
    private Handler mHandler = new Handler();
    public RotateImageView mImage;
    private MessageDispacher mMessageDispacher;
    private boolean mValideThumbnail;
    private boolean mVisible;

    public V6ThumbnailButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mImage = (RotateImageView) findViewById(R.id.v6_thumbnail_image);
        setOnClickListener(this);
        LayoutParams layout = this.mImage.getLayoutParams();
        int intrinsicWidth = this.mImage.getDrawable().getIntrinsicWidth();
        layout.width = intrinsicWidth;
        layout.height = intrinsicWidth;
        this.mImage.setLayoutParams(layout);
    }

    public void updateThumbnail(Thumbnail t, boolean animation, final Runnable callback) {
        if (t != null) {
            this.mImage.clearThumbs();
            this.mImage.setBitmap(t.getBitmap());
            if (animation) {
                ViewCompat.setAlpha(this, 0.3f);
                ViewCompat.setScaleX(this, 0.5f);
                ViewCompat.setScaleY(this, 0.5f);
                ViewCompat.animate(this).alpha(1.0f).scaleX(1.0f).scaleY(1.0f).setDuration(80).setListener(new ViewPropertyAnimatorListenerAdapter() {
                    public void onAnimationEnd(View view) {
                        if (callback != null) {
                            V6ThumbnailButton.this.mHandler.post(callback);
                        }
                    }
                }).start();
            }
            this.mValideThumbnail = true;
            return;
        }
        this.mImage.setBitmap(null);
        this.mValideThumbnail = false;
    }

    public void onResume() {
        clearAnimation();
        ActivityBase activity = this.mContext;
        boolean z = (activity.isImageCaptureIntent() || activity.isVideoCaptureIntent()) ? false : true;
        this.mVisible = z;
        if (this.mVisible) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }

    public void onPause() {
    }

    public void onCameraOpen() {
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mImage.setOrientation(orientation, animation);
    }

    public void setVisibility(int visibility) {
        if (!this.mVisible) {
            visibility = 8;
        }
        super.setVisibility(visibility);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onClick(View v) {
        if (this.mValideThumbnail) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_thumbnail_button, 2, null, null);
        }
    }
}
