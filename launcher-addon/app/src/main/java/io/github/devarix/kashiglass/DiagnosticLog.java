package io.github.devarix.kashiglass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Two bounded private segments; never reads logcat or system files. */
final class DiagnosticLog {
    static final int SEGMENT_BYTES=64*1024;
    private final File current,previous;
    DiagnosticLog(File directory){current=new File(directory,"glass-test.log");previous=new File(directory,"glass-test.previous.log");}
    synchronized void append(String event,String message) throws IOException {
        String line=Instant.now()+" "+clean(event,48)+" "+clean(message,2000)+"\n";
        byte[] data=line.getBytes(StandardCharsets.UTF_8);
        if(current.length()+data.length>SEGMENT_BYTES){
            if(previous.exists()&&!previous.delete())throw new IOException("Cannot rotate previous log");
            if(current.exists()&&!current.renameTo(previous))throw new IOException("Cannot rotate current log");
        }
        try(OutputStream stream=new FileOutputStream(current,true)){stream.write(data);}
    }
    synchronized String snapshot() throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        for(File file:new File[]{previous,current})if(file.exists()){
            try(InputStream in=new FileInputStream(file)){in.transferTo(out);}
        }
        return out.toString(StandardCharsets.UTF_8.name());
    }
    synchronized void clear() throws IOException {
        for(File file:new File[]{previous,current})if(file.exists()&&!file.delete())throw new IOException("Cannot clear logs");
    }
    static String clean(String text,int max){
        if(text==null)return "(none)";
        String value=text.replace('\n',' ').replace('\r',' ').replace('\t',' ');
        return value.substring(0,Math.min(max,value.length()));
    }
}
