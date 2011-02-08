package de.lmu.ifi.dbs.elki.data;


/**
 * Combines the SparseFeatureVector and NumberVector
 * 
 * @author Erich Schubert
 *
 * @param <V>
 * @param <N>
 */
public interface SparseNumberVector<V extends SparseNumberVector<V, N>, N extends Number> extends NumberVector<V, N>, SparseFeatureVector<V, N> {
  // Empty combination interface
}
