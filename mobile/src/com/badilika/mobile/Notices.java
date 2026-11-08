package com.badilika.mobile;
import android.app.*;
import android.content.*;
import android.os.Build;
public final class Notices {
    public static Notification make(Context c,String title,String text, boolean ongoing) {
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        String channel=ongoing?"status":"reminders";
        if(Build.VERSION.SDK_INT>=26){nm.createNotificationChannel(new NotificationChannel("status","Tracking & protection",NotificationManager.IMPORTANCE_LOW));nm.createNotificationChannel(new NotificationChannel("reminders","Daily app reminders",NotificationManager.IMPORTANCE_DEFAULT));}
        Notification.Builder builder=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,channel):new Notification.Builder(c);
        int pendingFlags=PendingIntent.FLAG_UPDATE_CURRENT;
        if(Build.VERSION.SDK_INT>=23)pendingFlags|=PendingIntent.FLAG_IMMUTABLE;
        return builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setOngoing(ongoing).setContentIntent(PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),pendingFlags)).build();
    }
    public static void send(Context c,int id,String title,String text) { try {c.getSystemService(NotificationManager.class).notify(id,make(c,title,text,false));}catch(SecurityException ignored){} }
}
