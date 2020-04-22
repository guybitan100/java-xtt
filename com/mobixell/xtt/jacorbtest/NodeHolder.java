package com.mobixell.xtt.jacorbtest;

/**
 *	Generated from IDL definition of struct "Node"
 *	@author JacORB IDL compiler 
 */

public final class NodeHolder
	implements org.omg.CORBA.portable.Streamable
{
	public com.mobixell.xtt.jacorbtest.Node value;

	public NodeHolder ()
	{
	}
	public NodeHolder(final com.mobixell.xtt.jacorbtest.Node initial)
	{
		value = initial;
	}
	public org.omg.CORBA.TypeCode _type ()
	{
		return com.mobixell.xtt.jacorbtest.NodeHelper.type ();
	}
	public void _read(final org.omg.CORBA.portable.InputStream _in)
	{
		value = com.mobixell.xtt.jacorbtest.NodeHelper.read(_in);
	}
	public void _write(final org.omg.CORBA.portable.OutputStream _out)
	{
		com.mobixell.xtt.jacorbtest.NodeHelper.write(_out, value);
	}
}
