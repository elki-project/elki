package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Provides the Edit Distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 */
public abstract class AbstractEditDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?, ?>, DoubleDistance> {
  /**
   * BANDSIZE parameter
   */
  public static final OptionID BANDSIZE_ID = OptionID.getOrCreateOptionID("edit.bandSize", "the band size for Edit Distance alignment (positive double value, 0 <= bandSize <= 1)");

  /**
   * Keeps the currently set bandSize.
   */
  protected double bandSize;

  /**
   * Constructor.
   * 
   * @param bandSize Band size
   */
  public AbstractEditDistanceFunction(double bandSize) {
    super();
    this.bandSize = bandSize;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  // TODO: relax this to VectorTypeInformation!
  @Override
  public VectorFieldTypeInformation<? super NumberVector<?, ?>> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static abstract class Parameterizer extends AbstractParameterizer {
    protected double bandSize = 0.0;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      final DoubleParameter bandSizeP = new DoubleParameter(BANDSIZE_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.1);
      if(config.grab(bandSizeP)) {
        bandSize = bandSizeP.getValue();
      }
    }
  }
}