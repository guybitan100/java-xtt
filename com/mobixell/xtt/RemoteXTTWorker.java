package com.mobixell.xtt;

import java.net.Socket;
import java.io.PrintStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Vector;
import java.util.LinkedHashMap;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;

/**
* Handles the actual data for RemoteXTT.
* <p>
* Can handle multiple streams at once.
*/

public class RemoteXTTWorker extends Thread
{
    private Socket client = null;
    private boolean connectionAllowed = false;
    private int returnCode = 0;

    private Document responseXML = new Document(new Element("remote"));
    private Vector<Element> additionalResponseNodes = new Vector<Element>();
    private Document requestXML = null;

    private static ProcessBuilder processEnvironment = new ProcessBuilder("");

    private final static String CRLF="\r\n";

    private long networkLagDelay = 100; //The amount of time to wait before ending the stream read

    private int workerId = 0;
    
    private static Object lastKnownTestKey=new Object();
    private static String lastKnownTestName = "";
    private static String lastKnownClient = "";
    private static String lastKnownIP = "";
    private static int lastKnownTestTotal   = 0;
    private static int lastKnownTestNumber  = 0;
    private static int expectedTestTotal    = 0;
    private static int expectedTestCurrent  = 0;

    RemoteXTTWorker(Socket client, boolean connectionAllowed, int workerId)
    {
        this.workerId = workerId;
        this.client = client;
        this.connectionAllowed = connectionAllowed;
        try
        {
            this.client.setKeepAlive(true);
        }
        catch(Exception e){}
    }

