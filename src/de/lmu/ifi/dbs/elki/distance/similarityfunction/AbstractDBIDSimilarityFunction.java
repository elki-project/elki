package de.lmu.ifi.dbs.elki.distance.similarityfunction;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * 
 * @param <D> distance type
 */
public abstract class AbstractDBIDSimilarityFunction<D extends Distance<D>> extends AbstractPrimitiveSimilarityFunction<DBID, D> implements DBIDSimilarityFunction<D> {
  /**
   * The database we work on
   */
  protected Relation<? extends DBID> database;

  /**
   * Constructor.
   * 
   * @param database Database
   */
  public AbstractDBIDSimilarityFunction(Relation<? extends DBID> database) {
    super();
    this.database = database;
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }
}