package com.adobe.xmp.impl;

import java.io.IOException;
import java.io.OutputStream;

public final class CountOutputStream extends OutputStream {
    private int bytesWritten = 0;
    private final OutputStream out;

    CountOutputStream(OutputStream out) {
        this.out = out;
    }

    public void write(byte[] buf, int off, int len) throws IOException {
        this.out.write(buf, off, len);
        this.bytesWritten += len;
    }

    public void write(byte[] buf) throws IOException {
        this.out.write(buf);
        this.bytesWritten += buf.length;
    }

    public void write(int b) throws IOException {
        this.out.write(b);
        this.bytesWritten++;
    }

    public int getBytesWritten() {
        return this.bytesWritten;
    }
}
