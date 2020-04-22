package com.mobixell.xtt;

import java.util.Map;
import java.util.Collections;
import java.util.HashMap;
import java.util.ArrayList;
import java.net.*;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

/**
 * FunctionModule_SMS provides SMSCenter and SMS send functions.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule_SMS.java,v 1.64 2010/08/10 10:47:19 awadhai Exp $
 */
public class FunctionModule_SMS extends FunctionModule implements MMSConstants
{
    private Map<String,TPDU> tpdus          = Collections.synchronizedMap(new HashMap<String,TPDU>());
    private Map<String,RPDU> rpdus          = Collections.synchronizedMap(new HashMap<String,RPDU>());
    private Map<String,MM1Packet> mm1Packets= Collections.synchronizedMap(new HashMap<String,MM1Packet>());

    private SMSCServer s                 = null;
    private Thread ws                    = null;
    private final static String CRLF     = "\r\n";
    //private static HashMap receiveHeader = new HashMap();
    private static byte[] serverResponse = null;
    private String serverResponseCode[]  = new String[]{"","not yet initialized"};

    private String protocol = "null";
    private String msisdnHeader="x-msisdn";
    private StringBuffer additonalHeaders = new StringBuffer();
    private boolean additonalHeader = false;

    /**
     * Clears and reinitializes all the variables and SMSC workers.
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        //receiveHeader        = new HashMap();
        serverResponse       = null;
        serverResponseCode   = new String[]{"","not yet initialized"};
        SMSCServer.resetWorkerId();
        SMPPWorker.initialize();
        UCPWorker.initialize();
        CIMDWorker.initialize();
        String pw=XTTProperties.getProperty("SMSCSERVER/PASSWORD");
        setSMSCPassword(new String[]{this.getClass().getName()+".initialize()",pw},true);
        String head=XTTProperties.getProperty("SMSCSERVER/MM1MSISDNHEADER");
        if(head==null||head.equals("null"))
        {
            msisdnHeader="x-msisdn";
        } else
        {
            msisdnHeader=head;
        }
        String autostart=XTTProperties.getProperty("SMSCSERVER/STARTONLOAD");
        if(!autostart.equals("null"))
        {
            if(SMSCServer.checkSockets())
            {
                startSMSCServer(new String[]{"startSMSCServer"});
            }
        }
    }

    private class TPDU
    {
        private byte[] tpdu = new byte[0];
        public TPDU(String name)
        {
        }
        public byte[] getTPDUs()
        {
            return tpdu;
        }
        public void addTPDU(byte[] tpd)
        {
            int pointer=0;
            byte[] newtpdus=new byte[tpdu.length+tpd.length];
            pointer=ConvertLib.addBytesToArray(newtpdus,pointer,tpdu);
            pointer=ConvertLib.addBytesToArray(newtpdus,pointer,tpd);
            tpdu=newtpdus;
        }
    }
    private class RPDU
    {
        private byte[] rpdu = new byte[0];
        public RPDU(String name)
        {
        }
        public byte[] getRPDUs()
        {
            return rpdu;
        }
        public void addRPDU(byte[] rpd)
        {
            int pointer=0;
            byte[] newrpdus=new byte[rpdu.length+rpd.length];
            pointer=ConvertLib.addBytesToArray(newrpdus,pointer,rpdu);
            pointer=ConvertLib.addBytesToArray(newrpdus,pointer,rpd);
            rpdu=newrpdus;
        }
    }
    private class MM1Packet
    {
        private byte[] mandatorypacket = new byte[0];
        private byte[] optionalpacket = new byte[0];
        private byte[] content = new byte[0];
        private byte[] contenttype = new byte[0];
        public MM1Packet(String name, byte[] packet)
        {
            this.mandatorypacket=packet;
        }
        public void clearMandatoryPacket()
        {
            mandatorypacket = new byte[0];
        }
        public void clearOptionalPacket()
        {
            optionalpacket = new byte[0];
        }
        public void clearContent()
        {
            content = new byte[0];
        }
        public byte[] getContent()
        {
            return content;
        }
        public void setContent(byte[] cnt)
        {
            content = cnt;
        }
        public void setContentType(byte[] cntt)
        {
            contenttype = cntt;
        }
        public void addOptionalPacket(byte[] packet)
        {
            int pointer=0;
            byte[] newoptionalpacket=new byte[optionalpacket.length+packet.length];
            pointer=ConvertLib.addBytesToArray(newoptionalpacket,pointer,optionalpacket);
            pointer=ConvertLib.addBytesToArray(newoptionalpacket,pointer,packet);
            optionalpacket=newoptionalpacket;
        }
        public byte[] getPacket()
        {
            int pointer=0;
            byte[] fullpacket=new byte[mandatorypacket.length+optionalpacket.length+contenttype.length+content.length];
            pointer=ConvertLib.addBytesToArray(fullpacket,pointer,mandatorypacket);
            pointer=ConvertLib.addBytesToArray(fullpacket,pointer,optionalpacket);
            pointer=ConvertLib.addBytesToArray(fullpacket,pointer,contenttype);
            pointer=ConvertLib.addBytesToArray(fullpacket,pointer,content);
            return fullpacket;
        }
    }

    /**
     * constructor starts SMSC when SMSCSERVER/STARTONLOAD is activated.
     */
    public FunctionModule_SMS()
    {
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function modules SMS, UCP -->"
            +"\n    <SMSCServer>"
            +"\n        <!-- password for SMS connections to server, "
            +"\n             do not use this tag when no checking should happen -->"
            +"\n        <!--Password>password</Password-->"
            +"\n        <!-- the listening port of the smscserver -->"
            +"\n        <Port>2775</Port>"
            +"\n        <!-- protocol of the smsc server -->"
            +"\n        <!--protocol>SMPP</protocol-->"
            +"\n        <!--protocol>UCP</protocol-->"
            +"\n        <Protocol>SMPP</Protocol>"
            +"\n        <!-- time to wait on a \"wait\" function before continuing -->"
            +"\n        <waitTimeout>32000</waitTimeout>"
            +"\n        <!-- time to wait on read function before throwing exception 0=infinite -->"
            +"\n        <UCPReadTimeout>30000</UCPReadTimeout>"
            +"\n        <!-- Window size possible (1=default) -->"
            +"\n        <!--UCPWindowSize>10</UCPWindowSize-->"
            +"\n        <!-- response delay in ms for each request (0=default) -->"
            +"\n        <!--UCPResponseDelay>1000</UCPResponseDelay-->"
            +"\n        <!-- maximum open sessions on the server (0=default, unlimited) -->"
            +"\n        <!--UCPMaxSessions>0</UCPMaxSessions-->"
            +"\n        <!-- maximum open connections to the server (0=default, unlimited) -->"
            +"\n        <!--UCPMaxConnections>0</UCPMaxConnections-->"
            +"\n        <!-- session timeout in ms on the server (0=default, unlimited) -->"
            +"\n        <!--UCPSessionTimeout>0</UCPSessionTimeout-->"
            +"\n        <!-- read timeout in ms on the server (0=default, unlimited) -->"
            +"\n        <!--UCPReadTimeout>0</UCPReadTimeout-->"
            +"\n        <!-- means that injectAutoMessages is enabled and for each UCP51 an UCP53 will begenerated -->"
            +"\n        <!--InjectAutoMessages>true</InjectAutoMessages-->"
            +"\n        <!-- earliestAutoMessageSendTime in ms before sending the automatic message (0=default no delay) -->"
            +"\n        <!--EarliestAutoMessageSendTime>1000</EarliestAutoMessageSendTime-->"
            +"\n        <!-- latestAutoMessageSendTime in ms before sending the automatic message (use earliestAutoMessageSendTime or bigger) -->"
            +"\n        <!--LatestAutoMessageSendTime>20000</LatestAutoMessageSendTime-->"
            +"\n        <!-- autoMessageRetryTime in ms is how long the smsc waits on a connection"
            +"\n             for data before checking if it has to send out auto message (500=default) -->"
            +"\n        <!--AutoMessageRetryTime>500</AutoMessageRetryTime-->"
            +"\n        <SMPP>"
            +"\n            <!-- Return code used for the setOverrideReturnCode function -->"
            +"\n            <OverrideReturnCode>0x00000001</OverrideReturnCode>"
            +"\n        </SMPP>"
            +"\n        <UCP>"
            +"\n            <!-- Return code used for the setOverrideReturnCode function -->"
            +"\n            <OverrideReturnCode>24</OverrideReturnCode>"
            +"\n        </UCP>"
            +"\n        <!-- MM1 ip and port for downloading MMS in module SMS -->"
            +"\n        <MM1Ip>127.0.0.1</MM1Ip>"
            +"\n        <MM1Port>1111</MM1Port>"
            +"\n        <!-- MM1 timeout -->"
            +"\n        <MM1Timeout>30000</MM1Timeout>"
            +"\n        <!-- MSISDN header name for MM1 requests -->"
            +"\n        <MM1MSISDNHeader>X-MSISDN</MM1MSISDNHeader>"
            +"\n    </SMSCServer>";
    }

    /**
     * Overriden from superclass to add the SMSCServer,SMPPWorker and UCPWorker version numbers.
     *
     * @see SMSCServer
     * @see SMPPWorker
     * @see UCPWorker
     * @see CIMDWorker
     */
    public void printVersion()
    {
        super.printVersion();
        XTTProperties.printDebug(this.getClass().getName()+": SMSCServer: "+parseVersion(SMSCServer.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": SMPPWorker: "+parseVersion(SMPPWorker.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": UCPWorker : "+parseVersion(UCPWorker.tantau_sccsid));
        XTTProperties.printDebug(this.getClass().getName()+": CIMDWorker: "+parseVersion(CIMDWorker.tantau_sccsid));
    }
    public void showVersions()
    {
        super.showVersions();
        System.out.println(ConvertLib.createString(this.getClass().getName()+": SMSCServer: ",SHOWLENGTH) + parseVersion(SMSCServer.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": SMPPWorker: ",SHOWLENGTH) + parseVersion(SMPPWorker.tantau_sccsid));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": UCPWorker : ",SHOWLENGTH) + parseVersion(UCPWorker.tantau_sccsid ));
        System.out.println(ConvertLib.createString(this.getClass().getName()+": CIMDWorker: ",SHOWLENGTH) + parseVersion(CIMDWorker.tantau_sccsid));
    }
    /**
     * returns a byte array containing the body part of the last request.
     */
    public static byte[] getBody()
    {
        return serverResponse;
    }

