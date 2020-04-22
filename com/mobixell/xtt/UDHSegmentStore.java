package com.mobixell.xtt;

import java.util.HashMap;

//
public class UDHSegmentStore
{
    private HashMap<String,UDHSegmentArrayWrapper> segmentData  = new HashMap<String,UDHSegmentArrayWrapper>();
    public UDHSegmentStore(){}

    public synchronized void clear()
    {
        segmentData  = new HashMap<String,UDHSegmentArrayWrapper>();
    }

    public synchronized void addData(int reference,byte[] data, int dataStart, int dataEnd,int segtot,int segnum)
    {

        UDHSegment seg=new UDHSegment(reference, data, dataStart, dataEnd, segtot, segnum);

        UDHSegment[] segments=null;

        UDHSegmentArrayWrapper segWrap=segmentData.get(reference+"");
        if(segWrap==null)
        {
            segments=new UDHSegment[segtot];
        }else
        {
            segments=segWrap.getArray();
        }


        segments[segnum-1]=seg;

        segWrap=new UDHSegmentArrayWrapper(segments);
        segmentData.put(reference+"",segWrap);
    }

    public synchronized byte[] getData(int reference)
    {
        UDHSegmentArrayWrapper segWrap=segmentData.get(reference+"");
        UDHSegment[] segments=null;
        if(segWrap==null)
        {
            throw new NullPointerException("Data missing for reference "+reference);
        }else
        {
            segments=segWrap.getArray();
        }
        byte[] data=null;
        byte[] currentdata=null;
        int datalength=0;
        for(int i=0;i<segments.length;i++)
        {
            datalength=datalength+segments[i].data.length;
        }

        data=new byte[datalength];
        int currentpointer=0;

        for(int i=0;i<segments.length;i++)
        {
            currentdata=segments[i].data;
            for(int j=0;j<currentdata.length;j++)
            {
                data[currentpointer++]=currentdata[j];
            }
        }
        return data;
    }

    public synchronized boolean checkAllSegmentsHere(int reference)
    {
        UDHSegmentArrayWrapper segWrap=segmentData.get(reference+"");
        UDHSegment[] segments=null;
        if(segWrap==null)
        {
            return false;
        }else
        {
            segments=segWrap.getArray();
        }
        for(int i=0;i<segments.length;i++)
        {
            XTTProperties.printDebug(this.getClass().getName()+".checkAllSegmentsHere:"+segments[i]);
            if(segments[i]==null)return false;
        }
        return true;
    }

    private static class UDHSegment
    {
        public int reference=-1;
        public int segtot=-1;
        public int segnum=-1;
        public byte[] data=new byte[0];
        public UDHSegment(int reference,byte[] data, int dataStart, int dataEnd,int segtot,int segnum)
        {
            this.reference=reference;
            this.segtot=segtot;
            this.segnum=segnum;
            int fullcount=0;
            this.data=new byte[dataEnd-dataStart];
            for(int i=dataStart;i<data.length&&i<dataEnd;i++)
            {
                this.data[fullcount++]=data[i];
            }
        }
        public String toString(){return "Ref:"+reference+" seg:"+segnum+"/"+segtot+" bytes:"+data.length;}
    }
    private static class UDHSegmentArrayWrapper
    {
        UDHSegment[] array;
        public UDHSegmentArrayWrapper(UDHSegment[] input)
        {
            array=input;
        }
        public UDHSegment[] getArray()
        {
            return array;
        }
    }
}