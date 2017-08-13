package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.RelativeLayout.LayoutParams;
import com.android.camera.ActivityBase;
import com.android.camera.Camera;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;

public class ObjectView extends FrameView {
    private static final int NEAR_EDGE = Util.dpToPixel(35.0f);
    private static final int[] OBJECT_TRACKING_ICON = new int[]{R.drawable.ic_object_tracking, R.drawable.ic_object_tracking_succeed};
    private static final int RECT_EDGE_WIDTH = Util.dpToPixel(10.0f);
    private static final int TOUCH_TOLERANCE = Util.dpToPixel(25.0f);
    private static final int VERTEXS_TOUCH_TOLERANCE = Util.dpToPixel(35.0f);
    private final RectF mDisplayBounds = new RectF();
    private Runnable mEndAction = new EndAction();
    protected CameraHardwareFace mFace;
    private Matrix mFace2UIMatrix = new Matrix();
    private Filter mFilter = new Filter();
    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    ObjectView.this.showStart();
                    if (!(ObjectView.this.mListener == null || ObjectView.this.getFocusRect() == null)) {
                        ObjectView.this.mListener.startObjectTracking();
                        ObjectView.this.mFilter.resetState();
                    }
                    sendEmptyMessageDelayed(2, 1000);
                    return;
                case 2:
                    if (ObjectView.this.mListener != null) {
                        ObjectView.this.mListener.stopObjectTracking(true);
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    };
    private float mLastX;
    private float mLastY;
    private ObjectViewListener mListener;
    private int mLostTrackThreshold;
    private int mLostTrackingNum = 0;
    private int mMovingMode;
    protected RectF mObjectRect = new RectF();
    protected RectF mOldObjectRect = new RectF();
    private int mPreviewHeight;
    private int mPreviewWidth;
    private Runnable mStartAction = new StartAction();
    protected Drawable mTrackIndicator;
    private Drawable[] mTrackStatusIndicator = new Drawable[OBJECT_TRACKING_ICON.length];
    private int mZoomAnimaState = 0;

    public interface ObjectViewListener {
        void onObjectStable();

        void startObjectTracking();

        void stopObjectTracking(boolean z);
    }

    private class EndAction implements Runnable {
        private EndAction() {
        }

        public void run() {
            ObjectView.this.mZoomAnimaState = 5;
        }
    }

    private class Filter {
        int mCurrentValue;
        int mHoldTimes;
        int mHoldValue;

        private Filter() {
            this.mCurrentValue = 0;
            this.mHoldValue = -1;
        }

        private int filter(int stop) {
            if (stop != this.mHoldValue) {
                this.mHoldValue = stop;
                this.mHoldTimes = 1;
            } else {
                if (this.mHoldTimes < 4) {
                    this.mHoldTimes++;
                }
                if (this.mHoldTimes == 4 && this.mHoldValue != this.mCurrentValue) {
                    this.mCurrentValue = this.mHoldValue;
                }
            }
            return this.mCurrentValue;
        }

        public void resetState() {
            this.mCurrentValue = 0;
            this.mHoldValue = -1;
        }

        public boolean isBecomeStable(int faceStop) {
            int lastValue = this.mCurrentValue;
            int stop = filter(faceStop);
            if (lastValue == 0 && stop == 1) {
                return true;
            }
            return false;
        }
    }

    private class StartAction implements Runnable {
        private StartAction() {
        }

        public void run() {
            ObjectView.this.mZoomAnimaState = 3;
        }
    }

    public ObjectView(Context context, AttributeSet attr) {
        super(context, attr);
        for (int i = 0; i < OBJECT_TRACKING_ICON.length; i++) {
            this.mTrackStatusIndicator[i] = this.mContext.getResources().getDrawable(OBJECT_TRACKING_ICON[i]);
        }
        this.mTrackIndicator = this.mTrackStatusIndicator[0];
        this.mContext = (Camera) context;
    }

