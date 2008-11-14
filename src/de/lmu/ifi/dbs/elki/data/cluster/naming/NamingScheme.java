package de.lmu.ifi.dbs.elki.data.cluster.naming;

import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;

public interface NamingScheme {
  public String getNameForCluster(BaseCluster<?,?> cluster);
}
