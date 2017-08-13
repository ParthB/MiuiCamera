package com.android.camera.effect.renders;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.location.Location;
import android.net.Uri;
import android.opengl.GLES20;
import android.os.ConditionVariable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.android.camera.ActivityBase;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Thumbnail;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.FrameBuffer;
import com.android.camera.effect.ShaderNativeUtil;
import com.android.camera.effect.SnapshotCanvas;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import com.android.camera.effect.draw_mode.DrawIntTexAttribute;
import com.android.camera.effect.draw_mode.DrawJPEGAttribute;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.storage.ImageSaver;
import com.android.camera.storage.Storage;
import com.android.gallery3d.exif.ExifInterface;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.BitmapTexture;
import com.android.gallery3d.ui.GLCanvasImpl;
import com.android.gallery3d.ui.GLId;
import com.android.gallery3d.ui.StringTexture;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

public class SnapshotEffectRender {
    private static final int[] CONFIG_SPEC = new int[]{12352, 4, 12324, 8, 12323, 8, 12322, 8, 12344};
    private ActivityBase mActivity;
    private Bitmap mDualCameraWaterMark;
    private EGL10 mEgl;
    private EGLConfig mEglConfig;
    private EGLContext mEglContext;
    private EGLDisplay mEglDisplay;
    private EGLHandler mEglHandler;
    private EGLSurface mEglSurface;
    private HandlerThread mEglThread;
    private ConditionVariable mEglThreadBlockVar = new ConditionVariable();
    private boolean mExifNeeded = true;
    private ImageSaver mImageSaver;
    private boolean mIsImageCaptureIntent;
    private volatile int mJpegQueueSize = 0;
    private final Object mLock = new Object();
    private int mQuality = 85;
    private boolean mRelease;
    private boolean mReleasePending;
    private Map<String, String> mTitleMap = new HashMap(7);

    private class EGLHandler extends Handler {
        private FrameBuffer mFrameBuffer;
        private SnapshotCanvas mGLCanvas;

        public EGLHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            boolean isExifNeeded = true;
            switch (msg.what) {
                case 0:
                    initEGL();
                    this.mGLCanvas = new SnapshotCanvas();
                    this.mGLCanvas.setSize(SnapshotEffectRender.this.mActivity.getCameraScreenNail().getWidth(), SnapshotEffectRender.this.mActivity.getCameraScreenNail().getHeight());
                    SnapshotEffectRender.this.mEglThreadBlockVar.open();
                    return;
                case 1:
                    drawMainJpeg((DrawJPEGAttribute) msg.obj, true);
                    this.mGLCanvas.recycledResources();
                    if (SnapshotEffectRender.this.mReleasePending && !hasMessages(1)) {
                        release();
                    }
                    synchronized (SnapshotEffectRender.this.mLock) {
                        SnapshotEffectRender snapshotEffectRender = SnapshotEffectRender.this;
                        snapshotEffectRender.mJpegQueueSize = snapshotEffectRender.mJpegQueueSize - 1;
                    }
                    return;
                case 2:
                    DrawJPEGAttribute jpeg = msg.obj;
                    if (msg.arg1 <= 0) {
                        isExifNeeded = false;
                    }
                    if (isExifNeeded) {
                        drawThumbJpeg(jpeg, false);
                    }
                    drawMainJpeg(jpeg, false);
                    this.mGLCanvas.recycledResources();
                    SnapshotEffectRender.this.mEglThreadBlockVar.open();
                    return;
                case 3:
                    drawThumbJpeg((DrawJPEGAttribute) msg.obj, true);
                    return;
                case 4:
                    drawThumbJpeg((DrawJPEGAttribute) msg.obj, true);
                    SnapshotEffectRender.this.mEglThreadBlockVar.open();
                    return;
                case 5:
                    release();
                    return;
                case 6:
                    this.mGLCanvas.prepareEffectRenders(false, msg.arg1);
                    return;
                default:
                    return;
            }
        }

