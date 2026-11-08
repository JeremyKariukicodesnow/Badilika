import com.badilika.mobile.DnsPacket;
import com.badilika.mobile.TimePolicy;
import com.badilika.mobile.AppBudget;
import com.badilika.mobile.ForegroundClock;
import java.io.*;
import java.util.*;
public final class PolicyTests {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static byte[] query(String host)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();b.write(new byte[]{12,34,1,0,0,1,0,0,0,0,0,0});for(String label:host.split("\\.")){b.write(label.length());b.write(label.getBytes("US-ASCII"));}b.write(new byte[]{0,0,1,0,1});return b.toByteArray();}
    public static void main(String[] args)throws Exception{
        long day=86400000L;
        check(TimePolicy.days(0)==1,"Minimum is a full day");check(TimePolicy.days(-100)==1,"Negative cannot bypass minimum");check(TimePolicy.days(31)==30,"Maximum is 30 days");check(TimePolicy.days(30)==30,"Month is available");
        check(TimePolicy.remaining(day,100,101,3,3)==day-1,"Only monotonic elapsed time counts");check(TimePolicy.remaining(day,100,day+99,3,3)==1,"One millisecond early remains locked");check(TimePolicy.remaining(day,100,day+100,3,3)==0,"Exact full-day boundary");check(TimePolicy.remaining(day,100,day*2,3,3)==0,"No negative remaining");check(TimePolicy.remaining(day,5000,12,3,4)==day,"Reboot cannot shorten countdown");check(TimePolicy.remaining(day,5000,12,3,3)==day,"Backward monotonic tick cannot shorten countdown");check(TimePolicy.remaining(day*30,100,100+day*29,3,3)==day,"30-day lock stays locked after 29 days");
        check(TimePolicy.delay(60000)==day,"Clock duration cannot go below one full day");check(TimePolicy.delay(day+3*3600000+17*60000)==day+3*3600000+17*60000,"Clock preserves custom days, hours and minutes");check(TimePolicy.delay(31*day)==30*day,"Custom duration capped at 30 full days");
        for(int allowance:new int[]{15,30,60,120,180,1440}){AppBudget budget=new AppBudget(0,allowance);check(!budget.exhausted(allowance*60000L-1),"Not exhausted one ms before "+allowance+" minutes");check(budget.exhausted(allowance*60000L),"Exhausted exactly at "+allowance+" minutes");check(budget.remaining(allowance*60000L+1)==0,"Remaining cannot go negative");check(AppBudget.parse(budget.encode()).limitMinutes==allowance,"Budget persists");}
        check(!new AppBudget(0,0).exhausted(day),"No limit means no closing");check(AppBudget.parse("malformed").limitMinutes==0,"Malformed rules do not close apps");check(AppBudget.parse("0,30").limitMinutes==30,"Existing saved goals retain their budgets");check(AppBudget.label(30).equals("30 minutes")&&AppBudget.label(60).equals("1 hour")&&AppBudget.label(120).equals("2 hours"),"Preset labels");
        boolean invalid=false;try{new AppBudget(31,30);}catch(IllegalArgumentException ex){invalid=true;}check(invalid,"Minimum cannot exceed daily allowance");
        ForegroundClock sessions=new ForegroundClock(1000,10000);sessions.resume("a",1100);sessions.pause("a",3100);sessions.resume("a",5000);sessions.pause("a",6000);check(sessions.used()==3000,"Separate sessions accumulate");
        ForegroundClock overlap=new ForegroundClock(1000,10000);overlap.resume("a",2000);overlap.resume("b",3000);overlap.pause("a",4000);overlap.pause("b",5000);check(overlap.used()==3000,"Simultaneous activities do not double-count");
        ForegroundClock midnight=new ForegroundClock(day,day+5000);midnight.resume("a",day-10000);midnight.pause("a",day+2000);check(midnight.used()==2000,"Midnight resets allowance while app spans midnight");
        ForegroundClock historical=new ForegroundClock(day,day+5000);historical.resume("a",day-10000);historical.pause("a",day-1000);check(historical.used()==0,"Previous-day usage excluded");
        ForegroundClock screen=new ForegroundClock(1000,10000);screen.resume("a",2000);screen.stopAll(3000);check(screen.used()==1000,"Screen off/shutdown stops usage");
        ForegroundClock current=new ForegroundClock(1000,10000);current.resume("a",5000);check(current.used()==5000,"Current open session counts before app exit");
        byte[] q=query("Example.COM");check(DnsPacket.host(q).equals("example.com"),"Case-normalized hostname");check(DnsPacket.host(new byte[4]).isEmpty(),"Truncated DNS header rejected");byte[] truncated=Arrays.copyOf(q,q.length-1);check(DnsPacket.host(truncated).isEmpty(),"Truncated question rejected");byte[] compressed=q.clone();compressed[12]=(byte)0xc0;check(DnsPacket.host(compressed).isEmpty(),"Untrusted compressed question rejected");
        byte[] denial=DnsPacket.denied(q);check((denial[3]&15)==3,"Blocked domain yields NXDOMAIN");check((denial[2]&128)!=0,"DNS response flag set");check(denial[0]==q[0]&&denial[1]==q[1],"Transaction preserved");check(q[2]==1,"Original query unchanged");
        byte[] ip=new byte[28];ip[0]=0x45;ip[9]=17;ip[12]=10;ip[15]=1;ip[16]=10;ip[19]=2;ip[20]=12;ip[21]=34;ip[23]=53;byte[] r=DnsPacket.ipv4(ip,20,denial);check(r.length==28+q.length,"Packet total length");check(r[12]==10&&r[15]==2&&r[19]==1,"Addresses reversed");check(DnsPacket.u16(r,20)==53&&DnsPacket.u16(r,22)==3106,"Ports reversed");check(DnsPacket.u16(r,24)==q.length+8,"UDP length");long sum=0;for(int i=0;i<20;i+=2)sum+=DnsPacket.u16(r,i);while(sum>65535)sum=(sum&65535)+(sum>>16);check(sum==65535,"Valid IPv4 checksum");
        byte[] ip6=new byte[48];ip6[0]=0x60;ip6[6]=17;ip6[7]=64;for(int i=0;i<16;i++){ip6[8+i]=(byte)(i+1);ip6[24+i]=(byte)(i+31);}ip6[40]=12;ip6[41]=34;ip6[43]=53;byte[] r6=DnsPacket.ipv6(ip6,denial);check(r6.length==48+q.length,"IPv6 packet total length");check(r6[0]==0x60&&r6[6]==17,"IPv6 UDP response header");check(r6[8]==31&&r6[24]==1,"IPv6 addresses reversed");check(DnsPacket.u16(r6,40)==53&&DnsPacket.u16(r6,42)==3106,"IPv6 ports reversed");check(DnsPacket.u16(r6,44)==q.length+8,"IPv6 UDP length");check(DnsPacket.u16(r6,46)!=0,"IPv6 UDP checksum is set");
        Random random=new Random(13);for(int i=0;i<10000;i++){byte[] bytes=new byte[random.nextInt(300)];random.nextBytes(bytes);DnsPacket.host(bytes);}check(true,"10000 malformed packets do not throw");
        System.out.println("PASS: "+checks+" checks (including 10000 malformed DNS packets)");
    }
}
