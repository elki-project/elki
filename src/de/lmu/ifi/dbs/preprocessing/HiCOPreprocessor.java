package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.varianceanalysis.LinearLocalPCA;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Abstract superclass for preprocessors for HiCO correlation dimension assignment
 * to objects of a certain database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public abstract class HiCOPreprocessor implements Preprocessor {
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
  public static final String DEFAULT_PCA_CLASS = LinearLocalPCA.class.getName();

  /**
   * Parameter for PCA.
   */
  public static final String PCA_CLASS_P = "varianceanalysis";

  /**
   * Description for parameter varianceanalysis.
   */
  public static final String PCA_CLASS_D = "<classname>the varianceanalysis to determine the strong eigenvectors - must implement " + LocalPCA.class.getName() + ". " + "(Default: " + DEFAULT_PCA_CLASS + ").";

  /**
   * The default distance function for the PCA.
   */
  public static final String DEFAULT_PCA_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for varianceanalysis distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_P = "pcaDistancefunction";

  /**
   * Description for parameter varianceanalysis distance function.
   */
  public static final String PCA_DISTANCE_FUNCTION_D = "<classname>the distance function for the PCA to determine the distance between database objects - must implement " + DistanceFunction.class.getName() + ". " + "(Default: " + DEFAULT_PCA_DISTANCE_FUNCTION + ").";

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
  protected String pcaClassName;

  /**
   * The parameter settings for the PCA.
   */
  private String[] pcaParameters;
  
  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];
  
  /**
   * The distance function for the PCA.
   */
  protected DistanceFunction<RealVector, DoubleDistance> pcaDistanceFunction;

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  public HiCOPreprocessor() {
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
   * @param time     flag to request output of performance time
   */
  public void run(Database<RealVector> database, boolean verbose, boolean time) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    try {
      long start = System.currentTimeMillis();
      Progress progress = new Progress("Preprocessing correlation dimension",database.size());
      if (verbose) {
        System.out.println("Preprocessing:");
      }
      Iterator<Integer> it = database.iterator();
      int processed = 1;
      while (it.hasNext()) {
        Integer id = it.next();
        List<Integer> ids = objectIDsForPCA(id, database, verbose, false);

        LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClassName);
        pca.setParameters(pcaParameters);
        pca.run(ids, database, alpha);

        database.associate(AssociationID.LOCAL_PCA, id, pca);
        database.associate(AssociationID.LOCALLY_WEIGHTED_MATRIX, id, pca.getSimilarityMatrix());
        progress.setProcessed(processed++);

        if (verbose) {
          System.out.print("\r" + progress.toString());
        }
      }
      if (verbose) {
        System.out.println();
      }

      long end = System.currentTimeMillis();
      if (time) {
        long elapsedTime = end - start;
        System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
      }
    }
    catch (ParameterException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }
    catch (UnableToComplyException e) {
      // tested before
      throw new RuntimeException("This should never happen!");
    }
  }

  /**
   * Sets the values for the parameters alpha, varianceanalysis and pcaDistancefunction if
   * specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    //alpha
    if (optionHandler.isSet(ALPHA_P)) {
      String alphaString = optionHandler.getOptionValue(ALPHA_P);
      try {
        alpha = Double.parseDouble(alphaString);
        if (alpha <= 0 || alpha >= 1)
          throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D);
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D, e);
      }
    }
    else {
      alpha = DEFAULT_ALPHA;
    }

    // varianceanalysis
    LocalPCA tmpPCA;
    if (optionHandler.isSet(PCA_CLASS_P)) {
      pcaClassName = optionHandler.getOptionValue(PCA_CLASS_P);
    }
    else {
      pcaClassName = DEFAULT_PCA_CLASS;
    }
    try {
      tmpPCA = Util.instantiate(LocalPCA.class, pcaClassName);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(PCA_CLASS_P, pcaClassName, PCA_CLASS_D);
    }

    // varianceanalysis distance function
    String pcaDistanceFunctionClassName;
    if (optionHandler.isSet(PCA_DISTANCE_FUNCTION_P)) {
      pcaDistanceFunctionClassName = optionHandler.getOptionValue(PCA_DISTANCE_FUNCTION_P);
    }
    else {
      pcaDistanceFunctionClassName = DEFAULT_PCA_DISTANCE_FUNCTION;
    }
    try {
      //noinspection unchecked
      pcaDistanceFunction = Util.instantiate(DistanceFunction.class, pcaDistanceFunctionClassName);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunctionClassName, PCA_DISTANCE_FUNCTION_D);
    }
    remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);

    // save parameters for varianceanalysis
    String[] pcaRemainingParameters = tmpPCA.setParameters(remainingParameters);
    List<String> tmpRemainingParameters = Arrays.asList(pcaRemainingParameters);
    List<String> params = new ArrayList<String>();
    for (String param : remainingParameters) {
      if (! tmpRemainingParameters.contains(param)) {
        params.add(param);
      }
    }
    pcaParameters = params.toArray(new String[params.size()]);
    setParameters(args, remainingParameters);
    return pcaRemainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array
   * as the currently set parameter array.
   * 
   * 
   * @param complete the complete array
   * @param part an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part)
  {
      currentParameterArray = Util.difference(complete, part);
  }
  
  /**
   * 
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters()
  {
      String[] param = new String[currentParameterArray.length];
      System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
      return param;
  }
  
  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    mySettings.addSetting(ALPHA_P, Double.toString(alpha));
    mySettings.addSetting(PCA_CLASS_P, pcaClassName);
    mySettings.addSetting(PCA_DISTANCE_FUNCTION_P, pcaDistanceFunction.getClass().getName());

    attributeSettings.add(mySettings);

    try {
      LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClassName);
      pca.setParameters(pcaParameters);
      attributeSettings.addAll(pca.getAttributeSettings());
    }
    catch (UnableToComplyException e) {
      // tested before!!!
      throw new RuntimeException("This should never happen!");
    }
    catch (ParameterException e) {
      // tested before!!!
      throw new RuntimeException("This should never happen!");
    }

    return attributeSettings;
  }

  /**
   * Returns the ids of the objects stored in the specified database to be
   * considerd within the PCA for the specified object id.
   *
   * @param id       the id of the object for which a PCA should be performed
   * @param database the database holding the objects
   * @param verbose  flag to allow verbose messages while performing the algorithm
   * @param time     flag to request output of performance time
   * @return the list of the object ids to be considerd within the PCA
   */
  protected abstract List<Integer> objectIDsForPCA(Integer id, Database<RealVector> database, boolean verbose, boolean time);
}
