package de.lmu.ifi.dbs.elki.index.tree.spatial;

import java.util.List;

import de.lmu.ifi.dbs.elki.index.tree.IndexTree;
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
   * Constructor.
   * 
   * @param pagefile Page file
   */
  public SpatialIndexTree(PageFile<N> pagefile) {
    super(pagefile);
  }

  /**
   * Add a new leaf entry to the tree.
   * 
   * @param leaf Leaf entry
   */
  public abstract void insertLeaf(E leaf);

  /**
   * Returns a list of entries pointing to the leaf entries of this spatial
   * index.
   * 
   * @return a list of entries pointing to the leaf entries of this spatial
   *         index
   */
  public abstract List<E> getLeaves();
}