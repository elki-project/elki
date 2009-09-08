package de.lmu.ifi.dbs.elki.distance.distancefunction.timeseries;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;

/**
 * Provides the Edit Distance for FeatureVectors.
 * 
 * @author Thomas Bernecker
 * @param <V> the type of FeatureVector to compute the distances in between
 */
public abstract class AbstractEditDistanceFunction<V extends FeatureVector<V, ?>> extends AbstractDoubleDistanceFunction<V> {

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
  protected AbstractEditDistanceFunction() {
    super();
    addOption(BANDSIZE_PARAM);
  }

  @Override
  public List<String> setParameters(List<String> args) throws ParameterException {
    List<String> remainingParameters = super.setParameters(args);

    bandSize = BANDSIZE_PARAM.getValue();

    return remainingParameters;
  }
}
