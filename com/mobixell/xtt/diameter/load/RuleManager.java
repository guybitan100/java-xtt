package com.mobixell.xtt.diameter.load;

import java.util.Hashtable;

public class RuleManager {

public final String ACTIVE_VIDEO_OPT = "activate_video_optimization"; 
public final String ACTIVE_WEB_OPT = "activate_web_optimization";
public final String ACTIVE_CON_FILTER = "activate_content_filter";
public final String ACTIVE_ANTI_V_FILTER = "activate_antivirus_filter";
public final String ACTIVE_ADVERTISING = "activate_advertising";

	public Hashtable<Integer, RuleDataItem> rulesTable = new Hashtable<Integer, RuleDataItem>();
	
	public RuleManager ()
	{
		rulesTable.put(1, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_lossless_impact"));
		rulesTable.put(2, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_no_visual_impact"));
		rulesTable.put(3, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_minor_visual_impact"));
		rulesTable.put(4, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_small_visual_impact"));
		rulesTable.put(5, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_volume_saver"));
		rulesTable.put(6, new RuleDataItem(ACTIVE_VIDEO_OPT,"set_vo_level_volume_saver_extra"));
		
		rulesTable.put(7, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_lossless_impact"));
		rulesTable.put(8, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_no_visual_impact"));
		rulesTable.put(9, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_minor_visual_impact"));
		rulesTable.put(10, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_small_visual_impact"));
		rulesTable.put(11, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_volume_saver"));
		rulesTable.put(12, new RuleDataItem(ACTIVE_WEB_OPT,"set_wo_level_volume_saver_extra"));
		
		rulesTable.put(13, new RuleDataItem(ACTIVE_CON_FILTER,ACTIVE_CON_FILTER));
		
		rulesTable.put(14, new RuleDataItem(ACTIVE_ANTI_V_FILTER,"set_av_depth_1"));
		rulesTable.put(15, new RuleDataItem(ACTIVE_ANTI_V_FILTER,"set_av_depth_2"));
		rulesTable.put(16, new RuleDataItem(ACTIVE_ANTI_V_FILTER,"set_av_depth_3"));
		
		rulesTable.put(17, new RuleDataItem(ACTIVE_ADVERTISING,ACTIVE_ADVERTISING));
	}
	public String[] getRuleByRuleId (int ruleId)
	{
		RuleDataItem ruleData = rulesTable.get(ruleId);
		String [] rule = new String [2];
		
		rule[0] = new String (ruleData.getRuleCategory());
		rule[1] = new String (ruleData.getRuleSubCategory());
		return rule;
	}
	public String[] generateVideoOptRule ()
	{
		 int min = 1;
	     int max = 6;
		 return getRuleByRuleId((int) (Math.random() * (max - min + 1) ) + min);		
	}
	public String[] generateWebOptRule ()
	{
		 int min = 7;
	     int max = 12;
	     return getRuleByRuleId((int) (Math.random() * (max - min + 1) ) + min);
	}
	public String[] generateAntiVirusRule ()
	{
		 int min = 14;
	     int max = 16;
	     return getRuleByRuleId((int) (Math.random() * (max - min + 1) ) + min);
	}
	public String[] generateConFilterRule ()
	{
		 int min = 13;
	     int max = 13;
	     return getRuleByRuleId((int) (Math.random() * (max - min + 1) ) + min);
	}
	public String[] generateAdvertisingRule ()
	{
		 int min = 17;
	     int max = 17;
	     return getRuleByRuleId((int) (Math.random() * (max - min + 1) ) + min);	
	}
}
