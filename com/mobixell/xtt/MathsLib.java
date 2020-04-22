package com.mobixell.xtt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MathsLib
{
    public static Double calc(String expression) throws NumberFormatException
    {
        StringBuffer workingExpression = new StringBuffer(expression);
        handleParentheses(workingExpression);
        handleExponents(workingExpression);
        handleMultiplicationAndDivision(workingExpression);
        handleAdditionAndSubtraction(workingExpression);
        
        return Double.parseDouble(workingExpression.toString());
    }
    
    private static void handleParentheses(StringBuffer workingExpression) throws NumberFormatException
    {
        String newValue = "";
        for(int i=0;i<workingExpression.length();i++)
        {
            if(workingExpression.charAt(i) == '(')
            {
                newValue = matchParentheses(workingExpression.toString(),i);
                if(newValue!=null)
                {
                    workingExpression.replace(i, i+newValue.length()+2, ""+calc(newValue));
                    //XTTProperties.printDebug("MathsLib: New expression is: " + workingExpression.toString());
                }
            }
        }
    }
    
    private static void handleExponents(StringBuffer workingExpression)
    {
        String operation="\\^";
        Matcher matcher = matchOperation(workingExpression,operation);
        
        Double a = 0d;
        Double b = 0d;
        
        while(matcher.find())
        {
            //XTTProperties.printDebug("MathsLib: found " + matcher.group(0) + " starting at " + matcher.start()  + " ending at " + matcher.end());
            a = Double.parseDouble(matcher.group(1));
            b = Double.parseDouble(matcher.group(2));
            Double d = Math.pow(a,b);
            workingExpression.replace(matcher.start(), matcher.end(), d.toString());
            //XTTProperties.printDebug("MathsLib: New expression is: " + workingExpression.toString());
            matcher = matchOperation(workingExpression,operation);
        }
    }
    
    private static void handleMultiplicationAndDivision(StringBuffer workingExpression)
    {
        String operation="(\\/|\\*|\\%)";
        Matcher matcher = matchOperation(workingExpression,operation);
        
        Double a = 0d;
        Double b = 0d;
        
        while(matcher.find())
        {
            //XTTProperties.printDebug("MathsLib: found " + matcher.group(0) + " starting at " + matcher.start()  + " ending at " + matcher.end());
            a = Double.parseDouble(matcher.group(1));
            b = Double.parseDouble(matcher.group(3));
            Double d = 0d;
            if(matcher.group(2).equals("*"))
            {
                d = a * b;
            }
            else if(matcher.group(2).equals("/"))
            {
                d = a / b;
            }
            else
            {
                d = a % b;    
            }
            workingExpression.replace(matcher.start(), matcher.end(), d.toString());
            //XTTProperties.printDebug("MathsLib: New expression is: " + workingExpression.toString());
            matcher = matchOperation(workingExpression,operation);
        }
    }
    
    private static void handleAdditionAndSubtraction(StringBuffer workingExpression)
    {
        String operation="(\\-|\\+)";
        Matcher matcher = matchOperation(workingExpression,operation);
        
        Double a = 0d;
        Double b = 0d;
        
        while(matcher.find())
        {
            //XTTProperties.printDebug("MathsLib: found " + matcher.group(0) + " starting at " + matcher.start()  + " ending at " + matcher.end());
            a = Double.parseDouble(matcher.group(1));
            b = Double.parseDouble(matcher.group(3));
            Double d = 0d;
            if(matcher.group(2).equals("+"))
            {
                d = a + b;
            }
            else
            {
                d = a - b;
            }
            workingExpression.replace(matcher.start(), matcher.end(), d.toString());
            //XTTProperties.printDebug("MathsLib: New expression is: " + workingExpression.toString());
            matcher = matchOperation(workingExpression,operation);
        }
    }
    
    private static Matcher matchOperation(StringBuffer workingExpression, String operation)
    {
        /*
        The regex explained: ((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)\\p{Space}*" + operation + "\\p{Space}*((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)
        
        group1: ((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)
            (?:\\-(?<![0-9]))? 
                A non capturing group of a '-' with a negative lookbehind for any digit, this can occur once, or not at all.
                This is so you can find negative numbers without being confused by a minus sign as an operation
            [0-9]+\\.?[0-9]*
                Search for at least one digit possibly followed by a '.' followed by zero or more digits
                This is to find real numbers
            E?[0-9]*
                Search for an E followed by any number of digits. This is because a Double can change to the format 5.845E8 meaning 5.845x10^8
        
        \\p{Space}* 
            Simple check to remove whitespace between numbers and operations
        
        operation 
            This is for the operation used in each match, normally \+, \-, \* or \/ can also be (\-|\+) to match either - or + and capture as a group
            
        \\p{Space}*
            Remove the whitespace to the right of the operator
            
        group2: ((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)
            (?:\\-(?<![0-9]))? 
                See group1, this is to match negative signs
            [0-9]+\\.?[0-9]*
                See group1, This is to find real numbers
            E?[0-9]*
                See group1, This is because a Double can change to the format 5.845E8 meaning 5.845x10^8
        
        */
        String regex = "((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)\\p{Space}*" + operation + "\\p{Space}*((?:\\-(?<![0-9]))?[0-9]+\\.?[0-9]*E?[0-9]*)";
        //XTTProperties.printDebug("MathLib: Using regex: " + regex);
        Pattern pattern = Pattern.compile(regex,Pattern.DOTALL);
        return pattern.matcher(workingExpression);    
    }
    
    private static String matchParentheses(String data, int offset)
    {
        int parenthesesCount = 0;
        int parenthesesStart = offset;
        int parenthesesEnd = -1;
    
        if(data.charAt(parenthesesStart) == '(')
        {
            //Loop round the data counting opening and closing parentheses, unless there wasn't an opening parentheses found.
            for(int i=parenthesesStart;i<data.length();i++)
            {
                if(data.charAt(i) == '(') parenthesesCount++;
                if(data.charAt(i) == ')') parenthesesCount--;
                if(parenthesesCount==0)
                {   
                    parenthesesEnd = i+1;
                    break;
                }
            }
            try
            {
                data = data.substring(parenthesesStart+1,parenthesesEnd-1);
            }
            catch(StringIndexOutOfBoundsException sioobe)
            {
                XTTProperties.printFail("MathsLib: Parentheses mismatch in '"+data+"'");
                data = null;
            }
            return data;
        }
        else
        {
            XTTProperties.printWarn("MathsLib: No opening parentheses found in '"+data+"' at pos " + parenthesesStart);
            return null;    
        }        
    }         
}