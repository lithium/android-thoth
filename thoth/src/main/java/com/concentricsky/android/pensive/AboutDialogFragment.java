package com.concentricsky.android.pensive;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * Created by wiggins on 7/16/13.
 */
public class AboutDialogFragment extends DialogFragment
{
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        LayoutInflater layoutInflater = activity.getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.dialog_about, null, false);

        TextView tv = (TextView)view.findViewById(R.id.version);
        tv.setText(getVersionName(activity));

        builder.setView(view);
        return builder.create();
    }

    public static String getVersionName(Context context)
    {
        try {
            ComponentName comp = new ComponentName(context, context.getClass());
            PackageInfo pinfo = context.getPackageManager().getPackageInfo(comp.getPackageName(), 0);
            return pinfo.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException e) {
            return null;
        }
    }

}
