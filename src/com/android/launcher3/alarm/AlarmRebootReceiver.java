package com.android.launcher3.alarm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.android.launcher3.Launcher;
import com.android.launcher3.util.TimePreferenceActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AlarmRebootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals("android.intent.action.BOOT_COMPLETED")){
            Set<String> set = Launcher.mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
            if (set!=null){
                ArrayList<String> alarms = new ArrayList<>(set);
                for(String alarm : alarms){
                    String profile = alarm.split("_")[0];
                    List<String> schedule = Launcher.getSchedulePref(profile);
                    int hour = Integer.parseInt(schedule.get(schedule.size()-1).split(":")[0]);
                    int minute = Integer.parseInt(schedule.get(schedule.size()-1).split(":")[1]);
                    ArrayList<String> days = new ArrayList<>();
                    for(int i = 0; i<schedule.size()-1; i++){
                        days.add(schedule.get(i));
                    }
                    AlarmModel alarmModel = new AlarmModel(profile, days, hour, minute);
                    AlarmReceiver.setReminderAlarm(context, alarmModel);
                }
            }
        }
    }
}
