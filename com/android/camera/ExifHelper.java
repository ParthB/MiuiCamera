package com.android.camera;

import android.location.Location;
import android.media.ExifInterface;
import android.os.Build;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class ExifHelper {
    private static DateFormat mDateTimeStampFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
    private static DateFormat mGPSDateStampFormat = new SimpleDateFormat("yyyy:MM:dd");
    private static DateFormat mGPSTimeStampFormat = new SimpleDateFormat("HH:mm:ss");

    static {
        TimeZone tzUTC = TimeZone.getTimeZone("UTC");
        mGPSDateStampFormat.setTimeZone(tzUTC);
        mGPSTimeStampFormat.setTimeZone(tzUTC);
    }

    public static void writeExif(String filePath, int orientation, Location location, long timeTaken) {
        try {
            ExifInterface exif = new ExifInterface(filePath);
            exif.setAttribute("GPSDateStamp", mGPSDateStampFormat.format(Long.valueOf(timeTaken)));
            exif.setAttribute("GPSTimeStamp", mGPSTimeStampFormat.format(Long.valueOf(timeTaken)));
            exif.setAttribute("DateTime", mDateTimeStampFormat.format(Long.valueOf(timeTaken)));
            exif.setAttribute("Orientation", getExifOrientation(orientation));
            exif.setAttribute("Make", Build.MANUFACTURER);
            if (location != null) {
                double latValue = location.getLatitude();
                double longValue = location.getLongitude();
                exif.setAttribute("GPSLatitude", convertDoubleToLaLon(latValue));
                exif.setAttribute("GPSLongitude", convertDoubleToLaLon(longValue));
                if (latValue > 0.0d) {
                    exif.setAttribute("GPSLatitudeRef", "N");
                } else {
                    exif.setAttribute("GPSLatitudeRef", "S");
                }
                if (longValue > 0.0d) {
                    exif.setAttribute("GPSLongitudeRef", "E");
                } else {
                    exif.setAttribute("GPSLongitudeRef", "W");
                }
            }
            if (Device.IS_MI2 || Device.IS_MI2A) {
                exif.setAttribute("Model", "MiTwo");
                exif.setAttribute("FocalLength", String.valueOf("354/100"));
            } else {
                exif.setAttribute("Model", Device.MODULE);
            }
            exif.saveAttributes();
        } catch (IOException e) {
        }
    }

    public static String convertDoubleToLaLon(double value) {
        int degrees = (int) Math.floor(Math.abs(value));
        double minutes = Math.floor((Math.abs(value) - ((double) degrees)) * 60.0d);
        double seconds = Math.floor(((Math.abs(value) - ((double) degrees)) - (minutes / 60.0d)) * 3600000.0d);
        if (value < 0.0d) {
            return "-" + degrees + "/1," + ((int) minutes) + "/1," + ((int) seconds) + "/1000";
        }
        return degrees + "/1," + ((int) minutes) + "/1," + ((int) seconds) + "/1000";
    }

    private static String getExifOrientation(int orientation) {
        switch (orientation) {
            case 0:
                return String.valueOf(1);
            case 90:
                return String.valueOf(6);
            case 180:
                return String.valueOf(3);
            case 270:
                return String.valueOf(8);
            default:
                throw new AssertionError("invalid: " + orientation);
        }
    }
}