    public void setObject(CameraHardwareFace face) {
        if (Util.sIsDumpLog) {
            Log.i("ObjectView", "setObject(), mZoomAnimaState:" + this.mZoomAnimaState + " , face.rect:" + face.rect + " , face.score:" + face.score + " , face.t2tStop:" + face.t2tStop + " , moving=" + this.mFilter.mCurrentValue + " , mPause=" + this.mPause + " , visible=" + getVisibility() + " , getWidth()=" + getWidth() + " ,mDisplayBounds.width()=" + this.mDisplayBounds.width() + " , getHeight()=" + getHeight() + " ,mDisplayBounds.height()=" + this.mDisplayBounds.height());
        }
        if (this.mZoomAnimaState > 2) {
            if (this.mZoomAnimaState == 3) {
                if (face.score >= 1) {
                    this.mHandler.removeMessages(2);
                    showSuccess();
                }
            } else if (this.mZoomAnimaState == 5 && !this.mPause) {
                boolean z;
                if (face.score < 1) {
                    if (this.mLostTrackThreshold == 0) {
                        if (this.mObjectRect.left - this.mDisplayBounds.left < ((float) NEAR_EDGE) || this.mObjectRect.top - this.mDisplayBounds.top < ((float) NEAR_EDGE) || this.mDisplayBounds.right - this.mObjectRect.right < ((float) NEAR_EDGE) || this.mDisplayBounds.bottom - this.mObjectRect.bottom < ((float) NEAR_EDGE)) {
                            this.mLostTrackThreshold = 150;
                        } else {
                            this.mLostTrackThreshold = 50;
                        }
                    }
                    this.mLostTrackingNum++;
                    if (this.mLostTrackingNum % 5 == 0 || this.mLostTrackingNum == this.mLostTrackThreshold) {
                        Log.v("ObjectView", "lost " + this.mLostTrackingNum + " times");
                    }
                    if (this.mLostTrackThreshold <= this.mLostTrackingNum) {
                        this.mListener.stopObjectTracking(true);
                    }
                } else {
                    this.mLostTrackThreshold = 0;
                    this.mLostTrackingNum = 0;
                }
                if (((double) face.rect.width()) / ((double) this.mPreviewWidth) > 0.33d) {
                    z = true;
                } else if (((double) face.rect.height()) / ((double) this.mPreviewHeight) > 0.33d) {
                    z = true;
                } else {
                    z = false;
                }
                this.mIsBigEnoughRect = z;
                if (((float) getWidth()) != this.mDisplayBounds.width() || ((float) getHeight()) != this.mDisplayBounds.height()) {
                    resetView();
                } else if (face == null || face.score < 1) {
                    this.mFace = null;
                } else {
                    this.mFace = face;
                    this.mTrackIndicator = this.mTrackStatusIndicator[1];
                    this.mObjectRect.set(mapRect(new RectF(this.mFace.rect), false));
                    this.mOldObjectRect.set(this.mObjectRect);
                    if (this.mFilter.isBecomeStable(face.t2tStop)) {
                        this.mListener.onObjectStable();
                    }
                }
                invalidate();
            }
        }
    }

    public boolean isAdjusting() {
        if (this.mMovingMode != 0) {
            return true;
        }
        if (this.mObjectRect.width() <= 0.0f || this.mObjectRect.height() <= 0.0f) {
            return false;
        }
        return this.mZoomAnimaState == 0;
    }

    public RectF getFocusRect() {
        if (((ActivityBase) getContext()).getCameraScreenNail() == null || this.mPause || this.mObjectRect.width() <= 0.0f || this.mObjectRect.height() <= 0.0f) {
            return null;
        }
        return this.mObjectRect;
    }

    public RectF getFocusRectInPreviewFrame() {
        RectF rect = getFocusRect();
        if (rect != null) {
            return mapRect(rect, true);
        }
        return null;
    }

    public boolean faceExists() {
        return this.mFace == null ? isAdjusting() : true;
    }

    public void setObjectViewListener(ObjectViewListener listener) {
        this.mListener = listener;
    }

