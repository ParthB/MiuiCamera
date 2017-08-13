package com.android.zxing.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.Util;

public class QRCodeFragmentLayout extends RelativeLayout {
    private boolean mDispatchTouchEvent = false;
    private Animation mFadeHide;
    private Animation mFadeShow;
    private TextView mViewFinderButton;

    public QRCodeFragmentLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (this.mDispatchTouchEvent) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mFadeShow = AnimationUtils.loadAnimation(this.mContext, R.anim.qrcode_frament_layout_show);
        this.mFadeHide = AnimationUtils.loadAnimation(this.mContext, R.anim.qrcode_frament_layout_hide);
        this.mViewFinderButton = (TextView) findViewById(R.id.qrcode_viewfinder_button);
        this.mViewFinderButton.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if (((ActivityBase) QRCodeFragmentLayout.this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
                    QRCodeFragmentLayout.this.mViewFinderButton.setBackgroundResource(R.drawable.btn_camera_mode_exit_full_screen);
                } else {
                    QRCodeFragmentLayout.this.mViewFinderButton.setBackgroundResource(R.drawable.btn_camera_mode_exit);
                }
                Util.expandViewTouchDelegate(QRCodeFragmentLayout.this.mViewFinderButton);
            }
        });
    }
}
