package com.badilika.tests;
import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import com.badilika.mobile.*;
import java.net.*;
import java.util.*;

/** Separate test APK only; never packaged in either shareable application. */
public class IntegrationRunner extends Instrumentation {
    int checks;private String mode="full";
    private void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public void onCreate(Bundle args){super.onCreate(args);if(args!=null)mode=args.getString("mode","full");start();}
    public void onStart(){Bundle result=new Bundle();try{
        Context c=getTargetContext();
        if(mode.equals("short-cooldown")){testShortCooldown(c);result.putString("stream","PASS: "+checks+" short cooldown checks\n");finish(Activity.RESULT_OK,result);return;}
        if(mode.equals("update-pass")){testUpdatePass(c);result.putString("stream","PASS: "+checks+" update pass and repeating window checks\n");finish(Activity.RESULT_OK,result);return;}
        if(mode.equals("admin-request")){testAdminRequest(c);result.putString("stream","PASS: "+checks+" administrator setup checks\n");finish(Activity.RESULT_OK,result);return;}
        if(mode.equals("guide")){testGuide(c);result.putString("stream","PASS: "+checks+" interactive guide checks\n");finish(Activity.RESULT_OK,result);return;}
        if(mode.equals("ui-seed")){c.getSharedPreferences("goals",0).edit().clear().putString("com.badilika.timerfixture","0,30").commit();c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit();TimerLocks.lock(c,"com.badilika.timerfixture").cancel();result.putString("stream","PASS: isolated UI fixture prepared");finish(Activity.RESULT_OK,result);return;}
        if(mode.startsWith("upgrade-")){upgrade(c,mode.equals("upgrade-seed"));result.putString("stream","PASS: "+checks+" "+mode+" checks\n");finish(Activity.RESULT_OK,result);return;}
        c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit();c.getSharedPreferences("locks",0).edit().clear().commit();
        LockState p=new LockState(c,"porn"),e=new LockState(c,"escort");check(!p.enabled(),"Initial protection is off");p.enable();e.enable();p.setDays(0);check(p.days()==1,"Enforces full-day minimum");p.request();check(p.pending()&&p.enabled(),"Request retains protection");check(p.remaining()>86390000,"Full 24 hours initially required");check(!p.disable(),"Early disable refused");p.setDays(30);check(p.days()==1,"Cannot alter pending delay");p.cancel();check(p.enabled()&&!p.pending(),"Cancellation remains protected");p.setDays(30);p.request();check(p.remaining()>2591990000L,"30 full days required");check(!e.pending()&&e.enabled(),"Escort has independent state");
        p.cancel();p.setDays(1);p.request();long before=p.remaining();LockState recreated=new LockState(c,"porn");check(recreated.pending()&&recreated.remaining()<=before,"Countdown persists across object recreation");
        c.getSharedPreferences("locks",0).edit().putLong("porn.remaining",0).commit();check(p.enabled(),"Completed timer still enabled");check(p.disable()&&!p.enabled(),"Confirmation disables only completed category");check(e.enabled(),"Other category remains enabled");
        Activity a=startActivitySync(new Intent().setClassName(c.getPackageName(),"com.badilika.mobile.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        for(String name:new String[]{"Protect","App timers","Insights","Settings","Home"}){runOnMainSync(()->{View v=find(a.getWindow().getDecorView(),name);check(v!=null,"Navigation exists: "+name);v.performClick();});waitForIdleSync();}
        check(History.access(c),"Usage access granted in test environment");try(History db=new History(c)){db.collect(2);String today=History.day(System.currentTimeMillis());check(db.screen(today,today).length==2,"Device screen report is readable");check(db.hours(today,today).size()==24,"24 hourly bins");check(!db.rows(today,today,"","Time").isEmpty(),"Real usage rows collected");}
        // Real local TUN query, with the test emulator's explicit VPN consent/app-op.
        p.enable();e.enable();check(android.net.VpnService.prepare(c)==null,"VPN consent is prepared for this fresh installation");runOnMainSync(()->c.startForegroundService(new Intent(c,GuardService.class)));
        for(int i=0;i<100&&!GuardService.active;i++)SystemClock.sleep(100);
        check(GuardService.active,"VPN tunnel established");SystemClock.sleep(2000);byte[] q=query("skipthegames.com");try(DatagramSocket s=new DatagramSocket()){s.setSoTimeout(5000);s.send(new DatagramPacket(q,q.length,InetAddress.getByName("10.83.0.2"),53));byte[] b=new byte[2048];DatagramPacket r=new DatagramPacket(b,b.length);s.receive(r);check((b[3]&15)==3,"Escort DNS blocked through actual TUN");}
        q=query("pornhub.com");try(DatagramSocket s=new DatagramSocket()){s.setSoTimeout(5000);s.send(new DatagramPacket(q,q.length,InetAddress.getByName("10.83.0.2"),53));byte[] b=new byte[2048];DatagramPacket r=new DatagramPacket(b,b.length);s.receive(r);check((b[3]&15)==3,"Porn DNS blocked through actual TUN");}
        q=query("example.com");try(DatagramSocket s=new DatagramSocket()){s.setSoTimeout(6000);s.send(new DatagramPacket(q,q.length,InetAddress.getByName("10.83.0.2"),53));byte[] b=new byte[4096];DatagramPacket r=new DatagramPacket(b,b.length);s.receive(r);check((b[3]&15)==0&&DnsPacket.u16(b,6)>0,"Allowed DNS resolves through family upstream");}
        c.getSharedPreferences("settings",0).edit().putBoolean("tracking",true).commit();long syncBefore=c.getSharedPreferences("settings",0).getLong("lastSync",0);runOnMainSync(()->c.startForegroundService(new Intent(c,TrackerService.class)));
        for(int i=0;i<100&&c.getSharedPreferences("settings",0).getLong("lastSync",0)<=syncBefore;i++)SystemClock.sleep(100);
        check(c.getSharedPreferences("settings",0).getLong("lastSync",0)>syncBefore,"Background journal collects and saves");check(c.getSharedPreferences("settings",0).getString("syncError","").isEmpty(),"No journal collection error");
        boolean ongoing=false;for(android.service.notification.StatusBarNotification n:c.getSystemService(NotificationManager.class).getActiveNotifications())if(n.getId()==21)ongoing=true;check(ongoing,"Tracking foreground notification is visible");
        testCooldown(c);
        testAppTimer(c);
        result.putString("stream","PASS: "+checks+" Android integration checks\n");finish(Activity.RESULT_OK,result);
    }catch(Throwable ex){result.putString("stream","FAIL after "+checks+" checks: "+android.util.Log.getStackTraceString(ex));finish(Activity.RESULT_CANCELED,result);}}
    private void testAdminRequest(Context c) throws Exception {
        c.getSharedPreferences("settings", 0).edit().putBoolean("guide_seen", true).commit();
        Activity activity = startActivitySync(new Intent()
            .setClassName(c.getPackageName(), "com.badilika.mobile.MainActivity")
            .putExtra("screen", "Settings").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        waitForIdleSync();
        ActivityMonitor consent = addMonitor(new IntentFilter(
            android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN), null, false);
        runOnMainSync(() -> {
            View button = find(activity.getWindow().getDecorView(), "Enable uninstall protection");
            check(button != null, "Administrator setup is available and initially off");
            button.performClick();
        });
        check(consent.getHits() == 1, "Button launches Android administrator consent");
        removeMonitor(consent);
        android.app.admin.DevicePolicyManager policy = c.getSystemService(android.app.admin.DevicePolicyManager.class);
        check(!policy.isAdminActive(new ComponentName(c, UninstallAdminReceiver.class)), "Consent is required before activation");
    }
    private void testUpdatePass(Context target) {
        Context c = new ContextWrapper(target) {
            @Override public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences("test-v17-" + name, mode);
            }
        };
        SharedPreferences goals = c.getSharedPreferences("goals", 0);
        goals.edit().clear().putString("example.first", "0,30").putString("example.second", "0,20").commit();
        c.getSharedPreferences("locks", 0).edit().clear().commit();
        TimerLocks.grantUpdatePasses(c);
        check(TimerLocks.hasUpdatePass(c, "example.first"), "Existing timer receives one pass");
        check(TimerLocks.save(c, "example.first", new AppBudget(0, 30, 300, 0), 95 * 60000L),
            "Pass allows converting existing daily timer to repeating and shortening its cooldown");
        AppBudget saved = AppBudget.parse(goals.getString("example.first", ""));
        check(saved.repeats() && saved.windowMinutes == 300 && saved.windowAnchor > 0, "Repeating schedule persists");
        check(saved.windowStart(saved.windowAnchor + 299 * 60000L, 0) == saved.windowAnchor, "Allowance uses the current window");
        check(saved.windowStart(saved.windowAnchor + 300 * 60000L, 0) == saved.windowAnchor + 300 * 60000L, "Allowance resets at the next window");
        check(!TimerLocks.hasUpdatePass(c, "example.first"), "First timer's pass is consumed");
        check(!TimerLocks.save(c, "example.first", new AppBudget(0, 40, 300, 0), 95 * 60000L), "Later increase requires a cooldown");
        check(TimerLocks.save(c, "example.first", new AppBudget(0, 15, 300, 0), 95 * 60000L), "Lowering the allowance still applies immediately");
        check(AppBudget.parse(goals.getString("example.first", "")).windowAnchor == saved.windowAnchor, "Editing allowance preserves the reset schedule");
        check(TimerLocks.remove(c, "example.second"), "Second timer can use its own pass to remove");
        check(TimerLocks.save(c, "example.new", new AppBudget(0, 10), 60000), "New timer saves");
        TimerLocks.grantUpdatePasses(c);
        check(!TimerLocks.hasUpdatePass(c, "example.new") && !TimerLocks.hasUpdatePass(c, "example.first"), "Reopening grants no additional passes");
    }
    private void testShortCooldown(Context c) {
        String pkg = "com.badilika.shortcooldownfixture";
        long delay = 95 * 60000L;
        check(TimerLocks.save(c, pkg, new AppBudget(0, 30), delay), "Initial timer saves");
        LockState lock = TimerLocks.lock(c, pkg);
        check(lock.delayMs() == delay, "Hour-and-minute duration persists");
        check(!TimerLocks.save(c, pkg, new AppBudget(0, 60), delay), "Increase requires cooldown");
        check(!TimerLocks.remove(c, pkg), "Removal requires cooldown");
        check(!TimerLocks.save(c, pkg, new AppBudget(0, 30), 60000), "Shortening cooldown requires a wait");
        check(TimerLocks.save(c, pkg, new AppBudget(0, 15), delay), "Reduction applies immediately");
        lock.request();
        check(lock.remaining() > delay - 10000 && lock.remaining() <= delay, "Countdown starts at the selected duration");
        check(!TimerLocks.remove(c, pkg), "Removal stays locked while countdown runs");
        c.getSharedPreferences("locks", 0).edit().putLong("timer." + pkg + ".remaining", 0).commit();
        check(TimerLocks.remove(c, pkg), "Removal succeeds after cooldown");
    }
    private void testGuide(Context c)throws Exception{
        String goals=snapshot(c,"goals"),locks=snapshot(c,"locks");
        c.getSharedPreferences("settings",0).edit().putBoolean("guide_seen",true).putBoolean("guide_active",true).putInt("guide_step",0).commit();
        Activity a=startActivitySync(new Intent().setClassName(c.getPackageName(),"com.badilika.mobile.MainActivity").putExtra("screen","Guide").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();
        String[] actions={"Next step","Open Usage access settings","Enable automatic closing","Open Protect and choose categories","Choose an app and set a timer","Open app timers and cooldowns","Set up notifications"};
        for(int step=0;step<7;step++){final int index=step;runOnMainSync(()->check(find(a.getWindow().getDecorView(),actions[index])!=null,"Real action present on step "+index));
            if(step==3){runOnMainSync(()->find(a.getWindow().getDecorView(),actions[index]).performClick());waitForIdleSync();runOnMainSync(()->{View label=find(a.getWindow().getDecorView(),"Continue setup guide");check(label!=null,"Return-to-guide control appears on Protect");while(!label.isClickable()&&label.getParent() instanceof View)label=(View)label.getParent();label.performClick();});waitForIdleSync();check(c.getSharedPreferences("settings",0).getInt("guide_step",-1)==3,"Guide retains step after real screen navigation");}
            if(step<6){runOnMainSync(()->find(a.getWindow().getDecorView(),"Next step").performClick());waitForIdleSync();check(c.getSharedPreferences("settings",0).getInt("guide_step",-1)==step+1,"Next step persisted");}
        }
        check(snapshot(c,"goals").equals(goals),"Guide leaves saved app allowances unchanged");check(snapshot(c,"locks").equals(locks),"Guide leaves protection locks unchanged");
        runOnMainSync(()->find(a.getWindow().getDecorView(),"Finish setup guide").performClick());waitForIdleSync();check(!c.getSharedPreferences("settings",0).getBoolean("guide_active",true),"Finish exits guide mode");
    }
    static View find(View view,String text){if(view instanceof TextView&&((TextView)view).getText().toString().equals(text))return view;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++){View found=find(group.getChildAt(i),text);if(found!=null)return found;}}return null;}
    private String snapshot(Context c,String file){return new TreeMap<String,Object>(c.getSharedPreferences(file,0).getAll()).toString();}
    private String historyHash(Context c)throws Exception{java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");try(History db=new History(c);android.database.Cursor cursor=db.getReadableDatabase().rawQuery("SELECT day,pkg,ms,opens,wifi,mobile FROM daily ORDER BY day,pkg",null)){while(cursor.moveToNext()){for(int i=0;i<6;i++)hash.update((String.valueOf(cursor.getString(i))+"\u0000").getBytes("UTF-8"));}}return android.util.Base64.encodeToString(hash.digest(),android.util.Base64.NO_WRAP);}
    private void upgrade(Context c,boolean seed)throws Exception{
        SharedPreferences baseline=c.getSharedPreferences("ui-upgrade-test-baseline",0);
        if(seed){
            runOnMainSync(()->{c.stopService(new Intent(c,TrackerService.class));c.stopService(new Intent(c,GuardService.class));});SystemClock.sleep(2500);
            c.getSharedPreferences("settings",0).edit().putBoolean("tracking",false).putBoolean("limitsEnabled",false).putString("domains","example.test\nblocked.example").commit();
            c.getSharedPreferences("goals",0).edit().putString("com.badilika.upgradefixture","17,77").putString("com.badilika.secondfixture","0,120").commit();
            LockState lock=new LockState(c,"porn");lock.enable();lock.setDelayMs(86400000L+8100000L);lock.request();LockState escort=new LockState(c,"escort");escort.enable();escort.setDays(30);
            try(History db=new History(c)){db.getWritableDatabase().execSQL("INSERT OR REPLACE INTO daily(day,pkg,ms,opens,wifi,mobile) VALUES('2001-02-03','com.badilika.upgradefixture',123456,7,11112222,33334444)");}
            SharedPreferences.Editor save=baseline.edit();for(String name:new String[]{"goals","locks","settings"})save.putString(name,snapshot(c,name));save.putString("history",historyHash(c));save.putString("version",c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName);check(save.commit(),"Seeded current user settings and history before update");
        }else{
            for(String name:new String[]{"goals","locks","settings"})check(snapshot(c,name).equals(baseline.getString(name,"missing")),name+" unchanged across APK update");
            check(historyHash(c).equals(baseline.getString("history","missing")),"All daily history rows retained across update");
            check(!c.getPackageManager().getPackageInfo(c.getPackageName(),0).versionName.equals(baseline.getString("version","")),"New version installed over the old one");
            LockState lock=new LockState(c,"porn");check(lock.enabled()&&lock.pending()&&lock.remaining()>0,"Active protection countdown resumes after update");check(lock.delayMs()==94500000L,"Custom clock-picked protection delay retained");check(c.getSharedPreferences("goals",0).getString("com.badilika.upgradefixture","").equals("17,77"),"Clock-picked 77-minute allowance and 17-minute goal retained");
            Activity activity=startActivitySync(new Intent().setClassName(c.getPackageName(),"com.badilika.mobile.MainActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();runOnMainSync(()->{for(String name:new String[]{"Home","Protect","App timers","Insights","Settings"})check(find(activity.getWindow().getDecorView(),name)!=null,"Updated navigation present: "+name);});
        }
    }
    private void testCooldown(Context c)throws Exception{
        SharedPreferences goals=c.getSharedPreferences("goals",0);Map<String,?> original=goals.getAll();boolean enabled=c.getSharedPreferences("settings",0).getBoolean("limitsEnabled",false);goals.edit().clear().commit();
        String a="com.badilika.cooldownfixture",b="com.badilika.othercooldown";long day=LockState.DAY;
        try{
            check(TimerLocks.save(c,a,new AppBudget(0,30),2*day),"New timer can select a two-day cooldown");
            check(!TimerLocks.save(c,a,new AppBudget(0,60),2*day),"Increasing allowance requires cooldown");
            check(!TimerLocks.save(c,a,new AppBudget(0,0),2*day),"Disabling allowance requires cooldown");
            check(!TimerLocks.remove(c,a),"Removing limit requires cooldown");
            check(!TimerLocks.pause(c),"Global pause cannot bypass a locked timer");
            LockState l=TimerLocks.lock(c,a);l.request();check(l.remaining()>2*day-10000,"Selected full cooldown starts");
            l.setDelayMs(day);check(l.delayMs()==2*day,"Pending cooldown cannot be shortened");
            check(TimerLocks.lock(c,a).pending(),"Cooldown persists across recreation");
            check(!TimerLocks.save(c,a,new AppBudget(0,60),2*day),"Running countdown cannot loosen limit");
            check(TimerLocks.save(c,a,new AppBudget(0,30),2*day)&&l.pending(),"Saving an unchanged timer preserves its countdown");
            l.cancel();check(!l.pending(),"Cancel keeps the saved allowance");l.request();check(l.remaining()>2*day-10000,"Restart requires full wait");
            check(TimerLocks.save(c,a,new AppBudget(0,20),2*day),"Reducing allowance applies immediately");check(!l.pending(),"Changed allowance invalidates prior unlock request");
            check(!TimerLocks.save(c,a,new AppBudget(0,20),day),"Shortening a saved cooldown requires wait");
            l.request();c.getSharedPreferences("locks",0).edit().putLong("timer."+a+".remaining",0).commit();
            check(goals.getString(a,"").equals("0,20"),"Finished cooldown does not remove limit without confirmation");
            check(TimerLocks.save(c,a,new AppBudget(0,60),day),"Completed wait permits confirmed increase and new delay");
            check(!TimerLocks.ready(c,a),"Successful change consumes unlock");check(!TimerLocks.remove(c,a),"Removal needs another request after consuming unlock");
            TimerLocks.save(c,b,new AppBudget(0,15),day);l.request();c.getSharedPreferences("locks",0).edit().putLong("timer."+a+".remaining",0).commit();
            check(!TimerLocks.pause(c),"Global pause requires every timer to finish");
            TimerLocks.lock(c,b).request();c.getSharedPreferences("locks",0).edit().putLong("timer."+b+".remaining",0).commit();check(TimerLocks.pause(c),"Global pause allowed after all cooldowns");check(!l.pending()&&!TimerLocks.lock(c,b).pending(),"Global pause consumes all unlocks");
            check(!TimerLocks.remove(c,a),"Pausing does not authorize later removal");l.request();c.getSharedPreferences("locks",0).edit().putLong("timer."+a+".remaining",0).commit();check(TimerLocks.remove(c,a)&&!goals.contains(a),"Confirmed removal works after completed cooldown");
        }finally{SharedPreferences.Editor restore=goals.edit().clear();for(Map.Entry<String,?> e:original.entrySet())if(e.getValue() instanceof String)restore.putString(e.getKey(),(String)e.getValue());restore.commit();c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",enabled).commit();}
    }
    private UiAutomation automation;
    private String shellOutput(String command)throws Exception{try(android.os.ParcelFileDescriptor fd=automation.executeShellCommand(command);java.io.FileInputStream in=new java.io.FileInputStream(fd.getFileDescriptor());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] bytes=new byte[4096];int n;while((n=in.read(bytes))>0)out.write(bytes,0,n);return out.toString("UTF-8");}}
    private void shell(String command)throws Exception{shellOutput(command);}
    private String front(){try{String output=shellOutput("dumpsys activity activities");java.util.regex.Matcher match=java.util.regex.Pattern.compile("(?:topResumedActivity=|mResumedActivity:)[^\\n]*?\\bu\\d+\\s+([\\w.]+)/").matcher(output);return match.find()?match.group(1):"";}catch(Exception ex){throw new IllegalStateException("Could not inspect resumed test activity",ex);}}
    private void launchFixture(Context c){runOnMainSync(()->c.startActivity(new Intent().setClassName("com.badilika.timerfixture","com.badilika.timerfixture.TimerFixture").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));}
    private void testAppTimer(Context c)throws Exception{
        automation=getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        // Instrumentation restarts its target process; re-enable after that test-only restart.
        shell("settings put secure enabled_accessibility_services null");
        shell("settings put secure enabled_accessibility_services com.badilika.mobile/com.badilika.mobile.AppLimitService");
        shell("settings put secure accessibility_enabled 1");
        for(int i=0;i<100&&!AppLimitService.connected;i++)SystemClock.sleep(100);
        check(AppLimitService.connected&&AppLimitService.authorized(c),"App closing service connected with explicit system permission");
        check(AppLimitService.essential(c,c.getPackageName())&&AppLimitService.essential(c,"com.android.settings"),"App and system settings cannot be locked out");
        final String fixture="com.badilika.timerfixture";
        c.getSharedPreferences("goals",0).edit().putString(fixture,"0,30").commit();c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",true).commit();
        launchFixture(c);SystemClock.sleep(2000);check(front().equals(fixture),"Selected app remains usable below its 30-minute budget");
        long initial=DailyUsage.read(c,fixture,System.currentTimeMillis());int allowance=(int)(initial/60000)+1;
        c.getSharedPreferences("goals",0).edit().putString(fixture,"0,"+allowance).commit();
        long deadline=SystemClock.elapsedRealtime()+75000;boolean closed=false;
        while(SystemClock.elapsedRealtime()<deadline){SystemClock.sleep(1000);if(!front().equals(fixture)){closed=true;break;}}
        check(closed,"App returns to Home while continuously open at the exact daily allowance");
        check(DailyUsage.read(c,fixture,System.currentTimeMillis())>=allowance*60000L,"App was not closed before its allowance was used");
        String home=front();check(!home.isEmpty()&&!home.equals(c.getPackageName()),"Home is shown rather than another Badilika window");
        SystemClock.sleep(1500);launchFixture(c);SystemClock.sleep(4000);check(!front().equals(fixture),"Reopening an exhausted app returns to Home again");
        c.getSharedPreferences("goals",0).edit().putString(fixture,"0,"+(allowance+30)).commit();launchFixture(c);SystemClock.sleep(3000);check(front().equals(fixture),"Increasing daily allowance immediately allows the app");
        c.getSharedPreferences("goals",0).edit().putString(fixture,"0,"+allowance).commit();c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit();SystemClock.sleep(3000);check(front().equals(fixture),"Pausing automatic closing leaves the app usable");
        c.getSharedPreferences("goals",0).edit().remove(fixture).commit();c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",true).commit();SystemClock.sleep(2500);check(front().equals(fixture),"Removing a timer leaves the app usable");
        c.getSharedPreferences("settings",0).edit().putBoolean("limitsEnabled",false).commit();
    }
    static byte[] query(String host)throws Exception{java.io.ByteArrayOutputStream b=new java.io.ByteArrayOutputStream();b.write(new byte[]{12,34,1,0,0,1,0,0,0,0,0,0});for(String label:host.split("\\.")){b.write(label.length());b.write(label.getBytes("US-ASCII"));}b.write(new byte[]{0,0,1,0,1});return b.toByteArray();}
}
