package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.LocalPCAPreprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.Preprocessor;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorClient;
import de.lmu.ifi.dbs.elki.preprocessing.PreprocessorHandler;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Abstract super class for locally weighted distance functions using a
 * preprocessor to compute the local weight matrix.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to compute the distances in between
 * @param <P> preprocessor type
 */
public abstract class AbstractLocallyWeightedDistanceFunction<O extends NumberVector<O, ?>, P extends LocalPCAPreprocessor<O>> extends AbstractDoubleDistanceFunction<O> implements PreprocessorClient<P, O>, LocalPCAPreprocessorBasedDistanceFunction<O, P, DoubleDistance> {
  /**
   * The handler class for the preprocessor.
   */
  private PreprocessorHandler<O, P> preprocessorHandler;

  /**
   * Provides an abstract locally weighted distance function.
   */
  protected AbstractLocallyWeightedDistanceFunction(Parameterization config) {
    super();
    preprocessorHandler = new PreprocessorHandler<O, P>(config, this);
  }

  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    preprocessorHandler.runPreprocessor(database);
  }

  /**
   * @return the name of the default preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.KnnQueryBasedLocalPCAPreprocessor}
   */
  @Override
  public Class<?> getDefaultPreprocessorClass() {
    return KnnQueryBasedLocalPCAPreprocessor.class;
  }

  public String getPreprocessorDescription() {
    return "Preprocessor class to determine the correlation dimension of each object.";
  }

  /**
   * @return the super class for the preprocessor, which is
   *         {@link de.lmu.ifi.dbs.elki.preprocessing.Preprocessor}
   */
  public Class<P> getPreprocessorSuperClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(Preprocessor.class);
  }
}