package com.mobixell.xtt.jacorbtest;
public final class stringsHelper
{
	private static org.omg.CORBA.TypeCode _type = org.omg.CORBA.ORB.init().create_array_tc(3,org.omg.CORBA.ORB.init().create_string_tc(0));
	public static void insert (final org.omg.CORBA.Any any, final java.lang.String[] s)
	{
		any.type(type());
		write( any.create_output_stream(),s);
	}

	public static java.lang.String[] extract (final org.omg.CORBA.Any any)
	{
		return read(any.create_input_stream());
	}

	public static org.omg.CORBA.TypeCode type()
	{
		return _type;
	}
	public static String id()
	{
		return "IDL:com/mobixell/xtt/jacorbtest/strings:1.0";
	}
	public static java.lang.String[] read (final org.omg.CORBA.portable.InputStream _in)
	{
		java.lang.String[] result = new java.lang.String[3]; // java.lang.String[]
		for (int i = 0; i < 3; i++)
		{
			result[i]=_in.read_string();
		}
		return result;
	}
	public static void write (final org.omg.CORBA.portable.OutputStream out, final java.lang.String[] s)
	{
		if (s.length != 3)
			throw new org.omg.CORBA.MARSHAL("Incorrect array size");
		for (int i = 0; i < s.length; i++)
		{
			out.write_string(s[i]);
		}
	}
}
