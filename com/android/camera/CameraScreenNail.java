package com.android.camera;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawBlurTexAttribute;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.gallery3d.ui.BitmapTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.RawTexture;
import java.nio.ByteBuffer;

public class CameraScreenNail extends SurfaceTextureScreenNail {
    private int mAnimState = 0;
    private CaptureAnimManager mCaptureAnimManager = new CaptureAnimManager();
    private boolean mDisableSwitchAnimationOnce;
    private boolean mFirstFrameArrived;
    private Listener mListener;
    private Object mLock = new Object();
    private ModuleAnimManager mModuleAnimManager = new ModuleAnimManager();
    private SwitchAnimManager mSwitchAnimManager = new SwitchAnimManager();
    private final float[] mTextureTransformMatrix = new float[16];
    private boolean mVisible;

    public interface Listener {
        boolean isKeptBitmapTexture();

        void onPreviewPixelsRead(byte[] bArr, int i, int i2);

        void onPreviewTextureCopied();

        void onSwitchAnimationDone();

        void requestRender();
    }

    public CameraScreenNail(Listener listener) {
        this.mListener = listener;
    }

    public void acquireSurfaceTexture() {
        synchronized (this.mLock) {
            this.mFirstFrameArrived = false;
            this.mDisableSwitchAnimationOnce = false;
            super.acquireSurfaceTexture();
        }
    }

    public void releaseSurfaceTexture() {
        synchronized (this.mLock) {
            super.releaseSurfaceTexture();
            this.mAnimState = 0;
            this.mFirstFrameArrived = false;
            this.mModuleSwitching = false;
        }
    }

    public void animateSwitchCopyTexture() {
        synchronized (this.mLock) {
            this.mListener.requestRender();
            this.mAnimState = 3;
        }
    }

    public void animateModuleChangeBefore() {
        synchronized (this.mLock) {
            if (this.mAnimState == 0 || this.mAnimState == 11) {
                this.mListener.requestRender();
                this.mAnimState = 9;
            }
        }
    }

    public void animateModuleChangeAfter() {
        if (this.mModuleSwitching) {
            this.mModuleSwitching = false;
            synchronized (this.mLock) {
                if (this.mAnimState == 11) {
                    this.mListener.requestRender();
                    this.mAnimState = 10;
                }
            }
        }
    }

    public void clearAnimation() {
        this.mSwitchAnimManager.clearAnimation();
        this.mCaptureAnimManager.clearAnimation();
        this.mModuleAnimManager.clearAnimation();
    }

    public void animateSwitchCameraBefore() {
        synchronized (this.mLock) {
            if (this.mAnimState == 4) {
                this.mAnimState = 7;
                this.mSwitchAnimManager.startAnimation();
                this.mListener.requestRender();
            }
        }
    }

    public void switchCameraDone() {
        synchronized (this.mLock) {
            if (this.mAnimState == 7) {
                this.mAnimState = 5;
            }
        }
    }

    public void animateCapture(int animOrientation) {
        synchronized (this.mLock) {
            if (this.mAnimState == 0) {
                this.mCaptureAnimManager.animateHoldAndSlide();
                this.mListener.requestRender();
                this.mAnimState = 1;
            }
        }
    }

    public void requestHibernate() {
        synchronized (this.mLock) {
            if (this.mAnimState == 0) {
                this.mAnimState = 12;
                this.mListener.requestRender();
            }
        }
    }

    public void requestAwaken() {
        synchronized (this.mLock) {
            if (this.mAnimState == 12) {
                this.mAnimState = 0;
                this.mFirstFrameArrived = false;
            }
        }
    }

    public void requestReadPixels() {
        synchronized (this.mLock) {
            if (this.mAnimState == 0) {
                this.mAnimState = 13;
                this.mListener.requestRender();
            }
        }
    }

    public void switchModule() {
        this.mModuleSwitching = true;
    }

    public boolean isModuleSwitching() {
        return this.mModuleSwitching;
    }

    public void animateHold(int displayRotation) {
        synchronized (this.mLock) {
            if (this.mAnimState == 0) {
                this.mCaptureAnimManager.animateHold();
                this.mListener.requestRender();
                this.mAnimState = 1;
            }
        }
    }

    public void animateSlide() {
        synchronized (this.mLock) {
            if (this.mAnimState != 2) {
                Log.v("CameraScreenNail", "Cannot animateSlide outside of animateCapture! Animation state = " + this.mAnimState);
            }
            this.mCaptureAnimManager.animateSlide();
            this.mListener.requestRender();
        }
    }

