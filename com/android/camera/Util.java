package com.android.camera;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.app.AlertDialog.Builder;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Country;
import android.location.CountryDetector;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.ParcelFileDescriptor;
import android.os.SystemProperties;
import android.provider.Settings.SettingNotFoundException;
import android.provider.Settings.System;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.text.format.Time;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.TouchDelegate;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;
import com.android.camera.CameraManager.CameraProxy;
import com.android.camera.aosp_porting.FeatureParser;
import com.android.camera.aosp_porting.ReflectUtil;
import com.android.camera.effect.EffectController;
import com.android.camera.preferences.CameraSettingPreferences;
import com.android.camera.steganography.SteganographyUtils;
import com.android.camera.storage.Storage;
import com.android.camera.ui.V6ModulePicker;
import dalvik.system.VMRuntime;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class Util {
    private static HashSet<String> ANTIBANDING_60_COUNTRY = new HashSet(Arrays.asList(new String[]{"TW", "KR", "SA", "US", "CA", "BR", "CO", "MX", "PH"}));
    private static final File INTERNAL_STORAGE_DIRECTORY = new File("/data/sdcard");
    private static String mCountryIso = null;
    private static int mLockedOrientation = -1;
    private static boolean sClearMemoryLimit;
    private static ImageFileNamer sImageFileNamer;
    public static boolean sIsDumpLog;
    private static float sPixelDensity = 1.0f;
    private static HashMap<String, Typeface> sTypefaces = new HashMap();
    public static int sWindowHeight = 1080;
    public static int sWindowWidth = 720;

    private static class ImageFileNamer {
        private SimpleDateFormat mFormat;
        private long mLastDate;
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            this.mFormat = new SimpleDateFormat(format);
        }

        public String generateName(long dateTaken) {
            String result = this.mFormat.format(new Date(dateTaken));
            if (dateTaken / 1000 == this.mLastDate / 1000) {
                this.mSameSecondCount++;
                return result + "_" + this.mSameSecondCount;
            }
            this.mLastDate = dateTaken;
            this.mSameSecondCount = 0;
            return result;
        }
    }

    private Util() {
    }

    public static boolean isAntibanding60() {
        return ANTIBANDING_60_COUNTRY.contains(mCountryIso);
    }

    public static void updateCountryIso(Context context) {
        Country detectedCountry = ((CountryDetector) context.getSystemService("country_detector")).detectCountry();
        if (detectedCountry != null) {
            mCountryIso = detectedCountry.getCountryIso();
        }
        Log.v("CameraUtil", "antiBanding mCountryIso=" + mCountryIso);
        sIsDumpLog = SystemProperties.getBoolean("camera_dump_parameters", false);
    }

    public static void initialize(Context context) {
        sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
        getWindowAttribute(context);
    }

    public static void getWindowAttribute(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService("window");
        wm.getDefaultDisplay().getMetrics(metrics);
        sPixelDensity = metrics.noncompatDensity;
        Display display = wm.getDefaultDisplay();
        Point p = new Point();
        if (Device.IS_A8 || Device.IS_D5) {
            ReflectUtil.callMethod(Display.class, display, "getRealSize", "(Landroid/graphics/Point;Z)V", p, Boolean.valueOf(true));
        } else {
            display.getRealSize(p);
        }
        if (p.x < p.y) {
            sWindowWidth = p.x;
            sWindowHeight = p.y;
        } else {
            sWindowWidth = p.y;
            sWindowHeight = p.x;
        }
        Log.d("CameraUtil", "Width = " + sWindowWidth + " Height = " + sWindowHeight + " sPixelDensity=" + sPixelDensity);
    }

    public static void clearMemoryLimit() {
        if (!sClearMemoryLimit) {
            long start = System.currentTimeMillis();
            VMRuntime.getRuntime().clearGrowthLimit();
            sClearMemoryLimit = true;
            Log.v("CameraUtil", "clearMemoryLimit() consume:" + (System.currentTimeMillis() - start));
        }
    }

    public static int dpToPixel(float dp) {
        return Math.round(sPixelDensity * dp);
    }

    public static Bitmap rotate(Bitmap b, int degrees) {
        return rotateAndMirror(b, degrees, false);
    }

    public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror) {
        if ((degrees == 0 && !mirror) || b == null) {
            return b;
        }
        Matrix m = new Matrix();
        if (mirror) {
            m.postScale(-1.0f, 1.0f);
            degrees = (degrees + 360) % 360;
            if (degrees == 0 || degrees == 180) {
                m.postTranslate((float) b.getWidth(), 0.0f);
            } else if (degrees == 90 || degrees == 270) {
                m.postTranslate((float) b.getHeight(), 0.0f);
            } else {
                throw new IllegalArgumentException("Invalid degrees=" + degrees);
            }
        }
        if (degrees != 0) {
            m.postRotate((float) degrees, ((float) b.getWidth()) / 2.0f, ((float) b.getHeight()) / 2.0f);
        }
        try {
            Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (b == b2) {
                return b;
            }
            b.recycle();
            return b2;
        } catch (OutOfMemoryError e) {
            return b;
        }
    }

    public static int computeSampleSize(Options options, int minSideLength, int maxNumOfPixels) {
        int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);
        if (initialSize > 8) {
            return ((initialSize + 7) / 8) * 8;
        }
        int roundedSize = 1;
        while (roundedSize < initialSize) {
            roundedSize <<= 1;
        }
        return roundedSize;
    }

    private static int computeInitialSampleSize(Options options, int minSideLength, int maxNumOfPixels) {
        int lowerBound;
        int upperBound;
        double w = (double) options.outWidth;
        double h = (double) options.outHeight;
        if (maxNumOfPixels < 0) {
            lowerBound = 1;
        } else {
            lowerBound = (int) Math.ceil(Math.sqrt((w * h) / ((double) maxNumOfPixels)));
        }
        if (minSideLength < 0) {
            upperBound = 128;
        } else {
            upperBound = (int) Math.min(Math.floor(w / ((double) minSideLength)), Math.floor(h / ((double) minSideLength)));
        }
        if (upperBound < lowerBound) {
            return lowerBound;
        }
        if (maxNumOfPixels < 0 && minSideLength < 0) {
            return 1;
        }
        if (minSideLength < 0) {
            return lowerBound;
        }
        return upperBound;
    }

    public static byte[] sealInvisibleWatermark(byte[] jpegData, int maxNumOfPixels, String watermark) {
        Bitmap bitmap = makeBitmap(jpegData, maxNumOfPixels);
        if (bitmap == null) {
            return null;
        }
        int orientation = Exif.getOrientation(jpegData);
        if (orientation != 0) {
            bitmap = rotate(bitmap, orientation);
        }
        Bitmap encodeBitmap = SteganographyUtils.encodeWatermark(bitmap, watermark);
        bitmap.recycle();
        if (encodeBitmap == null) {
            return null;
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        encodeBitmap.compress(CompressFormat.PNG, 100, os);
        encodeBitmap.recycle();
        return os.toByteArray();
    }

    public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels) {
        try {
            Options options = new Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
            if (options.mCancel || options.outWidth == -1 || options.outHeight == -1) {
                return null;
            }
            options.inSampleSize = computeSampleSize(options, -1, maxNumOfPixels);
            options.inJustDecodeBounds = false;
            options.inDither = false;
            options.inPreferredConfig = Config.ARGB_8888;
            return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
        } catch (OutOfMemoryError ex) {
            Log.e("CameraUtil", "Got oom exception ", ex);
            return null;
        }
    }

    public static void closeSilently(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Exception e) {
            }
        }
    }

    public static void Assert(boolean cond) {
        if (!cond) {
            throw new AssertionError();
        }
    }

    public static CameraProxy openCamera(Activity activity, int cameraId) throws CameraHardwareException, CameraDisabledException {
        if (((DevicePolicyManager) activity.getSystemService("device_policy")).getCameraDisabled(null)) {
            throw new CameraDisabledException();
        }
        try {
            return CameraHolder.instance().open(cameraId, ((ActivityBase) activity).getCurrentModule().isCaptureIntent() ? isPortraitIntent((ActivityBase) activity) : true);
        } catch (CameraHardwareException e) {
            if ("eng".equals(Build.TYPE)) {
                throw new RuntimeException("openCamera failed", e);
            }
            throw e;
        }
    }

    public static void showErrorAndFinish(final Activity activity, int msgId) {
        new Builder(activity).setCancelable(false).setIconAttribute(16843605).setTitle(R.string.camera_error_title).setMessage(msgId).setNeutralButton(R.string.dialog_ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                activity.finish();
            }
        }).show();
    }

    public static <T> T checkNotNull(T object) {
        if (object != null) {
            return object;
        }
        throw new NullPointerException();
    }

    public static boolean equals(Object a, Object b) {
        if (a != b) {
            return a == null ? false : a.equals(b);
        } else {
            return true;
        }
    }

    public static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    public static void checkLockedOrientation(Activity activity) {
        try {
            if (System.getInt(activity.getContentResolver(), "accelerometer_rotation") == 0) {
                mLockedOrientation = System.getInt(activity.getContentResolver(), "user_rotation");
            } else {
                mLockedOrientation = -1;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static int getShootOrientation(Activity activity, int orientation) {
        return ((orientation - getDisplayRotation(activity)) + 360) % 360;
    }

    public static float getShootRotation(Activity activity, float rotation) {
        rotation -= (float) getDisplayRotation(activity);
        while (rotation < 0.0f) {
            rotation += 360.0f;
        }
        while (rotation > 360.0f) {
            rotation -= 360.0f;
        }
        return rotation;
    }

    public static int getDisplayRotation(Activity activity) {
        int rotation = 0;
        if (activity.getRequestedOrientation() == 7) {
            rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        } else if (mLockedOrientation == 0 || mLockedOrientation == 2) {
            rotation = mLockedOrientation;
        }
        switch (rotation) {
            case 0:
                return 0;
            case 1:
                return 90;
            case 2:
                return 180;
            case 3:
                return 270;
            default:
                return 0;
        }
    }

    public static boolean isActivityInvert(Activity activity) {
        return getDisplayRotation(activity) == 180;
    }

    public static int getDisplayOrientation(int degrees, int cameraId) {
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        if (info.facing == 1) {
            return (360 - ((info.orientation + degrees) % 360)) % 360;
        }
        return ((info.orientation - degrees) + 360) % 360;
    }

    public static int roundOrientation(int orientation, int orientationHistory) {
        boolean changeOrientation;
        if (orientationHistory == -1) {
            changeOrientation = true;
        } else {
            int dist = Math.abs(orientation - orientationHistory);
            changeOrientation = Math.min(dist, 360 - dist) >= 50;
        }
        if (changeOrientation) {
            return (((orientation + 45) / 90) * 90) % 360;
        }
        return orientationHistory;
    }

    public static Size getOptimalPreviewSize(Activity currentActivity, List<Size> sizes, double targetRatio) {
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        Size optimalSizeSmaller = null;
        double minDiff = Double.MAX_VALUE;
        double minDiffSmaller = Double.MAX_VALUE;
        boolean small = false;
        int reduceFlag = FeatureParser.getInteger("camera_reduce_preview_flag", 0);
        if (reduceFlag != 0) {
            boolean frontCamera = CameraSettingPreferences.instance().isFrontCamera();
            if (sWindowWidth < 1080) {
                reduceFlag &= -15;
            }
            small = (reduceFlag & (((frontCamera ? 2 : 1) << (!V6ModulePicker.isVideoModule() ? 0 : 2)) | 0)) != 0;
        }
        Point point = new Point(sWindowWidth, sWindowHeight);
        int limitWidth = (Device.isMDPRender() || !Device.isSurfaceSizeLimited()) ? 1080 : 720;
        if (point.x > limitWidth) {
            point.y = (point.y * limitWidth) / point.x;
            point.x = limitWidth;
        }
        for (Size size : sizes) {
            int diff;
            if (Math.abs((((double) size.width) / ((double) size.height)) - targetRatio) <= 0.02d && (!small || (point.x > size.height && point.y > size.width))) {
                diff = Math.abs(point.x - size.height) + Math.abs(point.y - size.width);
                if (diff == 0) {
                    optimalSize = size;
                    optimalSizeSmaller = size;
                    break;
                }
                if (size.height <= point.x && size.width <= point.y && ((double) diff) < minDiffSmaller) {
                    optimalSizeSmaller = size;
                    minDiffSmaller = (double) diff;
                }
                if (((double) diff) < minDiff) {
                    optimalSize = size;
                    minDiff = (double) diff;
                }
            }
        }
        if (optimalSizeSmaller != null) {
            optimalSize = optimalSizeSmaller;
        }
        if (optimalSize == null) {
            Log.w("CameraUtil", "No preview size match the aspect ratio");
            minDiff = Double.MAX_VALUE;
            for (Size size2 : sizes) {
                diff = Math.abs(point.x - size2.height) + Math.abs(point.y - size2.width);
                if (((double) diff) < minDiff) {
                    optimalSize = size2;
                    minDiff = (double) diff;
                }
            }
        }
        if (optimalSize != null) {
            Log.i("CameraUtil", "The best preview size is :(" + optimalSize.width + " , " + optimalSize.height + ")");
        }
        return optimalSize;
    }

    public static Size getOptimalJpegThumbnailSize(List<Size> sizes, double targetRatio) {
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        double approachingRatio = 0.0d;
        for (Size size : sizes) {
            if (!(size.width == 0 || size.height == 0)) {
                double ratio = ((double) size.width) / ((double) size.height);
                double absRatio = Math.abs(ratio - targetRatio);
                if (absRatio <= Math.abs(approachingRatio - targetRatio) || absRatio <= 0.001d) {
                    if (optimalSize != null && absRatio >= Math.abs(approachingRatio - targetRatio)) {
                        if (size.width > optimalSize.width) {
                        }
                    }
                    optimalSize = size;
                    approachingRatio = ratio;
                }
            }
        }
        if (optimalSize == null) {
            Log.w("CameraUtil", "No thumbnail size match the aspect ratio");
            for (Size size2 : sizes) {
                if (optimalSize == null || size2.width > optimalSize.width) {
                    optimalSize = size2;
                }
            }
        }
        return optimalSize;
    }

    public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes, double targetRatio, int maxWidth, int maxHeight) {
        if (sizes == null) {
            return null;
        }
        Size optimalSize = null;
        for (Size size : sizes) {
            if (Math.abs((((double) size.width) / ((double) size.height)) - targetRatio) <= 0.02d && ((optimalSize == null || size.width > optimalSize.width) && size.width <= maxWidth && size.height <= maxHeight)) {
                optimalSize = size;
            }
        }
        if (optimalSize == null) {
            Log.w("CameraUtil", "No picture size match the aspect ratio");
            for (Size size2 : sizes) {
                if (optimalSize == null || size2.width > optimalSize.width) {
                    optimalSize = size2;
                }
            }
        }
        return optimalSize;
    }

    public static int getStartCameraId(Activity currentActivity) {
        int id = -1;
        if (currentActivity.getIntent().getBooleanExtra("android.intent.extras.START_WITH_FRONT_CAMERA", false)) {
            id = CameraHolder.instance().getFrontCameraId();
        } else if (currentActivity.getIntent().getBooleanExtra("android.intent.extras.START_WITH_BACK_CAMERA", false)) {
            id = CameraHolder.instance().getBackCameraId();
        }
        currentActivity.getIntent().removeExtra("android.intent.extras.START_WITH_FRONT_CAMERA");
        currentActivity.getIntent().removeExtra("android.intent.extras.START_WITH_BACK_CAMERA");
        return id;
    }

    public static int getStartModuleIndex(Activity currentActivity) {
        if ("android.media.action.STILL_IMAGE_CAMERA".equals(currentActivity.getIntent().getAction())) {
            return 0;
        }
        if ("android.media.action.VIDEO_CAMERA".equals(currentActivity.getIntent().getAction())) {
            return 1;
        }
        return -1;
    }

    public static int replaceStartEffectRender(Activity currentActivity) {
        if (Device.isSupportedShaderEffect()) {
            String resName = currentActivity.getIntent().getStringExtra("android.intent.extras.START_WITH_EFFECT_RENDER");
            if (resName != null) {
                int resId = currentActivity.getResources().getIdentifier(resName, "string", currentActivity.getPackageName());
                if (resId != 0) {
                    int effect = EffectController.getInstance().getEffectIndexByEntryName(currentActivity.getResources().getString(resId));
                    CameraSettings.setShaderEffect(effect);
                    return effect;
                }
            }
        }
        return 0;
    }

    public static boolean getNeedSealCameraUUIDIntentExtras(Activity currentActivity) {
        return currentActivity.getIntent().getBooleanExtra("android.intent.extras.EXTRAS_SEAL_CAMERAUUID_WATERMARK", false);
    }

    public static int getCameraFacingIntentExtras(Activity currentActivity) {
        int intentCameraId = currentActivity.getIntent().getIntExtra("android.intent.extras.CAMERA_FACING", -1);
        currentActivity.getIntent().removeExtra("android.intent.extras.CAMERA_FACING");
        if (intentCameraId == -1 && ((currentActivity.getIntent().getBooleanExtra("KEY_HANDOVER_THROUGH_VELVET", false) || TextUtils.equals(currentActivity.getIntent().getStringExtra("android.intent.extra.REFERRER_NAME"), "android-app://com.google.android.googlequicksearchbox/https/www.google.com")) && currentActivity.getIntent().hasExtra("android.intent.extra.USE_FRONT_CAMERA"))) {
            intentCameraId = currentActivity.getIntent().getBooleanExtra("android.intent.extra.USE_FRONT_CAMERA", false) ? 1 : CameraHolder.instance().getBackCameraId();
        }
        if (isFrontCameraIntent(intentCameraId)) {
            int frontCameraId = CameraHolder.instance().getFrontCameraId();
            if (frontCameraId != -1) {
                return frontCameraId;
            }
            return -1;
        } else if (isBackCameraIntent(intentCameraId)) {
            if (isViceBackIntent((ActivityBase) currentActivity)) {
                int viceBackId = CameraHolder.instance().getViceBackCameraId();
                if (viceBackId != -1) {
                    return viceBackId;
                }
                return -1;
            }
            int backCameraId = CameraHolder.instance().getBackCameraId();
            if (backCameraId != -1) {
                return backCameraId;
            }
            return -1;
        } else if (!isPortraitIntent((ActivityBase) currentActivity)) {
            return -1;
        } else {
            int backId = CameraHolder.instance().getBackCameraId();
            if (backId != -1) {
                return backId;
            }
            return -1;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isViceBackIntent(com.android.camera.ActivityBase r3) {
        /*
        r2 = 0;
        if (r3 == 0) goto L_0x0009;
    L_0x0003:
        r0 = r3.getIntent();
        if (r0 != 0) goto L_0x000a;
    L_0x0009:
        return r2;
    L_0x000a:
        r0 = r3.isImageCaptureIntent();
        if (r0 != 0) goto L_0x0011;
    L_0x0010:
        return r2;
    L_0x0011:
        r0 = r3.getIntent();
        r1 = "android.intent.extras.EXTRAS_CAMERA_VICE_BACK";
        r0 = r0.getBooleanExtra(r1, r2);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.Util.isViceBackIntent(com.android.camera.ActivityBase):boolean");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean isPortraitIntent(com.android.camera.ActivityBase r3) {
        /*
        r2 = 0;
        r0 = com.android.camera.CameraSettings.isSupportedPortrait();
        if (r0 != 0) goto L_0x0008;
    L_0x0007:
        return r2;
    L_0x0008:
        if (r3 == 0) goto L_0x0010;
    L_0x000a:
        r0 = r3.getIntent();
        if (r0 != 0) goto L_0x0011;
    L_0x0010:
        return r2;
    L_0x0011:
        r0 = r3.isImageCaptureIntent();
        if (r0 != 0) goto L_0x0018;
    L_0x0017:
        return r2;
    L_0x0018:
        r0 = r3.getIntent();
        r1 = "android.intent.extras.PORTRAIT";
        r0 = r0.getBooleanExtra(r1, r2);
        return r0;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.Util.isPortraitIntent(com.android.camera.ActivityBase):boolean");
    }

    private static boolean isFrontCameraIntent(int intentCameraId) {
        return intentCameraId == 1;
    }

    private static boolean isBackCameraIntent(int intentCameraId) {
        return intentCameraId == 0;
    }

    public static boolean pointInView(float x, float y, View v) {
        boolean z = true;
        if (v == null) {
            return false;
        }
        int[] location = new int[2];
        v.getLocationInWindow(location);
        if (x < ((float) location[0]) || x >= ((float) (location[0] + v.getWidth())) || y < ((float) location[1])) {
            z = false;
        } else if (y >= ((float) (location[1] + v.getHeight()))) {
            z = false;
        }
        return z;
    }

    public static int[] getRelativeLocation(View reference, View view) {
        int[] location = new int[2];
        reference.getLocationInWindow(location);
        int referenceX = location[0];
        int referenceY = location[1];
        view.getLocationInWindow(location);
        location[0] = location[0] - referenceX;
        location[1] = location[1] - referenceY;
        return location;
    }

    public static boolean isUriValid(Uri uri, ContentResolver resolver) {
        if (uri == null) {
            return false;
        }
        try {
            ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
            if (pfd == null) {
                Log.e("CameraUtil", "Fail to open URI. URI=" + uri);
                return false;
            }
            pfd.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void rectFToRect(RectF rectF, Rect rect) {
        rect.left = Math.round(rectF.left);
        rect.top = Math.round(rectF.top);
        rect.right = Math.round(rectF.right);
        rect.bottom = Math.round(rectF.bottom);
    }

    public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth, int viewHeight, int centerX, int centerY) {
        matrix.setScale((float) (mirror ? -1 : 1), 1.0f);
        matrix.postRotate((float) displayOrientation);
        matrix.postScale(((float) viewWidth) / 2000.0f, ((float) viewHeight) / 2000.0f);
        matrix.postTranslate((float) centerX, (float) centerY);
    }

    public static String createJpegName(long dateTaken) {
        String generateName;
        synchronized (sImageFileNamer) {
            generateName = sImageFileNamer.generateName(dateTaken);
        }
        return generateName;
    }

    public static void broadcastNewPicture(Context context, Uri uri) {
        if (VERSION.SDK_INT < 24) {
            context.sendBroadcast(new Intent("android.hardware.action.NEW_PICTURE", uri));
            context.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
        }
    }

    public static void fadeIn(View view, int duration) {
        if (view != null && view.getVisibility() != 0) {
            view.setVisibility(0);
            Animation animation = new AlphaAnimation(0.0f, 1.0f);
            animation.setDuration((long) duration);
            view.clearAnimation();
            view.startAnimation(animation);
        }
    }

    public static void fadeIn(View view) {
        fadeIn(view, 400);
    }

    public static void fadeOut(View view, int duration) {
        if (view != null && view.getVisibility() == 0) {
            Animation animation = new AlphaAnimation(1.0f, 0.0f);
            animation.setDuration((long) duration);
            view.clearAnimation();
            view.startAnimation(animation);
            view.setVisibility(8);
        }
    }

    public static void fadeOut(View view) {
        fadeOut(view, 400);
    }

    public static int getJpegRotation(int cameraId, int orientation) {
        CameraInfo info = CameraHolder.instance().getCameraInfo()[cameraId];
        if (orientation == -1) {
            return info.orientation;
        }
        if (info.facing == 1) {
            return ((info.orientation - orientation) + 360) % 360;
        }
        return (info.orientation + orientation) % 360;
    }

    public static void setGpsParameters(Parameters parameters, Location loc) {
        boolean hasLatLon = true;
        parameters.removeGpsData();
        parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);
        if (loc != null) {
            double lat = loc.getLatitude();
            double lon = loc.getLongitude();
            if (lat == 0.0d && lon == 0.0d) {
                hasLatLon = false;
            }
            if (hasLatLon) {
                Log.d("CameraUtil", "Set gps location");
                parameters.setGpsLatitude(lat);
                parameters.setGpsLongitude(lon);
                parameters.setGpsProcessingMethod(loc.getProvider().toUpperCase());
                if (loc.hasAltitude()) {
                    parameters.setGpsAltitude(loc.getAltitude());
                } else {
                    parameters.setGpsAltitude(0.0d);
                }
                if (loc.getTime() != 0) {
                    parameters.setGpsTimestamp(loc.getTime() / 1000);
                }
            }
        }
    }

    public static void setRotationParameter(Parameters parameters, int cameraId, int orientation) {
        int rotation = 0;
        if (orientation != -1) {
            CameraInfo info = CameraHolder.instance().getCameraInfo()[cameraId];
            if (info.facing == 1) {
                rotation = ((info.orientation - orientation) + 360) % 360;
            } else {
                rotation = (info.orientation + orientation) % 360;
            }
        }
        parameters.setRotation(rotation);
    }

    public static boolean isSupported(String value, List<String> supported) {
        return supported != null && supported.indexOf(value) >= 0;
    }

    public static boolean isLayoutRTL(Context context) {
        boolean z = true;
        if (context == null) {
            return false;
        }
        if (context.getResources().getConfiguration().getLayoutDirection() != 1) {
            z = false;
        }
        return z;
    }

    public static boolean mkdirs(File file, int mode, int uid, int gid) {
        if (file.exists()) {
            return false;
        }
        String parentDir = file.getParent();
        if (parentDir != null) {
            mkdirs(new File(parentDir), mode, uid, gid);
        }
        return file.mkdir();
    }

    public static boolean createFile(File file) {
        if (file.exists()) {
            return false;
        }
        String parentDir = file.getParent();
        if (parentDir != null) {
            mkdirs(new File(parentDir), 511, -1, -1);
        }
        try {
            file.createNewFile();
        } catch (IOException e) {
        }
        return true;
    }

    public static boolean isTimeout(long now, long last, long gap) {
        return now < last || now - last > gap;
    }

    public static Typeface getMiuiTypeface(Context context) {
        return getTypeface(context, "fonts/MIUI_Normal.ttf");
    }

    public static Typeface getMiuiTimeTypeface(Context context) {
        return getTypeface(context, "fonts/MIUI_Time.ttf");
    }

    private static synchronized Typeface getTypeface(Context context, String fontName) {
        Typeface typeface;
        synchronized (Util.class) {
            if (!sTypefaces.containsKey(fontName)) {
                sTypefaces.put(fontName, Typeface.createFromAsset(context.getAssets(), fontName));
            }
            typeface = (Typeface) sTypefaces.get(fontName);
        }
        return typeface;
    }

    public static boolean isProduceFocusInfoSuccess(byte[] depthMap) {
        return depthMap != null && 25 < depthMap.length && depthMap[depthMap.length - 25] == (byte) 0;
    }

    public static int getCenterFocusDepthIndex(byte[] depthMap, int imageWidth, int imageHeight) {
        if (depthMap == null || depthMap.length < 25) {
            return 1;
        }
        int metaDataIndex = depthMap.length - 25;
        int metaDataIndex2 = metaDataIndex + 1;
        if (depthMap[metaDataIndex] != (byte) 0) {
            return 1;
        }
        metaDataIndex = metaDataIndex2 + 1;
        metaDataIndex2 = metaDataIndex + 1;
        metaDataIndex = metaDataIndex2 + 1;
        metaDataIndex2 = metaDataIndex + 1;
        int mapwidth = ((((depthMap[metaDataIndex2] & 255) << 24) | ((depthMap[metaDataIndex] & 255) << 16)) | ((depthMap[metaDataIndex2] & 255) << 8)) | (depthMap[metaDataIndex] & 255);
        metaDataIndex = metaDataIndex2 + 1;
        metaDataIndex2 = metaDataIndex + 1;
        metaDataIndex = metaDataIndex2 + 1;
        metaDataIndex2 = metaDataIndex + 1;
        int mapheight = ((((depthMap[metaDataIndex2] & 255) << 24) | ((depthMap[metaDataIndex] & 255) << 16)) | ((depthMap[metaDataIndex2] & 255) << 8)) | (depthMap[metaDataIndex] & 255);
        int centerWidth = CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.focus_area_width);
        int width = (mapwidth * centerWidth) / sWindowWidth;
        int height = (int) (((float) (mapheight * CameraAppImpl.getAndroidContext().getResources().getDimensionPixelSize(R.dimen.focus_area_height))) / ((((float) sWindowWidth) * ((float) imageHeight)) / ((float) imageWidth)));
        int[] countArray = new int[5];
        int i = 0;
        int row = (mapheight - height) / 2;
        while (i < height) {
            int row2 = row + 1;
            int j = 0;
            int colindex = (row * mapwidth) + ((mapwidth - width) / 2);
            while (j < width) {
                int colindex2 = colindex + 1;
                byte b = depthMap[colindex];
                countArray[b] = countArray[b] + 1;
                j++;
                colindex = colindex2;
            }
            i++;
            row = row2;
        }
        int maxIndex = 0;
        for (i = 1; i < 5; i++) {
            if (countArray[maxIndex] < countArray[i]) {
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public static void expandViewTouchDelegate(View view) {
        if (view.isShown()) {
            Rect bounds = new Rect();
            view.getHitRect(bounds);
            int delegate = dpToPixel(10.0f);
            bounds.top -= delegate;
            bounds.bottom += delegate;
            bounds.left -= delegate;
            bounds.right += delegate;
            TouchDelegate touchDelegate = new TouchDelegate(bounds, view);
            if (View.class.isInstance(view.getParent())) {
                ((View) view.getParent()).setTouchDelegate(touchDelegate);
            }
        } else if (View.class.isInstance(view.getParent())) {
            ((View) view.getParent()).setTouchDelegate(null);
        }
    }

    public static String getTimeWatermark() {
        return getTimeWatermark(Device.isSupportedNewStyleTimeWaterMark());
    }

    public static String getTimeWatermark(boolean isNewStyle) {
        StringBuilder sb = new StringBuilder();
        if (isNewStyle) {
            sb.append(new SimpleDateFormat("yyyy/M/d", Locale.ENGLISH).format(new Date()).toCharArray());
        } else {
            sb.append(new SimpleDateFormat("yyyy-M-d", Locale.ENGLISH).format(new Date()).toCharArray());
        }
        sb.append(" ");
        new Time().set(System.currentTimeMillis());
        sb.append(String.format(Locale.ENGLISH, "%02d", new Object[]{Integer.valueOf(time.hour)}));
        sb.append(":");
        sb.append(String.format(Locale.ENGLISH, "%02d", new Object[]{Integer.valueOf(time.minute)}));
        return sb.toString();
    }

    public static int safeDelete(Uri url, String where, String[] selectionArgs) {
        int deleteResult = -1;
        try {
            deleteResult = CameraAppImpl.getAndroidContext().getContentResolver().delete(url, where, selectionArgs);
            Log.v("CameraUtil", "safeDelete url=" + url + " where=" + where + " selectionArgs=" + selectionArgs + " result=" + deleteResult);
            return deleteResult;
        } catch (Exception e) {
            e.printStackTrace();
            return deleteResult;
        }
    }

    public static boolean isShowDebugInfo() {
        return ("1".equals(SystemProperties.get("persist.camera.enable.log")) || "1".equals(SystemProperties.get("persist.camera.debug.show_af")) || "1".equals(SystemProperties.get("persist.camera.debug.show_awb")) || "1".equals(SystemProperties.get("persist.camera.debug.show_aec")) || "1".equals(SystemProperties.get("persist.camera.debug.autoscene"))) ? true : "1".equals(SystemProperties.get("persist.camera.debug.hht"));
    }

    public static String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        if ("1".equals(SystemProperties.get("persist.camera.debug.show_af")) || "1".equals(SystemProperties.get("persist.camera.debug.enable"))) {
            sb.append(addProperties("persist.camera.debug.param0"));
            sb.append(addProperties("persist.camera.debug.param1"));
            sb.append(addProperties("persist.camera.debug.param2"));
            sb.append(addProperties("persist.camera.debug.param3"));
            sb.append(addProperties("persist.camera.debug.param4"));
            sb.append(addProperties("persist.camera.debug.param5"));
            sb.append(addProperties("persist.camera.debug.param6"));
            sb.append(addProperties("persist.camera.debug.param7"));
            sb.append(addProperties("persist.camera.debug.param8"));
            sb.append(addProperties("persist.camera.debug.param9"));
        }
        if ("1".equals(SystemProperties.get("persist.camera.debug.show_awb"))) {
            sb.append(addProperties("persist.camera.debug.param10"));
            sb.append(addProperties("persist.camera.debug.param11"));
            sb.append(addProperties("persist.camera.debug.param12"));
            sb.append(addProperties("persist.camera.debug.param13"));
            sb.append(addProperties("persist.camera.debug.param14"));
            sb.append(addProperties("persist.camera.debug.param15"));
            sb.append(addProperties("persist.camera.debug.param16"));
            sb.append(addProperties("persist.camera.debug.param17"));
            sb.append(addProperties("persist.camera.debug.param18"));
            sb.append(addProperties("persist.camera.debug.param19"));
        }
        if ("1".equals(SystemProperties.get("persist.camera.debug.show_aec"))) {
            sb.append(addProperties("persist.camera.debug.param20"));
            sb.append(addProperties("persist.camera.debug.param21"));
            sb.append(addProperties("persist.camera.debug.param22"));
            sb.append(addProperties("persist.camera.debug.param23"));
            sb.append(addProperties("persist.camera.debug.param24"));
            sb.append(addProperties("persist.camera.debug.param25"));
            sb.append(addProperties("persist.camera.debug.param26"));
            sb.append(addProperties("persist.camera.debug.param27"));
            sb.append(addProperties("persist.camera.debug.param28"));
            sb.append(addProperties("persist.camera.debug.param29"));
        }
        sb.append(addProperties("persist.camera.debug.checkerf"));
        sb.append(addProperties("persist.camera.debug.fc"));
        if ("1".equals(SystemProperties.get("persist.camera.debug.hht"))) {
            sb.append(addProperties("camera.debug.hht.luma"));
        }
        if ("1".equals(SystemProperties.get("persist.camera.debug.autoscene"))) {
            sb.append(addProperties("camera.debug.hht.iso"));
        }
        return sb.toString();
    }

    private static String addProperties(String properties) {
        String content = "";
        if (SystemProperties.get(properties) == null) {
            return content;
        }
        return ("\t " + SystemProperties.get(properties)) + "\n";
    }

    public static int getIntField(String className, Object obj, String fieldName, String signature) {
        try {
            return ReflectUtil.getFieldInt(Class.forName(className), obj, fieldName, Integer.MIN_VALUE);
        } catch (ClassNotFoundException e) {
            Log.w("CameraUtil", "failed to get int field " + className + ", obj " + obj + "field " + fieldName);
            return Integer.MIN_VALUE;
        }
    }

    public static double getScreenInches(Context context) {
        DisplayMetrics metrics = new DisplayMetrics();
        ((WindowManager) context.getSystemService("window")).getDefaultDisplay().getMetrics(metrics);
        double inches = Math.sqrt(Math.pow((double) (((float) sWindowWidth) / metrics.xdpi), 2.0d) + Math.pow((double) (((float) sWindowHeight) / metrics.ydpi), 2.0d));
        Log.d("CameraUtil", "getScreenInches = " + inches);
        return inches;
    }

    public static boolean isContaints(Rect parent, RectF child) {
        boolean z = false;
        if (parent == null || child == null) {
            return false;
        }
        if (parent.left < parent.right && parent.top < parent.bottom && ((float) parent.left) <= child.left && ((float) parent.top) <= child.top && ((float) parent.right) >= child.right && ((float) parent.bottom) >= child.bottom) {
            z = true;
        }
        return z;
    }

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int height = resources.getDimensionPixelSize(resources.getIdentifier("navigation_bar_height", "dimen", "android"));
        Log.v("CameraUtil", "Navi height:" + height);
        return height;
    }

    public static boolean checkDeviceHasNavigationBar(Context activity) {
        boolean hasMenuKey = KeyCharacterMap.deviceHasKey(82);
        boolean hasBackKey = KeyCharacterMap.deviceHasKey(4);
        if (hasMenuKey || hasBackKey) {
            return false;
        }
        return true;
    }

    public static boolean isInVideoCall(Context context) {
        if (Device.isMTKPlatform() && 23 <= VERSION.SDK_INT) {
            try {
                return ((Boolean) Class.forName("android.telecom.TelecomManager").getMethod("isInVideoCall", new Class[0]).invoke(context.getSystemService("telecom"), new Object[0])).booleanValue();
            } catch (Exception e) {
                Log.e("CameraUtil", "check isInVideoCall Exception", e);
            }
        }
        return false;
    }

    public static boolean isFingerPrintKeyEvent(KeyEvent event) {
        if (event == null || 27 != event.getKeyCode() || event.getDevice() == null) {
            return false;
        }
        return Device.getFpNavEventNameList().contains(event.getDevice().getName());
    }

    public static boolean isMemoryRich(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService("activity");
        MemoryInfo mi = new MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.availMem > 419430400;
    }

    public static <T> int binarySearchRightMost(List<? extends Comparable<? super T>> list, T key) {
        int low = 0;
        int high = list.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            if (((Comparable) list.get(mid)).compareTo(key) >= 0) {
                high = mid - 1;
            } else {
                low = mid + 1;
            }
        }
        return low;
    }

    public static boolean isForceCamera0() {
        return new File(Storage.generatePrimaryFilepath("force_camera_0")).exists();
    }

    public static String getLocalizedNumberString(String text) {
        if (TextUtils.isEmpty(text)) {
            return text;
        }
        try {
            return String.format("%d", new Object[]{Integer.valueOf(text)});
        } catch (Exception e) {
            return text;
        }
    }

    public static void setNumberText(TextView textView, String number) {
        if (TextUtils.isDigitsOnly(number)) {
            textView.setText(getLocalizedNumberString(number));
        } else {
            textView.setText(number);
        }
    }

    public static final boolean isAppLocked(Context context, String packageName) {
        return false;
    }
}
