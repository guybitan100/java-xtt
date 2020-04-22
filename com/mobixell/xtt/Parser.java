package com.mobixell.xtt;

import java.util.TreeSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;
import java.util.Vector;
import java.util.List;
import java.io.File;
import java.io.FileInputStream;
import java.util.jar.JarFile;
import java.lang.reflect.Constructor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Attribute;

import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
import com.mobixell.xtt.gui.testlaunch.tree.nodes.AssetNode;

/**
 * Parser is responsible for converting a test xml file into single executable commands which
 * are executed from the {@link FunctionModule} classes. Also requires the org.jdom classes.
 *
 * @author      Roger Soder
 * @version     $Id: Parser.java,v 1.69 2009/06/25 11:40:28 rsoder Exp $
 * @see         FunctionModule
 * @see         <A HREF="http://jdom.org/docs/apidocs/index.html">JDOM</A>
 */
public class Parser
{
    private TreeSet<FunctionModule> functionModules=new TreeSet<FunctionModule>();
    private HashMap<String,FunctionModule> functionModuleMap=new HashMap<String,FunctionModule>();
    // name of the jarfile to search for the classes in
    private static final String JARFILE=XTTProperties.JARFILE;
    private static final String LIBDIR="lib";
    // the package name used mainly for the length of the variable
    private static final String packageName="com.mobixell.xtt.";
    // used mainly for the length of the variable
    private static final String functionPrefix="FunctionModule_";
    private static Vector<Thread> threadList=new Vector<Thread>();
    private static final String PARAMETER_ERROR="parameter decode error";
    private static boolean abortTestExecution=false;
    public static void setAbortTestExecution(boolean set){abortTestExecution=set;}
    /**
     * loads all the {@link FunctionModule} classes in the directory com/mobixell/xtt or from the JAR file XTT.jar.
     * Currently only loads from the directory where XTT is called. If it is not executed from
     * where the JAR file is it will not load any modules from the JAR.
     *
     * @see         FunctionModule
     */
    public Parser() throws Exception
    {
        this(true);
    }
    public Parser(boolean doInit) throws Exception
    {
        XTTProperties.printDebug("Parser: Constructing Parser() of XTT Version: "+XTTProperties.getXTTBuildVersion());
        String [] [] productsVersions = FunctionModule_Remote.getProducts();
        if (productsVersions!=null)
        {
        	String productVerFormat ="XMP Information\n\n";
        	for (int i = 0; i < productsVersions.length; i++) 
        	{
        		productVerFormat +=productsVersions[i][0]+": ";
        		productVerFormat +=productsVersions[i][1] + "\n";
        	}
        	 XTTProperties.printInfo(productVerFormat);
		}
        String buildDate="";
        if(!XTTProperties.getXTTBuildTimeStamp().equals(""))buildDate=" Build Date: "+XTTProperties.getXTTBuildTimeStamp();
        XTTProperties.printDebug("Parser:"+buildDate+" Tools Versions:"
            +"\n  Parser       : "+FunctionModule.parseVersion(Parser.tantau_sccsid)
            +"\n  HTTPHelper   : "+FunctionModule.parseVersion(HTTPHelper.tantau_sccsid)
            +"\n  ConvertLib   : "+FunctionModule.parseVersion(ConvertLib.tantau_sccsid)
            +"\n  XTTXML       : "+FunctionModule.parseVersion(XTTXML.tantau_sccsid)
            +"\n  XTTProperties: "+FunctionModule.parseVersion(XTTProperties.tantau_sccsid)
        );
        FunctionModule obj=null;
        try
        {
            // create the directory to search the class files in when not reading from the jar
            String directoryPrefix="com"+File.separator+"mobixell"+File.separator+"xtt"+File.separator;
            //System.out.println(System.getProperty("java.class.path"));
            // open the directory
            File directory=new File(directoryPrefix);
            JarFile jardir=null;
            Vector<String> jarContents=new Vector<String>();
            try
            {
                jardir=new JarFile(new File(JARFILE));
            } catch (Exception e1)
            {
                try
                {
                    jardir=new JarFile(new File("."+System.getProperty("path.separator")+LIBDIR+System.getProperty("path.separator")+JARFILE));
                } catch (Exception e2)
                {
                    String classpath[]=System.getProperty("java.class.path").split(System.getProperty("path.separator"));
                    for(int i=0;i<classpath.length;i++)
                    {
                        if(classpath[i].equals(JARFILE)||classpath[i].endsWith(File.separator+JARFILE))
                        {
                            try
                            {
                                jardir=new JarFile(classpath[i]);
                            } catch (Exception e3)
                            {
                            }
                        }
                    }
                }
            }
            if(jardir!=null)
            {
                Enumeration<?> en=jardir.entries();
                while(en.hasMoreElements())
                {
                    jarContents.add(en.nextElement().toString());
                }
            } else
            {
                XTTProperties.printWarn("Parser(): Jar-File "+JARFILE+" not found in current or '"+LIBDIR+"' directory or CLASSPATH: "+System.getProperty("java.class.path"));
            }
            String fileList2[]=directory.list();
            if(fileList2==null)
                fileList2=new String[0];
            String fileList[]=new String[fileList2.length+jarContents.size()];
            int count=0;
            for(int i=0;i<fileList2.length;i++)
            {
                fileList[count++]=directoryPrefix+fileList2[i];
            }
            for(int i=0;i<jarContents.size();i++)
            {
                fileList[count++]=jarContents.get(i);
            }
            String currentFile=null;
            //URL u[]={new URL("file:.")};
            ClassLoader loader=this.getClass().getClassLoader();
            Class<?> c=null;
            Constructor<?> con=null;
            String shortModuleName=null;
            String version="";
            for(int i=0;i<fileList.length;i++)
            {
                if(fileList[i].startsWith(functionPrefix,directoryPrefix.length())
                    && fileList[i].endsWith(".class"))
                {
                    currentFile=fileList[i];
                    try
                    {
                        for(int j=directoryPrefix.length()+functionPrefix.length();j<currentFile.length();j++)
                        {
                            //System.out.println(j+": '"+currentFile.charAt(j)+"'=='"+"'$' ? "+(currentFile.charAt(j)=='$'));
                            if(currentFile.charAt(j)=='$')break;
                            if(currentFile.charAt(j)=='.')
                            {
                                //System.out.println(currentFile.substring(0,j));
                                shortModuleName=currentFile.substring(directoryPrefix.length()+functionPrefix.length(),j);
                                if(functionModuleMap.keySet().contains(shortModuleName.toLowerCase()))
                                {
                                    XTTProperties.printWarn("Parser(): Duplicated Module found: "+shortModuleName+" already loaded");
                                    break;
                                }
                                c=loader.loadClass(packageName+currentFile.substring(directoryPrefix.length(),j));
                                con=c.getConstructor(new Class[0]);
                                obj=(FunctionModule)con.newInstance(new Object[0]);
                                version = obj.getVersion();
                                XTTProperties.printDebug("Parser(): Loaded Module: "+shortModuleName+" "+version);//+": "+currentFile.substring(0,j));
                                if(doInit){((FunctionModule)obj).initialize();}
                                functionModules.add(obj);
                                functionModuleMap.put(shortModuleName.toLowerCase(),obj);
                                break;
                            }
                        }
                    } catch(Throwable ex)
                    {
                        XTTProperties.printWarn(this.getClass().getName()+"(): "+ex.getClass().getName()+": "+currentFile+" could not be loaded");
                        if(XTTProperties.printDebug(null))
                        {
                            XTTProperties.printException(ex);
                        }
                    }
                }
            }
        } catch (ClassCastException cce)
        {
            XTTProperties.printFail(this.getClass().getName()+"(): "+cce.getClass().getName()+": "+obj.getClass().getName()+" not extending FunctionModule");
            throw cce;

        } catch (Exception e)
        {
            XTTProperties.printFail(this.getClass().getName()+": Constructor Failed: "+e.getClass().getName());
                //e.printStackTrace();
            throw e;
        }
    }

