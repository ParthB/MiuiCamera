package com.android.camera.ui;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnLayoutChangeListener;
import android.widget.FrameLayout;

public class V6SurfaceViewFrame extends FrameLayout implements OnLayoutChangeListener, V6FunctionUI {
    private MessageDispacher mMessageDispacher;
    private V6SurfaceManager mSurfaceManager;

    public V6SurfaceViewFrame(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void onResume() {
    }

    public void onPause() {
    }

    public void enableControls(boolean enable) {
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        addOnLayoutChangeListener(this);
        this.mSurfaceManager = new V6SurfaceManager(this.mContext, this);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
        this.mSurfaceManager.setMessageDispacher(p);
    }

    public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(1, R.id.v6_frame_layout, 2, v, new Rect(left, top, right, bottom));
            this.mMessageDispacher.dispacherMessage(1, R.id.v6_frame_layout, 3, v, new Rect(left, top, right, bottom));
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_frame_layout, 2, new Point(w, h), new Point(oldw, oldh));
        }
    }

    public void initSurfaceView() {
        this.mSurfaceManager.initializeSurfaceView();
    }

    public SurfaceHolder getSurfaceHolder() {
        return this.mSurfaceManager.getSurfaceHolder();
    }

    public boolean isSurfaceViewVisible() {
        return this.mSurfaceManager.isSurfaceViewVisible();
    }

    public boolean isSurfaceViewAvailable() {
        return isSurfaceViewVisible() && getSurfaceHolder() != null;
    }

    public void setSurfaceViewVisible(boolean visible) {
        this.mSurfaceManager.setSurfaceViewVisible(visible);
    }
}
