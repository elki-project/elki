package de.lmu.ifi.dbs.elki.data.cluster.naming;

import de.lmu.ifi.dbs.elki.data.cluster.BaseCluster;

/**
 * Naming scheme implementation for clusterings.
 * 
 * @author Erich Schubert
 *
 */
public interface NamingScheme<C extends BaseCluster<C,?>> {
  /**
   * Retrieve a name for the given cluster.
   * 
   * @param cluster cluster to get a name for
   * @return cluster name
   */
  public String getNameForCluster(C cluster);
}
