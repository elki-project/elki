package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Null Algorithm, which does nothing. Can be used to e.g. just visualize a data
 * set.
 * 
 * @author Erich Schubert
 */
@Title("Null Algorithm")
@Description("Algorithm which does nothing, just return a null object.")
public class NullAlgorithm extends AbstractAlgorithm<DatabaseObject, Result> {
  /**
   * Constructor.
   */
  public NullAlgorithm() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected Result runInTime(@SuppressWarnings("unused") Database<DatabaseObject> database) throws IllegalStateException {
    return null;
  }

  /**
   * Factory method for {@link Parameterizable}
   * 
   * @param config Parameterization
   * @return KNN outlier detection algorithm
   */
  public static NullAlgorithm parameterize(Parameterization config) {
    if(config.hasErrors()) {
      return null;
    }
    return new NullAlgorithm();
  }
}