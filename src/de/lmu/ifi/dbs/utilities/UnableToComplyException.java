package de.lmu.ifi.dbs.utilities;

/**
 * General Exception to state inability to execute an operation.
 * 
 * @author Arthur Zimek
 */
public class UnableToComplyException extends Exception
{
    /**
     * Exception to state inability to execute an operation.
     *
     */
    public UnableToComplyException()
    {
        super();
    }

    /**
     * Exception to state inability to execute an operation.
     * @param message a message to describe cause of exception
     */
    public UnableToComplyException(String message)
    {
        super(message);
    }

    /**
     * Exception to state inability to execute an operation.
     * @param message a message to describe cause of exception
     * @param cause cause of exception
     */
    public UnableToComplyException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Exception to state inability to execute an operation.
     * 
     * @param cause cause of exception
     */
    public UnableToComplyException(Throwable cause)
    {
        super(cause);
    }

}
