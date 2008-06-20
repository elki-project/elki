package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.preprocessing.HiCOPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.*;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class COPAA<V extends RealVector<V, ?>> extends AbstractAlgorithm<V> {
    /**
     * Parameter to specify the preprocessor to derive partition criterion,
     * must extend {@link HiCOPreprocessor}.
     * <p>Key: {@code -copaa.preprocessor} </p>
     */
    private final ClassParameter<HiCOPreprocessor> PREPROCESSOR_PARAM =
        new ClassParameter<HiCOPreprocessor>(OptionID.COPAA_PREPROCESSOR, HiCOPreprocessor.class);

    /**
     * Parameter to specify the algorithm to apply to each partition,
     * must extend {@link de.lmu.ifi.dbs.algorithm.Algorithm}.
     * <p>Key: {@code -copaa.partitionAlgorithm} </p>
     */
    @SuppressWarnings("unchecked")
    protected final ClassParameter PARTITION_ALGORITHM_PARAM =
        new ClassParameter(OptionID.COPAA_PARTITION_ALGORITHM, Algorithm.class);

    /**
     * Parameter to specify the database class for each partition,
     * must extend {@link de.lmu.ifi.dbs.database.Database}.
     * <p>Key: {@code -copaa.partitionDB} </p>
     */
    private final ClassParameter<Database> PARTITION_DB_PARAM =
        new ClassParameter<Database>(OptionID.COPAA_PARTITION_DATABASE, Database.class, true);

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
        addOption(PREPROCESSOR_PARAM);
        // parameter partition algorithm
        addOption(PARTITION_ALGORITHM_PARAM);
        // parameter partition database class
        addOption(PARTITION_DB_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    @Override
    protected void runInTime(Database<V> database) throws IllegalStateException {
        // preprocessing
        if (isVerbose()) {
            verbose("\ndb size = " + database.size());
            verbose("dimensionality = " + database.dimensionality());
        }
        preprocessor.run(database, isVerbose(), isTime());
        // partitioning
        if (isVerbose()) {
            verbose("\nPartitioning...");
        }
        Map<Integer, List<Integer>> partitionMap = new Hashtable<Integer, List<Integer>>();
        Progress partitionProgress = new Progress("Partitioning", database.size());
        int processed = 1;

        for (Iterator<Integer> dbiter = database.iterator(); dbiter.hasNext();) {
            Integer id = dbiter.next();
            Integer corrdim = (database.getAssociation(AssociationID.LOCAL_PCA, id)).getCorrelationDimension();

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
                verbose("\nPartition " + corrDim + " = " + list.size() + " objects.");
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
        // noinspection unchecked
        partitionAlgorithm = (Algorithm) PARTITION_ALGORITHM_PARAM.instantiateClass();

        // partition db
        if (optionHandler.isSet(PARTITION_DB_PARAM)) {
            Database tmpDB = PARTITION_DB_PARAM.instantiateClass();
            remainingParameters = tmpDB.setParameters(remainingParameters);
            partitionDatabaseParameters = tmpDB.getParameters();
            // noinspection unchecked
            partitionDatabase = (Class<Database<V>>) tmpDB.getClass();
        }

        // preprocessor
        // noinspection unchecked
        preprocessor = PREPROCESSOR_PARAM.instantiateClass();
        remainingParameters = preprocessor.setParameters(remainingParameters);

        // partition algorithm
        String[] partitiongAlgorithmParameters = new String[remainingParameters.length];
        System.arraycopy(remainingParameters, 0, partitiongAlgorithmParameters, 0, remainingParameters.length);
        partitionAlgorithm.setTime(isTime());
        partitionAlgorithm.setVerbose(isVerbose());
        remainingParameters = partitionAlgorithm.setParameters(partitiongAlgorithmParameters);

        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    @Override
    public List<AttributeSettings> getAttributeSettings() {
        List<AttributeSettings> result = super.getAttributeSettings();

        result.addAll(preprocessor.getAttributeSettings());
        result.addAll(partitionAlgorithm.getAttributeSettings());
        if (optionHandler.isSet(PARTITION_DB_PARAM)) {
            try {
                // noinspection unchecked
                Database<V> tmpDB = (Database<V>) Util.instantiate(Database.class, partitionDatabase.getName());
                result.addAll(tmpDB.getAttributeSettings());
            }
            catch (UnableToComplyException e) {
                // tested before
                throw new RuntimeException("This should never happen!", e);
            }
        }

        return result;
    }

    /**
     * Runs the partition algorithm and creates the result.
     *
     * @param database     the database to run this algorithm on
     * @param partitionMap the map of partition IDs to object ids
     * @return the result of the partition algorithm
     */
    protected PartitionResults<V> runPartitionAlgorithm(Database<V> database, Map<Integer, List<Integer>> partitionMap) {
        try {
            Map<Integer, Database<V>> databasePartitions = database.partition(partitionMap, partitionDatabase, partitionDatabaseParameters);
            Map<Integer, Result<V>> results = new Hashtable<Integer, Result<V>>();
            for (Integer partitionID : databasePartitions.keySet()) {
                if (isVerbose()) {
                    verbose("\nRunning " + partitionAlgorithm.getDescription().getShortTitle() +
                        " on partition " + partitionID);
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
