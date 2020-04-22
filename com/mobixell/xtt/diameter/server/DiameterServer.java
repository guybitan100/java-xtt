package com.mobixell.xtt.diameter.server;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;
import java.io.FileInputStream;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.TrustManagerFactory;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.diameter.DiameterConstants;
import com.mobixell.xtt.diameter.DiameterManager;
import com.mobixell.xtt.diameter.client.DiameterClient;
import com.mobixell.xtt.diameter.load.RuleManager;

import org.jdom.Document;
import org.jdom.Element;

/**
 * a very simple, multi-threaded Diameter server. Implementation notes are in
 * DiameterServer.html, and also as comments in the source code.
 * 
 * @version $Id: DiameterServer.java,v 1.6 2011/03/08 10:50:32 guybitan Exp $
 * @author Roger Soder & Guy Bitan
 */
public class DiameterServer extends Thread implements DiameterConstants
{
	public static final int DEFAULTPORT = 3868;
	public static final int DEFAULTSECUREPORT = 1345;
	public static RuleManager ruleManager = new RuleManager();
	private int port;
	private String ipAddr = "";
	public static Socket pcefSocket = null;
	DiameterClient diameterClient;
	public String getIp()
	{
		return ipAddr;
	}

	public Socket getPcefSocket() {
		return pcefSocket;
	}

	public int getPort()
	{
		return port;
	}

	public ServerSocket pcrfSocket = null;
	private static HashMap<String, DiameterServer> serverMap = new HashMap<String, DiameterServer>();

	private char[] KEYSTOREPW = "xttxtt".toCharArray();
	private boolean stopGracefully = false;

	/*
	 * A set of worker threads is available. This is where they live when they
	 * are idle
	 */
	private Vector<DiameterWorkerServer> runningServerWorkerthreads = new Vector<DiameterWorkerServer>();
	private static int idCounter = 0;

	public int getIdCounter()
	{
		return idCounter;
	}

	public static void resetWorkerId()
	{
		idCounter = 0;
	}

	private HashMap<Integer, DiameterResponse> responseMap = new HashMap<Integer, DiameterResponse>();

	private int timeout;
	private int workerCounter = 0;

	public synchronized void addWorker()
	{
		workerCounter++;
	}
	public synchronized void removeWorker(DiameterWorkerServer diameterWorkerServer)
	{
		workerCounter--;
		runningServerWorkerthreads.remove(diameterWorkerServer);
	}
	public synchronized int getNumWorkers()
	{
		return workerCounter;
	}
	public static final String tantau_sccsid = "@(#)$Id: DiameterServer.java,v 1.6 2011/03/08 10:50:32 mlichtin Exp $";
	public Document respDocument;

	/* *//**
	 * Main method for Diameter server
	 * 
	 * @param a
	 *            may hold port number, protocol etc. upon which to listen for
	 *            SMPP/UCP requests
	 * @throws Exception
	 */
	/*
	 * 
	 * /** Create a new instance of the DiameterServer class, listening on the
	 * specified non-secure port for HTTP requests. The configured number of
	 * DiameterWorker threads are started, and stored to the "threads" Vector A
	 * ServerSocket is started to listen for incoming requests, which will then
	 * be dispatched to an available worker thread
	 * 
	 * @param ip TODO
	 * 
	 * @param port
	 * 
	 * @throws Exception
	 */

	public DiameterServer(String ip, int port, Document respDocument, int timeout, boolean runSecure) throws Exception
	{
		super("DiameterServer(" + port +")");
		this.port = port;
		this.ipAddr = ip;
		setResponseDocument(respDocument);
		this.timeout = timeout;
		this.respDocument = respDocument;
		if (!runSecure)
		{
			pcrfSocket = new ServerSocket();
		}
		else
		{
			SSLServerSocket sslss = null;
			KeyStore keystore = null;
			KeyManagerFactory kmf = null;
			SSLContext sslc = null;

			sslc = SSLContext.getInstance("SSL");
			kmf = KeyManagerFactory.getInstance("SunX509");
			keystore = KeyStore.getInstance("JKS");
			keystore.load(new FileInputStream("key/xttkeystore"), KEYSTOREPW);
			kmf.init(keystore, KEYSTOREPW);

			TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
			tmf.init(keystore);

			sslc.init(kmf.getKeyManagers(), tmf.getTrustManagers(), new java.security.SecureRandom());

			SSLServerSocketFactory sslsocketfactory = sslc.getServerSocketFactory();
			sslss = (SSLServerSocket) sslsocketfactory.createServerSocket();

			pcrfSocket = sslss;

		}

		pcrfSocket.setReuseAddress(true);

		if (ipAddr == null) pcrfSocket.bind(new java.net.InetSocketAddress(port));
		else pcrfSocket.bind(new java.net.InetSocketAddress(ipAddr, port));
		
		synchronized (serverMap)
		{
			serverMap.put(port + "", this);
		}

	}

