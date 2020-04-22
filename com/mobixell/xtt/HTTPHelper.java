package com.mobixell.xtt;

import java.util.ArrayList;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Iterator;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;


/**
 * HTTPHelper provides HTTP and HTTPS helper functions.
 *
 * @author      Roger Soder
 * @version     $Id: HTTPHelper.java,v 1.47 2010/02/08 08:40:30 lstrzepka Exp $
 */
public class HTTPHelper
{
    private static final byte[] CRLF = {0x0D,0x0A};
    public static final String tantau_sccsid = "@(#)$Id: HTTPHelper.java,v 1.47 2010/02/08 08:40:30 lstrzepka Exp $";
    public static String createHTTPDate(GregorianCalendar calendar)
    {
        SimpleDateFormat format=new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy HH':'mm':'ss z",Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(calendar.getTime());
    }
    public static String createHTTPDate()
    {
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
        return createHTTPDate(calendar);
    }

    public static String createISODate()
    {
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
        return createISODate(calendar);
    }
    public static String createISODate(GregorianCalendar calendar)
    {
        SimpleDateFormat format=new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'",Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(calendar.getTime());
    }

    /** client side code */
    public static byte[] readStream(String funcname, BufferedInputStream in) throws java.io.IOException
    {
        byte buffer[]=new byte[1024];
        ArrayList<ByteArrayWrapper> bufferSet=new ArrayList<ByteArrayWrapper>();
        int thebyte=0;
        int byteCounter=-1;
        while(thebyte!=-1)
        {
            //System.out.println(in.available());
            byteCounter++;
            if((byteCounter%1024)==0)
            {
                if(funcname!=null)XTTProperties.printDebug(funcname+": Reading 1024 Bytes:");
                buffer=new byte[1024];
                bufferSet.add(new ByteArrayWrapper(buffer));
            }
            thebyte=in.read();
            if(thebyte==-1){byteCounter--;break;}
            buffer[byteCounter%1024]=(new Integer(thebyte)).byteValue();
        }
        byte[] buf=new byte[byteCounter+1];
        Object bytes[]=bufferSet.toArray();
        int end=1024;
        int count=0;
        for(int i=0;i<bytes.length;i++)
        {
            if(i==(bytes.length-1))end=byteCounter%1024+1;
            for(int j=0;j<end;j++)
            {
                buf[count++]=((ByteArrayWrapper)bytes[i]).getArray()[j];
            }
        }
        return buf;
    }

    /** server side code */
    public static byte[] readStream(String funcname, BufferedInputStream in, int networklagdelay) throws java.io.IOException
    {
        if(networklagdelay<=0)networklagdelay=0;
        byte buffer[]=new byte[1024];
        ArrayList<ByteArrayWrapper> bufferSet=new ArrayList<ByteArrayWrapper>();
        int thebyte=0;
        int byteCounter=-1;
        while(thebyte!=-1)
        {
            byteCounter++;
            if((byteCounter%1024)==0)
            {
                if(funcname!=null)XTTProperties.printDebug(funcname+": Reading 1024 Bytes:");
                buffer=new byte[1024];
                bufferSet.add(new ByteArrayWrapper(buffer));
            }
            thebyte=in.read();
            if(thebyte!=-1)
            {
                buffer[byteCounter%1024]=(new Integer(thebyte)).byteValue();
            } else
            {
                byteCounter--;
            }
            if(in.available()==0)
            {
                try
                {
                    if(funcname!=null)XTTProperties.printDebug(funcname+": no more data available");
                    Thread.sleep(networklagdelay);
                } catch (Exception irex){}
                if(in.available()==0)thebyte=-1;
            }
        }
        byte[] buf=new byte[byteCounter+1];
        Object bytes[]=bufferSet.toArray();
        int end=1024;
        int count=0;
        for(int i=0;i<bytes.length;i++)
        {
            if(i==(bytes.length-1))end=byteCounter%1024+1;
            for(int j=0;j<end;j++)
            {
                buf[count++]=((ByteArrayWrapper)bytes[i]).getArray()[j];
            }
        }
        return buf;
    }

