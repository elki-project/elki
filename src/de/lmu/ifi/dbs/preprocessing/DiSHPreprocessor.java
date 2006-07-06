package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DimensionSelectingDistanceFunction;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.logging.LoggingConfiguration;
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
 * Preprocessor for DiSH preference vector assignment to objects of a certain
 * database.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class DiSHPreprocessor extends AbstractPreprocessor implements PreferenceVectorPreprocessor {
  /**
   * Holds the class specific debug status.
   */
  @SuppressWarnings({"UNUSED_SYMBOL"})
  private static final boolean DEBUG = LoggingConfiguration.DEBUG;
//  private static final boolean DEBUG = true;

  /**
   * The logger of this class.
   */
  private Logger logger = Logger.getLogger(this.getClass().getName());

  /**
   * The default value for epsilon.
   */
  public static final DoubleDistance DEFAULT_EPSILON = new DoubleDistance(0.001);

  /**
   * Option string for parameter epsilon.
   */
  public static final String EPSILON_P = "epsilon";

  /**
   * Description for parameter epsilon.
   */
  public static String EPSILON_D = "<double>a double between 0 and 1 specifying the " +
                                   "maximum radius of the neighborhood to be " +
                                   "considered in each dimension for determination of " +
                                   "the preference vector " +
                                   "(default is " + DEFAULT_EPSILON + ").";


  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = "minpts";


  /**
   * Description for the determination pf the preference vector.
   */
  private static final String CONDITION = "The value of the preference vector in dimension d_i is set to 1 " +
                                          "if the epsilon neighborhood contains more than " + MINPTS_P + " " +
                                          "points and the following condition holds: " +
                                          "for all dimensions d_j: " +
                                          "|neighbors(d_i) intersection neighbors(d_j)| >= " + MINPTS_P +
                                          ".";

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = "<int>positive threshold for minumum numbers of points in the epsilon-" +
                                        "neighborhood of a point. " + CONDITION;

  /**
   * The epsilon value for each dimension;
   */
  private DoubleDistance epsilon;

  /**
   * Threshold for minimum number of points in the neighborhood.
   */
  private int minpts;

  /**
   * Provides a new AdvancedHiSCPreprocessor that computes the preference vector of
   * objects of a certain database.
   */
  public DiSHPreprocessor() {
    super();
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    optionHandler = new OptionHandler(parameterToDescription, getClass().getName());
  }

  /**
   * @see Preprocessor#run(de.lmu.ifi.dbs.database.Database, boolean, boolean)
   */
  public void run(Database<RealVector> database, boolean verbose, boolean time) {
    if (database == null) {
      throw new IllegalArgumentException("Database must not be null!");
    }

    if (database.size() == 0) {
      return;
    }

    try {
      long start = System.currentTimeMillis();
      Progress progress = new Progress("Preprocessing preference vector", database.size());

      String epsString = epsilon.toString();
      int dim = database.dimensionality();
      DimensionSelectingDistanceFunction[] distanceFunctions = initDistanceFunctions(database, dim, verbose, time);

      //noinspection unchecked
      final DistanceFunction<RealVector, DoubleDistance> euklideanDistanceFunction = new EuklideanDistanceFunction();
      euklideanDistanceFunction.setDatabase(database, false, false);

      int processed = 1;
      for (Iterator<Integer> it = database.iterator(); it.hasNext();) {
        StringBuffer msg = new StringBuffer();
        final Integer id = it.next();

        if (DEBUG) {
          msg.append("\n\nid = ").append(id);
          msg.append(" ").append(database.get(id));
//          msg.append(" ").append(database.getAssociation(AssociationID.LABEL, id));
        }

        // determine neighbors in each dimension
        //noinspection unchecked
        Set<Integer>[] allNeighbors = new Set[dim];
        for (int d = 0; d < dim; d++) {
          List<QueryResult<DoubleDistance>> qrList = database.rangeQuery(id, epsString, distanceFunctions[d]);
          allNeighbors[d] = new HashSet<Integer>(qrList.size());
          for (QueryResult<DoubleDistance> qr : qrList) {
            allNeighbors[d].add(qr.getID());
          }
        }

        BitSet preferenceVector = determinePreferenceVector(allNeighbors, msg);
        database.associate(AssociationID.PREFERENCE_VECTOR, id, preferenceVector);
        progress.setProcessed(processed++);

        if (DEBUG) {
          logger.info(msg.toString());
        }

        if (verbose) {
          logger.info("\r" + progress.getTask() + " - " + progress.toString());
        }
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
    catch (ParameterException e) {
      throw new IllegalStateException(e.getMessage(), e);
    }

  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(DiSHPreprocessor.class.getName());
    description.append(" computes the preference vector of objects of a certain database according to the DiSH algorithm.\n");
    description.append(optionHandler.usage("", false));
    return description.toString();
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0) {
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

    // epsilon
    if (optionHandler.isSet(EPSILON_P)) {
      String epsString = optionHandler.getOptionValue(EPSILON_P);
      try {
        epsilon = new DoubleDistance(Double.parseDouble(epsString));
        if (epsilon.getDoubleValue() < 0 || epsilon.getDoubleValue() > 1) {
          throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D);
        }
      }
      catch (NumberFormatException e) {
        throw new WrongParameterValueException(EPSILON_P, epsString, EPSILON_D, e);
      }
    }
    else {
      epsilon = DEFAULT_EPSILON;
    }

    return remainingParameters;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getAttributeSettings()
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(MINPTS_P, Double.toString(minpts));
    mySettings.addSetting(EPSILON_P, epsilon.toString());

    return attributeSettings;
  }

  /**
   * Determines the preference vector according to the specified neighbor ids.
   *
   * @param neighborIDs the list of ids of the neighbors in each dimension
   * @param msg         a string buffer for debug messages
   * @return the preference vector
   */
  private BitSet determinePreferenceVector(Set<Integer>[] neighborIDs, StringBuffer msg) {
    int dimensionality = neighborIDs.length;
    BitSet preferenceVector = new BitSet(dimensionality);

    //noinspection unchecked
    Map<Integer, Set<Integer>> candidates = new HashMap<Integer, Set<Integer>>(dimensionality);
    for (int i = 0; i < dimensionality; i++) {
      Set<Integer> s_i = neighborIDs[i];
      if (s_i.size() > minpts) {
        candidates.put(i, s_i);
      }
    }
    if (DEBUG) {
      msg.append("\ncandidates " + candidates.keySet());
    }

    if (! candidates.isEmpty()) {
      int i = max(candidates);
      Set<Integer> intersection = candidates.remove(i);
      preferenceVector.set(i);
      while (! candidates.isEmpty()) {
        Set<Integer> newIntersection = new HashSet<Integer>();
        i = maxIntersection(candidates, intersection, newIntersection);
        Set<Integer> s_i = candidates.remove(i);
        Util.intersection(intersection, s_i, newIntersection);
        intersection = newIntersection;

        if (intersection.size() < minpts) {
          break;
        }
        else {
          preferenceVector.set(i);
        }
      }
    }

    if (DEBUG) {
      msg.append("\npreference ");
      msg.append(Util.format(dimensionality, preferenceVector));
      msg.append("\n");
    }

    return preferenceVector;
  }

  /**
   * Returns the set with the maximum size contained in the specified map.
   *
   * @param candidates the map containing the sets
   * @return the set with the maximum size
   */
  private int max(Map<Integer, Set<Integer>> candidates) {
    Set<Integer> maxSet = null;
    Integer maxDim = null;
    for (Integer nextDim : candidates.keySet()) {
      Set<Integer> nextSet = candidates.get(nextDim);
      if (maxSet == null || maxSet.size() < nextSet.size()) {
        maxSet = nextSet;
        maxDim = nextDim;
      }
    }

    return maxDim;
  }

  /**
   * Returns the index of the set having the maximum
   * intersection set with the specified set contained in the specified map.
   *
   * @param candidates the map containing the sets
   * @param set the set to intersect with
   * @param result the set to put the result in
   * @return the set with the maximum size
   */
  private int maxIntersection(Map<Integer, Set<Integer>> candidates, Set<Integer> set, Set<Integer> result) {
    Integer maxDim = null;
    for (Integer nextDim : candidates.keySet()) {
      Set<Integer> nextSet = candidates.get(nextDim);
      Set<Integer> nextIntersection = new HashSet<Integer>();
      Util.intersection(set, nextSet, nextIntersection);
      if (result.size() < nextIntersection.size()) {
        result = nextIntersection;
        maxDim = nextDim;
      }
    }

    return maxDim;
  }

  /**
   * Initializes the dimension selecting distancefunctions to determine the preference vectors.
   *
   * @param database       the database storing the objects
   * @param dimensionality the dimensionality of the objects
   * @param verbose        flag to allow verbose messages while performing the algorithm
   * @param time           flag to request output of performance time
   * @return the dimension selecting distancefunctions to determine the preference vectors
   * @throws ParameterException
   */
  private DimensionSelectingDistanceFunction[] initDistanceFunctions(Database<RealVector> database, int dimensionality, boolean verbose, boolean time) throws ParameterException {
    DimensionSelectingDistanceFunction[] distanceFunctions = new DimensionSelectingDistanceFunction[dimensionality];
    for (int d = 0; d < dimensionality; d++) {
      String[] parameters = new String[2];
      parameters[0] = OptionHandler.OPTION_PREFIX + DimensionSelectingDistanceFunction.DIM_P;
      parameters[1] = Integer.toString(d + 1);
      distanceFunctions[d] = new DimensionSelectingDistanceFunction();
      distanceFunctions[d].setParameters(parameters);
      distanceFunctions[d].setDatabase(database, verbose, time);
    }
    return distanceFunctions;
  }

  public DoubleDistance getEpsilon() {
    return epsilon;
  }

  /**
   * Returns minpts.
   *
   * @return minpts
   */
  public int getMinpts() {
    return minpts;
  }

}
