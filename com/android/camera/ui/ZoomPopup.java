package com.android.camera.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.support.v7.recyclerview.R;
import android.text.Layout.Alignment;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Spline;
import android.view.MotionEvent;
import android.view.View.OnTouchListener;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraManager;
import com.android.camera.CameraSettings;
import com.android.camera.Util;
import com.android.camera.module.BaseModule;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.camera.ui.HorizontalSlideView.HorizontalDrawAdapter;
import com.android.camera.ui.HorizontalSlideView.OnPositionSelectListener;

public class ZoomPopup extends V6AbstractSettingPopup implements OnPositionSelectListener {
    private static final String TAG = ZoomPopup.class.getSimpleName();
    private static final int[] sTextActivatedColorState = new int[]{16843518};
    private static final int[] sTextDefaultColorState = new int[]{0};
    private static float[] sX = new float[]{0.0f, 10.0f, 12.0f, 20.0f, 25.0f, 27.0f, 29.0f, 30.0f, 32.0f, 35.0f};
    private static float[] sY = new float[]{100.0f, 200.0f, 220.0f, 370.0f, 510.0f, 580.0f, 660.0f, 700.0f, 800.0f, 1000.0f};
    private float mCurrentPosition = -1.0f;
    private TextAppearanceSpan mDigitsTextStyle;
    private Spline mEntryToZoomRatioSpline;
    private HorizontalSlideView mHorizontalSlideView;
    private int mLineColorDefault;
    private float mLineHalfHeight;
    private int mLineLineGap;
    private int mLineTextGap;
    private int mLineWidth;
    private ColorStateList mTextColor;
    private int mTextSize;
    private TextAppearanceSpan mXTextStyle;
    private int mZoomMax;
    private int mZoomRatio;
    private int mZoomRatioMax;
    private int mZoomRatioMin = 100;
    private int mZoomRatioTele;
    private Spline mZoomRatioToEntrySpline;
    private int mZoomRatioWide;

    class HorizontalSlideViewAdapter extends HorizontalDrawAdapter {
        private CharSequence[] mEntries;
        private StaticLayout[] mEntryLayouts;
        Paint mPaint = new Paint();
        TextPaint mTextPaint;

        public HorizontalSlideViewAdapter(CharSequence[] entries) {
            this.mEntries = entries;
            this.mPaint.setAntiAlias(true);
            this.mPaint.setStrokeWidth((float) ZoomPopup.this.mLineWidth);
            this.mPaint.setTextSize((float) ZoomPopup.this.mTextSize);
            this.mPaint.setTextAlign(Align.LEFT);
            this.mTextPaint = new TextPaint(this.mPaint);
            this.mEntryLayouts = new StaticLayout[this.mEntries.length];
            for (int i = 0; i < this.mEntries.length; i++) {
                this.mEntryLayouts[i] = new StaticLayout(this.mEntries[i], this.mTextPaint, Util.sWindowWidth, Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            }
        }

        private int indexToSection(int index) {
            if (index == 0) {
                return 0;
            }
            if (index == 10) {
                return 1;
            }
            if (index == 47) {
                return 2;
            }
            return -1;
        }

        private void drawText(int sectionIndex, Canvas canvas) {
            float height = (float) (this.mEntryLayouts[sectionIndex].getLineAscent(0) - this.mEntryLayouts[sectionIndex].getLineDescent(0));
            canvas.save();
            canvas.translate(0.0f, height / 2.0f);
            this.mEntryLayouts[sectionIndex].draw(canvas);
            canvas.restore();
        }

        public void draw(int index, Canvas canvas, boolean selected) {
            if (index == 0 || index == 10 || index == 47) {
                int[] -get7;
                TextPaint textPaint = this.mTextPaint;
                if (selected) {
                    -get7 = ZoomPopup.sTextActivatedColorState;
                } else {
                    -get7 = ZoomPopup.sTextDefaultColorState;
                }
                textPaint.drawableState = -get7;
                drawText(indexToSection(index), canvas);
                return;
            }
            int colorForState;
            Paint paint = this.mPaint;
            if (selected) {
                colorForState = ZoomPopup.this.mTextColor.getColorForState(ZoomPopup.sTextActivatedColorState, 0);
            } else {
                colorForState = ZoomPopup.this.mLineColorDefault;
            }
            paint.setColor(colorForState);
            canvas.drawLine(0.0f, -ZoomPopup.this.mLineHalfHeight, 0.0f, ZoomPopup.this.mLineHalfHeight, this.mPaint);
        }

        public float measureWidth(int index) {
            if (index == 0 || index == 10 || index == 47) {
                return this.mEntryLayouts[indexToSection(index)].getLineWidth(0);
            }
            return (float) ZoomPopup.this.mLineWidth;
        }

        public float measureGap(int index) {
            if (index == 0 || index == 10 || index == 47) {
                return (float) ZoomPopup.this.mLineTextGap;
            }
            return (float) ZoomPopup.this.mLineLineGap;
        }

        public Align getAlign(int index) {
            return Align.LEFT;
        }

        public int getCount() {
            return 48;
        }
    }

    private float[] convertSplineXToEntryX(float[] splineX) {
        int SPLINE_ENTRY_COUNT_2X_TO_10X = (int) ((splineX[splineX.length - 1] - 10.0f) + 1.0f);
        float[] entryX = new float[splineX.length];
        for (int i = 0; i < splineX.length; i++) {
            if (splineX[i] <= 10.0f) {
                entryX[i] = splineX[i];
            } else {
                entryX[i] = (((splineX[i] - 10.0f) / ((float) (SPLINE_ENTRY_COUNT_2X_TO_10X - 1))) * 37.0f) + 10.0f;
            }
        }
        return entryX;
    }

