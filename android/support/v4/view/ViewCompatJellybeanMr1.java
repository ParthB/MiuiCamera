package android.support.v4.view;

import android.view.Display;
import android.view.View;

class ViewCompatJellybeanMr1 {
    ViewCompatJellybeanMr1() {
    }

    public static int getLayoutDirection(View view) {
        return view.getLayoutDirection();
    }

    public static Display getDisplay(View view) {
        return view.getDisplay();
    }
}
