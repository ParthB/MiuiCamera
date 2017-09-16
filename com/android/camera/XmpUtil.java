package com.android.camera;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.options.SerializeOptions;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class XmpUtil {

    private static class Section {
        public byte[] data;
        public int length;
        public int marker;

        private Section() {
        }
    }

    static {
        try {
            XMPMetaFactory.getSchemaRegistry().registerNamespace("http://ns.google.com/photos/1.0/camera/", "GCamera");
        } catch (XMPException e) {
            e.printStackTrace();
        }
    }

    public static void addSpecialTypeMeta(String path) {
        XMPMeta meta = extractOrCreateXMPMeta(path);
        try {
            meta.setProperty("http://ns.google.com/photos/1.0/camera/", "SpecialTypeID", "PORTRAIT_TYPE");
        } catch (XMPException e) {
            Log.d("XmpUtil", "got exception when set metadata ", e);
        }
        writeXMPMeta(path, meta);
    }

    public static XMPMeta extractXMPMeta(String filename) {
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            try {
                return extractXMPMeta(new FileInputStream(filename));
            } catch (FileNotFoundException e) {
                Log.e("XmpUtil", "Could not read file: " + filename, e);
                return null;
            }
        }
        Log.d("XmpUtil", "XMP parse: only jpeg file is supported");
        return null;
    }

    public static XMPMeta extractXMPMeta(InputStream is) {
        List<Section> sections = parse(is, true);
        if (sections == null) {
            return null;
        }
        for (Section section : sections) {
            if (hasXMPHeader(section.data)) {
                byte[] buffer = new byte[(getXMPContentEnd(section.data) - 29)];
                System.arraycopy(section.data, 29, buffer, 0, buffer.length);
                try {
                    return XMPMetaFactory.parseFromBuffer(buffer);
                } catch (XMPException e) {
                    Log.d("XmpUtil", "XMP parse error", e);
                    return null;
                }
            }
        }
        return null;
    }

    public static XMPMeta createXMPMeta() {
        return XMPMetaFactory.create();
    }

    public static XMPMeta extractOrCreateXMPMeta(String filename) {
        XMPMeta meta = extractXMPMeta(filename);
        return meta == null ? createXMPMeta() : meta;
    }

    public static boolean writeXMPMeta(String filename, XMPMeta meta) {
        IOException e;
        Throwable th;
        if (filename.toLowerCase().endsWith(".jpg") || filename.toLowerCase().endsWith(".jpeg")) {
            try {
                List<Section> sections = insertXMPSection(parse(new FileInputStream(filename), false), meta);
                if (sections == null) {
                    return false;
                }
                FileOutputStream os = null;
                try {
                    FileOutputStream os2 = new FileOutputStream(filename);
                    try {
                        writeJpegFile(os2, sections);
                        if (os2 != null) {
                            try {
                                os2.close();
                            } catch (IOException e2) {
                            }
                        }
                        return true;
                    } catch (IOException e3) {
                        e = e3;
                        os = os2;
                        try {
                            Log.d("XmpUtil", "Write file failed:" + filename, e);
                            if (os != null) {
                                try {
                                    os.close();
                                } catch (IOException e4) {
                                }
                            }
                            return false;
                        } catch (Throwable th2) {
                            th = th2;
                            if (os != null) {
                                try {
                                    os.close();
                                } catch (IOException e5) {
                                }
                            }
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        os = os2;
                        if (os != null) {
                            os.close();
                        }
                        throw th;
                    }
                } catch (IOException e6) {
                    e = e6;
                    Log.d("XmpUtil", "Write file failed:" + filename, e);
                    if (os != null) {
                        os.close();
                    }
                    return false;
                }
            } catch (FileNotFoundException e7) {
                Log.e("XmpUtil", "Could not read file: " + filename, e7);
                return false;
            }
        }
        Log.d("XmpUtil", "XMP parse: only jpeg file is supported");
        return false;
    }

    private static void writeJpegFile(OutputStream os, List<Section> sections) throws IOException {
        os.write(255);
        os.write(216);
        for (Section section : sections) {
            os.write(255);
            os.write(section.marker);
            if (section.length > 0) {
                int ll = section.length & 255;
                os.write(section.length >> 8);
                os.write(ll);
            }
            os.write(section.data);
        }
    }

    private static List<Section> insertXMPSection(List<Section> sections, XMPMeta meta) {
        int position = 1;
        if (sections == null || sections.size() <= 1) {
            return null;
        }
        try {
            SerializeOptions options = new SerializeOptions();
            options.setUseCompactFormat(true);
            options.setOmitPacketWrapper(true);
            byte[] buffer = XMPMetaFactory.serializeToBuffer(meta, options);
            if (buffer.length > 65502) {
                return null;
            }
            byte[] xmpdata = new byte[(buffer.length + 29)];
            System.arraycopy("http://ns.adobe.com/xap/1.0/\u0000".getBytes(), 0, xmpdata, 0, 29);
            System.arraycopy(buffer, 0, xmpdata, 29, buffer.length);
            Section xmpSection = new Section();
            xmpSection.marker = 225;
            xmpSection.length = xmpdata.length + 2;
            xmpSection.data = xmpdata;
            int i = 0;
            while (i < sections.size()) {
                if (((Section) sections.get(i)).marker == 225 && hasXMPHeader(((Section) sections.get(i)).data)) {
                    sections.set(i, xmpSection);
                    return sections;
                }
                i++;
            }
            List<Section> newSections = new ArrayList();
            if (((Section) sections.get(0)).marker != 225) {
                position = 0;
            }
            newSections.addAll(sections.subList(0, position));
            newSections.add(xmpSection);
            newSections.addAll(sections.subList(position, sections.size()));
            return newSections;
        } catch (XMPException e) {
            Log.d("XmpUtil", "Serialize xmp failed", e);
            return null;
        }
    }

    private static boolean hasXMPHeader(byte[] data) {
        if (data.length < 29) {
            return false;
        }
        try {
            byte[] header = new byte[29];
            System.arraycopy(data, 0, header, 0, 29);
            if (new String(header, "UTF-8").equals("http://ns.adobe.com/xap/1.0/\u0000")) {
                return true;
            }
            return false;
        } catch (UnsupportedEncodingException e) {
            return false;
        }
    }

    private static int getXMPContentEnd(byte[] data) {
        int i = data.length - 1;
        while (i >= 1) {
            if (data[i] == (byte) 62 && data[i - 1] != (byte) 63) {
                return i + 1;
            }
            i--;
        }
        return data.length;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static java.util.List<com.android.camera.XmpUtil.Section> parse(java.io.InputStream r14, boolean r15) {
        /*
        r13 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r12 = -1;
        r11 = 0;
        r8 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        if (r8 != r13) goto L_0x0012;
    L_0x000a:
        r8 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        r9 = 216; // 0xd8 float:3.03E-43 double:1.067E-321;
        if (r8 == r9) goto L_0x001a;
    L_0x0012:
        if (r14 == 0) goto L_0x0017;
    L_0x0014:
        r14.close();	 Catch:{ IOException -> 0x0018 }
    L_0x0017:
        return r11;
    L_0x0018:
        r1 = move-exception;
        goto L_0x0017;
    L_0x001a:
        r7 = new java.util.ArrayList;	 Catch:{ IOException -> 0x00a8 }
        r7.<init>();	 Catch:{ IOException -> 0x00a8 }
    L_0x001f:
        r0 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        if (r0 == r12) goto L_0x00c7;
    L_0x0025:
        if (r0 == r13) goto L_0x002f;
    L_0x0027:
        if (r14 == 0) goto L_0x002c;
    L_0x0029:
        r14.close();	 Catch:{ IOException -> 0x002d }
    L_0x002c:
        return r11;
    L_0x002d:
        r1 = move-exception;
        goto L_0x002c;
    L_0x002f:
        r0 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        if (r0 == r13) goto L_0x002f;
    L_0x0035:
        if (r0 != r12) goto L_0x003f;
    L_0x0037:
        if (r14 == 0) goto L_0x003c;
    L_0x0039:
        r14.close();	 Catch:{ IOException -> 0x003d }
    L_0x003c:
        return r11;
    L_0x003d:
        r1 = move-exception;
        goto L_0x003c;
    L_0x003f:
        r5 = r0;
        r8 = 218; // 0xda float:3.05E-43 double:1.077E-321;
        if (r0 != r8) goto L_0x006d;
    L_0x0044:
        if (r15 != 0) goto L_0x0065;
    L_0x0046:
        r6 = new com.android.camera.XmpUtil$Section;	 Catch:{ IOException -> 0x00a8 }
        r8 = 0;
        r6.<init>();	 Catch:{ IOException -> 0x00a8 }
        r6.marker = r0;	 Catch:{ IOException -> 0x00a8 }
        r8 = -1;
        r6.length = r8;	 Catch:{ IOException -> 0x00a8 }
        r8 = r14.available();	 Catch:{ IOException -> 0x00a8 }
        r8 = new byte[r8];	 Catch:{ IOException -> 0x00a8 }
        r6.data = r8;	 Catch:{ IOException -> 0x00a8 }
        r8 = r6.data;	 Catch:{ IOException -> 0x00a8 }
        r9 = r6.data;	 Catch:{ IOException -> 0x00a8 }
        r9 = r9.length;	 Catch:{ IOException -> 0x00a8 }
        r10 = 0;
        r14.read(r8, r10, r9);	 Catch:{ IOException -> 0x00a8 }
        r7.add(r6);	 Catch:{ IOException -> 0x00a8 }
    L_0x0065:
        if (r14 == 0) goto L_0x006a;
    L_0x0067:
        r14.close();	 Catch:{ IOException -> 0x006b }
    L_0x006a:
        return r7;
    L_0x006b:
        r1 = move-exception;
        goto L_0x006a;
    L_0x006d:
        r3 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        r4 = r14.read();	 Catch:{ IOException -> 0x00a8 }
        if (r3 == r12) goto L_0x0079;
    L_0x0077:
        if (r4 != r12) goto L_0x0081;
    L_0x0079:
        if (r14 == 0) goto L_0x007e;
    L_0x007b:
        r14.close();	 Catch:{ IOException -> 0x007f }
    L_0x007e:
        return r11;
    L_0x007f:
        r1 = move-exception;
        goto L_0x007e;
    L_0x0081:
        r8 = r3 << 8;
        r2 = r8 | r4;
        if (r15 == 0) goto L_0x008b;
    L_0x0087:
        r8 = 225; // 0xe1 float:3.15E-43 double:1.11E-321;
        if (r0 != r8) goto L_0x00b8;
    L_0x008b:
        r6 = new com.android.camera.XmpUtil$Section;	 Catch:{ IOException -> 0x00a8 }
        r8 = 0;
        r6.<init>();	 Catch:{ IOException -> 0x00a8 }
        r6.marker = r0;	 Catch:{ IOException -> 0x00a8 }
        r6.length = r2;	 Catch:{ IOException -> 0x00a8 }
        r8 = r2 + -2;
        r8 = new byte[r8];	 Catch:{ IOException -> 0x00a8 }
        r6.data = r8;	 Catch:{ IOException -> 0x00a8 }
        r8 = r6.data;	 Catch:{ IOException -> 0x00a8 }
        r9 = r2 + -2;
        r10 = 0;
        r14.read(r8, r10, r9);	 Catch:{ IOException -> 0x00a8 }
        r7.add(r6);	 Catch:{ IOException -> 0x00a8 }
        goto L_0x001f;
    L_0x00a8:
        r1 = move-exception;
        r8 = "XmpUtil";
        r9 = "Could not parse file.";
        com.android.camera.Log.d(r8, r9, r1);	 Catch:{ all -> 0x00c0 }
        if (r14 == 0) goto L_0x00b7;
    L_0x00b4:
        r14.close();	 Catch:{ IOException -> 0x00cf }
    L_0x00b7:
        return r11;
    L_0x00b8:
        r8 = r2 + -2;
        r8 = (long) r8;
        r14.skip(r8);	 Catch:{ IOException -> 0x00a8 }
        goto L_0x001f;
    L_0x00c0:
        r8 = move-exception;
        if (r14 == 0) goto L_0x00c6;
    L_0x00c3:
        r14.close();	 Catch:{ IOException -> 0x00d1 }
    L_0x00c6:
        throw r8;
    L_0x00c7:
        if (r14 == 0) goto L_0x00cc;
    L_0x00c9:
        r14.close();	 Catch:{ IOException -> 0x00cd }
    L_0x00cc:
        return r7;
    L_0x00cd:
        r1 = move-exception;
        goto L_0x00cc;
    L_0x00cf:
        r1 = move-exception;
        goto L_0x00b7;
    L_0x00d1:
        r1 = move-exception;
        goto L_0x00c6;
        */
        throw new UnsupportedOperationException("Method not decompiled: com.android.camera.XmpUtil.parse(java.io.InputStream, boolean):java.util.List<com.android.camera.XmpUtil$Section>");
    }

    private XmpUtil() {
    }
}
