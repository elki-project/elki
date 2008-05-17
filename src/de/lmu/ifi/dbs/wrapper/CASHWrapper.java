package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.algorithm.clustering.cash.CASH;
import de.lmu.ifi.dbs.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.parser.ParameterizationFunctionLabelParser;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.UnusedParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for CASH algorithm.
 *
 * @author Elke Achtert
 */
public class CASHWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * Minimum points.
     */
    private int minpts;

    /**
     * The maximum level for splitting the hypercube.
     */
    private int maxLevel;

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
        optionHandler.put(new IntParameter(CASH.MINPTS_P, CASH.MINPTS_D, new GreaterConstraint(0)));

        // parameter max level
        optionHandler.put(new IntParameter(CASH.MAXLEVEL_P, CASH.MAXLEVEL_D, new GreaterConstraint(0)));
    }

    /**
     * @see de.lmu.ifi.dbs.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() throws UnusedParameterException {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm CASH
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(CASH.class.getName());

        // parser
        parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.PARSER_P);
        parameters.add(ParameterizationFunctionLabelParser.class.getName());

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + CASH.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        // maxLevel
        parameters.add(OptionHandler.OPTION_PREFIX + CASH.MAXLEVEL_P);
        parameters.add(Integer.toString(maxLevel));

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        //  minpts, maxLevel
        minpts = (Integer) optionHandler.getOptionValue(CASH.MINPTS_P);
        maxLevel = (Integer) optionHandler.getOptionValue(CASH.MAXLEVEL_P);

        return remainingParameters;
    }
}
