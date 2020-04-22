package com.mobixell.xtt.gui;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JOptionPane;
import com.mobixell.xtt.XTTProperties;
import com.mobixell.xtt.gui.main.XTTGui;
import com.mobixell.xtt.gui.testlaunch.tree.TreeTestController;
public class UpdateXTT
{
	/**
	 * Sun property pointing the main class and its arguments. Might not be
	 * defined on non Hotspot VM implementations.
	 */
	public static final String SUN_JAVA_COMMAND = "sun.java.command";
	private final static String JENKINS_XTT_PATH = "http://jenkins2.mobixell.com:8080/view/XTT/job/XTT/";
	private final static String JENKINS_JAR_BUILD_PATH = "/artifact/XTT/lib/xtt.jar";
	private static int LAST_STABLE_BUILD = 0;
	private static int CURRENT_BUILD = 0;
	private static String LOCAL_JAR_PATH = "";
	private static StringBuffer cmd;
	/**
	 * Restart the current Java application
	 * 
	 * @param runBeforeRestart
	 *            some custom code to be run before restarting
	 * @throws IOException
	 */
	public static void restartApplication() throws IOException
	{
		XTTProperties.printDebug("Restarting XTT Application...");
		// execute the command in a shutdown hook, to be sure that all the
		// resources have been disposed before restarting the application
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run()
			{
				try
				{
					XTTProperties.printDebug("Exe: " + cmd.toString());
					Runtime.getRuntime().exec(cmd.toString());
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		// exit
		System.exit(0);
	}
	public static void setCmd()
	{
		try
		{
			CURRENT_BUILD = Integer.parseInt(XTTProperties.getXTTBuild());
		}
		catch (Exception e)
		{
			CURRENT_BUILD = -1;
		}
		try
		{
			// java binary
			String java = System.getProperty("java.home") + "/bin/java";
			// vm arguments
			List<String> vmArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
			StringBuffer vmArgsOneLine = new StringBuffer();
			for (String arg : vmArguments)
			{
				// if it's the agent argument : we ignore it otherwise the
				// address of the old application and the new one will be in
				// conflict
				if (!arg.contains("-agentlib"))
				{
					vmArgsOneLine.append(arg);
					vmArgsOneLine.append(" ");
				}
			}
			// init the command to execute, add the vm args
			cmd = new StringBuffer("\"" + java + "\" " + vmArgsOneLine);

			// program main and program arguments
			String[] mainCommand = System.getProperty(SUN_JAVA_COMMAND).split(" ");
			// program main is a jar
			if (mainCommand[0].endsWith(".jar"))
			{
				// if it's a jar, add -jar mainJar
				cmd.append("-jar " + new File(mainCommand[0]).getPath());
			}
			else
			{
				// else it's a .class, add the classpath and mainClass
				cmd.append("-cp \"" + System.getProperty("java.class.path") + "\" " + mainCommand[0]);
				{
					String[] classPath = System.getProperty("java.class.path").split(";");
					for (int i = 0; i < classPath.length; i++)
					{
						if (classPath[i].toLowerCase().indexOf(XTTProperties.JARFILE) >= 0
								|| classPath[i].endsWith(File.separator + XTTProperties.JARFILE))
						{
							LOCAL_JAR_PATH = classPath[i];
						}
					}
				}
			}
			// finally add program arguments
			for (int i = 1; i < mainCommand.length; i++)
			{
				cmd.append(" ");
				cmd.append(mainCommand[i]);
			}
		}
		catch (Exception e){}
	}
	public static void updateJar(XTTGui xttgui) throws Exception
	{
		try
		{
			if (LAST_STABLE_BUILD > CURRENT_BUILD)
			{
				String jarPath = JENKINS_XTT_PATH + LAST_STABLE_BUILD + JENKINS_JAR_BUILD_PATH;
				XTTProperties.printDebug("There is stable build in: " + jarPath);
				URL u = new URL(jarPath);
				URLConnection uc = u.openConnection();
				uc = u.openConnection();
				int contentLength = uc.getContentLength();
				InputStream raw = uc.getInputStream();
				InputStream in = new BufferedInputStream(raw);
				byte[] data = new byte[contentLength];
				int bytesRead = 0;
				int offset = 0;
				while (offset < contentLength)
				{
					bytesRead = in.read(data, offset, data.length - offset);
					if (bytesRead == -1) break;
					offset += bytesRead;
				}
				in.close();

				if (offset != contentLength)
				{
					throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
				}
				setCmd();
				File file = new File(LOCAL_JAR_PATH);

				if (CURRENT_BUILD > 0) copyFile(file, new File(file.getPath() + "_" + CURRENT_BUILD));
				else copyFile(file, new File(file.getPath() + "_old"));

				FileOutputStream out = new FileOutputStream(LOCAL_JAR_PATH);
				out.write(data);
				out.flush();
				out.close();
				XTTProperties.printDebug("jar was successfully update");
				Object[] options = { "Cancel", "Restart" };
				TreeTestController.getTestLauncherGui();
				int n = JOptionPane.showOptionDialog(xttgui, "Do you want to restart xtt?","XTT was update successfuly !", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,null, options, options[0]);
				if (n == 1)
				{
					restartApplication();
				}
				else
				{
					TreeTestController.getTestLauncherGui();
					JOptionPane.showMessageDialog(xttgui, "Update will take efect only after restart!");
				}
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(TreeTestController.getTestLauncherGui(),
					"XTT jar is not available in this path: \n" + JENKINS_XTT_PATH + LAST_STABLE_BUILD
							+ JENKINS_JAR_BUILD_PATH, "XTT jar error", JOptionPane.ERROR_MESSAGE);
		}
	}

	public static void copyFile(File sorce, File dest) throws IOException
	{
		InputStream in = new FileInputStream(sorce);
		OutputStream out = new FileOutputStream(dest);

		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
		XTTProperties.printDebug("Previous jar was backup under: " + dest.getPath());
	}

	public static int getLastStableBuild()
	{
		try
		{
			URL u = new URL(JENKINS_XTT_PATH);
			URLConnection uc = u.openConnection();
			String contentType = uc.getContentType();
			if (contentType.startsWith("text/"))
			{
				String buf = readInputStreamToString(uc.getInputStream());
				int index = buf.indexOf("Last stable build");
				if (index >= 0)
				{
					Pattern intsOnly = Pattern.compile("\\d+");
					Matcher makeMatch = intsOnly.matcher(buf.substring(index));
					if (makeMatch.find())
					{
						try
						{
							LAST_STABLE_BUILD = Integer.parseInt(makeMatch.group());
						}
						catch (Exception e)
						{
							LAST_STABLE_BUILD = 0;
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			JOptionPane.showMessageDialog(null, "jenkins2.mobixell.com:8080 isn't available\ncheck the inteternet connection.","XTT Update",JOptionPane.ERROR_MESSAGE);
			
		}
		return LAST_STABLE_BUILD;
	}
	private static String readInputStreamToString(InputStream in) throws Exception
	{
		StringBuffer buf = new StringBuffer();
		int c;
		while ((c = in.read()) >= 0)
		{
			buf.append((char) c);
		}
		in.close();
		return buf.toString();
	}
}