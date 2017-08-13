package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SplitLineDrawer extends View {
    private boolean mBottomVisible = true;
    private int mColumnCount;
    private int mLineColor = 872415231;
    private int mRowCount;
    private boolean mTopVisible = true;

    public SplitLineDrawer(Context context) {
        super(context);
    }

    public SplitLineDrawer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SplitLineDrawer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setBorderVisible(boolean top, boolean bottom) {
        if (this.mTopVisible != top || this.mBottomVisible != bottom) {
            this.mTopVisible = top;
            this.mBottomVisible = bottom;
            invalidate();
        }
    }

    public void setLineColor(int lineColor) {
        this.mLineColor = lineColor;
    }

    public void initialize(int row, int column) {
        this.mColumnCount = column;
        this.mRowCount = row;
    }

    protected void onDraw(Canvas canvas) {
        int i;
        Paint paint = new Paint();
        paint.setColor(this.mLineColor);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        for (i = 1; i < this.mColumnCount; i++) {
            canvas.drawLine((float) ((i * w) / this.mColumnCount), 1.0f, (float) ((i * w) / this.mColumnCount), (float) (h - 1), paint);
        }
        int widthBorder = this.mBottomVisible ? 0 : 1;
        i = 0;
        while (i <= this.mRowCount) {
            if ((i == 0 || i == this.mRowCount) && !(i == 0 && this.mTopVisible)) {
                if (i == this.mRowCount && this.mBottomVisible) {
                }
                i++;
            }
            canvas.drawLine((float) widthBorder, (float) ((i * h) / this.mRowCount), (float) (w - widthBorder), (float) ((i * h) / this.mRowCount), paint);
            i++;
        }
        super.onDraw(canvas);
    }
}
