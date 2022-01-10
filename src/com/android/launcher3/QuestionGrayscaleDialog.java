package com.android.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.apps.nexuslauncher.ProfilesActivity;

public class QuestionGrayscaleDialog extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogAlert);
        builder.setTitle(getString(R.string.question_grayscale));
        builder.setPositiveButton(R.string.activate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*Intent returnIntent = new Intent();
                returnIntent.putExtra("result","on");
                setResult(Activity.RESULT_OK,returnIntent);*/

                ProfilesActivity.saveGrayscaleInfo(true);

                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e){
                    Log.e("Accessibility Settings", "Failed: ");
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton(R.string.deactivate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                /*Intent returnIntent = new Intent();
                returnIntent.putExtra("result","off");
                setResult(Activity.RESULT_OK,returnIntent);*/

                ProfilesActivity.saveGrayscaleInfo(false);

                try {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                } catch (Exception e){
                    Log.e("Accessibility Settings", "Failed: ");
                    e.printStackTrace();
                }
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                finish();
            }
        });

        builder.show();
    }
}
