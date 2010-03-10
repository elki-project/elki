package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Simple Interface for an heuristic to pick reference points.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public interface ReferencePointsHeuristic<O extends NumberVector<O,?>> {
  /**
   * Get the reference points for the given database.
   * 
   * @param db Database
   * @return Collection of reference points.
   */
  public Collection<O> getReferencePoints(Database<O> db);
}
