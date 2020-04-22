package com.mobixell.xtt.diameter.client;

import java.io.File;
import java.io.IOException;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
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
import com.mobixell.xtt.diameter.load.UsersDataTable;
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
import com.mobixell.xtt.diameter.statistics.CmdDataItem;
import com.mobixell.xtt.util.StringUtils;
/**
 * 
 * <p>DiameterWorkerPushClient</p>
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
 * @author Guy Bitan
 * @version $Id: DiameterWorkerPushClient.java,v 1.21 2011/04/18 12:17:18 guybitan Exp $
 *  <p>Handle the RAR/T/U request</P> 
 */
public class DiameterWorkerPushClient extends Thread implements DiameterConstants
{
    private boolean stop = false;
    private int id;
    int sec = 1000;
    private int chargingRuleNameCount = 0;
    private boolean isChargingRuleRemove = false;
    //private static final int OUTPUTLENGTH=40;

    /* Socket to client we're handling, which will be set by the DiameterServer
       when dispatching the request to us */
    private DiameterClient myDiamClient=null;
    public File myRoot=null;
    private String myDiamClientPort="0";
    public static String myDiamHostName=null;
    private static int instances=0;
    private Socket socket;
    private SocketBucket socketBucketPoll;
    public static int requestcount=0;
    private static Object key = new Object();
    private static Set<String> extendedStoreVar = Collections.synchronizedSet(new HashSet<String>());
    String pcef_IP = null;
   	String pcrf_Port =  null;
   	String pcef_Port =  null;
   	String pcrf_IP =    null;
    private boolean isOrgRoot = true;
    /**
     * Creates a new DiameterWorkerPushClient
     * @param id     ID number of this worker thread
     * @throws Exception 
     */
    public DiameterWorkerPushClient(int id,DiameterClient client) throws Exception
    {
        super("DiameterWorkerPushClient("+id+")");
        this.myDiamClient=client;
        this.id = id;
        myDiamClient.addPushWorker();
    }
	public int getWorkerId()
    {
        return id;
    }

