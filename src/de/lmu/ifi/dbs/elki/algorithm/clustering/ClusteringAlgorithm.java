package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface for Algorithms that are capable to provide a {@link Clustering Clustering} as Result.
 * in general, clustering algorithms are supposed to implement the {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface.
 * The more specialized interface {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}
 * requires an implementing algorithm to provide a special result class suitable as a partitioning of the database.
 * More relaxed clustering algorithms are allowed to provide a result that is a fuzzy clustering, does not
 * partition the database complete or is in any other sense a relaxed clustering result.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject handled by this Clustering
 */
public interface ClusteringAlgorithm<C extends Clustering<? extends Model>, O extends DatabaseObject> extends Algorithm<O, C> {
  /**
   * Runs the algorithm.
   *
   * @param database the database to run the algorithm on
   * @throws IllegalStateException if the algorithm has not been initialized
   *                               properly (e.g. the setParameters(String[]) method has been failed
   *                               to be called).
   */
  C run(Database<O> database) throws IllegalStateException;

  /**
   * Retrieve the result.
   */
  C getResult();
}
