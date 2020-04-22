package com.mobixell.xtt.gui.testlaunch.tree.nodes;

/**
 * @author guyb
 *
 */
public class TestNode extends AssetNode {
	private static final long serialVersionUID = 3639468403527260894L;
	String qcID;
	String description;
	String stepsDesc;
	String testLevel = "";
	String xfwVersion = "";
	String xfwFeature = "";
	String creationDate = "";
	String designer = "";

	public TestNode(String nodeName) {
		super(nodeName, NodeType.ROOT);
		setName(nodeName);
	}
	public String getDescription() {
		return description;
	}

	public void setDescription(String testDescription) {
		this.description = testDescription;
	}
	public String getStepsDesc() {
		return stepsDesc;
	}
	public void setStepsDesc(String stepsDesc) {
		this.stepsDesc = stepsDesc;
	}
	public String getXfwVersion() {
		return xfwVersion;
	}
	public void setXfwVersion(String xfwVersion) {
		this.xfwVersion = xfwVersion;
	}
	public String getCreationDate() {
		return creationDate;
	}
	public void setCreationDate(String creationDate) {
		this.creationDate = creationDate;
	}
	public String getDesigner() {
		return designer;
	}
	public void setDesigner(String designerName) {
		this.designer = designerName;
	}
	public String getTestLevel() {
		return testLevel;
	}
	public void setLevel(String testLevel) {
		this.testLevel = testLevel;
	}
	public String getQcID() {
		return qcID;
	}
	public void setQcID(String qcID) {
		this.qcID = qcID;
	}
}
