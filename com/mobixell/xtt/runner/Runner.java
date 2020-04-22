package com.mobixell.xtt.runner;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.concurrent.PriorityBlockingQueue;

public class Runner
{
    private static PriorityBlockingQueue<TestQueuer> testQueue = new PriorityBlockingQueue<TestQueuer>();

    public void executeXTT()
    {
        try
        {
            TestQueuer currentTest = testQueue.poll();
            
            Process xtt = null;
            BufferedReader input = null;
            String line = null;
            while(!currentTest.finished())
            {
                String xttCommand = currentTest.runTest();
                System.out.println("@Executing: " + xttCommand);
                
                xtt = Runtime.getRuntime().exec(xttCommand);
                input = new BufferedReader (new InputStreamReader(xtt.getInputStream()));
                line = null;
                
                while ((line = input.readLine()) != null) {
                    System.out.println(line);
                }
                input.close();
                
                int returnCode = xtt.exitValue();
                System.out.println("@XTT " + com.mobixell.xtt.XTTProperties.getStatusDescription(returnCode));
            }
            
            //xtt.waitFor();    
            System.out.println("@XTT Runner Done");
        } catch (Exception e) 
        {
            e.printStackTrace();    
        }           
    }

    public Runner()
    {
        TestQueuer testTest = new TestQueuer("test","test");
        testQueue.add(testTest);
        executeXTT();        
    }

    public static void main(String[] args) 
    {
        try
        {
            Runner runner = new Runner();
        }
        catch(Exception e)
        {
            e.printStackTrace();    
        }
    } 
    
    /**
     * TestQueuer is the Queue member for a test run of XTT.
     * It uses the <code>Comparable</code> interface to the Queue can sort based on time of execution.
     * Note: this class has a natural ordering that is inconsistent with equals.
     */
    private class TestQueuer implements Comparable
    {
        private String genericPrefix    = "";
        private String javaLocation     = "java";
        private String javaFlags        = "-Xmx512m";
        private String xttClass         = "com.mobixell.xtt.XTT";
        private Vector<String> xttTests = new Vector<String>();
        private String xttConfig        = "";
        //Only additional flags apart from tests or configs
        private String xttFlags         = "";
        private String genericSuffix    = "";
        
        private java.util.GregorianCalendar timeToRun = new java.util.GregorianCalendar();
        
        private int currentTest = 0;
        
        public TestQueuer(Vector<String> tests, String config)
        {
            xttTests=tests;
            currentTest=0;
            
            //This will take a list delimited by spaces and add the proper config loading prefix for XTT.
            if(config==null)config="";
            String[] configs = config.split(" ");
            xttConfig = "-c " + configs[0];
            for(int i=1;i<configs.length;i++)
            {
                xttConfig += " -c" + configs[i];
            }    
        }
        public TestQueuer(String test, String config)
        {
            xttTests.clear();
            xttTests.add(test);            
            currentTest=0;
            
            //This will take a list delimited by spaces and add the proper config loading prefix for XTT.
            if(config==null)config="";
            String[] configs = config.split(" ");
            xttConfig = "-c " + configs[0];
            for(int i=1;i<configs.length;i++)
            {
                xttConfig += " -c" + configs[i];
            }                 
        }
        
        public void runIn(int days, int hrs, int mins)
        {
            timeToRun.add(java.util.GregorianCalendar.DATE, days);
        }    
        
        public String runTest()
        {
            if(currentTest>=xttTests.size())
            {
                return null;    
            }    
            String xttCommand = genericPrefix + " " + javaLocation + " " + javaFlags + " " + xttClass + " " + xttConfig + " -s " + xttTests.get(currentTest) + " " + xttFlags + " " + genericSuffix;
            currentTest++;
            return xttCommand;
        }
        
        public int compareTo(Object anotherTestQueuer)
        {
            TestQueuer temp = (TestQueuer)anotherTestQueuer;
            return timeToRun.compareTo(temp.getTimeToRun());
        }
        
        public java.util.GregorianCalendar getTimeToRun()
        {
            return timeToRun;
        }
        
        public boolean finished()
        {
            if(currentTest>=xttTests.size())
            {
                return true;    
            } else
            {
                return false;    
            }   
        }
    }   
}