package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;
import com.android.camera.Util;

public class HorizontalSlideView extends View {
    private HorizontalDrawAdapter mDrawAdapter;
    private GestureDetector mGestureDetector;
    private OnGestureListener mGestureListener = new SimpleOnGestureListener() {
        public boolean onDown(MotionEvent e) {
            HorizontalSlideView.this.mScroller.forceFinished(true);
            HorizontalSlideView.this.mNeedJustify = false;
            return true;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            HorizontalSlideView.this.scroll((int) (e.getX() - HorizontalSlideView.this.mOriginX));
            return true;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (HorizontalSlideView.this.mPositionX == HorizontalSlideView.this.mMinX && distanceX < 0.0f) {
                return false;
            }
            if (HorizontalSlideView.this.mPositionX == HorizontalSlideView.this.mMaxX && distanceX > 0.0f) {
                return false;
            }
            HorizontalSlideView.this.setPositionX((int) (((float) HorizontalSlideView.this.mPositionX) + distanceX));
            return true;
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            HorizontalSlideView.this.flingX(-((int) velocityX));
            return true;
        }
    };
    private boolean mJustifyEnabled = true;
    private int mMaxX = 0;
    private int mMinX = 0;
    private boolean mNeedJustify;
    private OnItemSelectListener mOnItemSelectListener;
    private OnPositionSelectListener mOnPositionSelectListener;
    private float mOriginX;
    private int mPositionX = 0;
    private Scroller mScroller;
    private int mSelectedItemIndex;
    private boolean mSelectionFromSelf = false;

    public static abstract class HorizontalDrawAdapter {
        public abstract void draw(int i, Canvas canvas, boolean z);

        public abstract Align getAlign(int i);

        public abstract int getCount();

        public abstract float measureGap(int i);

        public abstract float measureWidth(int i);
    }

    public interface OnItemSelectListener {
        void onItemSelect(HorizontalSlideView horizontalSlideView, int i);
    }

    public interface OnPositionSelectListener {
        void onPositionSelect(HorizontalSlideView horizontalSlideView, float f);
    }

    protected void init(Context context) {
        this.mGestureDetector = new GestureDetector(context, this.mGestureListener);
        this.mGestureDetector.setIsLongpressEnabled(false);
        this.mScroller = new Scroller(context);
    }

    public HorizontalSlideView(Context context) {
        super(context);
        init(context);
    }

    public HorizontalSlideView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalSlideView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public void setOnItemSelectListener(OnItemSelectListener listener) {
        this.mOnItemSelectListener = listener;
    }

    public void setOnPositionSelectListener(OnPositionSelectListener listener) {
        this.mOnPositionSelectListener = listener;
    }

    public void setJustifyEnabled(boolean enable) {
        this.mJustifyEnabled = enable;
    }

    private float calculateLength(int fromIndex, int toIndex) {
        float fromPosition = 0.0f;
        float toPosition = 0.0f;
        float startX = this.mOriginX;
        float drawLeftX = 0.0f;
        float drawCenterX = startX;
        if (this.mDrawAdapter != null) {
            boolean rtl = Util.isLayoutRTL(getContext());
            int startIndex = rtl ? this.mDrawAdapter.getCount() - 1 : 0;
            int endIndex = rtl ? 0 : this.mDrawAdapter.getCount() - 1;
            int direction = rtl ? -1 : 1;
            for (int i = 0; i < this.mDrawAdapter.getCount(); i++) {
                int childIndex = startIndex + (i * direction);
                boolean isFirst = childIndex == startIndex;
                boolean isLast = childIndex == endIndex;
                float width = getItemWidth(childIndex);
                float halfWidth = width / 2.0f;
                if (isFirst) {
                    drawLeftX = this.mOriginX - halfWidth;
                }
                drawCenterX = isFirst ? startX : drawLeftX + halfWidth;
                drawLeftX += isLast ? 0.0f : getItemGap(i) + width;
                if (childIndex == fromIndex) {
                    fromPosition = drawCenterX;
                } else if (childIndex == toIndex) {
                    toPosition = drawCenterX;
                }
            }
        }
        return Math.abs(toPosition - fromPosition);
    }

