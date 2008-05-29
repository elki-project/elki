package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.data.DatabaseObject;

/**
 * Interface for Algorithms that are capable to provide a
 * {@link ClusteringResult ClusteringResult}.
 *
 * @author Arthur Zimek
 * @param <O> the type of DatabaseObject handled by this Clustering
 */
public interface Clustering<O extends DatabaseObject> extends Algorithm<O> {
    /**
     * @see Algorithm#getResult()
     */
    ClusteringResult<O> getResult();
}
