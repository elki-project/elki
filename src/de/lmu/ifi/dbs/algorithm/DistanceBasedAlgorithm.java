package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.data.MetricalObject;

/**
 * Provides an abstract algorithm already setting the distance funciton.
 * 
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DistanceBasedAlgorithm<T extends MetricalObject> extends AbstractAlgorithm<T>
{
    /**
     * The default distance function.
     */
    public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

    /**
     * Property for distance funcitons.
     */
    public static final String PROPERTY_DISTANCE_FUNCTIONS = "DISTANCE_FUNCTIONS";

    /**
     * Parameter for distance function.
     */
    public static final String DISTANCE_FUNCTION_P = "distancefunction";

    /**
     * Description for parameter distance function.
     */
    public static final String DISTANCE_FUNCTION_D = "<classname>the distance function to determine the distance between metrical objects - must implement " + DistanceFunction.class.getName() + ". (Default: " + DEFAULT_DISTANCE_FUNCTION.getClass().getName() + ").";

    /**
     * The distance function.
     */
    private DistanceFunction distanceFunction;

    /**
     * Adds parameter for distance function to parameter map.
     */
    protected DistanceBasedAlgorithm()
    {
        super();
        parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    }

    /**
     * @see AbstractAlgorithm#description()
     */
    @Override
    public String description()
    {
        StringBuffer description = new StringBuffer(super.description());
        description.append('\n');
        description.append("DistanceFunctions available within KDD-Framework:\n");
        description.append('\n');
        String distanceFunctions = KDDTask.PROPERTIES.getProperty(PROPERTY_DISTANCE_FUNCTIONS);
        String[] distanceFunctionNames = distanceFunctions != null ? KDDTask.PROPERTY_SEPARATOR.split(distanceFunctions) : new String[0];
        for(String distanceFunctionName : distanceFunctionNames)
        {
            try
            {
                String desc = ((DistanceFunction) Class.forName(distanceFunctionName).newInstance()).description();
                description.append(distanceFunctionName);
                description.append('\n');
                description.append(desc);
                description.append('\n');
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: " + e.getMessage() + " - " + e.getClass().getName());
            }
        }
        description.append('\n');
        description.append('\n');
        return description.toString();
    }

    /**
     * Calls
     * {@link AbstractAlgorithm#setParameters(String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the distance function, passing remaining parameters
     * to the set distance function.
     * 
     * @see AbstractAlgorithm#setParameters(String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(DISTANCE_FUNCTION_P))
        {
            try
            {
                String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
                distanceFunction = ((DistanceFunction) Class.forName(className).newInstance());
            }
            catch(UnusedParameterException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(NoParameterValueException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        else
        {
            try
            {
                distanceFunction = (DistanceFunction) Class.forName(DEFAULT_DISTANCE_FUNCTION).newInstance();
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e);
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e);
            }
        }
        return distanceFunction.setParameters(remainingParameters);
    }

    /**
     * Returns the distanceFunction.
     * 
     * @return the distanceFunction
     */
    protected DistanceFunction<T> getDistanceFunction()
    {
        return distanceFunction;
    }

}
