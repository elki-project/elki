package de.lmu.ifi.dbs.elki.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Result class for clusterings. Can be used for both hierarchical and non-hierarchical clusterings.
 * 
 * The class does not enforce or rely on clusterings to be a tree or DAG, instead they can be an arbitrary
 * forest of directed graphs that COULD contain cycles.
 * 
 * @author Erich Schubert
 *
 * @param <C> Cluster type
 */
public class Clustering<C extends BaseCluster<C, ?>> implements Result {
  /**
   * Keep a list of top level clusters.
   */
  private List<C> toplevelclusters;

  /**
   * Constructor with a list of top level clusters
   * 
   * @param toplevelclusters Top level clusters
   */
  public Clustering(List<C> toplevelclusters) {
    super();
    this.toplevelclusters = toplevelclusters;
  }

  /**
   * Constructor for an empty clustering
   */
  public Clustering() {
    this(new ArrayList<C>());
  }

  /**
   * Add a cluster to the clustering.
   * 
   * @param n new cluster
   */
  public void addCluster(C n) {
    toplevelclusters.add(n);
  }

  /**
   * Return top level clusters
   * 
   * @return top level clusters
   */
  public List<C> getToplevelClusters() {
    return toplevelclusters;
  }

  /**
   * Collect all clusters (recursively) into a Set.
   * 
   * @return Set of all clusters.
   */
  public Set<C> getAllClusters() {
    Set<C> clu = new HashSet<C>();
    for(C rc : toplevelclusters)
      if(!clu.contains(rc)) {
        clu.add(rc);
        clu = rc.getDescendants(clu);
      }
    return clu;
  }

  /**
   * Compute labels for the model, based on getSuggestedLabel()
   */
  public void updateLabels() {
    // keep track on how often each label has been used so far.
    HashMap<String, Integer> labelcount = new HashMap<String, Integer>();
    for(C clus : getAllClusters()) {
      if(clus.getName() == null) {
        String label = clus.getSuggestedLabel();
        Integer count = labelcount.get(label);
        if(count == null) {
          count = new Integer(0);
        }
        else {
          count++;
        }
        labelcount.put(label, count);

        clus.setName(label + " " + count);
      }
    }
    // Remove the " 0" postfix for labels that occurred only once.
    for(Entry<String, Integer> entry : labelcount.entrySet()) {
      if(entry.getValue().intValue() == 0) {
        for(C clus : getAllClusters()) {
          if(clus.getName().equals(entry.getKey() + " " + 0)) {
            clus.setName(entry.getKey());
          }
        }
      }
    }
  }
}
