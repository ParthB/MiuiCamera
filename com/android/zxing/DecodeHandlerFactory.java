package com.android.zxing;

import android.content.Context;
import android.os.HandlerThread;
import android.support.v7.recyclerview.R;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import java.util.Hashtable;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

final class DecodeHandlerFactory {
    private final Context mContext;
    private DecodeHandler mHandler;
    private final CountDownLatch mHandlerInitLatch = new CountDownLatch(1);
    private final Hashtable<DecodeHintType, Object> mHints = new Hashtable(1);

    public DecodeHandlerFactory(Context context, boolean all) {
        this.mContext = context;
        Vector<BarcodeFormat> decodeFormats = new Vector();
        decodeFormats.addAll(DecodeFormats.QR_CODE_FORMATS);
        if (all) {
            decodeFormats.addAll(DecodeFormats.ONE_D_FORMATS);
            decodeFormats.addAll(DecodeFormats.DATA_MATRIX_FORMATS);
        }
        this.mHints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
    }

    public DecodeHandler getHandler() {
        try {
            this.mHandlerInitLatch.await();
        } catch (InterruptedException e) {
        }
        return this.mHandler;
    }

    public void start() {
        HandlerThread handlerThread = new HandlerThread("DecodeThread");
        handlerThread.start();
        this.mHandler = new DecodeHandler(this.mContext, handlerThread.getLooper(), this.mHints);
        this.mHandlerInitLatch.countDown();
    }

    public void quit() {
        getHandler().cancel();
        getHandler().removeMessages(R.id.decode);
        getHandler().getLooper().quit();
    }
}