    public void directDraw(GLCanvas canvas, int x, int y, int width, int height) {
        super.draw(canvas, x, y, width, height);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void draw(com.android.gallery3d.ui.GLCanvas r21, int r22, int r23, int r24, int r25) {
        /*
        r20 = this;
        r0 = r20;
        r0 = r0.mLock;
        r19 = r0;
        monitor-enter(r19);
        r0 = r20;
        r4 = r0.mVisible;	 Catch:{ all -> 0x0084 }
        if (r4 != 0) goto L_0x0012;
    L_0x000d:
        r4 = 1;
        r0 = r20;
        r0.mVisible = r4;	 Catch:{ all -> 0x0084 }
    L_0x0012:
        r0 = r20;
        r4 = r0.mBitmapTexture;	 Catch:{ all -> 0x0084 }
        if (r4 == 0) goto L_0x002b;
    L_0x0018:
        r0 = r20;
        r4 = r0.mBitmapTexture;	 Catch:{ all -> 0x0084 }
        r5 = r21;
        r6 = r22;
        r7 = r23;
        r8 = r24;
        r9 = r25;
        r4.draw(r5, r6, r7, r8, r9);	 Catch:{ all -> 0x0084 }
        monitor-exit(r19);
        return;
    L_0x002b:
        r16 = r20.getSurfaceTexture();	 Catch:{ all -> 0x0084 }
        if (r16 == 0) goto L_0x007e;
    L_0x0031:
        r0 = r20;
        r4 = r0.mFirstFrameArrived;	 Catch:{ all -> 0x0084 }
        if (r4 == 0) goto L_0x007e;
    L_0x0037:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        switch(r4) {
            case 0: goto L_0x0080;
            case 1: goto L_0x0102;
            case 2: goto L_0x003e;
            case 3: goto L_0x00ca;
            case 4: goto L_0x00e8;
            case 5: goto L_0x003e;
            case 6: goto L_0x003e;
            case 7: goto L_0x003e;
            case 8: goto L_0x003e;
            case 9: goto L_0x011e;
            case 10: goto L_0x012d;
            case 11: goto L_0x003e;
            case 12: goto L_0x0087;
            case 13: goto L_0x008e;
            default: goto L_0x003e;
        };	 Catch:{ all -> 0x0084 }
    L_0x003e:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 7;
        if (r4 == r5) goto L_0x004c;
    L_0x0045:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 5;
        if (r4 != r5) goto L_0x013c;
    L_0x004c:
        r16.updateTexImage();	 Catch:{ all -> 0x0084 }
        r12 = 0;
        r0 = r20;
        r4 = r0.mDisableSwitchAnimationOnce;	 Catch:{ all -> 0x0084 }
        if (r4 == 0) goto L_0x0166;
    L_0x0056:
        r0 = r20;
        r4 = r0.mSwitchAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r10 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r5 = r21;
        r6 = r22;
        r7 = r23;
        r8 = r24;
        r9 = r25;
        r4.drawPreview(r5, r6, r7, r8, r9, r10);	 Catch:{ all -> 0x0084 }
    L_0x006b:
        if (r12 != 0) goto L_0x0075;
    L_0x006d:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 8;
        if (r4 == r5) goto L_0x0180;
    L_0x0075:
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.requestRender();	 Catch:{ all -> 0x0084 }
    L_0x007c:
        monitor-exit(r19);
        return;
    L_0x007e:
        monitor-exit(r19);
        return;
    L_0x0080:
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x0084:
        r4 = move-exception;
        monitor-exit(r19);
        throw r4;
    L_0x0087:
        r16.updateTexImage();	 Catch:{ all -> 0x0084 }
        r21.clearBuffer();	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x008e:
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r18 = r4.getWidth();	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r17 = r4.getHeight();	 Catch:{ all -> 0x0084 }
        r4 = r24 * r17;
        r5 = r25 * r18;
        if (r4 <= r5) goto L_0x00c3;
    L_0x00a7:
        r15 = r18;
        r4 = r25 * r18;
        r14 = r4 / r24;
    L_0x00ad:
        r0 = r20;
        r1 = r21;
        r13 = r0.readPreviewPixels(r1, r15, r14);	 Catch:{ all -> 0x0084 }
        r4 = 0;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.onPreviewPixelsRead(r13, r15, r14);	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x00c3:
        r4 = r24 * r17;
        r15 = r4 / r25;
        r14 = r17;
        goto L_0x00ad;
    L_0x00ca:
        r20.copyPreviewTexture(r21);	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mSwitchAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r22;
        r1 = r23;
        r2 = r24;
        r3 = r25;
        r4.setReviewDrawingSize(r0, r1, r2, r3);	 Catch:{ all -> 0x0084 }
        r4 = 4;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.onPreviewTextureCopied();	 Catch:{ all -> 0x0084 }
    L_0x00e8:
        r16.updateTexImage();	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mSwitchAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r10 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r5 = r21;
        r6 = r22;
        r7 = r23;
        r8 = r24;
        r9 = r25;
        r4.drawPreview(r5, r6, r7, r8, r9, r10);	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x0102:
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        r20.copyPreviewTexture(r21);	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mCaptureAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r22;
        r1 = r23;
        r2 = r24;
        r3 = r25;
        r4.startAnimation(r0, r1, r2, r3);	 Catch:{ all -> 0x0084 }
        r4 = 2;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x011e:
        r0 = r20;
        r4 = r0.mModuleAnimManager;	 Catch:{ all -> 0x0084 }
        r4.animateStartHide();	 Catch:{ all -> 0x0084 }
        r4 = 11;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x012d:
        r0 = r20;
        r4 = r0.mModuleAnimManager;	 Catch:{ all -> 0x0084 }
        r4.animateStartShow();	 Catch:{ all -> 0x0084 }
        r4 = 11;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        goto L_0x003e;
    L_0x013c:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 8;
        if (r4 == r5) goto L_0x004c;
    L_0x0144:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 2;
        if (r4 != r5) goto L_0x01a0;
    L_0x014b:
        r0 = r20;
        r4 = r0.mCaptureAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r5 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r0 = r21;
        r1 = r20;
        r12 = r4.drawAnimation(r0, r1, r5);	 Catch:{ all -> 0x0084 }
        if (r12 == 0) goto L_0x0196;
    L_0x015d:
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.requestRender();	 Catch:{ all -> 0x0084 }
        goto L_0x007c;
    L_0x0166:
        r0 = r20;
        r4 = r0.mSwitchAnimManager;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r11 = r0.mAnimTexture;	 Catch:{ all -> 0x0084 }
        r5 = r21;
        r6 = r22;
        r7 = r23;
        r8 = r24;
        r9 = r25;
        r10 = r20;
        r12 = r4.drawAnimation(r5, r6, r7, r8, r9, r10, r11);	 Catch:{ all -> 0x0084 }
        goto L_0x006b;
    L_0x0180:
        r4 = 0;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        r4 = 0;
        r0 = r20;
        r0.mDisableSwitchAnimationOnce = r4;	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.onSwitchAnimationDone();	 Catch:{ all -> 0x0084 }
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        goto L_0x007c;
    L_0x0196:
        r4 = 0;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        goto L_0x007c;
    L_0x01a0:
        r0 = r20;
        r4 = r0.mAnimState;	 Catch:{ all -> 0x0084 }
        r5 = 11;
        if (r4 != r5) goto L_0x007c;
    L_0x01a8:
        super.draw(r21, r22, r23, r24, r25);	 Catch:{ all -> 0x0084 }
        r0 = r20;
        r4 = r0.mModuleAnimManager;	 Catch:{ all -> 0x0084 }
        r5 = r21;
        r6 = r22;
        r7 = r23;
        r8 = r24;
        r9 = r25;
        r12 = r4.drawAnimation(r5, r6, r7, r8, r9);	 Catch:{ all -> 0x0084 }
        if (r12 == 0) goto L_0x01c8;
    L_0x01bf:
        r0 = r20;
        r4 = r0.mListener;	 Catch:{ all -> 0x0084 }
        r4.requestRender();	 Catch:{ all -> 0x0084 }
        goto L_0x007c;
    L_0x01c8:
        r0 = r20;
        r4 = r0.mModuleSwitching;	 Catch:{ all -> 0x0084 }
        if (r4 != 0) goto L_0x007c;
    L_0x01ce:
        r4 = 0;
        r0 = r20;
        r0.mAnimState = r4;	 Catch:{ all -> 0x0084 }
        goto L_0x007c;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.CameraScreenNail.draw(com.android.gallery3d.ui.GLCanvas, int, int, int, int):void");
    }

    private byte[] readPreviewPixels(GLCanvas canvas, int width, int height) {
        ByteBuffer buffer = ByteBuffer.allocate((width * height) * 4);
        getSurfaceTexture().getTransformMatrix(this.mTextureTransformMatrix);
        updateTransformMatrix(this.mTextureTransformMatrix);
        if (this.mFrameBuffer == null) {
            this.mFrameBuffer = new FrameBuffer(canvas, this.mAnimTexture, 0);
        }
        canvas.beginBindFrameBuffer(this.mFrameBuffer);
        canvas.draw(new DrawExtTexAttribute(this.mExtTexture, this.mTextureTransformMatrix, 0, 0, width, height));
        GLES20.glReadPixels(0, 0, width, height, 6408, 5121, buffer);
        canvas.endBindFrameBuffer();
        return buffer.array();
    }

    private void copyPreviewTexture(GLCanvas canvas) {
        copyTexture(canvas, this.mAnimTexture);
    }

    private void copyTexture(GLCanvas canvas, RawTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        getSurfaceTexture().getTransformMatrix(this.mTextureTransformMatrix);
        updateTransformMatrix(this.mTextureTransformMatrix);
        if (this.mFrameBuffer == null) {
            this.mFrameBuffer = new FrameBuffer(canvas, texture, 0);
        }
        canvas.beginBindFrameBuffer(this.mFrameBuffer);
        canvas.draw(new DrawExtTexAttribute(this.mExtTexture, this.mTextureTransformMatrix, 0, 0, width, height));
        canvas.endBindFrameBuffer();
    }

    public void drawBlurTexture(GLCanvas canvas, int x, int y, int width, int height) {
        canvas.draw(new DrawBasicTexAttribute(this.mAnimTexture, x, y, width, height));
    }

    public void renderBlurTexture(GLCanvas canvas) {
        renderBlurTexture(canvas, this.mAnimTexture);
    }

    private void renderBlurTexture(GLCanvas canvas, RawTexture texture) {
        int width = texture.getWidth();
        int height = texture.getHeight();
        if (this.mFrameBuffer == null) {
            this.mFrameBuffer = new FrameBuffer(canvas, texture, 0);
        }
        canvas.prepareBlurRenders();
        canvas.beginBindFrameBuffer(this.mFrameBuffer);
        canvas.draw(new DrawBlurTexAttribute(texture, 0, 0, width, height));
        canvas.endBindFrameBuffer();
    }

    public void renderBitmapToCanvas(Bitmap bitmap) {
        this.mVisible = false;
        this.mBitmapTexture = new BitmapTexture(bitmap);
        this.mListener.requestRender();
    }

    public void releaseBitmapIfNeeded() {
        if (this.mBitmapTexture != null && !this.mListener.isKeptBitmapTexture()) {
            this.mBitmapTexture = null;
            this.mListener.requestRender();
        }
    }

    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        if (getSurfaceTexture() == surfaceTexture) {
            synchronized (this.mLock) {
                if (!this.mFirstFrameArrived) {
                    this.mVisible = true;
                }
                this.mFirstFrameArrived = true;
                if (this.mVisible) {
                    if (this.mAnimState == 5) {
                        this.mAnimState = 8;
                        this.mSwitchAnimManager.restartPreview();
                        this.mSwitchAnimManager.startResume();
                    }
                    this.mListener.requestRender();
                }
            }
        }
    }

    public void setPreviewFrameLayoutSize(int width, int height) {
        int i = 720;
        synchronized (this.mLock) {
            if (!Device.isSurfaceSizeLimited()) {
                i = width;
            }
            this.mSurfaceWidth = i;
            if (Device.isSurfaceSizeLimited()) {
                height = (height * 720) / width;
            }
            this.mSurfaceHeight = height;
            this.mSwitchAnimManager.setPreviewFrameLayoutSize(this.mSurfaceWidth, this.mSurfaceHeight);
            updateRenderRect();
            if (this.mModuleSwitching) {
                this.mFirstFrameArrived = false;
            }
        }
    }

    protected void updateExtraTransformMatrix(float[] matrix) {
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        if (!(this.mAnimState == 7 || this.mAnimState == 5)) {
            if (this.mAnimState == 8) {
            }
            if (scaleX == 1.0f || scaleY != 1.0f) {
                Matrix.translateM(matrix, 0, 0.5f, 0.5f, 0.0f);
                Matrix.scaleM(matrix, 0, scaleX, scaleY, 1.0f);
                Matrix.translateM(matrix, 0, -0.5f, -0.5f, 0.0f);
            }
            return;
        }
        scaleX = this.mSwitchAnimManager.getExtScaleX();
        scaleY = this.mSwitchAnimManager.getExtScaleY();
        if (scaleX == 1.0f) {
        }
        Matrix.translateM(matrix, 0, 0.5f, 0.5f, 0.0f);
        Matrix.scaleM(matrix, 0, scaleX, scaleY, 1.0f);
        Matrix.translateM(matrix, 0, -0.5f, -0.5f, 0.0f);
    }

    public Rect getRenderRect() {
        return this.mRenderLayoutRect;
    }
}
