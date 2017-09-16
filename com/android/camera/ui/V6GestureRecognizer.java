package com.android.camera.ui;

import android.graphics.Point;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.view.ViewConfiguration;
import com.android.camera.ActivityBase;
import com.android.camera.Camera;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.Log;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.ui.EdgeGestureDetector.EdgeGestureListener;

public class V6GestureRecognizer {
    public static final int GESTURE_DETECT_DISTANCE = Util.dpToPixel(50.0f);
    public static final int SWITCH_CAMERA_IGNORE_DISTANCE = Util.dpToPixel(30.0f);
    private static V6GestureRecognizer sV6GestureRecognizer;
    private final Camera mActivity;
    private final CameraGestureDetector mCameraGestureDetector;
    private int mEdgeGesture = 0;
    private final EdgeGestureDetector mEdgeGestureDetector;
    private int mGesture = 0;
    private final GestureDetector mGestureDetector;
    private final ScaleGestureDetector mScaleDetector;
    private boolean mTouchDown;

    private class CameraGestureDetector {
        private final int MIN_DETECT_DISTANCE = (ViewConfiguration.get(V6GestureRecognizer.this.mActivity).getScaledTouchSlop() * ViewConfiguration.get(V6GestureRecognizer.this.mActivity).getScaledTouchSlop());
        private Point mStartPoint = new Point();

        public void onTouchEvent(MotionEvent ev) {
            switch (ev.getAction() & 255) {
                case 0:
                    this.mStartPoint.set((int) ev.getX(), (int) ev.getY());
                    return;
                case 2:
                    Log.v("Camera12", "CameraGestureDetector ACTION_MOVE mGesture=" + V6GestureRecognizer.this.mGesture);
                    if (V6GestureRecognizer.this.mGesture / 100 == 0) {
                        Point point = V6GestureRecognizer.this.getMoveVector(this.mStartPoint.x, this.mStartPoint.y, (int) ev.getX(), (int) ev.getY());
                        Log.v("CameraGestureRecognizer", "mGesture=" + V6GestureRecognizer.this.mGesture + " orientation=" + (Math.abs(point.x) > Math.abs(point.y) ? "h" : "v") + " dx=" + point.x + " dy=" + point.y);
                        if (this.MIN_DETECT_DISTANCE <= (point.x * point.x) + (point.y * point.y)) {
                            V6GestureRecognizer v6GestureRecognizer = V6GestureRecognizer.this;
                            v6GestureRecognizer.mGesture = (Math.abs(point.x) > Math.abs(point.y) ? 100 : 200) + v6GestureRecognizer.mGesture;
                        }
                    }
                    Log.v("Camera12", "CameraGestureDetector ACTION_MOVE end mGesture=" + V6GestureRecognizer.this.mGesture);
                    return;
                case 6:
                    if (ev.getPointerCount() == 2 && V6GestureRecognizer.this.couldNotifyGesture(false) && V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                        float l;
                        float r;
                        float t;
                        float b;
                        if (ev.getX(0) < ev.getX(1)) {
                            l = ev.getX(0);
                            r = ev.getX(1);
                        } else {
                            l = ev.getX(1);
                            r = ev.getX(0);
                        }
                        if (ev.getY(0) < ev.getY(1)) {
                            t = ev.getY(0);
                            b = ev.getY(1);
                        } else {
                            t = ev.getY(1);
                            b = ev.getY(0);
                        }
                        if (V6GestureRecognizer.this.couldNotifyGesture(false) && V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                            V6GestureRecognizer v6GestureRecognizer2 = V6GestureRecognizer.this;
                            v6GestureRecognizer2.mGesture = v6GestureRecognizer2.mGesture + 10;
                            V6GestureRecognizer.this.mActivity.getCurrentModule().onGestureTrack(new RectF(l, t, r, b), true);
                            return;
                        }
                        return;
                    }
                    return;
                default:
                    return;
            }
        }
    }

