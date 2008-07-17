package de.lmu.ifi.dbs.elki.wrapper;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.algorithm.outlier.OnlineLOF;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;

import java.io.File;
import java.util.List;

/**
 * Wrapper class for LOF algorithm. Performs an attribute wise normalization
 * on the database objects.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class OnlineLOFWrapper extends FileBasedDatabaseConnectionWrapper {

    /**
     * Parameter to specify the number of nearest neighbors of an object to be considered for computing its LOF,
     * must be an integer greater than 0.
     * <p>Key: {@code -lof.minpts} </p>
     */
    private final IntParameter MINPTS_PARAM = new IntParameter(
        OnlineLOF.MINPTS_ID,
        new GreaterConstraint(0));

    /**
     * The value of the insertions parameter.
     */
    private File insertions;

    /**
     * The value of the lof parameter.
     */
    private File lof;

    /**
     * The value of the nn parameter.
     */
    private File nn;

    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        OnlineLOFWrapper wrapper = new OnlineLOFWrapper();
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
     * Adds parameter
     * {@link #MINPTS_PARAM} todo
     * to the option handler additionally to parameters of super class.
     */
    public OnlineLOFWrapper() {
        super();
        // parameter min points
        addOption(MINPTS_PARAM);

        // parameter insertions
        optionHandler.put(new FileParameter(OnlineLOF.INSERTIONS_P, OnlineLOF.INSERTIONS_D,
            FileParameter.FileType.INPUT_FILE));

        // parameter LOF
        optionHandler.put(new FileParameter(OnlineLOF.LOF_P, OnlineLOF.LOF_D,
            FileParameter.FileType.INPUT_FILE));

        //parameter nn
        optionHandler.put(new FileParameter(OnlineLOF.NN_P, OnlineLOF.NN_D,
            FileParameter.FileType.INPUT_FILE));
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm OnlineLOF
        Util.addParameter(parameters, OptionID.ALGORITHM, OnlineLOF.class.getName());

        // minpts
        Util.addParameter(parameters, OnlineLOF.MINPTS_ID, Integer.toString(getParameterValue(MINPTS_PARAM)));

        // insertions
        parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.INSERTIONS_P);
        parameters.add(insertions.getPath());

        // lof
        parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.LOF_P);
        parameters.add(lof.getPath());

        // nn
        parameters.add(OptionHandler.OPTION_PREFIX + OnlineLOF.NN_P);
        parameters.add(nn.getPath());

        // distance function
        Util.addParameter(parameters, OnlineLOF.DISTANCE_FUNCTION_ID, EuklideanDistanceFunction.class.getName());

        // page size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.PAGE_SIZE_P);
//    parameters.add("8000");

        // cache size
//    parameters.add(OptionHandler.OPTION_PREFIX + LOF.CACHE_SIZE_P);
//    parameters.add("" + 8000 * 10);


        return parameters;
    }

    /**
     * @see de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable#setParameters(String[])
     */
    public String[] setParameters(String[] args) throws ParameterException {
        String[] remainingParameters = super.setParameters(args);

        insertions = (File) optionHandler.getOptionValue(OnlineLOF.INSERTIONS_P);
        lof = (File) optionHandler.getOptionValue(OnlineLOF.LOF_P);
        nn = (File) optionHandler.getOptionValue(OnlineLOF.NN_P);

        return remainingParameters;
    }
}
