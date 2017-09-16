package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import com.android.camera.CameraSettings;
import com.android.camera.Log;
import com.android.camera.Util;

public class V6ShutterButtonInternal extends V6BottomAnimationImageView {
    private static int LONG_PRESSED_TRIGGER_TIME = 500;
    private static final int OUTER_CIRCLE_WIDTH = Util.dpToPixel(1.0f);
    private static final String TAG = V6ShutterButtonInternal.class.getSimpleName();
    private int FLING_DISTANCE_THRESHOLD = (Util.dpToPixel(400.0f) * Util.dpToPixel(400.0f));
    private int FLING_VELOCITY_THRESHOLD = (Util.dpToPixel(21.0f) * Util.dpToPixel(21.0f));
    private boolean mActionDown;
    private int mAnimationType = 0;
    private float mBigRadius;
    private boolean mCameraOpened;
    private float mCenterMaxRadius;
    private float mCenterMinRadius;
    private Paint mCenterPaint;
    private Path mCenterPath;
    private float mCenterRadius;
    private float mCenterThresholdRadius;
    private int mCenterX;
    private int mCenterY;
    private long mDuration;
    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    V6ShutterButtonInternal.this.onLongPress();
                    break;
                case 4:
                    V6ShutterButtonInternal.this.invalidate();
                    long duration = SystemClock.uptimeMillis() - V6ShutterButtonInternal.this.mStartTime;
                    if (duration <= V6ShutterButtonInternal.this.mDuration) {
                        float t = ((float) duration) / ((float) V6ShutterButtonInternal.this.mDuration);
                        V6ShutterButtonInternal v6ShutterButtonInternal = V6ShutterButtonInternal.this;
                        float -get1 = V6ShutterButtonInternal.this.mCenterMinRadius;
                        float -get0 = V6ShutterButtonInternal.this.mCenterMaxRadius - V6ShutterButtonInternal.this.mCenterMinRadius;
                        if (!V6ShutterButtonInternal.this.mIncreaseFlag) {
                            t = 1.0f - t;
                        }
                        v6ShutterButtonInternal.mCenterRadius = -get1 + (-get0 * t);
                        V6ShutterButtonInternal.this.mHandler.sendEmptyMessageDelayed(4, 20);
                        break;
                    }
                    V6ShutterButtonInternal.this.animationDone();
                    return;
            }
        }
    };
    private boolean mInShutterButton;
    private boolean mIncreaseFlag;
    private boolean mIsVideo = false;
    private boolean mLongClickable = true;
    private long mOutTime = -1;
    private Paint mOuterPaint;
    private Rect mShutterRect;
    private long mStartTime;
    private int mTargetImage;

    private void onLongPress() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(1, R.id.v6_shutter_button, 2, null, null);
        }
    }

    private void onClick() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_shutter_button, 2, null, null);
        }
    }

    private void onFocused(boolean focus) {
        Log.v(TAG, "onFocused  mMessageDispacher+" + this.mMessageDispacher);
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(3, R.id.v6_shutter_button, 2, Boolean.valueOf(focus), null);
        }
    }

    private void onFling(Point start, Point center) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(2, R.id.v6_shutter_button, 2, start, center);
        }
    }

    public V6ShutterButtonInternal(Context context, AttributeSet attrs) {
        super(context, attrs);
        enablePressFilter(false);
    }

    public void onCreate() {
        int i;
        super.onCreate();
        this.mIsVideo = V6ModulePicker.isVideoModule();
        if (this.mIsVideo) {
            i = R.drawable.video_shutter_button_start_bg;
        } else {
            i = R.drawable.camera_shutter_button_bg;
        }
        setImageResource(i);
    }

    public void onResume() {
        super.onResume();
        prepareAnimation();
    }

    public void onPause() {
        super.onPause();
        this.mCameraOpened = false;
        this.mHandler.removeMessages(0);
    }

    public void onCameraOpen() {
        super.onCameraOpen();
        this.mCameraOpened = true;
    }

    private void prepareAnimation() {
        int i = -1;
        if (this.mCenterPaint == null) {
            this.mCenterPath = new Path();
            this.mCenterPaint = new Paint();
            this.mCenterPaint.setAntiAlias(true);
            this.mCenterPaint.setStyle(Style.FILL);
            this.mOuterPaint = new Paint();
            this.mOuterPaint.setAntiAlias(true);
            this.mOuterPaint.setStyle(Style.STROKE);
            this.mOuterPaint.setStrokeWidth((float) OUTER_CIRCLE_WIDTH);
        }
        this.mCenterPaint.setColor(this.mIsVideo ? -1032447 : -1);
        Paint paint = this.mOuterPaint;
        if (this.mIsVideo) {
            i = -1862270977;
        }
        paint.setColor(i);
        this.mBigRadius = (float) (getDrawable().getIntrinsicWidth() / 2);
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
    }

    public void setLongClickable(boolean longClickable) {
        this.mLongClickable = longClickable;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        if (this.mCameraOpened) {
            int action = event.getActionMasked();
            float x = event.getRawX();
            float y = event.getRawY();
            switch (action) {
                case 0:
                    if (isEnabled()) {
                        if (this.mIsVideo || CameraSettings.isAudioCaptureOpen() || V6ModulePicker.isPanoramaModule()) {
                            setPressed(true);
                        } else {
                            doAnimate(2, 200);
                        }
                        this.mActionDown = true;
                        onFocused(true);
                        if (isEnabled() && CameraSettings.isPressDownCapture()) {
                            onClick();
                        }
                        if (this.mLongClickable) {
                            this.mHandler.removeMessages(0);
                            this.mHandler.sendEmptyMessageDelayed(0, (long) LONG_PRESSED_TRIGGER_TIME);
                            break;
                        }
                    }
                    break;
                case 1:
                case 3:
                case 6:
                    if (this.mActionDown && event.getActionIndex() == 0) {
                        boolean hasMessages = this.mLongClickable ? this.mHandler.hasMessages(0) : true;
                        this.mHandler.removeMessages(0);
                        this.mInShutterButton = Util.pointInView(x, y, this);
                        if (hasMessages && this.mInShutterButton && isEnabled() && !CameraSettings.isPressDownCapture()) {
                            onClick();
                        } else if (hasMessages && !this.mInShutterButton) {
                            checkGesture((int) x, (int) y);
                        }
                        if (this.mIsVideo || CameraSettings.isAudioCaptureOpen() || V6ModulePicker.isPanoramaModule()) {
                            setPressed(false);
                        } else {
                            doAnimate(1, 200);
                        }
                        onFocused(false);
                        this.mOutTime = -1;
                        this.mActionDown = false;
                        break;
                    }
                    break;
                case 2:
                    if (!Util.pointInView(x, y, this)) {
                        if (this.mOutTime == -1) {
                            this.mOutTime = System.currentTimeMillis();
                            break;
                        }
                    }
                    this.mOutTime = -1;
                    break;
                    break;
            }
            return true;
        }
        Log.d(TAG, "dispatchTouchEvent: drop event " + event);
        return false;
    }

    public boolean isCanceled() {
        return !this.mInShutterButton;
    }

    private void checkGesture(int x, int y) {
        if (this.mOutTime != -1) {
            if (this.mShutterRect == null) {
                this.mShutterRect = new Rect();
                getGlobalVisibleRect(this.mShutterRect);
            }
            int dx = x - this.mShutterRect.centerX();
            int dy = y - this.mShutterRect.centerY();
            int d2 = (dx * dx) + (dy * dy);
            int duration = (int) (System.currentTimeMillis() - this.mOutTime);
            Log.v(TAG, "gesture d2(d*d)=" + d2 + " duration=" + duration);
            if (duration <= 0 || this.FLING_VELOCITY_THRESHOLD > d2 / duration) {
                if (this.FLING_DISTANCE_THRESHOLD >= d2) {
                    return;
                }
            }
            onFling(new Point(x, y), new Point(this.mShutterRect.centerX(), this.mShutterRect.centerY()));
        }
    }

    public void changeImageWithAnimation(int resId, long duration) {
        if (resId == R.drawable.video_shutter_button_stop_bg || resId == R.drawable.video_shutter_button_start_bg || resId == R.drawable.pano_shutter_button_stop_bg || resId == R.drawable.camera_shutter_button_bg) {
            this.mTargetImage = resId;
            if (resId == R.drawable.video_shutter_button_start_bg || resId == R.drawable.camera_shutter_button_bg) {
                this.mIncreaseFlag = true;
            } else {
                this.mIncreaseFlag = false;
            }
            doAnimate(3, duration);
            return;
        }
        setImageResource(resId);
    }

    private void doAnimate(int type, long duration) {
        boolean z = true;
        if (this.mCenterPaint != null) {
            if (this.mAnimationType != 0) {
                animationDone();
            }
            this.mAnimationType = type;
            switch (this.mAnimationType) {
                case 1:
                case 2:
                    this.mCenterMaxRadius = this.mBigRadius * 0.9053f;
                    this.mCenterMinRadius = this.mBigRadius * 0.81477f;
                    if (this.mAnimationType != 1) {
                        z = false;
                    }
                    this.mIncreaseFlag = z;
                    break;
                case 3:
                    this.mCenterMaxRadius = this.mBigRadius * 0.9053f;
                    this.mCenterMinRadius = this.mBigRadius * 0.4713f;
                    this.mCenterThresholdRadius = this.mCenterMinRadius + ((this.mCenterMaxRadius - this.mCenterMinRadius) * 0.7f);
                    break;
            }
            this.mStartTime = SystemClock.uptimeMillis();
            this.mDuration = duration;
            this.mCenterRadius = this.mIncreaseFlag ? this.mCenterMinRadius : this.mCenterMaxRadius;
            this.mHandler.removeMessages(4);
            this.mHandler.sendEmptyMessage(4);
        }
    }

    private void animationDone() {
        switch (this.mAnimationType) {
            case 1:
                setPressed(false);
                break;
            case 2:
                setPressed(true);
                break;
            case 3:
                setImageResource(this.mTargetImage);
                break;
        }
        this.mAnimationType = 0;
    }

    protected void onDraw(Canvas canvas) {
        if (this.mAnimationType == 0) {
            super.onDraw(canvas);
            return;
        }
        if (this.mCenterX == 0) {
            this.mCenterX = (this.mRight - this.mLeft) / 2;
            this.mCenterY = (this.mBottom - this.mTop) / 2;
        }
        canvas.drawCircle((float) this.mCenterX, (float) this.mCenterY, this.mBigRadius - 2.0f, this.mOuterPaint);
        if (this.mAnimationType != 3 || this.mCenterRadius > this.mCenterThresholdRadius) {
            canvas.drawCircle((float) this.mCenterX, (float) this.mCenterY, this.mCenterRadius, this.mCenterPaint);
            return;
        }
        float length = this.mCenterRadius * 0.71f;
        float left = ((float) this.mCenterX) - length;
        float right = ((float) this.mCenterX) + length;
        float top = ((float) this.mCenterY) - length;
        float bottom = ((float) this.mCenterY) + length;
        float distance = length * ((((this.mCenterRadius - this.mCenterMinRadius) * 0.8f) / (this.mCenterThresholdRadius - this.mCenterMinRadius)) + 1.0f);
        this.mCenterPath.reset();
        this.mCenterPath.moveTo(left, top);
        this.mCenterPath.quadTo((float) this.mCenterX, ((float) this.mCenterY) - distance, right, top);
        this.mCenterPath.quadTo(((float) this.mCenterX) + distance, (float) this.mCenterY, right, bottom);
        this.mCenterPath.quadTo((float) this.mCenterX, ((float) this.mCenterY) + distance, left, bottom);
        this.mCenterPath.quadTo(((float) this.mCenterX) - distance, (float) this.mCenterY, left, top);
        this.mCenterPath.close();
        canvas.drawPath(this.mCenterPath, this.mCenterPaint);
    }
}
