package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.util.*;

/**
 * Encapsulates a mapping of subspace dimensionalities to a list of set of ids forming a cluster
 * in a specific subspace dimension.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class SubspaceClusterMap {
  /**
   * The map holding the clusters.
   */
  private Map<Integer, List<Set<Integer>>> clusters;

  /**
   * The dimensionality of noise.
   */
  private int noiseDimensionality;

  /**
   * Provides a mapping of subspace dimensionalities to a list of set of ids forming a cluster
   * in a specific subspace dimension.
   *
   * @param noiseDimensionality the dimensionality of noise
   */
  public SubspaceClusterMap(int noiseDimensionality) {
    this.clusters = new HashMap<Integer, List<Set<Integer>>>();
    this.noiseDimensionality = noiseDimensionality;
  }

  /**
   * Adds a cluster with the specified subspace dimensionality and the
   * specified ids to this map.
   *
   * @param dimensionality the subspace dimensionality of the cluster
   * @param ids            the ids forming the cluster
   */
  public void add(Integer dimensionality, Set<Integer> ids) {
    List<Set<Integer>> cluster = clusters.get(dimensionality);
    if (cluster == null) {
      cluster = new ArrayList<Set<Integer>>();
      clusters.put(dimensionality, cluster);
    }
    cluster.add(ids);
  }

  /**
   * Adds the specified ids to noise.
   *
   * @param ids the ids forming the noise
   */
  public void addToNoise(Set<Integer> ids) {
    List<Set<Integer>> cluster = clusters.get(noiseDimensionality);
    if (cluster == null) {
      cluster = new ArrayList<Set<Integer>>();
      cluster.add(ids);
      clusters.put(noiseDimensionality, cluster);
    }
    else {
      cluster.get(0).addAll(ids);
    }
  }

  /**
   * Returns a set view of the subspace dimensionalities
   * contained in this cluster map.
   *
   * @return a set view of the subspace dimensionalities
   *         contained in this map
   */
  public List<Integer> subspaceDimensionalities() {
    List<Integer> dims = new ArrayList<Integer>(clusters.keySet());
    Collections.sort(dims);
    return dims;
  }

  /**
   * Returns the list of clusters to which this map maps the specified subspaceDimension.
   * @param subspaceDimension subspace dimension whose associated clusters are to be returned
   */
  public List<Set<Integer>> getCluster(Integer subspaceDimension) {
    return clusters.get(subspaceDimension);
  }

  /**
   * Returns the number of clusters (excl. noise) in this map.
   * @return the number of clusters (excl. noise) in this map
   */
  public int numClusters() {
    int result = 0;
    for (Integer d: clusters.keySet()) {
      if (d == noiseDimensionality) continue;
      List<Set<Integer>> clusters_d = clusters.get(d);
      result += clusters_d.size();
    }
    return result;
  }


}
