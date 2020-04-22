package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;
import java.net.Socket;
import java.net.URL;
import java.net.InetAddress;
import java.net.URLEncoder;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.TrustManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


/**
 * This class has been written as a replacement of javas http class. It should do most of the job at the momment,
 * including following a 301/302 redirect.
 *
 * @author      Roger Soder
 * @version     $Id: HTTPConnection.java,v 1.20 2010/03/18 05:30:39 rajesh Exp $
 */
public class HTTPConnection implements WebHttpConstants
{
    public final static int DEFAULT_HTTP_PORT  = 80;
    public final static int DEFAULT_HTTPS_PORT =443;
    public final static int MAXRESPONSESIZE = 1000000;
    private static final String sCRLF = "\r\n";

    public HTTPConnection(String name, String module)
    {
        this.name=name;
        this.module=module.toUpperCase();
    }
    private String name = null;
    public String getName(){return name;}
    private String module = null;
    public String getModule(){return module;}
    //Configuration//////////////////////////////////////////////////////////////////////////////////////////////
    private boolean followRedirects = true;
    public void setFollowRedirects(boolean follow){this.followRedirects=follow;}
    private String httpProxyHost        = null;
    private int    httpProxyPort        = -1;
    public void setProxy(String proxyHost, String proxyPort) throws java.net.UnknownHostException
    {
        if(proxyHost==null||proxyPort==null)
        {
            this.httpProxyHost=null;
            this.httpProxyPort=-1;
            return;
        }
        DNSServer.resolveAddressToInetAddress(proxyHost);
        int port=Integer.decode(proxyPort);
        if(port<0||port>65535) throw new java.lang.NumberFormatException("Out of Range");
        this.httpProxyHost=proxyHost;
        this.httpProxyPort=port;
    }
    private int timeout                = 30000;
    public void setTimeout(int timeout){this.timeout=timeout;}
    private boolean keepAlive                = true;
    public void setKeepAlive(boolean keepAlive){this.keepAlive=keepAlive;}
    private boolean enableCertcheck          = false;
    public void setEnableCertcheck(boolean enableCertcheck){this.enableCertcheck=enableCertcheck;}
    private boolean enableURLContract          = false;
    public void setEnableURLContract(boolean enableURLContract){this.enableURLContract=enableURLContract;}
    private String protocolVersion           = "1.1";
    public void setProtocolVersion(String version)
    {
        if(version.equals("1.0"))
        {
            keepAlive = false;
            protocolVersion="1.0";
        } else if (version.equals("1.1"))
        {
            keepAlive = true;
            protocolVersion="1.1";
        } else
        {
            XTTProperties.printFail("HTTPConnection: unsupported protocol "+version);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            throw new IllegalArgumentException("unsupported HTTP protocol "+version);
        }
    }
    
    private boolean closeCalled=false;

    private int transferChunkSize   = 1024;
    public void setTransferChunkSize(int transferChunkSize){this.transferChunkSize=transferChunkSize;}
    int bufferOutputSize = -1;
    public void setBufferOutputSize(int bufferOutputSize){this.bufferOutputSize=bufferOutputSize;}
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private java.security.cert.Certificate[] certs= new java.security.cert.Certificate[0];

    private byte[] response = new byte[0];
    public byte[] getResponse(){return response;}
    public void setResponse(byte[] response){this.response=response;}

    private LinkedHashMap<String,String> requestHeader  = new LinkedHashMap<String,String>();
    public LinkedHashMap<String,String> getRequestHeader(){return requestHeader;}

    private LinkedHashMap<String,Vector<String>> responseHeader = new LinkedHashMap<String,Vector<String>>();
    public void setResponseHeader(LinkedHashMap<String,Vector<String>> responseHeader){this.responseHeader=responseHeader;}
    public LinkedHashMap<String,Vector<String>> getResponseHeader(){return responseHeader;}

    private int responseCode=0;
    public void setResponseCode(int code){this.responseCode=code;}
    public int getResponseCode(){return responseCode;}
    private String responseMessage=null;
    public void setResponseMessage(String message){this.responseMessage=message;}
    public String getResponseMessage(){return responseMessage;}

