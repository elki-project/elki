package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;

/**
 * Distance function relying on an index (such as preprocessed neighborhoods).
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.stereotype factory
 * @apiviz.has Instance oneway - - «create»
 */
public interface IndexBasedDistanceFunction<O extends DatabaseObject, D extends Distance<D>> extends DistanceFunction<O, D> {
  /**
   * OptionID for the index parameter
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("distancefunction.index", "Distance index to use.");

  /**
   * Instance interface for Index based distance functions.
   * 
   * @author Erich Schubert
   * 
   * @param <T> Object type
   * @param <D> Distance type
   */
  public static interface Instance<T extends DatabaseObject, I extends Index<T>, D extends Distance<D>> extends DistanceQuery<T, D> {
    /**
     * Get the index used.
     * 
     * @return the index used
     */
    public I getIndex();
  }
}
