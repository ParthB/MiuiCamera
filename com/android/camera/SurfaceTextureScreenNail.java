package com.android.camera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.Matrix;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.V6ModulePicker;
import com.android.gallery3d.ui.BitmapTexture;
import com.android.gallery3d.ui.ExtTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;

public abstract class SurfaceTextureScreenNail implements OnFrameAvailableListener, Rotatable {
    private static final float MOVIE_SOLID_CROPPED_X;
    private static final float MOVIE_SOLID_CROPPED_Y;
    private static HandlerThread sFrameListener = new HandlerThread("FrameListener");
    private static int sMaxHightProrityFrameCount = 8;
    private int currentFrameCount = 0;
    protected RawTexture mAnimTexture;
    protected BitmapTexture mBitmapTexture;
    private int mCameraHeight;
    private int mCameraWidth;
    private DrawExtTexAttribute mDrawAttribute = new DrawExtTexAttribute();
    protected ExtTexture mExtTexture;
    protected FrameBuffer mFrameBuffer;
    protected GLSurfaceStatusBar mGLSurfaceStatusBar = new GLSurfaceStatusBar();
    private boolean mHasTexture = false;
    private int mHeight;
    private boolean mIsFullScreen;
    private boolean mIsRatio16_9 = true;
    protected boolean mModuleSwitching;
    private boolean mNeedCropped;
    private int mRenderHeight;
    protected Rect mRenderLayoutRect = new Rect();
    private int mRenderOffsetX;
    private int mRenderOffsetY;
    private int mRenderWidth;
    private float mScaleX = 1.0f;
    private float mScaleY = 1.0f;
    protected boolean mSkipFirstFrame;
    protected int mSurfaceHeight;
    private SurfaceTexture mSurfaceTexture;
    protected int mSurfaceWidth;
    private int mTargetRatio = -1;
    protected int mTheight;
    private float[] mTransform = new float[16];
    protected int mTwidth;
    protected int mTx;
    protected int mTy;
    private int mUncroppedRenderHeight;
    private int mUncroppedRenderWidth;
    private boolean mVideoStabilizationCropped;
    private int mWidth;

    public abstract void onFrameAvailable(SurfaceTexture surfaceTexture);

    public abstract void releaseBitmapIfNeeded();

    static {
        float f;
        float f2 = 0.9f;
        if (Device.isNvPlatform()) {
            f = 0.9f;
        } else {
            f = 0.8f;
        }
        MOVIE_SOLID_CROPPED_X = f;
        if (!Device.isNvPlatform()) {
            f2 = 0.8f;
        }
        MOVIE_SOLID_CROPPED_Y = f2;
    }