    private byte[] postDataBytes = null;
    public byte[] getPostDataBytes(){return postDataBytes;};
    public void setPostDataBytes(byte[] data){this.postDataBytes=data;}
    private LinkedHashMap<String,String> postData = new LinkedHashMap<String,String>();
    public LinkedHashMap<String,String> getPostData(){return postData;}
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Used to send a POST request to an URL
     */
    public void sendPostRequest(String funcname, String surl) throws IOException
    {
        sendGetRequest(funcname + "(" + module + ")",surl,true, null,null,65536);
    }
    /**
     * Used to send a POST request to an URL
     */
    public void sendGetRequest(String funcname, String surl) throws IOException
    {
        sendGetRequest(funcname + "(" + module + ")",surl,false, null,null,65536);
    }
    /**
     * Used to send a POST request to an URL
     */
    public void sendGetRequestWithBreak(String funcname, String surl,String bytesToRead) throws IOException
    {
        sendGetRequest(funcname + "(" + module + ")",surl,false, null,bytesToRead,65536);
    }
    /**
     * Used to send a POST request to an URL
     */
    public void sendGetRequest(String funcname, String surl,int readBufferSize) throws IOException
    {
        sendGetRequest(funcname + "(" + module + ")",surl,false, null,null,readBufferSize);
    }
    /**
     * Use this to send WebDAV requests with or without body
     */
    public void sendFreeRequest(String funcname, String surl, String method, boolean hasBody) throws IOException
    {
        sendGetRequest(funcname + "(" + module + ")",surl,hasBody,method,null,65536);
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The implementation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private String lastConnectedHostPort=null;
    private Socket socket=null;
    private int redirectCounter=0;

    private void sendGetRequest(String funcname, String surl, boolean isPost, String overrideMethod,String bytesToRead,int readBufferSize) throws IOException
    {
        try
        {
            closeCalled=false;
            URL remoteURL=null;
            String remoteHost=null;
            int remotePort=-1;
            //Handle IPv6 addresses - append [ & ] if URL doesn't contain - this is important so that URL class accepts proper URLs
            String hostAddress = surl.split("/")[2];
            if(surl.split(":").length > 3  && !(surl.contains("[")||surl.contains("]")) && hostAddress.contains(":")&& hostAddress.split(":").length>2)
            {
               String ipPort = surl.split("/")[2];   
                System.out.println("ipPort .."+ipPort);
                int lastIndexOfColon = ipPort.lastIndexOf(":");
                remoteHost =ipPort.substring(0, lastIndexOfColon); 
                System.out.println("remoteHost ..."+remoteHost);
                System.out.println("remotePost ..."+ipPort.substring(lastIndexOfColon+1, ipPort.length()));
                remotePort = Integer.decode(ipPort.substring(lastIndexOfColon+1, ipPort.length())); 
                System.out.println("remoteHost ..."+remoteHost);
                surl = surl.replace(remoteHost, "["+remoteHost+"]");   
                System.out.println("surl surl"+surl);
                surl =surl.replace(hostAddress,"["+hostAddress+"]");
                remoteURL    = new URL(surl);
            }
            else
            {
                remoteURL    = new URL(surl);
            }
            String requestURL=surl;
            if(enableURLContract)
            {
                String query=remoteURL.getQuery();
                if(query!=null)
                {
                    requestURL=remoteURL.getPath()+"?"+query;
                } else
                {
                    requestURL=remoteURL.getPath();
                }
            }
            remoteHost=remoteURL.getHost();
            remotePort=remoteURL.getPort();
    
            boolean isHTTPS=false;
            if(remoteURL.getProtocol().equals("https"))
            {
                isHTTPS=true;
                if(remotePort==-1)remotePort=DEFAULT_HTTPS_PORT;
            }
            if(remotePort==-1)remotePort=DEFAULT_HTTP_PORT;
    
            String host=remoteHost;
            int port=remotePort;
    
            boolean isLoopback=false;
            try
            {
				if (DNSServer.isHostExist(host))
				{
					if (DNSServer.isIPv4Host(host))
					{
						InetAddress i = DNSServer.resolveAddressToInetAddress(host);
						isLoopback = i.isLoopbackAddress();
					}
					else
					{
						InetAddress i = DNSServer.resolveIPv6AddressToInetAddress(host);
						isLoopback = i.isLoopbackAddress();
					}
				}
				else
				{
					InetAddress i = DNSServer.resolveAddressToInetAddress(host);
					isLoopback = i.isLoopbackAddress();
					XTTProperties.printDebug(funcname + ": The url " + host + " didnt exist in the local dns entry");
				}
            } catch(Exception lle)
            {}
    
            boolean isProxyConnection=false;
            if(httpProxyHost!=null&&httpProxyPort!=-1&&!isLoopback)
            {
                host=httpProxyHost;
                port=httpProxyPort;
                isProxyConnection=true;
            }
            String proxyInfo="";
            if(isProxyConnection)
            {
                proxyInfo="("+host+":"+port+")";
            }
            XTTProperties.printInfo(funcname + ": proxy="+isProxyConnection+proxyInfo+" download: "+surl);
            if(keepAlive)
            {
                if(!(new String(host+":"+port)).equals(lastConnectedHostPort))
                {
                    try{socket.close();} catch (Exception e){}
                    socket=null;
                }
            } else
            {
                try{socket.close();} catch (Exception e){}
                socket=null;
            }
            lastConnectedHostPort=host+":"+port;
    
         ////// CREATE HEADERS
            LinkedHashMap<String,String> requestHeadersToSend=new LinkedHashMap<String,String>();
    
            // REQUEST
            if(overrideMethod!=null)
            {
                requestHeadersToSend.put(null,overrideMethod+" "+requestURL+" HTTP/"+protocolVersion);
            } else if(isPost)
            {
                requestHeadersToSend.put(null,"POST "+requestURL+" HTTP/"+protocolVersion);
            } else
            {
                requestHeadersToSend.put(null,"GET "+requestURL+" HTTP/"+protocolVersion);
            }
            // HOST
            int defaultPort=DEFAULT_HTTP_PORT;
            if(isHTTPS)defaultPort=DEFAULT_HTTPS_PORT;
            String portString="";
            if(remotePort!=defaultPort)portString=":"+remotePort;
            requestHeadersToSend.put("host",remoteHost+portString);
            // DATE
            requestHeadersToSend.put("date",HTTPHelper.createHTTPDate());
            // CONNECTION/PROXY-CONNECTION
            String connectionType="keep-alive";
            if(!keepAlive)connectionType="close";
            if(isProxyConnection&&!isHTTPS)
            {
                requestHeadersToSend.put("proxy-connection",connectionType);
            } else
            {
                requestHeadersToSend.put("connection",connectionType);
            }
            //ACCEPT
            requestHeadersToSend.put("accept","text/html, image/gif, image/jpeg, image/png, *; q=.2, */*; q=.2");
            //USER-AGENT
            requestHeadersToSend.put("user-agent","XTT/"+XTTProperties.getXTTBuildVersion()
                +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
                +System.getProperties().getProperty("os.name")+" "
                +System.getProperties().getProperty("os.arch")+" "
                +System.getProperties().getProperty("os.version")+"; "
                +System.getProperties().getProperty("user.name")+"; "
                +module+"; "
                +"$Revision: 1.20 $"
                +")");
    
            // create post body
            byte[] postBody=null;
            if(isPost)
            {
                // Send post data
                String transferEncoding=requestHeader.get("transfer-encoding");
                if(transferEncoding!=null&&transferEncoding.equals("chunked"))
                {
                    postBody=getPostBody(requestHeadersToSend,requestHeader,true);
                } else
                {
                    postBody=getPostBody(requestHeadersToSend,requestHeader,false);
                }
            }
            //PRESET HEADERS
            mergeHeaders(requestHeadersToSend,requestHeader);
         ////// CREATE HEADERS
    
         ////// CONNECT
            boolean reusedSocket=false;
            String loopback="";
            if(isLoopback)loopback="loopback ";
            if(socket==null||socket.isClosed()||!socket.isConnected())
            {
				if (DNSServer.isHostExist(host))
				{
					if (DNSServer.isIPv4Host(host))
					{
						socket = new Socket(DNSServer.resolveAddressToInetAddress(host), port);
					}
					else
					{
						socket = new Socket(DNSServer.resolveIPv6AddressToInetAddress(host), port);
					}
					XTTProperties.printVerbose(funcname + ": opened " + loopback + socket);
				}
				else
				{
					socket = new Socket(DNSServer.resolveAddressToInetAddress(host), port);
				}
            } else
            {
                XTTProperties.printVerbose(funcname + ": reused "+loopback+socket);
                reusedSocket=true;
            }
            //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(timeout);
    
            if(isHTTPS)
            {
                if(isProxyConnection)
                {
                    BufferedInputStream inP = new java.io.BufferedInputStream(socket.getInputStream(),readBufferSize);
                    BufferedOutputStream outP = new java.io.BufferedOutputStream(socket.getOutputStream());
                    outP.write(ConvertLib.createBytes("CONNECT "+remoteHost+":"+remotePort+" HTTP/1.0\r\n"
                                +"User-Agent: "+requestHeadersToSend.get("user-agent")+"\r\n\r\n"));
                    outP.flush();
                    XTTProperties.printDebug(funcname + ": Connecting to proxy:\n"+"CONNECT "+remoteHost+":"+remotePort+" HTTP/1.0\r\n"
                                +"User-Agent: "+requestHeadersToSend.get("user-agent")+"\r\n\r\n");
                    LinkedHashMap<String,Vector<String>> connectResponseHeader=HTTPHelper.readHTTPStreamHeaders(funcname,inP);
                    String firstLineS=getHeaderValue(connectResponseHeader,null);
                    String[] firstLine=firstLineS.split("\\s+",3);
                    try
                    {
                        responseCode=0;
                        responseMessage=null;
                        responseCode=Integer.parseInt(firstLine[1]);
                        responseMessage=firstLine[2];
                    } catch(Exception ex){}
    
					if (responseCode < 200 || responseCode >= 300)
                    {
                        response=readResponse(funcname,inP,bytesToRead);
                    }
    
                    String connectionClosed=getHeaderValue(connectResponseHeader,"connection");
                    if(connectionClosed!=null && connectionClosed.equalsIgnoreCase("close"))
                    {
                        XTTProperties.printWarn(funcname + ": Unable to establish a secure connection");
                        closeConnection();
                        return;
                    }
                    else
                    {
                        socket=encryptConnection(funcname, socket, remoteHost,remotePort);
                    }
                } else
                {
                    if(!reusedSocket)
                    {
                        XTTProperties.printDebug(funcname + ": encrypting connection");
                        socket=encryptConnection(funcname, socket, remoteHost,remotePort);
                    } else
                    {
                        XTTProperties.printDebug(funcname + ": reused encrypted connection");
                    }
                }
            }
    
         ////// REQUEST
            BufferedInputStream in = new BufferedInputStream(socket.getInputStream(),readBufferSize);
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(),65536);
            // send headers
            StringBuffer headerView=new StringBuffer();
            HTTPHelper.writeHeader(out,requestHeadersToSend,headerView);
            out.flush();
            XTTProperties.printDebug(funcname + ": written Headers:\n"+headerView);
    
            // send body
            if(isPost)
            {
                out.write(postBody);
                out.flush();
                XTTProperties.printDebug(funcname + ": written PostData:\n"+ConvertLib.getHexView(postBody,0,bufferOutputSize));
            }
    
         ////// RESPONSE
            // read headers
            boolean serverKeepAlive=keepAlive;
            responseHeader=HTTPHelper.readHTTPStreamHeaders(funcname,in);
    
            String firstLineS=getHeaderValue(responseHeader,null);
            if(firstLineS==null)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printFail(funcname + ": INVALID or no headers returned:\n"+responseHeader);
                } else
                {
                    XTTProperties.printFail(funcname + ": INVALID or no headers returned!");
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] firstLine=firstLineS.split("\\s+",3);
            try
            {
                responseCode=0;
                responseMessage=null;
                responseCode=Integer.parseInt(firstLine[1]);
                responseMessage=firstLine[2];
            } catch(Exception ex){}
    
            HTTPHelper.storeHeaders(funcname+": headers stored:",new String[]{module+"/HEADER",module+"/"+name+"/HEADER"},"",responseHeader);
    
            String responseKeepAlive=getHeaderValue(responseHeader,"connection");
            if(responseKeepAlive!=null && responseKeepAlive.equalsIgnoreCase("close"))serverKeepAlive=false;
    
            // read body
            response = new byte[0];
            if(overrideMethod!=null&&overrideMethod.equals("HEAD"))
            {
                XTTProperties.printDebug(funcname + ": HEAD method, skipping reading of body");
            } else if(responseCode!=HTTP_NO_CONTENT&&responseCode!=HTTP_NOT_MODIFIED)
            {
                response=readResponse(funcname,in,bytesToRead);
            }
    
            if(!keepAlive||!serverKeepAlive)
            {
                try{socket.close();} catch (Exception e){}
                socket=null;
            }
    
            if((responseCode==HTTP_MOVED_PERM||responseCode==HTTP_MOVED_TEMP)&&followRedirects)
            {
                redirectCounter++;
                if(redirectCounter<=10)
                {
                    String location=getHeaderValue(responseHeader,"location");
                    if(location.startsWith("http://")||location.startsWith("https://"))
                    {
                        XTTProperties.printDebug(funcname + ": following redirection to: "+location);
                        sendGetRequest(funcname,location,isPost,overrideMethod,null,readBufferSize);
                    } else
                    {
                        String newlocation=HTTPHelper.buildURL(remoteURL.getProtocol()
                            ,remoteURL.getHost()
                            ,remoteURL.getPort()
                            ,location
                            ,remoteURL.getRef()
                            ,remoteURL.getQuery()
                            ,remoteURL.getUserInfo());
                        XTTProperties.printDebug(funcname + ": following redirection to: "+newlocation);
                        sendGetRequest(funcname,newlocation,isPost,overrideMethod,null,readBufferSize);
                    }
                } else
                {
                    throw new IOException("Too many redirects (>10)");
                }
            } else
            {
                redirectCounter=0;
            }
        } catch (java.net.SocketException ex)
        {
            if(closeCalled)
            {
                XTTProperties.printDebug(funcname + ": Socket closed: "+ex.getClass().getName()+": "+ex.getMessage());
                return;
            }
            try{socket.close();} catch (Exception e){}
            socket=null;
            throw ex;
        }
        closeConnection();
    }

    /**
    * just in case the connection has to be closed client side manually.
    */
    public void closeConnection()
    {
        closeCalled=true;
        try{socket.close();} catch (Exception e){}
        socket=null;
    }

    /**
     * Keys happen to be not case insensitive, if I want to be able to override default headers I have to check them manually when
     * adding the override headers.
     */
    private void mergeHeaders(LinkedHashMap<String,String> target,LinkedHashMap<String,String> source)
    {
        Iterator<String> it=source.keySet().iterator();
        String headerKey=null;
        String headerValue=null;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValue = source.get(headerKey);
            if(headerKey!=null)
            {
                if(target.containsKey(headerKey.toLowerCase()))
                {
                    target.remove(headerKey.toLowerCase());
                }
                target.put(headerKey,headerValue);
            }
        }
    }
    private byte[] readResponse(String funcname, BufferedInputStream inS,String bytesToRead) throws IOException
    {
        String contentlength=getHeaderValue(responseHeader,"content-length");
        int length=-1;
        try
        {
            length=Integer.parseInt(contentlength);
        } catch (Exception ex){}
        String contentEncoding=getHeaderValue(responseHeader,"content-encoding");
        String transferEncoding=getHeaderValue(responseHeader,"transfer-encoding");
        boolean isChunked=false;
        if(transferEncoding!=null && transferEncoding.equalsIgnoreCase("chunked"))isChunked=true;
        XTTProperties.printVerbose(funcname+": clength:"+contentlength+" length:"+length);
        byte[] serverresponse=new byte[0];
        if(length>0)
        {
            serverresponse=new byte[length];
        }
        // This is kind of a problem if the transfer is not chunked and no size known how do we even now that there is a body?
        // TODO: Probably check for content-type header too.
        if(length<0&&!isChunked)
        {
            BufferedInputStream in;
            XTTProperties.printVerbose(funcname+": downloading BODY: size Unknown");
            if(contentEncoding!=null && (contentEncoding.equalsIgnoreCase("gzip")||contentEncoding.equalsIgnoreCase("x-gzip")))
            {
                XTTProperties.printInfo(funcname + ": decompressing gzip format");
                in=new BufferedInputStream(new GZIPInputStream(inS));
                XTTProperties.setVariable(module+"/BODY/ENCODING","gzip");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","gzip");
            } else if(contentEncoding!=null && contentEncoding.equalsIgnoreCase("deflate"))
            {
                XTTProperties.printInfo(funcname + ": decompressing deflate format");
                in=new BufferedInputStream(new InflaterInputStream(inS,new Inflater(true)));
                XTTProperties.setVariable(module+"/BODY/ENCODING","deflate");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","deflate");
            } else if(contentEncoding!=null && (contentEncoding.equalsIgnoreCase("compress")||contentEncoding.equalsIgnoreCase("x-compress")))
            {
                XTTProperties.printInfo(funcname + ": decompressing compress(lzw) format");
                in=new BufferedInputStream(new UncompressInputStream(inS));
                XTTProperties.setVariable(module+"/BODY/ENCODING","compress");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","compress");
            } else
            {
                in=inS;
                XTTProperties.setVariable(module+"/BODY/ENCODING","");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","");
            }
            serverresponse=HTTPHelper.readStream(funcname,in);
        // With known content-length or chunked encoded tranfer we can proced without problems
        } 
        else if (length>0||isChunked)
        {
            BufferedInputStream in=inS;
            if(isChunked)
            {
                XTTProperties.printVerbose(funcname+": downloading CHUNKED BODY: ");
                try
                {
                    serverresponse=HTTPHelper.readChunkedBody(funcname,in,responseHeader);
                } 
                catch (Exception e)
                {
                    XTTProperties.printFail(funcname + ": INVALID CHUNKED BODY detected!");
                    IOException ioe=new IOException("INVALID CHUNKED BODY detected");
                    ioe.initCause(e);
                    throw ioe;
                }
            } 
            else
            {
                XTTProperties.printVerbose(funcname+": downloading BODY: "+length+" Bytes");
                int thebyte=0;
				if (bytesToRead != null)
				{
					XTTProperties.printInfo(funcname + ": BytesToRead: " + bytesToRead + " File Length: "
							+ serverresponse.length);
				}
                for(int i=0;i<length;i++)
                {
					if (bytesToRead != null)
					{
						if (Integer.parseInt(bytesToRead) > 0 && Integer.parseInt(bytesToRead) <= new Integer(thebyte).byteValue())
						{
							bytesToRead = null;
							in.close();
							break;
						}
						else
	                	{
	                    thebyte=in.read();
	                    if(thebyte==-1)
	                    	break;
	                    serverresponse[i]=(new Integer(thebyte)).byteValue();
	                	}
					}
                	else
                	{
                    thebyte=in.read();
                    if(thebyte==-1)
                    	break;
                    serverresponse[i]=(new Integer(thebyte)).byteValue();
                	}
                }
            }
            if(contentEncoding!=null && (contentEncoding.equalsIgnoreCase("gzip")||contentEncoding.equalsIgnoreCase("x-gzip")))
            {
                XTTProperties.printInfo(funcname + ": decompressing gzip format");
                serverresponse=HTTPHelper.readStream(funcname+": unpacking",new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(serverresponse))));
                XTTProperties.setVariable(module+"/BODY/ENCODING","gzip");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","gzip");
            } 
            else if(contentEncoding!=null && contentEncoding.equalsIgnoreCase("deflate"))
            {
                XTTProperties.printInfo(funcname + ": decompressing deflate format");
                serverresponse=HTTPHelper.readStream(funcname+": unpacking",new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(serverresponse),new Inflater(true))));
                XTTProperties.setVariable(module+"/BODY/ENCODING","deflate");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","deflate");
            } 
            else if(contentEncoding!=null && (contentEncoding.equalsIgnoreCase("compress")||contentEncoding.equalsIgnoreCase("x-compress")))
            {
                XTTProperties.printInfo(funcname + ": decompressing compress(lzw) format");
                serverresponse=HTTPHelper.readStream(funcname+": unpacking",new BufferedInputStream(new UncompressInputStream(new ByteArrayInputStream(serverresponse))));
                XTTProperties.setVariable(module+"/BODY/ENCODING","compress");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","compress");
            } else
            {
                XTTProperties.setVariable(module+"/BODY/ENCODING","");
                XTTProperties.setVariable(module+"/"+name+"/BODY/ENCODING","");
            }
        // With known content-length=0 there is no need to read a body.
        } 
        else
        {
            XTTProperties.printDebug(funcname + ": Response Body (0 Bytes): content-length="+contentlength);
        }

        if(serverresponse.length<MAXRESPONSESIZE)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printDebug(funcname + ": Response Body ("+serverresponse.length+" Bytes) START:\n"+ConvertLib.getHexView(serverresponse,0,bufferOutputSize)+"\n"+
                                         funcname + ": body stored in "+module+"/BODY/BASE64"+"\n"+
                                         funcname + ": body stored in "+module+"/BODY/PLAIN"+"\n"+
                                         funcname + ": body stored in "+module+"/"+name+"/BODY/BASE64"+"\n"+
                                         funcname + ": body stored in "+module+"/"+name+"/BODY/PLAIN"+"\n"+
                                         funcname + ": Response Body END:");
            } else
            {
                XTTProperties.printVerbose(funcname + ": Response Body ("+serverresponse.length+" Bytes) START:\n"+
                                           funcname + ": body stored in "+module+"/BODY/BASE64"+"\n"+
                                           funcname + ": body stored in "+module+"/BODY/PLAIN"+"\n"+
                                           funcname + ": body stored in "+module+"/"+name+"/BODY/BASE64"+"\n"+
                                           funcname + ": body stored in "+module+"/"+name+"/BODY/PLAIN"+"\n"+
                                           funcname + ": Response Body END:");
            }
            XTTProperties.setVariable(module+"/BODY/BASE64",ConvertLib.base64Encode(serverresponse));
            XTTProperties.setVariable(module+"/BODY/PLAIN",ConvertLib.createString(serverresponse));
            XTTProperties.setVariable(module+"/"+name+"/BODY/BASE64",ConvertLib.base64Encode(serverresponse));
            XTTProperties.setVariable(module+"/"+name+"/BODY/PLAIN",ConvertLib.createString(serverresponse));
        } else
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printDebug(funcname + ": Response Body ("+serverresponse.length+" Bytes) START:\n"+ConvertLib.getHexView(serverresponse,0,bufferOutputSize)+"\n"+
                                         funcname + ": body not stored because of massive size!"+"\n"+
                                         funcname + ": Response Body END:");
            } else
            {
                XTTProperties.printVerbose(funcname + ": Response Body ("+serverresponse.length+" Bytes) START:\n"+
                                           funcname + ": body not stored because of massive size!"+"\n"+
                                           funcname + ": Response Body END:");
            }
        }
        return serverresponse;
    }

    // Fiddle out the first value of a header. Multiple headers should not be there except perhaps for cookies.
    private String getHeaderValue(LinkedHashMap<String,Vector<String>> headers, String key)
    {
        Vector<String> vals=headers.get(key);
        if(vals==null)return null;
        return vals.get(0);
    }

    /**
     * Create a POST body, either from the provided byte array or as application/x-www-form-urlencoded or if the conntent type is set as multipart/form-data
     */
    private byte[] getPostBody(LinkedHashMap<String,String> requestHeadersToSend,LinkedHashMap<String,String> requestHeaders, boolean isChunked) throws IOException
    {
        byte[] requestPostData=null;
        String defaultContentType=null;
        //application/octet-stream
        if(postDataBytes!=null)
        {
            defaultContentType="application/octet-stream";
        } else
        {
            defaultContentType="application/x-www-form-urlencoded";
        }
        Iterator<String> it=requestHeaders.keySet().iterator();
        String headerKey;
        String headerValue;
        String boundary="--XTT0000000"+System.currentTimeMillis();
        boolean isMultipartPost=false;
        boolean foundContentType=false;
        while(it.hasNext())
        {
            headerKey=it.next();
            headerValue=requestHeaders.get(headerKey);
            if(headerKey!=null&&headerKey.toLowerCase().equals("content-type"))
            {
                foundContentType=true;
                if(headerValue.toLowerCase().equals("multipart/form-data"))
                {
                    headerValue="multipart/form-data; boundary="+boundary;
                    isMultipartPost=true;
                    requestHeaders.remove(headerKey);
                    requestHeadersToSend.put("content-type",headerValue);
                }
            }
        }
        if(!foundContentType)
        {
            requestHeadersToSend.put("content-type",defaultContentType);
        }

        if(postDataBytes==null)
        {
            String dataKey=null;
            String dataValue=null;
            StringBuffer postDataBuffer=new StringBuffer("");
            if(!isMultipartPost)
            {
                it=postData.keySet().iterator();
                String divider="";
                dataKey=null;
                while(it.hasNext())
                {
                    dataKey=(String)it.next();
                    try
                    {
                        postDataBuffer.append(divider+dataKey+"="+URLEncoder.encode(postData.get(dataKey),XTTProperties.getCharSet()));
                    } catch(java.io.UnsupportedEncodingException uee)
                    {
                        XTTProperties.printFail("Unsupported charset: "+XTTProperties.getCharSet());
                        if(XTTProperties.printDebug(null))
                        {
                            XTTProperties.printException(uee);
                        }
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return null;
                    }
                    divider="&";
                }
            } else
            {
                it=postData.keySet().iterator();
                dataKey=null;
                dataValue=null;
                while(it.hasNext())
                {
                    dataKey=it.next();
                    dataValue=postData.get(dataKey);
                    postDataBuffer.append("--"+boundary+sCRLF);
                    postDataBuffer.append("Content-Disposition: form-data; name=\""+dataKey+"\""+sCRLF);
                    postDataBuffer.append(sCRLF);
                    postDataBuffer.append(dataValue+sCRLF);
                }
                postDataBuffer.append("--"+boundary+"--"+sCRLF);
            }
            requestPostData=ConvertLib.createBytes(postDataBuffer.toString());
        } else
        {
            requestPostData=postDataBytes;
        }
        if(isChunked)
        {
            return HTTPHelper.createChunkedBody(requestPostData,transferChunkSize);
        } else
        {
            requestHeadersToSend.put("content-length",requestPostData.length+"");
            return requestPostData;
        }
    }

    /**
     * This will create a secure socket out of a normal socket.
     */
    private Socket encryptConnection(String funcname, Socket socket, String targetHost,int targetPort)
    {
        try
        {
            SSLSocketFactory sf=null;
            if(enableCertcheck)
            {
                SSLContext sslc = SSLContext.getInstance("SSL");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                KeyStore keystore = KeyStore.getInstance("JKS");
                keystore.load(new FileInputStream("key/xttkeystore"), KEYSTOREPW);
                kmf.init(keystore, KEYSTOREPW);

                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                tmf.init(keystore);

                sslc.init(kmf.getKeyManagers(),tmf.getTrustManagers(), new java.security.SecureRandom());
                sf=sslc.getSocketFactory();
            } else
            {
                TrustManager[] trustAllCerts = new TrustManager[]
                {
                    new X509TrustManager()
                };
                SSLContext sslc = SSLContext.getInstance("SSL");
                sslc.init(null, trustAllCerts, new java.security.SecureRandom());
                sf=sslc.getSocketFactory();
            }

            Socket ss=sf.createSocket(socket,targetHost,targetPort, false);

            javax.net.ssl.SSLSocket sss=(javax.net.ssl.SSLSocket)ss;

            certs = new java.security.cert.Certificate[0];
            certs = sss.getSession().getPeerCertificates();
            XTTProperties.printInfo(funcname+": HTTPS: "+certs.length+" certificates");
            java.security.cert.X509Certificate x509=null;
            for(int i=0;i<certs.length;i++)
            {
                XTTProperties.printVerbose(funcname+": Certificate["+i+"]: "+certs[i].getType());
                if(certs[i].getType().equals("X.509"))
                {
                    x509=(java.security.cert.X509Certificate)certs[i];
                    XTTProperties.printDebug(  funcname+": "+i+":serial: "+x509.getSerialNumber() );
                    XTTProperties.printVerbose(funcname+": "+i+":algorithm: "+x509.getSigAlgName());
                    XTTProperties.printVerbose(funcname+": "+i+":X500Principal: "+x509.getIssuerX500Principal().getName());
                    XTTProperties.printDebug(  funcname+": "+i+":DN: "+x509.getSubjectDN());
                    XTTProperties.printVerbose(funcname+": "+i+":NotAfter: " + x509.getNotAfter());
                }
            }
            XTTProperties.printVerbose(funcname+": HTTPS: END certificates");

            return ss;
        } catch(Exception ex)
        {
            XTTProperties.printFail("Exception: "+ex.getClass().getName()+": "+ex.getMessage());
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ex);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return socket;
        }

    }

    private class X509TrustManager implements javax.net.ssl.X509TrustManager
    {
        public java.security.cert.X509Certificate[] getAcceptedIssuers()
        {
            return new java.security.cert.X509Certificate[0];
        }
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
    }

    private char[] KEYSTOREPW = "xttxtt".toCharArray();

    /**
     * Reads the configuration from the properties. Values are stored under [MODULE]/[VALUE]
     */
    public void readConfiguration()
    {
        boolean followRedirects          = true;
        boolean keepAlive                = true;
        boolean enableCertcheck          = false;
        boolean enableURLContract        = false;
        String protocolVersion           = "1.1";
        String httpProxyPort             = null;
        String httpProxyHost             = null;
        int connectionTimeout            = 30000;
        int bufferOutputSize             = -1;

        if(XTTProperties.getProperty(module+"/ENABLECERTCHECK").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling Java default Certificate Trust Manager");
            enableCertcheck=false;
        }
        if(!XTTProperties.getProperty(module+"/ENABLEURLCONTRACTION").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Enabling URL contraction");
            enableURLContract=true;
        }
        if(!XTTProperties.getProperty(module+"/DISABLEKEEPALIVE").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling Keep-Alive");
            keepAlive=false;
        }
        if(!XTTProperties.getProperty(module+"/DISABLEFOLLOWREDIRECTS").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling follow redirects");
            followRedirects=false;
        }

        String ip=XTTProperties.getProperty(module+"/PROXYIP");
        String port=XTTProperties.getProperty(module+"/PROXYPORT");
        if(ip.equals("null")||port.equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": no proxy set, direct mode");
            httpProxyPort = null;
            httpProxyHost = null;
        } else
        {
            try
            {
                httpProxyHost = ip;
                httpProxyPort = port;
                this.setProxy(httpProxyHost,httpProxyPort);
                XTTProperties.printVerbose(this.getClass().getName()+": setting proxy to: "+ip+":"+port);
            } catch (Exception uhe)
            {
                XTTProperties.printWarn(this.getClass().getName()+": Unable to set Proxy: "+uhe.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(uhe);
                }
            }
        }

        connectionTimeout=XTTProperties.getIntProperty(module+"/TIMEOUT");
        if(connectionTimeout<=0)connectionTimeout=30000;

        bufferOutputSize=XTTProperties.getIntProperty("BUFFEROUTPUTSIZE");

        this.setFollowRedirects(followRedirects);
        this.setProtocolVersion(protocolVersion);
        this.setKeepAlive(keepAlive);
        this.setEnableCertcheck(enableCertcheck);
        this.setEnableURLContract(enableURLContract);
        this.setTimeout(connectionTimeout);
        this.setBufferOutputSize(bufferOutputSize);

    }

    /**
     * Create a connection by reusing the configuration of this connection.
     * Handy if you have a default connection and want to use its configuration as base.
     */
    public HTTPConnection createConnection(String newname)
    {
        HTTPConnection newConnection=new HTTPConnection(newname,module);
        newConnection.setFollowRedirects(followRedirects);
        newConnection.setProtocolVersion(protocolVersion);
        newConnection.setKeepAlive(keepAlive);
        newConnection.setEnableCertcheck(enableCertcheck);
        newConnection.setEnableURLContract(enableURLContract);
        newConnection.setTimeout(timeout);
        newConnection.httpProxyHost=httpProxyHost;
        newConnection.httpProxyPort=httpProxyPort;
        return newConnection;
    }

    public static String getConfigurationOptions()
    {
        return ""
        +"\n        <!-- timeout on client connections to the webserver/proxy -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- the proxy ip address for HTTP connections-->"
        +"\n        <!--ProxyIp>127.0.0.1</ProxyIp-->"
        +"\n        <!-- the http proxy port -->"
        +"\n        <!--ProxyPort>9000</ProxyPort-->"
        +"\n        <!-- enable certificate check on https connections -->"
        +"\n        <!--enableCertCheck>/-->"
        +"\n        <!-- shortens URLs from 'http://server.com/path' to '/path' in the request -->"
        +"\n        <!--enableURLContraction>/-->"
        +"\n        <!-- turn of keep-alive to the webserver -->"
        +"\n        <!--disableKeepAlive/-->"
        +"\n        <!-- disable automatically following a redirect 301/302 -->"
        +"\n        <!--disableFollowRedirects/-->"
        +"";
    }

    public static final String tantau_sccsid = "@(#)$Id: HTTPConnection.java,v 1.20 2010/03/18 05:30:39 rajesh Exp $";
}

