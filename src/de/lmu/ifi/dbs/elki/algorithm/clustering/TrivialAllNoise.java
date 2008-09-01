package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Trivial pseudo-clustering that just considers all points to be noise.
 * 
 * Useful for evaluation and testing.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */
public class TrivialAllNoise<O extends DatabaseObject> extends AbstractAlgorithm<O> implements Clustering<O> {
  /**
   * Holds the result of the algorithm.
   */
  private ClusteringResult<O> result;

  /**
   * Return clustering result
   */
  public ClusteringResult<O> getResult() {
    return result;
  }

  /**
   * Obtain a description of the algorithm
   */
  public Description getDescription() {
    return new Description("TrivialAllNoise", "Trivial all-noise clustering",
        "Returns a 'trivial' clustering which just considers all points as noise points.", "");
  }

  /**
   * Run the actual clustering algorithm.
   * 
   * @param database The database to process
   */
  @Override
  protected void runInTime(Database<O> database) throws IllegalStateException {
    Integer[][] rarray = new Integer[1][];
    rarray[0] = database.getIDs().toArray(new Integer[0]);
    result = new ClustersPlusNoise<O>(rarray, database);
  }
}
