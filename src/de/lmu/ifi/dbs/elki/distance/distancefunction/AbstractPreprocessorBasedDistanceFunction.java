package de.lmu.ifi.dbs.elki.distance.distancefunction;

import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.Distance;
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
   * Provides a super class for distance functions needing a preprocessor
   * 
   * @param pattern a pattern to define the required input format
   */
  public AbstractPreprocessorBasedDistanceFunction(Parameterization config, Pattern pattern) {
    super(pattern);
    preprocessorHandler = new PreprocessorHandler<O, P>(config, this);
  }

  /**
   * Calls
   * {@link de.lmu.ifi.dbs.elki.distance.AbstractMeasurementFunction#setDatabase(de.lmu.ifi.dbs.elki.database.Database,boolean,boolean)
   * AbstractMeasurementFunction(database, verbose, time)} and runs the
   * preprocessor on the database.
   * 
   * @param database the database to be set
   * @param verbose flag to allow verbose messages while performing the method
   * @param time flag to request output of performance time
   */
  @Override
  public void setDatabase(Database<O> database, boolean verbose, boolean time) {
    super.setDatabase(database, verbose, time);
    preprocessorHandler.runPreprocessor(database, verbose, time);
  }

  public final P getPreprocessor() {
    return preprocessorHandler.getPreprocessor();
  }
}
