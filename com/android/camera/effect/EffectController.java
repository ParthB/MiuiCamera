package com.android.camera.effect;

import android.graphics.PointF;
import android.graphics.RectF;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraAppImpl;
import com.android.camera.Device;
import com.android.camera.aosp_porting.FeatureParser;
import com.android.camera.effect.renders.BigFaceEffectRender;
import com.android.camera.effect.renders.BlackWhiteEffectRender;
import com.android.camera.effect.renders.FishEyeEffectRender;
import com.android.camera.effect.renders.FocusPeakingRender;
import com.android.camera.effect.renders.Gaussian2DEffectRender;
import com.android.camera.effect.renders.GaussianMaskEffectRender;
import com.android.camera.effect.renders.GradienterEffectRender;
import com.android.camera.effect.renders.GradienterSnapshotEffectRender;
import com.android.camera.effect.renders.GrayEffectRender;
import com.android.camera.effect.renders.InstagramClarendonEffectRender;
import com.android.camera.effect.renders.InstagramCremaEffectRender;
import com.android.camera.effect.renders.InstagramHudsonEffectRender;
import com.android.camera.effect.renders.InstagramRiseEffectRender;
import com.android.camera.effect.renders.LightTunnelEffectRender;
import com.android.camera.effect.renders.LongFaceEffectRender;
import com.android.camera.effect.renders.MirrorEffectRender;
import com.android.camera.effect.renders.MosaicEffectRender;
import com.android.camera.effect.renders.PipeRenderPair;
import com.android.camera.effect.renders.Render;
import com.android.camera.effect.renders.RenderGroup;
import com.android.camera.effect.renders.SketchEffectRender;
import com.android.camera.effect.renders.SmallFaceEffectRender;
import com.android.camera.effect.renders.TiltShiftMaskEffectRender;
import com.android.camera.effect.renders.VividEffectRender;
import com.android.camera.effect.renders.VscoA4EffectRender;
import com.android.camera.effect.renders.VscoF2EffectRender;
import com.android.camera.effect.renders.XBlurEffectRender;
import com.android.camera.effect.renders.XGaussianEffectRender;
import com.android.camera.effect.renders.XTiltShiftEffectRender;
import com.android.camera.effect.renders.YBlurEffectRender;
import com.android.camera.effect.renders.YGaussianEffectRender;
import com.android.camera.effect.renders.YTiltShiftEffectRender;
import com.android.camera.ui.V6ModulePicker;
import com.android.gallery3d.ui.GLCanvas;
import java.util.ArrayList;

public class EffectController {
    public static final int COLUMN_COUNT;
    public static final int SHOW_COUNT;
    public static int sBackgroundBlurIndex = 16;
    public static int sDividerIndex = 8;
    public static int sFishEyeIndex = 12;
    public static int sGaussianIndex = 19;
    public static int sGradienterIndex = 17;
    private static EffectController sInstance;
    public static int sPeakingMFIndex = 20;
    public static int sTiltShiftIndex = 18;
    private boolean mBlur = false;
    private int mBlurStep = -1;
    private float mDeviceRotation;
    public volatile int mDisplayEndIndex = SHOW_COUNT;
    public volatile boolean mDisplayShow = false;
    public volatile int mDisplayStartIndex = 0;
    private boolean mDrawPeaking;
    private int mEffectCount = 16;
    private ArrayList<String> mEffectEntries;
    private ArrayList<String> mEffectEntryValues;
    private int mEffectGroupSize = 21;
    private ArrayList<Integer> mEffectImageIds;
    private int mEffectIndex = 0;
    private ArrayList<String> mEffectKeys;
    private EffectRectAttribute mEffectRectAttribute = new EffectRectAttribute();
    public volatile boolean mFillAnimationCache = false;
    private boolean mIsDrawMainFrame = true;
    private ArrayList<Integer> mNeedRectSet;
    private ArrayList<Integer> mNeedScaleDownSet;
    private int mOrientation;
    private int mOverrideEffectIndex = -1;
    public SurfacePosition mSurfacePosition = new SurfacePosition();
    private float mTiltShiftMaskAlpha;

