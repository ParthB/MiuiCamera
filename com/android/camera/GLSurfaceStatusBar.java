package com.android.camera;

import android.support.v7.recyclerview.R;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLPaint;
import java.util.Vector;

public class GLSurfaceStatusBar {
    private static final int PREVIEW_TOP_PADDING = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.camera_control_top_height);
    private static final int ZOOM_HINT_TEXT_WIDTH = (Util.dpToPixel(8.0f) * 5);
    private Vector<BasicTexture> mDrawVector = new Vector();
    private float mEV = 0.0f;
    private BasicTexture mEvTexture;
    private int mOrientation;
    private GLPaint mPaint = new GLPaint();
    private float mZoomScale = 1.0f;
    private BasicTexture mZoomTexture;

    public GLSurfaceStatusBar() {
        this.mPaint.setColor(1090519039);
        this.mPaint.setLineWidth(2.0f);
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public void release() {
        this.mZoomTexture = null;
        this.mEvTexture = null;
        this.mDrawVector.clear();
    }
}
