package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClustersPlusNoise;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary clustering algorithm over the partitions.
 *
 * @param <V> the type of Realvector handled by this Algorithm
 * @author Arthur Zimek
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
    // put in the proper partition algorithm
    try { // TODO: instead remove, create Option in super class, here change description (and restriction class?)
      optionHandler.remove(PARTITION_ALGORITHM_P);
    }
    catch (UnusedParameterException e) {
      warning(e.getMessage());
    }

    // noinspection unchecked
    ClassParameter<Clustering<V>> partAlg = new ClassParameter(PARTITION_ALGORITHM_P, PARTITION_ALGORITHM_D, Clustering.class);
    optionHandler.put(partAlg);
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
    return remainingParameters;
  }

  /**
   * @see Clustering#getResult()
   */
  @Override
  public ClusteringResult<V> getResult() {
    return (ClusteringResult<V>) super.getResult();
  }

  /**
   * @see Algorithm#getDescription()
   */
  @Override
  public Description getDescription() {
    return new Description(
        "COPAC",
        "COrrelation PArtition Clustering",
        "Partitions a database according to the correlation dimension of its objects and performs a clustering algorithm over the partitions.",
        "Achtert E., B\u00F6hm C., Kriegel H.-P., Kr\u00F6ger P., Zimek A.: " +
        "Robust, Complete, and Efficient Correlation Clustering. " +
        "In Proceedings of the 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007");
  }

  /**
   * Runs the partition algorithm and creates the result.
   *
   * @param database     the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   */
  @Override
  protected PartitionResults<V> runPartitionAlgorithm(Database<V> database,
                                                               Map<Integer, List<Integer>> partitionMap) {
    try {
      Map<Integer, Database<V>> databasePartitions = database.partition(partitionMap,
                                                                                 partitionDatabase,
                                                                                 partitionDatabaseParameters);
      Map<Integer, ClusteringResult<V>> results = new Hashtable<Integer, ClusteringResult<V>>();
      Clustering<V> partitionAlgorithm = (Clustering<V>) getPartitionAlgorithm();
      for (Integer partitionID : databasePartitions.keySet()) {
        // noise partition
        if (partitionID == database.dimensionality()) {
          Database<V> noiseDB = databasePartitions.get(partitionID);
          Integer[][] noise = new Integer[1][noiseDB.size()];
          int i = 0;
          for (Iterator<Integer> it = noiseDB.iterator(); it.hasNext();) {
            noise[0][i++] = it.next();
          }
          ClusteringResult<V> r = new ClustersPlusNoise<V>(noise, noiseDB);
          results.put(partitionID, r);
        }
        else {
          if (isVerbose()) {
            verbose("\nRunning " +
                    partitionAlgorithm.getDescription().getShortTitle() +
                    " on partition " +
                    partitionID);
          }
          partitionAlgorithm.run(databasePartitions.get(partitionID));
          results.put(partitionID, partitionAlgorithm.getResult());
        }
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
