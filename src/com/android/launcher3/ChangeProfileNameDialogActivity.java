package com.android.launcher3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

public class ChangeProfileNameDialogActivity extends Activity {
    static String newProfileName;
    public static boolean cont = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogAlert);
        builder.setTitle(getString(R.string.change_profile_name_question));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                cont = true;
                newProfileName = input.getText().toString();
                if(newProfileName.length()>0){
                    if((""+newProfileName.charAt(newProfileName.length()-1)).equals(" ")){
                        newProfileName = newProfileName.substring(0,newProfileName.length()-1);
                    }
                }
                if(newProfileName.length()==0||newProfileName.length()==1||newProfileName.length()==2){
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result","too_short");
                    setResult(Activity.RESULT_OK,returnIntent);
                    cont = false;
                }
                for(String p : Launcher.availableProfiles){
                    if(newProfileName.equals(getString(R.string.profile_home)) || newProfileName.equals(getString(R.string.profile_disconnected)) || newProfileName.equals(getString(R.string.profile_default)) || newProfileName.equals(getString(R.string.profile_work))){
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("result","already_exists");
                        setResult(Activity.RESULT_OK,returnIntent);
                        cont = false;
                    } else if(newProfileName.equals(p)){
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
