package com.android.camera.ui;

import android.content.Context;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.preferences.IconListPreference;

public class V6SeekbarPopupTexts extends RelativeLayout implements Rotatable {
    private float mGap;
    private int mHeight;
    private int mPadding;
    private int mWidth;

    public V6SeekbarPopupTexts(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPadding = context.getResources().getDimensionPixelSize(R.dimen.v6_seek_bar_padding) + context.getResources().getDimensionPixelSize(R.dimen.half_of_cursor);
    }

    public void initialize(IconListPreference preference) {
        if (preference.getEntries() != null) {
            for (CharSequence addTextView : preference.getEntries()) {
                addTextView(addTextView);
            }
            setValue(preference.findIndexOfValue(preference.getValue()));
            requestLayout();
        }
    }

    public void setValue(int value) {
        for (int i = 0; i < getChildCount(); i++) {
            TextView textView = (TextView) getChildAt(i);
            if (value == i) {
                textView.setTextColor(-1);
            } else {
                textView.setTextColor(-1275068417);
            }
        }
    }

    private void addTextView(CharSequence text) {
        TextView textView = new TextView(this.mContext);
        textView.setLayoutParams(new LayoutParams(-2, -2));
        textView.setTextSize(12.0f);
        textView.setText(text);
        addView(textView);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mWidth = w;
        this.mHeight = h;
    }

    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        this.mGap = ((float) (this.mWidth - (this.mPadding * 2))) / ((float) (getChildCount() - 1));
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            int i2;
            TextView textView = (TextView) getChildAt(i);
            int w = ((int) textView.getPaint().measureText(textView.getText().toString())) + 1;
            if (1 == getLayoutDirection()) {
                i2 = (childCount - 1) - i;
            } else {
                i2 = i;
            }
            int center = (int) (((double) (((float) i2) * this.mGap)) + 0.5d);
            textView.layout((this.mPadding + center) - (w / 2), 0, (this.mPadding + center) + (w / 2), this.mHeight);
        }
    }

    public void setOrientation(int orientation, boolean animation) {
    }
}
