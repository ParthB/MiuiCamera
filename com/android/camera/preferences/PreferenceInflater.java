package com.android.camera.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Xml;
import android.view.InflateException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class PreferenceInflater {
    private static final Class<?>[] CTOR_SIGNATURE = new Class[]{Context.class, AttributeSet.class};
    private static final String PACKAGE_NAME = PreferenceInflater.class.getPackage().getName();
    private static final HashMap<String, Constructor<?>> sConstructorMap = new HashMap();
    private Context mContext;

    public PreferenceInflater(Context context) {
        this.mContext = context;
    }

    public CameraPreference inflate(int resId) {
        return inflate(this.mContext.getResources().getXml(resId));
    }

    private CameraPreference newPreference(String tagName, Object[] args) {
        String name = PACKAGE_NAME + "." + tagName;
        Constructor<?> constructor = (Constructor) sConstructorMap.get(name);
        if (constructor == null) {
            try {
                constructor = this.mContext.getClassLoader().loadClass(name).getConstructor(CTOR_SIGNATURE);
                sConstructorMap.put(name, constructor);
            } catch (NoSuchMethodException e) {
                throw new InflateException("Error inflating class " + name, e);
            } catch (ClassNotFoundException e2) {
                throw new InflateException("No such class: " + name, e2);
            } catch (Exception e3) {
                throw new InflateException("While create instance of" + name, e3);
            }
        }
        return (CameraPreference) constructor.newInstance(args);
    }

    private CameraPreference inflate(XmlPullParser parser) {
        AttributeSet attrs = Xml.asAttributeSet(parser);
        ArrayList<CameraPreference> list = new ArrayList();
        Object[] args = new Object[]{this.mContext, attrs};
        try {
            int type = parser.next();
            while (type != 1) {
                if (type == 2) {
                    CameraPreference pref = newPreference(parser.getName(), args);
                    int depth = parser.getDepth();
                    if (depth > list.size()) {
                        list.add(pref);
                    } else {
                        list.set(depth - 1, pref);
                    }
                    if (depth > 1) {
                        ((PreferenceGroup) list.get(depth - 2)).addChild(pref);
                    }
                }
                type = parser.next();
            }
            if (list.size() != 0) {
                return (CameraPreference) list.get(0);
            }
            throw new InflateException("No root element found");
        } catch (XmlPullParserException e) {
            throw new InflateException(e);
        } catch (IOException e2) {
            throw new InflateException(parser.getPositionDescription(), e2);
        }
    }
}
