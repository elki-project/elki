package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.LocalProjectionPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for locally weighted distance functions using a
 * preprocessor to compute the local weight matrix.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <P> preprocessor type
 */
public abstract class AbstractLocallyWeightedDistanceFunction<O extends NumberVector<O, ?>, P extends LocalProjectionPreprocessor<O, ?>> extends AbstractDistanceFunction<O, DoubleDistance> implements LocalPCAPreprocessorBasedDistanceFunction<O, P, DoubleDistance> {
  /**
   * Parameter to specify the preprocessor to be used, must extend at least
   * {@link Preprocessor}.
   * <p>
   * Key: {@code -distancefunction.preprocessor}
   * </p>
   */
  private final ObjectParameter<P> PREPROCESSOR_PARAM = new ObjectParameter<P>(PREPROCESSOR_ID, LocalProjectionPreprocessor.class, KnnQueryBasedLocalPCAPreprocessor.class);

  /**
   * The preprocessor.
   */
  private P preprocessor;

  /**
   * Provides an abstract locally weighted distance function.
   */
  protected AbstractLocallyWeightedDistanceFunction(Parameterization config) {
    super(DoubleDistance.FACTORY);
    if(config.grab(PREPROCESSOR_PARAM)) {
      preprocessor = PREPROCESSOR_PARAM.instantiateClass(config);
    }
  }

  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    // TODO: verbose, timinig?
    preprocessor.run(database, false, false);
  }

 /**
   * Access the preprocessor of this distance function.
   * 
   * @return the preprocessor of this distance function.
   */
  public P getPreprocessor() {
    return preprocessor;
  }
}