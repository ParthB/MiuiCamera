package com.android.camera;

import com.android.camera.panorama.NativeMemoryAllocator;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PanoUtil {
    public static ByteBuffer createByteBuffer(byte[] src) {
        ByteBuffer bb = NativeMemoryAllocator.allocateBuffer(src.length);
        bb.order(ByteOrder.nativeOrder());
        bb.position(0);
        bb.put(src);
        bb.position(0);
        return bb;
    }
}
