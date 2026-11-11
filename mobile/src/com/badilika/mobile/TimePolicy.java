package com.badilika.mobile;
/** Pure timing policy shared with host-side boundary tests. */
public final class TimePolicy {
    private TimePolicy(){}
    public static int days(int days){return Math.max(1,Math.min(30,days));}
    public static long delay(long duration){return Math.max(86400000L,Math.min(30*86400000L,duration));}
    public static long timerDelay(long duration){return Math.max(60000L,Math.min(30*86400000L,duration));}
    public static long remaining(long saved,long previousTick,long nowTick,int previousBoot,int nowBoot){
        long left=Math.max(0,saved);
        if(previousBoot!=nowBoot||nowTick<previousTick)return left;
        return Math.max(0,left-(nowTick-previousTick));
    }
}
