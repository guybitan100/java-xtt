package com.mobixell.xtt.jacorbtest;
/**
 *	Generated from IDL definition of union "Nums"
 *	@author JacORB IDL compiler 
 */

public final class NumsHelper
{
	private static org.omg.CORBA.TypeCode _type;
	public static void insert (final org.omg.CORBA.Any any, final com.mobixell.xtt.jacorbtest.Nums s)
	{
		any.type(type());
		write( any.create_output_stream(),s);
	}

	public static com.mobixell.xtt.jacorbtest.Nums extract (final org.omg.CORBA.Any any)
	{
		return read(any.create_input_stream());
	}

	public static String id()
	{
		return "IDL:com/mobixell/xtt/jacorbtest/Nums:1.0";
	}
	public static Nums read (org.omg.CORBA.portable.InputStream in)
	{
		Nums result = new Nums ();
		char disc=in.read_char();
		switch (disc)
		{
			case 'f':
			{
				float _var;
				_var=in.read_float();
				result.f (_var);
				break;
			}
			case 'l':
			{
				int _var;
				_var=in.read_long();
				result.l (_var);
				break;
			}
			default:
			{
				short _var;
				_var=in.read_short();
				result.s (_var);
			}
		}
		return result;
	}
	public static void write (org.omg.CORBA.portable.OutputStream out, Nums s)
	{
		out.write_char(s.discriminator ());
		switch (s.discriminator ())
		{
			case 'f':
			{
				out.write_float(s.f ());
				break;
			}
			case 'l':
			{
				out.write_long(s.l ());
				break;
			}
			default:
			{
				out.write_short(s.s ());
			}
		}
	}
	public static org.omg.CORBA.TypeCode type ()
	{
		if (_type == null)
		{
			org.omg.CORBA.UnionMember[] members = new org.omg.CORBA.UnionMember[3];
			org.omg.CORBA.Any label_any;
			label_any = org.omg.CORBA.ORB.init().create_any ();
			label_any.insert_char ('f');
			members[2] = new org.omg.CORBA.UnionMember ("f", label_any, org.omg.CORBA.ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.from_int(6)),null);
			label_any = org.omg.CORBA.ORB.init().create_any ();
			label_any.insert_char ('l');
			members[1] = new org.omg.CORBA.UnionMember ("l", label_any, org.omg.CORBA.ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.from_int(3)),null);
			label_any = org.omg.CORBA.ORB.init().create_any ();
			label_any.insert_octet ((byte)0);
			members[0] = new org.omg.CORBA.UnionMember ("s", label_any, org.omg.CORBA.ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.from_int(2)),null);
			 _type = org.omg.CORBA.ORB.init().create_union_tc(id(),"Nums",org.omg.CORBA.ORB.init().get_primitive_tc(org.omg.CORBA.TCKind.from_int(9)), members);
		}
		return _type;
	}
}
