package com.badilika.mobile;

import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GuardService extends VpnService {
    private ParcelFileDescriptor tunnel;
    private Thread worker;
    private volatile boolean running;
    private volatile Set<String> porn=Collections.emptySet();
    private final Set<String> escort=new HashSet<>();
    public static volatile boolean active;
    public static volatile String error="";
    public int onStartCommand(Intent intent,int flags,int id){
        if(!new LockState(this,"porn").enabled()&&!new LockState(this,"escort").enabled()){stopSelf();return START_NOT_STICKY;}
        startForeground(11,Notices.make(this,"Badilika protection","Local DNS filtering is active",true));
        if(worker==null){running=true;worker=new Thread(this::run,"Badilika DNS");worker.start();}
        return START_STICKY;
    }
    private boolean listed(String host,Set<String> set){String s=host;while(true){if(set.contains(s))return true;int d=s.indexOf('.');if(d<0)return false;s=s.substring(d+1);}}
    private void run(){
        try {
            porn=BlocklistStore.bundled(this,"porn.txt");escort.addAll(BlocklistStore.bundled(this,"escort.txt"));
            Builder builder=new Builder().setSession("Badilika • content protection").addAddress("10.83.0.1",32).addAddress("fd53:bad1:1::1",128).addDnsServer("10.83.0.2").addDnsServer("fd53:bad1:1::2").addRoute("10.83.0.2",32).addRoute("fd53:bad1:1::2",128).setMtu(1500).setBlocking(true);
            // Well-known resolvers are trapped locally so plaintext DNS, DNS-over-TLS, and their
            // common encrypted-DNS endpoints cannot bypass the local resolver while protection runs.
            for(String resolver:new String[]{"1.1.1.1","1.0.0.1","8.8.8.8","8.8.4.4","9.9.9.9","149.112.112.112","94.140.14.14","94.140.15.15","208.67.222.222","208.67.220.220"})builder.addRoute(resolver,32);
            for(String resolver:new String[]{"2606:4700:4700::1111","2606:4700:4700::1001","2001:4860:4860::8888","2001:4860:4860::8844","2620:fe::fe","2620:fe::9","2a10:50c0::ad1:ff","2a10:50c0::ad2:ff","2620:119:35::35","2620:119:53::53"})builder.addRoute(resolver,128);
            tunnel=builder.establish();
            if(tunnel==null)throw new IOException("VPN permission is required");
            active=true;error="";BlocklistStore.refreshPorn(this,updated->porn=updated);
            try(FileInputStream in=new FileInputStream(tunnel.getFileDescriptor());FileOutputStream out=new FileOutputStream(tunnel.getFileDescriptor())){
                byte[] buf=new byte[32767];
                while(running){int n=in.read(buf);if(n<1)continue;int version=buf[0]>>4,ip,udp;
                    if(version==4){if(n<28||buf[9]!=17)continue;ip=(buf[0]&15)*4;if(ip<20||n<ip+8||DnsPacket.u16(buf,ip+2)!=53||(DnsPacket.u16(buf,6)&0x3fff)!=0)continue;udp=ip;}
                    else if(version==6){if(n<48||buf[6]!=17||DnsPacket.u16(buf,42)!=53)continue;ip=40;udp=40;}else continue;
                    int len=DnsPacket.u16(buf,udp+4)-8;if(len<12||udp+8+len>n)continue;
                    byte[] q=Arrays.copyOfRange(buf,udp+8,udp+8+len);String host=DnsPacket.host(q);if(host.isEmpty())continue;
                    boolean p=new LockState(this,"porn").enabled(),e=new LockState(this,"escort").enabled();
                    byte[] answer;
                    if((p&&listed(host,porn))||(e&&listed(host,escort))||custom(host)){answer=DnsPacket.denied(q);}
                    else {
                        try(DatagramSocket socket=new DatagramSocket()){
                            if(!protect(socket))throw new IOException("Could not bypass DNS tunnel");socket.setSoTimeout(2500);
                            InetAddress resolver=InetAddress.getByName(p?"1.1.1.3":"1.1.1.1");socket.connect(resolver,53);
                            socket.send(new DatagramPacket(q,q.length));byte[] reply=new byte[4096];DatagramPacket response=new DatagramPacket(reply,reply.length);socket.receive(response);
                            if(response.getLength()<12||reply[0]!=q[0]||reply[1]!=q[1])continue;answer=Arrays.copyOf(reply,response.getLength());
                        }catch(IOException ex){answer=DnsPacket.denied(q);}
                    }
                    byte[] packet=version==4?DnsPacket.ipv4(buf,ip,answer):DnsPacket.ipv6(buf,answer);out.write(packet);
                }
            }
        }catch(Exception ex){if(running){error=ex.getMessage()==null?"DNS service stopped":ex.getMessage();Notices.send(this,13,"Protection needs attention",error+". Open Badilika to reconnect.");}}
        finally {active=false;running=false;worker=null;stopSelf();}
    }
    private boolean custom(String host){String rules=getSharedPreferences("settings",0).getString("domains","");for(String rule:rules.split("\\s+"))if(!rule.isEmpty()&&(host.equals(rule)||host.endsWith("."+rule)))return true;return false;}
    public void onRevoke(){error="VPN permission was revoked or another VPN connected. Reconnect protection.";active=false;Notices.send(this,13,"Protection disconnected",error);super.onRevoke();stopSelf();}
    public void onDestroy(){running=false;active=false;try{if(tunnel!=null)tunnel.close();}catch(IOException ignored){}if(worker!=null)worker.interrupt();super.onDestroy();}
}
