package com.android.launcher3.alarm;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.launcher3.Alarm;
import com.android.launcher3.Launcher;
import com.android.launcher3.util.TimePreferenceActivity;

import java.util.ArrayList;
import java.util.Set;

public class AlarmsService extends IntentService {

    private static final String TAG = AlarmsService.class.getSimpleName();
    public static final String ACTION_COMPLETE = TAG + ".ACTION_COMPLETE";
    static ArrayList<AlarmModel> alarmsList = new ArrayList<>();
    static ArrayList<String> alarmsIDList = new ArrayList<>();
    public static final String ALARMS_EXTRA = "alarms_extra";

    @SuppressWarnings("unused")
    public AlarmsService(){
        this(TAG);
    }

    public AlarmsService(String name) {
        super(name);
        updateAlarmsList();
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        updateAlarmsList();

        final Intent i = new Intent(ACTION_COMPLETE);
        ArrayList<AlarmModel> alarmsListClone = (ArrayList<AlarmModel>) alarmsList.clone();
        i.putParcelableArrayListExtra(ALARMS_EXTRA, alarmsListClone);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i);
        Log.d("---", "send broadcast "+i.getAction());
    }

    public static void launchAlarmsService(Context context) {
        final Intent launchAlarmsServiceIntent = new Intent(context, AlarmsService.class);
        context.startService(launchAlarmsServiceIntent);
    }

    public static void updateAlarmsList(){
        Set set = Launcher.mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
        if(set!=null){
            alarmsIDList = new ArrayList<>(set);
            for(String eachSchedule : alarmsIDList){
                String[] scheduleInfo = eachSchedule.split("_");
                String profile = scheduleInfo[0];
                int hours = Integer.parseInt(scheduleInfo[2].split(":")[0]);
                int minutes = Integer.parseInt(scheduleInfo[2].split(":")[1]);
                String sumDays = scheduleInfo[1].substring(1, scheduleInfo[1].length()-1);
                String[] eachDay = sumDays.split(",");
                ArrayList<String> days = new ArrayList<>();
                for(String day : eachDay){
                    day = day.replace(" ", "");
                    days.add(day);
                }
                AlarmModel alarm = new AlarmModel(profile, days, hours, minutes);
                alarmsList.add(alarm);
            }
        }
    }

}
