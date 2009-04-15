package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.ORCLUS;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.ProjectedClustering;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

/**
 * Wrapper class for ORCLUS algorithm.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class ORCLUSWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {
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
    @SuppressWarnings("unchecked")
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
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // ORCLUS algorithm
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, ORCLUS.class.getName());

        // l
        OptionUtil.addParameter(parameters, L_PARAM, Integer.toString(L_PARAM.getValue()));

        // k
        OptionUtil.addParameter(parameters, K_PARAM, Integer.toString(K_PARAM.getValue()));

        // k_i
        OptionUtil.addParameter(parameters, K_I_PARAM, Integer.toString(K_I_PARAM.getValue()));

        return parameters;
    }
}
