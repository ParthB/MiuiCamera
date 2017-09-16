package com.android.camera.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.drawable.Drawable;
import android.support.v7.recyclerview.R;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

public class ExitButton extends View {
    private boolean mExpand;
    private int mExpandLeft;
    private int mExpandRight;
    private String mText;
    private TextPaint mTextPaint = new TextPaint(1);

    public ExitButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mTextPaint.setTextSize((float) context.getResources().getDimensionPixelSize(R.dimen.camera_mode_exit_button_text_size));
        this.mTextPaint.setColor(-1);
    }

    public void setText(String text) {
        if (!TextUtils.equals(text, this.mText)) {
            this.mExpand = false;
            requestLayout();
            invalidate();
        }
        this.mText = text;
    }

    public void draw(Canvas canvas) {
        if (this.mExpand) {
            setLeft(this.mExpandLeft);
            setRight(this.mExpandRight);
        } else {
            this.mTextPaint.setAlpha(255);
        }
        super.draw(canvas);
        if (this.mText != null && getWidth() > getPaddingLeft() * 2) {
            FontMetricsInt metrics = this.mTextPaint.getFontMetricsInt();
            float baseline = (float) (((canvas.getHeight() - metrics.bottom) - metrics.top) / 2);
            int textWidth = (int) this.mTextPaint.measureText(this.mText, 0, this.mText.length());
            int textBoundsWidth = getWidth() - (getPaddingLeft() * 2);
            int textTrans = ((getWidth() - (getPaddingLeft() * 2)) - textWidth) / 2;
            canvas.save();
            if (textTrans < 0) {
                canvas.clipRect(getPaddingLeft(), 0, getWidth() - getPaddingLeft(), canvas.getHeight());
                if (this.mExpand) {
                    this.mTextPaint.setAlpha((textBoundsWidth * 255) / textWidth);
                }
            }
            canvas.drawText(this.mText, (float) (getPaddingLeft() + textTrans), baseline, this.mTextPaint);
            canvas.restore();
        }
    }

    public String getText() {
        return this.mText;
    }

    public TextPaint getPaint() {
        return this.mTextPaint;
    }

    public void setExpandedAnimation(boolean expanded) {
        this.mExpand = expanded;
    }

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int textWidth = 0;
        int bgWidth = 0;
        int bgHeight = 0;
        if (this.mText != null) {
            textWidth = (int) this.mTextPaint.measureText(this.mText, 0, this.mText.length());
        }
        FontMetricsInt metrics = this.mTextPaint.getFontMetricsInt();
        textWidth += getPaddingLeft() + getPaddingRight();
        int textHeight = (metrics.descent - metrics.ascent) + (getPaddingTop() + getPaddingBottom());
        Drawable background = getBackground();
        if (background != null) {
            bgWidth = background.getIntrinsicWidth();
            bgHeight = background.getIntrinsicHeight();
        }
        setMeasuredDimension(Math.max(textWidth, bgWidth), Math.max(textHeight, bgHeight));
    }

    public void setExpandingSize(int newLeft, int newRight) {
        this.mExpandLeft = newLeft;
        this.mExpandRight = newRight;
    }
}
