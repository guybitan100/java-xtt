package com.mobixell.xtt;

/* An example of a very simple, multi-threaded SMTP server.
 * Implementation notes are in SMTPServer.html, and also
 * as comments in the source code.
 */
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;

/**
 * Processes a single SMTP request which has been received by the SMTPServer.<br/><br/>
 * Stores headers in SMTP/[rcpt to]/header/[headername] and body in SMTP/[rcpt to]/body
 * @author Roger Soder
 * @version $Id: SMTPWorker.java,v 1.20 2008/12/04 09:37:37 rsoder Exp $
 */
public class SMTPWorker extends Thread implements SMTPConstants
{
    private boolean stop = false;
    private int id;

    private static HashMap<String,HashMap<String,Vector<String>>> receivedMailHeaders=null;
    private static HashMap<String,String> receivedMailBodys=null;

    private HashMap<String,Vector<String>> receivedServerHeader=new HashMap<String,Vector<String>>();
    private static int instances=0;
    private static Object key = new Object();

    private static Object mailkey=new Object();
    private static int mailcount=0;
    private static int perworkerdelay=0;

    private String body=null;

    final static int BUF_SIZE = 2048;

    static final String CRLF="\r\n";

    private PrintWriter smtpOUT=null;
    private BufferedReader smtpIN=null;
    private static String extendedStoreVar=null;

    private static String heloResponse = null;
    private static String mailfromResponse = null;
    private static String rcpttoResponse = null;
    private static String dataResponse = null;
    private static String rsetResponse = null;
    private static String quitResponse = null;

    private static int nextMessageDelay=0;public static void setNextMessageDelay(int delay){nextMessageDelay=delay;}

    /* buffer to use for requests */
    byte[] buf;
    /* Socket to client we're handling, which will be set by the SMTPServer
       when dispatching the request to us */
    private Socket s = null;

    /**
     * Creates a new SMTPWorker
     * @param id     ID number of this worker thread
     */
    public SMTPWorker(int id) 
    {
        super("SMTPWorker-"+id);
        buf = new byte[BUF_SIZE];
        s = null;
        this.id = id;
        //    System.out.println("Web:Worker:Started worker thread " + id);
    }

    public int getWorkerId() {
        return id;
    }

    public static void setHeloResponse(String response)
    {
        heloResponse = response;
    }
    
    public static void setMailfromResponse(String response)
    {
        mailfromResponse = response;
    }    
    
    public static void setRcpttoResponse(String response)
    {
        rcpttoResponse = response;
    }    

    public static void setDataResponse(String response)
    {
        dataResponse = response;
    }
    
    public static void setRsetResponse(String response)
    {
        rsetResponse = response;
    }
    
    public static void setQuitResponse(String response)
    {
        quitResponse = response;
    } 
    /**
     * Tell the worker thread which to which socket the remote client has
     * connected with the request to the Web Server
     * @param s     socket upon which SMTPServer accepted the connection
     */
    synchronized void setSocket(Socket s) 
    {
        this.s = s;
        //    System.out.println("SMTPWorker:set Socket:notifyAll");
        notifyAll();   // thread is wait()-ing, so notify() to wake it up
    }

    /**
     * set flag asking worker thread to stop
     */
    public void setStop()
    {
        this.stop = true;
        try
        {
            XTTProperties.printDebug("SMTPWorker("+getWorkerId()+"): stop request for id: "+id+" -> closing socket");
            this.s.close();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        synchronized(this)
        {
            notifyAll();
        }
        XTTProperties.printDebug("SMTPWorker("+getWorkerId()+"): setStop() finished");
    }

    /**
     * Start the worker thread
     */
    public synchronized void run()
    {
        try
        {
            handleClient();
        } catch (Exception e)
        {
            XTTProperties.printException(e);
            //e.printStackTrace();
        }
        SMTPServer.runningthreads.remove(this);
    }


    /**
     * Handles the SMTP request
     * @throws IOException
     */
    void handleClient() throws IOException
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("SMTPWorker: New Client handled by "+id+" instance "+instances);
            key.notify();
        }

        s.setTcpNoDelay(true);

        smtpIN = new BufferedReader(new InputStreamReader(s.getInputStream()),65536);
        smtpOUT = new PrintWriter(s.getOutputStream());


