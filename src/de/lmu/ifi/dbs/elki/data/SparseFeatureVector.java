package de.lmu.ifi.dbs.elki.data;

import java.util.BitSet;

/**
 * Extended interface for sparse feature vector types
 * 
 * @author Erich Schubert
 *
 * @param <V> Vector type (self-reference
 * @param <D> Data type
 */
public interface SparseFeatureVector<V extends SparseFeatureVector<V, D>, D> extends FeatureVector<V, D> {
  /**
   * Bit set of non-null features
   * 
   * @return Bit set
   */
  public BitSet getNotNullMask();
}
