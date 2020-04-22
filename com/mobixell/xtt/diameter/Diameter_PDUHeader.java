package com.mobixell.xtt.diameter;

import java.util.Iterator;
import java.util.Vector;
import java.io.BufferedInputStream;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.HTTPHelper;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;


/**
 * <p>Diameter_PDUHeader</p>
 * @author Roger Soder & Guy Bitan
 * @version $Id: Diameter_PDUHeader.java,v 1.2 2009/06/04 08:32:04 rsoder Exp $
 */
public class Diameter_PDUHeader implements DiameterConstants
{
    private String function=null;

    public final int HEADLENGTH=20;

    // These have to be set for creating a request and are set automatically by readPDUHeader
    private int version         = 0;
    private int messagelength   = 0;
    private int commandflags    = 0;
    private int commandcode     = 0;
    private int applicationID   = 0;
    private int hopbyhopID      = 0;
    private int endtoendID      = 0;

    private boolean request     = false;
    private boolean proxiable   = false;
    private boolean error       = false;
    private boolean tretr       = false;

    // These are informational only created by readPDUHeader
    private String commandname  = null;
    private String cmdflags     = "";

    public Diameter_PDUHeader(String sfunction)
    {
        function=sfunction;
    }
    public boolean readPDUHeader(int cmdCode) throws Exception
    {
    	version         =1;
        messagelength   =292;
        commandflags    =192;
        commandcode     =cmdCode;
        applicationID   =16777238;
        hopbyhopID      =456;
        endtoendID      =789;

        request     =((commandflags&CFLAG_REQUEST)==CFLAG_REQUEST);
        if(request)cmdflags=cmdflags+"R";
        proxiable   =((commandflags&CFLAG_PROXIABLE)==CFLAG_PROXIABLE);
        if(proxiable)cmdflags=cmdflags+"P";
        error       =((commandflags&CFLAG_ERROR)==CFLAG_ERROR);
        if(error)cmdflags=cmdflags+"E";
        tretr       =((commandflags&CFLAG_TRETR)==CFLAG_TRETR);
        if(tretr)cmdflags=cmdflags+"T";

        commandname=DiameterWorkerServer.getCommandFullName(commandcode,commandflags);
        XTTProperties.printDebug(function+": Head Responese DIAMETER/HEADER:"
                +"\nversion         ="+version
                +"\nmessagelength   ="+messagelength
                +"\ncommandflags    ="+commandflags+" ("+cmdflags+")"
                +"\ncommandcode     ="+commandcode+" ("+commandname+")"
                +"\napplicationID   ="+applicationID
                +"\nhopbyhopID      ="+hopbyhopID
                +"\nendtoendID      ="+endtoendID
                +"\n");
            return true;
    }
    public boolean readPDUHeader(BufferedInputStream in) throws Exception
    {
        byte[] b=new byte[HEADLENGTH];
        int first=in.read();
        if(first==-1)return false;
        b[0]=(byte)first;
        HTTPHelper.readBytes(in,b,1,HEADLENGTH-1);
        XTTProperties.printVerbose(function+": Head Received: 20 bytes");
        version         =ConvertLib.getIntFromByteArray(b, 0,1);
        messagelength   =ConvertLib.getIntFromByteArray(b, 1,3);
        commandflags    =ConvertLib.getIntFromByteArray(b, 4,1);
        commandcode     =ConvertLib.getIntFromByteArray(b, 5,3);
        applicationID   =ConvertLib.getIntFromByteArray(b, 8,4);
        hopbyhopID      =ConvertLib.getIntFromByteArray(b,12,4);
        endtoendID      =ConvertLib.getIntFromByteArray(b,16,4);

        request     =((commandflags&CFLAG_REQUEST)==CFLAG_REQUEST);
        if(request)cmdflags=cmdflags+"R";
        proxiable   =((commandflags&CFLAG_PROXIABLE)==CFLAG_PROXIABLE);
        if(proxiable)cmdflags=cmdflags+"P";
        error       =((commandflags&CFLAG_ERROR)==CFLAG_ERROR);
        if(error)cmdflags=cmdflags+"E";
        tretr       =((commandflags&CFLAG_TRETR)==CFLAG_TRETR);
        if(tretr)cmdflags=cmdflags+"T";

        commandname=DiameterWorkerServer.getCommandFullName(commandcode,commandflags);

        XTTProperties.printDebug(function+": Head Received DIAMETER/HEADER:"
            +"\nversion         ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(b, 0,1,2),8," ") +"="+version
            +"\nmessagelength   ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(b, 1,3,2),8," ") +"="+messagelength
            +"\ncommandflags    ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(b, 4,1,2),8," ") +"="+commandflags+" ("+cmdflags+")"
            +"\ncommandcode     ="+ConvertLib.addPrefixToString(ConvertLib.outputBytes(b, 5,3,2),8," ") +"="+commandcode+" ("+commandname+")"
            +"\napplicationID   ="+                             ConvertLib.outputBytes(b, 8,4,2)        +"="+applicationID
            +"\nhopbyhopID      ="+                             ConvertLib.outputBytes(b,12,4,2)        +"="+hopbyhopID
            +"\nendtoendID      ="+                             ConvertLib.outputBytes(b,16,4,2)        +"="+endtoendID
            +"\n");
        return true;
    }
    
