package com.android.camera;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.camera.CameraManager.CameraProxy;

public class CameraHolder {
    private static CameraProxy[] mMockCamera;
    private static CameraInfo[] mMockCameraInfo;
    private static CameraHolder sHolder;
    private int mAuxCameraId = -1;
    private int mBackCameraId = -1;
    private CameraProxy mCameraDevice;
    private int mCameraId = -1;
    private boolean mCameraOpened;
    private int mFrontCameraId = -1;
    private final Handler mHandler;
    private final CameraInfo[] mInfo;
    private long mKeepBeforeTime;
    private final int mNumberOfCameras;
    private Parameters mParameters;
    private int mViceBackCameraId = -1;

    private class MyHandler extends Handler {
        MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    synchronized (CameraHolder.this) {
                        if (!CameraHolder.this.mCameraOpened) {
                            CameraHolder.this.release();
                        }
                    }
                    return;
                default:
                    return;
            }
        }
    }

    public static synchronized CameraHolder instance() {
        CameraHolder cameraHolder;
        synchronized (CameraHolder.class) {
            if (sHolder == null) {
                sHolder = new CameraHolder();
            }
            cameraHolder = sHolder;
        }
        return cameraHolder;
    }

    public static void injectMockCamera(CameraInfo[] info, CameraProxy[] camera) {
        mMockCameraInfo = info;
        mMockCamera = camera;
        sHolder = new CameraHolder();
    }

    private CameraHolder() {
        int i;
        HandlerThread ht = new HandlerThread("CameraHolder");
        ht.start();
        this.mHandler = new MyHandler(ht.getLooper());
        if (mMockCameraInfo != null) {
            this.mNumberOfCameras = mMockCameraInfo.length;
            this.mInfo = mMockCameraInfo;
        } else {
            this.mNumberOfCameras = Camera.getNumberOfCameras();
            this.mInfo = new CameraInfo[this.mNumberOfCameras];
            for (i = 0; i < this.mNumberOfCameras; i++) {
                this.mInfo[i] = new CameraInfo();
                Camera.getCameraInfo(i, this.mInfo[i]);
            }
        }
        i = 0;
        while (i < this.mNumberOfCameras) {
            if (this.mBackCameraId == -1 && this.mInfo[i].facing == 0) {
                this.mBackCameraId = i;
            } else if (this.mFrontCameraId == -1 && this.mInfo[i].facing == 1) {
                this.mFrontCameraId = i;
            } else if (this.mViceBackCameraId == -1 && this.mInfo[i].facing == 0) {
                this.mViceBackCameraId = i;
            } else if (this.mAuxCameraId == -1 && this.mInfo[i].facing == 0) {
                this.mAuxCameraId = i;
            }
            i++;
        }
    }

    public int getNumberOfCameras() {
        return this.mNumberOfCameras;
    }

    public CameraInfo[] getCameraInfo() {
        return this.mInfo;
    }

    public CameraProxy open(int cameraId) throws CameraHardwareException {
        return open(cameraId, true);
    }

    public synchronized com.android.camera.CameraManager.CameraProxy open(int r8, boolean r9) throws com.android.camera.CameraHardwareException {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.JadxRuntimeException: Exception block dominator not found, method:com.android.camera.CameraHolder.open(int, boolean):com.android.camera.CameraManager$CameraProxy. bs: [B:29:0x005d, B:61:0x00dd]
	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.searchTryCatchDominators(ProcessTryCatchRegions.java:86)
	at jadx.core.dex.visitors.regions.ProcessTryCatchRegions.process(ProcessTryCatchRegions.java:45)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.postProcessRegions(RegionMakerVisitor.java:63)
	at jadx.core.dex.visitors.regions.RegionMakerVisitor.visit(RegionMakerVisitor.java:58)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:31)
	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:17)
	at jadx.core.ProcessClass.process(ProcessClass.java:37)
	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:59)
	at jadx.core.ProcessClass.process(ProcessClass.java:42)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:306)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
