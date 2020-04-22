package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;
import java.util.Iterator;

import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramSocket;
import java.net.DatagramPacket;

import java.io.BufferedOutputStream;
import java.io.IOException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;


/**
 * This class has been written as a replacement of javas http class. It should do most of the job at the momment,
 * including following a 301/302 redirect.
 *
 * @author      Roger Soder
 * @version     $Id: SIPConnection.java,v 1.9 2010/03/25 10:18:30 rajesh Exp $
 */
public class SIPConnection implements WebHttpConstants
{
    public final static int DEFAULT_HTTP_PORT  = 80;
    public final static int DEFAULT_HTTPS_PORT =443;
    public final static int MAXRESPONSESIZE = 1000000;

    private static final byte[] CRLF = {0x0D,0x0A};
    private static final String sCRLF = "\r\n";

    public SIPConnection(String name, String module)
    {
        this.name=name;
        this.module=module.toUpperCase();
    }
    private String name = null;
    public String getName(){return name;}
    private String module = null;
    public String getModule(){return module;}

    //Configuration//////////////////////////////////////////////////////////////////////////////////////////////
    private int port = -1;
    private int securePort = -1;

    private int timeout                = 30000;
    public void setTimeout(int timeout){this.timeout=timeout;}

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private java.security.cert.Certificate[] certs= new java.security.cert.Certificate[0];

