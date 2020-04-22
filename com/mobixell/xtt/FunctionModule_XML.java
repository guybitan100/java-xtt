package com.mobixell.xtt;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.DocType;
import org.jdom.Namespace;
import org.jdom.Attribute;
import org.jdom.input.JDOMParseException;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;

import java.io.File;
import java.io.StringReader;
/**
 * FunctionModule_XTT.
 * <p>
 * Functions for testing XML document consistency.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.24 $
 */
public class FunctionModule_XML extends FunctionModule
{
    private HashMap<String,XMLDocument> documentMap = new HashMap<String,XMLDocument>();
    private HTTPConnection defaultConnection = null;
    private HashMap<String,Namespace> namespaces = new HashMap<String,Namespace>();
    
    public FunctionModule_XML()
    {

    }

    private class XMLDocument
    {
        public final static String CRLF                     = "\r\n";
        public LinkedHashMap<String,String> sendHeader            = new LinkedHashMap<String,String>();
        public LinkedHashMap<String,Vector<String>> receiveHeader = new LinkedHashMap<String,Vector<String>>();
        public byte[] serverResponse                        = null;
        public String serverResponseCode[]                  = new String[]{"","not yet initialized"};
        public java.security.cert.Certificate certs[]       = new java.security.cert.Certificate[0];
        
        public Document document = null;

        public XMLDocument() throws Exception
        {
            this((String)null);
        }
        public XMLDocument(String docContent) throws Exception
        {
            if(docContent!=null)
            {
                org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
                parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); 
                parser.setFeature("http://xml.org/sax/features/validation", false); 
                document = parser.build(new StringReader(docContent));
            } else
            {
                document = new Document();
            }
        }

