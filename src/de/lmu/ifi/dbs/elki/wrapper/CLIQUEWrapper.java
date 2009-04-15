package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.CLIQUE;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.IntervalConstraint;

/**
 * Wrapper class for CLIQUE algorithm.
 *
 * @author Elke Achtert
 * @param <O> object type
 */
public class CLIQUEWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {

    /**
     * Parameter to specify the number of intervals (units) in each dimension,
     * must be an integer greater than 0.
     * <p>Key: {@code -clique.xsi} </p>
     */
    private final IntParameter XSI_PARAM = new IntParameter(CLIQUE.XSI_ID, new GreaterConstraint(0));

    /**
     * Parameter to specify the density threshold for the selectivity of a unit,
     * where the selectivity is the fraction of total feature vectors contained in this unit,
     * must be a double greater than 0 and less than 1.
     * <p>Key: {@code -clique.tau} </p>
     */
    private final DoubleParameter TAU_PARAM = new DoubleParameter(CLIQUE.TAU_ID,
        new IntervalConstraint(0, IntervalConstraint.IntervalBoundary.OPEN, 1, IntervalConstraint.IntervalBoundary.OPEN));

    /**
     * Flag to indicate that that only subspaces with large coverage
     * (i.e. the fraction of the database that is covered by the dense units)
     * are selected, the rest will be pruned.
     * <p>Key: {@code -clique.prune} </p>
     */
    private final Flag PRUNE_FLAG = new Flag(CLIQUE.PRUNE_ID);


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new CLIQUEWrapper().runCLIWrapper(args);
    }

    /**
     * Adds parameters
     * {@link #XSI_PARAM}, {@link #TAU_PARAM}, and flag {@link #PRUNE_FLAG}
     * to the option handler additionally to parameters of super class.
     */
    public CLIQUEWrapper() {
        super();
        //parameter xsi
        addOption(XSI_PARAM);

        //parameter tau
        addOption(TAU_PARAM);

        //flag prune
        addOption(PRUNE_FLAG);
    }

    @Override
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm CLIQUE
        OptionUtil.addParameter(parameters, OptionID.ALGORITHM, CLIQUE.class.getName());

        // xsi
        OptionUtil.addParameter(parameters, XSI_PARAM, Integer.toString(XSI_PARAM.getValue()));

        // tau
        OptionUtil.addParameter(parameters, TAU_PARAM, Double.toString(TAU_PARAM.getValue()));

        // prune
        if (PRUNE_FLAG.isSet()) {
            OptionUtil.addFlag(parameters, PRUNE_FLAG);
        }

        return parameters;
    }
}
