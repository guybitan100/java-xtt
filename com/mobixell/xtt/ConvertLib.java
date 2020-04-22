package com.mobixell.xtt;

import java.util.Vector;
import java.util.TreeSet;
import java.util.Iterator;
import java.util.ArrayList;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import com.mobixell.xtt.ConvertLib;

/**
 * Convert Strings and Numbers

 * @author Roger Soder and Gavin Cattell
 * @version $Id: ConvertLib.java,v 1.62 2010/06/28 09:21:27 awadhai Exp $
 */
public abstract class ConvertLib
{
    public static final String tantau_sccsid = "@(#)$Id: ConvertLib.java,v 1.62 2010/06/28 09:21:27 awadhai Exp $";

    /**
     * Calls {@link #outputBytes(byte[], int, int, int, java.lang.String)} with vlength set to 2.
     *
     * @see #outputBytes(byte[], int, int, int, java.lang.String)
     * @param bytes   array of bytes to convert
     * @param start   integer containing the position to start from in the array
     * @param len     integer containing the number of bytes to convert
     * @param divider String between the bytes
     * @return        String with the bytes values as characters.
     */
    public static String outputBytes(byte[] bytes,int start, int len, String divider)
    {
        return outputBytes(bytes,start, len, 2,divider);
    }
    /**
     * Calls {@link #outputBytes(byte[], int, int, int, java.lang.String)} with divider set to "" and vlength set to 2.
     *
     * @see #outputBytes(byte[], int, int, int, java.lang.String)
     * @param bytes   array of bytes to convert
     * @param start   integer containing the position to start from in the array
     * @param len     integer containing the number of bytes to convert
     * @return        String with the bytes values as characters.
     */
    public static String outputBytes(byte[] bytes,int start, int len)
    {
        return outputBytes(bytes,start, len, 2,"");
    }
    public static String outputBytes(byte[] bytes)
    {
        return outputBytes(bytes,0, bytes.length, 2,"");
    }
    /**
     * Calls {@link #outputBytes(byte[], int, int, int, java.lang.String)} with divider set to "".
     *
     * @see #outputBytes(byte[], int, int, int, java.lang.String)
     * @param bytes   array of bytes to convert
     * @param start   integer containing the position to start from in the array
     * @param len     integer containing the number of bytes to convert
     * @param vlength integer with number of characters to use per byte, filling with leading zeroes.
     * @return        String with the bytes values as characters.
     */
    public static String outputBytes(byte[] bytes,int start, int len, int vlength)
    {
        return outputBytes(bytes,start, len, vlength,"");
    }
    /**
     * Convert a byte array in a String of bytes by using Integer.toHexString().<br>
     * <br>
     * Examples:<br>
     * bytes={255,128,1}, start=0,len=3,vlength=2,divider=" " output="FF 80 01"<br>
     * bytes={0,128,0,255,0}, start=1,len=3,vlength=4,divider="" output="0080000000FF"<br>
     *
     *
     * @param bytes   array of bytes to convert
     * @param start   integer containing the position to start from in the array
     * @param len     integer containing the number of bytes to convert
     * @param vlength integer with number of characters to use per byte, filling with leading zeroes or triming to vlength.
     * @param divider String between the bytes
     * @return        String with the bytes values as characters.
     */
    public static String outputBytes(byte[] bytes,int start, int len, int vlength,String divider)
    {
        StringBuffer s=new StringBuffer();
        String temp=null;
        String div="";
        int current=0;
        for(int i=start;i<bytes.length && i<(start+len);i++)
        {
            s.append(div);
            current=bytes[i];
            temp=Integer.toHexString(current);
            if(temp.length()>vlength)
            {
                temp=temp.substring(temp.length()-vlength,temp.length());
            } else if(temp.length()<vlength)
            {
                for(int j=0;j<(vlength-temp.length());j++)
                {
                    s.append("0");
                }
            }
            s.append(temp);
            div=divider;
        }
        // Do not remove the toUpperCase, if you need lowercase do that again externally
        // Uppercase is needed by most of the code!
        return s.toString().toUpperCase();
    }

    /**
     *  see 3GPP TS 23.040 chapter 9.1.2.3
     */
    public static byte[] getSemiOctetFromNumber(String number)
    {
        //System.out.println("'"+number+"'");
        int len=((number.length()*10)/2+5)/10;
        //System.out.println("len:"+len);
        byte[] bytes=new byte[len];
        int stringpos=0;
        int bytepos=0;
        int val=0;
        while(bytepos<bytes.length)
        {
            //System.out.println(stringpos+":'"+number.substring(stringpos,stringpos+1)+"'");
            val=getSemiOctetVal(number.substring(stringpos,stringpos+1));
            bytes[bytepos]=(byte)(val|0x00F0);
            //System.out.println(bytepos+":'"+outputBytes(bytes,bytepos,1)+"'");
            stringpos++;
            if(stringpos<number.length())
            {
                //System.out.println(stringpos+":'"+number.substring(stringpos,stringpos+1)+"'");
                val=getSemiOctetVal(number.substring(stringpos,stringpos+1));
                bytes[bytepos]=(byte)((val<<4)|(0x000F&bytes[bytepos]));
                //System.out.println(bytepos+":'"+outputBytes(bytes,bytepos,1)+"'");
            }
            bytepos++;
            stringpos++;
        }
        return bytes;
    }
    private static int getSemiOctetVal(String s)
    {
        if(s.length()!=1)throw new IllegalArgumentException("Argument hast to be exactly 1 character long");
        if(s.equals("*"))
        {
            return 0x0A;
        } else if(s.equals("#"))
        {
            return 0x0B;
        } else if(s.equals("a"))
        {
            return 0x0C;
        } else if(s.equals("b"))
        {
            return 0x0D;
        } else if(s.equals("b"))
        {
            return 0x0E;
        } else
        {
            return Integer.decode(s);
        }
    }

    /**
     * Convert a byte string into a String.<br>Calls ConvertLib.getStringFromOctetByteArray(ConvertLib.getBytesFromByteString(input));
     *
     * @see #getStringFromOctetByteArray(byte[])
     * @see #getBytesFromByteString(String)
     * @param input  String to convert
     * @return       String with the values.
     */
    public static String getStringFromByteString(String input)
    {
        return ConvertLib.getStringFromOctetByteArray(ConvertLib.getBytesFromByteString(input));
    }

    /**
     * Convert a byte string into an array of bytes by using Integer.decode().<br>
     * <br>
     * Examples:<br>
     * input="FF8001" output bytes={255,128,1}<br>
     *
     * @param bytestring  String to convert
     * @return      byte array with the values.
     */
    public static byte[] getBytesFromByteString(String bytestring)
    {
        String current="";
        byte[] output=new byte[bytestring.length()/2];
        for(int i=0;i<bytestring.length();i+=2)
        {
            output[i/2]=getByteArrayFromInt((Integer.decode("0x"+bytestring.substring(i,i+2))).intValue(),1)[0];
        }
        return output;
    }

    /**
     * Convert any byte string into an array of bytes by using Integer.decode().<br>
     * <br>
     * Examples:<br>
     * input="FF8001" output bytes={255,128,1}<br>
     * input="FF 80 01" output bytes={255,128,1}<br>
     * input="0xFF 0x80 0x01" output bytes={255,128,1}<br>
     *
     * @param byteString  String to convert
     * @return      byte array with the values.
     */
    public static byte[] getByteArrayFromHexString(String byteString)
    {
        byteString = byteString.replaceAll("[^a-fA-F0-9]|(0x)","");
        return getBytesFromByteString(byteString);
    }

