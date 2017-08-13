package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import com.android.camera.Util;
import com.android.camera.preferences.IconListPreference;

public class V6SeekBar extends RelativeLayout {
    private static final int PADDING = Util.dpToPixel(0.0f);
    private TwoStateBar mBar;
    private int mBarHeight;
    private ImageView mCursor;
    private int mCursorHeight;
    private int mCursorPosition;
    private int mCursorWidth;
    private int mEndPosition;
    private float mGap;
    private int mHeight;
    private OnValueChangedListener mListener;
    private int mMaxValue = 9;
    private boolean mReLoad = false;
    private boolean mSmoothChange = true;
    private int mStartPosition;
    private int mValue = 0;
    private boolean mValueChanged = false;
    private int mWidth;

    public interface OnValueChangedListener {
        void onValueChanged(int i, boolean z);
    }

    public V6SeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mBar = new TwoStateBar(context);
        this.mBar.setContentDescription(context.getResources().getString(R.string.accessibility_seek_bar_line));
        addView(this.mBar, new LayoutParams(-1, -1));
        this.mCursor = new ImageView(context);
        this.mCursor.setImageResource(R.drawable.v6_ic_face_beauty_cursor);
        addView(this.mCursor);
        this.mBarHeight = Util.dpToPixel(1.0f);
        this.mCursorHeight = this.mCursor.getDrawable().getIntrinsicHeight();
        this.mCursorWidth = this.mCursor.getDrawable().getIntrinsicWidth();
        this.mCursorPosition = 0;
    }

    public void initialize(IconListPreference preference) {
        if (preference.getEntries() != null) {
            setMaxValue(preference.getEntries().length - 1);
            setValue(preference.findIndexOfValue(preference.getValue()));
        }
        requestLayout();
    }

    public void setOnValueChangedListener(OnValueChangedListener l) {
        this.mListener = l;
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
        this.mStartPosition = PADDING;
        this.mEndPosition = (this.mWidth - PADDING) - this.mCursorWidth;
        this.mGap = ((float) (this.mEndPosition - this.mStartPosition)) / ((float) this.mMaxValue);
        this.mCursorPosition = mapValueToPosition(this.mValue);
        requestLayout();
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.mReLoad) {
            this.mGap = ((float) (this.mEndPosition - this.mStartPosition)) / ((float) this.mMaxValue);
            this.mCursorPosition = mapValueToPosition(this.mValue);
        }
        if (this.mValueChanged || this.mReLoad) {
            this.mCursorPosition = mapValueToPosition(this.mValue);
        }
        this.mReLoad = false;
        this.mValueChanged = false;
        int barOffsetY = (this.mHeight - this.mBarHeight) / 2;
        this.mBar.layout(1, barOffsetY, this.mWidth - 1, this.mBarHeight + barOffsetY);
        int cursorOffsetY = (this.mHeight - this.mCursorHeight) / 2;
        this.mCursor.layout(this.mCursorPosition, cursorOffsetY, this.mCursorPosition + this.mCursorWidth, this.mCursorHeight + cursorOffsetY);
        this.mBar.setStatePosition(1, this.mCursorPosition - this.mBarHeight, (this.mCursorPosition + this.mCursorWidth) + this.mBarHeight, this.mWidth - 1);
    }

    public boolean dispatchTouchEvent(MotionEvent event) {
        int action = event.getAction();
        if (!isEnabled() && action != 4 && action != 3 && action != 1) {
            return false;
        }
        switch (action) {
            case 0:
                setActivated(true);
                notifyChange(false);
                break;
            case 1:
            case 3:
            case 4:
                setActivated(false);
                this.mCursorPosition = clip((int) ((((float) ((int) ((((event.getX() - ((float) this.mStartPosition)) - (((float) this.mCursorWidth) / 2.0f)) / this.mGap) + 0.5f))) * this.mGap) + ((float) this.mStartPosition)), this.mEndPosition, this.mStartPosition);
                requestLayout();
                this.mValue = mapPositionToValue(this.mCursorPosition);
                notifyChange(true);
                break;
            case 2:
                break;
        }
        int adsorbPos = (int) ((((float) ((int) ((((event.getX() - ((float) this.mStartPosition)) - (((float) this.mCursorWidth) / 2.0f)) / this.mGap) + 0.5f))) * this.mGap) + ((float) this.mStartPosition));
        int noAdsorbPos = (int) ((event.getX() - (((float) this.mCursorWidth) / 2.0f)) + 0.5f);
        int pos = noAdsorbPos;
        if ((this.mMaxValue > 3 || ((float) Math.abs(adsorbPos - noAdsorbPos)) >= this.mGap / 4.0f) && (3 >= this.mMaxValue || this.mMaxValue > 30)) {
            pos = noAdsorbPos;
        } else {
            pos = adsorbPos;
        }
        this.mCursorPosition = clip(pos, this.mEndPosition, this.mStartPosition);
        requestLayout();
        if (this.mSmoothChange) {
            this.mValue = mapPositionToValue(this.mCursorPosition);
            notifyChange(false);
        }
        return true;
    }

    private void notifyChange(boolean touchUp) {
        if (this.mListener != null) {
            this.mListener.onValueChanged(this.mValue, touchUp);
        }
    }

    public void setMaxValue(int max) {
        if (max > 0) {
            this.mMaxValue = max;
            this.mReLoad = true;
            requestLayout();
        }
    }

    public void setValue(int value) {
        if (this.mValue != value) {
            this.mValue = clip(value, this.mMaxValue, 0);
            requestLayout();
            this.mValueChanged = true;
        }
    }

    private int clip(int value, int max, int min) {
        if (value > max) {
            return max;
        }
        if (value < min) {
            return min;
        }
        return value;
    }

    private int mapPositionToValue(int position) {
        if (1 == getLayoutDirection()) {
            return clip((int) ((((float) (this.mEndPosition - position)) / this.mGap) + 0.5f), this.mMaxValue, 0);
        }
        return clip((int) ((((float) (position - this.mStartPosition)) / this.mGap) + 0.5f), this.mMaxValue, 0);
    }

    private int mapValueToPosition(int value) {
        if (1 == getLayoutDirection()) {
            return clip(this.mEndPosition - ((int) ((((float) value) * this.mGap) + 0.5f)), this.mEndPosition, this.mStartPosition);
        }
        return clip(((int) ((((float) value) * this.mGap) + 0.5f)) + this.mStartPosition, this.mEndPosition, this.mStartPosition);
    }
}
