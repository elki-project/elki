/**
 * <p>Package collects clustering algorithms.</p>
 * 
 * Clustering algorithms are supposed to implement the {@link de.lmu.ifi.dbs.elki.algorithm.Algorithm}-Interface.
 * The more specialized interface {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}
 * requires an implementing algorithm to provide a special result class suitable as a partitioning of the database.
 * More relaxed clustering algorithms are allowed to provide a result that is a fuzzy clustering, does not
 * partition the database complete or is in any other sense a relaxed clustering result.
 * 
 * @see de.lmu.ifi.dbs.elki.algorithm
 */
package de.lmu.ifi.dbs.elki.algorithm.clustering;