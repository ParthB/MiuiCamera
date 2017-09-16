package com.google.android.apps.photos.api;

import android.content.Context;
import android.net.Uri;
import android.net.Uri.Builder;
import android.support.v7.recyclerview.R;

public final class PhotosOemApi {
    public static String getAuthority(Context context) {
        return context.getString(R.string.photos_special_types_authority);
    }

    public static long getMediaStoreIdFromQueryTypeUri(Uri queryTypeUri) {
        return Long.parseLong(Uri.decode(queryTypeUri.getLastPathSegment()));
    }

    public static String getSpecialTypeIdFromQueryDataUri(Uri queryDataUri) {
        return Uri.decode(queryDataUri.getLastPathSegment());
    }

    public static Uri getQueryProcessingUri(Context context, long mediaStoreId) {
        return getBaseBuilder(context).appendPath("processing").appendPath(String.valueOf(mediaStoreId)).build();
    }

    private static Builder getBaseBuilder(Context context) {
        return new Builder().scheme("content").authority(getAuthority(context));
    }
}
