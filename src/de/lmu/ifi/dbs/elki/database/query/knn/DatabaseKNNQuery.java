package de.lmu.ifi.dbs.elki.database.query.knn;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A KNN query class that will try to use an index structure present in the database.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class DatabaseKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DatabaseKNNQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public <T extends O> KNNQuery.Instance<T, D> instantiate(Database<T> database) {
    return database.getKNNQuery(distanceFunction, k);
  }
}