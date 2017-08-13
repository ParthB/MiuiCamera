package com.android.camera.ui;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorInflater;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.media.AudioSystem;
import android.opengl.GLSurfaceView.Renderer;
import android.support.v4.view.ViewCompat;
import android.support.v7.recyclerview.R;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.Recycler;
import android.support.v7.widget.RecyclerView.State;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.support.v7.widget.helper.ItemTouchHelper.SimpleCallback;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import com.android.camera.ActivityBase;
import com.android.camera.AutoLockManager;
import com.android.camera.CameraDataAnalytics;
import com.android.camera.CameraScreenNail;
import com.android.camera.CameraSettings;
import com.android.camera.Device;
import com.android.camera.effect.EffectController;
import com.android.camera.effect.draw_mode.DrawExtTexAttribute;
import com.android.camera.preferences.IconListPreference;
import com.android.camera.preferences.PreferenceGroup;
import com.android.gallery3d.ui.GLCanvas;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import miui.reflect.Field;
import miui.reflect.NoSuchFieldException;

public class EffectPopup extends V6AbstractSettingPopup implements OnClickListener, OnLongClickListener {
    private final String TAG = "EffectPopup";
    protected int mCurrentIndex = -1;
    protected int mCurrentPosition = -1;
    private EffectDragHelper mDragHelperGroup1;
    private EffectDragHelper mDragHelperGroup2;
    ViewHolder mDraggingHolder;
    private EffectItemAdapter mEffectItemAdapter;
    private AnimatorUpdateListener mEffectItemAnimUpdateListener = new AnimatorUpdateListener() {
        public void onAnimationUpdate(ValueAnimator valueAnimator) {
            if (valueAnimator == EffectPopup.this.mEffectItemSlideUpAnim || valueAnimator == EffectPopup.this.mEffectItemSlideDownAnim) {
                ((LayoutParams) EffectPopup.this.mRecyclerView.getLayoutParams()).setMargins(0, -((int) ((Float) valueAnimator.getAnimatedValue()).floatValue()), 0, 0);
                EffectPopup.this.mRecyclerView.requestLayout();
            }
            EffectPopup.this.mRecyclerView.postInvalidateOnAnimation();
        }
    };
    private ValueAnimator mEffectItemDragRecoverAnim;
    private List<EffectItemHolder> mEffectItemHolderList = new LinkedList();
    private ObjectAnimator mEffectItemSlideDownAnim;
    private AnimatorListener mEffectItemSlideDownAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            EffectPopup.this.mEffectItemSlideUpAnim.cancel();
            EffectPopup.this.mNoneEffectMask.hide();
        }

        public void onAnimationEnd(Animator animation) {
            super.onAnimationEnd(animation);
        }
    };
    private int mEffectItemSlideHeight;
    private ObjectAnimator mEffectItemSlideUpAnim;
    private AnimatorListener mEffectItemSlideUpAnimListener = new AnimatorListenerAdapter() {
        public void onAnimationStart(Animator animation) {
            super.onAnimationStart(animation);
            EffectPopup.this.mEffectItemSlideDownAnim.cancel();
            EffectPopup.this.mNoneEffectMask.show();
        }
    };
    private ArrayList<Integer> mEffectOrderMapIndexToPosition;
    private ArrayList<Integer> mEffectOrderMapPositionToIndex;
    private int mHolderHeight;
    private int mHolderWidth;
    protected boolean mIgnoreSameItemClick = true;
    private LinearLayoutManager mLayoutManager;
    private EffectItemMask mNoneEffectMask;
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
                int position = holder.itemView.getRight() + (this.mFrameWidth / 2);
                c.drawLine((float) position, (float) top, (float) position, (float) bottom, this.mPaint);
            }
        }

        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, State state) {
            if (parent.getChildPosition(view) == this.mPosition) {
                outRect.set(0, 0, this.mFrameWidth - this.mPadding, 0);
            }
        }
    }

    protected class EffectDragHelper extends ItemTouchHelper {
        protected int mLeftMostPosition;
        protected int mRightMostPosition;

        public EffectDragHelper(int leftMostPosition, int rightMostPosition) {
            super(new EffectDragHelperCallBack(leftMostPosition, rightMostPosition));
            this.mLeftMostPosition = leftMostPosition;
            this.mRightMostPosition = rightMostPosition;
        }

        public void startDrag(ViewHolder viewHolder) {
            super.startDrag(viewHolder);
        }

        public boolean contains(int position) {
            return position <= this.mRightMostPosition && position >= this.mLeftMostPosition;
        }
    }

    protected class EffectDragHelperCallBack extends SimpleCallback {
        protected int mLeftMostPosition;
        protected int mRightMostPosition;

        public EffectDragHelperCallBack(int leftMostPosition, int rightMostPosition) {
            super(12, 0);
            this.mLeftMostPosition = leftMostPosition;
            this.mRightMostPosition = rightMostPosition;
        }

        public void onSelectedChanged(ViewHolder viewHolder, int actionState) {
            super.onSelectedChanged(viewHolder, actionState);
            if (actionState == 2) {
                if (EffectPopup.this.mDraggingHolder == null) {
                    EffectPopup.this.mDraggingHolder = viewHolder;
                    EffectPopup.this.mEffectItemSlideUpAnim.setTarget(EffectPopup.this.mDraggingHolder);
                    EffectPopup.this.mEffectItemSlideUpAnim.start();
                }
            } else if (actionState == 0 && EffectPopup.this.mDraggingHolder != null) {
                EffectPopup.this.mEffectItemSlideDownAnim.setTarget(EffectPopup.this.mDraggingHolder);
                EffectPopup.this.mEffectItemSlideDownAnim.setFloatValues(new float[]{((EffectItemHolder) EffectPopup.this.mDraggingHolder).getTranslationY(), 0.0f});
                EffectPopup.this.mEffectItemSlideDownAnim.start();
                EffectPopup.this.mEffectItemDragRecoverAnim.cancel();
                EffectPopup.this.mDraggingHolder = null;
            }
        }

        public ViewHolder chooseDropTarget(ViewHolder selected, List<ViewHolder> dropTargets, int curX, int curY) {
            int right = curX + selected.itemView.getWidth();
            ViewHolder winner = null;
            int winnerScore = -1;
            int dx = curX - selected.itemView.getLeft();
            int targetsSize = dropTargets.size();
            for (int i = 0; i < targetsSize; i++) {
                int diff;
                int score;
                ViewHolder target = (ViewHolder) dropTargets.get(i);
                float threshold = getMoveThreshold(target);
                if (dx > 0) {
                    diff = target.itemView.getRight() - right;
                    if (((float) diff) < ((float) target.itemView.getWidth()) * threshold && target.itemView.getRight() > selected.itemView.getRight()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
                if (dx < 0) {
                    diff = target.itemView.getLeft() - curX;
                    if (((float) diff) > ((float) target.itemView.getWidth()) * (-threshold) && target.itemView.getLeft() < selected.itemView.getLeft()) {
                        score = Math.abs(diff);
                        if (score > winnerScore) {
                            winnerScore = score;
                            winner = target;
                        }
                    }
                }
            }
            return winner;
        }

        public void onChildDraw(Canvas c, RecyclerView recyclerView, ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            dX = 0.0f;
            if (!(EffectPopup.this.mDraggingHolder == null || EffectPopup.this.mEffectItemSlideUpAnim.isRunning())) {
                dY = (float) (-EffectPopup.this.mEffectItemSlideHeight);
            }
            if (EffectPopup.this.mDraggingHolder != null && EffectPopup.this.mEffectItemDragRecoverAnim.isRunning()) {
                dX = ((Float) EffectPopup.this.mEffectItemDragRecoverAnim.getAnimatedValue()).floatValue();
            }
            if (!isCurrentlyActive && EffectPopup.this.mEffectItemSlideDownAnim.isRunning()) {
                dY = ((Float) EffectPopup.this.mEffectItemSlideDownAnim.getAnimatedValue()).floatValue();
            }
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }

        public boolean canDropOver(RecyclerView recyclerView, ViewHolder current, ViewHolder target) {
            if (target.getAdapterPosition() < this.mLeftMostPosition || target.getAdapterPosition() > this.mRightMostPosition) {
                return false;
            }
            return super.canDropOver(recyclerView, current, target);
        }

        public boolean isLongPressDragEnabled() {
            return false;
        }

        public boolean isItemViewSwipeEnabled() {
            return false;
        }

        public int interpolateOutOfBoundsScroll(RecyclerView recyclerView, int viewSize, int viewSizeOutOfBounds, int totalSize, long msSinceStartScroll) {
            return 0;
        }

        public boolean onMove(RecyclerView recyclerView, ViewHolder viewHolder, ViewHolder target) {
            Collections.swap(EffectPopup.this.mEffectOrderMapPositionToIndex, viewHolder.getAdapterPosition(), target.getAdapterPosition());
            Collections.swap(EffectPopup.this.mEffectOrderMapIndexToPosition, ((Integer) EffectPopup.this.mEffectOrderMapPositionToIndex.get(viewHolder.getAdapterPosition())).intValue(), ((Integer) EffectPopup.this.mEffectOrderMapPositionToIndex.get(target.getAdapterPosition())).intValue());
            CameraSettings.saveShaderEffectPositionList(EffectPopup.this.mEffectOrderMapIndexToPosition);
            if (EffectPopup.this.mSelectedOverlay.mPosition == viewHolder.getAdapterPosition()) {
                EffectPopup.this.mSelectedOverlay.mPosition = target.getAdapterPosition();
            } else if (EffectPopup.this.mSelectedOverlay.mPosition == target.getAdapterPosition()) {
                EffectPopup.this.mSelectedOverlay.mPosition = viewHolder.getAdapterPosition();
            }
            EffectPopup.this.mEffectItemDragRecoverAnim.cancel();
            EffectPopup.this.mEffectItemDragRecoverAnim.setFloatValues(new float[]{(float) (viewHolder.itemView.getLeft() - target.itemView.getLeft()), 0.0f});
            EffectPopup.this.mEffectItemDragRecoverAnim.start();
            EffectPopup.this.mEffectItemAdapter.notifyItemMoved(viewHolder.getAdapterPosition(), target.getAdapterPosition());
            return true;
        }

        public void onMoved(RecyclerView recyclerView, ViewHolder viewHolder, int fromPos, ViewHolder target, int toPos, int x, int y) {
            if (fromPos < toPos) {
                super.onMoved(recyclerView, target, toPos, viewHolder, fromPos, x, y);
            } else {
                super.onMoved(recyclerView, viewHolder, fromPos, target, toPos, x, y);
            }
        }

        public void onSwiped(ViewHolder viewHolder, int direction) {
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

        public float getTranslationY() {
            return this.itemView.getTranslationY();
        }

        public void setTranslationY(float translationY) {
            this.itemView.setTranslationY(translationY);
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
            view.setOnLongClickListener(EffectPopup.this);
            EffectPopup.this.mEffectItemHolderList.add(holder);
            return holder;
        }

        public void onBindViewHolder(ViewHolder holder, int position) {
            int index = ((Integer) EffectPopup.this.mEffectOrderMapPositionToIndex.get(position)).intValue();
            EffectItemHolder effectHolder = (EffectItemHolder) holder;
            effectHolder.itemView.setTag(Integer.valueOf(index));
            effectHolder.bindEffectIndex(index);
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

    protected class EffectItemMask extends ItemDecoration {
        protected int mAlpha;
        protected AnimatorListener mAnimatorListener = new AnimatorListenerAdapter() {
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (animation == EffectItemMask.this.mFadeOutAnimator) {
                    EffectItemMask.this.mVisible = false;
                }
            }

            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (animation == EffectItemMask.this.mFadeInAnimator) {
                    EffectItemMask.this.mVisible = true;
                }
            }
        };
        protected ObjectAnimator mFadeInAnimator;
        protected ObjectAnimator mFadeOutAnimator;
        protected Paint mPaint;
        protected int mPosition;
        protected boolean mVisible;

        public EffectItemMask(int position) {
            this.mFadeInAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(EffectPopup.this.mContext, R.anim.effect_item_mask_fade_in);
            this.mFadeInAnimator.setTarget(this);
            this.mFadeInAnimator.setAutoCancel(true);
            this.mFadeInAnimator.addListener(this.mAnimatorListener);
            this.mFadeOutAnimator = (ObjectAnimator) AnimatorInflater.loadAnimator(EffectPopup.this.mContext, R.anim.effect_item_mask_fade_out);
            this.mFadeOutAnimator.setTarget(this);
            this.mFadeOutAnimator.setAutoCancel(true);
            this.mFadeOutAnimator.addListener(this.mAnimatorListener);
            this.mPaint = new Paint();
            this.mPaint.setStyle(Style.FILL);
            this.mPaint.setColor(-16777216);
            this.mPosition = position;
            this.mVisible = false;
        }

        public void setAlpha(float alpha) {
            this.mAlpha = (int) (255.0f * alpha);
            EffectPopup.this.mRecyclerView.postInvalidateOnAnimation();
        }

        public void show() {
            this.mFadeInAnimator.start();
        }

        public void hide() {
            this.mFadeOutAnimator.start();
        }

        public void onDrawOver(Canvas c, RecyclerView parent, State state) {
            super.onDrawOver(c, parent, state);
            ViewHolder holder = parent.findViewHolderForPosition(this.mPosition);
            if (this.mVisible && holder != null) {
                View view = holder.itemView;
                this.mPaint.setAlpha(this.mAlpha);
                c.drawRect((float) view.getLeft(), (float) view.getTop(), (float) view.getRight(), (float) view.getBottom(), this.mPaint);
            }
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
            super.onDrawOver(canvas, parent, state);
            ViewHolder holder = EffectPopup.this.mRecyclerView.findViewHolderForPosition(this.mPosition);
            if (holder != null) {
                View view = holder.itemView;
                this.mOverlay.setBounds(this.mOffsetX + (view.getLeft() + Math.round(ViewCompat.getTranslationX(view))), view.getTop() + Math.round(ViewCompat.getTranslationY(view)), this.mOffsetX + (view.getRight() + Math.round(ViewCompat.getTranslationX(view))), view.getBottom() + Math.round(ViewCompat.getTranslationY(view)));
                canvas.save();
                ViewHolder draggingHolder = null;
                if (EffectPopup.this.mDraggingHolder != null) {
                    draggingHolder = EffectPopup.this.mDraggingHolder;
                } else if (EffectPopup.this.mEffectItemSlideDownAnim.isRunning()) {
                    draggingHolder = (ViewHolder) EffectPopup.this.mEffectItemSlideDownAnim.getTarget();
                }
                if (!(draggingHolder == null || draggingHolder.getAdapterPosition() == this.mPosition)) {
                    View draggingView = draggingHolder.itemView;
                    Canvas canvas2 = canvas;
                    canvas2.clipRect(ViewCompat.getTranslationX(draggingView) + ((float) draggingView.getLeft()), ViewCompat.getTranslationY(draggingView) + ((float) draggingView.getTop()), ViewCompat.getTranslationX(draggingView) + ((float) draggingView.getRight()), ViewCompat.getTranslationY(draggingView) + ((float) draggingView.getBottom()), Op.XOR);
                }
                this.mOverlay.draw(canvas);
                canvas.restore();
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
        try {
            this.mRecycler = (Recycler) Field.of(RecyclerView.class, "mRecycler", Recycler.class).get(this.mRecyclerView);
        } catch (NoSuchFieldException e) {
            Log.e("EffectPopup", "no mRecycler field ", e);
        }
        reloadEffectPosition();
        this.mTotalWidth = context.getResources().getDisplayMetrics().widthPixels;
        this.mHolderWidth = context.getResources().getDimensionPixelSize(R.dimen.effect_item_width);
        this.mHolderHeight = context.getResources().getDimensionPixelSize(R.dimen.effect_item_height);
        this.mEffectItemSlideHeight = context.getResources().getDimensionPixelSize(R.dimen.effect_item_text_height);
        this.mEffectItemSlideUpAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this.mContext, R.anim.effect_item_slide_up);
        this.mEffectItemSlideUpAnim.setFloatValues(new float[]{0.0f, (float) (-this.mEffectItemSlideHeight)});
        this.mEffectItemSlideUpAnim.addListener(this.mEffectItemSlideUpAnimListener);
        this.mEffectItemSlideUpAnim.addUpdateListener(this.mEffectItemAnimUpdateListener);
        this.mEffectItemSlideDownAnim = (ObjectAnimator) AnimatorInflater.loadAnimator(this.mContext, R.anim.effect_item_slide_down);
        this.mEffectItemSlideDownAnim.setFloatValues(new float[]{(float) (-this.mEffectItemSlideHeight), 0.0f});
        this.mEffectItemSlideDownAnim.addListener(this.mEffectItemSlideDownAnimListener);
        this.mEffectItemSlideDownAnim.addUpdateListener(this.mEffectItemAnimUpdateListener);
        this.mEffectItemDragRecoverAnim = (ValueAnimator) AnimatorInflater.loadAnimator(this.mContext, R.anim.effect_item_drag_recover);
        this.mEffectItemDragRecoverAnim.addUpdateListener(this.mEffectItemAnimUpdateListener);
        this.mEffectItemAdapter = new EffectItemAdapter(context, effectItem);
        this.mLayoutManager = new LinearLayoutManager(context);
        this.mLayoutManager.setOrientation(0);
        this.mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, EffectController.getInstance().getEffectCount());
        if (Device.IS_C8) {
            this.mRecyclerView.setItemViewCacheSize(0);
        }
        this.mRecyclerView.setLayoutManager(this.mLayoutManager);
        this.mRecyclerView.addItemDecoration(new EffectItemPadding());
        this.mRecyclerView.addItemDecoration(new EffectDivider(EffectController.sDividerIndex));
        this.mNoneEffectMask = new EffectItemMask(0);
        this.mRecyclerView.addItemDecoration(this.mNoneEffectMask);
        this.mSelectedOverlay = new EffectSelectedOverlay();
        this.mRecyclerView.addItemDecoration(this.mSelectedOverlay);
        this.mDragHelperGroup1 = new EffectDragHelper(1, EffectController.sDividerIndex);
        this.mDragHelperGroup1.attachToRecyclerView(this.mRecyclerView);
        this.mDragHelperGroup2 = new EffectDragHelper(EffectController.sDividerIndex + 1, EffectController.getInstance().getEffectCount() - 1);
        this.mDragHelperGroup2.attachToRecyclerView(this.mRecyclerView);
        this.mRecyclerView.setAdapter(this.mEffectItemAdapter);
        reloadPreference();
    }

    protected void setItemInCenter(int position) {
        this.mLayoutManager.scrollToPositionWithOffset(position, (this.mTotalWidth / 2) - (this.mHolderWidth / 2));
    }

    public void reloadEffectPosition() {
        this.mEffectOrderMapIndexToPosition = new ArrayList(CameraSettings.getShaderEffectPositionList());
        this.mEffectOrderMapPositionToIndex = new ArrayList(Collections.nCopies(this.mEffectOrderMapIndexToPosition.size(), Integer.valueOf(0)));
        for (int i = 0; i < this.mEffectOrderMapIndexToPosition.size(); i++) {
            this.mEffectOrderMapPositionToIndex.set(((Integer) this.mEffectOrderMapIndexToPosition.get(i)).intValue(), Integer.valueOf(i));
        }
    }

    public void restorePreference() {
        if (!(this.mEffectOrderMapIndexToPosition == null || this.mEffectOrderMapPositionToIndex == null)) {
            reloadEffectPosition();
        }
        if (this.mLayoutManager != null && this.mRecycler != null) {
            this.mLayoutManager.removeAndRecycleAllViews(this.mRecycler);
            this.mRecycler.clear();
        }
    }

    public void reloadPreference() {
        this.mCurrentIndex = this.mPreference.findIndexOfValue(this.mPreference.getValue());
        if (this.mCurrentIndex != -1) {
            if (Device.isNeedForceRecycleEffectPopup() && this.mRecycler != null) {
                this.mLayoutManager.removeAndRecycleAllViews(this.mRecycler);
            }
            this.mCurrentPosition = ((Integer) this.mEffectOrderMapIndexToPosition.get(this.mCurrentIndex)).intValue();
            setItemInCenter(this.mCurrentPosition);
            this.mSelectedOverlay.select(this.mCurrentPosition);
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
                this.mCurrentPosition = ((Integer) this.mEffectOrderMapIndexToPosition.get(this.mCurrentIndex)).intValue();
                this.mSelectedOverlay.select(this.mCurrentPosition);
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

    public boolean onLongClick(View view) {
        if (!this.mRecyclerView.isEnabled()) {
            return false;
        }
        int position = ((Integer) this.mEffectOrderMapIndexToPosition.get(((Integer) view.getTag()).intValue())).intValue();
        ViewHolder holder = this.mRecyclerView.findViewHolderForPosition(position);
        if (this.mDragHelperGroup1.contains(position)) {
            this.mDragHelperGroup1.startDrag(holder);
        } else if (!this.mDragHelperGroup2.contains(position)) {
            return false;
        } else {
            this.mDragHelperGroup2.startDrag(holder);
        }
        return true;
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
        int renderWidth = screen.getRenderWidth();
        int renderHeight = screen.getRenderHeight();
        this.mTextureOffsetX = 0;
        this.mTextureOffsetY = 0;
        this.mTextureWidth = this.mHolderWidth;
        this.mTextureHeight = this.mHolderHeight;
        if (this.mHolderWidth * renderHeight > this.mHolderHeight * renderWidth) {
            this.mTextureHeight = (this.mHolderWidth * renderHeight) / renderWidth;
            this.mTextureOffsetY = (-(this.mTextureHeight - this.mHolderHeight)) / 2;
        } else {
            this.mTextureWidth = (this.mHolderHeight * renderWidth) / renderHeight;
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
