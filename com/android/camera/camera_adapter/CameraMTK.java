package com.android.camera.camera_adapter;

import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.recyclerview.R;
import android.util.Log;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.Exif;
import com.android.camera.JpegEncodingQualityMappings;
import com.android.camera.MtkFBParamsUtil;
import com.android.camera.PictureSize;
import com.android.camera.PictureSizeManager;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.camera.hardware.CameraHardwareProxy;
import com.android.camera.hardware.CameraHardwareProxy.CameraHardwareFace;
import com.android.camera.hardware.CameraHardwareProxy.ContinuousShotCallback;
import com.android.camera.hardware.MTKCameraProxy;
import com.android.camera.hardware.MTKCameraProxy.StereoDataCallback;
import com.android.camera.module.BaseModule;
import com.android.camera.module.CameraModule;
import com.android.camera.stereo.StereoDataGroup;
import com.android.camera.stereo.WarningCallback;
import com.android.camera.ui.FaceView;
import java.util.List;

public class CameraMTK extends CameraModule {
    private static MTKCameraProxy sProxy = ((MTKCameraProxy) CameraHardwareProxy.getDeviceProxy());
    private byte[] mClearImage;
    private ContinuousShotCallback mContinuousShotCallback;
    private FBParams mCurrentFBParams;
    private int mCurrentNum;
    private byte[] mDepthMap;
    private FaceNo mFaceNo;
    private FBParams mInUseFBParams;
    private boolean mIsLongShotMode;
    private boolean mIsMTKFaceBeautyMode;
    private boolean mIsStereoCapture;
    private StereoPictureCallback mJpegPictureCB;
    private byte[] mJpsData;
    private byte[] mLdcData;
    private byte[] mMaskAndConfigData;
    private final Object mOperator;
    private byte[] mOriginalJpegData;
    private SaveHandler mSaveHandler;
    private final WarningCallback mStereoCameraWarningCallback;
    private final StereoPhotoDataCallback mStereoPhotoDataCallback;

    public enum FBLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    public class FBParams {
        public int enlargeEye;
        public int skinColor;
        public int slimFace;
        public int smoothLevel;

