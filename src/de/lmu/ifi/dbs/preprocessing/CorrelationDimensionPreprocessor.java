package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.pca.LinearCorrelationPCA;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract superclass for classes computing the correlation dimension
 * of objects of a certain database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CorrelationDimensionPreprocessor implements Preprocessor {
  /**
   * The association id to associate a pca to an object.
   */
  public static final String ASSOCIATION_ID_PCA = "associationIDPCA";

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.8;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<double>a double between 0 and 1 specifying " +
                                       "the threshold for strong eigenvectors: " +
                                       "the strong eigenvectors explain a " +
                                       "portion of at least alpha of the total variance " +
                                       "(default is alpha = " + DEFAULT_ALPHA + ")";

  /**
   * The default PCA class name.
   */
  public static final Class DEFAULT_PCA_CLASS = LinearCorrelationPCA.class;

  /**
   * Parameter for PCA.
   */
  public static final String PCA_CLASS_P = "pca";


  /**
   * Description for parameter pca.
   */
  public static final String PCA_CLASS_D = "<classname>the pca to determine the strong eigenvectors - must implement " +
                                           CorrelationPCA.class.getName() + ". " +
                                           "(Default: " + DEFAULT_PCA_CLASS.getName() + ").";

  /**
   * The default distance function for the PCA.
   */
  public static final DistanceFunction DEFAULT_PCA_DISTANCE_FUNCTION = new EuklideanDistanceFunction();

  /**
   * Parameter for pca distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_P = "pcaDistancefunction";

  /**
   * Description for parameter pca distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_D = "<classname>the distance function for the PCA to determine the " +
                                                       "distance between metrical objects - must implement " +
                                                       DistanceFunction.class.getName() + ". " +
                                                       "(Default: " + DEFAULT_PCA_DISTANCE_FUNCTION.getClass().getName() + ").";

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * The threshold for strong eigenvectors: the strong eigenvectors explain a
   * portion of at least alpha of the total variance.
   */
  protected double alpha;

  /**
   * The classname of the PCA to determine the strong eigenvectors.
   */
  protected Class pcaClass;

  /**
   * The distance function for the PCA.
   */
  protected DistanceFunction pcaDistanceFunction;

  /**
   * Provides a new Preprocessor that computes the correlation dimension
   * of objects of a certain database.
   */
  public CorrelationDimensionPreprocessor() {
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    parameterToDescription.put(PCA_CLASS_P + OptionHandler.EXPECTS_VALUE, PCA_CLASS_D);
    parameterToDescription.put(PCA_DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, PCA_DISTANCE_FUNCTION_D);
  }

  /**
   * This method determines the correlation dimensions of the
   * objects stored in the specified database and sets the necessary
   * associations in the database.
   *
   * @param database the database for which the preprocessing is performed
   */
  public void run(Database database) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    try {
      Iterator<Integer> it = database.iterator();
      while (it.hasNext()) {
        Integer id = it.next();
        List<QueryResult> objects = objectsForPCA(id, database);
        CorrelationPCA pca = (CorrelationPCA) pcaClass.newInstance();
        pca.run(objects, database, alpha);
        database.associate(ASSOCIATION_ID_PCA, id, pca);
      }
    }
    catch (InstantiationException e) {
      throw new RuntimeException(e.getMessage());
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * Sets the values for the parameters alpha, pca and pcaDistancefunction if specified.
   * If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(ALPHA_P)) {
      try {
        alpha = Double.parseDouble(optionHandler.getOptionValue(ALPHA_P));
        if (alpha <= 0 || alpha >= 1)
          throw new IllegalArgumentException("CorrelationDistanceFunction: alpha has to be between zero and one!");
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      alpha = DEFAULT_ALPHA;
    }

    if (optionHandler.isSet(PCA_CLASS_P)) {
      try {
        pcaClass = Class.forName(optionHandler.getOptionValue(PCA_CLASS_P));
        // test
        pcaClass.newInstance();
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      pcaClass = DEFAULT_PCA_CLASS;
    }

    if (optionHandler.isSet(PCA_DISTANCE_FUNCTION_P)) {
      try {
        pcaDistanceFunction = ((DistanceFunction) Class.forName(
        optionHandler.getOptionValue(PCA_DISTANCE_FUNCTION_P)).newInstance());
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e.getMessage());
      }
    }
    else {
      pcaDistanceFunction = DEFAULT_PCA_DISTANCE_FUNCTION;
    }
    return pcaDistanceFunction.setParameters(remainingParameters);
  }

  /**
   * Returns the ids of the objects stored in the specified database
   * to be considerd within the PCA for the specified object id.
   *
   * @param id       the id of the object for which a PCA should be performed
   * @param database the database holding the objects
   * @return the list of the objects to be considerd within the PCA
   */
  protected abstract List<QueryResult> objectsForPCA(Integer id, Database database);
}
