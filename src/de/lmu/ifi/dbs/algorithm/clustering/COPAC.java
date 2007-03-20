package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionClusteringResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary clustering algorithm over the partitions.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class COPAC<V extends RealVector<V,?>> extends COPAA<V> implements Clustering<V> {
  /**
   * Description for parameter partition algorithm
   */
  public static final String PARTITION_ALGORITHM_D = "algorithm to apply to each partition " +
                                                     Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
                                                     ".";

  /**
   * Sets the specific parameters additionally to the parameters set by the
   * super-class.
   */
  public COPAC() {
    super();
    // put in the right description
    try {
      optionHandler.remove(PARTITION_ALGORITHM_P);
    }
    catch (UnusedParameterException e) {
      warning(e.getMessage());
    }

    optionHandler.put(PARTITION_ALGORITHM_P, new ClassParameter(PARTITION_ALGORITHM_P, PARTITION_ALGORITHM_D, Clustering.class));
  }

  /**
   * Passes remaining parameters first to the partition algorithm, then to the
   * preprocessor.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);
    if (!(getPartitionAlgorithm() instanceof Clustering)) {
      throw new WrongParameterValueException(PARTITION_ALGORITHM_P, (String) optionHandler.getOptionValue(PARTITION_ALGORITHM_P), PARTITION_ALGORITHM_D);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see Clustering#getResult()
   */
  public ClusteringResult<V> getResult() {
    return (ClusteringResult<V>) super.getResult();
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
        "COPAC",
        "COrrelation PArtition Clustering",
        "Partitions a database according to the correlation dimension of its objects and performs a clustering algorithm over the partitions.",
        "Achtert E., Böhm C., Kriegel H.-P., Kröger P., Zimek A.: " +
        "Robust, Complete, and Efficient Correlation Clustering. " +
        "In Proceedings of the 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007");
  }

  /**
   * Runs the partition algorithm and creates the result.
   *
   * @param database     the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   */
  protected PartitionResults<V> runPartitionAlgorithm(Database<V> database,
                                                               Map<Integer, List<Integer>> partitionMap) {
    try {
      Map<Integer, Database<V>> databasePartitions = database.partition(partitionMap,
                                                                                 partitionDatabase,
                                                                                 partitionDatabaseParameters);
      Map<Integer, ClusteringResult<V>> results = new Hashtable<Integer, ClusteringResult<V>>();
      Clustering<V> partitionAlgorithm = (Clustering<V>) getPartitionAlgorithm();
      for (Integer partitionID : databasePartitions.keySet()) {
        if (isVerbose()) {
          verbose("\nRunning " +
                  partitionAlgorithm.getDescription().getShortTitle() +
                  " on partition " +
                  partitionID);
        }
        partitionAlgorithm.run(databasePartitions.get(partitionID));
        results.put(partitionID, partitionAlgorithm.getResult());
      }
      return new PartitionClusteringResults<V>(database,
                                                        results,
                                                        database.dimensionality());
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
  }

}
