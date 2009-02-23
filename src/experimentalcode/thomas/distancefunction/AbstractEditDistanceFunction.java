package experimentalcode.thomas.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.MetricDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint.IntervalBoundary;

/**
 * Provides the Edit Distance for NumberVectors.
 *
 * @author Thomas Bernecker
 * @param <V> the type of NumberVector to compute the distances in between
 */
public abstract class AbstractEditDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractDoubleDistanceFunction<V>
    implements MetricDistanceFunction<V, DoubleDistance> {

	public enum Step 
	{
		NONE, INS, DEL, MATCH
	}
	
	/**
     * OptionID for {@link #BANDSIZE_PARAM}
     */
    public static final OptionID BANDSIZE_ID = OptionID.getOrCreateOptionID("edit.bandSize",
        "the band size for Edit Distance alignment (positive double value, 0 <= bandSize <= 1)");

    /**
     * BANDSIZE parameter
     */
    protected final DoubleParameter BANDSIZE_PARAM = new DoubleParameter(BANDSIZE_ID, new IntervalConstraint(0, IntervalBoundary.CLOSE, 1, IntervalBoundary.CLOSE), 0.1);

    /**
     * Keeps the currently set bandSize.
     */
    protected double bandSize;
	
	/**
     * Provides a Dynamic Time Warping distance function that can compute the Dynamic Time Warping
     * distance (that is a DoubleDistance) for NumberVectors.
     */
    protected AbstractEditDistanceFunction() {
        super();
        addOption(BANDSIZE_PARAM);
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingOptions = super.setParameters(args);

        bandSize = BANDSIZE_PARAM.getValue();

        return remainingOptions;
    }
}
