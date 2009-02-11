package de.lmu.ifi.dbs.elki.data.cluster.naming;

/**
 * Naming scheme implementation for clusterings.
 * 
 * @author Erich Schubert
 *
 */
public interface NamingScheme {
  /**
   * Retrieve a name for the given cluster.
   * 
   * @param cluster cluster to get a name for
   * @return cluster name
   */
  public String getNameFor(Object cluster);
}
