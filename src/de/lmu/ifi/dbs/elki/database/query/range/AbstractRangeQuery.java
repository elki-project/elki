package de.lmu.ifi.dbs.elki.database.query.range;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract base class for range query objects.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public abstract class AbstractRangeQuery<O extends DatabaseObject, D extends Distance<D>> implements RangeQuery<O, D> {
  /**
   * Holds the range value
   */
  protected D range;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractRangeQuery(Parameterization config) {
    super();
    config = config.descend(this);
    // Range value?
  }
  
  @Override
  public abstract <T extends O> RangeQuery.Instance<T, D> instantiate(Database<T> database);

  /**
   * Instance for the query on a particular database.
   * 
   * @author Erich Schubert
   */
  public abstract static class Instance<O extends DatabaseObject, D extends Distance<D>> implements RangeQuery.Instance<O, D> {
    /**
     * The database we operate on.
     */
    protected Database<O> database;

    /**
     * Constructor.
     * 
     * @param database Database
     */
    public Instance(Database<O> database) {
      super();
      this.database = database;
    }
  }
}