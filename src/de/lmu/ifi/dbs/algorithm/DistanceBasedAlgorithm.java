package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

/**
 * Provides an abstract algorithm already setting the distance funciton.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DistanceBasedAlgorithm extends AbstractAlgorithm
{
    /**
     * The default distance function.
     */
    public static final DistanceFunction DEFAULT_DISTANCE_FUNCTION = new EuklideanDistanceFunction();
    
    /**
     * Property for distance funcitons.
     */
    public static final String PROPERTY_DISTANCE_FUNCTIONS = "DISTANCE_FUNCTIONS";        
    
    /**
     * Parameter for distance function.
     */
    public static final String DISTANCE_FUNCTION_P = "<classname>distance function";
    
    /**
     * Description for parameter distance function.
     */
    public static final String DISTANCE_FUNCTION_D = "the distance function to determine the distance between metrical objects - must implement "+DistanceFunction.class.getName()+". (Default: "+DEFAULT_DISTANCE_FUNCTION.getClass().getName()+").";
    
    // TODO how to discern whether preprocessing is required -> distancefunction itself?

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
        parameterToDescription.put(DISTANCE_FUNCTION_P+OptionHandler.EXPECTS_VALUE,DISTANCE_FUNCTION_D);
    }

    /**
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#description()
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
        for(int i = 0; i < distanceFunctionNames.length; i++)
        {
            try
            {
                String desc = ((DistanceFunction) Class.forName(distanceFunctionNames[i]).newInstance()).description();
                description.append(distanceFunctionNames[i]);
                description.append('\n');
                description.append(desc);
                description.append('\n');
            }
            catch(InstantiationException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(IllegalAccessException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(ClassNotFoundException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
            catch(ClassCastException e)
            {
                System.err.println("Invalid classname in property-file: "+e.getMessage()+" - "+e.getClass().getName());
            }
        }
        description.append('\n');
        description.append('\n');
        return description.toString();
    }

    /**
     * Calls {@link de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(java.lang.String[]) AbstractAlgorithm#setParameters(args)}
     * and sets additionally the distance function, passing remaining parameters to the set distance function.
     * 
     * @see de.lmu.ifi.dbs.algorithm.AbstractAlgorithm#setParameters(java.lang.String[])
     */
    @Override
    public String[] setParameters(String[] args) throws IllegalArgumentException
    {
        String[] remainingParameters = super.setParameters(args);
        if(optionHandler.isSet(DISTANCE_FUNCTION_P))
        {
            try
            {
                distanceFunction = ((DistanceFunction) Class.forName(optionHandler.getOptionValue(DISTANCE_FUNCTION_P)).newInstance());
            }
            catch(UnusedParameterException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(NoParameterValueException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(InstantiationException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(IllegalAccessException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
            catch(ClassNotFoundException e)
            {
                throw new IllegalArgumentException(e.getMessage());
            }
        }
        else
        {
            distanceFunction = DEFAULT_DISTANCE_FUNCTION;
        }
        return distanceFunction.setParameters(remainingParameters);
    }

    /**
     * Returns the distanceFunction.
     * 
     * @return the distanceFunction
     */
    protected DistanceFunction getDistanceFunction()
    {
        return distanceFunction;
    }
    
    

}
