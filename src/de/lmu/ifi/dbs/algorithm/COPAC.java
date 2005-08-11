package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.algorithm.result.Result;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.pca.CorrelationPCA;
import de.lmu.ifi.dbs.preprocessing.CorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Algorithm to partiton a database according to the correlation dimension of its objects
 * and to then perform an arbitrary algorithm over the partitions.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class COPAC extends AbstractAlgorithm
{
    /**
     * Parameter for preprocessor.
     */
    public static final String PREPROCESSOR_P = "preprocessor";
    
    /**
     * Description for parameter preprocessor.
     */
    public static final String PREPROCESSOR_D = "<classname>preprocessor to derive partition criterion - must extend "+CorrelationDimensionPreprocessor.class.getName()+".";
    
    /**
     * Parameter for partitioning algorithm.
     */
    public static final String PARTITION_ALGORITHM_P = "partAlg";
    
    /**
     * Description for parameter partitioning algorithm
     */
    public static final String PARTITION_ALGORITHM_D = "<classname>algorithm to apply to each partition - must implement "+Algorithm.class.getName()+".";

    /**
     * Holds the preprocessor.
     */
    private CorrelationDimensionPreprocessor preprocessor;
    
    /**
     * Holds the partitioning algorithm.
     */
    private Algorithm partitionAlgorithm;
    
    /**
     * Holds the result.
     */
    private PartitionResults result;
    
    /**
     * Sets the specific parameters additionally to the parameters
     * set by the super-class.
     *
     */
    public COPAC()
    {
        super();
        parameterToDescription.put(PREPROCESSOR_P+OptionHandler.EXPECTS_VALUE,PREPROCESSOR_D);
        parameterToDescription.put(PARTITION_ALGORITHM_P+OptionHandler.EXPECTS_VALUE,PARTITION_ALGORITHM_D);
        optionHandler = new OptionHandler(parameterToDescription,this.getClass().getName());
    }
    
    
    
    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#run(de.lmu.ifi.dbs.database.Database)
     */
    public void run(Database database) throws IllegalStateException
    {
        long start = System.currentTimeMillis();
        Progress partitionProgress = new Progress(database.size());
        if(isVerbose())
        {
            System.out.println("preprocessing...");
        }
        preprocessor.run(database);
        Map<Integer,List<Integer>> partitionMap = new Hashtable<Integer,List<Integer>>();
        if(isVerbose())
        {
            System.out.println("partitioning:");
        }
        int processed = 1;
        for(Iterator<Integer> dbiter = database.iterator(); dbiter.hasNext();)
        {
            Integer id = dbiter.next();
            Integer corrdim = new Integer(((CorrelationPCA) database.getAssociation(CorrelationDimensionPreprocessor.ASSOCIATION_ID_PCA,id)).getCorrelationDimension());
            if(!partitionMap.containsKey(corrdim))
            {
                partitionMap.put(corrdim, new ArrayList<Integer>());
            }
            partitionMap.get(corrdim).add(id);
            if(isVerbose())
            {
                partitionProgress.setProcessed(processed++);
                System.out.print(partitionProgress.toString());
            }
        }
        if(isVerbose())
        {
            partitionProgress.setProcessed(database.size());
            System.out.println(partitionProgress.toString());
        }
        try
        {
            Map<Integer,Database> databasePartitions = database.partition(partitionMap);
            Map<Integer,Result> results = new Hashtable<Integer,Result>();
            for(Iterator<Integer> partitionIter = databasePartitions.keySet().iterator(); partitionIter.hasNext();)
            {
                Integer partitionID = partitionIter.next();
                if(isVerbose())
                {
                    System.out.println("running on partition "+partitionID);
                }                
                partitionAlgorithm.run(databasePartitions.get(partitionID));
                results.put(partitionID,partitionAlgorithm.getResult());
            }
            result = new PartitionResults(results);
        }
        catch(UnableToComplyException e)
        {
            throw new IllegalStateException(e);
        }
        long end = System.currentTimeMillis();
        if(isTime())
        {
            long elapsedTime = end - start;
            System.out.println(this.getClass().getName()+" runtime: "+elapsedTime+" milliseconds.");
        }
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getResult()
     */
    public Result getResult()
    {
        return result;
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.Algorithm#getDescription()
     */
    public Description getDescription()
    {
        return new Description("CoPa",
                "Correlation Partitioning",
                "Partitions a database according to the correlation dimension of its objects and performs an arbitrary algorithm over the partitions.",
                "unpublished");
    }

    /**
     * 
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    @Override
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("",false));
        description.append('\n');
        description.append("Remaining parameters are firstly given to the partition algorithm, then to the preprocessor.");
        description.append('\n');
        description.append('\n');
        return description.toString();
    }

    /**
     *
     * Passes remaining parameters first to the partition algorithm,
     * then to the preprocessor.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            try
            {
                partitionAlgorithm = (Algorithm) Class.forName(optionHandler.getOptionValue(PARTITION_ALGORITHM_P)).newInstance();
            }
            catch(ClassNotFoundException e)
            {
                // TODO unify - method to init class for all default packages specified in properties for an interface
                partitionAlgorithm = (Algorithm) Class.forName(KDDTask.DEFAULT_ALGORITHM_PACKAGE+"."+optionHandler.getOptionValue(PARTITION_ALGORITHM_P)).newInstance();
            }
            preprocessor = (CorrelationDimensionPreprocessor) Class.forName(optionHandler.getOptionValue(PREPROCESSOR_P)).newInstance();
        }
        catch(Exception e)
        {
            throw new IllegalArgumentException(e);
        }
        
        remainingParameters = partitionAlgorithm.setParameters(remainingParameters);
        return preprocessor.setParameters(remainingParameters);
    }

    
    
}
