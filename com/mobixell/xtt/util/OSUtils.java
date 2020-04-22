package com.mobixell.xtt.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class OSUtils {

	public static String getLoginUser() {
		return System.getProperty("user.name").trim();
	}
	public static String getSystemPath() {
		return System.getProperty("user.dir").trim();
	}
	public static void printInfo() {
		String currentUser = System.getProperty("user.name");
		System.out.println("Current user is " + currentUser);
		String nameOS = "os.name";
		String versionOS = "os.version";
		String architectureOS = "os.arch";
		System.out.println("\n  The information about OS");
		System.out.println("\nName of the OS: " + System.getProperty(nameOS));
		System.out.println("Version of the OS: "
				+ System.getProperty(versionOS));
		System.out.println("Architecture of THe OS: "
				+ System.getProperty(architectureOS));
	}

	public static String getOSName() {
		return System.getProperty("os.name");
	}
	public static String getOSDateTime() {
		String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
		Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	    return sdf.format(cal.getTime());
	}
	public static String getOSVer() {
		return System.getProperty("os.version");
	}

	public static String getOSArch() {
		return System.getProperty("os.arch");
	}

	public static boolean checkRootUser() 
	{
		String currentUser = getLoginUser();
		if ("os.name".indexOf("Linux") >= 0) 
		{
			if (currentUser.indexOf("root") < 0) 
				return false;
			else 
				return true;
		}
		return true;
	}
	
	public static String getHostName()
	{
		InetAddress addr = null;
		try {
		    addr = InetAddress.getLocalHost();		
		} catch (UnknownHostException e) {
		}
		  return addr.getHostName();

	}
	public static String getIpAddr()
	{
		InetAddress addr = null;
		try {
		    addr = InetAddress.getLocalHost();

		} catch (UnknownHostException e) {
		}
		return  addr.getHostAddress();
	}
}
