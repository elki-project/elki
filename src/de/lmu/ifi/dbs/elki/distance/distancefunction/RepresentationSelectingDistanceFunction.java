package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.MultiRepresentedObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.parser.Parser;
import de.lmu.ifi.dbs.elki.parser.RealVectorLabelParser;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Distance function for multirepresented objects that selects one representation and
 * computes the distances only within the selected representation.
 *
 * @author Elke Achtert
 * @param <M> the type of MultiRepresentedObject to compute the distances in between
 * @param <O> the type of represented DatabaseObjects
 * @param <D> the type of Distance used
 */
public class RepresentationSelectingDistanceFunction<O extends DatabaseObject, M extends MultiRepresentedObject<O>, D extends Distance<D>>
    extends AbstractDistanceFunction<M, D> {
  /**
   * A pattern defining a comma.
   */
  public static final Pattern SPLIT = Pattern.compile(",");

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class.getName();

  /**
   * OptionID for {@link #DISTANCE_FUNCTIONS_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTIONS_ID = OptionID.getOrCreateOptionID(
      "distancefunctions",
      "A comma separated list of the distance functions to " +
      "determine the distance between objects within one representation."
  );

  /**
   * Parameter to specify the distance functions
   */
  private final ClassListParameter<DistanceFunction<O, D>> DISTANCE_FUNCTIONS_PARAM =
    new ClassListParameter<DistanceFunction<O, D>>(
      DISTANCE_FUNCTIONS_ID, DistanceFunction.class, true);

  /**
   * The index of the current representation.
   */
  private int currentRepresentationIndex = -1;

  /**
   * The list of distance functions for each representation.
   */
  private List<DistanceFunction<O, D>> distanceFunctions;

  /**
   * The default distance function.
   */
  private DistanceFunction<O, D> defaultDistanceFunction;

  /**
   * Provides a Distance function for multirepresented objects that selects one
   * represenation and computes the distances only within the selected representation.
   */
  public RepresentationSelectingDistanceFunction() {
    super();
    // TODO default values!!!
    addOption(DISTANCE_FUNCTIONS_PARAM);
  }

  /**
   * Sets the currently selected representation for which the distances will be computed.
   *
   * @param index the index of the representation to be selected
   */
  public void setCurrentRepresentationIndex(int index) {
    this.currentRepresentationIndex = index;
  }

  public D valueOf(String pattern) throws IllegalArgumentException {
    return getDistanceFunctionForCurrentRepresentation().valueOf(pattern);
  }

  public D infiniteDistance() {
    return getDistanceFunctionForCurrentRepresentation().infiniteDistance();
  }

  public D nullDistance() {
    return getDistanceFunctionForCurrentRepresentation().nullDistance();
  }

  public D undefinedDistance() {
    return getDistanceFunctionForCurrentRepresentation().undefinedDistance();
  }

  public D distance(M o1, M o2) {
    O object1 = o1.getRepresentation(currentRepresentationIndex);
    O object2 = o2.getRepresentation(currentRepresentationIndex);

    return getDistanceFunctionForCurrentRepresentation().distance(object1, object2);
  }

  public String parameterDescription() {
    return "Distance function for multirepresented objects that selects one represenation and " +
           "computes the distances only within the selected representation.";
  }

  @SuppressWarnings("unchecked")
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // distance functions
    if (DISTANCE_FUNCTIONS_PARAM.isSet()) {
      distanceFunctions = DISTANCE_FUNCTIONS_PARAM.instantiateClasses();

      // TODO: do we still need this, or will it be done automatically?
      for (DistanceFunction<O, D> distanceFunction : this.distanceFunctions) {
        remainingParameters = distanceFunction.setParameters(remainingParameters);
      }
      setParameters(args, remainingParameters);
      return remainingParameters;
    }
    else {
      try {
          // todo
        defaultDistanceFunction = Util.instantiate(DistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(DISTANCE_FUNCTIONS_PARAM, DEFAULT_DISTANCE_FUNCTION, e);
      }
      remainingParameters = defaultDistanceFunction.setParameters(remainingParameters);
      setParameters(args, remainingParameters);
      return remainingParameters;
    }
  }

  /**
   * Returns the distance function for the currently selected representation.
   *
   * @return the distance function for the currently selected representation
   */
  private DistanceFunction<O, D> getDistanceFunctionForCurrentRepresentation() {
    if (currentRepresentationIndex < 0)
      throw new IllegalStateException("Wrong representation set, current index = " + currentRepresentationIndex);

    if (distanceFunctions.size() > 0) {
      if (currentRepresentationIndex > distanceFunctions.size())
        throw new IllegalStateException("Wrong representation set, current index = " + currentRepresentationIndex);

      return distanceFunctions.get(currentRepresentationIndex);
    }


    else return defaultDistanceFunction;
  }

}