    public void setDrawAdapter(HorizontalDrawAdapter adapter) {
        this.mDrawAdapter = adapter;
        this.mNeedJustify = false;
        this.mSelectedItemIndex = 0;
        this.mScroller.forceFinished(true);
        if (this.mDrawAdapter != null) {
            this.mMaxX = this.mMinX + ((int) calculateLength(0, this.mDrawAdapter.getCount() - 1));
        }
        if (Util.isLayoutRTL(getContext())) {
            this.mPositionX = this.mMaxX;
        } else {
            this.mPositionX = this.mMinX;
        }
        invalidate();
    }

    public void setSelection(int index) {
        if (this.mSelectedItemIndex != index) {
            this.mNeedJustify = false;
            this.mScroller.forceFinished(true);
            if (this.mDrawAdapter != null) {
                if (index >= this.mDrawAdapter.getCount()) {
                    index = this.mDrawAdapter.getCount() - 1;
                }
                if (Util.isLayoutRTL(getContext())) {
                    this.mPositionX = this.mMaxX - ((int) calculateLength(0, index));
                } else {
                    this.mPositionX = this.mMinX + ((int) calculateLength(0, index));
                }
            }
            select(index);
            invalidate();
        }
    }

    public void setSelection(float positionRatio) {
        if (Util.isLayoutRTL(getContext()) && this.mDrawAdapter != null) {
            positionRatio = 1.0f - positionRatio;
        }
        this.mNeedJustify = false;
        this.mScroller.forceFinished(true);
        this.mPositionX = (int) (((float) (this.mMaxX - this.mMinX)) * positionRatio);
        invalidate();
    }

    private void select(int index) {
        this.mSelectionFromSelf = true;
        if (this.mSelectedItemIndex != index) {
            this.mSelectedItemIndex = index;
            if (this.mOnItemSelectListener != null) {
                this.mOnItemSelectListener.onItemSelect(this, this.mSelectedItemIndex);
            }
        }
        if (this.mOnPositionSelectListener != null) {
            float ratio = ((float) this.mPositionX) / ((float) (this.mMaxX - this.mMinX));
            OnPositionSelectListener onPositionSelectListener = this.mOnPositionSelectListener;
            if (Util.isLayoutRTL(getContext())) {
                ratio = 1.0f - ratio;
            }
            onPositionSelectListener.onPositionSelect(this, ratio);
        }
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mOriginX = ((float) w) / 2.0f;
    }

    public boolean onTouchEvent(MotionEvent event) {
        boolean result = this.mGestureDetector.onTouchEvent(event);
        if (event.getAction() == 1) {
            this.mNeedJustify = true;
            invalidate();
        }
        return result;
    }

    private void setPositionX(int positionX) {
        this.mPositionX = positionX;
        if (this.mPositionX < this.mMinX) {
            this.mPositionX = this.mMinX;
        } else if (this.mPositionX > this.mMaxX) {
            this.mPositionX = this.mMaxX;
        }
        invalidate();
    }

    private void flingX(int velocityX) {
        this.mScroller.fling(this.mPositionX, 0, velocityX, 0, this.mMinX, this.mMaxX, 0, 0);
        invalidate();
    }

    private void scroll(int distance) {
        if (distance != 0) {
            if (this.mPositionX + distance < this.mMinX) {
                distance = this.mMinX - this.mPositionX;
            } else if (this.mPositionX + distance > this.mMaxX) {
                distance = this.mMaxX - this.mPositionX;
            }
            this.mScroller.startScroll(this.mPositionX, 0, distance, 0);
            invalidate();
        }
    }

