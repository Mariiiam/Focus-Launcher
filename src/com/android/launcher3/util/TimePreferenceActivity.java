package com.android.launcher3.util;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.preference.DialogPreference;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TimePreferenceActivity extends DialogPreference {

    private int lastHour=0;
    private int lastMinute=0;
    private static TimePicker picker=null;
    public final static Map<ToggleButton, Integer> daysViewToDataMap = new HashMap<>();
    public final static String SCHEDULE_PREF = "schedule_pref";
    public static String selectedProfile;
    public static ArrayList<String> scheduleList = new ArrayList<>();


    public TimePreferenceActivity(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.time_schedule_layout);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }
        setPositiveButtonText(R.string.save);
        setNegativeButtonText(R.string.cancel);
    }

    public TimePreferenceActivity(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setDialogLayoutResource(R.layout.time_schedule_layout);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }
        setPositiveButtonText(R.string.save);
        setNegativeButtonText(R.string.cancel);
    }

    public TimePreferenceActivity(Context context) {
        super(context);
        setDialogLayoutResource(R.layout.time_schedule_layout);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }
        setPositiveButtonText(R.string.save);
        setNegativeButtonText(R.string.cancel);
    }

    public TimePreferenceActivity(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.time_schedule_layout);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }
        setPositiveButtonText(R.string.save);
        setNegativeButtonText(R.string.cancel);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onBindDialogView(View v) {
        super.onBindDialogView(v);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }

        picker=(TimePicker)v.findViewById(R.id.time_picker);
        picker.setIs24HourView(true);

        daysViewToDataMap.clear();

        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_mon), Calendar.MONDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_tues), Calendar.TUESDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_weds), Calendar.WEDNESDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_thurs), Calendar.THURSDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_fri), Calendar.FRIDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_sat), Calendar.SATURDAY);
        daysViewToDataMap.put((ToggleButton) v.findViewById(R.id.btn_sun), Calendar.SUNDAY);

        Log.d("---", "selectedProfile: "+selectedProfile);

        if (scheduleList.size()!=0){
            for(String eachSchedule : scheduleList){
                String[] separatedInfo = eachSchedule.split("_");
                if(separatedInfo[0].equals(selectedProfile)){
                    String hourString = separatedInfo[2].split(":")[0];
                    String minString = separatedInfo[2].split(":")[1];
                    picker.setHour(Integer.parseInt(hourString));
                    picker.setMinute(Integer.parseInt(minString));
                    String sumDays = separatedInfo[1].substring(1, separatedInfo[1].length()-1);
                    String[] eachDay = sumDays.split(",");
                    for(ToggleButton btnView : daysViewToDataMap.keySet()){
                        for (String day : eachDay){
                            day = day.replace(" ", "");
                            if(btnView.getText().equals(day)){
                                btnView.setChecked(true);
                                btnView.setSelected(true);
                            }
                        }
                    }
                }
            }
        }
    }



    public static int getHour(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[0]));
    }

    public static int getMinute(String time) {
        String[] pieces=time.split(":");

        return(Integer.parseInt(pieces[1]));
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        String time=null;

        if (restoreValue) {
            if (defaultValue==null) {
                time=getPersistedString("00:00");
            }
            else {
                time=getPersistedString(defaultValue.toString());
            }
        }
        else {
            time=defaultValue.toString();
        }

        lastHour=getHour(time);
        lastMinute=getMinute(time);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(DialogInterface dialog, int which){
        if(which == DialogInterface.BUTTON_POSITIVE) {
            // do your stuff to handle positive button
            saveAlarm(getContext());
        }else if(which == DialogInterface.BUTTON_NEGATIVE){
            // do your stuff to handle negative button
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void saveAlarm(Context context) {
        ArrayList<String> selectedDays = getSelectedDays();
        if(selectedDays.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.msg_no_day_selected), Toast.LENGTH_SHORT).show();
        } else {
            String configSchedule = selectedProfile+"_"+selectedDays+"_"+picker.getHour()+":"+picker.getMinute();
            for(String eachSchedule: scheduleList){
                String profileSchedule = eachSchedule.split("_")[0];
                if(selectedProfile.equals(profileSchedule)){
                    scheduleList.remove(eachSchedule);
                }
            }
            scheduleList.add(configSchedule);
            Set set = new HashSet(scheduleList);
            Launcher.mSharedPrefs.edit().putStringSet(SCHEDULE_PREF, set).apply();
        }
    }

    private static ArrayList<String> getSelectedDays(){
        ArrayList<String> list = new ArrayList<>();
        for(ToggleButton btnView : daysViewToDataMap.keySet()){
            if(btnView.isChecked()){
                list.add(btnView.getText().toString());
            }
        }
        return list;
    }

}
