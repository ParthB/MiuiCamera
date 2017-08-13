package com.android.camera.ui;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ListAdapter;
import android.widget.Scroller;
import com.android.camera.ActivityBase;
import com.android.camera.Util;
import java.util.LinkedList;
import java.util.Queue;

public class HorizontalListView extends AdapterView<ListAdapter> {
    private static final String TAG = HorizontalListView.class.getSimpleName();
    protected ListAdapter mAdapter;
    private boolean mBlockNotification;
    protected int mCurrentX;
    private boolean mDataChanged = false;
    private DataSetObserver mDataObserver = new DataSetObserver() {
        public void onChanged() {
            synchronized (HorizontalListView.this) {
                HorizontalListView.this.mDataChanged = true;
            }
            HorizontalListView.this.invalidate();
            HorizontalListView.this.requestLayout();
        }

        public void onInvalidated() {
            HorizontalListView.this.reset();
            HorizontalListView.this.invalidate();
            HorizontalListView.this.requestLayout();
        }
    };
    private int mDisplayOffset = 0;
    private GestureDetector mGesture;
    private boolean mIsScrollingPerformed;
    private int mItemWidth = 160;
    private View mLastSelectImageListItem;
    private int mLeftViewIndex = -1;
    private int mMaxX = Integer.MAX_VALUE;
    protected int mNextX;
    private OnGestureListener mOnGesture = new SimpleOnGestureListener() {
        public boolean onDown(MotionEvent e) {
            return HorizontalListView.this.onDown(e);
        }

        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return HorizontalListView.this.onFling(e1, e2, velocityX, velocityY);
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            synchronized (HorizontalListView.this) {
                HorizontalListView horizontalListView = HorizontalListView.this;
                horizontalListView.mNextX += (int) distanceX;
            }
            HorizontalListView.this.mIsScrollingPerformed = true;
            HorizontalListView.this.requestLayout();
            return true;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            HorizontalListView.this.mBlockNotification = true;
            for (int i = 0; i < HorizontalListView.this.getChildCount(); i++) {
                View child = HorizontalListView.this.getChildAt(i);
                if (isEventWithinView(e, child)) {
                    int dataIndex = HorizontalListView.this.toDataIndex((HorizontalListView.this.mLeftViewIndex + 1) + i);
                    if (HorizontalListView.this.mOnItemClicked != null) {
                        HorizontalListView.this.mOnItemClicked.onItemClick(HorizontalListView.this, child, dataIndex, HorizontalListView.this.mAdapter.getItemId(dataIndex));
                    }
                    if (HorizontalListView.this.mOnItemSelected != null) {
                        HorizontalListView.this.mOnItemSelected.onItemSelected(HorizontalListView.this, child, dataIndex, HorizontalListView.this.mAdapter.getItemId(dataIndex));
                    }
                    return true;
                }
            }
            return true;
        }

        public void onLongPress(MotionEvent e) {
            int childCount = HorizontalListView.this.getChildCount();
            int i = 0;
            while (i < childCount) {
                View child = HorizontalListView.this.getChildAt(i);
                if (!isEventWithinView(e, child)) {
                    i++;
                } else if (HorizontalListView.this.mOnItemLongClicked != null) {
                    int dataIndex = HorizontalListView.this.toDataIndex((HorizontalListView.this.mLeftViewIndex + 1) + i);
                    HorizontalListView.this.mOnItemLongClicked.onItemLongClick(HorizontalListView.this, child, dataIndex, HorizontalListView.this.mAdapter.getItemId(dataIndex));
                    return;
                } else {
                    return;
                }
            }
        }

