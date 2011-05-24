package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;

/**
 * Interface for OPTICS type algorithms, that can be analysed by OPTICS Xi etc.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ClusterOrderResult
 * 
 * @param <D> Distance type
 */
public interface OPTICSTypeAlgorithm<D extends Distance<D>> extends Algorithm {
  @Override
  ClusterOrderResult<D> run(Database database) throws IllegalStateException;
  
  /**
   * Get the minpts value used. Needed for OPTICS Xi etc.
   * 
   * @return minpts value
   */
  public int getMinPts();

  /**
   * Get the distance factory. Needed for type checking (i.e. is number distance)
   * 
   * @return distance factory
   */
  public D getDistanceFactory();
}