package com.mobixell.xtt;

import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyStore;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * This class is a helper class for <code>FunctionModule_MSRP</code> and is used to construct MSRP send requests.
 *
 * @author Anil Wadhai
 * @version $Id: MSRPConnection.java,v 1.5 2010/03/18 05:30:39 rajesh Exp $
 */

public class MSRPConnection implements MSRPConstants
{
    public final static int DEFAULT_MSRP_PORT   = 493;
    public final static int DEFAULT_MSRPS_PORT  = 8493;
    private String          name                = null;
    private String          module              = "MSRP";
    private String          transactionId       = null;
    private int             port                = -1;
    private String          msrpProxyHost       = null;
    private int             msrpProxyPort       = -1;
    private int             autoTransactionID   = 0;
    private boolean         isAutoTransactionID = true;
    private boolean         enableChunking      = true;
    private boolean         isCreateTCPSocket   = false;
    boolean                 reusedSocket        = false;
    private Socket          socket              = null;
    private boolean         enableCertcheck     = false;
    private char[]          KEYSTOREPW          = "xttxtt".toCharArray();
   
   
    private java.security.cert.Certificate[] certs      = new java.security.cert.Certificate[0];
    private LinkedHashMap<String, String> requestHeader = new LinkedHashMap<String, String>();

    public void setEnableCertcheck(boolean enableCertcheck){this.enableCertcheck=enableCertcheck;}
    public void setIsAutoTransactionID(boolean isAutoTransactionID){this.isAutoTransactionID=isAutoTransactionID;}
    public void setEnableChunking(boolean enableChunking){this.enableChunking=enableChunking;}
    
    public LinkedHashMap<String, String> getRequestHeader()
    {
        return requestHeader;
    }

    private LinkedHashMap<String, Vector<String>> responseHeader = new LinkedHashMap<String, Vector<String>>();

    public void setResponseHeader(LinkedHashMap<String, Vector<String>> responseHeader)
    {
        this.responseHeader = responseHeader;
    }

    public LinkedHashMap<String, Vector<String>> getResponseHeader()
    {
        return responseHeader;
    }

    public String getName()
    {
        return name;
    }

    public String getModule()
    {
        return module;
    }

    public void setProxy(String proxyHost, String proxyPort) throws java.net.UnknownHostException
    {
        if(proxyHost==null||proxyPort==null)
        {
            this.msrpProxyHost = null;
            this.msrpProxyPort = -1;
            return;
        }
        DNSServer.resolveAddressToInetAddress(proxyHost);
        int port = Integer.decode(proxyPort);
        if(port<0||port>65535)
            throw new java.lang.NumberFormatException("Out of Range");
        this.msrpProxyHost = proxyHost;
        this.msrpProxyPort = port;
    }

    private int timeout = 30000;

    public void setTimeout(int timeout)
    {
        this.timeout = timeout;
    }

    public MSRPConnection(String name, String module)
    {
        this.name = name;
        this.module = module.toUpperCase();
    }
    
    public MSRPConnection(){}
    

     /**
     * Used to send a POST request to an URL
     */
    private void sendRequest(String funcname, String remoteHost, int remotePort, String protocol, String headers, byte[] body) throws IOException
    {
        byte[] msrpBody = new byte[0];
        if(body!=null&&body.length>0){
            msrpBody = body;
        } else
        {
            headers=headers.replaceFirst(LINESEPARATOR+LINESEPARATOR, LINESEPARATOR);
            headers=headers.concat(SEVENHYPHEN+transactionId+DOLLER+LINESEPARATOR);   
        }
        byte[] msrpHeader = ConvertLib.createBytes(headers);
            
        if(protocol.equalsIgnoreCase("TCP"))
        {
            sendTCPRequest(funcname, remoteHost, remotePort, msrpHeader, msrpBody);
        } else if(protocol.equalsIgnoreCase("SSL"))
        {
            sendSSLRequest(funcname, remoteHost, remotePort,msrpHeader, msrpBody);
        } else
        {
            throw new java.net.ProtocolException("Unsupported protocol: "+protocol);
        }
    }

