package com.android.gallery3d.ui;

import android.opengl.GLES20;

public class GLId {
    private static int sNextId = 1;

    public static synchronized void glGenTextures(int n, int[] textures, int offset) {
        synchronized (GLId.class) {
            while (true) {
                int n2 = n - 1;
                if (n > 0) {
                    int i = offset + n2;
                    int i2 = sNextId;
                    sNextId = i2 + 1;
                    textures[i] = i2;
                    n = n2;
                }
            }
        }
    }

    public static synchronized void glGenFrameBuffers(int n, int[] buffers, int offset) {
        synchronized (GLId.class) {
            while (true) {
                int n2 = n - 1;
                if (n > 0) {
                    int i = offset + n2;
                    int i2 = sNextId;
                    sNextId = i2 + 1;
                    buffers[i] = i2;
                    n = n2;
                }
            }
        }
    }

    public static void glDeleteTextures(int n, int[] textures, int offset) {
        GLES20.glDeleteTextures(n, textures, offset);
    }

    public static void glDeleteBuffers(int n, int[] buffers, int offset) {
        GLES20.glDeleteBuffers(n, buffers, offset);
    }

    public static void glDeleteFrameBuffers(int n, int[] buffers, int offset) {
        GLES20.glDeleteFramebuffers(n, buffers, offset);
    }
}
