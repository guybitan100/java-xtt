package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.io.BufferedOutputStream;
import java.io.IOException;
import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


/**
 * This class has been written as a replacement of javas rtsp class. It should do most of the job at the momment,
 * including following a 301/302 redirect.
 *
 * @author      Roger Soder
 * @version     $Id: RTSPConnection.java,v 1.3 2010/03/18 05:30:40 rajesh Exp $
 */
public class RTSPConnection implements RTSPConstants
{
    public final static int DEFAULT_RTSP_PORT  = 554;
    public final static int DEFAULT_RTSPS_PORT =5554;
    public final static int MAXRESPONSESIZE = 1000000;

    private static final byte[] CRLF = {0x0D,0x0A};
    private static final String sCRLF = "\r\n";

    public RTSPConnection(String name, String module)
    {
        this.name=name;
        this.module=module.toUpperCase();
    }
    private String name = null;
    public String getName(){return name;}
    private String module = "RTSP";
    public String getModule(){return module;}

    //Configuration//////////////////////////////////////////////////////////////////////////////////////////////
    private int port = -1;

    private String protocolVersion           = "1.0";

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

    boolean useDate=true;
    boolean automaticCSEQ=true;
    private int cseq                = 1;
    public void setCSeq(int cseq){this.cseq=cseq;}
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private java.security.cert.Certificate[] certs= new java.security.cert.Certificate[0];

    private LinkedHashMap<String,String> requestHeader  = new LinkedHashMap<String,String>();
    public LinkedHashMap<String,String> getRequestHeader(){return requestHeader;}

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Used to send a POST request to an URL
     */
    public void sendRequest(String funcname, String remoteHost, int remotePort, String protocol, String headers, String base64body) throws IOException
    {
        byte[] rtspHeader=ConvertLib.createBytes(headers);
        byte[] rtspBody=new byte[0];
        if(base64body!=null&&!base64body.equals(""))rtspBody=ConvertLib.base64Decode(base64body);//ConvertLib.createBytes(body);
        if(protocol.equalsIgnoreCase("UDP"))
        {
            sendUDPRequest(funcname, remoteHost, remotePort,rtspHeader, rtspBody);
        } else if(protocol.equalsIgnoreCase("TCP"))
        {
            sendTCPRequest(funcname, remoteHost, remotePort,rtspHeader, rtspBody);
        } else if(protocol.equalsIgnoreCase("SSL"))
        {
            sendSSLRequest(funcname, remoteHost, remotePort,rtspHeader, rtspBody);
        } else
        {
            throw new java.net.ProtocolException("Unsupported protocol: "+protocol);
        }

    }

    public void sendRTSPRequest(String funcname, String rtspMethod, String surl, String base64body) throws IOException, URISyntaxException
    {
    	URI remoteURL = null;
		String remoteHost = null;
		int remotePort = -1;

        //Handle IPv6 addresses - append [ & ] if URL doesn't contain - this is important so that URL class accepts proper URLs
        //the current code assumes that port always comes in the URL
        String hostAddress = surl.split("/")[2];
        if(surl.split(":").length > 3  && !(surl.contains("[")||surl.contains("]")) && hostAddress.contains(":")&& hostAddress.split(":").length>2)
        {
            String ipPort = surl.split("/")[2];
			int lastIndexOfColon = ipPort.lastIndexOf(":");
			remoteHost = ipPort.substring(0, lastIndexOfColon);
			remotePort = Integer.decode(ipPort.substring(lastIndexOfColon + 1, ipPort.length()));
			surl = surl.replace(remoteHost, "[" + remoteHost + "]");
			remoteURL = new URI(surl);
		} else
        {
			remoteURL = new URI(surl);
			remoteHost = remoteURL.getHost();
			remotePort = remoteURL.getPort();
		}
    	
        String useProtocol="TCP";
        if(remoteURL.getScheme().equals("rtspu"))
        {
            useProtocol="UDP";
        }

        String host=remoteHost;
        int port=remotePort;

        boolean isLoopback=false;
        try
        {
            InetAddress i=DNSServer.resolveAddressToInetAddress(host);
            isLoopback=i.isLoopbackAddress();
        } catch(Exception lle)
        {}

        boolean isProxyConnection=false;
        if(httpProxyHost!=null&&httpProxyPort!=-1&&!isLoopback)
        {
            host=httpProxyHost;
            port=httpProxyPort;
            isProxyConnection=true;
        }
        XTTProperties.printInfo(funcname + ": proxy="+isProxyConnection + " destination: "+surl);

     ////// CREATE HEADERS
        LinkedHashMap<String,String> requestHeadersToSend=new LinkedHashMap<String,String>();

        // REQUEST
        requestHeadersToSend.put(null,rtspMethod+" "+surl+" RTSP/"+protocolVersion);
        // CSEQ
        if(automaticCSEQ)
        {
            requestHeadersToSend.put("cseq",""+(cseq++));
        }
        // DATE
        if(useDate)
        {
            requestHeadersToSend.put("date",HTTPHelper.createHTTPDate());
        }
        //USER-AGENT
        requestHeadersToSend.put("user-agent","XTT/"+XTTProperties.getXTTBuildVersion()
            +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
            +System.getProperties().getProperty("os.name")+" "
            +System.getProperties().getProperty("os.arch")+" "
            +System.getProperties().getProperty("os.version")+"; "
            +System.getProperties().getProperty("user.name")+"; "
            +module+"; "
            +"$Revision: 1.3 $"
            +")");

        //PRESET HEADERS
        mergeHeaders(requestHeadersToSend,requestHeader);
     ////// CREATE HEADERS


     /////  SEND
        StringOutputStream sheaders=new StringOutputStream();
        BufferedOutputStream headers=new BufferedOutputStream(sheaders);     
        StringBuffer headerView=new StringBuffer();
        HTTPHelper.writeHeader(headers,requestHeadersToSend,headerView);
        headers.flush();
        headers.close();
        sendRequest(funcname, host, port, useProtocol, sheaders.toString(), base64body);
     /////  SEND

    }


    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The implementation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Socket socket=null;
    private String lastConnectedHostPort=null;

