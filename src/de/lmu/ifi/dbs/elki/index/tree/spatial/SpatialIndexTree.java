package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.index.tree.IndexTree;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.persistent.PageFile;

/**
 * Abstract super class for all spatial index tree classes.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.has SpatialNode oneway - - contains
 * 
 * @param <N> Node type
 * @param <E> Entry type
 */
public abstract class SpatialIndexTree<N extends SpatialNode<N, E>, E extends SpatialEntry> extends IndexTree<N, E> {
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
   * @param pagefile Page file
   * @param bulk bulk flag
   * @param bulkLoadStrategy bulk load strategy
   */
  public SpatialIndexTree(PageFile<N> pagefile, boolean bulk, Strategy bulkLoadStrategy) {
    super(pagefile);
    this.bulk = bulk;
    this.bulkLoadStrategy = bulkLoadStrategy;
  }
  
  /**
   * Add a new leaf to the tree.
   * 
   * @param leaf Leaf
   */
  public abstract void insertLeaf(E leaf);

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   * 
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  public abstract List<E> getLeaves();
}