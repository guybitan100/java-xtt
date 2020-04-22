package com.mobixell.xtt;

import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * FunctionModule_Proxy provides Simple HTTP Proxy.
 *
 * @author      Roger Soder
 * @version     $Id: ProxyWorker.java,v 1.12 2010/03/18 05:30:39 rajesh Exp $
 */
public class ProxyWorker extends Thread implements WebHttpConstants
{
    private static int instances=0;
    private static Object key = new Object();

    private static Object requestkey=new Object();
    private static int requestcount=0;

    private static final byte[] CRLF = {0x0D,0x0A};
    private String name=null;
    private Socket clientS=null;
    private int timeout=0;
    private String myServerPort=null;
    private static final int MINLAGDELAY=100;
    private boolean keep_alive=true;
    private int id=0;
    private boolean stop = false;
    private ProxyServer myServer=null;

    public ProxyWorker(int id, Socket s,String name,int timeout,ProxyServer server)
    {
        super("ProxyWorker-"+id);
        this.id=id;
        this.clientS=s;
        this.name=name;
        this.timeout=timeout;
        this.myServer=server;
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

    /**
     * set flag asking worker thread to stop
     */
    public synchronized void setStop()
    {
        this.stop = true;
        XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): setting stop");
        try
        {
            this.clientS.close();
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
            XTTProperties.printWarn("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SSLException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(se);
            }
        } catch (java.net.SocketException soe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SocketException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(soe);
                }
            }
        } catch (java.net.SocketTimeoutException ste)
        {
            if(keep_alive)
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): SocketTimeoutException in run - Keep-Alive not enabled");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ste);
                }
            }
        } catch (java.io.IOException ioe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    /**
     * Main method of thread which does the proxy stuff
     */
    public void handleClient() throws IOException
    {
    	boolean isIPv6 = false;
        if(clientS==null)return;
        Socket serverS=null;
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): New Client handled by " +id+" instance "+instances);
            key.notify();
        }
        
        try
        {
            int networklagdelay=XTTProperties.getNetworkLagDelay();
            if(networklagdelay<MINLAGDELAY)networklagdelay=MINLAGDELAY;
            boolean server_keep_alive=true;
            keep_alive=true;
            boolean runbefore=false;
            BufferedInputStream serverIN = null;
            BufferedOutputStream serverOUT = null;
    
            clientS.setSoTimeout(timeout);
            clientS.setTcpNoDelay(true);
            BufferedInputStream clientIN = new BufferedInputStream(clientS.getInputStream(),65536);
            BufferedOutputStream clientOUT = new BufferedOutputStream(clientS.getOutputStream());
            String lastHost=null;
        
        KEEPALIVE:do
        {

        // CLIENT REQUEST /////////////////////////////////////////////////////////////////////////////////////////////

            boolean postData=false;
            /* we will only block in read for this many milliseconds
             * before we fail with java.io.InterruptedIOException,
             * at which point we will abandon the connection.
             */
            if(runbefore)
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Client keep-alive - receiving");
            } else
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Client connected - receiving");
            }

            LinkedHashMap<String,Vector<String>> clientHeader=HTTPHelper.readHTTPStreamHeaders("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST)",clientIN);
            //End fo stream so break
            if(clientHeader.get(null)==null)break;    
            String storeHeaderString=clientHeader.get(null).get(0);
            String[] firstLine=storeHeaderString.split("\\s+",4);
            if(firstLine[0].equals("GET"))
            {
                //XTTProperties.printTransaction("WEBSERVER/HTTP/GET"+XTTProperties.DELIMITER+firstLine[1]);
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Method is "+firstLine[0]);
            } else if(firstLine[0].equals("POST"))
            {
                //XTTProperties.printTransaction("WEBSERVER/HTTP/POST"+XTTProperties.DELIMITER+firstLine[1]);
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Method is "+firstLine[0]);
                // So we are doing a POST
                postData=true;
            } else if (firstLine[0].equals("HEAD"))
            {
                //XTTProperties.printTransaction("WEBSERVER/HTTP/HEAD"+XTTProperties.DELIMITER+firstLine[1]);
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Method is "+firstLine[0]);
            } else
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): Method '"+firstLine[0]+"' is NOT SUPPORTED");
                //returnCode = HTTP_BAD_METHOD;
                return;
            }

            String protocol = firstLine[2];
            String keepReason=null;
            if(protocol.equals("HTTP/1.0"))
            {
                keep_alive=false;
                keepReason="protocol="+protocol;
            } else if (clientHeader.get("proxy-connection")!=null&&clientHeader.get("proxy-connection").get(0).equalsIgnoreCase("close"))
            {
                keep_alive=false;
                keepReason="Proxy-Connection=close";
            } else
            {
                keep_alive=true;
                keepReason="protocol="+protocol;
            }
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): "+keepReason+" - Keep-Alive set to "+keep_alive);
            byte[] clientBody=null;

            if(postData!=false)
            {
                if(clientHeader.get("transfer-encoding")!=null&&clientHeader.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                {
                    // we do this later
                } else
                {
                    Vector contentlengthVector=clientHeader.get("content-length");
                    if(contentlengthVector!=null)
                    {
                        int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                        clientBody=new byte[contentlength];
                        HTTPHelper.readBytes(clientIN,clientBody);
                        XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): read additional "+contentlength+" bytes as body");
                    } else
                    {
                        clientBody=HTTPHelper.readStream("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST)",clientIN,networklagdelay);
                    }
                }
            }


            // Change the VIA header
            alterViaHeader(protocol,clientHeader);

            if(clientHeader.containsKey(""))
            {
                 clientHeader.remove(clientHeader.get("").get(0));
            }

            /*String thisHost=((Vector)clientHeader.get("host")).get(0).toString();
            String fullHost[]=thisHost.split(":");
            int port=80;
            if(fullHost.length>1)
            {
                port=Integer.parseInt(fullHost[1]);
            }
            String host=fullHost[0];*/

         // As per the RFC 2732 host header should be wrapped with [].
            String thisHost=((Vector)clientHeader.get("host")).get(0).toString();
            int port=80;
            String host = "";            
            //handle IPv6 addresses in host header
            if(thisHost.startsWith("["))
            {
            	isIPv6 = true;
            	String fullHost[] = thisHost.split("]:");
            	host=fullHost[0];//this host will still contain [ at the start & ] at the end if host also contains port 
            	if(fullHost.length>1)
                {
                    port=Integer.parseInt(fullHost[1]);
                    host = host.substring(1);
                }else
                {
                	host = host.substring(1,(host.length() -1));
                }
            }
            else
            {
	            String fullHost[]=thisHost.split(":");
	            if(fullHost.length>1)
	            {
	                port=Integer.parseInt(fullHost[1]);
	            }
	            host=fullHost[0];
            }
            	
            // ok, we know where to connect to now
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): connecting to host: "+host+" port: "+port);

            // lets connect to that host
            if(serverS==null||!thisHost.equals(lastHost))
            {
                if(serverS!=null)try{serverS.close();}catch(Exception serverSCloseE){};
              
                if (DNSServer.isHostExist(host))
				{
					if (DNSServer.isIPv4Host(host))
					{
						serverS = new Socket(DNSServer.resolveAddressToInetAddress(host), port);
					}
					else
					{
						serverS = new Socket(DNSServer.resolveIPv6AddressToInetAddress(host), port);
					}
				}
				else
				{
					serverS = new Socket(DNSServer.resolveAddressToInetAddress(host), port);
				}
                
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): server connected: "+serverS);
                serverIN = new BufferedInputStream(serverS.getInputStream(),65536);
                serverOUT = new BufferedOutputStream(serverS.getOutputStream());
                lastHost=thisHost;
            } else
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): server keep-alive: "+serverS);
            }
            
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): writing "+clientHeader.size()+" header lines");
            StringBuffer outputHeaders=new StringBuffer();
            printHeaders(serverOUT,clientHeader,outputHeaders);
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): written headers\n"+outputHeaders);

            if(postData!=false)
            {
                if(clientHeader.get("transfer-encoding")!=null&&clientHeader.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                {
                    XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): chunked body found, streaming");
                    //clientBody=HTTPHelper.readChunkedBody("ProxyWorker("+myServerPort+"/"+getWorkerId()+")",clientIN,clientHeader);
                    readWriteBody(serverOUT,clientIN,"REQUEST");
                } else
                {
                    // We already have the body
                    XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/REQUEST): writing "+clientBody.length+" bytes of body");
                    serverOUT.write(clientBody);
                }
            }
            try
            {
                serverOUT.flush();
            }catch(java.net.SocketException flushE){};

        // SERVER RESPONSE /////////////////////////////////////////////////////////////////////////////////////////////


            LinkedHashMap<String,Vector<String>> serverHeader=HTTPHelper.readHTTPStreamHeaders("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE)",serverIN);
            //End fo stream so break
            if(serverHeader.get(null)==null)break;    
            storeHeaderString=serverHeader.get(null).get(0);
            firstLine=storeHeaderString.split("\\s+",4);

            String responseCode=firstLine[1];
            int serverResponseCode=Integer.parseInt(responseCode);
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): Response-Code is "+responseCode);
            keepReason=null;
            if(protocol.equals("HTTP/1.0"))
            {
                server_keep_alive=false;
                keepReason="protocol="+protocol;
            } else if (serverHeader.get("connection")!=null&&serverHeader.get("connection").get(0).equalsIgnoreCase("close"))
            {
                server_keep_alive=false;
                keepReason="Connection=close";
            } else
            {
                server_keep_alive=true;
                keepReason="protocol="+protocol;
            }
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): "+keepReason+" - Keep-Alive set to "+server_keep_alive);
            byte[] serverBody=new byte[0];

            if(serverHeader.get("transfer-encoding")!=null&&serverHeader.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
            {
                // we do this later
            } else
            {
                Vector contentlengthVector=serverHeader.get("content-length");
                if(contentlengthVector!=null)
                {
                    int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                    serverBody=new byte[contentlength];
                    HTTPHelper.readBytes(serverIN,serverBody);
                    XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): read additional "+contentlength+" bytes as body");
                } else if(serverResponseCode!=HTTP_NO_CONTENT&&serverResponseCode!=HTTP_NOT_MODIFIED)
                {
                    serverBody=HTTPHelper.readStream("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE)",serverIN,networklagdelay);
                } 
            }

            // Change the VIA header
            alterViaHeader(protocol,serverHeader);

            if(serverHeader.containsKey(""))
            {
                 serverHeader.remove(serverHeader.get("").get(0));
            }

            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): writing "+serverHeader.size()+" header lines");
            outputHeaders=new StringBuffer();
            printHeaders(clientOUT,serverHeader,outputHeaders);
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): written headers\n"+outputHeaders);

            if(serverHeader.get("transfer-encoding")!=null&&serverHeader.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
            {
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): chunked body found, streaming");
                //clientBody=HTTPHelper.readChunkedBody("ProxyWorker("+myServerPort+"/"+getWorkerId()+")",clientIN,clientHeader);
                readWriteBody(clientOUT,serverIN,"RESPONSE");
            } else
            {
                // We already have the body
                XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/RESPONSE): writing "+serverBody.length+" bytes of body");
                clientOUT.write(serverBody);
            }
            try
            {
                clientOUT.flush();
            }catch(java.net.SocketException flushE){};

            if(!server_keep_alive||!keep_alive)
            {
                try
                {
                    serverS.close();
                }catch(Exception serverSCloseE){};
                serverS=null;
                serverOUT=null;
                serverIN=null;
            }
            synchronized (requestkey)
            {
                requestcount++;
                requestkey.notifyAll();
            }

        } while(keep_alive);

        } finally
        {
            try
            {
                serverS.close();
            }catch(Exception serverSCloseE){};
            clientS.close();
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            myServer.removeWorker(this);
        }
    }

    /**
    * Read a single chunk and then write it to the outgoing stream. When a chunk of size 0 is receifed close by writing the
    * chunk ending and trailer headers.
    */
    private void readWriteBody(BufferedOutputStream out,BufferedInputStream in, String type) throws java.io.IOException
    {
        byte[] chunk=null;
        Vector<String> chunkOptions=null;
        LinkedHashMap<String,Vector<String>> trailerHeader=new LinkedHashMap<String,Vector<String>>();
        do
        {
            chunkOptions=new Vector<String>();
            chunk=HTTPHelper.readSingleChunk("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/"+type+"): BODY",in,trailerHeader,chunkOptions);
            out.write(ConvertLib.createBytes(ConvertLib.intToHex(chunk.length)));
            if(chunkOptions.size()>0)
            {
                out.write(ConvertLib.createBytes("; "+chunkOptions.get(0)));
            }
            out.write(CRLF);
            if(chunk.length>0)
            {
                out.write(chunk);
                out.write(CRLF);
            }
            XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/"+type+"): BODY: written chunk, length: "+chunk.length);
            out.flush();
        } while(chunk.length!=0);
        //Trailer Headers
        printHeaders(out,trailerHeader, new StringBuffer());
        XTTProperties.printDebug("ProxyWorker("+myServerPort+"/"+getWorkerId()+"/"+type+"): BODY: written final data");
    }


    private void printHeaders(BufferedOutputStream outStream, LinkedHashMap<String,Vector<String>> header, StringBuffer output) throws java.io.IOException
    {
        HTTPHelper.writeHeaders(outStream,header,output);
    }

    /** 
     * Change/Add the via header of the HTTP Request/Response
     * @param protocol
     * @param header
     */
    private void alterViaHeader(String protocol,LinkedHashMap<String,Vector<String>> header)
    {
        Vector<String> vals=header.get("via");
        if(vals==null)
        {
            vals=new Vector<String>();
            vals.add(protocol+" "+name);
            header.put("via",vals);
        } else
        {
            String via=vals.get(0);
            via=via+", "+protocol+" "+name;
            vals.clear();
            vals.add(via);
            header.put("via",vals);
        }
    }
    public static void init()
    {
        synchronized (requestkey)
        {
            requestcount=0;
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: ProxyWorker.java,v 1.12 2010/03/18 05:30:39 rajesh Exp $";
}
