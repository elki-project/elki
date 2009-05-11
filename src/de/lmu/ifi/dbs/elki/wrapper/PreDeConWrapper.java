package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PreDeCon;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * A wrapper for the PreDeCon algorithm. Performs an attribute wise normalization on
 * the database objects.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class PreDeConWrapper<O extends DatabaseObject> extends NormalizationWrapper<O> {

    /**
     * Parameter to specify the maximum radius of the neighborhood to be considered,
     * must be suitable to {@link de.lmu.ifi.dbs.elki.distance.distancefunction.LocallyWeightedDistanceFunction}.
     * <p>Key: {@code -projdbscan.epsilon} </p>
     */
    private final PatternParameter EPSILON_PARAM = new PatternParameter(PreDeCon.EPSILON_ID);

    /**
     * Parameter to specify the threshold for minimum number of points in
     * the epsilon-neighborhood of a point,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        PreDeCon.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the intrinsic dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projdbscan.lambda} </p>
     */
    private final IntParameter LAMBDA_PARAM = new IntParameter(
        PreDeCon.LAMBDA_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new PreDeConWrapper<DatabaseObject>().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #EPSILON_PARAM}, {@link #MINPTS_PARAM} and {@link #LAMBDA_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public PreDeConWrapper() {
        super();
        // parameter epsilon
        addOption(EPSILON_PARAM);

        // parameter min points
        addOption(MINPTS_PARAM);

        // parameter lambda
        addOption(LAMBDA_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // PreDeCon algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, PreDeCon.class.getName());

        // epsilon for PreDeCon
        OptionUtil.addParameter(parameters, EPSILON_PARAM, EPSILON_PARAM.getValue());

        // minpts for PreDeCon
        OptionUtil.addParameter(parameters, MINPTS_PARAM, Integer.toString(MINPTS_PARAM.getValue()));

        // lambda for PreDeCon
        OptionUtil.addParameter(parameters, LAMBDA_PARAM, Integer.toString(LAMBDA_PARAM.getValue()));

        return parameters;
    }
}
