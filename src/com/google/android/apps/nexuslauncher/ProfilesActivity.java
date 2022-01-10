/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.nexuslauncher;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.android.launcher3.AddProfileDialogActivity;
import com.android.launcher3.ChangeProfileNameDialogActivity;
import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.QuestionGrayscaleDialog;
import com.android.launcher3.R;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.logger.FirebaseLogger;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.TimePreferenceActivity;
import com.android.launcher3.views.DependentSwitchPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class ProfilesActivity extends Activity {

    public final static String MINIMAL_DESIGN_PREF = "_minimal_design";
    public final static String WALLPAPER_BTN_CLICKED = "wallpaper_btn_clicked";
    public final static String CHANGE_NAME_PREF = "change_profile_name";
    static boolean changeWallpaper = false;
    private static Preference addProfilePref;
    public static final String ADD_PROFILE_PREF = "add_profile";
    final static int NEW_PROFILE_ADDED = 44;
    final static int PROFILE_NAME_CHANGE = 45;
    private static final int MAX_PROFILE_NUMBER = 8;
    public static int currentProfileNumber;
    public static final String PROFILES_MANAGED = "profiles_managed";
    public static ArrayList<String> newAddedProfiles;
    public static final int GRAYSCALE_CLICKED = 47;
    public static final int NOTIFICATION_BLOCKING_ALLOWED = 48;
    public static boolean grayscale_on = false;
    public static final String GRAYSCALE_PREF ="grayscale_pref";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Launcher.mSharedPrefs.getStringSet(ADD_PROFILE_PREF, null)==null){
            newAddedProfiles = new ArrayList<>();
        } else {
            Set set = Launcher.mSharedPrefs.getStringSet(ADD_PROFILE_PREF, null);
            newAddedProfiles = new ArrayList<>(set);
        }

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new ProfilesSettingsFragment())
                    .commit();
        }

        Set grayscaleInfos = Launcher.mSharedPrefs.getStringSet(GRAYSCALE_PREF, null);
        if(grayscaleInfos==null){
            grayscaleInfos = new HashSet<String>();
            grayscaleInfos.add("work_false");
            grayscaleInfos.add("home_false");
            grayscaleInfos.add("disconnected_false");
            grayscaleInfos.add("default_false"); //Todo check for added profiles
            for(String newAddedProfile : newAddedProfiles){
                grayscaleInfos.add(newAddedProfile.substring(1)+"_false");
            }
            Launcher.mSharedPrefs.edit().putStringSet(GRAYSCALE_PREF, grayscaleInfos).apply();
        }
    }

    public static void bindChangeNamePreference(String profile, Preference changeNamePref){
        if(changeNamePref!=null){
            if(Launcher.mSharedPrefs.getString(Launcher.CURRENT_PROFILE_PREF, "default").equals(profile)){
                changeNamePref.setEnabled(false);
                changeNamePref.setSummary(R.string.change_name_pref_not_active);
            } else {
                changeNamePref.setEnabled(true);
                changeNamePref.setSummary("");
            }
        }
    }

    public static void bindWallpaperPreference(String profile, Preference wallpaperPref){
        if(wallpaperPref!=null){
            String currentProfile = Launcher.mSharedPrefs.getString(Launcher.CURRENT_PROFILE_PREF, null);
            if(currentProfile!=null){
                if(currentProfile.equals(profile)){
                    wallpaperPref.setEnabled(true);
                    wallpaperPref.setSummary(R.string.wallpaper_pref_active);
                } else {
                    wallpaperPref.setEnabled(false);
                    wallpaperPref.setSummary(R.string.wallpaper_pref_not_active);
                }
            }
        }
    }

    public static void bindAlarmSummaryPreference(String profile, Preference alarmPref){
        Set set = Launcher.mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
        if(set!=null){
            ArrayList<String> alarmsList = new ArrayList<>(set);
            for (String eachSchedule : alarmsList){
                String profileNameInSchedule = eachSchedule.split("_")[0];
                if(profileNameInSchedule.equals(profile)){
                    String timeInfo = eachSchedule.split("_")[2];
                    if(timeInfo.split(":")[1].length()==1){
                        Character lastChar = timeInfo.charAt(timeInfo.length()-1);
                        timeInfo = timeInfo.substring(0,timeInfo.length()-1);
                        timeInfo = timeInfo+"0"+lastChar;
                    }
                    String daysInfo = eachSchedule.split("_")[1].substring(1, eachSchedule.split("_")[1].length()-1);
                    Context context = alarmPref.getContext();
                    String sumInfo = context.getString(R.string.every_string)+" "+daysInfo+" "+context.getString(R.string.at_string)+" "+timeInfo+" "+context.getString(R.string.clock_string);
                    alarmPref.setSummary(sumInfo);
                } else {
                    if(alarmPref!=null){
                        if(alarmPref.getSummary()==null){
                            alarmPref.setSummary(alarmPref.getContext().getString(R.string.summary_alarm_empty));
                        }
                    }
                }
            }
        } else {
            if(alarmPref!=null){
                if(alarmPref.getSummary()==null){
                    alarmPref.setSummary(alarmPref.getContext().getString(R.string.summary_alarm_empty));
                }
            }
        }
    }

    public static void saveGrayscaleInfo(boolean isEnabled){
        String grayscaleInfo = TimePreferenceActivity.selectedProfile+"_"+isEnabled;
        Set set = Launcher.mSharedPrefs.getStringSet(GRAYSCALE_PREF, null);
        if(set!=null){
            ArrayList<String> setAsArray = new ArrayList<>(set);
            String elementToRemove ="";
            boolean replaceInfo = false;
            for(String eachInfo : setAsArray){
                String profile = eachInfo.split("_")[0];
                if(profile.equals(grayscaleInfo.split("_")[0])){
                    elementToRemove = profile+"_"+!isEnabled;
                    replaceInfo = true;
                }
            }
            if(replaceInfo){
                setAsArray.remove(elementToRemove);
            }
            setAsArray.add(grayscaleInfo);
            Set set2 = new HashSet(setAsArray);
            Launcher.mSharedPrefs.edit().putStringSet(GRAYSCALE_PREF, set2).apply();
            FirebaseLogger firebaseLogger = FirebaseLogger.getInstance();
            //creating a user ID that is added to each log message in the Firebase database
            String userID = Launcher.mSharedPrefs.getString("userID_firebase", null);
            if(userID==null){
                userID = UUID.randomUUID().toString().substring(0,7);
                Launcher.mSharedPrefs.edit().putString("userID_firebase", userID).apply();
                firebaseLogger.setUserID(userID);
            }
            firebaseLogger.setUserID(userID);

            String profile = grayscaleInfo.split("_")[0];
            if(profile.length()>1){
                firebaseLogger.addLogMessage("events", "profile edited", profile+", grayscale edited, "+Launcher.getProfileSettings(profile));
            } else {
                for(String newAddedProfile:newAddedProfiles){
                    if(profile.equals(newAddedProfile.charAt(0)+"")){
                        profile = newAddedProfile.substring(1);
                        firebaseLogger.addLogMessage("events", "profile edited", profile+", grayscale edited, "+Launcher.getProfileSettings(newAddedProfile.charAt(0)+""));
                    }
                }
            }

        }
    }

    public static String getGrayscaleInfo(String profile){
        String isEnabled = "false";
        Set set = Launcher.mSharedPrefs.getStringSet(GRAYSCALE_PREF, null);
        if(set!=null){
            ArrayList<String> setAsArray = new ArrayList<>(set);
            for (String eachElement : setAsArray){
                if(eachElement.split("_")[0].equals(profile)){
                    isEnabled = eachElement.split("_")[1];
                    return isEnabled;
                }
            }
        }

        return isEnabled;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class ProfilesSettingsFragment extends PreferenceFragment {

        private PreferenceFragment parent;
        private Map<String, NotificationAccessObserver> mNotificationAccessObservers = new HashMap<>();
        private Map<String, GrayscaleAccessObserver> mGrayscaleAccessObservers = new HashMap<>();
        private SharedPreferences.OnSharedPreferenceChangeListener mCurrentProfileListener;
        private ScheduleChangeHandler mScheduleChangeHandler;
        private FirebaseLogger firebaseLogger;

        //public final static String[] availableProfiles = new String[]{"home", "work", "default", "disconnected"};
        public final static Map<String, Integer> resourceIdForProfileName = new HashMap<>();
        static {
            resourceIdForProfileName.put("home", R.string.profile_home);
            resourceIdForProfileName.put("work", R.string.profile_work);
            resourceIdForProfileName.put("disconnected", R.string.profile_disconnected);
            resourceIdForProfileName.put("default", R.string.profile_default);
        }

        public ProfilesSettingsFragment(PreferenceFragment parent) {
            super();
            this.parent = parent;
        }

        public ProfilesSettingsFragment() {
            super();
            this.parent = this;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(this.parent == this) super.onCreate(savedInstanceState);
            parent.getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            parent.addPreferencesFromResource(R.xml.profiles_preferences);

            if(Launcher.mSharedPrefs.getStringSet(ADD_PROFILE_PREF, null)==null){
                newAddedProfiles = new ArrayList<>();
                currentProfileNumber = 0;
            } else {
                Set set = Launcher.mSharedPrefs.getStringSet(ADD_PROFILE_PREF, null);
                newAddedProfiles = new ArrayList<>(set);
                currentProfileNumber = newAddedProfiles.size();
            }

            setupProfilePreferences();

            final Preference profilesGroup = parent.findPreference("profiles_screen");
            mCurrentProfileListener =
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        @Override
                        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                            if (key.equals(Launcher.CURRENT_PROFILE_PREF)) {
                                String profile = sharedPreferences.getString(key, "default");
                                //profilesGroup.setSummary(parent.getString(resourceIdForProfileName.get(profile)));
                                if(profile.equals("home") || profile.equals("work") || profile.equals("default") ||profile.equals("disconnected") ){
                                    profilesGroup.setSummary(parent.getString(resourceIdForProfileName.get(profile)));
                                } else {
                                    for(String sub : newAddedProfiles){
                                        if(sub.substring(1).equals(profile)){
                                            String profileID = sub.charAt(0)+"";
                                            profilesGroup.setSummary(profileID);
                                        }
                                    }
                                }
                                for (String p : Launcher.availableProfiles) {
                                    if(p.equals("home") || p.equals("work") || p.equals("default") || p.equals("disconnected")){
                                        final String pKey = "profile_"+p;
                                        Preference profileGroup = parent.findPreference(pKey);
                                        final String profileName = parent.getString(resourceIdForProfileName.get(p));
                                        profileGroup.setTitle(p.equals(profile) ? profileName + " (" + parent.getString(R.string.profile_active) +")" : profileName);

                                        //Preference wallpaperPref = parent.findPreference(p+"_choose_wallpaper");
                                        //bindWallpaperPreference(p, wallpaperPref);

                                        //Preference changeNamePref = parent.findPreference(p+"_change_name");
                                        //bindChangeNamePreference(p, changeNamePref);

                                    } else {
                                        for(String sub : newAddedProfiles){
                                            if(sub.substring(1).equals(p)){
                                                String profileID = sub.charAt(0)+"";
                                                final String pKey = "profile_"+profileID;
                                                Preference profileGroup = parent.findPreference(pKey);
                                                final String profileName = p;
                                                profileGroup.setTitle(p.equals(profile) ? profileName + " (" + parent.getString(R.string.profile_active) +")" : profileName);

                                                //Preference wallpaperPref = parent.findPreference(profileID+"_choose_wallpaper");
                                                //bindWallpaperPreference(p, wallpaperPref);

                                                //Preference changeNamePref = parent.findPreference(profileID+"_change_name");
                                                //bindChangeNamePreference(p, changeNamePref);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    };
            parent.getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mCurrentProfileListener);
            mCurrentProfileListener.onSharedPreferenceChanged(parent.getPreferenceManager().getSharedPreferences(), Launcher.CURRENT_PROFILE_PREF);

            mScheduleChangeHandler = new ScheduleChangeHandler();
            Launcher.mSharedPrefs.registerOnSharedPreferenceChangeListener(mScheduleChangeHandler);

            if (this.parent == this) Launcher.hasWritePermission(parent.getActivity(), true);
        }

        /**
         * Bind the summaries of EditText/List/Dialog/Ringtone preferences
         * to their values. When their values change, their summaries are
         * updated to reflect the new value, per the Android Design
         * guidelines.
         */
        private void setupProfilePreferences() {
            firebaseLogger = FirebaseLogger.getInstance();
            addProfilePref = parent.findPreference(ADD_PROFILE_PREF);
            addProfilePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if(currentProfileNumber<MAX_PROFILE_NUMBER){
                        Intent intent = new Intent(parent.getActivity(), AddProfileDialogActivity.class);
                        startActivityForResult(intent, NEW_PROFILE_ADDED);
                    } else {
                        Toast.makeText(parent.getActivity(), R.string.warning_max_number_profiles_reached, Toast.LENGTH_SHORT).show();
                    }
                    return true;
                }
            });
            for (final String profile : Launcher.availableProfiles) {
                if(profile.equals("work")||profile.equals("home")||profile.equals("default")||profile.equals("disconnected")){
                    Preference profileGroup = parent.findPreference("profile_" + profile);
                    profileGroup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            TimePreferenceActivity.selectedProfile = profile;
                            return true;
                        }
                    });

                    Preference ringtonePref = parent.findPreference(profile + "_ringtone");
                    bindPreferenceToSummary(ringtonePref);

                    Preference notificationSoundPref = parent.findPreference(profile + "_notification_sound");
                    bindPreferenceToSummary(notificationSoundPref);

                    Preference notificationBlockingPref = parent.findPreference(profile + "_hide_notifications");

                    observeNotificationBlockingSwitch(profile, (DependentSwitchPreference) notificationBlockingPref);

                    //Preference grayscalePref = parent.findPreference(profile + "_enable_grayscale");
                    //observeGrayscaleSwitch(profile, (DependentSwitchPreference) grayscalePref);
                    Preference grayscalePref = parent.findPreference(profile + "_enable_grayscale");
                    //observeGrayscalePref(profile, (Preference) grayscalePref);
                    grayscalePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            Intent intent = new Intent(getActivity(), QuestionGrayscaleDialog.class);
                            //startActivityForResult(intent, GRAYSCALE_CLICKED);
                            startActivity(intent);
                            return true;
                        }
                    });

                    Preference minimalDesignPref = parent.findPreference(profile + "_minimal_design");
                    minimalDesignPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            firebaseLogger.addLogMessage("events", "profile edited", profile+", minimal design edited, "+Launcher.getProfileSettings(profile));
                            return true;
                        }
                    });

                    Preference wallpaperPref = parent.findPreference(profile+"_choose_wallpaper");
                    bindWallpaperPreference(profile, wallpaperPref);
                    wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            changeWallpaper = Launcher.mSharedPrefs.getBoolean(WALLPAPER_BTN_CLICKED, false);
                            if(changeWallpaper){
                                Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, false).commit();
                                changeWallpaper = false;
                            } else {
                                Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, true).commit();
                                changeWallpaper = true;
                            }
                            return true;
                        }
                    });

                    Preference ssidsPref = parent.findPreference(profile + "_ssids");
                    if (ssidsPref.isEnabled()) bindPreferenceToOwnAndParentSummary(ssidsPref, profileGroup);

                    Preference schedulePref = parent.findPreference(profile + "_schedule");
                    bindAlarmSummaryPreference(profile, schedulePref);
                } else {
                    for(String sub : newAddedProfiles){
                        if(sub.substring(1).equals(profile)){
                            final String profileID = sub.charAt(0)+"";
                            Preference profileGroup = parent.findPreference("profile_" + profileID);
                            profileGroup.setEnabled(true);
                            profileGroup.setIcon(R.drawable.ic_profiles);
                            profileGroup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    TimePreferenceActivity.selectedProfile = profileID;
                                    return true;
                                }
                            });

                            Preference ringtonePref = parent.findPreference(profileID + "_ringtone");
                            bindPreferenceToSummary(ringtonePref);

                            Preference notificationSoundPref = parent.findPreference(profileID + "_notification_sound");
                            bindPreferenceToSummary(notificationSoundPref);

                            Preference notificationBlockingPref = parent.findPreference(profileID + "_hide_notifications");

                            observeNotificationBlockingSwitch(profile, (DependentSwitchPreference) notificationBlockingPref);

                            //Preference grayscalePref = parent.findPreference(profileID + "_enable_grayscale");
                            //observeGrayscaleSwitch(profileID, (DependentSwitchPreference) grayscalePref);
                            Preference grayscalePref = parent.findPreference(profileID + "_enable_grayscale");
                            //observeGrayscalePref(profileID, (Preference) grayscalePref);
                            grayscalePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    Intent intent = new Intent(getActivity(), QuestionGrayscaleDialog.class);
                                    //startActivityForResult(intent, GRAYSCALE_CLICKED);
                                    startActivity(intent);
                                    return true;
                                }
                            });

                            Preference minimalDesignPref = parent.findPreference(profileID + "_minimal_design");
                            minimalDesignPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    firebaseLogger.addLogMessage("events", "profile edited", profile+", minimal design edited, "+Launcher.getProfileSettings(profileID));
                                    return true;
                                }
                            });

                            Preference wallpaperPref = parent.findPreference(profileID+"_choose_wallpaper");
                            bindWallpaperPreference(profile, wallpaperPref);
                            wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                changeWallpaper = Launcher.mSharedPrefs.getBoolean(WALLPAPER_BTN_CLICKED, false);
                                if(changeWallpaper){
                                    Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, false).commit();
                                    changeWallpaper = false;
                                } else {
                                    Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, true).commit();
                                    changeWallpaper = true;
                                }
                                getActivity().finish();
                                return true;
                                }
                            });

                            Preference changeNamePref = parent.findPreference(profileID+"_change_name");
                            bindChangeNamePreference(profile, changeNamePref);
                            changeNamePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                                @Override
                                public boolean onPreferenceClick(Preference preference) {
                                    Launcher.mSharedPrefs.edit().putString(CHANGE_NAME_PREF, profile).commit();
                                    Intent intent = new Intent(getActivity(), ChangeProfileNameDialogActivity.class);
                                    startActivityForResult(intent, PROFILE_NAME_CHANGE);
                                    return true;
                                }
                            });

                            Preference ssidsPref = parent.findPreference(profileID + "_ssids");
                            if (ssidsPref.isEnabled()) bindPreferenceToOwnAndParentSummary(ssidsPref, profileGroup);

                            Preference schedulePref = parent.findPreference(profileID + "_schedule");
                            bindAlarmSummaryPreference(profileID, schedulePref);
                        }
                    }

                }

            }
        }

        private void observeNotificationBlockingSwitch(final String profile, DependentSwitchPreference switchPreference) {
            NotificationAccessObserver observer = new NotificationAccessObserver(switchPreference,
                    parent.getActivity().getContentResolver(), parent.getFragmentManager());
            mNotificationAccessObservers.put(profile, observer);
        }
