package com.badilika.mobile;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small native vector icon set; scales without downloaded fonts or bitmap assets. */
public final class UiIcon extends Drawable {
    private final String name;
    private final int color,size;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    public UiIcon(String name,int color,int pixels){this.name=name;this.color=color;size=pixels;}
    private void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,paint);}
    private void path(Canvas c,float... points){Path p=new Path();p.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)p.lineTo(points[i],points[i+1]);c.drawPath(p,paint);}
    public void draw(Canvas canvas){
        int save=canvas.save();Rect b=getBounds();canvas.translate(b.left,b.top);canvas.scale(b.width()/24f,b.height()/24f);
        paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.75f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        switch(name){
            case "Home":path(canvas,3,10,12,3,21,10);path(canvas,5,9,5,21,10,21,10,15,14,15,14,21,19,21,19,9);break;
            case "Protect":case "shield":path(canvas,12,2,20,5,20,12,18,17,12,22,6,17,4,12,4,5,12,2);path(canvas,8,12,11,15,16,9);break;
            case "Timers":case "clock":canvas.drawCircle(12,13,8,paint);line(canvas,10,2,14,2);line(canvas,12,5,12,2);line(canvas,19,6,21,4);path(canvas,12,8,12,13,15,15);break;
            case "Insights":case "chart":line(canvas,4,21,21,21);line(canvas,4,21,4,4);path(canvas,7,15,11,10,15,13,20,5);break;
            case "Settings":case "settings":canvas.drawCircle(12,12,4,paint);for(int i=0;i<8;i++){double angle=Math.PI*i/4;float x=(float)Math.cos(angle),y=(float)Math.sin(angle);line(canvas,12+x*8,12+y*8,12+x*10,12+y*10);}canvas.drawCircle(12,12,8,paint);break;
            case "leaf":Path leaf=new Path();leaf.moveTo(4,18);leaf.cubicTo(0,7,10,3,21,3);leaf.cubicTo(21,14,16,23,6,20);canvas.drawPath(leaf,paint);path(canvas,3,22,10,15,16,9);break;
            case "tools":for(int x:new int[]{3,14})for(int y:new int[]{3,14})canvas.drawRoundRect(x,y,x+7,y+7,2,2,paint);break;
            case "refresh":canvas.drawArc(4,4,20,20,35,290,false,paint);path(canvas,16,3,20,4,20,8);break;
            case "download":path(canvas,12,3,12,16,7,11);path(canvas,12,16,17,11);path(canvas,4,16,4,21,20,21,20,16);break;
            case "plus":line(canvas,12,5,12,19);line(canvas,5,12,19,12);break;
            case "check":path(canvas,5,12,10,17,20,6);break;
            case "chevron":path(canvas,9,5,16,12,9,19);break;
            case "wifi":canvas.drawArc(2,4,22,24,225,90,false,paint);canvas.drawArc(6,8,18,20,225,90,false,paint);canvas.drawCircle(12,17,1,paint);break;
            case "data":path(canvas,7,3,7,20,3,16);path(canvas,7,20,11,16);path(canvas,17,21,17,4,13,8);path(canvas,17,4,21,8);break;
            case "bell":path(canvas,5,16,7,13,7,9,8,5,12,3,16,5,17,9,17,13,19,16,5,16);canvas.drawArc(9,17,15,22,0,180,false,paint);break;
            case "history":canvas.drawArc(4,4,20,20,210,290,false,paint);path(canvas,3,3,3,9,9,9);path(canvas,12,7,12,12,16,14);break;
            case "pause":line(canvas,8,5,8,19);line(canvas,16,5,16,19);break;
            case "remove":line(canvas,5,6,19,6);path(canvas,9,6,9,3,15,3,15,6);path(canvas,7,6,8,21,16,21,17,6);break;
            default:canvas.drawCircle(12,12,8,paint);line(canvas,12,7,12,13);canvas.drawCircle(12,17,.5f,paint);
        }
        canvas.restoreToCount(save);
    }
    public int getIntrinsicWidth(){return size;}public int getIntrinsicHeight(){return size;}
    public void setAlpha(int alpha){paint.setAlpha(alpha);}public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);}public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
