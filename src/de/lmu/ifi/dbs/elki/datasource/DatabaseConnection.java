package de.lmu.ifi.dbs.elki.datasource;

import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * DatabaseConnection is used to load data into a database.
 * <p/>
 * A database connection is to manage the input and for a database where
 * algorithms can run on. An implementation may either use a parser to parse a
 * sequential file or piped input and provide a file based database or provide
 * an intermediate connection to a database system.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses MultipleObjectsBundle
 */
public interface DatabaseConnection extends Parameterizable {
  /**
   * Returns the initial data for a database.
   * 
   * @return a database object bundle
   */
  // TODO: streaming load?
  MultipleObjectsBundle loadData();
}