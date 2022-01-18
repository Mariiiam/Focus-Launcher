package com.android.launcher3.logger;

import java.io.Serializable;
import java.util.List;

public class LogEntryUnlocks implements Serializable {
    String profile;
    List<String> usedApps;
    List<String> usedShortcuts;

    public LogEntryUnlocks(){}

    public LogEntryUnlocks(String profile, List<String> usedApps, List<String> usedShortcuts) {
        this.profile = profile;
        this.usedApps = usedApps;
        this.usedShortcuts = usedShortcuts;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public List<String> getUsedApps() {
        return usedApps;
    }

    public void setUsedApps(List<String> usedApps) {
        this.usedApps = usedApps;
    }

    public List<String> getUsedShortcuts() {
        return usedShortcuts;
    }

    public void setUsedShortcuts(List<String> usedShortcuts) {
        this.usedShortcuts = usedShortcuts;
    }
}
