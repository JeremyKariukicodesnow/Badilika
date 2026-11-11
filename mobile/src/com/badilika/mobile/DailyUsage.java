package com.badilika.mobile;

import android.app.usage.*;
import android.content.Context;
import java.util.*;

/** Event intervals are clipped to today's local midnight; split activities are unioned. */
public final class DailyUsage {
    private DailyUsage(){}
    public static long read(Context c,String pkg,long now){
        return read(c, pkg, now, new AppBudget(0, 0));
    }
    public static long read(Context c, String pkg, long now, AppBudget budget) {
        long start = budget.windowStart(now, History.midnight(now));
        Calendar prior=Calendar.getInstance();prior.setTimeInMillis(start);prior.add(Calendar.DATE,-1);
        UsageEvents events=c.getSystemService(UsageStatsManager.class).queryEvents(prior.getTimeInMillis(),now);
        UsageEvents.Event event=new UsageEvents.Event();ForegroundClock clock=new ForegroundClock(start,now);
        while(events.hasNextEvent()){
            events.getNextEvent(event);long time=event.getTimeStamp();int type=event.getEventType();
            if(type==UsageEvents.Event.SCREEN_NON_INTERACTIVE||type==UsageEvents.Event.DEVICE_SHUTDOWN||type==UsageEvents.Event.DEVICE_STARTUP){
                clock.stopAll(time);continue;
            }
            if(!pkg.equals(event.getPackageName()))continue;
            String activity=String.valueOf(event.getClassName());
            if(type==UsageEvents.Event.ACTIVITY_RESUMED){clock.resume(activity,time);}
            else if(type==UsageEvents.Event.ACTIVITY_PAUSED||type==UsageEvents.Event.ACTIVITY_STOPPED){
                clock.pause(activity,time);
            }
        }
        return clock.used();
    }
}
