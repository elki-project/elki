package de.lmu.ifi.dbs.elki.data.cluster.naming;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;

/**
 * Simple enumerating naming scheme. Cluster names are generated as follows: -
 * if the cluster has a name assigned, use it - otherwise use getNameAutomatic()
 * as name, and add an enumeration postfix
 * 
 * @author Erich Schubert
 * 
 */
public class SimpleEnumeratingScheme implements NamingScheme {
  /**
   * Clustering this scheme is applied to.
   */
  private Clustering<?> clustering;

  /**
   * count how often each name occurred so far.
   */
  private Map<String, Integer> namecount = new HashMap<String, Integer>();

  /**
   * Assigned cluster names.
   */
  private Map<Cluster<?>, String> names = new HashMap<Cluster<?>, String>();

  /**
   * This is the postfix added to the first cluster, which will be removed when
   * there is only one cluster of this name.
   */
  private final static String nullpostfix = " " + Integer.toString(0);

  /**
   * Constructor.
   * 
   * @param clustering Clustering result to name.
   */
  public SimpleEnumeratingScheme(Clustering<?> clustering) {
    super();
    this.clustering = clustering;
  }

  /**
   * Assign names to each cluster (which doesn't have a name yet)
   */
  private void updateNames() {
    for(Cluster<?> cluster : clustering.getAllClusters()) {
      if(names.get(cluster) == null) {
        String sugname = cluster.getNameAutomatic();
        Integer count = namecount.get(sugname);
        if(count == null) {
          count = new Integer(0);
        }
        names.put(cluster, sugname + " " + count.toString());
        count++;
        namecount.put(sugname, count);
      }
    }
  }

  /**
   * Retrieve the cluster name. When a name has not yet been assigned, call
   * {@link #updateNames}
   */
  @Override
  public String getNameFor(Object o) {
    if(o instanceof Cluster<?>) {
      try {
        Cluster<?> cluster = (Cluster<?>) o;
        String nam = names.get(cluster);
        if(nam == null) {
          updateNames();
          nam = names.get(cluster);
        }
        if(nam.endsWith(nullpostfix)) {
          if(namecount.get(nam.substring(0, nam.length() - nullpostfix.length())) == 1) {
            nam = nam.substring(0, nam.length() - nullpostfix.length());
          }
        }
        return nam;
      }
      catch(ClassCastException e) {
        return null;
      }
    }
    return null;
  }
}