package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.pca.LocalPCA;
import de.lmu.ifi.dbs.pca.LinearLocalPCA;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSetting;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.*;

/**
 * Preprocessor for 4C correlation dimension assignment to
 * objects of a certain database.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourCPreprocessor implements Preprocessor {
  /**
   * The default value for delta.
   */
  public static final double DEFAULT_DELTA = 0.01;

  /**
   * Option string for parameter delta.
   */
  public static final String DELTA_P = "delta";

  /**
   * Description for parameter delta.
   */
  public static final String DELTA_D = "<double>a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = " + DEFAULT_DELTA + ").";

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = "preprocessorEpsilon";

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the Euklidean distance function";

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Map providing a mapping of parameters to their descriptions.
   */
  protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

  /**
   * OptionHandler for handling options.
   */
  protected OptionHandler optionHandler;

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The classname of the PCA to determine the strong eigenvectors.
   */
  protected String pcaClass = LinearLocalPCA.class.getName();

  /**
   * The parameter settings for the PCA.
   */
  private String[] pcaParameters;

  /**
   * The distance function for the PCA.
   */
  private EuklideanDistanceFunction<DoubleVector> pcaDistanceFunction = new EuklideanDistanceFunction<DoubleVector>();

  /**
   * Provides a new Preprocessor that computes the correlation dimension of
   * objects of a certain database.
   */
  public FourCPreprocessor() {
    parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE, DELTA_D);
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
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
    pcaDistanceFunction.setDatabase(database, verbose);

    Progress progress = new Progress(database.size());
    if (verbose) {
      System.out.println("Preprocessing:");
    }
    Iterator<Integer> it = database.iterator();
    int processed = 1;
    while (it.hasNext()) {
      Integer id = it.next();
      List<QueryResult<DoubleDistance>> qrs = database.rangeQuery(id, epsilon, pcaDistanceFunction);

      List<Integer> ids = new ArrayList<Integer>(qrs.size());
      for (QueryResult<DoubleDistance> qr : qrs) {
        ids.add(qr.getID());
      }


      LinearLocalPCA pca = Util.instantiate(LinearLocalPCA.class, pcaClass);
      pca.setParameters(pcaParameters);
      pca.run4CPCA(ids, database, delta);

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

  }

  /**
   * Sets the values for the parameters alpha, pca and pcaDistancefunction if
   * specified. If the parameters are not specified default values are set.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    if (optionHandler.isSet(DELTA_P)) {
      try {
        delta = Double.parseDouble(optionHandler.getOptionValue(DELTA_P));
        if (delta < 0 || delta > 1) {
          throw new IllegalArgumentException(DELTA_P + " has to be between zero and one!");
        }
      }
      catch (NumberFormatException e) {
        throw new IllegalArgumentException(DELTA_P + " has to be a double value between zero and one!");
      }
    }
    else {
      delta = DEFAULT_DELTA;
    }
    remainingParameters = pcaDistanceFunction.setParameters(remainingParameters);

    // epsilon
    epsilon = optionHandler.getOptionValue(EPSILON_P);

    // save parameters for pca
    LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClass);
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
    attributeSettings.addSetting(DELTA_P, Double.toString(delta));
    attributeSettings.addSetting(EPSILON_P, epsilon);
    result.add(attributeSettings);

    LocalPCA pca = Util.instantiate(LocalPCA.class, pcaClass);
    pca.setParameters(pcaParameters);
    List<AttributeSettings> pcaSettings = pca.getAttributeSettings();
    result.addAll(pcaSettings);

    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(FourCPreprocessor.class.getName());
    description.append(" computes the correlation dimension of objects of a certain database according to the 4C algorithm.\n");
    description.append("The PCA is based on epsilon range queries.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }


}