    private float[] convertSplineYToZoomRatioY(float[] splineY) {
        int SPLINE_ZOOM_MAX = (int) splineY[splineY.length - 1];
        float[] zoomRatioY = new float[splineY.length];
        for (int i = 0; i < splineY.length; i++) {
            if (splineY[i] <= ((float) this.mZoomRatioTele)) {
                zoomRatioY[i] = splineY[i];
            } else {
                zoomRatioY[i] = (((splineY[i] - ((float) this.mZoomRatioTele)) / ((float) (SPLINE_ZOOM_MAX - this.mZoomRatioTele))) * ((float) (this.mZoomRatioMax - this.mZoomRatioTele))) + ((float) this.mZoomRatioTele);
            }
        }
        return zoomRatioY;
    }

    public ZoomPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray style = context.obtainStyledAttributes(R.style.SingeTextItemTextStyle, new int[]{16842901, 16842904});
        this.mTextSize = style.getDimensionPixelSize(style.getIndex(0), this.mTextSize);
        this.mTextColor = style.getColorStateList(style.getIndex(1));
        style.recycle();
        Resources resources = context.getResources();
        this.mLineHalfHeight = ((float) resources.getDimensionPixelSize(R.dimen.zoom_popup_line_height)) / 2.0f;
        this.mLineWidth = resources.getDimensionPixelSize(R.dimen.zoom_popup_line_width);
        this.mLineLineGap = resources.getDimensionPixelSize(R.dimen.zoom_popup_line_line_gap);
        this.mLineTextGap = resources.getDimensionPixelSize(R.dimen.zoom_popup_line_text_gap);
        this.mLineColorDefault = resources.getColor(R.color.zoom_popup_line_color_default);
        this.mDigitsTextStyle = new TextAppearanceSpan(context, R.style.ZoomPopupDigitsTextStyle);
        this.mXTextStyle = new TextAppearanceSpan(context, R.style.ZoomPopupXTextStyle);
        this.mZoomRatioWide = Integer.valueOf(context.getResources().getString(R.string.pref_camera_zoom_ratio_wide)).intValue();
        this.mZoomRatioTele = Integer.valueOf(context.getResources().getString(R.string.pref_camera_zoom_ratio_tele)).intValue();
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        BaseModule module = (BaseModule) ((ActivityBase) this.mContext).getCurrentModule();
        this.mZoomMax = module.getZoomMax();
        this.mZoomRatioMax = module.getZoomMaxRatio();
        float[] x = convertSplineXToEntryX(sX);
        float[] y = convertSplineYToZoomRatioY(sY);
        this.mEntryToZoomRatioSpline = Spline.createMonotoneCubicSpline(x, y);
        this.mZoomRatioToEntrySpline = Spline.createMonotoneCubicSpline(y, x);
        this.mHorizontalSlideView.setDrawAdapter(new HorizontalSlideViewAdapter(new CharSequence[]{getDisplayedZoomRatio(this.mZoomRatioWide), getDisplayedZoomRatio(this.mZoomRatioTele), getDisplayedZoomRatio(this.mZoomRatioMax)}));
        reloadPreference();
    }

    public void updateBackground() {
        setBackgroundResource(R.color.fullscreen_background);
    }

    public void passTouchEvent(MotionEvent event) {
        this.mHorizontalSlideView.dispatchTouchEvent(event);
    }

    private CharSequence getDisplayedZoomRatio(int ratio) {
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(String.valueOf(ratio / 100), this.mDigitsTextStyle, 33);
        builder.append("X", this.mXTextStyle, 33);
        return builder;
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mHorizontalSlideView = (HorizontalSlideView) findViewById(R.id.horizon_slideview);
        this.mHorizontalSlideView.setOnPositionSelectListener(this);
        this.mHorizontalSlideView.setJustifyEnabled(false);
    }

    public void setOnTouchListener(OnTouchListener l) {
        this.mHorizontalSlideView.setOnTouchListener(l);
    }

    public void reloadPreference() {
        this.mZoomRatio = ((Integer) CameraManager.instance().getStashParameters().getZoomRatios().get(CameraSettings.readZoom(CameraSettingPreferences.instance()))).intValue();
        this.mCurrentPosition = mapZoomRatioToPosition(this.mZoomRatio);
        this.mHorizontalSlideView.setSelection(this.mCurrentPosition / 47.0f);
    }

    private float mapZoomRatioToPosition(int zoomRatio) {
        return this.mZoomRatioToEntrySpline.interpolate((float) zoomRatio);
    }

    private int mapPositionToZoomRatio(float position) {
        return Math.round(this.mEntryToZoomRatioSpline.interpolate(position));
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public void onPositionSelect(HorizontalSlideView view, float positionRatio) {
        float position = positionRatio * 47.0f;
        if (position != this.mCurrentPosition) {
            this.mCurrentPosition = position;
            if (isShown()) {
                ((ActivityBase) this.mContext).playCameraSound(6);
            }
            if (this.mMessageDispacher != null) {
                this.mMessageDispacher.dispacherMessage(7, 0, 0, getKey(), Integer.valueOf(mapPositionToZoomRatio(this.mCurrentPosition)));
            }
            AutoLockManager.getInstance(this.mContext).onUserInteraction();
        }
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        this.mHorizontalSlideView.setEnabled(enabled);
    }
}
