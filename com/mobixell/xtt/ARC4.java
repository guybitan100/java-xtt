package com.mobixell.xtt;

public class ARC4
{
    private int thisX = 0;
    private int thisY = 0;
    private byte[] state = new byte[256];

    private int arcfour_byte()
    {
        int x;
        int y;
        int sx, sy;

        x = (thisX + 1) & 0xff;
        sx = (int)state[x];
        y = (sx + thisY) & 0xff;
        sy = (int)state[y];
        thisX = x;
        thisY = y;
        state[y] = (byte)(sx & 0xff);
        state[x] = (byte)(sy & 0xff);
        return (int)state[((sx + sy) & 0xff)];
    }

    public byte[] encrypt(byte[] src)
    {
        byte[] result = new byte[src.length];
        encrypt(src,0,result,0,src.length);
        return result;
    }

    public byte[] decrypt(byte[] src)
    {
        byte[] result = new byte[src.length];
        decrypt(src,0,result,0,src.length);
        return result;
    }

    public void encrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len)
    {
        int end = srcOff + len;
        for(int si = srcOff, di = destOff; si < end; si++, di++)
        {
          dest[di] = (byte)(((int)src[si] ^ arcfour_byte()) & 0xff);
        }
    }

    public void decrypt(byte[] src, int srcOff, byte[] dest, int destOff, int len)
    {
        encrypt(src, srcOff, dest, destOff, len);
    }

    public void setKey(byte[] key)
    {
        //XTTProperties.printInfo("Setting Key to: \n" + ConvertLib.getHexView(key));

        int t=0;
        int u=0;
        int keyindex=0;
        int stateindex=0;
        int counter=0;

        for(counter = 0; counter < 256; counter++)
        {
          state[counter] = (byte)counter;
        }

        for(counter = 0; counter < 256; counter++)
        {
            t = (int)state[counter];
            stateindex = (stateindex + key[keyindex] + t) & 0xff;
            u = (int)state[stateindex];
            state[stateindex] = (byte)(t & 0xff);
            state[counter] = (byte)(u & 0xff);
            if(++keyindex >= key.length)
    	    keyindex = 0;
        }

        //XTTProperties.printInfo("State: \n" + ConvertLib.getHexView(state));

        thisX = 0;
        thisY = 0;
    }


/*    private static int[] cipherBox = new int [256];
    private static int[] cipherKeyArray = new int [256];


    public static byte[] doCipher(byte[] cipherKey, byte[] unencodedText)
    {
      	// doCipher will encrypt unencrypted data or decrypt encrypted data
       	int z = 0;
    	int t = 0;
    	int i = 0;
    	int cipherBy = 0;
        int tempInt=0;
        byte[] cipher = new byte[unencodedText.length];
        char cipherText;

       	//Initialize cipherBox and cipherKeyArray
        doRC4MatrixSeed(cipherKey);


    	for(int a = 0; a < unencodedText.length; a++)
		{
            i = (i + 1) % 255;
            t = (t + cipherBox[i]) % 255;
            tempInt = cipherBox[i];
            cipherBox[i] = cipherBox[t];
            cipherBox[t]= tempInt;

            z = cipherBox[(cipherBox[i] + cipherBox[t]) % 255];

            //convert to ascii value XOR'd by z
            cipherBy = unencodedText[a] ^ z;
            //System.out.println("CipherBy=" + cipherBy);

             cipher[a] = (byte)cipherBy;
            // System.out.println("CIPHER=" + cipher.toString());
   		}
        return cipher;
    }


    private static void doRC4MatrixSeed(byte[] thisKey)
    {
        //Initialize cipherBox and cipherKey Array's

        int keyLength =0;
        int dataSwap;
        int b;

        keyLength = thisKey.length;

        for(int a = 0; a < 255; a++)
        {
            //take the key character at the selected position
            cipherKeyArray[a] = thisKey[a % keyLength];
            cipherBox[a]=a;
        }

        b = 0;

        for(int a = 0; a < 255; a++)
        {
            b = (b + cipherBox[a] + cipherKeyArray[a]) % 255;
            dataSwap = cipherBox[a];
            cipherBox[a] = cipherBox[b];
            cipherBox[b] = dataSwap;
        }
    }

public static String doCipher(String cipherKey, String unencodedText)
{

  	// doCipher will encrypt unencrypted data or decrypt encrypted data
     	int z = 0;
	int t = 0;
	int i = 0;
	int cipherBy = 0;
        int tempInt=0;
        String cipher = "";
        char cipherText;

   	//Initialize cipherBox and cipherKeyArray
    	doRC4MatrixSeed(cipherKey);


	for(int a = 0; a < unencodedText.length(); a++)
		{

			i = (i + 1) % 255;
			t = (t + cipherBox[i]) % 255;
			tempInt = cipherBox[i];
			cipherBox[i] = cipherBox[t];
			cipherBox[t]= tempInt;

			z = cipherBox[(cipherBox[i] + cipherBox[t]) % 255];

			//get character at position a
			cipherText = unencodedText.charAt(a);

			//convert to ascii value XOR'd by z
			cipherBy = (int) cipherText ^z;
			//System.out.println("CipherBy=" + cipherBy);

			 cipher = cipher  + (char) cipherBy;
			// System.out.println("CIPHER=" + cipher.toString());

   		}
      return cipher;

}


  private static void doRC4MatrixSeed(String thisKey)
{


      //Initialize cipherBox and cipherKey Array's

      int keyLength =0;
      int dataSwap;
      int b;
      int asciiVal=0;
      char asciiConvert;
      char asciiChar ;
      keyLength = thisKey.length();

      for(int a = 0; a < 255; a++)
	{
	        //take the key character at the selected position
		asciiChar = thisKey.charAt(a % keyLength);
		asciiVal = (int) asciiChar;
		cipherKeyArray[a] = asciiVal;
        	cipherBox[a]=a;


	}

      b = 0;

      for(int a = 0; a < 255; a++)
	{

		 b = (b + cipherBox[a] + cipherKeyArray[a]) % 255;
		 dataSwap = cipherBox[a];
		 cipherBox[a] = cipherBox[b];
		 cipherBox[b] = dataSwap;
	}


}
*/



}