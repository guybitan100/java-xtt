package com.mobixell.xtt;
/**
 * FunctionModule_XTT.
 * <p>
 * Functions for testing XTT consistency. This functions should not be used in real tests.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.22 $
 */
public class FunctionModule_XTT extends FunctionModule
{
    public FunctionModule_XTT()
    {

    }
    
    /**
     * Display System.getProperties().
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int viewAllProperties(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": viewAllProperties: no arguments");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {        
                XTTProperties.printInfo(parameters[0] + ": Properties:");
                System.getProperties().list(System.out);
            }
            catch(Exception e)
            {
            	return XTTProperties.FAILED_UNKNOWN;
            }
        } 
        return XTTProperties.PASSED;
    }

    /**
     * Resolve a hostname using the internal resolver in FunctionModule_DNS.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the hostname
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int testResolve(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": testResolve: hostname");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {        
                XTTProperties.printInfo(parameters[0] + ": DNS=\n" + ConvertLib.getHexView(DNSServer.resolveAddress(parameters[1])));
            }
            catch(Exception e)
            {
            	return XTTProperties.FAILED_UNKNOWN;
            }
        }     
        return XTTProperties.PASSED;
    }

    /**
     * Create a xml document.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the message to send to all hive members
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sendMessage(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendMessage: message");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {        
                XTTProperties.getHive().sendGlobalMesssage(parameters[1]);
            }
            catch(Exception e)
            {
            	return XTTProperties.FAILED_UNKNOWN;
            }
        }
        return XTTProperties.PASSED;
    }

    public int md4Test(String parameters[])
    {    
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": md4Test: unknown arguments");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {
                java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD4", "XTTCrypto");
                //Take MD4 of password to create NTLM hash
                XTTProperties.printInfo("Hashing: \n" + ConvertLib.getHexView(ConvertLib.createBytes("")));
                byte[] hash1 = md.digest(ConvertLib.createBytes(""));            
                XTTProperties.printInfo("MD4: \n" + ConvertLib.getHexView(hash1));
            }
            catch(Exception e)
            {
                XTTProperties.printException(e);  
                return XTTProperties.FAILED_UNKNOWN;
            }
        }
        return XTTProperties.PASSED;
    }

    public int arc4Test(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": arc4Test: unknown arguments");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            //NTLMHelper.crcTest(parameters[1]);
            byte[] encrypted = NTLMHelper.arc4Test(ConvertLib.getByteArrayFromHexString(parameters[1]),ConvertLib.getByteArrayFromHexString(parameters[2]));
            byte[] decrypted = NTLMHelper.arc4Test(ConvertLib.getByteArrayFromHexString(parameters[1]),encrypted);
            
            XTTProperties.printInfo("Encrypted: \n" + ConvertLib.getHexView(encrypted));
            XTTProperties.printInfo("Decrypted: \n" + ConvertLib.getHexView(decrypted));
            
        }
        return XTTProperties.PASSED;
    }


    public int ntlmTest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": ntlmTest: unknown arguments");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            byte[] randomSessionKey = ConvertLib.getByteArrayFromHexString("0xf3,0x53,0xbb,0x04,0x57,0xb8,0x38,0x74,0xd6,0xd3,0xd7,0xed,0x1b,0xc0,0xa2,0x54");
            randomSessionKey = NTLMHelper.encryptRandomSessionKey("4harvey!",randomSessionKey);
            XTTProperties.printInfo("Pass: \n" + ConvertLib.getHexView(randomSessionKey));
            String rspauth = "01000000000000006fa8a9be2f6f93fb";  
                              //01000000000000006fa8a9be2f6f93fb                      
            NTLMHelper.getSequenceNumberFromSignature(rspauth,randomSessionKey);
             
            //String buffer = "<NTLM><74f3b8c2><1><SIP Communications Service><polycom-u7aixgm.siptest1.austin.polycom.com><e9b34db69c3d4d218308a743e1e4c0a2><3><REGISTER><sip:mtucker@siptest1.austin.polycom.com><179c6061a5b044a888164e3a644570f1><74f5eaeadfe3d27fd2f241c4c9e0aae1><600><200>";
            String buffer = "<NTLM><a29d7294><1><SIP Communications Service><polycom-u7aixgm.siptest1.austin.polycom.com><6e526a89781849c796a06df3f74c7a8d><1><SUBSCRIBE><sip:mtucker@siptest1.austin.polycom.com><41bc2b060c08497194c30b19ae14f6e8><><>";
            NTLMHelper.createSignature(buffer,randomSessionKey,2l);

         }
        return XTTProperties.PASSED;
    }

    /**
     * Display the decoded data of a base64 encoded string.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the base64encoded data
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int base64DecodeToHexView(String parameters[])
    {
    	int status =  XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": base64DecodeToHexView: data");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": data");
            status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            byte[] decoding=ConvertLib.base64Decode(parameters[1]);
            XTTProperties.printInfo(parameters[0]+": Decoding");
            System.out.println("Decoded:\n"+ConvertLib.getHexView(decoding));
        }
        return status;
    }

    /**
     * Throw an exception.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int throwException(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": throwException:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {
                throw new Exception("This is the upper error",new java.io.IOException("This is the lower error"));
            }
            catch(Exception e)
            {
                XTTProperties.printInfo(parameters[0] + ": Testing exception printing");
                XTTProperties.printException(e);
                return XTTProperties.FAILED_UNKNOWN;
            }
        }
    }

    /**
     * Save the current configuration to disk.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> filename
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int saveConfiguration(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": saveConfiguration: filenamePrefix");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if(parameters.length == 3)
        {
            XTTProperties.printInfo(parameters[0] + ": saving global and local configuration to file");
            XTTConfiguration.dumpConfiguration(parameters[1],parameters[2]);
        }
        else
        {
            XTTProperties.printInfo(parameters[0] + ": saving global and local configuration to file");
            XTTConfiguration.dumpConfiguration();
        }
        return XTTProperties.PASSED;
    }

    /**
     * Stop the execution of the current thread till a button or enter key is pressed.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> the optional message to display
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int stop(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stop:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": stop: message");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            try
            {
                String message="Press enter to continue...";
                if(parameters.length>1)message=parameters[1];
                if(XTTProperties.getXTTGui()!=null)
                {
                    XTTProperties.getXTTGui().doWaitForButton(message);
                } else
                {
                    System.out.println(message);
                    System.in.read();
                    System.in.skip(System.in.available());
                }
            }
            catch(Exception e)
            {
                //
            }
        }
        return XTTProperties.PASSED;
    }
    

    /**
     * store a value in the configuration. Don't use, use setVariable instead or use external configuration file.
     *
     * @param parameters   array of String containing the parameters. <code>parameters[0]</code> argument is always
     *                     the method name, <code>parameters[1]</code> is the configuration node to use, <code>parameters[2]</code>  and following are concationated to the variable value
     *                     If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int store(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": store: variable data-to-store");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length == 2)
        {
            XTTProperties.printInfo(parameters[0] + ": Storing data to configuration "+ parameters[1]);
            XTTProperties.setProperty(parameters[1],null);
        }
        else if (parameters.length > 2)
        {
            StringBuffer val=new StringBuffer("");
            for(int i=2;i<parameters.length;i++)
            {
                val.append(parameters[i]);
            }
            XTTProperties.printInfo(parameters[0] + ": Storing data to configuration "+ parameters[1]);
            XTTProperties.setProperty(parameters[1],val.toString());
        }
        return XTTProperties.PASSED;
    }

    /**
     * Print Teststring in all trace levels.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, other parameters will be ignored
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int testTracing(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": testTracing:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            XTTProperties.printDebug(  parameters[0]+": This is DEBUG   Level Information");
            XTTProperties.printVerbose(parameters[0]+": This is VERBOSE Level Information");
            XTTProperties.printInfo(   parameters[0]+": This is INFO    Level Information");
            XTTProperties.printWarn(   parameters[0]+": This is WARN    Level Information");
            XTTProperties.printFail(   parameters[0]+": This is FAIL    Level Information");
        }
        return status;
    }

    public void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_XTT.java,v 1.22 2009/03/05 10:48:50 rsoder Exp $";
}