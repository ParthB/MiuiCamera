package com.android.camera.storage;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.location.Location;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.StatFs;
import android.os.storage.StorageManager;
import android.provider.MediaStore.Files;
import android.provider.MediaStore.Images.Media;
import android.util.Log;
import com.android.camera.CameraAppImpl;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.ExifHelper;
import com.android.camera.Util;
import com.android.camera.effect.EffectController;
import com.android.gallery3d.exif.ExifInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import miui.reflect.Field;
import miui.reflect.Method;

public class Storage {
    public static int BUCKET_ID = DIRECTORY.toLowerCase().hashCode();
    public static String DIRECTORY = (FIRST_CONSIDER_STORAGE_PATH + "/DCIM/Camera");
    public static String FIRST_CONSIDER_STORAGE_PATH = (Device.IS_HM ? SECONDARY_STORAGE_PATH : PRIMARY_STORAGE_PATH);
    public static String HIDEDIRECTORY = (FIRST_CONSIDER_STORAGE_PATH + "/DCIM/Camera/.ubifocus");
    private static final AtomicLong LEFT_SPACE = new AtomicLong(0);
    public static int PRIMARY_BUCKET_ID = (PRIMARY_STORAGE_PATH + "/DCIM/Camera").toLowerCase().hashCode();
    private static final String PRIMARY_STORAGE_PATH = Environment.getExternalStorageDirectory().toString();
    public static int SECONDARY_BUCKET_ID = (SECONDARY_STORAGE_PATH + "/DCIM/Camera").toLowerCase().hashCode();
    private static String SECONDARY_STORAGE_PATH = System.getenv("SECONDARY_STORAGE");
    private static String sCurrentStoragePath = FIRST_CONSIDER_STORAGE_PATH;
    private static WeakReference<StorageListener> sStorageListener;

    public interface StorageListener {
        void onStoragePathChanged();
    }

    static {
        File unUsedFile = new File(DIRECTORY + File.separator + ".nomedia");
        if (unUsedFile.exists()) {
            unUsedFile.delete();
        }
    }

    public static void initStorage(Context context) {
        if (Device.isSupportedSecondaryStorage()) {
            if (VERSION.SDK_INT >= 23) {
                StorageManager storageManager = (StorageManager) context.getSystemService("storage");
                try {
                    Object vols = Method.of(storageManager.getClass(), "getVolumes", "()Ljava/util/List;").invokeObject(storageManager.getClass(), storageManager, new Object[0]);
                    Class<?> volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
                    Object sdcardVolume = null;
                    if (vols != null && (vols instanceof List)) {
                        Method getTypeMethod = Method.of(volumeInfoClazz, "getType", "()I");
                        Method writableMethod = Method.of(volumeInfoClazz, "isMountedWritable", "()Z");
                        Method getDiskMethod = Method.of(volumeInfoClazz, "getDisk", "()Landroid/os/storage/DiskInfo;");
                        int typePublic = Field.of(volumeInfoClazz, "TYPE_PUBLIC", "I").getInt(null);
                        Class<?> diskInfoClazz = Class.forName("android.os.storage.DiskInfo");
                        Method isSdMethod = Method.of(diskInfoClazz, "isSd", "()Z");
                        for (Object vol : (List) vols) {
                            if (getTypeMethod.invokeInt(volumeInfoClazz, vol, new Object[0]) == typePublic) {
                                if (writableMethod.invokeBoolean(volumeInfoClazz, vol, new Object[0])) {
                                    Object disk = getDiskMethod.invokeObject(volumeInfoClazz, vol, new Object[0]);
                                    if (disk != null && isSdMethod.invokeBoolean(diskInfoClazz, disk, new Object[0])) {
                                        sdcardVolume = vol;
                                        break;
                                    }
                                }
                                continue;
                            }
                        }
                    }
                    if (sdcardVolume != null) {
                        File file = (File) Method.of(volumeInfoClazz, "getPath", "()Ljava/io/File;").invokeObject(volumeInfoClazz, sdcardVolume, new Object[0]);
                        String sdcardPath = file == null ? null : file.getPath();
                        if (sdcardPath != null) {
                            Log.v("CameraStorage", "initStorage sd=" + sdcardPath);
                            SECONDARY_STORAGE_PATH = sdcardPath;
                            SECONDARY_BUCKET_ID = (SECONDARY_STORAGE_PATH + "/DCIM/Camera").toLowerCase().hashCode();
                        }
                    }
                } catch (Exception e) {
                    Log.e("CameraStorage", "initStorage Exception ", e);
                    e.printStackTrace();
                }
            }
            readSystemPriorityStorage();
        }
    }