	public void run()
	{
		synchronized (key)
		{
			instances++;
			XTTProperties.printDebug("DiameterWorkerPushClient New Push Client handled instance " + instances);
			key.notify();
		}
		while (true)
		{
			if (stop)
			{
				return;
			}
			try
			{
				UserDataItem pollUser = UsersDataTable.queue.poll(20 * sec, TimeUnit.MILLISECONDS);

				if (pollUser != null)
				{
						getSocket();
					
					if (socket.isConnected())
					{
						if (pollUser.getPushToPerfom() == pollUser.PUSH_RAR_T)
						{
							sendDiameterPushRequest(pollUser.getsSessionId(), "RE_AUTH_T_REQUEST", "", "");
							XTTProperties.printDebug("DiameterWorkerPushClient(" + socket.getLocalPort() + "/"+ instances + "): (Poll) RAR-T for user: " + pollUser.getsSessionId());
						}
						if (pollUser.getPushToPerfom() == pollUser.PUSH_RAR_U)
						{
							sendDiameterPushRequest(pollUser.getsSessionId(), "RE_AUTH_U_REQUEST",
									"RE_AUTH_REQUEST_TYPE", "AUTHORIZE_AUTHENTICATE");
							XTTProperties.printDebug("DiameterWorkerPushClient(" + socket.getLocalPort() + "/"
									+ instances + "): (Poll) RAR-U for user: " + pollUser.getsSessionId());
						}
						if (DiameterManager.customer.equalsIgnoreCase("OPL"))
						DiameterClient.socketWheel.addSocket(socket);
					}
					else
					{
						// If the push user didn't get socket need to add it to
						// the Q
						DiameterManager.usersTab.addUser(pollUser.getsSessionId(), pollUser.getIpAddress());
					}
				}
			}
			catch (Exception e)
			{
				XTTProperties.printDebug("DiameterWorkerPushClient closeConnection: " + socket.getLocalPort());
				try
				{
					// csvTarnlog.closeFile();
					if (this.socket != null)
					{
						this.socket.close();
					}
				}
				catch (IOException e1)
				{
					XTTProperties.printDebug("Could't open tcp (Push) Session to the PCEF !!!)");
					e1.printStackTrace();
				}

				synchronized (key)
				{
					instances--;
					key.notify();
				}
			}
		}
	}
   public synchronized void getSocket() throws IOException, InterruptedException
   {
		if (DiameterManager.customer.equalsIgnoreCase("OPL"))
		{
			socketBucketPoll = DiameterClient.socketWheel.getPollBucket();
			if (socketBucketPoll != null)
			{
				socket = socketBucketPoll.getSocket();
			}
		}
		else if (DiameterManager.customer.equalsIgnoreCase("vimpelcom"))
		{
			socket=DiameterServer.pcefSocket;
		}
   }
  //This function create empty pdu_header with relevant code and add to it the avps from the xml and send it
    public synchronized boolean sendDiameterPushRequest(String sessionId, String avpName,String matchAVPIn,String matchREGEXIn) throws Exception
    {
    	if (DiameterManager.usersTab.getUser(sessionId) == null)
 		{	
    		XTTProperties.printDebug(avpName + " (RE_AUTH_REQUEST) For SessionId: " + sessionId + " isn't the internal Users DB (user deleted/not exist!)");
    		return false;
 		}
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
        
        Diameter_PDUHeader pdu_header=new Diameter_PDUHeader("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + ")");
        Diameter_PDUBody pdu_body=new Diameter_PDUBody("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + ")",extendedStoreVar);
        //Hard Coded
        
       	pdu_header.readPDUHeader(258);
        
        StringBuffer output=new StringBuffer("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE: Header/Body: ");
        Diameter_PDUBody response_body = new Diameter_PDUBody("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
        
		        if (matchAVPIn != null && matchREGEXIn!=null)
		        {
		        	XTTProperties.printDebug("Sending: " +  avpName + "," + matchAVPIn + " "+ matchREGEXIn + " for SessionId-" + sessionId);
		        	pdu_body.createEmptyPDUBody(avpName,matchAVPIn,matchREGEXIn,sessionId);
		        }
		        else 
		        {	
		        	XTTProperties.printDebug("Sending " +  avpName + " for SessionId-" + sessionId);
		        	pdu_body.createEmptyPDUBody(avpName,null,null,sessionId);
		        }
		        //Set the req to res
		        pdu_header.setRequest(true);
		        
		        //Get the all relevant block from xml file 
		        Vector<Element> responseElemets = myDiamClient.getResponseElements(pdu_header.getCommandcode());
		
		        if(responseElemets==null)
		        {
		            XTTProperties.printFail("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response defined for " + pdu_header.getCommandname());
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
		                    avps=pdu_body.getAVPMap().get(DiameterWorkerServer.getAVPCode(tempAVP));
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
		            XTTProperties.printFail("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response matched for "+pdu_header.getCommandname());
		            XTTProperties.setTestStatus(XTTProperties.FAILED);
		            return false;
		        }
		        pdu_header.setProxiable(ConvertLib.textToBoolean(getElementAttribute(currentElement,"proxiable")));
		        pdu_header.setError(ConvertLib.textToBoolean(getElementAttribute(currentElement,"error")));
		        pdu_header.setTretr(ConvertLib.textToBoolean(getElementAttribute(currentElement,"tretr")));
		           //Send RAR-T to all users in the DB    
		        	isOrgRoot = true;	
			        response_body.setAVPs(createAVPsFromXml(currentElement,pdu_body.getAVPMap(),sessionId,CmdDataItem.Type.REQUEST,matchREGEXIn));
			        //System.out.println("RAVPS: "+response_body.getAVPs());
			        // Convert the defined response AVPs to binary ---> Here the convert pdu to bin
			        byte[] body=response_body.createPDUBody();
			        //System.out.println("RBODY: \n"+ConvertLib.getHexView(body)); 
			        pdu_header.setMessagelength(pdu_header.HEADLENGTH+body.length);
			        // Create the binary header---> Here the convert header to bin
			        byte[] header=pdu_header.createPDUHeader();			       
							
			        synchronized (socket)
					{	
								BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 65536);
								// Write the response
								out.write(header);
								out.write(body);
								out.flush();
								socket.notifyAll();			
					}			
			        XTTProperties.printTransaction("DiameterWorkerPushClient/RESPONSE"+XTTProperties.DELIMITER+myDiamClientPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
			            + pdu_header.getCommandname()+XTTProperties.DELIMITER
			            + pdu_header.getApplicationID()+XTTProperties.DELIMITER
			            + pdu_header.getHopbyhopID()+XTTProperties.DELIMITER
			            + pdu_header.getEndtoendID()+XTTProperties.DELIMITER
			            + matchAVP+XTTProperties.DELIMITER
			            + matchREGEX);
			        
			        output.append("\n"+ ConvertLib.getHexView(header));
			        output.append(""+ ConvertLib.getHexView(body));
			        XTTProperties.printDebug(output.toString());
        return true;
    }
    /*
     * Create the child AVPs from a Response XML root node
     * This function is recursion function.
     */
    private Vector<Diameter_AVP> createAVPsFromXml (Element root,HashMap<Integer, Vector<Diameter_AVP>> avpMap,String sSessionId,CmdDataItem.Type type, String matchREGEX) throws Exception
    {
    	   String cmdName="";
    	   CmdDataItem.Type cmdType = type; 
    	pcef_IP =    myDiamClient.getRemoteIP();
       	pcrf_Port =  socket.getLocalPort()+"";
       	pcef_Port =  socket.getPort()+"";
       	pcrf_IP =    myDiamClient.getLocalIP();
       	
    	 Vector<Diameter_AVP> returnValues=new Vector<Diameter_AVP>();
    	 if(root==null) return returnValues;
    	 //if is first call since this is a recursion function
		if (isOrgRoot)
		{
			CmdDataItem.totalUsers = DiameterManager.usersTab.users.size();
			
			cmdName = root.getName();
			if (cmdType == CmdDataItem.Type.REQUEST)
			{
				if (cmdName == "RE_AUTH_REQUEST")
				{
					if (matchREGEX.equalsIgnoreCase("AUTHORIZE_AUTHENTICATE"))
					{
						if (DiameterManager.usersTab.getUser(sSessionId) != null)
						{
						CmdDataItem.totalRARUReq++;
						if (DiameterManager.createtranslog==true)
						DiameterManager.cmdTab.addCmd("CP",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port, sSessionId, "RARU",cmdType, "UPDATE", DiameterManager.usersTab.getUser(sSessionId));
						}
						else
						{
							CmdDataItem.totalRARUReq++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("CP",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port,sSessionId, "RARU", cmdType, "UPDATE", null);
						}
					}
					else
					{
						if (DiameterManager.usersTab.getUser(sSessionId) != null)
						{
							CmdDataItem.totalRARTReq++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("CP",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port, sSessionId, "RART",cmdType, "TERMINATION", DiameterManager.usersTab.getUser(sSessionId));
						}
						else
						{
							CmdDataItem.totalRARTReq++;
							if (DiameterManager.createtranslog==true)
							DiameterManager.cmdTab.addCmd("CP",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port,sSessionId, "RART", cmdType, "TERMINATION", null);
						}
					}
				}
			}
			if (cmdType == CmdDataItem.Type.RESPONSE)
			{
				if (cmdName.equalsIgnoreCase("DEVICE_WATCHDOG_REQUEST"))
				{
					CmdDataItem.totalDWDRes++;
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("CP",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "DWD", cmdType, "DWD", null);
				}
				else if (cmdName.equalsIgnoreCase("CAPABILITIES_EXCHANGE_REQUEST"))
				{
					CmdDataItem.totalCERRes++;
					if (DiameterManager.createtranslog==true)
					DiameterManager.cmdTab.addCmd("CP",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CER", cmdType, "DWD", null);
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
            avpcode=DiameterWorkerServer.getAVPCode(currentE.getName());
            if(avpcode<=0)
            {
                XTTProperties.printFail("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId()+ "): AVP not found " + currentE.getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception("AVP not found "+currentE.getName());
            }

            tempReqAttr=getElementAttribute(currentE,"request");
            if(tempReqAttr!=null&&!tempReqAttr.equals(""))
            {
                // We have to get response AVPs directly from the REQUEST
                try
                {
                	//Get From the Req the AVP
                	currentAVPs=avpMap.get(DiameterWorkerServer.getAVPCode(tempReqAttr));
                	
                    if(currentAVPs==null||currentAVPs.size()<=0)
                    {
                        XTTProperties.printFail("DiameterWorkerPushClient(" + myDiamClientPort + "/" + getWorkerId() + "): AVP not found in request " + tempReqAttr);
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
		                    			if (avpcode==SUBSCRIPTION_ID_DATA)
		                    			{
		                    				currentAVP.data  =  DiameterManager.usersTab.getMsisdn(sSessionId);
		                    			}
		                    			else if (avpcode==CHARGING_RULE_REMOVE)
		                    			{
		                    				isChargingRuleRemove =true;
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
                currentAVP.setGroupedAVPs(createAVPsFromXml(currentE,avpMap,sSessionId,type,matchREGEX));
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
    public static final String tantau_sccsid = "@(#)$Id: DiameterWorkerPushClient.java,v 1.21 2011/04/18 12:17:18 rajesh Exp $";
}