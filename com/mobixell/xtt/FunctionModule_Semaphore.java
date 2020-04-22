package com.mobixell.xtt;

import java.util.HashMap;
import java.util.Stack;

/**
 * FunctionModule_Semaphore.
 * <p>
 * Functions for semaphoring.
 *
 * @author      Gavin Cattell
 * @version     $Revision: 1.3 $
 */
public class FunctionModule_Semaphore extends FunctionModule
{
    private static HashMap<String,Stack<Object>> semaphores = new HashMap<String,Stack<Object>>();
    //private static final Object semaphorekey = new Object();
    
    /**
     * Initialize a semaphore.
     * Sets up the number of semaphores available for the given stack, or the default one if not specified.
     * 
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> is the number of semaphores to allow to be taken.
     *                      <br><code>parameters[1]</code> [optional] is the name of the stack, if not present the default stack is used.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list     
     */
    public void createSemaphore(String[] parameters)
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": createSemaphore: concurrency");
            XTTProperties.printFail(this.getClass().getName()+": createSemaphore: concurrency name");
            return;
        }
        else if(parameters.length<2||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0] + ": concurrency");
            XTTProperties.printFail(parameters[0] + ": concurrency name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name = null;
            int concurrency = 0;

            if(parameters.length > 2)
            {
                name = parameters[2];
            }

            try
            {
                concurrency = Integer.decode(parameters[1]);
            }
            catch(NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0] + ": " + parameters[1] + " isn't a number");
                return;
            }

            Stack<Object> stack = new Stack<Object>();
            for(int i=0;i<concurrency;i++)
            {
                stack.push(new Object());
            }

            semaphores.put(name,stack);
        }
    }

    /**
     * Try to obtain a semaphore.
     * Tries to take a semaphore from the specified stack, or the default one if not supplied.
     * 
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> [optional] is the name of the stack to get the semaphore from.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list     
     */
    public void takeSemaphore(String[] parameters)
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": takeSemaphore: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": takeSemaphore: name");
            return;
        }
        else if(parameters.length<1||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name=null;
            if(parameters.length > 1)
                name = parameters[1];

            Stack<Object> stack = semaphores.get(name);
            if(stack == null)
            {
                XTTProperties.printFail(parameters[0] + ": Unknown semaphore '" + name + "'.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            
            int timeout = XTTProperties.getIntProperty("SEMAPHORE/TIMEOUT");
            if(timeout < 0) timeout = 30000;
            long starttime = System.currentTimeMillis();
            synchronized(stack)
            {
                XTTProperties.printVerbose(parameters[0] + ": Attempting to obtain a semaphore from '" + name + "'.");
                try
                {
                    while(stack.empty() && (System.currentTimeMillis()-starttime)<timeout)
                    {
                        stack.wait(timeout);
                    }
                }
                catch(Exception e){}

                Object semaphore = null;
                try
                {
                    semaphore = stack.pop();
                }
                catch(java.util.EmptyStackException ese){}
                
                if(semaphore == null)
                {
                    XTTProperties.printFail(parameters[0] + ": No semaphores available from '" + name + "'.");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                }
                else
                {
                    XTTProperties.printInfo(parameters[0] + ": Got a semaphore from '" + name + "', continuing.");
                }
            }
        }
    }

    /**
     * Return a semaphore.
     * Returns a semaphore to the specified stack, or the default one if not supplied.
     * 
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> [optional] is the name of the stack to return the semaphore to.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list     
     */
    public void returnSemaphore(String[] parameters)
    {
        if((parameters==null))
        {
            XTTProperties.printFail(this.getClass().getName()+": returnSemaphore: "+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": returnSemaphore: name");
            return;
        }
        else if(parameters.length<1||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0] + ": name");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            String name=null;
            if(parameters.length > 1)
                name = parameters[1];

            Stack<Object> stack = semaphores.get(name);
            if(stack == null)
            {
                XTTProperties.printFail(parameters[0] + ": Unknown semaphore '" + name + "'.");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                return;
            }
            
            synchronized(stack)
            {
                XTTProperties.printInfo(parameters[0] + ": Returning a semaphore to '" + name + "'.");

                Object semaphore = new Object();
                stack.push(semaphore);
                stack.notify();
            }                       
        }
    }
    
    public String getConfigurationOptions()
    {
        return "    <!-- function module Semaphore -->"
        +"\n    <Semaphore>"
        +"\n        <Timeout>30000</Timeout>"
        +"\n    </Semaphore>";
    }    

    public void initialize()
    {
        semaphores = new HashMap<String,Stack<Object>>();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): initialized.");
    }

    public String toString()
    {
        return this.getClass().getName();
    }
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Semaphore.java,v 1.3 2010/03/18 12:08:34 rajesh Exp $";
}