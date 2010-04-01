package de.lmu.ifi.dbs.elki.preprocessing;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Defines the requirements for classes that do some preprocessing steps for
 * objects of a certain database.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject handled by this Preprocessor
 * @param <D> the type of data returned by the preprocessor
 */
public interface Preprocessor<O extends DatabaseObject, D> {
  /**
   * This method executes the particular preprocessing step of this Preprocessor
   * for the objects of the specified database.
   * 
   * @param database the database for which the preprocessing is performed
   */
  void run(Database<O> database);
  
  /**
   * Get precomputed data for a given object ID.
   * 
   * @param id Object ID
   * @return precomputed data.
   */
  public D get(Integer id);
}
