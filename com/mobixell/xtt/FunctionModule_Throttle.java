package com.mobixell.xtt;

import java.util.HashMap;

/**
 * FunctionModule_Throttle.
 * <p>
 * Functions for throttling.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.3 $
 */
public class FunctionModule_Throttle extends FunctionModule
{

    private static HashMap<String,Throttle> throttles = new HashMap<String,Throttle>();

    public FunctionModule_Throttle()
    {

    }

    public void initializeThrottle(String parameters[])
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi");
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi name");
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi name IntervalMilliSeconds");
            return;
        }
        else if(parameters.length<2||parameters.length>4)
        {
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi");
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi name");
            XTTProperties.printFail(this.getClass().getName()+": initializeThrottle: TPi name IntervalMilliSeconds");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name="";
            int TPi=0;
            int interval=1000;
            if(parameters.length>2)
            {
                name=parameters[2];
            }
            try
            {
                TPi = Integer.parseInt(parameters[1]);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                return;
            }
            if(parameters.length>3)
            {
                try
                {
                    interval = Integer.parseInt(parameters[3]);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+": '"+parameters[3]+"' is NOT a correct number");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
                    return;
                } 
            }
           
            if(name.equals(""))
            {
                throttles.put(name,new Throttle("global",TPi));

            } else
            {
                throttles.put(name,new Throttle(name,TPi,interval));
            }
        }
    }

    public void throttle(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": throttle:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": throttle: name");
            return;
        }
        if(parameters.length>2)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            String name = "";
            if(parameters.length>1)
            {
                name = parameters[1];
            }
            Throttle currentThrottle = (Throttle)throttles.get(name);
            if(currentThrottle == null)
            {
                XTTProperties.printFail(parameters[0] +": throttle: No such throttle found, please initialize it first.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            else
            {
                XTTProperties.printVerbose(parameters[0] +": throttle: Checking TPS.");
                try
                {
                    currentThrottle.pause();
                }
                catch(Exception e)
                {
                    XTTProperties.printException(e);
                }
            }
        }
    }

    public void initialize()
    {
        throttles = new HashMap<String,Throttle>();
        throttles.put("",new Throttle("global",100));
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Throttle.java,v 1.3 2006/10/12 07:13:11 rsoder Exp $";
}