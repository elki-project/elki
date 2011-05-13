package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndex;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;

/**
 * Abstract super class for all spatial index classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.has SpatialNode oneway - - contains
 * 
 * @param <O> Vector type
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class SpatialIndex<O extends SpatialComparable, N extends SpatialNode<N, E>, E extends SpatialEntry> extends TreeIndex<O, N, E> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * If true, a bulk load will be performed.
   */
  protected boolean bulk;

  /**
   * The strategy for bulk load.
   */
  protected BulkSplit.Strategy bulkLoadStrategy;

  /**
   * Constructor.
   * 
   * @param relation Relation indexed
   * @param fileName file name
   * @param pageSize page size
   * @param cacheSize cache size
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   */
  public SpatialIndex(Relation<O> relation, String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy) {
    super(relation, fileName, pageSize, cacheSize);
    this.bulk = bulk;
    this.bulkLoadStrategy = bulkLoadStrategy;
  }

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}