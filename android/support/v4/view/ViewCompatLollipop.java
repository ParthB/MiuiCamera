package android.support.v4.view;

import android.view.View;
import android.view.View.OnApplyWindowInsetsListener;
import android.view.WindowInsets;

class ViewCompatLollipop {

    public interface OnApplyWindowInsetsListenerBridge {
        Object onApplyWindowInsets(View view, Object obj);
    }

    ViewCompatLollipop() {
    }

    public static void setOnApplyWindowInsetsListener(View view, final OnApplyWindowInsetsListenerBridge bridge) {
        if (bridge == null) {
            view.setOnApplyWindowInsetsListener(null);
        } else {
            view.setOnApplyWindowInsetsListener(new OnApplyWindowInsetsListener() {
                public WindowInsets onApplyWindowInsets(View view, WindowInsets insets) {
                    return (WindowInsets) bridge.onApplyWindowInsets(view, insets);
                }
            });
        }
    }

    public static Object onApplyWindowInsets(View v, Object insets) {
        WindowInsets unwrapped = (WindowInsets) insets;
        WindowInsets result = v.onApplyWindowInsets(unwrapped);
        if (result != unwrapped) {
            return new WindowInsets(result);
        }
        return insets;
    }

    public static void stopNestedScroll(View view) {
        view.stopNestedScroll();
    }
}
