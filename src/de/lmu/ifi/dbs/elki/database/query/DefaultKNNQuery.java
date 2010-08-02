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
    config = config.descend(this);
  }

  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database) {
    return new Instance<T, D>(database, distanceFunction.instantiate(database), k);
  }
  
  @Override
  public <T extends O> Instance<T, D> instantiate(Database<T> database, DistanceQuery<T, D> distanceQuery) {
    return new Instance<T, D>(database, distanceQuery, k);
  }

  /**
   * Instance of this query for a particular database.
   * 
   * @author Erich Schubert
   */
  public static class Instance<O extends DatabaseObject, D extends Distance<D>> extends AbstractKNNQuery.Instance<O, D> {
    /**
     * The query k
     */
    final int k;

    /**
     * Constructor.
     * 
     * @param database Database to query
     * @param distanceQuery Distance function to use
     */
    public Instance(Database<O> database, DistanceQuery<O, D> distanceQuery, int k) {
      super(database, distanceQuery);
      this.k = k;
    }

    @Override
    public List<DistanceResultPair<D>> get(DBID id) {
      return database.kNNQueryForID(id, k, distanceQuery);
    }
  }
}