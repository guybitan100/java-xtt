package com.mobixell.xtt;

import org.jdom.Document;
import org.jdom.Element;

import java.util.regex.Pattern;


/**
 * Allows tests to interact with RemoteXTT.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.59 $
 */
/**
 * @author guyb
 *
 */
public final class FunctionModule_Remote extends FunctionModule
{
    private static Document responseXML = null;

    private java.net.InetSocketAddress remoteAddress = null;

    private static String[][] processes = null; //NAME, STATUS, ROLE, PID, RSTRTS, MACHINE, STARTED
    private static String[][] products = null; //NAME, VERSION,
    private static String[][] machines = null; //NAME, STATUS

    private final static String showpro="show process";

    //This is more of the same fix for working on Windows. You may need to specify the SHELL to run on.
    //For example: MKS. This is included when you use the executeRemoteXMSCommand function.
    private String xmsShell=null;

    private String xmsPath=null;
    private String xms=null;

    private String xmsPrefix=null;
    private String xmsSuffix=null; //Set if you want to add something to the end of a command.

    private String stopProcessCommand=null;

    private static int XM26MODE=6;
    private static int XM25MODE=7;

    public FunctionModule_Remote() {
	initialize();
	}

	private static final class Machine
    {
        public static final int NAME = 0;
        public static final int STATUS = 1;
        private Machine(){}
    }

    private static final class Process
    {
        public static final int NAME    = 0;
        public static final int STATUS  = 1;
        public static final int ROLE    = 2;
        public static final int PID     = 3;
        public static final int RSTRTS  = 4;
        public static final int MACHINE = 5;
        public static final int STARTED = 6;
        private Process(){}
    }

    private static String getResponse()
    {
        Element dataElement = XTTXML.getElement("response/data",responseXML);
        String data = null;
        if(dataElement != null)
        {
            data = dataElement.getText();
        }
        return data;
    }

    private static int getReturnCode()
    {
        Element dataElement = XTTXML.getElement("response/returncode",responseXML);
        int returnCode = -1;
        //You don't need to do any more checks since it will just break the 'try' if it's invalid.
        try
        {
            returnCode = Integer.parseInt(dataElement.getText());
        }
        catch(Exception e)
        {
            XTTProperties.printFail("Remote: Error getting the return code");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            returnCode = -1;
        }
        return returnCode;
    }

