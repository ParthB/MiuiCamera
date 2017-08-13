package com.android.camera;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.Log;
import com.android.camera.storage.Storage;
import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class Thumbnail {
    private static Object sLock = new Object();
    private Bitmap mBitmap;
    private boolean mFromFile = false;
    private boolean mUpdateAnimation = true;
    private Uri mUri;

    private static class Media {
        public final long dateTaken;
        public final long id;
        public final int orientation;
        public final String path;
        public final Uri uri;

        public Media(long id, int orientation, long dateTaken, Uri uri, String path) {
            this.id = id;
            this.orientation = orientation;
            this.dateTaken = dateTaken;
            this.uri = uri;
            this.path = path;
        }
    }

    private Thumbnail(Uri uri, Bitmap bitmap, int orientation, boolean mirror) {
        this.mUri = uri;
        this.mBitmap = rotateImage(bitmap, orientation, mirror);
    }

    public Uri getUri() {
        return this.mUri;
    }

    public Bitmap getBitmap() {
        return this.mBitmap;
    }

    public void setFromFile(boolean fromFile) {
        this.mFromFile = fromFile;
    }

    public boolean fromFile() {
        return this.mFromFile;
    }

    public boolean needUpdateAnimation() {
        return this.mUpdateAnimation;
    }

    public void setUpdateAnimation(boolean updateAnimation) {
        this.mUpdateAnimation = updateAnimation;
    }

    private static Bitmap rotateImage(Bitmap bitmap, int orientation, boolean mirror) {
        Bitmap output;
        if (orientation != 0 || mirror) {
            Matrix m1 = new Matrix();
            Matrix m2 = new Matrix();
            if (orientation != 0) {
                m1.setRotate((float) orientation, ((float) bitmap.getWidth()) * 0.5f, ((float) bitmap.getHeight()) * 0.5f);
            }
            if (mirror) {
                m2.setScale(-1.0f, 1.0f, ((float) bitmap.getWidth()) * 0.5f, ((float) bitmap.getHeight()) * 0.5f);
                m1.postConcat(m2);
            }
            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m1, true);
                output = getCircleBitmap(rotated);
                if (rotated != bitmap) {
                    rotated.recycle();
                }
                bitmap.recycle();
                return output;
            } catch (Exception t) {
                Log.w("Thumbnail", "Failed to rotate thumbnail", t);
            }
        }
        output = getCircleBitmap(bitmap);
        bitmap.recycle();
        return output;
    }

    public static Bitmap getCircleBitmap(Bitmap bitmap) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int d = Math.min(w, h);
        Paint paint = new Paint();
        Rect rectSrc = new Rect((w - d) >> 1, (h - d) >> 1, (w + d) >> 1, (h + d) >> 1);
        int r = d >> 1;
        Rect rectDst = new Rect(0, 0, d, d);
        Bitmap output = Bitmap.createBitmap(d, d, Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setColor(-1);
        paint.setStyle(Style.FILL);
        canvas.drawCircle((float) r, (float) r, (float) r, paint);
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rectSrc, rectDst, paint);
        return output;
    }

    public void saveLastThumbnailToFile(File filesDir) {
        DataOutputStream d;
        IOException e;
        Object d2;
        Object obj;
        Object obj2;
        Throwable th;
        File file = new File(filesDir, "last_thumb");
        Closeable closeable = null;
        Closeable closeable2 = null;
        Closeable closeable3 = null;
        synchronized (sLock) {
            try {
                FileOutputStream f = new FileOutputStream(file);
                try {
                    BufferedOutputStream b = new BufferedOutputStream(f, 4096);
                    try {
                        d = new DataOutputStream(b);
                        try {
                            d.writeUTF(this.mUri.toString());
                            this.mBitmap.compress(CompressFormat.JPEG, 90, d);
                            d.close();
                        } catch (IOException e2) {
                            e = e2;
                            d2 = d;
                            obj = b;
                            obj2 = f;
                            try {
                                Log.e("Thumbnail", "Fail to store bitmap. path=" + file.getPath(), e);
                            } catch (Throwable th2) {
                                th = th2;
                                Util.closeSilently(closeable);
                                Util.closeSilently(closeable2);
                                Util.closeSilently(closeable3);
                                throw th;
                            }
                            try {
                                Util.closeSilently(closeable);
                                Util.closeSilently(closeable2);
                                Util.closeSilently(closeable3);
                            } catch (Throwable th3) {
                                th = th3;
                                throw th;
                            }
                        } catch (Throwable th4) {
                            th = th4;
                            d2 = d;
                            obj = b;
                            obj2 = f;
                            Util.closeSilently(closeable);
                            Util.closeSilently(closeable2);
                            Util.closeSilently(closeable3);
                            throw th;
                        }
                    } catch (IOException e3) {
                        e = e3;
                        obj = b;
                        obj2 = f;
                        Log.e("Thumbnail", "Fail to store bitmap. path=" + file.getPath(), e);
                        Util.closeSilently(closeable);
                        Util.closeSilently(closeable2);
                        Util.closeSilently(closeable3);
                    } catch (Throwable th5) {
                        th = th5;
                        obj = b;
                        obj2 = f;
                        Util.closeSilently(closeable);
                        Util.closeSilently(closeable2);
                        Util.closeSilently(closeable3);
                        throw th;
                    }
                    try {
                        Util.closeSilently(f);
                        Util.closeSilently(b);
                        Util.closeSilently(d);
                        FileOutputStream fileOutputStream = f;
                    } catch (Throwable th6) {
                        th = th6;
                        throw th;
                    }
                } catch (IOException e4) {
                    e = e4;
                    obj2 = f;
                    Log.e("Thumbnail", "Fail to store bitmap. path=" + file.getPath(), e);
                    Util.closeSilently(closeable);
                    Util.closeSilently(closeable2);
                    Util.closeSilently(closeable3);
                } catch (Throwable th7) {
                    th = th7;
                    obj2 = f;
                    Util.closeSilently(closeable);
                    Util.closeSilently(closeable2);
                    Util.closeSilently(closeable3);
                    throw th;
                }
            } catch (IOException e5) {
                e = e5;
                Log.e("Thumbnail", "Fail to store bitmap. path=" + file.getPath(), e);
                Util.closeSilently(closeable);
                Util.closeSilently(closeable2);
                Util.closeSilently(closeable3);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static com.android.camera.Thumbnail getLastThumbnailFromFile(java.io.File r16, android.content.ContentResolver r17) {
        /*
        r9 = new java.io.File;
        r12 = "last_thumb";
        r0 = r16;
        r9.<init>(r0, r12);
        r11 = 0;
        r3 = 0;
        r7 = 0;
        r1 = 0;
        r4 = 0;
        r13 = sLock;
        monitor-enter(r13);
        r8 = new java.io.FileInputStream;	 Catch:{ IOException -> 0x0060 }
        r8.<init>(r9);	 Catch:{ IOException -> 0x0060 }
        r2 = new java.io.BufferedInputStream;	 Catch:{ IOException -> 0x00a6, all -> 0x009a }
        r12 = 4096; // 0x1000 float:5.74E-42 double:2.0237E-320;
        r2.<init>(r8, r12);	 Catch:{ IOException -> 0x00a6, all -> 0x009a }
        r5 = new java.io.DataInputStream;	 Catch:{ IOException -> 0x00a9, all -> 0x009d }
        r5.<init>(r2);	 Catch:{ IOException -> 0x00a9, all -> 0x009d }
        r12 = r5.readUTF();	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        r11 = android.net.Uri.parse(r12);	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        r0 = r17;
        r12 = com.android.camera.Util.isUriValid(r11, r0);	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        if (r12 != 0) goto L_0x0042;
    L_0x0033:
        r5.close();	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        com.android.camera.Util.closeSilently(r8);	 Catch:{ all -> 0x0095 }
        com.android.camera.Util.closeSilently(r2);	 Catch:{ all -> 0x0095 }
        com.android.camera.Util.closeSilently(r5);	 Catch:{ all -> 0x0095 }
        r12 = 0;
        monitor-exit(r13);
        return r12;
    L_0x0042:
        r3 = android.graphics.BitmapFactory.decodeStream(r5);	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        r5.close();	 Catch:{ IOException -> 0x00ad, all -> 0x00a1 }
        com.android.camera.Util.closeSilently(r8);	 Catch:{ all -> 0x0095 }
        com.android.camera.Util.closeSilently(r2);	 Catch:{ all -> 0x0095 }
        com.android.camera.Util.closeSilently(r5);	 Catch:{ all -> 0x0095 }
        monitor-exit(r13);
        r12 = 0;
        r13 = 0;
        r10 = createThumbnail(r11, r3, r12, r13);
        if (r10 == 0) goto L_0x005f;
    L_0x005b:
        r12 = 1;
        r10.setFromFile(r12);
    L_0x005f:
        return r10;
    L_0x0060:
        r6 = move-exception;
    L_0x0061:
        r12 = "Thumbnail";
        r14 = new java.lang.StringBuilder;	 Catch:{ all -> 0x0087 }
        r14.<init>();	 Catch:{ all -> 0x0087 }
        r15 = "Fail to load bitmap. ";
        r14 = r14.append(r15);	 Catch:{ all -> 0x0087 }
        r14 = r14.append(r6);	 Catch:{ all -> 0x0087 }
        r14 = r14.toString();	 Catch:{ all -> 0x0087 }
        android.util.Log.i(r12, r14);	 Catch:{ all -> 0x0087 }
        com.android.camera.Util.closeSilently(r7);	 Catch:{ all -> 0x0092 }
        com.android.camera.Util.closeSilently(r1);	 Catch:{ all -> 0x0092 }
        com.android.camera.Util.closeSilently(r4);	 Catch:{ all -> 0x0092 }
        r12 = 0;
        monitor-exit(r13);
        return r12;
    L_0x0087:
        r12 = move-exception;
    L_0x0088:
        com.android.camera.Util.closeSilently(r7);	 Catch:{ all -> 0x0092 }
        com.android.camera.Util.closeSilently(r1);	 Catch:{ all -> 0x0092 }
        com.android.camera.Util.closeSilently(r4);	 Catch:{ all -> 0x0092 }
        throw r12;	 Catch:{ all -> 0x0092 }
    L_0x0092:
        r12 = move-exception;
    L_0x0093:
        monitor-exit(r13);
        throw r12;
    L_0x0095:
        r12 = move-exception;
        r4 = r5;
        r1 = r2;
        r7 = r8;
        goto L_0x0093;
    L_0x009a:
        r12 = move-exception;
        r7 = r8;
        goto L_0x0088;
    L_0x009d:
        r12 = move-exception;
        r1 = r2;
        r7 = r8;
        goto L_0x0088;
    L_0x00a1:
        r12 = move-exception;
        r4 = r5;
        r1 = r2;
        r7 = r8;
        goto L_0x0088;
    L_0x00a6:
        r6 = move-exception;
        r7 = r8;
        goto L_0x0061;
    L_0x00a9:
        r6 = move-exception;
        r1 = r2;
        r7 = r8;
        goto L_0x0061;
    L_0x00ad:
        r6 = move-exception;
        r4 = r5;
        r1 = r2;
        r7 = r8;
        goto L_0x0061;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.Thumbnail.getLastThumbnailFromFile(java.io.File, android.content.ContentResolver):com.android.camera.Thumbnail");
    }

    public static int getLastThumbnailFromContentResolver(ContentResolver resolver, Thumbnail[] result, Uri uri) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if (image == null && video == null) {
            return 0;
        }
        Bitmap bitmap;
        Media lastMedia;
        if (image == null || (video != null && image.dateTaken < video.dateTaken)) {
            if (uri != null && uri.equals(video.uri)) {
                return -1;
            }
            bitmap = Thumbnails.getThumbnail(resolver, video.id, 1, null);
            if (bitmap == null) {
                try {
                    bitmap = ThumbnailUtils.createVideoThumbnail(video.path, 1);
                } catch (Exception e) {
                    Log.e("Thumbnail", "exception in createVideoThumbnail", e);
                }
            }
            lastMedia = video;
        } else if (uri != null && uri.equals(image.uri)) {
            return -1;
        } else {
            bitmap = Images.Thumbnails.getThumbnail(resolver, image.id, 1, null);
            if (bitmap == null) {
                try {
                    bitmap = ThumbnailUtils.createImageThumbnail(image.path, 1);
                } catch (Exception e2) {
                    Log.e("Thumbnail", "exception in createImageThumbnail", e2);
                }
            }
            lastMedia = image;
        }
        if (bitmap == null || !Util.isUriValid(lastMedia.uri, resolver)) {
            return 2;
        }
        result[0] = createThumbnail(lastMedia.uri, bitmap, lastMedia.orientation, false);
        return 1;
    }

    private static String getImageBucketIds() {
        if (Storage.secondaryStorageMounted()) {
            return "bucket_id IN (" + Storage.PRIMARY_BUCKET_ID + "," + Storage.SECONDARY_BUCKET_ID + ")";
        }
        return "bucket_id=" + Storage.BUCKET_ID;
    }

    private static String getVideoBucketIds() {
        if (Storage.secondaryStorageMounted()) {
            return "bucket_id IN (" + Storage.PRIMARY_BUCKET_ID + "," + Storage.SECONDARY_BUCKET_ID + ")";
        }
        return "bucket_id=" + Storage.BUCKET_ID;
    }

    public static Uri getLastThumbnailUri(ContentResolver resolver) {
        Media image = getLastImageThumbnail(resolver);
        Media video = getLastVideoThumbnail(resolver);
        if (image != null && (video == null || image.dateTaken >= video.dateTaken)) {
            return image.uri;
        }
        if (video == null || (image != null && video.dateTaken < image.dateTaken)) {
            return null;
        }
        return video.uri;
    }

    private static Media getLastImageThumbnail(ContentResolver resolver) {
        Uri baseUri = android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        Media media = "1";
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", media).build();
        String[] projection = new String[]{"_id", "orientation", "datetaken", "_data"};
        String selection = "mime_type='image/jpeg' AND " + getImageBucketIds() + " AND " + "_size" + " > 0";
        String order = "datetaken DESC,_id DESC";
        Cursor cursor = null;
        Cursor cursor2 = null;
        boolean firstMiss = false;
        try {
            long id;
            cursor = resolver.query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                if (cursor.getString(3) == null || !new File(cursor.getString(3)).exists()) {
                    firstMiss = true;
                } else {
                    id = cursor.getLong(0);
                    media = new Media(id, cursor.getInt(1), cursor.getLong(2), ContentUris.withAppendedId(baseUri, id), cursor.getString(3));
                    if (cursor != null) {
                        cursor.close();
                    }
                    return media;
                }
            }
            if (firstMiss) {
                cursor2 = resolver.query(baseUri, projection, selection, null, order);
                if (cursor2 != null) {
                    while (cursor2.moveToNext()) {
                        if (cursor2.getString(3) != null && new File(cursor2.getString(3)).exists()) {
                            id = cursor2.getLong(0);
                            media = new Media(id, cursor2.getInt(1), cursor2.getLong(2), ContentUris.withAppendedId(baseUri, id), cursor2.getString(3));
                            return media;
                        }
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (cursor2 != null) {
                cursor2.close();
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (cursor2 != null) {
                cursor2.close();
            }
        }
    }

    private static Media getLastVideoThumbnail(ContentResolver resolver) {
        Uri baseUri = android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        Media media = "1";
        Uri query = baseUri.buildUpon().appendQueryParameter("limit", media).build();
        String[] projection = new String[]{"_id", "_data", "datetaken"};
        String selection = getVideoBucketIds() + " AND " + "_size" + " > 0";
        String order = "datetaken DESC,_id DESC";
        Cursor cursor = null;
        Cursor cursor2 = null;
        boolean firstMiss = false;
        try {
            long id;
            cursor = resolver.query(query, projection, selection, null, order);
            if (cursor != null && cursor.moveToFirst()) {
                id = cursor.getLong(0);
                if (cursor.getString(1) == null || !new File(cursor.getString(1)).exists()) {
                    firstMiss = true;
                } else {
                    media = new Media(id, 0, cursor.getLong(2), ContentUris.withAppendedId(baseUri, id), cursor.getString(1));
                    if (cursor != null) {
                        cursor.close();
                    }
                    return media;
                }
            }
            if (firstMiss) {
                cursor2 = resolver.query(baseUri, projection, selection, null, order);
                if (cursor2 != null) {
                    while (cursor2.moveToNext()) {
                        if (cursor2.getString(1) != null && new File(cursor2.getString(1)).exists()) {
                            id = cursor2.getLong(0);
                            media = new Media(id, 0, cursor2.getLong(2), ContentUris.withAppendedId(baseUri, id), cursor2.getString(1));
                            return media;
                        }
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (cursor2 != null) {
                cursor2.close();
            }
            return null;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            if (cursor2 != null) {
                cursor2.close();
            }
        }
    }

    public static Thumbnail createThumbnail(byte[] jpeg, int orientation, int inSampleSize, Uri uri, boolean mirror) {
        Options options = new Options();
        options.inSampleSize = inSampleSize;
        options.inPurgeable = true;
        return createThumbnail(uri, BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options), orientation, mirror);
    }

    public static Thumbnail createThumbnailFromUri(ContentResolver resolver, Uri uri, boolean mirror) {
        if (!(uri == null || uri.getPath() == null)) {
            boolean isImage = uri.getPath().contains(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI.getPath());
            Cursor cursor = resolver.query(uri, isImage ? new String[]{"_id", "_data", "orientation"} : new String[]{"_id", "_data"}, null, null, null);
            long id = -1;
            String str = null;
            int orientation = 0;
            boolean find = false;
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        id = cursor.getLong(0);
                        str = cursor.getString(1);
                        orientation = isImage ? cursor.getInt(2) : 0;
                        find = true;
                    }
                } catch (Throwable th) {
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
            if (cursor != null) {
                cursor.close();
            }
            if (find) {
                Bitmap bitmap;
                if (isImage) {
                    bitmap = Images.Thumbnails.getThumbnail(resolver, id, 1, null);
                    if (bitmap == null) {
                        bitmap = ThumbnailUtils.createImageThumbnail(str, 1);
                    }
                } else {
                    bitmap = Thumbnails.getThumbnail(resolver, id, 1, null);
                    if (bitmap == null) {
                        bitmap = ThumbnailUtils.createVideoThumbnail(str, 1);
                    }
                }
                return createThumbnail(uri, bitmap, orientation, mirror);
            }
        }
        return null;
    }

    public static Bitmap createBitmap(byte[] jpeg, int orientation, boolean mirror, int inSampleSize) {
        Options options = new Options();
        options.inSampleSize = inSampleSize;
        options.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length, options);
        orientation %= 360;
        if (bitmap != null && (orientation != 0 || mirror)) {
            Matrix m1 = new Matrix();
            Matrix m2 = new Matrix();
            if (orientation != 0) {
                m1.setRotate((float) orientation, ((float) bitmap.getWidth()) * 0.5f, ((float) bitmap.getHeight()) * 0.5f);
            }
            if (mirror) {
                m2.setScale(-1.0f, 1.0f, ((float) bitmap.getWidth()) * 0.5f, ((float) bitmap.getHeight()) * 0.5f);
                m1.postConcat(m2);
            }
            try {
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m1, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                return rotated;
            } catch (Exception t) {
                Log.w("Thumbnail", "Failed to rotate thumbnail", t);
            }
        }
        return bitmap;
    }

    public static Bitmap createVideoThumbnailBitmap(FileDescriptor fd, int targetWidth) {
        return createVideoThumbnailBitmap(null, fd, targetWidth);
    }

    public static Bitmap createVideoThumbnailBitmap(String filePath, int targetWidth) {
        return createVideoThumbnailBitmap(filePath, null, targetWidth);
    }

    private static Bitmap createVideoThumbnailBitmap(String filePath, FileDescriptor fd, int targetWidth) {
        Bitmap bitmap = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        if (filePath != null) {
            try {
                retriever.setDataSource(filePath);
            } catch (IllegalArgumentException e) {
                try {
                    retriever.release();
                } catch (RuntimeException e2) {
                }
            } catch (RuntimeException e3) {
                try {
                    retriever.release();
                } catch (RuntimeException e4) {
                }
            } catch (Throwable th) {
                try {
                    retriever.release();
                } catch (RuntimeException e5) {
                }
            }
        } else {
            retriever.setDataSource(fd);
        }
        bitmap = retriever.getFrameAtTime(-1);
        try {
            retriever.release();
        } catch (RuntimeException e6) {
        }
        if (bitmap == null) {
            return null;
        }
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width > targetWidth) {
            float scale = ((float) targetWidth) / ((float) width);
            bitmap = Bitmap.createScaledBitmap(bitmap, Math.round(((float) width) * scale), Math.round(((float) height) * scale), true);
        }
        return bitmap;
    }

    public static Thumbnail createThumbnail(Uri uri, Bitmap bitmap, int orientation, boolean mirror) {
        if (bitmap != null) {
            return new Thumbnail(uri, bitmap, orientation, mirror);
        }
        Log.e("Thumbnail", "Failed to create thumbnail from null bitmap");
        return null;
    }

    public static int getLastThumbnailFromUriList(ContentResolver resolver, Thumbnail[] result, ArrayList<Uri> uriList, Uri uriFromFile) {
        if (uriList == null || uriList.size() == 0) {
            return 0;
        }
        int i = uriList.size() - 1;
        while (i >= 0) {
            Uri uri = (Uri) uriList.get(i);
            if (!Util.isUriValid(uri, resolver)) {
                i--;
            } else if (uriFromFile != null && uriFromFile.equals(uri)) {
                return -1;
            } else {
                result[0] = createThumbnailFromUri(resolver, uri, false);
                return 1;
            }
        }
        return 0;
    }
}
