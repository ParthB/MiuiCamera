package com.android.camera.steganography;

import android.graphics.Bitmap;

public class Steg {
    private final int PASS_NONE = 0;
    private final int PASS_SIMPLE_XOR = 1;
    private Bitmap inBitmap = null;
    private String key = null;
    private int passmode = 0;

    public static Steg withInput(Bitmap bitmap) {
        Steg steg = new Steg();
        steg.setInputBitmap(bitmap);
        return steg;
    }

    private void setInputBitmap(Bitmap bitmap) {
        this.inBitmap = bitmap;
    }

    public EncodedObject encode(String string) throws Exception {
        return encode(string.getBytes());
    }

    public EncodedObject encode(byte[] bytes) throws Exception {
        if (bytes.length <= bytesAvaliableInBitmap()) {
            return new EncodedObject(BitmapEncoder.encode(this.inBitmap, bytes));
        }
        throw new IllegalArgumentException("Not enough space in bitmap to hold data (max:" + bytesAvaliableInBitmap() + ")");
    }

    private int bytesAvaliableInBitmap() {
        if (this.inBitmap == null) {
            return 0;
        }
        return (((this.inBitmap.getWidth() * this.inBitmap.getHeight()) * 3) / 8) - 12;
    }
}
