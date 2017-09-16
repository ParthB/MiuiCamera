package com.android.camera.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Vibrator;
import android.support.v7.recyclerview.R;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;
import com.android.camera.Util;

/* compiled from: ModeView */
class ModeGridView extends ViewGroup {
    private static int INVALID_POSITION = -1;
    private long dragResponseMS = 1000;
    private boolean isDrag = false;
    private int mChildHeight;
    private int mChildWidth;
    private int mColumnCount;
    private int mDownScrollBorder;
    private int mDownX;
    private int mDownY;
    private Bitmap mDragBitmap;
    private ImageView mDragImageView;
    private int mDragPosition;
    private boolean mEnableDrag = false;
    private int mGridViewHonSpacing;
    private int mGridViewMarginTop;
    private int mGridViewMarginWidth;
    private int mGridViewVerSpacing;
    private Handler mHandler = new Handler();
    private Runnable mLongClickRunnable = new Runnable() {
        public void run() {
            ModeGridView.this.isDrag = true;
            ModeGridView.this.mVibrator.vibrate(50);
            ModeGridView.this.mStartDragItemView.setVisibility(4);
            ModeGridView.this.createDragImage(ModeGridView.this.mDragBitmap, ModeGridView.this.mDownX, ModeGridView.this.mDownY);
        }
    };
    private int mOffset2Left;
    private int mOffset2Top;
    ScreenView mParent;
    private int mPoint2ItemLeft;
    private int mPoint2ItemTop;
    private int mRowCount;
    private int mScreenIndex;
    private Runnable mScrollRunnable = new Runnable() {
        public void run() {
            if (ModeGridView.this.moveY > ModeGridView.this.mUpScrollBorder) {
                ModeGridView.this.mHandler.postDelayed(ModeGridView.this.mScrollRunnable, 25);
            } else if (ModeGridView.this.moveY < ModeGridView.this.mDownScrollBorder) {
                ModeGridView.this.mHandler.postDelayed(ModeGridView.this.mScrollRunnable, 25);
            } else {
                ModeGridView.this.mHandler.removeCallbacks(ModeGridView.this.mScrollRunnable);
            }
            ModeGridView.this.onSwapItem(ModeGridView.this.moveX, ModeGridView.this.moveY);
            View view = ModeGridView.this.getChildAt(ModeGridView.this.mDragPosition - ModeGridView.this.getFirstVisiblePosition());
            ModeGridView.this.mParent.snapToScreen(ModeGridView.this.mScreenIndex + 1);
        }
    };
    private View mStartDragItemView = null;
    private int mStatusHeight;
    private Rect mTouchFrame;
    private int mUpScrollBorder;
    private Vibrator mVibrator;
    private LayoutParams mWindowLayoutParams;
    private WindowManager mWindowManager;
    private int moveX;
    private int moveY;
    private OnChanageListener onChanageListener;

    /* compiled from: ModeView */
    public interface OnChanageListener {
        void onChange(int i, int i2);
    }

    public ModeGridView(Context context, ScreenView parent, int rowCount, int columnCount, int childWidth, int childHeight, int screenIndex) {
        super(context);
        set(rowCount, columnCount, childWidth, childHeight);
        setDrawingCacheEnabled(true);
        setWillNotDraw(false);
        initGridViewLayout();
        initDrag(parent, screenIndex);
    }

    private void initDrag(ScreenView parent, int screenIndex) {
        this.mParent = parent;
        this.mVibrator = (Vibrator) this.mContext.getSystemService("vibrator");
        this.mWindowManager = (WindowManager) this.mContext.getSystemService("window");
        this.mScreenIndex = screenIndex;
    }

