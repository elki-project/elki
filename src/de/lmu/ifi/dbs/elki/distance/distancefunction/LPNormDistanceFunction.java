package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Provides a LP-Norm for FeatureVectors.
 *
 * @author Arthur Zimek
 * @param <V> the type of FeatureVector to compute the distances in between
 * TODO: implement SpatialDistanceFunction
 * todo parameter
 */
public class LPNormDistanceFunction<V extends FeatureVector<V, N>, N extends Number>
    extends AbstractDoubleDistanceFunction<V> {

    /**
     * OptionID for {@link #P_PARAM}
     */
    public static final OptionID P_ID = OptionID.getOrCreateOptionID("lpnorm.p",
        "the degree of the L-P-Norm (positive number)");

    /**
     * P parameter
     */
    private final DoubleParameter P_PARAM = new DoubleParameter(P_ID, new GreaterConstraint(0));

    /**
     * Keeps the currently set p.
     */
    private double p;

    /**
     * Provides a LP-Norm for FeatureVectors.
     */
    public LPNormDistanceFunction() {
        super();
        addOption(P_PARAM);
    }

    /**
     * Returns the distance between the specified FeatureVectors as a LP-Norm
     * for the currently set p.
     *
     * @param v1 first FeatureVector
     * @param v2 second FeatureVector
     * @return the distance between the specified FeatureVectors as a LP-Norm
     *         for the currently set p
     */
    public DoubleDistance distance(V v1, V v2) {
        if (v1.getDimensionality() != v2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of FeatureVectors\n  first argument: " + v1.toString() + "\n  second argument: " + v2.toString());
        }

        double sqrDist = 0;
        for (int i = 1; i <= v1.getDimensionality(); i++) {
            double manhattanI = Math.abs(v1.getValue(i).doubleValue() - v2.getValue(i).doubleValue());
            sqrDist += Math.pow(manhattanI, p);
        }
        return new DoubleDistance(Math.pow(sqrDist, 1.0 / p));
    }

    @Override
    public String parameterDescription() {
        StringBuffer description = new StringBuffer();
        description.append(optionHandler.usage("LP-Norm for FeatureVectors.", false));
        description.append('\n');
        return description.toString();
    }

    @Override
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingOptions = super.setParameters(args);

        p = P_PARAM.getValue();

        return remainingOptions;
    }
}