    private LinkedHashMap<String,String> requestHeader  = new LinkedHashMap<String,String>();
    public LinkedHashMap<String,String> getRequestHeader(){return requestHeader;}

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Used to send a POST request to an URL
     */
    public void sendRequest(String funcname, String remoteHost, int remotePort, String protocol, String headers, String body) throws IOException
    {
        byte[] sipHeader=ConvertLib.createBytes(headers);
        byte[] sipBody=new byte[0];
        if(body!=null&&!body.equals(""))sipBody=ConvertLib.base64Decode(body);//ConvertLib.createBytes(body);
        if(protocol.equalsIgnoreCase("UDP"))
        {
            sendUDPRequest(funcname, remoteHost, remotePort,sipHeader, sipBody);
        } else if(protocol.equalsIgnoreCase("TCP"))
        {
            sendTCPRequest(funcname, remoteHost, remotePort,sipHeader, sipBody);
        } else if(protocol.equalsIgnoreCase("SSL"))
        {
            sendSSLRequest(funcname, remoteHost, remotePort,sipHeader, sipBody);
        } else
        {
            throw new java.net.ProtocolException("Unsupported protocol: "+protocol);
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The implementation
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Socket socket=null;
    private String lastConnectedHostPort=null;
    private SIPWorker worker=null;

    private void sendTCPRequest(String funcname, String remoteHost, int remotePort,byte[] sipHeader, byte[] sipBody, boolean isSSL) throws IOException
    {
        if(!(new String(remoteHost+":"+remotePort)).equals(lastConnectedHostPort)&&lastConnectedHostPort!=null)
        {
            closeConnection();
        }
        lastConnectedHostPort=remoteHost+":"+remotePort;
        boolean reusedSocket=false;
        if(socket==null||socket.isClosed()||!socket.isConnected())
        {
            if(worker!=null){worker.doStop();worker=null;}
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

        if(worker==null)
        {
            worker=SIPServer.createSocketTCP(""+port,socket);
        }

        BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(),65536);

        //StringBuffer headerView=new StringBuffer();
        //HTTPHelper.writeHeader(out,requestHeadersToSend,headerView);
        Socket synchrosocket=socket;
        if(worker!=null)synchrosocket=worker.getSocket();
        synchronized(synchrosocket)
        {
            out.write(sipHeader);
            out.flush();
            XTTProperties.printDebug(funcname + ": written Headers:\n"+ConvertLib.createString(sipHeader));
    
            // send body
            if(sipBody!=null&&sipBody.length>0)
            {
                out.write(sipBody);
                out.flush();
                XTTProperties.printDebug(funcname + ": written Body:\n"+ConvertLib.getHexView(sipBody,0,sipBody.length));
            }
        }
    }
    
    /**
     * Method to send TCP request
     * @param funcname - name of the function
     * @param remoteHost - remote host address
     * @param remotePort - remote port number
     * @param sipHeader - name of sip header
     * @param sipBody  - content of the body
     * @throws IOException
     */
    private void sendTCPRequest(String funcname, String remoteHost, int remotePort, byte[] sipHeader, byte[] sipBody) throws IOException
    {
        sendTCPRequest(funcname,remoteHost,remotePort,sipHeader,sipBody,false);
    }
    
    /**
     * Method to send SSL request
     * @param funcname - name of the function
     * @param remoteHost - remote host address
     * @param remotePort - remote port number
     * @param sipHeader - name of sip header
     * @param sipBody  - content of the body
     * @throws IOException
     */
    private void sendSSLRequest(String funcname, String remoteHost, int remotePort, byte[] sipHeader, byte[] sipBody) throws IOException
    {
        sendTCPRequest(funcname,remoteHost,remotePort,sipHeader,sipBody,true);
    }
    
    /**
     * Method to send UDP request
     * @param funcname - name of the function
     * @param remoteHost - remote host address
     * @param remotePort - remote port number
     * @param sipHeader - name of sip header
     * @param sipBody  - content of the body
     * @throws IOException
     */
    private void sendUDPRequest(String funcname, String remoteHost, int remotePort, byte[] sipHeader, byte[] sipBody) throws IOException
    {
        DatagramSocket udpSocket=SIPServer.createSocketUDP(""+port);
        InetAddress ip=DNSServer.resolveAddressToInetAddress(remoteHost);
        byte[] data=new byte[sipHeader.length+sipBody.length];
        int p=0;
        for(int i=0;i<sipHeader.length;i++)
        {
            data[p++]=sipHeader[i];
        }
        for(int i=0;i<sipBody.length;i++)
        {
            data[p++]=sipBody[i];
        }
        XTTProperties.printDebug(funcname + ": UDP Headers:\n"+ConvertLib.createString(sipHeader));
        XTTProperties.printDebug(funcname + ": UDP Body:\n"+ConvertLib.getHexView(sipBody,0,sipBody.length));
        DatagramPacket packet=new DatagramPacket(data,data.length,ip,remotePort);
        udpSocket.send(packet);
        XTTProperties.printDebug(funcname + ": UDP packet sent: "+data.length+" bytes to: "+packet.getSocketAddress() );
    }

    /**
    * just in case the connection has to be closed client side manually.
    */
    public void closeConnection()
    {
        if(worker!=null)
        {
            worker.doStop();
        } else
        {
            try{socket.close();} catch (Exception e){}
        }
        socket=null;
        worker=null;
    }
    public void setSocket(SIPWorker w)
    {
        worker=w;
        socket=w.getSocket();
        lastConnectedHostPort=null;
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
    
    /**
     * Method creates header map
     */
    private void createHeaders()
    {
        LinkedHashMap<String,String> headers=new LinkedHashMap<String,String>();
    }

    /**
     * Fiddle out the first value of a header. Multiple headers should not be there except perhaps for cookies.
     * @param headers
     * @param key
     * @return - value of given key
     */    
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

    private String KEYSTORE = "xttserver";
    private char[] KEYSTOREPW = "xttxtt".toCharArray();
    private char[] KEYPW="xttxtt".toCharArray();

    /**
     * Reads the configuration from the properties. Values are stored under [MODULE]/[VALUE]
     */
    public void readConfiguration()
    {
        int connectionTimeout=XTTProperties.getIntProperty("SIPSERVER/TIMEOUT");
        if(connectionTimeout<=0)connectionTimeout=30000;
        this.setTimeout(connectionTimeout);
        int pport=XTTProperties.getIntProperty("SIPSERVER/PORT");
        if(pport<=0)pport=5060;
        int sport=XTTProperties.getIntProperty("SIPSERVER/SECUREPORT");
        if(sport<=0)sport=5061;
        this.setPorts(pport+"",sport+"");
    }
    
    /**
     * Method to set port and secure port
     * @param sport
     * @param ssecurePort
     */
    public void setPorts(String sport, String ssecurePort)
    {
        this.port=Integer.parseInt(sport);
        this.securePort =Integer.parseInt(ssecurePort);
    }

    /**
     * Create a connection by reusing the configuration of this connection.
     * Handy if you have a default connection and want to use its configuration as base.
     */
    public SIPConnection createConnection(String newname,String port, String securePort)
    {
        SIPConnection newConnection=new SIPConnection(newname,module);
        this.setPorts(port,securePort);
        return newConnection;
    }
    
    /**
     * Method to retrieve connection
     * @param connections - map of connections object
     * @param defaultConnection 
     * @param parameters
     * @return It returns SIPConnection
     */
    public static SIPConnection getConnection(Map<String,SIPConnection> connections,SIPConnection defaultConnection,String parameters[])
    {
        if(parameters.length==1)return defaultConnection;
        SIPConnection connection=connections.get(parameters[1].toLowerCase());
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
     * Method to set Header key and value
     * @param connections
     * @param defaultConnection
     * @param parameters
     */
    public static void setHeader(Map<String,SIPConnection> connections,SIPConnection defaultConnection,String parameters[])
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
            SIPConnection connection=connections.get(parameters[1].toLowerCase());
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
     * @return - timeout on client connections to the WEBSERVER/PROXY
     */
    public static String getConfigurationOptions()
    {
        return ""
        +"\n        <!-- timeout on client connections to the webserver/proxy -->"
        +"\n        <Timeout>30000</Timeout>"
        +"";
    }

    public static final String tantau_sccsid = "@(#)$Id: SIPConnection.java,v 1.9 2010/03/25 10:18:30 rajesh Exp $";
}

