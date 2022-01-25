package com.android.launcher3.logger;

import java.io.Serializable;
import java.util.List;

public class LogEntryProfileEdited implements Serializable {

    String profile;
    String editedEvent;
    List<String> ssid;
    List<String> schedule;
    String ringtone;
    String notificationSound;
    boolean notificationBlocked;
    boolean minimalDesignIsOn;
    List<String> homescreenApps;
    String wallpaper;
    boolean isGrayscaleOn;


    public LogEntryProfileEdited(){}

    public LogEntryProfileEdited(String profile, String editedEvent, List<String> ssid, List<String> schedule, String ringtone, String notificationSound, boolean notificationBlocked, boolean minimalDesignIsOn, List<String> homescreenApps, String wallpaper, boolean isGrayscaleOn) {
        this.profile = profile;
        this.editedEvent = editedEvent;
        this.ssid = ssid;
        this.schedule = schedule;
        this.ringtone = ringtone;
        this.notificationSound = notificationSound;
        this.notificationBlocked = notificationBlocked;
        this.minimalDesignIsOn = minimalDesignIsOn;
        this.homescreenApps = homescreenApps;
        this.wallpaper = wallpaper;
        this.isGrayscaleOn = isGrayscaleOn;
    }

    public String getProfile() {
        return profile;
    }

    public void setProfile(String profile) {
        this.profile = profile;
    }

    public String getEditedEvent() {
        return editedEvent;
    }

    public void setEditedEvent(String editedEvent) {
        this.editedEvent = editedEvent;
    }

    public List<String> getSsid() {
        return ssid;
    }

    public void setSsid(List<String> ssid) {
        this.ssid = ssid;
    }

    public List<String> getSchedule() {
        return schedule;
    }

    public void setSchedule(List<String> schedule) {
        this.schedule = schedule;
    }

    public String getRingtone() {
        return ringtone;
    }

    public void setRingtone(String ringtone) {
        this.ringtone = ringtone;
    }

    public String getNotificationSound() {
        return notificationSound;
    }

    public void setNotificationSound(String notificationSound) {
        this.notificationSound = notificationSound;
    }

    public boolean isNotificationBlocked() {
        return notificationBlocked;
    }

    public void setNotificationBlocked(boolean notificationBlocked) {
        this.notificationBlocked = notificationBlocked;
    }

    public boolean isMinimalDesignIsOn() {
        return minimalDesignIsOn;
    }

    public void setMinimalDesignIsOn(boolean minimalDesignIsOn) {
        this.minimalDesignIsOn = minimalDesignIsOn;
    }

    public List<String> getHomescreenApps() {
        return homescreenApps;
    }

    public void setHomescreenApps(List<String> homescreenApps) {
        this.homescreenApps = homescreenApps;
    }

    public String getWallpaper() {
        return wallpaper;
    }

    public void setWallpaper(String wallpaper) {
        this.wallpaper = wallpaper;
    }

    public boolean isGrayscaleOn() {
        return isGrayscaleOn;
    }

    public void setGrayscaleOn(boolean grayscaleOn) {
        isGrayscaleOn = grayscaleOn;
    }
}
