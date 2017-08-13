package miui.external;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.ContextWrapper;
import android.content.res.Configuration;

public abstract class ApplicationDelegate extends ContextWrapper implements ComponentCallbacks2 {
    private Application a;

    public ApplicationDelegate() {
        super(null);
    }

    void a(Application application) {
        this.a = application;
        attachBaseContext(application);
    }

    public void onCreate() {
        this.a.L();
    }

    public void onTerminate() {
        this.a.M();
    }

    public void onConfigurationChanged(Configuration configuration) {
        this.a.N(configuration);
    }

    public void onLowMemory() {
        this.a.O();
    }

    public void onTrimMemory(int i) {
        this.a.P(i);
    }

    public void registerComponentCallbacks(ComponentCallbacks componentCallbacks) {
        this.a.registerComponentCallbacks(componentCallbacks);
    }

    public void unregisterComponentCallbacks(ComponentCallbacks componentCallbacks) {
        this.a.unregisterComponentCallbacks(componentCallbacks);
    }
}
