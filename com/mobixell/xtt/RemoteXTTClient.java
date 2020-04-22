package com.mobixell.xtt;

import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.net.Socket;
import java.net.SocketException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.InetSocketAddress;

import java.util.Vector;
import java.util.LinkedHashMap;

import org.jdom.Document;
import org.jdom.Element;

public class RemoteXTTClient
{    
    private final static String CRLF="\r\n";

    protected static RemoteXTTPacket executeRemoteCommand(String command, int timeout, InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element executeTag = new Element("execute");
        Element commandTag = new Element("command");

        commandTag.addContent(command);
        executeTag.addContent(commandTag);

        if(timeout >= 0)
        {
            Element timeoutTag = new Element("timeout");
            timeoutTag.addContent(""+timeout);
            executeTag.addContent(timeoutTag);
        }

        requestTag.addContent(executeTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address);
    }
    protected static RemoteXTTPacket executeRemoteCommand(String command, int timeout, InetSocketAddress address, boolean stealtMode)
    {
        Element requestTag = new Element("request");
        Element executeTag = new Element("execute");
        Element commandTag = new Element("command");

        commandTag.addContent(command);
        executeTag.addContent(commandTag);

        if(timeout >= 0)
        {
            Element timeoutTag = new Element("timeout");
            timeoutTag.addContent(""+timeout);
            executeTag.addContent(timeoutTag);
        }

        requestTag.addContent(executeTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address,stealtMode);
    }
    protected static RemoteXTTPacket getRemoteSystemTime(InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element executeTag = new Element("getTime");
        requestTag.addContent(executeTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address);
    }

    protected static RemoteXTTPacket listRemoteProducts(InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element listTag = new Element("listProducts");
        requestTag.addContent(listTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address);
    }
    public static RemoteXTTPacket getStatus(InetSocketAddress address, boolean stealtMode)
    {
        Element requestTag = new Element("request");
        Element statusTag = new Element("getStatus");

        requestTag.addContent(statusTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address,stealtMode,-1);
    }
    protected static RemoteXTTPacket setRemoteTotalTests(InetSocketAddress address, int totaltests)
    {
        Element requestTag = new Element("request");
        Element statusTag = new Element("getStatus");

        requestTag.addContent(statusTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address,false,totaltests);
    }

    protected static RemoteXTTPacket writeRemoteFile(String fileName, String fileContent, String fileHash, InetSocketAddress address)
    {
        XTTProperties.printInfo("writeRemoteFile: Writing File: " + fileName);

        Element requestTag = new Element("request");
        Element fileTag = new Element("writeFile");
        Element fileNameTag = new Element("fileName");
        fileNameTag.addContent(fileName);
        fileTag.addContent(fileNameTag);

        Element fileDigestTag = new Element("fileMd5Digest");
        fileDigestTag.addContent(fileHash);
        fileTag.addContent(fileDigestTag);
        requestTag.addContent(fileTag);

        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);
        RemoteXTTPacket responsePacket = sendToRemoteXTT(requestXML,address);

        if(responsePacket.getReturnCode() == 0)
        {
            XTTProperties.printInfo("writeRemoteFile: The same file already existed on the remote machine, not re-sending");
        }
        else
        {
            Element fileContentTag = new Element("fileContent");
            fileContentTag.addContent(fileContent);
            fileTag.addContent(fileContentTag);

            responsePacket = sendToRemoteXTT(requestXML,address);
            if(responsePacket.getReturnCode() != 0)
            {
                XTTProperties.printFail("writeRemoteFile: Error writing file");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }

        return responsePacket;
    }

    protected static RemoteXTTPacket readRemoteFile(String fileName, InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element fileTag = new Element("readFile");
        Element fileNameTag = new Element("fileName");

        fileNameTag.addContent(fileName);
        fileTag.addContent(fileNameTag);
        requestTag.addContent(fileTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);
        RemoteXTTPacket dataResponse = sendToRemoteXTT(requestXML,address);

        return dataResponse;
    }

    protected static RemoteXTTPacket getNewestFile(String directoryName, String filter, InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element getNewestFileTag = new Element("getnewestfile");
        Element directoryTag = new Element("directory");
        Element filterTag = new Element("filter");

        directoryTag.addContent(directoryName);
        getNewestFileTag.addContent(directoryTag);
        
        if(filter != null)
        {
            filterTag.addContent(filter);
            getNewestFileTag.addContent(filterTag);
        }

        requestTag.addContent(getNewestFileTag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);
        RemoteXTTPacket dataResponse = sendToRemoteXTT(requestXML,address);

        return dataResponse;
    }

    protected static RemoteXTTPacket whereAmI(InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element whereAmITag = new Element("whereAmI");
        requestTag.addContent(whereAmITag);
        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);
        RemoteXTTPacket dataResponse = sendToRemoteXTT(requestXML,address);