    public void run()
    {
        StringBuffer response = new StringBuffer();
        PrintStream out = null;
        BufferedInputStream in = null;
        byte[] buf = new byte[0];
        try
        {
            //A 'ping' to try and delay the connection since the read may be cut off too early otherwise
            long time = System.currentTimeMillis();
            client.getInetAddress().isReachable(1000);
            time = System.currentTimeMillis() - time;
            networkLagDelay = time;
            if(networkLagDelay < 100)
                networkLagDelay = 100;

            XTTProperties.printInfo("["+workerId+"]:" +"Ping time to " + client.getInetAddress() + " was: " + time);

            out = new PrintStream( client.getOutputStream() );
            in = new BufferedInputStream(client.getInputStream(),65536);

            XTTProperties.printInfo("["+workerId+"]:" +"Starting stream read.");

            //buf = HTTPHelper.readStream("RemoteXTT",in,(int)networkLagDelay);

            LinkedHashMap<String,Vector<String>> header=HTTPHelper.readHTTPStreamHeaders("["+workerId+"]:",in);
            //End fo stream so break or invaild
            if(header.get(null)==null) throw new IOException("Invalid Protocol, no Method found");    
            if(header.get("transfer-encoding")!=null)
            {
                if(header.get("transfer-encoding").get(0).equalsIgnoreCase("chunked"))
                {
                    XTTProperties.printDebug("["+workerId+"]: chunked body found, unchunking");
                    buf=HTTPHelper.readChunkedBody("["+workerId+"]",in,header);
                }
            } else
            {
                Vector<String> contentlengthVector=header.get("content-length");
                if(contentlengthVector!=null)
                {
                    int contentlength=Integer.parseInt((String)contentlengthVector.get(0));
                    buf=new byte[contentlength];
                    HTTPHelper.readBytes(in,buf);
                    XTTProperties.printDebug("["+workerId+"]: read additional "+contentlength+" bytes as body");
                } else
                {
                    throw new IOException("Invalid Protocol, content-length not found");    
                }
            }
            	XTTProperties.printDebug("["+workerId+"]: Received Body:\n"+ConvertLib.getHexView(buf));
            	XTTProperties.printDebug("["+workerId+"]:" +"Finished reading stream from client.");

            if(!connectionAllowed)
            {
                writeResponse("IP NOT ALLOWED",out,500);
                //This should skip the rest, and close the ports, etc.
                throw new java.io.IOException("Client IP not allowed.");
            }

            org.jdom.input.SAXBuilder parser = new org.jdom.input.SAXBuilder();

            //This will be caught down below if it wasn't correct
            requestXML = parser.build(new ByteArrayInputStream(buf));

            Element currentNode = null;
            String currentNodeName = null;

            //Get all the children in the request node.
            List children = XTTXML.getElement("request",requestXML).getChildren();

            //Loop around all the children doing an action on each one.
            for (int j = 0; j<children.size();j++)
            {
                currentNode = (Element)children.get(j);
                currentNodeName = currentNode.getName();
                
                if(currentNodeName.equalsIgnoreCase("getTime"))
                {
                    long currentTime = System.currentTimeMillis()/1000l;
                    response.append(""+currentTime);
                    addAdditionalResponseNode("systemtime",""+currentTime);
                }
                //Check if the node name is 'execute'
                else if(currentNodeName.equalsIgnoreCase("execute"))
                {
                    String command = null;
                    int timeout = 0;

                    Element executeNode = XTTXML.getElement("request/execute/command",requestXML);
                    if(executeNode != null)
                    {
                        command = executeNode.getText();
                    }

                    executeNode = XTTXML.getElement("request/execute/timeout",requestXML);
                    if(executeNode != null)
                    {
                        try
                        {
                            timeout = Integer.parseInt(executeNode.getText());
                        }
                        catch(NumberFormatException nfe)
                        {
                            XTTProperties.printFail("["+workerId+"]:" +"The timeout '" + executeNode.getText() + "' wasn't a real number");
                            response.append("The timeout '" + executeNode.getText() + "' wasn't a real number");
                            setReturnCode(FAILEDRETURNCODE);
                            break;
                        }
                    }

                    response.append(executeCommand(command,timeout));
                }
                else if(currentNodeName.equalsIgnoreCase("writeFile"))
                {
                    String fileName = null;
                    String fileContent = null;
                    String fileMd5Digest = null;

                    Element fileNode = XTTXML.getElement("request/writeFile/fileName",requestXML);
                    if (fileNode == null)
                    {
                        response.append("Missing file name: Can't write file");
                        break;
                    }
                    else
                    {
                        fileName = fileNode.getText();
                    }

                    fileNode = XTTXML.getElement("request/writeFile/fileContent",requestXML);
                    if (fileNode != null)
                    {
                        fileContent = fileNode.getText();
                    }

                    fileNode = XTTXML.getElement("request/writeFile/fileMd5Digest",requestXML);
                    if (fileNode != null)
                    {
                        fileMd5Digest = fileNode.getText();
                    }

                    response.append(writeFile(fileName,fileContent,fileMd5Digest));
                }
                else if(currentNodeName.equalsIgnoreCase("readFile"))
                {
                    String fileName = null;

                    Element fileNode = XTTXML.getElement("request/readFile/fileName",requestXML);
                    if (fileNode == null)
                    {
                        response.append("Missing file name: Can't read file");
                        setReturnCode(FAILEDRETURNCODE);
                        break;
                    }
                    else
                    {
                        fileName = fileNode.getText();
                    }

                    response.append(readFile(fileName));
                }
                else if(currentNodeName.equalsIgnoreCase("listProducts"))
                {
                    response.append(listProductInfo());
                }
                else if(currentNodeName.equalsIgnoreCase("getStatus"))
                {
                    response.append(status());
                }
                else if(currentNodeName.equalsIgnoreCase("watchDog"))
                {
                    response.append(status());
                }
                else if(currentNodeName.equalsIgnoreCase("getnewestfile"))
                {
                    String directoryName = null;
                    String filter = "";

                    Element getNewestNode = XTTXML.getElement("request/getnewestfile/directory",requestXML);
                    if (getNewestNode == null)
                    {
                        response.append("Missing directory name");
                        break;
                    }
                    else
                    {
                        directoryName = getNewestNode.getText();
                    }

                    getNewestNode = XTTXML.getElement("request/getnewestfile/filter",requestXML);
                    if (getNewestNode != null)
                    {
                        filter = getNewestNode.getText();
                    }

                    response.append(getNewestFile(directoryName,filter));
                }     
                else if(currentNodeName.equalsIgnoreCase("whereami"))
                {
                    response.append(whereAmI());
                }  
                else if(currentNodeName.equalsIgnoreCase("stopRemoteXTT"))
                {
                    //FIX THIS! It's a stupid was of stopping!
                    response.append("Shutting down Remote XTT");
                }                                             
                else
                {
                    setReturnCode(FAILEDRETURNCODE);
                    response.append("Unknown tag '" + currentNodeName + "'");
                }
            }
            //Get all the children in the info node.
            Element infoNode=XTTXML.getElement("info",requestXML);
            if(infoNode!=null)
            {
                children = infoNode.getChildren();
    
                synchronized(lastKnownTestKey)
                {
                    if(header.get("user-agent")!=null)
                    {
                        lastKnownClient=header.get("user-agent").get(0);
                        lastKnownIP=""+client.getInetAddress();
                    }
                    String lastName=lastKnownTestName;
                    //Loop around all the children doing an action on each one.
                    for (int j = 0; j<children.size();j++)
                    {
                        currentNode = (Element)children.get(j);
                        currentNodeName = currentNode.getName();
                        
                        if(currentNodeName.equalsIgnoreCase("name"))
                        {
                            lastKnownTestName = currentNode.getText();
                        } else if(currentNodeName.equalsIgnoreCase("totaltests"))
                        {
                            lastKnownTestTotal=Integer.parseInt(currentNode.getText());
                        } else if(currentNodeName.equalsIgnoreCase("expectedtests"))
                        {
                            expectedTestTotal=Integer.parseInt(currentNode.getText());
                        } else if(currentNodeName.equalsIgnoreCase("currentest"))
                        {
                            lastKnownTestNumber=Integer.parseInt(currentNode.getText());
                        }
                    }
                    if(expectedTestTotal<1||lastKnownTestTotal>1)
                    {
                        expectedTestTotal=0;
                        expectedTestCurrent=0;
                    }
                    if(expectedTestTotal>1&&!(lastName==null)&&!lastName.equals(lastKnownTestName))
                    {
                        expectedTestCurrent++;
                    }
                    if(expectedTestCurrent>expectedTestTotal)
                    {
                        expectedTestTotal=lastKnownTestTotal;
                        expectedTestCurrent=lastKnownTestNumber;
                    }
                }
            }
            XTTProperties.printDebug("["+workerId+"]:" +"Sending response");

            writeResponse(response.toString(),out);

            out.close();
            //client.shutdownOutput();
            XTTProperties.printInfo("["+workerId+"]:" +"Finished sending response");

            //this.sleep(1000);
            XTTProperties.printInfo("["+workerId+"]:" +"All Done");

            if ((response != null) && (response.indexOf("Shutting down Remote XTT") != -1))
            {
                client.close();
                System.exit(0);
            }
        }
        catch(org.jdom.JDOMException je)
        {
	            XTTProperties.printDebug("["+workerId+"]:" +"Data recieved:\n" + ConvertLib.createString(buf) + "\n-End of data-");
	            writeResponse("XTT didn't send a proper request, invalid XML\nThis may be caused by lag",out,500);
	            XTTProperties.printFail("["+workerId+"]:" +"XTT didn't send a proper request");
	            XTTProperties.printException(je);
        }
        catch(java.io.IOException ioe)
        {
            XTTProperties.printFail("["+workerId+"]:" +"Warning: Missed client.");
            ioe.printStackTrace();
        }
        catch(NullPointerException npe)
        {
            npe.printStackTrace();
            XTTProperties.printFail("["+workerId+"]:" +"Warning: NullPointerException: No data from client recieved");
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            try
            {
                //Don't shut down, the client will shutdown when he's done reading.
                //XTTProperties.printDebug("Closing Socket");
                client.close();
                XTTProperties.printDebug("["+workerId+"]:" +"Closed Socket");
            }
            catch (Exception c)
            {
                c.printStackTrace();
            }
        }
    }

