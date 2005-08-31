package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.AbortException;
import de.lmu.ifi.dbs.algorithm.AbstractAlgorithm;
import de.lmu.ifi.dbs.algorithm.COPAC;
import de.lmu.ifi.dbs.algorithm.DBSCAN;
import de.lmu.ifi.dbs.algorithm.DependencyDerivator;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.result.ClustersPlusNoise;
import de.lmu.ifi.dbs.algorithm.result.PartitionResults;
import de.lmu.ifi.dbs.database.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.normalization.AttributeWiseDoubleVectorNormalization;
import de.lmu.ifi.dbs.preprocessing.KnnQueryBasedCorrelationDimensionPreprocessor;
import de.lmu.ifi.dbs.utilities.Description;
import de.lmu.ifi.dbs.utilities.PatternBasedFileFilter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * TODO: comment
 * 
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class ACEP extends AbstractWrapper
{
    /**
     * Pattern to match a partition directory.
     */
    public static final Pattern PARTITION_PATTERN = Pattern.compile(PartitionResults.PARTITION_MARKER+"\\d+");
    
    /**
     * Pattern to match a cluster file.
     */
    public static final Pattern CLUSTER_PATTERN = Pattern.compile(ClustersPlusNoise.CLUSTER_MARKER+"\\d+");
    
    /**
     * FileFilter to accept partition directories.
     */
    public static final FileFilter PARTITION_FILE_FILTER = new PatternBasedFileFilter(PARTITION_PATTERN);
    
    /**
     * FileFilter to accept cluster files.
     */
    public static final FileFilter CLUSTER_FILE_FILTER = new PatternBasedFileFilter(CLUSTER_PATTERN);
    
    /**
     * Marker to append to result file of correlation analysis solution.
     */
    public static final String DEPENDENCY_MARKER = "_dependency";
    
    /**
     * Parameter for epsilon.
     */
    public static final String EPSILON_P = "epsilon";

    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the specified distance function";

    /**
     * Parameter minimum points.
     */
    public static final String MINPTS_P = "minpts";

    /**
     * Description for parameter minimum points.
     */
    public static final String MINPTS_D = "<int>minpts";

    /**
     * Epsilon.
     */
    protected String epsilon;

    /**
     * Minimum points.
     */
    protected String minpts;

    /**
     * The class of the distance function.
     */
    private Class distanceFunctionClass;

    /**
     * Sets epsilon and minimum points to the optionhandler additionally to the
     * parameters provided by super-classes. Since ACEP is a non-abstract class,
     * finally optionHandler is initialized.
     */
    public ACEP()
    {
        super();
        parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE, EPSILON_D);
        parameterToDescription.put(MINPTS_P + OptionHandler.EXPECTS_VALUE, MINPTS_D);
        optionHandler = new OptionHandler(parameterToDescription, getClass().getName());

        distanceFunctionClass = EuklideanDistanceFunction.class;
    }

    /**
     * Returns a description of the algorithm.
     * 
     * @return a description of the algorithm
     */
    public Description getDescription()
    {
        return new Description("ACEP", "", "Wrapper class for derivating dependencies.", "");
    }

    /**
     * Sets the parameters epsilon and minpts additionally to the parameters set
     * by the super-class' method. Both epsilon and minpts are required
     * parameters.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        try
        {
            epsilon = optionHandler.getOptionValue(EPSILON_P);
            minpts = optionHandler.getOptionValue(MINPTS_P);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingParameters;
    }

    /**
     * Runs the COPAC algorithm.
     */
    public void runCOPAC()
    {
        if(output == null)
            throw new IllegalArgumentException("Parameter -output is not set!");

        ArrayList<String> params = new ArrayList<String>();

        params.add(OptionHandler.OPTION_PREFIX+KDDTask.ALGORITHM_P);
        params.add(COPAC.class.getName());

        params.add(OptionHandler.OPTION_PREFIX+COPAC.PARTITION_ALGORITHM_P);
        params.add(DBSCAN.class.getName());

        params.add(OptionHandler.OPTION_PREFIX+COPAC.PREPROCESSOR_P);
        params.add(KnnQueryBasedCorrelationDimensionPreprocessor.class.getName());

        params.add(OptionHandler.OPTION_PREFIX+DBSCAN.EPSILON_P);
        params.add(epsilon);

        params.add(OptionHandler.OPTION_PREFIX+DBSCAN.MINPTS_P);
        params.add(minpts);

        params.add(OptionHandler.OPTION_PREFIX+DBSCAN.DISTANCE_FUNCTION_P);
        params.add(distanceFunctionClass.getName());

        params.add(OptionHandler.OPTION_PREFIX+KDDTask.NORMALIZATION_P);
        params.add(AttributeWiseDoubleVectorNormalization.class.getName());

        // normalization undo?
        
        params.add(OptionHandler.OPTION_PREFIX+FileBasedDatabaseConnection.INPUT_P);
        params.add(input);

        params.add(OptionHandler.OPTION_PREFIX+KDDTask.OUTPUT_P);
        params.add(output);

        if(time)
        {
            params.add(OptionHandler.OPTION_PREFIX+AbstractAlgorithm.TIME_F);
        }

        if(verbose)
        {
            params.add(OptionHandler.OPTION_PREFIX+AbstractAlgorithm.VERBOSE_F);
            params.add(OptionHandler.OPTION_PREFIX+AbstractAlgorithm.VERBOSE_F);
        }

        KDDTask copacTask = new KDDTask();
        copacTask.setParameters(params.toArray(new String[params.size()]));
        copacTask.run();
    }

    /**
     * Runs the Dependency Derivating algorithm.
     */
    private void runDependencyDerivator()
    {
        
        File dir = new File(output);
        if(!dir.isDirectory())
        {
            throw new IllegalArgumentException(dir + " is no directory!");
        }
        File[] subDirs = dir.listFiles(PARTITION_FILE_FILTER);
        for(File subDir : subDirs)
        {
            File[] clusters = subDir.listFiles(CLUSTER_FILE_FILTER);

            for(File cluster : clusters)
            {
                ArrayList<String> params = new ArrayList<String>();

                params.add(OptionHandler.OPTION_PREFIX+KDDTask.ALGORITHM_P);
                params.add(DependencyDerivator.class.getName());

                params.add(OptionHandler.OPTION_PREFIX+FileBasedDatabaseConnection.INPUT_P);
                String inputName = output + File.separator + subDir.getName() + File.separator + cluster.getName();
                params.add(inputName);
                // params.add(cluster.getAbsolutePath());

                System.out.println(params);
                System.out.println(inputName);

                params.add(OptionHandler.OPTION_PREFIX+KDDTask.OUTPUT_P);
                params.add(cluster.getAbsolutePath() + DEPENDENCY_MARKER);

                if(verbose)
                {
                    params.add(OptionHandler.OPTION_PREFIX+AbstractAlgorithm.VERBOSE_F);
                }

                KDDTask dependencyTask = new KDDTask();
                dependencyTask.setParameters(params.toArray(new String[params.size()]));
                dependencyTask.run();
            }

        }

    }

    /**
     * Runs a KDD task accordingly to the specified parameters.
     * 
     * @param args
     *            parameter list according to description
     */
    public static void main(String[] args)
    {
        ACEP acep = new ACEP();
        try
        {
            acep.setParameters(args);
            acep.runCOPAC();
            acep.runDependencyDerivator();
        }
        catch(AbortException e)
        {
            System.out.println(e.getMessage());
            System.exit(0);
        }
        catch(IllegalArgumentException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        catch(IllegalStateException e)
        {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

}