/*
        private void observeGrayscaleSwitch(final String profile, final DependentSwitchPreference switchPreference) {
            switchPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                        if(switchPreference.isChecked()){
                            switchPreference.setChecked(false);
                            Launcher.changeGrayscaleSetting((Activity) preference.getContext());
                        }
                        else{
                            switchPreference.setChecked(true);
                            Launcher.changeGrayscaleSetting((Activity) preference.getContext());
                        }

                    return false;
                }
            });
            GrayscaleAccessObserver observer = new GrayscaleAccessObserver(switchPreference, 
                    parent.getActivity().getContentResolver(), parent.getFragmentManager());
            mGrayscaleAccessObservers.put(profile, observer);
        }
*/
        private void observeGrayscalePref(final String profile, final Preference gPreference){
            gPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Launcher.changeGrayscaleSetting((Activity) preference.getContext());
                    return true;
                }
            });
            //boolean isColorCorrectionEnabled = Launcher.isAccessibilityEnabled(getActivity());
        }
        /**
         * Binds a preference's summary to its value. More specifically, when the
         * preference's value is changed, its summary (line of text below the
         * preference title) is updated to reflect the value. The summary is also
         * immediately updated upon calling this method. The exact display format is
         * dependent on the type of preference.
         *
         * @see #sBindPreferenceSummaryToValueListener
         */
        private void bindPreferenceToSummary(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    parent.getPreferenceManager().getSharedPreferences().getString(preference.getKey(), ""));
        }

        private void bindPreferenceToOwnAndParentSummary(Preference preference, final Preference parent) {
            Preference.OnPreferenceChangeListener listener = new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object value) {
                    sBindPreferenceSummaryToValueListener.onPreferenceChange(preference, value);
                    parent.setSummary(preference.getSummary());
                    return true;
                }
            };

            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(listener);

            // Trigger the listener immediately with the preference's
            // current value.
            listener.onPreferenceChange(preference,
                    parent.getPreferenceManager().getSharedPreferences().getString(preference.getKey(), ""));
        }

        private class ScheduleChangeHandler implements SharedPreferences.OnSharedPreferenceChangeListener {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(key.equals(TimePreferenceActivity.SCHEDULE_PREF)){
                    Set set = Launcher.mSharedPrefs.getStringSet(TimePreferenceActivity.SCHEDULE_PREF, null);
                    if(set!=null){
                        for (final String profile : Launcher.availableProfiles) {
                            if(profile.equals("disconnected") || profile.equals("default")){
                                //do nothing
                            } else if(profile.equals("work")){
                                Preference alarmPref = findPreference("work_schedule");
                                bindAlarmSummaryPreference(profile, alarmPref);
                            } else if(profile.equals("home")){
                                Preference alarmPref = findPreference("home_schedule");
                                bindAlarmSummaryPreference(profile, alarmPref);
                            }
                            else {
                                for(String sub : newAddedProfiles){
                                    String profileID = sub.charAt(0)+"";
                                    Preference alarmPref = findPreference(profileID+"_schedule");
                                    bindAlarmSummaryPreference(profileID, alarmPref);
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                Log.i(ProfilesActivity.class.getSimpleName(), preference.getKey() + " has changed to " + stringValue);
                
                if (preference instanceof ListPreference) {
                    // For list preferences, look up the correct display value in
                    // the preference's 'entries' list.
                    ListPreference listPreference = (ListPreference) preference;
                    int index = listPreference.findIndexOfValue(stringValue);

                    // Set the summary to reflect the new value.
                    preference.setSummary(
                            index >= 0
                                    ? listPreference.getEntries()[index]
                                    : null);

                } else if (preference instanceof RingtonePreference) {
                    Uri ringtoneUri;
                    // For ringtone preferences, look up the correct display value
                    // using RingtoneManager.
                    if (TextUtils.isEmpty(stringValue)) {
                        // Empty values correspond to 'silent' (no ringtone).
                        preference.setSummary(R.string.profile_pref_ringtone_silent);
                        ringtoneUri = null;

                    } else {
                        ringtoneUri = Uri.parse(stringValue);
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                preference.getContext(), ringtoneUri);

                        if (ringtone == null) {
                            // Clear the summary if there was a lookup error.
                            preference.setSummary(null);
                            return true;
                        } else {
                            // Set the summary to reflect the new ringtone display
                            // name.
                            String name = ringtone.getTitle(preference.getContext());
                            preference.setSummary(name);
                        }
                    }
                    // Set ringtone
                    String profile = preference.getKey().split("_")[0];
                    String current_profile = preference.getSharedPreferences().getString(Launcher.CURRENT_PROFILE_PREF, "");
                    if (current_profile.equals(profile)) {
                        int type = ((RingtonePreference) preference).getRingtoneType();
                        Launcher.setRingtone(ringtoneUri, (Activity) preference.getContext(), type);
                    } else if(newAddedProfiles.size()!=0){
                        for(String sub : newAddedProfiles){
                            if((sub.charAt(0)+"").equals(profile)){
                                int type = ((RingtonePreference) preference).getRingtoneType();
                                Launcher.setRingtone(ringtoneUri, (Activity) preference.getContext(), type);
                            }
                        }
                    }
                } else {
                    // For all other preferences, set the summary to the value's
                    // simple string representation.
                    preference.setSummary(stringValue);
                }
                return true;
            }
        };

        @Override
        public void onDestroy() {
            if (mCurrentProfileListener != null) {
                parent.getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(mCurrentProfileListener);
                mCurrentProfileListener = null;
            }
            if (mNotificationAccessObservers != null) {
                for (NotificationAccessObserver observer : mNotificationAccessObservers.values()) {
                    observer.unregister();
                }
                mNotificationAccessObservers.clear();
                mNotificationAccessObservers = null;
            }
            if (mGrayscaleAccessObservers != null) {
                for (GrayscaleAccessObserver observer : mGrayscaleAccessObservers.values()) {
                    observer.unregister();
                }
                mGrayscaleAccessObservers.clear();
                mGrayscaleAccessObservers = null;
            }
            if(mScheduleChangeHandler!=null){
                Launcher.mSharedPrefs.unregisterOnSharedPreferenceChangeListener(mScheduleChangeHandler);
            }
            if(this.parent == this) super.onDestroy();
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if(requestCode == NEW_PROFILE_ADDED){
                if(resultCode == Activity.RESULT_OK){
                    final String result = data.getStringExtra("result");
                    if(result.equals("already_exists")){
                        Toast.makeText(parent.getActivity(), R.string.error_change_profile_name_already_exists, Toast.LENGTH_LONG).show();
                    } else if(result.equals("too_short")){
                        Toast.makeText(parent.getActivity(), R.string.error_change_profile_name_too_short, Toast.LENGTH_LONG).show();
                    } else {
                        //Firebase Logging
                        firebaseLogger = FirebaseLogger.getInstance();
                        currentProfileNumber += 1;
                        Launcher.availableProfiles.add(result);
                        Set set2 = new HashSet(Launcher.availableProfiles);
                        Launcher.mSharedPrefs.edit().putStringSet(PROFILES_MANAGED, set2).commit();
                        newAddedProfiles.add(currentProfileNumber+result);
                        Set set1 = new HashSet(newAddedProfiles);
                        Launcher.mSharedPrefs.edit().putStringSet(ADD_PROFILE_PREF, set1).commit();

                        Preference profileGroup = parent.findPreference("profile_"+currentProfileNumber);
                        profileGroup.setTitle(result);
                        profileGroup.setIcon(R.drawable.ic_profiles);
                        profileGroup.setEnabled(true);
                        profileGroup.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                TimePreferenceActivity.selectedProfile = currentProfileNumber+"";
                                return true;
                            }
                        });

                        Preference ringtonePref = parent.findPreference(currentProfileNumber+"_ringtone");
                        bindPreferenceToSummary(ringtonePref);

                        Preference notificationSoundPref = parent.findPreference(currentProfileNumber+"_notification_sound");
                        bindPreferenceToSummary(notificationSoundPref);

                        Preference notificationBlockingPref = parent.findPreference(currentProfileNumber+"_hide_notifications");
                        observeNotificationBlockingSwitch(currentProfileNumber+"", (DependentSwitchPreference) notificationBlockingPref);

                        Preference grayscalePref = parent.findPreference(currentProfileNumber+"_enable_grayscale");
                        //observeGrayscalePref(currentProfileNumber+"", (Preference) grayscalePref);
                        grayscalePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                Intent intent = new Intent(getActivity(), QuestionGrayscaleDialog.class);
                                //startActivityForResult(intent, GRAYSCALE_CLICKED);
                                startActivity(intent);
                                return true;
                            }
                        });
                        Set grayscaleSet = Launcher.mSharedPrefs.getStringSet(GRAYSCALE_PREF, null);
                        if(grayscaleSet!=null){
                            ArrayList<String> setAsArray = new ArrayList<>(grayscaleSet);
                            setAsArray.add(currentProfileNumber+"_false");
                            Set set3 = new HashSet(setAsArray);
                            Launcher.mSharedPrefs.edit().putStringSet(GRAYSCALE_PREF, set3).apply();
                        }

                        Preference minimalDesignPref = parent.findPreference(currentProfileNumber+"_minimal_design");
                        minimalDesignPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                firebaseLogger.addLogMessage("events", "profile edited", result+", minimal design edited, "+Launcher.getProfileSettings(currentProfileNumber+""));
                                return true;
                            }
                        });

                        Preference wallpaperPref = parent.findPreference(currentProfileNumber+"_choose_wallpaper");
                        bindWallpaperPreference(result, wallpaperPref);
                        wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                changeWallpaper = Launcher.mSharedPrefs.getBoolean(WALLPAPER_BTN_CLICKED, false);
                                if(changeWallpaper){
                                    Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, false).commit();
                                    changeWallpaper = false;
                                } else {
                                    Launcher.mSharedPrefs.edit().putBoolean(WALLPAPER_BTN_CLICKED, true).commit();
                                    changeWallpaper = true;
                                }
                                return true;
                            }
                        });

                        Preference ssidsPref = parent.findPreference(currentProfileNumber+"_ssids");
                        if (ssidsPref.isEnabled()) bindPreferenceToOwnAndParentSummary(ssidsPref, profileGroup);

                        Preference schedulePref = parent.findPreference(currentProfileNumber + "_schedule");
                        bindAlarmSummaryPreference(currentProfileNumber+"", schedulePref);

                        Preference changeNamePref = parent.findPreference(currentProfileNumber+"_change_name");
                        bindChangeNamePreference(result, changeNamePref);
                        changeNamePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                            @Override
                            public boolean onPreferenceClick(Preference preference) {
                                String profileID = preference.getKey().charAt(0)+"";
                                for(String sub : newAddedProfiles){
                                    if((sub.charAt(0)+"").equals(profileID)){
                                        String profile = sub.substring(1);
                                        Launcher.mSharedPrefs.edit().putString(CHANGE_NAME_PREF, profile).commit();
                                        Intent intent = new Intent(getActivity(), ChangeProfileNameDialogActivity.class);
                                        startActivityForResult(intent, PROFILE_NAME_CHANGE);
                                    }
                                }
                                return true;
                            }
                        });

                        //creating a user ID that is added to each log message in the Firebase database
                        String userID = Launcher.mSharedPrefs.getString("userID_firebase", null);
                        if(userID==null){
                            userID = UUID.randomUUID().toString().substring(0,7);
                            Launcher.mSharedPrefs.edit().putString("userID_firebase", userID).apply();
                            firebaseLogger.setUserID(userID);
                        }
                        firebaseLogger.setUserID(userID);

                        String ssIDInfo = "ssID: {"+ssidsPref.getSummary()+"}, ";
                        String scheduleInfo = schedulePref.getSummary()+"";
                        if(scheduleInfo.equals(getString(R.string.summary_alarm_empty))){
                            scheduleInfo = "schedule: {}, ";
                        } else {
                            scheduleInfo = "schedule: {"+schedulePref.getSummary()+"}, ";
                        }
                        String ringtoneInfo = "ringtone: "+ringtonePref.getSummary()+", ";
                        String notificationSoundInfo = "notification sound: "+notificationSoundPref.getSummary()+", ";
                        String notificationBlockInfo = "notifications blocked: "+((DependentSwitchPreference) notificationBlockingPref).isChecked()+", ";
                        String minimalDesignInfo = "minimal design on: "+Launcher.mSharedPrefs.getBoolean(currentProfileNumber+MINIMAL_DESIGN_PREF, false)+", ";
                        String homescreenAppsInfo ="homesreen apps: empty, ";
                        String wallpaperInfo = "wallpaper: "+Launcher.getWallpaperInfo(currentProfileNumber+"")+", ";
                        String grayscaleInfo = "grayscale on: "+getGrayscaleInfo(currentProfileNumber+"");

                        String profileSetting = ssIDInfo+scheduleInfo+ringtoneInfo+notificationSoundInfo+notificationBlockInfo+minimalDesignInfo+homescreenAppsInfo+wallpaperInfo+grayscaleInfo;

                        //*********** <timestamp> - profile added - <profilename> - <profilesettings>
                        firebaseLogger.addLogMessage("events", "profile added", result+", "+profileSetting);
                    }
                }
            } else if (requestCode == PROFILE_NAME_CHANGE) {
                if(resultCode == RESULT_OK) {
                    final String result = data.getStringExtra("result");
                    if(result.equals("already_exists")){
                        Toast.makeText(parent.getActivity(), R.string.error_change_profile_name_already_exists, Toast.LENGTH_SHORT).show();
                    } else if(result.equals("too_short")){
                        Toast.makeText(parent.getActivity(), R.string.error_change_profile_name_too_short, Toast.LENGTH_SHORT).show();
                    } else {
                        String changedProfile = Launcher.mSharedPrefs.getString(CHANGE_NAME_PREF, null);
                        String profileID = new String();
                        for(String sub : newAddedProfiles){
                            if(sub.substring(1).equals(changedProfile)){
                                profileID = sub.charAt(0)+"";
                            }
                        }
                        if(profileID!=null){
                            Preference profileGroup = parent.findPreference("profile_"+profileID);
                            profileGroup.setTitle(result);
                            int i = Launcher.availableProfiles.indexOf(changedProfile);
                            Launcher.availableProfiles.set(i, result);
                            newAddedProfiles.add(profileID+result);
                            Set set1 = new HashSet(newAddedProfiles);
                            Launcher.mSharedPrefs.edit().putStringSet(ADD_PROFILE_PREF, set1).commit();
                            firebaseLogger = FirebaseLogger.getInstance();
                            firebaseLogger.addLogMessage("events", "profile edited", "new name: "+result+", old name: "+changedProfile+", profile name edited, "+Launcher.getProfileSettings(profileID));
                        }
                    }
                }
            } else if(requestCode == GRAYSCALE_CLICKED){
                /*
                final String result = data.getStringExtra("result");
                if(result.equals("on")){
                    grayscale_on = true;
                    Log.d("---", "grayscale on");
                } else if(result.equals("off")){
                    grayscale_on = false;
                    Log.d("---", "grayscale off");
                }
                 */
            }
        }
    }

    /**
     * Content observer which listens for system notification setting changes,
     * and updates the launcher notification blocking setting subtext accordingly.
     */
    public static class NotificationAccessObserver extends SettingsActivity.NotificationAccessObserver {
        public NotificationAccessObserver(
                DependentSwitchPreference switchPreference,
                ContentResolver resolver,
                FragmentManager fragmentManager
        ) {
            super(switchPreference, resolver, fragmentManager);
            this.register(SettingsActivity.NOTIFICATION_ENABLED_LISTENERS);
        }

        @Override
        protected int getSummary(boolean serviceEnabled, boolean settingEnabled) {
            return serviceEnabled ? R.string.hide_foreign_notifications_summary : R.string.title_missing_notification_access;
        }

        @Override
        protected void showAccessConfirmation(FragmentManager fragmentManager) {
            new NotificationAccessConfirmation().show(fragmentManager, "notification_access");
        }
    }

    public static class GrayscaleAccessObserver extends SettingsActivity.GrayscaleAccessObserver {
        public GrayscaleAccessObserver(
                DependentSwitchPreference switchPreference,
                ContentResolver resolver,
                FragmentManager fragmentManager
        ) {
            super(switchPreference, resolver, fragmentManager);
            this.register(SettingsActivity.ENABLED_GRAYSCALE_LISTENERS);
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        private FirebaseLogger firebaseLogger;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access_for_blocking,
                    context.getString(R.string.derived_app_name));
            return new AlertDialog.Builder(context)
                    .setTitle(R.string.title_missing_notification_access)
                    .setMessage(msg)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.title_change_settings, this)
                    .create();
        }

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            ComponentName cn = new ComponentName(getActivity(), NotificationListener.class);
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(":settings:fragment_args_key", cn.flattenToString());
            getActivity().startActivityForResult(intent, NOTIFICATION_BLOCKING_ALLOWED);
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if(requestCode==NOTIFICATION_BLOCKING_ALLOWED){
                if(resultCode == RESULT_OK) {
                    firebaseLogger = FirebaseLogger.getInstance();
                    firebaseLogger.addLogMessage("events", "profile edited", "notification blocking edited, " + Launcher.getProfileSettings(TimePreferenceActivity.selectedProfile));
                }
            }
        }
    }
}