    public void acquireSurfaceTexture() {
        this.mExtTexture = new ExtTexture();
        this.mExtTexture.setSize(this.mWidth, this.mHeight);
        initializeTexture();
        this.mAnimTexture = new RawTexture(720, (this.mHeight * 720) / this.mWidth, true);
        this.mFrameBuffer = null;
        synchronized (this) {
            this.mHasTexture = true;
            this.mModuleSwitching = false;
            this.mSkipFirstFrame = false;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void initializeTexture() {
        /*
        r7 = this;
        r3 = 21;
        r0 = com.android.camera.Device.isSubthreadFrameListerner();
        if (r0 == 0) goto L_0x0019;
    L_0x0008:
        r0 = sFrameListener;
        r0 = r0.isAlive();
        if (r0 != 0) goto L_0x0015;
    L_0x0010:
        r0 = sFrameListener;
        r0.start();
    L_0x0015:
        r0 = android.os.Build.VERSION.SDK_INT;
        if (r0 >= r3) goto L_0x0019;
    L_0x0019:
        r0 = r7.mSurfaceTexture;
        if (r0 != 0) goto L_0x002a;
    L_0x001d:
        r0 = new android.graphics.SurfaceTexture;
        r1 = r7.mExtTexture;
        r1 = r1.getId();
        r0.<init>(r1);
        r7.mSurfaceTexture = r0;
    L_0x002a:
        r0 = r7.mSurfaceTexture;
        r1 = r7.mWidth;
        r2 = r7.mHeight;
        r0.setDefaultBufferSize(r1, r2);
        r0 = android.os.Build.VERSION.SDK_INT;
        if (r0 < r3) goto L_0x005f;
    L_0x0037:
        r0 = com.android.camera.Device.isSubthreadFrameListerner();
        if (r0 == 0) goto L_0x005f;
    L_0x003d:
        r0 = android.graphics.SurfaceTexture.class;
        r1 = r7.mSurfaceTexture;
        r2 = "setOnFrameAvailableListener";
        r3 = "(Landroid/graphics/SurfaceTexture$OnFrameAvailableListener;Landroid/os/Handler;)V";
        r4 = 2;
        r4 = new java.lang.Object[r4];
        r5 = 0;
        r4[r5] = r7;
        r5 = new android.os.Handler;
        r6 = sFrameListener;
        r6 = r6.getLooper();
        r5.<init>(r6);
        r6 = 1;
        r4[r6] = r5;
        com.android.camera.aosp_porting.ReflectUtil.callMethod(r0, r1, r2, r3, r4);
    L_0x005e:
        return;
    L_0x005f:
        r0 = r7.mSurfaceTexture;
        r0.setOnFrameAvailableListener(r7);
        goto L_0x005e;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.SurfaceTextureScreenNail.initializeTexture():void");
    }

    public ExtTexture getExtTexture() {
        return this.mExtTexture;
    }

    public SurfaceTexture getSurfaceTexture() {
        return this.mSurfaceTexture;
    }

    public void releaseSurfaceTexture() {
        synchronized (this) {
            this.mHasTexture = false;
        }
        if (this.mExtTexture != null) {
            this.mExtTexture.recycle();
            this.mExtTexture = null;
        }
        if (this.mSurfaceTexture != null) {
            this.mSurfaceTexture.release();
            this.mSurfaceTexture.setOnFrameAvailableListener(null);
            this.mSurfaceTexture = null;
        }
        if (this.mAnimTexture != null) {
            this.mAnimTexture.recycle();
            this.mAnimTexture = null;
        }
        this.mFrameBuffer = null;
        this.mGLSurfaceStatusBar.release();
        releaseBitmapIfNeeded();
    }

    private void checkThreadPriority() {
        if (this.currentFrameCount == sMaxHightProrityFrameCount) {
            Log.i("Camera/SurfaceTextureScreenNail", "normalHandlerCapacity:set normal");
            Process.setThreadPriority(sFrameListener.getThreadId(), 0);
            this.currentFrameCount++;
        } else if (this.currentFrameCount < sMaxHightProrityFrameCount) {
            this.currentFrameCount++;
        }
    }

    public int getRenderTargeRatio() {
        return this.mTargetRatio;
    }

    public void setSize(int width, int height) {
        if (width > height) {
            this.mCameraWidth = height;
            this.mCameraHeight = width;
        } else {
            this.mCameraWidth = width;
            this.mCameraHeight = height;
        }
        this.mTargetRatio = CameraSettings.getRenderAspectRatio(width, height);
        computeRatio();
    }

    private void computeRatio() {
        if (CameraSettings.getStrictAspectRatio(this.mRenderWidth, this.mRenderHeight) > -1 || !CameraSettings.isNearAspectRatio(this.mCameraWidth, this.mCameraHeight, this.mRenderWidth, this.mRenderHeight)) {
            int width = this.mCameraWidth;
            int height = this.mCameraHeight;
            int oldWidth;
            int oldHeight;
            switch (this.mTargetRatio) {
                case 0:
                    this.mIsFullScreen = false;
                    this.mIsRatio16_9 = false;
                    if (CameraSettings.isAspectRatio4_3(width, height)) {
                        this.mNeedCropped = false;
                        this.mScaleX = 1.0f;
                        this.mScaleY = 1.0f;
                    } else {
                        this.mNeedCropped = true;
                        if (width * 4 > height * 3) {
                            oldWidth = width;
                            width = (int) (((float) height) * 0.75f);
                            this.mScaleX = ((float) width) / ((float) oldWidth);
                        } else {
                            oldHeight = height;
                            height = (int) ((((float) width) * 4.0f) / 3.0f);
                            this.mScaleY = ((float) height) / ((float) oldHeight);
                        }
                    }
                    if (CameraSettings.sCroppedIfNeeded) {
                        this.mIsFullScreen = true;
                        this.mNeedCropped = true;
                        this.mIsRatio16_9 = true;
                        height = (int) ((((float) width) * 16.0f) / 9.0f);
                        this.mScaleX *= 0.75f;
                    }
                    if (Device.isPad()) {
                        this.mIsFullScreen = true;
                        break;
                    }
                    break;
                case 1:
                    this.mIsRatio16_9 = true;
                    this.mIsFullScreen = true;
                    if (CameraSettings.isAspectRatio16_9(width, height)) {
                        this.mNeedCropped = false;
                        this.mScaleX = 1.0f;
                        this.mScaleY = 1.0f;
                    } else {
                        this.mNeedCropped = true;
                        if (width * 16 > height * 9) {
                            oldWidth = width;
                            width = (int) ((((float) height) * 9.0f) / 16.0f);
                            this.mScaleX = ((float) width) / ((float) oldWidth);
                        } else {
                            oldHeight = height;
                            height = (int) ((((float) width) * 16.0f) / 9.0f);
                            this.mScaleY = ((float) height) / ((float) oldHeight);
                        }
                    }
                    if (Device.isPad()) {
                        this.mIsRatio16_9 = false;
                        this.mNeedCropped = true;
                        height = (int) (((float) height) * 0.75f);
                        this.mScaleY *= 0.75f;
                        break;
                    }
                    break;
                case 2:
                    this.mIsFullScreen = false;
                    this.mIsRatio16_9 = false;
                    this.mNeedCropped = true;
                    if (width != height) {
                        this.mScaleX = 1.0f;
                        oldHeight = height;
                        height = width;
                        this.mScaleY = ((float) width) / ((float) oldHeight);
                        break;
                    }
                    break;
            }
            this.mWidth = width;
            this.mHeight = height;
        } else if (!(this.mCameraWidth == 0 || this.mCameraHeight == 0)) {
            if (this.mRenderWidth == 0 || this.mRenderHeight == 0 || this.mRenderWidth * this.mCameraHeight == this.mRenderHeight * this.mCameraWidth) {
                this.mNeedCropped = false;
                this.mScaleX = 1.0f;
                this.mScaleY = 1.0f;
                this.mWidth = this.mCameraWidth;
                this.mHeight = this.mCameraHeight;
            } else {
                this.mNeedCropped = true;
                if (this.mCameraWidth * this.mRenderHeight > this.mCameraHeight * this.mRenderWidth) {
                    this.mHeight = this.mCameraHeight;
                    this.mWidth = (this.mCameraHeight * this.mRenderWidth) / this.mRenderHeight;
                    this.mScaleX = ((float) this.mWidth) / ((float) this.mCameraWidth);
                    this.mScaleY = 1.0f;
                } else {
                    this.mWidth = this.mCameraWidth;
                    this.mHeight = (this.mCameraWidth * this.mRenderHeight) / this.mRenderWidth;
                    this.mScaleX = 1.0f;
                    this.mScaleY = ((float) this.mHeight) / ((float) this.mCameraHeight);
                }
            }
        }
        updateRenderSize();
        updateRenderRect();
    }

    public void setRenderArea(Rect rect) {
        this.mRenderOffsetX = rect.left;
        this.mRenderOffsetY = rect.top;
        this.mRenderWidth = rect.width();
        this.mRenderHeight = rect.height();
        computeRatio();
    }

    private void updateRenderSize() {
        if (2 != this.mTargetRatio) {
            this.mUncroppedRenderWidth = (int) (((float) this.mRenderWidth) / this.mScaleX);
            this.mUncroppedRenderHeight = (int) (((float) this.mRenderHeight) / this.mScaleY);
            return;
        }
        this.mUncroppedRenderWidth = (int) (((float) this.mRenderWidth) / this.mScaleX);
        this.mUncroppedRenderHeight = (int) (((float) this.mRenderWidth) / this.mScaleY);
    }

    public int getRenderWidth() {
        return this.mUncroppedRenderWidth;
    }

    public int getRenderHeight() {
        return this.mUncroppedRenderHeight;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public void draw(GLCanvas canvas) {
        if (this.mSkipFirstFrame) {
            this.mSkipFirstFrame = false;
            this.mSurfaceTexture.updateTexImage();
            return;
        }
        canvas.clearBuffer();
        if (this.mIsFullScreen) {
            draw(canvas, 0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
        } else {
            draw(canvas, this.mTx, this.mTy, this.mTwidth, this.mTheight);
        }
    }

    public void draw(GLCanvas canvas, int x, int y, int width, int height) {
        synchronized (this) {
            if (this.mHasTexture) {
                if (Device.isSubthreadFrameListerner()) {
                    checkThreadPriority();
                }
                canvas.setPreviewSize(this.mWidth, this.mHeight);
                this.mSurfaceTexture.updateTexImage();
                this.mSurfaceTexture.getTransformMatrix(this.mTransform);
                canvas.getState().pushState();
                updateTransformMatrix(this.mTransform);
                updateExtraTransformMatrix(this.mTransform);
                canvas.draw(this.mDrawAttribute.init(this.mExtTexture, this.mTransform, x, y, width, height));
                canvas.getState().popState();
                return;
            }
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mGLSurfaceStatusBar.setOrientation(orientation);
    }

    public void setVideoStabilizationCropped(boolean cropped) {
        if (Device.isSupportedMovieSolid()) {
            this.mVideoStabilizationCropped = cropped;
        } else {
            this.mVideoStabilizationCropped = false;
        }
    }

    protected void updateTransformMatrix(float[] matrix) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        boolean change = false;
        if (this.mVideoStabilizationCropped && V6ModulePicker.isVideoModule()) {
            scaleX = 1.0f * MOVIE_SOLID_CROPPED_X;
            scaleY = 1.0f * MOVIE_SOLID_CROPPED_Y;
            change = true;
        }
        if (this.mNeedCropped) {
            scaleX *= this.mScaleX;
            scaleY *= this.mScaleY;
            change = true;
        }
        if (change) {
            Matrix.translateM(matrix, 0, 0.5f, 0.5f, 0.0f);
            Matrix.scaleM(matrix, 0, scaleX, scaleY, 1.0f);
            Matrix.translateM(matrix, 0, -0.5f, -0.5f, 0.0f);
        }
    }

    protected void updateExtraTransformMatrix(float[] matrix) {
    }

    protected void updateRenderRect() {
        int i = 0;
        if (this.mTargetRatio == 2) {
            this.mTx = this.mRenderWidth == 0 ? 0 : (this.mRenderOffsetX * this.mSurfaceWidth) / this.mRenderWidth;
            int i2 = (this.mSurfaceHeight - this.mSurfaceWidth) / 2;
            if (this.mRenderHeight != 0) {
                i = (this.mRenderOffsetY * this.mSurfaceHeight) / this.mRenderHeight;
            }
            this.mTy = i2 + i;
            this.mTwidth = this.mSurfaceWidth;
            this.mTheight = this.mSurfaceWidth;
            this.mRenderLayoutRect.set(this.mRenderOffsetX, ((this.mRenderHeight - this.mRenderWidth) / 2) + this.mRenderOffsetY, this.mRenderWidth + this.mRenderOffsetX, (((this.mRenderHeight - this.mRenderWidth) / 2) + this.mRenderOffsetY) + this.mRenderWidth);
            return;
        }
        this.mTx = this.mRenderWidth == 0 ? 0 : (this.mRenderOffsetX * this.mSurfaceWidth) / this.mRenderWidth;
        this.mTy = this.mRenderHeight == 0 ? 0 : (this.mRenderOffsetY * this.mSurfaceHeight) / this.mRenderHeight;
        this.mTwidth = this.mSurfaceWidth;
        this.mTheight = this.mSurfaceHeight;
        this.mRenderLayoutRect.set(0, 0, this.mRenderWidth, this.mRenderHeight);
    }
}
