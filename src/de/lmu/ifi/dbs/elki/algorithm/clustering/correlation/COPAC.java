package de.lmu.ifi.dbs.elki.algorithm.clustering.correlation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroup;
import de.lmu.ifi.dbs.elki.data.DatabaseObjectGroupCollection;
import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.data.cluster.Cluster;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
import de.lmu.ifi.dbs.elki.data.model.DimensionModel;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.database.AssociationID;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.Description;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.progress.FiniteProgress;

/**
 * Provides the COPAC algorithm, an algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary clustering algorithm over the partitions.
 * <p>Reference:
 * Achtert E., B&ouml;hm C., Kriegel H.-P., Kr&ouml;ger P., Zimek A.:
 * Robust, Complete, and Efficient Correlation Clustering.
 * <br>In Proc. 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007
 * </p>
 *
 * @author Arthur Zimek
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class COPAC<V extends RealVector<V, ?>> extends AbstractAlgorithm<V, Clustering<Model>> implements ClusteringAlgorithm<Clustering<Model>,V> {

    /**
     * OptionID for {@link #PREPROCESSOR_PARAM}
     */
    public static final OptionID PREPROCESSOR_ID = OptionID.getOrCreateOptionID(
        "copac.preprocessor",
        "Preprocessor to derive partition criterion.");

    /**
     * Parameter to specify the preprocessor to derive partition criterion,
     * must extend {@link de.lmu.ifi.dbs.elki.preprocessing.HiCOPreprocessor}.
     * <p>Key: {@code -copac.preprocessor} </p>
     */
    private final ClassParameter<HiCOPreprocessor<V>> PREPROCESSOR_PARAM =
        new ClassParameter<HiCOPreprocessor<V>>(PREPROCESSOR_ID, HiCOPreprocessor.class);

    /**
     * Holds the instance of preprocessor specified by {@link #PREPROCESSOR_PARAM}.
     */
    private HiCOPreprocessor<V> preprocessor;

    /**
     * OptionID for {@link #PARTITION_ALGORITHM_PARAM}
     */
    public static final OptionID PARTITION_ALGORITHM_ID = OptionID.getOrCreateOptionID(
        "copac.partitionAlgorithm",
        "Clustering algorithm to apply to each partition.");

    /**
     * Parameter to specify the clustering algorithm to apply to each partition,
     * must extend {@link de.lmu.ifi.dbs.elki.algorithm.clustering.ClusteringAlgorithm}.
     * <p>Key: {@code -copac.partitionAlgorithm} </p>
     */
    protected final ClassParameter<ClusteringAlgorithm<Clustering<Model>,V>> PARTITION_ALGORITHM_PARAM =
        new ClassParameter<ClusteringAlgorithm<Clustering<Model>,V>>(PARTITION_ALGORITHM_ID, ClusteringAlgorithm.class);

    /**
     * Holds the instance of the partitioning algorithm specified by {@link #PARTITION_ALGORITHM_PARAM}.
     */
    private ClusteringAlgorithm<Clustering<Model>,V> partitionAlgorithm;

    /**
     * OptionID for {#PARTITION_DB_PARAM}
     */
    public static final OptionID PARTITION_DB_ID = OptionID.getOrCreateOptionID(
        "copac.partitionDB",
        "Database class for each partition. "+
        "If this parameter is not set, the databases of the partitions have " +
        "the same class as the original database."
    );

    /**
     * Parameter to specify the database class for each partition,
     * must extend {@link de.lmu.ifi.dbs.elki.database.Database}.
     * <p>Key: {@code -copac.partitionDB} </p>
     */
    private final ClassParameter<Database<V>> PARTITION_DB_PARAM =
        new ClassParameter<Database<V>>(PARTITION_DB_ID, Database.class, true);

    /**
     * Holds the instance of the partition database specified by {@link #PARTITION_DB_PARAM}.
     */
    private Class<Database<V>> partitionDatabase;

    /**
     * Holds the parameters of the partition databases.
     */
    private String[] partitionDatabaseParameters;

    /**
     * Holds the result.
     */
    private Clustering<Model> result;


    /**
     * Adds parameters
     * {@link #PREPROCESSOR_PARAM}, {@link #PARTITION_ALGORITHM_PARAM}, and {@link #PARTITION_DB_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public COPAC() {
        super();
        //parameter preprocessor
        addOption(PREPROCESSOR_PARAM);
        // parameter partition algorithm
        addOption(PARTITION_ALGORITHM_PARAM);
        // parameter partition database class
        addOption(PARTITION_DB_PARAM);
    }

    /**
     * Performs the COPAC algorithm on the given database.
     */
    @Override
    protected Clustering<Model> runInTime(Database<V> database) throws IllegalStateException {
        // preprocessing
        if (logger.isVerbose()) {
          logger.verbose("db size = " + database.size());
            logger.verbose("dimensionality = " + database.dimensionality());
        }
        preprocessor.run(database, isVerbose(), isTime());
        // partitioning
        if (logger.isVerbose()) {
          logger.verbose("\nPartitioning...");
        }
        Map<Integer, List<Integer>> partitionMap = new Hashtable<Integer, List<Integer>>();
        FiniteProgress partitionProgress = new FiniteProgress("Partitioning", database.size());
        int processed = 1;

        for (Integer id : database) {
            Integer corrdim = (database.getAssociation(AssociationID.LOCAL_PCA, id)).getCorrelationDimension();

            if (!partitionMap.containsKey(corrdim)) {
                partitionMap.put(corrdim, new ArrayList<Integer>());
            }

            partitionMap.get(corrdim).add(id);
            if (logger.isVerbose()) {
                partitionProgress.setProcessed(processed++);
                logger.progress(partitionProgress);
            }
        }

        if (logger.isVerbose()) {
            partitionProgress.setProcessed(database.size());
            logger.progress(partitionProgress);

            for (Integer corrDim : partitionMap.keySet()) {
                List<Integer> list = partitionMap.get(corrDim);
                logger.verbose("Partition " + corrDim + " = " + list.size() + " objects.");
            }
        }

        // running partition algorithm
        runPartitionAlgorithm(database, partitionMap);
        return result;
    }

    public Clustering<Model> getResult() {
        return result;
    }

    public Description getDescription() {
        return new Description(
            "COPAC",
            "COrrelation PArtition Clustering",
            "Partitions a database according to the correlation dimension of its objects and performs " +
                "a clustering algorithm over the partitions.",
            "Achtert E., B\u00F6hm C., Kriegel H.-P., Kr\u00F6ger P., Zimek A.: " +
                "Robust, Complete, and Efficient Correlation Clustering. " +
                "In Proc. 7th SIAM International Conference on Data Mining (SDM'07), Minneapolis, MN, 2007");
    }

    /**
     * Calls the super method
     * and instantiates {@link #partitionAlgorithm} according to the value of parameter
     * {@link #PARTITION_ALGORITHM_PARAM},
     * {@link #partitionDatabase} according to the value of parameter {@link #PARTITION_DB_PARAM} (if specified),
     * and {@link #preprocessor} according to the value of parameter {@link #PREPROCESSOR_PARAM}.
     * <p/>
     * The remaining parameters are passed to the {@link #partitionAlgorithm},
     * then to the {@link #partitionDatabase} and afterwards to the {@link #preprocessor}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        // partition db
        if (PARTITION_DB_PARAM.isSet()) {
            Database<V> tmpDB = PARTITION_DB_PARAM.instantiateClass();
            remainingParameters = tmpDB.setParameters(remainingParameters);
            partitionDatabaseParameters = tmpDB.getParameters();
            // FIXME: we're leaking a reference here.
            addParameterizable(tmpDB);
            partitionDatabase = (Class<Database<V>>) tmpDB.getClass();
        }

        // preprocessor
        preprocessor = PREPROCESSOR_PARAM.instantiateClass();
        remainingParameters = preprocessor.setParameters(remainingParameters);
        addParameterizable(preprocessor);

        // partition algorithm
        partitionAlgorithm = PARTITION_ALGORITHM_PARAM.instantiateClass();
        String[] partitiongAlgorithmParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, partitiongAlgorithmParameters, 0, remainingParameters.length);
        partitionAlgorithm.setTime(isTime());
        partitionAlgorithm.setVerbose(isVerbose());
        remainingParameters = partitionAlgorithm.setParameters(partitiongAlgorithmParameters);
        addParameterizable(partitionAlgorithm);

        rememberParametersExcept(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Calls the super method
     * and appends the parameter description of
     * the {@link #preprocessor}, the {@link #partitionAlgorithm},
     * and {@link #partitionDatabase} (if they are already initialized).
     */
    @Override
    public String parameterDescription() {
        StringBuilder description = new StringBuilder();
        description.append(super.parameterDescription());

        // preprocessor
        if (preprocessor != null) {
            description.append(Description.NEWLINE);
            description.append(preprocessor.parameterDescription());
        }
        // partition algorithm
        if (partitionAlgorithm != null) {
            description.append(Description.NEWLINE);
            description.append(partitionAlgorithm.parameterDescription());
        }
        // partition database
        if (PARTITION_DB_PARAM.isSet()) {
            try {
                Database<V> tmpDB = ClassGenericsUtil.instantiateGenerics(Database.class, partitionDatabase.getName());
                description.append(Description.NEWLINE);
                description.append(tmpDB.parameterDescription());
            }
            catch (UnableToComplyException e) {
                // tested before
                throw new RuntimeException("This should never happen!", e);
            }
        }
        return description.toString();
    }

    /**
     * Runs the partition algorithm and creates the result.
     *
     * @param database     the database to run this algorithm on
     * @param partitionMap the map of partition IDs to object ids
     */
    private void runPartitionAlgorithm(Database<V> database,
                                       Map<Integer, List<Integer>> partitionMap) {
        try {
            Map<Integer, Database<V>> databasePartitions =
                database.partition(partitionMap, partitionDatabase, partitionDatabaseParameters);

            result = new Clustering<Model>();

            for (Integer partitionID : databasePartitions.keySet()) {
                // noise partition
                if (partitionID == database.dimensionality()) {
                    Database<V> noiseDB = databasePartitions.get(partitionID);
                    DatabaseObjectGroup group = new DatabaseObjectGroupCollection<List<Integer>>(noiseDB.getIDs());
                    // Make a Noise cluster
                    result.addCluster(new Cluster<Model>(group, true, ClusterModel.CLUSTER));
                }
                else {
                    if (logger.isVerbose()) {
                      logger.verbose("Running " +
                            partitionAlgorithm.getDescription().getShortTitle() +
                            " on partition " +
                            partitionID);
                    }
                    Clustering<Model> p = partitionAlgorithm.run(databasePartitions.get(partitionID));
                    // Re-Wrap resulting Clusters as DimensionModel clusters.
                    for (Cluster<Model> clus : p.getAllClusters()) {
                      DatabaseObjectGroup group = new DatabaseObjectGroupCollection<Collection<Integer>>(clus.getIDs());
                      if (clus.isNoise()) {
                        result.addCluster(new Cluster<Model>(group, true, ClusterModel.CLUSTER));
                      } else {
                        result.addCluster(new Cluster<Model>(group, new DimensionModel(partitionID)));                      
                      }
                    }
                }
            }
        }
        catch (UnableToComplyException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the partition algorithm.
     *
     * @return the specified partition algorithm
     */
    public ClusteringAlgorithm<Clustering<Model>,V> getPartitionAlgorithm() {
        return partitionAlgorithm;
    }
}
