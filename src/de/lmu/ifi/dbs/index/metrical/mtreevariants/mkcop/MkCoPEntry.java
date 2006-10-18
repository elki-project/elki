package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkcop;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.MTreeEntry;

/**
 * Defines the requirements for an entry in an MkCop-Tree node.
 * Additionally to an entry in an M-Tree conservative approximation of the
 * knn distances is provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface MkCoPEntry<D extends NumberDistance<D>> extends MTreeEntry<D> {

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k                the parameter k of the knn distance
   * @param distanceFunction the distance function
   * @return the conservative approximated knn distance of the entry
   */
  public <O extends DatabaseObject> D approximateConservativeKnnDistance(int k, DistanceFunction<O, D> distanceFunction);

  /**
   * Returns the conservative approximation line.
   *
   * @return the conservative approximation line
   */
  public ApproximationLine getConservativeKnnDistanceApproximation();

  /**
   * Sets the conservative approximation line
   *
   * @param conservativeApproximation the conservative approximation line to be set
   */
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation);
}
