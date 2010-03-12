package de.lmu.ifi.dbs.elki.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Result class for clusterings. Can be used for both hierarchical and
 * non-hierarchical clusterings.
 * 
 * The class does not enforce or rely on clusterings to be a tree or DAG,
 * instead they can be an arbitrary forest of directed graphs that COULD contain
 * cycles.
 * 
 * This class is NOT iterable for a simple reason: there is more than one method to do so.
 * You need to specify whether you want to use getToplevelClusters() or getAllClusters().
 * 
 * @author Erich Schubert
 * 
 * @param <M> Model type
 */
public class Clustering<M extends Model> implements Result {
  /**
   * Keep a list of top level clusters.
   */
  private List<Cluster<M>> toplevelclusters;

  /**
   * Constructor with a list of top level clusters
   * 
   * @param toplevelclusters Top level clusters
   */
  public Clustering(List<Cluster<M>> toplevelclusters) {
    super();
    this.toplevelclusters = toplevelclusters;
  }

  /**
   * Constructor for an empty clustering
   */
  public Clustering() {
    this(new ArrayList<Cluster<M>>());
  }

  /**
   * Add a cluster to the clustering.
   * 
   * @param n new cluster
   */
  public void addCluster(Cluster<M> n) {
    toplevelclusters.add(n);
  }

  /**
   * Return top level clusters
   * 
   * @return top level clusters
   */
  public List<Cluster<M>> getToplevelClusters() {
    return toplevelclusters;
  }

  /**
   * Collect all clusters (recursively) into a List.
   * 
   * @return List of all clusters.
   */
  public List<Cluster<M>> getAllClusters() {
    Set<Cluster<M>> clu = new HashSet<Cluster<M>>();
    for(Cluster<M> rc : toplevelclusters) {
      if(!clu.contains(rc)) {
        clu.add(rc);
        clu = rc.getDescendants(clu);
      }
    }
    // Note: we canNOT use TreeSet above, because this comparator is only
    // partial!
    ArrayList<Cluster<M>> res = new ArrayList<Cluster<M>>(clu);
    Collections.sort(res, new Cluster.PartialComparator());
    return res;
  }

  @Override
  public String getName() {
    return "clustering";
  }
}
