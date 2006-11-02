package de.lmu.ifi.dbs.algorithm.result.clustering;

import java.util.*;

/**
 * Encapsulates a mapping of correlation dimensionalities to a list of set of ids forming a cluster
 * in a specific correlation dimension.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class CorrelationClusterMap {
  /**
   * The map holding the clusters.
   */
  private Map<Integer, List<Set<Integer>>> clusters;

  /**
   * The dimensionality of noise.
   */
  private int noiseDimensionality;

  /**
   * Provides a mapping of correlation dimensionalities to a list of set of ids forming a cluster
   * in a specific correlation dimension.
   *
   * @param noiseDimensionality the dimensionality of noise
   */
  public CorrelationClusterMap(int noiseDimensionality) {
    this.clusters = new HashMap<Integer, List<Set<Integer>>>();
    this.noiseDimensionality = noiseDimensionality;
  }

  /**
   * Adds a cluster with the specified correlation dimensionality and the
   * specified ids to this map.
   *
   * @param dimensionality the correlation dimensionality of the cluster
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
   * Returns a set view of the correlation dimensionalities
   * contained in this cluster map.
   *
   * @return a set view of the correlation dimensionalities
   *         contained in this map
   */
  public Set<Integer> correlationDimensionalities() {
    return clusters.keySet();
  }

  /**
   * Returns the list of value to which this map maps the specified key.
   */
  public List<Set<Integer>> getCluster(Integer correlationDimension) {
    return clusters.get(correlationDimension);
  }


}
