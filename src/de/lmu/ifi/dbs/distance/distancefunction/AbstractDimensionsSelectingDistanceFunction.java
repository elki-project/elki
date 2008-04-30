package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.FeatureVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterEqualConstraint;

/**
 * Provides a distance function that computes the distance
 * between feature vectors only in specified dimensions.
 *
 * @author Elke Achtert
 */
public abstract class AbstractDimensionsSelectingDistanceFunction<N extends Number, V extends FeatureVector<V, N>, D extends Distance<D>> extends AbstractDistanceFunction<V, D> {

  /**
   * Option string for parameter dims.
   */
  public static final String DIMS_P = "dims";

  /**
   * Description for parameter dim.
   */
  public static final String DIMS_D = "an array of integer values between 1 and the " +
                                      "dimensionality of the feature space " +
                                      "specifying the dimensions to be considered " +
                                      "for distance computation.";
  /**
   * The dimensions to be considered for distance computation.
   */
  private int[] dims;

  public AbstractDimensionsSelectingDistanceFunction() {
    super();

    // todo int list parameter
    optionHandler.put(new IntParameter(DIMS_P, DIMS_D, new GreaterEqualConstraint(1)));
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // dim
    // todo
//    dims = (Integer) optionHandler.getOptionValue(DIMS_P);

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the selected dimension.
   *
   * @return the selected dimension
   */
  public int[] getSelectedDimension() {
    return dims;
  }


}
