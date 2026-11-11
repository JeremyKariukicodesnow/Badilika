package com.badilika.mobile;

import android.accessibilityservice.*;
import android.app.KeyguardManager;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.telecom.TelecomManager;
import android.view.accessibility.*;
import android.view.inputmethod.*;
import android.widget.Toast;
import java.util.*;
import java.util.concurrent.*;

/** User-enabled app timers. Receives package events only, never reads window contents. */
public class AppLimitService extends AccessibilityService {
    private final Handler handler=new Handler();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Set<String> keyboards=new HashSet<>();
    private String foreground="";
    private volatile boolean busy,alive;
    private long lastHome;
    public static volatile boolean connected;
    public static boolean authorized(Context c){
        AccessibilityManager manager=c.getSystemService(AccessibilityManager.class);
        for(AccessibilityServiceInfo info:manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)){
            ServiceInfo service=info.getResolveInfo().serviceInfo;
            if(c.getPackageName().equals(service.packageName)&&AppLimitService.class.getName().equals(service.name))return true;
        }
        return false;
    }
    public static boolean enforcing(Context c){return authorized(c)&&History.access(c)&&c.getSharedPreferences("settings",0).getBoolean("limitsEnabled",false);}
    /** Keep system controls, calling, keyboards, and this app available. */
    public static boolean essential(Context c,String pkg){
        if(pkg==null||pkg.equals(c.getPackageName())||pkg.equals("android")||pkg.equals("com.android.systemui")||pkg.equals("com.android.settings")||pkg.contains("permissioncontroller")||pkg.contains("packageinstaller"))return true;
        ResolveInfo home=c.getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
        if(home!=null&&pkg.equals(home.activityInfo.packageName))return true;
        TelecomManager phone=c.getSystemService(TelecomManager.class);if(phone!=null&&pkg.equals(phone.getDefaultDialerPackage()))return true;
        for(InputMethodInfo keyboard:c.getSystemService(InputMethodManager.class).getEnabledInputMethodList())if(pkg.equals(keyboard.getPackageName()))return true;
        return pkg.equals("com.android.phone")||pkg.equals("com.android.server.telecom")||pkg.contains("emergency");
    }
    protected void onServiceConnected(){super.onServiceConnected();alive=true;connected=true;for(InputMethodInfo keyboard:getSystemService(InputMethodManager.class).getEnabledInputMethodList())keyboards.add(keyboard.getPackageName());handler.post(tick);}
    public void onAccessibilityEvent(AccessibilityEvent event){
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||event.getPackageName()==null)return;
        String pkg=event.getPackageName().toString();
        // A keyboard or notification shade does not replace the app underneath it.
        if(pkg.equals("com.android.systemui")||keyboards.contains(pkg))return;
        foreground=pkg;check();
    }
    private final Runnable tick=new Runnable(){public void run(){if(!alive)return;check();handler.postDelayed(this,2000);}};
    private void check(){
        if(!alive||busy||foreground.isEmpty()||!getSharedPreferences("settings",0).getBoolean("limitsEnabled",false)||!History.access(this))return;
        if(!getSystemService(PowerManager.class).isInteractive()||getSystemService(KeyguardManager.class).isKeyguardLocked())return;
        final String pkg=foreground;
        AppBudget budget=AppBudget.parse(getSharedPreferences("goals",0).getString(pkg,"0,0"));
        if(budget.limitMinutes==0||essential(this,pkg))return;
        busy=true;
        worker.execute(()->{
            try{
                final long now=System.currentTimeMillis(),used=DailyUsage.read(this,pkg,now,budget);
                handler.post(()->{
                    try{
                        if(!alive||!foreground.equals(pkg)||!getSystemService(PowerManager.class).isInteractive()||getSystemService(KeyguardManager.class).isKeyguardLocked()||!getSharedPreferences("settings",0).getBoolean("limitsEnabled",false))return;
                        // Re-read after the background query: editing/removing a timer applies immediately.
                        AppBudget current=AppBudget.parse(getSharedPreferences("goals",0).getString(pkg,"0,0"));
                        long currentTime = System.currentTimeMillis();
                        long periodStart = budget.windowStart(now, History.midnight(now));
                        if (current.windowMinutes != budget.windowMinutes || current.windowAnchor != budget.windowAnchor
                                || current.windowStart(currentTime, History.midnight(currentTime)) != periodStart
                                || !current.exhausted(used)) return;
                        if(SystemClock.elapsedRealtime()-lastHome<1200)return;
                        if(performGlobalAction(GLOBAL_ACTION_HOME)){
                            lastHome=SystemClock.elapsedRealtime();String name=pkg;try{name=getPackageManager().getApplicationLabel(getPackageManager().getApplicationInfo(pkg,0)).toString();}catch(Exception ignored){}
                            String reset = current.repeats() ? "Your allowance resets at the next window." : "Your daily allowance resets at midnight.";
                            Toast.makeText(this,name+": your "+AppBudget.label(current.limitMinutes)+" allowance is used. "+reset,Toast.LENGTH_LONG).show();
                            SharedPreferences sent=getSharedPreferences("sent",0);
                            String key = "enforced." + pkg;
                            if (sent.getLong(key, -1) != periodStart) {
                                Notices.send(this,key.hashCode(),name+" • time is up","Returned to Home. "+reset);
                                sent.edit().putLong(key, periodStart).apply();
                            }
                        }
                    }finally{busy=false;}
                });
            }catch(RuntimeException ex){busy=false;}
        });
    }
    public void onInterrupt(){}
    public void onDestroy(){alive=false;connected=false;handler.removeCallbacksAndMessages(null);worker.shutdownNow();super.onDestroy();}
}
