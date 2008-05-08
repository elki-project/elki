package de.lmu.ifi.dbs.distance.distancefunction;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.utilities.optionhandling.IntListParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;

import java.util.BitSet;
import java.util.List;

/**
 * Provides a distance function that computes the distance
 * (which is a double distance) between feature vectors only in specified dimensions.
 *
 * @author Elke Achtert
 */
public abstract class AbstractDimensionsSelectingDoubleDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractDoubleDistanceFunction<V> {

  /**
   * Option string for parameter dims.
   */
  public static final String DIMS_P = "dims";

  /**
   * Description for parameter dim.
   */
  public static final String DIMS_D = "<d_1,...,d_n> a comma separated array of integer " +
                                      "values, where 1 <= d_i <= the " +
                                      "dimensionality of the feature space " +
                                      "specifying the dimensions to be considered " +
                                      "for distance computation.";
  /**
   * The dimensions to be considered for distance computation.
   */
  BitSet dimensions;

  /**
   * Provides a distance function that computes the distance
   * (which is a double distance) between feature vectors only in specified dimensions.
   */
  public AbstractDimensionsSelectingDoubleDistanceFunction() {
    super();
    optionHandler.put(new IntListParameter(DIMS_P, DIMS_D));
  }


  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // dim
    List<Integer> dimensionList = optionHandler.getOptionValue(DIMS_P);
    dimensions = new BitSet();
    for (int d : dimensionList) {
      dimensions.set(d);
    }

    return remainingParameters;
  }

  /**
   * Returns a bit set representing the selected dimensions.
   *
   * @return a bit set representing the selected dimensions
   */
  public BitSet getSelectedDimension() {
    return dimensions;
  }
}