    /**
     * Prints out the response sent from the last RemoteXTT request.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if no response is found.
     *
     * @param parameters No additional parameters needed
     */
    public int showResponse(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": showResponse:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(getResponse() != null)
            {
                XTTProperties.printInfo(parameters[0] + ": Response:\n"+getResponse());
            }
            else
            {
                XTTProperties.printFail(parameters[0] + ": No response found");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                
            }
        }
        return status;
    }

    public String[][] getProcesses()
    {
        return processes;
    }
    public static String[][] getProducts()
    {
        return products;
    }
    private static String[] getProcess(String name)
    {
        if ( processes == null )
            return null;

        Pattern processPattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
        for(int i=0;i<processes.length;i++)
        {
            if(processPattern.matcher(processes[i][Process.NAME]).find())
            {
                return processes[i];
            }
        }
        return null;
    }

    private static String[] getProcess( String name, String machine )
    {
        if(processes == null)
            return null;

        Pattern processPattern = Pattern.compile(name, Pattern.CASE_INSENSITIVE);
        for(int i = 0; i<processes.length; i++)
        {
           if( processPattern.matcher(processes[i][Process.NAME]).find() &&
                processes[i].length > Process.MACHINE && processes[i][Process.MACHINE].equalsIgnoreCase(machine) )
           {
                return processes[i];
           }

        }
        return null;
    }

    private static boolean isRunningProcess(String name)
    {
        String[] process = getProcess(name);
        if(process!=null && process[Process.STATUS]!=null)
        {
            return process[Process.STATUS].equals("running");
        }
        return false;
    }

    public void setProcesses()
    {
        if(xms == null)
        {
            XTTProperties.printFail("setProcesses: No XMS path info was configured, please set the remote/path.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return;
        }
        processes=null;
        String[] command = {"executeRemoteXMSCommand",showpro};
        executeRemoteXMSCommand(command);
        if(getResponse() != null)
        {
            String[] interim = getResponse().split("\\r\\n|\\n");

            if(interim.length < 2)
            {
                XTTProperties.printFail("setProcesses: incorrect data from xms show process\n\n"+getResponse());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }

            processes = new String[interim.length - 2][7];
            // this should be 6 for XMG26/XMG31 and 7 for XMG25 Branch
            int mode=interim[0].split("\\s+?\\*\\s++|\\s++").length;
            if(XTTProperties.printDebug(null))
            {
                if(mode==6)
                {
                    XTTProperties.printDebug("Mode: XM 2.6/3.1 ("+mode+")");
                } else if(mode==7)
                {
                    XTTProperties.printDebug("Mode: XM 2.5 ("+mode+")");
                }
            }
            String[] temp=null;
            String modecode="";

            for(int i = 0;i<processes.length;i++)
            {
                //XTTProperties.printDebug("Interim: "+interim[i+2]);
                temp = interim[i+2].split("\\s+?\\*\\s++|\\s++");
                processes[i] = new String[]{"","","","","","",""};
                if(mode==XM26MODE)
                {
                    if( temp.length == 4 )
                    {
                        //this means that ROLE and PID field is missing for this process
                        //shift all elements and set ROLE and PID to empty string
                        modecode="P6A";
                        processes[i][Process.STARTED] = "";// Not used in XMG26
                        processes[i][Process.MACHINE] = temp[Process.PID];
                        processes[i][Process.RSTRTS]  = temp[Process.ROLE];
                        processes[i][Process.PID]     = "";
                        processes[i][Process.ROLE]    = "";
                        processes[i][Process.STATUS]  = temp[Process.STATUS];
                        processes[i][Process.NAME]    = temp[Process.NAME];
                    }
                    else if( temp.length == 5 )
                    {
                        //this means that ROLE field is missing for this process,
                        //shift all elements and set ROLE to empty string
                        modecode="P6B";
                        processes[i][Process.STARTED] = "";// Not used in XMG26
                        processes[i][Process.MACHINE] = temp[Process.RSTRTS];
                        processes[i][Process.RSTRTS]  = temp[Process.PID];
                        processes[i][Process.PID]     = temp[Process.ROLE];
                        processes[i][Process.ROLE]    = "";
                        processes[i][Process.STATUS]  = temp[Process.STATUS];
                        processes[i][Process.NAME]    = temp[Process.NAME];
                    }
                    else if( temp.length == 6 ) // XM25 Build 66 and higher
                    {
                        //this means ALL fields are here,
                        modecode="P6C";
                        processes[i][Process.STARTED] = "";// Not used in XMG26
                        processes[i][Process.MACHINE] = temp[Process.MACHINE];
                        processes[i][Process.RSTRTS]  = temp[Process.RSTRTS];
                        processes[i][Process.PID]     = temp[Process.PID];
                        processes[i][Process.ROLE]    = temp[Process.ROLE];
                        processes[i][Process.STATUS]  = temp[Process.STATUS];
                        processes[i][Process.NAME]    = temp[Process.NAME];
                    }
                } else if(mode==XM25MODE)
                {
                    if( temp.length == 6 ) // XM25 Build 66 and higher
                    {
                        //this means that ROLE and PID field is missing for this process
                        //shift all elements and set ROLE and PID to empty string
                        modecode="P5A";
                        processes[i][Process.MACHINE] = temp[Process.PID+2];
                        processes[i][Process.STARTED] = temp[Process.PID]+" "+temp[Process.PID+1];
                        processes[i][Process.RSTRTS] = temp[Process.ROLE];
                        processes[i][Process.PID] = "";
                        processes[i][Process.ROLE] = "";
                        processes[i][Process.STATUS] = temp[Process.STATUS];
                        processes[i][Process.NAME] = temp[Process.NAME];
                    }
                    else if( temp.length == 7 ) // XM25 Build 66 and higher
                    {
                        //this means that ROLE field is missing for this process,
                        //shift all elements and set ROLE to empty string
                        modecode="P5B";
                        processes[i][Process.MACHINE] = temp[Process.RSTRTS+2];
                        processes[i][Process.STARTED] = temp[Process.RSTRTS]+" "+temp[Process.RSTRTS+1];
                        processes[i][Process.RSTRTS] = temp[Process.PID];
                        processes[i][Process.PID] = temp[Process.ROLE];
                        processes[i][Process.ROLE] = "";
                        processes[i][Process.STATUS] = temp[Process.STATUS];
                        processes[i][Process.NAME] = temp[Process.NAME];
                    }
                    else if( temp.length == 8 ) // XM25 Build 66 and higher
                    {
                        //this means ALL fields are here,
                        modecode="P5C";
                        processes[i][Process.MACHINE] = temp[Process.MACHINE+2];
                        processes[i][Process.STARTED] = temp[Process.MACHINE]+" "+temp[Process.MACHINE+1];
                        processes[i][Process.RSTRTS] = temp[Process.RSTRTS];
                        processes[i][Process.PID] = temp[Process.PID];
                        processes[i][Process.ROLE] = temp[Process.ROLE];
                        processes[i][Process.STATUS] = temp[Process.STATUS];
                        processes[i][Process.NAME] = temp[Process.NAME];
                    }
                } else
                {
                    XTTProperties.printFail("setProcesses: Not correct output from xms show process mode="+mode);
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return;
                }
                if(XTTProperties.printDebug(null))
                {
                    System.out.println(modecode+": 0:"+processes[i][Process.NAME]
                                                  +" 1:"+processes[i][Process.STATUS]
                                                  +" 2:"+processes[i][Process.ROLE]
                                                  +" 3:"+processes[i][Process.PID]
                                                  +" 4:"+processes[i][Process.RSTRTS]
                                                  +" 5:"+processes[i][Process.MACHINE]
                                                  +" 6:"+processes[i][Process.STARTED]
                                        );
                }
            }
        }
    }
    public boolean setProductsVersion()
    {
        products=null;
        String command =     "echo  \"XMP version: `" + xmsPath + "/bin/xms sh ver | head -1 | awk -F' ' '{ print $4 }'`\";" +
			        		 "echo  \"Interceptor version: `rpm -qa| grep intercept | cut -c13-`\";" +
			        		 "echo  \"Titanium version: `cd /opt/titanium/bin/; ./tiversion tiversion | cut -d\" \" -f5`\";" +
			        		 "echo  \"Base plugin: `rpm -qa| grep mx_p`\";" +
			        		 "echo  \"Video plugin: `rpm -qa| grep mx_v`\";echo  \"Ad plugin: `rpm -qa| grep mx_ad`\";" +
			        		 "echo  \"OS: `cat /etc/redhat-release`\";" +
			        		 "echo  \"Kernel version: `uname -a | cut -d\" \" -f3`\";" +
			        		 "echo  \"Hostname: `hostname`\"";
		try {
			executeRemoteCommand(command,true);
			if (getResponse() != null) 
			{
				String[] interim = getResponse().split("\\r\\n|\\n");

				products = new String[interim.length][2];

				for (int i = 0; i < products.length; i++) 
				{
					products[i] = new String[] { "", "" };
					products[i][0] = interim[i].split(":")[0].trim();
					products[i][1] = interim[i].split(":")[1].trim();
				}
			}
			else return false;
		} 
		catch (Exception e) 
		{
			return false;
		}
		return true;
	}
    /**
     * Saves the name of the active process to the given variable. variable/length contains the number of found processes, variable/0 to n contain the process name. variable laways contaisn the last found process.
     * <p>
     *
     * @param parameters 1) the name of the process to check, 2) the variable name to store the result to.
     */
    public int activeProcessToVar(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": activeProcessToVar: processName variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": processName variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String processname="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            int length=0;
            String varname=parameters[2];
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            StringBuffer output=new StringBuffer(parameters[0]+": Active processes saved for "+parameters[1]);
            for(int i=0;i<processes.length;i++)
            {
                if( processPattern.matcher(processes[i][Process.NAME]).find()
                  && (processes[i][Process.ROLE].equals("")||processes[i][Process.ROLE].startsWith("A")))
                {
                    processname=processes[i][Process.NAME];
                    output.append("\n  "+varname+"/"+ConvertLib.addSuffixToString(""+length,6," ")+" = "+processname);
                    XTTProperties.setVariable(varname+"/"+(length++),processname);
                }
            }
            output.append("\n  "+varname+"/length = "+length);
            XTTProperties.setVariable(varname,processname);
            XTTProperties.setVariable(varname+"/length",length+"");
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printInfo(output.toString());
            } else
            {
                XTTProperties.printInfo(parameters[0]+": "+length+" active processes saved for "+parameters[1]);
            }
        }
        return status;
    }
    /**
     * Saves the name of the active process to the given variable. variable/length contains the number of found processes, variable/0 to n contain the process name. variable laways contaisn the last found process.
     * <p>
     *
     * @param parameters 1) the name of the process to check, 2) the variable name to store the result to.
     */
    public int activeProcessPIDToVar(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": activeProcessPIDToVar: processName variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": processName variableName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String processPid="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            int length=0;
            String varname=parameters[2];
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                if( processPattern.matcher(processes[i][Process.NAME]).find()
                  && (processes[i][Process.ROLE].equals("")||processes[i][Process.ROLE].startsWith("A")))
                {
                    processPid=processes[i][Process.PID];
                    XTTProperties.setVariable(varname+"/"+(length++),processPid);
                }
            }
            XTTProperties.printInfo(parameters[0]+": "+parameters[2]+"="+processPid);
            XTTProperties.setVariable(varname,processPid);
            XTTProperties.setVariable(varname+"/length",length+"");
        }
        return status;
    }

    /**
     * Checks if the given process is in one of the given states.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if the states aren't matched.
     *
     * @param parameters 1) The process to check 2) the allowed state n) additional allowed states.
     */
    public int checkProcessStatus(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkProcessStatus: processName allowedStates1 ...");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": processName allowedStates1 ...");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String processstate="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            java.util.HashSet<String> allowedStates=new java.util.HashSet<String>();
            for(int i=2;i<parameters.length;i++)
            {
                allowedStates.add(parameters[i].toLowerCase());
            }

            boolean found=false;
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                //XTTProperties.printDebug(parameters[0] + ": Process: "+processes[i][Process.NAME]+" "+parameters[1]);
                if(processPattern.matcher(processes[i][Process.NAME]).find())
                {
                    processstate=processes[i][Process.STATUS].toLowerCase();
                    if(!allowedStates.contains(processstate))
                    {
                        XTTProperties.printFail(parameters[0] + ": Process "+parameters[1]+": unallowed status: "+processstate+" allowed: "+allowedStates);
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);
                        found=true;
                    } else
                    {
                        XTTProperties.printInfo(parameters[0] + ": Process "+parameters[1]+": status: "+processstate+" allowed: "+allowedStates);
                        found=true;
                    }
                }
            }
            if(!found)
            {
                XTTProperties.printFail(parameters[0] + ": Process "+parameters[1]+" not found");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Checks that all processes are in one of the given states.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if the states aren't matched.
     *
     * @param parameters 1) the allowed state n) additional allowed states.
     */
    public int checkAllProcessesStatus(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkAllProcessesStatus: allowedStates1 ...");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": allowedStates1 ...");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String processstate="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            java.util.HashSet<String> allowedStates=new java.util.HashSet<String>();
            for(int i=1;i<parameters.length;i++)
            {
                allowedStates.add(parameters[i].toLowerCase());
            }

            XTTProperties.printInfo(parameters[0] +" ("+processes.length+"): allowed: "+allowedStates);
            boolean found=false;
            for(int i=0;i<processes.length;i++)
            {
                processstate=processes[i][Process.STATUS].toLowerCase();
                if(!allowedStates.contains(processstate))
                {
                    XTTProperties.printFail(processes[i][Process.NAME]+": UNALLOWED status: "+processstate);
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                    found=true;
                } else
                {
                    XTTProperties.printInfo(processes[i][Process.NAME]+": status: "+processstate);
                    found=true;
                }
            }
            if(!found)
            {
                XTTProperties.printFail(parameters[0] + ": No Process found, hey, how could you get here????");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            XTTProperties.printInfo(parameters[0] +" ("+processes.length+"): END");
        }
        return status;
    }

    /**
     * Saves the number of restarts of the given process to a variable.
     * <p>
     *
     * @param parameters 1) The process to query 2) the variable to store the restarts to.
     */
    public int getProcessRestarts(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getProcessRestarts: processName variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": processName variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String restarts="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }

            boolean found=false;
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                //XTTProperties.printDebug(parameters[0] + ": Process: "+processes[i][Process.NAME]+" "+parameters[1]);
                if(processPattern.matcher(processes[i][Process.NAME]).find())
                {
                    restarts = processes[i][Process.RSTRTS];
                    XTTProperties.printInfo(parameters[0] + ": Process "+parameters[1]+" has " +restarts+ " restarts (storing to '"+parameters[2]+"'");
                    XTTProperties.setVariable(parameters[2],restarts);
                    found=true;
                }
            }
            if(!found)
            {
                XTTProperties.printFail(parameters[0] + ": Process "+parameters[1]+" not found");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Checks the number of restarts of the given process.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if the states aren't matched.
     *
     * @param parameters 1) The process to query 2) the variable to store the restarts to.
     */
    public int checkProcessRestarts(String[] parameters)
    {
    	
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkProcessRestarts: processName restartNum");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": processName restartNum");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String restarts="";
            setProcesses();
            if(processes==null)
            {
                XTTProperties.printFail(parameters[0] + ": no processlist found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }

            boolean found=false;
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                //XTTProperties.printDebug(parameters[0] + ": Process: "+processes[i][Process.NAME]+" "+parameters[1]);
                if(processPattern.matcher(processes[i][Process.NAME]).find())
                {
                    restarts = processes[i][Process.RSTRTS];
                    if(!restarts.equals(parameters[2]))
                    {
                        XTTProperties.printFail(parameters[0] + ": Process "+parameters[1]+": Incorrect restarts: " +restarts+ " != " + parameters[2]);
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);
                        found=true;
                    }
                    else
                    {
                        XTTProperties.printInfo(parameters[0] + ": Process "+parameters[1]+" has " +restarts+ " restarts");
                        found=true;
                    }
                }
            }
            if(!found)
            {
                XTTProperties.printFail(parameters[0] + ": Process "+parameters[1]+" not found");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * restarts all processes of specified "type" on all machines, one by one
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if the states aren't matched.
     *
     * @param parameters - process to restart
     * @return boolean
     */
    public int restartProcess(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": restartProcess: processName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": restartProcess processName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {

            setProcesses(); //Get newest process information
            XTTProperties.printDebug(this.getClass().getName()+": restartProcess " + parameters[1]);

            String process[] = getProcess(parameters[1]);
            if(process == null)
            {
                XTTProperties.printFail(this.getClass().getName()+": No such process to restart");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
            String[] command1 = {"executeRemoteXMSCommand",stopProcessCommand + process[0]};

            int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
            if(expectedStops == -1)
            {
                expectedStops = 0; //If it's not set, set it
            }
            expectedStops++;

            XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

            executeRemoteXMSCommand(command1);

            String[] command2 = {"executeRemoteXMSCommand","start process " + process[0]};
            executeRemoteXMSCommand(command2);
            do
            {
                setProcesses(); //Get newest process information
                process = getProcess(parameters[1]);
                XTTProperties.printDebug("Process "+parameters[1]+" still transient");
            }
            while((process!=null)&&(process.length > 2)&&(process[Process.ROLE].startsWith("T")));

        }
        return status;
    }


    /**
     * restarts all processes of specified "type" on all machines, one by one.
     * <p>
     *
     * @param parameters - process to restart
     * @return boolean
     */
    public int restartProcesses(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if (parameters == null)
        {
            XTTProperties.printFail( this.getClass().getName() + ": restartProcesses: processName" );
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": restartProcesses processName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            setMachines(); // get the latest machine status
            setProcesses(); //Get newest process information

            XTTProperties.printDebug( this.getClass().getName() + ": restartProcesses " + parameters[1] );

            for( int j = 0; j < machines.length; j++ )
            {
                if( machines[j][Machine.STATUS].equals( "Connected" ) )
                {
                    /* parameters[0] - function name
                     * parameters[1] - process name
                     */
                    String[] process = getProcess( parameters[1], machines[j][Machine.NAME] );
                    if( process == null )
                    {
                        XTTProperties.printWarn( this.getClass().getName() + ": No such process to restart" );
                        XTTProperties.setTestStatus( XTTProperties.FAILED_WITH_INVALID_ARGUMENTS );
                        return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    }
                    String[] command1 = { "executeRemoteXMSCommand",stopProcessCommand + process[Process.NAME] };

                    int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                    if(expectedStops == -1)
                    {
                        expectedStops = 0; //If it's not set, set it
                    }
                    expectedStops++;
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                    executeRemoteXMSCommand( command1 );

                    String[] command2 = { "executeRemoteXMSCommand","start process " + process[Process.NAME] };
                    executeRemoteXMSCommand( command2 );
                    do
                    {
                        setProcesses(); //Get newest process information
                        process = getProcess( parameters[1], machines[j][Machine.NAME] );
                        XTTProperties.printDebug("Process "+parameters[1]+" still transient" );
                    }
                    while ( ( process != null ) && ( process.length > 2 ) && ( process[Process.ROLE].startsWith( "T" ) ) );
                }
            }
        }
        return status;
    }

    /**
     * stops the given process.
     * <p>
     *
     * @param parameters - 1) process to stop 2) machine the process is on
     * @return boolean
     */
    public int stopProcess(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        boolean isMachine = false;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopProcess: processName");
            XTTProperties.printFail(this.getClass().getName()+": stopProcess: processName machineName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if( parameters.length == 3 )
        {
            isMachine = true;
        }

        if( parameters.length == 2 || parameters.length == 3 )
        {
            XTTProperties.printDebug( this.getClass().getName() + ": stopProcess " + parameters[1] );

            setProcesses(); //Get newest process information
            String process[]=null;
            if (isMachine)
            {
                setMachines(); //Get the newest machine info
                process = getProcess(parameters[1],parameters[2]);
            } else
            {
                process = getProcess(parameters[1]);
            }

            if (process == null)
            {
                XTTProperties.printWarn( this.getClass().getName() + ": No such process to stop" );
                XTTProperties.setTestStatus( XTTProperties.FAILED_WITH_INVALID_ARGUMENTS );
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }

            String[] command1 = { "executeRemoteXMSCommand",stopProcessCommand + process[Process.NAME] };

            int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
            if(expectedStops == -1)
            {
                expectedStops = 0; //If it's not set, set it
            }
            expectedStops++;
            XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

            executeRemoteXMSCommand( command1 );
        } else
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName");
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName machineName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }

    /**
     * Stops all processes of specified "type" on all machines, one by one.
     * <p>
     *
     * @param parameters - processes to start
     * @return boolean
     */
    public int stopProcesses(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopProcesses: processName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        /*
         * parameters[0] functions name
         * parameters[1] process name
         */
        if( parameters.length == 2)
        {
            XTTProperties.printInfo(parameters[0]+": all processes match " + parameters[1] );

            setProcesses(); //Get newest process information

            String[] command1=null;
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                if(processPattern.matcher(processes[i][Process.NAME]).find())
                {
                    command1 = new String[]{ "executeRemoteXMSCommand",stopProcessCommand+(processes[i][Process.NAME])};
                    int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                    if(expectedStops == -1)
                    {
                        expectedStops = 0; //If it's not set, set it
                    }
                    expectedStops++;
                    XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);
                    executeRemoteXMSCommand(command1);
                }
            }
        } else
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;

    }

    /**
     * Starts the given process.
     * <p>
     *
     * @param parameters - 1) process to start 2) the machine the process is on
     * @return boolean
     */
    public int startProcess(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        boolean isMachine = false;
        if ( parameters == null )
        {
            XTTProperties.printFail( this.getClass().getName() + ": startProcess: processName" );
            XTTProperties.printFail( this.getClass().getName() + ": startProcess: processName machineName" );
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if ( parameters.length == 3 )
        {
            isMachine = true;
        }
        /*
         * parameters[0] functions name parameters[1] process name parameters[2] machine name
         */
        if ( parameters.length == 2 || parameters.length == 3 )
        {
            if ( isMachine )
                setMachines(); //Get the newest machine info
            setProcesses(); //Get newest process information
            XTTProperties.printDebug( this.getClass().getName() + ": startProcess " + parameters[1] );
            for ( int i = 1; i < parameters.length; i++ )
            {
                String process[] = isMachine ? getProcess( parameters[1], parameters[2] ) : getProcess( parameters[1] );
                if ( process == null )
                {
                    XTTProperties.printWarn( this.getClass().getName() + ": No such process to start" );
                    XTTProperties.setTestStatus( XTTProperties.FAILED_WITH_INVALID_ARGUMENTS );
                    return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                }
                String[] command1 = { "executeRemoteXMSCommand","start process " + process[Process.NAME] };
                executeRemoteXMSCommand( command1 );
                do
                {
                    setProcesses(); //Get newest process information
                    process = isMachine ? getProcess( parameters[1], parameters[2] )
                                        : getProcess( parameters[1] );
                    XTTProperties.printDebug("Process "+parameters[1]+" still transient" );
                }
                while ( ( process != null ) && ( process.length > 2 ) && ( process[Process.ROLE].startsWith("T")));
            }
        } else
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName");
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName machineName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }

    /**
     * Starts all processes of specified "type" on all machines, one by one.
     * <p>
     *
     * @param parameters - processes to start
     * @return boolean
     */
    public int startProcesses(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if ( parameters == null )
        {
            XTTProperties.printFail( this.getClass().getName() + ": startProcesses: processName" );
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }

        if ( parameters.length == 2)
        {
            setProcesses(); //Get newest process information
            XTTProperties.printInfo( parameters[0] +": all processes matching " + parameters[1] );
            String[] command1=null;
            Pattern processPattern = Pattern.compile(parameters[1], Pattern.CASE_INSENSITIVE);
            for(int i=0;i<processes.length;i++)
            {
                if(processPattern.matcher(processes[i][Process.NAME]).find())
                {
                    command1 = new String[]{ "executeRemoteXMSCommand","start process " + processes[i][Process.NAME] };
                    executeRemoteXMSCommand( command1 );
                }
            }
            boolean exit=false;
            while(!exit)
            {
                exit=true;
                setProcesses(); //Get newest process information
                for(int i=0;i<processes.length;i++)
                {
                    if(processPattern.matcher(processes[i][Process.NAME]).find()&&(processes[i][Process.ROLE].startsWith("T")))
                    {
                        XTTProperties.printVerbose( parameters[0] +": process "+processes[i][Process.NAME]+" still Transient" );
                        exit=false;
                    }
                }
            }
        } else
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        }
        return status;
    }


    public int killOnEvent(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": killOnEvent: processName variable amount");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }

        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName variable amount");
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            XTTProperties.printInfo(parameters[0]+": killing " + parameters[1] + " when " + parameters[2] + " is greater than " + parameters[3]);
            setProcesses();
            String process[] = getProcess(parameters[1]);
            new AssassinThread(process[3],parameters[2],parameters[3],0).start();
        }
        return status;
    }

    public int runOnEvent(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
    	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": runOnEvent: remoteCommand variable amount");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }

        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": remoteCommand variable amount");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            XTTProperties.printInfo(parameters[0]+": running " + parameters[1] + " when " + parameters[2] + " is greater than " + parameters[3]);
            new AssassinThread(parameters[1],parameters[2],parameters[3],1).start();
        }
        return status;
    }

    /**
     * Runs a kill -9 on a process or a PID.
     * <p>
     *
     * @param parameters - 1) Process or PID to kill 2) set to true if (1) is a PID (defaults to false)
     * @return boolean
     */
    public int killProcess(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": killProcess: processName");
            XTTProperties.printFail(this.getClass().getName()+": killProcess: processName isPID");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length==2||parameters.length==3)
        {
            if((parameters.length == 3)&&ConvertLib.textToBoolean(parameters[2]))
            {
                String[] command1 = {"executeRemoteCommand","kill -9 " + parameters[1]};
                executeRemoteCommand(command1);
            } else
            {
                setProcesses(); //Get newest process information
                XTTProperties.printDebug(this.getClass().getName()+": killProcess " + parameters[1]);
                String process[] = getProcess(parameters[1]);
                if(process == null)
                {
                    XTTProperties.printFail(this.getClass().getName()+": No such process to kill");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
                }
                String[] command1 = {"executeRemoteCommand","kill -9 " + process[3]};

                int expectedStops = XTTProperties.getIntVariable("snmp/expectedstops");
                if(expectedStops == -1)
                {
                    expectedStops = 0; //If it's not set, set it
                }
                expectedStops++;
                XTTProperties.printDebug("" +  expectedStops);
                XTTProperties.setVariable("snmp/expectedstops",""+expectedStops);

                executeRemoteCommand(command1);
                do
                {
                    setProcesses(); //Get newest process information
                    process = getProcess(parameters[1]);
                    XTTProperties.printDebug("Process "+parameters[1]+" still transient");
                }
                while((process!=null)&&(process.length > 2)&&(process[2].startsWith("T")));
            }
        } else
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName");
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": processName isPID");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        return status;
    }

    /**
     * gathers machine information, by running xms show machine command
     */
    private int setMachines()
    {
    	int status = XTTProperties.PASSED;
        if(xms == null)
        {
            XTTProperties.printFail("setProcesses: No XMS path info was configured, please set the remote/path.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return XTTProperties.FAILED;
        }

        String[] command = { "executeRemoteXMSCommand","show machine" };
        executeRemoteXMSCommand( command );
        if( getResponse() != null )
        {
            String[] interim = getResponse().split( System.getProperty( "line.separator" ) );
            if(interim.length==1)
            {
                machines=null;
            } else
            {
                machines = new String[ interim.length - 2 ][ 2 ];
                for( int i = 0; i < machines.length; i++ )
                {
                    machines[ i ] = interim[ i + 2 ].split( "\\s+" );
                }
            }
        }
        return status;
    }

    /**
     *  gets machine information by name
     */
    private String[] getMachine( String name )
    {
        if( machines == null )
            return null;

        Pattern machinePattern = Pattern.compile(name);
        for(int i=0;i<machines.length;i++)
        {
            if( (machinePattern.matcher(machines[i][Machine.NAME]).find()) || (machines[i][Machine.NAME].toLowerCase().startsWith(name.toLowerCase())) )
            {
                return machines[i];
            }
        }
        return null;
    }

    /**
     * Gets the name of the current running product.
     * <p>
     * Does a xms show configuration then runs ".*?Product name:\\s*?(\\p{Alnum}++)\\s*+" on it and saves the result to the variable.
     *
     * @param parameters - variable to store the product name to.
     * @return boolean
     */
    public int getProduct( String[] parameters )
    {
    	int status = XTTProperties.PASSED;
        if( parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getProduct: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
            
        } else if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else if(xms == null)
        {
            XTTProperties.printFail("setProcesses: No XMS path info was configured, please set the remote/path.");
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            return XTTProperties.FAILED;
        } else
        {
            String[] command1 = {"executeRemoteXMSCommand","show configuration"};
            executeRemoteXMSCommand( command1 );
            ConvertLib.queryString("Remote",getResponse(),".*?Product name:\\s*?(\\p{Alnum}++)\\s*+",parameters[1]);
        }
        return status;
    }

    /**
     * Does an xms start machine of the given machine.
     * <p>
     *
     * @param parameters - name of the machine to start.
     * @return boolean
     */
    public int startMachine( String[] parameters )
    {
    	int status = XTTProperties.PASSED;
        if( parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startMachine: machineName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": machineName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            // only machine name
            String name = parameters[1];

            setMachines(); //Get newest machine information
            XTTProperties.printInfo( this.getClass().getName() + ": startMachine " + name );
            String machine[] = getMachine( name );
            if( machine == null )
            {
                XTTProperties.printFail( this.getClass().getName() + ": No such machine to start" );
                XTTProperties.setTestStatus( XTTProperties.FAILED );
                return XTTProperties.FAILED;
            }
            if( machine[Machine.STATUS].equals("Connected") )
            {
                XTTProperties.printInfo( this.getClass().getName() + ": Machine is already started" );
                return status;
            }
            String[] command1 = {"executeRemoteXMSCommand","start machine " + name };
            executeRemoteXMSCommand( command1 );

            setMachines(); //Get newest machine information
            machine = getMachine( name );
            while( ( machine != null )&&( machine.length == 2 )&& ( machine[Machine.STATUS].equals("Stopped") ) );
            {
                XTTProperties.printDebug("Machine "+name+" not started yet" );
                setMachines(); //Get newest machine information
                machine = getMachine( name );
            }
        }
        return status;
    }

    /**
     * Does an xms stop machine on the given machine.
     * <p>
     *
     * @param parameters - variable to store the product name to.
     * @return boolean
     */
    public int shutdownMachine( String[] parameters )
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": shutdownMachine: machineName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if ( parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0] +":"+MISSING_ARGUMENTS+": machineName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } else
        {
            // only machine name
            String name = parameters[1];

            setMachines(); //Get newest process information
            XTTProperties.printInfo(this.getClass().getName()+": shutdownMachine " + name );

            String machine[] = getMachine( name );
            if( machine == null )
            {
                XTTProperties.printFail(this.getClass().getName()+": No such machine to stutdown");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
            if( machine[Machine.STATUS].equals("Stopped") )
            {
                XTTProperties.printInfo( this.getClass().getName() + ": Machine is already stopped" );
                return status;
            }
            String[] command1 = {"executeRemoteXMSCommand","shutdown machine " + machine[Machine.NAME]};
            executeRemoteXMSCommand(command1);

            do
            {
               setMachines();
               machine = getMachine( name );
               if( machine == null )
               {
                   XTTProperties.printFail(this.getClass().getName()+": No such machine to stutdown");
                   XTTProperties.setTestStatus(XTTProperties.FAILED);
                   return XTTProperties.FAILED;
               }
            } while( machine[Machine.STATUS].equals("Connected") );
        }
        return status;
    }

    /**
     * Does an xms restart machine on the given machine.
     * <p>
     *
     * @param parameters - The machine to restart.
     * @return boolean
     */
    public int restartMachines( String[] parameters )
    {
    	int status = XTTProperties.PASSED;
        if( parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": restartMachines:"+NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        setMachines();
        for( int i = 0; i < machines.length; i++ )
        {
            if(shutdownMachine( new String[]{"shutdownMachine",machines[i][Machine.NAME]} )!= XTTProperties.PASSED ||
            startMachine( new String[]{"startMachine",machines[i][Machine.NAME]} )!= XTTProperties.PASSED)
            {
                return XTTProperties.FAILED;
            }
        }
        return status;
    }

    /**
     * Runs a regular expression on the data returned from the last remote request.
     * <p>
     *
     * @param parameters - 1) the regular expession 2) the variable to store the result to.
     * @return boolean
     */
    public int queryResponse(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryResponse: regularExpression variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": regularExpression variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[1]+"'");
            
            boolean bStatus = ConvertLib.queryString(parameters[0],getResponse(),parameters[1],parameters[2]);
			if (!bStatus) 
				status = XTTProperties.FAILED;
        }
        return status;
    }

    /**
     * Checks the return code of the last remote request.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if return code isn't the same as the given one.
     *
     * @param parameters - The expected return code.
     * @return boolean
     */
    public int checkReturnCode(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": checkReturnCode: returnCodeExpected");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": returnCodeExpected");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            try
            {
                int returnCodeToMatch = Integer.parseInt(parameters[1]);
                if (getReturnCode() == returnCodeToMatch)
                {
                    XTTProperties.printInfo(parameters[0] + ": return code matched: " + getReturnCode() + " == "+ parameters[1]);
                    return status;
                }
                else
                {
                    XTTProperties.printFail(parameters[0] + ": return code didn't matched: " + getReturnCode() + " != "+ parameters[1]);
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            }
            catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": " + parameters[1] + " isn't a valid number.");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Checks the regular expression doesn't match the data returned from the last remote request.
     * <p>
     * The test is set to {@link XTTProperties#FAILED FAILED} if the regular expression matches.
     *
     * @param parameters - 1) the regular expession
     * @return boolean
     */
    public int queryResponseNegative(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryResponseNegative: regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[1]+"'");
            
            boolean bStatus = ConvertLib.queryStringNegative(parameters[0],getResponse(),parameters[1]);
			if (!bStatus) 
				status = XTTProperties.FAILED;
        }
        return status;
    }

    /**
     * Runs an xms command on the remote machine.
     * <p>
     * The xms command, and location is specified via the configuration.
     *
     * @param parameters - 1) command to run without the xms prefix.
     * @return boolean
     */
    public int executeRemoteXMSCommand(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": executeRemoteXMSCommand: command");
            XTTProperties.printFail(this.getClass().getName()+": executeRemoteXMSCommand: command timeout");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if(parameters.length < 2|| parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": command");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": command timeout");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }

        if(xms == null)
        {
            XTTProperties.printFail("executeRemoteXMSCommand: No XMS path info was configured, please set the remote/path.");
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
            return status;
        }

        XTTProperties.printInfo(parameters[0] + ": Executing remote xms command: " + parameters[1]);

        String command = "";

        if((xmsShell != null)&&(!xmsShell.equals("null")))
        {
            command += xmsShell;
        }
        if((xmsPrefix != null)&&(!xmsShell.equals("null")))
        {
            command += xmsPrefix;
        }
        if((xmsPath != null)&&(!xmsPath.equals("null")))
        {
            command += xmsPath;
        }
        if((xms != null)&&(!xms.equals("null")))
        {
            command += xms;
        }

        command += parameters[1];

        if((xmsSuffix != null)&&(!xmsSuffix.equals("null")))
        {
            command += xmsSuffix;
        }

        if(parameters.length<3)
        {
            executeRemoteCommand(new String[]{"executeRemoteCommand",command});
        } else
        {
            executeRemoteCommand(new String[]{"executeRemoteCommand",command,parameters[2]});
        }
        return status;
    }

    public int executeRemoteCommand(String cmd,boolean stealtMode)
    {
    	int status = XTTProperties.PASSED;
        int timeout = -1;
        Document thisResponse = RemoteXTTClient.executeRemoteCommand(cmd,timeout,remoteAddress,stealtMode).getDocument();
        responseXML = thisResponse;
        return status;
    }
    /**
     * Runs an command on the remote machine.
     * <p>
     *
     * @param parameters - command to run.
     * @return boolean
     */
    public int executeRemoteCommand(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": executeRemoteCommand: command");
            XTTProperties.printFail(this.getClass().getName()+": executeRemoteCommand: command timeout");
            XTTProperties.printFail(this.getClass().getName()+": executeRemoteCommand: command timeout variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } 
        else if(parameters.length < 2|| parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": command");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": command timeout");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": command timeout variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }

        XTTProperties.printInfo(parameters[0] + ": Executing remote command: " + parameters[1]);

        String command = parameters[1];
        int timeout = -1;
        if(parameters.length > 2)
        {
            try
            {
                timeout=Integer.decode(parameters[2]);
            } catch (NumberFormatException ex)
            {
                XTTProperties.printFail(parameters[0]+":"+parameters[2]+" is not a number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            }
        }

        Document thisResponse = RemoteXTTClient.executeRemoteCommand(command,timeout,remoteAddress).getDocument();
        if(parameters.length > 3)
        {
            Element dataElement = XTTXML.getElement("response/data",thisResponse);
            String data = null;
            if(dataElement != null)
            {
                data = dataElement.getText();
            }
            XTTProperties.setVariable(parameters[3],data);
        }
        responseXML = thisResponse;
        return status;
    }
    /**
     * Gets the product info from the RemoteXTT configuration.
     * <p>
     *
     * @param parameters - no arguments required.
     * @return boolean
     */
    public int listRemoteProducts(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": listRemoteProducts:" +NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }

        XTTProperties.printInfo(parameters[0] + ": Grabbing Remote Product Info");

        responseXML = RemoteXTTClient.listRemoteProducts(remoteAddress).getDocument();

        return status;
    }

    /**
     * Tells RemoteXTT to write a file with the name, and content given.
     * <p>
     *
     * @param parameters - 1) [filepath]+filename 2) plain text file content
     */
    public int writeRemoteFile(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": writeRemoteFile: fileName fileData");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName fileData");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        String md5Hash = ConvertLib.getHexMD5Hash(parameters[2]);
        XTTProperties.printDebug(parameters[0] + ": File HASH: " +md5Hash);
        responseXML = RemoteXTTClient.writeRemoteFile(parameters[1],ConvertLib.base64Encode(parameters[2]),md5Hash,remoteAddress).getDocument();
        return status;
    }

    /**
     * Tells RemoteXTT to write a file with the name, and base64 encoded content given.
     * <p>
     * This should be used instead of writeRemoteFile to write binary files, use with the <base64file/> tag.
     *
     * @param parameters - 1) [filepath]+filename 2) plain text file content
     */
    public int writeRemoteBinaryFile(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": writeRemoteBinaryFile: fileName base64fileData");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName base64fileData");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        String md5Hash = ConvertLib.getHexMD5Hash(ConvertLib.base64Decode(parameters[2]));
        XTTProperties.printDebug(parameters[0] + ": File HASH: " +md5Hash);

        responseXML = RemoteXTTClient.writeRemoteFile(parameters[1],parameters[2],md5Hash,remoteAddress).getDocument();
        return status;
    }

    /**
     * Tells RemoteXTT to read a file and save the contents to a variable.
     * <p>
     *
     * @param parameters - 1) [filepath]+filename 2) variable to store plain text content to.
     */
    public int readRemoteFile(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": readRemoteFile: fileName variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        try
        {
            RemoteXTTPacket dataResponse = RemoteXTTClient.readRemoteFile(parameters[1],remoteAddress);
            responseXML = dataResponse.getDocument();

            String fileContents = XTTXML.getElement("response/filecontent",dataResponse.getDocument()).getText();

            String decodedData = ConvertLib.getStringFromOctetByteArray(ConvertLib.base64Decode(fileContents));
            XTTProperties.printInfo(parameters[0] + ": Saving "+ parameters[1] + " to " + parameters[2]);
            XTTProperties.setVariable(parameters[2], decodedData);
        }
        catch (NullPointerException npe)
        {
            //The error will already be printed.
        }
        catch (Exception e)
        {
            XTTProperties.printException(e);
            XTTProperties.printFail(parameters[0] + ": Counldn't write file");
        }
        return status;
    }

    /**
     *
     *
     */
    public int getRemoteSystemTime(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getRemoteSystemTime: variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
            
        } else if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        else
        {
            RemoteXTTPacket response = RemoteXTTClient.getRemoteSystemTime(remoteAddress);
            try
            {
                String systemtime = XTTXML.getElement("response/systemtime",response.getDocument()).getText();
                XTTProperties.setVariable(parameters[1],systemtime);
                XTTProperties.printInfo(parameters[0] + ": Remote system time '" + systemtime + "' saved to " + parameters[1]);
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+":Error getting remote system time");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }

        }
        return status;
    }

    /**
     * Tells RemoteXTT to read a file and save the base64 encoded contents to a variable.
     * <p>
     *
     * @param parameters - 1) [filepath]+filename 2) variable to store base64 encoded content to.
     */
    public int readRemoteBase64File(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": readRemoteBase64File: fileName variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        try
        {
            RemoteXTTPacket dataResponse = RemoteXTTClient.readRemoteFile(parameters[1],remoteAddress);
            responseXML = dataResponse.getDocument();

            String fileContents = XTTXML.getElement("response/filecontent",dataResponse.getDocument()).getText();

            XTTProperties.printInfo(parameters[0] + ": Saving "+ parameters[1] + " to " + parameters[2]);
            XTTProperties.setVariable(parameters[2], fileContents);
        }
        catch (Exception e)
        {
            XTTProperties.printException(e);
            XTTProperties.printFail(parameters[0] + ": Counldn't write file");
        }
        return status;
    }

    /**
     * @deprecated Use {@link #executeRemoteCommand executeRemoteCommand}, {@link #executeRemoteXMSCommand executeRemoteXMSCommand}, etc.
     * @param parameters - 1) command to run of the remote system.
     */
    public int sendRemoteCommand(String[] parameters)
    {
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sendRemoteCommand: commandToRun");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }

        XTTProperties.printFail(parameters[0] + " sendRemoteCommand has been removed\nTo fix your test you need to do the following:\nIf your function looks like:\n"
                                              + "<function name=\"sendRemoteCommand\" module=\"Remote\">\n"
	                                          +  "\t<parameter>writeFile bzTC10200-script.txt </parameter>\n"
		                                      +  "\t<file>bzTC10200-script.txt</file>\n"
                                              +  "</function>\n"
                                              +  "Change it to:\n"
                                              +  "<function name=\"writeRemoteFile\" module=\"Remote\">\n"
	                                          +  "\t<parameter>bzTC10200-script.txt</parameter>\n"
		                                      +  "\t<file>bzTC10200-script.txt</file>\n"
                                              +  "</function>\n\n"
                                              +  "If your function looks like:\n"
                                              +  "<function name=\"sendRemoteCommand\" module=\"Remote\">\n"
                                              +  "\t<configuration>XMG/PATH</configuration>\n"
                                              +  "\t<parameter>bin/xms extract data requestmodifications </parameter>\n"
                                              +  "\t<configuration>XMG/PATH</configuration>\n"
                                              +  "\t<parameter>cfg/bzTC11237.test.xml</parameter>\n"
                                              +  "</function>\n"
                                              +  "Change it to:\n"
                                              +  "<function name=\"executeRemoteXMSCommand\" module=\"Remote\">\n"
                                              +  "\t<parameter>\n"
                                              +  "\t\t<parameter>extract data requestmodifications </parameter>\n"
                                              +  "\t\t<configuration>XMG/PATH</configuration>\n"
                                              +  "\t\t<parameter>cfg/bzTC11237.test.xml</parameter>\n"
                                              +  "\t</parameter>\n"
                                              +  "</function>\n\n"
                                              + "For all other modifications you can use the executeRemoteCommand, and writeRemoteBinaryFile, readRemoteFile, and readRemoteBase64File."
                                              + "");
        XTTProperties.setTestStatus(XTTProperties.FAILED);
        return XTTProperties.FAILED;
    }

    /**
     * Returns the name of the newest filename at the given location on the remote machine.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable to store the filename to.
     *                     <br><code>parameters[2]</code> is the directory to look for the newest file in.
     *                     <br><code>parameters[3]</code> (optional) is the filter use when checking files.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int getNewestRemoteFileName(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getNewestRemoteFileName: variableName directoryName");
            XTTProperties.printFail(this.getClass().getName()+": getNewestRemoteFileName: variableName directoryName filter");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 3 && parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName directoryName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName directoryName filter");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            String directoryName = parameters[2];
            String filter = null;

            if(parameters.length>=4)
            {
                filter=parameters[3];
            }

            RemoteXTTPacket dataResponse = RemoteXTTClient.getNewestFile(directoryName,filter,remoteAddress);
            responseXML = dataResponse.getDocument();

            try
            {
                String result = XTTXML.getElement("response/newestfile",dataResponse.getDocument()).getText();

                XTTProperties.printInfo(parameters[0] + ": Newest file was '"+result+"'. Saving to " + parameters[1]);
                XTTProperties.setVariable(parameters[1], result);
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": Newest file not found");
                status =XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Returns the path of the location XTT is running on the Remote machine.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>No other arguments required.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int whereIsRemoteXTT(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": whereIsRemoteXTT: " +NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            RemoteXTTPacket dataResponse = RemoteXTTClient.whereAmI(remoteAddress);
            responseXML = dataResponse.getDocument();

            try
            {
                String result = XTTXML.getElement("response/whereiam",dataResponse.getDocument()).getText();

                XTTProperties.printInfo(parameters[0] + ": XTT is running at '"+result+"'.");
            }
            catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": XTT Running location not found.");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int getRemoteXTTStatus(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getRemoteXTTStatus: " +NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            RemoteXTTPacket dataResponse = RemoteXTTClient.getStatus(remoteAddress,false);
            responseXML = dataResponse.getDocument();
            try
            {
                String lasttestname = XTTXML.getElement("response/status/lasttest/name",dataResponse.getDocument()).getText();
                String lasttestnumber = XTTXML.getElement("response/status/lasttest/number",dataResponse.getDocument()).getText();
                String lasttesttotal = XTTXML.getElement("response/status/lasttest/total",dataResponse.getDocument()).getText();

                XTTProperties.printInfo(parameters[0] + ": Last Test: "+lasttestname+" "+lasttestnumber+"/"+lasttesttotal+"");
            } catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+": Error getting RemoteXTT status.");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    public int setRemoteXTTExpectedTests(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setRemoteXTTExpectedTests: numberOfTests");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": numberOfTests");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            try
            {
                RemoteXTTPacket dataResponse = RemoteXTTClient.setRemoteTotalTests(remoteAddress,Integer.decode(parameters[1]));
                responseXML = dataResponse.getDocument();

                String lasttestname = XTTXML.getElement("response/status/lasttest/name",dataResponse.getDocument()).getText();
                String lasttestnumber = XTTXML.getElement("response/status/lasttest/number",dataResponse.getDocument()).getText();
                String lasttesttotal = XTTXML.getElement("response/status/lasttest/total",dataResponse.getDocument()).getText();

                XTTProperties.printInfo(parameters[0] + ": Last Test: "+lasttestname+" "+lasttestnumber+"/"+lasttesttotal+"");
            } catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": " + parameters[1] + " isn't a valid number.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            } catch(NullPointerException npe)
            {
                XTTProperties.printFail(parameters[0]+":Error setting remote xtt expected tests");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return XTTProperties.FAILED;
            }
        }
        return status;
    }
    

    /**
     * Stops the Remote XTT.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br>No other arguments required.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int stopRemoteXTT(String[] parameters)
    {
    	int status = XTTProperties.PASSED;
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopRemoteXTT: " +NO_ARGUMENTS);
            return XTTProperties.FAILED_NO_ARGUMENTS;
        } else if(parameters.length != 1)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": Stopping Remote XTT");
        }
        return status;
    }

    public void initialize()
    {
        String path = null;
        String command = null;
        
        String remoteIP = XTTProperties.getProperty("system/remoteip");
        int remotePort = XTTProperties.getIntProperty("system/remoteport");
        if(remotePort <= 0)remotePort = RemoteXTT.DEFAULTPORT;

        if((!remoteIP.equals("null"))&&(remotePort>0))
        {
            remoteAddress = new java.net.InetSocketAddress(remoteIP,remotePort);
        }
        else
        {
            XTTProperties.printFail(this.getClass().getName()+".initialize(): No address for RemoteXTT configured.");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_CONFIG_VALUE);
        }

        if(XTTProperties.getQuietProperty("remote/disablenoftcheck").equals("") || ConvertLib.textToBoolean(XTTProperties.getQuietProperty("remote/disablenoftcheck")))
        {
            stopProcessCommand = "stop process ";
        }
        else
        {
            stopProcessCommand = "stop process -noftcheck ";
        }

        if((XTTProperties.getProperty("remote/path","xma/path","xmg/path")!=null)&&(!XTTProperties.getProperty("remote/path","xma/path","xmg/path").equals("null")))
        {
            String suffix = null;
            String prefix = null;
            String shell = null;

            XTTProperties.printDebug(this.getClass().getName()+".initialize(): Using generic configuration");

            path = XTTProperties.getProperty("remote/path","xma/path","xmg/path");
            command = XTTProperties.getProperty("remote/xms","xma/xms","xmg/xms");

            //This is a special case for if you want to run xms commands on windows, and need the MKS shell.
            //Therefore you need to wrap the command in quotes, so you set the path+xms to MKS and begin the quotes,
            //Then you set the xmsPrefix to quotes. There's no other use case for this at the moment.
            suffix = XTTProperties.getProperty("remote/xmsSuffix");
            if((suffix != null) && (!suffix.equals("null")))
            {
                xmsSuffix = suffix;
            }
            else
            {
                xmsSuffix = null;
            }
            prefix = XTTProperties.getProperty("remote/xmsPrefix");
            if((prefix != null) && (!prefix.equals("null")))
            {
                xmsPrefix = prefix;
            }
            else
            {
                xmsPrefix = null;
            }

            shell = XTTProperties.getProperty("remote/shell");
            if((shell != null) && (!shell.equals("null")))
            {
                xmsShell = shell;
            }
            else
            {
                xmsShell = null;
            }
            xmsPath=path;

            if((command != null) && (!command.equals("null")))
            {
                xms = command;
            }
            else
            {
                xms = "bin/xms ";
            }
        } else
        {
            XTTProperties.printWarn(this.getClass().getName()+".initialize(): Couldn't find any configuration for RemoteXTT.");
        }
    }

    public String toString()
    {
        return this.getClass().getName();
    }

    private class AssassinThread extends Thread
    {
        private int amount = 0;
        private String variable = "";
        private String process = "";
        private int action = -1;

        public AssassinThread(String process, String variable, String amount, int action)
        {
            try
            {
                this.process = process;
                this.variable = variable;
                this.amount = Integer.parseInt(amount);
                this.action = action;
                XTTProperties.setVariable(variable,""+(0));
            }
            catch(Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+ ": error setting kill values");
                try
                {
                    join();
                }
                catch(Exception f)
                {
                    //
                }
            }
        }

        public void run()
        {
            try
            {
                int value = amount - 1;
                while(value < amount)
                {
                    yield();
                    try
                    {
                        value = Integer.parseInt(XTTProperties.getProperty(variable));
                    }
                    catch (NumberFormatException f)
                    {
                        // Just try it again (this was to get around a bug where XTT was returning an empty string
                    }
                }
                if(action == 0)
                {
                    String[] arguments = {"killProcess",process,"true"};
                    killProcess(arguments);
                }
                else if(action == 1)
                {
                    String[] arguments = {"executeRemoteCommand",process};
                    executeRemoteCommand(arguments);
                }

            }
            catch(Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+ ": error running process assassin thread");
                try
                {
                    join();
                }
                catch(Exception f)
                {
                    //
                }
            }
        }
    }
	/**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <remote>"
        +"\n        <!-- path to the product for executing remote comands with FunctionModule_Remote -->"
        +"\n        <path>/opt/xmg/</path>"
        +"\n        <!-- command to execute tools for xms -->"
        +"\n        <command>run command </command>"
        +"\n        <!-- path to the log files -->"
        +"\n        <logpath>/opt/xmg/logs</logpath>"
        +"\n        <!-- path the process logs  -->"
        +"\n        <procpath>/opt/proc</procpath>"
        +"\n        <!-- xms location, relative to 'path', must end in a space -->"
        +"\n        <xms>bin/xms </xms>"
        +"\n        <!-- If you need to run XMS command on a different shell that the default, e.g. Windows (using KSH) set the following -->"
        +"\n        <!-- path to the shell to execute with -->"
        +"\n        <!--<shell>\"c:\\msk\\blahblah\\sh.exe\" -C</shell>-->"
        +"\n        <!-- a prefix to the xms command -->"
        +"\n        <!--<xmsPrefix>\"</xmsPrefix>-->"
        +"\n        <!-- a suffix to the xms comand -->"
        +"\n        <!--<xmsSuffix>\"</xmsSuffix>-->"
        +"\n    </remote>";
    }


    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Remote.java,v 1.59 2010/05/05 08:12:37 rajesh Exp $";
}