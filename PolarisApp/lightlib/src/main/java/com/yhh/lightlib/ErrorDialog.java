package com.yhh.lightlib;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;//两个activity 之间的通讯可以通过bundle类实现


 //设备不支持camera2 api时弹出提示窗口
public class ErrorDialog extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        return new AlertDialog.Builder(activity)
                .setMessage("This device doesn't support Camera2 API.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        activity.finish();
                    }
                })
                .create();
    }
}