package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.Clusters;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Trivial pseudo-clustering that just considers all points to be one big cluster.
 * 
 * Useful for evaluation and testing.
 * 
 * @author Erich Schubert
 *
 * @param <O>
 */

public class TrivialAllInOne<O extends DatabaseObject> extends AbstractAlgorithm<O> {
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
    return new Description("TrivialAllInOne", "Trivial all-in-one clustering",
        "Returns a 'tivial' clustering which just considers all points to be one big cluster.", "");
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
    result = new Clusters<O>(rarray, database);
  }
}