    private class MyEdgeGestureListener implements EdgeGestureListener {
        private MyEdgeGestureListener() {
        }
    }

    private class MyGestureListener extends SimpleOnGestureListener {
        private boolean mHandleConfirmTap;

        private MyGestureListener() {
        }

        private boolean handleSingleTap(MotionEvent e) {
            if (!V6GestureRecognizer.this.couldNotifyGesture(false) || !V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                return false;
            }
            V6GestureRecognizer.this.getUIController().getTopPopupParent().dismissAllPopupExceptSkinBeauty(true);
            V6GestureRecognizer.this.mActivity.getCurrentModule().onSingleTapUp((int) e.getX(), (int) e.getY());
            return true;
        }

        public boolean onSingleTapUp(MotionEvent e) {
            Log.v("CameraGestureRecognizer", "onSingleTapUp");
            if (!V6GestureRecognizer.this.getUIController().getEffectCropView().isVisible()) {
                return handleSingleTap(e);
            }
            boolean isPreviewPageVisible;
            if (V6GestureRecognizer.this.couldNotifyGesture(false)) {
                isPreviewPageVisible = V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible();
            } else {
                isPreviewPageVisible = false;
            }
            this.mHandleConfirmTap = isPreviewPageVisible;
            return false;
        }

        public boolean onDoubleTap(MotionEvent e) {
            int i = 0;
            if (!this.mHandleConfirmTap) {
                return false;
            }
            V6GestureRecognizer.this.getUIController().getTopPopupParent().dismissAllPopupExceptSkinBeauty(true);
            int invert = EffectController.getInstance().getInvertFlag();
            EffectController instance = EffectController.getInstance();
            if (invert == 0) {
                i = 1;
            }
            instance.setInvertFlag(i);
            return true;
        }

        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (V6GestureRecognizer.this.getUIController().getEffectCropView().isVisible() && this.mHandleConfirmTap) {
                return handleSingleTap(e);
            }
            return false;
        }

