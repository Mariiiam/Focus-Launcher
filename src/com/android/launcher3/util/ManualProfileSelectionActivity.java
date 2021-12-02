package com.android.launcher3.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.google.android.apps.nexuslauncher.NexusLauncherActivity;

public class ManualProfileSelectionActivity extends Activity {

    String[] mAllProfiles = Launcher.getAllProfiles();
    String chosenProfile;
    final int START_LAUNCHER_FROM_PROFILESELECTION = 44;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AlertDialog builder = new AlertDialog.Builder(this)
                .setTitle(R.string.title_manually_select_profile)
                .setItems(mAllProfiles, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Launcher.updateSharedPrefsProfile(mAllProfiles[i]);
                        chosenProfile = mAllProfiles[i];
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        builder.show();
    }

}
