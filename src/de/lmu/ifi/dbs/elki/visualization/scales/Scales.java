package de.lmu.ifi.dbs.elki.visualization.scales;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Scales helper class.
 * Currently, this will just compute a linear scale for each axis.
 * It is planned to add functionality to include some analysis to
 * be able to automatically choose log scales when appropriate. 
 * 
 * @author Erich Schubert
 *
 * @apiviz.has LinearScale oneway - - computes
 */
public class Scales {
  /**
   * Compute a linear scale for each dimension.
   * 
   * @param <O> vector type
   * @param db Database
   * @return Scales, indexed starting with 0 (like Vector, not database objects!)
   */
  public static <O extends NumberVector<?,? extends Number>> LinearScale[] calcScales(Database<O> db) {
    if (db == null) {
      throw new AbortException("No database was given to Scales.calcScales.");
    }
    int dim = DatabaseUtil.dimensionality(db);
    DoubleMinMax minmax[] = DoubleMinMax.newArray(dim);
    LinearScale scales[] = new LinearScale[dim];
  
    // analyze data
    for(DBID objId : db.getIDs()) {
      O v = db.get(objId);
      for(int d = 0; d < dim; d++) {
        minmax[d].put(v.doubleValue(d+1));
      }
    }
  
    // generate scales
    for(int d = 0; d < dim; d++) {
      scales[d] = new LinearScale(minmax[d].getMin(), minmax[d].getMax());
    }
    return scales;
  }
}
