package com.badilika.mobile;

/** Daily rules use the existing goal format, so previously saved limits keep working. */
public final class AppBudget {
    public final int minimumMinutes,limitMinutes;
    public AppBudget(int minimum,int limit){
        if(minimum<0||limit<0||minimum>1440||limit>1440||(limit>0&&minimum>limit))throw new IllegalArgumentException("Invalid daily budget");
        minimumMinutes=minimum;limitMinutes=limit;
    }
    public static AppBudget parse(String value){
        try{String[] parts=value.split(",");return new AppBudget(Integer.parseInt(parts[0]),Integer.parseInt(parts[1]));}
        catch(RuntimeException ex){return new AppBudget(0,0);}
    }
    public boolean exhausted(long usedMs){return limitMinutes>0&&usedMs>=limitMinutes*60000L;}
    public long remaining(long usedMs){return limitMinutes==0?Long.MAX_VALUE:Math.max(0,limitMinutes*60000L-Math.max(0,usedMs));}
    public String encode(){return minimumMinutes+","+limitMinutes;}
    public static String label(int minutes){return minutes==0?"No daily limit":minutes<60?minutes+" minutes":minutes%60==0?(minutes/60)+((minutes==60)?" hour":" hours"):(minutes/60)+"h "+minutes%60+"m";}
}
