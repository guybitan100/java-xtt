package com.mobixell.xtt.jacorbtest;

import org.omg.PortableServer.POA;

/**
 *	Generated from IDL interface "TestServer"
 *	@author JacORB IDL compiler V 2.2.3, 10-Dec-2005
 */

public class TestServerPOATie
	extends TestServerPOA
{
	private TestServerOperations _delegate;

	private POA _poa;
	public TestServerPOATie(TestServerOperations delegate)
	{
		_delegate = delegate;
	}
	public TestServerPOATie(TestServerOperations delegate, POA poa)
	{
		_delegate = delegate;
		_poa = poa;
	}
	public com.mobixell.xtt.jacorbtest.TestServer _this()
	{
		return com.mobixell.xtt.jacorbtest.TestServerHelper.narrow(_this_object());
	}
	public com.mobixell.xtt.jacorbtest.TestServer _this(org.omg.CORBA.ORB orb)
	{
		return com.mobixell.xtt.jacorbtest.TestServerHelper.narrow(_this_object(orb));
	}
	public TestServerOperations _delegate()
	{
		return _delegate;
	}
	public void _delegate(TestServerOperations delegate)
	{
		_delegate = delegate;
	}
	public POA _default_POA()
	{
		if (_poa != null)
		{
			return _poa;
		}
		else
		{
			return super._default_POA();
		}
	}
	public java.lang.String generic(org.omg.CORBA.Any a)
	{
		return _delegate.generic(a);
	}

}
