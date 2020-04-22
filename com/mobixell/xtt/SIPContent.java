package com.mobixell.xtt;

import java.util.LinkedHashMap;
import java.util.Vector;
import java.io.ByteArrayOutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

/**
 * SIPContent
 * <p>
 * Create contents for the SIP classes
 */
public class SIPContent implements SIPConstants
{

    public static byte[] createNotify(LinkedHashMap<String,Vector<String>> receivedHeader, String mode, String serverAddress, int serverPort) throws IOException
    {
        byte[] data=null;
        byte[] body=new byte[0];
        Vector<String> v=null;

        String contentType=receivedHeader.get("accept").get(0);

        LinkedHashMap<String,Vector<String>> sendHeader=new LinkedHashMap<String,Vector<String>>();

        //int cseq=SIPWorker.getCSeq();
        
        ByteArrayOutputStream dataStreamOut=new ByteArrayOutputStream(65535);
        BufferedOutputStream outputStream = new BufferedOutputStream(dataStreamOut);        
        
        String[] toUrl=receivedHeader.get("from").get(0).split("<|>",3);
        String[] fromUrl=receivedHeader.get("to").get(0).split(":|>",3);
        outputStream.write(ConvertLib.createBytes("NOTIFY "+toUrl[1]+" SIP/2.0\r\n"));
        
        sendHeader.put("from",receivedHeader.get("to"));
        sendHeader.put("to",receivedHeader.get("from"));
        sendHeader.put("call-id",receivedHeader.get("call-id"));
        sendHeader.put("event",receivedHeader.get("event"));
        sendHeader.put("max-forwards",receivedHeader.get("max-forwards"));

        String[] cseqS=receivedHeader.get("cseq").get(0).split("\\s",2);
        int cseq=Integer.parseInt(cseqS[0])+1;
        v=new Vector<String>();
        v.add(cseq+" NOTIFY");
        sendHeader.put("cseq",v);


        v=new Vector<String>();
        v.add(contentType);
        sendHeader.put("content-type",v);
        
        v=new Vector<String>();
        //Via: SIP/2.0/TCP 172.20.14.1:59189;branch=z9hG4bK6d84307ebf9535053c41670c6303d2ad
        v.add("SIP/2.0/"+mode+" "+serverAddress+":"+serverPort+";branch=12334456754");
        sendHeader.put("via",v);

        v=new Vector<String>();
        v.add(HTTPHelper.createHTTPDate());
        sendHeader.put("date",v);

        v=new Vector<String>();
        v.add("active");
        sendHeader.put("subscription-state",v);

        if(contentType!=null&&contentType.equals("application/pidf+xml"))
        {
            body=ConvertLib.createBytes("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
                                       +"<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"\r\n"
                                       +"    entity=\"pres:"+fromUrl[1]+"\">\r\n"
                                       +"  <tuple id=\"12345\">\r\n"
                                       +"    <status>\r\n"
                                       +"      <basic>open</basic>\r\n"
                                       +"    </status>\r\n"
                                       +"    <contact priority=\"0.8\">sip:"+fromUrl[1]+"</contact>\r\n"
                                       +"  </tuple>\r\n"
                                       +"</presence>\r\n");
        }
        v=new Vector<String>();
        v.add(""+body.length);
        sendHeader.put("content-length",v);

        String debug=SIPWorker.printDeepHeaders(mode,sendHeader,outputStream);
        outputStream.write(body);
        outputStream.flush();
        data=dataStreamOut.toByteArray();

        //System.out.println("DATA::::"+ConvertLib.getHexView(data,0,data.length));
        return data;
    }

}

