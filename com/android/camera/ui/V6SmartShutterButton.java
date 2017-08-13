package com.android.camera.ui;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.RelativeLayout;
import com.android.camera.CameraSettings;
import com.android.camera.Util;

public class V6SmartShutterButton extends RotateImageView implements V6FunctionUI {
    private static final int DISMISS_DISTANCE_THRESHOLD = Util.dpToPixel(70.0f);
    private static final int FADEOUT_BOUNT_THRESHOLD = Util.dpToPixel(10.0f);
    private static final int MOVE_THRESHOLD = Util.dpToPixel(30.0f);
    private static int UNUSED_TRIGGER_TIME = 15000;
    private static double sDeviceScreenInches;
    private Animation mFadeout;
    private int mFixedShutterCenterX;
    private int mFixedShutterCenterY;
    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    V6SmartShutterButton.this.mVisibleState = 1;
                    V6SmartShutterButton.this.updateVisibleState();
                    return;
                default:
                    return;
            }
        }
    };
    private boolean mInShutterButton;
    private MessageDispacher mMessageDispacher;
    private Rect mMoveBount;
    private int mOriginX;
    private int mOriginY;
    private int mState = 0;
    private Rect mVisableBount;
    private int mVisibleState;

    public V6SmartShutterButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        this.mVisableBount = new Rect(0, getResources().getDimensionPixelSize(R.dimen.camera_control_top_height), dm.widthPixels, dm.heightPixels - getResources().getDimensionPixelSize(R.dimen.camera_control_bottom_height));
        this.mMoveBount = new Rect(this.mVisableBount.left - FADEOUT_BOUNT_THRESHOLD, this.mVisableBount.top - FADEOUT_BOUNT_THRESHOLD, this.mVisableBount.right + FADEOUT_BOUNT_THRESHOLD, this.mVisableBount.bottom + FADEOUT_BOUNT_THRESHOLD);
    }

    private void onClick() {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(0, R.id.v6_shutter_button, 2, null, null);
        }
    }

    private void onFocused(boolean focus) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(3, R.id.v6_shutter_button, 2, Boolean.valueOf(focus), null);
        }
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
    }

    public void enableControls(boolean enable) {
        setEnabled(enable);
    }

    public void onResume() {
    }

    public void onPause() {
        this.mHandler.removeMessages(0);
        setRelateVisible(8);
        this.mVisibleState = 2;
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        int x = (int) event.getRawX();
        int y = (int) event.getRawY();
        switch (event.getAction()) {
            case 0:
                this.mOriginX = x;
                this.mOriginY = y;
                this.mState = 0;
                setPressed(true);
                onFocused(true);
                this.mHandler.removeMessages(0);
                break;
            case 1:
            case 3:
                setPressed(false);
                this.mHandler.sendEmptyMessageDelayed(0, (long) UNUSED_TRIGGER_TIME);
                this.mInShutterButton = Util.pointInView((float) x, (float) y, this);
                if (this.mInShutterButton && this.mState != 1) {
                    CameraSettings.setSmartShutterPosition(x + "x" + y);
                    onClick();
                }
                onFocused(false);
                updateVisibleState();
                break;
            case 2:
                Rect rect;
                if (this.mState != 1) {
                    if (MOVE_THRESHOLD <= getDistanceFromPoint(x, y, this.mOriginX, this.mOriginY)) {
                        this.mState = 1;
                    }
                }
                if (nearCenterOfShutter(x, y)) {
                    this.mVisibleState = 2;
                    setAlpha(0.6f);
                    rect = new Rect(this.mFixedShutterCenterX - (getWidth() / 2), this.mFixedShutterCenterY - (getHeight() / 2), this.mFixedShutterCenterX + (getWidth() / 2), this.mFixedShutterCenterY + (getHeight() / 2));
                } else {
                    setAlpha(1.0f);
                    rect = reviseLocation(x, y, this.mMoveBount);
                    if (this.mVisableBount.contains(rect)) {
                        if (this.mVisibleState == 1 && this.mFadeout != null) {
                            this.mFadeout.cancel();
                        }
                        this.mVisibleState = 0;
                    }
                }
                setDisplayPosition(rect.left, rect.top);
                break;
        }
        return true;
    }

    private int getDistanceFromPoint(int x, int y, int originX, int originY) {
        int tmpx = Math.abs(originX - x);
        int tmpy = Math.abs(originY - y);
        return (int) Math.sqrt((double) ((tmpx * tmpx) + (tmpy * tmpy)));
    }

    private boolean nearCenterOfShutter(int x, int y) {
        int tmpx = Math.abs(this.mFixedShutterCenterX - x);
        int tmpy = Math.abs(this.mFixedShutterCenterY - y);
        if (tmpx > DISMISS_DISTANCE_THRESHOLD || tmpy > DISMISS_DISTANCE_THRESHOLD || Math.sqrt((double) ((tmpx * tmpx) + (tmpy * tmpy))) >= ((double) DISMISS_DISTANCE_THRESHOLD)) {
            return false;
        }
        return true;
    }

    private void updateVisibleState() {
        if (this.mFadeout == null) {
            this.mFadeout = new AlphaAnimation(1.0f, 0.01f);
            this.mFadeout.setStartOffset(500);
            this.mFadeout.setDuration(2000);
        }
        switch (this.mVisibleState) {
            case 0:
                this.mFadeout.cancel();
                setRelateVisible(0);
                return;
            case 1:
                setAnimation(this.mFadeout);
                this.mFadeout.start();
                setRelateVisible(4);
                return;
            case 2:
                clearAnimation();
                setAlpha(1.0f);
                setRelateVisible(4);
                return;
            default:
                return;
        }
    }

    public void flyin(int endx, int endy, int centerX, int centerY) {
        this.mFixedShutterCenterX = centerX;
        this.mFixedShutterCenterY = centerY;
        if (needMovableShutter() && !isShown()) {
            if (this.mFadeout != null) {
                this.mFadeout.cancel();
            }
            setRelateVisible(0);
            setAlpha(0.01f);
            setDisplayPosition(centerX - (getWidth() / 2), centerY - (getHeight() / 2));
            String position = CameraSettings.getSmartShutterPosition();
            int x = -1;
            int y = -1;
            if (position != null) {
                int index = position.indexOf(120);
                if (index != -1) {
                    x = Integer.parseInt(position.substring(0, index));
                    y = Integer.parseInt(position.substring(index + 1));
                }
            }
            if (x == -1 && y == -1) {
                x = endx;
                y = endy;
            }
            Rect rec = reviseLocation(x, y, this.mVisableBount);
            animate().alpha(1.0f).x((float) rec.left).y((float) rec.top).setDuration(400);
            this.mVisibleState = 0;
            this.mHandler.removeMessages(0);
            this.mHandler.sendEmptyMessageDelayed(0, (long) UNUSED_TRIGGER_TIME);
        }
    }

    private void setRelateVisible(int visible) {
        int i = 8;
        RelativeLayout viewGroup = (RelativeLayout) getParent();
        if (viewGroup != null) {
            if (8 != visible) {
                i = 0;
            }
            viewGroup.setVisibility(i);
        }
        setVisibility(visible);
    }

    private void setDisplayPosition(int x, int y) {
        setX((float) x);
        setY((float) y);
    }

    private Rect reviseLocation(int x, int y, Rect bound) {
        Rect rec = new Rect(x - (getWidth() / 2), y - (getHeight() / 2), (getWidth() / 2) + x, (getHeight() / 2) + y);
        if (bound.contains(rec)) {
            return rec;
        }
        if (rec.left < bound.left) {
            rec.right = bound.left + rec.width();
            rec.left = bound.left;
        } else if (rec.right > bound.right) {
            rec.left = bound.right - rec.width();
            rec.right = bound.right;
        }
        if (rec.top < bound.top) {
            rec.bottom = bound.top + rec.height();
            rec.top = bound.top;
        } else if (rec.bottom > bound.bottom) {
            rec.top = bound.bottom - rec.height();
            rec.bottom = bound.bottom;
        }
        return rec;
    }

    private boolean needMovableShutter() {
        if (sDeviceScreenInches == 0.0d) {
            sDeviceScreenInches = Util.getScreenInches(getContext());
        }
        return sDeviceScreenInches > 4.9d;
    }
}
