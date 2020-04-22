package com.mobixell.xtt;

import java.util.Random;

public class RandomLib
{
    private static Random random=new Random();

    /**
     * Returns the <code>Random</code> object.
     *
     */
    public static Random getRandom()
    {
        return random;
    }

    public static long getRandomLong()
    {
        synchronized(random)
        {
            return random.nextLong();
        }
    }

    public static int getRandomInt()
    {
        synchronized(random)
        {
            return random.nextInt();
        }
    }

    public static int getRandomInt(int n)
    {
        synchronized(random)
        {
            return random.nextInt(n);
        }
    }
    
    public static String getRandomDigits(int length)
    {
        length = getRandomSize(length);
        
        StringBuffer digits = new StringBuffer();
        for(int i=0;i<length;i++)
        {
            digits.append(getRandomInt(10));
        }
        return digits.toString();
    }
    private static String ALPHANUM="0123456789ABCDEFGHIJKLMNOPQRSTUVW";
    public static String getRandomAplhaNumeric()
    {
        synchronized(random)
        {
            int pos=random.nextInt(33);
            return ALPHANUM.substring(pos,pos+1);
        }
    }
    public static String getRandomAplhaNumeric(int length)
    {
        length = getRandomSize(length);
        
        StringBuffer digits = new StringBuffer();
        for(int i=0;i<length;i++)
        {
            digits.append(getRandomAplhaNumeric());
        }
        return digits.toString();
    }
    private static byte[] ALPHANUMBYTES={'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W'};
    public static byte getRandomAplhaNumericByte()
    {
        synchronized(random)
        {
            int pos=random.nextInt(33);
            return ALPHANUMBYTES[pos];
        }
    }
    public static byte[] getRandomAplhaNumericBytes(int length)
    {
        length = getRandomSize(length);
        
        byte[] buffer=new byte[length];
        for(int i=0;i<length;i++)
        {
            buffer[i]=getRandomAplhaNumericByte();
        }
        return buffer;
    }

    /*
    * length defines the length of the randon bytes. if the number is negative the length is ranom up to the specified length.
    */
    public static byte[] getRandomBytes(int length)
    {
        length = getRandomSize(length);

        byte[] retVal=new byte[length];
        synchronized(random)
        {
            random.nextBytes(retVal);
        }
        return retVal;
    }

    /*
    * length defines the length of the size. if the number is negative the length is random up to the specified length, if it is positive it is exactly length.
    */
    public static int getRandomSize(int length)
    {
        if(length==-1)
        {
            return 0;
        }
        if(length<0)
        {
            int retlength=0;
            while(retlength==0)
            {
                synchronized(random)
                {
                    retlength=random.nextInt(Math.abs(length));
                }
            }
            return retlength;
        } else
        {
            return length;
        }
    }

    /*
    * length defines the length of the random bytes the hash is calculated on. Returns 16 bytes
    */
    public static byte[] getRandomMD5Hash(int length)
    {
        try
        {
            java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");
            return md5Hash.digest(getRandomBytes(length));
        } catch (java.security.NoSuchAlgorithmException ex)
        {
            XTTProperties.printFail("RandomLib.getRandomMD5Hash(): NoSuchAlgorithmException - check your java vm!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ex);
            }
            return new byte[16];
        }
    }
}