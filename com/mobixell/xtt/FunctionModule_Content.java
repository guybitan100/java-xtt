package com.mobixell.xtt;

import java.io.IOException;

/**
 * FunctionModule_Content.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.14 $
 */
public class FunctionModule_Content extends FunctionModule
{
    public void isGif(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": isGif: base64encodedGif");
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedGif");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            String gifString = parameters[1];

            ImageData myPic = checkImage(ConvertLib.base64Decode(gifString));

            if ((myPic != null)&&(myPic.getFormat().equals(ImageData.GIF)))
            {
                XTTProperties.printInfo(parameters[0] + ": is " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight());
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": the image is a " + myFormat);
            }
        }
    }


    public void isJpg(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": isJpg: base64encodedJpeg");
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedJpeg");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            String jpegString = parameters[1];

            ImageData myPic = checkImage(ConvertLib.base64Decode(jpegString));

            if ((myPic != null)&&(myPic.getFormat().equals(ImageData.JPG)))
            {
                XTTProperties.printInfo(parameters[0] + ": is " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight() + " isProgressive: " + myPic.getIsProgressive());
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": the image is a " + myFormat);
            }
        }
    }

    public void isBmp(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": isBmp: base64encodedBMP");
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedBMP");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            String bmpString = parameters[1];

            ImageData myPic = checkImage(ConvertLib.base64Decode(bmpString));

            if ((myPic != null)&&(myPic.getFormat().equals(ImageData.BMP)))
            {
                XTTProperties.printInfo(parameters[0] + ": is " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight() + " " + myPic.getNumberOfColours() + " colours");
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": the image is a " + myFormat);
            }
        }
    }

    public void isPng(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": isPng: base64encodedPNG");
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedPNG");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            String pngString = parameters[1];

            ImageData myPic = checkImage(ConvertLib.base64Decode(pngString));

            if ((myPic != null)&&(myPic.getFormat().equals(ImageData.PNG)))
            {
                XTTProperties.printInfo(parameters[0] + ": is " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight() + " isProgressive: " + myPic.getIsProgressive());
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": the image is a " + myFormat);
            }
        }
    }

    public void isWbmp(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": isWbmp: base64encodedWBMP");
        }
        else if (parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedWBMP");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            String wbmpString = parameters[1];

            ImageData myPic = checkImage(ConvertLib.base64Decode(wbmpString));

            if ((myPic != null)&&(myPic.getFormat().equals(ImageData.WBMP)))
            {
                XTTProperties.printInfo(parameters[0] + ": is " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight());
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": the image is a " + myFormat);
            }
        }
    }
    
    public void checkNumberOfColours(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkNumberOfColours: base64encodedImage numberOfColours");
        }
        else if (parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64encodedImage numberOfColours");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            //Don't choose -1, since -1 means not set in the ImageData
            int numberOfColours = -2;
            try
            {
                numberOfColours = Integer.parseInt(parameters[2]);
            }
            catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '" + parameters[2] + "' isn't a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(nfe);
                }
            }
            
            byte[] imageData = ConvertLib.base64Decode(parameters[1]);

            ImageData myPic = checkImage(imageData);

            if ((myPic != null) && (myPic.getNumberOfColours() == numberOfColours) && (numberOfColours != -1))
            {
                XTTProperties.printInfo(parameters[0] + ": " + myPic.getFormat() + " has " + numberOfColours + " colours");
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }
                String myColours = "UNKOWN";
                if ((myPic != null)&& (myPic.getNumberOfColours() > 0))
                {
                    myColours = "" + myPic.getNumberOfColours();
                }
                XTTProperties.printFail(parameters[0] + ": " + myFormat + " has " + myColours + " not " + numberOfColours + " colours");
            }            
                    
        }
    }

    public void checkIsProgressive(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkIsProgressive: base64EncodedImage");
        }
        else if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64EncodedImage");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            byte[] pngData = ConvertLib.base64Decode(parameters[1]);

            ImageData myPic = checkImage(pngData);

            if ( (myPic != null)&&( myPic.getFormat().equals(ImageData.JPG)||myPic.getFormat().equals(ImageData.PNG) ) && myPic.getIsProgressive())
            {
                XTTProperties.printInfo(parameters[0] + ": " + myPic.getFormat() + " is Progressive");
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": " + myFormat + " isn't progressive");
            }
        }
    }

    public void checkIsNotProgressive(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkIsNotProgressive: base64EncodedImage");
        }
        else if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64EncodedImage");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            byte[] pngData = ConvertLib.base64Decode(parameters[1]);

            ImageData myPic = checkImage(pngData);

            if ( (myPic != null)&&( myPic.getFormat().equals(ImageData.JPG)||myPic.getFormat().equals(ImageData.PNG) ) && !myPic.getIsProgressive())
            {
                XTTProperties.printInfo(parameters[0] + ": " + myPic.getFormat() + " is NOT Progressive");
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);

                String myFormat = "UNKNOWN";
                if ((myPic != null)&& (myPic.getFormat() != null))
                {
                    myFormat = myPic.getFormat();
                }

                XTTProperties.printFail(parameters[0] + ": " + myFormat + " is progressive");
            }
        }
    }

    public void imageDimensionsToVariable(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": imageDimensionsToVariable: base64EncodedImage variableName");
        }
        else if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": base64EncodedImage variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        else
        {
            ImageData myPic = checkImage(ConvertLib.base64Decode(parameters[1]));

            if (myPic != null)
            {
                XTTProperties.printInfo(parameters[0] + ": " + myPic.getFormat() + " " + myPic.getWidth() + "x" + myPic.getHeight() + " saved to " + parameters[2] + "/x and " + parameters[2] + "/y");
                XTTProperties.setVariable(parameters[2], myPic.getWidth() + "x" + myPic.getHeight());
                XTTProperties.setVariable(parameters[2] + "/x", ""+myPic.getWidth());
                XTTProperties.setVariable(parameters[2] + "/y", ""+myPic.getHeight());
            }
            else
            {
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printFail(parameters[0] + ": Cannot get dimesions, data is probably not an image");
            }
        }
    }

    private ImageData checkImage(byte[] data)
    {
		try {
			int b1 = data[0] & 0xff;
			int b2 = data[1] & 0xff;
			if (b1 == 0x47 && b2 == 0x49) {
				return checkGif(data);
			}
			else
			if (b1 == 0x89 && b2 == 0x50) {
				return checkPng(data);
			}
			else
			if (b1 == 0xff && b2 == 0xd8) {
				return checkJpeg(data);
			}
			else
			if (b1 == 0x42 && b2 == 0x4d) {
				return checkBmp(data);
			}
			else
			if (b1 == 0x00 && b2 == 0x00) {
				return checkWbmp(data);
			}
			else {
				return null;
			}
		}
		catch (IOException ioe)
		{
		    XTTProperties.setTestStatus(XTTProperties.FAILED);
		    if(XTTProperties.printDebug(null))
		    {
		        XTTProperties.printException(ioe);
		    }
		}
		return null;
    }

    private ImageData checkGif(byte[] data) throws IOException
    {
		final byte[] GIF_MAGIC_87A = {0x47, 0x49, 0x46, 0x38, 0x37, 0x61};
		final byte[] GIF_MAGIC_89A = {0x47, 0x49, 0x46, 0x38, 0x39, 0x61};

		if ((!ConvertLib.compareBytes(data,GIF_MAGIC_89A,6)) && (!ConvertLib.compareBytes(data,GIF_MAGIC_87A,6)))
		{
			return null;
		}
		else
		{
		    ImageData theImage = new ImageData(ImageData.GIF,ConvertLib.getIntFromLittleEndianByteArray(data, 6, 2), ConvertLib.getIntFromLittleEndianByteArray(data, 8, 2));
		    return theImage;
		}
    }

	private ImageData checkPng(byte[] data) throws IOException
	{
		final byte[] PNG_MAGIC = {(byte)0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};

		if (!ConvertLib.compareBytes(data,PNG_MAGIC,8))
		{
			return null;
		}
		else
		{
		    ImageData theImage = new ImageData(ImageData.PNG, ConvertLib.getIntFromByteArray(data, 16,4), ConvertLib.getIntFromByteArray(data, 20,4));
		    if(data[28] != 0)
		    {
		        theImage.setIsProgressive(true);
		    }
		    else
		    {
		        theImage.setIsProgressive(false);
		    }
		    return theImage;
		}
	}

	private ImageData checkJpeg(byte[] data) throws IOException
	{
	    int pos = 2;
		while (true)
		{
			int marker = ConvertLib.getIntFromByteArray(data, 0+pos,2);
			int size = ConvertLib.getIntFromByteArray(data, 2+pos,2);

			if ((marker & 0xff00) != 0xff00)
			{
				return null; // not a valid marker
			}
			else if (marker == 0xffe0) { // APPx
				if (size < 14) {
					return null; // APPx header must be >= 14 bytes
				}
				pos += size + 2;
			}
			else if (marker >= 0xffc0 && marker <= 0xffcf && marker != 0xffc4 && marker != 0xffc8)
			{
			    ImageData theImage = new ImageData (ImageData.JPG, ConvertLib.getIntFromByteArray(data, 7+pos,2), ConvertLib.getIntFromByteArray(data, 5+pos,2));
			    theImage.setIsProgressive(marker == 0xffc2 || marker == 0xffc6 || marker == 0xffca || marker == 0xffce);
				return theImage;
			}
			else
			{
				pos += size + 2;
			}
		}
	}

	private ImageData checkBmp(byte[] data) throws IOException
	{
		int width = ConvertLib.getIntFromLittleEndianByteArray(data, 18, 4);
		int height = ConvertLib.getIntFromLittleEndianByteArray(data, 22, 4);
		if (width < 1 || height < 1) {
			return null;
		}
		ImageData theImage = new ImageData(ImageData.BMP,width,height);
		theImage.setNumberOfColours(ConvertLib.getIntFromLittleEndianByteArray(data, 28, 2));
		return theImage;
	}

	private ImageData checkWbmp(byte[] data) throws IOException
	{
	    System.out.println(ConvertLib.getHexView(data));
		int width = ConvertLib.getIntFromByteArray(data, 2, 1);
		int height = ConvertLib.getIntFromByteArray(data, 3, 1);
		if (width < 1 || height < 1) {
			return null;
		}
		ImageData theImage = new ImageData(ImageData.WBMP,width,height);
		return theImage;
	}

    public void initialize(){}

    public String toString()
    {
        return this.getClass().getName();
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Content.java,v 1.14 2008/02/07 15:39:14 gcattell Exp $";

    private class ImageData
    {
        public static final String GIF="Gif";
        public static final String BMP="Bmp";
        public static final String WBMP="Wbmp";
        //public static final String Iff="Iff";
        public static final String JPG="Jpg";
        //public static final String PCX="Pcx";
        public static final String PNG="Png";
        //public static final String PSD="Psd";
        //public static final String RAS="Ras";

        private String format = null;
        private int width = -1;
        private int height = -1;
        private boolean isProgressive = false;
        private int colours = -1;

        public ImageData(String format, int width, int height)
        {
            this.format = format;
            this.width = width;
            this.height = height;
            this.isProgressive = false;
            this.colours = -1;
        }

        public void setIsProgressive(boolean isProgressive)
        {
            this.isProgressive = isProgressive;
        }
        public void setNumberOfColours(int colours)
        {
            this.colours = colours;
        }

        public String getFormat()
        {
            return format;
        }
        public int getWidth()
        {
            return width;
        }
        public int getHeight()
        {
            return height;
        }
        public boolean getIsProgressive()
        {
            return isProgressive;
        }
        public int getNumberOfColours()
        {
            return colours;
        }

    }
}