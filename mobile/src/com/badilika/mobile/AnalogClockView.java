package com.badilika.mobile;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.view.View;
import java.text.SimpleDateFormat;
import java.util.*;

/** Local-time analog clock. Only animates while attached; no background tracking or wake lock. */
public final class AnalogClockView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Handler handler=new Handler();
    private final boolean saver;
    private boolean attached;
    private long announcedMinute=-1;
    public AnalogClockView(Context context,boolean saver){super(context);this.saver=saver;setBackgroundColor(0xff080e0b);setFocusable(true);}
    private boolean seconds(){return getContext().getSharedPreferences("clock",0).getBoolean("seconds",false);}
    private final Runnable tick=new Runnable(){public void run(){if(!attached)return;long now=System.currentTimeMillis();invalidate();if(now/60000!=announcedMinute){announcedMinute=now/60000;setContentDescription("Analog clock. "+new SimpleDateFormat("HH:mm, EEEE d MMMM",Locale.getDefault()).format(new Date(now)));}long interval=seconds()?1000:60000;handler.postDelayed(this,interval-now%interval);}};
    protected void onAttachedToWindow(){super.onAttachedToWindow();attached=true;handler.post(tick);}
    protected void onDetachedFromWindow(){attached=false;handler.removeCallbacks(tick);super.onDetachedFromWindow();}
    public void refresh(){handler.removeCallbacks(tick);if(attached)handler.post(tick);invalidate();}
    private void stroke(int color,float width){paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(width);paint.setStrokeCap(Paint.Cap.ROUND);}
    private void hand(Canvas c,float cx,float cy,float radius,float degrees,float reach,float width,int color){double angle=Math.toRadians(degrees-90);stroke(color,width);c.drawLine(cx-(float)Math.cos(angle)*radius*.1f,cy-(float)Math.sin(angle)*radius*.1f,cx+(float)Math.cos(angle)*radius*reach,cy+(float)Math.sin(angle)*radius*reach,paint);}
    protected void onDraw(Canvas c){
        super.onDraw(c);long now=System.currentTimeMillis();Calendar time=Calendar.getInstance();time.setTimeInMillis(now);float w=getWidth(),h=getHeight();float radius=Math.min(w*.35f,h*.30f),cx=w/2,cy=h*.45f;
        if(saver){long minute=now/60000;cx+=((minute%7)-3)*radius*.012f;cy+=((minute/7%7)-3)*radius*.012f;}
        stroke(0xff23382c,Math.max(1,radius*.005f));c.drawCircle(cx,cy,radius*1.06f,paint);
        for(int i=0;i<60;i++){double angle=Math.toRadians(i*6-90);float inside=i%5==0?.89f:.95f;stroke(i%5==0?0xffb8c7b1:0xff354a3c,radius*(i%5==0?.012f:.005f));c.drawLine(cx+(float)Math.cos(angle)*radius*inside,cy+(float)Math.sin(angle)*radius*inside,cx+(float)Math.cos(angle)*radius,cy+(float)Math.sin(angle)*radius,paint);}
        paint.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);paint.setTextSize(radius*.15f);paint.setColor(0xff81927b);paint.setStyle(Paint.Style.FILL);
        for(int hour:new int[]{12,3,6,9}){double angle=Math.toRadians(hour*30-90);c.drawText(""+hour,cx+(float)Math.cos(angle)*radius*.72f,cy+(float)Math.sin(angle)*radius*.72f+paint.getTextSize()*.35f,paint);}
        float minute=time.get(Calendar.MINUTE)+(seconds()?time.get(Calendar.SECOND)/60f:0);hand(c,cx,cy,radius,(time.get(Calendar.HOUR)%12)*30+minute*.5f,.50f,radius*.032f,0xffe2ead6);hand(c,cx,cy,radius,minute*6,.78f,radius*.020f,0xffd3e5bc);if(seconds())hand(c,cx,cy,radius,time.get(Calendar.SECOND)*6,.84f,radius*.007f,0xff8fb57d);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xffd3e5bc);c.drawCircle(cx,cy,radius*.035f,paint);paint.setColor(0xff080e0b);c.drawCircle(cx,cy,radius*.012f,paint);
        float base=cy+radius*1.40f;paint.setColor(0xff9aa992);paint.setTextSize(radius*.10f);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));c.drawText(new SimpleDateFormat("EEEE · d MMMM",Locale.getDefault()).format(new Date(now)),cx,base,paint);paint.setTextSize(radius*.068f);paint.setColor(0xff536b57);c.drawText("B A D I L I K A",cx,base+radius*.22f,paint);
    }
}