        private void initEGL() {
            SnapshotEffectRender.this.mEgl = (EGL10) EGLContext.getEGL();
            SnapshotEffectRender.this.mEglDisplay = SnapshotEffectRender.this.mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
            if (SnapshotEffectRender.this.mEglDisplay == EGL10.EGL_NO_DISPLAY) {
                throw new RuntimeException("eglGetDisplay failed");
            }
            int[] version = new int[2];
            if (SnapshotEffectRender.this.mEgl.eglInitialize(SnapshotEffectRender.this.mEglDisplay, version)) {
                Log.v("SnapshotEffectProcessor", "EGL version: " + version[0] + '.' + version[1]);
                int[] attribList = new int[]{12440, 2, 12344};
                SnapshotEffectRender.this.mEglConfig = SnapshotEffectRender.chooseConfig(SnapshotEffectRender.this.mEgl, SnapshotEffectRender.this.mEglDisplay);
                SnapshotEffectRender.this.mEglContext = SnapshotEffectRender.this.mEgl.eglCreateContext(SnapshotEffectRender.this.mEglDisplay, SnapshotEffectRender.this.mEglConfig, EGL10.EGL_NO_CONTEXT, attribList);
                if (SnapshotEffectRender.this.mEglContext == null || SnapshotEffectRender.this.mEglContext == EGL10.EGL_NO_CONTEXT) {
                    throw new RuntimeException("failed to createContext");
                }
                SnapshotEffectRender.this.mEglSurface = SnapshotEffectRender.this.mEgl.eglCreatePbufferSurface(SnapshotEffectRender.this.mEglDisplay, SnapshotEffectRender.this.mEglConfig, new int[]{12375, Util.sWindowWidth, 12374, Util.sWindowHeight, 12344});
                if (SnapshotEffectRender.this.mEglSurface == null || SnapshotEffectRender.this.mEglSurface == EGL10.EGL_NO_SURFACE) {
                    throw new RuntimeException("failed to createWindowSurface");
                } else if (!SnapshotEffectRender.this.mEgl.eglMakeCurrent(SnapshotEffectRender.this.mEglDisplay, SnapshotEffectRender.this.mEglSurface, SnapshotEffectRender.this.mEglSurface, SnapshotEffectRender.this.mEglContext)) {
                    throw new RuntimeException("failed to eglMakeCurrent");
                } else {
                    return;
                }
            }
            throw new RuntimeException("eglInitialize failed");
        }

        private void drawWaterMark(WaterMark waterMark, int x, int y, int orientation) {
            this.mGLCanvas.getState().pushState();
            if (orientation != 0) {
                this.mGLCanvas.getState().translate((float) (waterMark.getCenterX() + x), (float) (waterMark.getCenterY() + y));
                this.mGLCanvas.getState().rotate((float) (-orientation), 0.0f, 0.0f, 1.0f);
                this.mGLCanvas.getState().translate((float) ((-x) - waterMark.getCenterX()), (float) ((-y) - waterMark.getCenterY()));
            }
            this.mGLCanvas.getBasicRender().draw(new DrawBasicTexAttribute(waterMark.getTexture(), waterMark.getLeft() + x, waterMark.getTop() + y, waterMark.getWidth(), waterMark.getHeight()));
            this.mGLCanvas.getState().popState();
        }

        private void drawWaterMark(int x, int y, int width, int height, int orientation) {
            if (Device.isEffectWatermarkFilted() && (CameraSettings.isTimeWaterMarkOpen(CameraSettingPreferences.instance()) || CameraSettings.isDualCameraWaterMarkOpen(CameraSettingPreferences.instance()))) {
                if (CameraSettings.isTimeWaterMarkOpen(CameraSettingPreferences.instance())) {
                    WaterMark waterMark;
                    String waterMarkText = Util.getTimeWatermark();
                    if (Device.isSupportedNewStyleTimeWaterMark()) {
                        waterMark = new NewStyleTextWaterMark(waterMarkText, width, height, orientation);
                    } else {
                        waterMark = new TextWaterMark(waterMarkText, width, height, orientation);
                    }
                    drawWaterMark(waterMark, x, y, orientation);
                }
                if (CameraSettings.isDualCameraWaterMarkOpen(CameraSettingPreferences.instance()) && SnapshotEffectRender.this.mDualCameraWaterMark != null) {
                    drawWaterMark(new ImageWaterMark(SnapshotEffectRender.this.mDualCameraWaterMark, width, height, orientation), x, y, orientation);
                }
            }
        }

