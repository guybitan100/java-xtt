package com.mobixell.xtt.jacorbtest;

/**
 *	Generated from IDL definition of union "Nums"
 *	@author JacORB IDL compiler 
 */

public final class Nums
	implements org.omg.CORBA.portable.IDLEntity
{
	private char discriminator;
	private float f;
	private int l;
	private short s;

	public Nums ()
	{
	}

	public char discriminator ()
	{
		return discriminator;
	}

	public float f ()
	{
		if (discriminator != 'f')
			throw new org.omg.CORBA.BAD_OPERATION();
		return f;
	}

	public void f (float _x)
	{
		discriminator = 'f';
		f = _x;
	}

	public int l ()
	{
		if (discriminator != 'l')
			throw new org.omg.CORBA.BAD_OPERATION();
		return l;
	}

	public void l (int _x)
	{
		discriminator = 'l';
		l = _x;
	}

	public short s ()
	{
		if (discriminator != (char)0)
			throw new org.omg.CORBA.BAD_OPERATION();
		return s;
	}

	public void s (short _x)
	{
		discriminator = (char)0;
		s = _x;
	}

	public void s (char _discriminator, short _x)
	{
		if (_discriminator != (char)0)
			throw new org.omg.CORBA.BAD_OPERATION();
		discriminator = _discriminator;
		s = _x;
	}

}
