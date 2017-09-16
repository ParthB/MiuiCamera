package com.android.gallery3d.ui;

import android.graphics.Bitmap;

public class BitmapTexture extends UploadedTexture {
    protected Bitmap mContentBitmap;

    public BitmapTexture(Bitmap bitmap) {
        this(bitmap, false);
    }

    private BitmapTexture(Bitmap bitmap, boolean hasBorder) {
        boolean z = false;
        super(hasBorder);
        if (!(bitmap == null || bitmap.isRecycled())) {
            z = true;
        }
        Utils.assertTrue(z);
        this.mContentBitmap = bitmap;
    }

    protected void onFreeBitmap(Bitmap bitmap) {
    }

    protected Bitmap onGetBitmap() {
        return this.mContentBitmap;
    }

    public void draw(GLCanvas canvas, int x, int y, int w, int h) {
        int width = this.mContentBitmap.getWidth();
        int height = this.mContentBitmap.getHeight();
        if (width * h != w * height) {
            int newX = x;
            int newY = y;
            int newW = w;
            int newH = h;
            if (width * h > w * height) {
                newW = (h * width) / height;
                newH = h;
                newX = x + ((w - newW) / 2);
                newY = y;
            } else {
                newW = w;
                newH = (w * height) / width;
                newX = x;
                newY = y + ((h - newH) / 2);
            }
            super.draw(canvas, newX, newY, newW, newH);
            return;
        }
        super.draw(canvas, x, y, w, h);
    }
}
