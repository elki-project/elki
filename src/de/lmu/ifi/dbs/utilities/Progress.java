package de.lmu.ifi.dbs.utilities;

/**
 * TODO comment
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class Progress
{
    private int total;
    
    private int processed;
    
    public Progress(int total)
    {
        this.total = total;
    }
    
    public void setProcessed(int processed) throws IllegalArgumentException
    {
        if(processed > total)
        {
            throw new IllegalArgumentException(processed + " exceeds total: "+total);
        }
        this.processed = processed;
    }
    
    public String toString()
    {
        String totalString = Integer.toString(total);
        String processedString = Integer.toString(processed);
        int percentage = (int) (processed*100.0/total); 
        StringBuffer message = new StringBuffer();
        message.append("Processed: ");
        for(int i = 0; i < totalString.length() - processedString.length(); i++)
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
