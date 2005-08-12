package de.lmu.ifi.dbs.parser;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.normalization.Normalization;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

import java.io.InputStream;

/**
 * A Parser shall provide a Database by parsing an InputStream.
 * 
 * The type of the provided Database should be set by parameters.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Parser extends Parameterizable
{
    /**
     * Returns a database containing those objects parsed
     * from the input stream.
     * 
     * If a normalization object is set,
     * the parsed objects should become normalized by it.
     * 
     * 
     * @param in the stream to parse objects from
     * @return a database containing those objects parsed
     * from the input stream
     */
    Database parse(InputStream in);
    
    /**
     * Sets the normalization object.
     * 
     * 
     * @param normalization the object to perform a normalization during
     * parsing
     */
    void setNormalization(Normalization normalization);
}
