package com.google.android.apps.photos.api;

import android.support.v7.recyclerview.R;

public enum IconQuery$Type {
    BADGE("badge", R.dimen.badge_icon_size),
    INTERACT("interact", R.dimen.interact_icon_size),
    DIALOG("dialog", R.dimen.interact_icon_size);
    
    private final int dimensionResourceId;
    private final String path;

    private IconQuery$Type(String path, int dimensionResourceId) {
        this.path = path;
        this.dimensionResourceId = dimensionResourceId;
    }

    public int getDimensionResourceId() {
        return this.dimensionResourceId;
    }
}
