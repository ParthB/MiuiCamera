package miui.external;

import android.util.Log;

/* compiled from: SdkConstants */
class b {
    b() {
    }

    public static Class<?> b() throws ClassNotFoundException {
        Class<?> cls;
        try {
            cls = Class.forName("miui.core.SdkManager");
        } catch (ClassNotFoundException e) {
            try {
                cls = Class.forName("com.miui.internal.core.SdkManager");
                Log.w("miuisdk", "using legacy sdk");
            } catch (ClassNotFoundException e2) {
                Log.e("miuisdk", "no sdk found");
                throw e2;
            }
        }
        return cls;
    }
}
