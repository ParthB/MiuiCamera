package com.android.camera.ui;

public class FloatSlideAdapter implements RollAdapter {
    private int mCenterItem;
    private float mGapValue;
    private int mItemCount;
    private int mMaxValue;
    private int mMinValue;

    public FloatSlideAdapter() {
        this(0, 9, 1.0f);
    }

    public FloatSlideAdapter(int minValue, int maxValue, float gapValue) {
        this.mGapValue = 1.0f;
        this.mMinValue = minValue;
        this.mMaxValue = maxValue;
        this.mGapValue = gapValue;
        this.mItemCount = (int) ((((float) (this.mMaxValue - this.mMinValue)) / this.mGapValue) + 1.0f);
        this.mCenterItem = (this.mItemCount - 1) / 2;
    }

    public int getItemValue(int index) {
        if (index < 0 || index >= getItemsCount()) {
            return -1;
        }
        return round(((float) this.mMinValue) + (((float) index) * this.mGapValue));
    }

    private int round(float f) {
        return (int) ((f < 0.0f ? -0.5d : 0.5d) + ((double) f));
    }

    public int getItemsCount() {
        return this.mItemCount;
    }

    public int getCenterIndex() {
        return this.mCenterItem;
    }

    public int getMaxItem() {
        return this.mItemCount - 1;
    }

    public int getItemIndexByValue(Object value) {
        int pureValue = 0;
        if (value instanceof Integer) {
            pureValue = ((Integer) value).intValue();
        }
        if (pureValue < this.mMinValue || pureValue > this.mMaxValue) {
            pureValue = (this.mMinValue + this.mMaxValue) / 2;
        }
        return (int) ((((float) (pureValue - this.mMinValue)) / this.mGapValue) + 0.5f);
    }
}
