package com.mobixell.xtt.diameter.client;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.DiameterConstants;
import com.mobixell.xtt.diameter.load.RuleManager;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;

import org.jdom.Document;
import org.jdom.Element;

/**
 * a very simple, multi-threaded Diameter client. Implementation notes are in
 * DiameterClient.html, and also as comments in the source code.
 * <p>Handle the RAR/T/U request/response</P>
 * @version $Id: DiameterClient.java,v 1.6 2011/03/08 10:50:32 guybitan Exp $
 * @author Roger Soder & Guy Bitan
 */
public class DiameterClient implements DiameterConstants
{
	public static final int DEFAULTPORT = 3868;
	public static RuleManager ruleManager = new RuleManager();
	private int localPort;
	private String remoteIP = "";
	private String localIP = "";
	public static int numOfTcpCon = 0;
	public int numOfClientReq = 0;
	public String getLocalIP()
	{
		return localIP;
	}
	public String getRemoteIP()
	{
		return remoteIP;
	}

	public int getLocalPort()
	{
		return localPort;
	}

	int sec = 1000;
	DiameterWorkerClient diameterWorkerClient;
	DiameterWorkerPushClient diameterWorkerPushClient;
	public static SocketWheel socketWheel;
	private static HashMap<String, DiameterClient> clientMap = new HashMap<String, DiameterClient>();
	/*
	 * A set of worker threads is available. This is where they live when they
	 * are idle
	 */
	private Vector<DiameterWorkerClient> runningClientWorkerthreads = new Vector<DiameterWorkerClient>();
	private Vector<DiameterWorkerPushClient> runningPushClientWorkerthreads = new Vector<DiameterWorkerPushClient>();
	
	private static int idCounter = 0;

	public int getIdCounter()
	{
		return idCounter;
	}

	public static void resetWorkerId()
	{
		idCounter = 0;
	}

	/* the Diameter client's virtual root */
	// private Document responseDocument = null;
	private HashMap<Integer, DiameterResponse> responseMap = new HashMap<Integer, DiameterResponse>();

	/* timeout on client connections */
	public static final int DEFAULTTIMEOUT = 30000;
	public static Integer RARU_DEALAY_SEC = null;
	public static Integer RART_DEALAY_SEC = null;
	public static Integer TPS_INTERVAL = 1;
	public static Integer SCENARIO_CHANGE_EVERY = null;
	private int workerCounter = 0;
	private int pushWorkerCounter = 0;
	//private static Object key = new Object();

	public synchronized void addWorker()
	{
		workerCounter++;
	}
	public synchronized void addPushWorker()
	{
		pushWorkerCounter++;
	}
	public synchronized void removeWorker(DiameterWorkerClient diameterWorkerClient)
	{
		workerCounter--;
		runningClientWorkerthreads.remove(diameterWorkerClient);
	}
	public synchronized void removePushWorker(DiameterWorkerPushClient diameterWorkerPushClient)
	{
		pushWorkerCounter--;
		runningPushClientWorkerthreads.remove(diameterWorkerPushClient);
	}
	public synchronized int getNumWorkers()
	{
		return pushWorkerCounter;
	}
	public synchronized int getNumPushWorkers()
	{
		return workerCounter;
	}
	public static final String tantau_sccsid = "@(#)$Id: DiameterClient.java,v 1.6 2011/03/08 10:50:32 mlichtin Exp $";

	/**
	 * Create a new instance of the DiameterClient class, listening on the
	 * specified non-secure port for HTTP requests. The configured number of
	 * DiameterWorker threads are started, and stored to the "threads" Vector A
	 * ServerSocket is started to listen for incoming requests, which will then
	 * be dispatched to an available worker thread
	 * 
	 * @param remoteIP
	 * @param localPort
	 * @throws Exception
	 */

	public DiameterClient(String localIP,String remoteIP, int localPort, Document respDocument) throws Exception
	{
		setResponseDocument(respDocument);
		this.remoteIP=remoteIP;
		this.localPort=localPort;
	}

	public synchronized void createWorkers () throws Exception
	{
        		socketWheel = new SocketWheel(remoteIP, localPort);
        		
        		for (int i = numOfClientReq; i < socketWheel.getSocketsTable().size(); i++, numOfClientReq++)
        		{
				diameterWorkerPushClient = new DiameterWorkerPushClient(idCounter++,this);
				runningPushClientWorkerthreads.add(diameterWorkerPushClient);
				diameterWorkerPushClient.start();
				
				Socket socket = socketWheel.getSocketsTable().get(i+1);
		        DiameterWorkerClient diameterWorkerClient = new DiameterWorkerClient(idCounter,socket,this);
		        runningClientWorkerthreads.add(diameterWorkerClient);
				diameterWorkerClient.start();
        		}
	}
	public synchronized void createPushWorker (int numOfThreads) throws Exception
	{
		for (int i = 0; i < numOfThreads; i++)
		{
				diameterWorkerPushClient = new DiameterWorkerPushClient(idCounter++,this);
				runningPushClientWorkerthreads.add(diameterWorkerPushClient);
				diameterWorkerPushClient.start();
		}
	}
	public static int getLastWorkerId(String port)
	{
		synchronized (clientMap)
		{
			DiameterClient s = (DiameterClient) clientMap.get(port);
			return s.getIdCounter();
		}
	}
	public static void setResponseDocument(String port, Document responseDocument)
	{
		synchronized (clientMap)
		{
			(clientMap.get(port)).setResponseDocument(responseDocument);
		}
	}

