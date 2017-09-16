package com.android.camera.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.Camera.Parameters;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Interpolator;
import com.android.camera.Camera;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraManager;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.aosp_porting.FeatureParser;
import com.android.camera.aosp_porting.animation.CubicEaseOutInterpolator;
import com.android.camera.preferences.CameraSettingPreferences;
import java.util.Locale;

public class FocusView extends View implements FocusIndicator, V6FunctionUI, Rotatable {
    public static final int BIG_INIT_RADIUS = Util.dpToPixel(55.0f);
    private static final int BIG_LINE_WIDTH = Util.dpToPixel(1.0f);
    private static final int BIG_MAX_RADIUS = Util.dpToPixel(80.0f);
    public static final int BIG_RADIUS = Util.dpToPixel(43.34f);
    private static final float GAP_NUM = FeatureParser.getFloat("camera_exposure_compensation_steps_num", 0.0f).floatValue();
    private static final int MARGIN = Util.dpToPixel(12.0f);
    private static final int MAX_SLIDE_DISTANCE = ((int) (((double) Util.sWindowWidth) * 0.4d));
    private static final int SMALL_LINE_WIDTH = Util.dpToPixel(1.5f);
    private static final int SMALL_MAX_RADIUS = Util.dpToPixel(7.0f);
    private static final int SMALL_MIN_RADIUS = Util.dpToPixel(3.0f);
    private static final int SMALL_RADIUS = Util.dpToPixel(6.0f);
    private static final int TRIANGLE_BASE_DIS = Util.dpToPixel(3.0f);
    private static final int TRIANGLE_BASE_HEIGHT = Util.dpToPixel(5.0f);
    private static final int TRIANGLE_BASE_LEN = Util.dpToPixel(8.0f);
    private static final int TRIANGLE_MAX_DIS = Util.dpToPixel(30.0f);
    private static final int TRIANGLE_MIN_MARGIN = Util.dpToPixel(25.0f);
    private Camera mActivity;
    private RollAdapter mAdapter;
    private long mAdjustedDoneTime;
    private int mBigAlpha = 150;
    private Paint mBigPaint;
    private int mBigRadius = BIG_RADIUS;
    private int mBottomRelative;
    private Drawable mCaptureBitmap;
    private Rect mCaptureBitmapBounds = new Rect();
    private boolean mCaptured;
    private int mCenterFlag = 0;
    private int mCenterX = (Util.sWindowWidth / 2);
    private int mCenterY = (Util.sWindowHeight / 2);
    private int mCurrentDistanceY;
    private int mCurrentItem;
    private int mCurrentMinusCircleCenter;
    private float mCurrentMinusCircleRadius;
    private int mCurrentRadius;
    private int mCurrentRayBottom;
    private int mCurrentRayHeight;
    private int mCurrentRayWidth;
    private int mCurrentViewState = 0;
    private int mCursorState = 0;
    private float mEVAnimationRatio;
    private long mEVAnimationStartTime;
    float mEVCaptureRatio = -1.0f;
    private boolean mEvAdjusted;
    private int mEvTextMargin;
    private int mEvTriangleDis = 0;
    private float mEvValue;
    private ExposureViewListener mExposureViewListener;
    private long mFailTime;
    private GestureDetector mGestureDetector;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (FocusView.this.mAdapter != null) {
                long duration;
                float t;
                switch (msg.what) {
                    case 1:
                        FocusView.this.invalidate();
                        duration = SystemClock.uptimeMillis() - FocusView.this.mStartTime;
                        if (duration <= 220) {
                            t = FocusView.this.getInterpolation(((float) duration) / 200.0f);
                            if (FocusView.this.isStableStart()) {
                                FocusView.this.mSmallRadius = FocusView.SMALL_MAX_RADIUS;
                                FocusView.this.mBigRadius = FocusView.BIG_RADIUS;
                                FocusView.this.mBigAlpha = 150;
                                FocusView.this.mEVCaptureRatio = 1.0f;
                            } else {
                                FocusView.this.mBigRadius = (int) (((float) FocusView.BIG_RADIUS) + ((1.0f - t) * ((float) (FocusView.BIG_INIT_RADIUS - FocusView.BIG_RADIUS))));
                                FocusView.this.mBigAlpha = (int) (150.0f * t);
                                FocusView.this.mEVCaptureRatio = -1.0f;
                            }
                            FocusView.this.mCenterFlag = 0;
                            FocusView.this.mEvTriangleDis = 0;
                            FocusView.this.mHandler.sendEmptyMessageDelayed(1, 20);
                            FocusView.this.processParameterIfNeeded(t);
                            break;
                        }
                        return;
                    case 2:
                        FocusView.this.invalidate();
                        if (FocusView.this.mState == 2) {
                            duration = SystemClock.uptimeMillis() - FocusView.this.mSuccessTime;
                            if (duration < 150) {
                                t = FocusView.this.getInterpolation(((float) duration) / 130.0f);
                                if (!FocusView.this.mIsTouchFocus || !FocusView.this.mExposureViewListener.isShowCaptureButton()) {
                                    FocusView.this.mCenterFlag = 0;
                                    FocusView.this.mSmallRadius = (int) (((float) FocusView.SMALL_RADIUS) + (((float) (FocusView.SMALL_MAX_RADIUS - FocusView.SMALL_RADIUS)) * t));
                                } else if (t <= 0.5f) {
                                    FocusView.this.mSmallRadius = (int) (((float) FocusView.SMALL_RADIUS) - (((float) (FocusView.SMALL_RADIUS - FocusView.SMALL_MIN_RADIUS)) * (t * 2.0f)));
                                    FocusView.this.mEVCaptureRatio = -1.0f;
                                    FocusView.this.mCenterFlag = 0;
                                } else {
                                    FocusView.this.mSmallRadius = 0;
                                    t = (t - 0.5f) * 2.0f;
                                    FocusView.this.mCenterFlag = 1;
                                    FocusView.this.mEVCaptureRatio = (0.6f * t) + 0.4f;
                                }
                                FocusView.this.mBigRadius = FocusView.BIG_RADIUS;
                                FocusView.this.mBigAlpha = 150;
                                FocusView.this.mEvTriangleDis = 0;
                                FocusView.this.mHandler.sendEmptyMessageDelayed(2, 20);
                                FocusView.this.processParameterIfNeeded(0.0f);
                                break;
                            }
                            return;
                        }
                        break;
                    case 3:
                        FocusView.this.invalidate();
                        if (FocusView.this.mState == 3) {
                            duration = SystemClock.uptimeMillis() - FocusView.this.mFailTime;
                            if (duration < 320) {
                                t = FocusView.this.getInterpolation(((float) duration) / 300.0f);
                                FocusView.this.mSmallAlpha = (int) ((1.0f - t) * 180.0f);
                                FocusView.this.mSmallLineWidth = (int) (((float) FocusView.SMALL_LINE_WIDTH) + ((((float) FocusView.SMALL_LINE_WIDTH) * t) / 2.0f));
                                FocusView.this.mBigRadius = (int) (((float) FocusView.BIG_RADIUS) + (((float) (FocusView.BIG_MAX_RADIUS - FocusView.BIG_RADIUS)) * t));
                                FocusView.this.mBigAlpha = (int) ((1.0f - t) * 150.0f);
                                FocusView.this.mEvTriangleDis = 0;
                                FocusView.this.mEVCaptureRatio = -1.0f;
                                FocusView.this.mCenterFlag = 0;
                                FocusView.this.mHandler.sendEmptyMessageDelayed(3, 20);
                                FocusView.this.processParameterIfNeeded(0.0f);
                                break;
                            }
                            return;
                        }
                        break;
                    case 4:
                    case 5:
                        if (!FocusView.this.mIsDraw || !FocusView.this.mIsDown) {
                            FocusView.this.reset();
                            break;
                        }
                        FocusView.this.clearMessages();
                        sendEmptyMessageDelayed(5, 50);
                        break;
                    case 6:
                        FocusView.this.mCurrentViewState = 0;
                        FocusView.this.mAdjustedDoneTime = System.currentTimeMillis();
                        FocusView.this.calculateAttribute();
                        FocusView.this.invalidate();
                        break;
                    case 7:
                        duration = SystemClock.uptimeMillis() - FocusView.this.mEVAnimationStartTime;
                        if (duration < 520) {
                            FocusView.this.mEVAnimationRatio = ((float) duration) / 500.0f;
                            FocusView.this.calculateAttribute();
                            FocusView.this.invalidate();
                            sendEmptyMessageDelayed(7, 20);
                            break;
                        }
                        FocusView.this.mCurrentViewState = 1;
                        FocusView.this.mCursorState = 0;
                        if (!hasMessages(8)) {
                            removeMessages(6);
                            sendEmptyMessageDelayed(6, 500);
                        }
                        return;
                    case 8:
                        duration = SystemClock.uptimeMillis() - FocusView.this.mSlideStartTime;
                        if (duration < 320) {
                            FocusView.this.mCurrentDistanceY = (int) (((float) FocusView.this.mSlideDistance) * (1.0f - FocusView.this.getInterpolation(((float) duration) / 300.0f)));
                            FocusView.this.invalidate();
                            sendEmptyMessageDelayed(8, 20);
                            break;
                        }
                        FocusView.this.mCursorState = 0;
                        if (!hasMessages(7)) {
                            removeMessages(6);
                            sendEmptyMessageDelayed(6, 500);
                        }
                        return;
                }
            }
        }
    };
    private int mHeight = Util.sWindowHeight;
    private Paint mIndicatorPaint;
    private Interpolator mInterpolator;
    private boolean mIsDown;
    private boolean mIsDraw;
    private boolean mIsTouchFocus;
    private int mLastItem;
    private MessageDispacher mMessageDispacher;
    private Paint mMinusMoonPaint;
    private int[] mRelativeLocation;
    private int mRotation;
    private SimpleOnGestureListener mSimpleOnGestureListener = new SimpleOnGestureListener() {
        public boolean onDown(MotionEvent e) {
            if (!FocusView.this.mIsDraw) {
                return false;
            }
            if (FocusView.this.mCurrentViewState == 0 && V6ModulePicker.isCameraModule() && FocusView.this.isInCircle(e.getX() - ((float) FocusView.this.mRelativeLocation[0]), e.getY() - ((float) FocusView.this.mRelativeLocation[1]), ((float) FocusView.this.mBigRadius) * 0.4f)) {
                if (!(FocusView.this.mMessageDispacher == null || FocusView.this.mAdapter == null)) {
                    FocusView.this.mMessageDispacher.dispacherMessage(2, R.id.v6_focus_view, 3, null, null);
                }
                CameraDataAnalytics.instance().trackEvent("capture_times_focus_view");
                CameraDataAnalytics.instance().trackEvent("touch_focus_focus_view_capture_times_key");
                FocusView.this.mCaptured = true;
            } else {
                FocusView.this.mIsDown = true;
                FocusView.this.removeMessages();
                FocusView.this.setTouchDown();
            }
            return true;
        }

        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!FocusView.this.mIsDown) {
                return false;
            }
            int getstureOri = V6GestureRecognizer.getInstance(FocusView.this.mActivity).getGestureOrientation();
            if ((getstureOri != 200 || (FocusView.this.mRotation != 0 && FocusView.this.mRotation != 180)) && (getstureOri != 100 || (FocusView.this.mRotation != 90 && FocusView.this.mRotation != 270))) {
                return false;
            }
            FocusView focusView;
            switch (FocusView.this.mRotation) {
                case 0:
                    focusView = FocusView.this;
                    focusView.mCurrentDistanceY = (int) (((float) focusView.mCurrentDistanceY) - distanceY);
                    break;
                case 90:
                    focusView = FocusView.this;
                    focusView.mCurrentDistanceY = (int) (((float) focusView.mCurrentDistanceY) - distanceX);
                    break;
                case 180:
                    focusView = FocusView.this;
                    focusView.mCurrentDistanceY = (int) (((float) focusView.mCurrentDistanceY) + distanceY);
                    break;
                case 270:
                    focusView = FocusView.this;
                    focusView.mCurrentDistanceY = (int) (((float) focusView.mCurrentDistanceY) + distanceX);
                    break;
            }
            int targetItem = FocusView.this.getItemByCoordinate();
            if (targetItem != FocusView.this.mCurrentItem) {
                if (FocusView.this.mCurrentViewState != 3 && targetItem < FocusView.this.mCurrentItem && FocusView.this.mCurrentItem >= FocusView.this.mAdapter.getCenterIndex() && targetItem < FocusView.this.mAdapter.getCenterIndex()) {
                    FocusView.this.startAnimation();
                    FocusView.this.mLastItem = FocusView.this.mCurrentItem;
                    FocusView.this.mCurrentViewState = 3;
                } else if (FocusView.this.mCurrentViewState != 4 && targetItem > FocusView.this.mCurrentItem && FocusView.this.mCurrentItem < FocusView.this.mAdapter.getCenterIndex() && targetItem >= FocusView.this.mAdapter.getCenterIndex()) {
                    FocusView.this.startAnimation();
                    FocusView.this.mLastItem = FocusView.this.mCurrentItem;
                    FocusView.this.mCurrentViewState = 4;
                }
                FocusView.this.setCurrentItem(targetItem, false);
            }
            if (FocusView.this.mCurrentViewState == 0 || FocusView.this.mCurrentViewState == 1) {
                FocusView.this.mCurrentViewState = 1;
                FocusView.this.calculateAttribute();
                FocusView.this.invalidate();
                FocusView.this.mHandler.removeMessages(6);
            }
            return true;
        }
    };
    private int mSlideDistance;
    private long mSlideStartTime;
    private int mSmallAlpha = 180;
    private int mSmallLineWidth = SMALL_LINE_WIDTH;
    private Paint mSmallPaint;
    private int mSmallRadius = SMALL_RADIUS;
    private long mStartTime;
    private int mState;
    private long mSuccessTime;
    private Paint mTextPaint;
    private int mWidth = Util.sWindowWidth;

    public interface ExposureViewListener {
        boolean isMeteringAreaOnly();

        boolean isShowCaptureButton();
    }

    public FocusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mActivity = (Camera) context;
        this.mBigPaint = new Paint();
        this.mBigPaint.setAntiAlias(true);
        this.mBigPaint.setStrokeWidth((float) BIG_LINE_WIDTH);
        this.mBigPaint.setStyle(Style.STROKE);
        this.mBigPaint.setAlpha(this.mBigAlpha);
        this.mSmallPaint = new Paint();
        this.mSmallPaint.setAntiAlias(true);
        this.mSmallPaint.setStyle(Style.STROKE);
        this.mSmallPaint.setStrokeWidth((float) SMALL_LINE_WIDTH);
        this.mSmallPaint.setAlpha(this.mSmallAlpha);
        this.mInterpolator = new CubicEaseOutInterpolator();
        TypedArray textStyle = context.obtainStyledAttributes(R.style.SettingStatusBarText, new int[]{16842901, 16842904});
        this.mTextPaint = new Paint();
        this.mTextPaint.setColor(textStyle.getColor(textStyle.getIndex(1), -1));
        this.mTextPaint.setStyle(Style.FILL);
        this.mTextPaint.setTextSize((float) textStyle.getDimensionPixelSize(textStyle.getIndex(0), 0));
        this.mTextPaint.setTextAlign(Align.LEFT);
        this.mTextPaint.setAntiAlias(true);
        this.mTextPaint.setAlpha(192);
        this.mIndicatorPaint = new Paint();
        this.mIndicatorPaint.setColor(-1);
        this.mIndicatorPaint.setStyle(Style.FILL);
        this.mIndicatorPaint.setAntiAlias(true);
        this.mMinusMoonPaint = new Paint();
        this.mMinusMoonPaint.setColor(-1);
        this.mMinusMoonPaint.setStyle(Style.FILL);
        this.mMinusMoonPaint.setAntiAlias(true);
        this.mMinusMoonPaint.setXfermode(new PorterDuffXfermode(Mode.DST_OUT));
        this.mGestureDetector = new GestureDetector(context, this.mSimpleOnGestureListener);
        this.mGestureDetector.setIsLongpressEnabled(false);
        this.mEvTextMargin = context.getResources().getDimensionPixelSize(R.dimen.focus_view_ev_text_margin);
        this.mWidth = Util.sWindowWidth;
        this.mHeight = Util.sWindowHeight;
        this.mCenterX = this.mWidth / 2;
        this.mCenterY = this.mHeight / 2;
        this.mCaptureBitmap = getResources().getDrawable(R.drawable.bg_capture);
        this.mCaptureBitmap.setFilterBitmap(true);
        this.mCaptureBitmapBounds.set((-this.mCaptureBitmap.getIntrinsicWidth()) / 2, (-this.mCaptureBitmap.getIntrinsicHeight()) / 2, this.mCaptureBitmap.getIntrinsicWidth() / 2, this.mCaptureBitmap.getIntrinsicHeight() / 2);
    }

    public void initialize(ExposureViewListener exposureViewListener) {
        this.mExposureViewListener = exposureViewListener;
        clear();
    }

    public boolean onViewTouchEvent(MotionEvent ev) {
        boolean z = true;
        if (this.mAdapter == null || !this.mIsTouchFocus || (this.mState != 2 && !isStableStart())) {
            return false;
        }
        this.mGestureDetector.onTouchEvent(ev);
        boolean oldDown = this.mIsDown;
        if (ev.getActionMasked() == 5 && this.mIsDown) {
            this.mIsDown = false;
            performSlideBack();
        }
        if (1 == ev.getAction() || 3 == ev.getAction()) {
            if (this.mEvAdjusted) {
                CameraDataAnalytics.instance().trackEvent("pref_camera_exposure_key");
                stopEvAdjust();
            }
            if (this.mCaptured) {
                this.mCaptured = false;
            }
            if (this.mIsDraw) {
                this.mIsDown = false;
                performSlideBack();
            }
        }
        if (!oldDown) {
            z = this.mIsDown;
        }
        return z;
    }

    private void processParameterIfNeeded(float ratio) {
        if (this.mIsTouchFocus && this.mEVCaptureRatio != -1.0f && this.mCenterFlag == 0) {
            this.mCenterFlag = 1;
        }
    }

    public void onCreate() {
    }

    public void onCameraOpen() {
        Parameters parameter = CameraManager.instance().getStashParameters();
        if (parameter != null) {
            int min = parameter.getMinExposureCompensation();
            int max = parameter.getMaxExposureCompensation();
            if (max != 0 && max != min) {
                this.mAdapter = new FloatSlideAdapter(min, max, GAP_NUM == 0.0f ? 1.0f : ((float) (max - min)) / GAP_NUM);
                if (this.mAdapter != null) {
                    this.mCurrentItem = 0;
                    int index = this.mAdapter.getItemIndexByValue(Integer.valueOf(CameraSettings.readExposure(CameraSettingPreferences.instance())));
                    if (index < 0) {
                        this.mCurrentItem = this.mAdapter.getMaxItem() / 2;
                    } else {
                        this.mCurrentItem = index;
                    }
                    updateEV();
                }
            }
        }
    }

    public void onResume() {
        setDraw(false);
        this.mActivity.loadCameraSound(6);
    }

    public void onPause() {
        setDraw(false);
        CameraSettings.resetExposure();
    }

    public void enableControls(boolean enabled) {
    }

    public void setMessageDispacher(MessageDispacher p) {
        this.mMessageDispacher = p;
    }

    public void setOrientation(int orientation, boolean animation) {
        if (this.mRotation != orientation) {
            this.mRotation = orientation;
            if (this.mIsDraw) {
                invalidate();
            }
        }
    }

    public void setFocusType(boolean isTouchFocus) {
        this.mIsTouchFocus = isTouchFocus;
    }

    public boolean isVisible() {
        return this.mIsDraw;
    }

    public void showStart() {
        clearMessages();
        this.mState = 1;
        this.mCursorState = 1;
        this.mSmallRadius = SMALL_RADIUS;
        this.mSmallAlpha = 180;
        this.mSmallLineWidth = SMALL_LINE_WIDTH;
        this.mStartTime = SystemClock.uptimeMillis();
        setDraw(true);
        if (isStableStart()) {
            this.mCenterFlag = 1;
            this.mHandler.sendEmptyMessage(1);
        } else {
            this.mBigRadius = BIG_INIT_RADIUS;
            this.mBigAlpha = 0;
            this.mEvTriangleDis = 0;
            this.mEVCaptureRatio = -1.0f;
            this.mCenterFlag = 0;
            processParameterIfNeeded(0.0f);
            this.mHandler.sendEmptyMessage(1);
            this.mHandler.sendEmptyMessageDelayed(4, 3000);
        }
        invalidate();
    }

    public void showSuccess() {
        if (this.mState == 1) {
            boolean animating = this.mHandler.hasMessages(1);
            clearMessages();
            setDraw(true);
            this.mState = 2;
            if (animating) {
                this.mSuccessTime = SystemClock.uptimeMillis();
                this.mHandler.sendEmptyMessageDelayed(2, 50);
            } else {
                this.mSuccessTime = SystemClock.uptimeMillis();
                this.mHandler.sendEmptyMessageDelayed(2, 50);
            }
            if (!this.mIsTouchFocus) {
                this.mHandler.sendEmptyMessageDelayed(5, 800);
            }
            invalidate();
        }
    }

    public void showFail() {
        if (this.mState == 1) {
            boolean animating = this.mHandler.hasMessages(1);
            clearMessages();
            setDraw(true);
            this.mState = 3;
            if (animating) {
                this.mFailTime = SystemClock.uptimeMillis();
                this.mHandler.sendEmptyMessageDelayed(3, 50);
                this.mHandler.sendEmptyMessageDelayed(5, 800);
                invalidate();
            } else {
                this.mFailTime = SystemClock.uptimeMillis();
                this.mHandler.sendEmptyMessageDelayed(3, 50);
                this.mHandler.sendEmptyMessageDelayed(5, 800);
                invalidate();
            }
        }
    }

    public void clear() {
        if (this.mIsDraw) {
            reset();
            invalidate();
        }
    }

    private void reset() {
        clearMessages();
        this.mState = 0;
        setPosition(this.mWidth / 2, this.mHeight / 2);
        this.mCurrentDistanceY = 0;
        this.mCurrentViewState = 0;
        this.mCenterFlag = 0;
        this.mIsDown = false;
        stopEvAdjust();
        setDraw(false);
        invalidate();
    }

    public void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            this.mWidth = right - left;
            this.mHeight = bottom - left;
            this.mCenterX = this.mWidth / 2;
            this.mCenterY = this.mHeight / 2;
        }
        this.mRelativeLocation = Util.getRelativeLocation(this.mActivity.getUIController().getGLView(), this);
    }

    protected void onDraw(Canvas canvas) {
        if (this.mIsDraw) {
            if (this.mRotation != 0) {
                canvas.save();
                canvas.translate((float) this.mCenterX, (float) this.mCenterY);
                canvas.rotate((float) (-this.mRotation));
                canvas.translate((float) (-this.mCenterX), (float) (-this.mCenterY));
            }
            this.mBigPaint.setColor(Color.argb(this.mBigAlpha, 255, 255, 255));
            canvas.drawCircle((float) this.mCenterX, (float) this.mCenterY, (float) this.mBigRadius, this.mBigPaint);
            drawCenterIndicator(canvas);
            drawCursor(canvas);
            if (CameraSettings.isSupportedPortrait()) {
                drawEvText(canvas);
            }
            if (this.mRotation != 0) {
                canvas.restore();
            }
        }
    }

    private void updateEV() {
        Parameters parameters = CameraManager.instance().getStashParameters();
        if (this.mAdapter == null || parameters == null) {
            this.mEvValue = 0.0f;
        } else {
            this.mEvValue = ((float) this.mAdapter.getItemValue(this.mCurrentItem)) * parameters.getExposureCompensationStep();
        }
    }

    private void drawEvText(Canvas canvas) {
        if (this.mIsTouchFocus && ((double) Math.abs(this.mEvValue)) > 0.05d && this.mCenterFlag != 1 && this.mCenterFlag != 0) {
            String symbol = this.mEvValue < 0.0f ? "-" : "+";
            String text = String.format(Locale.ENGLISH, "%s %.1f", new Object[]{symbol, Float.valueOf(Math.abs(this.mEvValue))});
            canvas.drawText(text, (((float) this.mCenterX) - this.mTextPaint.measureText(text.split("\\.")[0])) - (this.mTextPaint.measureText(".") / 2.0f), (float) ((this.mCenterY - BIG_RADIUS) - this.mEvTextMargin), this.mTextPaint);
        }
    }

    private void drawCursor(Canvas canvas) {
        boolean rtl = true;
        if (this.mIsTouchFocus && this.mAdapter != null) {
            int leftx;
            Path path = new Path();
            if (1 != getLayoutDirection()) {
                rtl = false;
            }
            if ((rtl || ((Util.sWindowWidth - this.mCenterX) - BIG_RADIUS) - MARGIN >= TRIANGLE_MIN_MARGIN) && (!rtl || (this.mCenterX - BIG_RADIUS) - MARGIN < TRIANGLE_MIN_MARGIN)) {
                leftx = ((this.mCenterX + BIG_RADIUS) + MARGIN) - (TRIANGLE_BASE_LEN / 2);
            } else {
                leftx = ((this.mCenterX - BIG_RADIUS) - MARGIN) - (TRIANGLE_BASE_LEN / 2);
            }
            int lefty = (this.mCenterY + this.mCurrentDistanceY) - ((this.mEvTriangleDis + TRIANGLE_BASE_DIS) / 2);
            path.moveTo((float) leftx, (float) lefty);
            path.lineTo((float) (TRIANGLE_BASE_LEN + leftx), (float) lefty);
            path.lineTo((float) ((TRIANGLE_BASE_LEN / 2) + leftx), (float) (lefty - TRIANGLE_BASE_HEIGHT));
            path.lineTo((float) leftx, (float) lefty);
            lefty = (this.mCenterY + this.mCurrentDistanceY) + ((this.mEvTriangleDis + TRIANGLE_BASE_DIS) / 2);
            path.moveTo((float) leftx, (float) lefty);
            path.lineTo((float) (TRIANGLE_BASE_LEN + leftx), (float) lefty);
            path.lineTo((float) ((TRIANGLE_BASE_LEN / 2) + leftx), (float) (TRIANGLE_BASE_HEIGHT + lefty));
            path.lineTo((float) leftx, (float) lefty);
            if (this.mState == 3) {
                this.mIndicatorPaint.setAlpha(this.mBigAlpha);
            } else {
                this.mIndicatorPaint.setAlpha(255);
            }
            canvas.drawPath(path, this.mIndicatorPaint);
        }
    }

    public boolean isEvAdjusted() {
        return !this.mEvAdjusted ? this.mCaptured : true;
    }

    public boolean isEvAdjustedTime() {
        if (!isShown() || !this.mIsTouchFocus) {
            return false;
        }
        if (this.mEvAdjusted) {
            return true;
        }
        if (Util.isTimeout(System.currentTimeMillis(), this.mAdjustedDoneTime, 2000)) {
            return false;
        }
        return true;
    }

    public void setPosition(int x, int y) {
        this.mCenterX = x;
        this.mCenterY = y;
        removeMessages();
    }

    private boolean isStableStart() {
        return this.mIsTouchFocus ? this.mExposureViewListener.isMeteringAreaOnly() : false;
    }

    private float getInterpolation(float t) {
        float interpolation = this.mInterpolator.getInterpolation(t);
        if (((double) interpolation) > 1.0d) {
            return 1.0f;
        }
        return interpolation;
    }

    private void clearMessages() {
        this.mHandler.removeMessages(1);
        this.mHandler.removeMessages(2);
        this.mHandler.removeMessages(3);
        this.mHandler.removeMessages(4);
        this.mHandler.removeMessages(5);
        this.mHandler.removeMessages(8);
        this.mHandler.removeMessages(6);
        this.mHandler.removeMessages(7);
        this.mHandler.removeMessages(8);
    }

    private void reload() {
        if (this.mAdapter != null) {
            this.mCurrentItem = 0;
            this.mCurrentItem = this.mAdapter.getItemIndexByValue(Integer.valueOf(CameraSettings.readExposure(CameraSettingPreferences.instance())));
            updateEV();
        }
    }

    private void setDraw(boolean draw) {
        if (draw && this.mIsTouchFocus && this.mIsDraw != draw) {
            reload();
        }
        this.mIsDraw = draw;
    }

    private void removeMessages() {
        this.mHandler.removeMessages(8);
    }

    private void setTouchDown() {
        this.mBottomRelative = (MAX_SLIDE_DISTANCE * this.mCurrentItem) / this.mAdapter.getMaxItem();
    }

    private int getItemByCoordinate() {
        return Util.clamp((this.mAdapter.getMaxItem() * (this.mBottomRelative - this.mCurrentDistanceY)) / MAX_SLIDE_DISTANCE, 0, this.mAdapter.getMaxItem());
    }

    private void performSlideBack() {
        this.mHandler.removeMessages(6);
        if (this.mCurrentDistanceY != 0) {
            this.mSlideDistance = this.mCurrentDistanceY;
            this.mSlideStartTime = SystemClock.uptimeMillis();
            this.mCursorState = 2;
            this.mHandler.removeMessages(8);
            this.mHandler.sendEmptyMessage(8);
            return;
        }
        this.mHandler.sendEmptyMessage(6);
    }

    private void setCurrentItem(int index, boolean animated) {
        if (index != this.mCurrentItem) {
            this.mCurrentItem = index;
            if (!(this.mMessageDispacher == null || this.mAdapter == null)) {
                this.mEvAdjusted = true;
                this.mMessageDispacher.dispacherMessage(1, R.id.v6_focus_view, 2, Integer.valueOf(this.mAdapter.getItemValue(index)), Integer.valueOf(1));
            }
            updateEV();
        }
    }

    private void stopEvAdjust() {
        if (this.mEvAdjusted) {
            this.mEvAdjusted = false;
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(1, R.id.v6_focus_view, 2, Integer.valueOf(0), Integer.valueOf(2));
            }
        }
    }

    private void startAnimation() {
        this.mEVAnimationStartTime = SystemClock.uptimeMillis();
        this.mHandler.removeMessages(7);
        this.mHandler.removeMessages(6);
        this.mHandler.sendEmptyMessage(7);
    }

    private void drawCenterIndicator(Canvas canvas) {
        canvas.save();
        canvas.translate((float) this.mCenterX, (float) this.mCenterY);
        if (this.mAdapter != null && this.mCenterFlag != 0) {
            switch (this.mCenterFlag) {
                case 1:
                    drawCaptureBitmap(canvas);
                    break;
                case 2:
                    drawSun(canvas);
                    break;
                case 3:
                    canvas.drawCircle(0.0f, 0.0f, (float) this.mCurrentRadius, this.mIndicatorPaint);
                    canvas.drawCircle((float) (-this.mCurrentMinusCircleCenter), (float) (-this.mCurrentMinusCircleCenter), this.mCurrentMinusCircleRadius, this.mMinusMoonPaint);
                    break;
                default:
                    break;
            }
        }
        this.mSmallPaint.setColor(Color.argb(this.mSmallAlpha, 255, 255, 255));
        this.mSmallPaint.setStrokeWidth((float) this.mSmallLineWidth);
        canvas.drawCircle(0.0f, 0.0f, (float) this.mSmallRadius, this.mSmallPaint);
        canvas.translate((float) (-this.mCenterX), (float) (-this.mCenterY));
        canvas.restore();
    }

    private float getItemRatio(int item) {
        float itemRatio = ((float) item) / ((float) this.mAdapter.getMaxItem());
        return itemRatio >= 0.5f ? 2.0f * (itemRatio - 0.5f) : itemRatio * 2.0f;
    }

    private void calculateAttribute() {
        float currentItemRatio = getItemRatio(this.mCurrentItem);
        float lastItemRatio = getItemRatio(this.mLastItem);
        float ratio;
        switch (this.mCurrentViewState) {
            case 0:
                this.mCenterFlag = this.mExposureViewListener.isShowCaptureButton() ? 1 : 0;
                return;
            case 1:
                ratio = currentItemRatio;
                if (this.mCurrentItem < this.mAdapter.getCenterIndex()) {
                    this.mCurrentRadius = Util.dpToPixel((2.0f * currentItemRatio) + 6.0f);
                    this.mCurrentMinusCircleCenter = (int) (((float) this.mCurrentRadius) * 0.5f);
                    this.mCurrentMinusCircleRadius = ((float) this.mCurrentRadius) * 0.8f;
                    this.mCenterFlag = 3;
                    return;
                }
                this.mCurrentRayWidth = Util.dpToPixel(1.5f);
                this.mCurrentRayHeight = Util.dpToPixel((2.0f * currentItemRatio) + 5.0f);
                this.mCurrentRayBottom = Util.dpToPixel((3.0f * currentItemRatio) + 7.5f);
                this.mCurrentRadius = Util.dpToPixel((2.0f * currentItemRatio) + 5.0f);
                this.mCenterFlag = 2;
                return;
            case 3:
                if (this.mEVAnimationRatio <= 0.5f) {
                    ratio = 2.0f * this.mEVAnimationRatio;
                    this.mCurrentRayWidth = Util.dpToPixel(1.5f);
                    this.mCurrentRayHeight = Util.dpToPixel(((((1.0f - ratio) * lastItemRatio) - ratio) * 2.0f) + 5.0f);
                    this.mCurrentRayBottom = Util.dpToPixel(((((1.0f - ratio) * lastItemRatio) - ratio) * 3.0f) + 7.5f);
                    this.mCurrentRadius = Util.dpToPixel(((3.0f * ratio) + 5.0f) + ((2.0f * lastItemRatio) * (1.0f - ratio)));
                    this.mCenterFlag = 2;
                    return;
                }
                ratio = 2.0f * (this.mEVAnimationRatio - 0.5f);
                this.mCurrentRadius = Util.dpToPixel(8.0f - (((1.0f - currentItemRatio) * ratio) * 2.0f));
                this.mCurrentMinusCircleCenter = (int) (((float) this.mCurrentRadius) * (((1.0f - ratio) * 0.914f) + 0.5f));
                this.mCurrentMinusCircleRadius = ((float) this.mCurrentRadius) * (((1.0f - ratio) * 0.2f) + 0.8f);
                this.mCenterFlag = 3;
                return;
            case 4:
                if (this.mEVAnimationRatio < 0.5f) {
                    ratio = 2.0f * this.mEVAnimationRatio;
                    this.mCurrentRadius = Util.dpToPixel(((((1.0f - lastItemRatio) * ratio) + lastItemRatio) * 2.0f) + 6.0f);
                    this.mCurrentMinusCircleCenter = (int) (((float) this.mCurrentRadius) * ((0.914f * ratio) + 0.5f));
                    this.mCurrentMinusCircleRadius = ((float) this.mCurrentRadius) * ((0.2f * ratio) + 0.8f);
                    this.mCenterFlag = 3;
                    return;
                }
                ratio = 2.0f * (this.mEVAnimationRatio - 0.5f);
                this.mCurrentRayWidth = Util.dpToPixel(1.5f);
                this.mCurrentRayHeight = Util.dpToPixel((((currentItemRatio * ratio) - (1.0f - ratio)) * 2.0f) + 5.0f);
                this.mCurrentRayBottom = Util.dpToPixel((((currentItemRatio * ratio) - (1.0f - ratio)) * 3.0f) + 7.5f);
                this.mCurrentRadius = Util.dpToPixel((((1.0f - ratio) * 3.0f) + 5.0f) + ((2.0f * currentItemRatio) * ratio));
                this.mCenterFlag = 2;
                return;
            default:
                return;
        }
    }

    private int getCurrentAngle() {
        int degree = 0;
        if (this.mCursorState == 2 && this.mCurrentViewState != 3 && this.mCurrentViewState != 4) {
            if (this.mCurrentItem >= this.mAdapter.getCenterIndex()) {
                degree = ((this.mCurrentItem - this.mAdapter.getCenterIndex()) * 360) / this.mAdapter.getCenterIndex();
            }
            return 360 - Util.clamp(degree, 0, 360);
        } else if (this.mCurrentViewState == 1) {
            int relativeDis = Util.clamp(this.mBottomRelative - this.mCurrentDistanceY, 0, MAX_SLIDE_DISTANCE);
            if (relativeDis >= MAX_SLIDE_DISTANCE / 2) {
                degree = ((relativeDis - (MAX_SLIDE_DISTANCE / 2)) * 360) / (MAX_SLIDE_DISTANCE / 2);
            }
            return 360 - Util.clamp(degree, 0, 360);
        } else if (this.mCurrentViewState == 3) {
            return Util.clamp((int) ((this.mEVAnimationRatio * 2.0f) * 135.0f), 0, 135);
        } else {
            if (this.mCurrentViewState == 4) {
                return Util.clamp((int) ((1.0f - ((this.mEVAnimationRatio - 0.5f) * 2.0f)) * 135.0f), 0, 135);
            }
            return 0;
        }
    }

    private void drawCaptureBitmap(Canvas canvas) {
        if (this.mExposureViewListener.isShowCaptureButton()) {
            this.mCaptureBitmap.setBounds((int) (((float) this.mCaptureBitmapBounds.left) * this.mEVCaptureRatio), (int) (((float) this.mCaptureBitmapBounds.top) * this.mEVCaptureRatio), (int) (((float) this.mCaptureBitmapBounds.right) * this.mEVCaptureRatio), (int) (((float) this.mCaptureBitmapBounds.bottom) * this.mEVCaptureRatio));
            this.mCaptureBitmap.draw(canvas);
        }
    }

    private void drawSun(Canvas canvas) {
        canvas.rotate((float) getCurrentAngle());
        for (int i = 0; i < 2; i++) {
            if (i > 0) {
                canvas.rotate(45.0f);
            }
            canvas.drawRect((float) ((-this.mCurrentRayWidth) / 2), (float) ((-this.mCurrentRayBottom) - this.mCurrentRayHeight), (float) (this.mCurrentRayWidth / 2), (float) (-this.mCurrentRayBottom), this.mIndicatorPaint);
            canvas.drawRect((float) ((-this.mCurrentRayWidth) / 2), (float) this.mCurrentRayBottom, (float) (this.mCurrentRayWidth / 2), (float) (this.mCurrentRayBottom + this.mCurrentRayHeight), this.mIndicatorPaint);
            canvas.drawRect((float) ((-this.mCurrentRayBottom) - this.mCurrentRayHeight), (float) ((-this.mCurrentRayWidth) / 2), (float) (-this.mCurrentRayBottom), (float) (this.mCurrentRayWidth / 2), this.mIndicatorPaint);
            canvas.drawRect((float) this.mCurrentRayBottom, (float) ((-this.mCurrentRayWidth) / 2), (float) (this.mCurrentRayBottom + this.mCurrentRayHeight), (float) (this.mCurrentRayWidth / 2), this.mIndicatorPaint);
        }
        canvas.drawCircle(0.0f, 0.0f, (float) this.mCurrentRadius, this.mIndicatorPaint);
    }

    private boolean isInCircle(float x, float y, float r) {
        float dx = x - ((float) this.mCenterX);
        float dy = y - ((float) this.mCenterY);
        return Math.sqrt((double) ((dx * dx) + (dy * dy))) <= ((double) r);
    }
}
