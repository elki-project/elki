package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OnlineBasicLOF;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.List;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class OnlineBasicLOFWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * The value of the minpts parameter.
     */
    private int minpts;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        OnlineBasicLOFWrapper wrapper = new OnlineBasicLOFWrapper();
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
     * Sets the parameter minpts in the parameter map additionally
     * to the parameters provided by super-classes.
     */
    public OnlineBasicLOFWrapper() {
        super();
        optionHandler.put(new IntParameter(OnlineBasicLOF.MINPTS_P, OnlineBasicLOF.MINPTS_D, new GreaterConstraint(0)));
    }

    /**
     * @see de.lmu.ifi.dbs.elki.wrapper.KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm LOF
        Util.addParameter(parameters, OptionID.ALGORITHM, OnlineBasicLOF.class.getName());

        // minpts
        parameters.add(OptionHandler.OPTION_PREFIX + OnlineBasicLOF.MINPTS_P);
        parameters.add(Integer.toString(minpts));

        // distance function
        Util.addParameter(parameters, OptionID.ALGORITHM_DISTANCEFUNCTION, EuklideanDistanceFunction.class.getName());

        // normalization
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_P);
//    parameters.add(AttributeWiseRealVectorNormalization.class.getName());
//    parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.NORMALIZATION_UNDO_F);

        // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + OnlineBasicLOF.PAGE_SIZE_P);
//    parameters.add("8000");

        // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + OnlineBasicLOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);

        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);
        minpts = (Integer) optionHandler.getOptionValue(OnlineBasicLOF.MINPTS_P);
        return remainingParameters;
    }
}


