package miui.external;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

public class Application extends android.app.Application {
    private ApplicationDelegate s;
    private boolean t;
    private boolean u;

    public Application() {
        if (T() && S()) {
            this.t = true;
        }
    }

    private boolean T() {
        try {
            if (c.j() || e.F(c.c(null, "com.miui.core", "miui"), null, c.d(null, "com.miui.core"), Application.class.getClassLoader())) {
                return true;
            }
            d.B(SdkConstants$SdkError.NO_SDK);
            return false;
        } catch (Throwable th) {
            Q(th);
            return false;
        }
    }

    private boolean S() {
        try {
            HashMap hashMap = new HashMap();
            int intValue = ((Integer) b.b().getMethod("initialize", new Class[]{android.app.Application.class, Map.class}).invoke(null, new Object[]{this, hashMap})).intValue();
            if (intValue == 0) {
                return true;
            }
            R("initialize", intValue);
            return false;
        } catch (Throwable th) {
            Q(th);
            return false;
        }
    }

    private boolean U() {
        try {
            HashMap hashMap = new HashMap();
            int intValue = ((Integer) b.b().getMethod("start", new Class[]{Map.class}).invoke(null, new Object[]{hashMap})).intValue();
            if (intValue == 1) {
                d.B(SdkConstants$SdkError.LOW_SDK_VERSION);
                return false;
            } else if (intValue == 0) {
                return true;
            } else {
                R("start", intValue);
                return false;
            }
        } catch (Throwable th) {
            Q(th);
            return false;
        }
    }

    private void Q(Throwable th) {
        while (th != null && th.getCause() != null) {
            if (!(th instanceof InvocationTargetException)) {
                if (!(th instanceof ExceptionInInitializerError)) {
                    break;
                }
                th = th.getCause();
            } else {
                th = th.getCause();
            }
        }
        Log.e("miuisdk", "MIUI SDK encounter errors, please contact miuisdk@xiaomi.com for support.", th);
        d.B(SdkConstants$SdkError.GENERIC);
    }

    private void R(String str, int i) {
        Log.e("miuisdk", "MIUI SDK encounter errors, please contact miuisdk@xiaomi.com for support. phase: " + str + " code: " + i);
        d.B(SdkConstants$SdkError.GENERIC);
    }

    public ApplicationDelegate onCreateApplicationDelegate() {
        return null;
    }

    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        if (this.t && U()) {
            this.s = onCreateApplicationDelegate();
            if (this.s != null) {
                this.s.a(this);
            }
            this.u = true;
        }
    }

    public final void onCreate() {
        if (this.u) {
            if (this.s != null) {
                this.s.onCreate();
            } else {
                L();
            }
        }
    }

    final void L() {
        super.onCreate();
    }

    public final void onTerminate() {
        if (this.s != null) {
            this.s.onTerminate();
        } else {
            M();
        }
    }

    final void M() {
        super.onTerminate();
    }

    public final void onLowMemory() {
        if (this.s != null) {
            this.s.onLowMemory();
        } else {
            O();
        }
    }

    final void O() {
        super.onLowMemory();
    }

    public final void onTrimMemory(int i) {
        if (this.s != null) {
            this.s.onTrimMemory(i);
        } else {
            P(i);
        }
    }

    final void P(int i) {
        super.onTrimMemory(i);
    }

    public final void onConfigurationChanged(Configuration configuration) {
        if (this.s != null) {
            this.s.onConfigurationChanged(configuration);
        } else {
            N(configuration);
        }
    }

    final void N(Configuration configuration) {
        super.onConfigurationChanged(configuration);
    }
}
