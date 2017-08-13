package com.android.camera.ui;

import android.os.SystemProperties;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import com.android.camera.Log;
import com.android.camera.Util;
import java.util.ArrayList;
import java.util.Locale;

public class EdgeGestureDetector {
    private final int TAP_TIMEOUT = SystemProperties.getInt("tap_timeout", 400);
    private final int TAP_TO_TOUCH_TIME = SystemProperties.getInt("tap_to_touch_min_time", 100);
    private final int TOUCH_SLOP_SQUARE = SystemProperties.getInt("edgetouch_slop_quare", Util.dpToPixel(66.67f) * Util.dpToPixel(66.67f));
    private boolean mCurDown;
    private int mCurNumPointers;
    private EdgeGestureListener mEdgeGestureListener;
    private long mLastTapEvent;
    private int mMaxNumPointers;
    private final ArrayList<PointerState> mPointers = new ArrayList();
    private boolean mPrintCoords = true;
    private final PointerCoords mTempCoords = new PointerCoords();
    private final FasterStringBuilder mText = new FasterStringBuilder();

    public interface EdgeGestureListener {
        boolean onEdgeTap(int i, int i2);

        boolean onEdgeTouch(int i, int i2);
    }

    private static final class FasterStringBuilder {
        private char[] mChars = new char[64];
        private int mLength;

        public FasterStringBuilder clear() {
            this.mLength = 0;
            return this;
        }

        public FasterStringBuilder append(String value) {
            int valueLength = value.length();
            value.getChars(0, valueLength, this.mChars, reserve(valueLength));
            this.mLength += valueLength;
            return this;
        }

        public FasterStringBuilder append(int value) {
            return append(value, 0);
        }

        public FasterStringBuilder append(int value, int zeroPadWidth) {
            boolean negative = false;
            if (value < 0) {
                negative = true;
            }
            if (negative) {
                value = -value;
                if (value < 0) {
                    append("-2147483648");
                    return this;
                }
            }
            int index = reserve(11);
            char[] chars = this.mChars;
            if (value == 0) {
                int index2 = index + 1;
                chars[index] = '0';
                this.mLength++;
                return this;
            }
            if (negative) {
                index2 = index + 1;
                chars[index] = '-';
                index = index2;
            }
            int divisor = 1000000000;
            int numberWidth = 10;
            index2 = index;
            while (value < divisor) {
                divisor /= 10;
                numberWidth--;
                if (numberWidth < zeroPadWidth) {
                    index = index2 + 1;
                    chars[index2] = '0';
                } else {
                    index = index2;
                }
                index2 = index;
            }
            do {
                index = index2;
                int digit = value / divisor;
                value -= digit * divisor;
                divisor /= 10;
                index2 = index + 1;
                chars[index] = (char) (digit + 48);
            } while (divisor != 0);
            this.mLength = index2;
            return this;
        }

        public FasterStringBuilder append(float value, int precision) {
            int scale = 1;
            for (int i = 0; i < precision; i++) {
                scale *= 10;
            }
            value = (float) (Math.rint((double) (((float) scale) * value)) / ((double) scale));
            append((int) value);
            if (precision != 0) {
                append(".");
                value = Math.abs(value);
                append((int) (((float) scale) * ((float) (((double) value) - Math.floor((double) value)))), precision);
            }
            return this;
        }

        public String toString() {
            return new String(this.mChars, 0, this.mLength);
        }

        private int reserve(int length) {
            int oldLength = this.mLength;
            int newLength = this.mLength + length;
            char[] oldChars = this.mChars;
            int oldCapacity = oldChars.length;
            if (newLength > oldCapacity) {
                char[] newChars = new char[(oldCapacity * 2)];
                System.arraycopy(oldChars, 0, newChars, 0, oldLength);
                this.mChars = newChars;
            }
            return oldLength;
        }
    }

    public static class PointerState {
        private PointerCoords mCoords = new PointerCoords();
        private boolean mCurDown;
        private float mDownFocusX;
        private float mDownFocusY;
        private long mDownTime;
        private boolean mHandleDown;
        private boolean mMoving;

        public String toString() {
            return String.format(Locale.ENGLISH, "PointerState mDownFocusX=%f mDownFocusY=%f mDownTime=%d mCurDown=%b mHandleDown=%b  mMoving=%b", new Object[]{Float.valueOf(this.mDownFocusX), Float.valueOf(this.mDownFocusY), Long.valueOf(this.mDownTime), Boolean.valueOf(this.mCurDown), Boolean.valueOf(this.mHandleDown), Boolean.valueOf(this.mMoving)});
        }
    }

