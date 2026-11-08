package com.badilika.mobile;

import java.util.HashSet;
import java.util.Set;

/** Unions resumed activity intervals and clips them to the requested local-day window. */
public final class ForegroundClock {
    private final long start,end;
    private final Set<String> active=new HashSet<>();
    private long begin=-1,total;
    public ForegroundClock(long start,long end){this.start=start;this.end=Math.max(start,end);}
    public void resume(String activity,long time){if(active.isEmpty())begin=Math.max(start,time);active.add(activity);}
    public void pause(String activity,long time){active.remove(activity);if(active.isEmpty())close(time);}
    public void stopAll(long time){active.clear();close(time);}
    private void close(long time){if(begin>=0){total+=Math.max(0,Math.min(time,end)-begin);begin=-1;}}
    public long used(){return Math.min(end-start,total+(begin>=0?Math.max(0,end-begin):0));}
}