    /**
     * starts the SMSCServer as an instance of SMSCServer.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name,<BR> <code>parameters[1]</code> argument is the SMSCServer port,<BR> <code>parameters[2]</code> is the
     *                     SMSCServer protocolle (SMPP/UCP). <BR>If UCP is used the following additional parameters can be used:<BR>
     *                     <code>parameters[3]</code> argument is the maxrequestcount before disconnecting (-1=default, unlimited).<BR>
     *                     <code>parameters[4]</code> argument is UCP Window size possible (1=default).<BR>
     *                     <code>parameters[5]</code> argument is UCP response delay in ms for each request (0=default).<BR>
     *                     <code>parameters[6]</code> argument is UCP maximum open sessions on the server (0=default, unlimited).<BR>
     *                     <code>parameters[7]</code> argument is UCP maximum open connections to the server (0=default, unlimited).<BR>
     *                     <code>parameters[8]</code> argument is UCP session timeout in ms on the server (0=default, unlimited).<BR>
     *                     <code>parameters[9]</code> argument is UCP read timeout in ms on the server (0=default, unlimited).<BR>
     *                     Additionally there can be:<BR>
     *                     <code>parameters[10]</code> argument means that injectAutoMessages is enabled and for each UCP51 an UCP53 will begenerated<BR>
     *                     <code>parameters[11]</code> argument is earliestAutoMessageSendTime in ms before sending the automatic message (0=default no delay).<BR>
     *                     <code>parameters[12]</code> argument is latestAutoMessageSendTime in ms before sending the automatic message (use earliestAutoMessageSendTime or bigger).<BR>
     *                     <code>parameters[13]</code> argument is autoMessageRetryTime in ms is how long the smsc waits on a connection for data before checking if it has to send out auto message (500=default).<BR>
     *                     <BR>If only <code>parameters[0]</code> is submitted earliestAutoMessageSendTime latestAutoMessageSendTime autoMessageRetryTime
     *                     the parameters will be taken from the configuration xml document in XTTProperties.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see SMSCServer
     * @see XTTProperties
     */
    public boolean startSMSCServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer: port protocol");
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer: port protocol maxrequestcount");
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer: port UCP maxrequestcount windowSize responseDelay maxSession maxConnection sessionTimeout readTimeout");
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer: port UCP maxrequestcount windowSize responseDelay maxSession maxConnection sessionTimeout readTimeout injectAutoMessages earliestAutoMessageSendTime latestAutoMessageSendTime autoMessageRetryTime");
            XTTProperties.printFail(this.getClass().getName()+": startSMSCServer: port SMPP maxrequestcount injectAutoMessages earliestAutoMessageSendTime latestAutoMessageSendTime autoMessageRetryTime");
            return false;
        }

        if(parameters.length==1)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting SMSC Server");
                this.protocol=XTTProperties.getProperty("SMSCSERVER/PROTOCOL");

                int maxreq          = -1;
                int windowSize      = 1;
                int responseDelay   = 0;
                int maxSession      = 0;
                int maxConnection   = 0;
                int sessionTimeout  = 0;
                int readTimeout     = 0;
                boolean injectAutoMessages=false;
                int earliestAutoMessageSendTime=0;
                int latestAutoMessageSendTime=0;
                int autoMessageRetryTime=500;

                maxreq=XTTProperties.getIntProperty("SMSCSERVER/MAXREQUESTCOUNT");
                if(maxreq<0){maxreq=-1;}

                if(protocol.equalsIgnoreCase("UCP"))
                {
                    windowSize=XTTProperties.getIntProperty("SMSCSERVER/UCPWINDOWSIZE");
                    if(windowSize<=1||!protocol.equalsIgnoreCase("UCP")){windowSize=1;}
                    responseDelay=XTTProperties.getIntProperty("SMSCSERVER/UCPRESPONSEDELAY");
                    if(responseDelay<=0||!protocol.equalsIgnoreCase("UCP")){responseDelay=0;}
                    maxSession=XTTProperties.getIntProperty("SMSCSERVER/UCPMAXSESSIONS");
                    if(maxSession<=0||!protocol.equalsIgnoreCase("UCP")){maxSession=0;}
                    maxConnection=XTTProperties.getIntProperty("SMSCSERVER/UCPMAXCONNECTIONS");
                    if(maxConnection<=0||!protocol.equalsIgnoreCase("UCP")){maxConnection=0;}
                    sessionTimeout=XTTProperties.getIntProperty("SMSCSERVER/UCPSESSIONTIMEOUT");
                    if(sessionTimeout<=0||!protocol.equalsIgnoreCase("UCP")){sessionTimeout=0;}
                    readTimeout=XTTProperties.getIntProperty("SMSCSERVER/UCPREADTIMEOUT");
                    if(readTimeout<=0||!protocol.equalsIgnoreCase("UCP")){readTimeout=0;}
                }
                String injectAutoMsg=XTTProperties.getQuietProperty("SMSCSERVER/INJECTAUTOMESSAGES");
                if(injectAutoMsg.equals("null"))
                {
                    injectAutoMessages=false;
                } else
                {
                    injectAutoMessages=true;
                }
                earliestAutoMessageSendTime=XTTProperties.getIntProperty("SMSCSERVER/EARLIESTAUTOMESSAGESENDTIME");
                if(earliestAutoMessageSendTime<=0){earliestAutoMessageSendTime=0;}
                latestAutoMessageSendTime=XTTProperties.getIntProperty("SMSCSERVER/LATESTAUTOMESSAGESENDTIME");
                if(latestAutoMessageSendTime<=0){latestAutoMessageSendTime=0;}
                autoMessageRetryTime=XTTProperties.getIntProperty("SMSCSERVER/AUTOMESSAGERETRYTIME");
                if(autoMessageRetryTime<=0){autoMessageRetryTime=500;}

                s = new SMSCServer(XTTProperties.getIntProperty("SMSCSERVER/PORT"), this.protocol,maxreq,windowSize,responseDelay,maxSession,sessionTimeout,readTimeout,maxConnection
                                    ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
                ws=(new Thread(s, "SMSCServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started SMSC Server");
                return true;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": "+e.getClass().getName()+": "+e.getMessage());
                //e.printStackTrace();
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else if (parameters.length>2||parameters.length<15)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Starting SMSC Server");
                this.protocol=parameters[2];
                int maxreq=-1;
                int windowSize=1;
                int responseDelay=0;
                int maxSession=0;
                int maxConnection=0;
                int sessionTimeout=0;
                int readTimeout=0;
                boolean injectAutoMessages=false;
                int earliestAutoMessageSendTime=0;
                int latestAutoMessageSendTime=0;
                int autoMessageRetryTime=500;

                if(parameters.length>3)maxreq=(Integer.decode(parameters[3])).intValue();
                if(this.protocol.equalsIgnoreCase("UCP"))
                {
                    int i=3;
                    try
                    {
                        windowSize      =(Integer.decode(parameters[++i])).intValue();
                        responseDelay   =(Integer.decode(parameters[++i])).intValue();
                        maxSession      =(Integer.decode(parameters[++i])).intValue();
                        maxConnection   =(Integer.decode(parameters[++i])).intValue();
                        sessionTimeout  =(Integer.decode(parameters[++i])).intValue();
                        readTimeout     =(Integer.decode(parameters[++i])).intValue();
                        String injectAutoMsg        =parameters[++i];
                        injectAutoMessages=ConvertLib.textToBoolean(injectAutoMsg);
                        earliestAutoMessageSendTime =(Integer.decode(parameters[++i])).intValue();
                        latestAutoMessageSendTime   =(Integer.decode(parameters[++i])).intValue();
                        autoMessageRetryTime        =(Integer.decode(parameters[++i])).intValue();
                    } catch (NumberFormatException nfe)
                    {
                        XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[i]+"' is not a number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return false;
                    } catch (ArrayIndexOutOfBoundsException aiex)
                    {
                        //whatever...
                    }
                } else if(this.protocol.equalsIgnoreCase("SMPP"))
                {
                    int i=3;
                    try
                    {
                        String injectAutoMsg        =parameters[++i];
                        injectAutoMessages=ConvertLib.textToBoolean(injectAutoMsg);
                        earliestAutoMessageSendTime =(Integer.decode(parameters[++i])).intValue();
                        latestAutoMessageSendTime   =(Integer.decode(parameters[++i])).intValue();
                        autoMessageRetryTime        =(Integer.decode(parameters[++i])).intValue();
                    } catch (NumberFormatException nfe)
                    {
                        XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[i]+"' is not a number");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return false;
                    } catch (ArrayIndexOutOfBoundsException aiex)
                    {
                        //whatever...
                    }
                }
                if(maxreq<0){maxreq=-1;}
                if(windowSize<=1){windowSize=1;}
                if(responseDelay<=0){responseDelay=0;}
                if(maxSession<=0){maxSession=0;}
                if(maxConnection<=0){maxConnection=0;}
                if(sessionTimeout<=0){sessionTimeout=0;}
                if(readTimeout<=0){readTimeout=0;}
                if(earliestAutoMessageSendTime<=0){earliestAutoMessageSendTime=0;}
                if(latestAutoMessageSendTime<=0){latestAutoMessageSendTime=0;}
                if(autoMessageRetryTime<=0){autoMessageRetryTime=500;}
                s = new SMSCServer(Integer.parseInt(parameters[1]), this.protocol,maxreq,windowSize,responseDelay,maxSession,sessionTimeout,readTimeout,maxConnection
                                    ,injectAutoMessages,earliestAutoMessageSendTime,latestAutoMessageSendTime,autoMessageRetryTime);
                ws=(new Thread(s, "SMSCServer"));
                ws.start();
                XTTProperties.printDebug(parameters[0] + ": Started SMSC Server");
                return true;
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port protocol");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port protocol maxrequestcount");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port UCP maxrequestcount windowSize responseDelay maxSession maxConnection sessionTimeout readTimeout");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port UCP maxrequestcount windowSize responseDelay maxSession maxConnection sessionTimeout readTimeout injectAutoMessages earliestAutoMessageSendTime latestAutoMessageSendTime autoMessageRetryTime");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": port SMPP maxrequestcount injectAutoMessages earliestAutoMessageSendTime latestAutoMessageSendTime autoMessageRetryTime");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        }
        return false;

    }

    /**
     * stops one/all SMSCServers and all it's threads.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired, the optional <code>parameters[1]</code> argument is the SMSCServer port
     *                     of the SMSCServer to stop, if omitted all running servers are stopped.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see SMSCServer#closeSocket(String)
     * @see SMSCServer#closeSockets()
     */
    public boolean stopSMSCServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopSMSCServer:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stopSMSCServer: port");
            return false;
        }
        if(parameters.length==2)
        {
            try
            {
                XTTProperties.printVerbose(parameters[0] + ": Stopping SMSC Server on port "+parameters[1]);
                SMSCServer.closeSocket(parameters[1]);
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        } else
        {
            XTTProperties.printWarn(parameters[0] + ": Stopping all SMSC Servers");
            try
            {
                SMSCServer.closeSockets();
                return true;
            }catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
        return false;
    }

    /**
     * wait for a SMPP Bind or an UCP60 message on the SMSC.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired , the optional <code>parameters[1]</code> argument can be an
     *                     ipaddress from which the connection is made OR it can be a systemId(SMPP) or OAdC(UCP).
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForBind()
     * @see UCPWorker#waitForBind(java.net.InetAddress)
     * @see UCPWorker#waitForBind(String)
     * @see SMPPWorker#waitForBind()
     * @see SMPPWorker#waitForBind(java.net.InetAddress)
     * @see SMPPWorker#waitForBind(String)
     * @see CIMDWorker#waitForBind()
     * @see CIMDWorker#waitForBind(java.net.InetAddress)
     * @see CIMDWorker#waitForBind(String)
     */
    public boolean waitForBind(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForBind:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": waitForBind: systemIdOrOAdC");
            XTTProperties.printFail(this.getClass().getName()+": waitForBind: ipAddress");
            return false;
        }
        try
        {
            if(parameters.length==2)
            {
                java.net.InetAddress remoteAddress=null;
                try
                {
                    String [] parts = parameters[1].split("\\056");
                    if  ( (parts.length == 4)
                        && ( (Integer.parseInt(parts[0]) >= 0) && (Integer.parseInt(parts[0]) < 256) )
                        && ( (Integer.parseInt(parts[1]) >= 0) && (Integer.parseInt(parts[1]) < 256) )
                        && ( (Integer.parseInt(parts[2]) >= 0) && (Integer.parseInt(parts[2]) < 256) )
                        && ( (Integer.parseInt(parts[3]) >= 0) && (Integer.parseInt(parts[3]) < 256) )
                    )
                    {
                        byte[] ip = new byte[4];
                        ip[0] = (byte) Integer.parseInt(parts[0]);
                        ip[1] = (byte) Integer.parseInt(parts[1]);
                        ip[2] = (byte) Integer.parseInt(parts[2]);
                        ip[3] = (byte) Integer.parseInt(parts[3]);
                        remoteAddress=InetAddress.getByAddress(ip);
                    }
                    else  if(parameters[1].contains(":")) //handle IPv6 address
                    {

                        byte[] ip = Inet6Address.getByName(parameters[1]).getAddress();
                        //System.out.println("ip.length = " + ip.length);
                        remoteAddress=Inet6Address.getByAddress(ip);
                        //System.out.println("remoteAddress = " + remoteAddress);
                    }
                } catch(Exception ex)
                {
                    remoteAddress=null;
                }

                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    if(remoteAddress==null)
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with systemid='"+parameters[1]+"' on SMSC");
                        SMPPWorker.waitForBind(parameters[1]);
                    } else
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with remoteAddress='"+remoteAddress+"' on SMSC");
                        SMPPWorker.waitForBind(remoteAddress);
                    }
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    if(remoteAddress==null)
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with OAdC='"+parameters[1]+"' on SMSC");
                        UCPWorker.waitForBind(parameters[1]);
                    } else
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with remoteAddress='"+remoteAddress+"' on SMSC");
                        UCPWorker.waitForBind(remoteAddress);
                    }
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    if(remoteAddress==null)
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with OAdC='"+parameters[1]+"' on SMSC");
                        CIMDWorker.waitForBind(parameters[1]);
                    } else
                    {
                        XTTProperties.printInfo(parameters[0] + ": waiting for Bind with remoteAddress='"+remoteAddress+"' on SMSC");
                        CIMDWorker.waitForBind(remoteAddress);
                    }
                } else if(protocol==null)
                {
                    XTTProperties.printFail(parameters[0] + ": SMSC not started propperly.");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return false;
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
            } else if (parameters.length==1)
            {
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForBind();
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForBind();
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForBind();
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
            } else
            {
                XTTProperties.printFail(parameters[1]+": waitForBind:"+NO_ARGUMENTS);
                XTTProperties.printFail(parameters[1]+": waitForBind: systemIdOrOAdC");
                XTTProperties.printFail(parameters[1]+": waitForBind: ipAddress");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                return false;
            }
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;
    }

    /**
     * set the password for SMPP and UCP on the SMSCServer
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters resets the password , the <code>parameters[1]</code> argument
     *                     sets the UCP and SMPP password to this value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForMessage(String)
     * @see SMPPWorker#waitForMessage(String)
     */
    public void setSMSCPassword(String parameters[])
    {
        setSMSCPassword(parameters,false);
    }
    private void setSMSCPassword(String parameters[], boolean quiet)
    {
        if(parameters==null)
        {
            //XTTProperties.printFail(this.getClass().getName()+": waitForMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setSMSCPassword: password");
            return;
        }

        if(parameters.length!=2||parameters[1].equals("null"))
        {
            SMPPWorker.setPassword("");
            UCPWorker.setPassword("");
            CIMDWorker.setPassword("");
            if(!quiet)XTTProperties.printInfo(parameters[0] + ": removed password on SMSC");
        } else
        {
            SMPPWorker.setPassword(parameters[1]);
            UCPWorker.setPassword(parameters[1]);
            CIMDWorker.setPassword(parameters[1]);
            if(!quiet)XTTProperties.printInfo(parameters[0] + ": setting password on SMSC to '"+parameters[1]+"'");
        }

    }
    /**
     * wait for a SMPP or an UCP message on the SMSCServer
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters are requeired , the optional <code>parameters[1]</code> argument
     *                     can be a systemId(SMPP) or OAdC(UCP).
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForMessage(String)
     * @see SMPPWorker#waitForMessage(String)
     */
    public boolean waitForMessage(String parameters[])
    {
        if(parameters==null)
        {
            //XTTProperties.printFail(this.getClass().getName()+": waitForMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": waitForMessage: systemIdOrOAdC");
            return false;
        }
        try
        {
            if(parameters.length==2)
            {
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForMessage(parameters[1]);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForMessage(parameters[1]);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForMessage(parameters[1]);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
            } else
            {
                XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": systemIdOrOAdC");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                return false;
                /*
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForMessage();
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForMessage();
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return false;
                }
                */
            }
            return true;
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return false;

    }

    /**
     * wait for a WSP message on the SMSCServer (SMPP or UCP)
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the number of messages received total since starting the SMSCServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForWSPMessages(int)
     * @see SMPPWorker#waitForWSPMessages(int)
     */
    public boolean waitForWSPMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForWSPMessages: numMessages");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numMessages");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" WSP-Messages on SMSC");
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForWSPMessages(messages);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForWSPMessages(messages);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForWSPMessages(messages);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
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
     * wait for a messages on the SMSCServer (SMPP or UCP or CIMD) (except bind)
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the number of messages received total since starting the SMSCServer
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForMessages(int)
     * @see SMPPWorker#waitForMessages(int)
     */
    public boolean waitForMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForMessages: numMessages");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numMessages");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" Messages on SMSC");
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForMessages(messages);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForMessages(messages);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForMessages(messages);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
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

    public boolean waitForTimeoutMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutMessages: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForTimeoutMessages: timeoutms maxPreviousMessages");
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
                        return false;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO Messages on SMSC");
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForTimeoutMessages(timeoutms,maxnumber);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForTimeoutMessages(timeoutms,maxnumber);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForTimeoutMessages(timeoutms,maxnumber);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
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

    public boolean waitForBinds(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForBinds: numMessages");
            return false;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": numMessages");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            try
            {
                int messages=Integer.parseInt(parameters[1]);
                XTTProperties.printInfo(parameters[0] + ": waiting for "+messages+" Messages on SMSC");
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForBinds(messages);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForBinds(messages);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForBinds(messages);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
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
     * wait for a NO WSP message on the SMSCServer (SMPP or UCP)
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the amount of time in ms to wait befor timing out with no message (success)
     *                     <code>parameters[2]</code> argument is the maximum number of messages that may already have been received
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForTimeoutWSPMessages(int,int)
     * @see SMPPWorker#waitForTimeoutWSPMessages(int,int)
     */
    public boolean waitForTimeoutWSPMessages(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForWSPMessages: timeoutms");
            XTTProperties.printFail(this.getClass().getName()+": waitForWSPMessages: timeoutms maxPreviousMessages");
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
                        return false;
                    }
                }
                XTTProperties.printInfo(parameters[0] + ": waiting for "+timeoutms+"ms and NO WSP-Messages on SMSC");
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.waitForTimeoutWSPMessages(timeoutms,maxnumber);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.waitForTimeoutWSPMessages(timeoutms,maxnumber);
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.waitForTimeoutWSPMessages(timeoutms,maxnumber);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
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
     * Set the returncode to respond after e certain numbers of requests to the configured return code. -1 to turn off.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters resets the password , the <code>parameters[1]</code> argument
     *                     sets the UCP and SMPP password to this value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#setOverrideReturnCode(int,int)
     * @see SMPPWorker#setOverrideReturnCode(int,int)
     */
    public void setOverrideReturnCode(String parameters[])
    {
        if(parameters==null)
        {
            //XTTProperties.printFail(this.getClass().getName()+": waitForMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setOverrideReturnCode: overideAfterNumResponses");
            return;
        }
        try
        {
            if(parameters.length==2)
            {
                int numMessages=0;
                try
                {
                    numMessages=Integer.decode(parameters[1]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[1]+"' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                int code=-1;
                code=XTTProperties.getIntProperty("SMSCSERVER/"+protocol+"/OVERRIDERETURNCODE");
                if(code<0)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: Configuration 'SMSCSERVER/"+protocol.toUpperCase()+"/OVERRIDERETURNCODE)' is invalid or SMSC not started");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.setOverrideReturnCode(code,numMessages);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.setOverrideReturnCode(code,numMessages);
                /*
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.setOverrideReturnCode(code,numMessages);
                */
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
            } else
            {
                XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": overideAfterNumResponses");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                return;
            }
            return;
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return;
    }

    /**
     * Set the returncode to respond after e certain numbers of requests to the configured return code. -1 to turn off.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, no additional parameters resets the password , the <code>parameters[1]</code> argument
     *                     sets the UCP and SMPP password to this value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#setOverrideReturnCode(int,int)
     * @see SMPPWorker#setOverrideReturnCode(int,int)
     */
    public void setOverridePattern(String parameters[])
    {
        if(parameters==null)
        {
            //XTTProperties.printFail(this.getClass().getName()+": waitForMessage:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": setOverridePattern: regexForRecipientNumber");
            return;
        }
        try
        {
            if(parameters.length==2)
            {
                int code=-1;
                code=XTTProperties.getIntProperty("SMSCSERVER/"+protocol+"/OVERRIDERETURNCODE");
                if(code<0)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: Configuration 'SMSCSERVER/"+protocol.toUpperCase()+"/OVERRIDERETURNCODE)' is invalid or SMSC not started");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                if(protocol.equalsIgnoreCase("SMPP"))
                {
                    SMPPWorker.setOverridePattern(code,parameters[1]);
                } else if(protocol.equalsIgnoreCase("UCP"))
                {
                    UCPWorker.setOverridePattern(code,parameters[1]);
                /*
                } else if(protocol.equalsIgnoreCase("CIMD"))
                {
                    CIMDWorker.setOverrideReturnCode(code,parameters[1]);
                */
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": Protocol not supported:"+protocol);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
            } else
            {
                XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": regexForRecipientNumber");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                return;
            }
            return;
        } catch(Exception e)
        {
            XTTProperties.printFail(parameters[0] + ": ERROR");
            XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
        return;
    }

    /**
     * wait for a NO WSP message on the SMSCServer (SMPP or UCP)
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the amount of time in ms to wait befor timing out with no message (success)
     *                     <code>parameters[2]</code> argument is the maximum number of messages that may already have been received
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see UCPWorker#waitForTimeoutWSPMessages(int,int)
     * @see SMPPWorker#waitForTimeoutWSPMessages(int,int)
     */
    public boolean sendMM1NotifyResponse(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMM1NotifyResponse: status transactionId msisdn");
            XTTProperties.printFail(this.getClass().getName()+": sendMM1NotifyResponse: status transactionId msisdn responseCode");
            return false;
        }
        if(parameters.length<4||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": status transactionId msisdn");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": status transactionId msisdn responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            int responseCode=-1;
            if(parameters.length>4)
            {
                try
                {
                    responseCode=Integer.decode(parameters[4]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[4]+"' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
            }
            int mmsstatus=-1;
            if(parameters[1].equalsIgnoreCase("expired"))
            {
                mmsstatus=128;
            } else if(parameters[1].equalsIgnoreCase("retrieved"))
            {
                mmsstatus=129;
            } else if(parameters[1].equalsIgnoreCase("rejected"))
            {
                mmsstatus=130;
            } else if(parameters[1].equalsIgnoreCase("deferred"))
            {
                mmsstatus=131;
            } else if(parameters[1].equalsIgnoreCase("unrecognised"))
            {
                mmsstatus=132;
            } else
            {
                XTTProperties.printFail(parameters[0] + ": invalid '"+parameters[1]+"' status, valid: expired,retrieved,rejected,deferred");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }

            byte[] transactionId=ConvertLib.getCOctetByteArrayFromString(parameters[2]);
            byte message[]=new byte[3+transactionId.length+4];

            message[0]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MESSAGE_TYPE+0x80,1)[0];
            message[1]=ConvertLib.getByteArrayFromInt(M_NOTIFYRESP_IND,1)[0];//m-notificationresp-ind

            message[2]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_TRANSACTION_ID+0x80,1)[0];//X-Mms-Transaction-Id
            for(int i=0;i<transactionId.length;i++)
            {
                message[3+i]=transactionId[i];
            }

            message[3+transactionId.length]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MMS_VERSION+0x80,1)[0];
            message[4+transactionId.length]=ConvertLib.getByteArrayFromInt(0x11+0x80,1)[0];

            message[5+transactionId.length]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_STATUS+0x80,1)[0];
            message[6+transactionId.length]=ConvertLib.getByteArrayFromInt(mmsstatus,1)[0];

            sendPostRequest(parameters[0],message,parameters[3],parameters[1], responseCode);
        }
        return false;

    }

    public void sendMM1AcknowledgeIndicator(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMM1AcknowledgeIndicator: reportAllowed transactionId msisdn");
            XTTProperties.printFail(this.getClass().getName()+": sendMM1AcknowledgeIndicator: reportAllowed transactionId msisdn responseCode");
            return;
        }
        if(parameters.length<4||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": reportAllowed transactionId msisdn");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": reportAllowed transactionId msisdn responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            int responseCode=-1;
            if(parameters.length>4)
            {
                try
                {
                    responseCode=Integer.decode(parameters[4]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[4]+"' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
            }
            int reportAllowed=-1;
            int reportlength=0;
            if(parameters[1].equals(""))
            {
                reportAllowed=-1;
            } else if(ConvertLib.textToBoolean(parameters[1]))//.equalsIgnoreCase("yes"))
            {
                reportAllowed=128;
                reportlength=2;
            } else// if(parameters[1].equalsIgnoreCase("no"))
            {
                reportAllowed=129;
                reportlength=2;
            }/* else
            {
                reportAllowed=-1;
            }*/

            byte[] transactionId=ConvertLib.getCOctetByteArrayFromString(parameters[2]);
            byte message[]=new byte[3+transactionId.length+2+reportlength];

            message[0]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MESSAGE_TYPE+0x80,1)[0];
            message[1]=ConvertLib.getByteArrayFromInt(M_ACKNOWLEDGE_IND,1)[0];//m-notificationresp-ind

            message[2]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_TRANSACTION_ID+0x80,1)[0];//X-Mms-Transaction-Id
            for(int i=0;i<transactionId.length;i++)
            {
                message[3+i]=transactionId[i];
            }

            message[3+transactionId.length]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MMS_VERSION+0x80,1)[0];
            message[4+transactionId.length]=ConvertLib.getByteArrayFromInt(0x11+0x80,1)[0];

            if(reportAllowed>0)
            {
                message[5+transactionId.length]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_REPORT_ALLOWED+0x80,1)[0];
                message[6+transactionId.length]=ConvertLib.getByteArrayFromInt(reportAllowed,1)[0];
            }
            sendPostRequest(parameters[0],message,parameters[3], responseCode);
        }
        return;

    }
