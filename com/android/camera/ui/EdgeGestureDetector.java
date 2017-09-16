package com.android.camera.ui;

import android.os.SystemProperties;
import android.view.MotionEvent.PointerCoords;
import com.android.camera.Util;
import java.util.ArrayList;

public class EdgeGestureDetector {
    private final int TAP_TIMEOUT = SystemProperties.getInt("tap_timeout", 400);
    private final int TAP_TO_TOUCH_TIME = SystemProperties.getInt("tap_to_touch_min_time", 100);
    private final int TOUCH_SLOP_SQUARE = SystemProperties.getInt("edgetouch_slop_quare", Util.dpToPixel(66.67f) * Util.dpToPixel(66.67f));
    private EdgeGestureListener mEdgeGestureListener;
    private final ArrayList<PointerState> mPointers = new ArrayList();
    private boolean mPrintCoords = true;
    private final PointerCoords mTempCoords = new PointerCoords();
    private final FasterStringBuilder mText = new FasterStringBuilder();

    public interface EdgeGestureListener {
    }

    private static final class FasterStringBuilder {
        private char[] mChars = new char[64];
        private int mLength;

        public String toString() {
            return new String(this.mChars, 0, this.mLength);
        }
    }

    public EdgeGestureDetector(EdgeGestureListener listener) {
        this.mEdgeGestureListener = listener;
    }
}
