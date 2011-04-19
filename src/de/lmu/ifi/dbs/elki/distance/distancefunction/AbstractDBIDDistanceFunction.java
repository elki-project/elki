package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.distance.DBIDDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * AbstractDistanceFunction provides some methods valid for any extending class.
 * 
 * @author Arthur Zimek
 * 
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
  public SimpleTypeInformation<DBID> getInputTypeRestriction() {
    return TypeUtil.DBID;
  }

  @SuppressWarnings("unchecked")
  @Override
  final public <O extends DBID> DistanceQuery<O, D> instantiate(Relation<O> database) {
    return (DistanceQuery<O, D>) new DBIDDistanceQuery<D>((Relation<DBID>) database, this);
  }
}