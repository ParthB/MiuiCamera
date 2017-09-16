package com.android.camera.google;

final class TypeIdTable {
    TypeIdTable() {
    }

    static String getCreateSql() {
        return "CREATE TABLE type_uri (media_store_id INTEGER PRIMARY KEY, special_type_id TEXT NOT NULL)";
    }
}
