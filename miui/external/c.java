package miui.external;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import java.io.File;

/* compiled from: SdkHelper */
class c {
    private c() {
    }

    public static boolean j() {
        return i("miui") != null;
    }

    public static String c(Context context, String str, String str2) {
        if (context == null) {
            return f(str, str2);
        }
        PackageInfo e = e(context, str);
        if (e != null) {
            return e.applicationInfo.publicSourceDir;
        }
        return null;
    }

    private static String f(String str, String str2) {
        String g = g(str);
        if (g == null) {
            return i(str2);
        }
        return g;
    }

    private static String g(String str) {
        return k(new String[]{"/data/app/" + str + "-1.apk", "/data/app/" + str + "-2.apk", "/data/app/" + str + "-1/base.apk", "/data/app/" + str + "-2/base.apk"});
    }

    private static String i(String str) {
        return k(new String[]{"/system/app/" + str + ".apk", "/system/priv-app/" + str + ".apk", "/system/app/" + str + "/" + str + ".apk", "/system/priv-app/" + str + "/" + str + ".apk"});
    }

    private static String k(String[] strArr) {
        for (String str : strArr) {
            if (new File(str).exists()) {
                return str;
            }
        }
        return null;
    }

    public static String d(Context context, String str) {
        if (context == null) {
            return h(str);
        }
        PackageInfo e = e(context, str);
        if (e != null) {
            return e.applicationInfo.nativeLibraryDir;
        }
        return null;
    }

    private static String h(String str) {
        return "/data/data/" + str + "/lib/";
    }

    private static PackageInfo e(Context context, String str) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = context.getPackageManager().getPackageInfo(str, 128);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return packageInfo;
    }
}
