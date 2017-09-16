package com.android.camera.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.os.Build.VERSION;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.VelocityTracker;
import android.view.View;
import android.view.View.BaseSavedState;
import android.view.View.MeasureSpec;
import android.view.View.OnLongClickListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Scroller;
import java.security.InvalidParameterException;

public class ScreenView extends ViewGroup {
    protected static final int INDICATOR_MEASURE_SPEC = MeasureSpec.makeMeasureSpec(0, 0);
    protected static final LayoutParams SEEK_POINT_LAYOUT_PARAMS = new LayoutParams(-1, -1, 1.0f);
    private static final float SMOOTHING_CONSTANT = ((float) (0.016d / Math.log(0.75d)));
    protected final float DEFAULT_CAMERA_DISTANCE;
    private boolean isFromcomputeScroll;
    protected int mActivePointerId;
    private boolean mAllowLongPress;
    private ArrowIndicator mArrowLeft;
    private int mArrowLeftOffResId;
    private int mArrowLeftOnResId;
    private ArrowIndicator mArrowRight;
    private int mArrowRightOffResId;
    private int mArrowRightOnResId;
    protected int mChildScreenWidth;
    private float mConfirmHorizontalScrollRatio;
    private boolean mCurrentGestureFinished;
    protected int mCurrentScreen;
    protected boolean mFirstLayout;
    GestureVelocityTracker mGestureVelocityTracker;
    protected int mHeightMeasureSpec;
    private int mIndicatorCount;
    protected float mLastMotionX;
    protected float mLastMotionY;
    protected OnLongClickListener mLongClickListener;
    private int mMaximumVelocity;
    protected int mNextScreen;
    protected float mOverScrollRatio;
    private float mOvershootTension;
    private ScaleGestureDetector mScaleDetector;
    protected int mScreenAlignment;
    private int mScreenCounter;
    protected int mScreenOffset;
    protected int mScreenPaddingBottom;
    protected int mScreenPaddingTop;
    protected SeekBarIndicator mScreenSeekBar;
    private int mScreenSnapDuration;
    private int mScreenTransitionType;
    protected int mScreenWidth;
    private ScreenViewOvershootInterpolator mScrollInterpolator;
    protected int mScrollLeftBound;
    protected int mScrollOffset;
    protected int mScrollRightBound;
    protected boolean mScrollWholeScreen;
    protected Scroller mScroller;
    private int mSeekPointResId;
    protected SlideBar mSlideBar;
    private float mSmoothingTime;
    private boolean mTouchIntercepted;
    private int mTouchSlop;
    private int mTouchState;
    private float mTouchX;
    protected int mVisibleRange;
    protected int mWidthMeasureSpec;

    private interface Indicator {
        boolean fastOffset(int i);
    }

    protected class ArrowIndicator extends ImageView implements Indicator {
        public boolean fastOffset(int offset) {
            if (this.mLeft == offset) {
                return false;
            }
            this.mRight = (this.mRight + offset) - this.mLeft;
            this.mLeft = offset;
            return true;
        }
    }

    private class GestureVelocityTracker {
        private float mFoldX;
        private int mPointerId;
        private float mPrevX;
        private float mStartX;
        private VelocityTracker mVelocityTracker;

        private GestureVelocityTracker() {
            this.mPointerId = -1;
            this.mStartX = -1.0f;
            this.mFoldX = -1.0f;
            this.mPrevX = -1.0f;
        }

        public void recycle() {
            if (this.mVelocityTracker != null) {
                this.mVelocityTracker.recycle();
                this.mVelocityTracker = null;
            }
            reset();
        }

