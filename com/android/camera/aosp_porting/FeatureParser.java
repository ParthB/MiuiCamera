package com.android.camera.aosp_porting;

import java.util.ArrayList;
import java.util.HashMap;

public class FeatureParser {
    private static HashMap<String, Boolean> sBooleanMap = new HashMap();
    private static HashMap<String, Float> sFloatMap = new HashMap();
    private static HashMap<String, ArrayList<Integer>> sIntArrMap = new HashMap();
    private static HashMap<String, Integer> sIntMap = new HashMap();
    private static HashMap<String, ArrayList<String>> sStrArrMap = new HashMap();
    private static HashMap<String, String> sStrMap = new HashMap();

    public static boolean getBoolean(String name, boolean defaultValue) {
        Boolean value = (Boolean) sBooleanMap.get(name);
        if (value != null) {
            return value.booleanValue();
        }
        return defaultValue;
    }

    public static String getString(String name) {
        return (String) sStrMap.get(name);
    }

    public static int getInteger(String name, int defaultValue) {
        Integer value = (Integer) sIntMap.get(name);
        if (value != null) {
            return value.intValue();
        }
        return defaultValue;
    }

    public static String[] getStringArray(String name) {
        ArrayList<String> strList = (ArrayList) sStrArrMap.get(name);
        if (strList != null) {
            return (String[]) strList.toArray(new String[0]);
        }
        return null;
    }