    public static class EffectRectAttribute {
        public int mInvertFlag;
        public PointF mPoint1;
        public PointF mPoint2;
        public float mRangeWidth;
        public RectF mRectF;

        private EffectRectAttribute() {
            this.mRectF = new RectF();
            this.mPoint1 = new PointF();
            this.mPoint2 = new PointF();
        }

        private EffectRectAttribute(EffectRectAttribute e) {
            this.mRectF = new RectF();
            this.mPoint1 = new PointF();
            this.mPoint2 = new PointF();
            this.mRectF.set(e.mRectF);
            this.mPoint1.set(e.mPoint1);
            this.mPoint2.set(e.mPoint2);
            this.mInvertFlag = e.mInvertFlag;
            this.mRangeWidth = e.mRangeWidth;
        }

        public String toString() {
            return "mRectF=" + this.mRectF + " mPoint1=" + this.mPoint1 + " mPoint2=" + this.mPoint2 + " mInvertFlag=" + this.mInvertFlag + " mRangeWidth=" + this.mRangeWidth;
        }
    }

    public static class SurfacePosition {
        public int mHonSpace;
        public boolean mIsRtl;
        public int mStartX;
        public int mStartY;
        public int mVerSpace;
        public int mWidth;
    }

    static {
        int i;
        int i2 = 7;
        if (Device.isPad()) {
            i = 7;
        } else {
            i = 12;
        }
        SHOW_COUNT = i;
        if (!Device.isPad()) {
            i2 = 3;
        }
        COLUMN_COUNT = i2;
    }

    private EffectController() {
        initialize();
    }

    public void initialize() {
        initEffectWeight();
        getEffectGroup(null, null, false, false, -1);
    }

    private void initEffectWeight() {
    }

    public SurfacePosition getSurfacePosition() {
        return this.mSurfacePosition;
    }

    public static synchronized EffectController getInstance() {
        EffectController effectController;
        synchronized (EffectController.class) {
            if (sInstance == null) {
                sInstance = new EffectController();
            }
            effectController = sInstance;
        }
        return effectController;
    }

    public static synchronized void releaseInstance() {
        synchronized (EffectController.class) {
            sInstance = null;
        }
    }

    public void setEffect(int effect) {
        synchronized (this) {
            this.mEffectIndex = effect;
        }
    }

    public void setBlurEffect(boolean blured) {
        int i = 0;
        if (blured != this.mBlur) {
            if (!blured) {
                this.mOverrideEffectIndex = -1;
            }
            if (this.mBlurStep < 0 || 8 < this.mBlurStep) {
                if (!blured) {
                    i = 8;
                }
                this.mBlurStep = i;
            }
            this.mIsDrawMainFrame = true;
        }
        this.mBlur = blured;
    }

    public int getBlurAnimationValue() {
        if (this.mBlurStep >= 0 && this.mBlurStep <= 8) {
            this.mBlurStep = (this.mBlur ? 1 : -1) + this.mBlurStep;
            if (8 <= this.mBlurStep && this.mBlur) {
                this.mOverrideEffectIndex = sBackgroundBlurIndex;
            }
            if (this.mBlurStep >= 0 && this.mBlurStep <= 8) {
                return (this.mBlurStep * 212) / 8;
            }
        }
        return -1;
    }

    public int getEffect(boolean includeOverride) {
        synchronized (this) {
            int i;
            if (includeOverride) {
                if (this.mOverrideEffectIndex != -1) {
                    i = this.mOverrideEffectIndex;
                    return i;
                }
            }
            i = this.mEffectIndex;
            return i;
        }
    }

