package com.badilika.mobile;

import android.content.*;
import android.os.Build;

/** Starts services on Android 6/7 as well as Android 8 and later. */
public final class ServiceStarter {
    private ServiceStarter(){}
    public static void foreground(Context context, Class<?> service){
        Intent intent=new Intent(context,service);
        if(Build.VERSION.SDK_INT>=26) context.startForegroundService(intent); else context.startService(intent);
    }
}
