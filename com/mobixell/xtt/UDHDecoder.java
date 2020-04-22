package com.mobixell.xtt;

/**
 * Decode UDH Packets
 */
public class UDHDecoder
{
    private byte[] body = null;
    private int pointer = 0;
    private String[] storeVar=null;
    private UDHSegmentStore segmentStore=null;

    //Internally handle the getting of CStrings
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
    //Internally handle the getting of Integers
    public int getIntFromByteArray(int octets) throws Exception
    {
        int retval=ConvertLib.getIntFromByteArray(this.body, pointer  ,octets);
        pointer=pointer+octets;
        return retval;
    }
    private void setVariable(String storePoint,String storeValue)
    {
        for(int i=0;i<storeVar.length;i++)
        {
            XTTProperties.setVariable(storeVar[i]+storePoint,storeValue);
        }
    }

    // Constructor
    public UDHDecoder(byte[] body, int pointer, String storeVar, UDHSegmentStore store)
    {
        this(body,pointer,new String[]{storeVar},store);
    }
    public UDHDecoder(byte[] body, int pointer, String[] storeVar, UDHSegmentStore store)
    {
        // Body
        this.body=body;
        // Start ointer inside body
        this.pointer=pointer;
        // where to sotre in memory
        this.storeVar=storeVar;
        // Where the segments are stored
        this.segmentStore=store;
    }

    // Decode the body into udh headers
    public void decode() throws Exception
    {
        // First byte is the header length
        int length=getIntFromByteArray(1);
        // set the end of the header bytes
        int end=pointer+length;
        // Buffer for the debug output
        StringBuffer output=new StringBuffer();
        for(int i=0;i<storeVar.length;i++)
        {
            output.append("\n stored in "+storeVar[i]+"/UDH/");
        }
        // While we haven't reached the end
        while((pointer<end) && (pointer<body.length))
        {
            // Get the header code
            int udh_ie_identifier=getIntFromByteArray(1);
            int udh_ie_length=0;
            // Check the header code, we support port number and SAR
            switch(udh_ie_identifier)
            {
                // port number
                case 0x05:
                    // get the length
                    udh_ie_length=getIntFromByteArray(1);
                    // Get the destination port
                    int destport=getIntFromByteArray(2);
                    setVariable("/UDH/"+"destination_port",""+destport     );
                    // Get the source port
                    int sourceport=getIntFromByteArray(2);
                    // Store in memory
                    setVariable("/UDH/"+"source_port"     ,""+sourceport     );
                    // Append to debug output
                    output.append("\n type: port numbers ("+udh_ie_identifier+")"
                        +"\n   length          : "+udh_ie_length
                        +"\n   destination_port: "+destport
                        +"\n   source_port     : "+sourceport);
                    break;
                // SAR
                case 0x08:
                    // get the length
                    udh_ie_length=getIntFromByteArray(1);
                    // Get the reference number
                    int refno=getIntFromByteArray(2);
                    // Store in memory
                    setVariable("/UDH/"+"reference number",""+refno     );
                    // get the total segment count
                    int segtot=getIntFromByteArray(1);
                    // Store in memory
                    setVariable("/UDH/"+"segments"        ,""+segtot     );
                    // get the current segement number
                    int segno=getIntFromByteArray(1);
                    // Store in memory
                    setVariable("/UDH/"+"segment_number"  ,""+segno     );
                    // Store in memory
                    setVariable("/UDH/"+segno+"/reference number",""+refno     );
                    setVariable("/UDH/"+segno+"/segments"        ,""+segtot     );
                    setVariable("/UDH/"+segno+"/segment_number"  ,""+segno     );
                    // Append to debug output
                    output.append("\n type: SAR ("+udh_ie_identifier+")"
                        +"\n   length          : "+udh_ie_length
                        +"\n   reference number: "+refno
                        +"\n   segments        : "+segtot
                        +"\n   segment_number  : "+segno);

                    // Set variables for reference
                    current_reference      = refno;
                    current_segments       = segtot;
                    current_segment_number = segno;
                    hasallsegments=false;
                    break;
                // Special SMS Message Indication
                case 0x01:
                    // get the length
                    udh_ie_length=getIntFromByteArray(1);
                    // Get the type and store info
                    int mis=getIntFromByteArray(1);
                    // Store in memory
                    setVariable("/UDH/"+"mis",""+mis     );
                    // get the total segment count
                    int num=getIntFromByteArray(1);
                    // Store in memory
                    setVariable("/UDH/"+"msgcount",""+num     );
                    // Append to debug output
                    String store="0 (Discard message after updating indication) ";
                    if((mis&7)>0)store="1 (Store message after updating indication) ";
                    int type=mis&3;
                    String types=null;
                    switch(type)
                    {
                        case 0:types="00 (Voice Message Waiting)";break;
                        case 1:types="01 (Fax Message Waiting)";break;
                        case 2:types="10 (Electronic Mail Message Waiting)";break;
                        case 3:types="11 (Extended Message Type Waiting)";break;
                    }
                    String extn="000 (No extended message indication type) ";
                    if((mis&28)==4)
                    {
                        extn="001 (Video message waiting) ";
                    } else if((mis&28)!=0)
                    {
                        extn="xxx (Reserved) ";
                    }
                    int prof=(mis&96);
                    String profs=null;
                    switch(prof)
                    {
                        case 0 :profs="00 (profile ID 1)";break;
                        case 32:profs="01 (profile ID 2)";break;
                        case 64:profs="10 (profile ID 3)";break;
                        case 96:profs="11 (profile ID 4)";break;
                    }

                    output.append("\n type: Special SMS Message Indication ("+udh_ie_identifier+")"
                        +"\n   length          : "+udh_ie_length
                        +"\n   mis             : "+mis
                        +"\n                   :   "+store
                        +"\n                   :  "+profs
                        +"\n                   : "+extn
                        +"\n                   :  "+types
                        +"\n   msgcount        : "+num);
                    break;
                // We don't know that one, just skipp it's content
                default:
                    udh_ie_length=getIntFromByteArray(1);
                    output.append("\n UDH type: '"+udh_ie_identifier+"' unsupported skipping "+udh_ie_length+" bytes");
                    pointer=pointer+udh_ie_length;
                    break;
            }
        }

        // Append the current segment to the segment store
        if(current_segments==-1)
        {
            segmentStore.addData(current_reference,this.body,pointer,this.body.length,1,1);
        } else
        {
            segmentStore.addData(current_reference,this.body,pointer,this.body.length,current_segments,current_segment_number);
        }

        // We got the SAR so we probably have multiple segments
        if(current_reference!=-1)
        {
            hasallsegments=segmentStore.checkAllSegmentsHere(current_reference);
        }
        if(hasAllSegments())
        {
            byte[] payload=getPayload();
            output.append("\n all segments received: payload stored to:"
                +"\n   base64          : "+payload.length+" bytes"
            );
            setVariable("/UDH/"+"base64"        ,ConvertLib.base64Encode(payload));
        }
        // Print the debug output
        XTTProperties.printDebug(this.getClass().getName()+".decodeUDH: head: "+length+" bytes"+output.toString());
    }

    private boolean hasallsegments  = true;
    private int current_reference=-1;
    private int current_segments=-1;
    private int current_segment_number=-1;
    
    public int getReference()
    {
        return current_reference;
    }
    public int getSegments()
    {
        return current_segments;
    }
    public int getSegmentNumber()
    {
        return current_segment_number;
    }
    
    public boolean hasAllSegments()
    {
        return hasallsegments;
    }
    // Return the remaining body
    public byte[] getPayload()
    {
        return segmentStore.getData(current_reference);
    }
}