    public void sendRequest(String funcname, int remotePort,String remoteHost, String msrpMethod, String headers, byte[] body, String transactionId, String boundary) throws IOException
    {
        // CREATE HEADERS
        LinkedHashMap<String, String> requestHeadersToSend = new LinkedHashMap<String, String>();
       
        if(isAutoTransactionID || transactionId==null)
        {
            autoTransactionID++;
            DecimalFormat df = new DecimalFormat("0000");
            transactionId    ="XTT"+df.format(new Double(autoTransactionID));
            boundary         = SEVENHYPHEN+transactionId+DOLLER;
        }
            msrpMethod =MSRP+WHITESPACE+transactionId+WHITESPACE+msrpMethod;
        
            String useProtocol    = "TCP";
            requestHeadersToSend.put(null, msrpMethod);
            mergeHeaders(requestHeadersToSend, headers);
            sendRequest(funcname, remoteHost, remotePort, useProtocol, body, requestHeadersToSend, transactionId,boundary);
    }

    public void sendMSRPRequest(String funcname, String msrpMethod, byte[] body,String transactionId) throws IOException, URISyntaxException
    {
        String  surl         = requestHeader.get(TOPATH);
        
        String  remoteHost   = null;
        int     remotePort   = -1;

        //Handle IPv6 addresses - append [ & ] if To-PATH doesn't contain - this is important so that URL class accepts proper URLs
        //the current code assumes that port always comes in the URL
        String hostAddress = surl.split("/")[2];
        if(surl.split(":").length > 3  && !(surl.contains("[")||surl.contains("]")) && hostAddress.contains(":")&& hostAddress.split(":").length>2)
        {
            String ipPort = surl.split("/")[2];   
            int lastIndexOfColon = ipPort.lastIndexOf(":");
            remoteHost =ipPort.substring(0, lastIndexOfColon); // get only IPv6 from URI
            remotePort = Integer.decode(ipPort.substring(lastIndexOfColon+1, ipPort.length())); // get only port from URI
        }
        else
        {
            URI     remoteURL    = new URI(surl);
            remoteHost   = remoteURL.getHost(); 
            remotePort   = remoteURL.getPort();
        }

        String  useProtocol  = "TCP";
        String  host         = remoteHost;
        int     port         = remotePort;
        boolean isLoopback   = false;
      
        try
        {
            InetAddress i = DNSServer.resolveAddressToInetAddress(host);
            isLoopback = i.isLoopbackAddress();
        } catch (Exception lle){}

        boolean isProxyConnection = false;
        if(msrpProxyHost !=null&& msrpProxyPort !=-1&&!isLoopback)
        {
            host = msrpProxyHost;
            port = msrpProxyPort;
            isProxyConnection = true;
        }
        XTTProperties.printInfo(funcname+": proxy="+isProxyConnection+" destination: "+surl);

        ////// CREATE HEADERS
        LinkedHashMap<String, String> requestHeadersToSend = new LinkedHashMap<String, String>();

        // updating trancationID 
        if(isAutoTransactionID || transactionId==null)
        {
            autoTransactionID++;
            DecimalFormat df = new DecimalFormat("0000");
            transactionId    ="XTT"+df.format(new Double(autoTransactionID));
        }
            msrpMethod =MSRP+WHITESPACE+transactionId+WHITESPACE+msrpMethod;
            requestHeadersToSend.put(null, msrpMethod);
            mergeHeaders(requestHeadersToSend, requestHeader);
            sendRequest(funcname, host, port, useProtocol, body, requestHeadersToSend, transactionId,null);
    }

    public void setSocket(Socket skt, boolean var)
    {
        this.socket = skt;
        this.isCreateTCPSocket =var;
    }

