package com.android.camera.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import com.android.camera.ActivityBase;
import com.android.camera.CameraAppImpl;
import com.android.camera.CameraScreenNail;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Util;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;

public class FaceView extends FrameView {
    private static Configuration configuration = CameraAppImpl.getAndroidContext().getResources().getConfiguration();
    private final boolean LOGV = true;
    private int mAgeFemaleHonPadding;
    private int mAgeMaleHonPadding;
    private String[] mAgeOnlyRangeAlias = CameraAppImpl.getAndroidContext().getResources().getStringArray(R.array.pref_camera_show_age_reports);
    private String[] mAgeRangeAlias = CameraAppImpl.getAndroidContext().getResources().getStringArray(R.array.pref_camera_show_gender_age_reports);
    private int mAgeVerPadding;
    private Drawable mBeautyScoreIc;
    private Drawable mBeautyScoreSurmounted;
    private Drawable mBeautyScoreWinner;
    private int mDisplayOrientation;
    private Drawable mFaceIndicator;
    private String mFaceInfoFormat = CameraAppImpl.getAndroidContext().getString(R.string.face_analyze_info);
    private Drawable mFaceInfoPop;
    private int mFacePopupBottom;
    private CameraHardwareFace[] mFaces;
    private int mGap;
    private String mGenderFemale = CameraAppImpl.getAndroidContext().getString(R.string.face_analyze_info_female);
    private String mGenderMale = CameraAppImpl.getAndroidContext().getString(R.string.face_analyze_info_male);
    private int mLatestFaceIndex = -1;
    private CameraHardwareFace[] mLatestFaces = new CameraHardwareFace[6];
    private Paint mMagicPaint;
    private Matrix mMatrix = new Matrix();
    private boolean mMirror;
    private int mOrientation;
    private Paint mPaint = new Paint();
    private int mPopBottomMargin;
    private RectF mRect = new RectF();
    private Paint mRectPaint;
    private Drawable mSBeautyScoreSurmounted;
    private int mScoreHonPadding;
    private int mScoreVerPadding;
    private Drawable mSexFemailIc;
    private Drawable mSexMailIc;
    private String mShowAgeandAge;
    private int mSingleDrawableMargin;
    private boolean mSkipDraw;
    private Rect mTextBounds;
    private int mWinnerIndex = -1;