        private byte[] applyEffect(DrawJPEGAttribute jpeg, int downScale, boolean applyToThumb, Size targetSize, Size originSize) {
            byte[] data = applyToThumb ? jpeg.mExif.getThumbnailBytes() : jpeg.mData;
            if (data == null) {
                Log.w("SnapshotEffectProcessor", "Null " + (applyToThumb ? "thumb!" : "jpeg!"));
                return null;
            }
            long lastime = System.currentTimeMillis();
            int[] texId = new int[1];
            GLId.glGenTextures(1, texId, 0);
            int[] textureSize = ShaderNativeUtil.initTexture(data, texId[0], downScale);
            Log.d("SnapshotEffectProcessor", "initTime=" + (System.currentTimeMillis() - lastime));
            int width = applyToThumb ? textureSize[0] : jpeg.mWidth;
            int height = applyToThumb ? textureSize[1] : jpeg.mHeight;
            int previewWidth = applyToThumb ? textureSize[0] : jpeg.mPreviewWidth;
            int previewHeight = applyToThumb ? textureSize[1] : jpeg.mPreviewHeight;
            if (applyToThumb && targetSize != null) {
                targetSize.width = width;
                targetSize.height = height;
                Log.d("SnapshotEffectProcessor", "thumbSize=" + targetSize.width + "*" + targetSize.height);
            }
            Render render = getEffectRender(jpeg.mEffectIndex);
            render.setPreviewSize(previewWidth, previewHeight);
            render.setEffectRangeAttribute(jpeg.mAttribute);
            render.setMirror(jpeg.mMirror);
            if (applyToThumb) {
                render.setSnapshotSize(width, height);
            } else {
                render.setSnapshotSize(originSize.width, originSize.height);
            }
            render.setOrientation(jpeg.mOrientation);
            render.setShootRotation(jpeg.mShootRotation);
            render.setJpegOrientation(jpeg.mJpegOrientation);
            checkFrameBuffer(jpeg.mWidth, jpeg.mHeight);
            this.mGLCanvas.beginBindFrameBuffer(this.mFrameBuffer);
            lastime = System.currentTimeMillis();
            render.draw(new DrawIntTexAttribute(texId[0], 0, 0, width, height));
            drawWaterMark(0, 0, width, height, jpeg.mJpegOrientation);
            Log.d("SnapshotEffectProcessor", "drawTime=" + (System.currentTimeMillis() - lastime));
            GLES20.glPixelStorei(3333, 1);
            lastime = System.currentTimeMillis();
            byte[] outData = ShaderNativeUtil.getPicture(width, height, SnapshotEffectRender.this.mQuality);
            Log.d("SnapshotEffectProcessor", "readTime=" + (System.currentTimeMillis() - lastime));
            if (GLES20.glIsTexture(texId[0])) {
                GLES20.glDeleteTextures(1, texId, 0);
            }
            this.mGLCanvas.endBindFrameBuffer();
            return outData;
        }

        private Render getEffectRender(int index) {
            RenderGroup group = this.mGLCanvas.getEffectRenderGroup();
            if (group.getRender(index) == null) {
                this.mGLCanvas.prepareEffectRenders(false, index);
            }
            return group.getRender(index);
        }

