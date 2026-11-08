package com.badilika.mobile;

import android.content.*;
import android.os.SystemClock;
import android.provider.Settings;

/** Countdown only consumes monotonic powered-on time; changing the date cannot shorten it. */
public final class LockState {
    public static final long DAY = 86400000L;
    private final SharedPreferences p;
    private final Context c;
    private final String key;
    public LockState(Context c, String key) { this.c=c; this.key=key; p=c.getSharedPreferences("locks",0); }
    public boolean enabled() { return p.getBoolean(key+".enabled", false); }
    public void enable() { p.edit().putBoolean(key+".enabled",true).remove(key+".remaining").commit(); }
    public long delayMs() { return TimePolicy.delay(p.getLong(key+".delayMs",TimePolicy.days(p.getInt(key+".days",1))*DAY)); }
    public void setDelayMs(long delay) { if(!pending())p.edit().putLong(key+".delayMs",TimePolicy.delay(delay)).commit(); }
    public int days() { return (int)(delayMs()/DAY); }
    public void setDays(int days) { setDelayMs(TimePolicy.days(days)*DAY); }
    public boolean pending() { return p.contains(key+".remaining"); }
    private int boot() { return Settings.Global.getInt(c.getContentResolver(),Settings.Global.BOOT_COUNT,0); }
    public synchronized long remaining() {
        if (!pending()) return -1;
        long now=SystemClock.elapsedRealtime(), old=p.getLong(key+".tick",now);
        long left=p.getLong(key+".remaining",delayMs());
        left=TimePolicy.remaining(left,old,now,p.getInt(key+".boot",-1),boot());
        p.edit().putLong(key+".remaining",left).putLong(key+".tick",now).putInt(key+".boot",boot()).commit();
        return left;
    }
    public void request() { if(enabled()&&!pending()) p.edit().putLong(key+".requestId",System.currentTimeMillis()).putLong(key+".remaining",delayMs()).putLong(key+".tick",SystemClock.elapsedRealtime()).putInt(key+".boot",boot()).commit(); }
    public void cancel() { p.edit().remove(key+".remaining").commit(); }
    public boolean disable() { if(pending()&&remaining()==0) {p.edit().putBoolean(key+".enabled",false).remove(key+".remaining").commit();return true;} return false; }
    public static String duration(long ms) { long s=Math.max(0,ms/1000); return (s/86400)+"d "+String.format(java.util.Locale.US,"%02d:%02d:%02d",s/3600%24,s/60%60,s%60); }
}
