package de.lmu.ifi.dbs.elki.data.model;

import java.util.BitSet;

/**
 * Simple model for Axis-Parallel Subspace Clusters.
 * Where the Subspace is modeled by a BitSet of dimensions.
 * 
 * @author Erich Schubert
 *
 */
public class AxesModel extends BaseModel {
  /**
   * Storage of BitSet for subspaces
   */
  private BitSet subspaces;

  /**
   * Constructor
   * 
   * @param subspaces Subspaces
   */
  public AxesModel(BitSet subspaces) {
    super();
    this.subspaces = subspaces;
  }

  /**
   * Access subspaces bitset.
   * 
   * @return bit set
   */
  public BitSet getSubspaces() {
    return subspaces;
  }

  /**
   * Access subspaces bitset.
   * 
   * @param subspaces
   */
  public void setSubspaces(BitSet subspaces) {
    this.subspaces = subspaces;
  }  
}
