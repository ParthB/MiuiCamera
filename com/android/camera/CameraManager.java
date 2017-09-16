package com.android.camera;

import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.AutoFocusMoveCallback;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.FaceDetectionListener;
import android.hardware.Camera.OnZoomChangeListener;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.CameraHardwareProxy.CameraMetaDataCallback;
import com.android.camera.hardware.CameraHardwareProxy.ContinuousShotCallback;
import java.io.IOException;
import java.util.ConcurrentModificationException;

public class CameraManager {
    private static CameraManager sCameraManager = new CameraManager();
    private Camera mCamera;
    private volatile boolean mCameraError;
    private Handler mCameraHandler;
    private CameraProxy mCameraProxy;
    private boolean mFlashOn;
    private boolean mFocusSuccessful;
    private Parameters mParameters;
    private boolean mPreviewEnable;
    private CameraHardwareProxy mProxy;
    private IOException mReconnectException;
    private ConditionVariable mSig = new ConditionVariable();
    private int mWBCT;

    private class CameraHandler extends Handler {
        CameraHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            String paramEmptyException;
            int retry_cnt;
            int retry_cnt2;
            try {
                switch (msg.what) {
                    case 1:
                        CameraManager.this.mCamera.release();
                        CameraManager.this.mCamera = null;
                        CameraManager.this.mCameraProxy = null;
                        break;
                    case 2:
                        CameraManager.this.mReconnectException = null;
                        try {
                            CameraManager.this.mCamera.reconnect();
                            break;
                        } catch (IOException ex) {
                            CameraManager.this.mReconnectException = ex;
                            break;
                        }
                    case 3:
                        CameraManager.this.mCamera.unlock();
                        break;
                    case 4:
                        CameraManager.this.mCamera.lock();
                        break;
                    case 5:
                        CameraManager.this.mCamera.setPreviewTexture((SurfaceTexture) msg.obj);
                        break;
                    case 6:
                        CameraManager.this.mCamera.startPreview();
                        return;
                    case 7:
                        CameraManager.this.mCamera.stopPreview();
                        break;
                    case 8:
                        CameraManager.this.mCamera.setPreviewCallbackWithBuffer((PreviewCallback) msg.obj);
                        break;
                    case 9:
                        CameraManager.this.mCamera.addCallbackBuffer((byte[]) msg.obj);
                        break;
                    case 10:
                        CameraManager.this.mCamera.autoFocus((AutoFocusCallback) msg.obj);
                        break;
                    case 11:
                        paramEmptyException = "cancelAutoFocus failed";
                        CameraManager.this.mCamera.cancelAutoFocus();
                        break;
                    case 12:
                        CameraManager.this.mCamera.setAutoFocusMoveCallback((AutoFocusMoveCallback) msg.obj);
                        break;
                    case 13:
                        CameraManager.this.mCamera.setDisplayOrientation(msg.arg1);
                        break;
                    case 14:
                        CameraManager.this.mCamera.setZoomChangeListener((OnZoomChangeListener) msg.obj);
                        break;
                    case 15:
                        CameraManager.this.mCamera.setFaceDetectionListener((FaceDetectionListener) msg.obj);
                        break;
                    case 16:
                        CameraManager.this.mCamera.startFaceDetection();
                        break;
                    case 17:
                        CameraManager.this.mCamera.stopFaceDetection();
                        break;
                    case 18:
                        CameraManager.this.mCamera.setErrorCallback((ErrorCallback) msg.obj);
                        break;
                    case 19:
                        CameraManager.this.mProxy.setParameters(CameraManager.this.mCamera, (Parameters) msg.obj);
                        break;
                    case 20:
                        paramEmptyException = "getParameters failed (empty parameters)";
                        retry_cnt2 = 3;
                        while (true) {
                            retry_cnt = retry_cnt2 - 1;
                            if (retry_cnt2 <= 0) {
                                break;
                            }
                            CameraManager.this.mParameters = CameraManager.this.mCamera.getParameters();
                            break;
                        }
                        break;
                    case 21:
                        CameraManager.this.mProxy.setParameters(CameraManager.this.mCamera, (Parameters) msg.obj);
                        return;
                    case 23:
                        CameraManager.this.mCamera.setOneShotPreviewCallback((PreviewCallback) msg.obj);
                        break;
                    case 24:
                        CameraManager.this.mCamera.addRawImageCallbackBuffer((byte[]) msg.obj);
                        break;
                    case 25:
                        CameraManager.this.mCamera.startPreview();
                        break;
                    case 100:
                        CameraManager.this.mFlashOn = CameraManager.this.mProxy.isNeedFlashOn(CameraManager.this.mCamera);
                        break;
                    case 103:
                        CameraManager.this.mWBCT = CameraManager.this.mProxy.getWBCurrentCCT(CameraManager.this.mCamera);
                        break;
                    case 104:
                        CameraManager.this.mProxy.cancelContinuousMode(CameraManager.this.mCamera);
                        break;
                    case 105:
                        CameraManager.this.mProxy.setLongshotMode(CameraManager.this.mCamera, ((Boolean) msg.obj).booleanValue());
                        break;
                    case 106:
                        RectF rect = msg.obj;
                        CameraManager.this.mProxy.startObjectTrack(CameraManager.this.mCamera, (int) rect.left, (int) rect.top, (int) rect.width(), (int) rect.height());
                        break;
                    case 107:
                        CameraManager.this.mProxy.stopObjectTrack(CameraManager.this.mCamera);
                        break;
                    case 108:
                        CameraManager.this.mProxy.setMetadataCb(CameraManager.this.mCamera, (CameraMetaDataCallback) msg.obj);
                        break;
                    case 109:
                        CameraManager.this.mFocusSuccessful = CameraManager.this.mProxy.isFocusSuccessful(CameraManager.this.mCamera);
                        break;
                    case 110:
                        CameraManager.this.mPreviewEnable = CameraManager.this.mProxy.isPreviewEnabled(CameraManager.this.mCamera);
                        break;
                    case 111:
                        CameraManager.this.mProxy.setBurstShotSpeed(CameraManager.this.mCamera, msg.arg1);
                        break;
                    case 112:
                        CameraManager.this.mCamera.setPreviewDisplay((SurfaceHolder) msg.obj);
                        break;
                    case 113:
                        CameraManager.this.mProxy.setContinuousShotCallback(CameraManager.this.mCamera, (ContinuousShotCallback) msg.obj);
                        break;
                    case 114:
                        CameraManager.this.mCamera.setPreviewCallback((PreviewCallback) msg.obj);
                        break;
                    case 115:
                        CameraManager.this.mProxy.setStereoDataCallback(CameraManager.this.mCamera, msg.obj);
                        break;
                    case 116:
                        CameraManager.this.mProxy.setStereoWarningCallback(CameraManager.this.mCamera, msg.obj);
                        break;
                    case 117:
                        CameraManager.this.mProxy.enableRaw(CameraManager.this.mCamera, msg.obj);
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception e2) {
                if (!e2.getMessage().contains(paramEmptyException) || retry_cnt == 0) {
                    throw new RuntimeException(e2.getMessage());
                }
                retry_cnt2 = retry_cnt;
            } catch (Exception e22) {
                if (!e22.getMessage().contains(paramEmptyException)) {
                    throw new RuntimeException(e22.getMessage());
                }
            } catch (IOException e3) {
                throw new RuntimeException(e3);
            } catch (ConcurrentModificationException e4) {
                Log.e("CameraManager", "ConcurrentModificationException: " + e4.toString());
            } catch (RuntimeException e5) {
                boolean hardwareFail = false;
                if (!(msg.what == 1 || CameraManager.this.mCamera == null)) {
                    if (!CameraManager.this.mCameraError) {
                        try {
                            Log.e("CameraManager", "camera hardware state test, use getParameters, msg=" + e5.getMessage());
                            CameraManager.this.mCamera.getParameters();
                            Log.e("CameraManager", "camera hardware state is normal");
                        } catch (Exception ex2) {
                            Log.e("CameraManager", "camera hardware crashed ", ex2);
                            hardwareFail = true;
                        }
                    }
                    try {
                        CameraManager.this.mCamera.release();
                    } catch (Exception ex22) {
                        Log.e("CameraManager", "Fail to release the camera.", ex22);
                        hardwareFail = true;
                    }
                    if (hardwareFail) {
                        CameraManager.this.mCameraProxy.notifyHardwareError();
                    }
                    CameraManager.this.mCamera = null;
                    CameraManager.this.mCameraProxy = null;
                }
                Log.v("CameraManager", "exception in camerahandler, mCameraError=" + CameraManager.this.mCameraError + " " + hardwareFail);
                if (!(CameraManager.this.mCameraError || hardwareFail)) {
                    throw e5;
                }
            }
            CameraManager.this.mSig.open();
        }
    }

