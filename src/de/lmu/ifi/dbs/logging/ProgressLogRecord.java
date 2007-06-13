package de.lmu.ifi.dbs.logging;


import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Additionally to the functionality of a LogRecord,
 * a ProgressLogRecord provides information concerning 
 * the progress as the name of the progressing task
 * and the percentage of the progress.
 * 
 * An example for usage may be:
 * <pre>
 * partitionProgress.setProcessed(processed++);
 * logger.log(new ProgressLogRecord(Level.INFO,Util.status(partitionProgress),partitionProgress.getTask(),partitionProgress.status()));
 * </pre>
 * This enables the presentation of the progress in a graphical context.
 * 
 * @author Arthur Zimek
 */
public class ProgressLogRecord extends LogRecord
{
    
    
    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = 8776951182872391225L;

    /**
     * The name of the progressing task.
     */
    private String task;
    
    /**
     * The percentage of progress.
     */
    private int percentage;

    /**
     * Provides a ProgressLogRecord.
     * 
     * @param level logging level
     * @param msg log message
     * @param task the name of the progressing task
     * @param percentage the percentage of progress
     * @see LogRecord#LogRecord(Level, String)
     */
    public ProgressLogRecord(Level level, String msg, String task, int percentage)
    {
        super(level, msg);
        this.task = task;
        this.percentage = percentage;
    }

    /**
     * Returns the percentage of the progress as set in the constructor.
     * 
     * 
     * @return the percentage of the progress as set in the constructor
     */
    public int getPercentage()
    {
        return this.percentage;
    }

    /**
     * Returns the name of the progressing task.
     * 
     * 
     * @return the name of the progressing task
     */
    public String getTask()
    {
        return this.task;
    }

    
}
