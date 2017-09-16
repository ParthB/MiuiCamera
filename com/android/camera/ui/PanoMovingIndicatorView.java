package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.View;
import com.android.camera.CameraAppImpl;
import com.android.camera.Log;
import com.android.camera.Util;
import java.util.HashMap;

public class PanoMovingIndicatorView extends View {
    private static final int MAX_GAP = Util.dpToPixel(6.0f);
    private static final int SPEED_DEVIATION = (2904 / MAX_GAP);
    private static final int STONE_WIDTH = Util.dpToPixel(10.67f);
    public static final String TAG = PanoMovingIndicatorView.class.getSimpleName();
    private static int[] sBlockWidth = new int[]{Util.dpToPixel(0.67f), Util.dpToPixel(2.0f), Util.dpToPixel(3.34f)};
    private static int[] sGapWidth = new int[]{Util.dpToPixel(2.67f), Util.dpToPixel(2.0f), Util.dpToPixel(1.34f)};
    private static HashMap<Boolean, Integer> sTimesMap = new HashMap(2);
    private int mArrowMargin = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelOffset(R.dimen.pano_arrow_margin);
    private Point mCurrentFramePos = new Point();
    private int mDirection;
    private boolean mFast;
    private int mFilterMoveSpeed;
    private int mHalfStoneHeight;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    if (((float) PanoMovingIndicatorView.this.getPointGap(PanoMovingIndicatorView.this.mLastestSpeed)) != PanoMovingIndicatorView.this.mPointGap) {
                        PanoMovingIndicatorView.this.filterSpeed(PanoMovingIndicatorView.this.mLastestSpeed);
                        PanoMovingIndicatorView.this.applyNewGap();
                        sendEmptyMessageDelayed(1, 10);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private int mLastestSpeed;
    private Drawable mMovingDirectionIc = getResources().getDrawable(R.drawable.ic_pano_direction_right);
    private int mOffsetX;
    private float mPointGap = -1.0f;
    private int mPreivewCenterY;
    private StateChangeTrigger<Boolean> mStateChangeTrigger = new StateChangeTrigger(Boolean.valueOf(false), sTimesMap);
    private Paint mTailPaint = new Paint();

    class StateChangeTrigger<T> {
        private T mCurrentState;
        private T mLastestState;
        private int mLastestTimes = 0;
        private HashMap<T, Integer> mMaxTimesMap;

        public StateChangeTrigger(T dValue, HashMap<T, Integer> timesMap) {
            this.mLastestState = dValue;
            this.mCurrentState = dValue;
            this.mMaxTimesMap = timesMap;
        }

        public void setCurrentState(T state) {
            this.mCurrentState = state;
        }
    }

    static {
        sTimesMap.put(Boolean.valueOf(true), Integer.valueOf(1));
        sTimesMap.put(Boolean.valueOf(false), Integer.valueOf(4));
    }

    public PanoMovingIndicatorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTailPaint.setColor(-1);
        this.mHalfStoneHeight = ((int) (((float) this.mMovingDirectionIc.getIntrinsicHeight()) * 0.5625f)) / 2;
    }

    public void onDraw(Canvas canvas) {
        Log.v(TAG, "onDraw mPointGap=" + this.mPointGap);
        if (this.mCurrentFramePos.x != Integer.MIN_VALUE && this.mCurrentFramePos.y != Integer.MIN_VALUE) {
            int narrowStartX = this.mCurrentFramePos.x;
            int margin = this.mArrowMargin;
            Drawable drawable = this.mMovingDirectionIc;
            if (this.mDirection == 0) {
                narrowStartX += (this.mOffsetX + margin) + drawable.getIntrinsicWidth();
            } else if (1 == this.mDirection) {
                narrowStartX -= (this.mOffsetX + margin) + drawable.getIntrinsicWidth();
            }
            int centerY = ((getHeight() / 2) + this.mCurrentFramePos.y) - this.mPreivewCenterY;
            canvas.save();
            canvas.translate((float) narrowStartX, (float) centerY);
            if (1 == this.mDirection) {
                canvas.rotate(180.0f);
            }
            int left = -drawable.getIntrinsicWidth();
            drawable.setBounds(left, (-drawable.getIntrinsicHeight()) / 2, 0, drawable.getIntrinsicHeight() / 2);
            drawable.draw(canvas);
            left = (int) (((float) left) - (((float) STONE_WIDTH) + this.mPointGap));
            int gap = (int) this.mPointGap;
            for (int i = 0; i < sGapWidth.length && gap > 0; i++) {
                canvas.drawRect((float) left, (float) (-this.mHalfStoneHeight), (float) (sBlockWidth[i] + left), (float) this.mHalfStoneHeight, this.mTailPaint);
                left += sBlockWidth[i];
                if (gap >= sGapWidth[i]) {
                    left += 8;
                    gap -= 8;
                } else {
                    left += gap;
                    gap = 0;
                }
            }
            canvas.drawRect((float) left, (float) (-this.mHalfStoneHeight), (float) (-drawable.getIntrinsicWidth()), (float) this.mHalfStoneHeight, this.mTailPaint);
            if (1 == this.mDirection) {
                canvas.rotate(-180.0f);
            }
            canvas.translate((float) (-narrowStartX), (float) (-centerY));
            canvas.restore();
        }
    }

