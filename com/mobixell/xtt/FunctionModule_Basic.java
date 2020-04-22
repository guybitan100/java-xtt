package com.mobixell.xtt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.HashSet;
import java.util.HashMap;

/**
 * FunctionModule_Basic.
 *
 * @version     $Id: FunctionModule_Basic.java,v 1.52 2010/07/30 11:36:59 rajesh Exp $
 */
public class FunctionModule_Basic extends FunctionModule
{
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_Basic.java,v 1.52 2010/07/30 11:36:59 rajesh Exp $";

    private HashMap<String,HashSet<String>> uniqueCheck = null;
    
    private static final int SHOWLENGTH=64;
    
    /**
     * Add the integer values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> and following are the integer values to add up
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int addVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addVariable: variableName valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus="";
                long val=0;
                for(i=2;i<parameters.length;i++)
                {
                    val=val+(Long.decode(parameters[i])).longValue();
                    add.append(plus+"'"+parameters[i]+"'");
                    plus=" + ";
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Inserts filler characters at the front of the string till the length is at least the minimum length.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> the data to add the prefix to
     *                      <br><code>parameters[2]</code> the minimum length of the string (the length can go over this if the filler is larger than one character).     
     *                      <br><code>parameters[3]</code> the string to fill with   
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int addPrefix(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addPrefix: variableName data minlength filler");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName data minlength filler");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String data = parameters[2];
            int minlength = 0;
            String filler = parameters[4];
            
            try
            {
                minlength = Integer.decode(parameters[3]);    
            }
            catch(NumberFormatException nfe)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                XTTProperties.printFail(parameters[0] +": " + parameters[3] + " isn't a number.");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(nfe);
                }
                return status;
            }
            
            String newdata = ConvertLib.addPrefixToString(data,minlength,filler);
                        
            XTTProperties.printInfo(parameters[0] + ": " + newdata + " saved to " + parameters[1]);
            XTTProperties.setVariable(parameters[1],newdata);
        }
        return status;
    }

    /**
     * Inserts filler characters at the back of the string till the length is at least the minimum length.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> the data to add the suffix to
     *                      <br><code>parameters[2]</code> the minimum length of the string (the length can go over this if the filler is larger than one character).     
     *                      <br><code>parameters[3]</code> the string to fill with   
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */    
    public int addSuffix(String parameters[])
    {
    	 int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": addSuffix: variableName data minlength filler");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName data minlength filler");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String data = parameters[2];
            int minlength = 0;
            String filler = parameters[4];
            
