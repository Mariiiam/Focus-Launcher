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
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.android.launcher3.graphics.IconShapeOverride;
import com.android.launcher3.notification.NotificationListener;
import com.android.launcher3.util.HelpActivity;
import com.android.launcher3.util.SettingsObserver;
import com.android.launcher3.views.DependentSwitchPreference;
import com.google.android.apps.nexuslauncher.ProfilesActivity;

/**
 * Settings activity for Launcher. Currently implements the following setting: Allow rotation
 */
public class SettingsActivity extends Activity {

    private static final String ICON_BADGING_PREFERENCE_KEY = "pref_icon_badging";
    private static final String AT_A_GLANCE_KEY = "pref_smartspace";
    /** Hidden field Settings.Secure.NOTIFICATION_BADGING */
    public static final String NOTIFICATION_BADGING = "notification_badging";
    /** Hidden field Settings.Secure.ENABLED_NOTIFICATION_LISTENERS */
    public static final String NOTIFICATION_ENABLED_LISTENERS = "enabled_notification_listeners";
    /** Hidden field Settings.Secure.ENABLED_GRAYSCALE_LISTENERS */
    public static final String ENABLED_GRAYSCALE_LISTENERS = "enabled_grayscale_listeners";

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
        private NotificationAccessObserver mIconBadgingObserver;
        //private ProfilesActivity.ProfilesSettingsFragment mProfileSettings = new ProfilesActivity.ProfilesSettingsFragment(this);
        private HelpActivity.HelpFragment mHelpFragment = new HelpActivity.HelpFragment(this);

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(LauncherFiles.SHARED_PREFERENCES_KEY);
            addPreferencesFromResource(R.xml.launcher_preferences);
            populateProfilesPlaceholder(savedInstanceState);

            // No smartspace
            getPreferenceScreen().removePreference(findPreference(AT_A_GLANCE_KEY));

            ContentResolver resolver = getActivity().getContentResolver();

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

            DependentSwitchPreference iconBadgingPref =
                    (DependentSwitchPreference) findPreference(ICON_BADGING_PREFERENCE_KEY);
            if (!Utilities.ATLEAST_OREO) {
                getPreferenceScreen().removePreference(
                        findPreference(SessionCommitReceiver.ADD_ICON_PREFERENCE_KEY));
            }
            if (!getResources().getBoolean(R.bool.notification_badging_enabled)) {
                getPreferenceScreen().removePreference(iconBadgingPref);
            } else {
                // Listen to system notification badge settings while this UI is active.
                mIconBadgingObserver = new NotificationAccessObserver(
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

        private void populateProfilesPlaceholder(Bundle savedInstanceState) {
            //PreferenceScreen profiles = (PreferenceScreen) findPreference("profiles_screen");
            PreferenceScreen help = (PreferenceScreen) findPreference("help_screen");
            PreferenceScreen settings = getPreferenceScreen();
            int lastSetting = settings.getPreferenceCount();
            //mProfileSettings.onCreate(savedInstanceState);
            mHelpFragment.onCreate(savedInstanceState);
            while (settings.getPreferenceCount () > lastSetting) {
                Preference p = settings.getPreference(lastSetting);
                settings.removePreference (p); // decreases the preference count
                //profiles.addPreference (p);
                help.addPreference(p);
            }
        }

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
            /*
            if (mProfileSettings != null) {
                mProfileSettings.onDestroy();
                mProfileSettings = null;
            }*/
            if (mHelpFragment != null) {
                mHelpFragment.onDestroy();
                mHelpFragment = null;
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
    public static class NotificationAccessObserver extends SettingsObserver.Secure
            implements Preference.OnPreferenceClickListener {

        private final DependentSwitchPreference mSwitchPreference;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;
        private boolean serviceEnabled = true;

        public NotificationAccessObserver(
                DependentSwitchPreference switchPreference,
                ContentResolver resolver,
                FragmentManager fragmentManager
        ) {
            super(resolver);
            mSwitchPreference = switchPreference;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners =
                        Settings.Secure.getString(mResolver, NOTIFICATION_ENABLED_LISTENERS);
                ComponentName myListener =
                        new ComponentName(mSwitchPreference.getContext(), NotificationListener.class);
                serviceEnabled = enabledListeners != null &&
                        (enabledListeners.contains(myListener.flattenToString()) ||
                                enabledListeners.contains(myListener.flattenToShortString()));
            }

            mSwitchPreference.setOnPreferenceClickListener(serviceEnabled ? null : this);
            mSwitchPreference.setDependencyResolved(serviceEnabled);

            mSwitchPreference.setSummary(getSummary(serviceEnabled, enabled));
        }

        protected int getSummary(boolean serviceEnabled, boolean settingEnabled) {
            return !serviceEnabled ? R.string.title_missing_notification_access :
                    (settingEnabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off);
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
                showAccessConfirmation(mFragmentManager);
            }
            return true; // click handled
        }

        protected void showAccessConfirmation(FragmentManager fragmentManager) {
            new NotificationAccessConfirmation().show(fragmentManager, "notification_access");
        }
    }

    /**
     * Content observer which listens for system badging setting changes,
     * and updates the launcher badging setting subtext accordingly.
     */
    public static class GrayscaleAccessObserver extends SettingsObserver.Secure 
        implements Preference.OnPreferenceChangeListener {

        private final DependentSwitchPreference mSwitchPreference;
        private final ContentResolver mResolver;
        private final FragmentManager mFragmentManager;

        public GrayscaleAccessObserver(
                DependentSwitchPreference switchPreference,
                ContentResolver resolver,
                FragmentManager fragmentManager
        ) {
            super(resolver);
            mSwitchPreference = switchPreference;
            mResolver = resolver;
            mFragmentManager = fragmentManager;
        }

        @Override
        public void onSettingChanged(boolean enabled) {
            /*if (enabled) {
                // Check if the listener is enabled or not.
                String enabledListeners = Settings.Secure.getString(mResolver, ENABLED_GRAYSCALE_LISTENERS);
            }
            mSwitchPreference.setOnPreferenceClickListener(null);
            mSwitchPreference.setDependencyResolved(true);
            mSwitchPreference.setSummary(getSummary(enabled));*/
        }

        protected int getSummary(boolean settingEnabled) {
            return settingEnabled ? R.string.icon_badging_desc_on : R.string.icon_badging_desc_off;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            
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
