package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract super class for distance functions needing a preprocessor.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <P> the type of Preprocessor used
 * @param <D> the type of Distance used
 */
public abstract class AbstractPreprocessorBasedDistanceFunction<O extends DatabaseObject, P extends Preprocessor<O>, D extends Distance<D>> extends AbstractDistanceFunction<O, D> implements PreprocessorBasedDistanceFunction<O, P, D> {
  /**
   * The handler class for the preprocessor.
   */
  private final PreprocessorHandler<O, P> preprocessorHandler;

  /**
   * Constructor, supporting
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable} style
   * classes.
   * 
   * @param config Parameterization
   * @param distance Distance Factory
   */
  public AbstractPreprocessorBasedDistanceFunction(Parameterization config, D distance) {
    super(distance);
    preprocessorHandler = new PreprocessorHandler<O, P>(config, this);
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction#setDatabase}
   * and runs the preprocessor on the database.
   * 
   * @param database the database to be set
   */
  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    preprocessorHandler.runPreprocessor(database);
  }

  /**
   * Get the preprocessor of this handler
   * 
   * @return Preprocessor
   */
  // TODO: try to remove this after 0.3 release?
  public final P getPreprocessor() {
    return preprocessorHandler.getPreprocessor();
  }
}
