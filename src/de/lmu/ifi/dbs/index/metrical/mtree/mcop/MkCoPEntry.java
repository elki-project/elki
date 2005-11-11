package de.lmu.ifi.dbs.index.metrical.mtree.mcop;

import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.index.metrical.mtree.Entry;

/**
 * Defines the requirements for an entry in a MCop-Tree node. Additionally to an entry in a M-Tree
 * getter and setter methods for the knn distances are provided.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */

interface MkCoPEntry extends Entry<DoubleDistance> {

  /**
   * Returns the conservative approximated knn distance of the entry.
   *
   * @param k the parameter k of the knn distance
   * @return the conservative approximated knn distance of the entry
   */
  public DoubleDistance approximateConservativeKnnDistance(int k);

  /**
   * Returns the progressive approximated knn distance of the entry.
   *
   * @param k the parameter k of the knn distance
   * @return the progressive approximated knn distance of the entry
   */
  public DoubleDistance approximateProgressiveKnnDistance(int k);

  /**
   * Returns the parameter k.
   *
   * @return the parameter k
   */
  public int getK();

  /**
   * Returns the conservative approximation line.
   *
   * @return the conservative approximation line
   */
  public ApproximationLine getConservativeKnnDistanceApproximation();

  /**
   * Returns the progressive approximation line.
   *
   * @return the progressive approximation line
   */
  public ApproximationLine getProgressiveKnnDistanceApproximation();

  /**
   * Sets the conservative approximation line
   *
   * @param conservativeApproximation the conservative approximation line to be set
   */
  public void setConservativeKnnDistanceApproximation(ApproximationLine conservativeApproximation);

  /**
   * Sets the progressive approximation line
   *
   * @param progressiveApproximation the progressive approximation line to be set
   */
  public void setProgressiveKnnDistanceApproximation(ApproximationLine progressiveApproximation);
}
