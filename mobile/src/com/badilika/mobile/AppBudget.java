package com.badilika.mobile;

/** Legacy two-field budgets remain daily. Repeating budgets store a window and its start. */
public final class AppBudget {
    public final int minimumMinutes,limitMinutes;
    public final int windowMinutes;
    public final long windowAnchor;
    public AppBudget(int minimum,int limit){
        this(minimum, limit, 0, 0);
    }
    public AppBudget(int minimum, int limit, int windowMinutes, long windowAnchor) {
        if(minimum<0||limit<0||minimum>1440||limit>1440||(limit>0&&minimum>limit))throw new IllegalArgumentException("Invalid daily budget");
        if (windowMinutes < 0 || windowMinutes > 1439 || windowAnchor < 0
                || (windowMinutes > 0 && (limit == 0 || limit > windowMinutes)))
            throw new IllegalArgumentException("Allowance must fit within the repeating window");
        minimumMinutes=minimum;limitMinutes=limit;
        this.windowMinutes = windowMinutes;
        this.windowAnchor = windowAnchor;
    }
    public static AppBudget parse(String value){
        try {
            String[] parts = value.split(",");
            int minimum = Integer.parseInt(parts[0]), limit = Integer.parseInt(parts[1]);
            if (parts.length == 4)
                return new AppBudget(minimum, limit, Integer.parseInt(parts[2]), Long.parseLong(parts[3]));
            return new AppBudget(minimum, limit);
        }
        catch(RuntimeException ex){return new AppBudget(0,0);}
    }
    public boolean exhausted(long usedMs){return limitMinutes>0&&usedMs>=limitMinutes*60000L;}
    public long remaining(long usedMs){return limitMinutes==0?Long.MAX_VALUE:Math.max(0,limitMinutes*60000L-Math.max(0,usedMs));}
    public boolean repeats() { return windowMinutes > 0; }
    public long windowStart(long now, long midnight) {
        if (!repeats()) return midnight;
        long length = windowMinutes * 60000L;
        return now < windowAnchor ? now : windowAnchor + ((now - windowAnchor) / length) * length;
    }
    public String encode(){
        String daily = minimumMinutes + "," + limitMinutes;
        return repeats() ? daily + "," + windowMinutes + "," + windowAnchor : daily;
    }
    public static String label(int minutes){return minutes==0?"No daily limit":minutes<60?minutes+" minutes":minutes%60==0?(minutes/60)+((minutes==60)?" hour":" hours"):(minutes/60)+"h "+minutes%60+"m";}
}
