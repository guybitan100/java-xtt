package com.mobixell.xtt;

import java.io.BufferedInputStream;


public class SMPP_PDUBody implements SMPPConstants
{
    private byte[]  body                = null;
    private int     pointer             = 0;
    private String[]  storeVar          = null;
    private boolean isMultiSegment      = false;
    private boolean hasallsegments      = true;
    private int current_reference       = -1;
    private int current_segments        = -1;
    private int current_segment_number  = -1;
    private UDHSegmentStore segmentStore= null;
    private String function=null;

    public SMPP_PDUBody(String sfunction)
    {
        function=sfunction;
    }

    public void setStoreVar(String[] s)
    {
        storeVar=s;
    }
    
    /**
     * Method to read PDU body
     * @param inHead - Headers
     * @param in - Input stream
     * @throws java.io.IOException
     */
    public void readPDUBody(SMPP_PDUHeader inHead, BufferedInputStream in) throws java.io.IOException
    {
        this.body=new byte[inHead.command_length-16];
        if(this.body.length>0)
        {
            HTTPHelper.readBytes(in,this.body);
        }
        if(XTTProperties.printDebug(null))
        {
            XTTProperties.printVerbose(function+": Received: "+body.length+" bytes\n"+ConvertLib.getHexView(this.body));
        } else
        {
            XTTProperties.printVerbose(function+": Received: "+body.length+" bytes");
        }

    }
    
    /**
     * Method to reset pointer
     */
    public void resetPointer()
    {
        pointer=0;
    }
    
    /**
     * Method to get string from byte
     * @param max
     * @return - string value
     */
    public String getStringFromCOctetByteArray(int max)
    {
        if(this.body.length>0)
        {
            String received=ConvertLib.getStringFromCOctetByteArray(this.body,pointer,max);
            int start=pointer;
            if(max==0)max=this.body.length-pointer;
            for(int i=start;i<this.body.length&&(i-start<max);i++)
            {
                pointer++;
                if(this.body[i]==(byte)0x00)
                {
                    break;
                }
            }
            //pointer=pointer+received.length()+1;
            return received;
        }else return "";
    }
    public String getStringFromOctetByteArray(int length)
    {
        String received=ConvertLib.getStringFromOctetByteArray(this.body,pointer,length);
        pointer=pointer+length;
        return received;
    }
    
    /**
     * Method to get array of byte
     * @param length
     * @return - byte value
     */
    public byte[] getByteArrayFromOctetByteArray(int length)
    {
        byte[] retVal=new byte[length];
        int count=0;
        for(int i=pointer;i<pointer+length;i++)
        {
            retVal[count++]=this.body[i];
        }
        pointer=pointer+length;
        return retVal;
    }
    
    /**
     * Method to get integer from byte array
     * @param octets
     * @return - integer value
     * @throws Exception
     */
    public int getIntFromByteArray(int octets) throws Exception
    {
        int retval=ConvertLib.getIntFromByteArray(this.body, pointer  ,octets);
        pointer=pointer+octets;
        return retval;
    }
    
    /**
     * Method to get body of PDU
     * @return It returns byte array
     */
    public byte[] getBody()
    {
        return this.body;
    }
    
    /**
     * Check Body
     */
    
    private void checkBody()
    {
        if(this.body==null)this.body=new byte[0];
    }
    
    /**
     * Handle Optional parameters
     * @throws Exception
     */
    public void decodeOptionalParameters() throws Exception
    {
        while(pointer<this.body.length)
        {
            decodeOptionalParameter();
        }
    }
    byte[] payload=null;
    
    /**
     * Method to retrieve payload
     * @return - Payload
     */
    public byte[] getPayload()
    {
        //XTTProperties.printDebug("getPayload(): Multi: "+isMultiSegment+" hasAll:"+hasallsegments);
        if(!isMultiSegment)
        {
            return payload;
        } else
        {
            return segmentStore.getData(current_reference);
        }
    }

    public boolean hasAllSegments()
    {
        //XTTProperties.printDebug("hasAllSegments(): Multi: "+isMultiSegment+" hasAll:"+hasallsegments);
        if(!isMultiSegment)
        {
            return true;
        } else
        {
            return hasallsegments;
        }
    }
    
