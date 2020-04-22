package com.mobixell.xtt;

public class MMSDecoder implements MMSConstants
{
    private byte[] body = null;
    private int pointer = 0;
    private int endpointer=0;
    private String[] storeVar=null;
    private static int PAD=23;
    private String content_type      = null;

    public String getStringFromCOctetByteArray(int max)
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
    }
    public String getStringFromOctetByteArray(int length)
    {
        String received=ConvertLib.getStringFromOctetByteArray(this.body,pointer,length);
        pointer=pointer+length;
        return received;
    }
    public int getIntFromByteArray(int octets) throws Exception
    {
        int retval=ConvertLib.getIntFromByteArray(this.body, pointer  ,octets);
        pointer=pointer+octets;
        return retval;
    }

    public int getValueLength() throws Exception
    {
        int length=getIntFromByteArray(1);
        if(length==31)
        {
            return uintVar();
        } else
        {
            return length;
        }
    }
    public int getIntegerValueFromByteArray() throws Exception
    {
        int length=getIntFromByteArray(1);
        if(length>=128)
        {
            return length-128;
        } else
        {
            return getIntFromByteArray(length);
        }
    }
    private void setVariable(String storePoint,String storeValue)
    {
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+storePoint,storeValue);
        }
    }

    public int uintVar() throws Exception
    {
        int length=0;
        int temp=0;
        for (int i=0;i<5;i++)
        {
            length=length<<7;
            temp=getIntFromByteArray(1);
            if(temp<0x80)
            {
                return length+temp;
            } else
            {
                temp=temp-0x80;
            }
            length=length+temp;
            //System.out.println("length="+length+" temp="+temp);
        }
        return length;

    }

    public MMSDecoder(byte[] body, int startpointer,int endpointer, String storeVar)
    {
        this(body,startpointer,endpointer,new String[]{storeVar});
    }
    public MMSDecoder(byte[] body, int startpointer,int endpointer, String[] storeVar)
    {
        this.body=body;
        this.pointer=startpointer;
        this.endpointer=endpointer;
        this.storeVar=storeVar;
    }

    public MMSDecoder(byte[] body, int startpointer, String[] storeVar)
    {
        this(body, startpointer, body.length, storeVar);
    }
    public MMSDecoder(byte[] body, int startpointer, String storeVar)
    {
        this(body, startpointer, body.length, storeVar);
    }

    private void decodeContentType(String insert, StringBuffer output) throws Exception
    {
        int length=0;
        int code=getIntFromByteArray(1);
        pointer--;
        if(code<0x20)
        {
            length=getValueLength();
            int endpointer=pointer+length;
            StringBuffer parameters=new StringBuffer("");
            code=getIntFromByteArray(1);
            if(code>=0x80)
            {
                content_type=WSPDecoder.getWellKnownContentType(code-0x80);
            } else
            {
                pointer--;
                content_type=getStringFromCOctetByteArray(0);
            }
            
            while(pointer<endpointer)
            {
                parameters.append("; "+decodeContentTypeParameter());
            }

            setVariable("/MMS/"+insert+"content_type",""+content_type+""+parameters);
            output.append("\n "+ConvertLib.createString(insert+"content_type",PAD)+": "+content_type+""+parameters);
        }else if(code>=0x20&&code<0x80)
        {
            content_type=getStringFromCOctetByteArray(body.length-pointer);
            setVariable("/MMS/"+insert+"content_type",""+content_type );
            output.append("\n "+ConvertLib.createString(insert+"content_type",PAD)+": "+content_type);
        } else
        {
            content_type=WSPDecoder.getWellKnownContentType(getIntFromByteArray(1)-0x80);
            setVariable("/MMS/"+insert+"content_type",""+content_type );
            output.append("\n "+ConvertLib.createString(insert+"content_type",PAD)+": "+content_type);
        }
    }
    
    private String decodeContentTypeParameter() throws Exception
    {
        int parameter=getIntFromByteArray(1);
        if(parameter>=0x80)
        {
            parameter=parameter-0x80;
        } else
        {
            parameter=-1;
            pointer--;
        }
        String name=WSPDecoder.getWellKnownParameter(parameter);
        int intval=0;
        String value=null;
        switch(parameter)
        {
            case 0x00: // q                     Q-value             
                intval=uintVar();
                return name+"=\""+intval+"\"";
            case 0x01: // charset               Well-known-charset  
                intval=getIntFromByteArray(1);
                if(intval>=0x80)
                {
                    return name+"=\""+WSPDecoder.getCharset(intval-0x80)+"\"";
                } else
                {
                    pointer--;
                    value=getStringFromCOctetByteArray(0);
                    if(!value.startsWith("\""))
                    {
                        return name+"=\""+value+"\"";
                    } else
                    {
                        return name+"="+value+"\"";
                    }
                }
            case 0x02: // level                 Version-value       
                intval=getIntFromByteArray(1);
                if(intval>=0x80)
                {
                    return name+"=\""+"0x"+ConvertLib.outputBytes(ConvertLib.getByteArrayFromInt(intval-0x80))+"\"";
                } else
                {
                    pointer--;
                    value=getStringFromCOctetByteArray(0);
                    if(!value.startsWith("\""))
                    {
                        return name+"=\""+value+"\"";
                    } else
                    {
                        return name+"="+value+"\"";
                    }
                }
            case 0x09: // type                  Constrained-encoding
                intval=getIntFromByteArray(1);
                if(intval>=0x80)
                {
                    return name+"=\""+WSPDecoder.getWellKnownContentType(intval-0x80)+"\"";
                } else
                {
                    pointer--;
                    value=getStringFromCOctetByteArray(0);
                    if(!value.startsWith("\""))
                    {
                        return name+"=\""+value+"\"";
                    } else
                    {
                        return name+"="+value+"\"";
                    }
                }
            case 0x03: // type                  Integer-value       
            case 0x16: // size                  Integer-value       
                intval=getIntegerValueFromByteArray();
                return name+"=\""+intval+"\"";
            case 0x07: // differences           Field-name          
                intval=getIntFromByteArray(1);
                if(intval>=0x80)
                {
                    return name+"=\""+intval+"\"";
                } else
                {
                    pointer--;
                    value=getStringFromCOctetByteArray(0);
                    if(!value.startsWith("\""))
                    {
                        return name+"=\""+value+"\"";
                    } else
                    {
                        return name+"="+value+"\"";
                    }
                }
            case 0x08: // padding               Short-integer       
            case 0x11: // sec                   Short-integer       
                intval=getIntegerValueFromByteArray();
                return name+"=\""+intval+"\"";
            case 0x0E: // max-age               Delta-seconds-value 
                intval=getIntegerValueFromByteArray();
                return name+"=\""+intval+"\"";
            case 0x10: // secure                No-value            
                intval=getIntFromByteArray(1);
                return name+"=\"\"";
            case 0x13: // creation-date         Date-value          
            case 0x14: // modification-date     Date-value          
            case 0x15: // read-date             Date-value          
                intval=getIntegerValueFromByteArray();
                return name+"=\""+intval+"\"";
            case 0x05: // name1                 Text-string         
            case 0x06: // filename1             Text-string         
            case 0x0A: // start                 Text-string         
            case 0x0B: // start-info            Text-string         
            case 0x0C: // comment1              Text-string         
            case 0x0D: // domain1               Text-string         
            case 0x0F: // path1                 Text-string         
                value=getStringFromCOctetByteArray(0);
                if(!value.startsWith("\""))
                {
                    return name+"=\""+value+"\"";
                } else
                {
                    return name+"="+value+"\"";
                }
            case 0x12: // mac                   Text-value          
            case 0x17: // name                  Text-value          
            case 0x18: // filename              Text-value          
            case 0x19: // start                 Text-value          
            case 0x1A: // start-info            Text-value          
            case 0x1B: // comment               Text-value          
            case 0x1C: // domain                Text-value          
            case 0x1D: // path                  Text-value          
                intval=getIntFromByteArray(1);
                if(intval==0)
                {
                    return name+"=\"\"";
                } else
                {
                    pointer--;
                    value=getStringFromCOctetByteArray(0);
                    if(!value.startsWith("\""))
                    {
                        return name+"=\""+value+"\"";
                    } else
                    {
                        return name+"="+value+"\"";
                    }
                }
            case -1:   // 
            default:   // unknown
                name=getStringFromCOctetByteArray(0);
                value=getStringFromCOctetByteArray(0);
                if(!value.startsWith("\""))
                {
                    return name+"=\""+value+"\"";
                } else
                {
                    return name+"="+value+"\"";
                }
        }
    }

    public void decode() throws Exception
    {
        StringBuffer output=new StringBuffer();
        try
        {
        String content="";
        int value=-1;
        int code=-1;
        int length=-1;
        int to_count=0;
        int cc_count=0;
        int bcc_count=0;
        StringBuffer to_list=new StringBuffer("");
        StringBuffer cc_list=new StringBuffer("");
        StringBuffer bcc_list=new StringBuffer("");
        String headername=null;
        boolean endofheaders=false;
        String charset="";
        while(pointer<endpointer&&!endofheaders)
        {
            charset="";
            code=getIntFromByteArray(1);
            pointer--;
            if(code<0x20)
            {
                    output.append("\n   unsupported mms skipping "+code+" bytes");
                    pointer=pointer+code+1;
                    return;

            } else if(code>=0x20&&code<0x80)
            {
                String headname=getStringFromCOctetByteArray(0);
                String headval=getStringFromCOctetByteArray(0);
                setVariable("/MMS/"+headname,headval);
                output.append("\n "+ConvertLib.createString(headname,PAD)+": "+headval);
            } else
            {
                code=getIntFromByteArray(1)-0x80;
                headername=getX_HEADER(code).toLowerCase();
                switch(code)
                {
                // TODO: IMPROVE CONTENT TYPE, to much hardcoded ATM
                    case MMS_CONTENT_TYPE:
                        decodeContentType("",output);
                        // Content type is supposed to be the last entry
                        endofheaders=true;
                        break;
                // TODO: IMPROVE FROM
                    case MMS_FROM:
                        length=getValueLength();
                        value=getIntFromByteArray(1);
                        if(value==128)
                        {
                            //SAME as Encoded string, currently only support well known charset
                            code=getIntFromByteArray(1);
                            pointer--;
                            if(code<0x20)
                            {
                                if(code==0x1F)
                                {
                                    length=uintVar();
                                } else
                                {
                                    length=getIntFromByteArray(1);
                                }
                                charset=WSPDecoder.getCharset(getIntFromByteArray(1)-0x80);
                                content=getStringFromCOctetByteArray(0);
                                setVariable("/MMS/"+headername,content);
                                setVariable("/MMS/"+headername+"/CHARSET",charset);
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content+" (/charset="+charset+")");
                            }else if(code>=0x20&&code<0x80)
                            {
                                content=getStringFromCOctetByteArray(0);
                                setVariable("/MMS/"+headername,""+content);
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                            }
                        } else if(value==129)
                        {
                            setVariable("/MMS/"+headername,"(Insert-address-token)" );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": (Insert-address-token)");
                        } else
                        {
                            content=getStringFromCOctetByteArray(length-1);
                            setVariable("/MMS/"+headername,"(Invalid)" );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": (Invalid)");
                        }
                        break;
                        
                // TODO: IMPROVE ENCODED STRING, not perfect yet, only suports well known charsets
                    case MMS_BCC:
                    case MMS_CC:
                    case MMS_TO:
                        int thisCode=code;
                        code=getIntFromByteArray(1);
                        pointer--;
                        if(code==0)
                        {
                            pointer++;
                            content="";
                        } else if(code<0x20)
                        {
                            if(code==0x1F)
                            {
                                length=uintVar();
                            } else
                            {
                                length=getIntFromByteArray(1);
                            }
                            charset=WSPDecoder.getCharset(getIntFromByteArray(1)-0x80);
                            content=getStringFromCOctetByteArray(0);
                        }else if(code>=0x20&&code<0x80)
                        {
                            content=getStringFromCOctetByteArray(0);
                        }
                        int num=0;
                        String val=content;
                        switch(thisCode)
                        {
                            case MMS_BCC:
                                if(!bcc_list.toString().equals(""))
                                {
                                    bcc_list.append(";");
                                }
                                bcc_list.append(content);
                                num=bcc_count++;
                                val=bcc_list.toString();
                                break;
                            case MMS_CC:
                                if(!cc_list.toString().equals(""))
                                {
                                    cc_list.append(";");
                                }
                                cc_list.append(content);
                                num=cc_count++;
                                val=cc_list.toString();
                                break;
                            case MMS_TO:
                                if(!to_list.toString().equals(""))
                                {
                                    to_list.append(";");
                                }
                                to_list.append(content);
                                num=to_count++;
                                val=to_list.toString();
                                break;
                        }
                        setVariable("/MMS/"+headername+"/length",""+(num+1));
                        setVariable("/MMS/"+headername+"/"+num,""+content);
                        setVariable("/MMS/"+headername,""+val);
                        setVariable("/MMS/"+headername+"/CHARSET",charset);
                        setVariable("/MMS/"+headername+"/"+num+"/CHARSET/",charset);
                        output.append("\n "+ConvertLib.createString(headername+"/"+num,PAD)+": "+content);
                        if(!charset.equals("")){output.append(" (/charset="+charset+")");}
                        break;
                    case MMS_SUBJECT:
                    case MMS_X_MMS_STATUS_TEXT:
                    case MMS_X_MMS_RETRIEVE_TEXT:
                    case MMS_X_MMS_RESPONSE_TEXT:
                    case MMS_X_MMS_STORE_STATUS_TEXT:
                    case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT:
                        code=getIntFromByteArray(1);
                        pointer--;
                        if(code==0)
                        {
                            pointer++;
                            content="";
                            setVariable("/MMS/"+headername,""+content);
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        } else if(code<0x20)
                        {
                            if(code==0x1F)
                            {
                                length=uintVar();
                            } else
                            {
                                length=getIntFromByteArray(1);
                            }
                            charset=WSPDecoder.getCharset(getIntFromByteArray(1)-0x80);
                            content=getStringFromCOctetByteArray(0);
                            setVariable("/MMS/"+headername,content);
                            setVariable("/MMS/"+headername+"/CHARSET",charset);
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content+"(/charset="+charset+")");
                        }else if(code>=0x20&&code<0x80)
                        {
                            content=getStringFromCOctetByteArray(0);
                            setVariable("/MMS/"+headername,""+content);
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        }
                        break;
                //TEXT-STRING
                    case MMS_MESSAGE_ID:
                    case MMS_X_MMS_CONTENT_LOCATION:
                    case MMS_X_MMS_TRANSACTION_ID:
                    case MMS_X_MMS_REPLY_CHARGING_ID:
                    case MMS_X_MMS_APPLIC_ID:
                    case MMS_X_MMS_REPLY_APPLIC_ID:
                    case MMS_X_MMS_AUX_APPLIC_INFO:
                    case MMS_X_MMS_REPLACE_ID:
                    case MMS_X_MMS_CANCEL_ID:
                        content=getStringFromCOctetByteArray(0);
                        setVariable("/MMS/"+headername,""+content);
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                //boolean
                    case MMS_X_MMS_REPORT_ALLOWED:
                    case MMS_X_MMS_READ_REPORT:
                    case MMS_X_MMS_DELIVERY_REPORT:
                    case MMS_X_MMS_STORE:
                    case MMS_X_MMS_STORED:
                    case MMS_X_MMS_TOTALS:
                    case MMS_X_MMS_QUOTAS:
                    case MMS_X_MMS_DISTRIBUTION_INDICATOR:
                    case MMS_X_MMS_DRM_CONTENT:
                    case MMS_X_MMS_ADAPTATION_ALLOWED:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Yes";break;
                            case 129: content="No"; break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                //INTEGER
                    case MMS_X_MMS_START:
                    case MMS_X_MMS_MESSAGE_COUNT:
                    case MMS_X_MMS_LIMIT:
                        value=getIntegerValueFromByteArray();
                        content=""+value;
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                // LONG INTEGER
                    case MMS_DATE:
                    case MMS_X_MMS_MESSAGE_SIZE:
                    case MMS_X_MMS_REPLY_CHARGING_SIZE:
                        length=getIntFromByteArray(1);
                        value=getIntFromByteArray(length);
                        setVariable("/MMS/"+headername,""+value );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+value);
                        break;
                // DATE-relative-absolute
                    case MMS_X_MMS_REPLY_CHARGING_DEADLINE:
                    case MMS_X_MMS_DELIVERY_TIME:
                    case MMS_X_MMS_EXPIRY:
                        length=getValueLength();
                        value=getIntFromByteArray(1);
                        if(value==128)
                        {
                            length=getIntFromByteArray(1);
                            value=getIntFromByteArray(length);
                            setVariable("/MMS/"+headername,""+value );
                            setVariable("/MMS/"+headername+"/type","Absolute" );
                            setVariable("/MMS/"+headername+"_type","Absolute" );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+value+" (/type=Absolute)");
                        } else if(value==129)
                        {
                            length=getIntFromByteArray(1);
                            value=getIntFromByteArray(length);
                            setVariable("/MMS/"+headername,""+value );
                            setVariable("/MMS/"+headername+"/type","Relative" );
                            setVariable("/MMS/"+headername+"_type","Relative" );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+value+" (/type=Relative)");
                        } else
                        {
                            length=getIntFromByteArray(1);
                            value=getIntFromByteArray(length);
                            setVariable("/MMS/"+headername,""+value );
                            setVariable("/MMS/"+headername+"/type","Invalid" );
                            setVariable("/MMS/"+headername+"_type","Invalid" );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+value+" (/type=Invalid)");
                        }
                        break;
                // Single bytes
                    case MMS_X_MMS_RETRIEVE_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Ok";                                 break;
                            case 192: content="Error-transient-failure";            break;
                            case 193: content="Error-transient-message-not-found";  break;
                            case 194: content="Error-transient-network-problem";    break;
                            case 224: content="Error-permanent-failure";            break;
                            case 225: content="Error-permanent-service-denied";     break;
                            case 226: content="Error-permanent-message-not-found";  break;
                            case 227: content="Error-permanent-content-unsupported";break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_READ_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Read";                       break;
                            case 129: content="Deleted without being read"; break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_REPLY_CHARGING:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Requested";              break;
                            case 129: content="Requested text only";    break;
                            case 130: content="Accepted";               break;
                            case 131: content="Accepted text only";     break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_MM_STATE:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Draft";          break;
                            case 129: content="Sent";           break;
                            case 130: content="New";            break;
                            case 131: content="Retrieved";      break;
                            case 132: content="Forwarded";      break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_STORE_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Success";                                break;
                            case 192: content="Error-transient-failure";                break;
                            case 193: content="Error-transient-network-problem";        break;
                            case 224: content="Error-permanent-failure";                break;
                            case 225: content="Error-permanent-service-denied";         break;
                            case 226: content="Error-permanent-message-format-corrupt"; break;
                            case 227: content="Error-permanent-message-not-found";      break;
                            case 228: content="Error-permanent-mmbox-full";             break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Manual";             break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_CONTENT_CLASS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="text";           break;
                            case 129: content="image-basic";    break;
                            case 130: content="image-rich";     break;
                            case 131: content="video-basic";    break;
                            case 132: content="video-rich";     break;
                            case 133: content="megapixel";      break;
                            case 134: content="content-basic";  break;
                            case 135: content="content-rich";   break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_CANCEL_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Cancel Request Successfully received";   break;
                            case 129: content="Cancel Request corrupted";               break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+headername,""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                // TODO: IMPROVE older single
                    case MMS_X_MMS_MESSAGE_CLASS:
                        code=getIntFromByteArray(1);
                        pointer--;
                        if(code<0x20)
                        {
                            length=getIntFromByteArray(1);
                            output.append("\n unsupported x_mms_message_class "+length+" bytes");
                            pointer=pointer+length;
                        }else if(code>=0x20&&code<0x80)
                        {
                            content=getStringFromCOctetByteArray(0);
                            setVariable("/MMS/"+"x_mms_message_class",""+content );
                                    output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        } else
                        {
                            value=getIntFromByteArray(1);
                            if(value==128)
                            {
                                setVariable("/MMS/"+"x_mms_message_class","Personal" );
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": Personal");
                            } else if(value==129)
                            {
                                setVariable("/MMS/"+"x_mms_message_class","Advertisement" );
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": Advertisement");
                            } else if(value==130)
                            {
                                setVariable("/MMS/"+"x_mms_message_class","Informational" );
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": Informational");
                            } else if(value==131)
                            {
                                setVariable("/MMS/"+"x_mms_message_class","Auto" );
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": Auto");
                            } else
                            {
                                content=getStringFromCOctetByteArray(length-1);
                                setVariable("/MMS/"+"x_mms_message_class","Invalid" );
                                output.append("\n "+ConvertLib.createString(headername,PAD)+": Invalid");
                            }
                        }
                        break;
                    case MMS_X_MMS_MESSAGE_TYPE:
                        value=getIntFromByteArray(1);
                        content=getX_MMS_MESSAGE_TYPE(value);
                        setVariable("/MMS/"+"x_mms_message_type",""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_MMS_VERSION:
                        value=getIntFromByteArray(1)-0x80;
                        setVariable("/MMS/"+"x_mms_mms_version","0x"+ConvertLib.outputBytes(ConvertLib.getByteArrayFromInt(value)));
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": 0x"+ConvertLib.outputBytes(ConvertLib.getByteArrayFromInt(value)));
                        break;
                    case MMS_X_MMS_PRIORITY:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Low";   break;
                            case 129: content="Normal";break;
                            case 130: content="High";  break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+"x_mms_priority",""+content );
                            output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_RESPONSE_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Ok";                                                 break;
                            case 129: content="Error-unspecified";                                  break;
                            case 130: content="Error-service-denied";                               break;
                            case 131: content="Error-message-format-corrupt";                       break;
                            case 132: content="Error-sending-address-unresolved";                   break;
                            case 133: content="Error-message-not-found";                            break;
                            case 134: content="Error-network-problem";                              break;
                            case 135: content="Error-content-not-accepted";                         break;
                            case 136: content="Error-unsupported-message";                          break;
                            case 192: content="Error-transient-failure";                            break;
                            case 193: content="Error-transient-sending-address-unresolved";         break;
                            case 194: content="Error-transient-message-not-found";                  break;
                            case 195: content="Error-transient-network-problem";                    break;
                            case 196: content="Error-transient-partial-success";                    break;
                            case 224: content="Error-permanent-failure";                            break;
                            case 225: content="Error-permanent-service-denied";                     break;
                            case 226: content="Error-permanent-message-format-corrupt";             break;
                            case 227: content="Error-permanent-sending-address-unresolved";         break;
                            case 228: content="Error-permanent-content-not-accepted";               break;
                            case 229: content="Error-permanent-content-not-accepted";               break;
                            case 230: content="Error-permanent-reply-charging-limitations-not-met"; break;
                            case 231: content="Error-permanent-reply-charging-request-not-accepted";break;
                            case 232: content="Error-permanent-reply-charging-forwarding-denied";   break;
                            case 233: content="Error-permanent-reply-charging-not-supported";       break;
                            case 234: content="Error-permanent-address-hiding-not-supported";       break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+"x_mms_response_status",""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_SENDER_VISIBILITY:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Hide";break;
                            case 129: content="Show";break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+"x_mms_response_text",""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;
                    case MMS_X_MMS_STATUS:
                        value=getIntFromByteArray(1);
                        content="";
                        switch(value)
                        {
                            case 128: content="Expired";        break;
                            case 129: content="Retrieved";      break;
                            case 130: content="Rejected";       break;
                            case 131: content="Deferred";       break;
                            case 132: content="Unrecognised";   break;
                            case 133: content="Indeterminate";  break;
                            case 134: content="Forwarded";      break;
                            default:  content="Invalid("+value+")";
                        }
                        setVariable("/MMS/"+"x_mms_status",""+content );
                        output.append("\n "+ConvertLib.createString(headername,PAD)+": "+content);
                        break;

            // TODO: Rest of the headers decoded

                // LENGTH/TOKEN/INTEGER
                    case MMS_X_MMS_MBOX_TOTALS:
                        output.append("\n aborting: found unsupported X_MMS_MBOX_TOTALS");
                        return;
                    case MMS_X_MMS_MBOX_QUOTAS:
                        output.append("\n aborting: found unsupported X_MMS_MBOX_QUOTAS");
                        return;
                // SWINGLE
                    case MMS_X_MMS_MM_FLAGS:
                        output.append("\n aborting: found unsupported X_MMS_MM_FLAGS");
                        return;
                    case MMS_X_MMS_PREVIOUSLY_SENT_BY:
                        output.append("\n aborting: found unsupported X_MMS_PREVIOUSLY_SENT_BY");
                        return;
                    case MMS_X_MMS_PREVIOUSLY_SENT_DATE:
                        output.append("\n aborting: found unsupported X_MMS_PREVIOUSLY_SENT_DATE");
                        return;
                    case MMS_X_MMS_ELEMENT_DESCRIPTOR:
                        output.append("\n aborting: found unsupported X_MMS_ELEMENT_DESCRIPTOR");
                        return;
                    case MMS_X_MMS_ATTRIBUTES:
                        output.append("\n aborting: found unsupported X_MMS_ATTRIBUTES");
                        return;
         // MMS 1.3
                    case MMS_CONTENT:
                        output.append("\n aborting: found unsupported CONTENT");
                        return;
                    case MMS_ADDITIONAL_HEADERS:
                        output.append("\n aborting: found unsupported ADDITIONAL_HEADERS");
                        return;
        // Unknown
                    default:
                        output.append("\n aborting: found unknown: "+code);
                        return;
                } //END IF SWITCH
            }//END OF IF
        }//END OF WHILE
        
        // BODY
        if(endpointer-pointer>0)
        {
            if(content_type!=null&&(content_type.equals("application/vnd.wap.multipart.related")||content_type.equals("application/vnd.wap.multipart.mixed")))
            {
                output.append("\n MULTIPART BODY");
                setVariable("/MMS/"+"body",""+ConvertLib.base64Encode(body,pointer,endpointer-pointer));
                output.append("\n "+ConvertLib.createString("body",PAD)+": "+(endpointer-pointer)+" bytes (base64 encoded)");
                // Decoding Body
                int numEntries=0;
                int countEntries=0;
                numEntries=uintVar();
                if(numEntries>0)
                {
                    setVariable("/MMS/"+"body/nentries",""+numEntries);
                    output.append("\n "+ConvertLib.createString("body/nentries",PAD)+": "+numEntries+" (WAPWSP<=1.2)");
                } else
                {
                    setVariable("/MMS/"+"body",""+numEntries);
                    output.append("\n "+ConvertLib.createString("body/nentries",PAD)+": "+numEntries+" (WAPWSP>=1.3)");
                }
                int headersLen=0;
                int headersLenLeft=0;
                int dataLen=0;
                int pointerbefore=0;
                int pointerend=0;
                while(pointer<endpointer)
                {
                    headersLen=uintVar();
                    dataLen=uintVar();
                    output.append("\n BODY headers="+headersLen+" bytes body="+dataLen+" bytes");
                    pointerbefore=pointer;
                    pointerend=pointer+headersLen+dataLen;
                    decodeContentType("body/"+countEntries+"/",output);

                    headersLenLeft=headersLen-(pointer-pointerbefore);
                    pointer=pointer+headersLenLeft;
                    
                    setVariable("/MMS/"+"body/"+countEntries,""+ConvertLib.base64Encode(body,pointer,dataLen));
                    output.append("\n "+ConvertLib.createString("body/"+countEntries,PAD)+": "+(dataLen)+" bytes (base64 encoded)");
                    pointer=pointerend;
                    countEntries++;
                }
                setVariable("/MMS/"+"body/length",""+countEntries);
                output.append("\n "+ConvertLib.createString("body/length",PAD)+": "+countEntries);
            } else
            {
                setVariable("/MMS/"+"body",""+ConvertLib.base64Encode(body,pointer,endpointer-pointer));
                output.append("\n BODY");
                output.append("\n "+ConvertLib.createString("body",PAD)+": "+(endpointer-pointer)+" bytes (base64 encoded)");
            }
        } 
        } finally
        {
            XTTProperties.printDebug(this.getClass().getName()+" "+storeVar[0]+"/MMS/"+output.toString());
        }
    }

    public static String getX_MMS_MESSAGE_TYPE(int type)
    {
        switch (type)
        {
            case M_SEND_REQ:
                return "m_send_req";
            case M_SEND_CONF:
                return "m_send_conf";
            case M_NOTIFICATION_IND:
                return "m_notification_ind";
            case M_NOTIFYRESP_IND:
                return "m_notifyresp_ind";
            case M_RETRIEVE_CONF:
                return "m_retrieve_conf";
            case M_ACKNOWLEDGE_IND:
                return "m_acknowledge_ind";
            case M_DELIVERY_IND:
                return "m_delivery_ind";
            case M_READ_REC_IND:
                return "m_read_rec_ind";
            case M_READ_ORIG_IND:
                return "m_read_orig_ind";
            case M_FORWARD_REQ:
                return "m_forward_req";
            case M_FORWARD_CONF:
                return "m_forward_conf";
        // MMS 1.3
            case M_MBOX_STORE_REQ  :
                return "m_mbox_store_req";
            case M_MBOX_STORE_CONF :
                return "m_mbox_store_conf";
            case M_MBOX_VIEW_REQ   :
                return "m_mbox_view_req";
            case M_MBOX_VIEW_CONF  :
                return "m_mbox_view_conf";
            case M_MBOX_UPLOAD_REQ :
                return "m_mbox_upload_req";
            case M_MBOX_UPLOAD_CONF:
                return "m_mbox_upload_conf";
            case M_MBOX_DELETE_REQ :
                return "m_mbox_delete_req";
            case M_MBOX_DELETE_CONF:
                return "m_mbox_delete_conf";
            case M_MBOX_DESCR      :
                return "m_mbox_descr";
            case M_DELETE_REQ      :
                return "m_delete_req";
            case M_DELETE_CONF     :
                return "m_delete_conf";
            case M_CANCEL_REQ      :
                return "m_cancel_req";
            case M_CANCEL_CONF     :
                return "m_cancel_conf";
            default:
                return "unknown";
        }
    }

    public static int getX_MMS_MESSAGE_TYPE(String header)
    {
        header=header.toUpperCase().trim();
        try
        {
            int val=Integer.decode(header);
            return val;
        } catch (Exception ex)
        {}
        if(header.equals("M_SEND_REQ"))
        {
            return M_SEND_REQ;
        } else if(header.equals("M_SEND_CONF"))
        {
            return M_SEND_CONF;
        } else if(header.equals("M_NOTIFICATION_IND"))
        {
            return M_NOTIFICATION_IND;
        } else if(header.equals("M_NOTIFYRESP_IND"))
        {
            return M_NOTIFYRESP_IND;
        } else if(header.equals("M_RETRIEVE_CONF"))
        {
            return M_RETRIEVE_CONF;
        } else if(header.equals("M_ACKNOWLEDGE_IND"))
        {
            return M_ACKNOWLEDGE_IND;
        } else if(header.equals("M_DELIVERY_IND"))
        {
            return M_DELIVERY_IND;
        } else if(header.equals("M_READ_REC_IND"))
        {
            return M_READ_REC_IND;
        } else if(header.equals("M_READ_ORIG_IND"))
        {
            return M_READ_ORIG_IND;
        } else if(header.equals("M_FORWARD_REQ"))
        {
            return M_FORWARD_REQ;
        } else if(header.equals("M_FORWARD_CONF"))
        {
            return M_FORWARD_CONF;
        } else if(header.equals("M_MBOX_STORE_REQ"))
        {
            return M_MBOX_STORE_REQ;
        } else if(header.equals("M_MBOX_STORE_CONF"))
        {
            return M_MBOX_STORE_CONF;
        } else if(header.equals("M_MBOX_VIEW_REQ"))
        {
            return M_MBOX_VIEW_REQ;
        } else if(header.equals("M_MBOX_VIEW_CONF"))
        {
            return M_MBOX_VIEW_CONF;
        } else if(header.equals("M_MBOX_UPLOAD_REQ"))
        {
            return M_MBOX_UPLOAD_REQ;
        } else if(header.equals("M_MBOX_UPLOAD_CONF"))
        {
            return M_MBOX_UPLOAD_CONF;
        } else if(header.equals("M_MBOX_DELETE_REQ"))
        {
            return M_MBOX_DELETE_REQ;
        } else if(header.equals("M_MBOX_DELETE_CONF"))
        {
            return M_MBOX_DELETE_CONF;
        } else if(header.equals("M_MBOX_DESCR"))
        {
            return M_MBOX_DESCR;
        } else if(header.equals("M_DELETE_REQ"))
        {
            return M_DELETE_REQ;
        } else if(header.equals("M_DELETE_CONF"))
        {
            return M_DELETE_CONF;
        } else if(header.equals("M_CANCEL_REQ"))
        {
            return M_CANCEL_REQ;
        } else if(header.equals("M_CANCEL_CONF"))
        {
            return M_CANCEL_CONF;
        } else
        {
            return -1;
        }
    }
    public static int getX_HEADER(String header)
    {
        try
        {
            int val=Integer.decode(header);
            return val;
        } catch (Exception ex)
        {}
        header=header.toUpperCase().trim();
        if(header.equals("BCC"))
        {
            return MMS_BCC;
        } else if(header.equals("CC"))
        {
            return MMS_CC;
        } else if(header.equals("X_MMS_CONTENT_LOCATION"))
        {
            return MMS_X_MMS_CONTENT_LOCATION;
        } else if(header.equals("CONTENT_TYPE"))
        {
            return MMS_CONTENT_TYPE;
        } else if(header.equals("DATE"))
        {
            return MMS_DATE;
        } else if(header.equals("X_MMS_DELIVERY_REPORT"))
        {
            return MMS_X_MMS_DELIVERY_REPORT;
        } else if(header.equals("X_MMS_DELIVERY_TIME"))
        {
            return MMS_X_MMS_DELIVERY_TIME;
        } else if(header.equals("X_MMS_EXPIRY"))
        {
            return MMS_X_MMS_EXPIRY;
        } else if(header.equals("FROM"))
        {
            return MMS_FROM;
        } else if(header.equals("X_MMS_MESSAGE_CLASS"))
        {
            return MMS_X_MMS_MESSAGE_CLASS;
        } else if(header.equals("MESSAGE_ID"))
        {
            return MMS_MESSAGE_ID;
        } else if(header.equals("X_MMS_MESSAGE_TYPE"))
        {
            return MMS_X_MMS_MESSAGE_TYPE;
        } else if(header.equals("X_MMS_MMS_VERSION"))
        {
            return MMS_X_MMS_MMS_VERSION;
        } else if(header.equals("X_MMS_MESSAGE_SIZE"))
        {
            return MMS_X_MMS_MESSAGE_SIZE;
        } else if(header.equals("X_MMS_PRIORITY"))
        {
            return MMS_X_MMS_PRIORITY;
        } else if(header.equals("X_MMS_READ_REPORT"))
        {
            return MMS_X_MMS_READ_REPORT;
        } else if(header.equals("X_MMS_REPORT_ALLOWED"))
        {
            return MMS_X_MMS_REPORT_ALLOWED;
        } else if(header.equals("X_MMS_RESPONSE_STATUS"))
        {
            return MMS_X_MMS_RESPONSE_STATUS;
        } else if(header.equals("X_MMS_RESPONSE_TEXT"))
        {
            return MMS_X_MMS_RESPONSE_TEXT;
        } else if(header.equals("X_MMS_SENDER_VISIBILITY"))
        {
            return MMS_X_MMS_SENDER_VISIBILITY;
        } else if(header.equals("X_MMS_STATUS"))
        {
            return MMS_X_MMS_STATUS;
        } else if(header.equals("SUBJECT"))
        {
            return MMS_SUBJECT;
        } else if(header.equals("TO"))
        {
            return MMS_TO;
        } else if(header.equals("X_MMS_TRANSACTION_ID"))
        {
            return MMS_X_MMS_TRANSACTION_ID;
        } else if(header.equals("X_MMS_RETRIEVE_STATUS"))
        {
            return MMS_X_MMS_RETRIEVE_STATUS;
        } else if(header.equals("X_MMS_RETRIEVE_TEXT"))
        {
            return MMS_X_MMS_RETRIEVE_TEXT;
        } else if(header.equals("X_MMS_READ_STATUS"))
        {
            return MMS_X_MMS_READ_STATUS;
        } else if(header.equals("X_MMS_REPLY_CHARGING"))
        {
            return MMS_X_MMS_REPLY_CHARGING;
        } else if(header.equals("X_MMS_REPLY_CHARGING_DEADLINE"))
        {
            return MMS_X_MMS_REPLY_CHARGING_DEADLINE;
        } else if(header.equals("X_MMS_REPLY_CHARGING_ID"))
        {
            return MMS_X_MMS_REPLY_CHARGING_ID;
        } else if(header.equals("X_MMS_REPLY_CHARGING_SIZE"))
        {
            return MMS_X_MMS_REPLY_CHARGING_SIZE;
        } else if(header.equals("X_MMS_PREVIOUSLY_SENT_BY"))
        {
            return MMS_X_MMS_PREVIOUSLY_SENT_BY;
        } else if(header.equals("X_MMS_PREVIOUSLY_SENT_DATE"))
        {
            return MMS_X_MMS_PREVIOUSLY_SENT_DATE;
        } else if(header.equals("X_MMS_STORE"))
        {
            return MMS_X_MMS_STORE;
        } else if(header.equals("X_MMS_MM_STATE"))
        {
            return MMS_X_MMS_MM_STATE;
        } else if(header.equals("X_MMS_MM_FLAGS"))
        {
            return MMS_X_MMS_MM_FLAGS;
        } else if(header.equals("X_MMS_STORE_STATUS"))
        {
            return MMS_X_MMS_STORE_STATUS;
        } else if(header.equals("X_MMS_STORE_STATUS_TEXT"))
        {
            return MMS_X_MMS_STORE_STATUS_TEXT;
        } else if(header.equals("X_MMS_STORED"))
        {
            return MMS_X_MMS_STORED;
        } else if(header.equals("X_MMS_ATTRIBUTES"))
        {
            return MMS_X_MMS_ATTRIBUTES;
        } else if(header.equals("X_MMS_TOTALS"))
        {
            return MMS_X_MMS_TOTALS;
        } else if(header.equals("X_MMS_MBOX_TOTALS"))
        {
            return MMS_X_MMS_MBOX_TOTALS;
        } else if(header.equals("X_MMS_QUOTAS"))
        {
            return MMS_X_MMS_QUOTAS;
        } else if(header.equals("X_MMS_MBOX_QUOTAS"))
        {
            return MMS_X_MMS_MBOX_QUOTAS;
        } else if(header.equals("X_MMS_MESSAGE_COUNT"))
        {
            return MMS_X_MMS_MESSAGE_COUNT;
        } else if(header.equals("CONTENT"))
        {
            return MMS_CONTENT;
        } else if(header.equals("X_MMS_START"))
        {
            return MMS_X_MMS_START;
        } else if(header.equals("ADDITIONAL_HEADERS"))
        {
            return MMS_ADDITIONAL_HEADERS;
        } else if(header.equals("X_MMS_DISTRIBUTION_INDICATOR"))
        {
            return MMS_X_MMS_DISTRIBUTION_INDICATOR;
        } else if(header.equals("X_MMS_ELEMENT_DESCRIPTOR"))
        {
            return MMS_X_MMS_ELEMENT_DESCRIPTOR;
        } else if(header.equals("X_MMS_LIMIT"))
        {
            return MMS_X_MMS_LIMIT;
        } else if(header.equals("X_MMS_RECOMMENDED_RETRIEVAL_MODE"))
        {
            return MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE;
        } else if(header.equals("X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT"))
        {
            return MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT;
        } else if(header.equals("X_MMS_STATUS_TEXT"))
        {
            return MMS_X_MMS_STATUS_TEXT;
        } else if(header.equals("X_MMS_APPLIC_ID"))
        {
            return MMS_X_MMS_APPLIC_ID;
        } else if(header.equals("X_MMS_REPLY_APPLIC_ID"))
        {
            return MMS_X_MMS_REPLY_APPLIC_ID;
        } else if(header.equals("X_MMS_AUX_APPLIC_INFO"))
        {
            return MMS_X_MMS_AUX_APPLIC_INFO;
        } else if(header.equals("X_MMS_CONTENT_CLASS"))
        {
            return MMS_X_MMS_CONTENT_CLASS;
        } else if(header.equals("X_MMS_DRM_CONTENT"))
        {
            return MMS_X_MMS_DRM_CONTENT;
        } else if(header.equals("X_MMS_ADAPTATION_ALLOWED"))
        {
            return MMS_X_MMS_ADAPTATION_ALLOWED;
        } else if(header.equals("X_MMS_REPLACE_ID"))
        {
            return MMS_X_MMS_REPLACE_ID;
        } else if(header.equals("X_MMS_CANCEL_ID"))
        {
            return MMS_X_MMS_CANCEL_ID;
        } else if(header.equals("X_MMS_CANCEL_STATUS"))
        {
            return MMS_X_MMS_CANCEL_STATUS;
        } else
        {
            return -1;
        }
    }
    public static String getX_HEADER(int header)
    {
        switch(header)
        {
            case MMS_BCC                          :return "BCC";
            case MMS_CC                           :return "CC";
            case MMS_X_MMS_CONTENT_LOCATION       :return "X_MMS_CONTENT_LOCATION";
            case MMS_CONTENT_TYPE                 :return "CONTENT_TYPE";
            case MMS_DATE                         :return "DATE";
            case MMS_X_MMS_DELIVERY_REPORT        :return "X_MMS_DELIVERY_REPORT";
            case MMS_X_MMS_DELIVERY_TIME          :return "X_MMS_DELIVERY_TIME";
            case MMS_X_MMS_EXPIRY                 :return "X_MMS_EXPIRY";
            case MMS_FROM                         :return "FROM";
            case MMS_X_MMS_MESSAGE_CLASS          :return "X_MMS_MESSAGE_CLASS";
            case MMS_MESSAGE_ID                   :return "MESSAGE_ID";
            case MMS_X_MMS_MESSAGE_TYPE           :return "X_MMS_MESSAGE_TYPE";
            case MMS_X_MMS_MMS_VERSION            :return "X_MMS_MMS_VERSION";
            case MMS_X_MMS_MESSAGE_SIZE           :return "X_MMS_MESSAGE_SIZE";
            case MMS_X_MMS_PRIORITY               :return "X_MMS_PRIORITY";
            case MMS_X_MMS_READ_REPORT            :return "X_MMS_READ_REPORT";
            case MMS_X_MMS_REPORT_ALLOWED         :return "X_MMS_REPORT_ALLOWED";
            case MMS_X_MMS_RESPONSE_STATUS        :return "X_MMS_RESPONSE_STATUS";
            case MMS_X_MMS_RESPONSE_TEXT          :return "X_MMS_RESPONSE_TEXT";
            case MMS_X_MMS_SENDER_VISIBILITY      :return "X_MMS_SENDER_VISIBILITY";
            case MMS_X_MMS_STATUS                 :return "X_MMS_STATUS";
            case MMS_SUBJECT                      :return "SUBJECT";
            case MMS_TO                           :return "TO";
            case MMS_X_MMS_TRANSACTION_ID         :return "X_MMS_TRANSACTION_ID";
            case MMS_X_MMS_RETRIEVE_STATUS        :return "X_MMS_RETRIEVE_STATUS";
            case MMS_X_MMS_RETRIEVE_TEXT          :return "X_MMS_RETRIEVE_TEXT";
            case MMS_X_MMS_READ_STATUS            :return "X_MMS_READ_STATUS";
            case MMS_X_MMS_REPLY_CHARGING         :return "X_MMS_REPLY_CHARGING";
            case MMS_X_MMS_REPLY_CHARGING_DEADLINE:return "X_MMS_REPLY_CHARGING_DEADLINE";
            case MMS_X_MMS_REPLY_CHARGING_ID      :return "X_MMS_REPLY_CHARGING_ID";
            case MMS_X_MMS_REPLY_CHARGING_SIZE    :return "X_MMS_REPLY_CHARGING_SIZE";
            case MMS_X_MMS_PREVIOUSLY_SENT_BY     :return "X_MMS_PREVIOUSLY_SENT_BY";
            case MMS_X_MMS_PREVIOUSLY_SENT_DATE   :return "X_MMS_PREVIOUSLY_SENT_DATE";
            case MMS_X_MMS_STORE                  :return "X_MMS_STORE";
            case MMS_X_MMS_MM_STATE               :return "X_MMS_MM_STATE";
            case MMS_X_MMS_MM_FLAGS               :return "X_MMS_MM_FLAGS";
            case MMS_X_MMS_STORE_STATUS           :return "X_MMS_STORE_STATUS";
            case MMS_X_MMS_STORE_STATUS_TEXT      :return "X_MMS_STORE_STATUS_TEXT";
            case MMS_X_MMS_STORED                 :return "X_MMS_STORED";
            case MMS_X_MMS_ATTRIBUTES             :return "X_MMS_ATTRIBUTES";
            case MMS_X_MMS_TOTALS                 :return "X_MMS_TOTALS";
            case MMS_X_MMS_MBOX_TOTALS            :return "X_MMS_MBOX_TOTALS";
            case MMS_X_MMS_QUOTAS                 :return "X_MMS_QUOTAS";
            case MMS_X_MMS_MBOX_QUOTAS            :return "X_MMS_MBOX_QUOTAS";
            case MMS_X_MMS_MESSAGE_COUNT          :return "X_MMS_MESSAGE_COUNT";
            case MMS_CONTENT                      :return "CONTENT";
            case MMS_X_MMS_START                  :return "X_MMS_START";
            case MMS_ADDITIONAL_HEADERS           :return "ADDITIONAL_HEADERS";
            case MMS_X_MMS_DISTRIBUTION_INDICATOR :return "X_MMS_DISTRIBUTION_INDICATOR";
            case MMS_X_MMS_ELEMENT_DESCRIPTOR     :return "X_MMS_ELEMENT_DESCRIPTOR";
            case MMS_X_MMS_LIMIT                  :return "X_MMS_LIMIT";
            case MMS_X_MMS_STATUS_TEXT            :return "X_MMS_STATUS_TEXT";
            case MMS_X_MMS_APPLIC_ID              :return "X_MMS_APPLIC_ID";
            case MMS_X_MMS_REPLY_APPLIC_ID        :return "X_MMS_REPLY_APPLIC_ID";
            case MMS_X_MMS_AUX_APPLIC_INFO        :return "X_MMS_AUX_APPLIC_INFO";
            case MMS_X_MMS_CONTENT_CLASS          :return "X_MMS_CONTENT_CLASS";
            case MMS_X_MMS_DRM_CONTENT            :return "X_MMS_DRM_CONTENT";
            case MMS_X_MMS_ADAPTATION_ALLOWED     :return "X_MMS_ADAPTATION_ALLOWED";
            case MMS_X_MMS_REPLACE_ID             :return "X_MMS_REPLACE_ID";
            case MMS_X_MMS_CANCEL_ID              :return "X_MMS_CANCEL_ID";
            case MMS_X_MMS_CANCEL_STATUS          :return "X_MMS_CANCEL_STATUS";
            case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE     :return "X_MMS_RECOMMENDED_RETRIEVAL_MODE";
            case MMS_X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT:return "X_MMS_RECOMMENDED_RETRIEVAL_MODE_TEXT";
            default: return "invalid";
        }
    }
}