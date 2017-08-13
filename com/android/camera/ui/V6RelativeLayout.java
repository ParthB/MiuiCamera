package com.android.camera.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import java.util.ArrayList;

public class V6RelativeLayout extends RelativeLayout implements Rotatable, V6FunctionUI {
    protected ArrayList<View> mChildren = new ArrayList();

    public V6RelativeLayout(Context context) {
        super(context);
    }

    public V6RelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public View findChildrenById(int id) {
        View view = super.findViewById(id);
        if (view != null) {
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
