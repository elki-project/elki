package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.pca.AbstractLocalPCA;
import de.lmu.ifi.dbs.pca.LocalPCA;
import de.lmu.ifi.dbs.preprocessing.FourCPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Provides the 4C algorithm.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourC extends AbstractAlgorithm<RealVector> implements Clustering<RealVector> {
  /**
   * Parameter for epsilon.
   */
  public static final String EPSILON_P = DBSCAN.EPSILON_P;

  /**
   * Description for parameter epsilon.
   */
  public static final String EPSILON_D = "<epsilon> the maximum radius of the neighborhood to" +
                                         "be considerd, must be suitable to " +
                                         LocallyWeightedDistanceFunction.class.getName();

  /**
   * Parameter minimum points.
   */
  public static final String MINPTS_P = DBSCAN.MINPTS_P;

  /**
   * Description for parameter minimum points.
   */
  public static final String MINPTS_D = DBSCAN.MINPTS_P;

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
  public static final String LAMBDA_D = "<lambda> a positive integer specifiying the intrinsic dimensionality of clusters to be found.";

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
  private ClustersPlusNoise<RealVector> result;

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
  private LocallyWeightedDistanceFunction distanceFunction = new LocallyWeightedDistanceFunction();

  /**
   * Provides the 4C algorithm.
   */
  public FourC() {
    super();
    parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
    parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
    parameterToDescription.put(LAMBDA_P + OptionHandler.EXPECTS_VALUE, LAMBDA_D);
    parameterToDescription.put(FourCPreprocessor.DELTA_P + OptionHandler.EXPECTS_VALUE, FourCPreprocessor.DELTA_D);
    optionHandler = new OptionHandler(parameterToDescription, FourC.class.getName());
  }

  /**
   * @see AbstractAlgorithm#runInTime(Database)
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {
    if (isVerbose()) {
      System.out.println();
    }
    try {
      Progress progress = new Progress(database.size());
      resultList = new ArrayList<List<Integer>>();
      noise = new HashSet<Integer>();
      processedIDs = new HashSet<Integer>(database.size());
      distanceFunction.setDatabase(database, isVerbose(), isTime());
      if (isVerbose()) {
        System.out.println("\nClustering:");
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
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
      }
      else {
        for (Iterator<Integer> iter = database.iterator(); iter.hasNext();) {
          Integer id = iter.next();
          noise.add(id);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
      }

      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print(Util.status(progress, resultList.size()));
      }

      Integer[][] resultArray = new Integer[resultList.size() + 1][];
      int i = 0;
      for (Iterator<List<Integer>> resultListIter = resultList.iterator(); resultListIter.hasNext(); i++) {
        resultArray[i] = resultListIter.next().toArray(new Integer[0]);
      }

      resultArray[resultArray.length - 1] = noise.toArray(new Integer[0]);
      result = new ClustersPlusNoise<RealVector>(resultArray, database);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print(Util.status(progress, resultList.size()));
        System.out.println();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException(e);
    }

  }

  /**
   * ExpandCluster function of 4C.
   */
  protected void expandCluster(Database<RealVector> database, Integer startObjectID, Progress progress) {
    List<QueryResult<DoubleDistance>> neighborhoodIDs = database.rangeQuery(startObjectID, epsilon, distanceFunction);
    if (neighborhoodIDs.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print(Util.status(progress, resultList.size()));
      }
    }
    else {
      List<Integer> currentCluster = new ArrayList<Integer>();
      if (((LocalPCA) database.getAssociation(AssociationID.LOCAL_PCA, startObjectID)).getCorrelationDimension() > lambda)
      {
        noise.add(startObjectID);
        processedIDs.add(startObjectID);
        if (isVerbose()) {
          progress.setProcessed(processedIDs.size());
          System.out.print(Util.status(progress, resultList.size()));
        }
      }
      else {
        List<QueryResult<DoubleDistance>> seeds = database.rangeQuery(startObjectID, epsilon, distanceFunction);
        if (seeds.size() < minpts) {
          noise.add(startObjectID);
          processedIDs.add(startObjectID);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
        else {
          for (QueryResult<DoubleDistance> nextSeed : seeds) {
            Integer nextID = nextSeed.getID();
            if (!processedIDs.contains(nextID)) {
              currentCluster.add(nextID);
              processedIDs.add(nextID);
            }
            else if (noise.contains(nextID)) {
              currentCluster.add(nextID);
              noise.remove(nextID);
            }
            if (isVerbose()) {
              progress.setProcessed(processedIDs.size());
              System.out.print(Util.status(progress, resultList.size()));
            }
          }
          seeds.remove(0);
          processedIDs.add(startObjectID);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            System.out.print(Util.status(progress, resultList.size()));
          }

          while (seeds.size() > 0) {
            Integer seedID = seeds.remove(0).getID();
            List<QueryResult<DoubleDistance>> seedNeighborhoodIDs = database.rangeQuery(seedID, epsilon, distanceFunction);
            if (seedNeighborhoodIDs.size() >= minpts) {
              if (((LocalPCA) database.getAssociation(AssociationID.LOCAL_PCA, seedID)).getCorrelationDimension() <= lambda)
              {
                List<QueryResult<DoubleDistance>> reachables = database.rangeQuery(seedID, epsilon, distanceFunction);
                if (reachables.size() >= minpts) {
                  for (QueryResult<DoubleDistance> reachable : reachables) {
                    boolean inNoise = noise.contains(reachable.getID());
                    boolean unclassified = !processedIDs.contains(reachable.getID());
                    if (inNoise || unclassified) {
                      if (unclassified) {
                        seeds.add(reachable);
                      }
                      currentCluster.add(reachable.getID());
                      processedIDs.add(reachable.getID());
                      if (inNoise) {
                        noise.remove(reachable.getID());
                      }
                      if (isVerbose()) {
                        progress.setProcessed(processedIDs.size());
                        System.out.print(Util.status(progress, resultList.size()));
                      }
                    }
                  }
                }
              }
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
            System.out.print(Util.status(progress, resultList.size()));
          }
        }
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    epsilon = optionHandler.getOptionValue(EPSILON_P);
    try {
      // test whether epsilon is compatible with distance function
      distanceFunction.valueOf(epsilon);
    }
    catch (IllegalArgumentException e) {
      throw new WrongParameterValueException(EPSILON_P, epsilon, EPSILON_D);
    }

    // minpts
    String minptsString = optionHandler.getOptionValue(MINPTS_P);
    try {
      minpts = Integer.parseInt(minptsString);
      if (minpts <= 0)
        throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(MINPTS_P, minptsString, MINPTS_D, e);
    }

    // lambda
    String lambdaString = optionHandler.getOptionValue(LAMBDA_P);
    try {
      lambda = Integer.parseInt(lambdaString);
      if (lambda <= 0) {
        throw new WrongParameterValueException(LAMBDA_P, lambdaString, LAMBDA_D);
      }
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(LAMBDA_P, lambdaString, LAMBDA_D, e);
    }

    //delta
    String deltaString;
    if (optionHandler.isSet(FourCPreprocessor.DELTA_P)) {
      deltaString = optionHandler.getOptionValue(FourCPreprocessor.DELTA_P);
    }
    else {
      deltaString = Double.toString(FourCPreprocessor.DEFAULT_DELTA);
    }

    // parameters for the distance function
    List<String> distanceFunctionParameters = new ArrayList<String>();
    // big value for PCA
    distanceFunctionParameters.add(OptionHandler.OPTION_PREFIX + AbstractLocalPCA.BIG_VALUE_P);
    distanceFunctionParameters.add("50");
    // small value for PCA
    distanceFunctionParameters.add(OptionHandler.OPTION_PREFIX + AbstractLocalPCA.SMALL_VALUE_P);
    distanceFunctionParameters.add("1");
    // delta
    distanceFunctionParameters.add(OptionHandler.OPTION_PREFIX + FourCPreprocessor.DELTA_P);
    distanceFunctionParameters.add(deltaString);
    // preprocessor
    distanceFunctionParameters.add(OptionHandler.OPTION_PREFIX + LocallyWeightedDistanceFunction.PREPROCESSOR_CLASS_P);
    distanceFunctionParameters.add(FourCPreprocessor.class.getName());
    // preprocessor epsilon
    distanceFunctionParameters.add(OptionHandler.OPTION_PREFIX + FourCPreprocessor.EPSILON_P);
    distanceFunctionParameters.add(epsilon);

    distanceFunction.setParameters(distanceFunctionParameters.toArray(new String[distanceFunctionParameters.size()]));

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
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("4C", "Computing Correlation Connected Clusters", "4C identifies local subgroups of data objects sharing a uniform correlation. The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).", "Christian B�hm, Karin Kailing, Peer Kr�ger, Arthur Zimek: Computing Clusters of Correlation Connected Objects, Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public ClustersPlusNoise<RealVector> getResult() {
    return result;
  }

}
