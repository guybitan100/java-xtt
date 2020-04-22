package com.mobixell.xtt;

import com.mobixell.xtt.ConvertLib;

public class ByteArrayWrapper
{
    byte[] array;
    public ByteArrayWrapper(byte[] input)
    {
        array=input;
    }
    public ByteArrayWrapper(byte[] input,int start, int stop)
    {
        this.array=new byte[stop-start];
        for(int i=start;i<stop;i++)
        {
            this.array[i-start]=input[i];
        }
    }
    public void setArray(byte[] input)
    {
        array=input;
    }
    public void setArray(byte[] input,int start, int stop)
    {
        this.array=new byte[stop-start];
        for(int i=start;i<stop;i++)
        {
            this.array[i-start]=input[i];
        }
    }
    public byte[] getArray()
    {
        return array;
    }
    public String toString()
    {
        return "\n"+ConvertLib.getHexView(array);
    }
}
