package com.android.camera.ui;

public interface RollAdapter {
    int getCenterIndex();

    int getItemIndexByValue(Object obj);

    int getItemValue(int i);

    int getMaxItem();
}
