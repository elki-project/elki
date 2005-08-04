package de.lmu.ifi.dbs.utilities;

/**
 * A progress object for a given overal number of items to process.
 * The number of already processed items at a point in time can be updated.
 * 
 * The main feature of this class is to provide a String representation of the progress
 * suitable as a message for printing to
 * the command line interface.
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class Progress
{
    /**
     * The overall number of items to process.
     */
    private final int total;
    
    /**
     * Holds the length of a String describing the total number.
     */
    private final int totalLength; 
    
    /**
     * The number of items already processed at a time being.
     */
    private int processed;
    
    /**
     * A progress object for a given overal number of items to process.
     * 
     * @param total the overall number of items to process
     */
    public Progress(int total)
    {
        this.total = total;
        this.totalLength = Integer.toString(total).length();
    }
    
    /**
     * Sets the number of items already processed at a time being.
     * 
     * 
     * @param processed the number of items already processed at a time being
     * @throws IllegalArgumentException if the given number is negative or exceeds the overall number of items to process
     */
    public void setProcessed(int processed) throws IllegalArgumentException
    {
        if(processed > total)
        {
            throw new IllegalArgumentException(processed + " exceeds total: "+total);
        }
        if(processed < 0)
        {
            throw new IllegalArgumentException("Negative number of processed: "+processed);
        }
        this.processed = processed;
    }
    
    /**
     * Returns a String representation of the progress
     * suitable as a message for printing to
     * the command line interface.
     * 
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        String processedString = Integer.toString(processed);
        int percentage = (int) (processed*100.0/total); 
        StringBuffer message = new StringBuffer();
        message.append("Processed: ");
        for(int i = 0; i < totalLength - processedString.length(); i++)
        {
            message.append(' ');
        }
        message.append(processed);
        message.append("  [");
        if(percentage < 100)
        {
            message.append(' ');
        }
        if(percentage < 10)
        {
            message.append(' ');
        }
        message.append(percentage);
        message.append("%].");
        return message.toString();
    }
}
