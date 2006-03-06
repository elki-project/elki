package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.clustering.ClusteringResult;
import de.lmu.ifi.dbs.algorithm.result.clustering.PartitionResults;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.pca.LocalPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.*;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class COPAC extends AbstractAlgorithm<RealVector> implements Clustering<RealVector> {
  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_P = "preprocessor";

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_D = "<classname>preprocessor to derive partition criterion - must extend " + CorrelationDimensionPreprocessor.class.getName() + ".";

  /**
   * Parameter for partitioning algorithm.
   */
  public static final String PARTITION_ALGORITHM_P = "partAlg";

  /**
   * Description for parameter partitioning algorithm
   */
  public static final String PARTITION_ALGORITHM_D = "<classname>algorithm to apply to each partition - must implement " + Algorithm.class.getName() + ".";

  /**
   * Holds the preprocessor.
   */
  private CorrelationDimensionPreprocessor preprocessor;

  /**
   * Holds the partitioning algorithm.
   */
  private Clustering<RealVector> partitionAlgorithm;

  /**
   * Holds the result.
   */
  private PartitionResults<RealVector> result;

  /**
   * Sets the specific parameters additionally to the parameters set by the
   * super-class.
   */
  public COPAC() {
    super();
    parameterToDescription.put(PREPROCESSOR_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_D);
    parameterToDescription.put(PARTITION_ALGORITHM_P + OptionHandler.EXPECTS_VALUE, PARTITION_ALGORITHM_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * @see Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<RealVector> database) throws IllegalStateException {

    // preprocessing
    if (isVerbose()) {
      System.out.println("\ndb size = " + database.size());
      System.out.println("dimensionality = " + database.dimensionality());
      System.out.println("\npreprocessing... ");
    }
    preprocessor.run(database, isVerbose());
    // partitioning
    if (isVerbose()) {
      System.out.println("\npartitioning... ");
    }
    Map<Integer, List<Integer>> partitionMap = new Hashtable<Integer, List<Integer>>();
    Progress partitionProgress = new Progress(database.size());
    int processed = 1;

    for (Iterator<Integer> dbiter = database.iterator(); dbiter.hasNext();) {
      Integer id = dbiter.next();
      Integer corrdim = ((LocalPCA) database.getAssociation(AssociationID.LOCAL_PCA, id)).getCorrelationDimension();

      if (!partitionMap.containsKey(corrdim)) {
        partitionMap.put(corrdim, new ArrayList<Integer>());
      }

      partitionMap.get(corrdim).add(id);
      if (isVerbose()) {
        partitionProgress.setProcessed(processed++);
        System.out.print("\r" + partitionProgress.toString());
      }
    }

    if (isVerbose()) {
      partitionProgress.setProcessed(database.size());
      System.out.print("\r" + partitionProgress.toString());
      System.out.println("");
      for (Integer corrDim : partitionMap.keySet()) {
        List<Integer> list = partitionMap.get(corrDim);
        System.out.println("Partition " + corrDim + " = " + list.size() + " objects.");
      }
    }

    // running partition algorithm
    try {
      Map<Integer, Database<RealVector>> databasePartitions = database.partition(partitionMap);
      Map<Integer, ClusteringResult<RealVector>> results = new Hashtable<Integer, ClusteringResult<RealVector>>();
      for (Integer partitionID : databasePartitions.keySet()) {
        if (isVerbose()) {
          System.out.println("\nRunning " + partitionAlgorithm.getDescription().getShortTitle() + " on partition " + partitionID);
        }
        partitionAlgorithm.run(databasePartitions.get(partitionID));
        results.put(partitionID, partitionAlgorithm.getResult());
      }
      result = new PartitionResults<RealVector>(database, results, database.dimensionality());
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * @see Algorithm#getResult()
   */
  public ClusteringResult<RealVector> getResult() {
    return result;
  }

  /**
   * @see Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("COPAC", "Correlation Partitioning", "Partitions a database according to the correlation dimension of its objects and performs an arbitrary algorithm over the partitions.", "unpublished");
  }

  /**
   * Returns the the partitioning algorithm.
   *
   * @return the the partitioning algorithm
   */
  public Algorithm<RealVector> getPartitionAlgorithm() {
    return partitionAlgorithm;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
   */
  @Override
  public String description() {
    StringBuffer description = new StringBuffer();
    description.append(optionHandler.usage("", false));
    description.append('\n');
    description.append("Remaining parameters are firstly given to the partition algorithm, then to the preprocessor.");
    description.append('\n');
    description.append('\n');
    return description.toString();
  }

  /**
   * Passes remaining parameters first to the partition algorithm, then to the
   * preprocessor.
   *
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  @Override
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    try {
      String name = optionHandler.getOptionValue(PARTITION_ALGORITHM_P);
      // noinspection unchecked
      partitionAlgorithm = Util.instantiate(Clustering.class, name);
      preprocessor = Util.instantiate(CorrelationDimensionPreprocessor.class, optionHandler.getOptionValue(PREPROCESSOR_P));
    }
    catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
    remainingParameters = preprocessor.setParameters(remainingParameters);
    return partitionAlgorithm.setParameters(remainingParameters);
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    AttributeSettings settings = result.get(0);
    settings.addSetting(PREPROCESSOR_P, preprocessor.getClass().getName());
    settings.addSetting(PARTITION_ALGORITHM_P, partitionAlgorithm.getClass().getName());

    result.addAll(preprocessor.getAttributeSettings());
    result.addAll(partitionAlgorithm.getAttributeSettings());

    return result;
  }
}
