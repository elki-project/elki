package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractDistanceFunction provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek
 * @param <D> the type of Distance used
 */
public abstract class AbstractDBIDDistanceFunction<D extends Distance<D>> implements DBIDDistanceFunction<D> {
  /**
   * Provides an abstract DistanceFunction.
   */
  protected AbstractDBIDDistanceFunction() {
    // Empty
  }

  @Override
  abstract public D distance(DBID o1, DBID o2);
  
  @Override
  abstract public D getDistanceFactory();

  @Override
  public boolean isSymmetric() {
    // Assume symmetric by default!
    return true;
  }

  @Override
  public boolean isMetric() {
    // Do NOT assume triangle equation by default!
    return false;
  }

  @Override
  public Class<? super DatabaseObject> getInputDatatype() {
    return DatabaseObject.class;
  }

  @Override
  final public <T extends DatabaseObject> DistanceQuery<T, D> instantiate(Database<T> database) {
    return new DBIDDistanceQuery<T, D>(database, this);
  }
}