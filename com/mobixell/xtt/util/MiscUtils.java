/*
 * Copyright 2005-2010 Ignis Software Tools Ltd. All rights reserved.
 */
package com.mobixell.xtt.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class MiscUtils {
	public static String ping(String addr) {
		BufferedReader out;
		StringBuffer sb = new StringBuffer();
		Process p;
		try {
			if (isWindows())
				p = Runtime.getRuntime().exec("ping " + addr);
			else 
				p = Runtime.getRuntime().exec("ping -c 4 -i 0.1 " + addr);	
			
			out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String s = null;
			while ((s = out.readLine()) != null) {
				sb.append(s + "\r\n");
				if (sb.indexOf("timed out")>=0)
					break;
			}
		} catch (IOException io) {
			return sb.toString() + "\n\n" + StringUtils.getStackTrace(io);
		}
		return sb.toString();
	}
	public static boolean isPing(String addr){
    	String result = ping(addr);
    	if(isWindows())
    		return (result.indexOf("timed out") < 0 && result.indexOf("Reply from " + addr) >= 0);
    	if (isSolaris() || isUnix())
    		return (result.indexOf("0 received") < 0 && result.indexOf("64 bytes from " + addr) >= 0);
    	else
    		return false;
    }

	public static boolean isWindows() {
		 
		String os = System.getProperty("os.name").toLowerCase();
		// windows
		return (os.indexOf("win") >= 0);
 
	}
	
	public static boolean isUnix() {
 
		String os = System.getProperty("os.name").toLowerCase();
		// linux or unix
		return (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0);
 
	}
 
	public static boolean isSolaris() {
 
		String os = System.getProperty("os.name").toLowerCase();
		// Solaris
		return (os.indexOf("sunos") >= 0);
 
	}
}
