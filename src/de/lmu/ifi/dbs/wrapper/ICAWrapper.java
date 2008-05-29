package de.lmu.ifi.dbs.wrapper;

import de.lmu.ifi.dbs.algorithm.ICA;
import de.lmu.ifi.dbs.algorithm.KDDTask;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.varianceanalysis.PercentageEigenPairFilter;
import de.lmu.ifi.dbs.varianceanalysis.ica.FastICA;
import de.lmu.ifi.dbs.varianceanalysis.ica.KurtosisBasedContrastFunction;

import java.util.List;

/**
 * Wrapper class for ICA algorithm.
 *
 * @author Elke Achtert
 */
public class ICAWrapper extends FileBasedDatabaseConnectionWrapper {
    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "the maximum radius of the neighborhood to" +
                                           "be considerd, must be suitable to " +
                                           EuklideanDistanceFunction.class.getName();


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    public static void main(String[] args) {
        ICAWrapper wrapper = new ICAWrapper();
        try {
            wrapper.setParameters(args);
            wrapper.run();
        }
//    catch (ParameterException e) {
//      Throwable cause = e.getCause() != null ? e.getCause() : e;
//      wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), cause);
//    }
//    catch (AbortException e) {
//      wrapper.verbose(e.getMessage());
//    }
        catch (Exception e) {
            wrapper.exception(wrapper.optionHandler.usage(e.getMessage()), e);
        }
    }

    /**
     * Sets the parameters epsilon and minpts in the parameter map additionally to the
     * parameters provided by super-classes.
     */
    public ICAWrapper() {
        super();
    }

    /**
     * @see KDDTaskWrapper#getKDDTaskParameters()
     */
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm ICA
        parameters.add(OptionHandler.OPTION_PREFIX + KDDTask.ALGORITHM_P);
        parameters.add(ICA.class.getName());

        // parser
//    parameters.add(OptionHandler.OPTION_PREFIX + FileBasedDatabaseConnection.PARSER_P);
//    parameters.add(RealVectorLabelTransposingParser.class.getName());

        // approach
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.APPROACH_P);
//    parameters.add(FastICA.Approach.SYMMETRIC.toString());
        parameters.add(FastICA.Approach.DEFLATION.toString());

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.EPSILON_P);
        parameters.add("0.01");

        // contrast function
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.G_P);
        parameters.add(KurtosisBasedContrastFunction.class.getName());
//    parameters.add(TanhContrastFunction.class.getName());
//    parameters.add(ExponentialContrastFunction.class.getName());

        // number ics
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.IC_P);
        parameters.add("2");

        // initial matrix
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.UNIT_F);

        // max iterations
        parameters.add(OptionHandler.OPTION_PREFIX + FastICA.MAX_ITERATIONS_P);
        parameters.add("1000");

        // epsilon
        parameters.add(OptionHandler.OPTION_PREFIX + PercentageEigenPairFilter.ALPHA_P);
        parameters.add("0.95");

        return parameters;
    }
}