    public void setDrawPeaking(boolean drawPeaking) {
        this.mDrawPeaking = drawPeaking;
    }

    public boolean isNeedDrawPeaking() {
        return this.mDrawPeaking;
    }

    public boolean isEffectPageSelected() {
        boolean z = false;
        synchronized (this) {
            if (this.mEffectIndex != 0 && this.mEffectIndex < this.mEffectCount) {
                z = true;
            }
        }
        return z;
    }

    public boolean hasEffect() {
        boolean z = false;
        synchronized (this) {
            if (Device.isSupportedShaderEffect() && this.mEffectIndex != 0) {
                z = true;
            }
        }
        return z;
    }

    public String[] getEntries() {
        String[] result = new String[this.mEffectEntries.size()];
        this.mEffectEntries.toArray(result);
        return result;
    }

    public String[] getEntryValues() {
        String[] result = new String[this.mEffectEntryValues.size()];
        this.mEffectEntryValues.toArray(result);
        return result;
    }

    public int[] getImageIds() {
        int[] result = new int[this.mEffectImageIds.size()];
        for (int i = 0; i < this.mEffectImageIds.size(); i++) {
            result[i] = ((Integer) this.mEffectImageIds.get(i)).intValue();
        }
        return result;
    }

    public String getAnalyticsKey() {
        String str;
        synchronized (this) {
            str = (this.mEffectKeys == null || this.mEffectIndex >= this.mEffectKeys.size()) ? "" : (String) this.mEffectKeys.get(this.mEffectIndex);
        }
        return str;
    }

    public int getDisplayStartIndex() {
        return this.mDisplayStartIndex;
    }

    public int getDisplayEndIndex() {
        return this.mDisplayEndIndex;
    }

    public boolean isDisplayShow() {
        return this.mDisplayShow;
    }

    public boolean isMainFrameDisplay() {
        return this.mIsDrawMainFrame;
    }

    public void setTiltShiftMaskAlpha(float alpha) {
        this.mTiltShiftMaskAlpha = alpha;
    }

    public float getTiltShiftMaskAlpha() {
        return this.mTiltShiftMaskAlpha;
    }

    public void setEffectAttribute(RectF rectF, PointF point1, PointF point2, float range) {
        this.mEffectRectAttribute.mRectF.set(rectF);
        this.mEffectRectAttribute.mPoint1.set(point1);
        this.mEffectRectAttribute.mPoint2.set(point2);
        this.mEffectRectAttribute.mRangeWidth = range;
    }

    public EffectRectAttribute getEffectAttribute() {
        return this.mEffectRectAttribute;
    }

    public void clearEffectAttribute() {
        this.mEffectRectAttribute.mRectF.set(0.0f, 0.0f, 0.0f, 0.0f);
        this.mEffectRectAttribute.mPoint1.set(0.0f, 0.0f);
        this.mEffectRectAttribute.mPoint2.set(0.0f, 0.0f);
        this.mEffectRectAttribute.mRangeWidth = 0.0f;
    }

    public RectF getEffectRectF() {
        return new RectF(this.mEffectRectAttribute.mRectF);
    }

    public EffectRectAttribute copyEffectRectAttribute() {
        return new EffectRectAttribute(this.mEffectRectAttribute);
    }

    public void setInvertFlag(int invert) {
        this.mEffectRectAttribute.mInvertFlag = invert;
    }

    public int getInvertFlag() {
        return this.mEffectRectAttribute.mInvertFlag;
    }

    public void setOrientation(int orientation) {
        this.mOrientation = orientation;
    }

    public int getOrientation() {
        return this.mOrientation;
    }

