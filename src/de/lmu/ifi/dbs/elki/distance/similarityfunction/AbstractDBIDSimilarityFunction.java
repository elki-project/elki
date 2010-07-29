package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <D> distance type
 */
public abstract class AbstractDBIDSimilarityFunction<D extends Distance<D>> extends AbstractPrimitiveSimilarityFunction<DatabaseObject, D> implements DBIDSimilarityFunction<D> {
  /**
   * The database we work on
   */
  protected Database<? extends DatabaseObject> database;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDBIDSimilarityFunction(Database<? extends DatabaseObject> database) {
    super();
    this.database = database;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}