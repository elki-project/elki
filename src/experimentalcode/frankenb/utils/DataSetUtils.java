package experimentalcode.frankenb.utils;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * No description given.
 * 
 * @author Florian Frankenberger
 */
public final class DataSetUtils {
  private DataSetUtils() {
  }

  /**
   * Splits a data set according to the dimension and position into two data
   * sets. If dimension is 1 the data gets split into two data sets where all
   * points with x < position get into one data set and all points >= position
   * get into the other
   * 
   * @param relation Relation to split
   * @param ids DBIDs to process
   * @param dimension Dimension to use
   * @param position Split position
   */
  public static <V extends NumberVector<?, ?>> ArrayModifiableDBIDs[] splitAtMedian(Relation<V> relation, DBIDs ids, int dimension, double position) {
    ArrayModifiableDBIDs dataSetLower = DBIDUtil.newArray((int) (ids.size() * 0.51));
    ArrayModifiableDBIDs dataSetExact = DBIDUtil.newArray((int) (ids.size() * 0.05));
    ArrayModifiableDBIDs dataSetHigher = DBIDUtil.newArray((int) (ids.size() * 0.51));
    for(DBID id : ids) {
      double val = relation.get(id).doubleValue(dimension);
      if(val < position) {
        dataSetLower.add(id);
      }
      else if(val > position) {
        dataSetHigher.add(id);
      }
      else {
        dataSetExact.add(id);
      }
    }
    return new ArrayModifiableDBIDs[] { dataSetLower, dataSetExact, dataSetHigher };
  }
}