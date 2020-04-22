package com.mobixell.xtt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;

public class SMPP_PDUHeader implements SMPPConstants
{
    public int command_length  =0;
    public int command_id      =0;
    public int command_status  =0;
    public int command_sequence=0;
    private String function=null;
    private int autoMessageRetryTime=0;public void setAutoMessageRetryTime(int autoMessageRetryTime){this.autoMessageRetryTime=autoMessageRetryTime;}
    
    /**
     * Constructor to set value of SMPP PDU headers
     * @param sfunction
     * @param scommand_id
     * @param scommand_status
     * @param scommand_sequence
     */
    public SMPP_PDUHeader(String sfunction,int scommand_id,int scommand_status,int scommand_sequence)
    {
        function=sfunction;
        command_id      =scommand_id;
        command_status  =scommand_status;
        command_sequence=scommand_sequence;
    }
    public SMPP_PDUHeader(String sfunction)
    {
        function=sfunction;
    }
    public boolean readPDUHeader(BufferedInputStream in) throws Exception
    {
        return readPDUHeader(in,null);
    }
    
    /**
     * Method to read PDU headers
     * @param in - Input stream contains headers
     * @param socket - socket object
     * @return - true or false
     * @throws Exception
     */
    public boolean readPDUHeader(BufferedInputStream in, Socket socket) throws Exception
    {
        byte[] b=new byte[16];
        int soTimeout=0;
        try
        {
            if(socket!=null)
            {
                soTimeout=socket.getSoTimeout();
                socket.setSoTimeout(autoMessageRetryTime);
            }
            HTTPHelper.readBytes(in,b);
        } catch (java.net.SocketTimeoutException stex)
        {
            return false;
        } finally
        {
            if(socket!=null)socket.setSoTimeout(soTimeout);
        }

        XTTProperties.printVerbose(function+": Received: 16 bytes");
        command_length  =ConvertLib.getIntFromByteArray(b, 0,4);
        command_id      =ConvertLib.getIntFromByteArray(b, 4,4);
        command_status  =ConvertLib.getIntFromByteArray(b, 8,4);
        command_sequence=ConvertLib.getIntFromByteArray(b,12,4);
        XTTProperties.printDebug(function+": Received: \n"
            +"\ncommand_length  ="+ConvertLib.outputBytes(b, 0,4,2)+"="+command_length
            +"\ncommand_id      ="+ConvertLib.outputBytes(b, 4,4,2)+"="+command_id
            +"\ncommand_status  ="+ConvertLib.outputBytes(b, 8,4,2)+"="+command_status
            +"\ncommand_sequence="+ConvertLib.outputBytes(b,12,4,2)+"="+command_sequence
            +"\n");
        return true;
    }
    public void writePDU(BufferedOutputStream out, byte[] body) throws Exception
    {
        writePDU(out,body,null);
    }
    
    /**
     * Method to write PDU information
     * @param out - output stream
     * @param body - Body of the PDU
     * @param tlv
     * @throws Exception
     */
    public void writePDU(BufferedOutputStream out, byte[] body, byte[] tlv) throws Exception
    {
        command_length=16;

        if(body!=null)
        {
            command_length=command_length+body.length;
        }
        if(tlv!=null)
        {
            command_length=command_length+tlv.length;
        }
        
        byte[] cle=ConvertLib.getByteArrayFromInt(command_length,4);
        out.write(cle,0,4);
        byte[] cid=ConvertLib.getByteArrayFromInt(command_id,4);
        out.write(cid,0,4);
        byte[] cst=ConvertLib.getByteArrayFromInt(command_status,4);
        out.write(cst,0,4);
        byte[] cse=ConvertLib.getByteArrayFromInt(command_sequence,4);
        out.write(cse,0,4);

        if(body!=null&&body.length>0)
        {
            out.write(body,0,body.length);
        }
        if(tlv!=null&&tlv.length>0)
        {
            out.write(tlv,0,tlv.length);
        }
        out.flush();

        XTTProperties.printDebug(function+".writePDU: Sent: \n"
            +"\ncommand_length  ="+ConvertLib.outputBytes(cle,0,4,2)+"="+command_length
            +"\ncommand_id      ="+ConvertLib.outputBytes(cid,0,4,2)+"="+command_id
            +"\ncommand_status  ="+ConvertLib.outputBytes(cst,0,4,2)+"="+command_status
            +"\ncommand_sequence="+ConvertLib.outputBytes(cse,0,4,2)+"="+command_sequence
            +"\n");
        if(body!=null)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printVerbose(function+":writeBody: "+body.length+" bytes\n"+ConvertLib.getHexView(body));
            } else
            {
                XTTProperties.printVerbose(function+":writeBody: "+body.length+" bytes\n");
            }
        } else
        {
            XTTProperties.printVerbose(function+":writeBody: no Body: \n");
        }
        if(tlv!=null)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printVerbose(function+":writeTLVs: "+tlv.length+" bytes\n"+ConvertLib.getHexView(tlv));
            } else
            {
                XTTProperties.printVerbose(function+":writeTLVs: "+tlv.length+" bytes\n");
            }
        }
    }
}
