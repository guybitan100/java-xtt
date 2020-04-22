package com.mobixell.xtt.gui.testlaunch.table;

import com.mobixell.xtt.gui.testlaunch.tree.nodes.LoopNode;

public class LoopDataItem
{
	private Boolean status = true;
	private String name = "Loop";
	private String start = "0";
	private String stop =  "1";
	private String step =  "1";
	
	public int id = 0;
	private LoopNode node;
	public LoopDataItem(int id)
	{
		this.status = true;
		this.id=id;
		this.name = "Loop" + id;
	}
	public LoopDataItem(boolean status, String name,String start,String stop,String step)
	{
		this.status = status;
		this.name = name;
		this.start = start;
		this.stop = stop;
		this.step = step;
	}
	public LoopNode getNode()
	{
		return node;
	}
	public void setNode(LoopNode node)
	{
		this.node = node;
	}
	public String getName()
	{
		return name;
	}
	public void setName(String Loopname)
	{
		this.name = Loopname;
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
	public String getStart() {
		return start;
	}
	public void setStart(String start) {
		this.start = start;
	}
	public String getStop() {
		return stop;
	}
	public void setStop(String stop) {
		this.stop = stop;
	}
	public String getStep() {
		return step;
	}
	public void setStep(String step) {
		this.step = step;
	}
}