package com.mobixell.xtt.diameter.server;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Element;
import org.jdom.Attribute;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.diameter.DiameterConstants;
import com.mobixell.xtt.diameter.DiameterManager;
import com.mobixell.xtt.diameter.Diameter_AVP;
import com.mobixell.xtt.diameter.Diameter_PDUBody;
import com.mobixell.xtt.diameter.Diameter_PDUHeader;
import com.mobixell.xtt.diameter.load.UserDataItem;
import com.mobixell.xtt.diameter.statistics.CmdDataItem;
import com.mobixell.xtt.util.StringUtils;
/**
 * <p>DiameterWorkerServer</p>
 * <pre>Values stored under:
    DIAMETER/HEADER/[headername]
    DIAMETER/[ServerPort]/HEADER/[headername]
    DIAMETER/AVP/[avpname]
    DIAMETER/AVP/[avpname]/vendorID
    DIAMETER/AVP/[avpname]/avpcode
    DIAMETER/AVP/[avpname]/avpSflags
    DIAMETER/AVP/[avpname]/length   (in case of grouped avps the number of avps in this avp)
    DIAMETER/[ServerPort]/AVP/[avpname]
    DIAMETER/[ServerPort]/AVP/[avpname]/vendorID
    DIAMETER/[ServerPort]/AVP/[avpname]/avpcode
    DIAMETER/[ServerPort]/AVP/[avpname]/avpSflags
    DIAMETER/[ServerPort]/AVP/[avpname]/length   (in case of grouped avps the number of avps in this avp)
   </pre>

 * <p>Company: mobixell Solutions Inc</p>
 * @author Roger Soder
 * @version $Id: DiameterWorkerServer.java,v 1.21 2011/04/18 12:17:18 guybitan Exp $
 */
public class DiameterWorkerServer extends Thread implements DiameterConstants
{
    private boolean stop = false;
    private int id;
    private static boolean extendedOutput=false;
    //private static final int OUTPUTLENGTH=40;
    /* Socket to client we're handling, which will be set by the DiameterServer
       when dispatching the request to us */
    private Socket pcefSocket = null;
    private DiameterServer pcrfServer=null;
    ServerSocket pcrfSocket =null;
    private int myTimeout=600000;
    public File myRoot=null;
    private String myDiamServerPort="-";
    public static String myDiamHostName=null;
    private static int instances=0;
    private static Object requestkey=new Object();
    public static int requestcount=0;
    private static Object key = new Object();
    private static Set<String> extendedStoreVar = Collections.synchronizedSet(new HashSet<String>());
    private static boolean keep_alive=true;
    private static int perworkerdelay=0;
    private boolean isOrgRoot = true;
    private int chargingRuleNameCount = 0;
    private boolean isChargingRuleRemove = false;
    private String pcef_IP = null;
    private String pcrf_IP = null;
    private String pcef_Port = null;
    private String pcrf_Port =  null; 
    /**
     * Creates a new DiameterWorkerServer
     * @param id     ID number of this worker thread
     * @throws Exception 
     */
    public DiameterWorkerServer(int id,Socket socket, DiameterServer server, int timeout) throws Exception
    {
        super("DiameterWorkerServer("+id+")");
        this.pcefSocket = socket;
        this.pcrfSocket = server.pcrfSocket;
        this.id = id;
        this.myTimeout=timeout;
        this.pcrfServer=server;
        myDiamHostName=socket.getLocalAddress().getCanonicalHostName();
        if(this.pcrfServer==null)
        {
            myDiamServerPort="-";
        } else
        {
            server.addWorker();
            myDiamServerPort=server.getPort()+"";
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
        XTTProperties.printDebug("DiameterWorkerServer(" + myDiamServerPort+ "/" + getWorkerId() + "): setting stop");
        try
        {
            this.pcefSocket.close();
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
            /*new SendMyResp().start();
            new SendMyResp().start();*/
        	
        } catch (javax.net.ssl.SSLProtocolException spe)
        {
            XTTProperties.printWarn("DiameterWorkerServer(" + myDiamServerPort+"/" + getWorkerId() + "): SSLProtocolException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spe);
            }
        } catch (javax.net.ssl.SSLPeerUnverifiedException spue)
        {
            XTTProperties.printWarn("DiameterWorkerServer(" + myDiamServerPort+ "/" + getWorkerId() + "): SSLPeerUnverifiedException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(spue);
            }
        } catch (javax.net.ssl.SSLKeyException ske)
        {
            XTTProperties.printWarn("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): SSLKeyException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(ske);
            }
        } catch (javax.net.ssl.SSLHandshakeException she)
        {
            XTTProperties.printWarn("DiameterWorkerServer(" + myDiamServerPort+"/" + getWorkerId() + "): SSLHandshakeException");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(she);
            }
        } catch (javax.net.ssl.SSLException se)
        {
            XTTProperties.printWarn("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): SSLException");
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
                XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort+"/" + getWorkerId() + "): SocketException in run");
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
                XTTProperties.printDebug("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId()+ "): SocketTimeoutException in run - Keep-Alive not enabled");
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
                XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): exception in run");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }

