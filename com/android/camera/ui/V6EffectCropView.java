package com.android.camera.ui;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.Keyframe;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.Interpolator;
import com.android.camera.ActivityBase;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import miui.view.animation.CubicEaseOutInterpolator;

public class V6EffectCropView extends View implements V6FunctionUI {
    private static final int ANIMATE_START_RADIUS = (Util.sWindowHeight / 2);
    private static final int ANIMATE_START_RANGE = Util.sWindowHeight;
    private static final int CIRCLE_RESIZE_TOUCH_TOLERANCE = Util.dpToPixel(36.0f);
    private static final int CORNER_BALL_RADIUS = Util.dpToPixel(5.0f);
    private static final int DEFAULT_RADIUS = (Util.sWindowHeight / 6);
    private static final int DEFAULT_RANGE = (Util.sWindowHeight / 3);
    private static final int MIN_CROP_WIDTH_HEIGHT = Util.dpToPixel(64.0f);
    private static final float MIN_DIS_FOR_MOVE_POINT = ((float) (Util.dpToPixel(30.0f) * Util.dpToPixel(30.0f)));
    private static final int MIN_DIS_FOR_SLOPE = (Util.dpToPixel(10.0f) * Util.dpToPixel(10.0f));
    private static final int MIN_RANGE = Util.dpToPixel(20.0f);
    private static final int TOUCH_TOLERANCE = Util.dpToPixel(18.0f);
    private Handler mAnimateHandler;
    private int mAnimateRadius = 0;
    private int mAnimateRangeWidth = 0;
    private HandlerThread mAnimateThread;
    private int mAnimationStartRadius;
    private int mAnimationStartRange;
    private long mAnimationStartTime;
    private long mAnimationTotalTime;
    private final Paint mBorderPaint = new Paint();
    private int mCenterLineSquare;
    private final Paint mCornerPaint;
    private final RectF mCropBounds = new RectF();
    private final RectF mDefaultCircleBounds = new RectF();
    private final RectF mDefaultRectBounds = new RectF();
    private final RectF mDisplayBounds = new RectF();
    private final PointF mEffectPoint1 = new PointF();
    private final PointF mEffectPoint2 = new PointF();
    private final RectF mEffectRect = new RectF(0.0f, 0.0f, 1.0f, 1.0f);
    private Interpolator mInterpolator = new CubicEaseOutInterpolator();
    private boolean mIsCircle;
    private boolean mIsInTapSlop;
    private boolean mIsRect;
    private boolean mIsTiltShift;
    private double mLastMoveDis;
    private float mLastX;
    private float mLastY;
    private int mMaxRange;
    private int mMovingEdges;
    private float mNormalizedWidth = 0.0f;
    private final Point mPoint1 = new Point();
    private final Point mPoint2 = new Point();
    private int mRadius = 0;
    private int mRangeWidth = 0;
    private int[] mRelativeLocation;
    private int mTapSlop;
    private boolean mTiltShiftMaskAlive;
    private ObjectAnimator mTiltShiftMaskFadeInAnimator;
    private ObjectAnimator mTiltShiftMaskFadeOutAnimator;
    private AnimatorListenerAdapter mTiltShiftMaskFadeOutListener = new AnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            if (V6EffectCropView.this.mTiltShiftMaskFadeOutAnimator.isRunning()) {
                V6EffectCropView.this.mTiltShiftMaskAlive = false;
            }
        }
    };
    private Handler mTiltShiftMaskHandler;
    private final Point mTouchCenter = new Point();
    private boolean mVisible;

    public V6EffectCropView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBorderPaint.setStyle(Style.STROKE);
        this.mBorderPaint.setColor(-1);
        this.mBorderPaint.setStrokeWidth((float) (Device.isPad() ? 4 : 2));
        this.mCornerPaint = new Paint();
        this.mCornerPaint.setAntiAlias(true);
        this.mCornerPaint.setColor(-1);
        ViewConfiguration configuration = ViewConfiguration.get(context);
        this.mTapSlop = configuration.getScaledTouchSlop() * configuration.getScaledTouchSlop();
        this.mTiltShiftMaskFadeInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.tilt_shift_mask_fade_in);
        this.mTiltShiftMaskFadeOutAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(context, R.anim.tilt_shift_mask_fade_out);
        this.mTiltShiftMaskFadeInAnimator.setTarget(EffectController.getInstance());
        ObjectAnimator objectAnimator = this.mTiltShiftMaskFadeInAnimator;
        PropertyValuesHolder[] propertyValuesHolderArr = new PropertyValuesHolder[1];
        propertyValuesHolderArr[0] = PropertyValuesHolder.ofKeyframe(this.mTiltShiftMaskFadeInAnimator.getPropertyName(), new Keyframe[]{Keyframe.ofFloat(0.0f), Keyframe.ofFloat(0.3f, 1.0f), Keyframe.ofFloat(1.0f, 1.0f)});
        objectAnimator.setValues(propertyValuesHolderArr);
        this.mTiltShiftMaskFadeOutAnimator.setTarget(EffectController.getInstance());
        this.mTiltShiftMaskFadeOutAnimator.addListener(this.mTiltShiftMaskFadeOutListener);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mDisplayBounds.set(0.0f, 0.0f, (float) w, (float) h);
        this.mDefaultRectBounds.set((float) ((w * 3) / 8), (float) ((h * 3) / 8), (float) ((w * 5) / 8), (float) ((h * 5) / 8));
        float radius = (float) DEFAULT_RADIUS;
        this.mDefaultCircleBounds.set((((float) w) / 2.0f) - radius, (((float) h) / 2.0f) - radius, (((float) w) / 2.0f) + radius, (((float) h) / 2.0f) + radius);
        this.mCropBounds.set(this.mIsRect ? this.mDefaultRectBounds : this.mDefaultCircleBounds);
        this.mPoint1.set(0, h / 2);
        this.mPoint2.set(w, h / 2);
        this.mMaxRange = (h * 2) / 3;
        this.mRangeWidth = this.mVisible ? DEFAULT_RANGE : ANIMATE_START_RANGE;
        this.mRelativeLocation = Util.getRelativeLocation(((ActivityBase) this.mContext).getUIController().getGLView(), this);
        onCropChange();
    }

    private void onCropChange() {
        float w = this.mDisplayBounds.width();
        float h = this.mDisplayBounds.height();
        this.mEffectRect.set(this.mCropBounds.left / w, this.mCropBounds.top / h, this.mCropBounds.right / w, this.mCropBounds.bottom / h);
        this.mEffectPoint1.set(((float) this.mPoint1.x) / w, ((float) this.mPoint1.y) / h);
        this.mEffectPoint2.set(((float) this.mPoint2.x) / w, ((float) this.mPoint2.y) / h);
        this.mCenterLineSquare = squareOfPoints(this.mPoint1, this.mPoint2);
        normalizeRangeWidth();
        EffectController.getInstance().setEffectAttribute(this.mEffectRect, this.mEffectPoint1, this.mEffectPoint2, this.mNormalizedWidth);
        if (this.mIsRect) {
            invalidate();
        }
    }

    private void normalizeRangeWidth() {
        Point point = computePointWithDistance(this.mRangeWidth);
        this.mNormalizedWidth = (float) Math.sqrt((double) getSquareOfDistance(((float) point.x) / this.mDisplayBounds.width(), ((float) point.y) / this.mDisplayBounds.height(), this.mEffectPoint1, this.mEffectPoint2, false));
    }

    public void removeTiltShiftMask() {
        if (this.mTiltShiftMaskHandler != null) {
            this.mTiltShiftMaskHandler.removeMessages(1);
            this.mTiltShiftMaskHandler.removeMessages(2);
        }
    }

    private void showTiltShiftMask() {
        this.mTiltShiftMaskHandler.sendEmptyMessage(1);
    }

    private void hideTiltShiftMask() {
        this.mTiltShiftMaskHandler.sendEmptyMessage(2);
    }

    private void detectMovingEdges(float x, float y) {
        this.mMovingEdges = 0;
        if (this.mIsRect) {
            if (y <= this.mCropBounds.bottom + ((float) TOUCH_TOLERANCE) && this.mCropBounds.top - ((float) TOUCH_TOLERANCE) <= y) {
                float left = Math.abs(x - this.mCropBounds.left);
                float right = Math.abs(x - this.mCropBounds.right);
                if (left <= ((float) TOUCH_TOLERANCE) && left < right) {
                    this.mMovingEdges |= 1;
                } else if (right <= ((float) TOUCH_TOLERANCE)) {
                    this.mMovingEdges |= 4;
                }
            }
            if (x <= this.mCropBounds.right + ((float) TOUCH_TOLERANCE) && this.mCropBounds.left - ((float) TOUCH_TOLERANCE) <= x) {
                float top = Math.abs(y - this.mCropBounds.top);
                float bottom = Math.abs(y - this.mCropBounds.bottom);
                if (((top < bottom ? 1 : 0) & (top <= ((float) TOUCH_TOLERANCE) ? 1 : 0)) != 0) {
                    this.mMovingEdges |= 2;
                } else if (bottom <= ((float) TOUCH_TOLERANCE)) {
                    this.mMovingEdges |= 8;
                }
            }
            if (this.mCropBounds.contains(x, y) && this.mMovingEdges == 0) {
                this.mMovingEdges = 16;
            }
        } else if (this.mIsCircle) {
            showTiltShiftMask();
            float centerX = this.mCropBounds.centerX();
            float centerY = this.mCropBounds.centerY();
            float radius = (this.mCropBounds.width() + this.mCropBounds.height()) / 4.0f;
            float toleranceSquare = (((float) CIRCLE_RESIZE_TOUCH_TOLERANCE) + radius) * (((float) CIRCLE_RESIZE_TOUCH_TOLERANCE) + radius);
            float distanceSquare = ((x - centerX) * (x - centerX)) + ((y - centerY) * (y - centerY));
            if (distanceSquare > radius * radius && distanceSquare <= toleranceSquare) {
                this.mMovingEdges = 32;
            }
            if (this.mCropBounds.contains(x, y) && this.mMovingEdges == 0) {
                this.mMovingEdges = 16;
            }
        } else {
            showTiltShiftMask();
            Point point = new Point((int) x, (int) y);
            this.mTouchCenter.set((this.mPoint1.x + this.mPoint2.x) / 2, (this.mPoint1.y + this.mPoint2.y) / 2);
            if (MIN_DIS_FOR_MOVE_POINT < ((float) this.mCenterLineSquare)) {
                if (squareOfPoints(point, this.mPoint1) < this.mCenterLineSquare / 16) {
                    this.mMovingEdges = 257;
                    return;
                }
            }
            if (MIN_DIS_FOR_MOVE_POINT < ((float) this.mCenterLineSquare)) {
                if (squareOfPoints(point, this.mPoint2) < this.mCenterLineSquare / 16) {
                    this.mMovingEdges = 258;
                    return;
                }
            }
            float touchDistance = getSquareOfDistance(x, y, new PointF(this.mPoint1), new PointF(this.mPoint2), false);
            if (touchDistance < ((float) ((this.mRangeWidth * this.mRangeWidth) / 9))) {
                this.mMovingEdges = 16;
                return;
            }
            this.mLastMoveDis = Math.sqrt((double) touchDistance);
            this.mMovingEdges = 260;
        }
    }

    private void moveEdges(float deltaX, float deltaY) {
        if (this.mMovingEdges == 16) {
            if (deltaX > 0.0f) {
                deltaX = Math.min(this.mDisplayBounds.right - this.mCropBounds.right, deltaX);
            } else {
                deltaX = Math.max(this.mDisplayBounds.left - this.mCropBounds.left, deltaX);
            }
            if (deltaY > 0.0f) {
                deltaY = Math.min(this.mDisplayBounds.bottom - this.mCropBounds.bottom, deltaY);
            } else {
                deltaY = Math.max(this.mDisplayBounds.top - this.mCropBounds.top, deltaY);
            }
            this.mCropBounds.offset(deltaX, deltaY);
        } else {
            float minWidth = (float) MIN_CROP_WIDTH_HEIGHT;
            float minHeight = (float) MIN_CROP_WIDTH_HEIGHT;
            if ((this.mMovingEdges & 1) != 0) {
                this.mCropBounds.left = Math.min(this.mCropBounds.left + deltaX, this.mCropBounds.right - minWidth);
            }
            if ((this.mMovingEdges & 2) != 0) {
                this.mCropBounds.top = Math.min(this.mCropBounds.top + deltaY, this.mCropBounds.bottom - minHeight);
            }
            if ((this.mMovingEdges & 4) != 0) {
                this.mCropBounds.right = Math.max(this.mCropBounds.right + deltaX, this.mCropBounds.left + minWidth);
            }
            if ((this.mMovingEdges & 8) != 0) {
                this.mCropBounds.bottom = Math.max(this.mCropBounds.bottom + deltaY, this.mCropBounds.top + minHeight);
            }
            this.mCropBounds.intersect(this.mDisplayBounds);
        }
        onCropChange();
    }

    private void moveCircle(float x, float y, float deltaX, float deltaY) {
        if (this.mMovingEdges == 16) {
            if (deltaX > 0.0f) {
                deltaX = Math.min(this.mDisplayBounds.right - this.mCropBounds.right, deltaX);
            } else {
                deltaX = Math.max(this.mDisplayBounds.left - this.mCropBounds.left, deltaX);
            }
            if (deltaY > 0.0f) {
                deltaY = Math.min(this.mDisplayBounds.bottom - this.mCropBounds.bottom, deltaY);
            } else {
                deltaY = Math.max(this.mDisplayBounds.top - this.mCropBounds.top, deltaY);
            }
            this.mCropBounds.offset(deltaX, deltaY);
        } else {
            float minRadius = (float) (MIN_CROP_WIDTH_HEIGHT / 2);
            float maxRadius = Math.min(this.mDisplayBounds.width(), this.mDisplayBounds.height()) / 2.0f;
            float centerX = this.mCropBounds.centerX();
            float centerY = this.mCropBounds.centerY();
            float newRadius = Math.min(maxRadius, Math.max(minRadius, (float) Math.sqrt((double) (((x - centerX) * (x - centerX)) + ((y - centerY) * (y - centerY))))));
            this.mCropBounds.set(centerX - newRadius, centerY - newRadius, centerX + newRadius, centerY + newRadius);
        }
        onCropChange();
    }

    private void moveCrop(float x, float y, float deltaX, float deltaY) {
        if (this.mMovingEdges == 260) {
            double currentDis = Math.sqrt((double) getSquareOfDistance(x, y, new PointF(this.mPoint1), new PointF(this.mPoint2), false));
            this.mRangeWidth = Util.clamp(this.mRangeWidth + ((int) (currentDis - this.mLastMoveDis)), MIN_RANGE, this.mMaxRange);
            this.mLastMoveDis = currentDis;
        } else if (this.mMovingEdges == 257 || this.mMovingEdges == 258) {
            computeCertenLineCrossPoints(this.mTouchCenter, new Point((int) x, (int) y));
        } else if (this.mMovingEdges == 16) {
            computeCertenLineCrossPoints(new Point(this.mPoint1.x + ((int) deltaX), this.mPoint1.y + ((int) deltaY)), new Point(this.mPoint2.x + ((int) deltaX), this.mPoint2.y + ((int) deltaY)));
        }
        onCropChange();
    }

    public boolean onViewTouchEvent(MotionEvent event) {
        if (!this.mVisible) {
            return false;
        }
        if (isEnabled()) {
            float x = event.getX() - ((float) this.mRelativeLocation[0]);
            float y = event.getY() - ((float) this.mRelativeLocation[1]);
            switch (event.getAction() & 255) {
                case 0:
                    detectMovingEdges(x, y);
                    this.mIsInTapSlop = true;
                    this.mLastX = x;
                    this.mLastY = y;
                    break;
                case 1:
                case 3:
                case 5:
                    this.mMovingEdges = 0;
                    hideTiltShiftMask();
                    invalidate();
                    break;
                case 2:
                    float deltaX = x - this.mLastX;
                    float deltaY = y - this.mLastY;
                    if (this.mIsInTapSlop && ((float) this.mTapSlop) < (deltaX * deltaX) + (deltaY * deltaY)) {
                        this.mIsInTapSlop = false;
                    }
                    if (!this.mIsInTapSlop) {
                        if (this.mMovingEdges != 0) {
                            if (this.mIsRect) {
                                moveEdges(x - this.mLastX, y - this.mLastY);
                            } else if (this.mIsCircle) {
                                moveCircle(x, y, x - this.mLastX, y - this.mLastY);
                            } else {
                                moveCrop(x, y, x - this.mLastX, y - this.mLastY);
                            }
                        }
                        this.mLastX = x;
                        this.mLastY = y;
                        break;
                    }
                    break;
            }
        }
        return true;
    }

    protected void onDraw(Canvas canvas) {
        if (this.mVisible && this.mIsRect) {
            canvas.drawRect(this.mCropBounds, this.mBorderPaint);
            canvas.drawCircle(this.mCropBounds.left, this.mCropBounds.top, (float) CORNER_BALL_RADIUS, this.mCornerPaint);
            canvas.drawCircle(this.mCropBounds.right, this.mCropBounds.top, (float) CORNER_BALL_RADIUS, this.mCornerPaint);
            canvas.drawCircle(this.mCropBounds.left, this.mCropBounds.bottom, (float) CORNER_BALL_RADIUS, this.mCornerPaint);
            canvas.drawCircle(this.mCropBounds.right, this.mCropBounds.bottom, (float) CORNER_BALL_RADIUS, this.mCornerPaint);
        }
    }

    public void show() {
        show(EffectController.getInstance().getEffect(false));
    }

    private static boolean isTiltShift(int index) {
        if (index == EffectController.sTiltShiftIndex || index == EffectController.sGaussianIndex) {
            return true;
        }
        return false;
    }

    private static boolean isRect(int index) {
        return !isTiltShift(index);
    }

    private static boolean isCircle(int index) {
        return index == EffectController.sGaussianIndex;
    }

    public void show(int index) {
        if (EffectController.getInstance().isNeedRect(index)) {
            if (this.mVisible && this.mIsRect == isRect(index)) {
                if (this.mIsCircle == isCircle(index)) {
                    return;
                }
            }
            this.mVisible = true;
            this.mMovingEdges = 0;
            setVisibility(0);
            this.mIsRect = isRect(index);
            this.mIsCircle = isCircle(index);
            this.mIsTiltShift = isTiltShift(index);
            if (this.mIsTiltShift) {
                this.mPoint1.set(0, ((int) this.mDisplayBounds.height()) / 2);
                this.mPoint2.set((int) this.mDisplayBounds.width(), ((int) this.mDisplayBounds.height()) / 2);
                this.mRangeWidth = ANIMATE_START_RANGE;
                this.mRadius = ANIMATE_START_RADIUS;
                this.mAnimationStartTime = System.currentTimeMillis();
                this.mAnimationTotalTime = 600;
                this.mAnimateRangeWidth = DEFAULT_RANGE - this.mRangeWidth;
                this.mAnimationStartRange = this.mRangeWidth;
                this.mAnimateRadius = DEFAULT_RADIUS - this.mRadius;
                this.mAnimationStartRadius = this.mRadius;
                float centerX = this.mDefaultCircleBounds.centerX();
                float centerY = this.mDefaultCircleBounds.centerY();
                this.mCropBounds.set(centerX - ((float) this.mRadius), centerY - ((float) this.mRadius), ((float) this.mRadius) + centerX, ((float) this.mRadius) + centerY);
                showTiltShiftMask();
                if (EffectController.sTiltShiftIndex == index) {
                    this.mAnimateHandler.sendEmptyMessage(1);
                } else if (EffectController.sGaussianIndex == index) {
                    this.mAnimateHandler.sendEmptyMessage(2);
                }
                invalidate();
            } else {
                this.mCropBounds.set(this.mDefaultRectBounds);
                setLayerType(2, null);
            }
            EffectController.getInstance().setInvertFlag(0);
            onCropChange();
        }
    }

    public boolean isVisible() {
        return this.mVisible;
    }

    public boolean isMoved() {
        return (this.mIsInTapSlop || this.mMovingEdges == 0) ? false : true;
    }

    public void hide() {
        if (this.mVisible) {
            this.mVisible = false;
            setVisibility(4);
            EffectController.getInstance().clearEffectAttribute();
            EffectController.getInstance().setInvertFlag(0);
        }
    }

    public void updateVisible() {
        updateVisible(EffectController.getInstance().getEffect(false));
    }

    public void updateVisible(int index) {
        if (EffectController.getInstance().isNeedRect(index) && V6ModulePicker.isCameraModule()) {
            show(index);
        } else {
            hide();
        }
    }

    public void onCreate() {
        initHandler();
    }

    public void onCameraOpen() {
        updateVisible();
    }

    public void onResume() {
    }

    public void onPause() {
        if (this.mAnimateHandler != null && this.mAnimateHandler.hasMessages(1)) {
            if (this.mAnimateHandler.hasMessages(1)) {
                this.mAnimateHandler.removeMessages(1);
                this.mRangeWidth = this.mAnimationStartRange + this.mAnimateRangeWidth;
            }
            if (this.mAnimateHandler.hasMessages(2)) {
                this.mAnimateHandler.removeMessages(2);
                this.mRadius = this.mAnimationStartRadius + this.mAnimateRadius;
            }
        }
    }

    public void enableControls(boolean enabled) {
    }

    public void setMessageDispacher(MessageDispacher p) {
    }

    private float getSquareOfDistance(float x, float y, PointF point1, PointF point2, boolean segment) {
        float x1 = point1.x;
        float y1 = point1.y;
        float x2 = point2.x;
        float y2 = point2.y;
        if (x1 == x2) {
            return (x - x1) * (x - x1);
        }
        if (y1 == y2) {
            return (y - y1) * (y - y1);
        }
        float cross = ((x2 - x1) * (x - x1)) + ((y2 - y1) * (y - y1));
        if (segment && ((double) cross) <= 0.0d) {
            return ((x - x1) * (x - x1)) + ((y - y1) * (y - y1));
        }
        float d2 = ((x2 - x1) * (x2 - x1)) + ((y2 - y1) * (y2 - y1));
        if (segment && cross >= d2) {
            return ((x - x2) * (x - x2)) + ((y - y2) * (y - y2));
        }
        float r = cross / d2;
        float px = x1 + ((x2 - x1) * r);
        float py = y1 + ((y2 - y1) * r);
        return ((x - px) * (x - px)) + ((py - y) * (py - y));
    }

    private void computeCertenLineCrossPoints(Point point1, Point point2) {
        if (squareOfPoints(point1, point2) >= MIN_DIS_FOR_SLOPE) {
            int w = (int) this.mDisplayBounds.width();
            int h = (int) this.mDisplayBounds.height();
            int x;
            if (point1.x == point2.x) {
                x = Util.clamp(point1.x, 0, w);
                this.mPoint1.set(x, 0);
                this.mPoint2.set(x, h);
            } else if (point1.y == point2.y) {
                y = Util.clamp(point1.y, 0, h);
                this.mPoint1.set(0, y);
                this.mPoint2.set(w, y);
            } else {
                int validIndex;
                Point[] tmpPoint = new Point[2];
                int validIndex2 = 0;
                float slope = ((float) (point2.y - point1.y)) / ((float) (point2.x - point1.x));
                x = (int) (((float) point1.x) - (((float) point1.y) / slope));
                if (x >= 0 && x <= w) {
                    validIndex2 = 1;
                    tmpPoint[0] = new Point(x, 0);
                }
                x = (int) (((float) point1.x) + (((float) (h - point1.y)) / slope));
                if (x >= 0 && x <= w) {
                    validIndex = validIndex2 + 1;
                    tmpPoint[validIndex2] = new Point(x, h);
                    validIndex2 = validIndex;
                }
                y = (int) (((float) point1.y) - (((float) point1.x) * slope));
                if (y >= 0 && y <= h && !isContained(tmpPoint, 0, y)) {
                    validIndex = validIndex2 + 1;
                    tmpPoint[validIndex2] = new Point(0, y);
                    validIndex2 = validIndex;
                }
                y = (int) (((float) point1.y) + (((float) (w - point1.x)) * slope));
                if (y < 0 || y > h || isContained(tmpPoint, w, y)) {
                    validIndex = validIndex2;
                } else {
                    validIndex = validIndex2 + 1;
                    tmpPoint[validIndex2] = new Point(w, y);
                }
                if (validIndex == 1) {
                    validIndex2 = validIndex + 1;
                    tmpPoint[validIndex] = new Point(tmpPoint[0]);
                } else {
                    validIndex2 = validIndex;
                }
                if (validIndex2 == 2 && MIN_CROP_WIDTH_HEIGHT * MIN_CROP_WIDTH_HEIGHT <= squareOfPoints(tmpPoint[0], tmpPoint[1])) {
                    this.mPoint1.set(tmpPoint[0].x, tmpPoint[0].y);
                    this.mPoint2.set(tmpPoint[1].x, tmpPoint[1].y);
                }
            }
        }
    }

    private boolean isContained(Point[] points, int x, int y) {
        if (!(points == null || points.length == 0)) {
            for (Point exist : points) {
                if (exist == null) {
                    return false;
                }
                if (exist.x == x || exist.y == y) {
                    return true;
                }
            }
        }
        return false;
    }

    private Point computePointWithDistance(int distance) {
        Point point = new Point();
        if (this.mPoint1.x == this.mPoint2.x) {
            point.set(this.mPoint1.x - distance, this.mPoint1.y);
        } else if (this.mPoint1.y == this.mPoint2.y) {
            point.set(this.mPoint1.x, this.mPoint1.y - distance);
        } else {
            float centerDistance = (float) Math.sqrt((double) this.mCenterLineSquare);
            point.set(this.mPoint1.x + ((int) (((float) ((this.mPoint1.y - this.mPoint2.y) * distance)) / centerDistance)), this.mPoint1.y - ((int) (((float) ((this.mPoint1.x - this.mPoint2.x) * distance)) / centerDistance)));
        }
        return point;
    }

    private int squareOfPoints(Point point1, Point point2) {
        int dx = point1.x - point2.x;
        int dy = point1.y - point2.y;
        return (dx * dx) + (dy * dy);
    }

    private void initHandler() {
        if (this.mTiltShiftMaskHandler == null) {
            this.mTiltShiftMaskHandler = new Handler(Looper.getMainLooper()) {
                public void dispatchMessage(Message msg) {
                    switch (msg.what) {
                        case 1:
                            V6EffectCropView.this.mTiltShiftMaskFadeOutAnimator.cancel();
                            if (!V6EffectCropView.this.mTiltShiftMaskAlive) {
                                V6EffectCropView.this.mTiltShiftMaskAlive = true;
                                V6EffectCropView.this.mTiltShiftMaskFadeInAnimator.setupStartValues();
                                V6EffectCropView.this.mTiltShiftMaskFadeInAnimator.start();
                                return;
                            }
                            return;
                        case 2:
                            if (V6EffectCropView.this.mTiltShiftMaskFadeInAnimator.isRunning()) {
                                V6EffectCropView.this.mTiltShiftMaskFadeOutAnimator.setStartDelay(V6EffectCropView.this.mTiltShiftMaskFadeInAnimator.getDuration() - V6EffectCropView.this.mTiltShiftMaskFadeInAnimator.getCurrentPlayTime());
                            } else {
                                V6EffectCropView.this.mTiltShiftMaskFadeOutAnimator.setStartDelay(0);
                            }
                            if (V6EffectCropView.this.mTiltShiftMaskAlive) {
                                V6EffectCropView.this.mTiltShiftMaskFadeOutAnimator.start();
                                return;
                            }
                            return;
                        default:
                            return;
                    }
                }
            };
        }
        if (this.mAnimateHandler == null) {
            this.mAnimateThread = new HandlerThread("animateThread");
            this.mAnimateThread.start();
            this.mAnimateHandler = new Handler(this.mAnimateThread.getLooper()) {
                public void dispatchMessage(Message msg) {
                    long duration = System.currentTimeMillis() - V6EffectCropView.this.mAnimationStartTime;
                    float delta = 1.0f;
                    switch (msg.what) {
                        case 1:
                            if (duration < 600) {
                                delta = V6EffectCropView.this.mInterpolator.getInterpolation(((float) duration) / ((float) V6EffectCropView.this.mAnimationTotalTime));
                                sendEmptyMessageDelayed(1, 30);
                            } else {
                                V6EffectCropView.this.hideTiltShiftMask();
                            }
                            V6EffectCropView.this.mRangeWidth = V6EffectCropView.this.mAnimationStartRange + ((int) (((float) V6EffectCropView.this.mAnimateRangeWidth) * delta));
                            V6EffectCropView.this.onCropChange();
                            return;
                        case 2:
                            if (duration < 600) {
                                delta = V6EffectCropView.this.mInterpolator.getInterpolation(((float) duration) / ((float) V6EffectCropView.this.mAnimationTotalTime));
                                sendEmptyMessageDelayed(2, 30);
                            } else {
                                V6EffectCropView.this.hideTiltShiftMask();
                            }
                            float centerX = V6EffectCropView.this.mDefaultCircleBounds.centerX();
                            float centerY = V6EffectCropView.this.mDefaultCircleBounds.centerY();
                            V6EffectCropView.this.mRadius = V6EffectCropView.this.mAnimationStartRadius + ((int) (((float) V6EffectCropView.this.mAnimateRadius) * delta));
                            V6EffectCropView.this.mCropBounds.set(centerX - ((float) V6EffectCropView.this.mRadius), centerY - ((float) V6EffectCropView.this.mRadius), ((float) V6EffectCropView.this.mRadius) + centerX, ((float) V6EffectCropView.this.mRadius) + centerY);
                            V6EffectCropView.this.onCropChange();
                            return;
                        default:
                            return;
                    }
                }
            };
        }
    }

    public void onDestory() {
        if (this.mAnimateThread != null) {
            this.mAnimateThread.quit();
            this.mAnimateThread = null;
            this.mAnimateHandler = null;
        }
    }
}
