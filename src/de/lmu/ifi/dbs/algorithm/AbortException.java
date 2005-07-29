package de.lmu.ifi.dbs.algorithm;

/**
 * Exception for aborting some process and transporting a message.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
@SuppressWarnings("serial")
public class AbortException extends RuntimeException
{

    /**
     * Exception for aborting some process and transporting a message.
     * 
     * @param message message to be transported
     */
    public AbortException(String message)
    {
        super(message);
    }

}
