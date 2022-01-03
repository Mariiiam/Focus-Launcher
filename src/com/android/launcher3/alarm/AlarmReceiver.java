package com.android.launcher3.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;

import com.android.launcher3.Launcher;
import com.android.launcher3.R;

import java.util.ArrayList;
import java.util.Calendar;

import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;

public class AlarmReceiver extends BroadcastReceiver {

        private static final String TAG = AlarmReceiver.class.getSimpleName();
        private static final String BUNDLE_EXTRA = "bundle_extra";
        private static final String ALARM_KEY = "alarm_key";
        public static final String CHANGE_PROFILE_ALARM = "change_profile_alarm";

        @Override
        public void onReceive(Context context, Intent intent) {
            final AlarmModel alarm = intent.getBundleExtra(BUNDLE_EXTRA).getParcelable(ALARM_KEY);
            Log.d("---", "on receive here? "+alarm);
            if(alarm == null) {
                Log.d("---", "alarm is null");
                Log.e(TAG, "Alarm is null", new NullPointerException());
                return;
            }
            Log.d("---", "update to profile "+alarm.getProfile());
            Launcher.mSharedPrefs.edit().putString(CHANGE_PROFILE_ALARM, alarm.getProfile()).apply();
            setReminderAlarm(context, alarm);
        }

        public static void setReminderAlarm(Context context, AlarmModel alarm){
            if(alarm.getDays().size() == 0){
                Log.d("---", "no alarm set");
                cancelReminderAlarm(context, alarm);
                return;
            }
            final Calendar nextAlarmTime = getTimeForNextAlarm(alarm, context);
            alarm.setTime(nextAlarmTime.getTimeInMillis());
            Log.d("---", "next alarm time: "+nextAlarmTime.getTime());

            final Intent intent = new Intent(context, AlarmReceiver.class);
            final Bundle bundle = new Bundle();
            bundle.putParcelable(ALARM_KEY, alarm);
            intent.putExtra(BUNDLE_EXTRA, bundle);

            final PendingIntent pendingIntent = PendingIntent.getBroadcast(context, alarm.getID(), intent, FLAG_UPDATE_CURRENT);

            ScheduleAlarm.with(context).schedule(alarm, pendingIntent);
        }

        private static Calendar getTimeForNextAlarm(AlarmModel alarm, Context context) {
            final Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MINUTE, alarm.getMinutes());
            calendar.set(Calendar.HOUR_OF_DAY, alarm.getHours());
            calendar.setTimeInMillis(calendar.getTimeInMillis());

            final long currentTime = System.currentTimeMillis();
            final int startIndex = getStartIndexFromTime(calendar);

            int count = 0;
            boolean isAlarmSetForDay;

            final SparseBooleanArray daysArray = getDaysBooleanArray(context, alarm);

            do {
                final int index = (startIndex + count) % 7;
                isAlarmSetForDay = daysArray.valueAt(index) && (calendar.getTimeInMillis() > currentTime);
                if(!isAlarmSetForDay) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1);
                    count++;
                }
            } while (!isAlarmSetForDay && count < 7);

            return calendar;
        }

        private static SparseBooleanArray getDaysBooleanArray(Context context, AlarmModel alarmModel){
            SparseBooleanArray daysBooleanArray = buildBaseArray();
            ArrayList<Integer> daysIntArray = new ArrayList<>();
            for (int i=0; i<alarmModel.getDays().size(); i++){
                daysIntArray.add(getDayInInt(context, alarmModel, i));
            }

            for(int j=0; j<daysIntArray.size(); j++){
                daysBooleanArray.put(daysIntArray.get(j), true);
            }

            return daysBooleanArray;
        }

        private static SparseBooleanArray buildBaseArray(){
            final int numDays = 7;
            SparseBooleanArray daysBooleanArray = new SparseBooleanArray(numDays);
            for(int i=1; i<=numDays; i++){
                daysBooleanArray.put(i, false);
            }
            return daysBooleanArray;
        }

        private static int getDayInInt(Context context, AlarmModel alarmModel, int index){
            String day = alarmModel.getDays().get(index);
            int calendarDay = 0;
            String monday = context.getString(R.string.day_su);
            if(day.equals(monday)){
                calendarDay = 1;
                return calendarDay;
            } else if(day.equals(context.getString(R.string.day_su))){
                calendarDay = 2;
                return calendarDay;
            } else if(day.equals(context.getString(R.string.day_tu))){
                calendarDay = 3;
                return calendarDay;
            } else if(day.equals(context.getString(R.string.day_w))){
                calendarDay = 4;
                return calendarDay;
            } else if(day.equals(context.getString(R.string.day_th))){
                calendarDay = 5;
                return calendarDay;
            } else if(day.equals(context.getString(R.string.day_f))){
                calendarDay = 6;
                return calendarDay;
            } else if (day.equals(context.getString(R.string.day_sa))){
                calendarDay = 7;
                return calendarDay;
            }
            return calendarDay;
        }

        private static int getStartIndexFromTime(Calendar calendar){
            final int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
            int startIndex = 0;
            switch (dayOfWeek){
                case Calendar.MONDAY:
                    startIndex = 0;
                    break;
                case Calendar.TUESDAY:
                    startIndex = 1;
                    break;
                case Calendar.WEDNESDAY:
                    startIndex = 2;
                    break;
                case Calendar.THURSDAY:
                    startIndex = 3;
                    break;
                case Calendar.FRIDAY:
                    startIndex = 4;
                    break;
                case Calendar.SATURDAY:
                    startIndex = 5;
                    break;
                case Calendar.SUNDAY:
                    startIndex = 6;
                    break;
            }
            return startIndex;
        }

        public static void cancelReminderAlarm(Context context, AlarmModel alarm){
            final Intent intent = new Intent(context, AlarmReceiver.class);
            final PendingIntent pIntent = PendingIntent.getBroadcast(
                    context,
                    alarm.getID(),
                    intent,
                    FLAG_UPDATE_CURRENT
            );

            final AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            manager.cancel(pIntent);
            Log.d("---", "alarm canceled");
        }

        private static class ScheduleAlarm {
            private final Context context;
            private final AlarmManager alarmManager;

            private ScheduleAlarm(Context context, AlarmManager alarmManager) {
                this.context = context;
                this.alarmManager = alarmManager;
            }

            static ScheduleAlarm with(Context context){
                final AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if(am == null){
                    throw new IllegalStateException("AlarmManager is null");
                }
                return new ScheduleAlarm(context, am);
            }

            void schedule(AlarmModel alarmModel, PendingIntent pendingIntent){
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmModel.getTime(), pendingIntent);
                Log.d("---", "send pending intent "+alarmModel.getHours()+":"+alarmModel.getMinutes());
            }

        }
}