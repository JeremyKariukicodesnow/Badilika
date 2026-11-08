package com.badilika.mobile;
import java.util.*;
/** Bounded DNS parser and IPv4/UDP response construction. No payload logging. */
public final class DnsPacket {
    public static int u16(byte[] p,int o){return ((p[o]&255)<<8)|(p[o+1]&255);}
    static void put(byte[] p,int o,int n){p[o]=(byte)(n>>8);p[o+1]=(byte)n;}
    public static String host(byte[] d){
        if(d.length<17||u16(d,4)!=1) return "";
        StringBuilder s=new StringBuilder(); int i=12;
        while(i<d.length){int n=d[i++]&255;if(n==0)return i+4<=d.length?s.toString().toLowerCase(Locale.US):"";if(n>63||i+n>d.length||s.length()+n>253)return "";if(s.length()>0)s.append('.');for(int j=0;j<n;j++){int ch=d[i++]&255;if(ch<33||ch>126)return "";s.append((char)ch);}}return "";
    }
    public static byte[] denied(byte[] q) { byte[] r=Arrays.copyOf(q,q.length);r[2]=(byte)0x81;r[3]=(byte)0x83;for(int i=6;i<12;i++)r[i]=0;return r; }
    public static byte[] ipv4(byte[] request,int ihl,byte[] dns){
        byte[] out=new byte[28+dns.length];out[0]=0x45;put(out,2,out.length);out[8]=64;out[9]=17;
        System.arraycopy(request,16,out,12,4);System.arraycopy(request,12,out,16,4);
        put(out,20,u16(request,ihl+2));put(out,22,u16(request,ihl));put(out,24,8+dns.length);
        System.arraycopy(dns,0,out,28,dns.length);long sum=0;for(int i=0;i<20;i+=2)sum+=u16(out,i);while((sum>>16)!=0)sum=(sum&65535)+(sum>>16);put(out,10,(int)~sum);return out;
    }
    /** Build a UDP/IPv6 response for the local synthetic DNS address. */
    public static byte[] ipv6(byte[] request,byte[] dns){
        byte[] out=new byte[48+dns.length];out[0]=0x60;put(out,4,8+dns.length);out[6]=17;out[7]=64;
        System.arraycopy(request,24,out,8,16);System.arraycopy(request,8,out,24,16);
        put(out,40,u16(request,42));put(out,42,u16(request,40));put(out,44,8+dns.length);System.arraycopy(dns,0,out,48,dns.length);
        long sum=0;for(int i=8;i<40;i+=2)sum+=u16(out,i);sum+=(8+dns.length);sum+=17;
        for(int i=40;i<out.length;i+=2){int word=(out[i]&255)<<8;if(i+1<out.length)word|=out[i+1]&255;sum+=word;}
        while((sum>>16)!=0)sum=(sum&65535)+(sum>>16);int checksum=(int)~sum;if(checksum==0)checksum=65535;put(out,46,checksum);return out;
    }
}