    /**
     * run a single Test from a test xml document.
     * <p>
     * The root node of the test xml document has to be <b>&lt;test&gt;</b>
     * <p>
     * The parser currently understands the following tags inside <b>&lt;test&gt;</b>
     * <ul>
     * <li><b>&lt;configuration&gt;</b>configuration file to load<b>&lt;/configuration&gt;</b> used to override/add configuration with test specific stuff.
     *      Should only be used at the start of the test and only carefully in subtests.
     * <li><b>&lt;name&gt;</b>test name<b>&lt;/name&gt;</b> sets the name of the test, should be the first tag or right after the <b>&lt;configuration&gt;</b> tag
     * <li><b>&lt;description&gt;</b>Descriptive text<b>&lt;/description&gt;</b> used to display a short description of the test on external level
     *      and in the gui. Only uses the first line in the gui. Should be used right after the <b>&lt;name&gt;</b> tag.
     * <li><b>&lt;div comment="Some text"&gt;</b>any other tags<b>&lt;/div&gt;</b> used to display a short comment of on external trace level. This tag can have any
     *      other tag as a subtag for grouping in xml editors. New local variables will NOT be created. Attributes can also be called fail, warn, info, verbose 
     *      and debug which then prints the attributes values in the corresponding output levels. Any other attributes than those mentioned will be ignored.
     * <li><b>&lt;function</b> name="function name" module="module name extension (Basic/HTTP/Radius) etc." <b>&gt;</b>
     *   <ol><li><b>&lt;parameter&gt;</b>the value you want to set<b>&lt;/parameter&gt;</b><BR>
     *              A String used as a value in the function. the function decides wheter it should be in a special format (Number, String etc.) See {@link FunctionModule_Basic#setVariable FunctionModule_Basic#setVariable}.
     *       <li><b>&lt;variable&gt;</b>variable name<b>&lt;/variable&gt;</b><BR>
     *              Returns the content of an internal variable whichs name has been specified here. The format for variable names used by internal functions is MODULE/THIS/THAT/FINAL like in a directory structure.
     *       <li><b>&lt;variablepointer&gt;</b>name of variable<b>&lt;/variablepointer&gt;</b><BR>
     *              Returns the content of an internal variable whichs name has been stored in the variable whichs name is specified here.
     *              <br>The same as &lt;variable&gt;&lt;variable&gt;variable name&lt;/variable&gt;&lt;/variable&gt;.
     *       <li><b>&lt;localvariable&gt;</b>local variable name<b>&lt;/localvariable&gt;</b><BR>
     *              Like a normal variable with the exception that each &lt;loop&gt;, &lt;thread&gt; and &lt;subtest&gt; called gets a copy
     *              of this variable. Modification of this copy does not modify the original. as opposed to the normal globaly used &lt;variable&gt;
     *       <li><b>&lt;localvariablepointer&gt;</b>local variable name<b>&lt;/localvariablepointer&gt;</b><BR>
     *              Like a normal variable pointer with the exception that each &lt;loop&gt;, &lt;thread&gt; and &lt;subtest&gt; called gets a copy
     *              of this variable pointer. Modification of this copy does not modify the original.
     *              <br>The same as &lt;variable&gt;&lt;localvariable&gt;variable name&lt;/localvariable&gt;&lt;/variable&gt;.
     *       <li><b>&lt;locallocalvariablepointer&gt;</b>local variable name<b>&lt;/locallocalvariablepointer&gt;</b><BR>
     *              Like a normal local variable pointer with the exception that each &lt;loop&gt;, &lt;thread&gt; and &lt;subtest&gt; called gets a copy
     *              of this variable pointer and that it points to a local variable instead of a normal variable. Modification of this copy does not modify the original.
     *              <br>The same as &lt;localvariable&gt;&lt;localvariable&gt;variable name&lt;/localvariable&gt;&lt;/localvariable&gt;.
     *       <li><b>&lt;configuration&gt;</b>OUTERTAG/INNERTAG(in the configuration xml)<b>&lt;/configuration&gt;</b><BR>
     *              Returns the content of a configuration value. The format for used is FIRSTNODE/SECONDNODE/ETCNODES like in a directory structure.
     *       <li><b>&lt;file&gt;</b>file name<b>&lt;/file&gt;</b><BR>
     *              Returns the content of a text file as string. The file name is relative to the location of the test.
     *       <li><b>&lt;base64file&gt;</b>file name<b>&lt;/base64file&gt;</b><BR>
     *              Returns the content of a binary or text file as base64 encoded string. The file name is relative to the location of the test.
     *       <li><b>&lt;bytestringfile&gt;</b>file name<b>&lt;/bytestringfile&gt;</b><BR>
     *              Returns the content of a binary or text file as string representing the bytes of the file like &lt;byteintencode&gt;. The file name is relative to the location of the test.
     *       <li><b>&lt;base64encode&gt;</b>String<b>&lt;/base64encode&gt;</b><BR>
     *              Base64Encodes the string.
     *       <li><b>&lt;base64decode&gt;</b>base64String<b>&lt;/base64decode&gt;</b><BR>
     *              Base64Decodes to a string.
     *       <li><b>&lt;bytestringencode&gt;</b>String<b>&lt;/bytestringencode&gt;</b><BR>
     *              Creates a String which contains a String representation of the bytes of the input String. "ABC"->"414243".
     *       <li><b>&lt;bytestringdecode&gt;</b>String of bytes<b>&lt;/bytestringdecode&gt;</b><BR>
     *              Creates a String out of a String of bytes. "414243"->"ABC".
     *       <li><b>&lt;bytestringtobase64&gt;</b>String of bytes<b>&lt;/bytestringtobase64&gt;</b><BR>
     *              Creates a byte array out of a String of bytes like "414243", "41 42 43" or "0x41 0x42 0x43"  and then directly base64encodes this byte array.
     *       <li><b>&lt;base64tobytestring&gt;</b>base64Encoded data<b>&lt;/bytestringtobase64&gt;</b><BR>
     *              Base64Decodes a String to a byte array and then creates a string out of it like "414243".
     *       <li><b>&lt;byteintencode&gt;</b>integer<b>&lt;/byteintencode&gt;</b><BR>
     *              Creates a String representation of the bytes of an integer. "123456789"->"075BCD15".
     *       <li><b>&lt;inttohex&gt;</b>integer<b>&lt;/inttohex&gt;</b><BR>
     *              Creates a String representation of the hex value of the integer. There will be no leading 0x and always an even number of characters.
     *       <li><b>&lt;testpath&gt;</b>Filename<b>&lt;/testpath&gt;</b><BR>
     *              Creates a String C:\where\you\have\the\current\running\test\Filename (Of course on linux with linux paths).
     *       <li><b>&lt;bytelength&gt;</b>String<b>&lt;/bytelength&gt;</b><BR>
     *              Creates an Integer containing the length of a byte array in UTF-8 encoding of the String. Some Unicode characters may use multiple bytes.
     *       <li><b>&lt;base64bytelength&gt;</b>String<b>&lt;/bytelength&gt;</b><BR>
     *              Creates an Integer containing the length of a base64encoded byte array in UTF-8 encoding of the String.
     *       <li><b>&lt;stringlength&gt;</b>String<b>&lt;/stringlength&gt;</b><BR>
     *              Creates an Integer containing the length of the String in Unicode Charactes. Every single characters it has has a length of "1".
     *       <li><b>&lt;randomint&gt;</b>maxint<b>&lt;/randomint&gt;</b><BR>
     *              Creates a random number between 0 and maxint including 0 and excluding maxint.
     *       <li><b>&lt;randombytes&gt;</b>length<b>&lt;/randombytes&gt;</b><BR>
     *              Creates length random bytes as byteencoded string. If length is negative the number is random up to length bytes excluding length.
     *       <li><b>&lt;randomdigits&gt;</b>length<b>&lt;/randomdigits&gt;</b><BR>
     *              Creates length random digits as a string. If length is negative the number is random up to length digits excluding length.
     *       <li><b>&lt;crlf&gt;</b>the value you want to set<b>&lt;/crlf&gt;</b><BR>
     *              Same as parameter but it appends CR/LF carriage return and line feed at the end.
     *       <li>Each of these parameter tags can have any of those tags as subtags. Having more than one subtag will concat the result to
     *       one value like: &lt;parameter&gt;&lt;parameter&gt;house&lt;/parameter&gt;&lt;parameter&gt;boat&lt;/parameter&gt;&lt;/parameter&gt; is the same as &lt;parameter&gt;houseboat&lt;/parameter&gt;
     *  </ol>
     *     <b>&lt;/function&gt;</b>
     * <li><b>&lt;loop</b> name="counter variable name" start="counter start" stop="counter stop" step="step size"<b>&gt;</b>
     * <br>Works like for(int counter variable name = counter start ; counter variable name &lt counter stop ; counter variable name += step size)
     * <br>With again the same 7 tags as <b>&lt;test&gt;</b>
     *   <ul><li><b>&lt;function&gt;</b>
     *       <li><b>&lt;loop&gt;</b>
     *       <li><b>&lt;conditional&gt;</b>
     *       <li><b>&lt;while&gt;</b>
     *       <li><b>&lt;configuration&gt;</b>
     *       <li><b>&lt;subtest&gt;</b>
     *       <li><b>&lt;thread&gt;</b></ul>
     * the counter variable name can be accessed in a <b>&lt;function&gt;</b> tag with the <b>&lt;variable&gt;</b> or <b>&lt;localvariable&gt;</b> tag
     * <li><b>&lt;subtest name="subtestname" file="filename.xml"&gt;</b><b>&lt;/subtest&gt;</b> used to load a test that is run as if the subtags of the <b>&lt;test&gt;</b> tag where copy/pasted into the current test.
     *        Any localvariables get copied to the subtest. Any parameters added to the subtest tag get added as localvariables with 
     *        name "subtestname/parameternumber" and "subtestfilename/parameternumber" starting with 0. localvariable "subtestname" 
     *        and "subtestfilename" contain the ammount of parameters. The path of the subtests filename is relative to the current test.
     * <li><b>&lt;thread name="threadname"&gt;</b> used to run commands in the background. the test automatically waits on all threads to finish on the end
     * <BR>or use {@link FunctionModule_Basic#waitOnThreads FunctionModule_Basic#waitOnThreads} during the test.
     *        Any localvariables get copied to the thread. Be carefull with normal variables as they are globally defined and there could be
     *        concurrent modification problems. The threadname can be the name of a localvariable or variable, in that case it's value will be taken.
     * <br>Works with again the same 7 tags as <b>&lt;test&gt;</b>
     *   <ul><li><b>&lt;function&gt;</b>
     *       <li><b>&lt;loop&gt;</b>
     *       <li><b>&lt;conditional&gt;</b>
     *       <li><b>&lt;while&gt;</b>
     *       <li><b>&lt;configuration&gt;</b>
     *       <li><b>&lt;subtest&gt;</b>
     *       <li><b>&lt;thread&gt;</b></ul>
     * <li><b>&lt;conditional</b> variable="some/variable" targetvalue="stufftocheck" inverted="false"<b>&gt;</b>
     *   Conditional(While) can be used as an IF statement. It can take 3 attributes as parameters and have any subtags as the &lt;test&gt; tag can have.<br>
     *   The allowed attributes for the source are configuration, variable and localvariable. Only one of those is allowed to be specified at a time.<br>
     *   The allowed atributes for the target can be targetvalue and targetvariable. Only one of those is allowed to be specified at a time.<br>
     *   The condition executed will be source==target. If the condition evaluates to true its subtags will be executed. A nonexistent variable or configuration<br>
     *   value will be interpreted as the text "null" which can be used as the targetvalue to check for nonexistant values.<br>
     *   The attribute inveted with the value "true" can be used to create a source!=target condition.<br>
     *   The difference between conditonal and while is that the while is executed not only once but until the condition evaluates to false.
     * <br>Works with again the same 7 tags as <b>&lt;test&gt;</b>
     *   <ul><li><b>&lt;function&gt;</b>
     *       <li><b>&lt;loop&gt;</b>
     *       <li><b>&lt;conditional&gt;</b>
     *       <li><b>&lt;while&gt;</b>
     *       <li><b>&lt;configuration&gt;</b>
     *       <li><b>&lt;subtest&gt;</b>
     *       <li><b>&lt;thread&gt;</b></ul>
     * </ul>
     *
     * @param test  the test xml document
     * @see         <A HREF="http://jdom.org/docs/apidocs/index.html">JDOM</A>
     * @see         XTTProperties
     * @see         FunctionModule_Basic
     */
    public void runTest(org.jdom.Document test,boolean isTestLauncher,boolean isRunAll)
    {
        runTest(test,true,isTestLauncher,isRunAll);
    }
    private void runTest(org.jdom.Document test, boolean doInit,boolean isTestLauncher,boolean isRunAll)
    {
        abortTestExecution=false;
        try
        {
            threadList=new Vector<Thread>();
            XTTProperties.printDebug(this.getClass().getName()+".runTest(): "+test);
            if(doInit)
            {
                if(doInit())return;
            }
            Element root = test.getRootElement();
            if(root.getName().toLowerCase().equals("test"))
            {

                HashMap<String,String> localVars=new HashMap<String,String>();
                //
                localVars.put("currenttest/testpath",XTTProperties.getCurrentTestPath());
                parseElement(root, localVars,isTestLauncher,isRunAll);
            } else
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): root node is not called test");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
            }
            waitOnThreads();
        } finally
        {
            if(abortTestExecution)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): test execution aborted");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
            }
            abortTestExecution=false;
        }
        System.gc();
    }
    public boolean doInit()
    {
        FunctionModule obj=null;
        try
        {
            FunctionModule module=null;
            Iterator<FunctionModule> it=functionModules.iterator();
            // While we have functionModules re-initialize them

            while(it.hasNext())
            {
                obj=it.next();
                module=obj;
                module.initialize();
            }
            return false;
        } catch (ClassCastException cce)
        {
            XTTProperties.printFail(this.getClass().getName()+" "+cce.getClass().getName()+": "+obj.getClass().getName()+" not extending FunctionModule");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
            return true;
        }
    }
    /**
     * Same as runTest but with the exception that there will be no initialization of the modules which means
     * connections stay open, variables keep their values etc.
     *
     * @param test  the test xml document
     */
    public void runTestNI(org.jdom.Document test,boolean isTestLauncher,boolean isRunAll)
    {
          runTest(test,isTestLauncher,isRunAll);
    }
    public boolean parseElement(Element element, HashMap<String,String> localVars,boolean isTestLauncher,Boolean isRunAll)
    {
        FunctionModule module=null;
        List<?> functions = element.getChildren();
        List<?> functionparameters = null;
        String parameters[]=null;
        // Iterator over the Functions
        Iterator<?> itF = functions.iterator();
        // the current function
        Element function = null;
        // the current parameter
        Element parameter= null;
        String functionName=null;
        String moduleName=null;
		String stepName = "";
		String stepId ="";
		String threadName = "";
		AssetNode runningNode = null;
		Enumeration<?> e = null;
		int status = XTTProperties.NOT_RUNNING;
		
		if (isTestLauncher)
		{
			refreshRunningNode();
			e = (Enumeration<?>) TreeTestController.test.getTestNode().children();
		}
		
    	withNextFunction: while (itF.hasNext() && !abortTestExecution)
		{
			function = (Element) itF.next();
			if (function.getName().toLowerCase().equals("loop"))
			{
				HashMap<String, String> newMap = new HashMap<String, String>(localVars);
				if (doLoop(function, newMap,isTestLauncher))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("conditional"))
			{
				HashMap<String, String> newMap = new HashMap<String, String>(localVars);
				if (doConditional(function, newMap, false,isTestLauncher))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("while"))
			{
				HashMap<String, String> newMap = new HashMap<String, String>(localVars);
				if (doConditional(function, newMap, true,isTestLauncher))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("description"))
			{
				XTTProperties.printExternal("Description:\n" + function.getText());
			}
			else if (function.getName().toLowerCase().equals("div"))
			{
				String divcomment = getAttribute(function, "comment");
				if (divcomment != null)
				{
					XTTProperties.printExternal("Div:\n" + divcomment);
				}
				divcomment = getAttribute(function, "fail");
				if (divcomment != null)
				{
					XTTProperties.printFail("Div:\n" + divcomment);
				}
				divcomment = getAttribute(function, "warn");
				if (divcomment != null)
				{
					XTTProperties.printWarn("Div:\n" + divcomment);
				}
				divcomment = getAttribute(function, "info");
				if (divcomment != null)
				{
					XTTProperties.printInfo("Div:\n" + divcomment);
				}
				divcomment = getAttribute(function, "verbose");
				if (divcomment != null)
				{
					XTTProperties.printVerbose("Div:\n" + divcomment);
				}
				divcomment = getAttribute(function, "debug");
				if (divcomment != null)
				{
					XTTProperties.printDebug("Div:\n" + divcomment);
				}
				if (parseElement(function, localVars,false,isTestLauncher))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("name"))
			{
				XTTProperties.printVerbose("Parser: setting Name to: " + function.getText());
				// Actually set the Name
				XTTProperties.setCurrentTestName(function.getText());
			}
			else if (function.getName().toLowerCase().equals("thread"))
			{
				HashMap<String, String> newMap = new HashMap<String, String>(localVars);
				if (doThread(threadName,function, newMap,isTestLauncher))
				{
					return true;
				}
			}
			else if (function.getName().toLowerCase().equals("configuration"))
			{
				XTTProperties.printVerbose(this.getClass().getName() + ".runTest(): load Configuration: "
						+ function.getText());
				if (!XTTProperties.loadTempConfiguration(getCurrentTestFilePath(localVars) + function.getText()))
				{
					XTTProperties.setTestStatus(XTTProperties.FAILED);
					return true;
				}
			}
            else if(function.getName().toLowerCase().equals("subtest"))
            {
                //XTTProperties.printDebug(this.getClass().getName()+".runTest(): load subtest: "+function.getText());
                HashMap<String,String> newMap=new HashMap<String,String>(localVars);

                // Get the name attribute of the function
                String filename=functionName=getAttribute(function,"file");
                // if there is no name abort running of test
                if(filename==null)
                {
                    //XTTProperties.printWarn(this.getClass().getName()+".runTest(): deprecated subtest call found: "+function.getText());
                    XTTProperties.printDebug(this.getClass().getName()+".runTest(): load subtest: "+function.getText());
                    if(subTest(function.getText(),newMap,isTestLauncher))
                    {
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return true;
                    }
                } else
                {
                    // Get the module attribute of the function
                    String name=getAttribute(function,"name");
                    if(name==null)
                    {
                        XTTProperties.printFail("Parser.runTest: skipping Subtest name missing: File '"+filename+"'");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                        return true;
                    }
                    XTTProperties.printDebug(this.getClass().getName()+".runTest(): load subtest: '"+name+"' file: "+filename);
                    // Get all the subnodes of the function
                    functionparameters=function.getChildren();
                    // Create the array of parameters for the runFunction method of the function module
                    String subparam=null;
                    newMap.put(name.toLowerCase(),functionparameters.size()+"");
                    // for all the parameters in the subtest
                    for(int i=0;i<functionparameters.size();i++)
                    {
                        // get the parameter
                        parameter=(Element)functionparameters.get(i);
                        try
                        {
                            subparam=decodeParameter("Subtest '"+name+"'",localVars,parameter);
                            newMap.put(filename.toLowerCase()+"/"+i,subparam);
                            newMap.put(name.toLowerCase()+"/"+i,subparam);
                        } catch (Exception dpe)
                        {
                            XTTProperties.printFail("Parser.runTest: skipping Subtest '"+name+"' File '"+filename+"'");
                            if(!dpe.getMessage().equals(PARAMETER_ERROR))
                            {
                                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                            }
                            continue withNextFunction;
                        }
                    }
                    if(subTest(filename,newMap,isTestLauncher))
                    {
                        XTTProperties.setTestStatus(XTTProperties.FAILED);
                        return true;
                    }
                }
            } 
            else if(function.getName().toLowerCase().equals("function"))
            {
				if (isTestLauncher)
				{
					if (TreeTestController.test.getChildCount() > 0)
					{
						if (e.hasMoreElements())
						{
							TreeTestController.setXttLog(XTTProperties.getOutputStream().getStreamVector().get(1).toString());
							try
							{
								updateNode(runningNode, status,isTestLauncher);
								runningNode = (AssetNode)e.nextElement();

								if (isRunAll)
								{
									while (!runningNode.isSelected() && e.hasMoreElements())
										runningNode = (AssetNode)e.nextElement();
								}
								else
								{
									runningNode = TreeTestController.getTestLastPathComponent();
								}
									updateNode(runningNode, XTTProperties.RUNNING,isTestLauncher);
								
								Thread.sleep(300);
							}
							catch (InterruptedException e1)
							{
								// TODO Auto-generated catch block
								e1.printStackTrace();
							}
						}
					}
				}
                // Get the name attribute of the function
                functionName=getAttribute(function,"name");
                // if there is no name abort running of test
                if(functionName==null)
                {
                    XTTProperties.printFail(this.getClass().getName()+".runTest(): function attribute 'name' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                    return true;
                }
                // Get the module attribute of the function
                moduleName=getAttribute(function,"module");
                // if there is no module abort running of test
                if(moduleName==null)
                {
                    XTTProperties.printFail(this.getClass().getName()+".runTest(): module='name' not found on function "+functionName);
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                    return true;
                }
                // Get all the subnodes of the function
                functionparameters=function.getChildren();
                // Create the array of parameters for the runFunction method of the function module
                parameters=new String[functionparameters.size()+1];
                // first parameter is always the function name
                parameters[0]=functionName;
                // Get the iterator over all the parameters
                // itP=functionparameters.iterator();
                // for all the parameters in the function
                for(int i=0;i<functionparameters.size();i++)
                {
                    // get the parameter
                    parameter=(Element)functionparameters.get(i);
                    try
                    {
                        parameters[i+1]=decodeParameter(functionName,localVars,parameter);
                    } 
                    catch (Exception dpe)
                    {
                        XTTProperties.printFail("Parser.runTest: skipping Function '"+functionName+"' in Module '"+moduleName+"'");
                        status=  XTTProperties.FAILED_WITH_PARSER_ERROR;
                        //dpe.printStackTrace();
                        if(!dpe.getMessage().equals(PARAMETER_ERROR))
                        {
                            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                        }
                        continue withNextFunction;
                    }
                }
                module=functionModuleMap.get(moduleName.toLowerCase());
                if(module==null)
                {
                    XTTProperties.printFail("Parser.runTest: FunctionModule '"+moduleName+"' not found");
                    XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                    return true;
                } else if(moduleName.equalsIgnoreCase("basic")&&functionName.equalsIgnoreCase("setlocalvariable"))
                {
                    if(parameters.length!=3)
                    {
                        XTTProperties.printFail(parameters[0]+":"+FunctionModule.MISSING_ARGUMENTS+": variableName value");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
                    } else
                    {
                        XTTProperties.printInfo(parameters[0]+": "+parameters[1]+"="+parameters[2]);
                        localVars.put(parameters[1].toLowerCase(),parameters[2]);
                    }
                } 
                else
                {	
                	String description = "";
                	if (!stepName.equals(""))
                	{
                		if (!stepId.equals(""))
                			description ="Parser.runTest: [Step (" + stepId+") " + stepName +"] FunctionModule '"+moduleName+"' found: "+functionName;
                		else
                			description ="Parser.runTest: [Step " + stepName +" ] FunctionModule '"+moduleName+"' found: "+functionName;	
                	}
                	else
                	{
                		description ="Parser.runTest: FunctionModule " + moduleName+"' found: "+functionName;
					}
                	XTTProperties.printExternal(description + ": [Start]");
                	//Run the function
					status = module.runFunction(parameters);
					
					if (status != XTTProperties.FAILED_FUNCTION_NOT_EXIST && status != XTTProperties.FAILED_UNKNOWN)
					{
						XTTProperties.printExternal(description + ": [Stop]");
						
						if (status==XTTProperties.PASSED && isTestLauncher)
						{
							if (runningNode.isMandatory())
							{
							XTTProperties.printExternal(description + ": [Mandatory]");
							XTTProperties.printFail("Parser.runTest: running test STOP!");
							updateNode(runningNode,status,isTestLauncher);
							return true;
							}
						}
					}
                    else if(status==XTTProperties.FAILED_FUNCTION_NOT_EXIST || status == XTTProperties.FAILED_UNKNOWN)
                    {
                    	XTTProperties.printFail("Parser.runTest: function '"+functionName+"' not found/error in module '"+moduleName+"'");
                        XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                        updateNode(runningNode,status,isTestLauncher);
                        return true;
                    }
                }
            } 
            else if (function.getName().toLowerCase().equals("stepname"))
            {
            	stepName = function.getText();
            }
            else if (function.getName().toLowerCase().equals("stepid"))
            {
            	stepId = function.getText();
            }
            else if (function.getName().toLowerCase().equals("threadname"))
            {
            	threadName = function.getText();
            }
            else if(function.getName().toLowerCase().equals("steps")||
            		function.getName().toLowerCase().equals("mandatory") ||
            		function.getName().toLowerCase().equals("qcid") || 
            		function.getName().toLowerCase().equals("designer")|| 
            		function.getName().toLowerCase().equals("creationdate") ||
            		function.getName().toLowerCase().equals("testlevel")|| 
            		function.getName().toLowerCase().equals("xfwversion"))
            {
            	//Do nothing
            }
            else
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): <"+function.getName()+"> is not an allowed tag");
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                return true;
            }
            
        }
			updateNode(runningNode,status,isTestLauncher);
        return false;
    }
    public void refreshRunningNode()
	{
			TreeTestController.test.getTestNode().setStatus(XTTProperties.NOT_RUNNING);
			TreeTestController.test.updateTestNode();
			Enumeration<?> e = (Enumeration<?>) TreeTestController.test.getTestNode().children();

			if (TreeTestController.test.getChildCount() > 0)
			{
				while (e.hasMoreElements())
				{
					AssetNode node = (AssetNode) e.nextElement();
					node.setStatus(XTTProperties.NOT_RUNNING);
					TreeTestController.test.updateNode(node);
				}
			}
	}
	public void updateNode(AssetNode runningNode, int stepStatus,boolean isTestLauncher)
	{
		if (isTestLauncher)
		{
				if (TreeTestController.test.getChildCount() > 0)
				{
					if (runningNode != null)
					{
						
						if (stepStatus ==XTTProperties.FAILED || stepStatus > XTTProperties.RUNNING)
						{
							if (((AssetNode)runningNode.getRoot()).getStatus()<stepStatus || ((AssetNode)runningNode.getRoot()).getStatus()==2 && stepStatus==1)
							{
							((AssetNode)runningNode.getRoot()).setStatus(stepStatus);
							TreeTestController.test.updateTestNode();
							}
						}
						if (stepStatus ==XTTProperties.PASSED && 
						   ((AssetNode)runningNode.getRoot()).getStatus()<=XTTProperties.RUNNING && 
						   ((AssetNode)runningNode.getRoot()).getStatus()!=XTTProperties.FAILED)
						{
							((AssetNode)runningNode.getRoot()).setStatus(stepStatus);
							TreeTestController.test.updateTestNode();
						}
						runningNode.setStatus(stepStatus);
						TreeTestController.test.updateNode(runningNode);
				}
			}
		}
	}
    
    private String getCurrentTestFilePath(HashMap<String,String> localVars)
    {
        return localVars.get("currenttest/testpath");
    }
    private String decodeParameter(String functionName,HashMap<String,String> localVars,Element parameter) throws Exception
    {
        String returnParameter=null;
        String parameterText=parameter.getText();

        if(parameter.getName().toLowerCase().equals("xml"))
        {
            try
            {
                if(parameter.getChildren().isEmpty())throw new Exception("No XML content in xml tag");
                if(parameter.getChildren().size()>1)throw new Exception("Only 1 direct child for xml tag allowed (used as root node)");
                Element e=(Element)((Element)parameter.getChildren().get(0)).clone();
                e.detach();
                return XTTXML.stringXML(new Document(e));
            } catch (Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": xml error "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(ex);
                }
                throw ex;
            }
        }

        StringBuffer concat=new StringBuffer("");
        List <?>subparameters=parameter.getChildren();
        if(subparameters.size()>0)
        {
            Element subparameter=null;
            for(int i=0;i<subparameters.size();i++)
            {
                // get the parameter
                subparameter=(Element)subparameters.get(i);
                concat.append(decodeParameter(functionName+"."+parameter,localVars,subparameter));
            }
            parameterText=concat.toString();
        }

        if(parameter.getName().toLowerCase().equals("configuration"))
        {
            returnParameter=XTTProperties.getProperty(parameterText);
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": configuration '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("variable"))
        {
            returnParameter=XTTProperties.getVariable(parameterText);
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": variable '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("variablepointer"))
        {
            String varpointer=XTTProperties.getVariable(parameterText).toLowerCase();
            if(varpointer==null||varpointer.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": variable '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
            returnParameter=XTTProperties.getVariable(varpointer);
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": variable '"+parameterText+"'->'"+varpointer+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("localvariable"))
        {
            returnParameter=localVars.get(parameterText.toLowerCase());
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": local variable '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("localvariablepointer"))
        {
            String varpointer=localVars.get(parameterText.toLowerCase()).toString();
            if(varpointer==null||varpointer.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": local variable '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
            returnParameter=XTTProperties.getVariable(varpointer);
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": variable '"+parameterText+"'->'"+varpointer+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("locallocalvariablepointer"))
        {
            String varpointer=localVars.get(parameterText.toLowerCase()).toString();
            if(varpointer==null||varpointer.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": local variable '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
            returnParameter=localVars.get(varpointer.toLowerCase()).toString();
            if(returnParameter==null||returnParameter.equals("null"))
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": local variable '"+parameterText+"'->'"+varpointer+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("file"))
        {
            returnParameter=loadFile(parameterText,localVars);
            if(returnParameter==null)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": file '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("base64file"))
        {
            returnParameter=loadBase64File(parameterText,localVars);
            if(returnParameter==null)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": file '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("base64encode"))
        {
            try
            {
                returnParameter=ConvertLib.base64Encode(parameterText);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64encode error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64encode parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("base64decode"))
        {
            try
            {
                returnParameter=ConvertLib.createString(ConvertLib.base64Decode(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64decode error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64decode parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("bytestringfile"))
        {
            returnParameter=loadByteStringFile(parameterText,localVars);
            if(returnParameter==null)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": file '"+parameterText+"' not found");
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                throw new Exception(PARAMETER_ERROR);
            }
        } else if(parameter.getName().toLowerCase().equals("bytestringencode"))
        {
            try
            {
                byte[] bytes=ConvertLib.createBytes(parameterText);
                returnParameter=ConvertLib.outputBytes(bytes,0,bytes.length);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringencode error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringencode parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("bytestringdecode"))
        {
            try
            {
                returnParameter=ConvertLib.createString(ConvertLib.getByteArrayFromHexString(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringdecode error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringdecode parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("bytestringtobase64"))
        {
            try
            {
                returnParameter=ConvertLib.base64Encode(ConvertLib.getByteArrayFromHexString(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringtobase64 error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringtobase64 parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("base64tobytestring"))
        {
            try
            {
                byte[] bytes=ConvertLib.base64Decode(parameterText);
                returnParameter=ConvertLib.outputBytes(bytes,0,bytes.length);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64tobytestring error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64tobytestring parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("base64togsm7bit"))
        {
            try
            {
                byte[] bytes=ConvertLib.base64Decode(parameterText);
                returnParameter=ConvertLib.getStringFromGSM7bitAlphabet(bytes);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64togsm7bit error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": base64togsm7bit parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("bytestringtogsm7bit"))
        {
            try
            {
                returnParameter=ConvertLib.getStringFromGSM7bitAlphabet(ConvertLib.getByteArrayFromHexString(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringtogsm7bit error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": bytestringtogsm7bit parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("byteintencode"))
        {
            try
            {
                byte[] bytes=ConvertLib.getByteArrayFromInt(Integer.decode(parameterText));
                returnParameter=ConvertLib.outputBytes(bytes,0,bytes.length);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": byteintencode error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": byteintencode parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("inttohex"))
        {
            try
            {
                returnParameter=ConvertLib.longToHex(Long.decode(parameterText));
                if(returnParameter.length()%2==1)returnParameter="0"+returnParameter;
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": inttohex error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": inttohex parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("testpath"))
        {
            returnParameter=getCurrentTestFilePath(localVars)+parameterText;
        } else if(parameter.getName().toLowerCase().equals("bytelength"))
        {
            returnParameter=""+(ConvertLib.createBytes(parameterText).length);
        } else if(parameter.getName().toLowerCase().equals("base64bytelength"))
        {
            returnParameter=""+(ConvertLib.base64Decode(parameterText).length);
        } else if(parameter.getName().toLowerCase().equals("stringlength"))
        {
            returnParameter=""+parameterText.length();
        } else if(parameter.getName().toLowerCase().equals("randomint"))
        {
            try
            {
                returnParameter=""+RandomLib.getRandomInt(Integer.decode(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": randomint error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": randomint parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("randombytes"))
        {
            try
            {
                byte[] bytes=RandomLib.getRandomBytes(Integer.decode(parameterText));
                returnParameter=ConvertLib.outputBytes(bytes,0,bytes.length);
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": randombytes error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": randombytes parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("randomdigits"))
        {
            try
            {
                returnParameter=""+RandomLib.getRandomDigits(Integer.decode(parameterText));
            } catch(Exception ex)
            {
                XTTProperties.printFail(this.getClass().getName()+".runTest(): function "
                    +functionName+": randomint error: "+ex.getMessage());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                XTTProperties.printDebug(this.getClass().getName()+".runTest(): function "
                    +functionName+": randomint parameter was:\n"+parameterText);
                XTTProperties.printDebugException(ex);
                throw ex;
            }
        } else if(parameter.getName().toLowerCase().equals("crlf"))
        {
            returnParameter=parameterText+"\r\n";
        } else if(parameter.getName().toLowerCase().equals("parameter"))
        {
            returnParameter=parameterText;
        } else
        {
            XTTProperties.printFail(this.getClass().getName()+".runTest(): <"+parameter.getName()+"> is not an allowed tag");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
            throw new Exception(PARAMETER_ERROR);
        }
        return returnParameter;
    }

    public static final Vector<String> getParameterList()
    {
        Vector<String> v=new Vector<String>();
        v.add("parameter");
        v.add("configuration");
        v.add("variable");
        v.add("variablepointer");
        v.add("file");
        v.add("xml");
        v.add("base64file");
        v.add("bytestringfile");
        v.add("base64encode");
        v.add("base64decode");
        v.add("bytestringencode");
        v.add("bytestringdecode");
        v.add("bytestringtobase64");
        v.add("base64tobytestring");
        v.add("byteintencode");
        v.add("bytelength");
        v.add("inttohex");
        v.add("base64bytelength");
        v.add("stringlength");
        v.add("randomint");
        v.add("randombytes");
        v.add("randomdigits");
        v.add("base64togsm7bit");
        v.add("bytestringtogsm7bit");
        v.add("crlf");
        return v;
    }


    /**
     * Depredacted: run one single function.
     */
    public String runSingleFunction(String moduleName, String functionName, String[] parameters)
    {
        FunctionModule module=null;
                module=functionModuleMap.get(moduleName.toLowerCase());
                if(module==null)
                {
                    return "Parser.runTest: FunctionModule '"+moduleName+"' not found";
                } else
                {
                    XTTProperties.printDebug("Parser.runTest: FunctionModule '"+moduleName+"' found: START");
                  //Run the function
					int status = module.runFunction(parameters);
					if (status != XTTProperties.FAILED_FUNCTION_NOT_EXIST && status != XTTProperties.FAILED_UNKNOWN)
                    {
                        XTTProperties.printDebug("Parser.runTest: FunctionModule '"+moduleName+"': "+functionName+": STOP");
                    }
                }
                return "Parser.runTest: function '"
                        +functionName+"' not found in module '"+moduleName+"'";
    }


    private String loadFile(String fileName,HashMap<String,String> localVars)
    {
        byte [] bdata=loadBinFile(fileName,"loadFile",localVars);
        if(bdata!=null)
        {
            return ConvertLib.createString(bdata);
        } else
        {
            return null;
        }
    }
    private byte[] loadBinFile(String fileName,String funcname, HashMap<String,String> localVars)
    {
        return loadBinFile(getCurrentTestFilePath(localVars), fileName, funcname, localVars);
    }
    private byte[] loadBinFile(String path, String fileName,String funcname, HashMap<String,String> localVars)
    {
        FileInputStream stream=null;
        try
        {
            File theFile=new File(path+fileName);
            int fileLength=Integer.parseInt(theFile.length()+"");
            byte fileContent[]=new byte[fileLength];

            stream=new FileInputStream(theFile);
            if(stream.available()>0)
            {
                HTTPHelper.readBytes(stream,fileContent);
            } else
            {
                return null;
            }
            return fileContent;
        } catch (Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printDebug(this.getClass().getName()+"."+funcname+"("+path+fileName+"): "+e.getClass().getName());
                XTTProperties.printException(e);
            }
            return null;
        } finally
        {
            try
            {
                stream.close();
            } catch (Exception ex){}

        }
    }
    private String loadBase64File(String fileName,HashMap<String,String> localVars)
    {
        byte[] file=loadBinFile(fileName,"loadBase64File",localVars);
        if(file==null)return null;
        return ConvertLib.base64Encode(file);
    }
    private String loadByteStringFile(String fileName,HashMap<String,String> localVars)
    {
        byte[] file=loadBinFile(fileName,"loadByteStringFile",localVars);
        if(file==null)return null;
        return ConvertLib.outputBytes(file);
    }
    private boolean subTest(String fileName, HashMap<String,String> localVars,boolean isTestLauncher)
    {
    	Document subtest;
    	subtest=XTTXML.readXML(System.getProperty("user.dir")+ "/tests/XMP/subtests/" +fileName);
        if(subtest==null)return true;
        Element root = subtest.getRootElement();
        if(root.getName().toLowerCase().equals("test"))
        {
			localVars.put("currenttest/testpath", getCurrentTestFilePath(localVars));
			boolean result = parseElement(root, localVars, true,isTestLauncher);
			XTTProperties.printDebug(this.getClass().getName() + ".runTest(): unloaded subtest: " + fileName);
			return result;
		}
		else
		{
			XTTProperties.printFail(this.getClass().getName() + ".subTest(): root node is not called test in "
					+ getCurrentTestFilePath(localVars) + fileName);
			XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_PARSER_ERROR);
			return true;
		}
    }
    public static String getAttribute(Element element,String name)
    {
        Iterator<?> it=element.getAttributes().iterator();
        Attribute attribute=null;
        while(it.hasNext())
        {
            attribute=(Attribute)it.next();
            if(attribute.getName().toLowerCase().equals(name))
            {
                return attribute.getValue();
            }
        }
        return null;
    }

    private boolean doThread(String threadName,Element element, HashMap<String,String> localVars,boolean isTestLauncher)
    {
        XTTProperties.printExternal(this.getClass().getName()+" ["+threadName+"].doThread(): start");
        Thread t=null;
        String name=getAttribute(element,"name");
        
        if(name==null)
        {
            t=new Thread(new DoThread(element,localVars,isTestLauncher));
            if (!threadName.equals(""))
            	t.setName(threadName);
        } else
        {
            String usedName=name;
            if (localVars.get(usedName.toLowerCase())!=null)
            {
                usedName=localVars.get(name.toLowerCase());
            } else if(!XTTProperties.getVariable(name).equals("null"))
            {
                usedName=XTTProperties.getVariable(name);//.replaceAll("[^0-9a-zA-Z/]",""));
            }
            t=new Thread(new DoThread(element,localVars,isTestLauncher),usedName);
        }
        threadList.add(t);
        t.start();
        XTTProperties.printExternal(this.getClass().getName()+" ["+threadName+"].doThread(): stop");
        return false;
    }

    private class DoThread implements Runnable 
    {
        Element element=null;
        HashMap<String,String> localVars=null;
        boolean isTestLauncher=false;
        public DoThread(Element element, HashMap<String,String> localVars,boolean isTestLauncher)
        {
            XTTProperties.printDebug(this.getClass().getName()+" constructor");
            this.element=element;
            this.localVars=localVars;
            this.isTestLauncher=isTestLauncher;
        }
        public void run()
        {
            XTTProperties.printDebug(this.getClass().getName()+".run(): start");
            parseElement(element,localVars,false,isTestLauncher);
            XTTProperties.printDebug(this.getClass().getName()+".run(): stop");
        }
    }
    /**
     * Wait for all threads created by &lt;thread&gt; to finish. Is called automatically at the end of the test.
     *
     * @see     FunctionModule_Basic#waitOnThreads
     */
    public static void waitOnThreads()
    {
        waitOnThreads(0);
    }
    public static void waitOnThreads(int numThreads)
    {
        XTTProperties.printDebug("Parser.waitOnThreads(): start");
        long activethreads=hasActiveThread(threadList);
        long lastactivethreads=0;
        if(activethreads<=numThreads)
        {
            XTTProperties.printDebug("Parser.waitOnThreads(): active threads:"+activethreads);
        }
        while(activethreads>numThreads)
        {
            if(lastactivethreads!=activethreads)
            {
                XTTProperties.printDebug("Parser.waitOnThreads(): active threads:"+activethreads);
            }
            try
            {
                Thread.sleep(100);
            } catch (Exception e)
            {
                XTTProperties.printFail("Parser.waitOnThreads(): "+e.getClass().getName());
                XTTProperties.setTestStatus(XTTProperties.FAILED);
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                    //e.printStackTrace();
                }
            }
            lastactivethreads=activethreads;
            activethreads=hasActiveThread(threadList);
        }
        XTTProperties.printDebug("Parser.waitOnThreads(): stop");
    }

    private static long hasActiveThread(Vector<Thread> v)
    {
        long numthreads=0;
        Thread t=null;
        int x=0;
        try
        {
            while(x<v.size())
            {
                t=v.elementAt(x);
                if(t.isAlive())
                {
                    numthreads++;
                    // only increase the counter when we found an active thread since the inactive get removed
                    x++;
                } else
                {
                    // This shouldn't pose a problem since we are counting the
                    // number of active threads in the vector and catch an out of bounds exception
                    v.remove(t);
                }
            }
        } catch(ArrayIndexOutOfBoundsException e){}
        return numthreads;
    }


    private boolean doLoop(Element loop, HashMap<String,String> localVars,boolean isTestLauncher)
    {
        String variable=getAttribute(loop,"name");
        if(variable==null)
        {
            XTTProperties.printFail(this.getClass().getName()+".runTest(): LOOP name not found");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return true;
        }
        variable = variable.replaceAll("[^0-9a-zA-Z/]","");
        String temp=null;
        long stop=0;
        long start=0;
        long step=1;
        String useStopVariable=null;
        String useStartVariable=null;
        String useStepVariable=null;
        try
        {
            temp=getAttribute(loop,"stop");
            stop=(Long.decode(temp)).longValue();
        } catch (Exception e1)
        {
            try
            {
                stop=Long.decode(localVars.get(temp.toLowerCase()).toString()).longValue();
            } catch (Exception e2)
            {
                try
                {
                    stop=(Long.decode(XTTProperties.getVariable(temp))).longValue();
                    useStopVariable=temp;
                } catch (Exception e3)
                {

                    XTTProperties.printFail(this.getClass().getName()+".runTest(): LOOP attribute 'stop="+temp+"' is neither a number nor a numeric variable");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return true;
                }
            }
        }

        try
        {
            temp=getAttribute(loop,"start");
            start=(Long.decode(temp)).longValue();
        } catch (Exception e1)
        {
            try
            {
                start=Long.decode(localVars.get(temp.toLowerCase()).toString()).longValue();
            } catch (Exception e2)
            {
                try
                {
                    start=(Long.decode(XTTProperties.getVariable(temp))).longValue();
                    useStartVariable=temp;
                } catch (Exception e3)
                {

                    XTTProperties.printFail(this.getClass().getName()+".runTest(): LOOP attribute 'start="+temp+"' is neither a number nor a numeric variable");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return true;
                }
            }
        }

        try
        {
            temp=getAttribute(loop,"step");
            if(temp==null)
            {
                step=1;
            } else
            {
                step=(Long.decode(temp)).longValue();
            }
        } catch (Exception e1)
        {
            try
            {
                step=Long.decode(localVars.get(temp.toLowerCase()).toString()).longValue();
            } catch (Exception e2)
            {
                try
                {
                    step=(Long.decode(XTTProperties.getVariable(temp))).longValue();
                    useStepVariable=temp;
                } catch (Exception e3)
                {

                    XTTProperties.printFail(this.getClass().getName()+".runTest(): LOOP attribute 'step="+temp+"' is neither a number nor a numeric variable");
                    XTTProperties.setTestStatus(XTTProperties.FAILED);
                    return true;
                }
            }
        }

        if(step==0)
        {
            XTTProperties.printFail(this.getClass().getName()+".runTest(): LOOP attribute 'step' can not be 0");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_INVALID_ARGUMENTS);
            return true;
        }

        if(step>0&&start>=stop)
        {
            XTTProperties.printWarn(this.getClass().getName()+".runTest(): SKIPPING LOOP with name='"+variable+"' step="+step
                +": start="+start+" is greater or equal stop="+stop);
            return false;
        }
        if(step<0&&start<=stop)
        {
            XTTProperties.printWarn(this.getClass().getName()+".runTest(): SKIPPING LOOP with name='"+variable+"' step="+step
                +": start="+start+" is less or equal stop="+stop);
            return false;
        }

        long current=start;
        long last=0;
        //moduleName=getAttribute(function,"module");
        while((step>0&&current<stop)||(step<0&&current>stop))
        {
            XTTProperties.printInfo("loop: "+variable+"="+current+" start="+start+" stop="+stop+" step="+step);
            XTTProperties.setVariable(variable,current+"");
            localVars.put(variable.toLowerCase(),current+"");
            if(parseElement(loop,localVars,false,isTestLauncher))return true;
            current=current+step;
            if(useStopVariable!=null)
            {
                last=stop;
                stop=(Long.decode(XTTProperties.getVariable(useStopVariable))).longValue();
                if(last!=stop)
                {
                    XTTProperties.printInfo("loop: reset: "+variable+": changing stop to "+stop);
                }
            }
            if(useStepVariable!=null)
            {
                last=step;
                step=(Long.decode(XTTProperties.getVariable(useStepVariable))).longValue();
                if(last!=step)
                {
                    XTTProperties.printInfo("loop: reset: "+variable+": changing step to "+step);
                }
            }
            if(useStartVariable!=null)
            {
                last=start;
                start=(Long.decode(XTTProperties.getVariable(useStartVariable))).longValue();
                if(last!=start)
                {
                    current=start;
                    XTTProperties.printInfo("loop: reset: "+variable+": changing current and start to "+start);
                }
            }
        }
        return false;
    }

    /**
     *   Conditional(While) can be used as an IF statement. It can take 3 attributes as parameters and have any subtags as the <Test> tag can have.
     *   The allowed attributes for the source are configuration, variable and localvariable. Only one of those is allowed to be specified at a time.
     *   The allowed atributes for the target can be targetvalue and targetvariable. Only one of those is allowed to be specified at a time.
     *   The condition executed will be source==target. If the condition evaluates to true its subtags will be executed. A nonexistent variable or configuration
     *   value will be interpreted as the text "null" which can be used as the targetvalue to check for nonexistant values.
     *   The attribute inveted with the value "true" can be sued to create a source!=target condition. The difference between conditonal and while is that the while is
     *   executed not only once but until the condition evaluates to false.
     *
     */
    private boolean doConditional(Element condition, HashMap<String,String> localVars, boolean isWhile,boolean isTestLauncher)
    {
        String type="Conditional";
        if(isWhile)type="While";
        String confVal   =getAttribute(condition,"configuration");
        String varVal    =getAttribute(condition,"variable");
        String locvarVal =getAttribute(condition,"localvariable");
        String targetVal =getAttribute(condition,"targetvalue");
        String targetVar =getAttribute(condition,"targetvariable");
        String inverted=getAttribute(condition,"inverted");
        if((confVal!=null&&varVal!=null&&locvarVal!=null)||(confVal==null&&varVal==null&&locvarVal==null))
        {
            XTTProperties.printFail(this.getClass().getName()+".do"+type+"(): please specify either configuration, variable or localvariable");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return true;
        } else if((targetVal!=null&&targetVar!=null)||(targetVal==null&&targetVar==null))
        {
            XTTProperties.printFail(this.getClass().getName()+".do"+type+"(): please specify either targetValue or targetVariable");
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return true;
        }
        String usedSource = getConditionalSource(confVal, varVal, locvarVal, localVars);
        String usedTarget = getConditionalTarget(targetVal, targetVar);

        boolean isInverted=ConvertLib.textToBoolean(inverted);

        XTTProperties.printDebug(this.getClass().getName()+".do"+type+"(): source='"+usedSource+"' target='"+usedTarget+"' inverted='"+isInverted+"'");

        if(!isInverted)
        {
            if(!isWhile)
            {
                if(usedSource.equals(usedTarget))
                {
                    if(parseElement(condition,localVars,false,isTestLauncher))return true;
                }
            } else
            {
                while(usedSource.equals(usedTarget))
                {
                    if(parseElement(condition,localVars,false,isTestLauncher))return true;
                    usedSource = getConditionalSource(confVal, varVal, locvarVal, localVars);
                    usedTarget = getConditionalTarget(targetVal, targetVar);
                }
            }
        } else
        {
            if(!isWhile)
            {
                if(!usedSource.equals(usedTarget))
                {
                    if(parseElement(condition,localVars,false,isTestLauncher))return true;
                }
            } else
            {
                while(!usedSource.equals(usedTarget))
                {
                    if(parseElement(condition,localVars,false,isTestLauncher))return true;
                    usedSource = getConditionalSource(confVal, varVal, locvarVal, localVars);
                    usedTarget = getConditionalTarget(targetVal, targetVar);
                }
            }
        }
        return false;
    }
    
    private String getConditionalSource(String confVal, String varVal, String locvarVal, HashMap<String,String> localVars)
    {
        String usedSource = "";
        if(confVal!=null)
        {
            usedSource=XTTProperties.getProperty(confVal.replaceAll("[^0-9a-zA-Z/]",""));
        } else if (locvarVal!=null)
        {
            usedSource=localVars.get(locvarVal.toLowerCase());
        } else
        {
            usedSource=XTTProperties.getVariable(varVal);//.replaceAll("[^0-9a-zA-Z/]",""));
        }
        if(usedSource==null)usedSource="null";
        return usedSource;
    }
    private String getConditionalTarget(String targetVal, String targetVar)
    {
        String usedTarget = "";
        if(targetVal!=null)
        {
            usedTarget=targetVal;
        } else
        {
            usedTarget=XTTProperties.getVariable(targetVar);//.replaceAll("[^0-9a-zA-Z/]",""));
        }
        if(usedTarget==null)usedTarget="null";
        return usedTarget;
    }

    public String getConfigurationOptions()
    {
        StringBuffer options=new StringBuffer("");
        options.append("<configuration>\n");
        options.append(XTTProperties.getConfigurationOptions());
        options.append("\n"+XTTGui.getConfigurationOptions());
        FunctionModule func=null;
        Iterator<FunctionModule> it=functionModules.iterator();
        // While we have functionModules
        String option=null;
        while(it.hasNext())
        {
            func=it.next();
            option=func.getConfigurationOptions();
            if(option!=null&&!option.equals(""))
            {
                options.append("\n"+option);
            }
        }
        options.append("\n</configuration>\n");
        return options.toString();
    }

    public void dumpFunctions()
    {
        FunctionModule func=null;
        Iterator<FunctionModule> it=functionModules.iterator();
        // While we have functionModules
        while(it.hasNext())
        {
            func=it.next();
            func.dumpFunctions();
        }
    }

    public void showVersions()
    {
        FunctionModule func=null;
        Iterator<FunctionModule> it=functionModules.iterator();
        // While we have functionModules
        while(it.hasNext())
        {
            func=it.next();
            func.showVersions();
        }
    }

    public Vector<String> checkResources()
    {
        Vector<String> moduleResources = new Vector<String>();
        FunctionModule func=null;
        Iterator<FunctionModule> it=functionModules.iterator();

        if(XTT.isXTTOk())
        {
            moduleResources.add("XTT Version: "+XTTProperties.getXTTBuildVersion()+" "+FunctionModule.RESOURCE_OK);
        }
        else
        {
            moduleResources.add("XTT: Another XTT is already running");
        }

        // While we have functionModules
        while(it.hasNext())
        {
            func=it.next();
            moduleResources.add(func.checkResources());
        }
        return moduleResources;
    }
    public static final String tantau_sccsid = "@(#)$Id: Parser.java,v 1.69 2009/06/25 11:40:28 rsoder Exp $";
}
