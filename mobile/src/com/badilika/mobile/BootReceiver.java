package com.badilika.mobile;
import android.content.*;
import android.net.VpnService;
public class BootReceiver extends BroadcastReceiver {
    public void onReceive(Context c,Intent i) {
        if(c.getSharedPreferences("settings",0).getBoolean("tracking",false)) try {ServiceStarter.foreground(c,TrackerService.class);}catch(RuntimeException ignored){}
        if(new LockState(c,"porn").enabled()||new LockState(c,"escort").enabled()) {
            if(VpnService.prepare(c)==null) try {ServiceStarter.foreground(c,GuardService.class);}catch(RuntimeException ex){Notices.send(c,13,"Protection needs attention","Open Badilika to reconnect protection.");}
            else Notices.send(c,13,"Protection needs attention","VPN permission is missing. Open Badilika to reconnect.");
        }
    }
}
