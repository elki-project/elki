package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.ProjectedClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ORCLUS;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for ORCLUS algorithm.
 *
 * @author Elke Achtert
 */
public class ORCLUSWrapper extends FileBasedDatabaseConnectionWrapper {
    /**
     * Parameter to specify the number of clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projectedclustering.k} </p>
     */
    private final IntParameter K_PARAM = new IntParameter(
        ProjectedClustering.K_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the multiplier for the initial number of seeds,
     * must be an integer greater than 0.
     * <p>Default value: {@code 30} </p>
     * <p>Key: {@code -projectedclustering.k_i} </p>
     */
    private final IntParameter K_I_PARAM = new IntParameter(
        ProjectedClustering.K_I_ID,
        new GreaterConstraint(0),
        30);

    /**
     * Parameter to specify the dimensionality of the clusters to find,
     * must be an integer greater than 0.
     * <p>Key: {@code -projectedclustering.l} </p>
     */
    private final IntParameter L_PARAM = new IntParameter(
        ProjectedClustering.L_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        new ORCLUSWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #K_PARAM}, {@link #K_I_PARAM} and {@link #L_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public ORCLUSWrapper() {
        super();
        // k
        addOption(K_PARAM);
        // k_i
        addOption(K_I_PARAM);
        // l
        addOption(L_PARAM);
    }

    @Override
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // ORCLUS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, ORCLUS.class.getName());

        // l
        Util.addParameter(parameters, L_PARAM, Integer.toString(getParameterValue(L_PARAM)));

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // k_i
        Util.addParameter(parameters, K_I_PARAM, Integer.toString(getParameterValue(K_I_PARAM)));

        return parameters;
    }
}