        public void setDocument(String docContent) throws Exception
        {
            org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder("org.apache.xerces.parsers.SAXParser", false);
            parser.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); 
            parser.setFeature("http://xml.org/sax/features/validation", false); 
            document = parser.build(new StringReader(docContent));
        }

        public void setDocType(DocType doctype)
        {
            document.setDocType(doctype);
        }

        public String toString()
        {
            if(document.getContentSize()<=0)
            {
                return "";
            } else
            {
                return XTTXML.stringXML(document);
            }
        }
    }

    /**
     * Create a xml document.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the name of the document for further referencing in this module
     *                      <br><code>parameters[2]</code> XML content for the document. Has to be valid xml.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void createDocument(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createDocument: documentName");
            XTTProperties.printFail(this.getClass().getName()+": createDocument: documentName documentConent");
            return;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName documentConent");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            try
            {
                String name=parameters[1].trim().toLowerCase();
                XMLDocument document=null;
                if(parameters.length<3)
                {
                    document=new XMLDocument();
                } else
                {
                    document=new XMLDocument(parameters[2]);
                }
                documentMap.put(name,document);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0]+": created Document name='"+name+"'\n"+document);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": created Document name='"+name+"'");
                }
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": Error creating document: "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }
        }
    }

    /**
     * Set the doctype of a document.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the name of the document to set the doctype on
     *                      <br><code>parameters[2]</code> the element name.
     *                      <br><code>parameters[2]</code> the system id.
     *                      <br><code>parameters[2]</code> the public id.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setDocType(String parameters[])
    {
        if(parameters==null)
        {

            XTTProperties.printFail(this.getClass().getName()+": setDocType: documentName elementName");
            XTTProperties.printFail(this.getClass().getName()+": setDocType: documentName elementName systemID");
            XTTProperties.printFail(this.getClass().getName()+": setDocType: documentName elementName systemID publicID");
            return;
        }
        if(parameters.length<3||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName elementName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName elementName systemID");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName elementName systemID publicID");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            try
            {
                XMLDocument document=documentMap.get(parameters[1].trim().toLowerCase());

                DocType doctype=null;
                if(parameters.length<=3)
                {
                    doctype=new DocType(parameters[2]);
                } else if(parameters.length<=4)
                {
                    doctype=new DocType(parameters[2],parameters[3]);
                } else
                {
                    doctype=new DocType(parameters[2],parameters[4],parameters[3]);
                }
                document.setDocType(doctype);
                XTTProperties.printInfo(parameters[0]+": Document name='"+name+"': '"+doctype+"'");
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": Error setting doctype: "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            }
        }
    }


    /**
     * check a XML document against the XSD document definition.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the xml document to check, this can either be a real text document or the name of an document created with {@link #createDocument(java.lang.String[]) createDocument}.
     *                      <br><code>parameters[2]</code> argument is the  noNamespace schema location file name. If none is used enter the string null or use an empty string,
     *                      <br><code>parameters[3]</code> is the schema location always pairing with the next value
     *                      <br><code>parameters[4]</code> is the file name of the xsd document, use multiple pairs for multiple schemas.
     *                      <br><code>parameters[5]/parameters[6]</code> and following pairs for more schemas
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void validateXML(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": validateXML: documentToCheck noNamespaceSchemaLocation");
            XTTProperties.printFail(this.getClass().getName()+": validateXML: documentToCheck noNamespaceSchemaLocation schemaLocation1 file1...");
            return;
        }
        if(parameters.length%2!=1||parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentToCheck noNamespaceSchemaLocation");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentToCheck noNamespaceSchemaLocation schemaLocation1 file1...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XMLDocument xDoc=documentMap.get(parameters[1].trim().toLowerCase());
            String document=parameters[1];
            String name="";
            if(xDoc!=null)
            {
                document=xDoc.toString();
                name="'"+parameters[1].trim()+"' ";
            }
            StringBuffer schemaLocations=new StringBuffer("");
            String noNamespaceSchemaLocation="";
            StringBuffer debugInfo=new StringBuffer("");
            try
            {
                if(!parameters[2].equals("")&&!parameters[2].equals("null"))
                {
                    noNamespaceSchemaLocation=XTTProperties.getCurrentTestPath()+parameters[2];
                    debugInfo.append(" 'noNamespaceSchemaLocation' = '"+parameters[2]+"'");
                    File f=new File(noNamespaceSchemaLocation);
                    if(!f.exists())
                    {
                        XTTProperties.printFail(parameters[0]+": file not found: "+noNamespaceSchemaLocation);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    } else if(f.isDirectory())
                    {
                        XTTProperties.printFail(parameters[0]+": directory found: "+noNamespaceSchemaLocation);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    } else
                    {
                        // We use the URI format because there could be spaces in the filename
                        noNamespaceSchemaLocation=f.toURI().toString();
                    }
                } else
                {
                    debugInfo.append(" 'noNamespaceSchemaLocation' = null");
                }


                int i=3;
                String filename=null;
                while(i<parameters.length)
                {
                    debugInfo.append("\n"+" '"+parameters[i]);
                    schemaLocations.append(parameters[i++]+" ");
                    debugInfo.append("' = '"+parameters[i]+"'");
                    filename=XTTProperties.getCurrentTestPath()+parameters[i++];
                    File f=new File(filename);
                    if(!f.exists())
                    {
                        XTTProperties.printFail(parameters[0]+": file not found: "+filename);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    } else if(f.isDirectory())
                    {
                        XTTProperties.printFail(parameters[0]+": directory found: "+filename);
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                        return;
                    } else
                    {
                        // We use the URI format because there could be spaces in the filename
                        schemaLocations.append(f.toURI()+" ");
                    }
                }
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": document "+name+"validation failed:\n"+e.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                return;
            }
            try
            {
                org.jdom.output.XMLOutputter outputter = new org.jdom.output.XMLOutputter();
                org.jdom.input.SAXBuilder parser = null;

                if(schemaLocations.toString().equals("")&&noNamespaceSchemaLocation.equals(""))
                {
                    parser = new org.jdom.input.SAXBuilder();
                    debugInfo=new StringBuffer(" no schema checking performed");
                } else
                {
                    parser = new org.jdom.input.SAXBuilder("org.apache.xerces.parsers.SAXParser", true);
                    parser.setFeature("http://apache.org/xml/features/validation/schema", true);
                }

                if(!schemaLocations.toString().equals(""))
                {
                    parser.setProperty("http://apache.org/xml/properties/schema/external-schemaLocation",
                        schemaLocations.toString());
                }
                if(!noNamespaceSchemaLocation.equals(""))
                {
                    parser.setProperty("http://apache.org/xml/properties/schema/external-noNamespaceSchemaLocation",
                        noNamespaceSchemaLocation);
                }
                Document xmldocument=parser.build(new StringReader(document));
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0]+": document "+name+"successfully validated!\n Document: "+document.length()+" characters"+"\n"+debugInfo);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": document "+name+"successfully validated!");
                }
            } catch (JDOMParseException jpe)
            {
                XTTProperties.printFail(parameters[0]+": document "+name+"validation failed:\n"+jpe.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0]+": document "+name+"validation failed:\n"+e.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
    }

    /**
     * Send a document as HTTP POST to a URL.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the xml document to send, this can either be a real text document or the name of an document created with {@link #createDocument(java.lang.String[]) createDocument}.
     *                      <br><code>parameters[2]</code> argument is the name of the document to store the xml response to as if create with {@link #createDocument(java.lang.String[]) createDocument},
     *                      <br><code>parameters[3]</code> url to send the document to
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void sendPostRequest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendPostRequest: documentToSend documentForResponse url");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentToSend documentForResponse url");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XMLDocument xDoc=documentMap.get(parameters[1].trim().toLowerCase());
            String document=parameters[1];
            String receiveName=parameters[2].trim().toLowerCase();
            String name="";
            if(xDoc!=null)
            {
                document=xDoc.toString();
                name="'"+parameters[1].trim()+"' as ";
            }

            HTTPConnection connection=defaultConnection;

            try
            {
                XMLDocument xDocReceive=new XMLDocument();

                URL url = new URL(parameters[3]);
                XTTProperties.printInfo(parameters[0]+": sending "+name+"HTTP POST to: "+parameters[3]);

                connection.getRequestHeader().clear();
                
                Iterator<String> it=xDoc.sendHeader.keySet().iterator();
                String hkey;
                String hval;
                String contenttype=null;
                while(it.hasNext())
                {
                    hkey=it.next();
                    hval=xDoc.sendHeader.get(hkey);
                    if(hkey.equalsIgnoreCase("content-type"))contenttype=hval;
                    connection.getRequestHeader().put(hkey, hval);
                }
                if(contenttype==null)
                {
                    XTTProperties.printWarn(parameters[0] + ": no content-type set: using text/xml");
                    connection.getRequestHeader().put("content-type","text/xml");
                }

                byte[] data=ConvertLib.createBytes(document);
                connection.setPostDataBytes(data);

                connection.sendPostRequest(parameters[0],parameters[3]);

                xDocReceive.receiveHeader=connection.getResponseHeader();
                xDocReceive.serverResponseCode=new String[]{connection.getResponseCode()+"",connection.getResponseMessage()};
                xDocReceive.serverResponse=connection.getResponse();

                xDocReceive.setDocument(ConvertLib.createString(xDocReceive.serverResponse));

                //xDocReceive.certs = HTTPHelper.getCertificate(parameters[0],connection);
                documentMap.put(receiveName,xDocReceive);

            } catch (MalformedURLException mux)
            {
                XTTProperties.printFail(this.getClass().getName()+"."+parameters[0] + ": MalformedURLException");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": "+e.getClass().getName()+": "+e.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * Send a document as HTTP POST to a URL.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the the name of an document created with {@link #createDocument(java.lang.String[]) createDocument}.
     *                      <br><code>parameters[2]</code> argument is the name of the variable to store the document to
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void documentToVariable(String[] parameters)
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": documentToVariable: documentName variableName");
            return;
        }
        if(parameters.length==3)
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            XTTProperties.setVariable(parameters[2],document.toString());
            XTTProperties.printInfo(parameters[0]+": stored Document '"+name+"' to variable='"+parameters[2]+"'");
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
    }

    /**
     * set the http headers to be sent from the client to the server.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the document to send,
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void setHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setHeader: documentName headerFieldKey headerFieldValue");
            return;
        }
        if(parameters.length==4)
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] newparameters=new String[]{parameters[0],parameters[2],parameters[3]};
            HTTPHelper.setHeader(document.sendHeader,newparameters);
            XTTProperties.printInfo(parameters[0]+": Document '"+name+"' header '"+parameters[2]+"'='"+parameters[3]+"'");
        } else
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": documentName headerFieldKey headerFieldValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
    }

    /**
     * compare the http response code of the last POST/GET request with a value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the document recieved,
     *                     <br><code>parameters[2]</code> and following are the allowed response codes.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     *
     */
    public void checkResponseCode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkResponseCode: documentName expectedValue1 expectedvalue2 ...");
            return;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName expectedValue1 expectedvalue2 ...");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            boolean found=false;
            StringBuffer checked=new StringBuffer();
            String divider="";
            for(int i=2;i<parameters.length;i++)
            {
                if(document.serverResponseCode[0].equals(parameters[i]))
                {
                    found=true;
                }
                checked.append(divider+parameters[i]);
                divider=",";
            }

            if(found)
            {
                XTTProperties.printInfo(parameters[0] + ": document '"+name+"': "+document.serverResponseCode[0]+" "+document.serverResponseCode[1]);
            } else
            {
                XTTProperties.printFail(parameters[0] + ": document '"+name+"': "+document.serverResponseCode[0]+" expected "+checked.toString());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
        }
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the document recieved,
     *                      <br><code>parameters[2]</code> argument is the header key,
     *                      <br><code>parameters[3]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: documentName headerFieldKey");
            XTTProperties.printFail(this.getClass().getName()+": checkHeader: documentName headerFieldKey expectedValue");
            return;
        }
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] newparameters=null;
            if(parameters.length==4)
            {
                newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2],parameters[3]};
            } else 
            {
                newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2]};
            }
            HTTPHelper.checkHeader(document.receiveHeader,newparameters,false);
        }
    }
    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the document recieved,
     *                      <br><code>parameters[2]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[3]</code> argument is the header key,
     *                      <br><code>parameters[4]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void queryHeader(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryHeader: documentName variable headerFieldKey regularExpression");
            return;
        }
        if(parameters.length!=5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName variable headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2],parameters[3],parameters[4]};
            HTTPHelper.queryHeader(document.receiveHeader,newparameters,false);
        }
    }
    public void queryHeaderNegative(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryHeaderNegative: documentName headerFieldKey regularExpression");
            return;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName headerFieldKey regularExpression");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2],parameters[3]};
            HTTPHelper.queryHeader(document.receiveHeader,newparameters,true);
        }
    }
    /**
     * compare the http headers received by the client from the server with a value is prohibited.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the document recieved,
     *                     <br><code>parameters[2]</code> argument is the header key,
     *                     <br><code>parameters[3]</code> argument is the header value.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void checkHeaderNot(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkHeaderNot: documentName headerFieldKey expectedValue");
            return;
        }
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName headerFieldKey");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName headerFieldKey expectedValue");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name=parameters[1].trim().toLowerCase();
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            String[] newparameters=null;
            if(parameters.length==4)
            {
                newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2],parameters[3]};
            } else 
            {
                newparameters=new String[]{parameters[0]+": document '"+name+"'",parameters[2]};
            }
            HTTPHelper.checkHeader(document.receiveHeader,newparameters,true);
        }
    }

    /**
     * Saves the text content of the node selected by the XPath to a variable.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node,
     *                     <br><code>parameters[3]</code> argument is the variable to store the result to.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public void getNodeFromDocument(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getNodeFromDocument: documentName XPath Variable");
            return;
        }
        else if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPath Variable");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {         
            String name=parameters[1].trim().toLowerCase();
            XTTProperties.printInfo(parameters[0] + ": Getting " + parameters[2] + " from " + name);
            
            XMLDocument document=documentMap.get(name);
            if(document==null)
            {
                XTTProperties.printFail(parameters[0]+": document '"+name+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
                        
            String nodeData = null;
            try
            {
                nodeData = XTTXML.getElementViaXPath(parameters[2],document.document).getText();
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;                        
            }
            
            XTTProperties.setVariable(parameters[3],nodeData);
        }            
    }
    
    /**
     * Adds a namespace for use in addNode and addAttribute.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the namespace prefix,
     *                     <br><code>parameters[2]</code> argument is the namespace uri,
     *                     <br><code>parameters[3]</code> argument (optional) is the XML document to add the declaration to.
     *                     <br><code>parameters[3]</code> argument (optional) is the XPath to the node to add the declaration to.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void addNamespace(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addNamespace: prefix uri");
            XTTProperties.printFail(this.getClass().getName()+": addNamespace: prefix uri document XPathForDeclaration");
            return;
        }
        else if(parameters.length!=3 && parameters.length!=5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": prefix uri");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": prefix uri document XPathForDeclaration");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            XTTProperties.printInfo(parameters[0] + ": Adding new namespace " + parameters[1] + "<" + parameters[2] + ">");
            Namespace newNamespace = Namespace.getNamespace(parameters[1],parameters[2]);
            namespaces.put(parameters[1],newNamespace);
            if(parameters.length==5)
            {
                String name=parameters[3].trim().toLowerCase();
                XTTProperties.printInfo(parameters[0] + ": Adding " + parameters[1] + " namespace declaration to " + name);
                
                XMLDocument document=documentMap.get(name);
                
                Element parent = null;
                try
                {
                    parent = XTTXML.getElementViaXPath(parameters[4],document.document);
                }
                catch(NullPointerException npe)
                {
                    XTTProperties.printFail(parameters[0]+": path '"+parameters[4]+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;                        
                }                
                parent.addNamespaceDeclaration(newNamespace);
            }
        }        
    }
    
    /**
     * Adds a new node to the document.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node you want to add a new child to,
     *                     <br><code>parameters[3]</code> argument is the name of the new node.
     *                     <br><code>parameters[3]</code> argument (optional) is the text data to add to the node.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void addNode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addNode: documentName XPathOfParent elementName");
            XTTProperties.printFail(this.getClass().getName()+": addNode: documentName XPathOfParent elementName elementData");
            return;
        }
        else if(parameters.length<4 && parameters.length > 5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent elementName");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent elementName elementData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
            if(parameters.length>=5)
            {                
                XTTProperties.printInfo(parameters[0] + ": Adding " + parameters[3] + "<" + parameters[4] + "> to " + name);
            }
            else
            {
                XTTProperties.printInfo(parameters[0] + ": Adding " + parameters[3] + " to " + name);
            }
            
            XMLDocument document=documentMap.get(name);
            
            Element parent = null;

            parent = XTTXML.getElementViaXPath(parameters[2],document.document);
            if(parent==null)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;                    
            }

            XTTProperties.printInfo("Found parent " + parent.getName());
            String elementName = parameters[3];
            Namespace elementNameSpace = parent.getNamespace();
            //If we have a : in the name, then split the name and find the namespace.
            if(elementName.indexOf(":") != -1)
            {
                elementNameSpace=namespaces.get(elementName.substring(0,elementName.indexOf(":")));                
                if(elementNameSpace==null)
                {
                    XTTProperties.printFail(parameters[0]+": namespace '"+elementName.substring(0,elementName.indexOf(":"))+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                elementName = elementName.substring(elementName.indexOf(":")+1);
                XTTProperties.printDebug(parameters[0] + ": New name " + elementName);
            }
            
            Element newElement = new Element(elementName);
            newElement.setNamespace(elementNameSpace);
            if(parameters.length>=5)
            {
                newElement.addContent(parameters[4]);    
            }
            
            parent.addContent(newElement);        
        }        
    }
    
    /**
     * Updates an existing node of the document.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node you want to update,
     *                     <br><code>parameters[3]</code> argument is the new value of the node you want to update.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void updateNode(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": updateNode: documentName XPathOfNode elementData");
            return;
        }
        else if(parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfNode elementData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
           
            XMLDocument document=documentMap.get(name);
            
            Element currentNode = null;
            String oldValue = null;

            currentNode = XTTXML.getElementViaXPath(parameters[2],document.document);
            if(currentNode==null)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;                    
            }

            XTTProperties.printInfo("Found node for updation: '" + currentNode.getName()+"'");
            
            if(currentNode.getChildren() != null && currentNode.getChildren().size() > 0)
            {
            	XTTProperties.printFail("Node cannot be updated as it has child nodes");
            	XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            else
            {
            	oldValue = currentNode.getText();
            	currentNode.setText(parameters[3]);
            	XTTProperties.printInfo( currentNode.getName() +" changed from  '"+ oldValue + "' to '"+parameters[3]+ "'");
            }
        }
    }
    
    /**
     * Adds a new attribute to the document.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node you want to add a new child to,
     *                     <br><code>parameters[3]</code> argument is the name of the attribute.
     *                     <br><code>parameters[3]</code> argument is the text data to add to the attribute.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void addAttribute(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addAttribute: documentName XPathOfParent attributeName elementData");
            return;
        }
        else if(parameters.length!=5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent elementName");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent elementName elementData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
            if(parameters.length>=5)
            {                
                XTTProperties.printInfo(parameters[0] + ": Adding " + parameters[3] + "<" + parameters[4] + "> to " + name);
            }
            else
            {
                XTTProperties.printInfo(parameters[0] + ": Adding " + parameters[3] + " to " + name);
            }
            
            XMLDocument document=documentMap.get(name);
            
            Element parent = null;

            parent = XTTXML.getElementViaXPath(parameters[2],document.document);
            if(parent==null)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;                    
            }

            String attributeName = parameters[3];
            Namespace attributeNameSpace = null;
            //If we have a : in the name, then split the name and find the namespace.
            if(attributeName.indexOf(":") != -1)
            {
                attributeNameSpace=namespaces.get(attributeName.substring(0,attributeName.indexOf(":")));                
                if(attributeNameSpace==null)
                {
                    XTTProperties.printFail(parameters[0]+": namespace '"+attributeName.substring(0,attributeName.indexOf(":"))+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                attributeName = attributeName.substring(attributeName.indexOf(":")+1);
                XTTProperties.printDebug(parameters[0] + ": New name " + attributeName);
            }

            Attribute newAttribute = new Attribute(attributeName,parameters[4]);
            if(attributeNameSpace!=null)
            {
                newAttribute.setNamespace(attributeNameSpace);    
            }
            parent.setAttribute(newAttribute);
        }        
    }   
    
    /**
     * Update an attribute of node of the document.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node whose attribute you want to update,
     *                     <br><code>parameters[3]</code> argument is the name of the attribute.
     *                     <br><code>parameters[4]</code> argument is the text data to add to the attribute.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void updateAttribute(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": updateAttribute: documentName XPathOfNode attributeName attributeData");
            return;
        }
        else if(parameters.length!=5)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfNode attributeName attributeData");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
                        
            XMLDocument document=documentMap.get(name);
            
            Element currentNode = null;

            currentNode = XTTXML.getElementViaXPath(parameters[2],document.document);
            if(currentNode==null)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;                    
            }

            String attributeName = parameters[3];
            Attribute oldAttribute = null;
            Namespace attributeNameSpace = null;
            //If we have a : in the name, then split the name and find the namespace.
            if(attributeName.indexOf(":") != -1)
            {
                attributeNameSpace=namespaces.get(attributeName.substring(0,attributeName.indexOf(":")));                
                if(attributeNameSpace==null)
                {
                    XTTProperties.printFail(parameters[0]+": namespace '"+attributeName.substring(0,attributeName.indexOf(":"))+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                attributeName = attributeName.substring(attributeName.indexOf(":")+1);
                XTTProperties.printDebug(parameters[0] + ": New name " + attributeName);
            }
            if(attributeNameSpace != null)
            	oldAttribute = currentNode.getAttribute(attributeName, attributeNameSpace);
            else 
            	oldAttribute = currentNode.getAttribute(attributeName);
            
            if(oldAttribute == null)
            {
            	XTTProperties.printFail("attribute '"+ parameters[3]+ "' of node '"+currentNode.getName()+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return; 
            }
            else
            {
            	String oldAttributeValue = oldAttribute.getValue();
            	currentNode.removeAttribute(oldAttribute);
            	Attribute newAttribute = new Attribute(attributeName,parameters[4]);
                if(attributeNameSpace!=null)
                {
                    newAttribute.setNamespace(attributeNameSpace);    
                }
                currentNode.setAttribute(newAttribute);
                XTTProperties.printInfo("attribute '"+ parameters[3]+ "' of node '"+currentNode.getName()+"' changed from '"+oldAttributeValue +"' to '"+ parameters[4]+"'");
            }
        }        
    }    
    
    /**
     * Remove the first occurance of the selected node.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node you want to remove the child of,
     *                     <br><code>parameters[3]</code> argument is the name of the child node to remove.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */      
    public void removeChild(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": removeChild: documentName XPathOfParent childName");
            return;
        }
        else if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent childName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
            XTTProperties.printInfo(parameters[0] + ": Removing first " + parameters[3] + " from " + name);
            XMLDocument document=documentMap.get(name);
            Element parent = null;
            try
            {
                parent = XTTXML.getElementViaXPath(parameters[2],document.document);
                boolean deletionOccured = parent.removeChild(parameters[3]);
                if(deletionOccured) 
                {
                    XTTProperties.printDebug(parameters[0] + ": Succesfully deleted '" + parameters[3]);
                }
                else
                {
                    XTTProperties.printDebug(parameters[0] + ": No node deleted '" + parameters[3]);
                }
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);                       
            }            
        }
                    
    }
    /**
     * Removes all occurances of the selected node.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> argument is the XML document name,
     *                     <br><code>parameters[2]</code> argument is the XPath to the node you want to remove the child of,
     *                     <br><code>parameters[3]</code> argument is the name of the children nodes to remove.    
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public void removeChildren(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": removeChildren: documentName XPathOfParent childrenName");
            return;
        }
        else if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": documentName XPathOfParent childrenName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } 
        else
        {
            String name=parameters[1].trim().toLowerCase();
            XTTProperties.printInfo(parameters[0] + ": Removing all " + parameters[3] + " from " + name);
            XMLDocument document=documentMap.get(name);
            Element parent = null;
            try
            {
                parent = XTTXML.getElementViaXPath(parameters[2],document.document);
                boolean deletionOccured = parent.removeChildren(parameters[3]);
                if(deletionOccured) 
                {
                    XTTProperties.printDebug(parameters[0] + ": Succesfully deleted '" + parameters[3]);
                }
                else
                {
                    XTTProperties.printDebug(parameters[0] + ": No node deleted '" + parameters[3]);
                }                
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": path '"+parameters[2]+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);                       
            }            
        }     
    }

    public void initialize()
    {
        documentMap = new HashMap<String,XMLDocument>();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        defaultConnection=new HTTPConnection("DEFAULT","XML");
        defaultConnection.readConfiguration();
    }
    public String getConfigurationOptions()
    {
        return "    <!-- function module XML -->"
        +"\n    <XML>"
        +HTTPConnection.getConfigurationOptions()
        +"\n    </XML>";
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_XML.java,v 1.24 2010/05/05 08:12:38 rajesh Exp $";
}