    private void sendTCPRequest(String funcname, String remoteHost, int remotePort,byte[] rtspHeader, byte[] rtspBody, boolean isSSL) throws IOException
    {
        if(!(new String(remoteHost+":"+remotePort)).equals(lastConnectedHostPort))
        {
            try{socket.close();} catch (Exception e){}
            socket=null;
        }
        lastConnectedHostPort=remoteHost+":"+remotePort;
        boolean reusedSocket=false;
        if(socket==null||socket.isClosed()||!socket.isConnected())
        {
            socket=new Socket(remoteHost,remotePort);
            XTTProperties.printVerbose(funcname + ": opened "+socket);
        } else
        {
            XTTProperties.printVerbose(funcname + ": reused "+socket);
            reusedSocket=true;
        }
        //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
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

        socket=RTSPServer.createSocketTCP(""+port,socket);

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(),65536);

        out.write(rtspHeader);
        out.flush();
        XTTProperties.printDebug(funcname + ": written Headers:\n"+ConvertLib.createString(rtspHeader));

        // send body
        if(rtspBody!=null&&rtspBody.length>0)
        {
            out.write(rtspBody);
            out.flush();
            XTTProperties.printDebug(funcname + ": written Body:\n"+ConvertLib.getHexView(rtspBody,0,rtspBody.length));
        }
    }

    private void sendTCPRequest(String funcname, String remoteHost, int remotePort, byte[] rtspHeader, byte[] rtspBody) throws IOException
    {
        sendTCPRequest(funcname,remoteHost,remotePort,rtspHeader,rtspBody,false);
    }
    private void sendSSLRequest(String funcname, String remoteHost, int remotePort, byte[] rtspHeader, byte[] rtspBody) throws IOException
    {
        sendTCPRequest(funcname,remoteHost,remotePort,rtspHeader,rtspBody,true);
    }
    private void sendUDPRequest(String funcname, String remoteHost, int remotePort, byte[] rtspHeader, byte[] rtspBody) throws IOException
    {
        DatagramSocket udpSocket=RTSPServer.createSocketUDP(""+port);
        InetAddress ip=DNSServer.resolveAddressToInetAddress(remoteHost);
        byte[] data=new byte[rtspHeader.length+rtspBody.length];
        int p=0;
        for(int i=0;i<rtspHeader.length;i++)
        {
            data[p++]=rtspHeader[i];
        }
        for(int i=0;i<rtspBody.length;i++)
        {
            data[p++]=rtspBody[i];
        }
        XTTProperties.printDebug(funcname + ": UDP Headers:\n"+ConvertLib.createString(rtspHeader));
        XTTProperties.printDebug(funcname + ": UDP Body:\n"+ConvertLib.getHexView(rtspBody,0,rtspBody.length));
        DatagramPacket packet=new DatagramPacket(data,data.length,ip,remotePort);
        udpSocket.send(packet);
        XTTProperties.printDebug(funcname + ": UDP packet sent: "+data.length+" bytes to: "+packet.getSocketAddress() );
    }

    /**
    * just in case the connection has to be closed client side manually.
    */
    public void closeConnection()
    {
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
                if(headerValue!=null&&!headerValue.equals(""))
                {
                    target.put(headerKey,headerValue);
                }
            }
        }
    }

    private void createHeaders()
    {
        LinkedHashMap<String,String> headers=new LinkedHashMap<String,String>();
    }


    // Fiddle out the first value of a header. Multiple headers should not be there except perhaps for cookies.
    private String getHeaderValue(LinkedHashMap<String,Vector<String>> headers, String key)
    {
        Vector<String> vals=headers.get(key);
        if(vals==null)return null;
        return vals.get(0);
    }

    /**
     * This will create a secure socket out of a normal socket.
     */
    private Socket encryptConnection(String funcname, Socket socket, String targetHost,int targetPort)
    {
        try
        {
            SSLSocketFactory sf=null;
            /*if(enableCertcheck)
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
            } else*/
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
            XTTProperties.printInfo(funcname+": RTSPS: "+certs.length+" certificates");
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
            XTTProperties.printVerbose(funcname+": RTSPS: END certificates");

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

    private String KEYSTORE = "xttserver";
    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private char[] KEYPW="xttxtt".toCharArray();

    /**
     * Reads the configuration from the properties. Values are stored under [MODULE]/[VALUE]
     */
    public void readConfiguration()
    {
        int connectionTimeout=XTTProperties.getIntProperty(module+"SERVER/TIMEOUT");
        if(connectionTimeout<=0)connectionTimeout=30000;
        this.setTimeout(connectionTimeout);
        int pport=XTTProperties.getIntProperty(module+"SERVER/PORT");
        if(pport<=0)pport=DEFAULT_RTSP_PORT;
        //int sport=XTTProperties.getIntProperty(module+"SERVER/SECUREPORT");
        //if(sport<=0)sport=DEFAULT_RTSPS_PORT;
        this.setPorts(pport+"");
        
        if(!XTTProperties.getProperty(module+"/DISABLEAUTOMATICCSEQ").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling automatic CSeq header");
            automaticCSEQ=false;
        }
        if(!XTTProperties.getProperty(module+"/DISABLEDATE").equals("null"))
        {
            XTTProperties.printVerbose(this.getClass().getName()+": "+module+":"+name+": Disabling Date header");
            useDate=false;
        }
        
    }

    public void setPorts(String sport)
    {
        this.port=Integer.parseInt(sport);
        //this.securePort =Integer.parseInt(ssecurePort);
    }

    /**
     * Create a connection by reusing the configuration of this connection.
     * Handy if you have a default connection and want to use its configuration as base.
     */
    public RTSPConnection createConnection(String newname,String port)
    {
        RTSPConnection newConnection=new RTSPConnection(newname,module);
        newConnection.setPorts(port);
        newConnection.setTimeout(timeout);
        return newConnection;
    }

    public static RTSPConnection getConnection(Map<String,RTSPConnection> connections,RTSPConnection defaultConnection,String parameters[])
    {
        if(parameters.length==1)return defaultConnection;
        RTSPConnection connection=connections.get(parameters[1].toLowerCase());
        if(connection==null)
        {
            return defaultConnection;
        } else
        {
            parameters[0]=parameters[0]+"("+connection.getName()+")";
            return connection;
        }
    }
    
    /**
     * set the RTSP headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> connection name (or not present for default),
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the header value or not present removing.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public static void setHeader(Map<String,RTSPConnection> connections,RTSPConnection defaultConnection,String parameters[])
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
            RTSPConnection connection=connections.get(parameters[1].toLowerCase());
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

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public static String getConfigurationOptions()
    {
        return ""
        +"\n        <!-- timeout on client tcp connections to the rtsp server -->"
        +"\n        <Timeout>30000</Timeout>"
        +"\n        <!-- turn off the generation of a new cseq header every request -->"
        +"\n        <!--<disableAutomaticCSeq>-->"
        +"\n        <!-- disable date header being added to a request -->"
        +"\n        <!--<disableDate>-->"
        +"";
    }

    public static final String tantau_sccsid = "@(#)$Id: RTSPConnection.java,v 1.3 2010/03/18 05:30:40 rajesh Exp $";
}

