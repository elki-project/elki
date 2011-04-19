package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is to provide a database.
 * <p/>
 * A database connection is to manage the input and to provide a database where
 * algorithms can run on. An implementation may either use a parser to parse a
 * sequential file or piped input and provide a file based database or provide
 * an intermediate connection to a database system.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses Database oneway - - «create»
 */
public interface DatabaseConnection extends Parameterizable {
  /**
   * Returns a Database according to parameter settings.
   * 
   * @return a Database according to parameter settings
   */
  Database getDatabase();
}