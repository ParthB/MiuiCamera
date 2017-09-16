package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.RelativeLayout;
import com.android.camera.Camera;
import com.android.camera.CameraAppImpl;
import com.android.camera.Util;
import com.android.camera.aosp_porting.animation.CubicEaseOutInterpolator;

public class V6EdgeShutterView extends View implements V6FunctionUI {
    private static final int CENTER_RADIUS = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.v6_edge_shutter_center_radius);
    private static final int NEAR_THRESHOLD = Util.dpToPixel(66.67f);
    private static int NORMAL_TAP_MAXY = ((Util.sWindowHeight * SystemProperties.getInt("camera_edge_max", 75)) / 100);
    private static int NORMAL_TOUCH_MAXY = (NORMAL_TAP_MAXY + (NEAR_THRESHOLD / 2));
    private static final int OUTER_CIRCLE_WIDTH = Util.dpToPixel(1.0f);
    private static final int OUT_RADIUS = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.v6_edge_shutter_out_radius);
    private static final int VIEW_WIDTH = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.v6_edge_shutter_width);
    private AnimatorListener mAnimatorListener;
    private Paint mCenterPaint;
    private ValueAnimator mClickAnim;
    private ValueAnimator mFlyOutAnim;
    private Interpolator mFlyinInterpolator = new OvershootInterpolator();
    private Handler mHandler = new Handler() {
        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case 0:
                case 2:
                    V6EdgeShutterView.this.hideShutterView();
                    return;
                case 1:
                    V6EdgeShutterView.this.checkPosture();
                    return;
                default:
                    return;
            }
        }
    };
    private MessageDispacher mMessageDispacher;
    private ValueAnimator mMoveAnim;
    private Interpolator mMoveInterpolator = new CubicEaseOutInterpolator();
    private Paint mOuterPaint;
    private Interpolator mPressInterpolator = new ReverseInterpolator();
    private Rect mVisableBount = new Rect(0, 0, Util.sWindowWidth, Util.sWindowHeight);
    private int mVisibleState;

    private class CustomAnimatorListener extends AnimatorListenerAdapter {
        private CustomAnimatorListener() {
        }

        public void onAnimationEnd(Animator animation) {
            Log.v("CameraEdgeShutterView", "onAnimationEnd animation=" + animation);
            if (animation == V6EdgeShutterView.this.mFlyOutAnim && V6EdgeShutterView.this.mVisibleState == 2) {
                V6EdgeShutterView.this.setRelateVisible(4);
                V6EdgeShutterView.this.mVisibleState = 4;
            } else if (animation == V6EdgeShutterView.this.mMoveAnim && V6EdgeShutterView.this.mVisibleState == 3) {
                V6EdgeShutterView.this.mVisibleState = 1;
            }
            V6EdgeShutterView.this.setX((float) V6EdgeShutterView.this.mLeft);
        }

        public void onAnimationStart(Animator animation) {
            if (animation == V6EdgeShutterView.this.mClickAnim) {
                V6EdgeShutterView.this.setX((float) V6EdgeShutterView.this.mLeft);
            }
        }

        public void onAnimationCancel(Animator animation) {
            Log.v("CameraEdgeShutterView", "onAnimationCancel animation=" + animation);
            V6EdgeShutterView.this.mVisibleState = 1;
        }
    }

    private class ReverseInterpolator implements Interpolator {
        private final Interpolator mInterpolator;

        private ReverseInterpolator(Interpolator interpolator) {
            if (interpolator == null) {
                interpolator = new AccelerateDecelerateInterpolator();
            }
            this.mInterpolator = interpolator;
        }

        private ReverseInterpolator(V6EdgeShutterView this$0) {
            this(new AccelerateDecelerateInterpolator());
        }

        public float getInterpolation(float input) {
            if (((double) input) <= 0.5d) {
                return this.mInterpolator.getInterpolation(input * 2.0f);
            }
            return this.mInterpolator.getInterpolation(Math.abs(input - 1.0f) * 2.0f);
        }
    }

    protected void onFinishInflate() {
        this.mCenterPaint = new Paint();
        this.mCenterPaint.setAntiAlias(true);
        this.mCenterPaint.setColor(-1);
        this.mCenterPaint.setStyle(Style.FILL);
        this.mOuterPaint = new Paint();
        this.mOuterPaint.setAntiAlias(true);
        this.mOuterPaint.setColor(-1);
        this.mOuterPaint.setStyle(Style.STROKE);
        this.mOuterPaint.setStrokeWidth((float) OUTER_CIRCLE_WIDTH);
        this.mAnimatorListener = new CustomAnimatorListener();
    }

    public V6EdgeShutterView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void onCreate() {
        if (V6ModulePicker.isVideoModule()) {
            this.mCenterPaint.setColor(-1032447);
            this.mOuterPaint.setColor(-1862270977);
            return;
        }
        this.mCenterPaint.setColor(-1);
        this.mOuterPaint.setColor(-1);
    }

    public void onCameraOpen() {
    }

    public void enableControls(boolean enable) {
    }

    public void onResume() {
    }

    public void onPause() {
        this.mHandler.removeMessages(0);
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        setRelateVisible(8);
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(VIEW_WIDTH, VIEW_WIDTH);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawCircle((float) (VIEW_WIDTH / 2), (float) (VIEW_WIDTH / 2), (float) (OUT_RADIUS - 2), this.mOuterPaint);
        canvas.drawCircle((float) (VIEW_WIDTH / 2), (float) (VIEW_WIDTH / 2), (float) CENTER_RADIUS, this.mCenterPaint);
    }

    private ValueAnimator createFlyOutAnimation() {
        int startX;
        int endX;
        if (this.mLeft < VIEW_WIDTH) {
            startX = 0;
            endX = -VIEW_WIDTH;
        } else {
            startX = 0;
            endX = VIEW_WIDTH;
        }
        ValueAnimator animFlyOut = ObjectAnimator.ofFloat(this, "translationX", new float[]{(float) startX, (float) endX});
        animFlyOut.setInterpolator(this.mMoveInterpolator);
        animFlyOut.setDuration(250);
        animFlyOut.addListener(this.mAnimatorListener);
        return animFlyOut;
    }

    public void cancelAnimation() {
        animate().cancel();
        setX((float) this.mLeft);
        setY((float) this.mTop);
    }

    private void hideShutterView() {
        if (this.mVisibleState == 1) {
            this.mFlyOutAnim = createFlyOutAnimation();
            this.mFlyOutAnim.start();
            this.mVisibleState = 2;
        }
    }

    private void checkPosture() {
        int posture = ((Camera) this.mContext).getCapturePosture();
        if ((posture != 1 || this.mLeft == 0) && !(posture == 2 && this.mLeft == 0)) {
            if (posture != 0 || this.mTop <= NORMAL_TAP_MAXY) {
                return;
            }
        }
        hideShutterView();
    }

    public void onDevicePostureChanged() {
        this.mHandler.sendEmptyMessage(1);
    }

    public void onDeviceMoving() {
        this.mHandler.sendEmptyMessage(2);
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
}