        public void onLongPress(MotionEvent e) {
            Log.v("CameraGestureRecognizer", "onLongPress");
            if (V6GestureRecognizer.this.couldNotifyGesture(false) && V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                V6GestureRecognizer.this.getUIController().getTopPopupParent().dismissAllPopupExceptSkinBeauty(true);
                V6GestureRecognizer.this.mActivity.getCurrentModule().onLongPress((int) e.getX(), (int) e.getY());
            }
        }
    }

    private class MyScaleListener extends SimpleOnScaleGestureListener {
        private MyScaleListener() {
        }

        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return V6GestureRecognizer.this.mActivity.getCurrentModule().onScaleBegin(detector.getFocusX(), detector.getFocusY());
        }

        public boolean onScale(ScaleGestureDetector detector) {
            if ((!V6GestureRecognizer.this.isGestureDetecting() && V6GestureRecognizer.this.getCurrentGesture() != 9) || !V6GestureRecognizer.this.getUIController().getPreviewPage().isPreviewPageVisible()) {
                return false;
            }
            CameraDataAnalytics.instance().trackEvent("zoom_gesture_times");
            V6GestureRecognizer.this.setGesture(9);
            return V6GestureRecognizer.this.mActivity.getCurrentModule().onScale(detector.getFocusX(), detector.getFocusY(), detector.getScaleFactor());
        }

        public void onScaleEnd(ScaleGestureDetector detector) {
            V6GestureRecognizer.this.mActivity.getCurrentModule().onScaleEnd();
        }
    }

    private V6GestureRecognizer(ActivityBase activity) {
        this.mActivity = (Camera) activity;
        this.mGestureDetector = new GestureDetector(activity, new MyGestureListener(), null, true);
        this.mEdgeGestureDetector = new EdgeGestureDetector(new MyEdgeGestureListener());
        this.mScaleDetector = new ScaleGestureDetector(activity, new MyScaleListener());
        this.mCameraGestureDetector = new CameraGestureDetector();
    }

    public static synchronized V6GestureRecognizer getInstance(ActivityBase activity) {
        V6GestureRecognizer v6GestureRecognizer;
        synchronized (V6GestureRecognizer.class) {
            if (sV6GestureRecognizer == null || activity != sV6GestureRecognizer.mActivity) {
                sV6GestureRecognizer = new V6GestureRecognizer(activity);
            }
            v6GestureRecognizer = sV6GestureRecognizer;
        }
        return v6GestureRecognizer;
    }

    private UIController getUIController() {
        return this.mActivity.getUIController();
    }

    public boolean onTouchEvent(MotionEvent e) {
        Log.v("CameraGestureRecognizer", "onTouchEvent mGesture=" + this.mGesture + " action=" + e.getAction());
        if (e.getActionMasked() == 0) {
            this.mGesture = 0;
        }
        if (this.mActivity.getCurrentModule().IsIgnoreTouchEvent() && e.getAction() != 1 && e.getAction() != 3) {
            return false;
        }
        if (e.getActionMasked() == 0) {
            this.mTouchDown = true;
        } else if (!this.mTouchDown) {
            return false;
        } else {
            if (e.getActionMasked() == 3 || e.getActionMasked() == 1) {
                this.mTouchDown = false;
            }
        }
        checkControlView(e);
        Log.v("CameraGestureRecognizer", "set to detector");
        this.mCameraGestureDetector.onTouchEvent(e);
        this.mGestureDetector.onTouchEvent(e);
        this.mScaleDetector.onTouchEvent(e);
        boolean result = !isGestureDetecting();
        if (e.getAction() == 1 || e.getAction() == 3) {
            this.mGesture = 0;
        }
        return result;
    }

    private boolean checkControlView(MotionEvent e) {
        if (getUIController().getPreviewPage().isPreviewPageVisible()) {
            V6EffectCropView cropVew = getUIController().getEffectCropView();
            if (cropVew.isVisible()) {
                cropVew.onViewTouchEvent(e);
                if (cropVew.isMoved()) {
                    if (isGestureDetecting()) {
                        this.mGesture += 6;
                    }
                } else if (!cropVew.isMoved() && getCurrentGesture() == 6) {
                    setGesture(0);
                }
            }
            FocusView focusView = getUIController().getFocusView();
            boolean adjustEv = focusView.isEvAdjusted();
            if (focusView.isVisible()) {
                focusView.onViewTouchEvent(e);
                if (focusView.isEvAdjusted()) {
                    if (isGestureDetecting()) {
                        this.mGesture += 7;
                    }
                } else if (!adjustEv && getCurrentGesture() == 7) {
                    setGesture(0);
                }
            }
        }
        if (isGestureDetecting()) {
            return false;
        }
        return true;
    }

    public void setGesture(int gesture) {
        this.mGesture = ((this.mGesture / 100) * 100) + gesture;
    }

    private Point getMoveVector(int e1x, int e1y, int e2x, int e2y) {
        Point vector = new Point();
        vector.x = e1x - e2x;
        vector.y = e1y - e2y;
        return vector;
    }

    private boolean couldNotifyGesture(boolean isEdge) {
        return isGestureDetecting(isEdge) && !this.mActivity.getCurrentModule().IsIgnoreTouchEvent();
    }

    private boolean isGestureDetecting(boolean isEdge) {
        return (isEdge ? this.mEdgeGesture : this.mGesture) % 100 == 0;
    }

    public boolean isGestureDetecting() {
        return this.mGesture % 100 == 0;
    }

    public int getGestureOrientation() {
        return (this.mGesture / 100) * 100;
    }

    public int getCurrentGesture() {
        return this.mGesture % 100;
    }

    public static void onDestory(ActivityBase activity) {
        if (sV6GestureRecognizer != null && sV6GestureRecognizer.mActivity == activity) {
            sV6GestureRecognizer = null;
        }
    }
}
