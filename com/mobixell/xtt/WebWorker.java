package com.mobixell.xtt;

import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URI;
import java.net.Socket;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.lang.reflect.Constructor;

/**
 * <p>WebWorker</p>
 * <p>Processes a single HTTP request which has been received by the WebServer</p>
 * <p>Copyright: Copyright (c) 2007</p>
 * <p>Company: 724 Solutions Inc</p>
 * @author Gavin Cattell & Roger Soder
 * @version $Id: WebWorker.java,v 1.69 2010/03/18 12:15:45 rajesh Exp $
 */
public class WebWorker extends Thread implements WebHttpConstants
{
    private static LinkedHashMap<String,Vector<String>> receivedServerHeader=new LinkedHashMap<String,Vector<String>>();
    private static LinkedHashMap<String,String> postData=null;
    private static LinkedHashMap<String,String> sendServerHeader=new LinkedHashMap<String,String>();
    private LinkedHashMap<String,String> headersToSend=new LinkedHashMap<String,String>(); //Not static, this has to be different for each request
    private StringBuffer responseHeader = new StringBuffer("");
    private byte[] responseBody = null;
    private String protocol = "";
    private boolean stop = false;
    private int id;

    private static final String DEFAULTFILE="index.html";
    private static HTTPCache fileCache = new HTTPCache(DEFAULTFILE);

    private static String recievedURL="null";
    private static int instances=0;
    //private static int totalConnections=0;
    private static Object key = new Object();

    final static int BUF_SIZE = 2048;

    private String returnMessage=null;
    private int returnCode = 500;
    private static int overrideReturnCode=0;
    private static String overrideReturnMessage=null;

    private static int serverBodyDelayms=0;
    private static int serverPartDelayms=0;
    private static int serverDelayms=0;
    private int bodydelayms=0;
    private int partdelayms=0;
    private int partdelaypieces=0;


    static final byte[] EOL = {(byte)'\r', (byte)'\n' };

    private static Object postkey=new Object();
    private static int postcount=0;

    private static Object requestkey=new Object();
    private static int requestcount=0;

    /* buffer to use for requests */
    byte[] buf;
    /* Socket to client we're handling, which will be set by the WebServer
       when dispatching the request to us */
    private Socket s = null;
    private WebServer myServer=null;
    private int myTimeout=600000;
    private File myRoot=null;
    private String myServerPort="-";
    
    private boolean keep_alive=true;

    private static final int MINLAGDELAY=100;
    private static int bufferoutputsize = -1;
    public static void setBufferOutputSize(int bufferOutputSize){bufferoutputsize=bufferOutputSize;}
    
    /**
     * Creates a new WebWorker
     * @param id     ID number of this worker thread
     */
    public WebWorker(int id, Socket setSocket, WebServer server, int timeout,File root)
    {
        super("WebWorker-"+id);
        this.buf = new byte[BUF_SIZE];
        this.s = setSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.myServer=server;
        this.myRoot=root;
        if(myServer==null)
        {
            myServerPort="-";
        } else
        {
            myServer.addWorker();
            myServerPort=myServer.getPort()+"";
        }
    }

