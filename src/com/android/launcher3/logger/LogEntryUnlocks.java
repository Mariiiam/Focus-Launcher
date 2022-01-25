package com.android.launcher3.logger;

import java.io.Serializable;
import java.util.List;

public class LogEntryUnlocks implements Serializable {
    String profile;
    List<String> usedApps;
    long startTime;
    List<String> profilesDuringUnlocks;

    public LogEntryUnlocks(){}

    public LogEntryUnlocks(String profile, List<String> usedApps, long startTime, List<String> profilesDuringUnlocks) {
        this.profile = profile;
        this.usedApps = usedApps;
        this.startTime = startTime;
        this.profilesDuringUnlocks = profilesDuringUnlocks;
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

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public List<String> getProfilesDuringUnlocks() {
        return profilesDuringUnlocks;
    }

    public void setProfilesDuringUnlocks(List<String> profilesDuringUnlocks) {
        this.profilesDuringUnlocks = profilesDuringUnlocks;
    }
}
