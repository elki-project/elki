package de.lmu.ifi.dbs.normalization;

/**
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class NonNumericFeaturesException extends Exception
{

    /**
     * 
     */
    public NonNumericFeaturesException()
    {
        super();
    }

    /**
     * 
     * @param message
     */
    public NonNumericFeaturesException(String message)
    {
        super(message);
    }

    /**
     * @param cause
     */
    public NonNumericFeaturesException(Throwable cause)
    {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public NonNumericFeaturesException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
