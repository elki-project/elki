package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialAdapter;
import de.lmu.ifi.dbs.elki.utilities.datastructures.ArrayAdapter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * RTree insertion strategy interface.
 * 
 * @author Erich Schubert
 */
public interface InsertionStrategy extends Parameterizable {
  /**
   * Choose insertion rectangle.
   * 
   * @param options Options to choose from
   * @param getter Array adapter for options
   * @param a1 Spatial adapter for options
   * @param obj Insertion object
   * @param a2 Spatial adapter for insertion object
   * @param leaf Choose at leaf height.
   * @return Subtree index in array.
   */
  public <E, I, A> int choose(A options, ArrayAdapter<E, A> getter, SpatialAdapter<? super E> a1, I obj, SpatialAdapter<? super I> a2, boolean leaf);
}