    public boolean initializeTrackView(RectF rectF, boolean up) {
        if (rectF != null) {
            CameraDataAnalytics.instance().trackEvent("t2t_times");
            rectF.intersect(this.mDisplayBounds);
            if (rectF.width() < 200.0f || rectF.height() < 200.0f) {
                return false;
            }
            this.mObjectRect.set(rectF);
            this.mOldObjectRect.set(rectF);
        } else {
            this.mObjectRect.set(this.mOldObjectRect);
        }
        setVisibility(0);
        resume();
        requestLayout();
        if (up) {
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessageDelayed(1, 0);
        }
        return true;
    }

    private boolean isInObjectViewArea(float x, float y) {
        if (x < this.mObjectRect.left - ((float) RECT_EDGE_WIDTH) || x >= this.mObjectRect.right + ((float) RECT_EDGE_WIDTH) || y < this.mObjectRect.top - ((float) RECT_EDGE_WIDTH) || y >= this.mObjectRect.bottom + ((float) RECT_EDGE_WIDTH)) {
            return false;
        }
        return true;
    }

    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        if (this.mPause || ((this.mMovingMode == 0 && !isInObjectViewArea(x, y)) || this.mLostTrackingNum != 0)) {
            return false;
        }
        if (isEnabled()) {
            switch (event.getActionMasked()) {
                case 0:
                    if (!this.mHandler.hasMessages(1)) {
                        if (this.mListener != null) {
                            if (this.mZoomAnimaState == 4 || this.mZoomAnimaState == 2) {
                                clearAnimation();
                            }
                            this.mZoomAnimaState = 0;
                            this.mListener.stopObjectTracking(false);
                        }
                        initializeTrackView(null, false);
                    }
                    this.mHandler.removeMessages(1);
                    detectMovingStyle(x, y, -1.0f, -1.0f);
                    this.mLastX = x;
                    this.mLastY = y;
                    break;
                case 1:
                case 3:
                    this.mMovingMode = 0;
                    invalidate();
                    this.mHandler.sendEmptyMessageDelayed(1, 0);
                    break;
                case 2:
                    if (this.mMovingMode != 0) {
                        moveObjectView(event);
                    }
                    this.mLastX = x;
                    this.mLastY = y;
                    break;
                case 5:
                    if (event.getPointerCount() == 2) {
                        detectMovingStyle(event.getX(0), event.getY(0), event.getX(1), event.getY(1));
                        if (this.mMovingMode == 32 && this.mListener != null) {
                            if (this.mZoomAnimaState == 4 || this.mZoomAnimaState == 2) {
                                clearAnimation();
                            }
                            this.mZoomAnimaState = 0;
                            this.mListener.stopObjectTracking(false);
                            moveObjectView(event);
                            break;
                        }
                    }
                    break;
                case 6:
                    if (event.getActionIndex() < 2 && this.mMovingMode == 32) {
                        this.mLastX = event.getX(1 - event.getActionIndex());
                        this.mLastY = event.getY(1 - event.getActionIndex());
                        detectMovingStyle(this.mLastX, this.mLastY, -1.0f, -1.0f);
                        break;
                    }
            }
        }
        return true;
    }

    private void detectMovingStyle(float x0, float y0, float x1, float y1) {
        int i = 1;
        if (x1 < 0.0f || y1 < 0.0f) {
            int i2;
            this.mMovingMode = 0;
            float left = Math.abs(x0 - this.mObjectRect.left);
            float right = Math.abs(x0 - this.mObjectRect.right);
            if (left <= ((float) TOUCH_TOLERANCE) && left < right) {
                this.mMovingMode |= 1;
            } else if (right <= ((float) TOUCH_TOLERANCE)) {
                this.mMovingMode |= 4;
            }
            float top = Math.abs(y0 - this.mObjectRect.top);
            float bottom = Math.abs(y0 - this.mObjectRect.bottom);
            if (top <= ((float) TOUCH_TOLERANCE)) {
                i2 = 1;
            } else {
                i2 = 0;
            }
            if (top >= bottom) {
                i = 0;
            }
            if ((i & i2) != 0) {
                this.mMovingMode |= 2;
            } else if (bottom <= ((float) TOUCH_TOLERANCE)) {
                this.mMovingMode |= 8;
            }
            if (this.mObjectRect.contains(x0, y0) && this.mMovingMode == 0) {
                this.mMovingMode = 16;
            }
        } else if (Math.abs(this.mObjectRect.left - Math.min(x0, x1)) <= ((float) VERTEXS_TOUCH_TOLERANCE) && Math.abs(this.mObjectRect.right - Math.max(x0, x1)) <= ((float) VERTEXS_TOUCH_TOLERANCE) && Math.abs(this.mObjectRect.top - Math.min(y0, y1)) <= ((float) VERTEXS_TOUCH_TOLERANCE) && Math.abs(this.mObjectRect.bottom - Math.max(y0, y1)) <= ((float) VERTEXS_TOUCH_TOLERANCE)) {
            this.mMovingMode = 32;
        }
        invalidate();
    }

    private void moveObjectView(MotionEvent event) {
        if (this.mMovingMode == 32) {
            float x0 = event.getX(0);
            float y0 = event.getY(0);
            float x1 = event.getX(1);
            float y1 = event.getY(1);
            if (200.0f < Math.abs(x1 - x0)) {
                this.mObjectRect.left = Math.min(x0, x1);
                this.mObjectRect.right = Math.max(x0, x1);
            }
            if (200.0f < Math.abs(y1 - y0)) {
                this.mObjectRect.top = Math.min(y0, y1);
                this.mObjectRect.bottom = Math.max(y0, y1);
            }
        } else {
            float deltaX = event.getX() - this.mLastX;
            float deltaY = event.getY() - this.mLastY;
            if (this.mMovingMode == 16) {
                if (deltaX > 0.0f) {
                    deltaX = Math.min(this.mDisplayBounds.right - this.mObjectRect.right, deltaX);
                } else {
                    deltaX = Math.max(this.mDisplayBounds.left - this.mObjectRect.left, deltaX);
                }
                if (deltaY > 0.0f) {
                    deltaY = Math.min(this.mDisplayBounds.bottom - this.mObjectRect.bottom, deltaY);
                } else {
                    deltaY = Math.max(this.mDisplayBounds.top - this.mObjectRect.top, deltaY);
                }
                this.mObjectRect.offset(deltaX, deltaY);
            } else {
                if ((this.mMovingMode & 1) != 0) {
                    this.mObjectRect.left = Math.min(this.mObjectRect.left + deltaX, this.mObjectRect.right - 200.0f);
                }
                if ((this.mMovingMode & 2) != 0) {
                    this.mObjectRect.top = Math.min(this.mObjectRect.top + deltaY, this.mObjectRect.bottom - 200.0f);
                }
                if ((this.mMovingMode & 4) != 0) {
                    this.mObjectRect.right = Math.max(this.mObjectRect.right + deltaX, this.mObjectRect.left + 200.0f);
                }
                if ((this.mMovingMode & 8) != 0) {
                    this.mObjectRect.bottom = Math.max(this.mObjectRect.bottom + deltaY, this.mObjectRect.top + 200.0f);
                }
                this.mObjectRect.intersect(this.mDisplayBounds);
            }
        }
        invalidate();
    }

    private void updateAnimateView() {
        LayoutParams params = (LayoutParams) getLayoutParams();
        params.width = (int) this.mObjectRect.width();
        params.height = (int) this.mObjectRect.height();
        params.setMargins((int) this.mObjectRect.left, (int) this.mObjectRect.top, 0, 0);
        requestLayout();
    }

    public void showStart() {
        Log.i("ObjectView", "showStart()");
        if (this.mZoomAnimaState == 0) {
            this.mZoomAnimaState = 1;
            this.mTrackIndicator = this.mTrackStatusIndicator[0];
            updateAnimateView();
            setBackground(this.mTrackIndicator);
            animate().withLayer().setDuration(150).scaleX(1.2f).scaleY(1.2f).withEndAction(this.mStartAction);
        }
    }

    public void showSuccess() {
        Log.i("ObjectView", "showSuccess()");
        if (this.mZoomAnimaState == 3) {
            this.mZoomAnimaState = 4;
            this.mTrackIndicator = this.mTrackStatusIndicator[1];
            updateAnimateView();
            setBackground(this.mTrackIndicator);
            animate().withLayer().setDuration(80).scaleX(0.7f).scaleY(0.7f).withEndAction(this.mEndAction);
        }
    }

    public void showFail() {
    }

    public void clear() {
        this.mFace = null;
        this.mOldObjectRect.set(this.mObjectRect);
        this.mObjectRect.set(0.0f, 0.0f, 0.0f, 0.0f);
        resetView();
        this.mZoomAnimaState = 0;
        invalidate();
    }

    protected void onDraw(Canvas canvas) {
        if (Util.sIsDumpLog) {
            Log.v("ObjectView", "onDraw(), mZoomAnimaState:" + this.mZoomAnimaState + ", mPause=" + this.mPause + " mObjectRect=" + this.mObjectRect + " mFace=" + this.mFace + ", mTrackIndicator=" + this.mTrackIndicator + ", getWidth=" + getWidth() + ", getheight=" + getHeight() + ", mDisplayBounds=" + this.mDisplayBounds);
        }
        if (this.mPause || (this.mZoomAnimaState != 0 && (this.mZoomAnimaState != 5 || this.mFace == null))) {
            super.onDraw(canvas);
            return;
        }
        canvas.save();
        canvas.translate(this.mObjectRect.centerX(), this.mObjectRect.centerY());
        this.mTrackIndicator.setBounds(Math.round((-(this.mObjectRect.right - this.mObjectRect.left)) / 2.0f), Math.round((-(this.mObjectRect.bottom - this.mObjectRect.top)) / 2.0f), Math.round((this.mObjectRect.right - this.mObjectRect.left) / 2.0f), Math.round((this.mObjectRect.bottom - this.mObjectRect.top) / 2.0f));
        this.mTrackIndicator.draw(canvas);
        canvas.restore();
    }

    private void resetView() {
        LayoutParams params = (LayoutParams) getLayoutParams();
        setBackground(null);
        animate().cancel();
        setScaleX(1.0f);
        setScaleY(1.0f);
        params.width = (int) this.mDisplayBounds.width();
        params.height = (int) this.mDisplayBounds.height();
        params.setMargins(0, 0, 0, 0);
        this.mTrackIndicator = this.mTrackStatusIndicator[0];
    }

    public boolean isTrackFailed() {
        return this.mFace == null || 1 > this.mFace.score;
    }

    public void setDisplaySize(int width, int height) {
        this.mDisplayBounds.set(0.0f, 0.0f, (float) width, (float) height);
        setMatrix();
    }

    public void setPreviewSize(int width, int height) {
        this.mPreviewWidth = width;
        this.mPreviewHeight = height;
        setMatrix();
    }

    private void setMatrix() {
        if (this.mPreviewWidth != 0 && this.mPreviewHeight != 0 && this.mDisplayBounds.width() != 0.0f && this.mDisplayBounds.height() != 0.0f) {
            this.mMatrix.reset();
            this.mMatrix.postScale(((float) this.mPreviewWidth) / this.mDisplayBounds.width(), ((float) this.mPreviewHeight) / this.mDisplayBounds.height());
            this.mFace2UIMatrix.reset();
            this.mMatrix.invert(this.mFace2UIMatrix);
        }
    }

    private RectF mapRect(RectF rect, boolean uiToDevice) {
        RectF result = new RectF();
        result.set(rect);
        if (uiToDevice) {
            this.mMatrix.mapRect(result);
        } else {
            this.mFace2UIMatrix.mapRect(result);
        }
        return result;
    }
}
