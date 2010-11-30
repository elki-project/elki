package de.lmu.ifi.dbs.elki.preprocessing;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.HiSC;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * Preprocessor for HiSC preference vector assignment to objects of a certain
 * database.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has Instance oneway - - produces
 * 
 * @see HiSC
 */
@Title("HiSC Preprocessor")
@Description("Computes the preference vector of objects of a certain database according to the HiSC algorithm.")
public class HiSCPreprocessor implements PreferenceVectorPreprocessor<NumberVector<?,?>> {
  /**
   * Logger to use
   */
  protected static final Logging logger = Logging.getLogger(HiSCPreprocessor.class);

  /**
   * The default value for alpha.
   */
  public static final double DEFAULT_ALPHA = 0.01;

  /**
   * OptionID for {@link #ALPHA_PARAM}
   */
  public static final OptionID ALPHA_ID = OptionID.getOrCreateOptionID("hisc.alpha", "The maximum absolute variance along a coordinate axis.");

  /**
   * The maximum absolute variance along a coordinate axis. Must be in the range
   * of [0.0, 1.0).
   * <p>
   * Default value: {@link #DEFAULT_ALPHA}
   * </p>
   * <p>
   * Key: {@code -hisc.alpha}
   * </p>
   */
  private final DoubleParameter ALPHA_PARAM = new DoubleParameter(ALPHA_ID, new IntervalConstraint(0.0, IntervalConstraint.IntervalBoundary.OPEN, 1.0, IntervalConstraint.IntervalBoundary.OPEN), DEFAULT_ALPHA);

  /**
   * Holds the value of parameter {@link #ALPHA_PARAM}.
   */
  protected double alpha;

  /**
   * OptionID for {@link #K_PARAM}.
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("hisc.k", "The number of nearest neighbors considered to determine the preference vector. If this value is not defined, k ist set to three times of the dimensionality of the database objects.");

  /**
   * The number of nearest neighbors considered to determine the preference
   * vector. If this value is not defined, k is set to three times of the
   * dimensionality of the database objects.
   * <p>
   * Key: {@code -hisc.k}
   * </p>
   * <p>
   * Default value: three times of the dimensionality of the database objects
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0), true);

  /**
   * Holds the value of parameter {@link #K_PARAM}.
   */
  protected Integer k;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public HiSCPreprocessor(Parameterization config) {
    super();
    config = config.descend(this);

    // parameter alpha
    if(config.grab(ALPHA_PARAM)) {
      alpha = ALPHA_PARAM.getValue();
    }

    // parameter k
    if(config.grab(K_PARAM)) {
      k = K_PARAM.getValue();
    }
  }

  @Override
  public <V extends NumberVector<?,?>> Instance<V> instantiate(Database<V> database) {
    return new Instance<V>(database);
  }

  /**
   * The actual preprocessor instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery
   * @apiviz.has de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction
   * 
   * @param <V> The actual data type
   */
  public class Instance<V extends NumberVector<?,?>> implements PreferenceVectorPreprocessor.Instance<V> {
    /**
     * The data storage for the precomputed data.
     */
    private WritableDataStore<BitSet> preferenceVectors;

    public Instance(Database<V> database) {
      if(database == null || database.size() <= 0) {
        throw new IllegalArgumentException(ExceptionMessages.DATABASE_EMPTY);
      }

      preferenceVectors = DataStoreUtil.makeStorage(database.getIDs(), DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, BitSet.class);

      StringBuffer msg = new StringBuffer();

      long start = System.currentTimeMillis();
      FiniteProgress progress = logger.isVerbose() ? new FiniteProgress("Preprocessing preference vector", database.size(), logger) : null;

      if(k == null) {
        k = 3 * DatabaseUtil.dimensionality(database);
      }

      KNNQuery<V, DoubleDistance> knnQuery = database.getKNNQuery(EuclideanDistanceFunction.STATIC, k);
      
      Iterator<DBID> it = database.iterator();
      while(it.hasNext()) {
        DBID id = it.next();

        if(logger.isDebugging()) {
          msg.append("\n\nid = ").append(id);
          msg.append(" ").append(database.getObjectLabel(id));
          msg.append("\n knns: ");
        }

        List<DistanceResultPair<DoubleDistance>> knns = knnQuery.getKNNForDBID(id, k);
        ModifiableDBIDs knnIDs = DBIDUtil.newArray(knns.size());
        for(DistanceResultPair<DoubleDistance> knn : knns) {
          knnIDs.add(knn.getID());
          if(logger.isDebugging()) {
            msg.append(database.getObjectLabel(knn.getID())).append(" ");
          }
        }

        BitSet preferenceVector = determinePreferenceVector(database, id, knnIDs, msg);
        preferenceVectors.put(id, preferenceVector);

        if(progress != null) {
          progress.incrementProcessed(logger);
        }
      }
      if(progress != null) {
        progress.ensureCompleted(logger);
      }

      if(logger.isDebugging()) {
        logger.debugFine(msg.toString());
      }

      long end = System.currentTimeMillis();
      // TODO: re-add timing code!
      if(logger.isVerbose()) {
        long elapsedTime = end - start;
        logger.verbose(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
      }
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
     * @param database the database storing the objects
     * @param id the id of the object for which the preference vector should be
     *        determined
     * @param neighborIDs the ids of the neighbors
     * @param msg a string buffer for debug messages
     * @return the preference vector
     */
    private BitSet determinePreferenceVector(Database<V> database, DBID id, DBIDs neighborIDs, StringBuffer msg) {
      // variances
      double[] variances = DatabaseUtil.variances(database, database.get(id), neighborIDs);

      // preference vector
      BitSet preferenceVector = new BitSet(variances.length);
      for(int d = 0; d < variances.length; d++) {
        if(variances[d] < alpha) {
          preferenceVector.set(d);
        }
      }

      if(msg != null && logger.isDebugging()) {
        msg.append("\nalpha " + alpha);
        msg.append("\nvariances ");
        msg.append(FormatUtil.format(variances, ", ", 4));
        msg.append("\npreference ");
        msg.append(FormatUtil.format(variances.length, preferenceVector));
      }

      return preferenceVector;
    }

    @Override
    public BitSet get(DBID id) {
      return preferenceVectors.get(id);
    }
  }
}