package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.HashMap;
import com.mobixell.xtt.smtp.SmtpServer;
import com.mobixell.xtt.util.GreenMail;
import com.mobixell.xtt.util.GreenMailUtil;
import com.mobixell.xtt.util.ServerSetup;


/**
 * FunctionModule_IMAP provides HTTP and HTTPS GET functions.
 *
 * @author      Anil Wadhai
 * @version     $Id: FunctionModule_IMAP.java,v 1.1 2010/05/14 13:02:09 awadhai Exp $
 */
public class FunctionModule_IMAP extends FunctionModule
{
    private IMAPServer imapserver                = null;
    GreenMail greenMail;
    private ServerSetup smtpServerSetup          = null;
    private ServerSetup imapServerSetup          = null;
    private Thread imapws                        = null;
    private final static String CRLF             = "\r\n";
    private Socket imapMailServer=null;
    private int count = 0;
    private String tag ="A";
    private int networklagdelay=100;
    
    /**
     * clears and reinitializes all the variables. Does not reset the gateway.
     */
    public void initialize()
    {
        networklagdelay=XTTProperties.getIntProperty("SYSTEM/NETWORKLAGDELAY");
        if(networklagdelay<0)networklagdelay=100;
        new HashMap<String,String>();
        IMAPWorker.init();
        IMAPServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }
    
