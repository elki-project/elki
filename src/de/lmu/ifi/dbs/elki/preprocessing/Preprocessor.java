package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Defines the requirements for classes that do some preprocessing steps for
 * objects of a certain database.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.uses Instance oneway - - «create»
 * 
 * @param <O> the minimal object type
 * @param <D> the type of data returned by the preprocessor
 */
public interface Preprocessor<O extends DatabaseObject, D> extends Parameterizable {
  /**
   * This method executes the particular preprocessing step of this Preprocessor
   * for the objects of the specified database.
   * 
   * @param database the database for which the preprocessing is performed
   */
  public <T extends O> Instance<D> instantiate(Database<T> database);

  /**
   * Interface for an instantiated preprocessor.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.landmark
   * 
   * @param <D> data result type
   */
  public static interface Instance<D> {
    /**
     * Get precomputed data for a given object ID.
     * 
     * @param id Object ID
     * @return precomputed data.
     */
    public D get(DBID id);
  }
}