        try
        {
            // We only support SMTP HELO,EHLO,QUIT,MAIL FROM, RCPT TO, NOOP, RSET, DATA and don't
            // support any fancy SMTP options

            String response=SMTP_CONNECT+" "+SMTPServer.getServerName()+" $Revision: 1.20 $";
            XTTProperties.printDebug("SMTPWorker("+id+"): Client connected: "+s.getRemoteSocketAddress()+"\n"+response);
            printOUT(response);
            String mailfrom="";
            String rcptto="";
            StringBuffer receivedData=new StringBuffer();
            boolean helo=false;

            String input=null;
            while((input=smtpIN.readLine())!=null)
            {
                if(input.toLowerCase().startsWith("helo")||input.toLowerCase().startsWith("ehlo"))
                {

                    if(input.length()<6)
                    {
                        response=SMTP_SYNTAXERROR;
                        helo=false;
                    } else
                    {
                        response=SMTP_OK+" "+SMTPServer.getHostName();
                        helo=true;
                    }
                    
                    if(heloResponse != null)
                    {
                        response=heloResponse;
                    }                    
                    printOUT(input,response);
                } else if(input.toLowerCase().startsWith("quit"))
                {
                    if(quitResponse != null)
                    {
                        printOUT(input,quitResponse);
                    }
                    else
                    {                    
                        printOUT(input,SMTP_QUIT+" "+SMTPServer.getServerName());
                    }
                    break;
                } else if(input.toLowerCase().startsWith("mail from:"))
                {
                    if(mailfromResponse != null)
                    {
                        printOUT(input,mailfromResponse);
                    }                    
                    else if(helo)
                    {
                        if(input.length()<11)
                        {
                            printOUT(input,SMTP_NEGATIVE_SYSERROR);
                        } else
                        {
                            int start=input.indexOf("<");
                            int end=input.indexOf(">");
                            if(start<0||end<0)
                            {
                                printOUT(input,SMTP_NEGATIVE_SYSERROR);
                            } else
                            {
                                mailfrom=input.substring(start+1,end);
                                printOUT(input,SMTP_OKOK);
                                //XTTProperties.printDebug("SMTPWorker: MAIL FROM: "+mailfrom);
                            }
                        }
                    } else
                    {
                        printOUT(input,SMTP_BADSEQUENCE);
                    }
                } else if(input.toLowerCase().startsWith("rcpt to:"))
                {
                    if(rcpttoResponse != null)
                    {
                        printOUT(input,rcpttoResponse);
                    }                     
                    else if(!mailfrom.equals(""))
                    {
                        if(input.length()<9)
                        {
                            printOUT(input,SMTP_NEGATIVE_SYSERROR);
                        } else
                        {
                            int start=input.indexOf("<");
                            int end=input.indexOf(">");
                            if(start<0||end<0)
                            {
                                printOUT(input,SMTP_NEGATIVE_SYSERROR);
                            } else
                            {
                                String to=input.substring(start+1,end).trim();
                                if(to.indexOf("@")==-1||to.indexOf(".")==-1||to.indexOf("@")>to.lastIndexOf("."))
                                {
                                    printOUT(input,SMTP_SYNTAXERROR_FORWARD);
                                } else
                                {
                                    printOUT(input,SMTP_OKOK);
                                    String divder="";
                                    if(!rcptto.equals(""))
                                    {
                                        rcptto=rcptto+";"+to;
                                    } else
                                    {
                                        rcptto=rcptto+to;
                                    }
                                    //XTTProperties.printDebug("SMTPWorker: RCPT TO: "+rcptto);
                                }
                            }
                        }
                    } else
                    {
                        printOUT(input,SMTP_BADSEQUENCE);
                    }
                } else if(input.toLowerCase().startsWith("rset"))
                {
                    mailfrom="";
                    rcptto="";
                    receivedData=new StringBuffer();
                    //helo=false;
                    if(rsetResponse != null)
                    {
                        printOUT(input,rsetResponse);
                    }
                    else
                    {                    
                        printOUT(input,SMTP_OKOK);
                    }
                } else if(input.toLowerCase().startsWith("data"))
                {
                    if(!rcptto.equals(""))
                    {
                        s.setSoTimeout(SMTPServer.timeout);
                        printOUT(input,SMTP_BEGINDATA);
                        boolean hasAllData=false;
                        while((input=smtpIN.readLine())!=null)
                        {
                            if(input.equals("."))
                            {
                                hasAllData=true;
                                break;
                            } else
                            {
                                receivedData.append(input+CRLF);
                            }
                        }
                        if(!hasAllData)
                        {
                            XTTProperties.printFail("SMTPWorker("+id+"): data transfer did not end with . on single line!");
                            XTTProperties.setTestStatus(XTTProperties.FAILED);
                            //return;
                        }
                        int length=receivedData.length();

                        if(XTTProperties.getIntProperty("BUFFEROUTPUTSIZE")>=0 && length>XTTProperties.getIntProperty("BUFFEROUTPUTSIZE"))
                        {
                            length=XTTProperties.getIntProperty("BUFFEROUTPUTSIZE");
                        }
                        String newline="";
                        if(!receivedData.substring(0,length).endsWith("\n"))newline="\n";

                        // Handle Mail /////////////////////////////////////////////////////////////////////
                        String storeVar="smtp/"+rcptto;
                        // Delay per worker before response is sent
                        if(perworkerdelay>0)
                        {
                            try
                            {
                                XTTProperties.printDebug("SMTPWorker("+id+"): per worker delay="+(perworkerdelay*(getWorkerId()+1))+"ms");
                                Thread.sleep(perworkerdelay*(getWorkerId()+1));
                            } catch(InterruptedException ex){};
                        }

                        XTTProperties.printDebug("SMTPWorker("+id+"): received: (max "+length+" characters)\n stored to "+storeVar+"/body \n"+receivedData.substring(0,length)+newline+"."+"\n"+SMTP_OKOK);

                        XTTProperties.setVariable(storeVar+"/xttthreadid",getWorkerId()+"");

                        receivedServerHeader=new HashMap<String,Vector<String>>();
                        Vector<String> valueList=new Vector<String>();
                        valueList.add(mailfrom);
                        receivedServerHeader.put("mail from",valueList);
                        XTTProperties.setVariable(storeVar+"/mail from",mailfrom);
                        XTTProperties.printDebug("SMTPWorker("+id+"): MAIL FROM: "+mailfrom);

                        valueList=new Vector<String>();
                        valueList.add(rcptto);
                        receivedServerHeader.put("rcpt to",valueList);
                        XTTProperties.setVariable(storeVar+"/rcpt to",rcptto);
                        XTTProperties.printDebug("SMTPWorker("+id+"): RCPT TO  : "+rcptto);
                        XTTProperties.printTransaction("SMTPWORKER/RECIEVE"+XTTProperties.DELIMITER+rcptto+XTTProperties.DELIMITER+mailfrom);

                        String dataBody[]=receivedData.toString().split("\\r\\n\\r\\n",2);
                        storeHeader(dataBody[0],storeVar+"/header");
                        String extendedStoreVarValue=storeVar+"/null";
                        if(extendedStoreVar!=null)
                        {
                            if(receivedServerHeader.get(extendedStoreVar.toLowerCase())!=null)
                            {
                                extendedStoreVarValue=storeVar+"/"+receivedServerHeader.get(extendedStoreVar.toLowerCase()).get(0);
                            }
                            XTTProperties.setVariable(extendedStoreVarValue+"/mail from",mailfrom);
                            XTTProperties.setVariable(extendedStoreVarValue+"/rcpt to",rcptto);
                            storeHeader(dataBody[0],extendedStoreVarValue+"/header");
                        }

                        if(dataBody.length==2)
                        {
                            body=dataBody[1];
                        } else
                        {
                            body="";
                        }

                        synchronized (key)
                        {
                            if(receivedMailHeaders==null)
                            {
                                receivedMailHeaders=new HashMap<String,HashMap<String,Vector<String>>>();
                                receivedMailBodys=new HashMap<String,String>();
                            }
                            receivedMailHeaders.put(rcptto,receivedServerHeader);
                            receivedMailBodys.put(rcptto,body);
                            XTTProperties.setVariable(storeVar+"/body",body);
                        }
                        if(extendedStoreVar!=null)
                        {
                            XTTProperties.setVariable(extendedStoreVarValue+"/body",body);
                        }
                        synchronized (mailkey)
                        {
                            mailcount++;
                            mailkey.notifyAll();
                        }
                        // clear buffers according to spec
                        mailfrom="";
                        rcptto="";
                        receivedData=new StringBuffer();
                        
                        if(nextMessageDelay>0)
                        {
                            try
                            {
                                XTTProperties.printDebug("SMTPWorker("+id+"): next message delay="+nextMessageDelay+"ms");
                                Thread.sleep(nextMessageDelay);
                            } catch(InterruptedException ex){};
                        }
                        // End Handle Mail /////////////////////////////////////////////////////////////////////
                        if(dataResponse != null)
                        {
                            printOUT(dataResponse);
                        }                    
                        else 
                        {
                            printOUT(SMTP_OKOK);
                        }
                        s.setSoTimeout(0);

                    } else
                    {
                        printOUT(input,SMTP_NORECIPIENTS);
                    }

                } else
                {
                    printOUT(input,SMTP_UNKNOWN);
                }
            }



        } catch (java.net.SocketException soe)
        {
            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("SMTPWorker("+getWorkerId()+"): SocketException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebugException(soe);
            }
        } finally
        {
            s.close();
            XTTProperties.printDebug("SMTPWorker("+id+"): Client disconnected");
            synchronized (key)
            {
                instances--;
                //System.err.println(">-"+id+"->"+instances);
                key.notify();
            }
        }
            synchronized (key)
            {
                XTTProperties.printDebug(receivedMailHeaders.toString());
                //XTTProperties.printDebug(receivedMailBodys.toString());
            }
    }

    private void printOUT(String response) throws IOException
    {
        smtpOUT.print(response+CRLF);
        smtpOUT.flush();
    }
    private void printOUT(String input,String response) throws IOException
    {
        XTTProperties.printDebug("SMTPWorker("+id+"): received: \n"+input+"\n"+response);
        printOUT(response);
    }

    public void storeHeader(String headerString,String storeVar)
    {
        XTTProperties.printVerbose("SMTPWorker("+id+"): Received Header "+storeVar+" START:");//+headerString+"\n");
        String[] headerLines=headerString.split("\\r\\n|\\n");
        String currentLine=null;
        String[] currentHeader=null;
        String[] currentHeaderValues=null;
        String currentHeaderValue=null;
        Vector<String> valueList=null;
        //receivedServerHeader=new HashMap();

        for(int i=0;i<headerLines.length;i++)
        {
            currentLine=headerLines[i].trim();
            if(currentLine!=null&&currentLine!=""&&currentLine.length()!=0)
            {
                //XTTProperties.printDebug("SMTPWorker: currentLine='"+currentLine+"' "+currentLine.length());
                currentHeader=currentLine.split(":",2);
                valueList=new Vector<String>();
                valueList.add(currentHeader[1].trim());
                XTTProperties.printDebug("SMTPWorker("+id+"): "+currentHeader[0].trim().toLowerCase()+" -> "+valueList);
                receivedServerHeader.put(currentHeader[0].trim().toLowerCase(),valueList);
                XTTProperties.setVariable(storeVar+"/"+currentHeader[0].trim(),currentHeader[1].trim());
            } else
            {
                i++;
                XTTProperties.printDebug("SMTPWorker("+id+"): End of headers i="+i+" <"+headerLines.length);
                break;
            }
        }
        //XTTProperties.printVerbose("SMTPWorker: Received Header START:\n"+receivedServerHeader.toString());
        XTTProperties.printVerbose("SMTPWorker("+id+"): Received Header END");
    }

    /*
    private void storePostData(String dataBody)
    {
        XTTProperties.printDebug("SMTPWorker: POST Method found");
        XTTProperties.printVerbose("SMTPWorker: Received Body START:\n"+dataBody+"\n");
        XTTProperties.printVerbose("SMTPWorker: Received Body STOP:\n");
        //String dataBody[]=headerString.split("\\r\\n\\r\\n",2);
        Vector contenttypeVector=(Vector)receivedServerHeader.get("content-type");
        String contenttype=(String)contenttypeVector.get(0);

        if(contenttype.toLowerCase().equals("application/x-www-form-urlencoded"))
        {

            XTTProperties.printDebug("SMTPWorker: POST Content-Type: "+contenttype);
            String dataLines[]=dataBody.split("&");//;[1].split("&");
            String dataLine[]=null;
            String pKey=null;
            String pData=null;
            String oData=null;
            for(int j=0;j<dataLines.length;j++)
            {
                XTTProperties.printDebug("SMTPWorker: POST current Line:"+dataLines[j]);
                dataLine=dataLines[j].split("=");
                pKey=dataLine[0];
                    pData=URLDecoder.decode(ConvertLib.createBytes(dataLine[1]);
                } catch (ArrayIndexOutOfBoundsException  aie)
                {
                    pData="";
                }
                oData=(String)postData.get(pKey);
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
        } else if (contenttype.toLowerCase().startsWith("multipart/form-data"))
        {
            XTTProperties.printDebug("SMTPWorker: POST Content-Type: "+contenttype);
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
                XTTProperties.printDebug("SMTPWorker: POST current part "+j+":"+parts[j]);
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
                    XTTProperties.printDebug("SMTPWorker: POST current line "+j+"-"+i+": '"+lines[i]+"'");
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
                XTTProperties.printDebug("SMTPWorker: POST current body "+j+": '"+partBody+"'");
                if(partKey!=null)
                {
                    oBody=(String)postData.get(partKey);
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

        } else
        {
            XTTProperties.printFail("SMTPWorker: POST Content-Type NOT FOUND!");
        }
        XTTProperties.printVerbose("SMTPWorker: POST Data received:\n"+postData);
        XTTProperties.printVerbose("SMTPWorker: POST Data received END");
    }
    */

    public static HashMap<String,Vector<String>> getReceivedHeader(String rcptto)
    {
        synchronized (key)
        {
            //System.out.println(rcptto);
            //System.out.println(receivedMailHeaders);
            //System.out.println(receivedMailHeaders.get(rcptto));
            return receivedMailHeaders.get(rcptto);
        }
    }
    public static String getReceivedBody(String rcptto)
    {
        synchronized (key)
        {
            return receivedMailBodys.get(rcptto);
        }
    }

    public static void receivedMailInit()
    {
        synchronized (key)
        {
            receivedMailHeaders=new HashMap<String,HashMap<String,Vector<String>>>();
            receivedMailBodys=new HashMap<String,String>();
        }
        synchronized (mailkey)
        {
            mailcount=0;
        }
    }
    public static void init()
    {
        receivedMailInit();
        extendedStoreVar=null;
        perworkerdelay=0;
        nextMessageDelay=0;
        heloResponse = null;
        mailfromResponse = null;
        rcpttoResponse = null;
        dataResponse = null;
        rsetResponse = null;
        quitResponse = null;
        instances=0;
    }
    
    public static void setExtendedStoreVar(String var)
    {
        extendedStoreVar=var;
    }
    public static void setPerWorkerDelay(int workerdelay)
    {
        perworkerdelay=workerdelay;
    }

    /**
     * Wait for Mail
     */
    public static void waitForMails(int number) throws java.lang.InterruptedException
    {
        String connection=null;
        if(SMTPServer.checkSocket())
        {
            XTTProperties.printFail("SMTPWorker.waitForMails: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("SMTPSERVER/WAITTIMEOUT");
        if(wait<0)wait=SMTPServer.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(mailkey)
        {
            while(mailcount<number)
            {
                XTTProperties.printInfo("SMTPWorker.waitForMails: "+mailcount+"/"+number);
                if(wait>0)
                {
                    prevcount=mailcount;
                    mailkey.wait(wait);
                    if(mailcount==prevcount)
                    {
                        XTTProperties.printFail("SMTPWorker.waitForMails: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    mailkey.wait();
                }
            }
            XTTProperties.printInfo("SMTPWorker.waitForMails: "+mailcount+"/"+number);
        }
    }

    /**
     * Wait for NO Mail
     */
    public static void waitForTimeoutMails(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        String connection=null;
        if(SMTPServer.checkSocket())
        {
            XTTProperties.printFail("SMTPWorker.waitForMails: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=timeouttime;
        int prevcount=0;
        int number=0;

        synchronized(mailkey)
        {
            if(maxnumber>=0)
            {
                number=maxnumber+1;
            } else
            {
                number=mailcount+1;
            }
            while(mailcount<number)
            {
                XTTProperties.printInfo("SMTPWorker.waitForTimeoutMails: "+mailcount+"/"+number+" time: "+timeouttime+"ms");
                    prevcount=mailcount;
                    mailkey.wait(wait);
                    if(mailcount==prevcount)
                    {
                        XTTProperties.printInfo("SMTPWorker.waitForTimeoutMails: timed out!");
                        return;
                    }
            }
            XTTProperties.printFail("SMTPWorker.waitForTimeoutMails: mail received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: SMTPWorker.java,v 1.20 2008/12/04 09:37:37 rsoder Exp $";
}