    public class CameraProxy {
        private HardwareErrorListener mHardwareErrorListener;

        private CameraProxy() {
            Util.Assert(CameraManager.this.mCamera != null);
        }

        public void notifyHardwareError() {
            Log.e("CameraManager", "mark camera error from manager notify");
            CameraManager.this.mCameraError = true;
            if (this.mHardwareErrorListener != null) {
                this.mHardwareErrorListener.notifyError();
            }
        }

        public void setHardwareListener(HardwareErrorListener listener) {
            this.mHardwareErrorListener = listener;
        }

        public Camera getCamera() {
            return CameraManager.this.mCamera;
        }

        public void setCameraError() {
            Log.e("CameraManager", "mark camera error from callback");
            CameraManager.this.mCameraError = true;
        }

        public void release() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(1);
            CameraManager.this.mSig.block();
            setHardwareListener(null);
        }

        public void reconnect() throws IOException {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(2);
            CameraManager.this.mSig.block();
            if (CameraManager.this.mReconnectException != null) {
                throw CameraManager.this.mReconnectException;
            }
        }

        public void unlock() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(3);
            CameraManager.this.mSig.block();
        }

        public void lock() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(4);
            CameraManager.this.mSig.block();
        }

        public void setPreviewTexture(SurfaceTexture surfaceTexture) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(5, surfaceTexture).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setPreviewDisplay(SurfaceHolder surfaceHolder) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(112, surfaceHolder).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void startPreviewAsync() {
            CameraManager.this.mCameraHandler.sendEmptyMessage(6);
        }

        public void startPreview() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(25);
            CameraManager.this.mSig.block();
        }

        public void removeAllAsyncMessage() {
            CameraManager.this.mCameraHandler.removeMessages(21);
            CameraManager.this.mCameraHandler.removeMessages(6);
        }

        public void stopPreview() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(7);
            CameraManager.this.mSig.block();
        }

        public void setPreviewCallbackWithBuffer(PreviewCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(8, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setPreviewCallback(PreviewCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(114, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setOneShotPreviewCallback(PreviewCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(23, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public final void setLongshotMode(boolean enable) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(105, Boolean.valueOf(enable)).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void addCallbackBuffer(byte[] callbackBuffer) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(9, callbackBuffer).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void autoFocus(AutoFocusCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(10, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void cancelAutoFocus() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(11);
            CameraManager.this.mSig.block();
        }

        public void setAutoFocusMoveCallback(AutoFocusMoveCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(12, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg) {
            CameraManager.this.mSig.close();
            final ShutterCallback shutterCallback = shutter;
            final PictureCallback pictureCallback = raw;
            final PictureCallback pictureCallback2 = postview;
            final PictureCallback pictureCallback3 = jpeg;
            CameraManager.this.mCameraHandler.post(new Runnable() {
                public void run() {
                    if (CameraManager.this.mCamera != null) {
                        CameraManager.this.mCamera.takePicture(shutterCallback, pictureCallback, pictureCallback2, pictureCallback3);
                    }
                    CameraManager.this.mSig.open();
                }
            });
            CameraManager.this.mSig.block();
        }

        public void cancelPicture() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(104);
            CameraManager.this.mSig.block();
        }

        public void setDisplayOrientation(int degrees) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(13, degrees, 0).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setZoomChangeListener(OnZoomChangeListener listener) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(14, listener).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setFaceDetectionListener(FaceDetectionListener listener) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(15, listener).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void startFaceDetection() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(16);
            CameraManager.this.mSig.block();
        }

        public void stopFaceDetection() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(17);
            CameraManager.this.mSig.block();
        }

        public void setErrorCallback(ErrorCallback cb) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(18, cb).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setParameters(Parameters params) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(19, params).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setParametersAsync(Parameters params) {
            CameraManager.this.mCameraHandler.removeMessages(21);
            CameraManager.this.mCameraHandler.obtainMessage(21, params).sendToTarget();
        }

        public boolean isNeedFlashOn() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(100);
            CameraManager.this.mSig.block();
            return CameraManager.this.mFlashOn;
        }

        public boolean isFocusSuccessful() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(109);
            CameraManager.this.mSig.block();
            return CameraManager.this.mFocusSuccessful;
        }

        public int getWBCT() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(103);
            CameraManager.this.mSig.block();
            return CameraManager.this.mWBCT;
        }

        public Parameters getParameters() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(20);
            CameraManager.this.mSig.block();
            return CameraManager.this.mParameters;
        }

        public void addRawImageCallbackBuffer(byte[] buffer) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(24, buffer).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void startObjectTrack(RectF rect) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(106, rect).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void stopObjectTrack() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(107);
            CameraManager.this.mSig.block();
        }

        public void setMetaDataCallback(CameraMetaDataCallback cameraMetaDataCallback) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(108, cameraMetaDataCallback).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public boolean isPreviewEnable() {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.sendEmptyMessage(110);
            CameraManager.this.mSig.block();
            return CameraManager.this.mPreviewEnable;
        }

        public void setBurstShotSpeed(int burstSpeed) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(111, burstSpeed, 0).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setContinuousShotCallback(ContinuousShotCallback callback) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(113, callback).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setStereoDataCallback(Object callback) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(115, callback).sendToTarget();
            CameraManager.this.mSig.block();
        }

        public void setStereoWarningCallback(Object callback) {
            CameraManager.this.mSig.close();
            CameraManager.this.mCameraHandler.obtainMessage(116, callback).sendToTarget();
            CameraManager.this.mSig.block();
        }
    }

    public interface HardwareErrorListener {
        void notifyError();
    }

    public static CameraManager instance() {
        return sCameraManager;
    }

    public CameraProxy getCameraProxy() {
        return this.mCameraProxy;
    }

    public Parameters getStashParameters() {
        return this.mParameters;
    }

    private CameraManager() {
        HandlerThread ht = new HandlerThread("Camera Handler Thread");
        ht.start();
        this.mCameraHandler = new CameraHandler(ht.getLooper());
        this.mProxy = CameraHardwareProxy.getDeviceProxy();
    }

    CameraProxy cameraOpen(int cameraId) {
        this.mCameraError = false;
        this.mCamera = this.mProxy.openCamera(cameraId);
        if (this.mCamera == null) {
            return null;
        }
        this.mCameraProxy = new CameraProxy();
        return this.mCameraProxy;
    }
}
