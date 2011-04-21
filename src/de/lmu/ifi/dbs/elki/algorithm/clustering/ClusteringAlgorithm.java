package de.lmu.ifi.dbs.elki.algorithm.clustering;

import de.lmu.ifi.dbs.elki.algorithm.Algorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Interface for Algorithms that are capable to provide a {@link Clustering
 * Clustering} as Result. in general, clustering algorithms are supposed to
 * implement the {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface. The
 * more specialized interface {@link ClusteringAlgorithm} requires an
 * implementing algorithm to provide a special result class suitable as a
 * partitioning of the database. More relaxed clustering algorithms are allowed
 * to provide a result that is a fuzzy clustering, does not partition the
 * database complete or is in any other sense a relaxed clustering result.
 * 
 * @author Arthur Zimek
 * 
 * @apiviz.uses Clustering
 * @apiviz.uses Model
 * 
 * @param <C> Clustering type
 */
public interface ClusteringAlgorithm<C extends Clustering<? extends Model>> extends Algorithm {
  @Override
  C run(Database database) throws IllegalStateException;
}