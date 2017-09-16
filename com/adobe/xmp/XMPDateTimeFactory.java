package com.adobe.xmp;

import com.adobe.xmp.impl.XMPDateTimeImpl;
import java.util.Calendar;
import java.util.TimeZone;

public final class XMPDateTimeFactory {
    private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

    private XMPDateTimeFactory() {
    }

    public static XMPDateTime createFromCalendar(Calendar calendar) {
        return new XMPDateTimeImpl(calendar);
    }
}