        private boolean drawMainJpeg(DrawJPEGAttribute jpeg, boolean save) {
            int downScale = 1;
            Size originSize = new Size(jpeg.mWidth, jpeg.mHeight);
            while (true) {
                if (jpeg.mWidth <= GLCanvasImpl.sMaxTextureSize && jpeg.mHeight <= GLCanvasImpl.sMaxTextureSize) {
                    break;
                }
                jpeg.mWidth /= 2;
                jpeg.mHeight /= 2;
                downScale *= 2;
            }
            if (EffectController.getInstance().needDownScale(jpeg.mEffectIndex)) {
                int effectDownScale = (int) ((((float) jpeg.mWidth) / ((float) jpeg.mPreviewWidth)) + 0.5f);
                if (effectDownScale > downScale) {
                    downScale = effectDownScale;
                }
            }
            byte[] data = applyEffect(jpeg, downScale, false, null, originSize);
            Log.d("SnapshotEffectProcessor", "mainLen=" + (data == null ? "null" : Integer.valueOf(data.length)));
            if (data != null) {
                jpeg.mData = data;
            }
            if (save) {
                String title;
                synchronized (SnapshotEffectRender.this) {
                    title = (String) SnapshotEffectRender.this.mTitleMap.get(jpeg.mTitle);
                    SnapshotEffectRender.this.mTitleMap.remove(jpeg.mTitle);
                }
                String str;
                if (SnapshotEffectRender.this.mImageSaver != null) {
                    boolean z;
                    ImageSaver -get9 = SnapshotEffectRender.this.mImageSaver;
                    byte[] bArr = jpeg.mData;
                    if (title == null) {
                        str = jpeg.mTitle;
                    } else {
                        str = title;
                    }
                    String str2 = title == null ? null : jpeg.mTitle;
                    long j = jpeg.mDate;
                    Uri uri = jpeg.mUri;
                    Location location = jpeg.mLoc;
                    int i = jpeg.mWidth;
                    int i2 = jpeg.mHeight;
                    ExifInterface exifInterface = jpeg.mExif;
                    int i3 = jpeg.mJpegOrientation;
                    if (title == null) {
                        z = jpeg.mFinalImage;
                    } else {
                        z = false;
                    }
                    -get9.addImage(2, bArr, str, str2, j, uri, location, i, i2, exifInterface, i3, false, false, z);
                } else if (jpeg.mUri == null) {
                    String str3;
                    Activity -get0 = SnapshotEffectRender.this.mActivity;
                    if (title == null) {
                        str3 = jpeg.mTitle;
                    } else {
                        str3 = title;
                    }
                    Storage.addImage(-get0, str3, jpeg.mDate, jpeg.mLoc, jpeg.mJpegOrientation, jpeg.mData, jpeg.mWidth, jpeg.mHeight, false);
                } else {
                    String str4;
                    Context -get02 = SnapshotEffectRender.this.mActivity;
                    byte[] bArr2 = jpeg.mData;
                    ExifInterface exifInterface2 = jpeg.mExif;
                    Uri uri2 = jpeg.mUri;
                    if (title == null) {
                        str = jpeg.mTitle;
                    } else {
                        str = title;
                    }
                    Location location2 = jpeg.mLoc;
                    int i4 = jpeg.mJpegOrientation;
                    int i5 = jpeg.mWidth;
                    int i6 = jpeg.mHeight;
                    if (title == null) {
                        str4 = null;
                    } else {
                        str4 = jpeg.mTitle;
                    }
                    Storage.updateImage(-get02, bArr2, exifInterface2, uri2, str, location2, i4, i5, i6, str4);
                }
            } else if (jpeg.mExif != null) {
                OutputStream s = new ByteArrayOutputStream();
                try {
                    jpeg.mExif.writeExif(jpeg.mData, s);
                    byte[] outData = s.toByteArray();
                    if (outData != null) {
                        jpeg.mData = outData;
                    }
                    s.close();
                } catch (Throwable e) {
                    Log.e("SnapshotEffectProcessor", e.getMessage(), e);
                }
            }
            return true;
        }

        private boolean drawThumbJpeg(DrawJPEGAttribute jpeg, boolean save) {
            if (jpeg.mExif == null) {
                jpeg.mExif = SnapshotEffectRender.this.getExif(jpeg.mData);
            }
            Size size = new Size();
            byte[] data = applyEffect(jpeg, 1, true, size, null);
            Log.d("SnapshotEffectProcessor", "drawThumbJepg: thumbLen=" + (data == null ? "null" : Integer.valueOf(data.length)));
            if (data != null) {
                jpeg.mExif.setCompressedThumbnail(data);
            }
            boolean appendExif = jpeg.mJpegOrientation != 0;
            if (save && jpeg.mExif.getThumbnailBytes() != null) {
                jpeg.mUri = Storage.addImage(SnapshotEffectRender.this.mActivity, jpeg.mTitle, jpeg.mDate, jpeg.mLoc, jpeg.mJpegOrientation, jpeg.mExif.getThumbnailBytes(), size.width, size.height, false, false, false, appendExif);
                if (jpeg.mUri != null) {
                    SnapshotEffectRender.this.mActivity.addSecureUri(jpeg.mUri);
                }
            }
            return true;
        }

        private void checkFrameBuffer(int w, int h) {
            if (this.mFrameBuffer != null && this.mFrameBuffer.getWidth() >= w) {
                if (this.mFrameBuffer.getHeight() >= h) {
                    return;
                }
            }
            this.mFrameBuffer = null;
            this.mFrameBuffer = new FrameBuffer(this.mGLCanvas, w, h, 0);
        }