    private void sendTCPRequest(String funcname, String remoteHost, int remotePort, byte[] msrpHeader, byte[] msrpBody, boolean isSSL) throws IOException
    {

        if(socket==null||socket.isClosed()||!socket.isConnected())
        {
            socket = new Socket(remoteHost, remotePort);
            XTTProperties.printVerbose(funcname+": opened "+socket);
        }
        // the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
        socket.setTcpNoDelay(true);
        socket.setSoTimeout(timeout);
        
        if(isSSL)
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

        if(!isCreateTCPSocket&&!reusedSocket)
        {
            socket = MSRPServer.createSocketTCP(""+port, socket);
            reusedSocket = true;
        }

        synchronized (socket)
        {
            BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 65536);
            out.write(msrpHeader);
            out.flush();
            XTTProperties.printDebug(funcname+": written Headers:\n"+ConvertLib.createString(msrpHeader));

            // send body
            if(msrpBody!=null&&msrpBody.length>0)
            {
                out.write(new byte[]{0x0D,0x0A});
                out.write(msrpBody);
                out.flush();
                XTTProperties.printDebug(funcname+": written Body:\n"+ConvertLib.getHexView(msrpBody, 0, msrpBody.length));
            }
        }
    }

    /**
     * Reads the configuration from the properties. Values are stored under
     * [MODULE]/[VALUE]
     */
    public void readConfiguration()
    {
        boolean enableCertcheck          = false;
        String httpProxyPort             = null;
        String httpProxyHost             = null;
        
        int connectionTimeout = XTTProperties.getIntProperty(module+"SERVER/TIMEOUT");
        if(connectionTimeout<=0)connectionTimeout = 30000;
        this.setTimeout(connectionTimeout);
        int pport = XTTProperties.getIntProperty(module+"SERVER/PORT");
        if(pport<=0)pport = DEFAULT_MSRP_PORT;
        this.setPorts(pport+"");
       
        if(!XTTProperties.getProperty(module+"/ENABLECERTCHECK").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling Java default Certificate Trust Manager");
            enableCertcheck=false;
        }
        if(XTTProperties.getProperty(module+"/DISABLEAUTOMATICTRANSACTIONID").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling automatic TransactionId generation");
            this.isAutoTransactionID=false;
        }
        
        if(!XTTProperties.getProperty(module+"/ENABLECHUNKING").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling chunking for sendFreeRequest");
            this.enableChunking=false;
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
        
        this.setEnableCertcheck(enableCertcheck);
       
    }

    public void setPorts(String sport)
    {
        this.port = Integer.parseInt(sport);
    }

    /**
     * Create a new connection with the given connection name and set the parameters<br>
     * like port, timout, enableCertcheck, isAutoTransactionID etc in created connection and return the same.
     *
     * @param newname
     * @param port
     * @return newConnection
     */
    public MSRPConnection createConnection(String newname, String port)
    {
        MSRPConnection newConnection = new MSRPConnection(newname, module);
        newConnection.setPorts(port);
        newConnection.setTimeout(timeout);
        newConnection.setEnableCertcheck(enableCertcheck);
        newConnection.setIsAutoTransactionID(isAutoTransactionID);
        newConnection.setEnableChunking(enableChunking);
        newConnection.msrpProxyHost =msrpProxyHost;
        newConnection.msrpProxyPort =msrpProxyPort;
        return newConnection;
    }

    /**
     * Just in case the connection has to be closed client side manually.
     */
    public void closeConnection()
    {
        try
        {
            socket.close();
        } catch (Exception e)
        {
        }
        socket = null;
    }

    public static MSRPConnection getConnection(Map<String, MSRPConnection> connections, String parameters[])
    {
        MSRPConnection connection = connections.get(parameters[1].toLowerCase());
        if(connection!=null)
        {
            parameters[0] = parameters[0]+"("+connection.getName()+")";
        }
        return connection;
        
    }

    private void sendTCPRequest(String funcname, String remoteHost, int remotePort, byte[] msrpHeader, byte[] msrpBody) throws IOException
    {
        sendTCPRequest(funcname, remoteHost, remotePort, msrpHeader, msrpBody, false);
    }
    
    private void sendSSLRequest(String funcname, String remoteHost, int remotePort, byte[] msrpHeader, byte[] msrpBody) throws IOException
    {
        sendTCPRequest(funcname,remoteHost,remotePort,msrpHeader,msrpBody,true);
    }

    public static String getConfigurationOptions()
    {
        return ""
        +"\n        <!-- timeout on client tcp connections to the msrp server -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- turn off the auto generation of transaction id -->"
        +"\n        <!-- disableAutomaticTransactionId/ -->"
        +"\n        <!-- enable the default java certificate check -->"
        +"\n        <!-- enableCertCheck>true</enableCertCheck -->"
        +"\n        <!-- enable the chunking -->"
        +"\n        <!-- enableChunking/ -->"
        +"";
    }

    /**
     * Keys happen to be not case insensitive, if I want to be able to override default headers I have to check them manually when
     * adding the override headers.
     */
    public static void mergeHeaders(LinkedHashMap<String, String> target, LinkedHashMap<String, String> source)
    {
        Iterator<String> it = source.keySet().iterator();
        String headerKey = null;
        String headerValue = null;
        while (it.hasNext())
        {
            headerKey = it.next();
            headerValue = source.get(headerKey);
            if (headerKey != null)
            {
                if (target.containsKey(headerKey.toLowerCase()))
                {
                    target.remove(headerKey.toLowerCase());
                }
                target.put(headerKey, headerValue);
            }
        }
    }
    
    /**
     * Keys happen to be not case insensitive, if I want to be able to override default headers I have to check them manually when
     * adding the override headers.
     */
    private static void mergeHeaders(LinkedHashMap<String, String> target, String source)
    {

        String currentHeader[] = null;
        String[] crlf_spit = source.split("\r\n|\r|\n");
        for (int i = 0; i<crlf_spit.length; i++)
        {
            if(crlf_spit[i]==null||crlf_spit[i].equals(""))
                break;
            currentHeader = crlf_spit[i].split(":", 2);
            if(!crlf_spit[i].matches("^\\s.*")&&currentHeader.length==2)
            {
                if(target.containsKey(currentHeader[0].toLowerCase()))
                {
                    target.remove(currentHeader[0].toLowerCase());
                }
                target.put(currentHeader[0], currentHeader[1]);
            }
        }
    }

    public static void setHeader(MSRPConnection connection, String parameters[])
    {
            int offset = 1;
            if (parameters.length == 3 + offset && !parameters[2 + offset].equals("null"))
            {
                XTTProperties.printInfo(parameters[0] + ": setting HeaderField " + parameters[1 + offset] + " to: " + parameters[2 + offset]);
                // Actually set the Header Key and Value
          /*      if((parameters[1 + offset].toLowerCase().equals("from-path")||parameters[1 + offset].toLowerCase().equals("to-path") ) && parameters[2 + offset].split(":").length >3 )
                {
                    //e.g. URI is msrp://2001::21e:8cff:c0a9:fc2a:849/iau39soe2843z;tcp
                    String ipPort = parameters[2 + offset].split("/")[2]; 
                    int lastIndexOfColon = ipPort.lastIndexOf(":");
                    String ip =ipPort.substring(0, lastIndexOfColon); // get only IPv6 from URI
                    parameters[2 + offset] = parameters[2 + offset].replace(ip, "["+ip+"]");
                    connection.getRequestHeader().put(parameters[1 + offset], parameters[2 + offset]);   
                }
                else
                {*/
                    connection.getRequestHeader().put(parameters[1 + offset], parameters[2 + offset]);    
              //  }
            } else
            {
                XTTProperties.printInfo(parameters[0] + ": removing HeaderField " + parameters[1 + offset]);
                // Actually remove the Header Key and Value
                connection.getRequestHeader().remove(parameters[1 + offset]);
            }
        
    }
    
    private void sendRequest(String funcname, String remoteHost, int remotePort, String useProtocol, byte[] body,LinkedHashMap<String, String> requestHeadersToSend, String id, String boundary )throws IOException
    {
        String byteRange      = requestHeadersToSend.get(BYTERANGE);
        transactionId         = id;
        int noOfBytesPerChunk = 2048; //Size of the msg in each chunk. 
        int numOfChunks       = 1;
        int bodyTotalLength   = 0;
        byte[] chunkedBody    = null;
        
        if(body!=null&&body.length>0)
        {
            bodyTotalLength = body.length;
        }
        if(requestHeadersToSend.get(TOPATH).startsWith("msrps"))
        {
            useProtocol = "SSL";
        }

        // if byterange header is set from testcase then only set
        if(byteRange!=null&&body.length>0)
        {
            String[] totalByteRange = byteRange.split(SINGLEHYPHEN);
            String[] bytesRange     = totalByteRange[1].split(SLASH);
            if(!bytesRange[0].equals(ASTERISK))
            {
                noOfBytesPerChunk = Integer.parseInt(bytesRange[0]);
            } else if(!bytesRange[1].equals(ASTERISK))
            {
                noOfBytesPerChunk = Integer.parseInt(bytesRange[1]);
            } else
            {
            	noOfBytesPerChunk = bodyTotalLength;
            }
            if(noOfBytesPerChunk != 0)
            {
            	numOfChunks = (int) Math.ceil((double) bodyTotalLength/noOfBytesPerChunk);
          	}
            
        } else if(bodyTotalLength>noOfBytesPerChunk) // body length > 2048
        {
            numOfChunks = (int) Math.ceil((double) bodyTotalLength/noOfBytesPerChunk);
        } else
        {
            byteRange = 1+SINGLEHYPHEN+ASTERISK+SLASH+bodyTotalLength; //i.e. 1-*/totallength
        }

        if(bodyTotalLength>noOfBytesPerChunk&&numOfChunks>1&&enableChunking)
        {
            int startChunkLength   = 1;
            int endByteRangeLength = noOfBytesPerChunk;
            int endChunkLength     = noOfBytesPerChunk;
            for (int i = 1; i<=numOfChunks; i++)
            {
                // methodArray[1] to get transactionId
                boundary           = SEVENHYPHEN+transactionId+PLUS;
                chunkedBody        = ConvertLib.subByteArray(body, startChunkLength-1, endChunkLength);
                byteRange          = (startChunkLength)+SINGLEHYPHEN+endByteRangeLength+SLASH+bodyTotalLength;
                startChunkLength   = startChunkLength+noOfBytesPerChunk;
                endByteRangeLength = endByteRangeLength+noOfBytesPerChunk;

                if((numOfChunks-1)==i)
                {
                    endChunkLength     = bodyTotalLength;
                    endByteRangeLength = bodyTotalLength;
                }
                //last chunked ended by $
                if(numOfChunks==i)
                {
                    boundary = (SEVENHYPHEN+transactionId+DOLLER);
                }

                boundary    = LINESEPARATOR+boundary+LINESEPARATOR;
                chunkedBody = ConvertLib.concatByteArray(chunkedBody, boundary.getBytes());

                requestHeadersToSend.put(BYTERANGE, byteRange);

                StringOutputStream sheaders     = new StringOutputStream();
                BufferedOutputStream bosheaders = new BufferedOutputStream(sheaders);
                StringBuffer headerView         = new StringBuffer();
                HTTPHelper.writeHeader(bosheaders, requestHeadersToSend, headerView,false);
                bosheaders.flush();
                bosheaders.close();

                sendRequest(funcname, remoteHost, remotePort, useProtocol, sheaders.toString(), chunkedBody);
            }
        } else
        {
            if(body.length!=0 && !enableChunking)
            {
                boundary = LINESEPARATOR+SEVENHYPHEN+transactionId+DOLLER+LINESEPARATOR;
                body     = ConvertLib.concatByteArray(body, boundary.getBytes());
                requestHeadersToSend.put(BYTERANGE, byteRange);
            } else if(body.length!=0)
            {
                if(boundary == null)
                {
                    boundary =    SEVENHYPHEN+transactionId+DOLLER;
                }
                boundary = LINESEPARATOR+boundary+LINESEPARATOR;
                body     = ConvertLib.concatByteArray(body, boundary.getBytes());
                requestHeadersToSend.put(BYTERANGE, byteRange);
            }
            
            StringOutputStream sheaders     = new StringOutputStream();
            BufferedOutputStream bosheaders = new BufferedOutputStream(sheaders);
            StringBuffer headerView         = new StringBuffer();
            HTTPHelper.writeHeader(bosheaders, requestHeadersToSend, headerView, false);
            bosheaders.flush();
            bosheaders.close();
            sendRequest(funcname, remoteHost, remotePort, useProtocol, sheaders.toString(), body);
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
                SSLContext        sslc     = SSLContext.getInstance("SSL");
                KeyManagerFactory kmf      = KeyManagerFactory.getInstance("SunX509");
                KeyStore          keystore = KeyStore.getInstance("JKS");
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

            Socket                  ss  = sf.createSocket(socket,targetHost,targetPort, false);
            javax.net.ssl.SSLSocket sss = (javax.net.ssl.SSLSocket)ss;

            certs = new java.security.cert.Certificate[0];
            certs = sss.getSession().getPeerCertificates();
            XTTProperties.printInfo(funcname+": MSRPS: "+certs.length+" certificates");
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
            XTTProperties.printVerbose(funcname+": MSRPS: END certificates");
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
     
}
