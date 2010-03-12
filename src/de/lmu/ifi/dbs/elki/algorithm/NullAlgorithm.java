package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Null Algorithm, which does nothing. Can be used to e.g. just visualize a data
 * set.
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 */
@Title("Null Algorithm")
@Description("Algorithm which does nothing, just return a null object.")
public class NullAlgorithm<V extends NumberVector<V, ?>> extends AbstractAlgorithm<V, Result> {
  /**
   * Empty constructor. Nothing to do.
   */
  public NullAlgorithm(Parameterization config) {
    super(config);
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected Result runInTime(@SuppressWarnings("unused") Database<V> database) throws IllegalStateException {
    return null;
  }
}
