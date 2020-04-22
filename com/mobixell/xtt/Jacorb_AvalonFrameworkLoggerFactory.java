package com.mobixell.xtt;

import org.jacorb.config.LoggerFactory;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;

public class Jacorb_AvalonFrameworkLoggerFactory implements LoggerFactory, Logger, java.io.Serializable
{
    public void configure(Configuration configuration) throws ConfigurationException
    {
        logger=this;
    }


    private Logger logger=null;
    /**
     * @return the name of the actual logging mechanism, e.g., "logkit"
     */
    public String getLoggingBackendName()
    {
        return "logkit";
    }

    /**
     * @return a console Logger for a given name
     */
    public Logger getNamedLogger(String name)
    {
        return logger;
    }

    /**
     * @return a console Logger for a given name
     */
    public Logger getNamedRootLogger(String name)
    {
        return logger;
    }

    /**
     * @return a name Logger for a given  file name and max size
     */
    public Logger getNamedLogger(String name, String fileName, long maxFileSize) throws java.io.IOException
    {
        return logger;
    }

    /**
     * set the file name and max file size for logging to a file
     */
    public void setDefaultLogFile(String fileName, long maxLogSize) throws java.io.IOException
    {}

    public void fatalError(java.lang.String message)
    {
        XTTProperties.printFail(message);
    }
    public void fatalError(java.lang.String message, java.lang.Throwable throwable)
    {
        XTTProperties.printFail(message);
        XTTProperties.printException(throwable);
    }
    public void error(java.lang.String message)
    {
        XTTProperties.printWarn(message);
    }
    public void error(java.lang.String message, java.lang.Throwable throwable)
    {
        XTTProperties.printWarn(message);
        XTTProperties.printException(throwable);
    }
    public void warn(java.lang.String message)
    {
        XTTProperties.printInfo(message);
    }
    public void warn(java.lang.String message, java.lang.Throwable throwable)
    {
        XTTProperties.printInfo(message);
        XTTProperties.printException(throwable);
    }
    public void info(java.lang.String message)
    {
        XTTProperties.printVerbose(message);
    }
    public void info(java.lang.String message, java.lang.Throwable throwable)
    {
        XTTProperties.printVerbose(message);
        XTTProperties.printException(throwable);
    }
    public void debug(java.lang.String message)
    {
        XTTProperties.printDebug(message);
    }
    public void debug(java.lang.String message, java.lang.Throwable throwable)
    {
        XTTProperties.printDebug(message);
        XTTProperties.printException(throwable);
    }
    public Logger getChildLogger(java.lang.String name)
    {
        return this;
    }
    public boolean isFatalErrorEnabled()
    {
        return XTTProperties.printFail(null);
    }
    public boolean isErrorEnabled()
    {
        return XTTProperties.printWarn(null);
    }
    public boolean isWarnEnabled()
    {
        return XTTProperties.printInfo(null);
    }
    public boolean isInfoEnabled()
    {
        return XTTProperties.printVerbose(null);
    }
    public boolean isDebugEnabled()
    {
        return XTTProperties.printDebug(null);
    }

}