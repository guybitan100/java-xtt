package com.mobixell.xtt;

import java.lang.reflect.Method;


/**
 * FunctionModule base class for Function Modules.
 *
 * @author      Roger Soder
 * @version     $Id: FunctionModule.java,v 1.15 2008/10/03 08:05:34 rsoder Exp $
 */
public abstract class FunctionModule implements Comparable<Object>
{
    /**
     * The string is part of the "missing arguments" error message when a function is called with wrong length of the parameters[] array.}
     */
    public static final String MISSING_ARGUMENTS = " invalid number of arguments";
    public static final String NO_ARGUMENTS      = " no arguments required";
    public static final String RESOURCE_OK       = " OK";
    public static final String RESOURCE_PORT     = " Unavailable port";
    public static final int    SHOWLENGTH        = 60;

    public FunctionModule()
    {
        printVersion();
    }

    /**
     * Finds the CVS version from the module loaded and print with XTTProperties.printDebug.
     *
     * @see XTTProperties#printDebug
     */
    protected void printVersion()
    {
        XTTProperties.printDebug(this.getClass().getName()+": "+getVersion());
    }

    /**
     * Run the Function of a FunctionModule
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @return      boolean containing true when the method was executed and false if not.
     */
    public int runFunction(String parameters[])
    {
        // No parameters? Should be at least length=1 for the function name
        if(parameters==null||parameters.length==0)
        	return XTTProperties.FAILED_NO_ARGUMENTS;
        try
        {
            // Get the methods of this Instance of a FunctionModule
            Method methods[]=this.getClass().getMethods();
            // Store our parameters String array in an object array
            Object reflectParam[]={parameters};
            // for all the methods
            for(int i=0;i<methods.length;i++)
            {
                /*/ Debug Code, comment out when not needed
                System.out.println(methods[i].getName().toLowerCase()+" ?= "+parameters[0].toLowerCase());
                Class[] classes=methods[i].getParameterTypes();
                for(int j=0;j<classes.length;j++)
                    System.out.println("  "+classes[j].getName());
                //*/
                // get the Name of the method and compare with the parameters[0] holding the function name
				if (methods[i].getName().toLowerCase().equals(parameters[0].toLowerCase()))
				{
					// If found call the function and return true
					Object o = methods[i].invoke(this, reflectParam);
					if (o != null)
					{
						if (o.getClass().equals(Integer.class)) 
							return Integer.parseInt(o.toString());
						if (o.getClass().equals(Boolean.class))
						{
							if (o.toString().equals("true")) return XTTProperties.PASSED;
							else return XTTProperties.FAILED_UNKNOWN;
						}
					}
					else return XTTProperties.PASSED;
				}
            }
        // Something went wrong, perhaps someone trying to call an Object method?
        } catch (Exception e)
        {
            String parameterLine="";
            for(int i=0;i<parameters.length;i++)
            {
                parameterLine=parameterLine+"'"+parameters[i]+"' ";
            }

            XTTProperties.printFail("FunctionModule: "+parameterLine+ ": " + e.getMessage());
            XTTProperties.setTestStatus(XTTProperties.FAILED);
            if(XTTProperties.printDebug(null))
                XTTProperties.printException(e);
            return XTTProperties.FAILED_UNKNOWN;
        }
        // the function name was none of our methods
        return XTTProperties.FAILED_FUNCTION_NOT_EXIST;
    }

    /**
     * Dump the Functions of a FunctionModule. Iterates over all Functions of the FunctionModule and calls it with a null parameter.
     *
     */
    public void dumpFunctions()
    {
        String parameters[]=null;
        try
        {
            // Get the methods of this Instance of a FunctionModule
            Method methods[]=this.getClass().getMethods();
            // Store our parameters String array in an object array
            Object reflectParam[]={parameters};
            // for all the methods
            for(int i=0;i<methods.length;i++)
            {
                try
                {
                    // Call the function
                    methods[i].invoke(this,reflectParam);
                } catch (Exception ex)
                {
                    //ex.printStackTrace();
                }
            }
        // Something went wrong, perhaps someone trying to call a Object method?
        } catch (Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "";
    }

    /**
     * Finds the CVS version from the module loaded and print to System.out.
     *
     */
    public void showVersions()
    {
        
        System.out.println(ConvertLib.createString(this.getClass().getName()+": ",SHOWLENGTH) + getVersion());
    }
    /**
     * Returns the CVS version from the module loaded.
     *
     * @return      String containing the CVS version String in format 1.2.1.3
     */
    public String getVersion()
    {
        String version = "";
        String sccsid = "";

        try
        {
            FunctionModule test = this;

            sccsid = (String)this.getClass().getField("tantau_sccsid").get(test);

            version = parseVersion(sccsid);

            return version;

            //Object testObj = this.getClass().getField("tantau_sccsid").get(test);

        } catch (Exception e)
        {
            XTTProperties.printException(e);
        }
        return "";
    }


    /**
     * Returns the version from a $Id: FunctionModule.java,v 1.15 2008/10/03 08:05:34 rsoder Exp $ CVS String.
     *
     * @return      String containing the CVS version String in format 1.2.1.3
     */
    public static String parseVersion(String sccsid)
    {
        int start = sccsid.indexOf(",v ") + 3;
        int end   = sccsid.indexOf(" ",start+1);
        return      sccsid.substring(start,end);
    }

    /**
     * Return the name of the Module.
     *
     */
    public String getModuleName()
    {
        return this.getClass().getName();
    }

    /**
     * Check the resources needed by the FunctionModule.
     * A null return means ok.
     */
    public String checkResources()
    {
        return "" + this.getClass().getName() + ":"+RESOURCE_OK;
    }
    
    public int compareTo(Object o)
    {
        return this.getClass().getName().compareTo(o.getClass().getName());
    }

    /**
     * Re-Initialize the Module
     *
     */
    protected abstract void initialize();
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule.java,v 1.15 2008/10/03 08:05:34 rsoder Exp $";

}