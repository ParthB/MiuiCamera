package miui.external;

import android.content.Context;
import android.os.Build.VERSION;
import dalvik.system.BaseDexClassLoader;
import dalvik.system.DexClassLoader;
import dalvik.system.PathClassLoader;
import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

/* compiled from: SdkLoader */
class e {
    e() {
    }

    public static boolean F(String str, String str2, String str3, ClassLoader classLoader) {
        return G(str, str2, str3, classLoader, null);
    }

    static boolean G(String str, String str2, String str3, ClassLoader classLoader, Context context) {
        if (str == null && (str3 == null || context == null)) {
            return false;
        }
        try {
            String str4;
            ClassLoader pathClassLoader;
            Object C = C(classLoader);
            if (str != null) {
                str4 = str;
            } else if (VERSION.SDK_INT < 23) {
                I(C, str3);
                return true;
            } else {
                str2 = null;
                str4 = context.getApplicationInfo().sourceDir;
            }
            if (str2 == null) {
                pathClassLoader = new PathClassLoader(str4, str3, classLoader.getParent());
            } else {
                pathClassLoader = new DexClassLoader(str4, str2, str3, classLoader.getParent());
            }
            Object C2 = C(pathClassLoader);
            if (str != null) {
                H(C, C2);
            }
            if (str3 != null) {
                J(C, C2, str3);
            }
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        } catch (IllegalAccessException e2) {
            return false;
        } catch (ClassNotFoundException e3) {
            return false;
        } catch (NoSuchFieldException e4) {
            return false;
        }
    }

    private static Object C(ClassLoader classLoader) throws NoSuchFieldException {
        if (classLoader instanceof BaseDexClassLoader) {
            Field[] declaredFields = BaseDexClassLoader.class.getDeclaredFields();
            int i = 0;
            int length = declaredFields.length;
            while (i < length) {
                Field field = declaredFields[i];
                if ("dalvik.system.DexPathList".equals(field.getType().getName())) {
                    field.setAccessible(true);
                    try {
                        return field.get(classLoader);
                    } catch (IllegalArgumentException e) {
                    } catch (IllegalAccessException e2) {
                    }
                } else {
                    i++;
                }
            }
        }
        throw new NoSuchFieldException("dexPathList field not found.");
    }

    private static void H(Object obj, Object obj2) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        K(obj, obj2, "dexElements");
    }

    private static void J(Object obj, Object obj2, String str) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        if (VERSION.SDK_INT >= 23) {
            K(obj, obj2, "nativeLibraryPathElements");
        } else {
            I(obj, str);
        }
    }

    private static void K(Object obj, Object obj2, String str) throws NoSuchFieldException, IllegalAccessException, ClassNotFoundException {
        Object[] objArr = (Object[]) D(obj2, str).get(obj2);
        Field D = D(obj, str);
        Object[] objArr2 = (Object[]) D.get(obj);
        Object[] objArr3 = (Object[]) Array.newInstance(Class.forName("dalvik.system.DexPathList$Element"), objArr2.length + 1);
        objArr3[0] = objArr[0];
        System.arraycopy(objArr2, 0, objArr3, 1, objArr2.length);
        D.set(obj, objArr3);
    }

    private static Field D(Object obj, String str) throws NoSuchFieldException {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.getName().equals(str)) {
                Class type = field.getType();
                if (type.isArray() && "dalvik.system.DexPathList$Element".equals(type.getComponentType().getName())) {
                    field.setAccessible(true);
                    return field;
                }
            }
        }
        throw new NoSuchFieldException(str + " field not found.");
    }

    private static void I(Object obj, String str) throws NoSuchFieldException, IllegalAccessException {
        Field E = E(obj);
        File[] fileArr = (File[]) E.get(obj);
        Object obj2 = new File[(fileArr.length + 1)];
        obj2[0] = new File(str);
        System.arraycopy(fileArr, 0, obj2, 1, fileArr.length);
        E.set(obj, obj2);
    }

    private static Field E(Object obj) throws NoSuchFieldException {
        for (Field field : obj.getClass().getDeclaredFields()) {
            Class type = field.getType();
            if (type.isArray() && type.getComponentType() == File.class) {
                field.setAccessible(true);
                return field;
            }
        }
        throw new NoSuchFieldException("nativeLibraryDirectories field not found.");
    }
}
