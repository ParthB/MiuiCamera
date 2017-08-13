package com.android.camera.ui;

import android.app.Activity;
import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.widget.FrameLayout;

public class V6SurfaceManager implements Callback {
    private Activity mActivity;
    private boolean mInitialized;
    private MessageDispacher mMessageDispacher;
    private SurfaceHolder mSurfaceHolder;
    private FrameLayout mSurfaceParent;
    private SurfaceView mSurfaceView;

    public V6SurfaceManager(Context context, FrameLayout surfaceViewParent) {
        this.mActivity = (Activity) context;
        this.mSurfaceParent = surfaceViewParent;
    }

    public void initializeSurfaceView() {
        Log.v("V6SurfaceManager", "initializeSurfaceView mSurfaceView=" + this.mSurfaceView + " mInitialized=" + this.mInitialized);
        this.mSurfaceParent.setVisibility(0);
        if (this.mSurfaceView == null) {
            this.mSurfaceView = (SurfaceView) this.mSurfaceParent.findViewById(R.id.v6_surfaceview);
            if (this.mSurfaceView == null) {
                this.mActivity.getLayoutInflater().inflate(R.layout.v6_surface_view, this.mSurfaceParent);
                this.mSurfaceView = (SurfaceView) this.mSurfaceParent.findViewById(R.id.v6_surfaceview);
            }
            if (!this.mInitialized && this.mSurfaceView != null) {
                this.mSurfaceView.setVisibility(0);
                SurfaceHolder surfaceHolder = this.mSurfaceView.getHolder();
                surfaceHolder.addCallback(this);
                surfaceHolder.setType(3);
                surfaceHolder.setFormat(-2);
                this.mInitialized = true;
                Log.v("V6SurfaceManager", "Using mdp_preview_content (MDP path)");
                return;
            }
            return;
        }
        this.mSurfaceView.setVisibility(0);
    }

    public void setSurfaceViewVisible(boolean visible) {
        int i;
        int i2 = 0;
        FrameLayout frameLayout = this.mSurfaceParent;
        if (visible) {
            i = 0;
        } else {
            i = 8;
        }
        frameLayout.setVisibility(i);
        if (this.mSurfaceView != null) {
            SurfaceView surfaceView = this.mSurfaceView;
            if (!visible) {
                i2 = 8;
            }
            surfaceView.setVisibility(i2);
        }
    }

    public SurfaceHolder getSurfaceHolder() {
        return this.mSurfaceHolder;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.v("V6SurfaceManager", "surfaceChanged: width = " + width + ", height = " + height + " mSurfaceHolder=" + this.mSurfaceHolder + " holder=" + holder);
        this.mSurfaceHolder = holder;
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.v("V6SurfaceManager", "surfaceCreated");
        this.mSurfaceHolder = holder;
        this.mMessageDispacher.dispacherMessage(0, R.id.v6_surfaceview, 2, null, null);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.v("V6SurfaceManager", "surfaceDestroyed");
        this.mSurfaceHolder = null;
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public boolean isSurfaceViewVisible() {
        return this.mSurfaceView != null && this.mSurfaceView.getVisibility() == 0;
    }
}
