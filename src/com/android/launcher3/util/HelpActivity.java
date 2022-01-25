package com.android.launcher3.util;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.launcher3.R;
import com.google.android.apps.nexuslauncher.ProfilesActivity;

public class HelpActivity extends Activity implements PreferenceFragment.OnPreferenceStartFragmentCallback{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // Display the fragment as the main content.
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new HelpActivity.HelpFragment())
                    .commit();
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        Fragment instantiate = Fragment.instantiate(this, pref.getFragment(), pref.getExtras());
        getFragmentManager().beginTransaction().replace(android.R.id.content, instantiate).addToBackStack(pref.getKey()).commit();
        return true;
    }


    public static class AppsHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.apps_help_layout, container, false);
        }
    }

    public static class NotificationsHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.notifications_help_layout, container, false);
        }
    }

    public static class TriggersHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.trigger_help_layout, container, false);
        }
    }

    public static class SoundsHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.sounds_help_layout, container, false);
        }
    }

    public static class GrayscaleHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.grayscale_help_layout, container, false);
        }
    }

    public static class WallpaperHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.wallpaper_help_layout, container, false);
        }
    }

    public static class ProfilesHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.profiles_help_layout, container, false);
        }
    }

    public static class MinimalHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.minimal_help_layout, container, false);
        }
    }

    public static class OtherHelpFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return inflater.inflate(R.layout.other_help_layout, container, false);
        }
    }

    public static class HelpFragment extends PreferenceFragment {
        private PreferenceFragment parent;

        public HelpFragment(PreferenceFragment parent) {
            super();
            this.parent = parent;
        }

        public HelpFragment() {
            super();
            this.parent = this;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            if(this.parent == this ) super.onCreate(savedInstanceState);
            parent.addPreferencesFromResource(R.xml.help_screens);
        }

        @Override
        public void onDestroy() {
            if(this.parent == this ) super.onDestroy();
        }
    }
}
