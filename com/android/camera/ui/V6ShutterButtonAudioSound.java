package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;
import com.android.camera.Device;
import com.android.camera.Util;

public class V6ShutterButtonAudioSound extends ImageView implements V6FunctionUI {
    private static final int LINE_WIDTH = Util.dpToPixel((float) (Device.isPad() ? 2 : 1));
    private int mAlpha = 255;
    private int mCurrentRadius;
    private int mDelta;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    long duration = SystemClock.uptimeMillis() - V6ShutterButtonAudioSound.this.mStartTime;
                    if (duration <= 500) {
                        float t = ((float) duration) / 500.0f;
                        V6ShutterButtonAudioSound.this.mCurrentRadius = V6ShutterButtonAudioSound.this.mStartRadius + ((int) (((float) (V6ShutterButtonAudioSound.this.mMaxRadius - V6ShutterButtonAudioSound.this.mStartRadius)) * V6ShutterButtonAudioSound.this.getInterpolation(t)));
                        V6ShutterButtonAudioSound.this.mAlpha = ((int) (-255.0f * t)) + 255;
                        V6ShutterButtonAudioSound.this.mHandler.sendEmptyMessageDelayed(0, 20);
                        V6ShutterButtonAudioSound.this.invalidate();
                        break;
                    }
                    return;
                case 1:
                    V6ShutterButtonAudioSound.this.invalidate();
                    break;
            }
        }
    };
    private Interpolator mInterpolator = new BounceInterpolator();
    private int mMaxRadius;
    private int mMinRadius;
    private Paint mPaint;
    private int mProgress;
    private int mStartRadius;
    private long mStartTime;

    public V6ShutterButtonAudioSound(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setAudioProgress(float progress) {
        this.mProgress = (int) (100.0f * progress);
        if (this.mProgress < 0) {
            this.mProgress = -1;
            this.mHandler.removeMessages(1);
            this.mHandler.sendEmptyMessage(0);
            this.mHandler.sendEmptyMessageDelayed(1, 10);
            return;
        }
        if (!this.mHandler.hasMessages(0)) {
            this.mStartRadius = this.mMinRadius + ((int) ((((float) this.mDelta) * progress) * 0.8f));
            this.mCurrentRadius = this.mStartRadius;
            this.mAlpha = 255;
            this.mStartTime = SystemClock.uptimeMillis();
            this.mHandler.sendEmptyMessage(0);
        }
    }

    public void setRadius(int min, int max) {
        this.mMinRadius = min;
        this.mMaxRadius = (int) (((float) max) * 0.85f);
        this.mDelta = this.mMaxRadius - this.mMinRadius;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
    }

    public void onCreate() {
        setVisibility(8);
    }

    public void onCameraOpen() {
    }

    public void onResume() {
        this.mProgress = -1;
        if (V6ModulePicker.isCameraModule()) {
            setVisibility(0);
        } else {
            setVisibility(8);
        }
    }

    public void onPause() {
        this.mProgress = -1;
        setVisibility(8);
    }

    public void setMessageDispacher(MessageDispacher p) {
    }

    public void enableControls(boolean enable) {
    }

    protected void onDraw(Canvas canvas) {
        if (this.mProgress > 0) {
            if (this.mPaint == null) {
                this.mPaint = new Paint();
                this.mPaint.setAntiAlias(true);
                this.mPaint.setStrokeWidth((float) LINE_WIDTH);
                this.mPaint.setStyle(Style.STROKE);
            }
            this.mPaint.setColor(Color.argb(this.mAlpha, 255, 255, 255));
            canvas.drawCircle((float) (canvas.getWidth() / 2), (float) (canvas.getHeight() / 2), (float) this.mCurrentRadius, this.mPaint);
        }
    }

    private float getInterpolation(float t) {
        return this.mInterpolator.getInterpolation(t);
    }
}
