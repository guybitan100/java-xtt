package com.mobixell.xtt.jacorbtest;

import com.mobixell.xtt.jacorbtest.Nums;
import com.mobixell.xtt.jacorbtest.NumsHelper;

/**
 *	Generated from IDL definition of union "Nums"
 *	@author JacORB IDL compiler 
 */

public final class NumsHolder
	implements org.omg.CORBA.portable.Streamable
{
	public Nums value;

	public NumsHolder ()
	{
	}
	public NumsHolder (final Nums initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return NumsHelper.type ();
	}
	public void _read (final org.omg.CORBA.portable.InputStream in)
	{
		value = NumsHelper.read (in);
	}
	public void _write (final org.omg.CORBA.portable.OutputStream out)
	{
		NumsHelper.write (out, value);
	}
}
