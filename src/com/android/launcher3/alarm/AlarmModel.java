package com.android.launcher3.alarm;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Objects;

public class AlarmModel implements Parcelable {

    private final ArrayList<String> days;
    private final String profile;
    private final int hours;
    private final int minutes;
    private long timeInMillis;
    private final int id;

    public AlarmModel(String profile, ArrayList<String> days, int hours, int minutes){
        this.profile = profile;
        this.days = days;
        this.hours = hours;
        this.minutes = minutes;
        if(profile.equals("home")){
            this.id = 9;
        } else if(profile.equals("work")){
            this.id = 10;
        } else {
            this.id = Integer.parseInt(profile);
        }
    }

    protected AlarmModel(Parcel in) {
        days = in.readArrayList(String.class.getClassLoader());
        profile = in.readString();
        hours = in.readInt();
        minutes = in.readInt();
        timeInMillis = in.readLong();
        id = in.readInt();
    }

    public static final Creator<AlarmModel> CREATOR = new Creator<AlarmModel>() {
        @Override
        public AlarmModel createFromParcel(Parcel in) {
            return new AlarmModel(in);
        }

        @Override
        public AlarmModel[] newArray(int size) {
            return new AlarmModel[size];
        }
    };

    public int getHours() {
        return hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public String getProfile() {
        return profile;
    }

    public ArrayList<String> getDays() {
        return days;
    }

    public int getID(){
        return id;
    }

    public void setTime(long time) {
        this.timeInMillis = time;
    }

    public long getTime() {
        return timeInMillis;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeList(days);
        parcel.writeString(profile);
        parcel.writeInt(hours);
        parcel.writeInt(minutes);
        parcel.writeLong(timeInMillis);
        parcel.writeInt(id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(days, profile, hours, minutes, timeInMillis, id);
    }
}