	public DiameterServer(int port, Document respDocument, int timeout) throws Exception
	{
		this(null, port, respDocument, timeout, false);
	}

	public static int getLastWorkerId(String port)
	{
		synchronized (serverMap)
		{
			DiameterServer s = (DiameterServer) serverMap.get(port);
			return s.getIdCounter();
		}
	}

	public void closeSocket() throws Exception
	{
		synchronized (serverMap)
		{
			pcrfSocket.close();
			serverMap.remove(port + "");
		}
	}

	public static void closeSockets() throws Exception
	{
		synchronized (serverMap)
		{
			java.util.Iterator<String> it = serverMap.keySet().iterator();
			DiameterServer s = null;
			String currentPort = null;
			while (it.hasNext())
			{
				currentPort = it.next();
				s = serverMap.get(currentPort);
				s.stopGracefully();
			}
			serverMap.clear();
		}
	}

	public static void closeSocket(String port) throws Exception
	{
		synchronized (serverMap)
		{
			DiameterServer s = (DiameterServer) serverMap.get(port);
			s.stopGracefully();
			serverMap.remove(port);
		}
	}

	public static boolean checkSockets()
	{
		synchronized (serverMap)
		{
			return serverMap.isEmpty();
		}
	}
	public boolean checkSocket()
	{
		synchronized (serverMap)
		{
			return (pcrfSocket == null);
		}
	}

	public static boolean checkSocket(String port)
	{
		synchronized (serverMap)
		{
			return (serverMap.get(port) == null);
		}
	}

	public static void setResponseDocument(String port, Document responseDocument)
	{
		synchronized (serverMap)
		{
			(serverMap.get(port)).setResponseDocument(responseDocument);
		}
	}

	public static int waitForRequests(String port, int commandcode, int number) throws java.lang.InterruptedException
	{
		DiameterServer server = null;
		synchronized (serverMap)
		{
			server = serverMap.get(port);
		}
		if (server == null)
		{
			XTTProperties.printFail("DiameterServer.waitForRequests: no instance running on port " + port + "");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return XTTProperties.FAILED;
		}
		else
		{
			return server.waitForRequests(commandcode, number);
		}
	}

	public int waitForRequests(int commandcode, int number) throws java.lang.InterruptedException
	{
		int status = XTTProperties.PASSED;
		
		DiameterResponse response = null;
		synchronized (responseMap)
		{
			response = responseMap.get(commandcode);
		}
		if (response == null)
		{
			XTTProperties.printFail("DiameterServer(" + port + ").waitForRequest: no response defined for "
					+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST));
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return XTTProperties.FAILED;
		}
		int wait = XTTProperties.getIntProperty("DIAMETERSERVER/WAITTIMEOUT");
		if (wait < 0) wait = DiameterManager.DEFAULTTIMEOUT;
		int prevcount = 0;
		synchronized (response)
		{
			if (number <= 0)
			{
				number = response.getRequests();
			}
			while (response.getRequests() < number)
			{
				XTTProperties.printInfo("DiameterServer(" + port + ").waitForRequest: "
						+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": "
						+ response.getRequests() + "/" + number);
				if (wait > 0)
				{
					prevcount = response.getRequests();
					response.wait(wait);
					if (response.getRequests() == prevcount)
					{
						XTTProperties.printFail("DiameterServer(" + port + ").waitForRequest: "
								+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return XTTProperties.FAILED;
					}
				}
				else
				{
					response.wait();
				}
			}
			XTTProperties.printInfo("DiameterServer(" + port + "): " + response.getRequests() + "/" + number);
		}
		return status;
	}

