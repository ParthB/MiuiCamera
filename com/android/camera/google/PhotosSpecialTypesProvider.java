package com.android.camera.google;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.ProviderInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.net.Uri.Builder;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.recyclerview.R;
import android.text.TextUtils;
import android.util.Log;
import com.android.camera.google.ProcessingMediaManager.JpegThumbnail;
import com.google.android.apps.photos.api.IconQuery$Type;
import com.google.android.apps.photos.api.PhotosOemApi;
import com.google.android.apps.photos.api.Preconditions;
import com.google.android.apps.photos.api.signature.TrustedPartners;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class PhotosSpecialTypesProvider extends ContentProvider {
    private static final String[] TYPE_URI_PROJECTION = new String[]{"special_type_id"};
    private String authority;
    private TrustedPartners trustedPartners;
    private UriMatcher uriMatcher;

    public void attachInfo(Context context, ProviderInfo info) {
        super.attachInfo(context, info);
        this.trustedPartners = new TrustedPartners(context, new HashSet(Arrays.asList(context.getResources().getStringArray(R.array.trusted_certificates))));
        this.authority = info.authority;
        this.uriMatcher = new UriMatcher(-1);
        this.uriMatcher.addURI(this.authority, "type/*", 1);
        this.uriMatcher.addURI(this.authority, "data/*", 2);
        this.uriMatcher.addURI(this.authority, "icon/#/badge", 3);
        this.uriMatcher.addURI(this.authority, "icon/#/interact", 4);
        this.uriMatcher.addURI(this.authority, "icon/#/dialog", 5);
        this.uriMatcher.addURI(this.authority, "delete/#", 6);
        this.uriMatcher.addURI(this.authority, "processing", 7);
        this.uriMatcher.addURI(this.authority, "processing/#", 8);
    }

    public boolean onCreate() {
        return true;
    }

    @Nullable
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        if (TextUtils.equals("version", method)) {
            return querySpecialTypesVersion();
        }
        return super.call(method, arg, extras);
    }

    @Nullable
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        validateCallingPackage();
        Log.d("PhotoTypes", "query, uri-> " + uri);
        switch (this.uriMatcher.match(uri)) {
            case 1:
                return querySpecialTypeId(uri);
            case 2:
                return querySpecialTypeMetadata(uri, projection);
            case 7:
                return queryProcessingMetadata(null);
            case 8:
                return queryProcessingMetadata(Long.valueOf(ContentUris.parseId(uri)));
            default:
                throw new IllegalArgumentException("Unrecognized uri: " + uri);
        }
    }

    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        validateCallingPackage();
        switch (this.uriMatcher.match(uri)) {
            case 6:
                boolean z;
                if (selection == null) {
                    z = true;
                } else {
                    z = false;
                }
                Preconditions.checkArgument(z);
                if (selectionArgs == null) {
                    z = true;
                } else {
                    z = false;
                }
                Preconditions.checkArgument(z);
                if (deleteSpecialType(uri)) {
                    return 1;
                }
                return 0;
            default:
                throw new IllegalArgumentException("Unrecognized uri: " + uri);
        }
    }

    private boolean deleteSpecialType(Uri uri) {
        Log.d("PhotoTypes", "delete uri->" + uri + ", media id -> " + uri.getLastPathSegment());
        ProviderDbHelper.get(getContext()).getReadableDatabase().delete("type_uri", "media_store_id=?", new String[]{id});
        return true;
    }

    private Bundle querySpecialTypesVersion() {
        Bundle result = new Bundle();
        result.putInt("version", 3);
        return result;
    }

    private Cursor queryProcessingMetadata(@Nullable Long mediaStoreId) {
        Log.d("PhotoTypes", "queryProcessingMetaData -> " + mediaStoreId);
        MatrixCursor retCursor = new MatrixCursor(new String[]{"media_store_id", "progress_status", "progress_percentage"});
        if (mediaStoreId == null) {
            List<String> medias = ProcessingMediaManager.instance().getProcessingMedias();
            Log.d("PhotoTypes", "query processing medias -> " + medias);
            Iterator id$iterator = medias.iterator();
            while (id$iterator.hasNext()) {
                retCursor.addRow(new Object[]{(String) id$iterator.next(), Integer.valueOf(1), Integer.valueOf(0)});
            }
        } else if (ProcessingMediaManager.instance().isProcessingMedia(mediaStoreId.longValue())) {
            Log.d("PhotoTypes", "query processing add into resutl id => " + mediaStoreId);
            retCursor.addRow(new Object[]{mediaStoreId, Integer.valueOf(1), Integer.valueOf(0)});
        }
        retCursor.moveToPosition(-1);
        return retCursor;
    }

    private Cursor querySpecialTypeId(Uri uri) {
        Log.d("PhotoTypes", "querySepcial Type id uri->" + uri);
        return queryOrScanAndQuery(ProviderDbHelper.get(getContext()).getReadableDatabase(), PhotosOemApi.getMediaStoreIdFromQueryTypeUri(uri));
    }

    private static Cursor queryOrScanAndQuery(SQLiteDatabase db, long mediaStoreId) {
        SpecialType type = querySpecialTypeId(db, mediaStoreId);
        Log.d("PhotoTypes", "queryOrScanAndQuery from query -> " + type);
        MatrixCursor result = new MatrixCursor(new String[]{"special_type_id"});
        if (!(type == SpecialType.NONE || type == SpecialType.UNKNOWN)) {
            result.addRow(new Object[]{type.name()});
        }
        return result;
    }

    private static SpecialType querySpecialTypeId(SQLiteDatabase db, long mediaStoreId) {
        SpecialType result = SpecialType.UNKNOWN;
        SQLiteDatabase sQLiteDatabase = db;
        Cursor cursor = sQLiteDatabase.query("type_uri", TYPE_URI_PROJECTION, "media_store_id = ?", new String[]{String.valueOf(mediaStoreId)}, null, null, null);
        try {
            if (cursor.moveToFirst()) {
                result = SpecialType.valueOf(cursor.getString(cursor.getColumnIndexOrThrow("special_type_id")));
            }
            cursor.close();
            return result;
        } catch (Throwable th) {
            cursor.close();
        }
    }

    private Cursor querySpecialTypeMetadata(Uri uri, String[] projection) {
        String specialTypeIdString = PhotosOemApi.getSpecialTypeIdFromQueryDataUri(uri);
        SpecialType specialType = SpecialType.valueOf(specialTypeIdString);
        Log.d("PhotoTypes", "query special uri -> " + uri);
        Log.d("PhotoTypes", "query special type id str -> " + specialTypeIdString + ", specialType->" + specialType);
        MatrixCursor result = new MatrixCursor(projection);
        Object[] row = new Object[projection.length];
        int i = 0;
        for (String column : projection) {
            if (column.equals("configuration")) {
                row[i] = specialType.getConfiguration().getKey();
            } else if (column.equals("special_type_name")) {
                row[i] = getContext().getString(specialType.nameResourceId);
            } else if (column.equals("special_type_description")) {
                row[i] = getContext().getString(specialType.descriptionResourceId);
            } else if (column.equals("special_type_icon_uri")) {
                row[i] = new Builder().scheme("content").authority(this.authority).appendPath("icon").appendPath(String.valueOf(specialType.typeId));
            } else {
                row[i] = null;
            }
            i++;
        }
        result.addRow(row);
        return result;
    }

    @Nullable
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        validateCallingPackage();
        if ("r".equals(mode)) {
            switch (this.uriMatcher.match(uri)) {
                case 3:
                    Log.i("PhotoTypes", "loading badge icon " + uri);
                    return loadIcon(uri, IconQuery$Type.BADGE);
                case 4:
                    Log.i("PhotoTypes", "loading interact icon " + uri);
                    return loadIcon(uri, IconQuery$Type.INTERACT);
                case 5:
                    Log.i("PhotoTypes", "loading dialog icon " + uri);
                    return loadIcon(uri, IconQuery$Type.DIALOG);
                case 8:
                    Log.i("PhotoTypes", "loading processing thumb " + uri);
                    return loadProcessingThumb(uri);
                default:
                    throw new IllegalArgumentException("Unrecognized format: " + uri);
            }
        }
        throw new IllegalArgumentException("Unsupported mode: " + mode);
    }

    private ParcelFileDescriptor loadProcessingThumb(Uri uri) throws FileNotFoundException {
        long mediaStoreId = ContentUris.parseId(uri);
        if (ProcessingMediaManager.instance().isProcessingMedia(mediaStoreId)) {
            JpegThumbnail thumb = ProcessingMediaManager.instance().getProcessingMedia(mediaStoreId);
            if (thumb == null) {
                throw new FileNotFoundException("Empty thumbnail");
            }
            Bitmap bm = thumb.decodeBitmap();
            if (bm == null) {
                return writeBytesToFd(thumb.data);
            }
            return writeBitmapToFd(bm, CompressFormat.JPEG);
        }
        throw new FileNotFoundException("Cannot find processing thumb for " + mediaStoreId);
    }

    private ParcelFileDescriptor writeBytesToFd(byte[] data) throws FileNotFoundException {
        IOException exception;
        IOException e;
        Throwable th;
        ParcelFileDescriptor parcelFileDescriptor = null;
        OutputStream outputStream = null;
        try {
            ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor outputFd = descriptors[0];
            parcelFileDescriptor = descriptors[1];
            OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
            try {
                outputStream2.write(data, 0, data.length);
                outputStream2.close();
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e2) {
                        exception = e2;
                    }
                }
                if (outputStream2 != null) {
                    try {
                        outputStream2.close();
                    } catch (IOException e22) {
                        exception = e22;
                    }
                }
                return outputFd;
            } catch (IOException e3) {
                e22 = e3;
                outputStream = outputStream2;
                exception = e22;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e222) {
                        exception = e222;
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e2222) {
                        exception = e2222;
                    }
                }
                throw new FileNotFoundException(exception.getMessage());
            } catch (Throwable th2) {
                th = th2;
                outputStream = outputStream2;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e22222) {
                        exception = e22222;
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e222222) {
                        exception = e222222;
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e222222 = e4;
            exception = e222222;
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            throw new FileNotFoundException(exception.getMessage());
        } catch (Throwable th3) {
            th = th3;
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            throw th;
        }
    }

    private ParcelFileDescriptor writeBitmapToFd(Bitmap bitmap, CompressFormat compressFormat) throws FileNotFoundException {
        IOException exception;
        IOException e;
        Throwable th;
        ParcelFileDescriptor parcelFileDescriptor = null;
        OutputStream outputStream = null;
        try {
            ParcelFileDescriptor[] descriptors = ParcelFileDescriptor.createPipe();
            ParcelFileDescriptor outputFd = descriptors[0];
            parcelFileDescriptor = descriptors[1];
            OutputStream outputStream2 = new BufferedOutputStream(new FileOutputStream(parcelFileDescriptor.getFileDescriptor()));
            try {
                bitmap.compress(compressFormat, 100, outputStream2);
                outputStream2.close();
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e2) {
                        exception = e2;
                    }
                }
                if (outputStream2 != null) {
                    try {
                        outputStream2.close();
                    } catch (IOException e22) {
                        exception = e22;
                    }
                }
                return outputFd;
            } catch (IOException e3) {
                e22 = e3;
                outputStream = outputStream2;
                exception = e22;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e222) {
                        exception = e222;
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e2222) {
                        exception = e2222;
                    }
                }
                throw new FileNotFoundException(exception.getMessage());
            } catch (Throwable th2) {
                th = th2;
                outputStream = outputStream2;
                if (parcelFileDescriptor != null) {
                    try {
                        parcelFileDescriptor.close();
                    } catch (IOException e22222) {
                        exception = e22222;
                    }
                }
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e222222) {
                        exception = e222222;
                    }
                }
                throw th;
            }
        } catch (IOException e4) {
            e222222 = e4;
            exception = e222222;
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            throw new FileNotFoundException(exception.getMessage());
        } catch (Throwable th3) {
            th = th3;
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            throw th;
        }
    }

    @Nullable
    private ParcelFileDescriptor loadIcon(Uri uri, IconQuery$Type type) throws FileNotFoundException {
        Log.d("PhotoTypes", "load Icon uri->" + uri);
        SpecialType specialType = SpecialType.fromTypeId(Integer.valueOf((String) uri.getPathSegments().get(1)).intValue());
        int resourceId = type == IconQuery$Type.BADGE ? specialType.iconBadgeResourceId : specialType.iconDialogResourceId;
        Resources resources = getContext().getResources();
        BitmapDrawable bitmapDrawable = (BitmapDrawable) resources.getDrawable(resourceId);
        int pixels = resources.getDimensionPixelSize(type.getDimensionResourceId());
        return writeBitmapToFd(Bitmap.createScaledBitmap(bitmapDrawable.getBitmap(), pixels, pixels, false), CompressFormat.PNG);
    }

    @Nullable
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Nullable
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        validateCallingPackage();
        Log.d("PhotoTypes", "insert uri->" + uri + ", values " + values);
        SpecialType specialType = SpecialType.valueOf(PhotosOemApi.getSpecialTypeIdFromQueryDataUri(uri));
        if (specialType != SpecialType.PORTRAIT_TYPE) {
            return null;
        }
        SQLiteDatabase db = ProviderDbHelper.get(getContext()).getReadableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("media_store_id", values.getAsString("media_store_id"));
        cv.put("special_type_id", specialType.name());
        long id = db.replace("type_uri", null, cv);
        return uri;
    }

    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    private void validateCallingPackage() {
        if (!this.trustedPartners.isTrustedApplication(getCallingPackage())) {
            throw new SecurityException();
        }
    }

    public static void markPortraitSpecialType(Context context, Uri uri) {
        if (uri != null) {
            long mediaId = ContentUris.parseId(uri);
            if (mediaId > 0) {
                Uri typeUri = new Builder().scheme("content").authority(context.getString(R.string.photos_special_types_authority)).appendEncodedPath(Uri.encode(SpecialType.PORTRAIT_TYPE.toString())).build();
                ContentResolver cr = context.getContentResolver();
                ContentValues cv = new ContentValues();
                cv.put("media_store_id", Long.valueOf(mediaId));
                cr.insert(typeUri, cv);
            }
        }
    }
}
