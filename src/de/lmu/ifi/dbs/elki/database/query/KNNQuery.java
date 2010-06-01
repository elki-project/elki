package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Interface to abstract a kNN query (with fixed k!).
 * This allows replacing the kNN query with approximations or the use of preprocessors.
 * 
 * @author Erich Schubert
 *
 * @param <O> Database object type
 * @param <D> Distance
 */
public interface KNNQuery<O extends DatabaseObject, D extends Distance<D>> extends Parameterizable {
  /**
   * OptionID for the 'k' parameter
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("materialize.k", "The number of nearest neighbors of an object to be materialized.");

  /**
   * OptionID for the distance function
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("materialize.distance", "the distance function to materialize the nearest neighbors");

  /**
   * Set the database to use.
   * 
   * @param database Database
   */
  public void setDatabase(Database<O> database);
  
  /**
   * Get the k nearest neighbors for a particular id.
   * 
   * @param id query object ID
   * @return neighbors
   */
  public List<DistanceResultPair<D>> get(DBID id);
}
