package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <O> object type
 * @param <D> distance type
 */
public abstract class AbstractDatabaseSimilarityFunction<O extends DatabaseObject, D extends Distance<D>> extends AbstractSimilarityFunction<O, D> implements DatabaseSimilarityFunction<O, D> {
  /**
   * The database we work on
   */
  protected Database<O> database;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDatabaseSimilarityFunction(Database<O> database) {
    super();
    this.database = database;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}