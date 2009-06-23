package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;

/**
 * Provides a distance function that computes the distance
 * (which is a double distance) between feature vectors only in specified dimensions.
 *
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 * todo parameter
 */
public abstract class AbstractDimensionsSelectingDoubleDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractDoubleDistanceFunction<V> {

  /**
   * OptionID for {@link #DIMS_PARAM}
   */
  public static final OptionID DIMS_ID = OptionID.getOrCreateOptionID(
      "distance.dims", "a comma separated array of integer " +
      "values, where 1 <= d_i <= the " +
      "dimensionality of the feature space " +
      "specifying the dimensions to be considered " +
      "for distance computation. If this parameter is not set, " +
      "no dimensions will be considered, i.e. the distance between " +
      "two objects is always 0.");

  /**
   * Dimensions parameter.
   */
  private final IntListParameter DIMS_PARAM = new IntListParameter(DIMS_ID, new ListGreaterEqualConstraint<Integer>(1), true, null);

  /**
   * The dimensions to be considered for distance computation.
   */
  private BitSet dimensions = new BitSet();
 
  /**
   * Provides a distance function that computes the distance
   * (which is a double distance) between feature vectors only in specified dimensions.
   */
  public AbstractDimensionsSelectingDoubleDistanceFunction() {
    super();
    addOption(DIMS_PARAM);
  }

  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // dim
    if (DIMS_PARAM.isSet()) {
      dimensions.clear();
      List<Integer> dimensionList = DIMS_PARAM.getValue();
      for (int d : dimensionList) {
        dimensions.set(d-1);
      }
    }

    return remainingParameters;
  }

  /**
   * Returns a bit set representing the selected dimensions.
   *
   * @return a bit set representing the selected dimensions
   */
  public BitSet getSelectedDimensions() {
    BitSet dimensions = new BitSet(this.dimensions.size());
    dimensions.or(this.dimensions);
    return dimensions;
  }

  /**
   * Sets the selected dimensions according to the set bits in the given BitSet.
   * 
   * 
   * @param dimensions a BitSet designating the new selected dimensions 
   */
  public void setSelectedDimensions(BitSet dimensions) {
    String s = dimensions.toString().replace("{", "").replace("}", "").replace(" ", "");
    try {
      this.DIMS_PARAM.setValue(s);
    }
    catch(ParameterException e) {
      throw new IllegalArgumentException(e);
    }
    this.dimensions.clear();
    this.dimensions.or(dimensions);
  }
  
  
}
