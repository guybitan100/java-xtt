package com.mobixell.xtt.gui.testlaunch.table;

import com.mobixell.xtt.gui.testlaunch.tree.nodes.ThreadNode;

public class ThreadDataItem
{
	private Boolean status = true;
	private String name = "Thread";
	public int id = 0;
	private ThreadNode node;
	public ThreadDataItem(int id)
	{
		this.status = true;
		this.id=id;
		this.name = "Thread" + id;
	}
	public ThreadDataItem(boolean status, String name)
	{
		this.status = status;
		this.name = name;
	}
	public ThreadNode getNode()
	{
		return node;
	}
	public void setNode(ThreadNode node)
	{
		this.node = node;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String threadname)
	{
		this.name = threadname;
	}
	public Boolean getStatus()
	{
		return status;
	}
	public void setStatus(Boolean status)
	{
		this.status = status;
	}
	public int getId()
	{
		return id;
	}
	public void setId(int id)
	{
		this.id = id;
	}
}