package com.adobe.xmp.options;

import com.adobe.xmp.XMPException;

public final class SerializeOptions extends Options {
    private int baseIndent = 0;
    private String indent = "  ";
    private String newline = "\n";
    private boolean omitVersionAttribute = false;
    private int padding = 2048;

    public SerializeOptions(int options) throws XMPException {
        super(options);
    }

    public boolean getOmitPacketWrapper() {
        return getOption(16);
    }

    public SerializeOptions setOmitPacketWrapper(boolean value) {
        setOption(16, value);
        return this;
    }

    public boolean getReadOnlyPacket() {
        return getOption(32);
    }

    public boolean getUseCompactFormat() {
        return getOption(64);
    }

    public SerializeOptions setUseCompactFormat(boolean value) {
        setOption(64, value);
        return this;
    }

    public boolean getIncludeThumbnailPad() {
        return getOption(256);
    }

    public boolean getExactPacketLength() {
        return getOption(512);
    }

    public boolean getSort() {
        return getOption(4096);
    }

    public boolean getEncodeUTF16BE() {
        return (getOptions() & 3) == 2;
    }

    public boolean getEncodeUTF16LE() {
        return (getOptions() & 3) == 3;
    }

    public int getBaseIndent() {
        return this.baseIndent;
    }

    public SerializeOptions setBaseIndent(int baseIndent) {
        this.baseIndent = baseIndent;
        return this;
    }

    public String getIndent() {
        return this.indent;
    }

    public SerializeOptions setIndent(String indent) {
        this.indent = indent;
        return this;
    }

    public String getNewline() {
        return this.newline;
    }

    public SerializeOptions setNewline(String newline) {
        this.newline = newline;
        return this;
    }

    public int getPadding() {
        return this.padding;
    }

    public SerializeOptions setPadding(int padding) {
        this.padding = padding;
        return this;
    }

    public boolean getOmitVersionAttribute() {
        return this.omitVersionAttribute;
    }

    public String getEncoding() {
        if (getEncodeUTF16BE()) {
            return "UTF-16BE";
        }
        if (getEncodeUTF16LE()) {
            return "UTF-16LE";
        }
        return "UTF-8";
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            SerializeOptions clone = new SerializeOptions(getOptions());
            clone.setBaseIndent(this.baseIndent);
            clone.setIndent(this.indent);
            clone.setNewline(this.newline);
            clone.setPadding(this.padding);
            return clone;
        } catch (XMPException e) {
            return null;
        }
    }

    protected int getValidOptions() {
        return 4976;
    }
}