        public void copy(FBParams other) {
            if (other != null) {
                this.skinColor = other.skinColor;
                this.smoothLevel = other.smoothLevel;
                this.slimFace = other.slimFace;
                this.enlargeEye = other.enlargeEye;
            }
        }

        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FBParams)) {
                return false;
            }
            FBParams fbParams = (FBParams) o;
            return this.enlargeEye == fbParams.enlargeEye && this.skinColor == fbParams.skinColor && this.slimFace == fbParams.slimFace && this.smoothLevel == fbParams.smoothLevel;
        }

        public int hashCode() {
            return (((((this.skinColor * 31) + this.smoothLevel) * 31) + this.slimFace) * 31) + this.enlargeEye;
        }
    }

    public enum FaceNo {
        NONE,
        SINGLE,
        MULTIPLE
    }

    class MtkCategory extends CameraCategory {
        public MtkCategory() {
            super();
        }

        public void takePicture(Location loc) {
            if (Device.isSupportedStereo() && CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                Log.d("Camera", "takePicture " + CameraMTK.this.mStereoCameraWarningCallback.isDualCameraReady());
                CameraMTK.this.mIsStereoCapture = CameraMTK.this.mStereoCameraWarningCallback.isDualCameraReady();
                CameraMTK.this.mJpegPictureCB.setLocation(loc);
                CameraMTK.this.mCameraDevice.setStereoDataCallback(CameraMTK.this.mStereoPhotoDataCallback);
                CameraMTK.this.mCameraDevice.takePicture(CameraMTK.this.mShutterCallback, null, null, CameraMTK.this.mJpegPictureCB);
                return;
            }
            super.takePicture(loc);
        }
    }

    private class SaveHandler extends Handler {
        private byte[] mXmpJpegData;

        SaveHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            Log.i("Camera", "Save handleMessage msg.what = " + msg.what + ", msg.obj = " + msg.obj);
            switch (msg.what) {
                case 10004:
                    StereoDataGroup dataGroup = msg.obj;
                    this.mXmpJpegData = CameraMTK.this.writeStereoCaptureInfoToJpg(dataGroup.getPictureName(), dataGroup.getOriginalJpegData(), dataGroup.getJpsData(), dataGroup.getMaskAndConfigData(), dataGroup.getClearImage(), dataGroup.getDepthMap(), dataGroup.getLdcData());
                    Log.i("Camera", "notifyMergeData mXmpJpegData: " + this.mXmpJpegData);
                    if (this.mXmpJpegData != null) {
                        saveFile(this.mXmpJpegData, dataGroup.getPictureName());
                        return;
                    }
                    return;
                default:
                    return;
            }
        }

        private void saveFile(byte[] mXmpJpegData, String title) {
            int width;
            int height;
            Size s = CameraMTK.this.mParameters.getPictureSize();
            int orientation = Exif.getOrientation(mXmpJpegData);
            if ((CameraMTK.this.mJpegRotation + orientation) % 180 == 0) {
                width = s.width;
                height = s.height;
            } else {
                width = s.height;
                height = s.width;
            }
            CameraMTK.this.mActivity.getImageSaver().addImage(mXmpJpegData, title, System.currentTimeMillis(), null, CameraMTK.this.mJpegPictureCB.getLocation(), width, height, null, orientation, false, false, true);
        }
    }

    private class StereoPhotoDataCallback implements StereoDataCallback {
        private StereoPhotoDataCallback() {
        }

        public void onJpsCapture(byte[] jpsData) {
            if (jpsData == null) {
                Log.i("Camera", "JPS data is null");
                return;
            }
            Log.i("Camera", "onJpsCapture jpsData:" + jpsData.length);
            CameraMTK.this.mJpsData = jpsData;
            CameraMTK.this.notifyMergeData();
        }

        public void onMaskCapture(byte[] maskData) {
            if (maskData == null) {
                Log.i("Camera", "Mask data is null");
                return;
            }
            Log.i("Camera", "onMaskCapture maskData:" + maskData.length);
            CameraMTK.this.mMaskAndConfigData = maskData;
            CameraMTK.this.setJsonBuffer(CameraMTK.this.mMaskAndConfigData);
            CameraMTK.this.notifyMergeData();
        }

        public void onDepthMapCapture(byte[] depthData) {
            Log.i("Camera", "onDepthMapCapture depthData");
            if (depthData == null) {
                Log.i("Camera", "depth data is null");
                return;
            }
            Log.i("Camera", "onDepthMapCapture depthData:" + depthData.length);
            CameraMTK.this.mDepthMap = depthData;
            CameraMTK.this.notifyMergeData();
        }

        public void onClearImageCapture(byte[] clearImageData) {
            Log.i("Camera", "onClearImageCapture clearImageData");
            if (clearImageData == null) {
                Log.i("Camera", " clearImage data is null");
                return;
            }
            Log.i("Camera", "onClearImageCapture clearImageData:" + clearImageData.length);
            CameraMTK.this.mClearImage = clearImageData;
            CameraMTK.this.notifyMergeData();
        }

        public void onLdcCapture(byte[] ldcData) {
            Log.i("Camera", "onLdcCapture ldcData");
            if (ldcData == null) {
                Log.i("Camera", " ldc data is null");
                return;
            }
            Log.i("Camera", "onLdcCapture ldcData:" + ldcData.length);
            CameraMTK.this.mLdcData = ldcData;
            CameraMTK.this.notifyMergeData();
        }
    }

    class StereoPictureCallback extends JpegPictureCallback {
        public StereoPictureCallback(Location loc) {
            super(loc);
        }

        public void onPictureTaken(byte[] jpegData, Camera camera) {
            Log.d("Camera", "[mJpegPictureCallback] " + CameraMTK.this.mIsStereoCapture);
            if (!CameraMTK.this.mPaused) {
                if (jpegData == null || !CameraMTK.this.mIsStereoCapture) {
                    super.onPictureTaken(jpegData, camera);
                    if (jpegData == null) {
                        return;
                    }
                }
                CameraMTK.this.mFocusManager.onShutter();
                CameraMTK.this.mOriginalJpegData = jpegData;
                CameraMTK.this.notifyMergeData();
                Log.d("Camera", "[mJpegPictureCallback] end");
            }
        }

        public Location getLocation() {
            return this.mLocation;
        }
    }

    public CameraMTK() {
        this.mIsLongShotMode = false;
        this.mIsMTKFaceBeautyMode = false;
        this.mInUseFBParams = new FBParams();
        this.mCurrentFBParams = new FBParams();
        this.mFaceNo = FaceNo.NONE;
        this.mContinuousShotCallback = new ContinuousShotCallback() {
            public void onContinuousShotDone(int capNum) {
                Log.d("Camera", "onContinuousShotDone: capNum=" + capNum);
                CameraMTK.this.mHandler.removeMessages(37);
                CameraMTK.this.handleMultiSnapDone();
            }
        };
        this.mStereoPhotoDataCallback = new StereoPhotoDataCallback();
        this.mStereoCameraWarningCallback = new WarningCallback();
        this.mJpegPictureCB = new StereoPictureCallback(null);
        this.mIsStereoCapture = false;
        this.mCurrentNum = 0;
        this.mCameraCategory = new MtkCategory();
        this.mOperator = constructObject();
    }

    private void mtkUpdateCameraParametersPreference() {
        sProxy.setCameraMode(this.mParameters, 1);
        String thumbnailQuality = getString(R.string.pref_camera_jpegquality_value_low);
        Log.v("Camera", "thumbnailQuality = " + thumbnailQuality);
        this.mParameters.setJpegThumbnailQuality(JpegEncodingQualityMappings.getQualityNumber(thumbnailQuality));
        if ((EffectController.getInstance().hasEffect() ? Device.isEffectWatermarkFilted() : false) || this.mMutexModePicker.isUbiFocus() || !CameraSettings.isTimeWaterMarkOpen(this.mPreferences) || CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            sProxy.setTimeWatermark(this.mParameters, "off");
        } else {
            sProxy.setTimeWatermark(this.mParameters, "on");
        }
        String iso = getManualValue("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
        if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
            Log.v("Camera", "ISO value = " + iso);
            sProxy.setISOValue(this.mParameters, iso);
        }
        String saturationStr = this.mPreferences.getString("pref_qc_camera_saturation_key", getString(R.string.pref_camera_saturation_default));
        Log.v("Camera", "Saturation value = " + saturationStr);
        sProxy.setSaturation(this.mParameters, saturationStr);
        String contrastStr = this.mPreferences.getString("pref_qc_camera_contrast_key", getString(R.string.pref_camera_contrast_default));
        Log.v("Camera", "Contrast value = " + contrastStr);
        sProxy.setContrast(this.mParameters, contrastStr);
        String sharpnessStr = this.mPreferences.getString("pref_qc_camera_sharpness_key", getString(R.string.pref_camera_sharpness_default));
        Log.v("Camera", "Sharpness value = " + sharpnessStr);
        sProxy.setSharpness(this.mParameters, sharpnessStr);
        setPictureFlipIfNeed(this.mParameters);
        boolean enableZsl = enableZSL();
        Log.v("Camera", "ZSL value = " + (enableZsl ? "on" : "off"));
        if (enableZsl) {
            this.mIsZSLMode = true;
            sProxy.setZSLMode(this.mParameters, "on");
        } else {
            this.mIsZSLMode = false;
            sProxy.setZSLMode(this.mParameters, "off");
        }
        if (Device.isSupportedStereo()) {
            if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
                sProxy.setVsDofMode(this.mParameters, true);
                this.mParameters.setPreviewFpsRange(5000, 24000);
                String vfLevel = this.mPreferences.getString("pref_camera_stereo_key", getString(R.string.pref_camera_stereo_default));
                Log.v("Camera", "vfLevel value = " + vfLevel);
                sProxy.setVsDofLevel(this.mParameters, vfLevel);
            } else {
                this.mParameters.setPreviewFpsRange(5000, 30000);
                sProxy.setVsDofMode(this.mParameters, false);
            }
        }
        if (this.mMultiSnapStatus && !this.mIsLongShotMode && isSupportContinuousShut(this.mParameters)) {
            this.mIsLongShotMode = true;
            setTimeWatermarkIfNeed();
        } else if (this.mIsLongShotMode) {
            this.mIsLongShotMode = false;
            applyMultiShutParameters(false);
        }
        Log.v("Camera", "Long Shot mode value = " + isLongShotMode());
        if (Device.isSupportedSkinBeautify()) {
            String fbValue = CameraSettings.getFaceBeautifyValue();
            sProxy.setStillBeautify(this.mParameters, fbValue);
            Log.v("Camera", "FB value =" + sProxy.getStillBeautify(this.mParameters));
            if (isUseMediaTekFaceBeautify()) {
                setMediatekBeautify(fbValue);
            } else {
                setBeautyParams();
            }
            sProxy.setFaceBeauty(this.mParameters, "false");
            sProxy.set3dnrMode(this.mParameters, "on");
        }
        if (Device.isSupportedIntelligentBeautify()) {
            String showGenderAndAge = this.mPreferences.getString("pref_camera_show_gender_age_key", getString(R.string.pref_camera_show_gender_age_default));
            getUIController().getFaceView().setShowGenderAndAge(showGenderAndAge);
            Log.v("Camera", "SetShowGenderAndAge =" + showGenderAndAge);
        }
        sProxy.setHDR(this.mParameters, "false");
        sProxy.setNightShot(this.mParameters, "false");
        sProxy.setNightAntiMotion(this.mParameters, "false");
        if (!this.mMutexModePicker.isNormal()) {
            if (this.mMutexModePicker.isHandNight()) {
                if (isSceneMotion()) {
                    sProxy.setNightAntiMotion(this.mParameters, "true");
                    Log.v("Camera", "AntiMotion = true");
                } else {
                    sProxy.setNightShot(this.mParameters, "true");
                    Log.v("Camera", "Hand Nigh = true");
                }
            } else if (this.mMutexModePicker.isMorphoHdr()) {
                sProxy.setHDR(this.mParameters, "true");
                Log.v("Camera", "Morpho HDR = true");
            }
        }
        if (isBackCamera() && Device.isSupportedASD()) {
            boolean asdEnable = (getUIController().getSettingPage().isItemSelected() || this.mIsLongShotMode) ? false : !CameraSettings.isSwitchOn("pref_camera_stereo_mode_key");
            Log.v("Camera", "ASD Enable = " + asdEnable);
            setMetaCallback(asdEnable);
        }
    }

    private void setMediatekBeautify(String fbValue) {
        if (this.mIsMTKFaceBeautyMode && getString(R.string.pref_face_beauty_close).equals(fbValue)) {
            this.mIsMTKFaceBeautyMode = false;
            sProxy.setCaptureMode(this.mParameters, "normal");
            sProxy.setFaceBeauty(this.mParameters, "false");
            sProxy.set3dnrMode(this.mParameters, "on");
            this.mHandler.obtainMessage(34, 0, 0, this).sendToTarget();
        } else if (!this.mIsMTKFaceBeautyMode && !getString(R.string.pref_face_beauty_close).equals(fbValue)) {
            this.mIsMTKFaceBeautyMode = true;
            stopFaceDetection(true);
            sProxy.setCaptureMode(this.mParameters, "face_beauty");
            sProxy.setFaceBeauty(this.mParameters, "true");
            sProxy.set3dnrMode(this.mParameters, "off");
            CameraHardwareFace face = null;
            if (this.mFaceNo == FaceNo.SINGLE) {
                sProxy.setExtremeBeauty(this.mParameters, "true");
                CameraHardwareFace[] faces = getUIController().getFaceView().getFaces();
                if (faces != null && faces.length >= 1) {
                    face = faces[0];
                }
            } else {
                sProxy.setExtremeBeauty(this.mParameters, "false");
            }
            updateFBParams(this.mInUseFBParams, fbValue, face);
            applyFBParams(this.mParameters, this.mInUseFBParams);
            this.mHandler.obtainMessage(34, 1, 0, this).sendToTarget();
        }
    }

    protected void prepareMultiCapture() {
        applyMultiShutParameters(true);
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setContinuousShotCallback(this.mContinuousShotCallback);
        }
    }

    protected void applyMultiShutParameters(boolean startshut) {
        String str;
        sProxy.setBurstShotNum(this.mParameters, startshut ? BURST_SHOOTING_COUNT : 1);
        MTKCameraProxy mTKCameraProxy = sProxy;
        Parameters parameters = this.mParameters;
        if (startshut) {
            str = "continuousshot";
        } else {
            str = "normal";
        }
        mTKCameraProxy.setCaptureMode(parameters, str);
    }

    protected void handleMultiSnapDone() {
        if (!this.mPaused) {
            if (this.mCameraDevice != null) {
                this.mCameraDevice.setContinuousShotCallback(null);
            }
            super.handleMultiSnapDone();
        }
    }

    protected void closeCamera() {
        if (this.mCameraDevice != null) {
            this.mCameraDevice.setContinuousShotCallback(null);
            this.mCameraDevice.setStereoWarningCallback(null);
        }
        super.closeCamera();
    }

    public void onSettingValueChanged(String key) {
        if (this.mCameraDevice != null) {
            if ("pref_qc_camera_iso_key".equals(key)) {
                String iso = this.mPreferences.getString("pref_qc_camera_iso_key", getString(R.string.pref_camera_iso_default));
                if (BaseModule.isSupported(iso, sProxy.getSupportedIsoValues(this.mParameters))) {
                    Log.v("Camera", "ISO value = " + iso);
                    sProxy.setISOValue(this.mParameters, iso);
                }
                this.mCameraDevice.setParametersAsync(this.mParameters);
            } else if ("pref_camera_stereo_key".equals(key)) {
                String vfLevel = this.mPreferences.getString("pref_camera_stereo_key", getString(R.string.pref_camera_stereo_default));
                Log.v("Camera", "Setting changed, vfLevel value = " + vfLevel);
                sProxy.setVsDofLevel(this.mParameters, vfLevel);
                this.mCameraDevice.setParametersAsync(this.mParameters);
                updateStatusBar("pref_camera_stereo_key");
            } else {
                super.onSettingValueChanged(key);
            }
        }
    }

    public void onFaceDetection(Face[] faces, Camera camera) {
        CameraHardwareFace face = null;
        super.onFaceDetection(faces, camera);
        if (Device.isSupportedSkinBeautify() && isUseMediaTekFaceBeautify()) {
            FaceView view = getUIController().getFaceView();
            if (view != null && view.faceExists() && view.isFaceStable()) {
                CameraHardwareFace[] cameraFaces = CameraHardwareFace.convertCameraHardwareFace(faces);
                FaceNo faceNo = getFaceNo(cameraFaces);
                if (faceNo == FaceNo.SINGLE || faceNo != this.mFaceNo) {
                    if (faceNo == FaceNo.SINGLE) {
                        face = cameraFaces[0];
                    }
                    if (face == null || (((double) face.gender) >= 0.001d && (face.gender <= 0.4f || face.gender >= 0.6f))) {
                        String fbValue = CameraSettings.getFaceBeautifyValue();
                        if (!getString(R.string.pref_face_beauty_close).equals(fbValue)) {
                            updateFBParams(this.mCurrentFBParams, fbValue, face);
                        }
                        if (!(faceNo == this.mFaceNo && this.mCurrentFBParams.equals(this.mInUseFBParams))) {
                            if (faceNo == FaceNo.SINGLE) {
                                sProxy.setExtremeBeauty(this.mParameters, "true");
                            } else {
                                sProxy.setExtremeBeauty(this.mParameters, "false");
                            }
                            applyFBParams(this.mParameters, this.mCurrentFBParams);
                            this.mCameraDevice.setParameters(this.mParameters);
                            this.mFaceNo = faceNo;
                            this.mInUseFBParams.copy(this.mCurrentFBParams);
                        }
                    }
                }
            }
        }
    }

    private FaceNo getFaceNo(CameraHardwareFace[] faces) {
        switch (faces == null ? 0 : faces.length) {
            case 0:
                return FaceNo.NONE;
            case 1:
                return FaceNo.SINGLE;
            default:
                return FaceNo.MULTIPLE;
        }
    }

    private void updateFBParams(FBParams fbParams, String fbValue, CameraHardwareFace face) {
        if (getString(R.string.pref_face_beauty_advanced).equals(fbValue)) {
            MtkFBParamsUtil.getAdvancedValue(fbParams);
        } else {
            FBLevel fbLevel;
            if (getString(R.string.pref_face_beauty_low).equals(fbValue)) {
                fbLevel = FBLevel.LOW;
            } else if (getString(R.string.pref_face_beauty_medium).equals(fbValue)) {
                fbLevel = FBLevel.MEDIUM;
            } else if (getString(R.string.pref_face_beauty_high).equals(fbValue)) {
                fbLevel = FBLevel.HIGH;
            } else {
                Log.w("Camera", "updateFBParams: unexpected fbMode " + fbValue);
                return;
            }
            MtkFBParamsUtil.getIntelligentValue(fbParams, fbLevel, face);
        }
    }

    private void applyFBParams(Parameters param, FBParams fbParams) {
        if (param == null || fbParams == null) {
            Log.w("Camera", "applyFBParams: unexpected null " + (param == null ? "cameraParam" : "fbParam"));
            return;
        }
        sProxy.setSmoothLevel(param, "" + fbParams.smoothLevel);
        sProxy.setEnlargeEye(param, "" + fbParams.enlargeEye);
        sProxy.setSlimFace(param, "" + fbParams.slimFace);
        sProxy.setSkinColor(param, "" + fbParams.skinColor);
    }

    private boolean enableZSL() {
        return (Device.IS_HM3Y || Device.IS_HM3Z || Device.IS_H3C) ? true : Device.IS_B6;
    }

    protected boolean isZeroShotMode() {
        return this.mIsZSLMode;
    }

    protected boolean isSupportSceneMode() {
        return true;
    }

    protected void setAutoExposure(Parameters parameters, String value) {
        List<String> aeList = sProxy.getSupportedAutoexposure(parameters);
        if (aeList != null && aeList.contains(value)) {
            sProxy.setAutoExposure(parameters, value);
        }
    }

    protected boolean isLongShotMode() {
        return this.mIsLongShotMode;
    }

    protected boolean isFaceBeautyMode() {
        return this.mIsMTKFaceBeautyMode;
    }

    protected void resetFaceBeautyMode() {
        this.mIsMTKFaceBeautyMode = false;
    }

    protected void prepareCapture() {
        setPictureFlipIfNeed(this.mParameters);
        setTimeWatermarkIfNeed();
        if (isFaceBeautyMode()) {
            setFacePoints(this.mParameters);
        }
    }

    protected boolean needAutoFocusBeforeCapture() {
        return isFlashWillOn(this.mCameraDevice.getParameters().get("flash-on"));
    }

    protected void updateCameraParametersPreference() {
        super.updateCameraParametersPreference();
        mtkUpdateCameraParametersPreference();
    }

    protected boolean needSetupPreview(boolean zslMode) {
        String previewStopped = this.mCameraDevice.getParameters().get("preview-stopped");
        return previewStopped != null ? "1".equals(previewStopped) : true;
    }

    private void setPictureFlipIfNeed(Parameters parameters) {
        if (isFrontMirror()) {
            sProxy.setPictureFlip(parameters, "1");
        } else {
            sProxy.setPictureFlip(parameters, "0");
        }
        Log.d("Camera", "Picture flip value = " + sProxy.getPictureFlip(parameters));
    }

    private static boolean isSupportContinuousShut(Parameters parameters) {
        List<String> supportedCaptureMode = sProxy.getSupportedCaptureMode(parameters);
        if (supportedCaptureMode != null && supportedCaptureMode.indexOf("continuousshot") >= 0) {
            return true;
        }
        return false;
    }

    protected void cancelContinuousShot() {
        this.mCameraDevice.cancelPicture();
    }

    private boolean isFlashWillOn(String paramsFlashOn) {
        String flashMode = this.mParameters.getFlashMode();
        if ("auto".equals(flashMode) && "1".equals(paramsFlashOn)) {
            return true;
        }
        return "on".equals(flashMode);
    }

    private String flattenFaces(CameraHardwareFace[] faces) {
        int length = 0;
        if (faces != null) {
            length = faces.length;
        }
        if (length == 0) {
            return null;
        }
        StringBuilder value = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int y = faces[i].rect.top + ((faces[i].rect.bottom - faces[i].rect.top) / 2);
            value.append(faces[i].rect.left + ((faces[i].rect.right - faces[i].rect.left) / 2)).append(":").append(y);
            if (i != length - 1) {
                value.append(",");
            }
        }
        return value.toString();
    }

    private void setFacePoints(Parameters parameters) {
        String value = flattenFaces(getUIController().getFaceView().getFaces());
        if (value != null) {
            sProxy.setFacePosition(parameters, value);
        }
    }

    protected PictureSize getBestPictureSize() {
        List<Size> sizes;
        if (Device.isSupportedStereo() && CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            sizes = sProxy.getSupportedStereoPictureSizes(this.mCameraDevice, this.mParameters);
        } else {
            sizes = this.mParameters.getSupportedPictureSizes();
        }
        PictureSizeManager.initialize(getActivity(), sizes, getMaxPictureSize());
        return PictureSizeManager.getBestPictureSize();
    }

    protected void prepareOpenCamera() {
        closeCamera();
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key") && !this.mIsImageCaptureIntent) {
            sProxy.enableStereoMode();
        }
    }

    protected void initializeAfterCameraOpen() {
        super.initializeAfterCameraOpen();
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            if (this.mSaveHandler == null) {
                HandlerThread ht = new HandlerThread("Stereo Save Handler Thread");
                ht.start();
                this.mSaveHandler = new SaveHandler(ht.getLooper());
            }
            this.mIsStereoCapture = true;
            return;
        }
        this.mIsStereoCapture = false;
    }

    public void onDestroy() {
        super.onDestroy();
        if (this.mSaveHandler != null) {
            this.mSaveHandler.getLooper().quit();
            this.mSaveHandler = null;
        }
    }

    private void notifyMergeData() {
        Log.i("Camera", "notifyMergeData mCurrentNum = " + this.mCurrentNum);
        this.mCurrentNum++;
        if (this.mCurrentNum == 6) {
            Log.i("Camera", "notifyMergeData Vs Dof " + this.mIsStereoCapture);
            setupPreview();
            if (this.mIsStereoCapture) {
                this.mSaveHandler.obtainMessage(10004, new StereoDataGroup(Util.createJpegName(System.currentTimeMillis()) + "_STEREO", this.mOriginalJpegData, this.mJpsData, this.mMaskAndConfigData, this.mDepthMap, this.mClearImage, this.mLdcData)).sendToTarget();
            }
            this.mCurrentNum = 0;
        }
    }

    private Object constructObject() {
        return !Device.isSupportedStereo() ? null : null;
    }

    private byte[] writeStereoCaptureInfoToJpg(String pictureName, byte[] originalJpegData, byte[] jpsData, byte[] maskAndConfigDat, byte[] clearImage, byte[] depthMap, byte[] ldcData) {
        return null;
    }

    private void setJsonBuffer(byte[] maskAndConfigData) {
    }

    public void onCameraStartPreview() {
        if (CameraSettings.isSwitchOn("pref_camera_stereo_mode_key")) {
            this.mStereoCameraWarningCallback.setActivity(this.mActivity);
            this.mCameraDevice.setStereoWarningCallback(this.mStereoCameraWarningCallback);
            if (CameraSettings.isDualCameraHintShown(this.mPreferences)) {
                this.mHandler.sendEmptyMessage(40);
            }
        }
    }

    private boolean isUseMediaTekFaceBeautify() {
        return !Device.IS_HM3Y ? Device.IS_HM3Z : true;
    }
}
