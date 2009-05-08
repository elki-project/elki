package de.lmu.ifi.dbs.elki.visualization.scales;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;

/**
 * Scales helper class.
 * Currently, this will just compute a linear scale for each axis.
 * It is planned to add functionality to include some analysis to
 * be able to automatically choose log scales when appropriate. 
 * 
 * @author Erich Schubert
 *
 */
public class Scales {
  /**
   * Compute a linear scale for each dimension.
   * 
   * @param <O> vector type
   * @param db Database
   * @return Scales, indexed starting with 1 (just like the database)
   */
  public static <O extends DoubleVector> LinearScale[] calcScales(Database<O> db) {
    int dim = db.dimensionality();
    double min[] = new double[dim];
    double max[] = new double[dim];
    LinearScale scales[] = new LinearScale[dim+1];
  
    // initialize
    for(int d = 0; d < dim; d++) {
      min[d] = Float.MAX_VALUE;
      max[d] = Float.MIN_VALUE;
    }
  
    // analyze data
    for(Integer objId : db.getIDs()) {
      DoubleVector v = db.get(objId);
      for(int d = 0; d < dim; d++) {
        min[d] = Math.min(min[d], v.getValue(d + 1));
        max[d] = Math.max(max[d], v.getValue(d + 1));
      }
    }
  
    // generate scales
    scales[0] = null;
    for(int d = 0; d < dim; d++) {
      scales[d+1] = new LinearScale(min[d], max[d]);
    }
    return scales;
  }

}
