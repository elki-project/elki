package de.lmu.ifi.dbs.database;

import de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is to provide a database.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public interface DatabaseConnection extends Parameterizable
{
    /**
     * Returns a Database according to parameter settings.
     * 
     * 
     * @return a Database according to parameter settings
     */
    Database getDatabase();
}
