package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;

/**
 * Strategy to use the complete database as reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type.
 */
public class FullDatabaseReferencePoints<O extends NumberVector<? extends O, ?>> implements ReferencePointsHeuristic<O> {
  /**
   * Constructor, Parameterizable style.
   */
  public FullDatabaseReferencePoints() {
    super();
  }

  @Override
  public <T extends O> Collection<O> getReferencePoints(Database<T> db) {
    return new DatabaseUtil.CollectionFromDatabase<O>(db);
  }
}