package com.badilika.mobile;

import android.service.dreams.DreamService;

/** Android starts/stops this screen saver according to the user's charging/idle settings. */
public final class AnalogClockDreamService extends DreamService {
    private AnalogClockView clock;
    public void onAttachedToWindow(){super.onAttachedToWindow();setInteractive(false);setFullscreen(true);setScreenBright(false);clock=new AnalogClockView(this,true);setContentView(clock);}
    public void onDreamingStarted(){super.onDreamingStarted();if(clock!=null)clock.refresh();}
    public void onDetachedFromWindow(){clock=null;super.onDetachedFromWindow();}
}
