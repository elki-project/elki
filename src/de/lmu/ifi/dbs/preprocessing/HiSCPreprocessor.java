package de.lmu.ifi.dbs.preprocessing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiSCPreprocessor extends AbstractParameterizable implements PreferenceVectorPreprocessor {

//  /**
//   * Holds the class specific debug status.
//   */
//  @SuppressWarnings({"UNUSED_SYMBOL"})
////  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;
//
//  /**
//   * The logger of this class.
//   */
//  private Logger logger = Logger.getLogger(this.getClass().getName());

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
  public static String ALPHA_D = "<double>a double between 0 and 1 specifying the " +
                                 "maximum absolute variance along a coordinate axis " +
                                 "(default is " + ALPHA_P + " = " + DEFAULT_ALPHA + ").";

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
  public static final String K_D = "<int>a positive integer specifying the number of "
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
   * Provides a new HiSCPreprocessor that computes the preference vector of
   * objects of a certain database.
   */
  public HiSCPreprocessor() {
    super();
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

      if (this.debug) {
        msg.append("\n\nid = ").append(id);
        msg.append(" ").append(database.getAssociation(AssociationID.LABEL, id));
        msg.append("\n knns: ");
      }

      List<QueryResult<DoubleDistance>> knns = database.kNNQueryForID(id, k, distanceFunction);
      List<Integer> knnIDs = new ArrayList<Integer>(knns.size());
      for (QueryResult knn : knns) {
        knnIDs.add(knn.getID());
        if (this.debug) {
          msg.append(database.getAssociation(AssociationID.LABEL, knn.getID())).append(" ");
        }
      }

      BitSet preferenceVector = determinePreferenceVector(database, id, knnIDs, msg);
      database.associate(AssociationID.PREFERENCE_VECTOR, id, preferenceVector);
      progress.setProcessed(processed++);

      if (verbose) {
    	  verbose("\r" + progress.getTask() + " - " + progress.toString());
//        logger.info("\r" + progress.getTask() + " - " + progress.toString());
      }
    }

    if (this.debug) {
//      logger.info(msg.toString());
    	debugFine(msg.toString());
//      logger.fine(msg.toString());
    }

    if (verbose) {
    	verbose("");
//      logger.info("\n");
    }

    long end = System.currentTimeMillis();
    if (time) {
      long elapsedTime = end - start;
      verbose(this.getClass().getName() + " runtime: "
                  + elapsedTime + " milliseconds.");
//      logger.info(this.getClass().getName() + " runtime: "
//                  + elapsedTime + " milliseconds.\n");
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
    String[] remainingParameters = super.setParameters(args);

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

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(ALPHA_P, Double.toString(alpha));
    mySettings.addSetting(K_P, Integer.toString(k));

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
   * Returns the value of the k parameter
   * (i.e. the number of nearest neighbors considered to determine the preference vector).
   *
   * @return the value of the k parameter
   */
  public int getK() {
    return k;
  }

  /**
   * Determines the preference vector according to the specified neighbor ids.
   *
   * @param database    the database storing the objects
   * @param id          the id of the object for which the preference vector should be determined
   * @param neighborIDs the ids of the neighbors
   * @param msg         a string buffer for debug messages
   * @return the preference vector
   */
  private BitSet determinePreferenceVector(Database<RealVector> database,
                                           Integer id,
                                           List<Integer> neighborIDs,
                                           StringBuffer msg) {
    // variances
    double[] variances = Util.variances(database, database.get(id), neighborIDs);

    // preference vector
    BitSet preferenceVector = new BitSet(variances.length);
    for (int d = 0; d < variances.length; d++) {
      if (variances[d] < alpha) {
        preferenceVector.set(d);
      }
    }

    if (this.debug) {
      msg.append("\nvariances ");
      msg.append(Util.format(variances, ", ", 4));
      msg.append("\npreference ");
      msg.append(Util.format(variances.length, preferenceVector));
    }

    return preferenceVector;
  }

}
