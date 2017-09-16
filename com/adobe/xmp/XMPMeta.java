package com.adobe.xmp;

import com.adobe.xmp.options.PropertyOptions;

public interface XMPMeta extends Cloneable {
    void setLocalizedText(String str, String str2, String str3, String str4, String str5, PropertyOptions propertyOptions) throws XMPException;

    void setProperty(String str, String str2, Object obj) throws XMPException;
}
