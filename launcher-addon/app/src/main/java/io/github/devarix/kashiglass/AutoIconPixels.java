package io.github.devarix.kashiglass;

/** Pixel-only foreground cleanup; no package lookup, file writes or network requests. */
final class AutoIconPixels {
    private AutoIconPixels(){}
    static int[] silver(int[] source,int size){
        if(size<1||source.length!=size*size)throw new IllegalArgumentException("Invalid square icon");
        int[] out=source.clone();
        int background=source[0];
        boolean flat=(background>>>24)>240;
        for(int corner:new int[]{size-1,size*(size-1),size*size-1})flat&=(source[corner]>>>24)>240&&distance(background,source[corner])<38;
        if(flat){
            boolean[] removed=new boolean[out.length];int[] queue=new int[out.length];int read=0,write=0;
            for(int i=0;i<size;i++)for(int index:new int[]{i,(size-1)*size+i,i*size,i*size+size-1}){
                if(!removed[index]&&distance(background,source[index])<54){removed[index]=true;queue[write++]=index;}
            }
            while(read<write){int p=queue[read++],x=p%size,y=p/size;
                for(int next:new int[]{x>0?p-1:-1,x<size-1?p+1:-1,y>0?p-size:-1,y<size-1?p+size:-1}){
                    if(next>=0&&!removed[next]&&distance(background,source[next])<54){removed[next]=true;queue[write++]=next;}
                }
            }
            // A solid tile or a photograph must not turn into an empty/near-empty glyph.
            int left=source.length-write;
            if(left>source.length/100&&left<source.length*9/10)for(int i=0;i<out.length;i++)if(removed[i])out[i]=0;
        }
        for(int i=0;i<out.length;i++){
            int color=out[i],alpha=color>>>24;
            if(alpha==0)continue;
            int luma=(((color>>16)&255)*54+((color>>8)&255)*183+(color&255)*19)>>8;
            int silver=214+(luma*36/255);out[i]=(alpha<<24)|(silver<<16)|(silver<<8)|silver;
        }
        return out;
    }
    private static int distance(int a,int b){return Math.max(Math.abs(((a>>16)&255)-((b>>16)&255)),Math.max(Math.abs(((a>>8)&255)-((b>>8)&255)),Math.abs((a&255)-(b&255))));}
}
