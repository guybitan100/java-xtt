package com.mobixell.xtt.diameter.client;

import java.io.File;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.BufferedOutputStream;
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
import com.mobixell.xtt.diameter.server.DiameterServer;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
import com.mobixell.xtt.diameter.statistics.CmdDataItem;
import com.mobixell.xtt.util.StringUtils;
/**
 * <p>DiameterWorkerClient</p>
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
 * <p>Handle the RAR/T/U response</P>
 * @author Guy Bitan
 * @version $Id: DiameterWorkerClient.java,v 1.21 2011/04/18 12:17:18 guybitan Exp $
 */
public class DiameterWorkerClient extends Thread implements DiameterConstants
{
    private boolean stop = false;
    private int id;
    int sec = 1000;
    private static boolean extendedOutput=false;
    private int chargingRuleNameCount = 0;
    private boolean isChargingRuleRemove = false;
    //private static final int OUTPUTLENGTH=40;

    /* Socket to client we're handling, which will be set by the DiameterServer
       when dispatching the request to us */
    public Socket socket = null;
    private DiameterClient myDiamClient=null;
    public File myRoot=null;
    private String myDiamClientPort="0";
    public static String myDiamHostName=null;
    private static int instances=0;

    private static Object requestkey=new Object();
    public static int requestcount=0;
    private static Object key = new Object();
    private static Set<String> extendedStoreVar = Collections.synchronizedSet(new HashSet<String>());
    private static boolean keep_alive=true;
    private static int perworkerdelay=0;
    private boolean isOrgRoot = true;
    String pcef_IP =  null;
	String pcrf_Port =  null;
	String pcef_Port =  null;
	String pcrf_IP =   null;
    /**
     * Creates a new DiameterWorkerClient
     * @param id     ID number of this worker thread
     * @throws Exception 
     */
    public DiameterWorkerClient(int id,Socket socket, DiameterClient client) throws Exception
    {
        super("DiameterWorkerClient("+id+")");
        this.myDiamClient=client;
        this.socket = socket;
        this.id = id;
       
		// this.responseDocument=responseDocument;
		myDiamHostName = socket.getLocalAddress().getCanonicalHostName();
		if (myDiamClient == null)
		{
			myDiamClientPort = "";
		}
		else
		{
			myDiamClient.addWorker();
			myDiamClientPort = socket.getLocalPort() + "";
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
        XTTProperties.printDebug("DiameterWorkerClient(" + myDiamClientPort+ "/" + getWorkerId() + "): setting stop");
        try
        {
            this.socket.close();
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
        	if (DiameterManager.usersTab.getUsersCount()>=1)
        		handleServer();
        }  
        catch (java.net.SocketException soe)
        {
            if(stop)
            {
                return;
            } 
            else
            {
                XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort+"/" + getWorkerId() + "): SocketException in run");
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
                XTTProperties.printDebug("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): SocketTimeoutException in run - Keep-Alive disconnected");
                return;
            } else
            {
                XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId()+ "): SocketTimeoutException in run - Keep-Alive not enabled");
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
                XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): IOException in run");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ioe);
                }
            }
        } catch (Exception e)
        {
            XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): exception in run");
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
    public void handleServer() throws Exception
    {
    	pcef_IP =  myDiamClient.getRemoteIP();
    	pcrf_Port =  socket.getLocalPort()+"";
    	pcef_Port =  socket.getPort()+"";
    	pcrf_IP =   myDiamClient.getLocalIP();
        synchronized (key)
        {
            instances++;
            XTTProperties.printDebug("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): New Server handled by "+id+" instance "+instances);
            key.notify();
        }
        try
        {
            // As long as it takes
            boolean doWhile=true;
            // do the loop

            while(doWhile&&socket.isConnected()&&!stop)
            {
            	 XTTProperties.printDebug("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): Client connected: "+socket.getRemoteSocketAddress()+"\n");
                 BufferedInputStream   in = new BufferedInputStream(socket.getInputStream(),65536);
                 BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream());    
				 Diameter_PDUHeader pdu_header = new Diameter_PDUHeader("DiameterWorkerClient(" + myDiamClientPort + "/"+ getWorkerId() + ")");
				try
				{
					pdu_header.readPDUHeader(in);
					
				}
				catch (Exception e)
				{
					XTTProperties.printVerbose("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId()+ "): pdu empty, possible disconnect");
					doWhile = false;
					return;
				}
                	XTTProperties.printTransaction("DiameterWorkerClient/HEADER  "+XTTProperties.DELIMITER+myDiamClientPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
                    + pdu_header.getCommandname()+XTTProperties.DELIMITER
                    + pdu_header.getApplicationID()+XTTProperties.DELIMITER
                    + pdu_header.getHopbyhopID()+XTTProperties.DELIMITER
                    + pdu_header.getEndtoendID()+XTTProperties.DELIMITER
                    + pdu_header.getCmdflags());
                
                //System.out.println("START: "+System.nanoTime()+" "+System.currentTimeMillis()+" "+getWorkerId());

                Diameter_PDUBody pdu_body=new Diameter_PDUBody("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + ")",extendedStoreVar);

                String[] storeVar=new String[]{"DIAMETER","DIAMETER/"+myDiamClientPort};
                
                try
				{
                	 pdu_body.readPDUBody(in,pdu_header,extendedOutput,storeVar);
				}
                
                catch (Exception e)
				{
					
                	XTTProperties.printDebug(e.getStackTrace().toString());
                	doWhile = false;
					return;
				}
					handeleCmdReq(pdu_header.getCommandcode(), pdu_body.getAVPMap());
					// If RE_AUTH_RESPONSE do not send response
					if (pdu_header.getCommandcode() == 258)
					{
						String sSessionId = pdu_body.getAVPMap().get(DiameterWorkerServer.getAVPCode("SESSION_ID")).elementAt(0).data;
						
						if (DiameterManager.usersTab.getUser(sSessionId) != null)
						{
							XTTProperties.printDebug("RE_AUTH_RESPONSE For SessionId: " + sSessionId);
							CmdDataItem.totalRARRes++;
							DiameterManager.cmdTab.addCmd("C",pcrf_IP,pcef_IP, pcrf_Port, pcef_Port,
									sSessionId, "RAR", CmdDataItem.Type.RESPONSE, "",DiameterManager.usersTab.getUser(sSessionId));
						}
						else
						{
							XTTProperties.printDebug("RE_AUTH_RESPONSE For SessionId: " + sSessionId+ " isn't the internal Users DB (user deleted/not exist!)");
							CmdDataItem.totalRARRes++;
							DiameterManager.cmdTab.addCmd("C",pcrf_IP,pcef_IP, pcrf_Port, pcef_Port,"", "RAR", CmdDataItem.Type.RESPONSE, "",null);
						}
					
					}
					else if (!sendDiameterResponse(pdu_header, pdu_body, out))
					{
						return;
					}
					

                myDiamClient.notifyRequest(pdu_header.getCommandcode());
                synchronized(requestkey)
                {
                    requestcount++;
                    requestkey.notifyAll();
                }
                //System.out.println("STOP : "+System.nanoTime()+" "+System.currentTimeMillis()+" "+getWorkerId());
            }

        } catch (Exception e)
        {
            socket.close();
            e.printStackTrace();
            XTTProperties.printDebug("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): Connection closed");
            synchronized (key)
            {
                instances--;
                key.notify();
                
                if(DiameterClient.numOfTcpCon>0)
                	DiameterClient.numOfTcpCon--;
            }
            
            myDiamClient.removeWorker(this);
        }
    }
	public synchronized void handeleCmdReq(int cmdCode, HashMap<Integer, Vector<Diameter_AVP>> avpMap)
			throws Exception
	{
		String sessionId = "";
		pcef_IP =  myDiamClient.getRemoteIP();
    	pcrf_Port =  socket.getLocalPort()+"";
    	pcef_Port =  socket.getPort()+"";
    	pcrf_IP =   myDiamClient.getLocalIP();
		CmdDataItem.totalUsers = DiameterManager.usersTab.users.size();
		
		if (cmdCode == DWR)
		{
				XTTProperties.printDebug("DEVICE_WATCHDOG_REQUEST For SessionId: " + sessionId);
				DiameterManager.cmdTab.addCmd("C",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "DWD", CmdDataItem.Type.REQUEST,"DWD",null);
				CmdDataItem.totalDWDReq++;
		}
		else if (cmdCode == CER)
		{
				XTTProperties.printDebug("CAPABILITIES_EXCHANGE_REQUEST For SessionId: " + sessionId);
				CmdDataItem.totalCERReq++;
				
				DiameterManager.cmdTab.addCmd("C",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sessionId, "CER", CmdDataItem.Type.REQUEST,"CER",null);
		}
	}
	//This function get the pdu_header full from the inputstream and add to it the AVP from the xml nad send it 
    public boolean sendDiameterResponse(Diameter_PDUHeader pdu_header, Diameter_PDUBody pdu_body, BufferedOutputStream out) throws Exception
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
        
        StringBuffer output=new StringBuffer("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE: Header/Body: ");

        Diameter_PDUBody response_body = new Diameter_PDUBody("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
        
        //Set the req to res
        pdu_header.setRequest(false);
        //Get the all relevant block from xml file 
        Vector<Element> responseElemets = myDiamClient.getResponseElements(pdu_header.getCommandcode());
        
        if(responseElemets==null)
        {
            XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response defined for " + pdu_header.getCommandname());
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
            XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response matched for "+pdu_header.getCommandname());
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
        
        response_body.setAVPs(createAVPsFromXml(currentElement,pdu_body.getAVPMap(),null,CmdDataItem.Type.RESPONSE,tempREGEX));
        	
        //System.out.println("RAVPS: "+response_body.getAVPs());

        // Convert the defined response AVPs to binary ---> Here the convert pdu to bin
        byte[] body=response_body.createPDUBody();
        //System.out.println("RBODY: \n"+ConvertLib.getHexView(body)); 
        pdu_header.setMessagelength(pdu_header.HEADLENGTH+body.length);
        // Create the binary header---> Here the convert header to bin
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
			        
        XTTProperties.printTransaction("DiameterWorkerClient/RESPONSE"+XTTProperties.DELIMITER+myDiamClientPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
            + pdu_header.getCommandname()+XTTProperties.DELIMITER
            + pdu_header.getApplicationID()+XTTProperties.DELIMITER
            + pdu_header.getHopbyhopID()+XTTProperties.DELIMITER
            + pdu_header.getEndtoendID()+XTTProperties.DELIMITER
            + matchAVP+XTTProperties.DELIMITER
            + matchREGEX);
        
        output.append("\n"+ ConvertLib.getHexView(header));
        //output.append("\nDiameterWorkerClient("+myServerPort+"/"+getWorkerId()+"): RESPONSE: Body:");
        output.append(""+ ConvertLib.getHexView(body));
        
        if(extendedOutput)
        {
            StringBuffer extoutput=new StringBuffer("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE: Body created: \n");
            Diameter_PDUBody sent_response=new Diameter_PDUBody("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
            String[] storeVar     = new String[] {"DIAMETER/"+myDiamClientPort+"/RESPONSE","DIAMETER/RESPONSE"};
            sent_response.decodePDUBody(body,pdu_header,extoutput,output,storeVar);
        }
        //if(extendedOutput)XTTProperties.printDebug(extoutput.toString());
        XTTProperties.printDebug(output.toString());
        return true;
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
        
        Diameter_PDUHeader pdu_header=new Diameter_PDUHeader("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + ")");
        Diameter_PDUBody pdu_body=new Diameter_PDUBody("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + ")",extendedStoreVar);
        //Hard Coded 
        if (avpName=="RE_AUTH_REQUEST") 
        {
        	pdu_header.readPDUHeader(258);
        	
        }
        
        StringBuffer output=new StringBuffer("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE: Header/Body: ");
        Diameter_PDUBody response_body = new Diameter_PDUBody("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): RESPONSE",extendedStoreVar);
        
		        if (matchAVPIn != null && matchREGEXIn!=null)
		        {
		        	XTTProperties.printDebug("Sending " +  avpName + "," + matchAVPIn + " "+ matchREGEXIn + " for SessionId-" + sessionId);
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
		            XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response defined for " + pdu_header.getCommandname());
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
		            XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): no response matched for "+pdu_header.getCommandname());
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
							synchronized (socket)
							{
								BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream(), 65536);
								// Write the response
								out.write(header);
								out.write(body);
								out.flush();
							}
			        XTTProperties.printTransaction("DiameterWorkerClient/RESPONSE"+XTTProperties.DELIMITER+myDiamClientPort+XTTProperties.DELIMITER+getWorkerId()+XTTProperties.DELIMITER
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
    	   
    	pcef_IP =  myDiamClient.getRemoteIP();
       	pcrf_Port =  socket.getLocalPort()+"";
       	pcef_Port =  socket.getPort()+"";
       	pcrf_IP =   myDiamClient.getLocalIP();
       	   
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
						DiameterManager.cmdTab.addCmd("C",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port, sSessionId, "RARU",cmdType, "UPDATE", DiameterManager.usersTab.getUser(sSessionId));
						}
						else
						{
							CmdDataItem.totalRARUReq++;
							DiameterManager.cmdTab.addCmd("C",pcrf_IP,pcef_IP, socket.getLocalPort() + "", socket.getPort() + "",sSessionId, "RARU", cmdType, "UPDATE", null);
						}
					}
					else
					{
						if (DiameterManager.usersTab.getUser(sSessionId) != null)
						{
							CmdDataItem.totalRARTReq++;
							DiameterManager.cmdTab.addCmd("C",pcef_IP,pcrf_IP, pcef_Port,pcrf_Port, sSessionId, "RART",cmdType, "TERMINATION", DiameterManager.usersTab.getUser(sSessionId));
						}
						else
						{
							CmdDataItem.totalRARTReq++;
							DiameterManager.cmdTab.addCmd("C",pcef_IP,pcrf_IP,pcef_Port,pcrf_Port,sSessionId, "RART", cmdType, "TERMINATION", null);
						}
					}
				}
			}
			if (cmdType == CmdDataItem.Type.RESPONSE)
			{
				if (cmdName.equalsIgnoreCase("DEVICE_WATCHDOG_REQUEST"))
				{
					CmdDataItem.totalDWDRes++;
					DiameterManager.cmdTab.addCmd("C",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "DWD", cmdType, "DWD", null);
				}
				else if (cmdName.equalsIgnoreCase("CAPABILITIES_EXCHANGE_REQUEST"))
				{
					CmdDataItem.totalCERRes++;
					DiameterManager.cmdTab.addCmd("C",pcrf_IP,pcef_IP,pcrf_Port,pcef_Port,sSessionId, "CER", cmdType, "DWD", null);
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
                XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId()+ "): AVP not found " + currentE.getName());
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
                	currentAVPs=avpMap.get(DiameterWorkerServer.getAVPCode(tempReqAttr));
                	
                    if(currentAVPs==null||currentAVPs.size()<=0)
                    {
                        XTTProperties.printFail("DiameterWorkerClient(" + myDiamClientPort + "/" + getWorkerId() + "): AVP not found in request " + tempReqAttr);
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
            XTTProperties.printFail("DiameterWorkerClient.waitForRequests: no instance running!");
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
                XTTProperties.printInfo("DiameterWorkerClient.waitForRequests: "+requestcount+"/"+number);
                if(wait>0)
                {
                    prevcount=requestcount;
                    requestkey.wait(wait);
                    if(requestcount==prevcount)
                    {
                        XTTProperties.printFail("DiameterWorkerClient.waitForRequests: timed out!");
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return;
                    }
                } else
                {
                    requestkey.wait();
                }
            }
            XTTProperties.printInfo("DiameterWorkerClient.waitForRequests: "+requestcount+"/"+number);
        }
    }
    public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
    {
        if(DiameterServer.checkSockets())
        {
            XTTProperties.printFail("DiameterWorkerClient.waitForTimeoutRequests: no instance running!");
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
                XTTProperties.printInfo("DiameterWorkerClient.waitForTimeoutRequests: "+requestcount+"/"+number+" time: "+timeouttime+"ms");
                prevcount=requestcount;
                requestkey.wait(wait);
                if(requestcount==prevcount)
                {
                    XTTProperties.printInfo("DiameterWorkerClient.waitForTimeoutRequests: timed out!");
                    return;
                }
            }
            XTTProperties.printFail("DiameterWorkerClient.waitForTimeoutRequests: request received");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
        }
    }
    public static final String tantau_sccsid = "@(#)$Id: DiameterWorkerClient.java,v 1.21 2011/04/18 12:17:18 rajesh Exp $";
}