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

package com.android.launcher3;

import android.app.*;
import android.content.*;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.RingtonePreference;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;
import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.ButtonPreference;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {

    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    private static final String AT_A_GLANCE_KEY = "pref_smartspace";
    /** Hidden field Settings.Secure.NOTIFICATION_BADGING */
    public static final String NOTIFICATION_BADGING = "notification_badging";
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    private static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new LauncherSettingsFragment())
                    .commit();
        }
    }

    /**
     * This fragment shows the launcher preferences.
     */
    public static class LauncherSettingsFragment extends PreferenceFragment {

        private SystemDisplayRotationLockObserver mRotationLockObserver;
        private IconBadgingObserver mIconBadgingObserver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);

            ContentResolver resolver = getActivity().getContentResolver();

            // Setup profile preferences TODO: use Fragment for this and reuse in launcher_preferences or for button in overview
            setupProfilePreferences("home");
            setupProfilePreferences("work");
            setupProfilePreferences("default");
            setupProfilePreferences("disconnected");
            final Preference profilesGroup = findPreference("profiles_screen");
            final SharedPreferences.OnSharedPreferenceChangeListener listener =
                    new SharedPreferences.OnSharedPreferenceChangeListener() {
                        @Override
                        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                            if (key.equals("current_profile")) {
                                String profile = sharedPreferences.getString(key, "default");
                                String capitalizedProfileName = profile.substring(0, 1).toUpperCase() + profile.substring(1).toLowerCase();
                                profilesGroup.setSummary(capitalizedProfileName);
                            }
                        }
                    };
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(listener);
            listener.onSharedPreferenceChanged(getPreferenceManager().getSharedPreferences(), "current_profile");

                    // Setup allow rotation preference
                    Preference rotationPref = findPreference(Utilities.ALLOW_ROTATION_PREFERENCE_KEY);
            if (getResources().getBoolean(R.bool.allow_rotation)) {
                // Launcher supports rotation by default. No need to show this setting.
                getPreferenceScreen().removePreference(rotationPref);
            } else {
                mRotationLockObserver = new SystemDisplayRotationLockObserver(rotationPref, resolver);

                // Register a content observer to listen for system setting changes while
                // this UI is active.
                mRotationLockObserver.register(Settings.System.ACCELEROMETER_ROTATION);

                // Initialize the UI once
                rotationPref.setDefaultValue(Utilities.getAllowRotationDefaultValue(getActivity()));
            }

            ButtonPreference iconBadgingPref =
                    (ButtonPreference) findPreference(ICON_BADGING_PREFERENCE_KEY);
            if (!Utilities.ATLEAST_OREO) {
                getPreferenceScreen().removePreference(
                        findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
            }
            if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else {
                // Listen to system notification badge settings while this UI is active.
                mIconBadgingObserver = new IconBadgingObserver(
                        iconBadgingPref, resolver, getFragmentManager());
                mIconBadgingObserver.register(NOTIFICATION_BADGING, NOTIFICATION_ENABLED_LISTENERS);
            }

            Preference iconShapeOverride = findPreference(IconShapeOverride.KEY_PREFERENCE);
            if (iconShapeOverride != null) {
                if (IconShapeOverride.isSupported(getActivity())) {
                    IconShapeOverride.handlePreferenceUi((ListPreference) iconShapeOverride);
                } else {
                    getPreferenceScreen().removePreference(iconShapeOverride);
                }
            }
        }

        private enum OPEN_WALLPAPER_CODES {
            HOME(2), WORK(3), DISCONNECTED(4), DEFAULT(1);

            private int code;
            OPEN_WALLPAPER_CODES(int code) {
                this.code = code;
            }

            public static int getCodeForProfile(String profile) {
                for (OPEN_WALLPAPER_CODES enm : OPEN_WALLPAPER_CODES.values()) {
                    if (enm.name().toLowerCase().equals(profile)) {
                        return enm.code;
                    }
                }
                return -1;
            }

            public static String getProfileFromCode(int code) {
                for (OPEN_WALLPAPER_CODES enm : OPEN_WALLPAPER_CODES.values()) {
                    if (enm.code == code) {
                        return enm.name().toLowerCase();
                    }
                }
                return null;
            }
        }

        private void setupProfilePreferences(final String profile) {
            Preference profileGroup = findPreference("profile_"+profile);

            // Bind the summaries of EditText/List/Dialog/Ringtone preferences
            // to their values. When their values change, their summaries are
            // updated to reflect the new value, per the Android Design
            // guidelines.
            Preference ringtonePref = findPreference(profile + "_ringtone");
            bindPreferenceSummaryToValue(ringtonePref);
            Preference ssidsPref = findPreference(profile + "_ssids");
            if(ssidsPref.isEnabled()) bindPreferenceToParentSummary(ssidsPref, profileGroup);

            Preference wallpaperPref = findPreference(profile + "_wallpaper");
            wallpaperPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Toast.makeText(preference.getContext(), "Select Wallpaper", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("image/*");
                    LauncherSettingsFragment.this.startActivityForResult(intent, OPEN_WALLPAPER_CODES.getCodeForProfile(profile));
                    return true;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
            if (resultCode == RESULT_OK) {
                String profile = OPEN_WALLPAPER_CODES.getProfileFromCode(requestCode);
                if (profile != null && resultData != null) {
                    // this is the image selected by the user
                    Uri imageUri = resultData.getData();
                    Preference wallpaperPref = findPreference(profile + "_wallpaper");
                    Toast.makeText(wallpaperPref.getContext(), "Wallpaper URI: " + imageUri, Toast.LENGTH_SHORT).show();
                    wallpaperPref.getEditor().putString(wallpaperPref.getKey(), imageUri.toString()).apply();
                    wallpaperPref.
                    getPreferenceManager().getSharedPreferences()
                            .edit().putString(wallpaperPref.getKey(), imageUri.toString()).apply();

                    //TODO this is a workaround, normally the binding should work and update the summary!
                    wallpaperPref.setSummary(imageUri.toString());
                }
            }
            super.onActivityResult(requestCode, resultCode, resultData);
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
        private void bindPreferenceSummaryToValue(Preference preference) {
            // Set the listener to watch for value changes.
            preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

            // Trigger the listener immediately with the preference's
            // current value.
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    getPreferenceManager().getSharedPreferences().getString(preference.getKey(), ""));
        }

        private void bindPreferenceToParentSummary(Preference preference, final Preference parent) {
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
                    getPreferenceManager().getSharedPreferences().getString(preference.getKey(), ""));
        }

        /**
         * A preference value change listener that updates the preference's summary
         * to reflect its new value.
         */
        private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object value) {
                String stringValue = value.toString();
                Log.i(SettingsActivity.class.getSimpleName(), preference.getKey()+" has changed to "+ stringValue);

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
                    // For ringtone preferences, look up the correct display value
                    // using RingtoneManager.
                    if (TextUtils.isEmpty(stringValue)) {
                        // Empty values correspond to 'silent' (no ringtone).
                        preference.setSummary(R.string.profile_pref_ringtone_silent);

                    } else {
                        Ringtone ringtone = RingtoneManager.getRingtone(
                                preference.getContext(), Uri.parse(stringValue));

                        if (ringtone == null) {
                            // Clear the summary if there was a lookup error.
                            preference.setSummary(null);
                        } else {
                            // Set the summary to reflect the new ringtone display
                            // name.
                            String name = ringtone.getTitle(preference.getContext());
                            preference.setSummary(name);
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
            if (mRotationLockObserver != null) {
                mRotationLockObserver.unregister();
                mRotationLockObserver = null;
            }
            if (mIconBadgingObserver != null) {
                mIconBadgingObserver.unregister();
                mIconBadgingObserver = null;
            }
            super.onDestroy();
        }
    }

    /**
     * Content observer which listens for system auto-rotate setting changes, and enables/disables
     * the launcher rotation setting accordingly.
     */
    private static class SystemDisplayRotationLockObserver extends SettingsObserver.System {

        private final Preference mRotationPref;

        public SystemDisplayRotationLockObserver(
                Preference rotationPref, ContentResolver resolver) {
            super(resolver);
            mRotationPref = rotationPref;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            mRotationPref.setEnabled(enabled);
            mRotationPref.setSummary(enabled
                    ? R.string.allow_rotation_desc : R.string.allow_rotation_blocked_desc);
        }
    }

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    private static class IconBadgingObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final ButtonPreference mBadgingPref;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;
        private boolean serviceEnabled = true;

        public IconBadgingObserver(ButtonPreference badgingPref, ContentResolver resolver,
                FragmentManager fragmentManager) {
            super(resolver);
            mBadgingPref = badgingPref;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            int summary = enabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;

            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mBadgingPref.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
                if (!serviceEnabled) {
                    summary = R.string.title_missing_notification_access;
                }
            }
            mBadgingPref.setWidgetFrameVisible(!serviceEnabled);
            mBadgingPref.setOnPreferenceClickListener(serviceEnabled && Utilities.ATLEAST_OREO ? null : this);
            mBadgingPref.setSummary(summary);

        }

        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (!Utilities.ATLEAST_OREO && serviceEnabled) {
                ComponentName cn = new ComponentName(preference.getContext(), NotificationListener.class);
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(":settings:fragment_args_key", cn.flattenToString());
                preference.getContext().startActivity(intent);
            } else {
                new NotificationAccessConfirmation().show(mFragmentManager, "notification_access");
            }
            return true;
        }
    }

    public static class NotificationAccessConfirmation
            extends DialogFragment implements DialogInterface.OnClickListener {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            String msg = context.getString(R.string.msg_missing_notification_access,
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
