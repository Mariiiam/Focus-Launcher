package com.android.launcher3.util;

import android.app.AlarmManager;
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
import com.android.launcher3.alarm.AlarmModel;
import com.android.launcher3.alarm.AlarmReceiver;
import com.android.launcher3.logger.FirebaseLogger;
import com.google.android.apps.nexuslauncher.ProfilesActivity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ListIterator;
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

    private FirebaseLogger firebaseLogger;

    public TimePreferenceActivity(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setDialogLayoutResource(R.layout.time_schedule_layout);
        Set set = Launcher.mSharedPrefs.getStringSet(SCHEDULE_PREF, null);
        if(set!=null){
            scheduleList = new ArrayList<String>(set);
        }
        setPositiveButtonText(R.string.save);
        setNegativeButtonText(R.string.cancel);
        //Firebase Logging
        firebaseLogger = FirebaseLogger.getInstance();
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
        //Firebase Logging
        firebaseLogger = FirebaseLogger.getInstance();
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
        //Firebase Logging
        firebaseLogger = FirebaseLogger.getInstance();
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
        //Firebase Logging
        firebaseLogger = FirebaseLogger.getInstance();
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
            saveAlarm(getContext());
        }else if(which == DialogInterface.BUTTON_NEGATIVE){
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void saveAlarm(Context context) {
        picker.clearFocus();
        ArrayList<String> selectedDays = getSelectedDays();

        Set newAddedProfilesSet = Launcher.mSharedPrefs.getStringSet(ProfilesActivity.ADD_PROFILE_PREF, null);
        ArrayList<String> newAddedProfiles;
        if(newAddedProfilesSet != null){
            newAddedProfiles = new ArrayList<>(newAddedProfilesSet);
        } else {
            newAddedProfiles = new ArrayList<>();
        }

        //check if no days are selected. If so, then delete the alarm if the profile already has a schedule or show a message if it is a new schedule.
        if(selectedDays.isEmpty()) {
            boolean profileAlreadyScheduled = false;
            ArrayList<String> scheduleToDelete = new ArrayList<>();
            for(String eachProfile : scheduleList){
                String profileName = eachProfile.split("_")[0];
                if(profileName.equals(selectedProfile)){
                    profileAlreadyScheduled = true;
                    scheduleToDelete.add(eachProfile);
                }
            }
            if(profileAlreadyScheduled){
                scheduleList.remove(scheduleToDelete);
                AlarmModel alarmModel = new AlarmModel(selectedProfile, selectedDays, picker.getHour(), picker.getMinute());
                AlarmReceiver.cancelReminderAlarm(context, alarmModel);
                if(selectedProfile.length()==1){
                    for(String newAddedProfile : newAddedProfiles){
                        if((newAddedProfile.charAt(0)+"").equals(selectedProfile)){
                            firebaseLogger.addLogMessage("events", "profile edited", newAddedProfile.substring(1)+", schedule edited alarm deleted, "+Launcher.getProfileSettings(selectedProfile));
                        }
                    }
                } else {
                    firebaseLogger.addLogMessage("events", "profile edited", selectedProfile+", schedule edited alarm deleted, "+Launcher.getProfileSettings(selectedProfile));
                }
            } else {
                Toast.makeText(context, context.getString(R.string.msg_no_day_selected), Toast.LENGTH_SHORT).show();
            }
        }
        // save the alarm.
        else {
            String configSchedule = selectedProfile+"_"+selectedDays+"_"+picker.getHour()+":"+picker.getMinute();
            boolean scheduleIsSame = false;

            // handling if schedule already exist
            for (String eachProfile : scheduleList){
                if(scheduleIsSame(eachProfile, configSchedule)){
                    String profileSchedule = eachProfile.split("_")[0];
                    if(!selectedProfile.equals(profileSchedule)){
                        // New profile has the same schedule of another profile. Show toast message.
                        scheduleIsSame = true;
                        Toast.makeText(context, context.getString(R.string.msg_same_schedule)+" "+translateProfileName(context, profileSchedule), Toast.LENGTH_LONG).show();
                    }
                }
            }

            if(!scheduleIsSame){
                //check if profile already has a schedule. If so, then update its schedule.
                ArrayList<String> itemsToRemove = new ArrayList<>();
                for(String eachProfile : scheduleList){
                    String oldProfileName = configSchedule.split("_")[0];
                    String newProfileName = eachProfile.split("_")[0];
                    if(newProfileName.equals(oldProfileName)){
                        itemsToRemove.add(eachProfile);
                    }
                }
                if(itemsToRemove.size()!=0){
                    scheduleList.removeAll(itemsToRemove);
                }
                scheduleList.add(configSchedule);
                Set set = new HashSet(scheduleList);
                Launcher.mSharedPrefs.edit().putStringSet(SCHEDULE_PREF, set).apply();

                if(set!=null) {
                    for(String newAddedProfile : newAddedProfiles){
                        if(configSchedule.split("_")[0].equals(newAddedProfile.charAt(0)+"")){
                            firebaseLogger.addLogMessage("events", "profile edited", newAddedProfile.substring(1)+", schedule edited, "+Launcher.getProfileSettings(configSchedule.split("_")[0]));
                        }
                    }
                } else {
                    firebaseLogger.addLogMessage("events", "profile edited", configSchedule.split("_")[0]+", schedule edited, "+Launcher.getProfileSettings(configSchedule.split("_")[0]));
                }
                AlarmModel alarmModel = new AlarmModel(selectedProfile, selectedDays, picker.getHour(), picker.getMinute());
                AlarmReceiver.setReminderAlarm(context, alarmModel);
            }
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

    private static ArrayList<String> convertDaysStringToArrayList(String daysAsString){
        ArrayList<String> daysAsArrayList = new ArrayList<>();
        String sumDays = daysAsString.substring(1, daysAsString.length()-1);
        String[] eachDay = sumDays.split(",");
        for (String day : eachDay) {
            day = day.replace(" ", "");
            daysAsArrayList.add(day);
        }
        return daysAsArrayList;
    }

    // checks if two profiles has a same day and time
    private static boolean scheduleIsSame(String oldProfile, String newProfile){
        boolean scheduleIsSame = false;
        String newProfileHour = newProfile.split("_")[2].split(":")[0];
        String newProfileMinute = newProfile.split("_")[2].split(":")[1];
        String oldProfileHour = oldProfile.split("_")[2].split(":")[0];
        String oldProfileMinute = oldProfile.split("_")[2].split(":")[1];

        ArrayList<String> newDays = convertDaysStringToArrayList(newProfile.split("_")[1]);
        ArrayList<String> oldDays = convertDaysStringToArrayList(oldProfile.split("_")[1]);

        for(String eachDay : newDays){
            if(oldDays.contains(eachDay)){
                if(oldProfileHour.equals(newProfileHour) && oldProfileMinute.equals(newProfileMinute)){
                    // The schedule of the profile did not change
                    scheduleIsSame = true;
                    return scheduleIsSame;
                } else {
                    // The schedule of the profile did change
                    scheduleIsSame = false;
                }
            }
        }
        return scheduleIsSame;
    }

    private static String translateProfileName(Context context, String profile){
        String translatedName;
        if(profile.equals("work")){
            translatedName = context.getString(R.string.profile_work);
            return translatedName;
        } else if(profile.equals("home")){
            translatedName = context.getString(R.string.profile_home);
            return translatedName;
        } else {
            translatedName = profile;
            return translatedName;
        }
    }
}
