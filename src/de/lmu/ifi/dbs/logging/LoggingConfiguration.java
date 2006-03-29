package de.lmu.ifi.dbs.logging;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Facility for configuration of logging.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LoggingConfiguration
{
    /**
     * Central switch for debug status.
     * Setting this variable to true will generally increase
     * runtime considerably.
     */
    public static final boolean DEBUG = false;
    
    /**
     * Configuration code for command line interface.
     */
    public static final int CLI = 0;
    
    /**
     * General logger level. Per default, the general logger level
     * is set to ALL in debug mode, to INFO in usual mode.
     */
    private Level loggerLevel = DEBUG ? Level.ALL : Level.INFO;
    
    private DebugFilter debugFilter = new DebugFilter(DEBUG ? Level.ALL : Level.OFF);    
    
    public void configure(Logger logger, int configuration)
    {
        switch (configuration)
        {
            case CLI:
                configure(logger,consoleHandlers());
                break;
            default:
                throw new IllegalArgumentException("unknown configuration code "+configuration);
        }
    }
    
    public void configure(Logger logger, Handler[] handler)
    {
        Handler[] oldHandler = logger.getHandlers();
        for(Handler h : oldHandler)
        {
            logger.removeHandler(h);
        }
        for(Handler h : handler)
        {
            logger.addHandler(h);
        }
        logger.setLevel(loggerLevel);
    }

    public void setLoggerLevel(Level level)
    {
        this.loggerLevel = level;
    }
    
    public void setDebugLevel(Level level)
    {
        debugFilter.setDebugLevel(level);
    }
    
    protected Handler[] consoleHandlers()
    {
        Handler debugHandler = new ImmediateFlushHandler(new MaskingOutputStream(System.err),new SimpleFormatter());
        debugHandler.setFilter(debugFilter);
        Handler verboseHandler = new ImmediateFlushHandler(new MaskingOutputStream(System.out),new MessageFormatter());
        verboseHandler.setFilter(new InfoFilter());
        // TODO: perhaps more suitable formatters?
        Handler warningHandler = new ImmediateFlushHandler(new MaskingOutputStream(System.err),new SimpleFormatter());
        warningHandler.setFilter(new WarningFilter());
        Handler exceptionHandler = new ImmediateFlushHandler(new MaskingOutputStream(System.err),new ExceptionFormatter());
        exceptionHandler.setFilter(new ExceptionFilter());
        Handler[] consoleHandlers = {debugHandler,verboseHandler,warningHandler,exceptionHandler};
        return consoleHandlers;
    }
    
    public static void configureRoot(int configuration)
    {
        LoggingConfiguration loggingConfiguration = new LoggingConfiguration();
        loggingConfiguration.configure(Logger.getLogger(""), configuration);
    }
}
