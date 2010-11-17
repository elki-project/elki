package de.lmu.ifi.dbs.elki.database.query.range;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * A range query class that will try to use an index structure present in the database.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Database object type
 * @param <D> Distance type
 */
public class DatabaseRangeQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractDistanceRangeQuery<O, D> {
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public DatabaseRangeQuery(Parameterization config) {
    super(config);
    config = config.descend(this);
  }

  @Override
  public <T extends O> RangeQuery.Instance<T, D> instantiate(Database<T> database) {
    return database.getRangeQuery(distanceFunction, range);
  }
}