    public boolean isNeedRect(int index) {
        if (Device.isSupportedShaderEffect()) {
            for (Integer i : this.mNeedRectSet) {
                if (i.intValue() == index) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean needDownScale(int index) {
        if (Device.isSupportedShaderEffect()) {
            for (Integer i : this.mNeedScaleDownSet) {
                if (i.intValue() == index) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getString(int strId) {
        return CameraAppImpl.getAndroidContext().getString(strId);
    }

    private void addEntryItem(int strId, int id) {
        this.mEffectEntries.add(getString(strId));
        this.mEffectEntryValues.add(String.valueOf(id));
    }

    public RenderGroup getEffectGroup(GLCanvas canvas, RenderGroup renderGroup, boolean wholeRender, boolean isSnapShotRender, int index) {
        if (!Device.isSupportedShaderEffect()) {
            return null;
        }
        boolean addEntry = canvas == null;
        boolean initOne = false;
        if (canvas == null) {
            this.mEffectEntries = new ArrayList();
            this.mEffectEntryValues = new ArrayList();
            this.mEffectImageIds = new ArrayList();
            this.mEffectKeys = new ArrayList();
            this.mNeedRectSet = new ArrayList();
            this.mNeedScaleDownSet = new ArrayList();
            addEntry = true;
        } else if (renderGroup == null) {
            renderGroup = new RenderGroup(canvas, this.mEffectGroupSize);
            if (!wholeRender && index < 0) {
                return renderGroup;
            }
        } else if (!renderGroup.isNeedInit(index)) {
            return renderGroup;
        }
        int id = 0;
        if (addEntry) {
            try {
                addEntryItem(R.string.pref_camera_coloreffect_entry_none, 0);
                this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_none));
                this.mEffectKeys.add("");
            } catch (IllegalArgumentException e) {
                if (index < 0) {
                    Log.e("EffectController", "IllegalArgumentException when create render.", e);
                } else {
                    throw e;
                }
            }
        } else if (renderGroup.getRender(0) == null) {
            if (!(wholeRender || index == 0)) {
                if (index < 0) {
                    if (null != null) {
                    }
                }
            }
            renderGroup.setRender(null, 0);
        }
        id = 0 + 1;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_instagram_rise, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_instagram_rise));
            this.mEffectKeys.add("effect_instagram_rise_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (null != null) {
                    }
                }
            }
            renderGroup.setRender(InstagramRiseEffectRender.create(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_instagram_clarendon, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_instagram_clarendon));
            this.mEffectKeys.add("effect_instagram_clarendon_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(InstagramClarendonEffectRender.create(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_instagram_crema, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_instagram_crema));
            this.mEffectKeys.add("effect_instagram_crema_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(InstagramCremaEffectRender.create(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_instagram_hudson, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_instagram_hudson));
            this.mEffectKeys.add("effect_instagram_hudson_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(InstagramHudsonEffectRender.create(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_vivid, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_vivid));
            this.mEffectKeys.add("effect_vivid_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new VividEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_vsco_a4, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_vsco_a4));
            this.mEffectKeys.add("effect_vsco_a4_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(VscoA4EffectRender.create(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_vsco_f2, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_vsco_f2));
            this.mEffectKeys.add("effect_vsco_f2_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new VscoF2EffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            this.mNeedScaleDownSet.add(Integer.valueOf(id));
            addEntryItem(R.string.pref_camera_coloreffect_entry_mono, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_mono));
            this.mEffectKeys.add("effect_gray_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new GrayEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_blackwhite, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_blackwhite));
            this.mEffectKeys.add("effect_blackwhite_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new BlackWhiteEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_sketch, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_sketch));
            this.mEffectKeys.add("effect_sketch_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            if (wholeRender || index == id || renderGroup.isPartComplete(2)) {
                renderGroup.setRender(new PipeRenderPair(canvas, renderGroup.getPartRender(0) != null ? renderGroup.getPartRender(0) : new Gaussian2DEffectRender(canvas, id), renderGroup.getPartRender(1) != null ? renderGroup.getPartRender(1) : new SketchEffectRender(canvas, id), false), id);
                renderGroup.clearPartRenders();
            } else if (renderGroup.getPartRender(0) == null) {
                renderGroup.addPartRender(new Gaussian2DEffectRender(canvas, id));
            } else if (renderGroup.getPartRender(1) == null) {
                renderGroup.addPartRender(new SketchEffectRender(canvas, id));
            }
            initOne = true;
        }
        if (addEntry) {
            sDividerIndex = id;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_big_face, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_big_face));
            this.mEffectKeys.add("effect_big_face_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new BigFaceEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_small_face, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_small_face));
            this.mEffectKeys.add("effect_small_face_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new SmallFaceEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_long_face, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_long_face));
            this.mEffectKeys.add("effect_long_face_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new LongFaceEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            sFishEyeIndex = id;
            addEntryItem(R.string.pref_camera_coloreffect_entry_fisheye, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_fisheye));
            this.mEffectKeys.add("effect_fisheye_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new FishEyeEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            this.mNeedRectSet.add(Integer.valueOf(id));
            addEntryItem(R.string.pref_camera_coloreffect_entry_mosaic, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_mosaic));
            this.mEffectKeys.add("effect_mosaic_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new MosaicEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_mirror, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_mirror));
            this.mEffectKeys.add("effect_mirror_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new MirrorEffectRender(canvas, id), id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            addEntryItem(R.string.pref_camera_coloreffect_entry_light_tunnel, id);
            this.mEffectImageIds.add(Integer.valueOf(R.drawable.camera_effect_image_light_tunnel));
            this.mEffectKeys.add("effect_light_tunnel_picture_taken_key");
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new LightTunnelEffectRender(canvas, id), id);
            initOne = true;
        }
        if (addEntry) {
            this.mEffectCount = 18;
        }
        id++;
        if (addEntry) {
            sBackgroundBlurIndex = id;
        } else if (renderGroup.getRender(id) == null) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            if (wholeRender || index == id || renderGroup.isPartComplete(2)) {
                boolean matchPartRender0 = renderGroup.getPartRender(0) != null ? renderGroup.getPartRender(0).getId() == id : false;
                boolean matchPartRender1 = renderGroup.getPartRender(1) != null ? renderGroup.getPartRender(1).getId() == id : false;
                renderGroup.setRender(new PipeRenderPair(canvas, matchPartRender0 ? renderGroup.getPartRender(0) : new XBlurEffectRender(canvas, id), matchPartRender1 ? renderGroup.getPartRender(1) : new YBlurEffectRender(canvas, id), false), id);
                if (matchPartRender0 || matchPartRender1) {
                    renderGroup.clearPartRenders();
                }
            } else if (renderGroup.getPartRender(0) == null) {
                renderGroup.addPartRender(new XBlurEffectRender(canvas, id));
            } else if (renderGroup.getPartRender(1) == null) {
                renderGroup.addPartRender(new YBlurEffectRender(canvas, id));
            }
            initOne = true;
        }
        id++;
        if (addEntry) {
            sGradienterIndex = id;
        } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
            Render gradienterSnapshotEffectRender;
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            if (isSnapShotRender) {
                gradienterSnapshotEffectRender = new GradienterSnapshotEffectRender(canvas, id);
            } else {
                gradienterSnapshotEffectRender = new GradienterEffectRender(canvas, id);
            }
            renderGroup.setRender(gradienterSnapshotEffectRender, id);
            initOne = true;
        }
        id++;
        if (addEntry) {
            sTiltShiftIndex = id;
            this.mNeedRectSet.add(Integer.valueOf(id));
        } else if (renderGroup.getRender(id) == null && Device.isSupportedTiltShift()) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            if (wholeRender || index == id || renderGroup.isPartComplete(3)) {
                renderGroup.setRender(new PipeRenderPair(canvas, new PipeRenderPair(canvas, renderGroup.getPartRender(0) != null ? renderGroup.getPartRender(0) : new XTiltShiftEffectRender(canvas, id), renderGroup.getPartRender(1) != null ? renderGroup.getPartRender(1) : new YTiltShiftEffectRender(canvas, id), false), renderGroup.getPartRender(2) != null ? renderGroup.getPartRender(2) : new TiltShiftMaskEffectRender(canvas, id), false), id);
                renderGroup.clearPartRenders();
            } else if (renderGroup.getPartRender(0) == null) {
                renderGroup.addPartRender(new XTiltShiftEffectRender(canvas, id));
            } else if (renderGroup.getPartRender(1) == null) {
                renderGroup.addPartRender(new YTiltShiftEffectRender(canvas, id));
            } else if (renderGroup.getPartRender(2) == null) {
                renderGroup.addPartRender(new TiltShiftMaskEffectRender(canvas, id));
            }
            initOne = true;
        }
        if (!FeatureParser.getBoolean("is_camera_replace_higher_cost_effect", false)) {
            id++;
            if (addEntry) {
                sGaussianIndex = id;
                this.mNeedRectSet.add(Integer.valueOf(id));
            } else if (renderGroup.getRender(id) == null && (V6ModulePicker.isCameraModule() || isSnapShotRender)) {
                if (!(wholeRender || index == id)) {
                    if (index < 0) {
                        if (initOne) {
                        }
                    }
                }
                if (wholeRender || index == id || renderGroup.isPartComplete(3)) {
                    renderGroup.setRender(new PipeRenderPair(canvas, new PipeRenderPair(canvas, renderGroup.getPartRender(0) != null ? renderGroup.getPartRender(0) : new XGaussianEffectRender(canvas, id), renderGroup.getPartRender(1) != null ? renderGroup.getPartRender(1) : new YGaussianEffectRender(canvas, id), false), renderGroup.getPartRender(2) != null ? renderGroup.getPartRender(2) : new GaussianMaskEffectRender(canvas, id), false), id);
                    renderGroup.clearPartRenders();
                } else if (renderGroup.getPartRender(0) == null) {
                    renderGroup.addPartRender(new XGaussianEffectRender(canvas, id));
                } else if (renderGroup.getPartRender(1) == null) {
                    renderGroup.addPartRender(new YGaussianEffectRender(canvas, id));
                } else if (renderGroup.getPartRender(2) == null) {
                    renderGroup.addPartRender(new GaussianMaskEffectRender(canvas, id));
                }
                initOne = true;
            }
        }
        id++;
        if (addEntry) {
            sPeakingMFIndex = id;
        } else if (renderGroup.getRender(id) == null && ((V6ModulePicker.isCameraModule() || isSnapShotRender) && Device.isSupportedPeakingMF() && !isSnapShotRender)) {
            if (!(wholeRender || index == id)) {
                if (index < 0) {
                    if (initOne) {
                    }
                }
            }
            renderGroup.setRender(new FocusPeakingRender(canvas, id), id);
        }
        if (addEntry) {
            this.mEffectGroupSize = id + 1;
        }
        return renderGroup;
    }

    public boolean isBackGroundBlur() {
        return getEffect(true) == sBackgroundBlurIndex;
    }

    public void setDeviceRotation(boolean isLying, float rotation) {
        if (isLying) {
            rotation = -1.0f;
        }
        this.mDeviceRotation = rotation;
    }

    public float getDeviceRotation() {
        return this.mDeviceRotation;
    }

    public boolean isFishEye() {
        boolean z;
        synchronized (this) {
            z = this.mEffectIndex == sFishEyeIndex;
        }
        return z;
    }

    public int getEffectCount() {
        return this.mEffectCount;
    }

    public int getEffectIndexByEntryName(String name) {
        for (int i = 0; i < this.mEffectEntries.size(); i++) {
            if (((String) this.mEffectEntries.get(i)).equals(name)) {
                return i;
            }
        }
        return 0;
    }
}