    public int getWorkerId() 
    {
        return id;
    }
    public String getMyServerPort() 
    {
        return myServerPort;
    }

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        this.stop = true;
        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        try
        {
            this.s.close();
        } catch (Exception ex){}
        // Notify anyone waiting on this object
        notifyAll();
    }

    /**
     * Start the worker thread
     */
    public void run()
    {
        try
        {
            handleClient();
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            XTTProperties.printDebugException(spe);
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            XTTProperties.printDebugException(spue);
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            XTTProperties.printDebugException(ske);
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            XTTProperties.printDebugException(she);
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
            XTTProperties.printDebugException(se);
        } catch (java.net.SocketException soe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebugException(soe);
            }
        } catch (java.net.SocketTimeoutException ste)
        {
            if(keep_alive)
            {
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebugException(ste);
            }
        } catch (java.io.IOException ioe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebugException(ioe);
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printDebugException(e);
        }
    }

    /**
     * Handles the HTTP request
     * @throws IOException
     */
    public void handleClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instance "+instances);
            key.notify();
        }

        InputStream is = new BufferedInputStream(s.getInputStream(),65536);
        PrintStream ps = new PrintStream(s.getOutputStream());
        s.setSoTimeout(myTimeout);
        //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
        s.setTcpNoDelay(true);

        try
        {

            KEEPALIVE:do
            {
                returnCode=HTTP_OK;
                headersToSend=new LinkedHashMap<String,String>();
                responseHeader = new StringBuffer("");
                responseBody=null;
                byte[] receivedData=null;
    
                XTTProperties.printDebug(this.getClass().getName()+"("+getWorkerId()+"): Client connected - receiving from "+s.getRemoteSocketAddress());
                BufferedInputStream in=(BufferedInputStream)is;
    
                int networklagdelay=XTTProperties.getNetworkLagDelay();
                if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;
                /*
                buf=HTTPHelper.readStream(this.getClass().getName() + "("+getWorkerId()+")",in,networklagdelay);
    
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): total bytes received: "+buf.length);
                String storeHeaderString=(new String(buf)).trim();
                */
                LinkedHashMap<String,Vector<String>> serverHeader=HTTPHelper.readHTTPStreamHeaders("WebWorker("+myServerPort+"/"+getWorkerId()+")",in);
                
                // We do this here so we are sure it is done AFTER reading the headers but BEFORE decoding the URL parameters.
                WebWorkerExtension wwe=myServer.getDefaultWebWorkerExtension();

                //End fo stream so break
                if(serverHeader.get(null)==null)break;    
                String storeHeaderString=serverHeader.get(null).get(0);
    
                // are we doing a GET/POST or just a HEAD
                boolean doingGet=false;
                String firstLine[]=null;
                synchronized(receivedServerHeader)
                {
                    receivedServerHeader.clear();
                    receivedServerHeader.putAll(serverHeader);
                    // only set this if we are doing a POST else set to null
                    postData=null;
                    firstLine=storeHeaderString.split("\\s+",4);
                    
                    postData=new LinkedHashMap<String,String>();
                    // HTTP
                    if(firstLine[0].equals("GET"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("POST"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/POST"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if (firstLine[0].equals("HEAD"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/HEAD"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = false;
                    // WEBDAV Methods, added here so WebWorker doesn't reject the requests
                    } else if(firstLine[0].equals("PROPFIND"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("PROPPATCH"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("MKCOL"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("DELETE"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("PUT"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("COPY"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("MOVE"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("LOCK"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    } else if(firstLine[0].equals("UNLOCK"))
                    {
                        XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method is "+firstLine[0]);
                        doingGet = true;
                    // GENERAL
                    } else if (buf.length==1&&buf[0]==-1)
                    {
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): connection has been closed");
                        return;
                    } else
                    {
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Method '"+firstLine[0]+"' is NOT SUPPORTED");
                        returnCode = HTTP_BAD_METHOD;
                    }
                    XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): fileName="+firstLine[1]);
                    recievedURL=firstLine[1];
                    protocol = firstLine[2];
                    String keepReason=null;
                    if(protocol.equals("HTTP/1.0"))
                    {
                        keep_alive=false;
                        keepReason="protocol="+protocol;
                    } else if (getServerHeader().get("connection")!=null&&getServerHeader().get("connection").get(0).equalsIgnoreCase("close"))
                    {
                        keep_alive=false;
                        keepReason="Connection=close";
                    } else
                    {
                        keep_alive=true;
                        keepReason="protocol="+protocol;
                    }
                    XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): "+keepReason+" - Keep-Alive set to "+keep_alive);
                    //String dataBody[]=storeHeaderString.split("\\r\\n\\r\\n",2);
    
                    //storeHeader(dataBody[0]);
                    //if(postData!=null)
                    //{
                        if(getServerHeader().get("transfer-encoding")!=null)
                        {
                            if(getServerHeader().get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                            {
                                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): chunked body found, unchunking");
                                receivedData=HTTPHelper.readChunkedBody("WebWorker("+myServerPort+"/"+getWorkerId()+")",in,getServerHeader());
                            }
                        } else
                        {
                            Vector<String> contentlengthVector=receivedServerHeader.get("content-length");
                            if(contentlengthVector!=null)
                            {
                                int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                                receivedData=new byte[contentlength];
                                HTTPHelper.readBytes(in,receivedData);
                                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): read additional "+contentlength+" bytes as body");
                            } else
                            {
                                //receivedData=HTTPHelper.readStream("WebWorker("+myServerPort+"/"+getWorkerId()+")",in,networklagdelay);
                            }
                        }
                        if(receivedData!=null)
                        {
                            storePostData(receivedData);
                        }
                    //}
                }
    
                String fname = "";
                String queryString = "";
                try 
                {
                    URI url = new URI(firstLine[1]);
                    fname = url.getPath();
                    if( fname == null ) fname = "";
                    if(fname.equals(DEFAULTFILE)||fname.endsWith("/"+DEFAULTFILE))
                    {
                        fname=fname.substring(0,fname.length()-(DEFAULTFILE.length()));
                    }
                    queryString = url.getQuery();
                    if( queryString == null ) queryString = "";
                } catch (Exception ex) 
                {
                    XTTProperties.printDebugException(ex);
                }
    
                int delayms = serverDelayms;
                int randomcookies=0;
                int randomcookieage=0;
                int chunks=0;
                bodydelayms=serverBodyDelayms;
                partdelayms=serverPartDelayms;
                partdelaypieces=2;
                try
                {
                    String query[] = queryString.split( ";|&" );
                    String delay        = "delay=";
                    String disconnect   = "disconnect=";
                    String bodydelay    = "bodydelay=";
                    String partdelay    = "partdelay=";
                    String partpieces   = "pieces=";
                    String randomc      = "randomcookie=";
                    String randomca     = "cookieage=";
                    String chunked      = "chunks=";
                    String xtt_ext      = "extension=";
                    for( int i=0; i<query.length; i++ )
                    {
                        if( query[i].startsWith( delay ) )
                        {
                            delayms = Integer.parseInt(query[i].substring(delay.length()))*1000;
                        } else if( query[i].startsWith( disconnect ) )
                        {
                            delayms = Integer.parseInt(query[i].substring(delay.length()))*1000;
                            this.stop=true;
                        } else if(query[i].startsWith(bodydelay))
                        {
                            bodydelayms =Integer.parseInt(query[i].substring(bodydelay.length()));
                        } else if(query[i].startsWith(partdelay))
                        {
                            partdelayms =Integer.parseInt(query[i].substring(partdelay.length()));
                        } else if(query[i].startsWith(partpieces))
                        {
                            partdelaypieces =Integer.parseInt(query[i].substring(partpieces.length()));
                            if(partdelaypieces<2)partdelaypieces=2;
                        } else if(query[i].startsWith(randomc))
                        {
                            randomcookies =Integer.parseInt(query[i].substring(randomc.length()));
                        } else if(query[i].startsWith(randomca))
                        {
                            randomcookieage =Integer.parseInt(query[i].substring(randomca.length()));
                        } else if(query[i].startsWith(chunked))
                        {
                            chunks = Integer.parseInt(query[i].substring(chunked.length()));
                        } else if(query[i].startsWith(xtt_ext))
                        {
                            String loadClass    = null;
                            ClassLoader loader  = null;
                            Class c             = null;
                            Constructor con     = null;
                            try
                            {
                                loadClass = query[i].substring(xtt_ext.length());
                                loader=this.getClass().getClassLoader();
                                c=loader.loadClass(loadClass);
                                con=c.getConstructor(new Class[0]);
                                wwe=(WebWorkerExtension)con.newInstance(new Object[0]);
                            } catch (ClassNotFoundException cnfe)
                            {
                                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): ClassNotFoundException: '"+loadClass+"' not found");
                                XTTProperties.setTestStatus(XTTProperties.FAILED);
                                XTTProperties.printDebugException(cnfe);
                                return;
                            } catch (ClassCastException cce)
                            {
                                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): ClassCastException: '"+loadClass+"' not extending WebWorkerExtension");
                                XTTProperties.setTestStatus(XTTProperties.FAILED);
                                XTTProperties.printDebugException(cce);
                                return;
                            } catch (Exception ex)
                            {
                                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): "+ex.getClass().getName()+": '"+loadClass+"' error");
                                XTTProperties.setTestStatus(XTTProperties.FAILED);
                                XTTProperties.printDebugException(ex);
                                return;
                            }
                        }
                    }
                } catch (java.lang.RuntimeException ex)
                {
                    XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): Invalid query string \"" + queryString + "\"" );
                    XTTProperties.printDebugException(ex);
                }
                if(delayms>0) 
                {
                    try
                    {
                        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): delay="+delayms+"ms" );
                        synchronized(this)
                        {
                            this.wait(delayms);
                        }
                    } catch(java.lang.InterruptedException ex)
                    {
                        XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): Delay interrupted: " + ex );
                    }
                    if(this.stop)return;
                }
    
                fname = fname.replace('/', File.separatorChar);
    
                if (fname.startsWith(File.separator))
                {
                    fname = fname.substring(1);
                }
    
                if ( (fname.toLowerCase().startsWith("server-status"+File.separatorChar) ) || (fname.toLowerCase().equals("server-status") ) )
                {
                    returnCode = HTTP_OK;
                    returnServerStatus(ps);
                } else
                {
                    if(returnCode<HTTP_MULT_CHOICE)
                    {
                        //System.err.println(fname);
                        File targ = new File(myRoot, fname);
                        if (targ.isDirectory())
                        {
                            File ind = new File(targ, DEFAULTFILE);
                            //System.err.println("Filename: " + fname + " " + fname.length());
                            if ((fname.length()>0)&&(!fname.endsWith(""+File.separatorChar)))
                            {
                                returnCode = HTTP_MOVED_PERM; //No ending / we're gonna send a 301 moved
                                if(receivedServerHeader.get("host") != null)
                                {
                                    fname = File.separatorChar+fname+File.separatorChar;
                                }
                            } else if (ind.exists())
                            {
                                fname += DEFAULTFILE;
                                returnCode = HTTP_OK;
                            } else
                            {
                                returnCode = HTTP_OK;
                            }
                        } else if (!targ.exists()&&(fileCache.getFileSize(myRoot,fname)<0))
                        {
                            returnCode = HTTP_NOT_FOUND;
                        } else
                        {
                            returnCode = HTTP_OK;
                        }
                    }    
                    if(overrideReturnCode>0)returnCode=overrideReturnCode;
    
                    printHeaders(fname);
                    if(randomcookies>0)
                    {
                        injectRandomCookies(randomcookies, randomcookieage);
                    }
    
                    if (doingGet)
                    {
                        if ((returnCode<HTTP_MULT_CHOICE))
                        {
                            sendFile(fname);
                        } else
                        {
                            sendError(fname);
                        }
                        if(chunks>0&&responseBody!=null)
                        {
                            headersToSend.put("transfer-encoding","chunked");
                            headersToSend.put("content-length","null");
                        }
                    }
                    if(wwe!=null)
                    {
                        ByteArrayWrapper extensionBody=new ByteArrayWrapper(responseBody);
                        if(!wwe.execute(this,serverHeader,new ByteArrayWrapper(receivedData),responseHeader,headersToSend,extensionBody,ps))
                        {
                            responseBody=extensionBody.getArray();
                            printResponse(ps,chunks,true);
                            ps.flush();
                        }
                    } else
                    {
                    	if (fname.endsWith(".resp")) printResponse(ps,chunks,false);
                    	else printResponse(ps,chunks,true);                    		
                        ps.flush();
                    }
                }
                synchronized (requestkey)
                {
                    requestcount++;
                    requestkey.notifyAll();
                }
                
            } while(keep_alive);
        } finally
        {
            s.close();
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }
    private void printResponse(PrintStream pst, int chunks,boolean isPrintHeader)
    {
        try
        {
            StreamSplit ps=new StreamSplit(pst);
            boolean sendChunked=false;
			if (isPrintHeader) 
			{
				headersToSend.putAll(sendServerHeader);
				String te = headersToSend.get("transfer-encoding");
				if (te != null && te.equals("chunked")) {
					sendChunked = true;
					if (chunks <= 0)
						chunks = RandomLib.getRandomInt(8) + 2;
				}

				ps.print(responseHeader.toString());
				ps.writeCRLF();

				Iterator it = headersToSend.keySet().iterator();
				String headerKey = null;
				String headerValue = null;
				while (it.hasNext()) {
					headerKey = (String) it.next();
					headerValue = (String) headersToSend.get(headerKey);
					if (!headerValue.equalsIgnoreCase("null")) {
						ps.print(headerKey + ": " + headerValue);
						ps.writeCRLF();
					}
				}

				ps.writeCRLF();
			}
            if(responseBody!=null)
            {

                if(bodydelayms>0)
                {
                    try
                    {
                        ps.flush();
                        Thread.sleep(bodydelayms);
                    } catch (InterruptedException ex)
                    {}
                    ps.appendOutput(" delayed body by "+bodydelayms+"ms\n");
                }
                if(sendChunked)
                {
                    int pointer=0;
                    int partsize=responseBody.length/chunks;
                    if(partsize<1)partsize=1;
                    for(int i=0;i<chunks-1;i++)
                    {
                        ps.appendOutput("chunk length (hex): ");
                        ps.print(ConvertLib.intToHex(partsize)+";");
                        ps.writeCRLF();
                        ps.write(responseBody,pointer,partsize);
                        ps.flush();
                        ps.appendOutput(responseBody,pointer,partsize);
                        pointer+=partsize;
                        if(partdelayms>0)
                        {
                            try
                            {
                                Thread.sleep(partdelayms);
                            } catch (InterruptedException ex)
                            {}
                            ps.appendOutput(" delayed next part by "+partdelayms+"ms\n");
                            ps.debugOutput();
                            ps.clearOutput();
                        }
                        ps.writeCRLF();
                        if(pointer>responseBody.length)break;
                    }
                    partsize=responseBody.length-pointer;
                    ps.appendOutput("chunk length (hex): ");
                    ps.print(ConvertLib.intToHex(partsize)+";");
                    ps.writeCRLF();
                    ps.write(responseBody,pointer,partsize);
                    ps.flush();
                    ps.appendOutput(responseBody,pointer,partsize);
                    ps.writeCRLF();
                    ps.appendOutput("chunk length (hex): ");
                    ps.print(ConvertLib.intToHex(0)+";");
                    ps.writeCRLF();
                    ps.writeCRLF();
                    ps.flush();
                } else
                {
                    if(partdelayms>0)
                    {
                        int splitlength=responseBody.length/partdelaypieces;
                        int i=1;
                        ps.write(responseBody,0,splitlength);
                        ps.flush();
                        ps.appendOutput(responseBody,0,splitlength);
                        ps.debugOutput();
                        ps.clearOutput();
                        int currentstart=splitlength;
                        while(currentstart+splitlength<responseBody.length)
                        {
                            
                            try
                            {
                                Thread.sleep(partdelayms);
                            } catch (InterruptedException ex)
                            {}
                            ps.write(responseBody,currentstart,splitlength);
                            ps.flush();
                            ps.appendOutput(responseBody,splitlength,splitlength);
                            ps.appendOutput(" delayed part "+(++i)+"/"+partdelaypieces+" by "+partdelayms+"ms\n");
                            ps.debugOutput();
                            ps.clearOutput();
                            currentstart=currentstart+splitlength;
                        }
                        try
                        {
                            Thread.sleep(partdelayms);
                        } catch (InterruptedException ex)
                        {}
                        ps.write(responseBody,currentstart,responseBody.length-currentstart);
                        ps.flush();
                        ps.appendOutput(responseBody,currentstart,responseBody.length-currentstart);
                        ps.appendOutput(" delayed part "+(++i)+"/"+partdelaypieces+" by "+partdelayms+"ms\n");
                        
                        
                    } else
                    {
                        ps.write(responseBody);
                        ps.flush();
                        ps.appendOutput(responseBody);
                    }
                }
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): Body written to stream "+responseBody.length+" bytes");
            } else if(sendChunked)
            {
                ps.print(ConvertLib.intToHex(0)+";");
                ps.writeCRLF();
                ps.writeCRLF();
                ps.flush();
            }
            //pst.flush();
            ps.debugOutput();
        }
        catch (Exception e)
        {
            XTTProperties.printDebugException(e);
        }
    }
    private class StreamSplit
    {
        PrintStream ps=null;
        StringBuffer output=new StringBuffer("");
        public StreamSplit(PrintStream ps)
        {
            this.ps=ps;
        }
        public void flush()
        {
            ps.flush();
        }
        public void print(String out)
        {
            ps.print(out);
            output.append(out);
        }
        public void writeCRLF() throws IOException
        {
            ps.write(EOL);
            output.append("\n");
        }
        public void write(byte[] out) throws IOException
        {
            ps.write(out);
        }
        public void write(byte[] out, int offset, int len) throws IOException
        {
            ps.write(out,offset,len);
        }
        public void appendOutput(String out) throws IOException
        {
            output.append(out);
        }
        public void appendOutput(byte[] out) throws IOException
        {
            output.append(ConvertLib.getHexView(out,0,bufferoutputsize));
        }
        public void appendOutput(byte[] out,int off, int len) throws IOException
        {
            output.append(ConvertLib.getHexView(out,off,len+off));
        }
        public void debugOutput()
        {
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): sent response START:\n"+output);
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): sent response END");
        }
        public void clearOutput()
        {
            output=new StringBuffer("");
        }
    }

    private void returnServerStatus(PrintStream pst) throws IOException
    {
        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): sendFile source: GENERATED");
        StreamSplit ps=new StreamSplit(pst);
        ps.print("HTTP/1.0 " + HTTP_OK+" OK");
        ps.writeCRLF();
        ps.print("content-type: text/html; charset=utf-8");
        ps.writeCRLF();
        ps.print("transfer-encoding: chunked");
        ps.writeCRLF();
        ps.print("date: "+createDate());
        ps.writeCRLF();
        ps.print("server: XTT JAVA WebServer $Revision: 1.69 $");
        
        ps.writeCRLF();
        ps.writeCRLF();
        ps.print("3F;");
        ps.writeCRLF();
        ps.print("<html>\n<head><title>XTT Web Server Status</title></head>\n<body>");
        ps.writeCRLF();
        ps.print("11;");
        ps.writeCRLF();
        ps.print("<h1>Headers:</h1>");
        ps.writeCRLF();
        String temp=null;
        synchronized(receivedServerHeader)
        {
            if (receivedServerHeader != null)
            {
                ps.print("10;");
                ps.writeCRLF();
                ps.print("<table border=0>");
                ps.writeCRLF();
                Iterator it=receivedServerHeader.keySet().iterator();
                String hkey;
                Vector hvals;
                //String hval;
                while(it.hasNext())
                {
                    hkey=(String)it.next();
                    hvals=receivedServerHeader.get(hkey);
                    for(int i=0;i<hvals.size();i++)
                    {
                        temp="<tr><td style=\"white-space: nowrap\"><b>" + hkey + ":</b></td><td>" + hvals.get(i) + "</td></tr>";
                        ps.print(Integer.toHexString(temp.length())+";");
                        ps.writeCRLF();
                        ps.print(temp);
                        ps.writeCRLF();
                    }
                    //ps.print("" + hkey + ": " + hval);
                }
                ps.print("8;");
                ps.writeCRLF();
                ps.print("</table>");
                ps.writeCRLF();
            }
        }
        ps.print("15;");
        ps.writeCRLF();
        ps.print("<h1>Client Info:</h1>");
        ps.writeCRLF();
        temp="<b>Connected from:</b>" + s.getRemoteSocketAddress();
        ps.print(Integer.toHexString(temp.length())+";");
        ps.writeCRLF();
        ps.print(temp);
        ps.writeCRLF();

        ps.print("15;");
        ps.writeCRLF();
        ps.print("<h1>Server Info:</h1>");
        ps.writeCRLF();

        java.util.Calendar cal = new java.util.GregorianCalendar();

        temp="<b>Server Time: </b>" + cal.get(java.util.Calendar.HOUR_OF_DAY) + ":" + cal.get(java.util.Calendar.MINUTE) + ":" + cal.get(java.util.Calendar.SECOND) + "<br />";
        ps.print(Integer.toHexString(temp.length())+";");
        ps.writeCRLF();
        ps.print(temp);
        ps.writeCRLF();

        temp="<b>Open Connections: </b>" + instances;
        ps.print(Integer.toHexString(temp.length())+";");
        ps.writeCRLF();
        ps.print(temp);
        ps.writeCRLF();
        //ps.print("<b>Total Connections: </b>" + totalConnections);
        //ps.writeCRLF();

        ps.print("F;");
        ps.writeCRLF();
        ps.print("</body>\n</html>");
        ps.writeCRLF();
        ps.print("0;");
        ps.writeCRLF();
        ps.print("x-xtt-end-status: true");
        ps.writeCRLF();
        ps.writeCRLF();
    }

    private void injectRandomCookies(int numcookies, int cookieage) throws IOException
    {
        java.util.Random random=new java.util.Random();
        StringBuffer cookie = new StringBuffer();
        String divider="";
        int randomcookies = RandomLib.getRandomSize(numcookies);

        for(int i=0;i<randomcookies;i++)
        {
            cookie.append(divider);
            if(random.nextBoolean())
            {
                cookie.append("Session-");
            }else
            {
                cookie.append("Persistent-");
            }
            cookie.append(Math.abs(random.nextInt()));
            cookie.append("=Val");
            cookie.append(Math.abs(random.nextLong()));
            cookie.append("; Max-Age=");
            if(cookieage == 0)
            {
                cookie.append(Math.abs(random.nextInt()));    
            }
            else
            {
                cookie.append(RandomLib.getRandomSize(cookieage));
            }
            cookie.append("; Path=/");
            divider=", ";
        }
        cookie.append(";");

        headersToSend.put("set-cookie",cookie.toString());
    }
