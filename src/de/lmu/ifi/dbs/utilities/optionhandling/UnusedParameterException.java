package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by OptionHandler in case of request of an unused parameter.
 */
public class UnusedParameterException extends RuntimeException
{
    /**
     * Thrown by OptionHandler in case of request of an unused parameter. 
     */
    public UnusedParameterException()
    {
        super();
    }
    
    /**
     * Thrown by OptionHandler in case of request of an unused parameter.
     * 
     * @param message
     */
    public UnusedParameterException(String message)
    {
        super(message);
    }
    
    /**
     * Thrown by OptionHandler in case of request of an unused parameter.
     * 
     * @param message
     * @param cause
     */
    public UnusedParameterException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Thrown by OptionHandler in case of request of an unused parameter.
     * 
     * @param cause
     */
    public UnusedParameterException(Throwable cause)
    {
        super(cause);
    }
}
