package de.lmu.ifi.dbs.distance;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.Hashtable;
import java.util.Map;

/**
 * Provides a LP-Norm for FeatureVectors.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public class LPNormDistanceFunction extends DoubleDistanceFunction<FeatureVector>
{
    /**
     * Parameter P.
     */
    public static final String P_P = "P";

    /**
     * Description for parameter P.
     */
    public static final String P_D = "<double>the degree of the L-P-Norm (positive number)";

    /**
     * Keeps the curerntly set p.
     */
    private double p;

    /**
     * Mapping of parameters to parameter-descriptions.
     */
    protected Map<String, String> parameterToDescription = new Hashtable<String, String>();

    /**
     * OptionHandler for handling of options.
     */
    protected OptionHandler optionHandler;

    /**
     * Provides a LP-Norm for FeatureVectors.
     */
    public LPNormDistanceFunction()
    {
        super();
        parameterToDescription.put(P_P + OptionHandler.EXPECTS_VALUE, P_D);
        optionHandler = new OptionHandler(parameterToDescription, LPNormDistanceFunction.class.getName());
    }

    /**
     * Returns the distance between the specified FeatureVectors as a LP-Norm
     * for the currently set p.
     * 
     * @param o1
     *            first FeatureVector
     * @param o2
     *            second FeatureVector
     * @return the distance between the specified FeatureVectors as a LP-Norm
     *         for the currently set p
     */
    public DoubleDistance distance(FeatureVector o1, FeatureVector o2)
    {
        if(o1.getDimensionality() != o2.getDimensionality())
        {
            throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + o1.toString() + "\n  second argument: " + o2.toString());
        }
        double sqrDist = 0;
        for(int i = 1; i <= o1.getDimensionality(); i++)
        {
            double manhattanI = Math.abs(o1.getValue(i).doubleValue() - o2.getValue(i).doubleValue());
            sqrDist += Math.pow(manhattanI, p);
        }
        return new DoubleDistance(Math.pow(sqrDist, 1.0 / p));
    }

    /**
     * Set the database that holds the associations for the DoubleVectors for
     * which the distances should be computed. This method does nothing because
     * in this distance function no associations are needed.
     * 
     * @param database
     *            the database to be set
     * @param verbose
     *            flag to allow verbose messages while performing the method
     * @see DistanceFunction#setDatabase(Database, boolean)
     */
    public void setDatabase(Database<FeatureVector> database, boolean verbose)
    {
        // no settings required
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#description()
     */
    public String description()
    {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("L-Norm for FeatureVectors.", false));
        description.append('\n');
        return description.toString();
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingOptions = optionHandler.grabOptions(args);
        try
        {
            p = Double.parseDouble(optionHandler.getOptionValue(P_P));
            if(p <= 0)
            {
                throw new IllegalArgumentException("Parameter " + P_P + " is supposed to be a positive number.");
            }
        }
        catch(NumberFormatException e)
        {
            throw new IllegalArgumentException(e);
        }
        catch(UnusedParameterException e)
        {
            throw new IllegalArgumentException(e);
        }
        return remainingOptions;
    }

}
