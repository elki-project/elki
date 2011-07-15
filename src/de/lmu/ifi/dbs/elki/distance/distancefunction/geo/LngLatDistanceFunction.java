package de.lmu.ifi.dbs.elki.distance.distancefunction.geo;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractVectorDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Distance function for 2D vectors in Longitude, Latitude form.
 * 
 * @author Erich Schubert
 */
public class LngLatDistanceFunction extends AbstractVectorDoubleDistanceFunction {
  /**
   * Static instance
   */
  public static final LngLatDistanceFunction STATIC = new LngLatDistanceFunction();

  /**
   * Constructor. Use static instance instead!
   */
  @Deprecated
  public LngLatDistanceFunction() {
    super();
  }

  @Override
  public double doubleDistance(NumberVector<?, ?> o1, NumberVector<?, ?> o2) {
    return MathUtil.latlngDistance(o1.doubleValue(2), o1.doubleValue(1), o2.doubleValue(2), o2.doubleValue(1));
  }

  @Override
  public SimpleTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return VectorFieldTypeInformation.get(NumberVector.class, 2);
  }

  @Override
  public boolean equals(Object obj) {
    if(obj == null) {
      return false;
    }
    if(obj == this) {
      return true;
    }
    if(this.getClass().equals(obj.getClass())) {
      return true;
    }
    return super.equals(obj);
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected LngLatDistanceFunction makeInstance() {
      return LngLatDistanceFunction.STATIC;
    }
  }
}