        private void release() {
            SnapshotEffectRender.this.mRelease = true;
            SnapshotEffectRender.this.mReleasePending = false;
            SnapshotEffectRender.this.mEgl.eglDestroySurface(SnapshotEffectRender.this.mEglDisplay, SnapshotEffectRender.this.mEglSurface);
            SnapshotEffectRender.this.mEgl.eglDestroyContext(SnapshotEffectRender.this.mEglDisplay, SnapshotEffectRender.this.mEglContext);
            SnapshotEffectRender.this.mEgl.eglMakeCurrent(SnapshotEffectRender.this.mEglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
            SnapshotEffectRender.this.mEgl.eglTerminate(SnapshotEffectRender.this.mEglDisplay);
            SnapshotEffectRender.this.mEglSurface = null;
            SnapshotEffectRender.this.mEglContext = null;
            SnapshotEffectRender.this.mEglDisplay = null;
            SnapshotEffectRender.this.mActivity = null;
            this.mFrameBuffer = null;
            System.gc();
            this.mGLCanvas.recycledResources();
            SnapshotEffectRender.this.mEglThread.quit();
            this.mGLCanvas = null;
        }

        public void sendMessageSync(int msg) {
            SnapshotEffectRender.this.mEglThreadBlockVar.close();
            sendEmptyMessage(msg);
            SnapshotEffectRender.this.mEglThreadBlockVar.block();
        }
    }

    private abstract class WaterMark {
        protected int mOrientation;
        protected int mPictureHeight;
        protected int mPictureWidth;

        public abstract int getCenterX();

        public abstract int getCenterY();

        public abstract int getHeight();

        public abstract BasicTexture getTexture();

        public abstract int getWidth();

        public WaterMark(int width, int height, int orientation) {
            this.mPictureWidth = width;
            this.mPictureHeight = height;
            this.mOrientation = orientation;
        }

        public int getLeft() {
            return getCenterX() - (getWidth() / 2);
        }

        public int getTop() {
            return getCenterY() - (getHeight() / 2);
        }
    }

    private class ImageWaterMark extends WaterMark {
        private int mCenterX;
        private int mCenterY;
        private int mHeight;
        private BitmapTexture mImageTexture;
        private int mPadding;
        private int mWidth;

        public ImageWaterMark(Bitmap image, int width, int height, int orientation) {
            super(width, height, orientation);
            float ratio = ((float) Math.min(width, height)) / 1080.0f;
            this.mHeight = ((int) Math.round(((double) ratio) * 57.294429708d)) & -2;
            this.mWidth = ((this.mHeight * image.getWidth()) / image.getHeight()) & -2;
            this.mPadding = ((int) Math.round(((double) ratio) * 46.551724138d)) & -2;
            this.mImageTexture = new BitmapTexture(image);
            this.mImageTexture.setOpaque(false);
            calcCenterAxis();
        }

        private void calcCenterAxis() {
            switch (this.mOrientation) {
                case 0:
                    this.mCenterX = this.mPadding + (getWidth() / 2);
                    this.mCenterY = (this.mPictureHeight - this.mPadding) - (getHeight() / 2);
                    return;
                case 90:
                    this.mCenterX = (this.mPictureWidth - this.mPadding) - (getHeight() / 2);
                    this.mCenterY = (this.mPictureHeight - this.mPadding) - (getWidth() / 2);
                    return;
                case 180:
                    this.mCenterX = (this.mPictureWidth - this.mPadding) - (getWidth() / 2);
                    this.mCenterY = this.mPadding + (getHeight() / 2);
                    return;
                case 270:
                    this.mCenterX = this.mPadding + (getHeight() / 2);
                    this.mCenterY = this.mPadding + (getWidth() / 2);
                    return;
                default:
                    return;
            }
        }

        public int getCenterX() {
            return this.mCenterX;
        }

        public int getCenterY() {
            return this.mCenterY;
        }

        public int getWidth() {
            return this.mWidth;
        }

        public int getHeight() {
            return this.mHeight;
        }

        public BasicTexture getTexture() {
            return this.mImageTexture;
        }
    }

    private class NewStyleTextWaterMark extends WaterMark {
        private final float TEXT_PIXEL_SIZE;
        private int mCenterX;
        private int mCenterY;
        private int mCharMargin;
        private int mHorizontalPadding;
        private int mPadding;
        private int mVerticalPadding;
        private int mWaterHeight;
        private String mWaterText;
        private BasicTexture mWaterTexture;
        private int mWaterWidth;

