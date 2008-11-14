package de.lmu.ifi.dbs.elki.data.cluster.naming;

import java.util.HashMap;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.Model;

public class SimpleEnumeratingScheme implements NamingScheme {
  private Clustering<Cluster<Model>> clustering;

  private Map<String, Integer> namecount = new HashMap<String, Integer>();

  private Map<Cluster<Model>, String> names = new HashMap<Cluster<Model>, String>();

  private final static String nullpostfix = " " + Integer.toString(0);

  @SuppressWarnings("unchecked")
  public SimpleEnumeratingScheme(Clustering<?> clustering) {
    super();
    this.clustering = (Clustering<Cluster<Model>>) clustering;
  }

  private void updateNames() {
    for(Cluster<Model> cluster : clustering.getAllClusters()) {
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

  @Override
  public String getNameForCluster(BaseCluster<?,?> cluster) {
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
}