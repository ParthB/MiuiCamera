package com.android.zxing;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraAppImpl;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.zxing.ui.QRCodeFragmentLayout;
import com.google.zxing.Result;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.WeakHashMap;

public class QRCodeManager implements OnClickListener {
    private static int CENTER_FRAME_WIDTH;
    private static final int DECODE_TOTAL_TIME = (Device.isLowPowerQRScan() ? 15000 : -1);
    private static int MAX_FRAME_HEIGHT;
    private static int MAX_FRAME_WIDTH;
    private static Rect mRectFinderCenter = new Rect(0, 0, 0, 0);
    private static Rect mRectFinderFocusArea = new Rect(0, 0, 0, 0);
    private static Rect mRectPreviewCenter = new Rect(0, 0, 0, 0);
    private static Rect mRectPreviewFocusArea = new Rect(0, 0, 0, 0);
    private static WeakHashMap<Context, QRCodeManager> sMap = new WeakHashMap();
    private Activity mActivity;
    private CameraProxy mCameraDevice;
    private boolean mDecode;
    private DecodeHandlerFactory mDecodeHandlerFactory;
    private Handler mHandler;
    private QRCodeManagerListener mListener;
    private boolean mNeedScan;
    private PreviewCallback mPreviewCallback = new PreviewCallback() {
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (QRCodeManager.this.mDecodeHandlerFactory != null && data != null) {
                QRCodeManager.this.mDecodeHandlerFactory.getHandler().removeMessages(R.id.decode);
                QRCodeManager.this.mDecodeHandlerFactory.getHandler().obtainMessage(R.id.decode, QRCodeManager.this.mPreviewHeight, QRCodeManager.this.mPreviewWidth, data).sendToTarget();
            }
        }
    };
    private int mPreviewFormat = 17;
    private int mPreviewHeight;
    private int mPreviewLayoutHeight;
    private int mPreviewLayoutWidth;
    private int mPreviewWidth;
    private String mResult;
    private long mResumeTime = -1;
    private boolean mUIInitialized;
    private TextView mViewFinderButton;
    private ViewGroup mViewFinderFrame;

    public interface QRCodeManagerListener {
        void findQRCode();

        boolean scanQRCodeEnabled();
    }

    private class MyHander extends Handler {
        public MyHander(Looper loop) {
            super(loop);
        }

        public void dispatchMessage(Message msg) {
            switch (msg.what) {
                case R.id.find_timeout:
                    if (QRCodeManager.this.mViewFinderFrame != null) {
                        QRCodeManager.this.mViewFinderFrame.setVisibility(8);
                        return;
                    }
                    return;
                case R.id.find_qrcode:
                    QRCodeManager.this.mResult = QRCodeManager.recode(((Result) msg.obj).getText());
                    if (!(QRCodeManager.this.mViewFinderFrame == null || QRCodeManager.this.mActivity == null || !QRCodeManager.this.scanQRCodeEnabled())) {
                        QRCodeManager.this.mViewFinderFrame.setVisibility(0);
                        QRCodeManager.this.mListener.findQRCode();
                    }
                    QRCodeManager.this.sendDecodeMessageSafe(4000);
                    return;
                case R.id.try_decode_qrcode:
                    if (QRCodeManager.this.scanQRCodeEnabled()) {
                        QRCodeManager.this.mCameraDevice.setOneShotPreviewCallback(QRCodeManager.this.mPreviewCallback);
                    }
                    QRCodeManager.this.sendDecodeMessageSafe(1000);
                    return;
                case R.id.decode_exit:
                    QRCodeManager.this.exitDecode();
                    Log.e("QRCodeManager", "exit decode qrcode for timeout, mResumeTime=" + QRCodeManager.this.mResumeTime + " now=" + System.currentTimeMillis() + " decodetime=" + QRCodeManager.DECODE_TOTAL_TIME);
                    return;
                default:
                    return;
            }
        }
    }

    static {
        MAX_FRAME_HEIGHT = 360;
        MAX_FRAME_WIDTH = 480;
        CENTER_FRAME_WIDTH = 720;
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) CameraAppImpl.getAndroidContext().getSystemService("window")).getDefaultDisplay().getMetrics(metrics);
        CENTER_FRAME_WIDTH = metrics.widthPixels;
        MAX_FRAME_HEIGHT = CENTER_FRAME_WIDTH;
        MAX_FRAME_WIDTH = CENTER_FRAME_WIDTH;
    }

    public void onClick(View v) {
        AutoLockManager.getInstance(this.mActivity).onUserInteraction();
        CameraDataAnalytics.instance().trackEvent("qrcode_detected_times_key");
        show();
    }

    public boolean onBackPressed() {
        return hide();
    }

    private void show() {
        if (this.mUIInitialized) {
            this.mViewFinderFrame.setVisibility(8);
            try {
                ActivityBase activityBase = this.mActivity;
                activityBase.dismissKeyguard();
                Intent intent = new Intent("android.intent.action.receiverResultBarcodeScanner");
                intent.setPackage("com.xiaomi.scanner");
                intent.putExtra("result", this.mResult);
                activityBase.startActivity(intent);
                activityBase.setJumpFlag(3);
            } catch (ActivityNotFoundException ex) {
                ex.printStackTrace();
                Log.e("QRCodeManager", "check if BarcodeScanner tool is installed or not");
            }
        }
    }

    public void hideViewFinderFrame() {
        if (this.mViewFinderFrame != null && this.mViewFinderFrame.getVisibility() == 0) {
            this.mViewFinderFrame.setVisibility(8);
        }
    }

    public void needScanQRCode(boolean scan) {
        this.mNeedScan = this.mDecode ? scan : false;
        if (this.mHandler != null) {
            if (scan) {
                sendDecodeMessageSafe(1000);
            } else {
                this.mHandler.removeMessages(R.id.try_decode_qrcode);
            }
        }
        if (this.mNeedScan) {
            startDecodeThreadIfNeeded();
        }
    }

    private boolean hide() {
        return false;
    }

    public static String recode(String str) {
        String formart = "";
        try {
            if (Charset.forName("ISO-8859-1").newEncoder().canEncode(str)) {
                return new String(str.getBytes("ISO-8859-1"), "GB2312");
            }
            return str;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return formart;
        }
    }

    private QRCodeManager() {
    }

    private boolean scanQRCodeEnabled() {
        return (!this.mUIInitialized || this.mPreviewWidth == 0 || this.mPreviewLayoutWidth == 0 || this.mCameraDevice == null || this.mListener == null || !this.mListener.scanQRCodeEnabled() || this.mViewFinderFrame.getVisibility() == 0) ? false : true;
    }

    private void sendDecodeMessageSafe(int delay) {
        if (this.mNeedScan && this.mUIInitialized && this.mDecode) {
            this.mHandler.removeMessages(R.id.try_decode_qrcode);
            this.mHandler.sendEmptyMessageDelayed(R.id.try_decode_qrcode, (long) delay);
        }
    }

    public static QRCodeManager instance(Context context) {
        QRCodeManager instance = (QRCodeManager) sMap.get(context);
        if (instance != null) {
            return instance;
        }
        instance = new QRCodeManager();
        sMap.put(context, instance);
        return instance;
    }

    public static void removeInstance(Context context) {
        QRCodeManager instance = (QRCodeManager) sMap.remove(context);
        if (instance != null) {
            instance.onPause();
        }
    }

    public void onCreate(Activity activity, Looper looper, QRCodeManagerListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
        this.mHandler = new MyHander(looper);
        this.mViewFinderFrame = (QRCodeFragmentLayout) this.mActivity.findViewById(R.id.qrcode_viewfinder_layout);
        this.mViewFinderButton = (TextView) this.mViewFinderFrame.findViewById(R.id.qrcode_viewfinder_button);
        this.mViewFinderButton.setOnClickListener(this);
        this.mUIInitialized = true;
        resetQRScanExit(false);
    }

    private void startDecodeThreadIfNeeded() {
        if (this.mDecodeHandlerFactory == null) {
            this.mDecodeHandlerFactory = new DecodeHandlerFactory(this.mActivity, false);
            this.mDecodeHandlerFactory.start();
        }
    }

    public void setCameraDevice(CameraProxy cameraDevice) {
        this.mCameraDevice = cameraDevice;
    }

    public void setPreviewLayoutSize(int width, int height) {
        if (this.mPreviewLayoutWidth != width || this.mPreviewLayoutHeight != height) {
            this.mPreviewLayoutWidth = width;
            this.mPreviewLayoutHeight = height;
            updateViewFinderRect();
        }
    }

    public void setTransposePreviewSize(int width, int height) {
        if (this.mPreviewWidth != height || this.mPreviewHeight != width) {
            this.mPreviewWidth = height;
            this.mPreviewHeight = width;
            updateRectInPreview();
        }
    }

    public void setPreviewFormat(int format) {
        this.mPreviewFormat = format;
    }

    public void updateViewFinderRect(Point area) {
        int width = Math.min(this.mPreviewLayoutWidth, CENTER_FRAME_WIDTH);
        int height = Math.min(this.mPreviewLayoutHeight, CENTER_FRAME_WIDTH);
        int left = (this.mPreviewLayoutWidth - width) / 2;
        int top = (this.mPreviewLayoutHeight - height) / 2;
        mRectFinderCenter.set(left, top, left + width, top + height);
        if (area != null) {
            width = Math.min(this.mPreviewLayoutWidth, MAX_FRAME_WIDTH);
            height = Math.min(this.mPreviewLayoutHeight, MAX_FRAME_HEIGHT);
            mRectFinderFocusArea.set(Math.max(area.x - (width / 2), 0), Math.max(area.y - (height / 2), 0), Math.min(area.x + (width / 2), this.mPreviewLayoutWidth), Math.min(area.y + (height / 2), this.mPreviewLayoutHeight));
        } else {
            mRectFinderFocusArea.set(0, 0, 0, 0);
        }
        updateRectInPreview();
    }

    public void updateViewFinderRect() {
        updateViewFinderRect(null);
    }

    private void updateRectInPreview() {
        if (this.mPreviewLayoutWidth != 0) {
            float scaleWidth = ((float) this.mPreviewWidth) / ((float) this.mPreviewLayoutWidth);
            float scaleHeight = ((float) this.mPreviewHeight) / ((float) this.mPreviewLayoutHeight);
            mRectPreviewFocusArea.set((int) (((float) mRectFinderFocusArea.left) * scaleWidth), (int) (((float) mRectFinderFocusArea.top) * scaleHeight), (int) (((float) mRectFinderFocusArea.right) * scaleWidth), (int) (((float) mRectFinderFocusArea.bottom) * scaleHeight));
            mRectPreviewCenter.set((int) (((float) mRectFinderCenter.left) * scaleWidth), (int) (((float) mRectFinderCenter.top) * scaleHeight), (int) (((float) mRectFinderCenter.right) * scaleWidth), (int) (((float) mRectFinderCenter.bottom) * scaleHeight));
        }
    }

    public void onPause() {
        if (this.mViewFinderFrame != null) {
            this.mViewFinderFrame.setVisibility(8);
        }
        exitDecode();
        this.mCameraDevice = null;
        this.mResult = null;
    }

    public void onDestroy() {
        removeInstance(this.mActivity);
        this.mActivity = null;
        this.mHandler = null;
    }

    public Handler getHandler() {
        return this.mHandler;
    }

    public void requestDecode() {
        if (this.mHandler != null) {
            sendDecodeMessageSafe(100);
        }
    }

    public YUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height, boolean center) {
        if (this.mPreviewFormat == 17 && scanQRCodeEnabled()) {
            if (center && !mRectPreviewCenter.isEmpty()) {
                return new YUVLuminanceSource(data, width, height, mRectPreviewCenter.left, mRectPreviewCenter.top, mRectPreviewCenter.width(), mRectPreviewCenter.height());
            } else if (!(mRectPreviewFocusArea.isEmpty() || mRectPreviewCenter.contains(mRectPreviewFocusArea))) {
                return new YUVLuminanceSource(data, width, height, mRectPreviewFocusArea.left, mRectPreviewFocusArea.top, mRectPreviewFocusArea.width(), mRectPreviewFocusArea.height());
            }
        }
        return null;
    }

    private void exitDecode() {
        if (this.mHandler != null) {
            this.mHandler.removeMessages(R.id.find_qrcode);
            this.mHandler.removeMessages(R.id.try_decode_qrcode);
            this.mHandler.removeMessages(R.id.find_timeout);
            this.mHandler.removeMessages(R.id.decode_exit);
        }
        if (this.mDecodeHandlerFactory != null) {
            this.mDecodeHandlerFactory.quit();
        }
        this.mDecode = false;
        this.mDecodeHandlerFactory = null;
    }

    public void resetQRScanExit(boolean resumeFlag) {
        if (resumeFlag) {
            this.mResumeTime = System.currentTimeMillis();
        }
        long now = System.currentTimeMillis();
        boolean z = (DECODE_TOTAL_TIME == -1 || this.mResumeTime == -1) ? true : !Util.isTimeout(now, this.mResumeTime, (long) DECODE_TOTAL_TIME);
        this.mDecode = z;
        if (!this.mDecode) {
            Log.e("QRCodeManager", "we should not decode qrcode, mResumeTime=" + this.mResumeTime + " now=" + now + " resumetime=" + (now - this.mResumeTime) + " decodetime=" + DECODE_TOTAL_TIME);
        }
        if (this.mDecode && this.mHandler != null && this.mResumeTime != -1 && DECODE_TOTAL_TIME != -1) {
            this.mHandler.removeMessages(R.id.decode_exit);
            this.mHandler.sendEmptyMessageDelayed(R.id.decode_exit, ((long) DECODE_TOTAL_TIME) - (now - this.mResumeTime));
        }
    }
}