/*
    private void chunkBody(int chunks)
    {
        byte buffer[];
        Vector<ByteArrayWrapper> bufferSet=new Vector<ByteArrayWrapper>();
        int byteCounter=0;
        int chunkSize=0;
        int totalSize=responseBody.length;
        String chunkSizeHexString;
        java.util.Random random=new java.util.Random();

        if (chunks > totalSize)
        {
            XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): Chunk size was larger than number of available bytes");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }

        for(int i=0;i<chunks;i++)
        {
            if(i!=(chunks-1))
            {
                chunkSize = random.nextInt(responseBody.length - byteCounter - (chunks - (i+1))) + 1;
            } else
            {
                chunkSize = responseBody.length - byteCounter;
            }

            if (chunkSize < 1)
            {
                XTTProperties.printFail("WebWorker("+myServerPort+"/"+getWorkerId()+"): Chunk size was zero, that's wrong, set less chunks");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            chunkSizeHexString =  ConvertLib.intToHex(chunkSize) + ";\r\n" ;

            //Add the hex size to the total size, and an EOL
            totalSize += (ConvertLib.getOctetByteArrayFromString(chunkSizeHexString).length);
            bufferSet.add(new ByteArrayWrapper(ConvertLib.getOctetByteArrayFromString(chunkSizeHexString)));
            //System.out.println("Total Size: " + totalSize);

            buffer=new byte[chunkSize];
            bufferSet.add(new ByteArrayWrapper(buffer));

            for(int j=0;j<chunkSize;j++)
            {
                buffer[j]=(new Integer(responseBody[byteCounter])).byteValue();
                byteCounter++;
            }
            totalSize += 2;
            bufferSet.add(new ByteArrayWrapper(ConvertLib.getOctetByteArrayFromString("\r\n")));

        }
        totalSize += 3;
        bufferSet.add(new ByteArrayWrapper(ConvertLib.getOctetByteArrayFromString("0\r\n")));

        Object bytes[]=bufferSet.toArray();
        byte[] buf = new byte[totalSize];
        int count=0;
        for(int i=0;i<bytes.length;i++)
        {
            buffer = ((ByteArrayWrapper)bytes[i]).getArray();
            for(int j=0;j<buffer.length;j++)
            {
                buf[count++]=buffer[j];
            }
        }

        responseBody = buf;
        headersToSend.put("transfer-encoding","chunked");
        headersToSend.put("content-length","null");

        XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): Content chunked");
    }
*/
    /**
     * to be completed
     * @param targ
     * @param ps
     * @return
     * @throws IOException
     */
    private boolean printHeaders(String fname) throws IOException
    {
        boolean ret = false;
        File targ = new File(myRoot,fname);
        returnMessage=overrideReturnMessage;

        switch(returnCode)
        {
            //200
            case HTTP_OK:
                if(returnMessage==null)returnMessage=" OK";
                responseHeader.append(protocol + " " + HTTP_OK+returnMessage);
                ret = true;
                break;
            //201
            case HTTP_CREATED:
                if(returnMessage==null)returnMessage=" Created Content";
                responseHeader.append(protocol + " " + HTTP_CREATED+returnMessage);
                ret = false;
                break;
            // 204
            case HTTP_NO_CONTENT:
                if(returnMessage==null)returnMessage=" No Content";
                responseHeader.append("HTTP/1.0 " + HTTP_NO_CONTENT + returnMessage);
                ret = false;
                break;
            //301
            case HTTP_MOVED_PERM:
                if(returnMessage==null)returnMessage=" Moved Permanently";
                responseHeader.append("HTTP/1.0 " + HTTP_MOVED_PERM + returnMessage);
                headersToSend.put("location",""+fname.replace(File.separatorChar,'/'));
                ret = false;
                break;
            //304
            case HTTP_NOT_MODIFIED:
                if(returnMessage==null)returnMessage=" Not Modified";
                responseHeader.append("HTTP/1.0 " + HTTP_NOT_MODIFIED + returnMessage);
                ret = false;
                break;
            //400
            case HTTP_BAD_REQUEST:
                if(returnMessage==null)returnMessage=" Bad Request";
                responseHeader.append(protocol + " " + HTTP_BAD_REQUEST + returnMessage);
                ret = false;
                break;
            //401
            case HTTP_UNAUTHORIZED:
                if(returnMessage==null)returnMessage=" Authorization Required";
                responseHeader.append(protocol + " " + HTTP_UNAUTHORIZED + returnMessage);
                ret = false;
                break;
            //403
            case HTTP_FORBIDDEN:
                if(returnMessage==null)returnMessage=" Forbidden";
                responseHeader.append(protocol + " " + HTTP_FORBIDDEN + returnMessage);
                ret = false;
                break;
            //404
            case HTTP_NOT_FOUND:
                if(returnMessage==null)returnMessage=" Not Found";
                responseHeader.append(protocol + " " + HTTP_NOT_FOUND + returnMessage);
                ret = false;
                break;
            //405
            case HTTP_BAD_METHOD:
                if(returnMessage==null)returnMessage=" Method Not Allowed";
                responseHeader.append(protocol + HTTP_BAD_METHOD + returnMessage);
                break;
            //500
            case HTTP_SERVER_ERROR:
                if(returnMessage==null)returnMessage=" Internal Server Error";
                responseHeader.append(protocol + " " + HTTP_SERVER_ERROR + returnMessage);
                ret = false;
                break;
            default:
                if(returnMessage==null)returnMessage=" XTT Preset Error";
                responseHeader.append(protocol + " " + returnCode + returnMessage);
                ret = true;
                break;
        }

        headersToSend.put("server","XTT JAVA WebServer $Revision: 1.69 $");
        headersToSend.put("date","" + createDate());

        if (ret) 
        {
            if (!targ.isDirectory())
            {
                int len=fileCache.getFileSize(myRoot,fname);
                if(len>=0)
                {
                    headersToSend.put("content-length",""+len);
                    //headersToSend.put("last-modified",""+(new Date(targ.lastModified())));
    
                    String name = targ.getName();
                    if(name.endsWith(targ.separator))name=name+DEFAULTFILE;
                    int ind = name.lastIndexOf('.');
                    String ct = null;
    
                    if (ind > 0) 
                    {
                        ct = HTTPHelper.getContentType(name.substring(ind).toLowerCase());
                    }
                    if (ct == null) 
                    {
                        ct = "unknown/unknown";
                    }
                    headersToSend.put("content-type",""+ct);
                }
            } else 
            {
                headersToSend.put("content-type","text/html");
                if(fileCache.getFileSize(myRoot,fname)>=0)
                {
                    responseBody=fileCache.getFile("WebWorker("+myServerPort+"/"+getWorkerId()+"): sendFile source: ",myRoot,fname);
                } else
                {
                    responseBody=listDirectory(targ);
                }
                headersToSend.put("content-length",""+responseBody.length);
            }
        }
        return ret;
    }

    private String createDate()
    {
        SimpleDateFormat format=new SimpleDateFormat("EEE', 'dd' 'MMM' 'yyyy kk':'mm':'ss z",Locale.US);
        GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        return format.format(calendar.getTime());
    }
    

    /**
     * to be completed
     * @param targ
     * @param ps
     * @throws IOException
     */
    private void sendError(String fname) throws IOException
    {
        String body=null;
        if(fname.equals(""+returnCode))
        {
            if(fileCache.getFileSize(myRoot,fname)>0)
            {
                sendFile(fname);
                return;
            }
        } else
        {
            switch(returnCode)
            {
                case HTTP_NOT_FOUND:
                    body = "<html><head><title>404 Not Found</title></head><body><h1>Not Found</h1><p />\n\n"+"The requested resource was not found.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
                case HTTP_BAD_REQUEST:
                    body = "<html><head><title>"+HTTP_SERVER_ERROR+" "+returnMessage+"</title></head><body><h1>"+returnMessage+"</h1><p />\n\n"+"Something went very wrong.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
                case HTTP_SERVER_ERROR:
                    body = "<html><head><title>"+HTTP_SERVER_ERROR+" Internal Server Error</title></head><body><h1>Internal Server Error</h1><p />\n\n"+"Something went very wrong.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
                case HTTP_UNAUTHORIZED:
                    body = "<html><head><title>"+HTTP_UNAUTHORIZED+" Authorization Required</title></head><body><h1>Authorization Required</h1><p />\n\n"+"Please send Authorization Headers.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
                case HTTP_MOVED_PERM:
                    body = "<html><head><title>"+HTTP_MOVED_PERM+" Moved Permanently</title></head><body><h1>Moved Permanently</h1><p />\n\n"+"The document has moved.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
                case HTTP_NO_CONTENT:
                case HTTP_NOT_MODIFIED:
                    body=null;
                    break;
                default:
                    body = "<html><head><title>"+returnCode+" "+returnMessage+"</title></head><body><h1>"+returnMessage+"</h1><p />\n\n"+"Sending of this errorcode was requested.\n</body></html>";
                    headersToSend.put("content-type","text/html");
                    break;
            }
        }
        if(body!=null)
        {
            responseBody = ConvertLib.createBytes(body);
            headersToSend.put("content-length",""+responseBody.length);
        } else
        {
            responseBody=null;
            headersToSend.remove("content-type");
            headersToSend.put("content-length","0");
        }
    }

    private void sendFile(String fname) 
    {
        try
        {
            File targ = new File(myRoot,fname);
            if (targ.isDirectory())
            {
                return;
            }
            responseBody=fileCache.getFile("WebWorker("+myServerPort+"/"+getWorkerId()+"): sendFile source: ",myRoot,fname);
        }
        catch(Exception e)
        {
            XTTProperties.printWarn("WebWorker("+myServerPort+"/"+getWorkerId()+"): exception sending file "+e.getClass().getName()+" "+e.getMessage());
            XTTProperties.printDebugException(e);
        }
    }
    public static void setCacheFile(String root,String fname, String content)
    {
        if(root.endsWith("/"))root=root.substring(0,root.length()-1);
        try
        {
            URI url = new URI(fname);
            fname = url.getPath();
        } catch(Exception e){}
        fname = fname.replace('/', File.separatorChar);
        if (fname.startsWith(File.separator))
        {
            fname = fname.substring(1);
        }
        byte[] thefile=ConvertLib.getOctetByteArrayFromString(content);
        XTTProperties.printInfo("WebWorker.setCacheFile: store content to cache: '"+root+"/"+fname+"' "+thefile.length+" bytes");
        fileCache.put(fname,thefile);
    }
    public static void setCacheFileBase64(String root,String fname, String content)
    {
        if(root.endsWith("/"))root=root.substring(0,root.length()-1);
        try
        {
            URI url = new URI(fname);
            fname = url.getPath();
        } catch(Exception e){}
        fname = fname.replace('/', File.separatorChar);
        if (fname.startsWith(File.separator))
        {
            fname = fname.substring(1);
        }
        byte[] thefile=ConvertLib.base64Decode(content);
        XTTProperties.printInfo("WebWorker.setCacheFile: base64 decoded to cache: '"+root+"/"+fname+"' "+thefile.length+" bytes");
        fileCache.put(fname,thefile);
    }

    public static void clearCache()
    {
        XTTProperties.printInfo("WebWorker.setCacheFile: clearing file cache");
        fileCache.clear();
    }


    /**
     * to be completed
     * @param dir
     * @param ps
     * @throws IOException
     */
    private byte[] listDirectory(File dir) throws IOException
    {
        String body = "";
        body += "<HTML>\n<HEAD><TITLE>Directory listing</TITLE></HEAD>\n<BODY><P/>\n";
        body += "<A HREF=\"..\">Parent Directory</A><BR/>\n";
        String[] list = dir.list();
        for (int i = 0; list != null && i < list.length; i++) {
            File f = new File(dir, list[i]);
            if (f.isDirectory()) {
                body += "<A HREF=\""+list[i]+"/\">"+list[i]+"/</A><BR/>\n";
            } else {
                body += "<A HREF=\""+list[i]+"\">"+list[i]+"</A><BR/>\n";
            }
        }
        body += "<P/><HR/><BR/><I>" + (new Date()) + "</I>\n</BODY>\n</HTML>";
        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): sendFile source: GENERATED");
        return ConvertLib.createBytes(body);
    }

