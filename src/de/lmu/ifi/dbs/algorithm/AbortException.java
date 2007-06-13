package de.lmu.ifi.dbs.algorithm;

/**
 * Exception for aborting some process and transporting a message.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class AbortException extends RuntimeException
{

    /**
     * 
     */
    private static final long serialVersionUID = -2248437536321126746L;

    /**
     * Exception for aborting some process and transporting a message.
     * 
     * @param message
     *            message to be transported
     */
    public AbortException(String message)
    {
        super(message);
    }

}
