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
    public static boolean cont = true;

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
                cont = true;
                newProfileName = input.getText().toString();
                if(newProfileName.length()==0||newProfileName.length()==1||newProfileName.length()==2){
                    Log.d("---", "add: too short name error");
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result","too_short");
                    setResult(Activity.RESULT_OK,returnIntent);
                    cont = false;
                }
                for(String p : Launcher.availableProfiles){
                    if(newProfileName.equals(getString(R.string.profile_home)) || newProfileName.equals(getString(R.string.profile_disconnected)) || newProfileName.equals(getString(R.string.profile_default)) || newProfileName.equals(getString(R.string.profile_work))){
                        Log.d("---", "here already exists");
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result","already_exists");
                        setResult(Activity.RESULT_OK,returnIntent);
                        cont = false;
                    }
                    else if(newProfileName.equals(p)){
                        Log.d("---", "add: same name error");
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result","already_exists");
                        setResult(Activity.RESULT_OK,returnIntent);
                        cont = false;
                    }
                }
                if(cont){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result",newProfileName);
                    setResult(Activity.RESULT_OK,returnIntent);
                    cont = true;
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
