package com.android.camera.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.support.v7.recyclerview.R;
import android.util.AttributeSet;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.ui.HorizontalSlideView.HorizontalDrawAdapter;
import com.android.camera.ui.HorizontalSlideView.OnItemSelectListener;

public class ManualFocusPositionPopup extends V6AbstractSettingPopup implements OnItemSelectListener {
    private static final String TAG = ManualFocusPositionPopup.class.getSimpleName();
    private static final int[] sTextActivatedColorState = new int[]{16843518};
    private static final int[] sTextDefaultColorState = new int[]{0};
    private int mCurrentIndex = -1;
    private HorizontalSlideView mHorizontalSlideView;
    private int mLineColorDefault;
    private float mLineHalfHeight;
    private int mLineLineGap;
    private int mLineTextGap;
    private int mLineWidth;
    private ColorStateList mTextColor;
    private int mTextSize;

    class HorizontalSlideViewAdapter extends HorizontalDrawAdapter {
        private CharSequence[] mEntries;
        Paint mPaint = new Paint();

        public HorizontalSlideViewAdapter(CharSequence[] entries) {
            this.mEntries = entries;
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStrokeWidth((float) ManualFocusPositionPopup.this.mLineWidth);
            this.mPaint.setTextSize((float) ManualFocusPositionPopup.this.mTextSize);
            this.mPaint.setTextAlign(Align.LEFT);
        }

        private void drawText(int index, Canvas canvas) {
            canvas.drawText(Util.getLocalizedNumberString(this.mEntries[index].toString()), 0.0f, (-(this.mPaint.ascent() + this.mPaint.descent())) / 2.0f, this.mPaint);
        }

        public void draw(int index, Canvas canvas, boolean selected) {
            this.mPaint.setColor(selected ? -65536 : -1);
            int colorForState;
            if (index % 10 == 0) {
                Paint paint = this.mPaint;
                if (selected) {
                    colorForState = ManualFocusPositionPopup.this.mTextColor.getColorForState(ManualFocusPositionPopup.sTextActivatedColorState, 0);
                } else {
                    colorForState = ManualFocusPositionPopup.this.mTextColor.getColorForState(ManualFocusPositionPopup.sTextDefaultColorState, 0);
                }
                paint.setColor(colorForState);
                drawText(index / 10, canvas);
                return;
            }
            Paint paint2 = this.mPaint;
            if (selected) {
                colorForState = ManualFocusPositionPopup.this.mTextColor.getColorForState(ManualFocusPositionPopup.sTextActivatedColorState, 0);
            } else {
                colorForState = ManualFocusPositionPopup.this.mLineColorDefault;
            }
            paint2.setColor(colorForState);
            canvas.drawLine(0.0f, -ManualFocusPositionPopup.this.mLineHalfHeight, 0.0f, ManualFocusPositionPopup.this.mLineHalfHeight, this.mPaint);
        }

        public float measureWidth(int index) {
            if (index % 10 == 0) {
                return this.mPaint.measureText(this.mEntries[index / 10].toString());
            }
            return (float) ManualFocusPositionPopup.this.mLineWidth;
        }

        public float measureGap(int index) {
            if (index % 10 == 0 || (index + 1) % 10 == 0) {
                return (float) ManualFocusPositionPopup.this.mLineTextGap;
            }
            return (float) ManualFocusPositionPopup.this.mLineLineGap;
        }

        public Align getAlign(int index) {
            return Align.LEFT;
        }

        public int getCount() {
            return 101;
        }
    }

    public ManualFocusPositionPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray style = context.obtainStyledAttributes(R.style.SingeTextItemTextStyle, new int[]{16842901, 16842904});
        this.mTextSize = style.getDimensionPixelSize(style.getIndex(0), this.mTextSize);
        this.mTextColor = style.getColorStateList(style.getIndex(1));
        style.recycle();
        Resources resources = context.getResources();
        this.mLineHalfHeight = ((float) resources.getDimensionPixelSize(R.dimen.focus_line_height)) / 2.0f;
        this.mLineWidth = resources.getDimensionPixelSize(R.dimen.focus_line_width);
        this.mLineLineGap = resources.getDimensionPixelSize(R.dimen.focus_line_line_gap);
        this.mLineTextGap = resources.getDimensionPixelSize(R.dimen.focus_line_text_gap);
        this.mLineColorDefault = resources.getColor(R.color.manual_focus_line_color_default);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        CharSequence[] entries = new CharSequence[11];
        for (int i = 0; i <= 10; i++) {
            entries[i] = getDisplayedFocusValue(i * 10);
        }
        this.mHorizontalSlideView.setDrawAdapter(new HorizontalSlideViewAdapter(entries));
        reloadPreference();
    }

    private String getDisplayedFocusValue(int value) {
        if (value == 0) {
            return this.mContext.getString(R.string.pref_camera_focusmode_entry_auto);
        }
        return String.valueOf(value);
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHorizontalSlideView = (HorizontalSlideView) findViewById(R.id.horizon_slideview);
        this.mHorizontalSlideView.setOnItemSelectListener(this);
    }

    public void reloadPreference() {
        this.mCurrentIndex = mapFocusToIndex(CameraSettings.getFocusPosition());
        this.mHorizontalSlideView.setSelection(this.mCurrentIndex);
    }

    private int mapFocusToIndex(int focusPosition) {
        return 100 - (Util.clamp(focusPosition, 0, 1000) / 10);
    }

    private int mapIndexToFocus(int index) {
        return 1000 - ((index * 1000) / 100);
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public void onItemSelect(HorizontalSlideView view, int position) {
        CameraSettings.setFocusPosition(mapIndexToFocus(position));
        if (position != this.mCurrentIndex) {
            boolean switchMode = this.mCurrentIndex == 0 || position == 0;
            this.mCurrentIndex = position;
            CameraSettings.setFocusModeSwitching(switchMode);
            CameraSettings.setFocusMode(this.mCurrentIndex == 0 ? "continuous-picture" : "manual");
            if (isShown()) {
                ((ActivityBase) this.mContext).playCameraSound(6);
            }
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(7, 0, 0, switchMode ? "pref_camera_focus_mode_key" : "pref_focus_position_key", null);
            }
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mHorizontalSlideView.setEnabled(enabled);
    }
}
