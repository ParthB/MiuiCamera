package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.View;

public class TwoStateBar extends View {
    private int mFutureEnd;
    private int mFutureStart;
    private int mPastEnd;
    private int mPastStart;

    public TwoStateBar(Context context) {
        super(context);
    }

    public void setStatePosition(int pastStart, int pastEnd, int futureStart, int futureEnd) {
        this.mPastStart = Math.max(0, pastStart - this.mLeft);
        this.mPastEnd = Math.min(getWidth(), pastEnd - this.mLeft);
        this.mFutureStart = Math.max(0, futureStart - this.mLeft);
        this.mFutureEnd = Math.min(getWidth(), futureEnd - this.mLeft);
        invalidate();
    }

    public void onDraw(Canvas canvas) {
        Paint p = new Paint();
        if (this.mPastStart < this.mPastEnd) {
            p.setColor(-16733953);
            canvas.drawRect((float) this.mPastStart, 0.0f, (float) this.mPastEnd, (float) getHeight(), p);
        }
        if (this.mFutureStart < this.mFutureEnd) {
            p.setColor(-1711276033);
            canvas.drawRect((float) this.mFutureStart, 0.0f, (float) this.mFutureEnd, (float) getHeight(), p);
        }
    }
}
