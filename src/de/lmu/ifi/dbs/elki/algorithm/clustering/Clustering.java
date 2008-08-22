package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;

/**
 * Interface for Algorithms that are capable to provide a
 * {@link ClusteringResult ClusteringResult}.
 * in general, clustering algorithms are supposed to implement the {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface.
 * The more specialized interface {@link de.lmu.ifi.dbs.elki.algorithm.clustering.Clustering}
 * requires an implementing algorithm to provide a special result class suitable as a partitioning of the database.
 * More relaxed clustering algorithms are allowed to provide a result that is a fuzzy clustering, does not
 * partition the database complete or is in any other sense a relaxed clustering result.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject handled by this Clustering
 */
public interface Clustering<O extends DatabaseObject> extends Algorithm<O> {
    ClusteringResult<O> getResult();
}
