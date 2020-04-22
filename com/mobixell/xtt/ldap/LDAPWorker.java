package com.mobixell.xtt.ldap;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.HashMap;
import java.util.ListIterator;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.HTTPHelper;

/**
 * LDAPWorker. Processes a connection which has been received by the LDAPServer
 * 
 * @author Roger Soder
 * @version $Id: LDAPWorker.java,v 1.10 2009/03/25 09:49:17 rsoder Exp $
 */
public class LDAPWorker extends Thread implements LDAPConstants
{
	// True if the Worker should stop
	private boolean stop = false;
	// ID of the Worker
	private int id;

	// Number of instances running
	private static int instances = 0;
	// Key for the instances and the current message id
	private static Object key = new Object();

	// Input and Output Streams
	private BufferedOutputStream ldapOUT = null;
	private BufferedInputStream ldapIN = null;
    private static int DELEAY_RESPONSE = 0;
	private int timeout = -1;

	// System name the SMSC answers with
	private static String systemname = "XTT-MOBIXELL-LDAP";

	// null if not connected, else the systemid of the connection
	private static Vector<String> connected = new Vector<String>();
	private static Vector<String> connectedIp = new Vector<String>();
	private String myConnection = null;
	private String myConnectionIp = null;
	// Key for waiting for a connection
	private static Object conkey = new Object();
	// Key for waiting for a message
	private static Object msgkey = new Object();
	// number of messages received
	private static int msgcount = 0;

	// Key for waiting for a wsp message (all udh packets available)
	private static Object reqkey = new Object();
	// number of wsp messages received
	private static int reqcount = 0;

	private static Object unbindkey = new Object();
	private static int unbindcount = 0;

	// Key for waiting for a wsp message (all udh packets available)
	private static HashMap<String, Integer> searchkey = new HashMap<String, Integer>();
	// number of wsp messages received
	private static int searchcount = 0;

	private LDAPServer myServer = null;
	private Vector<LDAPPacket> runningthreads = new Vector<LDAPPacket>();

	/*
	 * Socket to client we're handling, which will be set by the LDAPServer when
	 * dispatching the request to us
	 */
	private Socket socket = null;

	private boolean isBound = false;

	/**
	 * Creates a new LDAPWorker
	 * 
	 * @param id
	 *            ID number of this worker thread
	 * @param socket
	 *            Socket of the connection
	 */
	LDAPWorker(int id, Socket socket, LDAPServer myServer, int timeout)
	{
		super("LDAPWorker-" + id);
		this.socket = socket;
		this.id = id;
		this.timeout = timeout;
		this.myServer = myServer;
		myServer.addWorker();
	}

	public String toString()
	{
		return "Worker(" + myServer.getPort() + "/" + getWorkerId() + "/" + runningthreads.size() + ")";
	}

	public int getWorkerId()
	{
		return id;
	}

	public int getInstances()
	{
		synchronized (key)
		{
			return instances;
		}
	};

