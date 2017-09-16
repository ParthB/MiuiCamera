package com.android.camera.google;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

final class ProviderDbHelper extends SQLiteOpenHelper {
    private static volatile ProviderDbHelper helper;

    static ProviderDbHelper get(Context context) {
        if (helper == null) {
            synchronized (ProviderDbHelper.class) {
                if (helper == null) {
                    helper = new ProviderDbHelper(context);
                }
            }
        }
        return helper;
    }

    private ProviderDbHelper(Context context) {
        super(context, "provider_db_helper", null, 2);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TypeIdTable.getCreateSql());
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int currentVersion = oldVersion;
        while (currentVersion < newVersion) {
            switch (currentVersion) {
                case 1:
                    db.delete("type_uri", null, null);
                    currentVersion++;
                    break;
                default:
                    break;
            }
        }
    }
}