    public static Float getFloat(String name, float defaultValue) {
        Float value = (Float) sFloatMap.get(name);
        if (value != null) {
            defaultValue = value.floatValue();
        }
        return Float.valueOf(defaultValue);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static void read(android.content.Context r17) {
        /*
        r6 = 0;
        r5 = 0;
        r14 = "cancro";
        r15 = com.android.camera.aosp_porting.Build.DEVICE;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = r14.equals(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x007d;
    L_0x000d:
        r14 = com.android.camera.aosp_porting.Build.MODEL;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = "MI 3";
        r14 = r14.startsWith(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x006e;
    L_0x0018:
        r5 = "cancro_MI3.xml";
    L_0x001b:
        r14 = r17.getAssets();	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = "device_features/";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r15.append(r5);	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r6 = r14.open(r15);	 Catch:{ IOException -> 0x0094, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r7 = r6;
    L_0x0038:
        if (r7 != 0) goto L_0x01f9;
    L_0x003a:
        r4 = new java.io.File;	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r14 = "/system/etc/device_features";
        r4.<init>(r14, r5);	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r14 = r4.exists();	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        if (r14 == 0) goto L_0x00ce;
    L_0x0048:
        r6 = new java.io.FileInputStream;	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r6.<init>(r4);	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
    L_0x004d:
        r3 = org.xmlpull.v1.XmlPullParserFactory.newInstance();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r10 = r3.newPullParser();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = "UTF-8";
        r10.setInput(r6, r14);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r13 = r10.getEventType();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r12 = 0;
        r9 = 0;
        r8 = 0;
        r11 = 0;
    L_0x0063:
        r14 = 1;
        if (r14 == r13) goto L_0x01df;
    L_0x0066:
        switch(r13) {
            case 2: goto L_0x00f0;
            case 3: goto L_0x01b9;
            default: goto L_0x0069;
        };	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
    L_0x0069:
        r13 = r10.next();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0063;
    L_0x006e:
        r14 = com.android.camera.aosp_porting.Build.MODEL;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = "MI 4";
        r14 = r14.startsWith(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x001b;
    L_0x0079:
        r5 = "cancro_MI4.xml";
        goto L_0x001b;
    L_0x007d:
        r14 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.<init>();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = com.android.camera.aosp_porting.Build.DEVICE;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = r14.append(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = ".xml";
        r14 = r14.append(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r5 = r14.toString();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x001b;
    L_0x0094:
        r0 = move-exception;
        r14 = "FeatureParser";
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15.<init>();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = "can't find ";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r15.append(r5);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = " in assets/";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = "device_features/";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = ",it may be in ";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r16 = "/system/etc/device_features";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        android.util.Log.i(r14, r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r7 = r6;
        goto L_0x0038;
    L_0x00ce:
        r14 = "FeatureParser";
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r15.<init>();	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r16 = "both assets/device_features/ and /system/etc/device_features don't exist ";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r15 = r15.append(r5);	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        r15 = r15.toString();	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        android.util.Log.e(r14, r15);	 Catch:{ IOException -> 0x01f1, XmlPullParserException -> 0x01f5, all -> 0x01ee }
        if (r7 == 0) goto L_0x00ed;
    L_0x00ea:
        r7.close();	 Catch:{ IOException -> 0x00ee }
    L_0x00ed:
        return;
    L_0x00ee:
        r0 = move-exception;
        goto L_0x00ed;
    L_0x00f0:
        r12 = r10.getName();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = r10.getAttributeCount();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 <= 0) goto L_0x00ff;
    L_0x00fa:
        r14 = 0;
        r9 = r10.getAttributeValue(r14);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
    L_0x00ff:
        r14 = "integer-array";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x010f;
    L_0x0108:
        r8 = new java.util.ArrayList;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r8.<init>();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x010f:
        r14 = "string-array";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x011f;
    L_0x0118:
        r11 = new java.util.ArrayList;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r11.<init>();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x011f:
        r14 = "bool";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x013e;
    L_0x0128:
        r14 = sBooleanMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = java.lang.Boolean.valueOf(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x0137:
        r0 = move-exception;
    L_0x0138:
        if (r6 == 0) goto L_0x013d;
    L_0x013a:
        r6.close();	 Catch:{ IOException -> 0x01e9 }
    L_0x013d:
        return;
    L_0x013e:
        r14 = "integer";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x015f;
    L_0x0147:
        r14 = sIntMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = java.lang.Integer.valueOf(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x0156:
        r1 = move-exception;
    L_0x0157:
        if (r6 == 0) goto L_0x013d;
    L_0x0159:
        r6.close();	 Catch:{ IOException -> 0x015d }
        goto L_0x013d;
    L_0x015d:
        r0 = move-exception;
        goto L_0x013d;
    L_0x015f:
        r14 = "string";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x017a;
    L_0x0168:
        r14 = sStrMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x0173:
        r14 = move-exception;
    L_0x0174:
        if (r6 == 0) goto L_0x0179;
    L_0x0176:
        r6.close();	 Catch:{ IOException -> 0x01ec }
    L_0x0179:
        throw r14;
    L_0x017a:
        r14 = "float";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x0196;
    L_0x0183:
        r14 = sFloatMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = java.lang.Float.parseFloat(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r15 = java.lang.Float.valueOf(r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r15);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x0196:
        r14 = "item";
        r14 = r14.equals(r12);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x0069;
    L_0x019f:
        if (r8 == 0) goto L_0x01ae;
    L_0x01a1:
        r14 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = java.lang.Integer.valueOf(r14);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r8.add(r14);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x01ae:
        if (r11 == 0) goto L_0x0069;
    L_0x01b0:
        r14 = r10.nextText();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r11.add(r14);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        goto L_0x0069;
    L_0x01b9:
        r2 = r10.getName();	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14 = "integer-array";
        r14 = r14.equals(r2);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x01ce;
    L_0x01c6:
        r14 = sIntArrMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r8);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r8 = 0;
        goto L_0x0069;
    L_0x01ce:
        r14 = "string-array";
        r14 = r14.equals(r2);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        if (r14 == 0) goto L_0x0069;
    L_0x01d7:
        r14 = sStrArrMap;	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r14.put(r9, r11);	 Catch:{ IOException -> 0x0137, XmlPullParserException -> 0x0156, all -> 0x0173 }
        r11 = 0;
        goto L_0x0069;
    L_0x01df:
        if (r6 == 0) goto L_0x013d;
    L_0x01e1:
        r6.close();	 Catch:{ IOException -> 0x01e6 }
        goto L_0x013d;
    L_0x01e6:
        r0 = move-exception;
        goto L_0x013d;
    L_0x01e9:
        r0 = move-exception;
        goto L_0x013d;
    L_0x01ec:
        r0 = move-exception;
        goto L_0x0179;
    L_0x01ee:
        r14 = move-exception;
        r6 = r7;
        goto L_0x0174;
    L_0x01f1:
        r0 = move-exception;
        r6 = r7;
        goto L_0x0138;
    L_0x01f5:
        r1 = move-exception;
        r6 = r7;
        goto L_0x0157;
    L_0x01f9:
        r6 = r7;
        goto L_0x004d;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.aosp_porting.FeatureParser.read(android.content.Context):void");
    }
}
