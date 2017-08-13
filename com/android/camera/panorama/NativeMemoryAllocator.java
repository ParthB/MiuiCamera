package com.android.camera.panorama;

import android.util.Log;
import java.nio.ByteBuffer;

public class NativeMemoryAllocator {
    public static final native ByteBuffer allocateBuffer(int i);

    public static final native int freeBuffer(ByteBuffer byteBuffer);

    static {
        try {
            System.loadLibrary("morpho_memory_allocator");
            Log.d("MorphoNativeMemoryAllocator", "load libmorpho_memory_allocator.so");
        } catch (UnsatisfiedLinkError e) {
            Log.d("MorphoNativeMemoryAllocator", e.getMessage());
            Log.d("MorphoNativeMemoryAllocator", "can't loadLibrary");
        }
    }
}
