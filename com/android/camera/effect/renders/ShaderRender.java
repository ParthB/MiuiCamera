package com.android.camera.effect.renders;

import android.opengl.GLES20;
import android.util.Log;
import com.android.camera.effect.ShaderUtil;
import com.android.camera.effect.draw_mode.DrawAttribute;
import com.android.gallery3d.ui.BasicTexture;
import com.android.gallery3d.ui.GLCanvas;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public abstract class ShaderRender extends Render {
    private static final String VERTEX = ShaderUtil.loadFromAssetsFile("vertex_normal.txt");
    protected ArrayList<Integer> mAttriSupportedList = new ArrayList();
    protected int mAttributePositionH;
    protected int mAttributeTexCoorH;
    protected boolean mBlendEnabled = true;
    protected float[] mPreviewEffectRect = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    protected int mProgram = 0;
    protected float[] mSnapshotEffectRect = new float[]{0.0f, 0.0f, 1.0f, 1.0f};
    protected FloatBuffer mTexCoorBuffer;
    protected int mUniformAlphaH;
    protected int mUniformBlendAlphaH;
    protected int mUniformMVPMatrixH;
    protected int mUniformSTMatrixH;
    protected int mUniformTextureH;
    protected FloatBuffer mVertexBuffer;

    public abstract boolean draw(DrawAttribute drawAttribute);

    public abstract String getFragShaderString();

    protected abstract void initShader();

    protected abstract void initSupportAttriList();

    protected abstract void initVertexData();

    public ShaderRender(GLCanvas canvas) {
        super(canvas);
        initShader();
        initVertexData();
        initSupportAttriList();
    }

    public ShaderRender(GLCanvas canvas, int id) {
        super(canvas, id);
        initShader();
        initVertexData();
        initSupportAttriList();
    }

    public static ByteBuffer allocateByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    protected boolean bindTexture(BasicTexture texture, int texure) {
        if (!texture.onBind(this.mGLCanvas)) {
            return false;
        }
        GLES20.glActiveTexture(texure);
        GLES20.glBindTexture(texture.getTarget(), 0);
        GLES20.glBindTexture(texture.getTarget(), texture.getId());
        return true;
    }

    protected boolean bindTexture(int textureId, int texure) {
        GLES20.glActiveTexture(texure);
        GLES20.glBindTexture(3553, 0);
        GLES20.glBindTexture(3553, textureId);
        return true;
    }

    protected void setBlendEnabled(boolean enable) {
        setBlendEnabled(enable, false);
    }

    protected void setBlendEnabled(boolean enabled, boolean premultiplied) {
        if (enabled) {
            GLES20.glEnable(3042);
            GLES20.glBlendFunc(premultiplied ? 1 : 770, 771);
            return;
        }
        GLES20.glDisable(3042);
    }

    public boolean isAttriSupported(int attri) {
        return this.mAttriSupportedList.contains(Integer.valueOf(attri));
    }

    public String getVertexShaderString() {
        return VERTEX;
    }

    protected void finalize() throws Throwable {
        if (!(this.mProgram == 0 || this.mGLCanvas == null)) {
            Log.d("Camera", "delete mProgram = " + this.mProgram);
            this.mGLCanvas.deleteProgram(this.mProgram);
            this.mProgram = 0;
        }
        super.finalize();
    }
}