    public EdgeGestureDetector(EdgeGestureListener listener) {
        this.mEdgeGestureListener = listener;
    }

    private void detectMoving(float currentX, float currentY, PointerState ps) {
        if (!ps.mMoving) {
            int deltaX = (int) (currentX - ps.mDownFocusX);
            int deltaY = (int) (currentY - ps.mDownFocusY);
            if ((deltaX * deltaX) + (deltaY * deltaY) > this.TOUCH_SLOP_SQUARE) {
                Log.v("EdgeGestureDetector", String.format(Locale.ENGLISH, "detectMoving success currentX=%f currentY=%f distance=%d ps=%s TOUCH_SLOP_SQUARE=%d", new Object[]{Float.valueOf(currentX), Float.valueOf(currentY), Integer.valueOf((deltaX * deltaX) + (deltaY * deltaY)), ps, Integer.valueOf(this.TOUCH_SLOP_SQUARE)}));
                ps.mMoving = true;
            }
        }
    }

    private void detectTap(float currentX, float currentY, long upTime, PointerState ps) {
        Log.v("EdgeGestureDetector", String.format(Locale.ENGLISH, "detectTap currentX=%f currentY=%f upTime=%d ps=%s TAP_TIMEOUT=%d", new Object[]{Float.valueOf(currentX), Float.valueOf(currentY), Long.valueOf(upTime), ps, Integer.valueOf(this.TAP_TIMEOUT)}));
        if (!ps.mHandleDown && !ps.mMoving && ps.mCurDown) {
            if (!Util.isTimeout(upTime, ps.mDownTime, (long) this.TAP_TIMEOUT)) {
                Log.v("EdgeGestureDetector", "detectTap sucess");
                if (this.mEdgeGestureListener.onEdgeTap((int) currentX, (int) currentY)) {
                    this.mLastTapEvent = upTime;
                }
            }
        }
    }

    private void detectTouch(float currentX, float currentY, long eventTime, PointerState ps) {
        Log.v("EdgeGestureDetector", String.format(Locale.ENGLISH, "detectTouch currentX=%f currentY=%f ps=%s eventTime=%d mLastTapEvent=%d", new Object[]{Float.valueOf(currentX), Float.valueOf(currentY), ps, Long.valueOf(eventTime), Long.valueOf(this.mLastTapEvent)}));
        if (ps.mCurDown) {
            if (Util.isTimeout(eventTime, this.mLastTapEvent, (long) this.TAP_TO_TOUCH_TIME)) {
                ps.mHandleDown = this.mEdgeGestureListener.onEdgeTouch((int) currentX, (int) currentY);
            }
        }
    }