        public void addMovement(MotionEvent ev) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            }
            this.mVelocityTracker.addMovement(ev);
            float curX = ev.getX();
            if (this.mPointerId != -1) {
                int pIndex = ev.findPointerIndex(this.mPointerId);
                if (pIndex != -1) {
                    curX = ev.getX(pIndex);
                } else {
                    this.mPointerId = -1;
                }
            }
            if (this.mStartX < 0.0f) {
                this.mStartX = curX;
            } else if (this.mPrevX < 0.0f) {
                this.mPrevX = curX;
            } else {
                if (this.mFoldX < 0.0f) {
                    if (this.mPrevX <= this.mStartX || curX >= this.mPrevX) {
                        if (this.mPrevX < this.mStartX && curX > this.mPrevX) {
                        }
                    }
                    if (Math.abs(curX - this.mStartX) > 3.0f) {
                        this.mFoldX = this.mPrevX;
                    }
                } else if (this.mFoldX != this.mPrevX) {
                    if (this.mPrevX <= this.mFoldX || curX >= this.mPrevX) {
                        if (this.mPrevX < this.mFoldX && curX > this.mPrevX) {
                        }
                    }
                    if (Math.abs(curX - this.mFoldX) > 3.0f) {
                        this.mStartX = this.mFoldX;
                        this.mFoldX = this.mPrevX;
                    }
                }
                this.mPrevX = curX;
            }
        }

        private void reset() {
            this.mPointerId = -1;
            this.mStartX = -1.0f;
            this.mFoldX = -1.0f;
            this.mPrevX = -1.0f;
        }

        public void init(int pointerId) {
            if (this.mVelocityTracker == null) {
                this.mVelocityTracker = VelocityTracker.obtain();
            } else {
                this.mVelocityTracker.clear();
            }
            reset();
            this.mPointerId = pointerId;
        }

        public float getXVelocity(int units, int maxVelocity, int pointerId) {
            this.mVelocityTracker.computeCurrentVelocity(units, (float) maxVelocity);
            return this.mVelocityTracker.getXVelocity(pointerId);
        }

        public int getFlingDirection(float velocity) {
            int i = 1;
            if (velocity <= 300.0f) {
                return 4;
            }
            if (this.mFoldX >= 0.0f) {
                return this.mPrevX < this.mFoldX ? ScreenView.this.mScrollX < ScreenView.this.getCurrentScreen().getLeft() ? 3 : 2 : (this.mPrevX <= this.mFoldX || ScreenView.this.mScrollX > ScreenView.this.getCurrentScreen().getLeft()) ? 3 : 1;
            } else {
                if (this.mPrevX <= this.mStartX) {
                    i = 2;
                }
                return i;
            }
        }
    }

    public static class SavedState extends BaseSavedState {
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
        int currentScreen;

        SavedState(Parcelable superState) {
            super(superState);
            this.currentScreen = -1;
        }

        private SavedState(Parcel in) {
            super(in);
            this.currentScreen = -1;
            this.currentScreen = in.readInt();
        }

        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(this.currentScreen);
        }
    }

    private class ScaleDetectorListener implements OnScaleGestureListener {
        private ScaleDetectorListener() {
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return ScreenView.this.mTouchState == 0;
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            ScreenView.this.finishCurrentGesture();
        }

        public boolean onScale(ScaleGestureDetector detector) {
            float scale = detector.getScaleFactor();
            if (ScreenView.this.mTouchState == 0) {
                if (((float) detector.getTimeDelta()) <= 200.0f && scale >= 0.95f) {
                    if (scale > 1.0526316f) {
                    }
                }
                ScreenView.this.setTouchState(null, 4);
            }
            if (scale < 0.8f) {
                ScreenView.this.onPinchIn(detector);
                return true;
            } else if (scale <= 1.2f) {
                return false;
            } else {
                ScreenView.this.onPinchOut(detector);
                return true;
            }
        }
    }

    private class ScreenViewOvershootInterpolator implements Interpolator {
        private float mTension;

        public ScreenViewOvershootInterpolator() {
            this.mTension = ScreenView.this.mOvershootTension;
        }

        public void setDistance(int distance, int velocity) {
            float -get0;
            if (distance > 0) {
                -get0 = ScreenView.this.mOvershootTension / ((float) distance);
            } else {
                -get0 = ScreenView.this.mOvershootTension;
            }
            this.mTension = -get0;
        }

        public void disableSettle() {
            this.mTension = 0.0f;
        }

        public float getInterpolation(float t) {
            t -= 1.0f;
            return ((t * t) * (((this.mTension + 1.0f) * t) + this.mTension)) + 1.0f;
        }
    }

    protected class SeekBarIndicator extends LinearLayout implements Indicator {
        public SeekBarIndicator(Context context) {
            super(context);
            setDrawingCacheEnabled(true);
        }

        public boolean fastOffset(int offset) {
            if (this.mLeft == offset) {
                return false;
            }
            this.mRight = (this.mRight + offset) - this.mLeft;
            this.mLeft = offset;
            return true;
        }
    }

    protected class SlideBar extends FrameLayout implements Indicator {
        private Rect mPadding;
        private Rect mPos;
        private NinePatch mSlidePoint;

        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            if (this.mSlidePoint != null) {
                this.mSlidePoint.draw(canvas, this.mPos);
            }
        }

        protected boolean setFrame(int left, int top, int right, int bottom) {
            boolean r = super.setFrame(left, top, right, bottom);
            if (this.mSlidePoint != null) {
                this.mPos.bottom = (bottom - top) - this.mPadding.bottom;
                this.mPos.top = this.mPos.bottom - this.mSlidePoint.getHeight();
            }
            return r;
        }

        public void setPosition(int left, int right) {
            this.mPos.left = this.mPadding.left + left;
            this.mPos.right = this.mPadding.left + right;
        }

        public int getSlideWidth() {
            return (getMeasuredWidth() - this.mPadding.left) - this.mPadding.right;
        }

        public boolean fastOffset(int offset) {
            if (this.mLeft == offset) {
                return false;
            }
            this.mRight = (this.mRight + offset) - this.mLeft;
            this.mLeft = offset;
            return true;
        }
    }

    public void setMaximumSnapVelocity(int velocity) {
        this.mMaximumVelocity = velocity;
    }

    public ScreenView(Context context) {
        super(context);
        this.mFirstLayout = true;
        this.mArrowLeftOnResId = R.drawable.screen_view_arrow_left;
        this.mArrowLeftOffResId = R.drawable.screen_view_arrow_left_gray;
        this.mArrowRightOnResId = R.drawable.screen_view_arrow_right;
        this.mArrowRightOffResId = R.drawable.screen_view_arrow_right_gray;
        this.mSeekPointResId = R.drawable.screen_view_seek_point_selector;
        this.mVisibleRange = 1;
        this.mScreenWidth = 0;
        this.mNextScreen = -1;
        this.mOverScrollRatio = 0.33333334f;
        this.mScrollWholeScreen = true;
        this.mScreenCounter = 0;
        this.mTouchState = 0;
        this.isFromcomputeScroll = false;
        this.mAllowLongPress = true;
        this.mActivePointerId = -1;
        this.mConfirmHorizontalScrollRatio = 0.5f;
        this.mScreenSnapDuration = 300;
        this.mScreenTransitionType = 0;
        this.mOvershootTension = 1.3f;
        this.mGestureVelocityTracker = new GestureVelocityTracker();
        this.DEFAULT_CAMERA_DISTANCE = Resources.getSystem().getDisplayMetrics().density * 1280.0f;
        initScreenView();
    }

    public ScreenView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScreenView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.mFirstLayout = true;
        this.mArrowLeftOnResId = R.drawable.screen_view_arrow_left;
        this.mArrowLeftOffResId = R.drawable.screen_view_arrow_left_gray;
        this.mArrowRightOnResId = R.drawable.screen_view_arrow_right;
        this.mArrowRightOffResId = R.drawable.screen_view_arrow_right_gray;
        this.mSeekPointResId = R.drawable.screen_view_seek_point_selector;
        this.mVisibleRange = 1;
        this.mScreenWidth = 0;
        this.mNextScreen = -1;
        this.mOverScrollRatio = 0.33333334f;
        this.mScrollWholeScreen = true;
        this.mScreenCounter = 0;
        this.mTouchState = 0;
        this.isFromcomputeScroll = false;
        this.mAllowLongPress = true;
        this.mActivePointerId = -1;
        this.mConfirmHorizontalScrollRatio = 0.5f;
        this.mScreenSnapDuration = 300;
        this.mScreenTransitionType = 0;
        this.mOvershootTension = 1.3f;
        this.mGestureVelocityTracker = new GestureVelocityTracker();
        this.DEFAULT_CAMERA_DISTANCE = Resources.getSystem().getDisplayMetrics().density * 1280.0f;
        initScreenView();
    }

    private void initScreenView() {
        setAlwaysDrawnWithCacheEnabled(true);
        setClipToPadding(true);
        this.mScrollInterpolator = new ScreenViewOvershootInterpolator();
        this.mScroller = new Scroller(this.mContext, this.mScrollInterpolator);
        setCurrentScreenInner(0);
        ViewConfiguration configuration = ViewConfiguration.get(this.mContext);
        this.mTouchSlop = configuration.getScaledTouchSlop();
        setMaximumSnapVelocity(configuration.getScaledMaximumFlingVelocity());
        this.mScaleDetector = new ScaleGestureDetector(this.mContext, new ScaleDetectorListener());
    }

    public void setSeekPointResource(int seekPointResId) {
        this.mSeekPointResId = seekPointResId;
    }

    public void setSeekBarPosition(FrameLayout.LayoutParams params) {
        if (params != null) {
            if (this.mScreenSeekBar == null) {
                this.mScreenSeekBar = new SeekBarIndicator(this.mContext);
                this.mScreenSeekBar.setGravity(16);
                this.mScreenSeekBar.setAnimationCacheEnabled(false);
                addIndicator(this.mScreenSeekBar, params);
                return;
            }
            this.mScreenSeekBar.setLayoutParams(params);
        } else if (this.mScreenSeekBar != null) {
            removeIndicator(this.mScreenSeekBar);
            this.mScreenSeekBar = null;
        }
    }

    private void updateScreenOffset() {
        switch (this.mScreenAlignment) {
            case 0:
                this.mScrollOffset = this.mScreenOffset;
                break;
            case 1:
                this.mScrollOffset = 0;
                break;
            case 2:
                this.mScrollOffset = (this.mScreenWidth - this.mChildScreenWidth) / 2;
                break;
            case 3:
                this.mScrollOffset = this.mScreenWidth - this.mChildScreenWidth;
                break;
        }
        this.mScrollOffset += this.mPaddingLeft;
    }

    private void updateIndicatorPositions(int scrollX) {
        if (getWidth() > 0) {
            int indexOffset = getScreenCount();
            int screenWidth = getWidth();
            int screenHeight = getHeight();
            for (int i = 0; i < this.mIndicatorCount; i++) {
                View indicator = getChildAt(i + indexOffset);
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) indicator.getLayoutParams();
                int indicatorWidth = indicator.getMeasuredWidth();
                int indicatorHeight = indicator.getMeasuredHeight();
                int indicatorLeft = 0;
                int indicatorTop = 0;
                int gravity = lp.gravity;
                if (gravity != -1) {
                    int verticalGravity = gravity & 112;
                    switch (gravity & 7) {
                        case 1:
                            indicatorLeft = (((screenWidth - indicatorWidth) / 2) + lp.leftMargin) - lp.rightMargin;
                            break;
                        case 3:
                            indicatorLeft = lp.leftMargin;
                            break;
                        case 5:
                            indicatorLeft = (screenWidth - indicatorWidth) - lp.rightMargin;
                            break;
                        default:
                            indicatorLeft = lp.leftMargin;
                            break;
                    }
                    switch (verticalGravity) {
                        case 16:
                            indicatorTop = (((screenHeight - indicatorHeight) / 2) + lp.topMargin) - lp.bottomMargin;
                            break;
                        case 48:
                            indicatorTop = lp.topMargin;
                            break;
                        case 80:
                            indicatorTop = (screenHeight - indicatorHeight) - lp.bottomMargin;
                            break;
                        default:
                            indicatorTop = lp.topMargin;
                            break;
                    }
                }
                if ((indicator.isLayoutRequested() || indicator.getHeight() <= 0 || indicator.getWidth() <= 0) && !this.isFromcomputeScroll) {
                    if (VERSION.SDK_INT > 16) {
                        scrollX = 0;
                    }
                    indicator.layout(scrollX + indicatorLeft, indicatorTop, (scrollX + indicatorLeft) + indicatorWidth, indicatorTop + indicatorHeight);
                } else if (VERSION.SDK_INT > 16) {
                    indicator.setTranslationX((float) scrollX);
                } else if (((Indicator) indicator).fastOffset(scrollX + indicatorLeft)) {
                    indicator.invalidate();
                }
            }
        }
    }

    private void updateSlidePointPosition(int scrollX) {
        int screenCount = getScreenCount();
        if (this.mSlideBar != null && screenCount > 0) {
            int slidePointX;
            int slideBarWidth = this.mSlideBar.getSlideWidth();
            int slidePointWidth = Math.max((slideBarWidth / screenCount) * this.mVisibleRange, 48);
            int screenViewContentWidth = this.mChildScreenWidth * screenCount;
            if (screenViewContentWidth <= slideBarWidth) {
                slidePointX = 0;
            } else {
                slidePointX = ((slideBarWidth - slidePointWidth) * scrollX) / (screenViewContentWidth - slideBarWidth);
            }
            this.mSlideBar.setPosition(slidePointX, slidePointX + slidePointWidth);
            if (isHardwareAccelerated()) {
                this.mSlideBar.invalidate();
            }
        }
    }

    private void updateArrowIndicatorResource(int x) {
        if (this.mArrowLeft != null) {
            int i;
            ArrowIndicator arrowIndicator = this.mArrowLeft;
            if (x <= 0) {
                i = this.mArrowLeftOffResId;
            } else {
                i = this.mArrowLeftOnResId;
            }
            arrowIndicator.setImageResource(i);
            arrowIndicator = this.mArrowRight;
            if (x >= ((getScreenCount() * this.mChildScreenWidth) - this.mScreenWidth) - this.mScrollOffset) {
                i = this.mArrowRightOffResId;
            } else {
                i = this.mArrowRightOnResId;
            }
            arrowIndicator.setImageResource(i);
        }
    }

    public void setOverScrollRatio(float ratio) {
        this.mOverScrollRatio = ratio;
        refreshScrollBound();
    }

    private void refreshScrollBound() {
        this.mScrollLeftBound = ((int) (((float) (-this.mChildScreenWidth)) * this.mOverScrollRatio)) - this.mScrollOffset;
        if (this.mScrollWholeScreen) {
            this.mScrollRightBound = (int) (((float) (((getScreenCount() - 1) / this.mVisibleRange) * this.mScreenWidth)) + (((float) this.mChildScreenWidth) * this.mOverScrollRatio));
        } else {
            this.mScrollRightBound = ((int) ((((float) this.mChildScreenWidth) * (((float) getScreenCount()) + this.mOverScrollRatio)) - ((float) this.mScreenWidth))) + this.mScrollOffset;
        }
    }

    public void scrollToScreen(int index) {
        if (this.mScrollWholeScreen) {
            index -= index % this.mVisibleRange;
        }
        measure(this.mWidthMeasureSpec, this.mHeightMeasureSpec);
        scrollTo((this.mChildScreenWidth * index) - this.mScrollOffset, 0);
    }

    public void scrollTo(int x, int y) {
        this.mTouchX = (float) Math.max(this.mScrollLeftBound, Math.min(x, this.mScrollRightBound));
        this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
        super.scrollTo((int) this.mTouchX, y);
    }

    public void computeScroll() {
        this.isFromcomputeScroll = true;
        if (this.mScroller.computeScrollOffset()) {
            int currX = this.mScroller.getCurrX();
            this.mScrollX = currX;
            this.mTouchX = (float) currX;
            this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
            this.mScrollY = this.mScroller.getCurrY();
            postInvalidate();
        } else if (this.mNextScreen != -1) {
            setCurrentScreenInner(Math.max(0, Math.min(this.mNextScreen, getScreenCount() - 1)));
            this.mNextScreen = -1;
        } else if (this.mTouchState == 1) {
            float now = ((float) System.nanoTime()) / 1.0E9f;
            float dx = this.mTouchX - ((float) this.mScrollX);
            this.mScrollX = (int) (((float) this.mScrollX) + (dx * ((float) Math.exp((double) ((now - this.mSmoothingTime) / SMOOTHING_CONSTANT)))));
            this.mSmoothingTime = now;
            if (dx > 1.0f || dx < -1.0f) {
                postInvalidate();
            }
        }
        updateIndicatorPositions(this.mScrollX);
        updateSlidePointPosition(this.mScrollX);
        updateArrowIndicatorResource(this.mScrollX);
        this.isFromcomputeScroll = false;
    }

    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        computeScroll();
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int i;
        this.mWidthMeasureSpec = widthMeasureSpec;
        this.mHeightMeasureSpec = heightMeasureSpec;
        int maxHeight = 0;
        int maxWidth = 0;
        int count = getScreenCount();
        for (i = 0; i < this.mIndicatorCount; i++) {
            View child = getChildAt(i + count);
            ViewGroup.LayoutParams lp = child.getLayoutParams();
            child.measure(getChildMeasureSpec(widthMeasureSpec, this.mPaddingLeft + this.mPaddingRight, lp.width), getChildMeasureSpec(heightMeasureSpec, ((this.mPaddingTop + this.mScreenPaddingTop) + this.mPaddingBottom) + this.mScreenPaddingBottom, lp.height));
            maxWidth = Math.max(maxWidth, child.getMeasuredWidth());
            maxHeight = Math.max(maxHeight, child.getMeasuredHeight());
        }
        int maxChildHeight = 0;
        int maxChildWidth = 0;
        for (i = 0; i < count; i++) {
            child = getChildAt(i);
            lp = child.getLayoutParams();
            child.measure(getChildMeasureSpec(widthMeasureSpec, this.mPaddingLeft + this.mPaddingRight, lp.width), getChildMeasureSpec(heightMeasureSpec, ((this.mPaddingTop + this.mScreenPaddingTop) + this.mPaddingBottom) + this.mScreenPaddingBottom, lp.height));
            maxChildWidth = Math.max(maxChildWidth, child.getMeasuredWidth());
            maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
        }
        maxWidth = Math.max(maxChildWidth, maxWidth);
        setMeasuredDimension(resolveSize(maxWidth + (this.mPaddingLeft + this.mPaddingRight), widthMeasureSpec), resolveSize(Math.max(maxChildHeight, maxHeight) + (((this.mPaddingTop + this.mScreenPaddingTop) + this.mPaddingBottom) + this.mScreenPaddingBottom), heightMeasureSpec));
        if (count > 0) {
            this.mChildScreenWidth = maxChildWidth;
            this.mScreenWidth = (MeasureSpec.getSize(widthMeasureSpec) - this.mPaddingLeft) - this.mPaddingRight;
            updateScreenOffset();
            setOverScrollRatio(this.mOverScrollRatio);
            if (this.mChildScreenWidth > 0) {
                this.mVisibleRange = Math.max(1, (this.mScreenWidth + (this.mChildScreenWidth / 2)) / this.mChildScreenWidth);
            }
        }
        if (this.mFirstLayout && this.mVisibleRange > 0) {
            this.mFirstLayout = false;
            setHorizontalScrollBarEnabled(false);
            setCurrentScreen(this.mCurrentScreen);
            setHorizontalScrollBarEnabled(true);
        }
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        setFrame(left, top, right, bottom);
        left += this.mPaddingLeft;
        right -= this.mPaddingRight;
        updateIndicatorPositions(this.mScrollX);
        int count = getScreenCount();
        int childLeft = 0;
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != 8) {
                child.layout(childLeft, this.mPaddingTop + this.mScreenPaddingTop, child.getMeasuredWidth() + childLeft, (this.mPaddingTop + this.mScreenPaddingTop) + child.getMeasuredHeight());
                childLeft += child.getMeasuredWidth();
            }
        }
        if (this.mScrollWholeScreen && this.mCurrentScreen % this.mVisibleRange > 0) {
            setCurrentScreen(this.mCurrentScreen - (this.mCurrentScreen % this.mVisibleRange));
        }
    }

    protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
        updateChildStaticTransformation(child);
        return super.drawChild(canvas, child, drawingTime);
    }

    public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
        int screen = indexOfChild(child);
        if (screen >= getScreenCount()) {
            return super.requestChildRectangleOnScreen(child, rectangle, immediate);
        }
        if (screen == this.mCurrentScreen && this.mScroller.isFinished()) {
            return false;
        }
        snapToScreen(screen);
        return true;
    }

    public boolean dispatchUnhandledMove(View focused, int direction) {
        if (direction == 17) {
            if (this.mCurrentScreen > 0) {
                snapToScreen(this.mCurrentScreen - 1);
                return true;
            }
        } else if (direction == 66 && this.mCurrentScreen < getScreenCount() - 1) {
            snapToScreen(this.mCurrentScreen + 1);
            return true;
        }
        return super.dispatchUnhandledMove(focused, direction);
    }

    protected void setTouchState(MotionEvent ev, int touchState) {
        boolean z;
        this.mTouchState = touchState;
        ViewParent parent = getParent();
        if (this.mTouchState != 0) {
            z = true;
        } else {
            z = false;
        }
        parent.requestDisallowInterceptTouchEvent(z);
        if (this.mTouchState == 0) {
            this.mActivePointerId = -1;
            this.mAllowLongPress = false;
            this.mGestureVelocityTracker.recycle();
            return;
        }
        if (ev != null) {
            this.mActivePointerId = ev.getPointerId(0);
        }
        if (this.mAllowLongPress) {
            this.mAllowLongPress = false;
            View currentScreen = getChildAt(this.mCurrentScreen);
            if (currentScreen != null) {
                currentScreen.cancelLongPress();
            }
        }
        if (this.mTouchState == 1) {
            this.mLastMotionX = ev.getX(ev.findPointerIndex(this.mActivePointerId));
            this.mTouchX = (float) this.mScrollX;
            this.mSmoothingTime = ((float) System.nanoTime()) / 1.0E9f;
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction() & 255) {
            case 0:
                ev.setAction(3);
                this.mScaleDetector.onTouchEvent(ev);
                ev.setAction(0);
                this.mCurrentGestureFinished = false;
                this.mTouchIntercepted = false;
                this.mLastMotionX = ev.getX();
                this.mLastMotionY = ev.getY();
                if (!this.mScroller.isFinished()) {
                    this.mScroller.abortAnimation();
                    setTouchState(ev, 1);
                    break;
                }
                this.mAllowLongPress = true;
                break;
            case 1:
            case 3:
                setTouchState(ev, 0);
                break;
            case 2:
                onTouchEventUnique(ev);
                if (this.mTouchState == 0 && scrolledFarEnough(ev)) {
                    setTouchState(ev, 1);
                    break;
                }
        }
        if (2 != (ev.getAction() & 255)) {
            onTouchEventUnique(ev);
        }
        if (this.mCurrentGestureFinished) {
            return true;
        }
        if (this.mTouchState == 0 || this.mTouchState == 3) {
            return false;
        }
        return true;
    }

    private boolean scrolledFarEnough(MotionEvent ev) {
        float dx = Math.abs(ev.getX(0) - this.mLastMotionX);
        if (dx <= this.mConfirmHorizontalScrollRatio * Math.abs(ev.getY(0) - this.mLastMotionY) || dx <= ((float) (this.mTouchSlop * ev.getPointerCount()))) {
            return false;
        }
        return true;
    }

    private void onTouchEventUnique(MotionEvent ev) {
        this.mGestureVelocityTracker.addMovement(ev);
        if (this.mTouchState == 0 || 4 == this.mTouchState) {
            this.mScaleDetector.onTouchEvent(ev);
        }
    }

    public boolean onTouchEvent(MotionEvent ev) {
        int newPointerIndex = 0;
        if (this.mCurrentGestureFinished) {
            return true;
        }
        if (this.mTouchIntercepted) {
            onTouchEventUnique(ev);
        }
        int pointerIndex;
        switch (ev.getAction() & 255) {
            case 1:
            case 3:
                if (this.mTouchState == 1) {
                    snapByVelocity(this.mActivePointerId);
                }
                setTouchState(ev, 0);
                break;
            case 2:
                if (this.mTouchState == 0 && scrolledFarEnough(ev)) {
                    setTouchState(ev, 1);
                }
                if (this.mTouchState == 1) {
                    pointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    if (pointerIndex == -1) {
                        setTouchState(ev, 1);
                        pointerIndex = ev.findPointerIndex(this.mActivePointerId);
                    }
                    float x = ev.getX(pointerIndex);
                    float deltaX = this.mLastMotionX - x;
                    this.mLastMotionX = x;
                    if (deltaX == 0.0f) {
                        awakenScrollBars();
                        break;
                    }
                    scrollTo(Math.round(this.mTouchX + deltaX), 0);
                    break;
                }
                break;
            case 6:
                pointerIndex = (ev.getAction() & 65280) >> 8;
                if (ev.getPointerId(pointerIndex) == this.mActivePointerId) {
                    if (pointerIndex == 0) {
                        newPointerIndex = 1;
                    }
                    this.mLastMotionX = ev.getX(newPointerIndex);
                    this.mActivePointerId = ev.getPointerId(newPointerIndex);
                    this.mGestureVelocityTracker.init(this.mActivePointerId);
                    break;
                }
                break;
        }
        this.mTouchIntercepted = true;
        return true;
    }

    private void snapByVelocity(int pointerId) {
        if (this.mChildScreenWidth > 0 && getCurrentScreen() != null) {
            int velocityX = (int) this.mGestureVelocityTracker.getXVelocity(1000, this.mMaximumVelocity, pointerId);
            int flingDirection = this.mGestureVelocityTracker.getFlingDirection((float) Math.abs(velocityX));
            if (flingDirection == 1 && this.mCurrentScreen > 0) {
                snapToScreen(this.mCurrentScreen - this.mVisibleRange, velocityX, true);
            } else if (flingDirection == 2 && this.mCurrentScreen < getScreenCount() - 1) {
                snapToScreen(this.mCurrentScreen + this.mVisibleRange, velocityX, true);
            } else if (flingDirection == 3) {
                snapToScreen(this.mCurrentScreen, velocityX, true);
            } else {
                int i;
                int i2 = this.mChildScreenWidth;
                if (this.mScrollWholeScreen) {
                    i = this.mVisibleRange;
                } else {
                    i = 1;
                }
                snapToScreen((this.mScrollX + ((i2 * i) >> 1)) / this.mChildScreenWidth, 0, true);
            }
        }
    }

    protected void finishCurrentGesture() {
        this.mCurrentGestureFinished = true;
        setTouchState(null, 0);
    }

    public void snapToScreen(int whichScreen) {
        snapToScreen(whichScreen, 0, false);
    }

    protected void snapToScreen(int whichScreen, int velocity, boolean settle) {
        if (this.mScreenWidth > 0) {
            if (this.mScrollWholeScreen) {
                this.mNextScreen = Math.max(0, Math.min(whichScreen, getScreenCount() - 1));
                this.mNextScreen -= this.mNextScreen % this.mVisibleRange;
            } else {
                this.mNextScreen = Math.max(0, Math.min(whichScreen, getScreenCount() - this.mVisibleRange));
            }
            int screenDelta = Math.max(1, Math.abs(this.mNextScreen - this.mCurrentScreen));
            if (!this.mScroller.isFinished()) {
                this.mScroller.abortAnimation();
            }
            velocity = Math.abs(velocity);
            if (settle) {
                this.mScrollInterpolator.setDistance(screenDelta, velocity);
            } else {
                this.mScrollInterpolator.disableSettle();
            }
            int delta = ((this.mNextScreen * this.mChildScreenWidth) - this.mScrollOffset) - this.mScrollX;
            int duration = (Math.abs(delta) * this.mScreenSnapDuration) / this.mScreenWidth;
            if (velocity > 0) {
                duration += (int) ((((float) duration) / (((float) velocity) / 2500.0f)) * 0.4f);
            }
            duration = Math.max(this.mScreenSnapDuration, duration);
            if (screenDelta <= 1) {
                duration = Math.min(duration, this.mScreenSnapDuration * 2);
            }
            this.mScroller.startScroll(this.mScrollX, 0, delta, 0, duration);
            invalidate();
        }
    }

    public final int getScreenCount() {
        return this.mScreenCounter;
    }

    public View getCurrentScreen() {
        return getScreen(this.mCurrentScreen);
    }

    public void setCurrentScreen(int screenIndex) {
        if (this.mScrollWholeScreen) {
            screenIndex = Math.max(0, Math.min(screenIndex, getScreenCount() - 1));
            screenIndex -= screenIndex % this.mVisibleRange;
        } else {
            screenIndex = Math.max(0, Math.min(screenIndex, getScreenCount() - this.mVisibleRange));
        }
        setCurrentScreenInner(screenIndex);
        if (!this.mFirstLayout) {
            if (!this.mScroller.isFinished()) {
                this.mScroller.abortAnimation();
            }
            scrollToScreen(this.mCurrentScreen);
            invalidate();
        }
    }

    protected void setCurrentScreenInner(int screenIndex) {
        updateSeekPoints(this.mCurrentScreen, screenIndex);
        this.mCurrentScreen = screenIndex;
        this.mNextScreen = -1;
    }

    public View getScreen(int screenIndex) {
        if (screenIndex < 0 || screenIndex >= getScreenCount()) {
            return null;
        }
        return getChildAt(screenIndex);
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        int currentCount = getScreenCount();
        if (index < 0) {
            index = currentCount;
        } else {
            index = Math.min(index, currentCount);
        }
        this.mScreenCounter++;
        if (this.mScreenSeekBar != null) {
            this.mScreenSeekBar.addView(createSeekPoint(), index, SEEK_POINT_LAYOUT_PARAMS);
            if (getScreenCount() > 1) {
                this.mScreenSeekBar.setVisibility(0);
            }
        }
        refreshScrollBound();
        super.addView(child, index, params);
    }

    public void removeView(View view) {
        throw new UnsupportedOperationException("ScreenView doesn't support remove view directly.");
    }

    public void removeViewInLayout(View view) {
        throw new UnsupportedOperationException("ScreenView doesn't support remove view directly.");
    }

    public void removeViewsInLayout(int start, int count) {
        throw new UnsupportedOperationException("ScreenView doesn't support remove view directly.");
    }

    public void removeViewAt(int index) {
        throw new UnsupportedOperationException("ScreenView doesn't support remove view directly.");
    }

    public void removeViews(int start, int count) {
        throw new UnsupportedOperationException("ScreenView doesn't support remove view directly.");
    }

    public void removeAllViewsInLayout() {
        this.mIndicatorCount = 0;
        this.mScreenCounter = 0;
        super.removeAllViewsInLayout();
    }

    public void addIndicator(View indicator, FrameLayout.LayoutParams params) {
        this.mIndicatorCount++;
        super.addView(indicator, -1, params);
    }

    public void removeIndicator(View indicator) {
        int index = indexOfChild(indicator);
        if (index < getScreenCount()) {
            throw new InvalidParameterException("The view passed through the parameter must be indicator.");
        }
        this.mIndicatorCount--;
        super.removeViewAt(index);
    }

    public void removeAllScreens() {
        for (int i = 0; i < getScreenCount(); i++) {
            ((ViewGroup) getScreen(i)).removeAllViews();
        }
        removeScreensInLayout(0, getScreenCount());
        requestLayout();
        invalidate();
    }

    public void removeScreensInLayout(int start, int count) {
        if (start >= 0 && start < getScreenCount()) {
            count = Math.min(count, getScreenCount() - start);
            if (this.mScreenSeekBar != null) {
                this.mScreenSeekBar.removeViewsInLayout(start, count);
            }
            this.mScreenCounter = 0;
            super.removeViewsInLayout(start, count);
        }
    }

    public void setOnLongClickListener(OnLongClickListener l) {
        this.mLongClickListener = l;
        int count = getScreenCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).setOnLongClickListener(l);
        }
    }

    private ImageView createSeekPoint() {
        ImageView seekPoint = new ImageView(this.mContext);
        seekPoint.setScaleType(ScaleType.CENTER);
        seekPoint.setImageResource(this.mSeekPointResId);
        seekPoint.setPadding(4, 0, 4, 0);
        return seekPoint;
    }

    private void updateSeekPoints(int fromIndex, int toIndex) {
        if (this.mScreenSeekBar != null) {
            int count = getScreenCount();
            if (count <= 1) {
                this.mScreenSeekBar.setVisibility(8);
                return;
            }
            int i = 0;
            while (i < this.mVisibleRange && fromIndex + i < count) {
                this.mScreenSeekBar.getChildAt(fromIndex + i).setSelected(false);
                i++;
            }
            i = 0;
            while (i < this.mVisibleRange && toIndex + i < count) {
                this.mScreenSeekBar.getChildAt(toIndex + i).setSelected(true);
                i++;
            }
        }
    }

    protected void resetTransformation(View child) {
        child.setAlpha(1.0f);
        child.setTranslationX(0.0f);
        child.setTranslationY(0.0f);
        child.setPivotX(0.0f);
        child.setPivotY(0.0f);
        child.setRotation(0.0f);
        child.setRotationX(0.0f);
        child.setRotationY(0.0f);
        child.setCameraDistance(this.DEFAULT_CAMERA_DISTANCE);
        child.setScaleX(1.0f);
        child.setScaleY(1.0f);
    }

    protected void updateChildStaticTransformation(View child) {
        if (!(child instanceof Indicator)) {
            float childW = (float) child.getMeasuredWidth();
            float childH = (float) child.getMeasuredHeight();
            float halfChildW = childW / 2.0f;
            float halfChildH = childH / 2.0f;
            float interpolation = (((((float) this.mScrollX) + (((float) getMeasuredWidth()) / 2.0f)) - ((float) child.getLeft())) - halfChildW) / childW;
            switch (this.mScreenTransitionType) {
                case 0:
                    resetTransformation(child);
                    break;
                case 1:
                    resetTransformation(child);
                    break;
                case 2:
                    if (interpolation != 0.0f && Math.abs(interpolation) <= 1.0f) {
                        child.setAlpha(((1.0f - Math.abs(interpolation)) * 0.7f) + 0.3f);
                        child.setTranslationX(0.0f);
                        child.setTranslationY(0.0f);
                        child.setScaleX(1.0f);
                        child.setScaleY(1.0f);
                        child.setPivotX(0.0f);
                        child.setPivotY(0.0f);
                        child.setRotation(0.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY(0.0f);
                        child.setCameraDistance(this.DEFAULT_CAMERA_DISTANCE);
                        break;
                    }
                    resetTransformation(child);
                    break;
                    break;
                case 3:
                    if (interpolation != 0.0f && Math.abs(interpolation) <= 1.0f) {
                        child.setAlpha(1.0f);
                        child.setTranslationX(0.0f);
                        child.setTranslationY(0.0f);
                        child.setScaleX(1.0f);
                        child.setScaleY(1.0f);
                        child.setPivotX(halfChildW);
                        child.setPivotY(childH);
                        child.setRotation((-interpolation) * 30.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY(0.0f);
                        child.setCameraDistance(this.DEFAULT_CAMERA_DISTANCE);
                        break;
                    }
                    resetTransformation(child);
                    break;
                    break;
                case 4:
                    if (interpolation != 0.0f && Math.abs(interpolation) <= 1.0f) {
                        child.setAlpha(1.0f);
                        child.setTranslationX(0.0f);
                        child.setTranslationY(0.0f);
                        child.setScaleX(1.0f);
                        child.setScaleY(1.0f);
                        if (interpolation < 0.0f) {
                            childW = 0.0f;
                        }
                        child.setPivotX(childW);
                        child.setPivotY(halfChildH);
                        child.setRotation(0.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY(-90.0f * interpolation);
                        child.setCameraDistance(5000.0f);
                        break;
                    }
                    resetTransformation(child);
                    break;
                    break;
                case 5:
                    if (interpolation != 0.0f && Math.abs(interpolation) <= 1.0f) {
                        child.setAlpha(1.0f - Math.abs(interpolation));
                        child.setTranslationY(0.0f);
                        child.setTranslationX((childW * interpolation) - ((Math.abs(interpolation) * childW) * 0.3f));
                        float scale1 = 1.0f + (0.3f * interpolation);
                        child.setScaleX(scale1);
                        child.setScaleY(scale1);
                        child.setPivotX(0.0f);
                        child.setPivotY(halfChildH);
                        child.setRotation(0.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY((-interpolation) * 45.0f);
                        child.setCameraDistance(5000.0f);
                        break;
                    }
                    resetTransformation(child);
                    break;
                    break;
                case 7:
                    if (interpolation > 0.0f) {
                        child.setAlpha(1.0f - interpolation);
                        float scale2 = 0.6f + ((1.0f - interpolation) * 0.4f);
                        child.setTranslationX(((1.0f - scale2) * childW) * 3.0f);
                        child.setTranslationY(((1.0f - scale2) * childH) * 0.5f);
                        child.setScaleX(scale2);
                        child.setScaleY(scale2);
                        child.setPivotX(0.0f);
                        child.setPivotY(0.0f);
                        child.setRotation(0.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY(0.0f);
                        child.setCameraDistance(this.DEFAULT_CAMERA_DISTANCE);
                        break;
                    }
                    resetTransformation(child);
                    break;
                case 8:
                    if (interpolation != 0.0f && Math.abs(interpolation) <= 1.0f) {
                        child.setAlpha(1.0f - Math.abs(interpolation));
                        child.setTranslationX(childW * interpolation);
                        child.setTranslationY(0.0f);
                        child.setScaleX(1.0f);
                        child.setScaleY(1.0f);
                        child.setPivotX(halfChildW);
                        child.setPivotY(halfChildH);
                        child.setRotation(0.0f);
                        child.setRotationX(0.0f);
                        child.setRotationY((-interpolation) * 90.0f);
                        child.setCameraDistance(5000.0f);
                        break;
                    }
                    resetTransformation(child);
                    break;
                case 9:
                    updateChildStaticTransformationByScreen(child, interpolation);
                    break;
            }
        }
    }

    protected void updateChildStaticTransformationByScreen(View child, float interpolation) {
    }

    protected void onPinchIn(ScaleGestureDetector detector) {
    }

    protected void onPinchOut(ScaleGestureDetector detector) {
    }

    protected Parcelable onSaveInstanceState() {
        SavedState state = new SavedState(super.onSaveInstanceState());
        state.currentScreen = this.mCurrentScreen;
        return state;
    }

    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (savedState.currentScreen != -1) {
            setCurrentScreen(savedState.currentScreen);
        }
    }
}