	public static void waitForRequests(String port, int commandcode, int number) throws java.lang.InterruptedException
	{
		DiameterClient client = null;
		synchronized (clientMap)
		{
			client = clientMap.get(port);
		}
		if (client == null)
		{
			XTTProperties.printFail("DiameterClient.waitForRequests: no instance running on port " + port + "");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		else
		{
			client.waitForRequests(commandcode, number);
		}
	}

	public void waitForRequests(int commandcode, int number) throws java.lang.InterruptedException
	{
		DiameterResponse response = null;
		synchronized (responseMap)
		{
			response = responseMap.get(commandcode);
		}
		if (response == null)
		{
			XTTProperties.printFail("DiameterClient(" + localPort + ").waitForRequest: no response defined for "
					+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST));
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = XTTProperties.getIntProperty("DIAMETERSERVER/WAITTIMEOUT");
		if (wait < 0) wait = DiameterClient.DEFAULTTIMEOUT;
		int prevcount = 0;
		synchronized (response)
		{
			if (number <= 0)
			{
				number = response.getRequests();
			}
			while (response.getRequests() < number)
			{
				XTTProperties.printInfo("DiameterClient(" + localPort + ").waitForRequest: "
						+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": "
						+ response.getRequests() + "/" + number);
				if (wait > 0)
				{
					prevcount = response.getRequests();
					response.wait(wait);
					if (response.getRequests() == prevcount)
					{
						XTTProperties.printFail("DiameterClient(" + localPort + ").waitForRequest: "
								+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					response.wait();
				}
			}
			XTTProperties.printInfo("DiameterClient(" + localPort + "): " + response.getRequests() + "/" + number);
		}
	}

	public static void waitForTimeoutRequests(String port, int commandcode, int timeouttime, int maxnumber)
			throws java.lang.InterruptedException
	{
		DiameterClient client = null;
		synchronized (clientMap)
		{
			client = clientMap.get(port);
		}
		if (client == null)
		{
			XTTProperties.printFail("DiameterClient.waitForTimeoutRequests: no instance running on port " + port + "");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		else
		{
			client.waitForTimeoutRequests(commandcode, timeouttime, maxnumber);
		}
	}

	public void waitForTimeoutRequests(int commandcode, int timeouttime, int maxnumber)
			throws java.lang.InterruptedException
	{
		DiameterResponse response = null;
		synchronized (responseMap)
		{
			response = responseMap.get(commandcode);
		}
		if (response == null)
		{
			XTTProperties.printFail("DiameterClient(" + localPort + ").waitForTimeoutRequests: no response defined for "
					+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST));
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}

		int wait = timeouttime;
		int prevcount = 0;
		int number = 0;

		synchronized (response)
		{
			if (maxnumber >= 0)
			{
				number = maxnumber + 1;
			}
			else
			{
				number = response.getRequests() + 1;
			}
			while (response.getRequests() < number)
			{
				XTTProperties.printInfo("DiameterClient(" + localPort + ").waitForTimeoutRequests: "
						+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": "
						+ response.getRequests() + "/" + number + " time: " + timeouttime + "ms");
				prevcount = response.getRequests();
				response.wait(wait);
				if (response.getRequests() == prevcount)
				{
					XTTProperties.printInfo("DiameterClient(" + localPort + ").waitForTimeoutRequests: "
							+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": timed out!");
					return;
				}
			}
			XTTProperties.printFail("DiameterClient(" + localPort + ").waitForTimeoutRequests: "
					+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": "
					+ response.getRequests() + "/" + number + " request received");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
		}
	}

	public void notifyRequest(int commandcode)
	{
		DiameterResponse response = null;
		synchronized (responseMap)
		{
			response = responseMap.get(commandcode);
		}
		if (response != null)
		{
			synchronized (response)
			{
				response.incRequests();
				response.notifyAll();
			}
		}
	}

	public void setResponseDocument(Document responseDocument)
	{
		Element root = responseDocument.getRootElement();
		if (!root.getName().toLowerCase().equals("diameter"))
		{
			XTTProperties.printFail("DiameterClient(" + localPort
					+ ").setResponseDocument: Diameter Response Document invalid");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			throw new IllegalArgumentException("Diameter Response Document invalid");
		}
		else
		{
			List<?> elemts = null;
			Iterator<?> it = root.getChildren().iterator();
			while (it.hasNext())
			{
				root = (Element) it.next();
				if (root.getName().toLowerCase().equals("response"))
				{
					elemts = root.getChildren();
					break;
				}
			}
			if (elemts == null || elemts.size() == 0)
			{
				XTTProperties.printFail("DiameterClient(" + localPort
						+ ").setResponseDocument: Diameter Response Document invalid");
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				throw new IllegalArgumentException("Diameter Response Document invalid");
			}
			else
			{
				int code = 0;
				it = elemts.iterator();
				Vector<Element> elemnts = null;
				DiameterResponse response = null;
				synchronized (responseMap)
				{
					responseMap.clear();
					while (it.hasNext())
					{
						root = (Element) it.next();
						code = DiameterWorkerServer.getCommandCode(root.getName());
						response = responseMap.get(code);
						if (response == null) response = new DiameterResponse();
						elemnts = response.getResponseElements();
						elemnts.add(root);
						responseMap.put(code, response);
					}
				}
			}
		}
	}

	private class DiameterResponse
	{
		private Vector<Element> elements = new Vector<Element>();
		private int numRequests = 0;

		public Vector<Element> getResponseElements()
		{
			return elements;
		}

		public int getRequests()
		{
			return numRequests;
		}

		public void incRequests()
		{
			numRequests++;
		}
	}

	public synchronized Vector<Element> getResponseElements(int commandcode)
	{
		synchronized (responseMap)
		{
			DiameterResponse r = responseMap.get(commandcode);
			if (r == null) return null;
			return r.getResponseElements();
		}
	}
}
