package de.lmu.ifi.dbs.elki.distance.distancefunction.subspace;

import java.util.BitSet;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.ListGreaterEqualConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntListParameter;

/**
 * Provides a distance function that computes the distance (which is a double
 * distance) between feature vectors only in specified dimensions.
 * 
 * @author Elke Achtert
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public abstract class AbstractDimensionsSelectingDoubleDistanceFunction<V extends FeatureVector<V, ?>> extends AbstractDoubleDistanceFunction<V> {
  /**
   * OptionID for {@link #DIMS_PARAM}
   */
  public static final OptionID DIMS_ID = OptionID.getOrCreateOptionID("distance.dims", "a comma separated array of integer values, where 1 <= d_i <= the dimensionality of the feature space specifying the dimensions to be considered for distance computation. If this parameter is not set, no dimensions will be considered, i.e. the distance between two objects is always 0.");

  /**
   * Dimensions parameter.
   */
  private final IntListParameter DIMS_PARAM = new IntListParameter(DIMS_ID, new ListGreaterEqualConstraint<Integer>(1), true);

  /**
   * The dimensions to be considered for distance computation.
   */
  private BitSet dimensions = new BitSet();

  /**
   * Provides a distance function that computes the distance (which is a double
   * distance) between feature vectors only in specified dimensions.
   */
  public AbstractDimensionsSelectingDoubleDistanceFunction(Parameterization config) {
    super();
    if(config.grab(this, DIMS_PARAM)) {
      dimensions.clear();
      List<Integer> dimensionList = DIMS_PARAM.getValue();
      for(int d : dimensionList) {
        dimensions.set(d - 1);
      }
    }
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
    try {
      this.DIMS_PARAM.setValue(Util.convertBitSetToListInt(dimensions, 1));
    }
    catch(ParameterException e) {
      throw new IllegalArgumentException(e);
    }
    this.dimensions.clear();
    this.dimensions.or(dimensions);
  }

}