	public static void waitForTimeoutRequests(String port, int commandcode, int timeouttime, int maxnumber)
			throws java.lang.InterruptedException
	{
		DiameterServer server = null;
		synchronized (serverMap)
		{
			server = serverMap.get(port);
		}
		if (server == null)
		{
			XTTProperties.printFail("DiameterServer.waitForTimeoutRequests: no instance running on port " + port + "");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		else
		{
			server.waitForTimeoutRequests(commandcode, timeouttime, maxnumber);
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
			XTTProperties.printFail("DiameterServer(" + port + ").waitForTimeoutRequests: no response defined for "
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
				XTTProperties.printInfo("DiameterServer(" + port + ").waitForTimeoutRequests: "
						+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": "
						+ response.getRequests() + "/" + number + " time: " + timeouttime + "ms");
				prevcount = response.getRequests();
				response.wait(wait);
				if (response.getRequests() == prevcount)
				{
					XTTProperties.printInfo("DiameterServer(" + port + ").waitForTimeoutRequests: "
							+ DiameterWorkerServer.getCommandFullName(commandcode, CFLAG_REQUEST) + ": timed out!");
					return;
				}
			}
			XTTProperties.printFail("DiameterServer(" + port + ").waitForTimeoutRequests: "
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
			XTTProperties.printFail("DiameterServer(" + port
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
				XTTProperties.printFail("DiameterServer(" + port
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

	public Vector<Element> getResponseElements(int commandcode)
	{
		synchronized (responseMap)
		{
			DiameterResponse r = responseMap.get(commandcode);
			if (r == null) return null;
			return r.getResponseElements();
		}
	}

	/**
	 * Stop the smsc server
	 * 
	 * @throws Exception
	 */
	public void stopGracefully() throws java.lang.Exception
	{
		stopGracefully = true;
		XTTProperties.printDebug("DiameterServer(" + port + "): close ServerSocket and request stop");
		// enforce run() to break in infinite loop
		pcrfSocket.close();

		stopWorkers();

		synchronized (serverMap)
		{
			serverMap.remove(this);
		}

		// wait for process to stop, be sure that DiameterServer exits
		// this.join(); //please do that outside the DiameterServer
	}

	private void stopWorkers()
	{
		XTTProperties.printDebug("DiameterServer(" + port + "): Killing workers start");
		Vector<DiameterWorkerServer> close = new Vector<DiameterWorkerServer>();
		for (int i = 0; i < runningServerWorkerthreads.size(); ++i)
		{
			close.add(runningServerWorkerthreads.get(i));
		}
		for (int i = 0; i < close.size(); ++i)
		{
			DiameterWorkerServer w = close.get(i);
			w.setStop();
			// wait for processes to stop. be sure that all threads exit
			try
			{
				w.join();
			}
			catch (Exception ex)
			{
			}
		}
		runningServerWorkerthreads = new Vector<DiameterWorkerServer>();  // bye-bye threads
																		 // table, ready
																		// to start up
																	   // again
		XTTProperties.printDebug("DiameterServer(" + port + "): Killing workers done");
	}
	/**
	 * Part of Thread() interface, start the Diameter server
	 */
	public void run()
	{
		try
		{

			while (true)
			{
				// wait for a connection request to the Diameter server
				Socket localpcefSocket = null;
				try
				{
					localpcefSocket = pcrfSocket.accept();
					pcefSocket=localpcefSocket;	
					if (DiameterManager.customer.equalsIgnoreCase("vimpelcom"))
					{
						DiameterClient dc;
						try
						{
							dc = new DiameterClient(pcrfSocket.getInetAddress().getHostAddress(),pcefSocket.getInetAddress().getHostAddress(),pcefSocket.getLocalPort(),respDocument);
							dc.createPushWorker(2);
						}
						catch (Exception e)
						{
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				catch (java.io.IOException e)
				{
					if (stopGracefully)
					{
						XTTProperties.printDebug("DiameterServer(" + port + "): accept() interrupted by stop request");
						break;
					}
					XTTProperties.printDebug("DiameterServer(" + port + "): accept() interrupted - stopping workers");
					stopWorkers();
					throw e;
				}
				XTTProperties.printDebug("DiameterServer: connection request received");
				DiameterWorkerServer diameterServerWorker = null;
				try
				{
					diameterServerWorker = new DiameterWorkerServer(idCounter++, pcefSocket, this, timeout);
				}
				catch (Exception e)
				{

					XTTProperties.printDebug("DiameterClient: Could not connect to PCEF Server: "
							+ pcefSocket.getInetAddress().getHostName());
				}
				XTTProperties.printDebug("DiameterServer(" + port + "): starting new DiameterServerWorker: id "
						+ diameterServerWorker.getWorkerId());
				runningServerWorkerthreads.add(diameterServerWorker);
				diameterServerWorker.start();
			}
			DiameterManager.csvTarnlog.closeFile();
			if (pcefSocket != null)
			{
				pcefSocket.close();
			}
			if (pcrfSocket != null)
			{
				pcrfSocket.close();
			}
		}
		catch (java.io.IOException ex)
		{
			XTTProperties.printDebug("DiameterServer(" + port + "): Exception in run()      : " + ex.getMessage());
			return;
		}
	}
}