package com.android.camera.ui;

import android.graphics.SurfaceTexture;
import android.opengl.GLDebugHelper;
import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;
import java.io.Writer;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;
import javax.microedition.khronos.opengles.GL;
import javax.microedition.khronos.opengles.GL10;

public class GLSurfaceTexture extends SurfaceTexture {
    private static final GLThreadManager sGLThreadManager = new GLThreadManager();
    private int mDebugFlags;
    private EGLConfigChooser mEGLConfigChooser;
    private int mEGLContextClientVersion;
    private EGLContextFactory mEGLContextFactory;
    private EGLWindowSurfaceFactory mEGLWindowSurfaceFactory;
    private GLThread mGLThread;
    private GLWrapper mGLWrapper;
    private boolean mPreserveEGLContextOnPause;
    private int mPreservedHeight;
    private int mPreservedWidth;
    private Renderer mRenderer;
    private EGLContext mShareContext;
    private boolean mStopped;
    private final WeakReference<GLSurfaceTexture> mThisWeakRef = new WeakReference(this);

    public interface EGLConfigChooser {
        EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eGLDisplay);
    }

    private abstract class BaseConfigChooser implements EGLConfigChooser {
        protected int[] mConfigSpec;

        abstract EGLConfig chooseConfig(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig[] eGLConfigArr);

        public BaseConfigChooser(int[] configSpec) {
            this.mConfigSpec = filterConfigSpec(configSpec);
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
            int[] num_config = new int[1];
            if (egl.eglChooseConfig(display, this.mConfigSpec, null, 0, num_config)) {
                int numConfigs = num_config[0];
                if (numConfigs <= 0) {
                    throw new IllegalArgumentException("No configs match configSpec");
                }
                EGLConfig[] configs = new EGLConfig[numConfigs];
                if (egl.eglChooseConfig(display, this.mConfigSpec, configs, numConfigs, num_config)) {
                    EGLConfig config = chooseConfig(egl, display, configs);
                    if (config != null) {
                        return config;
                    }
                    throw new IllegalArgumentException("No config chosen");
                }
                throw new IllegalArgumentException("eglChooseConfig#2 failed");
            }
            throw new IllegalArgumentException("eglChooseConfig failed");
        }

        private int[] filterConfigSpec(int[] configSpec) {
            if (GLSurfaceTexture.this.mEGLContextClientVersion != 2) {
                return configSpec;
            }
            int len = configSpec.length;
            int[] newConfigSpec = new int[(len + 2)];
            System.arraycopy(configSpec, 0, newConfigSpec, 0, len - 1);
            newConfigSpec[len - 1] = 12352;
            newConfigSpec[len] = 4;
            newConfigSpec[len + 1] = 12344;
            return newConfigSpec;
        }
    }

    private class ComponentSizeChooser extends BaseConfigChooser {
        protected int mAlphaSize;
        protected int mBlueSize;
        protected int mDepthSize;
        protected int mGreenSize;
        protected int mRedSize;
        protected int mStencilSize;
        private int[] mValue = new int[1];

        public ComponentSizeChooser(int redSize, int greenSize, int blueSize, int alphaSize, int depthSize, int stencilSize) {
            super(new int[]{12324, redSize, 12323, greenSize, 12322, blueSize, 12321, alphaSize, 12325, depthSize, 12326, stencilSize, 12344});
            this.mRedSize = redSize;
            this.mGreenSize = greenSize;
            this.mBlueSize = blueSize;
            this.mAlphaSize = alphaSize;
            this.mDepthSize = depthSize;
            this.mStencilSize = stencilSize;
        }

        public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display, EGLConfig[] configs) {
            for (EGLConfig config : configs) {
                int d = findConfigAttrib(egl, display, config, 12325, 0);
                int s = findConfigAttrib(egl, display, config, 12326, 0);
                if (d >= this.mDepthSize && s >= this.mStencilSize) {
                    int r = findConfigAttrib(egl, display, config, 12324, 0);
                    int g = findConfigAttrib(egl, display, config, 12323, 0);
                    int b = findConfigAttrib(egl, display, config, 12322, 0);
                    int a = findConfigAttrib(egl, display, config, 12321, 0);
                    if (r == this.mRedSize && g == this.mGreenSize && b == this.mBlueSize && a == this.mAlphaSize) {
                        return config;
                    }
                }
            }
            return null;
        }

        private int findConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute, int defaultValue) {
            if (egl.eglGetConfigAttrib(display, config, attribute, this.mValue)) {
                return this.mValue[0];
            }
            return defaultValue;
        }
    }

    public interface EGLContextFactory {
        EGLContext createContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig, EGLContext eGLContext);

        void destroyContext(EGL10 egl10, EGLDisplay eGLDisplay, EGLContext eGLContext);
    }

    private class DefaultContextFactory implements EGLContextFactory {
        private int EGL_CONTEXT_CLIENT_VERSION;

        private DefaultContextFactory() {
            this.EGL_CONTEXT_CLIENT_VERSION = 12440;
        }

        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig config, EGLContext shareContext) {
            int[] attrib_list = new int[]{this.EGL_CONTEXT_CLIENT_VERSION, GLSurfaceTexture.this.mEGLContextClientVersion, 12344};
            if (shareContext == null) {
                shareContext = EGL10.EGL_NO_CONTEXT;
            }
            if (GLSurfaceTexture.this.mEGLContextClientVersion == 0) {
                attrib_list = null;
            }
            return egl.eglCreateContext(display, config, shareContext, attrib_list);
        }

        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
            if (!egl.eglDestroyContext(display, context)) {
                Log.e("DefaultContextFactory", "display:" + display + " context: " + context);
                EglHelper.throwEglException("eglDestroyContex", egl.eglGetError());
            }
        }
    }

    public interface EGLWindowSurfaceFactory {
        EGLSurface createWindowSurface(EGL10 egl10, EGLDisplay eGLDisplay, EGLConfig eGLConfig, Object obj);

        void destroySurface(EGL10 egl10, EGLDisplay eGLDisplay, EGLSurface eGLSurface);
    }

    private static class DefaultWindowSurfaceFactory implements EGLWindowSurfaceFactory {
        private DefaultWindowSurfaceFactory() {
        }

        public EGLSurface createWindowSurface(EGL10 egl, EGLDisplay display, EGLConfig config, Object nativeWindow) {
            EGLSurface result = null;
            try {
                result = egl.eglCreateWindowSurface(display, config, nativeWindow, null);
            } catch (IllegalArgumentException e) {
                Log.e("GLSurfaceTexture", "eglCreateWindowSurface", e);
            }
            return result;
        }

        public void destroySurface(EGL10 egl, EGLDisplay display, EGLSurface surface) {
            egl.eglDestroySurface(display, surface);
        }
    }

    private static class EglHelper {
        EGL10 mEgl;
        EGLConfig mEglConfig;
        EGLContext mEglContext;
        EGLDisplay mEglDisplay;
        EGLSurface mEglSurface;
        private WeakReference<GLSurfaceTexture> mGLSurfaceTextureWeakRef;

        public EglHelper(WeakReference<GLSurfaceTexture> glSurfaceTextureWeakRef) {
            this.mGLSurfaceTextureWeakRef = glSurfaceTextureWeakRef;
        }

        public void start() {
            this.mEgl = (EGL10) EGLContext.getEGL();
            this.mEglDisplay = this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            if (this.mEgl.eglInitialize(this.mEglDisplay, new int[2])) {
                GLSurfaceTexture surface = (GLSurfaceTexture) this.mGLSurfaceTextureWeakRef.get();
                if (surface == null) {
                    this.mEglConfig = null;
                    this.mEglContext = null;
                } else {
                    this.mEglConfig = surface.mEGLConfigChooser.chooseConfig(this.mEgl, this.mEglDisplay);
                    this.mEglContext = surface.mEGLContextFactory.createContext(this.mEgl, this.mEglDisplay, this.mEglConfig, surface.mShareContext);
                }
                if (this.mEglContext == null || this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                    this.mEglContext = null;
                    throwEglException("createContext");
                }
                this.mEglSurface = null;
                return;
            }
            throw new RuntimeException("eglInitialize failed");
        }

        public boolean createSurface() {
            if (this.mEgl == null) {
                throw new RuntimeException("egl not initialized");
            } else if (this.mEglDisplay == null) {
                throw new RuntimeException("eglDisplay not initialized");
            } else if (this.mEglConfig == null) {
                throw new RuntimeException("mEglConfig not initialized");
            } else {
                destroySurfaceImp();
                GLSurfaceTexture surface = (GLSurfaceTexture) this.mGLSurfaceTextureWeakRef.get();
                if (surface != null) {
                    this.mEglSurface = surface.mEGLWindowSurfaceFactory.createWindowSurface(this.mEgl, this.mEglDisplay, this.mEglConfig, surface);
                } else {
                    this.mEglSurface = null;
                }
                if (this.mEglSurface == null || this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                    if (this.mEgl.eglGetError() == 12299) {
                        Log.e("EglHelper", "createWindowSurface returned EGL_BAD_NATIVE_WINDOW.");
                    }
                    return false;
                } else if (this.mEgl.eglMakeCurrent(this.mEglDisplay, this.mEglSurface, this.mEglSurface, this.mEglContext)) {
                    return true;
                } else {
                    logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                    return false;
                }
            }
        }

        GL createGL() {
            GL gl = this.mEglContext.getGL();
            GLSurfaceTexture surface = (GLSurfaceTexture) this.mGLSurfaceTextureWeakRef.get();
            if (surface == null) {
                return gl;
            }
            if (surface.mGLWrapper != null) {
                gl = surface.mGLWrapper.wrap(gl);
            }
            if ((surface.mDebugFlags & 3) == 0) {
                return gl;
            }
            int configFlags = 0;
            Writer writer = null;
            if ((surface.mDebugFlags & 1) != 0) {
                configFlags = 1;
            }
            if ((surface.mDebugFlags & 2) != 0) {
                writer = new LogWriter();
            }
            return GLDebugHelper.wrap(gl, configFlags, writer);
        }

        public int swap() {
            if (this.mEgl.eglSwapBuffers(this.mEglDisplay, this.mEglSurface)) {
                return 12288;
            }
            return this.mEgl.eglGetError();
        }

        public void destroySurface() {
            destroySurfaceImp();
        }

        private void destroySurfaceImp() {
            if (this.mEglSurface != null && this.mEglSurface != EGL10.EGL_NO_SURFACE) {
                if (!this.mEgl.eglMakeCurrent(this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)) {
                    logEglErrorAsWarning("EGLHelper", "eglMakeCurrent", this.mEgl.eglGetError());
                }
                GLSurfaceTexture surface = (GLSurfaceTexture) this.mGLSurfaceTextureWeakRef.get();
                if (surface != null) {
                    surface.mEGLWindowSurfaceFactory.destroySurface(this.mEgl, this.mEglDisplay, this.mEglSurface);
                }
                this.mEglSurface = null;
            }
        }

        public void finish() {
            if (this.mEglContext != null) {
                GLSurfaceTexture surface = (GLSurfaceTexture) this.mGLSurfaceTextureWeakRef.get();
                if (surface != null) {
                    surface.mEGLContextFactory.destroyContext(this.mEgl, this.mEglDisplay, this.mEglContext);
                }
                this.mEglContext = null;
            }
            if (this.mEglDisplay != null) {
                this.mEgl.eglTerminate(this.mEglDisplay);
                this.mEglDisplay = null;
            }
        }

        private void throwEglException(String function) {
            throwEglException(function, this.mEgl.eglGetError());
        }

        public static void throwEglException(String function, int error) {
            throw new RuntimeException(formatEglError(function, error));
        }

        public static void logEglErrorAsWarning(String tag, String function, int error) {
            Log.w(tag, formatEglError(function, error));
        }

        public static String formatEglError(String function, int error) {
            return function + " failed";
        }
    }

    static class GLThread extends Thread {
        private EglHelper mEglHelper;
        private ArrayList<Runnable> mEventQueue = new ArrayList();
        private boolean mExited;
        private boolean mFinishedCreatingEglSurface;
        private WeakReference<GLSurfaceTexture> mGLSurfaceTextureWeakRef;
        private boolean mHasSurface;
        private boolean mHaveEglContext;
        private boolean mHaveEglSurface;
        private int mHeight = 0;
        private boolean mPaused;
        private boolean mRenderComplete;
        private int mRenderMode = 1;
        private boolean mRequestPaused;
        private boolean mRequestRender = true;
        private boolean mShouldExit;
        private boolean mShouldReleaseEglContext;
        private boolean mSizeChanged = true;
        private boolean mSurfaceIsBad;
        private boolean mWaitingForSurface;
        private int mWidth = 0;

        GLThread(WeakReference<GLSurfaceTexture> glSurfaceTextureWeakRef) {
            this.mGLSurfaceTextureWeakRef = glSurfaceTextureWeakRef;
        }

        public void run() {
            setName("GLThread " + getId());
            try {
                guardedRun();
            } catch (InterruptedException e) {
            } finally {
                GLSurfaceTexture.sGLThreadManager.threadExiting(this);
            }
        }

        private void stopEglSurfaceLocked() {
            if (this.mHaveEglSurface) {
                this.mHaveEglSurface = false;
                this.mEglHelper.destroySurface();
            }
        }

        private void stopEglContextLocked() {
            if (this.mHaveEglContext) {
                this.mEglHelper.finish();
                this.mHaveEglContext = false;
                GLSurfaceTexture.sGLThreadManager.releaseEglContextLocked(this);
            }
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void guardedRun() throws java.lang.InterruptedException {
            /*
            r22 = this;
            r19 = new com.android.camera.ui.GLSurfaceTexture$EglHelper;
            r0 = r22;
            r0 = r0.mGLSurfaceTextureWeakRef;
            r20 = r0;
            r19.<init>(r20);
            r0 = r19;
            r1 = r22;
            r1.mEglHelper = r0;
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mHaveEglContext = r0;
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mHaveEglSurface = r0;
            r8 = 0;
            r3 = 0;
            r4 = 0;
            r5 = 0;
            r10 = 0;
            r13 = 0;
            r18 = 0;
            r6 = 0;
            r2 = 0;
            r17 = 0;
            r9 = 0;
            r7 = 0;
        L_0x002f:
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a8 }
            monitor-enter(r20);	 Catch:{ all -> 0x01a8 }
        L_0x0034:
            r0 = r22;
            r0 = r0.mShouldExit;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x004d;
        L_0x003c:
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;
            monitor-enter(r20);
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x004a }
            r22.stopEglContextLocked();	 Catch:{ all -> 0x004a }
            monitor-exit(r20);
            return;
        L_0x004a:
            r19 = move-exception;
            monitor-exit(r20);
            throw r19;
        L_0x004d:
            r0 = r22;
            r0 = r0.mEventQueue;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r19 = r19.isEmpty();	 Catch:{ all -> 0x01a5 }
            if (r19 != 0) goto L_0x0076;
        L_0x0059:
            r0 = r22;
            r0 = r0.mEventQueue;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r21 = 0;
            r0 = r19;
            r1 = r21;
            r19 = r0.remove(r1);	 Catch:{ all -> 0x01a5 }
            r0 = r19;
            r0 = (java.lang.Runnable) r0;	 Catch:{ all -> 0x01a5 }
            r7 = r0;
        L_0x006e:
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            if (r7 == 0) goto L_0x022c;
        L_0x0071:
            r7.run();	 Catch:{ all -> 0x01a8 }
            r7 = 0;
            goto L_0x002f;
        L_0x0076:
            r11 = 0;
            r0 = r22;
            r0 = r0.mPaused;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r0 = r22;
            r0 = r0.mRequestPaused;	 Catch:{ all -> 0x01a5 }
            r21 = r0;
            r0 = r19;
            r1 = r21;
            if (r0 == r1) goto L_0x00a0;
        L_0x0089:
            r0 = r22;
            r11 = r0.mRequestPaused;	 Catch:{ all -> 0x01a5 }
            r0 = r22;
            r0 = r0.mRequestPaused;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r0 = r19;
            r1 = r22;
            r1.mPaused = r0;	 Catch:{ all -> 0x01a5 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
        L_0x00a0:
            r0 = r22;
            r0 = r0.mShouldReleaseEglContext;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x00b7;
        L_0x00a8:
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x01a5 }
            r22.stopEglContextLocked();	 Catch:{ all -> 0x01a5 }
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mShouldReleaseEglContext = r0;	 Catch:{ all -> 0x01a5 }
            r2 = 1;
        L_0x00b7:
            if (r10 == 0) goto L_0x00c0;
        L_0x00b9:
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x01a5 }
            r22.stopEglContextLocked();	 Catch:{ all -> 0x01a5 }
            r10 = 0;
        L_0x00c0:
            if (r11 == 0) goto L_0x00cd;
        L_0x00c2:
            r0 = r22;
            r0 = r0.mHaveEglSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x00cd;
        L_0x00ca:
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x01a5 }
        L_0x00cd:
            if (r11 == 0) goto L_0x00f5;
        L_0x00cf:
            r0 = r22;
            r0 = r0.mHaveEglContext;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x00f5;
        L_0x00d7:
            r0 = r22;
            r0 = r0.mGLSurfaceTextureWeakRef;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r14 = r19.get();	 Catch:{ all -> 0x01a5 }
            r14 = (com.android.camera.ui.GLSurfaceTexture) r14;	 Catch:{ all -> 0x01a5 }
            if (r14 != 0) goto L_0x01b6;
        L_0x00e5:
            r12 = 0;
        L_0x00e6:
            if (r12 == 0) goto L_0x00f2;
        L_0x00e8:
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19 = r19.shouldReleaseEGLContextWhenPausing();	 Catch:{ all -> 0x01a5 }
            if (r19 == 0) goto L_0x00f5;
        L_0x00f2:
            r22.stopEglContextLocked();	 Catch:{ all -> 0x01a5 }
        L_0x00f5:
            if (r11 == 0) goto L_0x010a;
        L_0x00f7:
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19 = r19.shouldTerminateEGLWhenPausing();	 Catch:{ all -> 0x01a5 }
            if (r19 == 0) goto L_0x010a;
        L_0x0101:
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            r19.finish();	 Catch:{ all -> 0x01a5 }
        L_0x010a:
            r0 = r22;
            r0 = r0.mHasSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 != 0) goto L_0x011a;
        L_0x0112:
            r0 = r22;
            r0 = r0.mWaitingForSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x01bc;
        L_0x011a:
            r0 = r22;
            r0 = r0.mHasSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x0139;
        L_0x0122:
            r0 = r22;
            r0 = r0.mWaitingForSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x0139;
        L_0x012a:
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mWaitingForSurface = r0;	 Catch:{ all -> 0x01a5 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
        L_0x0139:
            if (r6 == 0) goto L_0x014d;
        L_0x013b:
            r18 = 0;
            r6 = 0;
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mRenderComplete = r0;	 Catch:{ all -> 0x01a5 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
        L_0x014d:
            r19 = r22.readyToDraw();	 Catch:{ all -> 0x01a5 }
            if (r19 == 0) goto L_0x0223;
        L_0x0153:
            r0 = r22;
            r0 = r0.mHaveEglContext;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 != 0) goto L_0x015e;
        L_0x015b:
            if (r2 == 0) goto L_0x01e0;
        L_0x015d:
            r2 = 0;
        L_0x015e:
            r0 = r22;
            r0 = r0.mHaveEglContext;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x016e;
        L_0x0166:
            r0 = r22;
            r0 = r0.mHaveEglSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x0216;
        L_0x016e:
            r0 = r22;
            r0 = r0.mHaveEglSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x0223;
        L_0x0176:
            r0 = r22;
            r0 = r0.mSizeChanged;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x0194;
        L_0x017e:
            r13 = 1;
            r0 = r22;
            r0 = r0.mWidth;	 Catch:{ all -> 0x01a5 }
            r17 = r0;
            r0 = r22;
            r9 = r0.mHeight;	 Catch:{ all -> 0x01a5 }
            r18 = 1;
            r4 = 1;
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mSizeChanged = r0;	 Catch:{ all -> 0x01a5 }
        L_0x0194:
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mRequestRender = r0;	 Catch:{ all -> 0x01a5 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
            goto L_0x006e;
        L_0x01a5:
            r19 = move-exception;
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            throw r19;	 Catch:{ all -> 0x01a8 }
        L_0x01a8:
            r19 = move-exception;
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;
            monitor-enter(r20);
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x0324 }
            r22.stopEglContextLocked();	 Catch:{ all -> 0x0324 }
            monitor-exit(r20);
            throw r19;
        L_0x01b6:
            r12 = r14.mPreserveEGLContextOnPause;	 Catch:{ all -> 0x01a5 }
            goto L_0x00e6;
        L_0x01bc:
            r0 = r22;
            r0 = r0.mHaveEglSurface;	 Catch:{ all -> 0x01a5 }
            r19 = r0;
            if (r19 == 0) goto L_0x01c7;
        L_0x01c4:
            r22.stopEglSurfaceLocked();	 Catch:{ all -> 0x01a5 }
        L_0x01c7:
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mWaitingForSurface = r0;	 Catch:{ all -> 0x01a5 }
            r19 = 0;
            r0 = r19;
            r1 = r22;
            r1.mSurfaceIsBad = r0;	 Catch:{ all -> 0x01a5 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
            goto L_0x011a;
        L_0x01e0:
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r0 = r19;
            r1 = r22;
            r19 = r0.tryAcquireEglContextLocked(r1);	 Catch:{ all -> 0x01a5 }
            if (r19 == 0) goto L_0x015e;
        L_0x01ee:
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ RuntimeException -> 0x0209 }
            r19 = r0;
            r19.start();	 Catch:{ RuntimeException -> 0x0209 }
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mHaveEglContext = r0;	 Catch:{ all -> 0x01a5 }
            r3 = 1;
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.notifyAll();	 Catch:{ all -> 0x01a5 }
            goto L_0x015e;
        L_0x0209:
            r16 = move-exception;
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r0 = r19;
            r1 = r22;
            r0.releaseEglContextLocked(r1);	 Catch:{ all -> 0x01a5 }
            throw r16;	 Catch:{ all -> 0x01a5 }
        L_0x0216:
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mHaveEglSurface = r0;	 Catch:{ all -> 0x01a5 }
            r4 = 1;
            r5 = 1;
            r13 = 1;
            goto L_0x016e;
        L_0x0223:
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a5 }
            r19.wait();	 Catch:{ all -> 0x01a5 }
            goto L_0x0034;
        L_0x022c:
            if (r4 == 0) goto L_0x0250;
        L_0x022e:
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r19 = r19.createSurface();	 Catch:{ all -> 0x01a8 }
            if (r19 == 0) goto L_0x02fd;
        L_0x023a:
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a8 }
            monitor-enter(r20);	 Catch:{ all -> 0x01a8 }
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mFinishedCreatingEglSurface = r0;	 Catch:{ all -> 0x02fa }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x02fa }
            r19.notifyAll();	 Catch:{ all -> 0x02fa }
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            r4 = 0;
        L_0x0250:
            if (r5 == 0) goto L_0x026b;
        L_0x0252:
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r19 = r19.createGL();	 Catch:{ all -> 0x01a8 }
            r0 = r19;
            r0 = (javax.microedition.khronos.opengles.GL10) r0;	 Catch:{ all -> 0x01a8 }
            r8 = r0;
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a8 }
            r0 = r19;
            r0.checkGLDriver(r8);	 Catch:{ all -> 0x01a8 }
            r5 = 0;
        L_0x026b:
            if (r3 == 0) goto L_0x0293;
        L_0x026d:
            r0 = r22;
            r0 = r0.mGLSurfaceTextureWeakRef;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r14 = r19.get();	 Catch:{ all -> 0x01a8 }
            r14 = (com.android.camera.ui.GLSurfaceTexture) r14;	 Catch:{ all -> 0x01a8 }
            if (r14 == 0) goto L_0x0292;
        L_0x027b:
            r19 = r14.mRenderer;	 Catch:{ all -> 0x01a8 }
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ all -> 0x01a8 }
            r20 = r0;
            r0 = r20;
            r0 = r0.mEglConfig;	 Catch:{ all -> 0x01a8 }
            r20 = r0;
            r0 = r19;
            r1 = r20;
            r0.onSurfaceCreated(r8, r1);	 Catch:{ all -> 0x01a8 }
        L_0x0292:
            r3 = 0;
        L_0x0293:
            if (r13 == 0) goto L_0x02af;
        L_0x0295:
            r0 = r22;
            r0 = r0.mGLSurfaceTextureWeakRef;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r14 = r19.get();	 Catch:{ all -> 0x01a8 }
            r14 = (com.android.camera.ui.GLSurfaceTexture) r14;	 Catch:{ all -> 0x01a8 }
            if (r14 == 0) goto L_0x02ae;
        L_0x02a3:
            r19 = r14.mRenderer;	 Catch:{ all -> 0x01a8 }
            r0 = r19;
            r1 = r17;
            r0.onSurfaceChanged(r8, r1, r9);	 Catch:{ all -> 0x01a8 }
        L_0x02ae:
            r13 = 0;
        L_0x02af:
            r0 = r22;
            r0 = r0.mGLSurfaceTextureWeakRef;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r14 = r19.get();	 Catch:{ all -> 0x01a8 }
            r14 = (com.android.camera.ui.GLSurfaceTexture) r14;	 Catch:{ all -> 0x01a8 }
            if (r14 == 0) goto L_0x02c6;
        L_0x02bd:
            r19 = r14.mRenderer;	 Catch:{ all -> 0x01a8 }
            r0 = r19;
            r0.onDrawFrame(r8);	 Catch:{ all -> 0x01a8 }
        L_0x02c6:
            r0 = r22;
            r0 = r0.mEglHelper;	 Catch:{ all -> 0x01a8 }
            r19 = r0;
            r15 = r19.swap();	 Catch:{ all -> 0x01a8 }
            switch(r15) {
                case 12288: goto L_0x02f5;
                case 12302: goto L_0x031f;
                default: goto L_0x02d3;
            };	 Catch:{ all -> 0x01a8 }
        L_0x02d3:
            r19 = "GLThread";
            r20 = "eglSwapBuffers";
            r0 = r19;
            r1 = r20;
            com.android.camera.ui.GLSurfaceTexture.EglHelper.logEglErrorAsWarning(r0, r1, r15);	 Catch:{ all -> 0x01a8 }
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a8 }
            monitor-enter(r20);	 Catch:{ all -> 0x01a8 }
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mSurfaceIsBad = r0;	 Catch:{ all -> 0x0321 }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x0321 }
            r19.notifyAll();	 Catch:{ all -> 0x0321 }
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
        L_0x02f5:
            if (r18 == 0) goto L_0x002f;
        L_0x02f7:
            r6 = 1;
            goto L_0x002f;
        L_0x02fa:
            r19 = move-exception;
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            throw r19;	 Catch:{ all -> 0x01a8 }
        L_0x02fd:
            r20 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x01a8 }
            monitor-enter(r20);	 Catch:{ all -> 0x01a8 }
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mFinishedCreatingEglSurface = r0;	 Catch:{ all -> 0x031c }
            r19 = 1;
            r0 = r19;
            r1 = r22;
            r1.mSurfaceIsBad = r0;	 Catch:{ all -> 0x031c }
            r19 = com.android.camera.ui.GLSurfaceTexture.sGLThreadManager;	 Catch:{ all -> 0x031c }
            r19.notifyAll();	 Catch:{ all -> 0x031c }
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            goto L_0x002f;
        L_0x031c:
            r19 = move-exception;
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            throw r19;	 Catch:{ all -> 0x01a8 }
        L_0x031f:
            r10 = 1;
            goto L_0x02f5;
        L_0x0321:
            r19 = move-exception;
            monitor-exit(r20);	 Catch:{ all -> 0x01a8 }
            throw r19;	 Catch:{ all -> 0x01a8 }
        L_0x0324:
            r19 = move-exception;
            monitor-exit(r20);
            throw r19;
            */
            throw new UnsupportedOperationException("Method not decompiled: com.android.camera.ui.GLSurfaceTexture.GLThread.guardedRun():void");
        }

        public boolean ableToDraw() {
            return (this.mHaveEglContext && this.mHaveEglSurface) ? readyToDraw() : false;
        }

        private boolean readyToDraw() {
            if (this.mPaused || !this.mHasSurface || this.mSurfaceIsBad || this.mWidth <= 0 || this.mHeight <= 0) {
                return false;
            }
            if (this.mRequestRender || this.mRenderMode == 1) {
                return true;
            }
            return false;
        }

        public void setRenderMode(int renderMode) {
            if (renderMode < 0 || renderMode > 1) {
                throw new IllegalArgumentException("renderMode");
            }
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mRenderMode = renderMode;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
            }
        }

        public int getRenderMode() {
            int i;
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                i = this.mRenderMode;
            }
            return i;
        }

        public void requestRender() {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mRequestRender = true;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
            }
        }

        public void surfaceCreated() {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mHasSurface = true;
                this.mFinishedCreatingEglSurface = false;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
                while (this.mWaitingForSurface && !this.mFinishedCreatingEglSurface) {
                    if (this.mExited) {
                        break;
                    }
                    try {
                        GLSurfaceTexture.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onPause() {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mRequestPaused = true;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused) {
                    try {
                        GLSurfaceTexture.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onResume() {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mRequestPaused = false;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
                while (!this.mExited && this.mPaused && !this.mRenderComplete) {
                    try {
                        GLSurfaceTexture.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void onWindowResize(int w, int h) {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mWidth = w;
                this.mHeight = h;
                this.mSizeChanged = true;
                this.mRequestRender = true;
                this.mRenderComplete = false;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
                while (!this.mExited && !this.mPaused) {
                    if (this.mRenderComplete || !ableToDraw()) {
                        break;
                    }
                    try {
                        GLSurfaceTexture.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestExitAndWait() {
            synchronized (GLSurfaceTexture.sGLThreadManager) {
                this.mShouldExit = true;
                GLSurfaceTexture.sGLThreadManager.notifyAll();
                while (!this.mExited) {
                    try {
                        GLSurfaceTexture.sGLThreadManager.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        public void requestReleaseEglContextLocked() {
            this.mShouldReleaseEglContext = true;
            GLSurfaceTexture.sGLThreadManager.notifyAll();
        }
    }

    private static class GLThreadManager {
        private static String TAG = "GLThreadManager";
        private GLThread mEglOwner;
        private boolean mGLESDriverCheckComplete;
        private int mGLESVersion;
        private boolean mGLESVersionCheckComplete;
        private boolean mLimitedGLESContexts;
        private boolean mMultipleGLESContextsAllowed;

        private GLThreadManager() {
        }

        public synchronized void threadExiting(GLThread thread) {
            thread.mExited = true;
            if (this.mEglOwner == thread) {
                this.mEglOwner = null;
            }
            notifyAll();
        }

        public boolean tryAcquireEglContextLocked(GLThread thread) {
            if (this.mEglOwner == thread || this.mEglOwner == null) {
                this.mEglOwner = thread;
                notifyAll();
                return true;
            }
            checkGLESVersion();
            if (this.mMultipleGLESContextsAllowed) {
                return true;
            }
            if (this.mEglOwner != null) {
                this.mEglOwner.requestReleaseEglContextLocked();
            }
            return false;
        }

        public void releaseEglContextLocked(GLThread thread) {
            if (this.mEglOwner == thread) {
                this.mEglOwner = null;
            }
            notifyAll();
        }

        public synchronized boolean shouldReleaseEGLContextWhenPausing() {
            return this.mLimitedGLESContexts;
        }

        public synchronized boolean shouldTerminateEGLWhenPausing() {
            checkGLESVersion();
            return !this.mMultipleGLESContextsAllowed;
        }

        public synchronized void checkGLDriver(GL10 gl) {
            boolean z = false;
            synchronized (this) {
                if (!this.mGLESDriverCheckComplete) {
                    checkGLESVersion();
                    String renderer = gl.glGetString(7937);
                    if (this.mGLESVersion < 131072) {
                        boolean z2;
                        if (renderer.startsWith("Q3Dimension MSM7500 ")) {
                            z2 = false;
                        } else {
                            z2 = true;
                        }
                        this.mMultipleGLESContextsAllowed = z2;
                        notifyAll();
                    }
                    if (!this.mMultipleGLESContextsAllowed) {
                        z = true;
                    }
                    this.mLimitedGLESContexts = z;
                    this.mGLESDriverCheckComplete = true;
                }
            }
        }

        private void checkGLESVersion() {
            if (!this.mGLESVersionCheckComplete) {
                this.mMultipleGLESContextsAllowed = true;
                this.mGLESVersionCheckComplete = true;
            }
        }
    }

    public interface GLWrapper {
        GL wrap(GL gl);
    }

    static class LogWriter extends Writer {
        private StringBuilder mBuilder = new StringBuilder();

        LogWriter() {
        }

        public void close() {
            flushBuilder();
        }

        public void flush() {
            flushBuilder();
        }

        public void write(char[] buf, int offset, int count) {
            for (int i = 0; i < count; i++) {
                char c = buf[offset + i];
                if (c == '\n') {
                    flushBuilder();
                } else {
                    this.mBuilder.append(c);
                }
            }
        }

        private void flushBuilder() {
            if (this.mBuilder.length() > 0) {
                Log.v("GLSurfaceTexture", this.mBuilder.toString());
                this.mBuilder.delete(0, this.mBuilder.length());
            }
        }
    }

    private class SimpleEGLConfigChooser extends ComponentSizeChooser {
        public SimpleEGLConfigChooser(boolean withDepthBuffer) {
            int i;
            if (withDepthBuffer) {
                i = 16;
            } else {
                i = 0;
            }
            super(8, 8, 8, 0, i, 0);
        }
    }

    public GLSurfaceTexture() {
        super(0, false);
        detachFromGLContext();
    }

    protected void finalize() throws Throwable {
        try {
            if (this.mGLThread != null) {
                this.mGLThread.requestExitAndWait();
            }
            super.finalize();
        } catch (Throwable th) {
            super.finalize();
        }
    }

    public void setPreserveEGLContextOnPause(boolean preserveOnPause) {
        this.mPreserveEGLContextOnPause = preserveOnPause;
    }

    public void setRenderer(Renderer renderer) {
        checkRenderThreadState();
        if (this.mEGLConfigChooser == null) {
            this.mEGLConfigChooser = new SimpleEGLConfigChooser(true);
        }
        if (this.mEGLContextFactory == null) {
            this.mEGLContextFactory = new DefaultContextFactory();
        }
        if (this.mEGLWindowSurfaceFactory == null) {
            this.mEGLWindowSurfaceFactory = new DefaultWindowSurfaceFactory();
        }
        this.mRenderer = renderer;
        this.mGLThread = new GLThread(this.mThisWeakRef);
    }

    public void setEGLContextClientVersion(int version) {
        checkRenderThreadState();
        this.mEGLContextClientVersion = version;
    }

    public void setRenderMode(int renderMode) {
        this.mGLThread.setRenderMode(renderMode);
    }

    public void requestRender() {
        this.mGLThread.requestRender();
    }

    public void setSize(int w, int h) {
        this.mPreservedWidth = w;
        this.mPreservedHeight = h;
        this.mGLThread.onWindowResize(w, h);
    }

    public void release() {
    }

    private void internalStart() {
        if (this.mRenderer != null) {
            if (this.mStopped) {
                int renderMode = 1;
                if (this.mGLThread != null) {
                    renderMode = this.mGLThread.getRenderMode();
                }
                this.mGLThread = new GLThread(this.mThisWeakRef);
                setSize(this.mPreservedWidth, this.mPreservedHeight);
                if (renderMode != 1) {
                    this.mGLThread.setRenderMode(renderMode);
                }
            }
            if (!this.mGLThread.isAlive()) {
                this.mGLThread.start();
                this.mGLThread.surfaceCreated();
            }
        }
        this.mStopped = false;
    }

    public void startWithShareContext(EGLContext context) {
        this.mShareContext = context;
        internalStart();
    }

    public void stop() {
        if (this.mGLThread != null) {
            this.mGLThread.requestExitAndWait();
        }
        this.mStopped = true;
    }

    public void pause() {
        this.mGLThread.onPause();
    }

    public void resume() {
        this.mGLThread.onResume();
    }

    private void checkRenderThreadState() {
        if (this.mGLThread != null) {
            throw new IllegalStateException("setRenderer has already been called for this instance.");
        }
    }
}
