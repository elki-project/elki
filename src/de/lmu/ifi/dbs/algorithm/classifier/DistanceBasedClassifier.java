package de.lmu.ifi.dbs.algorithm.classifier;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;

import java.util.List;

/**
 * An abstract classifier already based on DistanceBasedAlgorithm
 * making use of settings for time and verbose and DistanceFunction.
 * 
 * @author Arthur Zimek (<a href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DistanceBasedClassifier<O extends DatabaseObject, D extends Distance<D>> extends AbstractClassifier<O>
{
    /**
     * The default distance function.
     */
    public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

    /**
     * Parameter for distance function.
     */
    public static final String DISTANCE_FUNCTION_P = "distancefunction";

    /**
     * Description for parameter distance function.
     */
    public static final String DISTANCE_FUNCTION_D = "<classname>the distance function to determine the distance between metrical objects - must implement " + DistanceFunction.class.getName() + ". (Default: " + DEFAULT_DISTANCE_FUNCTION + ").";

    /**
     * The distance function.
     */
    private DistanceFunction<O, D> distanceFunction;


    /**
     * Adds parameter for distance function to parameter map.
     */
    protected DistanceBasedClassifier()
    {
        super();
        parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    }



    /**
     * Provides a classification for a given instance.
     * The classification is the index of the class-label
     * in {@link #labels labels}.
     * 
     * This method returns the index of the maximum probability
     * as provided by {@link #classDistribution(O) classDistribution(M)}.
     * If an extending classifier requires a different classification,
     * it should overwrite this method.
     * 
     * @param instance an instance to classify
     * @return a classification for the given instance
     * @throws IllegalStateException if the Classifier has not been initialized
     * or properly trained
     */
    public int classify(O instance) throws IllegalStateException
    {
        return Util.getIndexOfMaximum(classDistribution(instance));
    }

    /**
     * Returns the parameter setting of the attributes.
     *
     * @return the parameter setting of the attributes
     */
    public List<AttributeSettings> getAttributeSettings() {
      List<AttributeSettings> result = super.getAttributeSettings();

      AttributeSettings attributeSettings = new AttributeSettings(this);
      attributeSettings.addSetting(DISTANCE_FUNCTION_P, distanceFunction.getClass().getSimpleName());
      result.add(attributeSettings);
      
      return result;
    }
    

    /**
     * Returns the distanceFunction.
     *
     * @return the distanceFunction
     */
    protected DistanceFunction<O,D> getDistanceFunction() {
      return distanceFunction;
    } 

}
