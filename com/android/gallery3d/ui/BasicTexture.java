package com.android.gallery3d.ui;

import android.util.Log;
import com.android.camera.effect.draw_mode.DrawBasicTexAttribute;
import java.util.Locale;
import java.util.WeakHashMap;

public abstract class BasicTexture implements Texture {
    private static WeakHashMap<BasicTexture, Object> sAllTextures = new WeakHashMap();
    private static ThreadLocal<Object> sInFinalizer = new ThreadLocal();
    protected GLCanvas mCanvasRef;
    private boolean mHasBorder;
    protected int mHeight;
    protected int mId;
    protected int mState;
    private int mTextureHeight;
    private int mTextureWidth;
    protected int mWidth;

    public abstract int getTarget();

    public abstract boolean onBind(GLCanvas gLCanvas);

    protected BasicTexture(GLCanvas canvas, int id, int state) {
        this.mWidth = -1;
        this.mHeight = -1;
        this.mCanvasRef = null;
        setAssociatedCanvas(canvas);
        this.mId = id;
        this.mState = state;
        synchronized (sAllTextures) {
            sAllTextures.put(this, null);
        }
    }

    protected BasicTexture() {
        this(null, 0, 0);
    }

    protected void setAssociatedCanvas(GLCanvas canvas) {
        this.mCanvasRef = canvas;
    }

    public void setSize(int width, int height) {
        this.mWidth = width;
        this.mHeight = height;
        this.mTextureWidth = this.mWidth;
        this.mTextureHeight = this.mHeight;
        if (this.mTextureWidth > 4096 || this.mTextureHeight > 4096) {
            Log.w("BasicTexture", String.format(Locale.ENGLISH, "texture is too large: %d x %d", new Object[]{Integer.valueOf(this.mTextureWidth), Integer.valueOf(this.mTextureHeight)}), new Exception());
        }
    }

    public int getId() {
        return this.mId;
    }

    public int getWidth() {
        return this.mWidth;
    }

    public int getHeight() {
        return this.mHeight;
    }

    public int getTextureWidth() {
        return this.mTextureWidth;
    }

    public int getTextureHeight() {
        return this.mTextureHeight;
    }

    protected void setBorder(boolean hasBorder) {
        this.mHasBorder = hasBorder;
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        canvas.draw(new DrawBasicTexAttribute(this, x, y, w, h));
    }

    public boolean isLoaded() {
        return this.mState == 1;
    }

    public void recycle() {
        freeResource();
    }

    private void freeResource() {
        GLCanvas canvas = this.mCanvasRef;
        if (canvas != null && isLoaded()) {
            canvas.deleteTexture(this);
        }
        this.mState = 0;
        setAssociatedCanvas(null);
    }

    protected void finalize() {
        sInFinalizer.set(BasicTexture.class);
        recycle();
        sInFinalizer.set(null);
    }

    public static boolean inFinalizer() {
        return sInFinalizer.get() != null;
    }

    public static void invalidateAllTextures(GLCanvas canvas) {
        synchronized (sAllTextures) {
            for (BasicTexture t : sAllTextures.keySet()) {
                if (t.mCanvasRef == canvas) {
                    t.mState = 0;
                    t.setAssociatedCanvas(null);
                }
            }
        }
    }
}
