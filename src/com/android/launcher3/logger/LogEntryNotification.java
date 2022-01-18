package com.android.launcher3.logger;

import java.io.Serializable;

public class LogEntryNotification implements Serializable {
    String profile;
    String appName;
    boolean isBlocked;

    public LogEntryNotification(){}

    public LogEntryNotification(String profile, String appName, boolean isBlocked) {
        this.profile = profile;
        this.appName = appName;
        this.isBlocked = isBlocked;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public boolean isBlocked() {
        return isBlocked;
    }

    public void setBlocked(boolean blocked) {
        isBlocked = blocked;
    }
}
