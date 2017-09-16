package com.android.camera.google;

enum ConfigurationImpl {
    BADGE("badge") {
        void validate(SpecialType specialType) {
            super.validate(specialType);
        }
    };
    
    private final String key;

    private ConfigurationImpl(String key) {
        this.key = key;
    }

    String getKey() {
        return this.key;
    }

    void validate(SpecialType specialType) {
        checkResourceId(specialType.descriptionResourceId, "description");
        checkResourceId(specialType.iconBadgeResourceId, "icon");
        checkResourceId(specialType.iconDialogResourceId, "icon");
        checkResourceId(specialType.nameResourceId, "name");
    }

    private static void checkResourceId(int resourceId, String name) {
        boolean z = false;
        if (resourceId != 0) {
            z = true;
        }
        checkArgument(z, name + " must be a valid resource id");
    }

    private static void checkArgument(boolean argument, String message) {
        if (!argument) {
            throw new IllegalArgumentException(message);
        }
    }
}