    public static HTTPConnection getConnection(Map<String,HTTPConnection> connections,HTTPConnection defaultConnection,String parameters[])
    {
        if(parameters.length==1)return defaultConnection;
        HTTPConnection connection=connections.get(parameters[1].toLowerCase());
        if(connection==null)
        {
            return defaultConnection;
        } else
        {
            parameters[0]=parameters[0]+"("+connection.getName()+")";
            return connection;
        }
    }
    public static void checkHeader(Map<String,HTTPConnection> connections,HTTPConnection defaultConnection,String parameters[],boolean inverse)
    {
        if(parameters.length<2||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey expectedValue");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            HTTPConnection connection=connections.get(parameters[1].toLowerCase());
            if(connection==null)
            {
                checkHeader(defaultConnection.getResponseHeader(),parameters,inverse);
            } else
            {
                String[] newparameters=new String[parameters.length-1];
                newparameters[0]=parameters[0]+"("+connection.getName()+")";
                for(int i=1;i<newparameters.length;i++)
                {
                    newparameters[i]=parameters[i+1];
                }
                checkHeader(connection.getResponseHeader(),newparameters,inverse);
            }
        }

    }
    public static void checkHeader(LinkedHashMap<String,Vector<String>> headerMap,String parameters[],boolean inverse)
    {
        Vector<String> valueList=new Vector<String>();
        if(parameters.length==2)
        {
            String headerKey=parameters[1];
            boolean hasKey=headerMap.keySet().contains(headerKey.toLowerCase());
            if(!inverse)
            {
                if(hasKey)
                {
                    XTTProperties.printInfo(parameters[0]+": header "+headerKey+": header key found: "+headerMap.get(headerKey.toLowerCase())+"");
                } else
                {
                    XTTProperties.printFail(parameters[0]+": header "+headerKey+": header key not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            } else
            {
                if(hasKey)
                {
                    XTTProperties.printFail(parameters[0]+": header "+headerKey+": header key found: "+headerMap.get(headerKey.toLowerCase())+"");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": header "+headerKey+": header key not found");
                }
            }
            return;
        } else if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        valueList.add(parameters[2]);
        checkHeader(parameters[0],headerMap,parameters[1],valueList,inverse);
    }

    private static void checkHeader(String functionName,LinkedHashMap<String,Vector<String>> headerMap,String headerKey,Vector<String> headerValues, boolean inverse)
    {
        String headkey=headerKey.toLowerCase();
        if(headkey.equals("null"))headkey=null;
        //XTTProperties.printDebug("HEADER:"+headerMap+"\n"+headerMap.get(headkey));
        if(headerMap.get(headkey)!=null
            && headerMap.get(headkey).containsAll(headerValues))
        {
            if(!inverse)
            {
                XTTProperties.printInfo(functionName+": header "+headerKey+": "+headerMap.get(headkey)+" required "+ headerValues);
            } else
            {
                XTTProperties.printFail(functionName+": header "+headerKey+": "+headerMap.get(headkey)+" prohibited "+ headerValues);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            if(headerMap.get(headkey)==null)
            {
                if(headerValues.contains("null"))
                {
                    if(!inverse)
                    {
                        XTTProperties.printInfo(functionName+": header "+headerKey+": header key not found required "+headerValues+" (header key not found)");
                        return;
                    } else
                    {
                        XTTProperties.printFail(functionName+": header "+headerKey+": header key not found prohibited "+headerValues+" (header key not found)");
                    }
                } else
                {
                    if(!inverse)
                    {
                        XTTProperties.printFail(functionName+": header "+headerKey+": header key not found required "+headerValues+"");
                    } else
                    {
                        XTTProperties.printInfo(functionName+": header "+headerKey+": header key not found prohibited "+headerValues+"");
                        return;
                    }
                }
            } else
            {
                String more="";
                if(headerValues.contains("null"))more=" (header key not found)";
                if(!inverse)
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": "+headerMap.get(headkey)+" required "+ headerValues+""+more);
                } else
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": "+headerMap.get(headkey)+" prohibited "+ headerValues+""+more);
                    return;
                }
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void checkHeaderPart(LinkedHashMap<String,Vector<String>> headerMap,String parameters[],boolean inverse)
    {
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey expectedValuePart");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        String functionName=parameters[0];
        String headerKey=parameters[1];
        String headkey=parameters[1].toLowerCase();
        String headerValue=parameters[2];
        if(headkey.equals("null"))headkey=null;
        if(headerMap.get(headkey)==null)
        {
            XTTProperties.printFail(functionName+": header "+headerKey+": header key not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        } else
        {
            Vector<String> l=headerMap.get(headkey);
            String value=l.get(l.size()-1);
            if(value.indexOf(headerValue)!=-1)
            {
                if(inverse)
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": ["+value+"] contains ["+ headerValue+"]");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                } else
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": ["+value+"] contains ["+ headerValue+"]");
                }
                return;
            } else
            {
                if(inverse)
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": ["+value+"] doesn't contain ["+ headerValue+"]");
                } else
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": ["+value+"] doesn't contain ["+ headerValue+"]");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
                return;
            }
        }
    }

    public static void setHeader(LinkedHashMap<String,String> header,String parameters[])
    {
        LinkedHashMap<String,String> headerMap=header;
        if(parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(parameters.length==3&&!parameters[2].equals("null"))
            {
                XTTProperties.printInfo(parameters[0]+": setting HeaderField "+parameters[1]+" to: "+parameters[2]);
                // Actually set the Header Key and Value
                headerMap.put(parameters[1].trim(),parameters[2]);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": removing HeaderField "+parameters[1]);
                // Actually remove the Header Key and Value
                headerMap.remove(parameters[1].trim());
            }
        }
    }
    public static void setHeader(Map<String,HTTPConnection> connections,HTTPConnection defaultConnection,String parameters[])
    {
        if(parameters.length<2||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": headerFieldKey headerFieldValue");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+FunctionModule.MISSING_ARGUMENTS+": connection headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            HTTPConnection connection=connections.get(parameters[1].toLowerCase());
            int offset=0;
            if(connection==null)
            {
                connection=defaultConnection;
                if(parameters.length==4)
                {
                    offset=1;
                    XTTProperties.printWarn(parameters[0]+": connection '"+parameters[1]+"' not found, using default");
                }
            } else
            {
                parameters[0]=parameters[0]+"("+connection.getName()+")";
                offset=1;
            }
            if(parameters.length==3+offset&&!parameters[2+offset].equals("null"))
            {
                XTTProperties.printInfo(parameters[0]+": setting HeaderField "+parameters[1+offset]+" to: "+parameters[2+offset]);
                // Actually set the Header Key and Value
                connection.getRequestHeader().put(parameters[1+offset],parameters[2+offset]);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": removing HeaderField "+parameters[1+offset]);
                // Actually remove the Header Key and Value
                connection.getRequestHeader().remove(parameters[1+offset]);
            }
        }
    }


    public static LinkedHashMap<String,Vector<String>> createHeaderFields(LinkedHashMap<String,List<String>> originalFields)
    {
        Iterator<String> it=originalFields.keySet().iterator();
        LinkedHashMap<String,Vector<String>> fields=new LinkedHashMap<String,Vector<String>>();
        //System.out.println("ORIGINAL: "+originalFields);
        String okey=null;
        String nkey=null;
        List<String> oldL=null;
        while(it.hasNext())
        {
            okey=it.next();
            nkey=okey;
            if(nkey!=null)nkey=nkey.toLowerCase();
            oldL=originalFields.get(okey);
            fields.put(nkey,new Vector<String>(oldL));
        }
        //System.out.println("NEW; "+fields);
        return fields;
    }

    public static boolean queryHeader(LinkedHashMap<String,Vector<String>> headerMap,String parameters[],boolean inverse)
    {
        String regex=null;
        String storeVar=null;
        String key=null;
        int off=1;
        if(inverse)
        {
            off=0;
        } else
        {
            storeVar=parameters[off];
        }
        key=parameters[off+1].toLowerCase();
        regex=parameters[off+2];

        if(key.equals("null"))key=null;
        Vector<String> headerV=headerMap.get(key);
        if(headerV==null)
        {
            XTTProperties.printFail(parameters[0]+": header "+key+": header key not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        }
        if(headerV.size()>1)
        {
            XTTProperties.printWarn(parameters[0]+": multiple "+key+": header keys found, using first");
        }
        String header=headerV.get(0);
        if(header==null)
        {
            XTTProperties.printFail(parameters[0]+": header "+key+": header key not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        }

        XTTProperties.printDebug(parameters[0]+": regex: '"+regex+"'");
        if(inverse)
        {
            return ConvertLib.queryStringNegative(parameters[0],header,regex);
        }
        else
        {
            return ConvertLib.queryString(parameters[0],header,regex,storeVar);
        }
    }

    public static LinkedHashMap<String,Vector<String>> readHeaders(BufferedInputStream in,LinkedHashMap<String,Vector<String>> headers,StringBuffer plainHeaderOutput,StringBuffer decodedHeaderOutput ) throws java.io.IOException
    {
        return readHeaders(in,headers,plainHeaderOutput,decodedHeaderOutput,"^$");
    }
    public static LinkedHashMap<String,Vector<String>> readHeaders(BufferedInputStream in,LinkedHashMap<String,Vector<String>> headers,StringBuffer plainHeaderOutput,StringBuffer decodedHeaderOutput, String endHeaderPattern ) throws java.io.IOException
    {
        String input=null;
        Vector<String> valueList=null;
        String currentHeader[]=null;
        String lastHeader=null;
        String newHeader=null;
        do
        {
            input=readCRLFLine(in);
            if(input==null||input.matches(endHeaderPattern))
            {
                if(input!=null)
                {
                    currentHeader=input.split(":",2);
                    lastHeader=currentHeader[0].trim().toLowerCase();
                    valueList=headers.get(lastHeader);
                    if(valueList==null)valueList=new Vector<String>();
                    valueList.add("");
                    decodedHeaderOutput.append("\n  "+lastHeader+" -> "+valueList+" (end of headers pattern='"+endHeaderPattern+"')");
                    headers.put(lastHeader,valueList);
                }
                break;
            }
            plainHeaderOutput.append(input+"\n");
            currentHeader=input.split(":",2);
            if(!input.matches("^\\s.*")&&currentHeader.length==2)
            {
                lastHeader=currentHeader[0].trim().toLowerCase();
                valueList=headers.get(lastHeader);
                if(valueList==null)valueList=new Vector<String>();
                valueList.add(currentHeader[1].trim());
                decodedHeaderOutput.append("\n  "+lastHeader+" -> "+valueList);
                headers.put(lastHeader,valueList);
            } else if(!input.matches("^\\s.*")&&currentHeader.length==1)
            {
                lastHeader=currentHeader[0].trim().toLowerCase();
                valueList=headers.get(lastHeader);
                if(valueList==null)valueList=new Vector<String>();
                valueList.add("");
                decodedHeaderOutput.append("\n  "+lastHeader+" -> "+valueList+" (warning: no colon/not multiline header)");
                headers.put(lastHeader,valueList);
            } else
            {
                valueList=headers.get(lastHeader);
                int vindex=valueList.size()-1;
                newHeader=valueList.elementAt(vindex)+" "+input.trim();
                valueList.remove(vindex);
                valueList.add(newHeader);//,vindex);
                decodedHeaderOutput.append("\n  "+lastHeader+" -> "+valueList+" (replaced last entry)");
            }
        }while(!input.matches(endHeaderPattern));
        return headers;
    }
    public static LinkedHashMap<String,Vector<String>> readHTTPStreamHeaders(String function, BufferedInputStream in) throws java.io.IOException
    {
        StringBuffer plainHeaderOutput=new StringBuffer(function+": Received Header START:\n");
        StringBuffer decodedHeaderOutput=new StringBuffer("");
        try
        {
            if(function!=null)XTTProperties.printDebug(function+": reading Headers");
            String input=null;
            LinkedHashMap<String,Vector<String>> headers=new LinkedHashMap<String,Vector<String>>();
            Vector<String> valueList=null;
            input=readCRLFLine(in);
            if(input==null)
            {
                decodedHeaderOutput=new StringBuffer(" nothing to decode");
                plainHeaderOutput=new StringBuffer(function+": End of stream detected");
                return headers;
            }

            plainHeaderOutput.append(input+"\n");
            valueList=new Vector<String>();
            valueList.add(input);
            headers.put((String)null,valueList);

            readHeaders(in,headers,plainHeaderOutput,decodedHeaderOutput);

            return headers;
        } finally
        {
            if(function!=null)XTTProperties.printDebug(plainHeaderOutput.toString());
            if(function!=null)XTTProperties.printDebug(function+": Decoding:"+decodedHeaderOutput);
        }
    }

    public static String readCRLFLine(BufferedInputStream in) throws java.io.IOException
    {
        return readCRLFLine(in,null);
    }

	public static String readCRLFLine(BufferedInputStream in, String function) throws java.io.IOException
	{
		StringOutputStream line = null;
		int CR = 0x0D;
		int LF = 0x0A;
		int thebyte = 0;
		int lastbyte = 0;
		while (true)
		{
			try
			{
				thebyte = in.read();
			}
			catch (SocketException se)
			{
				break;
			}

			if (thebyte == -1)
			{
				break;
			}
			if (lastbyte == CR && thebyte == LF)
			{
				if (line == null) 
					line = new StringOutputStream();
					break;
			}
			if (thebyte != CR && thebyte != LF && thebyte != -1)
			{
				if (line == null) line = new StringOutputStream();
				line.write(thebyte);
			}
			lastbyte = thebyte;
			if (thebyte == -1)
			{
				try
				{
					if (function != null) XTTProperties.printDebug(function + ":readCRLFLine: no more data available");
					Thread.sleep(100);
				}
				catch (Exception irex)
				{
				}
			}
		}
		if (line == null) return null;
		return line.toString();// ConvertLib.createString(b);
	}

    public static int readBytes(java.io.InputStream in, byte[] buffer) throws java.io.IOException
    {
        return readBytes(in,buffer,0,buffer.length);
    }
    public static int readBytes(java.io.InputStream in, byte[] buffer, int start, int length) throws java.io.IOException
    {
        if(length>buffer.length-start)length=buffer.length-start;
        int pointer=start;
        while(pointer!=-1&&(pointer<buffer.length)&&(pointer<length+start))
        {
            pointer=pointer+in.read(buffer, pointer, length+start-pointer);
        }
        return pointer;
    }
    public static byte[] readChunkedBody(String function, BufferedInputStream in, LinkedHashMap<String,Vector<String>> headers) throws java.io.IOException
    {
        return readChunkedBody(function, in, headers, null);
    }
    public static byte[] readChunkedBody(String function, BufferedInputStream in, LinkedHashMap<String,Vector<String>> headers, Vector<String> chunkOptions) throws java.io.IOException
    {
        boolean finished=false;
        byte[] currentChunks=null;
        int chunkLength=0;
        Vector<ByteArrayWrapper> chunks=new Vector<ByteArrayWrapper>();
        String input=null;
        String[] inputS=null;
        do
        {
            // Read chunk length
            input=readCRLFLine(in);
            try
            {
                inputS=input.split(";",2);
                chunkLength=Integer.decode("0x"+inputS[0].trim());
                if(chunkOptions!=null&&inputS.length==2)
                {
                    chunkOptions.add(inputS[1].trim());
                }
                if(chunkLength==0)
                {
                    if(function!=null)XTTProperties.printDebug(function+": reading chunk: '"+input+"'->end of chunked data");
                    break;
                }
                if(function!=null)XTTProperties.printDebug(function+": reading chunk: '"+input+"'->"+chunkLength+" bytes");
            } catch (NumberFormatException nfe)
            {
                if(function!=null)XTTProperties.printFail(function+": reading chunk: '"+input+"'-> unable to decode");
                throw nfe;
            }
            // create next chunk
            currentChunks=new byte[chunkLength];
            readBytes(in,currentChunks);
            chunks.add(new ByteArrayWrapper(currentChunks));
            // Read empty line before next junk length
            input=readCRLFLine(in);
        } while(!finished);

        StringBuffer plainHeaderOutput=new StringBuffer(function+": received chunked Body Trailers:\n");
        StringBuffer decodedHeaderOutput=new StringBuffer("");
        if(function!=null)XTTProperties.printDebug(function+": reading chunked Body Trailer");
        try
        {
            readHeaders(in,headers,plainHeaderOutput,decodedHeaderOutput);
        } finally
        {
            if(decodedHeaderOutput.length()>0)
            {
                if(function!=null)XTTProperties.printDebug(plainHeaderOutput.toString());
                if(function!=null)XTTProperties.printDebug(function+": Decoding Trailer:"+decodedHeaderOutput);
            }
        }

        int totalLength=0;
        Iterator<ByteArrayWrapper> it=chunks.iterator();
        while(it.hasNext())
        {
            totalLength=totalLength+it.next().getArray().length;
        }
        int pointer=0;
        it=chunks.iterator();
        byte[] unchunkedBody=new byte[totalLength];
        while(it.hasNext())
        {
            currentChunks=it.next().getArray();
            for(int i=0;i<currentChunks.length;i++)
            {
                unchunkedBody[pointer++]=currentChunks[i];
            }
        }
        return unchunkedBody;
    }

    /*
     * This function reads a single chunk from a stream starting with the chunk leanth until and including the CR/LF after the chunk data.
     * Should it be the last chunk it also reads the chunk trailer headers and stores them in the headers map.
     */
    public static byte[] readSingleChunk(String function, BufferedInputStream in, LinkedHashMap<String,Vector<String>> headers, Vector<String> chunkOptions) throws java.io.IOException
    {
        byte[] currentChunks=null;
        int chunkLength=0;
        String input=null;
        String[] inputS=null;
        // Read chunk length
        input=readCRLFLine(in);
        try
        {
            inputS=input.split(";",2);
            chunkLength=Integer.decode("0x"+inputS[0].trim());
            if(chunkOptions!=null&&inputS.length==2)
            {
                chunkOptions.add(inputS[1].trim());
            }
            if(chunkLength==0)
            {
                if(function!=null)XTTProperties.printDebug(function+": reading chunk: '"+input+"'->end of chunked data");
                StringBuffer plainHeaderOutput=new StringBuffer(function+": received chunked Body Trailers:\n");
                StringBuffer decodedHeaderOutput=new StringBuffer("");
                if(function!=null)XTTProperties.printDebug(function+": reading chunked Body Trailer");
                try
                {
                    readHeaders(in,headers,plainHeaderOutput,decodedHeaderOutput);
                } finally
                {
                    if(decodedHeaderOutput.length()>0)
                    {
                        if(function!=null)XTTProperties.printDebug(plainHeaderOutput.toString());
                        if(function!=null)XTTProperties.printDebug(function+": Decoding Trailer:"+decodedHeaderOutput);
                    }
                }
                return new byte[0];
            }
            if(function!=null)XTTProperties.printDebug(function+": reading chunk: '"+input+"'->"+chunkLength+" bytes");
        } catch (NumberFormatException nfe)
        {
            if(function!=null)XTTProperties.printFail(function+": reading chunk: '"+input+"'-> unable to decode");
            throw nfe;
        }
        // create next chunk
        currentChunks=new byte[chunkLength];
        readBytes(in,currentChunks);
        // Read empty line before next junk length
        input=readCRLFLine(in);
        return currentChunks;
    }

    public static String unchunkBody(String body, LinkedHashMap<String,Vector<String>> headers)
    {
            return new String(unchunkBody(ConvertLib.createBytes(body), headers));
    }
    public static byte[] unchunkBody(byte[] body, LinkedHashMap<String,Vector<String>> headers)
    {
        boolean finished=false;
        byte[] currentChunks=null;
        int chunkLength=0;
        int chunkStart=0;
        int chunkEnd=0;
        String chunkCode=null;
        Vector<ByteArrayWrapper> chunks=new Vector<ByteArrayWrapper>();
        do
        {
            chunkStart=findChunkStart(body,chunkEnd);

            //System.out.println(chunks.size()+" !!!! chunkEnd   ="+chunkEnd);
            //System.out.println(chunks.size()+" !!!! chunkStart ="+chunkStart);

            chunkCode=new String(body,chunkEnd,chunkStart-chunkEnd);

            //System.out.println(chunks.size()+" !!!! chunkCode  ='"+chunkCode+"'");
            //System.out.println(chunks.size()+" !!!! chunkCode  ='0x"+chunkCode.split(";|\r\n",2)[0].trim()+"'");

            chunkLength=Integer.decode("0x"+chunkCode.split(";|\r\n",2)[0].trim());

            //System.out.println(chunks.size()+" !!!! chunkLength="+chunkLength);

            if(chunkLength==0)break;

            chunks.add(new ByteArrayWrapper(body,chunkStart,chunkStart+chunkLength));

            chunkEnd=chunkStart+chunkLength+2;
        } while(!finished);
        int totalLength=0;
        Iterator<ByteArrayWrapper> it=chunks.iterator();
        while(it.hasNext())
        {
            totalLength=totalLength+it.next().getArray().length;
        }
        int pointer=0;
        it=chunks.iterator();
        byte[] unchunkedBody=new byte[totalLength];
        while(it.hasNext())
        {
            currentChunks=it.next().getArray();
            for(int i=0;i<currentChunks.length;i++)
            {
                unchunkedBody[pointer++]=currentChunks[i];
            }
        }
        return unchunkedBody;


    }
    private static int findChunkStart(byte[] body, int start)
    {
        //System.out.println("!!!! '"+new String(body,start,8)+"'");
        for(int i=start;i<body.length-1;i++)
        {
            if(body[i]==0x0D && body[i+1]==0x0A)
            {
                return i+2;
            }
        }
        return body.length;
    }


    public static void writeHeaders(BufferedOutputStream outStream, LinkedHashMap<String,Vector<String>> header, StringBuffer viewBuffer) throws java.io.IOException
    {
        writeHeaders(outStream, header, viewBuffer, true);
    }
    public static void writeHeaders(BufferedOutputStream outStream, LinkedHashMap<String,Vector<String>> header, StringBuffer viewBuffer, boolean appendCRLF) throws java.io.IOException
    {
        Iterator<String> it=header.keySet().iterator();
        Iterator<String> vals=null;
        String headerKey=null;
        Vector<String> headerValues=null;
        String headerValue=null;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValues = header.get(headerKey);
            if(headerKey!=null)
            {
                outStream.write(ConvertLib.createBytes(headerKey+":"));
                viewBuffer.append(headerKey+":");
                vals=headerValues.iterator();
                while(vals.hasNext())
                {
                    headerValue=vals.next();
                    outStream.write(ConvertLib.createBytes(" "+headerValue));
                    outStream.write(CRLF);
                    viewBuffer.append(" "+headerValue+"\r\n");
                }
            } else
            {
                vals=headerValues.iterator();
                while(vals.hasNext())
                {
                    headerValue=vals.next();
                    outStream.write(ConvertLib.createBytes(headerValue));
                    outStream.write(CRLF);
                    viewBuffer.append(headerValue+"\r\n");
                }
            }
        }
        if(appendCRLF)
        {
            outStream.write(CRLF);
            viewBuffer.append("\r\n");
        }
    }
    public static void writeHeader(BufferedOutputStream outStream, LinkedHashMap<String,String> header,StringBuffer viewBuffer) throws java.io.IOException
    {
        writeHeader(outStream,header,viewBuffer,true);
    }
    public static void writeHeader(BufferedOutputStream outStream, LinkedHashMap<String,String> header,StringBuffer viewBuffer, boolean appendCRLF) throws java.io.IOException
    {
        Iterator<String> it=header.keySet().iterator();
        String headerKey=null;
        String headerValue=null;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValue = header.get(headerKey);
            if(headerKey!=null)
            {
                if(!headerValue.equalsIgnoreCase("null"))
                {
                    outStream.write(ConvertLib.createBytes(headerKey+":"));
                    viewBuffer.append(headerKey+":");
                    outStream.write(ConvertLib.createBytes(" "+headerValue));
                    outStream.write(CRLF);
                    viewBuffer.append(" "+headerValue+"\r\n");
                }
            } else
            {
                outStream.write(ConvertLib.createBytes(headerValue));
                outStream.write(CRLF);
                viewBuffer.append(headerValue+"\r\n");
            }
        }
        if(appendCRLF)
        {
            outStream.write(CRLF);
            viewBuffer.append("\r\n");
        }
    }

    /**
     * Make sure "where" is an empty string if not used
     */
    public static void storeHeaders(String outputMessage, String[] storeVar,String where,LinkedHashMap<String,Vector<String>> header)
    {
        Iterator<String> it=header.keySet().iterator();
        Iterator<String> vals=null;
        String headerKey=null;
        Vector<String> headerValues=null;
        String headerValue=null;
        StringBuffer concatheader=null;
        String divider="";
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValues=header.get(headerKey);
            if(headerKey!=null)
            {
                XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey,headerValues.get(0));
                vals=headerValues.iterator();
                concatheader=new StringBuffer();
                int num=0;
                divider="";
                while(vals.hasNext())
                {
                    headerValue=vals.next();
                    num++;
                    XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey+"/"+num,headerValue);
                    concatheader.append(divider+headerValue);
                    divider=", ";
                }
                XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey+"/length",""+num);
                XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey+"/0",""+concatheader);
            } else
            {
                XTTProperties.setMultipleVariables(storeVar,where,headerValues.get(0));
                vals=headerValues.iterator();
                concatheader=new StringBuffer();
                int num=0;
                divider="";
                while(vals.hasNext())
                {
                    headerValue=vals.next();
                    num++;
                    XTTProperties.setMultipleVariables(storeVar,where+"/"+num,headerValue);
                    concatheader.append(divider+headerValue);
                    divider=", ";
                }
                XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey+"/length",""+num);
                XTTProperties.setMultipleVariables(storeVar,where+"/"+headerKey+"/0",""+concatheader);
            }
        }
        StringBuffer headersstored=new StringBuffer();
        for(int i=0;i<storeVar.length;i++)
        {
            headersstored.append("\n  "+storeVar[i]+where+"/");
        }
        XTTProperties.printDebug(outputMessage+headersstored);
    }

    public static byte[] createChunkedBody(byte[] body, int chunkSize) throws java.io.IOException
    {
        int pointer=0;
        int nextChunkSize=0;
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        while(pointer<body.length)
        {
            nextChunkSize=RandomLib.getRandomSize(chunkSize);
            if(nextChunkSize+pointer>body.length)nextChunkSize=body.length-pointer;
            buffer.write(ConvertLib.createBytes(""+ConvertLib.intToHex(nextChunkSize)));
            buffer.write(CRLF);
            buffer.write(body, pointer, nextChunkSize);
            pointer=pointer+nextChunkSize;
            buffer.write(CRLF);
        }
        buffer.write(ConvertLib.createBytes("0"));
        buffer.write(CRLF);
        buffer.write(CRLF);
        return buffer.toByteArray();
    }

    public static String buildURL(String protocol,String host, int port, String path, String ref, String query, String userinfo)
    {
        String sport="";
        if(port>=0)sport=":"+port;
        String suser="";
        if(userinfo!=null)suser=userinfo+"@";
        String squery="";
        if(query!=null)squery="?"+query;
        String sref="";
        if(ref!=null)sref="#"+ref;
        String spath="";
        if(path!=null)
        {
            if(!path.startsWith("/"))
            {
                spath="/"+path;
            } else
            {
                spath=path;
            }
        }
        //System.out.println(protocol+"://"+suser+host+sport+spath+squery+""+sref);
        return protocol+"://"+suser+host+sport+spath+squery+""+sref;
    }


    /**
     * Mapping of file extensions to content-types.
     */
    private static LinkedHashMap<String,String> contentTypeMap=new LinkedHashMap<String,String>();

    static
    {
        fillContentTypeMap();
    }

    public static String getContentType(String suffix)
    {
        String ct=null;
        synchronized(contentTypeMap)
        {
            ct=contentTypeMap.get(suffix);
        }
        if(ct==null)return "unknown/unknown";
        else return ct;
    }

    private static void fillContentTypeMap()
    {
        contentTypeMap.put("", "content/unknown");
        contentTypeMap.put(".ai","application/postscript");
        contentTypeMap.put(".aifc","audio/x-aiff");
        contentTypeMap.put(".aif","audio/x-aiff");
        contentTypeMap.put(".aiff","audio/x-aiff");
        contentTypeMap.put(".asc","text/plain");
        contentTypeMap.put(".atom","application/atom+xml");
        contentTypeMap.put(".au","audio/basic");
        contentTypeMap.put(".avi","video/x-msvideo");
        contentTypeMap.put(".bcpio","application/x-bcpio");
        contentTypeMap.put(".bin","application/octet-stream");
        contentTypeMap.put(".bmp","image/bmp");
        contentTypeMap.put(".c", "text/plain");
        contentTypeMap.put(".c++", "text/plain");
        contentTypeMap.put(".cc", "text/plain");
        contentTypeMap.put(".cdf","application/x-netcdf");
        contentTypeMap.put(".cgm","image/cgm");
        contentTypeMap.put(".class","application/octet-stream");
        contentTypeMap.put(".cpio","application/x-cpio");
        contentTypeMap.put(".cpt","application/mac-compactpro");
        contentTypeMap.put(".csh","application/x-csh");
        contentTypeMap.put(".css","text/css");
        contentTypeMap.put(".dcr","application/x-director");
        contentTypeMap.put(".dir","application/x-director");
        contentTypeMap.put(".djv","image/vnd.djvu");
        contentTypeMap.put(".djvu","image/vnd.djvu");
        contentTypeMap.put(".dll","application/octet-stream");
        contentTypeMap.put(".dmg","application/octet-stream");
        contentTypeMap.put(".dms","application/octet-stream");
        contentTypeMap.put(".doc","application/msword");
        contentTypeMap.put(".dtd","application/xml-dtd");
        contentTypeMap.put(".dvi","application/x-dvi");
        contentTypeMap.put(".dxr","application/x-director");
        contentTypeMap.put(".eps","application/postscript");
        contentTypeMap.put(".etx","text/x-setext");
        contentTypeMap.put(".exe","application/octet-stream");
        contentTypeMap.put(".ez","application/andrew-inset");
        contentTypeMap.put(".gif","image/gif");
        contentTypeMap.put(".gram","application/srgs");
        contentTypeMap.put(".grxml","application/srgs+xml");
        contentTypeMap.put(".gtar","application/x-gtar");
        contentTypeMap.put(".gz","application/x-gzip");
        contentTypeMap.put(".h", "text/plain");
        contentTypeMap.put(".hdf","application/x-hdf");
        contentTypeMap.put(".hdml", "text/x-hdml");
        contentTypeMap.put(".hqx","application/mac-binhex40");
        contentTypeMap.put(".html","text/html");
        contentTypeMap.put(".htm","text/html");
        contentTypeMap.put(".ice","x-conference/x-cooltalk");
        contentTypeMap.put(".ico","image/x-icon");
        contentTypeMap.put(".ics","text/calendar");
        contentTypeMap.put(".ief","image/ief");
        contentTypeMap.put(".ifb","text/calendar");
        contentTypeMap.put(".iges","model/iges");
        contentTypeMap.put(".igs","model/iges");
        contentTypeMap.put(".java", "text/plain");
        contentTypeMap.put(".jpeg","image/jpeg");
        contentTypeMap.put(".jpe","image/jpeg");
        contentTypeMap.put(".jpg","image/jpeg");
        contentTypeMap.put(".js","application/x-javascript");
        contentTypeMap.put(".kar","audio/midi");
        contentTypeMap.put(".latex","application/x-latex");
        contentTypeMap.put(".lha","application/octet-stream");
        contentTypeMap.put(".lzh","application/octet-stream");
        contentTypeMap.put(".m3u","audio/x-mpegurl");
        contentTypeMap.put(".m4u","video/vnd.mpegurl");
        contentTypeMap.put(".man","application/x-troff-man");
        contentTypeMap.put(".mathml","application/mathml+xml");
        contentTypeMap.put(".me","application/x-troff-me");
        contentTypeMap.put(".mesh","model/mesh");
        contentTypeMap.put(".mid","audio/midi");
        contentTypeMap.put(".midi","audio/midi");
        contentTypeMap.put(".mif","application/vnd.mif");
        contentTypeMap.put(".mov","video/quicktime");
        contentTypeMap.put(".movie","video/x-sgi-movie");
        contentTypeMap.put(".mp2","audio/mpeg");
        contentTypeMap.put(".mp3","audio/mpeg");
        contentTypeMap.put(".mpe","video/mpeg");
        contentTypeMap.put(".mpeg","video/mpeg");
        contentTypeMap.put(".mpg","video/mpeg");
        contentTypeMap.put(".mpga","audio/mpeg");
        contentTypeMap.put(".ms","application/x-troff-ms");
        contentTypeMap.put(".msh","model/mesh");
        contentTypeMap.put(".mxu","video/vnd.mpegurl");
        contentTypeMap.put(".nc","application/x-netcdf");
        contentTypeMap.put(".oda","application/oda");
        contentTypeMap.put(".ogg","application/ogg");
        contentTypeMap.put(".pbm","image/x-portable-bitmap");
        contentTypeMap.put(".pdb","chemical/x-pdb");
        contentTypeMap.put(".pdf","application/pdf");
        contentTypeMap.put(".pgm","image/x-portable-graymap");
        contentTypeMap.put(".pgn","application/x-chess-pgn");
        contentTypeMap.put(".pl", "text/plain");
        contentTypeMap.put(".png","image/png");
        contentTypeMap.put(".pnm","image/x-portable-anymap");
        contentTypeMap.put(".ppm","image/x-portable-pixmap");
        contentTypeMap.put(".ppt","application/vnd.ms-powerpoint");
        contentTypeMap.put(".ps","application/postscript");
        contentTypeMap.put(".qt","video/quicktime");
        contentTypeMap.put(".ra","audio/x-pn-realaudio");
        contentTypeMap.put(".ram","audio/x-pn-realaudio");
        contentTypeMap.put(".ras","image/x-cmu-raster");
        contentTypeMap.put(".rdf","application/rdf+xml");
        contentTypeMap.put(".rgb","image/x-rgb");
        contentTypeMap.put(".rm","application/vnd.rn-realmedia");
        contentTypeMap.put(".roff","application/x-troff");
        contentTypeMap.put(".rtf","text/rtf");
        contentTypeMap.put(".rtx","text/richtext");
        contentTypeMap.put(".sgml","text/sgml");
        contentTypeMap.put(".sgm","text/sgml");
        contentTypeMap.put(".sh","application/x-sh");
        contentTypeMap.put(".shar","application/x-shar");
        contentTypeMap.put(".silo","model/mesh");
        contentTypeMap.put(".sit","application/x-stuffit");
        contentTypeMap.put(".skd","application/x-koan");
        contentTypeMap.put(".skm","application/x-koan");
        contentTypeMap.put(".skp","application/x-koan");
        contentTypeMap.put(".skt","application/x-koan");
        contentTypeMap.put(".smi","application/smil");
        contentTypeMap.put(".smil","application/smil");
        contentTypeMap.put(".snd","audio/basic");
        contentTypeMap.put(".so","application/octet-stream");
        contentTypeMap.put(".spl","application/x-futuresplash");
        contentTypeMap.put(".src","application/x-wais-source");
        contentTypeMap.put(".sv4crc","application/x-sv4crc");
        contentTypeMap.put(".sv4cpio","application/x-sv4cpio");
        contentTypeMap.put(".svg","image/svg+xml");
        contentTypeMap.put(".swf","application/x-shockwave-flash");
        contentTypeMap.put(".t","application/x-troff");
        contentTypeMap.put(".tar","application/x-tar");
        contentTypeMap.put(".tcl","application/x-tcl");
        contentTypeMap.put(".texi","application/x-texinfo");
        contentTypeMap.put(".texinfo","application/x-texinfo");
        contentTypeMap.put(".tex","application/x-tex");
        contentTypeMap.put(".text", "text/plain");
        contentTypeMap.put(".tiff","image/tiff");
        contentTypeMap.put(".tif","image/tiff");
        contentTypeMap.put(".tr","application/x-troff");
        contentTypeMap.put(".tsv","text/tab-separated-values");
        contentTypeMap.put(".txt", "text/plain");
        contentTypeMap.put(".txt","text/plain");
        contentTypeMap.put(".ustar","application/x-ustar");
        contentTypeMap.put(".vcd","application/x-cdlink");
        contentTypeMap.put(".vrml","model/vrml");
        contentTypeMap.put(".vxml","application/voicexml+xml");
        contentTypeMap.put(".wav","audio/x-wav");
        contentTypeMap.put(".wbmp","image/vnd.wap.wbmp");
        contentTypeMap.put(".wbxml","application/vnd.wap.wbxml");
        contentTypeMap.put(".wca", "application/vnd.wap.wtls-ca-certificate");
        contentTypeMap.put(".wml","text/vnd.wap.wml");
        contentTypeMap.put(".wmlc","application/vnd.wap.wmlc");
        contentTypeMap.put(".wmls","text/vnd.wap.wmlscript");
        contentTypeMap.put(".wmlsc","application/vnd.wap.wmlscriptc");
        contentTypeMap.put(".wrl","model/vrml");
        contentTypeMap.put(".xbm","image/x-xbitmap");
        contentTypeMap.put(".xht","application/xhtml+xml");
        contentTypeMap.put(".xhtml","application/xhtml+xml");
        contentTypeMap.put(".xls","application/vnd.ms-excel");
        contentTypeMap.put(".xml","application/xml");
        contentTypeMap.put(".xpm","image/x-xpixmap");
        contentTypeMap.put(".xsl","application/xml");
        contentTypeMap.put(".xslt","application/xslt+xml");
        contentTypeMap.put(".xul","application/vnd.mozilla.xul+xml");
        contentTypeMap.put(".xwd","image/x-xwindowdump");
        contentTypeMap.put(".xwml","application/wml+xml");
        contentTypeMap.put(".xyz","chemical/x-xyz");
        contentTypeMap.put(".zip","application/zip");
    }
}