    /**
     * Handles the Diameter request
     * @throws IOException
     */
    public void handleClient() throws Exception
    {
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): New Client handled by "+id+" instance "+instances);
            key.notify();
		}
		try {
			synchronized (pcefSocket) {
        		  
            XTTProperties.printDebug("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): Client connected: "+pcefSocket.getRemoteSocketAddress()+"\n");
            BufferedInputStream   in = new BufferedInputStream(pcefSocket.getInputStream(),65536);
            BufferedOutputStream out = new BufferedOutputStream(pcefSocket.getOutputStream());
            pcefSocket.setSoTimeout(myTimeout);
            //the Nagle algorithm is used to automatically concatenate a number of small buffer messages (see RC896, RFC1122)
            pcefSocket.setTcpNoDelay(true);
            // As long as it takes
            boolean doWhile=true;
            // do the loop
            while(doWhile&&pcefSocket.isConnected()&&!stop)
            {
                Diameter_PDUHeader pdu_header=new Diameter_PDUHeader("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + ")");
                if(!pdu_header.readPDUHeader(in))
                {
                    XTTProperties.printVerbose("DiameterWorkerServer("+myDiamServerPort + "/" + getWorkerId()+ "): pdu empty, possible disconnect");
                    doWhile=false;
                    return;
                }
                XTTProperties.printTransaction("DiameterWorkerServer/HEADER  "+XTTProperties.DELIMITER+myDiamServerPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
                    + pdu_header.getCommandname()+XTTProperties.DELIMITER
                    + pdu_header.getApplicationID()+XTTProperties.DELIMITER
                    + pdu_header.getHopbyhopID()+XTTProperties.DELIMITER
                    + pdu_header.getEndtoendID()+XTTProperties.DELIMITER
                    + pdu_header.getCmdflags());
                
                Diameter_PDUBody pdu_body=new Diameter_PDUBody("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + ")",extendedStoreVar);

                String[] storeVar=new String[]{"DIAMETER","DIAMETER/"+myDiamServerPort};
                pdu_body.readPDUBody(in,pdu_header,extendedOutput,storeVar);
                if(pdu_header.isRequest())
                {
                	if (DiameterManager.runLoad)
                	{
                		handeleCmdAndUsersReq(pdu_header.getCommandcode(),pdu_body.getAVPMap());	
                	}
                	if(!sendDiameterResponse(pdu_header,pdu_body,out))
                		return;
                }
                pcrfServer.notifyRequest(pdu_header.getCommandcode());
                synchronized(requestkey)
                {
                    requestcount++;
						requestkey.notifyAll();
					}
				}
			}

        } finally
        {
            pcefSocket.close();
            XTTProperties.printDebug("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
            }
            
            
            pcrfServer.removeWorker(this);
        }
    }
    /*
     * Add user to the DB 
     */
	public synchronized void handeleCmdAndUsersReq(int cmdCode, HashMap<Integer, Vector<Diameter_AVP>> avpMap)
			throws Exception
	{
		String sessionId = "";
		Vector<Diameter_AVP> currentAVP = new Vector<Diameter_AVP>();
		CmdDataItem.totalUsers = DiameterManager.usersTab.users.size();
		
		pcef_IP =    pcefSocket.getInetAddress().getHostAddress();
		pcrf_IP =    pcrfServer.getIp();
		pcef_Port =  pcefSocket.getLocalPort()+ "";
		pcrf_Port =  pcefSocket.getPort()     + ""; 
		
		if (cmdCode == CCR)
		{
			currentAVP = avpMap.get((getAVPCode("CC_REQUEST_TYPE")));
			Iterator<Diameter_AVP> itAVP = currentAVP.iterator();
			while (itAVP.hasNext())
			{
				Diameter_AVP avp = itAVP.next();
				if (avp.data == "INITIAL_REQUEST")
				{
					XTTProperties.printDebug("CCRI-INITIAL_REQUEST For SessionId: " + sessionId);
					CmdDataItem.totalCCRIReq++;
					currentAVP = avpMap.get(getAVPCode("SESSION_ID"));
					sessionId = currentAVP.elementAt(0).data;
					currentAVP = avpMap.get(getAVPCode("FRAMED_IP_ADDRESS"));
					String ipAddress = StringUtils.hexToIp(currentAVP.elementAt(0).data);
					DiameterManager.usersTab.addUser(sessionId, ipAddress);
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("S",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "CCRI", CmdDataItem.Type.REQUEST, "INITIAL",
					DiameterManager.usersTab.getUser(sessionId));
					break;
				}
				else if (avp.data == "TERMINATION_REQUEST")
				{
					XTTProperties.printDebug("CCRI-TERMINATION_REQUEST For SessionId: " + sessionId);
					CmdDataItem.totalCCRTReq++;
					currentAVP = avpMap.get(getAVPCode("SESSION_ID"));
					sessionId = currentAVP.elementAt(0).data;
					UserDataItem user  = DiameterManager.usersTab.getUser(sessionId);
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("S",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "CCRT", CmdDataItem.Type.REQUEST, "TERMINATION",user);
					if (user != null) 
						DiameterManager.usersTab.removeUser(sessionId);
					break;
				}
				else if (avp.data == "UPDATE_REQUEST")
				{
					XTTProperties.printDebug("CCRI-UPDATE_REQUEST For SessionId: " + sessionId);
					CmdDataItem.totalCCRUReq++;
					currentAVP = avpMap.get(getAVPCode("SESSION_ID"));
					sessionId = currentAVP.elementAt(0).data;
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("S",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "CCRU", CmdDataItem.Type.REQUEST, "UPDATE",
					DiameterManager.usersTab.getUser(sessionId));
					break;
				}
			}
		}
		else if (cmdCode == DWR)
		{
				XTTProperties.printDebug("DEVICE_WATCHDOG_REQUEST For SessionId: " + sessionId);
				if (DiameterManager.createtranslog==true)
				DiameterManager.cmdTab.addCmd("S",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "DWD", CmdDataItem.Type.REQUEST,"DWD",null);
				CmdDataItem.totalDWDReq++;
		}
		else if (cmdCode == CER)
		{
				XTTProperties.printDebug("CAPABILITIES_EXCHANGE_REQUEST For SessionId: " + sessionId);
				CmdDataItem.totalCERReq++;
				if (DiameterManager.createtranslog==true)
				DiameterManager.cmdTab.addCmd("S",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "CER", CmdDataItem.Type.REQUEST,"CER",null);
		}
	}
	//This function get the pdu_header full from the inputstream and add to it the AVP from the xml nad send it 
    public synchronized boolean sendDiameterResponse(Diameter_PDUHeader pdu_header, Diameter_PDUBody pdu_body, BufferedOutputStream out) throws Exception
    {
    	
    	Element currentElement=null; // Current defined response for REQUEST
        Vector<String> matchAVP=null;
        HashMap<String,String> matchREGEX=null;
        Pattern pattern=null;
        Matcher matcher=null;
        Vector<Diameter_AVP> avps=null;
        Diameter_AVP avp=null;
        boolean found=false;
        String tempAVP=null;
        String tempREGEX=null;
        
        StringBuffer output=new StringBuffer("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): RESPONSE: Header/Body: ");
        Diameter_PDUBody response_body = new Diameter_PDUBody("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
        
        //Set the req to res
        pdu_header.setRequest(false);
        //Get the all relevant block from xml file 
        Vector<Element> responseElemets = pcrfServer.getResponseElements(pdu_header.getCommandcode());

       // System.out.println("elemts = " + responseElemets);

        if(responseElemets==null)
        {
            XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): no response defined for " + pdu_header.getCommandname());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        }
        
        Iterator<Element> it=responseElemets.iterator();  // Iterator over all defined responses for this REQUEST
        
        StringBuffer output2=new StringBuffer("");
        //Since we have a few CC type in the xml so we need to know which are the relevant CC we need the (matchavp and matchregex)  
        // For all defined responses for this REQUEST 
        while(it.hasNext()&&!found)
        {
            output2=new StringBuffer();
            currentElement=it.next();
            matchAVP=new Vector<String>();
            matchREGEX=new HashMap<String,String>();
            
            // Store AVP and REGEXES for this defined response
            tempAVP=getElementAttribute(currentElement,"matchavp");
            
            if(tempAVP!=null)
            {
                matchAVP.add(tempAVP);
                tempREGEX=getElementAttribute(currentElement,"matchregex");
                if(tempREGEX!=null)
                {
                    matchREGEX.put(tempAVP,tempREGEX);
                }
            }
            for(int i=0;i<10;i++)
            {
                tempAVP=getElementAttribute(currentElement,"matchavp" + i);
                if(tempAVP!=null)
                {
                    matchAVP.add(tempAVP);
                    tempREGEX=getElementAttribute(currentElement,"matchregex" + i);
                    if(tempREGEX!=null)
                    {
                        matchREGEX.put(tempAVP,tempREGEX);
                    }
                } else
                {
                    break;
                }
            }
            
            // if there are no AVPs defined we have a match
            if(matchAVP.size()<=0)
            {
                output.append("\nResponse MATCHED: " + currentElement.getName()+ "");
                break;
            } else
            {
                // Get the list of all AVPs defined for this RESPONSE
                Iterator<String> matchAVPs=matchAVP.iterator();
                while(matchAVPs.hasNext()&&currentElement!=null)
                {
                    tempAVP=matchAVPs.next();
                    avps=pdu_body.getAVPMap().get(getAVPCode(tempAVP));
                    // if this REQUEST has one or more of the AVP we are comparing against
                    if(avps!=null)
                    {
                        // Get the defined REGEX for this AVP
                        tempREGEX=matchREGEX.get(tempAVP);
                        // If we have a REGEX for this AVP
                        if(tempREGEX!=null)
                        {
                            // for all AVPs that we have in this REQUEST that matched
                            Iterator<Diameter_AVP> itAVP=avps.iterator();
                            // Compile the pattern for this defined REGEX
                            pattern = Pattern.compile(tempREGEX,Pattern.DOTALL);
                            found=false;
                            while(itAVP.hasNext()&&!found)
                            {
                                avp=itAVP.next();
                                matcher = pattern.matcher(avp.data);
                                if(matcher.find())
                                {
                                    found=true;
                                    output2.append("\nResponse MATCHED: "+currentElement.getName()+" matchAVP:'"+tempAVP+"' matchREGEX:'"+tempREGEX+"'");
                                    break;
                                }
                            }
                            if(!found)
                            {
                                currentElement=null;
                                output2=new StringBuffer();
                            }
                        // if we do not have a REGEX defined
                        } else
                        {
                            found=true;
                            output2.append("\nResponse MATCHED: " + currentElement.getName() + " matchAVP:'"+tempREGEX+"'");
                        }
                    // if this REQUEST does not have the AVP we are comparing against set current response to null
                    } else
                    {
                        currentElement=null;
                        output2=new StringBuffer();
                    }
                }
            }
            output.append(output2);
        }
        
        if(currentElement==null)
        {
            XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): no response matched for "+pdu_header.getCommandname());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return false;
        }
        pdu_header.setProxiable(ConvertLib.textToBoolean(getElementAttribute(currentElement,"proxiable")));
        pdu_header.setError(ConvertLib.textToBoolean(getElementAttribute(currentElement,"error")));
        pdu_header.setTretr(ConvertLib.textToBoolean(getElementAttribute(currentElement,"tretr")));

        int responseDelay=0;
        try
        {
            responseDelay=Integer.decode(getElementAttribute(currentElement,"responsedelay"));
        } catch (Exception ex)
        {}
            
        // Assemble the response PDUs with the data of the found response XML document as root
        isOrgRoot = true;	
        response_body.setAVPs(createAVPsFromXml(currentElement,pdu_body.getAVPMap(),null,CmdDataItem.Type.RESPONSE));
        	
        //System.out.println("RAVPS: "+response_body.getAVPs());
        // Convert the defined response AVPs to binary ---> Guy Here the convert pdu to bin
        byte[] body=response_body.createPDUBody();
        //System.out.println("RBODY: \n"+ConvertLib.getHexView(body)); 
        pdu_header.setMessagelength(pdu_header.HEADLENGTH+body.length);
        // Create the binary header---> Guy Here the convert header to bin
        byte[] header=pdu_header.createPDUHeader();

        if(perworkerdelay>0)
        {
            try
            {
                output.append("\nResponse per Worker delay: "+(perworkerdelay*(getWorkerId()+1))+"ms");
                Thread.sleep(perworkerdelay*(getWorkerId()+1));
            } catch(InterruptedException ex){};
        }
        if(responseDelay>0)
        {
            try
            {
                output.append("\nResponse DELAYED by "+responseDelay+"ms");
                Thread.sleep(responseDelay);
            } catch(InterruptedException ex){};
        }

        // Write the response
        out.write(header);
        out.write(body);
        out.flush();
        XTTProperties.printTransaction("DiameterWorkerServer/RESPONSE"+XTTProperties.DELIMITER+myDiamServerPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
            + pdu_header.getCommandname()+XTTProperties.DELIMITER
            + pdu_header.getApplicationID()+XTTProperties.DELIMITER
            + pdu_header.getHopbyhopID()+XTTProperties.DELIMITER
            + pdu_header.getEndtoendID()+XTTProperties.DELIMITER
            + matchAVP+XTTProperties.DELIMITER
            + matchREGEX);
        
        output.append("\n"+ ConvertLib.getHexView(header));
        //output.append("\nDiameterWorkerServer("+myServerPort+"/"+getWorkerId()+"): RESPONSE: Body:");
        output.append(""+ ConvertLib.getHexView(body));
        
        if(extendedOutput)
        {
            StringBuffer extoutput=new StringBuffer("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): RESPONSE: Body created: \n");
            Diameter_PDUBody sent_response=new Diameter_PDUBody("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
            String[] storeVar     = new String[] {"DIAMETER/"+myDiamServerPort+"/RESPONSE","DIAMETER/RESPONSE"};
            sent_response.decodePDUBody(body,pdu_header,extoutput,output,storeVar);
        }
        //if(extendedOutput)XTTProperties.printDebug(extoutput.toString());
        XTTProperties.printDebug(output.toString());
        return true;
    }
    /*
     * Create the child AVPs from a Response XML root node
     * This function is recursion function.
     */
    private Vector<Diameter_AVP> createAVPsFromXml (Element root,HashMap<Integer, Vector<Diameter_AVP>> avpMap,String sSessionId,CmdDataItem.Type type) throws Exception
    {
    	   String cmdName="";
    	   CmdDataItem.Type cmdType = type; 
    	   
    	pcef_IP =    pcefSocket.getInetAddress().getHostAddress();
   		pcrf_IP =    pcrfServer.getIp();
   		pcef_Port =  pcefSocket.getLocalPort()+ "";
   		pcrf_Port =  pcefSocket.getPort()     + ""; 
    	    
    	 Vector<Diameter_AVP> returnValues=new Vector<Diameter_AVP>();
    	 if(root==null) return returnValues;
    	 //if is first call since this is a recursion function
		if (isOrgRoot && DiameterManager.runLoad)
		{
			CmdDataItem.totalUsers = DiameterManager.usersTab.users.size();
			cmdName = root.getName();
			if (cmdType == CmdDataItem.Type.RESPONSE)
			{
				if (cmdName.equalsIgnoreCase("CREDIT_CONTROL_REQUEST"))
				{
					sSessionId = avpMap.get(getAVPCode("SESSION_ID")).elementAt(0).data;
					Vector<Diameter_AVP> currentAVP = avpMap.get((getAVPCode("CC_REQUEST_TYPE")));
					Iterator<Diameter_AVP> itAVP = currentAVP.iterator();
					while (itAVP.hasNext())
					{
						Diameter_AVP avp = itAVP.next();
						if (avp.data =="INITIAL_REQUEST")
						{
							
							XTTProperties.printDebug("CREDIT_CONTROL_INIT_RESPONSE For SessionId: " + sSessionId);
							CmdDataItem.totalCCRIRes++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("S",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CCRI", cmdType, "INITIAL", DiameterManager.usersTab.getUser(sSessionId));
							break;
						}
						if (avp.data =="TERMINATION_REQUEST")
						{
							//sSessionId = avpMap.get(getAVPCode("SESSION_ID")).elementAt(0).data;
							XTTProperties.printDebug("CREDIT_CONTROL_TERMINATION_RESPONSE For SessionId: " + sSessionId);
							CmdDataItem.totalCCRTRes++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("S",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CCRT", cmdType, "TERMINATION", DiameterManager.usersTab.getUser(sSessionId));
							break;
						}
						if (avp.data =="UPDATE_REQUEST")
						{
							//sSessionId = avpMap.get(getAVPCode("SESSION_ID")).elementAt(0).data;
							XTTProperties.printDebug("CREDIT_CONTROL_UPDATE_RESPONSE For SessionId: " + sSessionId);
							CmdDataItem.totalCCRURes++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("S",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CCRU", cmdType, "UPDATE", DiameterManager.usersTab.getUser(sSessionId));
							break;
						}
					}
				}
				if (cmdName.equalsIgnoreCase("DEVICE_WATCHDOG_REQUEST"))
				{
					XTTProperties.printDebug("DEVICE_WATCHDOG_REQUEST_RESPONSE");
					CmdDataItem.totalDWDRes++;
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("S",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "DWD", cmdType, "DWD", null);
				}
				if (cmdName.equalsIgnoreCase("CAPABILITIES_EXCHANGE_REQUEST"))
				{
					XTTProperties.printDebug("CAPABILITIES_EXCHANGE_RESPONSE");
					CmdDataItem.totalCERRes++;
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("S",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CER", cmdType, "CER", null);
				}
			}
		}
        // get the iterator over all AVP children of the root
        Iterator<?> itRoot = root.getChildren().iterator();
        // the current XML AVP
        Element currentE=null;
        // The current created AVP
        Diameter_AVP currentAVP=null;
        Diameter_AVP currentAVPRule= null;
        // A vector for storing the children
        Vector<Diameter_AVP> currentAVPs=new Vector<Diameter_AVP>();
        String tempReqAttr=null;
        int avpcode=0;
        // For each defined child for this ROOT AVP
        while(itRoot.hasNext())
        {
            currentE=(Element)itRoot.next();
            avpcode=getAVPCode(currentE.getName());
            if(avpcode<=0)
            {
                XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId()+ "): AVP not found " + currentE.getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception("AVP not found "+currentE.getName());
            }

            tempReqAttr=getElementAttribute(currentE,"request");
            if(tempReqAttr!=null&&!tempReqAttr.equals(""))
            {
                // We have to get response AVPs directly from the REQUEST
                try
                {
                	//Guy Get From the Req the AVP
                	currentAVPs=avpMap.get(getAVPCode(tempReqAttr));
                	
                    if(currentAVPs==null||currentAVPs.size()<=0)
                    {
                        XTTProperties.printFail("DiameterWorkerServer(" + myDiamServerPort + "/" + getWorkerId() + "): AVP not found in request " + tempReqAttr);
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        throw new Exception("AVP not found in request " + tempReqAttr);
                    }
                } catch (NullPointerException nex)
                {
                    throw new Exception("AVP not found in request " + tempReqAttr,nex);
                }
            } else
            {
                currentAVPs=new Vector<Diameter_AVP>();
                currentAVPs.add(new Diameter_AVP());
            }

            Iterator<Diameter_AVP> cit=currentAVPs.iterator();
            // For each AVP we have, either a single one or a 1 to n from the request
            while(cit.hasNext())
            {
                currentAVP=cit.next();
                currentAVP.avpcode=avpcode;
    
                tempReqAttr=getElementAttribute(currentE,"mandatory");
                
	                if(tempReqAttr!=null&&!tempReqAttr.equals(""))
	                {
	                	currentAVP.mandatory  = ConvertLib.textToBoolean(tempReqAttr);
	                }
	                
	                tempReqAttr=getElementAttribute(currentE,"protected");
	                
	                if(tempReqAttr!=null&&!tempReqAttr.equals(""))
	                {
	                	currentAVP.protectedf  = ConvertLib.textToBoolean(tempReqAttr);
	                }	
	                tempReqAttr=getElementAttribute(currentE,"vendorspecific");
	                
	                if(tempReqAttr!=null&&!tempReqAttr.equals(""))
	                {
	                	currentAVP.vendorspec  = ConvertLib.textToBoolean(tempReqAttr);
	                }
	                
	                tempReqAttr=getElementAttribute(currentE,"vendorid");
	                
	                if(currentAVP.vendorspec&&tempReqAttr!=null&&!tempReqAttr.equals(""))
	                {
	                    currentAVP.vendorID=Integer.decode(tempReqAttr);
	                }
	                tempReqAttr=getElementAttribute(currentE, "overrideavpcode");
					
	                if(tempReqAttr!=null&&!tempReqAttr.equals(""))
					{
						currentAVP.overrideavpcode = Integer.parseInt(tempReqAttr);
					}
	                //If tempReqAttr=value take from xml the value
	                tempReqAttr=getElementAttribute(currentE,"value");
		                //Here the copy from the Req to the xml
		                if(tempReqAttr!=null&&!tempReqAttr.equals(""))
		                {	
		                	if (DiameterManager.runLoad)
		                    {
		                		if (avpcode==CHARGING_RULE_REMOVE)
			                    {
	                    				isChargingRuleRemove =true;
		                    	}
			                    if (avpcode==SUBSCRIPTION_ID_DATA && DiameterManager.customer.equalsIgnoreCase("vimpelcom"))
			                    {
	                 				currentAVP.data  =  DiameterManager.usersTab.getMsisdn(sSessionId);
	                  			}
		                    	else if (avpcode==CHARGING_RULE_NAME)
		                    	{
		                    			  chargingRuleNameCount++;
		                    				
		                    				if (chargingRuleNameCount==1)
	                    					{
			                    				String ruleCat = DiameterManager.usersTab.getRuleCat(sSessionId);
			                    				String ruleSubCat =  DiameterManager.usersTab.getRuleSubCat(sSessionId);
			                    				
				                    				if (ruleCat!=ruleSubCat)
				                    				{
						                    			//New avp for sub rule
					                    				currentAVPRule = new Diameter_AVP();
						                    			currentAVPRule.avpcode=currentAVP.avpcode;
						                    			currentAVPRule.mandatory=currentAVP.mandatory;
						                    			currentAVPRule.protectedf=currentAVP.protectedf;
						                    			currentAVPRule.vendorspec=currentAVP.vendorspec;
						                    			currentAVPRule.vendorID=currentAVP.vendorID;
						                    			currentAVPRule.overrideavpcode=currentAVP.overrideavpcode;
						                    			if (isChargingRuleRemove)
						                    			{
						                    				//Need to take the old rule from the user
						                    				currentAVP.data  = StringUtils.bytesToString(DiameterManager.usersTab.getRuleCat(sSessionId).getBytes());
						                    				currentAVPRule.data  = StringUtils.bytesToString(DiameterManager.usersTab.getRuleSubCat(sSessionId).getBytes());
						                    				isChargingRuleRemove =false;
						                    			}
						                    			else
						                    			{
						                    				currentAVP.data  = StringUtils.bytesToString(ruleCat.getBytes());
						                    				currentAVPRule.data  = StringUtils.bytesToString(ruleSubCat.getBytes());
						                    			}
				                    				}
				                    				else 
				                    				{
				                    					currentAVP.data  = StringUtils.bytesToString(ruleCat.getBytes());
				                    				}
	                    					}
		                    				if (chargingRuleNameCount==2)
		                    				{
		                    					String newRuleCat = DiameterManager.usersTab.getNewRuleCat(sSessionId);
			                    				String newRuleSubCat =  DiameterManager.usersTab.getNewRuleSubCat(sSessionId);
			                    				
				                    				if (newRuleCat!=newRuleSubCat)
				                    				{				                    					
					                    				currentAVP.data  = StringUtils.bytesToString(newRuleCat.getBytes());
					                    				DiameterManager.usersTab.setNewRuleCat(sSessionId, DiameterManager.usersTab.getRuleCat(sSessionId));
						                    			DiameterManager.usersTab.setRuleCat(sSessionId, newRuleCat);
					                    				//New avp for sub rule
					                    				currentAVPRule = new Diameter_AVP();
						                    			currentAVPRule.avpcode=currentAVP.avpcode;
						                    			currentAVPRule.mandatory=currentAVP.mandatory;
						                    			currentAVPRule.protectedf=currentAVP.protectedf;
						                    			currentAVPRule.vendorspec=currentAVP.vendorspec;
						                    			currentAVPRule.vendorID=currentAVP.vendorID;
						                    			currentAVPRule.overrideavpcode=currentAVP.overrideavpcode;
						                    			currentAVPRule.data  = StringUtils.bytesToString(newRuleSubCat.getBytes());
						                    			DiameterManager.usersTab.setNewRuleSubCat(sSessionId, DiameterManager.usersTab.getRuleSubCat(sSessionId));
						                    			DiameterManager.usersTab.setRuleSubCat(sSessionId, newRuleSubCat);
						                    			chargingRuleNameCount=0;
				                    				}
				                    				else 
				                    				{
				                    					currentAVP.data  = StringUtils.bytesToString(newRuleCat.getBytes());
				                    				}
		                    				}
		                    			}
		                    			else
		                    			{
		                    				currentAVP.data  = tempReqAttr;
		                    			}
		                    				
		                    }
		                	else
		                	{
		                		currentAVP.data  = tempReqAttr;
		                	}
		                }
                // Set the child AVPs for this AVP
                //System.out.println(currentAVP);
		        isOrgRoot=false;        
                currentAVP.setGroupedAVPs(createAVPsFromXml(currentE,avpMap,sSessionId,type));
                // Add the current AVP to the response list
                returnValues.add(currentAVP);
	                if (DiameterManager.runLoad)
	                	if ( currentAVPRule!=null)
	                		returnValues.add(currentAVPRule);
                
            }
        }
        	
        return returnValues;
    }
    private String getElementAttribute(Element element,String name)
    {
        Iterator<?> it=element.getAttributes().iterator();
        Attribute attribute=null;
        while(it.hasNext())
        {
            attribute=(Attribute)it.next();
            if(attribute.getName().toLowerCase().equals(name))
            {
                return attribute.getValue();
            }
        }
        return null;
    }
    public static void init()
    {
        synchronized (requestkey)
        {
            requestcount=0;
        }
        extendedStoreVar.clear();
        extendedOutput=false;
        perworkerdelay=0;
    }
    public static void setPerWorkerDelay(int workerdelay)
    {
        perworkerdelay=workerdelay;
    }
    public static void addExtendedStoreVar(String var)
    {
        extendedStoreVar.add(var.toLowerCase());
    }
    public static void setExtendedOutput(boolean exo)
    {
        extendedOutput=exo;
    }
    public static void waitForRequests(int number) throws java.lang.InterruptedException
    {
        if(DiameterServer.checkSockets())
        {
            XTTProperties.printFail("DiameterWorkerServer.waitForRequests: no instance running!");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        int wait=XTTProperties.getIntProperty("DiameterSERVER/WAITTIMEOUT");
        if(wait<0)wait=DiameterManager.DEFAULTTIMEOUT;
        int prevcount=0;
        synchronized(requestkey)
        {
            while(requestcount<number)
            {
                XTTProperties.printInfo("DiameterWorkerServer.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("DiameterWorkerServer.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("DiameterWorkerServer.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(DiameterServer.checkSockets())
        {
            XTTProperties.printFail("DiameterWorkerServer.waitForTimeoutRequests: no instance running!");
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
                XTTProperties.printInfo("DiameterWorkerServer.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("DiameterWorkerServer.waitForTimeoutRequests: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("DiameterWorkerServer.waitForTimeoutRequests: request received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    public static int getAVPCode(String avpName)
    {
        try
        {
            return Integer.decode(avpName);
        } catch(Exception ex)
        {}
        // Diameter AVPs
             if(avpName.equalsIgnoreCase("ACCT_INTERIM_INTERVAL"))           {return ACCT_INTERIM_INTERVAL           ;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_REALTIME_REQUIRED"))    {return ACCOUNTING_REALTIME_REQUIRED    ;}
        else if(avpName.equalsIgnoreCase("ACCT_MULTI_SESSION_ID"))           {return ACCT_MULTI_SESSION_ID           ;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_RECORD_NUMBER"))        {return ACCOUNTING_RECORD_NUMBER        ;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_RECORD_TYPE"))          {return ACCOUNTING_RECORD_TYPE          ;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_SESSION_ID"))           {return ACCOUNTING_SESSION_ID           ;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_SUB_SESSION_ID"))       {return ACCOUNTING_SUB_SESSION_ID       ;}
        else if(avpName.equalsIgnoreCase("ACCT_APPLICATION_ID"))             {return ACCT_APPLICATION_ID             ;}
        else if(avpName.equalsIgnoreCase("AUTH_APPLICATION_ID"))             {return AUTH_APPLICATION_ID             ;}
        else if(avpName.equalsIgnoreCase("AUTH_REQUEST_TYPE"))               {return AUTH_REQUEST_TYPE               ;}
        else if(avpName.equalsIgnoreCase("AUTHORIZATION_LIFETIME"))          {return AUTHORIZATION_LIFETIME          ;}
        else if(avpName.equalsIgnoreCase("AUTH_GRACE_PERIOD"))               {return AUTH_GRACE_PERIOD               ;}
        else if(avpName.equalsIgnoreCase("AUTH_SESSION_STATE"))              {return AUTH_SESSION_STATE              ;}
        else if(avpName.equalsIgnoreCase("RE_AUTH_REQUEST_TYPE"))            {return RE_AUTH_REQUEST_TYPE            ;}
        else if(avpName.equalsIgnoreCase("CLASS"))                           {return CLASS                           ;}
        else if(avpName.equalsIgnoreCase("DESTINATION_HOST"))                {return DESTINATION_HOST                ;}
        else if(avpName.equalsIgnoreCase("DESTINATION_REALM"))               {return DESTINATION_REALM               ;}
        else if(avpName.equalsIgnoreCase("DISCONNECT_CAUSE"))                {return DISCONNECT_CAUSE                ;}
        else if(avpName.equalsIgnoreCase("E2E_SEQUENCE"))                    {return E2E_SEQUENCE                    ;}
        else if(avpName.equalsIgnoreCase("ERROR_MESSAGE"))                   {return ERROR_MESSAGE                   ;}
        else if(avpName.equalsIgnoreCase("ERROR_REPORTING_HOST"))            {return ERROR_REPORTING_HOST            ;}
        else if(avpName.equalsIgnoreCase("EVENT_TIMESTAMP"))                 {return EVENT_TIMESTAMP                 ;}
        else if(avpName.equalsIgnoreCase("EXPERIMENTAL_RESULT"))             {return EXPERIMENTAL_RESULT             ;}
        else if(avpName.equalsIgnoreCase("EXPERIMENTAL_RESULT_CODE"))        {return EXPERIMENTAL_RESULT_CODE        ;}
        else if(avpName.equalsIgnoreCase("FAILED_AVP"))                      {return FAILED_AVP                      ;}
        else if(avpName.equalsIgnoreCase("FIRMWARE_REVISION"))               {return FIRMWARE_REVISION               ;}
        else if(avpName.equalsIgnoreCase("HOST_IP_ADDRESS"))                 {return HOST_IP_ADDRESS                 ;}
        else if(avpName.equalsIgnoreCase("INBAND_SECURITY_ID"))              {return INBAND_SECURITY_ID              ;}
        else if(avpName.equalsIgnoreCase("MULTI_ROUND_TIME_OUT"))            {return MULTI_ROUND_TIME_OUT            ;}
        else if(avpName.equalsIgnoreCase("ORIGIN_HOST"))                     {return ORIGIN_HOST                     ;}
        else if(avpName.equalsIgnoreCase("ORIGIN_REALM"))                    {return ORIGIN_REALM                    ;}
        else if(avpName.equalsIgnoreCase("ORIGIN_STATE_ID"))                 {return ORIGIN_STATE_ID                 ;}
        else if(avpName.equalsIgnoreCase("PRODUCT_NAME"))                    {return PRODUCT_NAME                    ;}
        else if(avpName.equalsIgnoreCase("PROXY_HOST"))                      {return PROXY_HOST                      ;}
        else if(avpName.equalsIgnoreCase("PROXY_INFO"))                      {return PROXY_INFO                      ;}
        else if(avpName.equalsIgnoreCase("PROXY_STATE"))                     {return PROXY_STATE                     ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_HOST"))                   {return REDIRECT_HOST                   ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_HOST_USAGE"))             {return REDIRECT_HOST_USAGE             ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_MAX_CACHE_TIME"))         {return REDIRECT_MAX_CACHE_TIME         ;}
        else if(avpName.equalsIgnoreCase("RESULT_CODE"))                     {return RESULT_CODE                     ;}
        else if(avpName.equalsIgnoreCase("ROUTE_RECORD"))                    {return ROUTE_RECORD                    ;}
        else if(avpName.equalsIgnoreCase("SESSION_ID"))                      {return SESSION_ID                      ;}
        else if(avpName.equalsIgnoreCase("SESSION_TIMEOUT"))                 {return SESSION_TIMEOUT                 ;}
        else if(avpName.equalsIgnoreCase("SESSION_BINDING"))                 {return SESSION_BINDING                 ;}
        else if(avpName.equalsIgnoreCase("SESSION_SERVER_FAILOVER"))         {return SESSION_SERVER_FAILOVER         ;}
        else if(avpName.equalsIgnoreCase("SUPPORTED_VENDOR_ID"))             {return SUPPORTED_VENDOR_ID             ;}
        else if(avpName.equalsIgnoreCase("TERMINATION_CAUSE"))               {return TERMINATION_CAUSE               ;}
        else if(avpName.equalsIgnoreCase("USER_NAME"))                       {return USER_NAME                       ;}
        else if(avpName.equalsIgnoreCase("VENDOR_ID"))                       {return VENDOR_ID                       ;}
        else if(avpName.equalsIgnoreCase("VENDOR_SPECIFIC_APPLICATION_ID"))  {return VENDOR_SPECIFIC_APPLICATION_ID  ;}
        //  Credit-Control AVPs
        else if(avpName.equalsIgnoreCase("CC_CORRELATION_ID"))               {return CC_CORRELATION_ID               ;}
        else if(avpName.equalsIgnoreCase("CC_INPUT_OCTETS"))                 {return CC_INPUT_OCTETS                 ;}
        else if(avpName.equalsIgnoreCase("CC_MONEY"))                        {return CC_MONEY                        ;}
        else if(avpName.equalsIgnoreCase("CC_OUTPUT_OCTETS"))                {return CC_OUTPUT_OCTETS                ;}
        else if(avpName.equalsIgnoreCase("CC_REQUEST_NUMBER"))               {return CC_REQUEST_NUMBER               ;}
        else if(avpName.equalsIgnoreCase("CC_REQUEST_TYPE"))                 {return CC_REQUEST_TYPE                 ;}
        else if(avpName.equalsIgnoreCase("CC_SERVICE_SPECIFIC_UNITS"))       {return CC_SERVICE_SPECIFIC_UNITS       ;}
        else if(avpName.equalsIgnoreCase("CC_SESSION_FAILOVER"))             {return CC_SESSION_FAILOVER             ;}
        else if(avpName.equalsIgnoreCase("CC_SUB_SESSION_ID"))               {return CC_SUB_SESSION_ID               ;}
        else if(avpName.equalsIgnoreCase("CC_TIME"))                         {return CC_TIME                         ;}
        else if(avpName.equalsIgnoreCase("CC_TOTAL_OCTETS"))                 {return CC_TOTAL_OCTETS                 ;}
        else if(avpName.equalsIgnoreCase("CC_UNIT_TYPE"))                    {return CC_UNIT_TYPE                    ;}
        else if(avpName.equalsIgnoreCase("CHECK_BALANCE_RESULT"))            {return CHECK_BALANCE_RESULT            ;}
        else if(avpName.equalsIgnoreCase("COST_INFORMATION"))                {return COST_INFORMATION                ;}
        else if(avpName.equalsIgnoreCase("COST_UNIT"))                       {return COST_UNIT                       ;}
        else if(avpName.equalsIgnoreCase("CREDIT_CONTROL"))                  {return CREDIT_CONTROL                  ;}
        else if(avpName.equalsIgnoreCase("CREDIT_CONTROL_FAILURE_HANDLING")) {return CREDIT_CONTROL_FAILURE_HANDLING ;}
        else if(avpName.equalsIgnoreCase("CURRENCY_CODE"))                   {return CURRENCY_CODE                   ;}
        else if(avpName.equalsIgnoreCase("DIRECT_DEBITING_FAILURE_HANDLING")){return DIRECT_DEBITING_FAILURE_HANDLING;}
        else if(avpName.equalsIgnoreCase("EXPONENT"))                        {return EXPONENT                        ;}
        else if(avpName.equalsIgnoreCase("FINAL_UNIT_ACTION"))               {return FINAL_UNIT_ACTION               ;}
        else if(avpName.equalsIgnoreCase("FINAL_UNIT_INDICATION"))           {return FINAL_UNIT_INDICATION           ;}
        else if(avpName.equalsIgnoreCase("GRANTED_SERVICE_UNIT"))            {return GRANTED_SERVICE_UNIT            ;}
        else if(avpName.equalsIgnoreCase("G_S_U_POOL_IDENTIFIER"))           {return G_S_U_POOL_IDENTIFIER           ;}
        else if(avpName.equalsIgnoreCase("G_S_U_POOL_REFERENCE"))            {return G_S_U_POOL_REFERENCE            ;}
        else if(avpName.equalsIgnoreCase("MULTIPLE_SERVICES_CREDIT_CONTROL")){return MULTIPLE_SERVICES_CREDIT_CONTROL;}
        else if(avpName.equalsIgnoreCase("MULTIPLE_SERVICES_INDICATOR"))     {return MULTIPLE_SERVICES_INDICATOR     ;}
        else if(avpName.equalsIgnoreCase("RATING_GROUP"))                    {return RATING_GROUP                    ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_ADDRESS_TYPE"))           {return REDIRECT_ADDRESS_TYPE           ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_SERVER"))                 {return REDIRECT_SERVER                 ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_SERVER_ADDRESS"))         {return REDIRECT_SERVER_ADDRESS         ;}
        else if(avpName.equalsIgnoreCase("REDIRECT_MODE"))         			 {return REDIRECT_MODE                   ;}
        else if(avpName.equalsIgnoreCase("REQUESTED_ACTION"))                {return REQUESTED_ACTION                ;}
        else if(avpName.equalsIgnoreCase("REQUESTED_SERVICE_UNIT"))          {return REQUESTED_SERVICE_UNIT          ;}
        else if(avpName.equalsIgnoreCase("RESTRICTION_FILTER_RULE"))         {return RESTRICTION_FILTER_RULE         ;}
        else if(avpName.equalsIgnoreCase("SERVICE_CONTEXT_ID"))              {return SERVICE_CONTEXT_ID              ;}
        else if(avpName.equalsIgnoreCase("SERVICE_IDENTIFIER"))              {return SERVICE_IDENTIFIER              ;}
        else if(avpName.equalsIgnoreCase("SERVICE_PARAMETER_INFO"))          {return SERVICE_PARAMETER_INFO          ;}
        else if(avpName.equalsIgnoreCase("SERVICE_PARAMETER_TYPE"))          {return SERVICE_PARAMETER_TYPE          ;}
        else if(avpName.equalsIgnoreCase("SERVICE_PARAMETER_VALUE"))         {return SERVICE_PARAMETER_VALUE         ;}
        else if(avpName.equalsIgnoreCase("SUBSCRIPTION_ID"))                 {return SUBSCRIPTION_ID                 ;}
        else if(avpName.equalsIgnoreCase("SUBSCRIPTION_ID_DATA"))            {return SUBSCRIPTION_ID_DATA            ;}
        else if(avpName.equalsIgnoreCase("SUBSCRIPTION_ID_TYPE"))            {return SUBSCRIPTION_ID_TYPE            ;}
        else if(avpName.equalsIgnoreCase("TARIFF_CHANGE_USAGE"))             {return TARIFF_CHANGE_USAGE             ;}
        else if(avpName.equalsIgnoreCase("TARIFF_TIME_CHANGE"))              {return TARIFF_TIME_CHANGE              ;}
        else if(avpName.equalsIgnoreCase("UNIT_VALUE"))                      {return UNIT_VALUE                      ;}
        else if(avpName.equalsIgnoreCase("USED_SERVICE_UNIT"))               {return USED_SERVICE_UNIT               ;}
        else if(avpName.equalsIgnoreCase("USER_EQUIPMENT_INFO"))             {return USER_EQUIPMENT_INFO             ;}
        else if(avpName.equalsIgnoreCase("USER_EQUIPMENT_INFO_TYPE"))        {return USER_EQUIPMENT_INFO_TYPE        ;}
        else if(avpName.equalsIgnoreCase("USER_EQUIPMENT_INFO_VALUE"))       {return USER_EQUIPMENT_INFO_VALUE       ;}
        else if(avpName.equalsIgnoreCase("VALUE_DIGITS"))                    {return VALUE_DIGITS                    ;}
        else if(avpName.equalsIgnoreCase("VALIDITY_TIME"))                   {return VALIDITY_TIME                   ;}
        //  Diameter MIP
        else if(avpName.equalsIgnoreCase("MIP_REG_REQUEST"))                 {return MIP_REG_REQUEST;}
        else if(avpName.equalsIgnoreCase("MIP_REG_REPLY"))                   {return MIP_REG_REPLY;}
        else if(avpName.equalsIgnoreCase("MIP_MN_AAA_AUTH"))                 {return MIP_MN_AAA_AUTH;}
        else if(avpName.equalsIgnoreCase("MIP_MOBILE_NODE_ADDRESS"))         {return MIP_MOBILE_NODE_ADDRESS;}
        else if(avpName.equalsIgnoreCase("MIP_HOME_AGENT_ADDRESS"))          {return MIP_HOME_AGENT_ADDRESS;}
        else if(avpName.equalsIgnoreCase("MIP_CANDIDATE_HOME_AGENT_HOST"))   {return MIP_CANDIDATE_HOME_AGENT_HOST;}
        else if(avpName.equalsIgnoreCase("MIP_FEATURE_VECTOR"))              {return MIP_FEATURE_VECTOR;}
        else if(avpName.equalsIgnoreCase("MIP_AUTH_INPUT_DATA_LENGTH"))      {return MIP_AUTH_INPUT_DATA_LENGTH;}
        else if(avpName.equalsIgnoreCase("MIP_AUTHENTICATOR_LENGTH"))        {return MIP_AUTHENTICATOR_LENGTH;}
        else if(avpName.equalsIgnoreCase("MIP_AUTHENTICATOR_OFFSET"))        {return MIP_AUTHENTICATOR_OFFSET;}
        else if(avpName.equalsIgnoreCase("MIP_MN_AAA_SPI"))                  {return MIP_MN_AAA_SPI;}
        else if(avpName.equalsIgnoreCase("MIP_FILTER_RULE"))                 {return MIP_FILTER_RULE;}
        else if(avpName.equalsIgnoreCase("MIP_FA_CHALLENGE"))                {return MIP_FA_CHALLENGE;}
        else if(avpName.equalsIgnoreCase("MIP_ORIGINATING_FOREIGN_AAA"))     {return MIP_ORIGINATING_FOREIGN_AAA;}
        else if(avpName.equalsIgnoreCase("MIP_HOME_AGENT_HOST"))             {return MIP_HOME_AGENT_HOST;}
        else if(avpName.equalsIgnoreCase("MIP_FA_TO_HA_SPI"))                {return MIP_FA_TO_HA_SPI;}
        else if(avpName.equalsIgnoreCase("MIP_FA_TO_MN_SPI"))                {return MIP_FA_TO_MN_SPI;}
        else if(avpName.equalsIgnoreCase("MIP_HA_TO_FA_SPI"))                {return MIP_HA_TO_FA_SPI;}
        else if(avpName.equalsIgnoreCase("MIP_MN_TO_FA_MSA"))                {return MIP_MN_TO_FA_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_FA_TO_MN_MSA"))                {return MIP_FA_TO_MN_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_FA_TO_HA_MSA"))                {return MIP_FA_TO_HA_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_HA_TO_FA_MSA"))                {return MIP_HA_TO_FA_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_MN_TO_HA_MSA"))                {return MIP_MN_TO_HA_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_HA_TO_MN_MSA"))                {return MIP_HA_TO_MN_MSA;}
        else if(avpName.equalsIgnoreCase("MIP_NONCE"))                       {return MIP_NONCE;}
        else if(avpName.equalsIgnoreCase("MIP_SESSION_KEY"))                 {return MIP_SESSION_KEY;}
        else if(avpName.equalsIgnoreCase("MIP_ALGORITHM_"))                  {return MIP_ALGORITHM_;}
        else if(avpName.equalsIgnoreCase("MIP_REPLAY_MODE"))                 {return MIP_REPLAY_MODE;}
        else if(avpName.equalsIgnoreCase("MIP_MSA_LIFETIME"))                {return MIP_MSA_LIFETIME;}
        // Diameter Network Access Server Application
        else if(avpName.equalsIgnoreCase("NAS_PORT"))                        {return NAS_PORT;}
        else if(avpName.equalsIgnoreCase("NAS_PORT_ID"))                     {return NAS_PORT_ID;}
        else if(avpName.equalsIgnoreCase("NAS_PORT_TYPE"))                   {return NAS_PORT_TYPE;}
        else if(avpName.equalsIgnoreCase("CALLED_STATION_ID"))               {return CALLED_STATION_ID;}
        else if(avpName.equalsIgnoreCase("CALLING_STATION_ID"))              {return CALLING_STATION_ID;}
        else if(avpName.equalsIgnoreCase("CONNECT_INFO"))                    {return CONNECT_INFO;}
        else if(avpName.equalsIgnoreCase("ORIGINATING_LINE_INFO"))           {return ORIGINATING_LINE_INFO;}
        else if(avpName.equalsIgnoreCase("REPLY_MESSAGE"))                   {return REPLY_MESSAGE;}
        else if(avpName.equalsIgnoreCase("USER_PASSWORD"))                   {return USER_PASSWORD;}
        else if(avpName.equalsIgnoreCase("PASSWORD_RETRY"))                  {return PASSWORD_RETRY;}
        else if(avpName.equalsIgnoreCase("PROMPT"))                          {return PROMPT;}
        else if(avpName.equalsIgnoreCase("CHAP_AUTH"))                       {return CHAP_AUTH;}
        else if(avpName.equalsIgnoreCase("CHAP_ALGORITHM"))                  {return CHAP_ALGORITHM;}
        else if(avpName.equalsIgnoreCase("CHAP_IDENT"))                      {return CHAP_IDENT;}
        else if(avpName.equalsIgnoreCase("CHAP_RESPONSE"))                   {return CHAP_RESPONSE;}
        else if(avpName.equalsIgnoreCase("CHAP_CHALLENGE"))                  {return CHAP_CHALLENGE;}
        else if(avpName.equalsIgnoreCase("ARAP_PASSWORD"))                   {return ARAP_PASSWORD;}
        else if(avpName.equalsIgnoreCase("ARAP_CHALLENGE_RESPONSE"))         {return ARAP_CHALLENGE_RESPONSE;}
        else if(avpName.equalsIgnoreCase("ARAP_SECURITY"))                   {return ARAP_SECURITY;}
        else if(avpName.equalsIgnoreCase("ARAP_SECURITY_DATA"))              {return ARAP_SECURITY_DATA;}
        else if(avpName.equalsIgnoreCase("SERVICE_TYPE"))                    {return SERVICE_TYPE;}
        else if(avpName.equalsIgnoreCase("CALLBACK_NUMBER"))                 {return CALLBACK_NUMBER;}
        else if(avpName.equalsIgnoreCase("CALLBACK_ID"))                     {return CALLBACK_ID;}
        else if(avpName.equalsIgnoreCase("IDLE_TIMEOUT"))                    {return IDLE_TIMEOUT;}
        else if(avpName.equalsIgnoreCase("PORT_LIMIT"))                      {return PORT_LIMIT;}
        else if(avpName.equalsIgnoreCase("NAS_FILTER_RULE"))                 {return NAS_FILTER_RULE;}
        else if(avpName.equalsIgnoreCase("FILTER_ID"))                       {return FILTER_ID;}
        else if(avpName.equalsIgnoreCase("CONFIGURATION_TOKEN"))             {return CONFIGURATION_TOKEN;}
        else if(avpName.equalsIgnoreCase("QOS_FILTER_RULE"))                 {return QOS_FILTER_RULE;}
        else if(avpName.equalsIgnoreCase("FRAMED_PROTOCOL"))                 {return FRAMED_PROTOCOL;}
        else if(avpName.equalsIgnoreCase("FRAMED_ROUTING"))                  {return FRAMED_ROUTING;}
        else if(avpName.equalsIgnoreCase("FRAMED_MTU"))                      {return FRAMED_MTU;}
        else if(avpName.equalsIgnoreCase("FRAMED_COMPRESSION"))              {return FRAMED_COMPRESSION;}
        else if(avpName.equalsIgnoreCase("FRAMED_IP_ADDRESS"))               {return FRAMED_IP_ADDRESS;}
        else if(avpName.equalsIgnoreCase("FRAMED_IP_NETMASK"))               {return FRAMED_IP_NETMASK;}
        else if(avpName.equalsIgnoreCase("FRAMED_ROUTE"))                    {return FRAMED_ROUTE;}
        else if(avpName.equalsIgnoreCase("FRAMED_POOL"))                     {return FRAMED_POOL;}
        else if(avpName.equalsIgnoreCase("FRAMED_INTERFACE_ID"))             {return FRAMED_INTERFACE_ID;}
        else if(avpName.equalsIgnoreCase("FRAMED_IPV6_PREFIX"))              {return FRAMED_IPV6_PREFIX;}
        else if(avpName.equalsIgnoreCase("FRAMED_IPV6_ROUTE"))               {return FRAMED_IPV6_ROUTE;}
        else if(avpName.equalsIgnoreCase("FRAMED_IPV6_POOL"))                {return FRAMED_IPV6_POOL;}
        else if(avpName.equalsIgnoreCase("FRAMED_IPX_NETWORK"))              {return FRAMED_IPX_NETWORK;}
        else if(avpName.equalsIgnoreCase("FRAMED_APPLETALK_LINK"))           {return FRAMED_APPLETALK_LINK;}
        else if(avpName.equalsIgnoreCase("FRAMED_APPLETALK_NETWORK"))        {return FRAMED_APPLETALK_NETWORK;}
        else if(avpName.equalsIgnoreCase("FRAMED_APPLETALK_ZONE"))           {return FRAMED_APPLETALK_ZONE;}
        else if(avpName.equalsIgnoreCase("ARAP_FEATURES"))                   {return ARAP_FEATURES;}
        else if(avpName.equalsIgnoreCase("ARAP_ZONE_ACCESS"))                {return ARAP_ZONE_ACCESS;}
        else if(avpName.equalsIgnoreCase("LOGIN_IP_HOST"))                   {return LOGIN_IP_HOST;}
        else if(avpName.equalsIgnoreCase("LOGIN_IPV6_HOST"))                 {return LOGIN_IPV6_HOST;}
        else if(avpName.equalsIgnoreCase("LOGIN_SERVICE"))                   {return LOGIN_SERVICE;}
        else if(avpName.equalsIgnoreCase("LOGIN_TCP_PORT"))                  {return LOGIN_TCP_PORT;}
        else if(avpName.equalsIgnoreCase("LOGIN_LAT_SERVICE"))               {return LOGIN_LAT_SERVICE;}
        else if(avpName.equalsIgnoreCase("LOGIN_LAT_NODE"))                  {return LOGIN_LAT_NODE;}
        else if(avpName.equalsIgnoreCase("LOGIN_LAT_GROUP"))                 {return LOGIN_LAT_GROUP;}
        else if(avpName.equalsIgnoreCase("LOGIN_LAT_PORT"))                  {return LOGIN_LAT_PORT;}
        else if(avpName.equalsIgnoreCase("TUNNELING"))                       {return TUNNELING;}
        else if(avpName.equalsIgnoreCase("TUNNEL_TYPE"))                     {return TUNNEL_TYPE;}
        else if(avpName.equalsIgnoreCase("TUNNEL_MEDIUM_TYPE"))              {return TUNNEL_MEDIUM_TYPE;}
        else if(avpName.equalsIgnoreCase("TUNNEL_CLIENT_ENDPOINT"))          {return TUNNEL_CLIENT_ENDPOINT;}
        else if(avpName.equalsIgnoreCase("TUNNEL_SERVER_ENDPOINT"))          {return TUNNEL_SERVER_ENDPOINT;}
        else if(avpName.equalsIgnoreCase("TUNNEL_PASSWORD"))                 {return TUNNEL_PASSWORD;}
        else if(avpName.equalsIgnoreCase("TUNNEL_PRIVATE_GROUP_ID"))         {return TUNNEL_PRIVATE_GROUP_ID;}
        else if(avpName.equalsIgnoreCase("TUNNEL_ASSIGNMENT_ID"))            {return TUNNEL_ASSIGNMENT_ID;}
        else if(avpName.equalsIgnoreCase("TUNNEL_PREFERENCE"))               {return TUNNEL_PREFERENCE;}
        else if(avpName.equalsIgnoreCase("TUNNEL_CLIENT_AUTH_ID"))           {return TUNNEL_CLIENT_AUTH_ID;}
        else if(avpName.equalsIgnoreCase("TUNNEL_SERVER_AUTH_ID"))           {return TUNNEL_SERVER_AUTH_ID;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_INPUT_OCTETS"))         {return ACCOUNTING_INPUT_OCTETS;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_OUTPUT_OCTETS"))        {return ACCOUNTING_OUTPUT_OCTETS;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_INPUT_PACKETS"))        {return ACCOUNTING_INPUT_PACKETS;}
        else if(avpName.equalsIgnoreCase("ACCOUNTING_OUTPUT_PACKETS"))       {return ACCOUNTING_OUTPUT_PACKETS;}
        else if(avpName.equalsIgnoreCase("ACCT_SESSION_TIME"))               {return ACCT_SESSION_TIME;}
        else if(avpName.equalsIgnoreCase("ACCT_AUTHENTIC"))                  {return ACCT_AUTHENTIC;}
        else if(avpName.equalsIgnoreCase("ACOUNTING_AUTH_METHOD"))           {return ACOUNTING_AUTH_METHOD;}
        else if(avpName.equalsIgnoreCase("ACCT_DELAY_TIME"))                 {return ACCT_DELAY_TIME;}
        else if(avpName.equalsIgnoreCase("ACCT_LINK_COUNT"))                 {return ACCT_LINK_COUNT;}
        else if(avpName.equalsIgnoreCase("ACCT_TUNNEL_CONNECTION"))          {return ACCT_TUNNEL_CONNECTION;}
        else if(avpName.equalsIgnoreCase("ACCT_TUNNEL_PACKETS_LOST"))        {return ACCT_TUNNEL_PACKETS_LOST;}
        else if(avpName.equalsIgnoreCase("NAS_IDENTIFIER"))                  {return NAS_IDENTIFIER;}
        else if(avpName.equalsIgnoreCase("NAS_IP_ADDRESS"))                  {return NAS_IP_ADDRESS;}
        else if(avpName.equalsIgnoreCase("NAS_IPV6_ADDRESS"))                {return NAS_IPV6_ADDRESS;}
        else if(avpName.equalsIgnoreCase("STATE"))                           {return STATE;}
        else if(avpName.equalsIgnoreCase("ORIGIN_AAA_PROTOCOL"))             {return ORIGIN_AAA_PROTOCOL;}
        // Diameter SIP Application
        else if(avpName.equalsIgnoreCase("SIP_ACCOUNTING_INFORMATION"))      {return SIP_ACCOUNTING_INFORMATION;}
        else if(avpName.equalsIgnoreCase("SIP_ACCOUNTING_SERVER_URI"))       {return SIP_ACCOUNTING_SERVER_URI;}
        else if(avpName.equalsIgnoreCase("SIP_CREDIT_CONTROL_SERVER_URI"))   {return SIP_CREDIT_CONTROL_SERVER_URI;}
        else if(avpName.equalsIgnoreCase("SIP_SERVER_URI"))                  {return SIP_SERVER_URI;}
        else if(avpName.equalsIgnoreCase("SIP_SERVER_CAPABILITIES"))         {return SIP_SERVER_CAPABILITIES;}
        else if(avpName.equalsIgnoreCase("SIP_MANDATORY_CAPABILITY"))        {return SIP_MANDATORY_CAPABILITY;}
        else if(avpName.equalsIgnoreCase("SIP_OPTIONAL_CAPABILITY"))         {return SIP_OPTIONAL_CAPABILITY;}
        else if(avpName.equalsIgnoreCase("SIP_SERVER_ASSIGNMENT_TYPE"))      {return SIP_SERVER_ASSIGNMENT_TYPE;}
        else if(avpName.equalsIgnoreCase("SIP_AUTH_DATA_ITEM"))              {return SIP_AUTH_DATA_ITEM;}
        else if(avpName.equalsIgnoreCase("SIP_AUTHENTICATION_SCHEME"))       {return SIP_AUTHENTICATION_SCHEME;}
        else if(avpName.equalsIgnoreCase("SIP_ITEM_NUMBER"))                 {return SIP_ITEM_NUMBER;}
        else if(avpName.equalsIgnoreCase("SIP_AUTHENTICATE"))                {return SIP_AUTHENTICATE;}
        else if(avpName.equalsIgnoreCase("SIP_AUTHORIZATION"))               {return SIP_AUTHORIZATION;}
        else if(avpName.equalsIgnoreCase("SIP_AUTHENTICATION_INFO"))         {return SIP_AUTHENTICATION_INFO;}
        else if(avpName.equalsIgnoreCase("SIP_NUMBER_AUTH_ITEMS"))           {return SIP_NUMBER_AUTH_ITEMS;}
        else if(avpName.equalsIgnoreCase("SIP_DEREGISTRATION_REASON"))       {return SIP_DEREGISTRATION_REASON;}
        else if(avpName.equalsIgnoreCase("SIP_REASON_CODE"))                 {return SIP_REASON_CODE;}
        else if(avpName.equalsIgnoreCase("SIP_REASON_INFO"))                 {return SIP_REASON_INFO;}
        else if(avpName.equalsIgnoreCase("SIP_VISITED_NETWORK_ID"))          {return SIP_VISITED_NETWORK_ID;}
        else if(avpName.equalsIgnoreCase("SIP_USER_AUTHORIZATION_TYPE"))     {return SIP_USER_AUTHORIZATION_TYPE;}
        else if(avpName.equalsIgnoreCase("SIP_SUPPORTED_USER_DATA_TYPE"))    {return SIP_SUPPORTED_USER_DATA_TYPE;}
        else if(avpName.equalsIgnoreCase("SIP_USER_DATA"))                   {return SIP_USER_DATA;}
        else if(avpName.equalsIgnoreCase("SIP_USER_DATA_TYPE"))              {return SIP_USER_DATA_TYPE;}
        else if(avpName.equalsIgnoreCase("SIP_USER_DATA_CONTENTS"))          {return SIP_USER_DATA_CONTENTS;}
        else if(avpName.equalsIgnoreCase("SIP_USER_DATA_ALREADY_AVAILABLE")) {return SIP_USER_DATA_ALREADY_AVAILABLE;}
        else if(avpName.equalsIgnoreCase("SIP_METHOD"))                      {return SIP_METHOD;}
        // Swisscom:
        else if(avpName.equalsIgnoreCase("SERVICE_INFORMATION"))             {return SERVICE_INFORMATION;}
        else if(avpName.equalsIgnoreCase("SMS_INFORMATION"))                 {return SMS_INFORMATION;}
        else if(avpName.equalsIgnoreCase("MMS_INFORMATION"))                 {return MMS_INFORMATION;}
        else if(avpName.equalsIgnoreCase("ORIGINATOR_ADDRESS"))              {return ORIGINATOR_ADDRESS;}
        else if(avpName.equalsIgnoreCase("ADDRESS_TYPE"))                    {return ADDRESS_TYPE;}
        else if(avpName.equalsIgnoreCase("ADDRESS_DATA"))                    {return ADDRESS_DATA;}
        else if(avpName.equalsIgnoreCase("ADDRESS_DOMAIN"))                  {return ADDRESS_DOMAIN;}
        else if(avpName.equalsIgnoreCase("DOMAIN_NAME"))                     {return DOMAIN_NAME;}
        else if(avpName.equalsIgnoreCase("MESSAGE_ID"))                      {return MESSAGE_ID;}
        else if(avpName.equalsIgnoreCase("MESSAGE_CLASS"))                   {return MESSAGE_CLASS;}
        else if(avpName.equalsIgnoreCase("CLASS_IDENTIFIER"))                {return CLASS_IDENTIFIER;}
        else if(avpName.equalsIgnoreCase("TOKEN_TEXT"))                      {return TOKEN_TEXT;}
        // XL
        else if(avpName.equalsIgnoreCase("REMAINING_SERVICE_MONEY"))         {return REMAINING_SERVICE_MONEY;}
        else if(avpName.equalsIgnoreCase("UMB_CHARGING_GROUP"))              {return UMB_CHARGING_GROUP;}
        else if(avpName.equalsIgnoreCase("VAS_TYPE"))                        {return VAS_TYPE;}
        else if(avpName.equalsIgnoreCase("MESSAGE_TYPE"))                    {return MESSAGE_TYPE;}
        else if(avpName.equalsIgnoreCase("SDC"))                             {return SDC;}
        // Gx specific AVPs
        else if(avpName.equalsIgnoreCase("BEARER_USAGE"))                    {return BEARER_USAGE;}
        else if(avpName.equalsIgnoreCase("CHARGING_RULE_INSTALL"))           {return CHARGING_RULE_INSTALL;}
        else if(avpName.equalsIgnoreCase("CHARGING_RULE_REMOVE"))            {return CHARGING_RULE_REMOVE;}
        else if(avpName.equalsIgnoreCase("CHARGING_RULE_DEFINITION"))        {return CHARGING_RULE_DEFINITION;}
        else if(avpName.equalsIgnoreCase("CHARGING_RULE_BASE_NAME"))         {return CHARGING_RULE_BASE_NAME;}
        else if(avpName.equalsIgnoreCase("CHARGING_RULE_NAME"))              {return CHARGING_RULE_NAME;}
        else if(avpName.equalsIgnoreCase("EVENT_TRIGGER"))                   {return EVENT_TRIGGER;}
        else if(avpName.equalsIgnoreCase("METERING_METHOD"))                 {return METERING_METHOD;}
        else if(avpName.equalsIgnoreCase("OFFLINE"))                         {return OFFLINE;}
        else if(avpName.equalsIgnoreCase("ONLINE"))                          {return ONLINE;}
        else if(avpName.equalsIgnoreCase("PRECEDENCE"))                      {return PRECEDENCE;}
        else if(avpName.equalsIgnoreCase("REPORTING_LEVEL"))                 {return REPORTING_LEVEL;}
        else if(avpName.equalsIgnoreCase("PDP_SESSION_OPERATION"))           {return PDP_SESSION_OPERATION;}
        else if(avpName.equalsIgnoreCase("TFT_FILTER"))                      {return TFT_FILTER;}
        else if(avpName.equalsIgnoreCase("TFT_PACKET_FILTER_INFORMATION"))   {return TFT_PACKET_FILTER_INFORMATION;}
        else if(avpName.equalsIgnoreCase("TOS_TRAFFIC_CLASS"))               {return TOS_TRAFFIC_CLASS;}
        else if(avpName.equalsIgnoreCase("SESSION_RELEASE_CAUSE"))           {return SESSION_RELEASE_CAUSE;}
        else return 0;
    }

    public static String getAVPName(int avpCode)
    {
        switch(avpCode)
        {
            // Diameter AVPs
            case ACCT_INTERIM_INTERVAL           : return "ACCT_INTERIM_INTERVAL";
            case ACCOUNTING_REALTIME_REQUIRED    : return "ACCOUNTING_REALTIME_REQUIRED";
            case ACCT_MULTI_SESSION_ID           : return "ACCT_MULTI_SESSION_ID";
            case ACCOUNTING_RECORD_NUMBER        : return "ACCOUNTING_RECORD_NUMBER";
            case ACCOUNTING_RECORD_TYPE          : return "ACCOUNTING_RECORD_TYPE";
            case ACCOUNTING_SESSION_ID           : return "ACCOUNTING_SESSION_ID";
            case ACCOUNTING_SUB_SESSION_ID       : return "ACCOUNTING_SUB_SESSION_ID";
            case ACCT_APPLICATION_ID             : return "ACCT_APPLICATION_ID";
            case AUTH_APPLICATION_ID             : return "AUTH_APPLICATION_ID";
            case AUTH_REQUEST_TYPE               : return "AUTH_REQUEST_TYPE";
            case AUTHORIZATION_LIFETIME          : return "AUTHORIZATION_LIFETIME";
            case AUTH_GRACE_PERIOD               : return "AUTH_GRACE_PERIOD";
            case AUTH_SESSION_STATE              : return "AUTH_SESSION_STATE";
            case RE_AUTH_REQUEST_TYPE            : return "RE_AUTH_REQUEST_TYPE";
            case CLASS                           : return "CLASS";
            case DESTINATION_HOST                : return "DESTINATION_HOST";
            case DESTINATION_REALM               : return "DESTINATION_REALM";
            case DISCONNECT_CAUSE                : return "DISCONNECT_CAUSE";
            case E2E_SEQUENCE                    : return "E2E_SEQUENCE";
            case ERROR_MESSAGE                   : return "ERROR_MESSAGE";
            case ERROR_REPORTING_HOST            : return "ERROR_REPORTING_HOST";
            case EVENT_TIMESTAMP                 : return "EVENT_TIMESTAMP";
            case EXPERIMENTAL_RESULT             : return "EXPERIMENTAL_RESULT";
            case EXPERIMENTAL_RESULT_CODE        : return "EXPERIMENTAL_RESULT_CODE";
            case FAILED_AVP                      : return "FAILED_AVP";
            case FIRMWARE_REVISION               : return "FIRMWARE_REVISION";
            case HOST_IP_ADDRESS                 : return "HOST_IP_ADDRESS";
            case INBAND_SECURITY_ID              : return "INBAND_SECURITY_ID";
            case MULTI_ROUND_TIME_OUT            : return "MULTI_ROUND_TIME_OUT";
            case ORIGIN_HOST                     : return "ORIGIN_HOST";
            case ORIGIN_REALM                    : return "ORIGIN_REALM";
            case ORIGIN_STATE_ID                 : return "ORIGIN_STATE_ID";
            case PRODUCT_NAME                    : return "PRODUCT_NAME";
            case PROXY_HOST                      : return "PROXY_HOST";
            case PROXY_INFO                      : return "PROXY_INFO";
            case PROXY_STATE                     : return "PROXY_STATE";
            case REDIRECT_HOST                   : return "REDIRECT_HOST";
            case REDIRECT_HOST_USAGE             : return "REDIRECT_HOST_USAGE";
            case REDIRECT_MAX_CACHE_TIME         : return "REDIRECT_MAX_CACHE_TIME";
            case RESULT_CODE                     : return "RESULT_CODE";
            case ROUTE_RECORD                    : return "ROUTE_RECORD";
            case SESSION_ID                      : return "SESSION_ID";
            case SESSION_TIMEOUT                 : return "SESSION_TIMEOUT";
            case SESSION_BINDING                 : return "SESSION_BINDING";
            case SESSION_SERVER_FAILOVER         : return "SESSION_SERVER_FAILOVER";
            case SUPPORTED_VENDOR_ID             : return "SUPPORTED_VENDOR_ID";
            case TERMINATION_CAUSE               : return "TERMINATION_CAUSE";
            case USER_NAME                       : return "USER_NAME";
            case VENDOR_ID                       : return "VENDOR_ID";
            case VENDOR_SPECIFIC_APPLICATION_ID  : return "VENDOR_SPECIFIC_APPLICATION_ID";
            //  Credit-Control AVPs
            case CC_CORRELATION_ID               : return "CC_CORRELATION_ID";
            case CC_INPUT_OCTETS                 : return "CC_INPUT_OCTETS";
            case CC_MONEY                        : return "CC_MONEY";
            case CC_OUTPUT_OCTETS                : return "CC_OUTPUT_OCTETS";
            case CC_REQUEST_NUMBER               : return "CC_REQUEST_NUMBER";
            case CC_REQUEST_TYPE                 : return "CC_REQUEST_TYPE";
            case CC_SERVICE_SPECIFIC_UNITS       : return "CC_SERVICE_SPECIFIC_UNITS";
            case CC_SESSION_FAILOVER             : return "CC_SESSION_FAILOVER";
            case CC_SUB_SESSION_ID               : return "CC_SUB_SESSION_ID";
            case CC_TIME                         : return "CC_TIME";
            case CC_TOTAL_OCTETS                 : return "CC_TOTAL_OCTETS";
            case CC_UNIT_TYPE                    : return "CC_UNIT_TYPE";
            case CHECK_BALANCE_RESULT            : return "CHECK_BALANCE_RESULT";
            case COST_INFORMATION                : return "COST_INFORMATION";
            case COST_UNIT                       : return "COST_UNIT";
            case CREDIT_CONTROL                  : return "CREDIT_CONTROL";
            case CREDIT_CONTROL_FAILURE_HANDLING : return "CREDIT_CONTROL_FAILURE_HANDLING";
            case CURRENCY_CODE                   : return "CURRENCY_CODE";
            case DIRECT_DEBITING_FAILURE_HANDLING: return "DIRECT_DEBITING_FAILURE_HANDLING";
            case EXPONENT                        : return "EXPONENT";
            case FINAL_UNIT_ACTION               : return "FINAL_UNIT_ACTION";
            case FINAL_UNIT_INDICATION           : return "FINAL_UNIT_INDICATION";
            case GRANTED_SERVICE_UNIT            : return "GRANTED_SERVICE_UNIT";
            case G_S_U_POOL_IDENTIFIER           : return "G_S_U_POOL_IDENTIFIER";
            case G_S_U_POOL_REFERENCE            : return "G_S_U_POOL_REFERENCE";
            case MULTIPLE_SERVICES_CREDIT_CONTROL: return "MULTIPLE_SERVICES_CREDIT_CONTROL";
            case MULTIPLE_SERVICES_INDICATOR     : return "MULTIPLE_SERVICES_INDICATOR";
            case RATING_GROUP                    : return "RATING_GROUP";
            case REDIRECT_ADDRESS_TYPE           : return "REDIRECT_ADDRESS_TYPE";
            case REDIRECT_SERVER                 : return "REDIRECT_SERVER";
            case REDIRECT_SERVER_ADDRESS         : return "REDIRECT_SERVER_ADDRESS";
            case REQUESTED_ACTION                : return "REQUESTED_ACTION";
            case REQUESTED_SERVICE_UNIT          : return "REQUESTED_SERVICE_UNIT";
            case RESTRICTION_FILTER_RULE         : return "RESTRICTION_FILTER_RULE";
            case SERVICE_CONTEXT_ID              : return "SERVICE_CONTEXT_ID";
            case SERVICE_IDENTIFIER              : return "SERVICE_IDENTIFIER";
            case SERVICE_PARAMETER_INFO          : return "SERVICE_PARAMETER_INFO";
            case SERVICE_PARAMETER_TYPE          : return "SERVICE_PARAMETER_TYPE";
            case SERVICE_PARAMETER_VALUE         : return "SERVICE_PARAMETER_VALUE";
            case SUBSCRIPTION_ID                 : return "SUBSCRIPTION_ID";
            case SUBSCRIPTION_ID_DATA            : return "SUBSCRIPTION_ID_DATA";
            case SUBSCRIPTION_ID_TYPE            : return "SUBSCRIPTION_ID_TYPE";
            case TARIFF_CHANGE_USAGE             : return "TARIFF_CHANGE_USAGE";
            case TARIFF_TIME_CHANGE              : return "TARIFF_TIME_CHANGE";
            case UNIT_VALUE                      : return "UNIT_VALUE";
            case USED_SERVICE_UNIT               : return "USED_SERVICE_UNIT";
            case USER_EQUIPMENT_INFO             : return "USER_EQUIPMENT_INFO";
            case USER_EQUIPMENT_INFO_TYPE        : return "USER_EQUIPMENT_INFO_TYPE";
            case USER_EQUIPMENT_INFO_VALUE       : return "USER_EQUIPMENT_INFO_VALUE";
            case VALUE_DIGITS                    : return "VALUE_DIGITS";
            case VALIDITY_TIME                   : return "VALIDITY_TIME";
            //  Diameter MIP
            case MIP_REG_REQUEST                 : return "MIP_REG_REQUEST";
            case MIP_REG_REPLY                   : return "MIP_REG_REPLY";
            case MIP_MN_AAA_AUTH                 : return "MIP_MN_AAA_AUTH";
            case MIP_MOBILE_NODE_ADDRESS         : return "MIP_MOBILE_NODE_ADDRESS";
            case MIP_HOME_AGENT_ADDRESS          : return "MIP_HOME_AGENT_ADDRESS";
            case MIP_CANDIDATE_HOME_AGENT_HOST   : return "MIP_CANDIDATE_HOME_AGENT_HOST";
            case MIP_FEATURE_VECTOR              : return "MIP_FEATURE_VECTOR";
            case MIP_AUTH_INPUT_DATA_LENGTH      : return "MIP_AUTH_INPUT_DATA_LENGTH";
            case MIP_AUTHENTICATOR_LENGTH        : return "MIP_AUTHENTICATOR_LENGTH";
            case MIP_AUTHENTICATOR_OFFSET        : return "MIP_AUTHENTICATOR_OFFSET";
            case MIP_MN_AAA_SPI                  : return "MIP_MN_AAA_SPI";
            case MIP_FILTER_RULE                 : return "MIP_FILTER_RULE";
            case MIP_FA_CHALLENGE                : return "MIP_FA_CHALLENGE";
            case MIP_ORIGINATING_FOREIGN_AAA     : return "MIP_ORIGINATING_FOREIGN_AAA";
            case MIP_HOME_AGENT_HOST             : return "MIP_HOME_AGENT_HOST";
            case MIP_FA_TO_HA_SPI                : return "MIP_FA_TO_HA_SPI";
            case MIP_FA_TO_MN_SPI                : return "MIP_FA_TO_MN_SPI";
            case MIP_HA_TO_FA_SPI                : return "MIP_HA_TO_FA_SPI";
            case MIP_MN_TO_FA_MSA                : return "MIP_MN_TO_FA_MSA";
            case MIP_FA_TO_MN_MSA                : return "MIP_FA_TO_MN_MSA";
            case MIP_FA_TO_HA_MSA                : return "MIP_FA_TO_HA_MSA";
            case MIP_HA_TO_FA_MSA                : return "MIP_HA_TO_FA_MSA";
            case MIP_MN_TO_HA_MSA                : return "MIP_MN_TO_HA_MSA";
            case MIP_HA_TO_MN_MSA                : return "MIP_HA_TO_MN_MSA";
            case MIP_NONCE                       : return "MIP_NONCE";
            case MIP_SESSION_KEY                 : return "MIP_SESSION_KEY";
            case MIP_ALGORITHM_                  : return "MIP_ALGORITHM_";
            case MIP_REPLAY_MODE                 : return "MIP_REPLAY_MODE";
            case MIP_MSA_LIFETIME                : return "MIP_MSA_LIFETIME";
            // Diameter Network Access Server Application
            case NAS_PORT                        : return "NAS_PORT";
            case NAS_PORT_ID                     : return "NAS_PORT_ID";
            case NAS_PORT_TYPE                   : return "NAS_PORT_TYPE";
            case CALLED_STATION_ID               : return "CALLED_STATION_ID";
            case CALLING_STATION_ID              : return "CALLING_STATION_ID";
            case CONNECT_INFO                    : return "CONNECT_INFO";
            case ORIGINATING_LINE_INFO           : return "ORIGINATING_LINE_INFO";
            case REPLY_MESSAGE                   : return "REPLY_MESSAGE";
            case USER_PASSWORD                   : return "USER_PASSWORD";
            case PASSWORD_RETRY                  : return "PASSWORD_RETRY";
            case PROMPT                          : return "PROMPT";
            case CHAP_AUTH                       : return "CHAP_AUTH";
            case CHAP_ALGORITHM                  : return "CHAP_ALGORITHM";
            case CHAP_IDENT                      : return "CHAP_IDENT";
            case CHAP_RESPONSE                   : return "CHAP_RESPONSE";
            case CHAP_CHALLENGE                  : return "CHAP_CHALLENGE";
            case ARAP_PASSWORD                   : return "ARAP_PASSWORD";
            case ARAP_CHALLENGE_RESPONSE         : return "ARAP_CHALLENGE_RESPONSE";
            case ARAP_SECURITY                   : return "ARAP_SECURITY";
            case ARAP_SECURITY_DATA              : return "ARAP_SECURITY_DATA";
            case SERVICE_TYPE                    : return "SERVICE_TYPE";
            case CALLBACK_NUMBER                 : return "CALLBACK_NUMBER";
            case CALLBACK_ID                     : return "CALLBACK_ID";
            case IDLE_TIMEOUT                    : return "IDLE_TIMEOUT";
            case PORT_LIMIT                      : return "PORT_LIMIT";
            case NAS_FILTER_RULE                 : return "NAS_FILTER_RULE";
            case FILTER_ID                       : return "FILTER_ID";
            case CONFIGURATION_TOKEN             : return "CONFIGURATION_TOKEN";
            case QOS_FILTER_RULE                 : return "QOS_FILTER_RULE";
            case FRAMED_PROTOCOL                 : return "FRAMED_PROTOCOL";
            case FRAMED_ROUTING                  : return "FRAMED_ROUTING";
            case FRAMED_MTU                      : return "FRAMED_MTU";
            case FRAMED_COMPRESSION              : return "FRAMED_COMPRESSION";
            case FRAMED_IP_ADDRESS               : return "FRAMED_IP_ADDRESS";
            case FRAMED_IP_NETMASK               : return "FRAMED_IP_NETMASK";
            case FRAMED_ROUTE                    : return "FRAMED_ROUTE";
            case FRAMED_POOL                     : return "FRAMED_POOL";
            case FRAMED_INTERFACE_ID             : return "FRAMED_INTERFACE_ID";
            case FRAMED_IPV6_PREFIX              : return "FRAMED_IPV6_PREFIX";
            case FRAMED_IPV6_ROUTE               : return "FRAMED_IPV6_ROUTE";
            case FRAMED_IPV6_POOL                : return "FRAMED_IPV6_POOL";
            case FRAMED_IPX_NETWORK              : return "FRAMED_IPX_NETWORK";
            case FRAMED_APPLETALK_LINK           : return "FRAMED_APPLETALK_LINK";
            case FRAMED_APPLETALK_NETWORK        : return "FRAMED_APPLETALK_NETWORK";
            case FRAMED_APPLETALK_ZONE           : return "FRAMED_APPLETALK_ZONE";
            case ARAP_FEATURES                   : return "ARAP_FEATURES";
            case ARAP_ZONE_ACCESS                : return "ARAP_ZONE_ACCESS";
            case LOGIN_IP_HOST                   : return "LOGIN_IP_HOST";
            case LOGIN_IPV6_HOST                 : return "LOGIN_IPV6_HOST";
            case LOGIN_SERVICE                   : return "LOGIN_SERVICE";
            case LOGIN_TCP_PORT                  : return "LOGIN_TCP_PORT";
            case LOGIN_LAT_SERVICE               : return "LOGIN_LAT_SERVICE";
            case LOGIN_LAT_NODE                  : return "LOGIN_LAT_NODE";
            case LOGIN_LAT_GROUP                 : return "LOGIN_LAT_GROUP";
            case LOGIN_LAT_PORT                  : return "LOGIN_LAT_PORT";
            case TUNNELING                       : return "TUNNELING";
            case TUNNEL_TYPE                     : return "TUNNEL_TYPE";
            case TUNNEL_MEDIUM_TYPE              : return "TUNNEL_MEDIUM_TYPE";
            case TUNNEL_CLIENT_ENDPOINT          : return "TUNNEL_CLIENT_ENDPOINT";
            case TUNNEL_SERVER_ENDPOINT          : return "TUNNEL_SERVER_ENDPOINT";
            case TUNNEL_PASSWORD                 : return "TUNNEL_PASSWORD";
            case TUNNEL_PRIVATE_GROUP_ID         : return "TUNNEL_PRIVATE_GROUP_ID";
            case TUNNEL_ASSIGNMENT_ID            : return "TUNNEL_ASSIGNMENT_ID";
            case TUNNEL_PREFERENCE               : return "TUNNEL_PREFERENCE";
            case TUNNEL_CLIENT_AUTH_ID           : return "TUNNEL_CLIENT_AUTH_ID";
            case TUNNEL_SERVER_AUTH_ID           : return "TUNNEL_SERVER_AUTH_ID";
            case ACCOUNTING_INPUT_OCTETS         : return "ACCOUNTING_INPUT_OCTETS";
            case ACCOUNTING_OUTPUT_OCTETS        : return "ACCOUNTING_OUTPUT_OCTETS";
            case ACCOUNTING_INPUT_PACKETS        : return "ACCOUNTING_INPUT_PACKETS";
            case ACCOUNTING_OUTPUT_PACKETS       : return "ACCOUNTING_OUTPUT_PACKETS";
            case ACCT_SESSION_TIME               : return "ACCT_SESSION_TIME";
            case ACCT_AUTHENTIC                  : return "ACCT_AUTHENTIC";
            case ACOUNTING_AUTH_METHOD           : return "ACOUNTING_AUTH_METHOD";
            case ACCT_DELAY_TIME                 : return "ACCT_DELAY_TIME";
            case ACCT_LINK_COUNT                 : return "ACCT_LINK_COUNT";
            case ACCT_TUNNEL_CONNECTION          : return "ACCT_TUNNEL_CONNECTION";
            case ACCT_TUNNEL_PACKETS_LOST        : return "ACCT_TUNNEL_PACKETS_LOST";
            case NAS_IDENTIFIER                  : return "NAS_IDENTIFIER";
            case NAS_IP_ADDRESS                  : return "NAS_IP_ADDRESS";
            case NAS_IPV6_ADDRESS                : return "NAS_IPV6_ADDRESS";
            case STATE                           : return "STATE";
            case ORIGIN_AAA_PROTOCOL             : return "ORIGIN_AAA_PROTOCOL";
            // Diameter SIP Application
            case SIP_ACCOUNTING_INFORMATION      : return "SIP_ACCOUNTING_INFORMATION";
            case SIP_ACCOUNTING_SERVER_URI       : return "SIP_ACCOUNTING_SERVER_URI";
            case SIP_CREDIT_CONTROL_SERVER_URI   : return "SIP_CREDIT_CONTROL_SERVER_URI";
            case SIP_SERVER_URI                  : return "SIP_SERVER_URI";
            case SIP_SERVER_CAPABILITIES         : return "SIP_SERVER_CAPABILITIES";
            case SIP_MANDATORY_CAPABILITY        : return "SIP_MANDATORY_CAPABILITY";
            case SIP_OPTIONAL_CAPABILITY         : return "SIP_OPTIONAL_CAPABILITY";
            case SIP_SERVER_ASSIGNMENT_TYPE      : return "SIP_SERVER_ASSIGNMENT_TYPE";
            case SIP_AUTH_DATA_ITEM              : return "SIP_AUTH_DATA_ITEM";
            case SIP_AUTHENTICATION_SCHEME       : return "SIP_AUTHENTICATION_SCHEME";
            case SIP_ITEM_NUMBER                 : return "SIP_ITEM_NUMBER";
            case SIP_AUTHENTICATE                : return "SIP_AUTHENTICATE";
            case SIP_AUTHORIZATION               : return "SIP_AUTHORIZATION";
            case SIP_AUTHENTICATION_INFO         : return "SIP_AUTHENTICATION_INFO";
            case SIP_NUMBER_AUTH_ITEMS           : return "SIP_NUMBER_AUTH_ITEMS";
            case SIP_DEREGISTRATION_REASON       : return "SIP_DEREGISTRATION_REASON";
            case SIP_REASON_CODE                 : return "SIP_REASON_CODE";
            case SIP_REASON_INFO                 : return "SIP_REASON_INFO";
            case SIP_VISITED_NETWORK_ID          : return "SIP_VISITED_NETWORK_ID";
            case SIP_USER_AUTHORIZATION_TYPE     : return "SIP_USER_AUTHORIZATION_TYPE";
            case SIP_SUPPORTED_USER_DATA_TYPE    : return "SIP_SUPPORTED_USER_DATA_TYPE";
            case SIP_USER_DATA                   : return "SIP_USER_DATA";
            case SIP_USER_DATA_TYPE              : return "SIP_USER_DATA_TYPE";
            case SIP_USER_DATA_CONTENTS          : return "SIP_USER_DATA_CONTENTS";
            case SIP_USER_DATA_ALREADY_AVAILABLE : return "SIP_USER_DATA_ALREADY_AVAILABLE";
            case SIP_METHOD                      : return "SIP_METHOD";
            // Swisscom:
            case SERVICE_INFORMATION             : return "SERVICE_INFORMATION";
            case SMS_INFORMATION                 : return "SMS_INFORMATION";
            case MMS_INFORMATION                 : return "MMS_INFORMATION";
            case ORIGINATOR_ADDRESS              : return "ORIGINATOR_ADDRESS";
            case ADDRESS_TYPE                    : return "ADDRESS_TYPE";
            case ADDRESS_DATA                    : return "ADDRESS_DATA";
            case ADDRESS_DOMAIN                  : return "ADDRESS_DOMAIN";
            case DOMAIN_NAME                     : return "DOMAIN_NAME";
            case MESSAGE_ID                      : return "MESSAGE_ID";
            case MESSAGE_CLASS                   : return "MESSAGE_CLASS";
            case CLASS_IDENTIFIER                : return "CLASS_IDENTIFIER";
            case TOKEN_TEXT                      : return "TOKEN_TEXT";
            // XL
            case REMAINING_SERVICE_MONEY         : return "REMAINING_SERVICE_MONEY";
            case UMB_CHARGING_GROUP              : return "UMB_CHARGING_GROUP";
            case VAS_TYPE                        : return "VAS_TYPE";
            case MESSAGE_TYPE                    : return "MESSAGE_TYPE";
            case SDC                             : return "SDC";
            // Gx specific AVPs
            case BEARER_USAGE                    : return "BEARER_USAGE";
            case CHARGING_RULE_INSTALL           : return "CHARGING_RULE_INSTALL";
            case CHARGING_RULE_REMOVE            : return "CHARGING_RULE_REMOVE";
            case CHARGING_RULE_DEFINITION        : return "CHARGING_RULE_DEFINITION";
            case CHARGING_RULE_BASE_NAME         : return "CHARGING_RULE_BASE_NAME";
            case CHARGING_RULE_NAME              : return "CHARGING_RULE_NAME";
            case EVENT_TRIGGER                   : return "EVENT_TRIGGER";
            case METERING_METHOD                 : return "METERING_METHOD";
            case OFFLINE                         : return "OFFLINE";
            case ONLINE                          : return "ONLINE";
            case PRECEDENCE                      : return "PRECEDENCE";
            case REPORTING_LEVEL                 : return "REPORTING_LEVEL";
            case PDP_SESSION_OPERATION           : return "PDP_SESSION_OPERATION";
            case TFT_FILTER                      : return "TFT_FILTER";
            case TFT_PACKET_FILTER_INFORMATION   : return "TFT_PACKET_FILTER_INFORMATION";
            case TOS_TRAFFIC_CLASS               : return "TOS_TRAFFIC_CLASS";
            case SESSION_RELEASE_CAUSE           : return "SESSION_RELEASE_CAUSE";
            default: return "Unknown("+avpCode+")";
        }
    }
    public static int getCommandCode(String commandName)
    {
        try
        {
            return Integer.decode(commandName);
        } catch(Exception ex)
        {}
        // Diameter Base
             if(commandName.equalsIgnoreCase("ASR")){return ASR;}//  8.5.1 Abort-Session-Request
        else if(commandName.equalsIgnoreCase("ASA")){return ASA;}//  8.5.2 Abort-Session-Answer
        else if(commandName.equalsIgnoreCase("ACR")){return ACR;}//  9.7.1 Accounting-Request
        else if(commandName.equalsIgnoreCase("ACA")){return ACA;}//  9.7.2 Accounting-Answer
        else if(commandName.equalsIgnoreCase("CER")){return CER;}//  5.3.1 Capabilities-Exchange-Request
        else if(commandName.equalsIgnoreCase("CEA")){return CEA;}//  5.3.2 Capabilities-Exchange-Answer
        else if(commandName.equalsIgnoreCase("DWR")){return DWR;}//  5.5.1 Device-Watchdog-Request
        else if(commandName.equalsIgnoreCase("DWA")){return DWA;}//  5.5.2 Device-Watchdog-Answer
        else if(commandName.equalsIgnoreCase("DPR")){return DPR;}//  5.4.1 Disconnect-Peer-Request
        else if(commandName.equalsIgnoreCase("DPA")){return DPA;}//  5.4.2 Disconnect-Peer-Answer
        else if(commandName.equalsIgnoreCase("RAR")){return RAR;}//  8.3.1 Re-Auth-Request
        else if(commandName.equalsIgnoreCase("RAA")){return RAA;}//  8.3.2 Re-Auth-Answer
        else if(commandName.equalsIgnoreCase("STR")){return STR;}//  8.4.1 Session-Termination-Request
        else if(commandName.equalsIgnoreCase("STA")){return STA;}//  8.4.2 Session-Termination-Answer
        //  Credit Control
        else if(commandName.equalsIgnoreCase("CCR")){return CCR;}//  3.1 Credit-Control-Request
        else if(commandName.equalsIgnoreCase("CCA")){return CCA;}//  3.2 Credit-Control-Answer
        //  Diameter MIP
        else if(commandName.equalsIgnoreCase("AMR")){return AMR;}//  5.1 AA-Mobile-Node-Request
        else if(commandName.equalsIgnoreCase("AMA")){return AMA;}//  5.2 AA-Mobile-Node-Answer
        else if(commandName.equalsIgnoreCase("HAR")){return HAR;}//  5.3 Home-Agent-MIP-Request
        else if(commandName.equalsIgnoreCase("HAA")){return HAA;}//  5.4 Home-Agent-MIP-Answer
        // Diameter Network Access Server Application
        else if(commandName.equalsIgnoreCase("AAR")){return AAR;}//  3.1  AA-Request
        else if(commandName.equalsIgnoreCase("AAA")){return AAA;}//  3.2  AA-Answer
        // Diameter EAP Application
        else if(commandName.equalsIgnoreCase("DER")){return DER;}//  3.1  Diameter-EAP-Request
        else if(commandName.equalsIgnoreCase("DEA")){return DEA;}//  3.2  Diameter-EAP-Answer
        // Diameter SIP Application
        else if(commandName.equalsIgnoreCase("UAR")){return UAR;}//  8.1  User-Authorization-Request
        else if(commandName.equalsIgnoreCase("UAA")){return UAA;}//  8.2  User-Authorization-Answer
        else if(commandName.equalsIgnoreCase("SAR")){return SAR;}//  8.3  Server-Assignment-Request
        else if(commandName.equalsIgnoreCase("SAA")){return SAA;}//  8.4  Server-Assignment-Answer
        else if(commandName.equalsIgnoreCase("LIR")){return LIR;}//  8.5  Location-Info-Request
        else if(commandName.equalsIgnoreCase("LIA")){return LIA;}//  8.6  Location-Info-Answer
        else if(commandName.equalsIgnoreCase("MAR")){return MAR;}//  8.7  Multimedia-Auth-Request
        else if(commandName.equalsIgnoreCase("MAA")){return MAA;}//  8.8  Multimedia-Auth-Answer
        else if(commandName.equalsIgnoreCase("RTR")){return RTR;}//  8.9  Registration-Termination-Request
        else if(commandName.equalsIgnoreCase("RTA")){return RTA;}//  8.10 Registration-Termination-Answer
        else if(commandName.equalsIgnoreCase("PPR")){return PPR;}//  8.11 Push-Profile-Request
        else if(commandName.equalsIgnoreCase("PPA")){return PPA;}//  8.12 Push-Profile-Answer
        // Diameter Base
        else if(commandName.equalsIgnoreCase("ABORT_SESSION_REQUEST"))        {return ASR;}//  8.5.1 Abort-Session-Request
        else if(commandName.equalsIgnoreCase("ABORT_SESSION_ANSWER"))         {return ASA;}//  8.5.2 Abort-Session-Answer
        else if(commandName.equalsIgnoreCase("ACCOUNTING_REQUEST"))           {return ACR;}//  9.7.1 Accounting-Request
        else if(commandName.equalsIgnoreCase("ACCOUNTING_ANSWER"))            {return ACA;}//  9.7.2 Accounting-Answer
        else if(commandName.equalsIgnoreCase("CAPABILITIES_EXCHANGE_REQUEST")){return CER;}//  5.3.1 Capabilities-Exchange-Request
        else if(commandName.equalsIgnoreCase("CAPABILITIES_EXCHANGE_ANSWER")) {return CEA;}//  5.3.2 Capabilities-Exchange-Answer
        else if(commandName.equalsIgnoreCase("DEVICE_WATCHDOG_REQUEST"))      {return DWR;}//  5.5.1 Device-Watchdog-Request
        else if(commandName.equalsIgnoreCase("DEVICE_WATCHDOG_ANSWER"))       {return DWA;}//  5.5.2 Device-Watchdog-Answer
        else if(commandName.equalsIgnoreCase("DISCONNECT_PEER_REQUEST"))      {return DPR;}//  5.4.1 Disconnect-Peer-Request
        else if(commandName.equalsIgnoreCase("DISCONNECT_PEER_ANSWER"))       {return DPA;}//  5.4.2 Disconnect-Peer-Answer
        else if(commandName.equalsIgnoreCase("RE_AUTH_REQUEST"))              {return RAR;}//  8.3.1 Re-Auth-Request
        else if(commandName.equalsIgnoreCase("RE_AUTH_ANSWER"))               {return RAA;}//  8.3.2 Re-Auth-Answer
        else if(commandName.equalsIgnoreCase("SESSION_TERMINATION_REQUEST"))  {return STR;}//  8.4.1 Session-Termination-Request
        else if(commandName.equalsIgnoreCase("SESSION_TERMINATION_ANSWER"))   {return STA;}//  8.4.2 Session-Termination-Answer
        //  Credit Control
        else if(commandName.equalsIgnoreCase("CREDIT_CONTROL_REQUEST"))       {return CCR;}//  3.1   Credit-Control-Request
        else if(commandName.equalsIgnoreCase("CREDIT_CONTROL_ANSWER"))        {return CCA;}//  3.2   Credit-Control-Answer
        //  Diameter MIP
        else if(commandName.equalsIgnoreCase("AA_MOBILE_NODE_REQUEST"))          {return AMR;}//5.1 AA-Mobile-Node-Request
        else if(commandName.equalsIgnoreCase("AA_MOBILE_NODE_ANSWER"))           {return AMA;}//5.2 AA-Mobile-Node-Answer
        else if(commandName.equalsIgnoreCase("HOME_AGENT_MIP_REQUEST"))          {return HAR;}//5.3 Home-Agent-MIP-Request
        else if(commandName.equalsIgnoreCase("HOME_AGENT_MIP_ANSWER"))           {return HAA;}//5.4 Home-Agent-MIP-Answer
        //  Diameter MIP
        else if(commandName.equalsIgnoreCase("AA_REQUEST"))                      {return AAR;}//3.1  AA-Request
        else if(commandName.equalsIgnoreCase("AA_ANSWER"))                       {return AAA;}//3.2  AA-Answer
        // Diameter Network Access Server Application
        else if(commandName.equalsIgnoreCase("DIAMETER_EAP_REQUEST"))            {return DER;}//3.1  Diameter-EAP-Request
        else if(commandName.equalsIgnoreCase("DIAMETER_EAP_ANSWER"))             {return DEA;}//3.2  Diameter-EAP-Answer
        // Diameter SIP Application
        else if(commandName.equalsIgnoreCase("USER_AUTHORIZATION_REQUEST"))      {return UAR;}//8.1  User-Authorization-Request
        else if(commandName.equalsIgnoreCase("USER_AUTHORIZATION_ANSWER"))       {return UAA;}//8.2  User-Authorization-Answer
        else if(commandName.equalsIgnoreCase("SERVER_ASSIGNMENT_REQUEST"))       {return SAR;}//8.3  Server-Assignment-Request
        else if(commandName.equalsIgnoreCase("SERVER_ASSIGNMENT_ANSWER"))        {return SAA;}//8.4  Server-Assignment-Answer
        else if(commandName.equalsIgnoreCase("LOCATION_INFO_REQUEST"))           {return LIR;}//8.5  Location-Info-Request
        else if(commandName.equalsIgnoreCase("LOCATION_INFO_ANSWER"))            {return LIA;}//8.6  Location-Info-Answer
        else if(commandName.equalsIgnoreCase("MULTIMEDIA_AUTH_REQUEST"))         {return MAR;}//8.7  Multimedia-Auth-Request
        else if(commandName.equalsIgnoreCase("MULTIMEDIA_AUTH_ANSWER"))          {return MAA;}//8.8  Multimedia-Auth-Answer
        else if(commandName.equalsIgnoreCase("REGISTRATION_TERMINATION_REQUEST")){return RTR;}//8.9  Registration-Termination-Request
        else if(commandName.equalsIgnoreCase("REGISTRATION_TERMINATION_ANSWER")) {return RTA;}//8.10 Registration-Termination-Answer
        else if(commandName.equalsIgnoreCase("PUSH_PROFILE_REQUEST"))            {return PPR;}//8.11 Push-Profile-Request
        else if(commandName.equalsIgnoreCase("PUSH_PROFILE_ANSWER"))             {return PPA;}//8.12 Push-Profile-Answer
        else return 0;
    }

    public static String getCommandName(int commandCode, int type)
    {
        String typeS="A";
        if((type&CFLAG_REQUEST)==CFLAG_REQUEST){typeS="R";}

        switch(commandCode)
        {
            case ASR: return "AS"+typeS;
            case ACR: return "AC"+typeS;
            case CER: return "CE"+typeS;
            case DWR: return "DW"+typeS;
            case DPR: return "DP"+typeS;
            case RAR: return "RA"+typeS;
            case STR: return "ST"+typeS;
            case CCR: return "CC"+typeS;
            case AMR: return "AM"+typeS;
            case HAR: return "HA"+typeS;
            case AAR: return "AA"+typeS;
            case DER: return "DE"+typeS;
            case UAR: return "UA"+typeS;
            case SAR: return "SA"+typeS;
            case LIR: return "LI"+typeS;
            case MAR: return "MA"+typeS;
            case RTR: return "RT"+typeS;
            case PPR: return "PP"+typeS;
            default:  return "???";
        }
    }
    public static String getCommandFullName(int commandCode, int type)
    {
        String typeS="_ANSWER";
        if((type&CFLAG_REQUEST)==CFLAG_REQUEST){typeS="_REQUEST";}
        switch(commandCode)
        {
            case ASR: return "ABORT_SESSION"+typeS;
            case ACR: return "ACCOUNTING"+typeS;
            case CER: return "CAPABILITIES_EXCHANGE"+typeS;
            case DWR: return "DEVICE_WATCHDOG"+typeS;
            case DPR: return "DISCONNECT_PEER"+typeS;
            case RAR: return "RE_AUTH"+typeS;
            case STR: return "SESSION_TERMINATION"+typeS;
            case CCR: return "CREDIT_CONTROL"+typeS;
            case AMR: return "AA_MOBILE_NODE"+typeS;
            case HAR: return "HOME_AGENT_MIP"+typeS;
            case AAR: return "AA"+typeS;
            case DER: return "DIAMETER_EAP"+typeS;
            case UAR: return "USER_AUTHORIZATION"+typeS;
            case SAR: return "SERVER_ASSIGNMENT"+typeS;
            case LIR: return "LOCATION_INFO"+typeS;
            case MAR: return "MULTIMEDIA_AUTH"+typeS;
            case RTR: return "REGISTRATION_TERMINATION"+typeS;
            case PPR: return "PUSH_PROFILE"+typeS;
            default:  return "Unknown";
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: DiameterWorkerServer.java,v 1.21 2011/04/18 12:17:18 rajesh Exp $";
    
    public class SendMyResp extends Thread
    {
	public DiameterWorkerServer dws=null;
	public Diameter_PDUHeader pdu_header=null;
	public Diameter_PDUBody pdu_body=null;
	public BufferedOutputStream out=null;
	
    	public SendMyResp() throws Exception
    	{
    		super("SendMyResp");
    	}
    	
    	public void run()
        {
    		try {
				handleClient();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	
        }
    }
}

