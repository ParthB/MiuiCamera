package com.android.camera.ui;

public class NumericListAdapter {
    private int mGapValue;
    private int mMaxValue;
    private int mMinValue;

    public NumericListAdapter() {
        this(0, 9, 1);
    }

    public NumericListAdapter(int minValue, int maxValue, int gapValue) {
        this.mGapValue = 1;
        this.mMinValue = minValue;
        this.mMaxValue = maxValue;
        this.mGapValue = gapValue;
    }

    public String getItem(int index) {
        if (index < 0 || index >= getItemsCount()) {
            return null;
        }
        return Integer.toString(this.mMinValue + (this.mGapValue * index));
    }

    public int getItemValue(int index) {
        if (index < 0 || index >= getItemsCount()) {
            return -1;
        }
        return this.mMinValue + (this.mGapValue * index);
    }

    public int getItemsCount() {
        return ((this.mMaxValue - this.mMinValue) / this.mGapValue) + 1;
    }

    public int getItemIndexByValue(Object value) {
        int pureValue = 0;
        if (value instanceof Integer) {
            pureValue = ((Integer) value).intValue();
        }
        if (pureValue > this.mMaxValue || pureValue < this.mMinValue) {
            return -1;
        }
        return (int) ((((float) (pureValue - this.mMinValue)) / ((float) this.mGapValue)) + 0.5f);
    }
}
