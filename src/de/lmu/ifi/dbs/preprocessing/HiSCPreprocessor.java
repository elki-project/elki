package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.LessEqualConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.ParameterConstraint;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class HiSCPreprocessor<V extends RealVector<V,?>> extends AbstractParameterizable implements PreferenceVectorPreprocessor<V> {

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
  public static String ALPHA_D = "a double between 0 and 1 specifying the " + "maximum absolute variance along a coordinate axis "
                                 + "(default is " + ALPHA_P + " = " + DEFAULT_ALPHA + ").";

  /**
   * Option string for parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "a positive integer specifying the number of "+
                                   "nearest neighbors considered to determine the preference vector. " +
                                   "If this value is not defined, k ist set to three "+
                                   "times of the dimensionality of the database objects.";

  /**
   * The maximum allowed variance along a coordinate axis.
   */
  private double alpha;

  /**
   * The number of nearest neighbors considered to determine the preference
   * vector.
   */
  private Integer k;

  /**
   * Provides a new HiSCPreprocessor that computes the preference vector of
   * objects of a certain database.
   */
  public HiSCPreprocessor() {
    super();
//    this.debug = true;

    // parameter alpha
    ArrayList<ParameterConstraint<Number>> alphaCons = new ArrayList<ParameterConstraint<Number>>();
    alphaCons.add(new GreaterEqualConstraint(0));
    alphaCons.add(new LessEqualConstraint(1));
    DoubleParameter alpha = new DoubleParameter(ALPHA_P, ALPHA_D, alphaCons);
    alpha.setDefaultValue(DEFAULT_ALPHA);
    optionHandler.put(ALPHA_P, alpha);

    // parameter k
    IntParameter kParam = new IntParameter(K_P, K_D, new GreaterConstraint(0));
    kParam.setOptional(true);
    optionHandler.put(K_P, kParam);
  }

  /**
   * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void run(Database<V> database, boolean verbose, boolean time) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    if (database.size() == 0)
      return;

    StringBuffer msg = new StringBuffer();

    long start = System.currentTimeMillis();
    Progress progress = new Progress("Preprocessing preference vector", database.size());

    if (k == null) {
      V obj = database.get(database.iterator().next());
      k = 3 * obj.getDimensionality();
    }

    DistanceFunction<V, DoubleDistance> distanceFunction = new EuklideanDistanceFunction<V>();
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
        progress(progress);
      }
    }

    if (this.debug) {
      debugFine(msg.toString());
    }

    if (verbose) {
      verbose("");
    }

    long end = System.currentTimeMillis();
    if (time) {
      long elapsedTime = end - start;
      verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
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
    alpha = (Double) optionHandler.getOptionValue(ALPHA_P);

    // k
    if (optionHandler.isSet(K_P)) {
      k = (Integer) optionHandler.getOptionValue(K_P);
    }

    return remainingParameters;
  }

  /**
   * Returns the value of the alpha parameter (i.e. the maximum allowed
   * variance along a coordinate axis).
   *
   * @return the value of the alpha parameter
   */
  public double getAlpha() {
    return alpha;
  }

  /**
   * Returns the value of the k parameter (i.e. the number of nearest
   * neighbors considered to determine the preference vector).
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
   * @param id          the id of the object for which the preference vector should be
   *                    determined
   * @param neighborIDs the ids of the neighbors
   * @param msg         a string buffer for debug messages
   * @return the preference vector
   */
  private BitSet determinePreferenceVector(Database<V> database, Integer id, List<Integer> neighborIDs, StringBuffer msg) {
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
      msg.append("\nalpha "+alpha);
      msg.append("\nvariances ");
      msg.append(Util.format(variances, ", ", 4));
      msg.append("\npreference ");
      msg.append(Util.format(variances.length, preferenceVector));
    }

    return preferenceVector;
  }
}