    /**
     * Convert an integer to string by using Integer.toHexString().<br>
     * <br>
     * Examples:<br>
     * value="128" output="80"<br>
     *
     * @param value   integer to convert
     * @return        String with the integer values as Hex string.
     */
    public static String intToHex(int value)
    {
        return longToHex(value,0);
    }
    /**
     * Convert an long to string by using Long.toHexString().<br>
     * <br>
     * Examples:<br>
     * value="128" output="80"<br>
     *
     * @param value   long to convert
     * @return        String with the long values as Hex string.
     */
    public static String longToHex(long value)
    {
        return longToHex(value,0);
    }

    /**
     * Convert an integer to string by using Integer.toHexString().<br>
     * <br>
     * Examples:<br>
     * value="128",vlength=0 output="80"<br>
     * value="128",vlength=4 output="0080"<br>
     * value="65664",vlength=4 output="0080"<br>
     *
     * @param value   integer to convert
     * @param vlength integer with number of characters to use, filling with leading zeroes or triming to vlength.
     * @return        String with the integer values as Hex string.
     */
    public static String intToHex(int value,int vlength)
    {
        return longToHex(value,vlength);
    }
    /**
     * Convert an long to string by using Long.toHexString().<br>
     * <br>
     * Examples:<br>
     * value="128",vlength=0 output="80"<br>
     * value="128",vlength=4 output="0080"<br>
     * value="65664",vlength=4 output="0080"<br>
     *
     * @param value   long to convert
     * @param vlength integer with number of characters to use, filling with leading zeroes or triming to vlength.
     * @return        String with the long values as Hex string.
     */
    public static String longToHex(long value,int vlength)
    {
        StringBuffer s=new StringBuffer();
        String temp=null;
        temp=Long.toHexString(value);
        if(vlength<=0)return temp.toUpperCase();
        if(temp.length()>vlength)
        {
            temp=temp.substring(temp.length()-vlength,temp.length());
        } else if(temp.length()<vlength)
        {
           for(int j=0;j<(vlength-temp.length());j++)
           {
                s.append("0");
            }
        }
        s.append(temp);
        // Do not remove the toUpperCase, if you need lowercase do that again externally
        // Uppercase is needed by most of the code!
        return s.toString().toUpperCase();
    }

    public static final String hex="0123456789abcdef";
    public static String getHexStringFromByteArray(byte[] bs)
    {
        StringBuffer ret = new StringBuffer(bs.length);
        for(byte b : bs)
        {
            int n1 = b & 0xF;
            int n2 = ( b >> 4) & 0xF;
            ret.append(hex.charAt(n2));
            ret.append(hex.charAt(n1));
        }
        return ret.toString();
    }

    /**
     * Convert an integer to string by using Integer.toString().<br>
     * <br>
     * Examples:<br>
     * value="128" output="128"<br>
     *
     * @param value   integer to convert
     * @return        String with the integer values as Hex string.
     */
    public static String intToString(int value)
    {
        return intToString(value,0);
    }
    public static String longToString(long value)
    {
        return longToString(value,0);
    }

    /**
     * Convert an integer to string by using Integer.toString().<br>
     * <br>
     * Examples:<br>
     * value="128",vlength=0 output="128"<br>
     * value="128",vlength=4 output="0128"<br>
     * value="65664",vlength=4 output="5664"<br>
     *
     * @param value   integer to convert
     * @param vlength integer with number of characters to use, filling with leading zeroes or triming to vlength.
     * @return        String with the integer values as Hex string.
     */
    public static String intToString(int value,int vlength)
    {
        return longToString(value,vlength);
    }
    public static String longToString(long value,int vlength)
    {
        StringBuffer s=new StringBuffer();
        String temp=null;
        temp=Long.toString(value);
        if(vlength<=0)return temp;
        if(temp.length()>vlength)
        {
            temp=temp.substring(temp.length()-vlength,temp.length());
        } else if(temp.length()<vlength)
        {
           for(int j=0;j<(vlength-temp.length());j++)
           {
                s.append("0");
            }
        }
        s.append(temp);
        return s.toString().toUpperCase();
    }

    /**
     * Add up all the bytes in the array.<br>
     *
     * @param bytes   byte array with the values
     * @return        integer with.the total sum
     */
    public static int addBytes(byte[] bytes)
    {
        return addBytes(bytes,0,bytes.length);
    }
    /**
     * Add up all the bytes in the array.<br>
     *
     * @param bytes   byte array with the values
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use
     * @return        integer with.the total sum
     */
    public static int addBytes(byte[] bytes, int start, int len)
    {
        int checkSum=0;
        for(int i=start;i<bytes.length&&(i-start<len);i++)
        {
            checkSum=checkSum+bytes[i];
        }
        return checkSum;
    }

    /**
     * Get an integer from a String stored in a byte array. Returns Integer.parseInt(new String(bytes,start,len));
     *
     * @param bytes   byte array with the value
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use
     * @return        integer with.the value the String represents
     */
    public static int getIntFromStringBytes(byte[] bytes, int start, int len)
    {
        //return (Integer.decode(new String(bytes,start,len))).intValue();
        return Integer.parseInt(new String(bytes,start,len));
    }

    /**
     * Get a String from a C-Style null(0x00) terminated byte array.;
     *
     * @param bytes   byte array with the values
     * @return        String from the bytes
     */
    public static String getStringFromCOctetByteArray(byte[] bytes)
    {
        return getStringFromCOctetByteArray(bytes, 0, bytes.length);
    }
    /**
     * Get a String from a C-Style null(0x00) terminated byte array.;
     *
     * @param bytes   byte array with the value
     * @param start   integer with start position in the array
     * @param maxlen  integer with the number of bytes to use, stop earlier when getting a null(0x00)
     * @return        String from the bytes
     */
    public static String getStringFromCOctetByteArray(byte[] bytes, int start, int maxlen)
    {
        StringBuffer tmp=new StringBuffer();
        if(maxlen<=0)
        {
            maxlen=bytes.length-start;
        }
        int origmax=maxlen;
        for(int i=start;i<bytes.length&&(i-start<maxlen);i++)
        {
            if(bytes[i]==(byte)0x00)
            {
                maxlen=i-start;
                break;
            } else if(i+1-start>=maxlen)
            {
                throw new ArrayIndexOutOfBoundsException("String not 0x00 terminated or too long: "+outputBytes(bytes,start,maxlen));
            }
        }
        return createString(bytes,start,maxlen);
    }
    /**
     * Get a String from a byte array.;
     *
     * @param bytes   byte array with the values
     * @return        String from the bytes
     */
    public static String getStringFromOctetByteArray(byte[] bytes)
    {
        return getStringFromOctetByteArray(bytes, 0, bytes.length);
    }
    /**
     * Get a String from a byte array.;
     *
     * @param bytes       byte array with the value
     * @param start       integer with start position in the array
     * @param datalength  integer with the number of bytes to use
     * @return        String from the bytes
     */
    public static String getStringFromOctetByteArray(byte[] bytes, int start, int datalength)
    {
        return createString(bytes,start,datalength);
        /*
        StringBuffer tmp=new StringBuffer();
        for(int i=start;i<bytes.length&&(i-start<datalength);i++)
        {
            tmp.append((char)bytes[i]);
        }
        return tmp.toString();
        */
    }

