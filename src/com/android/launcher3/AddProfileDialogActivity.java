package com.android.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


public class AddProfileDialogActivity extends Activity {
    public static String newProfileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onStart() {
        super.onStart();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogAlert);
        builder.setTitle(getString(R.string.title_add_profile_name));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                newProfileName = input.getText().toString();
                boolean cont = true;
                if(newProfileName.length()==0||newProfileName.length()==1||newProfileName.length()==2){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",getString(R.string.error_change_profile_name_too_short));
                    setResult(Activity.RESULT_OK,returnIntent);
                    cont = false;
                }
                else if(cont){
                    for(String p : Launcher.availableProfiles){
                        if(newProfileName.equals(p)){
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("result",getString(R.string.error_change_profile_name_already_exists));
                            setResult(Activity.RESULT_OK,returnIntent);
                        }
                    }
                } else {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",newProfileName);
                    setResult(Activity.RESULT_OK,returnIntent);
                }
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
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
