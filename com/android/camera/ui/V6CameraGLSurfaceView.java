package com.android.camera.ui;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.opengl.GLSurfaceView.EGLConfigChooser;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Process;
import android.util.AttributeSet;
import android.util.Log;
import com.android.camera.ActivityBase;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;
import com.android.gallery3d.ui.GLCanvasImpl;
import com.android.gallery3d.ui.UploadedTexture;
import com.android.gallery3d.ui.Utils;
import java.util.concurrent.locks.ReentrantLock;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

public class V6CameraGLSurfaceView extends GLSurfaceView implements Renderer {
    private final ActivityBase mActivity;
    private GLCanvas mCanvas;
    private EGLContext mEGLContext;
    private final MyEGLConfigChooser mEglConfigChooser;
    private int mFrameCount;
    private long mFrameCountingStart;
    private GL11 mGL;
    protected int mHeight;
    private final ReentrantLock mRenderLock;
    private volatile boolean mRenderRequested;
    protected int mWidth;

    private class MyEGLConfigChooser implements EGLConfigChooser {
        private final int[] ATTR_ID;
        private final String[] ATTR_NAME;
        private final boolean USE_RGB888;
        private final int[] mConfigSpec;

        private MyEGLConfigChooser() {
            boolean z;
            int i;
            if (Device.IS_H3C) {
                z = true;
            } else {
                z = Device.IS_D3;
            }
            this.USE_RGB888 = z;
            int[] iArr = new int[11];
            iArr[0] = 12324;
            if (this.USE_RGB888) {
                i = 8;
            } else {
                i = 5;
            }
            iArr[1] = i;
            iArr[2] = 12323;
            iArr[3] = this.USE_RGB888 ? 8 : 6;
            iArr[4] = 12322;
            if (this.USE_RGB888) {
                i = 8;
            } else {
                i = 5;
            }
            iArr[5] = i;
            iArr[6] = 12321;
            iArr[7] = 0;
            iArr[8] = 12352;
            iArr[9] = 4;
            iArr[10] = 12344;
            this.mConfigSpec = iArr;
            this.ATTR_ID = new int[]{12324, 12323, 12322, 12321, 12325, 12326, 12328, 12327};
            this.ATTR_NAME = new String[]{"R", "G", "B", "A", "D", "S", "ID", "CAVEAT"};
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] numConfig = new int[1];
            if (!egl.eglChooseConfig(display, this.mConfigSpec, null, 0, numConfig)) {
                throw new RuntimeException("eglChooseConfig failed");
            } else if (numConfig[0] <= 0) {
                throw new RuntimeException("No configs match configSpec");
            } else {
                EGLConfig[] configs = new EGLConfig[numConfig[0]];
                if (egl.eglChooseConfig(display, this.mConfigSpec, configs, configs.length, numConfig)) {
                    return chooseConfig(egl, display, configs);
                }
                throw new RuntimeException();
            }
        }

        private EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            EGLConfig result = null;
            int minStencil = Integer.MAX_VALUE;
            int[] value = new int[1];
            int n = configs.length;
            for (int i = 0; i < n; i++) {
                if (!egl.eglGetConfigAttrib(display, configs[i], 12324, value) || value[0] != 8) {
                    if (!egl.eglGetConfigAttrib(display, configs[i], 12326, value)) {
                        throw new RuntimeException("eglGetConfigAttrib error: " + egl.eglGetError());
                    } else if (value[0] != 0 && value[0] < minStencil) {
                        minStencil = value[0];
                        result = configs[i];
                    }
                }
            }
            if (result == null) {
                result = configs[0];
            }
            egl.eglGetConfigAttrib(display, result, 12326, value);
            logConfig(egl, display, result);
            return result;
        }

        private void logConfig(EGL10 egl, EGLDisplay display, EGLConfig config) {
            int[] value = new int[1];
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < this.ATTR_ID.length; j++) {
                egl.eglGetConfigAttrib(display, config, this.ATTR_ID[j], value);
                sb.append(this.ATTR_NAME[j]).append(value[0]).append(" ");
            }
            Log.i("GLRootView", "Config chosen: " + sb.toString());
        }
    }

    public V6CameraGLSurfaceView(Context context) {
        this(context, null);
    }

    public V6CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mFrameCount = 0;
        this.mFrameCountingStart = 0;
        this.mRenderRequested = false;
        this.mEglConfigChooser = new MyEGLConfigChooser();
        this.mRenderLock = new ReentrantLock();
        setEGLContextClientVersion(2);
        setEGLConfigChooser(this.mEglConfigChooser);
        setRenderer(this);
        setRenderMode(0);
        getHolder().setFormat(4);
        if (Device.isSurfaceSizeLimited()) {
            getHolder().setFixedSize(720, (Util.sWindowHeight * 720) / Util.sWindowWidth);
        }
        this.mActivity = (ActivityBase) context;
    }

    public void requestRender() {
        if (!this.mRenderRequested) {
            this.mRenderRequested = true;
            super.requestRender();
        }
    }

    public EGLContext getEGLContext() {
        return this.mEGLContext;
    }

    public GLCanvas getGLCanvas() {
        return this.mCanvas;
    }

    public boolean isBusy() {
        return this.mRenderRequested;
    }

    public void onSurfaceCreated(GL10 gl1, EGLConfig config) {
        GL11 gl = (GL11) gl1;
        if (this.mGL != null) {
            Log.i("GLRootView", "GLObject has changed from " + this.mGL + " to " + gl);
        }
        this.mRenderLock.lock();
        try {
            this.mGL = gl;
            BasicTexture.invalidateAllTextures(this.mCanvas);
            this.mCanvas = new GLCanvasImpl();
            setRenderMode(0);
        } finally {
            this.mRenderLock.unlock();
        }
    }

    public void onSurfaceChanged(GL10 gl1, int width, int height) {
        Log.i("GLRootView", "onSurfaceChanged: " + width + "x" + height + ", gl10: " + gl1.toString());
        Process.setThreadPriority(-4);
        Utils.assertTrue(this.mGL == ((GL11) gl1));
        this.mWidth = width;
        this.mHeight = height;
        this.mCanvas.setSize(width, height);
        this.mEGLContext = ((EGL10) EGLContext.getEGL()).eglGetCurrentContext();
    }

    public void onDrawFrame(GL10 gl) {
        this.mCanvas.recycledResources();
        UploadedTexture.resetUploadLimit();
        this.mRenderRequested = false;
        synchronized (this.mCanvas) {
            this.mCanvas.getState().pushState();
            this.mActivity.getCameraScreenNail().draw(this.mCanvas);
            this.mCanvas.getState().popState();
        }
        if (UploadedTexture.uploadLimitReached()) {
            requestRender();
        }
        this.mCanvas.recycledResources();
    }
}