//*
    public boolean sendMM1ReadRec(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMM1ReadRec: status messageid to from msisdn");
            XTTProperties.printFail(this.getClass().getName()+": sendMM1ReadRec: status messageid to from msisdn responseCode");
            return false;
        }
        if(parameters.length!=6)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": status messageid to from msisdn");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": status messageid to from msisdn responseCode");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return false;
        } else
        {
            int responseCode=-1;
            if(parameters.length>6)
            {
                try
                {
                    responseCode=Integer.decode(parameters[6]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[6]+"' is not a number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return false;
                }
            }
            int mmsstatus=-1;
            if(parameters[1].equalsIgnoreCase("read"))
            {
                mmsstatus=128;
            } else if(parameters[1].equalsIgnoreCase("deleted"))
            {
                mmsstatus=129;
            } else
            {
                XTTProperties.printFail(parameters[0] + ": invalid '"+parameters[1]+"' status, valid: read,deleted");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return false;
            }

            byte[] to=ConvertLib.getCOctetByteArrayFromString(parameters[3]);
            byte[] from=ConvertLib.getCOctetByteArrayFromString(parameters[4]);
            byte[] messageid=ConvertLib.getCOctetByteArrayFromString(parameters[2]);
            byte message[]=new byte[11+to.length+from.length+messageid.length];

            int pointer=0;
            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MESSAGE_TYPE+0x80,1)[0];
            message[pointer++]=ConvertLib.getByteArrayFromInt(M_READ_REC_IND,1)[0];//m-notificationresp-ind

            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_MMS_VERSION+0x80,1)[0];
            message[pointer++]=ConvertLib.getByteArrayFromInt(0x11+0x80,1)[0];

            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_TO+0x80,1)[0];//X-Mms-Transaction-Id
            for(int i=0;i<to.length;i++)
            {
                message[pointer++]=to[i];
            }
            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_FROM+0x80,1)[0];//X-Mms-Transaction-Id
            if(parameters[4].length()>0)
            {
                message[pointer++]=ConvertLib.getByteArrayFromInt(from.length+1,1)[0];
                message[pointer++]=ConvertLib.getByteArrayFromInt(128,1)[0];
                for(int i=0;i<from.length;i++)
                {
                    message[pointer++]=from[i];
                }
            } else
            {
                message[pointer++]=ConvertLib.getByteArrayFromInt(1,1)[0];
                message[pointer++]=ConvertLib.getByteArrayFromInt(129,1)[0];
            }
            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_MESSAGE_ID+0x80,1)[0];//X-Mms-Transaction-Id
            for(int i=0;i<messageid.length;i++)
            {
                message[pointer++]=messageid[i];
            }


            message[pointer++]=ConvertLib.getByteArrayFromInt(MMS_X_MMS_READ_STATUS+0x80,1)[0];
            message[pointer++]=ConvertLib.getByteArrayFromInt(mmsstatus,1)[0];

            sendPostRequest(parameters[0],message,parameters[5],parameters[1], responseCode);
        }
        return false;

    }
//*/

    private void sendPostRequest(String postfuncname,byte[] message, String msisdn, int responseCode)
    {
        sendPostRequest(postfuncname,message, msisdn,"", responseCode);
    }
    private void sendPostRequest(String postfuncname,byte[] message, String msisdn,String moreOutput, int responseCode)
    {
        java.net.Socket socket=null;
        try
        {
            String IP=XTTProperties.getProperty("SMSCSERVER/MM1Ip");
            int port = XTTProperties.getIntProperty("SMSCSERVER/MM1Port");
            if(IP.equals("null"))
            {
                IP=XTTProperties.getProperty("XMA/MM1Ip");
            }
            if(port<=0||port>=65536)
            {
                port = XTTProperties.getIntProperty("XMA/MM1Port");
            }
            String urlval="http://"+IP+":"+port;

            int timeout = XTTProperties.getIntProperty("SMSCSERVER/MM1TIMEOUT");
            if(timeout<0)timeout=30000;

            socket = new java.net.Socket(IP,port);
            socket.setSoTimeout(timeout);

            XTTProperties.printInfo(postfuncname+": "+moreOutput+": url: "+urlval);

            byte[] output=null;

            try
            {
                BufferedOutputStream conStream=new BufferedOutputStream(socket.getOutputStream());
                output=ConvertLib.getOctetByteArrayFromString("POST / HTTP/1.0"+CRLF
                                                             +"Content-Length: "+message.length+CRLF
                                                             +"Content-Type: "+"application/vnd.wap.mms-message"+CRLF
                                                             +msisdnHeader+": "+msisdn+CRLF
                                                             +CRLF);
                if(additonalHeader)
                {
                    output=ConvertLib.getOctetByteArrayFromString("POST / HTTP/1.0"+CRLF
                            +"Content-Length: "+message.length+CRLF
                            +"Content-Type: "+"application/vnd.wap.mms-message"+CRLF
                            +msisdnHeader+": "+msisdn
                            +additonalHeaders.toString()+CRLF
                            +CRLF);
 
                }
                XTTProperties.printDebug(postfuncname + ": sending Head:\n"+ConvertLib.getHexView(output,0,output.length));
                conStream.write(output,0,output.length);
                XTTProperties.printDebug(postfuncname + ": sending Body:\n"+ConvertLib.getHexView(message,0,message.length));
                conStream.write(message,0,message.length);
                conStream.flush();
                XTTProperties.printDebug(postfuncname + ": finished sending");
            } catch (Exception e)
            {
                XTTProperties.printFail(postfuncname + ": POST ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            serverResponse=new byte[0];
            serverResponse=doRead(postfuncname,socket.getInputStream());
            try
            {
                serverResponseCode[0]="-1";
                serverResponseCode[1]="unknown";
                String resp=ConvertLib.createString(serverResponse);
                String[] respLines=resp.split("\\r\\n");
                String[] codes=respLines[0].split("\\s");
                serverResponseCode[0]=codes[1];
                serverResponseCode[1]=codes[2];
            } catch (Exception e){}

            XTTProperties.printDebug(postfuncname + ": Response Body ("+serverResponse.length+" Bytes) START:\n"+ConvertLib.getHexView(serverResponse,0,XTTProperties.getIntProperty("BUFFEROUTPUTSIZE"))+"\n"
                                    +postfuncname + ": Response END:");

            if(responseCode>0&&(!serverResponseCode[0].equals(""+responseCode)))
            {
                XTTProperties.printFail(postfuncname + ": invalid response code '"+serverResponseCode[0]+"' returned, wanted '"+responseCode+"'");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }


            } catch (Exception e)
            {
                if(e.getCause()!=null&&e.getCause().getClass().getName().equals("java.net.UnknownHostException"))
                {
                    XTTProperties.printFail(this.getClass().getName()+"."+postfuncname + ": UnknownHostException");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                } else if(e.getCause()!=null&&e.getCause().getClass().getName().equals("java.net.ProtocolException"))
                {
                    XTTProperties.printFail(this.getClass().getName()+"."+postfuncname + ": ProtocolException: "+e.getCause().getMessage());
                    if(XTTProperties.printDebug(null))
                        XTTProperties.printException(e);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }

                String errormessage=e.getMessage();
                XTTProperties.printFail(this.getClass().getName()+"."+postfuncname + ": " +e.getClass().getName()+" :"+ errormessage);
                if(XTTProperties.printDebug(null))
                    XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                    //e.printStackTrace();
            } finally
            {
                try
                {
                    socket.close();
                } catch (Exception ex){}
            }
    }

    /**
     * starts the SMSCServer as an instance of SMSCServer. The method also automatically sets the Accpet, User-Agent and host headers. Returned data
     * is decoded as MMS and stored in SMS/[msisdn].
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the msisdn for the x-msisdn header, <code>parameters[2]</code> is the url, <code>parameters[3]</code> is the User-Agent.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see MMSDecoder
     */
    public void sendGetRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest: msisdn URL");
            XTTProperties.printFail(this.getClass().getName()+": sendGetRequest: msisdn URL userAgent");
            return;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": msisdn URL");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": msisdn URL userAgent");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String userAgent="User-Agent-1";
            if(parameters.length==4)userAgent=parameters[3];
            sendGetRequest(parameters[0],parameters[2],parameters[1],"",userAgent);
        }
    }
    private void sendGetRequest(String getfuncname,String url, String msisdn,String moreOutput,String userAgent)
    {
        java.net.Socket socket=null;
        try
        {
            if(userAgent==null||userAgent.equals(""))userAgent="User-Agent-1";
            String urlsplit[]=url.split("/",4);
            String IP=urlsplit[2].split(":")[0];
            int port = Integer.parseInt(urlsplit[2].split(":")[1]);
            String urlval="http://"+IP+":"+port;
            String dir=urlsplit[3];

            int timeout = XTTProperties.getIntProperty("SMSCSERVER/MM1TIMEOUT");
            if(timeout<0)timeout=30000;
            socket = new java.net.Socket(IP,port);
            socket.setSoTimeout(timeout);

            XTTProperties.printInfo(getfuncname+": "+moreOutput+": url: "+urlval);

            byte[] output=null;

            try
            {
                BufferedOutputStream conStream=new BufferedOutputStream(socket.getOutputStream());
                output=ConvertLib.getOctetByteArrayFromString("GET /"+dir+" HTTP/1.0"+CRLF
                                                             +"Accept: "+"text/vnd.wap.wml, application/xml, image/gif, image/jpeg, application/vnd.wap.mms-message"+CRLF
                                                             +"User-Agent: "+userAgent+CRLF
                                                             +"Host: "+IP+CRLF
                                                             +msisdnHeader+": "+msisdn+CRLF
                                                             +CRLF);
                XTTProperties.printDebug(getfuncname + ": sending Head:\n"+ConvertLib.getHexView(output,0,output.length));
                conStream.write(output,0,output.length);
                conStream.flush();
                XTTProperties.printDebug(getfuncname + ": finished sending");
            } catch (Exception e)
            {
                XTTProperties.printFail(getfuncname + ": GET ERROR");
                XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            serverResponse=new byte[0];
            serverResponse=doRead(getfuncname,socket.getInputStream());
            try
            {
                serverResponseCode[0]="-1";
                serverResponseCode[1]="unknown";
                String resp=ConvertLib.createString(serverResponse);
                String[] respLines=resp.split("\\r\\n");
                String[] codes=respLines[0].split("\\s");
                serverResponseCode[0]=codes[1];
                serverResponseCode[1]=codes[2];
            } catch (Exception e){}

            XTTProperties.printDebug(getfuncname + ": Response Body ("+serverResponse.length+" Bytes) START:\n"+ConvertLib.getHexView(serverResponse,0,XTTProperties.getIntProperty("BUFFEROUTPUTSIZE"))+"\n"
                                    +getfuncname + ": Response END:");

            int headlength=(ConvertLib.createString(serverResponse)).split("\\r\\n\\r\\n")[0].length()+4;

            MMSDecoder d=new MMSDecoder(serverResponse,headlength,"SMS/"+msisdn);
            d.decode();


        } catch (Exception e)
        {
            if(e.getCause()!=null&&e.getCause().getClass().getName().equals("java.net.UnknownHostException"))
            {
                XTTProperties.printFail(this.getClass().getName()+"."+getfuncname + ": UnknownHostException");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            } else if(e.getCause()!=null&&e.getCause().getClass().getName().equals("java.net.ProtocolException"))
            {
                XTTProperties.printFail(this.getClass().getName()+"."+getfuncname + ": ProtocolException: "+e.getCause().getMessage());
                if(XTTProperties.printDebug(null))
                    XTTProperties.printException(e);
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            String errormessage=e.getMessage();
            XTTProperties.printFail(this.getClass().getName()+"."+getfuncname + ": " +e.getClass().getName()+" :"+ errormessage);
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(e);
            XTTProperties.setTestStatus(XTTProperties.FAILED);
                //e.printStackTrace();
        }
    }

    private byte[] doRead(String funcname,java.io.InputStream inS) throws java.io.IOException
    {
        byte response[]=null;
        BufferedInputStream in;
        XTTProperties.printVerbose(funcname+": downloading response: size Unknown");
        in=new BufferedInputStream(inS);
        byte buffer[]=new byte[1024];
        ArrayList<ByteArrayWrapper> bufferSet=new ArrayList<ByteArrayWrapper>();
        int thebyte=0;
        int byteCounter=-1;
        try
        {
            while(thebyte!=-1)
            {
                byteCounter++;
                if((byteCounter%1024)==0)
                {
                    XTTProperties.printDebug(funcname + ": Reading 1024 Bytes:");
                    buffer=new byte[1024];
                    bufferSet.add(new ByteArrayWrapper(buffer));
                }
                thebyte=in.read();
                //System.out.print(byteCounter%1024+" ");
                //System.out.print(((char)thebyte));
                buffer[byteCounter%1024]=(new Integer(thebyte)).byteValue();
            }
        }catch (java.net.SocketTimeoutException ex)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ex);
            }
        }
        response=new byte[byteCounter];
        Object bytes[]=bufferSet.toArray();
        int end=1024;
        int count=0;
        for(int i=0;i<bytes.length;i++)
        {
            if(i==(bytes.length-1))end=byteCounter%1024;
            for(int j=0;j<end;j++)
            {
                response[count++]=((ByteArrayWrapper)bytes[i]).getArray()[j];
            }
        }
        return response;
    }
