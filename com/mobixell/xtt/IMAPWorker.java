package com.mobixell.xtt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Vector;

import com.mobixell.xtt.imap.ImapHostManager;
import com.mobixell.xtt.imap.ImapRequestHandler;
import com.mobixell.xtt.imap.ImapSession;
import com.mobixell.xtt.imap.ImapSessionImpl;
import com.mobixell.xtt.imap.Managers;
import com.mobixell.xtt.imap.ProtocolException;
import com.mobixell.xtt.user.UserManager;
/**
 * Processes a single IMAP request which has been received by the IMAPServer.<br/><br/>
 * Stores headers in IMAP/[rcpt to]/header/[headername] and body in IMAP/[rcpt to]/body
 * @author Anil Wadhai
 * @version $Id: IMAPWorker.java,v 1.1 2010/05/14 13:02:09 awadhai Exp $
 */

public class IMAPWorker extends Thread implements IMAPConstants
{
    private static HashMap<String,HashMap<String,Vector<String>>> receivedMailHeaders=null;
    private static HashMap<String,String> receivedMailBodys=null;
    private static Object key = new Object();
    private static Object mailkey=new Object();
    private static int mailcount=0;
    private static int perworkerdelay=0;
    private static int instances=0;
    protected Managers managers = new Managers();
    
    private boolean stop = false;
   
    private ImapRequestHandler requestHandler = new ImapRequestHandler();
    private ImapSession session;
    private ImapHostManager imapHost;
    int id;
    private PrintWriter imapOUT=null;
    private BufferedReader imapIN=null;
    private Socket s = null;
    private String body=null;
    UserManager userManager;
    /* buffer to use for requests */
    byte[] buf;
    final static int BUF_SIZE = 2048;
    static final String CRLF="\r\n";
    
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
   }
    public int getWorkerId() {
        return id;
    }
    
    /**
     * Creates a new IMAPWorker
     * @param id     ID number of this worker thread
     */
    public IMAPWorker(int id) 
    {
        super("IMAPWorker-"+id);
        buf = new byte[BUF_SIZE];
        s = null;
        this.id = id;
     //   this.userManager= userManager;
        //    System.out.println("Web:Worker:Started worker thread " + id);
    }
    /**
     * Tell the worker thread which to which socket the remote client has
     * connected with the request to the Web Server
     * @param s     socket upon which IMAPServer accepted the connection
     */
    synchronized void setSocket(Socket s) 
    {
        this.s = s;
        notifyAll();   // thread is wait()-ing, so notify() to wake it up
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
        IMAPServer.runningthreads.remove(this);
    }
    
    /**
     * Handles the IMAP request
     * @throws IOException
     */
    void handleClient() throws IOException
    {
        
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("IMAPWorker: New Client handled by "+id+" instance "+instances);
            key.notify();
        }

        s.setTcpNoDelay(true);

        imapIN = new BufferedReader(new InputStreamReader(s.getInputStream()),65536);
        imapOUT = new PrintWriter(s.getOutputStream());


        try
        {
           
            String response=IMAP_CONNECT+" "+IMAPServer.getServerName()+" $Revision: 1.1 $";
            XTTProperties.printDebug("IMAPWorker("+id+"): Client connected: "+s.getRemoteSocketAddress()+"\n"+response);
            printOUT(response);
            
           // Managers managers = new Managers();
          //  ImapHostManager imapHostManager = managers.getImapHostManager();
        
            String input=null;
            session = new ImapSessionImpl(managers.getImapHostManager(),managers.getUserManager(),this,s.getInetAddress().getHostName(),
                    s.getInetAddress().getHostAddress());
            
            boolean keepON = true;
            
            while(keepON && null != s)
            {
                keepON = requestHandler.handleRequest(s.getInputStream(), s.getOutputStream(), session);
            }
            
        }
        catch(java.net.SocketException soe)
        {

            if(stop)
            {
                return;
            } else
            {
                XTTProperties.printFail("IMAPWorker("+getWorkerId()+"): SocketException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebugException(soe);
            }
        }
        catch(ProtocolException pe)
        {
           pe.printStackTrace(); 
        }
        finally
        {
            resetWorker();
            XTTProperties.printDebug("IMAPWorker("+id+"): Client disconnected");
            synchronized (key)
            {
                instances--;
                //System.err.println(">-"+id+"->"+instances);
                key.notify();
            }
        
        }
    }
    
    private void printOUT(String response) throws IOException
    {
        imapOUT.print(response+CRLF);
        imapOUT.flush();
    }
    
   
    /**
     * Resets the handler data to a basic state.
     */
    public void resetWorker() {

        // Close and clear streams, sockets

        try {
            try {
                if (s != null && !s.isClosed()) {
                    s.close();
                }
            } catch(NullPointerException ignored) {
                //empty
            }
        } catch (IOException ioe) {
            // Ignoring exception on close
        } finally {
            s = null;
        }

        try {
            if (imapIN != null) {
                imapIN.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
           // imapIN = null;
        }

        try {
            if (imapOUT != null) {
                imapOUT.close();
            }
        } catch (Exception e) {
            // Ignored
        } finally {
            //imapOUT = null;
        }

         // Clear user data
        session = null;
    }
    
    
    
    
    /**
     * set flag asking worker thread to stop
     */
    public void setStop()
    {
        this.stop = true;
        try
        {
            XTTProperties.printDebug("IMAPWorker("+getWorkerId()+"): stop request for id: "+id+" -> closing socket");
            this.s.close();
        } catch(Exception e)
        {
            e.printStackTrace();
        }
        synchronized(this)
        {
            notifyAll();
        }
        XTTProperties.printDebug("IMAPWorker("+getWorkerId()+"): setStop() finished");
    }
    
    public static final String tantau_sccsid = "@(#)$Id: IMAPWorker.java,v 1.1 2010/05/14 13:02:09 awadhai Exp $";
}
