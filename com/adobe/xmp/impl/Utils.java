package com.adobe.xmp.impl;

public class Utils {
    private static boolean[] xmlNameChars;
    private static boolean[] xmlNameStartChars;

    static {
        initCharTables();
    }

    private Utils() {
    }

    public static String normalizeLangValue(String value) {
        if ("x-default".equals(value)) {
            return value;
        }
        int subTag = 1;
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < value.length(); i++) {
            switch (value.charAt(i)) {
                case ' ':
                    break;
                case '-':
                case '_':
                    buffer.append('-');
                    subTag++;
                    break;
                default:
                    if (subTag == 2) {
                        buffer.append(Character.toUpperCase(value.charAt(i)));
                        break;
                    }
                    buffer.append(Character.toLowerCase(value.charAt(i)));
                    break;
            }
        }
        return buffer.toString();
    }

    static String[] splitNameAndValue(String selector) {
        int eq = selector.indexOf(61);
        int pos = 1;
        if (selector.charAt(1) == '?') {
            pos = 2;
        }
        String name = selector.substring(pos, eq);
        pos = eq + 1;
        char quote = selector.charAt(pos);
        pos++;
        int end = selector.length() - 2;
        StringBuffer value = new StringBuffer(end - eq);
        while (pos < end) {
            value.append(selector.charAt(pos));
            pos++;
            if (selector.charAt(pos) == quote) {
                pos++;
            }
        }
        return new String[]{name, value.toString()};
    }

    static boolean checkUUIDFormat(String uuid) {
        boolean z = false;
        boolean result = true;
        int delimCnt = 0;
        if (uuid == null) {
            return false;
        }
        int delimPos = 0;
        while (delimPos < uuid.length()) {
            if (uuid.charAt(delimPos) == '-') {
                delimCnt++;
                if (!result) {
                    result = false;
                } else if (delimPos == 8 || delimPos == 13 || delimPos == 18 || delimPos == 23) {
                    result = true;
                } else {
                    result = false;
                }
            }
            delimPos++;
        }
        if (result && 4 == delimCnt && 36 == delimPos) {
            z = true;
        }
        return z;
    }

    public static boolean isXMLName(String name) {
        if (name.length() > 0 && !isNameStartChar(name.charAt(0))) {
            return false;
        }
        for (int i = 1; i < name.length(); i++) {
            if (!isNameChar(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    public static boolean isXMLNameNS(String name) {
        if (name.length() > 0 && (!isNameStartChar(name.charAt(0)) || name.charAt(0) == ':')) {
            return false;
        }
        int i = 1;
        while (i < name.length()) {
            if (!isNameChar(name.charAt(i)) || name.charAt(i) == ':') {
                return false;
            }
            i++;
        }
        return true;
    }

    static boolean isControlChar(char c) {
        if ((c > '\u001f' && c != '') || c == '\t' || c == '\n' || c == '\r') {
            return false;
        }
        return true;
    }

    public static String escapeXML(String value, boolean forAttribute, boolean escapeWhitespaces) {
        int i;
        boolean needsEscaping = false;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '<' || c == '>' || c == '&' || ((escapeWhitespaces && (c == '\t' || c == '\n' || c == '\r')) || (forAttribute && c == '\"'))) {
                needsEscaping = true;
                break;
            }
        }
        if (!needsEscaping) {
            return value;
        }
        StringBuffer buffer = new StringBuffer((value.length() * 4) / 3);
        for (i = 0; i < value.length(); i++) {
            c = value.charAt(i);
            Object obj = (escapeWhitespaces && (c == '\t' || c == '\n' || c == '\r')) ? 1 : null;
            if (obj == null) {
                switch (c) {
                    case '\"':
                        buffer.append(forAttribute ? "&quot;" : "\"");
                        break;
                    case '&':
                        buffer.append("&amp;");
                        break;
                    case '<':
                        buffer.append("&lt;");
                        break;
                    case '>':
                        buffer.append("&gt;");
                        break;
                    default:
                        buffer.append(c);
                        break;
                }
            }
            buffer.append("&#x");
            buffer.append(Integer.toHexString(c).toUpperCase());
            buffer.append(';');
        }
        return buffer.toString();
    }

    static String removeControlChars(String value) {
        StringBuffer buffer = new StringBuffer(value);
        for (int i = 0; i < buffer.length(); i++) {
            if (isControlChar(buffer.charAt(i))) {
                buffer.setCharAt(i, ' ');
            }
        }
        return buffer.toString();
    }

    private static boolean isNameStartChar(char ch) {
        return ch <= 'ÿ' ? xmlNameStartChars[ch] : true;
    }

    private static boolean isNameChar(char ch) {
        return ch <= 'ÿ' ? xmlNameChars[ch] : true;
    }

    private static void initCharTables() {
        xmlNameChars = new boolean[256];
        xmlNameStartChars = new boolean[256];
        char ch = '\u0000';
        while (ch < xmlNameChars.length) {
            boolean[] zArr = xmlNameStartChars;
            boolean z = (('a' > ch || ch > 'z') && (('A' > ch || ch > 'Z') && ch != ':' && ch != '_' && ('À' > ch || ch > 'Ö'))) ? 'Ø' <= ch && ch <= 'ö' : true;
            zArr[ch] = z;
            zArr = xmlNameChars;
            z = (('a' > ch || ch > 'z') && (('A' > ch || ch > 'Z') && !(('0' <= ch && ch <= '9') || ch == ':' || ch == '_' || ch == '-' || ch == '.' || ch == '·' || ('À' <= ch && ch <= 'Ö')))) ? 'Ø' <= ch && ch <= 'ö' : true;
            zArr[ch] = z;
            ch = (char) (ch + 1);
        }
    }
}
