package com.android.camera.google;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Log;
import com.google.android.apps.photos.api.PhotosOemApi;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProcessingMediaManager {
    private Map<String, JpegThumbnail> mProcessingTable;

    private static class InstanceHolder {
        private static final ProcessingMediaManager sInstance = new ProcessingMediaManager();

        private InstanceHolder() {
        }
    }

    public static class JpegThumbnail {
        final byte[] data;
        final int orientation;
        private Bitmap thumbBitmap;

        public JpegThumbnail(int orientation, byte[] data) {
            this.orientation = orientation;
            this.data = data;
        }

        public Bitmap decodeBitmap() throws FileNotFoundException {
            if (this.thumbBitmap != null) {
                return this.thumbBitmap;
            }
            if (this.data == null) {
                Log.d("ProcessingMedia", "decodeBitmap, empty thumbnail");
                this.thumbBitmap = null;
                throw new FileNotFoundException("Empty thumbnail");
            }
            Bitmap bitmap = null;
            try {
                bitmap = BitmapFactory.decodeByteArray(this.data, 0, this.data.length);
            } catch (OutOfMemoryError error) {
                Log.d("ProcessingMedia", "decodeBitmap, first try failed ", error);
                Options opt = new Options();
                opt.inSampleSize = 2;
                try {
                    bitmap = BitmapFactory.decodeByteArray(this.data, 0, this.data.length, opt);
                } catch (OutOfMemoryError err) {
                    Log.d("ProcessingMedia", "decodeBitmap, second try failed again, ", err);
                }
            }
            if (bitmap == null) {
                Log.d("ProcessingMedia", "decodeBitmap, no bitmap, pass bytes directly");
                this.thumbBitmap = null;
                return null;
            } else if (this.orientation != 0) {
                Matrix mat = new Matrix();
                mat.postRotate((float) this.orientation);
                Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
                if (rotated != bitmap) {
                    bitmap.recycle();
                }
                this.thumbBitmap = rotated;
                return rotated;
            } else {
                this.thumbBitmap = bitmap;
                return bitmap;
            }
        }
    }

    private ProcessingMediaManager() {
        this.mProcessingTable = new ConcurrentHashMap();
    }

    public static ProcessingMediaManager instance() {
        return InstanceHolder.sInstance;
    }

    public void addProcessingMedia(Context context, Uri uri, JpegThumbnail thumb) {
        if (uri != null) {
            Log.d("ProcessingMedia", "addProcessingMedia uri -> " + uri);
            long id = ContentUris.parseId(uri);
            this.mProcessingTable.put(String.valueOf(id), thumb);
            notifyProcessingUri(context, id);
        }
    }

    public JpegThumbnail getProcessingMedia(long id) {
        return (JpegThumbnail) this.mProcessingTable.get(String.valueOf(id));
    }

    public boolean isProcessingMedia(long id) {
        return this.mProcessingTable.containsKey(String.valueOf(id));
    }

    public boolean isProcessingMedia(Uri uri) {
        return isProcessingMedia(ContentUris.parseId(uri));
    }

    public List<String> getProcessingMedias() {
        List<String> ret = new ArrayList();
        for (String id : this.mProcessingTable.keySet()) {
            ret.add(id);
        }
        return ret;
    }

    public void removeProcessingMedia(Context context, Uri uri) {
        if (uri != null) {
            Log.d("ProcessingMedia", "removeProcessingMedia uri->" + uri);
            long id = ContentUris.parseId(uri);
            if (this.mProcessingTable.containsKey(String.valueOf(id))) {
                this.mProcessingTable.remove(String.valueOf(id));
                notifyProcessingUri(context, id);
            }
        }
    }

    public static void notifyProcessingUri(Context context, long mediaStoreId) {
        if (context != null) {
            Uri uri = PhotosOemApi.getQueryProcessingUri(context, mediaStoreId);
            Log.d("ProcessingMedia", "notifyProcessingUri uri-> " + uri);
            context.getContentResolver().notifyChange(uri, null);
        }
    }
}
