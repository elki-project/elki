package de.lmu.ifi.dbs.elki.database.connection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.normalization.Normalization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is to provide a database. <p/> A database connection is to
 * manage the input and to provide a database where algorithms can run on. An
 * implementation may either use a parser to parse a sequential file or piped
 * input and provide a file based database or provide an intermediate connection
 * to a database system.
 * 
 * @author Arthur Zimek 
 */
public interface DatabaseConnection<O extends DatabaseObject> extends
        Parameterizable
{
    /**
     * Property key for available parsers.
     */
    public static final String PROPERTY_PARSER = "PARSER";

    /**
     * Property key for available databases.
     */
    public static final String PROPERTY_DATABASE = "DATABASE";

    /**
     * Returns a Database according to parameter settings.
     * 
     * @param normalization
     *            Normalization to perform a normalization if this action is
     *            supported. May remain null.
     * @return a Database according to parameter settings
     */
    Database<O> getDatabase(Normalization<O> normalization);
}
