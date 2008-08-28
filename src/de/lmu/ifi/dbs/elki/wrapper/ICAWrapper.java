package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.ICA;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.varianceanalysis.ica.FastICA;
import de.lmu.ifi.dbs.elki.varianceanalysis.ica.KurtosisBasedContrastFunction;

/**
 * Wrapper class for ICA algorithm.
 *
 * @author Elke Achtert
 *         todo parameter
 */
public class ICAWrapper<O extends DatabaseObject> extends FileBasedDatabaseConnectionWrapper<O> {
    /**
     * Description for parameter epsilon.
     */
    public static final String EPSILON_D = "the maximum radius of the neighborhood to" +
        "be considerd, must be suitable to " +
        EuclideanDistanceFunction.class.getName();


    /**
     * Main method to run this wrapper.
     *
     * @param args the arguments to run this wrapper
     */
    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        new ICAWrapper().runCLIWrapper(args);
    }

    @Override
    public List<String> getKDDTaskParameters() {
        List<String> parameters = super.getKDDTaskParameters();

        // algorithm ICA
        Util.addParameter(parameters, OptionID.ALGORITHM, ICA.class.getName());

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
        Util.addParameter(parameters, OptionID.EIGENPAIR_FILTER_ALPHA, "0.95");

        return parameters;
    }
}