        private NewStyleTextWaterMark(String text, int width, int height, int orientation) {
            super(width, height, orientation);
            this.TEXT_PIXEL_SIZE = 30.079576f;
            float ratio = ((float) Math.min(width, height)) / 1080.0f;
            this.mWaterText = text;
            this.mWaterTexture = StringTexture.newInstance(this.mWaterText, 30.079576f * ratio, -1, 2);
            this.mWaterWidth = this.mWaterTexture.getWidth();
            this.mWaterHeight = this.mWaterTexture.getHeight();
            this.mPadding = (int) Math.round(((double) ratio) * 43.687002653d);
            this.mCharMargin = (int) ((((float) this.mWaterHeight) * 0.13f) / 2.0f);
            this.mHorizontalPadding = this.mPadding & -2;
            this.mVerticalPadding = (this.mPadding - this.mCharMargin) & -2;
            calcCenterAxis();
            if (Util.sIsDumpLog) {
                print();
            }
        }

        private void calcCenterAxis() {
            switch (this.mOrientation) {
                case 0:
                    this.mCenterX = (this.mPictureWidth - this.mHorizontalPadding) - (this.mWaterWidth / 2);
                    this.mCenterY = (this.mPictureHeight - this.mVerticalPadding) - (this.mWaterHeight / 2);
                    return;
                case 90:
                    this.mCenterX = (this.mPictureWidth - this.mVerticalPadding) - (this.mWaterHeight / 2);
                    this.mCenterY = this.mHorizontalPadding + (this.mWaterWidth / 2);
                    return;
                case 180:
                    this.mCenterX = this.mHorizontalPadding + (this.mWaterWidth / 2);
                    this.mCenterY = this.mVerticalPadding + (this.mWaterHeight / 2);
                    return;
                case 270:
                    this.mCenterX = this.mVerticalPadding + (this.mWaterHeight / 2);
                    this.mCenterY = (this.mPictureHeight - this.mHorizontalPadding) - (this.mWaterWidth / 2);
                    return;
                default:
                    return;
            }
        }

        public int getCenterX() {
            return this.mCenterX;
        }

        public int getCenterY() {
            return this.mCenterY;
        }

        public int getWidth() {
            return this.mWaterWidth;
        }

        public int getHeight() {
            return this.mWaterHeight;
        }

        public BasicTexture getTexture() {
            return this.mWaterTexture;
        }

        private void print() {
            Log.v("SnapshotEffectProcessor", "WaterMark mPictureWidth=" + this.mPictureWidth + " mPictureHeight =" + this.mPictureHeight + " mWaterText=" + this.mWaterText + " mCenterX=" + this.mCenterX + " mCenterY=" + this.mCenterY + " mWaterWidth=" + this.mWaterWidth + " mWaterHeight=" + this.mWaterHeight + " mPadding=" + this.mPadding);
        }
    }

    private class Size {
        public int height;
        public int width;

        Size(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }

    private class TextWaterMark extends WaterMark {
        private final int[][] PIC_WIDTHS;
        private final int[][] WATERMARK_FONT_SIZES;
        private int mCenterX;
        private int mCenterY;
        private int mCharMargin;
        private int mFontIndex;
        private int mPadding;
        private int mWaterHeight;
        private String mWaterText;
        private BasicTexture mWaterTexture;
        private int mWaterWidth;

        private TextWaterMark(String text, int width, int height, int orientation) {
            super(width, height, orientation);
            this.WATERMARK_FONT_SIZES = new int[][]{new int[]{5, 4, 2, 4, 3, 7}, new int[]{8, 6, 2, 6, 3, 7}, new int[]{11, 6, 5, 6, 5, 12}, new int[]{12, 7, 5, 7, 5, 12}, new int[]{50, 32, 11, 31, 20, 47}, new int[]{58, 36, 19, 38, 24, 55}, new int[]{65, 41, 24, 42, 27, 63}, new int[]{80, 50, 24, 50, 32, 75}, new int[]{83, 52, 25, 52, 33, 78}, new int[]{104, 65, 33, 65, 42, 98}, new int[]{128, 80, 40, 80, 48, 132}};
            this.PIC_WIDTHS = new int[][]{new int[]{0, 149}, new int[]{150, 239}, new int[]{240, 279}, new int[]{280, 400}, new int[]{401, 1439}, new int[]{1440, 1511}, new int[]{1512, 1799}, new int[]{1800, 1899}, new int[]{1900, 2299}, new int[]{2300, 3120}, new int[]{3121, 4000}};
            this.mWaterText = text;
            this.mWaterTexture = StringTexture.newInstance(this.mWaterText, 144.0f, -262152, 0.0f, false, 1);
            this.mFontIndex = getFontIndex(width, height);
            this.mWaterWidth = getWaterMarkWidth(this.mWaterText, this.mFontIndex);
            this.mWaterHeight = (int) (((float) this.WATERMARK_FONT_SIZES[this.mFontIndex][0]) / 0.82f);
            this.mPadding = this.WATERMARK_FONT_SIZES[this.mFontIndex][5];
            this.mCharMargin = (int) ((((float) this.mWaterHeight) * 0.18f) / 2.0f);
            calcCenterAxis();
            if (Util.sIsDumpLog) {
                print();
            }
        }

