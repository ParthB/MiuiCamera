package com.adobe.xmp;

import com.adobe.xmp.impl.Base64;
import com.adobe.xmp.impl.ISO8601Converter;

public class XMPUtils {
    private XMPUtils() {
    }

    public static String convertFromBoolean(boolean value) {
        return value ? "True" : "False";
    }

    public static String convertFromInteger(int value) {
        return String.valueOf(value);
    }

    public static String convertFromLong(long value) {
        return String.valueOf(value);
    }

    public static String convertFromDouble(double value) {
        return String.valueOf(value);
    }

    public static XMPDateTime convertToDate(String rawValue) throws XMPException {
        if (rawValue != null && rawValue.length() != 0) {
            return ISO8601Converter.parse(rawValue);
        }
        throw new XMPException("Empty convert-string", 5);
    }

    public static String convertFromDate(XMPDateTime value) {
        return ISO8601Converter.render(value);
    }

    public static String encodeBase64(byte[] buffer) {
        return new String(Base64.encode(buffer));
    }
}