    private float getItemGap(int index) {
        return this.mDrawAdapter.measureGap(index);
    }

    private float getItemWidth(int index) {
        return this.mDrawAdapter.measureWidth(index);
    }

    protected void onDraw(Canvas canvas) {
        if (this.mScroller.computeScrollOffset()) {
            this.mPositionX = this.mScroller.getCurrX();
            invalidate();
        }
        float startX = this.mOriginX - ((float) this.mPositionX);
        float drawLeftX = 0.0f;
        float drawCenterX = startX;
        float drawCenterY = ((float) getHeight()) / 2.0f;
        boolean nearestUnFound = true;
        float nearestDistance = 0.0f;
        float rightHalfMargin = 0.0f;
        if (this.mDrawAdapter != null) {
            int i;
            int childIndex;
            boolean isFirst;
            boolean isLast;
            float width;
            float halfWidth;
            float f;
            boolean rtl = Util.isLayoutRTL(getContext());
            int startIndex = rtl ? this.mDrawAdapter.getCount() - 1 : 0;
            int endIndex = rtl ? 0 : this.mDrawAdapter.getCount() - 1;
            int direction = rtl ? -1 : 1;
            for (i = 0; i < this.mDrawAdapter.getCount(); i++) {
                childIndex = startIndex + (i * direction);
                isFirst = childIndex == startIndex;
                isLast = childIndex == endIndex;
                width = getItemWidth(childIndex);
                halfWidth = width / 2.0f;
                float leftHalfMargin = isFirst ? 0.0f : rightHalfMargin;
                rightHalfMargin = isLast ? 0.0f : getItemGap(childIndex) / 2.0f;
                if (isFirst) {
                    drawLeftX = startX - halfWidth;
                }
                drawCenterX = isFirst ? startX : drawLeftX + halfWidth;
                if (nearestUnFound) {
                    float distance = drawCenterX - this.mOriginX;
                    if (distance > 0.0f || (-distance) > halfWidth + rightHalfMargin) {
                        if (distance > 0.0f && distance <= halfWidth + leftHalfMargin) {
                        }
                    }
                    select(childIndex);
                    nearestUnFound = false;
                    nearestDistance = distance;
                }
                if (isLast) {
                    f = 0.0f;
                } else {
                    f = getItemGap(childIndex) + width;
                }
                drawLeftX += f;
            }
            this.mMaxX = (int) (drawCenterX - startX);
            for (i = 0; i < this.mDrawAdapter.getCount(); i++) {
                childIndex = startIndex + (i * direction);
                isFirst = childIndex == startIndex;
                isLast = childIndex == endIndex;
                width = getItemWidth(childIndex);
                halfWidth = width / 2.0f;
                if (isFirst) {
                    drawLeftX = startX - halfWidth;
                }
                drawCenterX = isFirst ? startX : drawLeftX + halfWidth;
                if (drawLeftX + width >= 0.0f && drawLeftX <= ((float) getWidth())) {
                    canvas.save();
                    if (this.mDrawAdapter.getAlign(childIndex) == Align.LEFT) {
                        canvas.translate(drawLeftX, drawCenterY);
                    } else {
                        if (this.mDrawAdapter.getAlign(childIndex) == Align.CENTER) {
                            canvas.translate(drawCenterX, drawCenterY);
                        } else {
                            canvas.translate(drawLeftX + width, drawCenterY);
                        }
                    }
                    this.mDrawAdapter.draw(childIndex, canvas, this.mSelectedItemIndex == childIndex);
                    canvas.restore();
                }
                if (isLast) {
                    f = 0.0f;
                } else {
                    f = getItemGap(childIndex) + width;
                }
                drawLeftX += f;
            }
        }
        if (this.mJustifyEnabled && this.mNeedJustify && this.mScroller.isFinished()) {
            this.mNeedJustify = false;
            scroll((int) nearestDistance);
        }
    }
}
