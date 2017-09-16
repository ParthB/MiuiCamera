package com.android.camera.steganography;

import android.graphics.Bitmap;

public class EncodedObject {
    private final Bitmap bitmap;

    public EncodedObject(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap intoBitmap() {
        return this.bitmap;
    }
}
