package com.android.launcher3.logger;

import java.io.Serializable;

public class LogEntry implements Serializable {

    long timestamp;
    String userID;
    String event;
    Object eventInfo;

    public LogEntry(){}

    public LogEntry(long timestamp, String userID, String event, Object eventInfo){
        this.timestamp = timestamp;
        this.userID = userID;
        this.event = event;
        this.eventInfo = eventInfo;
    }


    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public Object getEventInfo() {
        return eventInfo;
    }

    public void setEventInfo(Object eventInfo) {
        this.eventInfo = eventInfo;
    }
}
