package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.ProjectedClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.PROCLUS;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for PROCLUS algorithm.
 *
 * @author Elke Achtert
 */
public class PROCLUSWrapper extends FileBasedDatabaseConnectionWrapper {

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
        PROCLUSWrapper wrapper = new PROCLUSWrapper();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (ParameterException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
        }
        catch (AbortException e) {
            wrapper.verbose(e.getMessage());
        }
        catch (Exception e) {
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Adds parameters
     * {@link #K_PARAM}, {@link #K_I_PARAM} and {@link #L_PARAM}
     * to the option handler additionally to parameters of super class.
     */
    public PROCLUSWrapper() {
        super();
        // k
        addOption(K_PARAM);
        // k_i
        addOption(K_I_PARAM);
        // l
        addOption(L_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // PROCLUS algorithm
        Util.addParameter(parameters, OptionID.ALGORITHM, PROCLUS.class.getName());

        // l
        Util.addParameter(parameters, L_PARAM, Integer.toString(getParameterValue(L_PARAM)));

        // k
        Util.addParameter(parameters, K_PARAM, Integer.toString(getParameterValue(K_PARAM)));

        // k_i
        Util.addParameter(parameters, K_I_PARAM, Integer.toString(getParameterValue(K_I_PARAM)));


        return parameters;
    }
}
