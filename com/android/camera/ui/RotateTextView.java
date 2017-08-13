package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.animation.AnimationUtils;

public class RotateTextView extends TwoStateTextView implements Rotatable {
    private long mAnimationEndTime = 0;
    private long mAnimationStartTime = 0;
    private boolean mClockwise = false;
    private int mCurrentDegree = 0;
    private int mStartDegree = 0;
    private int mTargetDegree = 0;

    public RotateTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RotateTextView(Context context) {
        super(context);
    }

    public void setOrientation(int degree, boolean animation) {
        boolean z = false;
        degree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
        if (degree != this.mTargetDegree) {
            this.mTargetDegree = degree;
            if (animation) {
                this.mStartDegree = this.mCurrentDegree;
                this.mAnimationStartTime = AnimationUtils.currentAnimationTimeMillis();
                int diff = this.mTargetDegree - this.mCurrentDegree;
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
                this.mCurrentDegree = this.mTargetDegree;
            }
            invalidate();
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int w = getMeasuredWidth();
        int h = getMeasuredHeight();
        if (w != h) {
            int size;
            if (w > h) {
                size = w;
            } else {
                size = h;
            }
            setMeasuredDimension(size, size);
        }
    }

    protected void onDraw(Canvas canvas) {
        if (!TextUtils.isEmpty(getText())) {
            if (this.mCurrentDegree != this.mTargetDegree) {
                long time = AnimationUtils.currentAnimationTimeMillis();
                if (time < this.mAnimationEndTime) {
                    int deltaTime = (int) (time - this.mAnimationStartTime);
                    int i = this.mStartDegree;
                    if (!this.mClockwise) {
                        deltaTime = -deltaTime;
                    }
                    int degree = i + ((deltaTime * 270) / 1000);
                    this.mCurrentDegree = degree >= 0 ? degree % 360 : (degree % 360) + 360;
                    invalidate();
                } else {
                    this.mCurrentDegree = this.mTargetDegree;
                }
            }
            int saveCount = canvas.getSaveCount();
            int left = getPaddingLeft();
            int top = getPaddingTop();
            canvas.translate((float) ((((getWidth() - left) - getPaddingRight()) / 2) + left), (float) ((((getHeight() - top) - getPaddingBottom()) / 2) + top));
            canvas.rotate((float) (-this.mCurrentDegree));
            canvas.translate((float) ((-getWidth()) / 2), (float) ((-getHeight()) / 2));
            super.onDraw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }
}
