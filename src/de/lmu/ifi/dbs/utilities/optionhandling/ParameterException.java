package de.lmu.ifi.dbs.utilities.optionhandling;

/**
 * Abstract super class for all exceptions thrown during parmeterization.
 * 
 * @author Elke Achtert
 */
public abstract class ParameterException extends Exception
{
    /**
     * @see Exception#Exception(String)
     */
    protected ParameterException(String message)
    {
        super(message);
    }

    /**
     * @see Exception#Exception(String, Throwable)
     */
    protected ParameterException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