    public void setPosition(Point attachedPos, int previewRefY) {
        this.mCurrentFramePos.set(attachedPos.x, attachedPos.y);
        this.mPreivewCenterY = previewRefY;
        invalidate();
    }

    public void setMovingAttibute(int direction, int offsetX, int offsetY) {
        this.mDirection = direction & 1;
        this.mOffsetX = offsetX;
        this.mFast = false;
        this.mFilterMoveSpeed = 4096;
        this.mStateChangeTrigger.setCurrentState(Boolean.valueOf(this.mFast));
        this.mCurrentFramePos.set(Integer.MIN_VALUE, Integer.MIN_VALUE);
        this.mPointGap = -1.0f;
    }

    public void setToofast(boolean tooFast, int moveSpeed) {
        android.util.Log.i(TAG, "setToofast moveSpeed=" + moveSpeed + " fastFlag:" + tooFast);
        if (moveSpeed > 7000) {
            moveSpeed = 7000;
        }
        this.mLastestSpeed = moveSpeed;
        if (((float) getPointGap(this.mLastestSpeed)) != this.mPointGap && !this.mHandler.hasMessages(1)) {
            this.mHandler.sendEmptyMessage(1);
        }
    }

    private int getPointGap(int speed) {
        if (speed > 4096) {
            return (MAX_GAP * ((speed - 4096) + SPEED_DEVIATION)) / 2904;
        }
        return -1;
    }

    private void applyNewGap() {
        this.mPointGap = (float) getPointGap(this.mFilterMoveSpeed);
        invalidate();
    }

    public boolean isTooFast() {
        return this.mPointGap > 0.0f;
    }

    private void filterSpeed(int moveSpeed) {
        this.mFilterMoveSpeed = (int) ((((float) this.mFilterMoveSpeed) * 0.9f) + (((float) moveSpeed) * 0.1f));
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean isFar() {
        /*
        r4 = this;
        r3 = 0;
        r0 = r4.mCurrentFramePos;
        r0 = r0.y;
        r1 = -2147483648; // 0xffffffff80000000 float:-0.0 double:NaN;
        if (r0 == r1) goto L_0x000d;
    L_0x0009:
        r0 = r4.mPreivewCenterY;
        if (r0 != 0) goto L_0x000e;
    L_0x000d:
        return r3;
    L_0x000e:
        r0 = r4.mCurrentFramePos;
        r0 = r0.y;
        r1 = r4.mPreivewCenterY;
        r0 = r0 - r1;
        r0 = java.lang.Math.abs(r0);
        r0 = (float) r0;
        r1 = r4.mPreivewCenterY;
        r1 = (float) r1;
        r2 = 1048576000; // 0x3e800000 float:0.25 double:5.180653787E-315;
        r1 = r1 * r2;
        r0 = (r0 > r1 ? 1 : (r0 == r1 ? 0 : -1));
        if (r0 < 0) goto L_0x0050;
    L_0x0024:
        r0 = TAG;
        r1 = new java.lang.StringBuilder;
        r1.<init>();
        r2 = "too far current relative y is ";
        r1 = r1.append(r2);
        r2 = r4.mCurrentFramePos;
        r2 = r2.y;
        r1 = r1.append(r2);
        r2 = " refy is ";
        r1 = r1.append(r2);
        r2 = r4.mPreivewCenterY;
        r1 = r1.append(r2);
        r1 = r1.toString();
        android.util.Log.e(r0, r1);
        r0 = 1;
        return r0;
    L_0x0050:
        return r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ui.PanoMovingIndicatorView.isFar():boolean");
    }
}