    public void onTouchEvent(MotionEvent event) {
        int index;
        int id;
        int action = event.getAction();
        int NP = this.mPointers.size();
        if (action == 0 || (action & 255) == 5) {
            index = (65280 & action) >> 8;
            if (action == 0) {
                for (int p = 0; p < NP; p++) {
                    ((PointerState) this.mPointers.get(p)).mCurDown = false;
                }
                this.mCurDown = true;
                this.mCurNumPointers = 0;
                this.mMaxNumPointers = 0;
            }
            this.mCurNumPointers++;
            if (this.mMaxNumPointers < this.mCurNumPointers) {
                this.mMaxNumPointers = this.mCurNumPointers;
            }
            id = event.getPointerId(index);
            while (NP <= id) {
                this.mPointers.add(new PointerState());
                NP++;
            }
            PointerState ps = (PointerState) this.mPointers.get(id);
            ps.mCurDown = true;
            ps.mMoving = false;
            ps.mHandleDown = false;
            ps.mDownTime = event.getEventTime();
            ps.mDownFocusX = event.getX(index);
            ps.mDownFocusY = event.getY(index);
            detectTouch(event.getX(index), event.getY(index), event.getEventTime(), ps);
            Log.v("EdgeGestureDetector", String.format(Locale.ENGLISH, "new TouchDown event ps=%s mMaxNumPointers=%d mCurNumPointers=%d action=%d index=%d id=%d", new Object[]{ps, Integer.valueOf(this.mMaxNumPointers), Integer.valueOf(this.mCurNumPointers), Integer.valueOf(action), Integer.valueOf(index), Integer.valueOf(id)}));
        }
        int NI = event.getPointerCount();
        int N = event.getHistorySize();
        for (int historyPos = 0; historyPos < N; historyPos++) {
            int i;
            for (i = 0; i < NI; i++) {
                PointerCoords coords;
                id = event.getPointerId(i);
                if (id < this.mPointers.size()) {
                    ps = this.mCurDown ? (PointerState) this.mPointers.get(id) : null;
                    if (ps != null) {
                        coords = ps.mCoords;
                    } else {
                        coords = this.mTempCoords;
                    }
                    event.getHistoricalPointerCoords(i, historyPos, coords);
                    if (this.mPrintCoords) {
                        logCoords("Pointer", action, i, coords, id, event);
                    }
                    if (ps != null) {
                        detectMoving(coords.x, coords.y, ps);
                    }
                }
            }
        }
        for (i = 0; i < NI; i++) {
            id = event.getPointerId(i);
            if (id < this.mPointers.size()) {
                ps = this.mCurDown ? (PointerState) this.mPointers.get(id) : null;
                coords = ps != null ? ps.mCoords : this.mTempCoords;
                event.getPointerCoords(i, coords);
                if (this.mPrintCoords) {
                    logCoords("Pointer", action, i, coords, id, event);
                }
                if (ps != null) {
                    detectMoving(coords.x, coords.y, ps);
                }
            }
        }
        if (!(action == 1 || action == 3)) {
            if ((action & 255) != 6) {
                return;
            }
        }
        index = (65280 & action) >> 8;
        id = event.getPointerId(index);
        if (id < this.mPointers.size()) {
            ps = (PointerState) this.mPointers.get(id);
            Log.v("EdgeGestureDetector", String.format(Locale.ENGLISH, "new TouchUp event ps=%s mMaxNumPointers=%d mCurNumPointers=%d action=%d index=%d id=%d", new Object[]{ps, Integer.valueOf(this.mMaxNumPointers), Integer.valueOf(this.mCurNumPointers), Integer.valueOf(action), Integer.valueOf(index), Integer.valueOf(id)}));
            detectTap(event.getX(index), event.getY(index), event.getEventTime(), ps);
            ps.mCurDown = false;
        }
        if (action == 1 || action == 3) {
            this.mCurDown = false;
            this.mCurNumPointers = 0;
        } else if (id < this.mPointers.size()) {
            this.mCurNumPointers--;
        }
    }

    private void logCoords(String type, int action, int index, PointerCoords coords, int id, MotionEvent event) {
        String prefix;
        int toolType = event.getToolType(index);
        int buttonState = event.getButtonState();
        switch (action & 255) {
            case 0:
                prefix = "DOWN";
                break;
            case 1:
                prefix = "UP";
                break;
            case 2:
                prefix = "MOVE";
                break;
            case 3:
                prefix = "CANCEL";
                break;
            case 4:
                prefix = "OUTSIDE";
                break;
            case 5:
                if (index != ((65280 & action) >> 8)) {
                    prefix = "MOVE";
                    break;
                } else {
                    prefix = "DOWN";
                    break;
                }
            case 6:
                if (index != ((65280 & action) >> 8)) {
                    prefix = "MOVE";
                    break;
                } else {
                    prefix = "UP";
                    break;
                }
            case 7:
                prefix = "HOVER MOVE";
                break;
            case 8:
                prefix = "SCROLL";
                break;
            case 9:
                prefix = "HOVER ENTER";
                break;
            case 10:
                prefix = "HOVER EXIT";
                break;
            default:
                prefix = Integer.toString(action);
                break;
        }
        android.util.Log.i("EdgeGestureDetector", this.mText.clear().append(type).append(" id ").append(id + 1).append(": ").append(prefix).append(" (").append(coords.x, 3).append(", ").append(coords.y, 3).append(") Pressure=").append(coords.pressure, 3).append(" Size=").append(coords.size, 3).append(" TouchMajor=").append(coords.touchMajor, 3).append(" TouchMinor=").append(coords.touchMinor, 3).append(" ToolMajor=").append(coords.toolMajor, 3).append(" ToolMinor=").append(coords.toolMinor, 3).append(" Orientation=").append((float) (((double) (coords.orientation * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Tilt=").append((float) (((double) (coords.getAxisValue(25) * 180.0f)) / 3.141592653589793d), 1).append("deg").append(" Distance=").append(coords.getAxisValue(24), 1).append(" VScroll=").append(coords.getAxisValue(9), 1).append(" HScroll=").append(coords.getAxisValue(10), 1).append(" BoundingBox=[(").append(event.getAxisValue(32), 3).append(", ").append(event.getAxisValue(33), 3).append(")").append(", (").append(event.getAxisValue(34), 3).append(", ").append(event.getAxisValue(35), 3).append(")]").append(" ToolType=").append(MotionEvent.toolTypeToString(toolType)).append(" ButtonState=").append(MotionEvent.buttonStateToString(buttonState)).toString());
    }
}
