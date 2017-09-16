package com.android.camera.module;

import android.content.Intent;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import com.android.camera.Camera;
import java.util.List;

public interface Module {
    boolean IsIgnoreTouchEvent();

    boolean canIgnoreFocusChanged();

    void checkActivityOrientation();

    boolean dispatchTouchEvent(MotionEvent motionEvent);

    List<String> getSupportedSettingKeys();

    boolean handleMessage(int i, int i2, Object obj, Object obj2);

    boolean isCaptureIntent();

    boolean isKeptBitmapTexture();

    boolean isVideoRecording();

    void notifyError();

    void onActivityResult(int i, int i2, Intent intent);

    boolean onBackPressed();

    void onCreate(Camera camera);

    void onDestroy();

    boolean onGestureTrack(RectF rectF, boolean z);

    boolean onKeyDown(int i, KeyEvent keyEvent);

    boolean onKeyUp(int i, KeyEvent keyEvent);

    void onLongPress(int i, int i2);

    void onNewIntent();

    void onOrientationChanged(int i);

    void onPauseAfterSuper();

    void onPauseBeforeSuper();

    void onPreviewPixelsRead(byte[] bArr, int i, int i2);

    void onPreviewTextureCopied();

    void onResumeAfterSuper();

    void onResumeBeforeSuper();

    void onSaveInstanceState(Bundle bundle);

    boolean onScale(float f, float f2, float f3);

    boolean onScaleBegin(float f, float f2);

    void onScaleEnd();

    void onSingleTapUp(int i, int i2);

    void onStop();

    void onSwitchAnimationDone();

    void onUserInteraction();

    void onWindowFocusChanged(boolean z);

    void requestRender();

    void setRestoring(boolean z);

    void transferOrientationCompensation(Module module);
}
