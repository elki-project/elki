package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.distancefunction.AbstractLocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.preprocessing.ProjectedDBSCANPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.*;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.DistanceFunctionGlobalPatternConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GlobalParameterConstraint;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * Provides an abstract algorithm requiring a VarianceAnalysisPreprocessor.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class ProjectedDBSCAN<O extends RealVector, P extends ProjectedDBSCANPreprocessor> extends AbstractAlgorithm<O> implements
    Clustering<O> {

  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = DBSCAN.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "the maximum radius of the neighborhood to be considered, must be suitable to "
                                         + LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = DBSCAN.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = DBSCAN.MINPTS_D;

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function to determine the distance between database objects "
                                                   + Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(AbstractLocallyWeightedDistanceFunction.class) + ". Default: "
                                                   + DEFAULT_DISTANCE_FUNCTION;

  /**
   * Epsilon.
   */
  protected String epsilon;

  /**
   * Minimum points.
   */
  protected int minpts;

  /**
   * Parameter lambda.
   */
  public static final String LAMBDA_P = "lambda";

  /**
   * Description for parameter lambda.
   */
  public static final String LAMBDA_D = "a positive integer specifiying the intrinsic dimensionality of clusters to be found.";

  /**
   * Keeps lambda.
   */
  private int lambda;

  /**
   * Holds a list of clusters found.
   */
  private List<List<Integer>> resultList;

  /**
   * Provides the result of the algorithm.
   */
  private ClustersPlusNoise<O> result;

  /**
   * Holds a set of noise.
   */
  private Set<Integer> noise;

  /**
   * Holds a set of processed ids.
   */
  private Set<Integer> processedIDs;

  /**
   * The distance function.
   */
  private AbstractLocallyWeightedDistanceFunction<O> distanceFunction;

  /**
   * Provides the abstract algorithm for variance analysis based DBSCAN.
   */
  protected ProjectedDBSCAN() {
    super();
    // epsilon
    // TODO pattern distance constraint!
    PatternParameter eps_param = new PatternParameter(EPSILON_P, EPSILON_D);
    optionHandler.put(EPSILON_P, eps_param);
    // minpts
    optionHandler.put(MINPTS_P, new IntParameter(MINPTS_P, MINPTS_D, new GreaterConstraint(0)));
    // lambda
    optionHandler.put(LAMBDA_P, new IntParameter(LAMBDA_P, LAMBDA_D, new GreaterConstraint(0)));
    // parameter distance function
    ClassParameter distance = new ClassParameter(DISTANCE_FUNCTION_P, DISTANCE_FUNCTION_D,
                                                 AbstractLocallyWeightedDistanceFunction.class);
    distance.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
    optionHandler.put(DISTANCE_FUNCTION_P, distance);

    //global parameter constraint epsilon <-> distance function
    GlobalParameterConstraint con = new DistanceFunctionGlobalPatternConstraint(eps_param, distance);
    optionHandler.setGlobalParameterConstraint(con);

  }

  /**
   * @see AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<O> database) throws IllegalStateException {
    if (isVerbose()) {
      verbose("");
    }
    try {
      Progress progress = new Progress("Clustering", database.size());
      resultList = new ArrayList<List<Integer>>();
      noise = new HashSet<Integer>();
      processedIDs = new HashSet<Integer>(database.size());
      distanceFunction.setDatabase(database, isVerbose(), isTime());
      if (isVerbose()) {
        verbose("\nClustering:");
      }
      if (database.size() >= minpts) {
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
          Integer id = iter.next();
          if (!processedIDs.contains(id)) {
            expandCluster(database, id, progress);
            if (processedIDs.size() == database.size() && noise.size() == 0) {
              break;
            }
          }
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            progress(progress, resultList.size());
          }
        }
      }
      else {
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
          Integer id = iter.next();
          noise.add(id);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            progress(progress, resultList.size());
          }
        }
      }

      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        progress(progress, resultList.size());
      }

      Integer[][] resultArray = new Integer[resultList.size() + 1][];
      int i = 0;
      for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
        resultArray[i] = resultListIter.next().toArray(new Integer[0]);
      }

      resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
      result = new ClustersPlusNoise<O>(resultArray, database);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        progress(progress, resultList.size());
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }
  }

  /**
   * ExpandCluster function of DBSCAN.
   */
  protected void expandCluster(Database<O> database, Integer startObjectID, Progress progress) {
    String label = (String) database.getAssociation(AssociationID.LABEL, startObjectID);
    Integer corrDim = (Integer) database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, startObjectID);

    if (this.debug) {
      debugFine("\nEXPAND CLUSTER id = " + startObjectID + " " + label + " " + corrDim + "\n#clusters: " + resultList.size());

    }

    // euclidean epsilon neighborhood < minpts OR local dimensionality >
    // lambda -> noise
    if (corrDim == null || corrDim > lambda) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        progress(progress, resultList.size());
      }
      return;
    }

    // compute weighted epsilon neighborhood
    List<QueryResult<DoubleDistance>> seeds = database.rangeQuery(startObjectID, epsilon, distanceFunction);
    // neighbors < minPts -> noise
    if (seeds.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        progress(progress, resultList.size());
      }
      return;
    }

    // try to expand the cluster
    List<Integer> currentCluster = new ArrayList<Integer>();
    for (QueryResult seed : seeds) {
      Integer nextID = seed.getID();

      Integer nextID_corrDim = (Integer) database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, nextID);
      // nextID is not reachable from start object
      if (nextID_corrDim > lambda)
        continue;

      if (!processedIDs.contains(nextID)) {
        currentCluster.add(nextID);
        processedIDs.add(nextID);
      }
      else if (noise.contains(nextID)) {
        currentCluster.add(nextID);
        noise.remove(nextID);
      }
    }
    seeds.remove(0);

    while (seeds.size() > 0) {
      Integer q = seeds.remove(0).getID();
      Integer corrDim_q = (Integer) database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, q);
      // q forms no lambda-dim hyperplane
      if (corrDim_q > lambda)
        continue;

      List<QueryResult<DoubleDistance>> reachables = database.rangeQuery(q, epsilon, distanceFunction);
      if (reachables.size() > minpts) {
        for (QueryResult<DoubleDistance> r : reachables) {
          Integer corrDim_r = (Integer) database.getAssociation(AssociationID.LOCAL_DIMENSIONALITY, r.getID());
          // r is not reachable from q
          if (corrDim_r > lambda)
            continue;

          boolean inNoise = noise.contains(r.getID());
          boolean unclassified = !processedIDs.contains(r.getID());
          if (inNoise || unclassified) {
            if (unclassified) {
              seeds.add(r);
            }
            currentCluster.add(r.getID());
            processedIDs.add(r.getID());
            if (inNoise) {
              noise.remove(r.getID());
            }
            if (isVerbose()) {
              progress.setProcessed(processedIDs.size());
              int numClusters = currentCluster.size() > minpts ? resultList.size() + 1 : resultList.size();
              progress(progress, numClusters);
            }
          }
        }
      }

      if (processedIDs.size() == database.size() && noise.size() == 0) {
        break;
      }
    }

    if (currentCluster.size() >= minpts) {
      resultList.add(currentCluster);
    }
    else {
      for (Integer id : currentCluster) {
        noise.add(id);
      }
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
    }

    if (isVerbose()) {
      progress.setProcessed(processedIDs.size());
      progress(progress, resultList.size());
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // distance function
    String className = (String) optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
      distanceFunction = Util.instantiate(AbstractLocallyWeightedDistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P, className, DISTANCE_FUNCTION_D, e);
    }

    // epsilon
    epsilon = (String) optionHandler.getOptionValue(EPSILON_P);

    // minpts
    minpts = (Integer) optionHandler.getOptionValue(MINPTS_P);

    // lambda
    lambda = (Integer) optionHandler.getOptionValue(LAMBDA_P);

    // parameters for the distance function
    String[] distanceFunctionParameters = new String[remainingParameters.length + 7];
    System.arraycopy(remainingParameters, 0, distanceFunctionParameters, 7, remainingParameters.length);

    // omit preprocessing flag
    distanceFunctionParameters[0] = OptionHandler.OPTION_PREFIX + AbstractLocallyWeightedDistanceFunction.OMIT_PREPROCESSING_F;
    // preprocessor
    distanceFunctionParameters[1] = OptionHandler.OPTION_PREFIX + AbstractLocallyWeightedDistanceFunction.PREPROCESSOR_CLASS_P;
    distanceFunctionParameters[2] = preprocessorClass().getName();
    // preprocessor epsilon
    distanceFunctionParameters[3] = OptionHandler.OPTION_PREFIX + ProjectedDBSCANPreprocessor.EPSILON_P;
    distanceFunctionParameters[4] = epsilon;
    // preprocessor minpts
    distanceFunctionParameters[5] = OptionHandler.OPTION_PREFIX + ProjectedDBSCANPreprocessor.MINPTS_P;
    distanceFunctionParameters[6] = Integer.toString(minpts);

    distanceFunction.setParameters(distanceFunctionParameters);

    setParameters(args, remainingParameters);
    return remainingParameters;

  }

  /**
   * @see Algorithm#getAttributeSettings()
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();

    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(LAMBDA_P, Integer.toString(lambda));
    mySettings.addSetting(EPSILON_P, epsilon);
    mySettings.addSetting(MINPTS_P, Integer.toString(minpts));

    attributeSettings.addAll(distanceFunction.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Returns the class actually used as
   * {@link ProjectedDBSCANPreprocessor VarianceAnalysisPreprocessor}.
   *
   * @return the class actually used as
   *         {@link ProjectedDBSCANPreprocessor VarianceAnalysisPreprocessor}
   */
  public abstract Class<P> preprocessorClass();

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public ClustersPlusNoise<O> getResult() {
    return result;
  }
}