    private String executeCommand(String command, int timeout)
    {
        XTTProperties.printInfo("["+workerId+"]:" +"Running: executeCommand");
        Process process = null;
        InterrupterThread interrupter = null;
        StringBuffer output = new StringBuffer();
        StreamReaderThread inputReader = null;
        StreamReaderThread errorReader = null;

        XTTProperties.printInfo("["+workerId+"]:" +"Executing: " + command);
        try
        {
            //Always redirect the error stream to the standard output stream
            processEnvironment.redirectErrorStream(true);

            synchronized(processEnvironment)
            {
                //Do a .split so that the arguments get recognised properly.
//                processEnvironment.command(command.split(" "));
                processEnvironment.command("/bin/sh","-c",command);
                process = processEnvironment.start();
            }

            interrupter = new InterrupterThread(this,timeout);
            
            inputReader = new StreamReaderThread(process.getInputStream(),output,interrupter);
            errorReader = new StreamReaderThread(process.getErrorStream(),output,interrupter);
            
            inputReader.start();
            errorReader.start();
            interrupter.start();

            try
            {
                process.waitFor();
                XTTProperties.printInfo("["+workerId+"]:" +"The process finished, exit value:'"+process.exitValue()+"'");
                inputReader.waitFor();
                errorReader.waitFor();
                //while(!inputReader.finished() || !errorReader.finished())Thread.sleep(100);
                
            }
            catch(InterruptedException ie)
            {
                XTTProperties.printFail("["+workerId+"]:" +"The process was idle for too long, exiting");

                output.append("<!--Execution Interrupted By RemoteXTT-->");
            }
            /*This is done because of java bug #4784692
              See http://bugs.sun.com/bugdatabase/view_bug.do;jsessionid=22c0b2b16e8dd76d7a47c884546cb:WuuT?bug_id=4801027

              This doesn't seem to affect 1.5 after testing, but there's no harm in keeping this in*/

            finally
            {
                process.destroy();
                try
                {
                    process.getInputStream().close();
                	process.getOutputStream().close();
            	    process.getErrorStream().close();
                }
                catch(java.io.IOException ioe)
                {}                
            }
            interrupter.cancel();
            XTTProperties.printVerbose("["+workerId+"]:" +"Start of Program Output:\n" + output.toString() + "\nEnd of Program Output.");
            setReturnCode(process.exitValue());
        }
        catch (Exception e)
        {
            e.printStackTrace();
            output.append("" + e.getMessage() + "\n");
        }

        return output.toString();
    }

