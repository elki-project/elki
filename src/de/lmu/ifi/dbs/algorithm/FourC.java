package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.LocallyWeightedDistanceFunction;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides the 4C algorithm.
 * TODO: implement original preprocessor (delta)
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class FourC<D extends Distance> extends DBSCAN<DoubleVector, D> {
  /**
   * Parameter lambda.
   */
  public static final String LAMBDA_P = "lambda";

  /**
   * Description for parameter lambda.
   */
  public static final String LAMBDA_D = "<lambda>(integer) intrinsinc dimensionality of clusters to be found.";

  /**
   * Keeps lambda.
   */
  private int lambda;

  /**
   * Provides the 4C algorithm.
   */
  public FourC() {
    super();
    parameterToDescription.put(LAMBDA_P + OptionHandler.EXPECTS_VALUE, LAMBDA_D);
    optionHandler = new OptionHandler(parameterToDescription, FourC.class.getName());
  }

  /*
   *
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   *
  public void runInTime(Database<DoubleVector> database) throws IllegalStateException
  {
//        RangeQueryBasedCorrelationDimensionPreprocessor preprocessor = new RangeQueryBasedCorrelationDimensionPreprocessor();
//        String[] preprocessorParams = {OptionHandler.OPTION_PREFIX+RangeQueryBasedCorrelationDimensionPreprocessor.EPSILON_P, epsilon, OptionHandler.OPTION_PREFIX+AbstractCorrelationPCA.BIG_VALUE_P, "50", OptionHandler.OPTION_PREFIX+AbstractCorrelationPCA.SMALL_VALUE_P, "1"};
//        preprocessor.setParameters(preprocessorParams);
//        preprocessor.run(database, isVerbose());

      super.runInTime(database);
  }*/

  /**
   * Replaces the expandCluster function of DBSCAN by the respective  4C method.
   *
   * @see DBSCAN<DoubleVector>#expandCluster(Database, Integer, Progress)
   */
  @Override
  protected void expandCluster(Database<DoubleVector> database, Integer startObjectID, Progress progress) {
    List<QueryResult<D>> neighborhoodIDs = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());
    if (neighborhoodIDs.size() < minpts) {
      noise.add(startObjectID);
      processedIDs.add(startObjectID);
      if (isVerbose()) {
        progress.setProcessed(processedIDs.size());
        System.out.print(status(progress, resultList.size()));
      }
    }
    else {
      List<Integer> currentCluster = new ArrayList<Integer>();
      if (((CorrelationPCA) database.getAssociation(CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA, startObjectID)).getCorrelationDimension() > lambda) {
        noise.add(startObjectID);
        processedIDs.add(startObjectID);
        if (isVerbose()) {
          progress.setProcessed(processedIDs.size());
          System.out.print(status(progress, resultList.size()));
        }
      }
      else {
        List<QueryResult<D>> seeds = database.rangeQuery(startObjectID, epsilon, getDistanceFunction());
        if (seeds.size() < minpts) {
          noise.add(startObjectID);
          processedIDs.add(startObjectID);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            System.out.print(status(progress, resultList.size()));
          }
        }
        else {
          for (QueryResult nextSeed : seeds) {
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
              System.out.print(status(progress, resultList.size()));
            }
          }
          seeds.remove(0);
          processedIDs.add(startObjectID);
          if (isVerbose()) {
            progress.setProcessed(processedIDs.size());
            System.out.print(status(progress, resultList.size()));
          }

          while (seeds.size() > 0) {
            Integer seedID = seeds.remove(0).getID();
            List<QueryResult<D>> seedNeighborhoodIDs = database.rangeQuery(seedID, epsilon, getDistanceFunction());
            if (seedNeighborhoodIDs.size() >= minpts) {
              if (((CorrelationPCA) database.getAssociation(CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA, seedID)).getCorrelationDimension() <= lambda) {
                List<QueryResult<D>> reachables = database.rangeQuery(seedID, epsilon, getDistanceFunction());
                if (reachables.size() >= minpts) {
                  for (QueryResult<D> reachable : reachables) {
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
                        System.out.print(status(progress, resultList.size()));
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
            System.out.print(status(progress, resultList.size()));
          }
        }
      }
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      lambda = Integer.parseInt(optionHandler.getOptionValue(LAMBDA_P));
      if (lambda <= 0) {
        throw new IllegalArgumentException("parameter " + LAMBDA_P + " is supposed to be a positive integer - found: " + lambda);
      }
    }
    catch (NumberFormatException e) {
      throw new IllegalArgumentException("parameter " + LAMBDA_P + " is supposed to be a positive integer - found: " + optionHandler.getOptionValue(LAMBDA_P));
    }
    catch (UnusedParameterException e) {
      throw new IllegalArgumentException("parameter " + LAMBDA_P + " is required");
    }
    if (!(LocallyWeightedDistanceFunction.class.isAssignableFrom(getDistanceFunction().getClass()))) {
      throw new IllegalArgumentException("illegal distance function - must be or extend " + LocallyWeightedDistanceFunction.class.getName());
    }
    return remainingParameters;
  }

  /**
   * @see Algorithm#getAttributeSettings()
   */
  @Override
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = new ArrayList<AttributeSettings>();

    AttributeSettings attributeSettings = new AttributeSettings(this);
    attributeSettings.addSetting(LAMBDA_P, Integer.toString(lambda));

    result.add(attributeSettings);
    result.addAll(super.getAttributeSettings());
    return result;

  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("4C", "Computing Correlation Connected Clusters", "4C identifies local subgroups of data objects sharing a uniform correlation. The algorithm is based on a combination of PCA and density-based clustering (DBSCAN).", "Christian B�hm, Karin Kailing, Peer Kr�ger, Arthur Zimek: Computing Clusters of Correlation Connected Objects, Proc. ACM SIGMOD Int. Conf. on Management of Data, Paris, France, 2004, 455-466");
  }

}
