package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;
import android.view.animation.DecelerateInterpolator;
import com.android.camera.Util;

public class OrientationIndicator extends View implements V6FunctionUI {
    private static final int TRIANGLE_BASE_DIS = Util.dpToPixel(5.0f);
    private static final int TRIANGLE_BASE_HEIGHT = Util.dpToPixel(5.0f);
    private static final int TRIANGLE_BASE_LEN = Util.dpToPixel(8.0f);
    private Drawable mCaptureBitmap;
    private Paint mIndicatorPaint;
    private Path mIndicatorPath;
    private boolean mVisible;

    public OrientationIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.translate((float) (getWidth() / 2), (float) (getHeight() / 2));
        this.mCaptureBitmap.draw(canvas);
        getIndicatorPath();
        canvas.drawPath(this.mIndicatorPath, this.mIndicatorPaint);
        canvas.translate((float) ((-getWidth()) / 2), (float) ((-getHeight()) / 2));
        canvas.restore();
    }

    private void getIndicatorPath() {
        if (this.mIndicatorPath == null) {
            this.mIndicatorPath = new Path();
            int leftx = (-TRIANGLE_BASE_LEN) / 2;
            int lefty = ((-this.mCaptureBitmap.getIntrinsicHeight()) / 2) - (TRIANGLE_BASE_DIS / 2);
            this.mIndicatorPath.moveTo((float) leftx, (float) lefty);
            this.mIndicatorPath.lineTo((float) (TRIANGLE_BASE_LEN + leftx), (float) lefty);
            this.mIndicatorPath.lineTo((float) ((TRIANGLE_BASE_LEN / 2) + leftx), (float) (lefty - TRIANGLE_BASE_HEIGHT));
            this.mIndicatorPath.lineTo((float) leftx, (float) lefty);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mCaptureBitmap = getResources().getDrawable(R.drawable.bg_capture);
        this.mCaptureBitmap.setFilterBitmap(true);
        this.mCaptureBitmap.setBounds((-this.mCaptureBitmap.getIntrinsicWidth()) / 2, (-this.mCaptureBitmap.getIntrinsicHeight()) / 2, this.mCaptureBitmap.getIntrinsicWidth() / 2, this.mCaptureBitmap.getIntrinsicHeight() / 2);
        this.mIndicatorPaint = new Paint();
        this.mIndicatorPaint.setColor(-1);
        this.mIndicatorPaint.setStyle(Style.FILL);
        this.mIndicatorPaint.setAntiAlias(true);
    }

    public void updateVisible(boolean visible) {
        float f = 0.0f;
        if (this.mVisible != visible) {
            this.mVisible = visible;
            Runnable r = null;
            if (!this.mVisible) {
                r = new Runnable() {
                    public void run() {
                        OrientationIndicator.this.setVisibility(8);
                    }
                };
            } else if (getVisibility() != 0) {
                setVisibility(0);
                setAlpha(0.0f);
            }
            animate().cancel();
            ViewPropertyAnimator animate = animate();
            if (this.mVisible) {
                f = 1.0f;
            }
            animate.alpha(f).setDuration(150).setInterpolator(new DecelerateInterpolator()).withEndAction(r).start();
        }
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void onResume() {
    }

    public void onPause() {
        updateVisible(false);
    }

    public void enableControls(boolean enabled) {
    }

    public void setMessageDispacher(MessageDispacher p) {
    }
}
