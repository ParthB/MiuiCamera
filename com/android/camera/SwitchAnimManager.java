package com.android.camera;

import android.os.SystemClock;
import android.util.Log;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;

public class SwitchAnimManager {
    private long mAnimStartTime;
    private float mExtScale = 1.0f;
    private Interpolator mInterpolator = new AccelerateInterpolator();
    private boolean mMoveBack;
    private boolean mNewPreview;
    private int mPreviewFrameLayoutWidth;
    private boolean mRealFirst;
    private boolean mRecurBlur;
    private int mReviewDrawingHeight;
    private int mReviewDrawingWidth;
    private int mReviewDrawingX;
    private int mReviewDrawingY;
    private int mSwitchState = 0;

    public void setReviewDrawingSize(int x, int y, int width, int height) {
        boolean z = false;
        this.mReviewDrawingX = x;
        this.mReviewDrawingY = y;
        this.mReviewDrawingWidth = width;
        this.mReviewDrawingHeight = height;
        this.mMoveBack = CameraSettings.isBackCamera();
        this.mNewPreview = false;
        if (((double) Math.abs((((float) this.mReviewDrawingHeight) / ((float) this.mReviewDrawingWidth)) - (((float) Util.sWindowHeight) / ((float) Util.sWindowWidth)))) < 0.02d) {
            z = true;
        }
        this.mRealFirst = z;
    }

    public void setPreviewFrameLayoutSize(int width, int height) {
        this.mPreviewFrameLayoutWidth = width;
    }

    public void startAnimation() {
        this.mAnimStartTime = SystemClock.uptimeMillis();
        this.mRecurBlur = true;
    }

    public void clearAnimation() {
        this.mAnimStartTime = 0;
        this.mRecurBlur = false;
    }

    public void startResume() {
        this.mAnimStartTime = SystemClock.uptimeMillis();
        this.mRecurBlur = false;
    }

    public boolean drawAnimationBlend(GLCanvas canvas, int x, int y, int width, int height, CameraScreenNail preview, RawTexture review) {
        boolean result = true;
        long timeDiff = SystemClock.uptimeMillis() - this.mAnimStartTime;
        float duration = this.mRecurBlur ? 200.0f : 1.0f;
        if (((float) timeDiff) > duration) {
            result = false;
            timeDiff = (long) duration;
        }
        float fraction = this.mInterpolator.getInterpolation(((float) timeDiff) / duration);
        if (!result && this.mRecurBlur) {
            this.mRecurBlur = false;
        }
        drawRealTimeTexture(canvas, x, y, width, height, preview, fraction);
        drawBlurTexture(canvas, x, y, width, height, preview, fraction);
        return result;
    }

    private void drawRealTimeTexture(GLCanvas canvas, int x, int y, int width, int height, CameraScreenNail preview, float fraction) {
        if (this.mNewPreview) {
            canvas.getState().pushState();
            canvas.getState().setAlpha(fraction);
            preview.directDraw(canvas, x, y, width, height);
            canvas.getState().popState();
        }
    }

    private void drawBlurTexture(GLCanvas canvas, int x, int y, int width, int height, CameraScreenNail preview, float fraction) {
        if (this.mRecurBlur) {
            preview.renderBlurTexture(canvas);
        }
        canvas.getState().pushState();
        if (this.mNewPreview) {
            canvas.getState().setBlendAlpha(1.0f - fraction);
        }
        preview.drawBlurTexture(canvas, this.mReviewDrawingX, this.mReviewDrawingY, this.mReviewDrawingWidth, this.mReviewDrawingHeight);
        canvas.getState().popState();
    }

    public boolean drawPreview(GLCanvas canvas, int x, int y, int width, int height, RawTexture review) {
        float centerX = ((float) x) + (((float) width) / 2.0f);
        float centerY = ((float) y) + (((float) height) / 2.0f);
        float scaleRatio = 1.0f;
        if (this.mPreviewFrameLayoutWidth != 0) {
            scaleRatio = ((float) width) / ((float) this.mPreviewFrameLayoutWidth);
        } else {
            Log.e("SwitchAnimManager", "mPreviewFrameLayoutWidth is 0.");
        }
        float reviewWidth = ((float) this.mReviewDrawingWidth) * scaleRatio;
        float reviewHeight = ((float) this.mReviewDrawingHeight) * scaleRatio;
        int reviewX = Math.round(centerX - (reviewWidth / 2.0f));
        int reviewY = Math.round(centerY - (reviewHeight / 2.0f));
        float alpha = canvas.getState().getAlpha();
        review.draw(canvas, reviewX, reviewY, Math.round(reviewWidth), Math.round(reviewHeight));
        canvas.getState().setAlpha(alpha);
        return true;
    }

    public boolean drawAnimation(GLCanvas canvas, int x, int y, int width, int height, CameraScreenNail preview, RawTexture review) {
        return drawAnimationBlend(canvas, x, y, width, height, preview, review);
    }

    public float getExtScaleX() {
        return this.mExtScale;
    }

    public float getExtScaleY() {
        return this.mExtScale;
    }

    public void restartPreview() {
        this.mNewPreview = true;
    }
}
