package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Thrown by OptionHandler in case of incorrect parameter-array.
 */
public class NoParameterValueException extends RuntimeException
{
    /**
     * Thrown by OptionHandler in case of incorrect parameter-array.  
     */
    public NoParameterValueException()
    {
        super();
    }

    /**
     * Thrown by OptionHandler in case of incorrect parameter-array.
     * 
     * @param message
     */
    public NoParameterValueException(String message)
    {
        super(message);
    }

    /**
     * Thrown by OptionHandler in case of incorrect parameter-array.
     * 
     * @param message
     * @param cause
     */
    public NoParameterValueException(String message, Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Thrown by OptionHandler in case of incorrect parameter-array.
     * 
     * @param cause
     */
    public NoParameterValueException(Throwable cause)
    {
        super(cause);
    }
}
