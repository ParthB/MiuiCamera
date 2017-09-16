package android.support.v4.app;

import java.io.FileDescriptor;
import java.io.PrintWriter;

public abstract class FragmentManager {

    public interface OnBackStackChangedListener {
        void onBackStackChanged();
    }

    public abstract void dump(String str, FileDescriptor fileDescriptor, PrintWriter printWriter, String[] strArr);

    public abstract boolean popBackStackImmediate();
}