    /**
     * constructor sets gateway.
     */
    public FunctionModule_IMAP()
    {
    }
    
    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module IMAP -->"
            +"\n    <IMAPServer>"
            +"\n        <!-- the listening port of the internal imapserver -->"
            +"\n        <Port>143</Port>"
            +"\n        <!-- hostname of the imap server -->"
            +"\n        <hostname>imap.xtt724.com</hostname>"
            +"\n        <!-- timeout on client connections to the imapserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
            +"\n        <waitTimeout>30000</waitTimeout>"
            +"\n    </IMAPServer>";
    }
    
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": IMAPServer: "+parseVersion(IMAPServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": IMAPWorker: "+parseVersion(IMAPWorker.tantau_sccsid));
    }
    
    
    /**
     * starts the IMAPServer as an instance of IMAPServer.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the IMAPServer port, <code>parameters[2]</code> is the
     *                     IMAPServer hostName, <code>parameters[3]</code> argument is the number of IMAPServer threads,
     *                     <code>parameters[4]</code> argument is the IMAPServer timeout. If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see IMAPServer
     * @see IMAPWorker
     * @see XTTProperties
     */
    public boolean startIMAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startIMAPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startIMAPServer: smtpport imapport hostName timeOut");
            return false;
        }

        if(parameters.length == 1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting SMTP Server");
    
                int imapport = XTTProperties.getIntProperty("IMAPSERVER/PORT");
                int smtpport = XTTProperties.getIntProperty("SMTPSERVER/PORT");
                String hostname = XTTProperties.getProperty("IMAPSERVER/HOSTNAME");
//                smtpserver = new SMTPServer(XTTProperties.getIntProperty("SMTPSERVER/PORT"), XTTProperties.getProperty("SMTPSERVER/HOSTNAME"),XTTProperties.getIntProperty("SMTPSERVER/TIMEOUT"));
             
//                Managers managers = new Managers();
//                smtpServerSetup = new ServerSetup(XTTProperties.getIntProperty("SMTPSERVER/PORT"), XTTProperties.getProperty("SMTPSERVER/HOSTNAME"), ServerSetup.PROTOCOL_SMTP);
//                smtpserver = new SmtpServer(smtpServerSetup,managers );
//                imapserver = new IMAPServer(XTTProperties.getIntProperty("IMAPSERVER/PORT"), XTTProperties.getProperty("IMAPSERVER/HOSTNAME"),XTTProperties.getIntProperty("IMAPSERVER/TIMEOUT"));
//                smtpws=(new Thread(smtpserver, "SMTPServer"));
//                smtpws.start();

                /*   greenMail = new GreenMail(smtpServerSetup);
                greenMail.start();
                XTTProperties.printDebug(parameters[0] + ": Started SMTP Server");
                XTTProperties.printVerbose(parameters[0] + ": Starting IMAP Server");
                imapws=(new Thread(imapserver, "IMAPServer"));
                imapws.start();
                */
                imapServerSetup = new ServerSetup(imapport, hostname, ServerSetup.PROTOCOL_IMAP);
                smtpServerSetup = new ServerSetup(smtpport, hostname, ServerSetup.PROTOCOL_SMTP);
                ServerSetup[] smtpIMAPServerSetups = new ServerSetup[]{smtpServerSetup, imapServerSetup};
                greenMail = new GreenMail(smtpIMAPServerSetups);
                greenMail.start();
                
                XTTProperties.printDebug(parameters[0] + ": Started IMAP Server");
                return true;
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                //e.printStackTrace();
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if(parameters.length == 5)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting IMAP Server");
              
                int smtpport = Integer.parseInt(parameters[1]);
                int imapport = Integer.parseInt(parameters[2]);
                String hostname =  parameters[3];
                
                imapServerSetup = new ServerSetup(imapport, hostname, ServerSetup.PROTOCOL_IMAP);
                smtpServerSetup = new ServerSetup(smtpport, hostname, ServerSetup.PROTOCOL_SMTP);
                ServerSetup[] smtpIMAPServerSetups = new ServerSetup[]{smtpServerSetup, imapServerSetup};
                greenMail = new GreenMail(smtpIMAPServerSetups);
                greenMail.start();
                
                
                //smtpServerSetup = new ServerSetup(smtpport, hostname, ServerSetup.PROTOCOL_SMTP);
                
               XTTProperties.printDebug(parameters[0] + ": Started IMAP Server");
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' or '"+parameters[3]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": port hostName timeOut");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return false;

    }
    
    /**
     * stops the SMTPServer and all it's threads.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see SMTPServer#stopGracefully()
     */
    public boolean stopIMAPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopIMAPServer:"+NO_ARGUMENTS);
            return false;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        }
        try
        {
            XTTProperties.printVerbose(parameters[0] + ": Stopping IMAP Server");
            
           // smtpserver.stopGracefully();
           
            greenMail.stop();
           // smtpserver.quit();
           // smtpws.join();
            
            imapserver.stopGracefully();
            imapws.join();
           
            return true;
        } catch(java.lang.NullPointerException npe)
        {
            XTTProperties.printWarn(parameters[0] + ": IMAPServer not found, trying to kill it anyway");
            try
            {
                SMTPServer.closeSocket();
                IMAPServer.closeSocket();
            } catch (Exception ex){}
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;

    }
    
    public void connectMailServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": connectMailServer: recepientServerHost recepientImapServerPort recepientSmtpServerPort timeout");
            return;
        }
        if(parameters.length !=5 )
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectMailServer: recepientServerHost recepientImapServerPort recepientSmtpServerPort timeout");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                InetAddress address=DNSServer.resolveAddressToInetAddress(parameters[1]);
                int imapport=Integer.parseInt(parameters[2]);
                int smtpport=Integer.parseInt(parameters[3]);
                if(imapport<0||imapport>65535) throw new java.lang.NumberFormatException("IMAP Port Out of Range");
                if(smtpport<0||smtpport>65535) throw new java.lang.NumberFormatException("SMTP Port Out of Range");
                XTTProperties.printInfo(parameters[0] + ": connecting MailServer to send email to "+parameters[1]+":"+parameters[2]);
                imapMailServer=new Socket(address, imapport);
                imapServerSetup = new ServerSetup(imapport, parameters[1], ServerSetup.PROTOCOL_IMAP);
                smtpServerSetup = new ServerSetup(smtpport, parameters[1], ServerSetup.PROTOCOL_SMTP);
                int timeout=XTTProperties.getIntProperty("SMTPSERVER/TIMEOUT");
                if(timeout>0)
                {
                    imapMailServer.setSoTimeout(timeout);
                    imapMailServer.setTcpNoDelay(true);
                }
                BufferedReader smtpIN = new BufferedReader(new InputStreamReader(imapMailServer.getInputStream()));
                PrintWriter smtpOUT = new PrintWriter(imapMailServer.getOutputStream());

                String input=smtpIN.readLine();
                if(input!=null&&(input.toLowerCase().startsWith("256")||input.toLowerCase().startsWith("* ok")))
                {
                    XTTProperties.printInfo(parameters[0]+": connect response:\n\n"+input+"\n");
                } else
                {
                    XTTProperties.printFail(parameters[0]+": unable to connect MailServer: response:\n\n"+input+"\n");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }

            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": unable to connect MailServer - check parameters - "+e.getClass().getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    
    
  
    
  /*  public void getMessagesByAccount(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getMessagesByAccount:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String to      = parameters[1];
           try{
               greenMail = new GreenMail(new ServerSetup[]{imapRemoteServerSetup});
               Retriever retriever = new Retriever(greenMail.getImap());
               Message[] messages = retriever.getMessages(to);
               System.out.println("Total messages: --> " + messages.length);
               
               storevar = new String[] { "IMAP/"+to+"/NUMBEROFMSG/",
                       "NUMBEROFMSG/"};
               setMultipleVariables(storevar,"",String.valueOf(messages.length));
               
               for (int i = 0; i < messages.length; i++)
               {
                   Message message = messages[i];
                   storevar = new String[] { "IMAP/"+to+"/"+message.getMessageNumber()+"/","IMAP/"+to+"/"};
                   
                   
                   setMultipleVariables(storevar,"SUBJECT",messages[i].getSubject());
                   setMultipleVariables(storevar,"MSGFROM",message.getFrom()[0].toString());
                   setMultipleVariables(storevar,"BODY",((String) message.getContent()).trim());
                  
               }
               
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": unable to retrive email messages- check parameters - "+e.getClass().getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }*/

    /**
     * The CAPABILITY command requests a listing of capabilities that the
     * server supports. details available at{@linkplain http://tools.ietf.org/html/rfc3501#page-24} 
     * <code>parameters[0]</code> argument is always the method name
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.                    
     * @param parameters
     */
    
    public void sendCAPABILITY(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendCAPABILITY:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
       // parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.CAPABILITY,parameters,IMAPConstants.OK);
    }
    
    /**
     * The NOOP command always succeeds.  It does nothing.
     * The NOOP command can also be used
     * to reset any inactivity autologout timer on the server.
     *<code>parameters[0]</code> argument is always the method name
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.                    
     * @param parameters
     */
    public void sendNOOP(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendNOOP:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
       // parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.NOOP,parameters,IMAPConstants.OK);
    }
    
    /**
     * The LOGOUT command informs the server that the client is done with
     * the connection.  The server MUST send a BYE untagged response
     * before the (tagged) OK response, and then close the network
     * connection.
     * <code>parameters[0]</code> argument is always the method name
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.                    
     * 
     * @param parameters
     */
    public void sendLOGOUT(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendLOGOUT:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        //parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.LOGOUT,parameters,IMAPConstants.OK);
    }
    /**
     * The STARTTLS command is an alternate form of establishing session
     *  privacy protection and integrity checking, but does not establish
     *  authentication or enter the authenticated state.
     *
     * <code>parameters[0]</code> argument is always the method name
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @param parameters
     */
    public void sendSTARTTLS(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSTARTTLS:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
       // parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.STARTTLS,parameters,IMAPConstants.OK);
    }
    
    
    /**
     *
     * 
     * <code>parameters[0]</code> argument is always the method name
     * <code>parameters[1]</code> argument is authentication mechanism name
     * e.g. EXTERNAL ,GSSAPI, PLAIN   Other mechanisms ...
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @param parameters
     */
    public void sendAUTHENTICATE (String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendAUTHENTICATE:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": auth_type");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
     //   parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.AUTHENTICATE ,parameters,IMAPConstants.OK);
    }
   
  /**
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is username
    * <code>parameters[1]</code> argument is password
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendLOGIN (String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendLOGIN:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": username password");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried

       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.LOGIN ,parameters,IMAPConstants.OK);
   }
   
   /**
   *
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is reference name 
   * An empty ("" string) reference name argument indicates that the
   * mailbox name is interpreted as by SELECT.
   * <code>parameters[2]</code> argument is mailbox name with possible wildcards
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   * @param parameters
   */
   public void sendLIST (String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendLIST:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": reference_name mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.LIST ,parameters,IMAPConstants.OK);
   }
   
   /**
   *
   * The LSUB command returns a subset of names from the set of names
   * that the user has declared as being "active" or "subscribed".
   * Zero or more untagged LSUB replies are returned
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is reference name 
   * An empty ("" string) reference name argument indicates that the
   * mailbox name is interpreted as by SELECT.
   * <code>parameters[2]</code> argument is mailbox name with possible wildcards
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   * @param parameters
   */
   public void sendLSUB(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendLSUB:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": reference_name mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.LSUB ,parameters,IMAPConstants.OK);
   }
  
   /**
    *  The SELECT command selects a mailbox so that messages in the mailbox can be accessed.
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is mailbox name 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   
   public void sendSELECT(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendSELECT:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.SELECT ,parameters,IMAPConstants.OK);
   }
   
   
   /**
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is mailbox name 
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   * @param parameters
   */
   public void sendEXAMINE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendEXAMINE:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.EXAMINE ,parameters,IMAPConstants.OK);
   }
   /**
    * The CREATE command creates a mailbox with the given name.
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is mailbox name 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
    public void sendCREATE(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendCREATE:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
     //   parameters[0] ="";// no parameter requried
        sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.CREATE ,parameters,IMAPConstants.OK);
    }
    
    /**
     * The RENAME command changes the name of a mailbox.
     * <code>parameters[0]</code> argument is always the method name
     * <code>parameters[1]</code> argument is existing mailbox name 
     * <code>parameters[1]</code> argument is new mailbox name 
     * If null is used as <code>parameters</code> it sends the allowed parameters list
     * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @param parameters
     */
     public void sendRENAME(String parameters[])
     {
         if(parameters==null)
         {
             XTTProperties.printFail(this.getClass().getName()+": sendRENAME:"+NO_ARGUMENTS);
             return;
         }
         if(parameters.length!=3)
         {
             XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": existing_mailbox_name new_mailbox_name");
             XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
         }
      //   parameters[0] ="";// no parameter requried
         sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.RENAME ,parameters,IMAPConstants.OK);
     }
     
     /**
      * The DELETE command permanently removes the mailbox with the given name.
      * <code>parameters[0]</code> argument is always the method name
      * <code>parameters[1]</code> argument is mailbox name 
      * If null is used as <code>parameters</code> it sends the allowed parameters list
      * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
      * @param parameters
      */
      public void sendDELETE(String parameters[])
      {
          if(parameters==null)
          {
              XTTProperties.printFail(this.getClass().getName()+": sendDELETE:"+NO_ARGUMENTS);
              return;
          }
          if(parameters.length!=2)
          {
              XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
              XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
          }
       //   parameters[0] ="";// no parameter requried
          sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.DELETE ,parameters,IMAPConstants.OK);
      }
     
    
   /**
    * 
    * The SUBSCRIBE command adds the specified mailbox name to the
    * server's set of "active" or "subscribed" mailboxes as returned by
    * the LSUB command.
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is mailbox_name 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendSUBSCRIBE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendSUBSCRIBE:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.SUBSCRIBE ,parameters,IMAPConstants.OK);
   }
   
   /**
    *  The UNSUBSCRIBE command removes the specified mailbox name from
    *  the server's set of "active" or "subscribed" mailboxes as returned
    *  by the LSUB command.
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is mailbox_name 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendUNSUBSCRIBE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendUNSUBSCRIBE:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.UNSUBSCRIBE ,parameters,IMAPConstants.OK);
   }
    
   /**
    * The CHECK command requests a checkpoint of the currently selected
    * mailbox.  A checkpoint refers to any implementation-dependent
    * housekeeping associated with the mailbox
    * <code>parameters[0]</code> argument is always the method name
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendCHECK(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendCHECK:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=1)
       {
           XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.CHECK ,parameters,IMAPConstants.OK);
   }
    
   
   /**
    *  The CLOSE command permanently removes all messages that have the
      \Deleted flag set from the currently selected mailbox.
    * 
    * <code>parameters[0]</code> argument is always the method name
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendCLOSE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendCHECK:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=1)
       {
           XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.CLOSE ,parameters,IMAPConstants.OK);
   }
   
   /**
    *  The EXPUNGE command permanently removes all messages that have the
    *  \Deleted flag set from the currently selected mailbox.  Before
    *  returning an OK to the client, an untagged EXPUNGE response is
    *  sent for each message that is removed.
    * <code>parameters[0]</code> argument is always the method name
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    * @param parameters
    */
   public void sendEXPUNGE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendEXPUNGE:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length!=1)
       {
           XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.EXPUNGE ,parameters,IMAPConstants.OK);
   }
   
   /**
   *
   * This command return some basic information on the folder without selecting the folder, 
   * it takes arguments depending on what information you would like returned.
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is folder name
   * <code>parameters[2]</code> argument is status parenthesized list e.g (messages),(recent), (unseen)
   * 
   * C: A001 STATUS INBOX (messages)
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   *  
   * @param parameters
   */
   public void sendSTATUS(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendSTATUS:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": folder_name status_parenthesized_list");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.STATUS ,parameters,IMAPConstants.OK);
   }
   
   /**
   * 
   * The APPEND command appends the literal argument as a new message to the end of the specified destination mailbox.
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is mailbox name
   * <code>parameters[2]</code> argument is OPTIONAL flag parenthesized list
   * <code>parameters[3]</code> argument is OPTIONAL date/time string
   * <code>parameters[4]</code> argument is message literal
   * 
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   *  
   * @param parameters
   */
   public void sendAPPEND(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendAPPEND:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length < 3 || parameters.length > 5)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name message_literal");
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name flag_parenthesized_list message_literal");
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailbox_name flag_parenthesized_list date/time_string message_literal");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.APPEND ,parameters,IMAPConstants.OK);
   }
   
   
   
   /**
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is OPTIONAL [CHARSET] specification e.g CHARSET UTF-8
    * <code>parameters[2]</code> argument is searching criteria (one or more) if argument > 1 then it should 
    * seprated by SPACE, for more details please refer http://tools.ietf.org/html/rfc3501#page-49
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   
   public void sendSEARCH(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendSEARCH:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length < 2 || parameters.length > 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": charset");
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": charset search_criteria");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.SEARCH ,parameters,IMAPConstants.OK);
   }
   
   /**
   *
   * The FETCH command retrieves data associated with a message in the mailbox.
   * The data items to be fetched can be either a single atom or a parenthesized list
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is sequence set
   * <code>parameters[2]</code> argument is message data item names or macro
   * 
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   *  
   * @param parameters
   */
   public void sendFETCH(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendFETCH:"+NO_ARGUMENTS);
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": sequence_set message_data_item_names");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.FETCH ,parameters,IMAPConstants.OK);
   }
   
  
  /**
   * 
   * The STORE command alters data associated with a message in the mailbox.
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is sequence set
   * <code>parameters[2]</code> argument is message data item names
   * <code>parameters[3]</code> argument is value for message data item
   * 
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   *  
   * @param parameters
   */
   
   public void sendSTORE(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendCOPY: sequence_set message_data_item_names value_for_message_data_item");
           return;
       }
       if(parameters.length != 4)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": sequence_set message_data_item_names value_for_message_data_item");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.STORE ,parameters,IMAPConstants.OK);
   }
   
  /**
   *  The COPY command copies the specified message(s) to the end of the specified destination mailbox.
   * <code>parameters[0]</code> argument is always the method name
   * <code>parameters[1]</code> argument is sequence set
   * <code>parameters[2]</code> argument is mailbox name
   * 
   * If null is used as <code>parameters</code> it sends the allowed parameters list
   * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
   *  
   * @param parameters
   */
   public void sendCOPY(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendCOPY: sequence_set mailbox_name");
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": sequence_set mailbox_name");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.COPY ,parameters,IMAPConstants.OK);
   }
   
   /**
    * 
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is command name
    * <code>parameters[2]</code> argument is command arguments
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendUID(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendUID: command_name command_arguments");
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": command_name command_arguments");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.UID ,parameters,IMAPConstants.OK);
   }
   
   
   
   /**
    *   The SETQUOTA command takes the name of a mailbox quota root and a
    * list of resource limits. The resource limits for the named quota root
    * are changed to be the specified limits.  Any previous resource limits
    * for the named quota root are discarded.
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is quota_root(mailboxName)
    * <code>parameters[2]</code> argument is resource_limits
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   
   public void sendSETQUOTA(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendSETQUOTA: quota_root resource_limits");
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": quota_root resource_limits");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.SETQUOTA ,parameters,IMAPConstants.OK);
   }
   
   
   /**
    *  The GETQUOTA command takes the name of a quota root and returns the
    * quota root's resource usage and limits in an untagged QUOTA response.
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is quota_root(mailboxName)
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendGETQUOTA(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendGETQUOTA: quota_root");
           return;
       }
       if(parameters.length != 2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": quota_root");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.GETQUOTA ,parameters,IMAPConstants.OK);
   }
   
   /**
    *   
    *     The GETQUOTAROOT command takes the name of a mailbox and returns the
    * list of quota roots for the mailbox in an untagged QUOTAROOT
    * response.  For each listed quota root, it also returns the quota
    * root's resource usage and limits in an untagged QUOTA response.
    *
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is mailboxName.
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendGETQUOTAROOT (String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendGETQUOTAROOT: mailboxName");
           return;
       }
       if(parameters.length != 2)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": mailboxName");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.GETQUOTAROOT  ,parameters,IMAPConstants.OK);
   }
    
   /**
    * 
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is smtpHost
    * <code>parameters[2]</code> argument is smtpPort
    * <code>parameters[3]</code> argument is toAddress
    * <code>parameters[4]</code> argument is fromAddress
    * <code>parameters[5]</code> argument is subject
    * <code>parameters[6]</code> argument is body
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendFreeRequest(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: smtpHost smtpPort toAddress fromAddress subject");
           XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: smtpHost smtpPort toAddress fromAddress subject body");
           return;
       }
       if(parameters.length<6 || parameters.length>7)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: smtpHost smtpPort toAddress fromAddress subject");
           XTTProperties.printFail(this.getClass().getName()+": sendFreeRequest: smtpHost smtpPort toAddress fromAddress subject body");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       } else
       {
           String smtpHost      = parameters[1];
           String smtpPort      = parameters[2];
           String to            = parameters[3];
           String from          = parameters[4];
           String subject       = parameters[5];
           String body          = "";
           if (parameters.length == 7)
           {
               body = parameters[6];
           }
           try
           {
              
               GreenMailUtil.sendTextEmail(to, from, subject, body, new ServerSetup(Integer.decode(smtpPort), smtpHost, ServerSetup.PROTOCOL_SMTP)); 
           }
           catch(Exception e)
           {
               XTTProperties.printFail(parameters[0]+": unable to send email - check parameters - "+e.getClass().getName());
               XTTProperties.setTestStatus(XTTProperties.FAILED);
           }
        
        
       }
   }
   
   
    
   /**
    * 
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is toAddress
    * <code>parameters[2]</code> argument is fromAddress
    * <code>parameters[3]</code> argument is subject
    * <code>parameters[4]</code> argument is body
    * 
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendTextEmail(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendTextEmail: toAddress fromAddress subject");
           XTTProperties.printFail(this.getClass().getName()+": sendTextEmail: toAddress fromAddress subject body");
           return;
       }
       if(parameters.length<4 || parameters.length>5)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendTextEmail: toAddress fromAddress subject");
           XTTProperties.printFail(this.getClass().getName()+": sendTextEmail: toAddress fromAddress subject body");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       } else
       {
           String to      = parameters[1];
           String from    = parameters[2];
           String subject = parameters[3];
           String body    ="";
           if (parameters.length == 5)
           {
               body = parameters[4];
           }
           try
           {
               GreenMailUtil.sendTextEmail(to, from, subject, body, smtpServerSetup); 
               XTTProperties.printInfo(this.getClass().getName()+": "+parameters[0]+":\n To :"+to);
           }
           catch(Exception e)
           {
               XTTProperties.printFail(parameters[0]+": unable to send email - check parameters - "+e.getClass().getName());
               XTTProperties.setTestStatus(XTTProperties.FAILED);
           }
        
        
       }
   }
   
   /**
    * 
    * <code>parameters[0]</code> argument is always the method name
    * <code>parameters[1]</code> argument is toAddress
    * <code>parameters[2]</code> argument is fromAddress
    * <code>parameters[3]</code> argument is subject
    * <code>parameters[4]</code> argument is message[optional]
    * <code>parameters[5]</code> argument is attachment
    * <code>parameters[6]</code> argument is contentType
    * <code>parameters[7]</code> argument is filename
    * <code>parameters[8]</code> argument is description
    *
    * If null is used as <code>parameters</code> it sends the allowed parameters list
    * to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
    *  
    * @param parameters
    */
   public void sendAttachmentEmail(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendAttachmentEmail: toAddress fromAddress subject contentType filename description");
           XTTProperties.printFail(this.getClass().getName()+": sendAttachmentEmail: toAddress fromAddress subject message contentType filename description");
           return;
       }
       if(parameters.length<8 || parameters.length>9)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendAttachmentEmail: toAddress fromAddress subject contentType filename description");
           XTTProperties.printFail(this.getClass().getName()+": sendAttachmentEmail: toAddress fromAddress subject message contentType filename description");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       } else
       {
           int    off     = 0;
           String to      = parameters[1];
           String from    = parameters[2];
           String subject = parameters[3];
           String msg     = "";
           if (parameters.length == 9)
           {
               msg = parameters[4];
               off = 1;
           }
           byte[] attachment       = ConvertLib.base64Decode(parameters[4+off]);
           String contentType      = parameters[5+off];
           String filename         = parameters[6+off];
           String description      = parameters[7+off];
           try
           {
               GreenMailUtil.sendAttachmentEmail(to, from, subject, msg, attachment, contentType, filename, description, smtpServerSetup);
               XTTProperties.printInfo(this.getClass().getName()+": "+parameters[0]+":\n To :"+to);
           }
           catch(Exception e)
           {
               XTTProperties.printFail(parameters[0]+": unable to send email - check parameters - "+e.getClass().getName());
               XTTProperties.setTestStatus(XTTProperties.FAILED);
           }
       }
   }
   
   public void sendCREATEUSER(String parameters[])
   {
       if(parameters==null)
       {
           XTTProperties.printFail(this.getClass().getName()+": sendCREATEUSER: userid password");
           return;
       }
       if(parameters.length != 3)
       {
           XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":  userid password");
           XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
       }
    //   parameters[0] ="";// no parameter requried
       
      // GreenMailUtil.sendTextEmail(parameters[0], "test@localhost.com", "", "", smtpServerSetup); 
       sendIMAPCommand(parameters[0], IMAPConstants.SP+IMAPConstants.CREATEUSER ,parameters,IMAPConstants.OK);
   }
    

    private boolean sendIMAPCommand(String function, String command, String[] parameter, String response)
    {
        try
        {
            if((imapMailServer==null)||(!imapMailServer.isConnected()))
            {
                XTTProperties.printFail(this.getClass().getName()+": "+function+": MailServer not connected!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } else
            {
                
                tag = "A"+count;
                StringBuffer arg =new StringBuffer();
                if (parameter.length >1)
                {
                    for (int i =1;i<parameter.length; i++)
                    {
                        arg.append(IMAPConstants.SP);
                        if(parameter[i].equalsIgnoreCase(""))
                        {
                            arg.append("\"\"");  // add empty string
                        }
                        else{
                            arg.append(parameter[i]);  
                        }
                        
                    }
                }
                else
                {
                    arg.append(IMAPConstants.SP); 
                }
                
                PrintWriter imapOUT = new PrintWriter(imapMailServer.getOutputStream());
              //  if(parameter!=null&&!parameter.equals(""))space=" ";
                imapOUT.print(tag+command+arg.toString()+CRLF);
                imapOUT.flush();


                //* we wait for ONE response. all commands should only return 1 line, EHLO would return more, but we don't do that
                //BufferedReader imapIN = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
              //  String inputB="";
                
               // inputB=imapIN.readLine();
                /*/ if you realy nead multi line support use this code:
                BufferedInputStream smtpIN = new BufferedInputStream(mailServer.getInputStream());

                StringBuffer inputB=new StringBuffer();
                int input=-1;
                // have to wait here or available will always be 0, increase if network is realy shitty
                Thread.sleep(networklagdelay);

                while(smtpIN.available()>0&&(input=smtpIN.read())!=-1)
                {
                    inputB.append((char)input);
                }
                //*/
                
                BufferedInputStream imapIN = new BufferedInputStream(imapMailServer.getInputStream());
                StringBuffer commandResponse=new StringBuffer();
                int input=-1;
                Thread.sleep(networklagdelay);
                while(imapIN.available()>0&&(input=imapIN.read())!=-1)
                {
                    commandResponse.append((char)input);
                }
             
                if(commandResponse!=null&&commandResponse.toString().toLowerCase().contains("ok"))
                {
                    XTTProperties.printInfo(this.getClass().getName()+": "+function+":\n\n"+tag+command+arg+"\n"+commandResponse);
                    count++;
                    return true;
                }
                else if(commandResponse!=null&&commandResponse.toString().toLowerCase().contains("no"))
                {
                    XTTProperties.printWarn(this.getClass().getName()+": "+function+":\n\n"+tag+command+arg+"\n"+commandResponse);
                    count++;
                    return true;
                }else
                {
                    XTTProperties.printFail(this.getClass().getName()+": "+function+": MailServer: error:\n\n"+tag+command+arg+"\n"+commandResponse);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            }
            
        } catch (Exception e)
        {
            XTTProperties.printFail(this.getClass().getName()+": "+function+": exception sending to MailServer "+e.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;
    }
    
    /**
     * Returns the version from a $Id: FunctionModule_IMAP.java,v 1.1 2010/05/14 13:02:09 awadhai Exp $ CVS String.
     *
     * @return      String containing the CVS version String in format 1.2.1.3
     */
    public static String parseVersion(String sccsid)
    {
        int start = sccsid.indexOf(",v ") + 3;
        int end   = sccsid.indexOf(" ",start+1);
        return      sccsid.substring(start,end);
    }


}
