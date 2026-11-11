package com.badilika.mobile;
import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
public class TrackerService extends Service {
    private Handler handler=new Handler();private Thread worker;private volatile boolean alive;
    private final Runnable tick=new Runnable(){public void run(){if(!alive)return;if(worker==null||!worker.isAlive()){worker=new Thread(()->sample());worker.start();}handler.postDelayed(this,60000);}};
    public int onStartCommand(Intent i,int f,int id){startForeground(21,Notices.make(this,"Badilika • usage journal","Saving daily history on this phone · check every minute",true));if(!alive){alive=true;handler.post(tick);}return START_STICKY;}
    private void sample(){if(!TimerLocks.limited(this).isEmpty()&&(!History.access(this)||!AppLimitService.enforcing(this)))Notices.send(this,29,"App timers need attention","Restore Usage access and automatic closing in Badilika to enforce your limits.");try(History db=new History(this)){
        db.collect(2);String day=History.day(System.currentTimeMillis());List<History.Row> today=db.rows(day,day,"","Time");Map<String,Long> usage=new HashMap<>();for(History.Row r:today)usage.put(r.pkg,r.ms);
        SharedPreferences p=getSharedPreferences("goals",0);SharedPreferences sent=getSharedPreferences("sent",0);
        for(Map.Entry<String,?> entry:p.getAll().entrySet())if(entry.getValue() instanceof String){AppBudget budget=AppBudget.parse((String)entry.getValue());String pkg=entry.getKey();int goal=budget.minimumMinutes,limit=budget.repeats()?0:budget.limitMinutes;long used=usage.getOrDefault(pkg,0L)/60000;String name=pkg;try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
            if(goal>0&&used>=goal)alert(sent,day+pkg+"goal",name+" • goal reached",used+" minutes today. Your daily minimum is "+goal+" minutes.");
            if(limit>0&&used>=limit)alert(sent,day+pkg+"limit",name+" • daily limit",used+" minutes today. Your chosen limit is "+limit+" minutes. Time for a break.");
            java.util.Calendar now=java.util.Calendar.getInstance();if(goal>0&&now.get(java.util.Calendar.HOUR_OF_DAY)>=20&&used<goal)alert(sent,day+pkg+"evening",name+" • daily goal",(goal-used)+" minutes left to reach your minimum today.");
        }
        for(String category:new String[]{"porn","escort"}){LockState lock=new LockState(this,category);if(lock.pending()&&lock.remaining()==0)alert(sent,category+getSharedPreferences("locks",0).getLong(category+".requestId",0),"Unblock delay complete",category+" protection remains active until you confirm in Badilika.");}
        getSharedPreferences("settings",0).edit().putLong("lastSync",System.currentTimeMillis()).remove("syncError").apply();
    }catch(Exception ex){getSharedPreferences("settings",0).edit().putString("syncError",ex.getMessage()==null?"Usage data unavailable":ex.getMessage()).apply();}}
    private void alert(SharedPreferences p,String key,String title,String body){if(!p.getBoolean(key,false)){Notices.send(this,key.hashCode(),title,body);p.edit().putBoolean(key,true).apply();}}
    public IBinder onBind(Intent i){return null;}
    public void onDestroy(){alive=false;handler.removeCallbacks(tick);super.onDestroy();}
}
