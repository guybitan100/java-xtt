package com.mobixell.xtt;

import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;


/**
 * FunctionModule_SMTP provides HTTP and HTTPS GET functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_SMTP.java,v 1.21 2010/05/05 08:12:37 rajesh Exp $
 */
public class FunctionModule_SMTP extends FunctionModule
{
    private SMTPServer s                 = null;
    private Thread ws                    = null;
    private final static String CRLF     = "\r\n";
    private HashMap<String,String> sendHeader = new HashMap<String,String>();

    private String messageData=null;

    private Socket mailServer=null;

    private int networklagdelay=100;

    /**
     * clears and reinitializes all the variables. Does not reset the gateway.
     */
    public void initialize()
    {
        networklagdelay=XTTProperties.getIntProperty("SYSTEM/NETWORKLAGDELAY");
        if(networklagdelay<0)networklagdelay=100;
        sendHeader           = new HashMap<String,String>();
        messageData          = null;
        SMTPWorker.init();
        SMTPServer.resetWorkerId();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    public HashMap<String,String> getSendHeader()
    {
        return this.sendHeader;
    }

    /**
     * constructor sets gateway.
     */
    public FunctionModule_SMTP()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module SMTP -->"
            +"\n    <SMTPServer>"
            +"\n        <!-- the listening port of the internal smtpserver -->"
            +"\n        <Port>25</Port>"
            +"\n        <!-- hostname of the smtp server -->"
            +"\n        <hostname>smtp.xtt724.com</hostname>"
            +"\n        <!-- timeout on client connections to the smtpserver -->"
            +"\n        <Timeout>30000</Timeout>"
            +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
            +"\n        <waitTimeout>30000</waitTimeout>"
            +"\n    </SMTPServer>";
    }
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": SMTPServer: "+parseVersion(SMTPServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": SMTPWorker: "+parseVersion(SMTPWorker.tantau_sccsid));
    }

    /**
     * starts the SMTPServer as an instance of SMTPServer.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the SMTPServer port, <code>parameters[2]</code> is the
     *                     SMTPServer hostName, <code>parameters[3]</code> argument is the number of SMTPServer threads,
     *                     <code>parameters[4]</code> argument is the SMTPServer timeout. If only <code>parameters[0]</code> is submitted
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see SMTPServer
     * @see SMTPWorker
     * @see XTTProperties
     */
    public boolean startSMTPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSMTPServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSMTPServer: port hostName timeOut");
            return false;
        }

        if(parameters.length == 1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting SMTP Server");
                s = new SMTPServer(XTTProperties.getIntProperty("SMTPSERVER/PORT"), XTTProperties.getProperty("SMTPSERVER/HOSTNAME"),XTTProperties.getIntProperty("SMTPSERVER/TIMEOUT"));
                ws=(new Thread(s, "SMTPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started SMTP Server");
                return true;
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                //e.printStackTrace();
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if(parameters.length == 4)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting SMTP Server");
                s = new SMTPServer(Integer.parseInt(parameters[1]), parameters[2], Integer.parseInt(parameters[3]));
                ws=(new Thread(s, "SMTPServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started SMTP Server");
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
    public boolean stopSMTPServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSMTPServer:"+NO_ARGUMENTS);
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
            XTTProperties.printVerbose(parameters[0] + ": Stopping SMTP Server");
            s.stopGracefully();
            ws.join();
            //XTTProperties.printDebug(parameters[0] + ": Stopped SMTP Server");
            return true;
        } catch(java.lang.NullPointerException npe)
        {
            XTTProperties.printWarn(parameters[0] + ": SMTPServer not found, trying to kill it anyway");
            try
            {
                SMTPServer.closeSocket();
            } catch (Exception ex){}
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;

    }

    /**
     * remove all the headers that are to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearHeader:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing header");
            this.sendHeader=new HashMap<String,String>();
        }
    }

    /**
     * remove all the headers that are to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void clearReceivedMails(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearReceivedMails:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": clearing received Mails");
            SMTPWorker.receivedMailInit();
        }
    }

    /**
     * set the smtp headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the header key, <code>parameters[2]</code>
     *                     argument and following are concatenated together to the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: headerFieldKey headerFieldValue");
            return;
        }
        setHeader(getSendHeader(),parameters);
    }

    private void setHeader(HashMap<String,String> header,String parameters[])
    {
        HashMap<String,String> headerMap=header;
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            if(parameters.length>2)
            {
                StringBuffer val=new StringBuffer("");
                //String param[]=super.getLastFunctionLine().split("\\s+",2);
                for(int i=2;i<parameters.length;i++)
                {
                    val.append(parameters[i]);
                }
                //String param[]=super.getLastFunctionLine().split("\\s+",3);
                //String val=param[2].trim();
                XTTProperties.printInfo(parameters[0]+": setting HeaderField "+parameters[1]+" to: "+val.toString());
                // Actually set the Header Key and Value
                headerMap.put(parameters[1].trim(),val.toString());
            } else
            {
                XTTProperties.printInfo(parameters[0]+": removing HeaderField "+parameters[1]);
                // Actually remove the Header Key and Value
                headerMap.remove(parameters[1].trim());
            }
        }
    }

    /**
     * compare the http headers received by the server from the client with a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the RCPT TO Address, <code>parameters[2]</code> argument is the header key, <code>parameters[3]</code>
     *                     argument and following are concatenated together to the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeader: rcptto-address headerFieldKey expectedValue");
            return;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": rcptto-address headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            checkHeader(SMTPWorker.getReceivedHeader(parameters[1]),parameters);
        }
    }

    private void checkHeader(HashMap<String,Vector<String>> headerMap,String parameters[])
    {
        if(headerMap==null&&!parameters[2].equals("null"))
        {
            //System.out.println(headerMap);
            XTTProperties.printFail(parameters[0] + ": RCPT TO:"+parameters[1]+" not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        } else if(headerMap==null&&parameters[2].equals("null"))
        {
            XTTProperties.printInfo(parameters[0] + ": RCPT TO:"+parameters[1]+" not found");
            return;
        }
        Vector<String> valueList=new Vector<String>();
        StringBuffer val=new StringBuffer("");
        for(int i=3;i<parameters.length;i++)
        {
            val.append(parameters[i]);
        }
        valueList.add(val.toString());
        checkHeader(parameters[0],headerMap,parameters[2],valueList);
    }

    /**
     * check a http header received by the server from the client does contain a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the RCPT TO Address, <code>parameters[2]</code> argument is the header key, <code>parameters[3]</code>
     *                     argument and following are concatenated together to the header value part.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderPart: rcptto-address headerFieldKey expectedValuePart");
            return;
        }
        checkHeaderPart(SMTPWorker.getReceivedHeader(parameters[1]),parameters,false);
    }
    /**
     * check a http header received by the server from the client does NOT contain a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the RCPT TO Address, <code>parameters[2]</code> argument is the header key, <code>parameters[3]</code>
     *                     argument and following are concatenated together to the header value part.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkServerHeaderNotPart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkServerHeaderNotPart: rcptto-address headerFieldKey expectedValueNotPartOfHeader");
            return;
        }
        checkHeaderPart(SMTPWorker.getReceivedHeader(parameters[1]),parameters,true);
    }

    private void checkHeaderPart(HashMap<String,Vector<String>> headerMap,String parameters[],boolean inverse)
    {
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": rcptto-address headerFieldKey expectedValuePart");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        StringBuffer val=new StringBuffer("");
        for(int i=3;i<parameters.length;i++)
        {
            val.append(parameters[i]);
        }
        String functionName=parameters[0];
        String headerKey=parameters[2];
        String headkey=parameters[2].toLowerCase();
        String headerValue=val.toString();
        if(headerMap==null)
        {
            XTTProperties.printFail(functionName+": rcptto-address '"+parameters[1]+"' did not receive mail");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        } else if(headerMap.get(headkey)==null)
        {
            XTTProperties.printFail(functionName+": header "+headerKey+": header key not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        } else
        {
            String value=headerMap.get(headkey).get(0).toString();
            if(value.indexOf(headerValue)!=-1)
            {
                if(inverse)
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": ["+value+"] contains ["+ headerValue+"]");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                } else
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": ["+value+"] contains ["+ headerValue+"]");
                }
                return;
            } else
            {
                if(inverse)
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": ["+value+"] doesn't contain ["+ headerValue+"]");
                } else
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": ["+value+"] doesn't contain ["+ headerValue+"]");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
                return;
            }
        }
    }

    private void checkHeader(String functionName,HashMap<String,Vector<String>> headerMap,String headerKey,Vector<String> headerValues)
    {
        String headkey=headerKey.toLowerCase();
        if(headkey.equals("null"))headkey=null;
        //XTTProperties.printDebug("HEADER:"+headerMap+"\n"+headerMap.get(headkey));
        if(headerMap.get(headkey)!=null
            && headerMap.get(headkey).containsAll(headerValues))
        {
            XTTProperties.printInfo(functionName+": header "+headerKey+": "+headerMap.get(headkey));
        } else
        {
            if(headerMap.get(headkey)==null)
            {
                if(headerValues.contains("null"))
                {
                    XTTProperties.printInfo(functionName+": header "+headerKey+": header key not found");
                    return;
                } else
                {
                    XTTProperties.printFail(functionName+": header "+headerKey+": header key not found");
                }
            } else
            {
                String more="";
                if(headerValues.contains("null"))more=" (header key not found)";
                XTTProperties.printFail(functionName+": header "+headerKey+": "+headerMap.get(headkey)+" wanted "+ headerValues+""+more);
            }
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }


    public void connectMailServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": connectMailServer: recepientServerHost recepientServerPort");
            return;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": connectMailServer: recepientServerHost recepientServerPort");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                InetAddress address=DNSServer.resolveAddressToInetAddress(parameters[1]);
                int port=Integer.parseInt(parameters[2]);
                if(port<0||port>65535) throw new java.lang.NumberFormatException("Out of Range");
                XTTProperties.printInfo(parameters[0] + ": connecting MailServer to send email to "+parameters[1]+":"+parameters[2]);
                mailServer=new Socket(address, port);
                int timeout=XTTProperties.getIntProperty("SMTPSERVER/TIMEOUT");
                if(timeout>0)
                {
                    mailServer.setSoTimeout(timeout);
                    mailServer.setTcpNoDelay(true);
                }


                BufferedReader smtpIN = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
                String input=smtpIN.readLine();
                if(input!=null&&input.toLowerCase().startsWith("220"))
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

    public void disconnectMailServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": disconnectMailServer:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            sendSMTPCommand("QUIT","","221");
        }
    }

    public void sendHELO(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendHELO: myHostName");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": myHostName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        sendSMTPCommand("HELO",parameters[1],"250");
    }

    public void sendMAILFROM(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMAILFROM: fromAddress");
            XTTProperties.printFail(this.getClass().getName()+": sendMAILFROM: fromAddress responseCode");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fromAddress");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fromAddress responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        if(parameters.length==2)
        {
            sendSMTPCommand("MAIL FROM:","<"+parameters[1]+">","250");
        } else
        {
            sendSMTPCommand("MAIL FROM:","<"+parameters[1]+">",parameters[2]);
        }
    }

    public void sendRCPTTO(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendRCPTTO: toAddress");
            XTTProperties.printFail(this.getClass().getName()+": sendRCPTTO: toAddress responseCode");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": toAddress");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": toAddress responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        if(parameters.length==2)
        {
            sendSMTPCommand("RCPT TO:","<"+parameters[1]+">","250");
        } else
        {
            sendSMTPCommand("RCPT TO:","<"+parameters[1]+">",parameters[2]);
        }
    }

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
        } else
        {
            sendSMTPCommand("NOOP","","250");
        }
    }

    public void sendRSET(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendRSET:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            sendSMTPCommand("RSET","","250");
        }
    }

    public void setDATA(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setDATA: dataBody");
            XTTProperties.printFail(this.getClass().getName()+": setDATA: dataHeader dataBody");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": dataBody");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": dataHeader dataBody");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String body="";
            String head="";
            StringBuffer headers=new StringBuffer();

                Iterator<String> it=sendHeader.keySet().iterator();
                String hkey;
                String hval;
                while(it.hasNext())
                {
                    hkey=it.next();
                    hval=sendHeader.get(hkey);
                    headers.append(hkey+": "+hval+"\n");
                    XTTProperties.printDebug(parameters[0]+": adding header: "+hkey+": "+hval);
                }
            if(parameters.length==3)
            {
               body=parameters[2];
               head=headers.toString()+parameters[1];
            } else
            {
               body=parameters[1];
               head=headers.toString();
            }
            if(!head.equals(""))
            {
                if(!head.endsWith("\n"))
                {
                    head=head+"\n";
                }
                messageData=head+"\n"+body+"\n.\n";
            } else
            {
                messageData=body+"\n.\n";
            }
            XTTProperties.printDebug(parameters[0]+": message DATA set");
        }
    }

    public void sendDATA(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDATA:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": sendDATA: responseCode1 responseCode2");
            return;
        }
        if(parameters.length!=1&&parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": responseCode1 responseCode2");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        if((messageData==null)||messageData.equals(""))
        {
            XTTProperties.printFail(this.getClass().getName()+": sendDATA: mail DATA not set");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        String responseCode1="354";
        if(parameters.length>1)responseCode1=parameters[1];
        String responseCode2="250";
        if(parameters.length>2)responseCode2=parameters[2];
        if(sendSMTPCommand("DATA","",responseCode1))
        {
            sendDATA(messageData,responseCode2);
        }
    }


    private boolean sendSMTPCommand(String command, String parameter, String response)
    {
        try
        {
            if((mailServer==null)||(!mailServer.isConnected()))
            {
                XTTProperties.printFail(this.getClass().getName()+": sendSMTPCommand: MailServer not connected!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } else
            {
                PrintWriter smtpOUT = new PrintWriter(mailServer.getOutputStream());

                String space="";
                if(parameter!=null&&!parameter.equals(""))space=" ";
                smtpOUT.print(command+space+parameter+CRLF);
                smtpOUT.flush();


                //* we wait for ONE response. all commands should only return 1 line, EHLO would return more, but we don't do that
                BufferedReader smtpIN = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
                String inputB="";
                inputB=smtpIN.readLine();
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
                if(inputB!=null&&inputB.toString().toLowerCase().startsWith(response))
                {
                    XTTProperties.printInfo(this.getClass().getName()+": sendSMTPCommand:\n\n"+command+" "+parameter+"\n"+inputB);
                    return true;
                } else
                {
                    XTTProperties.printFail(this.getClass().getName()+": sendSMTPCommand: MailServer: error:\n\n"+command+" "+parameter+"\n"+inputB);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSMTPCommand: exception sending to MailServer "+e.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;
    }

    private boolean sendDATA(String data,String response)
    {
        try
        {
            if((mailServer==null)||(!mailServer.isConnected()))
            {
                XTTProperties.printFail(this.getClass().getName()+": sendDATA: MailServer not connected!");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } else
            {
                BufferedReader smtpIN = new BufferedReader(new InputStreamReader(mailServer.getInputStream()));
                
                //PrintWriter smtpOUT = new PrintWriter(mailServer.getOutputStream());

                /*
                String[] dataLines=data.split("\\r\\n|\\n");

                for(int i=0;i<dataLines.length;i++)
                {
                    smtpOUT.print(dataLines[i]+CRLF);
                    System.out.print(dataLines[i]+CRLF);
                    smtpOUT.flush();
                }
                smtpOUT.print("."+CRLF);
                System.out.print("."+CRLF);
                */
                
                // TODO: CHECK THE REST OF SMTP MODULE FOR UTF-8 INCOMPATIBILITY
                java.io.BufferedOutputStream outP = new java.io.BufferedOutputStream(mailServer.getOutputStream());
                outP.write(ConvertLib.createBytes(data.replaceAll("\\r\\n|\\n","\r\n")));
                outP.flush();
                

                //smtpOUT.print(data.replaceAll("\\r\\n|\\n","\r\n"));
                //smtpOUT.flush();
                //System.out.print(data.replaceAll("\\r\\n|\\n","\r\n"));

                String inputB="";

                inputB=smtpIN.readLine();

                String newline="";
                if(!data.endsWith("\n"))newline="\n";
                if(inputB!=null&&inputB.toString().toLowerCase().startsWith(response))
                {
                    XTTProperties.printInfo(this.getClass().getName()+": sendSMTPCommand:\n\n"+data+newline+""+inputB);
                    return true;
                } else
                {
                    XTTProperties.printFail(this.getClass().getName()+": sendSMTPCommand: MailServer: error:\n\n"+data+newline+""+inputB);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendSMTPCommand: exception sending to MailServer "+e.getClass().getName());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            XTTProperties.printException(e);
        }
        return false;
    }


    public boolean waitForMails(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForMails: numMails");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numMails");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" Mails received on SMTPServer");
                SMTPWorker.waitForMails(messages);
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;

    }
    public boolean waitForTimeoutMails(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutMails: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutMails: timeoutms maxPreviousMessages");
            return false;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": timeoutms maxPreviousMessages");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int timeoutms=Integer.parseInt(parameters[1]);
                int maxnumber=-1;
                if(parameters.length==3)
                {
                    try
                    {
                        maxnumber=Integer.parseInt(parameters[2]);
                    } catch (NumberFormatException nfe2)
                    {
                        XTTProperties.printFail(parameters[0]+": '"+parameters[2]+"' is NOT a correct number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO Mails received on SMTPServer");
                SMTPWorker.waitForTimeoutMails(timeoutms,maxnumber);
                return true;
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;

    }

    /**
     * Set a response code to over-write the 'real' one.
     */
    public void setReponseCode(String parameters[])
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: action");
            XTTProperties.printFail(this.getClass().getName()+": setHeader: action responseCode");
            return;
        } else if((parameters.length<2)||(parameters.length>3))
        {
            XTTProperties.printFail(parameters[0]+": setHeader: action");
            XTTProperties.printFail(parameters[0]+": setHeader: action responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        String action = parameters[1];
        String response = null; if (parameters.length > 2) response = parameters[2];
        
        if(action.equalsIgnoreCase("HELO"))
        {
            SMTPWorker.setHeloResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting HELO response to: " + response);
        }
        else if(action.equalsIgnoreCase("MAILFROM"))
        {
            SMTPWorker.setMailfromResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting MAILFROM response to: " + response);
        }
        else if(action.equalsIgnoreCase("RCPTTO"))
        {
            SMTPWorker.setRcpttoResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting RCPTTO response to: " + response);
        }
        else if(action.equalsIgnoreCase("DATA"))
        {
            SMTPWorker.setDataResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting DATA response to: " + response);
        }
        else if(action.equalsIgnoreCase("RSET"))
        {
            SMTPWorker.setRsetResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting RSET response to: " + response);
        }
        else if(action.equalsIgnoreCase("QUIT"))
        {
            SMTPWorker.setQuitResponse(response);
            XTTProperties.printInfo(parameters[0] + ": Setting QUIT response to: " + response);
        }
        else
        {
            XTTProperties.printFail(parameters[0] + ": Unknown action '" + action + "'");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);    
        }
    }
    
    public void setExtendedStoreHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setExtendedStoreHeader:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setExtendedStoreHeader: headerName");
            return;
        }
        if(parameters.length<1||parameters.length>2)
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": headerName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }
        if(parameters.length==1)
        {
            XTTProperties.printInfo(parameters[0] + ": Disabling extended Store Header");
            SMTPWorker.setExtendedStoreVar(null);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": Setting extended Store Header to: /"+parameters[1]);
            SMTPWorker.setExtendedStoreVar(parameters[1]);
        }
    }
    /**
     * Sets a delay which will delay the response by delay*workerid miliseconds, nice if you know parallel connections get opened in order so you can keep the responses in order
     *
     * @param parameters   array of String containing the parameters. 
     *                      <br><code>parameters[0]</code> argument is always the method name, 
     *                      <br><code>parameters[1]</code> is a delay which will delay the response by (delay*(workerid+1)) miliseconds.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see SMTPWorker#setPerWorkerDelay(int)
     */
    public void setPerWorkerDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setPerWorkerDelay: delayMsTimesWorkerID");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayMsTimesWorkerID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                SMTPWorker.setPerWorkerDelay(Integer.parseInt(parameters[1]));
                if(Integer.parseInt(parameters[1])>0)
                {
                    XTTProperties.printInfo(parameters[0] + ": setting per Worker delay to "+parameters[1]);
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": disabling per Worker delay");
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": '"+parameters[1]+"' is NOT a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": error setting OverrideSoapReturnCode");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }
    /**
     * Set a edelay for the SMSC befroe reading the next message. This should give time to check the recieved data before the next message arrives.
     * 
     * @param parameters   array of String containing the parameters. 
     *                      <code>parameters[0]</code> argument is always the method name, 
     *                      <code>parameters[1]</code> argument is delay time in miliseconds, 
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSDecoder
     */
    public void setNextMessageDelay(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setNextMessageDelay: delayinms");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": delayinms");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                int delay=Integer.decode(parameters[1]);
                SMTPWorker.setNextMessageDelay(delay);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[1]+"' is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }            
        }
    }
    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public String checkResources()
    {
        int standardPort = XTTProperties.getIntProperty("SMTPSERVER/PORT");

        String resourceString = null;

        try
        {
            if(standardPort>0)
            {
                java.net.ServerSocket stndrdPrt = new java.net.ServerSocket(standardPort);
                stndrdPrt.close();
            }
        } catch(Exception be)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_PORT+" '" +  standardPort + "'";
        }
        
        if(resourceString == null)
        {
            resourceString = "" + this.getClass().getName() + ":"+RESOURCE_OK;
        }
       
        return resourceString;
    }

    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SMTP.java,v 1.21 2010/05/05 08:12:37 rajesh Exp $";
}