        private int getFontIndex(int width, int height) {
            int shotL = Math.min(width, height);
            int index = this.WATERMARK_FONT_SIZES.length - 1;
            int i = 0;
            while (i < this.PIC_WIDTHS.length) {
                if (shotL >= this.PIC_WIDTHS[i][0] && shotL <= this.PIC_WIDTHS[i][1]) {
                    return i;
                }
                i++;
            }
            return index;
        }

        private int getWaterMarkWidth(String text, int index) {
            int dw = this.WATERMARK_FONT_SIZES[index][1];
            int mw = this.WATERMARK_FONT_SIZES[index][2];
            int sw = this.WATERMARK_FONT_SIZES[index][3];
            int cw = this.WATERMARK_FONT_SIZES[index][4];
            int length = 0;
            for (char c : text.toCharArray()) {
                if (c >= '0' && c <= '9') {
                    length += dw;
                } else if (c == ':') {
                    length += cw;
                } else if (c == '-') {
                    length += mw;
                } else if (c == ' ') {
                    length += sw;
                }
            }
            return length;
        }

        private void calcCenterAxis() {
            switch (this.mOrientation) {
                case 0:
                    this.mCenterX = (this.mPictureWidth - this.mPadding) - (this.mWaterWidth / 2);
                    this.mCenterY = ((this.mPictureHeight - this.mPadding) - (this.mWaterHeight / 2)) + this.mCharMargin;
                    return;
                case 90:
                    this.mCenterX = ((this.mPictureWidth - this.mPadding) - (this.mWaterHeight / 2)) + this.mCharMargin;
                    this.mCenterY = this.mPadding + (this.mWaterWidth / 2);
                    return;
                case 180:
                    this.mCenterX = this.mPadding + (this.mWaterWidth / 2);
                    this.mCenterY = (this.mPadding + (this.mWaterHeight / 2)) - this.mCharMargin;
                    return;
                case 270:
                    this.mCenterX = (this.mPadding + (this.mWaterHeight / 2)) - this.mCharMargin;
                    this.mCenterY = (this.mPictureHeight - this.mPadding) - (this.mWaterWidth / 2);
                    return;
                default:
                    return;
            }
        }

        public int getCenterX() {
            return this.mCenterX;
        }

        public int getCenterY() {
            return this.mCenterY;
        }

        public int getWidth() {
            return this.mWaterWidth;
        }

        public int getHeight() {
            return this.mWaterHeight;
        }

        public BasicTexture getTexture() {
            return this.mWaterTexture;
        }

        private void print() {
            Log.v("SnapshotEffectProcessor", "WaterMark mPictureWidth=" + this.mPictureWidth + " mPictureHeight =" + this.mPictureHeight + " mWaterText=" + this.mWaterText + " mFontIndex=" + this.mFontIndex + " mCenterX=" + this.mCenterX + " mCenterY=" + this.mCenterY + " mWaterWidth=" + this.mWaterWidth + " mWaterHeight=" + this.mWaterHeight + " mPadding=" + this.mPadding);
        }
    }

    public SnapshotEffectRender(ActivityBase activity, boolean isImageCaptureIntent) {
        this.mActivity = activity;
        this.mIsImageCaptureIntent = isImageCaptureIntent;
        this.mEglThread = new HandlerThread("SnapshotEffectProcessor");
        this.mEglThread.start();
        this.mEglHandler = new EGLHandler(this.mEglThread.getLooper());
        this.mEglHandler.sendMessageSync(0);
        this.mRelease = false;
        if (CameraSettings.isSupportedOpticalZoom()) {
            Options options = new Options();
            options.inScaled = false;
            options.inPurgeable = true;
            options.inPremultiplied = false;
            this.mDualCameraWaterMark = BitmapFactory.decodeFile("/system/etc/dualcamera.png", options);
        }
    }

    public void setImageSaver(ImageSaver imageSaver) {
        this.mImageSaver = imageSaver;
    }

    public void setQuality(int quality) {
        if (quality > 0 && quality < 100) {
            this.mQuality = quality;
        }
    }

