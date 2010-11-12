package de.lmu.ifi.dbs.elki.database.query;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;

/**
 * Utility classes for Database Query handling.
 * 
 * @author Erich Schubert
 */
public final class DatabaseQueryUtil {
  /**
   * Choose an appropriate query manager for a distance query and database.
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceFunction Distance function
   * @param database Database
   * @return Query
   */
  @Deprecated
  public static <O extends DatabaseObject, D extends Distance<D>> DistanceQuery<O, D> chooseDistanceQuery(DistanceFunction<? super O, D> distanceFunction, Database<O> database) {
    return distanceFunction.instantiate(database);
    /*if(distanceFunction instanceof DatabaseDistanceFunction) {
      return ((DatabaseDistanceFunction<O, D>)distanceFunction).instantiate(db);
    }
    if(distanceFunction instanceof SpatialPrimitiveDistanceFunction) {
      return (DistanceQuery<O, D>) new SpatialPrimitiveDistanceQuery<DoubleVector, D>((Database<DoubleVector>)db, (SpatialPrimitiveDistanceFunction<DoubleVector, D>) distanceFunction);
    }
    if(distanceFunction instanceof PrimitiveDistanceFunction) {
      return new PrimitiveDistanceQuery<O, D>(db, (PrimitiveDistanceFunction<O, D>) distanceFunction);
    }
    if(distanceFunction instanceof DBIDDistanceFunction) {
      return new DBIDDistanceQuery<O, D>(db, (DBIDDistanceFunction<D>) distanceFunction);
    }
    throw new UnsupportedOperationException("Encountered unknown distance function class. Only primitive and database distances are supported.");*/
  }
}