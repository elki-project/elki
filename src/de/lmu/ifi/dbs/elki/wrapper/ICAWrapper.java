package de.lmu.ifi.dbs.elki.wrapper;

import java.util.List;

import de.lmu.ifi.dbs.elki.algorithm.ICA;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.Util;
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
        Util.addParameter(parameters, FastICA.APPROACH_ID, FastICA.Approach.DEFLATION.toString());

        // epsilon
        Util.addParameter(parameters, FastICA.EPSILON_ID, "0.01");

        // contrast function
        Util.addParameter(parameters, FastICA.G_ID, KurtosisBasedContrastFunction.class.getName());
//    parameters.add(TanhContrastFunction.class.getName());
//    parameters.add(ExponentialContrastFunction.class.getName());

        // number ics
        Util.addParameter(parameters, FastICA.IC_ID, "2");

        // initial matrix
        Util.addFlag(parameters, FastICA.UNIT_ID);

        // max iterations
        Util.addParameter(parameters, FastICA.MAX_ITERATIONS_ID, "1000");

        // epsilon
        Util.addParameter(parameters, OptionID.EIGENPAIR_FILTER_ALPHA, "0.95");

        return parameters;
    }
}
