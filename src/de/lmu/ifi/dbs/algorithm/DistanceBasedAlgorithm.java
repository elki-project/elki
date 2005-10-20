package de.lmu.ifi.dbs.algorithm;

import de.lmu.ifi.dbs.data.MetricalObject;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.properties.PropertyDescription;
import de.lmu.ifi.dbs.properties.PropertyName;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.NoParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;

import java.util.List;

/**
 * Provides an abstract algorithm already setting the distance funciton.
 *
 * @author Arthur Zimek (<a
 *         href="mailto:zimek@dbs.ifi.lmu.de">zimek@dbs.ifi.lmu.de</a>)
 */
public abstract class DistanceBasedAlgorithm<O extends MetricalObject, D extends Distance, DF extends DistanceFunction<O,D>> 
extends AbstractAlgorithm<O> {

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
  private DF distanceFunction;

  /**
   * Adds parameter for distance function to parameter map.
   */
  protected DistanceBasedAlgorithm() {
    super();
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
  }

  /**
   * @see AbstractAlgorithm#description()
   */
  @Override
  public String description() {
    StringBuffer description = new StringBuffer(super.description());
    description.append('\n');
    description.append("DistanceFunctions available within KDD-Framework:\n");
    description.append('\n');
    for(PropertyDescription pd : Properties.KDD_FRAMEWORK_PROPERTIES.getProperties(PropertyName.DISTANCE_FUNCTIONS))
    {
        description.append(pd.getEntry());
        description.append('\n');
        description.append(pd.getDescription());
        description.append('\n');
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
  public String[] setParameters(String[] args) throws IllegalArgumentException {
    String[] remainingParameters = super.setParameters(args);
    if (optionHandler.isSet(DISTANCE_FUNCTION_P)) {
      try {
        String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
        //noinspection unchecked
        distanceFunction = (DF) Class.forName(className).newInstance();
      }
      catch (UnusedParameterException e) {
        throw new IllegalArgumentException(e);
      }
      catch (NoParameterValueException e) {
        throw new IllegalArgumentException(e);
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e);
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }
    else {
      try {
        //noinspection unchecked
        distanceFunction = (DF) Class.forName(DEFAULT_DISTANCE_FUNCTION).newInstance();
      }
      catch (InstantiationException e) {
        throw new IllegalArgumentException(e);
      }
      catch (IllegalAccessException e) {
        throw new IllegalArgumentException(e);
      }
      catch (ClassNotFoundException e) {
        throw new IllegalArgumentException(e);
      }
    }
    return distanceFunction.setParameters(remainingParameters);
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
