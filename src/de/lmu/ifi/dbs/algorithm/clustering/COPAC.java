package de.lmu.ifi.dbs.algorithm.clustering;

import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class COPAC extends COPAA implements Clustering<RealVector> {
 

 
  /**
   * Description for parameter partitioning algorithm
   */
  public static final String PARTITION_ALGORITHM_D = "algorithm to apply to each partition " +
                                                     Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Clustering.class) +
                                                     ".";

  /**
   * Sets the specific parameters additionally to the parameters set by the
   * super-class.
   */
  public COPAC(){
    super();
    // put in the right description
    try{
    optionHandler.remove(PARTITION_ALGORITHM_P);
    }
    catch(UnusedParameterException e){
    	warning(e.getMessage());
    }
    //TODO default parameter value??
    optionHandler.put(PARTITION_ALGORITHM_P, new ClassParameter(PARTITION_ALGORITHM_P,PARTITION_ALGORITHM_D,Clustering.class));
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
      throw new WrongParameterValueException(PARTITION_ALGORITHM_P,
                                             optionHandler.getOptionValue(PARTITION_ALGORITHM_P),
                                             PARTITION_ALGORITHM_D);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * @see Clustering#getResult()
   */
  public ClusteringResult<RealVector> getResult() {
    return (ClusteringResult<RealVector>) super.getResult();
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description(
    "COPAC",
    "COrrelation PArtition Clustering",
    "Partitions a database according to the correlation dimension of its objects and performs a clustering algorithm over the partitions.",
    "unpublished");
  }

  /**
   * Runs the partition algorithm and creates the result.
   *
   * @param database     the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   */
  protected PartitionResults<RealVector> runPartitionAlgorithm(
  Database<RealVector> database,
  Map<Integer, List<Integer>> partitionMap) {
    try {
      Map<Integer, Database<RealVector>> databasePartitions = database
      .partition(partitionMap, partitionDatabase,
                 partitionDatabaseParameters);
      Map<Integer, ClusteringResult<RealVector>> results = new Hashtable<Integer, ClusteringResult<RealVector>>();
      Clustering<RealVector> partitionAlgorithm = (Clustering<RealVector>) getPartitionAlgorithm();
      for (Integer partitionID : databasePartitions.keySet()) {
        if (isVerbose()) {
        	verbose("\nRunning "
                      + partitionAlgorithm.getDescription()
          .getShortTitle() + " on partition "
                           + partitionID);
        }
        partitionAlgorithm.run(databasePartitions.get(partitionID));
        results.put(partitionID, partitionAlgorithm.getResult());
      }
      return new PartitionClusteringResults<RealVector>(database,
                                                        results, database.dimensionality());
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
  }

}
