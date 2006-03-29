package de.lmu.ifi.dbs.algorithm.clustering;

import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.Algorithm;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.Result;
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
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Algorithm to partition a database according to the correlation dimension of
 * its objects and to then perform an arbitrary algorithm over the partitions.
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class COPAA extends AbstractAlgorithm<RealVector>
{
    /**
     * Parameter for preprocessor.
     */
    public static final String PREPROCESSOR_P = "preprocessor";

    /**
     * Description for parameter preprocessor.
     */
    public static final String PREPROCESSOR_D = "<classname>preprocessor to derive partition criterion - must extend "
            + CorrelationDimensionPreprocessor.class.getName() + ".";

    /**
     * Parameter for partition algorithm.
     */
    public static final String PARTITION_ALGORITHM_P = "partAlg";

    /**
     * Description for parameter partition algorithm
     */
    public static final String PARTITION_ALGORITHM_D = "<classname>algorithm to apply to each partition - must implement "
            + Algorithm.class.getName() + ".";

    /**
     * Parameter for class of partition database.
     */
    public static final String PARTITION_DATABASE_CLASS_P = "partDB";

    /**
     * Description for parameter partition database.
     */
    public static final String PARTITION_DATABASE_CLASS_D = "<classname>database class for each partition - must implement "
            + Database.class.getName()
            + ". "
            + "If this parameter is not set, the databases of the partitions "
            + "have the same class as the original database.";

    /**
     * Holds the preprocessor.
     */
    protected CorrelationDimensionPreprocessor preprocessor;

    /**
     * Holds the partitioning algorithm.
     */
    protected Algorithm<RealVector> partitionAlgorithm;

    /**
     * Holds the class of the partition databases.
     */
    protected Class<Database> partitionDatabase;

    /**
     * Holds the parameters of the partition databases.
     */
    protected String[] partitionDatabaseParameters;

    /**
     * Holds the result.
     */
    private PartitionResults<RealVector> result;

    /**
     * Sets the specific parameters additionally to the parameters set by the
     * super-class.
     */
    public COPAA()
    {
        super();
        parameterToDescription.put(
                PREPROCESSOR_P + OptionHandler.EXPECTS_VALUE, PREPROCESSOR_D);
        parameterToDescription.put(PARTITION_ALGORITHM_P
                + OptionHandler.EXPECTS_VALUE, PARTITION_ALGORITHM_D);
        parameterToDescription.put(PARTITION_DATABASE_CLASS_P
                + OptionHandler.EXPECTS_VALUE, PARTITION_DATABASE_CLASS_D);
        optionHandler = new OptionHandler(parameterToDescription, this
                .getClass().getName());
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    protected void runInTime(Database<RealVector> database)
            throws IllegalStateException
    {
        // preprocessing
        if (isVerbose())
        {
            System.out.println("\ndb size = " + database.size());
            System.out.println("dimensionality = " + database.dimensionality());
            System.out.println("\npreprocessing... ");
        }
        preprocessor.run(database, isVerbose(), isTime());
        // partitioning
        if (isVerbose())
        {
            System.out.println("\npartitioning... ");
        }
        Map<Integer, List<Integer>> partitionMap = new Hashtable<Integer, List<Integer>>();
        Progress partitionProgress = new Progress(database.size());
        int processed = 1;

        for (Iterator<Integer> dbiter = database.iterator(); dbiter.hasNext();)
        {
            Integer id = dbiter.next();
            Integer corrdim = ((LocalPCA) database.getAssociation(
                    AssociationID.LOCAL_PCA, id)).getCorrelationDimension();

            if (!partitionMap.containsKey(corrdim))
            {
                partitionMap.put(corrdim, new ArrayList<Integer>());
            }

            partitionMap.get(corrdim).add(id);
            if (isVerbose())
            {
                partitionProgress.setProcessed(processed++);
                System.out.print("\r" + partitionProgress.toString());
            }
        }

        if (isVerbose())
        {
            partitionProgress.setProcessed(database.size());
            System.out.print("\r" + partitionProgress.toString());
            System.out.println("");
            for (Integer corrDim : partitionMap.keySet())
            {
                List<Integer> list = partitionMap.get(corrDim);
                System.out.println("Partition " + corrDim + " = " + list.size()
                        + " objects.");
            }
        }

        // running partition algorithm
        result = runPartitionAlgorithm(database, partitionMap);
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result<RealVector> getResult()
    {
        return result;
    }

    /**
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description(
                "COPAA",
                "Correlation Partitioning",
                "Partitions a database according to the correlation dimension of its objects and performs an arbitrary algorithm over the partitions.",
                "unpublished");
    }

    /**
     * Returns the the partitioning algorithm.
     * 
     * @return the the partitioning algorithm
     */
    public Algorithm<RealVector> getPartitionAlgorithm()
    {
        return partitionAlgorithm;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    @Override
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("", false));
        description.append('\n');
        description
                .append("Remaining parameters are firstly given to the partition algorithm, then to the preprocessor.");
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
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = super.setParameters(args);

        // partition algorithm
        String partAlgString = optionHandler
                .getOptionValue(PARTITION_ALGORITHM_P);
        try
        {
            // noinspection unchecked
            partitionAlgorithm = Util.instantiate(Algorithm.class,
                    partAlgString);
        } catch (UnableToComplyException e)
        {
            throw new WrongParameterValueException(PARTITION_ALGORITHM_P,
                    partAlgString, PARTITION_ALGORITHM_D);
        }

        // partition db
        if (optionHandler.isSet(PARTITION_DATABASE_CLASS_P))
        {
            String partDBString = optionHandler
                    .getOptionValue(PARTITION_DATABASE_CLASS_P);
            try
            {
                Database tmpDB = Util.instantiate(Database.class, partDBString);
                remainingParameters = tmpDB.setParameters(remainingParameters);
                partitionDatabaseParameters = tmpDB.getParameters();
                // noinspection unchecked
                partitionDatabase = (Class<Database>) tmpDB.getClass();
            } catch (UnableToComplyException e)
            {
                throw new WrongParameterValueException(
                        PARTITION_DATABASE_CLASS_P, partDBString,
                        PARTITION_DATABASE_CLASS_D, e);
            }
        }

        // preprocessor
        String preprocessorString = optionHandler
                .getOptionValue(PREPROCESSOR_P);
        try
        {
            preprocessor = Util.instantiate(
                    CorrelationDimensionPreprocessor.class, preprocessorString);
        } catch (UnableToComplyException e)
        {
            throw new WrongParameterValueException(PREPROCESSOR_P,
                    preprocessorString, PREPROCESSOR_D, e);
        }

        remainingParameters = preprocessor.setParameters(remainingParameters);

        remainingParameters = partitionAlgorithm
                .setParameters(remainingParameters);
        partitionAlgorithm.setTime(isTime());
        partitionAlgorithm.setVerbose(isVerbose());
        setParameters(args, remainingParameters);
        return remainingParameters;
    }

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> result = super.getAttributeSettings();

        AttributeSettings settings = result.get(0);
        settings.addSetting(PREPROCESSOR_P, preprocessor.getClass().getName());
        settings.addSetting(PARTITION_ALGORITHM_P, partitionAlgorithm
                .getClass().getName());
        if (optionHandler.isSet(PARTITION_DATABASE_CLASS_P))
        {
            settings.addSetting(PARTITION_DATABASE_CLASS_P, partitionDatabase
                    .getName());
        }

        result.addAll(preprocessor.getAttributeSettings());
        result.addAll(partitionAlgorithm.getAttributeSettings());
        if (optionHandler.isSet(PARTITION_DATABASE_CLASS_P))
        {
            try
            {
                Database tmpDB = Util.instantiate(Database.class,
                        partitionDatabase.getName());
                result.addAll(tmpDB.getAttributeSettings());
            } catch (UnableToComplyException e)
            {
                // tested before
                throw new RuntimeException("This should never happen!");
            }
        }

        return result;
    }

    /**
     * Runs the partition algorithm and creates the result.
     * 
     * @param database
     *            the database to run this algorithm on
     * @param partitionMap
     *            the map of partition IDs to object ids
     */
    protected PartitionResults<RealVector> runPartitionAlgorithm(
            Database<RealVector> database,
            Map<Integer, List<Integer>> partitionMap)
    {
        try
        {
            Map<Integer, Database<RealVector>> databasePartitions = database
                    .partition(partitionMap, partitionDatabase,
                            partitionDatabaseParameters);
            Map<Integer, Result<RealVector>> results = new Hashtable<Integer, Result<RealVector>>();
            for (Integer partitionID : databasePartitions.keySet())
            {
                if (isVerbose())
                {
                    System.out.println("\nRunning "
                            + partitionAlgorithm.getDescription()
                                    .getShortTitle() + " on partition "
                            + partitionID);
                }
                partitionAlgorithm.run(databasePartitions.get(partitionID));
                results.put(partitionID, partitionAlgorithm.getResult());
            }
            return new PartitionResults<RealVector>(database, results);
        } catch (UnableToComplyException e)
        {
            throw new IllegalStateException(e);
        }
    }
}
