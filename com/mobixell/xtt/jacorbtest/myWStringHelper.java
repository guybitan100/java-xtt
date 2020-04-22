package com.mobixell.xtt.jacorbtest;

/**
 *	Generated from IDL definition of alias "myWString"
 *	@author JacORB IDL compiler 
 */

public final class myWStringHelper
{
	private static org.omg.CORBA.TypeCode _type = null;

	public static void insert (org.omg.CORBA.Any any, java.lang.String s)
	{
		any.type (type ());
		write (any.create_output_stream (), s);
	}

	public static java.lang.String extract (final org.omg.CORBA.Any any)
	{
		return read (any.create_input_stream ());
	}

	public static org.omg.CORBA.TypeCode type ()
	{
		if (_type == null)
		{
			_type = org.omg.CORBA.ORB.init().create_alias_tc(com.mobixell.xtt.jacorbtest.myWStringHelper.id(), "myWString",org.omg.CORBA.ORB.init().create_wstring_tc(0));
		}
		return _type;
	}

	public static String id()
	{
		return "IDL:com/mobixell/xtt/jacorbtest/myWString:1.0";
	}
	public static java.lang.String read (final org.omg.CORBA.portable.InputStream _in)
	{
		java.lang.String _result;
		_result=_in.read_wstring();
		return _result;
	}

	public static void write (final org.omg.CORBA.portable.OutputStream _out, java.lang.String _s)
	{
		_out.write_wstring(_s);
	}
}
