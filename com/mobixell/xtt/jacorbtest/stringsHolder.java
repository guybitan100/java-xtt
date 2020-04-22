package com.mobixell.xtt.jacorbtest;

import com.mobixell.xtt.jacorbtest.stringsHelper;

public final class stringsHolder
	implements org.omg.CORBA.portable.Streamable
{
	public java.lang.String[] value;
	public stringsHolder ()
	{
	}
	public stringsHolder (final java.lang.String[] initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return stringsHelper.type ();
	}
	public void _read (final org.omg.CORBA.portable.InputStream _in)
	{
		value = stringsHelper.read (_in);
	}
	public void _write (final org.omg.CORBA.portable.OutputStream _out)
	{
		stringsHelper.write (_out,value);
	}
}
