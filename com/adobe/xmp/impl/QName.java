package com.adobe.xmp.impl;

public class QName {
    private String localName;
    private String prefix;

    public QName(String qname) {
        int colon = qname.indexOf(58);
        if (colon >= 0) {
            this.prefix = qname.substring(0, colon);
            this.localName = qname.substring(colon + 1);
            return;
        }
        this.prefix = "";
        this.localName = qname;
    }

    public boolean hasPrefix() {
        return this.prefix != null && this.prefix.length() > 0;
    }

    public String getPrefix() {
        return this.prefix;
    }
}
