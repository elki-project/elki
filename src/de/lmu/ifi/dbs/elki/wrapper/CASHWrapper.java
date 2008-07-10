package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.CASH;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for CASH algorithm.
 *
 * @author Elke Achtert
 */
public class CASHWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * Parameter to specify the threshold for minimum number of points in a cluster,
     * must be an integer greater than 0.
     * <p>Key: {@code -cash.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(CASH.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * Parameter to specify the maximum level for splitting the hypercube,
     * must be an integer greater than 0.
     * <p>Key: {@code -cash.maxlevel} </p>
     */
    private final IntParameter MAXLEVEL_PARAM = new IntParameter(CASH.MAXLEVEL_ID,
        new GreaterConstraint(0));

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        CASHWrapper wrapper = new CASHWrapper();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
        catch (Exception e) {
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Sets the parameters epsilon and minpts in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public CASHWrapper() {
        super();
        // parameter min points
        addOption(MINPTS_PARAM);

        // parameter max level
        addOption(MAXLEVEL_PARAM);
    }

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm CASH
        Util.addParameter(parameters, OptionID.ALGORITHM, CASH.class.getName());

        // parser
        parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.PARSER_P);
        parameters.add(ParameterizationFunctionLabelParser.class.getName());

        // minpts
        Util.addParameter(parameters, MINPTS_PARAM, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // maxLevel
        Util.addParameter(parameters, MAXLEVEL_PARAM, Integer.toString(getParameterValue(MAXLEVEL_PARAM)));

        return parameters;
    }
}
