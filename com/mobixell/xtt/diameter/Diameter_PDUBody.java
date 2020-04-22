package com.mobixell.xtt.diameter;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import java.io.BufferedInputStream;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.ConvertLib;
import com.mobixell.xtt.HTTPHelper;
import com.mobixell.xtt.ByteArrayWrapper;
import com.mobixell.xtt.diameter.server.DiameterWorkerServer;
/**
 * <p>Diameter_PDUBody</p>
 * @author Roger Soder
 * @version $Id: Diameter_PDUBody.java,v 1.5 2009/11/17 14:14:18 rsoder Exp $
 */
public class Diameter_PDUBody implements DiameterConstants
{
    private static final int OUTPUTLENGTH=40;

    // A map of all the AVPs and their children
    private HashMap<Integer, Vector<Diameter_AVP>> avpMap=new HashMap<Integer, Vector<Diameter_AVP>>();
    public HashMap<Integer, Vector<Diameter_AVP>> getAVPMap()
														    {
														        return avpMap;
														    }
    
    // The AVPs store in this body
    private Vector<Diameter_AVP> avps=new Vector<Diameter_AVP>();
    public Vector<Diameter_AVP> getAVPs()
    {
        return avps;
    }
    public void setAVPs(Vector<Diameter_AVP> avps)
    {
        this.avps=avps;
    }

    public String function=null;
    private Set<String> extendedStoreVar = null;
    
    public Diameter_PDUBody(String sfunction)
    {
        function=sfunction;
    }

    

    public Diameter_PDUBody(String sfunction, Set<String> extendedStoreVar)
    {
        function=sfunction;
        this.extendedStoreVar=extendedStoreVar;
    }
    public void createEmptyPDUBody(String avpName,String matchAVP,String matchREGEX,String sessionId)
    {
    	Vector<Diameter_AVP> avps;
        Diameter_AVP avp ;
    	
        HashMap<Integer, Vector<Diameter_AVP>> avpmap = getAVPMap();
        avp = new Diameter_AVP();
        avps = new Vector<Diameter_AVP>();
        avp.avpcode = DiameterWorkerServer.getCommandCode(avpName);
        avp.avpname =avpName;
        avps.add(avp);
        avpmap.put(avp.avpcode, avps);
        setAVPs(avps);
        
        avps = new Vector<Diameter_AVP>();
        avp = new Diameter_AVP();
        avp.avpcode = DiameterWorkerServer.getAVPCode("SESSION_ID");
        avp.avpname =DiameterWorkerServer.getAVPName(avp.avpcode);
        avp.data = (String)sessionId;
        avps.add(avp);
        avpmap.put(avp.avpcode, avps);
        setAVPs(avps);
        
        if (matchAVP != null && matchREGEX!=null)
        {
        	avp = new Diameter_AVP();
            avps = new Vector<Diameter_AVP>();
            avp.avpcode = DiameterWorkerServer.getAVPCode(matchAVP);
            avp.avpname =matchAVP;
            avp.data = matchREGEX;
            avps.add(avp);
            avpmap.put(avp.avpcode, avps);
            setAVPs(avps);
            
        }
    }
    /**
    * Converts the Diamerer_AVP objects into the full binary message byte array.
    */
    public byte[] createPDUBody()
    {
        Iterator<Diameter_AVP> avpsI=avps.iterator();
        Vector<ByteArrayWrapper> avpBytes=new Vector<ByteArrayWrapper>();
        Diameter_AVP avp=null;
        byte[] avpEncoded=null;
        int length=0;
        while(avpsI.hasNext())
        {
            avp=avpsI.next();
            //System.out.println("CAVP: "+avp);
            avpEncoded=avp.encode();
            length=avpEncoded.length+length;
            avpBytes.add(new ByteArrayWrapper(avpEncoded));
        }
        byte[] payload=new byte[length];
        Iterator<ByteArrayWrapper> it=avpBytes.iterator();
        int currentpointer=0;
        ByteArrayWrapper current=null;
        while(it.hasNext())
        {
            current=it.next();
            currentpointer=ConvertLib.addBytesToArray(payload,currentpointer,current.getArray());
        }
        return payload;
    }

