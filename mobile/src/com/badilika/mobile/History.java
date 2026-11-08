package com.badilika.mobile;

import android.app.AppOpsManager;
import android.app.usage.*;
import android.content.*;
import android.content.pm.*;
import android.database.*;
import android.database.sqlite.*;
import android.net.ConnectivityManager;
import android.os.Process;
import java.util.*;
import java.text.*;

public class History extends SQLiteOpenHelper {
    private final Context c;
    public History(Context c){super(c,"history.db",null,2);this.c=c;}
    public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE daily(day TEXT,pkg TEXT,ms INTEGER,opens INTEGER,wifi INTEGER,mobile INTEGER,PRIMARY KEY(day,pkg))");db.execSQL("CREATE TABLE screens(day TEXT PRIMARY KEY,ms INTEGER,unlocks INTEGER)");db.execSQL("CREATE TABLE hours(day TEXT,hour INTEGER,ms INTEGER,PRIMARY KEY(day,hour))");}
    public void onUpgrade(SQLiteDatabase db,int old,int next){if(old<2)db.execSQL("CREATE TABLE hours(day TEXT,hour INTEGER,ms INTEGER,PRIMARY KEY(day,hour))");}
    public static long midnight(long time){Calendar d=Calendar.getInstance();d.setTimeInMillis(time);d.set(Calendar.HOUR_OF_DAY,0);d.set(Calendar.MINUTE,0);d.set(Calendar.SECOND,0);d.set(Calendar.MILLISECOND,0);return d.getTimeInMillis();}
    public static long nextDay(long time){Calendar d=Calendar.getInstance();d.setTimeInMillis(time);d.add(Calendar.DATE,1);return d.getTimeInMillis();}
    public static String day(long time){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date(time));}
    public static boolean access(Context c){return c.getSystemService(AppOpsManager.class).checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),c.getPackageName())==AppOpsManager.MODE_ALLOWED;}
    public static boolean deviceEvents(){return android.os.Build.VERSION.SDK_INT>=28;}
    public static final class Row {public String pkg,label;public long ms,opens,wifi,mobile; Row(String p){pkg=p;label=p;}}
    public synchronized void collect(int days){
        if(!access(c))throw new SecurityException("Allow Usage access in Settings first");
        long now=System.currentTimeMillis();Calendar cal=Calendar.getInstance();cal.setTimeInMillis(midnight(now));cal.add(Calendar.DATE,-days+1);
        for(long start=cal.getTimeInMillis();start<=now;start=nextDay(start)) collectDay(start,Math.min(nextDay(start),now));
    }
    private void collectDay(long start,long end){
        Map<String,Row> rows=new HashMap<>();Map<String,Long> active=new HashMap<>();
        UsageStatsManager manager=c.getSystemService(UsageStatsManager.class);
        // Include a preceding day to recover activities and interactive state spanning midnight.
        UsageEvents events=manager.queryEvents(start-86400000L,end);UsageEvents.Event ev=new UsageEvents.Event();
        long screenStart=-1,screenMs=0,unlocks=0;long[] hours=new long[24];
        while(events.hasNextEvent()){
            events.getNextEvent(ev);long t=ev.getTimeStamp();String pkg=ev.getPackageName();int type=ev.getEventType();
            if(type==UsageEvents.Event.ACTIVITY_RESUMED){
                if(!active.containsKey(pkg))active.put(pkg,Math.max(t,start));
                if(t>=start){Row r=rows.computeIfAbsent(pkg,Row::new);r.opens++;}
            }else if(type==UsageEvents.Event.ACTIVITY_PAUSED||type==UsageEvents.Event.ACTIVITY_STOPPED){
                Long a=active.remove(pkg);if(a!=null&&t>start){Row r=rows.computeIfAbsent(pkg,Row::new);r.ms+=Math.max(0,t-a);}
            }else if(type==UsageEvents.Event.SCREEN_INTERACTIVE){screenStart=Math.max(start,t);}
            else if(type==UsageEvents.Event.SCREEN_NON_INTERACTIVE||type==UsageEvents.Event.DEVICE_SHUTDOWN||type==UsageEvents.Event.DEVICE_STARTUP){if(screenStart>=0&&t>start){screenMs+=Math.max(0,t-screenStart);hourSpan(hours,screenStart,t);}screenStart=-1;
                for(Map.Entry<String,Long> a:active.entrySet())if(t>start)rows.computeIfAbsent(a.getKey(),Row::new).ms+=Math.max(0,t-a.getValue());active.clear();
            }else if(type==UsageEvents.Event.KEYGUARD_HIDDEN&&t>=start)unlocks++;
        }
        if(screenStart>=0){screenMs+=Math.max(0,end-screenStart);hourSpan(hours,screenStart,end);}
        for(Map.Entry<String,Long> a:active.entrySet())rows.computeIfAbsent(a.getKey(),Row::new).ms+=Math.max(0,end-a.getValue());
        for(Row r:rows.values())r.ms=Math.min(r.ms,end-start);
        network(rows,ConnectivityManager.TYPE_WIFI,start,end,true);network(rows,ConnectivityManager.TYPE_MOBILE,start,end,false);
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{for(Row r:rows.values()){
            String date=day(start);
            // Preserve archived maxima when Android has discarded some of its raw history.
            try(Cursor old=db.rawQuery("SELECT ms,opens,wifi,mobile FROM daily WHERE day=? AND pkg=?",new String[]{date,r.pkg})){if(old.moveToFirst()){r.ms=Math.max(r.ms,old.getLong(0));r.opens=Math.max(r.opens,old.getLong(1));r.wifi=Math.max(r.wifi,old.getLong(2));r.mobile=Math.max(r.mobile,old.getLong(3));}}
            ContentValues v=new ContentValues();v.put("day",date);v.put("pkg",r.pkg);v.put("ms",r.ms);v.put("opens",r.opens);v.put("wifi",r.wifi);v.put("mobile",r.mobile);db.insertWithOnConflict("daily",null,v,SQLiteDatabase.CONFLICT_REPLACE);
        }
        try(Cursor old=db.rawQuery("SELECT ms,unlocks FROM screens WHERE day=?",new String[]{day(start)})){if(old.moveToFirst()){screenMs=Math.max(screenMs,old.getLong(0));unlocks=Math.max(unlocks,old.getLong(1));}}
        ContentValues s=new ContentValues();s.put("day",day(start));s.put("ms",screenMs);s.put("unlocks",unlocks);db.insertWithOnConflict("screens",null,s,SQLiteDatabase.CONFLICT_REPLACE);
        for(int h=0;h<24;h++){try(Cursor old=db.rawQuery("SELECT ms FROM hours WHERE day=? AND hour=?",new String[]{day(start),""+h})){if(old.moveToFirst())hours[h]=Math.max(hours[h],old.getLong(0));}ContentValues v=new ContentValues();v.put("day",day(start));v.put("hour",h);v.put("ms",hours[h]);db.insertWithOnConflict("hours",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
        db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    private void hourSpan(long[] hours,long start,long end){while(start<end){Calendar c=Calendar.getInstance();c.setTimeInMillis(start);int h=c.get(Calendar.HOUR_OF_DAY);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);c.add(Calendar.HOUR_OF_DAY,1);long next=Math.min(end,c.getTimeInMillis());if(next<=start)break;hours[h]+=next-start;start=next;}}
    private void network(Map<String,Row> rows,int type,long start,long end,boolean wifi){
        NetworkStatsManager n=c.getSystemService(NetworkStatsManager.class);Map<Integer,String> packages=new HashMap<>();
        for(ApplicationInfo app:c.getPackageManager().getInstalledApplications(0)){String previous=packages.get(app.uid);packages.put(app.uid,previous==null?app.packageName:"uid:"+app.uid+" (shared)");}
        try(NetworkStats stats=n.querySummary(type,null,start,end)){
            NetworkStats.Bucket b=new NetworkStats.Bucket();while(stats.hasNextBucket()){stats.getNextBucket(b);String pkg=packages.getOrDefault(b.getUid(),"uid:"+b.getUid());Row r=rows.computeIfAbsent(pkg,Row::new);long bytes=b.getRxBytes()+b.getTxBytes();if(wifi)r.wifi+=bytes;else r.mobile+=bytes;}
        }catch(SecurityException|IllegalStateException ex){throw ex;}catch(Exception ex){throw new IllegalStateException("Android could not supply network statistics",ex);}
    }
    public List<Row> rows(String from,String to,String filter,String sort){
        List<Row> list=new ArrayList<>();String order=sort.equals("Data")?"SUM(wifi+mobile)":sort.equals("Opens")?"SUM(opens)":"SUM(ms)";
        try(Cursor cur=getReadableDatabase().rawQuery("SELECT pkg,SUM(ms),SUM(opens),SUM(wifi),SUM(mobile) FROM daily WHERE day>=? AND day<=? GROUP BY pkg ORDER BY "+order+" DESC",new String[]{from,to})){
            while(cur.moveToNext()){Row r=new Row(cur.getString(0));r.ms=cur.getLong(1);r.opens=cur.getLong(2);r.wifi=cur.getLong(3);r.mobile=cur.getLong(4);try{r.label=c.getPackageManager().getApplicationLabel(c.getPackageManager().getApplicationInfo(r.pkg,0)).toString();}catch(Exception ignored){}if((r.label+" "+r.pkg).toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT)))list.add(r);}}
        return list;
    }
    public long[] screen(String from,String to){try(Cursor cur=getReadableDatabase().rawQuery("SELECT SUM(ms),SUM(unlocks) FROM screens WHERE day>=? AND day<=?",new String[]{from,to})){return cur.moveToFirst()?new long[]{cur.getLong(0),cur.getLong(1)}:new long[2];}}
    public List<String[]> trend(String from,String to){List<String[]> list=new ArrayList<>();try(Cursor cur=getReadableDatabase().rawQuery("SELECT day,ms,unlocks FROM screens WHERE day>=? AND day<=? ORDER BY day",new String[]{from,to})){while(cur.moveToNext())list.add(new String[]{cur.getString(0),cur.getString(1),cur.getString(2)});}return list;}
    public List<String[]> hours(String from,String to){List<String[]> list=new ArrayList<>();long[] hours=new long[24];try(Cursor cur=getReadableDatabase().rawQuery("SELECT hour,SUM(ms) FROM hours WHERE day>=? AND day<=? GROUP BY hour",new String[]{from,to})){while(cur.moveToNext())hours[cur.getInt(0)]=cur.getLong(1);}for(int i=0;i<24;i++)list.add(new String[]{String.format(Locale.US,"%02d:00",i),""+hours[i],"0"});return list;}
    public static String time(long ms){long m=ms/60000;return m/60+"h "+m%60+"m";}
    public static String bytes(long b){return b>=1073741824?String.format(Locale.US,"%.2f GB",b/1073741824.0):String.format(Locale.US,"%.1f MB",b/1048576.0);}
}
