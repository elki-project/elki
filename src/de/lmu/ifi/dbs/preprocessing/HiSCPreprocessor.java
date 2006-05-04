package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;
import java.util.logging.Logger;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiSCPreprocessor implements Preprocessor {

  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
//  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.01;

  /**
   * Option string for parameter alpha.
   */
  public static final String ALPHA_P = "alpha";

  /**
   * Description for parameter alpha.
   */
  public static final String ALPHA_D = "<alpha>a double between 0 and 1 specifying the maximum variance along a coordinate axis " +
                                       "(default is alpha = " + DEFAULT_ALPHA + ").";

  /**
   * Undefined value for k.
   */
  public static final int UNDEFINED_K = -1;

  /**
   * Option string for parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>a positive integer specifying the number of "
                                   + "nearest neighbors considered to determine the preference vector. "
                                   + "If this value is not defined, k ist set to three "
                                   + "times of the dimensionality of the database objects.";

  /**
   * The maximum allowed variance along a coordinate axis.
   */
  private double alpha;

  /**
   * The number of nearest neighbors considered to determine the preference vector.
   */
  private int k;

  /**
   * OptionHandler for handling options.
   */
  private OptionHandler optionHandler;

  /**
   * Holds the currently set parameter array.
   */
  private String[] currentParameterArray = new String[0];

  /**
   * Provides a new HiSCPreprocessor that computes the preference vector of
   * objects of a certain database.
   */
  public HiSCPreprocessor() {
    Map<String, String> parameterToDescription = new Hashtable<String, String>();
    parameterToDescription.put(ALPHA_P + OptionHandler.EXPECTS_VALUE, ALPHA_D);
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void run(Database<RealVector> database, boolean verbose, boolean time) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    if (database.size() == 0) return;

    StringBuffer msg = new StringBuffer();

    long start = System.currentTimeMillis();
    Progress progress = new Progress("Preprocessing preference vector", database.size());

    if (k == UNDEFINED_K) {
      RealVector obj = database.get(database.iterator().next());
      k = 3 * obj.getDimensionality();
    }

    DistanceFunction<RealVector, DoubleDistance> distanceFunction = new EuklideanDistanceFunction<RealVector>();
    distanceFunction.setDatabase(database, verbose, time);

    Iterator<Integer> it = database.iterator();
    int processed = 1;
    while (it.hasNext()) {
      Integer id = it.next();

      if (DEBUG) {
        msg.append("\n\nid = ").append(id);
        msg.append(" ").append(database.getAssociation(AssociationID.LABEL, id));
        msg.append("\n knns: ");
      }


      List<QueryResult<DoubleDistance>> knns = database.kNNQueryForID(id, k, distanceFunction);

      List<Integer> knnIDs = new ArrayList<Integer>(knns.size());
      for (QueryResult knn : knns) {
        knnIDs.add(knn.getID());
        if (DEBUG) {
          msg.append(database.getAssociation(AssociationID.LABEL, knn.getID())).append(" ");
        }
      }

      BitSet preferenceVector = determinePreferenceVector(database, knnIDs, msg);
      database.associate(AssociationID.PREFERENCE_VECTOR, id, preferenceVector);
      progress.setProcessed(processed++);

      if (verbose) {
        logger.info("\r"+progress.getTask() + " - " + progress.toString());
      }
    }

    if (DEBUG) {
      logger.fine(msg.toString());
    }

    if (verbose) {
      logger.info("\n");
    }

    long end = System.currentTimeMillis();
    if (time) {
      long elapsedTime = end - start;
      logger.info(this.getClass().getName() + " runtime: "
                  + elapsedTime + " milliseconds.\n");
    }

  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(HiSCPreprocessor.class.getName());
    description.append(" computes the preference vector of objects of a certain database according to the HiSC algorithm.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = optionHandler.grabOptions(args);

    // alpha
    if (optionHandler.isSet(ALPHA_P)) {
      String alphaString = optionHandler.getOptionValue(ALPHA_P);
      try {
        alpha = Double.parseDouble(alphaString);
        if (alpha < 0 || alpha > 1) {
          throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(ALPHA_P, alphaString, ALPHA_D, e);
      }
    }
    else {
      alpha = DEFAULT_ALPHA;
    }

    // k
    if (optionHandler.isSet(K_P)) {
      String kString = optionHandler.getOptionValue(K_P);
      try {
        k = Integer.parseInt(kString);
        if (k <= 0) {
          throw new WrongParameterValueException(K_P, kString, K_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(K_P, kString, K_D, e);
      }
    }
    else {
      k = UNDEFINED_K;
    }

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Sets the difference of the first array minus the second array as the
   * currently set parameter array.
   *
   * @param complete the complete array
   * @param part     an array that contains only elements of the first array
   */
  protected void setParameters(String[] complete, String[] part) {
    currentParameterArray = Util.difference(complete, part);
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
   */
  public String[] getParameters() {
    String[] param = new String[currentParameterArray.length];
    System.arraycopy(currentParameterArray, 0, param, 0, currentParameterArray.length);
    return param;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

    AttributeSettings mySettings = new AttributeSettings(this);
    mySettings.addSetting(ALPHA_P, Double.toString(alpha));
    mySettings.addSetting(K_P, Integer.toString(k));
    attributeSettings.add(mySettings);

    return attributeSettings;
  }

  /**
   * Returns the value of the alpha parameter (i.e. the maximum
   * allowed variance along a coordinate axis).
   *
   * @return the value of the alpha parameter
   */
  public double getAlpha() {
    return alpha;
  }

  /**
   * Determines the preference vector according to the specified neighbor ids.
   *
   * @param database  the database storing the objects
   * @param neighbors the ids of the neighbors
   * @return the preference vector
   */
  private BitSet determinePreferenceVector(Database<RealVector> database,
                                           List<Integer> neighbors,
                                           StringBuffer msg) {
    // variances
    double[] variances = determineVariances(database, neighbors);

    // preference vector
    BitSet preferenceVector = new BitSet(variances.length);
    for (int d = 0; d < variances.length; d++) {
      if (variances[d] < alpha) {
        preferenceVector.set(d);
      }
    }

    if (DEBUG) {
      msg.append("\nvariances ");
      msg.append(Util.format(variances, ", ", 4));
      msg.append("\npreference ");
      msg.append(preferenceVectorToString(variances.length, preferenceVector));
    }

    return preferenceVector;
  }

  /**
   * Determines the variances in each dimension of the objects stored in the given
   * database.
   *
   * @param database the database storing the objects
   * @param knns     the ids of the objects
   * @return the variances in each dimension of the specified objects
   */
  private double[] determineVariances(Database<RealVector> database, List<Integer> knns) {
    // centroid
    RealVector centroid = Util.centroid(database, knns);
    StringBuffer msg = new StringBuffer();
    if (DEBUG) {
      msg.append("\ncentroid " + centroid);
    }

    double[] variances = new double[centroid.getDimensionality()];

    for (int d = 1; d <= centroid.getDimensionality(); d++) {
      double mu = centroid.getValue(d).doubleValue();

      for (Integer knn : knns) {
        RealVector o = database.get(knn);

        double diff = o.getValue(d).doubleValue() - mu;
        variances[d - 1] += diff * diff;

        //noinspection PointlessBooleanExpression
        if (d == 1 && DEBUG) {
          msg.append("\n");
          msg.append(o);
        }
      }

      variances[d - 1] /= knns.size();
    }

    if (DEBUG) {
      msg.append("\n");
    }

    return variances;
  }

  /**
   * Returns a string representation of the specified preference vector
   * for debugging purposes.
   *
   * @param dim              the dimensionality of the preference vector
   * @param preferenceVector the preference vector
   * @return a string representation of the specified preference vector
   *         for debugging purposes
   */
  private String preferenceVectorToString(int dim, BitSet preferenceVector) {
    StringBuffer msg = new StringBuffer();

    msg.append("[");

    for (int d = 0; d < dim; d++) {
      if (d > 0) msg.append(", ");
      if (preferenceVector.get(d)) msg.append("1");
      else msg.append("0");
    }

    msg.append("]");
    return msg.toString();
  }
}
