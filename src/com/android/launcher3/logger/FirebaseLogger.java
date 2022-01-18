package com.android.launcher3.logger;

import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseLogger {

    private static FirebaseLogger instance = new FirebaseLogger();

    private FirebaseLogger(){}

    public static FirebaseLogger getInstance(){
        return instance;
    }

    private String userID;
    private DatabaseReference database = FirebaseDatabase.getInstance(ConfigStore.databaseURL).getReference();

    public void setUserID(String userID){
        this.userID = userID;
    }

    public void addLogMessage(String databaseChild, String event, Object eventInfo){
        DatabaseReference reference = database.child(this.userID).child(databaseChild).push();
        reference.setValue(new LogEntry(System.currentTimeMillis(), this.userID, event, eventInfo));
    }

}
