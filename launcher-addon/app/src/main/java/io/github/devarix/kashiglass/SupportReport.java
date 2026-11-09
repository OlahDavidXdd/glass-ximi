package io.github.devarix.kashiglass;

final class SupportReport {
    private SupportReport(){}
    static String animation(String report){
        int split=report.indexOf("\n\n");String header=split<0?report:report.substring(0,split);String events=split<0?"":report.substring(split+2);
        StringBuilder out=new StringBuilder(header).append("\n\nAnimation and return-state events\n");int count=0;
        for(String line:events.split("\n"))if(line.contains("RETURN_STATE")||line.contains("SOURCE_RETURN")||line.contains("SOURCE_INPUT")||line.contains("ANIMATION")||line.contains("ICON_REPAIRED")||line.contains("HOOK_IDENTITY")||line.contains("HOOK_START")||line.contains("RESTART_")){out.append(line).append('\n');count++;}
        if(count==0)out.append("No matching events. Enable diagnostic logging, open and close an app, then copy this report again.\n");
        return out.toString();
    }
}
