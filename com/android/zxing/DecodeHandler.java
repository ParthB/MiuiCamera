package com.android.zxing;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.recyclerview.R;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import java.util.Hashtable;

final class DecodeHandler extends Handler {
    private boolean mCancel;
    private final Context mContext;
    private final MultiFormatReader mMultiFormatReader = new MultiFormatReader();

    public DecodeHandler(Context context, Looper looper, Hashtable<DecodeHintType, Object> hints) {
        super(looper);
        this.mContext = context;
        this.mMultiFormatReader.setHints(hints);
    }

    public void handleMessage(Message message) {
        switch (message.what) {
            case R.id.decode:
                decode((byte[]) message.obj, message.arg1, message.arg2);
                return;
            default:
                return;
        }
    }

    public void cancel() {
        this.mCancel = true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void decode(byte[] r17, int r18, int r19) {
        /*
        r16 = this;
        r0 = r16;
        r12 = r0.mCancel;
        if (r12 != 0) goto L_0x0014;
    L_0x0006:
        r12 = r18 * r19;
        r12 = (double) r12;
        r14 = 4609434218613702656; // 0x3ff8000000000000 float:0.0 double:1.5;
        r12 = r12 * r14;
        r0 = r17;
        r14 = r0.length;
        r14 = (double) r14;
        r12 = (r12 > r14 ? 1 : (r12 == r14 ? 0 : -1));
        if (r12 == 0) goto L_0x0015;
    L_0x0014:
        return;
    L_0x0015:
        r12 = r18 * r19;
        r7 = new byte[r12];
        r11 = 0;
    L_0x001a:
        r0 = r19;
        if (r11 >= r0) goto L_0x0037;
    L_0x001e:
        r10 = 0;
    L_0x001f:
        r0 = r18;
        if (r10 >= r0) goto L_0x0034;
    L_0x0023:
        r12 = r10 * r19;
        r12 = r12 + r19;
        r12 = r12 - r11;
        r12 = r12 + -1;
        r13 = r11 * r18;
        r13 = r13 + r10;
        r13 = r17[r13];
        r7[r12] = r13;
        r10 = r10 + 1;
        goto L_0x001f;
    L_0x0034:
        r11 = r11 + 1;
        goto L_0x001a;
    L_0x0037:
        r9 = r18;
        r18 = r19;
        r19 = r9;
        r2 = 0;
        r3 = 2;
        r8 = 0;
        r5 = 0;
    L_0x0041:
        r3 = r3 + -1;
        if (r3 < 0) goto L_0x00ac;
    L_0x0045:
        r0 = r16;
        r12 = r0.mCancel;
        if (r12 == 0) goto L_0x004c;
    L_0x004b:
        return;
    L_0x004c:
        r0 = r16;
        r12 = r0.mContext;
        r13 = com.android.zxing.QRCodeManager.instance(r12);
        if (r3 != 0) goto L_0x0081;
    L_0x0056:
        r12 = 1;
    L_0x0057:
        r0 = r18;
        r8 = r13.buildLuminanceSource(r7, r0, r9, r12);
        if (r8 == 0) goto L_0x0041;
    L_0x005f:
        r2 = new com.google.zxing.BinaryBitmap;
        r12 = new com.google.zxing.common.HybridBinarizer;
        r12.<init>(r8);
        r2.<init>(r12);
        r0 = r16;
        r12 = r0.mMultiFormatReader;	 Catch:{ ReaderException -> 0x0083, all -> 0x008c }
        r5 = r12.decodeWithState(r2);	 Catch:{ ReaderException -> 0x0083, all -> 0x008c }
        r0 = r16;
        r12 = r0.mMultiFormatReader;
        r12.reset();
    L_0x0078:
        if (r5 == 0) goto L_0x0041;
    L_0x007a:
        r0 = r16;
        r12 = r0.mCancel;
        if (r12 == 0) goto L_0x0095;
    L_0x0080:
        return;
    L_0x0081:
        r12 = 0;
        goto L_0x0057;
    L_0x0083:
        r6 = move-exception;
        r0 = r16;
        r12 = r0.mMultiFormatReader;
        r12.reset();
        goto L_0x0078;
    L_0x008c:
        r12 = move-exception;
        r0 = r16;
        r13 = r0.mMultiFormatReader;
        r13.reset();
        throw r12;
    L_0x0095:
        r0 = r16;
        r12 = r0.mContext;
        r12 = com.android.zxing.QRCodeManager.instance(r12);
        r12 = r12.getHandler();
        r13 = 2131296263; // 0x7f090007 float:1.8210438E38 double:1.0530002647E-314;
        r4 = r12.obtainMessage(r13, r5);
        r4.sendToTarget();
        return;
    L_0x00ac:
        r0 = r16;
        r12 = r0.mCancel;
        if (r12 == 0) goto L_0x00b3;
    L_0x00b2:
        return;
    L_0x00b3:
        r0 = r16;
        r12 = r0.mContext;
        r12 = com.android.zxing.QRCodeManager.instance(r12);
        r12 = r12.getHandler();
        r13 = 2131296265; // 0x7f090009 float:1.8210442E38 double:1.0530002656E-314;
        r12.sendEmptyMessage(r13);
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.zxing.DecodeHandler.decode(byte[], int, int):void");
    }
}
