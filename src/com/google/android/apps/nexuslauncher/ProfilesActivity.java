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

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.android.launcher3.Launcher;
import com.android.launcher3.LauncherFiles;
import com.android.launcher3.R;
import com.android.launcher3.SettingsActivity;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.views.DependentSwitchPreference;

import java.util.HashMap;
import java.util.Map;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class ProfilesActivity extends Activity {

    public final static String MINIMAL_DESIGN_PREF = "_minimal_design";
    private static Preference minimalDesignPref;
    private static Preference wallpaperPref;
    public final static String WALLPAPER_BTN_CLICKED = "wallpaper_btn_clicked";
    static boolean changeWallpaper = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new ProfilesSettingsFragment())
                    .commit();
        }
    }

    public static void bindWallpaperPreference(String profile){
        if(wallpaperPref!=null){
            if(Launcher.mSharedPrefs.getString(Launcher.CURRENT_PROFILE_PREF, "default").equals(profile)){
                wallpaperPref.setEnabled(true);
                wallpaperPref.setSummary(R.string.wallpaper_pref_active);
            } else {
                wallpaperPref.setEnabled(false);
                wallpaperPref.setSummary(R.string.wallpaper_pref_not_active);
            }
        }
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class ProfilesSettingsFragment extends PreferenceFragment {

        private PreferenceFragment parent;
        private Map<String, NotificationAccessObserver> mNotificationAccessObservers = new HashMap<>();
        private Map<String, GrayscaleAccessObserver> mGrayscaleAccessObservers = new HashMap<>();
        private SharedPreferences.OnSharedPreferenceChangeListener mCurrentProfileListener;

        public final static String[] availableProfiles = new String[]{"home", "work", "default", "disconnected"};
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

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(this.parent == this) super.onCreate(savedInstanceState);
            parent.getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            parent.addPreferencesFromResource(R.xml.profiles_preferences);

            setupProfilePreferences();

            final Preference profilesGroup = parent.findPreference("profiles_screen");
            mCurrentProfileListener =
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        @Override
                        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                            if (key.equals(Launcher.CURRENT_PROFILE_PREF)) {
                                String profile = sharedPreferences.getString(key, "default");
                                profilesGroup.setSummary(parent.getString(resourceIdForProfileName.get(profile)));

                                for (String p : availableProfiles) {
                                    final String pKey = "profile_"+p;
                                    Preference profileGroup = parent.findPreference(pKey);
                                    final String profileName = parent.getString(resourceIdForProfileName.get(p));
                                    profileGroup.setTitle(p.equals(profile) ? profileName + " (" + parent.getString(R.string.profile_active) +")" : profileName);
                                }
                            }
                        }
                    };
            parent.getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(mCurrentProfileListener);
            mCurrentProfileListener.onSharedPreferenceChanged(parent.getPreferenceManager().getSharedPreferences(), Launcher.CURRENT_PROFILE_PREF);

            if (this.parent == this) Launcher.hasWritePermission(parent.getActivity(), true);
        }

        /**
         * Bind the summaries of EditText/List/Dialog/Ringtone preferences
         * to their values. When their values change, their summaries are
         * updated to reflect the new value, per the Android Design
         * guidelines.
         */
        private void setupProfilePreferences() {
            for (String profile : availableProfiles) {
                Preference profileGroup = parent.findPreference("profile_" + profile);

                Preference ringtonePref = parent.findPreference(profile + "_ringtone");
                bindPreferenceToSummary(ringtonePref);

                Preference notificationSoundPref = parent.findPreference(profile + "_notification_sound");
                bindPreferenceToSummary(notificationSoundPref);

                Preference notificationBlockingPref = parent.findPreference(profile + "_hide_notifications");
                observeNotificationBlockingSwitch(profile, (DependentSwitchPreference) notificationBlockingPref);

                //Preference grayscalePref = parent.findPreference(profile + "_enable_grayscale");
                //observeGrayscaleSwitch(profile, (DependentSwitchPreference) grayscalePref);
                Preference grayscalePref = parent.findPreference(profile + "_enable_grayscale");
                observeGrayscalePref(profile, (Preference) grayscalePref);

                minimalDesignPref = parent.findPreference(profile + "_minimal_design");

                wallpaperPref = parent.findPreference(profile+"_choose_wallpaper");
                bindWallpaperPreference(profile);
                wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
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
            boolean isColorCorrectionEnabled = Launcher.isAccessibilityEnabled(getActivity());
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
            if(this.parent == this) super.onDestroy();
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
            getActivity().startActivity(intent);
        }
    }
}