    /**
    * Convert the values of the header into 20 bytes of encoded Diameter header. Do NOT forget to set the messagelength to HEADLENGTH plus the length of the body.
    */
    public byte[] createPDUHeader()
    {
        byte[] returnValue=new byte[HEADLENGTH];
        int pointer=0;

        commandflags=0;
        if(request)commandflags=commandflags|CFLAG_REQUEST;
        if(proxiable)commandflags=commandflags|CFLAG_PROXIABLE;
        if(error)commandflags=commandflags|CFLAG_ERROR;
        if(tretr)commandflags=commandflags|CFLAG_TRETR;

        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(version,1));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(messagelength,3));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(commandflags,1));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(commandcode,3));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(applicationID,4));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(hopbyhopID,4));
        pointer=ConvertLib.addBytesToArray(returnValue,pointer,ConvertLib.getByteArrayFromInt(endtoendID,4));

        return returnValue;

    }

    /**
    * Store the headers in XTT varables, storeVar is the location in front of the variable names, extendedStoreVarValue is a Vector containing
    * additional store name pieces set by the decodeBody on Diamerer_PDUBody according to extendedStoreVar and AVP values.
    */
    public void storeHeader(String storeVar, Vector<String> extendedStoreVarValue)
    {
        setVariable(storeVar+"/version"         ,version         +"");
        setVariable(storeVar+"/messagelength"   ,messagelength   +"");
        setVariable(storeVar+"/commandflags"    ,commandflags    +"");
        setVariable(storeVar+"/commandcode"     ,commandcode     +"");
        setVariable(storeVar+"/applicationID"   ,applicationID   +"");
        setVariable(storeVar+"/hopbyhopID"      ,hopbyhopID      +"");
        setVariable(storeVar+"/endtoendID"      ,endtoendID      +"");
        if(extendedStoreVarValue==null)return;
        
        Iterator<String> it=extendedStoreVarValue.iterator();
        
        StringBuffer extStoreVar=new StringBuffer("");
        while(it.hasNext())
        {
            extStoreVar.append(it.next());
        }
        storeVar="diameter/header/"+extStoreVar;                
        setVariable(storeVar+"/version"         ,version         +"");
        setVariable(storeVar+"/messagelength"   ,messagelength   +"");
        setVariable(storeVar+"/commandflags"    ,commandflags    +"");
        setVariable(storeVar+"/commandcode"     ,commandcode     +"");
        setVariable(storeVar+"/applicationID"   ,applicationID   +"");
        setVariable(storeVar+"/hopbyhopID"      ,hopbyhopID      +"");
        setVariable(storeVar+"/endtoendID"      ,endtoendID      +"");
    }
    private void setVariable(String where, String what)
    {
        //System.out.println(where);
        XTTProperties.setVariable(where,what);
    }
	public boolean isRequest() {
		return request;
	}
	public void setRequest(boolean request) {
		this.request = request;
	}
	public boolean isProxiable() {
		return proxiable;
	}
	public void setProxiable(boolean proxiable) {
		this.proxiable = proxiable;
	}
	public boolean isError() {
		return error;
	}
	public void setError(boolean error) {
		this.error = error;
	}
	public boolean isTretr() {
		return tretr;
	}
	public void setTretr(boolean tretr) {
		this.tretr = tretr;
	}
	public String getFunction() {
		return function;
	}
	public void setFunction(String function) {
		this.function = function;
	}
	public int getVersion() {
		return version;
	}
	public void setVersion(int version) {
		this.version = version;
	}
	public int getMessagelength() {
		return messagelength;
	}
	public void setMessagelength(int messagelength) {
		this.messagelength = messagelength;
	}
	public int getCommandflags() {
		return commandflags;
	}
	public void setCommandflags(int commandflags) {
		this.commandflags = commandflags;
	}
	public int getCommandcode() {
		return commandcode;
	}
	public void setCommandcode(int commandcode) {
		this.commandcode = commandcode;
	}
	public int getApplicationID() {
		return applicationID;
	}
	public void setApplicationID(int applicationID) {
		this.applicationID = applicationID;
	}
	public int getHopbyhopID() {
		return hopbyhopID;
	}
	public void setHopbyhopID(int hopbyhopID) {
		this.hopbyhopID = hopbyhopID;
	}
	public int getEndtoendID() {
		return endtoendID;
	}
	public void setEndtoendID(int endtoendID) {
		this.endtoendID = endtoendID;
	}
	public String getCommandname() {
		return commandname;
	}
	public void setCommandname(String commandname) {
		this.commandname = commandname;
	}
	public String getCmdflags() {
		return cmdflags;
	}
	public void setCmdflags(String cmdflags) {
		this.cmdflags = cmdflags;
	}
	
}
