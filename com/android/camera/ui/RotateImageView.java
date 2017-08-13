package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AnimationUtils;
import android.widget.ImageView.ScaleType;
import com.android.camera.ForceScaleThumbnailUtils;

public class RotateImageView extends TwoStateImageView implements Rotatable {
    private long mAnimationEndTime = 0;
    private long mAnimationStartTime = 0;
    private int mAxisCurrentDegree = 0;
    private boolean mClockwise = false;
    private boolean mEnableAnimation = true;
    private OnRotateFinishedListener mOnRotateFinishedListener = null;
    private boolean mOverturn = false;
    private int mPointCurrentDegree = 0;
    private int mPointStartDegree = 0;
    private int mPointTargetDegree = 0;
    private Runnable mSwitchEnd = new Runnable() {
        public void run() {
            if (RotateImageView.this.mThumbs != null && RotateImageView.this.mThumbs[1] != null) {
                RotateImageView.this.setImageDrawable(RotateImageView.this.mThumbs[1]);
            }
        }
    };
    private Bitmap mThumb;
    private TransitionDrawable mThumbTransition;
    private Drawable[] mThumbs;

    public interface OnRotateFinishedListener {
        void OnRotateAxisFinished();

        void OnRotatePointFinished();
    }

    public RotateImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotateImageView(Context context) {
        super(context);
    }

    public void setOrientation(int degree, boolean animation) {
        boolean z = false;
        degree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
        if (degree != this.mPointTargetDegree) {
            this.mEnableAnimation = animation;
            this.mPointTargetDegree = degree;
            if (this.mEnableAnimation) {
                this.mPointStartDegree = this.mPointCurrentDegree;
                this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
                int diff = this.mPointTargetDegree - this.mPointCurrentDegree;
                if (diff < 0) {
                    diff += 360;
                }
                if (diff > 180) {
                    diff -= 360;
                }
                if (diff >= 0) {
                    z = true;
                }
                this.mClockwise = z;
                this.mAnimationEndTime = this.mAnimationStartTime + ((long) ((Math.abs(diff) * 1000) / 270));
            } else {
                this.mPointCurrentDegree = this.mPointTargetDegree;
            }
            invalidate();
        }
    }

    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        Matrix matrix = new Matrix();
        Camera camera = new Camera();
        if (drawable != null) {
            Rect bounds = drawable.getBounds();
            int w = bounds.right - bounds.left;
            int h = bounds.bottom - bounds.top;
            if (w != 0 && h != 0) {
                if (!this.mOverturn) {
                    if (this.mPointCurrentDegree != this.mPointTargetDegree) {
                        long time = AnimationUtils.currentAnimationTimeMillis();
                        if (time < this.mAnimationEndTime) {
                            int deltaTime = (int) (time - this.mAnimationStartTime);
                            int i = this.mPointStartDegree;
                            if (!this.mClockwise) {
                                deltaTime = -deltaTime;
                            }
                            int degree = i + ((deltaTime * 270) / 1000);
                            this.mPointCurrentDegree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
                            invalidate();
                        } else {
                            this.mPointCurrentDegree = this.mPointTargetDegree;
                            if (this.mOnRotateFinishedListener != null) {
                                this.mOnRotateFinishedListener.OnRotatePointFinished();
                            }
                        }
                    } else {
                        this.mEnableAnimation = true;
                    }
                } else if (this.mAxisCurrentDegree == 180) {
                    this.mOverturn = false;
                    this.mAxisCurrentDegree = 0;
                    if (this.mOnRotateFinishedListener != null) {
                        this.mOnRotateFinishedListener.OnRotateAxisFinished();
                    }
                } else {
                    this.mAxisCurrentDegree += 10;
                    invalidate();
                }
                int left = getPaddingLeft();
                int top = getPaddingTop();
                int width = (getWidth() - left) - getPaddingRight();
                int height = (getHeight() - top) - getPaddingBottom();
                int saveCount = canvas.getSaveCount();
                camera.save();
                if (this.mPointCurrentDegree == 0 || this.mPointCurrentDegree == 180) {
                    camera.rotateY((float) this.mAxisCurrentDegree);
                } else {
                    camera.rotateX((float) this.mAxisCurrentDegree);
                }
                camera.getMatrix(matrix);
                camera.restore();
                matrix.preTranslate((float) (-(w >> 1)), (float) (-(h >> 1)));
                matrix.postTranslate((float) (w >> 1), (float) (h >> 1));
                canvas.concat(matrix);
                if (getScaleType() == ScaleType.FIT_CENTER && (width < w || height < h)) {
                    float ratio = Math.min(((float) width) / ((float) w), ((float) height) / ((float) h));
                    canvas.scale(ratio, ratio, ((float) width) / 2.0f, ((float) height) / 2.0f);
                }
                canvas.translate((float) ((width / 2) + left), (float) ((height / 2) + top));
                canvas.rotate((float) (-this.mPointCurrentDegree));
                canvas.translate((float) ((-w) / 2), (float) ((-h) / 2));
                drawable.draw(canvas);
                canvas.restoreToCount(saveCount);
            }
        }
    }

    public void clearThumbs() {
        this.mThumbs = null;
    }

    public void setBitmap(Bitmap bitmap) {
        removeCallbacks(this.mSwitchEnd);
        if (bitmap == null) {
            this.mThumb = null;
            this.mThumbs = null;
            setImageResource(R.drawable.ic_thumbnail_background);
            return;
        }
        LayoutParams param = getLayoutParams();
        this.mThumb = ForceScaleThumbnailUtils.extractThumbnail(bitmap, (param.width - getPaddingLeft()) - getPaddingRight(), (param.height - getPaddingTop()) - getPaddingBottom());
        if (this.mThumbs != null && this.mEnableAnimation && isShown()) {
            this.mThumbs[0] = this.mThumbs[1];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            this.mThumbTransition = new TransitionDrawable(this.mThumbs);
            setImageDrawable(this.mThumbTransition);
            this.mThumbTransition.startTransition(500);
            postDelayed(this.mSwitchEnd, 520);
        } else {
            this.mThumbs = new Drawable[2];
            this.mThumbs[1] = new BitmapDrawable(getContext().getResources(), this.mThumb);
            setImageDrawable(this.mThumbs[1]);
        }
        setVisibility(0);
    }
}