    public static Uri addImage(Activity activity, String title, long date, Location location, int orientation, byte[] jpeg, int width, int height, boolean mirror) {
        return addImage(activity, title, date, location, orientation, jpeg, width, height, mirror, false, false, false);
    }

    public static Uri addImage(Context context, String title, long date, Location location, int orientation, byte[] jpeg, int width, int height, boolean mirror, boolean isHide, boolean isMap) {
        return addImage(context, title, date, location, orientation, jpeg, width, height, mirror, isHide, isMap, false);
    }

    public static Uri addImage(Context context, String title, long date, Location location, int orientation, byte[] jpeg, int width, int height, boolean mirror, boolean isHide, boolean isMap, boolean appendExif) {
        Exception e;
        boolean focusSuccess;
        int centerFocused;
        ContentValues values;
        Uri uri;
        Throwable th;
        String path = generateFilepath(title, isHide, isMap);
        FileOutputStream fileOutputStream = null;
        boolean isException = false;
        try {
            FileOutputStream out = new FileOutputStream(path);
            if (mirror) {
                try {
                    Bitmap b = flipJpeg(jpeg);
                    if (b != null) {
                        b.compress(CompressFormat.JPEG, 100, out);
                        b.recycle();
                        appendExif = true;
                    } else {
                        out.write(jpeg);
                    }
                } catch (Exception e2) {
                    e = e2;
                    fileOutputStream = out;
                    try {
                        Log.e("CameraStorage", "Failed to write image", e);
                        isException = true;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } catch (Exception e3) {
                                Log.e("CameraStorage", "Failed to flush/close stream", e3);
                                isException = true;
                            }
                        }
                        if (!isException) {
                            return null;
                        }
                        if (isMap) {
                            focusSuccess = Util.isProduceFocusInfoSuccess(jpeg);
                            centerFocused = Util.getCenterFocusDepthIndex(jpeg, width, height);
                            title = title.substring(0, focusSuccess ? title.lastIndexOf("_UBIFOCUS_") : title.lastIndexOf("_"));
                            path = generateFilepath(title, false, false);
                            new File(generateFilepath(title + (focusSuccess ? "_UBIFOCUS_" : "_") + centerFocused, isHide, false)).renameTo(new File(path));
                            if (!focusSuccess) {
                                deleteImage(title);
                            }
                        }
                        if (!isHide) {
                        }
                        values = new ContentValues(11);
                        values.put("title", title);
                        values.put("_display_name", title + ".jpg");
                        values.put("datetaken", Long.valueOf(date));
                        values.put("mime_type", "image/jpeg");
                        values.put("orientation", Integer.valueOf(orientation));
                        values.put("_data", path);
                        values.put("_size", Long.valueOf(new File(path).length()));
                        values.put("width", Integer.valueOf(width));
                        values.put("height", Integer.valueOf(height));
                        if (location != null) {
                            values.put("latitude", Double.valueOf(location.getLatitude()));
                            values.put("longitude", Double.valueOf(location.getLongitude()));
                        }
                        uri = null;
                        try {
                            uri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
                        } catch (Exception th2) {
                            th2.printStackTrace();
                            Log.e("CameraStorage", "Failed to write MediaStore" + th2);
                        }
                        if (!EffectController.getInstance().hasEffect()) {
                            saveToCloudAlbum(context, path);
                        }
                        return uri;
                    } catch (Throwable th3) {
                        th = th3;
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            } catch (Exception e32) {
                                Log.e("CameraStorage", "Failed to flush/close stream", e32);
                            }
                        }
                        throw th;
                    }
                } catch (Throwable th4) {
                    th = th4;
                    fileOutputStream = out;
                    if (fileOutputStream != null) {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }
                    throw th;
                }
            }
            out.write(jpeg);
            if (appendExif) {
                out.flush();
                ExifHelper.writeExif(path, orientation, location, System.currentTimeMillis());
            }
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (Exception e322) {
                    Log.e("CameraStorage", "Failed to flush/close stream", e322);
                    isException = true;
                }
            }
            fileOutputStream = out;
        } catch (Exception e4) {
            e322 = e4;
            Log.e("CameraStorage", "Failed to write image", e322);
            isException = true;
            if (fileOutputStream != null) {
                fileOutputStream.flush();
                fileOutputStream.close();
            }
            if (!isException) {
                return null;
            }
            if (isMap) {
                focusSuccess = Util.isProduceFocusInfoSuccess(jpeg);
                centerFocused = Util.getCenterFocusDepthIndex(jpeg, width, height);
                if (focusSuccess) {
                }
                title = title.substring(0, focusSuccess ? title.lastIndexOf("_UBIFOCUS_") : title.lastIndexOf("_"));
                path = generateFilepath(title, false, false);
                if (focusSuccess) {
                }
                new File(generateFilepath(title + (focusSuccess ? "_UBIFOCUS_" : "_") + centerFocused, isHide, false)).renameTo(new File(path));
                if (focusSuccess) {
                    deleteImage(title);
                }
            }
            if (!isHide) {
            }
            values = new ContentValues(11);
            values.put("title", title);
            values.put("_display_name", title + ".jpg");
            values.put("datetaken", Long.valueOf(date));
            values.put("mime_type", "image/jpeg");
            values.put("orientation", Integer.valueOf(orientation));
            values.put("_data", path);
            values.put("_size", Long.valueOf(new File(path).length()));
            values.put("width", Integer.valueOf(width));
            values.put("height", Integer.valueOf(height));
            if (location != null) {
                values.put("latitude", Double.valueOf(location.getLatitude()));
                values.put("longitude", Double.valueOf(location.getLongitude()));
            }
            uri = null;
            uri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
            if (EffectController.getInstance().hasEffect()) {
                saveToCloudAlbum(context, path);
            }
            return uri;
        }
        if (!isException) {
            return null;
        }
        if (isMap) {
            focusSuccess = Util.isProduceFocusInfoSuccess(jpeg);
            centerFocused = Util.getCenterFocusDepthIndex(jpeg, width, height);
            if (focusSuccess) {
            }
            title = title.substring(0, focusSuccess ? title.lastIndexOf("_UBIFOCUS_") : title.lastIndexOf("_"));
            path = generateFilepath(title, false, false);
            if (focusSuccess) {
            }
            new File(generateFilepath(title + (focusSuccess ? "_UBIFOCUS_" : "_") + centerFocused, isHide, false)).renameTo(new File(path));
            if (focusSuccess) {
                deleteImage(title);
            }
        }
        if (!isHide && !isMap) {
            return null;
        }
        values = new ContentValues(11);
        values.put("title", title);
        values.put("_display_name", title + ".jpg");
        values.put("datetaken", Long.valueOf(date));
        values.put("mime_type", "image/jpeg");
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", path);
        values.put("_size", Long.valueOf(new File(path).length()));
        values.put("width", Integer.valueOf(width));
        values.put("height", Integer.valueOf(height));
        if (location != null) {
            values.put("latitude", Double.valueOf(location.getLatitude()));
            values.put("longitude", Double.valueOf(location.getLongitude()));
        }
        uri = null;
        uri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
        if (EffectController.getInstance().hasEffect()) {
            saveToCloudAlbum(context, path);
        }
        return uri;
    }

    public static Uri addImage(Context context, String filePath, int rotation, long date, Location location, int width, int height) {
        if (context == null || filePath == null) {
            return null;
        }
        File file = null;
        try {
            file = new File(filePath);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("CameraStorage", "Failed to open panorama file." + e);
        }
        if (file == null || !file.exists()) {
            return null;
        }
        String fileName = file.getName();
        ContentValues contentValues = new ContentValues(11);
        contentValues.put("title", fileName);
        contentValues.put("_display_name", fileName);
        contentValues.put("datetaken", Long.valueOf(date));
        contentValues.put("mime_type", "image/jpeg");
        contentValues.put("orientation", Integer.valueOf(rotation));
        contentValues.put("_data", filePath);
        contentValues.put("_size", Long.valueOf(file.length()));
        contentValues.put("width", Integer.valueOf(width));
        contentValues.put("height", Integer.valueOf(height));
        if (location != null) {
            contentValues.put("latitude", Double.valueOf(location.getLatitude()));
            contentValues.put("longitude", Double.valueOf(location.getLongitude()));
        }
        Uri uri = null;
        try {
            uri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, contentValues);
        } catch (Exception th) {
            th.printStackTrace();
            Log.e("CameraStorage", "Failed to write MediaStore" + th);
        }
        saveToCloudAlbum(context, filePath);
        return uri;
    }

    public static void deleteImage(String title) {
        File hideFolder = new File(HIDEDIRECTORY);
        if (hideFolder.exists() && hideFolder.isDirectory()) {
            for (File file : hideFolder.listFiles()) {
                if (file.getName().indexOf(title) != -1) {
                    file.delete();
                }
            }
        }
    }

    public static Uri newImage(Context context, String title, long date, int orientation, int width, int height) {
        String path = generateFilepath(title);
        ContentValues values = new ContentValues(6);
        values.put("datetaken", Long.valueOf(date));
        values.put("orientation", Integer.valueOf(orientation));
        values.put("_data", path);
        values.put("width", Integer.valueOf(width));
        values.put("height", Integer.valueOf(height));
        values.put("mime_type", "image/jpeg");
        Uri uri = null;
        try {
            uri = context.getContentResolver().insert(Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception th) {
            Log.e("CameraStorage", "Failed to new image" + th);
        }
        return uri;
    }

    public static boolean updateImage(Context context, byte[] jpeg, ExifInterface exif, Uri uri, String title, Location location, int orientation, int width, int height, String oldTitle) {
        String generateFilepath;
        Exception e;
        File tmpFile;
        long fileLength;
        Throwable th;
        String path = generateFilepath(title);
        StringBuilder stringBuilder = new StringBuilder();
        if (oldTitle != null) {
            generateFilepath = generateFilepath(oldTitle);
        } else {
            generateFilepath = path;
        }
        String tmpPath = stringBuilder.append(generateFilepath).append(".tmp").toString();
        FileOutputStream fileOutputStream = null;
        if (jpeg != null) {
            try {
                FileOutputStream out = new FileOutputStream(tmpPath);
                if (exif != null) {
                    try {
                        exif.writeExif(jpeg, out);
                        fileOutputStream = out;
                    } catch (IOException e2) {
                        try {
                            Log.e("CameraStorage", "Failed to rewrite Exif");
                            out.write(jpeg);
                            fileOutputStream = out;
                        } catch (Exception e3) {
                            e = e3;
                            fileOutputStream = out;
                            try {
                                Log.e("CameraStorage", "Failed to write image", e);
                                if (fileOutputStream != null) {
                                    try {
                                        fileOutputStream.flush();
                                        fileOutputStream.close();
                                    } catch (Exception e4) {
                                        Log.e("CameraStorage", "Failed to flush/close stream", e4);
                                    }
                                }
                                tmpFile = new File(tmpPath);
                                fileLength = tmpFile.length();
                                tmpFile.renameTo(new File(path));
                                try {
                                    new File(generateFilepath(oldTitle)).delete();
                                } catch (Exception e42) {
                                    Log.e("CameraStorage", "Exception when delete oldfile " + oldTitle, e42);
                                }
                                return false;
                            } catch (Throwable th2) {
                                th = th2;
                                if (fileOutputStream != null) {
                                    try {
                                        fileOutputStream.flush();
                                        fileOutputStream.close();
                                    } catch (Exception e422) {
                                        Log.e("CameraStorage", "Failed to flush/close stream", e422);
                                    }
                                }
                                tmpFile = new File(tmpPath);
                                fileLength = tmpFile.length();
                                tmpFile.renameTo(new File(path));
                                if (!(exif == null || oldTitle == null)) {
                                    try {
                                        new File(generateFilepath(oldTitle)).delete();
                                    } catch (Exception e4222) {
                                        Log.e("CameraStorage", "Exception when delete oldfile " + oldTitle, e4222);
                                    }
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            fileOutputStream = out;
                            if (fileOutputStream != null) {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                            tmpFile = new File(tmpPath);
                            fileLength = tmpFile.length();
                            tmpFile.renameTo(new File(path));
                            new File(generateFilepath(oldTitle)).delete();
                            throw th;
                        }
                    }
                }
                out.write(jpeg);
                fileOutputStream = out;
            } catch (Exception e5) {
                e4222 = e5;
                Log.e("CameraStorage", "Failed to write image", e4222);
                if (fileOutputStream != null) {
                    fileOutputStream.flush();
                    fileOutputStream.close();
                }
                tmpFile = new File(tmpPath);
                fileLength = tmpFile.length();
                tmpFile.renameTo(new File(path));
                if (!(exif == null || oldTitle == null)) {
                    new File(generateFilepath(oldTitle)).delete();
                }
                return false;
            }
        } else if (oldTitle != null) {
            tmpPath = generateFilepath(oldTitle);
        }
        if (fileOutputStream != null) {
            try {
                fileOutputStream.flush();
                fileOutputStream.close();
            } catch (Exception e42222) {
                Log.e("CameraStorage", "Failed to flush/close stream", e42222);
            }
        }
        tmpFile = new File(tmpPath);
        fileLength = tmpFile.length();
        tmpFile.renameTo(new File(path));
        if (!(exif == null || oldTitle == null)) {
            try {
                new File(generateFilepath(oldTitle)).delete();
            } catch (Exception e422222) {
                Log.e("CameraStorage", "Exception when delete oldfile " + oldTitle, e422222);
            }
        }
        ContentValues values = new ContentValues(9);
        values.put("title", title);
        values.put("_display_name", title + ".jpg");
        if (jpeg != null) {
            values.put("mime_type", "image/jpeg");
            values.put("orientation", Integer.valueOf(orientation));
            values.put("_size", Long.valueOf(fileLength));
            values.put("width", Integer.valueOf(width));
            values.put("height", Integer.valueOf(height));
            if (location != null) {
                values.put("latitude", Double.valueOf(location.getLatitude()));
                values.put("longitude", Double.valueOf(location.getLongitude()));
            }
            values.put("_data", path);
        } else if (oldTitle != null) {
            values.put("_data", path);
        }
        try {
            context.getContentResolver().update(uri, values, null, null);
            if (oldTitle != null) {
                deleteFromCloudAlbum(context, generateFilepath(oldTitle));
            }
            saveToCloudAlbum(context, path);
            return true;
        } catch (Exception th4) {
            Log.e("CameraStorage", "Failed to update image" + th4);
            return false;
        }
    }

    public static void addDNGToDataBase(Activity activity, String title) {
        String path = generateFilepath(title, ".dng");
        ContentValues values = new ContentValues(4);
        values.put("title", title);
        values.put("_display_name", title + ".dng");
        values.put("media_type", Integer.valueOf(1));
        values.put("_data", path);
        try {
            activity.getContentResolver().insert(Files.getContentUri("external"), values);
        } catch (Exception th) {
            Log.e("CameraStorage", "Failed to write MediaStore" + th);
        }
    }

    public static void saveToCloudAlbum(Context context, String filePath) {
        context.sendBroadcast(getSaveToCloudIntent(context, filePath));
    }

    private static Intent getSaveToCloudIntent(Context context, String filePath) {
        Intent intent = new Intent("com.miui.gallery.SAVE_TO_CLOUD");
        intent.setPackage("com.miui.gallery");
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            intent.setComponent(new ComponentName("com.miui.gallery", ((ResolveInfo) resolveInfos.get(0)).activityInfo.name));
        }
        intent.putExtra("extra_file_path", filePath);
        return intent;
    }

    public static void deleteFromCloudAlbum(Context context, String filePath) {
        context.sendBroadcast(getDeleteFromCloudIntent(context, filePath));
    }

    private static Intent getDeleteFromCloudIntent(Context context, String filePath) {
        Intent intent = new Intent("com.miui.gallery.DELETE_FROM_CLOUD");
        intent.setPackage("com.miui.gallery");
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryBroadcastReceivers(intent, 0);
        if (resolveInfos != null && resolveInfos.size() > 0) {
            intent.setComponent(new ComponentName("com.miui.gallery", ((ResolveInfo) resolveInfos.get(0)).activityInfo.name));
        }
        intent.putExtra("extra_file_path", filePath);
        return intent;
    }

    public static Bitmap flipJpeg(byte[] jpeg) {
        if (jpeg == null) {
            return null;
        }
        Options options = new Options();
        options.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
        Matrix m = new Matrix();
        m.setScale(-1.0f, 1.0f, ((float) bitmap.getWidth()) * 0.5f, ((float) bitmap.getHeight()) * 0.5f);
        try {
            Bitmap flip = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if (flip != bitmap) {
                bitmap.recycle();
            }
            if (flip.getWidth() == -1 || flip.getHeight() == -1) {
                return null;
            }
            return flip;
        } catch (Exception t) {
            Log.w("CameraStorage", "Failed to rotate thumbnail", t);
            return null;
        }
    }

    public static String generatePrimaryFilepath(String title) {
        return PRIMARY_STORAGE_PATH + "/DCIM/Camera" + '/' + title;
    }

    public static String generateFilepath(String title) {
        return generateFilepath(title, ".jpg");
    }

    public static String generateFilepath(String title, boolean isHide, boolean isMap) {
        if (isHide && isLowStorageSpace(HIDEDIRECTORY)) {
            return null;
        }
        return (isHide ? HIDEDIRECTORY : DIRECTORY) + '/' + title + (isMap ? ".y" : ".jpg");
    }

    public static String generateFilepath(String title, String ext) {
        return DIRECTORY + '/' + title + ext;
    }

    public static long getAvailableSpace(String path) {
        if (path == null) {
            return -1;
        }
        File dir = new File(path);
        boolean needScan = Util.mkdirs(dir, 511, -1, -1);
        if (!dir.exists() || !dir.isDirectory() || !dir.canWrite()) {
            return -1;
        }
        if (needScan && path.endsWith("/DCIM/Camera")) {
            Intent scanIntent = new Intent("miui.intent.action.MEDIA_SCANNER_SCAN_FOLDER");
            scanIntent.setData(Uri.fromFile(dir.getParentFile()));
            CameraAppImpl.getAndroidContext().sendBroadcast(scanIntent);
        }
        try {
            if (HIDEDIRECTORY.equals(path)) {
                Util.createFile(new File(HIDEDIRECTORY + File.separator + ".nomedia"));
            }
            StatFs stat = new StatFs(path);
            long available = ((long) stat.getAvailableBlocks()) * ((long) stat.getBlockSize());
            setLeftSpace(available);
            return available;
        } catch (Exception e) {
            Log.i("CameraStorage", "Fail to access external storage", e);
            return -3;
        }
    }

    public static long getAvailableSpace() {
        return getAvailableSpace(DIRECTORY);
    }

    public static boolean isLowStorageSpace(String path) {
        return getAvailableSpace(path) < 52428800;
    }

    public static boolean hasSecondaryStorage() {
        return Device.isSupportedSecondaryStorage() && SECONDARY_STORAGE_PATH != null;
    }

    public static boolean secondaryStorageMounted() {
        return hasSecondaryStorage() && getAvailableSpace(SECONDARY_STORAGE_PATH) > 0;
    }

    public static boolean isCurrentStorageIsSecondary() {
        return SECONDARY_STORAGE_PATH != null ? SECONDARY_STORAGE_PATH.equals(sCurrentStoragePath) : false;
    }

    public static void switchStoragePathIfNeeded() {
        if (hasSecondaryStorage()) {
            String firstConsiderPath = FIRST_CONSIDER_STORAGE_PATH;
            String secondConsiderPath = SECONDARY_STORAGE_PATH;
            if (FIRST_CONSIDER_STORAGE_PATH.equals(SECONDARY_STORAGE_PATH)) {
                secondConsiderPath = PRIMARY_STORAGE_PATH;
            }
            String oldPath = sCurrentStoragePath;
            if (!isLowStorageSpace(firstConsiderPath)) {
                sCurrentStoragePath = firstConsiderPath;
            } else if (!isLowStorageSpace(secondConsiderPath)) {
                sCurrentStoragePath = secondConsiderPath;
            } else {
                return;
            }
            if (!sCurrentStoragePath.equals(oldPath)) {
                updateDirectory();
                if (!(sStorageListener == null || sStorageListener.get() == null)) {
                    ((StorageListener) sStorageListener.get()).onStoragePathChanged();
                }
            }
            Log.i("CameraStorage", "Storage path is switched path = " + DIRECTORY);
        }
    }

    public static void switchToPhoneStorage() {
        FIRST_CONSIDER_STORAGE_PATH = PRIMARY_STORAGE_PATH;
        if (!PRIMARY_STORAGE_PATH.equals(sCurrentStoragePath)) {
            Log.v("CameraStorage", "switchToPhoneStorage");
            sCurrentStoragePath = PRIMARY_STORAGE_PATH;
            updateDirectory();
            if (sStorageListener != null && sStorageListener.get() != null) {
                ((StorageListener) sStorageListener.get()).onStoragePathChanged();
            }
        }
    }

    public static void readSystemPriorityStorage() {
        boolean isPriorityStorage = false;
        if (hasSecondaryStorage()) {
            isPriorityStorage = PriorityStorageBroadcastReceiver.isPriorityStorage();
            CameraSettings.setPriorityStoragePreference(isPriorityStorage);
        }
        FIRST_CONSIDER_STORAGE_PATH = isPriorityStorage ? SECONDARY_STORAGE_PATH : PRIMARY_STORAGE_PATH;
        sCurrentStoragePath = FIRST_CONSIDER_STORAGE_PATH;
        updateDirectory();
    }

    public static boolean isRelatedStorage(Uri uri) {
        boolean z = false;
        if (uri == null) {
            return false;
        }
        String path = uri.getPath();
        if (path != null) {
            if (path.equals(PRIMARY_STORAGE_PATH)) {
                z = true;
            } else {
                z = path.equals(SECONDARY_STORAGE_PATH);
            }
        }
        return z;
    }

    public static boolean isUsePhoneStorage() {
        return PRIMARY_STORAGE_PATH.equals(sCurrentStoragePath);
    }

    public static boolean isPhoneStoragePriority() {
        return PRIMARY_STORAGE_PATH.equals(FIRST_CONSIDER_STORAGE_PATH);
    }

    public static void setStorageListener(StorageListener listener) {
        if (listener != null) {
            sStorageListener = new WeakReference(listener);
        }
    }

    public static boolean isLowStorageAtLastPoint() {
        return getLeftSpace() < 52428800;
    }

    public static long getLeftSpace() {
        long left = LEFT_SPACE.get();
        Log.i("CameraStorage", "getLeftSpace() return " + left);
        return left;
    }

    private static void setLeftSpace(long left) {
        LEFT_SPACE.set(left);
        Log.i("CameraStorage", "setLeftSpace(" + left + ")");
    }

    private static void updateDirectory() {
        DIRECTORY = sCurrentStoragePath + "/DCIM/Camera";
        HIDEDIRECTORY = sCurrentStoragePath + "/DCIM/Camera/.ubifocus";
        BUCKET_ID = DIRECTORY.toLowerCase().hashCode();
    }
}
