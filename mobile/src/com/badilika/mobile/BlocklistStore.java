package com.badilika.mobile;

import android.content.*;
import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.*;
import java.util.*;

/** Keeps a bundled list for offline use and refreshes the adult list safely in the background. */
public final class BlocklistStore {
    private static final String PORN_URL="https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts";
    private static final String CACHE="adult-hosts-v1.txt";
    private static final int MAX_BYTES=8*1024*1024,MAX_DOMAINS=300000;
    private static final Set<String> ALLOW=new HashSet<>(Arrays.asList("netflix.com","disneyplus.com","amazon.com","primevideo.com","hulu.com","max.com","hbomax.com","youtube.com","twitch.tv","reddit.com","twitter.com","x.com","instagram.com","facebook.com","linkedin.com","tiktok.com","github.com","wikipedia.org"));
    private BlocklistStore(){}
    public static Set<String> bundled(Context c,String asset) throws IOException {
        Set<String> result=new HashSet<>();
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(c.getAssets().open(asset),"UTF-8"))){read(reader,result);}
        if(asset.equals("porn.txt")){
            File cache=new File(c.getFilesDir(),CACHE);
            if(cache.isFile())try(BufferedReader reader=new BufferedReader(new InputStreamReader(new FileInputStream(cache),"UTF-8"))){read(reader,result);}catch(IOException ignored){}
        }
        return result;
    }
    public static void refreshPorn(Context c,java.util.function.Consumer<Set<String>> installed){
        new Thread(()->{
            File target=new File(c.getFilesDir(),CACHE),temp=new File(c.getFilesDir(),CACHE+".tmp");
            // A successful cache is already loaded at startup.  Refreshing at
            // most twice per day avoids a large download every VPN reconnect.
            if(target.isFile()&&System.currentTimeMillis()-target.lastModified()<12L*60*60*1000)return;
            HttpsURLConnection connection=null;
            try{
                connection=(HttpsURLConnection)new URL(PORN_URL).openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setRequestProperty("User-Agent","Badilika/1.4");
                if(connection.getResponseCode()!=200)throw new IOException("List update unavailable");
                Set<String> result=new HashSet<>();int seen=0;
                try(BufferedReader reader=new BufferedReader(new InputStreamReader(new BoundedInputStream(connection.getInputStream(),MAX_BYTES),"UTF-8"))){read(reader,result);}
                if(result.size()<1000||result.size()>MAX_DOMAINS)throw new IOException("List update was not valid");
                try(Writer out=new OutputStreamWriter(new FileOutputStream(temp),"UTF-8")){for(String domain:result)out.write(domain+"\n");}
                if(!temp.renameTo(target)){try(InputStream in=new FileInputStream(temp);OutputStream out=new FileOutputStream(target)){byte[] bytes=new byte[8192];while((seen=in.read(bytes))>0)out.write(bytes,0,seen);}temp.delete();}
                installed.accept(result);
            }catch(Exception ignored){if(temp.exists())temp.delete();}finally{if(connection!=null)connection.disconnect();}
        },"Badilika list update").start();
    }
    private static void read(BufferedReader reader,Set<String> result)throws IOException{
        String line;while((line=reader.readLine())!=null){String domain=parse(line);if(domain!=null)result.add(domain);if(result.size()>MAX_DOMAINS)throw new IOException("Too many domains");}
    }
    private static String parse(String line){
        line=line.trim().toLowerCase(Locale.US);if(line.isEmpty()||line.startsWith("#"))return null;String[] parts=line.split("\\s+");String candidate=parts.length>=2?parts[1]:parts[0];
        if(!candidate.matches("(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z0-9-]{2,63}"))return null;
        for(String allowed:ALLOW)if(candidate.equals(allowed)||candidate.endsWith("."+allowed))return null;return candidate;
    }
    private static final class BoundedInputStream extends FilterInputStream{
        private final int limit;private int read;
        BoundedInputStream(InputStream in,int limit){super(in);this.limit=limit;}
        public int read(byte[] bytes,int off,int len)throws IOException{int count=super.read(bytes,off,len);if(count>0&&(read+=count)>limit)throw new IOException("List too large");return count;}
        public int read()throws IOException{int value=super.read();if(value>=0&&++read>limit)throw new IOException("List too large");return value;}
    }
}
