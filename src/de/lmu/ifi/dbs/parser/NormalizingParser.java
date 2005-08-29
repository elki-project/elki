package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.normalization.Normalization;

/**
 * A parser able to perform a normalization during parsing.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class NormalizingParser<T extends FeatureVector> extends AbstractParser<T>
{
    protected Normalization<T> normalization;
    
    /**
     * Sets the normalization object.
     * 
     * 
     * @param normalization the object to perform a normalization during
     * parsing
     */
    public void setNormalization(Normalization<T> normalization)
    {
        this.normalization = normalization;
    }
    
    /**
     * Returns the Normalization object.
     * 
     * 
     * @return the Normalization object
     */
    public Normalization<T> getNormalization()
    {
        return normalization;
    }
}