    private String writeFile(String fileName, String fileContents, String md5Digest)
    {
        XTTProperties.printInfo("["+workerId+"]:" +"Running: writeFile");
        StringBuffer returnResponse = new StringBuffer();
        //If we have the hash and no content, we should just see if the file exists and if it does compare the hashes
        if((md5Digest != null)&&(fileContents==null))
        {
            try
            {
                File myFile = new File(fileName);
                //If the file exists, check the hashes match
                if(myFile.exists())
                {
                    byte[] fileBytes = new byte[(int)myFile.length()];
                    BufferedInputStream fileIn = new BufferedInputStream(new java.io.FileInputStream(fileName));
                    fileIn.read(fileBytes);
                    fileIn.close();
                    java.security.MessageDigest md5Hash = java.security.MessageDigest.getInstance("MD5");

                    byte[] myDigest = md5Hash.digest(fileBytes);

                    //System.out.println(ConvertLib.getHexView(myDigest));
                    if(ConvertLib.getHexStringFromByteArray(myDigest).equalsIgnoreCase(md5Digest))
                    {
                        setReturnCode(PASSEDRETURNCODE);
                        returnResponse.append("writeFile: The files match\n");
                    }
                    else
                    {
                        setReturnCode(SPECIALRETURNCODE);
                        returnResponse.append("writeFile: The files are different, please send the content\n");
                    }
                }
                else
                {
                    setReturnCode(SPECIALRETURNCODE);
                    returnResponse.append("writeFile: No such file\n");
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
                setReturnCode(FAILEDRETURNCODE);
                returnResponse.append(""+e.getStackTrace()+"\n");
            }
        }
        //We got the content, we should write the file.
        else
        {
            byte[] binaryFileContents=new byte[0];
            /*We are going to decode the fileContents, because it should be base64.
              old tests will still send the data not base64 though, so we should be nice to them. */
            try
            {
                binaryFileContents = ConvertLib.base64Decode(fileContents);
            }
            catch(Exception e)
            {
                returnResponse.append("Error decoding file.\n");
            }
            XTTProperties.printVerbose("["+workerId+"]:" +">>>" + fileName + "<<<\n" + fileContents + "\n>>> EOF <<<");
            try
            {

                FileOutputStream fOut = new FileOutputStream(fileName,false);
                fOut.write(binaryFileContents);
                fOut.flush();
                fOut.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                setReturnCode(FAILEDRETURNCODE);
                XTTProperties.printFail("["+workerId+"]:" +"Counldn't write file");
                returnResponse.append("Couldn't write file\n");

                //This causes the next step to skip, if we already failed.
                md5Digest = null;
            }

            //If XTT send an MD5 hash, we should check our file is the same.
            if(md5Digest != null)
            {
                try
                {
                    File myFile = new File(fileName);
                    if(myFile.exists())
                    {
                        byte[] fileBytes = new byte[(int)myFile.length()];
                        BufferedInputStream fileIn = new BufferedInputStream(new java.io.FileInputStream(fileName));
                        fileIn.read(fileBytes);
                        fileIn.close();
                        
                        String newMd5Digest = ConvertLib.getHexMD5Hash(fileBytes);

                        XTTProperties.printVerbose("["+workerId+"]:" +">>" + newMd5Digest + "<< >>" + md5Digest + "<<");

                        if(newMd5Digest.equalsIgnoreCase(md5Digest))
                        {
                            setReturnCode(PASSEDRETURNCODE);
                            returnResponse.append("File written\n");
                        }
                        else
                        {
                            setReturnCode(FAILEDRETURNCODE);
                            returnResponse.append("Error writing file, written file didn't match the hash provided\n");
                        }
                    }
                    else
                    {
                        setReturnCode(FAILEDRETURNCODE);
                        returnResponse.append("writeFile: No such file\n");
                    }
                }
                catch (Exception e)
                {
                    setReturnCode(FAILEDRETURNCODE);
                    e.printStackTrace();
                    returnResponse.append(""+e.getStackTrace()+"\n");
                }
            }
            else
            {
                setReturnCode(PASSEDRETURNCODE);
                returnResponse.append("File written\n");
            }
        }
        XTTProperties.printInfo("["+workerId+"]:" +""+returnResponse.toString());
        return returnResponse.toString();
    }

    private String readFile(String fileName)
    {
        XTTProperties.printInfo("["+workerId+"]:" +"Running: readFile");
        StringBuffer returnResponse = new StringBuffer();

        File fileToRead = new File(fileName);
        if(fileToRead.exists())
        {
            try
            {
                int fileLength=Integer.parseInt(fileToRead.length()+"");
                byte fileContent[]=new byte[fileLength];

                java.io.FileInputStream stream=new java.io.FileInputStream(fileToRead);
                if(stream.available()>0)
                {
                    stream.read(fileContent);
                    addAdditionalResponseNode("filecontent",ConvertLib.base64Encode(fileContent));
                    addAdditionalResponseNode("fileMd5Digest",ConvertLib.getHexMD5Hash(ConvertLib.base64Encode(fileContent)));
                    returnResponse.append("File data in filecontent node");
                } else
                {
                    setReturnCode(FAILEDRETURNCODE);
                    returnResponse.append("Error reading " + fileName);
                }
            }
            //Since we already did a check to see if the file exists, this probably means we don't have permission to read it.
            catch (java.io.FileNotFoundException fnfe)
            {
                setReturnCode(FAILEDRETURNCODE);
                returnResponse.append("You don't have permission to read " + fileName);
            }
            catch (Exception e)
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printDebug("["+workerId+"]:" +"readFile("+fileName+"): ");
                    XTTProperties.printException(e);
                }
                setReturnCode(FAILEDRETURNCODE);
                returnResponse.append("Error reading " + fileName);
            }
        }
        else
        {
            returnResponse.append("File " + fileName + " doesn't exist.");
            setReturnCode(FAILEDRETURNCODE);
        }
        return returnResponse.toString();
    }

    private String getNewestFile(String directoryName, String filterText)
    {
        XTTProperties.printInfo("["+workerId+"]:" +"Running: getNewestFile");

        File folder;
        File[] files;
        try
        {
            folder = new File(directoryName);
            if(!folder.isDirectory())
            {
                XTTProperties.printFail("["+workerId+"]:" +"The directory '"+directoryName+"' you specified doesn't exist or isn't a directory");
                return "The directory '"+directoryName+"' you specified doesn't exist or isn't a directory";
            }
            else
            {
                int newest = 0;
                if(!filterText.equals(""))
                {
                    files = folder.listFiles(new SimpleFilenameFilter(filterText));
                    XTTProperties.printVerbose("["+workerId+"]:" +"getNewestFile: Using filter >>"+filterText+"<<");
                }
                else
                {
                    files = folder.listFiles();
                }
                if(files==null||files.length==0)
                {
                    XTTProperties.printFail("["+workerId+"]:" +"No files found with this filter!");
                    return "No files found with this filter!";
                }

                for(int i = 0;i<files.length;i++)
                {
                    if((files[i].isFile())&&(files[newest].lastModified()  < files[i].lastModified()))
                    {
                        newest = i;
                    }
                }

                XTTProperties.printInfo("["+workerId+"]:" +"Newest file was: " + files[newest]);
                addAdditionalResponseNode("newestfile",files[newest].getAbsolutePath());
                return "Newest file was: " + files[newest];
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            XTTProperties.printFail("["+workerId+"]:" +"The directory '"+directoryName+"' you specified doesn't exist or isn't a directory");
            return "The directory '"+directoryName+"' you specified doesn't exist or isn't a directory";
        }
    }

    private String listProductInfo()
    {
        StringBuffer response = new StringBuffer();
        Element products = XTTXML.getElement("products");

        addAdditionalResponseNode(products);
        response.append("See products node");

        return response.toString();
    }
    private String status()
    {
        StringBuffer response = new StringBuffer();
        Element status = new Element("status");
        String lastName=null;
        String lastClient=null;
        String lastIP=null;
        int lastTest=0;
        int lastTotal=0;
        synchronized(lastKnownTestKey)
        {
            lastName=lastKnownTestName;
            if(expectedTestTotal>0)
            {
                lastTest=expectedTestCurrent;
                lastTotal=expectedTestTotal;
            } else
            {
                lastTest=lastKnownTestNumber;
                lastTotal=lastKnownTestTotal;
            }                                    
            lastClient=lastKnownClient;
            lastIP=lastKnownIP;
        }
        Element lastTestTag = new Element("lastTest");
        Element nameTag = new Element("name");
        nameTag.addContent(lastName);
        lastTestTag.addContent(nameTag);
        Element currentTestTag = new Element("number");
        currentTestTag.addContent(""+lastTest);
        lastTestTag.addContent(currentTestTag);
        Element totalTestTag = new Element("total");
        totalTestTag.addContent(""+lastTotal);
        lastTestTag.addContent(totalTestTag);

        Element userTag = new Element("user");
        Element clientTag = new Element("client");
        clientTag.addContent(lastClient);
        userTag.addContent(clientTag);
        Element ipTag = new Element("ip");
        ipTag.addContent(""+lastIP);
        userTag.addContent(ipTag);
        
        Element todo = new Element("todo");
        todo.addContent("TODO: add more status information");
        
        status.addContent(todo);
        status.addContent(lastTestTag);
        status.addContent(userTag);

        addAdditionalResponseNode(status);
        response.append("See status node, last test: "+lastTest+"/"+lastTotal+" name: "+lastName);

        return response.toString();
    }

    private String whereAmI()
    {
        XTTProperties.printInfo("["+workerId+"]:" +"Running: whereAmI");
        File current = new File(".");
        addAdditionalResponseNode("whereiam",current.getAbsolutePath());
        
        return "" + current.getAbsolutePath();
    }

    private void addAdditionalResponseNode(String name, String data)
    {
        Element additionalElement = new Element(name);
        additionalElement.setText(data);
        additionalResponseNodes.add(additionalElement);
    }
    private void addAdditionalResponseNode(Element node)
    {
        additionalResponseNodes.add((Element)node.clone());
    }

    private void writeResponse(String response, PrintStream out)
    {
        writeResponse(response, out, 200);
    }
    private void writeResponse(String response, PrintStream out, int httpCode)
    {
        Element responseXMLTag = new Element("response");

        Element dataTag = new Element("data");
        dataTag.addContent(response);
        responseXMLTag.addContent(dataTag);

        Element returnCodeTag = new Element("returncode");
        returnCodeTag.addContent(""+returnCode);
        responseXMLTag.addContent(returnCodeTag);

        Element versionTag = new Element("version");
        versionTag.addContent(XTTProperties.getXTTBuildVersion());
        responseXMLTag.addContent(versionTag);

        for(Element additionalElement: additionalResponseNodes)
        {
            responseXMLTag.addContent(additionalElement);
        }

        responseXML.getRootElement().addContent(responseXMLTag);

        //XTT helper function to stream an XML document to a given stream
        //XTTXML.streamXML(responseXML,out);
        try
        {
            //XTT helper function to stream an XML document to a given stream
            byte[] dataToSend=ConvertLib.createBytes(XTTXML.stringXML(responseXML));
            String message="Ok";
            if(httpCode!=200)message="Internal Server Error";
            StringBuffer headers=new StringBuffer();
            
            headers.append("HTTP/1.0 "+httpCode+" "+message+CRLF);
            headers.append("date: "+HTTPHelper.createHTTPDate()+CRLF);
            headers.append("connection: close"+CRLF);
            headers.append("server: XTT/"+XTTProperties.getXTTBuildVersion()
                +" (testing; Java/"+System.getProperties().getProperty("java.vm.version")+"; "
                +System.getProperties().getProperty("os.name")+" "
                +System.getProperties().getProperty("os.arch")+" "
                +System.getProperties().getProperty("os.version")+"; "
                +System.getProperties().getProperty("user.name")+"; "
                +"RemoteXTT"+"; "
                +"$Revision: 1.21 $"
                +")"+CRLF);
            headers.append("content-type: text/xml; charset=UTF-8"+CRLF);
            headers.append("content-length: "+dataToSend.length+CRLF);
            headers.append(CRLF);
            
            byte[] headerToSend=ConvertLib.createBytes(headers.toString());
            out.write(headerToSend);
            out.write(dataToSend);
            out.flush();
        } catch (Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    private static final int PASSEDRETURNCODE = 0;
    private static final int FAILEDRETURNCODE = 1;
    private static final int SPECIALRETURNCODE = 2;


    private void setReturnCode(int code)
    {
        if((returnCode != PASSEDRETURNCODE)&&(code==PASSEDRETURNCODE))
        {
            XTTProperties.printWarn("["+workerId+"]:" +"Return code is " + returnCode + " not changing to '" + code + "'");
        }
        else
        {
            XTTProperties.printInfo("["+workerId+"]:" +"Setting return code to " + code);
            returnCode = code;
        }
    }

    public static final String tantau_sccsid = "@(#)$Id: RemoteXTTWorker.java,v 1.21 2011/03/21 10:30:39 rajesh Exp $";
}