package com.android.launcher3.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;

import java.util.ArrayList;

public class ManualProfileSelectionActivity extends Activity {

    String[] allProfiles = Launcher.getAllProfiles();
    ArrayList<String> allProfilesLabels = new ArrayList<String>();
    String chosenProfile;
    final static String ACTIVE_LABEL = " (Active)";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        for(int i = 0; i< allProfiles.length; i++){
            if(allProfiles[i].equals(Launcher.mSharedPrefs.getString(Launcher.CURRENT_PROFILE_PREF, "default"))){
                allProfilesLabels.add(allProfiles[i] + ACTIVE_LABEL);
            } else {
                allProfilesLabels.add(allProfiles[i]);
            }
        }
        String[] allProfilesLabelsCopy = new String[allProfilesLabels.size()];
        for (int j=0; j<allProfilesLabels.size(); j++) {
            allProfilesLabelsCopy[j] = allProfilesLabels.get(j);
        }
        AlertDialog builder = new AlertDialog.Builder(this, R.style.DialogAlert)
                .setTitle(R.string.title_manually_select_profile)
                .setItems(allProfilesLabelsCopy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Launcher.updateSharedPrefsProfile(allProfiles[i]);
                        chosenProfile = allProfiles[i];
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        builder.show();
    }

}
