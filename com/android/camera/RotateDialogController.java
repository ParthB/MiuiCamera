package com.android.camera;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.support.v7.recyclerview.R;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.android.camera.ui.Rotatable;
import com.android.camera.ui.RotateLayout;

public class RotateDialogController implements Rotatable {
    private Activity mActivity;
    private View mDialogRootLayout;
    private Animation mFadeInAnim;
    private Animation mFadeOutAnim;
    private int mLayoutResourceID;
    private RotateLayout mRotateDialog;
    private TextView mRotateDialogButton1;
    private TextView mRotateDialogButton2;
    private View mRotateDialogButtonLayout;
    private ProgressBar mRotateDialogSpinner;
    private TextView mRotateDialogText;
    private TextView mRotateDialogTitle;
    private View mRotateDialogTitleLayout;

    private void inflateDialogLayout() {
        if (this.mDialogRootLayout == null) {
            View v = this.mActivity.getLayoutInflater().inflate(this.mLayoutResourceID, (ViewGroup) this.mActivity.getWindow().getDecorView());
            this.mDialogRootLayout = v.findViewById(R.id.rotate_dialog_root_layout);
            this.mRotateDialog = (RotateLayout) v.findViewById(R.id.rotate_dialog_layout);
            this.mRotateDialogTitleLayout = v.findViewById(R.id.rotate_dialog_title_layout);
            this.mRotateDialogButtonLayout = v.findViewById(R.id.rotate_dialog_button_layout);
            this.mRotateDialogTitle = (TextView) v.findViewById(R.id.rotate_dialog_title);
            this.mRotateDialogSpinner = (ProgressBar) v.findViewById(R.id.rotate_dialog_spinner);
            this.mRotateDialogText = (TextView) v.findViewById(R.id.rotate_dialog_text);
            this.mRotateDialogButton1 = (Button) v.findViewById(R.id.rotate_dialog_button1);
            this.mRotateDialogButton2 = (Button) v.findViewById(R.id.rotate_dialog_button2);
            this.mFadeInAnim = AnimationUtils.loadAnimation(this.mActivity, 17432576);
            this.mFadeOutAnim = AnimationUtils.loadAnimation(this.mActivity, 17432577);
            this.mFadeInAnim.setDuration(150);
            this.mFadeOutAnim.setDuration(150);
        }
    }

    public void setOrientation(int orientation, boolean animation) {
        inflateDialogLayout();
        this.mRotateDialog.setOrientation(orientation, animation);
    }

    public static void showSystemAlertDialog(Context context, String title, String msg, String button1Text, final Runnable r1, String button2Text, final Runnable r2) {
        Builder builder = new Builder(context);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setCancelable(false);
        if (button1Text != null) {
            builder.setPositiveButton(button1Text, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (r1 != null) {
                        r1.run();
                    }
                }
            });
        }
        if (button2Text != null) {
            builder.setNegativeButton(button2Text, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (r2 != null) {
                        r2.run();
                    }
                }
            });
        }
        builder.create().show();
    }

    public static void showSystemChoiceDialog(Context context, String title, String msg, String choiceContent, String buttonText, final Runnable checkrun, final Runnable uncheckrun) {
        Builder builder = new Builder(context);
        View message = LayoutInflater.from(context).inflate(R.layout.v6_choice_alertdialog, null);
        ((TextView) message.findViewById(R.id.alert_declaration)).setText(msg);
        final CheckBox choice = (CheckBox) message.findViewById(R.id.alert_declaration_checkbox);
        choice.setText(choiceContent);
        builder.setTitle(title);
        builder.setCancelable(false);
        builder.setView(message);
        if (buttonText != null) {
            builder.setPositiveButton(buttonText, new OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (choice.isChecked()) {
                        if (checkrun != null) {
                            checkrun.run();
                        }
                    } else if (uncheckrun != null) {
                        uncheckrun.run();
                    }
                }
            });
        }
        builder.create().show();
    }
}
