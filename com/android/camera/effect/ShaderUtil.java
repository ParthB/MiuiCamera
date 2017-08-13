package com.android.camera.effect;

import android.opengl.GLES20;
import android.util.Log;
import com.android.camera.CameraAppImpl;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ShaderUtil {
    public static int loadShader(int shaderType, String source) {
        int shader = GLES20.glCreateShader(shaderType);
        if (shader == 0) {
            return shader;
        }
        GLES20.glShaderSource(shader, source);
        GLES20.glCompileShader(shader);
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, 35713, compiled, 0);
        if (compiled[0] != 0) {
            return shader;
        }
        Log.e("Camera_ShaderUtil", "Could not compile shader " + shaderType + ":" + source);
        Log.e("Camera_ShaderUtil", "Info: " + GLES20.glGetShaderInfoLog(shader));
        GLES20.glDeleteShader(shader);
        return 0;
    }

    public static int createProgram(String vertexSource, String fragmentSource) {
        int vertexShader = loadShader(35633, vertexSource);
        if (vertexShader == 0) {
            Log.e("Camera_ShaderUtil", "Fail to init vertex shader " + vertexSource);
            return 0;
        }
        int pixelShader = loadShader(35632, fragmentSource);
        if (pixelShader == 0) {
            Log.e("Camera_ShaderUtil", "Fail to init fragment shader " + fragmentSource);
            return 0;
        }
        int program = GLES20.glCreateProgram();
        if (program != 0) {
            GLES20.glGetError();
            GLES20.glAttachShader(program, vertexShader);
            checkGlError("glAttachShader");
            GLES20.glAttachShader(program, pixelShader);
            checkGlError("glAttachShader");
            GLES20.glLinkProgram(program);
            int[] linkStatus = new int[1];
            GLES20.glGetProgramiv(program, 35714, linkStatus, 0);
            if (linkStatus[0] != 1) {
                Log.e("Camera_ShaderUtil", "Could not link program: ");
                Log.e("Camera_ShaderUtil", GLES20.glGetProgramInfoLog(program));
                GLES20.glDeleteProgram(program);
                program = 0;
            }
        }
        return program;
    }

    public static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != 0) {
            Log.e("Camera_ShaderUtil", "ES20_ERROR: op " + op + ": glError " + error);
            throw new RuntimeException(op + ": glError " + error);
        }
    }

    public static String loadFromAssetsFile(String fileName) {
        Exception e;
        String result = null;
        try {
            InputStream in = CameraAppImpl.getAndroidContext().getAssets().open("shading_script/" + fileName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while (true) {
                int ch = in.read();
                if (ch == -1) {
                    break;
                }
                baos.write(ch);
            }
            byte[] buff = baos.toByteArray();
            baos.close();
            in.close();
            String result2 = new String(buff, "UTF-8");
            try {
                result = result2.replaceAll("\\r\\n", "\n");
            } catch (Exception e2) {
                e = e2;
                result = result2;
                e.printStackTrace();
                return result;
            }
        } catch (Exception e3) {
            e = e3;
            e.printStackTrace();
            return result;
        }
        return result;
    }
}
