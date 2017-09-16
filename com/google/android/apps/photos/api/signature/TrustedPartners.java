package com.google.android.apps.photos.api.signature;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.TextUtils;
import android.util.Log;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;

public final class TrustedPartners {
    private final PackageManager packageManager;
    private final Set<String> trustedPartnerCertificateHashes;

    public TrustedPartners(Context context, Set<String> trustedPartnerCertificateHashes) {
        this.packageManager = context.getPackageManager();
        this.trustedPartnerCertificateHashes = trustedPartnerCertificateHashes;
    }

    public boolean isTrustedApplication(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            if (Log.isLoggable("TrustedPartners", 5)) {
                Log.w("TrustedPartners", "null or empty package name; do not trust");
            }
            return false;
        }
        try {
            PackageInfo info = this.packageManager.getPackageInfo(packageName, 64);
            if (info.signatures == null || info.signatures.length != 1) {
                if (Log.isLoggable("TrustedPartners", 5)) {
                    Log.w("TrustedPartners", info.signatures.length + " signatures found for package (" + packageName + "); do not trust");
                }
                return false;
            }
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA1");
                digest.update(info.signatures[0].toByteArray());
                return this.trustedPartnerCertificateHashes.contains(HexConvert.bytesToHex(digest.digest()));
            } catch (NoSuchAlgorithmException e) {
                if (Log.isLoggable("TrustedPartners", 6)) {
                    Log.e("TrustedPartners", "unable to compute hash using SHA1; do not trust");
                }
                return false;
            }
        } catch (NameNotFoundException e2) {
            if (Log.isLoggable("TrustedPartners", 5)) {
                Log.w("TrustedPartners", "package not found (" + packageName + "); do not trust");
            }
            return false;
        }
    }
}