    /**
     * Get a C-Style null(0x00) terminated byte array from a String
     *
     * @param input   String with the value
     * @return        byte array with the result
     */
    public static byte[] getCOctetByteArrayFromString(String input)
    {
        byte[] output;
        if (input == null)
        {
            output = new byte[1];
            output[0] = 0x00;
        } else
        {
            byte[] data=createBytes(input);
            int len = data.length;
            output = new byte[len + 1];
            for (int i = 0; i < len; i++)
            {
                output[i] = data[i];
            }
            output[len] = (byte) 0x00;
        }
        return output;
    }

    /**
     * Get a byte array from a String
     *
     * @param input   String with the value
     * @return        byte array with the result
     */
    public static byte[] getOctetByteArrayFromString(String input)
    {
        return createBytes(input);
    }


    /**
     * Get an integer from a byte array where the bytes contain the 4 bytes (32 bits) of the integer.
     * bytes[0] contains the MSB, bytes[3] the LSB. BigEndian or network byte order.
     *
     * @param bytes   byte array with the value
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use, maximum is 4
     * @return        integer from the bytes
     */
    public static int getIntFromByteArray(byte[] bytes, int start, int len) throws ArrayIndexOutOfBoundsException
    {
        if (len > 4)
        {
            throw new ArrayIndexOutOfBoundsException(len + " to long for integer conversion");
        }
        byte[] barray = {0,0,0,0};
        int retVal    = 0x00000000;

        for (int i = len-1; i >= 0; i--)
        {
            //System.out.println(i+"+4-"+len+"="+(i+3-len)+" s:"+start+" i:"+i+" l:"+bytes.length);
            barray[i+4-len] = bytes[start + i];
        }

        retVal |= ((((int) barray[0]) << 24) & 0xff000000);
        retVal |= ((((int) barray[1]) << 16) & 0x00ff0000);
        retVal |= ((((int) barray[2]) << 8)  & 0x0000ff00);
        retVal |= ( ((int) barray[3])        & 0x000000ff);

        return retVal;
    }

    /**
     * Get an long from a byte array where the bytes contain the 8 bytes (64 bits) of the long.
     * bytes[0] contains the MSB, bytes[7] the LSB. BigEndian.
     *
     * @param bytes   byte array with the value
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use, maximum is 8
     * @return        long from the bytes
     */
    public static long getLongFromByteArray(byte[] bytes, int start, int len) throws ArrayIndexOutOfBoundsException
    {
        if (len > 8)
        {
            throw new ArrayIndexOutOfBoundsException(len + " to long for long conversion");
        }
        byte[] barray = {0,0,0,0,0,0,0,0};
        long retVal    = 0x0000000000000000;

        for (int i = len-1; i >= 0; i--)
        {
            //System.out.println(i+"+4-"+len+"="+(i+3-len)+" s:"+start+" i:"+i+" l:"+bytes.length);
            barray[i+8-len] = bytes[start + i];
        }

        retVal |= ((((long) barray[0]) << 56) & 0xff00000000000000l);
        retVal |= ((((long) barray[1]) << 48) & 0x00ff000000000000l);
        retVal |= ((((long) barray[2]) << 40) & 0x0000ff0000000000l);
        retVal |= ((((long) barray[3]) << 32) & 0x000000ff00000000l);
        retVal |= ((((long) barray[4]) << 24) & 0x00000000ff000000l);
        retVal |= ((((long) barray[5]) << 16) & 0x0000000000ff0000l);
        retVal |= ((((long) barray[6]) << 8)  & 0x000000000000ff00l);
        retVal |= ( ((long) barray[7])        & 0x00000000000000ffl);

        return retVal;
    }

    /**
     * Get an integer from a byte array where the bytes contain the 4 bytes (32 bits) of the integer.
     * bytes[3] contains the MSB, bytes[0] the LSB. LittleEndian.
     *
     * @param bytes   byte array with the value
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use, maximum is 4
     * @return        integer from the bytes
     */
    public static int getIntFromLittleEndianByteArray(byte[] bytes, int start, int len) throws ArrayIndexOutOfBoundsException
    {
        if (len > 4)
        {
            throw new ArrayIndexOutOfBoundsException(len + " to long for integer conversion");
        }
        byte[] barray = {0,0,0,0};
        int retVal    = 0x00000000;

        for (int i = 0; i < len; i++)
        {
            //System.out.println(i+"+4-"+len+"="+(i+3-len)+" s:"+start+" i:"+i+" l:"+bytes.length);
            barray[i] = bytes[start + i];
        }

        retVal |= ((((int) barray[3]) << 24) & 0xff000000);
        retVal |= ((((int) barray[2]) << 16) & 0x00ff0000);
        retVal |= ((((int) barray[1]) << 8)  & 0x0000ff00);
        retVal |= ( ((int) barray[0])        & 0x000000ff);

        return retVal;
    }

    /**
     * Get a byte array from an integer where the bytes contain the 4 bytes (32 bits) of the integer.
     * bytes[0] contains the MSB, bytes[3] the LSB.
     *
     * @param intVal  integer to get the bytes from
     * @return        byte array with the value of the integer
     */
    public static byte[] getByteArrayFromInt(int intVal)
    {
        if(intVal<0x100)
        {
            return getByteArrayFromInt(intVal,1);
        } else if(intVal<0x10000)
        {
            return getByteArrayFromInt(intVal,2);
        } else if(intVal<0x1000000)
        {
            return getByteArrayFromInt(intVal,3);
        } else
        {
            return getByteArrayFromInt(intVal,4);
        }
    }
    public static byte[] getByteArrayFromLong(long longVal)
    {
        if(longVal<0x100l)
        {
            return getByteArrayFromLong(longVal,1);
        } else if(longVal<0x10000l)
        {
            return getByteArrayFromLong(longVal,2);
        } else if(longVal<0x1000000l)
        {
            return getByteArrayFromLong(longVal,3);
        } else if(longVal<0x100000000l)
        {
            return getByteArrayFromLong(longVal,4);
        } else if(longVal<0x10000000000l)
        {
            return getByteArrayFromLong(longVal,5);
        } else if(longVal<0x1000000000000l)
        {
            return getByteArrayFromLong(longVal,6);
        } else if(longVal<0x100000000000000l)
        {
            return getByteArrayFromLong(longVal,7);
        } else
        {
            return getByteArrayFromLong(longVal,8);
        }
    }

    /**
    Create uIntVar according to WAP-230-WSP-20010705-a chapter 8.1.2 Variable Length Unsigned Integers.
    */
    public static byte[] getUIntVarFromInt(int intVal)
    {
        byte[] retVal=null;
        if(intVal<0)throw new IllegalArgumentException("uIntVal is UNSIGNED integer only, negative numbers not suported!");
        if(intVal>=(268435456)) //2^28
        {
            retVal=new byte[5];
            retVal[4]=(byte)(intVal&0x7F);
            intVal=intVal>>7;
            retVal[3]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[2]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[1]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[0]=(byte)((intVal&0x7F)|0x80);
        } else if(intVal>=(2097152)) //2^21
        {
            retVal=new byte[4];
            retVal[3]=(byte)(intVal&0x7F);
            intVal=intVal>>7;
            retVal[2]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[1]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[0]=(byte)((intVal&0x7F)|0x80);
        } else if(intVal>=(16384)) //2^14
        {
            retVal=new byte[3];
            retVal[2]=(byte)(intVal&0x7F);
            intVal=intVal>>7;
            retVal[1]=(byte)((intVal&0x7F)|0x80);
            intVal=intVal>>7;
            retVal[0]=(byte)((intVal&0x7F)|0x80);
        } else if(intVal>=(128)) //2^7
        {
            retVal=new byte[2];
            retVal[1]=(byte)(intVal&0x7F);
            intVal=intVal>>7;
            retVal[0]=(byte)((intVal&0x7F)|0x80);
        } else
        {
            retVal=new byte[1];
            retVal[0]=(byte)(intVal&0x7F);
        }
        return retVal;
    }