    public int pointToPosition(int x, int y) {
        Rect frame = this.mTouchFrame;
        if (frame == null) {
            this.mTouchFrame = new Rect();
            frame = this.mTouchFrame;
        }
        for (int i = getChildCount() - 1; i >= 0; i--) {
            View child = getChildAt(i);
            if (child.getVisibility() == 0) {
                child.getHitRect(frame);
                if (frame.contains(x, y)) {
                    return i;
                }
            }
        }
        return -1;
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!this.mEnableDrag) {
            return super.dispatchTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case 0:
                this.mHandler.postDelayed(this.mLongClickRunnable, this.dragResponseMS);
                this.mDownX = (int) ev.getX();
                this.mDownY = (int) ev.getY();
                this.mDragPosition = pointToPosition(this.mDownX, this.mDownY);
                if (this.mDragPosition != INVALID_POSITION) {
                    this.mStartDragItemView = getChildAt(this.mDragPosition - getFirstVisiblePosition());
                    this.mPoint2ItemTop = this.mDownY - this.mStartDragItemView.getTop();
                    this.mPoint2ItemLeft = this.mDownX - this.mStartDragItemView.getLeft();
                    this.mOffset2Top = (int) (ev.getRawY() - ((float) this.mDownY));
                    this.mOffset2Left = (int) (ev.getRawX() - ((float) this.mDownX));
                    this.mDownScrollBorder = getHeight() / 4;
                    this.mUpScrollBorder = (getHeight() * 3) / 4;
                    this.mStartDragItemView.setDrawingCacheEnabled(true);
                    this.mDragBitmap = Bitmap.createBitmap(this.mStartDragItemView.getDrawingCache());
                    this.mStartDragItemView.destroyDrawingCache();
                    break;
                }
                return super.dispatchTouchEvent(ev);
            case 1:
                this.mHandler.removeCallbacks(this.mLongClickRunnable);
                this.mHandler.removeCallbacks(this.mScrollRunnable);
                break;
            case 2:
                if (!isTouchInItem(this.mStartDragItemView, (int) ev.getX(), (int) ev.getY())) {
                    this.mHandler.removeCallbacks(this.mLongClickRunnable);
                    break;
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    private int getFirstVisiblePosition() {
        return 0;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean isTouchInItem(android.view.View r5, int r6, int r7) {
        /*
        r4 = this;
        r3 = 0;
        r0 = r5.getLeft();
        r1 = r5.getTop();
        if (r6 < r0) goto L_0x0012;
    L_0x000b:
        r2 = r5.getWidth();
        r2 = r2 + r0;
        if (r6 <= r2) goto L_0x0013;
    L_0x0012:
        return r3;
    L_0x0013:
        if (r7 < r1) goto L_0x001c;
    L_0x0015:
        r2 = r5.getHeight();
        r2 = r2 + r1;
        if (r7 <= r2) goto L_0x001d;
    L_0x001c:
        return r3;
    L_0x001d:
        r2 = 1;
        return r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ui.ModeGridView.isTouchInItem(android.view.View, int, int):boolean");
    }

    public boolean onTouchEvent(MotionEvent ev) {
        if (!this.isDrag || this.mDragImageView == null) {
            return super.onTouchEvent(ev);
        }
        switch (ev.getAction()) {
            case 1:
                onStopDrag();
                this.isDrag = false;
                break;
            case 2:
                this.moveX = (int) ev.getX();
                this.moveY = (int) ev.getY();
                onDragItem(this.moveX, this.moveY);
                break;
        }
        return true;
    }

    private void createDragImage(Bitmap bitmap, int downX, int downY) {
        this.mWindowLayoutParams = new LayoutParams();
        this.mWindowLayoutParams.format = -3;
        this.mWindowLayoutParams.gravity = 51;
        this.mWindowLayoutParams.x = (downX - this.mPoint2ItemLeft) + this.mOffset2Left;
        this.mWindowLayoutParams.y = ((downY - this.mPoint2ItemTop) + this.mOffset2Top) - this.mStatusHeight;
        this.mWindowLayoutParams.alpha = 0.55f;
        this.mWindowLayoutParams.width = -2;
        this.mWindowLayoutParams.height = -2;
        this.mWindowLayoutParams.flags = 24;
        this.mDragImageView = new ImageView(getContext());
        this.mDragImageView.setImageBitmap(bitmap);
        this.mWindowManager.addView(this.mDragImageView, this.mWindowLayoutParams);
    }

    private void removeDragImage() {
        if (this.mDragImageView != null) {
            this.mWindowManager.removeView(this.mDragImageView);
            this.mDragImageView = null;
        }
    }

    private void onDragItem(int moveX, int moveY) {
        this.mWindowLayoutParams.x = (moveX - this.mPoint2ItemLeft) + this.mOffset2Left;
        this.mWindowLayoutParams.y = ((moveY - this.mPoint2ItemTop) + this.mOffset2Top) - this.mStatusHeight;
        this.mWindowManager.updateViewLayout(this.mDragImageView, this.mWindowLayoutParams);
        onSwapItem(moveX, moveY);
        this.mHandler.post(this.mScrollRunnable);
    }

    private void onSwapItem(int moveX, int moveY) {
        int tempPosition = pointToPosition(moveX, moveY);
        if (tempPosition != this.mDragPosition && tempPosition != INVALID_POSITION) {
            getChildAt(tempPosition - getFirstVisiblePosition()).setVisibility(4);
            getChildAt(this.mDragPosition - getFirstVisiblePosition()).setVisibility(0);
            if (this.onChanageListener != null) {
                this.onChanageListener.onChange(this.mDragPosition, tempPosition);
            }
            this.mDragPosition = tempPosition;
        }
    }

    private void onStopDrag() {
        getChildAt(this.mDragPosition - getFirstVisiblePosition()).setVisibility(0);
        removeDragImage();
    }

    public void set(int rowCount, int columnCount, int childWidth, int childHeight) {
        this.mRowCount = Math.max(1, rowCount);
        this.mColumnCount = Math.max(1, columnCount);
        this.mChildHeight = Math.max(1, childHeight);
        this.mChildWidth = Math.max(1, childWidth);
    }

    private void initGridViewLayout() {
        this.mGridViewHonSpacing = getResources().getDimensionPixelSize(R.dimen.setting_grid_horizontal_space);
        this.mGridViewVerSpacing = getResources().getDimensionPixelSize(R.dimen.setting_grid_vertical_space);
        this.mGridViewMarginWidth = ((Util.sWindowWidth - (this.mChildWidth * this.mColumnCount)) - (this.mGridViewHonSpacing * (this.mColumnCount - 1))) / 2;
        this.mGridViewMarginTop = getResources().getDimensionPixelSize(R.dimen.mode_settings_margin_top);
        if (this.mRowCount < 3) {
            this.mGridViewMarginTop += ((this.mChildHeight + this.mGridViewVerSpacing) * (3 - this.mRowCount)) / 2;
        }
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureDimension(widthMeasureSpec, ((this.mGridViewMarginWidth + (this.mChildWidth * this.mColumnCount)) + (this.mGridViewHonSpacing * (this.mColumnCount - 1))) + this.mGridViewMarginWidth), measureDimension(heightMeasureSpec, (this.mGridViewMarginTop + (this.mChildHeight * this.mRowCount)) + (this.mGridViewVerSpacing * (this.mRowCount - 1))));
        measureChildren(MeasureSpec.makeMeasureSpec(this.mChildWidth, 1073741824), MeasureSpec.makeMeasureSpec(this.mChildHeight, 1073741824));
    }

    int measureDimension(int measureSpec, int contentDimension) {
        switch (MeasureSpec.getMode(measureSpec)) {
            case Integer.MIN_VALUE:
                return Math.min(contentDimension, MeasureSpec.getSize(measureSpec));
            case 0:
                return contentDimension;
            case 1073741824:
                return MeasureSpec.getSize(measureSpec);
            default:
                return 0;
        }
    }

    protected void layoutChildByIndex(int index) {
        int i;
        int i2 = 0;
        int rowIndex = index / this.mColumnCount;
        int columnIndex = index % this.mColumnCount;
        int i3 = (this.mChildWidth * columnIndex) + this.mGridViewMarginWidth;
        if (columnIndex > 0) {
            i = this.mGridViewHonSpacing * columnIndex;
        } else {
            i = 0;
        }
        int left = i3 + i;
        i = this.mGridViewMarginTop + (this.mChildHeight * rowIndex);
        if (rowIndex > 0) {
            i2 = this.mGridViewVerSpacing * rowIndex;
        }
        int top = i + i2;
        getChildAt(index).layout(left, top, this.mChildWidth + left, this.mChildHeight + top);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        doLayout(left, top, right, bottom);
    }

    protected void doLayout(int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            layoutChildByIndex(i);
        }
    }

    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
    }
}
