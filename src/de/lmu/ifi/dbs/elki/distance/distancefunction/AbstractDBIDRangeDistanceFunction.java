package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.query.distance.DBIDRangeDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Abstract base class for distance functions that rely on integer offsets
 * within a consecutive range. This is beneficial for external distances.
 * 
 * @author Erich Schubert
 */
public abstract class AbstractDBIDRangeDistanceFunction extends AbstractDatabaseDistanceFunction<DBID> implements DBIDRangeDistanceFunction {
  @Override
  public double distance(DBIDRef o1, DBIDRef o2) {
    throw new AbortException("This must be called via a distance query to determine the DBID offset, not directly.");
  }

  @Override
  public SimpleTypeInformation<DBID> getInputTypeRestriction() {
    return TypeUtil.DBID;
  }

  @SuppressWarnings("unchecked")
  @Override
  public final <O extends DBID> DistanceQuery<O> instantiate(Relation<O> database) {
    return (DistanceQuery<O>) new DBIDRangeDistanceQuery((Relation<DBID>) database, this);
  }
}