    /**
    * Read and Convert the Body of the Diamter packet from the stream according to the information in the Diameter_PDUHeader. ExtendedOutput can be enabled to spam more information, do NOT enable by default.
    */
    public void readPDUBody(BufferedInputStream in, Diameter_PDUHeader header, boolean extendedOutput, String[] storeVar) throws Exception
    {
        StringBuffer extoutput=new StringBuffer(function+": Body Received: \n");
        StringBuffer output=new StringBuffer(function+": Storing AVPS "+storeVar[0]+"/AVP/:");
        try
        {
            byte[] body=new byte[header.getMessagelength()-header.HEADLENGTH];
            HTTPHelper.readBytes(in,body);
            XTTProperties.printVerbose(function+": Body Received: "+body.length+" bytes");
            
            //Decode the AVPs
            decodePDUBody(body, header, extoutput, output, storeVar);
            
         } catch (Exception ex)
         {
        	 if (!DiameterManager.runLoad)
        	 {	 
        		 XTTProperties.printFail(function+": Exception while reading AVPs: \n"+ extoutput.toString());
        		 XTTProperties.setTestStatus(XTTProperties.FAILED);
        	 }
        	 throw ex;
            
         } finally
         {
            if(extendedOutput)XTTProperties.printDebug(extoutput.toString());
            XTTProperties.printDebug(output.toString());
         }
    }
    /**
     * Decode the Body as Diameter_AVP and store its and the ehaders values as XTT Variables.
     */
    public void decodePDUBody(byte[] body, Diameter_PDUHeader header, StringBuffer extoutput, StringBuffer output, String[] storeVar) throws Exception
    {
        Vector<String> extendedStoreVarValue=new Vector<String>();
        int pointer=0;
        int i=0;
        Diameter_AVP avp=null;
        while(pointer<body.length)
        {
            avp=new Diameter_AVP();
            
            //Decode current AVP
            pointer=avp.decode(body,pointer,i,extendedStoreVar,extendedStoreVarValue,avpMap);
            
            extoutput.append("\nAVP number "+i+" pointer="+pointer);
            extoutput.append(avp.output);
            avps.add(avp);
            //extoutput.append("\n");
            i++;
        }
        boolean doStore=false;
        if(storeVar!=null)
        {
            doStore=true;
            for(int y=0;y<storeVar.length;y++)
            {
                header.storeHeader(storeVar[y]+"/HEADER",extendedStoreVarValue);
                setVariable(storeVar[y]+"/AVP/LENGTH",""+avps.size());
                //setVariable("DIAMETER/"+myServerPort+"/AVP/LENGTH",""+avps.size());
            }
        }
        StringBuffer currentOutput=output;
        for(int y=0;y<storeVar.length;y++)
        {
            storeAVPs(avps,storeVar[y]+"/AVP","  ", extendedStoreVarValue,currentOutput,doStore);
            //storeAVPs(avps,"DIAMETER/"+myServerPort+"/AVP","  ", extendedStoreVarValue,null,doStore);
            currentOutput=null;
        }
            
    }
    public void storeAVPs(Vector<Diameter_AVP> avps,String storeVarPrefix, String dis, Vector<String> extendedStoreVarValue, StringBuffer output, boolean doStore)
    {
        Iterator<Diameter_AVP> avpsI=avps.iterator();
        Diameter_AVP avp=null;
        //System.out.println("AP: "+avps);
        while(avpsI.hasNext())
        {
            avp=avpsI.next();
            //System.out.println("SA: "+avp);
            if(doStore)storeAVP(avp,storeVarPrefix,extendedStoreVarValue);
            if(output!=null)output.append("\n"+ConvertLib.createString(dis+avp.avpname,OUTPUTLENGTH)+"= "+avp.data);
            storeAVPs(avp.getGroupedAVPs(),storeVarPrefix+"/"+avp.avpname, dis+"  ", extendedStoreVarValue,output,doStore);
        }
    }
    public void storeAVP(Diameter_AVP avp, String storeVarPrefix,Vector<String> extendedStoreVarValue)
    {
        String storeVar=storeVarPrefix;
        setVariable(storeVar+"/"+avp.avpname,avp.data);
        setVariable(storeVar+"/"+avp.avpname+"/"+avp.vendorID,avp.data);
        setVariable(storeVar+"/"+avp.avpname+"/vendorID",""+avp.vendorID);
        setVariable(storeVar+"/"+avp.avpname+"/avpcode",""+avp.avpcode);
        setVariable(storeVar+"/"+avp.avpname+"/avpSflags",""+avp.avpSflags);
        setVariable(storeVar+"/"+avp.avpname+"/length",""+avp.getGroupedAVPs().size());
        if(extendedStoreVarValue==null)return;
        Iterator<String> it=extendedStoreVarValue.iterator();
        StringBuffer extStoreVar=new StringBuffer("");
        while(it.hasNext())
        {
            extStoreVar.append(it.next());
        }
        storeVar=storeVarPrefix+"/"+extStoreVar;
        //System.out.println(storeVar+"/"+avp.avpname);
        setVariable(storeVar+"/"+avp.avpname,avp.data);
        setVariable(storeVar+"/"+avp.avpname+"/"+avp.vendorID,avp.data);
        setVariable(storeVar+"/"+avp.avpname+"/vendorID",""+avp.vendorID);
        setVariable(storeVar+"/"+avp.avpname+"/avpcode",""+avp.avpcode);
        setVariable(storeVar+"/"+avp.avpname+"/avpSflags",""+avp.avpSflags);
        setVariable(storeVar+"/"+avp.avpname+"/length",""+avp.getGroupedAVPs().size());
    }
    private void setVariable(String where, String what)
    {
        //System.out.println(where);
        XTTProperties.setVariable(where,what);
    }
}
