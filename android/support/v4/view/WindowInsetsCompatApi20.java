package android.support.v4.view;

import android.view.WindowInsets;

class WindowInsetsCompatApi20 {
    WindowInsetsCompatApi20() {
    }

    public static int getSystemWindowInsetBottom(Object insets) {
        return ((WindowInsets) insets).getSystemWindowInsetBottom();
    }
}
