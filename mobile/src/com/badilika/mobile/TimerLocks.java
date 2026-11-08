package com.badilika.mobile;

import android.content.*;
import java.util.*;

/** Every in-app path that loosens an allowance passes through the same persisted gate. */
public final class TimerLocks {
    private TimerLocks(){}
    public static LockState lock(Context c,String pkg){LockState l=new LockState(c,"timer."+pkg);if(!l.enabled())l.enable();return l;}
    public static boolean ready(Context c,String pkg){LockState l=lock(c,pkg);return l.pending()&&l.remaining()==0;}
    public static boolean relaxes(AppBudget old,AppBudget next){return old.limitMinutes>0&&(next==null||next.limitMinutes==0||next.limitMinutes>old.limitMinutes);}
    public static boolean save(Context c,String pkg,AppBudget next,long delay){
        SharedPreferences p=c.getSharedPreferences("goals",0);AppBudget old=AppBudget.parse(p.getString(pkg,"0,0"));LockState l=lock(c,pkg);
        // A stricter daily limit can always take effect immediately. If the
        // same edit also shortens its lock, retain the existing lock duration.
        if(old.limitMinutes>0&&next.limitMinutes>0&&next.limitMinutes<old.limitMinutes&&TimePolicy.delay(delay)<l.delayMs()&&!ready(c,pkg))delay=l.delayMs();
        if(p.contains(pkg)&&old.encode().equals(next.encode())&&TimePolicy.delay(delay)==l.delayMs())return true;
        if((relaxes(old,next)||(old.limitMinutes>0&&TimePolicy.delay(delay)<l.delayMs()))&&!ready(c,pkg))return false;
        if(!p.edit().putString(pkg,next.encode()).commit())throw new IllegalStateException("Timer could not be saved");
        // A stricter allowance must not silently cancel a running unlock request.
        // A completed unlock is consumed only when the change actually relaxes it.
        if(relaxes(old,next)||(old.limitMinutes>0&&TimePolicy.delay(delay)<l.delayMs()))l.cancel();
        l.setDelayMs(delay);return true;
    }
    public static boolean remove(Context c,String pkg){
        AppBudget old=AppBudget.parse(c.getSharedPreferences("goals",0).getString(pkg,"0,0"));
        if(relaxes(old,null)&&!ready(c,pkg))return false;
        if(!c.getSharedPreferences("goals",0).edit().remove(pkg).commit())throw new IllegalStateException("Timer could not be removed");lock(c,pkg).cancel();return true;
    }
    public static List<String> limited(Context c){List<String> result=new ArrayList<>();for(Map.Entry<String,?> e:c.getSharedPreferences("goals",0).getAll().entrySet())if(e.getValue() instanceof String&&AppBudget.parse((String)e.getValue()).limitMinutes>0)result.add(e.getKey());return result;}
    public static boolean pause(Context c){List<String> apps=limited(c);for(String pkg:apps)if(!ready(c,pkg))return false;if(!c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit())throw new IllegalStateException("Could not pause timers");for(String pkg:apps)lock(c,pkg).cancel();return true;}
}
