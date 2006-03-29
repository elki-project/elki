package de.lmu.ifi.dbs.preprocessing;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.database.AssociationID;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.DoubleDistance;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.pca.LinearLocalPCA;
import de.lmu.ifi.dbs.utilities.Progress;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.*;

/**
 * Preprocessor for 4C correlation dimension assignment to objects of a certain
 * database.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class VarianceAnalysisPreprocessor implements Preprocessor
{
    /**
     * The default value for delta.
     */
    public static final double DEFAULT_DELTA = 0.01;

    /**
     * Option string for parameter delta.
     */
    public static final String DELTA_P = "delta";

    /**
     * Description for parameter delta.
     */
    public static final String DELTA_D = "<double>a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = "
            + DEFAULT_DELTA + ").";

    /**
     * Parameter for epsilon.
     */
    public static final String EPSILON_P = "preprocessorEpsilon";

    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "<epsilon>an epsilon value suitable to the Euklidean distance function";

    /**
     * Epsilon.
     */
    protected String epsilon;

    /**
     * Map providing a mapping of parameters to their descriptions.
     */
    protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

    /**
     * OptionHandler for handling options.
     */
    protected OptionHandler optionHandler;

    /**
     * The threshold for small eigenvalues.
     */
    protected double delta;

    /**
     * The distance function for the PCA.
     */
    protected EuklideanDistanceFunction<RealVector> rangeQueryDistanceFunction = new EuklideanDistanceFunction<RealVector>();

    /**
     * Holds the currently set parameter array.
     */
    private String[] currentParameterArray = new String[0];

    /**
     * Provides a new Preprocessor that computes the correlation dimension of
     * objects of a certain database.
     */
    protected VarianceAnalysisPreprocessor()
    {
        parameterToDescription.put(DELTA_P + OptionHandler.EXPECTS_VALUE,
                DELTA_D);
        parameterToDescription.put(EPSILON_P + OptionHandler.EXPECTS_VALUE,
                EPSILON_D);
        optionHandler = new OptionHandler(parameterToDescription, getClass()
                .getName());
    }

    /**
     * This method determines the correlation dimensions of the objects stored
     * in the specified database and sets the necessary associations in the
     * database.
     * 
     * @param database
     *            the database for which the preprocessing is performed
     * @param verbose
     *            flag to allow verbose messages while performing the algorithm
     * @param time
     *            flag to request output of performance time
     */
    public void run(Database<RealVector> database, boolean verbose, boolean time)
    {
        if (database == null)
        {
            throw new IllegalArgumentException("Database must not be null!");
        }

        long start = System.currentTimeMillis();
        rangeQueryDistanceFunction.setDatabase(database, verbose, time);

        Progress progress = new Progress(database.size());
        if (verbose)
        {
            System.out.println("Preprocessing:");
        }
        Iterator<Integer> it = database.iterator();
        int processed = 1;
        while (it.hasNext())
        {
            Integer id = it.next();
            List<QueryResult<DoubleDistance>> qrs = database.rangeQuery(id,
                    epsilon, rangeQueryDistanceFunction);

            List<Integer> ids = new ArrayList<Integer>(qrs.size());
            for (QueryResult<DoubleDistance> qr : qrs)
            {
                ids.add(qr.getID());
            }
            
            runSpecialVarianceAnalysis(id, ids, database);

            progress.setProcessed(processed++);
            if (verbose)
            {
                System.out.print("\r" + progress.toString());
            }
        }
        if (verbose)
        {
            System.out.println();
        }

        long end = System.currentTimeMillis();
        if (time)
        {
            long elapsedTime = end - start;
            System.out.println(this.getClass().getName() + " runtime: "
                    + elapsedTime + " milliseconds.");
        }
    }

    /**
     * This method implements the type of variance analysis to be computed for a given point.
     * 
     * Example1: for 4C, this method should implement a PCA for the given point.
     * Example2: for PreDeCon, this method should implement a simple axis-parallel variance analysis.
     * @param id
     *            the given point
     * @param ids
     *            neighbors of the given point
     * @param database
     *            the database for which the preprocessing is performed
     */
    protected abstract void runSpecialVarianceAnalysis(Integer id, List<Integer> ids, Database<RealVector> database);
    
    /**
     * Sets the values for the parameters alpha, pca and pcaDistancefunction if
     * specified. If the parameters are not specified default values are set.
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException
    {
        String[] remainingParameters = optionHandler.grabOptions(args);

        // delta
        if (optionHandler.isSet(DELTA_P))
        {
            String deltaString = optionHandler.getOptionValue(DELTA_P);
            try
            {
                delta = Double.parseDouble(deltaString);
                if (delta < 0 || delta > 1)
                {
                    throw new WrongParameterValueException(DELTA_P,
                            deltaString, DELTA_D);
                }
            } catch (NumberFormatException e)
            {
                throw new WrongParameterValueException(DELTA_P, deltaString,
                        DELTA_D, e);
            }
        } else
        {
            delta = DEFAULT_DELTA;
        }

        // epsilon
        epsilon = optionHandler.getOptionValue(EPSILON_P);
        try
        {
            rangeQueryDistanceFunction.valueOf(epsilon);
        } catch (IllegalArgumentException e)
        {
            throw new WrongParameterValueException(EPSILON_P, epsilon,
                    EPSILON_D, e);
        }

        remainingParameters = rangeQueryDistanceFunction
                .setParameters(remainingParameters);
        return remainingParameters;
    }

    /**
     * Sets the difference of the first array minus the second array as the
     * currently set parameter array.
     * 
     * 
     * @param complete
     *            the complete array
     * @param part
     *            an array that contains only elements of the first array
     */
    protected void setParameters(String[] complete, String[] part)
    {
        currentParameterArray = Util.difference(complete, part);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#getParameters()
     */
    public String[] getParameters()
    {
        String[] param = new String[currentParameterArray.length];
        System.arraycopy(currentParameterArray, 0, param, 0,
                currentParameterArray.length);
        return param;
    }

    /**
     * Returns the parameter setting of the attributes.
     * 
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings()
    {
        List<AttributeSettings> attributeSettings = new ArrayList<AttributeSettings>();

        AttributeSettings mySettings = new AttributeSettings(this);
        mySettings.addSetting(DELTA_P, Double.toString(delta));
        mySettings.addSetting(EPSILON_P, epsilon);
        attributeSettings.add(mySettings);

        return attributeSettings;
    }

}
