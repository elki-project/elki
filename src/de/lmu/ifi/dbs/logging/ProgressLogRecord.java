package de.lmu.ifi.dbs.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class ProgressLogRecord extends LogRecord
{
    
    
    /**
     * 
     */
    private static final long serialVersionUID = 8776951182872391225L;

    private String task;
    
    private int percentage;

    /**
     * @param level
     * @param msg
     * @param task
     * @param percentage
     */
    public ProgressLogRecord(Level level, String msg, String task, int percentage)
    {
        super(level, msg);
        this.task = task;
        this.percentage = percentage;
    }

    public int getPercentage()
    {
        return this.percentage;
    }

    public String getTask()
    {
        return this.task;
    }

    
}