*/
        /*
        r7 = this;
        r4 = 1;
        monitor-enter(r7);
        r5 = r7.mCameraOpened;	 Catch:{ all -> 0x00d3 }
        if (r5 == 0) goto L_0x0007;	 Catch:{ all -> 0x00d3 }
    L_0x0006:
        r4 = 0;	 Catch:{ all -> 0x00d3 }
    L_0x0007:
        com.android.camera.Util.Assert(r4);	 Catch:{ all -> 0x00d3 }
        r4 = com.android.camera.CameraSettings.isSupportedOpticalZoom();	 Catch:{ all -> 0x00d3 }
        if (r4 != 0) goto L_0x00a3;	 Catch:{ all -> 0x00d3 }
    L_0x0010:
        r3 = com.android.camera.CameraSettings.isSupportedPortrait();	 Catch:{ all -> 0x00d3 }
    L_0x0014:
        if (r9 == 0) goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x0016:
        if (r3 == 0) goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x0018:
        r4 = r7.getBackCameraId();	 Catch:{ all -> 0x00d3 }
        if (r8 != r4) goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x001e:
        r4 = com.android.camera.ui.V6ModulePicker.isCameraModule();	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x002d;	 Catch:{ all -> 0x00d3 }
    L_0x0024:
        r4 = "pref_camera_manual_mode_key";	 Catch:{ all -> 0x00d3 }
        r4 = com.android.camera.CameraSettings.isSwitchOn(r4);	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x00a6;	 Catch:{ all -> 0x00d3 }
    L_0x002d:
        r4 = com.android.camera.CameraSettings.isSwitchCameraZoomMode();	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x0033:
        r2 = com.android.camera.CameraSettings.getCameraZoomMode();	 Catch:{ all -> 0x00d3 }
        r4 = 2131624430; // 0x7f0e01ee float:1.887604E38 double:1.0531624007E-314;	 Catch:{ all -> 0x00d3 }
        r4 = com.android.camera.CameraSettings.getString(r4);	 Catch:{ all -> 0x00d3 }
        r4 = r4.equals(r2);	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x00a9;	 Catch:{ all -> 0x00d3 }
    L_0x0044:
        r8 = r7.mBackCameraId;	 Catch:{ all -> 0x00d3 }
    L_0x0046:
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x0059;	 Catch:{ all -> 0x00d3 }
    L_0x004a:
        r4 = r7.mCameraId;	 Catch:{ all -> 0x00d3 }
        if (r4 == r8) goto L_0x0059;	 Catch:{ all -> 0x00d3 }
    L_0x004e:
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        r4.release();	 Catch:{ all -> 0x00d3 }
        r4 = 0;	 Catch:{ all -> 0x00d3 }
        r7.mCameraDevice = r4;	 Catch:{ all -> 0x00d3 }
        r4 = -1;	 Catch:{ all -> 0x00d3 }
        r7.mCameraId = r4;	 Catch:{ all -> 0x00d3 }
    L_0x0059:
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        if (r4 != 0) goto L_0x00dd;
    L_0x005d:
        r4 = "CameraHolder";	 Catch:{ RuntimeException -> 0x00c3 }
        r5 = new java.lang.StringBuilder;	 Catch:{ RuntimeException -> 0x00c3 }
        r5.<init>();	 Catch:{ RuntimeException -> 0x00c3 }
        r6 = "open camera ";	 Catch:{ RuntimeException -> 0x00c3 }
        r5 = r5.append(r6);	 Catch:{ RuntimeException -> 0x00c3 }
        r5 = r5.append(r8);	 Catch:{ RuntimeException -> 0x00c3 }
        r5 = r5.toString();	 Catch:{ RuntimeException -> 0x00c3 }
        android.util.Log.v(r4, r5);	 Catch:{ RuntimeException -> 0x00c3 }
        r4 = mMockCameraInfo;	 Catch:{ RuntimeException -> 0x00c3 }
        if (r4 != 0) goto L_0x00b9;	 Catch:{ RuntimeException -> 0x00c3 }
    L_0x007b:
        r4 = com.android.camera.CameraManager.instance();	 Catch:{ RuntimeException -> 0x00c3 }
        r4 = r4.cameraOpen(r8);	 Catch:{ RuntimeException -> 0x00c3 }
        r7.mCameraDevice = r4;	 Catch:{ RuntimeException -> 0x00c3 }
    L_0x0085:
        r7.mCameraId = r8;	 Catch:{ RuntimeException -> 0x00c3 }
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        r4 = r4.getParameters();	 Catch:{ all -> 0x00d3 }
        r7.mParameters = r4;	 Catch:{ all -> 0x00d3 }
    L_0x008f:
        r4 = 1;	 Catch:{ all -> 0x00d3 }
        r7.mCameraOpened = r4;	 Catch:{ all -> 0x00d3 }
        com.android.camera.CameraSettings.resetOpenCameraFailTimes();	 Catch:{ all -> 0x00d3 }
        r4 = r7.mHandler;	 Catch:{ all -> 0x00d3 }
        r5 = 1;	 Catch:{ all -> 0x00d3 }
        r4.removeMessages(r5);	 Catch:{ all -> 0x00d3 }
        r4 = 0;	 Catch:{ all -> 0x00d3 }
        r7.mKeepBeforeTime = r4;	 Catch:{ all -> 0x00d3 }
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        monitor-exit(r7);
        return r4;
    L_0x00a3:
        r3 = 1;
        goto L_0x0014;
    L_0x00a6:
        r8 = r7.mAuxCameraId;	 Catch:{ all -> 0x00d3 }
        goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x00a9:
        r4 = 2131624431; // 0x7f0e01ef float:1.8876042E38 double:1.053162401E-314;	 Catch:{ all -> 0x00d3 }
        r4 = com.android.camera.CameraSettings.getString(r4);	 Catch:{ all -> 0x00d3 }
        r4 = r4.equals(r2);	 Catch:{ all -> 0x00d3 }
        if (r4 == 0) goto L_0x0046;	 Catch:{ all -> 0x00d3 }
    L_0x00b6:
        r8 = r7.mViceBackCameraId;	 Catch:{ all -> 0x00d3 }
        goto L_0x0046;
    L_0x00b9:
        r4 = mMockCamera;	 Catch:{ RuntimeException -> 0x00c3 }
        if (r4 != 0) goto L_0x00d6;	 Catch:{ RuntimeException -> 0x00c3 }
    L_0x00bd:
        r4 = new java.lang.RuntimeException;	 Catch:{ RuntimeException -> 0x00c3 }
        r4.<init>();	 Catch:{ RuntimeException -> 0x00c3 }
        throw r4;	 Catch:{ RuntimeException -> 0x00c3 }
    L_0x00c3:
        r1 = move-exception;
        r4 = "CameraHolder";	 Catch:{ all -> 0x00d3 }
        r5 = "fail to connect Camera";	 Catch:{ all -> 0x00d3 }
        android.util.Log.e(r4, r5, r1);	 Catch:{ all -> 0x00d3 }
        r4 = new com.android.camera.CameraHardwareException;	 Catch:{ all -> 0x00d3 }
        r4.<init>(r1);	 Catch:{ all -> 0x00d3 }
        throw r4;	 Catch:{ all -> 0x00d3 }
    L_0x00d3:
        r4 = move-exception;
        monitor-exit(r7);
        throw r4;
    L_0x00d6:
        r4 = mMockCamera;	 Catch:{ RuntimeException -> 0x00c3 }
        r4 = r4[r8];	 Catch:{ RuntimeException -> 0x00c3 }
        r7.mCameraDevice = r4;	 Catch:{ RuntimeException -> 0x00c3 }
        goto L_0x0085;
    L_0x00dd:
        r4 = r7.mCameraDevice;	 Catch:{ IOException -> 0x00ea }
        r4.reconnect();	 Catch:{ IOException -> 0x00ea }
        r4 = r7.mCameraDevice;	 Catch:{ all -> 0x00d3 }
        r5 = r7.mParameters;	 Catch:{ all -> 0x00d3 }
        r4.setParameters(r5);	 Catch:{ all -> 0x00d3 }
        goto L_0x008f;	 Catch:{ all -> 0x00d3 }
    L_0x00ea:
        r0 = move-exception;	 Catch:{ all -> 0x00d3 }
        r4 = "CameraHolder";	 Catch:{ all -> 0x00d3 }
        r5 = "reconnect failed.";	 Catch:{ all -> 0x00d3 }
        android.util.Log.e(r4, r5);	 Catch:{ all -> 0x00d3 }
        r4 = new com.android.camera.CameraHardwareException;	 Catch:{ all -> 0x00d3 }
        r4.<init>(r0);	 Catch:{ all -> 0x00d3 }
        throw r4;	 Catch:{ all -> 0x00d3 }
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.CameraHolder.open(int, boolean):com.android.camera.CameraManager$CameraProxy");
    }

    public synchronized CameraProxy tryOpen(int cameraId) {
        CameraProxy cameraProxy = null;
        synchronized (this) {
            try {
                if (!this.mCameraOpened) {
                    cameraProxy = open(cameraId);
                }
            } catch (CameraHardwareException e) {
                if (!"eng".equals(Build.TYPE)) {
                    return null;
                }
                throw new RuntimeException(e);
            }
        }
        return cameraProxy;
    }

    public synchronized void release() {
        boolean z = true;
        synchronized (this) {
            if (this.mCameraDevice == null) {
                z = false;
            }
            Util.Assert(z);
            long now = System.currentTimeMillis();
            if (now < this.mKeepBeforeTime) {
                if (this.mCameraOpened) {
                    this.mCameraOpened = false;
                    this.mCameraDevice.stopPreview();
                }
                this.mHandler.sendEmptyMessageDelayed(1, this.mKeepBeforeTime - now);
                return;
            }
            this.mCameraOpened = false;
            this.mCameraDevice.release();
            this.mCameraDevice = null;
            this.mParameters = null;
            this.mCameraId = -1;
        }
    }

    public synchronized void keep() {
        this.mKeepBeforeTime = System.currentTimeMillis() + 3000;
    }

    public int getBackCameraId() {
        return this.mBackCameraId;
    }

    public int getFrontCameraId() {
        return this.mFrontCameraId;
    }

    public int getViceBackCameraId() {
        return this.mViceBackCameraId;
    }

    public boolean hasAuxCamera() {
        return this.mAuxCameraId > 0 && !Util.isForceCamera0();
    }
}
