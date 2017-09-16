package com.android.camera.ui;

import android.animation.AnimatorInflater;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioSystem;
import android.opengl.GLSurfaceView.Renderer;
import android.support.v4.view.ViewCompat;
import android.support.v7.recyclerview.R;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.LayoutParams;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraScreenNail;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.aosp_porting.ReflectUtil;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.gallery3d.ui.GLCanvas;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class EffectPopup extends V6AbstractSettingPopup implements OnClickListener {
    private final String TAG = "EffectPopup";
    protected int mCurrentIndex = -1;
    private EffectItemAdapter mEffectItemAdapter;
    private List<EffectItemHolder> mEffectItemHolderList = new LinkedList();
    private int mHolderHeight;
    private int mHolderWidth;
    protected boolean mIgnoreSameItemClick = true;
    private LinearLayoutManager mLayoutManager;
    private Recycler mRecycler;
    private RecyclerView mRecyclerView;
    private EffectSelectedOverlay mSelectedOverlay;
    private int mTextureHeight;
    private int mTextureOffsetX;
    private int mTextureOffsetY;
    private int mTextureWidth;
    private int mTotalWidth;

    protected class EffectDivider extends ItemDecoration {
        protected int mFrameWidth;
        protected int mPadding;
        protected Paint mPaint = new Paint(1);
        protected int mPosition;
        protected int mVerticalPadding;
        protected int mWidth;

        public EffectDivider(int position) {
            Resources resources = EffectPopup.this.mContext.getResources();
            this.mPadding = resources.getDimensionPixelSize(R.dimen.effect_item_padding);
            this.mWidth = resources.getDimensionPixelSize(R.dimen.effect_divider_width);
            this.mFrameWidth = resources.getDimensionPixelSize(R.dimen.effect_divider_frame_width);
            this.mVerticalPadding = resources.getDimensionPixelSize(R.dimen.effect_divider_vertical_padding);
            this.mPosition = position;
            this.mPaint.setStyle(Style.STROKE);
            this.mPaint.setColor(resources.getColor(R.color.effect_divider_color));
            this.mPaint.setStrokeWidth((float) this.mWidth);
        }

        public void onDraw(Canvas c, RecyclerView parent, State state) {
            super.onDraw(c, parent, state);
            int top = parent.getPaddingTop() + this.mVerticalPadding;
            int bottom = (parent.getHeight() - parent.getPaddingBottom()) - this.mVerticalPadding;
            ViewHolder holder = parent.findViewHolderForPosition(this.mPosition);
            if (holder != null) {
                View view = holder.itemView;
                int position = ((view.getRight() + ((LayoutParams) view.getLayoutParams()).rightMargin) + Math.round(ViewCompat.getTranslationX(view))) + (this.mFrameWidth / 2);
                c.drawLine((float) position, (float) top, (float) position, (float) bottom, this.mPaint);
            }
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            if (parent.getChildPosition(view) == this.mPosition) {
                outRect.set(0, 0, this.mFrameWidth - this.mPadding, 0);
            }
        }
    }

    protected abstract class EffectItemHolder extends ViewHolder {
        protected int mEffectIndex;
        protected TextView mTextView;

        public EffectItemHolder(View itemView) {
            super(itemView);
            this.mTextView = (TextView) itemView.findViewById(R.id.effect_item_text);
            updateBackground();
        }

        public void updateBackground() {
            if (((ActivityBase) EffectPopup.this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
                this.mTextView.setBackgroundResource(R.color.effect_item_text_fullscreen_background);
            } else {
                this.mTextView.setBackgroundResource(R.color.effect_item_text_halfscreen_background);
            }
        }

        public void bindEffectIndex(int index) {
            this.mEffectIndex = index;
            this.mTextView.setText(EffectPopup.this.mPreference.getEntries()[this.mEffectIndex]);
        }

        public void stop() {
        }

        public void start() {
        }

        public void pause() {
        }

        public void resume() {
        }

        public void requestRender() {
        }
    }

    protected class EffectDynamicItemHolder extends EffectItemHolder {
        protected EffectDynamicItemRender mEffectRender;
        protected GLSurfaceTexture mEffectSurface = new GLSurfaceTexture();
        protected TextureView mTextureView;

        public EffectDynamicItemHolder(View itemView) {
            super(itemView);
            this.mTextureView = (TextureView) itemView.findViewById(R.id.effect_item_texture);
            this.mEffectRender = new EffectDynamicItemRender();
            this.mEffectSurface.setEGLContextClientVersion(2);
            this.mEffectSurface.setRenderer(this.mEffectRender);
            this.mEffectSurface.setPreserveEGLContextOnPause(true);
            this.mEffectSurface.setRenderMode(0);
            this.mEffectSurface.setSize(EffectPopup.this.mHolderWidth, EffectPopup.this.mHolderHeight);
            this.mEffectSurface.startWithShareContext(((ActivityBase) EffectPopup.this.mContext).getUIController().getGLView().getEGLContext());
        }

        public void bindEffectIndex(int index) {
            super.bindEffectIndex(index);
            this.mEffectRender.bindEffectIndex(index);
        }

        public void stop() {
            this.mEffectSurface.stop();
        }

        public void start() {
            if (this.mTextureView.getSurfaceTexture() != this.mEffectSurface) {
                this.mTextureView.setSurfaceTexture(this.mEffectSurface);
            }
            this.mEffectSurface.startWithShareContext(((ActivityBase) EffectPopup.this.mContext).getUIController().getGLView().getEGLContext());
        }

        public void pause() {
            this.mEffectSurface.pause();
        }

        public void resume() {
            this.mEffectSurface.resume();
            if (this.mTextureView.getSurfaceTexture() != this.mEffectSurface) {
                this.mTextureView.setSurfaceTexture(this.mEffectSurface);
            }
        }

        public void requestRender() {
            this.mEffectSurface.requestRender();
        }
    }

    protected class EffectDynamicItemRender implements Renderer {
        int mEffectIndex;
        private DrawExtTexAttribute mExtTexture = new DrawExtTexAttribute(true);
        float[] mTransform = new float[16];

        protected EffectDynamicItemRender() {
        }

        public void bindEffectIndex(int index) {
            this.mEffectIndex = index;
        }

        public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        }

        public void onSurfaceChanged(GL10 gl10, int width, int height) {
        }

        public void onDrawFrame(GL10 gl10) {
            CameraScreenNail screen = ((ActivityBase) EffectPopup.this.mContext).getCameraScreenNail();
            GLCanvas canvas = ((ActivityBase) EffectPopup.this.mContext).getUIController().getGLView().getGLCanvas();
            if (screen != null && canvas != null && screen.getSurfaceTexture() != null) {
                synchronized (canvas) {
                    canvas.clearBuffer();
                    int oldWidth = canvas.getWidth();
                    int oldHeight = canvas.getHeight();
                    canvas.getState().pushState();
                    canvas.setSize(EffectPopup.this.mHolderWidth, EffectPopup.this.mHolderHeight);
                    screen.getSurfaceTexture().getTransformMatrix(this.mTransform);
                    EffectController controller = EffectController.getInstance();
                    synchronized (controller) {
                        int oldEffect = controller.getEffect(false);
                        controller.setEffect(this.mEffectIndex);
                        canvas.draw(this.mExtTexture.init(screen.getExtTexture(), this.mTransform, EffectPopup.this.mTextureOffsetX, EffectPopup.this.mTextureOffsetY, EffectPopup.this.mTextureWidth, EffectPopup.this.mTextureHeight));
                        controller.setEffect(oldEffect);
                    }
                    canvas.setSize(oldWidth, oldHeight);
                    canvas.getState().popState();
                    canvas.recycledResources();
                }
            }
        }
    }

    protected class EffectItemAdapter extends Adapter {
        protected List<Map<String, Object>> mEffectItem;
        protected LayoutInflater mLayoutInflater;

        public EffectItemAdapter(Context context, List<Map<String, Object>> effectItem) {
            this.mEffectItem = effectItem;
            this.mLayoutInflater = LayoutInflater.from(context);
        }

        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view;
            EffectItemHolder holder;
            if (Device.isSupportedDynamicEffectPopup()) {
                view = this.mLayoutInflater.inflate(R.layout.effect_dynamic_item, parent, false);
                holder = new EffectDynamicItemHolder(view);
            } else {
                view = this.mLayoutInflater.inflate(R.layout.effect_still_item, parent, false);
                holder = new EffectStillItemHolder(view);
            }
            view.setOnClickListener(EffectPopup.this);
            EffectPopup.this.mEffectItemHolderList.add(holder);
            return holder;
        }

        public void onBindViewHolder(ViewHolder holder, int position) {
            EffectItemHolder effectHolder = (EffectItemHolder) holder;
            effectHolder.itemView.setTag(Integer.valueOf(position));
            effectHolder.bindEffectIndex(position);
        }

        public void onViewAttachedToWindow(ViewHolder holder) {
            ((EffectItemHolder) holder).resume();
            super.onViewAttachedToWindow(holder);
        }

        public void onViewDetachedFromWindow(ViewHolder holder) {
            ((EffectItemHolder) holder).pause();
            super.onViewDetachedFromWindow(holder);
        }

        public int getItemCount() {
            return this.mEffectItem.size();
        }
    }

    protected class EffectItemPadding extends ItemDecoration {
        protected int mPadding;

        public EffectItemPadding() {
            this.mPadding = EffectPopup.this.mContext.getResources().getDimensionPixelSize(R.dimen.effect_item_padding);
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            int i = 0;
            if (parent.getChildPosition(view) == 0) {
                i = this.mPadding;
            }
            outRect.set(i, this.mPadding, this.mPadding, this.mPadding);
        }
    }

    protected class EffectSelectedOverlay extends ItemDecoration {
        protected ObjectAnimator mAnimator;
        protected int mOffsetX;
        protected Drawable mOverlay;
        protected int mPosition;

        public EffectSelectedOverlay() {
            this.mAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(EffectPopup.this.mContext, R.anim.effect_select_slide);
            this.mOverlay = EffectPopup.this.mContext.getResources().getDrawable(R.drawable.effect_item_selected);
            this.mAnimator.setTarget(this);
        }

        public void setOffsetX(int offsetX) {
            this.mOffsetX = offsetX;
            EffectPopup.this.mRecyclerView.postInvalidateOnAnimation();
        }

        public void select(int position) {
            this.mAnimator.cancel();
            this.mAnimator.setIntValues(new int[]{calcOffsetX(this.mPosition, position), 0});
            this.mPosition = position;
            this.mAnimator.start();
        }

        private int calcOffsetX(int from, int to) {
            int leftFrom = getLeft(from);
            if (EffectPopup.this.mRecyclerView.findViewHolderForPosition(from) == null) {
                if (from < to) {
                    leftFrom = EffectPopup.this.mRecyclerView.getLeft();
                } else if (from > to) {
                    leftFrom = EffectPopup.this.mRecyclerView.getRight();
                }
            }
            return leftFrom - getLeft(to);
        }

        private int getLeft(int position) {
            ViewHolder holder = EffectPopup.this.mRecyclerView.findViewHolderForPosition(position);
            if (holder == null) {
                return 0;
            }
            View view = holder.itemView;
            return view.getLeft() + Math.round(ViewCompat.getTranslationX(view));
        }

        public void onDrawOver(Canvas canvas, RecyclerView parent, State state) {
            super.onDraw(canvas, parent, state);
            ViewHolder holder = EffectPopup.this.mRecyclerView.findViewHolderForPosition(this.mPosition);
            if (holder != null) {
                View view = holder.itemView;
                this.mOverlay.setBounds(this.mOffsetX + (view.getLeft() + Math.round(ViewCompat.getTranslationX(view))), view.getTop(), this.mOffsetX + (view.getRight() + Math.round(ViewCompat.getTranslationX(view))), view.getBottom());
                this.mOverlay.draw(canvas);
            }
        }
    }

    protected class EffectStillItemHolder extends EffectItemHolder {
        protected ImageView mImageView;

        public EffectStillItemHolder(View itemView) {
            super(itemView);
            this.mImageView = (ImageView) itemView.findViewById(R.id.effect_item_image);
        }

        public void bindEffectIndex(int index) {
            super.bindEffectIndex(index);
            if (index < EffectPopup.this.mPreference.getIconIds().length) {
                this.mImageView.setImageResource(EffectPopup.this.mPreference.getIconIds()[index]);
            }
        }
    }

    public EffectPopup(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void initialize(PreferenceGroup preferenceGroup, IconListPreference preference, MessageDispacher p) {
        super.initialize(preferenceGroup, preference, p);
        Context context = getContext();
        CharSequence[] entries = this.mPreference.getEntries();
        List<Map<String, Object>> effectItem = new ArrayList();
        for (CharSequence charSequence : entries) {
            HashMap<String, Object> map = new HashMap();
            map.put("text", charSequence.toString());
            effectItem.add(map);
        }
        this.mRecycler = (Recycler) ReflectUtil.getFieldValue(this.mRecyclerView.getClass(), this.mRecyclerView, "mRecycler", "");
        this.mTotalWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.mHolderWidth = context.getResources().getDimensionPixelSize(R.dimen.effect_item_width);
        this.mHolderHeight = context.getResources().getDimensionPixelSize(R.dimen.effect_item_height);
        this.mEffectItemAdapter = new EffectItemAdapter(context, effectItem);
        this.mLayoutManager = new LinearLayoutManager(context);
        this.mLayoutManager.setOrientation(0);
        this.mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, EffectController.getInstance().getEffectCount());
        this.mRecyclerView.setLayoutManager(this.mLayoutManager);
        this.mRecyclerView.addItemDecoration(new EffectItemPadding());
        this.mRecyclerView.addItemDecoration(new EffectDivider(EffectController.sDividerIndex));
        this.mSelectedOverlay = new EffectSelectedOverlay();
        this.mRecyclerView.addItemDecoration(this.mSelectedOverlay);
        this.mRecyclerView.setAdapter(this.mEffectItemAdapter);
        reloadPreference();
    }

    protected void setItemInCenter(int position) {
        this.mLayoutManager.scrollToPositionWithOffset(position, (this.mTotalWidth / 2) - (this.mHolderWidth / 2));
    }

    public void reloadPreference() {
        this.mCurrentIndex = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        if (this.mCurrentIndex != -1) {
            if (Device.isNeedForceRecycleEffectPopup() && this.mRecycler != null) {
                this.mLayoutManager.removeAndRecycleAllViews(this.mRecycler);
            }
            setItemInCenter(this.mCurrentIndex);
            this.mSelectedOverlay.select(this.mCurrentIndex);
            return;
        }
        Log.e("EffectPopup", "Invalid preference value.");
        this.mPreference.print();
    }

    public void onClick(View v) {
        if (this.mRecyclerView.isEnabled()) {
            int index = ((Integer) v.getTag()).intValue();
            if (this.mCurrentIndex != index || !this.mIgnoreSameItemClick) {
                boolean sameItem = this.mCurrentIndex == index;
                this.mCurrentIndex = index;
                this.mSelectedOverlay.select(this.mCurrentIndex);
                this.mPreference.setValueIndex(index);
                if ("pref_camera_scenemode_key".equals(this.mPreference.getKey())) {
                    CameraSettings.setFocusModeSwitching(true);
                } else if ("pref_audio_focus_key".equals(this.mPreference.getKey()) && ((ActivityBase) this.mContext).getCurrentModule().isVideoRecording()) {
                    AudioSystem.setParameters("camcorder_mode=" + this.mPreference.getValue());
                }
                EffectController.getInstance().setInvertFlag(0);
                ((ActivityBase) this.mContext).getUIController().getEffectCropView().updateVisible(this.mCurrentIndex);
                notifyToDispatcher(sameItem);
                if (this.mCurrentIndex != 0) {
                    CameraDataAnalytics.instance().trackEvent(this.mPreference.getKey());
                }
                AutoLockManager.getInstance(this.mContext).onUserInteraction();
            }
        }
    }

    protected void notifyToDispatcher(boolean sameItem) {
        if (this.mMessageDispacher != null) {
            this.mMessageDispacher.dispacherMessage(6, 0, 3, this.mPreference.getKey(), this);
        }
    }

    public void show(boolean animation) {
        super.show(animation);
        if ("pref_camera_scenemode_key".equals(this.mPreference.getKey()) && !"auto".equals(this.mPreference.getValue())) {
            CameraSettings.setFocusModeSwitching(true);
        }
    }

    protected void onFinishInflate() {
        super.onFinishInflate();
        this.mRecyclerView = (RecyclerView) findViewById(R.id.effect_list);
    }

    public void setOrientation(int orientation, boolean animation) {
    }

    public Animation getAnimation(boolean slideUp) {
        return AnimationUtils.loadAnimation(this.mContext, slideUp ? R.anim.effect_popup_slide_up : R.anim.effect_popup_slide_down);
    }

    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (this.mRecyclerView != null) {
            this.mRecyclerView.setEnabled(enabled);
        }
    }

    public void requestEffectRender() {
        for (int i = 0; i < this.mLayoutManager.getChildCount(); i++) {
            View view = this.mLayoutManager.getChildAt(i);
            if (view != null) {
                EffectItemHolder holder = (EffectItemHolder) this.mRecyclerView.getChildViewHolder(view);
                if (holder != null) {
                    holder.requestRender();
                }
            }
        }
    }

    public void startEffectRender() {
        CameraScreenNail screen = ((ActivityBase) this.mContext).getCameraScreenNail();
        int screenWidth = screen.getWidth();
        int screenHeight = screen.getHeight();
        this.mTextureOffsetX = 0;
        this.mTextureOffsetY = 0;
        this.mTextureWidth = this.mHolderWidth;
        this.mTextureHeight = this.mHolderHeight;
        if (this.mHolderWidth * screenHeight > this.mHolderHeight * screenWidth) {
            this.mTextureHeight = (this.mHolderWidth * screenHeight) / screenWidth;
            this.mTextureOffsetY = (-(this.mTextureHeight - this.mHolderHeight)) / 2;
        } else {
            this.mTextureWidth = (this.mHolderHeight * screenWidth) / screenHeight;
            this.mTextureOffsetX = (-(this.mTextureWidth - this.mHolderWidth)) / 2;
        }
        for (EffectItemHolder holder : this.mEffectItemHolderList) {
            holder.start();
        }
    }

    public void stopEffectRender() {
        for (EffectItemHolder holder : this.mEffectItemHolderList) {
            holder.stop();
        }
    }

    public void updateBackground() {
        if (((ActivityBase) this.mContext).getUIController().getPreviewFrame().isFullScreen()) {
            this.mRecyclerView.setBackgroundResource(R.color.effect_popup_fullscreen_background);
        } else {
            this.mRecyclerView.setBackgroundResource(R.color.effect_popup_halfscreen_background);
        }
        for (EffectItemHolder holder : this.mEffectItemHolderList) {
            holder.updateBackground();
        }
    }
}
