package com.mobixell.xtt;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * FunctionModule_Multipart.
 * <p>
 * Functions for encode and decode MIME/Multipart message.
 *
 * @author Anil Wadhai
 * @version $Id: FunctionModule_Multipart.java,v 1.0 2009/01/16 11:47:12 anilw
 *          Exp $
 */

public class FunctionModule_Multipart extends FunctionModule
{
    public static final String     tantau_sccsid    = "@(#)$Id: FunctionModule_Multipart.java,v 1.2 2009/02/09 10:43:47 awadhai Exp $";
    private Map<String, Multipart> multiParts       = Collections.synchronizedMap(new LinkedHashMap<String, Multipart>());
    private final static String    CRLF             = "\r\n";
    private final static String    DOUBLE_HYPHEN    = "--";
    private final static String    COLON            = ": ";
    private final static String    SEMICOLON        = ";";
    private final static String    COMMA            = ", ";
    private final static String    SLASH            = "/";
    private final static String    PER              = "pre";
    private final static String    END              = "end";
    private final static String    HEADER           = "header";
    private final static String    HEADERS          = "headers";
    private final static String    LENGTH           = "length";
    private final static String    VALUE            = "value";
    private final static String    BOBY             = "body";
    private final static String    BASE64           = "base64";
    private final static String    PLAIN            = "plain";
    private int                    bufferOutputSize = -1;
    private final static byte[]    ENDOFHEADERS     = {(byte)'\r', (byte)'\n', (byte)'\r', (byte)'\n'};