/*
    public void storeHeader(String headerString)
    {
        XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): Received Header START:\n"+headerString+"\n");
        XTTProperties.setVariable("webserver/headers",headerString);
        String[] headerLines=headerString.split("\\r\\n|\\n");
        String currentLine=null;
        String[] currentHeader=null;
        String[] currentHeaderValues=null;
        String currentHeaderValue=null;
        receivedServerHeader.clear();// receivedServerHeader=new HashMap();
        Vector<String> valueList=null;
        if(headerLines.length>0 && headerLines[0].split("\\s+").length>1)
        {
            recievedURL=headerLines[0].split("\\s+")[1];
        } else
        {
            recievedURL="null";
        }

        String lastHeader="";
        String keepstring="";
        for(int i=1;i<headerLines.length;i++)
        {
            currentLine=headerLines[i].trim();
            if(currentLine!=null&&currentLine!=""&&currentLine.trim().length()!=0)
            {
                //XTTProperties.printDebug(this.getClass().getName()+": currentLine='"+currentLine+"' "+currentLine.length());
                currentHeader=currentLine.split(":",2);
                if(currentHeader.length==2)
                {
                    valueList=new Vector<String>();
                    valueList.add(currentHeader[1].trim());
                    lastHeader=currentHeader[0].trim().toLowerCase();
                    if(lastHeader.equals("connection"))
                    {
                        if(currentHeader[1].trim().equalsIgnoreCase("close"))
                        {
                            keep_alive=false;
                        } else
                        {
                            keep_alive=true;
                        }
                        keepstring=" - Keep-Alive set to "+keep_alive;
                    } else
                    {
                        keepstring="";
                    }
                    XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): "+lastHeader+" -> "+valueList+keepstring);
                    receivedServerHeader.put(lastHeader,valueList);
                } else
                {
                    valueList=receivedServerHeader.get(lastHeader);
                    int vindex=valueList.size()-1;
                    valueList.add(valueList.elementAt(vindex)+" "+currentHeader[0].trim());//,vindex);
                    XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): "+lastHeader+" -> "+valueList);
                }
            } else
            {
                i++;
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): End of headers i="+i+" <"+headerLines.length);
                break;
            }
        }
        XTTProperties.printVerbose(this.getClass().getName()+": Received Header END");
    }
//*/
    private void storePostData(byte[] binDataBody)
    {
        String dataBody=ConvertLib.createString(binDataBody);
        XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Method found");
        XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): Received Body:\n"+ConvertLib.getHexView(binDataBody, 0, bufferoutputsize));
        //XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): Received Body STOP:\n");
        Vector<String> contenttypeVector=receivedServerHeader.get("content-type");
        String contenttype="null";
        if(contenttypeVector==null||contenttypeVector.size()<1)
        {
            returnCode=HTTP_BAD_REQUEST;
        } else
        {
            contenttype=(String)contenttypeVector.get(0);
        }
        if(contenttype.toLowerCase().indexOf("application/x-www-form-urlencoded")>=0)
        {
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Content-Type: "+contenttype);
            String dataLines[]=dataBody.split("&");//;[1].split("&");
            String dataLine[]=null;
            String pKey=null;
            String pData=null;
            String oData=null;
            for(int j=0;j<dataLines.length;j++)
            {
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST current Line:"+dataLines[j]);
                dataLine=dataLines[j].split("=");
                if(dataLine.length==0)
                {
                    XTTProperties.printFail(this.getClass().getName()+": POST malformed data: "+dataLines[j]);
                    break;
                }
                pKey=dataLine[0];
                try
                {
                    pData=URLDecoder.decode(dataLine[1],XTTProperties.getCharSet());
                } catch (java.io.UnsupportedEncodingException uee)
                {
                    if(XTTProperties.printDebug(null))
                    {
                        XTTProperties.printException(uee);
                    }
                } catch (ArrayIndexOutOfBoundsException  aie)
                {
                    pData="";
                }
                oData=postData.get(pKey);
                if(oData==null)
                {
                    oData="";
                } else
                {
                    oData=oData+";";
                }
                pData=oData+pData;
                postData.put(pKey,pData);
            }
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Data received:\n"+postData);
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Data received END");
        } else if (contenttype.toLowerCase().indexOf("multipart/form-data")>=0)
        {
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Content-Type: "+contenttype);
            String boundary=contenttype.split("=")[1];
            String parts[]=dataBody.split("--"+boundary);//[1].split("--"+boundary);
            String lines[]=null;
            String partBH[]=null;
            String partKey=null;
            String partBody=null;
            String partHead=null;
            String oBody=null;
            String partHeadparts[]=null;
            String partHeadpart=null;
            for(int j=0;j<parts.length;j++)
            {
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST current part "+j+":"+parts[j]);
                partKey=null;
                partBody="";
                partBH=parts[j].split("\\r\\n\\r\\n",2);
                partHead=partBH[0];
                if(partBH.length>1)
                {
                    partBody=partBH[1].substring(0,partBH[1].length()-2);
                }
                lines=partHead.split("\\r\\n");
                for(int i=0;i<lines.length;i++)
                {
                    XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST current line "+j+"-"+i+": '"+lines[i]+"'");
                    if(lines[i].toLowerCase().startsWith("content-disposition: form-data;"))
                    {
                        partHeadparts=lines[i].split(";");
                        for(int k=0;k<partHeadparts.length;k++)
                        {
                            partHeadpart=partHeadparts[k].trim();
                            if(partHeadpart.startsWith("name=\"")&&
                               partHeadpart.endsWith("\""))
                            {
                                partKey=partHeadpart.substring(6,partHeadpart.length()-1);
                            }
                        }
                    }
                }
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST current body "+j+": '"+partBody+"'");
                if(partKey!=null)
                {
                    oBody=postData.get(partKey);
                    if(oBody==null)
                    {
                        oBody="";
                    } else
                    {
                        oBody=oBody+";";
                    }
                    partBody=oBody+partBody;
                    postData.put(partKey,partBody);
                }
            }
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Data received:\n"+postData);
            XTTProperties.printVerbose("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST Data received END");
        } else if (contenttype.toLowerCase().indexOf("text/xml")>=0)
        {
            // Code for MM7 VASP Delivery Report handling
            Pattern pattern = Pattern.compile("<Recipient>(\\s)*+<Number>(\\+*\\d++)[^<]*+</Number>(\\s)*+</Recipient>",Pattern.MULTILINE);
            //Pattern pattern = Pattern.compile("<Recipient>(\\s)*+<Number>(\\+*\\d++)[^<]*+</Number>(\\s)*+</Recipient>",Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(dataBody);
            Pattern pattern2 = Pattern.compile("<Recipient>(\\s)*+<RFC2822Address>([^<]++)</RFC2822Address>(\\s)*+</Recipient>",Pattern.MULTILINE);
            Matcher matcher2 = pattern2.matcher(dataBody);
            //System.out.println("dataBody.indexOf(\"DeliveryReportReq\")="+dataBody.indexOf("DeliveryReportReq"));
            if(dataBody.indexOf("DeliveryReportReq")>=0 && matcher.find())
            {
                /*
                for (int i=1;i<=matcher.groupCount();i++)
                {
                    System.out.println("MATCHER:"+i+": '"+matcher.group(i)+"'");
                }
                //*/
                XTTProperties.setVariable("webserver/post/mm7/deliveryreportrsp/"+matcher.group(2),dataBody);
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variable='WEBSERVER/POST/MM7/DELIVERYREPORTRSP/"+matcher.group(2)+"'");
            } else if(dataBody.indexOf("DeliveryReportReq")>=0 && matcher2.find())
            {
                /*
                for (int i=1;i<=matcher2.groupCount();i++)
                {
                    System.out.println("MATCHER:"+i+": '"+matcher2.group(i)+"'");
                }
                //*/
                XTTProperties.setVariable("webserver/post/mm7/deliveryreportrsp/"+matcher2.group(2),dataBody);
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variable='WEBSERVER/POST/MM7/DELIVERYREPORTRSP/"+matcher2.group(2)+"'");
            } else if(dataBody.indexOf("ReadReplyReq")>=0 && matcher.find())
            {
                /*
                for (int i=1;i<=matcher.groupCount();i++)
                {
                    System.out.println("MATCHER:"+i+": '"+matcher.group(i)+"'");
                }
                //*/
                XTTProperties.setVariable("webserver/post/mm7/readreportrsp/"+matcher.group(2),dataBody);
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variable='WEBSERVER/POST/MM7/READREPORTRSP/"+matcher.group(2)+"'");
            } else if(dataBody.indexOf("ReadReplyReq")>=0 && matcher2.find())
            {
                /*
                for (int i=1;i<=matcher2.groupCount();i++)
                {
                    System.out.println("MATCHER:"+i+": '"+matcher2.group(i)+"'");
                }
                //*/
                XTTProperties.setVariable("webserver/post/mm7/readreportrsp/"+matcher2.group(2),dataBody);
                XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variable='WEBSERVER/POST/MM7/READREPORTRSP/"+matcher2.group(2)+"'");
            }

            // End of MM7 VASP Delivery Report handling
            XTTProperties.setVariable("webserver/post/text/xml",dataBody);
            XTTProperties.setVariable("webserver/post/text/xml/plain",dataBody);
            XTTProperties.setVariable("webserver/post/text/xml/base64",ConvertLib.base64Encode(binDataBody));
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variable='WEBSERVER/POST/TEXT/XML'");
        } else
        {
            XTTProperties.setVariable("webserver/post/"+contenttype,dataBody);
            XTTProperties.setVariable("webserver/post/"+contenttype+"/plain",dataBody);
            XTTProperties.setVariable("webserver/post/"+contenttype+"/base64",ConvertLib.base64Encode(binDataBody));
            
            XTTProperties.printDebug("WebWorker("+myServerPort+"/"+getWorkerId()+"): POST body stored in variables:"
                +"\n  WEBSERVER/POST/"+contenttype.toUpperCase()
                +"\n  WEBSERVER/POST/"+contenttype.toUpperCase()+"/PLAIN"
                +"\n  WEBSERVER/POST/"+contenttype.toUpperCase()+"/BASE64");
        }
        synchronized (postkey)
        {
            postcount++;
            postkey.notifyAll();
        }
    }

    public static LinkedHashMap<String,Vector<String>> getServerHeader()
    {
        LinkedHashMap<String,Vector<String>> returnMap=null;
        synchronized(receivedServerHeader)
        {
            returnMap=new LinkedHashMap<String,Vector<String>>();
            returnMap.putAll(receivedServerHeader);
        }
        return returnMap;
    }
    public static LinkedHashMap<String,String> getPostData()
    {
        return postData;
    }
    public static LinkedHashMap<String,String> getServerSendHeader()
    {
        return sendServerHeader;
    }
    public static void setServerSendHeader(LinkedHashMap<String,String> serverHeader)
    {
        sendServerHeader=serverHeader;
    }
    public static void setOverrideReturnCode(int code)
    {
        overrideReturnCode=code;
    }
    public static void setOverrideReturnMessage(String msg)
    {
        overrideReturnMessage=msg;
    }
    public static void setServerBodyDelayms(int delay)
    {
        serverBodyDelayms=delay;
    }
    public static void setServerPartDelayms(int delay)
    {
        serverPartDelayms=delay;
    }
    public static void setServerDelayms(int delay)
    {
        serverDelayms=delay;
    }
    public static String getServerRecievedURL()
    {
        return recievedURL;
    }
    public static void init()
    {
        synchronized (postkey)
        {
            postcount=0;
        }
        synchronized (requestkey)
        {
            requestcount=0;
        }
        overrideReturnCode=0;
        overrideReturnMessage=null;
        serverBodyDelayms=0;
        serverPartDelayms=0;
        fileCache.clear();
    }
    /**
     * Wait for Mail
     */
    public static void waitForPOSTs(int number) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorker.waitForPOSTs: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("WEBSERVER/WAITTIMEOUT");
        if(wait<0)wait=WebServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(postkey)
        {
            while(postcount<number)
            {
                XTTProperties.printInfo("WebWorker.waitForPOSTs: "+postcount+"/"+number);
                if(wait>0)
                {
                    prevcount=postcount;
                    postkey.wait(wait);
                    if(postcount==prevcount)
                    {
                        XTTProperties.printFail("WebWorker.waitForPOSTs: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        postcount=number;
                        return;
                    }
                } else
                {
                    postkey.wait();
                }
            }
            XTTProperties.printInfo("WebWorker.waitForPOSTs: "+postcount+"/"+number);
        }
    }
    /**
     * Wait for Mail
     */
    public static void waitForTimeoutPOSTs(int timeouttime) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorker.waitForPOSTs: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;
        synchronized(postkey)
        {
            number=postcount+1;
            while(postcount<number)
            {
                XTTProperties.printInfo("WebWorker.waitForTimeoutPOSTs: "+postcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=postcount;
                postkey.wait(wait);
                if(postcount==prevcount)
                {
                    XTTProperties.printInfo("WebWorker.waitForTimeoutPOSTs: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("WebWorker.waitForTimeoutPOSTs: POST received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorker.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("WEBSERVER/WAITTIMEOUT");
        if(wait<0)wait=WebServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("WebWorker.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("WebWorker.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("WebWorker.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(WebServer.checkSockets())
        {
            XTTProperties.printFail("WebWorker.waitForTimeoutRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(requestkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=requestcount+1;
            }
            while(requestcount<number)
            {
                XTTProperties.printInfo("WebWorker.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("WebWorker.waitForTimeoutRequests: timed out with no requests!");
                    return;
                }
            }
            XTTProperties.printFail("WebWorker.waitForTimeoutRequests: request received! "+requestcount+"/"+number);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: WebWorker.java,v 1.69 2010/03/18 12:15:45 rajesh Exp $";
}
