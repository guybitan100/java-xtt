package com.mobixell.xtt;


import java.util.HashMap;
import java.util.Vector;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.StringReader;

public class ModuleList
{
    final static String linePrefix="com.mobixell.xtt.FunctionModule_";
    private Vector<String> modules=new Vector<String>();
    private HashMap<String,Vector<String>> functions=new HashMap<String,Vector<String>>();
    private HashMap<String,Vector<String>> functionDescriptions=new HashMap<String,Vector<String>>();
    public ModuleList()
    {
        try
        {
            construct(new Parser());
        } catch (Exception e)
        {
            XTTProperties.printException(e);
        }
    }

    public ModuleList(Parser p)
    {
        construct(p);
    }
    
    private void construct(Parser p)
    {
        String currentLine=null;
        StringOutputStream stringStream=new StringOutputStream();
        PrintStream currentSystemOut=System.out;
        String currentFailComment=XTTProperties.FAILCOMMENT;
        int tracing=XTTProperties.getTracing();
        try
        {
            XTTProperties.setTracing(XTTProperties.FAIL);
            System.setOut(new PrintStream(stringStream));
            XTTProperties.FAILCOMMENT="";
            XTTProperties.setTestList((Vector<XTTTest>)null);
            p.dumpFunctions();
            System.setOut(currentSystemOut);
            XTTProperties.FAILCOMMENT=currentFailComment;
            XTTProperties.setTracing(tracing);

            BufferedReader in=new BufferedReader(new StringReader(stringStream.toString()));

            String[] contentLine=null;

            Vector<String> f=null;
            Vector<String> fd=null;

            while((currentLine=in.readLine())!=null)
            {
                contentLine=currentLine.split(linePrefix);
                //System.out.println(contentLine[0]+" - "+contentLine[1]);
                if(contentLine.length==2)
                {
                    contentLine=contentLine[1].split(": ");
                    //System.out.println(contentLine[0]+" - "+contentLine[1]);
                    if(modules.indexOf(contentLine[0])==-1)
                    {
                        modules.add(contentLine[0]);
                    }
                    f=functions.get(contentLine[0]);
                    if(f==null){f=new Vector<String>();}
                    if(f.indexOf(contentLine[1])==-1)
                    {
                        f.add(contentLine[1]);
                    }
                    functions.put(contentLine[0],f);

                    fd=functionDescriptions.get(contentLine[0]+": "+contentLine[1]);
                    if(fd==null){fd=new Vector<String>();}
                    fd.add(contentLine[2]);
                    functionDescriptions.put(contentLine[0]+": "+contentLine[1],fd);
                }
            }

            //System.out.println(functions);
        } catch (Exception e)
        {
            if(System.out!=currentSystemOut)
            {
               System.setOut(currentSystemOut);
               XTTProperties.FAILCOMMENT=currentFailComment;
            }
            XTTProperties.printFail("MODULE EXCEPTION: MODULE FUNCTION LINE:\n"+currentLine);
            XTTProperties.printException(e);
        }
    }
    public Vector<String> getModules()
    {
        return modules;
    }
    public Vector<String> getModuleFunctions(String module)
    {
        return functions.get(module);
    }
    public Vector<String> getModuleFunctionsDescriptions(String module, String function)
    {
        return functionDescriptions.get(module+": "+function);
    }

    public static final String tantau_sccsid = "@(#)$Id: ModuleList.java,v 1.6 2008/01/10 14:06:54 rsoder Exp $";
}