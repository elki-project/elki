package de.lmu.ifi.dbs.elki.utilities.scaling.outlier;

import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.scaling.ScalingFunction;

/**
 * Interface for scaling functions used by Outlier evaluation such as Histograms
 * and visualization. Make sure to invoke {@link #prepare} prior to applying the
 * scaling function.
 * 
 * @author Erich Schubert
 */
public interface OutlierScalingFunction extends ScalingFunction {
  /**
   * Prepare is called once for each data set, before getScaled() will be
   * called. This function can be used to extract global parameters such as
   * means, minimums or maximums from the Database, Result or Annotation.
   * 
   * @param ids Database IDs to process
   * @param or Outlier result to use
   */
  public void prepare(DBIDs ids, OutlierResult or);
}