package com.mobixell.xtt.jacorbtest;

/**
 *	Generated from IDL interface "TestServer"
 *	@author JacORB IDL compiler V 2.2.3, 10-Dec-2005
 */


public abstract class TestServerPOA
	extends org.omg.PortableServer.Servant
	implements org.omg.CORBA.portable.InvokeHandler, com.mobixell.xtt.jacorbtest.TestServerOperations
{
	static private final java.util.Hashtable<String,Integer> m_opsHash = new java.util.Hashtable<String,Integer>();
	static
	{
		m_opsHash.put ( "generic", new java.lang.Integer(0));
	}
	private String[] ids = {"IDL:com/mobixell/xtt/jacorbtest/TestServer:1.0"};
	public com.mobixell.xtt.jacorbtest.TestServer _this()
	{
		return com.mobixell.xtt.jacorbtest.TestServerHelper.narrow(_this_object());
	}
	public com.mobixell.xtt.jacorbtest.TestServer _this(org.omg.CORBA.ORB orb)
	{
		return com.mobixell.xtt.jacorbtest.TestServerHelper.narrow(_this_object(orb));
	}
	public org.omg.CORBA.portable.OutputStream _invoke(String method, org.omg.CORBA.portable.InputStream _input, org.omg.CORBA.portable.ResponseHandler handler)
		throws org.omg.CORBA.SystemException
	{
		org.omg.CORBA.portable.OutputStream _out = null;
		// do something
		// quick lookup of operation
		java.lang.Integer opsIndex = (java.lang.Integer)m_opsHash.get ( method );
		if ( null == opsIndex )
			throw new org.omg.CORBA.BAD_OPERATION(method + " not found");
		switch ( opsIndex.intValue() )
		{
			case 0: // generic
			{
				org.omg.CORBA.Any _arg0=_input.read_any();
				_out = handler.createReply();
				_out.write_string(generic(_arg0));
				break;
			}
		}
		return _out;
	}

	public String[] _all_interfaces(org.omg.PortableServer.POA poa, byte[] obj_id)
	{
		return ids;
	}
}
