package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import java.util.HashMap;

public abstract class UploadedTexture extends BasicTexture {
    private static BorderKey sBorderKey = new BorderKey();
    private static HashMap<BorderKey, Bitmap> sBorderLines = new HashMap();
    static float[] sCropRect = new float[4];
    static int[] sTextureId = new int[1];
    private static int sUploadedCount;
    protected Bitmap mBitmap;
    private int mBorder;
    private boolean mContentValid;
    private boolean mIsUploading;
    private boolean mOpaque;
    private boolean mThrottled;

    private static class BorderKey implements Cloneable {
        public Config config;
        public int length;
        public boolean vertical;

        private BorderKey() {
        }

        public int hashCode() {
            int x = this.config.hashCode() ^ this.length;
            return this.vertical ? x : -x;
        }

        public boolean equals(Object object) {
            boolean z = false;
            if (!(object instanceof BorderKey)) {
                return false;
            }
            BorderKey o = (BorderKey) object;
            if (this.vertical == o.vertical && this.config == o.config && this.length == o.length) {
                z = true;
            }
            return z;
        }

        public BorderKey clone() {
            try {
                return (BorderKey) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(e);
            }
        }
    }

    protected abstract void onFreeBitmap(Bitmap bitmap);

    protected abstract Bitmap onGetBitmap();

    protected UploadedTexture() {
        this(false);
    }

    protected UploadedTexture(boolean hasBorder) {
        super(null, 0, 0);
        this.mContentValid = true;
        this.mIsUploading = false;
        this.mOpaque = true;
        this.mThrottled = false;
        if (hasBorder) {
            setBorder(true);
            this.mBorder = 1;
        }
    }

    private static Bitmap getBorderLine(boolean vertical, Config config, int length) {
        BorderKey key = sBorderKey;
        key.vertical = vertical;
        key.config = config;
        key.length = length;
        Bitmap bitmap = (Bitmap) sBorderLines.get(key);
        if (bitmap == null) {
            if (vertical) {
                bitmap = Bitmap.createBitmap(1, length, config);
            } else {
                bitmap = Bitmap.createBitmap(length, 1, config);
            }
            sBorderLines.put(key.clone(), bitmap);
        }
        return bitmap;
    }

    private Bitmap getBitmap() {
        if (this.mBitmap == null) {
            this.mBitmap = onGetBitmap();
            int w = this.mBitmap.getWidth() + (this.mBorder * 2);
            int h = this.mBitmap.getHeight() + (this.mBorder * 2);
            if (this.mWidth == -1) {
                setSize(w, h);
            }
        }
        return this.mBitmap;
    }

    private void freeBitmap() {
        Utils.assertTrue(this.mBitmap != null);
        onFreeBitmap(this.mBitmap);
        this.mBitmap = null;
    }

    public int getWidth() {
        if (this.mWidth == -1) {
            getBitmap();
        }
        return this.mWidth;
    }

    public int getHeight() {
        if (this.mWidth == -1) {
            getBitmap();
        }
        return this.mHeight;
    }

    public boolean isContentValid() {
        return isLoaded() ? this.mContentValid : false;
    }

    public void updateContent(GLCanvas canvas) {
        if (!isLoaded()) {
            if (this.mThrottled) {
                int i = sUploadedCount + 1;
                sUploadedCount = i;
                if (i > 100) {
                    return;
                }
            }
            uploadToCanvas(canvas);
        } else if (!this.mContentValid) {
            Bitmap bitmap = getBitmap();
            int format = GLUtils.getInternalFormat(bitmap);
            int type = GLUtils.getType(bitmap);
            GLES20.glBindTexture(3553, this.mId);
            GLUtils.texSubImage2D(3553, 0, this.mBorder, this.mBorder, bitmap, format, type);
            freeBitmap();
            this.mContentValid = true;
        }
    }

    public static void resetUploadLimit() {
        sUploadedCount = 0;
    }

    public static boolean uploadLimitReached() {
        return sUploadedCount > 100;
    }

    private void uploadToCanvas(GLCanvas canvas) {
        Bitmap bitmap = getBitmap();
        if (bitmap != null) {
            try {
                int bWidth = bitmap.getWidth();
                int bHeight = bitmap.getHeight();
                int width = bWidth + (this.mBorder * 2);
                int height = bHeight + (this.mBorder * 2);
                int texWidth = getTextureWidth();
                int texHeight = getTextureHeight();
                boolean z = bWidth <= texWidth && bHeight <= texHeight;
                Utils.assertTrue(z);
                sCropRect[0] = (float) this.mBorder;
                sCropRect[1] = (float) (this.mBorder + bHeight);
                sCropRect[2] = (float) bWidth;
                sCropRect[3] = (float) (-bHeight);
                GLId.glGenTextures(1, sTextureId, 0);
                GLES20.glBindTexture(3553, sTextureId[0]);
                GLES20.glTexParameterfv(3553, 35741, sCropRect, 0);
                GLES20.glTexParameteri(3553, 10242, 33071);
                GLES20.glTexParameteri(3553, 10243, 33071);
                GLES20.glTexParameterf(3553, 10241, 9729.0f);
                GLES20.glTexParameterf(3553, 10240, 9729.0f);
                if (bWidth == texWidth && bHeight == texHeight) {
                    GLUtils.texImage2D(3553, 0, bitmap, 0);
                } else {
                    int format = GLUtils.getInternalFormat(bitmap);
                    int type = GLUtils.getType(bitmap);
                    Config config = bitmap.getConfig();
                    GLES20.glTexImage2D(3553, 0, format, texWidth, texHeight, 0, format, type, null);
                    GLUtils.texSubImage2D(3553, 0, this.mBorder, this.mBorder, bitmap, format, type);
                    if (this.mBorder > 0) {
                        GLUtils.texSubImage2D(3553, 0, 0, 0, getBorderLine(true, config, texHeight), format, type);
                        GLUtils.texSubImage2D(3553, 0, 0, 0, getBorderLine(false, config, texWidth), format, type);
                    }
                    if (this.mBorder + bWidth < texWidth) {
                        GLUtils.texSubImage2D(3553, 0, this.mBorder + bWidth, 0, getBorderLine(true, config, texHeight), format, type);
                    }
                    if (this.mBorder + bHeight < texHeight) {
                        GLUtils.texSubImage2D(3553, 0, 0, this.mBorder + bHeight, getBorderLine(false, config, texWidth), format, type);
                    }
                }
                freeBitmap();
                setAssociatedCanvas(canvas);
                this.mId = sTextureId[0];
                this.mState = 1;
                this.mContentValid = true;
            } catch (Throwable th) {
                freeBitmap();
            }
        } else {
            this.mState = -1;
            throw new RuntimeException("Texture load fail, no bitmap");
        }
    }

    public boolean onBind(GLCanvas canvas) {
        updateContent(canvas);
        return isContentValid();
    }

    public int getTarget() {
        return 3553;
    }

    public void setOpaque(boolean isOpaque) {
        this.mOpaque = isOpaque;
    }

    public boolean isOpaque() {
        return this.mOpaque;
    }

    public void recycle() {
        super.recycle();
        if (this.mBitmap != null) {
            freeBitmap();
        }
    }
}