        private boolean isEventWithinView(MotionEvent e, View child) {
            Rect viewRect = new Rect();
            int[] childPosition = new int[2];
            child.getLocationOnScreen(childPosition);
            int left = childPosition[0];
            int right = left + child.getWidth();
            int top = childPosition[1];
            viewRect.set(left, top, right, top + child.getHeight());
            return viewRect.contains((int) e.getRawX(), (int) e.getRawY());
        }
    };
    private OnItemClickListener mOnItemClicked;
    private OnItemLongClickListener mOnItemLongClicked;
    private OnItemSelectedListener mOnItemSelected;
    private int mPaddingWidth;
    private int mPresetWidth = 0;
    private int mPreviousSelectViewIndex = 0;
    private Queue<View> mRemovedViewQueue = new LinkedList();
    private int mRightViewIndex = 0;
    protected Scroller mScroller;
    private boolean mSelectCenter = true;
    private int mSelectViewIndex = 0;
    private boolean mTouchDown;

    public HorizontalListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView();
    }

    public void setItemWidth(int width) {
        this.mItemWidth = width;
        if (this.mSelectCenter) {
            this.mPaddingWidth = (this.mPresetWidth - this.mItemWidth) / 2;
            this.mDisplayOffset = this.mPaddingWidth;
        }
    }

    private synchronized void initView() {
        WindowManager wm = (WindowManager) this.mContext.getSystemService("window");
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        this.mPresetWidth = dm.widthPixels;
        this.mLeftViewIndex = -1;
        this.mRightViewIndex = 0;
        this.mCurrentX = 0;
        this.mNextX = 0;
        this.mMaxX = Integer.MAX_VALUE;
        if (this.mSelectCenter) {
            this.mPaddingWidth = (this.mPresetWidth - this.mItemWidth) / 2;
            this.mDisplayOffset = this.mPaddingWidth;
        } else {
            this.mDisplayOffset = 0;
        }
        this.mScroller = new Scroller(getContext());
        this.mGesture = new GestureDetector(getContext(), this.mOnGesture);
        if (this.mLastSelectImageListItem != null) {
            this.mLastSelectImageListItem.setActivated(false);
            this.mLastSelectImageListItem = null;
        }
        ((ActivityBase) this.mContext).loadCameraSound(6);
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.mOnItemSelected = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mOnItemClicked = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.mOnItemLongClicked = listener;
    }

    public ListAdapter getAdapter() {
        return this.mAdapter;
    }

    public View getSelectedView() {
        return null;
    }

    public void setAdapter(ListAdapter adapter) {
        if (this.mAdapter != null) {
            this.mAdapter.unregisterDataSetObserver(this.mDataObserver);
        }
        this.mAdapter = adapter;
        this.mAdapter.registerDataSetObserver(this.mDataObserver);
        reset();
    }

    private synchronized void reset() {
        initView();
        removeAllViewsInLayout();
        requestLayout();
    }

    public void setSelection(int position) {
        position = toViewIndex(position);
        if (this.mSelectViewIndex != position) {
            this.mPreviousSelectViewIndex = this.mSelectViewIndex;
            this.mSelectViewIndex = position;
            if (isShown()) {
                ((ActivityBase) this.mContext).playCameraSound(6);
            }
            if (position > this.mLeftViewIndex && position < this.mRightViewIndex) {
                View child = getChildAt((position - this.mLeftViewIndex) - 1);
                int dataIndex = toDataIndex(position);
                notifyItemSelect(child, dataIndex, this.mAdapter.getItemId(dataIndex));
            }
            if (!this.mIsScrollingPerformed) {
                justify();
            }
        }
    }

    private void justify() {
        boolean scroll = true;
        if (this.mSelectViewIndex > this.mLeftViewIndex && this.mSelectViewIndex < this.mRightViewIndex) {
            scroll = Math.abs((getChildAt((this.mSelectViewIndex - this.mLeftViewIndex) + -1).getLeft() + (this.mItemWidth / 2)) - (this.mPresetWidth / 2)) > 10;
        }
        if (scroll) {
            int distance = ((this.mPaddingWidth + (this.mItemWidth * this.mSelectViewIndex)) + (this.mItemWidth / 2)) - (this.mPresetWidth / 2);
            this.mMaxX = ((this.mPaddingWidth * 2) + (this.mItemWidth * this.mAdapter.getCount())) - this.mPresetWidth;
            if (distance > this.mMaxX) {
                distance = this.mMaxX;
            }
            if (distance != this.mCurrentX) {
                if (isShown()) {
                    scrollTo(distance);
                } else {
                    this.mNextX = distance;
                    requestLayout();
                }
            }
        }
    }

    private void addAndMeasureChild(View child, int viewPos) {
        LayoutParams params = child.getLayoutParams();
        if (params == null) {
            params = new LayoutParams(-1, -1);
        }
        addViewInLayout(child, viewPos, params, true);
        child.measure(MeasureSpec.makeMeasureSpec(getWidth(), Integer.MIN_VALUE), MeasureSpec.makeMeasureSpec(getHeight(), Integer.MIN_VALUE));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected synchronized void onLayout(boolean r9, int r10, int r11, int r12, int r13) {
        /*
        r8 = this;
        monitor-enter(r8);
        super.onLayout(r9, r10, r11, r12, r13);	 Catch:{ all -> 0x00c0 }
        r6 = r8.mAdapter;	 Catch:{ all -> 0x00c0 }
        if (r6 != 0) goto L_0x000a;
    L_0x0008:
        monitor-exit(r8);
        return;
    L_0x000a:
        r1 = 0;
        r6 = r8.mDataChanged;	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x001d;
    L_0x000f:
        r4 = r8.mCurrentX;	 Catch:{ all -> 0x00c0 }
        r8.initView();	 Catch:{ all -> 0x00c0 }
        r8.removeAllViewsInLayout();	 Catch:{ all -> 0x00c0 }
        r8.mNextX = r4;	 Catch:{ all -> 0x00c0 }
        r6 = 0;
        r8.mDataChanged = r6;	 Catch:{ all -> 0x00c0 }
        r1 = 1;
    L_0x001d:
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r6 = r6.computeScrollOffset();	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x002d;
    L_0x0025:
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r5 = r6.getCurrX();	 Catch:{ all -> 0x00c0 }
        r8.mNextX = r5;	 Catch:{ all -> 0x00c0 }
    L_0x002d:
        r6 = r8.mNextX;	 Catch:{ all -> 0x00c0 }
        if (r6 > 0) goto L_0x003a;
    L_0x0031:
        r6 = 0;
        r8.mNextX = r6;	 Catch:{ all -> 0x00c0 }
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r7 = 1;
        r6.forceFinished(r7);	 Catch:{ all -> 0x00c0 }
    L_0x003a:
        r6 = r8.mNextX;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mMaxX;	 Catch:{ all -> 0x00c0 }
        if (r6 < r7) goto L_0x004a;
    L_0x0040:
        r6 = r8.mMaxX;	 Catch:{ all -> 0x00c0 }
        r8.mNextX = r6;	 Catch:{ all -> 0x00c0 }
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r7 = 1;
        r6.forceFinished(r7);	 Catch:{ all -> 0x00c0 }
    L_0x004a:
        r6 = r8.mCurrentX;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mNextX;	 Catch:{ all -> 0x00c0 }
        r3 = r6 - r7;
        r6 = r8.mNextX;	 Catch:{ all -> 0x00c0 }
        r8.mCurrentX = r6;	 Catch:{ all -> 0x00c0 }
        r8.removeNonVisibleItems(r3);	 Catch:{ all -> 0x00c0 }
        r8.fillList(r3);	 Catch:{ all -> 0x00c0 }
        r8.positionItems(r3);	 Catch:{ all -> 0x00c0 }
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r6 = r6.isFinished();	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x0067;
    L_0x0065:
        if (r1 == 0) goto L_0x0071;
    L_0x0067:
        r6 = new com.android.camera.ui.HorizontalListView$3;	 Catch:{ all -> 0x00c0 }
        r6.<init>();	 Catch:{ all -> 0x00c0 }
        r8.post(r6);	 Catch:{ all -> 0x00c0 }
    L_0x006f:
        monitor-exit(r8);
        return;
    L_0x0071:
        r8.loadItems();	 Catch:{ all -> 0x00c0 }
        r6 = r8.mScroller;	 Catch:{ all -> 0x00c0 }
        r6 = r6.isFinished();	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x006f;
    L_0x007c:
        r6 = r8.mTouchDown;	 Catch:{ all -> 0x00c0 }
        if (r6 != 0) goto L_0x006f;
    L_0x0080:
        r6 = 0;
        r8.mIsScrollingPerformed = r6;	 Catch:{ all -> 0x00c0 }
        r6 = r8.mSelectCenter;	 Catch:{ all -> 0x00c0 }
        if (r6 == 0) goto L_0x008f;
    L_0x0087:
        r6 = new com.android.camera.ui.HorizontalListView$4;	 Catch:{ all -> 0x00c0 }
        r6.<init>();	 Catch:{ all -> 0x00c0 }
        r8.post(r6);	 Catch:{ all -> 0x00c0 }
    L_0x008f:
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mPreviousSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        if (r6 == r7) goto L_0x006f;
    L_0x0095:
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mLeftViewIndex;	 Catch:{ all -> 0x00c0 }
        if (r6 <= r7) goto L_0x00bb;
    L_0x009b:
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mRightViewIndex;	 Catch:{ all -> 0x00c0 }
        if (r6 > r7) goto L_0x00bb;
    L_0x00a1:
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r2 = r8.toDataIndex(r6);	 Catch:{ all -> 0x00c0 }
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r7 = r8.mLeftViewIndex;	 Catch:{ all -> 0x00c0 }
        r6 = r6 - r7;
        r6 = r6 + -1;
        r0 = r8.getChildAt(r6);	 Catch:{ all -> 0x00c0 }
        r6 = r8.mAdapter;	 Catch:{ all -> 0x00c0 }
        r6 = r6.getItemId(r2);	 Catch:{ all -> 0x00c0 }
        r8.notifyItemSelect(r0, r2, r6);	 Catch:{ all -> 0x00c0 }
    L_0x00bb:
        r6 = r8.mSelectViewIndex;	 Catch:{ all -> 0x00c0 }
        r8.mPreviousSelectViewIndex = r6;	 Catch:{ all -> 0x00c0 }
        goto L_0x006f;
    L_0x00c0:
        r6 = move-exception;
        monitor-exit(r8);
        throw r6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ui.HorizontalListView.onLayout(boolean, int, int, int, int):void");
    }

    private void fillList(int dx) {
        int edge = 0;
        View child = getChildAt(getChildCount() - 1);
        if (child != null) {
            edge = child.getRight();
        }
        fillListRight(edge, dx);
        edge = 0;
        child = getChildAt(0);
        if (child != null) {
            edge = child.getLeft();
        }
        fillListLeft(edge, dx);
    }

    private void fillListRight(int rightEdge, int dx) {
        while (rightEdge + dx < getWidth() && this.mRightViewIndex < this.mAdapter.getCount()) {
            View child = this.mAdapter.getView(toDataIndex(this.mRightViewIndex), (View) this.mRemovedViewQueue.poll(), this);
            if (this.mSelectCenter || this.mRightViewIndex != this.mSelectViewIndex) {
                child.setActivated(false);
            } else {
                this.mLastSelectImageListItem = child;
                child.setActivated(true);
            }
            addAndMeasureChild(child, -1);
            rightEdge += getChildWidth();
            if (this.mRightViewIndex == this.mAdapter.getCount() - 1) {
                this.mMaxX = ((this.mPaddingWidth * 2) + (getChildWidth() * this.mAdapter.getCount())) - getWidth();
            }
            if (this.mMaxX < 0) {
                this.mMaxX = 0;
            }
            this.mRightViewIndex++;
        }
    }

    private int toDataIndex(int viewIndex) {
        if (Util.isLayoutRTL(getContext())) {
            return (this.mAdapter.getCount() - 1) - viewIndex;
        }
        return viewIndex;
    }

    private int toViewIndex(int dataIndex) {
        if (Util.isLayoutRTL(getContext())) {
            return (this.mAdapter.getCount() - 1) - dataIndex;
        }
        return dataIndex;
    }

    private void fillListLeft(int leftEdge, int dx) {
        while (leftEdge + dx > 0 && this.mLeftViewIndex >= 0) {
            View child = this.mAdapter.getView(toDataIndex(this.mLeftViewIndex), (View) this.mRemovedViewQueue.poll(), this);
            if (this.mSelectCenter || this.mLeftViewIndex != this.mSelectViewIndex) {
                child.setActivated(false);
            } else {
                this.mLastSelectImageListItem = child;
                child.setActivated(true);
            }
            addAndMeasureChild(child, 0);
            leftEdge -= getChildWidth();
            this.mLeftViewIndex--;
            this.mDisplayOffset -= getChildWidth();
        }
    }

    private void removeNonVisibleItems(int dx) {
        View child = getChildAt(0);
        int end = 0;
        while (child != null && child.getRight() + dx <= 0) {
            this.mDisplayOffset += getChildWidth();
            cacheChildItem(child);
            this.mLeftViewIndex++;
            end++;
            child = getChildAt(end);
        }
        if (end > 0) {
            removeViewsInLayout(0, end + 0);
        }
        end = getChildCount() - 1;
        int start = end;
        child = getChildAt(getChildCount() - 1);
        while (child != null && child.getLeft() + dx >= getWidth()) {
            cacheChildItem(child);
            this.mRightViewIndex--;
            start--;
            child = getChildAt(start);
        }
        if (end > start) {
            removeViewsInLayout(start + 1, end - start);
        }
    }

    private void cacheChildItem(View view) {
        if (this.mRemovedViewQueue.size() < 10) {
            this.mRemovedViewQueue.offer(view);
        }
    }

    private void positionItems(int dx) {
        if (getChildCount() > 0) {
            this.mDisplayOffset += dx;
            int left = this.mDisplayOffset;
            int childWidth = getChildWidth();
            int childHeight = getHeight();
            int center = this.mPresetWidth / 2;
            int index = this.mLeftViewIndex + 1;
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                boolean oldCenter = child.getLeft() < center && child.getRight() > center;
                child.layout(left, 0, left + childWidth, childHeight);
                if (this.mSelectCenter && left < center && left + childWidth > center && !oldCenter) {
                    int dataIndex = toDataIndex(index);
                    notifyItemSelect(child, dataIndex, this.mAdapter.getItemId(dataIndex));
                }
                index++;
                left += childWidth;
            }
        }
    }

    private void loadItems() {
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i);
        }
    }

    public synchronized void scrollTo(int x) {
        this.mIsScrollingPerformed = true;
        this.mScroller.startScroll(this.mNextX, 0, x - this.mNextX, 0);
        requestLayout();
    }

    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean handled = super.dispatchTouchEvent(ev) | this.mGesture.onTouchEvent(ev);
        switch (ev.getAction()) {
            case 0:
                this.mTouchDown = true;
                this.mBlockNotification = false;
                break;
            case 1:
            case 3:
                if (this.mScroller.isFinished()) {
                    this.mIsScrollingPerformed = false;
                    justify();
                }
                this.mTouchDown = false;
                break;
        }
        return handled;
    }

    protected boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        synchronized (this) {
            this.mScroller.fling(this.mNextX, 0, (int) (-velocityX), 0, 0, this.mMaxX, 0, 0);
        }
        requestLayout();
        return true;
    }

    protected boolean onDown(MotionEvent e) {
        this.mScroller.forceFinished(true);
        return true;
    }

    public boolean isScrolling() {
        return this.mIsScrollingPerformed;
    }

    private void notifyItemSelect(View view, int index, long id) {
        if (view != null) {
            if (!this.mBlockNotification) {
                if (this.mOnItemClicked != null) {
                    this.mOnItemClicked.onItemClick(this, view, index, id);
                }
                if (this.mOnItemSelected != null) {
                    this.mOnItemSelected.onItemSelected(this, view, index, id);
                }
            }
            if (this.mLastSelectImageListItem != null) {
                this.mLastSelectImageListItem.setActivated(false);
            }
            this.mLastSelectImageListItem = view;
            view.setActivated(true);
        }
    }

    private int getChildWidth() {
        return this.mItemWidth;
    }
}
