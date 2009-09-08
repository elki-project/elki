package de.lmu.ifi.dbs.elki.algorithm;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.utilities.Description;

/**
 * Null Algorithm, which does nothing.
 * Can be used to e.g. just visualize a data set.
 * 
 * @author Erich Schubert
 * @param <V> Vector type
 */
public class NullAlgorithm<V extends FeatureVector<V,?>> extends AbstractAlgorithm<V,Result> {

  /**
   * Empty constructor. Nothing to do.
   */
  public NullAlgorithm() {
    super();
  }

  /**
   * Iterates over all points in the database.
   */
  @Override
  protected Result runInTime(@SuppressWarnings("unused") Database<V> database) throws IllegalStateException {
    return null;
  }

  /**
   * Describe the algorithm and it's use.
   */
  public Description getDescription() {
    return new Description("Null","Null Algorithm",
        "Algorithm which does nothing, just return a null object.","");
  }

  /**
   * Return a result object
   */
  public Result getResult() {
    // Usually, you'll want to make a custom class derived from Result.
    return null;
  }
}
