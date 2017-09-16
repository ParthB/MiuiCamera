package com.android.camera.ui;

import android.view.View;
import com.android.camera.Camera;
import java.util.ArrayList;

public class V6ModuleUI implements Rotatable, V6FunctionUI {
    protected Camera mActivity;
    protected ArrayList<View> mChildren = new ArrayList();

    public V6ModuleUI(Camera activity) {
        this.mActivity = activity;
    }

    public View findViewById(int id) {
        View view = this.mActivity.findViewById(id);
        if (!this.mChildren.contains(view)) {
            this.mChildren.add(view);
        }
        return view;
    }

    public void onCreate() {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).onCreate();
            }
        }
    }

    public void onResume() {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).onResume();
            }
        }
    }

    public void onPause() {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).onPause();
            }
        }
    }

    public void onCameraOpen() {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).onCameraOpen();
            }
        }
    }

    public void enableControls(boolean enable) {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).enableControls(enable);
            }
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        for (View i : this.mChildren) {
            if (i instanceof Rotatable) {
                ((Rotatable) i).setOrientation(orientation, animation);
            }
        }
    }

    public void setMessageDispacher(MessageDispacher p) {
        for (View i : this.mChildren) {
            if (i instanceof V6FunctionUI) {
                ((V6FunctionUI) i).setMessageDispacher(p);
            }
        }
    }
}
