package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.varianceanalysis.LocalPCA;

import java.util.*;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAA<V extends RealVector<V,?>> extends AbstractAlgorithm<V> {
  /**
   * Parameter for preprocessor.
   */
  public static final String PREPROCESSOR_P = "preprocessor";

  /**
   * Description for parameter preprocessor.
   */
  public static final String PREPROCESSOR_D = "preprocessor to derive partition criterion " +
                                              Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(HiCOPreprocessor.class) +
                                              ".";

  /**
   * Parameter for partition algorithm.
   */
  public static final String PARTITION_ALGORITHM_P = "partAlg";

  /**
   * Description for parameter partition algorithm
   */
  public static final String PARTITION_ALGORITHM_D = "algorithm to apply to each partition" +
                                                     Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Algorithm.class) +
                                                     ".";

  /**
   * Parameter for class of partition database.
   */
  public static final String PARTITION_DATABASE_CLASS_P = "partDB";

  /**
   * Description for parameter partition database.
   */
  public static final String PARTITION_DATABASE_CLASS_D = "database class for each partition " +
                                                          Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(Database.class) +
                                                          ". If this parameter is not set, the databases of the partitions have the same class as the original database.";

  /**
   * Holds the preprocessor.
   */
  protected HiCOPreprocessor<V> preprocessor;

  /**
   * Holds the partitioning algorithm.
   */
  protected Algorithm<V> partitionAlgorithm;

  /**
   * Holds the class of the partition databases.
   */
  protected Class<Database<V>> partitionDatabase;

  /**
   * Holds the parameters of the partition databases.
   */
  protected String[] partitionDatabaseParameters;

  /**
   * Holds the result.
   */
  private PartitionResults<V> result;

  /**
   * Sets the specific parameters additionally to the parameters set by the
   * super-class.
   */
  public COPAA() {
    super();

    //parameter preprocessor
    optionHandler.put(PREPROCESSOR_P, new ClassParameter(PREPROCESSOR_P, PREPROCESSOR_D, HiCOPreprocessor.class));

    // parameter partition algorithm
    optionHandler.put(PARTITION_ALGORITHM_P, new ClassParameter(PARTITION_ALGORITHM_P, PARTITION_ALGORITHM_D, Algorithm.class));

    // parameter partition database class
    ClassParameter pdc = new ClassParameter(PARTITION_DATABASE_CLASS_P, PARTITION_DATABASE_CLASS_D, Database.class);
    pdc.setOptional(true);
    optionHandler.put(PARTITION_DATABASE_CLASS_P, pdc);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
   */
  protected void runInTime(Database<V> database) throws IllegalStateException {
    // preprocessing
    if (isVerbose()) {
      verbose("\ndb size = " + database.size());
      verbose("dimensionality = " + database.dimensionality());
    }
    preprocessor.run(database, isVerbose(), isTime());
    // partitioning
    if (isVerbose()) {
      verbose("\npartitioning...");
    }
    Map<Integer, List<Integer>> partitionMap = new Hashtable<Integer, List<Integer>>();
    Progress partitionProgress = new Progress("Partitioning", database.size());
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
        progress(partitionProgress);
      }
    }

    if (isVerbose()) {
      partitionProgress.setProcessed(database.size());
      progress(partitionProgress);

      for (Integer corrDim : partitionMap.keySet()) {
        List<Integer> list = partitionMap.get(corrDim);
        verbose("Partition " + corrDim + " = " + list.size() + " objects.");
      }
    }

    // running partition algorithm
    result = runPartitionAlgorithm(database, partitionMap);
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
   */
  public Result<V> getResult() {
    return result;
  }

  /**
   * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
   */
  public Description getDescription() {
    return new Description("COPAA",
                           "COrrelation PArtitioning Algorithm",
                           "Partitions a database according to the correlation dimension of its objects and performs an arbitrary algorithm over the partitions.",
                           "unpublished");
  }

  /**
   * Returns the the partitioning algorithm.
   *
   * @return the the partitioning algorithm
   */
  public Algorithm<V> getPartitionAlgorithm() {
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
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // partition algorithm
    String partAlgString = (String) optionHandler.getOptionValue(PARTITION_ALGORITHM_P);
    try {
      // noinspection unchecked
      partitionAlgorithm = Util.instantiate(Algorithm.class, partAlgString);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(PARTITION_ALGORITHM_P, partAlgString, PARTITION_ALGORITHM_D);
    }

    // partition db
    if (optionHandler.isSet(PARTITION_DATABASE_CLASS_P)) {
      String partDBString = (String) optionHandler.getOptionValue(PARTITION_DATABASE_CLASS_P);
      try {
        Database<V> tmpDB = Util.instantiate(Database.class, partDBString);
        remainingParameters = tmpDB.setParameters(remainingParameters);
        partitionDatabaseParameters = tmpDB.getParameters();
        // noinspection unchecked
        partitionDatabase = (Class<Database<V>>) tmpDB.getClass();
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(PARTITION_DATABASE_CLASS_P, partDBString, PARTITION_DATABASE_CLASS_D, e);
      }
    }

    // preprocessor
    String preprocessorString = (String) optionHandler.getOptionValue(PREPROCESSOR_P);
    try {
      preprocessor = Util.instantiate(HiCOPreprocessor.class, preprocessorString);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(PREPROCESSOR_P, preprocessorString, PREPROCESSOR_D, e);
    }

    remainingParameters = preprocessor.setParameters(remainingParameters);

    remainingParameters = partitionAlgorithm.setParameters(remainingParameters);
    partitionAlgorithm.setTime(isTime());
    partitionAlgorithm.setVerbose(isVerbose());
    
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Sets whether the time should be assessed.
   *
   * @param time whether the time should be assessed
   */
  public void setTime(boolean time) {
    super.setTime(time);
    partitionAlgorithm.setTime(time);
  }

  /**
   * Sets whether verbose messages should be printed while executing the
   * algorithm.
   *
   * @param verbose whether verbose messages should be printed while executing the
   *                algorithm
   */
  public void setVerbose(boolean verbose) {
    super.setVerbose(verbose);
    partitionAlgorithm.setVerbose(verbose);
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> result = super.getAttributeSettings();

    result.addAll(preprocessor.getAttributeSettings());
    result.addAll(partitionAlgorithm.getAttributeSettings());
    if (optionHandler.isSet(PARTITION_DATABASE_CLASS_P)) {
      try {
        Database<V> tmpDB = (Database<V>) Util.instantiate(Database.class, partitionDatabase.getName());
        result.addAll(tmpDB.getAttributeSettings());
      }
      catch (UnableToComplyException e) {
        // tested before
        throw new RuntimeException("This should never happen!");
      }
    }

    return result;
  }

  /**
   * Runs the partition algorithm and creates the result.
   *
   * @param database     the database to run this algorithm on
   * @param partitionMap the map of partition IDs to object ids
   */
  protected PartitionResults<V> runPartitionAlgorithm(Database<V> database, Map<Integer, List<Integer>> partitionMap) {
    try {
      Map<Integer, Database<V>> databasePartitions = database.partition(partitionMap, partitionDatabase, partitionDatabaseParameters);
      Map<Integer, Result<V>> results = new Hashtable<Integer, Result<V>>();
      for (Integer partitionID : databasePartitions.keySet()) {
        if (isVerbose()) {
          verbose("\nRunning " + partitionAlgorithm.getDescription().getShortTitle() + " on partition " + partitionID);
        }
        partitionAlgorithm.run(databasePartitions.get(partitionID));
        results.put(partitionID, partitionAlgorithm.getResult());
      }
      return new PartitionResults<V>(database, results);
    }
    catch (UnableToComplyException e) {
      throw new IllegalStateException(e);
    }
  }
}
