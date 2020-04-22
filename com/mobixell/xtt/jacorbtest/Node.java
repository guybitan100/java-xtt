package com.mobixell.xtt.jacorbtest;

/**
 *	Generated from IDL definition of struct "Node"
 *	@author JacORB IDL compiler 
 */

public final class Node
	implements org.omg.CORBA.portable.IDLEntity
{
	public Node(){}
	public java.lang.String name = "";
	public com.mobixell.xtt.jacorbtest.Node[] next;
	public Node(java.lang.String name, com.mobixell.xtt.jacorbtest.Node[] next)
	{
		this.name = name;
		this.next = next;
	}
}
