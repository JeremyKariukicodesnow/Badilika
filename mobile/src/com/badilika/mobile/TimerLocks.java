package com.badilika.mobile;

import android.content.*;
import java.util.*;

/** Every in-app path that loosens an allowance passes through the same persisted gate. */
public final class TimerLocks {
    private static final String PASS_PREFIX = "__v17_timer_pass__.";
    private static final String MIGRATED = "__v17_timer_pass_migrated__";
    private static final String RESET_AVAILABLE = "__v18_reset_available__";
    private static final String RESET_CLEANUP = "__v18_reset_cleanup__";
    private TimerLocks(){}
    /** Offer the reset once to people who already have saved app rules. */
    public static synchronized boolean prepareUpdateReset(Context c) {
        SharedPreferences goals = c.getSharedPreferences("goals", 0);
        finishResetCleanup(c);
        if (goals.contains(RESET_AVAILABLE)) return false;
        boolean existingRules = false;
        SharedPreferences.Editor edit = goals.edit().remove(MIGRATED);
        for (Map.Entry<String, ?> entry : goals.getAll().entrySet()) {
            if (entry.getValue() instanceof String) existingRules = true;
            if (entry.getKey().startsWith(PASS_PREFIX)) edit.remove(entry.getKey());
        }
        return edit.putBoolean(RESET_AVAILABLE, existingRules).commit() && existingRules;
    }

    public static boolean updateResetAvailable(Context c) {
        return c.getSharedPreferences("goals", 0).getBoolean(RESET_AVAILABLE, false);
    }

    public static synchronized boolean resetForUpdate(Context c) {
        if (!updateResetAvailable(c)) return false;
        SharedPreferences goals = c.getSharedPreferences("goals", 0);
        // Persist the empty timer list before removing locks. A restart can
        // finish cleanup without leaving existing rules temporarily unlocked.
        if (!goals.edit().clear().putBoolean(RESET_AVAILABLE, false)
                .putBoolean(RESET_CLEANUP, true).commit()) return false;
        return finishResetCleanup(c);
    }

    private static boolean finishResetCleanup(Context c) {
        SharedPreferences goals = c.getSharedPreferences("goals", 0);
        if (!goals.getBoolean(RESET_CLEANUP, false)) return true;
        SharedPreferences locks = c.getSharedPreferences("locks", 0);
        SharedPreferences.Editor cleanup = locks.edit();
        for (String key : locks.getAll().keySet()) {
            if (key.startsWith("timer.")) cleanup.remove(key);
        }
        return cleanup.commit() && goals.edit().remove(RESET_CLEANUP).commit();
    }
    public static LockState lock(Context c,String pkg){LockState l=new LockState(c,"timer."+pkg);if(!l.enabled())l.enable();return l;}
    public static boolean ready(Context c,String pkg){LockState l=lock(c,pkg);return l.pending()&&l.remaining()==0;}
    public static boolean relaxes(AppBudget old, AppBudget next) {
        return old.limitMinutes > 0 && (next == null || next.limitMinutes == 0
            || next.limitMinutes > old.limitMinutes || next.windowMinutes != old.windowMinutes);
    }
    public static synchronized boolean save(Context c,String pkg,AppBudget next,long delay){
        if (!finishResetCleanup(c)) throw new IllegalStateException("Timer reset cleanup could not finish");
        SharedPreferences p=c.getSharedPreferences("goals",0);AppBudget old=AppBudget.parse(p.getString(pkg,"0,0"));LockState l=lock(c,pkg);
        if (next.repeats()) {
            long anchor = old.windowMinutes == next.windowMinutes && old.windowAnchor > 0
                ? old.windowAnchor : System.currentTimeMillis();
            next = new AppBudget(next.minimumMinutes, next.limitMinutes, next.windowMinutes, anchor);
        }
        // A stricter daily limit can always take effect immediately. If the
        // same edit also shortens its lock, retain the existing lock duration.
        delay=TimePolicy.timerDelay(delay);
        if(old.limitMinutes>0&&next.limitMinutes>0&&next.limitMinutes<old.limitMinutes&&delay<l.delayMs()&&!ready(c,pkg))delay=l.delayMs();
        if(p.contains(pkg)&&old.encode().equals(next.encode())&&delay==l.delayMs())return true;
        boolean loosens = relaxes(old,next) || (old.limitMinutes>0 && delay<l.delayMs());
        if (loosens && !ready(c, pkg)) return false;
        if (!p.edit().putString(pkg, next.encode()).commit()) throw new IllegalStateException("Timer could not be saved");
        // A stricter allowance must not silently cancel a running unlock request.
        // A completed unlock is consumed only when the change actually relaxes it.
        if(loosens || delay != l.delayMs())l.cancel();
        l.setDelayMs(delay);return true;
    }
    public static synchronized boolean remove(Context c,String pkg){
        SharedPreferences goals = c.getSharedPreferences("goals", 0);
        AppBudget old = AppBudget.parse(goals.getString(pkg, "0,0"));
        if (relaxes(old, null) && !ready(c, pkg)) return false;
        if (!goals.edit().remove(pkg).commit())
            throw new IllegalStateException("Timer could not be removed");
        lock(c, pkg).cancel();
        return true;
    }
    public static List<String> limited(Context c){List<String> result=new ArrayList<>();for(Map.Entry<String,?> e:c.getSharedPreferences("goals",0).getAll().entrySet())if(e.getValue() instanceof String&&AppBudget.parse((String)e.getValue()).limitMinutes>0)result.add(e.getKey());return result;}
    public static boolean pause(Context c){List<String> apps=limited(c);for(String pkg:apps)if(!ready(c,pkg))return false;if(!c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit())throw new IllegalStateException("Could not pause timers");for(String pkg:apps)lock(c,pkg).cancel();return true;}
}