            try
            {
                minlength = Integer.decode(parameters[3]);    
            }
            catch(NumberFormatException nfe)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                XTTProperties.printFail(parameters[0] +": " + parameters[3] + " isn't a number.");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(nfe);
                }
                return status;
            }
            
            String newdata = ConvertLib.addSuffixToString(data,minlength,filler);
                        
            XTTProperties.printInfo(parameters[0] + ": " + newdata + " saved to " + parameters[1]);
            XTTProperties.setVariable(parameters[1],newdata);
        }
        return status;
    }    

    /**
     * Base64 encode a value
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> is the value to base64 encode
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int base64Encode(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": base64encode: variableName value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName value");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String encoding=ConvertLib.base64Encode(parameters[2]);
            XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"='"+encoding+"'");
            XTTProperties.setVariable(parameters[1],encoding);
        }
        return status;
    }

    /**
     * Calculates a mathmatical expression and stores the result to a variable.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to.
     *                      <br><code>parameters[2]</code> argument is the expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int calculate(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": calculate: variableName expression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName expression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        }
        else
        {
            try
            {
                Double result = MathsLib.calc(parameters[2]);
                XTTProperties.printInfo(parameters[0] +": " + parameters[2] + "=" + result.longValue());
                XTTProperties.setVariable(parameters[1],""+result.longValue());
            }
            catch(NumberFormatException nfe)
            {
            	status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                XTTProperties.printFail(parameters[0] +": " + parameters[2] + " isn't a proper equation");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(nfe);
                }
            }
        }
        return status;
    }

    /**
     * compares two strings if they are equal, ignoring case.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> string A,
     *                     <br><code>parameters[2]</code> string B
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int compareString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": compareString: stringA stringB");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": stringA stringB");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String show1=parameters[1];
            if(show1.length()>SHOWLENGTH)
            {
                int leng=show1.length();
                show1=show1.substring(0,SHOWLENGTH)+"' plus "+(leng-SHOWLENGTH)+" chars";
            } else
            {
                show1=show1+"'";
            }
            String show2=parameters[2];
            if(show2.length()>64)
            {
                int leng=show2.length();
                show2=show2.substring(0,SHOWLENGTH)+"' plus "+(leng-SHOWLENGTH)+" chars";
            } else
            {
                show2=show2+"'";
            }
            if(parameters[1].toLowerCase().replaceAll("\\n|\\r|\\n\\r", "").equals(parameters[2].toLowerCase().replaceAll("\\n|\\r|\\n\\r", "")))
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printInfo(parameters[0]+": '"+parameters[1]+"' = '"+parameters[2]+"'");
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": '"+show1+" = '"+show2);
                }
            } else
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' IS NOT '"+parameters[2]+"'");
                } else
                {
                    XTTProperties.printFail(parameters[0]+": '"+show2+" IS NOT '"+show2);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Set the value of a variable with an ASCII text representation of the BYTES which a String is made of.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable name
     *                     <br><code>parameters[2]</code> is the variable value
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ConvertLib#outputBytes(byte[], int, int)
     */
    public int createASCIIString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createASCIIString: variableName value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName value");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"="+ConvertLib.outputBytes(ConvertLib.createBytes(parameters[2]),0,parameters[2].length()));
            XTTProperties.setVariable(parameters[1],ConvertLib.outputBytes(ConvertLib.createBytes(parameters[2]),0,parameters[2].length()));
        }
        return status;
    }
    
    private int createDate(String funcName,String dateFormat, String varName, String secondsToAdd, String timeZone, long epochDate)
    {
    	int status = XTTProperties.PASSED;
        try
        {
            String datestring="";
            SimpleDateFormat format=new SimpleDateFormat(dateFormat,Locale.US);
            GregorianCalendar calendar=new java.util.GregorianCalendar(Locale.US);
            if(epochDate>0)
            {
                calendar.setTime(new java.util.Date(epochDate));
            }
            if(timeZone!=null)
            {
                format.setTimeZone(TimeZone.getTimeZone(timeZone));
            }
            if(secondsToAdd!=null)
            {
                int time = Integer.parseInt(secondsToAdd.replaceAll("[^\\-0-9]",""));
                if (secondsToAdd.endsWith("s"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' seconds to the date");
                    calendar.add(GregorianCalendar.SECOND,time);
                }
                else if (secondsToAdd.endsWith("h"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' hours to the date");
                    calendar.add(GregorianCalendar.HOUR_OF_DAY,time);
                }
                else if (secondsToAdd.endsWith("d"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' days to the date");
                    calendar.add(GregorianCalendar.DAY_OF_YEAR,time);
                }
                else if (secondsToAdd.endsWith("w"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' weeks to the date");
                    calendar.add(GregorianCalendar.WEEK_OF_YEAR,time);
                }
                else if (secondsToAdd.endsWith("m"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' months to the date");
                    calendar.add(GregorianCalendar.MONTH,time);
                }
                else if (secondsToAdd.endsWith("y"))
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' years to the date");
                    calendar.add(GregorianCalendar.YEAR,time);
                }
                else
                {
                    XTTProperties.printDebug(funcName+": Adding '" + time + "' seconds to the date");
                    calendar.add(GregorianCalendar.SECOND,time);
                }
            }
            datestring=format.format(calendar.getTime());
            XTTProperties.printInfo(funcName+": date '"+datestring+"' stored in variable "+varName);
            XTTProperties.setVariable(varName,datestring);

        } catch (java.lang.NullPointerException npx)
        {
            XTTProperties.printFail(funcName+": '"+dateFormat+"' IS NOT a correct date format");
            status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } catch (java.lang.NumberFormatException nfe)
        {
            XTTProperties.printFail(funcName+": '"+secondsToAdd+"' IS NOT a correct number");
            status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } catch (java.lang.IllegalArgumentException iax)
        {
            XTTProperties.printFail(funcName+": '"+dateFormat+"' IS NOT a correct date format");
            status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } catch (Exception ex)
        {
            XTTProperties.printFail(funcName+": "+ex.getClass().getName()+" "+ex.getMessage());
            status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }

    /**
     * Create a datestring with the current time and specified formating. The used Locale will be US.
     * Default timezone is system timezone. Default no seconds will be added to the time value.
     * <br/><br/>
     * You can add or subtract different types of units to the time by appending a symbol to <code>parameter[3]</code>
     * Supported values are 's' for seconds (default), 'h' for hours, 'd' for days, 'w' for weeks, 'm' for months, and 'y' for years.
     * For example 5d will add 5 days to the current time, -5d will subract 5 days, etc.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is a time format allowed by {@link java.text.SimpleDateFormat java.text.SimpleDateFormat}.
     *                     <br><code>parameters[2]</code> is the variable name to store the created datestring in.
     *                     <br><code>parameters[3]</code> (optional) time to add.
     *                     <br><code>parameters[4]</code> (optional, requeires parameter[3]) Timezone to use in java.util.TimeZone.getTimeZone(parameter[4]) format.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int createDateString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createDateString: dateFormat variableName");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: dateFormat variableName secondsToAdd");
            XTTProperties.printFail(this.getClass().getName()+": createDateString: dateFormat variableName secondsToAdd timezone");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3||parameters.length>5)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": dateFormat variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": dateFormat variableName secondsToAdd");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": dateFormat variableName secondsToAdd timezone");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String funcName     =parameters[0];
            String dateFormat   =parameters[1];
            String varName      =parameters[2];
            String secondsToAdd = null;
            String timeZone     = null;
            long epochDate=-1;
                if(parameters.length==5)
                {
                    timeZone=parameters[4];
                }
                if(parameters.length>=4)
                {
                    secondsToAdd=parameters[3];
                }
            createDate(funcName,dateFormat,varName,secondsToAdd,timeZone,epochDate);
        }
        return status;
    }

    /**
     * Create a datestring with the time specified in epoch (seconds (if<9999999999) or miliseconds since 1.1.1970) and specified formating. The used Locale will be US.
     * Default timezone is system timezone. Default no seconds will be added to the time value.
     * <br/><br/>
     * The units of time to add or subtract can be modified, see {@link FunctionModule_Basic#createDateString(java.lang.String[]) createDateString}
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the time in seconds since January 1, 1970, 00:00:00 GMT.
     *                     <br><code>parameters[2]</code> is a time format allowed by {@link java.text.SimpleDateFormat java.text.SimpleDateFormat}.
     *                     <br><code>parameters[3]</code> is the variable name to store the created datestring in.
     *                     <br><code>parameters[4]</code> (optional) time to add to the time value.
     *                     <br><code>parameters[5]</code> (optional, requeires parameter[3]) Timezone to use in java.util.TimeZone.getTimeZone(parameter[4]) format.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int createEpochDateString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createEpochDateString: epochDate dateFormat variableName");
            XTTProperties.printFail(this.getClass().getName()+": createEpochDateString: epochDate dateFormat variableName secondsToAdd");
            XTTProperties.printFail(this.getClass().getName()+": createEpochDateString: epochDate dateFormat variableName secondsToAdd timezone");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4||parameters.length>6)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": epochDate dateFormat variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": epochDate dateFormat variableName secondsToAdd");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": epochDate dateFormat variableName secondsToAdd timezone");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String funcName         =parameters[0];
            try
            {
                long epochDate=Long.parseLong(parameters[1]);
                long maxSec=9999999999l;
                if(epochDate<maxSec)epochDate=epochDate*1000;
                String dateFormat   =parameters[2];
                String varName      =parameters[3];
                String secondsToAdd = null;
                String timeZone     = null;
                if(parameters.length==6)
                {
                    timeZone=parameters[5];
                }
                if(parameters.length>=5)
                {
                    secondsToAdd=parameters[4];
                }
                createDate(funcName,dateFormat,varName,secondsToAdd,timeZone,epochDate);
            } catch (NumberFormatException e)
            {
                XTTProperties.printFail(funcName+": '"+parameters[1]+"' IS NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Create a datestring with the current time and specified formating. The used Locale will be US.
     * Default timezone is system timezone. Default no seconds will be added to the time value.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable name to store the created datestring in.
     *                     <br><code>parameters[2]</code> is a time format allowed by {@link java.text.SimpleDateFormat java.text.SimpleDateFormat}.
     *                     <br><code>parameters[3]</code> is the time to parse.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int createEpochFromDateString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": createEpochFromDateString:  variableName dateFormat dateString");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName dateFormat dateString");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String varName      =parameters[1];
            String dateFormat   =parameters[2];
            String dateString   =parameters[3];
            long epoch = 0;
            try
            {
                SimpleDateFormat format = new SimpleDateFormat(dateFormat);
                java.util.Date date = format.parse(dateString);
                epoch = date.getTime();
                XTTProperties.setVariable(varName,""+epoch);
                XTTProperties.printInfo(parameters[0] + ": " + epoch + " stored to " + varName);
            }
            catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": Error parsing date");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
        return status;
    }

    /**
     * Converts a string of hex characters representing a byte array into ASCII characters.
     * i.e. 74616E7461753330303830 into tantau30080
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no more arguments required.
     *                     <br><code>parameters[1]</code> is the variable name to store the result to
     *                     <br><code>parameters[2]</code> is the value to decode
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ConvertLib#getStringFromByteString(String)
    */
    public int decodeByteString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": decodeByteString: variableName ByteString");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName ByteString");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            String decodedString = ConvertLib.getStringFromByteString(parameters[2]);
            XTTProperties.setVariable(parameters[1],decodedString);
        }
        return status;
    }
    
    /**
     * Converts a string of ASCII characters into hex characters representing a byte array.
     * i.e. tantau30080 into 74616E7461753330303830 
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no more arguments required.
     *                     <br><code>parameters[1]</code> is the variable name to store the result to
     *                     <br><code>parameters[2]</code> is the value to encode
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ConvertLib#getStringFromByteString(String)
    */
    public int encodeByteString(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": encodeByteString: variableName ByteString");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName ByteString");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        } else
        {
            String encodedString = ConvertLib.getHexStringFromByteArray(ConvertLib.createBytes(parameters[2]));
            XTTProperties.setVariable(parameters[1],encodedString);
        	
        }
        return status;
    }


    /**
     * Divide the integer values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> and following are the integer values to divide
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int divideVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": divideVariable: variableName valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus="";
                long val=(Long.decode(parameters[i])).longValue();
                add.append(plus+"'"+parameters[i]+"'");
                plus=" / ";
                for(i=3;i<parameters.length;i++)
                {
                    val=val / (Long.decode(parameters[i])).longValue();
                    add.append(plus+"'"+parameters[i]+"'");
                    plus=" / ";
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    
    /**
     * Executes a command on the local shell.
     * Using this in tests may result in inconsistencies when run on different machines due to environment differences.
     */
    public int executeCommand(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        boolean ended = false;
        String command = "";
        String line = "";
        String variableName = "";
        StringBuffer response = new StringBuffer();

        if((parameters==null)||(parameters.length<3))
        {
            XTTProperties.printFail(this.getClass().getName()+": executeCommand: responseVariable command");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        else
        {
            variableName = parameters[1];
            for (int i=2;i<parameters.length;i++)
            {
                command += parameters[i];
            }
            XTTProperties.printInfo(parameters[0]+": executeCommand: Run command " + command);
            try
            {
                Process process =  Runtime.getRuntime().exec(command);
                BufferedReader processInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
                while((!ended)||(processInput.ready()))
                {
                    try
                    {
                        if((!ended) && (process.exitValue() == 0))
                        {
                            System.out.println("Program ended");
                            ended = true;
                        }
                        else if(!ended)
                        {
                            System.out.println("Program ended");
                            ended = true;
                        }
                    }
                    catch(Exception e)
                    {
                        //The program should still be running if this happens so lets read
                    }
                    line = processInput.readLine();
                    if ((line!=null) && (line.length() != 0))
                    {
                        response.append(line + "\r\n");
                    }
                }

                XTTProperties.setVariable(variableName,response.toString());
                XTTProperties.printDebug(parameters[0]+": executeCommand: Result:\n" + response.toString());
            }
            catch (Exception e)
            {
                XTTProperties.printFail(this.getClass().getName()+": Error running command");
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Store the difference, measured in seconds, between the current time and midnight, January 1, 1970 UTC. Added as local version of getRemoteSystemTime in FunctionModule Remote
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable to store the value in.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int getLocalSystemTime(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getLocalSystemTime: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            long time=System.currentTimeMillis()/1000;
            XTTProperties.printInfo(parameters[0]+": stored '"+time+"' to "+parameters[1]);
            XTTProperties.setVariable(parameters[1],""+time);
        }
        return status;
    }

    /**
     * Checks if parameters[1] is greater than parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> left parameter of the &gt; sign,
     *                      <br><code>parameters[2]</code> right parameter of the &gt; sign
     *                      <br><code>parameters[3]</code> [optional] variable name to store 'true/false' to (note: stops test being set to failed when not true)     
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int greaterThan(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": greaterThan: value1 value2");
            XTTProperties.printFail(this.getClass().getName()+": greaterThan: value1 value2 variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2 variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                double a=0;
                double b=0;
                //a = (Long.decode(parameters[1])).longValue();
                //b = (Long.decode(parameters[2])).longValue();
                a = Double.valueOf(parameters[1]);
                b = Double.valueOf(parameters[2]);

                if(a > b)
                {
                    XTTProperties.printInfo(parameters[0]+": "+a+" is greater than "+b+"");
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'true'");
                        XTTProperties.setVariable(parameters[3],"true");
                    }
                }
                else
                {

                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": "+a+" is NOT greater than "+b+"");
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'false'");
                        XTTProperties.setVariable(parameters[3],"false");

                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": "+a+" is NOT greater than "+b+"");
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);                 
                    }                    
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    
    /**
     * Checks if parameters[1] is greater than parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> left parameter of the &gt; sign,
     *                      <br><code>parameters[2]</code> right parameter of the &gt; sign
     *                      <br><code>parameters[3]</code> [optional] variable name to store 'true/false' to (note: stops test being set to failed when not true)     
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int greaterThanOrEqual(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": greaterThanOrEqual: value1 value2");
            XTTProperties.printFail(this.getClass().getName()+": greaterThanOrEqual: value1 value2 variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2 variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                double a=0;
                double b=0;
                a = Double.valueOf(parameters[1]);
                b = Double.valueOf(parameters[2]);

                if(a >= b)
                {
                    XTTProperties.printInfo(parameters[0]+": "+a+" is greater than or equal to "+b+"");
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'true'");
                        XTTProperties.setVariable(parameters[3],"true");
                    }
                }
                else
                {

                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": "+a+" is NOT greater than or equal to"+b+"");
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'false'");
                        XTTProperties.setVariable(parameters[3],"false");

                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": "+a+" is NOT greater than or equal to "+b+"");
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);                
                    }                    
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Output string on tracing level info.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code>  and following are concationated to the string to output on info level.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int info(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": info: stringToView");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": stringToView");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            StringBuffer val=new StringBuffer("");
            for(int i=1;i<parameters.length;i++)
            {
                val.append(parameters[i]);
            }
            XTTProperties.printInfo(val.toString());
        }
        return status;
    }

    /**
     * Output string on tracing level fail and fails the test.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code>  and following are concationated to the string to output on info level.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int fail(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": fail: stringToView");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": stringToView");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            StringBuffer val=new StringBuffer("");
            for(int i=1;i<parameters.length;i++)
            {
                val.append(parameters[i]);
            }
            XTTProperties.printFail(val.toString());
            status = XTTProperties.FAILED;
            XTTProperties.setTestStatus(status);
        }
        return status;
    }
    /**
     * clear all variables.
     */
    protected void initialize()
    {
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
        uniqueCheck=new HashMap<String,HashSet<String>>();
    }

    /**
     * Checks if parameters[1] is less than parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> left parameter of the &lt; sign,
     *                      <br><code>parameters[2]</code> right parameter of the &lt; sign
     *                      <br><code>parameters[3]</code> [optional] variable name to store 'true/false' to (note: stops test being set to failed when not true)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int lessThan(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": lessThan: value1 value2");
            XTTProperties.printFail(this.getClass().getName()+": lessThan: value1 value2 variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2 variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                double a=0;
                double b=0;
                a = Double.valueOf(parameters[1]);
                b = Double.valueOf(parameters[2]);
                if(a < b)
                {
                    XTTProperties.printInfo(parameters[0]+": "+a+" is less than "+b+"");
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'true'");
                        XTTProperties.setVariable(parameters[3],"true");
                    }
                }
                else
                {
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": "+a+" is NOT less than "+b+"");
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'false'");
                        XTTProperties.setVariable(parameters[3],"false");

                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": "+a+" is NOT less than "+b+"");
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status); 
                    }     
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    /**
     * Checks if parameters[1] is less than parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> left parameter of the &lt; sign,
     *                      <br><code>parameters[2]</code> right parameter of the &lt; sign
     *                      <br><code>parameters[3]</code> [optional] variable name to store 'true/false' to (note: stops test being set to failed when not true)
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int lessThanOrEqual(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": lessThanOrEqual: value1 value2");
            XTTProperties.printFail(this.getClass().getName()+": lessThanOrEqual: value1 value2 variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value1 value2 variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                double a=0;
                double b=0;
                a = Double.valueOf(parameters[1]);
                b = Double.valueOf(parameters[2]);
                if(a <= b)
                {
                    XTTProperties.printInfo(parameters[0]+": "+a+" is less than or equal to "+b+"");
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'true'");
                        XTTProperties.setVariable(parameters[3],"true");
                    }
                }
                else
                {
                    if(parameters.length > 3)
                    {
                        XTTProperties.printInfo(parameters[0]+": "+a+" is NOT less than or equal to "+b+"");
                        XTTProperties.printInfo(parameters[0]+": '" + parameters[3] + "' -> 'false'");
                        XTTProperties.setVariable(parameters[3],"false");

                    } else
                    {
                        XTTProperties.printFail(parameters[0]+": "+a+" is NOT less than or equal to "+b+"");
                        status = XTTProperties.FAILED;
                        XTTProperties.setTestStatus(status);                    
                    }     
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    /**
     * isEqualWithDeviation parameters[1] and parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> actual value parameter of the &lt; sign,
     *                      <br><code>parameters[2]</code> expected value parameter of the &lt; sign,
     *                      <br><code>parameters[3]</code> deviation parameter of the &lt; sign
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int equalWithDeviation(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": equalWithDeviation: actual value1 expected value2 deviation value3");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": actual value1 expected value2 deviation value3");
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } 
        else
        {
            try
            {
                double actual=Double.valueOf(parameters[1]);
                double expectedValue=Double.valueOf(parameters[2]);
                double deviation=Double.valueOf(parameters[3]);
                
                if ((actual- deviation) <= expectedValue   && (actual+deviation) >= expectedValue) 
                {
                	 XTTProperties.printInfo(parameters[0]+": "+  actual + " is as expected " + expectedValue + " deviation: " + deviation);
                     status = XTTProperties.PASSED;
                     XTTProperties.setTestStatus(status); 
    			} 
                else {
                	 XTTProperties.printFail(parameters[0]+": "+ actual + " is NOT as expected " + expectedValue + " deviation: " + deviation);
                     status = XTTProperties.FAILED;
                     XTTProperties.setTestStatus(status); 
    			}
                
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    /**
     * Analyze parameters[1] and parameter[2]
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> actual value parameter of the &lt; sign,
     *                      <br><code>parameters[2]</code> expected value parameter of the &lt; sign,
     *                      <br><code>parameters[3]</code> deviation in percentage parameter of the &lt; sign
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int equalWithDeviationPercentage(String parameters[])
    {
    	int status = XTTProperties.PASSED;
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": equalWithDeviationPercentage: actual value1 expected value2 deviation(%) value3");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3 || parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": actual value1 expected value2 deviation value3");
            return XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
        } 
        else
        {
            try
            {
                double actual=Double.valueOf(parameters[1]);
                double expectedValue=Double.valueOf(parameters[2]);
                double deviation=Double.valueOf(parameters[3]);
                double actualDeviation = expectedValue*deviation / 100;
                
                if ((actual - actualDeviation) <= expectedValue  && (actual + actualDeviation) >= expectedValue) 
                {
                	 XTTProperties.printInfo(parameters[0]+": "+  actual + " is as expected " + expectedValue + " deviation: " + deviation + "%");
                     status = XTTProperties.PASSED;
                     XTTProperties.setTestStatus(status); 
    			} 
                else 
                {
                	 XTTProperties.printFail(parameters[0]+": "+ actual + " is NOT as expected " + expectedValue + " deviation: " + deviation+"%");
                     status = XTTProperties.FAILED;
                     XTTProperties.setTestStatus(status); 
    			}
                
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": At least one parameter is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    /**
     * Multiply the integer values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> and following are the integer values to multiply
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int multiplyVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": multiplyVariable: variableName valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus="";
                long val=1;
                for(i=2;i<parameters.length;i++)
                {
                    val=val*(Long.decode(parameters[i])).longValue();
                    add.append(plus+"'"+parameters[i]+"'");
                    plus=" * ";
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    
    /**
     * Multiply the decimal values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> and following are the decimal values to multiply
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int multiplyDecimalVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": multiplyDecimalVariable: variableName valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus="";
                double val=1.0;
                for(i=2;i<parameters.length;i++)
                {
                    val=val*(Double.valueOf(parameters[i]));
                    add.append(plus+"'"+parameters[i]+"'");
                    plus=" * ";
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Compare a text with a regular expression.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the text to query,
     *                      <br><code>parameters[2]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[3]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ConvertLib#queryString(String,String,String,String)
     */
    public int queryText(String[] parameters)
    {
    	int status = XTTProperties.PASSED;	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryText: text variableTo regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": text variableTo regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[3]+"'");
            ConvertLib.queryString(parameters[0],parameters[1],parameters[3],parameters[2]);
        }
        return status;
    }
    public int queryTextNegative(String[] parameters)
    {
    	int status = XTTProperties.PASSED;	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryTextNegative: text regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": text regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
            return status;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[2]+"'");
            ConvertLib.queryStringNegative(parameters[0],parameters[1],parameters[2]);
        }
        return status;
    }

    /**
     * compare the http headers received by the client from the server with a value which is required.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable wiith the content to query,
     *                      <br><code>parameters[2]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[3]</code> argument is the regular expression.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see ConvertLib#queryString(String,String,String,String)
     */
    public int queryVariable(String[] parameters)
    {
    	int status = XTTProperties.PASSED;	
        if(parameters == null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryVariable: variableFrom variableTo regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableFrom variableTo regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
             return status;
        }
        else
        {
            XTTProperties.printDebug(parameters[0]+": regex: '"+parameters[3]+"'");
            ConvertLib.queryString(parameters[0],XTTProperties.getVariable(parameters[1]),parameters[3],parameters[2]);
        }
        return status;
    }

    public int queryVariableNegative(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": queryVariableMegative: variableName");
            XTTProperties.printFail(this.getClass().getName()+": queryVariableMegative: variableName regularExpression");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName regularExpression");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String variable=XTTProperties.getVariable(parameters[1]);
            if(parameters.length==2)
            {
                if(variable.equals("null"))
                {
                    XTTProperties.printInfo(parameters[0]+": Variable '"+parameters[1]+"' not found.");
                } else
                {
                    XTTProperties.printFail(parameters[0]+": Variable '"+parameters[1]+"' found.");
                    status = XTTProperties.FAILED;
                    XTTProperties.setTestStatus(status);
                }
            } else
            {
                ConvertLib.queryStringNegative(parameters[0],variable,parameters[2]);
            }
        }
        return status;
    }
    /**
     * Replace text in a variable.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable to replace parts therof
     *                     <br><code>parameters[2]</code> is the regular expression for the String.replaceAll function.
     *                     <br><code>parameters[3]</code> is the new data
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int replace(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": replace: variable regex newData");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if (parameters.length != 4)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variable regex newData");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
             return status;
        } else
        {
            String data = XTTProperties.getVariable(parameters[1]);
            if(data == null || data.equals("null"))
            {
                XTTProperties.printFail(parameters[0] + ": Variable '"+parameters[1]+"' not found.");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
            try
            {
                XTTProperties.printInfo(parameters[0]+": replacing "+"'"+parameters[2]+"' with '"+parameters[3]+"'");
                data = data.replaceAll(parameters[2],parameters[3]);
                XTTProperties.printInfo(parameters[0]+": replaced data");
            } catch (Exception e)
            {
                XTTProperties.printInfo(parameters[0]+": no data replaced");
            }
            XTTProperties.setVariable(parameters[1],data);
        }
        return status;
    }
    /**
     * Set the value of a local variable. Local variables are only valid inside the current &lt;subtest&gt;, &lt;loop&gt;, &lt;thread&gt; etc.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the name of the variable to use,
     *                     <br><code>parameters[2]</code> is the variable value
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setLocalVariable(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setLocalVariable: variableName value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        return XTTProperties.PASSED;
    }

    /**
     * Set the value of a variable.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the name of the variable to use,
     *                     <br><code>parameters[2]</code> is the variable value
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName");
            XTTProperties.printFail(this.getClass().getName()+": setVariable: variableName value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName value");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(parameters.length==2)
            {
                XTTProperties.printInfo(parameters[0]+": removing "+parameters[1]);
                XTTProperties.setVariable(parameters[1],null);
            }else
            {
                String show=parameters[2];
                if(show.length()>SHOWLENGTH)
                {
                    int leng=show.length();
                    show=show.substring(0,SHOWLENGTH)+"' plus "+(leng-SHOWLENGTH)+" bytes";
                } else
                {
                    show=show+"'";
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"='"+show);
                XTTProperties.setVariable(parameters[1],parameters[2]);
            }
        }
        return status;
    }
    /**
     * Set the value of a variable.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the regex for the variables name to use,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int removeVariables(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": removeVariables: regex");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": regex");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            XTTProperties.printInfo(parameters[0]+": removing with regex: "+parameters[1]);
            XTTProperties.removeVariables(parameters[1]);
        }
        return status;
    }
        
    /**
     * Set the value of a variable with multuple lines devided by CR/LF. This function is very good for creating content for SMTP,HTML etc. type protocols.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the after how many parameters a CR/LF should be inserted,
     *                     <br><code>parameters[2]</code> is the variable name
     *                     <br><code>parameters[3]</code> and following are the values for the lines
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int setVariableLines(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": setVariableLines: breakAfterNum variableName values");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": breakAfterNum variableName values");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int breakNum=0;
            try
            {
                breakNum=(Integer.decode(parameters[1])).intValue();
                if(breakNum<=0)
                {
                    XTTProperties.printFail(parameters[0]+": "+breakNum+" has to be greater than 0!");
                    status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                     return status;
                }
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is not a number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
                 return status;
            }
            StringBuffer val=new StringBuffer("");
            for(int i=3;i<parameters.length;i++)
            {
                val.append(parameters[i]);
                if((i-2)%breakNum==0)
                {
                    val.append("\r\n");
                }
            }
            XTTProperties.setVariable(parameters[2],val.toString());
            if(XTTProperties.printVerbose(null))
            {
                XTTProperties.printVerbose(parameters[0]+": "+parameters[2]+" set to:\n"+val.toString());
            } else
            {
                XTTProperties.printInfo(parameters[0]+": "+parameters[2]+" set");
            }
        }
        return status;
    }

    /**
     * Do a Thread.sleep() for a specified amount of miliseconds.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> integer value of miliseconds to wait.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int sleep(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": sleep: sleepTimeInMilliseconds");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        XTTProperties.printDebug(this.getClass().getName()+": sleep");
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": sleepTimeInMilliseconds");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            try
            {
                long sleepTime=Long.parseLong(parameters[1]);
                XTTProperties.printInfo(parameters[0]+": waiting for "+sleepTime+" milliseconds");
                Thread.sleep(sleepTime);
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            } catch (Exception e)
            {
                XTTProperties.printFail(parameters[0]+": Failed "+e.getClass().getName());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Subtract the integer values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> argument is the integer values to subtract from
     *                      <br><code>parameters[3]</code> and following are the integer values to subtract
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int subtractVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": subractVariable: variableName initialValue valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName initialValue valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus=" - ";
                long val=0;
                val= (Long.decode(parameters[2])).longValue();

                for(i=3;i<parameters.length;i++)
                {
                    val=val-(Long.decode(parameters[i])).longValue();
                    add.append(plus+"'"+parameters[i]+"'");
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+parameters[2]+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    
    /**
     * Subtract the integer values of the parameters.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the variable to store the result to,
     *                      <br><code>parameters[2]</code> argument is the decimal values to subtract from
     *                      <br><code>parameters[3]</code> and following are the decimal values to subtract
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int subtractDecimalVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": subtractDecimalVariable: variableName initialValue valueX");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName initialValue valueX");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            int i=2;
            try
            {
                StringBuffer add=new StringBuffer("");
                String plus=" - ";
                //long val=0;
                double val=0;
               // val= (Long.decode(parameters[2])).longValue();
                val= Double.valueOf(parameters[2]);

                for(i=3;i<parameters.length;i++)
                {
                    //val=val-(Long.decode(parameters[i])).longValue();
                    val=val-Double.valueOf(parameters[i]);
                    add.append(plus+"'"+parameters[i]+"'");
                }
                XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"= "+parameters[2]+add+" = "+val);
                XTTProperties.setVariable(parameters[1],val+"");
            } catch (NumberFormatException nfe)
            {
                XTTProperties.printFail(parameters[0]+": parameter"+i+" is not a number: '"+parameters[i]+"'");
                status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Set the name of the test.
     *
     * @deprecated Use <code>&gt;name&lt;TEST NAME&gt;/name&lt;</code> instead.
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the testname
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int testName(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": testName: nameOfTest");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": testName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            // Print name on Info level
            XTTProperties.printInfo(parameters[0]+": setting Name to: "+parameters[1]);
            // Actually set the Name
            XTTProperties.setCurrentTestName(parameters[1]);
        }
        return status;
    }
    /**
     * Store the difference, measured in milliseconds, between the current time and midnight, January 1, 1970 UTC.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable to store the value in.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int timeToVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": timeToVariable: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            long time=System.currentTimeMillis();
            XTTProperties.printInfo(parameters[0]+": stored '"+time+"' to "+parameters[1]);
            XTTProperties.setVariable(parameters[1],""+time);
        }
        return status;
    }
    /**
     * Creates a random positive long and stores to the given variable.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the variable to store the value in.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.     
     */
    public int randomToVariable(String parameters[], HashMap<String,String> localVars)
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": randomToVariable: variableName");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": variableName");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            long rand = RandomLib.getRandomLong();
            rand = Math.abs(rand);
            XTTProperties.printInfo(parameters[0]+": stored '"+rand+"' to "+parameters[1]);
            XTTProperties.setVariable(parameters[1],""+rand);
        } 
        return status;
    }

    /**
     * For Debug resons.
     *
     * @return      String containing the classname of this FunctionModule.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    /**
     * Uses a <code>HashSet</code> to check if a value is unique over the duration of a test. Function does a Fail if the same value is used again as a parameter.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name,
     *                     <br><code>parameters[1]</code> is the value of the variable to use,
     *                     <br><code>parameters[2]</code> (optional) is the name of the set,
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public synchronized int uniqueCheck(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": uniqueCheck: value");
            XTTProperties.printFail(this.getClass().getName()+": uniqueCheck: name value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=2 && parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": value");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name value");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            String uniqueKey=null;
            String uniqueValue=null;
            if(parameters.length == 3)
            {
                uniqueKey=parameters[1];
                uniqueValue=parameters[2];
            }
            else if(parameters.length == 2)
            {
                uniqueValue=parameters[1];
            }
            HashSet<String> thisSet = uniqueCheck.get(uniqueKey);
            if(thisSet == null)
            {
                thisSet=new HashSet<String>();
                XTTProperties.printInfo(parameters[0]+": '"+uniqueValue+"' is unique");
                thisSet.add(uniqueValue);
                uniqueCheck.put(uniqueKey,thisSet);
            } else if(thisSet.contains(uniqueValue))
            {
                XTTProperties.printFail(parameters[0]+": '"+uniqueValue+"' is NOT unique");
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            } else
            {
                XTTProperties.printInfo(parameters[0]+": '"+uniqueValue+"' is unique");
                thisSet.add(uniqueValue);
            }
        }
        return status;
    }

    /**
     * Reset the <code>HashSet</code> used for {@link #uniqueCheck(java.lang.String[]) uniqueCheck(java.lang.String[])}.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br/><code>parameters[0]</code> argument is always the method name,
     *                     <br/><code>parameters[1]</code> (optional) is the name of the set to reset.
     *                     <br/>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public synchronized int uniqueCheckReset(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": uniqueCheckReset:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": uniqueCheckReset: name");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=1 && parameters.length!=2)
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": name");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(parameters.length == 2)
            {
                uniqueCheck.remove(parameters[1]);
                XTTProperties.printInfo(parameters[0]+": '"+parameters[1]+"' reset.");
            }
            else
            {
                uniqueCheck.clear();
                XTTProperties.printInfo(parameters[0]+": uniqueCheck reset.");
            }
        }
        return status;
    }


    /**
     * Wait for a variable to be set. Timeout if no variable is set in CONFIGURATION/VARIABLEWAITTIMEOUT miliseconds.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name, no more arguments required.
     *                     <br><code>parameters[1]</code> is the name of the variable to wait for.
     *                     <br><code>parameters[1]</code> is the optional value of the variable.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     * @see XTTProperties#waitForVariable(String)
     */
    public int waitForVariable(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitForVariable: variableName");
            XTTProperties.printFail(this.getClass().getName()+": waitForVariable: variableName value");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<2||parameters.length>3)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName");
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+": variableName value");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
             return status;
        } else
        {
            try
            {
                if(parameters.length>2)
                {
                    XTTProperties.printInfo(parameters[0]+": '"+parameters[1]+"' value '"+parameters[2]+"'");
                    XTTProperties.waitForVariable(parameters[1],parameters[2]);
                } else
                {
                    XTTProperties.printInfo(parameters[0]+": '"+parameters[1]+"'");
                    XTTProperties.waitForVariable(parameters[1]);
                }
                 return status;
            } catch(Exception e)
            {
                XTTProperties.printFail(parameters[0] + ": ERROR");
                XTTProperties.printException(e);
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }

    /**
     * Wait until all threads started by &lt;thread&gt; have finished.
     * This is automatically done at the end of every test but could be usefull to call ths function
     * so you can stop servers etc. after the threads have finished.
     *
     * @param parameters   array of String containing the parameters.
     *                     <br><code>parameters[0]</code> argument is always the method name.
     *                     <br><code>parameters[1]</code> optional integer value of the amount of threads allowed running.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int waitOnThreads(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": waitOnThreads:"+NO_ARGUMENTS);
            XTTProperties.printFail(this.getClass().getName()+": waitOnThreads: numThreads");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length>2)
        {
            XTTProperties.printFail(parameters[0]+":"+NO_ARGUMENTS);
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": numThreads");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            if(parameters.length>1)
            {
                try
                {
                    int numthreads=Integer.decode(parameters[1]);
                    XTTProperties.printInfo(parameters[0] + ": Waiting for all but "+numthreads+" threads started during test to end");
                    Parser.waitOnThreads(numthreads);
                } catch (NumberFormatException nfe)
                {
                    XTTProperties.printFail(parameters[0]+": '"+parameters[1]+"' is NOT a correct number");
                    status = XTTProperties.FAILED_WITH_INVALID_ARGUMENTS;
                    XTTProperties.setTestStatus(status);
                }
            } else
            {
                XTTProperties.printInfo(parameters[0] + ": Waiting for all threads started during test to end");
                Parser.waitOnThreads();
            }
        }
        return status;
    }

    /**
     * Decodes base64 and writes the result to a file.
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the file name relative to the location of the current test file,
     *                      <br><code>parameters[2]</code> is the base64 encoded information to decode
                            <br><code>parameters[2]</code> (optional) boolean value specifying if the file should be appended to
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int writeBase64File(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": writeBase64File: fileName base64Value");
            XTTProperties.printFail(this.getClass().getName()+": writeBase64File: fileName base64Value append");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName base64Value");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName base64Value append");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            XTTProperties.printInfo(parameters[0]+": writing binary file "+parameters[1]);
            boolean append=false;
            if(parameters.length>3)
            {
                append=ConvertLib.textToBoolean(parameters[3]);
            }
            try
            {
                java.io.BufferedOutputStream out=new java.io.BufferedOutputStream(new java.io.FileOutputStream(XTTProperties.getCurrentTestPath()+parameters[1],append));
                byte[] data=ConvertLib.base64Decode(parameters[2]);
                //XTTProperties.printDebug("Writing file size: "+data.length+" bytes name: "+XTTProperties.getCurrentTestPath()+parameters[1]);
                out.write(data, 0, data.length);
                out.flush();
                out.close();
            }
            catch (Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+": Error writing file: "+ex.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }

        }
        return status;
    }

    /**
     * Write a value to a file
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> argument is the file name relative to the location of the current test file,
     *                      <br><code>parameters[2]</code> is the value to write into the file
                            <br><code>parameters[2]</code> (optional) boolean value specifying if the file should be appended to
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int writeFile(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": writeFile: fileName value");
            XTTProperties.printFail(this.getClass().getName()+": writeFile: fileName value append");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length<3||parameters.length>4)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName value");
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": fileName value append");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
        } else
        {
            XTTProperties.printInfo(parameters[0]+": writing file "+parameters[1]);
            boolean append=false;
            if(parameters.length>3)
            {
                append=ConvertLib.textToBoolean(parameters[3]);
            }
            try
            {
                java.io.BufferedOutputStream out=new java.io.BufferedOutputStream(new java.io.FileOutputStream(XTTProperties.getCurrentTestPath()+parameters[1],append));
                byte[] data=ConvertLib.createBytes(parameters[2]);
                //XTTProperties.printDebug("Writing file size: "+data.length+" bytes name: "+XTTProperties.getCurrentTestPath()+parameters[1]);
                out.write(data, 0, data.length);
                out.flush();
                out.close();
            } catch (java.io.IOException ex)
            {
                XTTProperties.printFail(this.getClass().getName()+": Error writing file: "+ex.getMessage());
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                status = XTTProperties.FAILED;
                XTTProperties.setTestStatus(status);
            }
        }
        return status;
    }
    
    /**
     * To convert input string into lower case
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> input string to change into lower case
     *                      <br><code>parameters[2]</code> variable name to store  result.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int toLowerCase(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": toLowerCase: inputstring variablename");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": inputstring variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
		}
		else
		{
			if (parameters[1] == null || parameters[1].equals(""))
			{
				XTTProperties.printWarn("invalid input string '" + parameters[1] + "'");
			}
			else
			{
				String str = parameters[1].toLowerCase();
				XTTProperties.setVariable(parameters[2], str);
				XTTProperties.printInfo("Lower case string '" + str + "' stored to '" + parameters[2] + "'");

			}
		}
        return status;
	}
    /**
     * To convert input string into Upper case
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> input string
     *                      <br><code>parameters[2]</code> variable name to store  result.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int toUpperCase(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": toUpperCase: inputstring variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
        if(parameters.length!=3)
        {
            XTTProperties.printFail(parameters[0]+":"+MISSING_ARGUMENTS+": inputstring variable");
            status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
            XTTProperties.setTestStatus(status);
		}
		else
		{
			if (parameters[1] == null || parameters[1].equals(""))
			{
				XTTProperties.printWarn("invalid input string '" + parameters[1] + "'");
			}
			else
			{
				String str = parameters[1].toUpperCase();
				XTTProperties.setVariable(parameters[2], str);
				XTTProperties.printInfo("Upper case string '" + str + "' stored to '" + parameters[2] + "'");

			}

		}
        return status;
	}
    /**
     * To convert input string into hex MD5 encoded string
     *
     * @param parameters   array of String containing the parameters.
     *                      <br><code>parameters[0]</code> argument is always the method name,
     *                      <br><code>parameters[1]</code> input string
     *                      <br><code>parameters[2]</code> variable name to store  result.
     *                     <br>If null is used as <code>parameters</code> it sends the allowed parameters list
     *                     to the {@link XTTProperties#printFail(java.lang.String) XTTProperties.printFail(java.lang.String)} method and returns.
     */
    public int getHexMD5Hash(String parameters[])
    {
    	int status = XTTProperties.PASSED;	
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": getHexMD5Hash: inputstring variable");
            return XTTProperties.FAILED_NO_ARGUMENTS;
        }
		if (parameters.length != 3)
		{
			XTTProperties.printFail(parameters[0] + ":" + MISSING_ARGUMENTS + ": inputstring variable");
			status = XTTProperties.FAILED_WITH_MISSING_ARGUMENTS;
			XTTProperties.setTestStatus(status);
		}
		else
		{
			if (parameters[1] == null || parameters[1].equals(""))
			{
				XTTProperties.printWarn("invalid input string '" + parameters[1] + "'");
			}
			else
			{
				String str = ConvertLib.getHexMD5Hash(parameters[1]);
				XTTProperties.setVariable(parameters[2], str);
				XTTProperties.printInfo("Hex MD5 encoded string '" + str + "' stored to '" + parameters[2] + "'");
			}
		}
		return status;
	}

}