package com.mobixell.xtt;

public class WSPDecoder implements WSPConstants
{
    private byte[] body = null;
    private int pointer = 0;
    private String[] storeVar=null;
    private String decodedContentType="";
    public String getContentType(){return decodedContentType;}

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

    public WSPDecoder(byte[] body, int pointer, String storeVar)
    {
        this.body=body;
        this.pointer=pointer;
        this.storeVar=new String[]{storeVar};
    }
    public WSPDecoder(byte[] body, int pointer, String[] storeVar)
    {
        this.body=body;
        this.pointer=pointer;
        this.storeVar=storeVar;
    }

    private void setVariable(String storePoint,String storeValue)
    {
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+storePoint,storeValue);
        }
    }

    public void decode() throws Exception
    {                                       
        if(body.length<=pointer)return;
        int wspsession=getIntFromByteArray(1);
        int wsppdutype=getIntFromByteArray(1);
        int wspheader =getIntFromByteArray(1);
        setVariable("/WSP/"+"session"     ,""+wspsession );
        setVariable("/WSP/"+"pdu_type"    ,""+wsppdutype );
        setVariable("/WSP/"+"headerlength",""+wspheader  );

        int end=pointer+wspheader;
        StringBuffer output=new StringBuffer();

        switch(wsppdutype)
        {
            case WSP_PUSH:
                output.append(" -> PUSH:");

                int code=getIntFromByteArray(1);
                pointer--;

                if(code<0x20)
                {
                    int length=getIntFromByteArray(1);
                    if(length==1)
                    {
                        String content_type=getWellKnownContentType(getIntFromByteArray(1)-0x80);
                        decodedContentType=content_type;
                        setVariable("/WSP/"+"content_type",""+content_type );
                                output.append("\n   content_type             : "+content_type);
                    } else if(length==3)
                    {
                        String content_type=getWellKnownContentType(getIntFromByteArray(1)-0x80);
                        decodedContentType=content_type;
                        String charset=getCharset(getIntFromByteArray(1)-0x80);
                        setVariable("/WSP/"+"content_type",""+content_type+"; charset="+charset );
                                output.append("\n   content_type             : "+content_type+"; charset="+charset);
                    } else
                    {
                        output.append("\n   unsupported content_type skipping "+length+" bytes.");
                        pointer=pointer+length;
                    }

                }else if(code>=0x20&&code<0x80)
                {
                    String content_type=getStringFromCOctetByteArray(body.length-pointer);
                    decodedContentType=content_type;
                    setVariable("/WSP/"+"content_type",""+content_type );
                            output.append("\n   content_type             : "+content_type);
                } else
                {
                        String content_type=getWellKnownContentType(getIntFromByteArray(1)-0x80);
                        decodedContentType=content_type;
                        setVariable("/WSP/"+"content_type",""+content_type );
                                output.append("\n   content_type             : "+content_type);
                }

                int length=-1;
                while((pointer<end) && (pointer<body.length))
                {
                    code=getIntFromByteArray(1);
                    pointer--;
                    if(code<0x20)
                    {
                        length=getIntFromByteArray(1);
                        output.append("\n   unsupported wsp skipping "+length+" bytes");
                        pointer=pointer+length;

                    }else if(code>=0x20&&code<0x80)
                    {
                        String headname=getStringFromCOctetByteArray(body.length-pointer);
                        String headval=getStringFromCOctetByteArray(body.length-pointer);
                        setVariable("/WSP/"+headname,headval );
                                output.append("\n   "+headname+": "+headval);
                    } else
                    {
                        int head=getIntFromByteArray(1)-0x80;
                        switch(head)
                        {
                            case WAP_ENCODING_VERSION:
                                int encoding=getIntFromByteArray(1)-0x80;
                                setVariable("/WSP/"+"encoding_version","0x"+ConvertLib.intToHex(encoding,2)  );
                                output.append("\n   encoding_version ("+head+")    : 0x"+ConvertLib.intToHex(encoding,2));
                                break;
                            case WAP_X_WAP_APPLICATION_ID:
                                int id=getIntFromByteArray(1)-0x80;
                                setVariable("/WSP/"+"x_wap_application_id",""+id  );
                                output.append("\n   x_wap_application_id ("+head+"): "+id);
                                break;
                            case WAP_CONTENT_LENGTH:
                                int value=getIntFromByteArray(1);
                                if(value<0x80)
                                {
                                    value=getIntFromByteArray(value);
                                } else
                                {
                                    value=value-0x80;
                                }
                                setVariable("/WSP/"+"wap_content_length",""+value  );
                                output.append("\n   wap_content_length   ("+head+"): "+value);
                                break;
                            default:
                                output.append("\n   unsupported: ("+head+") ");
                                break;
                        }
                    }
                }
                if(body.length>pointer)
                {
                    int count=0;
                    payload=new byte[body.length-pointer];
                    for(int i=pointer;i<body.length;i++)
                    {
                        payload[count++]=body[i];
                    }
                } else
                {
                    payload=new byte[0];
                }
                break;
            default:
                output.append("\n WSP unsupported skipping "+wspheader+" bytes");
                pointer=pointer+wspheader;
                break;
        }
        XTTProperties.printDebug(this.getClass().getName()+".decodeWSP: head: "+wspheader+" bytes"
            +"\n session: "+wspsession
            +"\n pdutype: "+wsppdutype
            +output.toString()
            +"\n payload: "+payload.length+" bytes"
            );
    }
    private byte[] payload=new byte[0];
    public byte[] getPayload()
    {
        return payload;
    }

    public static String getWellKnownParameter(int type)
    {
        switch(type)
        {
            case 0x00: return "q";
            case 0x01: return "charset";
            case 0x02: return "level";
            case 0x03: return "type";
            case 0x05: return "name1";
            case 0x06: return "filename1";
            case 0x07: return "differences";
            case 0x08: return "padding";
            case 0x09: return "type";
            case 0x0A: return "start";
            case 0x0B: return "start-info";
            case 0x0C: return "comment1";
            case 0x0D: return "domain1";
            case 0x0E: return "max-age";
            case 0x0F: return "path1";
            case 0x10: return "secure";
            case 0x11: return "sec";
            case 0x12: return "mac";
            case 0x13: return "creation-date";
            case 0x14: return "modification-date";
            case 0x15: return "read-date";
            case 0x16: return "size";
            case 0x17: return "name";
            case 0x18: return "filename";
            case 0x19: return "start";
            case 0x1A: return "start-info";
            case 0x1B: return "comment";
            case 0x1C: return "domain";
            case 0x1D: return "path";
            default:   return "unknown";
        }
    }

    public static String getWellKnownContentType(int type)
    {
        switch(type)
        {
            case 0x00: return "*/*";
            case 0x01: return "text/*";
            case 0x02: return "text/html";
            case 0x03: return "text/plain";
            case 0x04: return "text/x-hdml";
            case 0x05: return "text/x-ttml";
            case 0x06: return "text/x-vCalendar";
            case 0x07: return "text/x-vCard";
            case 0x08: return "text/vnd.wap.wml";
            case 0x09: return "text/vnd.wap.wmlscript";
            case 0x0A: return "text/vnd.wap.wta-event";
            case 0x0B: return "multipart/*";
            case 0x0C: return "multipart/mixed";
            case 0x0D: return "multipart/form-data";
            case 0x0E: return "multipart/byterantes";
            case 0x0F: return "multipart/alternative";
            case 0x10: return "application/*";
            case 0x11: return "application/java-vm";
            case 0x12: return "application/x-www-form-urlencoded";
            case 0x13: return "application/x-hdmlc";
            case 0x14: return "application/vnd.wap.wmlc";
            case 0x15: return "application/vnd.wap.wmlscriptc";
            case 0x16: return "application/vnd.wap.wta-eventc";
            case 0x17: return "application/vnd.wap.uaprof";
            case 0x18: return "application/vnd.wap.wtls-ca-certificate";
            case 0x19: return "application/vnd.wap.wtls-user-certificate";
            case 0x1A: return "application/x-x509-ca-cert";
            case 0x1B: return "application/x-x509-user-cert";
            case 0x1C: return "image/*";
            case 0x1D: return "image/gif";
            case 0x1E: return "image/jpeg";
            case 0x1F: return "image/tiff";
            case 0x20: return "image/png";
            case 0x21: return "image/vnd.wap.wbmp";
            case 0x22: return "application/vnd.wap.multipart.*";
            case 0x23: return "application/vnd.wap.multipart.mixed";
            case 0x24: return "application/vnd.wap.multipart.form-data";
            case 0x25: return "application/vnd.wap.multipart.byteranges";
            case 0x26: return "application/vnd.wap.multipart.alternative";
            case 0x27: return "application/xml";
            case 0x28: return "text/xml";
            case 0x29: return "application/vnd.wap.wbxml";
            case 0x2A: return "application/x-x968-cross-cert";
            case 0x2B: return "application/x-x968-ca-cert";
            case 0x2C: return "application/x-x968-user-cert";
            case 0x2D: return "text/vnd.wap.si";
            case 0x2E: return "application/vnd.wap.sic";
            case 0x2F: return "text/vnd.wap.sl";
            case 0x30: return "application/vnd.wap.slc";
            case 0x31: return "text/vnd.wap.co";
            case 0x32: return "application/vnd.wap.coc";
            case 0x33: return "application/vnd.wap.multipart.related";
            case 0x34: return "application/vnd.wap.sia";
            case 0x35: return "text/vnd.wap.connectivity-xml";
            case 0x36: return "application/vnd.wap.connectivity-wbxml";
            case 0x37: return "application/pkcs7-mime";
            case 0x38: return "application/vnd.wap.hashed-certificate";
            case 0x39: return "application/vnd.wap.signed-certificate";
            case 0x3A: return "application/vnd.wap.cert-response";
            case 0x3B: return "application/xhtml+xml";
            case 0x3C: return "application/wml+xml";
            case 0x3D: return "text/css";
            case 0x3E: return "application/vnd.wap.mms-message";
            case 0x3F: return "application/vnd.wap.rollover-certificate";
            case 0x40: return "application/vnd.wap.locc+wbxml";
            case 0x41: return "application/vnd.wap.loc+xml";
            case 0x42: return "application/vnd.syncml.dm+wbxml";
            case 0x43: return "application/vnd.syncml.dm+xml";
            case 0x44: return "application/vnd.syncml.notification";
            case 0x45: return "application/vnd.wap.xhtml+xml";
            case 0x46: return "application/vnd.wv.csp.cir";
            case 0x47: return "application/vnd.oma.dd+xml";
            case 0x48: return "application/vnd.oma.drm.message";
            case 0x49: return "application/vnd.oma.drm.content";
            case 0x4A: return "application/vnd.oma.drm.rights+xml";
            case 0x4B: return "application/vnd.oma.drm.rights+wbxml";
            default:   return "unknown/unknown";
        }
    }

    public static int getWellKnownContentType(String type)
    {
        try
        {
            int val=Integer.decode(type);
            return val;
        } catch (Exception ex)
        {}
        if(type.equals("*/*")){                                                return 0x00;}
        else if(type.equals("text/*")){                                        return 0x01;}
        else if(type.equals("text/html")){                                     return 0x02;}
        else if(type.equals("text/plain")){                                    return 0x03;}
        else if(type.equals("text/x-hdml")){                                   return 0x04;}
        else if(type.equals("text/x-ttml")){                                   return 0x05;}
        else if(type.equals("text/x-vCalendar")){                              return 0x06;}
        else if(type.equals("text/x-vCard")){                                  return 0x07;}
        else if(type.equals("text/vnd.wap.wml")){                              return 0x08;}
        else if(type.equals("text/vnd.wap.wmlscript")){                        return 0x09;}
        else if(type.equals("text/vnd.wap.wta-event")){                        return 0x0A;}
        else if(type.equals("multipart/*")){                                   return 0x0B;}
        else if(type.equals("multipart/mixed")){                               return 0x0C;}
        else if(type.equals("multipart/form-data")){                           return 0x0D;}
        else if(type.equals("multipart/byterantes")){                          return 0x0E;}
        else if(type.equals("multipart/alternative")){                         return 0x0F;}
        else if(type.equals("application/*")){                                 return 0x10;}
        else if(type.equals("application/java-vm")){                           return 0x11;}
        else if(type.equals("application/x-www-form-urlencoded")){             return 0x12;}
        else if(type.equals("application/x-hdmlc")){                           return 0x13;}
        else if(type.equals("application/vnd.wap.wmlc")){                      return 0x14;}
        else if(type.equals("application/vnd.wap.wmlscriptc")){                return 0x15;}
        else if(type.equals("application/vnd.wap.wta-eventc")){                return 0x16;}
        else if(type.equals("application/vnd.wap.uaprof")){                    return 0x17;}
        else if(type.equals("application/vnd.wap.wtls-ca-certificate")){       return 0x18;}
        else if(type.equals("application/vnd.wap.wtls-user-certificate")){     return 0x19;}
        else if(type.equals("application/x-x509-ca-cert")){                    return 0x1A;}
        else if(type.equals("application/x-x509-user-cert")){                  return 0x1B;}
        else if(type.equals("image/*")){                                       return 0x1C;}
        else if(type.equals("image/gif")){                                     return 0x1D;}
        else if(type.equals("image/jpeg")){                                    return 0x1E;}
        else if(type.equals("image/tiff")){                                    return 0x1F;}
        else if(type.equals("image/png")){                                     return 0x20;}
        else if(type.equals("image/vnd.wap.wbmp")){                            return 0x21;}
        else if(type.equals("application/vnd.wap.multipart.*")){               return 0x22;}
        else if(type.equals("application/vnd.wap.multipart.mixed")){           return 0x23;}
        else if(type.equals("application/vnd.wap.multipart.form-data")){       return 0x24;}
        else if(type.equals("application/vnd.wap.multipart.byteranges")){      return 0x25;}
        else if(type.equals("application/vnd.wap.multipart.alternative")){     return 0x26;}
        else if(type.equals("application/xml")){                               return 0x27;}
        else if(type.equals("text/xml")){                                      return 0x28;}
        else if(type.equals("application/vnd.wap.wbxml")){                     return 0x29;}
        else if(type.equals("application/x-x968-cross-cert")){                 return 0x2A;}
        else if(type.equals("application/x-x968-ca-cert")){                    return 0x2B;}
        else if(type.equals("application/x-x968-user-cert")){                  return 0x2C;}
        else if(type.equals("text/vnd.wap.si")){                               return 0x2D;}
        else if(type.equals("application/vnd.wap.sic")){                       return 0x2E;}
        else if(type.equals("text/vnd.wap.sl")){                               return 0x2F;}
        else if(type.equals("application/vnd.wap.slc")){                       return 0x30;}
        else if(type.equals("text/vnd.wap.co")){                               return 0x31;}
        else if(type.equals("application/vnd.wap.coc")){                       return 0x32;}
        else if(type.equals("application/vnd.wap.multipart.related")){         return 0x33;}
        else if(type.equals("application/vnd.wap.sia")){                       return 0x34;}
        else if(type.equals("text/vnd.wap.connectivity-xml")){                 return 0x35;}
        else if(type.equals("application/vnd.wap.connectivity-wbxml")){        return 0x36;}
        else if(type.equals("application/pkcs7-mime")){                        return 0x37;}
        else if(type.equals("application/vnd.wap.hashed-certificate")){        return 0x38;}
        else if(type.equals("application/vnd.wap.signed-certificate")){        return 0x39;}
        else if(type.equals("application/vnd.wap.cert-response")){             return 0x3A;}
        else if(type.equals("application/xhtml+xml")){                         return 0x3B;}
        else if(type.equals("application/wml+xml")){                           return 0x3C;}
        else if(type.equals("text/css")){                                      return 0x3D;}
        else if(type.equals("application/vnd.wap.mms-message")){               return 0x3E;}
        else if(type.equals("application/vnd.wap.rollover-certificate")){      return 0x3F;}
        else if(type.equals("application/vnd.wap.locc+wbxml")){                return 0x40;}
        else if(type.equals("application/vnd.wap.loc+xml")){                   return 0x41;}
        else if(type.equals("application/vnd.syncml.dm+wbxml")){               return 0x42;}
        else if(type.equals("application/vnd.syncml.dm+xml")){                 return 0x43;}
        else if(type.equals("application/vnd.syncml.notification")){           return 0x44;}
        else if(type.equals("application/vnd.wap.xhtml+xml")){                 return 0x45;}
        else if(type.equals("application/vnd.wv.csp.cir")){                    return 0x46;}
        else if(type.equals("application/vnd.oma.dd+xml")){                    return 0x47;}
        else if(type.equals("application/vnd.oma.drm.message")){               return 0x48;}
        else if(type.equals("application/vnd.oma.drm.content")){               return 0x49;}
        else if(type.equals("application/vnd.oma.drm.rights+xml")){            return 0x4A;}
        else if(type.equals("application/vnd.oma.drm.rights+wbxml")){          return 0x4B;}
        else {return -1;}
    }

    public static String getCharset(int set)
    {
        switch(set)
        {
            case 0x04: return "iso-8859-1";
            case 0x05: return "iso-8859-2";
            case 0x06: return "iso-8859-3";
            case 0x07: return "iso-8859-4";
            case 0x08: return "iso-8859-5";
            case 0x09: return "iso-8859-6";
            case 0x0A: return "iso-8859-7";
            case 0x0B: return "iso-8859-8";
            case 0x0C: return "iso-8859-9";
            case 0x11: return "shift_JIS";
            case 0x03: return "us-ascii";
            case 0x6A: return "utf-8";
            default:   return "unknown";
        }
    }
    public static int getCharset(String set)
    {
        try
        {
            int val=Integer.decode(set);
            return val;
        } catch (Exception ex)
        {}
             if(set.equals("iso-8859-1")){ return 0x04;} 
        else if(set.equals("iso-8859-2")){ return 0x05;} 
        else if(set.equals("iso-8859-3")){ return 0x06;} 
        else if(set.equals("iso-8859-4")){ return 0x07;} 
        else if(set.equals("iso-8859-5")){ return 0x08;} 
        else if(set.equals("iso-8859-6")){ return 0x09;} 
        else if(set.equals("iso-8859-7")){ return 0x0A;} 
        else if(set.equals("iso-8859-8")){ return 0x0B;} 
        else if(set.equals("iso-8859-9")){ return 0x0C;} 
        else if(set.equals("shift_JIS")) { return 0x11;} 
        else if(set.equals("us-ascii"))  { return 0x03;} 
        else if(set.equals("utf-8"))     { return 0x6A;} 
        else {return -1;}
    }

    public static String getHeader(int header)
    {
        switch(header)
        {
            case 0x00: return "accept";
            case 0x01: return "accept-charset1";
            case 0x02: return "accept-encoding1";
            case 0x03: return "accept-language";
            case 0x04: return "accept-ranges";
            case 0x05: return "age";
            case 0x06: return "allow";
            case 0x07: return "authorization";
            case 0x08: return "cache-control1";
            case 0x09: return "connection";
            case 0x0A: return "content-base1";
            case 0x0B: return "content-encoding";
            case 0x0C: return "content-language";
            case 0x0D: return "content-length";
            case 0x0E: return "content-location";
            case 0x0F: return "content-md5";
            case 0x10: return "content-range1";
            case 0x11: return "content-type";
            case 0x12: return "date";
            case 0x13: return "etag";
            case 0x14: return "expires";
            case 0x15: return "from";
            case 0x16: return "host";
            case 0x17: return "if-modified-since";
            case 0x18: return "if-match";
            case 0x19: return "if-none-match";
            case 0x1A: return "if-range";
            case 0x1B: return "if-unmodified-since";
            case 0x1C: return "location";
            case 0x1D: return "last-modified";
            case 0x1E: return "max-forwards";
            case 0x1F: return "pragma";
            case 0x20: return "proxy-authenticate";
            case 0x21: return "proxy-authorization";
            case 0x22: return "public";
            case 0x23: return "range";
            case 0x24: return "referer";
            case 0x25: return "retry-after";
            case 0x26: return "server";
            case 0x27: return "transfer-encoding";
            case 0x28: return "upgrade";
            case 0x29: return "user-agent";
            case 0x2A: return "vary";
            case 0x2B: return "via";
            case 0x2C: return "warning";
            case 0x2D: return "www-authenticate";
            case 0x2E: return "content-disposition1";
            case 0x2F: return "x-wap-application-id";
            case 0x30: return "x-wap-content-uri";
            case 0x31: return "x-wap-initiator-uri";
            case 0x32: return "accept-application";
            case 0x33: return "bearer-indication";
            case 0x34: return "push-flag";
            case 0x35: return "profile";
            case 0x36: return "profile-diff";
            case 0x37: return "profile-warning1";
            case 0x38: return "expect";
            case 0x39: return "te";
            case 0x3A: return "trailer";
            case 0x3B: return "accept-charset";
            case 0x3C: return "accept-encoding";
            case 0x3D: return "cache-control1";
            case 0x3E: return "content-range";
            case 0x3F: return "x-wap-tod";
            case 0x40: return "content-id";
            case 0x41: return "set-cookie";
            case 0x42: return "cookie";
            case 0x43: return "encoding-version";
            case 0x44: return "profile-warning";
            case 0x45: return "content-disposition";
            case 0x46: return "x-wap-security";
            case 0x47: return "cache-control";
            default  : return "unknown";
        }	
    }
}