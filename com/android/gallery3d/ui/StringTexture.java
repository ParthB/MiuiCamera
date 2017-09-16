package com.android.gallery3d.ui;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Typeface;
import android.os.Build.VERSION;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.FloatMath;
import com.android.camera.CameraAppImpl;
import com.android.camera.Util;
import com.android.camera.aosp_porting.ReflectUtil;

public class StringTexture extends CanvasTexture {
    private final FontMetricsInt mMetrics;
    private final TextPaint mPaint;
    private final String mText;

    private StringTexture(String text, TextPaint paint, FontMetricsInt metrics, int width, int height) {
        super(width, height);
        this.mText = text;
        this.mPaint = paint;
        this.mMetrics = metrics;
    }

    public static TextPaint getDefaultPaint(float textSize, int color, int type) {
        TextPaint paint = new TextPaint();
        paint.setTextSize(textSize);
        paint.setAntiAlias(true);
        paint.setColor(color);
        if (type == 1) {
            paint.setTypeface(Util.getMiuiTypeface(CameraAppImpl.getAndroidContext()));
            paint.setShadowLayer(0.1f, 5.0f, 5.0f, -16777216);
            setLongshotMode(paint, 0.1f);
        } else if (type == 2) {
            paint.setTypeface(Util.getMiuiTimeTypeface(CameraAppImpl.getAndroidContext()));
            paint.setShadowLayer(0.1f, 0.0f, 3.0f, 771751936);
            setLongshotMode(paint, 0.1f);
        } else {
            paint.setShadowLayer(2.0f, 0.0f, 0.0f, -16777216);
        }
        return paint;
    }

    private static void setLongshotMode(TextPaint paint, float letterSpacing) {
        if (VERSION.SDK_INT >= 21) {
            ReflectUtil.callMethod(TextPaint.class, paint, "setLetterSpacing", "(F)V", Float.valueOf(letterSpacing));
        }
    }

    public static StringTexture newInstance(String text, float textSize, int color, int type) {
        return newInstance(text, getDefaultPaint(textSize, color, type), type);
    }

    public static StringTexture newInstance(String text, float textSize, int color, float lengthLimit, boolean isBold, int type) {
        TextPaint paint = getDefaultPaint(textSize, color, type);
        if (isBold) {
            paint.setTypeface(Typeface.defaultFromStyle(1));
        }
        if (lengthLimit > 0.0f) {
            text = TextUtils.ellipsize(text, paint, lengthLimit, TruncateAt.END).toString();
        }
        return newInstance(text, paint, type);
    }

    private static StringTexture newInstance(String text, TextPaint paint, int type) {
        int i = 0;
        FontMetricsInt metrics = paint.getFontMetricsInt();
        int ceil = (int) FloatMath.ceil(paint.measureText(text));
        if (type == 1) {
            i = 5;
        }
        int width = ceil + i;
        int height = metrics.descent - metrics.ascent;
        if (width <= 0) {
            width = 1;
        }
        if (height <= 0) {
            height = 1;
        }
        return new StringTexture(text, paint, metrics, width, height);
    }

    protected void onDraw(Canvas canvas, Bitmap backing) {
        canvas.translate(0.0f, (float) (-this.mMetrics.ascent));
        canvas.drawText(this.mText, 0.0f, 0.0f, this.mPaint);
    }
}