	/**
	 * set flag asking worker thread to stop
	 */
	public void setStop()
	{
		this.stop = true;
		try
		{
			XTTProperties.printDebug("LDAPWorker: stop request for id: " + myServer.getPort() + "/" + getWorkerId()
					+ " -> closing socket");
			Vector<LDAPPacket> close = new Vector<LDAPPacket>();
			for (int i = 0; i < runningthreads.size(); ++i)
			{
				close.add(runningthreads.get(i));
			}
			socket.close();
			LDAPPacket w = null;
			for (int i = 0; i < close.size(); i++)
			{
				w = close.get(i);
				do
				{
					// wait for processes to stop. be sure that all threads exit
					w.join();
				}
				while (w.isAlive());
			}
		}
		catch (Exception e)
		{
		}
		synchronized (this)
		{
			notifyAll();
		}
		XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "): setStop() finished");
	}

	/**
	 * Start the worker thread
	 */
	public synchronized void run()
	{
		handleClient();
	}

	/**
	 * Handles the smpp request
	 * 
	 * @throws IOException
	 */
	private void handleClient()
	{
		// Increase the current count of running workers
		synchronized (key)
		{
			instances++;
			XTTProperties.printDebug("LDAPWorker: New Client handled by " + id + " instance " + instances);
			key.notify();
		}

		try
		{
			// Set the streams
			ldapIN = new BufferedInputStream(socket.getInputStream());
			ldapOUT = new BufferedOutputStream(socket.getOutputStream());

			// Say hello to the world
			XTTProperties.printDebug("LDAPWorker: Client connected: " + socket.getRemoteSocketAddress() + "\n");

			isBound = false;
			// As long as it takes
			boolean doWhile = true;
			// do the loop
			while (doWhile && socket.isConnected() && !stop)
			{
				// Try reading the first 16 Bytes fo the header which are always
				// the same
				// This method finishes on a disconnect or close of the socket
				// and of course on receiving the 16 bytes
				LDAPPacket packet = new LDAPPacket(ldapOUT);
				packet.readPacket(ldapIN);

				if (packet.rootTag == -1)
				{
					doWhile = false;
				}
			}
			setStop();
			// Something was wrong with the socket, perhaps it was closed on a
			// set Stop
		}
		catch (java.net.SocketException se)
		{
			XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId()
					+ "): java.net.SocketException: " + se.getMessage());
			// XTTProperties.printException(se);
			// Everything else goes here which should not happen
		}
		catch (Exception e)
		{
			XTTProperties.printException(e);
		}
		finally
		{
			// The connection is definitively gone now
			synchronized (conkey)
			{
				connected.remove(myConnection);
				connectedIp.remove(myConnectionIp);
				myConnection = null;
				myConnectionIp = null;
				conkey.notifyAll();
			}
			synchronized (msgkey)
			{
				msgcount++;
				msgkey.notifyAll();
			}
			// Make sure the socket is closed
			try
			{
				socket.close();
			}
			catch (java.io.IOException ioex)
			{
			}
			myServer.removeWorker(this);
			XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId()
					+ "): Client disconnected");
			// Decrease the number of running instances
			synchronized (key)
			{
				instances--;
				key.notify();
			}
		}
	}

	private class LDAPPacket extends Thread
	{
		private byte[] data = null;
		private int pointer = 0;
		public int rootTag = 0;
		public int messageId = -1;
		private BufferedOutputStream ldapOUT = null;

		public LDAPPacket(BufferedOutputStream ldapOUT)
		{
			this.ldapOUT = ldapOUT;
		}

		public void readPacket(BufferedInputStream in) throws Exception
		{
			rootTag = in.read();
			if (rootTag == -1)
			{
				XTTProperties.printDebug("LDAPWorker.readPacket(): unexpected root tag: 0x"
						+ ConvertLib.intToHex(rootTag) + " possible disconnect?");
				return;
			}
			else if (rootTag != UNIVERSAL + CONSTRUCTED + SEQUENCE)
			{
				XTTProperties.printFail("LDAPWorker.readPacket(): unexpected root tag: 0x"
						+ ConvertLib.intToHex(rootTag));
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				throw new Exception("unexpected root tag: 0x" + ConvertLib.intToHex(rootTag));
			}
			int len = in.read();
			if (len > 0x80)
			{
				len = len - 0x80;
				byte[] lenB = new byte[len];
				HTTPHelper.readBytes(in, lenB);
				len = ConvertLib.getIntFromByteArray(lenB, 0, lenB.length);
			}
			data = new byte[len];
			HTTPHelper.readBytes(in, data);

			pointer = 0;
			int type = getInt(1);
			if (type != UNIVERSAL + PRIMITIVE + INTEGER)
			{
				XTTProperties
						.printFail("LDAPWorker.readPacket(): messagId tag missing: 0x" + ConvertLib.intToHex(type));
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				throw new Exception("messagId tag missing: 0x" + ConvertLib.intToHex(type));
			}
			int slen = getLength();
			messageId = getInt(slen);

			XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "): 0x"
					+ ConvertLib.intToHex(rootTag) + "(UNIVERSAL:CONSTRUCTED:SEQUENCE) length:" + len + "(0x"
					+ ConvertLib.intToHex(len) + "):\n" + ConvertLib.getHexView(data, 0, data.length)
					+ "Spawning thread msgid=" + messageId + "(0x" + ConvertLib.intToHex(messageId) + ")");
			runningthreads.add(this);
			this.start();
		}

		public void run()
		{
			XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/" + messageId
					+ ") started");
			int type = data[pointer++];
			switch (type)
			{
				case APPLICATION + CONSTRUCTED + BINDREQUEST:
					handleBind();
					break;
				case APPLICATION + CONSTRUCTED + SEARCHREQUEST:
					handleSearchRequest();
					break;
				case APPLICATION + PRIMITIVE + UNBINDREQUEST:
				case APPLICATION + CONSTRUCTED + UNBINDREQUEST:
					handleUnBind();
					break;
				case APPLICATION + CONSTRUCTED + BINDRESPONSE:
				case APPLICATION + CONSTRUCTED + SEARCHRESENTRY:
				case APPLICATION + CONSTRUCTED + SEARCHRESDONE:
				case APPLICATION + CONSTRUCTED + SEARCHRESREF:
				case APPLICATION + CONSTRUCTED + MODIFYREQUEST:
				case APPLICATION + CONSTRUCTED + MODIFYRESPONSE:
				case APPLICATION + CONSTRUCTED + ADDREQUEST:
				case APPLICATION + CONSTRUCTED + ADDRESPONSE:
				case APPLICATION + CONSTRUCTED + DELREQUEST:
				case APPLICATION + CONSTRUCTED + DELRESPONSE:
				case APPLICATION + CONSTRUCTED + MODDNREQUEST:
				case APPLICATION + CONSTRUCTED + MODDNRESPONSE:
				case APPLICATION + CONSTRUCTED + COMPAREREQUEST:
				case APPLICATION + CONSTRUCTED + COMPARERESPONSE:
				case APPLICATION + CONSTRUCTED + ABANDONREQUEST:
				case APPLICATION + CONSTRUCTED + EXTENDEDREQ:
				case APPLICATION + CONSTRUCTED + EXTENDEDRESP:
					XTTProperties.printFail("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/" + messageId
							+ ") unsupported type: " + type + "(0x" + ConvertLib.intToHex(type) + ")");
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return;
				default:
					XTTProperties.printFail("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/" + messageId
							+ ") unrecoginzed type: " + type + "(0x" + ConvertLib.intToHex(type) + ")");
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return;
			}

			runningthreads.remove(this);
			XTTProperties.printDebug("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/" + messageId
					+ ") stopped");
			synchronized (reqkey)
			{
				reqcount++;
				reqkey.notifyAll();
			}
		}

		private void handleUnBind()
		{
			StringBuffer output = new StringBuffer("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/"
					+ messageId + ") decoded:");
			int len = getLength();
			XTTProperties.printDebug(output.toString() + "\n UNBINDREQUEST");
			try
			{
				XTTProperties.printDebug("LDAPWorker.handleUnBind(): stop request for id: " + myServer.getPort() + "/"
						+ getWorkerId() + " -> closing socket");
				socket.close();
			}
			catch (Exception e)
			{
			}
			isBound = false;
			synchronized (unbindkey)
			{
				unbindcount++;
				unbindkey.notifyAll();
			}
		}

		private void handleBind()
		{
			StringBuffer output = new StringBuffer("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/"
					+ messageId + ") decoded:");
			int len = getLength();
			output.append("\n BINDREQUEST");
			int type = getInt(1);
			len = getLength();
			int version = getInt(len);
			output.append("\n   version        : " + version);

			type = getInt(1);
			String name = null;
			if (type == UNIVERSAL + PRIMITIVE + NULL)
			{
				len = getLength();
			}
			else if (type == UNIVERSAL + PRIMITIVE + OCTET_STRING)
			{
				len = getLength();
				name = getOctetString(len);
			}
			output.append("\n   name           :" + name);

			String password = null;
			int authentication = getInt(1);
			int response_code = SUCCESS;
			if (authentication == CONTEXT + PRIMITIVE + AUTHENTICATIONCHOICE_SIMPLE)
			{
				len = getLength();
				password = getOctetString(len);
				output.append("\n   AUTH_SIMPLE password=" + password);

				if (myServer.checkLogin(name, password))
				{
					response_code = SUCCESS;
					output.append(" -> SUCCESS");
				}
				else
				{
					response_code = INVALIDCREDENTIALS;
					output.append(" -> INVALIDCREDENTIALS");
				}
			}
			else if (authentication == CONTEXT + PRIMITIVE + AUTHENTICATIONCHOICE_SASL)
			{
				output.append("\n   AUTH_SASL unsupported " + authentication + "(0x"
						+ ConvertLib.intToHex(authentication) + ")");
				response_code = AUTHMETHODNOTSUPPORTED;
			}
			else
			{
				output.append("\n   AUTH_UNKNOWN unsupported " + authentication + "(0x"
						+ ConvertLib.intToHex(authentication) + ")");
				response_code = AUTHMETHODNOTSUPPORTED;
			}

			byte[] response = createSimpleResponse(APPLICATION + CONSTRUCTED + BINDRESPONSE, response_code, null, null);
			XTTProperties.printDebug(output.toString() + "\nSending:\n BINDRESPONSE\n"
					+ ConvertLib.getHexView(response, 0, response.length));
			isBound = (response_code == SUCCESS);

			writeStream(ldapOUT, response);
			XTTProperties.printTransaction("LDAPWORKER/BINDREQUEST" + XTTProperties.DELIMITER + name
					+ XTTProperties.DELIMITER + authentication + XTTProperties.DELIMITER + response_code);
			synchronized (conkey)
			{
				myConnection = name;
				myConnectionIp = socket.getInetAddress().toString();
				connected.add(myConnection);
				connectedIp.add(myConnectionIp);
				conkey.notifyAll();
			}

		}

		private synchronized void handleSearchRequest()
		{
			StringBuffer output = new StringBuffer("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId() + "/"
					+ messageId + ") decoded:");
			int len = getLength();
			int maxlen = len + pointer;
			output.append("\n SEARCHREQUEST");

			int type = getInt(1);
			len = getLength();
			String baseDN = getOctetString(len);
			
			output.append("\n   baseDN         : " + baseDN);
			type = getInt(1);
			len = getLength();
			int scope = getInt(len);
			output.append("\n   scope          : " + scope);

			type = getInt(1);
			len = getLength();
			int derefAliases = getInt(len);
			output.append("\n   derefAliases   : " + derefAliases);

			type = getInt(1);
			len = getLength();
			int sizeLimit = getInt(len);
			output.append("\n   sizeLimit      : " + sizeLimit);

			type = getInt(1);
			len = getLength();
			int timeLimit = getInt(len);
			output.append("\n   timeLimit      : " + timeLimit);

			type = getInt(1);
			len = getLength();
			int attributesOnly = getInt(len);
			output.append("\n   attributesOnly : " + attributesOnly);
			if (attributesOnly == 0)
			{
				output.append(" -> FALSE");
			}
			else
			{
				output.append(" -> TRUE");
			}

			type = getInt(1);
			len = getLength();
			String filter = getFilter(type, len);
			output.append("\n   filter         : " + filter);

			Vector<String> selectedAttributes = getAttributeSelector();
			output.append("\n   selectedAttributes:" + selectedAttributes);
			if (pointer < maxlen)
			{
				type = getInt(1);
				len = getLength();
				type = getInt(1);
				len = getLength();
				type = getInt(1);
				len = getLength();
				String oid = getOctetString(len);
				;
				output.append("\n   oid :" + oid);
			}

			Vector<LDAPEntry> matches = myServer.getMatches(baseDN, filter);

			int count = 0;
			ListIterator<LDAPEntry> matchesIT = matches.listIterator();
			LDAPEntry ldapEntry = null;
			byte[] response = null;
			int response_code = SUCCESS;
			String errorMessage = null;
			if (DELEAY_RESPONSE>0)
			{
				try {
					waitForResponse();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (LDAPEntry.attributeArrMapList.size()==0)
			{
				try
				{
					while (matchesIT.hasNext() && count <= sizeLimit)
					{
						ldapEntry = matchesIT.next();
						response = createSearchEntryResponse(ldapEntry.getData(selectedAttributes,LDAPEntry.attributeMap));
						writeStream(ldapOUT, response);
						output.append("\nSending:\n SEARCHRESENTRY\n"
								+ ConvertLib.getHexView(response, 0, response.length));
						XTTProperties.printTransaction("LDAPWORKER/SEARCHRESENTRY" + XTTProperties.DELIMITER
								+ ldapEntry.getBaseDN() + XTTProperties.DELIMITER + ldapEntry.getFilter()
								+ XTTProperties.DELIMITER + selectedAttributes);
						count++;
					}
				}
				catch (NoSuchFieldException nsfe)
				{
					XTTProperties.printWarn("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId()
							+ ").handleSearchRequest(): " + nsfe.getMessage());
					response_code = NOSUCHATTRIBUTE;
					errorMessage = nsfe.getMessage();
				}
				synchronized (searchkey)
				{
					Integer i = searchkey.get(filter);
					if (i == null) i = new Integer(0);
					searchcount++;
					int sc = i.intValue() + 1;
					searchkey.put(filter, new Integer(sc));
					searchkey.notifyAll();
				}
			}
			else
			{
				Iterator<LinkedHashMap> it = LDAPEntry.attributeArrMapList.iterator();
                int ind = 1;
				while (it.hasNext())
				{
					try
					{
						if (matchesIT.hasNext())
						{
							while (matchesIT.hasNext() && count < sizeLimit)
							{
								ldapEntry = matchesIT.next();
								ldapEntry.setMultiAttrId(ind++);
								response = createSearchEntryResponse(ldapEntry.getData(selectedAttributes, it.next()));
								writeStream(ldapOUT, response);
								output.append("\nSending:\n SEARCHRESENTRY\n"
										+ ConvertLib.getHexView(response, 0, response.length));
								XTTProperties.printTransaction("LDAPWORKER/SEARCHRESENTRY" + XTTProperties.DELIMITER
										+ ldapEntry.getBaseDN() + XTTProperties.DELIMITER + ldapEntry.getFilter()
										+ XTTProperties.DELIMITER + selectedAttributes);
								count++;
							}
						}
						else
						{
							it.next();
						}
						count = 0;
						matchesIT = matches.listIterator();
				}
					catch (NoSuchFieldException nsfe)
					{
						XTTProperties.printWarn("LDAPWorker(" + myServer.getPort() + "/" + getWorkerId()
								+ ").handleSearchRequest(): " + nsfe.getMessage());
						response_code = NOSUCHATTRIBUTE;
						errorMessage = nsfe.getMessage();
					}
					synchronized (searchkey)
					{
						Integer i = searchkey.get(filter);
						if (i == null) i = new Integer(0);
						searchcount++;
						int sc = i.intValue() + 1;
						searchkey.put(filter, new Integer(sc));
						searchkey.notifyAll();
					}
				}
			}
			response = createSimpleResponse(APPLICATION + CONSTRUCTED + SEARCHRESDONE, response_code, null,errorMessage);
			XTTProperties.printDebug(output.toString() + "\nSending:\n SEARCHRESDONE\n"
					+ ConvertLib.getHexView(response, 0, response.length));
			writeStream(ldapOUT, response);
		}

		private byte[] createSimpleResponse(int type, int responsecode, String matchDN, String errorMessage)
		{
			byte[] rspCode = LDAPEntry.encodeInt(UNIVERSAL + PRIMITIVE + ENUMERATED, responsecode);
			byte[] mDN = LDAPEntry.encodeString(UNIVERSAL + PRIMITIVE + OCTET_STRING, matchDN);
			byte[] errMsg = LDAPEntry.encodeString(UNIVERSAL + PRIMITIVE + OCTET_STRING, errorMessage);
			byte[] lenB = LDAPEntry.encodeLength(rspCode.length + mDN.length + errMsg.length);

			byte[] msgId = LDAPEntry.encodeInt(UNIVERSAL + PRIMITIVE + INTEGER, messageId);
			byte[] lenTot = LDAPEntry.encodeLength(msgId.length + lenB.length + 1 + rspCode.length + mDN.length
					+ errMsg.length);
			byte[] response = new byte[1 + lenTot.length + msgId.length + lenB.length + 1 + rspCode.length + mDN.length
					+ errMsg.length];
			int counter = 0;
			response[counter++] = ConvertLib.getByteArrayFromInt(UNIVERSAL + CONSTRUCTED + SEQUENCE)[0];

			for (int i = 0; i < lenTot.length; i++)
			{
				response[counter++] = lenTot[i];
			}
			for (int i = 0; i < msgId.length; i++)
			{
				response[counter++] = msgId[i];
			}
			response[counter++] = ConvertLib.getByteArrayFromInt(type)[0];
			;
			for (int i = 0; i < lenB.length; i++)
			{
				response[counter++] = lenB[i];
			}
			for (int i = 0; i < rspCode.length; i++)
			{
				response[counter++] = rspCode[i];
			}
			for (int i = 0; i < mDN.length; i++)
			{
				response[counter++] = mDN[i];
			}
			for (int i = 0; i < errMsg.length; i++)
			{
				response[counter++] = errMsg[i];
			}
			return response;
		}

		private byte[] createSearchEntryResponse(byte[] data)
		{
			byte[] lenData = LDAPEntry.encodeLength(data.length);
			byte[] msgId = LDAPEntry.encodeInt(UNIVERSAL + PRIMITIVE + INTEGER, messageId);
			byte[] lenTot = LDAPEntry.encodeLength(msgId.length + 1 + lenData.length + data.length);
			byte[] response = new byte[1 + lenTot.length + msgId.length + 1 + lenData.length + data.length];

			int counter = 0;
			response[counter++] = ConvertLib.getByteArrayFromInt(UNIVERSAL + CONSTRUCTED + SEQUENCE)[0];

			for (int i = 0; i < lenTot.length; i++)
			{
				response[counter++] = lenTot[i];
			}
			for (int i = 0; i < msgId.length; i++)
			{
				response[counter++] = msgId[i];
			}
			response[counter++] = ConvertLib.getByteArrayFromInt(APPLICATION + CONSTRUCTED + SEARCHRESENTRY)[0];
			;
			for (int i = 0; i < lenData.length; i++)
			{
				response[counter++] = lenData[i];
			}
			for (int i = 0; i < data.length; i++)
			{
				response[counter++] = data[i];
			}
			return response;
		}

		private int getInt(int len)
		{
			// System.out.println(pointer+" "+len);
			int retval = ConvertLib.getIntFromByteArray(data, pointer, len);
			pointer = pointer + len;
			return retval;
		}

		private String getOctetString(int len)
		{
			String retval = ConvertLib.getStringFromOctetByteArray(data, pointer, len);
			if (retval.equals("")) return null;
			pointer = pointer + len;
			return retval;
		}

		private int getLength()
		{
			int len = ConvertLib.getIntFromByteArray(data, pointer++, 1);
			if (len > 0x80)
			{
				len = len - 0x80;
				len = getInt(len);
			}
			return len;
		}

		private Vector<String> getAttributeSelector()
		{
			int ftype = getInt(1);
			int flen = getLength();
			int endpointer = pointer + flen;

			Vector<String> result = new Vector<String>();
			int len = -1;
			int type = -1;
			String attribute = null;
			while (pointer < endpointer)
			{
				type = getInt(1);
				len = getLength();
				attribute = getOctetString(len);
				;
				result.add(attribute);
			}
			pointer = endpointer;
			return result;
		}

		private String getFilter(int ftype, int flen)
		{
			int endpointer = pointer + flen;
			StringBuffer filter = new StringBuffer("");

			int type = -1;
			int len = -1;
			switch (ftype)
			{
				case CONTEXT + CONSTRUCTED + FILTER_EQUALITYMATCH:
					type = getInt(1);
					len = getLength();
					filter.append(getOctetString(len));
					filter.append("=");
					type = getInt(1);
					len = getLength();
					filter.append(getOctetString(len));
					break;
				default:
					break;
			}

			pointer = endpointer;
			return filter.toString();
		}

		private void writeStream(BufferedOutputStream bout, byte[] data)
		{
			try
			{
				synchronized (bout)
				{
					bout.write(data);
					bout.flush();
				}
			}
			catch (IOException iox)
			{
				XTTProperties.printFail("LDAPWorker.readPacket(): unexpected exception " + iox.getClass().getName());
				if (XTTProperties.printDebug(null))
				{
					XTTProperties.printException(iox);
				}
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				return;
			}
		}
	}
	/**
	 * Wait on PDU_BIND from any systemid
	 */
	public static void waitForResponse() throws java.lang.InterruptedException
	{
		int wait = DELEAY_RESPONSE;
		synchronized (conkey)
		{
			if (LDAPServer.checkSockets())
			{
				XTTProperties.printFail("LDAPWorker.waitForResponse: no instance running!");
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				return;
			}
				if (wait > 0)
				{
					XTTProperties.printInfo("waitForResponse: waiting before response=" + wait);
					conkey.wait(wait);
				}
			}
	}
	/**
	 * Wait on PDU_BIND from any systemid
	 */
	public static void waitForBind() throws java.lang.InterruptedException
	{
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		synchronized (conkey)
		{
			if (LDAPServer.checkSockets())
			{
				XTTProperties.printFail("LDAPWorker.waitForBind: no instance running!");
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				return;
			}
			while (connected.isEmpty())
			{
				if (wait > 0)
				{
					XTTProperties.printInfo("waitForBind: waiting for any LDAP Bind timeout=" + wait);
					conkey.wait(wait);
					if (connected.isEmpty())
					{
						XTTProperties.printFail("LDAPWorker.waitForBind: timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					XTTProperties.printInfo("waitForBind: waiting for any LDAP Bind no timeout");
					conkey.wait();
				}
			}
		}
	}

	/**
	 * Wait on PDU_BIND from specific systemid
	 */
	public static void waitForBind(String name) throws java.lang.InterruptedException
	{
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		synchronized (conkey)
		{
			if (LDAPServer.checkSockets())
			{
				XTTProperties.printFail("LDAPWorker.waitForBind: no instance running!");
				XTTProperties.setTestStatus(XTTProperties.FAILED);
				return;
			}
			while (!connected.contains(name))
			{
				if (wait > 0)
				{
					XTTProperties.printInfo("waitForBind: waiting for LDAP Bind with name='" + name
							+ "' on LDAP-Server timeout=" + wait);
					conkey.wait(wait);
					if (!connected.contains(name))
					{
						XTTProperties.printFail("LDAPWorker.waitForBind: timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					XTTProperties.printInfo("waitForBind: waiting for LDAP Bind with name='" + name
							+ "' on LDAP-Server no timeout");
					conkey.wait();
				}
			}
		}
	}

	private static boolean checkConnection()
	{
		synchronized (conkey)
		{
			return !connected.isEmpty();
		}
	}

	private static boolean checkConnection(String name)
	{
		synchronized (conkey)
		{
			return connected.contains(name);
		}
	}

	/**
	 * Wait for a number of wsp messages
	 */
	public static void waitForRequests(int number) throws java.lang.InterruptedException
	{
		if (LDAPServer.checkSockets())
		{
			XTTProperties.printFail("LDAPWorker.waitForRequests: no instance running!");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		int prevcount = 0;

		synchronized (reqkey)
		{
			while (reqcount < number)
			{
				/*
				 * if(!checkConnection()) { XTTProperties.printFail(
				 * "LDAPWorker.waitForRequests: not connected!");
				 * XTTProperties.setTestStatus(XTTProperties.FAILED); return; }
				 */
				XTTProperties.printInfo("LDAPWorker.waitForRequests: " + reqcount + "/" + number);
				if (wait > 0)
				{
					prevcount = reqcount;
					reqkey.wait(wait);
					if (reqcount == prevcount)
					{
						XTTProperties.printFail("LDAPWorker.waitForRequests: timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					reqkey.wait();
				}
			}
			XTTProperties.printInfo("LDAPWorker.waitForRequests: " + reqcount + "/" + number);
		}
	}

	/**
	 * Wait for a number of wsp messages
	 */
	public static void waitForSearchRequests(int number) throws java.lang.InterruptedException
	{
		if (LDAPServer.checkSockets())
		{
			XTTProperties.printFail("LDAPWorker.waitForRequests: no instance running!");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		int prevcount = 0;

		synchronized (searchkey)
		{
			while (searchcount < number)
			{

				if (!checkConnection())
				{
					XTTProperties.printFail("LDAPWorker.waitForRequests: not connected!");
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return;
				}
				XTTProperties.printInfo("LDAPWorker.waitForRequests: " + searchcount + "/" + number);
				if (wait > 0)
				{
					prevcount = searchcount;
					searchkey.wait(wait);
					if (searchcount == prevcount)
					{
						XTTProperties.printFail("LDAPWorker.waitForRequests: timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					searchkey.wait();
				}
			}
			XTTProperties.printInfo("LDAPWorker.waitForRequests: " + searchcount + "/" + number);
		}
	}

	public static void waitForSearchRequests(int number, String filter) throws java.lang.InterruptedException
	{
		if (LDAPServer.checkSockets())
		{
			XTTProperties.printFail("LDAPWorker.waitForSearchRequests: no instance running!");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		int prevcount = 0;

		Integer i = searchkey.get(filter);
		if (i == null) i = new Integer(0);
		int thissearchcount = i.intValue();

		synchronized (searchkey)
		{
			while (thissearchcount < number)
			{

				if (!checkConnection())
				{
					XTTProperties.printFail("LDAPWorker.waitForSearchRequests: not connected!");
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return;
				}
				XTTProperties.printInfo("LDAPWorker.waitForSearchRequests: " + filter + ":" + thissearchcount + "/"
						+ number);
				if (wait > 0)
				{
					prevcount = thissearchcount;
					searchkey.wait(wait);
					if (thissearchcount == prevcount)
					{
						XTTProperties.printFail("LDAPWorker.waitForSearchRequests: " + filter + ": timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					searchkey.wait();
				}
			}
			XTTProperties.printInfo("LDAPWorker.waitForRequests: " + filter + ": " + thissearchcount + "/" + number);
		}
	}

	/**
	 * Wait for a number of wsp messages
	 */
	public static void waitForTimeoutRequests(int timeouttime, int maxnumber) throws java.lang.InterruptedException
	{
		if (LDAPServer.checkSockets())
		{
			XTTProperties.printFail("LDAPWorker.waitForTimeoutRequests: no instance running!");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = timeouttime;
		int prevcount = 0;
		int number = 0;

		synchronized (reqkey)
		{
			if (maxnumber >= 0)
			{
				number = maxnumber + 1;
			}
			else
			{
				number = reqcount + 1;
			}
			while (reqcount < number)
			{
				/*
				 * if(!checkConnection()) { XTTProperties.printFail(
				 * "LDAPWorker.waitForTimeoutRequests: not connected!");
				 * XTTProperties.setTestStatus(XTTProperties.FAILED); return; }
				 */
				XTTProperties.printInfo("LDAPWorker.waitForTimeoutRequests: " + reqcount + "/" + number + " time: "
						+ timeouttime + "ms");
				prevcount = reqcount;
				reqkey.wait(wait);
				if (reqcount == prevcount)
				{
					XTTProperties.printInfo("LDAPWorker.waitForTimeoutRequests: timed out with no WSP messages!");
					return;
				}
			}
			XTTProperties.printFail("LDAPWorker.waitForTimeoutRequests: WSP message received! " + reqcount + "/"
					+ number + "");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
		}
	}

	public static void waitForUnBind() throws java.lang.InterruptedException
	{
		int unbinds = 0;
		synchronized (unbindkey)
		{
			unbinds = unbindcount + 1;
		}
		waitForUnBind(unbinds);
	}
	public static void delaySearchResponse(int deleayMs) throws java.lang.InterruptedException
	{
		DELEAY_RESPONSE = deleayMs;
	}
	public static void waitForUnBind(int number) throws java.lang.InterruptedException
	{
		if (LDAPServer.checkSockets())
		{
			XTTProperties.printFail("LDAPWorker.waitForUnBind: no instance running!");
			XTTProperties.setTestStatus(XTTProperties.FAILED);
			return;
		}
		int wait = XTTProperties.getIntProperty("LDAPServer/WAITTIMEOUT");
		if (wait < 0) wait = LDAPServer.DEFAULTTIMEOUT;
		int prevcount = 0;

		synchronized (unbindkey)
		{
			while (unbindcount < number)
			{

				if (!checkConnection())
				{
					XTTProperties.printFail("LDAPWorker.waitForUnBind: not connected!");
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return;
				}
				XTTProperties.printInfo("LDAPWorker.waitForUnBind: " + unbindcount + "/" + number);
				if (wait > 0)
				{
					prevcount = unbindcount;
					unbindkey.wait(wait);
					if (unbindcount == prevcount)
					{
						XTTProperties.printFail("LDAPWorker.waitForUnBind: timed out!");
						XTTProperties.setTestStatus(XTTProperties.FAILED);
						return;
					}
				}
				else
				{
					unbindkey.wait();
				}
			}
			XTTProperties.printInfo("LDAPWorker.waitForUnBind: " + reqcount + "/" + number);
		}
	}

	public static void initialize()
	{
		synchronized (reqkey)
		{
			reqcount = 0;
		}
		synchronized (searchkey)
		{
			searchkey.clear();
			searchcount = 0;
		}
		synchronized (conkey)
		{
			connected.clear();
			connectedIp.clear();
		}
		synchronized (unbindkey)
		{
			unbindcount = 0;
		}
	}

	public static final String tantau_sccsid = "@(#)$Id: LDAPWorker.java,v 1.10 2009/03/25 09:49:17 rsoder Exp $";
}