//*/



    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> argument is the post field name, <code>parameters[2]</code>
     *                     argument and following are concatenated together to the header value.
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     */
    public void checkResponseCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode: expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            boolean found=false;
            StringBuffer checked=new StringBuffer();
            String divider="";
            for(int i=1;i<parameters.length;i++)
            {
                if(serverResponseCode[0].equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider=",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": found "+serverResponseCode[0]+" "+serverResponseCode[1]);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": found "+serverResponseCode[0]+" expected "+checked.toString());
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
                SMPPWorker.setNextMessageDelay(delay);
                UCPWorker.setNextMessageDelay(delay);
                CIMDWorker.setNextMessageDelay(delay);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[1]+"' is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }            
        }
    }

    /** Create an SMS TP packet.
    Allowed parameters:<br>
name SMS_DELIVER TP_MMS TP_RP TP_UDHI TP_SRI TP_OA_TYPE TP_OA_PLAN TP_OA_NUMBER TP_PID TP_DCS TP_SCTS TP_UDL TP_UD_base64Encoded<br>
name SMS_DELIVER_REPORT TP_UDHI TP_FCS TP_PI TP_PID TP_DCS TP_UDL TP_UD_base64Encoded<br>
name SMS_SUBMIT TP_RD TP_VPF TP_RP TP_UDHI TP_SRR TP_MR TP_DA_TYPE TP_DA_PLAN TP_DA_NUMBER TP_PID TP_DCS TP_VP TP_UDL TP_UD_base64Encoded<br>
name SMS_SUBMIT_REPORT TP_UDHI TP_FCS TP_PI TP_SCTS TP_PID TP_DCS TP_UDL TP_UD_base64Encoded<br>
name SMS_STATUS_REPORT TP_UDHI TP_MMS TP_SRQ TP_MR TP_RA_TYPE TP_RA_PLAN TP_RA_NUMBER TP_SCTS TP_DT TP_ST TP_PI TP_PID TP_DCS TP_UDL TP_UD_base64Encoded<br>
name SMS_COMMAND TP_UDHI TP_SRR TP_MR TP_PID TP_CT TP_MN TP_DA_TYPE TP_DA_PLAN TP_DA_NUMBER TP_CDL TP_CD_base64Encoded<br>
    */
    public void createTPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createTPPacket: name messageType moreOptions...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name messageType moreOptions...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                String name=parameters[1];
                TPDU tp=new TPDU(name);
                byte[] fulldata=null;

                String messagetype=parameters[2];
                if(messagetype.equalsIgnoreCase("SMS_DELIVER"))
                {
                    /*
                    TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the message type.
                    TP MMS	TP More Messages to Send	    M	b	Parameter indicating whether or not there are more messages to send
                    TP RP	TP Reply Path	                M	b	Parameter indicating that Reply Path exists.
                    TP UDHI	TP User Data Header Indicator	O	b	Parameter indicating that the TP UD field contains a Header
                    TP SRI	TP Status Report Indication	    O	b	Parameter indicating if the SME has requested a status report.
                    TP OA	TP Originating Address	        M	2 12o	Address of the originating SME.
                    TP PID	TP Protocol Identifier	        M	o	Parameter identifying the above layer protocol, if any.
                    TP DCS	TP Data Coding Scheme	        M	o	Parameter identifying the coding scheme within the TP User Data.
                    TP SCTS	TP Service Centre Time Stamp	M	7o	Parameter identifying time when the SC received the message.
                    TP UDL	TP User Data Length	            M	I	Parameter indicating the length of the TP User Data field to follow.
                    TP UD	TP User Data	                O	3)
                    */
                    if(parameters.length!=15)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_DELIVER TP_MMS TP_RP TP_UDHI TP_SRI TP_OA_TYPE TP_OA_PLAN TP_OA_NUMBER TP_PID TP_DCS TP_SCTS TP_UDL TP_UD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=0;
                    byte[] first=new byte[]{(byte)mtype};
                    // TP MMS
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(4);
                    }
                    //TP RP
                    if(ConvertLib.textToBoolean(parameters[4]))
                    {
                        mtype=mtype|(128);
                    }
                    // RP UDHI
                    if(ConvertLib.textToBoolean(parameters[5]))
                    {
                        mtype=mtype|(64);
                    }
                    //TP SRI
                    if(ConvertLib.textToBoolean(parameters[6]))
                    {
                        mtype=mtype|(32);
                    }
                    first[0]=(byte)mtype;
                    // TP OA
                    byte[] tpoa=create3GPPAdressField(parameters[7],parameters[8],parameters[9],true);
                    // TP PID
                    byte[] pid=new byte[1];
                    pid[0]=(byte)(Integer.decode(parameters[10]).intValue());
                    /// TP DCS	(0 should work for plain text 7bit GSM, 4 for 8bit data)
                    byte[] dcs=new byte[1];
                    dcs[0]=(byte)(Integer.decode(parameters[11]).intValue());
                    // TP SCTS	YYMMDDHHMMSSZZ
                    byte[] scts=ConvertLib.getSemiOctetFromNumber(parameters[12]);
                    // TP UDL
                    byte[] udl=new byte[1];
                    udl[0]=(byte)(Integer.decode(parameters[13]).intValue());
                    // TP UD
                    byte[] ud=new byte[0];
                    if(parameters[14].length()>0)
                    {
                        ud=ConvertLib.base64Decode(parameters[14]);
                    }
                    fulldata=new byte[first.length+tpoa.length+pid.length+dcs.length+scts.length+udl.length+ud.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,tpoa);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,scts);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,udl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ud);
                } else if(messagetype.equalsIgnoreCase("SMS_DELIVER_REPORT"))
                {
                    /*
                        TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the message type
                        TP-UDHI	TP-User-Data-Header-Indication	O	b	Parameter indicating that the TP-UD field contains a Header
                        TP FCS	TP Failure Cause	            M	I	Parameter indicating the reason for SMS DELIVER failure
                        TP PI	TP Parameter Indicator	        M	o	Parameter indicating the presence of any of the optional parameters which follow
                        TP PID	TP Protocol Identifier	        O	o	see clause 9.2.3.9
                        TP DCS	TP Data Coding Scheme	        O	o	see clause 9.2.3.10
                        TP UDL	TP User Data Length	            O	o	see clause 9.2.3.16
                        TP UD	TP User Data	                O	3) 4)	see clause 9.2.3.24
                    */
                    if(parameters.length<6||parameters.length>10)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_DELIVER_REPORT TP_UDHI TP_FCS TP_PI TP_PID TP_DCS TP_UDL TP_UD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=0;
                    byte[] first=new byte[]{(byte)mtype};
                    // RP UDHI
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(64);
                    }
                    first[0]=(byte)mtype;
                    //TP FCS
                    byte[] fcs=new byte[0];
                    if(parameters[4].length()>0)
                    {
                        fcs=new byte[1];
                        fcs[0]=(byte)(Integer.decode(parameters[4]).intValue());
                    }

                    //TP PI:
                    //bit 7	        bit 6	    bit 5	    bit 4	    bit 3	    bit 2	bit 1	bit 0
                    //Extension bit	Reserved	Reserved	Reserved	Reserved	TP UDL	TP DCS	TP PID
                    byte[] pi=new byte[1];
                    pi[0]=(byte)(Integer.decode(parameters[5]).intValue());

                    // TP PID
                    byte[] pid=new byte[0];
                    if((pi[0]&(1))>0)
                    {
                        pid=new byte[1];
                        pid[0]=(byte)(Integer.decode(parameters[6]).intValue());
                    }
                    // TP DCS	(0 should work for plain text 7bit GSM, 4 for 8bit data)
                    byte[] dcs=new byte[0];
                    if((pi[0]&(2))>0)
                    {
                        dcs=new byte[1];
                        dcs[0]=(byte)(Integer.decode(parameters[7]).intValue());
                    }
                    // TP UDL
                    byte[] udl=new byte[0];
                    byte[] ud=new byte[0];
                    if((pi[0]&(4))>0)
                    {
                        udl=new byte[1];
                        udl[0]=(byte)(Integer.decode(parameters[8]).intValue());
                        // TP UD
                        if(parameters[9].length()>0)
                        {
                            ud=ConvertLib.base64Decode(parameters[9]);
                        }
                    }

                    fulldata=new byte[first.length+fcs.length+pi.length+pid.length+dcs.length+udl.length+ud.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pi);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,udl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ud);
                } else if(messagetype.equalsIgnoreCase("SMS_SUBMIT"))
                {
                    /*
                    TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the message type.
                    TP RD	TP Reject Duplicates	        M	b	Parameter indicating whether or not the SC shall accept an SMS SUBMIT for an SM still held in the SC which has the same TP MR and the same TP DA as a previously submitted SM from the same OA
                    TP VPF	TP Validity Period Format	    M	2b	Parameter indicating whether or not the TP VP field is present.
                    TP RP	TP Reply Path	                M	b	Parameter indicating the request for Reply Path.
                    TP UDHI	TP User Data Header Indicator	O	b	Parameter indicating that the TP UD field contains a Header.
                    TP SRR	TP Status Report Request	    O	b	Parameter indicating if the MS is requesting a status report.
                    TP MR	TP Message Reference	        M	I	Parameter identifying the SMS SUBMIT.
                    TP DA	TP Destination Address	        M	2 12o	Address of the destination SME.
                    TP PID	TP Protocol Identifier	        M	o	Parameter identifying the above layer protocol, if any.
                    TP DCS	TP Data Coding Scheme	        M	o	Parameter identifying the coding scheme within the TP User Data.
                    TP VP	TP Validity Period	            O	o/7o	Parameter identifying the time from where the message is no longer valid.
                    TP UDL	TP User Data Length	            M	I	Parameter indicating the length of the TP User Data field to follow.
                    TP UD	TP User Data	                O	3)
                    */
                    if(parameters.length!=17)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_SUBMIT TP_RD TP_VPF TP_RP TP_UDHI TP_SRR TP_MR TP_DA_TYPE TP_DA_PLAN TP_DA_NUMBER TP_PID TP_DCS TP_VP TP_UDL TP_UD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=1;
                    byte[] first=new byte[]{(byte)mtype};
                    // TP RD
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(4);
                    }
                    // TP VPF
                    int vpf=Integer.decode(parameters[4]);
                    mtype=mtype|(vpf<<3);
                    // TP RP
                    if(ConvertLib.textToBoolean(parameters[5]))
                    {
                        mtype=mtype|(128);
                    }
                    // TP UDHI
                    if(ConvertLib.textToBoolean(parameters[6]))
                    {
                        mtype=mtype|(64);
                    }
                    // TP SRR
                    if(ConvertLib.textToBoolean(parameters[7]))
                    {
                        mtype=mtype|(32);
                    }
                    first[0]=(byte)mtype;
                    //TP MR
                    byte[] mr=new byte[1];
                    mr[0]=(byte)(Integer.decode(parameters[8]).intValue());
                    // TP DA
                    byte[] tpda=create3GPPAdressField(parameters[9],parameters[10],parameters[11],true);
                    // TP PID
                    byte[] pid=new byte[1];
                    pid[0]=(byte)(Integer.decode(parameters[12]).intValue());
                    // TP DCS	(0 should work for plain text 7bit GSM, 4 for 8bit data)
                    byte[] dcs=new byte[1];
                    dcs[0]=(byte)(Integer.decode(parameters[13]).intValue());
                    // TP VP
                    byte[] vp=new byte[0];
                    /*
                    0	0	TP VP field not present
                	1	0	TP VP field present - relative format
                	0	1	TP-VP field present - enhanced format
                	1	1	TP VP field present - absolute format
	                */
                    switch(vpf)
                    {
                        default:
                        case 0:
                            break;
                        case 1:
                            vp=ConvertLib.getByteArrayFromLong(Long.decode(parameters[14]),7);
                            break;
                        case 2:
                            vp=new byte[1];
                            vp[0]=(byte)(Integer.decode(parameters[14]).intValue());
                            break;
                        case 3:
                            //YYMMDDHHMMSSZZ
                            vp=ConvertLib.getSemiOctetFromNumber(parameters[14]);
                            break;
                    }
                    // TP UDL
                    byte[] udl=new byte[1];
                    udl[0]=(byte)(Integer.decode(parameters[15]).intValue());
                    byte[] ud=new byte[0];
                    if(parameters[16].length()>0)
                    {
                        ud=ConvertLib.base64Decode(parameters[16]);
                    }

                    fulldata=new byte[first.length+mr.length+tpda.length+pid.length+dcs.length+vp.length+udl.length+ud.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mr);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,tpda);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,vp);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,udl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ud);

                } else if(messagetype.equalsIgnoreCase("SMS_SUBMIT_REPORT"))
                {
                    /*
                    TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the message type
                    TP-UDHI	TP-User-Data-Header-Indication	O	b	Parameter indicating that the TP-UD field contains a Header
                    TP FCS	TP Failure Cause	            M	I	Parameter indicating the reason for SMS SUBMIT failure
                    TP PI	TP Parameter Indicator	        M	o	Parameter indicating the presence of any of the optional parameters which follow
                    TP SCTS	TP Service Centre Time Stamp	M	7o  5)	Parameter identifying the time when the SC received the SMS SUBMIT See clause 9.2.3.11
                    TP PID	TP Protocol Identifier	        O	o	See clause 9.2.3.9
                    TP DCS	TP Data Coding Scheme	        O	o	see clause 9.2.3.10
                    TP UDL	TP User Data Length	            O	o	see clause 9.2.3.16
                    TP UD	TP User Data	                O	3) 4)	see clause 9.2.3.24
                    */
                    if(parameters.length<7||parameters.length>11)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_SUBMIT_REPORT TP_UDHI TP_FCS TP_PI TP_SCTS TP_PID TP_DCS TP_UDL TP_UD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=1;
                    byte[] first=new byte[]{(byte)mtype};
                    // RP UDHI
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(64);
                    }
                    first[0]=(byte)mtype;
                    //TP FCS
                    byte[] fcs=new byte[1];
                    fcs[0]=(byte)(Integer.decode(parameters[4]).intValue());

                    //TP PI:
                    //bit 7	        bit 6	    bit 5	    bit 4	    bit 3	    bit 2	bit 1	bit 0
                    //Extension bit	Reserved	Reserved	Reserved	Reserved	TP UDL	TP DCS	TP PID
                    byte[] pi=new byte[1];
                    pi[0]=(byte)(Integer.decode(parameters[5]).intValue());

                    // TP SCTS	YYMMDDHHMMSSZZ
                    byte[] scts=ConvertLib.getSemiOctetFromNumber(parameters[6]);

                    // TP PID
                    byte[] pid=new byte[0];
                    if((pi[0]&(1))>0)
                    {
                        pid=new byte[1];
                        pid[0]=(byte)(Integer.decode(parameters[7]).intValue());
                    }
                    // TP DCS	(0 should work for plain text 7bit GSM, 4 for 8bit data)
                    byte[] dcs=new byte[0];
                    if((pi[0]&(2))>0)
                    {
                        dcs=new byte[1];
                        dcs[0]=(byte)(Integer.decode(parameters[8]).intValue());
                    }
                    // TP UDL
                    byte[] udl=new byte[0];
                    byte[] ud=new byte[0];
                    if((pi[0]&(4))>0)
                    {
                        udl=new byte[1];
                        udl[0]=(byte)(Integer.decode(parameters[9]).intValue());
                        // TP UD
                        if(parameters[10].length()>0)
                        {
                            ud=ConvertLib.base64Decode(parameters[10]);
                        }
                    }
                    fulldata=new byte[first.length+fcs.length+pi.length+scts.length+pid.length+dcs.length+udl.length+ud.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pi);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,scts);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,udl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ud);

                } else if(messagetype.equalsIgnoreCase("SMS_STATUS_REPORT"))
                {
                    /*
                    TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the message type
                    TP-UDHI	TP-User-Data-Header-Indication	O	b	Parameter indicating that the TP-UD field contains a Header
                    TP MMS	TP More Messages to Send	    M	b	Parameter indicating whether or not there are more messages to send
                    TP SRQ	TP Status Report Qualifier	    M	b	Parameter indicating whether the previously submitted TPDU was an SMS-SUBMIT or an SMS COMMAND
                    TP MR	TP Message Reference 3)	        M	I	Parameter identifying the previously submitted SMS SUBMIT or SMS COMMAND
                    TP RA	TP Recipient Address	        M	2 12o	Address of the recipient of the previously submitted mobile originated short message
                    TP SCTS	TP Service Centre Time Stamp	M	7o	Parameter identifying time when the SC received the previously sent SMS SUBMIT
                    TP DT	TP Discharge Time	            M	7o	Parameter identifying the time associated with a particular TP ST outcome
                    TP ST	TP Status	                    M	o	Parameter identifying the status of the previously sent mobile originated short message
                    TP-PI	TP-Parameter-Indicator	        O 4)o	Parameter indicating the presence of any of the optional parameters which follow
                    TP-PID	TP-Protocol-Identifier	        O	o	see clause 9.2.3.9. TP-PID of original SMS-SUBMIT
                    TP-DCS	TP-Data-Coding-Scheme	        O	o	see clause 9.2.3.10
                    TP-UDL	TP-User-Data-Length	            O	o	see clause 9.2.3.16
                    TP-UD	TP-User-Data	                O	5)	see clause 9.2.3.24
                    */
                    if(parameters.length<14||parameters.length>18)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_STATUS_REPORT TP_UDHI TP_MMS TP_SRQ TP_MR TP_RA_TYPE TP_RA_PLAN TP_RA_NUMBER TP_SCTS TP_DT TP_ST TP_PI TP_PID TP_DCS TP_UDL TP_UD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=2;
                    byte[] first=new byte[]{(byte)mtype};
                    // RP UDHI
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(64);
                    }
                    // TP MMS
                    if(ConvertLib.textToBoolean(parameters[4]))
                    {
                        mtype=mtype|(4);
                    }
                    //TP SRQ
                    if(ConvertLib.textToBoolean(parameters[5]))
                    {
                        mtype=mtype|(32);
                    }
                    first[0]=(byte)mtype;
                    //TP MR
                    byte[] mr=new byte[1];
                    mr[0]=(byte)(Integer.decode(parameters[6]).intValue());
                    // TP RA
                    byte[] ra=create3GPPAdressField(parameters[7],parameters[8],parameters[9],true);
                    // TP SCTS	YYMMDDHHMMSSZZ
                    byte[] scts=ConvertLib.getSemiOctetFromNumber(parameters[10]);
                    //TP DT
                    byte[] dt=ConvertLib.getSemiOctetFromNumber(parameters[11]);
                    //TP ST
                    byte[] st=new byte[1];
                    st[0]=(byte)(Integer.decode(parameters[12]).intValue());

                    //TP PI:
                    //bit 7	        bit 6	    bit 5	    bit 4	    bit 3	    bit 2	bit 1	bit 0
                    //Extension bit	Reserved	Reserved	Reserved	Reserved	TP UDL	TP DCS	TP PID
                    byte[] pi=new byte[1];
                    pi[0]=(byte)(Integer.decode(parameters[13]).intValue());
                    // TP PID
                    byte[] pid=new byte[0];
                    if((pi[0]&(1))>0)
                    {
                        pid=new byte[1];
                        pid[0]=(byte)(Integer.decode(parameters[14]).intValue());
                    }
                    // TP DCS	(0 should work for plain text 7bit GSM, 4 for 8bit data)
                    byte[] dcs=new byte[0];
                    if((pi[0]&(2))>0)
                    {
                        dcs=new byte[1];
                        dcs[0]=(byte)(Integer.decode(parameters[15]).intValue());
                    }
                    // TP UDL
                    byte[] udl=new byte[0];
                    byte[] ud=new byte[0];
                    if((pi[0]&(4))>0)
                    {
                        udl=new byte[1];
                        udl[0]=(byte)(Integer.decode(parameters[16]).intValue());
                        // TP UD
                        if(parameters[17].length()>0)
                        {
                            ud=ConvertLib.base64Decode(parameters[17]);
                        }
                    }

                    fulldata=new byte[first.length+mr.length+ra.length+scts.length+dt.length+st.length+pi.length+pid.length+dcs.length+udl.length+ud.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mr);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ra);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,scts);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dt);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,st);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pi);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dcs);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,udl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ud);
                } else if(messagetype.equalsIgnoreCase("SMS_COMMAND"))
                {
                    /*
                    TP MTI	TP Message Type Indicator	    M	2b	Parameter describing the type
                    TP-UDHI	TP-User-Data-Header-Indication	O	b	Parameter indicating that the TP-CD field contains a Header
                    TP SRR	TP Status Report  Request	    O	b	Parameter indicating if the SMS Command is requesting a status report.
                    TP MR	TP Message Reference	        M	I	Parameter identifying the SMS COMMAND
                    TP PID	TP Protocol  Identifier	        M	o	Parameter identifying the above layer protocol, if any
                    TP CT	TP Command Type	                M	o	Parameter specifying which operation is to be performed on a SM
                    TP MN	TP Message Number	            M3)	o	Parameter indicating which SM in the SC to operate on
                    TP DA	TP Destination Address	        M4)	2 12o	Parameter indicating the Destination Address to which the TP Command refers
                    TP CDL	TP Command Data Length	        M	o	Parameter indicating the length of the TP CD field in octets
                    TP CD	TP Command Data	                O	o	Parameter containing user data
                    */
                    if(parameters.length!=14)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name SMS_COMMAND TP_UDHI TP_SRR TP_MR TP_PID TP_CT TP_MN TP_DA_TYPE TP_DA_PLAN TP_DA_NUMBER TP_CDL TP_CD_base64Encoded");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    //TP MTI
                    int mtype=2;
                    byte[] first=new byte[]{(byte)mtype};
                    // TP UDHI
                    if(ConvertLib.textToBoolean(parameters[3]))
                    {
                        mtype=mtype|(64);
                    }
                    // TP SRR
                    if(ConvertLib.textToBoolean(parameters[4]))
                    {
                        mtype=mtype|(32);
                    }
                    first[0]=(byte)mtype;
                    //TP MR
                    byte[] mr=new byte[1];
                    mr[0]=(byte)(Integer.decode(parameters[5]).intValue());
                    // TP PID
                    byte[] pid=new byte[1];
                    pid[0]=(byte)(Integer.decode(parameters[6]).intValue());

                    // TP CT
                    byte[] ct=new byte[1];
                    ct[0]=(byte)(Integer.decode(parameters[7]).intValue());
                    // TP MN
                    byte[] mn=new byte[1];
                    mn[0]=(byte)(Integer.decode(parameters[8]).intValue());

                    // TP DA
                    byte[] tpda=create3GPPAdressField(parameters[9],parameters[10],parameters[11],true);
                    // TP CDL
                    byte[] cdl=new byte[1];
                    cdl[0]=(byte)(Integer.decode(parameters[12]).intValue());
                    byte[] cd=new byte[0];
                    if(parameters[13].length()>0)
                    {
                        cd=ConvertLib.base64Decode(parameters[13]);
                    }

                    fulldata=new byte[first.length+mr.length+pid.length+ct.length+mn.length+tpda.length+cdl.length+cd.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mr);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,pid);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,ct);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mn);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,tpda);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,cdl);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,cd);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": '"+messagetype+"' is not a valid message type");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                tp.addTPDU(fulldata);
                tpdus.put(name,tp);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' message named '"+name+"'\n"+ConvertLib.getHexView(fulldata));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' message named '"+name+"'");
                }

            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": parameter error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }
        }
    }

    private byte[] create3GPPAdressField(String type, String plan, String number, boolean useNumberLength) throws Exception
    {
        int itype=((Integer.decode(type)&0x07)|0x08)<<4;
        int iplan=Integer.decode(plan)&0x0F;
        byte typeplan=(byte)(iplan|itype);
        byte[] bnumber=null;
        if(itype==0x08+0x04+0x01)
        {
            bnumber=ConvertLib.createBytes(number);
        } else
        {
            bnumber=ConvertLib.getSemiOctetFromNumber(number);
        }
        int blength=bnumber.length+1;
        if(blength>11)blength=0;
        byte[] retval=new byte[blength+1];
        if(useNumberLength)
        {
            retval[0]=(byte)(number.length());
        } else
        {
            retval[0]=(byte)(blength);
        }
        retval[1]=typeplan;
        for(int i=0;i<blength-1;i++)
        {
            retval[i+2]=bnumber[i];
        }
        return retval;
    }

    /** Create a SMS RP packet.<br>
    Allowed parameters:<br>
name RP-DATA-MSN reference RP-OA-IEI RP-DA-IEI RP-DA-TYPE RP-DA-PLAN RP-DA-NUMBER RPPacketName<br>
name RP-DATA-NMS reference RP-OA-IEI RP-OA-TYPE RP-OA-PLAN RP-OA-NUMBER RP-DA-IEI RPPacketName<br>
name RP-ACK-MSN reference RPPacketName<br>
name RP-ACK-NMS reference RPPacketName<br>
name RP-ERROR-MSN reference RP-CAUSE RP-DIAGNOSTIC RPPacketName<br>
name RP-ERROR-NMS reference RP-CAUSE RP-DIAGNOSTIC RPPacketName<br>
name RP-SMMA reference<br>
    */
    public void createRPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createRPPacket: name messageType moreOptions...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name messageType moreOptions...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                String name=parameters[1];
                RPDU rp=new RPDU(name);
                byte[] fulldata=null;

                // MESSAGE TYPE
                String messagetype=parameters[2];
                int mtype=0;
                try
                {
                    mtype=Integer.decode(messagetype);
                    mtype=mtype&0x07;
                } catch (Exception mte)
                {
                    /*
                     0 0 0 	 ms > n 	 RP-DATA-1
                     0 0 0 	 n  > ms	 Reserved
                     0 0 1 	 ms > n 	 Reserved
                     0 0 1 	 n  > ms	 RP-DATA-2
                     0 1 0 	 ms > n 	 RP-ACK-1
                     0 1 0 	 n  > ms	 Reserved
                     0 1 1 	 ms > n 	 Reserved
                     0 1 1 	 n  > ms	 RP-ACK-2
                     1 0 0 	 ms > n 	 RP-ERROR-1
                     1 0 0 	 n  > ms	 Reserved
                     1 0 1 	 ms > n 	 Reserved
                     1 0 1 	 n  > ms	 RP-ERROR-2
                     1 1 0 	 ms > n 	 RP-SMMA
                     1 1 0 	 n  > ms	 Reserved
                     1 1 1 	 ms > n 	 Reserved
                     1 1 1 	 n  > ms	 Reserved
                    */
                    if(messagetype.equalsIgnoreCase("RP-DATA-MSN"))
                    {
                        if(parameters.length!=8)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-DATA-MSN reference RP-DA-TYPE RP-DA-PLAN RP-DA-NUMBER RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=0;
                    } else if(messagetype.equalsIgnoreCase("RP-DATA-NMS"))
                    {
                        if(parameters.length!=8)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-DATA-NMS reference RP-OA-TYPE RP-OA-PLAN RP-OA-NUMBER RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=1;
                    } else if(messagetype.equalsIgnoreCase("RP-ACK-MSN"))
                    {
                        if(parameters.length!=5)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-ACK-MSN reference RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=2;
                    } else if(messagetype.equalsIgnoreCase("RP-ACK-NMS"))
                    {
                        if(parameters.length!=5)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-ACK-NMS reference RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=3;
                    } else if(messagetype.equalsIgnoreCase("RP-ERROR-MSN"))
                    {
                        if(parameters.length<5||parameters.length>7)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-ERROR-MSN reference RP-CAUSE RP-DIAGNOSTIC RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=4;
                    } else if(messagetype.equalsIgnoreCase("RP-ERROR-NMS"))
                    {
                        if(parameters.length<5||parameters.length>7)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-ERROR-NMS reference RP-CAUSE RP-DIAGNOSTIC RPPacketName");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=5;
                    } else if(messagetype.equalsIgnoreCase("RP-SMMA"))
                    {
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name RP-SMMA reference");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        mtype=6;
                    } else
                    {
                        XTTProperties.printFail(parameters[0] + ": Invalid message type '"+messagetype+"'");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    }
                }
                byte[] first=new byte[]{(byte)mtype};

                // MESSAGE REFERENCE
                byte[] reference=new byte[]{(byte)(Integer.decode(parameters[3]).intValue())};;


                // OTHER REQUIRED INFORMATION ELEMENTS
                byte[] rporig=new byte[0];
                byte[] addr=null;
                byte[] rpdest=new byte[0];
                byte[] rpuserdatiei=new byte[0];
                byte[] rpuserdatlength=new byte[0];
                byte[] rpuserdat=new byte[0];
                byte[] rpcauselength=new byte[0];
                byte[] rpcause=new byte[0];
                TPDU tp=null;
                int pointer=0;
                messagetype=null;

                switch(mtype)
                {
                    /* RP-DATA
                    RP Message Type	Subclause 8.2.2	M	V	3 bits
                    RP Message Reference	Subclause 8.2.3	M	V	1 octet
                    RP Originator Address	Subclause 8.2.5.1	M	LV	1 octet
                    RP Destination Address	Subclause 8.2.5.2	M	LV	1 12 octets
                    RP User Data	Subclause 8.2.5.3	M	LV	? 233 octets
                    */
                    case 0:
                        // MS->N
                        messagetype="RP-DATA-MSN";
                        rporig=new byte[]{0};//,0};
                        // RP Originator Address number IEI
                        //rporig[0]=(byte)(Integer.decode(parameters[4]).intValue());
                        // RP Destination Address number IEI
                        //tmp=(byte)(Integer.decode(parameters[5]).intValue());
                        // TYPE, PLAN, NUMBER
                        addr=create3GPPAdressField(parameters[4],parameters[5],parameters[6],false);
                        rpdest=new byte[addr.length];
                        //rpdest[0]=tmp;
                        pointer=ConvertLib.addBytesToArray(rpdest,0,addr);
                        // RP USER DATA
                        rpuserdatiei=new byte[0];//{(byte)0x41};
                        tp=tpdus.get(parameters[7]);
                        if(tp==null)
                        {
                            rpuserdat=ConvertLib.base64Decode(parameters[7]);
                        } else
                        {
                            rpuserdat=tp.getTPDUs();
                        }
                        rpuserdatlength=new byte[]{(byte)rpuserdat.length};

                        fulldata=new byte[first.length+reference.length+rporig.length+rpdest.length+rpuserdatiei.length+rpuserdatlength.length+rpuserdat.length];
                        pointer=0;
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,reference);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rporig);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpdest);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatiei);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatlength);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdat);
                        break;
                    case 1:
                        // N->MS
                        messagetype="RP-DATA-NMS";
                        // RP Destination Address number IEI
                        //tmp=(byte)(Integer.decode(parameters[4]).intValue());
                        // TYPE, PLAN, NUMBER
                        addr=create3GPPAdressField(parameters[4],parameters[5],parameters[6],false);
                        rporig=new byte[addr.length];
                        //rporig[0]=tmp;
                        pointer=ConvertLib.addBytesToArray(rporig,0,addr);
                        rporig=new byte[]{0};//,0};
                        // RP Originator Address number IEI
                        //rpdest[0]=(byte)(Integer.decode(parameters[8]).intValue());
                        // RP USER DATA
                        //rpuserdatiei=new byte[]{(byte)0x41};
                        tp=tpdus.get(parameters[7]);
                        if(tp==null)
                        {
                            rpuserdat=ConvertLib.base64Decode(parameters[7]);
                        } else
                        {
                            rpuserdat=tp.getTPDUs();
                        }
                        rpuserdatlength=new byte[]{(byte)rpuserdat.length};

                        fulldata=new byte[first.length+reference.length+rporig.length+rpdest.length+rpuserdatiei.length+rpuserdatlength.length+rpuserdat.length];
                        pointer=0;
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,reference);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rporig);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpdest);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatiei);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatlength);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdat);
                        break;
                    /* RP-ACK
                    RP Message Type	Subclause 8.2.2	M	V	3 bits
                    RP Message Reference	Subclause 8.2.3	M	V	1 octet
                    RP-User Data	Subclause 8.2.5.3	O	TLV	? 234 octets
                    */
                    case 2:
                        // MS->N
                        messagetype="RP-ACK-MSN";
                    case 3:
                        // N->MS
                        if(messagetype!=null)messagetype="RP-ACK-NMS";
                        if(parameters.length==5)
                        {
                            rpuserdatiei=new byte[]{(byte)0x41};
                            tp=tpdus.get(parameters[4]);
                            if(tp==null)
                            {
                                rpuserdat=ConvertLib.base64Decode(parameters[4]);
                            } else
                            {
                                rpuserdat=tp.getTPDUs();
                            }
                            rpuserdatlength=new byte[]{(byte)rpuserdat.length};
                        }

                        fulldata=new byte[first.length+reference.length+rpuserdatiei.length+rpuserdatlength.length+rpuserdat.length];
                        pointer=0;
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,reference);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatiei);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatlength);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdat);
                        break;
                    /* RP-ERROR
                    RP Message Type	Subclause 8.2.2	M	V	3 bits
                    RP Message Reference	Subclause 8.2.3	M	V	1 octet
                    RP Cause	Subclause 8.2.5.4	M	LV	2 3 octets
                    RP User Data	Subclause 8.2.5.3	O	TLV	? 234 octets
                    */
                    case 4:
                        // MS->N
                        messagetype="RP-ERROR-MSN";
                    case 5:
                        if(messagetype!=null)messagetype="RP-ERROR-NMS";
                        // N->MS
                        //  rpcauseiei=new byte[]{(byte)0x42};
                        if(parameters.length>5&&!parameters[5].equals(""))
                        {
                            rpcause=new byte[2];
                            rpcause[0]=(byte)(Integer.decode(parameters[4]).intValue());
                            rpcause[1]=(byte)(Integer.decode(parameters[5]).intValue());
                        } else
                        {
                            rpcause=new byte[1];
                            rpcause[0]=(byte)(Integer.decode(parameters[4]).intValue());
                        }
                        rpcauselength=new byte[]{(byte)(rpcause.length)};
                        if(parameters.length>6)
                        {
                            rpuserdatiei=new byte[]{(byte)0x41};
                            tp=tpdus.get(parameters[6]);
                            if(tp==null)
                            {
                                rpuserdat=ConvertLib.base64Decode(parameters[6]);
                            } else
                            {
                                rpuserdat=tp.getTPDUs();
                            }
                            rpuserdatlength=new byte[]{(byte)rpuserdat.length};
                        }
                        fulldata=new byte[first.length+reference.length/*+rpcauseiei.length*/+rpcauselength.length+rpcause.length+rpuserdatiei.length+rpuserdatlength.length+rpuserdat.length];
                        pointer=0;
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,reference);
                        // pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpcauseiei);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpcauselength);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpcause);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatiei);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdatlength);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,rpuserdat);
                        break;
                    /* RP-SMMA
                        RP Message Type	Subclause 8.2.2	M	V	3 bits
                        RP Message Reference	Subclause 8.2.3	M	V	1 octet
                    */
                    case 6:
                        // MS->N
                        messagetype="RP-SMMA";
                        fulldata=new byte[first.length+reference.length];
                        pointer=0;
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,first);
                        pointer=ConvertLib.addBytesToArray(fulldata,pointer,reference);
                        break;
                    default:
                        XTTProperties.printFail(parameters[0] + ": Invalid message type '"+messagetype+"'");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                }
                rp.addRPDU(fulldata);
                rpdus.put(name,rp);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' message named '"+name+"'\n"+ConvertLib.getHexView(fulldata));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' message named '"+name+"'");
                }
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": parameter error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }

        }
    }
    public void storeTPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": storeTPPacket: name variableName");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[2];
            TPDU tp=tpdus.get(name);
            if(tp==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                String data=ConvertLib.base64Encode(tp.getTPDUs());
                XTTProperties.printInfo(parameters[0]+": packet '"+name+"' stored to "+parameters[1]);
                XTTProperties.setVariable(parameters[1],data);
            }
        }
    }
    public void storeRPPacket(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": storeRPPacket: variableName packetName");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName packetName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[2];
            RPDU rp=rpdus.get(name);
            if(rp==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                String data=ConvertLib.base64Encode(rp.getRPDUs());
                XTTProperties.printInfo(parameters[0]+": packet '"+name+"' stored to "+parameters[1]);
                XTTProperties.setVariable(parameters[1],data);
            }
        }
    }

    public void storeMM1Packet(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": storeMM1Packet: variableName packetName");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName packetName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[2];
            MM1Packet mm1packet=mm1Packets.get(name);
            if(mm1packet==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                String data=ConvertLib.base64Encode(mm1packet.getPacket());
                XTTProperties.printInfo(parameters[0]+": packet '"+name+"' stored to "+parameters[1]);
                XTTProperties.setVariable(parameters[1],data);
            }
        }
    }
    public void createMM1PacketContent(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContent: packetName base64Content");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName base64Content");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1];
            MM1Packet mm1packet=mm1Packets.get(name);
            if(mm1packet==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                byte[] data=ConvertLib.base64Decode(parameters[2]);
                XTTProperties.printInfo(parameters[0]+": "+data.length+" bytes of content set on packet '"+name+"' ");
                mm1packet.setContent(data);
            }
        }
    }

    public void createMM1PacketContentMultipart(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContentMultipart: packetName base64Content contentType name1");
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContentMultipart: packetName base64Content contentType name1 charset");
            return;
        }
        if(parameters.length<5||parameters.length>6)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName base64Content contentType name1");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName base64Content contentType name1 charset");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String ctCharset=null;
            if(parameters.length>5)ctCharset=parameters[5];
            createMM1PacketContentMultipart(parameters[0],parameters[1],parameters[2],parameters[3],parameters[4], ctCharset);
        }
    }

    private void createMM1PacketContentMultipart(String funcname,String name, String base64Content, String contentType, String ctName1, String ctCharset)
    {
        //String name=parameters[1];
        MM1Packet mm1packet=mm1Packets.get(name);
        if(mm1packet==null)
        {
            XTTProperties.printFail(funcname + ": packet '"+name+"' not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
        } else
        {
            byte[] data=ConvertLib.base64Decode(base64Content);
            byte[] olddata=mm1packet.getContent();
            byte[] btype=new byte[0];
            byte[] bcset=new byte[0];
            byte[] bname=new byte[0];

            int type=WSPDecoder.getWellKnownContentType(contentType);
            if(type==-1)
            {
                btype=ConvertLib.getCOctetByteArrayFromString(contentType);
            } else
            {
                btype=new byte[]{(byte)(type+0x80)};
            }
            if(ctCharset!=null)
            {
                int cset=WSPDecoder.getCharset(ctCharset);
                if(cset==-1)
                {
                    byte[] c=ConvertLib.getCOctetByteArrayFromString(ctCharset);
                    bcset=new byte[1+c.length];
                    bcset[0]=(byte)0x81;//Charset
                    int pointer=1;
                    pointer=ConvertLib.addBytesToArray(bcset,pointer,c);
                } else
                {
                    bcset=new byte[2];
                    bcset[0]=(byte)0x81;//Charset
                    bcset[1]=(byte)(cset+0x80);//charset well known value
                }
            }

            byte[] n=ConvertLib.getCOctetByteArrayFromString(ctName1);
            bname=new byte[1+n.length];
            bname[0]=(byte)0x85;//name1
            int pointer=1;
            pointer=ConvertLib.addBytesToArray(bname,pointer,n);

            if(olddata.length==0)
            {
                olddata=new byte[1];
            }
            
            byte[] ctlen=createLength(btype.length+bname.length+bcset.length);
            byte[] contenttype=new byte[ctlen.length+btype.length+bname.length+bcset.length];
            //contenttype[0]=(byte)(MMS_CONTENT_TYPE+0x80);

            byte[] headlen=ConvertLib.getUIntVarFromInt(contenttype.length);
            byte[] datalen=ConvertLib.getUIntVarFromInt(data.length);

            pointer=0;
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,ctlen);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,btype);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,bname);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,bcset);

            byte[] newpacket=new byte[olddata.length+headlen.length+datalen.length+contenttype.length+data.length];

            pointer=0;
            pointer=ConvertLib.addBytesToArray(newpacket,pointer,olddata);
            pointer=ConvertLib.addBytesToArray(newpacket,pointer,headlen);
            pointer=ConvertLib.addBytesToArray(newpacket,pointer,datalen);
            pointer=ConvertLib.addBytesToArray(newpacket,pointer,contenttype);
            pointer=ConvertLib.addBytesToArray(newpacket,pointer,data);
            newpacket[0]=(byte)0;

            XTTProperties.printInfo(funcname+": "+newpacket.length+" bytes of content set on packet '"+name+"' ");
            mm1packet.setContent(newpacket);
        }
    }
    public void setMM1PacketContentMultipartNumber(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setMM1PacketContentMultipartNumber: packetName numParts");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName numParts");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                String name=parameters[1];
                MM1Packet mm1packet=mm1Packets.get(name);
                if(mm1packet==null)
                {
                    XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": cleared content on packet '"+name+"' ");
                    byte[] data=mm1packet.getContent();
                    data[0]=Byte.decode(parameters[2]);;
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR: '"+parameters[2]+"' is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }
        }
    }    
    public void clearMM1PacketContent(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContent: packetName");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1];
            MM1Packet mm1packet=mm1Packets.get(name);
            if(mm1packet==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": cleared content on packet '"+name+"' ");
                mm1packet.clearContent();
            }
        }
    }

    public void decodeMM1Packet(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": decodeMM1Packet: variable base64Packet");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variable base64Packet");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                byte[] packet=ConvertLib.base64Decode(parameters[2]);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": decoding "+packet.length+" bytes to '"+parameters[1]+"'\n"+ConvertLib.getHexView(packet));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": decoding "+packet.length+" bytes to '"+parameters[1]+"'");
                }
                MMSDecoder d=new MMSDecoder(packet,0,parameters[1]);
                d.decode();
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": decoding error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }
        }
    }
    /** Create an MM1 package.<br>
    Allowed Parameters, generic format:<br>
    name<br>
    name messageType moreOptions...<br>
    Allowed Parameters, more specific format:<br>
    name M_SEND_REQ transactionID mmsVersion toAddress contentType<br>
    name M_SEND_REQ transactionID mmsVersion toAddress contentType contentTypeCharset<br>
    name M_SEND_REQ transactionID mmsVersion toAddress contentType contentTypeMultipartType contentTypeMultipartStart<br>
    name M_SEND_CONF transactionID mmsVersion responseStatus<br>
    name M_NOTIFICATION_IND transactionID mmsVersion messageClass messageSize expiryIsAbsolute expiryDate contentLocation<br>
    name M_NOTIFYRESP_IND transactionID mmsVersion<br>
    name M_RETRIEVE_CONF mmsVersion date contentType<br>
    name M_RETRIEVE_CONF mmsVersion date contentType contentTypeCharset<br>
    name M_RETRIEVE_CONF mmsVersion date contentType contentTypeMultipartType contentTypeMultipartStart<br>
    name M_ACKNOWLEDGE_IND mmsVersion<br>
    name M_DELIVERY_IND mmsVersion messageID toAddress dateTime mmStatus<br>
    name M_READ_REC_IND mmsVersion toAddress fromAddress messageID readStatus<br>
    name M_READ_ORIG_IND mmsVersion toAddress fromAddress messageID dateTime readStatus<br>
    name M_FORWARD_REQ transactionID mmsVersion toAddress contentLocation<br>
    name M_FORWARD_CONF transactionID mmsVersion responseStatus messageID<br>
    <br>
    For more information on allowed Optional and Mandatory parameters<br>
    see Multimedia Messaging Service Encapsulation Protocol Candidate Version 1.3 – 27 Sep 2005 (OMA-TS-MMS-ENC-V1_3-20050927-C.pdf)<br>
    see 3GPP TS 23.140 V6.12.0 2006-03 (23140-6c0.doc) <br>
    see Wireless Application Protocol, Wireless Session Protocol Specification, Approved Version 5-July-2001 (WAP-230-WSP-20010705-a.pdf)<br>
    */
    public void createMM1Packet(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1Packet: name");
            XTTProperties.printFail(this.getClass().getName()+": createMM1Packet: name messageType moreOptions...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name messageType moreOptions...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else if(parameters.length==2)
        {
            String name=parameters[1];
            MM1Packet mm1p=new MM1Packet(name,new byte[0]);
            mm1Packets.put(name,mm1p);
            XTTProperties.printInfo(parameters[0] + ": created empty packet named '"+name+"'");
        } else
        {
            try
            {
                String name=parameters[1];
                byte[] fulldata=new byte[0];
                byte[] contenttype=new byte[0];

                String messagetype=parameters[2];
                int mtype=MMSDecoder.getX_MMS_MESSAGE_TYPE(parameters[2]);
                messagetype=MMSDecoder.getX_MMS_MESSAGE_TYPE(mtype);
                if(mtype<=0)
                {
                    XTTProperties.printFail(parameters[0] + ": invalid messaage type '"+parameters[2]+"'");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                } else if(mtype==M_SEND_REQ)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_submit.REQ
                    Transaction ID	    Mandatory	The identification of the MM1_submit.REQ/MM1_submit.RES pair.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the MMS UA.
                    Recipient address	Mandatory	The address of the recipient(s) of the MM. Multiple addresses are possible. (you may ommit to and specify bcc or cc)
                    Content type	    Mandatory	The content type of the MM’s content.
                    */
                   if(parameters.length<8||parameters.length>10)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_SEND_REQ transactionID mmsVersion toAddress fromAddress contentType");
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_SEND_REQ transactionID mmsVersion toAddress fromAddress contentType contentTypeCharset");
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_SEND_REQ transactionID mmsVersion toAddress fromAddress contentType contentTypeMultipartType contentTypeMultipartStart");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_SEND_REQ;
                    byte[] transactionId=createHeader(MMS_X_MMS_TRANSACTION_ID,parameters[3]);
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION   ,parameters[4]);
                    byte[] toAddress    =new byte[0];
                    if(parameters[5].length()>0)
                    {
                        toAddress       =createHeader(MMS_TO                  ,parameters[5]);
                    }
                    byte[] fromAddress  =createHeader(MMS_FROM                  ,parameters[6]);
                    if(parameters.length>9)
                    {
                        contenttype=createContentType(parameters[7],parameters[8],parameters[9]);
                    } else if(parameters.length>8)
                    {
                        contenttype=createContentType(parameters[7],parameters[8],null);

                    } else if(parameters.length>7)
                    {
                        contenttype=createContentType(parameters[7],null,null);
                    }

                    fulldata=new byte[type.length+transactionId.length+version.length+toAddress.length+fromAddress.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,toAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fromAddress);
                } else if(mtype==M_SEND_CONF)
                {
                    /*
                    Message Type	Mandatory	Identifies this message as MM1_submit.RES.
                    Transaction ID	Mandatory	The identification of the MM1_submit.REQ/MM1_submit.RES pair.
                    MMS Version	    Mandatory	Identifies the version of the interface supported by the MMS  Relay/Server.
                    Request Status	Mandatory	The status of the MM submit request.
                    */
                   if(parameters.length!=6)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_SEND_CONF transactionID mmsVersion responseStatus");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_SEND_CONF;
                    byte[] transactionId        =createHeader(MMS_X_MMS_TRANSACTION_ID  ,parameters[3]);
                    byte[] version              =createHeader(MMS_X_MMS_MMS_VERSION     ,parameters[4]);
                    byte[] responseStatusValue  =createHeader(MMS_X_MMS_RESPONSE_STATUS ,parameters[5]);

                    fulldata=new byte[type.length+transactionId.length+version.length+responseStatusValue.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,responseStatusValue);

                } else if(mtype==M_NOTIFICATION_IND)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_notification.REQ
                    Transaction ID	    Mandatory	The identification of the MM1_notification.REQ/MM1_notification.RES pair.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the MMS  Relay/Server.
                    Message class	    Mandatory	The class of the MM (e.g., personal, advertisement, information service; default = personal)
                    Message size	    Mandatory	The approximate size of the MM
                    Time of expiry	    Mandatory	The time of expiry for the MM (time stamp).
                    Message Reference	Mandatory	 a reference, e.g., URI, for the MM
                    */
                   if(parameters.length!=10)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_NOTIFICATION_IND transactionID mmsVersion messageClass messageSize expiryIsAbsolute expiryDate contentLocation");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_NOTIFICATION_IND;
                    byte[] transactionId=createHeader(MMS_X_MMS_TRANSACTION_ID      ,parameters[3]);
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION         ,parameters[4]);
                    byte[] messageClass =createHeader(MMS_X_MMS_MESSAGE_CLASS       ,parameters[5]);
                    byte[] messageSize  =createHeader(MMS_X_MMS_MESSAGE_SIZE        ,parameters[6]);//I'm lazy, only support 8 bytes of message size (which is still more that 8 Million Terabytes).
                    byte[] expiryDate=createHeader(MMS_X_MMS_EXPIRY                 ,parameters[7],parameters[8]);
                    byte[] contentLocation=createHeader(MMS_X_MMS_CONTENT_LOCATION  ,parameters[9]);
                    fulldata=new byte[type.length+transactionId.length+version.length+messageClass.length+messageSize.length+expiryDate.length+contentLocation.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageClass);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageSize);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,expiryDate);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,contentLocation);

                } else if(mtype==M_NOTIFYRESP_IND)
                {
                    /*
                    Message Type	Mandatory	Identifies this message as MM1_notification.RES.
                    Transaction ID	Mandatory	The identification of the MM1_notification.REQ/MM1_notification.RES pair.
                    MMS Version	    Mandatory	Identifies the version of the interface supported by the MMS User Agent.
                    */
                   if(parameters.length!=10)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_NOTIFYRESP_IND transactionID mmsVersion");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_NOTIFYRESP_IND;
                    byte[] transactionId=createHeader(MMS_X_MMS_TRANSACTION_ID,parameters[3]);
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION   ,parameters[4]);

                    fulldata=new byte[type.length+transactionId.length+version.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);

                } else if(mtype==M_RETRIEVE_CONF)
                {
                    /*
                    Message Type	Mandatory	Identifies this message as MM1_retrieve.RES.
                    MMS Version	    Mandatory	Identifies the version of the interface supported by the MMS Relay/Server.
                    Content type	Mandatory	The content type of the MM’s content.
                    Date and time	Mandatory	The time and date of the most recent handling (i.e. either submission or forwarding) of the MM by an MMS User Agent (time stamp).
                    */
                    if(parameters.length<6||parameters.length>8)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_RETRIEVE_CONF mmsVersion date contentType");
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_RETRIEVE_CONF mmsVersion date contentType contentTypeCharset");
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_RETRIEVE_CONF mmsVersion date contentType contentTypeMultipartType contentTypeMultipartStart");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_SEND_REQ;
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION   ,parameters[3]);
                    byte[] dateTime     =createHeader(MMS_DATE                ,parameters[4]);

                    if(parameters.length>7)
                    {
                        contenttype=createContentType(parameters[5],parameters[6],parameters[7]);
                    } else if(parameters.length>6)
                    {
                        contenttype=createContentType(parameters[5],parameters[6],null);
                    } else if(parameters.length>5)
                    {
                        contenttype=createContentType(parameters[5],null,null);
                    }

                    fulldata=new byte[type.length+version.length+dateTime.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dateTime);

                } else if(mtype==M_ACKNOWLEDGE_IND)
                {
                    /*
                    Message Type	Mandatory	Identifies this message as MM1_acknowledgment.REQ.
                    MMS Version	    Mandatory	Identifies the version of the interface supported by the MMS User Agent.
                    */
                   if(parameters.length!=4)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_ACKNOWLEDGE_IND mmsVersion");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_ACKNOWLEDGE_IND;
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION   ,parameters[3]);

                    fulldata=new byte[type.length+version.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);

                } else if(mtype==M_DELIVERY_IND)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_delivery_report.REQ.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the MMS Relay/Server.
                    Message ID	        Mandatory	The identification of the original MM.
                    Recipient address	Mandatory	The address of the MM recipient of the original MM.
                    Date and Time	    Mandatory	Date and time the MM was handled (retrieved, expired, rejected, etc.) (time stamp)
                    MM Status	        Mandatory	Status of the MM, e.g. retrieved, forwarded, expired, rejected
                    */
                   if(parameters.length!=8)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_DELIVERY_IND mmsVersion messageID toAddress dateTime mmStatus");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_DELIVERY_IND;
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION     ,parameters[3]);
                    byte[] messageId    =createHeader(MMS_MESSAGE_ID            ,parameters[4]);
                    byte[] toAddress    =createHeader(MMS_TO                    ,parameters[5]);
                    byte[] dateTime     =createHeader(MMS_DATE                  ,parameters[6]);
                    byte[] mmStatus     =createHeader(MMS_X_MMS_STATUS          ,parameters[7]);

                    fulldata=new byte[type.length+messageId.length+version.length+toAddress.length+dateTime.length+mmStatus.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,toAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dateTime);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mmStatus);

                } else if(mtype==M_READ_REC_IND)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_read_reply_recipient.REQ.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the MMS User Agent.
                    Recipient address	Mandatory	The address of the MM recipient of the original MM, i,e, the originator of the read-reply report.
                    Originator address	Mandatory	The address of the MM originator of the original MM, i,e, the recipient of the read-reply report.
                    Message ID	        Mandatory	The message ID of the original MM.
                    Read Status	        Mandatory	Status of the MM, e.g. Read, Deleted without being read
                    */
                   if(parameters.length!=8)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_READ_REC_IND mmsVersion toAddress fromAddress messageID readStatus");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_READ_REC_IND;
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION ,parameters[3]);
                    byte[] toAddress    =createHeader(MMS_TO                ,parameters[4]);
                    byte[] fromAddress  =createHeader(MMS_FROM              ,parameters[5]);
                    byte[] messageId    =createHeader(MMS_MESSAGE_ID        ,parameters[6]);
                    byte[] mmStatus     =createHeader(MMS_X_MMS_READ_STATUS ,parameters[7]);

                    fulldata=new byte[type.length+messageId.length+version.length+toAddress.length+fromAddress.length+mmStatus.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,toAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fromAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mmStatus);

                } else if(mtype==M_READ_ORIG_IND)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_read_reply_originator.REQ.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the MMS Relay/Server.
                    Recipient address	Mandatory	The address of the MM recipient of the original MM, i,e, the originator of the read-reply report.
                    Originator address	Mandatory	The address of the MM originator of the original MM, i,e, the recipient of the read-reply report.
                    Message ID	        Mandatory	The message ID of the original MM.
                    Date and Time	    Mandatory	Date and time the MM was handled (read, deleted without being read, etc.) (time stamp)
                    Read Status	        Mandatory	Status of the MM, e.g. Read, Deleted without being read
                    */
                    if(parameters.length!=9)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_READ_ORIG_IND mmsVersion toAddress fromAddress messageID dateTime readStatus");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_READ_ORIG_IND;
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION ,parameters[3]);
                    byte[] toAddress    =createHeader(MMS_TO                ,parameters[4]);
                    byte[] fromAddress  =createHeader(MMS_FROM              ,parameters[5]);
                    byte[] messageId    =createHeader(MMS_MESSAGE_ID        ,parameters[6]);
                    byte[] dateTime     =createHeader(MMS_DATE              ,parameters[7]);
                    byte[] mmStatus     =createHeader(MMS_X_MMS_READ_STATUS ,parameters[8]);

                    fulldata=new byte[type.length+messageId.length+version.length+toAddress.length+fromAddress.length+dateTime.length+mmStatus.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,toAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fromAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,dateTime);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mmStatus);

                } else if(mtype==M_FORWARD_REQ)
                {
                    /*
                    Message Type	    Mandatory	Identifies this message as MM1_forward.REQ.
                    Transaction ID	    Mandatory	The identification of the MM1_forward.REQ/MM1_forward.RES pair.
                    MMS Version	        Mandatory	Identifies the version of the interface supported by the forwarding MMS User Agent.
                    Recipient address	Mandatory	The address of the recipient of the forwarded MM. Multiple addresses are possible.
                    Message Reference	Mandatory	A reference, e.g., URI, for the MM being forwarded.  This may either be the Message Reference from MM1_notification.REQ, MM1_mmbox_store.REQ, or MM1_mmbox_view.REQ.
                    */
                    if(parameters.length!=8)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_FORWARD_REQ transactionID mmsVersion toAddress fromAddress contentLocation");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_FORWARD_REQ;
                    byte[] transactionId    =createHeader(MMS_X_MMS_TRANSACTION_ID  ,parameters[3]);
                    byte[] version          =createHeader(MMS_X_MMS_MMS_VERSION     ,parameters[4]);
                    byte[] toAddress        =createHeader(MMS_TO                    ,parameters[5]);
                    byte[] fromAddress      =createHeader(MMS_FROM                  ,parameters[6]);
                    byte[] contentLocation  =createHeader(MMS_X_MMS_CONTENT_LOCATION,parameters[7]);

                    fulldata=new byte[type.length+transactionId.length+version.length+toAddress.length+fromAddress.length+contentLocation.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,toAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,fromAddress);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,contentLocation);

                } else if(mtype==M_FORWARD_CONF)
                {
                    /*
                    Message Type	Mandatory	Identifies this message as MM1_forward.RES.
                    Transaction ID	Mandatory	The identification of the MM1_forward.REQ/MM1_forward.RES pair.
                    MMS Version	    Mandatory	Identifies the version of the interface supported by the MMS Relay/Server.
                    Request Status	Mandatory	The status of the MM Forward request.
                    Message ID	    Mandatory	The unique identification of the forwarded MM.
                    */
                    if(parameters.length!=7)
                    {
                        XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name M_FORWARD_CONF transactionID mmsVersion responseStatus messageID");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                        return;
                    }
                    byte[] type=new byte[2];
                    type[0]=(byte)(MMS_X_MMS_MESSAGE_TYPE+0x80);
                    type[1]=(byte)M_FORWARD_CONF;
                    byte[] transactionId=createHeader(MMS_X_MMS_TRANSACTION_ID  ,parameters[3]);
                    byte[] version      =createHeader(MMS_X_MMS_MMS_VERSION     ,parameters[4]);
                    byte[] mmStatus     =createHeader(MMS_X_MMS_READ_STATUS     ,parameters[5]);
                    byte[] messageId    =createHeader(MMS_MESSAGE_ID            ,parameters[6]);

                    fulldata=new byte[type.length+transactionId.length+messageId.length+version.length+mmStatus.length];
                    int pointer=0;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,type);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,transactionId);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,version);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,mmStatus);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,messageId);
                } else
                {
                    XTTProperties.printFail(parameters[0] + ": unsupported message type '"+messagetype+"', use free mode");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }

                MM1Packet mm1p=new MM1Packet(name,fulldata);
                mm1p.setContentType(contenttype);
                mm1Packets.put(name,mm1p);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' packet named '"+name+"'\n"+ConvertLib.getHexView(fulldata));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": created '"+messagetype+"' packet named '"+name+"'");
                }
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": parameter error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }
        }
    }
    
    /**
     * set the addtional headers to be sent in MM1 Request
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is headerkey
     *                     <br><code>parameters[2]</code> is headervalue
     *                 
     * @param parameters
     */
    public void setAdditionalHeader(String parameters[])
    {
        
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setAdditionalHeader: headerkey headervalue");
            return;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": headerkey headervalue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            additonalHeaders.append(CRLF).append(parameters[1]).append(": ").append(parameters[2]);
            additonalHeader = true;
            XTTProperties.printInfo(parameters[0]+": setting Addtional Header '"+parameters[1]+": "+parameters[2]+"'");
        }
        
    }

    private byte[] createContentType(String c1, String c2, String c3)
    {
        byte[] contenttype=null;
        /*
        8.4.2.24 Content type field
        The following rules are used to encode content type values. The short form of the Content-type-value MUST only be
        used when the well-known media is in the range of 0-127 or a text string. In all other cases the general form MUST be
        used.

        Content-type-value   = Constrained-media | Content-general-form
        Content-general-form = Value-length Media-type
        Media-type           = (Well-known-media | Extension-Media) *(Parameter)

        Constrained-media = Constrained-encoding
        Well-known-media  = Integer-value
        ; Both are encoded using values from Content Type Assignments table in Assigned Numbers

        Constrained-encoding = Extension-Media | Short-integer
        ; This encoding is used for token values, which have no well-known binary encoding, or when
        ; the assigned number of the well-known encoding is small enough to fit into Short-integer.

        Extension-media = *TEXT End-of-string
        ; This encoding is used for media values, which have no well-known binary encoding

        */
        if(c3==null&&c2==null)
        {
            int type=WSPDecoder.getWellKnownContentType(c1);
            if(type==-1)
            {
                byte[] b=ConvertLib.getCOctetByteArrayFromString(c1);
                //byte[] len=createLength(b.length);
                contenttype=new byte[b.length+1];//+len.length];
                contenttype[0]=(byte)(MMS_CONTENT_TYPE+0x80);
                int pointer=1;
                //pointer=ConvertLib.addBytesToArray(contenttype,pointer,len);
                pointer=ConvertLib.addBytesToArray(contenttype,pointer,b);
            } else
            {
                contenttype=new byte[2];
                contenttype[0]=(byte)(MMS_CONTENT_TYPE+0x80);
                //contenttype[1]=(byte)0x01;
                contenttype[1]=(byte)(type+0x80);
            }
        } else if (c3==null)
        {
            int type=WSPDecoder.getWellKnownContentType(c1);
            byte[] btype=new byte[0];
            int cset=WSPDecoder.getCharset(c2);
            byte[] bcset=new byte[0];
            if(type==-1)
            {
                btype=ConvertLib.getCOctetByteArrayFromString(c1);
            } else
            {
                btype=new byte[]{(byte)(type+0x80)};
            }
            if(cset==-1)
            {
                bcset=new byte[2];
                bcset[0]=(byte)0x81;//Charset
                byte[] c=ConvertLib.getCOctetByteArrayFromString(c2);
                int pointer=1;
                pointer=ConvertLib.addBytesToArray(bcset,pointer,c);
            } else
            {
                bcset=new byte[2];
                bcset[0]=(byte)0x81;//Charset
                bcset[1]=(byte)(cset+0x80);//charset well know value
            }
            byte[] len=createLength(btype.length+bcset.length);
            contenttype=new byte[1+btype.length+len.length+bcset.length];
            contenttype[0]=(byte)(MMS_CONTENT_TYPE+0x80);
            int pointer=1;
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,len);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,btype);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,bcset);

        } else
        {
            // In case anyone is checking this out, I do not know if it works :-P
            int type=WSPDecoder.getWellKnownContentType(c1)+0x80;
            byte[] btype=new byte[]{(byte)type};
            byte[] bttypeC=new byte[]{(byte)0x8A};
            byte[] bttype=ConvertLib.getCOctetByteArrayFromString(c2);
            byte[] btstartC=new byte[]{(byte)0x89};
            byte[] btstart=ConvertLib.getCOctetByteArrayFromString(c3);
            byte[] len=createLength(btype.length+bttypeC.length+bttype.length+btstartC.length+btstart.length);
            contenttype=new byte[1+btype.length+len.length+bttypeC.length+bttype.length+btstartC.length+btstart.length];
            contenttype[0]=(byte)(MMS_CONTENT_TYPE+0x80);
            int pointer=1;
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,len);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,btype);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,bttypeC);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,bttype);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,btstartC);
            pointer=ConvertLib.addBytesToArray(contenttype,pointer,btstart);
        }
        return contenttype;
    }

    private byte[] createLength(int leng)
    {
        byte[] len=new byte[0];
        if(leng>=31)
        {
            byte[] len1=ConvertLib.getUIntVarFromInt(leng);
            byte[] pad=new byte[]{(byte)31};
            len=new byte[len1.length+pad.length];
            int pointer=0;
            pointer=ConvertLib.addBytesToArray(len,pointer,pad);
            pointer=ConvertLib.addBytesToArray(len,pointer,len1);
        } else
        {
            len=new byte[1];
            len[0]=(byte)leng;
        }
        return len;
    }

    private byte[] createDate(byte type, boolean isAbsolute, long longdate)
    {
        byte[] bdate=ConvertLib.getByteArrayFromLong(longdate);
        byte[] len=createLength(2+bdate.length);
        byte[] date=new byte[1+len.length+2+bdate.length];

        date[0]=type;
        int pointer=1;
        pointer=ConvertLib.addBytesToArray(date,pointer,len);

        if(isAbsolute)
        {
            date[pointer++]=(byte)128;
        } else
        {
            date[pointer++]=(byte)129;
        }
        date[pointer++]=(byte)bdate.length;
        pointer=ConvertLib.addBytesToArray(date,pointer,bdate);

        return date;
    }
    /** Set the MM1 packet content type header on a packet. The content-type header will always be the LAST header sent if set this way (or when using the createMM1Packet with content-type as mandatory header).
    */
    public void createMM1PacketContentType(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContentType: name contentType");
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContentType: name contentType contentTypeCharset");
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketContentType: name contentType contentTypeMultipartType contentTypeMultipartStart");
            return;
        }
        if(parameters.length<3||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name contentType");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name contentType contentTypeCharset");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name contentType contentTypeMultipartType contentTypeMultipartStart");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                String name=parameters[1];
                byte[] fulldata=new byte[0];
                MM1Packet mm1packet=mm1Packets.get(name);
                if(mm1packet==null)
                {
                    XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                if(parameters.length>4)
                {
                    fulldata=createContentType(parameters[2],parameters[3],parameters[4]);
                } else if(parameters.length>3)
                {
                    fulldata=createContentType(parameters[2],parameters[3],null);
                } else if(parameters.length>2)
                {
                    fulldata=createContentType(parameters[2],null,null);
                }
                mm1packet.setContentType(fulldata);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": set CONTENT_TYPE on packet named '"+name+"'\n"+ConvertLib.getHexView(fulldata));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": set CONTENT_TYPE on packet named '"+name+"'");
                }
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": parameter error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }
        }
    }

    /** Add optional/conditional headers to the MM1 packet. encoded String format not yet supported, used text-string instead. only headerType and boolean can be added as text values at te momment. use numeric values for now.
    <br><br>
    For more information on allowed Optional and Mandatory parameters<br>
    see Multimedia Messaging Service Encapsulation Protocol Candidate Version 1.3 – 27 Sep 2005 (OMA-TS-MMS-ENC-V1_3-20050927-C.pdf)<br>
    see 3GPP TS 23.140 V6.12.0 2006-03 (23140-6c0.doc) <br>
    see Wireless Application Protocol, Wireless Session Protocol Specification, Approved Version 5-July-2001 (WAP-230-WSP-20010705-a.pdf)<br>
    */
    public void createMM1PacketOptions(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMM1PacketOptions: name headerType headerOptions...");
            return;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name headerType headerOptions...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            try
            {
                String name=parameters[1];
                byte[] fulldata=new byte[0];
                MM1Packet mm1packet=mm1Packets.get(name);
                if(mm1packet==null)
                {
                    XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }

                String headertype=parameters[2];
                int htype=MMSDecoder.getX_HEADER(headertype);
                if(htype<=0)
                {
                    XTTProperties.printFail(parameters[0] + ": invalid header type '"+headertype+"'");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                headertype=MMSDecoder.getX_HEADER(htype);
                switch(htype)
                {
                    case MMS_CONTENT_TYPE:
                        if(parameters.length<6||parameters.length>8)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name CONTENT_TYPE contentType");
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name CONTENT_TYPE contentType contentTypeCharset");
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name CONTENT_TYPE contentType contentTypeMultipartType contentTypeMultipartStart");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        if(parameters.length>5)
                        {
                            fulldata=createContentType(parameters[3],parameters[4],parameters[5]);
                        } else if(parameters.length>4)
                        {
                            fulldata=createContentType(parameters[3],parameters[4],null);
                        } else if(parameters.length>3)
                        {
                            fulldata=createContentType(parameters[3],null,null);
                        }
                        break;
                    // ENCODED STRING
                    case MMS_BCC:
                    case MMS_CC:
                    case MMS_SUBJECT:
                    case MMS_TO:
                    case MMS_X_MMS_STATUS_TEXT:
                    case MMS_X_MMS_STORE_STATUS_TEXT:
                    case MMS_X_MMS_RETRIEVE_TEXT:
                    case MMS_X_MMS_RESPONSE_TEXT:
                    case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT:
                        // Currently only support TEXT-String mode of encoded string
                        //break;
                    // TEXT STRING
                    case MMS_X_MMS_CONTENT_LOCATION:
                    case MMS_X_MMS_TRANSACTION_ID:
                    case MMS_X_MMS_REPLY_CHARGING_ID:
                    case MMS_X_MMS_REPLACE_ID:
                    case MMS_X_MMS_REPLY_APPLIC_ID:
                    case MMS_MESSAGE_ID:
                    case MMS_X_MMS_CANCEL_ID:
                    case MMS_X_MMS_APPLIC_ID:
                    case MMS_X_MMS_AUX_APPLIC_INFO:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" stringvalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    // LONG-INTEGER
                    case MMS_DATE:
                    case MMS_X_MMS_REPLY_CHARGING_SIZE:
                    case MMS_X_MMS_MESSAGE_SIZE:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" integervalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    // DATE-relative-absolute
                    case MMS_X_MMS_REPLY_CHARGING_DEADLINE:
                    case MMS_X_MMS_EXPIRY:
                    case MMS_X_MMS_DELIVERY_TIME:
                        if(parameters.length!=5)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" isAbsolute datevalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3],parameters[4]);
                        break;
                    // BYTE
                    case MMS_X_MMS_STORE_STATUS:
                    case MMS_X_MMS_STATUS:
                    case MMS_X_MMS_SENDER_VISIBILITY:
                    case MMS_X_MMS_RESPONSE_STATUS:
                    case MMS_X_MMS_REPLY_CHARGING:
                    case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE:
                    case MMS_X_MMS_READ_STATUS:
                    case MMS_X_MMS_PRIORITY:
                    case MMS_X_MMS_MMS_VERSION:
                    case MMS_X_MMS_MM_FLAGS:
                    case MMS_X_MMS_MESSAGE_TYPE:
                    case MMS_X_MMS_MESSAGE_CLASS:
                    case MMS_X_MMS_CONTENT_CLASS:
                    case MMS_X_MMS_CANCEL_STATUS:
                    case MMS_X_MMS_ATTRIBUTES:
                    case MMS_X_MMS_RETRIEVE_STATUS:
                    case MMS_X_MMS_MM_STATE:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" bytevalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    // BOOLEAN
                    case MMS_X_MMS_STORED:
                    case MMS_X_MMS_STORE:
                    case MMS_X_MMS_TOTALS:
                    case MMS_X_MMS_REPORT_ALLOWED:
                    case MMS_X_MMS_READ_REPORT:
                    case MMS_X_MMS_QUOTAS:
                    case MMS_X_MMS_DISTRIBUTION_INDICATOR:
                    case MMS_X_MMS_DELIVERY_REPORT:
                    case MMS_X_MMS_ADAPTATION_ALLOWED:
                    case MMS_X_MMS_DRM_CONTENT:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" booleanvalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    //INTEGER
                    case MMS_X_MMS_START:
                    case MMS_X_MMS_MESSAGE_COUNT:
                    case MMS_X_MMS_LIMIT:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" integervalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    // FROM
                    case MMS_FROM:
                        if(parameters.length!=4)
                        {
                            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": name "+headertype+" stringvalue");
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                            return;
                        }
                        fulldata=createHeader(htype,parameters[3]);
                        break;
                    // LENGTH/TOKEN/INTEGER
                    case MMS_X_MMS_MBOX_TOTALS:
                    case MMS_X_MMS_MBOX_QUOTAS:
                    // SINGLE
                    case MMS_X_MMS_PREVIOUSLY_SENT_BY:
                    case MMS_X_MMS_PREVIOUSLY_SENT_DATE:
                    case MMS_X_MMS_ELEMENT_DESCRIPTOR:
                    // MMS1.3:
                    case MMS_CONTENT:
                    case MMS_ADDITIONAL_HEADERS:
                    default:
                        XTTProperties.printFail(parameters[0] + ": "+headertype+" not yet supported");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                }
                mm1packet.addOptionalPacket(fulldata);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0] + ": added '"+headertype+"' to packet '"+name+"'\n"+ConvertLib.getHexView(fulldata));
                } else
                {
                    XTTProperties.printInfo(parameters[0] + ": added '"+headertype+"' to packet '"+name+"'");
                }
            } catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": parameter error: "+ex.getClass().getName()+" "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }
        }
    }

    public void clearMM1PacketOptions(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearMM1PacketOptions: packetName");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1];
            MM1Packet mm1packet=mm1Packets.get(name);
            if(mm1packet==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": cleared optional parameters on packet '"+name+"' ");
                mm1packet.clearOptionalPacket();
            }
        }
    }
    public void clearMM1PacketMandatory(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": clearMM1PacketMandatory: packetName");
            return;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": packetName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1];
            MM1Packet mm1packet=mm1Packets.get(name);
            if(mm1packet==null)
            {
                XTTProperties.printFail(parameters[0] + ": packet '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": cleared mandatory parameters on packet '"+name+"' ");
                mm1packet.clearMandatoryPacket();
            }
        }
    }

    private byte[] createHeader(int mtype, String... parameters)
    {
        byte[] fulldata=new byte[0];
        byte[] partdata=new byte[0];
        int pointer=0;
        switch(mtype)
        {
            case MMS_CONTENT_TYPE:
                if(parameters.length>2)
                {
                    fulldata=createContentType(parameters[0],parameters[1],parameters[2]);
                } else if(parameters.length>1)
                {
                    fulldata=createContentType(parameters[0],parameters[1],null);

                } else if(parameters.length>0)
                {
                    fulldata=createContentType(parameters[0],null,null);
                }
                //mm1packet.setContentType(fulldata);
                return fulldata;
            // ENCODED STRING
            case MMS_BCC:
            case MMS_CC:
            case MMS_SUBJECT:
            case MMS_TO:
            case MMS_X_MMS_STATUS_TEXT:
            case MMS_X_MMS_STORE_STATUS_TEXT:
            case MMS_X_MMS_RETRIEVE_TEXT:
            case MMS_X_MMS_RESPONSE_TEXT:
            case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT:
                // Currently only support TEXT-String mode of encoded string
                //break;
            // TEXT STRING
            case MMS_X_MMS_CONTENT_LOCATION:
            case MMS_X_MMS_TRANSACTION_ID:
            case MMS_X_MMS_REPLY_CHARGING_ID:
            case MMS_X_MMS_REPLACE_ID:
            case MMS_X_MMS_REPLY_APPLIC_ID:
            case MMS_MESSAGE_ID:
            case MMS_X_MMS_CANCEL_ID:
            case MMS_X_MMS_APPLIC_ID:
            case MMS_X_MMS_AUX_APPLIC_INFO:
                partdata=ConvertLib.getCOctetByteArrayFromString(parameters[0]);
                fulldata=new byte[partdata.length+1];
                fulldata[0]=(byte)(mtype+0x80);
                pointer=1;
                pointer=ConvertLib.addBytesToArray(fulldata,pointer,partdata);
                break;
            // LONG-INTEGER
            case MMS_DATE:
            case MMS_X_MMS_REPLY_CHARGING_SIZE:
            case MMS_X_MMS_MESSAGE_SIZE:
                partdata=ConvertLib.getByteArrayFromLong(Long.decode(parameters[0]));
                fulldata=new byte[partdata.length+2];
                fulldata[0]=(byte)(mtype+0x80);
                fulldata[1]=(byte)(partdata.length);
                pointer=2;
                pointer=ConvertLib.addBytesToArray(fulldata,pointer,partdata);
                break;
            // DATE-relative-absolute
            case MMS_X_MMS_REPLY_CHARGING_DEADLINE:
            case MMS_X_MMS_EXPIRY:
            case MMS_X_MMS_DELIVERY_TIME:
                fulldata=createDate((byte)(mtype+0x80),ConvertLib.textToBoolean(parameters[0]),Long.decode(parameters[1]));
                break;
            // BYTE
            case MMS_X_MMS_STORE_STATUS:
            case MMS_X_MMS_STATUS:
            case MMS_X_MMS_SENDER_VISIBILITY:
            case MMS_X_MMS_RESPONSE_STATUS:
            case MMS_X_MMS_REPLY_CHARGING:
            case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE:
            case MMS_X_MMS_READ_STATUS:
            case MMS_X_MMS_PRIORITY:
            case MMS_X_MMS_MESSAGE_CLASS:
            case MMS_X_MMS_CONTENT_CLASS:
            case MMS_X_MMS_CANCEL_STATUS:
            case MMS_X_MMS_ATTRIBUTES:
            case MMS_X_MMS_RETRIEVE_STATUS:
            case MMS_X_MMS_MM_STATE:
                fulldata=new byte[2];
                fulldata[0]=(byte)(mtype+0x80);
                fulldata[1]=(byte)(Integer.decode(parameters[0]).intValue());
                break;
            case MMS_X_MMS_MESSAGE_TYPE:
                fulldata=new byte[2];
                fulldata[0]=(byte)(mtype+0x80);
                fulldata[1]=(byte)(MMSDecoder.getX_MMS_MESSAGE_TYPE(parameters[0]));
                break;
            // VERSION
            case MMS_X_MMS_MMS_VERSION:
                fulldata=new byte[2];
                fulldata[0]=(byte)(mtype+0x80);
                fulldata[1]=(byte)(Integer.decode(parameters[0]).intValue()+0x80);
                break;
            // BOOLEAN
            case MMS_X_MMS_STORED:
            case MMS_X_MMS_STORE:
            case MMS_X_MMS_TOTALS:
            case MMS_X_MMS_REPORT_ALLOWED:
            case MMS_X_MMS_READ_REPORT:
            case MMS_X_MMS_QUOTAS:
            case MMS_X_MMS_DISTRIBUTION_INDICATOR:
            case MMS_X_MMS_DELIVERY_REPORT:
            case MMS_X_MMS_ADAPTATION_ALLOWED:
            case MMS_X_MMS_DRM_CONTENT:
                fulldata=new byte[2];
                fulldata[0]=(byte)(mtype+0x80);
                if(ConvertLib.textToBoolean(parameters[0]))
                {
                    fulldata[1]=(byte)(128);
                } else
                {
                    fulldata[1]=(byte)(129);
                }
                break;
            // FROM
            case MMS_FROM:
                if(parameters[0].length()==0)
                {
                    fulldata=new byte[3];
                    fulldata[0]=(byte)(mtype+0x80);
                    fulldata[1]=(byte)(1);
                    fulldata[2]=(byte)(129); // INSER ADDRESS TOKEN
                }else
                {
                    // ENCODED STRING, currently only support TEXT-STRING
                    partdata=ConvertLib.getCOctetByteArrayFromString(parameters[0]);
                    byte[] lengthdata=createLength(partdata.length+1);
                    fulldata=new byte[partdata.length+2+lengthdata.length];
                    fulldata[0]=(byte)(mtype+0x80);
                    pointer=1;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,lengthdata);
                    fulldata[pointer++]=(byte)(128);
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,partdata);
                }
                break;
            //INTEGER.
            case MMS_X_MMS_START:
            case MMS_X_MMS_MESSAGE_COUNT:
            case MMS_X_MMS_LIMIT:
                long longval=Long.decode(parameters[0]);
                if(longval<128l)
                {
                    fulldata=new byte[2];
                    fulldata[0]=(byte)(mtype+0x80);
                    fulldata[1]=(byte)((int)longval+128);
                } else
                {
                    partdata=ConvertLib.getByteArrayFromLong(Long.decode(parameters[0]));
                    fulldata=new byte[partdata.length+2];
                    fulldata[0]=(byte)(mtype+0x80);
                    fulldata[1]=(byte)(partdata.length);
                    pointer=2;
                    pointer=ConvertLib.addBytesToArray(fulldata,pointer,partdata);
                }
                break;
            // LENGTH/TOKEN/INTEGER
            case MMS_X_MMS_MBOX_TOTALS:
            case MMS_X_MMS_MBOX_QUOTAS:
            // SINGLE
            case MMS_X_MMS_MM_FLAGS:
            case MMS_X_MMS_PREVIOUSLY_SENT_BY:
            case MMS_X_MMS_PREVIOUSLY_SENT_DATE:
            case MMS_X_MMS_ELEMENT_DESCRIPTOR:
            // MMS1.3:
            case MMS_CONTENT:
            case MMS_ADDITIONAL_HEADERS:
            default:
                throw new IllegalArgumentException("'"+MMSDecoder.getX_HEADER(mtype)+"' currently not supported");
        }
        return fulldata;
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
        int standardPort = XTTProperties.getIntProperty("SMSCSERVER/PORT");

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
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_SMS.java,v 1.64 2010/08/10 10:47:19 awadhai Exp $";
}