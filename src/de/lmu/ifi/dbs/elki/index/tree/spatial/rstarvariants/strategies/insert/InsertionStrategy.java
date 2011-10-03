package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.strategies.insert;

import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
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
   * @param obj Insertion object
   * @param height Tree height
   * @param depth Insertion depth (depth == height - 1 indicates leaf level)
   * @return Subtree index in array.
   */
  public <A> int choose(A options, ArrayAdapter<? extends SpatialComparable, A> getter, SpatialComparable obj, int height, int depth);
}