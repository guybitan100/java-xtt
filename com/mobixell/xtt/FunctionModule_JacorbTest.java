package com.mobixell.xtt;

import com.mobixell.xtt.jacorbtest.TestServer;
import com.mobixell.xtt.jacorbtest.TestServerHelper;
import com.mobixell.xtt.jacorbtest.TestServerImpl;
import com.mobixell.xtt.jacorbtest.TestServerPOA;

import java.io.*;
import org.omg.CORBA.Any;
import org.omg.PortableServer.*;
import org.omg.CosNaming.*;


/**
 * FunctionModule_SIS.
 *
 * @author      Roger Soder
 * @version     $Revision: 1.6 $
 */
public class FunctionModule_JacorbTest extends AbstractFunctionModule_Jacorb
{
    public static final String tantau_sccsid = "@(#)$Id: FunctionModule_JacorbTest.java,v 1.6 2008/03/17 08:48:11 rsoder Exp $";

    private static org.omg.CORBA.ORB serverorb=null;

    public void initialize()
    {
        super.initialize();
        XTTProperties.printDebug(this.getClass().getName()+".initialize(): clearing variables");
    }

    /**
     * returns the getClass().getName() of this object. For debug reasons only.
     */
    public String toString()
    {
        return this.getClass().getName();
    }

    /**
     * Returns the Configuration Options as a String ready to copy/paste in a configuration file
     *
     */
    public String getConfigurationOptions()
    {
        return "    <!-- function module JacorbTest/SIS -->"
            +"\n    <jacorb>"
            +"\n        <naming>"
            +"\n            <iorfilename>tests/xmaSIS/NS_Ref</iorfilename>"
            +"\n            <timeout>0</timeout>"
            +"\n            <port>2055</port>"
            +"\n        </naming>"
            +"\n"
            +"\n        <properties>tests/xmaSIS/jacorb.properties</properties>"
            +"\n        <javaendorseddirs>./lib</javaendorseddirs>"
            +"\n        <home>.</home>"
            +"\n        <log>"
            +"\n            <verbosity>4</verbosity>"
            +"\n            <useXTTLogger/>"
            +"\n        </log>"
            +"\n    </jacorb>";
    }

    public void startJacorbTestServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startJacorbTestServer:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+MISSING_ARGUMENTS+":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": starting.");
            XTTProperties.printDebug(parameters[0] + ": starting now:");
            serverorb = org.omg.CORBA.ORB.init(new String[]{}, null);
            try
            {
                org.omg.PortableServer.POA poa =
                org.omg.PortableServer.POAHelper.narrow(serverorb.resolve_initial_references("RootPOA"));
                poa.the_POAManager().activate();
                org.omg.CORBA.Object o = poa.servant_to_reference(new TestServerImpl());

                if( parameters.length == 2 )
                {
                    PrintWriter ps = new PrintWriter(new FileOutputStream(new File( parameters[1] )));
                    ps.println(serverorb.object_to_string(o));
                    ps.close();
                } else
                {
                    // register server with naming context
                    NamingContextExt nc = NamingContextExtHelper.narrow(serverorb.resolve_initial_references("NameService"));
                    nc.bind(nc.to_name("TestServer.service"),o);
                }
            } catch ( Exception e )
            {
                if(XTTProperties.printDebug(null))
                {
                    XTTProperties.printException(e);
                }
            }
        }
    }
    private class RunServer extends Thread
    {
        public RunServer()
        {
            super("JacorbRunServer");
        }
        public void run()
        {
            serverorb.run();
        }
    }
    public void stopJacorbTestServer(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": stopJacorbTestServer:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
            return;
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": shutting down.");
            XTTProperties.printDebug(parameters[0] + ": shutting down now:");
            serverorb.shutdown(true);
        }
    }

    public void startJacorbClientBasicTest(String parameters[])
    {
        if(parameters==null)
        {
            XTTProperties.printFail(this.getClass().getName()+": startJacorbClientBasicTest:"+NO_ARGUMENTS);
            return;
        }
        if(parameters.length!=1)
        {
            XTTProperties.printFail(parameters[0] + ":"+NO_ARGUMENTS);
            XTTProperties.setTestStatus(XTTProperties.FAILED_WITH_MISSING_ARGUMENTS);
        } else
        {
            XTTProperties.printInfo(parameters[0] + ": running.");
            XTTProperties.printDebug(parameters[0] + ": running now:");
            basicTest();
        }
    }

    public void basicTest()
    {
        TestServer s = null;
        try
        {
            Client c = new Client();
            org.omg.CORBA.ORB clientorb = org.omg.CORBA.ORB.init(new String[]{},null);

            // get hold of the naming service
            NamingContextExt nc = NamingContextExtHelper.narrow(clientorb.resolve_initial_references("NameService"));

            s = TestServerHelper.narrow(nc.resolve(nc.to_name("TestServer.service")));

            // create a new any
            Any a = org.omg.CORBA.ORB.init().create_any();

            /* There are two ways to insert object references: */

            c.incr(1); // remember how many call backs we have to expect

            XTTProperties.printDebug("main(): Passing an object...");
            POA poa = POAHelper.narrow(clientorb.resolve_initial_references("RootPOA"));
            poa.the_POAManager().activate();

            poa.activate_object(c);

            /* insert an untyped reference */

            a.insert_Object(c._this_object());
            XTTProperties.printDebug( "main(): Output of generic: " + s.generic( a ) );

            c.incr(1);

            XTTProperties.printDebug("main(): Passing object again");

            /* insert an typed reference */

            TestServerHelper.insert( a, c._this());
            XTTProperties.printDebug( "main(): Output of generic: " + s.generic( a ) );


            /* insert an any */

            XTTProperties.printDebug("main(): Passing an any");
            Any inner_any = clientorb.create_any();
            inner_any.insert_string("Hello in any");
            a.insert_any(inner_any);
            XTTProperties.printDebug( "main(): Output of generic: " + s.generic( a ) );

            while( c.counter > 0 )
            {
            XTTProperties.printDebug("main(): Going to sleep to wait for incoming calls");
            Thread.currentThread().sleep(3000);
            }
            clientorb.shutdown(true);
        } catch ( Exception e)
        {
            if(XTTProperties.printDebug(null))
            {
                XTTProperties.printException(e);
            }
        }
    }


    private class Client extends TestServerPOA
    {
        public int counter;

        private synchronized void incr(int val)
        {
            counter += val;
        }

        public java.lang.String generic(Any a)
        {
            XTTProperties.printDebug("Client: Someone called me!");
            incr(-1);
            return "call back succeeded!";
        }

    }


}