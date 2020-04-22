package com.mobixell.xtt.diameter.load;

public class RuleDataItem {	
	private String ruleCategory = null;
	private String ruleSubCategory = null;
		
public RuleDataItem (String ruleCategory,String ruleSubCategory){
		
		setRuleCategory(ruleCategory);
		setRuleSubCategory(ruleSubCategory);
	}
	public String getRuleCategory() {
		return ruleCategory;
	}
	public void setRuleCategory(String ruleCategory) {
		this.ruleCategory = ruleCategory;
	}
	public String getRuleSubCategory() {
		return ruleSubCategory;
	}
	public void setRuleSubCategory(String ruleSubCategory) {
		this.ruleSubCategory = ruleSubCategory;
	}	
}