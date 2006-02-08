package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.pca.LinearLocalPCA;
import de.lmu.ifi.dbs.pca.LocalPCA;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;

import java.util.*;

/**
 * Abstract superclass for classes computing the correlation dimension of
 * objects of a certain database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class CorrelationDimensionPreprocessor implements Preprocessor {
  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.85;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<double>a double between 0 and 1 specifying " + "the threshold for strong eigenvectors: " + "the strong eigenvectors explain a " + "portion of at least alpha of the total variance " + "(default is alpha = " + DEFAULT_ALPHA + ")";

  /**
   * The default PCA class name.
   */
  public static final Class DEFAULT_PCA_CLASS = LinearLocalPCA.class;

  /**
   * Parameter for PCA.
   */
  public static final String PCA_CLASS_P = "pca";

  /**
   * Description for parameter pca.
   */
  public static final String PCA_CLASS_D = "<classname>the pca to determine the strong eigenvectors - must implement " + LocalPCA.class.getName() + ". " + "(Default: " + DEFAULT_PCA_CLASS.getName() + ").";

  /**
   * The default distance function for the PCA.
   */
  public static final String DEFAULT_PCA_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for pca distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_P = "pcaDistancefunction";

  /**
   * Description for parameter pca distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_D = "<classname>the distance function for the PCA to determine the distance between metrical objects - must implement " + DistanceFunction.class.getName() + ". " + "(Default: " + DEFAULT_PCA_DISTANCE_FUNCTION + ").";

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
   * The parameter settings for the PCA.
   */
  private String[] pcaParameters;

  /**
   * The distance function for the PCA.
   */
  protected DistanceFunction<DoubleVector, DoubleDistance> pcaDistanceFunction;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  public CorrelationDimensionPreprocessor() {
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    parameterToDescription.put(PCA_CLASS_P + OptionHandler.EXPECTS_VALUE, PCA_CLASS_D);
    parameterToDescription.put(PCA_DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, PCA_DISTANCE_FUNCTION_D);
  }

  /**
   * This method determines the correlation dimensions of the objects stored
   * in the specified database and sets the necessary associations in the
   * database.
   *
   * @param database the database for which the preprocessing is performed
   * @param verbose  flag to allow verbose messages while performing the algorithm
   */
  public void run(Database<DoubleVector> database, boolean verbose) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    try {
      Progress progress = new Progress(database.size());
      if (verbose) {
        System.out.println("Preprocessing:");
      }
      Iterator<Integer> it = database.iterator();
      int processed = 1;
      while (it.hasNext()) {
        Integer id = it.next();
        List<Integer> ids = objectIDsForPCA(id, database, verbose);

        LocalPCA pca = (LocalPCA) pcaClass.newInstance();
        pca.setParameters(pcaParameters);
        pca.run(ids, database, alpha);

        database.associate(AssociationID.LOCAL_PCA, id, pca);
        progress.setProcessed(processed++);

        if (verbose) {
          System.out.print("\r" + progress.toString());
        }
      }
      if (verbose) {
        System.out.println();
      }
    }
    catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
    catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the values for the parameters alpha, pca and pcaDistancefunction if
   * specified. If the parameters are not specified default values are set.
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
        throw new IllegalArgumentException(e);
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e);
      }
    }
    else {
      alpha = DEFAULT_ALPHA;
    }

    if (optionHandler.isSet(PCA_CLASS_P)) {
      pcaClass = Util.instantiate(LocalPCA.class, optionHandler.getOptionValue(PCA_CLASS_P)).getClass();
    }
    else {
      pcaClass = DEFAULT_PCA_CLASS;
    }

    if (optionHandler.isSet(PCA_DISTANCE_FUNCTION_P)) {
      pcaDistanceFunction = Util.instantiate(DistanceFunction.class, optionHandler.getOptionValue(PCA_DISTANCE_FUNCTION_P));
    }
    else {
      pcaDistanceFunction = Util.instantiate(DistanceFunction.class, DEFAULT_PCA_DISTANCE_FUNCTION);
    }
    remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);

    // save parameters for pca
    try {
      LocalPCA pca = (LocalPCA) pcaClass.newInstance();
      remainingParameters = pca.setParameters(remainingParameters);
      List<AttributeSettings> pcaSettings = pca.getAttributeSettings();
      List<String> params = new ArrayList<String>();
      for (AttributeSettings as : pcaSettings) {
        List<AttributeSetting> settings = as.getSettings();
        for (AttributeSetting s : settings) {
          params.add(OptionHandler.OPTION_PREFIX + s.getName());
          params.add(s.getValue());
        }
      }
      pcaParameters = params.toArray(new String[params.size()]);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException(e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(ALPHA_P, Double.toString(alpha));
    attributeSettings.addSetting(PCA_CLASS_P, pcaClass.getName());
    attributeSettings.addSetting(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunction.getClass().getName());

    result.add(attributeSettings);

    try {
      LocalPCA pca = (LocalPCA) pcaClass.newInstance();
      pca.setParameters(pcaParameters);
      List<AttributeSettings> pcaSettings = pca.getAttributeSettings();
      result.addAll(pcaSettings);
    }
    catch (InstantiationException e) {
      throw new IllegalArgumentException(e);
    }
    catch (IllegalAccessException e) {
      throw new IllegalArgumentException(e);
    }

    return result;
  }

  /**
   * Returns the ids of the objects stored in the specified database to be
   * considerd within the PCA for the specified object id.
   *
   * @param id       the id of the object for which a PCA should be performed
   * @param database the database holding the objects
   * @param verbose  flag to allow verbose messages while performing the algorithm
   * @return the list of the object ids to be considerd within the PCA
   */
  protected abstract List<Integer> objectIDsForPCA(Integer id, Database<DoubleVector> database, boolean verbose);
}