    public FaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mPaint.setColor(-1);
        this.mPaint.setTextSize(context.getResources().getDimension(R.dimen.face_info_textSize));
        this.mPaint.setAlpha(150);
        this.mFaceIndicator = getResources().getDrawable(R.drawable.ic_face_detected);
        this.mTextBounds = new Rect();
        if (Device.isSupportedMagicMirror()) {
            this.mMagicPaint = new Paint();
            this.mMagicPaint.setColor(-1);
            this.mMagicPaint.setTextSize(context.getResources().getDimension(R.dimen.face_info_magic_textSize));
            this.mMagicPaint.setTypeface(Util.getMiuiTypeface(CameraAppImpl.getAndroidContext()));
            this.mFaceInfoPop = getResources().getDrawable(R.drawable.face_info_pop);
            this.mSexMailIc = getResources().getDrawable(R.drawable.ic_sex_mail);
            this.mSexFemailIc = getResources().getDrawable(R.drawable.ic_sex_femail);
            this.mBeautyScoreIc = getResources().getDrawable(R.drawable.ic_beauty_score);
            this.mBeautyScoreWinner = getResources().getDrawable(R.drawable.ic_beauty_score_winner);
            this.mBeautyScoreSurmounted = getResources().getDrawable(R.drawable.ic_beauty_surmounted);
            this.mSBeautyScoreSurmounted = getResources().getDrawable(R.drawable.ic_beauty_super_surmounted);
            this.mAgeVerPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_ver_padding);
            this.mGap = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_text_left_dis);
            this.mPopBottomMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_pop_bottom_margin);
            this.mScoreHonPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_score_hon_padding);
            this.mScoreVerPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_score_ver_padding);
            this.mAgeMaleHonPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_male_hon_padding);
            this.mAgeFemaleHonPadding = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_female_hon_padding);
            this.mSingleDrawableMargin = this.mContext.getResources().getDimensionPixelSize(R.dimen.face_info_no_popup_bottom_margin);
            this.mFacePopupBottom = (int) (((double) this.mFaceInfoPop.getIntrinsicHeight()) * 0.3d);
            this.mRectPaint = new Paint();
            this.mRectPaint.setColor(-18377);
            this.mRectPaint.setStrokeWidth((float) this.mContext.getResources().getDimensionPixelSize(R.dimen.face_rect_width));
            this.mRectPaint.setStyle(Style.STROKE);
        }
    }

    public boolean setFaces(CameraHardwareFace[] faces) {
        Log.v("FaceView", "Num of faces=" + faces.length);
        boolean isFacesChanged = faceExists() || (faces != null && faces.length > 0);
        this.mFaces = faces;
        updateLatestFaces();
        if (isFacesChanged) {
            setToVisible();
            invalidate();
        }
        return true;
    }

    public CameraHardwareFace[] getFaces() {
        return this.mFaces;
    }

    private void updateLatestFaces() {
        if (this.mLatestFaceIndex >= 5) {
            this.mLatestFaceIndex = 0;
        } else {
            this.mLatestFaceIndex++;
        }
        if (faceExists()) {
            CameraHardwareFace face = this.mFaces[0];
            for (int i = 1; i < this.mFaces.length; i++) {
                if (this.mFaces[i].rect.right - this.mFaces[i].rect.left > face.rect.right - face.rect.left) {
                    face = this.mFaces[i];
                }
            }
            this.mLatestFaces[this.mLatestFaceIndex] = face;
            return;
        }
        this.mLatestFaces[this.mLatestFaceIndex] = null;
    }

    public RectF getFocusRect() {
        RectF rect = new RectF();
        CameraScreenNail screenNail = ((ActivityBase) getContext()).getCameraScreenNail();
        if (screenNail == null || this.mLatestFaceIndex < 0 || this.mLatestFaceIndex >= 6) {
            return null;
        }
        this.mMatrix.reset();
        Util.prepareMatrix(this.mMatrix, this.mMirror, this.mDisplayOrientation, screenNail.getRenderWidth(), screenNail.getRenderHeight(), getWidth() / 2, getHeight() / 2);
        rect.set(this.mLatestFaces[this.mLatestFaceIndex].rect);
        this.mMatrix.postRotate((float) this.mOrientation);
        this.mMatrix.mapRect(rect);
        return rect;
    }

    public boolean isFaceStable() {
        int emptyFacesCount = 0;
        int averageWidth = 0;
        int averageHeight = 0;
        int averageLeft = 0;
        int averageTop = 0;
        for (CameraHardwareFace face : this.mLatestFaces) {
            if (face == null) {
                emptyFacesCount++;
                if (emptyFacesCount >= 3) {
                    return false;
                }
            } else {
                averageWidth += face.rect.right - face.rect.left;
                averageHeight += face.rect.bottom - face.rect.top;
                averageLeft += face.rect.left;
                averageTop += face.rect.top;
            }
        }
        int faceCount = this.mLatestFaces.length - emptyFacesCount;
        averageWidth /= faceCount;
        averageHeight /= faceCount;
        averageLeft /= faceCount;
        averageTop /= faceCount;
        int faceWidthRestrict = averageWidth / 3 > 90 ? averageWidth / 3 : 90;
        for (CameraHardwareFace face2 : this.mLatestFaces) {
            if (face2 != null) {
                if (Math.abs((face2.rect.right - face2.rect.left) - averageWidth) <= faceWidthRestrict && Math.abs(face2.rect.left - averageLeft) <= 120) {
                    if (Math.abs(face2.rect.top - averageTop) > 120) {
                    }
                }
                return false;
            }
        }
        boolean z = averageWidth > 670 || averageHeight > 670;
        this.mIsBigEnoughRect = z;
        return true;
    }

    public void setDisplayOrientation(int orientation) {
        this.mDisplayOrientation = orientation;
        Log.v("FaceView", "mDisplayOrientation=" + orientation);
    }

    public void setOrientation(int orientation, boolean animation) {
        this.mOrientation = orientation;
        if (!this.mPause && faceExists() && !this.mSkipDraw) {
            invalidate();
        }
    }

    public void setMirror(boolean mirror) {
        this.mMirror = mirror;
        Log.v("FaceView", "mMirror=" + mirror);
    }

    public boolean faceExists() {
        return this.mFaces != null && this.mFaces.length > 0;
    }

    public void showStart() {
        setToVisible();
        invalidate();
    }

    public void showSuccess() {
        setToVisible();
        invalidate();
    }

    public void showFail() {
        setToVisible();
        invalidate();
    }

    public void clear() {
        this.mFaces = null;
        clearPreviousFaces();
        invalidate();
    }

    public void pause() {
        super.pause();
    }

    public void clearPreviousFaces() {
        this.mLatestFaceIndex = -1;
        for (int i = 0; i < this.mLatestFaces.length; i++) {
            this.mLatestFaces[i] = null;
        }
    }

    public void setShowGenderAndAge(String show) {
        this.mShowAgeandAge = show;
    }

    private boolean showFaceInfo() {
        return !"off".equals(this.mShowAgeandAge);
    }

    public void setSkipDraw(boolean skipDraw) {
        this.mSkipDraw = skipDraw;
    }

    private int getAgeIndex(float age) {
        if (age <= 7.0f) {
            return 0;
        }
        if (age <= 17.0f) {
            return 1;
        }
        if (age <= 30.0f) {
            return 2;
        }
        if (age <= 44.0f) {
            return 3;
        }
        if (age <= 60.0f) {
            return 4;
        }
        return 5;
    }

    private String getShowInfo(CameraHardwareFace face) {
        if ("on".equals(this.mShowAgeandAge)) {
            String gender = this.mGenderMale;
            String age = Integer.toString((int) face.ageMale);
            if (face.gender < 0.4f) {
                gender = this.mGenderFemale;
                age = Integer.toString((int) face.ageFemale);
            }
            return String.format(configuration.locale, this.mFaceInfoFormat, new Object[]{gender, age});
        }
        int index = getAgeIndex(face.ageMale);
        if (face.gender < 0.4f) {
            index = getAgeIndex(face.ageFemale) + 6;
        }
        if (index < this.mAgeRangeAlias.length) {
            return this.mAgeRangeAlias[index];
        }
        return null;
    }

    protected void onDraw(Canvas canvas) {
        if (!this.mSkipDraw) {
            CameraScreenNail screenNail = ((ActivityBase) getContext()).getCameraScreenNail();
            if (!(this.mPause || this.mFaces == null || this.mFaces.length <= 0 || screenNail == null)) {
                this.mMatrix.reset();
                Util.prepareMatrix(this.mMatrix, this.mMirror, this.mDisplayOrientation, screenNail.getRenderWidth(), screenNail.getRenderHeight(), getWidth() / 2, getHeight() / 2);
                this.mMatrix.postRotate((float) this.mOrientation);
                canvas.save();
                canvas.rotate((float) (-this.mOrientation));
                int type = getShowType(this.mFaces);
                boolean squareMode = CameraSettings.isSwitchOn("pref_camera_square_mode_key");
                int i = 0;
                while (i < this.mFaces.length) {
                    this.mRect.set(this.mFaces[i].rect);
                    this.mMatrix.mapRect(this.mRect);
                    if (!squareMode || Util.isContains(screenNail.getRenderRect(), this.mRect)) {
                        drawFaceRect(canvas);
                        switch (type) {
                            case 1:
                                drawGenderAge(canvas, this.mFaces[i]);
                                break;
                            case 2:
                                if (i != this.mWinnerIndex) {
                                    break;
                                }
                                drawFacePopInfo(canvas, getScoreDrawable(i), null, null, 0, 0, 0, this.mSingleDrawableMargin);
                                break;
                            case 4:
                                if (this.mFaces[i].beautyscore != 0.0f) {
                                    if (this.mFaces[i].beautyscore <= 90.0f) {
                                        Canvas canvas2 = canvas;
                                        drawFacePopInfo(canvas2, this.mBeautyScoreIc, this.mFaceInfoPop, String.format("%.1f", new Object[]{Float.valueOf(this.mFaces[i].beautyscore / 10.0f)}), this.mScoreHonPadding, this.mScoreVerPadding, this.mFacePopupBottom, this.mPopBottomMargin);
                                        break;
                                    }
                                    drawFacePopInfo(canvas, getScoreDrawable(i), null, null, 0, 0, 0, this.mSingleDrawableMargin);
                                    break;
                                }
                                break;
                            default:
                                break;
                        }
                        i++;
                    } else {
                        canvas.restore();
                    }
                }
                canvas.restore();
            }
        }
    }

    private void setToVisible() {
        if (getVisibility() != 0) {
            setVisibility(0);
        }
    }

    private Drawable getScoreDrawable(int index) {
        if (index < 0 || this.mFaces == null || index > this.mFaces.length) {
            return null;
        }
        if (this.mFaces[index].beautyscore > 98.0f) {
            return this.mSBeautyScoreSurmounted;
        }
        if (this.mFaces[index].beautyscore > 90.0f) {
            return this.mBeautyScoreSurmounted;
        }
        if (index == this.mWinnerIndex) {
            return this.mBeautyScoreWinner;
        }
        return this.mBeautyScoreIc;
    }

    private void drawFaceRect(Canvas canvas) {
        if (Device.isSupportedMagicMirror() && CameraSettings.isSwitchOn("pref_camera_magic_mirror_key")) {
            canvas.drawRect(this.mRect, this.mRectPaint);
            return;
        }
        this.mFaceIndicator.setBounds((int) this.mRect.left, (int) this.mRect.top, (int) this.mRect.right, (int) this.mRect.bottom);
        this.mFaceIndicator.draw(canvas);
    }

    private void drawFacePopInfo(Canvas canvas, Drawable drawable, Drawable pop, String info, int honPadding, int verPadding, int popBottom, int bottomMargin) {
        if (TextUtils.isEmpty(info)) {
            this.mTextBounds.set(0, 0, 0, 0);
        } else {
            this.mMagicPaint.getTextBounds(info, 0, info.length(), this.mTextBounds);
        }
        int infoWidth = ((this.mTextBounds.width() != 0 ? this.mGap : 0) + ((honPadding * 2) + (drawable != null ? drawable.getIntrinsicWidth() : 0))) + this.mTextBounds.width();
        int infoHeight = (verPadding * 2) + (drawable != null ? drawable.getIntrinsicHeight() : 0);
        Rect popRect = new Rect(((int) this.mRect.centerX()) - (infoWidth / 2), ((((int) this.mRect.top) - infoHeight) - bottomMargin) - popBottom, ((int) this.mRect.centerX()) + (infoWidth / 2), ((int) this.mRect.top) - bottomMargin);
        if (pop != null) {
            pop.setBounds(popRect);
            pop.draw(canvas);
        }
        if (drawable != null) {
            drawable.setBounds(popRect.left + honPadding, popRect.top + verPadding, (popRect.left + honPadding) + drawable.getIntrinsicWidth(), (popRect.top + verPadding) + drawable.getIntrinsicHeight());
            drawable.draw(canvas);
        }
        if (this.mTextBounds.width() != 0) {
            canvas.drawText(info, (float) ((drawable != null ? drawable.getIntrinsicWidth() + this.mGap : 0) + (popRect.left + honPadding)), (float) ((popRect.top + (infoHeight / 2)) + (this.mTextBounds.height() / 2)), this.mMagicPaint);
        }
    }

    private void drawGenderAge(Canvas canvas, CameraHardwareFace face) {
        if (!isValideAGInfo(face)) {
            return;
        }
        String info;
        if (Device.isSupportedMagicMirror() && CameraSettings.isSwitchOn("pref_camera_magic_mirror_key")) {
            boolean isFemail = face.gender < 0.4f;
            info = "";
            if ("on".equals(this.mShowAgeandAge)) {
                info = Integer.toString((int) (isFemail ? face.ageFemale : face.ageMale));
            } else {
                int index = getAgeIndex(face.ageMale);
                if (index < this.mAgeOnlyRangeAlias.length) {
                    info = this.mAgeOnlyRangeAlias[index];
                }
            }
            drawFacePopInfo(canvas, isFemail ? this.mSexFemailIc : this.mSexMailIc, this.mFaceInfoPop, info, isFemail ? this.mAgeFemaleHonPadding : this.mAgeMaleHonPadding, this.mAgeVerPadding, this.mFacePopupBottom, this.mPopBottomMargin);
            return;
        }
        info = getShowInfo(face);
        if (!TextUtils.isEmpty(info)) {
            this.mPaint.getTextBounds(info, 0, info.length(), this.mTextBounds);
            canvas.drawText(info, this.mRect.centerX() - ((float) this.mTextBounds.centerX()), this.mRect.top - ((float) (this.mTextBounds.bottom - this.mTextBounds.top)), this.mPaint);
        }
    }

    private int getShowType(CameraHardwareFace[] faces) {
        if (faces == null || faces.length <= 0) {
            return 0;
        }
        if (Device.isSupportedMagicMirror() && CameraSettings.isSwitchOn("pref_camera_magic_mirror_key")) {
            int socreNo = 0;
            this.mWinnerIndex = -1;
            for (int i = 0; i < this.mFaces.length; i++) {
                CameraHardwareFace face = faces[i];
                if (face.beautyscore > 0.0f) {
                    socreNo++;
                    if (this.mWinnerIndex == -1 || face.beautyscore > faces[this.mWinnerIndex].beautyscore) {
                        this.mWinnerIndex = i;
                    }
                }
            }
            if (socreNo > 1) {
                return 2;
            }
            if (socreNo > 0) {
                return 4;
            }
            return 0;
        } else if (showFaceInfo() && Device.isSupportedIntelligentBeautify()) {
            return 1;
        } else {
            return 0;
        }
    }

    private boolean isValideAGInfo(CameraHardwareFace face) {
        if (0.5f <= face.prob) {
            return face.gender <= 0.4f || 0.6f <= face.gender;
        } else {
            return false;
        }
    }
}
