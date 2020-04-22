package com.mobixell.xtt.jacorbtest;

import com.mobixell.xtt.jacorbtest.MyStringSeqHelper;

/**
 *	Generated from IDL definition of alias "MyStringSeq"
 *	@author JacORB IDL compiler 
 */

public final class MyStringSeqHolder
	implements org.omg.CORBA.portable.Streamable
{
	public java.lang.String[] value;

	public MyStringSeqHolder ()
	{
	}
	public MyStringSeqHolder (final java.lang.String[] initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return MyStringSeqHelper.type ();
	}
	public void _read (final org.omg.CORBA.portable.InputStream in)
	{
		value = MyStringSeqHelper.read (in);
	}
	public void _write (final org.omg.CORBA.portable.OutputStream out)
	{
		MyStringSeqHelper.write (out,value);
	}
}