        return dataResponse;
    }
    
    protected static RemoteXTTPacket stopRemoteXTT(InetSocketAddress address)
    {
        Element requestTag = new Element("request");
        Element stopRemoteXTTTag = new Element("stopRemoteXTT");

        requestTag.addContent(stopRemoteXTTTag);

        Document requestXML = new Document(new Element("remote"));
        requestXML.getRootElement().addContent(requestTag);

        RemoteXTTPacket dataResponse = sendToRemoteXTT(requestXML,address);

        return dataResponse;
    }    

    /*
    *   Takes a String then wraps it in the XML needed for transfer to Remote XTT
    */
    protected static RemoteXTTPacket sendToRemoteXTT(String data, InetSocketAddress address)
    {
        Element requestTag = new Element("request");

        Element dataTag = new Element("data");
        dataTag.addContent(data);
        requestTag.addContent(dataTag);

        Document requestXML = new Document(new Element("remote"));

        requestXML.getRootElement().addContent(requestTag);

        return sendToRemoteXTT(requestXML,address);
    }
    /*
     *   Expects an XML document properly formatted for transfer to Remote XTT
     */
     protected static RemoteXTTPacket sendToRemoteXTT(Document data, InetSocketAddress address,boolean stealtMode)
     {
         return sendToRemoteXTT(data, address, stealtMode,-1);
     }
    /*
    *   Expects an XML document properly formatted for transfer to Remote XTT
    */
    protected static RemoteXTTPacket sendToRemoteXTT(Document data, InetSocketAddress address)
    {
        return sendToRemoteXTT(data, address, false,-1);
    }
    protected static RemoteXTTPacket sendToRemoteXTT(Document data, InetSocketAddress address, boolean stealtMode, int expectedTests)
    {
        PrintStream out = null;
        BufferedInputStream in = null;
        Socket sock = null;
        Document dataResponse = null;
        RemoteXTTPacket responsePacket=new RemoteXTTPacket(null);

        // Info elements added normaly when not in stealth mode
        if(!stealtMode)
        {
            Element infoTag = new Element("info");
            Element nameTag = new Element("name");
            nameTag.addContent(XTTProperties.getCurrentTestName());
            infoTag.addContent(nameTag);
            Element totalTestTag = new Element("totaltests");
            int totaltests=XTTProperties.getNumberOfTests();
            if(totaltests==0)totaltests=1;
            totalTestTag.addContent(""+totaltests);
            infoTag.addContent(totalTestTag);

            if(expectedTests>=0)
            {
                Element expectedTestsTag = new Element("expectedtests");
                expectedTestsTag.addContent(""+expectedTests);
                infoTag.addContent(expectedTestsTag);
            }
            Element currentTestTag = new Element("currentest");
            int currenttests=XTTProperties.getCurrentTestNumber();
            currenttests++;
            currentTestTag.addContent(""+currenttests);
            infoTag.addContent(currentTestTag);
    
            data.getRootElement().addContent(infoTag);
        }

        try
        {
            sock = new Socket();
            sock.setKeepAlive(true);
            sock.connect(address);
            sock.setTcpNoDelay(true);
            if(stealtMode)
            {
                //stealth mode is used by the guy, we do not want to keep hanging in case remotextt doesn't answer
                sock.setSoTimeout(30000);
            }

            if(!stealtMode)XTTProperties.printDebug("Remote connection: "+sock.getLocalAddress()+":"+sock.getLocalPort()+"->"+sock.getInetAddress()+":"+sock.getPort());
            out = new PrintStream(sock.getOutputStream());
            in  = new BufferedInputStream(sock.getInputStream());
            //XTT helper function to stream an XML document to a given stream
            byte[] dataToSend=ConvertLib.createBytes(XTTXML.stringXML(data));
            StringBuffer headers=new StringBuffer();
            headers.append("POST /remoteXTT HTTP/1.0"+CRLF);
            headers.append("date: "+HTTPHelper.createHTTPDate()+CRLF);
            headers.append("connection: close"+CRLF);
            headers.append("accept: text/xml"+CRLF);
            headers.append("user-agent: XTT/"+XTTProperties.getXTTBuildVersion()
                +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
                +System.getProperties().getProperty("os.name")+" "
                +System.getProperties().getProperty("os.arch")+" "
                +System.getProperties().getProperty("os.version")+"; "
                +System.getProperties().getProperty("user.name")+"; "
                +"RemoteXTT"+"; "
                +"$Revision: 1.11 $"
                +")"+CRLF);
            headers.append("content-type: text/xml"+CRLF);
            headers.append("content-length: "+dataToSend.length+CRLF);
            headers.append(CRLF);
            
            byte[] headerToSend=ConvertLib.createBytes(headers.toString());
            out.write(headerToSend);
            out.write(dataToSend);
            out.flush();
            org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();
            byte[] buf = new byte[0];
            try
            {
                LinkedHashMap<String,Vector<String>> header=HTTPHelper.readHTTPStreamHeaders(null,in);
                //End fo stream so break or invaild
                if(header.get(null)==null) throw new IOException("Invalid Protocol found");    
                try
                {
                } catch(Exception ex){}

                if(header.get("transfer-encoding")!=null)
                {
                    if(header.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                    {
                        buf=HTTPHelper.readChunkedBody(null,in,header);
                    }
                } 
                else
                {
                    Vector<String> contentlengthVector=header.get("content-length");
                    if(contentlengthVector!=null)
                    {
                        int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                        buf=new byte[contentlength];
                        HTTPHelper.readBytes(in,buf);
                    } else
                    {
                        throw new IOException("Invalid Protocol, content-length not found");    
                    }
                }
                dataResponse = parser.build(new ByteArrayInputStream(buf));
                checkResponseVersion(dataResponse);
            } catch(IOException ioe)
            {
                if(!stealtMode)
                {
                    XTTProperties.printFail("RemoteXTT didn't respond properly");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(ioe);
                    }
                }
                responsePacket.setException(ioe);
            } catch(org.jdom.JDOMException je)
            {
                if(!stealtMode)
                {
                    XTTProperties.printFail("RemoteXTT didn't respond properly");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(je);
                    }
                }
                responsePacket.setException(je);
            }

            if(!stealtMode)XTTProperties.printDebug("Remote Command Response:\n"+XTTXML.stringXML(dataResponse)+"\nEND of response");
        } catch (SocketTimeoutException ste) //Error if a connection can't be made
        {
            if(!stealtMode)
            {
                XTTProperties.printFail("RemoteXTT: Timeout on RemoteXTT connection!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            responsePacket.setException(ste);
        } catch (ConnectException ce) //Error if a connection can't be made
        {
            if(!stealtMode)
            {
                XTTProperties.printFail("RemoteXTT: Remote XTT isn't running");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            responsePacket.setException(ce);
        } catch (SocketException se) //Parent of ConnectException, used here to see that connection was denied
        {
            if(!stealtMode)
            {
                XTTProperties.printFail("RemoteXTT: Remote XTT denied your IP");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printException(se);
            }
            responsePacket.setException(se);
        } catch (Exception e) //Lastly something else went wrong
        {
            if(!stealtMode)
            {
                XTTProperties.printFail("RemoteXTT: Couldn't Test Launcher remotely");
                if (XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            responsePacket.setException(e);
        } finally
        {
            try
            {
                if(!stealtMode)XTTProperties.printDebug("SendToRemoteXTT: Trying to clean up sockets.");
                in.close();
                out.close();
                sock.close();
            }
            catch(Exception e)
            {
            }
        }
        responsePacket.setDocument(dataResponse);
        return responsePacket;
    }
    protected synchronized static boolean isRemoteXTTRunning(InetSocketAddress address)
    {
		PrintStream out = null;
		Socket sock = null;

		Element requestTag = new Element("request");
		Element statusTag = new Element("watchDog");

		requestTag.addContent(statusTag);
		Document data = new Document(new Element("remote"));
		data.getRootElement().addContent(requestTag);
         
         byte[] dataToSend=ConvertLib.createBytes(XTTXML.stringXML(data));
         StringBuffer headers=new StringBuffer();
         headers.append("POST /remoteXTT HTTP/1.0"+CRLF);
         headers.append("date: "+HTTPHelper.createHTTPDate()+CRLF);
         headers.append("connection: close"+CRLF);
         headers.append("accept: text/xml"+CRLF);
         headers.append("user-agent: XTT/"+XTTProperties.getXTTBuildVersion()
             +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
             +System.getProperties().getProperty("os.name")+" "
             +System.getProperties().getProperty("os.arch")+" "
             +System.getProperties().getProperty("os.version")+"; "
             +System.getProperties().getProperty("user.name")+"; "
             +"RemoteXTT"+"; "
             +"$Revision: 1.11 $"
             +")"+CRLF);
         headers.append("content-type: text/xml"+CRLF);
         headers.append("content-length: "+dataToSend.length+CRLF);
         headers.append(CRLF);
         
         byte[] headerToSend=ConvertLib.createBytes(headers.toString());
		try {
			sock = new Socket();
			sock.setKeepAlive(true);
			sock.connect(address);
			sock.setTcpNoDelay(true);
			out = new PrintStream(sock.getOutputStream());
			out.write(headerToSend);
			out.write(dataToSend);
			out.flush();
		} 
		catch (IOException ioe) 
		{
			try {
				out.close();
				sock.close();
			} catch (Exception e) {
			}
			return false;
		}
		try {
			out.close();
			sock.close();
		} 
		catch (Exception e) 
		{
			return false;
		}
		return true;
    }
    private static void checkResponseVersion(Document dataResponse) throws Exception
    {
        String   result  = XTTXML.getElement("response/version",dataResponse.getDocument()).getText();
        if(result.equals(XTTProperties.PRIVATEVERSION))return;
        String[] resultS = result.split("\\.");
        
        int version=Integer.parseInt(resultS[0])*100000+Integer.parseInt(resultS[1])*10000+Integer.parseInt(resultS[2]);
                    
        if(version<200176)throw new java.lang.UnsupportedOperationException("This version of RemoteXTT is no longer supported (<2.0.0176)!");
    }
}
