package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
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
public abstract class AbstractEditDistanceFunction extends AbstractPrimitiveDistanceFunction<NumberVector<?,?>, DoubleDistance> {
  /**
   * @apiviz.exclude
   */
  protected enum Step {
    NONE, INS, DEL, MATCH
  }

  /**
   * OptionID for {@link #BANDSIZE_PARAM}
   */
  public static final OptionID BANDSIZE_ID = OptionID.getOrCreateOptionID("edit.bandSize", "the band size for Edit Distance alignment (positive double value, 0 <= bandSize <= 1)");

  /**
   * BANDSIZE parameter
   */
  protected final DoubleParameter BANDSIZE_PARAM = new DoubleParameter(BANDSIZE_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.1);

  /**
   * Keeps the currently set bandSize.
   */
  protected double bandSize;

  /**
   * Provides a Dynamic Time Warping distance function that can compute the
   * Dynamic Time Warping distance (that is a DoubleDistance) for FeatureVectors.
   */
  protected AbstractEditDistanceFunction(Parameterization config) {
    config = config.descend(this);
    if (config.grab(BANDSIZE_PARAM)) {
      bandSize = BANDSIZE_PARAM.getValue();
    }
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public Class<? super NumberVector<?,?>> getInputDatatype() {
    return NumberVector.class;
  }
}