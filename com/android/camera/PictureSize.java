package com.android.camera;

import android.hardware.Camera.Size;

public class PictureSize implements Comparable<PictureSize> {
    public int height;
    public int width;

    public PictureSize() {
        setPictureSize(0, 0);
    }

    public PictureSize(int w, int h) {
        setPictureSize(w, h);
    }

    public PictureSize(String value) {
        setPictureSize(value);
    }

    public PictureSize setPictureSize(String value) {
        int index = value == null ? -1 : value.indexOf(120);
        if (index == -1) {
            this.width = 0;
            this.height = 0;
        } else {
            this.width = Integer.parseInt(value.substring(0, index));
            this.height = Integer.parseInt(value.substring(index + 1));
        }
        return this;
    }

    public PictureSize setPictureSize(int w, int h) {
        this.width = w;
        this.height = h;
        return this;
    }

    public PictureSize setPictureSize(Size size) {
        if (size != null) {
            this.width = size.width;
            this.height = size.height;
        } else {
            this.width = 0;
            this.height = 0;
        }
        return this;
    }

    public boolean isEmpty() {
        return this.width * this.height <= 0;
    }

    public int area() {
        return isEmpty() ? 0 : this.width * this.height;
    }

    public boolean isAspectRatio18_9() {
        if (isEmpty()) {
            return false;
        }
        return CameraSettings.isAspectRatio18_9(this.width, this.height);
    }

    public boolean isAspectRatio16_9() {
        if (isEmpty()) {
            return false;
        }
        return CameraSettings.isAspectRatio16_9(this.width, this.height);
    }

    public boolean isAspectRatio4_3() {
        if (isEmpty()) {
            return false;
        }
        return CameraSettings.isAspectRatio4_3(this.width, this.height);
    }

    public boolean isAspectRatio1_1() {
        if (isEmpty()) {
            return false;
        }
        return CameraSettings.isAspectRatio1_1(this.width, this.height);
    }

    public int compareTo(PictureSize another) {
        if (another.width == this.width && another.height == this.height) {
            return 0;
        }
        return -1;
    }

    public String toString() {
        return this.width + "x" + this.height;
    }
}
