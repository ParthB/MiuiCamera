package miui.external;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class SdkErrorActivity extends Activity {
    private static final /* synthetic */ int[] j = null;
    private OnClickListener g = new OnClickListener(this) {
        final /* synthetic */ SdkErrorActivity v;

        {
            this.v = r1;
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            this.v.finish();
            System.exit(0);
        }
    };
    private String h;
    private OnClickListener i = new OnClickListener(this) {
        final /* synthetic */ SdkErrorActivity w;

        {
            this.w = r1;
        }

        public void onClick(DialogInterface dialogInterface, int i) {
            dialogInterface.dismiss();
            final Dialog v = this.w.q();
            new SdkDialogFragment(this.w, v).show(this.w.getFragmentManager(), "SdkUpdatePromptDialog");
            new AsyncTask<Void, Void, Boolean>(this) {
                final /* synthetic */ AnonymousClass2 x;

                protected /* bridge */ /* synthetic */ Object doInBackground(Object[] objArr) {
                    return V((Void[]) objArr);
                }

                protected Boolean V(Void... voidArr) {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return Boolean.valueOf(this.x.w.u());
                }

                protected /* bridge */ /* synthetic */ void onPostExecute(Object obj) {
                    W((Boolean) obj);
                }

                protected void W(Boolean bool) {
                    v.dismiss();
                    new SdkDialogFragment(this.x.w, bool.booleanValue() ? this.x.w.s() : this.x.w.r()).show(this.x.w.getFragmentManager(), "SdkUpdateFinishDialog");
                }
            }.execute(new Void[0]);
        }
    };

    class SdkDialogFragment extends DialogFragment {
        private Dialog k;
        final /* synthetic */ SdkErrorActivity l;

        public SdkDialogFragment(SdkErrorActivity sdkErrorActivity, Dialog dialog) {
            this.l = sdkErrorActivity;
            this.k = dialog;
        }

        public Dialog onCreateDialog(Bundle bundle) {
            return this.k;
        }
    }

    private static /* synthetic */ int[] z() {
        if (j != null) {
            return j;
        }
        int[] iArr = new int[SdkConstants$SdkError.values().length];
        try {
            iArr[SdkConstants$SdkError.GENERIC.ordinal()] = 3;
        } catch (NoSuchFieldError e) {
        }
        try {
            iArr[SdkConstants$SdkError.LOW_SDK_VERSION.ordinal()] = 1;
        } catch (NoSuchFieldError e2) {
        }
        try {
            iArr[SdkConstants$SdkError.NO_SDK.ordinal()] = 2;
        } catch (NoSuchFieldError e3) {
        }
        j = iArr;
        return iArr;
    }

    protected void onCreate(Bundle bundle) {
        Dialog n;
        SdkConstants$SdkError sdkConstants$SdkError = null;
        setTheme(16973909);
        super.onCreate(bundle);
        this.h = Locale.getDefault().getLanguage();
        Intent intent = getIntent();
        if (intent != null) {
            sdkConstants$SdkError = (SdkConstants$SdkError) intent.getSerializableExtra("com.miui.sdk.error");
        }
        if (sdkConstants$SdkError == null) {
            sdkConstants$SdkError = SdkConstants$SdkError.GENERIC;
        }
        switch (z()[sdkConstants$SdkError.ordinal()]) {
            case 1:
                n = n();
                break;
            case 2:
                n = o();
                break;
            default:
                n = m();
                break;
        }
        new SdkDialogFragment(this, n).show(getFragmentManager(), "SdkErrorPromptDialog");
    }

    private Dialog p(String str, String str2, OnClickListener onClickListener) {
        return new Builder(this).setTitle(str).setMessage(str2).setPositiveButton(17039370, onClickListener).setIcon(17301543).setCancelable(false).create();
    }

    private Dialog l(String str, String str2, OnClickListener onClickListener, OnClickListener onClickListener2) {
        return new Builder(this).setTitle(str).setMessage(str2).setPositiveButton(17039370, onClickListener).setNegativeButton(17039360, onClickListener2).setIcon(17301543).setCancelable(false).create();
    }

    private Dialog m() {
        String str;
        String str2;
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            str = "MIUI SDK发生错误";
            str2 = "请重新安装MIUI SDK再运行本程序。";
        } else {
            str = "MIUI SDK encounter errors";
            str2 = "Please re-install MIUI SDK and then re-run this application.";
        }
        return p(str, str2, this.g);
    }

    private Dialog o() {
        String str;
        String str2;
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            str = "没有找到MIUI SDK";
            str2 = "请先安装MIUI SDK再运行本程序。";
        } else {
            str = "MIUI SDK not found";
            str2 = "Please install MIUI SDK and then re-run this application.";
        }
        return p(str, str2, this.g);
    }

    private Dialog n() {
        String str;
        String str2;
        if (t()) {
            if (Locale.CHINESE.getLanguage().equals(this.h)) {
                str = "MIUI SDK版本过低";
                str2 = "请先升级MIUI SDK再运行本程序。是否现在升级？";
            } else {
                str = "MIUI SDK too old";
                str2 = "Please upgrade MIUI SDK and then re-run this application. Upgrade now?";
            }
            return l(str, str2, this.i, this.g);
        }
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            str = "MIUI SDK版本过低";
            str2 = "请先升级MIUI SDK再运行本程序。";
        } else {
            str = "MIUI SDK too old";
            str2 = "Please upgrade MIUI SDK and then re-run this application.";
        }
        return p(str, str2, this.g);
    }

    private Dialog q() {
        CharSequence charSequence;
        CharSequence charSequence2;
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            charSequence = "MIUI SDK正在更新";
            charSequence2 = "请稍候...";
        } else {
            charSequence = "MIUI SDK updating";
            charSequence2 = "Please wait...";
        }
        return ProgressDialog.show(this, charSequence, charSequence2, true, false);
    }

    private Dialog s() {
        String str;
        String str2;
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            str = "MIUI SDK更新完成";
            str2 = "请重新运行本程序。";
        } else {
            str = "MIUI SDK updated";
            str2 = "Please re-run this application.";
        }
        return p(str, str2, this.g);
    }

    private Dialog r() {
        String str;
        String str2;
        if (Locale.CHINESE.getLanguage().equals(this.h)) {
            str = "MIUI SDK更新失败";
            str2 = "请稍后重试。";
        } else {
            str = "MIUI SDK update failed";
            str2 = "Please try it later.";
        }
        return p(str, str2, this.g);
    }

    private boolean t() {
        try {
            return ((Boolean) b.b().getMethod("supportUpdate", new Class[]{Map.class}).invoke(null, new Object[]{null})).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean u() {
        try {
            HashMap hashMap = new HashMap();
            return ((Boolean) b.b().getMethod("update", new Class[]{Map.class}).invoke(null, new Object[]{hashMap})).booleanValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
