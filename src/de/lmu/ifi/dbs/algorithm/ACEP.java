package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.*;
import de.lmu.ifi.dbs.data.DoubleVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.linearalgebra.Matrix;

import java.util.*;

/**
 * ACEP Algorithm.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ACEP extends AbstractAlgorithm<DoubleVector> {

  /**
   * Holds the result.
   */
  private Result result;

  /**
   * The copac algorithm.
   */
  private COPAC copac;

  /**
   * The Dependency Derivator algorithm.
   */
  private DependencyDerivator dependencyDerivator;

  /**
   * Sets the parameters in the super-class.
   */
  public ACEP() {
    super();
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }


  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  @SuppressWarnings({"unchecked"})
  public void run(Database<DoubleVector> database) throws IllegalStateException {
    long start = System.currentTimeMillis();
    try {
      // run COPAC
      if (isVerbose()) {
        System.out.println("\napply COPAC... ");
      }
      copac.run(database);

      // get the results from COPAC
      PartitionResults<DoubleVector> partitionResults = (PartitionResults<DoubleVector>) copac.getResult();
      Iterator<Integer> it = partitionResults.partitionsIterator();

      // list for the result
      Map<Integer, Result<DoubleVector>> partitions = new HashMap<Integer, Result<DoubleVector>>();

      // iterate over the partion results
      while (it.hasNext()) {
        Integer partitionID = it.next();
        ClustersPlusNoise<DoubleVector> clustersPlusNoise = (ClustersPlusNoise<DoubleVector>) partitionResults.getResult(partitionID);
        List<Matrix> correlationAnalysisSolutions = new ArrayList<Matrix>();

        // get a database for each cluster
        Map<Integer, List<Integer>> clusters = new HashMap<Integer, List<Integer>>();
        Integer[][] clusterAndNoiseArray = clustersPlusNoise.getClusterAndNoiseArray();
        Database<DoubleVector> partitionDB = clustersPlusNoise.getDatabase();
        for (int i = 0; i < clusterAndNoiseArray.length - 1; i++) {
          clusters.put(i, Arrays.asList(clusterAndNoiseArray[i]));
        }
        Map<Integer, Database<DoubleVector>> clusterDBs = partitionDB.partition(clusters);

        // iterate over each cluster database
        for (int i = 0; i < clusterAndNoiseArray.length - 1; i++) {
//        for (Integer clusterID : clusterDBs.keySet()) {
          Database<DoubleVector> clusterDB = clusterDBs.get(i);

          if (isVerbose()) {
            System.out.println("\nApply Correlation Analysis on Partition " + partitionID + ", Cluster " + (i + 1)
                               + ": " + clusterDB.size() + " objects");
          }

          dependencyDerivator.run(clusterDB);
          CorrelationAnalysisSolution result = (CorrelationAnalysisSolution) dependencyDerivator.getResult();
          correlationAnalysisSolutions.add(result.getSolutionMatrix());
        }

        DBSCAN dbscan = (DBSCAN) copac.getPartitionAlgorithm();

        ClustersPlusNoisePlusCorrelationAnalysis<DoubleVector> r =
          new ClustersPlusNoisePlusCorrelationAnalysis<DoubleVector>(clusterAndNoiseArray, partitionDB,
                                                                     dbscan.getEpsilon(), dbscan.getMinpts(),
                                                                     correlationAnalysisSolutions.toArray(new Matrix[correlationAnalysisSolutions.size()]),
                                                                     dependencyDerivator.NF);
        partitions.put(partitionID, r);

      }
      result = new PartitionResults<DoubleVector>(partitions);

      long end = System.currentTimeMillis();
      if (isTime()) {
        long elapsedTime = end - start;
        System.out.println(this.getClass().getName() + " runtime: " + elapsedTime + " milliseconds.");
      }
    }
    catch (UnableToComplyException e) {
      e.fillInStackTrace();
      throw new IllegalArgumentException(e);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public Result getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("ACEP",
                           "algorithm without name ;-)",
                           "First it partitions a database according to the correlation dimension of its objects and " +
                           "performs DBSCAN over the partitions. " +
                           "Second an equality-system describing dependencies between attributes " +
                           "in a correlation-cluster is derived.",
                           "unpublished");
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  @Override
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("", false));
    description.append('\n');
    description.append("Remaining parameters are firstly given to the COPAC algorithm and then to the Dependency" +
                       "Derivator algorithm.");
    description.append('\n');
    description.append('\n');
    return description.toString();
  }

  /**
   * Passes remaining parameters first to the partition algorithm,
   * then to the preprocessor.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);

    ArrayList<String> params = new ArrayList<String>(Arrays.asList(remainingParameters));
    // -partAlg
    params.add(OptionHandler.OPTION_PREFIX + COPAC.PARTITION_ALGORITHM_P);
    params.add(DBSCAN.class.getName());
    // -verbose
    if (isVerbose()) {
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
      params.add(OptionHandler.OPTION_PREFIX + AbstractAlgorithm.VERBOSE_F);
    }

    remainingParameters = params.toArray(new String[params.size()]);

    try {
      copac = new COPAC();
      dependencyDerivator = new DependencyDerivator();
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    remainingParameters = copac.setParameters(remainingParameters);
    return dependencyDerivator.setParameters(remainingParameters);
  }


}