    /**
     * Get a byte array from an integer where the bytes contain the 4 bytes (32 bits) of the integer.
     * bytes[0] contains the MSB, bytes[3] the LSB.
     *
     * @param intVal  integer to get the bytes from
     * @param len     integer with the number of bytes to use, maximum is 4
     * @return        byte array with the value of the integer
     */
    public static byte[] getByteArrayFromInt(int intVal, int len)
    {
        return getByteArrayFromLong((long)intVal,len);
    }

    /**
     * Get a byte array from an long where the bytes contain the 8 bytes (64 bits) of the long.
     * bytes[0] contains the MSB, bytes[3] the LSB.
     *
     * @param longVal  integer to get the bytes from
     * @param len     integer with the number of bytes to use, maximum is 8
     * @return        byte array with the value of the long
     */
    public static byte[] getByteArrayFromLong(long longVal, int len)
    {
        byte[] retVal = new byte[len];
        int shift = (len - 1) * 8;

        for (int i = 0; i < len; i++)
        {
            retVal[i] = (byte)(longVal >>> shift);
            shift -= 8;
        }
        return retVal;
    }
    /**
     * Check if a String contains a certain substring via regular expression..
     * This stores the actuall groups under variable/matnumber/groupnumber
     * So if you need to access group 0 of the first match it would be in variable/0/0
     * If you defined groups you'll find group 1 under variable/matchnumber (also variable/matchnumber/1) and
     * if you have no groups then variable/matchnumber (also variable/matchnumber/0) will contain the full match.
     *
     * @param classname  String containing the name of the calling class for debug output.
     * @param data       String to check
     * @param regex      String containing regular expression to use
     * @param variable   String containing variable name to store result in
     * @return           boolean true=success, false=failed
     */
    public static boolean queryString(String classname,String data, String regex, String variable)
    {
        if((data != null)&&(data.length() != 0))
        {
            if(regex.equals(""))
            {
                XTTProperties.printFail(classname+": wanted: Nothing to check found:\n"+data);
                XTTProperties.setVariable(variable,null);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
            Pattern pattern = Pattern.compile(regex,Pattern.DOTALL);
            Matcher matcher = pattern.matcher(data);
            boolean found=false;
            int length=0;
            StringBuffer output=new StringBuffer("");
            String start=null;
            while(matcher.find())
            {
                found=true;
                if(length==0)
                {
                    if (matcher.groupCount() > 0)
                    {
                        start=classname+": Regex result: '" + matcher.group(1) + "' stored to " + variable;
                        XTTProperties.setVariable(variable, matcher.group(1));
                    } else
                    {
                        start=classname+": Regex result: '" + matcher.group(0) + "' stored to " + variable;
                        XTTProperties.setVariable(variable, matcher.group(0));
                    }
                }
                // This stores the actuall groups under variable/matchnumber/groupnumber
                // So if you need to access group 0 of the first match it would be in variable/0/0
                // If you defined groups you'll find group 1 under variable/matchnumber (also variable/matchnumber/1) and
                // if you have no groups then variable/matchnumber (also variable/matchnumber/0) will contain the full match.
                for (int i=0;i<=matcher.groupCount();i++)
                {
                    XTTProperties.setVariable(variable + "/"+length+"/"+i, matcher.group(i));
                }
                if (matcher.groupCount() > 0)
                {
                    output.append("\n "+length+": '" + matcher.group(1) + "' stored to " + variable+ "/"+length+" groups:"+matcher.groupCount());
                    XTTProperties.setVariable(variable+ "/"+length, matcher.group(1));
                } else
                {
                    output.append("\n "+length+": '" + matcher.group(0) + "' stored to " + variable+ "/"+length);
                    XTTProperties.setVariable(variable+ "/"+length, matcher.group(0));
                }
                XTTProperties.setVariable(variable + "/length", ""+(++length));
            }

            if(!found)
            {
                XTTProperties.printFail(classname+": pattern '"+regex+"' not found");
                XTTProperties.setVariable(variable,null);
                //XTTProperties.printDebug(classname+": data:\n" + data);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            } else
            {
                XTTProperties.printInfo(start+"\n Number of Matches = "+length+output.toString());
                XTTProperties.printInfo("\n Number of Matches length stored to "+variable+"/length");
                return true;
            }
        } else
        {
            if(regex.equals(""))
            {
                XTTProperties.printInfo(classname+": found: Nothing to check");
                XTTProperties.setVariable(variable,null);
                return true;
            } else
            {
                XTTProperties.printFail(classname+": Nothing to check: pattern '"+regex+"'");
                XTTProperties.setVariable(variable,null);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            }
        }

    }

    public static boolean decodeHTML(String funcname, String variable, String baseURL, String htmlBody)
    {
        return decodeHTML(funcname, variable, baseURL, htmlBody, true);
    }
    public static boolean decodeHTML(String funcname, String variable, String baseURL, String htmlBody, boolean doOutput)
    {
            String imgRegex="<[iI][mM][gG].*?[sS][rR][cC]=\"(.*?)\".*?>";
            String linkRegex="<[aA].*?[hH][rR][eE][fF]=\"(.*?)\".*?>";

            Pattern pattern = Pattern.compile("((http://.*?)/(.*/)?).*",Pattern.DOTALL);
            Matcher matcher = pattern.matcher(baseURL);
            String serverURL="";
            String directoryURL="";
            if(matcher.find())
            {
                directoryURL=matcher.group(1);
                serverURL=matcher.group(2);
                //System.out.println("directoryURL="+directoryURL);
                //System.out.println("serverURL   ="+serverURL);
            } else
            {
                //System.out.println("baseURL   ="+baseURL);
            }
            StringBuffer output=new StringBuffer(funcname+": decoded '"+baseURL+"' HTML document to:");
            boolean retval=true;

            retval=decodeHTML(funcname,variable,"/image",serverURL,directoryURL,htmlBody,imgRegex ,output)&&retval;

            retval=decodeHTML(funcname,variable,"/link" ,serverURL,directoryURL,htmlBody,linkRegex,output)&&retval;

            if(doOutput)
            {
                XTTProperties.printInfo(output.toString());
            }
            return retval;
    }
    private static boolean decodeHTML(String funcname,String variable,String varExtension,String serverURL,String directoryURL,String htmlBody, String regex, StringBuffer output)
    {
        Pattern pattern = Pattern.compile(regex,Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlBody);
        String found=null;
        TreeSet<String> urls=new TreeSet<String>();
        while(matcher.find())
        {
            found=matcher.group(1);
            if(found.startsWith("http://"))
            {
                urls.add(found);
            } else if(found.startsWith("/"))
            {
                urls.add(serverURL+found);
            } else
            {
                urls.add(directoryURL+found);
            }
        }
        Iterator<String> it=urls.iterator();
        int length=0;
        XTTProperties.setVariable(variable +varExtension+ "/length", ""+urls.size());
        output.append(ConvertLib.createString("\n "+variable +varExtension+ "/length"+"",25)+": "+urls.size());
        String value=null;
        while(it.hasNext())
        {
            value=it.next();
            XTTProperties.setVariable(variable +varExtension+ "/"+(length), ""+value);
            if(XTTProperties.printDebug(null))
            {
                output.append(ConvertLib.createString("\n "+variable +varExtension+ "/"+length,25)+": "+value);
            }
            length++;
        }
        return true;
    }

    /**
     * Check if a String does not contain a certain substring via regular expression..
     *
     * @param classname  String containignthe name of the calling class for debug output.
     * @param data       String to check
     * @param regex      String containing regular expression to use
     * @return           boolean true=success, false=failed
     */
    public static boolean queryStringNegative(String classname,String data, String regex)
    {
        if((data != null)&&(data.length() != 0))
        {
            if(regex.equals(""))
            {
                XTTProperties.printInfo(classname+": wanted: Something to check found:\n"+data);
                return true;
            }
            Pattern pattern = Pattern.compile(regex,Pattern.DOTALL);
            Matcher matcher = pattern.matcher(data);
            if(matcher.find())
            {
                XTTProperties.printFail(classname+": '" + matcher.group(0) + "' found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            } else
            {
                XTTProperties.printInfo(classname+": pattern '"+regex+"' not found");
                return true;
            }
        } else
        {
            if(regex.equals(""))
            {
                XTTProperties.printFail(classname+": Nothing to check: pattern '"+regex+"'");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return false;
            } else
            {
                XTTProperties.printInfo(classname+": found: Nothing to check");
                return true;
            }
        }
    }

    public static boolean checkPattern(String content, String spattern)
    {
        if(spattern==null||content==null)return false;
        Pattern pattern = Pattern.compile(spattern,Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        boolean match=matcher.find();
        return match;
    }


    public static boolean compareBytes(byte[] a1, byte[] a2)
    {
        int len =a1.length;
        if(a2.length < len)
        {
            len=a2.length;
        }

        return compareBytes(a1,0,a2,0,len);
    }

    public static boolean compareBytes(byte[] a1, byte[] a2, int len)
    {
        return compareBytes(a1,0,a2,0,len);
    }

    public static boolean compareBytes(byte[] a1, int offs1, byte[] a2, int offs2, int len)
    {
        while (len-- > 0) {
            if (a1[offs1++] != a2[offs2++]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Converts a String into a Base64 encoded string.
     *
     * @param s  String to encode into base64
     * @return        String with the encoded data
     */
    public static String base64Encode(String s)
    {
        return base64Encode(s,true);
    }

    /**
     * Converts a String into a Base64 encoded string.
     *
     * @param s  String to encode into base64
     * @param addCRLF if true, adds a CRLF every 72 characters to the encoded data
     * @return        String with the encoded data
     */
    public static String base64Encode(String s, boolean addCRLF)
    {
        return base64Encode(createBytes(s),addCRLF);
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     *
     * @param b  byte array to encode into base64
     * @return        String with the encoded data
     */
    public static String base64Encode(byte[] b)
    {
        return base64Encode(b,true);
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     *
     * @param b  byte array to encode into base64
     * @param addCRLF if true, adds a CRLF every 72 characters to the encoded data
     * @return        String with the encoded data
     */
    public static String base64Encode(byte[] b, boolean addCRLF)
    {
        return base64Encode(b,0,b.length,addCRLF);
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     *
     * @param b       byte array with the values
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use
     * @return        String with the encoded data
     */
    public static String base64Encode(byte[] b, int start, int len)
    {
        return base64Encode(b,start,len,true);
    }

    /**
     * Converts a byte array into a Base64 encoded string.
     *
     * @param b       byte array with the values
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use
     * @param addCRLF if true, adds a CRLF every 72 characters to the encoded data
     * @return        String with the encoded data
     */
    public static String base64Encode(byte[] b, int start, int len, boolean addCRLF)
    {
        char pad = '=';
        char[] nibble2code = {
            'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P',
            'Q','R','S','T','U','V','W','X','Y','Z','a','b','c','d','e','f',
            'g','h','i','j','k','l','m','n','o','p','q','r','s','t','u','v',
            'w','x','y','z','0','1','2','3','4','5','6','7','8','9','+','/'
        };
        byte[] code2nibble = new byte[256];
        for (int i=0;i<256;i++)
        {
            code2nibble[i]=-1;
        }
        for (byte bb=0;bb<64;bb++)
        {
            code2nibble[(byte)nibble2code[bb]]=bb;
        }

        int bLen = len;
        int rLen = ((bLen+2)/3)*4;
        char r[] = new char[rLen];
        /*The line below adds 2 extra bytes for newlines every 72 bytes.
          char r[] = new char[rLen+ ((rLen/72)*2)];
          This isn't so clean, and we'll just split the string at the end instead*/
        int ri = 0;
        int bi = start;
        byte b0, b1, b2;
        int stop = ((bLen/3)*3)+start;

        try
        {
            while (bi < stop)
            {
                b0 = b[bi++];
                b1 = b[bi++];
                b2 = b[bi++];

                r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
                r[ri++] = nibble2code[(b0 << 4) & 0x3f | (b1 >>> 4) & 0x0f];
                r[ri++] = nibble2code[(b1 << 2) & 0x3f | (b2 >>> 6) & 0x03];
                r[ri++] = nibble2code[ b2 & 0x3f];

                //Every 54 bytes add the newline. (54/3)*4 = 72
                /*if((bi%54) == 0)
                {
                    r[ri++] = '\r';
                    r[ri++] = '\n';
                }*/
            }
            if (b.length != bi)
            {
                switch (bLen % 3)
                {
                    case 2:
                        b0 = b[bi++];
                        b1 = b[bi++];
                        r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
                        r[ri++] = nibble2code[(b0 << 4) & 0x3f | (b1 >>> 4) & 0x0f];
                        r[ri++] = nibble2code[(b1 << 2) & 0x3f];
                        r[ri++] = pad;
                        break;
                    case 1:
                        b0 = b[bi++];
                        r[ri++] = nibble2code[(b0 >>> 2) & 0x3f];
                        r[ri++] = nibble2code[(b0 << 4) & 0x3f];
                        r[ri++] = pad;
                        r[ri++] = pad;
                        break;
                    default:
                        break;
                }
            }
        }
        catch(ArrayIndexOutOfBoundsException aioobe)
        {
            XTTProperties.printFail("base64Encode: Error encoding data start=" + start + ", len=" + len + ", data.size=" + b.length);
            XTTProperties.printDebug("base64Encode: bi=" + bi + ", ri=" + ri + ", rLen="+rLen+", stop="+stop);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            throw aioobe;
        }
        //Workaround to fix the problem when a file contains length%54==0 bytes.
        /*if (ri == (r.length-2))
        {
            r[ri++] = '\r';
            r[ri++] = '\n';
        }*/

        //Do this instead of inserting the newlines during the encode.
        String base64 = new String(r);
        StringBuffer base64WithNewlines = new StringBuffer();

        int k=0;

        //System.out.println("String before >>" + base64 + "<<");

        if(addCRLF)
        {
            for(int i=0;i<base64.length();)
            {
                i=i+72;
                if(i>=base64.length())i=base64.length();

                base64WithNewlines.append(base64.substring(k,i));
                base64WithNewlines.append("\r\n");
                k=i;
            }
            base64 = base64WithNewlines.toString();
        }

        //System.out.println("String after >>" + base64WithNewlines + "<<");

        return base64;
    }

    /**
     * Decodes a Base64 encoded String into a byte array.
     *
     * @param s    String with the encoded values
     * @return     byte array with the decoded data
     */
    public static byte[] base64Decode (String s)
    {
        String trimmed = s.replaceAll("[^0-9a-zA-Z+=/]","");
        //if(s.length() != trimmed.length())
        //    XTTProperties.printDebug("base64Decode:" + (s.length() - trimmed.length()) + "characters deleted");

        return base64Decode(createBytes(trimmed));

    }

    /**
     * Decodes a Base64 encoded String into a byte array.
     *
     * @param data    byte array with the encoded values
     * @return        byte array with the decoded data
     */
    private static byte[] base64Decode(byte[] data)
    {
        return base64Decode(data,0,data.length);
    }

    /**
     * Decodes a Base64 encoded String into a byte array.
     *
     * @param data    byte array with the encoded values
     * @param start   integer with start position in the array
     * @param len     integer with the number of bytes to use
     * @return        byte array with the decoded data
     */
    private static byte[] base64Decode(byte[] data, int start, int len)
    {
        if(data.length==0)return new byte[0];
        byte[] base64DecMap = new byte[128];

        base64DecMap[(byte)'A']=0;
        base64DecMap[(byte)'B']=1;
        base64DecMap[(byte)'C']=2;
        base64DecMap[(byte)'D']=3;
        base64DecMap[(byte)'E']=4;
        base64DecMap[(byte)'F']=5;
        base64DecMap[(byte)'G']=6;
        base64DecMap[(byte)'H']=7;
        base64DecMap[(byte)'I']=8;
        base64DecMap[(byte)'J']=9;
        base64DecMap[(byte)'K']=10;
        base64DecMap[(byte)'L']=11;
        base64DecMap[(byte)'M']=12;
        base64DecMap[(byte)'N']=13;
        base64DecMap[(byte)'O']=14;
        base64DecMap[(byte)'P']=15;
        base64DecMap[(byte)'Q']=16;
        base64DecMap[(byte)'R']=17;
        base64DecMap[(byte)'S']=18;
        base64DecMap[(byte)'T']=19;
        base64DecMap[(byte)'U']=20;
        base64DecMap[(byte)'V']=21;
        base64DecMap[(byte)'W']=22;
        base64DecMap[(byte)'X']=23;
        base64DecMap[(byte)'Y']=24;
        base64DecMap[(byte)'Z']=25;
        base64DecMap[(byte)'a']=26;
        base64DecMap[(byte)'b']=27;
        base64DecMap[(byte)'c']=28;
        base64DecMap[(byte)'d']=29;
        base64DecMap[(byte)'e']=30;
        base64DecMap[(byte)'f']=31;
        base64DecMap[(byte)'g']=32;
        base64DecMap[(byte)'h']=33;
        base64DecMap[(byte)'i']=34;
        base64DecMap[(byte)'j']=35;
        base64DecMap[(byte)'k']=36;
        base64DecMap[(byte)'l']=37;
        base64DecMap[(byte)'m']=38;
        base64DecMap[(byte)'n']=39;
        base64DecMap[(byte)'o']=40;
        base64DecMap[(byte)'p']=41;
        base64DecMap[(byte)'q']=42;
        base64DecMap[(byte)'r']=43;
        base64DecMap[(byte)'s']=44;
        base64DecMap[(byte)'t']=45;
        base64DecMap[(byte)'u']=46;
        base64DecMap[(byte)'v']=47;
        base64DecMap[(byte)'w']=48;
        base64DecMap[(byte)'x']=49;
        base64DecMap[(byte)'y']=50;
        base64DecMap[(byte)'z']=51;
        base64DecMap[(byte)'0']=52;
        base64DecMap[(byte)'1']=53;
        base64DecMap[(byte)'2']=54;
        base64DecMap[(byte)'3']=55;
        base64DecMap[(byte)'4']=56;
        base64DecMap[(byte)'5']=57;
        base64DecMap[(byte)'6']=58;
        base64DecMap[(byte)'7']=59;
        base64DecMap[(byte)'8']=60;
        base64DecMap[(byte)'9']=61;
        base64DecMap[(byte)'+']=62;
        base64DecMap[(byte)'/']=63;

        int tail = len;
        while (data[tail-1] == '=')  tail--;

        byte dest[] = new byte[( (tail-1) - ( (tail-1) /4) )];
        // ascii printable to 0-63 conversion
        for (int idx = start; idx <(start+len); idx++)
        {
           data[idx] = base64DecMap[data[idx]];
        }
        // 4-byte to 3-byte conversion
        int sidx, didx;
        for (sidx = start, didx=0; didx < dest.length-2; sidx += 4, didx += 3)
        {
            dest[didx]   = (byte) ( ((data[sidx]   << 2) & 255) | ((data[sidx+1] >>> 4) & 003) );
            dest[didx+1] = (byte) ( ((data[sidx+1] << 4) & 255) | ((data[sidx+2] >>> 2) & 017) );
            dest[didx+2] = (byte) ( ((data[sidx+2] << 6) & 255) | (data[sidx+3]         & 077) );
        }

        if (didx < dest.length)
        {
            dest[didx]   = (byte) ( ((data[sidx]   << 2) & 255) | ((data[sidx+1] >>> 4) & 003) );
        }

        if (++didx < dest.length)
        {
            dest[didx]   = (byte) ( ((data[sidx+1] << 4) & 255) | ((data[sidx+2] >>> 2) & 017) );
        }
        return dest;
    }

    /*public static void getHexView(String stream)
    {
        StringBuffer outputBuffer = new StringBuffer();
        int number = 8;

        outputBuffer.append("Input    | Numerical ASCII Values          | HEX ASCII Values\r\n");
        if(stream==null)
        {
            outputBuffer.append("null\r\n");
            System.out.println(outputBuffer);
            return;
        }

        for (int i = 0; i < stream.length(); i+= 8)
        {
            if ((i+8)>=stream.length())
            {
                number = stream.length()-i;
            }

            //Print Actual Text (only visible characters though)
            for (int j=i;j<(i+number);j++)
            {
                if (((int)(stream.charAt(j))<32) || ((int)(stream.charAt(j))>255))
                {
                    outputBuffer.append(".");
                }
                else
                {
                    outputBuffer.append(stream.charAt(j));
                }
            }

            //Print spaces between Text and ACSII values
            for (int b=number;b<8;b++)
            {
                outputBuffer.append(" ");
            }
            outputBuffer.append(" | ");

            //Print the numerical ACSII values
            for (int k=i;k<(i+number);k++)
            {
                if ( (int)(stream.charAt(k)) < 100 )
                {
                    outputBuffer.append("0" + (int)(stream.charAt(k)) + " ");
                }
                else
                {
                    outputBuffer.append((int)(stream.charAt(k)) + " ");
                }
            }

            //Print spaces between numerical ACSII and HEX
            for (int b=number;b<8;b++)
            {
                outputBuffer.append("    ");
            }

            outputBuffer.append("| ");
            String out="";
            //Print HEX
            for (int k=i;k<(i+number);k++)
            {
                if((int)(stream.charAt(k)) < 16)
                {
                    out=new String("0" + Integer.toHexString((int)(stream.charAt(k))) + " ");
                }
                else
                {
                    out=new String(Integer.toHexString((int)(stream.charAt(k))) + " ");
                }
                outputBuffer.append(out.toUpperCase());
            }
            outputBuffer.append("\r\n");

         //Integer.toHexString(i);

        }
        return outputBuffer;
    }*/

    public static String getHexView(String stream)
    {
        if(stream==null)
        {
            return getHexView(null,0, 0);
        } else
        {
            return getHexView(createBytes(stream),0, stream.length());
        }
    }

    public static String getHexView(byte[] stream)
    {
        if(stream==null)
        {
            return getHexView(stream,0, 0);
        } else
        {
            return getHexView(stream,0, stream.length);
        }
    }

    public static String getHexView(byte[] stream,int start, int stop)
    {
        StringBuffer outputBuffer = new StringBuffer();
        int number = 8;
        outputBuffer.append("Input    | Numerical ASCII Values          | HEX ASCII Values\r\n");
        if(stream==null)
        {
            outputBuffer.append("null\r\n");
            System.out.println(outputBuffer);
            return "";
        }
        //If the stop was less than zero, just print it all
        if(stop < 0)
        {
            stop = stream.length;
        }
        for (int i = start; i < stream.length&&i<stop; i+= 8)
        {
            if ((i+8)>=stream.length)
            {
                number = stream.length-i;
            } else if((i+8)>=stop)
            {
                number = stop-i;
            }

            //Print Actual Text (only visible characters though)
            for (int j=i;j<(i+number);j++)
            {
                if ((stream[j]<32) || (stream[j]>255))
                {
                    outputBuffer.append(".");
                }
                else
                {
                    outputBuffer.append((new Character((char)stream[j])));
                }
            }

            //Print spaces between Text and ACSII values
            for (int b=number;b<8;b++)
            {
                outputBuffer.append(" ");
            }
            outputBuffer.append(" | ");

            int currentValue=0;
            //Print the numerical ACSII values
            for (int k=i;k<(i+number);k++)
            {
                currentValue=stream[k];
                if(currentValue<0)currentValue=currentValue+256;
                if ( currentValue < 10 )
                {
                    outputBuffer.append("00" + currentValue + " ");
                } else if ( currentValue < 100 )
                {
                    outputBuffer.append("0" + currentValue + " ");
                }
                else
                {
                    outputBuffer.append(currentValue + " ");
                }
            }

            //Print spaces between numerical ACSII and HEX
            for (int b=number;b<8;b++)
            {
                outputBuffer.append("    ");
            }

            outputBuffer.append("| ");
            String out="";
            //Print HEX
            for (int k=i;k<(i+number);k++)
            {
                currentValue=stream[k];
                if(currentValue<0)currentValue=currentValue+256;
                if(currentValue < 16)
                {
                    out="0" + Integer.toHexString(currentValue)+" ";
                }
                else
                {
                    out=Integer.toHexString(currentValue)+" ";
                }
                outputBuffer.append(out.toUpperCase());
            }
            outputBuffer.append("\r\n");


         //Integer.toHexString(i);

        }
        if((stop < stream.length) || (start > 0))
        {
            outputBuffer.append(" - ");
            if(start>0)
                outputBuffer.append("" + start + " bytes skipped at start ");
            if (stop < stream.length)
                outputBuffer.append("" + (stream.length - stop) + " bytes skipped from end\r\n");
        }
        return outputBuffer.toString();
    }

    public static boolean textToBoolean(String text)
    {
        if(text==null)return false;

        if(text.equalsIgnoreCase("true")
         ||text.equalsIgnoreCase("yes")
         ||text.equalsIgnoreCase("ok")
         ||text.equalsIgnoreCase("1")
         ||text.equalsIgnoreCase("ja")
         ||text.equalsIgnoreCase("oui")
         ||text.equalsIgnoreCase("t")
         ||text.equalsIgnoreCase("one")
         // WAP WSP true, (129 or 0x81 is false)
         ||text.equalsIgnoreCase("128")
         ||text.equalsIgnoreCase("0x80")
          )
        {
            return true;
        } else
        {
            return false;
        }
    }

    public static int addBytesToArray(byte[] base, int basePointer, byte[] data)
    {
        return addBytesToArray(base,basePointer,data,0,data.length);
    }
    public static int addBytesToArray(byte[] base, int basePointer, byte[] data,int dataStart,int datalength)
    {
        for(int i=dataStart;i<data.length&&i<(datalength+dataStart);i++)
        {
            base[basePointer++]=data[i];
        }
        return basePointer;
    }

    public static String addPrefixToString(String data, int minlength, String filler)
    {
        if(filler.length()<=0)throw new IllegalArgumentException("filler String has to be at least 1 character long!");
        StringBuffer b=new StringBuffer(data);
        while(b.length()<minlength)
        {
            b.insert(0,filler);
        }
        return b.toString();
    }

    public static String addSuffixToString(String data, int minlength, String filler)
    {
        return createString(data,minlength,filler);
    }

    public static String createString(String data)
    {
        return new String(data);
    }
    /** Creates a string of minimum length minlength filled with spaces
    */
    public static String createString(String data, int minlength)
    {
        return createString(data,minlength," ");
    }
    public static String createString(String data, int minlength, String filler)
    {
        if(filler.length()<=0)throw new IllegalArgumentException("filler String has to be at least 1 character long!");
        StringBuffer b=new StringBuffer(data);
        while(b.length()<minlength)
        {
            b.append(filler);
        }
        return b.toString();
    }
    public static String createString(byte[] data)
    {
        return createString(data,0,data.length);
    }

    public static String createString(byte[] data, int off, int len)
    {
        try
        {
            return new String(data,off,len,XTTProperties.getCharSet());
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            return new String(data,off,len);
        }

    }

    public static byte[] createBytes(String data)
    {
        try
        {
            return data.getBytes(XTTProperties.getCharSet());
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+XTTProperties.getCharSet());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            return data.getBytes();
        }
    }

    public static byte[] createBytes(String data, String charSet)
    {
        try
        {
            return data.getBytes(charSet);
        } catch (java.io.UnsupportedEncodingException uee)
        {
            XTTProperties.printWarn("Unsupported charset: "+charSet);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(uee);
            }
            return data.getBytes();
        }
    }

    public static String getHexMD5Hash(String data)
    {
        return getHexMD5Hash(createBytes(data));
    }

    public static String getHexMD5Hash(byte[] data)
    {
        byte[] md5Digest = new byte[0];
        try
        {
            MessageDigest md5Hash = MessageDigest.getInstance("MD5");
            md5Digest = md5Hash.digest(data);
            return getHexStringFromByteArray(md5Digest);
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        return "";
    }

    public static String getHexSha512Hash(String data)
    {
        return getHexSha512Hash(createBytes(data));
    }

    public static String getHexSha512Hash(byte[] data)
    {
        byte[] sha512Digest = new byte[0];
        try
        {
            MessageDigest sha512Hash = MessageDigest.getInstance("SHA-512");
            sha512Digest = sha512Hash.digest(data);
            return getHexStringFromByteArray(sha512Digest);
        }
        catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
        return "";
    }

    public static String getStringFromVector(Vector<String> vec, String delimeter)
    {
        StringBuffer completeString = new StringBuffer();
        String seperator = "";
        for(int j=0;j<(vec.size());j++)
        {
            completeString.append(seperator);
            completeString.append(vec.get(j));
            seperator = delimeter;
        }
        return completeString.toString();
    }

    public static String shortFileName(String filename)
    {
        return shortFileName(filename, XTTProperties.getXTTFilePath());
    }
    public static String shortFileName(String filename, String pathRelativeTo)
    {
        String showname=filename;
        // Circumvent stupid bug in filechoser, returns c:\ as C:\ whereas File returns it as c:\
        // Windows
        if(java.io.File.separator.equals("\\"))
        {
            if(filename.toLowerCase().startsWith(pathRelativeTo.toLowerCase()))
            {
                showname=filename.substring(pathRelativeTo.length(),filename.length());
            }
        // Unix
        } else
        {
            if(filename.startsWith(pathRelativeTo))
            {
                showname=filename.substring(pathRelativeTo.length(),filename.length());
            }
        }
        return showname;
    }
    
    // The GSM 7bit alphabet character table, position in the array equals the byte value of the character
    private static char[] gsmCharacters=
                             {'@','£','$','¥','è','é','ù','ì' ,'ò','Ç',0x0A,'Ø','ø',0x0D,'Å','å'
                             ,'°','_','°','°','°','°','°','°' ,'°','°','°' ,' ','Æ','æ' ,'?','É'
                             ,' ','!','"','#','¤','%','&','\'','(',')','*' ,'+',',','-' ,'.','/'
                             ,'0','1','2','3','4','5','6','7' ,'8','9',':' ,';','<','=' ,'>','?'
                             ,'¡','A','B','C','D','E','F','G' ,'H','I','J' ,'K','L','M' ,'N','O'
                             ,'P','Q','R','S','T','U','V','W' ,'X','Y','Z' ,'Ä','Ö','Ñ' ,'?','§'
                             ,'¿','a','b','c','d','e','f','g' ,'h','i','j' ,'k','l','m' ,'n','o'
                             ,'p','q','r','s','t','u','v','w' ,'x','y','z' ,'ä','ö','ñ' ,'?','à'
                             };
    // The GSM 7bit alphabet extension character table, position in the array equals the byte value of the character, 
    // fallback for unknown positions is the value of the standard table
    private static char[] gsmExtension= 
                             {'@','£','$','¥','è','é','ù','ì' ,'ò','Ç',0x0C,'Ø','ø',0x0D,'Å','å'
                             ,'°','_','°','°','^','°','°','°' ,'°','°','°' ,' ','Æ','æ' ,'?','É'
                             ,' ','!','"','#','¤','%','&','\'','{','}','*' ,'+',',','-' ,'.','\\'
                             ,'0','1','2','3','4','5','6','7' ,'8','9',':' ,';','[','~' ,']','?'
                             ,'|','A','B','C','D','E','F','G' ,'H','I','J' ,'K','L','M' ,'N','O'
                             ,'P','Q','R','S','T','U','V','W' ,'X','Y','Z' ,'Ä','Ö','Ñ' ,'?','§'
                             ,'¿','a','b','c','d','€','f','g' ,'h','i','j' ,'k','l','m' ,'n','o'
                             ,'p','q','r','s','t','u','v','w' ,'x','y','z' ,'ä','ö','ñ' ,'?','à'
                             };
    /**
     * Converts an array of bytes into a string with the characters taken from the GSM 7bit Aplhabet according to the byte values.
     * See 3GPP TS 23.038 V7.0.0 (2006-03).
     */
    public static String getStringFromGSM7bitAlphabet(byte[] data)
    {
        return getStringFromGSM7bitAlphabet(data,0,data.length);
    }
    /**
     * Converts an array of bytes into a string with the characters taken from the GSM 7bit Aplhabet according to the byte values.
     * See 3GPP TS 23.038 V7.0.0 (2006-03).
     */
    public static String getStringFromGSM7bitAlphabet(byte[] data, int off, int len)
    {
        StringBuffer retval=new StringBuffer();
        for(int i=off;i<off+len;i++)
        {
            if(i>off&&data[i-1]==27)
            {
                retval.append(gsmExtension[data[i]]);
            } else
            {
                retval.append(gsmCharacters[data[i]]);
            }
        }
        return retval.toString();
    }

    /**
     * splits the byte array around matches of the given delimiter.<br>
     * <br>
     * Examples:<br>
     * input : <br>
     *  src = {0,1,2,'a','b','c',3,4,'a','b','c',5,6,7,8,9} <br>
     *  delimiter = {'a','b','c'} <br>
     * output : {{0,1,2}, {3,4}, {5,6,7,8,9}} <br>
     * @return array of splited byte arrays
     */
     public static byte[][] splitByteArray(byte[] src, byte[] delimiter)
    {
        ArrayList<byte[]> tempArrayList = new ArrayList<byte[]>();
        int delimiterLen = delimiter.length;
        int start = 0;
        int len = 0;
        if(delimiterLen==0 || delimiterLen>=src.length)
        {
            return new byte[][]{src};
        }
        for(int i=0; i<src.length-delimiterLen+1; i++)
        {
            if(compareBytes(src,i,delimiter,0,delimiterLen))
            {
                tempArrayList.add(subByteArray(src,start,len));
                start = start + len + delimiterLen;
                i = i + delimiterLen - 1;
                len = -1;
            }
            len++;
        }
        tempArrayList.add(subByteArray(src,start));
        return tempArrayList.toArray(new byte[0][]);
    }

    /**
     * returns a byte array with the content of the given array starting from the specified start index.<br>
     * <br>
     * Examples:<br>
     * input : <br>
     *  bytes = {0,1,2,3,4,5,6} <br>
     *  start = 3 <br>
     * output : {3,4,5,6} <br>
     * @return a part of the byte array starting from specified start index
     */
    public static byte[] subByteArray(byte[] bytes, int start)
    {
        return subByteArray(bytes,start,bytes.length);
    }

    /**
     * returns a byte array with the content of the given array starting from
     * the specified start index and of specified length.<br>
     * <br>
     * Examples:<br>
     * input : <br>
     *  bytes = {0,1,2,3,4,5,6} <br>
     *  start = 3 <br>
     *  len   = 2 <br>
     * output : {3,4} <br>
     * @return a part of the byte array starting from specified start index and of specified length
     */
    public static byte[] subByteArray(byte[] bytes, int start, int len)
    {
        int arrayLen = len;
        if(arrayLen > bytes.length-start)
        {
            arrayLen = bytes.length-start;
        }
        byte[] subArray = new byte[arrayLen];
        for (int i=start,j=0; i<bytes.length && i<(start+len); i++,j++)
        {
            subArray[j] = bytes[i];
        }
        return subArray;
    }

    /**
     * Return a byte array concatenated from two or more byte arrays <br>
     * <br>
     * Examples:<br>
     * input : <br>
     *  bytes1 = {0,1,2} <br>
     *  bytes2 = {3,4,5} <br>
     * output : {0,1,2,3,4,5} <br>
     * @return the concatenated byte array
     */
    public static byte[] concatByteArray(byte[] byteArray1, byte[]... byteArray2)
    {
        
        int len=byteArray1.length;
        for(byte[] array : byteArray2) 
        {
            len=len+array.length;
        }
        byte[] ret=new byte[len];
        int pointer=addBytesToArray(ret,0,byteArray1);
        for(byte[] array : byteArray2) 
        {
            pointer=addBytesToArray(ret,pointer,array);
        }
        return ret;
    }
    /**
     * returns a hex string of the given byte array starting from the specified start index and of specified datalength..<br>
     * Examples:<br>
     * input : <br>
     *  bytes = {10,11,12,13,14,15,16} <br>
     *  start = 3 <br>
     *  datalength = 2 <br>
     * output : 0d0e <br>
     * @return a part of the byte array starting from specified start index
     */
    public static String getHexStringFromByteArray(byte[] bs, int start, int datalength)
    {
        byte[] rawBytes =new byte[datalength];
        for (int i =0; i<datalength; i++)
        {
            rawBytes[i] = bs[start+i] ;
        }
        return getHexStringFromByteArray(rawBytes);
    }
}