    /**
     * Method to decode optional parameters
     * @param store
     * @throws Exception
     */
    public void decodeOptionalParameters(UDHSegmentStore store) throws Exception
    {
        this.segmentStore=store;
        decodeOptionalParameters();
        if(isMultiSegment)
        {
            if(current_segments==-1)
            {
                segmentStore.addData(current_reference,payload  ,0      ,payload.length  ,1,1);
              //segmentStore.addData(current_reference,this.body,pointer,this.body.length,1,1);
            } else
            {
                segmentStore.addData(current_reference,payload  ,0      ,payload.length  ,current_segments,current_segment_number);
              //segmentStore.addData(current_reference,this.body,pointer,this.body.length,current_segments,current_segment_number);
            }
            // We got the SAR so we probably have multiple segments
            if(current_reference!=-1)
            {
                hasallsegments=segmentStore.checkAllSegmentsHere(current_reference);
            }
        }
    }
    
    /**
     * Method to decode optional parameters
     * @throws Exception
     */
    private void decodeOptionalParameter() throws Exception
    {
        int parameter_tag=ConvertLib.getIntFromByteArray(this.body, pointer  ,2);
        int parameter_pointer=pointer;
        pointer=pointer+2;
        int length=ConvertLib.getIntFromByteArray(this.body, pointer  ,2);
        int length_pointer=pointer;
        pointer=pointer+2;
        int val_pointer=pointer;
        int val=0;
        String sval="";
        switch(parameter_tag)
        {
            // SMPP 3.4
            case OPT_DEST_ADDR_SUBUNIT:
                val=getIntVal("DEST_ADDR_SUBUNIT"   ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_NETWORK_TYPE:
                val=getIntVal("DEST_NETWORK_TYPE"   ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_BEARER_TYPE:
                val=getIntVal("DEST_BEARER_TYPE"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_TELEMATICS_ID:
                val=getIntVal("DEST_TELEMATICS_ID"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_ADDR_SUBUNIT:
                val=getIntVal("SOURCE_ADDR_SUBUNIT" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_NETWORK_TYPE:
                val=getIntVal("SOURCE_NETWORK_TYPE" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_BEARER_TYPE:
                val=getIntVal("SOURCE_BEARER_TYPE"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_TELEMATICS_ID:
                val=getIntVal("SOURCE_TELEMATICS_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_QOS_TIME_TO_LIVE:
                val=getIntVal("QOS_TIME_TO_LIVE"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_PAYLOAD_TYPE:
                val=getIntVal("PAYLOAD_TYPE"        ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_ADDITIONAL_STATUS_INFO_TEXT:
                sval=getCOctVal("ADDITIONAL_STATUS_INFO_TEXT",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_RECEIPTED_MESSAGE_ID:
                sval=getCOctVal("RECEIPTED_MESSAGE_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MS_MSG_WAIT_FACILITIES:
                val=getIntVal("MS_MSG_WAIT_FACILITIES",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_PRIVACY_INDICATOR:
                val=getIntVal("PRIVACY_INDICATOR"   ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_SUBADDRESS:
                sval=getOctVal("SOURCE_SUBADDRESS"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_SUBADDRESS:
                sval=getOctVal("DEST_SUBADDRESS"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_USER_MESSAGE_REFERENCE:
                val=getIntVal("USER_MESSAGE_REFERENCE",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_USER_RESPONSE_CODE:
                val=getIntVal("USER_RESPONSE_CODE"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_PORT:
                val=getIntVal("SOURCE_PORT"         ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DESTINATION_PORT:
                val=getIntVal("DESTINATION_PORT"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SAR_MSG_REF_NUM:
                val=getIntVal("SAR_MSG_REF_NUM"     ,parameter_tag,parameter_pointer,length,length_pointer);
                isMultiSegment  = true;
                current_reference=val;
                break;
            case OPT_LANGUAGE_INDICATOR:
                val=getIntVal("LANGUAGE_INDICATOR"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SAR_TOTAL_SEGMENTS:
                val=getIntVal("SAR_TOTAL_SEGMENTS"  ,parameter_tag,parameter_pointer,length,length_pointer);
                isMultiSegment  = true;
                current_segments=val;
                break;
            case OPT_SAR_SEGMENT_SEQNUM:
                val=getIntVal("SAR_SEGMENT_SEQNUM"  ,parameter_tag,parameter_pointer,length,length_pointer);
                isMultiSegment  = true;
                current_segment_number=val;
                break;
            case OPT_SC_INTERFACE_VERSION:
                val=getIntVal("SC_INTERFACE_VERSION",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_CALLBACK_NUM_PRES_IND:
                val=getIntVal("CALLBACK_NUM_PRES_IND",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_CALLBACK_NUM_ATAG:
                sval=getOctVal("CALLBACK_NUM_ATAG"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_NUMBER_OF_MESSAGES:
                val=getIntVal("NUMBER_OF_MESSAGES"  ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_CALLBACK_NUM:
                sval=getOctVal("CALLBACK_NUM"       ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DPF_RESULT:
                val=getIntVal("DPF_RESULT"          ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SET_DPF:
                val=getIntVal("SET_DPF"             ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MS_AVAILABILITY_STATUS:
                val=getIntVal("MS_AVAILABILITY_STATUS",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_NETWORK_ERROR_CODE:
                sval=getOctVal("NETWORK_ERROR_CODE" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MESSAGE_PAYLOAD:
                sval=getOctVal("MESSAGE_PAYLOAD"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DELIVERY_FAILURE_REASON:
                val=getIntVal("DELIVERY_FAILURE_REASON",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MORE_MESSAGES_TO_SEND:
                val=getIntVal("MORE_MESSAGES_TO_SEND",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MESSAGE_STATE:
                val=getIntVal("MESSAGE_STATE"       ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_USSD_SERVICE_OP:
                val=getIntVal("USSD_SERVICE_OP"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DISPLAY_TIME:
                val=getIntVal("DISPLAY_TIME"        ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SMS_SIGNAL:
                val=getIntVal("SMS_SIGNAL"          ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_MS_VALIDITY:
                val=getIntVal("MS_VALIDITY"         ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_ALERT_ON_MESSAGE_DELIVERY:
                val=getIntVal("ALERT_ON_MESSAGE_DELIVERY",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_ITS_REPLY_TYPE:
                val=getIntVal("ITS_REPLY_TYPE"      ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_ITS_SESSION_INFO:
                sval=getOctVal("ITS_SESSION_INFO"   ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            // SMPP 5.0
            case OPT_CONGESTION_STATE:
                val=getIntVal("CONGESTION_STATE"    ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_CHANNEL_INDICATOR:
                val=getIntVal("BROADCAST_CHANNEL_INDICATOR" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_CONTENT_TYPE:
                sval=getOctVal("BROADCAST_CONTENT_TYPE"     ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_CONTENT_TYPE_INFO:
                sval=getOctVal("BROADCAST_CONTENT_TYPE_INFO",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_MESSAGE_CLASS:
                val=getIntVal("BROADCAST_MESSAGE_CLASS" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_REP_NUM:
                val=getIntVal("BROADCAST_REP_NUM" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_FREQUENCY_INTERVAL:
                sval=getOctVal("BROADCAST_FREQUENCY_INTERVAL",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_AREA_IDENTIFIER:
                sval=getOctVal("BROADCAST_AREA_IDENTIFIER"   ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_ERROR_STATUS:
                val=getIntVal("BROADCAST_ERROR_STATUS" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_AREA_SUCCESS:
                val=getIntVal("BROADCAST_AREA_SUCCESS" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_END_TIME:
                sval=getCOctVal("BROADCAST_END_TIME",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BROADCAST_SERVICE_GROUP:
                sval=getOctVal("BROADCAST_SERVICE_GROUP",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_BILLING_IDENTIFICATION:
                sval=getOctVal("BILLING_IDENTIFICATION",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_NETWORK_ID:
                sval=getCOctVal("SOURCE_NETWORK_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_NETWORK_ID:
                sval=getCOctVal("DEST_NETWORK_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_SOURCE_NODE_ID:
                sval=getOctVal("SOURCE_NODE_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_NODE_ID:
                sval=getOctVal("DEST_NODE_ID",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_ADDR_NP_RESOLUTION:
                val=getIntVal("DEST_ADDR_NP_RESOLUTION" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_ADDR_NP_INFORMATION:
                sval=getOctVal("DEST_ADDR_NP_INFORMATION",parameter_tag,parameter_pointer,length,length_pointer);
                break;
            case OPT_DEST_ADDR_NP_COUNTRY:
                val=getIntVal("DEST_ADDR_NP_COUNTRY" ,parameter_tag,parameter_pointer,length,length_pointer);
                break;
            default:
                sval=getOctVal("UNKNOWN",parameter_tag,parameter_pointer,length,length_pointer);
                break;
        }
    }
    
    /**
     * 
     * @param name
     * @param parameter_tag
     * @param parameter_pointer
     * @param length
     * @param length_pointer
     * @return
     * @throws Exception
     */
    private int getIntVal(String name,int parameter_tag,int parameter_pointer, int length,int length_pointer) throws Exception
    {
        int val_pointer=pointer;
        int val=ConvertLib.getIntFromByteArray(this.body, pointer  ,length);
        pointer=pointer+length;
        XTTProperties.printDebug(function+": OptionalParameter: \n"
            +"\n  "+name
            +" parameter_tag=0x"+ConvertLib.outputBytes(this.body,parameter_pointer,2     ,2)+"="+parameter_tag
            +" length="       +ConvertLib.outputBytes(this.body,length_pointer   ,2     ,2)+"="+length
            +" value="       +ConvertLib.outputBytes(this.body,val_pointer      ,length,2)+"="+val
            +"\n");
        if(storeVar!=null)
        {
            for(int i=0;i<storeVar.length;i++)
            {
                XTTProperties.setVariable(storeVar[i]+"/"+name,val+"");
            }
        }
        return val;
    }
    
    /**
     * 
     * @param name
     * @param parameter_tag
     * @param parameter_pointer
     * @param length
     * @param length_pointer
     * @return
     * @throws Exception
     */
    private String getCOctVal(String name,int parameter_tag,int parameter_pointer, int length,int length_pointer) throws Exception
    {
        int val_pointer=pointer;
        String sval=getStringFromCOctetByteArray(length);
        //pointer=pointer+sval.length()+1;
        XTTProperties.printDebug(function+": OptionalParameter: \n"
            +"\n  "+name
            +" parameter_tag=0x"+ConvertLib.outputBytes(this.body,parameter_pointer,2     ,2)+"="+parameter_tag
            +" length="       +ConvertLib.outputBytes(this.body,length_pointer   ,2     ,2)+"="+length+" ->"+(sval.length()+1)
            //+"\nvalue="       +ConvertLib.outputBytes(this.body,val_pointer      ,length,2)+"="+sval
            +"\n"+ConvertLib.getHexView(body,val_pointer,val_pointer+length)+"\n");
        if(storeVar!=null)
        {
            for(int i=0;i<storeVar.length;i++)
            {
                XTTProperties.setVariable(storeVar[i]+"/"+name,sval+"");
            }
        }
        return sval;
    }
    
    /**
     * 
     * @param name
     * @param parameter_tag
     * @param parameter_pointer
     * @param length
     * @param length_pointer
     * @return
     * @throws Exception
     */
    private String getOctVal(String name,int parameter_tag,int parameter_pointer, int length,int length_pointer) throws Exception
    {
        int val_pointer=pointer;
        String sval=getStringFromOctetByteArray(length);
        //pointer=pointer+sval.length();
        XTTProperties.printDebug(function+": OptionalParameter: \n"
            +"\n  "+name
            +" parameter_tag =0x"+ConvertLib.outputBytes(this.body,parameter_pointer,2     ,2)+"="+parameter_tag
            +" length ="       +ConvertLib.outputBytes(this.body,length_pointer   ,2     ,2)+"="+length
            //+"\nvalue ="       +ConvertLib.outputBytes(this.body,val_pointer      ,length,2)+"="+sval
            +"\n"+ConvertLib.getHexView(body,val_pointer,val_pointer+length));
        if(storeVar!=null)
        {
            String theval=sval;
            byte[] bval=new byte[length];
            ConvertLib.addBytesToArray(bval,0,this.body,val_pointer,length);
            if(parameter_tag==OPT_MESSAGE_PAYLOAD)theval=ConvertLib.base64Encode(bval);
            for(int i=0;i<storeVar.length;i++)
            {
                XTTProperties.setVariable(storeVar[i]+"/"+name,theval+"");
            }
        }
        if(parameter_tag==OPT_MESSAGE_PAYLOAD)
        {
            payload=new byte[length];
            int j=0;
            for(int i=val_pointer;i<val_pointer+length;i++)
            {
                payload[j++]=this.body[i];
            }
        }
        return sval;
    }
}
