package com.android.camera.aosp_porting;

import android.util.Log;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectUtil {
    private static Map<Character, Class> BASIC_TYPES = new HashMap();

    static {
        BASIC_TYPES.put(Character.valueOf('V'), Void.TYPE);
        BASIC_TYPES.put(Character.valueOf('Z'), Boolean.TYPE);
        BASIC_TYPES.put(Character.valueOf('B'), Byte.TYPE);
        BASIC_TYPES.put(Character.valueOf('C'), Character.TYPE);
        BASIC_TYPES.put(Character.valueOf('S'), Short.TYPE);
        BASIC_TYPES.put(Character.valueOf('I'), Integer.TYPE);
        BASIC_TYPES.put(Character.valueOf('J'), Long.TYPE);
        BASIC_TYPES.put(Character.valueOf('F'), Float.TYPE);
        BASIC_TYPES.put(Character.valueOf('D'), Double.TYPE);
    }

    public static Object callMethod(Class<?> clazz, Object object, String methodName, String signature, Object... params) {
        try {
            Method method = clazz.getMethod(methodName, parseTypesFromSignature(signature));
            method.setAccessible(true);
            return method.invoke(object, params);
        } catch (NoSuchMethodException e) {
            Log.w("Camera", "ReflectUtil#callMethod ", e);
            return null;
        } catch (InvocationTargetException e2) {
            Log.w("Camera", "ReflectUtil#callMethod ", e2);
            return null;
        } catch (IllegalAccessException e3) {
            Log.w("Camera", "ReflectUtil#callMethod ", e3);
            return null;
        } catch (ClassNotFoundException e4) {
            Log.w("Camera", "ReflectUtil#callMethod ", e4);
            return null;
        }
    }

    private static Class<?>[] parseTypesFromSignature(String signature) throws ClassNotFoundException {
        if (signature == null || signature == "") {
            return null;
        }
        String params = signature.substring(signature.indexOf(40) + 1, signature.indexOf(41));
        if (params == null || params == "") {
            return null;
        }
        int i;
        List<Class<?>> types = new ArrayList();
        int referenceStart = -1;
        int referenceEnd = -1;
        boolean arrayFound = false;
        for (i = 0; i < params.length(); i++) {
            char ch = params.charAt(i);
            if (referenceStart < 0 && BASIC_TYPES.containsKey(Character.valueOf(ch))) {
                if (arrayFound) {
                    types.add(Array.newInstance((Class) BASIC_TYPES.get(Character.valueOf(ch)), 0).getClass());
                } else {
                    types.add((Class) BASIC_TYPES.get(Character.valueOf(ch)));
                }
                arrayFound = false;
            } else if (ch == '[') {
                arrayFound = true;
            } else if (ch == 'L') {
                if (referenceEnd == -1 && referenceStart == -1) {
                    referenceStart = i;
                }
            } else if (ch == ';') {
                referenceEnd = i;
                String name = params.substring(referenceStart + 1, i).replaceAll("/", ".");
                if (arrayFound) {
                    types.add(Array.newInstance(Class.forName(name), 0).getClass());
                } else {
                    types.add(Class.forName(name));
                }
                arrayFound = false;
                referenceStart = -1;
                referenceEnd = -1;
            }
        }
        Class<?>[] typesArray = new Class[types.size()];
        for (i = 0; i < types.size(); i++) {
            typesArray[i] = (Class) types.get(i);
        }
        return typesArray;
    }

    public static Object getFieldValue(Class<?> clazz, Object object, String fieldName, String signature) {
        Field field;
        try {
            field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(object);
        } catch (NoSuchFieldException e) {
            try {
                field = clazz.getField(fieldName);
                field.setAccessible(true);
                return field.get(object);
            } catch (NoSuchFieldException e1) {
                Log.w("Camera", "ReflectUtil#getFieldValue ", e1);
                return null;
            } catch (IllegalAccessException e12) {
                Log.w("Camera", "ReflectUtil#getFieldValue ", e12);
                return null;
            }
        } catch (IllegalAccessException e2) {
            Log.w("Camera", "ReflectUtil#getFieldValue ", e2);
            return null;
        }
    }

    public static int getFieldInt(Class<?> clazz, Object object, String fieldName, int defaultValue) {
        Object value = getFieldValue(clazz, object, fieldName, "I");
        return value == null ? defaultValue : ((Integer) value).intValue();
    }
}
