package de.lmu.ifi.dbs.parser;

import java.io.InputStream;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * A Parser shall provide a Database by parsing an InputStream.
 * 
 * The type of the provided Database should be set by parameters.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface Parser extends Parameterizable
{
    Database parse(InputStream in);
}
