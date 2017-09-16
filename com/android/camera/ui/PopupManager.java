package com.android.camera.ui;

import android.content.Context;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;

public class PopupManager {
    private static HashMap<Context, PopupManager> sMap = new HashMap();
    private OnOtherPopupShowedListener mLastListener;
    private ArrayList<OnOtherPopupShowedListener> mListeners = new ArrayList();

    public interface OnOtherPopupShowedListener {
        boolean onOtherPopupShowed(int i);

        void recoverIfNeeded();
    }

    private PopupManager() {
    }

    public void notifyShowPopup(View view, int level) {
        for (int i = 0; i < this.mListeners.size(); i++) {
            OnOtherPopupShowedListener listener = (OnOtherPopupShowedListener) this.mListeners.get(i);
            if (((View) listener) != view && listener.onOtherPopupShowed(level)) {
                this.mLastListener = listener;
            }
        }
    }

    public void notifyDismissPopup() {
        if (this.mLastListener != null) {
            this.mLastListener.recoverIfNeeded();
            this.mLastListener = null;
        }
    }

    public void clearRecoveredPopupListenerIfNeeded(OnOtherPopupShowedListener listener) {
        if (this.mLastListener == listener) {
            this.mLastListener = null;
        }
    }

    public void setOnOtherPopupShowedListener(OnOtherPopupShowedListener listener) {
        if (!this.mListeners.contains(listener)) {
            this.mListeners.add(listener);
        }
    }

    public OnOtherPopupShowedListener getLastOnOtherPopupShowedListener() {
        return this.mLastListener;
    }

    public void removeOnOtherPopupShowedListener(OnOtherPopupShowedListener listener) {
        this.mListeners.remove(listener);
    }

    private void onDestroy() {
        if (this.mListeners != null) {
            this.mListeners.clear();
        }
        this.mListeners = null;
        this.mLastListener = null;
    }

    public static PopupManager getInstance(Context context) {
        PopupManager instance = (PopupManager) sMap.get(context);
        if (instance != null) {
            return instance;
        }
        instance = new PopupManager();
        sMap.put(context, instance);
        return instance;
    }

    public static void removeInstance(Context context) {
        PopupManager instance = (PopupManager) sMap.get(context);
        if (instance != null) {
            instance.onDestroy();
            sMap.remove(context);
        }
    }
}
