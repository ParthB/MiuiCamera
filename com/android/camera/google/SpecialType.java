package com.android.camera.google;

import android.app.Activity;
import android.support.annotation.Nullable;
import android.support.v7.recyclerview.R;

public enum SpecialType {
    UNKNOWN,
    NONE,
    PORTRAIT_TYPE(1, R.string.photos_portrait_type_name, R.string.photos_portrait_type_description, R.drawable.ic_photos_portrait_badge, R.drawable.ic_photos_portrait_dialog, null, null, null, ConfigurationImpl.BADGE);
    
    @Nullable
    private final ConfigurationImpl configuration;
    final int descriptionResourceId;
    @Nullable
    private final Class<? extends Activity> editActivityClass;
    final int iconBadgeResourceId;
    final int iconDialogResourceId;
    @Nullable
    private final Class<? extends Activity> interactActivityClass;
    @Nullable
    private Class<? extends Activity> launchActivityClass;
    final int nameResourceId;
    final int typeId;

    private SpecialType(int typeId, int nameResourceId, int descriptionResourceId, int iconBadgeResourceId, int iconDialogResourceId, Class<? extends Activity> editActivityClass, Class<? extends Activity> interactActivityClass, @Nullable Class<? extends Activity> launchActivityClass, @Nullable ConfigurationImpl configuration) {
        this.typeId = typeId;
        this.nameResourceId = nameResourceId;
        this.descriptionResourceId = descriptionResourceId;
        this.iconBadgeResourceId = iconBadgeResourceId;
        this.iconDialogResourceId = iconDialogResourceId;
        this.editActivityClass = editActivityClass;
        this.interactActivityClass = interactActivityClass;
        this.launchActivityClass = launchActivityClass;
        this.configuration = configuration;
        if (configuration != null) {
            configuration.validate(this);
        }
    }

    ConfigurationImpl getConfiguration() {
        if (this.configuration != null) {
            return this.configuration;
        }
        throw new UnsupportedOperationException();
    }

    static SpecialType fromTypeId(int id) {
        switch (id) {
            case 1:
                return PORTRAIT_TYPE;
            default:
                return UNKNOWN;
        }
    }
}
