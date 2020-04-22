package com.mobixell.xtt.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateTimeUtils {

	public static String getTime (String format)
	{
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(format);
	    return sdf.format(cal.getTime());
	}
	public static String getTime ()
	{
		String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss(SSS)";
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	    return sdf.format(cal.getTime());
	}
	public static long getTimeInSec ()
	{
		return System.currentTimeMillis()/1000;
	}
	public static long getTimeInMillis ()
	{
		return System.currentTimeMillis();
	}
}
