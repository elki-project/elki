package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.Progress;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 *
 * @author Elke Achtert 
 */
public class HiSCPreprocessor<V extends RealVector<V,? >> extends AbstractParameterizable implements PreferenceVectorPreprocessor<V> {

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.01;

  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID(
      "hisc.alpha", "a double between 0 and 1 specifying the "
      + "maximum absolute variance along a coordinate axis.");

  /**
   * Alpha parameter
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID,
      new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN,
          1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);

  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID(
      "hisc.k", "a positive integer specifying the number of "
      + "nearest neighbors considered to determine the preference vector. " 
      + "If this value is not defined, k ist set to three "
      + "times of the dimensionality of the database objects.");

  /**
   * k Parameter
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

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
    addOption(ALPHA_PARAM);

    // parameter k
    addOption(K_PARAM);
  }

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

    DistanceFunction<V, DoubleDistance> distanceFunction = new EuclideanDistanceFunction<V>();
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
      for (QueryResult<DoubleDistance> knn : knns) {
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

  @Override
  public String parameterDescription() {
    StringBuffer description = new StringBuffer();
    description.append(HiSCPreprocessor.class.getName());
    description.append(" computes the preference vector of objects of a certain database according to the HiSC algorithm.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // alpha
    alpha = ALPHA_PARAM.getValue();

    // k
    if (K_PARAM.isSet())
      k = K_PARAM.getValue();

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
    double[] variances = DatabaseUtil.variances(database, database.get(id), neighborIDs);

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
      msg.append(FormatUtil.format(variances, ", ", 4));
      msg.append("\npreference ");
      msg.append(FormatUtil.format(variances.length, preferenceVector));
    }

    return preferenceVector;
  }
}
