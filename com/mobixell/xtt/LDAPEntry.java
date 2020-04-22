package com.mobixell.xtt;


import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;

import org.jdom.Document;
import org.jdom.Element;

public class LDAPEntry implements LDAPConstants
{
    private String baseDN=null;
    private byte[] dn=null;
    private String oid=null;
    private String filter=null;
    
    private LinkedHashMap<String,LDAPAttribute> attributeMap=new LinkedHashMap<String,LDAPAttribute>();
    
    public LDAPEntry(String baseDN, Document data) throws Exception
    {
        StringBuffer output=new StringBuffer("LDAPEntry: found:");

        Element root = data.getRootElement();
        if(!root.getName().equals("data"))
        {
            throw new Exception("LDAPEntry: root node is not called data");
        }
        Element filterElement=root.getChild("filter");
        if(filterElement==null)
        {
            throw new Exception("LDAPEntry: node data/filter missing");
        }
        filter=filterElement.getText();
        output.append("\n  filter   : "+filter);

        Element dnElement=root.getChild("dn");
        if(dnElement==null)
        {
            throw new Exception("LDAPEntry: node data/dn missing");
        }
        dn=encodeString(OCTET_STRING,dnElement.getText());
        output.append("\n  dn       : "+dnElement.getText());

        Element oidElement=root.getChild("oid");
        if(oidElement==null)
        {
            throw new Exception("LDAPEntry: node data/oid missing");
        }
        oid=oidElement.getText();
        output.append("\n  oid      : "+oid);

        List attributeList=root.getChildren("attribute");
        if(attributeList==null||attributeList.size()==0)
        {
            throw new Exception("LDAPEntry: node data/attribute missing, one or more required");
        }
        Iterator it=attributeList.listIterator();
        Element attributeEntry=null;
        String attributeName=null;
        int attributeType=-1;
        String attributeValue=null;
        while(it.hasNext())
        {
            attributeEntry=(Element)it.next();
            try
            {
                attributeName=attributeEntry.getChild("name").getText();
                attributeType=Integer.decode(attributeEntry.getChild("type").getText());
                attributeValue=attributeEntry.getChild("value").getText();
                attributeMap.put(attributeName,new LDAPAttribute(attributeName,attributeType,attributeValue));
                output.append("\n  attribute:");
                output.append("\n    name   = "+attributeName);
                output.append("\n    type   = "+attributeType+"(0x"+ConvertLib.intToHex(attributeType)+")");
                output.append("\n    value  = "+attributeValue);
            } catch (NullPointerException npe)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node data/attribute/name or data/attribute/type or data/attribute/value missing");
            } catch (NumberFormatException nfe)
            {
                throw new Exception("LDAPServer.buildConfiguration(): node data/attribute/type='"+attributeEntry.getChild("type").getText()+"' is not a number");
            }
        }
        XTTProperties.printDebug(output.toString());
    }
    public String toString()
    {
        return filter;
    }
    public String getFilter()
    {
        return filter;
    }
    public String getBaseDN()
    {
        return baseDN;
    }
    public byte[] getData(Vector<String> attributesWanted) throws NoSuchFieldException
    {
        byte[] data=null;
        
        Iterator<String> it=attributesWanted.listIterator();
        if(attributesWanted.size()<=0)
        {
            it=attributeMap.keySet().iterator();
        }
        Vector<LDAPAttribute> attributes=new Vector<LDAPAttribute>();
        LDAPAttribute currentAtt=null;
        int attLength=0;
        String key=null;
        while(it.hasNext())
        {
            key=it.next();
            currentAtt=attributeMap.get(key);
            if(currentAtt==null)
            {
                throw new NoSuchFieldException("Attribute '"+key+"' does not exist in server");
            }
            attLength=attLength+currentAtt.getData().length;
            attributes.add(currentAtt);
        }
        
        byte[] attrLen=LDAPEntry.encodeLength(attLength);
        data=new byte[dn.length+1+attrLen.length+attLength];

        int counter=0;
        for(int i=0;i<dn.length;i++)
        {
            data[counter++]=dn[i];
        }
        data[counter++]=ConvertLib.getByteArrayFromInt(UNIVERSAL+CONSTRUCTED+SEQUENCE)[0];
        for(int i=0;i<attrLen.length;i++)
        {
            data[counter++]=attrLen[i];
        }

        byte[] att=null;
        Iterator<LDAPAttribute> itAtt=attributes.listIterator();
        currentAtt=null;
        while(itAtt.hasNext())
        {
            att=itAtt.next().getData();
            for(int i=0;i<att.length;i++)
            {
                data[counter++]=att[i];
            }
            
        }
        return data;
    }

    private class LDAPAttribute
    {
        private byte[] data=null;
        
        public LDAPAttribute(String name, int type, String value) throws Exception
        {
            switch(type)
            {
                case OCTET_STRING: 
                    data=createData(encodeString(OCTET_STRING,name),encodeString(type,value));
                    break;
                default:
                    throw new Exception("LDAPEntry: unsupported attribute type: "+type+"(0x"+ConvertLib.intToHex(type)+")");
            }
        }
        
        private byte[] createData(byte[] namedata, byte[] valuedata)
        {
            //byte[] nameLen  = LDAPEntry.encodeLength(namedata.length);
            //This is the length of the SET part
            byte[] valueLen = LDAPEntry.encodeLength(valuedata.length);
            //This is the total length of the name and value part
            byte[] nameLen  = LDAPEntry.encodeLength(namedata.length+1+valueLen.length+valuedata.length);
            //byte[] lenTot   = LDAPEntry.encodeLength(1+nameLen.length+namedata.length+1+valueLen.length+valuedata.length);

            byte[] response = new byte[1+nameLen.length+namedata.length+1+valueLen.length+valuedata.length];
            int counter=0;
            response[counter++]=ConvertLib.getByteArrayFromInt(UNIVERSAL+CONSTRUCTED+SEQUENCE)[0];
            for(int i=0;i<nameLen.length;i++)
            {
                response[counter++]=nameLen[i];
            }
            for(int i=0;i<namedata.length;i++)
            {
                response[counter++]=namedata[i];
            }

            response[counter++]=ConvertLib.getByteArrayFromInt(UNIVERSAL+CONSTRUCTED+SET)[0];;
            for(int i=0;i<valueLen.length;i++)
            {
                response[counter++]=valueLen[i];
            }
            for(int i=0;i<valuedata.length;i++)
            {
                response[counter++]=valuedata[i];
            }
            return response;
            
        }
        
        public byte[] getData()
        {
            return data;
        }
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////
    //// Static helper methods ///////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    public static byte[] encodeString(int type,String val)
    {
        if(val==null)return new byte[]{ConvertLib.getByteArrayFromInt(type)[0],0};
        byte[] lenB=encodeLength(val.length());
        byte[] str=ConvertLib.getOctetByteArrayFromString(val);
        byte[] response=new byte[1+lenB.length+str.length];
        int counter=0;
        response[counter++]=ConvertLib.getByteArrayFromInt(type)[0];
        for(int i=0;i<lenB.length;i++)
        {
            response[counter++]=lenB[i];
        }
        for(int i=0;i<str.length;i++)
        {
            response[counter++]=str[i];
        }
        return response;
    }
    public static byte[] encodeInt(int type,int val)
    {
        byte[] intB=ConvertLib.getByteArrayFromInt(val);
        byte[] lenB=encodeLength(intB.length);
        byte[] response=new byte[1+lenB.length+intB.length];
        int counter=0;
        response[counter++]=ConvertLib.getByteArrayFromInt(type)[0];
        for(int i=0;i<lenB.length;i++)
        {
            response[counter++]=lenB[i];
        }
        for(int i=0;i<intB.length;i++)
        {
            response[counter++]=intB[i];
        }
        return response;
    }
    public static byte[] encodeLength(int val)
    {
        if(val<0x80)
        {
            return ConvertLib.getByteArrayFromInt(val);
        }
        byte[] intB=ConvertLib.getByteArrayFromInt(val);
        byte[] lenB=ConvertLib.getByteArrayFromInt(intB.length+0x80,1);
        byte[] response=new byte[lenB.length+intB.length];
        int counter=0;
        for(int i=0;i<lenB.length;i++)
        {
            response[counter++]=lenB[i];
        }
        for(int i=0;i<intB.length;i++)
        {
            response[counter++]=intB[i];
        }
        return response;
    }
}