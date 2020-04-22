package com.mobixell.xtt.jacorbtest;

import com.mobixell.xtt.jacorbtest.TestServer;
import com.mobixell.xtt.jacorbtest.TestServerHelper;

/**
 *	Generated from IDL interface "TestServer"
 *	@author JacORB IDL compiler V 2.2.3, 10-Dec-2005
 */

public final class TestServerHolder	implements org.omg.CORBA.portable.Streamable{
	 public TestServer value;
	public TestServerHolder()
	{
	}
	public TestServerHolder (final TestServer initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type()
	{
		return TestServerHelper.type();
	}
	public void _read (final org.omg.CORBA.portable.InputStream in)
	{
		value = TestServerHelper.read (in);
	}
	public void _write (final org.omg.CORBA.portable.OutputStream _out)
	{
		TestServerHelper.write (_out,value);
	}
}
