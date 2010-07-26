package de.lmu.ifi.dbs.elki.database.query;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Default (on-demand) KNN query class.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class DefaultKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DefaultKNNQuery(Parameterization config) {
    super(config);
  }

  @Override
  public <T extends O> Instance<T> instantiate(Database<T> database) {
    return new Instance<T>(database, distanceFunction.instantiate(database));
  }

  /**
   * Instance of this query for a particular database.
   * 
   * @author Erich Schubert
   */
  public class Instance<T extends O> extends AbstractKNNQuery<O, D>.Instance<T> {
    /**
     * Constructor.
     * 
     * @param database Database to query
     * @param distanceQuery Distance function to use
     */
    public Instance(Database<T> database, DistanceQuery<T, D> distanceQuery) {
      super(database, distanceQuery);
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      return database.kNNQueryForID(id, k, distanceQuery);
    }
  }
}