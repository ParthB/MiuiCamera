package com.android.camera;

import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.SurfaceTexture.OnFrameAvailableListener;
import android.opengl.Matrix;
import android.os.Build.VERSION;
import android.os.Handler;
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
import miui.reflect.Constructor;
import miui.reflect.Method;
import miui.reflect.NoSuchMethodException;

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

    private void initializeTexture() {
        if (Device.isSubthreadFrameListerner()) {
            if (!sFrameListener.isAlive()) {
                sFrameListener.start();
            }
            if (VERSION.SDK_INT < 21) {
                try {
                    this.mSurfaceTexture = (SurfaceTexture) Constructor.of(SurfaceTexture.class, "(ILandroid/os/Looper;)V").newInstance(new Object[]{Integer.valueOf(this.mExtTexture.getId()), sFrameListener.getLooper()});
                    Log.i("Camera/SurfaceTextureScreenNail", "fullHandlerCapacity:set urgent display");
                    Process.setThreadPriority(sFrameListener.getThreadId(), -8);
                    this.currentFrameCount = 0;
                } catch (NoSuchMethodException e) {
                    Log.e("Camera/SurfaceTextureScreenNail", "SurfaceTexture Constructor NoSuchMethodException");
                } catch (IllegalArgumentException e2) {
                    Log.e("Camera/SurfaceTextureScreenNail", "SurfaceTexture Constructor IllegalArgumentException");
                }
            }
        }
        if (this.mSurfaceTexture == null) {
            this.mSurfaceTexture = new SurfaceTexture(this.mExtTexture.getId());
        }
        this.mSurfaceTexture.setDefaultBufferSize(this.mWidth, this.mHeight);
        if (VERSION.SDK_INT < 21 || !Device.isSubthreadFrameListerner()) {
            this.mSurfaceTexture.setOnFrameAvailableListener(this);
            return;
        }
        try {
            Method.of(SurfaceTexture.class, "setOnFrameAvailableListener", "(Landroid/graphics/SurfaceTexture$OnFrameAvailableListener;Landroid/os/Handler;)V").invoke(SurfaceTexture.class, this.mSurfaceTexture, new Object[]{this, new Handler(sFrameListener.getLooper())});
        } catch (NoSuchMethodException e3) {
            Log.e("Camera/SurfaceTextureScreenNail", "SurfaceTexture setOnFrameAvailableListener NoSuchMethodException");
        } catch (IllegalArgumentException e4) {
            Log.e("Camera/SurfaceTextureScreenNail", "SurfaceTexture setOnFrameAvailableListener IllegalArgumentException");
        }
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
        boolean z = true;
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
            if ((((float) this.mRenderHeight) / ((float) this.mRenderWidth)) - (((float) Util.sWindowHeight) / ((float) Util.sWindowWidth)) >= 0.1f) {
                z = false;
            }
            this.mIsFullScreen = z;
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
        if (!this.mIsFullScreen || Device.is18x9RatioScreen()) {
            draw(canvas, this.mTx, this.mTy, this.mTwidth, this.mTheight);
        } else {
            draw(canvas, 0, 0, this.mSurfaceWidth, this.mSurfaceHeight);
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
