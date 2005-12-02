package de.lmu.ifi.dbs.normalization;

/**
 * An exception to signal the encounter of non numeric features where numeric features have been expected.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class NonNumericFeaturesException extends Exception
{

    
    /**
     * Generated serial version UID.
     */
    private static final long serialVersionUID = 284302959521511627L;

    /**
     * An exception to signal the encounter of non numeric features where numeric features have been expected.
     * 
     * @see Exception
     */
    public NonNumericFeaturesException()
    {
        super();
    }

    /**
     * An exception to signal the encounter of non numeric features where numeric features have been expected.
     * 
     * @see Exception
     */
    public NonNumericFeaturesException(String message)
    {
        super(message);
    }

    /**
     * An exception to signal the encounter of non numeric features where numeric features have been expected.
     * 
     * @see Exception
     */
    public NonNumericFeaturesException(Throwable cause)
    {
        super(cause);
    }

    /**
     * An exception to signal the encounter of non numeric features where numeric features have been expected.
     * 
     * @see Exception
     */
    public NonNumericFeaturesException(String message, Throwable cause)
    {
        super(message, cause);
    }

}