    /**
     * clear all variables.
     *
     * @see com.mobixell.xtt.FunctionModule#initialize()
     */
    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName() + ".initialize(): clearing variables");
        bufferOutputSize = XTTProperties.getIntProperty("BUFFEROUTPUTSIZE");
        multiParts.clear();
    }

    /**
     * Create a multipart document with a specific name which can be used in
     * subsequent functions to identify the multipart.
     *
     * @param parameters
     *            array of String containing the parameters.
     *            <br><code>parameters[0]</code> argument is always the method name,
     *            <br><code>parameters[1]</code> argument is the multipartName,
     *            <br><code>parameters[2]</code> argument is the boundary. it is used to
     *            <br>identify the number of parts available in multipart documnet
     *            <br><code>parameters[3]</code> argument is the optional.It can be either
     *            <br>'multipart/related' or 'multipart/mixed'. If not specified then 'multipart/related'
     *            <br>is taken as default.
     *            <br>If null is used as <code>parameters</code> it sends the allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            <br>XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void createMultipart(String parameters[])
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createMultipart: multipartName boundary");
            return;
        } else if(parameters.length < 3 || parameters.length > 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName boundary");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String multipartName = parameters[1];
            String boundary      = parameters[2];
            String subtype       = "multipart/related";

            if(parameters.length == 4)
            {
                subtype = parameters[3];
            }
            Multipart multipart = new Multipart(boundary, subtype);
            multiParts.put(multipartName, multipart);
            XTTProperties.printInfo(parameters[0]+": multipart '"+ multipartName+"'"+": with subtype ='"+subtype+"'");
        }
    }

    /**
     * Add part to the specified multipart.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            If null is used as <code>parameters</code> it sends the<br>
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.<br>
     *            To add the part into specified multipart,first get multipart using<br>
     *            given mulitpart name & then add new part into the obtained multipart.
     *
     */
    public void addPart(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": addPart: multipartName");
            return;
        } else if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            Multipart multiPart  = multiParts.get(multipartName);
            if(null == multiPart)
            {
                XTTProperties.printFail(parameters[0]+": multiPart '"+multipartName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                Part part = new Part();
                multiPart.addPart(part);
                XTTProperties.printInfo(parameters[0]+": part added to multipart '"+multipartName+"'");
            }
        }
    }

    /**
     * Remove the specified part from the multipart document.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the partNumber , <br>
     *            partNumber being used to remove specific part from multipart
     *            document. <br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.<br>
     *            To remove specific part from multipart document first get multipart<br>
     *            document using multipart name then remove the specific part with the <br>
     *            help of part number from obtained multipart.
     *
     */
    public void removePart(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": removePart: multipartName partNumber");
            return;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName partNumber");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            String partNumber    = parameters[2];
            int int_partNumber   = Integer.parseInt(partNumber);
            Multipart multiPart  = multiParts.get(multipartName);
            Part part            = multiPart.getParts().get(int_partNumber);
            if(null == multiPart || null == part)
            {
                XTTProperties.printFail(parameters[0]+": multiPart '"+multipartName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                multiPart.getParts().remove(int_partNumber);
                XTTProperties.printInfo(parameters[0]+": '"+multipartName+"/"+ partNumber+"' removed");
            }
        }
    }

    /**
     * Add a header to the specified multipart part.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the partNumber , <br>
     *            It is used to specify header can be added in specific part <br>
     *            <code>parameters[3]</code> argument is the headerName , <br>
     *            <code>parameters[4]</code> argument is the headerValue , <br>
     *            headerName & headerValue fields can be used to further <br>
     *            identify and describe the data in a message body
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.<br>
     *            To add the part header into specific part,first obtain multipart <br>
     *            using given mulitpart, then obtain a specif part using partNumber <br>
     *            then add new header into the obtained part.If header name is already<br>
     *            present in header name list then it has to be added a second time.
     */
    public void addPartHeader(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": addPartHeader: multipartName partNumber headerName headerValue");
            return;
        } else if(parameters.length != 5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName partNumber headerName headerValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            String partNumber    = parameters[2];
            String headerName    = parameters[3];
            String headerValue   = parameters[4];
            int int_partNumber   = Integer.parseInt(partNumber);
            Multipart multiPart  = multiParts.get(multipartName);
            Part part            = multiPart.getParts().get(int_partNumber);
            if(null == multiPart || null == part)
            {
                XTTProperties.printFail(parameters[0]+": multiPart '"+multipartName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                multiPart.getParts().get(int_partNumber).addHeader(headerName, headerValue);
                XTTProperties.printInfo(parameters[0]+": '"+multipartName+"/"+ partNumber+"' added header '"+headerName+"' to the part");
            }
        }
    }

    /**
     * Removes all the headers of specified name from a specified part
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the partNumber, <br>
     *            <code>parameters[3]</code> argument is the headerName <br>
     *            If null is used as <code>parameters</code> it sends the allowed
     *            parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.<br>
     *            Using multipartName, partNumber,headerName remove the headername<br>
     *            & corresponding values of specific part
     *
     */
    public void removePartHeader(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": removePartHeader: multipartName partNumber headerName");
            return;
        } else if(parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName partNumber headerName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        }else
        {
            String multipartName      = parameters[1];
            String partNumber         = parameters[2];
            String headerName         = parameters[3];
            int int_partNumber        = Integer.parseInt(partNumber);
            Multipart multiPart       = multiParts.get(multipartName);
            List<String> headerNames  = multiPart.getParts().get(int_partNumber).getHeaderNames();
            List<String> headerValues = multiPart.getParts().get(int_partNumber).getHeaderValues();

            if(null == multiPart || null == headerNames)
            {
                XTTProperties.printFail(parameters[0]+": '"+multipartName+"/"+ partNumber+"/"+headerName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }else
            {
                for(int i =0 ; i<headerNames.size();i++)
                {
                    while(headerNames.contains(headerName))
                    {
                        int indexPos = headerNames.indexOf(headerName);
                        headerNames.remove(indexPos);
                        headerValues.remove(indexPos);
                    }
                }
                XTTProperties.printInfo(parameters[0]+": '"+multipartName+"/"+ partNumber+"' removed header "+headerName+" from the part");
            }
        }
    }

    /**
     * Add binary data as a multiparts part body. XTT stores binary data
     * internally as base64, which has to be base64 decoded before being used as
     * a byte array. To add bas64 encoded body the body needs to be base64
     * encoded TWICE.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the partNumber.<br>
     *            This can be used to add body in specific part <br>
     *            <code>parameters[3]</code> argument is the base64file.<br>
     *            File containing the base64 TWICE encoded data. <br>
     *            If null is used as <code>parameters</code> it sends the allowed<br>
     *            parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void addPartBody(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": addPartBody: multipartName partNumber base64EncodedData");
            return;
        } else if(parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": multipartName partNumber base64EncodedData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            String partNumber    = parameters[2];
            int int_partNumber   = Integer.parseInt(partNumber);
            Multipart multiPart  = multiParts.get(multipartName);
            Part part            = multiPart.getParts().get(int_partNumber);
            byte[] data          = ConvertLib.base64Decode(parameters[3]);

            if(null == multiPart || null == part)
            {
                XTTProperties.printFail(parameters[0]+": multiPart '"+multipartName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                multiPart.getParts().get(int_partNumber).setContent(data);
                XTTProperties.printInfo(parameters[0]+": '"+multipartName+"/"+ partNumber+"' added '"+data.length+"' bytes of body");
            }
        }
    }

    /**
     * Remove the body from a mutlipart message.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the partNumber<br>
     *            part number for which body needs to be removed.<br>
     *            If null is used as <code>parameters</code> it sends the allowed<br>
     *            parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.<br>
     *            Using multipartName and partNumber get the specific part of mulitpart<br>
     *            document & remove the body of specific part.
     */
    public void removePartBody(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": removePartBody: multipartName partNumber");
            return;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": multipartName partNumber ");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            String partNumber    = parameters[2];
            int int_partNumber   = Integer.parseInt(partNumber);
            Multipart multiPart  = multiParts.get(multipartName);
            Part part            = multiPart.getParts().get(int_partNumber);

            if(null == multiPart || null == part)
            {
                XTTProperties.printFail(parameters[0]+": multiPart '"+multipartName+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                part.clearContent();
                XTTProperties.printInfo(parameters[0]+": '"+multipartName+"/"+ partNumber+"' removed body from the part");
            }
        }
    }

    /**
     * Add the text description before multipart i.e.preamble
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the discription of Pre Multipart text, <br>
     *            This can be added to the before boundary start.
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void addMultipartPreamble(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": addMultipartPreamble: multipartName preamble");
            return;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": multipartName preamble");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            Multipart multiPart  = multiParts.get(multipartName);
            multiPart.setPreamble(parameters[2]);
            XTTProperties.printInfo(parameters[0]+": preamble added to multipart '"+multipartName+"'");
        }
    }

    /**
     * Add the text description after multipart i.e.epilogue
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the discription of post Multipart text<br>
     *            This can be added to end of mulitpart(i.e.)
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void addMultipartEpilogue(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": addMultipartEpilogue: multipartName epilogue");
            return;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": multipartName epilogue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[1];
            Multipart multiPart  = multiParts.get(multipartName);
            multiPart.setEpilogue(parameters[2]);
            XTTProperties.printInfo(parameters[0]+": epilogue added to multipart'"+multipartName+"'");
        }
    }

    /**
     * Create the whole multipart as a document and then store it base64 encoded
     * into a xtt-variable.
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the multipartName, <br>
     *            <code>parameters[2]</code> argument is the variablename<br>
     *            If null is used as <code>parameters</code> it sends the
     *            allowed parameters list to the
     *            {@link XTTProperties#printFail(java.lang.String)
     *            XTTProperties.printFail(java.lang.String)} method and returns.
     *
     */
    public void storeMultipart(String parameters[])
    {
        if(null == parameters)
        {
            XTTProperties.printFail(this.getClass().getName()+": storeMultipart: multipartName variableName");
            return;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ":  multipartName variableName ");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String multipartName = parameters[2];
            Multipart multiPart  = multiParts.get(multipartName);
            if(null == multiPart)
            {
                XTTProperties.printFail(parameters[0] + ": multipart '" + multipartName + "' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            } else
            {
                byte[] byte_multiPart = createMultipartByteArray(multiPart);
                String data           = ConvertLib.base64Encode(byte_multiPart);
                XTTProperties.printInfo(parameters[0] + ": multipart document '" + multipartName + "' stored to '" + parameters[1]+"'");
                XTTProperties.setVariable(parameters[1], data);
            }
        }
    }

    /**
     * Decodes the multipart document
     *
     * @param parameters
     *            array of String containing the parameters. <br>
     *            <code>parameters[0]</code> argument is always the method name, <br>
     *            <code>parameters[1]</code> argument is the store name, <br>
     *            <code>parameters[2]</code> argument is the base64 multipart document, <br>
     *            <code>parameters[3]</code> argument is the contentType, <br>
     *            If null is used as <code>parameters</code> it sends the allowed parameters list to the <br>
     *            {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)}<br>
     *            method and returns.<br><br>
     * <pre>
     * The decoded multipart is stored in XTT variables as:
     *
     * [storeName]/length
     *     The number of parts found in the multipart
     * [storeName]/pre
     *     Anything found before the first boundary, normally an empty string
     * [storeName]/end
     *     Anything found after the last boundary, normally an empty string
     * [storeName]/[partNumber]/headers/length
     *     The number of headers found in the multipart
     * [storeName]/[partNumber]/headers/[headerNumber]
     *     The name of the found header in the order they are found
     * [storeName]/[partNumber]/headers/[headerNumber]/value
     *     The value of the found header in the order they are found
     * [storeName]/[partNumber]/header/[headerName]
     *     The value of the specified header, if there are duplicate headers this
     *     is a comma separated list of the values in the order received.
     * [storeName]/[partNumber]/header/[headerName]/length
     *     The number of headers of this name received.
     * [storeName]/[partNumber]/header/[headerName]/[duplicateHeaderNumber]
     *     The value of the specified header with duplicateHeaderNumber being the
     *     current number of the header with this name received.
     * [storeName]/[partNumber]/body/plain
     *     The body of the part as plain text
     * [storeName]/[partNumber]/body/base64
     *     The body of the part as base64 encoded binary
     *</pre>
     */
    public void decodeMultipart(String parameters[])
    {
        if (parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName() + ": decodeMultipart: storeName base64MultipartDocument contentType");
            return;
        }
        if (parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": storeName base64MultipartDocument contentType");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            StringBuffer sb = new StringBuffer();
            try
            {
                String storeName               = parameters[1];
                String base64MultipartDocument = parameters[2];
                String contentType             = parameters[3];

                sb.append(parameters[0]+": stored variable: "+storeName).append(CRLF);

                //extract the boundary from contentType
                String boundaryMarker = "boundary=";
                int boundaryIndex = contentType.indexOf(boundaryMarker);
                if (boundaryIndex == -1)
                {
                    XTTProperties.printFail(parameters[0] + ": '" + contentType + "' is a incorrect Content-Type, should have boundary parameter");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                }
                boundaryIndex = boundaryIndex + boundaryMarker.length();
                String boundary = contentType.substring(boundaryIndex).trim();
                if(boundary.startsWith("\""))
                {
                    boundary = boundary.substring(1, boundary.indexOf("\"",1));
                }else if(boundary.indexOf(SEMICOLON) != -1)
                {
                    boundary = boundary.substring(0, boundary.indexOf(SEMICOLON)).trim();
                }

                //created an instance of Multipart class which will be populated
                //with the parts received in the multipart document
                Multipart multipart = new Multipart();
                multipart.setBoundary(boundary);
                int partNumberCount = 0;
                //decode the multipart document
                byte[] packet = ConvertLib.base64Decode(base64MultipartDocument);
                //split the decoded multipart document with 'CRLF--boundary'
                byte[][] parts = ConvertLib.splitByteArray(packet, ConvertLib.createBytes(CRLF+DOUBLE_HYPHEN+boundary));
                for (int i = 0; i < parts.length; i++)
                {
                    byte[] part = parts[i];
                    if(i == 0)
                    {
                        byte[] tempBytes = ConvertLib.createBytes(DOUBLE_HYPHEN+boundary);
                        //check if the part starts with --boundary
                        if(part.length>=tempBytes.length && ConvertLib.compareBytes(part,tempBytes))
                        {
                            //It means there is no preamble in the multipart and part[0] is also a part
                            //hence add the part to multipart and set [storeName]/pre variable as empty string
                            multipart.addPart(createPart(part,partNumberCount++,parameters[0]));
                            sb.append(SLASH+PER+" = ").append("").append(CRLF);
                            XTTProperties.setVariable(storeName+SLASH+PER, ConvertLib.createString(part));
                        }else
                        {
                            //It means parts[0] contains preamble
                            sb.append(SLASH+PER+" = ").append(ConvertLib.createString(part)).append(CRLF);
                            //set [storeName]/pre
                            XTTProperties.setVariable(storeName+SLASH+PER, ConvertLib.createString(part));
                        }
                    }else if ((part.length != 0) && ConvertLib.compareBytes(part,new byte[]{'-','-'}))
                    {
                        //since this part starts with '--' just after '--boundary' it means it's epilogue
                        part = ConvertLib.subByteArray(part,2);
                        String end = ConvertLib.createString(part);
                        sb.append(SLASH+END+" = ").append(end).append(CRLF);
                        //set [storeName]/end
                        XTTProperties.setVariable(storeName+SLASH+END, end);
                    }else if (part.length != 0)
                    {
                        //It means this part is nither preamble nor epilogue
                        //hence add it to the multipart
                        multipart.addPart(createPart(part,partNumberCount++,parameters[0]));
                    }
                }

                //Now we have the instance of Multipart populated with parts
                //and we can use it to decode the multipart
                List allParts = multipart.getParts();
                if(allParts != null)
                {
                    sb.append(SLASH+LENGTH+" = ").append(allParts.size()).append(CRLF);
                    //set [storeName]/length
                    XTTProperties.setVariable(storeName+SLASH+LENGTH, allParts.size()+"");
                    //iterate over the parts
                    for(Part part : multipart.getParts())
                    {
                        String partNumber = part.getPartNumber();
                        //check if the part have headers
                        if(part.getHeaderNames() != null)
                        {
                            List<String> allHeaderNames = new ArrayList<String>(part.getHeaderNames());
                            List<String> allHeaderValues = new ArrayList<String>(part.getHeaderValues());
                            //created a copy of headerNames with names in lower case, since we need to
                            //consider headers with same name but different case as duplicate headers
                            List<String> allHeaderNamesLowerCased = toLowerCase(allHeaderNames);
                            int headerCount = 0;

                            for (Iterator<String> iterator = allHeaderNames.iterator(); iterator.hasNext();)
                            {
                                String headerName = iterator.next();
                                String headerValue = allHeaderValues.get(headerCount);
                                sb.append(SLASH+partNumber+SLASH+HEADERS+SLASH+headerCount+" = ").append(headerName).append(CRLF);
                                //set [storeName]/[partNumber]/headers/[headerNumber]
                                XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADERS+SLASH+headerCount, headerName);
                                sb.append(SLASH+partNumber+SLASH+HEADERS+SLASH+headerCount+SLASH+VALUE+" = ").append(headerValue).append(CRLF);
                                //set [storeName]/[partNumber]/headers/[headerNumber]/value
                                XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADERS+SLASH+headerCount+SLASH+VALUE, headerValue);
                                headerCount++;
                            }
                            sb.append(SLASH+partNumber+SLASH+HEADERS+SLASH+LENGTH+" = ").append(headerCount).append(CRLF);
                            //set [storeName]/[partNumber]/headers/length
                            XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADERS+SLASH+LENGTH, headerCount+"");
                            while(!allHeaderNames.isEmpty())
                            {
                                int count = 0;
                                String headerName = allHeaderNames.remove(0);
                                String headerValue = allHeaderValues.remove(0);
                                allHeaderNamesLowerCased.remove(0);
                                sb.append(SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+count+" = ").append(headerValue).append(CRLF);
                                XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+count, headerValue);
                                while(allHeaderNamesLowerCased.contains(headerName.toLowerCase()))
                                {
                                    count++;
                                    int duplicateHeaderIndexPos = allHeaderNamesLowerCased.indexOf(headerName.toLowerCase());
                                    allHeaderNamesLowerCased.remove(duplicateHeaderIndexPos);
                                    allHeaderNames.remove(duplicateHeaderIndexPos);
                                    String duplicateHeaderValue = allHeaderValues.remove(duplicateHeaderIndexPos);
                                    sb.append(SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+count+" = ").append(duplicateHeaderValue).append(CRLF);
                                    //set [storeName]/[partNumber]/header/[headerName]/[duplicateHeaderNumber]
                                    XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+count, duplicateHeaderValue);
                                    headerValue = headerValue + COMMA + duplicateHeaderValue;
                                }
                                sb.append(SLASH+partNumber+SLASH+HEADER+SLASH+headerName+" = ").append(headerValue).append(CRLF);
                                //set [storeName]/[partNumber]/header/[headerName]
                                XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADER+SLASH+headerName, headerValue);
                                sb.append(SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+LENGTH+" = ").append(count+1).append(CRLF);
                                //set [storeName]/[partNumber]/header/[headerName]/length
                                XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADER+SLASH+headerName+SLASH+LENGTH, (count+1)+"");
                            }
                        }else
                        {
                            // There are no headers in the part
                            sb.append(SLASH+partNumber+SLASH+HEADERS+SLASH+LENGTH+" = ").append("0").append(CRLF);
                            XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+HEADERS+SLASH+LENGTH, "0");
                        }
                        sb.append(SLASH+partNumber+SLASH+BOBY+SLASH+PLAIN+" = ").append(part.getContent().length+" bytes").append(CRLF);
                        sb.append(SLASH+partNumber+SLASH+BOBY+SLASH+BASE64+" = ").append(part.getContent().length+" bytes").append(CRLF);
                        sb.append(ConvertLib.getHexView(part.getContent(),0,bufferOutputSize)).append(CRLF);
                        //set [storeName]/[partNumber]/body/plain
                        XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+BOBY+SLASH+PLAIN, ConvertLib.createString(part.getContent()));
                        //set [storeName]/[partNumber]/body/base64
                        XTTProperties.setVariable(storeName+SLASH+partNumber+SLASH+BOBY+SLASH+BASE64, ConvertLib.base64Encode(part.getContent()));
                    }
                }else
                {
                    //There are no parts in the multipart
                    XTTProperties.printWarn(parameters[0]+": No parts in the multipart");
                }
                XTTProperties.printInfo(parameters[0]+": decoded multipart with store name '" + storeName + "'");
            }catch (Exception ex)
            {
                XTTProperties.printFail(parameters[0] + ": decoding error: " + ex.getClass().getName() + " " + ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if (XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                return;
            }finally
            {
                XTTProperties.printVerbose(sb.toString());
            }
        }
    }

    /**
     * Returns a byte array containing the whole multipart document. using<br>
     * multiprt name get multipart document, then get the number of parts <br>
     * available in mulitpart document then retrive the headernames, headervalues, <br>
     * body of each part and added into ByteArrayOutputStream and retrun the<br>
     * byte array containing the whole multipart document
     *
     * @param multipart
     */
    private byte[] createMultipartByteArray(Multipart multipart)
    {
        ByteArrayOutputStream baos    = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(baos);
        try
        {
            // if preamble is present then add pre + CRLF
            if(null != multipart.getPreamble())
            {
                outputStream.writeBytes(multipart.getPreamble()+CRLF);
            }
            List<Part> parts = multipart.getParts();
            for (Part part : parts)
            {
                outputStream.writeBytes(DOUBLE_HYPHEN +multipart.getBoundary()+CRLF);
                List<String> allHeaderNames  = part.getHeaderNames();
                List<String> allHeaderValues = part.getHeaderValues();
                if((null != allHeaderNames && null != allHeaderValues)&&(allHeaderNames.size()>0 && allHeaderValues.size()>0)) {
                    for (int i=0 ;i<allHeaderNames.size();i++ )
                    {
                        String headerName  = allHeaderNames.get(i);
                        String headerValue = allHeaderValues.get(i);
                        outputStream.writeBytes(headerName+COLON+headerValue+CRLF);
                    }
                }
                outputStream.writeBytes(CRLF);
                outputStream.write(part.getContent());
                outputStream.writeBytes(CRLF);
            }
            outputStream.writeBytes(DOUBLE_HYPHEN +multipart.getBoundary()+ DOUBLE_HYPHEN);
            // if epilogue is present add CRLF + end
            if(null != multipart.getEpilogue())
            {
                outputStream.writeBytes(CRLF+multipart.getEpilogue());
            }
            outputStream.flush();
            return baos.toByteArray();
        }catch (IOException e)
        {
            XTTProperties.printFail("createMultipartByteArray: Unable to create multipart byte array");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
            return null;
        }finally
        {
            try
            {
                outputStream.close();
                baos.close();
            }catch (IOException e)
            {
                // do nothing
            }
        }
    }

    /**
     * Creates a Part object from the byte array
     * @return new created object of Part class
     */
    private Part createPart(byte[] part, int partNumberCount, String parameterZero)
    {
        Part hPart = new Part();
        hPart.setPartNumber("" + partNumberCount);
        byte[][] headersAndBody = ConvertLib.splitByteArray(part, ENDOFHEADERS);
        if (headersAndBody.length < 2)
        {
            XTTProperties.printFail(parameterZero + ": invalid part");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
        }
        byte[] headers = headersAndBody[0];
        byte[] body = new byte[0];
        for (int j = 1; j < headersAndBody.length; j++)
        {
            body = ConvertLib.concatByteArray(body, headersAndBody[j]);
            if (j != headersAndBody.length - 1)
            {
                body = ConvertLib.concatByteArray(body, ENDOFHEADERS);
            }
        }
        if (headers.length != 0)
        {
            String[] lines = ConvertLib.createString(headers).split(CRLF);
            for (int k = 1; k < lines.length; k++)
            {
                String line = lines[k];
                if (line.trim().length() != 0)
                {
                    int colonIndexPos = line.indexOf(COLON.trim());
                    if (colonIndexPos == -1)
                    {
                        XTTProperties.printWarn(parameterZero + ": '" + line + "' is a incorrect header, should have [name]:[value] format");
                    } else
                    {
                        String headerName  = line.substring(0, colonIndexPos).trim();
                        String headerValue = line.substring(colonIndexPos + 1).trim();
                        hPart.addHeader(headerName, headerValue);
                    }
                }
            }
        }
        hPart.setContent(body);
        return hPart;
    }

    /**
     * creates and returns a new List with all the elements of the given List in lower case
     * @param list source list
     * @return
     */
    private List<String> toLowerCase(List<String> list)
    {
        List<String> lowerCaseList = new ArrayList<String>();
        for (Iterator<String> iterator = list.iterator(); iterator.hasNext();)
        {
            String s =  iterator.next();
            lowerCaseList.add(s.toLowerCase());
        }
        return lowerCaseList;
    }

    private class Multipart
    {
        private String boundary  = null;
        private String subtype   = null;
        private String preamble  = null;
        private String epilogue  = null;
        private List<Part> parts = null;

        /**
         * default constructor
         */
        public Multipart()
        {
        }

        /**
         * constructor
         *
         * @param boundary
         * @param subtype
         */
        public Multipart(String boundary, String subtype)
        {
            this.boundary = boundary;
            this.subtype = subtype;
        }

        /**
         * returns a boundary of Multipart as string.
         *
         * @return boundary
         */
        public String getBoundary()
        {
            return boundary;
        }

        /**
         * set a boundary in Multipart.
         *
         * @param boundary
         */
        public void setBoundary(String boundary)
        {
            this.boundary = boundary;
        }

        /**
         * returns a subtype of Multipart as string.
         *
         * @return subtype
         */
        public String getSubtype()
        {
            return subtype;
        }

        /**
         * set a subtype to Multipart.
         *
         * @param subtype
         */
        public void setSubtype(String subtype)
        {
            this.subtype = subtype;
        }

        /**
         * @param part
         */
        public void addPart(Part part)
        {
            if (parts == null)
            {
                parts = Collections.synchronizedList(new ArrayList<Part>());
            }
            parts.add(part);
        }

        /**
         * returns a <code>List</code> of parts from multipart.
         */
        public List<Part> getParts()
        {
            return parts;
        }

        /**
         * @return the preamble
         */
        public String getPreamble()
        {
            return preamble;
        }

        /**
         * @param preamble the preamble to set
         */
        public void setPreamble(String preamble)
        {
            this.preamble = preamble;
        }

        /**
         * @return the epilogue
         */
        public String getEpilogue()
        {
            return epilogue;
        }

        /**
         * @param epilogue the epilogue to set
         */
        public void setEpilogue(String epilogue)
        {
            this.epilogue = epilogue;
        }
    }

    private class Part
    {
        private String      partNumber     = null;
        private byte[]      content        = new byte[0];
        private List<String> headerNames   = null;
        private List<String> headerValues  = null;

        /**
         * constructor
         */
        public Part()
        {

        }

        /**
         * constructor
         * @param partNumber
         * @param content
         */
        public Part(String partNumber, byte[] content)
        {
            this.partNumber = partNumber;
            this.content = content;
        }

        /**
         * Returns the of <code>List</code> of headerNames from the part
         */
        public List<String> getHeaderNames()
        {
            return headerNames;
        }

        /**
         * Returns the of <code>List</code> of headerValues from the part
         */
        public List<String> getHeaderValues()
        {
            return headerValues;
        }

        /**
         * Adds a header to the part
         * @param headerName
         * @param headerValue
         */
        public void addHeader(String headerName, String headerValue)
        {
            if (headerNames == null)
            {
                headerNames = Collections.synchronizedList(new ArrayList<String>());
                headerValues = Collections.synchronizedList(new ArrayList<String>());
            }
            headerNames.add(headerName);
            headerValues.add(headerValue);
        }

        /**
         * @return the partNumber
         */
        public String getPartNumber()
        {
            return partNumber;
        }

        /**
         * @param partNumber the partNumber to set
         */
        public void setPartNumber(String partNumber)
        {
            this.partNumber = partNumber;
        }

        /**
         * returns a byte array containing the body part.
         * @return content
         */
        public byte[] getContent()
        {
            return content;
        }

        /**
         * clear a byte array .
         */
        public void clearContent()
        {
            content = new byte[0];
        }

       /**
         * set a byte array containing the body part.
         * @param content
         */
        public void setContent(byte[] content)
        {
            this.content = content;
        }
    }

    /**
     * Overridden from superclass to add the MIME/Multipart version numbers.
     *
     * @see com.mobixell.xtt.FunctionModule#printVersion()
     */
    public void printVersion()
    {
        super.printVersion();
    }

    /**
     * Overridden from superclass to add the MIME/Multipart version numbers.
     *
     * @see com.mobixell.xtt.FunctionModule#showVersions()
     */
    public void showVersions()
    {
        super.showVersions();
    }

    /**
     * @return the getClass().getName() of this object. For debug reasons only.
     * @see java.lang.String#toString()
     */
    public String toString()
    {
        return this.getClass().getName();
    }

}
