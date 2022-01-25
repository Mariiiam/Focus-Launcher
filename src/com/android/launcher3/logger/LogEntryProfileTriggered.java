package com.android.launcher3.logger;

import java.io.Serializable;

public class LogEntryProfileTriggered implements Serializable {
    String profile;
    String trigger;
    String triggerInfo;

    public LogEntryProfileTriggered(){}

    public LogEntryProfileTriggered(String profile, String trigger, String triggerInfo) {
        this.profile = profile;
        this.trigger = trigger;
        this.triggerInfo = triggerInfo;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getTrigger() {
        return trigger;
    }

    public void setTrigger(String trigger) {
        this.trigger = trigger;
    }

    public String getTriggerInfo() {
        return triggerInfo;
    }

    public void setTriggerInfo(String triggerInfo) {
        this.triggerInfo = triggerInfo;
    }
}
