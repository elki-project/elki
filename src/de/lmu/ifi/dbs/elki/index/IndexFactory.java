package de.lmu.ifi.dbs.elki.index;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Factory interface for indexes.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has de.lmu.ifi.dbs.elki.index.Index oneway - - produces
 *
 * @param <O> Object type
 * @param <I> Index type
 */
public interface IndexFactory<O extends DatabaseObject, I extends Index<O>> extends Parameterizable {
  /**
   * Sets the database in the distance function of this index (if existing).
   * 
   * @param database the database
   */
  public I instantiate(Database<O> database);
}
