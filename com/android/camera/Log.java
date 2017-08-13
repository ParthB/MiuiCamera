package com.android.camera;

public class Log {
    public static int v(String tag, String msg) {
        if (Util.sIsDumpLog) {
            return android.util.Log.v(tag, msg);
        }
        return -1;
    }

    public static int d(String tag, String msg) {
        return android.util.Log.d(tag, msg);
    }

    public static int i(String tag, String msg) {
        return android.util.Log.i(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return android.util.Log.w(tag, msg, tr);
    }

    public static int e(String tag, String msg) {
        return android.util.Log.e(tag, msg);
    }
}
