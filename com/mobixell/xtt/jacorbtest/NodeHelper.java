package com.mobixell.xtt.jacorbtest;


/**
 *	Generated from IDL definition of struct "Node"
 *	@author JacORB IDL compiler 
 */

public final class NodeHelper
{
	private static org.omg.CORBA.TypeCode _type = null;
	public static org.omg.CORBA.TypeCode type ()
	{
		if (_type == null)
		{
			_type = org.omg.CORBA.ORB.init().create_struct_tc(com.mobixell.xtt.jacorbtest.NodeHelper.id(),"Node",new org.omg.CORBA.StructMember[]{new org.omg.CORBA.StructMember("name", org.omg.CORBA.ORB.init().create_string_tc(0), null),new org.omg.CORBA.StructMember("next", org.omg.CORBA.ORB.init().create_sequence_tc(0, org.omg.CORBA.ORB.init().create_recursive_tc("IDL:com/mobixell/xtt/jacorbtest/Node:1.0")), null)});
		}
		return _type;
	}

	public static void insert (final org.omg.CORBA.Any any, final com.mobixell.xtt.jacorbtest.Node s)
	{
		any.type(type());
		write( any.create_output_stream(),s);
	}

	public static com.mobixell.xtt.jacorbtest.Node extract (final org.omg.CORBA.Any any)
	{
		return read(any.create_input_stream());
	}

	public static String id()
	{
		return "IDL:com/mobixell/xtt/jacorbtest/Node:1.0";
	}
	public static com.mobixell.xtt.jacorbtest.Node read (final org.omg.CORBA.portable.InputStream in)
	{
		com.mobixell.xtt.jacorbtest.Node result = new com.mobixell.xtt.jacorbtest.Node();
		result.name=in.read_string();
		int _lresult_next0 = in.read_long();
		result.next = new com.mobixell.xtt.jacorbtest.Node[_lresult_next0];
		for (int i=0;i<result.next.length;i++)
		{
			result.next[i]=com.mobixell.xtt.jacorbtest.NodeHelper.read(in);
		}

		return result;
	}
	public static void write (final org.omg.CORBA.portable.OutputStream out, final com.mobixell.xtt.jacorbtest.Node s)
	{
		out.write_string(s.name);
		
		out.write_long(s.next.length);
		for (int i=0; i<s.next.length;i++)
		{
			com.mobixell.xtt.jacorbtest.NodeHelper.write(out,s.next[i]);
		}

	}
}