    public boolean processorJpegAsync(DrawJPEGAttribute jpeg) {
        boolean z = false;
        Log.d("SnapshotEffectProcessor", "queueSize=" + this.mJpegQueueSize);
        if (this.mJpegQueueSize >= 7) {
            Log.d("SnapshotEffectProcessor", "queueSize is full, drop it " + jpeg.mTitle);
            return false;
        }
        boolean sync;
        if (this.mJpegQueueSize == 0) {
            sync = true;
        } else {
            sync = false;
        }
        if (sync) {
            processorThumSync(jpeg);
        } else {
            processorThumAsync(jpeg);
        }
        if (!this.mIsImageCaptureIntent && sync && this.mExifNeeded) {
            Bitmap bitmap = jpeg.mExif.getThumbnailBitmap();
            if (!(bitmap == null || jpeg.mUri == null)) {
                jpeg.mFinalImage = false;
                Thumbnail t = Thumbnail.createThumbnail(jpeg.mUri, bitmap, jpeg.mJpegOrientation, false);
                if (!CameraSettings.isSwitchOn("pref_camera_portrait_mode_key")) {
                    z = true;
                }
                t.setUpdateAnimation(z);
                this.mActivity.getThumbnailUpdater().setThumbnail(t);
            }
        }
        synchronized (this.mLock) {
            this.mJpegQueueSize++;
        }
        this.mEglHandler.obtainMessage(1, jpeg).sendToTarget();
        return true;
    }

    public void processorJpegSync(DrawJPEGAttribute jpeg) {
        int i;
        this.mEglThreadBlockVar.close();
        EGLHandler eGLHandler = this.mEglHandler;
        if (this.mExifNeeded) {
            i = 1;
        } else {
            i = 0;
        }
        eGLHandler.obtainMessage(2, i, 0, jpeg).sendToTarget();
        this.mEglThreadBlockVar.block();
    }

    public void changeJpegTitle(String title, String oldTitle) {
        if (oldTitle != null && oldTitle.length() != 0) {
            synchronized (this) {
                this.mTitleMap.put(oldTitle, title);
            }
        }
    }

    private void processorThumAsync(DrawJPEGAttribute jpeg) {
        if (this.mExifNeeded) {
            this.mEglHandler.obtainMessage(3, jpeg).sendToTarget();
        } else {
            jpeg.mUri = Storage.newImage(this.mActivity, jpeg.mTitle, jpeg.mDate, jpeg.mJpegOrientation, jpeg.mPreviewWidth, jpeg.mPreviewHeight);
        }
    }

    private void processorThumSync(DrawJPEGAttribute jpeg) {
        if (this.mExifNeeded) {
            jpeg.mExif = getExif(jpeg.mData);
            if (jpeg.mExif.getThumbnailBytes() != null) {
                this.mEglThreadBlockVar.close();
                this.mEglHandler.obtainMessage(4, jpeg).sendToTarget();
                this.mEglThreadBlockVar.block();
                return;
            }
        }
        jpeg.mUri = Storage.newImage(this.mActivity, jpeg.mTitle, jpeg.mDate, jpeg.mJpegOrientation, jpeg.mPreviewWidth, jpeg.mPreviewHeight);
    }

    private ExifInterface getExif(byte[] jpeg) {
        ExifInterface exif = new ExifInterface();
        try {
            exif.readExif(jpeg);
        } catch (IOException e) {
            Log.d("SnapshotEffectProcessor", e.getMessage());
        }
        return exif;
    }

    public void release() {
        if (this.mEglHandler.hasMessages(1)) {
            this.mReleasePending = true;
        } else {
            this.mEglHandler.sendEmptyMessage(5);
        }
    }

    private static EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
        int[] numConfig = new int[1];
        if (egl.eglChooseConfig(display, CONFIG_SPEC, null, 0, numConfig)) {
            int numConfigs = numConfig[0];
            if (numConfigs <= 0) {
                throw new IllegalArgumentException("No configs match configSpec");
            }
            EGLConfig[] configs = new EGLConfig[numConfigs];
            if (egl.eglChooseConfig(display, CONFIG_SPEC, configs, numConfigs, numConfig)) {
                return configs[0];
            }
            throw new IllegalArgumentException("eglChooseConfig#2 failed");
        }
        throw new IllegalArgumentException("eglChooseConfig failed");
    }

    public void prepareEffectRender(int effect) {
        this.mEglHandler.obtainMessage(6, effect, 0).sendToTarget();
    }
}
