package de.lmu.ifi.dbs.elki.utilities.referencepoints;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Simple Interface for an heuristic to pick reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 */
public interface ReferencePointsHeuristic<O> extends Parameterizable {
  /**
   * Get the reference points for the given database.
   * 
   * @param db Database
   * @return Collection of reference points.
   */
  public <T extends O> Collection<O> getReferencePoints(Relation<T> db);
}