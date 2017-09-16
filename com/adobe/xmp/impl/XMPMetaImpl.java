package com.adobe.xmp.impl;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.impl.xpath.XMPPathParser;
import com.adobe.xmp.options.PropertyOptions;
import java.util.Iterator;

public class XMPMetaImpl implements XMPMeta {
    static final /* synthetic */ boolean -assertionsDisabled = (!XMPMetaImpl.class.desiredAssertionStatus());
    private String packetHeader;
    private XMPNode tree;

    public XMPMetaImpl() {
        this.packetHeader = null;
        this.tree = new XMPNode(null, null, null);
    }

    public XMPMetaImpl(XMPNode tree) {
        this.packetHeader = null;
        this.tree = tree;
    }

    public boolean doesPropertyExist(String schemaNS, String propName) {
        boolean z = false;
        try {
            ParameterAsserts.assertSchemaNS(schemaNS);
            ParameterAsserts.assertPropName(propName);
            if (XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), false, null) != null) {
                z = true;
            }
            return z;
        } catch (XMPException e) {
            return false;
        }
    }

    public void setLocalizedText(String schemaNS, String altTextName, String genericLang, String specificLang, String itemValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertArrayName(altTextName);
        ParameterAsserts.assertSpecificLang(specificLang);
        genericLang = genericLang != null ? Utils.normalizeLangValue(genericLang) : null;
        specificLang = Utils.normalizeLangValue(specificLang);
        XMPNode arrayNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, altTextName), true, new PropertyOptions(7680));
        if (arrayNode == null) {
            throw new XMPException("Failed to find or create array node", 102);
        }
        if (!arrayNode.getOptions().isArrayAltText()) {
            if (arrayNode.hasChildren() || !arrayNode.getOptions().isArrayAlternate()) {
                throw new XMPException("Specified property is no alt-text array", 102);
            }
            arrayNode.getOptions().setArrayAltText(true);
        }
        boolean haveXDefault = false;
        XMPNode xMPNode = null;
        Iterator it = arrayNode.iterateChildren();
        while (it.hasNext()) {
            XMPNode currItem = (XMPNode) it.next();
            if (currItem.hasQualifier() && "xml:lang".equals(currItem.getQualifier(1).getName())) {
                if ("x-default".equals(currItem.getQualifier(1).getValue())) {
                    xMPNode = currItem;
                    haveXDefault = true;
                    break;
                }
            }
            throw new XMPException("Language qualifier must be first", 102);
        }
        if (xMPNode != null && arrayNode.getChildrenLength() > 1) {
            arrayNode.removeChild(xMPNode);
            arrayNode.addChild(1, xMPNode);
        }
        Object[] result = XMPNodeUtils.chooseLocalizedText(arrayNode, genericLang, specificLang);
        int match = ((Integer) result[0]).intValue();
        XMPNode itemNode = result[1];
        boolean specificXDefault = "x-default".equals(specificLang);
        switch (match) {
            case 0:
                XMPNodeUtils.appendLangItem(arrayNode, "x-default", itemValue);
                haveXDefault = true;
                if (!specificXDefault) {
                    XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                    break;
                }
                break;
            case 1:
                if (!specificXDefault) {
                    if (haveXDefault && xMPNode != itemNode && xMPNode != null && xMPNode.getValue().equals(itemNode.getValue())) {
                        xMPNode.setValue(itemValue);
                    }
                    itemNode.setValue(itemValue);
                    break;
                }
                if (!-assertionsDisabled) {
                    Object obj = (haveXDefault && xMPNode == itemNode) ? 1 : null;
                    if (obj == null) {
                        throw new AssertionError();
                    }
                }
                it = arrayNode.iterateChildren();
                while (it.hasNext()) {
                    currItem = (XMPNode) it.next();
                    if (currItem != xMPNode) {
                        if (currItem.getValue().equals(xMPNode != null ? xMPNode.getValue() : null)) {
                            currItem.setValue(itemValue);
                        }
                    }
                }
                if (xMPNode != null) {
                    xMPNode.setValue(itemValue);
                    break;
                }
                break;
            case 2:
                if (haveXDefault && xMPNode != itemNode && xMPNode != null && xMPNode.getValue().equals(itemNode.getValue())) {
                    xMPNode.setValue(itemValue);
                }
                itemNode.setValue(itemValue);
                break;
            case 3:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                    break;
                }
                break;
            case 4:
                if (xMPNode != null && arrayNode.getChildrenLength() == 1) {
                    xMPNode.setValue(itemValue);
                }
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                break;
            case 5:
                XMPNodeUtils.appendLangItem(arrayNode, specificLang, itemValue);
                if (specificXDefault) {
                    haveXDefault = true;
                    break;
                }
                break;
            default:
                throw new XMPException("Unexpected result from ChooseLocalizedText", 9);
        }
        if (!haveXDefault && arrayNode.getChildrenLength() == 1) {
            XMPNodeUtils.appendLangItem(arrayNode, "x-default", itemValue);
        }
    }

    public void setProperty(String schemaNS, String propName, Object propValue, PropertyOptions options) throws XMPException {
        ParameterAsserts.assertSchemaNS(schemaNS);
        ParameterAsserts.assertPropName(propName);
        options = XMPNodeUtils.verifySetOptions(options, propValue);
        XMPNode propNode = XMPNodeUtils.findNode(this.tree, XMPPathParser.expandXPath(schemaNS, propName), true, options);
        if (propNode != null) {
            setNode(propNode, propValue, options, false);
            return;
        }
        throw new XMPException("Specified property does not exist", 102);
    }

    public void setProperty(String schemaNS, String propName, Object propValue) throws XMPException {
        setProperty(schemaNS, propName, propValue, null);
    }

    public void setPacketHeader(String packetHeader) {
        this.packetHeader = packetHeader;
    }

    public Object clone() {
        return new XMPMetaImpl((XMPNode) this.tree.clone());
    }

    public void sort() {
        this.tree.sort();
    }

    public XMPNode getRoot() {
        return this.tree;
    }

    void setNode(XMPNode node, Object value, PropertyOptions newOptions, boolean deleteExisting) throws XMPException {
        if (deleteExisting) {
            node.clear();
        }
        node.getOptions().mergeWith(newOptions);
        if (!node.getOptions().isCompositeProperty()) {
            XMPNodeUtils.setNodeValue(node, value);
        } else if (value == null || value.toString().length() <= 0) {
            node.removeChildren();
        } else {
            throw new XMPException("Composite nodes can't have values", 102);
        }
    }
}
