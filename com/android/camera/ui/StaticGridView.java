package com.android.camera.ui;

import android.content.Context;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

/* compiled from: SettingScreenView */
class StaticGridView extends ViewGroup {
    private int mChildHeight;
    private int mChildWidth;
    private int mColumnCount;
    private int mRowCount;

    public StaticGridView(Context context, int rowCount, int columnCount, int childWidth, int childHeight) {
        super(context);
        set(rowCount, columnCount, childWidth, childHeight);
        setDrawingCacheEnabled(true);
        setWillNotDraw(false);
    }

    public void set(int rowCount, int columnCount, int childWidth, int childHeight) {
        this.mRowCount = Math.max(1, rowCount);
        this.mColumnCount = Math.max(1, columnCount);
        this.mChildHeight = Math.max(1, childHeight);
        this.mChildWidth = Math.max(1, childWidth);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureDimension(widthMeasureSpec, this.mChildWidth * this.mColumnCount), measureDimension(heightMeasureSpec, this.mChildHeight * this.mRowCount));
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
        int rowIndex = index / this.mColumnCount;
        int columnIndex = index % this.mColumnCount;
        if (1 == getLayoutDirection()) {
            columnIndex = (this.mColumnCount - 1) - columnIndex;
        }
        getChildAt(index).layout((this.mChildWidth * columnIndex) + 1, (this.mChildHeight * rowIndex) + 1, (this.mChildWidth * (columnIndex + 1)) - 1, (this.mChildHeight * (rowIndex + 1)) - 1);
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        doLayout(left, top, right, bottom);
    }

    protected void doLayout(int left, int top, int right, int bottom) {
        for (int i = 0; i < getChildCount(); i++) {
            layoutChildByIndex(i);
        }
    }

    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
    }
}
